package com.lgzClient.types;

import com.lgzClient.types.status.ResultCodeEnum;
import lombok.Data;

/**
 * 全局统一返回结果类
 */
@Data
public class Result<T> {
    // 返回码
    private Object code; // string or integer 加密后为 string
    // 返回消息
    private String message;
    // 返回数据
    private T data; // string or T

    public Result() {
    }
    public static Result success(){
        return Result.success(null, "请求成功");
    }
    // 返回数据
    private static <T> Result<T> build(T data) {
        Result<T> result = new Result<T>();
        if (data != null)
            result.setData(data);
        return result;
    }
    public static <T> Result<T> success(T body, String message) {
        return build(body, ResultCodeEnum.SUCCESS.getCode(), message);
    }

    public static <T> Result<T> success(T body) {
        return build(body, ResultCodeEnum.SUCCESS.getCode(), "请求成功");
    }

    public static <T> Result<T> error(T body, String message) {
        return build(body, ResultCodeEnum.NORMAL_ERROR.getCode(), message);
    }


    public static <T> Result<T> error(String message) {
        return Result.build(null, ResultCodeEnum.NORMAL_ERROR.getCode(), message);
    }

    public static <T> Result<T> error(ResultCodeEnum resultCodeEnum) {
        return build(null, resultCodeEnum.getCode(), resultCodeEnum.getMessage());
    }

    private static <T> Result<T> build(T body, Integer code, String message) {
        Result<T> result = build(body);
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    public Boolean isSuccess(){
        return this.code.equals(200);
    }
}