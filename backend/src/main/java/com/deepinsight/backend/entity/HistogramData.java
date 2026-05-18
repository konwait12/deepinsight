package com.deepinsight.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "histogram_data")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class HistogramData {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "run_id", nullable = false) private Long runId;
    @Column(nullable = false, length = 200) private String tag;
    @Column(nullable = false) private Long step;
    @Column(name = "wall_time") private Double wallTime;
    @Column(name = "limits_json", columnDefinition = "MEDIUMTEXT") private String limitsJson;
    @Column(name = "counts_json", columnDefinition = "MEDIUMTEXT") private String countsJson;
    @Column(name = "sum_val") private Double sumVal;
    @Column(name = "sum_squares") private Double sumSquares;
    @Column(name = "total_count") private Long totalCount;
    @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
}
