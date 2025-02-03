package com.lgzClient.interceptors;

import com.alibaba.nacos.common.utils.StringUtils;
import com.lgzClient.redis.TransactSqlRedisHelper;
import com.lgzClient.service.LocalTransactionManager;
import com.lgzClient.types.LocalLog;
import com.lgzClient.types.LocalType;
import com.lgzClient.types.ThreadContext;
import com.lgzClient.types.status.DCSHeaders;
import com.lgzClient.types.status.LocalStatus;
import com.lgzClient.utils.AddressUtil;
import com.lgzClient.utils.RequestUtil;
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
    @Autowired
    TransactSqlRedisHelper redisHelper;
    private final String DcsPreHandlerOnce="DcsPreHandlerOnce";
    private final String DcsAfterHandlerOnce="DcsAfterHandlerOnce";
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if(request.getAttribute(DcsPreHandlerOnce)!=null){
            return true;
        }
        if(request.getAttribute(DcsAfterHandlerOnce)!=null){
            request.setAttribute(DcsPreHandlerOnce,1);
        }
        HttpServletRequest httpServletRequest= RequestUtil.instance.getRequest();
        String globalId= httpServletRequest.getHeader(DCSHeaders.globalId);//获得全局事务id
        Boolean isBegin=!StringUtils.hasLength(globalId);
        if(!isBegin){//如果当前是分布式事务调用
            ThreadContext.globalId.set(globalId);//设置当前的globalId
            LocalType localType= new LocalType(globalId, AddressUtil.buildAddress(AddressUtil.getIp()));
            localTransactionManager.buildLocalTransaction(localType);//创建一个本地事务.并将其与本地事务关联
            redisHelper.addBranchTransaction(localType);
        }
        return true; // 返回true表示继续处理请求，返回false则中断请求处理
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        if(request.getAttribute(DcsAfterHandlerOnce)!=null){//如果已经执行过了就直接返回
            return;
        }
        if(request.getAttribute(DcsAfterHandlerOnce)!=null){
            request.setAttribute(DcsAfterHandlerOnce,1);
        }
        String globalId = request.getHeader(DCSHeaders.globalId);
        boolean isBegin = !StringUtils.hasLength(globalId);
        if (!isBegin) {//如果不是事务的发起者那么就执行;
            LocalType localType = (LocalType) request.getAttribute("localType");
            if (localType != null) {
                //如果抛出了异常 那么就回滚
                if (ex != null||ThreadContext.error.get()!=null) {
                    localTransactionManager.rollBack(localType);
                    // 请求抛出了异常
                    localTransactionManager.updateStatusWithNotice(localType, LocalStatus.fail);
                } else {
                    LocalLog localLog = LocalLog.buildFromLocalType(localType);
                    localLog.status=LocalStatus.success;
                    localTransactionManager.addLogToDatabase(localLog);//将localLog添加到数据库中
                    localTransactionManager.updateStatus(localType, LocalStatus.success);//修改redis中的本地事务状态为成功
                }
            }
            TransactionSynchronizationManager.clear();
            TransactionSynchronizationManager.unbindResource(dataSourceTransactionManager.getDataSource());//解除关联
        }
        ThreadContext.removeAll();
    }

}