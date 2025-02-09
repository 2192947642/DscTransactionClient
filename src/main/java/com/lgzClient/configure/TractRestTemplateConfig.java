package com.lgzClient.configure;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

public class TractRestTemplateConfig {
    @Bean("tractRestTemplate")
    @LoadBalanced
    public RestTemplate tractRestTemplate() {
        return new RestTemplate();
    }
}
