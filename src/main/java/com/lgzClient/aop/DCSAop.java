package com.lgzClient.aop;

import com.lgzClient.NettyClient;
import com.lgzClient.redis.TransactSqlRedisHelper;
import com.lgzClient.service.LocalTransactionManager;
import com.lgzClient.types.*;
import com.lgzClient.types.status.DCSAttributes;
import com.lgzClient.types.status.DCSHeaders;
import com.lgzClient.types.status.LocalStatus;
import com.lgzClient.types.status.MessageTypeEnum;
import com.lgzClient.utils.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

@Component
@Aspect
@Slf4j
public class DCSAop {
     @Autowired
     TransactSqlRedisHelper redisHelper;
     @Autowired
     LocalTransactionManager localTransactionManager;//本地事务管理器
     @Autowired
     DataSourceTransactionManager dataSourceTransactionManager;

     @Pointcut("@annotation(com.lgzClient.annotations.DCSTransaction)")
     public void dscPoint(){};


     @Around("dscPoint()")
     public Object dscAround(ProceedingJoinPoint joinPoint) throws Throwable {
          HttpServletRequest httpServletRequest=RequestUtil.instance.getRequest();
          httpServletRequest.setAttribute(DCSAttributes.isDscTransaction,true);//设置为分布式事务
          Boolean isBegin= StatusUtil.instance.isBegin(httpServletRequest);//是否是分布式事务的发起者 只有发起者才会向server发送 本地事务完成的通知 其他分支事务都是直接修改redis中的本地事务状态
          if(isBegin){
               LocalType localType=null;
               try {
                    localType=new LocalType(AddressUtil.buildAddress(AddressUtil.getIp()));
                    httpServletRequest.setAttribute(DCSAttributes.localType,localType);//将localType加入到上下文中
                    ThreadContext.globalId.set(localType.getGlobalId());//将该全局事务的id添加到当前的线程中
                    localTransactionManager.buildLocalTransaction(localType);//创建一个本地事务.并将其与本地事务关联
                    redisHelper.addBranchTransaction(localType);//向redis中添加该本地事务
                    Object[] args=joinPoint.getArgs();
                    Object res=joinPoint.proceed(args);
                    return res;
               }catch (Throwable e){
                    ThreadContext.error.set(e);
                    throw  e;
               }
          }
          else{
               Object[] args=joinPoint.getArgs();
               Object res=joinPoint.proceed(args);
               return res;
          }
     }

}

