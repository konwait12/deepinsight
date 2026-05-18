package com.deepinsight.backend.entity;

import lombok.*;
import java.time.LocalDateTime;

/** AI对话消息 — 存储在Redis中 (chat:session:{convId}) */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ChatMessage {
    private Long id;
    private Long conversationId;
    private String role;
    private String content;
    @Builder.Default private Integer tokenCount = 0;
    private LocalDateTime createdAt;
}
