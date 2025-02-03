package com.lgzClient.types.sql;

import java.util.ArrayList;

public class InsertRecode {
    public InsertRecode(String sql){
        this.sql=sql;
    }
    public String sql;
    public SqlType sqlType=SqlType.insert;
    public ArrayList<Integer> generatedKeys=new ArrayList<>();//生成的主键
}
