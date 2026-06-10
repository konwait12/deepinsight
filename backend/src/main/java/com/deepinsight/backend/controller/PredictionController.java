package com.deepinsight.backend.controller;

import com.deepinsight.backend.common.Result;
import com.deepinsight.backend.service.BSARecClientService;
import com.deepinsight.backend.service.ModelCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/prediction")
@RequiredArgsConstructor
public class PredictionController {

    private final ModelCatalogService modelCatalogService;
    private final BSARecClientService bsarecClientService;

    @PostMapping("/recommend")
    public Result<Map<String, Object>> recommend(@RequestBody Map<String, Object> request) {
        return Result.success(bsarecClientService.recommend(request));
    }

    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        return Result.success(bsarecClientService.health());
    }

    @GetMapping("/models")
    public Result<List<Map<String, Object>>> listModels() {
        return Result.success(modelCatalogService.listPredictionModels());
    }
}
