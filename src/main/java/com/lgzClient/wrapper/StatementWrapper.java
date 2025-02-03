package com.lgzClient.wrapper;

import com.lgzClient.types.ThreadContext;
import com.lgzClient.types.sql.*;
import com.lgzClient.utils.SqlUtil;

import java.sql.*;
import java.util.ArrayList;

public class StatementWrapper implements Statement {
    private Statement statement;
    private SqlUtil sqlUtil;
    private ArrayList<String> batches;

    public StatementWrapper(Statement statement) {
        this.statement = statement;
        this.sqlUtil = new SqlUtil();
        this.batches = new ArrayList<>();
    }

    @Override
    public ResultSet executeQuery(String sql) throws SQLException {
        SelectRecode selectRecode = new SelectRecode(sql);
        ThreadContext.sqlRecodes.get().add(selectRecode);
        return statement.executeQuery(sql);
    }

    @Override
    public int executeUpdate(String sql) throws SQLException {
        SqlType sqlType = sqlUtil.getSqlType(sql);
        if (sqlType == SqlType.insert) {
            InsertRecode insertRecode = new InsertRecode(sql);
            int number = statement.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
            ResultSet generatedKeys = statement.getGeneratedKeys();
            if (generatedKeys.next()) {
                insertRecode.generatedKeys.add(generatedKeys.getInt(1));
            }
            ThreadContext.sqlRecodes.get().add(insertRecode);
            return number;
        } else if (sqlType == SqlType.update) {
            UpdateRecode updateRecode = new UpdateRecode(sql);
            String selectSql = sqlUtil.buildSelectSql(sql);
            updateRecode.before = statement.executeQuery(selectSql).toString();
            int number = statement.executeUpdate(sql);
            updateRecode.after = statement.executeQuery(selectSql).toString();
            ThreadContext.sqlRecodes.get().add(updateRecode);
            return number;
        } else if (sqlType == SqlType.delete) {
            DeleteRecode deleteRecode = new DeleteRecode(sql);
            String selectSql = sqlUtil.buildSelectSql(sql);
            deleteRecode.before = statement.executeQuery(selectSql).toString();
            int number = statement.executeUpdate(sql);
            ThreadContext.sqlRecodes.get().add(deleteRecode);
            return number;
        }
        return statement.executeUpdate(sql);
    }

    @Override
    public void close() throws SQLException {
        statement.close();
    }

    @Override
    public int getMaxFieldSize() throws SQLException {
        return statement.getMaxFieldSize();
    }

    @Override
    public void setMaxFieldSize(int max) throws SQLException {
        statement.setMaxFieldSize(max);
    }

    @Override
    public int getMaxRows() throws SQLException {
        return statement.getMaxRows();
    }

    @Override
    public void setMaxRows(int max) throws SQLException {
        statement.setMaxRows(max);
    }

    @Override
    public void setEscapeProcessing(boolean enable) throws SQLException {
        statement.setEscapeProcessing(enable);
    }

    @Override
    public int getQueryTimeout() throws SQLException {
        return statement.getQueryTimeout();
    }

    @Override
    public void setQueryTimeout(int seconds) throws SQLException {
        statement.setQueryTimeout(seconds);
    }

    @Override
    public void cancel() throws SQLException {
        statement.cancel();
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return statement.getWarnings();
    }

    @Override
    public void clearWarnings() throws SQLException {
        statement.clearWarnings();
    }

    @Override
    public void setCursorName(String name) throws SQLException {
        statement.setCursorName(name);
    }

    @Override
    public boolean execute(String sql) throws SQLException {
        SqlType sqlType = sqlUtil.getSqlType(sql);
        if (sqlType == SqlType.select) {
            SelectRecode selectRecode = new SelectRecode(sql);
            ThreadContext.sqlRecodes.get().add(selectRecode);
            return statement.execute(sql);
        } else if (sqlType == SqlType.insert) {
            InsertRecode insertRecode = new InsertRecode(sql);
            boolean flag = statement.execute(sql, Statement.RETURN_GENERATED_KEYS);
            ResultSet generatedKeys = statement.getGeneratedKeys();
            if (generatedKeys.next()) {
                insertRecode.generatedKeys.add(generatedKeys.getInt(1));
            }
            ThreadContext.sqlRecodes.get().add(insertRecode);
            return flag;
        } else if (sqlType == SqlType.update) {
            UpdateRecode updateRecode = new UpdateRecode(sql);
            String selectSql = sqlUtil.buildSelectSql(sql);
            updateRecode.before = statement.executeQuery(selectSql).toString();
            boolean flag = statement.execute(sql);
            updateRecode.after = statement.executeQuery(selectSql).toString();
            ThreadContext.sqlRecodes.get().add(updateRecode);
            return flag;
        } else if (sqlType == SqlType.delete) {
            DeleteRecode deleteRecode = new DeleteRecode(sql);
            String selectSql = sqlUtil.buildSelectSql(sql);
            deleteRecode.before = statement.executeQuery(selectSql).toString();
            boolean flag = statement.execute(sql);
            ThreadContext.sqlRecodes.get().add(deleteRecode);
            return flag;
        }
        return statement.execute(sql);
    }

    @Override
    public ResultSet getResultSet() throws SQLException {
        return statement.getResultSet();
    }

    @Override
    public int getUpdateCount() throws SQLException {
        return statement.getUpdateCount();
    }

    @Override
    public boolean getMoreResults() throws SQLException {
        return statement.getMoreResults();
    }

    @Override
    public void setFetchDirection(int direction) throws SQLException {
        statement.setFetchDirection(direction);
    }

    @Override
    public int getFetchDirection() throws SQLException {
        return statement.getFetchDirection();
    }

    @Override
    public void setFetchSize(int rows) throws SQLException {
        statement.setFetchSize(rows);
    }

    @Override
    public int getFetchSize() throws SQLException {
        return statement.getFetchSize();
    }

    @Override
    public int getResultSetConcurrency() throws SQLException {
        return statement.getResultSetConcurrency();
    }

    @Override
    public int getResultSetType() throws SQLException {
        return statement.getResultSetType();
    }

    @Override
    public void addBatch(String sql) throws SQLException {
        statement.addBatch(sql);
        batches.add(sql);
    }

    @Override
    public void clearBatch() throws SQLException {
        statement.clearBatch();
        batches.clear();
    }

    @Override
    public int[] executeBatch() throws SQLException {
        return statement.executeBatch();
    }

    @Override
    public Connection getConnection() throws SQLException {
        return statement.getConnection();
    }

    @Override
    public boolean getMoreResults(int current) throws SQLException {
        return statement.getMoreResults(current);
    }

    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        return statement.getGeneratedKeys();
    }

    @Override
    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        SqlType sqlType = sqlUtil.getSqlType(sql);
        if (sqlType == SqlType.insert) {
            InsertRecode insertRecode = new InsertRecode(sql);
            int number = statement.executeUpdate(sql, autoGeneratedKeys);
            ResultSet generatedKeys = statement.getGeneratedKeys();
            if (generatedKeys.next()) {
                insertRecode.generatedKeys.add(generatedKeys.getInt(1));
            }
            ThreadContext.sqlRecodes.get().add(insertRecode);
            return number;
        } else if (sqlType == SqlType.update) {
            UpdateRecode updateRecode = new UpdateRecode(sql);
            String selectSql = sqlUtil.buildSelectSql(sql);
            updateRecode.before = statement.executeQuery(selectSql).toString();
            int number = statement.executeUpdate(sql, autoGeneratedKeys);
            updateRecode.after = statement.executeQuery(selectSql).toString();
            ThreadContext.sqlRecodes.get().add(updateRecode);
            return number;
        } else if (sqlType == SqlType.delete) {
            DeleteRecode deleteRecode = new DeleteRecode(sql);
            String selectSql = sqlUtil.buildSelectSql(sql);
            deleteRecode.before = statement.executeQuery(selectSql).toString();
            int number = statement.executeUpdate(sql, autoGeneratedKeys);
            ThreadContext.sqlRecodes.get().add(deleteRecode);
            return number;
        }
        return statement.executeUpdate(sql, autoGeneratedKeys);
    }

    @Override
    public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
        SqlType sqlType = sqlUtil.getSqlType(sql);
        if (sqlType == SqlType.insert) {
            InsertRecode insertRecode = new InsertRecode(sql);
            int number = statement.executeUpdate(sql, columnIndexes);
            ResultSet generatedKeys = statement.getGeneratedKeys();
            if (generatedKeys.next()) {
                insertRecode.generatedKeys.add(generatedKeys.getInt(1));
            }
            ThreadContext.sqlRecodes.get().add(insertRecode);
            return number;
        } else if (sqlType == SqlType.update) {
            UpdateRecode updateRecode = new UpdateRecode(sql);
            String selectSql = sqlUtil.buildSelectSql(sql);
            updateRecode.before = statement.executeQuery(selectSql).toString();
            int number = statement.executeUpdate(sql, columnIndexes);
            updateRecode.after = statement.executeQuery(selectSql).toString();
            ThreadContext.sqlRecodes.get().add(updateRecode);
            return number;
        } else if (sqlType == SqlType.delete) {
            DeleteRecode deleteRecode = new DeleteRecode(sql);
            String selectSql = sqlUtil.buildSelectSql(sql);
            deleteRecode.before = statement.executeQuery(selectSql).toString();
            int number = statement.executeUpdate(sql, columnIndexes);
            ThreadContext.sqlRecodes.get().add(deleteRecode);
            return number;
        }
        return statement.executeUpdate(sql, columnIndexes);
    }

    @Override
    public int executeUpdate(String sql, String[] columnNames) throws SQLException {
        SqlType sqlType = sqlUtil.getSqlType(sql);
        if (sqlType == SqlType.insert) {
            InsertRecode insertRecode = new InsertRecode(sql);
            int number = statement.executeUpdate(sql, columnNames);
            ResultSet generatedKeys = statement.getGeneratedKeys();
            if (generatedKeys.next()) {
                insertRecode.generatedKeys.add(generatedKeys.getInt(1));
            }
            ThreadContext.sqlRecodes.get().add(insertRecode);
            return number;
        } else if (sqlType == SqlType.update) {
            UpdateRecode updateRecode = new UpdateRecode(sql);
            String selectSql = sqlUtil.buildSelectSql(sql);
            updateRecode.before = statement.executeQuery(selectSql).toString();
            int number = statement.executeUpdate(sql, columnNames);
            updateRecode.after = statement.executeQuery(selectSql).toString();
            ThreadContext.sqlRecodes.get().add(updateRecode);
            return number;
        } else if (sqlType == SqlType.delete) {
            DeleteRecode deleteRecode = new DeleteRecode(sql);
            String selectSql = sqlUtil.buildSelectSql(sql);
            deleteRecode.before = statement.executeQuery(selectSql).toString();
            int number = statement.executeUpdate(sql, columnNames);
            ThreadContext.sqlRecodes.get().add(deleteRecode);
            return number;
        }
        return statement.executeUpdate(sql, columnNames);
    }

    @Override
    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
        SqlType sqlType = sqlUtil.getSqlType(sql);
        if (sqlType == SqlType.select) {
            SelectRecode selectRecode = new SelectRecode(sql);
            ThreadContext.sqlRecodes.get().add(selectRecode);
            return statement.execute(sql, autoGeneratedKeys);
        } else if (sqlType == SqlType.insert) {
            InsertRecode insertRecode = new InsertRecode(sql);
            boolean flag = statement.execute(sql, autoGeneratedKeys);
            ResultSet generatedKeys = statement.getGeneratedKeys();
            if (generatedKeys.next()) {
                insertRecode.generatedKeys.add(generatedKeys.getInt(1));
            }
            ThreadContext.sqlRecodes.get().add(insertRecode);
            return flag;
        } else if (sqlType == SqlType.update) {
            UpdateRecode updateRecode = new UpdateRecode(sql);
            String selectSql = sqlUtil.buildSelectSql(sql);
            updateRecode.before = statement.executeQuery(selectSql).toString();
            boolean flag = statement.execute(sql, autoGeneratedKeys);
            updateRecode.after = statement.executeQuery(selectSql).toString();
            ThreadContext.sqlRecodes.get().add(updateRecode);
            return flag;
        } else if (sqlType == SqlType.delete) {
            DeleteRecode deleteRecode = new DeleteRecode(sql);
            String selectSql = sqlUtil.buildSelectSql(sql);
            deleteRecode.before = statement.executeQuery(selectSql).toString();
            boolean flag = statement.execute(sql, autoGeneratedKeys);
            ThreadContext.sqlRecodes.get().add(deleteRecode);
            return flag;
        }
        return statement.execute(sql, autoGeneratedKeys);
    }

    @Override
    public boolean execute(String sql, int[] columnIndexes) throws SQLException {
        SqlType sqlType = sqlUtil.getSqlType(sql);
        if (sqlType == SqlType.select) {
            SelectRecode selectRecode = new SelectRecode(sql);
            ThreadContext.sqlRecodes.get().add(selectRecode);
            return statement.execute(sql, columnIndexes);
        } else if (sqlType == SqlType.insert) {
            InsertRecode insertRecode = new InsertRecode(sql);
            boolean flag = statement.execute(sql, columnIndexes);
            ResultSet generatedKeys = statement.getGeneratedKeys();
            if (generatedKeys.next()) {
                insertRecode.generatedKeys.add(generatedKeys.getInt(1));
            }
            ThreadContext.sqlRecodes.get().add(insertRecode);
            return flag;
        } else if (sqlType == SqlType.update) {
            UpdateRecode updateRecode = new UpdateRecode(sql);
            String selectSql = sqlUtil.buildSelectSql(sql);
            updateRecode.before = statement.executeQuery(selectSql).toString();
            boolean flag = statement.execute(sql, columnIndexes);
            updateRecode.after = statement.executeQuery(selectSql).toString();
            ThreadContext.sqlRecodes.get().add(updateRecode);
            return flag;
        } else if (sqlType == SqlType.delete) {
            DeleteRecode deleteRecode = new DeleteRecode(sql);
            String selectSql = sqlUtil.buildSelectSql(sql);
            deleteRecode.before = statement.executeQuery(selectSql).toString();
            boolean flag = statement.execute(sql, columnIndexes);
            ThreadContext.sqlRecodes.get().add(deleteRecode);
            return flag;
        }
        return statement.execute(sql, columnIndexes);
    }

    @Override
    public boolean execute(String sql, String[] columnNames) throws SQLException {
        SqlType sqlType = sqlUtil.getSqlType(sql);
        if (sqlType == SqlType.select) {
            SelectRecode selectRecode = new SelectRecode(sql);
            ThreadContext.sqlRecodes.get().add(selectRecode);
            return statement.execute(sql, columnNames);
        } else if (sqlType == SqlType.insert) {
            InsertRecode insertRecode = new InsertRecode(sql);
            boolean flag = statement.execute(sql, columnNames);
            ResultSet generatedKeys = statement.getGeneratedKeys();
            if (generatedKeys.next()) {
                insertRecode.generatedKeys.add(generatedKeys.getInt(1));
            }
            ThreadContext.sqlRecodes.get().add(insertRecode);
            return flag;
        } else if (sqlType == SqlType.update) {
            UpdateRecode updateRecode = new UpdateRecode(sql);
            String selectSql = sqlUtil.buildSelectSql(sql);
            updateRecode.before = statement.executeQuery(selectSql).toString();
            boolean flag = statement.execute(sql, columnNames);
            updateRecode.after = statement.executeQuery(selectSql).toString();
            ThreadContext.sqlRecodes.get().add(updateRecode);
            return flag;
        } else if (sqlType == SqlType.delete) {
            DeleteRecode deleteRecode = new DeleteRecode(sql);
            String selectSql = sqlUtil.buildSelectSql(sql);
            deleteRecode.before = statement.executeQuery(selectSql).toString();
            boolean flag = statement.execute(sql, columnNames);
            ThreadContext.sqlRecodes.get().add(deleteRecode);
            return flag;
        }
        return statement.execute(sql, columnNames);
    }

    @Override
    public int getResultSetHoldability() throws SQLException {
        return statement.getResultSetHoldability();
    }

    @Override
    public boolean isClosed() throws SQLException {
        return statement.isClosed();
    }

    @Override
    public void setPoolable(boolean poolable) throws SQLException {
        statement.setPoolable(poolable);
    }

    @Override
    public boolean isPoolable() throws SQLException {
        return statement.isPoolable();
    }

    @Override
    public void closeOnCompletion() throws SQLException {
        statement.closeOnCompletion();
    }

    @Override
    public boolean isCloseOnCompletion() throws SQLException {
        return statement.isCloseOnCompletion();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return statement.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return statement.isWrapperFor(iface);
    }
}
