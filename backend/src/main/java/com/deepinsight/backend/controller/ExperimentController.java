package com.deepinsight.backend.controller;

import com.deepinsight.backend.common.Result;
import com.deepinsight.backend.entity.*;
import com.deepinsight.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/experiments")
@RequiredArgsConstructor
public class ExperimentController {

    private final TrainingJobRepository jobRepo;
    private final TrainingStepRepository stepRepo;
    private final ExperimentRunRepository runRepo;
    private final ScalarLogRepository scalarRepo;
    private final ImageLogRepository imageRepo;
    private final AudioLogRepository audioRepo;
    private final TextLogRepository textRepo;
    private final HistogramDataRepository histRepo;
    private final EmbeddingDataRepository embRepo;
    private final PRCurveDataRepository prRepo;
    private final RocCurveDataRepository rocRepo;
    private final HParamDataRepository hparamRepo;
    private final ProfilerDataRepository profRepo;
    private final UserRepository userRepo;

    private Long getUserId(Principal p) {
        if (p == null) return null;
        return userRepo.findByUsername(p.getName()).map(User::getId).orElse(null);
    }

    private boolean isOwner(Principal p, Long id, String type) {
        Long uid = getUserId(p);
        if (uid == null) return false;
        if ("training".equals(type)) {
            TrainingJob job = jobRepo.findById(id).orElse(null);
            return job != null && uid.equals(job.getCreatedBy());
        }
        ExperimentRun run = runRepo.findById(id).orElse(null);
        return run != null && uid.equals(run.getUserId());
    }

    @GetMapping
    public Result<List<Map<String, Object>>> list(Principal principal) {
        Long uid = getUserId(principal);
        List<Map<String, Object>> result = new ArrayList<>();
        List<TrainingJob> jobs = jobRepo.findAll();
        if (uid != null) {
            final Long id = uid;
            jobs = jobs.stream().filter(j -> id.equals(j.getCreatedBy())).collect(Collectors.toList());
        }
        for (TrainingJob j : jobs) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", j.getId());
            m.put("name", j.getName());
            m.put("status", j.getStatus());
            m.put("type", "training");
            m.put("architecture", j.getModelArchitecture());
            m.put("modelArchitecture", j.getModelArchitecture());
            m.put("optimizer", j.getOptimizer());
            m.put("learningRate", j.getLearningRate());
            m.put("batchSize", j.getBatchSize());
            m.put("epochs", j.getEpochs());
            m.put("currentEpoch", j.getCurrentEpoch());
            m.put("currentLoss", j.getCurrentLoss());
            m.put("currentAccuracy", j.getCurrentAccuracy());
            result.add(m);
        }
        List<ExperimentRun> runs = runRepo.findAll();
        if (uid != null) {
            final Long id = uid;
            runs = runs.stream().filter(r -> id.equals(r.getUserId())).collect(Collectors.toList());
        }
        for (ExperimentRun r : runs) {
            result.add(Map.of("id", r.getId(), "name", r.getName(), "status", r.getStatus(), "type", "upload"));
        }
        return Result.success(result);
    }

    @GetMapping("/{id}/tags")
    public Result<List<String>> tags(Principal p, @PathVariable Long id, @RequestParam String type) {
        if (!isOwner(p, id, type)) return Result.error(403, "无权访问此数据");
        if ("training".equals(type)) {
            return Result.success(List.of("train/loss", "train/accuracy", "val/loss", "val/accuracy", "train/learning_rate"));
        }
        Set<String> tags = new LinkedHashSet<>();
        tags.addAll(scalarRepo.findDistinctTagsByRunId(id));
        tags.addAll(imageRepo.findDistinctTagsByRunId(id));
        tags.addAll(audioRepo.findDistinctTagsByRunId(id));
        tags.addAll(textRepo.findDistinctTagsByRunId(id));
        tags.addAll(histRepo.findDistinctTagsByRunId(id));
        tags.addAll(embRepo.findDistinctTagsByRunId(id));
        tags.addAll(prRepo.findDistinctTagsByRunId(id));
        tags.addAll(rocRepo.findDistinctTagsByRunId(id));
        return Result.success(new ArrayList<>(tags));
    }

    @GetMapping("/{id}/scalars")
    public Result<List<ScalarLog>> scalars(Principal p, @PathVariable Long id, @RequestParam String type,
                                            @RequestParam String tag) {
        if (!isOwner(p, id, type)) return Result.error(403, "无权访问此数据");
        if ("training".equals(type)) {
            List<TrainingStep> steps = stepRepo.findByJobIdOrderByEpochAsc(id);
            List<ScalarLog> result = new ArrayList<>();
            for (TrainingStep s : steps) {
                Double value = switch (tag) {
                    case "train/loss" -> s.getTrainLoss();
                    case "train/accuracy" -> s.getTrainAccuracy();
                    case "val/loss" -> s.getValLoss();
                    case "val/accuracy" -> s.getValAccuracy();
                    case "train/learning_rate" -> s.getLearningRate();
                    default -> null;
                };
                if (value != null) {
                    result.add(ScalarLog.builder().runId(id).tag(tag).step((long) s.getEpoch()).value(value).wallTime((double) (s.getEpoch() * 2)).build());
                }
            }
            return Result.success(result);
        }
        return Result.success(scalarRepo.findByRunIdAndTagOrderByStepAsc(id, tag));
    }

    @GetMapping("/{id}/images")
    public Result<?> images(Principal p, @PathVariable Long id, @RequestParam String type, @RequestParam String tag) {
        if (!isOwner(p, id, type)) return Result.error(403, "无权访问此数据");
        if ("training".equals(type)) return Result.success(List.of());
        return Result.success(imageRepo.findByRunIdAndTagOrderByStepAsc(id, tag));
    }

    @GetMapping("/{id}/audio")
    public Result<?> audio(Principal p, @PathVariable Long id, @RequestParam String type, @RequestParam String tag) {
        if (!isOwner(p, id, type)) return Result.error(403, "无权访问此数据");
        if ("training".equals(type)) return Result.success(List.of());
        return Result.success(audioRepo.findByRunIdAndTagOrderByStepAsc(id, tag));
    }

    @GetMapping("/{id}/text")
    public Result<?> text(Principal p, @PathVariable Long id, @RequestParam String type, @RequestParam String tag) {
        if (!isOwner(p, id, type)) return Result.error(403, "无权访问此数据");
        if ("training".equals(type)) return Result.success(List.of());
        return Result.success(textRepo.findByRunIdAndTagOrderByStepAsc(id, tag));
    }

    @GetMapping("/{id}/histograms")
    public Result<?> histograms(Principal p, @PathVariable Long id, @RequestParam String type, @RequestParam String tag) {
        if (!isOwner(p, id, type)) return Result.error(403, "无权访问此数据");
        if ("training".equals(type)) return Result.success(List.of());
        return Result.success(histRepo.findByRunIdAndTagOrderByStepAsc(id, tag));
    }

    @GetMapping("/{id}/embeddings")
    public Result<?> embeddings(Principal p, @PathVariable Long id, @RequestParam String type, @RequestParam String tag) {
        if (!isOwner(p, id, type)) return Result.error(403, "无权访问此数据");
        if ("training".equals(type)) return Result.success(List.of());
        return Result.success(embRepo.findByRunIdAndTagOrderByStepAsc(id, tag));
    }

    @GetMapping("/{id}/pr-curves")
    public Result<?> prCurves(Principal p, @PathVariable Long id, @RequestParam String type, @RequestParam String tag) {
        if (!isOwner(p, id, type)) return Result.error(403, "无权访问此数据");
        if ("training".equals(type)) return Result.success(List.of());
        return Result.success(prRepo.findByRunIdAndTagOrderByStepAsc(id, tag));
    }

    @GetMapping("/{id}/roc-curves")
    public Result<?> rocCurves(Principal p, @PathVariable Long id, @RequestParam String type, @RequestParam String tag) {
        if (!isOwner(p, id, type)) return Result.error(403, "无权访问此数据");
        if ("training".equals(type)) return Result.success(List.of());
        return Result.success(rocRepo.findByRunIdAndTagOrderByStepAsc(id, tag));
    }

    @GetMapping("/{id}/hparams")
    public Result<?> hparams(Principal p, @PathVariable Long id, @RequestParam String type) {
        if (!isOwner(p, id, type)) return Result.error(403, "无权访问此数据");
        if ("training".equals(type)) return Result.success(List.of());
        return Result.success(hparamRepo.findByRunIdOrderByStepAsc(id));
    }

    @GetMapping("/{id}/profiler")
    public Result<?> profiler(Principal p, @PathVariable Long id, @RequestParam String type, @RequestParam String tag) {
        if (!isOwner(p, id, type)) return Result.error(403, "无权访问此数据");
        if ("training".equals(type)) return Result.success(List.of());
        return Result.success(profRepo.findByRunIdAndTagOrderByStepAsc(id, tag));
    }
}
