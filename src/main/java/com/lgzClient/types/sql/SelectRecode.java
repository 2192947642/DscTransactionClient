package com.lgzClient.types.sql;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SelectRecode {
    private String sql;
    private SqlType sqlType=SqlType.select;
    public SelectRecode(String sql){
        this.sql=sql;
    }
}
