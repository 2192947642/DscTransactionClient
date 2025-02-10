package com.lgzClient.utils;
import com.lgzClient.types.sql.recode.*;
import com.lgzClient.wrapper.PreparedStatementWrapper;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.update.Update;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class SqlUtil {
    private static String convertToSelect(String sql) throws JSQLParserException {
        // 解析 SQL 语句
        Statement statement = CCJSqlParserUtil.parse(sql);
        if(statement instanceof Select){
            return sql;
        }
        if(statement instanceof Insert){
            return "";
        }
        if (statement instanceof Update) {
            // 处理 UPDATE 语句
            return convertUpdateToSelect((Update) statement);
        } else if (statement instanceof Delete) {
            // 处理 DELETE 语句
            return convertDeleteToSelect((Delete) statement);
        }
        return "";
    }

    /**
     * 将 UPDATE 语句转换为 SELECT 语句
     *
     * @param updateStatement UPDATE 语句对象
     * @return 转换后的 SELECT 语句
     */
    private static String convertUpdateToSelect(Update updateStatement) {
        // 提取列名（UPDATE 语句中设置的列）
        List<SelectItem<?>> selectItems = new ArrayList<>();
        for (Column column : updateStatement.getColumns()) {
            selectItems.add(new SelectItem<>(column));
        }

        // 提取 WHERE 条件
        Expression whereClause = updateStatement.getWhere();

        // 构建 SELECT 语句
        PlainSelect select = new PlainSelect();
        select.setSelectItems(selectItems);
        select.setFromItem(updateStatement.getTable());
        select.setWhere(whereClause);
        return select.toString();
    }

    /**
     * 将 DELETE 语句转换为 SELECT 语句
     *
     * @param deleteStatement DELETE 语句对象
     * @return 转换后的 SELECT 语句
     */
     private static String convertDeleteToSelect(Delete deleteStatement) {
        // 提取 WHERE 条件
        Expression whereClause = deleteStatement.getWhere();
        // 构建 SELECT 语句（选择所有列）
        PlainSelect select = new PlainSelect();
        select.addSelectItems(new SelectItem<>(new Column("*")));
        select.setFromItem(deleteStatement.getTable());
        select.setWhere(whereClause);
        return select.toString();
    }
     public SqlType getSqlType(String sql){
        try{
            Statement statement= CCJSqlParserUtil.parse(sql);
            if(statement instanceof Update){
                return SqlType.update;
            }
            else if(statement instanceof Delete){
                return SqlType.delete;
            }
            else if(statement instanceof Insert){
                return SqlType.insert;
            }else if(statement instanceof Select){
                return SqlType.select;
            }
        }catch (Exception e){
            throw new RuntimeException(e);
        }
        return null;
    }

     public String buildSelectSql(String sql)  {
         try {
             return convertToSelect(sql);
         }catch (Exception e){
             throw new RuntimeException(e);
         }
     }
     public  String getFinalSql(PreparedStatement preparedStatement,String sql) throws SQLException {
        // 获取原始 SQL 模板
        // 获取参数值列表
        List<Object> parameterValues = getParameterValues(preparedStatement);

        // 替换占位符为实际参数值
        return replacePlaceholders(sql, parameterValues);
    }

     private  List<Object> getParameterValues(PreparedStatement preparedStatement) throws SQLException {
        List<Object> parameterValues = new ArrayList<>();
        if(preparedStatement instanceof PreparedStatementWrapper preparedStatementWrapper){
            HashMap hashMap=preparedStatementWrapper.getParameters();
            for (int i = 1; i <= hashMap.size(); i++) {
                parameterValues.add(hashMap.get(i));
            }
        }
        return parameterValues;
    }

     private  String replacePlaceholders(String sql, List<Object> parameterValues) {
        StringBuilder finalSql = new StringBuilder();
        int paramIndex = 0;
        for (int i = 0; i < sql.length(); i++) {
            if (sql.charAt(i) == '?' && paramIndex < parameterValues.size()) {
                Object value = parameterValues.get(paramIndex++);
                if (value instanceof String) {
                    // 处理字符串类型的参数，添加引号并转义特殊字符
                    finalSql.append("'").append(value.toString().replace("'", "\\'")).append("'");
                } else {
                    finalSql.append(value);
                }
            } else {
                finalSql.append(sql.charAt(i));
            }
        }
        return finalSql.toString();
    }
     public String getResultSetJson(ResultSet resultSet) throws SQLException {
        ResultSetMetaData metaData=resultSet.getMetaData();
        int columnCount=metaData.getColumnCount();
        ArrayList<HashMap<String,String>> resultList=new ArrayList<>();
        while(resultSet.next()){
            HashMap hashMap=new HashMap();
            for(int i=1;i<=columnCount;i++){
                String columnName=metaData.getColumnName(i);
                Object value= resultSet.getObject(columnName);
                String strValue="";
                if(!(value instanceof String)) strValue=JsonUtil.objToJson(value);
                else if(value instanceof String str) strValue=str;
                hashMap.put(columnName,strValue);
            }
            resultList.add(hashMap);
        }
        return JsonUtil.objToJson(resultList);
     }
     public static void main(String [] args){
         SqlUtil sqlUtil=new SqlUtil();
         String sql="update user set name='lgz' where id=?";
         SqlType sqlType=sqlUtil.getSqlType(sql);
         System.out.println(sqlType);
         System.out.println(sqlUtil.buildSelectSql(sql));
     }
}
