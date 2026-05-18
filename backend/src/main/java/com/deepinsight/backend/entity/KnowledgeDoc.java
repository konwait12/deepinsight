package com.deepinsight.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/** AI知识库文档实体 */
@Entity @Table(name = "knowledge_docs")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class KnowledgeDoc {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(length = 300, nullable = false) private String title;
    @Column(columnDefinition = "LONGTEXT", nullable = false) private String content;
    @Column(length = 50) private String category;
    @Column(length = 500) private String tags;
    @Column(name = "embedding_vector", columnDefinition = "TEXT") private String embeddingVector;
    @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); }
}
