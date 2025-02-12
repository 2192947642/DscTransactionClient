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
    @Value("${spring.cloud.dcstransaction.maxReconnectAttempts:5}")
    public  int maxReconnectAttempts;//设置最大重连次数
    @Value("${spring.cloud.dcstransaction.reconnectInterval:2000}")
    public  int reconnectInterval;//重连的时间,单位s
}
