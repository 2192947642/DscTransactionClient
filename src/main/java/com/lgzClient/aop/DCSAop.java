package com.lgzClient.aop;

import com.lgzClient.NettyClient;
import com.lgzClient.redis.TransactSqlRedisHelper;
import com.lgzClient.service.LocalTransactionManager;
import com.lgzClient.types.*;
import com.lgzClient.types.status.DCSHeaders;
import com.lgzClient.types.status.LocalStatus;
import com.lgzClient.types.status.MessageTypeEnum;
import com.lgzClient.utils.AddressUtil;
import com.lgzClient.utils.JsonUtil;
import com.lgzClient.utils.RequestUtil;
import com.lgzClient.utils.TimeUtil;
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
          String globalId= httpServletRequest.getHeader(DCSHeaders.globalId);//获得全局事务id
          Boolean isBegin= !StringUtils.hasLength(globalId);//是否是分布式事务的发起者 只有发起者才会向server发送 本地事务完成的通知 其他分支事务都是直接修改redis中的本地事务状态
          if(isBegin){
               LocalType localType=null;
               try {
                    String serverAddress= AddressUtil.buildAddress(AddressUtil.getIp());
                    localType=new LocalType(globalId,serverAddress);
                    ThreadContext.globalId.set(localType.globalId);//将该全局事务的id添加到当前的线程中
                    localTransactionManager.buildLocalTransaction(localType);//创建一个本地事务.并将其与本地事务关联
                    redisHelper.addBranchTransaction(localType);//向redis中添加该本地事务
                    Object[] args=joinPoint.getArgs();
                    Object res=joinPoint.proceed(args);
                    localType.status=LocalStatus.success;
                    LocalLog localLog=LocalLog.buildFromLocalType(localType);
                    localTransactionManager.addLogToDatabase(localLog);//本地事务执行成功后在向服务端发送成功通知前 将其写入到数据库中
                    localTransactionManager.updateStatusWithNotice(localType,LocalStatus.success);//向服务端发送成功通知
                    localLog.buildFromLocalType(localType);
                    return res;
               }catch (Throwable e){
                    localTransactionManager.rollBack(localType);//出现错误直接进行回滚
                    localTransactionManager.updateStatusWithNotice(localType,LocalStatus.fail);//执行失败
                    throw  e;
               }finally {
                    TransactionSynchronizationManager.clear();
                    TransactionSynchronizationManager.unbindResource(dataSourceTransactionManager.getDataSource());//解除关联
               }
          }
          else{
               Object[] args=joinPoint.getArgs();
               Object res=joinPoint.proceed(args);
               return res;
          }
     }

}

