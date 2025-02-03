package com.lgzClient.types.sql;

public class UpdateRecode {
    public UpdateRecode(String sql){
        this.sql=sql;
    }
    public SqlType sqlType=SqlType.update;
    public String before;
    public String after;
    public String sql;
}
