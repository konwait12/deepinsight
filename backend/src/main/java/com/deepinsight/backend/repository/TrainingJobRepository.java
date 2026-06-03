package com.deepinsight.backend.repository;

import com.deepinsight.backend.entity.TrainingJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TrainingJobRepository extends JpaRepository<TrainingJob, Long> {
    List<TrainingJob> findByCreatedByOrderByCreatedAtDesc(Long createdBy);
    List<TrainingJob> findByCreatedByIsNullOrderByCreatedAtDesc();
    List<TrainingJob> findByCreatedByAndStatusOrderByCreatedAtDesc(Long createdBy, String status);
    List<TrainingJob> findByStatusOrderByCreatedAtDesc(String status);
    List<TrainingJob> findByStatus(String status);
}
