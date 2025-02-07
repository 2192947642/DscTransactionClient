#### 功能

- 负责对本地事务的创建与加入到全局事务当中去(即在redis中创建对应的数据)
- 在openFeign中通过在header中加入globalId来进行分布式事务传递

- 通过aop和请求拦截器拦截器来对事务进行拦截处理,创建一个与数据库的连接并且与当前线程绑定，将该连接添加到一个统一的队列中,当接受到服务端发送来的全局通知时，将其进行回滚或者提交

#### 使用

使用注解@DscTransaction来进行使用,在全局事务进行创建时，@Transactional和@DscTransaction不可以一起进行使用,但是在加入到全局事务中时 可以一起使用

#### 实现

全局事务使用aop来进行创建,实现代码

```java
package com.lgzClient.aop;

import com.lgzClient.NettyClient;
import com.lgzClient.redis.TransactSqlRedisHelper;
import com.lgzClient.service.LocalTransactionManager;
import com.lgzClient.types.LocalNotice;
import com.lgzClient.types.LocalType;
import com.lgzClient.types.Message;
import com.lgzClient.types.ThreadContext;
import com.lgzClient.types.status.BranchStatus;
import com.lgzClient.types.status.DCSHeaders;
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
    public void dscPoint() {
    }

    ;

    private void updateStatusWithNotice(LocalType localType, BranchStatus branchStatus) {
        updateStatus(localType, branchStatus);
        LocalNotice localNotice = LocalNotice.buildFronLocalType(localType);
        Message message = new Message(MessageTypeEnum.LocalNotice, JsonUtil.objToJson(localNotice), TimeUtil.getLocalTime());
        NettyClient.sendMsg(message, true);
    }

    private void updateStatus(LocalType localType, BranchStatus branchStatus) {
        localType.status = branchStatus;
        if (localType.trxId == null) {
            localType.trxId = LocalTransactionManager.getTransactionId(ThreadContext.connection.get());
        }
        redisHelper.updateLocalTransaction(localType);
    }

    @Around("dscPoint()")
    public Object dscAround(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest httpServletRequest = RequestUtil.instance.getRequest();
        String globalId = httpServletRequest.getHeader(DCSHeaders.globalId);//获得全局事务id
        Boolean isBegin = !StringUtils.hasLength(globalId);//是否是分布式事务的发起者 只有发起者才会向server发送 本地事务完成的通知 其他分支事务都是直接修改redis中的本地事务状态
        if (isBegin) {
            LocalType localType = null;
            try {
                String serverAddress = AddressUtil.buildAddress(AddressUtil.getIp());
                localType = new LocalType(globalId, serverAddress);
                ThreadContext.globalId.set(localType.globalId);//将该全局事务的id添加到当前的线程中
                localTransactionManager.buildLocalTransaction(localType);//创建一个本地事务.并将其与本地事务关联
                redisHelper.addBranchTransaction(localType);//向redis中添加该本地事务
                Object[] args = joinPoint.getArgs();
                Object res = joinPoint.proceed(args);
                updateStatusWithNotice(localType, BranchStatus.success);
                return res;
            } catch (Throwable e) {
                localTransactionManager.rollBack(localType);
                updateStatusWithNotice(localType, BranchStatus.fail);//执行失败
                throw e;
            } finally {
                TransactionSynchronizationManager.clear();
                TransactionSynchronizationManager.unbindResource(dataSourceTransactionManager.getDataSource());//解除关联
            }
        } else {
            Object[] args = joinPoint.getArgs();
            Object res = joinPoint.proceed(args);
            return res;
        }
    }

}
```

在调用链路中使用拦截器来进行加入

```java
package com.lgzClient.interceptors;

import com.alibaba.nacos.common.utils.StringUtils;
import com.lgzClient.NettyClient;
import com.lgzClient.redis.TransactSqlRedisHelper;
import com.lgzClient.service.LocalTransactionManager;
import com.lgzClient.types.LocalNotice;
import com.lgzClient.types.LocalType;
import com.lgzClient.types.Message;
import com.lgzClient.types.ThreadContext;
import com.lgzClient.types.status.BranchStatus;
import com.lgzClient.types.status.DCSHeaders;
import com.lgzClient.types.status.MessageTypeEnum;
import com.lgzClient.utils.AddressUtil;
import com.lgzClient.utils.JsonUtil;
import com.lgzClient.utils.RequestUtil;
import com.lgzClient.utils.TimeUtil;
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
    private final String DcsPreHandlerOnce = "DcsPreHandlerOnce";
    private final String DcsAfterHandlerOnce = "DcsAfterHandlerOnce";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (request.getAttribute(DcsPreHandlerOnce) != null) {
            return true;
        }
        if (request.getAttribute(DcsAfterHandlerOnce) != null) {
            request.setAttribute(DcsPreHandlerOnce, 1);
        }
        HttpServletRequest httpServletRequest = RequestUtil.instance.getRequest();
        String globalId = httpServletRequest.getHeader(DCSHeaders.globalId);//获得全局事务id
        Boolean isBegin = !StringUtils.hasLength(globalId);
        if (!isBegin) {//如果当前是分布式事务调用
            ThreadContext.globalId.set(globalId);//设置当前的globalId
            LocalType localType = new LocalType(globalId, AddressUtil.buildAddress(AddressUtil.getIp()));
            localTransactionManager.buildLocalTransaction(localType);//创建一个本地事务.并将其与本地事务关联
            redisHelper.addBranchTransaction(localType);
        }
        return true; // 返回true表示继续处理请求，返回false则中断请求处理
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        if (request.getAttribute(DcsAfterHandlerOnce) != null) {//如果已经执行过了就直接返回
            return;
        }
        if (request.getAttribute(DcsAfterHandlerOnce) != null) {
            request.setAttribute(DcsAfterHandlerOnce, 1);
        }
        String globalId = request.getHeader(DCSHeaders.globalId);
        boolean isBegin = !StringUtils.hasLength(globalId);
        if (!isBegin) {//如果不是事务的发起者那么就执行;
            LocalType localType = (LocalType) request.getAttribute("localType");
            if (localType != null) {
                //如果抛出了异常 那么就回滚
                if (ex != null || ThreadContext.error.get() != null) {
                    localTransactionManager.rollBack(localType);
                    // 请求抛出了异常
                    uploadStatusWithNotice(localType, BranchStatus.fail);
                } else {
                    // 请求成功
                    uploadStatus(localType, BranchStatus.success);
                }
            }
            TransactionSynchronizationManager.clear();
            TransactionSynchronizationManager.unbindResource(dataSourceTransactionManager.getDataSource());//解除关联
        }
        ThreadContext.removeAll();
    }

    private void uploadStatusWithNotice(LocalType localType, BranchStatus branchStatus) {
        localType.status = branchStatus;
        redisHelper.updateLocalTransaction(localType);
        LocalNotice localNotice = LocalNotice.buildFronLocalType(localType);
        Message message = new Message(MessageTypeEnum.LocalNotice, JsonUtil.objToJson(localNotice), TimeUtil.getLocalTime());
        NettyClient.sendMsg(message, true);
    }

    private void uploadStatus(LocalType localType, BranchStatus branchStatus) {
        localType.status = branchStatus;
        if (localType.trxId == null) {
            localType.trxId = LocalTransactionManager.getTransactionId(ThreadContext.connection.get());
        }
        redisHelper.updateLocalTransaction(localType);
    }
}
```

#### 事务的统一处理

```java
package com.lgzClient.service;

import com.lgzClient.redis.TransactSqlRedisHelper;
import com.lgzClient.types.LocalType;
import com.lgzClient.types.ThreadContext;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.sql.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LocalTransactionManager {
    @Autowired
    TransactSqlRedisHelper transactSqlRedisHelper;
    @Autowired
    DataSourceTransactionManager transactionManager;

    public static LocalTransactionManager instance;

    //获得事务的id
    public static Long getTransactionId(Connection connection) {
        String sql = "SELECT TRX_ID FROM information_schema.INNODB_TRX WHERE TRX_MYSQL_THREAD_ID = CONNECTION_ID()";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getLong("TRX_ID");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    @PostConstruct
    public void init() {
        instance = this;
    }

    private static final ConcurrentHashMap<String, Connection> localTransactionMaps = new ConcurrentHashMap<>();

    public Connection buildLocalTransaction(LocalType localType) throws SQLException//新建一个本地事务,并将其绑定到当前的线程中
    {
        Connection connection = transactionManager.getDataSource().getConnection();
        ThreadContext.connection.set(connection);
        connection.setAutoCommit(false);
        ConnectionHolder connectionHolder = new ConnectionHolder(connection);
        TransactionSynchronizationManager.bindResource(transactionManager.getDataSource(), connectionHolder);
        localTransactionMaps.put(localType.localId, connection);
        return connection;
    }

    public Connection getLocalTransaction(String localId) {
        return localTransactionMaps.get(localId);
    }

    //回滚事务
    public void rollBack(LocalType localType) throws SQLException {
        Connection connection = getLocalTransaction(localType.localId);
        if (connection == null) return;//如果连接为null 那么说明已经被操作了
        try {
            connection.rollback();
            // localType.status=LocalStatus.rollback;
            // transactSqlRedisHelper.updateLocalTransaction(localType);
            transactSqlRedisHelper.deleteLocalTransactionWithDeleteGlobal(localType.globalId, localType.localId);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            connection.close();
            localTransactionMaps.remove(localType.localId);
        }

    }

    //提交事务
    public void commit(LocalType localType) throws SQLException {
        Connection connection = getLocalTransaction(localType.localId);
        if (connection == null) return;
        try {
            connection.commit();
            // localType.status= LocalStatus.commit;
            // transactSqlRedisHelper.updateLocalTransaction(localType);//修改status为成功
            transactSqlRedisHelper.deleteLocalTransactionWithDeleteGlobal(localType.globalId, localType.localId);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            connection.close();
            localTransactionMaps.remove(localType.localId);
        }
    }


}
```

