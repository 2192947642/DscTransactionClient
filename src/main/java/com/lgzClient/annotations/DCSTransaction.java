package com.lgzClient.annotations;

import java.lang.annotation.*;

@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DCSTransaction {//分布式事务注解
    long timeout() default 5000;
}
