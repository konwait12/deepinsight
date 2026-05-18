package com.deepinsight.backend.service.impl;

import com.deepinsight.backend.entity.*;
import com.deepinsight.backend.repository.*;
import com.deepinsight.backend.service.LogIngestionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.*;
import java.util.zip.CRC32;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogIngestionServiceImpl implements LogIngestionService {

    private final ScalarLogRepository scalarLogRepository;
    private final ImageLogRepository imageLogRepository;
    private final AudioLogRepository audioLogRepository;
    private final TextLogRepository textLogRepository;
    private final HistogramDataRepository histogramDataRepository;
    private final EmbeddingDataRepository embeddingDataRepository;
    private final PRCurveDataRepository prCurveDataRepository;
    private final RocCurveDataRepository rocCurveDataRepository;
    private final HParamDataRepository hParamDataRepository;
    private final ProfilerDataRepository profilerDataRepository;

    private final ExperimentRunRepository experimentRunRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String UPLOAD_DIR = "uploads/runs";

    @Override
    @Transactional
    public int ingest(Long runId, InputStream inputStream) throws Exception {
        // Read all bytes
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] tmp = new byte[8192];
        int n;
        while ((n = inputStream.read(tmp)) != -1) {
            buffer.write(tmp, 0, n);
        }
        byte[] data = buffer.toByteArray();

        int count = 0;
        int offset = 0;
        while (offset + 12 <= data.length) {
            ByteBuffer bb = ByteBuffer.wrap(data, offset, 12);
            long crcHeader = bb.getInt() & 0xFFFFFFFFL;
            int length = bb.getInt();
            if (offset + 12 + length > data.length) break;

            byte[] payload = Arrays.copyOfRange(data, offset + 8, offset + 8 + length);
            long crcFooter = ByteBuffer.wrap(data, offset + 8 + length, 4).getInt() & 0xFFFFFFFFL;

            // Verify CRC
            CRC32 crc = new CRC32();
            crc.update(payload);
            long computed = crc.getValue();
            if (computed != crcHeader || computed != crcFooter) {
                log.warn("CRC mismatch at offset {}, skipping record", offset);
                offset += 12 + length;
                continue;
            }

            String json = new String(payload, java.nio.charset.StandardCharsets.UTF_8);
            @SuppressWarnings("unchecked")
            Map<String, Object> record = objectMapper.readValue(json, Map.class);
            String type = (String) record.get("type");

            if (type != null) {
                persistRecord(runId, type, record);
                count++;
            }

            offset += 12 + length;
        }

        // Update run status
        experimentRunRepository.findById(runId).ifPresent(run -> {
            run.setStatus("ingested");
            run.setUpdatedAt(java.time.LocalDateTime.now());
            experimentRunRepository.save(run);
        });

        return count;
    }

    private void persistRecord(Long runId, String type, Map<String, Object> record) {
        String tag = (String) record.getOrDefault("tag", "unknown");
        long step = record.get("step") instanceof Number ? ((Number) record.get("step")).longValue() : 0L;
        double wallTime = record.get("wall_time") instanceof Number ? ((Number) record.get("wall_time")).doubleValue() : 0.0;

        try {
            switch (type) {
                case "scalar":
                    persistScalar(runId, tag, step, wallTime, record);
                    break;
                case "image":
                    persistImage(runId, tag, step, wallTime, record);
                    break;
                case "audio":
                    persistAudio(runId, tag, step, wallTime, record);
                    break;
                case "text":
                    persistText(runId, tag, step, wallTime, record);
                    break;
                case "histogram":
                    persistHistogram(runId, tag, step, wallTime, record);
                    break;
                case "embedding":
                    persistEmbedding(runId, tag, step, wallTime, record);
                    break;
                case "pr_curve":
                    persistPRCurve(runId, tag, step, wallTime, record);
                    break;
                case "roc_curve":
                    persistRocCurve(runId, tag, step, wallTime, record);
                    break;
                case "hparam":
                    persistHParam(runId, tag, step, wallTime, record);
                    break;
                case "profiler_step":
                    persistProfiler(runId, tag, step, wallTime, record);
                    break;
                case "figure":
                    persistImage(runId, tag, step, wallTime, record);  // figures stored same as images
                    break;
                case "image_matrix":
                    // image_matrix stored as individual images per column
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> columns = (List<Map<String, Object>>) record.get("columns");
                    if (columns != null) {
                        for (Map<String, Object> col : columns) {
                            String colName = (String) col.get("name");
                            @SuppressWarnings("unchecked")
                            List<String> images = (List<String>) col.get("images");
                            if (images != null) {
                                for (int i = 0; i < images.size(); i++) {
                                    Map<String, Object> imgRecord = new HashMap<>();
                                    imgRecord.put("encoded_image", images.get(i));
                                    imgRecord.put("height", 0);
                                    imgRecord.put("width", 0);
                                    persistImage(runId, tag + "/" + colName + "/" + i, step, wallTime, imgRecord);
                                }
                            }
                        }
                    }
                    break;
            }
        } catch (Exception e) {
            log.error("Failed to persist record type={} tag={} step={}: {}", type, tag, step, e.getMessage());
        }
    }

    private void persistScalar(Long runId, String tag, long step, double wallTime, Map<String, Object> record) {
        ScalarLog log = ScalarLog.builder()
                .runId(runId).tag(tag).step(step).wallTime(wallTime)
                .value(record.get("value") instanceof Number ? ((Number) record.get("value")).doubleValue() : 0.0)
                .build();
        scalarLogRepository.save(log);
    }

    private void persistImage(Long runId, String tag, long step, double wallTime, Map<String, Object> record) {
        String encoded = (String) record.get("encoded_image");
        if (encoded == null && record.get("encoded_figure") != null) {
            encoded = (String) record.get("encoded_figure");
        }
        if (encoded == null) return;

        try {
            byte[] imageBytes = Base64.getDecoder().decode(encoded);
            String dir = UPLOAD_DIR + "/" + runId + "/images";
            Files.createDirectories(Path.of(dir));
            String safeTag = tag.replace("/", "_").replace("\\", "_");
            String filename = safeTag + "_step" + step + ".png";
            Files.write(Path.of(dir, filename), imageBytes);

            int height = record.get("height") instanceof Number ? ((Number) record.get("height")).intValue() : 0;
            int width = record.get("width") instanceof Number ? ((Number) record.get("width")).intValue() : 0;

            ImageLog log = ImageLog.builder()
                    .runId(runId).tag(tag).step(step).wallTime(wallTime)
                    .filename(filename).height(height).width(width)
                    .build();
            imageLogRepository.save(log);
        } catch (IOException e) {
            log.error("Failed to save image: {}", e.getMessage());
        }
    }

    private void persistAudio(Long runId, String tag, long step, double wallTime, Map<String, Object> record) {
        String encoded = (String) record.get("encoded_audio");
        if (encoded == null) return;

        try {
            byte[] audioBytes = Base64.getDecoder().decode(encoded);
            String dir = UPLOAD_DIR + "/" + runId + "/audio";
            Files.createDirectories(Path.of(dir));
            String safeTag = tag.replace("/", "_").replace("\\", "_");
            String filename = safeTag + "_step" + step + ".wav";
            Files.write(Path.of(dir, filename), audioBytes);

            AudioLog log = AudioLog.builder()
                    .runId(runId).tag(tag).step(step).wallTime(wallTime)
                    .filename(filename)
                    .sampleRate(record.get("sample_rate") instanceof Number ? ((Number) record.get("sample_rate")).floatValue() : 16000f)
                    .numChannels(record.get("num_channels") instanceof Number ? ((Number) record.get("num_channels")).intValue() : 1)
                    .numFrames(record.get("num_frames") instanceof Number ? ((Number) record.get("num_frames")).intValue() : 0)
                    .build();
            audioLogRepository.save(log);
        } catch (IOException e) {
            log.error("Failed to save audio: {}", e.getMessage());
        }
    }

    private void persistText(Long runId, String tag, long step, double wallTime, Map<String, Object> record) {
        TextLog log = TextLog.builder()
                .runId(runId).tag(tag).step(step).wallTime(wallTime)
                .text((String) record.get("text"))
                .build();
        textLogRepository.save(log);
    }

    private void persistHistogram(Long runId, String tag, long step, double wallTime, Map<String, Object> record) {
        try {
            HistogramData data = HistogramData.builder()
                    .runId(runId).tag(tag).step(step).wallTime(wallTime)
                    .limitsJson(objectMapper.writeValueAsString(record.get("limits")))
                    .countsJson(objectMapper.writeValueAsString(record.get("counts")))
                    .sumVal(record.get("sum") instanceof Number ? ((Number) record.get("sum")).doubleValue() : 0.0)
                    .sumSquares(record.get("sum_squares") instanceof Number ? ((Number) record.get("sum_squares")).doubleValue() : 0.0)
                    .totalCount(record.get("total_count") instanceof Number ? ((Number) record.get("total_count")).longValue() : 0L)
                    .build();
            histogramDataRepository.save(data);
        } catch (Exception e) {
            log.error("Failed to serialize histogram: {}", e.getMessage());
        }
    }

    private void persistEmbedding(Long runId, String tag, long step, double wallTime, Map<String, Object> record) {
        try {
            EmbeddingData data = EmbeddingData.builder()
                    .runId(runId).tag(tag).step(step).wallTime(wallTime)
                    .valuesJson(objectMapper.writeValueAsString(record.get("values")))
                    .label((String) record.get("label"))
                    .classId(record.get("class_id") instanceof Number ? ((Number) record.get("class_id")).intValue() : null)
                    .sampleId(record.get("sample_id") instanceof Number ? ((Number) record.get("sample_id")).longValue() : null)
                    .build();
            embeddingDataRepository.save(data);
        } catch (Exception e) {
            log.error("Failed to serialize embedding: {}", e.getMessage());
        }
    }

    private void persistPRCurve(Long runId, String tag, long step, double wallTime, Map<String, Object> record) {
        try {
            PRCurveData data = PRCurveData.builder()
                    .runId(runId).tag(tag).step(step).wallTime(wallTime)
                    .precisionJson(objectMapper.writeValueAsString(record.get("precision")))
                    .recallJson(objectMapper.writeValueAsString(record.get("recall")))
                    .thresholdsJson(objectMapper.writeValueAsString(record.get("thresholds")))
                    .numThresholds(record.get("num_thresholds") instanceof Number ? ((Number) record.get("num_thresholds")).intValue() : 0)
                    .build();
            prCurveDataRepository.save(data);
        } catch (Exception e) {
            log.error("Failed to serialize PR curve: {}", e.getMessage());
        }
    }

    private void persistRocCurve(Long runId, String tag, long step, double wallTime, Map<String, Object> record) {
        try {
            RocCurveData data = RocCurveData.builder()
                    .runId(runId).tag(tag).step(step).wallTime(wallTime)
                    .tprJson(objectMapper.writeValueAsString(record.get("tpr")))
                    .fprJson(objectMapper.writeValueAsString(record.get("fpr")))
                    .thresholdsJson(objectMapper.writeValueAsString(record.get("thresholds")))
                    .build();
            rocCurveDataRepository.save(data);
        } catch (Exception e) {
            log.error("Failed to serialize ROC curve: {}", e.getMessage());
        }
    }

    private void persistHParam(Long runId, String tag, long step, double wallTime, Map<String, Object> record) {
        try {
            HParamData data = HParamData.builder()
                    .runId(runId).tag(tag).step(step).wallTime(wallTime)
                    .metricValuesJson(objectMapper.writeValueAsString(record.get("metric_values")))
                    .stringValuesJson(objectMapper.writeValueAsString(record.get("string_values")))
                    .build();
            hParamDataRepository.save(data);
        } catch (Exception e) {
            log.error("Failed to serialize hparam: {}", e.getMessage());
        }
    }

    private void persistProfiler(Long runId, String tag, long step, double wallTime, Map<String, Object> record) {
        try {
            ProfilerData data = ProfilerData.builder()
                    .runId(runId).tag(tag).step(step).wallTime(wallTime)
                    .profilerJson(objectMapper.writeValueAsString(record.get("profiler_data")))
                    .build();
            profilerDataRepository.save(data);
        } catch (Exception e) {
            log.error("Failed to serialize profiler: {}", e.getMessage());
        }
    }

    @Override
    public List<String> getTags(Long runId) {
        Set<String> tags = new LinkedHashSet<>();
        tags.addAll(scalarLogRepository.findDistinctTagsByRunId(runId));
        tags.addAll(imageLogRepository.findDistinctTagsByRunId(runId));
        tags.addAll(audioLogRepository.findDistinctTagsByRunId(runId));
        tags.addAll(textLogRepository.findDistinctTagsByRunId(runId));
        tags.addAll(histogramDataRepository.findDistinctTagsByRunId(runId));
        tags.addAll(embeddingDataRepository.findDistinctTagsByRunId(runId));
        tags.addAll(prCurveDataRepository.findDistinctTagsByRunId(runId));
        tags.addAll(rocCurveDataRepository.findDistinctTagsByRunId(runId));
        return new ArrayList<>(tags);
    }

    @Override
    @Transactional
    public void deleteRunData(Long runId) {
        scalarLogRepository.deleteByRunId(runId);
        imageLogRepository.deleteByRunId(runId);
        audioLogRepository.deleteByRunId(runId);
        textLogRepository.deleteByRunId(runId);
        histogramDataRepository.deleteByRunId(runId);
        embeddingDataRepository.deleteByRunId(runId);
        prCurveDataRepository.deleteByRunId(runId);
        rocCurveDataRepository.deleteByRunId(runId);
        hParamDataRepository.deleteByRunId(runId);
        profilerDataRepository.deleteByRunId(runId);
    }
}
