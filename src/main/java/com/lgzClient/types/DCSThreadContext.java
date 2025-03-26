package com.lgzClient.types;
import com.lgzClient.rpc.BranchTransactRpc;
import com.lgzClient.types.sql.service.BranchTransaction;

import java.sql.Connection;
import java.util.ArrayList;

public class DCSThreadContext {
    //当前线程是否已经发送过
    public static final ThreadLocal<Boolean> sended=new ThreadLocal<Boolean>();
    public static final ThreadLocal<String> globalId=new ThreadLocal<String>();
    public static final ThreadLocal<Throwable> error=new ThreadLocal<Throwable>();
    public static final ThreadLocal<Connection> connection =new ThreadLocal<Connection>();
    public static final ThreadLocal<Connection> recodeConnection=new ThreadLocal<Connection>();//用于进行本地事务信息的记录
    public static final ThreadLocal<ArrayList<Object>> sqlRecodes=new ThreadLocal<ArrayList<Object>>();
    public static final ThreadLocal<BranchTransaction> branchTransaction =new ThreadLocal<BranchTransaction>();
    public static final ThreadLocal<Boolean> isDscTransaction= new ThreadLocal<>();

    public static void setGlobalId(String globalId){
        DCSThreadContext.globalId.set(globalId);
    }
    public static void init(BranchTransaction branchTransaction){
        DCSThreadContext.sqlRecodes.set(new ArrayList<>());
        DCSThreadContext.globalId.set(branchTransaction.getGlobalId());//将该全局事务的id添加到当前的线程中
        DCSThreadContext.isDscTransaction.set(true);
        DCSThreadContext.branchTransaction.set(branchTransaction);
    }
    public static void removeAll(){
        sended.remove();
        branchTransaction.remove();
        isDscTransaction.remove();
        globalId.remove();
        error.remove();
        connection.remove();
        if(sqlRecodes.get()!=null)
        sqlRecodes.get().clear();
        sqlRecodes.remove();
        recodeConnection.remove();
    }
}
