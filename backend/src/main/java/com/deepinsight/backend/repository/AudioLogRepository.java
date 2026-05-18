package com.deepinsight.backend.repository;

import com.deepinsight.backend.entity.AudioLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface AudioLogRepository extends JpaRepository<AudioLog, Long> {
    List<AudioLog> findByRunIdAndTagOrderByStepAsc(Long runId, String tag);

    @Query("SELECT DISTINCT a.tag FROM AudioLog a WHERE a.runId = :runId")
    List<String> findDistinctTagsByRunId(Long runId);

    void deleteByRunId(Long runId);
}
