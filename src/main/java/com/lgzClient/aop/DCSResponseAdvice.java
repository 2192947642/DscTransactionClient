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
    LocalTransactionManager localTransactionManager;
    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        if(DCSThreadContext.isDscTransaction.get()==null||DCSThreadContext.isDscTransaction.get()==false){
            return false;
        }
        return true;
    }
    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType, Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {
        BranchTransaction branchTransaction= DCSThreadContext.branchTransaction.get();
        if(DCSThreadContext.error.get()!=null){//如果抛出了异常 那么就直接进行回滚
            //1.本身服务出现了异常 2.获取connection资源时出现了超时异常,此时connection并未获得
            localTransactionManager.rollBack(branchTransaction,false,true);//回滚本地事务
        }
        else {//如果没有抛出异常,那么就向远程提交本地事务执行成功
            localTransactionManager.success(branchTransaction,StatusUtil.instance.isBegin());
        }
        return body;
    }
}
