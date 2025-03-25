package com.lgzClient.configure;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "spring.cloud.transaction")
@Configuration
@Data
public class ClientConfig {
    private static ClientConfig instance;

    public static ClientConfig getInstance() {
        return instance;
    }

    @PostConstruct
    public void init() {
        instance = this;
    }

    /**
     * 分布式事务最大持有的连接数量,需要小于数据库连接池的最大数量,否则会发生死锁
     */
    private Integer maxHandlerConnection = 4;
    /**
     * 最大可等待其他服务的时间
     */
    private Integer checkTimeOutIntervalWaitOthers = 5000;
    /**
     * 本地事务最长的执行时间 大于最大可等待其他服务的时间
     */
    private Integer checkTimeOutIntervalPersonal = 6000;//检查
    /**
     * //重连的间隔,单位s
     */
    private Integer reconnectInterval = 5000;
}
