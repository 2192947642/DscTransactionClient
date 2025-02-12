package com.lgzClient.types.status;

/**
 * 统一返回结果状态信息类
 */
public enum ResultCodeEnum {
    NORMAL_ERROR(555, "逻辑错误"),
    DATA_ERROR(500, "数据库没有数据"),
    SUCCESS(200, "成功"),
    DATABASE_ERROR(506, "数据库插入失败"),
    AUTHORITY_ERROR(507, "权限不够"),
    Visit_Error(509,"无法跨接口访问");
    private Integer code;
    private String message;

    ResultCodeEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
