package com.deepinsight.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "model_registry")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ModelRegistry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 200, nullable = false)
    private String name;

    @Column(name = "display_name_zh", length = 200)
    private String displayNameZh;

    @Column(length = 50, nullable = false)
    private String taskType;  // recommendation, nlp, audio, other

    @Column(name = "task_type_zh", length = 80)
    private String taskTypeZh;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "description_zh", columnDefinition = "TEXT")
    private String descriptionZh;

    @Column(length = 500)
    private String paperUrl;

    @Column(name = "param_count_m", nullable = false)
    @Builder.Default
    private Double paramCountM = 0.0;

    @Column(length = 100)
    private String inputSize;  // e.g. "224x224x3"

    @Column(length = 50)
    private String framework;  // pytorch, tensorflow, jax, etc.

    @Column(name = "is_official", nullable = false)
    @Builder.Default
    private Boolean isOfficial = false;

    @Column(name = "created_by")
    private Long createdBy;  // null for official models

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }
}
