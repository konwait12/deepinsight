package com.deepinsight.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 训练任务实体
 * <p>
 * 记录模型训练任务的完整生命周期：配置 → 排队 → 运行 → 完成/失败。
 */
@Entity
@Table(name = "training_jobs")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TrainingJob {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 任务名称 */
    @Column(nullable = false, length = 200)
    private String name;

    /** 使用的模型架构 */
    @Column(name = "model_architecture", length = 100)
    private String modelArchitecture;

    /** 数据集ID（关联datasets表） */
    @Column(name = "dataset_id")
    private Long datasetId;

    /** 训练配置（JSON格式：超参数等） */
    @Column(name = "config_json", columnDefinition = "TEXT")
    private String configJson;

    /** 学习率 */
    @Column(name = "learning_rate")
    @Builder.Default
    private Double learningRate = 0.001;

    /** 批次大小 */
    @Column(name = "batch_size")
    @Builder.Default
    private Integer batchSize = 32;

    /** 最大训练轮次 */
    @Column(nullable = false)
    @Builder.Default
    private Integer epochs = 100;

    /** 优化器：adam/sgd/adamw/rmsprop */
    @Column(length = 20)
    @Builder.Default
    private String optimizer = "adam";

    /** 当前轮次 */
    @Column(name = "current_epoch")
    @Builder.Default
    private Integer currentEpoch = 0;

    /** 状态：queued/running/completed/failed/paused */
    @Column(length = 20)
    @Builder.Default
    private String status = "queued";

    /** 当前损失值 */
    @Column(name = "current_loss")
    private Double currentLoss;

    /** 当前准确率 */
    @Column(name = "current_accuracy")
    private Double currentAccuracy;

    /** 总训练时间（秒） */
    @Column(name = "total_time_sec")
    private Integer totalTimeSec;

    /** 设备：cpu/cuda:0/cuda:1 */
    @Column(length = 20)
    @Builder.Default
    private String device = "cpu";

    /** 发起训练的用户 */
    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = createdAt; }
    @PreUpdate protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
