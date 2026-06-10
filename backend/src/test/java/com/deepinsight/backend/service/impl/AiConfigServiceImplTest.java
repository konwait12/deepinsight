package com.deepinsight.backend.service.impl;

import com.deepinsight.backend.entity.AiConfig;
import com.deepinsight.backend.repository.AiConfigRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;

import java.net.UnknownHostException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AiConfigServiceImplTest {

    @Test
    void formatsDeepSeekNetworkFailureWithoutLeakingRawRestTemplateError() {
        AiConfigServiceImpl service = new AiConfigServiceImpl(mock(AiConfigRepository.class));
        AiConfig config = AiConfig.builder()
            .name("DeepSeek API")
            .modelType("openai")
            .modelName("deepseek-chat")
            .apiUrl("https://api.deepseek.com")
            .apiKey("test-key")
            .build();

        ResourceAccessException error = new ResourceAccessException(
            "I/O error on POST request for \"https://api.deepseek.com/chat/completions\": api.deepseek.com",
            new UnknownHostException("api.deepseek.com")
        );

        Map<String, Object> response = service.openAiFailureResponse(
            config,
            "[用户问题]\n请结合当前页面解释我刚才点击的「BSARec Job 服务离线」是什么，下一步应该怎么用。",
            "https://api.deepseek.com/chat/completions",
            error
        );

        assertThat(response.get("role")).isEqualTo("assistant");
        assertThat(response.get("content")).asString()
            .contains("BSARec Job")
            .contains("外部大模型当前无法连接")
            .contains("站内知识兜底")
            .doesNotContain("I/O error on POST request");
        assertThat(response.get("apiStatus")).isInstanceOf(Map.class);
        Map<?, ?> apiStatus = (Map<?, ?>) response.get("apiStatus");
        assertThat(apiStatus.get("status")).isEqualTo("network_unreachable");
        assertThat(apiStatus.get("provider")).isEqualTo("DeepSeek");
        assertThat(apiStatus.get("host")).isEqualTo("api.deepseek.com");
        assertThat(apiStatus.get("errorType")).isEqualTo("UnknownHostException");
    }

    @Test
    void answersModelOverviewContextWhenDeepSeekIsUnreachable() {
        AiConfigServiceImpl service = new AiConfigServiceImpl(mock(AiConfigRepository.class));
        AiConfig config = AiConfig.builder()
            .name("DeepSeek API")
            .modelType("openai")
            .modelName("deepseek-chat")
            .apiUrl("https://api.deepseek.com")
            .apiKey("test-key")
            .build();

        ResourceAccessException error = new ResourceAccessException(
            "I/O error on POST request for \"https://api.deepseek.com/chat/completions\": api.deepseek.com",
            new UnknownHostException("api.deepseek.com")
        );

        Map<String, Object> response = service.openAiFailureResponse(
            config,
            "[用户问题]\n请结合我当前所在的「模型总览」页面，说明这里能做什么、重点看哪些模块。",
            "https://api.deepseek.com/chat/completions",
            error
        );

        assertThat(response.get("content")).asString()
            .contains("模型总览")
            .contains("9 个推荐系统模型")
            .contains("模型接入测试")
            .contains("性能看板")
            .contains("外部大模型当前无法连接")
            .doesNotContain("I/O error on POST request");
    }
}
