package com.deepinsight.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/** 论坛帖子实体 */
@Entity @Table(name = "forum_posts")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ForumPost {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(length = 300, nullable = false) private String title;
    @Column(columnDefinition = "LONGTEXT", nullable = false) private String content;
    @Column(name = "cover_url", length = 500) private String coverUrl;
    @Column(name = "source_type", length = 40) private String sourceType;
    @Column(name = "source_id") private Long sourceId;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "is_official") @Builder.Default private Boolean isOfficial = false;
    @Column(name = "is_pinned") @Builder.Default private Boolean isPinned = false;
    @Column(name = "is_locked") @Builder.Default private Boolean isLocked = false;
    @Column(name = "view_count") @Builder.Default private Integer viewCount = 0;
    @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
    @PrePersist void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }
}
