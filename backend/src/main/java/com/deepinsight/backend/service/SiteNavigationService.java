package com.deepinsight.backend.service;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class SiteNavigationService {

    private static final List<String> INTENT_WORDS = List.of(
        "打开", "跳转", "跳到", "前往", "进入", "去", "看看", "查看", "带我", "切到",
        "导航", "入口", "链接", "在哪", "在哪里", "页面", "open", "go", "show", "view", "navigate", "link"
    );

    private static final List<String> STRONG_INTENT_WORDS = List.of(
        "打开", "跳转", "跳到", "前往", "进入", "带我", "切到", "导航", "入口", "链接",
        "在哪", "在哪里", "页面", "open", "go", "navigate", "link"
    );

    private static final List<String> EXPLANATION_WORDS = List.of(
        "解释", "为什么", "原因", "原理", "是什么", "什么意思", "区别", "对比", "分析",
        "怎么理解", "如何理解", "怎么做", "如何做", "说明", "讲讲", "介绍"
    );

    private static final List<NavigationTarget> TARGETS = List.of(
        target("training", "模型总览", "/model-overview", "查看 9 个推荐系统模型的状态、指标、数据集、训练配置和参数画像。", "看看模型总览",
            "模型总览", "模型", "训练", "模型列表", "已接入模型", "model overview", "training", "/training"),
        target("prediction", "模型接入测试", "/model-access-test", "验证推荐系统样例输入、Top-K 输出、后端代理和服务健康状态。", "打开模型接入测试",
            "模型接入测试", "接入测试", "预测", "推理", "推荐测试", "模型测试", "prediction", "inference", "/prediction"),
        target("performance", "性能看板", "/performance-dashboard", "按 HR、NDCG、MRR、数据规模和接入成熟度查看推荐模型可视化对比。", "打开性能看板",
            "性能看板", "性能", "指标看板", "模型可视化", "模型分析", "对比图表", "viz", "performance", "/viz"),
        target("datasetViz", "数据集实时可视化", "/dataset-visualization", "上传或选择数据集，查看字段、分布、缺失、样例和可视化摘要。", "打开数据集实时可视化",
            "数据集实时可视化", "数据集可视化", "数据可视化", "上传数据集", "csv可视化", "dataset visualization", "/dataset-viz"),
        target("data", "数据中心", "/data-center", "统一管理数据集、素材、云端资源和 AI 上下文材料。", "进入数据中心",
            "数据中心", "数据管理", "数据", "云端中心", "云空间", "素材库", "文件库", "cloud", "workspace", "/data"),
        target("analysisWorkbench", "图表工作台", "/analysis-workbench", "查看保留的训练运行、预测记录和分析批次工作台。", "打开图表工作台",
            "图表工作台", "可视化分析", "分析工作台", "高级分析", "analysis workbench"),
        target("ai", "AI 工作区", "/ai-studio", "打开完整 AI 对话、素材上下文、深度思考和站内知识问答界面。", "打开 AI 工作区",
            "AI 工作区", "ai工作区", "AI 对话", "完整对话", "助手页面", "deepseek", "ai studio", "/ai"),
        target("knowledge", "知识中心", "/knowledge-base", "浏览知识图谱、文章详情和学习资料。", "打开知识中心",
            "知识库", "知识中心", "知识", "文章", "教程", "资料", "knowledge", "/knowledge"),
        target("forum", "交流论坛", "/community-forum", "查看社区帖子、官方文章、经验分享和问题讨论。", "打开交流论坛",
            "论坛", "交流", "社区", "帖子", "官方文章", "forum", "/forum"),
        target("profile", "个人中心", "/profile", "查看个人资料、账号信息和用户设置。", "打开个人中心",
            "个人中心", "个人资料", "账号", "用户信息", "profile"),
        target("adminAi", "管理员 AI 配置", "/admin/ai", "配置 AI 服务商、模型、系统提示词、温度和上下文窗口。", "打开管理员 AI 配置",
            "AI配置", "ai配置", "管理员AI", "模型配置", "系统提示词", "admin ai"),
        target("adminKb", "管理员知识库", "/admin/kb", "维护知识库文档、知识节点、分类、标签和检索内容。", "打开管理员知识库",
            "知识库管理", "管理员知识库", "管理知识", "admin kb"),
        target("adminForum", "管理员社区", "/admin/forum", "维护论坛帖子、官方文章和社区内容。", "打开管理员社区",
            "论坛管理", "社区管理", "文章管理", "管理员论坛", "admin forum"),
        target("adminData", "管理员数据", "/admin/data", "管理用户上传数据集、训练任务和数据资产记录。", "打开管理员数据",
            "数据管理后台", "管理员数据", "数据后台", "admin data"),
        target("adminUsers", "管理员用户", "/admin/users", "管理用户账号、角色、权限和状态。", "打开管理员用户",
            "用户管理", "管理员用户", "账号管理", "权限管理", "admin users")
    );

    public Map<String, Object> resolve(String input) {
        String text = normalize(input);
        if (text.isBlank()) return Map.of();
        boolean hasIntent = INTENT_WORDS.stream().anyMatch(word -> text.contains(normalize(word)));
        boolean hasStrongIntent = STRONG_INTENT_WORDS.stream().anyMatch(word -> text.contains(normalize(word)));
        boolean asksForExplanation = EXPLANATION_WORDS.stream().anyMatch(word -> text.contains(normalize(word)));

        NavigationTarget bestTarget = null;
        int bestScore = 0;
        for (NavigationTarget target : TARGETS) {
            int score = scoreTarget(text, target);
            if (score > bestScore) {
                bestScore = score;
                bestTarget = target;
            }
        }
        if (bestTarget == null || bestScore <= 0) return Map.of();
        if (!hasIntent && bestScore < 90) return Map.of();
        if (!hasStrongIntent && asksForExplanation && bestScore < 82) return Map.of();

        String mode = hasStrongIntent ? "direct" : "suggested";
        String reason = hasStrongIntent
            ? "用户明确表达了页面入口或跳转意图"
            : "问题内容与该页面强相关，可作为下一步入口";
        return bestTarget.toMap(Math.min(0.97, 0.58 + bestScore / 120.0), mode, reason);
    }

    private int scoreTarget(String text, NavigationTarget target) {
        int score = text.contains(normalize(target.path())) ? 100 : 0;
        score += text.contains(normalize(target.label())) ? 40 + target.label().length() : 0;
        for (String alias : target.aliases()) {
            String normalized = normalize(alias);
            if (!normalized.isBlank() && text.contains(normalized)) {
                score += 12 + Math.min(24, normalized.length());
            }
        }
        return score;
    }

    private static NavigationTarget target(String key, String label, String path, String description, String promptHint, String... aliases) {
        return new NavigationTarget(key, label, path, description, promptHint, List.of(aliases));
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("\\s+", "").trim();
    }

    private record NavigationTarget(
        String key,
        String label,
        String path,
        String description,
        String promptHint,
        List<String> aliases
    ) {
        Map<String, Object> toMap(double confidence, String mode, String reason) {
            LinkedHashMap<String, Object> map = new LinkedHashMap<>();
            map.put("key", key);
            map.put("label", label);
            map.put("path", path);
            map.put("description", description);
            map.put("promptHint", promptHint);
            map.put("confidence", confidence);
            map.put("mode", mode);
            map.put("reason", reason);
            map.put("reply", "direct".equals(mode)
                ? "已为你准备好「" + label + "」的跳转入口。"
                : "这个问题可能需要配合「" + label + "」继续看，我把入口放在下面。");
            return map;
        }
    }
}
