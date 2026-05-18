package com.deepinsight.backend.security;

import com.deepinsight.backend.common.Constants;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Spring Security 安全配置类
 * <p>
 * 配置平台的安全策略：
 * <ul>
 *   <li><b>认证方式</b>：无状态JWT令牌认证，不使用Session</li>
 *   <li><b>密码编码</b>：BCrypt哈希算法（自动包含随机盐）</li>
 *   <li><b>CSRF</b>：已禁用（前后端分离 + 无状态Token不需要CSRF保护）</li>
 *   <li><b>CORS</b>：由独立的 {@link com.deepinsight.backend.config.CorsConfig} 管理</li>
 *   <li><b>公开接口</b>：登录注册、论坛浏览、模型文章和官方模型列表无需认证，其余接口均需JWT验证</li>
 * </ul>
 * <p>
 * 安全过滤链执行顺序：
 * <ol>
 *   <li>CORS过滤器处理跨域请求</li>
 *   <li>JwtAuthenticationFilter提取并校验JWT令牌</li>
 *   <li>UsernamePasswordAuthenticationFilter（本项目中不触发，因无表单登录）</li>
 *   <li>授权检查（AuthorizeHttpRequests）</li>
 * </ol>
 *
 * @author DeepInsight Team
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /** JWT认证过滤器 */
    private final JwtAuthenticationFilter jwtAuthFilter;

    /** Spring Security用户详情服务 */
    private final UserDetailsService userDetailsService;

    /** CORS配置源（由CorsConfig独立提供Bean） */
    private final CorsConfigurationSource corsConfigurationSource;

    /**
     * 安全过滤链配置
     * <p>
     * 定义HTTP请求的安全处理规则：
     * CSRF禁用 → CORS启用 → 授权规则 → 无状态会话 → 认证提供者 → JWT过滤器前置。
     *
     * @param http Spring Security的HTTP安全构建器
     * @return 组装完成的安全过滤链
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 禁用CSRF保护（前后端分离架构中，API使用JWT Token认证，无需CSRF）
            .csrf(AbstractHttpConfigurer::disable)

            // 启用CORS，使用独立的CorsConfig配置
            .cors(cors -> cors.configurationSource(corsConfigurationSource))

            // 配置接口权限
            .authorizeHttpRequests(auth -> auth
                // 认证模块、论坛浏览和静态资源等公开接口无需认证即可访问
                .requestMatchers(Constants.PUBLIC_PATHS).permitAll()
                // 官方模型列表是训练页的只读入口，匿名只允许读取，不能创建或修改模型
                .requestMatchers(HttpMethod.GET, Constants.PUBLIC_GET_PATHS).permitAll()
                // 其余所有接口需要认证
                .anyRequest().authenticated()
            )

            // 设置会话管理为无状态（不创建HttpSession，每次请求携带JWT）
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // 注册自定义认证提供者
            .authenticationProvider(authenticationProvider())

            // 在UsernamePasswordAuthenticationFilter之前插入JWT过滤器
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 认证提供者Bean
     * <p>
     * 使用DaoAuthenticationProvider，基于数据库中的用户信息进行认证。
     * 内部调用UserDetailsService加载用户 → 比对PasswordEncoder加密的密码。
     *
     * @return 认证提供者实例
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        // 设置用户详情服务（从数据库加载用户）
        authProvider.setUserDetailsService(userDetailsService);
        // 设置密码编码器（BCrypt）
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * 认证管理器Bean
     * <p>
     * 由Spring Security基于已注册的AuthenticationProvider自动构建。
     * AuthService中调用此管理器执行用户名密码验证。
     *
     * @param config 认证配置
     * @return 认证管理器实例
     * @throws Exception 配置异常
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * 密码编码器Bean
     * <p>
     * 使用BCrypt算法（Blowfish加密的变体）。
     * BCrypt特点：自动生成随机盐并嵌入哈希结果中，强度因子默认10。
     * 输出格式：$2a$10$[22位盐值+31位哈希值]，总长度60字符。
     *
     * @return BCrypt密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
