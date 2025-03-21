package com.lgzClient.aop;

import com.lgzClient.service.LocalTransactionManager;
import com.lgzClient.types.DCSThreadContext;
import com.lgzClient.types.sql.client.NotDoneSqlLog;
import com.lgzClient.types.sql.service.BranchTransaction;
import com.lgzClient.types.status.BranchStatus;
import com.lgzClient.utils.NotDoneSqlLogUtil;
import com.lgzClient.utils.StatusUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@ControllerAdvice
public class DCSResponseAdvice implements  ResponseBodyAdvice<Object> {
    @Autowired
    NotDoneSqlLogUtil notDoneSqlLogUtil;
    @Autowired
    LocalTransactionManager localTransactionManager;
    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return DCSThreadContext.isDscTransaction.get()==true;
    }
    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType, Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {
        BranchTransaction branchTransaction= DCSThreadContext.branchTransaction.get();
        if(DCSThreadContext.error.get()!=null){//如果抛出了异常 那么就直接进行回滚
            localTransactionManager.rollBack(branchTransaction.getBranchId(),false,true);//回滚本地事务
        }
        else {//如果没有抛出异常
            NotDoneSqlLog notDoneSqlLog = notDoneSqlLogUtil.buildNotDoneLogByThread();//建立localLog
            localTransactionManager.updateLogOfDBS(notDoneSqlLog);//将localLog更新到数据库中
            localTransactionManager.updateLocalSuccessTime(branchTransaction.getBranchId());
            if(StatusUtil.instance.isBegin()){//如果是分布式事务的发起者 那么通知全局事务成功
                localTransactionManager.updateStatusWithNotice(branchTransaction, BranchStatus.success);
            }else{
                localTransactionManager.updateStatus(branchTransaction, BranchStatus.success);//修改redis状态为成功
            }
        }
        return body;
    }
}
