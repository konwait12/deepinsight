package com.deepinsight.backend.controller;

import com.deepinsight.backend.common.Result;
import com.deepinsight.backend.entity.KnowledgeDoc;
import com.deepinsight.backend.entity.User;
import com.deepinsight.backend.repository.KnowledgeDocRepository;
import com.deepinsight.backend.repository.UserRepository;
import com.deepinsight.backend.service.AiConfigService;
import com.deepinsight.backend.service.AiWorkspaceService;
import com.deepinsight.backend.service.ChatSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.*;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiChatController {

    private final AiConfigService aiConfigService;
    private final AiWorkspaceService workspaceService;
    private final ChatSessionService sessionService;
    private final KnowledgeDocRepository kbRepo;
    private final UserRepository userRepo;

    private Long getUserId(Principal p) {
        if (p == null) return null;
        return userRepo.findByUsername(p.getName()).map(User::getId).orElse(null);
    }

    private Long requireUserId(Principal p) {
        Long userId = getUserId(p);
        if (userId == null || userId <= 0L) {
            throw new IllegalArgumentException("请先登录后再使用 AI 对话");
        }
        return userId;
    }

    @PostMapping("/chat")
    public Result<Map<String, Object>> chat(@RequestBody Map<String, Object> request, Principal principal) {
        String message = (String) request.getOrDefault("message", "");
        List<?> attachments = request.get("attachments") instanceof List<?> list ? list : List.of();
        List<?> resources = request.get("resources") instanceof List<?> list ? list : List.of();
        boolean deepThink = Boolean.TRUE.equals(request.get("deepThink"));
        String reasoningLevel = String.valueOf(request.getOrDefault("reasoningLevel", deepThink ? "high" : "off"));
        Double temperature = request.get("temperature") instanceof Number n ? n.doubleValue() : null;
        Long convId = request.get("conversationId") != null ?
                Long.valueOf(request.get("conversationId").toString()) : null;
        Long userId = requireUserId(principal);

        // Create or verify conversation ownership
        if (convId == null) {
            String title = message.length() > 30 ? message.substring(0, 30) + "..." : message;
            convId = sessionService.createConversation(userId, title);
        } else if (!sessionService.isOwner(convId, userId)) {
            return Result.error(403, "无权访问此对话");
        }

        // Load history + memories (scoped to user)
        List<Map<String, String>> history = sessionService.getHistory(convId);
        String memoryContext = "";
        List<Map<String, Object>> memories = sessionService.getMemories(userId);
        if (!memories.isEmpty()) {
            StringBuilder ctx = new StringBuilder("[用户相关记忆]\n");
            memories.stream()
                .filter(m -> {
                    Object imp = m.get("importance");
                    float f = imp instanceof Number ? ((Number) imp).floatValue() : 0f;
                    return f > 0.3f;
                })
                .limit(3)
                .forEach(m -> ctx.append("- ").append(m.get("content")).append("\n"));
            memoryContext = ctx.toString();
        }

        // Knowledge base search (public)
        String kbContext = "";
        try {
            var kbDocs = kbRepo.searchByFulltext(message);
            if (kbDocs != null && !kbDocs.isEmpty()) {
                StringBuilder ctx = new StringBuilder("[相关知识库文档]\n");
                for (KnowledgeDoc doc : kbDocs) {
                    String snippet = doc.getContent().length() > 300 ?
                            doc.getContent().substring(0, 300) + "..." : doc.getContent();
                    ctx.append("--- ").append(doc.getTitle()).append(" ---\n").append(snippet).append("\n\n");
                }
                kbContext = ctx.toString();
            }
        } catch (Exception ignored) {}

        AiWorkspaceService.WorkspaceContext workspaceContext = workspaceService.buildWorkspaceContext(principal, attachments, resources);

        // Enrich + AI call
        String enrichedMessage = message;
        if (!memoryContext.isEmpty() || !kbContext.isEmpty() || !workspaceContext.text().isEmpty()) {
            enrichedMessage = memoryContext + kbContext + workspaceContext.text() + "[用户问题]\n" + message;
        }
        Map<String, Object> aiResponse = aiConfigService.chat(
            enrichedMessage,
            history,
            new AiConfigService.ChatOptions(deepThink, reasoningLevel, temperature, workspaceContext.visionInputs())
        );

        // Save to Redis
        String aiReply = (String) aiResponse.getOrDefault("content", "");
        String aiRole = (String) aiResponse.getOrDefault("role", "assistant");
        sessionService.appendMessage(convId, Map.of("role", "user", "content", message));
        sessionService.appendMessage(convId, Map.of("role", aiRole, "content", aiReply));

        // Auto-extract memory
        if (message.contains("我") && aiReply.length() > 50) {
            String memContent = "用户问题: " + (message.length() > 200 ? message.substring(0, 200) : message);
            sessionService.saveMemory(userId, memContent, 0.5f);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("conversationId", convId);
        result.put("reply", aiReply);
        result.put("role", aiRole);
        result.put("memoryUsed", !memoryContext.isEmpty());
        result.put("kbDocsFound", !kbContext.isEmpty());
        if (aiResponse.containsKey("reasoning")) {
            result.put("reasoning", aiResponse.get("reasoning"));
        }
        return Result.success(result);
    }

    @GetMapping("/conversations")
    public Result<List<Map<String, Object>>> conversations(Principal principal) {
        Long userId = requireUserId(principal);
        return Result.success(sessionService.getUserConversations(userId));
    }

    @GetMapping("/conversations/{id}/messages")
    public Result<List<Map<String, String>>> messages(@PathVariable Long id, Principal principal) {
        Long userId = requireUserId(principal);
        if (!sessionService.isOwner(id, userId)) return Result.error(403, "无权访问");
        return Result.success(sessionService.getHistory(id));
    }

    @DeleteMapping("/conversations/{id}")
    public Result<String> deleteConversation(@PathVariable Long id, Principal principal) {
        Long userId = requireUserId(principal);
        if (!sessionService.isOwner(id, userId)) return Result.error(403, "无权访问");
        sessionService.deleteConversation(id);
        return Result.success("已删除");
    }

    @GetMapping("/knowledge")
    public Result<List<KnowledgeDoc>> knowledge(@RequestParam(required = false) String category) {
        if (category != null) return Result.success(kbRepo.findByCategory(category));
        return Result.success(kbRepo.findAll());
    }

    @PostMapping("/knowledge")
    public Result<KnowledgeDoc> addKnowledge(@RequestBody KnowledgeDoc doc) {
        return Result.success("已添加", kbRepo.save(doc));
    }

    @DeleteMapping("/knowledge/{id}")
    public Result<String> deleteKnowledge(@PathVariable Long id) {
        kbRepo.deleteById(id);
        return Result.success("已删除");
    }
}
