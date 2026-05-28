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
          <strong>{{ copy.modulesTitle }}</strong>
        </div>
        <div class="module-grid">
          <button
            v-for="module in moduleCatalog"
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
  BarChart3,
  CheckCircle2,
  Database,
  FileText,
  GitBranch,
  Image,
  LineChart,
  ListTree,
  Play,
  SlidersHorizontal,
  TableProperties,
  Timer,
  AudioWaveform,
} from 'lucide-vue-next';
import { visualizationApi } from '@/api';

type AnalysisTarget = {
  id: number
  key: string
  name: string
  type: 'training' | 'upload'
  status?: string
  architecture?: string
  modelArchitecture?: string
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

const { locale } = useI18n();
const isZh = computed(() => locale.value.startsWith('zh'));

const targets = ref<AnalysisTarget[]>([]);
const moduleCatalog = ref<AnalysisModule[]>([]);
const recentBatches = ref<AnalysisBatch[]>([]);
const activeBatch = ref<AnalysisBatch | null>(null);
const selectedTargetKey = ref('');
const selectedModules = ref<string[]>(['scalars', 'hparams', 'graphs']);
const selectedResultId = ref(0);
const batchLoading = ref(false);
const statusChartRef = ref<HTMLDivElement>();
const recordsChartRef = ref<HTMLDivElement>();
const scoreChartRef = ref<HTMLDivElement>();
const stepChartRef = ref<HTMLDivElement>();
const coverageChartRef = ref<HTMLDivElement>();
const recentChartRef = ref<HTMLDivElement>();
const chartInstances = new Map<string, echarts.ECharts>();

const copy = computed(() => (isZh.value ? {
  eyebrow: '可视化分析工作台',
  subtitle: '这里分析真实训练任务或上传运行的日志记录：标量、分布、超参数、结构、PR/ROC 和性能剖析，不再复述模型总览。',
  resultCount: '条分析结果',
  analysisTarget: '分析对象',
  analysisTargetTitle: '选择一个真实训练任务或上传运行',
  targetPlaceholder: '选择要分析的数据运行',
  runAnalysis: '生成分析',
  noTarget: '没有可分析对象',
  noTargetDesc: '请先产生训练任务或上传运行日志，再进入可视化分析。',
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
  statusTargets: '对象',
  statusModules: '模块',
  statusReady: '可用结果',
  statusNoData: '缺数据结果',
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
  subtitle: 'Analyze real training jobs or uploaded run logs: scalars, distributions, hyperparameters, graphs, PR/ROC, and profiler data. This is not another model overview.',
  resultCount: 'analysis results',
  analysisTarget: 'Target',
  analysisTargetTitle: 'Choose a real training job or uploaded run',
  targetPlaceholder: 'Select a run to analyze',
  runAnalysis: 'Run Analysis',
  noTarget: 'No target available',
  noTargetDesc: 'Create a training job or upload run logs before using visual analysis.',
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
  statusTargets: 'Targets',
  statusModules: 'Modules',
  statusReady: 'Ready results',
  statusNoData: 'No-data results',
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

const analysisTargets = computed(() => targets.value);
const selectedTarget = computed(() => analysisTargets.value.find((target) => target.key === selectedTargetKey.value) || null);
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

function resizeCharts() {
  chartInstances.forEach((instance) => instance.resize());
}

async function renderCharts() {
  if (!analysisResults.value.length) {
    disposeCharts();
    return;
  }

  await nextTick();

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
  return moduleCatalog.value.find((module) => module.key === key)?.label || key;
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
  };
  return icons[key] || BarChart3;
}

function targetIcon(target: AnalysisTarget) {
  return target.type === 'training' ? LineChart : Database;
}

function targetLabel(target: AnalysisTarget) {
  const type = target.type === 'training' ? copy.value.targetTraining : copy.value.targetUpload;
  const model = target.architecture || target.modelArchitecture;
  return model ? `${type} · ${target.name} · ${model}` : `${type} · ${target.name}`;
}

function targetDescription(target: AnalysisTarget) {
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
  const storedRunId = sessionStorage.getItem('vizRunId');
  const storedModule = sessionStorage.getItem('vizModuleKey');
  const fromSession = storedRunId
    ? targets.value.find((target) => String(target.id) === storedRunId && target.type === 'upload')
      || targets.value.find((target) => String(target.id) === storedRunId)
    : null;
  selectedTargetKey.value = fromSession?.key || targets.value[0]?.key || '';
  if (storedModule) selectedModules.value = [storedModule];
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
    await Promise.all([loadModules(), loadTargets(), loadRecentBatches()]);
    if (recentBatches.value[0]?.id) await loadBatch(recentBatches.value[0].id);
    await renderCharts();
  } catch {
    ElMessage.error(isZh.value ? '可视化分析数据加载失败。' : 'Failed to load visual analysis data.');
  }
  window.addEventListener('resize', resizeCharts, { passive: true });
});

watch(
  [analysisResults, recentBatches, moduleCatalog, isZh],
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
  .analysis-workbench {
    grid-template-columns: 1fr;
  }

  .chart-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .analysis-status-band,
  .recent-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
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

  .header-status {
    text-align: left;
  }

  .target-toolbar,
  .target-summary,
  .module-grid,
  .analysis-status-band,
  .chart-grid,
  .result-grid,
  .recent-list {
    grid-template-columns: 1fr;
  }

  .analysis-chart-card {
    min-height: 290px;
  }

  .chart-box {
    height: 220px;
  }

  .target-summary > span {
    width: max-content;
  }
}
</style>
