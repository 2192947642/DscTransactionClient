package com.lgzClient.types;

import com.lgzClient.types.status.LocalStatus;
import lombok.Data;

@Data
public class LocalNotice {
    public static LocalNotice buildFronLocalType(LocalType localType){
        LocalNotice localNotice=new LocalNotice();
        localNotice.globalId=localType.globalId;
        localNotice.localId=localType.localId;
        localNotice.isSuccess=localType.status== LocalStatus.success;
        return localNotice;
    }
    public String globalId;//全局事务id
    public String localId;//本地事务id
    public boolean  isSuccess;//是否成功
}
