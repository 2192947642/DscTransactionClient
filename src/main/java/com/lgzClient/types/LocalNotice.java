package com.lgzClient.types;

import com.lgzClient.types.status.LocalStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class LocalNotice {
    public static LocalNotice buildFronLocalType(LocalType localType){
        LocalNotice localNotice=new LocalNotice();
        localNotice.globalId= localType.getGlobalId();
        localNotice.localId= localType.getLocalId();
        localNotice.isSuccess= localType.getStatus() == LocalStatus.success;
        return localNotice;
    }
    private String globalId;//全局事务id
    private String localId;//本地事务id
    private boolean  isSuccess;//是否成功
}
