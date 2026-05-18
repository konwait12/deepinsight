package com.deepinsight.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * AI助手配置实体
 * <p>
 * 存储AI助手的模型配置、人设、提示词模板等设置。
 * 支持多种模型类型：本地Ollama模型、OpenAI API、Gemini API等。
 */
@Entity
@Table(name = "ai_configs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiConfig {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 配置名称（如：默认助手、代码助手、论文助手） */
    @Column(nullable = false, length = 100)
    private String name;

    /** 模型类型：ollama / openai / gemini */
    @Column(name = "model_type", nullable = false, length = 20)
    private String modelType;

    /** 模型名称（如：qwen2.5:7b / gpt-4o / gemini-2.5-flash） */
    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    /** API地址（Ollama: http://localhost:11434, OpenAI: https://api.openai.com/v1） */
    @Column(name = "api_url", length = 500)
    private String apiUrl;

    /** API密钥（加密存储建议，当前明文） */
    @Column(name = "api_key", length = 500)
    private String apiKey;

    /** 系统提示词（人设定义） */
    @Column(name = "system_prompt", columnDefinition = "TEXT")
    private String systemPrompt;

    /** 温度参数 (0-2)，控制输出随机性 */
    @Column(name = "temperature", columnDefinition = "DOUBLE DEFAULT 0.7")
    @Builder.Default
    private Double temperature = 0.7;

    /** 最大输出Token数 */
    @Column(name = "max_tokens")
    @Builder.Default
    private Integer maxTokens = 4096;

    /** 上下文窗口大小（保留最近N轮对话） */
    @Column(name = "context_window")
    @Builder.Default
    private Integer contextWindow = 10;

    /** 是否启用（同一时间只有一个激活） */
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = false;

    /** 知识库内容（可为空，用于RAG增强） */
    @Column(name = "knowledge_base", columnDefinition = "MEDIUMTEXT")
    private String knowledgeBase;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = createdAt; }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
