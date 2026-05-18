package com.deepinsight.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/** 知识文章实体(官方) */
@Entity @Table(name = "knowledge_articles")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class KnowledgeArticle {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "node_id", length = 50) private String nodeId;
    @Column(length = 300, nullable = false) private String title;
    @Column(columnDefinition = "LONGTEXT", nullable = false) private String content;
    @Column(name = "author_id") private Long authorId;
    @Column(name = "is_pinned") @Builder.Default private Boolean isPinned = false;
    @Column(name = "view_count") @Builder.Default private Integer viewCount = 0;
    @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
    @PrePersist void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }
}
