package com.deepinsight.backend.config;

import com.deepinsight.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * 应用基础配置类
 * <p>
 * 定义Spring Security所需的核心Bean。
 * 当前提供UserDetailsService，用于从数据库加载用户信息供认证流程使用。
 *
 * @author DeepInsight Team
 */
@Configuration
@RequiredArgsConstructor
public class ApplicationConfig {

    private final UserRepository repository;

    /**
     * 用户详情服务Bean
     * <p>
     * 实现Spring Security的UserDetailsService接口，
     * 通过用户名从数据库查询用户信息。
     * 该方法被DaoAuthenticationProvider调用，
     * 用于加载用户实体并验证密码。
     *
     * @return UserDetailsService实例（Lambda表达式实现）
     * @throws UsernameNotFoundException 如果数据库中不存在该用户名
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> repository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));
    }
}
