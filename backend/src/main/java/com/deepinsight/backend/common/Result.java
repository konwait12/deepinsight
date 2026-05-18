package com.deepinsight.backend.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * 统一响应结果封装
 * <p>
 * 所有Controller接口统一使用此类包装返回值，
 * 前端通过{@code code}字段判断业务状态，
 * 通过{@code message}获取提示信息，
 * 通过{@code data}获取业务数据。
 *
 * <p>使用示例：
 * <pre>{@code
 * // 成功（带数据）
 * return Result.success(user);
 *
 * // 成功（自定义消息）
 * return Result.success("注册成功", authResponse);
 *
 * // 失败
 * return Result.error(400, "用户名不能为空");
 * }</pre>
 *
 * @param <T> 业务数据的类型，无数据时可为Void
 * @author DeepInsight Team
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> {

    /** 状态码（200表示成功，其他为业务错误码） */
    private int code;

    /** 提示信息（成功或失败的具体描述） */
    private String message;

    /** 业务数据（可为null） */
    private T data;

    // ==================== 构造函数（Lombok @Data已生成getter/setter） ====================

    private Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // ==================== 成功响应静态工厂方法 ====================

    /**
     * 成功响应（无数据）
     */
    public static <T> Result<T> success() {
        return new Result<>(Constants.CODE_SUCCESS, "操作成功", null);
    }

    /**
     * 成功响应（携带数据）
     *
     * @param data 业务数据
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(Constants.CODE_SUCCESS, "操作成功", data);
    }

    /**
     * 成功响应（自定义消息 + 携带数据）
     *
     * @param message 自定义成功提示
     * @param data    业务数据
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(Constants.CODE_SUCCESS, message, data);
    }

    // ==================== 失败响应静态工厂方法 ====================

    /**
     * 失败响应
     *
     * @param code    业务错误码
     * @param message 错误描述
     */
    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }

    /**
     * 失败响应（携带数据，用于需要返回部分数据时的错误场景）
     *
     * @param code    业务错误码
     * @param message 错误描述
     * @param data    附加数据
     */
    public static <T> Result<T> error(int code, String message, T data) {
        return new Result<>(code, message, data);
    }
}
