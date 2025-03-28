package com.lgzClient.handlers;
import com.lgzClient.types.Result;
import com.lgzClient.types.DCSThreadContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Comparator;

@Slf4j
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class DCSExceptionHandler  {
    @ExceptionHandler(value= Throwable.class)
    public Result exceptionHandler(Exception ex){
        Result result=Result.error(ex.getMessage());
        DCSThreadContext.error.set(ex);
        return result;
    }

}