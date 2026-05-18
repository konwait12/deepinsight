package com.deepinsight.backend.controller;

import com.deepinsight.backend.common.Constants;
import com.deepinsight.backend.common.Result;
import com.deepinsight.backend.dto.AuthRequest;
import com.deepinsight.backend.dto.AuthResponse;
import com.deepinsight.backend.entity.User;
import com.deepinsight.backend.repository.UserRepository;
import com.deepinsight.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 认证控制器（Controller层 - 表示层）
 * <p>
 * 负责接收和响应HTTP请求，属于Spring MVC三层架构中的表示层。
 * 此层只做请求转发和响应封装，不包含业务逻辑。
 *
 * <p>请求流程：
 * <ol>
 *   <li>接收HTTP请求 → 参数校验（@Valid）</li>
 *   <li>调用Service接口（业务逻辑层）</li>
 *   <li>将Service返回结果封装为统一响应格式 {@link Result}</li>
 * </ol>
 *
 * <p>API路径：所有接口以 {@value Constants#AUTH_API_ROOT} 为前缀。
 *
 * @author DeepInsight Team
 */
@RestController
@RequestMapping(Constants.AUTH_API_ROOT)
@RequiredArgsConstructor
public class AuthController {

    /** 认证服务接口（Spring自动注入AuthServiceImpl实现） */
    private final AuthService authService;
    private final UserRepository userRepo;

    /**
     * 服务健康检查接口（公开）
     * <p>
     * 用于验证认证服务是否正常运行，无需认证即可访问。
     *
     * @return 统一响应结果，data中包含服务状态描述
     */
    @GetMapping("/test")
    public Result<String> test() {
        return Result.success("Auth Service is running! (PermitAll)");
    }

    /**
     * 用户注册接口（公开）
     * <p>
     * 接收用户名和密码创建新账户，注册成功后直接返回JWT令牌（注册即登录）。
     * 新注册用户默认分配USER角色，可通过管理员接口升级。
     *
     * @param request 注册请求（含用户名和密码，通过@Valid自动校验）
     * @return 统一响应结果，data中包含JWT令牌和用户基本信息
     */
    @PostMapping("/register")
    public Result<AuthResponse> register(@Valid @RequestBody AuthRequest request) {
        AuthResponse authResponse = authService.register(request);
        return Result.success("注册成功", authResponse);
    }

    /**
     * 用户登录接口（公开）
     * <p>
     * 验证用户名和密码，认证成功后返回JWT令牌。
     * 后续请求需在Authorization头中携带此令牌（格式：Bearer <token>）。
     *
     * @param request 登录请求（含用户名和密码）
     * @return 统一响应结果，data中包含JWT令牌和用户基本信息
     */
    @PostMapping("/login")
    public Result<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        AuthResponse authResponse = authService.authenticate(request);
        return Result.success("登录成功", authResponse);
    }

    @GetMapping("/me")
    public Result<Map<String, Object>> me(Principal principal) {
        if (principal == null) return Result.error(401, "未登录");
        User user = userRepo.findByUsername(principal.getName()).orElse(null);
        if (user == null) return Result.error(404, "用户不存在");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", user.getId());
        m.put("username", user.getUsername());
        m.put("email", user.getEmail());
        m.put("role", user.getRole().name());
        return Result.success(m);
    }
}
