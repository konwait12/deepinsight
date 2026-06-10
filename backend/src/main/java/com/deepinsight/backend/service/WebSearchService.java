package com.deepinsight.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class WebSearchService {

    private static final int MAX_CONTEXT_CHARS = 6_800;
    private static final int MAX_SEARCH_RESULTS = 15;
    private static final Pattern DDG_RESULT_LINK = Pattern.compile(
        "<a[^>]+class=\"[^\"]*result__a[^\"]*\"[^>]+href=\"([^\"]+)\"[^>]*>(.*?)</a>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern DDG_LITE_LINK = Pattern.compile(
        "<a[^>]+rel=\"nofollow\"[^>]+href=\"([^\"]+)\"[^>]*>(.*?)</a>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern DDG_SNIPPET = Pattern.compile(
        "<(?:a|div)[^>]+class=\"[^\"]*result__snippet[^\"]*\"[^>]*>(.*?)</(?:a|div)>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern DDG_LITE_SNIPPET = Pattern.compile(
        "<td[^>]+class=\"[^\"]*result-snippet[^\"]*\"[^>]*>(.*?)</td>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern BAIDU_RESULT_LINK = Pattern.compile(
        "<h3[^>]*>\\s*<a[^>]+href=\"([^\"]+)\"[^>]*>(.*?)</a>\\s*</h3>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern BING_RESULT_BLOCK = Pattern.compile(
        "<li[^>]+class=\"[^\"]*b_algo[^\"]*\"[^>]*>(.*?)</li>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern BING_RESULT_LINK = Pattern.compile(
        "<h2[^>]*>\\s*<a[^>]+href=\"([^\"]+)\"[^>]*>(.*?)</a>\\s*</h2>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern BING_SNIPPET = Pattern.compile(
        "<p[^>]*>(.*?)</p>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern BAIDU_SNIPPET = Pattern.compile(
        "<(?:div|span)[^>]+class=\"[^\"]*(?:c-abstract|content-right|c-span-last)[^\"]*\"[^>]*>(.*?)</(?:div|span)>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final List<String> SEARCH_TRIGGERS = List.of(
        "联网", "网上", "网络", "搜索", "查一下", "搜一下", "最新", "今天", "新闻",
        "资料来源", "外部资料", "web", "internet", "search", "latest", "online"
    );
    private static final List<ModelSearchProfile> MODEL_SEARCH_PROFILES = List.of(
        new ModelSearchProfile(
            List.of("bsarec", "bsarec job", "岗位推荐", "序列推荐"),
            List.of(
                "\"BSARec\" sequential recommendation",
                "\"BSARec\" recommender system",
                "\"sequential recommendation\" \"contrastive learning\" recommender"
            ),
            List.of(
                "bsarec", "sequential recommendation", "recommendation", "recommender", "user sequence",
                "item sequence", "ranking", "top-k", "hr", "ndcg", "arxiv", "github", "推荐", "序列"
            ),
            true
        ),
        new ModelSearchProfile(
            List.of("bert4rec", "bert4rec 双向序列推荐"),
            List.of(
                "\"BERT4Rec\" sequential recommendation",
                "\"BERT4Rec\" paper recommender system",
                "\"Bidirectional Sequential Recommendation\" BERT4Rec"
            ),
            List.of("bert4rec", "bidirectional", "sequential recommendation", "recommendation", "recommender", "transformer", "mask", "ranking"),
            true
        ),
        new ModelSearchProfile(
            List.of("sasrec", "tisasrec", "self-attention sequential recommendation", "时间间隔推荐"),
            List.of(
                "\"SASRec\" self-attentive sequential recommendation",
                "\"TiSASRec\" time interval aware sequential recommendation",
                "\"self-attention\" \"sequential recommendation\" SASRec TiSASRec"
            ),
            List.of("sasrec", "tisasrec", "self-attention", "time interval", "sequential recommendation", "recommendation", "recommender"),
            true
        ),
        new ModelSearchProfile(
            List.of("duorec", "fearec", "fmlp-rec", "fmlprec", "recbole", "推荐框架", "对比推荐", "频域推荐"),
            List.of(
                "\"DuoRec\" contrastive learning sequential recommendation",
                "\"FEARec\" frequency enhanced sequential recommendation",
                "\"FMLP-Rec\" filter-enhanced sequential recommendation",
                "\"RecBole\" recommender system framework"
            ),
            List.of("duorec", "fearec", "fmlp-rec", "fmlprec", "recbole", "contrastive", "frequency", "filter", "sequential recommendation", "recommender"),
            true
        )
    );
    private static final List<String> MODEL_CONTEXT_TERMS = List.of(
        "model", "dataset", "recommendation", "recommender", "ranking", "top-k", "hr", "ndcg", "mrr",
        "bsarec", "bert4rec", "duorec", "fearec", "fmlp-rec", "fmlprec", "recbole", "sasrec", "tisasrec",
        "\u6a21\u578b", "\u6570\u636e\u96c6", "\u63a8\u8350\u7cfb\u7edf", "\u5e8f\u5217\u63a8\u8350", "\u63a8\u8350\u6a21\u578b"
    );
    private static final List<String> MODEL_SEARCH_NOISE_TERMS = List.of(
        "minecraft", "\u6211\u7684\u4e16\u754c", "\u767e\u5ea6\u767e\u79d1", "\u6c49\u8bed", "\u6c49\u5b57",
        "\u901a\u7528\u89c4\u8303\u6c49\u5b57", "\u6e38\u620f\u5b98\u7f51", "\u7f51\u6613\u6e38\u620f",
        "\u8bcd\u5178", "\u5b57\u5178", "\u7ffb\u8bd1", "\u6709\u9053", "\u91d1\u5c71\u8bcd\u9738",
        "\u65b0\u534e\u5b57\u5178", "\u62fc\u97f3", "\u767e\u79d1", "\u6296\u97f3", "\u4f18\u9177",
        "\u653f\u5e9c", "\u6d77\u5b89", "youdao", "iciba", "zdic", "baike", "douyin", "youku", "gov.cn"
    );
    private static final List<String> SIMILAR_PLATFORM_SEARCH_QUERIES = List.of(
        "\"recommender system\" \"model evaluation\" platform",
        "\"recommendation system\" \"dataset visualization\" dashboard",
        "\"recommender systems\" experiment tracking visualization platform",
        "\"RecBole\" \"recommender system\" framework evaluation",
        "\u63a8\u8350\u7cfb\u7edf \u6a21\u578b\u8bc4\u4f30 \u6570\u636e\u96c6 \u53ef\u89c6\u5316 \u5e73\u53f0"
    );
    private static final String SIMILAR_PLATFORM_DISPLAY_QUERY =
        "\u63a8\u8350\u7cfb\u7edf \u6a21\u578b\u8bc4\u4f30 \u6570\u636e\u96c6\u53ef\u89c6\u5316 \u540c\u7c7b\u5e73\u53f0";
    private static final List<String> SIMILAR_PLATFORM_RANKING_TERMS = List.of(
        "recommender system", "recommendation system", "recommender systems", "model evaluation",
        "experiment tracking", "dataset visualization", "ml platform", "mlops", "recbole",
        "ranking", "top-k", "hr", "ndcg", "\u63a8\u8350\u7cfb\u7edf", "\u6a21\u578b\u8bc4\u4f30",
        "\u6570\u636e\u96c6", "\u53ef\u89c6\u5316", "\u8bad\u7ec3", "\u6307\u6807"
    );

    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${DEEPINSIGHT_WEB_SEARCH_ENABLED:true}")
    private boolean enabled;

    @Value("${BING_SEARCH_API_KEY:}")
    private String bingSearchApiKey;

    @Value("${BING_SEARCH_ENDPOINT:https://api.bing.microsoft.com/v7.0/search}")
    private String bingSearchEndpoint;

    public boolean shouldSearch(String query, boolean requested) {
        if (!enabled || query == null || query.isBlank()) return false;
        if (requested) return true;
        if (isSimilarPlatformQuery(query)) return true;
        String normalized = query.toLowerCase(Locale.ROOT);
        return SEARCH_TRIGGERS.stream().anyMatch(normalized::contains);
    }

    public List<Map<String, Object>> search(String query, boolean requested, int limit) {
        if (!shouldSearch(query, requested)) return List.of();
        int safeLimit = Math.max(1, Math.min(limit, MAX_SEARCH_RESULTS));
        SearchPlan plan = buildSearchPlan(query);
        if (bingSearchApiKey != null && !bingSearchApiKey.isBlank()) {
            try {
                List<Map<String, Object>> bingResults = searchWithPlan(plan, safeLimit, this::searchBing);
                if (!bingResults.isEmpty()) return withDisplayRefs(bingResults, safeLimit);
            } catch (Exception ignored) {}
        }
        try {
            List<Map<String, Object>> bingHtmlResults = searchWithPlan(plan, safeLimit, this::searchBingHtml);
            if (!bingHtmlResults.isEmpty()) return withDisplayRefs(bingHtmlResults, safeLimit);
        } catch (Exception ignored) {}
        try {
            List<Map<String, Object>> instantResults = searchWithPlan(plan, safeLimit, this::searchDuckDuckGoInstant);
            if (!instantResults.isEmpty()) return withDisplayRefs(instantResults, safeLimit);
        } catch (Exception ignored) {}
        try {
            List<Map<String, Object>> duckHtmlResults = searchWithPlan(plan, safeLimit, this::searchDuckDuckGoHtml);
            if (!duckHtmlResults.isEmpty()) return withDisplayRefs(duckHtmlResults, safeLimit);
        } catch (Exception ignored) {}
        try {
            return withDisplayRefs(searchWithPlan(plan, safeLimit, this::searchBaiduHtml), safeLimit);
        } catch (Exception ignored) {
            return List.of();
        }
    }

    public String buildContext(List<Map<String, Object>> results) {
        if (results == null || results.isEmpty()) return "";
        StringBuilder context = new StringBuilder("[联网搜索结果]\n");
        int index = 1;
        for (Map<String, Object> result : results.stream().limit(MAX_SEARCH_RESULTS).toList()) {
            String refId = text(result.get("refId"));
            if (refId.isBlank()) refId = "W" + index;
            context.append(refId).append(". ")
                .append(text(result.get("title"))).append("\n")
                .append("   来源：").append(text(result.get("source"))).append(" | ")
                .append(text(result.get("url"))).append("\n")
                .append("   摘要：").append(text(result.get("snippet"))).append("\n");
            index++;
        }
        context.append("回答规则：如果使用联网资料，只能引用上面列出的 W 编号；推荐继续阅读的网站必须与回答结论对应，不能编造未列出的网站。外部结果只是参考，平台官方模型、指标和路径仍以站内知识库为准。\n\n");
        return trim(context.toString(), MAX_CONTEXT_CHARS);
    }

    public String displayQuery(String query) {
        if (isSimilarPlatformQuery(query)) return SIMILAR_PLATFORM_DISPLAY_QUERY;
        SearchPlan plan = buildSearchPlan(query);
        if (!plan.queries().isEmpty()) return plan.queries().get(0);
        return text(query).trim();
    }

    private List<Map<String, Object>> searchBing(String query, int limit) throws Exception {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String separator = bingSearchEndpoint.contains("?") ? "&" : "?";
        URI uri = URI.create(bingSearchEndpoint + separator + "q=" + encoded + "&count=" + limit + "&mkt=zh-CN");
        HttpRequest request = HttpRequest.newBuilder(uri)
            .timeout(Duration.ofSeconds(8))
            .header("Ocp-Apim-Subscription-Key", bingSearchApiKey.trim())
            .header("User-Agent", "DeepInsight-AI/1.0")
            .GET()
            .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) return List.of();
        JsonNode root = objectMapper.readTree(response.body());
        JsonNode values = root.path("webPages").path("value");
        List<Map<String, Object>> results = new ArrayList<>();
        if (values.isArray()) {
            for (JsonNode item : values) {
                String title = item.path("name").asText("");
                String url = item.path("url").asText("");
                String snippet = item.path("snippet").asText("");
                addResult(results, title, url, snippet, "Bing", limit);
            }
        }
        return results;
    }

    private List<Map<String, Object>> searchDuckDuckGoInstant(String query, int limit) throws Exception {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        URI uri = URI.create("https://api.duckduckgo.com/?q=" + encoded + "&format=json&no_html=1&skip_disambig=1");
        HttpRequest request = HttpRequest.newBuilder(uri)
            .timeout(Duration.ofSeconds(8))
            .header("User-Agent", "DeepInsight-AI/1.0")
            .GET()
            .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) return List.of();
        JsonNode root = objectMapper.readTree(response.body());
        List<Map<String, Object>> results = new ArrayList<>();
        addResult(
            results,
            root.path("Heading").asText(""),
            root.path("AbstractURL").asText(""),
            root.path("AbstractText").asText(""),
            "DuckDuckGo",
            limit
        );
        collectDuckDuckGoTopics(root.path("Results"), results, limit);
        collectDuckDuckGoTopics(root.path("RelatedTopics"), results, limit);
        return results;
    }

    private List<Map<String, Object>> searchDuckDuckGoHtml(String query, int limit) throws Exception {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        List<Map<String, Object>> htmlResults = searchDuckDuckGoHtmlEndpoint(
            "https://html.duckduckgo.com/html/?q=" + encoded,
            DDG_RESULT_LINK,
            DDG_SNIPPET,
            limit
        );
        if (!htmlResults.isEmpty()) return htmlResults;
        return searchDuckDuckGoHtmlEndpoint(
            "https://lite.duckduckgo.com/lite/?q=" + encoded,
            DDG_LITE_LINK,
            DDG_LITE_SNIPPET,
            limit
        );
    }

    private List<Map<String, Object>> searchDuckDuckGoHtmlEndpoint(
        String url,
        Pattern linkPattern,
        Pattern snippetPattern,
        int limit
    ) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(10))
            .header("User-Agent", "Mozilla/5.0 DeepInsight-AI/1.0")
            .header("Accept", "text/html,application/xhtml+xml")
            .GET()
            .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) return List.of();

        String html = response.body();
        List<Map<String, Object>> results = new ArrayList<>();
        Matcher matcher = linkPattern.matcher(html);
        while (matcher.find() && results.size() < limit) {
            String rawUrl = normalizeSearchUrl(matcher.group(1));
            String title = cleanHtml(matcher.group(2));
            String snippet = snippetNear(html, matcher.end(), snippetPattern);
            if (snippet.isBlank()) snippet = title;
            addResult(results, title, rawUrl, snippet, "DuckDuckGo", limit);
        }
        return results;
    }

    private List<Map<String, Object>> searchBaiduHtml(String query, int limit) throws Exception {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://www.baidu.com/s?wd=" + encoded))
            .timeout(Duration.ofSeconds(10))
            .header("User-Agent", "Mozilla/5.0 DeepInsight-AI/1.0")
            .header("Accept", "text/html,application/xhtml+xml")
            .GET()
            .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) return List.of();

        String html = response.body();
        List<Map<String, Object>> results = new ArrayList<>();
        Matcher matcher = BAIDU_RESULT_LINK.matcher(html);
        while (matcher.find() && results.size() < limit) {
            String url = normalizeSearchUrl(matcher.group(1));
            String title = cleanHtml(matcher.group(2));
            String snippet = snippetNear(html, matcher.end(), BAIDU_SNIPPET);
            if (snippet.isBlank()) snippet = title;
            addResult(results, title, url, snippet, "Baidu", limit);
        }
        return results;
    }

    private List<Map<String, Object>> searchBingHtml(String query, int limit) throws Exception {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://www.bing.com/search?q=" + encoded))
            .timeout(Duration.ofSeconds(10))
            .header("User-Agent", "Mozilla/5.0 DeepInsight-AI/1.0")
            .header("Accept", "text/html,application/xhtml+xml")
            .GET()
            .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) return List.of();

        List<Map<String, Object>> results = new ArrayList<>();
        Matcher blockMatcher = BING_RESULT_BLOCK.matcher(response.body());
        while (blockMatcher.find() && results.size() < limit) {
            String block = blockMatcher.group(1);
            Matcher linkMatcher = BING_RESULT_LINK.matcher(block);
            if (!linkMatcher.find()) continue;
            String url = normalizeSearchUrl(linkMatcher.group(1));
            String title = cleanHtml(linkMatcher.group(2));
            Matcher snippetMatcher = BING_SNIPPET.matcher(block);
            String snippet = snippetMatcher.find() ? cleanHtml(snippetMatcher.group(1)) : title;
            addResult(results, title, url, snippet, "Bing", limit);
        }
        return results;
    }

    private void collectDuckDuckGoTopics(JsonNode topics, List<Map<String, Object>> results, int limit) {
        if (!topics.isArray() || results.size() >= limit) return;
        for (JsonNode topic : topics) {
            if (results.size() >= limit) return;
            if (topic.has("Topics")) {
                collectDuckDuckGoTopics(topic.path("Topics"), results, limit);
                continue;
            }
            String text = topic.path("Text").asText("");
            String url = topic.path("FirstURL").asText("");
            String title = text.contains(" - ") ? text.substring(0, text.indexOf(" - ")) : firstSentence(text);
            addResult(results, title, url, text, "DuckDuckGo", limit);
        }
    }

    private void addResult(List<Map<String, Object>> results, String title, String url, String snippet, String source, int limit) {
        if (results.size() >= limit || title == null || title.isBlank() || url == null || url.isBlank()) return;
        String safeUrl = safeResultUrl(url);
        if (safeUrl.isBlank()) return;
        Set<String> usedUrls = new LinkedHashSet<>(results.stream().map(item -> text(item.get("url"))).toList());
        if (usedUrls.contains(safeUrl)) return;
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("title", trim(title, 120));
        map.put("url", safeUrl);
        map.put("snippet", trim(snippet, 280));
        map.put("source", source);
        results.add(map);
    }

    private List<Map<String, Object>> withDisplayRefs(List<Map<String, Object>> results, int limit) {
        if (results == null || results.isEmpty()) return List.of();
        List<Map<String, Object>> ranked = new ArrayList<>();
        int index = 1;
        for (Map<String, Object> result : results.stream().limit(Math.min(limit, MAX_SEARCH_RESULTS)).toList()) {
            LinkedHashMap<String, Object> item = new LinkedHashMap<>(result);
            item.put("rank", index);
            item.put("refId", "W" + index);
            ranked.add(item);
            index++;
        }
        return ranked;
    }

    private SearchPlan buildSearchPlan(String query) {
        String cleaned = cleanSearchQuery(query);
        String normalized = normalizeForMatch(query + " " + cleaned);
        LinkedHashSet<String> queries = new LinkedHashSet<>();
        LinkedHashSet<String> rankingTerms = new LinkedHashSet<>(queryTerms(cleaned));
        boolean strict = false;
        boolean strictTermsApplied = false;
        boolean similarPlatformIntent = isSimilarPlatformQuery(query) || isSimilarPlatformQuery(cleaned);

        if (similarPlatformIntent) {
            queries.addAll(SIMILAR_PLATFORM_SEARCH_QUERIES);
            rankingTerms.clear();
            rankingTerms.addAll(SIMILAR_PLATFORM_RANKING_TERMS);
            strict = true;
            strictTermsApplied = true;
        }

        for (ModelSearchProfile profile : MODEL_SEARCH_PROFILES) {
            if (profile.aliases().stream().anyMatch(alias -> normalized.contains(normalizeForMatch(alias)))) {
                queries.addAll(profile.queries());
                if (profile.strict() && !strictTermsApplied) {
                    rankingTerms.clear();
                    strictTermsApplied = true;
                }
                rankingTerms.addAll(profile.rankingTerms());
                strict = strict || profile.strict();
            }
        }

        if (!similarPlatformIntent) {
            String enhanced = enhanceGenericQuery(cleaned);
            if (!enhanced.isBlank()) queries.add(enhanced);
            if (!cleaned.isBlank()) queries.add(cleaned);
        }
        if (queries.isEmpty()) queries.add(text(query).trim());
        boolean requireRelevance = strict || isModelContextQuery(normalized);

        return new SearchPlan(
            queries.stream().filter(item -> !item.isBlank()).limit(5).toList(),
            rankingTerms.stream().filter(item -> !item.isBlank()).limit(24).toList(),
            strict,
            requireRelevance
        );
    }

    private List<Map<String, Object>> searchWithPlan(SearchPlan plan, int limit, SearchExecutor executor) throws Exception {
        List<Map<String, Object>> rawResults = new ArrayList<>();
        for (String searchQuery : plan.queries()) {
            rawResults.addAll(executor.search(searchQuery, Math.max(limit, 4)));
        }
        return rankAndFilter(rawResults, plan, limit);
    }

    private List<Map<String, Object>> rankAndFilter(List<Map<String, Object>> rawResults, SearchPlan plan, int limit) {
        if (rawResults == null || rawResults.isEmpty()) return List.of();
        LinkedHashMap<String, Map<String, Object>> unique = new LinkedHashMap<>();
        for (Map<String, Object> result : rawResults) {
            String url = text(result.get("url"));
            if (!url.isBlank()) unique.putIfAbsent(url, result);
        }

        List<ScoredResult> scored = unique.values().stream()
            .map(result -> new ScoredResult(result, relevanceScore(result, plan.rankingTerms())))
            .sorted(Comparator.comparingInt(ScoredResult::score).reversed())
            .toList();

        int threshold = plan.strict() ? 1 : (plan.requireRelevance() ? 2 : 1);
        List<Map<String, Object>> relevant = scored.stream()
            .filter(item -> item.score() >= threshold)
            .map(ScoredResult::result)
            .limit(limit)
            .toList();

        if (!relevant.isEmpty()) return relevant;
        if (plan.strict() || plan.requireRelevance()) return List.of();

        return scored.stream()
            .map(ScoredResult::result)
            .limit(limit)
            .toList();
    }

    private int relevanceScore(Map<String, Object> result, List<String> rankingTerms) {
        if (rankingTerms == null || rankingTerms.isEmpty()) return 1;
        String corpus = normalizeForMatch(
            text(result.get("title")) + " " + text(result.get("snippet")) + " " + text(result.get("url"))
        );
        int score = 0;
        for (String term : rankingTerms) {
            String normalizedTerm = normalizeForMatch(term);
            if (normalizedTerm.isBlank() || !corpus.contains(normalizedTerm)) continue;
            score += normalizedTerm.contains(" ") ? 2 : 1;
        }
        if (corpus.contains("excel") && corpus.contains("financial") && corpus.contains("pmt")) {
            score -= 4;
        }
        if (corpus.contains("pmt function") || corpus.contains("pmt 函数")) {
            score -= 4;
        }
        if (MODEL_SEARCH_NOISE_TERMS.stream().anyMatch(term -> corpus.contains(normalizeForMatch(term)))) {
            score -= 6;
        }
        return score;
    }

    private boolean isSimilarPlatformQuery(String query) {
        String compact = normalizeForMatch(query).replace(" ", "");
        if (compact.isBlank()) return false;
        return compact.contains("\u7c7b\u4f3c\u7f51\u7ad9")
            || compact.contains("\u7c7b\u4f3c\u7684\u7f51\u7ad9")
            || compact.contains("\u540c\u7c7b\u7f51\u7ad9")
            || compact.contains("\u53c2\u8003\u7f51\u7ad9")
            || compact.contains("\u7ade\u54c1")
            || compact.contains("\u66ff\u4ee3\u54c1")
            || compact.contains("\u66ff\u4ee3\u65b9\u6848")
            || compact.contains("similarwebsite")
            || compact.contains("similarwebsites")
            || compact.contains("siteslike")
            || compact.contains("alternatives")
            || compact.contains("competitors")
            || (compact.contains("\u7c7b\u4f3c") && compact.contains("\u7f51\u7ad9"))
            || (compact.contains("\u6709\u6ca1\u6709") && compact.contains("\u7f51\u7ad9"));
    }

    private boolean isModelContextQuery(String normalizedQuery) {
        if (normalizedQuery == null || normalizedQuery.isBlank()) return false;
        return MODEL_CONTEXT_TERMS.stream()
            .map(this::normalizeForMatch)
            .filter(term -> !term.isBlank())
            .anyMatch(normalizedQuery::contains);
    }

    private String snippetNear(String html, int start, Pattern snippetPattern) {
        int end = Math.min(html.length(), start + 2_200);
        Matcher matcher = snippetPattern.matcher(html.substring(start, end));
        if (!matcher.find()) return "";
        return cleanHtml(matcher.group(1));
    }

    private String normalizeSearchUrl(String rawUrl) {
        String url = htmlDecode(rawUrl).replaceAll("[\\r\\n\\t]", "").trim();
        if (url.startsWith("//")) url = "https:" + url;
        if (url.startsWith("/")) url = "https://www.baidu.com" + url;
        Matcher redirect = Pattern.compile("[?&]uddg=([^&]+)").matcher(url);
        if (redirect.find()) {
            try {
                return URLDecoder.decode(redirect.group(1), StandardCharsets.UTF_8);
            } catch (Exception e) {
                return "";
            }
        }
        return url;
    }

    private String safeResultUrl(String rawUrl) {
        String normalized = normalizeSearchUrl(rawUrl);
        if (normalized.isBlank()) return "";
        try {
            URI uri = URI.create(normalized);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null || host.isBlank()) return "";
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) return "";
            return uri.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private String cleanHtml(String value) {
        return htmlDecode(value)
            .replaceAll("<[^>]+>", " ")
            .replaceAll("\\s+", " ")
            .trim();
    }

    private String htmlDecode(String value) {
        return text(value)
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&#x27;", "'");
    }

    private String firstSentence(String text) {
        String safe = text(text).trim();
        if (safe.length() <= 64) return safe;
        int split = safe.indexOf('。');
        if (split < 0) split = safe.indexOf('.');
        return split > 0 ? safe.substring(0, Math.min(split + 1, 64)) : safe.substring(0, 64);
    }

    private String cleanSearchQuery(String query) {
        String cleaned = text(query)
            .replaceAll("(?i)\\b(web\\s*search|internet\\s*search|online\\s*search|search|latest)\\b", " ")
            .replace("联网搜索", " ")
            .replace("联网查找", " ")
            .replace("联网查询", " ")
            .replace("联网", " ")
            .replace("搜索", " ")
            .replace("搜一下", " ")
            .replace("查一下", " ")
            .replace("网上", " ")
            .replace("网络", " ")
            .replace("结合平台", " ")
            .replace("结合我们的平台", " ")
            .replace("告诉我", " ")
            .replace("是什么", " ")
            .replace("说明", " ")
            .replaceAll("[，。；、：:]+", " ")
            .replaceAll("\\s+", " ")
            .trim();
        return cleaned.isBlank() ? text(query).trim() : cleaned;
    }

    private String enhanceGenericQuery(String query) {
        String cleaned = text(query).trim();
        if (cleaned.isBlank()) return "";
        String normalized = normalizeForMatch(cleaned);
        if (normalized.contains("recommend") || normalized.contains("recommender") || cleaned.contains("推荐")) {
            return cleaned + " recommender system sequential recommendation model";
        }
        if (normalized.contains("sequence") || normalized.contains("sasrec") || normalized.contains("bert4rec") || normalized.contains("tisasrec")) {
            return cleaned + " sequential recommendation recommender system";
        }
        return cleaned;
    }

    private List<String> queryTerms(String query) {
        String normalized = normalizeForMatch(query);
        if (normalized.isBlank()) return List.of();
        List<String> terms = new ArrayList<>();
        for (String term : normalized.split("\\s+")) {
            if (term.length() >= 3 && !SEARCH_TRIGGERS.contains(term)) terms.add(term);
        }
        return terms;
    }

    private String normalizeForMatch(String value) {
        return text(value)
            .toLowerCase(Locale.ROOT)
            .replace('‑', '-')
            .replace('–', '-')
            .replace('—', '-')
            .replaceAll("[\"'`]+", " ")
            .replaceAll("[^\\p{IsHan}a-z0-9]+", " ")
            .replaceAll("\\s+", " ")
            .trim();
    }

    private String trim(String value, int maxChars) {
        String safe = text(value).replaceAll("\\s+", " ").trim();
        if (safe.length() <= maxChars) return safe;
        return safe.substring(0, Math.max(0, maxChars - 3)) + "...";
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    @FunctionalInterface
    private interface SearchExecutor {
        List<Map<String, Object>> search(String query, int limit) throws Exception;
    }

    private record ModelSearchProfile(
        List<String> aliases,
        List<String> queries,
        List<String> rankingTerms,
        boolean strict
    ) {}

    private record SearchPlan(List<String> queries, List<String> rankingTerms, boolean strict, boolean requireRelevance) {}

    private record ScoredResult(Map<String, Object> result, int score) {}
}
