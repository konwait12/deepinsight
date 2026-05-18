package com.deepinsight.backend.service;

import com.deepinsight.backend.entity.AiConfig;
import java.util.List;
import java.util.Map;

/**
 * AI配置服务接口
 */
public interface AiConfigService {
    String MASKED_API_KEY = "__KEEP_EXISTING_SECRET__";

    record ChatOptions(
        boolean deepThink,
        String reasoningLevel,
        Double temperature,
        List<Map<String, Object>> visionInputs
    ) {
        public ChatOptions(boolean deepThink, String reasoningLevel, Double temperature) {
            this(deepThink, reasoningLevel, temperature, List.of());
        }

        public ChatOptions {
            visionInputs = visionInputs == null ? List.of() : List.copyOf(visionInputs);
        }
    }

    List<AiConfig> findAll();
    AiConfig getActive();
    AiConfig getById(Long id);
    AiConfig save(AiConfig config);
    AiConfig activate(Long id);
    void delete(Long id);
    AiConfig sanitize(AiConfig config);

    default Map<String, Object> chat(String message, List<Map<String, String>> history, boolean deepThink) {
        return chat(message, history, new ChatOptions(deepThink, deepThink ? "high" : "off", null));
    }

    Map<String, Object> chat(String message, List<Map<String, String>> history, ChatOptions options);
}
