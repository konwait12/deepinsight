package com.deepinsight.backend.repository;

import com.deepinsight.backend.entity.ModelRegistry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ModelRegistryRepository extends JpaRepository<ModelRegistry, Long> {
    List<ModelRegistry> findByIsOfficialTrueOrderByNameAsc();
    List<ModelRegistry> findByCreatedByOrderByNameAsc(Long createdBy);
    List<ModelRegistry> findByTaskTypeOrderByNameAsc(String taskType);
    Optional<ModelRegistry> findByName(String name);
}
