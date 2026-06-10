package com.deepinsight.backend.service.impl;

import com.deepinsight.backend.entity.AiConfig;
import com.deepinsight.backend.repository.AiConfigRepository;
import com.deepinsight.backend.service.AiConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiConfigServiceImpl implements AiConfigService {

    private final AiConfigRepository repository;
    private final RestTemplate restTemplate = buildRestTemplate();
    private static final Pattern MASKED_KEY_PATTERN = Pattern.compile("^.{0,8}\\*{3,}.{0,8}$");

    @Value("${DEEPSEEK_API_KEY:}")
    private String deepSeekApiKey;

    @Value("${DEEPSEEK_API_URL:https://api.deepseek.com}")
    private String deepSeekApiUrl;

    @Value("${DEEPSEEK_MODEL:deepseek-chat}")
    private String deepSeekModel;

    private RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15_000);
        factory.setReadTimeout(90_000);
        proxyFromEnv().ifPresent(factory::setProxy);
        return new RestTemplate(factory);
    }

    private Optional<Proxy> proxyFromEnv() {
        String raw = firstNonBlank(
            System.getenv("DEEPINSIGHT_HTTPS_PROXY"),
            System.getenv("HTTPS_PROXY"),
            System.getenv("https_proxy"),
            System.getenv("HTTP_PROXY"),
            System.getenv("http_proxy")
        );
        if (raw == null || raw.isBlank()) return Optional.empty();
        try {
            String normalized = raw.replace("http://", "").replace("https://", "");
            int at = normalized.lastIndexOf('@');
            if (at >= 0) normalized = normalized.substring(at + 1);
            String[] parts = normalized.split(":");
            if (parts.length < 2) return Optional.empty();
            return Optional.of(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(parts[0], Integer.parseInt(parts[1]))));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    @Override
    public List<AiConfig> findAll() {
        return repository.findAll().stream().map(this::sanitize).toList();
    }

    @Override
    public AiConfig getActive() {
        return sanitize(activeConfig());
    }

    @Override
    public AiConfig getById(Long id) {
        return sanitize(repository.findById(id).orElseThrow(() -> new IllegalArgumentException("AI config not found")));
    }

    @Override
    @Transactional
    public AiConfig save(AiConfig config) {
        if (config == null) throw new IllegalArgumentException("AI config is required");
        AiConfig target = config.getId() == null
            ? new AiConfig()
            : repository.findById(config.getId()).orElseThrow(() -> new IllegalArgumentException("AI config not found"));

        String existingKey = target.getApiKey();
        BeanUtils.copyProperties(config, target, "createdAt", "updatedAt");
        if (isMaskedSecret(config.getApiKey()) || MASKED_API_KEY.equals(config.getApiKey())) {
            target.setApiKey(existingKey);
        }
        if (target.getName() == null || target.getName().isBlank()) target.setName("DeepInsight AI");
        if (target.getModelType() == null || target.getModelType().isBlank()) target.setModelType("openai");
        if (target.getModelName() == null || target.getModelName().isBlank()) target.setModelName(defaultModelName(target));
        if (target.getTemperature() == null) target.setTemperature(0.7);
        if (target.getMaxTokens() == null || target.getMaxTokens() <= 0) target.setMaxTokens(4096);
        if (target.getContextWindow() == null || target.getContextWindow() <= 0) target.setContextWindow(10);
        if (target.getIsActive() == null) target.setIsActive(false);
        if (Boolean.TRUE.equals(target.getIsActive())) deactivateOthers(target.getId());
        return sanitize(repository.save(target));
    }

    @Override
    @Transactional
    public AiConfig activate(Long id) {
        AiConfig config = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("AI config not found"));
        repository.findAll().forEach(item -> {
            item.setIsActive(false);
            repository.save(item);
        });
        config.setIsActive(true);
        return sanitize(repository.save(config));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Override
    public AiConfig sanitize(AiConfig config) {
        if (config == null) return null;
        AiConfig copy = new AiConfig();
        BeanUtils.copyProperties(config, copy);
        copy.setApiKey(maskSecret(copy.getApiKey()));
        return copy;
    }

    @Override
    public Map<String, Object> chat(String message, List<Map<String, String>> history, ChatOptions options) {
        AiConfig config = activeConfig();
        try {
            return callProvider(config, message, history == null ? List.of() : history, options == null ? new ChatOptions(false, "off", null) : options, null);
        } catch (Exception error) {
            log.warn("AI provider call failed: {}", error.getMessage());
            return errorResponse(config, error);
        }
    }

    @Override
    public Map<String, Object> chatStream(
        String message,
        List<Map<String, String>> history,
        ChatOptions options,
        ChatStreamSink sink
    ) {
        AiConfig config = activeConfig();
        try {
            if (sink != null) sink.status("正在调用 " + providerLabel(config) + "...");
            Map<String, Object> result = callProvider(config, message, history == null ? List.of() : history, options == null ? new ChatOptions(false, "off", null) : options, sink);
            String reasoning = text(result.get("reasoning"));
            if (!reasoning.isBlank() && sink != null) sink.reasoning(reasoning);
            String content = text(result.get("content"));
            if (!content.isBlank() && sink != null) {
                for (String piece : content.split("(?<=\\G.{24})")) {
                    if (!piece.isBlank()) sink.content(piece);
                }
            }
            return result;
        } catch (Exception error) {
            log.warn("AI provider stream failed: {}", error.getMessage());
            Map<String, Object> result = errorResponse(config, error);
            if (sink != null) sink.content(text(result.get("content")));
            return result;
        }
    }

    private Map<String, Object> callProvider(
        AiConfig config,
        String message,
        List<Map<String, String>> history,
        ChatOptions options,
        ChatStreamSink sink
    ) {
        String type = normalizeType(config.getModelType());
        if ("ollama".equals(type)) return callOllama(config, message, history, options);
        return callOpenAiCompatible(config, message, history, options);
    }

    private Map<String, Object> callOpenAiCompatible(
        AiConfig config,
        String message,
        List<Map<String, String>> history,
        ChatOptions options
    ) {
        String apiUrl = normalizeOpenAiUrl(firstNonBlank(config.getApiUrl(), isDeepSeek(config) ? deepSeekApiUrl : null, "https://api.openai.com/v1"));
        String apiKey = firstNonBlank(config.getApiKey(), isDeepSeek(config) ? deepSeekApiKey : null);
        if (apiKey == null || apiKey.isBlank()) throw new IllegalStateException("AI API key is not configured");

        List<Map<String, Object>> messages = buildMessages(config, message, history);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", defaultModelName(config));
        payload.put("messages", messages);
        payload.put("temperature", temperature(config, options));
        payload.put("max_tokens", maxTokens(config));
        if (usesReasoning(options)) {
            payload.put("reasoning", Map.of("effort", reasoningEffort(options.reasoningLevel())));
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        ResponseEntity<Map> response = restTemplate.exchange(
            apiUrl,
            HttpMethod.POST,
            new HttpEntity<>(payload, headers),
            Map.class
        );

        Map<?, ?> body = response.getBody() == null ? Map.of() : response.getBody();
        String content = extractOpenAiContent(body);
        String reasoning = extractOpenAiReasoning(body);
        Map<String, Object> result = successBase(config);
        result.put("content", content.isBlank() ? "AI 没有返回可显示内容。" : content);
        result.put("reasoning", reasoning);
        result.put("reasoningDiagnostics", Map.of(
            "provider", providerLabel(config),
            "model", defaultModelName(config),
            "nativeReasoning", !reasoning.isBlank()
        ));
        return result;
    }

    private Map<String, Object> callOllama(
        AiConfig config,
        String message,
        List<Map<String, String>> history,
        ChatOptions options
    ) {
        String base = firstNonBlank(config.getApiUrl(), "http://localhost:11434");
        String url = base.replaceAll("/+$", "") + "/api/chat";
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", defaultModelName(config));
        payload.put("messages", buildMessages(config, message, history));
        payload.put("stream", false);
        payload.put("options", Map.of("temperature", temperature(config, options), "num_predict", maxTokens(config)));

        ResponseEntity<Map> response = restTemplate.postForEntity(url, payload, Map.class);
        Map<?, ?> body = response.getBody() == null ? Map.of() : response.getBody();
        Map<?, ?> msg = body.get("message") instanceof Map<?, ?> map ? map : Map.of();
        Map<String, Object> result = successBase(config);
        result.put("content", text(msg.get("content")));
        result.put("reasoning", "");
        result.put("reasoningDiagnostics", Map.of(
            "provider", providerLabel(config),
            "model", defaultModelName(config),
            "nativeReasoning", false
        ));
        return result;
    }

    private List<Map<String, Object>> buildMessages(AiConfig config, String message, List<Map<String, String>> history) {
        List<Map<String, Object>> messages = new ArrayList<>();
        String system = firstNonBlank(config.getSystemPrompt(), defaultSystemPrompt());
        messages.add(Map.of("role", "system", "content", system));
        int keep = Math.max(0, config.getContextWindow() == null ? 10 : config.getContextWindow());
        List<Map<String, String>> tail = history.size() <= keep ? history : history.subList(history.size() - keep, history.size());
        for (Map<String, String> item : tail) {
            String role = "user".equals(item.get("role")) ? "user" : "assistant";
            String content = item.getOrDefault("content", "");
            if (!content.isBlank()) messages.add(Map.of("role", role, "content", content));
        }
        messages.add(Map.of("role", "user", "content", message == null ? "" : message));
        return messages;
    }

    private AiConfig activeConfig() {
        return repository.findByIsActiveTrue().orElseGet(() -> {
            AiConfig config = repository.findAll().stream().findFirst().orElse(null);
            if (config != null) return config;
            AiConfig created = AiConfig.builder()
                .name("DeepSeek API")
                .modelType("openai")
                .modelName(firstNonBlank(deepSeekModel, "deepseek-chat"))
                .apiUrl(deepSeekApiUrl)
                .apiKey(deepSeekApiKey)
                .systemPrompt(defaultSystemPrompt())
                .temperature(0.7)
                .maxTokens(4096)
                .contextWindow(10)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
            return repository.save(created);
        });
    }

    private void deactivateOthers(Long keepId) {
        repository.findAll().forEach(item -> {
            if (keepId == null || !keepId.equals(item.getId())) {
                item.setIsActive(false);
                repository.save(item);
            }
        });
    }

    private Map<String, Object> successBase(AiConfig config) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("role", "assistant");
        result.put("apiStatus", Map.of(
            "ok", true,
            "provider", providerLabel(config),
            "model", defaultModelName(config)
        ));
        return result;
    }

    private Map<String, Object> errorResponse(AiConfig config, Exception error) {
        String message = rootMessage(error);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("role", "assistant");
        result.put("content", "AI 调用失败：" + message);
        result.put("reasoning", "");
        result.put("apiStatus", Map.of(
            "ok", false,
            "provider", providerLabel(config),
            "model", defaultModelName(config),
            "message", message
        ));
        return result;
    }

    private String extractOpenAiContent(Map<?, ?> body) {
        List<?> choices = body.get("choices") instanceof List<?> list ? list : List.of();
        if (choices.isEmpty() || !(choices.get(0) instanceof Map<?, ?> choice)) return "";
        Object message = choice.get("message");
        if (message instanceof Map<?, ?> map) return text(map.get("content"));
        return text(choice.get("text"));
    }

    private String extractOpenAiReasoning(Map<?, ?> body) {
        List<?> choices = body.get("choices") instanceof List<?> list ? list : List.of();
        if (choices.isEmpty() || !(choices.get(0) instanceof Map<?, ?> choice)) return "";
        Object message = choice.get("message");
        if (!(message instanceof Map<?, ?> map)) return "";
        return firstNonBlank(
            text(map.get("reasoning_content")),
            text(map.get("reasoning")),
            text(map.get("reasoningContent"))
        );
    }

    private String normalizeOpenAiUrl(String raw) {
        String base = raw == null || raw.isBlank() ? "https://api.openai.com/v1" : raw.trim();
        if (base.endsWith("/chat/completions")) return base;
        return base.replaceAll("/+$", "") + "/chat/completions";
    }

    private String normalizeType(String type) {
        String value = type == null ? "" : type.toLowerCase(Locale.ROOT);
        if (value.contains("ollama")) return "ollama";
        return "openai";
    }

    private boolean isDeepSeek(AiConfig config) {
        String haystack = (text(config.getName()) + " " + text(config.getModelName()) + " " + text(config.getApiUrl())).toLowerCase(Locale.ROOT);
        return haystack.contains("deepseek");
    }

    private boolean usesReasoning(ChatOptions options) {
        return options != null && options.deepThink() && !"off".equalsIgnoreCase(text(options.reasoningLevel()));
    }

    private String reasoningEffort(String level) {
        return switch (text(level).toLowerCase(Locale.ROOT)) {
            case "quick", "low" -> "low";
            case "max" -> "high";
            default -> "medium";
        };
    }

    private double temperature(AiConfig config, ChatOptions options) {
        Double value = options != null && options.temperature() != null ? options.temperature() : config.getTemperature();
        if (value == null || !Double.isFinite(value)) return 0.7d;
        return Math.max(0.0d, Math.min(1.0d, value));
    }

    private int maxTokens(AiConfig config) {
        return config.getMaxTokens() == null || config.getMaxTokens() <= 0 ? 4096 : config.getMaxTokens();
    }

    private String defaultModelName(AiConfig config) {
        return firstNonBlank(config.getModelName(), isDeepSeek(config) ? deepSeekModel : null, "deepseek-chat");
    }

    private String providerLabel(AiConfig config) {
        if (config == null) return "AI API";
        if ("ollama".equals(normalizeType(config.getModelType()))) return "Ollama";
        if (isDeepSeek(config)) return "DeepSeek";
        return "OpenAI Compatible";
    }

    private String defaultSystemPrompt() {
        return "你是 DeepInsight 的站内 AI。回答时结合推荐系统模型、站内知识、用户数据隔离和当前页面功能。不要编造不存在的指标或服务状态。";
    }

    private boolean isMaskedSecret(String value) {
        return value != null && MASKED_KEY_PATTERN.matcher(value).matches();
    }

    private String maskSecret(String value) {
        if (value == null || value.isBlank()) return "";
        if (value.length() <= 8) return "****";
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }

    private String rootMessage(Exception error) {
        if (error instanceof HttpStatusCodeException http) {
            String body = http.getResponseBodyAsString();
            return http.getStatusCode() + (body == null || body.isBlank() ? "" : " " + truncate(body, 360));
        }
        if (error instanceof ResourceAccessException) return "无法连接到外部 AI API，请检查网络、代理或 API 地址。";
        return firstNonBlank(error.getMessage(), error.getClass().getSimpleName());
    }

    private String truncate(String text, int max) {
        if (text == null || text.length() <= max) return text;
        return text.substring(0, Math.max(0, max - 3)) + "...";
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String firstNonBlank(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return "";
    }
}
