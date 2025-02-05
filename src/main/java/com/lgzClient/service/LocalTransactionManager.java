package com.lgzClient.service;

import com.lgzClient.NettyClient;
import com.lgzClient.redis.TransactSqlRedisHelper;
import com.lgzClient.types.*;
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
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.net.UnknownHostException;
import java.sql.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public  class LocalTransactionManager {
        @Autowired
        TransactSqlRedisHelper transactSqlRedisHelper;
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
        @Autowired
        TransactSqlRedisHelper redisHelper;
        public void begin(String globalId) throws SQLException, UnknownHostException {
                HttpServletRequest httpServletRequest= RequestUtil.instance.getRequest();
                LocalType localType=new LocalType(globalId,AddressUtil.buildAddress(AddressUtil.getIp()));//如果存在globalId那么就是加入到一个分布式事务中,不存在那么就生成一个新的globalId
                ThreadContext.globalId.set(localType.getGlobalId());//将该全局事务的id添加到当前的线程中
                ThreadContext.isDscTransaction.set(true);
                ThreadContext.localType.set(localType);
                this.buildLocalTransaction(localType);//创建一个本地事务.并将其与本地事务关联
                redisHelper.addBranchTransaction(localType);//向redis中添加该本地事务
        }
        //修改存储在redis中的本地事务状态
        public void updateStatus(LocalType localType, LocalStatus localStatus) {
             localType.setStatus(localStatus);
             if (localType.getTrxId() ==null){
                 localType.setTrxId(LocalTransactionManager.getTransactionId(ThreadContext.connetion.get()));
             }
             redisHelper.updateLocalTransaction(localType);
         }
        public void updateStatusWithNotice(LocalType localType, LocalStatus localStatus){

            updateStatus(localType,localStatus);
             LocalNotice localNotice=LocalNotice.buildFronLocalType(localType);
             Message message=new Message(MessageTypeEnum.LocalNotice, JsonUtil.objToJson(localNotice), TimeUtil.getLocalTime());
             NettyClient.sendMsg(message,true);
        }
        public void updateStatus(LocalStatus localStatus){
            LocalType localType=ThreadContext.localType.get();
            updateStatus(localType,localStatus);
        }
        public void updateStatusWithNotice(LocalStatus localStatus){
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
                LocalTransactionManager.instance.deleteFromDatabase(connection, localType.getGlobalId(), localType.getLocalId());
                connection.commit();
                transactSqlRedisHelper.deleteLocalTransactionWithDeleteGlobal(localType.getGlobalId(), localType.getLocalId());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            } finally {
                connection.close();
                localTransactionMaps.remove(localType.getLocalId());
            }
        }
        public void addLogToDatabase(LocalLog localLog) throws SQLException {
        String sql = "insert into transact_sql_log(trx_id,global_id, local_id, begin_time, status, logs) values (?, ?, ?, ?, ?, ?)";
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        try {
            connection = transactionManager.getDataSource().getConnection();
            preparedStatement = connection.prepareStatement(sql);
            // 设置参数
            preparedStatement.setLong(1, localLog.getTrxId());
            preparedStatement.setString(2, localLog.getGlobalId());
            preparedStatement.setString(3, localLog.getLocalId());
            preparedStatement.setTimestamp(4, new Timestamp(TimeUtil.strToDate(localLog.getBeginTime()).getTime()));
            preparedStatement.setString(5, JsonUtil.objToJson(localLog.getStatus()));
            preparedStatement.setString(6, localLog.getLogs());
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
        public void deleteFromDatabase(ConnectionWrapper connection,String globalId,String localId) throws SQLException {
        String sql = "delete from transact_sql_log where global_id = ? and local_id = ?";
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatementWithoutWrapper(sql);
            // 设置参数
            preparedStatement.setString(1, globalId);
            preparedStatement.setString(2, localId);
            preparedStatement.executeUpdate();
        }
        catch (SQLException e) {
            // 记录日志或其他处理逻辑
            throw new SQLException("Error deleting log from database", e);
        }
    }

}