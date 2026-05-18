package com.deepinsight.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "hparam_data")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class HParamData {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "run_id", nullable = false) private Long runId;
    @Column(nullable = false, length = 200) private String tag;
    @Column(nullable = false) private Long step;
    @Column(name = "wall_time") private Double wallTime;
    @Column(name = "metric_values_json", columnDefinition = "MEDIUMTEXT") private String metricValuesJson;
    @Column(name = "string_values_json", columnDefinition = "MEDIUMTEXT") private String stringValuesJson;
    @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
}
