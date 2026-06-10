package com.deepinsight.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ModelCatalogService {

    private static final Pattern METRIC_PAIR = Pattern.compile("'([^']+)'\\s*:\\s*'?([^',}]+)'?");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());

    private final BSARecClientService bsarecClientService;
    private volatile List<Map<String, Object>> cachedStaticCatalog;

    public List<Map<String, Object>> listModels() {
        Map<String, Object> bsarecHealth = bsarecClientService.health();
        return staticCatalog().stream()
            .map(this::copyModel)
            .map(model -> enrichRuntimeStatus(model, bsarecHealth))
            .toList();
    }

    public List<Map<String, Object>> listPredictionModels() {
        return listModels().stream()
            .filter(model -> "recommendation".equals(String.valueOf(model.get("taskType"))))
            .filter(model -> Boolean.TRUE.equals(model.get("canExecute")))
            .toList();
    }

    public boolean containsName(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        return listModels().stream().anyMatch(model -> name.equalsIgnoreCase(String.valueOf(model.get("name"))));
    }

    public List<String> modelNames() {
        return listModels().stream().map(model -> String.valueOf(model.get("name"))).toList();
    }

    private List<Map<String, Object>> staticCatalog() {
        List<Map<String, Object>> current = cachedStaticCatalog;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (cachedStaticCatalog == null) {
                cachedStaticCatalog = buildStaticCatalog();
            }
            return cachedStaticCatalog;
        }
    }

    private List<Map<String, Object>> buildStaticCatalog() {
        Path root = projectRoot();
        List<Map<String, Object>> models = new ArrayList<>();

        models.add(recommender(
            root,
            "bsarec-job",
            "BSARec Job",
            "BSARec 岗位推荐",
            "BSARec",
            "本地岗位推荐模型，包含 Job.txt 真实序列数据、BSARec_Job.pt 权重和 Flask 推理桥接服务。",
            "Job",
            "model/BSARec-main-api/src/data/Job.txt",
            DataFormat.USER_SEQUENCE,
            "model/BSARec-main-api/src/output/BSARec_Job.pt",
            "model/BSARec-main-api/src/output/BSARec_Job.log",
            "model/BSARec-main-api/bsarec_api/app.py",
            "model/BSARec-main-api/bsarec_env.yaml",
            "本地 Job.txt 岗位序列数据",
            true,
            linked("Embedding Dim", "64", "Hidden Dim", "64", "Transformer Blocks", "2", "Attention Heads", "2", "Alpha", "0.7", "c", "5")
        ));

        models.add(familyRecommender(root, "bsarec", "BSARec", "BSARec 序列推荐", "BSARec",
            "本地 BSARec 对比模型家族，包含多份真实序列数据，其中 Beauty 和 LastFM 已有权重与评估日志。",
            "model/similar-models/BSARec/src/data",
            List.of(
                dataAsset("Beauty", "model/similar-models/BSARec/src/data/Beauty.txt", DataFormat.USER_SEQUENCE),
                dataAsset("LastFM", "model/similar-models/BSARec/src/data/LastFM.txt", DataFormat.USER_SEQUENCE),
                dataAsset("ML-1M", "model/similar-models/BSARec/src/data/ML-1M.txt", DataFormat.USER_SEQUENCE),
                dataAsset("Sports_and_Outdoors", "model/similar-models/BSARec/src/data/Sports_and_Outdoors.txt", DataFormat.USER_SEQUENCE),
                dataAsset("Toys_and_Games", "model/similar-models/BSARec/src/data/Toys_and_Games.txt", DataFormat.USER_SEQUENCE),
                dataAsset("Yelp", "model/similar-models/BSARec/src/data/Yelp.txt", DataFormat.USER_SEQUENCE)
            ),
            List.of(
                "model/similar-models/BSARec/src/output/BSARec_Beauty_best.pt",
                "model/similar-models/BSARec/src/output/BSARec_LastFM_best.pt"
            ),
            List.of(
                "model/similar-models/BSARec/src/output/BSARec_Beauty_best.log",
                "model/similar-models/BSARec/src/output/BSARec_LastFM_best.log"
            ),
            "model/similar-models/BSARec/src/main.py",
            null,
            "BSARec 本地多数据集",
            linked("Embedding Dim", "64", "Hidden Dim", "64", "Transformer Blocks", "2", "Attention Heads", "2", "Max sequence length", "50")));

        models.add(familyRecommender(root, "bert4rec", "BERT4Rec", "BERT4Rec 双向序列推荐", "BERT4Rec",
            "本地 BERT4Rec 模型家族，包含 Beauty、ML-1M、Steam 真实交互数据和 ML-20M 数据归档。",
            "model/similar-models/BERT4Rec/data",
            List.of(
                dataAsset("beauty", "model/similar-models/BERT4Rec/data/beauty.txt", DataFormat.USER_ITEM),
                dataAsset("ml-1m", "model/similar-models/BERT4Rec/data/ml-1m.txt", DataFormat.USER_ITEM),
                dataAsset("steam", "model/similar-models/BERT4Rec/data/steam.txt", DataFormat.USER_ITEM),
                dataAsset("ml-20m", "model/similar-models/BERT4Rec/data/ml-20m.zip", DataFormat.ARCHIVE)
            ),
            List.of(),
            List.of(),
            "model/similar-models/BERT4Rec/run.py",
            "model/similar-models/BERT4Rec/bert_train/bert_config_ml-1m_64.json",
            "BERT4Rec 本地数据与配置",
            linked(
                "Hidden Size", "64",
                "Transformer Blocks", "2",
                "Attention Heads", "2",
                "Max position embeddings", "200",
                "Dropout", "0.2",
                "Batch Size", "256",
                "Learning Rate", "1e-4",
                "Max sequence length", "200",
                "Train steps", "400000"
            )));

        models.add(familyRecommender(root, "duorec", "DuoRec", "DuoRec 对比序列推荐", "DuoRec",
            "本地 DuoRec 模型目录。原目录没有 dataset/ml-100k，我已放入本项目真实 RecBole ml-100k 数据，便于按模型 README 直接识别。",
            "model/similar-models/DuoRec/dataset/ml-100k",
            List.of(dataAsset("ml-100k", "model/similar-models/DuoRec/dataset/ml-100k/ml-100k.inter", DataFormat.RECBOLE_ATOMIC)),
            List.of(),
            List.of(),
            "model/similar-models/DuoRec/run_seq.py",
            "model/similar-models/DuoRec/seq.yaml",
            "真实 ml-100k 数据，已复制到 DuoRec 期望目录",
            linked("Contrast", "us_x", "Similarity", "dot", "Batch Size", "256", "Learning Rate", "0.001", "Max sequence length", "50")));

        models.add(familyRecommender(root, "fearec", "FEARec", "FEARec 频域增强序列推荐", "FEARec",
            "本地 FEARec 模型目录。原目录只有空数据目录，我已放入本项目真实 RecBole ml-100k 数据，便于按模型 README 直接识别。",
            "model/similar-models/FEARec/dataset/ml-100k",
            List.of(dataAsset("ml-100k", "model/similar-models/FEARec/dataset/ml-100k/ml-100k.inter", DataFormat.RECBOLE_ATOMIC)),
            List.of(),
            List.of(),
            "model/similar-models/FEARec/run_seq.py",
            "model/similar-models/FEARec/seq.yaml",
            "真实 ml-100k 数据，已复制到 FEARec 期望目录",
            linked("Contrast", "us_x", "Similarity", "dot", "Batch Size", "256", "Learning Rate", "0.001", "Max sequence length", "50", "Epochs", "1000")));

        models.add(familyRecommender(root, "fmlprec", "FMLP-Rec", "FMLP-Rec 滤波增强推荐", "FMLP-Rec",
            "本地 FMLP-Rec 模型，包含 Beauty 真实数据、评估权重和测试指标日志。",
            "model/similar-models/FMLP-Rec/data",
            List.of(dataAsset("Beauty", "model/similar-models/FMLP-Rec/data/Beauty.txt", DataFormat.USER_SEQUENCE)),
            List.of("model/similar-models/FMLP-Rec/output/FMLPRec-Beauty-4eval.pt"),
            List.of("model/similar-models/FMLP-Rec/output/FMLPRec-Beauty-4eval.txt"),
            "model/similar-models/FMLP-Rec/main.py",
            null,
            "FMLP-Rec 本地 Beauty 数据与负采样文件",
            linked("Hidden Size", "64", "Filter-enhanced Blocks", "2", "Attention Heads", "2", "Batch Size", "256", "Learning Rate", "0.001", "Max sequence length", "50")));

        models.add(familyRecommender(root, "recbole", "RecBole", "RecBole 推荐框架", "RecBole",
            "本地 RecBole 框架目录，包含真实 ml-100k atomic 数据和大量模型配置。",
            "model/similar-models/RecBole/dataset/ml-100k",
            List.of(dataAsset("ml-100k", "model/similar-models/RecBole/dataset/ml-100k/ml-100k.inter", DataFormat.RECBOLE_ATOMIC)),
            List.of(),
            List.of(),
            "model/similar-models/RecBole/run_recbole.py",
            "model/similar-models/RecBole/recbole/properties/dataset/ml-100k.yaml",
            "RecBole 本地 ml-100k atomic 数据",
            linked(
                "Framework", "RecBole",
                "Dataset format", ".inter/.item/.user",
                "Default evaluator", "RecBole 配置驱动",
                "Batch Size", "2048",
                "Learning Rate", "0.001",
                "Epochs", "300",
                "Top-K", "10"
            )));

        models.add(familyRecommender(root, "sasrec", "SASRec", "SASRec 自注意力序列推荐", "SASRec",
            "本地 SASRec 模型家族，包含 Beauty、ML-1M、Steam、Video 真实交互数据。",
            "model/similar-models/SASRec/data",
            List.of(
                dataAsset("Beauty", "model/similar-models/SASRec/data/Beauty.txt", DataFormat.USER_ITEM),
                dataAsset("ml-1m", "model/similar-models/SASRec/data/ml-1m.txt", DataFormat.USER_ITEM),
                dataAsset("Steam", "model/similar-models/SASRec/data/Steam.txt", DataFormat.USER_ITEM),
                dataAsset("Video", "model/similar-models/SASRec/data/Video.txt", DataFormat.USER_ITEM)
            ),
            List.of(),
            List.of(),
            "model/similar-models/SASRec/main.py",
            null,
            "SASRec 本地多数据集",
            linked("Hidden Units", "50", "Transformer Blocks", "2", "Attention Heads", "1", "Batch Size", "128", "Learning Rate", "0.001", "Max sequence length", "50")));

        models.add(familyRecommender(root, "tisasrec", "TiSASRec", "TiSASRec 时间间隔序列推荐", "TiSASRec",
            "本地 TiSASRec 模型，包含 ML-1M 用户、物品、评分和时间戳数据。",
            "model/similar-models/TiSASRec/data",
            List.of(dataAsset("ml-1m", "model/similar-models/TiSASRec/data/ml-1m.txt", DataFormat.USER_ITEM_RATING_TIME)),
            List.of(),
            List.of(),
            "model/similar-models/TiSASRec/main.py",
            null,
            "TiSASRec 本地 ML-1M 时间戳数据",
            linked("Hidden Units", "50", "Transformer Blocks", "2", "Attention Heads", "1", "Time span", "256", "Batch Size", "128", "Learning Rate", "0.001")));

        return List.copyOf(models);
    }

    private Map<String, Object> familyRecommender(
        Path root,
        String id,
        String name,
        String displayNameZh,
        String architecture,
        String description,
        String dataRootPath,
        List<DataAsset> datasets,
        List<String> artifactPaths,
        List<String> logPaths,
        String entrypointPath,
        String configPath,
        String dataSource,
        Map<String, Object> parameterSummary
    ) {
        Path dataRoot = resolve(root, dataRootPath);
        Path entrypoint = resolve(root, entrypointPath);
        Path config = resolve(root, configPath);
        List<Path> artifacts = artifactPaths.stream().map(path -> resolve(root, path)).toList();
        List<Path> logs = logPaths.stream().map(path -> resolve(root, path)).toList();
        List<Map<String, Object>> dataAssets = new ArrayList<>();

        long totalRecords = 0;
        long totalUsers = 0;
        long totalItems = 0;
        long totalInteractions = 0;
        long totalBytes = 0;
        long totalRatingCount = 0;
        double totalRatingSum = 0.0;
        Double minRating = null;
        Double maxRating = null;
        Long minTimestamp = null;
        Long maxTimestamp = null;
        int maxSequenceLength = 0;
        int existingDatasets = 0;
        List<String> datasetNames = new ArrayList<>();

        for (DataAsset asset : datasets) {
            Path dataset = resolve(root, asset.path());
            DatasetStats stats = datasetStats(dataset, asset.format());
            boolean exists = dataset != null && Files.isRegularFile(dataset);
            if (exists) {
                existingDatasets++;
            }
            datasetNames.add(asset.name());
            totalRecords += stats.records;
            totalUsers += stats.users;
            totalItems += stats.items;
            totalInteractions += stats.interactions;
            totalBytes += fileSize(dataset);
            maxSequenceLength = Math.max(maxSequenceLength, stats.maxSequenceLength);
            if (stats.ratingCount > 0) {
                totalRatingCount += stats.ratingCount;
                totalRatingSum += stats.ratingSum;
                minRating = minRating == null ? stats.minRating : Math.min(minRating, stats.minRating);
                maxRating = maxRating == null ? stats.maxRating : Math.max(maxRating, stats.maxRating);
            }
            if (stats.minTimestamp != null) {
                minTimestamp = minTimestamp == null ? stats.minTimestamp : Math.min(minTimestamp, stats.minTimestamp);
                maxTimestamp = maxTimestamp == null ? stats.maxTimestamp : Math.max(maxTimestamp, stats.maxTimestamp);
            }
            dataAssets.add(datasetAssetSummary(root, dataset, asset, stats, exists));
        }

        boolean datasetExists = existingDatasets > 0;
        boolean artifactExists = artifacts.stream().anyMatch(path -> path != null && Files.isRegularFile(path));
        boolean logExists = logs.stream().anyMatch(path -> path != null && Files.isRegularFile(path));
        boolean codeExists = entrypoint != null && Files.isRegularFile(entrypoint);
        Map<String, Object> metrics = firstMetricsFromLogs(logs);
        String integrationStatus = integrationStatus(datasetExists, artifactExists, logExists, codeExists, false);

        LinkedHashMap<String, Object> model = new LinkedHashMap<>();
        model.put("id", id);
        model.put("name", name);
        model.put("displayNameZh", displayNameZh);
        model.put("taskType", "recommendation");
        model.put("taskTypeZh", "推荐系统");
        model.put("architecture", architecture);
        model.put("framework", frameworkFor(architecture));
        model.put("modelGroup", architecture);
        model.put("description", description);
        model.put("descriptionZh", description);
        model.put("catalogSource", "本地模型目录");
        model.put("metrics", metrics.isEmpty()
            ? unavailableMetrics("当前模型目录没有可读取的评估日志。")
            : metrics);
        model.put("datasetSummary", familyDatasetSummary(
            datasetNames,
            dataSource,
            existingDatasets,
            datasets.size(),
            totalUsers,
            totalItems,
            totalInteractions,
            totalRecords,
            maxSequenceLength,
            totalBytes,
            totalRatingCount,
            totalRatingSum,
            minRating,
            maxRating,
            minTimestamp,
            maxTimestamp
        ));
        model.put("trainingSummary", familyTrainingSummary(root, artifacts, logs, entrypoint, config));
        model.put("parameterSummary", parameterSummary);
        model.put("visualProfile", visualProfile(datasetExists, artifactExists, logExists, codeExists, false));
        model.put("dataAssets", dataAssets);
        model.put("isOfficial", true);
        model.put("official", true);
        model.put("readOnly", true);
        model.put("canManage", false);
        model.put("canSync", false);
        model.put("databaseBacked", false);
        model.put("online", false);
        model.put("canExecute", false);
        model.put("integrationStatus", integrationStatus);
        model.put("statusLabel", statusLabelFor(integrationStatus));
        model.put("latencyMs", null);
        model.put("endpoint", null);
        model.put("serviceUrl", null);
        model.put("artifactExists", artifactExists);
        model.put("datasetExists", datasetExists);
        model.put("artifactPath", artifacts.stream().filter(path -> path != null && Files.isRegularFile(path)).findFirst().map(path -> pathText(root, path)).orElse(""));
        model.put("dataPath", pathText(root, dataRoot));
        model.put("entrypointPath", pathText(root, entrypoint));
        model.put("configPath", pathText(root, config));
        model.put("metricSource", metricSourceText(root, logs));
        model.put("statusReason", statusReason(datasetExists, artifactExists, logExists, codeExists, false));
        model.put("dataSource", dataSource);
        return model;
    }

    private DataAsset dataAsset(String name, String path, DataFormat format) {
        return new DataAsset(name, path, format);
    }

    private Map<String, Object> datasetAssetSummary(Path root, Path dataset, DataAsset asset, DatasetStats stats, boolean exists) {
        LinkedHashMap<String, Object> summary = new LinkedHashMap<>();
        summary.put("Name", asset.name());
        summary.put("Path", pathText(root, dataset));
        summary.put("Format", asset.format().label);
        summary.put("User scale", number(stats.users));
        summary.put("Item scale", number(stats.items));
        summary.put("Interactions", number(stats.interactions));
        summary.put("Records", number(stats.records));
        summary.put("Avg interactions/user", stats.users > 0 ? decimal(stats.avgInteractionsPerUser) : "Not calculated");
        summary.put("Density", stats.users > 0 && stats.items > 0 ? percent(stats.density) : "Not calculated");
        summary.put("Rating range", ratingRange(stats));
        summary.put("Rating mean", stats.meanRating == null ? "Not recorded" : decimal(stats.meanRating));
        summary.put("Timestamp range", timestampRange(stats));
        summary.put("Status", exists ? "exists" : "missing");
        return summary;
    }

    private Map<String, Object> familyDatasetSummary(
        List<String> datasetNames,
        String dataSource,
        int existingDatasets,
        int datasetCount,
        long totalUsers,
        long totalItems,
        long totalInteractions,
        long totalRecords,
        int maxSequenceLength,
        long totalBytes,
        long totalRatingCount,
        double totalRatingSum,
        Double minRating,
        Double maxRating,
        Long minTimestamp,
        Long maxTimestamp
    ) {
        LinkedHashMap<String, Object> summary = new LinkedHashMap<>();
        summary.put("Dataset", String.join(", ", datasetNames));
        summary.put("Data source", dataSource);
        summary.put("Dataset files", existingDatasets + " / " + datasetCount);
        summary.put("User scale", number(totalUsers));
        summary.put("Item scale", number(totalItems));
        summary.put("Interactions", number(totalInteractions));
        summary.put("Records", number(totalRecords));
        summary.put("Max sequence length", maxSequenceLength > 0 ? number(maxSequenceLength) : "Not calculated");
        summary.put("Avg interactions/user", totalUsers > 0 ? decimal((double) totalInteractions / totalUsers) : "Not calculated");
        summary.put("Density", totalUsers > 0 && totalItems > 0 ? percent((double) totalInteractions / ((double) totalUsers * totalItems)) : "Not calculated");
        summary.put("Rating range", minRating == null || maxRating == null ? "Not recorded" : String.format(Locale.US, "%.1f - %.1f", minRating, maxRating));
        summary.put("Rating mean", totalRatingCount == 0 ? "Not recorded" : decimal(totalRatingSum / totalRatingCount));
        summary.put("Timestamp range", minTimestamp == null || maxTimestamp == null
            ? "Not recorded"
            : DATE_FORMAT.format(Instant.ofEpochSecond(minTimestamp)) + " - " + DATE_FORMAT.format(Instant.ofEpochSecond(maxTimestamp)));
        summary.put("File size", formatBytes(totalBytes));
        summary.put("Stats source", "Calculated by scanning local dataset files; cross-dataset IDs are not merged.");
        return summary;
    }

    private Map<String, Object> firstMetricsFromLogs(List<Path> logs) {
        for (Path log : logs) {
            Map<String, Object> metrics = latestMetricsFromLog(log);
            if (!metrics.isEmpty()) {
                return metrics;
            }
        }
        return Map.of();
    }

    private Map<String, Object> familyTrainingSummary(
        Path root,
        List<Path> artifacts,
        List<Path> logs,
        Path entrypoint,
        Path config
    ) {
        long artifactCount = artifacts.stream().filter(path -> path != null && Files.isRegularFile(path)).count();
        long logCount = logs.stream().filter(path -> path != null && Files.isRegularFile(path)).count();
        return linked(
            "权重文件", artifactCount > 0 ? artifactCount + " 个" : "缺失",
            "评估日志", logCount > 0 ? logCount + " 个" : "缺失",
            "入口脚本", entrypoint != null && Files.isRegularFile(entrypoint) ? pathText(root, entrypoint) : "缺失",
            "配置文件", config != null && Files.isRegularFile(config) ? pathText(root, config) : "不需要或未提供",
            "推理服务", "未注册后端推理服务"
        );
    }

    private String metricSourceText(Path root, List<Path> logs) {
        List<String> existingLogs = logs.stream()
            .filter(path -> path != null && Files.isRegularFile(path))
            .map(path -> pathText(root, path))
            .toList();
        return existingLogs.isEmpty() ? "当前目录没有评估日志" : String.join("；", existingLogs);
    }

    private void addBsaRecFamily(List<Map<String, Object>> models, Path root) {
        Map<String, Object> params = linked(
            "Embedding Dim", "64",
            "Hidden Dim", "64",
            "Transformer Blocks", "2",
            "Attention Heads", "2",
            "Max sequence length", "50"
        );
        models.add(recommender(root, "bsarec-beauty", "BSARec Beauty", "BSARec Beauty", "BSARec",
            "BSARec checkpoint and evaluation log for Beauty.",
            "Beauty", "model/similar-models/BSARec/src/data/Beauty.txt", DataFormat.USER_SEQUENCE,
            "model/similar-models/BSARec/src/output/BSARec_Beauty_best.pt",
            "model/similar-models/BSARec/src/output/BSARec_Beauty_best.log",
            "model/similar-models/BSARec/src/main.py", null, "local BSARec Beauty data", false, params));
        models.add(recommender(root, "bsarec-lastfm", "BSARec LastFM", "BSARec LastFM", "BSARec",
            "BSARec checkpoint and evaluation log for LastFM.",
            "LastFM", "model/similar-models/BSARec/src/data/LastFM.txt", DataFormat.USER_SEQUENCE,
            "model/similar-models/BSARec/src/output/BSARec_LastFM_best.pt",
            "model/similar-models/BSARec/src/output/BSARec_LastFM_best.log",
            "model/similar-models/BSARec/src/main.py", null, "local BSARec LastFM data", false, params));
        models.add(recommender(root, "bsarec-ml-1m", "BSARec ML-1M", "BSARec ML-1M", "BSARec",
            "BSARec source tree with ML-1M sequence data.",
            "ML-1M", "model/similar-models/BSARec/src/data/ML-1M.txt", DataFormat.USER_SEQUENCE,
            null, null, "model/similar-models/BSARec/src/main.py", null, "local BSARec ML-1M data", false, params));
        models.add(recommender(root, "bsarec-sports", "BSARec Sports and Outdoors", "BSARec Sports and Outdoors", "BSARec",
            "BSARec source tree with Sports and Outdoors sequence data.",
            "Sports_and_Outdoors", "model/similar-models/BSARec/src/data/Sports_and_Outdoors.txt", DataFormat.USER_SEQUENCE,
            null, null, "model/similar-models/BSARec/src/main.py", null, "local BSARec Sports and Outdoors data", false, params));
        models.add(recommender(root, "bsarec-toys", "BSARec Toys and Games", "BSARec Toys and Games", "BSARec",
            "BSARec source tree with Toys and Games sequence data.",
            "Toys_and_Games", "model/similar-models/BSARec/src/data/Toys_and_Games.txt", DataFormat.USER_SEQUENCE,
            null, null, "model/similar-models/BSARec/src/main.py", null, "local BSARec Toys and Games data", false, params));
        models.add(recommender(root, "bsarec-yelp", "BSARec Yelp", "BSARec Yelp", "BSARec",
            "BSARec source tree with Yelp sequence data.",
            "Yelp", "model/similar-models/BSARec/src/data/Yelp.txt", DataFormat.USER_SEQUENCE,
            null, null, "model/similar-models/BSARec/src/main.py", null, "local BSARec Yelp data", false, params));
    }

    private void addBert4RecFamily(List<Map<String, Object>> models, Path root) {
        Map<String, Object> params = linked(
            "Hidden Size", "64",
            "Transformer Blocks", "2",
            "Attention Heads", "2",
            "Max position embeddings", "200",
            "Dropout", "0.2"
        );
        models.add(recommender(root, "bert4rec-beauty", "BERT4Rec Beauty", "BERT4Rec Beauty", "BERT4Rec",
            "BERT4Rec bidirectional sequence recommendation code with Beauty interaction data and JSON model config.",
            "beauty", "model/similar-models/BERT4Rec/data/beauty.txt", DataFormat.USER_ITEM,
            null, null, "model/similar-models/BERT4Rec/run.py",
            "model/similar-models/BERT4Rec/bert_train/bert_config_beauty_64.json",
            "local BERT4Rec Beauty data", false, params));
        models.add(recommender(root, "bert4rec-ml-1m", "BERT4Rec ML-1M", "BERT4Rec ML-1M", "BERT4Rec",
            "BERT4Rec bidirectional sequence recommendation code with ML-1M interaction data and JSON model config.",
            "ml-1m", "model/similar-models/BERT4Rec/data/ml-1m.txt", DataFormat.USER_ITEM,
            null, null, "model/similar-models/BERT4Rec/run.py",
            "model/similar-models/BERT4Rec/bert_train/bert_config_ml-1m_64.json",
            "local BERT4Rec ML-1M data", false, params));
        models.add(recommender(root, "bert4rec-steam", "BERT4Rec Steam", "BERT4Rec Steam", "BERT4Rec",
            "BERT4Rec bidirectional sequence recommendation code with Steam interaction data and JSON model config.",
            "steam", "model/similar-models/BERT4Rec/data/steam.txt", DataFormat.USER_ITEM,
            null, null, "model/similar-models/BERT4Rec/run.py",
            "model/similar-models/BERT4Rec/bert_train/bert_config_steam_64.json",
            "local BERT4Rec Steam data", false, params));
        models.add(recommender(root, "bert4rec-ml-20m-archive", "BERT4Rec ML-20M Archive", "BERT4Rec ML-20M 数据归档", "BERT4Rec",
            "BERT4Rec local ML-20M archive. It is registered as a data asset, not as a trained checkpoint.",
            "ml-20m", "model/similar-models/BERT4Rec/data/ml-20m.zip", DataFormat.ARCHIVE,
            null, null, "model/similar-models/BERT4Rec/run.py",
            "model/similar-models/BERT4Rec/bert_train/bert_config_ml-20m_64.json",
            "local BERT4Rec ML-20M zip archive", false, params));
    }

    private void addSasRecFamily(List<Map<String, Object>> models, Path root) {
        Map<String, Object> params = linked(
            "Hidden Units", "50",
            "Transformer Blocks", "2",
            "Attention Heads", "1",
            "Batch Size", "128",
            "Learning Rate", "0.001",
            "Max sequence length", "50"
        );
        models.add(recommender(root, "sasrec-beauty", "SASRec Beauty", "SASRec Beauty", "SASRec",
            "SASRec TensorFlow source with Beauty sequence data.",
            "Beauty", "model/similar-models/SASRec/data/Beauty.txt", DataFormat.USER_ITEM,
            null, null, "model/similar-models/SASRec/main.py", null, "local SASRec Beauty data", false, params));
        models.add(recommender(root, "sasrec-ml-1m", "SASRec ML-1M", "SASRec ML-1M", "SASRec",
            "SASRec TensorFlow source with ML-1M sequence data.",
            "ml-1m", "model/similar-models/SASRec/data/ml-1m.txt", DataFormat.USER_ITEM,
            null, null, "model/similar-models/SASRec/main.py", null, "local SASRec ML-1M data", false, params));
        models.add(recommender(root, "sasrec-steam", "SASRec Steam", "SASRec Steam", "SASRec",
            "SASRec TensorFlow source with Steam sequence data.",
            "Steam", "model/similar-models/SASRec/data/Steam.txt", DataFormat.USER_ITEM,
            null, null, "model/similar-models/SASRec/main.py", null, "local SASRec Steam data", false, params));
        models.add(recommender(root, "sasrec-video", "SASRec Video", "SASRec Video", "SASRec",
            "SASRec TensorFlow source with Amazon Video sequence data.",
            "Video", "model/similar-models/SASRec/data/Video.txt", DataFormat.USER_ITEM,
            null, null, "model/similar-models/SASRec/main.py", null, "local SASRec Video data", false, params));
    }

    private void addTiSasRecFamily(List<Map<String, Object>> models, Path root) {
        models.add(recommender(root, "tisasrec-ml-1m", "TiSASRec ML-1M", "TiSASRec ML-1M", "TiSASRec",
            "Time interval aware SASRec source with ML-1M user-item-rating-timestamp data.",
            "ml-1m", "model/similar-models/TiSASRec/data/ml-1m.txt", DataFormat.USER_ITEM_RATING_TIME,
            null, null, "model/similar-models/TiSASRec/main.py", null,
            "local TiSASRec ML-1M timestamp data", false,
            linked("Hidden Units", "50", "Transformer Blocks", "2", "Attention Heads", "1", "Time span", "256", "Batch Size", "128", "Learning Rate", "0.001")));
    }

    private void addFmlpRecFamily(List<Map<String, Object>> models, Path root) {
        models.add(recommender(root, "fmlprec-beauty", "FMLP-Rec Beauty", "FMLP-Rec Beauty", "FMLP-Rec",
            "FMLP-Rec filter-enhanced MLP recommender with Beauty data, evaluation checkpoint, and recorded test metrics.",
            "Beauty", "model/similar-models/FMLP-Rec/data/Beauty.txt", DataFormat.USER_SEQUENCE,
            "model/similar-models/FMLP-Rec/output/FMLPRec-Beauty-4eval.pt",
            "model/similar-models/FMLP-Rec/output/FMLPRec-Beauty-4eval.txt",
            "model/similar-models/FMLP-Rec/main.py", null,
            "local FMLP-Rec Beauty data and negative sample file", false,
            linked("Hidden Size", "64", "Filter-enhanced Blocks", "2", "Attention Heads", "2", "Batch Size", "256", "Learning Rate", "0.001", "Max sequence length", "50")));
    }

    private void addRecBoleFamily(List<Map<String, Object>> models, Path root) {
        models.add(recommender(root, "recbole-ml-100k", "RecBole ML-100K", "RecBole ML-100K", "RecBole",
            "RecBole framework sample with real ml-100k atomic data.",
            "ml-100k", "model/similar-models/RecBole/dataset/ml-100k/ml-100k.inter", DataFormat.RECBOLE_ATOMIC,
            null, null, "model/similar-models/RecBole/run_recbole.py",
            "model/similar-models/RecBole/recbole/properties/dataset/ml-100k.yaml",
            "local RecBole ml-100k atomic data", false,
            linked("Framework", "RecBole", "Dataset format", ".inter/.item/.user", "Default evaluator", "RecBole config driven")));
    }

    private void addDuoRecFamily(List<Map<String, Object>> models, Path root) {
        models.add(recommender(root, "duorec-ml-100k", "DuoRec ML-100K", "DuoRec ML-100K", "DuoRec",
            "DuoRec contrastive sequential recommender with ml-100k data placed under its expected dataset directory.",
            "ml-100k", "model/similar-models/DuoRec/dataset/ml-100k/ml-100k.inter", DataFormat.RECBOLE_ATOMIC,
            null, null, "model/similar-models/DuoRec/run_seq.py",
            "model/similar-models/DuoRec/seq.yaml",
            "local ml-100k data copied into DuoRec dataset path", false,
            linked("Contrast", "us_x", "Similarity", "dot", "Batch Size", "256", "Learning Rate", "0.001", "Max sequence length", "50")));
    }

    private void addFeaRecFamily(List<Map<String, Object>> models, Path root) {
        models.add(recommender(root, "fearec-ml-100k", "FEARec ML-100K", "FEARec ML-100K", "FEARec",
            "FEARec frequency-enhanced sequential recommender with ml-100k data placed under its expected dataset directory.",
            "ml-100k", "model/similar-models/FEARec/dataset/ml-100k/ml-100k.inter", DataFormat.RECBOLE_ATOMIC,
            null, null, "model/similar-models/FEARec/run_seq.py",
            "model/similar-models/FEARec/seq.yaml",
            "local ml-100k data copied into FEARec dataset path", false,
            linked("Contrast", "us_x", "Similarity", "dot", "Batch Size", "256", "Learning Rate", "0.001", "Max sequence length", "50", "Epochs", "1000")));
    }

    private Map<String, Object> recommender(
        Path root,
        String id,
        String name,
        String displayNameZh,
        String architecture,
        String description,
        String datasetName,
        String dataPath,
        DataFormat dataFormat,
        String artifactPath,
        String logPath,
        String entrypointPath,
        String configPath,
        String dataSource,
        boolean callableThroughBackend,
        Map<String, Object> parameterSummary
    ) {
        Path dataset = resolve(root, dataPath);
        Path artifact = resolve(root, artifactPath);
        Path log = resolve(root, logPath);
        Path entrypoint = resolve(root, entrypointPath);
        Path config = resolve(root, configPath);
        DatasetStats stats = datasetStats(dataset, dataFormat);
        boolean datasetExists = Files.isRegularFile(dataset);
        boolean artifactExists = artifact != null && Files.isRegularFile(artifact);
        boolean logExists = log != null && Files.isRegularFile(log);
        boolean codeExists = entrypoint != null && Files.isRegularFile(entrypoint);
        Map<String, Object> metrics = latestMetricsFromLog(log);

        LinkedHashMap<String, Object> model = new LinkedHashMap<>();
        model.put("id", id);
        model.put("name", name);
        model.put("displayNameZh", displayNameZh);
        model.put("taskType", "recommendation");
        model.put("taskTypeZh", "推荐系统");
        model.put("architecture", architecture);
        model.put("framework", frameworkFor(architecture));
        model.put("modelGroup", architecture);
        model.put("description", description);
        model.put("descriptionZh", description);
        model.put("catalogSource", "本地模型目录");
        model.put("metrics", metrics.isEmpty()
            ? unavailableMetrics("当前模型目录没有可读取的评估日志。")
            : metrics);
        model.put("datasetSummary", datasetSummary(datasetName, dataSource, dataFormat, dataset, stats));
        model.put("trainingSummary", trainingSummary(root, artifact, log, entrypoint, config, callableThroughBackend));
        model.put("parameterSummary", withDataFormat(parameterSummary, dataFormat));
        model.put("visualProfile", visualProfile(datasetExists, artifactExists, logExists, codeExists, callableThroughBackend));
        model.put("isOfficial", true);
        model.put("official", true);
        model.put("readOnly", true);
        model.put("canManage", false);
        model.put("canSync", false);
        model.put("databaseBacked", false);
        model.put("online", false);
        model.put("canExecute", callableThroughBackend && artifactExists && datasetExists);
        String integrationStatus = integrationStatus(datasetExists, artifactExists, logExists, codeExists, callableThroughBackend);
        model.put("integrationStatus", integrationStatus);
        model.put("statusLabel", statusLabelFor(integrationStatus));
        model.put("latencyMs", null);
        model.put("endpoint", callableThroughBackend ? "/api/v1/prediction/recommend" : null);
        model.put("serviceUrl", callableThroughBackend ? "http://127.0.0.1:5000" : null);
        model.put("artifactExists", artifactExists);
        model.put("datasetExists", datasetExists);
        model.put("artifactPath", pathText(root, artifact));
        model.put("dataPath", pathText(root, dataset));
        model.put("entrypointPath", pathText(root, entrypoint));
        model.put("configPath", pathText(root, config));
        model.put("metricSource", logExists ? pathText(root, log) : "当前目录没有评估日志");
        model.put("statusReason", statusReason(datasetExists, artifactExists, logExists, codeExists, callableThroughBackend));
        model.put("dataSource", dataSource);
        return model;
    }

    private Map<String, Object> enrichRuntimeStatus(Map<String, Object> model, Map<String, Object> bsarecHealth) {
        if (!"bsarec-job".equals(String.valueOf(model.get("id")))) {
            return model;
        }
        boolean artifactExists = Boolean.TRUE.equals(model.get("artifactExists"));
        boolean datasetExists = Boolean.TRUE.equals(model.get("datasetExists"));
        boolean serviceOnline = Boolean.TRUE.equals(bsarecHealth.get("online"));
        model.put("online", serviceOnline);
        model.put("canExecute", artifactExists && datasetExists);
        model.put("integrationStatus", serviceOnline ? "online" : "service-offline");
        model.put("statusLabel", serviceOnline ? "在线" : "服务离线");
        model.put("latencyMs", bsarecHealth.get("elapsedMs"));
        model.put("serviceUrl", bsarecHealth.get("serviceUrl"));
        model.put("runtimeHealth", bsarecHealth);
        model.put("jobInfoExists", Files.isRegularFile(projectRoot().resolve("model/BSARec-main-api/output/item_id_to_job_info.json")));
        model.put("statusReason", serviceOnline
            ? "BSARec Flask 服务已响应 /health，后端代理可以调用 /recommend。"
            : "权重和 Job.txt 数据已存在，但 BSARec Flask /health 当前不可达。");
        model.put("visualProfile", linked(
            "数据就绪度", datasetExists ? 100 : 0,
            "权重就绪度", artifactExists ? 100 : 0,
            "指标可信度", 100,
            "代码就绪度", 100,
            "服务就绪度", serviceOnline ? 100 : 0
        ));
        return model;
    }

    private Map<String, Object> latestMetricsFromLog(Path log) {
        if (log == null || !Files.isRegularFile(log)) {
            return Map.of();
        }
        try {
            List<String> lines = Files.readAllLines(log, StandardCharsets.UTF_8);
            LinkedHashMap<String, Object> metrics = new LinkedHashMap<>();
            Object latestLoss = null;
            Object latestLossEpoch = null;
            for (int i = lines.size() - 1; i >= 0; i--) {
                String line = lines.get(i);
                LinkedHashMap<String, Object> pairs = metricPairsFromLine(line);
                if (latestLoss == null) {
                    String lossKey = firstExistingKey(pairs, "rec_loss", "loss", "Loss", "train_loss", "Train Loss", "masked_lm_loss");
                    if (lossKey != null) {
                        latestLoss = pairs.get(lossKey);
                        String epochKey = firstExistingKey(pairs, "epoch", "Epoch");
                        latestLossEpoch = epochKey == null ? null : pairs.get(epochKey);
                    }
                }

                if (metrics.isEmpty() && (line.contains("NDCG") || line.contains("HR@") || line.contains("HIT@") || line.contains("Recall@") || line.contains("MRR"))) {
                    metrics.putAll(pairs);
                    if (metrics.containsKey("HIT@10") && !metrics.containsKey("HR@10")) {
                        metrics.put("HR@10", metrics.get("HIT@10"));
                    }
                }

                if (!metrics.isEmpty() && latestLoss != null) {
                    break;
                }
            }
            if (latestLoss != null) {
                metrics.putIfAbsent("Loss", latestLoss);
                if (latestLossEpoch != null) {
                    metrics.putIfAbsent("Loss Epoch", latestLossEpoch);
                }
            }
            if (!metrics.isEmpty()) {
                return metrics;
            }
        } catch (IOException ignored) {
            return Map.of();
        }
        return Map.of();
    }

    private LinkedHashMap<String, Object> metricPairsFromLine(String line) {
        LinkedHashMap<String, Object> metrics = new LinkedHashMap<>();
        Matcher matcher = METRIC_PAIR.matcher(line);
        while (matcher.find()) {
            String key = matcher.group(1).trim();
            String value = matcher.group(2).trim();
            metrics.put(key, value);
        }
        return metrics;
    }

    private String firstExistingKey(Map<String, Object> values, String... keys) {
        for (String key : keys) {
            if (values.containsKey(key)) {
                return key;
            }
        }
        return null;
    }

    private DatasetStats datasetStats(Path path, DataFormat format) {
        if (path == null || !Files.isRegularFile(path)) {
            return DatasetStats.empty();
        }
        if (format == DataFormat.ARCHIVE) {
            return new DatasetStats(0, 0, 0, 0, 0, 0.0, 0.0, 0.0, 0, null, null, null, null, null, fileSize(path));
        }

        Set<String> users = new HashSet<>();
        Set<String> items = new HashSet<>();
        long records = 0;
        long interactions = 0;
        int maxSequence = 0;
        long ratingCount = 0;
        double ratingSum = 0.0;
        Double minRating = null;
        Double maxRating = null;
        Long minTimestamp = null;
        Long maxTimestamp = null;

        try (var lines = Files.lines(path, StandardCharsets.UTF_8)) {
            boolean[] firstLine = {true};
            for (String line : (Iterable<String>) lines::iterator) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                if (firstLine[0] && format == DataFormat.RECBOLE_ATOMIC) {
                    firstLine[0] = false;
                    continue;
                }
                firstLine[0] = false;
                records++;
                String[] parts = format == DataFormat.RECBOLE_ATOMIC ? trimmed.split("\\t+") : trimmed.split("\\s+");
                if (parts.length < 2) {
                    continue;
                }
                users.add(parts[0]);
                if (format == DataFormat.USER_ITEM_RATING_TIME || format == DataFormat.RECBOLE_ATOMIC || format == DataFormat.USER_ITEM) {
                    items.add(parts[1]);
                    interactions++;
                    maxSequence = Math.max(maxSequence, 1);
                    if (format == DataFormat.USER_ITEM_RATING_TIME || format == DataFormat.RECBOLE_ATOMIC) {
                        Double rating = parts.length > 2 ? parseDouble(parts[2]) : null;
                        if (rating != null) {
                            ratingCount++;
                            ratingSum += rating;
                            minRating = minRating == null ? rating : Math.min(minRating, rating);
                            maxRating = maxRating == null ? rating : Math.max(maxRating, rating);
                        }
                        Long timestamp = parts.length > 3 ? parseTimestamp(parts[3]) : null;
                        if (timestamp != null) {
                            minTimestamp = minTimestamp == null ? timestamp : Math.min(minTimestamp, timestamp);
                            maxTimestamp = maxTimestamp == null ? timestamp : Math.max(maxTimestamp, timestamp);
                        }
                    }
                    continue;
                }
                int sequenceLength = 0;
                for (int i = 1; i < parts.length; i++) {
                    items.add(parts[i]);
                    sequenceLength++;
                }
                interactions += sequenceLength;
                maxSequence = Math.max(maxSequence, sequenceLength);
            }
        } catch (IOException ignored) {
            return DatasetStats.empty();
        }
        double avgInteractionsPerUser = users.isEmpty() ? 0.0 : (double) interactions / users.size();
        double density = users.isEmpty() || items.isEmpty() ? 0.0 : (double) interactions / ((double) users.size() * items.size());
        Double meanRating = ratingCount == 0 ? null : ratingSum / ratingCount;
        return new DatasetStats(
            records,
            users.size(),
            items.size(),
            interactions,
            maxSequence,
            avgInteractionsPerUser,
            density,
            ratingSum,
            ratingCount,
            minRating,
            maxRating,
            meanRating,
            minTimestamp,
            maxTimestamp,
            fileSize(path)
        );
    }

    private Map<String, Object> datasetSummary(
        String datasetName,
        String dataSource,
        DataFormat format,
        Path dataset,
        DatasetStats stats
    ) {
        LinkedHashMap<String, Object> summary = new LinkedHashMap<>();
        summary.put("Dataset", datasetName);
        summary.put("Data source", dataSource);
        summary.put("Data format", format.label);
        summary.put("数据文件", dataset == null ? "缺失" : dataset.getFileName().toString());
        summary.put("文件大小", stats.fileSizeText);
        if (format == DataFormat.ARCHIVE) {
            summary.put("归档状态", Files.isRegularFile(dataset) ? "存在" : "缺失");
            return summary;
        }
        summary.put("User scale", number(stats.users));
        summary.put("Item scale", number(stats.items));
        summary.put("Interactions", number(stats.interactions));
        summary.put("Records", number(stats.records));
        summary.put("Max sequence length", stats.maxSequenceLength > 0 ? number(stats.maxSequenceLength) : "未计算");
        summary.put("Avg interactions/user", stats.users > 0 ? decimal(stats.avgInteractionsPerUser) : "Not calculated");
        summary.put("Density", stats.users > 0 && stats.items > 0 ? percent(stats.density) : "Not calculated");
        summary.put("Rating range", ratingRange(stats));
        summary.put("Rating mean", stats.meanRating == null ? "Not recorded" : decimal(stats.meanRating));
        summary.put("Timestamp range", timestampRange(stats));
        summary.put("Stats source", "Calculated by scanning the local dataset file.");
        return summary;
    }

    private Map<String, Object> trainingSummary(
        Path root,
        Path artifact,
        Path log,
        Path entrypoint,
        Path config,
        boolean callableThroughBackend
    ) {
        return linked(
            "权重文件", artifact != null && Files.isRegularFile(artifact) ? "存在" : "缺失",
            "评估日志", log != null && Files.isRegularFile(log) ? pathText(root, log) : "缺失",
            "入口脚本", entrypoint != null && Files.isRegularFile(entrypoint) ? pathText(root, entrypoint) : "缺失",
            "配置文件", config != null && Files.isRegularFile(config) ? pathText(root, config) : "不需要或未提供",
            "推理服务", callableThroughBackend ? "后端代理 /api/v1/prediction/recommend" : "未注册"
        );
    }

    private Map<String, Object> visualProfile(
        boolean datasetExists,
        boolean artifactExists,
        boolean logExists,
        boolean codeExists,
        boolean callableThroughBackend
    ) {
        return linked(
            "数据就绪度", datasetExists ? 100 : 0,
            "权重就绪度", artifactExists ? 100 : 0,
            "指标可信度", logExists ? 100 : 0,
            "代码就绪度", codeExists ? 100 : 0,
            "服务就绪度", callableThroughBackend ? 50 : 0
        );
    }

    private Map<String, Object> withDataFormat(Map<String, Object> parameterSummary, DataFormat format) {
        LinkedHashMap<String, Object> copy = new LinkedHashMap<>(parameterSummary);
        copy.put("Data format", format.label);
        return copy;
    }

    private String integrationStatus(
        boolean datasetExists,
        boolean artifactExists,
        boolean logExists,
        boolean codeExists,
        boolean callableThroughBackend
    ) {
        if (callableThroughBackend) {
            return "service-offline";
        }
        if (artifactExists && logExists && datasetExists) {
            return "artifact-and-log";
        }
        if (artifactExists && datasetExists) {
            return "artifact-only";
        }
        if (datasetExists && codeExists) {
            return "code-and-data";
        }
        if (datasetExists) {
            return "data-only";
        }
        return "missing-data";
    }

    private String statusLabelFor(String integrationStatus) {
        return switch (integrationStatus) {
            case "online" -> "在线";
            case "service-offline" -> "服务离线";
            case "artifact-and-log" -> "权重+日志";
            case "artifact-only" -> "仅有权重";
            case "code-and-data" -> "代码+数据";
            case "data-only" -> "仅有数据";
            case "missing-data" -> "缺少数据";
            default -> "未注册推理";
        };
    }

    private String statusReason(
        boolean datasetExists,
        boolean artifactExists,
        boolean logExists,
        boolean codeExists,
        boolean callableThroughBackend
    ) {
        if (callableThroughBackend) {
            return "本地数据和权重已登记，运行状态由 BSARec Flask 服务健康检查决定。";
        }
        if (artifactExists && logExists) {
            return "本地权重和评估日志已存在，但 DeepInsight 后端尚未为该模型注册推理接口。";
        }
        if (datasetExists && codeExists) {
            return "本地代码和真实数据已存在，但还没有登记权重或可调用的后端推理接口。";
        }
        if (datasetExists) {
            return "真实本地数据已存在，但未在预期位置找到可运行入口脚本。";
        }
        return "登记路径下没有找到可用数据文件。";
    }

    private Map<String, Object> unavailableMetrics(String reason) {
        return linked("指标状态", "暂无真实评估日志", "原因", reason);
    }

    private String frameworkFor(String architecture) {
        if ("BERT4Rec".equals(architecture) || "SASRec".equals(architecture) || "TiSASRec".equals(architecture)) {
            return "TensorFlow";
        }
        if ("RecBole".equals(architecture) || "DuoRec".equals(architecture) || "FEARec".equals(architecture)) {
            return "PyTorch / RecBole";
        }
        return "PyTorch";
    }

    private Map<String, Object> copyModel(Map<String, Object> source) {
        return new LinkedHashMap<>(source);
    }

    private Path resolve(Path root, String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return null;
        }
        return root.resolve(relativePath).normalize();
    }

    private String pathText(Path root, Path path) {
        if (path == null) {
            return "";
        }
        Path normalized = path.toAbsolutePath().normalize();
        Path normalizedRoot = root.toAbsolutePath().normalize();
        if (normalized.startsWith(normalizedRoot)) {
            return normalizedRoot.relativize(normalized).toString();
        }
        return normalized.toString();
    }

    private long fileSize(Path path) {
        try {
            return Files.isRegularFile(path) ? Files.size(path) : 0L;
        } catch (IOException ignored) {
            return 0L;
        }
    }

    private String number(long value) {
        return NumberFormat.getIntegerInstance(Locale.US).format(value);
    }

    private String decimal(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private String percent(double ratio) {
        return String.format(Locale.US, "%.4f%%", ratio * 100.0);
    }

    private String ratingRange(DatasetStats stats) {
        if (stats.minRating == null || stats.maxRating == null) {
            return "Not recorded";
        }
        return String.format(Locale.US, "%.1f - %.1f", stats.minRating, stats.maxRating);
    }

    private String timestampRange(DatasetStats stats) {
        if (stats.minTimestamp == null || stats.maxTimestamp == null) {
            return "Not recorded";
        }
        return DATE_FORMAT.format(Instant.ofEpochSecond(stats.minTimestamp))
            + " - "
            + DATE_FORMAT.format(Instant.ofEpochSecond(stats.maxTimestamp));
    }

    private Double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Long parseTimestamp(String value) {
        Double parsed = parseDouble(value);
        return parsed == null ? null : parsed.longValue();
    }

    private String formatBytes(long bytes) {
        if (bytes <= 0) {
            return "0 B";
        }
        double value = bytes;
        String[] units = {"B", "KB", "MB", "GB"};
        int unit = 0;
        while (value >= 1024 && unit < units.length - 1) {
            value /= 1024;
            unit++;
        }
        return String.format(Locale.US, "%.1f %s", value, units[unit]);
    }

    private Path projectRoot() {
        Path current = Paths.get("").toAbsolutePath().normalize();
        if (Files.isDirectory(current.resolve("model"))) {
            return current;
        }
        Path parent = current.getParent();
        if (parent != null && Files.isDirectory(parent.resolve("model"))) {
            return parent;
        }
        return current;
    }

    private static Map<String, Object> linked(Object... values) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < values.length; i += 2) {
            map.put(String.valueOf(values[i]), values[i + 1]);
        }
        return map;
    }

    private enum DataFormat {
        USER_SEQUENCE("用户序列文本"),
        USER_ITEM("用户-物品交互文本"),
        USER_ITEM_RATING_TIME("用户-物品-评分-时间戳文本"),
        RECBOLE_ATOMIC("RecBole atomic 交互文件"),
        ARCHIVE("数据归档文件");

        private final String label;

        DataFormat(String label) {
            this.label = label;
        }
    }

    private record DataAsset(String name, String path, DataFormat format) {
    }

    private record DatasetStats(
        long records,
        long users,
        long items,
        long interactions,
        int maxSequenceLength,
        double avgInteractionsPerUser,
        double density,
        double ratingSum,
        long ratingCount,
        Double minRating,
        Double maxRating,
        Double meanRating,
        Long minTimestamp,
        Long maxTimestamp,
        String fileSizeText
    ) {
        static DatasetStats empty() {
            return new DatasetStats(0, 0, 0, 0, 0, 0.0, 0.0, 0.0, 0, null, null, null, null, null, "0 B");
        }

        DatasetStats(
            long records,
            long users,
            long items,
            long interactions,
            int maxSequenceLength,
            double avgInteractionsPerUser,
            double density,
            double ratingSum,
            long ratingCount,
            Double minRating,
            Double maxRating,
            Double meanRating,
            Long minTimestamp,
            Long maxTimestamp,
            long bytes
        ) {
            this(
                records,
                users,
                items,
                interactions,
                maxSequenceLength,
                avgInteractionsPerUser,
                density,
                ratingSum,
                ratingCount,
                minRating,
                maxRating,
                meanRating,
                minTimestamp,
                maxTimestamp,
                formatStaticBytes(bytes)
            );
        }

        private static String formatStaticBytes(long bytes) {
            if (bytes <= 0) {
                return "0 B";
            }
            double value = bytes;
            String[] units = {"B", "KB", "MB", "GB"};
            int unit = 0;
            while (value >= 1024 && unit < units.length - 1) {
                value /= 1024;
                unit++;
            }
            return String.format(Locale.US, "%.1f %s", value, units[unit]);
        }
    }
}
