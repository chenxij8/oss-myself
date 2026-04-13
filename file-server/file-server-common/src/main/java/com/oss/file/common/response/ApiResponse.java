package com.oss.file.common.response;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应体
 * 所有接口都使用此类进行统一的响应
 */
@Data
@NoArgsConstructor
public class ApiResponse<T> {
    /**
     * 应答码：0表示成功，其他值表示业务异常
     */
    private Integer code;

    /**
     * 应答信息
     */
    private String message;

    /**
     * 应答数据
     */
    private T data;

    /**
     * 时间戳
     */
    private Long timestamp;

    /**
     * 请求ID，用于日志追踪
     */
    private String requestId;

    public ApiResponse(Integer code, String message) {
        this.code = code;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }

    public ApiResponse(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 成功响应
     */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(0, "请求成功");
    }

    /**
     * 成功响应（带数据）
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, "请求成功", data);
    }

    /**
     * 成功响应（带自定义消息）
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(0, message, data);
    }

    /**
     * 失败响应
     */
    public static <T> ApiResponse<T> fail(Integer code, String message) {
        return new ApiResponse<>(code, message);
    }

    /**
     * 业务异常响应
     */
    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(1, message);
    }

    /**
     * 参数验证失败
     */
    public static <T> ApiResponse<T> paramError(String message) {
        return new ApiResponse<>(400, message);
    }

    /**
     * 未认证
     */
    public static <T> ApiResponse<T> unauthorized() {
        return new ApiResponse<>(401, "未认证，请先登录");
    }

    /**
     * 无权限
     */
    public static <T> ApiResponse<T> forbidden() {
        return new ApiResponse<>(403, "无权限访问此资源");
    }

    /**
     * 系统异常
     */
    public static <T> ApiResponse<T> error() {
        return new ApiResponse<>(500, "系统内部错误");
    }
}
