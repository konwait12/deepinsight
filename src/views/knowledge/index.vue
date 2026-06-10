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
          <button type="button" class="ghost" @click="openArticleIndex">{{ pageText.officialArticles }} {{ officialArticles.length }}</button>
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
        <el-button class="article-index-button" @click="openArticleIndex">
          {{ pageText.viewOfficialPrefix }} {{ officialArticles.length }} {{ pageText.viewOfficialSuffix }}
        </el-button>
      </div>
    </section>

    <Teleport to="body">
      <Transition name="article-index">
        <aside
          v-if="showArticleIndex"
          class="article-index-panel"
          :class="{ 'article-index-panel--reader-open': readerOpen }"
        >
          <div class="article-index-head">
            <div>
              <span>{{ pageText.articleIndexTitle }}</span>
              <small>{{ pageText.articleIndexHint }}</small>
            </div>
            <button type="button" @click="closeArticleIndex">×</button>
          </div>
          <div class="articles-dialog-list">
            <div v-for="a in officialArticles" :key="a.id" class="article-dialog-item" @click="openKnowledgeArticle(a.id)">
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
    </Teleport>

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
                <span class="stat-label">{{ model.scaleLabel || '规模' }}</span>
              </div>
              <div class="model-stat">
                <span class="stat-value" :style="{ color: model.color }">{{ model.top1Acc }}</span>
                <span class="stat-label">{{ model.metricLabel || '核心指标' }}</span>
              </div>
              <div class="model-stat">
                <span class="stat-value" :style="{ color: model.color }">{{ model.task }}</span>
                <span class="stat-label">{{ model.taskLabel || '任务' }}</span>
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
      <div class="kb-action-row">
        <el-button class="kb-cta-button kb-cta-button-primary" @click="$router.push(ROUTES.TRAINING)">
          {{ pageText.ctaStart }}
        </el-button>
        <el-button class="kb-cta-button kb-cta-button-secondary" @click="scrollToTop">
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
import { useRoute } from 'vue-router';
import { useThemeStore } from '@/stores/theme.store';
import { ArrowDown } from '@element-plus/icons-vue';
import * as echarts from 'echarts';
import KnowledgeGraph3D from './KnowledgeGraph3D.vue';
import { renderMarkdown } from '@/utils/markdown';
import ArticleReaderDrawer from '@/components/common/ArticleReaderDrawer.vue';
import { forumApi } from '@/api';
import { useI18n } from 'vue-i18n';
import { buildChartTheme, chartAlpha, chartGrid, chartLegend, chartTooltip } from '@/utils/chartTheme';
import { ROUTES } from '@/constants';

const themeStore = useThemeStore();
const { locale } = useI18n();
const route = useRoute();
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
  heroBadge: 'DeepInsight Atlas',
  heroTitle: '平台与模型',
  heroTitleEm: '知识图谱',
  heroDesc: '围绕 9 个推荐系统模型、数据资产、评测指标、接入状态和 AI 助手组织知识。点击节点即可查看对应文章。',
  startLearning: '从平台入门',
  officialArticles: '平台文章',
  atlasStatus: 'Knowledge Status',
  articlesIndexed: 'articles indexed',
  legendCore: '平台核心',
  legendModule: '功能入口',
  legendArch: '模型家族',
  legendSkill: '使用流程',
  legendMetric: '评测指标',
  legendDeploy: 'AI 导航',
  legendRead: '右侧阅读',
  foundationKicker: 'Platform Guide',
  tutorialTitle: '零基础上手 DeepInsight',
  tutorialDesc: '从模型总览、数据资产、推荐指标、接入状态和 AI 助手开始，按平台功能学习。',
  viewOfficialPrefix: '查看',
  viewOfficialSuffix: '篇平台知识文章',
  articleIndexTitle: '平台知识文章索引',
  articleIndexHint: '点击后从右侧打开，不离开知识中心',
  related: '关联',
  general: '平台通用',
  views: '浏览',
  noOfficialArticles: '暂无平台知识文章',
  curveKicker: 'Model Signals',
  curveTitle: '推荐模型图表怎么看',
  curveDesc: '把 HR/NDCG/MRR、数据规模、日志状态和服务状态拆成图表，帮助理解每个模型的接入完整度。',
  modelKicker: '模型清单',
  modelTitle: '已接入模型家族',
  modelDesc: '模型清单聚焦本地 model 目录中的 9 个推荐系统模型，不再按旧任务类型分级。',
  ctaTitle: '下一步进入模型总览',
  ctaDesc: '先确认 9 个推荐模型状态，再进入接入测试中心和性能看板做横向分析。',
  ctaStart: '进入模型总览',
  ctaBack: '回到知识图谱',
  readerSource: '平台知识文章',
} : {
  heroBadge: 'DeepInsight Atlas',
  heroTitle: 'Platform And Model',
  heroTitleEm: 'Knowledge Map',
  heroDesc: 'Knowledge is organized around 9 recommender models, data assets, metrics, integration status, and the AI assistant.',
  startLearning: 'Start With Platform',
  officialArticles: 'Platform Articles',
  atlasStatus: 'Knowledge Status',
  articlesIndexed: 'articles indexed',
  legendCore: 'Platform Core',
  legendModule: 'Entries',
  legendArch: 'Model Families',
  legendSkill: 'Workflow',
  legendMetric: 'Metrics',
  legendDeploy: 'AI Navigation',
  legendRead: 'Read on the right',
  foundationKicker: 'Platform Guide',
  tutorialTitle: 'Beginner Guide For DeepInsight',
  tutorialDesc: 'Learn the real recommender workflow: model status, data assets, ranking metrics, visualization, and AI assistant.',
  viewOfficialPrefix: 'View',
  viewOfficialSuffix: 'platform knowledge articles',
  articleIndexTitle: 'Platform Article Index',
  articleIndexHint: 'Open articles from the right without leaving Knowledge Center',
  related: 'Related',
  general: 'Platform',
  views: 'Views',
  noOfficialArticles: 'No platform knowledge articles yet',
  curveKicker: 'Model Signals',
  curveTitle: 'How To Read Model Charts',
  curveDesc: 'HR/NDCG/MRR, data scale, log status, and service readiness are visualized separately.',
  modelKicker: 'Model Catalog',
  modelTitle: 'Integrated Model Families',
  modelDesc: 'The catalog focuses on the 9 integrated recommender-system models.',
  ctaTitle: 'Next: Open Model Overview',
  ctaDesc: 'Check model status first, then run access tests and visualization comparisons.',
  ctaStart: 'Open Model Overview',
  ctaBack: 'Back To Graph',
  readerSource: 'Platform Knowledge Article',
});

type TutorialItem = {
  icon: string
  title: string
  duration: string
  color: string
  expanded: boolean
  desc: string
  detail: string
}

const expandedTutorials = ref<Record<string, boolean>>({});
const tutorials = computed<TutorialItem[]>(() => {
  const rows = isZh.value ? [
    {
      icon: '01',
      title: '先认识平台入口',
      duration: '3 分钟',
      color: '#42e6a4',
      desc: 'DeepInsight 当前围绕模型总览、接入测试、可视化分析、数据中心和 AI 工作区展开。',
      detail: [
        '#### 推荐学习顺序',
        '1. 进入模型总览，确认 9 个推荐模型的接入状态。',
        '2. 进入性能看板，查看 HR、NDCG、MRR、数据规模和接入证据。',
        '3. 进入接入测试，验证 BSARec Job 的后端代理和服务健康度。',
        '4. 进入数据中心，查看用户、物品、交互和序列结构。',
        '5. 使用 AI 助手提问，让它解释指标、状态和页面跳转入口。',
        '#### 当前边界',
        '平台暂时不提供用户上传模型服务。知识库围绕 9 个推荐系统模型、数据资产和日志状态解释。',
      ].join('\n'),
    },
    {
      icon: '02',
      title: '9 个模型怎么分',
      duration: '4 分钟',
      color: '#60a5fa',
      desc: '当前 9 个模型全部是推荐系统模型，只按架构、数据和接入证据区分。',
      detail: [
        '#### BSARec 家族',
        'BSARec Job 面向岗位推荐，登记了后端代理；BSARec 多数据集版本拥有权重和评估日志。',
        '#### Transformer 序列推荐',
        'BERT4Rec、SASRec、TiSASRec 分别代表双向建模、自注意力建模和时间间隔建模。',
        '#### 增强推荐与框架',
        'DuoRec、FEARec、FMLP-Rec 分别对应对比、频域和滤波增强路线；RecBole 是推荐实验框架。',
      ].join('\n'),
    },
    {
      icon: '03',
      title: '数据资产看什么',
      duration: '5 分钟',
      color: '#22c55e',
      desc: '推荐系统数据先看用户数、物品数、交互数和序列长度。',
      detail: [
        '#### 关键字段',
        '用户数决定覆盖范围，物品数决定候选空间，交互数决定训练信号密度，序列长度决定模型能学到多长的兴趣轨迹。',
        '#### 典型数据',
        'Job 有 15,000 用户、1,152 物品、409,466 交互；BSARec 汇总 6 份数据，交互数 2,030,952；SASRec 汇总 4 份数据，交互数 5,367,798。',
        '#### 注意',
        '跨数据集汇总时 ID 不做合并去重，因此规模只能表示本地文件资产，不等于统一业务库规模。',
      ].join('\n'),
    },
    {
      icon: '04',
      title: '推荐指标怎么看',
      duration: '5 分钟',
      color: '#38bdf8',
      desc: 'HR@K、NDCG@K 和 MRR 都围绕排序质量解释。',
      detail: [
        '#### HR@K',
        '看前 K 个推荐里有没有命中目标，更像“有没有推荐中”。',
        '#### NDCG@K',
        '看命中是否排得靠前，越靠前得分越高。',
        '#### MRR',
        '看第一个相关结果出现得早不早，适合评估用户需要扫多久才能看到有效推荐。',
        '#### 当前可用指标',
        'BSARec HR@10 0.1008、NDCG@10 0.0611；FMLP-Rec HR@10 0.4935、NDCG@10 0.3317、MRR 0.2985。',
      ].join('\n'),
    },
    {
      icon: '05',
      title: '接入状态怎么看',
      duration: '5 分钟',
      color: '#a78bfa',
      desc: '服务、权重、日志、代码和数据代表不同功能状态。',
      detail: [
        '#### 服务离线',
        'BSARec Job 权重、数据、日志和代理都存在，但本地推荐服务当前不可达，所以应显示服务离线。',
        '#### 权重+日志',
        'BSARec 和 FMLP-Rec 有可用评估指标，适合进入性能看板。',
        '#### 代码+数据',
        'BERT4Rec、DuoRec、FEARec、RecBole、SASRec、TiSASRec 具备代码和数据，缺少日志或服务时会显示待补充状态。',
      ].join('\n'),
    },
    {
      icon: '06',
      title: '数据中心做什么',
      duration: '4 分钟',
      color: '#f59e0b',
      desc: '数据中心用于用户数据集记录、结构预览和推荐数据可视化准备，不是模型上传入口。',
      detail: [
        '#### 推荐数据字段',
        '关注 user id、item id、时间顺序、评分/时间戳、交互稀疏度和最大序列长度。',
        '#### 数据质量',
        '优先检查空行、重复交互、极短序列、异常长序列和物品长尾分布。',
        '#### 平台边界',
        '上传数据后先看结构和分布；它不会自动变成模型训练数据，也不会自动生成后端推理服务。',
      ].join('\n'),
    },
    {
      icon: '07',
      title: '模型怎么比对',
      duration: '5 分钟',
      color: '#ec4899',
      desc: '先确定比指标、比数据、比服务，还是比模型机制。',
      detail: [
        '#### 比指标',
        '优先把有日志的模型放进指标排名，例如 BSARec 和 FMLP-Rec。',
        '#### 比数据',
        '看用户、物品、交互数、数据集数量和文件大小。',
        '#### 比服务',
        '看是否登记后端代理、服务健康度和调用结果。',
        '#### 比机制',
        'BERT4Rec 强调双向上下文，SASRec 强调自注意力，TiSASRec 强调时间间隔，DuoRec/FEARec/FMLP-Rec 强调增强路线。',
      ].join('\n'),
    },
    {
      icon: '08',
      title: 'AI 知识更新',
      duration: '4 分钟',
      color: '#14b8a6',
      desc: 'AI 助手基于站内模型、数据和可用指标回答。',
      detail: [
        '#### 好问题示例',
        '打开模型总览。',
        '解释 BSARec Job 为什么显示服务离线。',
        '比较 BSARec 和 FMLP-Rec 的 HR@10 和 NDCG@10。',
        'BERT4Rec 为什么只有代码和数据，没有指标？',
        '#### 回答边界',
        'AI 不应暴露模型目录、数据库连接、密钥或服务器路径。它应该解释模型名称、推荐系统定位、数据资产、指标状态和页面入口。',
      ].join('\n'),
    },
  ] : [
    {
      icon: '01',
      title: 'Start From Platform Entries',
      duration: '3 min',
      color: '#42e6a4',
      desc: 'DeepInsight is organized around model overview, access testing, visual analysis, data center, and AI workspace.',
      detail: [
        '#### Recommended path',
        '1. Open Model Overview to confirm the real status of the 9 recommender models.',
        '2. Open Performance Dashboard to inspect HR, NDCG, MRR, data scale, and readiness.',
        '3. Open Access Test to verify the BSARec Job backend proxy and service health.',
        '4. Open Data Center to inspect users, items, interactions, and sequence structure.',
        '5. Ask the AI assistant to explain metrics, state, and page entries.',
        '#### Current boundary',
        'The platform does not provide user-uploaded model services. The knowledge base explains recommender models, data assets, and log status.',
      ].join('\n'),
    },
    {
      icon: '02',
      title: 'How The 9 Models Differ',
      duration: '4 min',
      color: '#60a5fa',
      desc: 'All 9 models are recommender-system models; they differ by architecture, data, and integration maturity.',
      detail: [
        '#### BSARec family',
        'BSARec Job targets job recommendation and has a backend proxy. BSARec has multi-dataset artifacts, weights, and evaluation logs.',
        '#### Transformer sequential recommendation',
        'BERT4Rec, SASRec, and TiSASRec represent bidirectional, self-attention, and time-interval modeling.',
        '#### Enhanced recommendation and framework',
        'DuoRec, FEARec, and FMLP-Rec cover contrastive, frequency-domain, and filtering-enhanced routes. RecBole is an experiment framework.',
      ].join('\n'),
    },
    {
      icon: '03',
      title: 'What Real Data Means',
      duration: '5 min',
      color: '#22c55e',
      desc: 'Recommendation data should first be read by users, items, interactions, and sequence length.',
      detail: [
        '#### Key fields',
        'Users define coverage, items define candidate space, interactions define signal density, and sequence length defines how much preference history the model can learn.',
        '#### Typical assets',
        'Job has 15,000 users, 1,152 items, and 409,466 interactions. BSARec aggregates 6 datasets with 2,030,952 interactions. SASRec aggregates 4 datasets with 5,367,798 interactions.',
        '#### Note',
        'Cross-dataset totals are local file-asset scale. IDs are not merged or deduplicated into one business database.',
      ].join('\n'),
    },
    {
      icon: '04',
      title: 'How To Read Metrics',
      duration: '5 min',
      color: '#38bdf8',
      desc: 'HR@K, NDCG@K, and MRR all explain ranking quality.',
      detail: [
        '#### HR@K',
        'Whether the target appears in the top K recommendations.',
        '#### NDCG@K',
        'Whether the hit is ranked near the top. Earlier hits receive higher scores.',
        '#### MRR',
        'How early the first relevant result appears.',
        '#### Current real metrics',
        'BSARec: HR@10 0.1008 and NDCG@10 0.0611. FMLP-Rec: HR@10 0.4935, NDCG@10 0.3317, and MRR 0.2985.',
      ].join('\n'),
    },
    {
      icon: '05',
      title: 'How To Read Access State',
      duration: '5 min',
      color: '#a78bfa',
      desc: 'Service, weights, logs, code, and data describe different feature states.',
      detail: [
        '#### Service offline',
        'BSARec Job has weights, data, logs, and proxy registration, but the local recommender service can still be unreachable.',
        '#### Weights and logs',
        'BSARec and FMLP-Rec have real evaluation metrics that can be used in the performance dashboard.',
        '#### Code and data',
        'BERT4Rec, DuoRec, FEARec, RecBole, SASRec, and TiSASRec have code and data. Missing logs or services are shown as pending status.',
      ].join('\n'),
    },
    {
      icon: '06',
      title: 'What Data Center Does',
      duration: '4 min',
      color: '#f59e0b',
      desc: 'Data Center stores user dataset records, structure previews, and preparation material. It is not a model-upload entry.',
      detail: [
        '#### Recommendation data fields',
        'Check user id, item id, time order, ratings/timestamps, sparsity, and maximum sequence length.',
        '#### Data quality',
        'Inspect empty rows, duplicate interactions, very short sequences, abnormal long sequences, and item long-tail distribution.',
        '#### Platform boundary',
        'Uploaded data supports preview and analysis preparation. It does not automatically become model training data or an inference service.',
      ].join('\n'),
    },
    {
      icon: '07',
      title: 'How To Compare Models',
      duration: '5 min',
      color: '#ec4899',
      desc: 'First decide whether you are comparing metrics, data, services, or model mechanisms.',
      detail: [
        '#### Compare metrics',
        'Only models with real logs, such as BSARec and FMLP-Rec, should enter metric ranking.',
        '#### Compare data',
        'Read users, items, interactions, dataset count, and file size.',
        '#### Compare services',
        'Check backend proxy registration, service health, and callable results.',
        '#### Compare mechanisms',
        'BERT4Rec uses bidirectional context, SASRec uses self-attention, TiSASRec uses time intervals, and DuoRec/FEARec/FMLP-Rec use enhancement routes.',
      ].join('\n'),
    },
    {
      icon: '08',
      title: 'AI Training Rules',
      duration: '4 min',
      color: '#14b8a6',
      desc: 'The assistant must cite only real in-site models, data, and metrics.',
      detail: [
        '#### Good prompts',
        'Open Model Overview.',
        'Explain why BSARec Job shows service offline.',
        'Compare BSARec and FMLP-Rec by HR@10 and NDCG@10.',
        'Why does BERT4Rec have code and data but no metrics?',
        '#### Answer boundary',
        'AI must not expose model directories, database connections, secrets, or server paths. It should explain model name, recommender-system role, data assets, metric state, and page entry.',
      ].join('\n'),
    },
  ];

  return rows.map((row) => ({ ...row, expanded: expandedTutorials.value[row.icon] || false }));
});
const tutorialColumns = computed(() => {
  const columns = [[], []] as Array<typeof tutorials.value>;
  tutorials.value.forEach((tutorial, index) => {
    columns[index % 2].push(tutorial);
  });
  return columns;
});
const toggleTutorial = (tutorial: TutorialItem) => {
  expandedTutorials.value = {
    ...expandedTutorials.value,
    [tutorial.icon]: !expandedTutorials.value[tutorial.icon],
  };
};

/* ========== 曲线解读 ========== */
const initCurveCharts = () => {
  const theme = buildChartTheme();
  const textColor = theme.text;
  const borderColor = theme.border;
  const primaryWash = chartAlpha(theme.primary, 0.04);
  const primaryFill = chartAlpha(theme.primary, 0.18);
  const chartCopy = isZh.value ? {
    interactions: '交互数',
    data: '数据',
    weights: '权重',
    logs: '日志',
    service: '服务',
    structurePreview: '结构预览',
    missingCheck: '缺失检查',
    sequenceLength: '序列长度',
    longTail: '长尾分布',
    evaluationReady: '评估准备',
    qualityChecks: '推荐数据检查项',
  } : {
    interactions: 'Interactions',
    data: 'Data',
    weights: 'Weights',
    logs: 'Logs',
    service: 'Service',
    structurePreview: 'Structure preview',
    missingCheck: 'Missing check',
    sequenceLength: 'Sequence length',
    longTail: 'Long-tail distribution',
    evaluationReady: 'Evaluation readiness',
    qualityChecks: 'Recommendation data checks',
  };

  const charts: Array<{ id: string; option: any }> = [
    {
      id: 'rec-metrics',
      option: {
        backgroundColor: 'transparent',
        tooltip: { ...chartTooltip(theme), trigger: 'axis', axisPointer: { type: 'shadow' } },
        legend: { data: ['HR@10', 'NDCG@10'], bottom: 0, ...chartLegend(theme) },
        grid: chartGrid({ left: 45, right: 18, top: 24, bottom: 42 }),
        xAxis: { type: 'category', data: ['BSARec Job', 'BSARec', 'FMLP-Rec'], axisLabel: { color: textColor, fontSize: 10 }, axisLine: { lineStyle: { color: borderColor } } },
        yAxis: { type: 'value', axisLabel: { color: textColor, fontSize: 9 }, splitLine: { lineStyle: { color: borderColor, type: 'dashed' } } },
        series: [
          { name: 'HR@10', type: 'bar', data: [0.0287, 0.1008, 0.4935], itemStyle: { color: theme.primary, borderRadius: [7, 7, 0, 0] } },
          { name: 'NDCG@10', type: 'bar', data: [0.0132, 0.0611, 0.3317], itemStyle: { color: theme.accent, borderRadius: [7, 7, 0, 0] } },
        ],
      },
    },
    {
      id: 'data-scale',
      option: {
        backgroundColor: 'transparent',
        tooltip: { ...chartTooltip(theme), trigger: 'axis', axisPointer: { type: 'shadow' } },
        legend: { data: [chartCopy.interactions], bottom: 0, ...chartLegend(theme) },
        grid: chartGrid({ left: 45, right: 18, top: 18, bottom: 42 }),
        xAxis: { type: 'category', data: ['Job', 'BSARec', 'BERT4Rec', 'SASRec', 'TiSASRec'], axisLabel: { color: textColor, fontSize: 9 }, axisLine: { lineStyle: { color: borderColor } } },
        yAxis: { type: 'value', axisLabel: { color: textColor, fontSize: 9, formatter: (value: number) => `${value}M` }, splitLine: { lineStyle: { color: borderColor, type: 'dashed' } } },
        series: [
          { name: chartCopy.interactions, type: 'bar', data: [0.409, 2.031, 4.842, 5.368, 1.0], itemStyle: { color: theme.accent, borderRadius: [7, 7, 0, 0] } },
        ],
      },
    },
    {
      id: 'readiness-profile',
      option: {
        backgroundColor: 'transparent',
        tooltip: { ...chartTooltip(theme), trigger: 'axis', axisPointer: { type: 'shadow' } },
        legend: { data: [chartCopy.data, chartCopy.weights, chartCopy.logs, chartCopy.service], bottom: 0, ...chartLegend(theme) },
        grid: chartGrid({ left: 45, right: 18, top: 18, bottom: 42 }),
        xAxis: { type: 'category', data: ['BSARec Job', 'BSARec', 'FMLP-Rec', 'BERT4Rec'], axisLabel: { color: textColor, fontSize: 9 }, axisLine: { lineStyle: { color: borderColor } } },
        yAxis: { type: 'value', max: 100, axisLabel: { color: textColor, fontSize: 9, formatter: '{value}%' }, splitLine: { lineStyle: { color: borderColor, type: 'dashed' } } },
        series: [
          { name: chartCopy.data, type: 'bar', data: [100, 100, 100, 100], itemStyle: { color: theme.primary, borderRadius: [7, 7, 0, 0] } },
          { name: chartCopy.weights, type: 'bar', data: [100, 100, 100, 0], itemStyle: { color: theme.warning, borderRadius: [7, 7, 0, 0] } },
          { name: chartCopy.logs, type: 'bar', data: [100, 100, 100, 0], itemStyle: { color: theme.danger, borderRadius: [7, 7, 0, 0] } },
          { name: chartCopy.service, type: 'line', data: [0, 0, 0, 0], smooth: true, symbolSize: 8, lineStyle: { color: theme.muted, width: 2.5 }, itemStyle: { color: theme.muted } },
        ],
      },
    },
    {
      id: 'dataset-quality',
      option: {
        backgroundColor: 'transparent',
        tooltip: chartTooltip(theme),
        radar: {
          radius: '66%',
          indicator: [
            { name: chartCopy.structurePreview, max: 100 },
            { name: chartCopy.missingCheck, max: 100 },
            { name: chartCopy.sequenceLength, max: 100 },
            { name: chartCopy.longTail, max: 100 },
            { name: chartCopy.evaluationReady, max: 100 },
          ],
          axisName: { color: textColor, fontSize: 10 },
          splitLine: { lineStyle: { color: borderColor } },
          splitArea: { areaStyle: { color: ['transparent', primaryWash] } },
          axisLine: { lineStyle: { color: borderColor } },
        },
        series: [{
          type: 'radar',
          data: [{ value: [92, 86, 90, 84, 82], name: chartCopy.qualityChecks }],
          areaStyle: { color: primaryFill },
          lineStyle: { color: theme.primary, width: 2 },
          itemStyle: { color: theme.primary },
        }],
      },
    },
  ];

  charts.forEach(chart => {
    const el = curveRefs.value[chart.id];
    if (el) {
      const instance = echarts.getInstanceByDom(el) || echarts.init(el);
      instance.setOption(chart.option, true);
    }
  });
};

/* ========== 模型展示 ========== */
const modelShowcase = computed(() => isZh.value ? [
  { name: 'BSARec Job', year: '推荐系统', type: 'BSARec', color: '#22c55e', desc: '岗位序列推荐模型，权重、Job 数据和代理配置存在，但服务当前离线。', params: '409,466', scaleLabel: '交互数', top1Acc: 'HR@10 0.0287', metricLabel: '可用指标', task: '服务离线', taskLabel: '状态', tags: ['Job 数据', 'NDCG@10 0.0132', '后端代理'] },
  { name: 'BSARec', year: '推荐系统', type: 'BSARec', color: '#16a34a', desc: '多数据集序列推荐资产，Beauty 和 LastFM 已有权重与评估日志。', params: '2,030,952', scaleLabel: '交互数', top1Acc: 'HR@10 0.1008', metricLabel: '可用指标', task: '权重+日志', taskLabel: '状态', tags: ['6 数据集', 'NDCG@10 0.0611', 'HR@20 0.1373'] },
  { name: 'BERT4Rec', year: '推荐系统', type: 'BERT4Rec', color: '#0ea5e9', desc: '双向序列推荐模型，包含 Beauty、ML-1M、Steam 和 ML-20M 数据资产。', params: '4,842,458', scaleLabel: '交互数', top1Acc: '无日志', metricLabel: '指标状态', task: '代码+数据', taskLabel: '状态', tags: ['Hidden 64', '2 Blocks', 'ML-20M 归档'] },
  { name: 'DuoRec', year: '推荐系统', type: 'DuoRec', color: '#7dd3fc', desc: '对比序列推荐模型，已接入 ml-100k atomic 数据。', params: '100,000', scaleLabel: '交互数', top1Acc: '无日志', metricLabel: '指标状态', task: '代码+数据', taskLabel: '状态', tags: ['ml-100k', 'Contrast us_x', 'Batch 256'] },
  { name: 'FEARec', year: '推荐系统', type: 'FEARec', color: '#a78bfa', desc: '频域增强序列推荐模型，使用 ml-100k 数据和 seq.yaml 配置。', params: '100,000', scaleLabel: '交互数', top1Acc: '无日志', metricLabel: '指标状态', task: '代码+数据', taskLabel: '状态', tags: ['ml-100k', 'Epochs 1000', '频域增强'] },
  { name: 'FMLP-Rec', year: '推荐系统', type: 'FMLP-Rec', color: '#ec4899', desc: '滤波增强推荐模型，Beauty 数据、权重和测试指标日志已存在。', params: '198,502', scaleLabel: '交互数', top1Acc: 'HR@10 0.4935', metricLabel: '可用指标', task: '权重+日志', taskLabel: '状态', tags: ['NDCG@10 0.3317', 'MRR 0.2985', 'Beauty'] },
  { name: 'RecBole', year: '推荐系统', type: 'RecBole', color: '#f97316', desc: '推荐实验框架，包含 ml-100k atomic 数据和配置驱动评估体系。', params: '100,000', scaleLabel: '交互数', top1Acc: '无日志', metricLabel: '指标状态', task: '代码+数据', taskLabel: '状态', tags: ['框架', '.inter/.item/.user', 'ml-100k'] },
  { name: 'SASRec', year: '推荐系统', type: 'SASRec', color: '#06b6d4', desc: '自注意力序列推荐模型，包含 Beauty、ML-1M、Steam、Video 四份数据。', params: '5,367,798', scaleLabel: '交互数', top1Acc: '无日志', metricLabel: '指标状态', task: '代码+数据', taskLabel: '状态', tags: ['4 数据集', 'Hidden 50', 'Self-Attention'] },
  { name: 'TiSASRec', year: '推荐系统', type: 'TiSASRec', color: '#14b8a6', desc: '时间间隔序列推荐模型，包含 ML-1M 用户、物品、评分和时间戳数据。', params: '1,000,209', scaleLabel: '交互数', top1Acc: '无日志', metricLabel: '指标状态', task: '代码+数据', taskLabel: '状态', tags: ['Time span 256', 'ML-1M', '时间间隔'] },
] : [
  { name: 'BSARec Job', year: 'Recommender', type: 'BSARec', color: '#22c55e', desc: 'Job-sequence recommender with weights, Job data, and proxy config present, while the service is currently offline.', params: '409,466', scaleLabel: 'interactions', top1Acc: 'HR@10 0.0287', metricLabel: 'real metric', task: 'service offline', taskLabel: 'status', tags: ['Job data', 'NDCG@10 0.0132', 'backend proxy'] },
  { name: 'BSARec', year: 'Recommender', type: 'BSARec', color: '#16a34a', desc: 'Multi-dataset sequential recommendation asset. Beauty and LastFM include weights and evaluation logs.', params: '2,030,952', scaleLabel: 'interactions', top1Acc: 'HR@10 0.1008', metricLabel: 'real metric', task: 'weights+logs', taskLabel: 'status', tags: ['6 datasets', 'NDCG@10 0.0611', 'HR@20 0.1373'] },
  { name: 'BERT4Rec', year: 'Recommender', type: 'BERT4Rec', color: '#0ea5e9', desc: 'Bidirectional sequential recommendation model with Beauty, ML-1M, Steam, and ML-20M data assets.', params: '4,842,458', scaleLabel: 'interactions', top1Acc: 'no logs', metricLabel: 'metric state', task: 'code+data', taskLabel: 'status', tags: ['Hidden 64', '2 Blocks', 'ML-20M archive'] },
  { name: 'DuoRec', year: 'Recommender', type: 'DuoRec', color: '#7dd3fc', desc: 'Contrastive sequential recommender with ml-100k atomic data in local assets.', params: '100,000', scaleLabel: 'interactions', top1Acc: 'no logs', metricLabel: 'metric state', task: 'code+data', taskLabel: 'status', tags: ['ml-100k', 'Contrast us_x', 'Batch 256'] },
  { name: 'FEARec', year: 'Recommender', type: 'FEARec', color: '#a78bfa', desc: 'Frequency-enhanced sequential recommender using real ml-100k data and seq.yaml config.', params: '100,000', scaleLabel: 'interactions', top1Acc: 'no logs', metricLabel: 'metric state', task: 'code+data', taskLabel: 'status', tags: ['ml-100k', 'Epochs 1000', 'frequency enhanced'] },
  { name: 'FMLP-Rec', year: 'Recommender', type: 'FMLP-Rec', color: '#ec4899', desc: 'Filtering-enhanced recommender with Beauty data, weights, and test metric logs.', params: '198,502', scaleLabel: 'interactions', top1Acc: 'HR@10 0.4935', metricLabel: 'real metric', task: 'weights+logs', taskLabel: 'status', tags: ['NDCG@10 0.3317', 'MRR 0.2985', 'Beauty'] },
  { name: 'RecBole', year: 'Recommender', type: 'RecBole', color: '#f97316', desc: 'Recommendation experiment framework with ml-100k atomic data and config-driven evaluation.', params: '100,000', scaleLabel: 'interactions', top1Acc: 'no logs', metricLabel: 'metric state', task: 'code+data', taskLabel: 'status', tags: ['framework', '.inter/.item/.user', 'ml-100k'] },
  { name: 'SASRec', year: 'Recommender', type: 'SASRec', color: '#06b6d4', desc: 'Self-attention sequential recommender with Beauty, ML-1M, Steam, and Video datasets.', params: '5,367,798', scaleLabel: 'interactions', top1Acc: 'no logs', metricLabel: 'metric state', task: 'code+data', taskLabel: 'status', tags: ['4 datasets', 'Hidden 50', 'Self-Attention'] },
  { name: 'TiSASRec', year: 'Recommender', type: 'TiSASRec', color: '#14b8a6', desc: 'Time-interval sequential recommender with ML-1M users, items, ratings, and timestamps.', params: '1,000,209', scaleLabel: 'interactions', top1Acc: 'no logs', metricLabel: 'metric state', task: 'code+data', taskLabel: 'status', tags: ['Time span 256', 'ML-1M', 'time interval'] },
]);

/* ========== 曲线教程数据 ========== */
const curveGuides = computed(() => isZh.value ? [
  {
    id: 'rec-metrics', badge: '推荐指标', color: '#22c55e',
    title: '有日志模型 HR/NDCG 对比',
    annotations: [
      { label: 'FMLP-Rec', color: '#ec4899', desc: 'HR@10 0.4935，NDCG@10 0.3317' },
      { label: 'BSARec', color: '#22c55e', desc: 'HR@10 0.1008，NDCG@10 0.0611' },
    ],
  },
  {
    id: 'data-scale', badge: '数据规模', color: '#38bdf8',
    title: '主要数据资产交互数',
    annotations: [
      { label: 'SASRec', color: '#38bdf8', desc: '5,367,798 交互，数据规模最大' },
      { label: 'Job', color: '#f59e0b', desc: '409,466 交互，服务代理模型专用数据' },
    ],
  },
  {
    id: 'readiness-profile', badge: '接入状态', color: '#a78bfa',
    title: '数据、权重、日志、服务状态',
    annotations: [
      { label: 'BSARec Job', color: '#f59e0b', desc: '数据、权重、日志存在；服务当前离线' },
      { label: 'BERT4Rec', color: '#64748b', desc: '代码和数据存在，权重与日志缺失' },
    ],
  },
  {
    id: 'dataset-quality', badge: '数据中心', color: '#42e6a4',
    title: '推荐数据检查重点',
    annotations: [
      { label: '结构', color: '#42e6a4', desc: '先看用户、物品、时间和序列字段' },
      { label: '评估准备', color: '#60a5fa', desc: '再决定是否补训练或评估日志' },
    ],
  },
] : [
  {
    id: 'rec-metrics', badge: 'Ranking metrics', color: '#22c55e',
    title: 'HR/NDCG comparison for logged models',
    annotations: [
      { label: 'FMLP-Rec', color: '#ec4899', desc: 'HR@10 0.4935, NDCG@10 0.3317' },
      { label: 'BSARec', color: '#22c55e', desc: 'HR@10 0.1008, NDCG@10 0.0611' },
    ],
  },
  {
    id: 'data-scale', badge: 'Data scale', color: '#38bdf8',
    title: 'Interaction counts for major data assets',
    annotations: [
      { label: 'SASRec', color: '#38bdf8', desc: '5,367,798 interactions, largest local asset scale' },
      { label: 'Job', color: '#f59e0b', desc: '409,466 interactions, dedicated to the proxied job recommender' },
    ],
  },
  {
    id: 'readiness-profile', badge: 'Readiness', color: '#a78bfa',
    title: 'Data, weights, logs, and service readiness',
    annotations: [
      { label: 'BSARec Job', color: '#f59e0b', desc: 'Data, weights, and logs exist; service is offline' },
      { label: 'BERT4Rec', color: '#64748b', desc: 'Code and data exist; weights and logs are missing' },
    ],
  },
  {
    id: 'dataset-quality', badge: 'Data Center', color: '#42e6a4',
    title: 'Recommendation data checks',
    annotations: [
      { label: 'Structure', color: '#42e6a4', desc: 'Inspect user, item, time, and sequence fields first' },
      { label: 'Evaluation readiness', color: '#60a5fa', desc: 'Then decide whether training or evaluation logs need to be added' },
    ],
  },
]);

const scrollToTop = () => window.scrollTo({ top: 0, behavior: 'smooth' });
const scrollToTutorials = () => document.getElementById('tutorials')?.scrollIntoView({ behavior: 'smooth' });

// 平台文章
const showArticleIndex = ref(false);
const officialArticles = ref<any[]>([]);
const readerOpen = ref(false);
const activeArticle = ref<any>(null);
const articleLoading = ref(false);
const articleIndexRestoreKey = 'deepinsight:knowledge:articleIndex';

const openArticleIndex = () => {
  showArticleIndex.value = true;
};

const closeArticleIndex = () => {
  showArticleIndex.value = false;
  if (typeof window === 'undefined' || !shouldRestoreArticleIndex(route.query.articleIndex)) return;

  const url = new URL(window.location.href);
  url.searchParams.delete('articleIndex');
  window.history.replaceState(window.history.state, '', `${url.pathname}${url.search}${url.hash}`);
};

const shouldRestoreArticleIndex = (value: unknown) => {
  const raw = Array.isArray(value) ? value[0] : value;
  return raw === 'open' || raw === '1' || raw === 'true';
};

const consumeArticleIndexRestoreFlag = () => {
  if (typeof window === 'undefined') return false;
  const shouldRestore = window.sessionStorage.getItem(articleIndexRestoreKey) === 'open';
  if (shouldRestore) window.sessionStorage.removeItem(articleIndexRestoreKey);
  return shouldRestore;
};

const restoreArticleIndexFromRoute = () => {
  const fromRoute = shouldRestoreArticleIndex(route.query.articleIndex);
  const fromSession = consumeArticleIndexRestoreFlag();
  if (!fromRoute && !fromSession) return;

  openArticleIndex();
};

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
  restoreArticleIndexFromRoute();
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

watch([() => themeStore.isDarkMode, isZh], () => {
  Object.values(curveRefs.value).forEach(el => {
    const instance = echarts.getInstanceByDom(el);
    if (instance) instance.dispose();
  });
  nextTick(() => initCurveCharts());
});

watch(readerOpen, (open) => {
  document.documentElement.classList.toggle('knowledge-overlay-open', open);
});

watch(() => route.query.articleIndex, () => {
  restoreArticleIndexFromRoute();
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
    radial-gradient(820px circle at 18% 12%, rgba(var(--primary-rgb), 0.085), transparent 50%),
    radial-gradient(980px circle at 82% 16%, color-mix(in srgb, var(--accent-glow) 10%, transparent), transparent 54%),
    linear-gradient(180deg, #fbfdff 0%, #f1f8f8 55%, #f8fafc 100%);
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
  top: clamp(20px, 3.5vw, 40px);
  left: clamp(18px, 4.2vw, 52px);
  z-index: 20;
  width: min(410px, calc(100vw - 36px));
  padding: clamp(13px, 1.45vw, 17px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 22px;
  background:
    linear-gradient(135deg, rgba(var(--primary-rgb), 0.075), transparent 56%),
    rgba(var(--glass-bg-rgb), 0.42);
  box-shadow: 0 24px 70px rgba(0, 0, 0, 0.22);
  backdrop-filter: blur(22px) saturate(150%);
}
:global(.light .kg-hero-title) {
  top: clamp(22px, 4vw, 42px);
  left: clamp(18px, 3.6vw, 48px);
  width: min(360px, calc(100vw - 36px));
  padding: 16px;
  border-radius: 20px;
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.74), rgba(255, 255, 255, 0.42) 58%, rgba(235, 243, 255, 0.28)),
    radial-gradient(360px circle at 0% 0%, rgba(var(--primary-rgb), 0.08), transparent 58%);
  border-color: rgba(var(--primary-rgb), 0.14);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.7),
    0 18px 46px rgba(68, 84, 118, 0.1);
}
.hero-badge {
  display: inline-block;
  padding: 4px 11px;
  border-radius: 999px;
  border: 1px solid rgba(var(--primary-rgb),0.34);
  background: rgba(var(--primary-rgb),0.1);
  font-size: 8px;
  font-weight: var(--font-weight-title);
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--primary-color);
  margin-bottom: 9px;
  backdrop-filter: blur(8px);
}
.kg-hero-title h1 {
  font-size: clamp(28px, 3.55vw, 43px);
  font-weight: var(--font-weight-title);
  letter-spacing: 0;
  color: var(--text-primary);
  margin: 0 0 9px;
  line-height: 1.02;
}
:global(.light .kg-hero-title h1) {
  color: var(--text-primary);
  font-size: clamp(28px, 3vw, 40px);
  line-height: 1.04;
  margin-bottom: 10px;
}
:global(.light .kg-hero-title h1 em) {
  color: var(--primary-color);
  text-shadow: 0 16px 38px rgba(var(--primary-rgb), 0.18);
}
.kg-hero-title h1 em {
  display: block;
  color: var(--primary-color);
  font-style: normal;
}
.kg-hero-title p {
  max-width: 48ch;
  font-size: 11.5px;
  line-height: 1.58;
  color: var(--text-secondary);
  margin: 0 0 12px;
}
:global(.light .kg-hero-title p) {
  max-width: 40ch;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.58;
  margin-bottom: 13px;
}
.hero-actions { display:flex; flex-wrap:wrap; gap:10px; }
.hero-actions button,
.article-index-button {
  height: 32px;
  padding: 0 12px;
  border: 1px solid rgba(var(--primary-rgb),0.28);
  border-radius: 999px;
  background: rgba(var(--primary-rgb),0.16);
  color: color-mix(in srgb, var(--primary-color) 62%, white);
  font-size: 11px;
  font-weight: var(--font-weight-label);
  cursor: pointer;
  transition: transform 180ms ease, border-color 180ms ease, background 180ms ease;
}
.article-index-button {
  color: #08120f;
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--primary-color) 96%, white 4%), color-mix(in srgb, var(--primary-color) 82%, black 18%));
  box-shadow: 0 14px 30px rgba(var(--primary-rgb), 0.18);
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
:global(.light .hero-actions button) {
  height: 34px;
  padding-inline: 13px;
  color: color-mix(in srgb, var(--primary-color) 72%, #1f2937);
  border-color: rgba(var(--primary-rgb), 0.22);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.72), color-mix(in srgb, var(--primary-color) 11%, rgba(255,255,255,0.62))),
    rgba(var(--primary-rgb), 0.08);
  box-shadow: 0 12px 28px rgba(var(--primary-rgb), 0.12);
}
:global(.light .hero-actions button.ghost) {
  color: rgba(42, 50, 66, 0.72);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.74), rgba(245, 247, 252, 0.62));
  border-color: rgba(86, 100, 132, 0.12);
}
:global(.light .article-index-button) {
  color: color-mix(in srgb, var(--primary-color) 74%, #1f2937);
  border-color: rgba(var(--primary-rgb), 0.22);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.82), color-mix(in srgb, var(--primary-color) 12%, rgba(255,255,255,0.72)));
  box-shadow: 0 14px 34px rgba(var(--primary-rgb), 0.13);
}

.kg-system-panel {
  position: absolute;
  right: 24px;
  top: 40px;
  z-index: 20;
  width: 132px;
  padding: 11px 12px;
  border: 1px solid rgba(var(--primary-rgb), 0.14);
  border-radius: 16px;
  background:
    linear-gradient(135deg, rgba(var(--primary-rgb), 0.08), transparent 60%),
    rgba(var(--glass-bg-rgb), 0.5);
  backdrop-filter: blur(16px);
}
:global(.light .kg-system-panel) {
  right: clamp(18px, 3vw, 42px);
  top: clamp(22px, 4vw, 42px);
  width: 126px;
  padding: 11px 12px;
  border-radius: 15px;
  background:
    linear-gradient(150deg, rgba(255,255,255,0.68), rgba(246, 249, 255, 0.46)),
    rgba(247, 250, 255, 0.44);
  border-color: rgba(var(--primary-rgb), 0.12);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.68),
    0 14px 38px rgba(68, 84, 118, 0.08);
}
.kg-system-panel span,
.kg-system-panel em { display:block; color:var(--text-muted); font-size:9px; font-weight:850; text-transform:uppercase; font-style:normal; letter-spacing: 0.04em; }
:global(.light .kg-system-panel span),
:global(.light .kg-system-panel em) { color:var(--text-muted); }
.kg-system-panel strong { display:block; margin:3px 0; color:var(--primary-color); font-size:25px; font-weight:950; }
:global(.light .kg-system-panel strong) {
  margin: 3px 0;
  font-size: 24px;
}

/* 右上简化图例 */
.kg-legend-simple {
  position: absolute;
  top: 136px;
  right: 24px;
  z-index: 20;
  display: flex;
  flex-direction: column;
  gap: 4px;
  background: rgba(var(--glass-bg-rgb), 0.42);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(var(--primary-rgb), 0.12);
  border-radius: 14px;
  padding: 10px 12px;
}
:global(.light .kg-legend-simple) {
  top: auto;
  right: clamp(18px, 3vw, 42px);
  bottom: 26px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, max-content));
  gap: 6px 12px;
  padding: 10px 12px;
  border-radius: 18px;
  background:
    linear-gradient(150deg, rgba(255,255,255,0.62), rgba(246, 249, 255, 0.42)),
    rgba(247, 250, 255, 0.38);
  color: rgba(42, 50, 66, 0.72);
  border-color: rgba(var(--primary-rgb), 0.12);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.62),
    0 14px 38px rgba(68, 84, 118, 0.08);
}
.leg-row {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 9px;
  font-weight: var(--font-weight-body);
  color: var(--text-secondary);
}
:global(.light .leg-row) { color: var(--text-secondary); }
.leg-dot {
  width: 7px;
  height: 7px;
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
:global(.light .knowledge-page) {
  color: #263241;
}
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
  font-weight: var(--font-weight-title);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}
:global(.light .section-kicker) {
  color: color-mix(in srgb, var(--primary-color) 74%, #1f2937);
  border-color: rgba(var(--primary-rgb), 0.2);
  background: rgba(var(--primary-rgb), 0.075);
}
.section-intro h2 {
  font-size: clamp(24px, 3vw, 34px);
  font-weight: var(--font-weight-title);
  color: var(--text-primary);
  margin: 0 0 10px;
}
:global(.light .section-intro h2) { color: #263241; }
.section-intro p {
  align-self: end;
  max-width: 420px;
  justify-self: end;
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.7;
  margin: 0;
}
:global(.light .section-intro p) { color: rgba(52, 64, 84, 0.7); }

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
:global(.light .tutorial-card),
:global(.light .curve-card),
:global(.light .model-showcase-card),
:global(.light .kb-cta) {
  border-color: rgba(var(--primary-rgb), 0.14);
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.82), rgba(247, 249, 255, 0.62)),
    rgba(255, 255, 255, 0.5);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.78),
    0 20px 54px rgba(68, 84, 118, 0.11);
}
:global(.light .tutorial-card.expanded) {
  border-color: rgba(var(--primary-rgb), 0.22);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.82),
    0 22px 58px rgba(var(--primary-rgb), 0.13);
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
  font-weight: var(--font-weight-body);
  margin: 0 0 2px;
}
.tut-duration {
  font-size: 10px;
  color: var(--text-secondary);
  font-weight: 600;
  text-transform: uppercase;
}
:global(.light .tut-duration),
:global(.light .tut-desc),
:global(.light .tut-detail :deep(p)),
:global(.light .tut-detail :deep(li)) {
  color: rgba(52, 64, 84, 0.68);
}
:global(.light .tut-info h3),
:global(.light .tut-detail),
:global(.light .tut-detail :deep(strong)) {
  color: #263241;
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
.tut-detail :deep(h4) { font-size: 13px; font-weight: var(--font-weight-body); margin: 16px 0 8px; color: var(--text-primary); border-left: 3px solid var(--primary-color); padding-left: 10px; }
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
  font-weight: var(--font-weight-title);
  text-transform: uppercase;
  padding: 2px 10px;
  border-radius: 20px;
  white-space: nowrap;
}
.curve-title {
  font-size: 13px;
  font-weight: var(--font-weight-body);
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
:global(.light .curve-title),
:global(.light .model-card-name),
:global(.light .kb-cta h2) {
  color: #263241;
}
:global(.light .ann-text),
:global(.light .model-card-desc),
:global(.light .stat-label),
:global(.light .model-tag),
:global(.light .kb-cta p) {
  color: rgba(52, 64, 84, 0.68);
}

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
  font-weight: var(--font-weight-body);
  color: var(--text-secondary);
  background: var(--bg-input);
  padding: 2px 10px;
  border-radius: 20px;
}
.model-type {
  font-size: 10px;
  font-weight: var(--font-weight-body);
  text-transform: uppercase;
}
.model-card-name {
  font-size: 18px;
  font-weight: var(--font-weight-title);
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
.stat-value { font-size: 16px; font-weight: var(--font-weight-title); display: block; }
.stat-label { font-size: 9px; color: var(--text-secondary); text-transform: uppercase; }
.model-card-tags { display: flex; flex-wrap: wrap; gap: 4px; }
.model-tag {
  font-size: 9px;
  font-weight: var(--font-weight-body);
  padding: 2px 8px;
  border-radius: 8px;
  background: var(--bg-input);
  color: var(--text-secondary);
}
:global(.light .model-year),
:global(.light .model-tag) {
  background: rgba(246, 249, 255, 0.78);
  border: 1px solid rgba(86, 100, 132, 0.1);
}
:global(.light .model-stat) {
  border-color: rgba(var(--primary-rgb), 0.12);
  background: rgba(255, 255, 255, 0.5);
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
  font-weight: var(--font-weight-title);
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
.kb-action-row {
  position: relative;
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 12px;
}
.kb-cta-button {
  height: 48px;
  min-width: 148px;
  padding: 0 26px;
  border-radius: 16px;
  border: 1px solid rgba(var(--primary-rgb), 0.22);
  font-size: 14px;
  font-weight: var(--font-weight-label);
  transition: transform 180ms ease, box-shadow 180ms ease, border-color 180ms ease, background 180ms ease;
}
.kb-cta-button:hover {
  transform: translateY(-1px);
}
.kb-cta-button-primary {
  color: #07110f;
  border-color: transparent;
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--primary-color) 98%, white 2%), color-mix(in srgb, var(--primary-color) 76%, black 24%));
  box-shadow: 0 18px 44px rgba(var(--primary-rgb), 0.24);
}
.kb-cta-button-secondary {
  color: var(--text-primary);
  border-color: rgba(var(--primary-rgb), 0.18);
  background: rgba(var(--glass-bg-rgb), 0.46);
}
.kb-cta-button-secondary:hover {
  border-color: rgba(var(--primary-rgb), 0.32);
  background: rgba(var(--primary-rgb), 0.1);
}
:global(.light .kb-cta-button-primary) {
  color: color-mix(in srgb, var(--primary-color) 74%, #1f2937);
  border-color: rgba(var(--primary-rgb), 0.2);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.86), color-mix(in srgb, var(--primary-color) 13%, rgba(255,255,255,0.76)));
  box-shadow: 0 18px 44px rgba(var(--primary-rgb), 0.14);
}
:global(.light .kb-cta-button-secondary) {
  color: #344054;
  border-color: rgba(86, 100, 132, 0.16);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.76), rgba(245, 248, 252, 0.68));
}
:global(.light .kb-cta-button-secondary:hover) {
  color: #263241;
  border-color: rgba(var(--primary-rgb), 0.24);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.86), color-mix(in srgb, var(--primary-color) 10%, rgba(255,255,255,0.62)));
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
  right: clamp(18px, 5vw, 72px);
  top: clamp(72px, 8vh, 92px);
  bottom: clamp(18px, 4vh, 34px);
  z-index: 3300;
  display: flex;
  flex-direction: column;
  width: min(440px, 90vw);
  max-height: calc(100dvh - 112px);
  padding: 14px;
  border: 1px solid rgba(var(--primary-rgb),0.2);
  border-radius: 22px;
  background:
    radial-gradient(circle at 18% 0%, rgba(var(--primary-rgb), 0.16), transparent 34%),
    linear-gradient(180deg, rgba(20, 24, 34, 0.94), rgba(12, 16, 24, 0.92));
  box-shadow: 0 28px 86px rgba(0,0,0,0.34), 0 0 0 1px rgba(255,255,255,0.025) inset;
  backdrop-filter: blur(26px) saturate(155%);
  -webkit-backdrop-filter: blur(26px) saturate(155%);
  overflow: hidden;
}
.article-index-panel--reader-open {
  right: calc(min(620px, 92vw) + 28px);
  width: min(400px, calc(100vw - min(620px, 92vw) - 64px));
}
@media (max-width: 1220px) {
  .article-index-panel--reader-open {
    right: auto;
    left: clamp(14px, 3vw, 24px);
    width: min(360px, calc(100vw - 660px));
  }
}
@media (max-width: 1020px) {
  .article-index-panel--reader-open {
    display: none;
  }
}
.article-index-head { flex: 0 0 auto; display:flex; justify-content:space-between; align-items:flex-start; gap: 12px; margin-bottom:10px; padding: 6px 4px 13px; border-bottom: 1px solid var(--border-color); }
.article-index-head span { display:block; color:var(--text-primary); font-size:13px; font-weight:900; text-transform:uppercase; letter-spacing:.08em; }
.article-index-head small { display:block; margin-top:4px; color:var(--text-muted); font-size:11px; line-height:1.45; }
.article-index-head button { flex: 0 0 auto; width:30px; height:30px; border-radius:999px; border:1px solid var(--border-color); background:rgba(var(--glass-bg-rgb), 0.42); color:var(--text-secondary); cursor:pointer; font-size:18px; line-height: 1; transition: transform .18s ease, color .18s ease, background .18s ease; }
.article-index-head button:hover { transform: scale(1.04); color: var(--text-primary); background: rgba(var(--primary-rgb), 0.12); }
.article-index-panel .articles-dialog-list {
  min-height: 0;
  flex: 1 1 auto;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding: 2px 4px 4px 0;
  scrollbar-width: thin;
  scrollbar-color: rgba(var(--primary-rgb), 0.42) transparent;
}
.article-index-panel .articles-dialog-list::-webkit-scrollbar { width: 8px; }
.article-index-panel .articles-dialog-list::-webkit-scrollbar-track { background: transparent; }
.article-index-panel .articles-dialog-list::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: rgba(var(--primary-rgb), 0.34);
  border: 2px solid transparent;
  background-clip: content-box;
}
.article-index-enter-active,
.article-index-leave-active { transition: opacity .22s ease, transform .22s ease; }
.article-index-enter-from,
.article-index-leave-to { opacity:0; transform: translateX(30px); }
.article-dialog-item h4 { font-size: 14px; font-weight: var(--font-weight-body); color: var(--text-primary); margin: 0 0 2px; }
.article-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; margin-top: 6px; box-shadow: 0 0 12px rgba(var(--primary-rgb), 0.55); }
.article-meta-text { font-size: 10px; color: var(--text-secondary); }
.article-index-empty { padding: 30px 12px; color: var(--text-muted); font-size: 12px; text-align: center; }
:global(.light .article-index-panel) {
  border-color: rgba(var(--primary-rgb), 0.18);
  background:
    radial-gradient(circle at 18% 0%, rgba(var(--primary-rgb), 0.13), transparent 34%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(247, 249, 255, 0.93));
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.86),
    0 28px 86px rgba(68, 84, 118, 0.2);
}
:global(.light .article-index-head) { border-bottom-color: rgba(86, 100, 132, 0.14); }
:global(.light .article-index-head span) { color: #263241; }
:global(.light .article-index-head small) { color: rgba(52, 64, 84, 0.62); }
:global(.light .article-index-head button) {
  color: #5f6b7a;
  border-color: rgba(86, 100, 132, 0.14);
  background: rgba(255, 255, 255, 0.68);
}
:global(.light .article-index-head button:hover) {
  color: color-mix(in srgb, var(--primary-color) 74%, #1f2937);
  background: rgba(var(--primary-rgb), 0.1);
}
:global(.light .article-dialog-item:hover) {
  border-color: rgba(var(--primary-rgb), 0.18);
  background: rgba(var(--primary-rgb), 0.075);
}
:global(.light .article-dialog-item h4) { color: #263241; }
:global(.light .article-meta-text) { color: rgba(52, 64, 84, 0.62); }
:global(.light .article-index-empty) { color: rgba(52, 64, 84, 0.58); }
.article-detail-content { font-size: 14px; line-height: 1.9; color: var(--text-primary); }
.article-detail-content :deep(h1), .article-detail-content :deep(h2), .article-detail-content :deep(h3) { margin: 16px 0 8px; }
.article-detail-content :deep(pre) { background: var(--bg-input); padding: 16px; border-radius: 12px; overflow-x: auto; }
.article-detail-content :deep(code) { background: var(--bg-input); padding: 2px 6px; border-radius: 4px; font-size: 13px; }
.article-detail-content :deep(li) { margin-left: 16px; }
.mt-6 { margin-top: 24px; }
.text-center { text-align: center; }

/* knowledge-liquid-system-pass:start */
.tutorial-card,
.curve-card,
.model-showcase-card,
.kg-hero-title,
.kg-system-panel,
.kg-legend-simple {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.07), rgba(255, 255, 255, 0.018)),
    color-mix(in srgb, rgba(var(--glass-bg-rgb), var(--glass-opacity)) 78%, transparent);
  border-color: color-mix(in srgb, var(--primary-color) 20%, var(--border-color));
  box-shadow: var(--shadow-soft);
  backdrop-filter: blur(var(--glass-blur)) saturate(var(--glass-saturate));
  -webkit-backdrop-filter: blur(var(--glass-blur)) saturate(var(--glass-saturate));
}

@media (hover: hover) and (pointer: fine) {
  .tutorial-card:hover,
  .curve-card:hover,
  .model-showcase-card:hover {
    transform: translate3d(0, -1px, 0);
    border-color: color-mix(in srgb, var(--primary-color) 34%, var(--border-color));
  }
}

@media (prefers-reduced-motion: reduce) {
  .hero-actions button,
  .article-index-button,
  .tutorial-card,
  .curve-card,
  .model-showcase-card,
  .tut-arrow,
  .tut-content,
  .article-dialog-item,
  .article-index-enter-active,
  .article-index-leave-active {
    transition: none !important;
    transform: none !important;
  }
}
/* knowledge-liquid-system-pass:end */

:global(.light .tutorial-card),
:global(.light .curve-card),
:global(.light .model-showcase-card),
:global(.light .kb-cta) {
  border-color: rgba(var(--primary-rgb), 0.14);
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.84), rgba(247, 249, 255, 0.62)),
    rgba(255, 255, 255, 0.5);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.78),
    0 20px 54px rgba(68, 84, 118, 0.11);
}

:global(.light .article-index-panel) {
  border-color: rgba(var(--primary-rgb), 0.18);
  background:
    radial-gradient(circle at 18% 0%, rgba(var(--primary-rgb), 0.13), transparent 34%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(247, 249, 255, 0.93));
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.86),
    0 28px 86px rgba(68, 84, 118, 0.2);
}

.article-index-button,
.kb-cta-button-primary,
.kb-cta-button-secondary {
  --el-button-hover-text-color: currentColor;
  --el-button-active-text-color: currentColor;
}
.article-index-button :deep(span),
.kb-cta-button :deep(span) {
  color: inherit;
}
.article-index-button,
.kb-cta-button-primary {
  color: #07110f !important;
  border-color: transparent !important;
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--primary-color) 98%, white 2%), color-mix(in srgb, var(--primary-color) 76%, black 24%)) !important;
}
.kb-cta-button-secondary {
  color: var(--text-primary) !important;
  border-color: rgba(var(--primary-rgb), 0.18) !important;
  background: rgba(var(--glass-bg-rgb), 0.46) !important;
}
:global(.light .knowledge-page .article-index-button),
:global(.light .knowledge-page .kb-cta-button-primary) {
  color: color-mix(in srgb, var(--primary-color) 74%, #1f2937) !important;
  border-color: rgba(var(--primary-rgb), 0.2) !important;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.88), color-mix(in srgb, var(--primary-color) 13%, rgba(255,255,255,0.76))) !important;
}
:global(.light .knowledge-page .kb-cta-button-secondary) {
  color: #344054 !important;
  border-color: rgba(86, 100, 132, 0.16) !important;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.78), rgba(245, 248, 252, 0.68)) !important;
}

@media (max-width: 980px) {
  .kg-hero {
    height: auto;
    min-height: 760px;
  }
  .kg-hero-title {
    top: 22px;
  }
  :global(.light .kg-hero-title) {
    width: min(360px, calc(100vw - 36px));
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
  :global(.light .kg-system-panel) {
    top: 22px;
    right: 18px;
    width: 112px;
  }
  :global(.light .kg-legend-simple) {
    right: 18px;
    left: 18px;
    bottom: 22px;
    grid-template-columns: repeat(2, minmax(0, 1fr));
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
  :global(.light .kg-hero-title) {
    right: 18px;
    width: auto;
  }
  :global(.light .kg-hero-title h1) {
    font-size: 27px;
  }
  .kg-system-panel {
    left: 18px;
    right: auto;
    top: 250px;
    bottom: auto;
  }
  :global(.light .kg-system-panel) {
    top: auto;
    left: 18px;
    right: auto;
    bottom: 150px;
  }
  :global(.light .kg-legend-simple) {
    bottom: 18px;
    grid-template-columns: 1fr;
  }
  .model-card-stats {
    grid-template-columns: 1fr;
  }
}
</style>
