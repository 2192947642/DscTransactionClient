package com.lgzClient.types.sql.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotDoneSqlLog {//本地事务日志 只有本地事务执行成功才会进行记录
    private String branchId;//分支事务的id
    private String globalId;//事务id
    private String beginTime;//开始时间
    private String requestUri;//请求的路径
    private String applicationName;//微服务的名称
    private String serverAddress;//该项目的运行地址
    private String logs;  //其内部类型为 delete/insert/select/update(Recode)
}
