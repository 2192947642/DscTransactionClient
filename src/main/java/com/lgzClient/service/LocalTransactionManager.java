package com.lgzClient.service;

import com.lgzClient.NettyClient;
import com.lgzClient.redis.TransactSqlRedisHelper;
import com.lgzClient.rpc.BranchTransactRpc;
import com.lgzClient.rpc.GlobalTransactRpc;
import com.lgzClient.types.*;
import com.lgzClient.types.sql.BranchTransaction;
import com.lgzClient.types.sql.GlobalTransaction;
import com.lgzClient.types.status.LocalStatus;
import com.lgzClient.types.status.MessageTypeEnum;
import com.lgzClient.utils.AddressUtil;
import com.lgzClient.utils.JsonUtil;
import com.lgzClient.utils.RequestUtil;
import com.lgzClient.utils.TimeUtil;
import com.lgzClient.wrapper.ConnectionWrapper;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
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
        TransactSqlRedisHelper transactSqlRedisHelper;
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
     //  @Autowired
     //  TransactSqlRedisHelper redisHelper;
        public void begin(String globalId) throws SQLException, UnknownHostException {
                LocalType localType=new LocalType(AddressUtil.buildAddress(AddressUtil.getIp()));//如果存在globalId那么就是加入到一个分布式事务中,不存在那么就生成一个新的globalId
                localType.setApplicationName(applicationName);
                if(StringUtils.hasLength(globalId)){
                    localType.setGlobalId(globalId);//设置所属的globalId
                    localType.setLocalId(branchTransactRpc.joinBranchTransaction(localType).getData().getLocalId());//加入到当前
                }

                else if(!StringUtils.hasLength(localType.getGlobalId())){//如果不存在globalId那么就开启一个新的分布式事务
                   GlobalTransaction globalTransaction=globalTransactRpc.createGlobalTransaction().getData();
                   localType.setGlobalId(globalTransaction.getGlobalId());//设置globalId
                }
                ThreadContext.globalId.set(localType.getGlobalId());//将该全局事务的id添加到当前的线程中
                ThreadContext.isDscTransaction.set(true);
                ThreadContext.localType.set(localType);
                this.buildLocalTransaction(localType);//创建一个本地事务.并将其与本地事务关联
        }
        //修改存储在redis中的本地事务状态
        public void updateStatus(LocalType localType, LocalStatus localStatus) {
             localType.setStatus(localStatus);
             BranchTransaction branchTransaction=new BranchTransaction();
             branchTransaction.setLocalId(localType.getLocalId());
             branchTransaction.setStatus(localStatus);
             branchTransactRpc.updateBranchTransactionStatus(branchTransaction);//修改分支事务的状
         }
        public void updateStatusWithNotice(LocalType localType, LocalStatus localStatus) throws InterruptedException {
           localType.setStatus(localStatus);
            BranchTransaction branchTransaction=new BranchTransaction();
            branchTransaction.setLocalId(localType.getLocalId());
            branchTransaction.setStatus(localStatus);
            branchTransactRpc.updateBranchTransactionStatusWithNotice(branchTransaction);
        }
        public void updateStatus(LocalStatus localStatus){
            LocalType localType=ThreadContext.localType.get();
            updateStatus(localType,localStatus);
        }
        public void updateStatusWithNotice(LocalStatus localStatus) throws InterruptedException {
             LocalType localType=ThreadContext.localType.get();
             updateStatusWithNotice(localType,localStatus);
        }
        private Connection buildLocalTransaction(LocalType localType) throws  SQLException//新建一个本地事务,并将其绑定到当前的线程中
        {
            ConnectionWrapper connection=new ConnectionWrapper(transactionManager.getDataSource().getConnection());
            ThreadContext.connetion.set(connection);
            connection.setAutoCommit(false);
            ConnectionHolder connectionHolder=new ConnectionHolder(connection);
            TransactionSynchronizationManager.bindResource(transactionManager.getDataSource(),connectionHolder);
            localTransactionMaps.put(localType.getLocalId(),connection);
            return connection;
        }

        public ConnectionWrapper getLocalTransaction(String localId)
        {
           return localTransactionMaps.get(localId);
        }
        //回滚事务
        public void rollBack(LocalType localType) throws SQLException {
            Connection connection=getLocalTransaction(localType.getLocalId());
            if (connection==null) return;//如果连接为null 那么说明已经被操作了
            try {
                connection.rollback();
                transactSqlRedisHelper.deleteLocalTransactionWithDeleteGlobal(localType.getGlobalId(), localType.getLocalId());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            } finally {
                connection.close();
                localTransactionMaps.remove(localType.getLocalId());
            }

        }
        //提交事务
        public void commit(LocalType localType) throws SQLException {
            ConnectionWrapper connection=getLocalTransaction(localType.getLocalId());
            if (connection==null) return;
            try {
                LocalTransactionManager.instance.deleteFromDatabase(connection, localType.getLocalId());
                connection.commit();
                transactSqlRedisHelper.deleteLocalTransactionWithDeleteGlobal(localType.getGlobalId(), localType.getLocalId());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            } finally {
                connection.close();
                localTransactionMaps.remove(localType.getLocalId());
            }
        }
        public void addLogToDatabase(TransactSqlLog transactSqlLog) throws SQLException {
        String sql = "insert into transact_sql_log(trx_id,global_id, local_id, begin_time, request_uri,application_name,server_address,logs) values ( ? , ? , ? , ?, ?, ?, ?, ?)";
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        try {
            connection = transactionManager.getDataSource().getConnection();
            preparedStatement = connection.prepareStatement(sql);
            // 设置参数
            preparedStatement.setLong(1, transactSqlLog.getTrxId());
            preparedStatement.setString(2, transactSqlLog.getGlobalId());
            preparedStatement.setString(3, transactSqlLog.getLocalId());
            preparedStatement.setTimestamp(4, new Timestamp(TimeUtil.strToDate(transactSqlLog.getBeginTime()).getTime()));
            preparedStatement.setString(5, transactSqlLog.getRequestUri());
            preparedStatement.setString(6, transactSqlLog.getApplicationName());
            preparedStatement.setString(7, transactSqlLog.getServerAddress());
            preparedStatement.setString(8, transactSqlLog.getLogs());
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
             String sql = "delete from transact_sql_log where  local_id = ?";
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