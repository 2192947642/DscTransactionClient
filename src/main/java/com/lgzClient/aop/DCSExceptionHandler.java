package com.lgzClient.aop;
import com.lgzClient.types.Result;
import com.lgzClient.types.ThreadContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class DCSExceptionHandler {
    @ExceptionHandler(value= Exception.class)
    public Result exceptionHandler(Exception ex){
        Result result=Result.error(ex.getMessage());
        ThreadContext.error.set(ex);
        return result;
    }
}