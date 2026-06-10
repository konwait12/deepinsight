package com.deepinsight.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 数据集实体
 * <p>
 * 记录用户上传的数据集元信息，当前主要用于推荐数据、表格数据和工作区素材。
 * 实际文件存储在文件系统中，此表记录索引和状态。
 */
@Entity
@Table(name = "datasets")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Dataset {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 数据集名称 */
    @Column(nullable = false, length = 200)
    private String name;

    /** 描述 */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** 任务类型：recommendation/nlp/audio/other */
    @Column(name = "task_type", length = 50)
    private String taskType;

    /** 数据格式：csv/zip/txt/recbole_atomic/user_sequence */
    @Column(length = 30)
    private String format;

    /** 文件存储路径 */
    @Column(name = "file_path", length = 1000)
    private String filePath;

    /** 文件大小（MB） */
    @Column(name = "file_size_mb")
    private Double fileSizeMb;

    /** 样本数量 */
    @Column(name = "sample_count")
    private Integer sampleCount;

    /** 类别数量 */
    @Column(name = "class_count")
    private Integer classCount;

    /** 训练集/验证集/测试集比例 */
    @Column(name = "split_ratio", length = 50)
    private String splitRatio;

    /** 状态：uploading/ready/processing/error */
    @Column(length = 20)
    @Builder.Default
    private String status = "uploading";

    /** 上传用户 */
    @Column(name = "uploaded_by")
    private Long uploadedBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); }
}
