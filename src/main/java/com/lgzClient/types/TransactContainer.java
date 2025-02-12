package com.lgzClient.types;

import com.lgzClient.wrapper.ConnectionWrapper;
import lombok.Data;

import java.util.Date;
@Data
public class TransactContainer {
    public TransactContainer(ConnectionWrapper connection){
        this.connection=connection;
    }

    ConnectionWrapper connection;
    Date successTime;//本地分支事务执行成功的时间
}
