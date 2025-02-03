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

import java.sql.PreparedStatement;
import java.sql.SQLException;


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
                 stringBuilder.append(" where ").append(where).append(";");
             }
             return stringBuilder.toString();
         }catch (Exception e){
             throw new RuntimeException(e);
         }
     }
     public static void main(String [] args){
     }
}
