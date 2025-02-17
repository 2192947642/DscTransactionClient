package com.lgzClient.utils;

import com.lgzClient.types.DCSThreadContext;
import com.lgzClient.types.sql.client.NotDoneSqlLog;
import com.lgzClient.types.sql.recode.*;
import com.lgzClient.types.sql.service.BranchTransaction;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;

@Component
public class NotDoneSqlLogUtil {

    public NotDoneSqlLog buildUndoSqlLogFromLocalBranchTransaction(BranchTransaction branchTransaction){
        NotDoneSqlLog notDoneSqlLog =new NotDoneSqlLog();
        notDoneSqlLog.setBranchId(branchTransaction.getBranchId());
        notDoneSqlLog.setGlobalId(branchTransaction.getGlobalId());
        notDoneSqlLog.setBeginTime(branchTransaction.getBeginTime());
        notDoneSqlLog.setApplicationName(branchTransaction.getApplicationName());
        notDoneSqlLog.setServerAddress(branchTransaction.getServerAddress());
        return notDoneSqlLog;
    };
    public NotDoneSqlLog buildNotDoneLogByThread(){
        NotDoneSqlLog notDoneSqlLog = buildUndoSqlLogFromLocalBranchTransaction(DCSThreadContext.branchTransaction.get());
        String requestUri= RequestUtil.instance.getRequest().getRequestURI();//请求的接口路径
        notDoneSqlLog.setRequestUri(requestUri);
        notDoneSqlLog.setLogs(JsonUtil.objToJson(DCSThreadContext.sqlRecodes.get()));//记录的sql日志
        return notDoneSqlLog;
    }

    public ArrayList<Object> getRecodesByUndoLog(NotDoneSqlLog notDoneSqlLog) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        ArrayList<Object> recodes=new ArrayList<>();
        ArrayList<String> logs=JsonUtil.jsonToObject(notDoneSqlLog.getBranchId(),ArrayList.class);
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
