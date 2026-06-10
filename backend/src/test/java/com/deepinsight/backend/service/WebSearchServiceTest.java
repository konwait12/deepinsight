package com.deepinsight.backend.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WebSearchServiceTest {

    @Test
    void rewritesVagueSimilarWebsiteIntentToPlatformSearch() throws Exception {
        WebSearchService service = enabledService();

        Object plan = buildSearchPlan(service, "\u6709\u6ca1\u6709\u7c7b\u4f3c\u7684\u7f51\u7ad9");

        assertThat(service.shouldSearch("\u6709\u6ca1\u6709\u7c7b\u4f3c\u7684\u7f51\u7ad9", false)).isTrue();
        assertThat(service.displayQuery("\u6709\u6ca1\u6709\u7c7b\u4f3c\u7684\u7f51\u7ad9"))
            .isEqualTo("\u63a8\u8350\u7cfb\u7edf \u6a21\u578b\u8bc4\u4f30 \u6570\u636e\u96c6\u53ef\u89c6\u5316 \u540c\u7c7b\u5e73\u53f0");
        assertThat(accessList(plan, "queries"))
            .asString()
            .contains("recommender system")
            .contains("model evaluation")
            .doesNotContain("\u6709\u6ca1\u6709\u7c7b\u4f3c\u7684\u7f51\u7ad9");
        assertThat(accessList(plan, "rankingTerms"))
            .asString()
            .contains("recommendation system")
            .contains("model evaluation")
            .contains("dataset visualization");
        assertThat(accessBoolean(plan, "strict")).isTrue();
        assertThat(accessBoolean(plan, "requireRelevance")).isTrue();
    }

    @Test
    void filtersDictionaryNoiseFromSimilarWebsiteSearchResults() throws Exception {
        WebSearchService service = enabledService();
        Object plan = buildSearchPlan(service, "\u6709\u6ca1\u6709\u7c7b\u4f3c\u7684\u7f51\u7ad9");

        List<Map<String, Object>> filtered = rankAndFilter(service, plan, List.of(
            result(
                "\u6709\u9053\u7ffb\u8bd1_\u5728\u7ebf\u8bcd\u5178",
                "https://fanyi.youdao.com/",
                "\u6709 \u662f\u4ec0\u4e48\u610f\u601d\uff0c\u5728\u7ebf\u7ffb\u8bd1\u548c\u6c49\u8bed\u8bcd\u5178"
            ),
            result(
                "RecBole: a unified recommender system library",
                "https://recbole.io/",
                "Recommendation system algorithms, datasets, training and evaluation for recommender research."
            ),
            result(
                "MLflow model evaluation and experiment tracking",
                "https://mlflow.org/",
                "Machine learning model evaluation, experiment tracking and platform workflow."
            )
        ));

        assertThat(filtered)
            .extracting(item -> String.valueOf(item.get("url")))
            .contains("https://recbole.io/", "https://mlflow.org/")
            .doesNotContain("https://fanyi.youdao.com/");
    }

    private static WebSearchService enabledService() {
        WebSearchService service = new WebSearchService();
        ReflectionTestUtils.setField(service, "enabled", true);
        return service;
    }

    private static Object buildSearchPlan(WebSearchService service, String query) throws Exception {
        Method method = WebSearchService.class.getDeclaredMethod("buildSearchPlan", String.class);
        method.setAccessible(true);
        return method.invoke(service, query);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> rankAndFilter(
        WebSearchService service,
        Object plan,
        List<Map<String, Object>> rawResults
    ) throws Exception {
        Method method = WebSearchService.class.getDeclaredMethod("rankAndFilter", List.class, plan.getClass(), int.class);
        method.setAccessible(true);
        return (List<Map<String, Object>>) method.invoke(service, rawResults, plan, 10);
    }

    @SuppressWarnings("unchecked")
    private static List<String> accessList(Object target, String accessor) throws Exception {
        Method method = target.getClass().getDeclaredMethod(accessor);
        method.setAccessible(true);
        return (List<String>) method.invoke(target);
    }

    private static boolean accessBoolean(Object target, String accessor) throws Exception {
        Method method = target.getClass().getDeclaredMethod(accessor);
        method.setAccessible(true);
        return (boolean) method.invoke(target);
    }

    private static Map<String, Object> result(String title, String url, String snippet) {
        return Map.of(
            "title", title,
            "url", url,
            "snippet", snippet,
            "source", "test"
        );
    }
}
