#### 分布式事务客户端

##### 功能

- 负责将本地事务加入到分布式事务中
- 将数据库连接交由LocalTransactionManage来进行管理,
- 对connection和statement进行包装,记录执行的sql语句和执行前后的数据库数据变化,当本地事务执行开始前将其基础状态(global_id,branch_id,begin_time)保存到not_do_sql_log中.当本地事务执行成功后,将执行的sql记录更新到not_do_sql_log中
- 通过netty来接受来自服务端的分布式事务消息通知,根据全局事务的状态来控制对应connection的回滚或者提交同时删除客户端数据库中的表not_do_sql_log中的对应的本地事务的sql记录.

##### 使用

######    事务开启

​	使用注解**@DcsTransaction**来表示一个分布式事务的起点,在后续调用链路中,会通过**http**请求头中的**globaId**来进行分布式事务的传递。@DcsTransaction和@Transaction两个注解不可以同时使用。但是在调用链路中被调用者可以使用@Transaction注解(事务传播类型应该是加入到当前事务当中),如果自定义了全局异常处理器 那么应该对ThreadContext.error来进行错误赋值 来使得错误正常捕获

###### 	事务状态查询

分支事务(本地事务)状态

```java
package com.lgzClient.rpc;
import com.lgzClient.types.Result;
import com.lgzClient.types.sql.service.BranchTransaction;
import org.springframework.web.bind.annotation.*;
public interface BranchTransactRpc {
    String prefix="http://TractSqlServiceServlet";
    @GetMapping("/branchTransaction")
    Result<BranchTransaction> getBranchTransaction(@RequestParam("branchId") String branchId);
    @PostMapping("/branchTransaction")
    Result<BranchTransaction> joinBranchTransaction(@RequestBody BranchTransaction localType);
    @DeleteMapping("/branchTransaction")
    Result<BranchTransaction> deleteBranchTransaction(@RequestParam("branchId") String branchId);
    @PutMapping("/branchTransaction/status")
    Result<BranchTransaction> updateBranchTransactionStatus(@RequestBody BranchTransaction branchTransaction);
    @PutMapping("/branchTransaction/status/notice")
    Result<BranchTransaction> updateBranchTransactionStatusWithNotice(@RequestBody BranchTransaction branchTransaction);
}
```

```java
package com.lgzClient.rpc;

import com.lgzClient.types.Result;
import com.lgzClient.types.sql.service.GlobalTransaction;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
public interface GlobalTransactRpc {
    String prefix="http://TractSqlServiceServlet";
    @GetMapping("/globalTransaction")
    Result<GlobalTransaction> getGlobalTransaction(@RequestParam("globalId") String globalId);
    @PostMapping("/globalTransaction/create")
    Result<GlobalTransaction> createGlobalTransaction(@RequestParam("timeout") Long timeout);
    @GetMapping("/globalTransactions")
    Result<ArrayList<GlobalTransaction>> getGlobalTransactions(@RequestParam("globalIds")ArrayList<String> globalIds);
}
```

###### 表not_do_sql_log的对应类

```java
package com.lgzClient.types.sql.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotDoneSqlLog {//本地事务日志 只有本地事务执行成功才会进行记录
    private String branchId;//分支事务的id
    private String globalId;//事务id
    private String beginTime;//开始时间
    private Integer done; //是否以及提交或者是回滚 0为 未完成 1为已完成
    private String requestUri;//请求的路径
    private String applicationName;//微服务的名称
    private String serverAddress;//该项目的运行地址
    private String logs;  //其内部类型为 delete/insert/select/update(Recode),通过NotDoneSql
}
```

###### 对应类的工具类

```java
package com.lgzClient.utils;

import com.lgzClient.types.ThreadContext;
import com.lgzClient.types.sql.client.NotDoneSqlLog;
import com.lgzClient.types.sql.recode.*;
import com.lgzClient.types.sql.service.BranchTransaction;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;

@Component
public class NotDoneSqlLogUtil {
	
    public NotDoneSqlLog buildUndoSqlLogFromLocalBranchTransaction(BranchTransaction branchTransaction){
        NotDoneSqlLog notDoneSqlLog =new NotDoneSqlLog();
        notDoneSqlLog.setBranchId(branchTransaction.getBranchId());
        notDoneSqlLog.setGlobalId(branchTransaction.getGlobalId());
        notDoneSqlLog.setBeginTime(branchTransaction.getBeginTime());
        notDoneSqlLog.setApplicationName(branchTransaction.getApplicationName());
        notDoneSqlLog.setServerAddress(branchTransaction.getServerAddress());
        return notDoneSqlLog;
    };
    public NotDoneSqlLog buildUndoLogByThread(){
        NotDoneSqlLog notDoneSqlLog = buildUndoSqlLogFromLocalBranchTransaction(ThreadContext.branchTransaction.get());
        String requestUri= RequestUtil.instance.getRequest().getRequestURI();//请求的接口路径
        notDoneSqlLog.setRequestUri(requestUri);
        notDoneSqlLog.setLogs(JsonUtil.objToJson(ThreadContext.sqlRecodes.get()));//记录的sql日志
        return notDoneSqlLog;
    }
	//负责将logs属性转换为delete/insert/select/update(Recode)链表
    public ArrayList<Object> getRecodesByUndoLog(NotDoneSqlLog notDoneSqlLog) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        ArrayList<Object> recodes=new ArrayList<>();
        ArrayList<String> logs=JsonUtil.jsonToObject(notDoneSqlLog.getBranchId(),ArrayList.class);
        for(String log:logs){
            HashMap<String,String> hashMap=JsonUtil.jsonToObject(log,HashMap.class);
            if(hashMap.get("sqlType").equals(SqlType.insert.name())){
                InsertRecode insertRecode=BeanMapUtil.mapToBean(hashMap, InsertRecode.class);
                recodes.add(insertRecode);
            }else if(hashMap.get("sqlType").equals(SqlType.delete.name())){
                DeleteRecode deleteRecode=BeanMapUtil.mapToBean(hashMap, DeleteRecode.class);
                recodes.add(deleteRecode);
            }else if(hashMap.get("sqlType").equals(SqlType.update.name())){
                UpdateRecode updateRecode=BeanMapUtil.mapToBean(hashMap, UpdateRecode.class);
                recodes.add(updateRecode);
            }else if(hashMap.get("sqlType").equals(SqlType.select.name())){
                SelectRecode selectRecode=BeanMapUtil.mapToBean(hashMap, SelectRecode.class);
                recodes.add(selectRecode);
            }
        }
        return recodes;
    }
}
```

##### 实现

在本地事务开启前,获得一个connection连接,并将其进行包装,确保其内部的statement返回的也是包装类，随后通过TransactionSynchronizationManager将其与当前线程绑定，在本地事务执行完成后将其与当前线程解绑,交由LocalTransactionManager进行管理控制后续的回滚或者是提交

###### DCSAOP(开启分布式事务)

```java
package com.lgzClient.aop;

import com.lgzClient.annotations.DCSTransaction;
import com.lgzClient.service.LocalTransactionManager;
import com.lgzClient.utils.RequestUtil;
import com.lgzClient.utils.StatusUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Component
@Aspect
@Slf4j
public class DCSAop {
     @Autowired
     LocalTransactionManager localTransactionManager;//本地事务管理器
     @Pointcut("@annotation(com.lgzClient.annotations.DCSTransaction)")
     public void dscPoint(){};
     @Around("dscPoint()")
     public Object dscAround(ProceedingJoinPoint joinPoint) throws Throwable {
          HttpServletRequest httpServletRequest=RequestUtil.instance.getRequest();
          Boolean isBegin= StatusUtil.instance.isBegin(httpServletRequest);//是否是分布式事务的发起者 只有发起者才会向server发送 本地事务完成的通知 其他分支事务都是直接修改redis中的本地事务状态
          if(isBegin){
               MethodSignature signature = (MethodSignature) joinPoint.getSignature();
               Method method = signature.getMethod();
               DCSTransaction dscTransaction=method.getAnnotation(DCSTransaction.class);
               localTransactionManager.begin(null,dscTransaction);// 开启一个事务
          }
          Object[] args=joinPoint.getArgs();
          Object res=joinPoint.proceed(args);
          return res;
     }



}
```

###### DCSResponseAdvice (在结果返回前更新服务端的本地事务的状态)

```java

package com.lgzClient.aop;

import com.lgzClient.service.LocalTransactionManager;
import com.lgzClient.types.ThreadContext;
import com.lgzClient.types.sql.client.NotDoneSqlLog;
import com.lgzClient.types.sql.service.BranchTransaction;
import com.lgzClient.types.status.BranchStatus;
import com.lgzClient.utils.NotDoneSqlLogUtil;
import com.lgzClient.utils.StatusUtil;
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
public class DCSResponseAdvice implements  ResponseBodyAdvice<Object> {
    @Autowired
    NotDoneSqlLogUtil notDoneSqlLogUtil;
    @Autowired
    LocalTransactionManager localTransactionManager;
    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return ThreadContext.isDscTransaction.get()==true;
    }
    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType, Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {
        BranchTransaction branchTransaction=ThreadContext.branchTransaction.get();
        try{
            if(ThreadContext.error.get()!=null){//如果抛出了异常 那么就进行回滚
              localTransactionManager.rollBack(branchTransaction.getBranchId());//回滚本地事务
              localTransactionManager.updateStatusWithNotice(branchTransaction, BranchStatus.fail);//通知服务端本地事务执行失败了
            }
            else {//如果没有抛出异常
                NotDoneSqlLog notDoneSqlLog = notDoneSqlLogUtil.buildUndoLogByThread();//建立localLog
                localTransactionManager.updateLogOfDBS(notDoneSqlLog);//将localLog更新到数据库中
                localTransactionManager.updateLocalSuccessTime(branchTransaction.getBranchId());
                if(StatusUtil.instance.isBegin()){//如果是分布式事务的发起者 那么通知全局事务成功
                    localTransactionManager.updateStatusWithNotice(branchTransaction, BranchStatus.success);
                }else{
                    localTransactionManager.updateStatus(branchTransaction, BranchStatus.success);//修改redis状态为成功
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
```



###### LocalTransactionManager(管理事务的提交回滚以及对应的connection的查询)

```java
package com.lgzClient.service;

import com.lgzClient.annotations.DCSTransaction;
import com.lgzClient.rpc.BranchTransactRpc;
import com.lgzClient.rpc.GlobalTransactRpc;
import com.lgzClient.types.ThreadContext;
import com.lgzClient.types.TransactContainer;
import com.lgzClient.types.sql.client.NotDoneSqlLog;
import com.lgzClient.types.sql.service.BranchTransaction;
import com.lgzClient.types.sql.service.GlobalTransaction;
import com.lgzClient.types.status.BranchStatus;
import com.lgzClient.utils.BranchTransactionUtil;
import com.lgzClient.utils.NotDoneSqlLogUtil;
import com.lgzClient.utils.TimeUtil;
import com.lgzClient.wrapper.ConnectionWrapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

@Component
public  class LocalTransactionManager {
        @Autowired
        NotDoneSqlLogUtil notDoneSqlLogUtil;
        @Autowired
        GlobalTransactRpc globalTransactRpc;
        @Autowired
        BranchTransactRpc branchTransactRpc;
        @Autowired
        BranchTransactionUtil branchTransactionUtil;

        @Autowired
        DataSourceTransactionManager transactionManager;
        public static LocalTransactionManager instance;
        public void updateLocalSuccessTime(String branchId){//修改本地事务的successTime
            localTransactionMaps.get(branchId).setSuccessTime(new Date());
        }
        @PostConstruct
        public void init()
        {
            instance=this;
        }
        private static final ConcurrentHashMap<String, TransactContainer> localTransactionMaps=new ConcurrentHashMap<>();

        //获得 n毫秒之前完成但是没有进行提交或者回滚的本地事务
        public ArrayList<TransactContainer> getUnDoTransactions(long millisecond){
            ArrayList<TransactContainer> transactContainers=new ArrayList<>();
            localTransactionMaps.forEach((k,v)->{
                if(v.getSuccessTime()!=null){
                    transactContainers.add(v);
                }
            });//将所有已经成功的本地事务加入到list中
            transactContainers.sort((o1, o2) -> o1.getSuccessTime().compareTo(o2.getSuccessTime()));
            Long nowTime=TimeUtil.getNowTime();
            ArrayList<TransactContainer> returnList=new ArrayList<>();
            for (TransactContainer transactContainer :  transactContainers){
                Date successTime=transactContainer.getSuccessTime();
                if(nowTime-successTime.getTime()>=millisecond){
                    returnList.add(transactContainer);
                }else{
                    break;
                }
            }
            return returnList;
        }
        //开启一个分布式事务,如果globalId不为空 那么就会开启一个新的分布式事务
        public void begin(String globalId, DCSTransaction dcsTransaction) throws SQLException, UnknownHostException {
                BranchTransaction branchTransaction=branchTransactionUtil.buildDefaultTransaction();
                if(StringUtils.hasLength(globalId)){
                    branchTransaction.setGlobalId(globalId);//设置所属的globalId
                }
                else if(!StringUtils.hasLength(branchTransaction.getGlobalId())){//如果不存在globalId那么就开启一个新的分布式事务
                    GlobalTransaction globalTransaction=globalTransactRpc.createGlobalTransaction(dcsTransaction.timeout()).getData();
                    branchTransaction.setGlobalId(globalTransaction.getGlobalId());//设置globalId
                }
                branchTransaction.setBranchId(branchTransactRpc.joinBranchTransaction(branchTransaction).getData().getBranchId());//加入到当前的事务中
                ThreadContext.sqlRecodes.set(new ArrayList<>());
                ThreadContext.globalId.set(branchTransaction.getGlobalId());//将该全局事务的id添加到当前的线程中
                ThreadContext.isDscTransaction.set(true);
                ThreadContext.branchTransaction.set(branchTransaction);
                NotDoneSqlLog notDoneSqlLog = notDoneSqlLogUtil.buildUndoLogByThread();//建立localLog
                this.addLogToDatabase(notDoneSqlLog);
                this.buildLocalTransaction(branchTransaction);//创建一个本地事务.并将其与本地事务关联
        }
        //修改存储在服务端中的本地事务状态
        public void updateStatus(BranchTransaction branchTransaction, BranchStatus branchStatus) {
             branchTransaction.setStatus(branchStatus);
             branchTransactRpc.updateBranchTransactionStatus(branchTransaction);//修改分支事务的状
         }
        public void updateStatusWithNotice(BranchTransaction branchTransaction, BranchStatus branchStatus) throws InterruptedException {
            branchTransaction.setStatus(branchStatus);
            branchTransactRpc.updateBranchTransactionStatusWithNotice(branchTransaction);
        }
        public void updateStatus(BranchStatus branchStatus){
            BranchTransaction branchTransaction=ThreadContext.branchTransaction.get();
            updateStatus(branchTransaction, branchStatus);
        }
    //修改并且令服务器检查全局事务是否成功或者失败,从而进行全局事务通知
        public void updateStatusWithNotice(BranchStatus branchStatus) throws InterruptedException {
             BranchTransaction branchTransaction=ThreadContext.branchTransaction.get();
             updateStatusWithNotice(branchTransaction, branchStatus);
        }
    //新建一个本地事务,并将其绑定到当前的线程中
        private Connection buildLocalTransaction(BranchTransaction branchTransaction) throws  SQLException
        {
            ConnectionWrapper connection=new ConnectionWrapper(transactionManager.getDataSource().getConnection());
            ThreadContext.connection.set(connection);
            connection.setAutoCommit(false);
            ConnectionHolder connectionHolder=new ConnectionHolder(connection);
            TransactionSynchronizationManager.bindResource(transactionManager.getDataSource(),connectionHolder);
            localTransactionMaps.put(branchTransaction.getBranchId(),new TransactContainer(connection));
            return connection;
        }

        public ConnectionWrapper getLocalTransaction(String branchId)
        {
            TransactContainer transactContainer=localTransactionMaps.get(branchId);
            if(transactContainer==null) return null;
            return  transactContainer.getConnection();
        }
        public void removeLocalTransaction(String branchId){
            localTransactionMaps.remove(branchId);
        }
        //回滚事务
        public void rollBack(String branchId) throws SQLException {
            ConnectionWrapper connection=getLocalTransaction(branchId);
            if (connection==null) return;
            if (connection.isClosed()){
                localTransactionMaps.remove(branchId);
                return;
            };
            try {
                connection.rollback();
                connection.setAutoCommit(true);
                LocalTransactionManager.instance.deleteUnDoLogFromDatabase(connection,branchId);//从数据库中删除该未完成的事务
                BranchTransaction branchTransaction= BranchTransaction.builder().branchId(branchId).status(BranchStatus.rollback).build();
                branchTransactRpc.updateBranchTransactionStatus(branchTransaction);//更新服务端的分支事务状态 为回滚
            } catch (SQLException e) {
                throw new RuntimeException(e);
            } finally {
                connection.close();
                localTransactionMaps.remove(branchId);
            }

        }
        //提交事务
        public void commit(String branchId) throws SQLException {
            ConnectionWrapper connection=getLocalTransaction(branchId);
            if (connection==null) return;
            if (connection.isClosed()){
                localTransactionMaps.remove(branchId);
                return;
            };
            try {
                LocalTransactionManager.instance.deleteUnDoLogFromDatabase(connection,branchId);//从数据库中删除该未完成的事务
                connection.commit();//提交事务 与上一个为同一事务 确保原子性
                BranchTransaction branchTransaction= BranchTransaction.builder().branchId(branchId).status(BranchStatus.commit).build();
                branchTransactRpc.updateBranchTransactionStatus(branchTransaction);//更新服务端的分支事务状态
            } catch (SQLException e) {
                throw new RuntimeException(e);
            } finally {
                connection.close();
                localTransactionMaps.remove(branchId);
            }
        }
        public void updateLogOfDBS(NotDoneSqlLog notDoneSqlLog){
            String sql = "update not_done_sql_log set logs= ? where branch_id= ?";
            Connection connection = null;
            PreparedStatement preparedStatement = null;
            try {
                connection = transactionManager.getDataSource().getConnection();
                preparedStatement = connection.prepareStatement(sql);
                //设置参数
                preparedStatement.setString(1, notDoneSqlLog.getLogs());
                preparedStatement.setString(2, notDoneSqlLog.getBranchId());
                preparedStatement.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            finally {
                // 关闭资源
                if (preparedStatement != null) {
                    try {
                        preparedStatement.close();
                    } catch (SQLException e) {
                    }
                }
                if (connection != null) {
                    try {
                        connection.close();
                    } catch (SQLException e) {
                    }
                }
            }
        }

		//添加undoSqlLog
        public void addLogToDatabase(NotDoneSqlLog notDoneSqlLog) throws SQLException {
            String sql = "insert into not_done_sql_log(global_id, branch_id, begin_time, request_uri,application_name,server_address,logs) values (?, ? , ? , ?, ?, ?, ?)";
            Connection connection = null;
            PreparedStatement preparedStatement = null;
            try {
                connection = transactionManager.getDataSource().getConnection();
                preparedStatement = connection.prepareStatement(sql);
                // 设置参数
                preparedStatement.setString(1, notDoneSqlLog.getGlobalId());
                preparedStatement.setString(2, notDoneSqlLog.getBranchId());
                preparedStatement.setTimestamp(3, new Timestamp(TimeUtil.strToDate(notDoneSqlLog.getBeginTime()).getTime()));
                preparedStatement.setString(4, notDoneSqlLog.getRequestUri());
                preparedStatement.setString(5, notDoneSqlLog.getApplicationName());
                preparedStatement.setString(6, notDoneSqlLog.getServerAddress());
                preparedStatement.setString(7, notDoneSqlLog.getLogs());
                // 执行插入操作
                preparedStatement.executeUpdate();
            } catch (SQLException e) {
                // 记录日志或其他处理逻辑
                throw new SQLException("Error inserting log into database", e);
            } finally {
                // 关闭资源
                if (preparedStatement != null) {
                    try {
                        preparedStatement.close();
                    } catch (SQLException e) {
                    }
                }
                if (connection != null) {
                    try {
                        connection.close();
                    } catch (SQLException e) {
                    }
                }
            }
    }
        public void deleteUnDoLogFromDatabase(ConnectionWrapper connection,String branchId) throws SQLException {
             String sql = "delete from not_done_sql_log where  branch_id = ?";
             PreparedStatement preparedStatement = null;
             try {
                 preparedStatement = connection.prepareStatementWithoutWrapper(sql);
                 // 设置参数
                 preparedStatement.setString(1, branchId);
                 preparedStatement.executeUpdate();
             }
             catch (SQLException e) {
                 // 记录日志或其他处理逻辑
                 throw new SQLException("Error deleting log from database", e);
             }
        }
}

```

###### 本地事务超时检查,防止锁被长时间占用

```java
package com.lgzClient.service;
import com.lgzClient.rpc.GlobalTransactRpc;
import com.lgzClient.types.TransactContainer;
import com.lgzClient.types.sql.service.GlobalTransaction;
import com.lgzClient.types.status.GlobalStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.HashMap;

@Service
public class TimeOutConnectionHandler {
    @Autowired
    GlobalTransactRpc globalTransactRpc;
    @Autowired
    LocalTransactionManager localTransactionManager;
    //超时检查
    @Scheduled(fixedDelay = 3000)
    public void checkTimeOut() throws Exception {
        ArrayList<TransactContainer> transactContainers =localTransactionManager.getUnDoTransactions(3000);
        ArrayList<String> globalIds=new ArrayList<>();
        for (TransactContainer transactContainer : transactContainers) {
            String globalId=transactContainer.getBranchTransaction().getGlobalId();
            globalIds.add(globalId);
        }
        ArrayList<GlobalTransaction> globalTransactions=globalTransactRpc.getGlobalTransactions(globalIds).getData();
        HashMap<String ,GlobalTransaction> globalTransactionHashMap=new HashMap<>();
        for (GlobalTransaction globalTransaction : globalTransactions) {
            globalTransactionHashMap.put(globalTransaction.getGlobalId(),globalTransaction);
        }
        for (TransactContainer transactContainer : transactContainers){
            String globalId=transactContainer.getBranchTransaction().getGlobalId();
            String branchId=transactContainer.getBranchTransaction().getBranchId();
            if(globalTransactionHashMap.get(globalId).getStatus() == GlobalStatus.fail){
                localTransactionManager.rollBack(branchId);
            }
            else if(globalTransactionHashMap.get(globalId).getStatus()== GlobalStatus.success){
                localTransactionManager.commit(branchId);
            }
        }
    }
}
```
