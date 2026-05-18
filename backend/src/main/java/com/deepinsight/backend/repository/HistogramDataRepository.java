package com.deepinsight.backend.repository;

import com.deepinsight.backend.entity.HistogramData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface HistogramDataRepository extends JpaRepository<HistogramData, Long> {
    List<HistogramData> findByRunIdAndTagOrderByStepAsc(Long runId, String tag);

    @Query("SELECT DISTINCT h.tag FROM HistogramData h WHERE h.runId = :runId")
    List<String> findDistinctTagsByRunId(Long runId);

    void deleteByRunId(Long runId);
}
