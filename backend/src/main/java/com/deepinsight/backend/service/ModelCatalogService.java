package com.deepinsight.backend.service;

import com.deepinsight.backend.entity.ModelArticle;
import com.deepinsight.backend.entity.ModelRegistry;
import com.deepinsight.backend.repository.ModelArticleRepository;
import com.deepinsight.backend.repository.ModelRegistryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ModelCatalogService {

    private static final OfficialModel[] OFFICIAL_MODELS = {
        new OfficialModel("ResNet-50", "残差网络 50 层", "classification", "图像分类", 25.6, "224x224x3", "pytorch"),
        new OfficialModel("ResNet-101", "残差网络 101 层", "classification", "图像分类", 44.5, "224x224x3", "pytorch"),
        new OfficialModel("VGG-19", "VGG 19 层卷积网络", "classification", "图像分类", 143.7, "224x224x3", "pytorch"),
        new OfficialModel("EfficientNet-B4", "高效卷积网络 B4", "classification", "图像分类", 19.3, "380x380x3", "pytorch"),
        new OfficialModel("ViT-B/16", "视觉 Transformer 基础版", "classification", "图像分类", 86.6, "384x384x3", "pytorch"),
        new OfficialModel("Swin-T", "Swin Transformer 轻量版", "classification", "图像分类", 28.3, "224x224x3", "pytorch"),
        new OfficialModel("ConvNeXt-T", "现代化卷积网络轻量版", "classification", "图像分类", 28.6, "224x224x3", "pytorch"),
        new OfficialModel("MobileNetV3-L", "移动端高效网络大版", "classification", "图像分类", 5.4, "224x224x3", "pytorch"),
        new OfficialModel("DenseNet-201", "密集连接卷积网络 201 层", "classification", "图像分类", 20.0, "224x224x3", "pytorch"),
        new OfficialModel("YOLOv8n", "YOLOv8 Nano 实时检测", "detection", "目标检测", 3.2, "640x640x3", "pytorch"),
        new OfficialModel("YOLOv8s", "YOLOv8 Small 目标检测", "detection", "目标检测", 11.2, "640x640x3", "pytorch"),
        new OfficialModel("DeepLabV3-RN50", "DeepLabV3 语义分割", "segmentation", "语义分割", 58.6, "513x513x3", "pytorch"),
        new OfficialModel("DeepFM", "深度因子分解推荐模型", "recommendation", "推荐系统", 5.0, "sparse", "tensorflow"),
        new OfficialModel("Wide&Deep", "宽深联合推荐模型", "recommendation", "推荐系统", 3.5, "sparse", "tensorflow"),
        new OfficialModel("NCF", "神经协同过滤模型", "recommendation", "推荐系统", 2.1, "dense", "pytorch"),
        new OfficialModel("DIN", "深度兴趣网络", "recommendation", "推荐系统", 4.8, "sparse", "tensorflow"),
        new OfficialModel("BERT-Base", "BERT 文本理解基础版", "nlp", "自然语言处理", 110.0, "512 tokens", "pytorch"),
        new OfficialModel("GPT-2", "GPT-2 文本生成模型", "nlp", "自然语言处理", 124.0, "1024 tokens", "pytorch"),
        new OfficialModel("T5-Small", "T5 文本到文本小型版", "nlp", "自然语言处理", 60.0, "512 tokens", "pytorch"),
        new OfficialModel("LLaMA-7B", "LLaMA 7B 大语言模型", "nlp", "自然语言处理", 7000.0, "2048 tokens", "pytorch")
    };

    private final ModelRegistryRepository modelRepository;
    private final ModelArticleRepository articleRepository;

    public String seedOfficialModels() {
        int created = 0;
        int synced = 0;
        for (OfficialModel model : OFFICIAL_MODELS) {
            Optional<ModelRegistry> existing = modelRepository.findByName(model.name());
            ModelRegistry registry = existing.orElseGet(ModelRegistry::new);
            if (existing.isEmpty()) {
                created++;
                registry.setName(model.name());
            } else {
                synced++;
            }

            registry.setTaskType(model.taskType());
            registry.setDisplayNameZh(model.displayNameZh());
            registry.setTaskTypeZh(model.taskTypeZh());
            registry.setParamCountM(model.paramCountM());
            registry.setInputSize(model.inputSize());
            registry.setFramework(model.framework());
            registry.setIsOfficial(true);
            registry.setPaperUrl(getPaperUrl(registry));
            registry.setDescription(buildEnglishDescription(model));
            registry.setDescriptionZh(getDescription(registry));
            modelRepository.save(registry);
        }
        return "已同步 " + OFFICIAL_MODELS.length + " 个官方模型（新增 " + created + "，更新 " + synced + "）";
    }

    private String buildEnglishDescription(OfficialModel model) {
        return model.name() + " - " + model.taskType() + " model, " + model.paramCountM() + "M params";
    }

    public int seedModelArticles(boolean force) {
        var models = modelRepository.findByIsOfficialTrueOrderByNameAsc();
        if (models.isEmpty()) {
            return -1;
        }

        int count = 0;
        for (ModelRegistry model : models) {
            Optional<ModelArticle> existing = articleRepository.findByModelId(model.getId());
            if (force) {
                existing.ifPresent(articleRepository::delete);
            }
            if (force || existing.isEmpty()) {
                articleRepository.save(ModelArticle.builder()
                    .modelId(model.getId())
                    .title(model.getName())
                    .content(buildArticleContent(model))
                    .paperUrl(getPaperUrl(model))
                    .build());
                count++;
            }
        }
        return count;
    }

    private String buildArticleContent(ModelRegistry model) {
        String paperUrl = getPaperUrl(model);
        return """
## %s

### 基本信息

| 属性 | 值 |
|------|-----|
| 任务类型 | %s |
| 参数量 | %.1fM |
| 输入尺寸 | %s |
| 框架 | %s |

### 模型简介

%s

### 适用场景

%s

### 训练建议

- **学习率**: 建议从 0.001 开始，使用余弦退火调度
- **批次大小**: 根据显存调整，通常 32-256
- **优化器**: Adam/AdamW 适合大多数场景，SGD 配合 Momentum 在部分任务上可能更优
- **正则化**: 建议使用 Weight Decay 1e-4，配合 Dropout/Label Smoothing

### 论文原文

%s

---
*本文由 DeepInsight 平台自动生成，数据来源于原论文及公开基准测试。*
""".formatted(
            model.getName(),
            model.getTaskType(),
            model.getParamCountM(),
            model.getInputSize() != null ? model.getInputSize() : "N/A",
            model.getFramework(),
            getDescription(model),
            getUseCases(model.getTaskType()),
            paperUrl != null ? "📄 [查看论文](" + paperUrl + ")" : "暂无公开论文链接"
        );
    }

    private String getDescription(ModelRegistry model) {
        String name = model.getName();
        if (name.contains("ResNet-101")) return "ResNet-101 是 ResNet 系列的深层版本，包含 101 个卷积层。与 ResNet-50 相比，它使用了更多的 Bottleneck 残差块（3层卷积的残差单元），在 ImageNet 上 Top-1 准确率达到 77.4%，比 ResNet-50 高出约 1.3 个百分点。更深的网络带来了更强的特征表达能力，适合需要高精度的图像分类和特征提取任务，但相应的计算开销也更大（44.5M 参数）。";
        if (name.contains("ResNet-50")) return "ResNet-50 是残差网络（Residual Network）家族的经典代表，由微软亚洲研究院的何恺明等人在 2015 年提出。它通过引入**跳跃连接（Skip Connection）**解决了深层网络的退化问题——当网络变深时，训练误差不降反升。每个残差块学习的是输入与输出的残差映射 F(x) + x，使得梯度可以通过跳跃连接无损地流过深层网络。ResNet-50 包含 50 个卷积层，使用 Bottleneck 设计（1×1→3×3→1×1 卷积）来减少计算量，是图像分类、目标检测和语义分割中最常用的骨干网络之一。";
        if (name.contains("VGG")) return "VGG-19 由牛津大学 Visual Geometry Group 在 2014 年提出，是 VGG 系列中最深的版本。其核心设计哲学是**简洁与规整**——全部使用 3×3 小卷积核堆叠，通过增加深度来提升性能。VGG-19 包含 16 个卷积层和 3 个全连接层，参数量高达 143.7M。虽然参数量大、推理速度较慢，但其提取的特征在迁移学习、风格迁移和图像生成任务中表现极其优异，至今仍被广泛用作感知损失的特征提取器。";
        if (name.contains("EfficientNet")) return "EfficientNet 由 Google Brain 在 2019 年提出，通过**神经架构搜索（NAS）**系统地研究了网络深度、宽度和输入分辨率之间的关系。作者发现这三个维度可以通过复合系数统一缩放，提出了 EfficientNet-B0 到 B7 系列。EfficientNet-B4 在 ImageNet 上达到 82.9% Top-1 准确率，仅使用 19.3M 参数——比 ResNet-50 更小更快却更准。它是移动端部署和边缘计算的首选方案之一。";
        if (name.contains("ViT")) return "Vision Transformer (ViT) 由 Google Research 在 2020 年提出，是将 Transformer 架构直接应用于图像分类的开创性工作。它将图像切分为 16×16 的固定大小 Patch，将这些 Patch 的线性嵌入序列作为 Transformer 编码器的输入。ViT-B/16 在 JFT-300M 大规模数据集上预训练后，在 ImageNet 上达到了 84.2% 的准确率。它完全抛弃了卷积归纳偏置，证明了**纯注意力机制在视觉任务上的有效性**，开启了 Vision Transformer 的研究浪潮。";
        if (name.contains("Swin")) return "Swin Transformer 由微软亚洲研究院在 2021 年提出，获得了 ICCV 2021 最佳论文奖（马尔奖）。它引入了**移动窗口注意力机制（Shifted Window Attention）**，在保持 Transformer 全局建模能力的同时，通过分层结构实现了类似 CNN 的多尺度特征提取。Swin-T 是轻量版本，在 ImageNet 上达到 81.3% 准确率，仅 28.3M 参数。Swin Transformer 在 COCO 检测和 ADE20K 分割任务上全面超越此前最优的 CNN 模型，证明了 Transformer 可以作为通用视觉骨干网络。";
        if (name.contains("ConvNeXt")) return "ConvNeXt 由 Facebook AI Research (FAIR) 在 2022 年提出，旨在探索：如果完全使用现代训练技术和架构设计，纯卷积网络是否还能与 Transformer 竞争？作者将 ResNet-50 逐步「现代化」——引入 Transformer 的训练策略（更长训练周期、AdamW 优化器、数据增强）和结构设计（LayerNorm 替代 BatchNorm、GELU 激活、大卷积核 7×7），最终得到的 ConvNeXt-T 在 ImageNet 上达到 82.1% 准确率，证明了**纯卷积网络在正确设计下仍然具有强大竞争力**。";
        if (name.contains("MobileNet")) return "MobileNetV3 由 Google 在 2019 年提出，是专为移动端和边缘设备设计的高效网络架构。它结合了**深度可分离卷积（Depthwise Separable Convolution）**、线性瓶颈逆残差结构和基于 Platform-Aware NAS 的网络搜索。MobileNetV3-Large 在 ImageNet 上达到 75.2% 准确率，仅 5.4M 参数，推理速度极快。它使用 h-swish 激活函数替代计算昂贵的 swish，进一步优化移动端推理效率，是资源受限场景（手机、IoT、嵌入式设备）的首选。";
        if (name.contains("DenseNet")) return "DenseNet（密集连接卷积网络）由康奈尔大学和清华大学在 2017 年提出，获得了 CVPR 2017 最佳论文奖。它通过**密集连接**将每一层与前面所有层直接相连，最大化特征复用。第 l 层接收前面所有层的特征图作为输入：x_l = H([x_0, x_1, ..., x_{l-1}])。这种设计减轻了梯度消失问题、增强了特征传播、大幅减少了参数数量。DenseNet-201 在 ImageNet 上准确率与 ResNet-101 相当，但参数量不到其一半（20M），计算效率更高。";
        if (name.contains("YOLOv8s")) return "YOLOv8 由 Ultralytics 在 2023 年发布，是 YOLO 系列的最新版本之一。YOLO（You Only Look Once）将目标检测转化为回归问题——单次前向传播即可同时预测边界框坐标和类别概率。YOLOv8s 是中等规模的版本（11.2M 参数），在 COCO 数据集上达到 44.9% mAP。相比前代版本，YOLOv8 引入了 Anchor-Free 检测头、新的损失函数（CIoU + DFL）和解耦头结构，在速度与精度之间达到了新的平衡点。";
        if (name.contains("YOLOv8n")) return "YOLOv8n 是 YOLOv8 系列的 Nano 版本，仅 3.2M 参数，专为极致效率设计。尽管体积极小，它在 COCO 上仍能达到 37.3% mAP，推理速度超过 1000 FPS（在 GPU 上）。YOLOv8 使用 Anchor-Free 检测头，无需预设锚框，简化了训练和推理流程。它支持检测、分类、分割和姿态估计等多种任务，是目前工业界应用最广泛的目标检测框架之一。";
        if (name.contains("DeepLabV3")) return "DeepLabV3 由 Google 在 2017 年提出，是语义分割领域的里程碑工作。它使用**空洞卷积（Atrous/Dilated Convolution）**在不增加参数量的情况下扩大感受野，并通过**空洞空间金字塔池化（ASPP）**模块并行捕获多尺度上下文信息。配合 ResNet-50 骨干网络，DeepLabV3-RN50 在 PASCAL VOC 2012 上达到 74.6% mIoU，在 Cityscapes 上也表现优异。它是自动驾驶场景理解和医学图像分割的基础架构之一。";
        if (name.contains("DeepFM")) return "DeepFM 由华为诺亚方舟实验室在 2017 年提出，是推荐系统和 CTR（点击率）预估领域的经典模型。它巧妙地将**因子分解机（FM）**和**深度神经网络（DNN）**结合在一个端到端的框架中：FM 组件负责捕捉低阶特征交互（一阶和二阶），DNN 组件负责学习高阶非线性特征组合。两者共享相同的输入（Embedding 层），无需额外的特征工程或预训练。DeepFM 在 Criteo 和 Company* 数据集上超越了当时的主流方案（Wide&Deep、FNN 等）。";
        if (name.contains("Wide&Deep")) return "Wide & Deep Learning 由 Google 在 2016 年提出，是 Google Play 商店推荐系统的核心技术。模型由两部分组成：**Wide 部分**是广义线性模型，通过交叉积变换（Cross-Product Transformation）记忆历史模式，确保模型不会「忘记」高频共现特征；**Deep 部分**是前馈神经网络，通过 Embedding 层将稀疏特征转化为稠密向量，泛化到未见过的特征组合。两者联合训练，同时具备了「记忆」（Memorization）和「泛化」（Generalization）能力，使 Google Play 的 App 安装率提升了 3.9%。";
        if (name.contains("NCF")) return "Neural Collaborative Filtering (NCF) 由新加坡国立大学和哥伦比亚大学在 2017 年提出。传统矩阵分解使用内积来建模用户-物品交互，但内积的线性性质限制了其表达能力。NCF 用**神经网络替代内积操作**：通过多层感知机（MLP）学习任意复杂的用户-物品交互函数。它还可以与广义矩阵分解（GMF）结合，形成 NeuMF 模型，灵活地捕捉线性和非线性特征交互。在 MovieLens 和 Pinterest 数据集上，NCF 显著优于传统协同过滤方法。";
        if (name.contains("DIN")) return "Deep Interest Network (DIN) 由阿里巴巴在 2018 年提出，针对电商推荐场景中用户兴趣的**多样性**和**局部激活**特性进行了创新设计。核心组件是自适应激活单元——根据候选广告，通过注意力机制动态计算用户历史行为中每个物品的权重。与传统模型将用户兴趣压缩为固定向量不同，DIN 允许用户兴趣随候选商品自适应变化。在阿里巴巴的在线 A/B 测试中，DIN 使 CTR 提升了 10.7%，eCPM 提升了 9.1%，是工业级推荐系统的重要基线。";
        if (name.contains("BERT")) return "BERT（Bidirectional Encoder Representations from Transformers）由 Google AI Language 在 2018 年提出，是 NLP 领域最具影响力的模型之一。它使用 Transformer 编码器的双向结构，通过两个创新性的预训练任务——**掩码语言模型（MLM，Masked Language Model）**和**下一句预测（NSP，Next Sentence Prediction）**——在无标注文本上进行预训练。BERT-Base 拥有 110M 参数，在发布时横扫了 11 项 NLP 基准测试（GLUE、SQuAD 1.1/2.0、SWAG 等）的记录。它开创了「预训练 + 微调」的 NLP 范式，至今仍是文本理解任务的首选基线模型。";
        if (name.contains("GPT")) return "GPT-2（Generative Pre-trained Transformer 2）由 OpenAI 在 2019 年发布，拥有 15 亿参数，是当时最大的公开语言模型。它是**自回归语言模型**，通过预测下一个 Token 的方式进行训练，擅长文本生成、补全和少样本学习（Few-Shot Learning）。GPT-2 的核心发现是：规模足够大的语言模型可以在无监督预训练后，仅通过提供少量示例（Zero-Shot/Few-Shot）就完成多种下游任务，无需任何微调。它证明了「规模即能力」的 Scaling Law，为大语言模型的发展奠定了基础。";
        if (name.contains("T5")) return "T5（Text-to-Text Transfer Transformer）由 Google Research 在 2019 年提出。它的核心思想是**将所有的 NLP 任务统一为 Text-to-Text 格式**——无论是翻译、问答、分类还是摘要，输入和输出都表示为文本序列。T5 在 C4（Colossal Clean Crawled Corpus）数据集上预训练，使用 Span Corruption 作为预训练目标。T5-Small 是轻量版本（60M 参数），适合研究和快速实验。T5 的贡献不仅在于模型本身，更在于它提出的统一框架和系统的实验方法论。";
        if (name.contains("LLaMA")) return "LLaMA（Large Language Model Meta AI）由 Meta AI 在 2023 年发布，是一系列开源大语言模型，包含 7B、13B、33B 和 65B 参数版本。LLaMA-7B 仅使用公开可用的数据（CommonCrawl、C4、GitHub、Wikipedia 等）进行训练，在多项基准上超越了参数规模更大的 GPT-3（175B）。其训练采用了高效的优化技术（AdamW、余弦学习率调度、梯度裁剪）。LLaMA 的开源极大地推动了学术界和开源社区的大模型研究，催生了 Alpaca、Vicuna、Koala 等众多衍生模型。";
        return model.getName() + " 是一个 " + model.getTaskType() + " 领域的深度学习模型，拥有 " + model.getParamCountM() + "M 参数。";
    }

    private String getPaperUrl(ModelRegistry model) {
        String name = model.getName();
        if (name.contains("ResNet-50") || name.contains("ResNet-101")) return "https://arxiv.org/abs/1512.03385";
        if (name.contains("VGG")) return "https://arxiv.org/abs/1409.1556";
        if (name.contains("EfficientNet")) return "https://arxiv.org/abs/1905.11946";
        if (name.contains("ViT")) return "https://arxiv.org/abs/2010.11929";
        if (name.contains("Swin")) return "https://arxiv.org/abs/2103.14030";
        if (name.contains("ConvNeXt")) return "https://arxiv.org/abs/2201.03545";
        if (name.contains("MobileNet")) return "https://arxiv.org/abs/1905.02244";
        if (name.contains("DenseNet")) return "https://arxiv.org/abs/1608.06993";
        if (name.contains("YOLOv8")) return "https://github.com/ultralytics/ultralytics";
        if (name.contains("DeepLabV3")) return "https://arxiv.org/abs/1706.05587";
        if (name.contains("DeepFM")) return "https://arxiv.org/abs/1703.04247";
        if (name.contains("Wide&Deep")) return "https://arxiv.org/abs/1606.07792";
        if (name.contains("NCF")) return "https://arxiv.org/abs/1708.05031";
        if (name.contains("DIN")) return "https://arxiv.org/abs/1706.06978";
        if (name.contains("BERT")) return "https://arxiv.org/abs/1810.04805";
        if (name.contains("GPT")) return "https://d4mucfpksywv.cloudfront.net/better-language-models/language_models_are_unsupervised_multitask_learners.pdf";
        if (name.contains("T5")) return "https://arxiv.org/abs/1910.10683";
        if (name.contains("LLaMA")) return "https://arxiv.org/abs/2302.13971";
        return null;
    }

    private String getUseCases(String taskType) {
        return switch (taskType) {
            case "classification" -> "- 图像分类\n- 迁移学习特征提取\n- 细粒度识别\n- 医学影像分析";
            case "detection" -> "- 实时目标检测\n- 安防监控\n- 自动驾驶感知\n- 工业缺陷检测";
            case "segmentation" -> "- 语义分割\n- 医学图像分割\n- 自动驾驶场景理解\n- 遥感图像分析";
            case "recommendation" -> "- CTR 预估\n- 个性化推荐\n- 广告排序\n- 用户行为预测";
            case "nlp" -> "- 文本分类\n- 情感分析\n- 命名实体识别\n- 文本生成与摘要";
            default -> "- 通用深度学习任务";
        };
    }

    private record OfficialModel(
        String name,
        String displayNameZh,
        String taskType,
        String taskTypeZh,
        double paramCountM,
        String inputSize,
        String framework
    ) {
    }
}
