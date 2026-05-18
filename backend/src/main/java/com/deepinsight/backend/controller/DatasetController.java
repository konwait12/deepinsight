package com.deepinsight.backend.controller;

import com.deepinsight.backend.common.Result;
import com.deepinsight.backend.entity.Dataset;
import com.deepinsight.backend.entity.User;
import com.deepinsight.backend.repository.DatasetRepository;
import com.deepinsight.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/datasets")
@RequiredArgsConstructor
public class DatasetController {

    private final DatasetRepository repository;
    private final UserRepository userRepository;

    private Long getUserId(Principal p) {
        if (p == null) return null;
        return userRepository.findByUsername(p.getName()).map(User::getId).orElse(null);
    }

    @GetMapping
    public Result<List<Dataset>> list(@RequestParam(required = false) String taskType,
                                       @RequestParam(required = false) String status,
                                       Principal principal) {
        List<Dataset> all = taskType != null ? repository.findByTaskType(taskType) :
                            status != null ? repository.findByStatusOrderByCreatedAtDesc(status) :
                            repository.findAll();
        Long uid = getUserId(principal);
        if (uid != null) { final Long id = uid; all = all.stream().filter(d -> id.equals(d.getUploadedBy())).toList(); }
        return Result.success(all);
    }

    @GetMapping("/{id}")
    public Result<Dataset> get(@PathVariable Long id, Principal principal) {
        Dataset ds = repository.findById(id).orElse(null);
        if (ds == null) return Result.error(404, "Not found");
        Long uid = getUserId(principal);
        if (uid != null && ds.getUploadedBy() != null && !ds.getUploadedBy().equals(uid))
            return Result.error(403, "无权查看");
        return Result.success(ds);
    }

    @PostMapping
    public Result<Dataset> create(@RequestBody Dataset dataset, Principal principal) {
        Long uid = getUserId(principal);
        if (uid != null) dataset.setUploadedBy(uid);
        return Result.success("数据集注册成功", repository.save(dataset));
    }

    @PutMapping("/{id}")
    public Result<Dataset> update(@PathVariable Long id, @RequestBody Dataset dataset, Principal principal) {
        Dataset existing = repository.findById(id).orElseThrow();
        Long uid = getUserId(principal);
        if (uid != null && existing.getUploadedBy() != null && !existing.getUploadedBy().equals(uid))
            return Result.error(403, "无权操作");
        dataset.setId(id); dataset.setUploadedBy(existing.getUploadedBy());
        return Result.success("更新成功", repository.save(dataset));
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id, Principal principal) {
        Dataset existing = repository.findById(id).orElseThrow();
        Long uid = getUserId(principal);
        if (uid != null && existing.getUploadedBy() != null && !existing.getUploadedBy().equals(uid))
            return Result.error(403, "无权操作");
        repository.deleteById(id);
        return Result.success("已删除");
    }
}
