package com.lgzClient.types.sql.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UnCommitSqlLog {//本地事务日志 只有本地事务执行成功才会进行记录
    private String branchId;
    private String globalId;
    private String beginTime;
    private String requestUri;//请求的路径
    private String applicationName;//微服务的名称
    private String serverAddress;//该项目的运行地址
    private String logs;  //其内部类型为 delete/insert/select/update(Recode)
}
