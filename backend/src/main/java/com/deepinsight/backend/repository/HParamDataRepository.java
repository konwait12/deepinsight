package com.deepinsight.backend.repository;

import com.deepinsight.backend.entity.HParamData;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HParamDataRepository extends JpaRepository<HParamData, Long> {
    List<HParamData> findByRunIdOrderByStepAsc(Long runId);

    void deleteByRunId(Long runId);
}
