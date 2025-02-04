package com.lgzClient.types;

import com.lgzClient.types.sql.*;
import com.lgzClient.types.status.LocalStatus;
import com.lgzClient.utils.BeanMapUtil;
import com.lgzClient.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LocalLog {
    public static LocalLog buildFromLocalType(LocalType localType){
        LocalLog localLog=new LocalLog();
        localLog.trxId= localType.getTrxId();
        localLog.localId= localType.getLocalId();
        localLog.globalId= localType.getGlobalId();
        localLog.beginTime= localType.getBeginTime();
        localLog.status= localType.getStatus();
        localLog.logs= JsonUtil.objToJson(ThreadContext.sqlRecodes.get());
        return localLog;
    };
    private String applicationName;//微服务的名称
    private String serverAddress;//该项目的运行地址
    private String requestUri;//请求的路径
    private Long trxId;
    private String localId;
    private String globalId;
    private String beginTime;
    private LocalStatus status;
    private String logs;
    //其内部类型为 delete/insert/select/update(Recode)
    public ArrayList<Object> getRecodesByLogs() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        ArrayList<Object> recodes=new ArrayList<>();
        ArrayList<String> logs=JsonUtil.jsonToObject(this.logs,ArrayList.class);
        for(String log:logs){
            HashMap<String,String> hashMap=JsonUtil.jsonToObject(log,HashMap.class);
            if(hashMap.get("sqlType").equals(SqlType.insert.name())){
              InsertRecode insertRecode=BeanMapUtil.mapToBean(hashMap, InsertRecode.class);
              recodes.add(insertRecode);
            }else if(hashMap.get("sqlType").equals(SqlType.delete.name())){
                DeleteRecode deleteRecode=BeanMapUtil.mapToBean(hashMap, DeleteRecode.class);
                recodes.add(deleteRecode);
            }else if(hashMap.get("sqlType").equals(SqlType.update.name())){
                UpdateRecode updateRecode=BeanMapUtil.mapToBean(hashMap, UpdateRecode.class);
                recodes.add(updateRecode);
            }else if(hashMap.get("sqlType").equals(SqlType.select.name())){
                SelectRecode selectRecode=BeanMapUtil.mapToBean(hashMap, SelectRecode.class);
                recodes.add(selectRecode);
            }
        }
        return recodes;
    }
}
