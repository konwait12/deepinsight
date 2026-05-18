package com.deepinsight.backend.repository;

import com.deepinsight.backend.entity.EmbeddingData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface EmbeddingDataRepository extends JpaRepository<EmbeddingData, Long> {
    List<EmbeddingData> findByRunIdAndTagOrderByStepAsc(Long runId, String tag);

    @Query("SELECT DISTINCT e.tag FROM EmbeddingData e WHERE e.runId = :runId")
    List<String> findDistinctTagsByRunId(Long runId);

    void deleteByRunId(Long runId);
}
