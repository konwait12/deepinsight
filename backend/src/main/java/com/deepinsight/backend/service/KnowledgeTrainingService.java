package com.deepinsight.backend.service;

import com.deepinsight.backend.entity.KnowledgeDoc;
import com.deepinsight.backend.repository.ForumPostRepository;
import com.deepinsight.backend.repository.KnowledgeArticleRepository;
import com.deepinsight.backend.repository.KnowledgeDocRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class KnowledgeTrainingService {

    private static final int VECTOR_DIMENSIONS = 192;
    private static final String VECTOR_PREFIX = "di-hash-v1:" + VECTOR_DIMENSIONS + ":";
    private static final String TRAINED_CATEGORY = "ai-training";
    private static final Pattern LATIN_TOKEN = Pattern.compile("[a-z0-9][a-z0-9@+._-]*");
    private static final Pattern CJK_RUN = Pattern.compile("[\\p{IsHan}]{2,}");

    private final KnowledgeDocRepository knowledgeDocRepository;
    private final KnowledgeArticleRepository knowledgeArticleRepository;
    private final ForumPostRepository forumPostRepository;
    private final ModelCatalogService modelCatalogService;

    @Transactional
    public TrainingReport rebuildIndex() {
        List<KnowledgeDoc> generated = generatedTrainingDocs();
        int created = 0;
        int updated = 0;

        for (KnowledgeDoc source : generated) {
            KnowledgeDoc target = findTrainingDoc(source.getTitle());
            if (target == null) {
                target = source;
                created++;
            } else {
                target.setContent(source.getContent());
                target.setCategory(source.getCategory());
                target.setTags(source.getTags());
                updated++;
            }
            target.setEmbeddingVector(embeddingVector(trainingText(target)));
            knowledgeDocRepository.save(target);
        }

        int embedded = 0;
        for (KnowledgeDoc doc : knowledgeDocRepository.findAll()) {
            String vector = doc.getEmbeddingVector();
            if (vector == null || vector.isBlank() || !vector.startsWith(VECTOR_PREFIX)) {
                doc.setEmbeddingVector(embeddingVector(trainingText(doc)));
                knowledgeDocRepository.save(doc);
                embedded++;
            }
        }

        long trainedDocs = knowledgeDocRepository.findAll().stream()
            .filter(doc -> doc.getEmbeddingVector() != null && doc.getEmbeddingVector().startsWith(VECTOR_PREFIX))
            .count();

        return new TrainingReport(
            created,
            updated,
            embedded,
            generated.size(),
            trainedDocs,
            knowledgeArticleRepository.count(),
            forumPostRepository.count(),
            modelCatalogService.listModels().size(),
            VECTOR_DIMENSIONS,
            LocalDateTime.now().toString()
        );
    }

    public TrainingStatus status() {
        List<KnowledgeDoc> docs = knowledgeDocRepository.findAll();
        long trained = docs.stream()
            .filter(doc -> doc.getEmbeddingVector() != null && doc.getEmbeddingVector().startsWith(VECTOR_PREFIX))
            .count();
        return new TrainingStatus(
            docs.size(),
            trained,
            docs.size() - trained,
            modelCatalogService.listModels().size(),
            knowledgeArticleRepository.count(),
            forumPostRepository.count(),
            VECTOR_DIMENSIONS
        );
    }

    public boolean requiresTraining() {
        List<KnowledgeDoc> docs = knowledgeDocRepository.findAll();
        if (docs.stream().anyMatch(doc -> doc.getEmbeddingVector() == null || !doc.getEmbeddingVector().startsWith(VECTOR_PREFIX))) {
            return true;
        }
        for (String title : generatedTrainingDocs().stream().map(KnowledgeDoc::getTitle).toList()) {
            boolean exists = docs.stream()
                .anyMatch(doc -> TRAINED_CATEGORY.equals(doc.getCategory()) && title.equals(doc.getTitle()));
            if (!exists) return true;
        }
        return false;
    }

    public List<RankedKnowledgeDoc> rankKnowledgeDocs(String query, int limit) {
        if (query == null || query.isBlank()) return List.of();
        double[] queryVector = parseVector(embeddingVector(query));
        List<String> queryTerms = terms(query);
        int safeLimit = Math.max(1, Math.min(limit, 20));

        return knowledgeDocRepository.findAll().stream()
            .map(doc -> new RankedKnowledgeDoc(doc, relevanceScore(queryVector, queryTerms, doc)))
            .filter(item -> item.score() > 0)
            .sorted(Comparator.comparingDouble(RankedKnowledgeDoc::score).reversed()
                .thenComparing(item -> item.doc().getCreatedAt(), Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(safeLimit)
            .toList();
    }

    public String embeddingVector(String text) {
        double[] vector = new double[VECTOR_DIMENSIONS];
        for (String term : terms(text)) {
            int hash = term.hashCode();
            int index = Math.floorMod(hash, VECTOR_DIMENSIONS);
            double sign = (hash & 1) == 0 ? 1.0d : -1.0d;
            vector[index] += sign;
        }
        normalize(vector);
        return VECTOR_PREFIX + serialize(vector);
    }

    private List<KnowledgeDoc> generatedTrainingDocs() {
        List<KnowledgeDoc> docs = new ArrayList<>();
        docs.add(trainingDoc(
            "AI训练：DeepInsight 平台功能地图",
            """
            DeepInsight 当前是推荐系统工作台。主要页面包括：
            /model-overview 模型总览，
            /model-access-test 模型接入测试，
            /performance-dashboard 性能看板，
            /dataset-visualization 数据集实时可视化，
            /data-center 数据中心，
            /ai-studio AI 工作区，
            /knowledge-base 知识中心，
            /community-forum 社区与文章，
            /admin/ai 管理员 AI 配置。

            回答用户问题时要先判断用户想看模型、数据、推理、可视化、知识库还是管理配置，再给出对应页面和操作步骤。
            不要把用户上传数据说成官方模型训练数据；不要把未接入服务说成已上线。
            """,
            "site,map,navigation,ai"
        ));
        docs.add(trainingDoc(
            "AI训练：推荐系统回答边界",
            """
            当前官方模型固定为 9 个推荐系统模型：BSARec Job、BSARec、BERT4Rec、DuoRec、FEARec、FMLP-Rec、RecBole、SASRec、TiSASRec。
            所有模型都按推荐系统解释，不再按图像、音频或检测分类解释。
            回答指标时优先使用站内真实日志中的 HR@10、NDCG@10、MRR、HR@20。
            没有日志时必须说明当前站内未记录真实评估日志，不能编造分数。
            """,
            "model,boundary,recommendation"
        ));

        for (Map<String, Object> model : modelCatalogService.listModels()) {
            docs.add(trainingDoc(
                "模型训练知识：" + text(model.get("name")),
                modelTrainingContent(model),
                "model,recommendation," + text(model.get("architecture"))
            ));
        }

        return docs;
    }

    private KnowledgeDoc trainingDoc(String title, String content, String tags) {
        return KnowledgeDoc.builder()
            .title(title)
            .content(content.strip())
            .category(TRAINED_CATEGORY)
            .tags(tags)
            .build();
    }

    private String modelTrainingContent(Map<String, Object> model) {
        StringBuilder content = new StringBuilder();
        content.append("模型名称: ").append(text(model.get("name"))).append("\n");
        content.append("中文名称: ").append(text(model.get("displayNameZh"))).append("\n");
        content.append("任务: 推荐系统\n");
        content.append("架构: ").append(text(model.get("architecture"))).append("\n");
        content.append("框架: ").append(text(model.get("framework"))).append("\n");
        content.append("接入状态: ").append(text(model.get("integrationStatus"))).append(" / ").append(text(model.get("statusLabel"))).append("\n");
        content.append("说明: ").append(text(model.get("descriptionZh"))).append("\n");
        appendMap(content, "指标", model.get("metrics"));
        appendMap(content, "数据集", model.get("datasetSummary"));
        appendMap(content, "训练与接入", model.get("trainingSummary"));
        appendMap(content, "参数", model.get("parameterSummary"));
        appendMap(content, "可视化画像", model.get("visualProfile"));
        content.append("回答规则: 只能引用上述真实数据、日志和接入状态。若指标状态显示暂无真实评估日志，应明确说明缺少日志。\n");
        return content.toString();
    }

    private void appendMap(StringBuilder content, String label, Object value) {
        if (!(value instanceof Map<?, ?> map) || map.isEmpty()) return;
        content.append(label).append(": ");
        int count = 0;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (count > 0) content.append("; ");
            content.append(entry.getKey()).append("=").append(entry.getValue());
            count++;
            if (count >= 14) break;
        }
        content.append("\n");
    }

    private KnowledgeDoc findTrainingDoc(String title) {
        return knowledgeDocRepository.findAll().stream()
            .filter(doc -> TRAINED_CATEGORY.equals(doc.getCategory()))
            .filter(doc -> title.equals(doc.getTitle()))
            .findFirst()
            .orElse(null);
    }

    private double relevanceScore(double[] queryVector, List<String> queryTerms, KnowledgeDoc doc) {
        double vectorScore = cosine(queryVector, parseVector(doc.getEmbeddingVector()));
        double lexicalScore = lexicalScore(queryTerms, trainingText(doc));
        return vectorScore * 0.72d + lexicalScore * 0.28d;
    }

    private double lexicalScore(List<String> queryTerms, String text) {
        if (queryTerms.isEmpty() || text == null || text.isBlank()) return 0d;
        String normalized = normalizeText(text);
        int hits = 0;
        for (String term : queryTerms) {
            if (normalized.contains(term)) hits++;
        }
        return Math.min(1d, hits / Math.max(4d, queryTerms.size()));
    }

    private String trainingText(KnowledgeDoc doc) {
        if (doc == null) return "";
        return String.join("\n",
            text(doc.getTitle()),
            text(doc.getCategory()),
            text(doc.getTags()),
            text(doc.getContent())
        );
    }

    private List<String> terms(String text) {
        String normalized = normalizeText(text);
        Map<String, Integer> ordered = new LinkedHashMap<>();
        Matcher latin = LATIN_TOKEN.matcher(normalized);
        while (latin.find()) {
            String token = latin.group();
            if (token.length() > 1) ordered.put(token, 1);
        }
        Matcher cjk = CJK_RUN.matcher(normalized);
        while (cjk.find()) {
            String run = cjk.group();
            for (int i = 0; i + 1 < run.length(); i++) {
                ordered.put(run.substring(i, i + 2), 1);
            }
            for (int i = 0; i + 2 < run.length(); i++) {
                ordered.put(run.substring(i, i + 3), 1);
            }
        }
        return new ArrayList<>(ordered.keySet());
    }

    private String normalizeText(String text) {
        if (text == null) return "";
        return Normalizer.normalize(text, Normalizer.Form.NFKC)
            .toLowerCase(Locale.ROOT)
            .replace('，', ' ')
            .replace('。', ' ')
            .replace('、', ' ')
            .replace('：', ' ');
    }

    private double[] parseVector(String raw) {
        double[] vector = new double[VECTOR_DIMENSIONS];
        if (raw == null || !raw.startsWith(VECTOR_PREFIX)) return vector;
        String payload = raw.substring(VECTOR_PREFIX.length());
        String[] parts = payload.split(",");
        for (int i = 0; i < Math.min(parts.length, vector.length); i++) {
            try {
                vector[i] = Double.parseDouble(parts[i]);
            } catch (NumberFormatException ignored) {
                vector[i] = 0d;
            }
        }
        return vector;
    }

    private String serialize(double[] vector) {
        StringBuilder serialized = new StringBuilder(vector.length * 8);
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) serialized.append(',');
            serialized.append(String.format(Locale.US, "%.6f", vector[i]));
        }
        return serialized.toString();
    }

    private void normalize(double[] vector) {
        double sum = 0d;
        for (double value : vector) sum += value * value;
        double norm = Math.sqrt(sum);
        if (norm <= 0d) return;
        for (int i = 0; i < vector.length; i++) vector[i] /= norm;
    }

    private double cosine(double[] a, double[] b) {
        double dot = 0d;
        for (int i = 0; i < Math.min(a.length, b.length); i++) dot += a[i] * b[i];
        return Math.max(0d, dot);
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    public record RankedKnowledgeDoc(KnowledgeDoc doc, double score) {}

    public record TrainingReport(
        int createdDocs,
        int updatedDocs,
        int embeddedExistingDocs,
        int generatedTrainingDocs,
        long totalEmbeddedDocs,
        long articleCount,
        long forumPostCount,
        int modelCount,
        int vectorDimensions,
        String trainedAt
    ) {}

    public record TrainingStatus(
        int knowledgeDocs,
        long embeddedDocs,
        long missingEmbeddings,
        int modelCount,
        long articleCount,
        long forumPostCount,
        int vectorDimensions
    ) {}
}
