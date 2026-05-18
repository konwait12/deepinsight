package com.deepinsight.backend.controller;

import com.deepinsight.backend.common.Result;
import com.deepinsight.backend.entity.TrainingJob;
import com.deepinsight.backend.entity.TrainingStep;
import com.deepinsight.backend.repository.TrainingJobRepository;
import com.deepinsight.backend.repository.TrainingStepRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/analysis")
public class AnalysisController {

    @Autowired
    private TrainingJobRepository trainingJobRepository;
    @Autowired
    private TrainingStepRepository stepRepository;
    @Autowired
    private com.deepinsight.backend.repository.UserRepository userRepo;

    private Long getUserId(Principal p) {
        if (p == null) return null;
        return userRepo.findByUsername(p.getName()).map(com.deepinsight.backend.entity.User::getId).orElse(null);
    }

    private List<TrainingJob> getUserJobs(Principal principal) {
        Long uid = getUserId(principal);
        if (uid != null) {
            return trainingJobRepository.findByCreatedByOrderByCreatedAtDesc(uid);
        }
        return trainingJobRepository.findAll();
    }

    @GetMapping("/overview")
    public Result<Map<String, Object>> overview(Principal principal) {
        List<TrainingJob> jobs = getUserJobs(principal);
        long completed = jobs.stream().filter(j -> "completed".equals(j.getStatus())).count();
        long running = jobs.stream().filter(j -> "running".equals(j.getStatus())).count();
        double avgLoss = jobs.stream().filter(j -> j.getCurrentLoss() != null)
                .mapToDouble(TrainingJob::getCurrentLoss).average().orElse(0);
        double avgAcc = jobs.stream().filter(j -> j.getCurrentAccuracy() != null)
                .mapToDouble(TrainingJob::getCurrentAccuracy).average().orElse(0);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalJobs", jobs.size());
        result.put("completedJobs", completed);
        result.put("runningJobs", running);
        result.put("failedJobs", jobs.stream().filter(j -> "failed".equals(j.getStatus())).count());
        result.put("avgLoss", Math.round(avgLoss * 10000.0) / 10000.0);
        result.put("avgAccuracy", Math.round(avgAcc * 10000.0) / 10000.0);
        return Result.success(result);
    }

    @GetMapping("/training-curve/{jobId}")
    public Result<Map<String, Object>> trainingCurve(Principal p, @PathVariable Long jobId) {
        Long uid = getUserId(p);
        if (uid == null) return Result.error(401, "请先登录");
        Optional<TrainingJob> opt = trainingJobRepository.findById(jobId);
        if (opt.isEmpty()) return Result.error(404, "Job not found");
        TrainingJob job = opt.get();
        if (!uid.equals(job.getCreatedBy())) return Result.error(403, "无权访问此数据");

        // Load real training steps from DB
        List<TrainingStep> steps = stepRepository.findByJobIdOrderByEpochAsc(jobId);

        List<Double> loss = new ArrayList<>();
        List<Double> accuracy = new ArrayList<>();
        List<Double> valLoss = new ArrayList<>();
        List<Double> valAccuracy = new ArrayList<>();
        List<Double> lrHistory = new ArrayList<>();

        for (TrainingStep s : steps) {
            loss.add(s.getTrainLoss());
            accuracy.add(s.getTrainAccuracy());
            valLoss.add(s.getValLoss());
            valAccuracy.add(s.getValAccuracy());
            if (s.getLearningRate() != null) lrHistory.add(s.getLearningRate());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("jobName", job.getName());
        result.put("architecture", job.getModelArchitecture());
        result.put("loss", loss);
        result.put("accuracy", accuracy);
        result.put("valLoss", valLoss);
        result.put("valAccuracy", valAccuracy);
        result.put("learningRate", lrHistory);
        result.put("currentEpoch", job.getCurrentEpoch());
        result.put("totalEpochs", job.getEpochs() != null ? job.getEpochs() : 100);
        result.put("isRealData", !steps.isEmpty());
        return Result.success(result);
    }

    @GetMapping("/hyperparams")
    public Result<Map<String, Object>> hyperparams(Principal principal) {
        List<TrainingJob> jobs = getUserJobs(principal);
        List<String> dims = List.of("learningRate", "batchSize", "epochs", "accuracy", "loss");
        List<Map<String, Object>> rows = new ArrayList<>();
        for (TrainingJob j : jobs) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", j.getName());
            row.put("learningRate", j.getLearningRate());
            row.put("batchSize", j.getBatchSize());
            row.put("epochs", j.getCurrentEpoch());
            row.put("accuracy", j.getCurrentAccuracy() != null ? Math.round(j.getCurrentAccuracy() * 10000.0) / 10000.0 : null);
            row.put("loss", j.getCurrentLoss());
            row.put("status", j.getStatus());
            row.put("optimizer", j.getOptimizer());
            rows.add(row);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dimensions", dims);
        result.put("data", rows);
        return Result.success(result);
    }

    @GetMapping("/model-comparison")
    public Result<List<Map<String, Object>>> modelComparison(Principal principal) {
        List<TrainingJob> jobs = getUserJobs(principal);
        List<Map<String, Object>> list = new ArrayList<>();
        for (TrainingJob j : jobs) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", j.getId());
            m.put("name", j.getName());
            m.put("architecture", j.getModelArchitecture());
            m.put("status", j.getStatus());
            m.put("accuracy", j.getCurrentAccuracy());
            m.put("loss", j.getCurrentLoss());
            m.put("epochs", j.getCurrentEpoch() != null ? j.getCurrentEpoch() : 0);
            m.put("totalEpochs", j.getEpochs() != null ? j.getEpochs() : 100);
            m.put("optimizer", j.getOptimizer());
            m.put("learningRate", j.getLearningRate());
            m.put("batchSize", j.getBatchSize());
            list.add(m);
        }
        return Result.success(list);
    }
}
