import type { ModelMetricMap, ModelOption } from '@/types/models'

export type ModelDataAsset = {
  Name?: string
  Path?: string
  Format?: string
  'User scale'?: string | number
  'Item scale'?: string | number
  Interactions?: string | number
  Records?: string | number
  'Avg interactions/user'?: string | number
  Density?: string | number
  'Rating range'?: string
  'Rating mean'?: string | number
  'Timestamp range'?: string
  Status?: string
  名称?: string
  路径?: string
  格式?: string
  用户数?: string | number
  物品数?: string | number
  交互数?: string | number
  状态?: string
}

export function modelKey(model: ModelOption) {
  return String(model.id ?? model.name)
}

export function modelTitle(model?: ModelOption) {
  if (!model) return '-'
  return model.displayNameZh || model.name
}

export function statusText(model?: ModelOption) {
  if (!model) return '-'
  if (model.online || model.integrationStatus === 'online' || model.statusLabel === '在线') return '在线'
  if (model.statusLabel && !['service-offline', 'not-served', 'online'].includes(model.statusLabel)) {
    return model.statusLabel
  }
  if (model.canExecute) return '服务离线'
  const labels: Record<string, string> = {
    'service-offline': '服务离线',
    'artifact-and-log': '权重+日志',
    'artifact-only': '仅有权重',
    'code-and-data': '代码+数据',
    'data-only': '仅有数据',
    'missing-data': '缺少数据',
  }
  return labels[String(model.integrationStatus || '')] || '未注册推理'
}

export function statusTone(model?: ModelOption) {
  if (!model) return 'muted'
  if (model.online) return 'online'
  if (model.canExecute) return 'service'
  if (model.integrationStatus === 'artifact-and-log' || model.integrationStatus === 'artifact-only') return 'artifact'
  if (model.datasetExists) return 'data'
  return 'offline'
}

export function displayValue(value: unknown, fallback = '暂无') {
  if (value === null || value === undefined) return fallback
  const text = String(value).trim()
  return !text || text.toUpperCase() === 'TBD' || text.toUpperCase() === 'N/A' ? fallback : text
}

export function parseMetric(value: unknown) {
  if (typeof value === 'number') return Number.isFinite(value) ? value : 0
  const text = displayValue(value, '0')
  const matches = text.replace(/,/g, '').replace(/%/g, '').match(/-?\d+(?:\.\d+)?/g)
  const numeric = Number.parseFloat(matches?.[matches.length - 1] || '0')
  return Number.isFinite(numeric) ? numeric : 0
}

const keyAliases: Record<string, string[]> = {
  'HR@10': ['HR@10', 'HIT@10', 'Hit rate@10'],
  'HR@20': ['HR@20', 'HIT@20', 'Hit rate@20'],
  'Recall@10': ['Recall@10', 'Recall @ 10', 'RECALL@10'],
  'Recall@20': ['Recall@20', 'Recall @ 20', 'RECALL@20'],
  AUC: ['AUC', 'ROC AUC', 'AUC@10'],
  RMSE: ['RMSE', 'Root Mean Square Error'],
  MAE: ['MAE', 'Mean Absolute Error'],
  Loss: ['Loss', 'loss', 'Train Loss', 'Validation Loss', 'currentLoss', 'Current Loss'],
  'Batch Size': ['Batch Size', 'Batch size', 'batch_size', 'Batch'],
  'Learning Rate': ['Learning Rate', 'learning_rate', 'Learning rate', 'lr', 'LR'],
  MRR: ['MRR', 'MRR@10', 'Mean reciprocal rank'],
  'NDCG@10': ['NDCG@10', 'NDCG @ 10'],
  'NDCG@20': ['NDCG@20', 'NDCG @ 20'],
  Dataset: ['Dataset', '数据集', '数据集名称'],
  'Data source': ['Data source', '数据来源', '数据源'],
  'Data format': ['Data format', '格式', '数据格式'],
  'User scale': ['User scale', 'Users', 'User count', '用户数', '用户规模'],
  'Item scale': ['Item scale', 'Items', 'Item count', '物品数', '商品数', '物品规模'],
  Interactions: ['Interactions', 'Interaction count', '交互数', '交互规模'],
  'Avg interactions/user': ['Avg interactions/user', 'Average interactions per user', 'Avg/User', '人均交互'],
  Density: ['Density', 'Dataset density', 'Sparsity complement', '稀疏度', '稠密度'],
  Records: ['Records', 'Rows', 'Record count', '记录数', '行数'],
  'Max sequence length': ['Max sequence length', 'Max sequence', '最大序列长度', '最大序列'],
  'Dataset readiness': ['Dataset readiness', 'Data readiness', '数据就绪度'],
  'Artifact readiness': ['Artifact readiness', 'Weight readiness', 'Artifacts readiness', '权重就绪度'],
  'Metric provenance': ['Metric provenance', 'Metric readiness', 'Log readiness', '指标可信度', '日志就绪度'],
  'Code readiness': ['Code readiness', '代码就绪度'],
  'Service readiness': ['Service readiness', '服务就绪度'],
}

function normalizeLookupKey(key: string) {
  return key.toLowerCase().replace(/[\s_\-:：]+/g, '')
}

function expandKeys(keys: string | string[]) {
  const seed = Array.isArray(keys) ? keys : [keys]
  const expanded = seed.flatMap((key) => keyAliases[key] || [key])
  return [...new Set(expanded)]
}

export function metricValue(source: ModelMetricMap | undefined, keys: string | string[]) {
  if (!source) return undefined
  const candidates = expandKeys(keys)
  const exactKey = candidates.find((item) => source[item] !== undefined && source[item] !== null)
  if (exactKey) return source[exactKey]
  const normalized = new Map(Object.keys(source).map((key) => [normalizeLookupKey(key), key]))
  const looseKey = candidates.map(normalizeLookupKey).map((key) => normalized.get(key)).find(Boolean)
  return looseKey ? source[looseKey] : undefined
}

export function modelMetricNumber(model: ModelOption, keys: string | string[]) {
  return parseMetric(metricValue(model.metrics, keys))
}

export function datasetMetricValue(model: ModelOption | undefined, keys: string | string[]) {
  return metricValue(model?.datasetSummary, keys)
}

export function datasetMetricNumber(model: ModelOption, keys: string | string[]) {
  return parseMetric(datasetMetricValue(model, keys))
}

export function readinessNumber(model: ModelOption, keys: string | string[]) {
  return parseMetric(metricValue(model.visualProfile, keys))
}

export function formatInteger(value: unknown) {
  const number = typeof value === 'number' ? value : parseMetric(value)
  return Number(number || 0).toLocaleString('zh-CN')
}

export function formatDecimal(value: unknown, digits = 4) {
  const number = typeof value === 'number' ? value : parseMetric(value)
  return Number.isFinite(number) && number > 0 ? number.toFixed(digits) : '暂无'
}

export function dataAssets(model?: ModelOption): ModelDataAsset[] {
  const raw = (model as any)?.dataAssets
  return Array.isArray(raw) ? raw as ModelDataAsset[] : []
}

export function assetValue(asset: Record<string, unknown>, keys: string | string[], fallback = '') {
  const candidates = expandKeys(keys)
  for (const key of candidates) {
    const value = asset[key]
    if (value !== null && value !== undefined && String(value).trim()) return String(value)
  }
  const normalized = new Map(Object.keys(asset).map((key) => [normalizeLookupKey(key), key]))
  for (const key of candidates.map(normalizeLookupKey)) {
    const sourceKey = normalized.get(key)
    const value = sourceKey ? asset[sourceKey] : undefined
    if (value !== null && value !== undefined && String(value).trim()) return String(value)
  }
  return fallback
}

export function primaryDatasetLabel(model?: ModelOption) {
  return displayValue(model?.datasetSummary?.Dataset, '未登记数据集')
}

export function metricSourceLabel(model?: ModelOption) {
  return displayValue(model?.metricSource, '当前目录没有评估日志')
}

export function recommendationMetrics(model: ModelOption) {
  return [
    { key: 'HR@10', label: 'HR@10', value: metricValue(model.metrics, ['HR@10', 'HIT@10']) },
    { key: 'NDCG@10', label: 'NDCG@10', value: metricValue(model.metrics, 'NDCG@10') },
    { key: 'HR@20', label: 'HR@20 / HIT@5', value: metricValue(model.metrics, ['HR@20', 'HIT@5']) },
    { key: 'MRR', label: 'MRR', value: metricValue(model.metrics, 'MRR') },
  ].map((item) => ({
    ...item,
    display: displayValue(item.value, '无日志'),
    numeric: parseMetric(item.value),
  }))
}

export function normalizePercent(value: number, max: number, minimum = 4) {
  if (!Number.isFinite(value) || value <= 0 || !Number.isFinite(max) || max <= 0) return minimum
  return Math.max(minimum, Math.min(100, Math.round((value / max) * 100)))
}
