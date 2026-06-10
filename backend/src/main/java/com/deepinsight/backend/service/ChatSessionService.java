package com.deepinsight.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ChatSessionService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String SESSION_PREFIX = "chat:session:";
    private static final String CONV_PREFIX = "chat:conv:";
    private static final String USER_CONVS = "user:convs:";
    private static final String MEM_PREFIX = "user:mem:";
    private static final String CONV_SEQ = "chat:conv:seq";
    private static final long TTL_DAYS = 7;

    // ========== Conversation ==========

    public Long createConversation(Long userId, String title) {
        Long id = redisTemplate.opsForValue().increment(CONV_SEQ);
        String key = CONV_PREFIX + id;
        Map<String, Object> conv = new LinkedHashMap<>();
        conv.put("title", title != null ? title : "新对话");
        conv.put("userId", String.valueOf(userId));
        conv.put("createdAt", String.valueOf(System.currentTimeMillis()));
        conv.put("updatedAt", String.valueOf(System.currentTimeMillis()));
        redisTemplate.opsForHash().putAll(key, conv);
        redisTemplate.expire(key, TTL_DAYS, TimeUnit.DAYS);
        if (userId != null) redisTemplate.opsForSet().add(USER_CONVS + userId, id.toString());
        return id;
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getUserConversations(Long userId) {
        Set<Object> ids = redisTemplate.opsForSet().members(USER_CONVS + userId);
        if (ids == null || ids.isEmpty()) return Collections.emptyList();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object idObj : ids) {
            String key = CONV_PREFIX + idObj;
            Map<Object, Object> raw = redisTemplate.opsForHash().entries(key);
            if (raw.isEmpty()) continue;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", Long.valueOf(idObj.toString()));
            item.put("title", raw.getOrDefault("title", ""));
            item.put("updatedAt", raw.getOrDefault("updatedAt", ""));
            item.put("createdAt", raw.getOrDefault("createdAt", ""));
            result.add(item);
        }
        result.sort((a, b) -> String.valueOf(b.getOrDefault("updatedAt", ""))
                .compareTo(String.valueOf(a.getOrDefault("updatedAt", ""))));
        return result;
    }

    public boolean isOwner(Long convId, Long userId) {
        if (convId == null || userId == null || userId <= 0L) return false;
        String owner = (String) redisTemplate.opsForHash().get(CONV_PREFIX + convId, "userId");
        return owner != null && owner.equals(String.valueOf(userId));
    }

    public void bumpConversation(Long convId) {
        redisTemplate.opsForHash().put(CONV_PREFIX + convId, "updatedAt", String.valueOf(System.currentTimeMillis()));
        redisTemplate.expire(CONV_PREFIX + convId, TTL_DAYS, TimeUnit.DAYS);
    }

    public void deleteConversation(Long convId) {
        redisTemplate.opsForHash().entries(CONV_PREFIX + convId).forEach((k, v) -> {
            if ("userId".equals(k)) redisTemplate.opsForSet().remove(USER_CONVS + v, convId.toString());
        });
        redisTemplate.delete(CONV_PREFIX + convId);
        redisTemplate.delete(SESSION_PREFIX + convId);
    }

    // ========== Messages ==========

    public void saveHistory(Long convId, List<? extends Map<String, ?>> history) {
        String key = SESSION_PREFIX + convId;
        redisTemplate.delete(key);
        for (Map<String, ?> msg : history) redisTemplate.opsForList().rightPush(key, new LinkedHashMap<>(msg));
        redisTemplate.expire(key, TTL_DAYS, TimeUnit.DAYS);
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getHistory(Long convId) {
        String key = SESSION_PREFIX + convId;
        List<Object> raw = redisTemplate.opsForList().range(key, 0, -1);
        if (raw == null) return new ArrayList<>();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object obj : raw) {
            if (obj instanceof Map<?, ?> map) {
                Map<String, Object> item = new LinkedHashMap<>();
                map.forEach((field, value) -> item.put(String.valueOf(field), value));
                result.add(item);
            }
        }
        return result;
    }

    public void appendMessage(Long convId, Map<String, ?> message) {
        String key = SESSION_PREFIX + convId;
        redisTemplate.opsForList().rightPush(key, new LinkedHashMap<>(message));
        redisTemplate.expire(key, TTL_DAYS, TimeUnit.DAYS);
        bumpConversation(convId);
    }

    // ========== Memories ==========

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getMemories(Long userId) {
        String key = MEM_PREFIX + userId;
        List<Object> raw = redisTemplate.opsForList().range(key, 0, -1);
        if (raw == null) return new ArrayList<>();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object obj : raw) if (obj instanceof Map) result.add((Map<String, Object>) obj);
        return result;
    }

    public void saveMemory(Long userId, String content, float importance) {
        String key = MEM_PREFIX + userId;
        Map<String, Object> mem = new LinkedHashMap<>();
        mem.put("content", content);
        mem.put("importance", String.valueOf(importance));
        mem.put("createdAt", String.valueOf(System.currentTimeMillis()));
        redisTemplate.opsForList().rightPush(key, mem);
        redisTemplate.expire(key, TTL_DAYS, TimeUnit.DAYS);
    }

    // ========== Online ==========

    public void setUserOnline(Long userId) {
        redisTemplate.opsForValue().set("user:online:" + userId, "1", 30, TimeUnit.MINUTES);
    }

    public boolean isUserOnline(Long userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("user:online:" + userId));
    }
}
