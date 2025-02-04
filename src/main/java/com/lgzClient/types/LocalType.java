package com.lgzClient.types;
import com.alibaba.nacos.shaded.javax.annotation.concurrent.NotThreadSafe;
import com.lgzClient.types.status.LocalStatus;
import com.lgzClient.utils.TimeUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;
//本地事务的信息 存储在redis中
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LocalType {
    public static String generateGlobalId(){
        return "global"+"_"+UUID.randomUUID().toString();
    }
    public static String generateLocalId(){
        return "local"+"_"+UUID.randomUUID().toString();
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
    private Long trxId;//本地的sql事务id
    private String serverAddress;//当前事务的服务地址
    private String globalId;//全局事务的uuid 存放在redis中
    private String localId;//本地事务的uuid
    private String beginTime;//事务开始时间
    private LocalStatus status;//事务的状态

}
