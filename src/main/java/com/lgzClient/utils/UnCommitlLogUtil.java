package com.lgzClient.utils;

import com.lgzClient.types.sql.client.UnCommitSqlLog;

import com.lgzClient.types.ThreadContext;
import com.lgzClient.types.sql.recode.*;
import com.lgzClient.types.sql.service.BranchTransaction;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;

@Component
public class UnCommitlLogUtil {

    public UnCommitSqlLog buildUndoSqlLogFromLocalBranchTransaction(BranchTransaction branchTransaction){
        UnCommitSqlLog unCommitSqlLog =new UnCommitSqlLog();
        unCommitSqlLog.setBranchId(branchTransaction.getBranchId());
        unCommitSqlLog.setGlobalId(branchTransaction.getGlobalId());
        unCommitSqlLog.setBeginTime(branchTransaction.getBeginTime());
        unCommitSqlLog.setApplicationName(branchTransaction.getApplicationName());
        unCommitSqlLog.setServerAddress(branchTransaction.getServerAddress());
        return unCommitSqlLog;
    };
    public UnCommitSqlLog buildUndoLogByThread(){
        UnCommitSqlLog unCommitSqlLog = buildUndoSqlLogFromLocalBranchTransaction(ThreadContext.branchTransaction.get());
        String requestUri= RequestUtil.instance.getRequest().getRequestURI();//请求的接口路径
        unCommitSqlLog.setRequestUri(requestUri);
        unCommitSqlLog.setLogs(JsonUtil.objToJson(ThreadContext.sqlRecodes.get()));//记录的sql日志
        return unCommitSqlLog;
    }

    public ArrayList<Object> getRecodesByUndoLog(UnCommitSqlLog unCommitSqlLog) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        ArrayList<Object> recodes=new ArrayList<>();
        ArrayList<String> logs=JsonUtil.jsonToObject(unCommitSqlLog.getBranchId(),ArrayList.class);
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
