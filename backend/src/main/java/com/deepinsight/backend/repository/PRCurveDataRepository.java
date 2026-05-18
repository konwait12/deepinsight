package com.deepinsight.backend.repository;

import com.deepinsight.backend.entity.PRCurveData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface PRCurveDataRepository extends JpaRepository<PRCurveData, Long> {
    List<PRCurveData> findByRunIdAndTagOrderByStepAsc(Long runId, String tag);

    @Query("SELECT DISTINCT p.tag FROM PRCurveData p WHERE p.runId = :runId")
    List<String> findDistinctTagsByRunId(Long runId);

    void deleteByRunId(Long runId);
}
