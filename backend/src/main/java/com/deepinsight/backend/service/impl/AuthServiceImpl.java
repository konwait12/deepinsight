package com.deepinsight.backend.service.impl;

import com.deepinsight.backend.common.Constants;
import com.deepinsight.backend.dto.AuthRequest;
import com.deepinsight.backend.dto.AuthResponse;
import com.deepinsight.backend.entity.User;
import com.deepinsight.backend.repository.UserRepository;
import com.deepinsight.backend.security.JwtUtils;
import com.deepinsight.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 认证服务实现类（Service层 - 业务逻辑层）
 * <p>
 * 实现用户注册和登录认证的核心业务逻辑。
 * 采用BCrypt算法对密码进行哈希处理，使用JWT进行身份令牌生成。
 * 注册时默认分配USER角色，可通过管理员接口升级为RESEARCHER或ADMIN。
 *
 * <p>依赖关系：
 * <ul>
 *   <li>{@link UserRepository} — 用户数据持久化</li>
 *   <li>{@link PasswordEncoder} — BCrypt密码编码</li>
 *   <li>{@link JwtUtils} — JWT令牌生成与验证</li>
 *   <li>{@link AuthenticationManager} — Spring Security认证管理器</li>
 * </ul>
 *
 * @author DeepInsight Team
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;

    /**
     * 用户注册
     * <p>
     * 业务流程：
     * <ol>
     *   <li>校验用户名唯一性，防止重复注册</li>
     *   <li>如有邮箱，校验邮箱唯一性</li>
     *   <li>使用BCrypt对明文密码进行哈希编码</li>
     *   <li>创建User实体（默认角色USER）并持久化到数据库</li>
     *   <li>为新用户生成JWT令牌，实现注册即登录</li>
     * </ol>
     *
     * @param request 注册请求（含用户名、密码，可选邮箱）
     * @return 包含JWT令牌和用户基本信息的认证响应
     * @throws IllegalArgumentException 如果用户名或邮箱已被注册
     */
    @Override
    @Transactional
    public AuthResponse register(AuthRequest request) {
        // 1. 校验用户名唯一性
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("用户名已被注册: " + request.getUsername());
        }

        // 2. 校验邮箱唯一性（如果提供了邮箱）
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException("邮箱已被注册: " + request.getEmail());
            }
        }

        // 3. 构建用户实体，BCrypt加密密码
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .role(User.Role.USER)
                .build();

        // 4. 持久化到数据库
        user = userRepository.save(user);

        // 5. 生成JWT令牌
        String jwtToken = jwtUtils.generateToken(user);

        // 6. 构建响应
        return AuthResponse.builder()
                .token(jwtToken)
                .username(user.getUsername())
                .role(user.getRole().name())
                .build();
    }

    /**
     * 用户登录认证
     * <p>
     * 业务流程：
     * <ol>
     *   <li>通过Spring Security的AuthenticationManager验证用户名密码</li>
     *   <li>认证管理器内部调用UserDetailsService加载用户，比对BCrypt密码</li>
     *   <li>认证成功后加载用户信息并生成JWT令牌</li>
     * </ol>
     *
     * @param request 登录请求（含用户名和明文密码）
     * @return 包含JWT令牌和用户基本信息的认证响应
     * @throws BadCredentialsException 如果用户名或密码错误
     */
    @Override
    public AuthResponse authenticate(AuthRequest request) {
        try {
            // 1. 执行Spring Security认证流程
            //    DaoAuthenticationProvider会调用UserDetailsService.loadUserByUsername()
            //    然后用PasswordEncoder.matches()比对密码
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );
        } catch (AuthenticationException e) {
            // 2. 认证失败，返回友好提示
            throw new BadCredentialsException("用户名或密码错误", e);
        }

        // 3. 认证成功，从数据库加载用户完整信息
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalStateException("认证成功但用户不存在，数据异常"));

        // 4. 生成JWT令牌并构建响应
        String jwtToken = jwtUtils.generateToken(user);

        return AuthResponse.builder()
                .token(jwtToken)
                .username(user.getUsername())
                .role(user.getRole().name())
                .build();
    }
}
