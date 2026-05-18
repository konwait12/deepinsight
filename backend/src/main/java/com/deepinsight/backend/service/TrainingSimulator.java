package com.deepinsight.backend.service;

import com.deepinsight.backend.entity.TrainingJob;
import com.deepinsight.backend.entity.TrainingStep;
import com.deepinsight.backend.repository.TrainingJobRepository;
import com.deepinsight.backend.repository.TrainingStepRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrainingSimulator {

    private final TrainingJobRepository repo;
    private final TrainingStepRepository stepRepo;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final Map<Long, ScheduledFuture<?>> runningJobs = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        scheduler.scheduleAtFixedRate(this::resumeJobs, 5, 10, TimeUnit.SECONDS);
    }

    private void resumeJobs() {
        for (TrainingJob job : repo.findAll()) {
            if ("running".equals(job.getStatus()) && !runningJobs.containsKey(job.getId())) {
                start(job.getId());
            }
        }
    }

    public void start(Long jobId) {
        if (runningJobs.containsKey(jobId)) return;
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> tick(jobId), 1, 2, TimeUnit.SECONDS);
        runningJobs.put(jobId, future);
    }

    public void stop(Long jobId) {
        ScheduledFuture<?> f = runningJobs.remove(jobId);
        if (f != null) f.cancel(false);
    }

    private void tick(Long jobId) {
        try {
            TrainingJob job = repo.findById(jobId).orElse(null);
            if (job == null || !"running".equals(job.getStatus())) { stop(jobId); return; }
            int epoch = job.getCurrentEpoch() != null ? job.getCurrentEpoch() + 1 : 1;
            int total = job.getEpochs() != null && job.getEpochs() > 0 ? job.getEpochs() : 100;
            double progress = (double) epoch / total;
            Random rng = new Random(jobId * 31 + epoch);
            double loss = 3.0 * Math.exp(-4.0 * progress) + 0.02 * rng.nextGaussian() + 0.03;
            double acc = 0.96 * (1 - Math.exp(-4.5 * progress)) + 0.01 * rng.nextGaussian();
            if (loss < 0.01) loss = 0.01 + rng.nextDouble() * 0.02;
            if (acc > 0.995) acc = 0.98 + rng.nextDouble() * 0.015;

            job.setCurrentEpoch(epoch);
            job.setCurrentLoss(Math.round(loss * 10000.0) / 10000.0);
            job.setCurrentAccuracy(Math.round(acc * 10000.0) / 10000.0);
            job.setTotalTimeSec(job.getTotalTimeSec() == null ? 2 : job.getTotalTimeSec() + 2);
            repo.save(job);

            // Log training step
            double valLoss = Math.round((loss + 0.015 + rng.nextDouble() * 0.06) * 10000.0) / 10000.0;
            double valAcc = Math.round((acc - 0.005 - rng.nextDouble() * 0.03) * 10000.0) / 10000.0;
            stepRepo.save(TrainingStep.builder().jobId(jobId).epoch(epoch)
                .trainLoss(job.getCurrentLoss()).valLoss(valLoss)
                .trainAccuracy(job.getCurrentAccuracy()).valAccuracy(valAcc)
                .learningRate(job.getLearningRate()).build());

            if (epoch >= total) {
                job.setStatus("completed");
                job.setCurrentEpoch(total);
                repo.save(job);
                stop(jobId);
            }
        } catch (Exception e) {
            log.error("TrainingSimulator tick failed for job {}: {}", jobId, e.getMessage());
        }
    }
}
