package com.lgzClient.types;

import java.sql.Connection;

public class ThreadContext {
    public static final ThreadLocal<String> globalId=new ThreadLocal<String>();
    public static final ThreadLocal<Throwable> error=new ThreadLocal<Throwable>();
    public static final ThreadLocal<Connection> connetion=new ThreadLocal<Connection>();
    public static void removeAll(){
        globalId.remove();
        error.remove();
        connetion.remove();
    }
}
