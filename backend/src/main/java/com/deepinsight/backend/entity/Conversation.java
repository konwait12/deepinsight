package com.deepinsight.backend.entity;

import lombok.*;
import java.time.LocalDateTime;

/** AI对话会话 — 存储在Redis中 (chat:conv:{id}) */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Conversation {
    private Long id;
    private Long userId;
    @Builder.Default private String title = "新对话";
    private String modelType;
    private String modelName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
