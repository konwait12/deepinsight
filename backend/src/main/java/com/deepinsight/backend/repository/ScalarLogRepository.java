package com.deepinsight.backend.repository;

import com.deepinsight.backend.entity.ScalarLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface ScalarLogRepository extends JpaRepository<ScalarLog, Long> {
    List<ScalarLog> findByRunIdAndTagOrderByStepAsc(Long runId, String tag);

    @Query("SELECT DISTINCT s.tag FROM ScalarLog s WHERE s.runId = :runId")
    List<String> findDistinctTagsByRunId(Long runId);

    void deleteByRunId(Long runId);
}
