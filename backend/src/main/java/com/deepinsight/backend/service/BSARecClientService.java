package com.deepinsight.backend.service;

import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
public class BSARecClientService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${bsarec.api.base-url:http://127.0.0.1:5000}")
    private String baseUrl;

    public Map<String, Object> recommend(Map<String, Object> request) {
        Map<String, Object> payload = normalizeRequest(request);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/recommend",
                HttpMethod.POST,
                new HttpEntity<>(payload, headers),
                Map.class
            );
            return normalizeResponse(response.getBody(), payload);
        } catch (RestClientException ex) {
            return serviceUnavailable(ex);
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

    private Map<String, Object> normalizeResponse(Map<?, ?> body, Map<String, Object> payload) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("model", "BSARec-Job");
        result.put("request", payload);
        result.put("serviceUrl", baseUrl);

        if (body == null) {
            result.put("status", "error");
            result.put("message", "BSARec service returned an empty response");
            result.put("recommendations", List.of());
            return result;
        }

        result.put("status", valueOrDefault(body, "status", "success"));
        result.put("recommendations", valueOrDefault(body, "recommendations", List.of()));
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
        Object value = source.get(key);
        return value != null ? value : defaultValue;
    }

    private Map<String, Object> serviceUnavailable(RestClientException ex) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("model", "BSARec-Job");
        result.put("status", "offline");
        result.put("serviceUrl", baseUrl);
        result.put("recommendations", List.of());
        result.put("message", "BSARec service is not reachable. Start the Flask API on port 5000 first.");
        result.put("detail", ex.getMessage());
        return result;
    }
}
