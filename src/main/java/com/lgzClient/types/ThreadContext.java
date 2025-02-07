package com.lgzClient.types;
import com.lgzClient.types.sql.service.BranchTransaction;

import java.sql.Connection;
import java.util.ArrayList;

public class ThreadContext {
    public static final ThreadLocal<String> globalId=new ThreadLocal<String>();
    public static final ThreadLocal<Throwable> error=new ThreadLocal<Throwable>();
    public static final ThreadLocal<Connection> connection =new ThreadLocal<Connection>();
    public static final ThreadLocal<Connection> recodeConnection=new ThreadLocal<Connection>();//用于进行本地事务信息的记录
    public static final ThreadLocal<ArrayList<Object>> sqlRecodes=new ThreadLocal<ArrayList<Object>>();
    public static final ThreadLocal<BranchTransaction> branchTransaction =new ThreadLocal<BranchTransaction>();
    public static final ThreadLocal<Boolean> isDscTransaction=new ThreadLocal<Boolean>();
    public static void removeAll(){
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
