package com.lgzClient.wrapper;

import com.lgzClient.types.ThreadContext;
import com.lgzClient.types.sql.recode.*;
import com.lgzClient.utils.SqlUtil;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.ArrayList;
import java.util.Calendar;

public class PreparedStatementWrapper implements PreparedStatement {
    private PreparedStatement preparedStatement;
    private Statement selectStatement;
    private String originalSql;
    private ArrayList<String> batchSqls;
    private SqlUtil sqlUtil;
    public PreparedStatementWrapper(PreparedStatement preparedStatement,String sql) throws SQLException {
        this.preparedStatement = preparedStatement;
        this.sqlUtil=new SqlUtil();
        String selectSql=sqlUtil.buildSelectSql(sql);
        this.originalSql=sql;
        selectStatement=preparedStatement.getConnection().createStatement();
        this.batchSqls=new ArrayList<>();
    }

    @Override
    public ResultSet executeQuery() throws SQLException {
        String finalSql=sqlUtil.getFinalSql(preparedStatement,originalSql);
        SelectRecode selectRecode=new SelectRecode(finalSql);
        ThreadContext.sqlRecodes.get().add(selectRecode);//将执行的这条select进行插入
        return preparedStatement.executeQuery();
    }

    @Override
    public int executeUpdate() throws SQLException {
        SqlType sqlType = sqlUtil.getSqlType(originalSql);
        String finalSql=sqlUtil.getFinalSql(preparedStatement,originalSql);
        String selectSql=sqlUtil.buildSelectSql(finalSql);
        if (sqlType == SqlType.insert) {
            InsertRecode insertRecode = new InsertRecode(finalSql);
            int number = preparedStatement.executeUpdate();
            ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
            if (generatedKeys.next()) {
                insertRecode.getGeneratedKeys().add(generatedKeys.getInt(1));
            }
            ThreadContext.sqlRecodes.get().add(insertRecode);
            return number;
        } else if (sqlType == SqlType.update) {
            UpdateRecode updateRecode = new UpdateRecode(finalSql);
            updateRecode.setBefore(sqlUtil.getResultSetJson(selectStatement.executeQuery(selectSql)));

            int number = preparedStatement.executeUpdate();
            updateRecode.setAfter(sqlUtil.getResultSetJson(selectStatement.executeQuery(selectSql)));
            ThreadContext.sqlRecodes.get().add(updateRecode);
            return number;
        } else if (sqlType == SqlType.delete) {
            DeleteRecode deleteRecode = new DeleteRecode(finalSql);
            deleteRecode.setBefore(sqlUtil.getResultSetJson(selectStatement.executeQuery(selectSql)));
            int number = preparedStatement.executeUpdate();
            ThreadContext.sqlRecodes.get().add(deleteRecode);
            return number;
        }
        return preparedStatement.executeUpdate();
    }

    @Override
    public void setNull(int parameterIndex, int sqlType) throws SQLException {
        preparedStatement.setNull(parameterIndex, sqlType);
    }

    @Override
    public void setBoolean(int parameterIndex, boolean x) throws SQLException {
        preparedStatement.setBoolean(parameterIndex, x);
    }

    @Override
    public void setByte(int parameterIndex, byte x) throws SQLException {
        preparedStatement.setByte(parameterIndex, x);
    }

    @Override
    public void setShort(int parameterIndex, short x) throws SQLException {
        preparedStatement.setShort(parameterIndex, x);
    }

    @Override
    public void setInt(int parameterIndex, int x) throws SQLException {
        preparedStatement.setInt(parameterIndex, x);
    }

    @Override
    public void setLong(int parameterIndex, long x) throws SQLException {
        preparedStatement.setLong(parameterIndex, x);
    }

    @Override
    public void setFloat(int parameterIndex, float x) throws SQLException {
        preparedStatement.setFloat(parameterIndex, x);
    }

    @Override
    public void setDouble(int parameterIndex, double x) throws SQLException {
        preparedStatement.setDouble(parameterIndex, x);
    }

    @Override
    public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
        preparedStatement.setBigDecimal(parameterIndex, x);
    }

    @Override
    public void setString(int parameterIndex, String x) throws SQLException {
        preparedStatement.setString(parameterIndex, x);
    }

    @Override
    public void setBytes(int parameterIndex, byte[] x) throws SQLException {
        preparedStatement.setBytes(parameterIndex, x);
    }

    @Override
    public void setDate(int parameterIndex, Date x) throws SQLException {
        preparedStatement.setDate(parameterIndex, x);
    }

    @Override
    public void setTime(int parameterIndex, Time x) throws SQLException {
        preparedStatement.setTime(parameterIndex, x);
    }

    @Override
    public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
        preparedStatement.setTimestamp(parameterIndex, x);
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException {
        preparedStatement.setAsciiStream(parameterIndex, x, length);
    }

    @Override
    public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException {
        preparedStatement.setUnicodeStream(parameterIndex, x, length);
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {
        preparedStatement.setBinaryStream(parameterIndex, x, length);
    }

    @Override
    public void clearParameters() throws SQLException {
        preparedStatement.clearParameters();
    }

    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
        preparedStatement.setObject(parameterIndex, x, targetSqlType);
    }

    @Override
    public void setObject(int parameterIndex, Object x) throws SQLException {
        preparedStatement.setObject(parameterIndex, x);
    }

    @Override
    public boolean execute() throws SQLException {
        SqlType sqlType=sqlUtil.getSqlType(originalSql);
        String finalSql=sqlUtil.getFinalSql(preparedStatement,originalSql);
        String selectSql=sqlUtil.buildSelectSql(finalSql);
        if(sqlType == SqlType.select){
            SelectRecode selectRecode=new SelectRecode(finalSql);
            ThreadContext.sqlRecodes.get().add(selectRecode);
            return preparedStatement.execute();
        }
        if(sqlType == SqlType.update){
            UpdateRecode updateRecode=new UpdateRecode(finalSql);
            updateRecode.setBefore(sqlUtil.getResultSetJson(selectStatement.executeQuery(selectSql)));
            boolean flag= preparedStatement.execute();
            updateRecode.setAfter(sqlUtil.getResultSetJson(selectStatement.executeQuery(selectSql)));
            ThreadContext.sqlRecodes.get().add(updateRecode);
            return flag;
        }
        if(sqlType == SqlType.insert){
            InsertRecode insertRecode=new InsertRecode(finalSql);
            boolean flag= preparedStatement.execute();
            ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
            if (generatedKeys.next()) {
                insertRecode.getGeneratedKeys().add(generatedKeys.getInt(1));
            }
            ThreadContext.sqlRecodes.get().add(insertRecode);
            return flag;
        }
        if(sqlType == SqlType.delete){
            DeleteRecode deleteRecode=new DeleteRecode(finalSql);
            deleteRecode.setBefore(sqlUtil.getResultSetJson(selectStatement.executeQuery(selectSql)));
            ThreadContext.sqlRecodes.get().add(deleteRecode);
            boolean flag= preparedStatement.execute();
            return flag;
        }
        return preparedStatement.execute();

    }

    @Override
    public void addBatch() throws SQLException {
        String finalSql=sqlUtil.getFinalSql(preparedStatement,originalSql);
        preparedStatement.addBatch();
        batchSqls.add(finalSql);
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {
        preparedStatement.setCharacterStream(parameterIndex, reader, length);
    }

    @Override
    public void setRef(int parameterIndex, Ref x) throws SQLException {
        preparedStatement.setRef(parameterIndex, x);
    }

    @Override
    public void setBlob(int parameterIndex, Blob x) throws SQLException {
        preparedStatement.setBlob(parameterIndex, x);
    }

    @Override
    public void setClob(int parameterIndex, Clob x) throws SQLException {
        preparedStatement.setClob(parameterIndex, x);
    }

    @Override
    public void setArray(int parameterIndex, Array x) throws SQLException {
        preparedStatement.setArray(parameterIndex, x);
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        return preparedStatement.getMetaData();
    }

    @Override
    public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {
        preparedStatement.setDate(parameterIndex, x, cal);
    }

    @Override
    public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {
        preparedStatement.setTime(parameterIndex, x, cal);
    }

    @Override
    public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {
        preparedStatement.setTimestamp(parameterIndex, x, cal);
    }

    @Override
    public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException {
        preparedStatement.setNull(parameterIndex, sqlType, typeName);
    }

    @Override
    public void setURL(int parameterIndex, URL x) throws SQLException {
        preparedStatement.setURL(parameterIndex, x);
    }

    @Override
    public ParameterMetaData getParameterMetaData() throws SQLException {
        return preparedStatement.getParameterMetaData();
    }

    @Override
    public void setRowId(int parameterIndex, RowId x) throws SQLException {
        preparedStatement.setRowId(parameterIndex, x);
    }

    @Override
    public void setNString(int parameterIndex, String value) throws SQLException {
        preparedStatement.setNString(parameterIndex, value);
    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {
        preparedStatement.setNCharacterStream(parameterIndex, value, length);
    }

    @Override
    public void setNClob(int parameterIndex, NClob value) throws SQLException {
        preparedStatement.setNClob(parameterIndex, value);
    }

    @Override
    public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {
        preparedStatement.setClob(parameterIndex, reader, length);
    }

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {
        preparedStatement.setBlob(parameterIndex, inputStream, length);
    }

    @Override
    public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {
        preparedStatement.setNClob(parameterIndex, reader, length);
    }

    @Override
    public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
        preparedStatement.setSQLXML(parameterIndex, xmlObject);
    }

    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) throws SQLException {
        preparedStatement.setObject(parameterIndex, x, targetSqlType, scaleOrLength);
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {
        preparedStatement.setAsciiStream(parameterIndex, x, length);
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {
        preparedStatement.setBinaryStream(parameterIndex, x, length);
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {
        preparedStatement.setCharacterStream(parameterIndex, reader, length);
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {
        preparedStatement.setAsciiStream(parameterIndex, x);
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {
        preparedStatement.setBinaryStream(parameterIndex, x);
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
        preparedStatement.setCharacterStream(parameterIndex, reader);
    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {
        preparedStatement.setNCharacterStream(parameterIndex, value);
    }

    @Override
    public void setClob(int parameterIndex, Reader reader) throws SQLException {
        preparedStatement.setClob(parameterIndex, reader);
    }

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {
        preparedStatement.setBlob(parameterIndex, inputStream);
    }

    @Override
    public void setNClob(int parameterIndex, Reader reader) throws SQLException {
        preparedStatement.setNClob(parameterIndex, reader);
    }

    @Override
    public ResultSet executeQuery(String sql) throws SQLException {
        SelectRecode selectRecode = new SelectRecode(sql);
        ThreadContext.sqlRecodes.get().add(selectRecode);
        return preparedStatement.executeQuery(sql);
    }

    @Override
    public int executeUpdate(String sql) throws SQLException {
        SqlType sqlType=sqlUtil.getSqlType(sql);
        String selectSql=sqlUtil.buildSelectSql(sql);
        if(sqlType == SqlType.update){
            UpdateRecode updateRecode=new UpdateRecode(sql);
            updateRecode.setBefore(sqlUtil.getResultSetJson(selectStatement.executeQuery(selectSql)));
            int number= preparedStatement.executeUpdate(sql);
            updateRecode.setAfter(sqlUtil.getResultSetJson(selectStatement.executeQuery(selectSql)));
            ThreadContext.sqlRecodes.get().add(updateRecode);
            return number;
        }
        if(sqlType == SqlType.insert){
            InsertRecode insertRecode=new InsertRecode(sql);
            int number= preparedStatement.executeUpdate(sql);
            ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
            if (generatedKeys.next()) {
                insertRecode.getGeneratedKeys().add(generatedKeys.getInt(1));
            }
            ThreadContext.sqlRecodes.get().add(insertRecode);
            return number;
        }
        if(sqlType == SqlType.delete){
            DeleteRecode deleteRecode=new DeleteRecode(sql);
            deleteRecode.setBefore(sqlUtil.getResultSetJson(selectStatement.executeQuery(selectSql)));
            ThreadContext.sqlRecodes.get().add(deleteRecode);
            int number= preparedStatement.executeUpdate(sql);
            return number;
        }
        return preparedStatement.executeUpdate(sql);
    }

    @Override
    public void close() throws SQLException {
        preparedStatement.close();
    }

    @Override
    public int getMaxFieldSize() throws SQLException {
        return preparedStatement.getMaxFieldSize();
    }

    @Override
    public void setMaxFieldSize(int max) throws SQLException {
        preparedStatement.setMaxFieldSize(max);
    }

    @Override
    public int getMaxRows() throws SQLException {
        return preparedStatement.getMaxRows();
    }

    @Override
    public void setMaxRows(int max) throws SQLException {
        preparedStatement.setMaxRows(max);
    }

    @Override
    public void setEscapeProcessing(boolean enable) throws SQLException {
        preparedStatement.setEscapeProcessing(enable);
    }

    @Override
    public int getQueryTimeout() throws SQLException {
        return preparedStatement.getQueryTimeout();
    }

    @Override
    public void setQueryTimeout(int seconds) throws SQLException {
        preparedStatement.setQueryTimeout(seconds);
    }

    @Override
    public void cancel() throws SQLException {
        preparedStatement.cancel();
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return preparedStatement.getWarnings();
    }

    @Override
    public void clearWarnings() throws SQLException {
        preparedStatement.clearWarnings();
    }

    @Override
    public void setCursorName(String name) throws SQLException {
        preparedStatement.setCursorName(name);
    }

    @Override
    public boolean execute(String sql) throws SQLException {
        SqlType sqlType=sqlUtil.getSqlType(sql);
        String selectSql=sqlUtil.buildSelectSql(sql);
        if(sqlType == SqlType.select){
            SelectRecode selectRecode = new SelectRecode(sql);
            ThreadContext.sqlRecodes.get().add(selectRecode);
            return preparedStatement.execute(sql);
        }
        if(sqlType == SqlType.insert){
            InsertRecode insertRecode = new InsertRecode(sql);
            boolean flag = preparedStatement.execute(sql, Statement.RETURN_GENERATED_KEYS);
            ResultSet generatedKeys = preparedStatement.getResultSet();
            if (generatedKeys.next()) {
                insertRecode.getGeneratedKeys().add(generatedKeys.getInt(1));
            }
            ThreadContext.sqlRecodes.get().add(insertRecode);
            return flag;
        }
        if(sqlType==SqlType.update){
            UpdateRecode updateRecode = new UpdateRecode(sql);
            updateRecode.setBefore(sqlUtil.getResultSetJson(selectStatement.executeQuery(selectSql)));
            boolean flag= preparedStatement.execute(sql);
            updateRecode.setAfter(sqlUtil.getResultSetJson(selectStatement.executeQuery(selectSql)));
            ThreadContext.sqlRecodes.get().add(updateRecode);
            return flag;
        }
        if(sqlType==SqlType.delete){
            DeleteRecode deleteRecode = new DeleteRecode(sql);
            deleteRecode.setBefore(sqlUtil.getResultSetJson(selectStatement.executeQuery(selectSql)));
            boolean flag= preparedStatement.execute(sql);
            ThreadContext.sqlRecodes.get().add(deleteRecode);
            return flag;
        }
        return preparedStatement.execute(sql);
    }

    @Override
    public ResultSet getResultSet() throws SQLException {
        return preparedStatement.getResultSet();
    }

    @Override
    public int getUpdateCount() throws SQLException {
        return preparedStatement.getUpdateCount();
    }

    @Override
    public boolean getMoreResults() throws SQLException {
        return preparedStatement.getMoreResults();
    }

    @Override
    public void setFetchDirection(int direction) throws SQLException {
        preparedStatement.setFetchDirection(direction);
    }

    @Override
    public int getFetchDirection() throws SQLException {
        return preparedStatement.getFetchDirection();
    }

    @Override
    public void setFetchSize(int rows) throws SQLException {
        preparedStatement.setFetchSize(rows);
    }

    @Override
    public int getFetchSize() throws SQLException {
        return preparedStatement.getFetchSize();
    }

    @Override
    public int getResultSetConcurrency() throws SQLException {
        return preparedStatement.getResultSetConcurrency();
    }

    @Override
    public int getResultSetType() throws SQLException {
        return preparedStatement.getResultSetType();
    }

    @Override
    public void addBatch(String sql) throws SQLException {
        preparedStatement.addBatch(sql);
        batchSqls.add(sql);
    }

    @Override
    public void clearBatch() throws SQLException {
        preparedStatement.clearBatch();
        batchSqls.clear();
    }

    @Override
    public int[] executeBatch() throws SQLException {

        return preparedStatement.executeBatch();
    }

    @Override
    public Connection getConnection() throws SQLException {
        return preparedStatement.getConnection();
    }

    @Override
    public boolean getMoreResults(int current) throws SQLException {
        return preparedStatement.getMoreResults(current);
    }

    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        return preparedStatement.getGeneratedKeys();
    }

    @Override
    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        SqlType sqlType=sqlUtil.getSqlType(sql);
        String selectSql=sqlUtil.buildSelectSql(sql);
        if(sqlType == SqlType.insert){//如果是插入的话那么就返回插入的id
            InsertRecode insertRecode=new InsertRecode(sql);
            int number= preparedStatement.executeUpdate(sql, autoGeneratedKeys);
            if(autoGeneratedKeys==Statement.RETURN_GENERATED_KEYS){
                ResultSet generatedKeys = preparedStatement.getResultSet();
                if (generatedKeys.next()) {
                    insertRecode.getGeneratedKeys().add(generatedKeys.getInt(1));
                }
            }
            ThreadContext.sqlRecodes.get().add(insertRecode);
            return number;
        }
        if(sqlType == SqlType.update){
            UpdateRecode updateRecode=new UpdateRecode(sql);
            updateRecode.setBefore(sqlUtil.getResultSetJson(selectStatement.executeQuery(selectSql)));
            int number= preparedStatement.executeUpdate(sql, autoGeneratedKeys);
            updateRecode.setAfter(sqlUtil.getResultSetJson(selectStatement.executeQuery(selectSql)));
            ThreadContext.sqlRecodes.get().add(updateRecode);
            return number;
        }
        if(sqlType == SqlType.delete){//如果sql的类型是删除
            DeleteRecode deleteRecode=new DeleteRecode(sql);
            deleteRecode.setBefore(sqlUtil.getResultSetJson(selectStatement.executeQuery(selectSql)));
            ThreadContext.sqlRecodes.get().add(deleteRecode);
            int number= preparedStatement.executeUpdate(sql,autoGeneratedKeys);
            return number;
        }
        return preparedStatement.executeUpdate(sql, autoGeneratedKeys);
    }

    @Override
    public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
        SqlType sqlType=sqlUtil.getSqlType(sql);
        String selectSql=sqlUtil.buildSelectSql(sql);
        if(sqlType == SqlType.insert){//如果是插入的话那么就返回插入的id
            InsertRecode insertRecode=new InsertRecode(sql);
            int number= preparedStatement.executeUpdate(sql, columnIndexes);
            ResultSet generatedKeys = preparedStatement.getResultSet();
            if (generatedKeys.next()) {
                insertRecode.getGeneratedKeys().add(generatedKeys.getInt(1));
            }
            ThreadContext.sqlRecodes.get().add(insertRecode);
            return number;
        }
        if(sqlType == SqlType.update){
            UpdateRecode updateRecode=new UpdateRecode(sql);
            updateRecode.setBefore(sqlUtil.getResultSetJson(selectStatement.executeQuery(selectSql)));
            int number= preparedStatement.executeUpdate(sql, columnIndexes);
            updateRecode.setAfter(sqlUtil.getResultSetJson(selectStatement.executeQuery(selectSql)));
            ThreadContext.sqlRecodes.get().add(updateRecode);
            return number;
        }
        if(sqlType == SqlType.delete){//如果sql的类型是删除
            DeleteRecode deleteRecode=new DeleteRecode(sql);
            deleteRecode.setBefore(sqlUtil.getResultSetJson(selectStatement.executeQuery(selectSql)));
            ThreadContext.sqlRecodes.get().add(deleteRecode);
            int number= preparedStatement.executeUpdate(sql,columnIndexes);
            return number;
        }
        return preparedStatement.executeUpdate(sql, columnIndexes);
    }

    @Override
    public int executeUpdate(String sql, String[] columnNames) throws SQLException {
        SqlType sqlType=sqlUtil.getSqlType(sql);
        String selectSql=sqlUtil.buildSelectSql(sql);
        if(sqlType == SqlType.insert){//如果是插入的话那么就返回插入的id
            InsertRecode insertRecode=new InsertRecode(sql);
            int number= preparedStatement.executeUpdate(sql, columnNames);
            ResultSet generatedKeys = preparedStatement.getResultSet();
            if (generatedKeys.next()) {
                insertRecode.getGeneratedKeys().add(generatedKeys.getInt(1));
            }
            ThreadContext.sqlRecodes.get().add(insertRecode);
            return number;
        }
        if(sqlType == SqlType.update){
            UpdateRecode updateRecode=new UpdateRecode(sql);
            updateRecode.setBefore(sqlUtil.getResultSetJson(selectStatement.executeQuery(selectSql)));
            int number= preparedStatement.executeUpdate(sql, columnNames);
            updateRecode.setAfter(sqlUtil.getResultSetJson(selectStatement.executeQuery(selectSql)));
            ThreadContext.sqlRecodes.get().add(updateRecode);
            return number;
        }
        if(sqlType == SqlType.delete){//如果sql的类型是删除
            DeleteRecode deleteRecode=new DeleteRecode(sql);
            deleteRecode.setBefore(sqlUtil.getResultSetJson(selectStatement.executeQuery(selectSql)));
            ThreadContext.sqlRecodes.get().add(deleteRecode);
            int number= preparedStatement.executeUpdate(sql,columnNames);
            return number;
        }
        return preparedStatement.executeUpdate(sql, columnNames);
    }

    @Override
    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
        SqlType sqlType=sqlUtil.getSqlType(sql);
        String selectSql=sqlUtil.buildSelectSql(sql);
        if(sqlType == SqlType.select){
            SelectRecode selectRecode=new SelectRecode(sql);
            ThreadContext.sqlRecodes.get().add(selectRecode);
            return selectStatement.execute(sql, autoGeneratedKeys);
        }
        if(sqlType == SqlType.insert){//如果是插入的话那么就返回插入的id
            InsertRecode insertRecode=new InsertRecode(sql);
            boolean flag= preparedStatement.execute(sql, autoGeneratedKeys);
            if(autoGeneratedKeys==Statement.RETURN_GENERATED_KEYS){
                ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
                if (generatedKeys.next()) {
                    insertRecode.getGeneratedKeys().add(generatedKeys.getInt(1));
                }
            }
            ThreadContext.sqlRecodes.get().add(insertRecode);
            return flag;
        }
        if(sqlType == SqlType.update){
            UpdateRecode updateRecode=new UpdateRecode(sql);
            updateRecode.setBefore(sqlUtil.getResultSetJson(selectStatement.executeQuery(selectSql)));
            boolean flag= preparedStatement.execute(sql, autoGeneratedKeys);
            updateRecode.setAfter(sqlUtil.getResultSetJson(selectStatement.executeQuery(selectSql)));
            ThreadContext.sqlRecodes.get().add(updateRecode);
            return flag;
        }
        if(sqlType == SqlType.delete){//如果sql的类型是删除
            DeleteRecode deleteRecode=new DeleteRecode(sql);
            deleteRecode.setBefore(sqlUtil.getResultSetJson(selectStatement.executeQuery(selectSql)));
            ThreadContext.sqlRecodes.get().add(deleteRecode);
            boolean flag= preparedStatement.execute(sql,autoGeneratedKeys);
            return flag;
        }
        return preparedStatement.execute(sql, autoGeneratedKeys);
    }

    @Override
    public boolean execute(String sql, int[] columnIndexes) throws SQLException {
        SqlType sqlType=sqlUtil.getSqlType(sql);
        String selectSql=sqlUtil.buildSelectSql(sql);
        if(sqlType == SqlType.select){
            SelectRecode selectRecode=new SelectRecode(sql);
            ThreadContext.sqlRecodes.get().add(selectRecode);
            return selectStatement.execute(sql, columnIndexes);
        }
        if(sqlType == SqlType.insert){//如果是插入的话那么就返回插入的id
            InsertRecode insertRecode=new InsertRecode(sql);
            boolean flag= preparedStatement.execute(sql, columnIndexes);
            ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
            if (generatedKeys.next()) {
                insertRecode.getGeneratedKeys().add(generatedKeys.getInt(1));
            }
            ThreadContext.sqlRecodes.get().add(insertRecode);
            return flag;
        }
        if(sqlType == SqlType.update){
            UpdateRecode updateRecode=new UpdateRecode(sql);
            updateRecode.setBefore(sqlUtil.getResultSetJson(selectStatement.executeQuery(selectSql)));
            boolean flag= preparedStatement.execute(sql, columnIndexes);
            updateRecode.setAfter(sqlUtil.getResultSetJson(selectStatement.executeQuery(selectSql)));
            ThreadContext.sqlRecodes.get().add(updateRecode);
            return flag;
        }
        if(sqlType == SqlType.delete){//如果sql的类型是删除
            DeleteRecode deleteRecode=new DeleteRecode(sql);
            deleteRecode.setBefore(sqlUtil.getResultSetJson(selectStatement.executeQuery(selectSql)));
            ThreadContext.sqlRecodes.get().add(deleteRecode);
            boolean flag= preparedStatement.execute(sql,columnIndexes);
            return flag;
        }
        return preparedStatement.execute(sql, columnIndexes);
    }

    @Override
    public boolean execute(String sql, String[] columnNames) throws SQLException {
        SqlType sqlType=sqlUtil.getSqlType(sql);
        String selectSql=sqlUtil.buildSelectSql(sql);
        if(sqlType == SqlType.select){
            SelectRecode selectRecode=new SelectRecode(sql);
            ThreadContext.sqlRecodes.get().add(selectRecode);
            return selectStatement.execute(sql, columnNames);
        }
        if(sqlType == SqlType.insert){//如果是插入的话那么就返回插入的id
            InsertRecode insertRecode=new InsertRecode(sql);
            boolean flag= preparedStatement.execute(sql, columnNames);
            ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
            if (generatedKeys.next()) {
                insertRecode.getGeneratedKeys().add(generatedKeys.getInt(1));
            }
            ThreadContext.sqlRecodes.get().add(insertRecode);
            return flag;
        }
        if(sqlType == SqlType.update){
            UpdateRecode updateRecode=new UpdateRecode(sql);
            updateRecode.setBefore(sqlUtil.getResultSetJson(selectStatement.executeQuery(selectSql)));
            boolean flag= preparedStatement.execute(sql, columnNames);
            updateRecode.setAfter(sqlUtil.getResultSetJson(selectStatement.executeQuery(selectSql)));
            ThreadContext.sqlRecodes.get().add(updateRecode);
            return flag;
        }
        if(sqlType == SqlType.delete){//如果sql的类型是删除
            DeleteRecode deleteRecode=new DeleteRecode(sql);
            ResultSet resultSet = selectStatement.executeQuery(selectSql);

            deleteRecode.setBefore(sqlUtil.getResultSetJson(selectStatement.executeQuery(selectSql)));
            ThreadContext.sqlRecodes.get().add(deleteRecode);
            boolean flag= preparedStatement.execute(sql,columnNames);
            return flag;
        }
        return preparedStatement.execute(sql, columnNames);
    }

    @Override
    public int getResultSetHoldability() throws SQLException {
        return preparedStatement.getResultSetHoldability();
    }

    @Override
    public boolean isClosed() throws SQLException {
        return preparedStatement.isClosed();
    }

    @Override
    public void setPoolable(boolean poolable) throws SQLException {
        preparedStatement.setPoolable(poolable);
    }

    @Override
    public boolean isPoolable() throws SQLException {
        return preparedStatement.isPoolable();
    }

    @Override
    public void closeOnCompletion() throws SQLException {
        preparedStatement.closeOnCompletion();
    }

    @Override
    public boolean isCloseOnCompletion() throws SQLException {
        return preparedStatement.isCloseOnCompletion();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return preparedStatement.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return preparedStatement.isWrapperFor(iface);
    }
}
