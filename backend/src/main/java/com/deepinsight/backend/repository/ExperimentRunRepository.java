package com.deepinsight.backend.repository;

import com.deepinsight.backend.entity.ExperimentRun;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExperimentRunRepository extends JpaRepository<ExperimentRun, Long> {
    List<ExperimentRun> findByUserIdOrderByCreatedAtDesc(Long userId);
}
