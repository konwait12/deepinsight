package com.deepinsight.backend.controller;

import com.deepinsight.backend.common.Result;
import com.deepinsight.backend.entity.AiConfig;
import com.deepinsight.backend.entity.User;
import com.deepinsight.backend.repository.UserRepository;
import com.deepinsight.backend.service.AiConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/**
 * AI助手控制器
 * <p>
 * 管理AI模型配置和对话接口。
 * 支持Ollama本地模型、OpenAI API、Gemini API。
 */
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiConfigService aiConfigService;
    private final UserRepository userRepository;

    private boolean isAdmin(Principal principal) {
        if (principal == null) return false;
        return userRepository.findByUsername(principal.getName())
            .map(user -> user.getRole() == User.Role.ADMIN)
            .orElse(false);
    }

    private <T> Result<T> requireAdmin(Principal principal) {
        return isAdmin(principal) ? null : Result.error(403, "Admin permission required");
    }

    /** 获取所有AI配置 */
    @GetMapping("/configs")
    public Result<List<AiConfig>> getConfigs(Principal principal) {
        Result<List<AiConfig>> denied = requireAdmin(principal);
        if (denied != null) return denied;
        return Result.success(aiConfigService.findAll());
    }

    /** 获取当前激活的AI配置 */
    @GetMapping("/configs/active")
    public Result<AiConfig> getActive(Principal principal) {
        Result<AiConfig> denied = requireAdmin(principal);
        if (denied != null) return denied;
        return Result.success(aiConfigService.getActive());
    }

    /** 保存AI配置 */
    @PostMapping("/configs")
    public Result<AiConfig> save(@RequestBody AiConfig config, Principal principal) {
        Result<AiConfig> denied = requireAdmin(principal);
        if (denied != null) return denied;
        return Result.success("保存成功", aiConfigService.save(config));
    }

    /** 更新AI配置 */
    @PutMapping("/configs/{id}")
    public Result<AiConfig> update(@PathVariable Long id, @RequestBody AiConfig config, Principal principal) {
        Result<AiConfig> denied = requireAdmin(principal);
        if (denied != null) return denied;
        config.setId(id);
        return Result.success("更新成功", aiConfigService.save(config));
    }

    /** 激活指定AI配置 */
    @PutMapping("/configs/{id}/activate")
    public Result<AiConfig> activate(@PathVariable Long id, Principal principal) {
        Result<AiConfig> denied = requireAdmin(principal);
        if (denied != null) return denied;
        return Result.success("已激活", aiConfigService.activate(id));
    }

    /** 删除AI配置 */
    @DeleteMapping("/configs/{id}")
    public Result<String> delete(@PathVariable Long id, Principal principal) {
        Result<String> denied = requireAdmin(principal);
        if (denied != null) return denied;
        aiConfigService.delete(id);
        return Result.success("已删除");
    }
}
