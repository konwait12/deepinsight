package com.deepinsight.backend.controller;

import com.deepinsight.backend.common.Result;
import com.deepinsight.backend.entity.ModelRegistry;
import com.deepinsight.backend.entity.User;
import com.deepinsight.backend.repository.ModelArticleRepository;
import com.deepinsight.backend.repository.ModelRegistryRepository;
import com.deepinsight.backend.repository.UserRepository;
import com.deepinsight.backend.service.ModelCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/models")
@RequiredArgsConstructor
public class ModelController {

    private final ModelRegistryRepository repository;
    private final ModelArticleRepository articleRepository;
    private final UserRepository userRepository;
    private final ModelCatalogService modelCatalogService;

    private Long getUserId(Principal principal) {
        if (principal == null) return null;
        return userRepository.findByUsername(principal.getName()).map(User::getId).orElse(null);
    }

    private boolean isAdmin(Principal principal) {
        if (principal == null) return false;
        return userRepository.findByUsername(principal.getName())
            .map(user -> user.getRole() == User.Role.ADMIN)
            .orElse(false);
    }

    @GetMapping
    public Result<Map<String, Object>> list(Principal principal) {
        List<ModelRegistry> official = repository.findByIsOfficialTrueOrderByNameAsc();
        List<ModelRegistry> userModels = Collections.emptyList();
        Long uid = getUserId(principal);
        if (uid != null) {
            userModels = repository.findByCreatedByOrderByNameAsc(uid);
        }

        List<Map<String, Object>> officialWithArticles = new ArrayList<>(official.stream().map(model -> {
            Map<String, Object> map = modelToMap(model);
            articleRepository.findByModelId(model.getId()).ifPresent(article -> map.put("articleId", article.getId()));
            return map;
        }).toList());
        officialWithArticles.add(bsarecModel());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("official", officialWithArticles);
        result.put("userModels", userModels.stream().map(this::modelToMap).toList());
        return Result.success(result);
    }

    @GetMapping("/official")
    public Result<List<ModelRegistry>> official() {
        return Result.success(repository.findByIsOfficialTrueOrderByNameAsc());
    }

    @PostMapping
    public Result<ModelRegistry> create(@RequestBody ModelRegistry model, Principal principal) {
        model.setId(null);
        model.setIsOfficial(false);
        Long uid = getUserId(principal);
        if (uid != null) model.setCreatedBy(uid);
        return Result.success("模型已注册", repository.save(model));
    }

    @PutMapping("/{id}")
    public Result<ModelRegistry> update(@PathVariable Long id, @RequestBody ModelRegistry model, Principal principal) {
        ModelRegistry existing = repository.findById(id).orElseThrow();
        if (Boolean.TRUE.equals(existing.getIsOfficial())) {
            return Result.error(403, "官方模型只能在管理后台维护");
        }
        Long uid = getUserId(principal);
        if (uid != null && existing.getCreatedBy() != null && !existing.getCreatedBy().equals(uid)) {
            return Result.error(403, "无权修改");
        }
        model.setId(id);
        model.setCreatedBy(existing.getCreatedBy());
        model.setIsOfficial(existing.getIsOfficial());
        return Result.success("更新成功", repository.save(model));
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id, Principal principal) {
        ModelRegistry existing = repository.findById(id).orElseThrow();
        if (Boolean.TRUE.equals(existing.getIsOfficial())) return Result.error(403, "官方模型只能在管理后台维护");
        Long uid = getUserId(principal);
        if (uid != null && existing.getCreatedBy() != null && !existing.getCreatedBy().equals(uid)) {
            return Result.error(403, "无权删除");
        }
        repository.deleteById(id);
        return Result.success("已删除");
    }

    @PostMapping("/seed-articles")
    public Result<String> seedModelArticles(@RequestParam(defaultValue = "false") boolean force, Principal principal) {
        if (!isAdmin(principal)) return Result.error(403, "Admin required");
        int count = modelCatalogService.seedModelArticles(force);
        if (count < 0) {
            return Result.error(400, "请先执行 /api/v1/models/seed");
        }
        return Result.success("已创建/更新 " + count + " 篇模型文章");
    }

    @PostMapping("/seed")
    public Result<String> seedOfficialModels(Principal principal) {
        if (!isAdmin(principal)) return Result.error(403, "Admin required");
        return Result.success(modelCatalogService.seedOfficialModels());
    }

    private Map<String, Object> bsarecModel() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", -1001);
        map.put("name", "BSARec-Job");
        map.put("displayNameZh", "BSARec 岗位推荐模型");
        map.put("taskType", "recommendation");
        map.put("taskTypeZh", "推荐系统");
        map.put("paramCountM", 0.2);
        map.put("inputSize", "50 item ids");
        map.put("framework", "pytorch/cpu");
        map.put("isOfficial", true);
        map.put("official", true);
        map.put("readOnly", true);
        map.put("canManage", false);
        map.put("canSync", false);
        map.put("description", "BSARec sequential recommendation model served by the local Flask API.");
        map.put("descriptionZh", "通过本地 Flask API 提供 CPU 推理的 BSARec 序列推荐模型。");
        map.put("integrationType", "external-api");
        return map;
    }

    private Map<String, Object> modelToMap(ModelRegistry model) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", model.getId());
        map.put("name", model.getName());
        map.put("displayNameZh", model.getDisplayNameZh());
        map.put("taskType", model.getTaskType());
        map.put("taskTypeZh", model.getTaskTypeZh());
        map.put("paramCountM", model.getParamCountM());
        map.put("inputSize", model.getInputSize());
        map.put("framework", model.getFramework());
        map.put("isOfficial", model.getIsOfficial());
        map.put("official", model.getIsOfficial());
        map.put("readOnly", Boolean.TRUE.equals(model.getIsOfficial()));
        map.put("canManage", !Boolean.TRUE.equals(model.getIsOfficial()));
        map.put("canSync", !Boolean.TRUE.equals(model.getIsOfficial()));
        map.put("description", model.getDescription());
        map.put("descriptionZh", model.getDescriptionZh());
        map.put("paperUrl", model.getPaperUrl());
        return map;
    }
}
