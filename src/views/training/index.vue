<template>
  <main class="reco-page">
    <section class="overview-hero">
      <div class="hero-copy">
        <span>{{ copy.heroEyebrow }}</span>
        <h1>{{ copy.title }}</h1>
        <p>{{ copy.subtitle }}</p>
      </div>
      <div class="hero-metrics">
        <article>
          <span>{{ copy.modelFamilies }}</span>
          <strong>{{ modelCatalog.length }}</strong>
          <em>{{ copy.fromModelDir }}</em>
        </article>
        <article>
          <span>{{ copy.realData }}</span>
          <strong>{{ modelsWithData }}</strong>
          <em>{{ copy.readableData }}</em>
        </article>
        <article>
          <span>{{ copy.artifactLogs }}</span>
          <strong>{{ modelsWithArtifacts }}</strong>
          <em>{{ copy.artifactLogsHint }}</em>
        </article>
        <article>
          <span>{{ copy.totalInteractions }}</span>
          <strong>{{ totalInteractionsLabel }}</strong>
          <em>{{ copy.fromDatasetScan }}</em>
        </article>
      </div>
    </section>

    <section class="overview-shell">
      <aside class="model-rail">
        <header>
          <span>{{ copy.modelFamilies }}</span>
          <strong>{{ modelCatalog.length }}</strong>
        </header>
        <button
          v-for="model in modelCatalog"
          :key="modelKey(model)"
          type="button"
          class="model-option"
          :class="[statusTone(model), { active: selectedKey === modelKey(model) }]"
          @click="selectedKey = modelKey(model)"
        >
          <i aria-hidden="true"></i>
          <span>
            <strong>{{ modelTitle(model) }}</strong>
            <em>{{ model.architecture || model.framework }}</em>
          </span>
          <b>{{ localizedStatusText(model) }}</b>
        </button>
      </aside>

      <section v-if="selectedModel" class="model-workbench">
        <header class="model-header">
          <div>
            <span>{{ selectedModel.architecture || selectedModel.framework }}</span>
            <h2>{{ modelTitle(selectedModel) }}</h2>
            <p>{{ localizedDescription(selectedModel) }}</p>
          </div>
          <div class="status-pill" :class="statusTone(selectedModel)">
            <strong>{{ localizedStatusText(selectedModel) }}</strong>
            <em>{{ selectedModel.canExecute ? copy.backendProxyAvailable : copy.noBackendInference }}</em>
          </div>
        </header>

        <section class="fact-strip">
          <article>
            <span>{{ copy.dataset }}</span>
            <strong>{{ localizedPrimaryDatasetLabel(selectedModel) }}</strong>
          </article>
          <article>
            <span>{{ copy.interactions }}</span>
            <strong>{{ valueText(selectedModel.datasetSummary?.Interactions) }}</strong>
          </article>
          <article>
            <span>{{ copy.users }}</span>
            <strong>{{ valueText(selectedModel.datasetSummary?.['User scale']) }}</strong>
          </article>
          <article>
            <span>{{ copy.items }}</span>
            <strong>{{ valueText(selectedModel.datasetSummary?.['Item scale']) }}</strong>
          </article>
          <article>
            <span>{{ copy.avgInteractions }}</span>
            <strong>{{ valueText(selectedModel.datasetSummary?.['Avg interactions/user']) }}</strong>
          </article>
          <article>
            <span>{{ copy.density }}</span>
            <strong>{{ valueText(selectedModel.datasetSummary?.Density) }}</strong>
          </article>
          <article>
            <span>{{ copy.ratingRange }}</span>
            <strong>{{ valueText(selectedModel.datasetSummary?.['Rating range']) }}</strong>
          </article>
          <article>
            <span>{{ copy.timeRange }}</span>
            <strong>{{ valueText(selectedModel.datasetSummary?.['Timestamp range']) }}</strong>
          </article>
        </section>

        <section class="detail-grid">
          <article class="panel metric-panel">
            <div class="panel-title">
              <span>{{ copy.recommendationMetrics }}</span>
              <strong>{{ localizedMetricSourceLabel(selectedModel) }}</strong>
            </div>
            <div class="metric-cells">
              <div v-for="metric in metricsForSelected" :key="metric.label">
                <span>{{ metric.label }}</span>
                <strong>{{ metric.display }}</strong>
              </div>
            </div>
          </article>

          <article class="panel readiness-panel">
            <div class="panel-title">
              <span>{{ copy.readiness }}</span>
              <strong>{{ selectedModel.statusReason || copy.noStatusReason }}</strong>
            </div>
            <div class="readiness-list">
              <div v-for="row in readinessRows" :key="row.label">
                <div>
                  <span>{{ row.label }}</span>
                  <strong>{{ row.display }}</strong>
                </div>
                <i><b :style="{ width: row.percent + '%' }"></b></i>
              </div>
            </div>
          </article>
        </section>

        <section class="detail-grid">
          <article class="panel">
            <div class="panel-title">
              <span>{{ copy.dataAssets }}</span>
              <strong>{{ selectedAssets.length ? `${selectedAssets.length} ${copy.assetFilesUnit}` : copy.familySummary }}</strong>
            </div>
            <div class="asset-list">
              <div v-for="asset in selectedAssetRows" :key="asset.name + asset.path">
                <strong>{{ asset.name }}</strong>
                <span>{{ asset.format }}</span>
                <em>{{ asset.interactions }} {{ copy.interactionsUnit }} / {{ asset.users }} {{ copy.usersUnit }} / {{ asset.items }} {{ copy.itemsUnit }}</em>
                <small>{{ copy.avgInteractions }} {{ asset.avg }} · {{ copy.density }} {{ asset.density }}</small>
                <small v-if="asset.rating !== copy.valueFallback || asset.time !== copy.valueFallback">
                  {{ copy.ratingRange }} {{ asset.rating }} · {{ copy.timeRange }} {{ asset.time }}
                </small>
              </div>
              <div v-if="!selectedAssets.length" class="empty-note">{{ copy.noAssetList }}</div>
            </div>
          </article>

          <article class="panel">
            <div class="panel-title">
              <span>{{ copy.trainingAndAccess }}</span>
              <strong>{{ copy.realRegisteredOnly }}</strong>
            </div>
            <div class="kv-grid">
              <div v-for="row in trainingRows" :key="row.label">
                <span>{{ row.label }}</span>
                <strong>{{ row.value }}</strong>
              </div>
            </div>
          </article>
        </section>
      </section>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { trainingApi } from '@/api'
import type { ModelOption } from '@/types/models'
import {
  assetValue,
  dataAssets,
  datasetMetricNumber,
  displayValue,
  formatInteger,
  metricSourceLabel,
  modelKey,
  modelTitle,
  primaryDatasetLabel,
  readinessNumber,
  recommendationMetrics,
  statusText,
  statusTone,
} from '@/utils/recommenderCatalog'

type Row = { label: string; value: string | number; display?: string; percent?: number }

const { locale } = useI18n()
const isZh = computed(() => !locale.value.startsWith('en'))
const modelCatalog = ref<ModelOption[]>([])
const selectedKey = ref('')

const copy = computed(() => isZh.value
  ? {
      title: '推荐系统模型总览',
      heroEyebrow: '推荐模型实验室',
      subtitle: '按推荐模型家族查看数据、权重日志、可用指标和推理接入状态，九个模型统一放在推荐系统视角下展示。',
      modelFamilies: '模型家族',
      fromModelDir: '来自 model 目录',
      realData: '数据资产',
      readableData: '存在可读取数据文件',
      artifactLogs: '权重日志',
      artifactLogsHint: '具备权重或评估记录',
      totalInteractions: '总交互',
      fromDatasetScan: '来自数据集扫描',
      backendProxyAvailable: '可走后端推荐接口',
      noBackendInference: '等待服务接入',
      dataset: '数据集',
      interactions: '交互数',
      users: '用户数',
      items: '物品数',
      avgInteractions: '人均交互',
      density: '稠密度',
      ratingRange: '评分范围',
      timeRange: '时间范围',
      recommendationMetrics: '推荐指标',
      readiness: '接入证据',
      noStatusReason: '状态待更新',
      dataAssets: '数据资产',
      assetFilesUnit: '个数据文件',
      familySummary: '按家族汇总',
      dataFile: '数据文件',
      recommendationData: '推荐数据',
      interactionsUnit: '交互',
      usersUnit: '用户',
      itemsUnit: '物品',
      noAssetList: '该模型返回的是汇总数据，暂无单独文件列表。',
      trainingAndAccess: '训练与接入',
      realRegisteredOnly: '训练记录与服务状态',
      valueFallback: '暂无',
      noDataset: '未登记数据集',
      noMetricLog: '暂无评估日志',
      noLog: '无日志',
      found: '已发现',
      missing: '缺失',
      registered: '已登记',
      unregistered: '未登记',
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
    }
  : {
      title: 'Recommender Model Overview',
      heroEyebrow: 'Recommendation Lab',
      subtitle: 'Review datasets, artifacts, log status, metrics, and inference access by recommender model family.',
      modelFamilies: 'Model families',
      fromModelDir: 'From the model directory',
      realData: 'Data assets',
      readableData: 'Readable data files exist',
      artifactLogs: 'Artifacts / logs',
      artifactLogsHint: 'Weights or evaluation records exist',
      totalInteractions: 'Interactions',
      fromDatasetScan: 'From dataset scan',
      backendProxyAvailable: 'Backend recommender proxy available',
      noBackendInference: 'No backend inference registered',
      dataset: 'Dataset',
      interactions: 'Interactions',
      users: 'Users',
      items: 'Items',
      avgInteractions: 'Avg/User',
      density: 'Density',
      ratingRange: 'Rating',
      timeRange: 'Time range',
      recommendationMetrics: 'Recommendation metrics',
      readiness: 'Access evidence',
      noStatusReason: 'No status note',
      dataAssets: 'Data assets',
      assetFilesUnit: 'data files',
      familySummary: 'Family summary',
      dataFile: 'Data file',
      recommendationData: 'Recommendation data',
      interactionsUnit: 'interactions',
      usersUnit: 'users',
      itemsUnit: 'items',
      noAssetList: 'This model only returned summary data; no separate data-file list is available.',
      trainingAndAccess: 'Training and access',
      realRegisteredOnly: 'Only real registered entries are shown',
      valueFallback: 'None',
      noDataset: 'No dataset registered',
      noMetricLog: 'No evaluation log in the current directory',
      noLog: 'No log',
      found: 'Found',
      missing: 'Missing',
      registered: 'Registered',
      unregistered: 'Not registered',
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
    })

const selectedModel = computed(() => modelCatalog.value.find((model) => modelKey(model) === selectedKey.value) || modelCatalog.value[0])
const selectedAssets = computed(() => dataAssets(selectedModel.value))
const modelsWithData = computed(() => modelCatalog.value.filter((model) => model.datasetExists).length)
const modelsWithArtifacts = computed(() => modelCatalog.value.filter((model) => model.artifactExists || model.integrationStatus === 'artifact-and-log').length)
const metricsForSelected = computed(() => selectedModel.value ? recommendationMetrics(selectedModel.value) : [])
const totalInteractionsLabel = computed(() => formatInteger(modelCatalog.value.reduce((sum, model) => sum + datasetMetricNumber(model, 'Interactions'), 0)))
const selectedAssetRows = computed(() => {
  const model = selectedModel.value
  if (!model) return []
  return dataAssets(model).map((asset) => ({
    name: assetValue(asset, ['Name', '名称', 'name'], copy.value.dataFile),
    path: assetValue(asset, ['Path', '路径', 'path'], ''),
    format: assetValue(asset, ['Format', '格式', 'format'], copy.value.recommendationData),
    users: assetValue(asset, ['User scale', '用户数', 'users'], '0'),
    items: assetValue(asset, ['Item scale', '物品数', 'items'], '0'),
    interactions: assetValue(asset, ['Interactions', '交互数', 'interactions'], '0'),
    avg: assetValue(asset, ['Avg interactions/user', '人均交互'], copy.value.valueFallback),
    density: assetValue(asset, ['Density', '稠密度'], copy.value.valueFallback),
    rating: assetValue(asset, ['Rating range', '评分范围'], copy.value.valueFallback),
    time: assetValue(asset, ['Timestamp range', '时间范围'], copy.value.valueFallback),
  }))
})

const readinessRows = computed<Row[]>(() => {
  const model = selectedModel.value
  if (!model) return []
  return [
    evidenceRow(isZh.value ? '数据' : 'Data', readinessNumber(model, ['数据就绪度', 'Dataset readiness'])),
    evidenceRow(isZh.value ? '权重' : 'Artifacts', readinessNumber(model, ['权重就绪度', 'Artifact readiness'])),
    evidenceRow(isZh.value ? '日志' : 'Logs', readinessNumber(model, ['指标可信度', 'Metric provenance'])),
    evidenceRow(isZh.value ? '代码' : 'Code', readinessNumber(model, ['代码就绪度', 'Code readiness'])),
    evidenceRow(isZh.value ? '服务' : 'Service', readinessNumber(model, ['服务就绪度', 'Service readiness']), copy.value.registered, copy.value.unregistered),
  ]
})

const trainingRows = computed<Row[]>(() => {
  const source = selectedModel.value?.trainingSummary || {}
  return Object.entries(source).map(([label, value]) => ({ label, value: displayValue(value, copy.value.valueFallback) }))
})

function valueText(value: unknown, fallback = copy.value.valueFallback) {
  return displayValue(value, fallback)
}

function evidenceRow(label: string, rawValue: number, positive = copy.value.found, negative = copy.value.missing): Row {
  return {
    label,
    value: rawValue,
    display: rawValue > 0 ? positive : negative,
    percent: rawValue > 0 ? 100 : 0,
  }
}

function localizedStatusText(model?: ModelOption) {
  const raw = statusText(model)
  return copy.value.statusMap[raw] || raw
}

function localizedDescription(model: ModelOption) {
  if (isZh.value) return model.descriptionZh || model.description || model.statusReason || copy.value.noStatusReason
  return model.description || model.descriptionZh || model.statusReason || copy.value.noStatusReason
}

function localizedMetricSourceLabel(model?: ModelOption) {
  const raw = metricSourceLabel(model)
  if (isZh.value) return raw
  return raw === '当前目录没有评估日志' ? copy.value.noMetricLog : raw
}

function localizedPrimaryDatasetLabel(model?: ModelOption) {
  const raw = primaryDatasetLabel(model)
  if (isZh.value) return raw
  return raw === '未登记数据集' ? copy.value.noDataset : raw
}

async function loadModels() {
  const response = await trainingApi.listModels()
  const data = response.data.data
  modelCatalog.value = [...(data.official || []), ...(data.userModels || [])]
  selectedKey.value = modelCatalog.value[0] ? modelKey(modelCatalog.value[0]) : ''
}

onMounted(loadModels)
</script>

<style scoped>
.reco-page {
  width: min(1480px, 100%);
  min-height: calc(100dvh - var(--header-height, 72px));
  margin: 0 auto;
  padding: 22px;
  color: var(--text-primary);
  --training-glass-bg:
    linear-gradient(180deg, rgba(255, 255, 255, 0.08), rgba(255, 255, 255, 0.018)),
    color-mix(in srgb, rgba(var(--glass-bg-rgb), var(--glass-opacity)) 78%, transparent);
  --training-card-bg:
    linear-gradient(180deg, rgba(255, 255, 255, 0.055), transparent),
    color-mix(in srgb, var(--panel-bg) 84%, transparent);
}

.overview-hero,
.overview-shell,
.model-rail,
.model-workbench,
.panel {
  border: 1px solid var(--border-color);
  background: var(--training-glass-bg);
  box-shadow: var(--shadow-soft);
  backdrop-filter: blur(var(--glass-blur)) saturate(var(--glass-saturate));
  -webkit-backdrop-filter: blur(var(--glass-blur)) saturate(var(--glass-saturate));
  transition:
    border-color var(--motion-hover) var(--ease-liquid),
    background var(--motion-medium) var(--ease-smooth),
    box-shadow var(--motion-medium) var(--ease-smooth);
}

.overview-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(360px, 0.72fr);
  gap: 16px;
  margin: 0 auto 16px;
  padding: 22px;
  border-radius: 8px;
}

.hero-copy span,
.panel-title span,
.model-header span,
.model-rail header span {
  color: var(--primary-color);
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.hero-copy h1 {
  margin: 8px 0;
  font-size: clamp(32px, 4vw, 52px);
  line-height: 1;
}

.hero-copy p {
  max-width: 820px;
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.75;
}

.hero-metrics,
.fact-strip,
.detail-grid,
.metric-cells,
.kv-grid {
  display: grid;
  gap: 10px;
}

.hero-metrics {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.hero-metrics article,
.fact-strip article,
.metric-cells div,
.kv-grid div,
.asset-list div {
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--training-card-bg);
}

.hero-metrics article,
.fact-strip article,
.metric-cells div,
.kv-grid div {
  padding: 13px;
}

.hero-metrics span,
.fact-strip span,
.metric-cells span,
.kv-grid span,
.asset-list span,
.asset-list em {
  display: block;
  color: var(--text-secondary);
  font-size: 12px;
  font-style: normal;
}

.hero-metrics strong {
  display: block;
  margin: 8px 0;
  font-size: 28px;
}

.overview-shell {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 14px;
  margin: 0 auto;
  padding: 14px;
  border-radius: 8px;
}

.model-rail,
.model-workbench {
  border-radius: 8px;
}

.model-rail {
  padding: 12px;
}

.model-rail header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  padding: 6px 6px 12px;
}

.model-rail header strong {
  color: var(--primary-color);
  font-size: 24px;
}

.model-option {
  width: 100%;
  display: grid;
  grid-template-columns: 8px minmax(0, 1fr) auto;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
  padding: 11px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--training-card-bg);
  color: var(--text-primary);
  text-align: left;
  cursor: pointer;
  transition:
    border-color var(--motion-hover) var(--ease-liquid),
    background var(--motion-medium) var(--ease-smooth),
    box-shadow var(--motion-medium) var(--ease-smooth),
    transform var(--motion-hover) var(--ease-liquid);
}

.model-option.active {
  border-color: rgba(var(--primary-rgb), 0.42);
  background: rgba(var(--primary-rgb), 0.12);
}

.model-option i {
  width: 8px;
  height: 38px;
  border-radius: 99px;
  background: var(--text-muted);
}

.model-option.online i { background: var(--primary-color); }
.model-option.service i { background: var(--warning-glow); }
.model-option.artifact i { background: var(--accent-glow); }
.model-option.data i { background: color-mix(in srgb, var(--primary-color) 62%, var(--accent-glow)); }
.model-option.offline i { background: var(--danger-glow); }

.model-option strong,
.model-option em {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.model-option em {
  color: var(--text-secondary);
  font-size: 12px;
  font-style: normal;
}

.model-option b {
  min-width: 0;
  overflow: hidden;
  color: var(--primary-color);
  font-size: 12px;
  text-align: right;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.model-workbench {
  min-width: 0;
  display: grid;
  gap: 12px;
  padding: 16px;
}

.model-header {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 170px;
  gap: 16px;
}

.model-header h2 {
  margin: 7px 0;
  font-size: clamp(26px, 3vw, 38px);
  line-height: 1.05;
}

.model-header p {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.7;
}

.status-pill {
  display: grid;
  place-items: center;
  align-content: center;
  min-height: 116px;
  border: 1px solid rgba(var(--primary-rgb), 0.26);
  border-radius: 8px;
  background: rgba(var(--primary-rgb), 0.1);
}

.status-pill strong {
  max-width: 100%;
  overflow: hidden;
  color: var(--primary-color);
  font-size: 22px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-pill em {
  max-width: 100%;
  overflow: hidden;
  margin-top: 6px;
  color: var(--text-secondary);
  font-size: 12px;
  font-style: normal;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.fact-strip {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.fact-strip strong,
.metric-cells strong,
.kv-grid strong {
  display: block;
  margin-top: 7px;
  font-size: 18px;
  word-break: break-word;
}

.detail-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.panel {
  min-width: 0;
  padding: 15px;
  border-radius: 8px;
}

.panel-title {
  margin-bottom: 12px;
}

.panel-title strong {
  display: block;
  margin-top: 6px;
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 600;
  line-height: 1.6;
}

.metric-cells,
.kv-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.readiness-list {
  display: grid;
  gap: 11px;
}

.readiness-list div div {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 6px;
}

.readiness-list span {
  color: var(--text-secondary);
  font-size: 12px;
}

.readiness-list strong {
  color: var(--primary-color);
  font-size: 12px;
}

.readiness-list i {
  display: block;
  height: 9px;
  overflow: hidden;
  border-radius: 99px;
  background: color-mix(in srgb, var(--surface-1) 72%, transparent);
}

.readiness-list b {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--primary-color);
}

.asset-list {
  display: grid;
  gap: 8px;
  max-height: 280px;
  overflow: auto;
}

.asset-list div {
  padding: 12px;
}

.asset-list strong {
  display: block;
}

.asset-list em {
  margin-top: 5px;
}

.asset-list small {
  display: block;
  margin-top: 5px;
  color: var(--text-muted);
  font-size: 11px;
  line-height: 1.45;
}

.empty-note {
  color: var(--text-secondary);
}

@media (hover: hover) and (pointer: fine) {
  .overview-hero:hover,
  .overview-shell:hover,
  .model-rail:hover,
  .model-workbench:hover,
  .panel:hover {
    border-color: color-mix(in srgb, var(--primary-color) 34%, var(--border-color));
    box-shadow: var(--shadow-hover);
  }

  .model-option:hover {
    border-color: color-mix(in srgb, var(--primary-color) 42%, var(--border-color));
    background: rgba(var(--primary-rgb), 0.1);
    box-shadow: var(--shadow-ring);
    transform: translate3d(0, -1px, 0);
  }
}

@media (prefers-reduced-motion: reduce) {
  .overview-hero,
  .overview-shell,
  .model-rail,
  .model-workbench,
  .panel,
  .model-option {
    transition: none;
    transform: none !important;
  }
}

@media (max-width: 1180px) {
  .overview-hero,
  .overview-shell,
  .model-header,
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .model-rail {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 8px;
  }

  .model-rail header {
    grid-column: 1 / -1;
  }

  .model-option {
    margin-bottom: 0;
  }
}

@media (max-width: 720px) {
  .reco-page {
    padding: 14px;
  }

  .hero-metrics,
  .model-rail,
  .fact-strip,
  .metric-cells,
  .kv-grid {
    grid-template-columns: 1fr;
  }
}
</style>
