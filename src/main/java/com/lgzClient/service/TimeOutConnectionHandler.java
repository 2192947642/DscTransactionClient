package com.lgzClient.service;

import com.lgzClient.configure.ClientConfig;
import com.lgzClient.rpc.GlobalTransactRpc;
import com.lgzClient.types.TransactContainer;
import com.lgzClient.types.sql.service.BranchTransaction;
import com.lgzClient.types.sql.service.GlobalTransaction;
import com.lgzClient.types.status.GlobalStatus;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class TimeOutConnectionHandler {
    @Autowired
    ClientConfig clientConfig;
    @PostConstruct
    public void init(){
        Executors.newScheduledThreadPool(1).scheduleWithFixedDelay(()->{
            try {
                checkTimeOutWaitOthers();
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        },0,clientConfig.getCheckTimeOutIntervalWaitOthers(), TimeUnit.MILLISECONDS);
        Executors.newScheduledThreadPool(1).scheduleWithFixedDelay(()->{
            try {
                checkTimeOutPersonal();
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        },0,clientConfig.getCheckTimeOutIntervalPersonal(), TimeUnit.MILLISECONDS);

    }

    @Autowired
    GlobalTransactRpc globalTransactRpc;
    @Autowired
    LocalTransactionManager localTransactionManager;
    //本地超时检查，避免数据库连接被长时间占用
    public void checkTimeOutPersonal(){
        ArrayList<TransactContainer> transactContainers =localTransactionManager.getUnDoTransactionsPersonal(clientConfig.getCheckTimeOutIntervalPersonal());
        checkTimeOut(transactContainers);
    }
    public void checkTimeOut(ArrayList<TransactContainer> transactContainers){
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
            BranchTransaction branchTransaction=transactContainer.getBranchTransaction();
            if(globalTransactionHashMap.get(branchTransaction.getGlobalId()).getStatus() == GlobalStatus.fail){
                localTransactionManager.rollBackByThreadPoolAndWebFlux(branchTransaction);
            }
            else if(globalTransactionHashMap.get(branchTransaction.getGlobalId()).getStatus()== GlobalStatus.success){
                localTransactionManager.commitByThreadPoolAndWebFlux(branchTransaction);
            }
        }
    }
    //远程超时检查
    public void checkTimeOutWaitOthers() {
        ArrayList<TransactContainer> transactContainers =localTransactionManager.getUnDoTransactionsWaitOther(clientConfig.getCheckTimeOutIntervalWaitOthers());
        checkTimeOut(transactContainers);
    }

}
