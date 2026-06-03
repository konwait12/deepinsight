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
  Clock3,
  GitBranch,
  Layers,
  Zap,
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

const bsarecModel = computed<ModelEntry>(() => ({
  id: 'bsarec-job',
  mark: 'BSA',
  accent: '#22c55e',
  visual: 'sequence',
  icon: BrainCircuit,
  evidenceLevel: 'live',
  name: {
    zh: 'BSARec Job',
    en: 'BSARec Job',
  },
  slug: 'BSARec / main online recommender',
  task: {
    zh: '序列推荐 / 岗位推荐',
    en: 'Sequential job recommendation',
  },
  status: isZh.value ? '已接入' : 'Integrated',
  source: isZh.value ? '项目内真实接入模型' : 'Project-backed model',
  integration: isZh.value ? '在线 / 推荐模型' : 'Online / recommendation model',
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

function plannedMetrics(modelName: string, sourcePath: string): MetricCard[] {
  return [
    {
      label: 'HR@10',
      value: isZh.value ? '待评估' : 'Pending',
      percent: 0,
      available: false,
      explain: isZh.value
        ? `${modelName} 还没有接入岗位推荐 API，不能把其他数据集结果当成本站在线命中率。`
        : `${modelName} is not connected to the job recommendation API, so external dataset scores are not shown as platform hit rate.`,
      basis: sourcePath,
    },
    {
      label: 'NDCG@10',
      value: isZh.value ? '待评估' : 'Pending',
      percent: 0,
      available: false,
      explain: isZh.value
        ? '接入后需要用同一批岗位行为序列和 Top-10 结果重新计算，才适合和 BSARec 对比。'
        : 'After integration, compute this again on the same job behavior sequences and Top-10 outputs before comparing with BSARec.',
      basis: isZh.value ? '本站暂无该模型在线排序日志' : 'No online ranking log in this platform yet',
    },
    {
      label: 'Recall@10',
      value: isZh.value ? '待评估' : 'Pending',
      percent: 0,
      available: false,
      explain: isZh.value
        ? '用于衡量候选覆盖面；当前卡片只展示模型结构和接入方法，不填模拟数值。'
        : 'Used for candidate coverage. This card only shows model structure and integration methods for now.',
      basis: isZh.value ? '待统一评估集' : 'Awaiting a shared evaluation set',
    },
    {
      label: isZh.value ? '接入状态' : 'Integration',
      value: isZh.value ? '待接入' : 'Planned',
      percent: 0,
      available: false,
      explain: isZh.value
        ? '已在 similar-models 中找到源码，可以作为后续离线训练和在线适配对象。'
        : 'Source exists under similar-models and can be adapted for offline training and future serving.',
      basis: sourcePath,
    },
  ];
}

function plannedFlow(modelFolder: string): string[] {
  return isZh.value
    ? [
        `similar-models/${modelFolder} 源码审阅`,
        '统一岗位 item_id / 行为序列格式',
        '离线训练并导出同口径 HR@10、NDCG@10、Recall@10',
        '封装为 /prediction/recommend 可调用适配器',
      ]
    : [
        `Review similar-models/${modelFolder}`,
        'Unify job item_id and behavior sequence format',
        'Train offline and export HR@10, NDCG@10, Recall@10 on the same protocol',
        'Wrap as a /prediction/recommend adapter',
      ];
}

function plannedGuardrail(modelName: string) {
  return isZh.value
    ? `${modelName} 当前只作为模型展示和后续对比接入对象。页面展示源码默认参数、适用场景和评估口径，不展示伪造的在线推理结果。`
    : `${modelName} is currently shown as a model card and future comparison target. The page shows source-backed defaults, fit, and evaluation protocol without fake online inference results.`;
}

const sasrecModel = computed<ModelEntry>(() => ({
  id: 'sasrec-base',
  mark: 'SAS',
  accent: '#2563eb',
  visual: 'sequence',
  icon: GitBranch,
  evidenceLevel: 'planned',
  name: {
    zh: 'SASRec Base',
    en: 'SASRec Base',
  },
  slug: 'SASRec / sequential baseline',
  task: {
    zh: '序列推荐 / 基线模型',
    en: 'Sequential recommendation baseline',
  },
  status: isZh.value ? '待接入' : 'Planned',
  source: 'similar-models/SASRec',
  integration: isZh.value ? '序列推荐 / 基线模型' : 'Sequential / baseline model',
  metricStatus: isZh.value ? '暂无在线指标' : 'No online metrics yet',
  description: {
    zh: 'SASRec 是经典自注意力序列推荐基线，适合放在 BSARec 旁边做结构对比。它只依赖用户历史 item 序列，便于解释“同样的岗位浏览历史，在基础 Transformer 序列模型下会怎样排序”。',
    en: 'SASRec is a classic self-attention sequential recommendation baseline. It is useful beside BSARec because it uses the same item sequence idea and makes a clean baseline for ranking comparisons.',
  },
  profile: [
    { label: isZh.value ? '模型标识' : 'Model id', value: 'SASRec Base', basis: 'similar-models/SASRec/model.py' },
    { label: isZh.value ? '输入数据' : 'Input', value: 'item sequence', basis: 'input_seq placeholder: (None, args.maxlen)' },
    { label: isZh.value ? '默认长度' : 'Default length', value: 'maxlen = 50', basis: 'SASRec/main.py default' },
    { label: isZh.value ? '隐藏维度' : 'Hidden units', value: 'hidden_units = 50', basis: 'SASRec/main.py default' },
    { label: isZh.value ? '结构层数' : 'Blocks', value: 'num_blocks = 2', basis: 'SASRec/main.py default' },
    { label: isZh.value ? '运行框架' : 'Runtime', value: 'TensorFlow style', basis: 'SASRec/model.py tf.placeholder' },
  ],
  metrics: plannedMetrics('SASRec Base', 'similar-models/SASRec/main.py'),
  parameterMethods: [
    {
      name: 'maxlen',
      currentValue: '50',
      changeMethod: isZh.value
        ? '控制模型读取的最近岗位行为数量。岗位兴趣变化快时可保持 50；若用户历史较长且稳定，可以在离线实验中尝试 100 或 200。'
        : 'Controls how many recent job events the model reads. Keep 50 for fast-changing interests; try 100 or 200 offline for long stable histories.',
      whenToAdjust: isZh.value ? '召回过窄时增加；噪声浏览较多时降低。' : 'Increase for broader recall; decrease when browsing history is noisy.',
      basis: 'SASRec/main.py --maxlen default=50; README example uses --maxlen=200',
    },
    {
      name: 'hidden_units',
      currentValue: '50',
      changeMethod: isZh.value
        ? '表示 item 序列的隐向量维度。岗位类别更复杂时可以上调，但要配合验证集观察过拟合。'
        : 'Embedding width for item sequences. Increase for more complex job taxonomies, then watch validation overfitting.',
      whenToAdjust: isZh.value ? '类别多、语义差异大时增加；小数据集先保持默认。' : 'Increase for many categories; keep default on small datasets.',
      basis: 'SASRec/main.py --hidden_units default=50',
    },
    {
      name: 'num_blocks',
      currentValue: '2',
      changeMethod: isZh.value
        ? '控制自注意力块堆叠深度。作为基线模型建议先保持 2 层，方便和 BSARec 做清楚对比。'
        : 'Controls the depth of stacked self-attention blocks. Keep 2 first so the baseline stays easy to compare with BSARec.',
      whenToAdjust: isZh.value ? '长序列依赖明显时再增加。' : 'Increase only when long-range sequence dependency is clear.',
      basis: 'SASRec/main.py --num_blocks default=2',
    },
    {
      name: 'dropout_rate',
      currentValue: '0.5',
      changeMethod: isZh.value
        ? '控制训练正则强度。岗位行为稀疏时较高 dropout 可降低记忆噪声，数据量增加后可下调。'
        : 'Controls regularization. Higher dropout helps sparse job histories; lower it after data volume grows.',
      whenToAdjust: isZh.value ? '训练集高、验证集低时增加；收敛慢时降低。' : 'Increase when validation lags training; lower if convergence is too slow.',
      basis: 'SASRec/main.py --dropout_rate default=0.5',
    },
  ],
  flowTitle: isZh.value ? '基线模型接入路线' : 'Baseline integration route',
  flow: plannedFlow('SASRec'),
  guardrailTitle: isZh.value ? '只展示基线证据' : 'Baseline evidence only',
  guardrail: plannedGuardrail('SASRec Base'),
  evidencePills: ['SASRec/main.py', 'SASRec/model.py', 'SASRec/modules.py', 'SASRec/README.md'],
}));

const bert4RecModel = computed<ModelEntry>(() => ({
  id: 'bert4rec',
  mark: 'BERT',
  accent: '#7c3aed',
  visual: 'patch',
  icon: Layers,
  evidenceLevel: 'planned',
  name: {
    zh: 'BERT4Rec',
    en: 'BERT4Rec',
  },
  slug: 'BERT4Rec / masked sequence modeling',
  task: {
    zh: 'Transformer / 推荐模型',
    en: 'Transformer recommendation model',
  },
  status: isZh.value ? '待接入' : 'Planned',
  source: 'similar-models/BERT4Rec',
  integration: isZh.value ? 'Transformer / 推荐模型' : 'Transformer / recommendation model',
  metricStatus: isZh.value ? '暂无在线指标' : 'No online metrics yet',
  description: {
    zh: 'BERT4Rec 用掩码序列建模学习用户兴趣，名字识别度高，也适合解释“根据上下文补全下一类岗位”的推荐逻辑。它比 SASRec 更像双向 Transformer 表征模型，展示在网站里很直观。',
    en: 'BERT4Rec learns preference through masked sequence modeling. It is recognizable, Transformer-shaped, and easy to explain as filling in the next likely job from sequence context.',
  },
  profile: [
    { label: isZh.value ? '模型标识' : 'Model id', value: 'BERT4Rec', basis: 'similar-models/BERT4Rec/run.py' },
    { label: isZh.value ? '输入数据' : 'Input', value: 'masked item sequence', basis: 'masked_lm_positions / masked_lm_ids' },
    { label: isZh.value ? '序列长度' : 'Sequence length', value: '50 or 200', basis: 'run_beauty.sh = 50; run_ml-1m.sh = 200' },
    { label: isZh.value ? '隐藏维度' : 'Hidden size', value: '64', basis: 'bert_config_ml-1m_64.json' },
    { label: isZh.value ? '层数 / 头数' : 'Layers / heads', value: '2 layers / 2 heads', basis: 'bert_config_ml-1m_64.json' },
    { label: isZh.value ? '运行框架' : 'Runtime', value: 'TensorFlow BERT', basis: 'BERT4Rec/run.py' },
  ],
  metrics: plannedMetrics('BERT4Rec', 'similar-models/BERT4Rec/run.py'),
  parameterMethods: [
    {
      name: 'max_seq_length',
      currentValue: '50 / 200',
      changeMethod: isZh.value
        ? '岗位浏览链路较短时用 50；如果要覆盖更长求职周期，可按 ml-1m 脚本思路扩到 200，并同步调整位置编码。'
        : 'Use 50 for short job browsing paths. For longer job-search cycles, expand toward 200 and keep positional settings aligned.',
      whenToAdjust: isZh.value ? '用户行为时间跨度变长时增加。' : 'Increase when user behavior spans longer time windows.',
      basis: 'run_beauty.sh max_seq_length=50; run_ml-1m.sh max_seq_length=200',
    },
    {
      name: 'masked_lm_prob',
      currentValue: '0.2 - 0.6',
      changeMethod: isZh.value
        ? '控制训练时被遮蔽的历史岗位比例。行为很稀疏时不要过高，否则上下文不足；行为丰富时可提高以增强泛化。'
        : 'Controls the masked portion during training. Keep it lower for sparse histories; raise it when sequences are rich enough.',
      whenToAdjust: isZh.value ? '泛化不足时增加；训练不稳定时降低。' : 'Increase for generalization; lower when training becomes unstable.',
      basis: 'run_ml-1m.sh masked_lm_prob=0.2; run_beauty.sh masked_lm_prob=0.6',
    },
    {
      name: 'hidden_size',
      currentValue: '64',
      changeMethod: isZh.value
        ? '表示 BERT 编码宽度。若岗位标签、行业、技能组合更多，可离线尝试更大 hidden_size，但要同步扩大中间层。'
        : 'BERT encoder width. Try larger values offline when job labels, industries, and skill combinations grow.',
      whenToAdjust: isZh.value ? '岗位语义空间明显增大时调整。' : 'Adjust when the semantic job space becomes larger.',
      basis: 'bert_config_ml-1m_64.json hidden_size=64; intermediate_size=256',
    },
    {
      name: 'num_hidden_layers',
      currentValue: '2',
      changeMethod: isZh.value
        ? '当前配置是轻量 BERT4Rec。若未来数据充足，可以增加层数，但要重新记录训练耗时和推理延迟。'
        : 'Current config is lightweight. Add layers only with enough data, and record training cost plus inference latency again.',
      whenToAdjust: isZh.value ? '需要更强上下文建模且算力允许时增加。' : 'Increase when stronger context modeling is needed and compute allows it.',
      basis: 'bert_config_ml-1m_64.json num_hidden_layers=2',
    },
  ],
  flowTitle: isZh.value ? 'Transformer 接入路线' : 'Transformer integration route',
  flow: plannedFlow('BERT4Rec'),
  guardrailTitle: isZh.value ? '不混用预训练指标' : 'No mixed pretraining score',
  guardrail: plannedGuardrail('BERT4Rec'),
  evidencePills: ['BERT4Rec/run.py', 'BERT4Rec/modeling.py', 'bert_config_ml-1m_64.json', 'run_beauty.sh'],
}));

const tisasRecModel = computed<ModelEntry>(() => ({
  id: 'tisasrec',
  mark: 'Ti',
  accent: '#0891b2',
  visual: 'detect',
  icon: Clock3,
  evidenceLevel: 'planned',
  name: {
    zh: 'TiSASRec',
    en: 'TiSASRec',
  },
  slug: 'TiSASRec / time-aware sequential recommendation',
  task: {
    zh: '时间感知 / 序列推荐',
    en: 'Time-aware sequential recommendation',
  },
  status: isZh.value ? '待接入' : 'Planned',
  source: 'similar-models/TiSASRec',
  integration: isZh.value ? '时间感知 / 序列推荐' : 'Time-aware / sequential model',
  metricStatus: isZh.value ? '暂无在线指标' : 'No online metrics yet',
  description: {
    zh: 'TiSASRec 在序列推荐里显式加入行为时间间隔，适合岗位浏览、收藏、投递这类有明显时间节奏的场景。它能帮助页面说明“同样点击过这些岗位，最近发生和很久以前发生的权重不同”。',
    en: 'TiSASRec adds explicit time-interval signals to sequential recommendation. It fits job browsing, saving, and application timelines where recency and gaps matter.',
  },
  profile: [
    { label: isZh.value ? '模型标识' : 'Model id', value: 'TiSASRec', basis: 'similar-models/TiSASRec/model.py' },
    { label: isZh.value ? '输入数据' : 'Input', value: 'item sequence + time matrix', basis: 'time_matrix placeholder: (None, maxlen, maxlen)' },
    { label: isZh.value ? '默认长度' : 'Default length', value: 'maxlen = 50', basis: 'TiSASRec/main.py default' },
    { label: isZh.value ? '时间桶' : 'Time span', value: 'time_span = 256', basis: 'TiSASRec/main.py default' },
    { label: isZh.value ? '结构层数' : 'Blocks', value: 'num_blocks = 2', basis: 'TiSASRec/main.py default' },
    { label: isZh.value ? '运行框架' : 'Runtime', value: 'TensorFlow style', basis: 'TiSASRec/model.py tf.placeholder' },
  ],
  metrics: plannedMetrics('TiSASRec', 'similar-models/TiSASRec/main.py'),
  parameterMethods: [
    {
      name: 'time_span',
      currentValue: '256',
      changeMethod: isZh.value
        ? '控制时间间隔矩阵的截断上限。岗位行为按天记录时可把 256 理解为较长兴趣跨度；如果只看近期求职，应缩小这个窗口。'
        : 'Controls the clipping limit of the time relation matrix. Treat 256 as a long interest window for day-level job events; shrink it for recent-only search intent.',
      whenToAdjust: isZh.value ? '近期行为更重要时降低；长期职业偏好更重要时提高。' : 'Lower for stronger recency; raise for long-term career preference.',
      basis: 'TiSASRec/main.py --time_span default=256; util.computeRePos',
    },
    {
      name: 'maxlen',
      currentValue: '50',
      changeMethod: isZh.value
        ? '和 SASRec 类似控制历史岗位长度，但 TiSASRec 还会生成 maxlen x maxlen 时间矩阵，调大后成本上升更明显。'
        : 'Like SASRec, it controls history length, but TiSASRec also builds a maxlen x maxlen time matrix, so cost rises faster.',
      whenToAdjust: isZh.value ? '需要更长行为链时增加，并同步检查延迟。' : 'Increase for longer histories and recheck latency.',
      basis: 'TiSASRec/main.py --maxlen default=50; Relation(user_train, usernum, maxlen, time_span)',
    },
    {
      name: 'hidden_units',
      currentValue: '50',
      changeMethod: isZh.value
        ? '控制 item、绝对位置和时间关系表示的宽度。时间特征加入后，先保持默认更容易定位收益。'
        : 'Controls item, absolute position, and time-relation representation width. Keep default first to isolate time-signal gains.',
      whenToAdjust: isZh.value ? '时间特征有效但容量不足时增加。' : 'Increase when time features help but capacity is insufficient.',
      basis: 'TiSASRec/main.py --hidden_units default=50',
    },
    {
      name: 'dropout_rate',
      currentValue: '0.2',
      changeMethod: isZh.value
        ? '比 SASRec 默认更低，适合先观察时间间隔信号本身是否有贡献；若验证集波动，再逐步上调。'
        : 'Lower than SASRec default, useful for first measuring the time-interval signal. Raise gradually if validation is unstable.',
      whenToAdjust: isZh.value ? '时间矩阵带来过拟合时增加。' : 'Increase when the time matrix overfits.',
      basis: 'TiSASRec/main.py --dropout_rate default=0.2',
    },
  ],
  flowTitle: isZh.value ? '时间序列接入路线' : 'Time-aware integration route',
  flow: plannedFlow('TiSASRec'),
  guardrailTitle: isZh.value ? '时间特征需要真实时间戳' : 'Real timestamps required',
  guardrail: plannedGuardrail('TiSASRec'),
  evidencePills: ['TiSASRec/main.py', 'TiSASRec/model.py', 'TiSASRec/util.py', 'time_matrix'],
}));

const fmlpRecModel = computed<ModelEntry>(() => ({
  id: 'fmlp-rec',
  mark: 'FMLP',
  accent: '#ea580c',
  visual: 'residual',
  icon: Zap,
  evidenceLevel: 'planned',
  name: {
    zh: 'FMLP-Rec',
    en: 'FMLP-Rec',
  },
  slug: 'FMLP-Rec / lightweight filter model',
  task: {
    zh: '轻量模型 / 高效推荐',
    en: 'Lightweight efficient recommendation',
  },
  status: isZh.value ? '待接入' : 'Planned',
  source: 'similar-models/FMLP-Rec',
  integration: isZh.value ? '轻量模型 / 高效推荐' : 'Lightweight / efficient recommender',
  metricStatus: isZh.value ? '暂无在线指标' : 'No online metrics yet',
  description: {
    zh: 'FMLP-Rec 用 Filter-enhanced Blocks 替代多头自注意力，定位更轻量，能和 Transformer 类模型形成差异。放进网站后可以说明“如果更关注部署效率，可以怎样换一种序列建模结构”。',
    en: 'FMLP-Rec replaces multi-head self-attention with filter-enhanced blocks. It gives the site a lightweight alternative to Transformer-style recommenders.',
  },
  profile: [
    { label: isZh.value ? '模型标识' : 'Model id', value: 'FMLP-Rec', basis: 'similar-models/FMLP-Rec/models.py' },
    { label: isZh.value ? '输入数据' : 'Input', value: 'item sequence', basis: 'FMLPRecModel.add_position_embedding' },
    { label: isZh.value ? '默认长度' : 'Default length', value: 'max_seq_length = 50', basis: 'FMLP-Rec/main.py default' },
    { label: isZh.value ? '隐藏维度' : 'Hidden size', value: '64', basis: 'FMLP-Rec/main.py default' },
    { label: isZh.value ? '滤波块数' : 'Filter blocks', value: 'num_hidden_layers = 2', basis: 'FMLP-Rec/main.py default' },
    { label: isZh.value ? '运行框架' : 'Runtime', value: 'PyTorch', basis: 'FMLP-Rec/models.py imports torch' },
  ],
  metrics: plannedMetrics('FMLP-Rec', 'similar-models/FMLP-Rec/main.py'),
  parameterMethods: [
    {
      name: 'hidden_size',
      currentValue: '64',
      changeMethod: isZh.value
        ? '控制岗位序列表示宽度。FMLP-Rec 目标是高效，建议先保持 64，再用延迟和 NDCG 共同判断是否上调。'
        : 'Controls sequence representation width. Since FMLP-Rec targets efficiency, keep 64 first and use both latency and NDCG before increasing.',
      whenToAdjust: isZh.value ? '精度不足且延迟预算充足时增加。' : 'Increase when accuracy is low and latency budget is available.',
      basis: 'FMLP-Rec/main.py --hidden_size default=64',
    },
    {
      name: 'num_hidden_layers',
      currentValue: '2',
      changeMethod: isZh.value
        ? '控制 Filter-enhanced Blocks 数量。README 示例中也提供过 4 层训练方式，可作为离线对比候选。'
        : 'Controls the number of filter-enhanced blocks. The README also shows a 4-layer run, which is a good offline comparison candidate.',
      whenToAdjust: isZh.value ? '序列模式复杂但仍想保持轻量时尝试 4。' : 'Try 4 when sequence patterns are complex but the model should stay lightweight.',
      basis: 'FMLP-Rec/main.py default=2; README example --num_hidden_layers=4',
    },
    {
      name: 'no_filters',
      currentValue: 'false',
      changeMethod: isZh.value
        ? '默认启用滤波层；加上该参数会退化到自注意力结构，可用于验证 FMLP 过滤结构是否真的带来收益。'
        : 'Filters are enabled by default. Turning this on changes blocks toward self-attention and can test whether the filter structure helps.',
      whenToAdjust: isZh.value ? '做消融实验时开启。' : 'Enable for ablation experiments.',
      basis: 'FMLP-Rec/main.py --no_filters; modules.py FilterLayer',
    },
    {
      name: 'hidden_dropout_prob',
      currentValue: '0.5',
      changeMethod: isZh.value
        ? '控制隐藏层 dropout。岗位数据稀疏时保守保持 0.5，数据量更大时可以降低以提高表达能力。'
        : 'Controls hidden dropout. Keep 0.5 for sparse job data, then lower it on larger datasets for more capacity.',
      whenToAdjust: isZh.value ? '验证集稳定且欠拟合时降低。' : 'Lower when validation is stable and underfitting appears.',
      basis: 'FMLP-Rec/main.py --hidden_dropout_prob default=0.5',
    },
  ],
  flowTitle: isZh.value ? '高效模型接入路线' : 'Efficient-model integration route',
  flow: plannedFlow('FMLP-Rec'),
  guardrailTitle: isZh.value ? '效率模型也需同口径验证' : 'Efficiency still needs fair evaluation',
  guardrail: plannedGuardrail('FMLP-Rec'),
  evidencePills: ['FMLP-Rec/main.py', 'FMLP-Rec/models.py', 'FMLP-Rec/modules.py', 'fig/model.png'],
}));

const modelCatalog = computed(() => [
  bsarecModel.value,
  sasrecModel.value,
  bert4RecModel.value,
  tisasRecModel.value,
  fmlpRecModel.value,
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
  max-width: 100%;
  color: var(--primary-color);
  font-size: 11px;
  font-weight: var(--font-weight-title);
  letter-spacing: 0.08em;
  text-transform: uppercase;
  margin-bottom: 7px;
  overflow-wrap: anywhere;
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
  overflow-wrap: anywhere;
}

.entry-copy small,
.entry-copy em,
.entry-status {
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.5;
  font-style: normal;
  overflow-wrap: anywhere;
}

.entry-status {
  grid-column: 2;
  width: fit-content;
  max-width: 100%;
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
  max-width: 100%;
  min-height: 30px;
  padding: 6px 11px;
  border: 1px solid color-mix(in srgb, var(--model-accent, var(--primary-color)) 28%, var(--border-color));
  border-radius: 999px;
  background: color-mix(in srgb, var(--model-accent, var(--primary-color)) 9%, transparent);
  color: color-mix(in srgb, var(--model-accent, var(--primary-color)) 76%, var(--text-primary));
  font-size: 12px;
  line-height: 1.45;
  overflow-wrap: anywhere;
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
  min-width: 0;
  min-height: 92px;
  padding: 14px;
  display: grid;
  align-content: start;
  gap: 6px;
  overflow: hidden;
}

.model-facts span,
.metric-tile span,
.section-heading span {
  color: var(--text-secondary);
  font-size: 12px;
}

.model-facts strong {
  display: block;
  min-width: 0;
  max-width: 100%;
  color: var(--text-primary);
  font-size: clamp(15px, 1.05vw, 18px);
  line-height: 1.25;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.model-facts small,
.metric-tile small,
.method-meta small,
.guardrail-card p {
  min-width: 0;
  color: var(--text-muted);
  font-size: 11px;
  line-height: 1.55;
  overflow-wrap: anywhere;
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
  min-width: 0;
  color: var(--text-primary);
  font-size: 16px;
  line-height: 1.35;
  overflow-wrap: anywhere;
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
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 9px;
}

.method-list code {
  max-width: 100%;
  color: color-mix(in srgb, var(--model-accent, var(--primary-color)) 72%, var(--text-primary));
  font-size: 13px;
  background: color-mix(in srgb, var(--model-accent, var(--primary-color)) 9%, transparent);
  border-radius: 8px;
  padding: 4px 8px;
  white-space: normal;
  overflow-wrap: anywhere;
}

.method-list strong {
  min-width: 0;
  color: var(--text-primary);
  font-size: 15px;
  text-align: right;
  overflow-wrap: anywhere;
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
