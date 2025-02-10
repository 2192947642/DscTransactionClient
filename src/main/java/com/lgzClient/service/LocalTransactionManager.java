package com.lgzClient.service;
import com.lgzClient.rpc.BranchTransactRpc;
import com.lgzClient.rpc.GlobalTransactRpc;
import com.lgzClient.types.*;
import com.lgzClient.types.sql.service.BranchTransaction;
import com.lgzClient.types.sql.service.GlobalTransaction;
import com.lgzClient.types.sql.client.UnCommitSqlLog;
import com.lgzClient.types.status.BranchStatus;
import com.lgzClient.utils.BranchTransactionUtil;
import com.lgzClient.utils.TimeUtil;
import com.lgzClient.wrapper.ConnectionWrapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.net.UnknownHostException;
import java.sql.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public  class LocalTransactionManager {
        @Autowired
        GlobalTransactRpc globalTransactRpc;
        @Autowired
        BranchTransactRpc branchTransactRpc;
        @Autowired
        BranchTransactionUtil branchTransactionUtil;
        @Value("${spring.application.name}")
        public String applicationName;
        @Autowired
        DataSourceTransactionManager transactionManager;
        public static LocalTransactionManager instance;
        //获得事务的id
        public static  Long getTransactionId(Connection connection){
            String sql = "SELECT TRX_ID FROM information_schema.INNODB_TRX WHERE TRX_MYSQL_THREAD_ID = CONNECTION_ID()";
            try (PreparedStatement ps = connection.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("TRX_ID");
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return null;
        }

        @PostConstruct
        public void init()
        {
            instance=this;
        }
        private static final ConcurrentHashMap<String, ConnectionWrapper> localTransactionMaps=new ConcurrentHashMap<>();
        public void begin(String globalId) throws SQLException, UnknownHostException {
                BranchTransaction branchTransaction=branchTransactionUtil.buildDefaultTransaction();
                if(StringUtils.hasLength(globalId)){
                    branchTransaction.setGlobalId(globalId);//设置所属的globalId
                }
                else if(!StringUtils.hasLength(branchTransaction.getGlobalId())){//如果不存在globalId那么就开启一个新的分布式事务
                   GlobalTransaction globalTransaction=globalTransactRpc.createGlobalTransaction().getData();
                    branchTransaction.setGlobalId(globalTransaction.getGlobalId());//设置globalId
                }
                branchTransaction.setBranchId(branchTransactRpc.joinBranchTransaction(branchTransaction).getData().getBranchId());//加入到当前的事务中
                ThreadContext.globalId.set(branchTransaction.getGlobalId());//将该全局事务的id添加到当前的线程中
                ThreadContext.isDscTransaction.set(true);
                ThreadContext.branchTransaction.set(branchTransaction);
                this.buildLocalTransaction(branchTransaction);//创建一个本地事务.并将其与本地事务关联
        }
        //修改存储在redis中的本地事务状态
        public void updateStatus(BranchTransaction branchTransaction, BranchStatus branchStatus) {
             branchTransaction.setStatus(branchStatus);
             branchTransaction.setStatus(branchStatus);
             branchTransactRpc.updateBranchTransactionStatus(branchTransaction);//修改分支事务的状
         }
        public void updateStatusWithNotice(BranchTransaction branchTransaction, BranchStatus branchStatus) throws InterruptedException {
            branchTransaction.setStatus(branchStatus);
            branchTransactRpc.updateBranchTransactionStatusWithNotice(branchTransaction);
        }
        public void updateStatus(BranchStatus branchStatus){
            BranchTransaction branchTransaction=ThreadContext.branchTransaction.get();
            updateStatus(branchTransaction, branchStatus);
        }
        public void updateStatusWithNotice(BranchStatus branchStatus) throws InterruptedException {
             BranchTransaction branchTransaction=ThreadContext.branchTransaction.get();
             updateStatusWithNotice(branchTransaction, branchStatus);
        }
        private Connection buildLocalTransaction(BranchTransaction branchTransaction) throws  SQLException//新建一个本地事务,并将其绑定到当前的线程中
        {
            ConnectionWrapper connection=new ConnectionWrapper(transactionManager.getDataSource().getConnection());
            ThreadContext.connection.set(connection);
            connection.setAutoCommit(false);
            ConnectionHolder connectionHolder=new ConnectionHolder(connection);
            TransactionSynchronizationManager.bindResource(transactionManager.getDataSource(),connectionHolder);
            localTransactionMaps.put(branchTransaction.getBranchId(),connection);
            return connection;
        }

        public ConnectionWrapper getLocalTransaction(String localId)
        {
           return localTransactionMaps.get(localId);
        }
        //回滚事务
        public void rollBack(String localId) throws SQLException {
            Connection connection=getLocalTransaction(localId);
            if (connection==null) return;//如果连接为null 那么说明已经被操作了
            try {
                connection.rollback();
                BranchTransaction branchTransaction= BranchTransaction.builder().branchId(localId).status(BranchStatus.rollback).build();
                branchTransactRpc.updateBranchTransactionStatus(branchTransaction);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            } finally {
                connection.close();
                localTransactionMaps.remove(localId);
            }

        }
        //提交事务
        public void commit(String localId ) throws SQLException {
            ConnectionWrapper connection=getLocalTransaction(localId);
            if (connection==null) return;
            try {
                LocalTransactionManager.instance.deleteFromDatabase(connection,localId);
                connection.commit();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            } finally {
                connection.close();
                localTransactionMaps.remove(localId);
            }
        }
        public void addLogToDatabase(UnCommitSqlLog unCommitSqlLog) throws SQLException {
        String sql = "insert into un_commit_sql_log(global_id, branch_id, begin_time, request_uri,application_name,server_address,logs) values ( ? , ? , ?, ?, ?, ?, ?)";
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        try {
            connection = transactionManager.getDataSource().getConnection();
            preparedStatement = connection.prepareStatement(sql);
            // 设置参数
            preparedStatement.setString(1, unCommitSqlLog.getGlobalId());
            preparedStatement.setString(2, unCommitSqlLog.getBranchId());
            preparedStatement.setTimestamp(3, new Timestamp(TimeUtil.strToDate(unCommitSqlLog.getBeginTime()).getTime()));
            preparedStatement.setString(4, unCommitSqlLog.getRequestUri());
            preparedStatement.setString(5, unCommitSqlLog.getApplicationName());
            preparedStatement.setString(6, unCommitSqlLog.getServerAddress());
            preparedStatement.setString(7, unCommitSqlLog.getLogs());
            // 执行插入操作
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            // 记录日志或其他处理逻辑
            throw new SQLException("Error inserting log into database", e);
        } finally {
            // 关闭资源
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException e) {
                }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                }
            }
        }
    }
        public void deleteFromDatabase(ConnectionWrapper connection,String localId) throws SQLException {
             String sql = "delete from un_commit_sql_log where  branch_id = ?";
             PreparedStatement preparedStatement = null;
             try {
                 preparedStatement = connection.prepareStatementWithoutWrapper(sql);
                 // 设置参数
                 preparedStatement.setString(1, localId);
                 preparedStatement.executeUpdate();
             }
             catch (SQLException e) {
                 // 记录日志或其他处理逻辑
                 throw new SQLException("Error deleting log from database", e);
             }
        }
}

