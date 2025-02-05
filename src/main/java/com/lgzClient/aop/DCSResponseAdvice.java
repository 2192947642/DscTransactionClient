package com.lgzClient.aop;

import com.lgzClient.service.LocalTransactionManager;
import com.lgzClient.types.TransactSqlLog;
import com.lgzClient.types.LocalType;
import com.lgzClient.types.ThreadContext;
import com.lgzClient.types.status.LocalStatus;
import com.lgzClient.utils.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.sql.SQLException;

@Slf4j
@ControllerAdvice
public class DCSResponseAdvice implements ResponseBodyAdvice<Object> {
    @Autowired
    LocalLogUtil localLogUtil;
    @Autowired
    LocalTransactionManager localTransactionManager;
    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return ThreadContext.isDscTransaction.get()==true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType, Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {
        LocalType localType=(LocalType) RequestUtil.instance.getRequest().getAttribute("localType");
        try{
            if(ThreadContext.error.get()!=null){//如果抛出了异常 那么就进行回滚
                localTransactionManager.rollBack(localType);//回滚本地事务
                localTransactionManager.updateStatusWithNotice(localType, LocalStatus.fail);//通知服务端本地事务执行失败了
            }
            else {//如果没有抛出异常,那么进行提交
                TransactSqlLog transactSqlLog =localLogUtil.buildLocalLogByThread();//建立localLog
                localTransactionManager.addLogToDatabase(transactSqlLog);//将localLog添加到数据库中
                if(StatusUtil.instance.isBegin()){//如果是分布式事务的发起者 那么通知全局事务成功
                    localTransactionManager.updateStatusWithNotice(localType, LocalStatus.success);
                }else{
                    localTransactionManager.updateStatus(localType, LocalStatus.success);//修改redis状态为成功
                }
            }
        }catch (SQLException sqlException){
            throw new RuntimeException(sqlException);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return body;
    }
}
