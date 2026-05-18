package com.deepinsight.backend.service;

import com.deepinsight.backend.dto.AuthRequest;
import com.deepinsight.backend.dto.AuthResponse;
import org.springframework.security.authentication.BadCredentialsException;

/**
 * 认证服务接口
 * <p>
 * 定义用户注册、登录认证等核心认证业务操作规范。
 * 由 {@link com.deepinsight.backend.service.impl.AuthServiceImpl} 提供具体实现。
 * 此接口遵循Spring MVC分层架构：Controller → Service接口 → ServiceImpl → Repository。
 *
 * @author DeepInsight Team
 */
public interface AuthService {

    /**
     * 用户注册
     * <p>
     * 验证用户名唯一性，使用BCrypt加密密码，
     * 创建用户记录并返回JWT令牌，实现注册即登录。
     *
     * @param request 包含用户名和密码的注册请求
     * @return 包含JWT令牌的认证响应
     * @throws IllegalArgumentException 如果用户名已存在
     */
    AuthResponse register(AuthRequest request);

    /**
     * 用户登录认证
     * <p>
     * 通过Spring Security的AuthenticationManager验证用户名密码，
     * 认证成功后生成并返回JWT令牌。
     *
     * @param request 包含用户名和密码的登录请求
     * @return 包含JWT令牌的认证响应
     * @throws BadCredentialsException 如果用户名或密码错误
     */
    AuthResponse authenticate(AuthRequest request);
}
