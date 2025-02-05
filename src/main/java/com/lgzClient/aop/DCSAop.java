package com.lgzClient.aop;

import com.lgzClient.NettyClient;
import com.lgzClient.redis.TransactSqlRedisHelper;
import com.lgzClient.service.LocalTransactionManager;
import com.lgzClient.types.*;
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
     @Pointcut("@annotation(com.lgzClient.annotations.DCSTransaction)")
     public void dscPoint(){};


     @Around("dscPoint()")
     public Object dscAround(ProceedingJoinPoint joinPoint) throws Throwable {
          HttpServletRequest httpServletRequest=RequestUtil.instance.getRequest();
          Boolean isBegin= StatusUtil.instance.isBegin(httpServletRequest);//是否是分布式事务的发起者 只有发起者才会向server发送 本地事务完成的通知 其他分支事务都是直接修改redis中的本地事务状态
          if(isBegin){
               try {
                    localTransactionManager.begin(null);// 开启一个事务
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

