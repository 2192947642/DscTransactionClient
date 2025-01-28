package com.lgzClient.types;
import com.lgzClient.types.status.LocalStatus;
import com.lgzClient.utils.TimeUtil;
import lombok.Data;

import java.util.UUID;

@Data
public class LocalType {
    public static String generateGlobalId(){
        return "global"+"_"+UUID.randomUUID().toString();
    }
    public static String generateLocalId(){
        return "local"+"_"+UUID.randomUUID().toString();
    }
    public LocalType(){

    }
    public LocalType(String serverAddress){
        this.serverAddress=serverAddress;
        globalId=generateGlobalId();
        localId=generateLocalId();
        beginTime= TimeUtil.getLocalTime();
        status= LocalStatus.wait;
    }
    public LocalType(String globalId,String serverAddress){
        if(globalId!=null) this.globalId=globalId;
        else this.globalId=generateGlobalId();
        localId=generateLocalId();
        beginTime= TimeUtil.getLocalTime();
        status= LocalStatus.wait;
        this.serverAddress=serverAddress;
    }
    public Long trxId;//本地的sql事务id
    public String serverAddress;//当前事务的服务地址
    public String globalId;//全局事务的uuid 存放在redis中
    public String localId;//本地事务的uuid
    public String beginTime;//事务开始时间
    public LocalStatus status;//事务的状态

}
