package com.deepinsight.backend.controller;

import com.deepinsight.backend.common.Result;
import com.deepinsight.backend.service.VisualAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/visual-analysis")
@RequiredArgsConstructor
public class VisualAnalysisController {

    private final VisualAnalysisService visualAnalysisService;

    @GetMapping("/modules")
    public Result<List<Map<String, Object>>> modules() {
        return Result.success(visualAnalysisService.moduleCatalog());
    }

    @GetMapping("/batches")
    public Result<List<Map<String, Object>>> batches(Principal principal, @RequestParam(defaultValue = "6") int limit) {
        return Result.success(visualAnalysisService.recentBatches(principal, limit));
    }

    @GetMapping("/batches/{id}")
    public Result<Map<String, Object>> batch(Principal principal, @PathVariable Long id) {
        return Result.success(visualAnalysisService.getBatch(principal, id));
    }

    @PostMapping("/batches")
    public Result<Map<String, Object>> createBatch(Principal principal, @RequestBody VisualAnalysisService.BatchRequest request) {
        return Result.success("可视化矩阵分析已完成", visualAnalysisService.createBatch(principal, request));
    }

    @PostMapping("/results/{id}/ai-panel")
    public Result<Map<String, Object>> regenerateAiPanel(Principal principal, @PathVariable Long id) {
        return Result.success("AI 面板已刷新", visualAnalysisService.regenerateAiPanel(principal, id));
    }

    @PostMapping("/results/{id}/ai-panel/model")
    public Result<Map<String, Object>> regenerateModelAiPanel(Principal principal, @PathVariable Long id) {
        return Result.success("模型分析已生成", visualAnalysisService.regenerateModelAiPanel(principal, id));
    }

    @PostMapping("/results/{id}/ai-panel/rule")
    public Result<Map<String, Object>> regenerateRuleAiPanel(Principal principal, @PathVariable Long id) {
        return Result.success("规则分析已刷新", visualAnalysisService.regenerateRuleAiPanel(principal, id));
    }

    @PostMapping("/results/save")
    public Result<Map<String, Object>> saveResults(
        Principal principal,
        @RequestBody VisualAnalysisService.SaveResultsRequest request
    ) {
        return Result.success("分析记录已保存", visualAnalysisService.saveResults(principal, request));
    }

    @GetMapping("/results/saved")
    public Result<List<Map<String, Object>>> savedResults(
        Principal principal,
        @RequestParam(defaultValue = "50") int limit
    ) {
        return Result.success(visualAnalysisService.listSavedResults(principal, limit));
    }

    @PostMapping("/results/import-chat")
    public Result<Map<String, Object>> importResultsToChat(
        Principal principal,
        @RequestBody VisualAnalysisService.ImportToChatRequest request
    ) {
        return Result.success("已生成新的 AI 分析对话", visualAnalysisService.importResultsToChat(principal, request));
    }
}
