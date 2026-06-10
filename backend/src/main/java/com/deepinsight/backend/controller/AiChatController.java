package com.deepinsight.backend.controller;

import com.deepinsight.backend.common.Result;
import com.deepinsight.backend.entity.KnowledgeDoc;
import com.deepinsight.backend.entity.User;
import com.deepinsight.backend.repository.KnowledgeDocRepository;
import com.deepinsight.backend.repository.UserRepository;
import com.deepinsight.backend.service.AiConfigService;
import com.deepinsight.backend.service.AiWorkspaceService;
import com.deepinsight.backend.service.ChatSessionService;
import com.deepinsight.backend.service.KnowledgeTrainingService;
import com.deepinsight.backend.service.ModelCatalogService;
import com.deepinsight.backend.service.SiteKnowledgeService;
import com.deepinsight.backend.service.SiteNavigationService;
import com.deepinsight.backend.service.WebSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiChatController {

    private static final int MAX_CHAT_MESSAGE_CHARS = 8_000;
    private static final Pattern TECH_TOKEN_PATTERN = Pattern.compile("\\b[A-Za-z][A-Za-z0-9]*(?:[-_][A-Za-z0-9]+)+\\b");
    private static final List<String> SEARCH_CONTEXT_ANCHORS = List.of(
        "BSARec Job", "BSARec", "BERT4Rec", "DuoRec", "FEARec", "FMLP-Rec", "FMLPRec",
        "RecBole", "SASRec", "TiSASRec", "Job", "Beauty", "LastFM", "ML-100K", "ML-1M",
        "Steam", "Video", "Top-K", "HR@K", "HR@10", "NDCG@K", "NDCG@10", "MRR",
        "\u63a8\u8350\u7cfb\u7edf", "\u63a8\u8350\u6a21\u578b", "\u5e8f\u5217\u63a8\u8350",
        "\u7528\u6237\u5e8f\u5217", "\u4ea4\u4e92\u6570", "\u7269\u54c1\u6570", "\u670d\u52a1\u79bb\u7ebf",
        "recommender system", "recommendation model", "sequential recommendation"
    );

    private final AiConfigService aiConfigService;
    private final AiWorkspaceService workspaceService;
    private final SiteKnowledgeService siteKnowledgeService;
    private final ModelCatalogService modelCatalogService;
    private final SiteNavigationService siteNavigationService;
    private final WebSearchService webSearchService;
    private final ChatSessionService sessionService;
    private final KnowledgeTrainingService knowledgeTrainingService;
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

    private boolean isAdmin(Principal p) {
        if (p == null) return false;
        return userRepo.findByUsername(p.getName())
            .map(user -> user.getRole() == User.Role.ADMIN)
            .orElse(false);
    }

    @PostMapping("/chat")
    public Result<Map<String, Object>> chat(@RequestBody Map<String, Object> request, Principal principal) {
        if (request == null) request = Map.of();
        String message = text(request.getOrDefault("message", "")).trim();
        if (message.isBlank()) return Result.error(400, "消息不能为空");
        if (message.length() > MAX_CHAT_MESSAGE_CHARS) {
            return Result.error(400, "单次消息不能超过 " + MAX_CHAT_MESSAGE_CHARS + " 字符");
        }
        List<?> attachments = request.get("attachments") instanceof List<?> list ? list : List.of();
        List<?> resources = request.get("resources") instanceof List<?> list ? list : List.of();
        boolean deepThink = Boolean.TRUE.equals(request.get("deepThink"));
        String reasoningLevel = normalizeReasoning(String.valueOf(request.getOrDefault("reasoningLevel", deepThink ? "high" : "off")));
        boolean webSearchFlagProvided = request.containsKey("webSearch");
        boolean webSearchRequested = Boolean.TRUE.equals(request.get("webSearch"));
        Double temperature = boundedTemperature(request.get("temperature"));
        Long convId = parseLong(request.get("conversationId"));
        Long userId = requireUserId(principal);

        // Create or verify conversation ownership
        if (convId == null) {
            String title = safeConversationTitle(message);
            convId = sessionService.createConversation(userId, title);
        } else if (!sessionService.isOwner(convId, userId)) {
            return Result.error(403, "无权访问此对话");
        }

        // Load history + memories (scoped to user)
        List<Map<String, Object>> storedHistory = sessionService.getHistory(convId);
        List<Map<String, String>> history = toChatHistory(storedHistory);
        String retrievalQuery = buildRetrievalQuery(message, storedHistory);
        boolean directMode = isDirectReasoning(reasoningLevel);
        boolean quickMode = isQuickReasoning(reasoningLevel);
        String memoryContext = directMode ? "" : buildMemoryContext(userId);
        String kbContext = (directMode || quickMode) ? "" : buildKbContext(retrievalQuery);

        AiWorkspaceService.WorkspaceContext workspaceContext = workspaceService.buildWorkspaceContext(principal, attachments, resources);
        String siteContext = directMode ? "" : siteKnowledgeService.buildContext(retrievalQuery);
        List<Map<String, Object>> relatedArticles = directMode
            ? List.of()
            : siteKnowledgeService.findRelatedContent(retrievalQuery, relatedLimit(reasoningLevel));
        String relatedArticleContext = buildRelatedArticleContext(relatedArticles);
        Map<String, Object> navigation = siteNavigationService.resolve(message);
        boolean webSearchAttempted = webSearchFlagProvided
            ? webSearchRequested && webSearchService.shouldSearch(retrievalQuery, true)
            : !directMode && webSearchService.shouldSearch(retrievalQuery, false);
        String webSearchQuery = webSearchAttempted ? webSearchService.displayQuery(retrievalQuery) : retrievalQuery;
        List<Map<String, Object>> webResults = webSearchAttempted
            ? webSearchService.search(retrievalQuery, true, webSearchLimit(reasoningLevel))
            : List.of();
        String webContext = webSearchService.buildContext(webResults);
        String reasoningContext = buildReasoningContext(reasoningLevel, webSearchRequested, !webResults.isEmpty(), !relatedArticles.isEmpty());

        // Enrich + AI call
        String enrichedMessage = message;
        if (!siteContext.isEmpty()
            || !memoryContext.isEmpty()
            || !kbContext.isEmpty()
            || !workspaceContext.text().isEmpty()
            || !relatedArticleContext.isEmpty()
            || !webContext.isEmpty()
            || !reasoningContext.isEmpty()) {
            enrichedMessage = reasoningContext
                + siteContext
                + memoryContext
                + kbContext
                + relatedArticleContext
                + webContext
                + workspaceContext.text()
                + "[用户问题]\n"
                + message;
        }
        Map<String, Object> aiResponse = aiConfigService.chat(
            enrichedMessage,
            history,
            new AiConfigService.ChatOptions(deepThink, reasoningLevel, temperature, workspaceContext.visionInputs())
        );

        String aiReply = (String) aiResponse.getOrDefault("content", "");
        String aiRole = (String) aiResponse.getOrDefault("role", "assistant");
        Object visibleReasoning = hasContent(aiResponse.get("reasoning"))
            ? aiResponse.get("reasoning")
            : (deepThink ? buildVisibleReasoningSummary(reasoningLevel, relatedArticles, webResults, navigation) : null);
        Map<String, Object> reasoningDiagnostics = buildReasoningDiagnostics(
            reasoningLevel,
            deepThink,
            temperature,
            relatedArticles,
            webSearchRequested,
            webSearchAttempted,
            webResults,
            webSearchQuery,
            aiResponse.get("reasoningDiagnostics"),
            attachments.size() + resources.size(),
            !memoryContext.isEmpty(),
            !kbContext.isEmpty(),
            !siteContext.isEmpty(),
            !workspaceContext.text().isEmpty()
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("conversationId", convId);
        result.put("reply", aiReply);
        result.put("role", aiRole);
        result.put("memoryUsed", !memoryContext.isEmpty());
        result.put("kbDocsFound", !kbContext.isEmpty());
        result.put("siteContextUsed", !siteContext.isEmpty());
        result.put("relatedArticles", relatedArticles);
        result.put("webResults", webResults);
        result.put("webSearchAttempted", webSearchAttempted);
        result.put("webSearchUsed", !webResults.isEmpty());
        result.put("webSearchQuery", webSearchQuery);
        result.put("reasoningLevel", reasoningLevel);
        result.put("reasoningDiagnostics", reasoningDiagnostics);
        if (aiResponse.containsKey("apiStatus")) result.put("apiStatus", aiResponse.get("apiStatus"));
        if (!navigation.isEmpty()) result.put("navigation", navigation);
        if (hasContent(visibleReasoning)) result.put("reasoning", visibleReasoning);

        // Save full assistant UI metadata with the message so reopened conversations restore cards.
        sessionService.appendMessage(convId, Map.of("role", "user", "content", message));
        Map<String, Object> assistantMessage = new LinkedHashMap<>();
        assistantMessage.put("role", aiRole);
        assistantMessage.put("content", aiReply);
        assistantMessage.put("reasoningLevel", reasoningLevel);
        assistantMessage.put("webSearchAttempted", webSearchAttempted);
        assistantMessage.put("webSearchUsed", !webResults.isEmpty());
        assistantMessage.put("webSearchQuery", webSearchQuery);
        assistantMessage.put("reasoningDiagnostics", reasoningDiagnostics);
        if (aiResponse.containsKey("apiStatus")) assistantMessage.put("apiStatus", aiResponse.get("apiStatus"));
        if (hasContent(visibleReasoning)) assistantMessage.put("reasoning", visibleReasoning);
        if (!navigation.isEmpty()) assistantMessage.put("navigation", navigation);
        if (!relatedArticles.isEmpty()) assistantMessage.put("relatedArticles", relatedArticles);
        if (!webResults.isEmpty()) assistantMessage.put("webResults", webResults);
        sessionService.appendMessage(convId, assistantMessage);

        // Auto-extract memory
        if (message.contains("我") && aiReply.length() > 50) {
            String memContent = "用户问题: " + (message.length() > 200 ? message.substring(0, 200) : message);
            sessionService.saveMemory(userId, memContent, 0.5f);
        }
        return Result.success(result);
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody Map<String, Object> request, Principal principal) {
        Map<String, Object> streamRequest = request == null ? Map.of() : request;
        SseEmitter emitter = new SseEmitter(120_000L);
        CompletableFuture.runAsync(() -> {
            try {
                String message = text(streamRequest.getOrDefault("message", "")).trim();
                if (message.isBlank()) {
                    sendEvent(emitter, "error", Map.of("message", "消息不能为空"));
                    return;
                }
                if (message.length() > MAX_CHAT_MESSAGE_CHARS) {
                    sendEvent(emitter, "error", Map.of("message", "单次消息不能超过 " + MAX_CHAT_MESSAGE_CHARS + " 字符"));
                    return;
                }

                sendEvent(emitter, "status", Map.of("message", "正在读取会话、记忆和站内训练知识。"));
                List<?> attachments = streamRequest.get("attachments") instanceof List<?> list ? list : List.of();
                List<?> resources = streamRequest.get("resources") instanceof List<?> list ? list : List.of();
                boolean deepThink = Boolean.TRUE.equals(streamRequest.get("deepThink"));
                String reasoningLevel = normalizeReasoning(String.valueOf(streamRequest.getOrDefault("reasoningLevel", deepThink ? "high" : "off")));
                boolean webSearchFlagProvided = streamRequest.containsKey("webSearch");
                boolean webSearchRequested = Boolean.TRUE.equals(streamRequest.get("webSearch"));
                Double temperature = boundedTemperature(streamRequest.get("temperature"));
                Long convId = parseLong(streamRequest.get("conversationId"));
                Long userId = requireUserId(principal);

                if (convId == null) {
                    convId = sessionService.createConversation(userId, safeConversationTitle(message));
                } else if (!sessionService.isOwner(convId, userId)) {
                    sendEvent(emitter, "error", Map.of("message", "无权访问此对话"));
                    return;
                }
                sendEvent(emitter, "metadata", Map.of("conversationId", convId));

                List<Map<String, Object>> storedHistory = sessionService.getHistory(convId);
                List<Map<String, String>> history = toChatHistory(storedHistory);
                String retrievalQuery = buildRetrievalQuery(message, storedHistory);
                boolean directMode = isDirectReasoning(reasoningLevel);
                boolean quickMode = isQuickReasoning(reasoningLevel);
                String memoryContext = directMode ? "" : buildMemoryContext(userId);
                String kbContext = (directMode || quickMode) ? "" : buildKbContext(retrievalQuery);

                sendEvent(emitter, "status", Map.of(
                    "message", directMode
                        ? "已切换为直答：只保留必要页面判断和你手动选择的素材。"
                        : "正在装配模型目录、知识文章、页面资源和检索上下文。"
                ));
                AiWorkspaceService.WorkspaceContext workspaceContext = workspaceService.buildWorkspaceContext(principal, attachments, resources);
                String siteContext = directMode ? "" : siteKnowledgeService.buildContext(retrievalQuery);
                List<Map<String, Object>> relatedArticles = directMode
                    ? List.of()
                    : siteKnowledgeService.findRelatedContent(retrievalQuery, relatedLimit(reasoningLevel));
                String relatedArticleContext = buildRelatedArticleContext(relatedArticles);
                Map<String, Object> navigation = siteNavigationService.resolve(message);
                boolean webSearchAttempted = webSearchFlagProvided
                    ? webSearchRequested && webSearchService.shouldSearch(retrievalQuery, true)
                    : !directMode && webSearchService.shouldSearch(retrievalQuery, false);
                String webSearchQuery = webSearchAttempted ? webSearchService.displayQuery(retrievalQuery) : retrievalQuery;
                if (webSearchAttempted) {
                    sendEvent(emitter, "status", Map.of(
                        "phase", "web_search",
                        "message", "正在联网搜索：" + webSearchQuery,
                        "webSearchQuery", webSearchQuery,
                        "webSearchAttempted", true
                    ));
                }
                List<Map<String, Object>> webResults = webSearchAttempted
                    ? webSearchService.search(retrievalQuery, true, webSearchLimit(reasoningLevel))
                    : List.of();
                if (webSearchAttempted) {
                    Map<String, Object> searchMetadata = new LinkedHashMap<>();
                    searchMetadata.put("webResults", webResults);
                    searchMetadata.put("webSearchAttempted", true);
                    searchMetadata.put("webSearchUsed", !webResults.isEmpty());
                    searchMetadata.put("webSearchQuery", webSearchQuery);
                    sendEvent(emitter, "metadata", searchMetadata);
                    sendEvent(emitter, "status", Map.of(
                        "phase", "web_search_done",
                        "message", webResults.isEmpty()
                            ? "联网搜索没有返回可用网站，继续使用站内知识回答。"
                            : "已找到 " + webResults.size() + " 条外部参考，正在与站内知识合并。",
                        "webSearchQuery", webSearchQuery,
                        "webResultCount", webResults.size()
                    ));
                }
                String webContext = webSearchService.buildContext(webResults);
                String reasoningContext = buildReasoningContext(reasoningLevel, webSearchRequested, !webResults.isEmpty(), !relatedArticles.isEmpty());
                Map<String, Object> preliminaryDiagnostics = buildReasoningDiagnostics(
                    reasoningLevel,
                    deepThink,
                    temperature,
                    relatedArticles,
                    webSearchRequested,
                    webSearchAttempted,
                    webResults,
                    webSearchQuery,
                    null,
                    attachments.size() + resources.size(),
                    !memoryContext.isEmpty(),
                    !kbContext.isEmpty(),
                    !siteContext.isEmpty(),
                    !workspaceContext.text().isEmpty()
                );
                Map<String, Object> activeMetadata = new LinkedHashMap<>();
                activeMetadata.put("reasoningDiagnostics", preliminaryDiagnostics);
                activeMetadata.put("relatedArticles", relatedArticles);
                activeMetadata.put("webResults", webResults);
                activeMetadata.put("webSearchAttempted", webSearchAttempted);
                activeMetadata.put("webSearchUsed", !webResults.isEmpty());
                activeMetadata.put("webSearchQuery", webSearchQuery);
                sendEvent(emitter, "metadata", activeMetadata);
                String enrichedMessage = enrichMessage(
                    message,
                    reasoningContext,
                    siteContext,
                    memoryContext,
                    kbContext,
                    relatedArticleContext,
                    webContext,
                    workspaceContext.text()
                );

                sendEvent(emitter, "status", Map.of("message", "正在调用外部 API，回答和公开推理摘要会实时显示。"));
                Map<String, Object> aiResponse = aiConfigService.chatStream(
                    enrichedMessage,
                    history,
                    new AiConfigService.ChatOptions(deepThink, reasoningLevel, temperature, workspaceContext.visionInputs()),
                    new AiConfigService.ChatStreamSink() {
                        @Override
                        public void status(String message) {
                            sendEvent(emitter, "status", Map.of("message", message));
                        }

                        @Override
                        public void reasoning(String delta) {
                            if (!delta.isBlank()) sendEvent(emitter, "reasoning", Map.of("delta", delta));
                        }

                        @Override
                        public void content(String delta) {
                            if (!delta.isBlank()) sendEvent(emitter, "content", Map.of("delta", delta));
                        }
                    }
                );

                String aiReply = (String) aiResponse.getOrDefault("content", "");
                String aiRole = (String) aiResponse.getOrDefault("role", "assistant");
                Object visibleReasoning = hasContent(aiResponse.get("reasoning"))
                    ? aiResponse.get("reasoning")
                    : (deepThink ? buildVisibleReasoningSummary(reasoningLevel, relatedArticles, webResults, navigation) : null);
                Map<String, Object> reasoningDiagnostics = buildReasoningDiagnostics(
                    reasoningLevel,
                    deepThink,
                    temperature,
                    relatedArticles,
                    webSearchRequested,
                    webSearchAttempted,
                    webResults,
                    webSearchQuery,
                    aiResponse.get("reasoningDiagnostics"),
                    attachments.size() + resources.size(),
                    !memoryContext.isEmpty(),
                    !kbContext.isEmpty(),
                    !siteContext.isEmpty(),
                    !workspaceContext.text().isEmpty()
                );

                Map<String, Object> result = new LinkedHashMap<>();
                result.put("conversationId", convId);
                result.put("reply", aiReply);
                result.put("role", aiRole);
                result.put("memoryUsed", !memoryContext.isEmpty());
                result.put("kbDocsFound", !kbContext.isEmpty());
                result.put("siteContextUsed", !siteContext.isEmpty());
                result.put("relatedArticles", relatedArticles);
                result.put("webResults", webResults);
                result.put("webSearchAttempted", webSearchAttempted);
                result.put("webSearchUsed", !webResults.isEmpty());
                result.put("webSearchQuery", webSearchQuery);
                result.put("reasoningLevel", reasoningLevel);
                result.put("reasoningDiagnostics", reasoningDiagnostics);
                if (aiResponse.containsKey("apiStatus")) result.put("apiStatus", aiResponse.get("apiStatus"));
                if (!navigation.isEmpty()) result.put("navigation", navigation);
                if (hasContent(visibleReasoning)) result.put("reasoning", visibleReasoning);

                saveAssistantTurn(convId, message, aiRole, aiReply, reasoningLevel, webSearchAttempted, webResults, webSearchQuery, reasoningDiagnostics, visibleReasoning, navigation, relatedArticles, aiResponse);
                if (message.contains("我") && aiReply.length() > 50) {
                    sessionService.saveMemory(userId, "用户问题: " + (message.length() > 200 ? message.substring(0, 200) : message), 0.5f);
                }
                sendEvent(emitter, "metadata", result);
                sendEvent(emitter, "done", Map.of("ok", true));
            } catch (Exception e) {
                sendEvent(emitter, "error", Map.of("message", e.getMessage() == null ? "流式对话失败" : e.getMessage()));
            } finally {
                emitter.complete();
            }
        });
        return emitter;
    }

    private String buildReasoningContext(String level, boolean webSearchRequested, boolean webSearchUsed, boolean relatedFound) {
        String normalized = normalizeReasoning(level);
        StringBuilder context = new StringBuilder("[回答策略]\n");
        switch (normalized) {
            case "off" -> context.append("- 直答模式：只给必要结论，不展开分析步骤；如果用户明确要求跳转，可以给入口。\n");
            case "quick" -> context.append("- 快速模式：优先定位页面、模型或指标，回答控制在短段落，并给最可能的下一步。\n");
            case "low" -> context.append("- 标准模式：给出结论、依据和操作建议；相关页面只作为可选入口，不要强行跳转。\n");
            case "deep" -> context.append("- 深度模式：先判断问题类型，再结合站内模型、指标、相关文章和页面功能分析，给出可执行建议。\n");
            case "max" -> context.append("- 极限模式：跨模型、知识文章、论坛文章、数据/可视化入口多源复核；明确区分站内事实、外部参考和推断。\n");
            default -> context.append("- 标准模式：回答要贴合平台功能，避免泛泛而谈。\n");
        }
        context.append("- 跳转入口规则：只有用户明确要打开页面，或某页面确实能作为下一步操作时才建议；解释类问题先回答，再给可选入口。\n");
        context.append("- 相关文章规则：如果站内相关文章匹配，要在回答末尾用一句话提示可继续阅读，不要把文章标题堆成列表。\n");
        if (webSearchRequested || webSearchUsed) {
            context.append("- 联网搜索规则：联网结果只作为外部参考；如果引用或推荐网站，必须使用检索上下文里的 W 编号，并让推荐网站与回答结论一一对应；当前平台官方模型、指标和路径仍以站内知识库为准。\n");
        }
        if (!relatedFound) {
            context.append("- 如果站内没有相关文章，直接说明当前知识库未匹配到专门文章。\n");
        }
        context.append("\n");
        return context.toString();
    }

    private void sendEvent(SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data));
        } catch (IOException ignored) {
            // Client may have closed the stream; the async task will complete naturally.
        }
    }

    private String buildMemoryContext(Long userId) {
        StringBuilder ctx = new StringBuilder("[用户相关记忆]\n");
        try {
            List<Map<String, Object>> memories = sessionService.getMemories(userId);
            memories.stream()
                .filter(m -> {
                    Object imp = m.get("importance");
                    float f = imp instanceof Number ? ((Number) imp).floatValue() : 0f;
                    return f > 0.3f;
                })
                .limit(3)
                .forEach(m -> ctx.append("- ").append(m.get("content")).append("\n"));
        } catch (Exception ignored) {
            return "";
        }
        return ctx.length() > "[用户相关记忆]\n".length() ? ctx.toString() : "";
    }

    private String buildKbContext(String retrievalQuery) {
        try {
            var kbDocs = kbRepo.searchByFulltext(retrievalQuery);
            if (kbDocs == null || kbDocs.isEmpty()) return "";
            StringBuilder ctx = new StringBuilder("[相关知识库文档]\n");
            for (KnowledgeDoc doc : kbDocs) {
                String snippet = doc.getContent().length() > 300
                    ? doc.getContent().substring(0, 300) + "..."
                    : doc.getContent();
                ctx.append("--- ").append(doc.getTitle()).append(" ---\n").append(snippet).append("\n\n");
            }
            return ctx.toString();
        } catch (Exception ignored) {
            return "";
        }
    }

    private String enrichMessage(
        String message,
        String reasoningContext,
        String siteContext,
        String memoryContext,
        String kbContext,
        String relatedArticleContext,
        String webContext,
        String workspaceContext
    ) {
        if (siteContext.isEmpty()
            && memoryContext.isEmpty()
            && kbContext.isEmpty()
            && workspaceContext.isEmpty()
            && relatedArticleContext.isEmpty()
            && webContext.isEmpty()
            && reasoningContext.isEmpty()) {
            return message;
        }
        return reasoningContext
            + siteContext
            + memoryContext
            + kbContext
            + relatedArticleContext
            + webContext
            + workspaceContext
            + "[用户问题]\n"
            + message;
    }

    private void saveAssistantTurn(
        Long convId,
        String userMessage,
        String aiRole,
        String aiReply,
        String reasoningLevel,
        boolean webSearchAttempted,
        List<Map<String, Object>> webResults,
        String retrievalQuery,
        Map<String, Object> reasoningDiagnostics,
        Object visibleReasoning,
        Map<String, Object> navigation,
        List<Map<String, Object>> relatedArticles,
        Map<String, Object> aiResponse
    ) {
        sessionService.appendMessage(convId, Map.of("role", "user", "content", userMessage));
        Map<String, Object> assistantMessage = new LinkedHashMap<>();
        assistantMessage.put("role", aiRole);
        assistantMessage.put("content", aiReply);
        assistantMessage.put("reasoningLevel", reasoningLevel);
        assistantMessage.put("webSearchAttempted", webSearchAttempted);
        assistantMessage.put("webSearchUsed", webResults != null && !webResults.isEmpty());
        assistantMessage.put("webSearchQuery", retrievalQuery);
        assistantMessage.put("reasoningDiagnostics", reasoningDiagnostics);
        if (aiResponse.containsKey("apiStatus")) assistantMessage.put("apiStatus", aiResponse.get("apiStatus"));
        if (hasContent(visibleReasoning)) assistantMessage.put("reasoning", visibleReasoning);
        if (navigation != null && !navigation.isEmpty()) assistantMessage.put("navigation", navigation);
        if (relatedArticles != null && !relatedArticles.isEmpty()) assistantMessage.put("relatedArticles", relatedArticles);
        if (webResults != null && !webResults.isEmpty()) assistantMessage.put("webResults", webResults);
        sessionService.appendMessage(convId, assistantMessage);
    }

    private boolean shouldUseLocalGreeting(
        String message,
        List<?> attachments,
        List<?> resources,
        boolean deepThink,
        boolean webSearchRequested
    ) {
        String normalized = text(message).trim().toLowerCase(Locale.ROOT)
            .replaceAll("[\\s!！。,.，？?~～]+", "");
        return List.of("你好", "您好", "hello", "hi", "hey").contains(normalized);
    }

    private Result<Map<String, Object>> localGreetingResponse(Long convId, String message, String reasoningLevel) {
        String reply = "你好，我是 DeepInsight AI 助手。你可以问我模型总览、推荐系统模型、接入测试、性能看板、数据中心、知识库或页面跳转入口。";
        Map<String, Object> diagnostics = buildReasoningDiagnostics(
            reasoningLevel,
            false,
            null,
            List.of(),
            false,
            false,
            List.of(),
            message,
            Map.of(
                "provider", "local",
                "model", "DeepInsight local greeting",
                "effectiveModel", "local",
                "nativeReasoning", false,
                "nativeReasoningLabel", "本地快速问候",
                "maxTokens", 0,
                "appliedControls", List.of("simple_greeting_short_circuit")
            ),
            0,
            false,
            false,
            false,
            false
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("conversationId", convId);
        result.put("reply", reply);
        result.put("role", "assistant");
        result.put("memoryUsed", false);
        result.put("kbDocsFound", false);
        result.put("siteContextUsed", false);
        result.put("relatedArticles", List.of());
        result.put("webResults", List.of());
        result.put("webSearchAttempted", false);
        result.put("webSearchUsed", false);
        result.put("webSearchQuery", message);
        result.put("reasoningLevel", reasoningLevel);
        result.put("reasoningDiagnostics", diagnostics);

        sessionService.appendMessage(convId, Map.of("role", "user", "content", message));
        Map<String, Object> assistantMessage = new LinkedHashMap<>();
        assistantMessage.put("role", "assistant");
        assistantMessage.put("content", reply);
        assistantMessage.put("reasoningLevel", reasoningLevel);
        assistantMessage.put("webSearchAttempted", false);
        assistantMessage.put("webSearchUsed", false);
        assistantMessage.put("webSearchQuery", message);
        assistantMessage.put("reasoningDiagnostics", diagnostics);
        sessionService.appendMessage(convId, assistantMessage);
        return Result.success(result);
    }

    private boolean shouldUseLocalKnowledgeResponse(
        String message,
        List<?> attachments,
        List<?> resources,
        boolean deepThink,
        boolean webSearchRequested,
        List<Map<String, Object>> relatedArticles,
        Map<String, Object> navigation
    ) {
        if (deepThink || webSearchRequested) return false;
        if ((attachments != null && !attachments.isEmpty()) || (resources != null && !resources.isEmpty())) return false;
        String normalized = text(message).toLowerCase(Locale.ROOT);
        String compact = normalized.replaceAll("\\s+", "");
        boolean platformIntent = List.of(
            "deepinsight", "bsarec", "bert4rec", "duorec", "fearec", "fmlp", "recbole", "sasrec", "tisasrec",
            "推荐", "模型", "数据集", "指标", "hr@", "ndcg", "mrr", "top-k", "topk",
            "可视化", "比对", "接入", "训练", "知识库", "页面", "入口", "性能看板", "数据中心"
        ).stream().anyMatch(compact::contains);
        boolean navigationIntent = navigation != null && !navigation.isEmpty()
            && List.of("打开", "跳转", "在哪", "入口", "页面", "去").stream().anyMatch(compact::contains);
        return platformIntent || navigationIntent || (relatedArticles != null && !relatedArticles.isEmpty() && compact.length() <= 60);
    }

    private Result<Map<String, Object>> localKnowledgeResponse(
        Long convId,
        String message,
        String retrievalQuery,
        String reasoningLevel,
        List<Map<String, Object>> relatedArticles,
        Map<String, Object> navigation
    ) {
        String reply = buildLocalKnowledgeReply(message, relatedArticles, navigation);
        Map<String, Object> diagnostics = buildReasoningDiagnostics(
            reasoningLevel,
            false,
            null,
            relatedArticles,
            false,
            false,
            List.of(),
            retrievalQuery,
            Map.of(
                "provider", "local",
                "model", "DeepInsight trained site knowledge",
                "effectiveModel", "local-rag",
                "nativeReasoning", false,
                "nativeReasoningLabel", "本地站内知识快速回答",
                "maxTokens", 0,
                "appliedControls", List.of("trained_site_knowledge_fast_path")
            ),
            0,
            false,
            relatedArticles != null && !relatedArticles.isEmpty(),
            true,
            false
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("conversationId", convId);
        result.put("reply", reply);
        result.put("role", "assistant");
        result.put("memoryUsed", false);
        result.put("kbDocsFound", relatedArticles != null && relatedArticles.stream().anyMatch(item -> "knowledge_doc".equals(text(item.get("source")))));
        result.put("siteContextUsed", true);
        result.put("relatedArticles", relatedArticles == null ? List.of() : relatedArticles);
        result.put("webResults", List.of());
        result.put("webSearchAttempted", false);
        result.put("webSearchUsed", false);
        result.put("webSearchQuery", retrievalQuery);
        result.put("reasoningLevel", reasoningLevel);
        result.put("reasoningDiagnostics", diagnostics);
        if (navigation != null && !navigation.isEmpty()) result.put("navigation", navigation);

        sessionService.appendMessage(convId, Map.of("role", "user", "content", message));
        Map<String, Object> assistantMessage = new LinkedHashMap<>();
        assistantMessage.put("role", "assistant");
        assistantMessage.put("content", reply);
        assistantMessage.put("reasoningLevel", reasoningLevel);
        assistantMessage.put("webSearchAttempted", false);
        assistantMessage.put("webSearchUsed", false);
        assistantMessage.put("webSearchQuery", retrievalQuery);
        assistantMessage.put("reasoningDiagnostics", diagnostics);
        if (navigation != null && !navigation.isEmpty()) assistantMessage.put("navigation", navigation);
        if (relatedArticles != null && !relatedArticles.isEmpty()) assistantMessage.put("relatedArticles", relatedArticles);
        sessionService.appendMessage(convId, assistantMessage);
        return Result.success(result);
    }

    private String buildLocalKnowledgeReply(
        String message,
        List<Map<String, Object>> relatedArticles,
        Map<String, Object> navigation
    ) {
        List<Map<String, Object>> models = matchedOfficialModels(message);
        boolean overview = isModelOverviewQuestion(message);
        StringBuilder reply = new StringBuilder();
        reply.append("按 DeepInsight 已训练的站内知识回答：\n\n");

        if (!models.isEmpty()) {
            if (overview && models.size() >= 9) {
                reply.append("当前官方模型共 9 个，全部定位为推荐系统模型：");
                reply.append(models.stream().map(model -> text(model.get("name"))).toList());
                reply.append("\n\n");
            } else {
                reply.append("匹配到的模型：\n");
            }
            for (Map<String, Object> model : models.stream().limit(overview ? 9 : 4).toList()) {
                appendModelFact(reply, model, overview);
            }
        } else if (navigation != null && !navigation.isEmpty()) {
            reply.append("这个问题更像页面入口或操作路径问题。");
            reply.append("可直接进入「").append(text(navigation.get("label"))).append("」");
            if (hasContent(navigation.get("path"))) reply.append("（").append(text(navigation.get("path"))).append("）");
            reply.append("。\n");
        } else {
            reply.append("站内知识库已接入推荐系统模型、数据集、指标口径、页面功能和知识文章。");
            reply.append("你可以继续指定某个模型，例如 BSARec、BERT4Rec、DuoRec、FEARec、FMLP-Rec、RecBole、SASRec 或 TiSASRec。\n");
        }

        if (relatedArticles != null && !relatedArticles.isEmpty()) {
            reply.append("\n相关站内内容：\n");
            for (Map<String, Object> article : relatedArticles.stream().limit(3).toList()) {
                reply.append("- ")
                    .append(text(article.get("sourceLabel"))).append("《")
                    .append(text(article.get("title"))).append("》：")
                    .append(trimText(text(article.get("snippet")), 120))
                    .append("\n");
            }
        }

        if (navigation != null && !navigation.isEmpty() && !text(navigation.get("path")).isBlank()) {
            reply.append("\n可操作入口：")
                .append(text(navigation.get("label")))
                .append(" -> ")
                .append(text(navigation.get("path")))
                .append("。");
        }
        return reply.toString().trim();
    }

    private List<Map<String, Object>> matchedOfficialModels(String message) {
        String compact = text(message).toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
        boolean overview = isModelOverviewQuestion(message);
        List<Map<String, Object>> all = modelCatalogService.listModels();
        if (overview) return all;
        return all.stream()
            .filter(model -> modelMatches(compact, model))
            .toList();
    }

    private boolean isModelOverviewQuestion(String message) {
        String compact = text(message).toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
        boolean asksModels = compact.contains("模型") || compact.contains("model") || compact.contains("推荐系统");
        boolean asksAll = compact.contains("全部") || compact.contains("所有") || compact.contains("总共")
            || compact.contains("几个") || compact.contains("哪些") || compact.contains("清单") || compact.contains("overview");
        return asksModels && asksAll;
    }

    private boolean modelMatches(String compactQuery, Map<String, Object> model) {
        List<String> tokens = List.of(
            text(model.get("id")),
            text(model.get("name")),
            text(model.get("displayNameZh")),
            text(model.get("architecture")),
            text(model.get("modelGroup"))
        );
        for (String token : tokens) {
            String compactToken = token.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
            if (!compactToken.isBlank() && compactQuery.contains(compactToken)) return true;
        }
        return false;
    }

    private void appendModelFact(StringBuilder reply, Map<String, Object> model, boolean compact) {
        reply.append("- ").append(text(model.get("name")));
        String display = text(model.get("displayNameZh"));
        if (!display.isBlank() && !display.equals(text(model.get("name")))) reply.append("（").append(display).append("）");
        reply.append("：").append(text(model.get("statusLabel"))).append("。");
        Map<?, ?> dataset = asMap(model.get("datasetSummary"));
        Map<?, ?> metrics = asMap(model.get("metrics"));
        if (!dataset.isEmpty()) {
            reply.append("数据集 ").append(firstValue(dataset, "Dataset", "Data source"));
            appendOptionalFact(reply, "用户", firstValue(dataset, "User scale"));
            appendOptionalFact(reply, "物品", firstValue(dataset, "Item scale"));
            appendOptionalFact(reply, "交互", firstValue(dataset, "Interactions"));
            reply.append("。");
        }
        if (!metrics.isEmpty()) {
            if (hasContent(metrics.get("指标状态"))) {
                reply.append(text(metrics.get("指标状态"))).append("。");
            } else {
                reply.append("指标 ");
                reply.append(metricBrief(metrics));
                reply.append("。");
            }
        }
        if (!compact && hasContent(model.get("statusReason"))) {
            reply.append("说明：").append(text(model.get("statusReason")));
        }
        reply.append("\n");
    }

    private void appendOptionalFact(StringBuilder reply, String label, String value) {
        if (value == null || value.isBlank()) return;
        reply.append("，").append(label).append(" ").append(value);
    }

    private String firstValue(Map<?, ?> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (hasContent(value)) return text(value);
        }
        return "";
    }

    private String metricBrief(Map<?, ?> metrics) {
        List<String> preferred = List.of("HR@10", "NDCG@10", "HR@20", "NDCG@20", "MRR");
        List<String> parts = new ArrayList<>();
        for (String key : preferred) {
            if (hasContent(metrics.get(key))) parts.add(key + "=" + text(metrics.get(key)));
        }
        if (parts.isEmpty()) {
            for (Map.Entry<?, ?> entry : metrics.entrySet()) {
                if (parts.size() >= 3) break;
                if (hasContent(entry.getValue())) parts.add(entry.getKey() + "=" + entry.getValue());
            }
        }
        return parts.isEmpty() ? "当前站内未记录可读指标" : String.join("，", parts);
    }

    private Map<?, ?> asMap(Object value) {
        return value instanceof Map<?, ?> map ? map : Map.of();
    }

    private String trimText(String value, int maxChars) {
        String normalized = text(value).replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxChars) return normalized;
        return normalized.substring(0, Math.max(0, maxChars - 3)) + "...";
    }

    private List<Map<String, String>> toChatHistory(List<Map<String, Object>> storedHistory) {
        if (storedHistory == null || storedHistory.isEmpty()) return List.of();
        List<Map<String, String>> history = new ArrayList<>();
        for (Map<String, Object> item : storedHistory) {
            String role = text(item.get("role"));
            String content = text(item.get("content"));
            if (role.isBlank() || content.isBlank()) continue;
            history.add(Map.of(
                "role", "user".equals(role) ? "user" : "assistant",
                "content", content
            ));
        }
        return history;
    }

    private String buildRetrievalQuery(String message, List<Map<String, Object>> storedHistory) {
        String safeMessage = text(message).replaceAll("\\s+", " ").trim();
        if (safeMessage.isBlank() || storedHistory == null || storedHistory.isEmpty()) return safeMessage;
        if (!needsContextualRetrieval(safeMessage)) return safeMessage;

        LinkedHashSet<String> anchors = new LinkedHashSet<>();
        collectSearchAnchors(anchors, safeMessage);
        for (int i = storedHistory.size() - 1; i >= 0 && anchors.size() < 10; i--) {
            Map<String, Object> item = storedHistory.get(i);
            String role = text(item.get("role"));
            if (role.isBlank()) continue;
            collectSearchAnchors(anchors, text(item.get("content")));
            collectSearchAnchors(anchors, text(item.get("webSearchQuery")));
            collectArticleAnchors(anchors, item.get("relatedArticles"));
            collectWebResultAnchors(anchors, item.get("webResults"));
        }

        if (anchors.isEmpty()) return safeMessage;
        String intent = retrievalIntent(safeMessage);
        return String.join(" ", anchors.stream().limit(8).toList()) + (intent.isBlank() ? "" : " " + intent);
    }

    private boolean needsContextualRetrieval(String message) {
        String normalized = message.toLowerCase(Locale.ROOT);
        if (SEARCH_CONTEXT_ANCHORS.stream().anyMatch(anchor -> normalized.contains(anchor.toLowerCase(Locale.ROOT)))) {
            return false;
        }
        if (TECH_TOKEN_PATTERN.matcher(message).find()) return false;
        String compact = message.replaceAll("\\s+", "");
        if (compact.length() <= 28) return true;
        return compact.contains("\u8fd9\u4e2a\u6a21\u578b")
            || compact.contains("\u8fd9\u4e2a\u6570\u636e\u96c6")
            || compact.contains("\u8fd9\u4e2a")
            || compact.contains("\u4e0e\u8fd9\u4e2a")
            || compact.contains("\u548c\u8fd9\u4e2a")
            || compact.contains("\u8ddf\u8fd9\u4e2a")
            || compact.contains("\u6211\u8bf4\u7684\u662f")
            || compact.contains("\u4e0d\u662f\u64cd\u4f5c")
            || compact.contains("\u6709\u5173\u7684");
    }

    private void collectSearchAnchors(LinkedHashSet<String> anchors, String text) {
        if (text == null || text.isBlank()) return;
        String normalized = text.toLowerCase(Locale.ROOT);
        for (String anchor : SEARCH_CONTEXT_ANCHORS) {
            if (normalized.contains(anchor.toLowerCase(Locale.ROOT))) anchors.add(anchor);
        }
        Matcher matcher = TECH_TOKEN_PATTERN.matcher(text);
        while (matcher.find() && anchors.size() < 12) {
            String token = matcher.group();
            if (token.length() >= 4 && !token.equalsIgnoreCase("api-v1")) anchors.add(token);
        }
    }

    private void collectArticleAnchors(LinkedHashSet<String> anchors, Object relatedArticles) {
        if (!(relatedArticles instanceof List<?> list)) return;
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> map)) continue;
            collectSearchAnchors(anchors, text(map.get("title")) + " " + text(map.get("snippet")));
            if (anchors.size() >= 12) return;
        }
    }

    private void collectWebResultAnchors(LinkedHashSet<String> anchors, Object webResults) {
        if (!(webResults instanceof List<?> list)) return;
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> map)) continue;
            collectSearchAnchors(anchors, text(map.get("title")) + " " + text(map.get("snippet")));
            if (anchors.size() >= 12) return;
        }
    }

    private String retrievalIntent(String message) {
        String compact = text(message).replaceAll("\\s+", "");
        if ((compact.contains("\u7c7b\u4f3c") && compact.contains("\u7f51\u7ad9"))
            || compact.contains("\u540c\u7c7b\u7f51\u7ad9")
            || compact.contains("\u53c2\u8003\u7f51\u7ad9")
            || compact.contains("\u7ade\u54c1")
            || compact.contains("\u66ff\u4ee3")) {
            return "\u63a8\u8350\u7cfb\u7edf \u6a21\u578b\u8bc4\u4f30 \u6570\u636e\u96c6\u53ef\u89c6\u5316 \u540c\u7c7b\u5e73\u53f0 \u7c7b\u4f3c\u7f51\u7ad9 alternatives";
        }
        if (compact.contains("\u6a21\u578b")) return "\u63a8\u8350\u7cfb\u7edf \u6a21\u578b \u63a5\u5165\u72b6\u6001 \u63a8\u8350\u6307\u6807";
        if (compact.contains("\u6570\u636e\u96c6")) return "\u63a8\u8350\u6570\u636e \u7528\u6237\u6570 \u7269\u54c1\u6570 \u4ea4\u4e92\u6570 \u5e8f\u5217\u957f\u5ea6";
        if (compact.contains("\u547d\u4e2d\u7387") || compact.contains("HR") || compact.contains("NDCG") || compact.contains("Top")) return "HR@K NDCG@K MRR Top-K recommendation evaluation";
        return "\u76f8\u5173\u63a8\u8350\u6a21\u578b \u5e73\u53f0\u77e5\u8bc6";
    }

    private String buildRelatedArticleContext(List<Map<String, Object>> relatedArticles) {
        if (relatedArticles == null || relatedArticles.isEmpty()) return "";
        StringBuilder context = new StringBuilder("[可推荐给用户的站内相关文章]\n");
        int index = 1;
        for (Map<String, Object> article : relatedArticles.stream().limit(6).toList()) {
            context.append(index++).append(". ")
                .append(text(article.get("sourceLabel"))).append("《")
                .append(text(article.get("title"))).append("》")
                .append(" | 路径：").append(text(article.get("path"))).append("\n")
                .append("   摘要：").append(text(article.get("snippet"))).append("\n");
        }
        context.append("\n");
        return context.toString();
    }

    private String buildVisibleReasoningSummary(
        String level,
        List<Map<String, Object>> relatedArticles,
        List<Map<String, Object>> webResults,
        Map<String, Object> navigation
    ) {
        String normalized = normalizeReasoning(level);
        StringBuilder summary = new StringBuilder();
        summary.append("分析强度：").append(reasoningLabel(normalized)).append("\n");
        summary.append("处理方式：").append(reasoningMethod(normalized)).append("\n");
        summary.append("站内知识：匹配到 ").append(relatedArticles == null ? 0 : relatedArticles.size()).append(" 条相关文章/知识来源。\n");
        summary.append("联网搜索：").append(webResults == null || webResults.isEmpty() ? "未使用或未返回结果。" : "返回 " + webResults.size() + " 条外部参考。").append("\n");
        summary.append("跳转判断：").append(navigation == null || navigation.isEmpty()
            ? "本轮没有强行给页面入口。"
            : "给出「" + text(navigation.get("label")) + "」入口，原因：" + text(navigation.get("reason"))).append("\n");
        return summary.toString();
    }

    private Map<String, Object> buildReasoningDiagnostics(
        String level,
        boolean deepThink,
        Double temperature,
        List<Map<String, Object>> relatedArticles,
        boolean webSearchRequested,
        boolean webSearchAttempted,
        List<Map<String, Object>> webResults,
        String retrievalQuery,
        Object modelDiagnostics,
        int attachmentCount,
        boolean memoryUsed,
        boolean kbDocsFound,
        boolean siteContextUsed,
        boolean workspaceUsed
    ) {
        String normalized = normalizeReasoning(level);
        Map<String, Object> diagnostics = new LinkedHashMap<>();
        diagnostics.put("level", normalized);
        diagnostics.put("label", reasoningLabel(normalized));
        diagnostics.put("enabled", deepThink && !"off".equals(normalized));
        diagnostics.put("strategy", reasoningMethod(normalized));
        if (temperature != null) diagnostics.put("temperature", temperature);

        Map<String, Object> related = new LinkedHashMap<>();
        related.put("matched", relatedArticles == null ? 0 : relatedArticles.size());
        related.put("limit", relatedLimit(normalized));
        diagnostics.put("related", related);

        Map<String, Object> web = new LinkedHashMap<>();
        web.put("requested", webSearchRequested);
        web.put("attempted", webSearchAttempted);
        web.put("returned", webResults == null ? 0 : webResults.size());
        web.put("limit", webSearchLimit(normalized));
        web.put("query", retrievalQuery);
        diagnostics.put("web", web);

        diagnostics.put("attachments", Map.of("count", Math.max(0, attachmentCount)));
        diagnostics.put("context", Map.of(
            "memoryUsed", memoryUsed,
            "kbDocsFound", kbDocsFound,
            "siteContextUsed", siteContextUsed,
            "workspaceUsed", workspaceUsed
        ));

        if (modelDiagnostics instanceof Map<?, ?> map) {
            Map<String, Object> model = new LinkedHashMap<>();
            map.forEach((key, value) -> model.put(String.valueOf(key), value));
            diagnostics.put("model", model);
        }
        return diagnostics;
    }

    private String reasoningMethod(String level) {
        return switch (level) {
            case "off" -> "直接回答，不做多源扩展。";
            case "quick" -> "快速匹配页面、模型名和最短操作路径。";
            case "low" -> "结合站内知识和当前页面功能给出常规建议。";
            case "deep" -> "综合模型清单、知识文章、指标口径和可选页面入口。";
            case "max" -> "多源复核站内知识、论坛文章、联网结果和页面下一步。";
            default -> "标准站内问答策略。";
        };
    }

    private String reasoningLabel(String level) {
        return switch (level) {
            case "off" -> "直答";
            case "quick" -> "快速";
            case "low" -> "标准";
            case "deep" -> "深度";
            case "max" -> "极限";
            default -> level;
        };
    }

    private int relatedLimit(String level) {
        return switch (normalizeReasoning(level)) {
            case "off" -> 0;
            case "quick" -> 2;
            case "deep" -> 6;
            case "max" -> 8;
            default -> 5;
        };
    }

    private int webSearchLimit(String level) {
        return switch (normalizeReasoning(level)) {
            case "off" -> 4;
            case "quick" -> 6;
            case "deep" -> 12;
            case "max" -> 15;
            default -> 10;
        };
    }

    private boolean isDirectReasoning(String level) {
        return "off".equals(normalizeReasoning(level));
    }

    private boolean isQuickReasoning(String level) {
        return "quick".equals(normalizeReasoning(level));
    }

    private String normalizeReasoning(String level) {
        if (level == null || level.isBlank()) return "low";
        return switch (level.toLowerCase(Locale.ROOT)) {
            case "off", "none" -> "off";
            case "quick", "minimal" -> "quick";
            case "low" -> "low";
            case "deep", "high" -> "deep";
            case "max", "xhigh" -> "max";
            default -> "low";
        };
    }

    private boolean hasContent(Object value) {
        return value != null && !String.valueOf(value).isBlank();
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    @GetMapping("/conversations")
    public Result<List<Map<String, Object>>> conversations(Principal principal) {
        Long userId = requireUserId(principal);
        return Result.success(sessionService.getUserConversations(userId));
    }

    @GetMapping("/conversations/{id}/messages")
    public Result<List<Map<String, Object>>> messages(@PathVariable Long id, Principal principal) {
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

    @GetMapping("/knowledge/train/status")
    public Result<KnowledgeTrainingService.TrainingStatus> knowledgeTrainingStatus(Principal principal) {
        if (!isAdmin(principal)) return Result.error(403, "仅管理员可查看 AI 知识训练状态");
        return Result.success(knowledgeTrainingService.status());
    }

    @PostMapping("/knowledge/train")
    public Result<KnowledgeTrainingService.TrainingReport> trainKnowledge(Principal principal) {
        if (!isAdmin(principal)) return Result.error(403, "仅管理员可训练 AI 知识库");
        return Result.success("AI 知识训练已完成", knowledgeTrainingService.rebuildIndex());
    }

    @PostMapping("/knowledge")
    public Result<KnowledgeDoc> addKnowledge(@RequestBody KnowledgeDoc doc, Principal principal) {
        if (!isAdmin(principal)) return Result.error(403, "仅管理员可维护知识库");
        return Result.success("已添加", kbRepo.save(doc));
    }

    @DeleteMapping("/knowledge/{id}")
    public Result<String> deleteKnowledge(@PathVariable Long id, Principal principal) {
        if (!isAdmin(principal)) return Result.error(403, "仅管理员可维护知识库");
        kbRepo.deleteById(id);
        return Result.success("已删除");
    }

    private Double boundedTemperature(Object value) {
        if (!(value instanceof Number n)) return null;
        double raw = n.doubleValue();
        if (!Double.isFinite(raw)) return null;
        return Math.max(0.0d, Math.min(1.0d, raw));
    }

    private Long parseLong(Object value) {
        if (value == null) return null;
        try {
            Long parsed = Long.valueOf(String.valueOf(value));
            return parsed > 0 ? parsed : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String safeConversationTitle(String message) {
        String normalized = text(message).replaceAll("\\s+", " ").trim();
        if (normalized.isBlank()) return "新对话";
        return normalized.length() > 30 ? normalized.substring(0, 30) + "..." : normalized;
    }
}
