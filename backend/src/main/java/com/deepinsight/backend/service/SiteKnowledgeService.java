package com.deepinsight.backend.service;

import com.deepinsight.backend.entity.ForumPost;
import com.deepinsight.backend.entity.KnowledgeArticle;
import com.deepinsight.backend.entity.KnowledgeDoc;
import com.deepinsight.backend.repository.ForumPostRepository;
import com.deepinsight.backend.repository.KnowledgeArticleRepository;
import com.deepinsight.backend.repository.KnowledgeDocRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SiteKnowledgeService {

    private static final int MAX_CONTEXT_CHARS = 14_000;
    private static final int MAX_ARTICLE_CHARS = 760;
    private static final int MAX_FORUM_CHARS = 620;

    private final ModelCatalogService modelCatalogService;
    private final KnowledgeDocRepository knowledgeDocRepository;
    private final KnowledgeArticleRepository knowledgeArticleRepository;
    private final ForumPostRepository forumPostRepository;
    private final KnowledgeTrainingService knowledgeTrainingService;

    public String buildContext(String userMessage) {
        StringBuilder context = new StringBuilder(16_000);
        appendOperatingRules(context);
        appendFeatureMap(context);
        appendModelCatalog(context);
        appendArticleKnowledge(context, userMessage);
        return trimContext(context.toString(), MAX_CONTEXT_CHARS);
    }

    public List<Map<String, Object>> findRelatedContent(String userMessage, int limit) {
        List<String> terms = queryTerms(userMessage);
        if (terms.isEmpty()) return List.of();
        int safeLimit = Math.max(1, Math.min(limit, 10));
        LinkedHashMap<String, Map<String, Object>> selected = new LinkedHashMap<>();
        Set<String> usedTitles = new LinkedHashSet<>();

        try {
            knowledgeArticleRepository.findAll().stream()
                .map(article -> Map.entry(article, relevanceScore(terms, article.getTitle(), article.getContent())))
                .filter(entry -> entry.getValue() > 0)
                .sorted(Map.Entry.<KnowledgeArticle, Integer>comparingByValue().reversed()
                    .thenComparing(entry -> articleUpdatedAt(entry.getKey()), Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(safeLimit)
                .forEach(entry -> addRelatedArticle(selected, usedTitles, entry.getKey(), entry.getValue()));
        } catch (Exception ignored) {}

        try {
            collectForumPosts(userMessage).stream()
                .map(post -> Map.entry(post, relevanceScore(terms, post.getTitle(), post.getContent())))
                .filter(entry -> entry.getValue() > 0)
                .limit(safeLimit)
                .forEach(entry -> addForumPost(selected, usedTitles, entry.getKey(), entry.getValue()));
        } catch (Exception ignored) {}

        try {
            collectKnowledgeDocs(userMessage).stream()
                .map(doc -> Map.entry(doc, relevanceScore(terms, doc.getTitle(), doc.getContent())))
                .filter(entry -> entry.getValue() > 0)
                .limit(safeLimit)
                .forEach(entry -> addKnowledgeDoc(selected, usedTitles, entry.getKey(), entry.getValue()));
        } catch (Exception ignored) {}

        return selected.values().stream()
            .sorted(Comparator
                .comparingInt((Map<String, Object> item) -> number(item.get("score"))).reversed()
                .thenComparing(item -> sourceWeight(text(item.get("source"))), Comparator.reverseOrder()))
            .limit(safeLimit)
            .toList();
    }

    private void appendOperatingRules(StringBuilder context) {
        context.append("[DeepInsight 站内 AI 适配规则]\n")
            .append("- 你是 DeepInsight 平台内置 AI，要优先结合当前站点功能、页面路径、文章内容和官方模型清单回答。\n")
            .append("- 不要暴露服务器绝对路径、密钥、数据库连接等隐私信息；用户只需要模型名称、任务类型、指标、输入输出和使用方式。\n")
            .append("- 当前官方模型固定为 9 个推荐系统模型：BSARec Job、BSARec、BERT4Rec、DuoRec、FEARec、FMLP-Rec、RecBole、SASRec、TiSASRec。\n")
            .append("- 用户问操作时给出站内页面和步骤；用户问模型时围绕推荐系统、真实数据、指标日志、接入状态、参数和可视化解释。\n")
            .append("- 遇到缺失指标时明确说明当前站内未记录该指标，不要编造实验数值。\n\n");
    }

    private void appendFeatureMap(StringBuilder context) {
        context.append("[全站功能地图]\n");
        List<FeatureInfo> features = List.of(
            new FeatureInfo("/model-overview", "模型总览", "展示 9 个推荐系统模型的状态、核心指标、数据集摘要、训练配置、参数和模型画像。"),
            new FeatureInfo("/model-access-test", "模型接入测试", "验证推荐系统样例输入、Top-K 输出、后端代理和服务健康状态。"),
            new FeatureInfo("/performance-dashboard", "性能看板", "按 HR、NDCG、MRR、数据规模、训练配置和接入成熟度对推荐模型做可视化比对。"),
            new FeatureInfo("/dataset-visualization", "数据集实时可视化", "面向推荐数据做用户、物品、交互、序列长度、缺失分析、分布图和可视化摘要。"),
            new FeatureInfo("/data-center", "数据中心", "管理用户数据集上传、素材管理、数据记录和工作区资源。"),
            new FeatureInfo("/ai-studio", "AI 工作区", "完整 AI 对话、资源选择、附件上下文、视觉结果导入、会话记忆和深度思考控制。"),
            new FeatureInfo("/knowledge-base", "知识中心", "展示知识图谱与文章详情，可作为 AI 回答的站内知识来源。"),
            new FeatureInfo("/community-forum", "社区与文章", "承载官方文章、知识文章同步内容和用户讨论，AI 可引用其摘要辅助回答。"),
            new FeatureInfo("/profile", "个人中心", "用户资料、账号信息和个性化设置入口。"),
            new FeatureInfo("/admin/ai", "管理员 AI 配置", "配置兼容模型、系统提示词、温度和上下文窗口。"),
            new FeatureInfo("/admin/kb", "管理员知识库", "维护知识库文档、标签、分类和检索内容。"),
            new FeatureInfo("/admin/forum", "管理员社区", "维护官方文章和论坛内容。"),
            new FeatureInfo("/admin/data", "管理员数据", "管理用户上传数据集记录和数据资产。"),
            new FeatureInfo("/admin/users", "管理员用户", "管理用户账号、权限和状态。")
        );
        for (FeatureInfo feature : features) {
            context.append("- ").append(feature.path()).append(" | ")
                .append(feature.name()).append(": ")
                .append(feature.description()).append("\n");
        }
        context.append("\n");
    }

    private void appendModelCatalog(StringBuilder context) {
        context.append("[官方模型精简清单]\n");
        for (Map<String, Object> model : modelCatalogService.listModels()) {
            String name = text(model.get("name"));
            String taskType = text(model.get("taskType"));
            context.append("- ").append(name)
                .append(" | 任务: ").append(taskLabel(taskType))
                .append(" | 架构: ").append(text(model.get("architecture")))
                .append(" | 状态: ").append(text(model.get("integrationStatus")))
                .append(" | 样例耗时: ").append(text(model.get("latencyMs"))).append(" ms\n");
            appendMapLine(context, "  指标", model.get("metrics"), 8);
            appendMapLine(context, "  数据集", model.get("datasetSummary"), 8);
            appendMapLine(context, "  训练", model.get("trainingSummary"), 7);
            appendMapLine(context, "  参数", model.get("parameterSummary"), 8);
            context.append("  AI 回答要点: ").append(modelGuide(taskType, name)).append("\n");
        }
        context.append("\n");
    }

    private void appendArticleKnowledge(StringBuilder context, String userMessage) {
        List<KnowledgeDoc> docs = collectKnowledgeDocs(userMessage);
        List<ForumPost> posts = collectForumPosts(userMessage);
        if (docs.isEmpty() && posts.isEmpty()) return;

        context.append("[站内文章与知识摘要]\n");
        for (KnowledgeDoc doc : docs.stream().limit(6).toList()) {
            context.append("- 知识库#").append(doc.getId())
                .append("《").append(nullSafe(doc.getTitle())).append("》")
                .append(categorySuffix(doc.getCategory(), doc.getTags()))
                .append("\n  摘要: ").append(trimContext(doc.getContent(), MAX_ARTICLE_CHARS))
                .append("\n");
        }
        for (ForumPost post : posts.stream().limit(5).toList()) {
            context.append("- 文章/帖子#").append(post.getId())
                .append("《").append(nullSafe(post.getTitle())).append("》")
                .append(Boolean.TRUE.equals(post.getIsOfficial()) ? " | 官方" : "")
                .append(Boolean.TRUE.equals(post.getIsPinned()) ? " | 置顶" : "")
                .append("\n  摘要: ").append(trimContext(post.getContent(), MAX_FORUM_CHARS))
                .append("\n");
        }
        context.append("\n");
    }

    private List<KnowledgeDoc> collectKnowledgeDocs(String query) {
        LinkedHashMap<Long, KnowledgeDoc> selected = new LinkedHashMap<>();
        String normalizedQuery = normalize(query);
        if (!normalizedQuery.isBlank()) {
            try {
                for (KnowledgeTrainingService.RankedKnowledgeDoc ranked : knowledgeTrainingService.rankKnowledgeDocs(query, 10)) {
                    KnowledgeDoc doc = ranked.doc();
                    if (doc.getId() != null) selected.put(doc.getId(), doc);
                }
            } catch (Exception ignored) {
                // Vector context is best-effort; lexical/fulltext fallbacks below still apply.
            }
        }
        if (!normalizedQuery.isBlank()) {
            try {
                for (KnowledgeDoc doc : knowledgeDocRepository.searchByFulltext(query)) {
                    if (doc.getId() != null) selected.put(doc.getId(), doc);
                }
            } catch (Exception ignored) {
                // Some local schemas may not have a FULLTEXT index yet; fall back to in-memory ranking.
            }
        }

        try {
            List<String> terms = queryTerms(query);
            knowledgeDocRepository.findAll().stream()
                .sorted(Comparator
                    .comparingInt((KnowledgeDoc doc) -> relevanceScore(terms, doc.getTitle(), doc.getContent())).reversed()
                    .thenComparing(SiteKnowledgeService::docCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(10)
                .forEach(doc -> {
                    if (doc.getId() != null) selected.putIfAbsent(doc.getId(), doc);
                });
        } catch (Exception ignored) {
            // Knowledge context is best-effort and should never block chat.
        }
        return new ArrayList<>(selected.values());
    }

    private List<ForumPost> collectForumPosts(String query) {
        try {
            List<String> terms = queryTerms(query);
            return forumPostRepository.findAllByOrderByIsPinnedDescCreatedAtDesc().stream()
                .sorted(Comparator
                    .comparingInt((ForumPost post) -> relevanceScore(terms, post.getTitle(), post.getContent())).reversed()
                    .thenComparing((ForumPost post) -> Boolean.TRUE.equals(post.getIsOfficial()) ? 1 : 0, Comparator.reverseOrder())
                    .thenComparing((ForumPost post) -> Boolean.TRUE.equals(post.getIsPinned()) ? 1 : 0, Comparator.reverseOrder())
                    .thenComparing(ForumPost::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(8)
                .toList();
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private void appendMapLine(StringBuilder context, String label, Object value, int limit) {
        if (!(value instanceof Map<?, ?> map) || map.isEmpty()) return;
        context.append(label).append(": ");
        int count = 0;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (count > 0) context.append("; ");
            context.append(entry.getKey()).append("=").append(entry.getValue());
            count++;
            if (count >= limit) break;
        }
        context.append("\n");
    }

    private String modelGuide(String taskType, String name) {
        return switch (taskType) {
            case "recommendation" -> name + " 是推荐系统模型，可解释用户历史、Top-K、HR/NDCG/MRR、候选物品分数、数据规模、日志状态和接入成熟度。";
            default -> name + " 是站内官方推荐模型，回答时需要围绕真实数据、指标日志、输入输出、接入状态和可视化解释。";
        };
    }

    private String taskLabel(String taskType) {
        return switch (taskType) {
            case "recommendation" -> "推荐系统";
            default -> taskType;
        };
    }

    private static LocalDateTime docCreatedAt(KnowledgeDoc doc) {
        return doc == null ? null : doc.getCreatedAt();
    }

    private static LocalDateTime articleUpdatedAt(KnowledgeArticle article) {
        return article == null ? null : article.getUpdatedAt();
    }

    private void addRelatedArticle(
        LinkedHashMap<String, Map<String, Object>> selected,
        Set<String> usedTitles,
        KnowledgeArticle article,
        int score
    ) {
        if (article == null || article.getId() == null || isDuplicateTitle(usedTitles, article.getTitle())) return;
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("id", article.getId());
        map.put("source", "knowledge_article");
        map.put("sourceLabel", "知识文章");
        map.put("title", article.getTitle());
        map.put("snippet", trimContext(article.getContent(), 220));
        map.put("path", "/knowledge-base/article/" + article.getId());
        map.put("nodeId", article.getNodeId());
        map.put("score", score);
        selected.put("knowledge_article:" + article.getId(), map);
    }

    private void addForumPost(
        LinkedHashMap<String, Map<String, Object>> selected,
        Set<String> usedTitles,
        ForumPost post,
        int score
    ) {
        if (post == null || post.getId() == null || isDuplicateTitle(usedTitles, post.getTitle())) return;
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("id", post.getId());
        map.put("source", "forum_post");
        map.put("sourceLabel", Boolean.TRUE.equals(post.getIsOfficial()) ? "官方论坛文章" : "论坛讨论");
        map.put("title", post.getTitle());
        map.put("snippet", trimContext(post.getContent(), 220));
        map.put("path", "/community-forum/" + post.getId());
        map.put("official", Boolean.TRUE.equals(post.getIsOfficial()));
        map.put("pinned", Boolean.TRUE.equals(post.getIsPinned()));
        map.put("score", score);
        selected.put("forum_post:" + post.getId(), map);
    }

    private void addKnowledgeDoc(
        LinkedHashMap<String, Map<String, Object>> selected,
        Set<String> usedTitles,
        KnowledgeDoc doc,
        int score
    ) {
        if (doc == null || doc.getId() == null || isDuplicateTitle(usedTitles, doc.getTitle())) return;
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("id", doc.getId());
        map.put("source", "knowledge_doc");
        map.put("sourceLabel", "AI 知识文档");
        map.put("title", doc.getTitle());
        map.put("snippet", trimContext(doc.getContent(), 220));
        map.put("path", "/knowledge-base");
        map.put("category", doc.getCategory());
        map.put("tags", doc.getTags());
        map.put("score", score);
        selected.put("knowledge_doc:" + doc.getId(), map);
    }

    private boolean isDuplicateTitle(Set<String> usedTitles, String title) {
        String normalized = normalize(title);
        if (normalized.isBlank()) return true;
        if (usedTitles.contains(normalized)) return true;
        usedTitles.add(normalized);
        return false;
    }

    private int sourceWeight(String source) {
        return switch (source) {
            case "knowledge_article" -> 3;
            case "forum_post" -> 2;
            case "knowledge_doc" -> 1;
            default -> 0;
        };
    }

    private int number(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private int relevanceScore(List<String> terms, String title, String content) {
        String source = normalize(title + " " + content);
        if (source.isBlank()) return 0;
        int score = 0;
        for (String term : terms) {
            if (term.isBlank()) continue;
            if (normalize(title).contains(term)) score += 8;
            if (source.contains(term)) score += 3;
        }
        return score;
    }

    private List<String> queryTerms(String query) {
        String normalized = normalize(query);
        if (normalized.isBlank()) return List.of();
        Set<String> terms = new LinkedHashSet<>();
        for (String part : normalized.split("[^\\p{IsHan}\\p{L}\\p{N}]+")) {
            if (part.length() >= 2 && part.length() <= 32) terms.add(part);
        }
        String compactHan = normalized.replaceAll("[^\\p{IsHan}]", "");
        for (int i = 0; i + 2 <= compactHan.length() && terms.size() < 20; i += 2) {
            terms.add(compactHan.substring(i, i + 2));
        }
        return terms.stream().limit(20).toList();
    }

    private String categorySuffix(String category, String tags) {
        StringBuilder suffix = new StringBuilder();
        if (category != null && !category.isBlank()) suffix.append(" | ").append(category);
        if (tags != null && !tags.isBlank()) suffix.append(" | 标签: ").append(tags);
        return suffix.toString();
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    private String trimContext(String value, int maxChars) {
        String text = nullSafe(value).replaceAll("\\s+", " ").trim();
        if (text.length() <= maxChars) return text;
        return text.substring(0, Math.max(0, maxChars - 3)) + "...";
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String nullSafe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private record FeatureInfo(String path, String name, String description) {}
}
