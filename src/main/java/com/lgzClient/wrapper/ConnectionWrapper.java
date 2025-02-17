package com.lgzClient.wrapper;

import java.sql.*;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

// 自定义 Connection 包装类
public class ConnectionWrapper implements Connection {
    private static final Logger LOGGER = Logger.getLogger(ConnectionWrapper.class.getName());
    private final Connection targetConnection;
    public ConnectionWrapper(Connection targetConnection) {
        this.targetConnection = targetConnection;
    }
    public Statement createStatementWithoutWrapper() throws SQLException {
        return targetConnection.createStatement();
    }
    public PreparedStatement prepareStatementWithoutWrapper(String sql) throws SQLException {
        // 在创建 PreparedStatement 之前添加额外操作
        return targetConnection.prepareStatement(sql);
    }
    @Override
    public Statement createStatement() throws SQLException {
        // 在创建 Statement 之前添加额外操作
        return new StatementWrapper(targetConnection.createStatement());
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        // 在创建 PreparedStatement 之前添加额外操作
        return new PreparedStatementWrapper(targetConnection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS),sql);
    }

    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
        // 在创建 CallableStatement 之前添加额外操作
        return targetConnection.prepareCall(sql);
    }

    @Override
    public String nativeSQL(String sql) throws SQLException {
        return targetConnection.nativeSQL(sql);
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
      targetConnection.setAutoCommit(autoCommit);
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        return targetConnection.getAutoCommit();
    }

    @Override
    public void commit() throws SQLException {
        targetConnection.commit();
    }

    @Override
    public void rollback() throws SQLException {
        targetConnection.rollback();
    }

    // 重写其他 Connection 接口的方法，直接调用原始 Connection 对象的相应方法
    @Override
    public void close() throws SQLException {
        targetConnection.close();
    }

    @Override
    public boolean isClosed() throws SQLException {
        return targetConnection.isClosed();
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        return targetConnection.getMetaData();
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
        targetConnection.setReadOnly(readOnly);
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        return targetConnection.isReadOnly();
    }

    @Override
    public void setCatalog(String catalog) throws SQLException {
        targetConnection.setCatalog(catalog);
    }

    @Override
    public String getCatalog() throws SQLException {
        return targetConnection.getCatalog();
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException {
        targetConnection.setTransactionIsolation(level);
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        return targetConnection.getTransactionIsolation();
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return targetConnection.getWarnings();
    }

    @Override
    public void clearWarnings() throws SQLException {
        targetConnection.clearWarnings();
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
          return new StatementWrapper(targetConnection.createStatement(resultSetType, resultSetConcurrency)) ;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
         return new PreparedStatementWrapper(targetConnection.prepareStatement(sql, resultSetType, resultSetConcurrency),sql);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
       return targetConnection.prepareCall(sql, resultSetType, resultSetConcurrency);
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        return  targetConnection.getTypeMap();
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
         targetConnection.setTypeMap(map);
    }

    @Override
    public void setHoldability(int holdability) throws SQLException {
        targetConnection.setHoldability(holdability);
    }

    @Override
    public int getHoldability() throws SQLException {
        return targetConnection.getHoldability();
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        return targetConnection.setSavepoint();
    }

    @Override
    public Savepoint setSavepoint(String name) throws SQLException {
        return setSavepoint(name);
    }

    @Override
    public void rollback(Savepoint savepoint) throws SQLException {
         targetConnection.rollback(savepoint);
    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        targetConnection.releaseSavepoint(savepoint);
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
       return new StatementWrapper(targetConnection.createStatement(resultSetType,resultSetConcurrency,resultSetHoldability));
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return new PreparedStatementWrapper(targetConnection.prepareStatement(sql,resultSetType,resultSetConcurrency,resultSetHoldability),sql) ;
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return targetConnection.prepareCall(sql,resultSetType,resultSetConcurrency,resultSetHoldability);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
         return new PreparedStatementWrapper(targetConnection.prepareStatement(sql,autoGeneratedKeys),sql);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        return new PreparedStatementWrapper(targetConnection.prepareStatement(sql,columnIndexes),sql) ;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        return new PreparedStatementWrapper(targetConnection.prepareStatement(sql,columnNames),sql) ;
    }

    @Override
    public Clob createClob() throws SQLException {
        return targetConnection.createClob();
    }

    @Override
    public Blob createBlob() throws SQLException {
        return targetConnection.createBlob();
    }

    @Override
    public NClob createNClob() throws SQLException {
        return targetConnection.createNClob();
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        return targetConnection.createSQLXML();
    }

    @Override
    public boolean isValid(int timeout) throws SQLException {
        return targetConnection.isValid(timeout);
    }

    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException {
         targetConnection.setClientInfo(name,value);
    }

    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        targetConnection.setClientInfo(properties);
    }

    @Override
    public String getClientInfo(String name) throws SQLException {
        return targetConnection.getClientInfo(name);
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        return targetConnection.getClientInfo();
    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        return targetConnection.createArrayOf(typeName,elements);
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        return targetConnection.createStruct(typeName,attributes);
    }

    @Override
    public void setSchema(String schema) throws SQLException {
         targetConnection.setSchema(schema);
    }

    @Override
    public String getSchema() throws SQLException {
        return targetConnection.getSchema();
    }

    @Override
    public void abort(Executor executor) throws SQLException {
         targetConnection.abort(executor);
    }

    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        targetConnection.setNetworkTimeout(executor,milliseconds);
    }

    @Override
    public int getNetworkTimeout() throws SQLException {
        return targetConnection.getNetworkTimeout();
    }


    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {

        return targetConnection.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return targetConnection.isWrapperFor(iface);
    }
}

