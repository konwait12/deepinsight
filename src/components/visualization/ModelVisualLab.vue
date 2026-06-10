<template>
  <section class="reco-visual-lab">
    <div class="lab-hero">
      <div>
        <span>{{ copy.heroEyebrow }}</span>
        <h1>{{ copy.title }}</h1>
        <p>{{ copy.subtitle }}</p>
      </div>
      <div class="lab-summary">
        <article>
          <span>{{ copy.modelFamilies }}</span>
          <strong>{{ modelCountLabel }}</strong>
        </article>
        <article>
          <span>{{ copy.withEvalLogs }}</span>
          <strong>{{ loggedModelsLabel }}</strong>
        </article>
        <article>
          <span>{{ copy.totalInteractions }}</span>
          <strong>{{ totalInteractions }}</strong>
        </article>
      </div>
    </div>

    <p v-if="catalogNotice" class="notice">{{ catalogNotice }}</p>

    <div class="metric-workbench">
      <aside class="metric-groups" :aria-label="copy.groupLabel">
        <button
          v-for="group in groups"
          :key="group.key"
          type="button"
          :class="{ active: selectedGroup === group.key }"
          @click="selectedGroup = group.key"
        >
          <strong>{{ group.label }}</strong>
          <span>{{ group.desc }}</span>
        </button>
      </aside>

      <article class="metric-main">
        <header class="metric-main-header">
          <div>
            <span>{{ activeGroup.label }}</span>
            <strong>{{ activeMetric.title }}</strong>
            <p>{{ activeMetric.desc }}</p>
          </div>
          <b>{{ availabilityLabel }}</b>
        </header>

        <div class="metric-tabs" :aria-label="copy.metricLabel">
          <button
            v-for="metric in activeGroup.metrics"
            :key="metric.key"
            type="button"
            :class="{ active: selectedMetricKey === metric.key }"
            @click="selectedMetricKey = metric.key"
          >
            {{ metric.label }}
          </button>
        </div>

        <div class="chart-shell">
          <div ref="chartRef" class="chart-box" :class="{ muted: !availableRows.length }"></div>
          <div v-if="!availableRows.length" class="empty-state">
            <strong>{{ copy.noDataTitle }}</strong>
            <span>{{ copy.noDataDesc }}</span>
          </div>
        </div>

        <div class="stat-grid">
          <article v-for="card in statCards" :key="card.label" :class="{ wide: card.wide }">
            <span>{{ card.label }}</span>
            <strong>{{ card.value }}</strong>
            <p v-if="card.desc">{{ card.desc }}</p>
          </article>
        </div>
      </article>
    </div>

    <section class="comparison-list">
      <article
        v-for="row in displayRows"
        :key="row.name"
        :class="{ missing: !row.available }"
      >
        <header>
          <div>
            <strong>{{ row.title }}</strong>
            <span>{{ row.status }} / {{ row.source }}</span>
          </div>
          <b>{{ row.display }}</b>
        </header>
        <div class="row-meter" aria-hidden="true">
          <i :style="{ width: row.percent + '%' }"></i>
        </div>
      </article>
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import * as echarts from 'echarts'
import { trainingApi } from '@/api'
import type { ModelMetricMap, ModelOption } from '@/types/models'
import { buildChartTheme, chartAxis, chartGrid, chartTooltip } from '@/utils/chartTheme'
import {
  datasetMetricNumber,
  displayValue,
  formatInteger,
  metricSourceLabel,
  modelTitle,
  parseMetric,
  statusText,
} from '@/utils/recommenderCatalog'

type GroupKey = 'effect' | 'error' | 'scale' | 'training'
type MetricSource = 'metrics' | 'datasetSummary' | 'trainingSummary' | 'parameterSummary'
type MetricFormat = 'decimal' | 'integer' | 'scientific'

type MetricSpec = {
  key: string
  label: string
  title: string
  desc: string
  formula: string
  sources: MetricSource[]
  aliases: string[]
  format: MetricFormat
  higherBetter: boolean | null
}

type MetricGroup = {
  key: GroupKey
  label: string
  desc: string
  metrics: MetricSpec[]
}

type MetricRow = {
  name: string
  title: string
  status: string
  source: string
  numeric: number
  display: string
  available: boolean
  percent: number
}

const { locale } = useI18n()
const isZh = computed(() => !locale.value.startsWith('en'))
const modelCatalog = ref<ModelOption[]>([])
const selectedGroup = ref<GroupKey>('effect')
const selectedMetricKey = ref('HR@10')
const chartRef = ref<HTMLElement | null>(null)
let chart: echarts.ECharts | null = null

const copy = computed(() => isZh.value
  ? {
      title: '推荐模型指标对比',
      heroEyebrow: '模型对比',
      subtitle: '按推荐系统常用指标查看每个模型：推荐效果、误差指标、数据规模和训练配置。没有真实记录的字段会标为未记录，不再用 0 或百分制伪装。',
      modelFamilies: '模型',
      withEvalLogs: '有评估日志',
      totalInteractions: '总交互',
      groupLabel: '指标分组',
      metricLabel: '指标切换',
      availableModels: '有记录',
      noDataTitle: '当前指标没有真实记录',
      noDataDesc: '后端目录没有读取到这个字段，所以这里保留为空，不生成模拟数值。',
      missing: '未记录',
      noMetricLog: '当前目录没有评估日志',
      catalogFailed: '后端未连接，无法读取模型目录。请先启动 Spring Boot 后端，然后刷新本页。',
      bestHigh: '最高值',
      bestLow: '最低值',
      maxValue: '最大值',
      average: '平均值',
      coverage: '覆盖模型',
      definition: '指标定义',
      sourceMap: {
        metrics: '评估日志',
        datasetSummary: '数据集扫描',
        trainingSummary: '训练记录',
        parameterSummary: '训练配置',
      } as Record<MetricSource, string>,
      statusMap: {
        在线: '在线',
        服务离线: '服务离线',
        '权重+日志': '权重+日志',
        仅有权重: '仅有权重',
        '代码+数据': '代码+数据',
        仅有数据: '仅有数据',
        缺少数据: '缺少数据',
        未注册推理: '未注册推理',
      } as Record<string, string>,
      groups: [
        {
          key: 'effect' as const,
          label: '推荐效果',
          desc: '命中率和排序质量',
          metrics: [
            { key: 'HR@10', label: 'HR@10', title: 'HR@10 对比', desc: 'Top-10 推荐列表是否命中目标物品，数值越高越好。', formula: 'Hit Rate@10 = 命中的用户数 / 参与评估的用户数', sources: ['metrics'], aliases: ['HR@10', 'HIT@10', 'Hit rate@10'], format: 'decimal', higherBetter: true },
            { key: 'NDCG@10', label: 'NDCG@10', title: 'NDCG@10 对比', desc: '同时考虑命中位置，越靠前命中得分越高。', formula: 'NDCG@10 = DCG@10 / IDCG@10', sources: ['metrics'], aliases: ['NDCG@10', 'NDCG @ 10'], format: 'decimal', higherBetter: true },
            { key: 'Recall@10', label: 'Recall@10', title: 'Recall@10 对比', desc: 'Top-10 召回目标物品的比例。只有日志中真的记录 Recall 才展示。', formula: 'Recall@10 = Top-10 命中的相关物品数 / 全部相关物品数', sources: ['metrics'], aliases: ['Recall@10', 'Recall @ 10'], format: 'decimal', higherBetter: true },
            { key: 'AUC', label: 'AUC', title: 'AUC 对比', desc: '衡量正负样本排序区分能力。当前日志没有该字段时保持未记录。', formula: 'AUC = 随机正样本排在随机负样本之前的概率', sources: ['metrics'], aliases: ['AUC', 'ROC AUC'], format: 'decimal', higherBetter: true },
          ],
        },
        {
          key: 'error' as const,
          label: '误差指标',
          desc: '评分预测误差',
          metrics: [
            { key: 'RMSE', label: 'RMSE', title: 'RMSE 对比', desc: '平方误差开方，常用于评分预测任务，数值越低越好。', formula: 'RMSE = sqrt(mean((y - y_hat)^2))', sources: ['metrics', 'trainingSummary'], aliases: ['RMSE', 'Root Mean Square Error'], format: 'decimal', higherBetter: false },
            { key: 'MAE', label: 'MAE', title: 'MAE 对比', desc: '平均绝对误差，常用于评分预测任务，数值越低越好。', formula: 'MAE = mean(abs(y - y_hat))', sources: ['metrics', 'trainingSummary'], aliases: ['MAE', 'Mean Absolute Error'], format: 'decimal', higherBetter: false },
          ],
        },
        {
          key: 'scale' as const,
          label: '数据规模',
          desc: '用户、物品、交互量',
          metrics: [
            { key: 'User scale', label: '用户数', title: '用户规模对比', desc: '来自数据集扫描结果，用于判断训练覆盖的用户体量。', formula: 'User scale = 数据集中可识别的用户数量', sources: ['datasetSummary'], aliases: ['User scale', 'Users', 'User count', '用户数', '用户规模'], format: 'integer', higherBetter: null },
            { key: 'Item scale', label: '物品数', title: '物品规模对比', desc: '来自数据集扫描结果，用于判断候选物品体量。', formula: 'Item scale = 数据集中可识别的物品数量', sources: ['datasetSummary'], aliases: ['Item scale', 'Items', 'Item count', '物品数', '物品规模'], format: 'integer', higherBetter: null },
            { key: 'Interactions', label: '交互数', title: '交互规模对比', desc: '来自数据集扫描结果，用于判断行为序列是否足够丰富。', formula: 'Interactions = 用户和物品之间的行为记录数', sources: ['datasetSummary'], aliases: ['Interactions', 'Interaction count', '交互数', '交互规模'], format: 'integer', higherBetter: null },
            { key: 'Avg interactions/user', label: '人均交互', title: '人均交互对比', desc: '由交互数除以用户数计算，反映每个用户平均贡献的序列长度。', formula: 'Avg interactions/user = Interactions / User scale', sources: ['datasetSummary'], aliases: ['Avg interactions/user', 'Average interactions per user', '人均交互'], format: 'decimal', higherBetter: null },
            { key: 'Density', label: '稀疏度(%)', title: '数据稠密度对比', desc: '由交互数除以用户数和物品数的乘积计算，推荐数据通常非常稀疏。', formula: 'Density = Interactions / (Users × Items)', sources: ['datasetSummary'], aliases: ['Density', 'Sparsity complement', '稀疏度', '稠密度'], format: 'decimal', higherBetter: null },
          ],
        },
        {
          key: 'training' as const,
          label: '训练配置',
          desc: 'Batch、学习率、Loss',
          metrics: [
            { key: 'Batch Size', label: 'Batch Size', title: 'Batch Size 对比', desc: '训练配置中的批大小，用于判断吞吐和稳定性设置。', formula: 'Batch Size = 每次参数更新使用的样本数量', sources: ['parameterSummary', 'trainingSummary'], aliases: ['Batch Size', 'Batch size', 'batch_size', 'Batch'], format: 'integer', higherBetter: null },
            { key: 'Learning Rate', label: 'Learning Rate', title: 'Learning Rate 对比', desc: '训练配置中的学习率，用于判断优化步长。', formula: 'Learning Rate = 优化器每次更新的步长系数', sources: ['parameterSummary', 'trainingSummary'], aliases: ['Learning Rate', 'learning_rate', 'lr', 'LR'], format: 'scientific', higherBetter: null },
            { key: 'Loss', label: 'Loss', title: 'Loss 对比', desc: '训练或评估日志记录的损失值，数值越低越好。', formula: 'Loss = 训练目标函数在记录点上的数值', sources: ['trainingSummary', 'metrics'], aliases: ['Loss', 'Train Loss', 'Validation Loss', 'currentLoss', 'Current Loss'], format: 'decimal', higherBetter: false },
          ],
        },
      ] satisfies MetricGroup[],
    }
  : {
      title: 'Recommender Metric Comparison',
      heroEyebrow: 'Model Comparison',
      subtitle: 'Compare each recommender by common evaluation groups: recommendation quality, error metrics, data scale, and training settings. Missing fields stay unavailable instead of being converted into fake zeros.',
      modelFamilies: 'Models',
      withEvalLogs: 'With eval logs',
      totalInteractions: 'Total interactions',
      groupLabel: 'Metric groups',
      metricLabel: 'Metrics',
      availableModels: 'Recorded',
      noDataTitle: 'No real record for this metric',
      noDataDesc: 'The backend catalog did not read this field, so the board leaves it empty instead of generating simulated values.',
      missing: 'Not recorded',
      noMetricLog: 'No evaluation log in the current directory',
      catalogFailed: 'Backend is not connected, so the real model catalog cannot be read. Start the Spring Boot backend and refresh this page.',
      bestHigh: 'Highest',
      bestLow: 'Lowest',
      maxValue: 'Max value',
      average: 'Average',
      coverage: 'Coverage',
      definition: 'Definition',
      sourceMap: {
        metrics: 'Eval log',
        datasetSummary: 'Dataset scan',
        trainingSummary: 'Training record',
        parameterSummary: 'Training config',
      } as Record<MetricSource, string>,
      statusMap: {
        在线: 'Online',
        服务离线: 'Service offline',
        '权重+日志': 'Artifacts + logs',
        仅有权重: 'Artifacts only',
        '代码+数据': 'Code + data',
        仅有数据: 'Data only',
        缺少数据: 'Missing data',
        未注册推理: 'No inference registered',
      } as Record<string, string>,
      groups: [
        {
          key: 'effect' as const,
          label: 'Recommendation quality',
          desc: 'Hit rate and ranking quality',
          metrics: [
            { key: 'HR@10', label: 'HR@10', title: 'HR@10 Comparison', desc: 'Whether the Top-10 list hits the target item. Higher is better.', formula: 'Hit Rate@10 = hit users / evaluated users', sources: ['metrics'], aliases: ['HR@10', 'HIT@10', 'Hit rate@10'], format: 'decimal', higherBetter: true },
            { key: 'NDCG@10', label: 'NDCG@10', title: 'NDCG@10 Comparison', desc: 'Ranking quality with position discount. Earlier hits score higher.', formula: 'NDCG@10 = DCG@10 / IDCG@10', sources: ['metrics'], aliases: ['NDCG@10', 'NDCG @ 10'], format: 'decimal', higherBetter: true },
            { key: 'Recall@10', label: 'Recall@10', title: 'Recall@10 Comparison', desc: 'Recall within the Top-10 list. Displayed only when Recall is truly logged.', formula: 'Recall@10 = relevant hits in Top-10 / all relevant items', sources: ['metrics'], aliases: ['Recall@10', 'Recall @ 10'], format: 'decimal', higherBetter: true },
            { key: 'AUC', label: 'AUC', title: 'AUC Comparison', desc: 'Ranking separability between positive and negative samples.', formula: 'AUC = probability a positive sample ranks above a negative sample', sources: ['metrics'], aliases: ['AUC', 'ROC AUC'], format: 'decimal', higherBetter: true },
          ],
        },
        {
          key: 'error' as const,
          label: 'Error metrics',
          desc: 'Rating prediction error',
          metrics: [
            { key: 'RMSE', label: 'RMSE', title: 'RMSE Comparison', desc: 'Root mean squared error for rating prediction tasks. Lower is better.', formula: 'RMSE = sqrt(mean((y - y_hat)^2))', sources: ['metrics', 'trainingSummary'], aliases: ['RMSE', 'Root Mean Square Error'], format: 'decimal', higherBetter: false },
            { key: 'MAE', label: 'MAE', title: 'MAE Comparison', desc: 'Mean absolute error for rating prediction tasks. Lower is better.', formula: 'MAE = mean(abs(y - y_hat))', sources: ['metrics', 'trainingSummary'], aliases: ['MAE', 'Mean Absolute Error'], format: 'decimal', higherBetter: false },
          ],
        },
        {
          key: 'scale' as const,
          label: 'Data scale',
          desc: 'Users, items, interactions',
          metrics: [
            { key: 'User scale', label: 'Users', title: 'User Scale Comparison', desc: 'Read from dataset scans to show how many users are covered.', formula: 'User scale = identifiable users in the dataset', sources: ['datasetSummary'], aliases: ['User scale', 'Users', 'User count', '用户数', '用户规模'], format: 'integer', higherBetter: null },
            { key: 'Item scale', label: 'Items', title: 'Item Scale Comparison', desc: 'Read from dataset scans to show candidate item volume.', formula: 'Item scale = identifiable items in the dataset', sources: ['datasetSummary'], aliases: ['Item scale', 'Items', 'Item count', '物品数', '物品规模'], format: 'integer', higherBetter: null },
            { key: 'Interactions', label: 'Interactions', title: 'Interaction Scale Comparison', desc: 'Read from dataset scans to show behavior sequence volume.', formula: 'Interactions = behavior records between users and items', sources: ['datasetSummary'], aliases: ['Interactions', 'Interaction count', '交互数', '交互规模'], format: 'integer', higherBetter: null },
            { key: 'Avg interactions/user', label: 'Avg/User', title: 'Average Interactions Per User', desc: 'Calculated as interactions divided by users.', formula: 'Avg interactions/user = Interactions / User scale', sources: ['datasetSummary'], aliases: ['Avg interactions/user', 'Average interactions per user', '人均交互'], format: 'decimal', higherBetter: null },
            { key: 'Density', label: 'Density (%)', title: 'Dataset Density Comparison', desc: 'Calculated from interactions divided by users and items. Recommender datasets are usually sparse.', formula: 'Density = Interactions / (Users × Items)', sources: ['datasetSummary'], aliases: ['Density', 'Sparsity complement', '稀疏度', '稠密度'], format: 'decimal', higherBetter: null },
          ],
        },
        {
          key: 'training' as const,
          label: 'Training config',
          desc: 'Batch, learning rate, loss',
          metrics: [
            { key: 'Batch Size', label: 'Batch Size', title: 'Batch Size Comparison', desc: 'Batch size from training configuration.', formula: 'Batch Size = samples used for one parameter update', sources: ['parameterSummary', 'trainingSummary'], aliases: ['Batch Size', 'Batch size', 'batch_size', 'Batch'], format: 'integer', higherBetter: null },
            { key: 'Learning Rate', label: 'Learning Rate', title: 'Learning Rate Comparison', desc: 'Learning rate from training configuration.', formula: 'Learning Rate = optimizer step size coefficient', sources: ['parameterSummary', 'trainingSummary'], aliases: ['Learning Rate', 'learning_rate', 'lr', 'LR'], format: 'scientific', higherBetter: null },
            { key: 'Loss', label: 'Loss', title: 'Loss Comparison', desc: 'Loss value from training or evaluation logs. Lower is better.', formula: 'Loss = objective value at the recorded training point', sources: ['trainingSummary', 'metrics'], aliases: ['Loss', 'Train Loss', 'Validation Loss', 'currentLoss', 'Current Loss'], format: 'decimal', higherBetter: false },
          ],
        },
      ] satisfies MetricGroup[],
    })

const groups = computed<MetricGroup[]>(() => copy.value.groups)
const catalogOffline = ref(false)
const catalogNotice = ref('')

const activeGroup = computed(() => groups.value.find((group) => group.key === selectedGroup.value) || groups.value[0])
const activeMetric = computed(() => activeGroup.value.metrics.find((metric) => metric.key === selectedMetricKey.value) || activeGroup.value.metrics[0])
const loggedModels = computed(() => modelCatalog.value.filter((model) => model.integrationStatus === 'artifact-and-log').length)
const modelCountLabel = computed(() => catalogOffline.value ? '-' : String(modelCatalog.value.length))
const loggedModelsLabel = computed(() => catalogOffline.value ? '-' : String(loggedModels.value))
const totalInteractions = computed(() => catalogOffline.value ? '-' : formatInteger(modelCatalog.value.reduce((sum, model) => sum + datasetMetricNumber(model, 'Interactions'), 0)))

const rawRows = computed<MetricRow[]>(() => {
  const rows = modelCatalog.value.map((model) => metricRowForModel(model, activeMetric.value))
  const available = rows.filter((row) => row.available)
  const max = Math.max(...available.map((row) => row.numeric), 0)
  return rows.map((row) => ({
    ...row,
    percent: row.available && max > 0 ? Math.max(4, Math.round((row.numeric / max) * 100)) : 0,
  }))
})

const availableRows = computed(() => {
  const rows = rawRows.value.filter((row) => row.available)
  if (activeMetric.value.higherBetter === false) return rows.sort((a, b) => a.numeric - b.numeric)
  return rows.sort((a, b) => b.numeric - a.numeric)
})

const displayRows = computed(() => {
  const missingRows = rawRows.value.filter((row) => !row.available).sort((a, b) => a.title.localeCompare(b.title))
  return [...availableRows.value, ...missingRows]
})

const availabilityLabel = computed(() => `${copy.value.availableModels} ${availableRows.value.length}/${modelCatalog.value.length}`)

const statCards = computed(() => {
  const rows = availableRows.value
  const metric = activeMetric.value
  const featured = rows[0]
  const average = rows.length ? rows.reduce((sum, row) => sum + row.numeric, 0) / rows.length : null
  const bestLabel = metric.higherBetter === false ? copy.value.bestLow : metric.higherBetter === true ? copy.value.bestHigh : copy.value.maxValue

  return [
    {
      label: bestLabel,
      value: featured ? featured.display : copy.value.missing,
      desc: featured ? featured.title : copy.value.noDataTitle,
    },
    {
      label: copy.value.average,
      value: average === null ? copy.value.missing : formatMetricNumber(average, metric),
      desc: rows.length ? `${rows.length}/${modelCatalog.value.length}` : copy.value.noDataDesc,
    },
    {
      label: copy.value.coverage,
      value: `${rows.length}/${modelCatalog.value.length}`,
      desc: metric.sources.map((source) => copy.value.sourceMap[source]).join(' / '),
    },
    {
      label: copy.value.definition,
      value: metric.label,
      desc: metric.formula,
      wide: true,
    },
  ]
})

watch(selectedGroup, () => {
  selectedMetricKey.value = activeGroup.value.metrics[0]?.key || 'HR@10'
})

watch([selectedMetricKey, modelCatalog, locale], renderChart, { deep: true })

function metricRowForModel(model: ModelOption, spec: MetricSpec): MetricRow {
  const resolved = resolveMetric(model, spec)
  const available = resolved.raw !== undefined && resolved.raw !== null && displayValue(resolved.raw, '') !== ''
  const numeric = available ? parseMetric(resolved.raw) : 0
  return {
    name: String(model.id || model.name),
    title: modelTitle(model),
    status: localizedStatusText(model),
    source: available && resolved.source ? copy.value.sourceMap[resolved.source] : localizedMetricSourceLabel(model),
    numeric,
    display: available ? formatMetricNumber(numeric, spec) : copy.value.missing,
    available,
    percent: 0,
  }
}

function resolveMetric(model: ModelOption, spec: MetricSpec) {
  for (const source of spec.sources) {
    const raw = strictMetricValue(model[source] as ModelMetricMap | undefined, spec.aliases)
    if (raw !== undefined && raw !== null && displayValue(raw, '') !== '') {
      return { raw, source }
    }
  }
  return { raw: undefined, source: undefined }
}

function strictMetricValue(source: ModelMetricMap | undefined, keys: string[]) {
  if (!source) return undefined
  const exactKey = keys.find((key) => source[key] !== undefined && source[key] !== null)
  if (exactKey) return source[exactKey]
  const normalized = new Map(Object.keys(source).map((key) => [normalizeLookupKey(key), key]))
  const looseKey = keys.map(normalizeLookupKey).map((key) => normalized.get(key)).find(Boolean)
  return looseKey ? source[looseKey] : undefined
}

function normalizeLookupKey(key: string) {
  return key.toLowerCase().replace(/[\s_\-:：]+/g, '')
}

function formatMetricNumber(value: number, spec: MetricSpec) {
  if (!Number.isFinite(value)) return copy.value.missing
  if (spec.format === 'integer') return formatInteger(value)
  if (spec.format === 'scientific') {
    if (value > 0 && value < 0.001) return value.toExponential(2)
    return Number(value.toFixed(6)).toLocaleString('zh-CN')
  }
  return value.toFixed(4)
}

function localizedStatusText(model?: ModelOption) {
  const raw = statusText(model)
  return copy.value.statusMap[raw] || raw
}

function localizedMetricSourceLabel(model?: ModelOption) {
  const raw = metricSourceLabel(model)
  if (isZh.value) return raw
  return raw === '当前目录没有评估日志' ? copy.value.noMetricLog : raw
}

function getChart() {
  if (!chartRef.value) return null
  if (!chart || chart.isDisposed()) {
    chart = echarts.init(chartRef.value)
  }
  return chart
}

function renderChart() {
  void nextTick(() => {
    const instance = getChart()
    if (!instance) return
    const rows = availableRows.value
    const colors = buildChartTheme()
    const axis = chartAxis(colors)
    const metric = activeMetric.value
    instance.setOption({
      backgroundColor: 'transparent',
      tooltip: {
        ...chartTooltip(colors),
        trigger: 'axis',
        axisPointer: { type: 'shadow' },
        formatter: (items: any) => {
          const item = Array.isArray(items) ? items[0] : items
          const row = rows[item?.dataIndex || 0]
          return row ? `${row.title}<br/>${metric.label}: ${row.display}` : ''
        },
      },
      color: [colors.primary],
      grid: chartGrid({ left: 52, right: 24, top: 26, bottom: 72 }),
      xAxis: {
        type: 'category',
        data: rows.map((row) => row.title.replace(' 推荐', '')),
        ...axis,
        axisLabel: { ...axis.axisLabel, rotate: rows.length > 6 ? 24 : 0 },
      },
      yAxis: {
        type: 'value',
        ...axis,
      },
      series: [{
        name: metric.label,
        type: 'bar',
        barMaxWidth: 34,
        data: rows.map((row) => row.numeric),
        itemStyle: {
          borderRadius: [8, 8, 2, 2],
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: colors.primary },
              { offset: 1, color: colors.accent },
            ],
          },
        },
        emphasis: { focus: 'series' },
      }],
    }, true)
  })
}

function resizeChart() {
  chart?.resize()
}

async function loadModels() {
  try {
    const response = await trainingApi.listModels()
    const data = response.data.data
    modelCatalog.value = [...(data.official || []), ...(data.userModels || [])]
    catalogOffline.value = false
    catalogNotice.value = ''
  } catch (error: any) {
    modelCatalog.value = []
    catalogOffline.value = true
    catalogNotice.value = error?.response?.data?.message || error?.message || copy.value.catalogFailed
  } finally {
    renderChart()
  }
}

onMounted(async () => {
  await loadModels()
  window.addEventListener('resize', resizeChart, { passive: true })
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeChart)
  chart?.dispose()
  chart = null
})
</script>

<style scoped>
.reco-visual-lab {
  display: grid;
  gap: 18px;
  color: var(--text-primary);
  --lab-glass-bg:
    linear-gradient(180deg, rgba(255, 255, 255, 0.08), rgba(255, 255, 255, 0.018)),
    color-mix(in srgb, rgba(var(--glass-bg-rgb), var(--glass-opacity)) 78%, transparent);
  --lab-card-bg:
    linear-gradient(180deg, rgba(255, 255, 255, 0.055), transparent),
    color-mix(in srgb, var(--panel-bg) 84%, transparent);
}

.lab-hero,
.metric-workbench,
.comparison-list article {
  border: 1px solid var(--border-color);
  background: var(--lab-glass-bg);
  box-shadow: var(--shadow-soft);
  backdrop-filter: blur(var(--glass-blur));
  -webkit-backdrop-filter: blur(var(--glass-blur));
  transition:
    border-color var(--motion-hover) var(--ease-liquid),
    background var(--motion-medium) var(--ease-smooth),
    box-shadow var(--motion-medium) var(--ease-smooth),
    transform var(--motion-hover) var(--ease-liquid);
}

.lab-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(320px, 0.58fr);
  gap: 20px;
  padding: clamp(22px, 2.4vw, 30px);
  border-radius: 10px;
}

.lab-hero span,
.metric-main-header span,
.stat-grid span {
  color: var(--primary-color);
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.lab-hero h1 {
  margin: 8px 0;
  font-size: clamp(30px, 3.4vw, 48px);
  line-height: 1.05;
}

.lab-hero p {
  max-width: 860px;
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.75;
}

.notice {
  margin: 0;
  padding: 10px 12px;
  border: 1px solid rgba(217, 119, 6, 0.24);
  border-radius: 8px;
  background: rgba(217, 119, 6, 0.08);
  color: var(--text-primary);
  line-height: 1.6;
}

.lab-summary,
.metric-groups,
.metric-tabs,
.stat-grid,
.comparison-list {
  display: grid;
  gap: 10px;
}

.lab-summary {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.lab-summary article,
.metric-groups button,
.stat-grid article {
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--lab-card-bg);
}

.lab-summary article {
  min-width: 0;
  padding: 14px;
}

.lab-summary strong {
  display: block;
  margin-top: 8px;
  font-size: 25px;
}

.metric-workbench {
  display: grid;
  grid-template-columns: minmax(210px, 0.28fr) minmax(0, 1fr);
  gap: 14px;
  padding: 14px;
  border-radius: 10px;
}

.metric-groups {
  align-content: start;
}

.metric-groups button,
.metric-tabs button {
  color: var(--text-primary);
  cursor: pointer;
  transition:
    border-color var(--motion-hover) var(--ease-liquid),
    background var(--motion-medium) var(--ease-smooth),
    box-shadow var(--motion-medium) var(--ease-smooth),
    transform var(--motion-hover) var(--ease-liquid);
}

.metric-groups button {
  min-width: 0;
  padding: 15px;
  text-align: left;
}

.metric-groups strong,
.metric-groups span {
  display: block;
}

.metric-groups span {
  margin-top: 6px;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.45;
}

.metric-groups button.active,
.metric-tabs button.active {
  border-color: rgba(var(--primary-rgb), 0.42);
  background: rgba(var(--primary-rgb), 0.12);
  box-shadow: var(--shadow-ring);
}

.metric-main {
  min-width: 0;
  display: grid;
  gap: 14px;
}

.metric-main-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.metric-main-header strong {
  display: block;
  margin-top: 6px;
  font-size: clamp(22px, 2.4vw, 30px);
  line-height: 1.1;
}

.metric-main-header p {
  max-width: 720px;
  margin: 8px 0 0;
  color: var(--text-secondary);
  line-height: 1.65;
}

.metric-main-header b {
  align-self: start;
  flex: 0 0 auto;
  padding: 8px 11px;
  border: 1px solid rgba(var(--primary-rgb), 0.24);
  border-radius: 999px;
  background: rgba(var(--primary-rgb), 0.09);
  color: var(--primary-color);
  font-size: 12px;
  white-space: nowrap;
}

.metric-tabs {
  grid-template-columns: repeat(4, minmax(0, max-content));
  justify-content: start;
  overflow-x: auto;
  padding-bottom: 2px;
}

.metric-tabs button {
  min-width: 82px;
  padding: 9px 13px;
  border: 1px solid var(--border-color);
  border-radius: 999px;
  background: color-mix(in srgb, var(--surface-1) 70%, transparent);
  font-weight: 800;
  white-space: nowrap;
}

.chart-shell {
  position: relative;
  min-height: 360px;
  overflow: hidden;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background:
    radial-gradient(circle at 10% 0%, rgba(var(--primary-rgb), 0.08), transparent 32%),
    color-mix(in srgb, var(--surface-1) 58%, transparent);
}

.chart-box {
  width: 100%;
  height: 360px;
  transition: opacity var(--motion-medium) var(--ease-smooth);
}

.chart-box.muted {
  opacity: 0.22;
}

.empty-state {
  position: absolute;
  inset: 0;
  display: grid;
  place-content: center;
  gap: 8px;
  padding: 24px;
  text-align: center;
  pointer-events: none;
}

.empty-state strong {
  font-size: 18px;
}

.empty-state span {
  max-width: 420px;
  color: var(--text-secondary);
  line-height: 1.6;
}

.stat-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.stat-grid article {
  min-width: 0;
  padding: 14px;
}

.stat-grid article.wide {
  grid-column: span 1;
}

.stat-grid strong {
  display: block;
  margin-top: 7px;
  overflow: hidden;
  font-size: 22px;
  line-height: 1.15;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.stat-grid p {
  margin: 7px 0 0;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.55;
}

.comparison-list {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.comparison-list article {
  min-width: 0;
  padding: 15px;
  border-radius: 8px;
}

.comparison-list article.missing {
  opacity: 0.72;
}

.comparison-list header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.comparison-list header strong,
.comparison-list header span {
  display: block;
}

.comparison-list header span {
  margin-top: 4px;
  overflow: hidden;
  color: var(--text-secondary);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.comparison-list header b {
  flex: 0 0 auto;
  color: var(--primary-color);
  font-size: 15px;
  white-space: nowrap;
}

.row-meter {
  height: 7px;
  overflow: hidden;
  border-radius: 999px;
  background: color-mix(in srgb, var(--surface-1) 76%, transparent);
}

.row-meter i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, var(--primary-color), var(--accent-color));
}

@media (hover: hover) and (pointer: fine) {
  .lab-hero:hover,
  .metric-workbench:hover,
  .comparison-list article:hover {
    border-color: color-mix(in srgb, var(--primary-color) 34%, var(--border-color));
    box-shadow: var(--shadow-hover);
    transform: translate3d(0, -1px, 0);
  }

  .metric-groups button:hover,
  .metric-tabs button:hover {
    border-color: rgba(var(--primary-rgb), 0.42);
    background: rgba(var(--primary-rgb), 0.1);
    box-shadow: var(--shadow-ring);
    transform: translate3d(0, -1px, 0);
  }
}

@media (prefers-reduced-motion: reduce) {
  .lab-hero,
  .metric-workbench,
  .comparison-list article,
  .metric-groups button,
  .metric-tabs button,
  .chart-box {
    transition: none;
    transform: none !important;
  }
}

@media (max-width: 1180px) {
  .lab-hero,
  .metric-workbench {
    grid-template-columns: 1fr;
  }

  .metric-groups {
    grid-template-columns: repeat(4, minmax(170px, 1fr));
    overflow-x: auto;
  }

  .lab-summary,
  .stat-grid,
  .comparison-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 680px) {
  .lab-summary,
  .metric-groups,
  .stat-grid,
  .comparison-list {
    grid-template-columns: 1fr;
  }

  .metric-main-header {
    display: grid;
  }

  .chart-shell,
  .chart-box {
    min-height: 300px;
    height: 300px;
  }
}
</style>
