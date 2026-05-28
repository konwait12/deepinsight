package com.deepinsight.backend.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BSARecClientServiceTest {

    @Test
    void returnsOfflinePayloadWhenServiceCannotBeReached() {
        BSARecClientService service = new BSARecClientService();
        ReflectionTestUtils.setField(service, "baseUrl", "http://127.0.0.1:1");

        Map<String, Object> result = service.recommend(Map.of("user_history", List.of(1, 2, 3), "top_k", 3));

        assertThat(result.get("model")).isEqualTo("BSARec-Job");
        assertThat(result.get("status")).isEqualTo("offline");
        assertThat(result.get("recommendations")).isEqualTo(List.of());
        assertThat(result.get("message")).asString().contains("BSARec service is not reachable");
    }
}
