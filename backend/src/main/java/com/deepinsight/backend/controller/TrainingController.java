package com.deepinsight.backend.controller;

import com.deepinsight.backend.common.Result;
import com.deepinsight.backend.entity.TrainingJob;
import com.deepinsight.backend.entity.User;
import com.deepinsight.backend.repository.TrainingJobRepository;
import com.deepinsight.backend.repository.UserRepository;
import com.deepinsight.backend.service.TrainingSimulator;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/training")
@RequiredArgsConstructor
public class TrainingController {

    private final TrainingJobRepository repository;
    private final UserRepository userRepository;
    private final TrainingSimulator simulator;

    private Long getUserId(Principal p) {
        if (p == null) return null;
        return userRepository.findByUsername(p.getName()).map(User::getId).orElse(null);
    }

    @GetMapping
    public Result<List<TrainingJob>> list(@RequestParam(required = false) String status, Principal principal) {
        Long uid = getUserId(principal);
        List<TrainingJob> jobs;
        if (uid != null && status != null) {
            jobs = repository.findByCreatedByAndStatusOrderByCreatedAtDesc(uid, status);
        } else if (uid != null) {
            jobs = repository.findByCreatedByOrderByCreatedAtDesc(uid);
        } else if (status != null) {
            jobs = repository.findByStatusOrderByCreatedAtDesc(status);
        } else {
            jobs = repository.findAll();
        }
        return Result.success(jobs);
    }

    @GetMapping("/{id}")
    public Result<TrainingJob> get(@PathVariable Long id, Principal principal) {
        TrainingJob job = repository.findById(id).orElse(null);
        if (job == null) return Result.error(404, "Not found");
        Long uid = getUserId(principal);
        if (uid != null && job.getCreatedBy() != null && !job.getCreatedBy().equals(uid))
            return Result.error(403, "无权查看");
        return Result.success(job);
    }

    @PostMapping
    public Result<TrainingJob> create(@RequestBody TrainingJob job, Principal principal) {
        job.setId(null); job.setStatus("queued");
        Long uid = getUserId(principal);
        if (uid != null) job.setCreatedBy(uid);
        return Result.success("训练任务已创建", repository.save(job));
    }

    @PutMapping("/{id}/start")
    public Result<TrainingJob> start(@PathVariable Long id, Principal principal) { return modify(id, principal, "running", true); }
    @PutMapping("/{id}/pause")
    public Result<TrainingJob> pause(@PathVariable Long id, Principal principal) { return modify(id, principal, "paused", false); }
    @PutMapping("/{id}/stop")
    public Result<TrainingJob> stop(@PathVariable Long id, Principal principal) { return modify(id, principal, "completed", false); }

    private Result<TrainingJob> modify(Long id, Principal principal, String status, boolean start) {
        TrainingJob job = repository.findById(id).orElseThrow();
        Long uid = getUserId(principal);
        if (uid != null && job.getCreatedBy() != null && !job.getCreatedBy().equals(uid))
            return Result.error(403, "无权操作");
        job.setStatus(status);
        job = repository.save(job);
        if (start) simulator.start(id); else simulator.stop(id);
        return Result.success("操作成功", job);
    }

    @PutMapping("/{id}")
    public Result<TrainingJob> update(@PathVariable Long id, @RequestBody TrainingJob job, Principal principal) {
        TrainingJob existing = repository.findById(id).orElseThrow();
        Long uid = getUserId(principal);
        if (uid != null && existing.getCreatedBy() != null && !existing.getCreatedBy().equals(uid))
            return Result.error(403, "无权操作");
        job.setId(id); job.setCreatedBy(existing.getCreatedBy());
        return Result.success("更新成功", repository.save(job));
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id, Principal principal) {
        TrainingJob job = repository.findById(id).orElseThrow();
        Long uid = getUserId(principal);
        if (uid != null && job.getCreatedBy() != null && !job.getCreatedBy().equals(uid))
            return Result.error(403, "无权操作");
        simulator.stop(id);
        repository.deleteById(id);
        return Result.success("已删除");
    }
}
