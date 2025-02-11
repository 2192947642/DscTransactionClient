package com.lgzClient.wrapper;

import com.lgzClient.types.ThreadContext;
import com.lgzClient.types.sql.recode.*;
import com.lgzClient.utils.SqlUtil;

import java.io.*;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

public class PreparedStatementWrapper implements PreparedStatement {
    private PreparedStatement preparedStatement;
    private Statement selectStatement;
    private String originalSql;
    private ArrayList<String> batchSqls;
    private HashMap<Integer,Object> parameters;
    private SqlUtil sqlUtil;
    public HashMap<Integer,Object> getParameters(){
        return parameters;
    }
    public PreparedStatementWrapper(PreparedStatement preparedStatement,String sql) throws SQLException {
        this.preparedStatement = preparedStatement;
        this.sqlUtil=new SqlUtil();
        String selectSql=sqlUtil.buildSelectSql(sql);
        this.originalSql=sql;
        selectStatement=preparedStatement.getConnection().createStatement();
        this.batchSqls=new ArrayList<>();
        this.parameters=new HashMap<>();
    }

    @Override
    public ResultSet executeQuery() throws SQLException {
        String finalSql=sqlUtil.getFinalSql(this,originalSql);
        SelectRecode selectRecode=new SelectRecode(finalSql);
        ThreadContext.sqlRecodes.get().add(selectRecode);//将执行的这条select进行插入
        return preparedStatement.executeQuery();
    }

    @Override
    public int executeUpdate() throws SQLException {
        SqlType sqlType = sqlUtil.getSqlType(originalSql);
        String finalSql=sqlUtil.getFinalSql(this,originalSql);
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
        parameters.put(parameterIndex,null);
        preparedStatement.setNull(parameterIndex, sqlType);
    }

    @Override
    public void setBoolean(int parameterIndex, boolean x) throws SQLException {
        parameters.put(parameterIndex,x);
        preparedStatement.setBoolean(parameterIndex, x);
    }

    @Override
    public void setByte(int parameterIndex, byte x) throws SQLException {
        parameters.put(parameterIndex,x);
        preparedStatement.setByte(parameterIndex, x);
    }

    @Override
    public void setShort(int parameterIndex, short x) throws SQLException {
        parameters.put(parameterIndex,x);
        preparedStatement.setShort(parameterIndex, x);
    }

    @Override
    public void setInt(int parameterIndex, int x) throws SQLException {
        parameters.put(parameterIndex,x);
        preparedStatement.setInt(parameterIndex, x);
    }

    @Override
    public void setLong(int parameterIndex, long x) throws SQLException {
        parameters.put(parameterIndex,x);
        preparedStatement.setLong(parameterIndex, x);
    }

    @Override
    public void setFloat(int parameterIndex, float x) throws SQLException {
        parameters.put(parameterIndex,x);
        preparedStatement.setFloat(parameterIndex, x);
    }

    @Override
    public void setDouble(int parameterIndex, double x) throws SQLException {
        parameters.put(parameterIndex,x);
        preparedStatement.setDouble(parameterIndex, x);
    }

    @Override
    public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
        parameters.put(parameterIndex,x);
        preparedStatement.setBigDecimal(parameterIndex, x);
    }

    @Override
    public void setString(int parameterIndex, String x) throws SQLException {
        parameters.put(parameterIndex,x);
        preparedStatement.setString(parameterIndex, x);
    }

    @Override
    public void setBytes(int parameterIndex, byte[] x) throws SQLException {
        parameters.put(parameterIndex,x);
        preparedStatement.setBytes(parameterIndex, x);
    }

    @Override
    public void setDate(int parameterIndex, Date x) throws SQLException {
        parameters.put(parameterIndex,x);
        preparedStatement.setDate(parameterIndex, x);
    }

    @Override
    public void setTime(int parameterIndex, Time x) throws SQLException {
        parameters.put(parameterIndex,x);
        preparedStatement.setTime(parameterIndex, x);
    }

    @Override
    public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
        parameters.put(parameterIndex,x);
        preparedStatement.setTimestamp(parameterIndex, x);
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException {
        try {
            PushbackInputStream pushbackInputStream = new PushbackInputStream(x);
            byte[] bytes=pushbackInputStream.readNBytes(length);
            parameters.put(parameterIndex,bytes);
            pushbackInputStream.unread(bytes);
            preparedStatement.setAsciiStream(parameterIndex, pushbackInputStream, length);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException {
        try {
            // 使用 PushbackInputStream 包装原始的 InputStream
            PushbackInputStream pushbackInputStream = new PushbackInputStream(x);

            // 读取前 length 个字节
            byte[] bytes = pushbackInputStream.readNBytes(length);

            // 将字节数组存储在 parameters 中
            parameters.put(parameterIndex, bytes);

            // 将读取的字节推回到流中
            pushbackInputStream.unread(bytes);

            // 使用原始的 InputStream 调用 setUnicodeStream
            preparedStatement.setUnicodeStream(parameterIndex, pushbackInputStream, length);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {
        try {
            // 使用 PushbackInputStream 包装原始的 InputStream
            PushbackInputStream pushbackInputStream = new PushbackInputStream(x);

            // 读取前 length 个字节
            byte[] bytes = pushbackInputStream.readNBytes(length);

            // 将字节数组存储在 parameters 中
            parameters.put(parameterIndex, bytes);

            // 将读取的字节推回到流中
            pushbackInputStream.unread(bytes);

            // 使用原始的 InputStream 调用 setBinaryStream
            preparedStatement.setBinaryStream(parameterIndex, pushbackInputStream, length);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void clearParameters() throws SQLException {
        parameters.clear();
        preparedStatement.clearParameters();
    }

    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
        parameters.put(parameterIndex,x);
        preparedStatement.setObject(parameterIndex, x, targetSqlType);
    }

    @Override
    public void setObject(int parameterIndex, Object x) throws SQLException {
        parameters.put(parameterIndex,x);
        preparedStatement.setObject(parameterIndex, x);
    }

    @Override
    public boolean execute() throws SQLException {
        SqlType sqlType=sqlUtil.getSqlType(originalSql);
        String finalSql=sqlUtil.getFinalSql(this,originalSql);
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
        String finalSql=sqlUtil.getFinalSql(this,originalSql);
        preparedStatement.addBatch();
        batchSqls.add(finalSql);
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {
        try {
            // 使用 PushbackReader 包装原始的 Reader
            PushbackReader pushbackReader = new PushbackReader(reader);

            // 创建一个字符数组来存储读取的字符
            char[] chars = new char[length];

            // 读取前 length 个字符
            int readCount = pushbackReader.read(chars, 0, length);

            // 将读取的字符数组存储在 parameters 中
            if (readCount > 0) {
                parameters.put(parameterIndex, new String(chars, 0, readCount));
            } else {
                parameters.put(parameterIndex, "");
            }

            // 将读取的字符推回到流中
            if (readCount > 0) {
                pushbackReader.unread(chars, 0, readCount);
            }

            // 使用原始的 Reader 调用 setCharacterStream
            preparedStatement.setCharacterStream(parameterIndex, pushbackReader, length);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void setRef(int parameterIndex, Ref x) throws SQLException {
        parameters.put(parameterIndex,x);
        preparedStatement.setRef(parameterIndex, x);
    }

    @Override
    public void setBlob(int parameterIndex, Blob x) throws SQLException {
        parameters.put(parameterIndex,x);
        preparedStatement.setBlob(parameterIndex, x);
    }

    @Override
    public void setClob(int parameterIndex, Clob x) throws SQLException {
        parameters.put(parameterIndex,x);
        preparedStatement.setClob(parameterIndex, x);
    }

    @Override
    public void setArray(int parameterIndex, Array x) throws SQLException {
        parameters.put(parameterIndex,x);
        preparedStatement.setArray(parameterIndex, x);
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {

        return preparedStatement.getMetaData();
    }

    @Override
    public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {
        parameters.put(parameterIndex,x);
        preparedStatement.setDate(parameterIndex, x, cal);
    }

    @Override
    public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {
        parameters.put(parameterIndex,x);
        preparedStatement.setTime(parameterIndex, x, cal);
    }

    @Override
    public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {
        parameters.put(parameterIndex,x);
        preparedStatement.setTimestamp(parameterIndex, x, cal);
    }

    @Override
    public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException {
        parameters.put(parameterIndex,null);
        preparedStatement.setNull(parameterIndex, sqlType, typeName);
    }

    @Override
    public void setURL(int parameterIndex, URL x) throws SQLException {
        parameters.put(parameterIndex,x);
        preparedStatement.setURL(parameterIndex, x);
    }

    @Override
    public ParameterMetaData getParameterMetaData() throws SQLException {
        return preparedStatement.getParameterMetaData();
    }

    @Override
    public void setRowId(int parameterIndex, RowId x) throws SQLException {
        parameters.put(parameterIndex,x);
        preparedStatement.setRowId(parameterIndex, x);
    }

    @Override
    public void setNString(int parameterIndex, String value) throws SQLException {
        parameters.put(parameterIndex,value);
        preparedStatement.setNString(parameterIndex, value);
    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {
        try {
            // 使用 PushbackReader 包装原始的 Reader
            PushbackReader pushbackReader = new PushbackReader(value);

            // 创建一个字符数组来存储读取的字符
            char[] chars = new char[(int) length];

            // 读取前 length 个字符
            int readCount = pushbackReader.read(chars, 0, (int) length);

            // 将读取的字符数组存储在 parameters 中
            if (readCount > 0) {
                parameters.put(parameterIndex, new String(chars, 0, readCount));
            } else {
                parameters.put(parameterIndex, "");
            }

            // 将读取的字符推回到流中
            if (readCount > 0) {
                pushbackReader.unread(chars, 0, readCount);
            }

            // 使用原始的 Reader 调用 setNCharacterStream
            preparedStatement.setNCharacterStream(parameterIndex, pushbackReader, length);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void setNClob(int parameterIndex, NClob value) throws SQLException {
        parameters.put(parameterIndex,value);
        preparedStatement.setNClob(parameterIndex, value);
    }

    @Override
    public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {

        preparedStatement.setClob(parameterIndex, reader, length);
    }

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {
        PushbackInputStream pushbackInputStream=new PushbackInputStream(inputStream);

        preparedStatement.setBlob(parameterIndex, inputStream, length);
    }

    @Override
    public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {
        preparedStatement.setNClob(parameterIndex, reader, length);
    }

    @Override
    public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
        parameters.put(parameterIndex,xmlObject);
        preparedStatement.setSQLXML(parameterIndex, xmlObject);
    }

    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) throws SQLException {
        parameters.put(parameterIndex,x);
        preparedStatement.setObject(parameterIndex, x, targetSqlType, scaleOrLength);
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {
        try {
            // 使用 PushbackInputStream 包装原始的 InputStream
            PushbackInputStream pushbackInputStream = new PushbackInputStream(x);

            // 创建一个字节数组来存储读取的字节
            byte[] bytes = new byte[(int) length];

            // 读取前 length 个字节
            int readCount = pushbackInputStream.read(bytes, 0, (int) length);

            // 将读取的字节数组存储在 parameters 中
            if (readCount > 0) {
                parameters.put(parameterIndex, new String(bytes, 0, readCount, "ASCII"));
            } else {
                parameters.put(parameterIndex, "");
            }

            // 将读取的字节推回到流中
            if (readCount > 0) {
                pushbackInputStream.unread(bytes, 0, readCount);
            }

            // 使用原始的 InputStream 调用 setAsciiStream
            preparedStatement.setAsciiStream(parameterIndex, pushbackInputStream, length);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {
        try {
            // 使用 PushbackInputStream 包装原始的 InputStream
            PushbackInputStream pushbackInputStream = new PushbackInputStream(x);

            // 创建一个字节数组来存储读取的字节
            byte[] bytes = new byte[(int) length];

            // 读取前 length 个字节
            int readCount = pushbackInputStream.read(bytes, 0, (int) length);

            // 将读取的字节数组存储在 parameters 中
            if (readCount > 0) {
                parameters.put(parameterIndex, bytes);
            } else {
                parameters.put(parameterIndex, new byte[0]);
            }

            // 将读取的字节推回到流中
            if (readCount > 0) {
                pushbackInputStream.unread(bytes, 0, readCount);
            }

            // 使用原始的 InputStream 调用 setBinaryStream
            preparedStatement.setBinaryStream(parameterIndex, pushbackInputStream, length);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {
        try {
            // 使用 PushbackReader 包装原始的 Reader
            PushbackReader pushbackReader = new PushbackReader(reader);

            // 创建一个字符数组来存储读取的字符
            char[] chars = new char[(int) length];

            // 读取前 length 个字符
            int readCount = pushbackReader.read(chars, 0, (int) length);

            // 将读取的字符数组存储在 parameters 中
            if (readCount > 0) {
                parameters.put(parameterIndex, new String(chars, 0, readCount));
            } else {
                parameters.put(parameterIndex, "");
            }

            // 将读取的字符推回到流中
            if (readCount > 0) {
                pushbackReader.unread(chars, 0, readCount);
            }

            // 使用原始的 Reader 调用 setCharacterStream
            preparedStatement.setCharacterStream(parameterIndex, pushbackReader, length);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {
        try {
            // 使用 ByteArrayOutputStream 读取 InputStream 的全部内容
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = x.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }

            // 获取读取到的字节数组
            byte[] bytes = byteArrayOutputStream.toByteArray();

            // 将字节数组存储在 parameters 中
            parameters.put(parameterIndex, bytes);

            // 使用 ByteArrayInputStream 包装字节数组
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);

            // 使用 ByteArrayInputStream 调用 setAsciiStream
            preparedStatement.setAsciiStream(parameterIndex, byteArrayInputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {
        try {
            // 使用 ByteArrayOutputStream 读取 InputStream 的全部内容
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = x.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }

            // 获取读取到的字节数组
            byte[] bytes = byteArrayOutputStream.toByteArray();

            // 将字节数组存储在 parameters 中
            parameters.put(parameterIndex, bytes);

            // 使用 ByteArrayInputStream 包装字节数组
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);

            // 使用 ByteArrayInputStream 调用 setBinaryStream
            preparedStatement.setBinaryStream(parameterIndex, byteArrayInputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
        try {
            // 使用 StringWriter 读取 Reader 的全部内容
            StringWriter stringWriter = new StringWriter();
            char[] buffer = new char[1024];
            int charsRead;
            while ((charsRead = reader.read(buffer)) != -1) {
                stringWriter.write(buffer, 0, charsRead);
            }

            // 获取读取到的字符串
            String content = stringWriter.toString();

            // 将字符串存储在 parameters 中
            parameters.put(parameterIndex, content);

            // 使用 StringReader 包装字符串
            StringReader stringReader = new StringReader(content);

            // 使用 StringReader 调用 setCharacterStream
            preparedStatement.setCharacterStream(parameterIndex, stringReader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {
        try {
            // 使用 StringWriter 读取 Reader 的全部内容
            StringWriter stringWriter = new StringWriter();
            char[] buffer = new char[1024];
            int charsRead;
            while ((charsRead = value.read(buffer)) != -1) {
                stringWriter.write(buffer, 0, charsRead);
            }

            // 获取读取到的字符串
            String content = stringWriter.toString();

            // 将字符串存储在 parameters 中
            parameters.put(parameterIndex, content);

            // 使用 StringReader 包装字符串
            StringReader stringReader = new StringReader(content);

            // 使用 StringReader 调用 setNCharacterStream
            preparedStatement.setNCharacterStream(parameterIndex, stringReader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
        if(iface.isInstance(this)) return (T) this.preparedStatement;
        return preparedStatement.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        if(iface.isInstance(this)) return true;
        return preparedStatement.isWrapperFor(iface);
    }
}
