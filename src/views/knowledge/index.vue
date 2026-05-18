<template>
  <div class="knowledge-page">
    <section class="kg-hero">
      <KnowledgeGraph3D class="kg-3d-canvas" @open-article="openKnowledgeArticle" />
      <div class="kg-hero-vignette" aria-hidden="true"></div>
      <div class="kg-hero-title entrance-hero">
        <div class="hero-badge">{{ pageText.heroBadge }}</div>
        <h1>{{ pageText.heroTitle }}<em>{{ pageText.heroTitleEm }}</em></h1>
        <p>{{ pageText.heroDesc }}</p>
        <div class="hero-actions">
          <button type="button" @click="scrollToTutorials">{{ pageText.startLearning }}</button>
          <button type="button" class="ghost" @click="showArticleIndex = true">{{ pageText.officialArticles }} {{ officialArticles.length }}</button>
        </div>
      </div>
      <div class="kg-system-panel">
        <span>{{ pageText.atlasStatus }}</span>
        <strong>{{ officialArticles.length }}</strong>
        <em>{{ pageText.articlesIndexed }}</em>
      </div>
      <div class="kg-legend-simple">
        <div class="leg-row"><span class="leg-dot leg-core"></span> {{ pageText.legendCore }}</div>
        <div class="leg-row"><span class="leg-dot leg-module"></span> {{ pageText.legendModule }}</div>
        <div class="leg-row"><span class="leg-dot leg-arch"></span> {{ pageText.legendArch }}</div>
        <div class="leg-row"><span class="leg-dot leg-skill"></span> {{ pageText.legendSkill }}</div>
        <div class="leg-row"><span class="leg-dot leg-metric"></span> {{ pageText.legendMetric }}</div>
        <div class="leg-row"><span class="leg-dot leg-deploy"></span> {{ pageText.legendDeploy }}</div>
        <div class="leg-row"><span class="leg-dot leg-read"></span> {{ pageText.legendRead }}</div>
      </div>
    </section>

    <!-- ========== 快速入门 ========== -->
    <section class="kb-section kb-section-primary">
      <div class="section-intro" id="tutorials">
        <span class="section-kicker">{{ pageText.foundationKicker }}</span>
        <h2>{{ pageText.tutorialTitle }}</h2>
        <p>{{ pageText.tutorialDesc }}</p>
      </div>

      <div class="tutorial-grid">
        <div v-for="(column, columnIndex) in tutorialColumns" :key="columnIndex" class="tutorial-column">
          <div v-for="tut in column" :key="tut.title" class="tutorial-card" :class="{ expanded: tut.expanded }">
            <button class="tut-header" type="button" :aria-expanded="tut.expanded" @click="toggleTutorial(tut)">
              <div class="tut-icon" :style="{ background: tut.color + '15', color: tut.color }">{{ tut.icon }}</div>
              <div class="tut-info">
                <h3 :style="{ color: tut.color }">{{ tut.title }}</h3>
                <span class="tut-duration">{{ tut.duration }}</span>
              </div>
              <el-icon class="tut-arrow" :class="{ rotated: tut.expanded }"><ArrowDown /></el-icon>
            </button>
            <Transition
              @before-enter="onTutorialBeforeEnter"
              @enter="onTutorialEnter"
              @after-enter="onTutorialAfterEnter"
              @before-leave="onTutorialBeforeLeave"
              @leave="onTutorialLeave"
              @after-leave="onTutorialAfterLeave"
            >
              <div v-if="tut.expanded" class="tut-content">
                <p class="tut-desc">{{ tut.desc }}</p>
                <div class="tut-detail" v-html="renderMarkdown(tut.detail)"></div>
              </div>
            </Transition>
          </div>
        </div>
      </div>
      <div class="mt-6 text-center" v-if="officialArticles.length > 0">
        <el-button class="article-index-button" :style="{ background: 'var(--primary-color)', border: 'none', color: '#fff' }" @click="showArticleIndex = true">
          {{ pageText.viewOfficialPrefix }} {{ officialArticles.length }} {{ pageText.viewOfficialSuffix }}
        </el-button>
      </div>
    </section>

    <Transition name="article-index">
      <aside v-if="showArticleIndex" class="article-index-panel">
        <div class="article-index-head">
          <div>
            <span>{{ pageText.articleIndexTitle }}</span>
            <small>{{ pageText.articleIndexHint }}</small>
          </div>
          <button type="button" @click="showArticleIndex = false">×</button>
        </div>
        <div class="articles-dialog-list">
          <div v-for="a in officialArticles" :key="a.id" class="article-dialog-item" @click="openKnowledgeArticle(a.id); showArticleIndex = false">
            <span class="article-dot" :style="{ background: 'var(--primary-color)' }"></span>
            <div>
              <h4>{{ a.title }}</h4>
              <span class="article-meta-text">{{ pageText.related }}: {{ a.nodeLabel || pageText.general }} · {{ pageText.views }} {{ a.viewCount || 0 }}</span>
            </div>
          </div>
          <div v-if="!officialArticles.length" class="article-index-empty">{{ pageText.noOfficialArticles }}</div>
        </div>
      </aside>
    </Transition>

    <!-- ========== 训练曲线解读 ========== -->
    <section class="kb-section">
      <div class="section-intro">
        <span class="section-kicker">{{ pageText.curveKicker }}</span>
        <h2>{{ pageText.curveTitle }}</h2>
        <p>{{ pageText.curveDesc }}</p>
      </div>

      <div class="curves-showcase">
        <div v-for="curve in curveGuides" :key="curve.title" class="curve-card">
          <div class="curve-card-header">
            <span class="curve-badge" :style="{ background: curve.color + '20', color: curve.color }">{{ curve.badge }}</span>
            <h3 class="curve-title">{{ curve.title }}</h3>
          </div>
          <div :ref="el => setCurveRef(curve.id, el)" class="curve-chart"></div>
          <div class="curve-annotations">
            <div v-for="ann in curve.annotations" :key="ann.label" class="annotation">
              <span class="ann-dot" :style="{ background: ann.color }"></span>
              <span class="ann-text">{{ ann.label }}: {{ ann.desc }}</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ========== 模型架构图谱 ========== -->
    <section class="kb-section">
      <div class="section-intro">
        <span class="section-kicker">{{ pageText.modelKicker }}</span>
        <h2>{{ pageText.modelTitle }}</h2>
        <p>{{ pageText.modelDesc }}</p>
      </div>

      <div class="models-grid">
        <div v-for="model in modelShowcase" :key="model.name" class="model-showcase-card">
          <div class="model-card-bg" :style="{ background: `linear-gradient(135deg, ${model.color}15, ${model.color}05)` }"></div>
          <div class="model-card-content">
            <div class="model-card-top">
              <span class="model-year">{{ model.year }}</span>
              <span class="model-type" :style="{ color: model.color }">{{ model.type }}</span>
            </div>
            <h3 class="model-card-name">{{ model.name }}</h3>
            <p class="model-card-desc">{{ model.desc }}</p>
            <div class="model-card-stats">
              <div class="model-stat">
                <span class="stat-value" :style="{ color: model.color }">{{ model.params }}</span>
                <span class="stat-label">参数量</span>
              </div>
              <div class="model-stat">
                <span class="stat-value" :style="{ color: model.color }">{{ model.top1Acc }}</span>
                <span class="stat-label">Top-1 Acc</span>
              </div>
              <div class="model-stat">
                <span class="stat-value" :style="{ color: model.color }">{{ model.task }}</span>
                <span class="stat-label">任务</span>
              </div>
            </div>
            <div class="model-card-tags">
              <span v-for="tag in model.tags" :key="tag" class="model-tag">{{ tag }}</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ========== CTA ========== -->
    <section class="kb-cta">
      <div class="cta-glow"></div>
      <h2>{{ pageText.ctaTitle }}</h2>
      <p>{{ pageText.ctaDesc }}</p>
      <div class="flex gap-4">
        <el-button class="!rounded-2xl !font-black !px-8 !h-12 !text-sm" :style="{ background: 'var(--primary-color)', border: 'none', color: '#fff' }" @click="$router.push('/login')">
          {{ pageText.ctaStart }}
        </el-button>
        <el-button class="!rounded-2xl !font-black !px-8 !h-12 !text-sm !border-slate-300 dark:!border-white/15 !text-slate-600 dark:!text-slate-200" @click="scrollToTop">
          {{ pageText.ctaBack }}
        </el-button>
      </div>
    </section>

    <ArticleReaderDrawer
      v-model="readerOpen"
      :article="activeArticle"
      :loading="articleLoading"
      :source-label="pageText.readerSource"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted, watch, computed, onBeforeUnmount } from 'vue';
import { useThemeStore } from '@/stores/theme.store';
import { ArrowDown } from '@element-plus/icons-vue';
import * as echarts from 'echarts';
import KnowledgeGraph3D from './KnowledgeGraph3D.vue';
import { renderMarkdown } from '@/utils/markdown';
import ArticleReaderDrawer from '@/components/common/ArticleReaderDrawer.vue';
import { forumApi } from '@/api';
import { useI18n } from 'vue-i18n';

const themeStore = useThemeStore();
const { locale } = useI18n();
const isZh = computed(() => locale.value.startsWith('zh'));
const curveRefs = ref<Record<string, HTMLDivElement>>({});
const setCurveRef = (id: string, el: any) => { if (el) curveRefs.value[id] = el; };
const getTutorialContent = (el: Element) => el as HTMLElement;
const tutorialExpandedGap = '16px';
const tutorialExpandedBorder = 'rgba(var(--primary-rgb), 0.12)';
const onTutorialBeforeEnter = (el: Element) => {
  const node = getTutorialContent(el);
  node.style.height = '0px';
  node.style.opacity = '0';
  node.style.marginTop = '0px';
  node.style.paddingTop = '0px';
  node.style.borderTopColor = 'rgba(var(--primary-rgb), 0)';
  node.style.overflow = 'hidden';
};
const onTutorialEnter = (el: Element) => {
  const node = getTutorialContent(el);
  void node.offsetHeight;
  requestAnimationFrame(() => {
    node.style.height = `${node.scrollHeight}px`;
    node.style.opacity = '1';
    node.style.marginTop = tutorialExpandedGap;
    node.style.paddingTop = tutorialExpandedGap;
    node.style.borderTopColor = tutorialExpandedBorder;
  });
};
const onTutorialAfterEnter = (el: Element) => {
  const node = getTutorialContent(el);
  node.style.height = 'auto';
  node.style.overflow = 'visible';
};
const onTutorialBeforeLeave = (el: Element) => {
  const node = getTutorialContent(el);
  node.style.height = `${node.scrollHeight}px`;
  node.style.opacity = '1';
  node.style.marginTop = tutorialExpandedGap;
  node.style.paddingTop = tutorialExpandedGap;
  node.style.borderTopColor = tutorialExpandedBorder;
  node.style.overflow = 'hidden';
};
const onTutorialLeave = (el: Element) => {
  const node = getTutorialContent(el);
  void node.offsetHeight;
  requestAnimationFrame(() => {
    node.style.height = '0px';
    node.style.opacity = '0';
    node.style.marginTop = '0px';
    node.style.paddingTop = '0px';
    node.style.borderTopColor = 'rgba(var(--primary-rgb), 0)';
  });
};
const onTutorialAfterLeave = (el: Element) => {
  const node = getTutorialContent(el);
  node.removeAttribute('style');
};

const pageText = computed(() => isZh.value ? {
  heroBadge: 'Exploration Atlas',
  heroTitle: '深度学习',
  heroTitleEm: '知识宇宙',
  heroDesc: '在同一张研究图谱里展开概念、查看关联文章、追踪模型架构与训练指标。文章会从右侧挤出，不打断当前探索路径。',
  startLearning: '开始学习',
  officialArticles: '官方文章',
  atlasStatus: 'Atlas Status',
  articlesIndexed: 'articles indexed',
  legendCore: '平台核心',
  legendModule: '功能模块',
  legendArch: '模型架构',
  legendSkill: '训练技巧',
  legendMetric: '评估指标',
  legendDeploy: '部署推理',
  legendRead: '右侧阅读',
  foundationKicker: 'Foundation',
  tutorialTitle: '零基础入门',
  tutorialDesc: '从未接触过深度学习？从这里开始，逐步了解一切。',
  viewOfficialPrefix: '查看',
  viewOfficialSuffix: '篇官方深度文章',
  articleIndexTitle: '官方文章索引',
  articleIndexHint: '点击后从右侧打开，不离开探索中心',
  related: '关联',
  general: '通用',
  views: '浏览',
  noOfficialArticles: '暂无官方文章',
  curveKicker: 'Training Signals',
  curveTitle: '如何看懂训练曲线',
  curveDesc: '理解损失值、准确率等指标的含义，判断模型是否在正常学习',
  modelKicker: 'Model Atlas',
  modelTitle: '模型架构全景',
  modelDesc: '从经典 CNN 到现代 Transformer，主流深度学习架构一览',
  ctaTitle: '准备好开始了吗？',
  ctaDesc: '登录平台，立即体验数据管理、模型训练与可视化分析全流程',
  ctaStart: '立即开始',
  ctaBack: '回顶部看图谱',
  readerSource: '知识文章',
} : {
  heroBadge: 'Exploration Atlas',
  heroTitle: 'Deep Learning',
  heroTitleEm: 'Knowledge Universe',
  heroDesc: 'Expand concepts, read linked articles, and trace model architecture in one research graph. Articles slide in from the right without breaking exploration.',
  startLearning: 'Start Learning',
  officialArticles: 'Official Articles',
  atlasStatus: 'Atlas Status',
  articlesIndexed: 'articles indexed',
  legendCore: 'Platform Core',
  legendModule: 'Modules',
  legendArch: 'Architecture',
  legendSkill: 'Training Skills',
  legendMetric: 'Metrics',
  legendDeploy: 'Deployment',
  legendRead: 'Read on the right',
  foundationKicker: 'Foundation',
  tutorialTitle: 'Beginner Guide',
  tutorialDesc: 'New to deep learning? Start here and build the mental model step by step.',
  viewOfficialPrefix: 'View',
  viewOfficialSuffix: 'official deep-dive articles',
  articleIndexTitle: 'Official Article Index',
  articleIndexHint: 'Open articles from the right without leaving Explore Center',
  related: 'Related',
  general: 'General',
  views: 'Views',
  noOfficialArticles: 'No official articles yet',
  curveKicker: 'Training Signals',
  curveTitle: 'How to Read Training Curves',
  curveDesc: 'Understand loss, accuracy, and related metrics to judge model learning behavior',
  modelKicker: 'Model Atlas',
  modelTitle: 'Model Architecture Overview',
  modelDesc: 'From classic CNNs to modern Transformers, see the main architectures at a glance',
  ctaTitle: 'Ready to Get Started?',
  ctaDesc: 'Log in to experience data management, model training, and visual analysis workflows',
  ctaStart: 'Start Now',
  ctaBack: 'Back to Graph',
  readerSource: 'Knowledge Article',
});

/* ========== 入门教程 (16篇, 从零基础到进阶) ========== */
const tutorials = ref([
  { icon:'🎯', title:'深度学习是什么？', duration:'2分钟', color:'#4dc9f0', expanded:false,
    desc:'深度学习是机器学习的一个分支，使用多层人工神经网络从大量数据中自动学习和提取特征。',
    detail:`#### 简单类比

想象教孩子认猫：不需要告诉他"猫有尖耳朵、胡子"——给他看成千上万张猫图片，他自己会总结出猫的特征。深度学习也是如此——让计算机从海量数据中自己"悟出"规律。

#### 核心三要素

- **数据**：越多越好，质量越高越好
- **模型**：神经网络的架构（如CNN、Transformer）
- **算力**：GPU等硬件加速训练

#### 训练五步曲

1. 输入数据 → 2. 模型预测 → 3. 计算损失（预测与真实差距）→ 4. 反向传播更新参数 → 5. 重复直到收敛`,
  },
  { icon:'🧠', title:'神经网络是怎么工作的？', duration:'5分钟', color:'#6366f1', expanded:false,
    desc:'从感知机到深层网络——理解神经元、激活函数和反向传播的基本原理。',
    detail:`#### 单个神经元

神经元接收多个输入x₁,x₂,...,xₙ，每个输入乘以权重wᵢ，求和后加上偏置b，通过激活函数f输出：**y = f(Σ wᵢxᵢ + b)**。激活函数引入非线性，让网络能拟合复杂函数。

#### 常见激活函数

- **ReLU**: max(0,x)，最常用，计算快且缓解梯度消失
- **Sigmoid**: 1/(1+e⁻ˣ)，输出(0,1)，适合二分类输出层
- **Softmax**: 多分类输出层，输出概率分布
- **GELU**: Transformer标配，比ReLU平滑

#### 反向传播

链式法则从输出层向输入层逐层计算梯度，更新每个参数。关键公式：**∂L/∂w = ∂L/∂y · ∂y/∂w**（链式法则）。梯度下降：**w_new = w_old - lr × ∇L**`,
  },
  { icon:'🖥️', title:'DeepInsight平台做什么？', duration:'3分钟', color:'#10b981', expanded:false,
    desc:'一站式深度学习可视化平台，覆盖从数据管理到模型部署的全流程。',
    detail:`#### 六大核心模块

- **数据管理**：上传、预处理、增强、版本管理
- **模型训练**：选择架构、配置超参数、实时监控
- **可视化分析**：损失曲线、混淆矩阵、权重分布、ROC
- **预测推理**：用训练好的模型对新数据预测
- **知识库**：交互式3D知识图谱，拖拽探索AI知识体系
- **AI助手**：随时获得深度学习问题的解答`,
  },
  { icon:'📊', title:'常见指标怎么看？', duration:'5分钟', color:'#f59e0b', expanded:false,
    desc:'准确率、精确率、召回率、F1、AUC——这些指标分别代表什么？什么时候用哪个？',
    detail:`#### 分类任务指标

- **准确率Accuracy**：预测正确的比例。样本不均衡时不可靠——比如99%是负样本，全猜负也能99%准确
- **精确率Precision**：预测为正例中真正为正的比例。关注"误报"时用它（如垃圾邮件检测）
- **召回率Recall**：所有正例中被正确识别的比例。关注"漏报"时用它（如疾病筛查）
- **F1分数**：P和R的调和平均，综合评价。F1 = 2×P×R/(P+R)
- **AUC-ROC**：ROC曲线下面积，0.5=随机猜测，1.0=完美分类。0.8以上算好模型

#### 回归任务指标

- **MSE**：均方误差，对大误差惩罚更重（平方）
- **MAE**：平均绝对误差，对异常值更鲁棒
- **R²**：决定系数，衡量模型解释的方差比例，越接近1越好`,
  },
  { icon:'🔧', title:'超参数怎么调？', duration:'4分钟', color:'#8b5cf6', expanded:false,
    desc:'学习率、批次大小、训练轮次——这些参数如何影响训练结果？推荐值是什么？',
    detail:`#### 关键超参数

- **学习率lr**：最重要！太大→震荡不收敛，太小→收敛太慢。推荐0.001(Adam)或0.01(SGD)
- **批次大小batch size**：小批次→训练噪声大但泛化好；大批次→训练稳定。常用32/64/128。受GPU显存限制
- **训练轮次epochs**：配合早停策略，一般50-200。看验证集曲线判断
- **优化器**：Adam最通用，AdamW更好（解耦权重衰减），SGD+Momentum在部分任务更优
- **权重衰减**：L2正则化系数，常用1e-4~1e-2，防止过拟合

#### 调参策略

1. 先过拟合小数据集确保模型能学习
2. 粗调学习率(对数尺度)
3. 确定batch size(看显存)
4. 加正则化防过拟合
5. 学习率调度微调`,
  },
  { icon:'📉', title:'梯度下降的数学原理', duration:'6分钟', color:'#ef4444', expanded:false,
    desc:'理解梯度、学习率和各种优化器的数学本质——从SGD到Adam的演进。',
    detail:`#### 梯度下降

损失函数L(θ)对参数θ的梯度∇L指向L增长最快的方向。参数更新沿负梯度方向：**θ := θ - α·∇L(θ)**，α是学习率。

#### 优化器演进

- **SGD**：纯梯度下降，收敛慢但泛化好
- **SGD+Momentum**：累积历史梯度，像球滚下山坡，加速收敛：v=βv+(1-β)∇L, θ-=αv
- **AdaGrad**：自适应学习率，但学习率单调递减到0
- **RMSprop**：用指数移动平均修正Adagrad的衰减问题
- **Adam**：Momentum+RMSprop，最通用。β₁=0.9, β₂=0.999, ε=1e-8
- **AdamW**：解耦权重衰减，更好的泛化性能`,
  },
  { icon:'🏗️', title:'CNN卷积神经网络详解', duration:'8分钟', color:'#a855f7', expanded:false,
    desc:'从卷积核到特征图，理解CNN如何"看"图像——局部感受野、权重共享和池化。',
    detail:`#### 为什么需要CNN？

全连接网络处理图像时参数爆炸（224×224×3=150528个输入）。CNN利用**局部相关性**和**平移不变性**大幅减少参数。

#### 核心组件

- **卷积层**：用可学习的卷积核在图像上滑动提取特征。3×3卷积核最常用。输出尺寸=(W-F+2P)/S+1
- **池化层**：降采样减少计算量。MaxPool 2×2最常用，取区域内最大值
- **全连接层**：在最后几层做分类/回归决策

#### 经典架构

- **LeNet-5**(1998)：最早的CNN，手写数字识别
- **AlexNet**(2012)：ImageNet冠军，引入ReLU+Dropout
- **VGG**(2014)：全部用3×3卷积，简洁但参数多
- **ResNet**(2015)：跳跃连接解决深层退化，可训练1000+层`,
  },
  { icon:'🔄', title:'Transformer与自注意力', duration:'8分钟', color:'#ec4899', expanded:false,
    desc:'从RNN到Transformer的革命——理解Self-Attention、多头注意力和位置编码。',
    detail:`#### 为什么Transformer取代RNN？

RNN是串行的（必须等前一步算完），Transformer是并行的。自注意力机制让每个词直接关注所有其他词，解决了长程依赖问题。

#### 自注意力核心公式

**Attention(Q,K,V) = softmax(QKᵀ/√dₖ)·V**

Q(Query): 我要找什么？K(Key): 我有什么？V(Value): 我提供什么信息？除以√dₖ防止点积过大导致softmax梯度消失。

#### 多头注意力

用多组QKV并行计算，每组关注不同方面（语法/语义/位置关系），最后拼接。典型h=8或16个头。

#### 位置编码

Transformer没有内置的顺序概念。用正弦/余弦位置编码或可学习的位置嵌入告诉模型token的位置。`,
  },
  { icon:'🎨', title:'数据增强完全指南', duration:'5分钟', color:'#14b8a6', expanded:false,
    desc:'从基础翻转到CutMix——数据增强是如何成为防止过拟合的第一道防线。',
    detail:`#### 为什么需要数据增强？

更多数据=更好的泛化。但标注数据昂贵。数据增强在不改变语义标签的前提下变换现有数据，变相扩大数据集。

#### 图像增强方法

- **几何变换**：随机翻转(水平/垂直)、旋转(±15°)、裁剪、缩放、平移
- **色彩变换**：亮度(±20%)、对比度、饱和度、色相微调
- **噪声注入**：高斯噪声、椒盐噪声，增强鲁棒性
- **遮挡**：RandomErasing随机遮挡部分区域，CutOut遮挡固定区域
- **混合增强**：MixUp(两张图线性混合)、CutMix(区域替换)

#### 注意事项

增强不能改变标签语义——把"6"旋转180°就变成了"9"。医学影像要谨慎使用色彩变换。测试时不增强（除非TTA）。`,
  },
  { icon:'🛡️', title:'过拟合与正则化', duration:'6分钟', color:'#f97316', expanded:false,
    desc:'训练集上得100分，测试集不及格——过拟合的诊断、原因与全套解决方案。',
    detail:`#### 什么是过拟合？

模型在训练集上表现很好但测试集上表现差——它"背诵"了训练数据而非"理解"规律。判断标准：**训练损失持续下降但验证损失开始上升**。

#### 全套解决方案

- **更多数据**：最有效的办法。数据增强变相实现
- **Dropout**：训练时随机丢弃神经元(p=0.2~0.5)，迫使网络学习冗余表示。测试时不丢弃但要缩放
- **BatchNorm/LayerNorm**：归一化层输出，有轻微正则化效果
- **权重衰减(L2)**：损失函数加λ∥w∥²，惩罚大权重
- **早停Early Stopping**：验证损失不再下降时停止训练
- **标签平滑**：将one-hot标签从[0,1,0]改为[0.05,0.9,0.05]，防止过度自信`,
  },
  { icon:'⚖️', title:'偏差与方差的权衡', duration:'5分钟', color:'#e11d48', expanded:false,
    desc:'欠拟合(高偏差) vs 过拟合(高方差)——如何找到最佳平衡点？',
    detail:`#### 偏差Bias

模型预测值与真实值的系统性偏离。高偏差=欠拟合=模型太简单。增加模型容量（更多层、更多参数）可降低偏差。

#### 方差Variance

模型对训练数据微小变化的敏感度。高方差=过拟合=模型太复杂。增加数据、正则化可降低方差。

#### 权衡法则

- 简单模型→高偏差+低方差（欠拟合）
- 复杂模型→低偏差+高方差（过拟合）
- 目标：找到最小化**总误差=Bias²+Variance+不可约误差**的点

#### 诊断方法

训练误差高+验证误差高→欠拟合(增加模型容量)；训练误差低+验证误差高→过拟合(加正则化)；两者都低→完美！继续调参可能有害。`,
  },
  { icon:'📦', title:'BatchNorm vs LayerNorm', duration:'4分钟', color:'#06b6d4', expanded:false,
    desc:'两种归一化方法的原理、区别和适用场景——为什么CNN用BN而Transformer用LN？',
    detail:`#### BatchNorm(BN)

对每个特征在**batch维度**上归一化：μ,σ²是对同一通道、不同样本计算的。依赖batch size——batch太小(≤4)时统计不稳定。**CNN标配**。

#### LayerNorm(LN)

对每个样本在**特征维度**上归一化：μ,σ²是对同一样本、所有通道计算的。不依赖batch size，适合变长序列。**Transformer标配**。

#### 为什么Transformer用LN？

- 序列长度可变，BN在变长序列上统计不稳定
- LN对每个样本独立归一化，训练和推理一致
- LN可以作用在token维度上，保持token间相对关系

#### 其他归一化

- **InstanceNorm**：每个样本每个通道独立，风格迁移常用
- **GroupNorm**：BN和LN的折中，小batch时替代BN`,
  },
  { icon:'🎓', title:'迁移学习实战', duration:'6分钟', color:'#84cc16', expanded:false,
    desc:'站在巨人的肩膀上——如何用预训练模型在小数据集上达到SOTA效果。',
    detail:`#### 核心思想

在ImageNet等大数据集上预训练的模型已经学到了通用特征（边缘、纹理、形状）。迁移到新任务时，这些知识可以复用，只需少量标注数据。

#### 两种策略

- **特征提取**：冻结预训练模型的所有卷积层，只训练最后新增的分类头。适合小数据集(≤1000张)，训练快
- **Fine-tuning**：解冻最后几层（或全部层），用很小的学习率(1e-5~1e-4)微调。数据越多解冻越多层

#### 实践建议

1. 使用与预训练相同的归一化参数
2. 初始学习率设为预训练的1/10
3. 先训练分类头几epoch再微调
4. 使用余弦学习率衰减
5. 数据增强不要太激进`,
  },
  { icon:'🏎️', title:'学习率调度策略', duration:'5分钟', color:'#d946ef', expanded:false,
    desc:'固定的学习率不够好——从阶梯衰减到Cosine退火再到Warmup，理解各种调度策略。',
    detail:`#### 为什么需要调度？

训练初期大步快走，后期小步精调。固定学习率要么初期收敛太慢，要么后期在最优点附近震荡。

#### 常用策略

- **阶梯衰减StepLR**：每N个epoch将lr乘以γ(如0.1)。简单但不平滑
- **余弦退火CosineAnnealing**：lr沿余弦曲线从初始值衰减到0。平滑，SOTA常用
- **带重启的余弦CosineWarmRestart**：周期性地重置lr，每次周期更长。帮助跳出局部最优
- **Warmup**：前几个epoch从很小的lr线性增长到目标值，防止训练初期不稳定（Transformer必备！）
- **ReduceLROnPlateau**：监控验证指标，不下降时降低lr。自适应但保守`,
  },
  { icon:'🗜️', title:'模型压缩与部署', duration:'5分钟', color:'#64748b', expanded:false,
    desc:'训练好的模型太大太慢？量化、剪枝、蒸馏——三大压缩技术让模型跑在边缘设备上。',
    detail:`#### 量化Quantization

将32位浮点权重和激活值转为8位整数(INT8)或16位浮点(FP16)。INT8推理速度可提升2-4倍，模型大小减至1/4。PTQ(训练后量化)简单，QAT(量化感知训练)精度更高。

#### 剪枝Pruning

移除不重要的权重(非结构化剪枝)或整个神经元/通道(结构化剪枝)。非结构化剪枝压缩率高但需要稀疏矩阵支持。结构化剪枝可直接加速。典型可剪掉50-90%参数。

#### 知识蒸馏Distillation

用大模型(Teacher)的软标签训练小模型(Student)。Student学习Teacher的"思考方式"而不仅是正确答案。温度参数T控制软标签平滑度。

#### 推理引擎

TensorRT(NVIDIA)、ONNX Runtime、OpenVINO(Intel)、CoreML(Apple)——导出模型到这些引擎可获得2-5倍加速。`,
  },
  { icon:'🔬', title:'GAN生成对抗网络', duration:'6分钟', color:'#c026d3', expanded:false,
    desc:'让两个网络互相博弈——理解GAN的原理、训练技巧和经典变体。',
    detail:`#### 核心思想

**生成器G**从随机噪声生成假数据，**判别器D**判断数据是真是假。G想骗过D，D想不被骗。这是**极小极大博弈**：min_G max_D E[log D(x)] + E[log(1-D(G(z)))]

#### 训练技巧

- **模式坍塌**：G只生成少数几种样本。解决方案：WGAN、Minibatch Discrimination
- **训练不稳定**：D和G需要平衡。D太强→G梯度消失；G太强→D全是假判
- **WGAN**：用Wasserstein距离替代JS散度，训练更稳定

#### 经典变体

- **DCGAN**：CNN+GAN，生成图像的基础架构
- **StyleGAN**：控制生成图像的风格层级（粗糙→细节）
- **CycleGAN**：无配对数据做风格迁移（马↔斑马）
- **Pix2Pix**：成对数据的图像翻译（草图→照片）`,
  },
]);
const tutorialColumns = computed(() => {
  const columns = [[], []] as Array<typeof tutorials.value>;
  tutorials.value.forEach((tutorial, index) => {
    columns[index % 2].push(tutorial);
  });
  return columns;
});
const toggleTutorial = (tutorial: (typeof tutorials.value)[number]) => {
  tutorial.expanded = !tutorial.expanded;
};

/* ========== 曲线解读 ========== */
const initCurveCharts = () => {
  const isDark = themeStore.isDarkMode;
  const textColor = isDark ? '#a0aec0' : '#64748b';
  const borderColor = isDark ? '#1e293b' : '#e2e8f0';
  const epochs = Array.from({ length: 60 }, (_, i) => i + 1);

  const charts: Array<{ id: string; option: any }> = [
    {
      id: 'healthy-loss',
      option: {
        tooltip: { trigger: 'axis' },
        legend: { data: ['训练损失', '验证损失'], bottom: 0, textStyle: { color: textColor, fontSize: 11 } },
        grid: { left: 50, right: 20, top: 15, bottom: 35 },
        xAxis: { type: 'category', data: epochs, axisLabel: { color: textColor, fontSize: 9 }, axisLine: { lineStyle: { color: borderColor } } },
        yAxis: { type: 'value', name: 'Loss', nameTextStyle: { color: textColor, fontSize: 10 }, axisLabel: { color: textColor, fontSize: 9 }, splitLine: { lineStyle: { color: borderColor, type: 'dashed' } } },
        series: [
          { name: '训练损失', type: 'line', data: epochs.map(i => (2.8 * Math.exp(-0.08 * i) + 0.1 * Math.random()).toFixed(3)), smooth: true, symbol: 'none', lineStyle: { color: '#f87171', width: 2 }, areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(248,113,113,0.12)' }, { offset: 1, color: 'rgba(248,113,113,0)' }]) } },
          { name: '验证损失', type: 'line', data: epochs.map(i => (2.9 * Math.exp(-0.07 * i) + 0.15 * Math.random()).toFixed(3)), smooth: true, symbol: 'none', lineStyle: { color: '#fbbf24', width: 2, type: 'dashed' } },
        ],
      },
    },
    {
      id: 'overfit-loss',
      option: {
        tooltip: { trigger: 'axis' },
        legend: { data: ['训练损失', '验证损失'], bottom: 0, textStyle: { color: textColor, fontSize: 11 } },
        grid: { left: 50, right: 20, top: 15, bottom: 35 },
        xAxis: { type: 'category', data: epochs, axisLabel: { color: textColor, fontSize: 9 }, axisLine: { lineStyle: { color: borderColor } } },
        yAxis: { type: 'value', name: 'Loss', nameTextStyle: { color: textColor, fontSize: 10 }, axisLabel: { color: textColor, fontSize: 9 }, splitLine: { lineStyle: { color: borderColor, type: 'dashed' } } },
        series: [
          { name: '训练损失', type: 'line', data: epochs.map(i => Math.max(0.05, 2.5 * Math.exp(-0.12 * i) + 0.01 * Math.random()).toFixed(3)), smooth: true, symbol: 'none', lineStyle: { color: '#f87171', width: 2 } },
          { name: '验证损失', type: 'line', data: epochs.map(i => (2.8 * Math.exp(-0.05 * i) + 0.05 * i * 0.008 + 0.08 * Math.random()).toFixed(3)), smooth: true, symbol: 'none', lineStyle: { color: '#fbbf24', width: 2, type: 'dashed' }, markLine: { silent: true, symbol: 'none', lineStyle: { color: '#ef4444', type: 'dashed', width: 1 }, label: { color: '#ef4444', fontSize: 10, formatter: '开始过拟合' }, data: [{ xAxis: 20 }] } },
        ],
      },
    },
    {
      id: 'accuracy-curve',
      option: {
        tooltip: { trigger: 'axis' },
        legend: { data: ['训练准确率', '验证准确率'], bottom: 0, textStyle: { color: textColor, fontSize: 11 } },
        grid: { left: 55, right: 20, top: 15, bottom: 35 },
        xAxis: { type: 'category', data: epochs, axisLabel: { color: textColor, fontSize: 9 }, axisLine: { lineStyle: { color: borderColor } } },
        yAxis: { type: 'value', name: '%', max: 100, nameTextStyle: { color: textColor, fontSize: 10 }, axisLabel: { color: textColor, fontSize: 9 }, splitLine: { lineStyle: { color: borderColor, type: 'dashed' } } },
        series: [
          { name: '训练准确率', type: 'line', data: epochs.map(i => Math.min(99.5, 98 * (1 - Math.exp(-0.08 * i)) + Math.random()).toFixed(1)), smooth: true, symbol: 'none', lineStyle: { color: '#4ade80', width: 2 }, areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(74,222,128,0.1)' }, { offset: 1, color: 'rgba(74,222,128,0)' }]) } },
          { name: '验证准确率', type: 'line', data: epochs.map(i => Math.min(97, 95 * (1 - Math.exp(-0.06 * i)) + Math.random() * 2).toFixed(1)), smooth: true, symbol: 'none', lineStyle: { color: '#60a5fa', width: 2, type: 'dashed' } },
        ],
      },
    },
    {
      id: 'lr-comparison',
      option: {
        tooltip: { trigger: 'axis' },
        legend: { data: ['lr=0.1 (太大)', 'lr=0.01', 'lr=0.001 (合适)', 'lr=0.0001 (太小)'], bottom: 0, textStyle: { color: textColor, fontSize: 10 } },
        grid: { left: 50, right: 20, top: 15, bottom: 45 },
        xAxis: { type: 'category', data: epochs, axisLabel: { color: textColor, fontSize: 9 }, axisLine: { lineStyle: { color: borderColor } } },
        yAxis: { type: 'value', name: 'Loss', nameTextStyle: { color: textColor, fontSize: 10 }, axisLabel: { color: textColor, fontSize: 9 }, splitLine: { lineStyle: { color: borderColor, type: 'dashed' } } },
        series: [
          { name: 'lr=0.1 (太大)', type: 'line', data: epochs.map(i => (2.5 + 1.5 * Math.sin(i * 0.3) + 0.8 * Math.random()).toFixed(3)), smooth: true, symbol: 'none', lineStyle: { color: '#ef4444', width: 1.5 } },
          { name: 'lr=0.01', type: 'line', data: epochs.map(i => (2.2 * Math.exp(-0.12 * i) + 0.15 + 0.1 * Math.random()).toFixed(3)), smooth: true, symbol: 'none', lineStyle: { color: '#f59e0b', width: 1.5 } },
          { name: 'lr=0.001 (合适)', type: 'line', data: epochs.map(i => (2.6 * Math.exp(-0.08 * i) + 0.08 * Math.random()).toFixed(3)), smooth: true, symbol: 'none', lineStyle: { color: '#4ade80', width: 2.5 } },
          { name: 'lr=0.0001 (太小)', type: 'line', data: epochs.map(i => (2.8 * Math.exp(-0.015 * i) + 0.05 * Math.random()).toFixed(3)), smooth: true, symbol: 'none', lineStyle: { color: '#94a3b8', width: 1.5, type: 'dashed' } },
        ],
      },
    },
  ];

  charts.forEach(chart => {
    const el = curveRefs.value[chart.id];
    if (el) {
      const instance = echarts.init(el);
      instance.setOption(chart.option);
    }
  });
};

/* ========== 模型展示 ========== */
const modelShowcase = ref([
  { name: 'ResNet-50', year: '2015', type: 'CNN', color: '#6366f1', desc: '引入残差连接，解决深层网络退化问题。50层残差网络，ImageNet冠军', params: '25.6M', top1Acc: '76.1%', task: '分类', tags: ['残差连接', 'BatchNorm', 'Skip Connection'] },
  { name: 'VGG-19', year: '2014', type: 'CNN', color: '#8b5cf6', desc: '使用连续小卷积核替代大卷积核，结构简洁但参数量大', params: '143.7M', top1Acc: '72.4%', task: '分类', tags: ['小卷积核', '深层网络', '规则结构'] },
  { name: 'EfficientNet-B4', year: '2019', type: 'CNN', color: '#10b981', desc: '通过NAS搜索最优深度/宽度/分辨率组合，效率与精度兼得', params: '19.3M', top1Acc: '82.9%', task: '分类', tags: ['NAS', '复合缩放', '高效'] },
  { name: 'ViT-B/16', year: '2020', type: 'Transformer', color: '#ec4899', desc: '将图像切分为patch序列，直接用Transformer处理，颠覆CNN范式', params: '86.6M', top1Acc: '81.8%', task: '分类', tags: ['自注意力', 'Patch', '预训练'] },
  { name: 'YOLOv8', year: '2023', type: '检测', color: '#f59e0b', desc: '实时目标检测标杆，平衡速度与精度，工业界广泛应用', params: '43.7M', top1Acc: '53.9%', task: '检测', tags: ['实时', 'Anchor-Free', '多尺度'] },
  { name: 'Swin Transformer', year: '2021', type: 'Transformer', color: '#14b8a6', desc: '引入窗口注意力机制，在检测和分割任务上全面超越CNN', params: '49.6M', top1Acc: '83.0%', task: '分类/检测', tags: ['窗口注意力', '分层结构', 'SOTA'] },
]);

/* ========== 曲线教程数据 ========== */
const curveGuides = [
  {
    id: 'healthy-loss', badge: '正常收敛', color: '#4ade80',
    title: '健康的损失曲线 — Loss 持续下降',
    annotations: [
      { label: '训练损失', color: '#f87171', desc: '持续下降且趋于平稳，说明模型在学习' },
      { label: '验证损失', color: '#fbbf24', desc: '跟随训练损失下降，说明泛化能力良好' },
    ],
  },
  {
    id: 'overfit-loss', badge: '过拟合', color: '#ef4444',
    title: '过拟合 — 训练损失降但验证损失升',
    annotations: [
      { label: '训练损失', color: '#f87171', desc: '一直下降，模型在"背诵"训练数据' },
      { label: '验证损失', color: '#fbbf24', desc: '在Epoch 20后开始回升，模型已过拟合' },
    ],
  },
  {
    id: 'accuracy-curve', badge: '准确率', color: '#60a5fa',
    title: '准确率曲线 — 训练 vs 验证的差距',
    annotations: [
      { label: '训练准确率', color: '#4ade80', desc: '接近100%说明模型容量足够' },
      { label: '验证准确率', color: '#60a5fa', desc: '与训练准确率差距越大，过拟合越严重' },
    ],
  },
  {
    id: 'lr-comparison', badge: '学习率', color: '#f59e0b',
    title: '学习率影响 — 四个等级对比',
    annotations: [
      { label: '太大 0.1', color: '#ef4444', desc: '剧烈震荡，无法收敛' },
      { label: '合适 0.001', color: '#4ade80', desc: '平滑下降，最快收敛' },
      { label: '太小 0.0001', color: '#94a3b8', desc: '下降极慢，需要更多Epoch' },
    ],
  },
];

const scrollToTop = () => window.scrollTo({ top: 0, behavior: 'smooth' });
const scrollToTutorials = () => document.getElementById('tutorials')?.scrollIntoView({ behavior: 'smooth' });

// 官方文章
const showArticleIndex = ref(false);
const officialArticles = ref<any[]>([]);
const readerOpen = ref(false);
const activeArticle = ref<any>(null);
const articleLoading = ref(false);

const closeKnowledgeOverlays = () => {
  showArticleIndex.value = false;
  readerOpen.value = false;
  articleLoading.value = false;
  activeArticle.value = null;
  document.documentElement.classList.remove('knowledge-overlay-open');
};

const clearKnowledgeModalResidue = () => {
  if (typeof document === 'undefined') return;
  document.body.style.removeProperty('overflow');
  document.body.style.removeProperty('padding-right');
  document.documentElement.classList.remove('knowledge-overlay-open');
  document.querySelectorAll<HTMLElement>('.reader-backdrop, .v-modal, .el-overlay').forEach((el) => {
    if (el.classList.contains('reader-backdrop')) {
      if (!readerOpen.value) el.remove();
      return;
    }
    const hasLivePanel = el.querySelector('.el-dialog, .el-drawer, .el-message-box');
    if (!hasLivePanel) el.remove();
  });
};

const openKnowledgeArticle = async (articleOrId: any) => {
  const id = typeof articleOrId === 'object' ? articleOrId.id : articleOrId;
  if (!id) return;
  readerOpen.value = true;
  document.documentElement.classList.add('knowledge-overlay-open');
  articleLoading.value = true;
  activeArticle.value = typeof articleOrId === 'object' && articleOrId.content ? articleOrId : null;
  try {
    const res = await forumApi.getKnowledgeArticle(id);
    activeArticle.value = res.data.data || res.data;
  } catch {
    activeArticle.value = typeof articleOrId === 'object' ? articleOrId : null;
  }
  articleLoading.value = false;
};
const fetchOfficialArticles = async () => {
  try {
    const res = await forumApi.listKnowledgeArticles();
    if (res.data.code === 200) officialArticles.value = res.data.data || [];
  } catch(e) {}
};

onMounted(() => {
  clearKnowledgeModalResidue();
  nextTick(() => initCurveCharts());
  fetchOfficialArticles();
});

onBeforeUnmount(() => {
  closeKnowledgeOverlays();
  clearKnowledgeModalResidue();
  Object.values(curveRefs.value).forEach(el => {
    const instance = echarts.getInstanceByDom(el);
    if (instance) instance.dispose();
  });
});

watch(() => themeStore.isDarkMode, () => {
  // 重建图表以适应主题
  Object.values(curveRefs.value).forEach(el => {
    const instance = echarts.getInstanceByDom(el);
    if (instance) instance.dispose();
  });
  nextTick(() => initCurveCharts());
});

watch(readerOpen, (open) => {
  document.documentElement.classList.toggle('knowledge-overlay-open', open);
});
</script>

<style scoped>
.knowledge-page {
  position: relative;
  isolation: isolate;
  width: 100% !important;
  max-width: none !important;
  min-height: 100vh;
  padding: 0 !important;
  overflow-x: hidden;
  background: transparent;
}

.knowledge-page::before {
  display: none;
}

.knowledge-page > * {
  position: relative;
  z-index: 1;
}

/* ========== Hero ========== */
.kg-hero {
  position: relative;
  width: 100%;
  height: min(820px, calc(100vh - 72px));
  min-height: 640px;
  overflow: hidden;
  background:
    radial-gradient(circle at 18% 14%, rgba(var(--primary-rgb), 0.14), transparent 30%),
    radial-gradient(circle at 86% 22%, color-mix(in srgb, var(--accent-glow) 12%, transparent), transparent 28%);
}
:global(.light .kg-hero) {
  background:
    radial-gradient(circle at 16% 14%, rgba(var(--primary-rgb), 0.09), transparent 30%),
    radial-gradient(circle at 82% 18%, color-mix(in srgb, var(--accent-glow) 8%, transparent), transparent 30%),
    linear-gradient(180deg, rgba(237, 243, 238, 0.4), rgba(237, 243, 238, 0.12));
}
.kg-3d-canvas {
  position: absolute;
  inset: 0;
  z-index: 1;
}

.kg-hero-vignette {
  position: absolute;
  inset: 0;
  z-index: 10;
  pointer-events: none;
  background:
    linear-gradient(90deg, color-mix(in srgb, var(--bg-color) 72%, transparent) 0%, color-mix(in srgb, var(--bg-color) 28%, transparent) 34%, transparent 58%),
    linear-gradient(0deg, var(--bg-color) 0%, transparent 18%);
}
:global(.light .kg-hero-vignette) {
  display: none;
}

/* 左上标题 */
.kg-hero-title {
  position: absolute;
  top: clamp(22px, 4.2vw, 48px);
  left: clamp(18px, 5vw, 64px);
  z-index: 20;
  width: min(480px, calc(100vw - 36px));
  padding: clamp(15px, 1.7vw, 20px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 22px;
  background:
    linear-gradient(135deg, rgba(var(--primary-rgb), 0.075), transparent 56%),
    rgba(var(--glass-bg-rgb), 0.42);
  box-shadow: 0 24px 70px rgba(0, 0, 0, 0.22);
  backdrop-filter: blur(22px) saturate(150%);
}
:global(.light .kg-hero-title) {
  background:
    linear-gradient(135deg, rgba(var(--primary-rgb), 0.09), transparent 58%),
    rgba(var(--glass-bg-rgb),0.54);
  border-color: rgba(var(--primary-rgb),0.16);
  box-shadow: 0 22px 58px rgba(29,56,46,0.08);
}
.hero-badge {
  display: inline-block;
  padding: 5px 14px;
  border-radius: 999px;
  border: 1px solid rgba(var(--primary-rgb),0.34);
  background: rgba(var(--primary-rgb),0.1);
  font-size: 9px;
  font-weight: 900;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--primary-color);
  margin-bottom: 12px;
  backdrop-filter: blur(8px);
}
.kg-hero-title h1 {
  font-size: clamp(32px, 4.4vw, 52px);
  font-weight: 900;
  letter-spacing: 0;
  color: var(--text-primary);
  margin: 0 0 12px;
  line-height: 1.02;
}
:global(.light .kg-hero-title h1) { color: var(--text-primary); }
.kg-hero-title h1 em {
  display: block;
  color: var(--primary-color);
  font-style: normal;
}
.kg-hero-title p {
  max-width: 54ch;
  font-size: 12.5px;
  line-height: 1.68;
  color: var(--text-secondary);
  margin: 0 0 16px;
}
:global(.light .kg-hero-title p) { color: var(--text-secondary); }
.hero-actions { display:flex; flex-wrap:wrap; gap:10px; }
.hero-actions button,
.article-index-button {
  height: 36px;
  padding: 0 15px;
  border: 1px solid rgba(var(--primary-rgb),0.28);
  border-radius: 999px;
  background: rgba(var(--primary-rgb),0.16);
  color: color-mix(in srgb, var(--primary-color) 62%, white);
  font-size: 12px;
  font-weight: 850;
  cursor: pointer;
  transition: transform 180ms ease, border-color 180ms ease, background 180ms ease;
}
.hero-actions button:hover,
.article-index-button:hover {
  transform: translateY(-1px);
  border-color: rgba(var(--primary-rgb), 0.42);
}
.hero-actions button.ghost {
  background: rgba(255,255,255,0.055);
  color: var(--text-secondary);
}
:global(.light .hero-actions button.ghost) { color: var(--text-secondary); background: rgba(var(--primary-rgb),0.055); }

.kg-system-panel {
  position: absolute;
  right: 32px;
  top: 48px;
  z-index: 20;
  width: 160px;
  padding: 14px;
  border: 1px solid rgba(var(--primary-rgb), 0.14);
  border-radius: 16px;
  background:
    linear-gradient(135deg, rgba(var(--primary-rgb), 0.08), transparent 60%),
    rgba(var(--glass-bg-rgb), 0.5);
  backdrop-filter: blur(16px);
}
:global(.light .kg-system-panel) { background: rgba(var(--glass-bg-rgb),0.52); border-color: rgba(var(--primary-rgb),0.16); box-shadow: 0 18px 48px rgba(29,56,46,0.07); }
.kg-system-panel span,
.kg-system-panel em { display:block; color:var(--text-muted); font-size:10px; font-weight:850; text-transform:uppercase; font-style:normal; letter-spacing: 0.04em; }
:global(.light .kg-system-panel span),
:global(.light .kg-system-panel em) { color:var(--text-muted); }
.kg-system-panel strong { display:block; margin:5px 0; color:var(--primary-color); font-size:30px; font-weight:950; }

/* 右上简化图例 */
.kg-legend-simple {
  position: absolute;
  top: 160px;
  right: 32px;
  z-index: 20;
  display: flex;
  flex-direction: column;
  gap: 5px;
  background: rgba(var(--glass-bg-rgb), 0.42);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(var(--primary-rgb), 0.12);
  border-radius: 16px;
  padding: 12px 14px;
}
:global(.light .kg-legend-simple) { background: rgba(var(--glass-bg-rgb),0.52); color: var(--text-secondary); border-color: rgba(var(--primary-rgb),0.16); box-shadow: 0 18px 48px rgba(29,56,46,0.07); }
.leg-row {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 10px;
  font-weight: 750;
  color: var(--text-secondary);
}
:global(.light .leg-row) { color: var(--text-secondary); }
.leg-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
  box-shadow: 0 0 10px rgba(var(--primary-rgb), 0.18);
}
.leg-core { background: var(--primary-color); box-shadow: 0 0 12px rgba(var(--primary-rgb), 0.58); }
.leg-module { background: var(--tone-blue); box-shadow: 0 0 10px color-mix(in srgb, var(--tone-blue) 42%, transparent); }
.leg-arch { background: var(--tone-violet); box-shadow: 0 0 10px color-mix(in srgb, var(--tone-violet) 42%, transparent); }
.leg-skill { background: var(--tone-amber); box-shadow: 0 0 10px color-mix(in srgb, var(--tone-amber) 42%, transparent); }
.leg-metric { background: var(--tone-rose); box-shadow: 0 0 10px color-mix(in srgb, var(--tone-rose) 42%, transparent); }
.leg-deploy { background: var(--tone-green); box-shadow: 0 0 10px color-mix(in srgb, var(--tone-green) 42%, transparent); }
.leg-read {
  background: transparent;
  border: 1px dashed rgba(var(--primary-rgb), 0.55);
  box-shadow: none;
}

/* ========== Sections ========== */
.kb-section {
  max-width: 1200px;
  margin: 0 auto;
  padding: 68px 24px 24px;
}
.kb-section-primary { padding-top: 72px; }
.section-intro {
  display: grid;
  grid-template-columns: minmax(0, 0.82fr) minmax(240px, 0.42fr);
  gap: 20px;
  align-items: end;
  text-align: left;
  margin-bottom: 28px;
  padding-bottom: 18px;
  border-bottom: 1px solid var(--border-color);
}
.section-kicker {
  display: inline-flex;
  width: max-content;
  margin-bottom: 10px;
  padding: 4px 9px;
  border: 1px solid rgba(var(--primary-rgb), 0.24);
  border-radius: 999px;
  background: rgba(var(--primary-rgb), 0.08);
  color: var(--primary-color);
  font-size: 10px;
  font-weight: 900;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}
.section-intro h2 {
  font-size: clamp(24px, 3vw, 34px);
  font-weight: 900;
  color: var(--text-primary);
  margin: 0 0 10px;
}
.section-intro p {
  align-self: end;
  max-width: 420px;
  justify-self: end;
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.7;
  margin: 0;
}

/* ========== 教程卡片 ========== */
.tutorial-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  align-items: start;
}
.tutorial-column {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.tutorial-card {
  position: relative;
  display: block;
  width: 100%;
  overflow: hidden;
  background:
    linear-gradient(135deg, rgba(var(--primary-rgb), 0.04), transparent 58%),
    color-mix(in srgb, var(--surface-1) 78%, transparent);
  border: 1px solid rgba(var(--primary-rgb), 0.13);
  border-radius: 18px;
  padding: 20px 24px;
  cursor: pointer;
  transition: transform 220ms ease, border-color 220ms ease, box-shadow 220ms ease;
  backdrop-filter: blur(18px) saturate(145%);
}
.tutorial-card.expanded {
  border-color: rgba(var(--primary-rgb), 0.24);
  box-shadow: 0 18px 42px rgba(var(--primary-rgb), 0.08);
}
.tutorial-card:hover {
  transform: translateY(-2px);
  border-color: rgba(var(--primary-rgb), 0.34);
  box-shadow: var(--shadow-hover);
}
.tut-header {
  width: 100%;
  padding: 0;
  border: 0;
  background: transparent;
  text-align: left;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 16px;
}
.tut-header:focus-visible {
  outline: 2px solid rgba(var(--primary-rgb), 0.38);
  outline-offset: 8px;
  border-radius: 12px;
}
.tut-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 17px;
  flex-shrink: 0;
  filter: saturate(0.9);
}
.tut-info { flex: 1; min-width: 0; }
.tut-info h3 {
  font-size: 15px;
  font-weight: 800;
  margin: 0 0 2px;
}
.tut-duration {
  font-size: 10px;
  color: var(--text-secondary);
  font-weight: 600;
  text-transform: uppercase;
}
.tut-arrow {
  color: var(--text-secondary);
  transition: transform 360ms cubic-bezier(0.16, 1, 0.3, 1), color 220ms ease;
  flex-shrink: 0;
}
.tutorial-card.expanded .tut-arrow,
.tut-header:hover .tut-arrow {
  color: var(--primary-color);
}
.tut-arrow.rotated { transform: rotate(180deg); }
.tut-content {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid rgba(var(--primary-rgb), 0.12);
  overflow: hidden;
  will-change: height, opacity, margin-top, padding-top;
  transition:
    height 360ms cubic-bezier(0.16, 1, 0.3, 1),
    opacity 240ms ease,
    margin-top 360ms cubic-bezier(0.16, 1, 0.3, 1),
    padding-top 360ms cubic-bezier(0.16, 1, 0.3, 1),
    border-top-color 280ms ease;
}
.tut-desc {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.7;
  margin: 0 0 12px;
}
.tut-detail { font-size: 13px; color: var(--text-primary); line-height: 1.8; }
.tut-detail :deep(h4) { font-size: 13px; font-weight: 800; margin: 16px 0 8px; color: var(--text-primary); border-left: 3px solid var(--primary-color); padding-left: 10px; }
.tut-detail :deep(p) { margin: 0 0 8px; color: var(--text-secondary); }
.tut-detail :deep(ul) { padding-left: 18px; margin: 0 0 12px; }
.tut-detail :deep(ol) { padding-left: 18px; margin: 0 0 12px; }
.tut-detail :deep(li) { color: var(--text-secondary); margin-bottom: 4px; }
.tut-detail :deep(li::marker) { color: var(--primary-color); }
.tut-detail :deep(strong) { color: var(--text-primary); }
.tut-detail :deep(a) { color: var(--primary-color); }
.tut-detail :deep(code) { background: var(--bg-input); padding: 1px 5px; border-radius: 4px; font-size: 12px; color: var(--primary-color); }

/* ========== 曲线展示 ========== */
.curves-showcase {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}
@media (max-width: 900px) { .curves-showcase { grid-template-columns: 1fr; } }
.curve-card {
  background:
    linear-gradient(135deg, rgba(var(--primary-rgb), 0.035), transparent 60%),
    color-mix(in srgb, var(--surface-1) 78%, transparent);
  border: 1px solid rgba(var(--primary-rgb), 0.13);
  border-radius: 18px;
  padding: 20px;
  backdrop-filter: blur(18px) saturate(145%);
  transition: transform 220ms ease, border-color 220ms ease, box-shadow 220ms ease;
}
.curve-card:hover {
  transform: translateY(-2px);
  border-color: rgba(var(--primary-rgb), 0.3);
  box-shadow: var(--shadow-soft);
}
.curve-card-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}
.curve-badge {
  font-size: 9px;
  font-weight: 900;
  text-transform: uppercase;
  padding: 2px 10px;
  border-radius: 20px;
  white-space: nowrap;
}
.curve-title {
  font-size: 13px;
  font-weight: 800;
  color: var(--text-primary);
  margin: 0;
}
.curve-chart { width: 100%; height: 260px; }
.curve-annotations {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--border-color);
}
.annotation { display: flex; align-items: center; gap: 6px; }
.ann-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.ann-text { font-size: 10px; color: var(--text-secondary); font-weight: 600; }

/* ========== 模型展示 ========== */
.models-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}
@media (max-width: 1000px) { .models-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 600px) { .models-grid { grid-template-columns: 1fr; } }
.model-showcase-card {
  position: relative;
  min-height: 260px;
  border-radius: 18px;
  overflow: hidden;
  border: 1px solid rgba(var(--primary-rgb), 0.13);
  background: color-mix(in srgb, var(--surface-1) 78%, transparent);
  transition: transform 220ms ease, border-color 220ms ease, box-shadow 220ms ease;
  backdrop-filter: blur(18px) saturate(145%);
}
.model-showcase-card:hover {
  transform: translateY(-3px);
  border-color: rgba(var(--primary-rgb), 0.3);
  box-shadow: var(--shadow-hover);
}
.model-card-bg {
  position: absolute;
  inset: 0;
  opacity: 0.5;
}
.model-card-content {
  position: relative;
  padding: 20px;
  z-index: 1;
}
.model-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.model-year {
  font-size: 10px;
  font-weight: 800;
  color: var(--text-secondary);
  background: var(--bg-input);
  padding: 2px 10px;
  border-radius: 20px;
}
.model-type {
  font-size: 10px;
  font-weight: 800;
  text-transform: uppercase;
}
.model-card-name {
  font-size: 18px;
  font-weight: 900;
  color: var(--text-primary);
  margin: 0 0 8px;
}
.model-card-desc {
  font-size: 12px;
  color: var(--text-secondary);
  line-height: 1.6;
  margin: 0 0 14px;
}
.model-card-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin-bottom: 12px;
}
.model-stat {
  min-width: 0;
  text-align: left;
  padding: 8px;
  border: 1px solid rgba(var(--primary-rgb), 0.1);
  border-radius: 12px;
  background: color-mix(in srgb, var(--surface-2) 72%, transparent);
}
.stat-value { font-size: 16px; font-weight: 900; display: block; }
.stat-label { font-size: 9px; color: var(--text-secondary); text-transform: uppercase; }
.model-card-tags { display: flex; flex-wrap: wrap; gap: 4px; }
.model-tag {
  font-size: 9px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 8px;
  background: var(--bg-input);
  color: var(--text-secondary);
}

/* ========== CTA ========== */
.kb-cta {
  text-align: center;
  width: min(1200px, calc(100% - 48px));
  margin: 52px auto 64px;
  padding: 58px 24px;
  position: relative;
  overflow: hidden;
  border: 1px solid rgba(var(--primary-rgb), 0.14);
  border-radius: 24px;
  background:
    linear-gradient(135deg, rgba(var(--primary-rgb), 0.09), transparent 62%),
    color-mix(in srgb, var(--surface-1) 78%, transparent);
  backdrop-filter: blur(20px) saturate(145%);
}
.cta-glow {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 500px;
  height: 500px;
  border-radius: 50%;
  background: var(--primary-color);
  opacity: 0.06;
  filter: blur(120px);
  pointer-events: none;
}
.kb-cta h2 {
  font-size: 32px;
  font-weight: 900;
  color: var(--text-primary);
  margin: 0 0 12px;
  position: relative;
}
.kb-cta p {
  font-size: 15px;
  color: var(--text-secondary);
  margin: 0 0 32px;
  position: relative;
}
.flex { display: flex; }
.gap-4 { gap: 16px; }
.justify-center { justify-content: center; }
.articles-dialog-list { display: flex; flex-direction: column; gap: 2px; }
.article-dialog-item {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: start;
  gap: 12px;
  padding: 14px 16px;
  border: 1px solid transparent;
  border-radius: 14px;
  cursor: pointer;
  transition: transform .2s ease, border-color .2s ease, background .2s ease;
}
.article-dialog-item:hover {
  border-color: rgba(var(--primary-rgb), 0.24);
  background: rgba(var(--primary-rgb), 0.08);
  transform: translateX(-2px);
}
.article-index-panel {
  position: fixed;
  right: 18px;
  top: 88px;
  bottom: 24px;
  z-index: 2600;
  width: min(440px, 90vw);
  padding: 16px;
  border: 1px solid rgba(var(--primary-rgb),0.2);
  border-radius: 18px;
  background:
    radial-gradient(circle at 18% 0%, rgba(var(--primary-rgb), 0.14), transparent 36%),
    rgba(var(--glass-bg-rgb),0.9);
  box-shadow: var(--shadow-floating);
  backdrop-filter: blur(20px);
  overflow-y: auto;
}
.article-index-head { display:flex; justify-content:space-between; align-items:flex-start; gap: 12px; margin-bottom:12px; padding: 4px 2px 12px; border-bottom: 1px solid var(--border-color); }
.article-index-head span { display:block; color:var(--text-primary); font-size:13px; font-weight:900; text-transform:uppercase; letter-spacing:.08em; }
.article-index-head small { display:block; margin-top:4px; color:var(--text-muted); font-size:11px; line-height:1.45; }
.article-index-head button { width:30px; height:30px; border-radius:999px; border:1px solid var(--border-color); background:transparent; color:var(--text-secondary); cursor:pointer; font-size:18px; }
.article-index-enter-active,
.article-index-leave-active { transition: opacity .22s ease, transform .22s ease; }
.article-index-enter-from,
.article-index-leave-to { opacity:0; transform: translateX(30px); }
.article-dialog-item h4 { font-size: 14px; font-weight: 800; color: var(--text-primary); margin: 0 0 2px; }
.article-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; margin-top: 6px; box-shadow: 0 0 12px rgba(var(--primary-rgb), 0.55); }
.article-meta-text { font-size: 10px; color: var(--text-secondary); }
.article-index-empty { padding: 30px 12px; color: var(--text-muted); font-size: 12px; text-align: center; }
.article-detail-content { font-size: 14px; line-height: 1.9; color: var(--text-primary); }
.article-detail-content :deep(h1), .article-detail-content :deep(h2), .article-detail-content :deep(h3) { margin: 16px 0 8px; }
.article-detail-content :deep(pre) { background: var(--bg-input); padding: 16px; border-radius: 12px; overflow-x: auto; }
.article-detail-content :deep(code) { background: var(--bg-input); padding: 2px 6px; border-radius: 4px; font-size: 13px; }
.article-detail-content :deep(li) { margin-left: 16px; }
.mt-6 { margin-top: 24px; }
.text-center { text-align: center; }

@media (max-width: 980px) {
  .kg-hero {
    height: auto;
    min-height: 760px;
  }
  .kg-hero-title {
    top: 22px;
  }
  .kg-legend-simple {
    top: auto;
    right: 18px;
    left: 18px;
    bottom: 96px;
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .kg-system-panel {
    right: 18px;
    top: 238px;
    bottom: auto;
  }
  .section-intro {
    grid-template-columns: 1fr;
  }
  .section-intro p {
    justify-self: start;
  }
  .tutorial-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .kb-section {
    padding-inline: 16px;
  }
  .kg-legend-simple {
    grid-template-columns: 1fr;
    bottom: 112px;
  }
  .kg-system-panel {
    left: 18px;
    right: auto;
    top: 250px;
    bottom: auto;
  }
  .model-card-stats {
    grid-template-columns: 1fr;
  }
}
</style>
