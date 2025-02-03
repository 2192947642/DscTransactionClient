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

    public Long trxId;
    public String localId;
    public String globalId;
    public String beginTime;
    public LocalStatus status;
    public String logs;
}
