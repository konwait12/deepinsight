package com.deepinsight.backend.security;

import com.deepinsight.backend.common.Constants;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT认证过滤器
 * <p>
 * 在每个HTTP请求到达Controller之前执行，负责：
 * <ol>
 *   <li>从请求头中提取Authorization令牌</li>
 *   <li>验证JWT令牌的有效性（签名 + 过期时间）</li>
 *   <li>令牌有效则将用户信息写入SecurityContext，标记为已认证</li>
 *   <li>令牌无效或缺失则直接放行，由后续授权检查决定是否拒绝</li>
 * </ol>
 *
 * <p>过滤器位置：在SecurityConfig中注册，
 * 执行顺序在UsernamePasswordAuthenticationFilter之前。
 *
 * <p>继承OncePerRequestFilter确保每个请求只过滤一次。
 *
 * @author DeepInsight Team
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** JWT令牌工具类 */
    private final JwtUtils jwtUtils;

    /** Spring Security用户详情服务（从数据库加载用户） */
    private final UserDetailsService userDetailsService;

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    /**
     * 请求过滤核心逻辑
     * <p>
     * 处理流程：
     * <ol>
     *   <li>检查Authorization头是否存在且以"Bearer "开头 → 否则直接放行</li>
     *   <li>提取Token值（去除"Bearer "前缀）</li>
     *   <li>从Token中解析用户名</li>
     *   <li>如果当前SecurityContext尚未认证且用户名有效，加载用户信息</li>
     *   <li>验证Token有效性，有效则设置认证状态</li>
     *   <li>继续执行过滤器链</li>
     * </ol>
     *
     * @param request     HTTP请求
     * @param response    HTTP响应
     * @param filterChain 过滤器链
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        // 1. 从请求头中提取Authorization值
        final String authHeader = request.getHeader(Constants.AUTHORIZATION_HEADER);

        // 2. 如果没有Authorization头或不是Bearer格式，直接放行
        if (authHeader == null || !authHeader.startsWith(Constants.TOKEN_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. 提取Token（去除"Bearer "前缀）
        final String jwt = authHeader.substring(Constants.TOKEN_PREFIX_LENGTH);

        // 4. 从Token中解析用户名（过期Token会抛出ExpiredJwtException，捕获后放行）
        final String username;
        try {
            username = jwtUtils.extractUsername(jwt);
        } catch (JwtException e) {
            logger.debug("Invalid JWT token: {}", e.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        // 5. 如果用户名有效且当前未认证，进行Token验证
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // 从数据库加载用户信息（利用Spring Security缓存机制）
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

            // 验证Token有效性（签名 + 过期时间 + 用户名匹配）
            if (jwtUtils.isTokenValid(jwt, userDetails)) {
                // 创建认证令牌（已认证状态，密码为null因为是Token认证）
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

                // 设置请求详情（IP、Session等）
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 将认证信息写入SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // 6. 继续执行过滤器链
        filterChain.doFilter(request, response);
    }
}
