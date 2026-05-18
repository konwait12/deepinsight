package com.deepinsight.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * DeepInsight 深度学习可视化分析平台 — 后端启动类
 * <p>
 * 该平台为深度学习研究人员提供数据管理、模型训练监控、
 * 可视化分析和预测测试等核心功能。
 * 后端采用 Spring Boot 3.2.0 + Spring Security + JWT 认证方案，
 * 遵循Spring MVC三层分层架构。
 *
 * <p>核心注解说明：
 * <ul>
 *   <li>{@code @SpringBootApplication} — 自动配置、组件扫描、声明式事务</li>
 *   <li>{@code @EnableJpaAuditing} — 启用JPA审计，自动填充{@code @CreatedDate}等字段</li>
 * </ul>
 *
 * @author DeepInsight Team
 */
@SpringBootApplication
@EnableJpaAuditing
public class DeepInsightBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeepInsightBackendApplication.class, args);
    }
}
