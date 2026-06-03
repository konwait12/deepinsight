<template>
  <div class="viz-analysis-page">
    <div class="page-header entrance-hero">
      <div>
        <span class="hierarchy-eyebrow">{{ copy.eyebrow }}</span>
        <h2>{{ $t('nav.analysis') }}</h2>
        <p>{{ copy.subtitle }}</p>
      </div>
      <div class="header-status">
        <span>{{ analysisResults.length }}</span>
        <small>{{ copy.resultCount }}</small>
      </div>
    </div>

    <section class="analysis-builder entrance-up">
      <div class="builder-main">
        <div class="section-heading">
          <span>{{ copy.analysisTarget }}</span>
          <strong>{{ copy.analysisTargetTitle }}</strong>
        </div>

        <div class="target-toolbar">
          <el-select
            v-model="selectedTargetKey"
            filterable
            :placeholder="copy.targetPlaceholder"
            class="target-select"
            popper-class="glass-select-popper"
          >
            <el-option
              v-for="target in analysisTargets"
              :key="target.key"
              :label="targetLabel(target)"
              :value="target.key"
            />
          </el-select>

          <el-button type="primary" :loading="batchLoading" @click="runAnalysis" :style="{ backgroundColor: 'var(--primary-color)' }">
            <Play :size="15" />
            {{ copy.runAnalysis }}
          </el-button>
        </div>

        <div class="target-summary" :class="{ empty: !selectedTarget }">
          <component :is="selectedTarget ? targetIcon(selectedTarget) : Database" :size="24" />
          <div>
            <strong>{{ selectedTarget ? selectedTarget.name : copy.noTarget }}</strong>
            <p>{{ selectedTarget ? targetDescription(selectedTarget) : copy.noTargetDesc }}</p>
          </div>
          <span v-if="selectedTarget">{{ selectedTarget.type }}</span>
        </div>
      </div>

      <aside class="builder-side">
        <div class="section-heading compact">
          <span>{{ copy.modules }}</span>
          <strong>{{ modulesTitle }}</strong>
        </div>
        <div class="module-grid">
          <button
            v-for="module in activeModuleCatalog"
            :key="module.key"
            type="button"
            class="module-chip"
            :class="{ active: selectedModules.includes(module.key) }"
            @click="toggleModule(module.key)"
          >
            <component :is="moduleIcon(module.key)" :size="18" />
            <span>{{ module.label }}</span>
          </button>
        </div>
      </aside>
    </section>

    <section class="analysis-status-band entrance-up" style="animation-delay: 0.06s">
      <article v-for="item in statusCards" :key="item.label">
        <component :is="item.icon" :size="20" />
        <div>
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </div>
      </article>
    </section>

    <section v-if="modelEvidenceContext" class="model-evidence-board entrance-up" style="animation-delay: 0.08s">
      <div class="section-heading">
        <span>{{ copy.modelEvidence }}</span>
        <strong>{{ modelEvidenceContext.title }}</strong>
      </div>

      <div class="evidence-overview">
        <article v-for="item in modelEvidenceContext.overview" :key="item.label">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <small>{{ item.basis }}</small>
        </article>
      </div>

      <div class="evidence-analysis-layout">
        <aside class="evidence-group-list">
          <button
            v-for="group in modelEvidenceGroups"
            :key="group.key"
            type="button"
            :class="{ active: selectedEvidenceGroupKey === group.key }"
            @click="selectedEvidenceGroupKey = group.key"
          >
            <strong>{{ group.label }}</strong>
            <span>{{ group.summary }}</span>
          </button>
        </aside>

        <div class="evidence-main">
          <div class="evidence-kpi-grid">
            <article v-for="metric in activeEvidenceMetrics" :key="metric.label">
              <div>
                <span>{{ metric.label }}</span>
                <strong>{{ metric.value }}</strong>
              </div>
              <p>{{ metric.caption }}</p>
              <div v-if="metric.percent !== undefined" class="metric-bar">
                <i :style="{ width: `${Math.min(Math.max(metric.percent, 0), 100)}%` }"></i>
              </div>
              <small>{{ metric.basis }}</small>
            </article>
          </div>

          <div class="evidence-detail-grid">
            <article class="evidence-chart-panel">
              <header>
                <div>
                  <span>{{ copy.evidenceChart }}</span>
                  <strong>{{ activeEvidenceGroup?.label }}</strong>
                </div>
                <BarChart3 :size="18" />
              </header>
              <div ref="modelEvidenceChartRef" class="evidence-chart-box"></div>
            </article>

            <article class="parameter-evidence-panel">
              <header>
                <div>
                  <span>{{ copy.parameterEvidence }}</span>
                  <strong>{{ copy.parameterEvidenceTitle }}</strong>
                </div>
                <SlidersHorizontal :size="18" />
              </header>
              <div class="parameter-evidence-list">
                <div v-for="item in activeEvidenceParameters" :key="item.name">
                  <span>{{ item.name }}</span>
                  <strong>{{ item.value }}</strong>
                  <p>{{ item.impact }}</p>
                  <small>{{ item.basis }}</small>
                </div>
              </div>
            </article>
          </div>

          <div class="evidence-source-strip">
            <span>{{ copy.evidenceSource }}</span>
            <b v-for="source in modelEvidenceContext.sources" :key="source">{{ source }}</b>
          </div>
        </div>
      </div>
    </section>

    <section class="chart-gallery entrance-up" style="animation-delay: 0.09s">
      <div class="section-heading">
        <span>{{ copy.chartGallery }}</span>
        <strong>{{ copy.chartGalleryTitle }}</strong>
      </div>

      <div v-if="analysisResults.length" class="chart-grid">
        <article class="analysis-chart-card">
          <header>
            <div>
              <span>{{ copy.statusChart }}</span>
              <strong>{{ readyCount }} / {{ analysisResults.length }}</strong>
            </div>
            <CheckCircle2 :size="18" />
          </header>
          <div ref="statusChartRef" class="chart-box"></div>
        </article>

        <article class="analysis-chart-card">
          <header>
            <div>
              <span>{{ copy.recordsChart }}</span>
              <strong>{{ totalRecords }}</strong>
            </div>
            <BarChart3 :size="18" />
          </header>
          <div ref="recordsChartRef" class="chart-box"></div>
        </article>

        <article class="analysis-chart-card">
          <header>
            <div>
              <span>{{ copy.scoreChart }}</span>
              <strong>{{ scoreAvailableCount }}</strong>
            </div>
            <LineChart :size="18" />
          </header>
          <div ref="scoreChartRef" class="chart-box"></div>
        </article>

        <article class="analysis-chart-card">
          <header>
            <div>
              <span>{{ copy.stepChart }}</span>
              <strong>{{ stepAvailableCount }}</strong>
            </div>
            <Timer :size="18" />
          </header>
          <div ref="stepChartRef" class="chart-box"></div>
        </article>

        <article class="analysis-chart-card">
          <header>
            <div>
              <span>{{ copy.coverageChart }}</span>
              <strong>{{ analysisResults.length }}</strong>
            </div>
            <TableProperties :size="18" />
          </header>
          <div ref="coverageChartRef" class="chart-box"></div>
        </article>

        <article class="analysis-chart-card">
          <header>
            <div>
              <span>{{ copy.recentChart }}</span>
              <strong>{{ recentBatches.length }}</strong>
            </div>
            <GitBranch :size="18" />
          </header>
          <div ref="recentChartRef" class="chart-box"></div>
        </article>
      </div>

      <div v-else class="chart-empty">
        <LineChart :size="30" />
        <strong>{{ copy.chartEmptyTitle }}</strong>
        <p>{{ copy.chartEmptyDesc }}</p>
      </div>
    </section>

    <section class="analysis-workbench entrance-up" style="animation-delay: 0.12s">
      <div class="result-panel">
        <div class="section-heading">
          <span>{{ copy.resultMatrix }}</span>
          <strong>{{ activeBatchTitle }}</strong>
        </div>

        <div v-if="analysisResults.length" class="result-grid">
          <article
            v-for="result in analysisResults"
            :key="result.id || result.moduleKey + result.runName"
            class="result-card"
            :class="result.status"
            @click="selectedResultId = result.id || 0"
          >
            <header>
              <span>{{ moduleLabel(result.moduleKey) }}</span>
              <strong>{{ result.status === 'ready' ? copy.ready : copy.noData }}</strong>
            </header>
            <div class="result-score">
              <b>{{ result.recordCount ?? 0 }}</b>
              <small>{{ copy.records }}</small>
            </div>
            <p>{{ result.summary }}</p>
            <div class="metric-pills">
              <span>{{ copy.latestStep }} {{ result.latestStep ?? copy.none }}</span>
              <span>{{ copy.score }} {{ formatScore(result.score) }}</span>
            </div>
          </article>
        </div>

        <div v-else class="empty-result">
          <BarChart3 :size="30" />
          <strong>{{ copy.emptyTitle }}</strong>
          <p>{{ copy.emptyDesc }}</p>
        </div>
      </div>

      <aside class="insight-panel">
        <div class="section-heading compact">
          <span>{{ copy.analysisInsight }}</span>
          <strong>{{ selectedInsightTitle }}</strong>
        </div>

        <div v-if="selectedResult" class="insight-body">
          <p>{{ selectedInsightText }}</p>
          <div class="recommendation-list">
            <article v-for="item in selectedRecommendations" :key="item">
              <CheckCircle2 :size="16" />
              <span>{{ item }}</span>
            </article>
          </div>
        </div>

        <div v-else class="insight-empty">
          {{ copy.pickResult }}
        </div>
      </aside>
    </section>

    <section class="recent-band entrance-up" style="animation-delay: 0.18s">
      <div class="section-heading">
        <span>{{ copy.recent }}</span>
        <strong>{{ copy.recentTitle }}</strong>
      </div>
      <div class="recent-list">
        <button v-for="batch in recentBatches" :key="batch.id" type="button" @click="loadBatch(batch.id)">
          <span>{{ batch.title }}</span>
          <small>{{ batch.targetCount }} × {{ batch.moduleCount }} / {{ formatTime(batch.createdAt) }}</small>
        </button>
        <div v-if="!recentBatches.length" class="recent-empty">
          {{ copy.noRecent }}
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { ElMessage } from 'element-plus';
import * as echarts from 'echarts';
import {
  Activity,
  BarChart3,
  Brain,
  BriefcaseBusiness,
  CheckCircle2,
  ClipboardList,
  Cpu,
  Database,
  FileText,
  Gauge,
  GitBranch,
  Image,
  LineChart,
  ListTree,
  Network,
  Play,
  Route,
  SlidersHorizontal,
  TableProperties,
  Timer,
  AudioWaveform,
} from 'lucide-vue-next';
import { visualizationApi } from '@/api';
import {
  buildPredictionAnalysisRecord,
  getSelectedPredictionAnalysisRecordId,
  listPredictionAnalysisRecords,
} from '@/utils/predictionAnalysis';
import type { PredictionAnalysisRecord } from '@/utils/predictionAnalysis';

type AnalysisTarget = {
  id: number
  key: string
  name: string
  type: 'training' | 'upload' | 'prediction' | 'model'
  status?: string
  architecture?: string
  modelArchitecture?: string
  predictionRecord?: PredictionAnalysisRecord
}

type AnalysisModule = {
  key: string
  label: string
  description: string
  officialModels?: string[]
}

type AnalysisPanel = {
  title?: string
  insightText?: string
  recommendations?: string[]
}

type AnalysisResult = {
  id?: number
  moduleKey: string
  runName: string
  runType: string
  modelName?: string
  status: 'ready' | 'no_data' | string
  score?: number | null
  recordCount?: number
  latestStep?: number | null
  summary?: string
  metrics?: Record<string, unknown>
  aiPanels?: AnalysisPanel[]
}

type AnalysisBatch = {
  id: number
  title: string
  targetCount?: number
  moduleCount?: number
  createdAt?: string
  results?: AnalysisResult[]
}

type EvidenceMetric = {
  label: string
  value: string
  basis: string
  caption: string
  percent?: number
  chartValue?: number
}

type EvidenceParameter = {
  name: string
  value: string
  basis: string
  impact: string
}

type EvidenceGroup = {
  key: string
  label: string
  summary: string
  metrics: EvidenceMetric[]
  parameters: EvidenceParameter[]
}

type ModelEvidenceContext = {
  title: string
  overview: EvidenceMetric[]
  sources: string[]
}

const { locale } = useI18n();
const isZh = computed(() => locale.value.startsWith('zh'));

const backendDefaultModules = ['scalars', 'hparams', 'graphs'];
const predictionDefaultModules = ['modelProfile', 'inferenceFlow', 'inputSequence', 'recommendationQuality', 'jobDistribution', 'responseHealth'];
const modelDefaultModules = ['modelProfile', 'sequenceEncoder', 'inputContract', 'servingRuntime'];

const targets = ref<AnalysisTarget[]>([]);
const predictionRecords = ref<PredictionAnalysisRecord[]>([]);
const moduleCatalog = ref<AnalysisModule[]>([]);
const recentBatches = ref<AnalysisBatch[]>([]);
const activeBatch = ref<AnalysisBatch | null>(null);
const selectedTargetKey = ref('');
const selectedModules = ref<string[]>(backendDefaultModules);
const selectedResultId = ref(0);
const batchLoading = ref(false);
const statusChartRef = ref<HTMLDivElement>();
const recordsChartRef = ref<HTMLDivElement>();
const scoreChartRef = ref<HTMLDivElement>();
const stepChartRef = ref<HTMLDivElement>();
const coverageChartRef = ref<HTMLDivElement>();
const recentChartRef = ref<HTMLDivElement>();
const modelEvidenceChartRef = ref<HTMLDivElement>();
const chartInstances = new Map<string, echarts.ECharts>();
const selectedEvidenceGroupKey = ref('ranking');

const copy = computed(() => (isZh.value ? {
  eyebrow: '可视化分析工作台',
  subtitle: '这里分析真实训练任务、上传日志和 BSARec 推理记录：既看模型本体，也看输入序列、推荐覆盖、岗位分布和服务健康。',
  resultCount: '条分析结果',
  analysisTarget: '分析对象',
  analysisTargetTitle: '选择训练运行、BSARec 模型或预测推理记录',
  targetPlaceholder: '选择要分析的数据运行',
  runAnalysis: '生成分析',
  noTarget: '没有可分析对象',
  noTargetDesc: '可以直接分析 BSARec 模型画像；预测页产生推理结果后也会出现在这里。',
  modules: '分析模块',
  modulesTitle: '按日志类型选择',
  resultMatrix: '分析矩阵',
  ready: '可分析',
  noData: '缺少数据',
  records: '记录数',
  latestStep: '最新步',
  score: '评分',
  none: '无',
  emptyTitle: '还没有生成分析',
  emptyDesc: '选择运行和模块后生成结果；如果缺日志，页面会明确显示缺哪类记录。',
  analysisInsight: '分析解读',
  pickResult: '点击左侧结果查看该模块的证据和建议。',
  recent: '最近分析',
  recentTitle: '从后端已保存批次读取',
  noRecent: '暂无历史分析批次。',
  targetTraining: '训练任务',
  targetUpload: '上传运行',
  targetPrediction: '预测推理',
  targetModel: '模型画像',
  predictionModulesTitle: '按模型推理证据选择',
  modelModulesTitle: '按模型本体结构选择',
  predictionRunSuccess: '已生成预测推理可视化分析',
  statusTargets: '对象',
  statusModules: '模块',
  statusReady: '可用结果',
  statusNoData: '缺数据结果',
  modelEvidence: '模型证据分析',
  evidenceChart: '证据图表',
  parameterEvidence: '参数佐证',
  parameterEvidenceTitle: '来源、取值与影响',
  evidenceSource: '证据来源',
  defaultBatchTitle: '待生成分析',
  moduleFallback: '分析模块',
  chartGallery: '图表分析',
  chartGalleryTitle: '由当前批次结果自动生成',
  statusChart: '结果状态分布',
  recordsChart: '模块证据量',
  scoreChart: '模块评分对比',
  stepChart: '最新步数覆盖',
  coverageChart: '可分析覆盖矩阵',
  recentChart: '历史批次规模',
  chartEmptyTitle: '暂无可绘制图表',
  chartEmptyDesc: '生成或打开一个分析批次后，图表会基于后端返回的记录数、评分、步数和状态自动刷新。',
  scoreAvailable: '有评分',
  scoreMissing: '无评分',
  readyAxis: '可分析',
  noDataAxis: '缺数据',
  recordsAxis: '记录数',
  scoreAxis: '评分',
  latestStepAxis: '最新步',
  batchTargetAxis: '对象',
  batchModuleAxis: '模块',
} : {
  eyebrow: 'Visual Analysis Workspace',
  subtitle: 'Analyze real training jobs, uploaded logs, and BSARec inference records: model profile, input sequence, recommendation coverage, job distribution, and service health.',
  resultCount: 'analysis results',
  analysisTarget: 'Target',
  analysisTargetTitle: 'Choose a training run, BSARec model, or prediction record',
  targetPlaceholder: 'Select a run to analyze',
  runAnalysis: 'Run Analysis',
  noTarget: 'No target available',
  noTargetDesc: 'You can analyze the BSARec model profile here. Prediction results sent from the inference page will also appear.',
  modules: 'Modules',
  modulesTitle: 'Choose by logged data type',
  resultMatrix: 'Analysis Matrix',
  ready: 'Ready',
  noData: 'No data',
  records: 'Records',
  latestStep: 'Step',
  score: 'Score',
  none: 'none',
  emptyTitle: 'No analysis generated yet',
  emptyDesc: 'Choose a run and modules. Missing logs are reported explicitly.',
  analysisInsight: 'Insight',
  pickResult: 'Click a result on the left to inspect evidence and recommendations.',
  recent: 'Recent',
  recentTitle: 'Loaded from saved backend batches',
  noRecent: 'No recent analysis batches.',
  targetTraining: 'Training job',
  targetUpload: 'Uploaded run',
  targetPrediction: 'Prediction inference',
  targetModel: 'Model profile',
  predictionModulesTitle: 'Choose by inference evidence',
  modelModulesTitle: 'Choose by model structure',
  predictionRunSuccess: 'Prediction inference analysis generated',
  statusTargets: 'Targets',
  statusModules: 'Modules',
  statusReady: 'Ready results',
  statusNoData: 'No-data results',
  modelEvidence: 'Model Evidence',
  evidenceChart: 'Evidence chart',
  parameterEvidence: 'Parameter evidence',
  parameterEvidenceTitle: 'Source, value, and impact',
  evidenceSource: 'Sources',
  defaultBatchTitle: 'Analysis not generated',
  moduleFallback: 'Analysis module',
  chartGallery: 'Charts',
  chartGalleryTitle: 'Generated from the active analysis batch',
  statusChart: 'Result status',
  recordsChart: 'Module evidence volume',
  scoreChart: 'Module score comparison',
  stepChart: 'Latest-step coverage',
  coverageChart: 'Analysis coverage matrix',
  recentChart: 'Recent batch scale',
  chartEmptyTitle: 'No charts to render yet',
  chartEmptyDesc: 'Generate or open an analysis batch. Charts refresh from backend record counts, scores, steps, and statuses.',
  scoreAvailable: 'scored',
  scoreMissing: 'missing score',
  readyAxis: 'Ready',
  noDataAxis: 'No data',
  recordsAxis: 'Records',
  scoreAxis: 'Score',
  latestStepAxis: 'Latest step',
  batchTargetAxis: 'Targets',
  batchModuleAxis: 'Modules',
}));

const fallbackModules = computed<AnalysisModule[]>(() => [
  { key: 'scalars', label: isZh.value ? '标量趋势' : 'Scalars', description: isZh.value ? '分析损失、准确率、学习率和收敛信号。' : 'Analyze loss, accuracy, learning rate, and convergence signals.' },
  { key: 'histograms', label: isZh.value ? '分布直方图' : 'Histograms', description: isZh.value ? '分析权重、梯度和标量分布。' : 'Analyze weights, gradients, and scalar distributions.' },
  { key: 'hparams', label: isZh.value ? '超参数对比' : 'HParams', description: isZh.value ? '比较学习率、批次大小、优化器和配置差异。' : 'Compare learning rate, batch size, optimizer, and config differences.' },
  { key: 'graphs', label: isZh.value ? '模型结构' : 'Graphs', description: isZh.value ? '分析架构、层级和拓扑关系。' : 'Analyze architecture, hierarchy, and topology metadata.' },
]);

const predictionModules = computed<AnalysisModule[]>(() => [
  { key: 'modelProfile', label: isZh.value ? '模型画像' : 'Model profile', description: isZh.value ? '分析 BSARec 的任务、结构、参数规模和运行方式。' : 'Analyze BSARec task, structure, parameter scale, and runtime.' },
  { key: 'inferenceFlow', label: isZh.value ? '推理链路' : 'Inference flow', description: isZh.value ? '分析页面、后端接口和 Flask 模型服务之间的真实链路状态。' : 'Analyze the page, backend endpoint, and Flask model service path.' },
  { key: 'inputSequence', label: isZh.value ? '输入序列' : 'Input sequence', description: isZh.value ? '分析用户历史岗位 ID 的长度、Top-K 约束和序列充足度。' : 'Analyze user history length, Top-K contract, and sequence sufficiency.' },
  { key: 'recommendationQuality', label: isZh.value ? '推荐覆盖' : 'Recommendation coverage', description: isZh.value ? '分析返回数量、Top-K 填充率、详情覆盖率和分数完整度。' : 'Analyze returned count, Top-K fill rate, detail coverage, and score coverage.' },
  { key: 'jobDistribution', label: isZh.value ? '岗位分布' : 'Job distribution', description: isZh.value ? '分析推荐岗位的公司覆盖和结果集中程度。' : 'Analyze company coverage and concentration in returned jobs.' },
  { key: 'responseHealth', label: isZh.value ? '响应健康' : 'Response health', description: isZh.value ? '分析推理延迟、服务状态和响应可解释性。' : 'Analyze latency, service status, and response explainability.' },
]);

const modelModules = computed<AnalysisModule[]>(() => [
  { key: 'modelProfile', label: isZh.value ? '模型画像' : 'Model profile', description: isZh.value ? '分析 BSARec 模型本身的任务定位、参数规模和部署方式。' : 'Analyze the BSARec model task, parameter scale, and deployment mode.' },
  { key: 'sequenceEncoder', label: isZh.value ? '序列编码机制' : 'Sequence encoder', description: isZh.value ? '分析顺序推荐模型如何使用历史岗位序列形成候选偏好。' : 'Analyze how sequential recommendation uses history to form candidate preference.' },
  { key: 'inputContract', label: isZh.value ? '输入输出契约' : 'I/O contract', description: isZh.value ? '分析 user_history、top_k 和 include_job_info 对推理结果的约束。' : 'Analyze user_history, top_k, and include_job_info constraints.' },
  { key: 'servingRuntime', label: isZh.value ? '服务运行时' : 'Serving runtime', description: isZh.value ? '分析平台后端到本地 Flask CPU 服务的部署关系。' : 'Analyze the backend-to-local Flask CPU serving relationship.' },
]);

const bsarecModelRecord = computed(() => buildPredictionAnalysisRecord({
  id: 'bsarec-model-profile',
  modelName: 'BSARec-Job',
  request: {
    user_history: [],
    top_k: 10,
    include_job_info: true,
  },
  response: {
    status: 'profile',
    message: 'BSARec model profile target',
    serviceUrl: 'http://127.0.0.1:5000/recommend',
  },
  status: 'profile',
  message: 'BSARec model profile target',
  serviceUrl: 'http://127.0.0.1:5000/recommend',
  elapsedMs: null,
  recommendations: [],
}));

function predictionTarget(record: PredictionAnalysisRecord, index: number): AnalysisTarget {
  const isModelProfile = record.id === 'bsarec-model-profile';
  return {
    id: isModelProfile ? 0 : -(index + 1),
    key: isModelProfile ? 'model:bsarec' : `prediction:${record.id}`,
    name: isModelProfile
      ? (isZh.value ? 'BSARec 模型画像' : 'BSARec model profile')
      : `${record.modelName} / ${formatTime(record.createdAt)}`,
    type: isModelProfile ? 'model' : 'prediction',
    status: record.status,
    architecture: 'BSARec sequential recommendation',
    modelArchitecture: 'BSARec sequential recommendation',
    predictionRecord: record,
  };
}

const analysisTargets = computed<AnalysisTarget[]>(() => [
  predictionTarget(bsarecModelRecord.value, -1),
  ...predictionRecords.value.map(predictionTarget),
  ...targets.value,
]);
const selectedTarget = computed(() => analysisTargets.value.find((target) => target.key === selectedTargetKey.value) || null);
const activeModuleCatalog = computed(() => {
  if (selectedTarget.value?.type === 'prediction') return predictionModules.value;
  if (selectedTarget.value?.type === 'model') return modelModules.value;
  return moduleCatalog.value;
});
const modulesTitle = computed(() => {
  if (selectedTarget.value?.type === 'prediction') return copy.value.predictionModulesTitle;
  if (selectedTarget.value?.type === 'model') return copy.value.modelModulesTitle;
  return copy.value.modulesTitle;
});
const analysisResults = computed<AnalysisResult[]>(() => activeBatch.value?.results || []);
const activeBatchTitle = computed(() => activeBatch.value?.title || copy.value.defaultBatchTitle);
const selectedResult = computed(() => analysisResults.value.find((result) => result.id === selectedResultId.value) || analysisResults.value[0] || null);
const selectedPanel = computed(() => selectedResult.value?.aiPanels?.[0] || null);
const selectedInsightTitle = computed(() => selectedPanel.value?.title || (selectedResult.value ? moduleLabel(selectedResult.value.moduleKey) : copy.value.moduleFallback));
const selectedInsightText = computed(() => selectedPanel.value?.insightText || selectedResult.value?.summary || copy.value.pickResult);
const selectedRecommendations = computed(() => selectedPanel.value?.recommendations || []);
const readyCount = computed(() => analysisResults.value.filter((result) => result.status === 'ready').length);
const noDataCount = computed(() => analysisResults.value.filter((result) => result.status !== 'ready').length);
const totalRecords = computed(() => String(analysisResults.value.reduce((sum, result) => sum + Number(result.recordCount || 0), 0)));
const scoreAvailableCount = computed(() => `${analysisResults.value.filter((result) => result.score !== null && result.score !== undefined).length}/${analysisResults.value.length}`);
const stepAvailableCount = computed(() => `${analysisResults.value.filter((result) => result.latestStep !== null && result.latestStep !== undefined).length}/${analysisResults.value.length}`);

const statusCards = computed(() => [
  { label: copy.value.statusTargets, value: String(analysisTargets.value.length), icon: Database },
  { label: copy.value.statusModules, value: String(selectedModules.value.length), icon: SlidersHorizontal },
  { label: copy.value.statusReady, value: String(readyCount.value), icon: CheckCircle2 },
  { label: copy.value.statusNoData, value: String(noDataCount.value), icon: BarChart3 },
]);

const projectRankingMetrics = computed<EvidenceMetric[]>(() => [
  {
    label: 'HR@10',
    value: '9.92%',
    percent: 9.92,
    chartValue: 9.92,
    caption: isZh.value ? 'Top-10 命中能力，衡量推荐列表是否覆盖目标岗位。' : 'Top-10 hit ability: whether the list covers the target job.',
    basis: isZh.value ? '项目已录入 BSARec 展示指标：0.0992' : 'Recorded BSARec display metric: 0.0992',
  },
  {
    label: 'NDCG@10',
    value: '4.05%',
    percent: 4.05,
    chartValue: 4.05,
    caption: isZh.value ? '考虑命中位置，越靠前命中贡献越高。' : 'Ranking-aware quality; earlier hits contribute more.',
    basis: isZh.value ? '项目已录入 BSARec 展示指标：0.0405' : 'Recorded BSARec display metric: 0.0405',
  },
  {
    label: 'Recall@10',
    value: '9.01%',
    percent: 9.01,
    chartValue: 9.01,
    caption: isZh.value ? 'Top-10 对相关岗位的覆盖能力。' : 'Top-10 relevant-job coverage.',
    basis: isZh.value ? '项目已录入 BSARec 展示指标：0.0901' : 'Recorded BSARec display metric: 0.0901',
  },
  {
    label: 'AUC',
    value: '75.15%',
    percent: 75.15,
    chartValue: 75.15,
    caption: isZh.value ? '正负样本排序区分能力。' : 'Positive versus negative ranking separability.',
    basis: isZh.value ? '项目已录入 BSARec 展示指标：0.7515' : 'Recorded BSARec display metric: 0.7515',
  },
]);

const trainingLogMetrics = computed<EvidenceMetric[]>(() => [
  {
    label: 'Total Parameters',
    value: '177,408',
    chartValue: 177.408,
    caption: isZh.value ? '模型日志记录的可训练参数量，约 0.177M。' : 'Trainable parameters recorded in the model log, about 0.177M.',
    basis: 'src/output/BSARec_Job.log: Total Parameters',
  },
  {
    label: 'Rec Loss',
    value: '6.5810 -> 6.4376',
    chartValue: 6.4376,
    caption: isZh.value ? '训练日志从 epoch 0 到 epoch 19 的推荐损失变化。' : 'Recommendation loss change from epoch 0 to epoch 19.',
    basis: 'BSARec_Job.log epoch 0..19',
  },
  {
    label: 'Test HR@10',
    value: '2.87%',
    percent: 2.87,
    chartValue: 2.87,
    caption: isZh.value ? '本地训练日志测试集 Top-10 命中率。' : 'Top-10 hit rate from the local training-log test score.',
    basis: 'BSARec_Job.log Test Score HR@10=0.0287',
  },
  {
    label: 'Test NDCG@10',
    value: '1.32%',
    percent: 1.32,
    chartValue: 1.32,
    caption: isZh.value ? '本地训练日志测试集排序质量。' : 'Ranking quality from the local training-log test score.',
    basis: 'BSARec_Job.log Test Score NDCG@10=0.0132',
  },
  {
    label: 'Early Stop',
    value: 'epoch 19',
    chartValue: 19,
    caption: isZh.value ? '连续 10 次未提升后停止，避免继续无效训练。' : 'Stopped after 10 non-improving checks.',
    basis: 'BSARec_Job.log Early stopping',
  },
]);

const dataScaleMetrics = computed<EvidenceMetric[]>(() => [
  {
    label: 'Job.txt',
    value: '15,000 行',
    chartValue: 15000,
    caption: isZh.value ? '本地序列数据文件行数，用作训练/评估样本来源。' : 'Rows in the local sequence data file used as training/evaluation source.',
    basis: 'src/data/Job.txt line count',
  },
  {
    label: 'item_size',
    value: '1,152',
    chartValue: 1152,
    caption: isZh.value ? '测试脚本和模型加载器使用的候选岗位空间规模。' : 'Candidate job-item space used by tests and the model loader.',
    basis: 'bsarec_api/test_bsarec_api.py item_size',
  },
  {
    label: 'max_seq_length',
    value: '50',
    chartValue: 50,
    caption: isZh.value ? '历史岗位序列最长保留 50 个 ID。' : 'Historical job sequence is capped at 50 IDs.',
    basis: 'bsarec_api/config.py MAX_SEQ_LENGTH',
  },
  {
    label: 'default_top_k',
    value: '10',
    chartValue: 10,
    caption: isZh.value ? '平台默认返回 Top-10，和 HR/NDCG/Recall@10 对齐。' : 'Default Top-10 response, aligned with HR/NDCG/Recall@10.',
    basis: 'bsarec_api/config.py DEFAULT_TOP_K',
  },
]);

const modelConfigMetrics = computed<EvidenceMetric[]>(() => [
  {
    label: 'hidden_size',
    value: '64',
    chartValue: 64,
    caption: isZh.value ? '序列表示和岗位 embedding 的隐藏维度。' : 'Hidden dimension for sequence and item embeddings.',
    basis: 'bsarec_api/config.py MODEL_PARAMS',
  },
  {
    label: 'batch_size',
    value: '256',
    chartValue: 256,
    caption: isZh.value ? '训练批量大小，影响收敛稳定性和单步吞吐。' : 'Training batch size; affects stability and throughput.',
    basis: 'bsarec_api/config.py / src/utils.py',
  },
  {
    label: 'layers',
    value: '2',
    chartValue: 2,
    caption: isZh.value ? 'BSARec encoder block 数。' : 'Number of BSARec encoder blocks.',
    basis: 'num_hidden_layers = 2',
  },
  {
    label: 'heads',
    value: '2',
    chartValue: 2,
    caption: isZh.value ? '多头注意力头数，64 维下每头 32 维。' : 'Attention heads; 64 hidden dims give 32 dims per head.',
    basis: 'num_attention_heads = 2',
  },
  {
    label: 'dropout',
    value: '0.5',
    chartValue: 0.5,
    caption: isZh.value ? '隐藏层和注意力 dropout，用于抑制过拟合。' : 'Hidden and attention dropout for regularization.',
    basis: 'hidden_dropout_prob / attention_probs_dropout_prob',
  },
  {
    label: 'lr',
    value: '0.001',
    chartValue: 0.001,
    caption: isZh.value ? '启动命令指定的 Adam 学习率。' : 'Adam learning rate from the launch command.',
    basis: '启动命令.txt --lr 0.001',
  },
  {
    label: 'alpha / c',
    value: '0.7 / 5',
    chartValue: 0.7,
    caption: isZh.value ? 'BSARec 频域/全局信号组合相关参数。' : 'BSARec frequency/global signal combination parameters.',
    basis: 'bsarec_api/config.py alpha=0.7, c=5',
  },
]);

function predictionEvidenceMetrics(record: PredictionAnalysisRecord): EvidenceMetric[] {
  return [
    {
      label: isZh.value ? '历史长度' : 'History length',
      value: String(record.metrics.historyLength),
      chartValue: record.metrics.historyLength,
      caption: isZh.value ? '本次推理输入的历史岗位 ID 数量。' : 'Number of historical job IDs used by this inference.',
      basis: 'request.user_history',
    },
    {
      label: isZh.value ? '返回覆盖' : 'Top-K fill',
      value: percentLabel(record.metrics.fillRate),
      percent: record.metrics.fillRate * 100,
      chartValue: record.metrics.fillRate * 100,
      caption: isZh.value ? '返回数量 / 请求 Top-K，衡量候选是否补齐。' : 'Returned count divided by requested Top-K.',
      basis: `${record.metrics.recommendationCount}/${record.metrics.requestedTopK}`,
    },
    {
      label: isZh.value ? '详情覆盖' : 'Detail coverage',
      value: percentLabel(record.metrics.detailCoverage),
      percent: record.metrics.detailCoverage * 100,
      chartValue: record.metrics.detailCoverage * 100,
      caption: isZh.value ? '岗位名称、公司、薪资等可读字段覆盖率。' : 'Coverage of readable fields such as title, company, and salary.',
      basis: 'recommendations.job_info',
    },
    {
      label: isZh.value ? '分数覆盖' : 'Score coverage',
      value: percentLabel(record.metrics.scoreCoverage),
      percent: record.metrics.scoreCoverage * 100,
      chartValue: record.metrics.scoreCoverage * 100,
      caption: isZh.value ? '返回项中携带 score/confidence 的比例。' : 'Share of returned items carrying score/confidence.',
      basis: 'recommendations.score',
    },
    {
      label: isZh.value ? '公司覆盖' : 'Company coverage',
      value: `${record.metrics.uniqueCompanyCount}/${record.metrics.recommendationCount || 0}`,
      chartValue: record.metrics.uniqueCompanyCount,
      caption: isZh.value ? '公司/来源去重数，用于观察推荐是否同质化。' : 'Unique company/source count for diversity inspection.',
      basis: 'recommendations.company',
    },
    {
      label: isZh.value ? '推理延迟' : 'Latency',
      value: record.elapsedMs === null ? '--' : `${record.elapsedMs}ms`,
      chartValue: record.elapsedMs ?? 0,
      caption: isZh.value ? '从页面发起请求到后端响应的耗时。' : 'Elapsed time from page request to backend response.',
      basis: `latencyBucket=${record.metrics.latencyBucket}`,
    },
  ];
}

const modelEvidenceContext = computed<ModelEvidenceContext | null>(() => {
  const target = selectedTarget.value;
  if (!target || (target.type !== 'model' && target.type !== 'prediction')) return null;
  const record = target.predictionRecord || bsarecModelRecord.value;
  const isPredictionTarget = target.type === 'prediction';
  const overview: EvidenceMetric[] = isPredictionTarget
    ? [
        {
          label: isZh.value ? '推理状态' : 'Inference status',
          value: record.status,
          chartValue: serviceScore(record),
          caption: isZh.value ? '后端返回的推理状态。' : 'Inference status returned by backend.',
          basis: 'response.status',
        },
        {
          label: isZh.value ? 'Top-K 返回率' : 'Top-K fill',
          value: percentLabel(record.metrics.fillRate),
          percent: record.metrics.fillRate * 100,
          chartValue: record.metrics.fillRate * 100,
          caption: isZh.value ? '本次返回数量和请求 Top-K 的比例。' : 'Returned count against requested Top-K.',
          basis: `${record.metrics.recommendationCount}/${record.metrics.requestedTopK}`,
        },
        {
          label: isZh.value ? '详情覆盖率' : 'Detail coverage',
          value: percentLabel(record.metrics.detailCoverage),
          percent: record.metrics.detailCoverage * 100,
          chartValue: record.metrics.detailCoverage * 100,
          caption: isZh.value ? '本次岗位详情字段覆盖。' : 'Readable detail coverage for returned jobs.',
          basis: 'recommendations.job_info',
        },
        {
          label: isZh.value ? '延迟' : 'Latency',
          value: record.elapsedMs === null ? '--' : `${record.elapsedMs}ms`,
          chartValue: record.elapsedMs ?? 0,
          caption: isZh.value ? '本次推理请求耗时。' : 'Elapsed time for this inference request.',
          basis: record.serviceUrl || 'serviceUrl',
        },
      ]
    : [
        {
          label: isZh.value ? '参数量' : 'Parameters',
          value: '177,408',
          chartValue: 177.408,
          caption: isZh.value ? 'BSARec_Job.log 记录的模型参数量。' : 'Parameter count recorded in BSARec_Job.log.',
          basis: 'Total Parameters',
        },
        {
          label: 'HR@10',
          value: '9.92%',
          percent: 9.92,
          chartValue: 9.92,
          caption: isZh.value ? '项目已录入展示指标。' : 'Recorded project display metric.',
          basis: '0.0992',
        },
        {
          label: isZh.value ? '序列长度' : 'Sequence limit',
          value: '50',
          chartValue: 50,
          caption: isZh.value ? '输入历史序列最大长度。' : 'Maximum input history length.',
          basis: 'MAX_SEQ_LENGTH',
        },
        {
          label: isZh.value ? '数据行数' : 'Data rows',
          value: '15,000',
          chartValue: 15000,
          caption: isZh.value ? 'Job.txt 本地序列数据行数。' : 'Rows in local Job.txt sequence data.',
          basis: 'src/data/Job.txt',
        },
      ];

  return {
    title: isPredictionTarget
      ? (isZh.value ? 'BSARec 推理记录证据面板' : 'BSARec inference evidence panel')
      : (isZh.value ? 'BSARec 模型本体与评估证据' : 'BSARec model profile and evaluation evidence'),
    overview,
    sources: isPredictionTarget
      ? ['predictionAnalysis localStorage', 'PredictionController', 'BSARecClientService', 'bsarec_api/config.py']
      : ['src/output/BSARec_Job.log', 'bsarec_api/config.py', 'src/data/Job.txt', 'src/views/training/index.vue'],
  };
});

const modelEvidenceGroups = computed<EvidenceGroup[]>(() => {
  const record = selectedTarget.value?.predictionRecord || bsarecModelRecord.value;
  const inferenceMetrics = selectedTarget.value?.type === 'prediction'
    ? predictionEvidenceMetrics(record)
    : [
        {
          label: isZh.value ? '平台入口' : 'Platform endpoint',
          value: '/api/v1/prediction/recommend',
          chartValue: 1,
          caption: isZh.value ? '前端推荐请求走后端代理接口。' : 'Frontend requests go through the backend recommendation endpoint.',
          basis: 'predictionApi.recommend',
        },
        {
          label: isZh.value ? '模型服务' : 'Model service',
          value: '127.0.0.1:5000',
          chartValue: 1,
          caption: isZh.value ? '后端再调用本地 Flask BSARec 服务。' : 'Backend then calls the local Flask BSARec service.',
          basis: 'BSARecClientService base URL',
        },
        {
          label: 'top_k',
          value: '10',
          chartValue: 10,
          caption: isZh.value ? '默认返回 Top-10。' : 'Default Top-10 return size.',
          basis: 'DEFAULT_TOP_K',
        },
        {
          label: 'include_job_info',
          value: 'true',
          chartValue: 1,
          caption: isZh.value ? '默认返回岗位详情，便于可视化岗位分布。' : 'Returns job details by default for visual distribution analysis.',
          basis: 'prediction page default',
        },
      ];

  return [
    {
      key: 'ranking',
      label: isZh.value ? '推荐效果' : 'Ranking quality',
      summary: isZh.value ? 'HR/NDCG/Recall/AUC' : 'HR/NDCG/Recall/AUC',
      metrics: projectRankingMetrics.value,
      parameters: [
        { name: 'top_k', value: '10', basis: 'recorded metrics are @10', impact: isZh.value ? 'HR、NDCG、Recall 均按 Top-10 统计，改动 Top-K 后需要重新做 @K 评估。' : 'HR, NDCG, and Recall are computed at Top-10; changing Top-K needs new @K evaluation.' },
        { name: 'AUC', value: '0.7515', basis: 'project display metric', impact: isZh.value ? 'AUC 更关注整体排序区分能力，不直接表示 Top-K 命中。' : 'AUC reflects overall separability, not direct Top-K hit quality.' },
        { name: '展示指标来源', value: isZh.value ? '平台录入' : 'platform record', basis: 'src/views/training/index.vue', impact: isZh.value ? '和训练日志测试分数分开呈现，避免把不同来源的指标混成一个实验。' : 'Kept separate from training-log test scores to avoid mixing sources.' },
      ],
    },
    {
      key: 'trainingLog',
      label: isZh.value ? '训练日志' : 'Training log',
      summary: isZh.value ? '参数量、loss、early stop' : 'Params, loss, early stop',
      metrics: trainingLogMetrics.value,
      parameters: [
        { name: 'Total Parameters', value: '177,408', basis: 'BSARec_Job.log', impact: isZh.value ? '约 0.177M 参数，和平台注册 0.2M 参数规模一致。' : 'About 0.177M parameters, consistent with the registered 0.2M scale.' },
        { name: 'EarlyStopping', value: '10 checks', basis: 'BSARec_Job.log', impact: isZh.value ? '验证指标多轮未提升后停止，说明后段收益有限。' : 'Stops after repeated non-improvement, indicating limited late-stage gains.' },
        { name: 'Test Score', value: 'HR@10=0.0287', basis: 'BSARec_Job.log', impact: isZh.value ? '本地日志测试分数作为训练复盘证据，和展示指标并列而不互相覆盖。' : 'Local test score is training-review evidence and is shown beside display metrics.' },
      ],
    },
    {
      key: 'dataScale',
      label: isZh.value ? '数据规模' : 'Data scale',
      summary: isZh.value ? 'Job.txt、item_size、序列上限' : 'Job.txt, item_size, sequence cap',
      metrics: dataScaleMetrics.value,
      parameters: [
        { name: 'Job.txt', value: '15,000 lines', basis: 'src/data/Job.txt', impact: isZh.value ? '数据体量决定训练和评估覆盖面，后续扩展岗位数据时应同步重训。' : 'Data volume controls training/evaluation coverage; retrain after expanding job data.' },
        { name: 'item_size', value: '1,152', basis: 'test_bsarec_api.py', impact: isZh.value ? '候选空间影响推荐难度，也影响输出层参数和 Top-K 搜索范围。' : 'Candidate space affects recommendation difficulty, output parameters, and Top-K search.' },
        { name: 'max_seq_length', value: '50', basis: 'data_processor.validate_user_history', impact: isZh.value ? '超过 50 的历史只保留最近记录，输入顺序和最近性很重要。' : 'Histories longer than 50 keep the newest IDs, so recency and order matter.' },
      ],
    },
    {
      key: 'trainingConfig',
      label: isZh.value ? '训练配置' : 'Training config',
      summary: isZh.value ? 'hidden、batch、dropout、lr' : 'hidden, batch, dropout, lr',
      metrics: modelConfigMetrics.value,
      parameters: [
        { name: 'hidden_size', value: '64', basis: 'MODEL_PARAMS', impact: isZh.value ? '决定岗位 embedding 和序列表征容量。' : 'Controls item embedding and sequence representation capacity.' },
        { name: 'num_hidden_layers / heads', value: '2 / 2', basis: 'MODEL_PARAMS', impact: isZh.value ? '两层两头是轻量设置，适合 CPU 推理，但表达能力有限。' : 'A light two-layer/two-head setup fits CPU serving but limits capacity.' },
        { name: 'dropout', value: '0.5', basis: 'MODEL_PARAMS', impact: isZh.value ? '较强正则化，适合抑制小数据过拟合，也可能降低拟合速度。' : 'Strong regularization; helps overfitting but can slow fitting.' },
        { name: 'lr', value: '0.001', basis: '启动命令.txt', impact: isZh.value ? '学习率来自实际启动命令，是复现实验的关键参数。' : 'Learning rate comes from the actual launch command and matters for reproduction.' },
      ],
    },
    {
      key: 'inference',
      label: selectedTarget.value?.type === 'prediction' ? (isZh.value ? '本次推理' : 'This inference') : (isZh.value ? '推理链路' : 'Inference path'),
      summary: selectedTarget.value?.type === 'prediction'
        ? (isZh.value ? '返回率、详情、延迟' : 'Fill, detail, latency')
        : (isZh.value ? '接口、服务、输入契约' : 'Endpoint, service, contract'),
      metrics: inferenceMetrics,
      parameters: selectedTarget.value?.type === 'prediction'
        ? [
            { name: 'request.user_history', value: `${record.metrics.historyLength}`, basis: 'predictionAnalysis record', impact: isZh.value ? '历史越短，序列偏好证据越弱。' : 'Shorter histories carry weaker preference evidence.' },
            { name: 'recommendations', value: `${record.metrics.recommendationCount}/${record.metrics.requestedTopK}`, basis: 'predictionAnalysis metrics', impact: isZh.value ? '返回不足时优先检查 Flask 响应格式和候选空间。' : 'When underfilled, inspect Flask response format and candidate space.' },
            { name: 'latency', value: record.elapsedMs === null ? '--' : `${record.elapsedMs}ms`, basis: record.serviceUrl || 'serviceUrl', impact: isZh.value ? '延迟用于判断是否适合交互式页面。' : 'Latency indicates whether the service fits an interactive page.' },
          ]
        : [
            { name: 'request body', value: 'user_history/top_k/include_job_info', basis: 'PredictionController', impact: isZh.value ? '这三个字段决定输入序列、返回数量和详情可视化能力。' : 'These fields control sequence evidence, return count, and visual detail coverage.' },
            { name: 'Flask API', value: '/recommend', basis: 'bsarec_api/app.py', impact: isZh.value ? '本地服务不可用时，预测页会保留原始响应用于排查。' : 'When the local service is unavailable, the prediction page keeps raw response evidence.' },
            { name: 'item_size', value: 'optional', basis: 'BSARecClientService pass-through', impact: isZh.value ? '只有模型服务版本要求时才传入，避免错误限制候选空间。' : 'Pass only when required by the model-service version to avoid wrong candidate limits.' },
          ],
    },
  ];
});

const activeEvidenceGroup = computed(() => modelEvidenceGroups.value.find((group) => group.key === selectedEvidenceGroupKey.value) || modelEvidenceGroups.value[0] || null);
const activeEvidenceMetrics = computed(() => activeEvidenceGroup.value?.metrics || []);
const activeEvidenceParameters = computed(() => activeEvidenceGroup.value?.parameters || []);

function chartTextColor() {
  if (typeof window === 'undefined') return '#64748b';
  return getComputedStyle(document.documentElement).getPropertyValue('--text-secondary').trim() || '#64748b';
}

function chartPrimaryTextColor() {
  if (typeof window === 'undefined') return '#0f172a';
  return getComputedStyle(document.documentElement).getPropertyValue('--text-primary').trim() || '#0f172a';
}

function chartBorderColor() {
  if (typeof window === 'undefined') return '#e2e8f0';
  return getComputedStyle(document.documentElement).getPropertyValue('--border-color').trim() || '#e2e8f0';
}

function moduleAxisLabel(result: AnalysisResult, index: number) {
  return `${index + 1}. ${moduleLabel(result.moduleKey)}`;
}

function shortAxisLabel(value: string) {
  return value.length > 12 ? `${value.slice(0, 11)}…` : value;
}

function getChart(key: string, el?: HTMLDivElement) {
  if (!el) return null;
  const existing = chartInstances.get(key);
  if (existing) return existing;
  const instance = echarts.init(el);
  chartInstances.set(key, instance);
  return instance;
}

function baseGrid() {
  return {
    left: 44,
    right: 20,
    top: 28,
    bottom: 54,
  };
}

function baseAxis() {
  return {
    axisLine: { lineStyle: { color: chartBorderColor() } },
    axisTick: { lineStyle: { color: chartBorderColor() } },
    axisLabel: { color: chartTextColor(), fontSize: 11 },
    splitLine: { lineStyle: { color: chartBorderColor(), opacity: 0.42 } },
  };
}

function disposeCharts() {
  chartInstances.forEach((instance) => instance.dispose());
  chartInstances.clear();
}

function disposeChart(key: string) {
  chartInstances.get(key)?.dispose();
  chartInstances.delete(key);
}

function disposeResultCharts() {
  ['status', 'records', 'score', 'step', 'coverage', 'recent'].forEach(disposeChart);
}

function resizeCharts() {
  chartInstances.forEach((instance) => instance.resize());
}

async function renderModelEvidenceChart() {
  if (!modelEvidenceContext.value || !activeEvidenceGroup.value) {
    disposeChart('modelEvidence');
    return;
  }

  await nextTick();
  const chart = getChart('modelEvidence', modelEvidenceChartRef.value);
  if (!chart) return;

  const metrics = activeEvidenceMetrics.value;
  const textColor = chartTextColor();
  const titleColor = chartPrimaryTextColor();
  const axis = baseAxis();
  const values = metrics.map((metric) => (metric.chartValue ?? metric.percent ?? Number.parseFloat(metric.value)) || 0);
  const max = Math.max(...values, 1);

  chart.setOption({
    color: ['#22c55e', '#38bdf8', '#f97316', '#a855f7', '#14b8a6', '#64748b'],
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params: any) => {
        const item = Array.isArray(params) ? params[0] : params;
        const metric = metrics[item.dataIndex];
        return `${metric.label}<br/>${metric.value}<br/>${metric.basis}`;
      },
    },
    grid: { left: 54, right: 20, top: 28, bottom: 56 },
    xAxis: {
      type: 'category',
      data: metrics.map((metric) => metric.label),
      ...axis,
      axisLabel: {
        ...axis.axisLabel,
        interval: 0,
        rotate: metrics.length > 4 ? 22 : 0,
        formatter: shortAxisLabel,
      },
    },
    yAxis: {
      type: 'value',
      max: max <= 100 ? Math.max(100, Math.ceil(max * 1.2)) : undefined,
      ...axis,
    },
    series: [{
      type: 'bar',
      barMaxWidth: 34,
      data: values.map((value, index) => ({
        value,
        itemStyle: {
          borderRadius: [8, 8, 0, 0],
        },
        label: {
          show: true,
          position: 'top',
          color: titleColor,
          fontSize: 11,
          formatter: () => metrics[index].value,
        },
      })),
    }],
    graphic: metrics.length
      ? []
      : [{
          type: 'text',
          left: 'center',
          top: 'middle',
          style: { text: copy.value.noData, fill: textColor, fontSize: 13 },
        }],
  }, true);
}

async function renderCharts() {
  if (!analysisResults.value.length) {
    if (modelEvidenceContext.value) {
      disposeResultCharts();
      await renderModelEvidenceChart();
    } else {
      disposeCharts();
    }
    return;
  }

  await nextTick();
  await renderModelEvidenceChart();

  const results = analysisResults.value;
  const labels = results.map(moduleAxisLabel);
  const textColor = chartTextColor();
  const titleColor = chartPrimaryTextColor();
  const grid = baseGrid();
  const axis = baseAxis();
  const readyColor = '#22c55e';
  const missingColor = '#f59e0b';
  const recordColor = '#38bdf8';
  const scoreColor = '#a855f7';
  const stepColor = '#14b8a6';
  const mutedColor = '#94a3b8';

  getChart('status', statusChartRef.value)?.setOption({
    color: [readyColor, missingColor],
    tooltip: { trigger: 'item' },
    legend: {
      bottom: 0,
      textStyle: { color: textColor },
    },
    series: [{
      type: 'pie',
      radius: ['52%', '74%'],
      center: ['50%', '43%'],
      avoidLabelOverlap: true,
      label: {
        color: textColor,
        formatter: '{b}\n{c}',
      },
      data: [
        { name: copy.value.ready, value: readyCount.value },
        { name: copy.value.noData, value: noDataCount.value },
      ],
    }],
  }, true);

  getChart('records', recordsChartRef.value)?.setOption({
    color: [recordColor],
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const item = Array.isArray(params) ? params[0] : params;
        const result = results[item.dataIndex];
        return `${moduleLabel(result.moduleKey)}<br/>${copy.value.recordsAxis}: ${result.recordCount ?? 0}<br/>${copy.value.readyAxis}: ${result.status === 'ready' ? copy.value.ready : copy.value.noData}`;
      },
    },
    grid,
    xAxis: {
      type: 'category',
      data: labels,
      ...axis,
      axisLabel: {
        ...axis.axisLabel,
        interval: 0,
        rotate: labels.length > 4 ? 24 : 0,
        formatter: shortAxisLabel,
      },
    },
    yAxis: {
      type: 'value',
      name: copy.value.recordsAxis,
      nameTextStyle: { color: textColor },
      minInterval: 1,
      ...axis,
    },
    series: [{
      type: 'bar',
      barMaxWidth: 36,
      data: results.map((result) => ({
        value: result.recordCount ?? 0,
        itemStyle: { color: result.status === 'ready' ? recordColor : missingColor },
      })),
    }],
  }, true);

  const numericScores = results
    .map((result) => result.score)
    .filter((value): value is number => value !== null && value !== undefined);
  const maxScore = numericScores.length ? Math.max(...numericScores) : 1;

  getChart('score', scoreChartRef.value)?.setOption({
    color: [scoreColor, missingColor],
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const item = Array.isArray(params) ? params[0] : params;
        const result = results[item.dataIndex];
        return `${moduleLabel(result.moduleKey)}<br/>${copy.value.scoreAxis}: ${formatScore(result.score)}<br/>${copy.value.recordsAxis}: ${result.recordCount ?? 0}`;
      },
    },
    legend: {
      top: 0,
      right: 0,
      textStyle: { color: textColor },
    },
    grid: { ...grid, top: 42 },
    xAxis: {
      type: 'category',
      data: labels,
      ...axis,
      axisLabel: {
        ...axis.axisLabel,
        interval: 0,
        rotate: labels.length > 4 ? 24 : 0,
        formatter: shortAxisLabel,
      },
    },
    yAxis: {
      type: 'value',
      name: copy.value.scoreAxis,
      nameTextStyle: { color: textColor },
      min: 0,
      max: maxScore <= 1 ? 1 : undefined,
      ...axis,
    },
    series: [
      {
        name: copy.value.scoreAvailable,
        type: 'line',
        smooth: true,
        symbolSize: 9,
        connectNulls: false,
        data: results.map((result) => result.score ?? null),
        lineStyle: { width: 3 },
      },
      {
        name: copy.value.scoreMissing,
        type: 'scatter',
        symbol: 'diamond',
        symbolSize: 10,
        data: results.map((result) => (result.score === null || result.score === undefined ? 0 : null)),
      },
    ],
  }, true);

  getChart('step', stepChartRef.value)?.setOption({
    color: [stepColor],
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const item = Array.isArray(params) ? params[0] : params;
        const result = results[item.dataIndex];
        return `${moduleLabel(result.moduleKey)}<br/>${copy.value.latestStepAxis}: ${result.latestStep ?? copy.value.none}<br/>${copy.value.recordsAxis}: ${result.recordCount ?? 0}`;
      },
    },
    grid: { left: 104, right: 28, top: 22, bottom: 28 },
    xAxis: {
      type: 'value',
      name: copy.value.latestStepAxis,
      nameTextStyle: { color: textColor },
      minInterval: 1,
      ...axis,
    },
    yAxis: {
      type: 'category',
      data: labels,
      inverse: true,
      ...axis,
      axisLabel: {
        ...axis.axisLabel,
        formatter: shortAxisLabel,
      },
    },
    series: [{
      type: 'bar',
      barMaxWidth: 22,
      label: {
        show: true,
        position: 'right',
        color: titleColor,
        formatter: (params: any) => results[params.dataIndex].latestStep ?? copy.value.none,
      },
      data: results.map((result) => ({
        value: result.latestStep ?? 0,
        itemStyle: { color: result.latestStep === null || result.latestStep === undefined ? mutedColor : stepColor },
      })),
    }],
  }, true);

  const coverageRows = [copy.value.readyAxis, copy.value.recordsAxis, copy.value.scoreAxis, copy.value.latestStepAxis];
  const coverageData = results.flatMap((result, xIndex) => [
    [xIndex, 0, result.status === 'ready' ? 1 : 0],
    [xIndex, 1, Number(result.recordCount || 0) > 0 ? 1 : 0],
    [xIndex, 2, result.score !== null && result.score !== undefined ? 1 : 0],
    [xIndex, 3, result.latestStep !== null && result.latestStep !== undefined ? 1 : 0],
  ]);

  getChart('coverage', coverageChartRef.value)?.setOption({
    tooltip: {
      position: 'top',
      formatter: (params: any) => {
        const [xIndex, yIndex, value] = params.value;
        const result = results[xIndex];
        return `${moduleLabel(result.moduleKey)}<br/>${coverageRows[yIndex]}: ${value ? copy.value.ready : copy.value.noData}`;
      },
    },
    grid: { left: 82, right: 16, top: 24, bottom: 54 },
    xAxis: {
      type: 'category',
      data: labels,
      ...axis,
      axisLabel: {
        ...axis.axisLabel,
        interval: 0,
        rotate: labels.length > 4 ? 24 : 0,
        formatter: shortAxisLabel,
      },
    },
    yAxis: {
      type: 'category',
      data: coverageRows,
      ...axis,
    },
    visualMap: {
      show: false,
      min: 0,
      max: 1,
      inRange: { color: ['rgba(148, 163, 184, 0.18)', stepColor] },
    },
    series: [{
      type: 'heatmap',
      data: coverageData,
      label: {
        show: true,
        color: titleColor,
        formatter: (params: any) => (params.value[2] ? '✓' : ''),
      },
      itemStyle: {
        borderColor: chartBorderColor(),
        borderWidth: 1,
      },
    }],
  }, true);

  const recent = recentBatches.value.slice(0, 7).reverse();
  const recentChart = getChart('recent', recentChartRef.value);
  if (recentChart && !recent.length) {
    recentChart.setOption({
      title: {
        text: copy.value.noRecent,
        left: 'center',
        top: 'middle',
        textStyle: { color: textColor, fontSize: 13, fontWeight: 400 },
      },
    }, true);
  } else {
    recentChart?.setOption({
      color: ['#f97316', '#0ea5e9'],
      tooltip: { trigger: 'axis' },
      legend: {
        top: 0,
        right: 0,
        textStyle: { color: textColor },
      },
      grid: { ...grid, top: 42 },
      xAxis: {
        type: 'category',
        data: recent.map((batch) => formatTime(batch.createdAt)),
        ...axis,
        axisLabel: {
          ...axis.axisLabel,
          interval: 0,
          rotate: recent.length > 3 ? 20 : 0,
        },
      },
      yAxis: {
        type: 'value',
        minInterval: 1,
        ...axis,
      },
      series: [
        {
          name: copy.value.batchTargetAxis,
          type: 'bar',
          barMaxWidth: 24,
          data: recent.map((batch) => batch.targetCount ?? 0),
        },
        {
          name: copy.value.batchModuleAxis,
          type: 'line',
          smooth: true,
          symbolSize: 8,
          data: recent.map((batch) => batch.moduleCount ?? 0),
        },
      ],
    }, true);
  }
}

function moduleLabel(key: string) {
  return [
    ...activeModuleCatalog.value,
    ...predictionModules.value,
    ...modelModules.value,
    ...moduleCatalog.value,
    ...fallbackModules.value,
  ].find((module) => module.key === key)?.label || key;
}

function moduleIcon(key: string) {
  const icons: Record<string, unknown> = {
    scalars: LineChart,
    images: Image,
    audio: AudioWaveform,
    text: FileText,
    histograms: BarChart3,
    embeddings: ListTree,
    prCurves: GitBranch,
    hparams: SlidersHorizontal,
    graphs: TableProperties,
    profiler: Timer,
    modelProfile: Brain,
    sequenceEncoder: Network,
    inputContract: ClipboardList,
    servingRuntime: Cpu,
    inferenceFlow: Route,
    inputSequence: ListTree,
    recommendationQuality: Gauge,
    jobDistribution: BriefcaseBusiness,
    responseHealth: Activity,
  };
  return icons[key] || BarChart3;
}

function targetIcon(target: AnalysisTarget) {
  if (target.type === 'model') return Brain;
  if (target.type === 'prediction') return BriefcaseBusiness;
  return target.type === 'training' ? LineChart : Database;
}

function targetLabel(target: AnalysisTarget) {
  const type = target.type === 'training'
    ? copy.value.targetTraining
    : target.type === 'upload'
      ? copy.value.targetUpload
      : target.type === 'model'
        ? copy.value.targetModel
        : copy.value.targetPrediction;
  const model = target.architecture || target.modelArchitecture;
  return model && target.type !== 'prediction' && target.type !== 'model'
    ? `${type} · ${target.name} · ${model}`
    : `${type} · ${target.name}`;
}

function targetDescription(target: AnalysisTarget) {
  if (target.type === 'model') {
    return isZh.value
      ? 'BSARec 顺序推荐模型，CPU / Flask 本地服务，用 user_history 生成 Top-K 岗位候选。'
      : 'BSARec sequential recommendation model served by local CPU / Flask, using user_history to generate Top-K job candidates.';
  }
  if (target.type === 'prediction' && target.predictionRecord) {
    const record = target.predictionRecord;
    return isZh.value
      ? `模型：${record.modelName}；历史长度：${record.metrics.historyLength}；返回：${record.metrics.recommendationCount}/${record.metrics.requestedTopK}；延迟：${record.elapsedMs ?? '--'}ms；服务：${record.serviceUrl || '未标注'}。`
      : `Model: ${record.modelName}; history length: ${record.metrics.historyLength}; returned: ${record.metrics.recommendationCount}/${record.metrics.requestedTopK}; latency: ${record.elapsedMs ?? '--'}ms; service: ${record.serviceUrl || 'not labeled'}.`;
  }
  const model = target.architecture || target.modelArchitecture || (isZh.value ? '未标注模型' : 'model not labeled');
  const status = target.status || (isZh.value ? '未知状态' : 'unknown status');
  return isZh.value
    ? `类型：${target.type === 'training' ? '训练任务' : '上传运行'}；模型/架构：${model}；状态：${status}。`
    : `Type: ${target.type}; model/architecture: ${model}; status: ${status}.`;
}

function toggleModule(key: string) {
  if (selectedModules.value.includes(key)) {
    selectedModules.value = selectedModules.value.filter((item) => item !== key);
    return;
  }
  selectedModules.value = [...selectedModules.value, key];
}

function formatScore(score?: number | null) {
  if (score === null || score === undefined) return copy.value.none;
  return Number(score).toFixed(3);
}

function formatTime(value?: string) {
  if (!value) return copy.value.none;
  return String(value).replace('T', ' ').slice(0, 16);
}

function percentLabel(value: number) {
  return `${Math.round(value * 100)}%`;
}

function latencyScore(record: PredictionAnalysisRecord) {
  if (record.elapsedMs === null || record.elapsedMs === undefined) return null;
  if (record.elapsedMs <= 500) return 1;
  if (record.elapsedMs <= 1500) return 0.82;
  if (record.elapsedMs <= 3000) return 0.58;
  return 0.35;
}

function serviceScore(record: PredictionAnalysisRecord) {
  if (record.status === 'success') return 1;
  if (record.status === 'offline') return 0.35;
  if (record.status === 'error') return 0.2;
  return 0.8;
}

function resultPanel(title: string, insightText: string, recommendations: string[]): AnalysisPanel[] {
  return [{ title, insightText, recommendations }];
}

function localAnalysisResult(
  moduleKey: string,
  record: PredictionAnalysisRecord,
  index: number,
  runType: 'prediction' | 'model',
): AnalysisResult {
  const metrics = record.metrics;
  const runName = runType === 'model' ? 'BSARec model profile' : `${record.modelName} ${formatTime(record.createdAt)}`;
  const base = {
    id: index + 1,
    moduleKey,
    runName,
    runType,
    modelName: record.modelName,
  };

  if (moduleKey === 'modelProfile') {
    const summary = isZh.value
      ? 'BSARec 是面向岗位序列的推荐模型：输入历史岗位 ID，输出 Top-K 候选岗位；当前平台以 CPU / Flask 服务接入真实推理接口。'
      : 'BSARec is a job-sequence recommendation model: it consumes historical job IDs and returns Top-K candidate jobs through the local CPU / Flask inference service.';
    return {
      ...base,
      status: 'ready',
      score: 1,
      recordCount: 4,
      latestStep: null,
      summary,
      metrics: { task: 'sequential recommendation', paramsM: 0.2, framework: 'PyTorch / CPU', endpoint: record.serviceUrl },
      aiPanels: resultPanel(moduleLabel(moduleKey), summary, isZh.value
        ? ['重点查看输入序列是否足够长。', 'Top-K 与详情开关会直接影响返回覆盖率。', '模型本体分析不依赖训练日志也可以独立查看。']
        : ['Check whether the input sequence is long enough.', 'Top-K and detail flags directly affect result coverage.', 'The model profile can be analyzed without training logs.']),
    };
  }

  if (moduleKey === 'sequenceEncoder') {
    const summary = isZh.value
      ? '顺序推荐模型利用历史岗位顺序表达用户偏好，序列越短越容易出现兴趣信号不足；默认输入上限按 50 个岗位 ID 理解。'
      : 'The sequential encoder uses ordered job history as preference evidence. Short histories carry weaker intent signals; the displayed input contract treats 50 job IDs as the practical ceiling.';
    return {
      ...base,
      status: 'ready',
      score: 0.86,
      recordCount: 50,
      latestStep: null,
      summary,
      metrics: { sequenceLimit: 50, currentHistoryLength: metrics.historyLength, encoderType: 'sequential recommender' },
      aiPanels: resultPanel(moduleLabel(moduleKey), summary, isZh.value
        ? ['用不同长度的历史序列观察推荐稳定性。', '保留岗位 ID 的顺序，不要只把它当集合处理。']
        : ['Compare recommendation stability across different history lengths.', 'Preserve job ID order rather than treating the history as an unordered set.']),
    };
  }

  if (moduleKey === 'inputContract') {
    const summary = isZh.value
      ? `当前输入契约包含 user_history、top_k 和 include_job_info；默认 Top-K 为 ${metrics.requestedTopK}，详情开关决定岗位名称、公司和薪资字段是否有覆盖。`
      : `The input contract contains user_history, top_k, and include_job_info. Current Top-K is ${metrics.requestedTopK}; the detail flag controls position, company, and salary coverage.`;
    return {
      ...base,
      status: 'ready',
      score: 1,
      recordCount: 3,
      latestStep: metrics.requestedTopK,
      summary,
      metrics: { fields: ['user_history', 'top_k', 'include_job_info'], requestedTopK: metrics.requestedTopK },
      aiPanels: resultPanel(moduleLabel(moduleKey), summary, isZh.value
        ? ['Top-K 变大后要重新观察填充率。', '关闭详情返回时，岗位分布分析只能依赖 itemId。']
        : ['Re-check fill rate after increasing Top-K.', 'Without details, job distribution analysis can only rely on item IDs.']),
    };
  }

  if (moduleKey === 'servingRuntime') {
    const summary = isZh.value
      ? `推理链路为前端页面到 DeepInsight 后端，再到本地 Flask BSARec 服务；当前服务地址为 ${record.serviceUrl || '未标注'}。`
      : `The serving path is frontend to DeepInsight backend to local Flask BSARec service. Current service URL is ${record.serviceUrl || 'not labeled'}.`;
    return {
      ...base,
      status: 'ready',
      score: 0.82,
      recordCount: 2,
      latestStep: null,
      summary,
      metrics: { backendEndpoint: '/api/v1/prediction/recommend', serviceUrl: record.serviceUrl, runtime: 'CPU / Flask' },
      aiPanels: resultPanel(moduleLabel(moduleKey), summary, isZh.value
        ? ['优先保证 Flask /recommend 健康。', '延迟异常时先区分后端耗时和模型服务耗时。']
        : ['Keep the Flask /recommend endpoint healthy first.', 'When latency is high, separate backend time from model-service time.']),
    };
  }

  if (moduleKey === 'inferenceFlow') {
    const score = serviceScore(record);
    const summary = isZh.value
      ? `本次推理状态为 ${record.status}，链路记录了平台接口、模型服务地址和后端消息，可用于判断问题发生在登录、后端还是 Flask 模型服务。`
      : `This inference status is ${record.status}. The record keeps platform endpoint, model-service URL, and backend message to locate failures across auth, backend, or Flask serving.`;
    return {
      ...base,
      status: 'ready',
      score,
      recordCount: record.serviceUrl ? 3 : 2,
      latestStep: metrics.recommendationCount,
      summary,
      metrics: { status: record.status, message: record.message, serviceUrl: record.serviceUrl },
      aiPanels: resultPanel(moduleLabel(moduleKey), summary, isZh.value
        ? ['如果状态为 offline，先启动本地 Flask API。', '如果状态为 error，检查登录状态和后端安全拦截。']
        : ['If status is offline, start the local Flask API first.', 'If status is error, check login state and backend security handling.']),
    };
  }

  if (moduleKey === 'inputSequence') {
    const score = Math.min(metrics.historyLength / 12, 1);
    const hasHistory = metrics.historyLength > 0;
    const summary = isZh.value
      ? `本次输入包含 ${metrics.historyLength} 个历史岗位 ID；序列越长，BSARec 越容易捕捉连续偏好，但过长时也应观察噪声。`
      : `This request contains ${metrics.historyLength} historical job IDs. Longer histories give BSARec more preference evidence, while very long histories should be checked for noise.`;
    return {
      ...base,
      status: hasHistory ? 'ready' : 'no_data',
      score: hasHistory ? score : null,
      recordCount: metrics.historyLength,
      latestStep: metrics.historyLength || null,
      summary,
      metrics: { historyLength: metrics.historyLength, requestedTopK: metrics.requestedTopK },
      aiPanels: resultPanel(moduleLabel(moduleKey), summary, isZh.value
        ? ['建议对短序列和中长序列分别观察推荐覆盖。', '历史 ID 应保持真实点击或浏览顺序。']
        : ['Compare coverage for short and medium-long histories.', 'Keep history IDs in real click or browse order.']),
    };
  }

  if (moduleKey === 'recommendationQuality') {
    const hasRecommendations = metrics.recommendationCount > 0;
    const summary = isZh.value
      ? `接口返回 ${metrics.recommendationCount}/${metrics.requestedTopK} 个推荐，Top-K 填充率为 ${percentLabel(metrics.fillRate)}，详情覆盖率为 ${percentLabel(metrics.detailCoverage)}。`
      : `The endpoint returned ${metrics.recommendationCount}/${metrics.requestedTopK} recommendations. Top-K fill rate is ${percentLabel(metrics.fillRate)} and detail coverage is ${percentLabel(metrics.detailCoverage)}.`;
    return {
      ...base,
      status: hasRecommendations ? 'ready' : 'no_data',
      score: hasRecommendations ? metrics.fillRate : null,
      recordCount: metrics.recommendationCount,
      latestStep: metrics.requestedTopK,
      summary,
      metrics,
      aiPanels: resultPanel(moduleLabel(moduleKey), summary, isZh.value
        ? ['填充率低时检查候选集规模或服务返回格式。', '详情覆盖低时确认 include_job_info 是否开启。']
        : ['When fill rate is low, inspect candidate size or response format.', 'When detail coverage is low, confirm include_job_info is enabled.']),
    };
  }

  if (moduleKey === 'jobDistribution') {
    const hasRecommendations = metrics.recommendationCount > 0;
    const diversityScore = hasRecommendations ? metrics.uniqueCompanyCount / metrics.recommendationCount : null;
    const summary = isZh.value
      ? `推荐结果覆盖 ${metrics.uniqueCompanyCount} 个公司/来源；该指标用于观察结果是否过度集中在少数岗位来源。`
      : `The recommendations cover ${metrics.uniqueCompanyCount} companies or sources. This helps identify whether results concentrate too heavily around a few sources.`;
    return {
      ...base,
      status: hasRecommendations ? 'ready' : 'no_data',
      score: diversityScore,
      recordCount: metrics.uniqueCompanyCount,
      latestStep: metrics.recommendationCount || null,
      summary,
      metrics: { uniqueCompanyCount: metrics.uniqueCompanyCount, recommendationCount: metrics.recommendationCount },
      aiPanels: resultPanel(moduleLabel(moduleKey), summary, isZh.value
        ? ['公司覆盖过低时，结合岗位名称检查推荐是否同质化。', '没有公司字段时，可以先用 itemId 聚类做近似观察。']
        : ['If company coverage is low, inspect positions for homogeneity.', 'When company fields are missing, cluster by item ID as an approximate view.']),
    };
  }

  const score = latencyScore(record);
  const summary = isZh.value
    ? `本次响应延迟为 ${record.elapsedMs ?? '--'}ms，延迟分档为 ${metrics.latencyBucket}；该模块用于观察推理服务是否适合交互式使用。`
    : `This response latency is ${record.elapsedMs ?? '--'}ms, bucketed as ${metrics.latencyBucket}. This module checks whether the service is suitable for interactive use.`;
  return {
    ...base,
    status: score === null ? 'no_data' : 'ready',
    score,
    recordCount: score === null ? 0 : 1,
    latestStep: record.elapsedMs,
    summary,
    metrics: { elapsedMs: record.elapsedMs, latencyBucket: metrics.latencyBucket, status: record.status },
    aiPanels: resultPanel(moduleLabel(moduleKey), summary, isZh.value
      ? ['延迟超过 1500ms 时，优先检查 Flask 服务和模型加载状态。', '没有延迟数据时，重新在预测页运行一次推理。']
      : ['If latency exceeds 1500ms, inspect Flask serving and model loading first.', 'If latency is missing, run inference again from the prediction page.']),
  };
}

function createLocalAnalysisBatch(target: AnalysisTarget): AnalysisBatch | null {
  if ((target.type !== 'prediction' && target.type !== 'model') || !target.predictionRecord) return null;
  const results = selectedModules.value.map((moduleKey, index) => localAnalysisResult(
    moduleKey,
    target.predictionRecord as PredictionAnalysisRecord,
    index,
    target.type,
  ));
  return {
    id: -Date.now(),
    title: target.type === 'model'
      ? (isZh.value ? 'BSARec 模型本体可视化分析' : 'BSARec model visual analysis')
      : (isZh.value ? `${target.predictionRecord.modelName} 推理可视化分析` : `${target.predictionRecord.modelName} inference visual analysis`),
    targetCount: 1,
    moduleCount: results.length,
    createdAt: new Date().toISOString(),
    results,
  };
}

function runLocalAnalysis(target: AnalysisTarget) {
  const batch = createLocalAnalysisBatch(target);
  if (!batch) return false;
  activeBatch.value = batch;
  selectedResultId.value = batch.results?.[0]?.id || 0;
  void nextTick(renderCharts);
  return true;
}

function syncSelectedModulesForTarget() {
  const target = selectedTarget.value;
  const catalog = target?.type === 'prediction'
    ? predictionModules.value
    : target?.type === 'model'
      ? modelModules.value
      : moduleCatalog.value;
  const validKeys = new Set(catalog.map((module) => module.key));
  const currentSelectionStillValid = selectedModules.value.length > 0 && selectedModules.value.every((key) => validKeys.has(key));
  if (currentSelectionStillValid) return;
  if (target?.type === 'prediction') {
    selectedModules.value = predictionDefaultModules;
  } else if (target?.type === 'model') {
    selectedModules.value = modelDefaultModules;
  } else {
    selectedModules.value = backendDefaultModules;
  }
}

function loadPredictionRecords() {
  predictionRecords.value = listPredictionAnalysisRecords();
}

function selectInitialTarget() {
  const selectedPredictionId = getSelectedPredictionAnalysisRecordId();
  const storedRunId = sessionStorage.getItem('vizRunId');
  const storedModule = sessionStorage.getItem('vizModuleKey');
  const selectedPredictionTarget = selectedPredictionId
    ? analysisTargets.value.find((target) => target.key === `prediction:${selectedPredictionId}`)
    : null;
  const fromSession = storedRunId
    ? targets.value.find((target) => String(target.id) === storedRunId && target.type === 'upload')
      || targets.value.find((target) => String(target.id) === storedRunId)
    : null;

  selectedTargetKey.value = selectedPredictionTarget?.key || fromSession?.key || analysisTargets.value[0]?.key || '';
  syncSelectedModulesForTarget();
  if (storedModule && selectedTarget.value?.type !== 'prediction' && selectedTarget.value?.type !== 'model') {
    selectedModules.value = [storedModule];
  }
}

async function loadTargets() {
  const response = await visualizationApi.listExperiments();
  targets.value = (response.data.data || []).map((item: any) => ({
    id: Number(item.id),
    key: `${item.type === 'upload' ? 'upload' : 'training'}:${item.id}`,
    name: String(item.name || item.runName || item.id),
    type: item.type === 'upload' ? 'upload' : 'training',
    status: item.status,
    architecture: item.architecture,
    modelArchitecture: item.modelArchitecture,
  }));
}

async function loadModules() {
  try {
    const response = await visualizationApi.getAnalysisModules();
    moduleCatalog.value = response.data.data || fallbackModules.value;
  } catch {
    moduleCatalog.value = fallbackModules.value;
  }
}

async function loadRecentBatches() {
  const response = await visualizationApi.listAnalysisBatches(8);
  recentBatches.value = response.data.data || [];
}

async function loadBatch(id?: number) {
  if (!id) return;
  const response = await visualizationApi.getAnalysisBatch(id);
  activeBatch.value = response.data.data || null;
  selectedResultId.value = activeBatch.value?.results?.[0]?.id || 0;
}

async function runAnalysis() {
  if (!selectedTarget.value) {
    ElMessage.warning(copy.value.noTargetDesc);
    return;
  }
  if (!selectedModules.value.length) {
    ElMessage.warning(isZh.value ? '请至少选择一个分析模块。' : 'Select at least one analysis module.');
    return;
  }

  batchLoading.value = true;
  try {
    if (selectedTarget.value.type === 'prediction' || selectedTarget.value.type === 'model') {
      runLocalAnalysis(selectedTarget.value);
      ElMessage.success(copy.value.predictionRunSuccess);
      return;
    }

    const title = isZh.value
      ? `${selectedTarget.value.name} 可视化分析`
      : `${selectedTarget.value.name} visual analysis`;
    const response = await visualizationApi.createAnalysisBatch({
      title,
      targets: [{ runId: selectedTarget.value.id, runType: selectedTarget.value.type }],
      modules: selectedModules.value,
    });
    activeBatch.value = response.data.data || null;
    selectedResultId.value = activeBatch.value?.results?.[0]?.id || 0;
    await loadRecentBatches();
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || (isZh.value ? '分析生成失败，请检查登录状态和后端服务。' : 'Analysis failed. Check login state and backend service.'));
  } finally {
    batchLoading.value = false;
  }
}

onMounted(async () => {
  try {
    loadPredictionRecords();
    await Promise.all([loadModules(), loadTargets(), loadRecentBatches()]);
    selectInitialTarget();
    if (selectedTarget.value?.type === 'prediction' || selectedTarget.value?.type === 'model') {
      runLocalAnalysis(selectedTarget.value);
    } else if (recentBatches.value[0]?.id) {
      await loadBatch(recentBatches.value[0].id);
    }
    await renderCharts();
  } catch {
    ElMessage.error(isZh.value ? '可视化分析数据加载失败。' : 'Failed to load visual analysis data.');
  }
  window.addEventListener('resize', resizeCharts, { passive: true });
});

watch(selectedTargetKey, (_key, oldKey) => {
  if (!oldKey) return;
  selectedEvidenceGroupKey.value = 'ranking';
  syncSelectedModulesForTarget();
  activeBatch.value = null;
  selectedResultId.value = 0;
  if (selectedTarget.value?.type === 'prediction' || selectedTarget.value?.type === 'model') {
    runLocalAnalysis(selectedTarget.value);
  }
});

watch(selectedEvidenceGroupKey, () => {
  void renderModelEvidenceChart();
});

watch(
  [analysisResults, recentBatches, moduleCatalog, activeModuleCatalog, isZh],
  () => {
    void renderCharts();
  },
  { deep: true },
);

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts);
  disposeCharts();
});
</script>

<style scoped>
.viz-analysis-page {
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

.hierarchy-eyebrow {
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
  max-width: 780px;
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.7;
  margin: 0;
}

.header-status {
  min-width: 138px;
  padding: 12px 14px;
  border: 1px solid var(--border-color);
  border-radius: 14px;
  background: color-mix(in srgb, var(--surface-1) 80%, transparent);
  text-align: right;
}

.header-status span {
  display: block;
  color: var(--text-primary);
  font-size: 26px;
  line-height: 1;
  font-weight: var(--font-weight-title);
}

.header-status small {
  color: var(--text-secondary);
  font-size: 12px;
}

.analysis-builder,
.chart-gallery,
.result-panel,
.insight-panel,
.recent-band {
  border: 1px solid var(--border-color);
  border-radius: 16px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.07), rgba(255, 255, 255, 0.015)),
    color-mix(in srgb, var(--surface-1) 82%, transparent);
  box-shadow: var(--shadow-soft);
}

.analysis-builder {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(320px, 440px);
  gap: 18px;
  padding: clamp(18px, 2vw, 24px);
}

.builder-main,
.builder-side {
  min-width: 0;
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

.section-heading span {
  color: var(--text-secondary);
  font-size: 12px;
}

.section-heading strong {
  color: var(--text-primary);
  font-size: 16px;
  line-height: 1.35;
}

.target-toolbar {
  display: grid;
  grid-template-columns: minmax(260px, 1fr) auto;
  gap: 12px;
  align-items: center;
}

.target-select {
  width: 100%;
}

.target-toolbar :deep(.el-button) {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  min-height: 38px;
}

.target-summary {
  margin-top: 16px;
  min-height: 104px;
  padding: 16px;
  border: 1px solid var(--border-color);
  border-radius: 14px;
  background: var(--panel-bg);
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr) auto;
  gap: 14px;
  align-items: center;
}

.target-summary.empty {
  border-style: dashed;
}

.target-summary svg {
  color: var(--primary-color);
}

.target-summary strong {
  color: var(--text-primary);
  font-size: 16px;
}

.target-summary p {
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.7;
  margin: 5px 0 0;
}

.target-summary > span {
  padding: 5px 9px;
  border-radius: 999px;
  background: rgba(var(--primary-rgb), 0.1);
  color: var(--primary-color);
  font-size: 12px;
}

.module-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.module-chip {
  min-height: 42px;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  background: var(--panel-bg);
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 10px 12px;
  text-align: left;
  cursor: pointer;
  transition: border-color 180ms ease, background 180ms ease, color 180ms ease;
}

.module-chip.active,
.module-chip:hover {
  border-color: color-mix(in srgb, var(--primary-color) 48%, var(--border-color));
  background: rgba(var(--primary-rgb), 0.1);
  color: var(--text-primary);
}

.module-chip svg {
  color: var(--primary-color);
  flex-shrink: 0;
}

.analysis-status-band {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-top: 18px;
}

.analysis-status-band article {
  min-height: 86px;
  padding: 16px;
  border: 1px solid var(--border-color);
  border-radius: 14px;
  background: color-mix(in srgb, var(--surface-2) 72%, transparent);
  display: flex;
  align-items: center;
  gap: 13px;
}

.analysis-status-band svg {
  color: var(--primary-color);
}

.analysis-status-band span {
  color: var(--text-secondary);
  font-size: 12px;
}

.analysis-status-band strong {
  display: block;
  color: var(--text-primary);
  font-size: 24px;
  line-height: 1.15;
  margin-top: 4px;
}

.model-evidence-board {
  margin-top: 18px;
  padding: 18px;
  border: 1px solid var(--border-color);
  border-radius: 16px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.07), rgba(255, 255, 255, 0.015)),
    color-mix(in srgb, var(--surface-1) 82%, transparent);
  box-shadow: var(--shadow-soft);
}

.evidence-overview {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

.evidence-overview article {
  min-width: 0;
  min-height: 104px;
  padding: 14px;
  border: 1px solid var(--border-color);
  border-radius: 14px;
  background: color-mix(in srgb, var(--surface-2) 72%, transparent);
}

.evidence-overview span,
.evidence-kpi-grid span,
.parameter-evidence-panel header span,
.evidence-chart-panel header span,
.evidence-source-strip span {
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.35;
}

.evidence-overview strong {
  display: block;
  color: var(--text-primary);
  font-size: 24px;
  line-height: 1.15;
  margin: 8px 0;
  word-break: break-word;
}

.evidence-overview small,
.evidence-kpi-grid small,
.parameter-evidence-list small {
  color: var(--text-muted);
  font-size: 11px;
  line-height: 1.45;
  word-break: break-word;
}

.evidence-analysis-layout {
  display: grid;
  grid-template-columns: minmax(220px, 280px) minmax(0, 1fr);
  gap: 14px;
  align-items: start;
}

.evidence-group-list {
  display: grid;
  gap: 10px;
}

.evidence-group-list button {
  min-width: 0;
  min-height: 78px;
  padding: 14px;
  border: 1px solid var(--border-color);
  border-radius: 14px;
  background: var(--panel-bg);
  color: var(--text-secondary);
  text-align: left;
  cursor: pointer;
  transition: border-color 180ms ease, background 180ms ease, transform 180ms ease;
}

.evidence-group-list button.active,
.evidence-group-list button:hover {
  border-color: color-mix(in srgb, var(--primary-color) 48%, var(--border-color));
  background: rgba(var(--primary-rgb), 0.1);
}

.evidence-group-list button.active {
  transform: translateY(-1px);
}

.evidence-group-list strong {
  display: block;
  color: var(--text-primary);
  font-size: 14px;
  line-height: 1.35;
  margin-bottom: 6px;
}

.evidence-group-list span {
  display: block;
  font-size: 12px;
  line-height: 1.55;
}

.evidence-main {
  min-width: 0;
}

.evidence-kpi-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.evidence-kpi-grid article {
  min-width: 0;
  min-height: 178px;
  padding: 14px;
  border: 1px solid var(--border-color);
  border-radius: 14px;
  background: var(--panel-bg);
  display: grid;
  grid-template-rows: auto auto auto 1fr;
  gap: 9px;
}

.evidence-kpi-grid strong {
  display: block;
  color: var(--text-primary);
  font-size: 22px;
  line-height: 1.15;
  margin-top: 5px;
  word-break: break-word;
}

.evidence-kpi-grid p,
.parameter-evidence-list p {
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.6;
  margin: 0;
}

.metric-bar {
  height: 8px;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.18);
  overflow: hidden;
}

.metric-bar i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #22c55e, #38bdf8);
}

.evidence-detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(280px, 0.85fr);
  gap: 12px;
  margin-top: 12px;
}

.evidence-chart-panel,
.parameter-evidence-panel {
  min-width: 0;
  min-height: 340px;
  padding: 14px;
  border: 1px solid var(--border-color);
  border-radius: 14px;
  background: var(--panel-bg);
}

.evidence-chart-panel header,
.parameter-evidence-panel header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  min-height: 42px;
  margin-bottom: 10px;
}

.evidence-chart-panel header strong,
.parameter-evidence-panel header strong {
  display: block;
  color: var(--text-primary);
  font-size: 15px;
  line-height: 1.35;
  margin-top: 3px;
}

.evidence-chart-panel header svg,
.parameter-evidence-panel header svg {
  color: var(--primary-color);
  flex-shrink: 0;
}

.evidence-chart-box {
  width: 100%;
  height: 270px;
  min-width: 0;
}

.parameter-evidence-list {
  display: grid;
  gap: 9px;
}

.parameter-evidence-list div {
  min-width: 0;
  padding: 11px 12px;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  background: color-mix(in srgb, var(--surface-2) 68%, transparent);
}

.parameter-evidence-list span {
  display: block;
  color: var(--text-secondary);
  font-size: 11px;
  line-height: 1.35;
}

.parameter-evidence-list strong {
  display: block;
  color: var(--text-primary);
  font-size: 14px;
  line-height: 1.35;
  margin: 5px 0;
  word-break: break-word;
}

.evidence-source-strip {
  min-width: 0;
  margin-top: 12px;
  padding: 12px;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  background: color-mix(in srgb, var(--surface-2) 72%, transparent);
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.evidence-source-strip b {
  padding: 5px 9px;
  border-radius: 999px;
  background: rgba(var(--primary-rgb), 0.1);
  color: var(--primary-color);
  font-size: 11px;
  line-height: 1.2;
  word-break: break-word;
}

.chart-gallery {
  margin-top: 18px;
  padding: 18px;
}

.chart-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.analysis-chart-card {
  min-width: 0;
  min-height: 320px;
  padding: 14px;
  border: 1px solid var(--border-color);
  border-radius: 14px;
  background: var(--panel-bg);
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 10px;
}

.analysis-chart-card header {
  min-height: 44px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.analysis-chart-card header span {
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.35;
}

.analysis-chart-card header strong {
  display: block;
  color: var(--text-primary);
  font-size: 16px;
  line-height: 1.35;
  margin-top: 3px;
}

.analysis-chart-card header svg {
  flex-shrink: 0;
  color: var(--primary-color);
}

.chart-box {
  width: 100%;
  height: 246px;
  min-width: 0;
}

.chart-empty {
  min-height: 220px;
  border: 1px dashed var(--border-color);
  border-radius: 14px;
  background: var(--panel-bg);
  color: var(--text-secondary);
  display: grid;
  place-items: center;
  text-align: center;
  padding: 24px;
}

.chart-empty svg {
  color: var(--primary-color);
}

.chart-empty strong {
  color: var(--text-primary);
}

.chart-empty p {
  max-width: 460px;
  margin: 0;
  line-height: 1.7;
}

.analysis-workbench {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(320px, 430px);
  gap: 18px;
  margin-top: 18px;
  align-items: start;
}

.result-panel,
.insight-panel,
.recent-band {
  padding: 18px;
}

.result-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.result-card {
  min-height: 198px;
  padding: 15px;
  border: 1px solid var(--border-color);
  border-radius: 14px;
  background: var(--panel-bg);
  cursor: pointer;
  display: grid;
  gap: 10px;
}

.result-card.ready {
  border-left: 4px solid #22c55e;
}

.result-card.no_data {
  border-left: 4px solid #f59e0b;
}

.result-card header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.result-card header span,
.metric-pills span {
  color: var(--text-secondary);
  font-size: 12px;
}

.result-card header strong {
  color: var(--text-primary);
  font-size: 13px;
}

.result-score {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.result-score b {
  color: var(--text-primary);
  font-size: 34px;
  line-height: 1;
}

.result-score small {
  color: var(--text-secondary);
}

.result-card p,
.insight-body p {
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.7;
  margin: 0;
}

.metric-pills {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.metric-pills span {
  padding: 5px 9px;
  border-radius: 999px;
  background: rgba(var(--primary-rgb), 0.09);
}

.empty-result,
.insight-empty,
.recent-empty {
  min-height: 180px;
  border: 1px dashed var(--border-color);
  border-radius: 14px;
  background: var(--panel-bg);
  color: var(--text-secondary);
  display: grid;
  place-items: center;
  text-align: center;
  padding: 24px;
}

.empty-result svg {
  color: var(--primary-color);
}

.empty-result strong {
  color: var(--text-primary);
}

.empty-result p {
  max-width: 420px;
  margin: 0;
  line-height: 1.7;
}

.insight-body {
  display: grid;
  gap: 14px;
}

.recommendation-list {
  display: grid;
  gap: 10px;
}

.recommendation-list article {
  padding: 12px;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  background: var(--panel-bg);
  display: flex;
  gap: 9px;
  align-items: flex-start;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.recommendation-list svg {
  color: #22c55e;
  flex-shrink: 0;
  margin-top: 2px;
}

.recent-band {
  margin-top: 18px;
}

.recent-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.recent-list button {
  min-height: 72px;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  background: var(--panel-bg);
  display: grid;
  gap: 6px;
  padding: 12px;
  text-align: left;
  cursor: pointer;
}

.recent-list span {
  color: var(--text-primary);
  font-size: 13px;
  font-weight: var(--font-weight-title);
  line-height: 1.4;
}

.recent-list small {
  color: var(--text-secondary);
  font-size: 12px;
}

.recent-empty {
  grid-column: 1 / -1;
  min-height: 90px;
}

@media (max-width: 1180px) {
  .analysis-builder,
  .analysis-workbench,
  .evidence-analysis-layout,
  .evidence-detail-grid {
    grid-template-columns: 1fr;
  }

  .chart-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .analysis-status-band,
  .evidence-overview,
  .evidence-kpi-grid,
  .recent-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .evidence-group-list {
    grid-template-columns: repeat(5, minmax(150px, 1fr));
    overflow-x: auto;
    padding-bottom: 3px;
  }
}

@media (max-width: 760px) {
  .viz-analysis-page {
    padding: 16px;
  }

  .page-header,
  .section-heading,
  .target-toolbar {
    display: grid;
    justify-items: start;
  }

  .model-evidence-board {
    padding: 14px;
  }

  .header-status {
    text-align: left;
  }

  .target-toolbar,
  .target-summary,
  .module-grid,
  .analysis-status-band,
  .evidence-overview,
  .evidence-kpi-grid,
  .evidence-detail-grid,
  .chart-grid,
  .result-grid,
  .recent-list {
    grid-template-columns: 1fr;
  }

  .evidence-group-list {
    grid-template-columns: 1fr;
    overflow: visible;
  }

  .analysis-chart-card {
    min-height: 290px;
  }

  .chart-box {
    height: 220px;
  }

  .evidence-chart-box {
    height: 230px;
  }

  .target-summary > span {
    width: max-content;
  }
}
</style>
