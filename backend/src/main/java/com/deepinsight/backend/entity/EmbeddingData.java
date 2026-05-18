package com.deepinsight.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "embedding_data")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class EmbeddingData {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "run_id", nullable = false) private Long runId;
    @Column(nullable = false, length = 200) private String tag;
    @Column(nullable = false) private Long step;
    @Column(name = "wall_time") private Double wallTime;
    @Column(name = "values_json", columnDefinition = "MEDIUMTEXT") private String valuesJson;
    @Column(length = 200) private String label;
    @Column(name = "class_id") private Integer classId;
    @Column(name = "sample_id") private Long sampleId;
    @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
}
