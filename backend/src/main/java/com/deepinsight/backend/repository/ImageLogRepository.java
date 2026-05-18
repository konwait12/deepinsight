package com.deepinsight.backend.repository;

import com.deepinsight.backend.entity.ImageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface ImageLogRepository extends JpaRepository<ImageLog, Long> {
    List<ImageLog> findByRunIdAndTagOrderByStepAsc(Long runId, String tag);

    @Query("SELECT DISTINCT i.tag FROM ImageLog i WHERE i.runId = :runId")
    List<String> findDistinctTagsByRunId(Long runId);

    void deleteByRunId(Long runId);
}
