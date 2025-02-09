package com.lgzClient.configure;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
@Configuration
public class TractRestTemplateConfig {
    @Bean("tractRestTemplate")
    @LoadBalanced
    public RestTemplate tractRestTemplate() {
        return new RestTemplate();
    }
}
