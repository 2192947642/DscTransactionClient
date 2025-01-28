package com.lgzClient.interceptors;

import com.lgzClient.interceptors.DcsRequestInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
@Configurable
public class DcsTransactionWebConfigure implements WebMvcConfigurer {
    @Autowired
    DcsRequestInterceptor dcsRequestInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(dcsRequestInterceptor).addPathPatterns("/**");
    }
}
