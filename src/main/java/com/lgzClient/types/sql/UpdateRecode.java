package com.lgzClient.types.sql;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRecode {
    public UpdateRecode(String sql){
        this.sql=sql;
    }
    private SqlType sqlType=SqlType.update;
    private String before;
    private String after;
    private String sql;
}
