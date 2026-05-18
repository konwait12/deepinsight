package com.deepinsight.backend.repository;

import com.deepinsight.backend.entity.TextLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface TextLogRepository extends JpaRepository<TextLog, Long> {
    List<TextLog> findByRunIdAndTagOrderByStepAsc(Long runId, String tag);

    @Query("SELECT DISTINCT t.tag FROM TextLog t WHERE t.runId = :runId")
    List<String> findDistinctTagsByRunId(Long runId);

    void deleteByRunId(Long runId);
}
