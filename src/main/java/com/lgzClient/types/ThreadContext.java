package com.lgzClient.types;
import java.sql.Connection;
import java.util.ArrayList;

public class ThreadContext {
    public static final ThreadLocal<String> globalId=new ThreadLocal<String>();
    public static final ThreadLocal<Throwable> error=new ThreadLocal<Throwable>();
    public static final ThreadLocal<Connection> connetion=new ThreadLocal<Connection>();
    public static final ThreadLocal<Connection> recodeConnection=new ThreadLocal<Connection>();//用于进行本地事务信息的记录
    public static final ThreadLocal<ArrayList<Object>> sqlRecodes=new ThreadLocal<ArrayList<Object>>();
    public static final ThreadLocal<LocalType> localType=new ThreadLocal<LocalType>();
    public static final ThreadLocal<Boolean> isDscTransaction=new ThreadLocal<Boolean>();
    public static void removeAll(){
        localType.remove();
        isDscTransaction.remove();
        globalId.remove();
        error.remove();
        connetion.remove();
        if(sqlRecodes.get()!=null)
        sqlRecodes.get().clear();
        sqlRecodes.remove();
        recodeConnection.remove();
    }
}
