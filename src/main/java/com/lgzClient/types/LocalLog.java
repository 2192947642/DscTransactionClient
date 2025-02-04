package com.lgzClient.types;

import com.lgzClient.types.status.LocalStatus;
import com.lgzClient.utils.JsonUtil;
import lombok.Data;

import java.util.ArrayList;
@Data
public class LocalLog {
    public static LocalLog buildFromLocalType(LocalType localType){
        LocalLog localLog=new LocalLog();
        localLog.trxId=localType.trxId;
        localLog.localId=localType.localId;
        localLog.globalId=localType.globalId;
        localLog.beginTime=localType.beginTime;
        localLog.status=localType.status;
        localLog.logs= JsonUtil.objToJson(ThreadContext.sqlRecodes.get());
        return localLog;
    };
    public String applicationName;//微服务的名称
    public String serverAddress;//该项目的运行地址
    public String requestUri;//请求的路径
    public Long trxId;
    public String localId;
    public String globalId;
    public String beginTime;
    public LocalStatus status;
    public String logs;
}
