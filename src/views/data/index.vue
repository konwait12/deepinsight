<template>
  <div class="data-center-page">
    <section class="data-center-hero entrance-hero">
      <div class="hero-copy">
        <span>{{ copy.heroEyebrow }}</span>
        <h1>{{ copy.title }}</h1>
        <p>{{ copy.subtitle }}</p>
        <div class="pipeline-strip">
          <article v-for="item in pipelineCards" :key="item.label">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
            <em>{{ item.hint }}</em>
          </article>
        </div>
      </div>
      <div class="hero-actions">
        <button type="button" class="ghost-btn" :disabled="loading" @click="loadAssets">
          {{ loading ? copy.syncing : copy.refreshAssets }}
        </button>
        <button type="button" class="primary-btn" @click="scrollToDataset">{{ copy.datasetViz }}</button>
        <button type="button" class="primary-btn" @click="scrollToCloud">{{ copy.cloudLibrary }}</button>
      </div>
    </section>

    <section class="asset-section center-section">
      <div class="section-head">
        <div>
          <span>{{ copy.assetIndex }}</span>
          <strong>{{ copy.assetSyncTitle }}</strong>
          <p>{{ copy.assetSyncDesc }}</p>
        </div>
        <div class="asset-updated">{{ copy.updated }}{{ overview?.updatedAt || copy.waitingSync }}</div>
      </div>

      <div class="summary-grid">
        <article v-for="card in summaryCards" :key="card.label">
          <span>{{ card.label }}</span>
          <strong>{{ card.value }}</strong>
          <em>{{ card.hint }}</em>
        </article>
      </div>

      <div class="asset-workbench">
        <aside class="asset-sidebar">
          <div class="side-head">
            <span>{{ copy.assetScope }}</span>
            <strong>{{ filteredAssets.length }} {{ copy.itemsUnit }}</strong>
          </div>
          <button
            v-for="tab in tabs"
            :key="tab.key"
            type="button"
            class="asset-tab"
            :class="{ active: activeTab === tab.key }"
            @click="activeTab = tab.key"
          >
            <span>{{ tab.label }}</span>
            <b>{{ countByTab(tab.key) }}</b>
          </button>

          <div class="cloud-brief">
            <span>{{ copy.cloudLinkage }}</span>
            <strong>{{ overview?.summary.cloud || 0 }} {{ copy.cloudStoredUnit }}</strong>
        <p>{{ copy.cloudLinkageDesc }}</p>
          </div>
        </aside>

        <main class="asset-main">
          <div class="asset-toolbar">
            <div class="search-shell">
              <input v-model="search" type="search" :placeholder="copy.searchPlaceholder" />
            </div>
            <select v-model="statusFilter" :aria-label="copy.statusFilter">
              <option value="">{{ copy.allStatus }}</option>
              <option value="ready">{{ copy.readyStatus }}</option>
              <option value="pending">{{ copy.pendingStatus }}</option>
              <option value="error">{{ copy.errorStatus }}</option>
            </select>
          </div>

          <div class="selection-bar">
            <span>{{ copy.selectedPrefix }} {{ selectedKeys.length }} {{ copy.itemsUnit }}</span>
            <button type="button" :disabled="!selectedKeys.length || syncing" @click="syncSelected">
              {{ syncing ? copy.syncing : copy.syncSelected }}
            </button>
            <button type="button" :disabled="!syncableAssets.length || syncing" @click="syncVisible">{{ copy.syncVisible }}</button>
            <button type="button" :disabled="!selectedKeys.length" @click="selectedKeys = []">{{ copy.clearSelection }}</button>
          </div>

          <div class="asset-table" :class="{ loading }">
            <button
              v-for="asset in filteredAssets"
              :key="asset.key"
              type="button"
              class="asset-row"
              :class="{ active: activeAsset?.key === asset.key, selected: selectedKeys.includes(asset.key), readonly: isReadOnly(asset) }"
              @click="activeAsset = asset"
            >
              <span class="row-type">{{ typeCode(asset) }}</span>
              <strong>{{ asset.title }}</strong>
              <em>{{ asset.typeLabel }}</em>
              <i>{{ asset.readiness }}</i>
              <small>{{ assetSyncStateText(asset) }}</small>
              <b :class="{ disabled: !canSyncAsset(asset) }" @click.stop="toggleSelect(asset)">
                {{ selectedKeys.includes(asset.key) ? copy.selected : canSyncAsset(asset) ? copy.select : copy.readOnlyShort }}
              </b>
            </button>
            <div v-if="!filteredAssets.length" class="empty-state">{{ loading ? copy.readingAssets : copy.noMatchedAssets }}</div>
          </div>
        </main>

        <aside class="asset-detail">
          <template v-if="activeAsset">
            <div class="detail-title">
              <span>{{ activeAsset.subtype }}</span>
              <h2>{{ activeAsset.title }}</h2>
              <p>{{ activeAsset.summary }}</p>
            </div>
            <div class="detail-tags">
              <b>{{ statusText(activeAsset) }}</b>
              <b>{{ activeAsset.format }}</b>
              <b v-if="isReadOnly(activeAsset)">{{ copy.officialReadOnly }}</b>
              <b>{{ activeAsset.cloudSaved ? copy.syncedCloud : copy.siteAsset }}</b>
            </div>
            <div class="metric-list">
              <div v-for="(value, key) in activeAsset.metrics || {}" :key="key">
                <span>{{ key }}</span>
                <strong>{{ value }}</strong>
              </div>
            </div>
            <div class="detail-actions">
              <button type="button" class="primary-btn" :disabled="!canSyncAsset(activeAsset) || syncing" @click="syncOne(activeAsset)">
                {{ syncActionText(activeAsset) }}
              </button>
              <button v-if="activeAsset.route" type="button" class="ghost-btn" @click="router.push(activeAsset.route)">
                {{ copy.goRelatedWorkspace }}
              </button>
            </div>
          </template>
          <div v-else class="detail-empty">{{ copy.selectAssetHint }}</div>
        </aside>
      </div>
    </section>

    <section ref="cloudSectionRef" class="cloud-section center-section">
      <CloudWorkspacePortal
        variant="page"
        default-open
        :title="copy.cloudPortalTitle"
        :subtitle="copy.cloudPortalSubtitle"
      />
    </section>

    <section ref="datasetSectionRef" class="center-section dataset-lab-section">
      <DatasetRealtimeVisualization embedded />
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import DatasetRealtimeVisualization from '@/components/data/DatasetRealtimeVisualization.vue'
import CloudWorkspacePortal from '@/components/cloud/CloudWorkspacePortal.vue'
import { datasetApi } from '@/api'
import type { DataAsset, DataAssetOverview } from '@/api/modules/data.api'

const router = useRouter()
const { locale } = useI18n()
const isZh = computed(() => !locale.value.startsWith('en'))

const loading = ref(false)
const syncing = ref(false)
const search = ref('')
const activeTab = ref('all')
const statusFilter = ref('')
const selectedKeys = ref<string[]>([])
const overview = ref<DataAssetOverview | null>(null)
const activeAsset = ref<DataAsset | null>(null)
const datasetSectionRef = ref<HTMLElement | null>(null)
const cloudSectionRef = ref<HTMLElement | null>(null)

const assets = computed(() => overview.value?.assets || [])
const summary = computed(() => overview.value?.summary || {
  total: 0,
  datasets: 0,
  training: 0,
  runs: 0,
  analysis: 0,
  cloud: 0,
  ready: 0,
})

const copy = computed(() => isZh.value
  ? {
      title: '推荐数据中心',
      heroEyebrow: '推荐数据中心',
      subtitle: '集中管理模型目录数据、用户上传数据集、训练/分析记录和可供 AI 使用的工作素材。',
      syncing: '同步中',
      refreshAssets: '刷新资产',
      datasetViz: '推荐数据可视化',
      cloudLibrary: '素材库',
      assetIndex: '数据资产索引',
      assetSyncTitle: '数据资产台账',
      assetSyncDesc: '查看用户数据、训练任务、运行日志、分析结果和工作素材；模型目录数据保持只读，用户资产可加入素材库供 AI 和图表使用。',
      updated: '更新：',
      waitingSync: '待同步',
      assetScope: '资产范围',
      itemsUnit: '项',
      cloudLinkage: '素材联动',
      cloudStoredUnit: '项已沉淀',
      cloudLinkageDesc: '加入素材库后的文件、数据和视图可供 AI 对话和图表工作台读取。',
      searchPlaceholder: '搜索名称、类型、状态、摘要...',
      statusFilter: '状态筛选',
      allStatus: '全部状态',
      readyStatus: '可用 / 已保存',
      pendingStatus: '待处理',
      errorStatus: '异常',
      selectedPrefix: '已选择',
      syncSelected: '加入素材库',
      syncVisible: '加入当前筛选',
      clearSelection: '清空选择',
      officialReadOnly: '内置只读',
      synced: '已同步',
      unsynced: '未同步',
      selected: '已选',
      select: '选择',
      readOnlyShort: '只读',
      readingAssets: '正在读取数据资产...',
      noMatchedAssets: '暂无匹配的数据资产',
      syncedCloud: '已入素材库',
      siteAsset: '站内资产',
      goRelatedWorkspace: '前往相关工作区',
      selectAssetHint: '选择一个资产查看详情和素材库操作',
      cloudPortalTitle: '数据中心素材库',
      cloudPortalSubtitle: '文件夹、上传、下载、重命名、删除、拖拽移动和复制摘要都可直接使用，也能给 AI 与图表工作台复用。',
      pipeModel: '模型目录',
      pipeModelHint: '9 个推荐模型扫描结果',
      pipeUser: '用户资产',
      pipeUserHint: '上传数据与训练记录',
      pipeCloud: '素材复用',
      pipeCloudHint: 'AI 与分析工作台读取',
      pipeReadonly: '内置只读',
      pipeReadonlyHint: '不在普通页面误同步',
      allAssets: '全部资产',
      dataset: '数据集',
      trainingData: '训练数据',
      runLogs: '运行日志',
      analysisResults: '分析结果',
      cloudMaterials: '工作素材',
      totalAssets: '资产总数',
      managedObjects: '已纳入管理的数据对象',
      reusable: '可复用',
      reusableHint: '可训练、可分析或已保存',
      trainingLogs: '训练 / 日志',
      trainingLogsHint: '训练任务与运行记录',
      cloudStored: '素材库',
      cloudStoredHint: '可被 AI 与工作区读取',
      readFailed: '数据资产读取失败，请确认后端与登录状态正常',
      readonlyAdminOnly: '内置资产只能在管理后台维护',
      cannotSync: '当前资产不可同步',
      noSyncableAssets: '没有可同步的资产',
      syncedCountPrefix: '已加入',
      syncedCountSuffix: '项到素材库',
      alreadySynced: '当前资产已在素材库，无需重复加入',
      syncFailed: '加入素材库失败',
      syncActionReadonly: '内置只读',
      syncActionCloud: '已入素材库',
      syncActionDisabled: '不可同步',
      syncAction: '加入素材库',
      statusLabels: {
        ready: '可用',
        saved: '已保存',
        completed: '已完成',
        ingested: '已解析',
        running: '运行中',
        queued: '排队中',
        created: '已创建',
        uploading: '上传中',
        processing: '处理中',
        error: '异常',
        failed: '失败',
        unknown: '未知',
      } as Record<string, string>,
    }
  : {
      title: 'Recommendation Data Center',
      heroEyebrow: 'Recommendation Data Center',
      subtitle: 'Manage model-directory data, user-uploaded datasets, training and analysis records, and AI-readable work materials.',
      syncing: 'Syncing',
      refreshAssets: 'Refresh assets',
      datasetViz: 'Recommendation data visualization',
      cloudLibrary: 'Material library',
      assetIndex: 'Data asset index',
      assetSyncTitle: 'Data asset ledger',
      assetSyncDesc: 'Read backend-registered datasets, training jobs, run logs, analysis records, and work materials. Built-in model-directory assets are read-only; user assets can be added to the material library.',
      updated: 'Updated: ',
      waitingSync: 'waiting',
      assetScope: 'Asset scope',
      itemsUnit: 'items',
      cloudLinkage: 'Material linkage',
      cloudStoredUnit: 'stored items',
      cloudLinkageDesc: 'Materials added here can be read by AI conversations and the chart workspace.',
      searchPlaceholder: 'Search by name, type, status, or summary...',
      statusFilter: 'Status filter',
      allStatus: 'All statuses',
      readyStatus: 'Ready / saved',
      pendingStatus: 'Pending',
      errorStatus: 'Error',
      selectedPrefix: 'Selected',
      syncSelected: 'Add selected',
      syncVisible: 'Add current filter',
      clearSelection: 'Clear selection',
      officialReadOnly: 'Built-in read-only',
      synced: 'Synced',
      unsynced: 'Not synced',
      selected: 'Selected',
      select: 'Select',
      readOnlyShort: 'Read-only',
      readingAssets: 'Reading data assets...',
      noMatchedAssets: 'No matching data assets',
      syncedCloud: 'In material library',
      siteAsset: 'Site asset',
      goRelatedWorkspace: 'Open related workspace',
      selectAssetHint: 'Select an asset to inspect details and material-library actions',
      cloudPortalTitle: 'Data Center Material Library',
      cloudPortalSubtitle: 'Folders, upload, download, rename, delete, drag-to-move, and copy summary are available for reuse in AI or chart analysis.',
      pipeModel: 'Model directory',
      pipeModelHint: '9 recommender scans',
      pipeUser: 'User assets',
      pipeUserHint: 'Uploads and training records',
      pipeCloud: 'Cloud reuse',
      pipeCloudHint: 'Readable by AI and analysis',
      pipeReadonly: 'Built-in read-only',
      pipeReadonlyHint: 'No misleading sync',
      allAssets: 'All assets',
      dataset: 'Datasets',
      trainingData: 'Training data',
      runLogs: 'Run logs',
      analysisResults: 'Analysis results',
      cloudMaterials: 'Work materials',
      totalAssets: 'Total assets',
      managedObjects: 'Managed data objects',
      reusable: 'Reusable',
      reusableHint: 'Ready for training, analysis, or saved use',
      trainingLogs: 'Training / logs',
      trainingLogsHint: 'Training jobs and run records',
      cloudStored: 'Material library',
      cloudStoredHint: 'Readable by AI and workspaces',
      readFailed: 'Failed to read data assets. Check backend and login status.',
      readonlyAdminOnly: 'Built-in assets can only be maintained in the admin console',
      cannotSync: 'This asset cannot be synced',
      noSyncableAssets: 'No syncable assets',
      syncedCountPrefix: 'Added',
      syncedCountSuffix: 'items to the material library',
      alreadySynced: 'These assets are already in the material library',
      syncFailed: 'Failed to add materials',
      syncActionReadonly: 'Built-in read-only',
      syncActionCloud: 'Already added',
      syncActionDisabled: 'Cannot sync',
      syncAction: 'Add to library',
      statusLabels: {
        ready: 'Ready',
        saved: 'Saved',
        completed: 'Completed',
        ingested: 'Ingested',
        running: 'Running',
        queued: 'Queued',
        created: 'Created',
        uploading: 'Uploading',
        processing: 'Processing',
        error: 'Error',
        failed: 'Failed',
        unknown: 'Unknown',
      } as Record<string, string>,
    })

const tabs = computed(() => [
  { key: 'all', label: copy.value.allAssets },
  { key: 'dataset', label: copy.value.dataset },
  { key: 'training', label: copy.value.trainingData },
  { key: 'run', label: copy.value.runLogs },
  { key: 'analysis', label: copy.value.analysisResults },
  { key: 'cloud', label: copy.value.cloudMaterials },
])

const summaryCards = computed(() => [
  { label: copy.value.totalAssets, value: summary.value.total, hint: copy.value.managedObjects },
  { label: copy.value.reusable, value: summary.value.ready, hint: copy.value.reusableHint },
  { label: copy.value.trainingLogs, value: summary.value.training + summary.value.runs, hint: copy.value.trainingLogsHint },
  { label: copy.value.cloudStored, value: summary.value.cloud, hint: copy.value.cloudStoredHint },
])

const pipelineCards = computed(() => [
  { label: copy.value.pipeModel, value: 9, hint: copy.value.pipeModelHint },
  { label: copy.value.pipeUser, value: summary.value.datasets + summary.value.training + summary.value.runs, hint: copy.value.pipeUserHint },
  { label: copy.value.pipeCloud, value: summary.value.cloud, hint: copy.value.pipeCloudHint },
  { label: copy.value.pipeReadonly, value: assets.value.filter(isReadOnly).length, hint: copy.value.pipeReadonlyHint },
])

const filteredAssets = computed(() => {
  const keyword = search.value.trim().toLowerCase()
  return assets.value.filter((asset) => {
    const tabMatched = activeTab.value === 'all'
      || (activeTab.value === 'run' && ['run', 'run_log'].includes(asset.type))
      || (activeTab.value === 'analysis' && ['analysis_result', 'saved_view'].includes(asset.type))
      || (activeTab.value === 'cloud' && asset.type === 'cloud_item')
      || asset.type === activeTab.value
    const statusMatched = !statusFilter.value || statusBucket(asset) === statusFilter.value
    const haystack = [
      asset.title,
      asset.typeLabel,
      asset.subtype,
      asset.status,
      asset.summary,
      asset.readiness,
    ].join(' ').toLowerCase()
    return tabMatched && statusMatched && (!keyword || haystack.includes(keyword))
  })
})

const syncableAssets = computed(() => filteredAssets.value.filter(canSyncAsset))

watch(filteredAssets, (items) => {
  if (!items.length) {
    activeAsset.value = null
    return
  }
  if (!activeAsset.value || !items.some((item) => item.key === activeAsset.value?.key)) {
    activeAsset.value = items[0]
  }
})

async function loadAssets() {
  loading.value = true
  try {
    const response = await datasetApi.assets()
    if (response.data.code === 200) {
      overview.value = response.data.data
      activeAsset.value = filteredAssets.value[0] || assets.value[0] || null
    }
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || copy.value.readFailed)
  } finally {
    loading.value = false
  }
}

function countByTab(key: string) {
  if (key === 'all') return assets.value.length
  if (key === 'run') return assets.value.filter((asset) => ['run', 'run_log'].includes(asset.type)).length
  if (key === 'analysis') return assets.value.filter((asset) => ['analysis_result', 'saved_view'].includes(asset.type)).length
  if (key === 'cloud') return assets.value.filter((asset) => asset.type === 'cloud_item').length
  return assets.value.filter((asset) => asset.type === key).length
}

function statusBucket(asset: DataAsset) {
  if (asset.cloudSaved || ['ready', 'saved', 'completed', 'ingested'].includes(asset.status)) return 'ready'
  if (['error', 'failed'].includes(asset.status)) return 'error'
  return 'pending'
}

function isReadOnly(asset: DataAsset) {
  return Boolean(asset.official || asset.readOnly || asset.canManage === false)
}

function canSyncAsset(asset: DataAsset) {
  return Boolean(asset.canSync !== false && !asset.cloudSaved && asset.type !== 'cloud_item' && !isReadOnly(asset))
}

function syncActionText(asset: DataAsset) {
  if (isReadOnly(asset)) return copy.value.syncActionReadonly
  if (asset.cloudSaved) return copy.value.syncActionCloud
  if (!canSyncAsset(asset)) return copy.value.syncActionDisabled
  return copy.value.syncAction
}

function statusText(asset: DataAsset) {
  return copy.value.statusLabels[asset.status] || asset.status || copy.value.statusLabels.unknown
}

function assetSyncStateText(asset: DataAsset) {
  if (isReadOnly(asset)) return copy.value.officialReadOnly
  return asset.cloudSaved ? copy.value.synced : copy.value.unsynced
}

function typeCode(asset: DataAsset) {
  const zhCodes: Record<string, string> = {
    dataset: '数据',
    training: '训练',
    run: '运行',
    run_log: '日志',
    analysis_result: '分析',
    saved_view: '视图',
    cloud_item: '素材',
  }
  const enCodes: Record<string, string> = {
    dataset: 'DATA',
    training: 'TRAIN',
    run: 'RUN',
    run_log: 'LOG',
    analysis_result: 'AI',
    saved_view: 'VIEW',
    cloud_item: 'CLD',
  }
  return (isZh.value ? zhCodes : enCodes)[asset.type] || (isZh.value ? '资产' : 'ASSET')
}

function toggleSelect(asset: DataAsset) {
  if (!canSyncAsset(asset)) {
    ElMessage.info(isReadOnly(asset) ? copy.value.readonlyAdminOnly : copy.value.cannotSync)
    return
  }
  selectedKeys.value = selectedKeys.value.includes(asset.key)
    ? selectedKeys.value.filter((key) => key !== asset.key)
    : [...selectedKeys.value, asset.key]
}

async function syncOne(asset: DataAsset) {
  if (!canSyncAsset(asset)) {
    ElMessage.info(isReadOnly(asset) ? copy.value.readonlyAdminOnly : copy.value.cannotSync)
    return
  }
  await syncAssets([asset.key])
}

async function syncSelected() {
  const allowed = selectedKeys.value.filter((key) => {
    const asset = assets.value.find((item) => item.key === key)
    return asset && canSyncAsset(asset)
  })
  if (!allowed.length) {
    ElMessage.info(copy.value.noSyncableAssets)
    return
  }
  await syncAssets(allowed)
}

async function syncVisible() {
  await syncAssets(syncableAssets.value.map((asset) => asset.key))
}

async function syncAssets(keys: string[]) {
  if (!keys.length || syncing.value) return
  syncing.value = true
  try {
    const response = await datasetApi.syncAssetsToCloud({ assetKeys: keys })
    const count = response.data.data?.savedCount || 0
    ElMessage.success(count ? `${copy.value.syncedCountPrefix} ${count} ${copy.value.syncedCountSuffix}` : copy.value.alreadySynced)
    selectedKeys.value = selectedKeys.value.filter((key) => !keys.includes(key))
    await loadAssets()
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || copy.value.syncFailed)
  } finally {
    syncing.value = false
  }
}

function scrollToDataset() {
  datasetSectionRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function scrollToCloud() {
  cloudSectionRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

onMounted(loadAssets)
</script>

<style scoped>
.data-center-page {
  width: min(1520px, 100%);
  min-height: calc(100dvh - var(--header-height, 72px));
  margin: 0 auto;
  padding: 22px;
  color: var(--text-primary);
  --data-glass-bg:
    linear-gradient(180deg, rgba(255, 255, 255, 0.08), rgba(255, 255, 255, 0.018)),
    color-mix(in srgb, rgba(var(--glass-bg-rgb), var(--glass-opacity)) 78%, transparent);
  --data-card-bg:
    linear-gradient(180deg, rgba(255, 255, 255, 0.055), transparent),
    color-mix(in srgb, var(--panel-bg) 84%, transparent);
}

.data-center-hero,
.asset-section {
  border: 1px solid color-mix(in srgb, var(--primary-color) 12%, var(--border-color));
  border-radius: 8px;
  background: var(--data-glass-bg);
  box-shadow: var(--shadow-soft);
  backdrop-filter: blur(var(--glass-blur)) saturate(var(--glass-saturate));
  -webkit-backdrop-filter: blur(var(--glass-blur)) saturate(var(--glass-saturate));
  transition:
    border-color var(--motion-hover) var(--ease-liquid),
    background var(--motion-medium) var(--ease-smooth),
    box-shadow var(--motion-medium) var(--ease-smooth);
}

.data-center-hero {
  display: flex;
  align-items: stretch;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 16px;
  padding: 22px;
}

.hero-copy {
  min-width: 0;
}

.hero-copy span,
.section-head span,
.side-head span,
.cloud-brief span,
.detail-title span,
.summary-grid span {
  color: var(--primary-color);
  font-size: 12px;
  font-weight: var(--font-weight-title);
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.hero-copy h1 {
  margin: 8px 0;
  font-size: clamp(32px, 4.4vw, 58px);
  line-height: 1;
  letter-spacing: -0.06em;
}

.hero-copy p,
.section-head p {
  max-width: 900px;
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.75;
}

.pipeline-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-top: 18px;
}

.pipeline-strip article {
  min-width: 0;
  min-height: 82px;
  padding: 12px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: color-mix(in srgb, var(--surface-2) 68%, transparent);
}

.pipeline-strip span,
.pipeline-strip strong,
.pipeline-strip em {
  display: block;
}

.pipeline-strip span {
  color: var(--primary-color);
  font-size: 11px;
  font-weight: var(--font-weight-title);
}

.pipeline-strip strong {
  margin: 7px 0 4px;
  font-size: 24px;
}

.pipeline-strip em {
  overflow: hidden;
  color: var(--text-secondary);
  font-size: 12px;
  font-style: normal;
  line-height: 1.45;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hero-actions,
.detail-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.hero-actions {
  flex: 0 1 460px;
  align-content: flex-end;
  align-items: flex-end;
  justify-content: flex-end;
}

.primary-btn,
.ghost-btn,
.selection-bar button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  max-width: 100%;
  min-height: 40px;
  min-width: 0;
  padding: 0 16px;
  border: 1px solid rgba(var(--primary-rgb), 0.26);
  border-radius: 8px;
  background: var(--workbench-control-bg);
  color: var(--text-primary);
  font-size: 12px;
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

.primary-btn {
  color: var(--primary-color);
  background: rgba(var(--primary-rgb), 0.12);
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.center-section {
  margin-top: 16px;
}

.asset-section {
  padding: 18px;
}

.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 14px;
}

.section-head > div:first-child {
  min-width: 0;
}

.section-head strong {
  display: block;
  margin: 7px 0;
  font-size: 24px;
}

.asset-updated {
  flex: 0 0 auto;
  align-self: start;
  max-width: min(320px, 100%);
  overflow: hidden;
  padding: 8px 12px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  color: var(--text-secondary);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

.summary-grid article {
  min-height: 96px;
  padding: 15px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--data-card-bg);
}

.summary-grid strong {
  display: block;
  margin-top: 7px;
  font-size: 28px;
}

.summary-grid em {
  color: var(--text-muted);
  font-size: 12px;
  font-style: normal;
}

.asset-workbench {
  display: grid;
  grid-template-columns: minmax(220px, 0.72fr) minmax(0, 1fr) minmax(280px, 0.88fr);
  gap: 14px;
}

.asset-sidebar,
.asset-main,
.asset-detail {
  min-width: 0;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--data-card-bg);
}

.asset-sidebar,
.asset-detail {
  padding: 13px;
}

.side-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 10px;
}

.asset-tab {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 7px;
  padding: 10px 11px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--data-card-bg);
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: var(--font-weight-title);
  text-align: left;
  transition:
    border-color var(--motion-hover) var(--ease-liquid),
    background var(--motion-medium) var(--ease-smooth),
    transform var(--motion-hover) var(--ease-liquid);
}

.asset-tab span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.asset-tab b {
  flex: 0 0 auto;
}

.asset-tab.active {
  border-color: rgba(var(--primary-rgb), 0.42);
  background: rgba(var(--primary-rgb), 0.1);
  color: var(--primary-color);
}

.cloud-brief {
  margin-top: 12px;
  padding: 12px;
  border: 1px solid rgba(var(--primary-rgb), 0.18);
  border-radius: 8px;
  background: var(--data-card-bg);
}

.cloud-brief strong {
  display: block;
  margin-top: 6px;
}

.cloud-brief p {
  margin: 7px 0 0;
  color: var(--text-muted);
  font-size: 12px;
  line-height: 1.55;
}

.asset-main {
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr);
  padding: 14px;
}

.asset-toolbar {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) 170px;
  gap: 10px;
  margin-bottom: 12px;
}

.search-shell input,
.asset-toolbar select {
  width: 100%;
  height: 40px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--workbench-control-bg);
  color: var(--text-primary);
  outline: none;
}

.search-shell input {
  padding: 0 14px;
}

.asset-toolbar select {
  padding: 0 12px;
}

.selection-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  padding: 12px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--data-card-bg);
}

.selection-bar span {
  flex: 1 1 160px;
  min-width: 0;
  overflow: hidden;
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: var(--font-weight-title);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.asset-table {
  display: grid;
  gap: 8px;
  max-height: 520px;
  overflow: auto;
  padding-right: 4px;
}

.asset-row {
  width: 100%;
  display: grid;
  grid-template-columns: 58px minmax(0, 1fr) 110px 110px 84px 62px;
  gap: 10px;
  align-items: center;
  padding: 11px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--data-card-bg);
  color: var(--text-secondary);
  text-align: left;
  transition:
    border-color var(--motion-hover) var(--ease-liquid),
    background var(--motion-medium) var(--ease-smooth),
    transform var(--motion-hover) var(--ease-liquid);
}

.asset-row.active,
.asset-row.selected {
  border-color: rgba(var(--primary-rgb), 0.42);
  background: rgba(var(--primary-rgb), 0.1);
}

.row-type {
  color: var(--primary-color);
  font-size: 11px;
  font-weight: 900;
}

.asset-row strong {
  min-width: 0;
  overflow: hidden;
  color: var(--text-primary);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.asset-row em,
.asset-row i,
.asset-row small {
  overflow: hidden;
  color: var(--text-secondary);
  font-size: 12px;
  font-style: normal;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.asset-row b {
  color: var(--primary-color);
  font-size: 12px;
  overflow: hidden;
  text-align: right;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.asset-row b.disabled {
  color: var(--text-muted);
}

.empty-state,
.detail-empty {
  padding: 22px;
  color: var(--text-muted);
  text-align: center;
}

.detail-title h2 {
  margin: 8px 0;
  overflow-wrap: anywhere;
  font-size: 24px;
}

.detail-title p {
  color: var(--text-secondary);
  line-height: 1.65;
}

.detail-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 12px 0;
}

.detail-tags b {
  padding: 6px 9px;
  border: 1px solid rgba(var(--primary-rgb), 0.22);
  border-radius: 8px;
  color: var(--primary-color);
  font-size: 11px;
}

.metric-list {
  display: grid;
  gap: 8px;
  margin-bottom: 14px;
}

.metric-list div {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  min-width: 0;
  padding: 10px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
}

.metric-list span {
  min-width: 0;
  overflow: hidden;
  color: var(--text-secondary);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.metric-list strong {
  min-width: 0;
  overflow: hidden;
  text-align: right;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cloud-section :deep(.cloud-portal) {
  width: 100%;
}

.cloud-section :deep(.cloud-workspace) {
  min-height: 0;
}

.cloud-section {
  scroll-margin-top: calc(var(--header-height, 72px) + 16px);
}

.dataset-lab-section {
  scroll-margin-top: calc(var(--header-height, 72px) + 16px);
}

@media (hover: hover) and (pointer: fine) {
  .data-center-hero:hover,
  .asset-section:hover,
  .summary-grid article:hover,
  .asset-sidebar:hover,
  .asset-main:hover,
  .asset-detail:hover {
    border-color: color-mix(in srgb, var(--primary-color) 34%, var(--border-color));
    box-shadow: var(--shadow-hover);
  }

  .primary-btn:hover,
  .ghost-btn:hover,
  .selection-bar button:hover,
  .asset-tab:hover,
  .asset-row:hover {
    border-color: color-mix(in srgb, var(--primary-color) 42%, var(--border-color));
    background: rgba(var(--primary-rgb), 0.1);
    transform: translate3d(0, -1px, 0);
  }
}

@media (prefers-reduced-motion: reduce) {
  .data-center-hero,
  .asset-section,
  .primary-btn,
  .ghost-btn,
  .selection-bar button,
  .asset-tab,
  .asset-row {
    transition: none;
    transform: none !important;
  }
}

@media (max-width: 1280px) {
  .asset-workbench {
    grid-template-columns: minmax(220px, 0.55fr) minmax(0, 1fr);
  }

  .asset-detail {
    grid-column: 1 / -1;
  }

  .summary-grid,
  .pipeline-strip {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .asset-row {
    grid-template-columns: 58px minmax(0, 1fr) 90px;
  }

  .asset-row i,
  .asset-row small,
  .asset-row b {
    display: none;
  }
}

@media (max-width: 980px) {
  .asset-workbench {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .data-center-page {
    padding: 14px;
  }

  .data-center-hero,
  .section-head {
    align-items: stretch;
    flex-direction: column;
  }

  .hero-actions {
    flex-basis: auto;
    justify-content: stretch;
  }

  .hero-actions button,
  .detail-actions button,
  .selection-bar button {
    flex: 1 1 150px;
  }

  .summary-grid,
  .pipeline-strip,
  .asset-toolbar {
    grid-template-columns: 1fr;
  }
}
</style>
