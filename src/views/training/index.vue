<template>
  <div class="training-page">
    <div class="page-header entrance-hero">
      <div>
        <span class="hierarchy-eyebrow">{{ copy.eyebrow }}</span>
        <h2>{{ $t('nav.training') }}</h2>
        <p>{{ $t('training.subtitle') }}</p>
      </div>
      <div v-if="isLoggedIn" class="header-count">
        <strong>{{ modelCatalog.length }}</strong>
        <span>{{ copy.modelCount }}</span>
      </div>
    </div>

    <el-card v-if="!isLoggedIn" shadow="never" class="login-hint">
      <div class="hint-content">
        <AlertCircle :size="40" />
        <h3>{{ copy.loginRequired }}</h3>
        <p>{{ copy.loginRequiredDesc }}</p>
        <el-button type="primary" @click="$router.push('/login')" :style="{ backgroundColor: 'var(--primary-color)' }">
          {{ copy.goLogin }}
        </el-button>
      </div>
    </el-card>

    <section v-else class="model-overview-shell entrance-up">
      <aside class="model-directory" :aria-label="copy.modelDirectory">
        <div class="directory-head">
          <span>{{ copy.modelDirectory }}</span>
          <strong>{{ copy.clickHint }}</strong>
        </div>

        <div class="directory-list">
          <button
            v-for="model in modelCatalog"
            :key="model.id"
            type="button"
            class="model-entry"
            :class="{ active: selectedModelId === model.id }"
            :style="accentStyle(model)"
            :aria-pressed="selectedModelId === model.id"
            @click="selectedModelId = model.id"
          >
            <span class="entry-glyph" :class="`visual-${model.visual}`" aria-hidden="true">
              <component :is="model.icon" :size="24" />
            </span>
            <span class="entry-copy">
              <strong>{{ displayName(model) }}</strong>
              <small>{{ model.slug }}</small>
              <em>{{ displayTask(model) }}</em>
            </span>
            <span class="entry-status" :class="`status-${model.evidenceLevel}`">
              {{ selectedModelId === model.id ? copy.viewing : model.status }}
            </span>
          </button>
        </div>
      </aside>

      <main v-if="selectedModel" class="model-detail" :style="accentStyle(selectedModel)">
        <section class="model-evaluation-hero">
          <div class="model-identity">
            <div class="model-mark" :class="`visual-${selectedModel.visual}`" aria-hidden="true">
              <component :is="selectedModel.icon" :size="34" />
              <span>{{ selectedModel.mark }}</span>
            </div>
            <div>
              <span class="model-kicker">{{ selectedModel.source }}</span>
              <h3>{{ displayName(selectedModel) }}</h3>
              <p>{{ displayDescription(selectedModel) }}</p>
              <div class="hero-badges">
                <span>{{ selectedModel.status }}</span>
                <span>{{ selectedModel.integration }}</span>
                <span>{{ selectedModel.metricStatus }}</span>
              </div>
            </div>
          </div>

          <div class="model-facts">
            <article v-for="fact in selectedModel.profile" :key="fact.label">
              <span>{{ fact.label }}</span>
              <strong>{{ fact.value }}</strong>
              <small>{{ fact.basis }}</small>
            </article>
          </div>
        </section>

        <section class="metric-band">
          <article
            v-for="metric in selectedModel.metrics"
            :key="metric.label"
            class="metric-tile"
            :class="{ muted: !metric.available }"
          >
            <div>
              <span>{{ metric.label }}</span>
              <strong>{{ metric.value }}</strong>
            </div>
            <div v-if="metric.available" class="metric-bar" :aria-label="metric.label">
              <i :style="{ width: metric.percent + '%' }"></i>
            </div>
            <p>{{ metric.explain }}</p>
            <small>{{ metric.basis }}</small>
          </article>
        </section>

        <section class="overview-workbench">
          <div class="parameter-panel">
            <div class="section-heading">
              <span>{{ copy.parameterMethods }}</span>
              <strong>{{ copy.parameterMethodsTitle }}</strong>
            </div>

            <div class="method-list">
              <article v-for="param in selectedModel.parameterMethods" :key="param.name">
                <header>
                  <code>{{ param.name }}</code>
                  <strong>{{ param.currentValue }}</strong>
                </header>
                <p>{{ param.changeMethod }}</p>
                <div class="method-meta">
                  <span>{{ param.whenToAdjust }}</span>
                  <small>{{ param.basis }}</small>
                </div>
              </article>
            </div>
          </div>

          <aside class="detail-side">
            <section class="service-card">
              <div class="section-heading compact">
                <span>{{ copy.serviceFlow }}</span>
                <strong>{{ selectedModel.flowTitle }}</strong>
              </div>
              <ol>
                <li v-for="step in selectedModel.flow" :key="step">{{ step }}</li>
              </ol>
            </section>

            <section class="guardrail-card">
              <div class="section-heading compact">
                <span>{{ copy.noFakeData }}</span>
                <strong>{{ selectedModel.guardrailTitle }}</strong>
              </div>
              <p>{{ selectedModel.guardrail }}</p>
              <div class="evidence-pills">
                <span v-for="pill in selectedModel.evidencePills" :key="pill">{{ pill }}</span>
              </div>
            </section>
          </aside>
        </section>
      </main>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, type Component } from 'vue';
import { useI18n } from 'vue-i18n';
import {
  AlertCircle,
  BrainCircuit,
  Boxes,
  Layers3,
  ScanSearch,
} from 'lucide-vue-next';
import { hasStoredAuthToken } from '@/utils/authState';

type LocalizedText = {
  zh: string
  en: string
}

type EvidenceItem = {
  label: string
  value: string
  basis: string
}

type MetricCard = {
  label: string
  value: string
  percent: number
  explain: string
  basis: string
  available: boolean
}

type ParameterMethod = {
  name: string
  currentValue: string
  changeMethod: string
  whenToAdjust: string
  basis: string
}

type ModelEntry = {
  id: string
  mark: string
  accent: string
  visual: 'sequence' | 'residual' | 'patch' | 'detect'
  icon: Component
  evidenceLevel: 'live' | 'planned'
  name: LocalizedText
  slug: string
  task: LocalizedText
  status: string
  source: string
  integration: string
  metricStatus: string
  description: LocalizedText
  profile: EvidenceItem[]
  metrics: MetricCard[]
  parameterMethods: ParameterMethod[]
  flowTitle: string
  flow: string[]
  guardrailTitle: string
  guardrail: string
  evidencePills: string[]
}

const { locale } = useI18n();
const isZh = computed(() => locale.value.startsWith('zh'));
const isLoggedIn = computed(() => hasStoredAuthToken());
const selectedModelId = ref('bsarec-job');

const copy = computed(() => (isZh.value ? {
  eyebrow: '模型总栏',
  modelCount: '个模型条目',
  loginRequired: '请先登录',
  loginRequiredDesc: '模型总览需要登录后查看。',
  goLogin: '前往登录',
  modelDirectory: '模型列表',
  clickHint: '点击切换参数',
  viewing: '正在查看',
  parameterMethods: '参数改变方法',
  parameterMethodsTitle: '只给有依据的当前值；未接入模型只给调整方法',
  serviceFlow: '接入链路',
  noFakeData: '数据边界',
} : {
  eyebrow: 'Model Overview',
  modelCount: 'model entries',
  loginRequired: 'Login Required',
  loginRequiredDesc: 'Model overview requires a signed-in account.',
  goLogin: 'Go to Login',
  modelDirectory: 'Model List',
  clickHint: 'Click to switch params',
  viewing: 'Viewing',
  parameterMethods: 'Parameter Change Methods',
  parameterMethodsTitle: 'Known values only; planned models show methods, not fake metrics',
  serviceFlow: 'Integration Flow',
  noFakeData: 'Data Boundary',
}));

const noProjectMetrics = computed(() => isZh.value ? {
  value: '暂无项目实测指标',
  explain: '该模型目前只有注册资料和接入方法，项目内还没有评估集、推理服务或日志结果。',
  basis: 'ModelCatalogService 注册信息；未找到项目实测结果',
} : {
  value: 'No project metric yet',
  explain: 'This model currently has registry metadata and integration guidance only. No project evaluation set, serving endpoint, or logged result is connected yet.',
  basis: 'ModelCatalogService registry; no project evaluation result found',
});

const bsarecModel = computed<ModelEntry>(() => ({
  id: 'bsarec-job',
  mark: 'BSA',
  accent: '#22c55e',
  visual: 'sequence',
  icon: BrainCircuit,
  evidenceLevel: 'live',
  name: {
    zh: 'BSARec 岗位推荐模型',
    en: 'BSARec Job Recommendation',
  },
  slug: 'BSARec-Job / sequential recommendation',
  task: {
    zh: '序列推荐 / 岗位推荐',
    en: 'Sequential job recommendation',
  },
  status: isZh.value ? '已接入' : 'Integrated',
  source: isZh.value ? '项目内真实接入模型' : 'Project-backed model',
  integration: isZh.value ? '外部 Flask API' : 'External Flask API',
  metricStatus: isZh.value ? '有项目记录指标' : 'Recorded project metrics',
  description: {
    zh: '当前平台真正接入的是 BSARec：通过用户历史岗位 ID 序列请求本地 Flask 服务，返回 Top-K 岗位推荐。这里展示它的接口参数、已录入推荐指标，以及这些参数该怎么改。',
    en: 'BSARec is the model actually integrated in this platform. It sends historical job item ids to a local Flask service and returns Top-K job recommendations. This page shows its API parameters, recorded ranking metrics, and how each parameter should be changed.',
  },
  profile: [
    { label: isZh.value ? '模型标识' : 'Model id', value: 'BSARec-Job', basis: 'PredictionController / ModelController' },
    { label: isZh.value ? '参数规模' : 'Parameters', value: '0.2M', basis: 'paramCountM = 0.2' },
    { label: isZh.value ? '输入长度' : 'Input length', value: '50 item ids', basis: 'inputSize = 50 item ids' },
    { label: isZh.value ? '运行框架' : 'Runtime', value: 'pytorch/cpu', basis: 'framework = pytorch/cpu' },
    { label: isZh.value ? '模型服务' : 'Model service', value: '127.0.0.1:5000/recommend', basis: 'bsarec.api.base-url default' },
    { label: isZh.value ? '平台入口' : 'Platform endpoint', value: '/prediction/recommend', basis: 'predictionApi.recommend' },
  ],
  metrics: [
    {
      label: 'HR@10',
      value: '0.0992',
      percent: 9.92,
      available: true,
      explain: isZh.value ? 'Top-10 推荐命中能力，适合看候选召回是否覆盖目标岗位。' : 'Top-10 hit rate for candidate coverage.',
      basis: isZh.value ? '项目已录入 BSARec 展示指标' : 'Recorded BSARec project metric',
    },
    {
      label: 'NDCG@10',
      value: '0.0405',
      percent: 4.05,
      available: true,
      explain: isZh.value ? '考虑排序位置的推荐质量，越靠前命中贡献越大。' : 'Ranking-aware recommendation quality metric.',
      basis: isZh.value ? '项目已录入 BSARec 展示指标' : 'Recorded BSARec project metric',
    },
    {
      label: 'Recall@10',
      value: '0.0901',
      percent: 9.01,
      available: true,
      explain: isZh.value ? 'Top-10 候选覆盖真实相关岗位的比例，反映召回覆盖面。' : 'Top-10 relevant-item coverage.',
      basis: isZh.value ? '项目已录入 BSARec 展示指标' : 'Recorded BSARec project metric',
    },
    {
      label: 'AUC',
      value: '0.7515',
      percent: 75.15,
      available: true,
      explain: isZh.value ? '排序区分能力指标，用于观察正负样本相对排序是否可靠。' : 'Ranking separability metric for positive versus negative samples.',
      basis: isZh.value ? '项目已录入 BSARec 展示指标' : 'Recorded BSARec project metric',
    },
  ],
  parameterMethods: [
    {
      name: 'user_history',
      currentValue: isZh.value ? '最近岗位 ID，最长 50' : 'recent item ids, max 50',
      changeMethod: isZh.value
        ? '从用户最近浏览、收藏或投递过的岗位 ID 里取序列，按时间保留最近记录；超过 50 个时裁剪到 50，少于 1 个时不要发起推荐。'
        : 'Build the sequence from recent viewed, saved, or applied job ids. Keep the newest ids, trim to 50, and do not call recommendation with an empty history.',
      whenToAdjust: isZh.value ? '推荐太泛时优先检查历史是否过短或混入无关岗位。' : 'If recommendations are too broad, first check whether history is too short or noisy.',
      basis: 'inputSize = 50 item ids; BSARecClientService.normalizeRequest',
    },
    {
      name: 'top_k',
      currentValue: '10',
      changeMethod: isZh.value
        ? '通过 /prediction/recommend 请求体调整返回数量；当前 HR、NDCG、Recall 都按 @10 记录，所以默认保持 10，改大时要重新做 @K 评估。'
        : 'Change the returned count through the /prediction/recommend request body. Current HR, NDCG, and Recall are reported at @10, so changing it requires a new @K evaluation.',
      whenToAdjust: isZh.value ? '页面需要更多候选时调大；只需要首屏结果时调小。' : 'Increase it when the UI needs more candidates; decrease it for a compact first-screen result.',
      basis: 'BSARecClientService top_k default = 10; recorded metrics are @10',
    },
    {
      name: 'include_job_info',
      currentValue: 'true',
      changeMethod: isZh.value
        ? '需要岗位名称、公司、薪资等可读信息时保持 true；只做内部排序或压测时可以改为 false，减少返回体。'
        : 'Keep true when readable job title, company, or salary is needed. Set false for ranking-only or load-test calls to reduce response size.',
      whenToAdjust: isZh.value ? '展示页保持 true；批量评估可考虑 false。' : 'Keep true for display pages; consider false for batch evaluation.',
      basis: 'include_job_info default = true',
    },
    {
      name: 'user_id',
      currentValue: 'deepinsight-user',
      changeMethod: isZh.value
        ? '未传时后端使用默认用户；后续接真实账号后，把登录态里的用户标识写入请求，保证推荐和用户上下文一致。'
        : 'The backend uses a default user when omitted. After account context is connected, pass the signed-in user id so recommendations match the user context.',
      whenToAdjust: isZh.value ? '接入真实用户画像或多用户推荐时必须替换。' : 'Replace it when real user profiles or multi-user recommendation are connected.',
      basis: 'user_id default = deepinsight-user',
    },
    {
      name: 'item_size',
      currentValue: isZh.value ? '默认不传' : 'omit by default',
      changeMethod: isZh.value
        ? '后端只在请求出现该字段时透传，当前项目没有固定值；只有当 Flask 服务明确要求 item_size 时才补充。'
        : 'The backend only passes this field through when present. There is no fixed project value; add it only if the Flask service explicitly requires it.',
      whenToAdjust: isZh.value ? '服务端模型版本要求词表/物品空间大小时再填。' : 'Set it only when the model service version requires an item-space size.',
      basis: 'optional pass-through field in BSARecClientService',
    },
  ],
  flowTitle: isZh.value ? '当前可运行链路' : 'Current runnable path',
  flow: [
    'DeepInsight /training',
    '/api/v1/prediction/recommend',
    'BSARecClientService.normalizeRequest',
    'http://127.0.0.1:5000/recommend',
  ],
  guardrailTitle: isZh.value ? '实测和方法分开显示' : 'Metrics and methods are separated',
  guardrail: isZh.value
    ? 'BSARec 的数值来自项目已录入记录；参数建议来自后端默认值、接口请求体和模型注册输入，不把实时训练曲线或随机演示当成评估结论。'
    : 'BSARec numbers come from recorded project data. Parameter guidance comes from backend defaults, request fields, and registered input constraints.',
  evidencePills: ['PredictionController', 'BSARecClientService', 'predictionApi.recommend'],
}));

const plannedModels = computed<ModelEntry[]>(() => {
  const unavailable = noProjectMetrics.value;
  return [
    {
      id: 'resnet-50',
      mark: 'R50',
      accent: '#0ea5e9',
      visual: 'residual',
      icon: Layers3,
      evidenceLevel: 'planned',
      name: { zh: '残差网络 50 层', en: 'ResNet-50' },
      slug: 'ResNet-50 / image classification',
      task: { zh: '图像分类 / CNN 骨干网络', en: 'Image classification / CNN backbone' },
      status: isZh.value ? '待接入评估' : 'Evaluation pending',
      source: isZh.value ? '官方模型注册信息' : 'Official registry metadata',
      integration: isZh.value ? '未接真实推理服务' : 'No real serving endpoint yet',
      metricStatus: isZh.value ? '暂无项目实测指标' : 'No project metric yet',
      description: {
        zh: 'ResNet-50 已在项目官方模型目录中登记参数量、输入尺寸和框架，但当前平台没有它的真实权重推理、评估集或日志结果；这里只展示接入后参数该如何改变。',
        en: 'ResNet-50 has parameter count, input size, and framework metadata in the official model registry, but no real weights, evaluation set, or logs are connected in this platform yet.',
      },
      profile: [
        { label: isZh.value ? '模型标识' : 'Model id', value: 'ResNet-50', basis: 'ModelCatalogService.OFFICIAL_MODELS' },
        { label: isZh.value ? '参数规模' : 'Parameters', value: '25.6M', basis: 'paramCountM = 25.6' },
        { label: isZh.value ? '输入尺寸' : 'Input size', value: '224x224x3', basis: 'inputSize = 224x224x3' },
        { label: isZh.value ? '任务类型' : 'Task', value: isZh.value ? '图像分类' : 'classification', basis: 'taskType = classification' },
        { label: isZh.value ? '运行框架' : 'Runtime', value: 'pytorch', basis: 'framework = pytorch' },
        { label: isZh.value ? '接入状态' : 'Integration', value: isZh.value ? '待接入' : 'Pending', basis: 'no project endpoint wired' },
      ],
      metrics: ['Top-1 Accuracy', 'Validation Loss', 'Latency', 'Confusion Matrix'].map((label) => ({
        label,
        value: unavailable.value,
        percent: 0,
        available: false,
        explain: unavailable.explain,
        basis: unavailable.basis,
      })),
      parameterMethods: [
        {
          name: 'input_size',
          currentValue: '224x224x3',
          changeMethod: isZh.value
            ? '接入图像管线后，在预处理层统一 resize/crop 到注册尺寸；如果要改尺寸，需要同步检查模型权重、首层输入和评估脚本。'
            : 'After image pipeline integration, resize/crop inputs to the registered size. If this changes, validate weights, first-layer input, and evaluation scripts together.',
          whenToAdjust: isZh.value ? '迁移到不同分辨率数据集时再调整。' : 'Adjust only when moving to a dataset with a different resolution requirement.',
          basis: 'ModelCatalogService inputSize = 224x224x3',
        },
        {
          name: 'top_k',
          currentValue: isZh.value ? '待接入' : 'pending',
          changeMethod: isZh.value
            ? '需要先扩展真实分类接口，让请求体支持 top_k；当前 classify 是演示接口，不能拿随机返回当 ResNet 实测。'
            : 'First connect a real classification endpoint that accepts top_k. The current classify path is a demo and must not be treated as ResNet evaluation.',
          whenToAdjust: isZh.value ? '类别很多或需要候选解释时使用。' : 'Use it when many classes or candidate explanations are needed.',
          basis: 'PredictionController.classify is not a real ResNet evaluation path',
        },
        {
          name: 'batch_size',
          currentValue: isZh.value ? '待接入日志后决定' : 'decide after logs',
          changeMethod: isZh.value
            ? '接入训练或评估日志后，结合显存占用、吞吐和验证损失波动调整；不要在没有运行记录时写死。'
            : 'After logs are connected, tune it with memory, throughput, and validation-loss stability. Do not hard-code a value without run records.',
          whenToAdjust: isZh.value ? '显存不足、吞吐太低或验证曲线震荡时。' : 'Adjust when memory is tight, throughput is low, or validation curves become unstable.',
          basis: 'visual-analysis profiler / scalars modules after integration',
        },
      ],
      flowTitle: isZh.value ? '后续接入路径' : 'Future integration path',
      flow: ['ModelCatalogService registry', 'image preprocessing 224x224x3', 'real classification endpoint', 'evaluation logs / visual-analysis'],
      guardrailTitle: isZh.value ? '只展示注册依据' : 'Registry evidence only',
      guardrail: isZh.value
        ? 'ResNet-50 的参数量和输入尺寸有项目注册依据；准确率、延迟、混淆矩阵都没有项目实测，所以页面明确标为暂无。'
        : 'ResNet-50 parameter count and input size are project registry values. Accuracy, latency, and confusion matrix are not project-measured yet.',
      evidencePills: ['ModelCatalogService', 'model_registry', '待接入评估集'],
    },
    {
      id: 'vit-b16',
      mark: 'ViT',
      accent: '#8b5cf6',
      visual: 'patch',
      icon: Boxes,
      evidenceLevel: 'planned',
      name: { zh: '视觉 Transformer 基础版', en: 'ViT-B/16' },
      slug: 'ViT-B/16 / patch transformer',
      task: { zh: '图像分类 / Patch 序列', en: 'Image classification / patch sequence' },
      status: isZh.value ? '待接入评估' : 'Evaluation pending',
      source: isZh.value ? '官方模型注册信息' : 'Official registry metadata',
      integration: isZh.value ? '未接真实推理服务' : 'No real serving endpoint yet',
      metricStatus: isZh.value ? '暂无项目实测指标' : 'No project metric yet',
      description: {
        zh: 'ViT-B/16 在项目目录中登记为 384x384x3 输入的视觉 Transformer。由于还没有接入权重、评估集和日志，参数区只说明 patch 类模型接入后该怎么改。',
        en: 'ViT-B/16 is registered as a visual Transformer with 384x384x3 input. Since weights, evaluation data, and logs are not connected, this section only explains how patch-model parameters should change after integration.',
      },
      profile: [
        { label: isZh.value ? '模型标识' : 'Model id', value: 'ViT-B/16', basis: 'ModelCatalogService.OFFICIAL_MODELS' },
        { label: isZh.value ? '参数规模' : 'Parameters', value: '86.6M', basis: 'paramCountM = 86.6' },
        { label: isZh.value ? '输入尺寸' : 'Input size', value: '384x384x3', basis: 'inputSize = 384x384x3' },
        { label: isZh.value ? '任务类型' : 'Task', value: isZh.value ? '图像分类' : 'classification', basis: 'taskType = classification' },
        { label: isZh.value ? '运行框架' : 'Runtime', value: 'pytorch', basis: 'framework = pytorch' },
        { label: isZh.value ? '接入状态' : 'Integration', value: isZh.value ? '待接入' : 'Pending', basis: 'no project endpoint wired' },
      ],
      metrics: ['Top-1 Accuracy', 'Attention Map', 'Embedding Drift', 'Latency'].map((label) => ({
        label,
        value: unavailable.value,
        percent: 0,
        available: false,
        explain: unavailable.explain,
        basis: unavailable.basis,
      })),
      parameterMethods: [
        {
          name: 'input_size',
          currentValue: '384x384x3',
          changeMethod: isZh.value
            ? '保持预处理尺寸与注册信息一致；ViT-B/16 的 patch 粒度要求输入尺寸能被 16 整除，改尺寸时要重新核对位置编码处理。'
            : 'Keep preprocessing aligned with the registered size. ViT-B/16 patching requires dimensions divisible by 16, and position embeddings must be checked when size changes.',
          whenToAdjust: isZh.value ? '数据源分辨率或权重版本改变时。' : 'Adjust when dataset resolution or weight version changes.',
          basis: 'ModelCatalogService inputSize = 384x384x3; ViT-B/16 patch naming',
        },
        {
          name: 'patch_size',
          currentValue: '16',
          changeMethod: isZh.value
            ? '这里的 16 来自模型名称 B/16，不是项目实测参数；除非更换模型变体，否则不要在页面里让用户随意改。'
            : 'The 16 comes from the B/16 model variant name, not a measured project hyperparameter. Do not expose it as a casual runtime knob unless the model variant changes.',
          whenToAdjust: isZh.value ? '更换 ViT 模型变体时才调整。' : 'Change only when switching ViT variants.',
          basis: 'model name ViT-B/16',
        },
        {
          name: 'embedding_analysis',
          currentValue: isZh.value ? '待接入向量日志' : 'requires embedding logs',
          changeMethod: isZh.value
            ? '接入 embedding 日志后，用可视化分析的向量投影模块看类别是否聚团，再决定是否调整增强策略或微调层。'
            : 'After embedding logs are connected, use the embedding module to inspect clustering before changing augmentation or fine-tuning layers.',
          whenToAdjust: isZh.value ? '类别混叠、聚类不明显时。' : 'Adjust when classes overlap or clusters are weak.',
          basis: 'visual-analysis embeddings module',
        },
      ],
      flowTitle: isZh.value ? '后续接入路径' : 'Future integration path',
      flow: ['ModelCatalogService registry', 'image preprocessing 384x384x3', 'patch / embedding logs', 'visual-analysis embeddings'],
      guardrailTitle: isZh.value ? '不把通用知识当实测' : 'No generic facts as measured data',
      guardrail: isZh.value
        ? 'ViT-B/16 的尺寸、参数量来自项目注册；注意力图、准确率和向量分布必须等项目日志接入后再展示。'
        : 'ViT-B/16 size and parameter count come from the project registry. Attention maps, accuracy, and embeddings must wait for connected project logs.',
      evidencePills: ['ModelCatalogService', 'ViT-B/16', 'visual-analysis embeddings'],
    },
    {
      id: 'yolov8s',
      mark: 'Y8S',
      accent: '#f97316',
      visual: 'detect',
      icon: ScanSearch,
      evidenceLevel: 'planned',
      name: { zh: 'YOLOv8 Small 目标检测', en: 'YOLOv8s Object Detection' },
      slug: 'YOLOv8s / object detection',
      task: { zh: '目标检测 / 阈值评估', en: 'Object detection / threshold evaluation' },
      status: isZh.value ? '待接入评估' : 'Evaluation pending',
      source: isZh.value ? '官方模型注册信息' : 'Official registry metadata',
      integration: isZh.value ? '未接真实检测服务' : 'No real detection endpoint yet',
      metricStatus: isZh.value ? '暂无项目实测指标' : 'No project metric yet',
      description: {
        zh: 'YOLOv8s 已在官方模型目录中登记输入尺寸和参数量，但当前检测接口没有接入真实 YOLOv8s 服务。这里把阈值、NMS 和返回数量作为后续接入时的调参方法展示。',
        en: 'YOLOv8s has registered input size and parameter count, but the current detection path is not wired to a real YOLOv8s service. This section shows threshold, NMS, and detection-count tuning methods for future integration.',
      },
      profile: [
        { label: isZh.value ? '模型标识' : 'Model id', value: 'YOLOv8s', basis: 'ModelCatalogService.OFFICIAL_MODELS' },
        { label: isZh.value ? '参数规模' : 'Parameters', value: '11.2M', basis: 'paramCountM = 11.2' },
        { label: isZh.value ? '输入尺寸' : 'Input size', value: '640x640x3', basis: 'inputSize = 640x640x3' },
        { label: isZh.value ? '任务类型' : 'Task', value: isZh.value ? '目标检测' : 'detection', basis: 'taskType = detection' },
        { label: isZh.value ? '运行框架' : 'Runtime', value: 'pytorch', basis: 'framework = pytorch' },
        { label: isZh.value ? '接入状态' : 'Integration', value: isZh.value ? '待接入' : 'Pending', basis: 'no project endpoint wired' },
      ],
      metrics: ['mAP', 'Precision', 'Recall', 'Detection Latency'].map((label) => ({
        label,
        value: unavailable.value,
        percent: 0,
        available: false,
        explain: unavailable.explain,
        basis: unavailable.basis,
      })),
      parameterMethods: [
        {
          name: 'confidence_threshold',
          currentValue: isZh.value ? '待接入' : 'pending',
          changeMethod: isZh.value
            ? '真实检测服务接入后，把它作为请求参数或服务配置暴露；误检多时调高，漏检多时调低，并用 PR/ROC 模块复核。'
            : 'After a real detection service is connected, expose this through request or service config. Raise it for false positives, lower it for missed detections, and verify with PR/ROC analysis.',
          whenToAdjust: isZh.value ? '误检/漏检平衡不符合业务目标时。' : 'Adjust when the false-positive / false-negative balance misses business needs.',
          basis: 'visual-analysis prCurves module after detection logs exist',
        },
        {
          name: 'nms_iou_threshold',
          currentValue: isZh.value ? '待接入' : 'pending',
          changeMethod: isZh.value
            ? '接入后用于控制重叠框合并；同一目标出现多个框时调低，邻近目标被误合并时调高。'
            : 'Use it to control duplicate-box suppression. Lower it when one object has duplicate boxes; raise it when nearby objects are merged incorrectly.',
          whenToAdjust: isZh.value ? '框重复或密集目标漏框时。' : 'Adjust for duplicate boxes or crowded-object misses.',
          basis: 'detection post-processing method; no project value yet',
        },
        {
          name: 'max_detections',
          currentValue: isZh.value ? '待接入' : 'pending',
          changeMethod: isZh.value
            ? '接入后按业务画面复杂度限制最大返回框数；过小会截断目标，过大会增加前端渲染和人工审核成本。'
            : 'After integration, limit returned boxes by scene complexity. Too low truncates objects; too high increases rendering and review cost.',
          whenToAdjust: isZh.value ? '检测结果太多或被截断时。' : 'Adjust when detections are too many or truncated.',
          basis: 'future detection response contract',
        },
      ],
      flowTitle: isZh.value ? '后续接入路径' : 'Future integration path',
      flow: ['ModelCatalogService registry', 'image preprocessing 640x640x3', 'real detection service', 'PR/ROC + profiler logs'],
      guardrailTitle: isZh.value ? '检测指标必须等标注集' : 'Detection metrics need labeled data',
      guardrail: isZh.value
        ? 'YOLOv8s 只有注册信息有依据；mAP、Precision、Recall 必须基于项目标注集和检测日志生成，不能提前填。'
        : 'YOLOv8s has registry evidence only. mAP, Precision, and Recall require project labels and detection logs before display.',
      evidencePills: ['ModelCatalogService', 'YOLOv8s', 'PR/ROC 待接入'],
    },
  ];
});

const modelCatalog = computed(() => [
  bsarecModel.value,
  ...plannedModels.value,
]);
const selectedModel = computed(() => modelCatalog.value.find((model) => model.id === selectedModelId.value) || modelCatalog.value[0]);

function displayName(model: ModelEntry) {
  return isZh.value ? model.name.zh : model.name.en;
}

function displayTask(model: ModelEntry) {
  return isZh.value ? model.task.zh : model.task.en;
}

function displayDescription(model: ModelEntry) {
  return isZh.value ? model.description.zh : model.description.en;
}

function accentStyle(model: ModelEntry) {
  return { '--model-accent': model.accent };
}
</script>

<style scoped>
.training-page {
  padding: 24px;
  max-width: 1460px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-start;
  margin-bottom: 20px;
}

.hierarchy-eyebrow,
.model-kicker {
  display: block;
  color: var(--primary-color);
  font-size: 11px;
  font-weight: var(--font-weight-title);
  letter-spacing: 0.08em;
  text-transform: uppercase;
  margin-bottom: 7px;
}

.page-header h2 {
  font-size: 22px;
  font-weight: var(--font-weight-body);
  color: var(--text-primary);
  margin: 0 0 4px;
}

.page-header p {
  max-width: 760px;
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.7;
  margin: 0;
}

.header-count {
  min-width: 136px;
  padding: 12px 14px;
  border: 1px solid var(--border-color);
  border-radius: 14px;
  background: color-mix(in srgb, var(--surface-1) 82%, transparent);
  text-align: right;
}

.header-count strong {
  display: block;
  color: var(--text-primary);
  font-size: 26px;
  line-height: 1;
}

.header-count span {
  color: var(--text-secondary);
  font-size: 12px;
}

.login-hint {
  border-radius: 20px;
  text-align: center;
  padding: 60px;
}

.hint-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.hint-content svg {
  color: var(--primary-color);
}

.hint-content h3 {
  font-size: 18px;
  font-weight: var(--font-weight-body);
  color: var(--text-primary);
  margin: 0;
}

.hint-content p {
  font-size: 13px;
  color: var(--text-secondary);
  margin: 0;
}

.model-overview-shell {
  display: grid;
  grid-template-columns: minmax(280px, 360px) minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}

.model-directory,
.model-detail {
  border: 1px solid var(--border-color);
  border-radius: 16px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.07), rgba(255, 255, 255, 0.015)),
    color-mix(in srgb, var(--surface-1) 82%, transparent);
  box-shadow: var(--shadow-soft);
}

.model-directory {
  padding: 16px;
  display: grid;
  gap: 14px;
  position: sticky;
  top: 96px;
}

.directory-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: var(--text-secondary);
  font-size: 13px;
}

.directory-head strong {
  font-size: 12px;
  color: var(--primary-color);
}

.directory-list {
  display: grid;
  gap: 10px;
}

.model-entry {
  width: 100%;
  min-height: 126px;
  padding: 14px;
  border: 1px solid var(--border-color);
  border-radius: 14px;
  background: color-mix(in srgb, var(--surface-2) 72%, transparent);
  display: grid;
  grid-template-columns: 58px minmax(0, 1fr);
  gap: 12px;
  text-align: left;
  cursor: pointer;
  transition: border-color 180ms ease, background 180ms ease, transform 180ms ease, box-shadow 180ms ease;
}

.model-entry:hover,
.model-entry.active {
  border-color: color-mix(in srgb, var(--model-accent, var(--primary-color)) 48%, var(--border-color));
  background:
    linear-gradient(135deg, color-mix(in srgb, var(--model-accent, var(--primary-color)) 13%, transparent), transparent 55%),
    var(--surface-2);
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.08);
  transform: translateY(-1px);
}

.entry-glyph,
.model-mark {
  position: relative;
  overflow: hidden;
  display: grid;
  place-items: center;
  color: #fff;
  background:
    radial-gradient(circle at 28% 20%, color-mix(in srgb, var(--model-accent, var(--primary-color)) 50%, transparent), transparent 30%),
    linear-gradient(145deg, #101827, #263143);
}

.entry-glyph {
  width: 58px;
  height: 58px;
  border-radius: 16px;
}

.entry-glyph::before,
.model-mark::before {
  content: '';
  position: absolute;
  border-color: color-mix(in srgb, var(--model-accent, var(--primary-color)) 62%, transparent);
}

.visual-sequence::before {
  inset: 10px;
  border: 2px dashed;
  border-radius: 999px;
}

.visual-residual::before {
  width: 70%;
  height: 46%;
  border: 2px solid;
  border-left-width: 6px;
  border-radius: 10px;
}

.visual-patch::before {
  width: 62%;
  height: 62%;
  border: 2px solid;
  border-radius: 6px;
  box-shadow:
    14px 0 0 -8px color-mix(in srgb, var(--model-accent, var(--primary-color)) 52%, transparent),
    0 14px 0 -8px color-mix(in srgb, var(--model-accent, var(--primary-color)) 52%, transparent);
}

.visual-detect::before {
  inset: 13px;
  border: 2px solid;
  border-radius: 4px;
  box-shadow: 18px -10px 0 -12px #fff, -18px 14px 0 -12px #fff;
}

.entry-glyph svg,
.model-mark svg,
.model-mark span {
  position: relative;
  z-index: 1;
}

.entry-copy {
  min-width: 0;
  display: grid;
  gap: 6px;
}

.entry-copy strong {
  color: var(--text-primary);
  font-size: 15px;
  font-weight: var(--font-weight-title);
  line-height: 1.25;
}

.entry-copy small,
.entry-copy em,
.entry-status {
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.5;
  font-style: normal;
}

.entry-status {
  grid-column: 2;
  width: max-content;
  align-self: end;
  padding: 4px 9px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--model-accent, var(--primary-color)) 12%, transparent);
  color: color-mix(in srgb, var(--model-accent, var(--primary-color)) 72%, var(--text-primary));
}

.entry-status.status-planned {
  color: var(--text-secondary);
}

.model-detail {
  min-width: 0;
  padding: clamp(18px, 2vw, 26px);
}

.model-evaluation-hero,
.parameter-panel,
.service-card,
.guardrail-card {
  border: 1px solid var(--border-color);
  border-radius: 14px;
  background: color-mix(in srgb, var(--surface-2) 72%, transparent);
}

.model-evaluation-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(420px, 0.95fr);
  gap: 24px;
  padding: clamp(22px, 2.3vw, 34px);
}

.model-identity {
  display: flex;
  align-items: flex-start;
  gap: 18px;
  min-width: 0;
}

.model-mark {
  width: 78px;
  height: 78px;
  flex-shrink: 0;
  border-radius: 20px;
}

.model-mark span {
  position: absolute;
  right: 8px;
  bottom: 7px;
  font-size: 11px;
  font-weight: 850;
}

.model-identity h3 {
  color: var(--text-primary);
  font-size: clamp(30px, 3.2vw, 46px);
  line-height: 1.05;
  letter-spacing: 0;
  margin: 0;
}

.model-identity p {
  color: var(--text-secondary);
  max-width: 720px;
  margin: 12px 0 0;
  font-size: 14px;
  line-height: 1.8;
}

.hero-badges,
.evidence-pills {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.hero-badges span,
.evidence-pills span {
  min-height: 30px;
  padding: 6px 11px;
  border: 1px solid color-mix(in srgb, var(--model-accent, var(--primary-color)) 28%, var(--border-color));
  border-radius: 999px;
  background: color-mix(in srgb, var(--model-accent, var(--primary-color)) 9%, transparent);
  color: color-mix(in srgb, var(--model-accent, var(--primary-color)) 76%, var(--text-primary));
  font-size: 12px;
}

.model-facts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.model-facts article,
.metric-tile,
.method-list article {
  border: 1px solid var(--border-color);
  border-radius: 14px;
  background: var(--panel-bg);
}

.model-facts article {
  min-height: 92px;
  padding: 14px;
  display: grid;
  align-content: start;
  gap: 6px;
}

.model-facts span,
.metric-tile span,
.section-heading span {
  color: var(--text-secondary);
  font-size: 12px;
}

.model-facts strong {
  color: var(--text-primary);
  font-size: 18px;
  line-height: 1.25;
}

.model-facts small,
.metric-tile small,
.method-meta small,
.guardrail-card p {
  color: var(--text-muted);
  font-size: 11px;
  line-height: 1.55;
}

.metric-band {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-top: 18px;
}

.metric-tile {
  min-height: 184px;
  padding: 16px;
  display: grid;
  align-content: start;
  gap: 11px;
}

.metric-tile.muted {
  border-style: dashed;
}

.metric-tile strong {
  display: block;
  color: var(--text-primary);
  font-size: clamp(17px, 2vw, 30px);
  line-height: 1.15;
  margin-top: 6px;
}

.metric-tile.muted strong {
  font-size: 18px;
  color: var(--text-secondary);
}

.metric-bar {
  height: 9px;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.16);
  overflow: hidden;
}

.metric-bar i {
  display: block;
  height: 100%;
  min-width: 8px;
  border-radius: inherit;
  background: linear-gradient(90deg, var(--model-accent, var(--primary-color)), #38bdf8);
}

.metric-tile p,
.method-list p,
.guardrail-card p {
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.7;
  margin: 0;
}

.overview-workbench {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(320px, 410px);
  gap: 18px;
  margin-top: 18px;
  align-items: start;
}

.parameter-panel,
.service-card,
.guardrail-card {
  padding: 18px;
}

.section-heading {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 16px;
}

.section-heading.compact {
  display: grid;
  gap: 4px;
}

.section-heading strong {
  color: var(--text-primary);
  font-size: 16px;
  line-height: 1.35;
}

.method-list {
  display: grid;
  gap: 10px;
}

.method-list article {
  padding: 15px;
}

.method-list header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 9px;
}

.method-list code {
  color: color-mix(in srgb, var(--model-accent, var(--primary-color)) 72%, var(--text-primary));
  font-size: 13px;
  background: color-mix(in srgb, var(--model-accent, var(--primary-color)) 9%, transparent);
  border-radius: 8px;
  padding: 4px 8px;
}

.method-list strong {
  color: var(--text-primary);
  font-size: 15px;
  text-align: right;
}

.method-meta {
  display: grid;
  gap: 6px;
  margin-top: 9px;
}

.method-meta span {
  color: color-mix(in srgb, var(--model-accent, var(--primary-color)) 70%, var(--text-primary));
  font-size: 12px;
}

.detail-side {
  display: grid;
  gap: 18px;
  align-content: start;
}

.service-card ol {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  gap: 10px;
  counter-reset: flow;
}

.service-card li {
  counter-increment: flow;
  position: relative;
  min-height: 44px;
  padding: 12px 12px 12px 44px;
  border-radius: 12px;
  background: var(--panel-bg);
  color: var(--text-primary);
  font-size: 13px;
  line-height: 1.5;
  word-break: break-word;
}

.service-card li::before {
  content: counter(flow);
  position: absolute;
  left: 12px;
  top: 12px;
  width: 22px;
  height: 22px;
  border-radius: 8px;
  background: var(--text-primary);
  color: var(--bg-color);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 800;
}

@media (max-width: 1180px) {
  .model-overview-shell,
  .model-evaluation-hero,
  .overview-workbench {
    grid-template-columns: 1fr;
  }

  .model-directory {
    position: static;
  }

  .metric-band {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .training-page {
    padding: 16px;
  }

  .page-header,
  .model-identity,
  .section-heading,
  .method-list header {
    display: grid;
    justify-items: start;
  }

  .header-count {
    text-align: left;
  }

  .model-facts,
  .metric-band {
    grid-template-columns: 1fr;
  }

  .model-mark {
    width: 64px;
    height: 64px;
  }

  .model-detail {
    padding: 16px;
  }
}
</style>
