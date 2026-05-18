package com.deepinsight.backend.repository;

import com.deepinsight.backend.entity.Dataset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DatasetRepository extends JpaRepository<Dataset, Long> {
    List<Dataset> findByUploadedByOrderByCreatedAtDesc(Long uploadedBy);
    List<Dataset> findByStatusOrderByCreatedAtDesc(String status);
    List<Dataset> findByTaskType(String taskType);
}
