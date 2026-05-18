package com.deepinsight.backend.entity;

import lombok.*;
import java.time.LocalDateTime;

/** AI长期记忆 — 存储在Redis中 (user:mem:{userId}) */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AiMemory {
    private Long id;
    private Long userId;
    @Builder.Default private String memoryType = "fact";
    private String content;
    private String embeddingVector;
    @Builder.Default private Float importance = 0.5f;
    @Builder.Default private Integer accessCount = 0;
    private LocalDateTime lastAccessed;
    private LocalDateTime createdAt;
}
