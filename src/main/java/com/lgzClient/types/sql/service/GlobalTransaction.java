package com.lgzClient.types.sql.service;

import com.lgzClient.types.status.GlobalStatus;
import lombok.Data;

import java.util.UUID;

@Data
public class GlobalTransaction {
    public static String generateGlobalId(){
        return "global"+"_"+ UUID.randomUUID().toString();
    }
    private String globalId;//本地事务的id
    private String beginTime;//开始时间
    private GlobalStatus status;//全局事务的状态
}
