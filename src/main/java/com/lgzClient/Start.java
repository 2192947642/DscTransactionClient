package com.lgzClient;

import com.alibaba.cloud.nacos.discovery.NacosServiceDiscovery;
import com.alibaba.nacos.api.exception.NacosException;
import com.lgzClient.utils.AddressUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class Start {
    @Autowired
    Environment environment;
    @Autowired
    private NacosServiceDiscovery nacosServiceDiscovery;
    //查询 在naccos中注册的netty服务
    private final String serverName="TractSqlServiceNetty";
    @PostConstruct
    public void init() throws NacosException {
       Integer port=environment.getProperty("server.port",Integer.class);
       AddressUtil.initPort(port);//设置端口
       this.startConnect();
    }
    //每15秒检查一次连接 查看是否有新的 没有连接
    @Scheduled(cron = "0/15 * * * * ?")
    public void startConnect() throws NacosException {
        List<ServiceInstance> instances= nacosServiceDiscovery.getInstances(serverName);
        for(ServiceInstance instance :instances){
            NettyClient.buildClient(instance.getHost(),instance.getPort());//建立连接
        }
    }
}
