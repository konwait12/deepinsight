package com.deepinsight.backend.controller;

import com.deepinsight.backend.common.Result;
import com.deepinsight.backend.entity.ExperimentRun;
import com.deepinsight.backend.entity.User;
import com.deepinsight.backend.repository.ExperimentRunRepository;
import com.deepinsight.backend.repository.UserRepository;
import com.deepinsight.backend.service.LogIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/logs")
@RequiredArgsConstructor
public class LogIngestionController {

    private final ExperimentRunRepository runRepository;
    private final UserRepository userRepository;
    private final LogIngestionService ingestionService;

    private Long getUserId(Principal p) {
        if (p == null) return null;
        return userRepository.findByUsername(p.getName()).map(User::getId).orElse(null);
    }

    @PostMapping("/runs")
    public Result<ExperimentRun> createRun(@RequestBody Map<String, String> body, Principal principal) {
        Long uid = getUserId(principal);
        ExperimentRun run = ExperimentRun.builder()
                .name(body.getOrDefault("name", "Untitled"))
                .status("created")
                .userId(uid)
                .build();
        return Result.success("实验运行已创建", runRepository.save(run));
    }

    @GetMapping("/runs")
    public Result<List<ExperimentRun>> listRuns(Principal principal) {
        Long uid = getUserId(principal);
        List<ExperimentRun> runs;
        if (uid != null) {
            runs = runRepository.findByUserIdOrderByCreatedAtDesc(uid);
        } else {
            runs = runRepository.findAll();
        }
        return Result.success(runs);
    }

    @GetMapping("/runs/{runId}")
    public Result<ExperimentRun> getRun(@PathVariable Long runId, Principal principal) {
        ExperimentRun run = runRepository.findById(runId).orElse(null);
        if (run == null) return Result.error(404, "实验运行不存在");
        Long uid = getUserId(principal);
        if (uid != null && run.getUserId() != null && !run.getUserId().equals(uid))
            return Result.error(403, "无权访问此数据");
        return Result.success(run);
    }

    @PostMapping("/runs/{runId}/upload")
    public Result<Map<String, Object>> upload(@PathVariable Long runId,
                                               @RequestParam("file") MultipartFile file,
                                               Principal principal) {
        ExperimentRun run = runRepository.findById(runId).orElse(null);
        if (run == null) return Result.error(404, "实验运行不存在");
        Long uid = getUserId(principal);
        if (uid != null && run.getUserId() != null && !run.getUserId().equals(uid))
            return Result.error(403, "无权操作");

        try {
            int count = ingestionService.ingest(runId, file.getInputStream());
            run.setStatus("ingested");
            runRepository.save(run);
            return Result.success("数据上传成功",
                    Map.of("records", count, "fileName", file.getOriginalFilename()));
        } catch (Exception e) {
            return Result.error(500, "数据解析失败: " + e.getMessage());
        }
    }

    @GetMapping("/runs/{runId}/tags")
    public Result<List<String>> getTags(@PathVariable Long runId, Principal principal) {
        if (!runRepository.existsById(runId)) return Result.error(404, "实验运行不存在");
        ExperimentRun run = runRepository.findById(runId).orElse(null);
        Long uid = getUserId(principal);
        if (uid != null && run != null && run.getUserId() != null && !run.getUserId().equals(uid))
            return Result.error(403, "无权访问此数据");
        return Result.success(ingestionService.getTags(runId));
    }

    @DeleteMapping("/runs/{runId}")
    public Result<String> deleteRun(@PathVariable Long runId, Principal principal) {
        ExperimentRun run = runRepository.findById(runId).orElse(null);
        if (run == null) return Result.error(404, "实验运行不存在");
        Long uid = getUserId(principal);
        if (uid != null && run.getUserId() != null && !run.getUserId().equals(uid))
            return Result.error(403, "无权操作");
        ingestionService.deleteRunData(runId);
        runRepository.deleteById(runId);
        return Result.success("已删除");
    }
}
