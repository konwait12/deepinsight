package com.deepinsight.backend.controller;

import com.deepinsight.backend.common.Result;
import com.deepinsight.backend.entity.*;
import com.deepinsight.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.*;

@RestController
@RequestMapping("/api/v1/visual-data")
@RequiredArgsConstructor
public class VisualDataController {

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
    private final UserRepository userRepository;

    private boolean isOwner(Principal p, Long runId) {
        if (p == null) return false;
        ExperimentRun run = experimentRunRepository.findById(runId).orElse(null);
        if (run == null || run.getUserId() == null) return false;
        var user = userRepository.findByUsername(p.getName()).orElse(null);
        return user != null && run.getUserId().equals(user.getId());
    }

    @GetMapping("/{runId}/scalars")
    public Result<List<ScalarLog>> getScalars(Principal p, @PathVariable Long runId,
                                               @RequestParam String tag,
                                               @RequestParam(required = false) Long startStep,
                                               @RequestParam(required = false) Long endStep) {
        if (!isOwner(p, runId)) return Result.error(403, "无权访问此数据");
        List<ScalarLog> logs = scalarLogRepository.findByRunIdAndTagOrderByStepAsc(runId, tag);
        if (startStep != null || endStep != null) {
            long start = startStep != null ? startStep : Long.MIN_VALUE;
            long end = endStep != null ? endStep : Long.MAX_VALUE;
            logs = logs.stream().filter(l -> l.getStep() >= start && l.getStep() <= end).toList();
        }
        return Result.success(logs);
    }

    @GetMapping("/{runId}/images")
    public Result<List<ImageLog>> getImages(Principal p, @PathVariable Long runId,
                                             @RequestParam String tag) {
        if (!isOwner(p, runId)) return Result.error(403, "无权访问此数据");
        return Result.success(imageLogRepository.findByRunIdAndTagOrderByStepAsc(runId, tag));
    }

    @GetMapping("/{runId}/images/{imageId}/file")
    public Result<Map<String, String>> getImageFile(Principal p, @PathVariable Long runId,
                                                     @PathVariable Long imageId) {
        if (!isOwner(p, runId)) return Result.error(403, "无权访问此数据");
        ImageLog img = imageLogRepository.findById(imageId).orElse(null);
        if (img == null) return Result.error(404, "Image not found");
        return Result.success(Map.of(
                "url", "/uploads/runs/" + runId + "/images/" + img.getFilename(),
                "filename", img.getFilename(),
                "height", String.valueOf(img.getHeight()),
                "width", String.valueOf(img.getWidth())
        ));
    }

    @GetMapping("/{runId}/audio")
    public Result<List<AudioLog>> getAudio(Principal p, @PathVariable Long runId,
                                            @RequestParam String tag) {
        if (!isOwner(p, runId)) return Result.error(403, "无权访问此数据");
        return Result.success(audioLogRepository.findByRunIdAndTagOrderByStepAsc(runId, tag));
    }

    @GetMapping("/{runId}/audio/{audioId}/file")
    public Result<Map<String, String>> getAudioFile(Principal p, @PathVariable Long runId,
                                                     @PathVariable Long audioId) {
        if (!isOwner(p, runId)) return Result.error(403, "无权访问此数据");
        AudioLog audio = audioLogRepository.findById(audioId).orElse(null);
        if (audio == null) return Result.error(404, "Audio not found");
        return Result.success(Map.of(
                "url", "/uploads/runs/" + runId + "/audio/" + audio.getFilename(),
                "filename", audio.getFilename(),
                "sampleRate", String.valueOf(audio.getSampleRate()),
                "numChannels", String.valueOf(audio.getNumChannels())
        ));
    }

    @GetMapping("/{runId}/text")
    public Result<List<TextLog>> getText(Principal p, @PathVariable Long runId,
                                          @RequestParam String tag) {
        if (!isOwner(p, runId)) return Result.error(403, "无权访问此数据");
        return Result.success(textLogRepository.findByRunIdAndTagOrderByStepAsc(runId, tag));
    }

    @GetMapping("/{runId}/histograms")
    public Result<List<Map<String, Object>>> getHistograms(Principal p, @PathVariable Long runId,
                                                            @RequestParam String tag) {
        if (!isOwner(p, runId)) return Result.error(403, "无权访问此数据");
        List<HistogramData> data = histogramDataRepository.findByRunIdAndTagOrderByStepAsc(runId, tag);
        List<Map<String, Object>> result = new ArrayList<>();
        for (HistogramData h : data) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", h.getId()); m.put("step", h.getStep()); m.put("wallTime", h.getWallTime());
            m.put("limitsJson", h.getLimitsJson()); m.put("countsJson", h.getCountsJson());
            m.put("sumVal", h.getSumVal()); m.put("sumSquares", h.getSumSquares());
            m.put("totalCount", h.getTotalCount());
            result.add(m);
        }
        return Result.success(result);
    }

    @GetMapping("/{runId}/embeddings")
    public Result<List<Map<String, Object>>> getEmbeddings(Principal p, @PathVariable Long runId,
                                                            @RequestParam String tag) {
        if (!isOwner(p, runId)) return Result.error(403, "无权访问此数据");
        List<EmbeddingData> data = embeddingDataRepository.findByRunIdAndTagOrderByStepAsc(runId, tag);
        List<Map<String, Object>> result = new ArrayList<>();
        for (EmbeddingData e : data) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", e.getId()); m.put("step", e.getStep()); m.put("valuesJson", e.getValuesJson());
            m.put("label", e.getLabel()); m.put("classId", e.getClassId()); m.put("sampleId", e.getSampleId());
            result.add(m);
        }
        return Result.success(result);
    }

    @GetMapping("/{runId}/pr-curves")
    public Result<List<Map<String, Object>>> getPRCurves(Principal p, @PathVariable Long runId,
                                                          @RequestParam String tag) {
        if (!isOwner(p, runId)) return Result.error(403, "无权访问此数据");
        List<PRCurveData> data = prCurveDataRepository.findByRunIdAndTagOrderByStepAsc(runId, tag);
        List<Map<String, Object>> result = new ArrayList<>();
        for (PRCurveData pr : data) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", pr.getId()); m.put("step", pr.getStep());
            m.put("precisionJson", pr.getPrecisionJson()); m.put("recallJson", pr.getRecallJson());
            m.put("thresholdsJson", pr.getThresholdsJson()); m.put("numThresholds", pr.getNumThresholds());
            result.add(m);
        }
        return Result.success(result);
    }

    @GetMapping("/{runId}/roc-curves")
    public Result<List<Map<String, Object>>> getRocCurves(Principal p, @PathVariable Long runId,
                                                           @RequestParam String tag) {
        if (!isOwner(p, runId)) return Result.error(403, "无权访问此数据");
        List<RocCurveData> data = rocCurveDataRepository.findByRunIdAndTagOrderByStepAsc(runId, tag);
        List<Map<String, Object>> result = new ArrayList<>();
        for (RocCurveData r : data) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId()); m.put("step", r.getStep());
            m.put("tprJson", r.getTprJson()); m.put("fprJson", r.getFprJson());
            m.put("thresholdsJson", r.getThresholdsJson());
            result.add(m);
        }
        return Result.success(result);
    }

    @GetMapping("/{runId}/hparams")
    public Result<List<Map<String, Object>>> getHParams(Principal p, @PathVariable Long runId) {
        if (!isOwner(p, runId)) return Result.error(403, "无权访问此数据");
        List<HParamData> data = hParamDataRepository.findByRunIdOrderByStepAsc(runId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (HParamData h : data) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", h.getId()); m.put("step", h.getStep());
            m.put("metricValuesJson", h.getMetricValuesJson()); m.put("stringValuesJson", h.getStringValuesJson());
            result.add(m);
        }
        return Result.success(result);
    }

    @GetMapping("/{runId}/profiler")
    public Result<List<Map<String, Object>>> getProfiler(Principal p, @PathVariable Long runId,
                                                          @RequestParam String tag) {
        if (!isOwner(p, runId)) return Result.error(403, "无权访问此数据");
        List<ProfilerData> data = profilerDataRepository.findByRunIdAndTagOrderByStepAsc(runId, tag);
        List<Map<String, Object>> result = new ArrayList<>();
        for (ProfilerData pr : data) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", pr.getId()); m.put("step", pr.getStep());
            m.put("profilerJson", pr.getProfilerJson());
            result.add(m);
        }
        return Result.success(result);
    }
}
