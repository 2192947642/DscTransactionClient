package com.lgzClient.types;

import com.lgzClient.types.sql.service.BranchTransaction;
import com.lgzClient.wrapper.ConnectionWrapper;
import lombok.Data;

import java.util.Date;
@Data
public class TransactContainer {
    public TransactContainer(ConnectionWrapper connection,BranchTransaction branchTransaction){
        this.connection=connection;
        this.branchTransaction=branchTransaction;
    }
    BranchTransaction branchTransaction;//本地分支事务
    ConnectionWrapper connection;
    Date successTime;//本地分支事务执行成功的时间
}
