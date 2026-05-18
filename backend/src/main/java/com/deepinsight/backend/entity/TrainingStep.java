package com.deepinsight.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "training_steps")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TrainingStep {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "job_id", nullable = false) private Long jobId;
    @Column(nullable = false) private Integer epoch;
    @Column(name = "train_loss") private Double trainLoss;
    @Column(name = "val_loss") private Double valLoss;
    @Column(name = "train_accuracy") private Double trainAccuracy;
    @Column(name = "val_accuracy") private Double valAccuracy;
    @Column(name = "learning_rate") private Double learningRate;
    @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
}
