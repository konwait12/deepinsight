package com.deepinsight.backend.service;

import com.deepinsight.backend.entity.AiConfig;
import com.deepinsight.backend.entity.ExperimentRun;
import com.deepinsight.backend.entity.TrainingJob;
import com.deepinsight.backend.entity.TrainingStep;
import com.deepinsight.backend.entity.User;
import com.deepinsight.backend.repository.ExperimentRunRepository;
import com.deepinsight.backend.repository.TrainingJobRepository;
import com.deepinsight.backend.repository.TrainingStepRepository;
import com.deepinsight.backend.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class VisualAnalysisService {

    public record AnalysisTarget(Long runId, String runType) {}
    public record BatchRequest(String title, List<AnalysisTarget> targets, List<String> modules) {}
    public record SaveResultsRequest(Long batchId, List<Long> resultIds, String title) {}
    public record ImportToChatRequest(Long batchId, List<Long> resultIds, String message) {}

    private static final Set<String> SUPPORTED_MODULES = Set.of(
        "scalars", "text", "histograms", "prCurves", "hparams", "graphs", "profiler"
    );
    private static final DateTimeFormatter PANEL_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbcTemplate;
    private final TrainingJobRepository trainingJobRepository;
    private final ExperimentRunRepository experimentRunRepository;
    private final TrainingStepRepository trainingStepRepository;
    private final UserRepository userRepository;
    private final AiConfigService aiConfigService;
    private final ChatSessionService chatSessionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void initializeStorage() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS visual_analysis_batches (
              id BIGINT NOT NULL AUTO_INCREMENT,
              user_id BIGINT NULL,
              title VARCHAR(200) NOT NULL,
              status VARCHAR(30) NOT NULL DEFAULT 'completed',
              target_count INT NOT NULL DEFAULT 0,
              module_count INT NOT NULL DEFAULT 0,
              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
              updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              PRIMARY KEY (id),
              KEY idx_visual_analysis_batches_user_created (user_id, created_at)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS visual_analysis_results (
              id BIGINT NOT NULL AUTO_INCREMENT,
              batch_id BIGINT NOT NULL,
              user_id BIGINT NULL,
              run_id BIGINT NOT NULL,
              run_type VARCHAR(30) NOT NULL,
              run_name VARCHAR(200) NOT NULL,
              model_name VARCHAR(200) NULL,
              module_key VARCHAR(80) NOT NULL,
              status VARCHAR(30) NOT NULL,
              score DOUBLE NULL,
              record_count BIGINT NOT NULL DEFAULT 0,
              latest_step BIGINT NULL,
              summary TEXT NULL,
              metrics_json MEDIUMTEXT NULL,
              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
              updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              PRIMARY KEY (id),
              KEY idx_visual_analysis_results_batch (batch_id),
              KEY idx_visual_analysis_results_user_module (user_id, module_key)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS visual_ai_panels (
              id BIGINT NOT NULL AUTO_INCREMENT,
              result_id BIGINT NOT NULL,
              module_key VARCHAR(80) NOT NULL,
              title VARCHAR(200) NOT NULL,
              insight_text TEXT NULL,
              recommendations_json MEDIUMTEXT NULL,
              ai_model_name VARCHAR(120) NOT NULL DEFAULT 'DeepInsight 规则分析器',
              status VARCHAR(30) NOT NULL DEFAULT 'ready',
              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
              updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              PRIMARY KEY (id),
              KEY idx_visual_ai_panels_result (result_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS visual_saved_analysis_records (
              id BIGINT NOT NULL AUTO_INCREMENT,
              user_id BIGINT NOT NULL,
              batch_id BIGINT NOT NULL,
              result_id BIGINT NOT NULL,
              module_key VARCHAR(80) NOT NULL,
              run_id BIGINT NOT NULL,
              run_type VARCHAR(30) NOT NULL,
              run_name VARCHAR(200) NOT NULL,
              model_name VARCHAR(200) NULL,
              title VARCHAR(220) NOT NULL,
              summary TEXT NULL,
              snapshot_json MEDIUMTEXT NULL,
              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
              updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              PRIMARY KEY (id),
              UNIQUE KEY uk_visual_saved_analysis_user_result (user_id, result_id),
              KEY idx_visual_saved_analysis_user_created (user_id, created_at),
              KEY idx_visual_saved_analysis_batch (batch_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);
        ensureOfficialExampleModels();
    }

    public Long getUserId(Principal principal) {
        if (principal == null) return null;
        return userRepository.findByUsername(principal.getName()).map(User::getId).orElse(null);
    }

    public List<Map<String, Object>> moduleCatalog() {
        return moduleDefinitions().values().stream().map(definition -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("key", definition.key());
            item.put("label", definition.label());
            item.put("description", definition.description());
            item.put("officialModels", definition.officialModels());
            return item;
        }).toList();
    }

    public Map<String, Object> createBatch(Principal principal, BatchRequest request) {
        Long userId = getUserId(principal);
        if (userId == null) {
            throw new IllegalArgumentException("请先登录后再执行矩阵分析");
        }
        List<AnalysisTarget> targets = normalizeTargets(request.targets());
        List<String> modules = normalizeModules(request.modules());
        if (targets.isEmpty()) throw new IllegalArgumentException("请至少选择一个训练任务或上传运行");
        if (modules.isEmpty()) throw new IllegalArgumentException("请至少选择一个分析模块");

        String title = request.title() == null || request.title().isBlank()
            ? "可视化矩阵分析 " + LocalDateTime.now()
            : request.title().trim();
        Long batchId = insertAndReturnId(
            "INSERT INTO visual_analysis_batches (user_id, title, status, target_count, module_count, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
            userId, title, "running", targets.size(), modules.size(), Timestamp.valueOf(LocalDateTime.now()), Timestamp.valueOf(LocalDateTime.now())
        );

        int resultCount = 0;
        for (AnalysisTarget target : targets) {
            ResolvedRun run = resolveRun(userId, target);
            for (String module : modules) {
                AnalysisOutcome outcome = analyze(run, module);
                Long resultId = insertResult(batchId, userId, run, module, outcome);
                insertAiPanel(resultId, module, run, outcome);
                resultCount++;
            }
        }

        jdbcTemplate.update(
            "UPDATE visual_analysis_batches SET status = ?, updated_at = ? WHERE id = ?",
            "completed", Timestamp.valueOf(LocalDateTime.now()), batchId
        );
        Map<String, Object> batch = getBatch(userId, batchId);
        batch.put("createdResults", resultCount);
        return batch;
    }

    public List<Map<String, Object>> recentBatches(Principal principal, int limit) {
        Long userId = getUserId(principal);
        if (userId == null) return List.of();
        int boundedLimit = Math.max(1, Math.min(limit, 20));
        return jdbcTemplate.queryForList(
            "SELECT * FROM visual_analysis_batches WHERE user_id = ? ORDER BY created_at DESC LIMIT ?",
            userId, boundedLimit
        ).stream().map(this::normalizeRow).toList();
    }

    public Map<String, Object> getBatch(Principal principal, Long batchId) {
        Long userId = getUserId(principal);
        if (userId == null) throw new IllegalArgumentException("请先登录后再查看矩阵分析");
        return getBatch(userId, batchId);
    }

    public Map<String, Object> regenerateAiPanel(Principal principal, Long resultId) {
        return regenerateModelAiPanel(principal, resultId);
    }

    public Map<String, Object> regenerateModelAiPanel(Principal principal, Long resultId) {
        PanelContext context = loadPanelContext(principal, resultId);
        Long panelId = insertModelAiPanel(context.resultId(), context.module(), context.run(), context.outcome());
        jdbcTemplate.update("DELETE FROM visual_ai_panels WHERE result_id = ? AND id <> ?", resultId, panelId);
        return normalizePanel(panelId);
    }

    public Map<String, Object> regenerateRuleAiPanel(Principal principal, Long resultId) {
        PanelContext context = loadPanelContext(principal, resultId);
        Long panelId = insertRuleAiPanel(context.resultId(), context.module(), context.run(), context.outcome(), "");
        jdbcTemplate.update("DELETE FROM visual_ai_panels WHERE result_id = ? AND id <> ?", resultId, panelId);
        return normalizePanel(panelId);
    }

    public Map<String, Object> saveResults(Principal principal, SaveResultsRequest request) {
        Long userId = requireUserId(principal, "请先登录后再保存分析记录");
        List<Map<String, Object>> results = loadOwnedResults(userId, request == null ? null : request.batchId(), request == null ? null : request.resultIds());
        if (results.isEmpty()) throw new IllegalArgumentException("没有可保存的分析结果");

        int savedCount = upsertSavedRecords(userId, results, request == null ? null : request.title());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("savedCount", savedCount);
        payload.put("records", listSavedResults(userId, Math.min(savedCount, 50)));
        return payload;
    }

    public List<Map<String, Object>> listSavedResults(Principal principal, int limit) {
        Long userId = requireUserId(principal, "请先登录后再查看已保存分析记录");
        return listSavedResults(userId, limit);
    }

    public Map<String, Object> importResultsToChat(Principal principal, ImportToChatRequest request) {
        Long userId = requireUserId(principal, "请先登录后再导入 AI 对话");
        List<Map<String, Object>> results = loadOwnedResults(userId, request == null ? null : request.batchId(), request == null ? null : request.resultIds());
        if (results.isEmpty()) throw new IllegalArgumentException("请选择至少一条分析结果导入 AI 对话");

        int savedCount = upsertSavedRecords(userId, results, null);
        String title = buildConversationTitle(results);
        Long conversationId = chatSessionService.createConversation(userId, title);
        String context = buildChatImportContext(results);
        String userMessage = request != null && request.message() != null && !request.message().isBlank()
            ? request.message().trim()
            : "请基于这些训练分析记录继续诊断，优先指出跨模型/跨模块的风险、证据和下一步实验建议。";
        String assistantIntro = "已导入 " + results.size() + " 条可视化分析结果。下面是这次复盘的结构化上下文，后续对话会基于它继续分析：\n\n" + context;

        chatSessionService.appendMessage(conversationId, Map.of("role", "assistant", "content", assistantIntro));
        chatSessionService.appendMessage(conversationId, Map.of("role", "user", "content", userMessage));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("conversationId", conversationId);
        payload.put("savedCount", savedCount);
        payload.put("importedCount", results.size());
        payload.put("title", title);
        return payload;
    }

    private PanelContext loadPanelContext(Principal principal, Long resultId) {
        Long userId = getUserId(principal);
        if (userId == null) throw new IllegalArgumentException("请先登录后再刷新 AI 分析面板");
        Map<String, Object> result = jdbcTemplate.queryForMap(
            "SELECT * FROM visual_analysis_results WHERE id = ? AND user_id = ?",
            resultId, userId
        );
        String module = String.valueOf(result.get("module_key"));
        String runType = String.valueOf(result.get("run_type"));
        Long runId = numberToLong(result.get("run_id"));
        ResolvedRun run = resolveRun(userId, new AnalysisTarget(runId, runType));
        AnalysisOutcome outcome = new AnalysisOutcome(
            String.valueOf(result.get("status")),
            numberToDouble(result.get("score")),
            numberToLong(result.get("record_count")),
            numberToLong(result.get("latest_step")),
            String.valueOf(result.get("summary")),
            parseMap(result.get("metrics_json"))
        );
        return new PanelContext(resultId, module, run, outcome);
    }

    private Long requireUserId(Principal principal, String message) {
        Long userId = getUserId(principal);
        if (userId == null || userId <= 0L) throw new IllegalArgumentException(message);
        return userId;
    }

    private List<Map<String, Object>> loadOwnedResults(Long userId, Long batchId, List<Long> requestedResultIds) {
        List<Long> resultIds = normalizeResultIds(requestedResultIds);
        if (resultIds.isEmpty() && batchId == null) {
            throw new IllegalArgumentException("请选择要处理的分析结果");
        }

        StringBuilder sql = new StringBuilder("""
            SELECT r.*, b.title AS batch_title
            FROM visual_analysis_results r
            JOIN visual_analysis_batches b ON b.id = r.batch_id
            WHERE r.user_id = ? AND b.user_id = ?
            """);
        List<Object> params = new ArrayList<>();
        params.add(userId);
        params.add(userId);
        if (!resultIds.isEmpty()) {
            sql.append(" AND r.id IN (").append(placeholders(resultIds.size())).append(")");
            params.addAll(resultIds);
        } else {
            sql.append(" AND r.batch_id = ?");
            params.add(batchId);
        }
        sql.append(" ORDER BY r.run_name ASC, r.module_key ASC, r.id ASC");

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.toString(), params.toArray());
        if (!resultIds.isEmpty() && rows.size() != resultIds.size()) {
            throw new IllegalArgumentException("部分分析结果不存在，或不属于当前登录用户");
        }
        if (!resultIds.isEmpty()) {
            Map<Long, Integer> order = new LinkedHashMap<>();
            for (int i = 0; i < resultIds.size(); i++) order.put(resultIds.get(i), i);
            rows.sort(Comparator.comparingInt(row -> order.getOrDefault(numberToLong(row.get("id")), Integer.MAX_VALUE)));
        }
        return rows;
    }

    private int upsertSavedRecords(Long userId, List<Map<String, Object>> results, String title) {
        int saved = 0;
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        for (Map<String, Object> row : results) {
            Long resultId = numberToLong(row.get("id"));
            if (resultId == null) continue;
            String module = text(row.get("module_key"));
            Map<String, Object> snapshot = buildResultSnapshot(row);
            jdbcTemplate.update(
                """
                INSERT INTO visual_saved_analysis_records
                  (user_id, batch_id, result_id, module_key, run_id, run_type, run_name, model_name, title, summary, snapshot_json, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                  batch_id = VALUES(batch_id),
                  module_key = VALUES(module_key),
                  run_id = VALUES(run_id),
                  run_type = VALUES(run_type),
                  run_name = VALUES(run_name),
                  model_name = VALUES(model_name),
                  title = VALUES(title),
                  summary = VALUES(summary),
                  snapshot_json = VALUES(snapshot_json),
                  updated_at = VALUES(updated_at)
                """,
                userId,
                numberToLong(row.get("batch_id")),
                resultId,
                module,
                numberToLong(row.get("run_id")),
                text(row.get("run_type")),
                text(row.get("run_name")),
                nullableText(row.get("model_name")),
                savedRecordTitle(row, title),
                nullableText(row.get("summary")),
                toJson(snapshot),
                now,
                Timestamp.valueOf(LocalDateTime.now())
            );
            saved++;
        }
        return saved;
    }

    private List<Map<String, Object>> listSavedResults(Long userId, int limit) {
        int boundedLimit = Math.max(1, Math.min(limit, 100));
        return jdbcTemplate.queryForList(
            "SELECT * FROM visual_saved_analysis_records WHERE user_id = ? ORDER BY updated_at DESC LIMIT ?",
            userId,
            boundedLimit
        ).stream().map(row -> {
            Map<String, Object> normalized = normalizeRow(row);
            normalized.put("snapshot", parseMap(row.get("snapshot_json")));
            normalized.remove("snapshotJson");
            return normalized;
        }).toList();
    }

    private Map<String, Object> buildResultSnapshot(Map<String, Object> row) {
        Long resultId = numberToLong(row.get("id"));
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", resultId);
        snapshot.put("batchId", numberToLong(row.get("batch_id")));
        snapshot.put("batchTitle", text(row.get("batch_title")));
        snapshot.put("runId", numberToLong(row.get("run_id")));
        snapshot.put("runType", text(row.get("run_type")));
        snapshot.put("runName", text(row.get("run_name")));
        snapshot.put("modelName", nullableText(row.get("model_name")));
        snapshot.put("moduleKey", text(row.get("module_key")));
        snapshot.put("moduleLabel", moduleLabel(text(row.get("module_key"))));
        snapshot.put("status", text(row.get("status")));
        snapshot.put("score", numberToDouble(row.get("score")));
        snapshot.put("recordCount", numberToLong(row.get("record_count")));
        snapshot.put("latestStep", numberToLong(row.get("latest_step")));
        snapshot.put("summary", nullableText(row.get("summary")));
        snapshot.put("metrics", parseMap(row.get("metrics_json")));
        snapshot.put("aiPanels", loadPanels(resultId));
        snapshot.put("savedAt", panelTime());
        return snapshot;
    }

    private List<Map<String, Object>> loadPanels(Long resultId) {
        if (resultId == null) return List.of();
        return jdbcTemplate.queryForList(
            "SELECT * FROM visual_ai_panels WHERE result_id = ? ORDER BY id DESC",
            resultId
        ).stream().map(panel -> {
            Map<String, Object> normalizedPanel = normalizeRow(panel);
            normalizedPanel.put("recommendations", parseList(panel.get("recommendations_json")));
            normalizedPanel.remove("recommendationsJson");
            return normalizedPanel;
        }).toList();
    }

    private String buildConversationTitle(List<Map<String, Object>> results) {
        if (results.size() == 1) {
            Map<String, Object> row = results.get(0);
            return truncate("分析复盘：" + modelOrRunName(row) + " · " + moduleLabel(text(row.get("module_key"))), 80);
        }
        long modelCount = results.stream().map(this::modelOrRunName).distinct().count();
        long moduleCount = results.stream().map(row -> text(row.get("module_key"))).distinct().count();
        return "分析矩阵复盘：" + modelCount + " 个模型 × " + moduleCount + " 个模块";
    }

    private String buildChatImportContext(List<Map<String, Object>> results) {
        StringBuilder builder = new StringBuilder();
        builder.append("批次：").append(text(results.get(0).get("batch_title"))).append("\n");
        builder.append("导入结果数：").append(results.size()).append("\n\n");
        for (int i = 0; i < results.size(); i++) {
            Map<String, Object> row = results.get(i);
            Map<String, Object> snapshot = buildResultSnapshot(row);
            builder.append(i + 1).append(". ")
                .append(modelOrRunName(row)).append(" / ")
                .append(snapshot.get("moduleLabel")).append("\n");
            builder.append("   状态：").append(snapshot.get("status"))
                .append("；记录数：").append(snapshot.get("recordCount"))
                .append("；最新步：").append(snapshot.get("latestStep") == null ? "无" : snapshot.get("latestStep"))
                .append("；评分：").append(snapshot.get("score") == null ? "无" : snapshot.get("score"))
                .append("\n");
            builder.append("   摘要：").append(truncate(text(snapshot.get("summary")), 280)).append("\n");
            builder.append("   指标：").append(truncate(toJson(snapshot.get("metrics")), 420)).append("\n");
            List<Map<String, Object>> panels = castPanels(snapshot.get("aiPanels"));
            if (!panels.isEmpty()) {
                Map<String, Object> panel = panels.get(0);
                builder.append("   AI 面板：").append(text(panel.get("title")))
                    .append(" / ").append(text(panel.get("aiModelName"))).append("\n");
                builder.append("   面板观点：").append(truncate(text(panel.get("insightText")), 360)).append("\n");
            }
            builder.append("\n");
        }
        return builder.toString().trim();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castPanels(Object panels) {
        if (panels instanceof List<?> list) {
            return list.stream()
                .filter(item -> item instanceof Map<?, ?>)
                .map(item -> (Map<String, Object>) item)
                .toList();
        }
        return List.of();
    }

    private List<Long> normalizeResultIds(List<Long> resultIds) {
        if (resultIds == null) return List.of();
        LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>();
        for (Long id : resultIds) {
            if (id != null && id > 0) uniqueIds.add(id);
            if (uniqueIds.size() >= 200) break;
        }
        return new ArrayList<>(uniqueIds);
    }

    private String placeholders(int count) {
        return "?,".repeat(Math.max(1, count)).replaceAll(",$", "");
    }

    private String savedRecordTitle(Map<String, Object> row, String title) {
        String base = modelOrRunName(row) + " · " + moduleLabel(text(row.get("module_key")));
        if (title == null || title.isBlank()) return truncate(base, 220);
        return truncate(title.trim() + " · " + base, 220);
    }

    private String modelOrRunName(Map<String, Object> row) {
        String modelName = nullableText(row.get("model_name"));
        return modelName == null || modelName.isBlank() ? text(row.get("run_name")) : modelName;
    }

    private Map<String, Object> getBatch(Long userId, Long batchId) {
        Map<String, Object> batch = normalizeRow(jdbcTemplate.queryForMap(
            "SELECT * FROM visual_analysis_batches WHERE id = ? AND user_id = ?",
            batchId, userId
        ));
        List<Map<String, Object>> results = jdbcTemplate.queryForList(
            "SELECT * FROM visual_analysis_results WHERE batch_id = ? AND user_id = ? ORDER BY module_key ASC, run_name ASC",
            batchId, userId
        ).stream().map(row -> {
            Map<String, Object> normalized = normalizeRow(row);
            normalized.put("metrics", parseMap(row.get("metrics_json")));
            normalized.remove("metricsJson");
            normalized.put("aiPanels", loadPanels(numberToLong(row.get("id"))));
            return normalized;
        }).toList();
        batch.put("results", results);
        return batch;
    }

    private AnalysisOutcome analyze(ResolvedRun run, String module) {
        if ("training".equals(run.type())) {
            return analyzeTraining(run, module);
        }
        return analyzeUpload(run, module);
    }

    private AnalysisOutcome analyzeTraining(ResolvedRun run, String module) {
        TrainingJob job = run.trainingJob().orElseThrow();
        List<TrainingStep> steps = trainingStepRepository.findByJobIdOrderByEpochAsc(job.getId());
        return switch (module) {
            case "scalars" -> analyzeTrainingScalars(job, steps);
            case "histograms" -> analyzeTrainingDistribution(steps);
            case "hparams" -> analyzeTrainingHParams(job);
            case "graphs" -> analyzeTrainingGraph(job);
            default -> noData(module, "该训练任务还没有记录「" + moduleLabel(module) + "」模块需要的日志。");
        };
    }

    private AnalysisOutcome analyzeTrainingScalars(TrainingJob job, List<TrainingStep> steps) {
        Map<String, Object> metrics = new LinkedHashMap<>();
        if (steps.isEmpty()) {
            if (job.getCurrentLoss() == null && job.getCurrentAccuracy() == null) {
                return noData("scalars", "该训练任务还没有写入推荐指标或标量步记录，也没有可用的当前指标摘要。");
            }
            metrics.put("latestLoss", job.getCurrentLoss());
            metrics.put("latestAccuracy", job.getCurrentAccuracy());
            return new AnalysisOutcome("ready", scoreFromAccuracy(job.getCurrentAccuracy(), job.getCurrentLoss()), 1L,
                Long.valueOf(job.getCurrentEpoch() == null ? 0 : job.getCurrentEpoch()),
                "已从训练任务记录中读取当前指标摘要，可作为临时模型状态判断。", metrics);
        }
        TrainingStep latest = steps.get(steps.size() - 1);
        Double firstLoss = firstNonNull(steps.stream().map(TrainingStep::getTrainLoss).toList());
        Double latestLoss = latest.getTrainLoss();
        Double bestAcc = steps.stream().map(TrainingStep::getTrainAccuracy).filter(v -> v != null).max(Double::compareTo).orElse(null);
        Double bestValAcc = steps.stream().map(TrainingStep::getValAccuracy).filter(v -> v != null).max(Double::compareTo).orElse(null);
        metrics.put("latestLoss", latestLoss);
        metrics.put("firstLoss", firstLoss);
        metrics.put("lossDelta", firstLoss != null && latestLoss != null ? latestLoss - firstLoss : null);
        metrics.put("bestAccuracy", bestAcc);
        metrics.put("bestValAccuracy", bestValAcc);
        metrics.put("latestLearningRate", latest.getLearningRate());
        return new AnalysisOutcome("ready", scoreFromAccuracy(bestValAcc != null ? bestValAcc : bestAcc, latestLoss), (long) steps.size(),
            Long.valueOf(latest.getEpoch()), "已基于 training_steps 表中的真实训练步记录分析推荐指标、误差和学习率变化。", metrics);
    }

    private AnalysisOutcome analyzeTrainingDistribution(List<TrainingStep> steps) {
        List<Double> losses = steps.stream().map(TrainingStep::getTrainLoss).filter(v -> v != null).toList();
        if (losses.isEmpty()) return noData("histograms", "没有可用于生成分布直方图的指标或误差记录。");
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("min", losses.stream().min(Double::compareTo).orElse(null));
        metrics.put("max", losses.stream().max(Double::compareTo).orElse(null));
        metrics.put("mean", losses.stream().mapToDouble(Double::doubleValue).average().orElse(0));
        metrics.put("source", "training_steps.train_loss");
        return new AnalysisOutcome("ready", 1.0 / Math.max(1.0, (Double) metrics.get("mean")), (long) losses.size(),
            Long.valueOf(steps.get(steps.size() - 1).getEpoch()), "已用真实指标记录生成分布统计，便于观察误差是否集中或波动异常。", metrics);
    }

    private AnalysisOutcome analyzeTrainingHParams(TrainingJob job) {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("learningRate", job.getLearningRate());
        metrics.put("batchSize", job.getBatchSize());
        metrics.put("epochs", job.getEpochs());
        metrics.put("optimizer", job.getOptimizer());
        metrics.put("device", job.getDevice());
        metrics.put("configJson", job.getConfigJson());
        return new AnalysisOutcome("ready", scoreFromAccuracy(job.getCurrentAccuracy(), job.getCurrentLoss()), 1L,
            Long.valueOf(job.getCurrentEpoch() == null ? 0 : job.getCurrentEpoch()), "已从训练任务配置中读取学习率、批次大小、轮次、优化器和设备信息。", metrics);
    }

    private AnalysisOutcome analyzeTrainingGraph(TrainingJob job) {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("architecture", job.getModelArchitecture());
        metrics.put("modelFamily", familyOf(job.getModelArchitecture()));
        metrics.put("knownOfficialModel", hasOfficialModel(job.getModelArchitecture()));
        return new AnalysisOutcome(job.getModelArchitecture() == null || job.getModelArchitecture().isBlank() ? "no_data" : "ready",
            null, job.getModelArchitecture() == null ? 0L : 1L,
            Long.valueOf(job.getCurrentEpoch() == null ? 0 : job.getCurrentEpoch()), "已读取该训练任务的模型架构元数据，可用于结构对比和官方模型匹配。", metrics);
    }

    private AnalysisOutcome analyzeUpload(ResolvedRun run, String module) {
        return switch (module) {
            case "scalars" -> analyzeTable(run.id(), "scalar_logs", "已从上传运行数据中分析标量日志。");
            case "images" -> analyzeTable(run.id(), "image_logs", "已从上传运行数据中分析样本预览和可视化记录。");
            case "audio" -> analyzeTable(run.id(), "audio_logs", "已从上传运行数据中分析序列片段和时间记录。");
            case "text" -> analyzeTable(run.id(), "text_logs", "已从上传运行数据中分析文本日志和预测片段。");
            case "histograms" -> analyzeTable(run.id(), "histogram_data", "已从上传运行数据中分析权重、梯度或标量分布记录。");
            case "embeddings" -> analyzeTable(run.id(), "embedding_data", "已从上传运行数据中分析向量投影和嵌入记录。");
            case "prCurves" -> analyzeEvalCurves(run.id());
            case "hparams" -> analyzeTable(run.id(), "hparam_data", "已从上传运行数据中分析模型参数记录。");
            case "profiler" -> analyzeTable(run.id(), "profiler_data", "已从上传运行数据中分析性能剖析记录。");
            case "graphs" -> noData("graphs", "上传运行暂未接入模型拓扑表，当前无法生成结构图分析。");
            default -> noData(module, "暂不支持该分析模块。");
        };
    }

    private AnalysisOutcome analyzeTable(Long runId, String tableName, String summary) {
        Map<String, Object> row = jdbcTemplate.queryForMap(
            "SELECT COUNT(*) AS record_count, MAX(step) AS latest_step FROM " + tableName + " WHERE run_id = ?",
            runId
        );
        long count = numberToLong(row.get("record_count"));
        Long latestStep = numberToLong(row.get("latest_step"));
        if (count == 0) return noData(tableName, "数据表 " + tableName + " 中还没有找到该运行的记录。");
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("table", tableName);
        metrics.put("recordCount", count);
        metrics.put("latestStep", latestStep);
        metrics.put("tags", distinctTags(tableName, runId));
        return new AnalysisOutcome("ready", Math.log10(count + 1), count, latestStep, summary, metrics);
    }

    private AnalysisOutcome analyzeEvalCurves(Long runId) {
        AnalysisOutcome pr = analyzeTable(runId, "pr_curve_data", "已从上传运行数据中分析命中曲线记录。");
        AnalysisOutcome roc = analyzeTable(runId, "roc_curve_data", "已从上传运行数据中分析排序曲线记录。");
        if ("no_data".equals(pr.status()) && "no_data".equals(roc.status())) {
            return noData("prCurves", "没有找到命中或排序曲线记录，暂时无法做 Top-K 评估对比。");
        }
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("prRecords", pr.recordCount());
        metrics.put("rocRecords", roc.recordCount());
        metrics.put("prTags", pr.metrics().getOrDefault("tags", List.of()));
        metrics.put("rocTags", roc.metrics().getOrDefault("tags", List.of()));
        return new AnalysisOutcome("ready", Math.log10(pr.recordCount() + roc.recordCount() + 1),
            pr.recordCount() + roc.recordCount(),
            Math.max(pr.latestStep() == null ? 0 : pr.latestStep(), roc.latestStep() == null ? 0 : roc.latestStep()),
            "已合并上传运行中的命中/排序评估曲线记录，可用于比较 Top-K、HR、NDCG 和 MRR。", metrics);
    }

    private AnalysisOutcome noData(String module, String summary) {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("module", module);
        metrics.put("reason", summary);
        return new AnalysisOutcome("no_data", null, 0L, null, summary, metrics);
    }

    private Long insertResult(Long batchId, Long userId, ResolvedRun run, String module, AnalysisOutcome outcome) {
        return insertAndReturnId(
            """
            INSERT INTO visual_analysis_results
              (batch_id, user_id, run_id, run_type, run_name, model_name, module_key, status, score, record_count, latest_step, summary, metrics_json, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            batchId, userId, run.id(), run.type(), run.name(), run.modelName(), module, outcome.status(), outcome.score(),
            outcome.recordCount(), outcome.latestStep(), outcome.summary(), toJson(outcome.metrics()),
            Timestamp.valueOf(LocalDateTime.now()), Timestamp.valueOf(LocalDateTime.now())
        );
    }

    private Long insertAiPanel(Long resultId, String module, ResolvedRun run, AnalysisOutcome outcome) {
        return insertRuleAiPanel(resultId, module, run, outcome, "");
    }

    private Long insertModelAiPanel(Long resultId, String module, ResolvedRun run, AnalysisOutcome outcome) {
        AiConfig config = aiConfigService.getActive();
        if (config == null) {
            throw new IllegalStateException("当前没有激活的 AI 配置，请先在管理员控制台启用模型，或使用规则分析按钮。");
        }

        String prompt = buildAiPanelPrompt(module, run, outcome);
        Map<String, Object> response = aiConfigService.chat(prompt, List.of(), false);
        String content = String.valueOf(response.getOrDefault("content", "")).trim();
        if (content.isBlank()
            || content.startsWith("AI调用失败")
            || content.startsWith("API调用失败")
            || content.startsWith("AI助手尚未配置")) {
            throw new IllegalStateException("已尝试调用「" + displayAiConfig(config) + "」，但模型没有返回可用内容：" + (content.isBlank() ? "空响应" : content));
        }

        List<String> recommendations = recommendations(module, outcome);
        String title = moduleLabel(module) + " AI 解读";
        String insight = content + "\n\n刷新时间：" + panelTime();
        return insertPanel(resultId, module, title, insight, recommendations, displayAiConfig(config), "ai_generated");
    }

    private Long insertRuleAiPanel(Long resultId, String module, ResolvedRun run, AnalysisOutcome outcome, String prefix) {
        List<String> recommendations = recommendations(module, outcome);
        String title = moduleLabel(module) + " 规则解读";
        String insight = "ready".equals(outcome.status())
            ? "已针对「" + run.name() + "」使用「" + moduleLabel(module) + "」模块分析 " + outcome.recordCount() + " 条真实记录。" + outcome.summary()
            : "「" + moduleLabel(module) + "」模块已经为「" + run.name() + "」预留独立 AI 面板，但当前缺少必要记录。" + outcome.summary();
        if (prefix != null && !prefix.isBlank()) {
            insight = prefix + "\n\n" + insight;
        }
        insight = insight + "\n\n生成方式：规则分析兜底；刷新时间：" + panelTime();
        return insertPanel(resultId, module, title, insight, recommendations, "DeepInsight 规则分析器", "rule_fallback");
    }

    private Long insertPanel(
        Long resultId,
        String module,
        String title,
        String insight,
        List<String> recommendations,
        String modelName,
        String status
    ) {
        return insertAndReturnId(
            """
            INSERT INTO visual_ai_panels
              (result_id, module_key, title, insight_text, recommendations_json, ai_model_name, status, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            resultId, module, title, insight, toJson(recommendations), modelName, status,
            Timestamp.valueOf(LocalDateTime.now()), Timestamp.valueOf(LocalDateTime.now())
        );
    }

    private Map<String, Object> normalizePanel(Long panelId) {
        Map<String, Object> panel = jdbcTemplate.queryForMap("SELECT * FROM visual_ai_panels WHERE id = ?", panelId);
        Map<String, Object> normalizedPanel = normalizeRow(panel);
        normalizedPanel.put("recommendations", parseList(panel.get("recommendations_json")));
        normalizedPanel.remove("recommendationsJson");
        return normalizedPanel;
    }

    private String buildAiPanelPrompt(String module, ResolvedRun run, AnalysisOutcome outcome) {
        return """
            你是 DeepInsight 的可视化分析面板，请基于下面的真实落库分析结果生成中文解读。
            要求：
            1. 不要编造没有给出的指标或样本。
            2. 如果 status 是 no_data，要明确说明缺少哪类日志，以及下一步该接入什么。
            3. 输出 3 段以内，包含「结论」「证据」「下一步」。
            4. 语气专业、直接，适合展示在产品面板中。

            运行名称：%s
            运行类型：%s
            模型名称：%s
            分析模块：%s
            状态：%s
            记录数：%d
            最新步：%s
            评分：%s
            后端摘要：%s
            指标 JSON：%s
            """.formatted(
                run.name(),
                "training".equals(run.type()) ? "训练任务" : "上传运行",
                run.modelName() == null || run.modelName().isBlank() ? "未知" : run.modelName(),
                moduleLabel(module),
                outcome.status(),
                outcome.recordCount(),
                outcome.latestStep() == null ? "无" : String.valueOf(outcome.latestStep()),
                outcome.score() == null ? "无" : String.valueOf(outcome.score()),
                outcome.summary(),
                toJson(outcome.metrics())
            );
    }

    private String displayAiConfig(AiConfig config) {
        String model = config.getModelName() == null || config.getModelName().isBlank() ? "未命名模型" : config.getModelName();
        String name = config.getName() == null || config.getName().isBlank() ? "AI 配置" : config.getName();
        return name + " / " + model;
    }

    private String panelTime() {
        return LocalDateTime.now().format(PANEL_TIME_FORMAT);
    }

    private List<String> recommendations(String module, AnalysisOutcome outcome) {
        if ("no_data".equals(outcome.status())) {
            return List.of(
                "先在训练或上传流程中接入「" + moduleLabel(module) + "」日志，再做横向对比。",
                "可以先选择另一个已有记录的模型/运行，确认该模块的展示链路是否正常。",
                "保留这个 AI 面板，它能持续暴露缺少埋点或缺少数据的问题。"
            );
        }
        if ("scalars".equals(module)) {
            return List.of("把多个模型的 HR、NDCG、MRR 放在一起比较，优先确认指标是否来自真实日志。", "同时看数据规模和日志来源，避免只盯单个最高值。", "如果指标缺失，先补评估日志或明确标注为未记录。");
        }
        if ("profiler".equals(module)) {
            return List.of("优先按接口耗时和数据加载耗时排序，定位最慢环节。", "把不同模型的服务状态并排比较，区分后端代理、模型服务和数据准备问题。", "优化推理链路前先检查输入序列、Top-K 和模型服务是否可达。");
        }
        return List.of("把这个结果和其他已选模型放在同一矩阵里比较，不要孤立判断。", "记录数本身就是数据质量信号，记录太少时先补齐日志。", "进入详情面板确认趋势和样本，再决定是否调整训练策略。");
    }

    private List<String> distinctTags(String tableName, Long runId) {
        if ("hparam_data".equals(tableName)) return List.of("hparams");
        return jdbcTemplate.queryForList(
            "SELECT DISTINCT tag FROM " + tableName + " WHERE run_id = ? ORDER BY tag ASC LIMIT 12",
            String.class,
            runId
        );
    }

    private ResolvedRun resolveRun(Long userId, AnalysisTarget target) {
        String type = "upload".equalsIgnoreCase(target.runType()) ? "upload" : "training";
        if ("training".equals(type)) {
            TrainingJob job = trainingJobRepository.findById(target.runId()).orElseThrow(() -> new IllegalArgumentException("未找到训练任务"));
            if (job.getCreatedBy() == null || !job.getCreatedBy().equals(userId)) {
                throw new IllegalArgumentException("没有权限分析该训练任务");
            }
            return new ResolvedRun(job.getId(), "training", job.getName(), job.getModelArchitecture(), Optional.of(job), Optional.empty());
        }
        ExperimentRun run = experimentRunRepository.findById(target.runId()).orElseThrow(() -> new IllegalArgumentException("未找到上传运行"));
        if (run.getUserId() == null || !run.getUserId().equals(userId)) {
            throw new IllegalArgumentException("没有权限分析该上传运行");
        }
        return new ResolvedRun(run.getId(), "upload", run.getName(), run.getName(), Optional.empty(), Optional.of(run));
    }

    private List<AnalysisTarget> normalizeTargets(List<AnalysisTarget> targets) {
        if (targets == null) return List.of();
        Set<String> seen = new LinkedHashSet<>();
        List<AnalysisTarget> normalized = new ArrayList<>();
        for (AnalysisTarget target : targets) {
            if (target == null || target.runId() == null) continue;
            String type = "upload".equalsIgnoreCase(target.runType()) ? "upload" : "training";
            String key = type + ":" + target.runId();
            if (seen.add(key)) normalized.add(new AnalysisTarget(target.runId(), type));
        }
        return normalized;
    }

    private List<String> normalizeModules(List<String> modules) {
        if (modules == null) return List.of();
        return modules.stream()
            .filter(module -> module != null && SUPPORTED_MODULES.contains(module))
            .distinct()
            .limit(10)
            .toList();
    }

    private Map<String, ModuleDefinition> moduleDefinitions() {
        Map<String, ModuleDefinition> defs = new LinkedHashMap<>();
        defs.put("scalars", new ModuleDefinition("scalars", "推荐指标趋势", "读取 training_steps 或当前训练摘要中的 loss、accuracy、HR/NDCG 等标量证据。", List.of("BSARec", "FMLP-Rec", "BERT4Rec")));
        defs.put("histograms", new ModuleDefinition("histograms", "误差分布", "用真实训练步记录统计 loss 分布，判断收敛波动和异常区间。", List.of("BSARec Job", "DuoRec", "FEARec")));
        defs.put("hparams", new ModuleDefinition("hparams", "训练配置", "读取训练任务中的学习率、批次大小、轮次、优化器和设备配置。", List.of("DuoRec", "FEARec", "TiSASRec")));
        defs.put("graphs", new ModuleDefinition("graphs", "模型结构", "读取模型架构元数据，匹配 9 个推荐系统模型的家族和接入状态。", List.of("BERT4Rec", "SASRec", "TiSASRec")));
        defs.put("prCurves", new ModuleDefinition("prCurves", "排序评估曲线", "合并上传运行中的 PR/ROC 记录，用于 Top-K、HR、NDCG、MRR 对比。", List.of("BSARec", "FMLP-Rec", "BSARec Job")));
        defs.put("text", new ModuleDefinition("text", "日志文本", "读取上传运行的训练日志、评估摘要和配置文本，不再伪装成媒体数据。", List.of("BSARec", "FMLP-Rec", "RecBole")));
        defs.put("profiler", new ModuleDefinition("profiler", "服务性能", "分析推荐服务耗时、数据加载、评估循环和吞吐瓶颈。", List.of("BSARec Job", "RecBole", "FMLP-Rec")));
        return defs;
    }

    private String moduleLabel(String module) {
        return moduleDefinitions().getOrDefault(module, new ModuleDefinition(module, module, module, List.of())).label();
    }

    private void ensureOfficialExampleModels() {
        List<OfficialExampleModel> examples = List.of(
            new OfficialExampleModel("BSARec Job", "recommendation", 0.0, "user-item sequence", "pytorch", "岗位序列推荐模型，登记后端推荐代理。"),
            new OfficialExampleModel("BSARec", "recommendation", 0.0, "user-item sequence", "pytorch", "多数据集序列推荐模型，拥有权重和评估日志。"),
            new OfficialExampleModel("BERT4Rec", "recommendation", 0.0, "user-item sequence", "tensorflow", "双向序列推荐模型，代码和真实数据已接入。"),
            new OfficialExampleModel("DuoRec", "recommendation", 0.0, "user-item sequence", "pytorch", "对比序列推荐模型，ml-100k 数据已接入。"),
            new OfficialExampleModel("FEARec", "recommendation", 0.0, "user-item sequence", "pytorch", "频域增强序列推荐模型，ml-100k 数据已接入。"),
            new OfficialExampleModel("FMLP-Rec", "recommendation", 0.0, "user-item sequence", "pytorch", "滤波增强推荐模型，拥有 Beauty 权重和指标日志。"),
            new OfficialExampleModel("RecBole", "recommendation", 0.0, "atomic recommendation data", "pytorch", "推荐系统实验框架，使用配置驱动评估。"),
            new OfficialExampleModel("SASRec", "recommendation", 0.0, "user-item sequence", "tensorflow", "自注意力序列推荐模型，包含多份真实交互数据。"),
            new OfficialExampleModel("TiSASRec", "recommendation", 0.0, "user-item-time sequence", "tensorflow", "时间间隔序列推荐模型，包含 ML-1M 时间戳数据。")
        );
        for (OfficialExampleModel model : examples) {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM model_registry WHERE name = ?", Integer.class, model.name());
            if (count != null && count > 0) continue;
            jdbcTemplate.update(
                """
                INSERT INTO model_registry
                  (name, display_name_zh, task_type, task_type_zh, description, description_zh, param_count_m, input_size, framework, is_official, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?, ?)
                """,
                model.name(), model.name(), model.taskType(), model.taskType(), model.description(), model.description(),
                model.paramCountM(), model.inputSize(), model.framework(),
                Timestamp.valueOf(LocalDateTime.now()), Timestamp.valueOf(LocalDateTime.now())
            );
        }
    }

    private boolean hasOfficialModel(String name) {
        if (name == null || name.isBlank()) return false;
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM model_registry WHERE name = ? AND is_official = 1", Integer.class, name);
        return count != null && count > 0;
    }

    private String familyOf(String modelName) {
        if (modelName == null) return "unknown";
        if (modelName.contains("BSARec")) return "bsarec-family";
        if (modelName.contains("BERT4Rec") || modelName.contains("SASRec") || modelName.contains("TiSASRec")) return "transformer-recommender";
        if (modelName.contains("DuoRec") || modelName.contains("FEARec") || modelName.contains("FMLP")) return "enhanced-recommender";
        if (modelName.contains("RecBole")) return "recommender-framework";
        return "recommender-system";
    }

    private Double firstNonNull(List<Double> values) {
        return values.stream().filter(v -> v != null).findFirst().orElse(null);
    }

    private Double scoreFromAccuracy(Double accuracy, Double loss) {
        if (accuracy != null) return accuracy > 1.5 ? accuracy / 100.0 : accuracy;
        if (loss != null) return 1.0 / (1.0 + Math.max(0.0, loss));
        return null;
    }

    private Long insertAndReturnId(String sql, Object... values) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < values.length; i++) {
                ps.setObject(i + 1, values[i]);
            }
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) throw new IllegalStateException("新增记录没有返回 ID");
        return key.longValue();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }

    private Map<String, Object> parseMap(Object value) {
        if (value == null) return Map.of();
        try {
            return objectMapper.readValue(String.valueOf(value), new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private List<Object> parseList(Object value) {
        if (value == null) return List.of();
        try {
            return objectMapper.readValue(String.valueOf(value), new TypeReference<List<Object>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private Map<String, Object> normalizeRow(Map<String, Object> row) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        row.entrySet().stream()
            .sorted(Comparator.comparing(Map.Entry::getKey))
            .forEach(entry -> normalized.put(toCamel(entry.getKey()), entry.getValue()));
        return normalized;
    }

    private String toCamel(String input) {
        StringBuilder out = new StringBuilder();
        boolean upperNext = false;
        for (char c : input.toCharArray()) {
            if (c == '_') {
                upperNext = true;
            } else if (upperNext) {
                out.append(Character.toUpperCase(c));
                upperNext = false;
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private Long numberToLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private Double numberToDouble(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String nullableText(Object value) {
        if (value == null) return null;
        String text = String.valueOf(value);
        return text.isBlank() ? null : text;
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return "";
        if (value.length() <= maxLength) return value;
        return value.substring(0, Math.max(0, maxLength - 1)) + "…";
    }

    private record ResolvedRun(
        Long id,
        String type,
        String name,
        String modelName,
        Optional<TrainingJob> trainingJob,
        Optional<ExperimentRun> experimentRun
    ) {}

    private record AnalysisOutcome(
        String status,
        Double score,
        Long recordCount,
        Long latestStep,
        String summary,
        Map<String, Object> metrics
    ) {}

    private record PanelContext(
        Long resultId,
        String module,
        ResolvedRun run,
        AnalysisOutcome outcome
    ) {}

    private record ModuleDefinition(String key, String label, String description, List<String> officialModels) {}

    private record OfficialExampleModel(
        String name,
        String taskType,
        Double paramCountM,
        String inputSize,
        String framework,
        String description
    ) {}
}
