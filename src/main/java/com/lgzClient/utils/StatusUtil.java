package com.lgzClient.utils;

import com.lgzClient.types.status.DCSHeaders;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

public class StatusUtil {
    public static final StatusUtil instance=new StatusUtil();
    public boolean isBegin(HttpServletRequest httpServletRequest){
        String globalId= httpServletRequest.getHeader(DCSHeaders.globalId);//获得全局事务id
        return !StringUtils.hasLength(globalId);
    }

    public boolean isBegin(){
        HttpServletRequest httpServletRequest=RequestUtil.instance.getRequest();
        return isBegin(httpServletRequest);
    }
}
