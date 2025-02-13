package com.lgzClient.service;

import com.lgzClient.annotations.DCSTransaction;
import com.lgzClient.rpc.BranchTransactRpc;
import com.lgzClient.rpc.GlobalTransactRpc;
import com.lgzClient.types.ThreadContext;
import com.lgzClient.types.TransactContainer;
import com.lgzClient.types.sql.client.NotDoneSqlLog;
import com.lgzClient.types.sql.service.BranchTransaction;
import com.lgzClient.types.sql.service.GlobalTransaction;
import com.lgzClient.types.status.BranchStatus;
import com.lgzClient.utils.BranchTransactionUtil;
import com.lgzClient.utils.NotDoneSqlLogUtil;
import com.lgzClient.utils.TimeUtil;
import com.lgzClient.wrapper.ConnectionWrapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

@Component
public  class LocalTransactionManager {
        @Autowired
        private NotDoneSqlLogUtil notDoneSqlLogUtil;
        @Autowired
        private GlobalTransactRpc globalTransactRpc;
        @Autowired
        private BranchTransactRpc branchTransactRpc;
        @Autowired
        private BranchTransactionUtil branchTransactionUtil;

        @Autowired
        private DataSourceTransactionManager transactionManager;
        public static LocalTransactionManager instance;
        public void updateLocalSuccessTime(String branchId){//修改本地事务的successTime
            localTransactionMaps.get(branchId).setSuccessTime(new Date());
        }
        @PostConstruct
        public void init()
        {
            instance=this;
        }
        private static final ConcurrentHashMap<String, TransactContainer> localTransactionMaps=new ConcurrentHashMap<>();

        //获得 n毫秒之前完成但是没有进行提交或者回滚的本地事务
        public ArrayList<TransactContainer> getUnDoTransactions(long millisecond){
            ArrayList<TransactContainer> transactContainers=new ArrayList<>();
            localTransactionMaps.forEach((k,v)->{
                if(v.getSuccessTime()!=null){
                    transactContainers.add(v);
                }
            });//将所有已经成功的本地事务加入到list中
            transactContainers.sort((o1, o2) -> o1.getSuccessTime().compareTo(o2.getSuccessTime()));
            Long nowTime=TimeUtil.getNowTime();
            ArrayList<TransactContainer> returnList=new ArrayList<>();
            for (TransactContainer transactContainer :  transactContainers){
                Date successTime=transactContainer.getSuccessTime();
                if(nowTime-successTime.getTime()>=millisecond){
                    returnList.add(transactContainer);
                }else{
                    break;
                }
            }
            return returnList;
        }
        public void begin(String globalId, DCSTransaction dcsTransaction) throws SQLException, UnknownHostException {
                BranchTransaction branchTransaction=branchTransactionUtil.buildDefaultTransaction();
                if(StringUtils.hasLength(globalId)){
                    branchTransaction.setGlobalId(globalId);//设置所属的globalId
                }
                else if(!StringUtils.hasLength(branchTransaction.getGlobalId())){//如果不存在globalId那么就开启一个新的分布式事务
                    GlobalTransaction globalTransaction=globalTransactRpc.createGlobalTransaction(dcsTransaction.timeout()).getData();
                    branchTransaction.setGlobalId(globalTransaction.getGlobalId());//设置globalId
                }
                branchTransaction.setBranchId(branchTransactRpc.joinBranchTransaction(branchTransaction).getData().getBranchId());//加入到当前的事务中
                ThreadContext.sqlRecodes.set(new ArrayList<>());
                ThreadContext.globalId.set(branchTransaction.getGlobalId());//将该全局事务的id添加到当前的线程中
                ThreadContext.isDscTransaction.set(true);
                ThreadContext.branchTransaction.set(branchTransaction);
                NotDoneSqlLog notDoneSqlLog = notDoneSqlLogUtil.buildUndoLogByThread();//建立localLog
                this.addLogToDatabase(notDoneSqlLog);
                this.buildLocalTransaction(branchTransaction);//创建一个本地事务.并将其与本地事务关联
        }
        //修改存储在服务端中的本地事务状态
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
            localTransactionMaps.put(branchTransaction.getBranchId(),new TransactContainer(connection,branchTransaction));
            return connection;
        }
        public ConnectionWrapper getLocalTransaction(String branchId)
        {
            TransactContainer transactContainer=localTransactionMaps.get(branchId);
            if(transactContainer==null) return null;
            return  transactContainer.getConnection();
        }
        public void removeLocalTransaction(String branchId){
            localTransactionMaps.remove(branchId);
        }
        //回滚事务
        public void rollBack(String branchId) throws SQLException {
            ConnectionWrapper connection=getLocalTransaction(branchId);
            if (connection==null) return;
            if (connection.isClosed()){
                localTransactionMaps.remove(branchId);
                return;
            };
            try {
                connection.rollback();
                connection.setAutoCommit(true);
                LocalTransactionManager.instance.deleteUnDoLogFromDatabase(connection,branchId);//从数据库中删除该未完成的事务
                BranchTransaction branchTransaction= BranchTransaction.builder().branchId(branchId).status(BranchStatus.rollback).build();
                branchTransactRpc.updateBranchTransactionStatus(branchTransaction);//更新服务端的分支事务状态 为回滚
            } catch (SQLException e) {
                throw new RuntimeException(e);
            } finally {
                connection.close();
                localTransactionMaps.remove(branchId);
            }

        }
        //提交事务
        public void commit(String branchId) throws SQLException {
            ConnectionWrapper connection=getLocalTransaction(branchId);
            if (connection==null) return;
            if (connection.isClosed()){
                localTransactionMaps.remove(branchId);
                return;
            };
            try {
                LocalTransactionManager.instance.deleteUnDoLogFromDatabase(connection,branchId);//从数据库中删除该未完成的事务
                connection.commit();//提交事务 与上一个为同一事务 确保原子性
                BranchTransaction branchTransaction= BranchTransaction.builder().branchId(branchId).status(BranchStatus.commit).build();
                branchTransactRpc.updateBranchTransactionStatus(branchTransaction);//更新服务端的分支事务状态
            } catch (SQLException e) {
                throw new RuntimeException(e);
            } finally {
                connection.close();
                localTransactionMaps.remove(branchId);
            }
        }
        public void updateLogOfDBS(NotDoneSqlLog notDoneSqlLog){
            String sql = "update not_done_sql_log set logs= ? where branch_id= ?";
            Connection connection = null;
            PreparedStatement preparedStatement = null;
            try {
                connection = transactionManager.getDataSource().getConnection();
                preparedStatement = connection.prepareStatement(sql);
                //设置参数
                preparedStatement.setString(1, notDoneSqlLog.getLogs());
                preparedStatement.setString(2, notDoneSqlLog.getBranchId());
                preparedStatement.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            finally {
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


        public void addLogToDatabase(NotDoneSqlLog notDoneSqlLog) throws SQLException {
            String sql = "insert into not_done_sql_log(global_id, branch_id, begin_time, request_uri,application_name,server_address,logs) values (?, ? , ? , ?, ?, ?, ?)";
            Connection connection = null;
            PreparedStatement preparedStatement = null;
            try {
                connection = transactionManager.getDataSource().getConnection();
                preparedStatement = connection.prepareStatement(sql);
                // 设置参数
                preparedStatement.setString(1, notDoneSqlLog.getGlobalId());
                preparedStatement.setString(2, notDoneSqlLog.getBranchId());
                preparedStatement.setTimestamp(3, new Timestamp(TimeUtil.strToDate(notDoneSqlLog.getBeginTime()).getTime()));
                preparedStatement.setString(4, notDoneSqlLog.getRequestUri());
                preparedStatement.setString(5, notDoneSqlLog.getApplicationName());
                preparedStatement.setString(6, notDoneSqlLog.getServerAddress());
                preparedStatement.setString(7, notDoneSqlLog.getLogs());
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
        public void deleteUnDoLogFromDatabase(ConnectionWrapper connection,String branchId) throws SQLException {
             String sql = "delete from not_done_sql_log where  branch_id = ?";
             PreparedStatement preparedStatement = null;
             try {
                 preparedStatement = connection.prepareStatementWithoutWrapper(sql);
                 // 设置参数
                 preparedStatement.setString(1, branchId);
                 preparedStatement.executeUpdate();
             }
             catch (SQLException e) {
                 // 记录日志或其他处理逻辑
                 throw new SQLException("Error deleting log from database", e);
             }
        }
}

