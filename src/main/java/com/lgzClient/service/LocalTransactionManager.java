package com.lgzClient.service;

import com.lgzClient.redis.MsgReceiveHelper;
import com.lgzClient.redis.TransactSqlRedisHelper;
import com.lgzClient.types.LocalType;
import com.lgzClient.types.ThreadContext;
import com.lgzClient.types.status.LocalStatus;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
        private static final ConcurrentHashMap<String, Connection> localTransactionMaps=new ConcurrentHashMap<>();

        public Connection buildLocalTransaction(LocalType localType) throws  SQLException//新建一个本地事务,并将其绑定到当前的线程中
        {
            Connection connection=transactionManager.getDataSource().getConnection();
            ThreadContext.connetion.set(connection);
            connection.setAutoCommit(false);
            ConnectionHolder connectionHolder=new ConnectionHolder(connection);
            TransactionSynchronizationManager.bindResource(transactionManager.getDataSource(),connectionHolder);
            localTransactionMaps.put(localType.localId,connection);
            return connection;
        }

        public Connection getLocalTransaction(String localId)
        {
           return localTransactionMaps.get(localId);
        }
        //回滚事务
        public void rollBack(LocalType localType) throws SQLException {
            Connection connection=getLocalTransaction(localType.localId);
            if (connection==null) return;//如果连接为null 那么说明已经被操作了
            try {
                connection.rollback();
              // localType.status=LocalStatus.rollback;
              // transactSqlRedisHelper.updateLocalTransaction(localType);
                transactSqlRedisHelper.deleteLocalTransactionWithDeleteGlobal(localType.globalId,localType.localId);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            } finally {
                connection.close();
                localTransactionMaps.remove(localType.localId);
            }

        }
        //提交事务
        public void commit(LocalType localType) throws SQLException {
            Connection connection=getLocalTransaction(localType.localId);
            if (connection==null) return;
            try {
                connection.commit();
               // localType.status= LocalStatus.commit;
               // transactSqlRedisHelper.updateLocalTransaction(localType);//修改status为成功
                transactSqlRedisHelper.deleteLocalTransactionWithDeleteGlobal(localType.globalId,localType.localId);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            } finally {
                connection.close();
                localTransactionMaps.remove(localType.localId);
            }
        }


    }