package com.deepinsight.backend.repository;

import com.deepinsight.backend.entity.RocCurveData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface RocCurveDataRepository extends JpaRepository<RocCurveData, Long> {
    List<RocCurveData> findByRunIdAndTagOrderByStepAsc(Long runId, String tag);

    @Query("SELECT DISTINCT r.tag FROM RocCurveData r WHERE r.runId = :runId")
    List<String> findDistinctTagsByRunId(Long runId);

    void deleteByRunId(Long runId);
}
