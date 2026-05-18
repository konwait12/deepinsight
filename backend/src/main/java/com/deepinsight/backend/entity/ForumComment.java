package com.deepinsight.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/** 论坛评论实体 */
@Entity @Table(name = "forum_comments")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ForumComment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "post_id", nullable = false) private Long postId;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(columnDefinition = "TEXT", nullable = false) private String content;
    @Column(name = "parent_id") private Long parentId;
    @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
}
