package com.lgzClient.types.sql.service;

import com.lgzClient.types.status.BranchStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchTransaction {
    private String globalId;
    private String branchId;
    private String applicationName;//服务名
    private String serverAddress;
    private BranchStatus status;//本地事务的状态
    private String beginTime;//开启时间
}
