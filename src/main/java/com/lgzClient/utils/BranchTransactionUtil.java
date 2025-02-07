package com.lgzClient.utils;

import com.lgzClient.types.sql.service.BranchTransaction;
import com.lgzClient.types.status.BranchStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.UnknownHostException;

@Component
public class BranchTransactionUtil {
    @Value("${spring.application.name}")
    private String applicationName;
    public BranchTransaction buildDefaultTransaction() throws UnknownHostException {
        BranchTransaction branchTransaction=new BranchTransaction();
        branchTransaction.setApplicationName(applicationName);//当前的服务名称
        branchTransaction.setServerAddress(AddressUtil.buildAddress(AddressUtil.getIp()));
        branchTransaction.setBeginTime(TimeUtil.getLocalTime());//当前的时间
        branchTransaction.setStatus(BranchStatus.wait);
        return branchTransaction;
    }
}
