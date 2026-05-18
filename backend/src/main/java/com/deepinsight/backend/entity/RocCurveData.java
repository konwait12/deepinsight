package com.deepinsight.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "roc_curve_data")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RocCurveData {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "run_id", nullable = false) private Long runId;
    @Column(nullable = false, length = 200) private String tag;
    @Column(nullable = false) private Long step;
    @Column(name = "wall_time") private Double wallTime;
    @Column(name = "tpr_json", columnDefinition = "MEDIUMTEXT") private String tprJson;
    @Column(name = "fpr_json", columnDefinition = "MEDIUMTEXT") private String fprJson;
    @Column(name = "thresholds_json", columnDefinition = "MEDIUMTEXT") private String thresholdsJson;
    @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
}
