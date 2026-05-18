package com.deepinsight.backend.common;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * <p>
 * 使用 @RestControllerAdvice 拦截所有Controller中抛出的异常，
 * 统一转换为 Result 格式的错误响应，确保前端能一致地处理错误。
 *
 * <p>处理的异常类型：
 * <ul>
 *   <li>{@link IllegalArgumentException} — 参数校验失败（如用户名重复） → 400</li>
 *   <li>{@link MethodArgumentNotValidException} — @Valid校验失败 → 400</li>
 *   <li>{@link BadCredentialsException} — 认证失败 → 401</li>
 *   <li>{@link RuntimeException} — 其他未预期异常 → 500</li>
 * </ul>
 *
 * @author DeepInsight Team
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务参数异常
     * <p>
     * 如：用户名已存在、邮箱已注册等业务校验失败。
     *
     * @param e 参数异常
     * @return 400错误响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleIllegalArgument(IllegalArgumentException e) {
        return Result.error(Constants.CODE_BAD_REQUEST, e.getMessage());
    }

    /**
     * 处理 @Valid 校验失败
     * <p>
     * 提取所有字段校验错误信息，拼接后返回。
     *
     * @param e 校验异常
     * @return 400错误响应（含所有字段的错误信息）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return Result.error(Constants.CODE_BAD_REQUEST, message);
    }

    /**
     * 处理认证失败异常
     * <p>
     * 用户名或密码错误时由AuthServiceImpl抛出。
     *
     * @param e 认证异常
     * @return 401错误响应
     */
    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleBadCredentials(BadCredentialsException e) {
        return Result.error(Constants.CODE_UNAUTHORIZED, e.getMessage());
    }

    /**
     * 处理其他未预期的运行时异常
     * <p>
     * 兜底处理，防止堆栈信息泄漏给前端。
     *
     * @param e 运行时异常
     * @return 500错误响应
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleRuntime(RuntimeException e) {
        return Result.error(Constants.CODE_INTERNAL_ERROR, "服务器内部错误: " + e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleIllegalState(IllegalStateException e) {
        return Result.error(Constants.CODE_BAD_REQUEST, e.getMessage());
    }
}
