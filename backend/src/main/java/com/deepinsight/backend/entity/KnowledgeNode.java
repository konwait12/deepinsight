package com.deepinsight.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/** 知识节点实体 */
@Entity @Table(name = "knowledge_nodes")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class KnowledgeNode {
    @Id @Column(length = 50) private String id;
    @Column(name = "parent_id", length = 50) private String parentId;
    @Column(length = 100, nullable = false) private String label;
    @Column(length = 50) private String category;
    @Column(columnDefinition = "TEXT") private String description;
    @Column(length = 7) private String color;
    @Column(name = "size_val") private Double sizeVal;
    @Column(name = "sort_order") private Integer sortOrder;
    @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
    @PrePersist void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }
}
