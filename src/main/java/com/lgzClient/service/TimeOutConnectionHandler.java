package com.lgzClient.service;

import com.lgzClient.ClientConfig;
import com.lgzClient.rpc.GlobalTransactRpc;
import com.lgzClient.types.TransactContainer;
import com.lgzClient.types.sql.service.GlobalTransaction;
import com.lgzClient.types.status.GlobalStatus;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
public class TimeOutConnectionHandler {
    @Autowired
    ClientConfig clientConfig;
    @PostConstruct
    public void init(){
        Executors.newScheduledThreadPool(1).scheduleWithFixedDelay(()->{
            try {
                checkTimeOut();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        },0,clientConfig.checkTimeOutIntervalWaitOthers, TimeUnit.MILLISECONDS);
        Executors.newScheduledThreadPool(1).scheduleWithFixedDelay(()->{
            try {
                checkTimeOutPersonal();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        },0,clientConfig.checkTimeOutIntervalPersonal, TimeUnit.MILLISECONDS);

    }

    @Autowired
    GlobalTransactRpc globalTransactRpc;
    @Autowired
    LocalTransactionManager localTransactionManager;
    //本地超时检查，避免数据库连接被长时间占用
    public void checkTimeOutPersonal(){
        ArrayList<TransactContainer> transactContainers =localTransactionManager.getUnDoTransactionsPersonal(clientConfig.checkTimeOutIntervalPersonal);
        if(transactContainers.size()==0) return;
        for(TransactContainer transactContainer : transactContainers){
           localTransactionManager.rollBackByThreadPoolAndWebFlux(transactContainer.getBranchTransaction().getBranchId());
        }
    }
    //远程超时检查
    public void checkTimeOut() {
        ArrayList<TransactContainer> transactContainers =localTransactionManager.getUnDoTransactionsWaitOther(clientConfig.checkTimeOutIntervalWaitOthers);
        if(transactContainers.size()==0) return;
        ArrayList<String> globalIds=new ArrayList<>();
        for (TransactContainer transactContainer : transactContainers) {
            String globalId=transactContainer.getBranchTransaction().getGlobalId();
            globalIds.add(globalId);
        }
        ArrayList<GlobalTransaction> globalTransactions=globalTransactRpc.getGlobalTransactions(globalIds).getData();
        HashMap<String ,GlobalTransaction> globalTransactionHashMap=new HashMap<>();
        for (GlobalTransaction globalTransaction : globalTransactions) {
            globalTransactionHashMap.put(globalTransaction.getGlobalId(),globalTransaction);
        }
        for (TransactContainer transactContainer : transactContainers){
            String globalId=transactContainer.getBranchTransaction().getGlobalId();
            String branchId=transactContainer.getBranchTransaction().getBranchId();
            if(globalTransactionHashMap.get(globalId).getStatus() == GlobalStatus.fail){
                localTransactionManager.rollBackByThreadPoolAndWebFlux(branchId);
            }
            else if(globalTransactionHashMap.get(globalId).getStatus()== GlobalStatus.success){
                localTransactionManager.commitByThreadPoolAndWebFlux(branchId);
            }
        }
    }

}
