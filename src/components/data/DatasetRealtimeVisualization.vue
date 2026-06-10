<template>
  <section class="dataset-realtime" :class="{ embedded }">
    <div class="dataset-hero">
      <div>
        <span>{{ copy.heroEyebrow }}</span>
        <h1>{{ copy.title }}</h1>
        <p>{{ copy.subtitle }}</p>
      </div>
      <div class="hero-actions">
        <button type="button" :disabled="modelLoading || historyLoading" @click="refreshAll">
          <RefreshCw :size="16" />
          {{ modelLoading || historyLoading ? copy.refreshing : copy.refreshAssets }}
        </button>
        <button type="button" @click="focusUpload">
          <Upload :size="16" />
          {{ copy.uploadRecoData }}
        </button>
      </div>
    </div>

    <p v-if="notice" class="notice">{{ notice }}</p>

    <section class="summary-grid">
      <article v-for="card in summaryCards" :key="card.label">
        <span>{{ card.label }}</span>
        <strong>{{ card.value }}</strong>
        <em>{{ card.hint }}</em>
      </article>
    </section>

    <section class="data-layout">
      <aside class="model-index">
        <header>
          <div>
            <span>{{ copy.modelDataDirectory }}</span>
            <strong>{{ modelCatalog.length }} {{ copy.modelsUnit }}</strong>
          </div>
          <b>{{ modelAssetCount }} {{ copy.assetGroupsUnit }}</b>
        </header>
        <button
          v-for="model in modelCatalog"
          :key="modelKey(model)"
          type="button"
          :class="[statusTone(model), { active: selectedKey === modelKey(model) }]"
          @click="selectedKey = modelKey(model)"
        >
          <i aria-hidden="true"></i>
          <span>
            <strong>{{ modelTitle(model) }}</strong>
            <em>{{ primaryDatasetLabel(model) }}</em>
          </span>
          <b>{{ displayValue(model.datasetSummary?.Interactions, '0') }}</b>
        </button>
      </aside>

      <main v-if="selectedModel" class="asset-console">
        <header class="selected-head">
          <div>
            <span>{{ selectedModel.architecture || selectedModel.framework }}</span>
            <h2>{{ modelTitle(selectedModel) }}</h2>
            <p>{{ selectedModel.dataSource || selectedModel.descriptionZh || selectedModel.statusReason }}</p>
          </div>
          <div class="status-box" :class="statusTone(selectedModel)">
            <strong>{{ localizedStatusText(selectedModel) }}</strong>
            <em>{{ selectedAssetRows.length }} {{ copy.assetGroupsUnit }}</em>
          </div>
        </header>

        <section class="metric-strip">
          <article v-for="metric in selectedMetrics" :key="metric.label">
            <span>{{ metric.label }}</span>
            <strong>{{ metric.value }}</strong>
          </article>
        </section>

        <section class="chart-grid">
          <article class="chart-panel">
            <header>
              <div>
                <span>{{ copy.allModelScale }}</span>
                <strong>{{ copy.fullScaleCompare }}</strong>
              </div>
              <b>{{ copy.fromModelScan }}</b>
            </header>
            <div ref="modelChartRef" class="chart-box"></div>
          </article>
          <article class="chart-panel">
            <header>
              <div>
                <span>{{ copy.currentModelData }}</span>
                <strong>{{ copy.currentModelInteractions }}</strong>
              </div>
              <b>{{ primaryDatasetLabel(selectedModel) }}</b>
            </header>
            <div ref="assetChartRef" class="chart-box"></div>
          </article>
        </section>

        <section class="asset-table-panel">
          <header>
            <div>
              <span>{{ copy.dedicatedDataDisplay }}</span>
              <strong>{{ selectedAssetRows.length }} {{ copy.realAssetGroups }}</strong>
            </div>
          </header>
          <div class="table-shell">
            <table>
              <thead>
                <tr>
                  <th>{{ copy.dataset }}</th>
                  <th>{{ copy.format }}</th>
                  <th>{{ copy.users }}</th>
                  <th>{{ copy.items }}</th>
                  <th>{{ copy.interactions }}</th>
                  <th>{{ copy.fileStatus }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="asset in selectedAssetRows" :key="asset.name + asset.path">
                  <td>
                    <strong>{{ asset.name }}</strong>
                    <em>{{ asset.path }}</em>
                  </td>
                  <td>{{ asset.format }}</td>
                  <td>{{ asset.users }}</td>
                  <td>{{ asset.items }}</td>
                  <td>{{ asset.interactions }}</td>
                  <td>{{ asset.status }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>
      </main>
    </section>

    <section ref="uploadSectionRef" class="upload-workbench">
      <article class="upload-panel">
        <header>
          <div>
            <span>{{ copy.userUploadedDatasets }}</span>
            <strong>{{ uploadLabel }}</strong>
          </div>
          <FileUp :size="22" />
        </header>
        <input ref="fileInputRef" class="hidden-input" type="file" accept=".csv,.zip" @change="handleFileChange" />
        <button type="button" class="file-picker" @click="chooseFile">
          {{ selectedFile ? selectedFile.name : copy.chooseFile }}
        </button>
        <label>
          <span>{{ copy.datasetName }}</span>
          <input v-model="datasetName" type="text" :placeholder="copy.datasetNamePlaceholder" />
        </label>
        <label>
          <span>{{ copy.description }}</span>
          <textarea v-model="datasetDescription" rows="3" :placeholder="copy.descriptionPlaceholder"></textarea>
        </label>
        <button type="button" class="primary-action" :disabled="uploading" @click="uploadAndVisualize">
          {{ uploading ? copy.uploading : copy.uploadAndPreview }}
        </button>
      </article>

      <article class="history-panel">
        <header>
          <div>
            <span>{{ copy.backendUploadRecords }}</span>
            <strong>{{ datasets.length }} {{ copy.datasetsUnit }}</strong>
          </div>
          <div class="history-actions">
            <button type="button" :disabled="historyLoading" @click="loadDatasets">{{ copy.refresh }}</button>
            <button type="button" :disabled="!selectedIds.length || historyLoading" @click="deleteSelected">
              {{ copy.deleteSelected }} {{ selectedIds.length }}
            </button>
          </div>
        </header>

        <div class="dataset-list">
          <article
            v-for="dataset in datasets"
            :key="dataset.id"
            :class="{ active: activeDataset?.id === dataset.id }"
          >
            <label>
              <input
                type="checkbox"
                :checked="selectedIds.includes(dataset.id)"
                @change="toggleSelect(dataset.id)"
              />
            </label>
            <button type="button" @click="viewDataset(dataset.id)">
              <strong>{{ dataset.name }}</strong>
              <em>{{ dataset.date }} / {{ dataset.size }} / {{ dataset.kind.toUpperCase() }}</em>
              <span>{{ copy.samples }} {{ formatInteger(dataset.totalRows) }}，{{ copy.fields }} {{ dataset.featureCount }}</span>
            </button>
            <button type="button" class="danger" :disabled="historyLoading" @click="deleteDataset(dataset.id)">{{ copy.delete }}</button>
          </article>
          <div v-if="!datasets.length" class="empty-state">
            {{ copy.noBackendDatasets }}
          </div>
        </div>
      </article>
    </section>

    <section class="preview-panel">
      <header>
        <div>
          <span>{{ copy.uploadPreview }}</span>
          <strong>{{ activeDataset?.name || copy.noDatasetSelected }}</strong>
        </div>
        <div class="pager">
          <span>{{ copy.pagePrefix }} {{ currentPage }}/{{ totalPages }} {{ copy.pageSuffix }}</span>
          <button type="button" :disabled="currentPage <= 1" @click="goToPage(currentPage - 1)">{{ copy.prevPage }}</button>
          <button type="button" :disabled="currentPage >= totalPages" @click="goToPage(currentPage + 1)">{{ copy.nextPage }}</button>
        </div>
      </header>

      <div class="preview-metrics">
        <article v-for="metric in activeUploadMetrics" :key="metric.label">
          <span>{{ metric.label }}</span>
          <strong>{{ metric.value }}</strong>
        </article>
      </div>

      <div class="table-shell">
        <table>
          <thead>
            <tr>
              <th>#</th>
              <th v-for="column in visibleColumns" :key="column">{{ column }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, rowIndex) in visibleRows" :key="rowIndex">
              <td>{{ pageStart + rowIndex }}</td>
              <td v-for="(cell, cellIndex) in row" :key="cellIndex">{{ cell }}</td>
            </tr>
            <tr v-if="!visibleRows.length">
              <td colspan="8">{{ copy.noPreviewRows }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import * as echarts from 'echarts'
import { FileUp, RefreshCw, Upload } from 'lucide-vue-next'
import { datasetApi, trainingApi } from '@/api'
import type { DatasetPreview } from '@/api/modules/data.api'
import type { Dataset, ModelOption } from '@/types/models'
import { buildChartTheme, chartAxis, chartGrid, chartLegend, chartTooltip } from '@/utils/chartTheme'
import {
  assetValue as catalogAssetValue,
  dataAssets,
  datasetMetricNumber,
  displayValue,
  formatInteger,
  modelKey,
  modelTitle,
  primaryDatasetLabel,
  statusText,
  statusTone,
} from '@/utils/recommenderCatalog'

type DatasetKind = 'csv' | 'zip'

type DatasetRecord = {
  id: string
  sourceId?: number
  name: string
  kind: DatasetKind
  date: string
  size: string
  totalRows: number
  statsRows: number
  featureCount: number
  description: string
  columns: string[]
  rows: string[][]
  seed: number[]
  previewLoaded?: boolean
}

const props = withDefaults(defineProps<{ embedded?: boolean }>(), {
  embedded: false,
})

const { locale } = useI18n()
const isZh = computed(() => !locale.value.startsWith('en'))
const embedded = computed(() => props.embedded)
const modelCatalog = ref<ModelOption[]>([])
const selectedKey = ref('')
const modelLoading = ref(false)
const historyLoading = ref(false)
const uploading = ref(false)
const notice = ref('')
const datasets = ref<DatasetRecord[]>([])
const activeDatasetId = ref('')
const selectedIds = ref<string[]>([])
const selectedFile = ref<File | null>(null)
const datasetName = ref('')
const datasetDescription = ref('')
const fileInputRef = ref<HTMLInputElement | null>(null)
const uploadSectionRef = ref<HTMLElement | null>(null)
const modelChartRef = ref<HTMLElement | null>(null)
const assetChartRef = ref<HTMLElement | null>(null)
const currentPage = ref(1)
const pageSize = ref(12)
const charts = new Map<string, echarts.ECharts>()

const copy = computed(() => isZh.value
  ? {
      title: '推荐数据资产实时可视化',
      heroEyebrow: '推荐数据观测台',
      subtitle: '按 9 个推荐系统模型展示本地数据文件、用户规模、物品规模、交互规模和数据格式；用户上传区处理后端保存的 CSV/ZIP 数据集。',
      refreshing: '刷新中',
      refreshAssets: '刷新数据资产',
      uploadRecoData: '上传推荐数据',
      modelDataDirectory: '模型数据目录',
      modelsUnit: '个模型',
      assetGroupsUnit: '组数据',
      allModelScale: '全模型规模',
      fullScaleCompare: '用户 / 物品 / 交互对比',
      fromModelScan: '来自模型目录扫描',
      currentModelData: '当前模型数据',
      currentModelInteractions: '专属数据资产交互量',
      dedicatedDataDisplay: '专门数据展示',
      realAssetGroups: '组数据资产',
      dataset: '数据集',
      format: '格式',
      users: '用户数',
      items: '物品数',
      interactions: '交互数',
      fileStatus: '文件状态',
      userUploadedDatasets: '用户上传数据集',
      chooseFile: '选择 CSV 或 ZIP 文件',
      datasetName: '数据集名称',
      datasetNamePlaceholder: '例如 job_sequence_eval',
      description: '说明',
      descriptionPlaceholder: '记录数据来源、字段含义、推荐任务用途',
      uploading: '上传保存中',
      uploadAndPreview: '上传并读取预览',
      backendUploadRecords: '后端上传记录',
      datasetsUnit: '个数据集',
      refresh: '刷新',
      deleteSelected: '删除选中',
      samples: '样本',
      fields: '字段',
      delete: '删除',
      noBackendDatasets: '后端暂无用户上传数据集。选择 CSV 或 ZIP 后会保存到后端，并在这里展示预览。',
      uploadPreview: '上传数据预览',
      noDatasetSelected: '未选择数据集',
      pagePrefix: '第',
      pageSuffix: '页',
      prevPage: '上一页',
      nextPage: '下一页',
      noPreviewRows: '暂无可预览行。选择一个后端数据集后会调用预览接口。',
      noFileSelected: '未选择文件',
      modelFamily: '模型家族',
      modelFamilyHint: '推荐系统模型目录',
      dataAssets: '数据资产',
      dataAssetsHint: '模型专属数据文件',
      totalInteractions: '总交互',
      totalInteractionsHint: '按模型目录汇总',
      uploadRecords: '上传记录',
      uploadRecordsHint: '后端保存数据集',
      records: '记录数',
      maxSequence: '最大序列',
      unnamedDataset: '未命名数据集',
      unmarked: '未标注',
      exists: '存在',
      missing: '缺失',
      unregisteredPath: '未登记路径',
      totalRows: '总行数',
      statsRows: '统计行数',
      featureCount: '字段数',
      refreshedPrefix: '已刷新',
      refreshedSuffix: '个推荐模型的数据资产。',
      modelAssetsFailed: '模型数据资产读取失败。',
      datasetRecordsFailed: '上传数据集记录读取失败。',
      chooseFileNotice: '请先选择 CSV 或 ZIP 文件。',
      unsupportedFile: '后端当前只支持 CSV 或 ZIP 数据集上传。',
      uploadSuccess: '已保存到后端，并读取预览。',
      uploadFailed: '数据集上传失败。',
      deletedDatasetPrefix: '已删除数据集：',
      deleteFailed: '数据集删除失败。',
      batchDeletedPrefix: '已删除',
      batchDeletedSuffix: '个后端数据集。',
      batchDeleteFailed: '批量删除失败。',
      previewLoaded: '已从后端读取预览。',
      previewFailed: '数据预览读取失败。',
      backendDatasetRecord: '后端数据集记录',
      taskType: '任务类型',
      status: '状态',
      sampleCount: '样本数',
      filePath: '文件路径',
      chartUsers: '用户数',
      chartItems: '物品数',
      chartInteractions: '交互数',
      noChartInteractions: '当前模型没有可绘制的交互量',
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
      title: 'Realtime Recommendation Data Visualization',
      heroEyebrow: 'Recommender Data Observatory',
      subtitle: 'Show local data files, user scale, item scale, interaction scale, and data format for the 9 recommender-system models. The upload area handles CSV/ZIP datasets saved by the backend.',
      refreshing: 'Refreshing',
      refreshAssets: 'Refresh data assets',
      uploadRecoData: 'Upload recommendation data',
      modelDataDirectory: 'Model data directory',
      modelsUnit: 'models',
      assetGroupsUnit: 'data groups',
      allModelScale: 'All-model scale',
      fullScaleCompare: 'Users / items / interactions',
      fromModelScan: 'From model-directory scan',
      currentModelData: 'Current model data',
      currentModelInteractions: 'Dedicated asset interactions',
      dedicatedDataDisplay: 'Dedicated data display',
      realAssetGroups: 'data asset groups',
      dataset: 'Dataset',
      format: 'Format',
      users: 'Users',
      items: 'Items',
      interactions: 'Interactions',
      fileStatus: 'File status',
      userUploadedDatasets: 'User-uploaded datasets',
      chooseFile: 'Choose a CSV or ZIP file',
      datasetName: 'Dataset name',
      datasetNamePlaceholder: 'e.g. job_sequence_eval',
      description: 'Description',
      descriptionPlaceholder: 'Record data source, field meanings, and recommendation-task usage',
      uploading: 'Saving upload',
      uploadAndPreview: 'Upload and read preview',
      backendUploadRecords: 'Backend upload records',
      datasetsUnit: 'datasets',
      refresh: 'Refresh',
      deleteSelected: 'Delete selected',
      samples: 'Samples',
      fields: 'fields',
      delete: 'Delete',
      noBackendDatasets: 'No user-uploaded datasets in the backend yet. Choose a CSV or ZIP file to save it and show a preview here.',
      uploadPreview: 'Uploaded data preview',
      noDatasetSelected: 'No dataset selected',
      pagePrefix: 'Page',
      pageSuffix: '',
      prevPage: 'Previous',
      nextPage: 'Next',
      noPreviewRows: 'No preview rows. Select a backend dataset to call the preview API.',
      noFileSelected: 'No file selected',
      modelFamily: 'Model families',
      modelFamilyHint: 'Recommender model directory',
      dataAssets: 'Data assets',
      dataAssetsHint: 'Model-specific data files',
      totalInteractions: 'Total interactions',
      totalInteractionsHint: 'Summed from model directories',
      uploadRecords: 'Upload records',
      uploadRecordsHint: 'Datasets saved by the backend',
      records: 'Records',
      maxSequence: 'Max sequence',
      unnamedDataset: 'Unnamed dataset',
      unmarked: 'Unmarked',
      exists: 'Exists',
      missing: 'Missing',
      unregisteredPath: 'No path registered',
      totalRows: 'Total rows',
      statsRows: 'Stats rows',
      featureCount: 'Fields',
      refreshedPrefix: 'Refreshed',
      refreshedSuffix: 'recommender model data assets.',
      modelAssetsFailed: 'Failed to read model data assets.',
      datasetRecordsFailed: 'Failed to read uploaded dataset records.',
      chooseFileNotice: 'Choose a CSV or ZIP file first.',
      unsupportedFile: 'The backend currently supports only CSV or ZIP dataset uploads.',
      uploadSuccess: 'Saved to backend and loaded the preview.',
      uploadFailed: 'Dataset upload failed.',
      deletedDatasetPrefix: 'Deleted dataset: ',
      deleteFailed: 'Dataset deletion failed.',
      batchDeletedPrefix: 'Deleted',
      batchDeletedSuffix: 'backend datasets.',
      batchDeleteFailed: 'Batch deletion failed.',
      previewLoaded: 'Loaded a preview from the backend.',
      previewFailed: 'Failed to read data preview.',
      backendDatasetRecord: 'Backend dataset record',
      taskType: 'Task type',
      status: 'Status',
      sampleCount: 'Samples',
      filePath: 'File path',
      chartUsers: 'Users',
      chartItems: 'Items',
      chartInteractions: 'Interactions',
      noChartInteractions: 'The current model has no drawable interaction volume',
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

const selectedModel = computed(() => modelCatalog.value.find((model) => modelKey(model) === selectedKey.value) || modelCatalog.value[0] || null)
const activeDataset = computed(() => datasets.value.find((dataset) => dataset.id === activeDatasetId.value) || datasets.value[0] || null)
const uploadLabel = computed(() => selectedFile.value?.name || copy.value.noFileSelected)
const modelAssetCount = computed(() => modelCatalog.value.reduce((sum, model) => sum + Math.max(1, dataAssets(model).length || (model.datasetExists ? 1 : 0)), 0))
const totalInteractions = computed(() => modelCatalog.value.reduce((sum, model) => sum + datasetMetricNumber(model, 'Interactions'), 0))

const summaryCards = computed(() => [
  { label: copy.value.modelFamily, value: modelCatalog.value.length, hint: copy.value.modelFamilyHint },
  { label: copy.value.dataAssets, value: modelAssetCount.value, hint: copy.value.dataAssetsHint },
  { label: copy.value.totalInteractions, value: formatInteger(totalInteractions.value), hint: copy.value.totalInteractionsHint },
  { label: copy.value.uploadRecords, value: datasets.value.length, hint: copy.value.uploadRecordsHint },
])

const selectedMetrics = computed(() => {
  const model = selectedModel.value
  if (!model) return []
  return [
    { label: copy.value.dataset, value: primaryDatasetLabel(model) },
    { label: copy.value.users, value: displayValue(model.datasetSummary?.['User scale'], '0') },
    { label: copy.value.items, value: displayValue(model.datasetSummary?.['Item scale'], '0') },
    { label: copy.value.interactions, value: displayValue(model.datasetSummary?.Interactions, '0') },
    { label: isZh.value ? '人均交互' : 'Avg/User', value: displayValue(model.datasetSummary?.['Avg interactions/user'], '0') },
    { label: isZh.value ? '稠密度' : 'Density', value: displayValue(model.datasetSummary?.Density, '0') },
    { label: copy.value.records, value: displayValue(model.datasetSummary?.Records, '0') },
    { label: copy.value.maxSequence, value: displayValue(model.datasetSummary?.['Max sequence length'], '0') },
  ]
})

const selectedAssetRows = computed(() => {
  const model = selectedModel.value
  if (!model) return []
  const rows = dataAssets(model).map((asset) => ({
    name: catalogAssetValue(asset, ['Name', '名称', 'name'], copy.value.unnamedDataset),
    path: catalogAssetValue(asset, ['Path', '路径', 'path'], ''),
    format: catalogAssetValue(asset, ['Format', '格式', 'format'], copy.value.unmarked),
    users: catalogAssetValue(asset, ['User scale', '用户数', 'users'], '0'),
    items: catalogAssetValue(asset, ['Item scale', '物品数', 'items'], '0'),
    interactions: catalogAssetValue(asset, ['Interactions', '交互数', 'interactions'], '0'),
    status: catalogAssetValue(asset, ['Status', '状态', 'status'], model.datasetExists ? copy.value.exists : copy.value.missing),
  }))
  if (rows.length) return rows
  return [{
    name: primaryDatasetLabel(model),
    path: displayValue(model.dataPath, copy.value.unregisteredPath),
    format: displayValue(model.datasetSummary?.['Data format'], copy.value.unmarked),
    users: displayValue(model.datasetSummary?.['User scale'], '0'),
    items: displayValue(model.datasetSummary?.['Item scale'], '0'),
    interactions: displayValue(model.datasetSummary?.Interactions, '0'),
    status: model.datasetExists ? copy.value.exists : copy.value.missing,
  }]
})

const activeUploadMetrics = computed(() => [
  { label: copy.value.totalRows, value: formatInteger(activeDataset.value?.totalRows || 0) },
  { label: copy.value.statsRows, value: formatInteger(activeDataset.value?.statsRows || 0) },
  { label: copy.value.featureCount, value: formatInteger(activeDataset.value?.featureCount || 0) },
  { label: copy.value.format, value: activeDataset.value?.kind.toUpperCase() || '-' },
])

const visibleColumns = computed(() => activeDataset.value?.columns.slice(0, 12) || [])
const visibleRows = computed(() => {
  const rows = activeDataset.value?.rows || []
  const start = (currentPage.value - 1) * pageSize.value
  return rows.slice(start, start + pageSize.value).map((row) => row.slice(0, visibleColumns.value.length))
})
const totalPages = computed(() => Math.max(1, Math.ceil((activeDataset.value?.rows.length || 0) / pageSize.value)))
const pageStart = computed(() => activeDataset.value?.rows.length ? (currentPage.value - 1) * pageSize.value + 1 : 0)

watch([modelCatalog, selectedKey], renderCharts, { deep: true })
watch(datasets, renderCharts, { deep: true })

function focusUpload() {
  uploadSectionRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

async function refreshAll() {
  await Promise.all([loadModels(), loadDatasets()])
}

async function loadModels() {
  modelLoading.value = true
  try {
    const response = await trainingApi.listModels()
    const data = response.data.data
    modelCatalog.value = [...(data.official || []), ...(data.userModels || [])]
    if (!modelCatalog.value.some((model) => modelKey(model) === selectedKey.value)) {
      selectedKey.value = modelCatalog.value[0] ? modelKey(modelCatalog.value[0]) : ''
    }
    notice.value = `${copy.value.refreshedPrefix} ${modelCatalog.value.length} ${copy.value.refreshedSuffix}`
  } catch (error: any) {
    notice.value = error?.response?.data?.message || error?.message || copy.value.modelAssetsFailed
  } finally {
    modelLoading.value = false
    renderCharts()
  }
}

async function loadDatasets() {
  historyLoading.value = true
  try {
    const response = await datasetApi.list()
    datasets.value = (response.data.data || [])
      .map(datasetFromEntity)
      .sort((a, b) => b.id.localeCompare(a.id, undefined, { numeric: true }))
    activeDatasetId.value = datasets.value[0]?.id || ''
    selectedIds.value = []
    currentPage.value = 1
    if (activeDatasetId.value) {
      await loadPreviewForDataset(activeDatasetId.value, { silent: true })
    }
  } catch (error: any) {
    notice.value = error?.response?.data?.message || error?.message || copy.value.datasetRecordsFailed
  } finally {
    historyLoading.value = false
  }
}

function chooseFile() {
  fileInputRef.value?.click()
}

function handleFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  selectedFile.value = input.files?.[0] || null
}

async function uploadAndVisualize() {
  if (!selectedFile.value) {
    notice.value = copy.value.chooseFileNotice
    return
  }
  const extension = selectedFile.value.name.split('.').pop()?.toLowerCase()
  if (extension !== 'csv' && extension !== 'zip') {
    notice.value = copy.value.unsupportedFile
    return
  }

  uploading.value = true
  try {
    const response = await datasetApi.upload(selectedFile.value, {
      name: datasetName.value.trim(),
      description: datasetDescription.value.trim(),
      taskType: 'recommendation',
    })
    const saved = response.data.data
    const record = datasetFromEntity(saved)
    datasets.value = [record, ...datasets.value.filter((item) => item.sourceId !== saved.id)]
    activeDatasetId.value = record.id
    currentPage.value = 1
    await loadPreviewForDataset(record.id, { force: true, silent: true })
    selectedFile.value = null
    datasetName.value = ''
    datasetDescription.value = ''
    if (fileInputRef.value) fileInputRef.value.value = ''
    notice.value = copy.value.uploadSuccess
  } catch (error: any) {
    notice.value = error?.response?.data?.message || error?.message || copy.value.uploadFailed
  } finally {
    uploading.value = false
  }
}

async function viewDataset(id: string) {
  activeDatasetId.value = id
  currentPage.value = 1
  await loadPreviewForDataset(id)
}

function toggleSelect(id: string) {
  selectedIds.value = selectedIds.value.includes(id)
    ? selectedIds.value.filter((item) => item !== id)
    : [...selectedIds.value, id]
}

async function deleteDataset(id: string) {
  const target = datasets.value.find((dataset) => dataset.id === id)
  if (!target?.sourceId) return
  historyLoading.value = true
  try {
    await datasetApi.delete(target.sourceId)
    removeDatasets([id])
    notice.value = `${copy.value.deletedDatasetPrefix}${target.name}`
  } catch (error: any) {
    notice.value = error?.response?.data?.message || error?.message || copy.value.deleteFailed
  } finally {
    historyLoading.value = false
  }
}

async function deleteSelected() {
  const ids = [...selectedIds.value]
  const targets = datasets.value.filter((dataset) => ids.includes(dataset.id) && dataset.sourceId)
  if (!targets.length) return
  historyLoading.value = true
  try {
    await Promise.all(targets.map((dataset) => dataset.sourceId ? datasetApi.delete(dataset.sourceId) : Promise.resolve()))
    removeDatasets(ids)
    notice.value = `${copy.value.batchDeletedPrefix} ${targets.length} ${copy.value.batchDeletedSuffix}`
  } catch (error: any) {
    notice.value = error?.response?.data?.message || error?.message || copy.value.batchDeleteFailed
  } finally {
    historyLoading.value = false
  }
}

async function loadPreviewForDataset(id: string, options: { force?: boolean; silent?: boolean } = {}) {
  const target = datasets.value.find((dataset) => dataset.id === id)
  if (!target?.sourceId || (target.previewLoaded && !options.force)) return
  try {
    const response = await datasetApi.preview(target.sourceId)
    const preview = response.data.data
    datasets.value = datasets.value.map((dataset) => dataset.id === id ? applyPreview(dataset, preview) : dataset)
    if (!options.silent) {
      notice.value = preview.message || copy.value.previewLoaded
    }
  } catch (error: any) {
    if (!options.silent) {
      notice.value = error?.response?.data?.message || error?.message || copy.value.previewFailed
    }
  }
}

function datasetFromEntity(dataset: Dataset): DatasetRecord {
  const totalRows = Number(dataset.sampleCount || 0)
  const featureCount = Number(dataset.classCount || 0)
  return {
    id: String(dataset.id),
    sourceId: dataset.id,
    name: dataset.name,
    kind: kindFromFormat(dataset.format),
    date: formatDate(dataset.createdAt),
    size: formatStoredSize(dataset.fileSizeMb),
    totalRows,
    statsRows: Math.min(totalRows, 5000),
    featureCount,
    description: dataset.description || copy.value.backendDatasetRecord,
    columns: [copy.value.fields, isZh.value ? '值' : 'Value'],
    rows: [
      [copy.value.taskType, dataset.taskType || '-'],
      [copy.value.format, dataset.format || '-'],
      [copy.value.status, dataset.status || '-'],
      [copy.value.sampleCount, String(totalRows)],
      [copy.value.featureCount, String(featureCount)],
      [copy.value.filePath, dataset.filePath || '-'],
    ],
    seed: [totalRows, featureCount].filter((value) => value > 0),
  }
}

function applyPreview(dataset: DatasetRecord, preview: DatasetPreview): DatasetRecord {
  const rows = normalizeRows(preview.rows)
  const columns = Array.isArray(preview.columns) && preview.columns.length
    ? preview.columns.map((column) => String(column))
    : dataset.columns
  const seed = Array.isArray(preview.seed)
    ? preview.seed.map((value) => Number(value)).filter((value) => Number.isFinite(value))
    : dataset.seed
  return {
    ...dataset,
    kind: kindFromFormat(preview.kind),
    totalRows: Number(preview.totalRows ?? dataset.totalRows),
    statsRows: Number(preview.statsRows ?? dataset.statsRows),
    featureCount: Number(preview.featureCount ?? dataset.featureCount),
    columns,
    rows: rows.length ? rows : dataset.rows,
    seed: seed.length ? seed : dataset.seed,
    previewLoaded: true,
  }
}

function normalizeRows(rows: unknown): string[][] {
  if (!Array.isArray(rows)) return []
  return rows
    .filter((row): row is unknown[] => Array.isArray(row))
    .map((row) => row.map((cell) => String(cell)))
}

function removeDatasets(ids: string[]) {
  const deleting = new Set(ids)
  datasets.value = datasets.value.filter((dataset) => !deleting.has(dataset.id))
  selectedIds.value = selectedIds.value.filter((id) => !deleting.has(id))
  if (!datasets.value.some((dataset) => dataset.id === activeDatasetId.value)) {
    activeDatasetId.value = datasets.value[0]?.id || ''
    currentPage.value = 1
  }
}

function goToPage(page: number) {
  currentPage.value = Math.max(1, Math.min(Math.round(page), totalPages.value))
}

function kindFromFormat(format?: string): DatasetKind {
  return format === 'zip' || format === 'image_folder' ? 'zip' : 'csv'
}

function formatStoredSize(size?: number) {
  return typeof size === 'number' && Number.isFinite(size) ? `${size.toFixed(3)} MB` : '-'
}

function formatDate(date?: string) {
  return date ? String(date).slice(0, 10) : '-'
}

function localizedStatusText(model?: ModelOption) {
  const raw = statusText(model)
  return copy.value.statusMap[raw] || raw
}

function parseNumber(value: unknown) {
  const number = Number.parseFloat(String(value ?? '0').replace(/,/g, '').replace('%', ''))
  return Number.isFinite(number) ? number : 0
}

function chartPalette() {
  const theme = buildChartTheme()
  return {
    ...theme,
    secondary: theme.text,
    user: theme.primary,
    item: theme.accent,
    interaction: theme.warning,
  }
}

function getChart(key: string, el: HTMLElement | null) {
  if (!el) return null
  const existing = charts.get(key)
  if (existing && !existing.isDisposed()) return existing
  const chart = echarts.init(el)
  charts.set(key, chart)
  return chart
}

function renderCharts() {
  void nextTick(() => {
    renderModelChart()
    renderAssetChart()
  })
}

function renderModelChart() {
  const chart = getChart('model-scale', modelChartRef.value)
  if (!chart) return
  const colors = chartPalette()
  const axis = chartAxis(colors)
  const rows = [...modelCatalog.value]
    .sort((a, b) => datasetMetricNumber(b, 'Interactions') - datasetMetricNumber(a, 'Interactions'))
  chart.setOption({
    backgroundColor: 'transparent',
    tooltip: {
      ...chartTooltip(colors),
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
    },
    legend: { top: 0, ...chartLegend(colors) },
    color: [colors.user, colors.item, colors.interaction],
    grid: chartGrid({ left: 50, right: 18, top: 46, bottom: 70 }),
    xAxis: {
      type: 'category',
      data: rows.map((model) => modelTitle(model).replace(' 推荐', '')),
      ...axis,
      axisLabel: { ...axis.axisLabel, rotate: rows.length > 6 ? 22 : 0 },
    },
    yAxis: {
      type: 'value',
      ...axis,
    },
    series: [
      { name: copy.value.chartUsers, type: 'bar', barMaxWidth: 18, data: rows.map((model) => datasetMetricNumber(model, 'User scale')), itemStyle: { borderRadius: [7, 7, 0, 0] }, emphasis: { focus: 'series' } },
      { name: copy.value.chartItems, type: 'bar', barMaxWidth: 18, data: rows.map((model) => datasetMetricNumber(model, 'Item scale')), itemStyle: { borderRadius: [7, 7, 0, 0] }, emphasis: { focus: 'series' } },
      { name: copy.value.chartInteractions, type: 'bar', barMaxWidth: 18, data: rows.map((model) => datasetMetricNumber(model, 'Interactions')), itemStyle: { borderRadius: [7, 7, 0, 0] }, emphasis: { focus: 'series' } },
    ],
  }, true)
}

function renderAssetChart() {
  const chart = getChart('asset-scale', assetChartRef.value)
  if (!chart) return
  const colors = chartPalette()
  const axis = chartAxis(colors)
  const rows = selectedAssetRows.value
  const max = Math.max(...rows.map((row) => parseNumber(row.interactions)), 1)
  chart.setOption({
    backgroundColor: 'transparent',
    tooltip: {
      ...chartTooltip(colors),
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
    },
    grid: chartGrid({ left: 50, right: 18, top: 28, bottom: 62 }),
    xAxis: {
      type: 'category',
      data: rows.map((row) => row.name),
      ...axis,
      axisLabel: { ...axis.axisLabel, rotate: rows.length > 4 ? 22 : 0 },
    },
    yAxis: {
      type: 'value',
      ...axis,
    },
    series: [{
      name: copy.value.chartInteractions,
      type: 'bar',
      barMaxWidth: 28,
      itemStyle: { color: colors.primary, borderRadius: [7, 7, 0, 0] },
      emphasis: { focus: 'series' },
      data: rows.map((row) => parseNumber(row.interactions)),
    }],
    graphic: rows.length && max > 0 ? [] : [{
      type: 'text',
      left: 'center',
      top: 'middle',
      style: { text: copy.value.noChartInteractions, fill: colors.secondary },
    }],
  }, true)
}

function resizeCharts() {
  charts.forEach((chart) => chart.resize())
}

onMounted(async () => {
  await refreshAll()
  window.addEventListener('resize', resizeCharts, { passive: true })
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  charts.forEach((chart) => chart.dispose())
  charts.clear()
})
</script>

<style scoped>
.dataset-realtime {
  display: grid;
  gap: 14px;
  color: var(--text-primary);
  --dataset-glass-bg:
    linear-gradient(180deg, rgba(255, 255, 255, 0.08), rgba(255, 255, 255, 0.018)),
    color-mix(in srgb, rgba(var(--glass-bg-rgb), var(--glass-opacity)) 78%, transparent);
  --dataset-card-bg:
    linear-gradient(180deg, rgba(255, 255, 255, 0.055), transparent),
    color-mix(in srgb, var(--panel-bg) 84%, transparent);
}

.dataset-hero,
.summary-grid article,
.model-index,
.asset-console,
.upload-panel,
.history-panel,
.preview-panel,
.chart-panel,
.asset-table-panel {
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--dataset-glass-bg);
  box-shadow: var(--shadow-soft);
  backdrop-filter: blur(var(--glass-blur));
  -webkit-backdrop-filter: blur(var(--glass-blur));
  transition:
    border-color var(--motion-hover) var(--ease-liquid),
    background var(--motion-medium) var(--ease-smooth),
    box-shadow var(--motion-medium) var(--ease-smooth),
    transform var(--motion-hover) var(--ease-liquid);
}

.dataset-hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
  padding: 24px;
}

.dataset-hero span,
.model-index header span,
.selected-head span,
.chart-panel header span,
.asset-table-panel header span,
.upload-panel header span,
.history-panel header span,
.preview-panel header span {
  color: var(--primary-color);
  font-size: 12px;
  font-weight: var(--font-weight-title);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.dataset-hero h1 {
  margin: 8px 0;
  font-size: clamp(28px, 4vw, 42px);
  letter-spacing: 0;
}

.dataset-hero p {
  max-width: 780px;
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.8;
}

.hero-actions,
.history-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

button {
  font: inherit;
}

.hero-actions button,
.history-actions button,
.file-picker,
.primary-action,
.dataset-list button,
.preview-panel .pager button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  max-width: 100%;
  min-height: 40px;
  min-width: 0;
  padding: 0 14px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--surface-color);
  color: var(--text-primary);
  font-weight: var(--font-weight-title);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
  transition:
    border-color var(--motion-hover) var(--ease-liquid),
    background var(--motion-medium) var(--ease-smooth),
    box-shadow var(--motion-medium) var(--ease-smooth),
    transform var(--motion-hover) var(--ease-liquid);
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.primary-action {
  border-color: rgba(var(--primary-rgb), 0.34);
  background: var(--primary-color);
  color: var(--primary-contrast, #fff);
}

.notice {
  margin: 0;
  padding: 10px 12px;
  border: 1px solid rgba(217, 119, 6, 0.24);
  border-radius: 8px;
  background: rgba(217, 119, 6, 0.08);
  line-height: 1.6;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.summary-grid article {
  padding: 16px;
}

.summary-grid span,
.summary-grid em,
.model-index button em,
.model-index button b,
.status-box em,
.chart-panel header b,
.asset-table-panel td em,
.dataset-list em,
.dataset-list span {
  color: var(--text-secondary);
}

.summary-grid strong {
  display: block;
  margin: 8px 0 4px;
  font-size: 26px;
}

.summary-grid em {
  font-style: normal;
  font-size: 13px;
}

.data-layout {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 14px;
}

.model-index {
  align-self: start;
  padding: 14px;
}

.model-index header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.model-index header strong {
  display: block;
  margin-top: 4px;
  font-size: 20px;
}

.model-index header b {
  color: var(--text-secondary);
  font-size: 12px;
}

.model-index button {
  display: grid;
  grid-template-columns: 10px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  width: 100%;
  min-height: 62px;
  margin-top: 8px;
  padding: 10px;
  border: 1px solid transparent;
  border-radius: 8px;
  background: transparent;
  color: var(--text-primary);
  text-align: left;
  cursor: pointer;
}

.model-index button:hover,
.model-index button.active {
  border-color: rgba(var(--primary-rgb), 0.34);
  background: rgba(var(--primary-rgb), 0.08);
}

.model-index i {
  width: 8px;
  height: 34px;
  border-radius: 999px;
  background: #0f8f9a;
}

.model-index .artifact i {
  background: #d97706;
}

.model-index .service i {
  background: #ef4444;
}

.model-index span,
.model-index strong,
.model-index em {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.model-index strong,
.model-index em {
  display: block;
}

.asset-console {
  padding: 18px;
}

.selected-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
}

.selected-head h2 {
  margin: 6px 0;
  overflow-wrap: anywhere;
  font-size: 28px;
}

.selected-head p {
  max-width: 760px;
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.7;
}

.status-box {
  flex: 0 0 180px;
  min-width: 0;
  padding: 14px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
}

.status-box strong,
.status-box em {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-box strong {
  margin-bottom: 4px;
  font-size: 20px;
}

.metric-strip {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 10px;
  margin: 16px 0 12px;
}

.metric-strip article {
  padding: 14px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--dataset-card-bg);
}

.metric-strip span,
.metric-strip strong {
  display: block;
}

.metric-strip span {
  color: var(--text-secondary);
  font-size: 12px;
}

.metric-strip strong {
  margin-top: 8px;
  overflow-wrap: anywhere;
  font-size: 18px;
}

.chart-grid,
.upload-workbench {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.chart-panel,
.asset-table-panel,
.upload-panel,
.history-panel,
.preview-panel {
  padding: 16px;
}

.chart-panel header,
.asset-table-panel header,
.upload-panel header,
.history-panel header,
.preview-panel header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.chart-panel header > div,
.asset-table-panel header > div,
.upload-panel header > div,
.history-panel header > div,
.preview-panel header > div {
  min-width: 0;
}

.chart-panel strong,
.asset-table-panel strong,
.upload-panel strong,
.history-panel strong,
.preview-panel strong {
  display: block;
  overflow-wrap: anywhere;
  margin-top: 5px;
  font-size: 18px;
}

.chart-box {
  width: 100%;
  min-height: 300px;
}

.table-shell {
  overflow: auto;
}

table {
  width: 100%;
  min-width: 760px;
  border-collapse: collapse;
}

th,
td {
  padding: 12px 10px;
  border-bottom: 1px solid var(--border-color);
  text-align: left;
  vertical-align: top;
}

th {
  color: var(--text-secondary);
  font-size: 12px;
}

td strong,
td em {
  display: block;
}

td em {
  max-width: 420px;
  margin-top: 4px;
  overflow-wrap: anywhere;
  font-size: 12px;
  font-style: normal;
}

.upload-workbench {
  scroll-margin-top: 92px;
}

.hidden-input {
  display: none;
}

.upload-panel label {
  display: grid;
  gap: 6px;
  margin-top: 10px;
  color: var(--text-secondary);
  font-size: 13px;
}

.upload-panel input,
.upload-panel textarea {
  width: 100%;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--surface-color);
  color: var(--text-primary);
  font: inherit;
}

.upload-panel input {
  height: 40px;
  padding: 0 10px;
}

.upload-panel textarea {
  padding: 10px;
  resize: vertical;
}

.file-picker,
.primary-action {
  width: 100%;
  margin-top: 10px;
}

.dataset-list {
  display: grid;
  gap: 8px;
}

.dataset-list article {
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
  padding: 8px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--surface-color);
}

.dataset-list article.active {
  border-color: rgba(var(--primary-rgb), 0.34);
  background: rgba(var(--primary-rgb), 0.08);
}

.dataset-list article > button:not(.danger) {
  align-items: flex-start;
  flex-direction: column;
  min-height: 58px;
  border: none;
  background: transparent;
  text-align: left;
}

.dataset-list strong,
.dataset-list em,
.dataset-list span {
  display: block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dataset-list em {
  font-style: normal;
  font-size: 12px;
}

.danger {
  color: #dc2626;
}

.empty-state {
  padding: 18px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  color: var(--text-secondary);
  line-height: 1.7;
}

.preview-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 12px;
}

.preview-metrics article {
  padding: 12px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--surface-color);
}

.preview-metrics span {
  display: block;
  color: var(--text-secondary);
  font-size: 12px;
}

.preview-metrics strong {
  margin-top: 6px;
}

.pager {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.pager span {
  min-width: 0;
  overflow: hidden;
  color: var(--text-secondary);
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (hover: hover) and (pointer: fine) {
  .dataset-hero:hover,
  .summary-grid article:hover,
  .chart-panel:hover,
  .asset-table-panel:hover,
  .upload-panel:hover,
  .history-panel:hover,
  .preview-panel:hover {
    border-color: color-mix(in srgb, var(--primary-color) 34%, var(--border-color));
    box-shadow: var(--shadow-hover);
    transform: translate3d(0, -1px, 0);
  }

  .hero-actions button:hover,
  .history-actions button:hover,
  .file-picker:hover,
  .primary-action:hover,
  .preview-panel .pager button:hover {
    border-color: color-mix(in srgb, var(--primary-color) 42%, var(--border-color));
    background: rgba(var(--primary-rgb), 0.1);
    box-shadow: var(--shadow-ring);
    transform: translate3d(0, -1px, 0);
  }

  .model-index button:hover {
    transform: translate3d(2px, 0, 0);
  }
}

@media (prefers-reduced-motion: reduce) {
  .dataset-hero,
  .summary-grid article,
  .model-index,
  .asset-console,
  .upload-panel,
  .history-panel,
  .preview-panel,
  .chart-panel,
  .asset-table-panel,
  .hero-actions button,
  .history-actions button,
  .file-picker,
  .primary-action,
  .dataset-list button,
  .preview-panel .pager button,
  .model-index button {
    transition: none;
    transform: none !important;
  }
}

@media (max-width: 1180px) {
  .data-layout,
  .chart-grid,
  .upload-workbench {
    grid-template-columns: 1fr;
  }

  .model-index {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 8px;
  }

  .model-index header {
    grid-column: 1 / -1;
  }

  .model-index button {
    margin-top: 0;
  }
}

@media (max-width: 760px) {
  .dataset-hero,
  .selected-head {
    align-items: stretch;
    flex-direction: column;
  }

  .summary-grid,
  .model-index,
  .metric-strip,
  .preview-metrics {
    grid-template-columns: 1fr;
  }

  .dataset-list article {
    grid-template-columns: 28px minmax(0, 1fr);
  }

  .dataset-list .danger {
    grid-column: 2;
    justify-self: start;
  }
}
</style>
