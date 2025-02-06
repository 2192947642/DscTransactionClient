package com.lgzClient.types.sql;

import com.lgzClient.types.status.LocalStatus;
import lombok.Data;

@Data
public class BranchTransaction {
    private String globalId;
    private String localId;
    private String applicationName;//服务名
    private String serverAddress;
    private LocalStatus status;//本地事务的状态
    private String beginTime;//开启时间
}
