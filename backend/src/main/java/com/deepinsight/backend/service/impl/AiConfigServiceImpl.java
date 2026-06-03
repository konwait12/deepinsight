package com.deepinsight.backend.service.impl;

import com.deepinsight.backend.entity.AiConfig;
import com.deepinsight.backend.repository.AiConfigRepository;
import com.deepinsight.backend.service.AiConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.regex.Pattern;

/**
 * AI配置服务实现
 * <p>
 * 管理AI模型配置，支持Ollama本地模型和OpenAI/Gemini API。
 * 使用RestTemplate调用各模型API，统一返回格式。
 */
@Service
@RequiredArgsConstructor
public class AiConfigServiceImpl implements AiConfigService {

    private final AiConfigRepository repository;
    private final RestTemplate restTemplate = new RestTemplate();
    private static final Pattern MASKED_KEY_PATTERN = Pattern.compile("^.{0,8}\\*{3,}.{0,8}$");

    @Value("${DEEPSEEK_API_KEY:}")
    private String deepSeekApiKey;

    @Value("${DEEPSEEK_API_URL:https://api.deepseek.com}")
    private String deepSeekApiUrl;

    @Value("${DEEPSEEK_MODEL:deepseek-v4-pro}")
    private String deepSeekModel;

    @Override
    public List<AiConfig> findAll() {
        return repository.findAll().stream().map(this::sanitize).toList();
    }

    @Override
    public AiConfig getActive() {
        return findActiveConfig().map(this::sanitize).orElse(null);
    }

    @Override
    public AiConfig getById(Long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("AI配置不存在"));
    }

    @Override
    @Transactional
    public AiConfig save(AiConfig config) {
        preserveExistingSecret(config);
        // 如果设为激活，先取消其他激活
        if (Boolean.TRUE.equals(config.getIsActive())) {
            deactivateAll();
        }
        return sanitize(repository.save(config));
    }

    @Override
    @Transactional
    public AiConfig activate(Long id) {
        deactivateAll();
        AiConfig config = getById(id);
        config.setIsActive(true);
        return sanitize(repository.save(config));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    private void deactivateAll() {
        repository.findAll().forEach(c -> {
            c.setIsActive(false);
            repository.save(c);
        });
    }

    @Override
    public AiConfig sanitize(AiConfig config) {
        if (config == null) return null;
        return AiConfig.builder()
            .id(config.getId())
            .name(config.getName())
            .modelType(config.getModelType())
            .modelName(config.getModelName())
            .apiUrl(config.getApiUrl())
            .apiKey(maskApiKey(config.getApiKey()))
            .systemPrompt(config.getSystemPrompt())
            .temperature(config.getTemperature())
            .maxTokens(config.getMaxTokens())
            .contextWindow(config.getContextWindow())
            .isActive(config.getIsActive())
            .knowledgeBase(config.getKnowledgeBase())
            .createdAt(config.getCreatedAt())
            .updatedAt(config.getUpdatedAt())
            .build();
    }

    private void preserveExistingSecret(AiConfig config) {
        if (config == null || config.getId() == null) return;
        repository.findById(config.getId()).ifPresent(existing -> {
            if (shouldKeepExistingSecret(config.getApiKey())) {
                config.setApiKey(existing.getApiKey());
            }
            if (config.getCreatedAt() == null) {
                config.setCreatedAt(existing.getCreatedAt());
            }
        });
    }

    private boolean shouldKeepExistingSecret(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) return true;
        if (AiConfigService.MASKED_API_KEY.equals(apiKey)) return true;
        return MASKED_KEY_PATTERN.matcher(apiKey).matches();
    }

    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) return "";
        int length = apiKey.length();
        String prefix = apiKey.substring(0, Math.min(6, length));
        String suffix = length > 10 ? apiKey.substring(length - 4) : "";
        return prefix + "******" + suffix;
    }

    /**
     * AI对话接口
     * <p>
     * 根据当前激活的AI配置，调用对应模型API进行对话。
     */
    @Override
    public Map<String, Object> chat(String message, List<Map<String, String>> history, ChatOptions options) {
        AiConfig config = findActiveConfig().orElse(null);
        if (config == null) {
            return Map.of("role", "assistant", "content", "AI助手尚未配置，请在管理员控制台中设置。");
        }

        ChatOptions effectiveOptions = options == null ? new ChatOptions(false, "off", null) : options;
        boolean deepThink = effectiveOptions.deepThink() && !"off".equalsIgnoreCase(effectiveOptions.reasoningLevel());

        try {
            Map<String, Object> result;
            if ("ollama".equalsIgnoreCase(config.getModelType())) {
                result = chatOllama(config, message, history, effectiveOptions);
            } else if ("gemini".equalsIgnoreCase(config.getModelType())) {
                result = chatGemini(config, message, history, effectiveOptions);
            } else {
                // openai-compatible (DeepSeek/GPT)
                result = chatOpenAI(config, message, history, effectiveOptions);
            }

            // DeepThink模式: 解析 reasoning_content
            if (deepThink && result.containsKey("reasoning")) {
                return result;
            }
            return result;
        } catch (Exception e) {
            return Map.of("role", "assistant", "content", "AI调用失败: " + e.getMessage());
        }
    }

    /** Ollama本地模型调用 */
    private Map<String, Object> chatOllama(AiConfig config, String message, List<Map<String, String>> history, ChatOptions options) {
        String url = (config.getApiUrl() != null ? config.getApiUrl() : "http://localhost:11434") + "/api/chat";
        List<Map<String, Object>> messages = buildMessages(config, message, history, options, false);

        Map<String, Object> body = new HashMap<>();
        body.put("model", config.getModelName());
        body.put("messages", messages);
        body.put("stream", false);
        Map<String, Object> ollamaOptions = new LinkedHashMap<>();
        ollamaOptions.put("temperature", options.temperature() != null ? options.temperature() : config.getTemperature());
        if (options.deepThink()) {
            ollamaOptions.put("num_predict", tokensForReasoning(config, options));
            ollamaOptions.put("reasoning_level", normalizeReasoningEffort(options.reasoningLevel()));
        }
        body.put("options", ollamaOptions);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> resp = restTemplate.postForEntity(url, new HttpEntity<>(body, headers), Map.class);

        Map<String, Object> result = resp.getBody();
        if (result != null && result.containsKey("message")) {
            return (Map<String, Object>) result.get("message");
        }
        return Map.of("role", "assistant", "content", "Ollama返回异常");
    }

    /** OpenAI兼容API调用 (DeepSeek/GPT/等) */
    private Map<String, Object> chatOpenAI(AiConfig config, String message, List<Map<String, String>> history, ChatOptions options) {
        String baseUrl = config.getApiUrl() != null ? config.getApiUrl() : "https://api.openai.com/v1";
        String url = baseUrl.endsWith("/chat/completions") ? baseUrl : baseUrl + "/chat/completions";

        // DeepThink: switch to deepseek-reasoner model
        boolean deepThink = options.deepThink() && !"off".equalsIgnoreCase(options.reasoningLevel());
        String model = resolveChatModel(config, baseUrl, deepThink);
        boolean deepSeek = isDeepSeekEndpoint(baseUrl, model);
        List<Map<String, Object>> messages = buildMessages(config, message, history, options, supportsVisionInput(baseUrl, model));

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        if (!isReasoningModel(model)) {
            body.put("temperature", options.temperature() != null ? options.temperature() : config.getTemperature() != null ? config.getTemperature() : 0.7);
        }
        body.put("max_tokens", tokensForReasoning(config, options));
        if (deepThink && deepSeek && supportsDeepSeekThinking(model)) {
            body.put("thinking", Map.of("type", "enabled"));
            body.put("reasoning_effort", normalizeDeepSeekReasoningEffort(options.reasoningLevel()));
        } else if (deepThink && supportsReasoningEffort(baseUrl, model)) {
            body.put("reasoning_effort", normalizeReasoningEffort(options.reasoningLevel()));
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(config.getApiKey());

        try {
            ResponseEntity<String> resp = restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                Map<String, Object> result = mapper.readValue(resp.getBody(), Map.class);
                if (result.containsKey("choices")) {
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) result.get("choices");
                    if (!choices.isEmpty()) {
                        Map<String, Object> choice = choices.get(0);
                        Map<String, Object> msg = (Map<String, Object>) choice.get("message");
                        if (deepThink && msg.containsKey("reasoning_content")) {
                            Map<String, Object> enriched = new java.util.LinkedHashMap<>(msg);
                            enriched.put("reasoning", msg.get("reasoning_content"));
                            return enriched;
                        }
                        return msg;
                    }
                }
                return Map.of("role", "assistant", "content", "API返回格式异常");
            }
            return Map.of("role", "assistant", "content", "API错误 HTTP " + resp.getStatusCodeValue());
        } catch (Exception e) {
            return Map.of("role", "assistant", "content", "API调用失败: " + e.getMessage());
        }
    }

    /** Gemini API调用 */
    private Map<String, Object> chatGemini(AiConfig config, String message, List<Map<String, String>> history, ChatOptions options) {
        String url = (config.getApiUrl() != null ? config.getApiUrl() : "https://generativelanguage.googleapis.com/v1beta/models/") + config.getModelName() + ":generateContent?key=" + config.getApiKey();

        // 构建Gemini格式的contents
        List<Map<String, Object>> contents = new ArrayList<>();
        if (config.getSystemPrompt() != null && !config.getSystemPrompt().isBlank()) {
            contents.add(Map.of("role", "user", "parts", List.of(Map.of("text", "[System]\n" + config.getSystemPrompt()))));
            contents.add(Map.of("role", "model", "parts", List.of(Map.of("text", "Understood."))));
        }
        for (Map<String, String> h : history) {
            String role = "user".equals(h.get("role")) ? "user" : "model";
            contents.add(Map.of("role", role, "parts", List.of(Map.of("text", h.get("content")))));
        }
        contents.add(Map.of("role", "user", "parts", buildGeminiParts(message, options.visionInputs())));

        Map<String, Object> body = new HashMap<>();
        body.put("contents", contents);
        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("temperature", options.temperature() != null ? options.temperature() : config.getTemperature() != null ? config.getTemperature() : 0.7);
        generationConfig.put("maxOutputTokens", tokensForReasoning(config, options));
        if (options.deepThink()) {
            generationConfig.put("thinkingConfig", Map.of("thinkingBudget", thinkingBudget(options.reasoningLevel())));
        }
        body.put("generationConfig", generationConfig);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> resp = restTemplate.postForEntity(url, new HttpEntity<>(body, headers), Map.class);

        Map<String, Object> result = resp.getBody();
        if (result != null && result.containsKey("candidates")) {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) result.get("candidates");
            if (!candidates.isEmpty()) {
                Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                if (!parts.isEmpty()) {
                    return Map.of("role", "model", "content", parts.get(0).get("text"));
                }
            }
        }
        return Map.of("role", "assistant", "content", "Gemini返回异常");
    }

    private List<Map<String, Object>> buildGeminiParts(String message, List<Map<String, Object>> visionInputs) {
        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(Map.of("text", message));
        if (visionInputs == null || visionInputs.isEmpty()) return parts;
        for (Map<String, Object> item : visionInputs.stream().limit(8).toList()) {
            String mimeType = String.valueOf(item.getOrDefault("mimeType", "image/png"));
            String data = String.valueOf(item.getOrDefault("data", ""));
            if (data.isBlank()) continue;
            parts.add(Map.of(
                "inlineData",
                Map.of(
                    "mimeType", mimeType,
                    "data", data
                )
            ));
        }
        return parts;
    }

    /** 构建消息列表，包含系统提示词、历史对话和当前消息 */
    private Optional<AiConfig> findActiveConfig() {
        return repository.findByIsActiveTrue().or(this::envDeepSeekConfig);
    }

    private Optional<AiConfig> envDeepSeekConfig() {
        if (deepSeekApiKey == null || deepSeekApiKey.isBlank()) return Optional.empty();
        return Optional.of(AiConfig.builder()
            .name("DeepSeek API (env)")
            .modelType("openai")
            .modelName(blankToDefault(deepSeekModel, "deepseek-v4-pro"))
            .apiUrl(blankToDefault(deepSeekApiUrl, "https://api.deepseek.com"))
            .apiKey(deepSeekApiKey.trim())
            .systemPrompt("你是 DeepInsight 深度学习可视化平台的 AI 助手，请围绕训练诊断、模型分析、预测推理和实验计划给出清晰、可执行的回答。")
            .temperature(0.7)
            .maxTokens(4096)
            .contextWindow(10)
            .isActive(true)
            .build());
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String resolveChatModel(AiConfig config, String baseUrl, boolean deepThink) {
        String model = config.getModelName();
        if (!deepThink) return model;
        String lowerBase = baseUrl == null ? "" : baseUrl.toLowerCase(Locale.ROOT);
        String lowerModel = model == null ? "" : model.toLowerCase(Locale.ROOT);
        if ((lowerBase.contains("deepseek") || lowerModel.contains("deepseek")) && "deepseek-chat".equals(lowerModel)) {
            return "deepseek-reasoner";
        }
        return model;
    }

    private boolean isDeepSeekEndpoint(String baseUrl, String model) {
        String lowerBase = baseUrl == null ? "" : baseUrl.toLowerCase(Locale.ROOT);
        String lowerModel = model == null ? "" : model.toLowerCase(Locale.ROOT);
        return lowerBase.contains("deepseek") || lowerModel.startsWith("deepseek-");
    }

    private boolean supportsDeepSeekThinking(String model) {
        String lowerModel = model == null ? "" : model.toLowerCase(Locale.ROOT);
        return lowerModel.startsWith("deepseek-v4");
    }

    private boolean supportsReasoningEffort(String baseUrl, String model) {
        String lowerBase = baseUrl == null ? "" : baseUrl.toLowerCase(Locale.ROOT);
        String lowerModel = model == null ? "" : model.toLowerCase(Locale.ROOT);
        if (lowerBase.contains("deepseek")) return false;
        return lowerModel.startsWith("gpt-5")
            || lowerModel.startsWith("o1")
            || lowerModel.startsWith("o3")
            || lowerModel.startsWith("o4")
            || lowerModel.contains("reasoning");
    }

    private boolean isReasoningModel(String model) {
        String lowerModel = model == null ? "" : model.toLowerCase(Locale.ROOT);
        return lowerModel.startsWith("gpt-5")
            || lowerModel.startsWith("o1")
            || lowerModel.startsWith("o3")
            || lowerModel.startsWith("o4")
            || lowerModel.contains("reasoner")
            || lowerModel.contains("reasoning");
    }

    private String normalizeReasoningEffort(String level) {
        if (level == null || level.isBlank()) return "medium";
        return switch (level.toLowerCase(Locale.ROOT)) {
            case "off", "none" -> "none";
            case "quick", "minimal" -> "minimal";
            case "low" -> "low";
            case "deep", "high" -> "high";
            case "max", "xhigh" -> "xhigh";
            default -> "medium";
        };
    }

    private String normalizeDeepSeekReasoningEffort(String level) {
        if (level == null || level.isBlank()) return "medium";
        return switch (level.toLowerCase(Locale.ROOT)) {
            case "off", "none", "quick", "minimal", "low" -> "low";
            case "deep", "high", "max", "xhigh" -> "high";
            default -> "medium";
        };
    }

    private int tokensForReasoning(AiConfig config, ChatOptions options) {
        int configured = config.getMaxTokens() != null ? config.getMaxTokens() : 4096;
        if (options == null || !options.deepThink()) return configured;
        return switch (normalizeReasoningEffort(options.reasoningLevel())) {
            case "minimal", "low" -> Math.max(configured, 4096);
            case "high" -> Math.max(configured, 8192);
            case "xhigh" -> Math.max(configured, 12288);
            default -> configured;
        };
    }

    private int thinkingBudget(String level) {
        return switch (normalizeReasoningEffort(level)) {
            case "minimal", "low" -> 1024;
            case "high" -> 4096;
            case "xhigh" -> 8192;
            default -> 2048;
        };
    }

    private boolean supportsVisionInput(String baseUrl, String model) {
        String lowerBase = baseUrl == null ? "" : baseUrl.toLowerCase(Locale.ROOT);
        String lowerModel = model == null ? "" : model.toLowerCase(Locale.ROOT);
        if (lowerBase.contains("deepseek")) return false;
        return lowerModel.contains("gpt-4o")
            || lowerModel.contains("gpt-4.1")
            || lowerModel.contains("gpt-5")
            || lowerModel.contains("vision")
            || lowerModel.contains("omni");
    }

    private List<Map<String, Object>> buildMessages(
        AiConfig config,
        String message,
        List<Map<String, String>> history,
        ChatOptions options,
        boolean includeVision
    ) {
        List<Map<String, Object>> messages = new ArrayList<>();

        // 系统提示词（人设）
        if (config.getSystemPrompt() != null && !config.getSystemPrompt().isBlank()) {
            messages.add(Map.of("role", "system", "content", config.getSystemPrompt()));
        }

        // 历史对话（限制上下文窗口大小）
        int ctxWindow = config.getContextWindow() != null ? config.getContextWindow() : 10;
        if (history != null && !history.isEmpty()) {
            int start = Math.max(0, history.size() - ctxWindow * 2);
            for (int i = start; i < history.size(); i++) {
                Map<String, String> h = history.get(i);
                messages.add(Map.of("role", h.get("role"), "content", h.get("content")));
            }
        }

        if (includeVision && options != null && !options.visionInputs().isEmpty()) {
            messages.add(Map.of("role", "user", "content", buildOpenAiContentParts(message, options.visionInputs())));
        } else {
            messages.add(Map.of("role", "user", "content", message));
        }
        return messages;
    }

    private List<Map<String, Object>> buildOpenAiContentParts(String message, List<Map<String, Object>> visionInputs) {
        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(Map.of("type", "text", "text", message));
        for (Map<String, Object> item : visionInputs.stream().limit(8).toList()) {
            String dataUrl = String.valueOf(item.getOrDefault("dataUrl", ""));
            if (dataUrl.isBlank()) continue;
            parts.add(Map.of(
                "type", "image_url",
                "image_url", Map.of("url", dataUrl)
            ));
        }
        return parts;
    }
}
