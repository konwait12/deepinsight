package com.deepinsight.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class BSARecClientService {

    private final RestTemplate restTemplate = new RestTemplate(requestFactory());

    @Value("${bsarec.api.base-url:http://127.0.0.1:5000}")
    private String baseUrl;

    public Map<String, Object> health() {
        long started = System.nanoTime();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("serviceUrl", baseUrl);
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(baseUrl + "/health", Map.class);
            Map<?, ?> body = response.getBody();
            result.put("status", valueOrDefault(body, "status", "unknown"));
            result.put("online", response.getStatusCode().is2xxSuccessful());
            result.put("modelLoaded", valueOrDefault(body, "model_loaded", false));
            result.put("jobInfoLoaded", valueOrDefault(body, "job_info_loaded", false));
            result.put("modelPath", valueOrDefault(body, "model_path", ""));
            result.put("jobInfoPath", valueOrDefault(body, "job_info_path", ""));
            result.put("device", valueOrDefault(body, "device", ""));
        } catch (RestClientException ex) {
            result.put("status", "offline");
            result.put("online", false);
            result.put("message", "BSARec service is not reachable. Start the Flask API on port 5000 first.");
            result.put("detail", ex.getMessage());
        }
        result.put("elapsedMs", elapsedMillis(started));
        return result;
    }

    public Map<String, Object> recommend(Map<String, Object> request) {
        Map<String, Object> payload = normalizeRequest(request);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        long started = System.nanoTime();
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/recommend",
                HttpMethod.POST,
                new HttpEntity<>(payload, headers),
                Map.class
            );
            return normalizeResponse(response.getBody(), payload, elapsedMillis(started));
        } catch (RestClientException ex) {
            return serviceUnavailable(ex, elapsedMillis(started));
        }
    }

    private Map<String, Object> normalizeRequest(Map<String, Object> request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("user_history", request.getOrDefault("user_history", List.of()));
        payload.put("user_id", request.getOrDefault("user_id", "deepinsight-user"));
        payload.put("top_k", request.getOrDefault("top_k", 10));
        payload.put("include_job_info", request.getOrDefault("include_job_info", true));
        if (request.containsKey("item_size")) {
            payload.put("item_size", request.get("item_size"));
        }
        return payload;
    }

    private Map<String, Object> normalizeResponse(Map<?, ?> body, Map<String, Object> payload, long elapsedMs) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("model", "BSARec-Job");
        result.put("request", payload);
        result.put("serviceUrl", baseUrl);
        result.put("elapsedMs", elapsedMs);

        if (body == null) {
            result.put("status", "error");
            result.put("message", "BSARec service returned an empty response");
            result.put("recommendations", List.of());
            return result;
        }

        result.put("status", valueOrDefault(body, "status", "success"));
        result.put("recommendations", normalizeRecommendations(valueOrDefault(body, "recommendations", List.of())));
        result.put("userId", body.get("user_id"));
        result.put("requestId", body.get("request_id"));
        result.put("includeJobInfo", body.get("include_job_info"));
        if (body.containsKey("error")) {
            result.put("status", "error");
            result.put("message", body.get("error"));
        }
        return result;
    }

    private Object valueOrDefault(Map<?, ?> source, String key, Object defaultValue) {
        if (source == null) {
            return defaultValue;
        }
        Object value = source.get(key);
        return value != null ? value : defaultValue;
    }

    private List<Map<String, Object>> normalizeRecommendations(Object raw) {
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        int rank = 1;
        List<Map<String, Object>> normalized = new java.util.ArrayList<>();
        for (Object item : list) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("rank", rank++);
            if (item instanceof Map<?, ?> map) {
                Object itemId = valueOrDefault(map, "item_id", valueOrDefault(map, "itemId", ""));
                row.put("itemId", String.valueOf(itemId));
                row.put("id", itemId);
                Object score = valueOrDefault(map, "score", null);
                if (score != null) row.put("score", score);
                Object jobInfo = valueOrDefault(map, "job_info", valueOrDefault(map, "jobInfo", null));
                if (jobInfo instanceof Map<?, ?> job) {
                    row.put("position", valueOrDefault(job, "position", ""));
                    row.put("company", valueOrDefault(job, "company", ""));
                    row.put("salary", valueOrDefault(job, "salary", ""));
                }
            } else {
                row.put("itemId", String.valueOf(item));
                row.put("id", item);
            }
            normalized.add(row);
        }
        return normalized;
    }

    private Map<String, Object> serviceUnavailable(RestClientException ex, long elapsedMs) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("model", "BSARec-Job");
        result.put("status", "offline");
        result.put("serviceUrl", baseUrl);
        result.put("recommendations", List.of());
        result.put("message", "BSARec service is not reachable. Start the Flask API on port 5000 first.");
        result.put("detail", ex.getMessage());
        result.put("elapsedMs", elapsedMs);
        return result;
    }

    private long elapsedMillis(long startedNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos);
    }

    private static SimpleClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(1500);
        factory.setReadTimeout(5000);
        return factory;
    }
}
