package com.deepinsight.backend.repository;

import com.deepinsight.backend.entity.ProfilerData;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProfilerDataRepository extends JpaRepository<ProfilerData, Long> {
    List<ProfilerData> findByRunIdAndTagOrderByStepAsc(Long runId, String tag);

    void deleteByRunId(Long runId);
}
