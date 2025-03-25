package com.lgzClient.interceptors;

import com.alibaba.nacos.common.utils.StringUtils;
import com.lgzClient.service.LocalTransactionManager;
import com.lgzClient.types.DCSThreadContext;
import com.lgzClient.types.status.DCSHeaders;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class DcsRequestInterceptor implements HandlerInterceptor {
    @Autowired
    DataSourceTransactionManager dataSourceTransactionManager;
    @Autowired
    LocalTransactionManager localTransactionManager;
    private final String DcsPreHandlerOnce="DcsPreHandlerOnce";
    private final String DcsAfterHandlerOnce="DcsAfterHandlerOnce";
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if(request.getAttribute(DcsPreHandlerOnce)!=null){
            return true;
        }
        if(request.getAttribute(DcsAfterHandlerOnce)!=null){//只执行一次
            request.setAttribute(DcsPreHandlerOnce,1);
        }
        String globalId= request.getHeader(DCSHeaders.globalId);//获得全局事务id
        if(StringUtils.hasLength(globalId)){//如果当前是分布式事务调用,那么就加入到分布式事务中
           localTransactionManager.begin(globalId,null);
        }
        return true; // 返回true表示继续处理请求，返回false则中断请求处理
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)  {
        if(DCSThreadContext.isDscTransaction.get()==null|| DCSThreadContext.isDscTransaction.get()!=true) return;
        if(request.getAttribute(DcsAfterHandlerOnce)!=null){//如果已经执行过了就直接返回
            return;
        }
        if(request.getAttribute(DcsAfterHandlerOnce)!=null){
            request.setAttribute(DcsAfterHandlerOnce,1);
        } //接触与本地事务的关联
        try {
            TransactionSynchronizationManager.clear();
            TransactionSynchronizationManager.unbindResource(dataSourceTransactionManager.getDataSource());//解除关联
        }finally {
            DCSThreadContext.removeAll();
        }
    }

}