package com.lgzClient.types.sql.recode;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeleteRecode   {
    public DeleteRecode(String sql){
        this.sql=sql;
    }
    private String sql;
    private String before;
    private SqlType sqlType=SqlType.delete;
}
