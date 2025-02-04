package com.lgzClient.types.sql;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InsertRecode {
    public InsertRecode(String sql){
        this.sql=sql;
    }
    private String sql;
    private SqlType sqlType=SqlType.insert;
    private ArrayList<Integer> generatedKeys=new ArrayList<>();//生成的主键
}
