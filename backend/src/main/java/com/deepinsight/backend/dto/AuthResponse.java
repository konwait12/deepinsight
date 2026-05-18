package com.deepinsight.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 认证响应DTO
 * <p>
 * 注册和登录接口的统一响应数据结构。
 * 包含JWT令牌和基本用户信息，前端可根据role进行路由守卫和权限控制。
 *
 * @author DeepInsight Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    /** JWT访问令牌（后续请求需放在Authorization头中，格式：Bearer <token>） */
    private String token;

    /** 认证成功的用户名 */
    private String username;

    /** 用户角色（USER / RESEARCHER / ADMIN），前端据此进行页面权限控制 */
    private String role;

    /** Token类型，固定为"Bearer" */
    @Builder.Default
    private String tokenType = "Bearer";
}
