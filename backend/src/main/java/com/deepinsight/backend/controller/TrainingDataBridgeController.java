package com.deepinsight.backend.controller;

import com.deepinsight.backend.common.Result;
import com.deepinsight.backend.entity.ScalarLog;
import com.deepinsight.backend.entity.TrainingJob;
import com.deepinsight.backend.entity.TrainingStep;
import com.deepinsight.backend.entity.User;
import com.deepinsight.backend.repository.TrainingJobRepository;
import com.deepinsight.backend.repository.TrainingStepRepository;
import com.deepinsight.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Bridges simulated training data into the visual-data API format,
 * so viz pages can display training job data alongside uploaded runs.
 */
@RestController
@RequestMapping("/api/v1/visual-data/jobs")
@RequiredArgsConstructor
public class TrainingDataBridgeController {

    private final TrainingJobRepository jobRepo;
    private final TrainingStepRepository stepRepo;
    private final UserRepository userRepo;

    private Long getUserId(Principal p) {
        if (p == null) return null;
        return userRepo.findByUsername(p.getName()).map(User::getId).orElse(null);
    }

    /** List training jobs — user-filtered, returns in same shape as experiment runs */
    @GetMapping
    public Result<List<Map<String, Object>>> listJobs(Principal principal) {
        Long uid = getUserId(principal);
        List<TrainingJob> jobs = jobRepo.findAll();
        if (uid != null) {
            final Long id = uid;
            jobs = jobs.stream().filter(j -> id.equals(j.getCreatedBy())).collect(Collectors.toList());
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (TrainingJob j : jobs) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", j.getId());
            m.put("name", j.getName());
            m.put("status", j.getStatus());
            m.put("source", "training");
            m.put("modelArchitecture", j.getModelArchitecture());
            m.put("currentLoss", j.getCurrentLoss());
            m.put("currentAccuracy", j.getCurrentAccuracy());
            result.add(m);
        }
        return Result.success(result);
    }

    /** List available scalar tags for a training job */
    @GetMapping("/{jobId}/tags")
    public Result<List<String>> getTags(Principal p, @PathVariable Long jobId) {
        Long uid = getUserId(p);
        if (uid == null) return Result.error(401, "请先登录");
        TrainingJob job = jobRepo.findById(jobId).orElse(null);
        if (job == null) return Result.error(404, "Job not found");
        if (!uid.equals(job.getCreatedBy())) return Result.error(403, "无权访问此数据");
        return Result.success(List.of("train/loss", "train/accuracy", "val/loss", "val/accuracy", "train/learning_rate"));
    }

    /** Get training steps as scalar records */
    @GetMapping("/{jobId}/scalars")
    public Result<List<ScalarLog>> getScalars(Principal p, @PathVariable Long jobId,
                                               @RequestParam String tag) {
        Long uid = getUserId(p);
        if (uid == null) return Result.error(401, "请先登录");
        TrainingJob job = jobRepo.findById(jobId).orElse(null);
        if (job == null) return Result.error(404, "Job not found");
        if (!uid.equals(job.getCreatedBy())) return Result.error(403, "无权访问此数据");
        List<TrainingStep> steps = stepRepo.findByJobIdOrderByEpochAsc(jobId);
        List<ScalarLog> result = new ArrayList<>();

        for (int i = 0; i < steps.size(); i++) {
            TrainingStep s = steps.get(i);
            Double value = switch (tag) {
                case "train/loss" -> s.getTrainLoss();
                case "train/accuracy" -> s.getTrainAccuracy();
                case "val/loss" -> s.getValLoss();
                case "val/accuracy" -> s.getValAccuracy();
                case "train/learning_rate" -> s.getLearningRate();
                default -> null;
            };
            if (value != null) {
                result.add(ScalarLog.builder()
                        .runId(jobId).tag(tag).step((long) s.getEpoch())
                        .value(value).wallTime((double) (s.getEpoch() * 2))
                        .build());
            }
        }
        return Result.success(result);
    }
}
