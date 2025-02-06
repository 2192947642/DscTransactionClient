package com.lgzClient.utils;

import com.lgzClient.types.TransactSqlLog;
import com.lgzClient.types.LocalType;
import com.lgzClient.types.ThreadContext;
import com.lgzClient.types.sql.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;

@Component
public class LocalLogUtil {
    public TransactSqlLog buildLocalLogFromLocalType(LocalType localType){
        TransactSqlLog transactSqlLog =new TransactSqlLog();
        transactSqlLog.setTrxId(localType.getTrxId());
        transactSqlLog.setLocalId(localType.getLocalId());
        transactSqlLog.setGlobalId(localType.getGlobalId());
        transactSqlLog.setBeginTime(localType.getBeginTime());
        transactSqlLog.setApplicationName(localType.getApplicationName());
        transactSqlLog.setServerAddress(localType.getServerAddress());
        return transactSqlLog;
    };
    public TransactSqlLog buildLocalLogByThread(){
        TransactSqlLog transactSqlLog =buildLocalLogFromLocalType(ThreadContext.localType.get());
        String requestUri= RequestUtil.instance.getRequest().getRequestURI();//请求的接口路径
        transactSqlLog.setRequestUri(requestUri);
        transactSqlLog.setLogs(JsonUtil.objToJson(ThreadContext.sqlRecodes.get()));//记录的sql日志
        return transactSqlLog;
    }

    public ArrayList<Object> getRecodesByLocalLog(TransactSqlLog transactSqlLog) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        ArrayList<Object> recodes=new ArrayList<>();
        ArrayList<String> logs=JsonUtil.jsonToObject(transactSqlLog.getLocalId(),ArrayList.class);
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
