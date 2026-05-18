package com.deepinsight.backend.repository;

import com.deepinsight.backend.entity.AiConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AiConfigRepository extends JpaRepository<AiConfig, Long> {
    Optional<AiConfig> findByIsActiveTrue();
    Optional<AiConfig> findByName(String name);
}
