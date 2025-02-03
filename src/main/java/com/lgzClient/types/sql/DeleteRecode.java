package com.lgzClient.types.sql;

public class DeleteRecode {
    public DeleteRecode(String sql){
        this.sql=sql;
    }
    public String sql;
    public String before;
    public SqlType sqlType=SqlType.delete;
}
