package com.lgzClient.service;

import com.lgzClient.ClientConfig;
import com.lgzClient.rpc.GlobalTransactRpc;
import com.lgzClient.types.TransactContainer;
import com.lgzClient.types.sql.service.GlobalTransaction;
import com.lgzClient.types.status.GlobalStatus;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
        },0,clientConfig.checkTimeOutInterval, TimeUnit.MILLISECONDS);
    }

    @Autowired
    GlobalTransactRpc globalTransactRpc;
    @Autowired
    LocalTransactionManager localTransactionManager;
    //超时检查
    public void checkTimeOut() throws Exception {
        ArrayList<TransactContainer> transactContainers =localTransactionManager.getUnDoTransactions(3000);
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
                localTransactionManager.rollBack(branchId);
            }
            else if(globalTransactionHashMap.get(globalId).getStatus()== GlobalStatus.success){
                localTransactionManager.commit(branchId);
            }
        }
    }

}
