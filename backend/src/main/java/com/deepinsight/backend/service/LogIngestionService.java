package com.deepinsight.backend.service;

import java.io.InputStream;
import java.util.List;

public interface LogIngestionService {
    int ingest(Long runId, InputStream inputStream) throws Exception;

    List<String> getTags(Long runId);

    void deleteRunData(Long runId);
}
