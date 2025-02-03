package com.lgzClient.utils;
import com.lgzClient.types.ThreadContext;
import com.lgzClient.types.sql.*;
import com.lgzClient.wrapper.PreparedStatementWrapper;
import com.mysql.cj.xdevapi.PreparableStatement;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;

import java.lang.reflect.Field;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;


public class SqlUtil {
     public SqlType getSqlType(String sql){
        try{
            Statement statement= CCJSqlParserUtil.parse(sql);
            if(statement instanceof Update update){
                return SqlType.update;
            }
            else if(statement instanceof Delete delete){
                return SqlType.delete;
            }
            else if(statement instanceof Insert insert){
                return SqlType.insert;
            }else if(statement instanceof Select select){
                return SqlType.select;
            }
        }catch (Exception e){
            throw new RuntimeException(e);
        }
        return null;
    }
     public String buildSelectSql(String sql)  {
         try{
             Statement statement= CCJSqlParserUtil.parse(sql);

             Expression where=null;
             Table table=null;
             if(statement instanceof Update update){
               //  System.out.println(update.getUpdateSet(0).getColumns());
                 where=update.getWhere();
                 table=update.getTable();
             }
             else if(statement instanceof Delete delete){
                 where=delete.getWhere();
                 table=delete.getTable();
             }
             StringBuilder stringBuilder=new StringBuilder();
             stringBuilder.append("select * from ").append(table.getName());
             if(where!=null){
                 stringBuilder.append(" where ").append(where);
             }
             return stringBuilder.toString();
         }catch (Exception e){
             throw new RuntimeException(e);
         }
     }
     public  String getFinalSql(PreparedStatement preparedStatement,String sql) throws SQLException {
        // 获取原始 SQL 模板
       // String sql = getRawSqlFromPreparedStatement(preparedStatement);

        // 获取参数值列表
        List<Object> parameterValues = getParameterValues(preparedStatement);

        // 替换占位符为实际参数值
        return replacePlaceholders(sql, parameterValues);
    }

     private  List<Object> getParameterValues(PreparedStatement preparedStatement) throws SQLException {
        List<Object> parameterValues = new ArrayList<>();
        try {
            // 使用反射获取 PreparedStatement 中的参数值列表
            Field parameterValuesField = preparedStatement.getClass().getDeclaredField("parameterValues");
            parameterValuesField.setAccessible(true);
            Object[] values = (Object[]) parameterValuesField.get(preparedStatement);
            for (Object value : values) {
                parameterValues.add(value);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
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
     public static void main(String [] args){
         SqlUtil sqlUtil=new SqlUtil();
         String sql="update user set name='lgz' where id=?";
         SqlType sqlType=sqlUtil.getSqlType(sql);
         System.out.println(sqlType);
         System.out.println(sqlUtil.buildSelectSql(sql));
     }
}
