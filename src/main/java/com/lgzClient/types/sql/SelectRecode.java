package com.lgzClient.types.sql;

public class SelectRecode {
    public String sql;
    public SqlType sqlType=SqlType.select;
    public SelectRecode(String sql){
        this.sql=sql;
    }
}
