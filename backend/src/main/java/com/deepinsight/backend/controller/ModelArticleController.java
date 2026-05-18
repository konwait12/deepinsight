package com.deepinsight.backend.controller;

import com.deepinsight.backend.common.Result;
import com.deepinsight.backend.entity.ModelArticle;
import com.deepinsight.backend.repository.ModelArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/models/articles")
@RequiredArgsConstructor
public class ModelArticleController {

    private final ModelArticleRepository repository;

    @GetMapping("/{id}")
    public Result<ModelArticle> get(@PathVariable Long id) {
        ModelArticle a = repository.findById(id).orElse(null);
        if (a == null) return Result.error(404, "文章不存在");
        a.setViewCount(a.getViewCount() + 1);
        return Result.success(repository.save(a));
    }

    @GetMapping("/by-model/{modelId}")
    public Result<ModelArticle> getByModelId(@PathVariable Long modelId) {
        ModelArticle a = repository.findByModelId(modelId).orElse(null);
        if (a == null) return Result.error(404, "文章不存在");
        a.setViewCount(a.getViewCount() + 1);
        return Result.success(repository.save(a));
    }
}
