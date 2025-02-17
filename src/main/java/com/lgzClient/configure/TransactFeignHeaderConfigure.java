package com.lgzClient.configure;

import com.lgzClient.types.DCSThreadContext;
import com.lgzClient.types.status.DCSHeaders;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.util.StringUtils;

@Configurable
public class TransactFeignHeaderConfigure implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate requestTemplate) {
        String globalId= DCSThreadContext.globalId.get();
        if(StringUtils.hasLength(globalId)){
            requestTemplate.header(DCSHeaders.globalId,globalId);
        }
    }
}
