package com.deepinsight.backend.controller;

import com.deepinsight.backend.common.Result;
import com.deepinsight.backend.repository.*;
import com.deepinsight.backend.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepo;
    private final ModelRegistryRepository modelRepo;
    private final ModelArticleRepository modelArticleRepo;
    private final KnowledgeNodeRepository nodeRepo;
    private final KnowledgeArticleRepository articleRepo;
    private final ForumPostRepository postRepo;
    private final ForumCommentRepository commentRepo;
    private final DatasetRepository datasetRepo;
    private final TrainingJobRepository trainingRepo;

    private boolean isAdmin(Principal p) {
        if (p == null) return false;
        User u = userRepo.findByUsername(p.getName()).orElse(null);
        return u != null && u.getRole() == User.Role.ADMIN;
    }

    private Result<?> adminOnly(Principal p) {
        return isAdmin(p) ? null : Result.error(403, "需要管理员权限");
    }

    // ========== 系统状态 ==========
    @GetMapping("/status")
    public Result<Map<String, Object>> status(Principal p) {
        Result<?> check = adminOnly(p); if (check != null) return (Result<Map<String, Object>>) check;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("users", userRepo.count());
        m.put("models", modelRepo.count());
        m.put("modelArticles", modelArticleRepo.count());
        m.put("knowledgeNodes", nodeRepo.count());
        m.put("knowledgeArticles", articleRepo.count());
        m.put("forumPosts", postRepo.count());
        m.put("forumComments", commentRepo.count());
        m.put("datasets", datasetRepo.count());
        m.put("trainingJobs", trainingRepo.count());
        return Result.success(m);
    }

    // ========== 用户管理 ==========
    @GetMapping("/users")
    public Result<List<User>> users(Principal p) {
        Result<?> check = adminOnly(p); if (check != null) return (Result<List<User>>) check;
        return Result.success(userRepo.findAll());
    }

    @PutMapping("/users/{id}/role")
    public Result<String> setRole(@PathVariable Long id, @RequestParam String role, Principal p) {
        Result<?> check = adminOnly(p); if (check != null) return (Result<String>) check;
        User u = userRepo.findById(id).orElse(null);
        if (u == null) return Result.error(404, "用户不存在");
        u.setRole(User.Role.valueOf(role));
        userRepo.save(u);
        return Result.success("角色已更新");
    }

    @DeleteMapping("/users/{id}")
    public Result<String> deleteUser(@PathVariable Long id, Principal p) {
        Result<?> check = adminOnly(p); if (check != null) return (Result<String>) check;
        userRepo.deleteById(id);
        return Result.success("已删除");
    }

    // ========== 模型管理 ==========
    @GetMapping("/models")
    public Result<List<Map<String, Object>>> models(Principal p) {
        Result<?> check = adminOnly(p); if (check != null) return (Result<List<Map<String, Object>>>) check;
        return Result.success(modelRepo.findAll().stream().map(m -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", m.getId()); map.put("name", m.getName());
            map.put("displayNameZh", m.getDisplayNameZh());
            map.put("taskType", m.getTaskType()); map.put("paramCountM", m.getParamCountM());
            map.put("taskTypeZh", m.getTaskTypeZh());
            map.put("framework", m.getFramework()); map.put("isOfficial", m.getIsOfficial());
            map.put("createdBy", m.getCreatedBy());
            return map;
        }).toList());
    }

    @DeleteMapping("/models/{id}")
    @Transactional
    public Result<String> deleteModel(@PathVariable Long id, Principal p) {
        Result<?> check = adminOnly(p); if (check != null) return (Result<String>) check;
        modelArticleRepo.findByModelId(id).ifPresent(modelArticleRepo::delete);
        modelRepo.deleteById(id);
        return Result.success("已删除");
    }

    // ========== 知识库 ==========
    @GetMapping("/knowledge-nodes")
    public Result<?> knowledgeNodes(Principal p) {
        Result<?> check = adminOnly(p); if (check != null) return check;
        return Result.success(nodeRepo.findAll());
    }

    @GetMapping("/knowledge-articles")
    public Result<?> knowledgeArticles(Principal p) {
        Result<?> check = adminOnly(p); if (check != null) return check;
        return Result.success(articleRepo.findAllByOrderByIsPinnedDescCreatedAtDesc());
    }

    @DeleteMapping("/knowledge-articles/{id}")
    public Result<String> deleteArticle(@PathVariable Long id, Principal p) {
        Result<?> check = adminOnly(p); if (check != null) return (Result<String>) check;
        articleRepo.deleteById(id);
        return Result.success("已删除");
    }

    // ========== 论坛 ==========
    @GetMapping("/forum-posts")
    public Result<?> forumPosts(Principal p) {
        Result<?> check = adminOnly(p); if (check != null) return check;
        return Result.success(postRepo.findAll());
    }

    @DeleteMapping("/forum-posts/{id}")
    @Transactional
    public Result<String> deletePost(@PathVariable Long id, Principal p) {
        Result<?> check = adminOnly(p); if (check != null) return (Result<String>) check;
        commentRepo.findByPostIdOrderByCreatedAtAsc(id).forEach(c -> commentRepo.deleteById(c.getId()));
        postRepo.deleteById(id);
        return Result.success("已删除");
    }

    // ========== 数据集 / 训练任务 ==========
    @GetMapping("/datasets")
    public Result<?> datasets(Principal p) {
        Result<?> check = adminOnly(p); if (check != null) return check;
        return Result.success(datasetRepo.findAll());
    }

    @GetMapping("/training-jobs")
    public Result<?> trainingJobs(Principal p) {
        Result<?> check = adminOnly(p); if (check != null) return check;
        return Result.success(trainingRepo.findAll());
    }
}
