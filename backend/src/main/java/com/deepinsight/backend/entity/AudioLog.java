package com.deepinsight.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "audio_logs")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AudioLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "run_id", nullable = false) private Long runId;
    @Column(nullable = false, length = 200) private String tag;
    @Column(nullable = false) private Long step;
    @Column(name = "wall_time") private Double wallTime;
    @Column(length = 300) private String filename;
    @Column(name = "sample_rate") private Float sampleRate;
    @Column(name = "num_channels") private Integer numChannels;
    @Column(name = "num_frames") private Integer numFrames;
    @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
}
