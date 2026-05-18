package com.deepinsight.backend.service;

import com.deepinsight.backend.entity.Dataset;
import com.deepinsight.backend.entity.ExperimentRun;
import com.deepinsight.backend.entity.TrainingJob;
import com.deepinsight.backend.entity.TrainingStep;
import com.deepinsight.backend.repository.DatasetRepository;
import com.deepinsight.backend.repository.ExperimentRunRepository;
import com.deepinsight.backend.repository.TrainingJobRepository;
import com.deepinsight.backend.repository.TrainingStepRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DataAssetService {

    public record SyncRequest(List<String> assetKeys, boolean includeCloudItems) {}

    private static final Map<String, String> LOG_TABLE_LABELS = Map.of(
        "scalar_logs", "标量日志",
        "image_logs", "图像记录",
        "audio_logs", "音频记录",
        "text_logs", "文本记录",
        "histogram_data", "分布记录",
        "embedding_data", "向量记录",
        "pr_curve_data", "PR 曲线",
        "roc_curve_data", "ROC 曲线",
        "hparam_data", "超参数记录",
        "profiler_data", "性能记录"
    );

    private final AiWorkspaceService workspaceService;
    private final DatasetRepository datasetRepository;
    private final TrainingJobRepository trainingJobRepository;
    private final TrainingStepRepository trainingStepRepository;
    private final ExperimentRunRepository experimentRunRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> overview(Principal principal) {
        Long userId = workspaceService.requireUserId(principal);
        List<Map<String, Object>> assets = collectAssets(userId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("assets", assets);
        result.put("summary", buildSummary(assets));
        result.put("updatedAt", LocalDateTime.now().toString());
        return result;
    }

    public Map<String, Object> syncToCloud(Principal principal, SyncRequest request) {
        Long userId = workspaceService.requireUserId(principal);
        List<Map<String, Object>> assets = collectAssets(userId);
        List<String> keys = request == null || request.assetKeys() == null ? List.of() : request.assetKeys();
        boolean includeCloudItems = request != null && request.includeCloudItems();

        List<Map<String, Object>> selectedAssets = assets.stream()
            .filter(asset -> keys.isEmpty() || keys.contains(text(asset.get("key"))))
            .toList();

        if (!keys.isEmpty()) {
            Optional<Map<String, Object>> blocked = selectedAssets.stream()
                .filter(asset -> !Boolean.TRUE.equals(asset.get("canSync")))
                .findFirst();
            if (blocked.isPresent()) {
                throw new IllegalArgumentException("官方或只读资产不能在普通数据管理界面同步到云端，请在管理后台处理。");
            }
        }

        List<Map<String, Object>> targets = selectedAssets.stream()
            .filter(asset -> Boolean.TRUE.equals(asset.get("canSync")))
            .filter(asset -> includeCloudItems || !Boolean.TRUE.equals(asset.get("cloudSaved")))
            .toList();

        List<Map<String, Object>> saved = new ArrayList<>();
        for (Map<String, Object> asset : targets) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("assetKey", text(asset.get("key")));
            payload.put("assetType", text(asset.get("type")));
            payload.put("sourceId", asset.get("id"));
            payload.put("status", text(asset.get("status")));
            payload.put("metrics", asset.getOrDefault("metrics", Map.of()));
            payload.put("official", asset.getOrDefault("official", false));
            payload.put("readOnly", asset.getOrDefault("readOnly", false));
            payload.put("canManage", asset.getOrDefault("canManage", true));
            Long id = workspaceService.createWorkspaceItem(
                userId,
                text(asset.get("title")),
                "data_asset",
                text(asset.get("sourceType")).isBlank() ? "data_management" : text(asset.get("sourceType")),
                text(asset.get("format")),
                buildAssetContent(asset),
                null,
                payload
            );
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", id);
            row.put("assetKey", asset.get("key"));
            row.put("title", asset.get("title"));
            saved.add(row);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("savedCount", saved.size());
        result.put("items", saved);
        return result;
    }

    private List<Map<String, Object>> collectAssets(Long userId) {
        List<Map<String, Object>> assets = new ArrayList<>();
        datasetRepository.findByUploadedByOrderByCreatedAtDesc(userId).forEach(dataset -> assets.add(datasetAsset(dataset)));
        trainingJobRepository.findByCreatedByOrderByCreatedAtDesc(userId).forEach(job -> assets.add(trainingAsset(job)));
        experimentRunRepository.findByUserIdOrderByCreatedAtDesc(userId).forEach(run -> {
            assets.add(runAsset(run));
            assets.addAll(runLogAssets(run));
        });
        assets.addAll(savedAnalysisAssets(userId));
        assets.addAll(savedViewAssets(userId));
        assets.addAll(workspaceAssets(userId));
        markCloudState(userId, assets);
        return assets;
    }

    private Map<String, Object> datasetAsset(Dataset dataset) {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("样本", n(dataset.getSampleCount()));
        metrics.put("类别", n(dataset.getClassCount()));
        metrics.put("大小", formatMb(dataset.getFileSizeMb()));
        metrics.put("切分", blank(dataset.getSplitRatio(), "未设置"));

        Map<String, Object> item = base(
            "dataset:" + dataset.getId(),
            dataset.getId(),
            "dataset",
            "数据集",
            dataset.getName(),
            blank(dataset.getTaskType(), "数据集"),
            dataset.getStatus(),
            blank(dataset.getFormat(), "dataset"),
            "dataset_registry",
            dataset.getCreatedAt()
        );
        item.put("summary", blank(dataset.getDescription(), "用于训练、评估或可视化分析的数据集。"));
        item.put("metrics", metrics);
        item.put("readiness", readiness(dataset.getStatus(), dataset.getSampleCount()));
        item.put("route", "/data");
        return item;
    }

    private Map<String, Object> trainingAsset(TrainingJob job) {
        List<TrainingStep> steps = trainingStepRepository.findByJobIdOrderByEpochAsc(job.getId());
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("进度", n(job.getCurrentEpoch()) + "/" + n(job.getEpochs()));
        metrics.put("Loss", job.getCurrentLoss() == null ? "-" : String.format("%.4f", job.getCurrentLoss()));
        metrics.put("Accuracy", job.getCurrentAccuracy() == null ? "-" : String.format("%.2f%%", job.getCurrentAccuracy() * 100));
        metrics.put("步记录", steps.size());

        Map<String, Object> item = base(
            "training:" + job.getId(),
            job.getId(),
            "training",
            "训练任务",
            job.getName(),
            blank(job.getModelArchitecture(), "模型训练"),
            job.getStatus(),
            "training",
            "training_job",
            job.getUpdatedAt() == null ? job.getCreatedAt() : job.getUpdatedAt()
        );
        item.put("summary", "模型 " + blank(job.getModelArchitecture(), "-") + "，数据集 #" + n(job.getDatasetId()) + "，优化器 " + blank(job.getOptimizer(), "-") + "。");
        item.put("metrics", metrics);
        item.put("readiness", steps.isEmpty() ? "缺少训练步记录" : "可进入可视化分析");
        item.put("route", "/training");
        return item;
    }

    private Map<String, Object> runAsset(ExperimentRun run) {
        Map<String, Object> counts = logCounts(run.getId());
        int total = counts.values().stream().mapToInt(value -> ((Number) value).intValue()).sum();
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("日志记录", total);
        metrics.put("目录", blank(run.getLogDir(), "-"));

        Map<String, Object> item = base(
            "run:" + run.getId(),
            run.getId(),
            "run",
            "运行日志",
            run.getName(),
            "上传运行",
            run.getStatus(),
            "log",
            "experiment_run",
            run.getUpdatedAt() == null ? run.getCreatedAt() : run.getUpdatedAt()
        );
        item.put("summary", total > 0 ? "已解析 " + total + " 条训练可视化记录。" : "运行已创建，等待上传或解析日志。");
        item.put("metrics", metrics);
        item.put("readiness", total > 0 ? "可进入可视化分析" : "等待日志数据");
        item.put("route", "/viz");
        return item;
    }

    private List<Map<String, Object>> runLogAssets(ExperimentRun run) {
        Map<String, Object> counts = logCounts(run.getId());
        List<Map<String, Object>> items = new ArrayList<>();
        counts.forEach((table, countValue) -> {
            int count = ((Number) countValue).intValue();
            if (count <= 0) return;
            Map<String, Object> metrics = new LinkedHashMap<>();
            metrics.put("记录数", count);
            metrics.put("运行", run.getName());

            Map<String, Object> item = base(
                "run-log:" + run.getId() + ":" + table,
                run.getId(),
                "run_log",
                "日志数据",
                LOG_TABLE_LABELS.getOrDefault(table, table) + " · " + run.getName(),
                LOG_TABLE_LABELS.getOrDefault(table, table),
                "ready",
                table,
                "parsed_log",
                run.getUpdatedAt() == null ? run.getCreatedAt() : run.getUpdatedAt()
            );
            item.put("summary", "来自运行「" + run.getName() + "」的 " + count + " 条" + LOG_TABLE_LABELS.getOrDefault(table, "日志") + "。");
            item.put("metrics", metrics);
            item.put("readiness", "可用于矩阵分析");
            item.put("route", "/viz");
            items.add(item);
        });
        return items;
    }

    private List<Map<String, Object>> savedAnalysisAssets(Long userId) {
        return safeQuery("""
            SELECT id, title, module_key, run_name, model_name, summary, updated_at
            FROM visual_saved_analysis_records
            WHERE user_id = ?
            ORDER BY updated_at DESC
            LIMIT 100
            """, userId).stream().map(row -> {
                Map<String, Object> metrics = new LinkedHashMap<>();
                metrics.put("模块", moduleLabel(text(row.get("module_key"))));
                metrics.put("运行", blank(text(row.get("run_name")), "-"));
                metrics.put("模型", blank(text(row.get("model_name")), "-"));

                Map<String, Object> item = base(
                    "analysis:" + row.get("id"),
                    numberToLong(row.get("id")),
                    "analysis_result",
                    "分析结果",
                    text(row.get("title")),
                    moduleLabel(text(row.get("module_key"))),
                    "saved",
                    "analysis",
                    "visual_analysis",
                    row.get("updated_at")
                );
                item.put("summary", blank(text(row.get("summary")), "已保存的矩阵分析结果。"));
                item.put("metrics", metrics);
                item.put("readiness", "可导入 AI 继续分析");
                item.put("route", "/viz");
                return item;
            }).toList();
    }

    private List<Map<String, Object>> savedViewAssets(Long userId) {
        return safeQuery("""
            SELECT id, title, view_type, format, updated_at
            FROM visual_saved_views
            WHERE user_id = ?
            ORDER BY updated_at DESC
            LIMIT 100
            """, userId).stream().map(row -> {
                Map<String, Object> metrics = new LinkedHashMap<>();
                metrics.put("视图", blank(text(row.get("view_type")), "-"));
                metrics.put("格式", blank(text(row.get("format")), "-"));

                Map<String, Object> item = base(
                    "saved-view:" + row.get("id"),
                    numberToLong(row.get("id")),
                    "saved_view",
                    "保存视图",
                    text(row.get("title")),
                    blank(text(row.get("view_type")), "可视化视图"),
                    "saved",
                    blank(text(row.get("format")), "view"),
                    "visual_view",
                    row.get("updated_at")
                );
                item.put("summary", "已保存的可视化视图，可作为复盘、导出或 AI 分析材料。");
                item.put("metrics", metrics);
                item.put("readiness", "可复用");
                item.put("route", "/cloud");
                return item;
            }).toList();
    }

    private List<Map<String, Object>> workspaceAssets(Long userId) {
        return safeQuery("""
            SELECT id, title, item_type, source_type, format, file_size, payload_json, updated_at
            FROM ai_workspace_items
            WHERE user_id = ?
            ORDER BY updated_at DESC
            LIMIT 120
            """, userId).stream().map(row -> {
                String itemType = text(row.get("item_type"));
                Map<String, Object> metrics = new LinkedHashMap<>();
                metrics.put("类型", workspaceTypeLabel(itemType));
                metrics.put("大小", formatBytes(numberToLong(row.get("file_size"))));

                Map<String, Object> item = base(
                    "cloud:" + row.get("id"),
                    numberToLong(row.get("id")),
                    "cloud_item",
                    "云端素材",
                    text(row.get("title")),
                    workspaceTypeLabel(itemType),
                    "saved",
                    blank(text(row.get("format")), itemType),
                    blank(text(row.get("source_type")), "cloud"),
                    row.get("updated_at")
                );
                item.put("summary", "已经保存到云端素材库，可在 AI、可视化和云端中心继续读取。");
                item.put("metrics", metrics);
                item.put("readiness", "已入云端");
                item.put("route", "/cloud");
                item.put("cloudSaved", true);
                item.put("canSync", false);
                Map<String, Object> payload = parseMap(row.get("payload_json"));
                boolean official = Boolean.TRUE.equals(payload.get("official"));
                boolean readOnly = official || Boolean.TRUE.equals(payload.get("readOnly"));
                item.put("official", official);
                item.put("readOnly", readOnly);
                item.put("canManage", !readOnly);
                return item;
            }).toList();
    }

    private Map<String, Object> logCounts(Long runId) {
        Map<String, Object> counts = new LinkedHashMap<>();
        LOG_TABLE_LABELS.keySet().forEach(table -> counts.put(table, countTable(table, runId)));
        return counts;
    }

    private int countTable(String table, Long runId) {
        try {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE run_id = ?", Integer.class, runId);
            return count == null ? 0 : count;
        } catch (Exception e) {
            return 0;
        }
    }

    private void markCloudState(Long userId, List<Map<String, Object>> assets) {
        List<Map<String, Object>> rows = safeQuery("""
            SELECT JSON_UNQUOTE(JSON_EXTRACT(payload_json, '$.assetKey')) AS asset_key
            FROM ai_workspace_items
            WHERE user_id = ? AND JSON_EXTRACT(payload_json, '$.assetKey') IS NOT NULL
            """, userId);
        List<String> cloudKeys = rows.stream().map(row -> text(row.get("asset_key"))).filter(key -> !key.isBlank()).toList();
        assets.forEach(asset -> {
            if (Boolean.TRUE.equals(asset.get("cloudSaved"))) return;
            asset.put("cloudSaved", cloudKeys.contains(text(asset.get("key"))));
        });
    }

    private Map<String, Object> buildSummary(List<Map<String, Object>> assets) {
        long datasets = countType(assets, "dataset");
        long training = countType(assets, "training");
        long runs = countType(assets, "run") + countType(assets, "run_log");
        long analysis = countType(assets, "analysis_result") + countType(assets, "saved_view");
        long cloud = assets.stream().filter(asset -> Boolean.TRUE.equals(asset.get("cloudSaved"))).count();
        long ready = assets.stream().filter(asset -> {
            String readiness = text(asset.get("readiness"));
            return readiness.contains("可") || readiness.contains("已");
        }).count();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total", assets.size());
        summary.put("datasets", datasets);
        summary.put("training", training);
        summary.put("runs", runs);
        summary.put("analysis", analysis);
        summary.put("cloud", cloud);
        summary.put("ready", ready);
        return summary;
    }

    private long countType(List<Map<String, Object>> assets, String type) {
        return assets.stream().filter(asset -> type.equals(asset.get("type"))).count();
    }

    private String buildAssetContent(Map<String, Object> asset) {
        StringBuilder builder = new StringBuilder();
        builder.append("# ").append(text(asset.get("title"))).append("\n\n");
        builder.append("- 类型：").append(text(asset.get("typeLabel"))).append("\n");
        builder.append("- 状态：").append(text(asset.get("status"))).append("\n");
        builder.append("- 格式：").append(text(asset.get("format"))).append("\n");
        builder.append("- 来源：").append(text(asset.get("sourceType"))).append("\n");
        builder.append("- 可用性：").append(text(asset.get("readiness"))).append("\n\n");
        builder.append(text(asset.get("summary"))).append("\n\n");
        Object metrics = asset.get("metrics");
        if (metrics instanceof Map<?, ?> map && !map.isEmpty()) {
            builder.append("## 指标\n");
            map.forEach((key, value) -> builder.append("- ").append(key).append("：").append(value).append("\n"));
        }
        return builder.toString();
    }

    private Map<String, Object> base(
        String key,
        Object id,
        String type,
        String typeLabel,
        String title,
        String subtype,
        String status,
        String format,
        String sourceType,
        Object updatedAt
    ) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("key", key);
        item.put("id", id);
        item.put("type", type);
        item.put("typeLabel", typeLabel);
        item.put("title", blank(title, typeLabel));
        item.put("subtype", blank(subtype, typeLabel));
        item.put("status", blank(status, "unknown"));
        item.put("format", blank(format, type));
        item.put("sourceType", blank(sourceType, type));
        item.put("updatedAt", updatedAt == null ? "" : String.valueOf(updatedAt));
        item.put("official", false);
        item.put("readOnly", false);
        item.put("canManage", true);
        item.put("canSync", true);
        return item;
    }

    private List<Map<String, Object>> safeQuery(String sql, Long userId) {
        try {
            return jdbcTemplate.queryForList(sql, userId);
        } catch (Exception e) {
            return List.of();
        }
    }

    private String moduleLabel(String key) {
        return switch (key) {
            case "architecture" -> "模型结构";
            case "scalars" -> "标量曲线";
            case "images" -> "图像样本";
            case "audio" -> "音频记录";
            case "text" -> "文本日志";
            case "histograms" -> "直方图";
            case "embeddings" -> "向量嵌入";
            case "hparams" -> "超参数";
            case "profiler" -> "性能剖析";
            case "pr_curves" -> "PR 曲线";
            case "roc_curves" -> "ROC 曲线";
            default -> blank(key, "分析模块");
        };
    }

    private String workspaceTypeLabel(String type) {
        return switch (type) {
            case "image" -> "图片";
            case "file" -> "文件";
            case "text" -> "文本";
            case "context_bundle" -> "素材包";
            case "data_asset" -> "数据资产";
            default -> blank(type, "素材");
        };
    }

    private String readiness(String status, Integer sampleCount) {
        if ("ready".equals(status) && sampleCount != null && sampleCount > 0) return "可训练";
        if ("ready".equals(status)) return "缺少样本数";
        if ("error".equals(status)) return "需要处理异常";
        if ("processing".equals(status) || "uploading".equals(status)) return "处理中";
        return "待完善";
    }

    private String formatMb(Double mb) {
        if (mb == null || mb <= 0) return "-";
        return mb >= 1024 ? String.format("%.1f GB", mb / 1024) : String.format("%.0f MB", mb);
    }

    private String formatBytes(Long bytes) {
        if (bytes == null || bytes <= 0) return "-";
        if (bytes >= 1024L * 1024L * 1024L) return String.format("%.1f GB", bytes / 1024.0 / 1024.0 / 1024.0);
        if (bytes >= 1024L * 1024L) return String.format("%.1f MB", bytes / 1024.0 / 1024.0);
        if (bytes >= 1024L) return String.format("%.1f KB", bytes / 1024.0);
        return bytes + " B";
    }

    private String n(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private Map<String, Object> parseMap(Object value) {
        if (value == null) return Map.of();
        try {
            return objectMapper.readValue(String.valueOf(value), new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private String blank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private Long numberToLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.longValue();
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }
}
