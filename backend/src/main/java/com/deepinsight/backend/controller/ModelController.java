package com.deepinsight.backend.controller;

import com.deepinsight.backend.common.Result;
import com.deepinsight.backend.service.ModelCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/models")
@RequiredArgsConstructor
public class ModelController {

    private final ModelCatalogService modelCatalogService;

    @GetMapping
    public Result<Map<String, Object>> list() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("official", modelCatalogService.listModels());
        result.put("userModels", List.of());
        result.put("source", "local-model-directory");
        result.put("databaseBacked", false);
        result.put("modelUploadEnabled", false);
        return Result.success(result);
    }

    @GetMapping("/official")
    public Result<List<Map<String, Object>>> official() {
        return Result.success(modelCatalogService.listModels());
    }
}
