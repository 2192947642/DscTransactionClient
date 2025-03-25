package com.lgzClient.service;

import com.lgzClient.configure.ClientConfig;
import com.lgzClient.annotations.DCSTransaction;
import com.lgzClient.exceptions.DcsTransactionError;
import com.lgzClient.rpc.BranchTransactRpc;
import com.lgzClient.rpc.GlobalTransactRpc;
import com.lgzClient.rpc.webflux.BranchTransactRpcWebFlux;
import com.lgzClient.types.BothTransaction;
import com.lgzClient.types.DCSThreadContext;
import com.lgzClient.types.TransactContainer;
import com.lgzClient.types.sql.client.NotDoneSqlLog;
import com.lgzClient.types.sql.service.BranchTransaction;
import com.lgzClient.types.status.BranchStatus;
import com.lgzClient.utils.BranchTransactionUtil;
import com.lgzClient.utils.NotDoneSqlLogUtil;
import com.lgzClient.utils.TimeUtil;
import com.lgzClient.wrapper.ConnectionWrapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

@Slf4j
@Component
public class LocalTransactionManager {

    private Semaphore connectionSemaphore;
    private static ExecutorService dbExecutor = Executors.newFixedThreadPool(10);
    @Autowired
    ClientConfig clientConfig;
    @Autowired
    private NotDoneSqlLogUtil notDoneSqlLogUtil;
    @Autowired
    BranchTransactRpcWebFlux branchTransactRpcWebFlux;
    @Autowired
    private GlobalTransactRpc globalTransactRpc;
    @Autowired
    private BranchTransactRpc branchTransactRpc;
    @Autowired
    private BranchTransactionUtil branchTransactionUtil;

    @Autowired
    private DataSourceTransactionManager transactionManager;
    public static LocalTransactionManager instance;

    public void updateLocalSuccessTime(String branchId) {//修改本地事务的successTime
        localTransactionMaps.get(branchId).setSuccessTime(System.currentTimeMillis());
    }

    @PostConstruct
    public void init() {
        instance = this;
        this.connectionSemaphore = new Semaphore(clientConfig.getMaxHandlerConnection());
    }

    private static final ConcurrentHashMap<String, TransactContainer> localTransactionMaps = new ConcurrentHashMap<>();

    //获得个人执行了n毫秒后还没有执行完的事务

    public ArrayList<TransactContainer> getUnDoTransactionsPersonal(long millisecond) {
        ArrayList<TransactContainer> transactContainers = new ArrayList<>();
        localTransactionMaps.forEach((k, v) -> {
            transactContainers.add(v);
        });//将所有已经成功的本地事务加入到list中
        transactContainers.sort((o1, o2) -> o1.getBeginTime().compareTo(o2.getBeginTime()));
        Long nowTime = TimeUtil.getNowTime();
        ArrayList<TransactContainer> returnList = new ArrayList<>();
        for (TransactContainer transactContainer : transactContainers) {
            Long successTime = transactContainer.getSuccessTime();
            if (nowTime - successTime >= millisecond) {
                returnList.add(transactContainer);
            } else {
                break;
            }
        }
        return returnList;
    }

    //获得 n毫秒之前完成（等待其他分支事务状态）但是没有进行提交或者回滚的本地事务

    public ArrayList<TransactContainer> getUnDoTransactionsWaitOther(long millisecond) {
        ArrayList<TransactContainer> transactContainers = new ArrayList<>();
        localTransactionMaps.forEach((k, v) -> {
            if (v.getSuccessTime() != null) {
                transactContainers.add(v);
            }
        });//将所有已经成功的本地事务加入到list中
        transactContainers.sort((o1, o2) -> o1.getSuccessTime().compareTo(o2.getSuccessTime()));
        Long nowTime = TimeUtil.getNowTime();
        ArrayList<TransactContainer> returnList = new ArrayList<>();
        for (TransactContainer transactContainer : transactContainers) {
            Long successTime = transactContainer.getSuccessTime();
            if (nowTime - successTime >= millisecond) {
                returnList.add(transactContainer);
            } else {
                break;
            }
        }
        return returnList;
    }

    //分布式连接开启
    public void begin(String globalId, DCSTransaction dcsTransaction) throws SQLException, UnknownHostException, InterruptedException {

        BranchTransaction branchTransaction = branchTransactionUtil.buildDefaultTransaction();
        if (StringUtils.hasLength(globalId)) {//如果存在globalId那么就加入到当前的事务中
            branchTransaction.setGlobalId(globalId);//设置所属的globalId
            branchTransaction.setBranchId(branchTransactRpc.joinBranchTransaction(branchTransaction).getData().getBranchId());//加入到当前的事务中
        } else {//如果不存在globalId那么就开启并加入一个新的分布式事务
            BothTransaction bothTransaction = globalTransactRpc.createAndJoinGlobalTransaction(dcsTransaction.timeout(), branchTransaction).getData();
            branchTransaction.setGlobalId(bothTransaction.getGlobalTransaction().getGlobalId());//设置globalId
            branchTransaction.setBranchId(bothTransaction.getBranchTransaction().getBranchId());//设置branchId
        }
        DCSThreadContext.init(branchTransaction);//对ThreadLocal中的数据进行初始化
        NotDoneSqlLog notDoneSqlLog = notDoneSqlLogUtil.buildNotDoneLogByThread();//建立localLog
        notDoneSqlLogUtil.addLogToDatabase(notDoneSqlLog);
        this.buildLocalTransaction(branchTransaction);//创建一个本地事务.并将其与本地事务关联
    }

    //修改存储在服务端中的本地事务状态
    public void updateStatus(BranchTransaction branchTransaction, BranchStatus branchStatus) {
        branchTransaction.setStatus(branchStatus);
        branchTransaction.setStatus(branchStatus);
        branchTransactRpc.updateBranchTransactionStatus(branchTransaction);//修改存储在远程的分支事务状态
    }

    public void updateStatusWithNotice(BranchTransaction branchTransaction, BranchStatus branchStatus) {
        branchTransaction.setStatus(branchStatus);
        branchTransactRpc.updateBranchTransactionStatusWithNotice(branchTransaction);
    }

    private Connection buildLocalTransaction(BranchTransaction branchTransaction) throws SQLException, InterruptedException//新建一个本地事务,并将其绑定到当前的线程中
    {
        connectionSemaphore.acquire();//申请一个连接资源·········
        ConnectionWrapper connection = new ConnectionWrapper(transactionManager.getDataSource().getConnection());
        DCSThreadContext.connection.set(connection);
        connection.setAutoCommit(false);
        ConnectionHolder connectionHolder = new ConnectionHolder(connection);
        TransactionSynchronizationManager.bindResource(transactionManager.getDataSource(), connectionHolder);
        localTransactionMaps.put(branchTransaction.getBranchId(), new TransactContainer(connection, branchTransaction, System.currentTimeMillis()));
        return connection;
    }

    public ConnectionWrapper getConnection(String branchId) {
        TransactContainer transactContainer = localTransactionMaps.get(branchId);
        if (transactContainer == null) return null;
        return transactContainer.getConnection();
    }

    public void removeLocalTransaction(String branchId) {
        localTransactionMaps.remove(branchId);
    }

    //回滚事务
    public void rollBack(String branchId, Boolean useFlux, Boolean notice) {
        ConnectionWrapper connection = getConnection(branchId);
        if (connection == null) return;
        synchronized (connection) {
            try {
                if(connection.isClosed()) return;
                connection.rollback();
                connection.setAutoCommit(true);
                notDoneSqlLogUtil.deleteUnDoLogFromDatabase(connection, branchId);//从数据库中删除该未完成的事务
                BranchTransaction branchTransaction = BranchTransaction.builder().globalId(DCSThreadContext.globalId.get()).branchId(branchId).status(BranchStatus.rollback).build();
                if (!notice) {
                    if (!useFlux) branchTransactRpc.updateBranchTransactionStatus(branchTransaction);//更新服务端的分支事务状态 为回滚
                    else branchTransactRpcWebFlux.updateBranchTransactionStatus(branchTransaction).subscribe();
                } else if (notice) {
                    if (!useFlux)
                        branchTransactRpc.updateBranchTransactionStatusWithNotice(branchTransaction);//更新服务端的分支事务状态 为回滚
                    else
                        branchTransactRpcWebFlux.updateBranchTransactionStatusWithNotice(branchTransaction).subscribe();
                }
            } catch (SQLException sqlException) {
                throw new DcsTransactionError(sqlException.getMessage());
            } finally {
                try {
                    if (!connection.isClosed()){//如果没有关闭
                        connection.close();//关闭数据库连接
                    }
                } catch (SQLException sqlException) {
                    throw new DcsTransactionError(sqlException.getMessage());
                }finally {
                    removeLocalTransaction(branchId);//移除
                    connectionSemaphore.release();//释放资源
                }
            }
        }
    }

    //回滚事务 通过线程池和flux 不会通知服务器
    public void rollBackByThreadPoolAndWebFlux(String branchId) {
        dbExecutor.submit(() -> {
            rollBack(branchId, true, false);
        });
    }

    public void commitByThreadPoolAndWebFlux(String branchId) {
        dbExecutor.submit(() -> {
            commit(branchId, true);
        });
    }

    //提交事务
    public void commit(String branchId, Boolean useFlux) {
        ConnectionWrapper connection = getConnection(branchId);
        if (connection == null) return;
        synchronized (connection) {
            try {
                if (connection.isClosed()) {
                    return;
                };
                notDoneSqlLogUtil.deleteUnDoLogFromDatabase(connection, branchId);//从数据库中删除该未完成的事务
                connection.commit();//提交事务 提交后同时也删除了本地事务的记录 防止了事务多次提交
                BranchTransaction branchTransaction = BranchTransaction.builder().branchId(branchId).status(BranchStatus.commit).build();
                if (!useFlux) branchTransactRpc.updateBranchTransactionStatus(branchTransaction);//更新服务端的分支事务状态
                else branchTransactRpcWebFlux.updateBranchTransactionStatus(branchTransaction).subscribe();
            } catch (SQLException e) {
                throw new DcsTransactionError(e.getMessage());
            } finally {
                try {
                    if (!connection.isClosed()) connection.close();
                } catch (SQLException e) {
                    throw new DcsTransactionError(e.getMessage());
                } finally {
                    connectionSemaphore.release();
                    removeLocalTransaction(branchId);
                }
            }
        }

    }

}

