package com.deepinsight.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 认证请求DTO
 * <p>
 * 用于接收注册和登录接口的请求体参数。
 * 使用Jakarta Validation进行参数校验，
 * 校验失败时Spring自动返回400 Bad Request。
 *
 * @author DeepInsight Team
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthRequest {

    /** 用户名（3-50个字符，不可为空） */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度需在3-50个字符之间")
    private String username;

    /** 密码（6-100个字符，不可为空） */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度需在6-100个字符之间")
    private String password;

    /** 邮箱（可选，用于注册时绑定邮箱） */
    @Size(max = 100, message = "邮箱长度不能超过100个字符")
    private String email;
}
