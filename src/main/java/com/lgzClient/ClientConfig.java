package com.lgzClient;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClientConfig {
    private static ClientConfig instance;
    public static ClientConfig getInstance()
    {
        return instance;
    }
    @PostConstruct
    public void init()
    {
        instance=this;
    }
    @Value("${spring.cloud.dcstransaction.checkTransactionTimeOutInterval:5000}")
    public  int checkTimeOutInterval;//检查

    @Value("${spring.cloud.dcstransaction.reconnectInterval:5000}")
    public  int reconnectInterval;//重连的间隔,单位s
}
