package com.deepinsight.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "pr_curve_data")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PRCurveData {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "run_id", nullable = false) private Long runId;
    @Column(nullable = false, length = 200) private String tag;
    @Column(nullable = false) private Long step;
    @Column(name = "wall_time") private Double wallTime;
    @Column(name = "precision_json", columnDefinition = "MEDIUMTEXT") private String precisionJson;
    @Column(name = "recall_json", columnDefinition = "MEDIUMTEXT") private String recallJson;
    @Column(name = "thresholds_json", columnDefinition = "MEDIUMTEXT") private String thresholdsJson;
    @Column(name = "num_thresholds") private Integer numThresholds;
    @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
}
