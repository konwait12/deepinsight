package com.deepinsight.backend.repository;

import com.deepinsight.backend.entity.TrainingStep;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TrainingStepRepository extends JpaRepository<TrainingStep, Long> {
    List<TrainingStep> findByJobIdOrderByEpochAsc(Long jobId);
    void deleteByJobId(Long jobId);
}
