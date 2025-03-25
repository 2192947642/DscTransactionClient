package com.lgzClient.utils;

import com.lgzClient.types.DCSThreadContext;
import com.lgzClient.types.sql.client.NotDoneSqlLog;
import com.lgzClient.types.sql.recode.*;
import com.lgzClient.types.sql.service.BranchTransaction;
import com.lgzClient.wrapper.ConnectionWrapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;

@Component
public class NotDoneSqlLogUtil {
    @Autowired
    DataSourceTransactionManager transactionManager;

    public NotDoneSqlLog buildUndoSqlLogFromLocalBranchTransaction(BranchTransaction branchTransaction) {
        NotDoneSqlLog notDoneSqlLog = new NotDoneSqlLog();
        notDoneSqlLog.setBranchId(branchTransaction.getBranchId());
        notDoneSqlLog.setGlobalId(branchTransaction.getGlobalId());
        notDoneSqlLog.setBeginTime(branchTransaction.getBeginTime());
        notDoneSqlLog.setApplicationName(branchTransaction.getApplicationName());
        notDoneSqlLog.setServerAddress(branchTransaction.getServerAddress());
        return notDoneSqlLog;
    }


    public NotDoneSqlLog buildNotDoneLogByThread() {
        NotDoneSqlLog notDoneSqlLog = buildUndoSqlLogFromLocalBranchTransaction(DCSThreadContext.branchTransaction.get());
        String requestUri = RequestUtil.instance.getRequest().getRequestURI();//请求的接口路径
        notDoneSqlLog.setRequestUri(requestUri);
        notDoneSqlLog.setLogs(JsonUtil.objToJson(DCSThreadContext.sqlRecodes.get()));//记录的sql日志
        return notDoneSqlLog;
    }

    public void deleteUnDoLogFromDatabase(ConnectionWrapper connection, String branchId) throws SQLException {
        String sql = "delete from not_done_sql_log where  branch_id = ?";
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatementWithoutWrapper(sql);
            // 设置参数
            preparedStatement.setString(1, branchId);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            // 记录日志或其他处理逻辑
            throw new SQLException("Error deleting log from database", e);
        }
    }

    public void addLogToDatabase(NotDoneSqlLog notDoneSqlLog) throws SQLException {
        String sql = "insert into not_done_sql_log(global_id, branch_id, begin_time, request_uri,application_name,server_address,logs) values (?, ? , ? , ?, ?, ?, ?)";
        PreparedStatement preparedStatement = null;
        Connection connection = null;
        try {
            connection = transactionManager.getDataSource().getConnection();
            preparedStatement = connection.prepareStatement(sql);
            // 设置参数
            preparedStatement.setString(1, notDoneSqlLog.getGlobalId());
            preparedStatement.setString(2, notDoneSqlLog.getBranchId());
            preparedStatement.setTimestamp(3, Timestamp.valueOf(TimeUtil.strToDate(notDoneSqlLog.getBeginTime())));
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

            try {
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
            } catch (SQLException e) {
            } finally {
                if (connection != null) {
                    try {
                        connection.close();
                    } catch (SQLException e) {
                    }
                }
            }
        }


    }

    public void updateLogOfDBS(NotDoneSqlLog notDoneSqlLog) {
        String sql = "update not_done_sql_log set logs= ? where branch_id= ?";
        PreparedStatement preparedStatement = null;
        Connection connection = null;
        try {
            connection = transactionManager.getDataSource().getConnection();
            preparedStatement = connection.prepareStatement(sql);
            //设置参数
            preparedStatement.setString(1, notDoneSqlLog.getLogs());
            preparedStatement.setString(2, notDoneSqlLog.getBranchId());
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            // 关闭资源
            try {
                if (preparedStatement != null)
                    preparedStatement.close();
            } catch (SQLException e) {
            } finally {
                if (connection != null) {
                    try {
                        connection.close();
                    } catch (SQLException e) {
                    }
                }
            }
        }


    }

    public ArrayList<Object> getRecodesByUndoLog(NotDoneSqlLog notDoneSqlLog) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        ArrayList<Object> recodes = new ArrayList<>();
        ArrayList<String> logs = JsonUtil.jsonToObject(notDoneSqlLog.getBranchId(), ArrayList.class);
        for (String log : logs) {
            HashMap<String, String> hashMap = JsonUtil.jsonToObject(log, HashMap.class);
            if (hashMap.get("sqlType").equals(SqlType.insert.name())) {
                InsertRecode insertRecode = BeanMapUtil.mapToBean(hashMap, InsertRecode.class);
                recodes.add(insertRecode);
            } else if (hashMap.get("sqlType").equals(SqlType.delete.name())) {
                DeleteRecode deleteRecode = BeanMapUtil.mapToBean(hashMap, DeleteRecode.class);
                recodes.add(deleteRecode);
            } else if (hashMap.get("sqlType").equals(SqlType.update.name())) {
                UpdateRecode updateRecode = BeanMapUtil.mapToBean(hashMap, UpdateRecode.class);
                recodes.add(updateRecode);
            } else if (hashMap.get("sqlType").equals(SqlType.select.name())) {
                SelectRecode selectRecode = BeanMapUtil.mapToBean(hashMap, SelectRecode.class);
                recodes.add(selectRecode);
            }
        }
        return recodes;
    }
}
