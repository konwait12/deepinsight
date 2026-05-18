<template>
  <div class="data-page">
    <section class="data-hero entrance-hero">
      <div class="hero-copy">
        <span>Data Asset Center</span>
        <h1>数据资产管理</h1>
        <p>统一管理数据集、训练任务、运行日志、分析结果、保存视图和云端素材。数据进入训练、可视化和 AI 分析前，先在这里确认来源、状态和可复用性。</p>
      </div>
      <div class="hero-actions">
        <button type="button" class="ghost-btn" :disabled="loading" @click="loadAssets">
          <el-icon><Refresh /></el-icon>
          {{ loading ? '同步中' : '刷新资产' }}
        </button>
        <button type="button" class="primary-btn" @click="showUpload = true">
          <el-icon><Upload /></el-icon>
          登记数据集
        </button>
      </div>
    </section>

    <section class="asset-command-grid entrance-up">
      <article v-for="card in summaryCards" :key="card.label" class="summary-card">
        <span>{{ card.label }}</span>
        <strong>{{ card.value }}</strong>
        <em>{{ card.hint }}</em>
      </article>
    </section>

    <section class="asset-workbench">
      <aside class="asset-sidebar entrance-up">
        <div class="side-head">
          <span>资产范围</span>
          <strong>{{ filteredAssets.length }} 项</strong>
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
          <span>云端联动</span>
          <strong>{{ overview?.summary.cloud || 0 }} 项已沉淀</strong>
          <p>同步后可在云端中心、AI 对话和可视化分析中继续读取。</p>
          <button type="button" @click="router.push('/cloud')">进入云端中心</button>
        </div>

        <div class="readonly-note">
          <span>权限边界</span>
          <p>官方资产在普通数据管理界面始终只读，管理员也需要进入管理后台才能维护。</p>
        </div>
      </aside>

      <main class="asset-main entrance-up">
        <div class="asset-toolbar">
          <div class="search-shell">
            <el-icon><Search /></el-icon>
            <input v-model="search" type="search" placeholder="搜索名称、类型、状态、摘要..." />
          </div>
          <select v-model="statusFilter" aria-label="状态筛选">
            <option value="">全部状态</option>
            <option value="ready">可用 / 已保存</option>
            <option value="pending">待处理</option>
            <option value="error">异常</option>
          </select>
          <div class="view-switch" :class="`is-${viewMode}`">
            <i />
            <button type="button" :class="{ active: viewMode === 'cards' }" @click="viewMode = 'cards'">封面</button>
            <button type="button" :class="{ active: viewMode === 'table' }" @click="viewMode = 'table'">清单</button>
          </div>
        </div>

        <div class="selection-bar">
          <span>已选择 {{ selectedKeys.length }} 项</span>
          <button type="button" :disabled="!selectedKeys.length || syncing" @click="syncSelected">
            {{ syncing ? '同步中' : '选择同步云端' }}
          </button>
          <button type="button" :disabled="!syncableAssets.length || syncing" @click="syncVisible">
            同步当前筛选
          </button>
          <button type="button" :disabled="!selectedKeys.length" @click="selectedKeys = []">清空选择</button>
        </div>

        <Transition name="asset-mode" mode="out-in">
          <div v-if="viewMode === 'cards'" key="cards" class="asset-cards" v-loading="loading">
            <article
              v-for="(asset, index) in filteredAssets"
              :key="asset.key"
              class="asset-card"
              :class="{ active: activeAsset?.key === asset.key, selected: selectedKeys.includes(asset.key), saved: asset.cloudSaved, readonly: isReadOnly(asset) }"
              :style="{ '--delay': `${Math.min(index, 10) * 42}ms` }"
              @click="activeAsset = asset"
            >
              <button
                type="button"
                class="select-dot"
                :disabled="!canSyncAsset(asset)"
                :aria-label="`选择 ${asset.title}`"
                @click.stop="toggleSelect(asset)"
              >
                <el-icon v-if="selectedKeys.includes(asset.key)"><Select /></el-icon>
              </button>
              <span v-if="isReadOnly(asset)" class="readonly-badge">官方只读</span>
              <div class="asset-cover">
                <span>{{ asset.typeLabel }}</span>
                <strong>{{ coverText(asset) }}</strong>
                <em>{{ asset.subtype }}</em>
              </div>
              <div class="asset-card-copy">
                <div>
                  <span>{{ asset.format }}</span>
                  <b :class="statusClass(asset)">{{ statusText(asset) }}</b>
                </div>
                <h3>{{ asset.title }}</h3>
                <p>{{ asset.summary }}</p>
              </div>
              <div class="asset-card-foot">
                <small>{{ asset.readiness }}</small>
                <button type="button" :disabled="!canSyncAsset(asset) || syncing" @click.stop="syncOne(asset)">
                  {{ syncActionText(asset) }}
                </button>
              </div>
            </article>
            <div v-if="!filteredAssets.length" class="empty-state">暂无匹配的数据资产</div>
          </div>

          <div v-else key="table" class="asset-table" v-loading="loading">
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
              <small>{{ isReadOnly(asset) ? '官方只读' : asset.cloudSaved ? '已同步' : '未同步' }}</small>
              <b :class="{ disabled: !canSyncAsset(asset) }" @click.stop="toggleSelect(asset)">
                {{ selectedKeys.includes(asset.key) ? '已选' : canSyncAsset(asset) ? '选择' : '只读' }}
              </b>
            </button>
            <div v-if="!filteredAssets.length" class="empty-state">暂无匹配的数据资产</div>
          </div>
        </Transition>
      </main>

      <aside class="asset-detail entrance-up">
        <template v-if="activeAsset">
          <div class="detail-cover">
            <span>{{ activeAsset.typeLabel }}</span>
            <strong>{{ coverText(activeAsset) }}</strong>
          </div>
          <div class="detail-title">
            <span>{{ activeAsset.subtype }}</span>
            <h2>{{ activeAsset.title }}</h2>
            <p>{{ activeAsset.summary }}</p>
          </div>
          <div class="detail-tags">
            <b>{{ statusText(activeAsset) }}</b>
            <b>{{ activeAsset.format }}</b>
            <b v-if="isReadOnly(activeAsset)">官方只读</b>
            <b>{{ activeAsset.cloudSaved ? '已同步云端' : '站内资产' }}</b>
          </div>
          <div class="metric-list">
            <div v-for="(value, key) in activeAsset.metrics" :key="key">
              <span>{{ key }}</span>
              <strong>{{ value }}</strong>
            </div>
          </div>
          <div class="detail-actions">
            <button type="button" class="primary-btn" :disabled="!canSyncAsset(activeAsset) || syncing" @click="syncOne(activeAsset)">
              {{ syncActionText(activeAsset) }}
            </button>
            <button v-if="activeAsset.route" type="button" class="ghost-btn" @click="router.push(activeAsset.route)">
              前往相关工作区
            </button>
          </div>
        </template>
        <div v-else class="detail-empty">选择一个资产查看详情和云端操作</div>
      </aside>
    </section>

    <el-dialog v-model="showUpload" title="登记新数据集" width="540px" class="dataset-dialog">
      <el-form :model="uploadForm" label-position="top">
        <el-form-item label="数据集名称">
          <el-input v-model="uploadForm.name" placeholder="例如 defect-detection-v2" />
        </el-form-item>
        <el-form-item label="任务类型">
          <el-select v-model="uploadForm.taskType" style="width: 100%">
            <el-option label="分类" value="classification" />
            <el-option label="目标检测" value="detection" />
            <el-option label="语义分割" value="segmentation" />
            <el-option label="表格 / 推荐" value="recommendation" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item label="数据格式">
          <el-select v-model="uploadForm.format" style="width: 100%">
            <el-option label="COCO JSON" value="coco" />
            <el-option label="YOLO TXT" value="yolo" />
            <el-option label="Pascal VOC XML" value="voc" />
            <el-option label="CSV" value="csv" />
            <el-option label="Image Folder" value="image_folder" />
          </el-select>
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="uploadForm.description" type="textarea" :rows="3" placeholder="描述来源、版本、标注口径或使用限制" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showUpload = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="createDataset">创建记录</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Refresh, Search, Select, Upload } from '@element-plus/icons-vue'
import { datasetApi } from '@/api'
import type { DataAsset, DataAssetOverview } from '@/api/modules/data.api'

const router = useRouter()

const loading = ref(false)
const syncing = ref(false)
const showUpload = ref(false)
const uploading = ref(false)
const search = ref('')
const activeTab = ref('all')
const statusFilter = ref('')
const viewMode = ref<'cards' | 'table'>('cards')
const selectedKeys = ref<string[]>([])
const overview = ref<DataAssetOverview | null>(null)
const activeAsset = ref<DataAsset | null>(null)
const uploadForm = ref({ name: '', taskType: 'classification', format: 'coco', description: '' })

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

const tabs = [
  { key: 'all', label: '全部资产' },
  { key: 'dataset', label: '数据集' },
  { key: 'training', label: '训练数据' },
  { key: 'run', label: '运行日志' },
  { key: 'analysis', label: '分析结果' },
  { key: 'cloud', label: '云端素材' },
]

const summaryCards = computed(() => [
  { label: '资产总数', value: summary.value.total, hint: '已纳入管理的数据对象' },
  { label: '可复用', value: summary.value.ready, hint: '可训练、可分析或已保存' },
  { label: '训练 / 日志', value: summary.value.training + summary.value.runs, hint: '训练任务与运行记录' },
  { label: '云端沉淀', value: summary.value.cloud, hint: '可被 AI 与工作区读取' },
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
    const haystack = `${asset.title} ${asset.typeLabel} ${asset.subtype} ${asset.status} ${asset.summary} ${asset.readiness}`.toLowerCase()
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
    ElMessage.error(error?.response?.data?.message || '数据资产读取失败，请确认后端与登录状态正常')
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
  if (isReadOnly(asset)) return '官方只读'
  if (asset.cloudSaved) return '已入云端'
  if (!canSyncAsset(asset)) return '不可同步'
  return '同步云端'
}

function statusClass(asset: DataAsset) {
  return `is-${statusBucket(asset)}`
}

function statusText(asset: DataAsset) {
  const labels: Record<string, string> = {
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
  }
  return labels[asset.status] || asset.status || '未知'
}

function coverText(asset: DataAsset) {
  const compact = asset.typeLabel || asset.type || '数据'
  if (compact.length <= 2) return compact
  if (asset.type === 'run_log') return 'LOG'
  if (asset.type === 'analysis_result') return 'AI'
  if (asset.type === 'saved_view') return 'VIEW'
  if (asset.type === 'cloud_item') return '云'
  return compact.slice(0, 2)
}

function typeCode(asset: DataAsset) {
  const codes: Record<string, string> = {
    dataset: 'DATA',
    training: 'TRAIN',
    run: 'RUN',
    run_log: 'LOG',
    analysis_result: 'AI',
    saved_view: 'VIEW',
    cloud_item: 'CLD',
  }
  return codes[asset.type] || 'ASSET'
}

function toggleSelect(asset: DataAsset) {
  if (!canSyncAsset(asset)) {
    ElMessage.info(isReadOnly(asset) ? '官方资产只能在管理后台维护' : '当前资产不可同步')
    return
  }
  if (selectedKeys.value.includes(asset.key)) {
    selectedKeys.value = selectedKeys.value.filter((key) => key !== asset.key)
  } else {
    selectedKeys.value = [...selectedKeys.value, asset.key]
  }
}

async function syncOne(asset: DataAsset) {
  if (!canSyncAsset(asset)) {
    ElMessage.info(isReadOnly(asset) ? '官方资产只能在管理后台维护' : '当前资产不可同步')
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
    ElMessage.info('没有可同步的资产')
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
    ElMessage.success(count ? `已同步 ${count} 项到云端` : '当前资产已在云端，无需重复同步')
    selectedKeys.value = selectedKeys.value.filter((key) => !keys.includes(key))
    await loadAssets()
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || '云端同步失败')
  } finally {
    syncing.value = false
  }
}

async function createDataset() {
  uploading.value = true
  try {
    const response = await datasetApi.create({
      name: uploadForm.value.name || '新数据集',
      taskType: uploadForm.value.taskType,
      format: uploadForm.value.format,
      description: uploadForm.value.description,
      status: 'uploading',
      sampleCount: 0,
      fileSizeMb: 0,
      splitRatio: '7:2:1',
    })
    if (response.data.code === 200) {
      ElMessage.success('已创建数据集记录')
      showUpload.value = false
      uploadForm.value = { name: '', taskType: 'classification', format: 'coco', description: '' }
      await loadAssets()
    }
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || '创建数据集失败')
  } finally {
    uploading.value = false
  }
}

onMounted(loadAssets)
</script>

<style scoped>
.data-page {
  width: min(1520px, 100%);
  min-height: calc(100dvh - var(--header-height, 72px));
  height: calc(100dvh - var(--header-height, 72px));
  margin: 0 auto;
  padding: 18px 24px 24px;
  overflow: hidden;
  overscroll-behavior: contain;
}

.data-hero,
.summary-card,
.asset-sidebar,
.asset-main,
.asset-detail {
  position: relative;
  overflow: hidden;
  border: 1px solid rgba(var(--primary-rgb), 0.18);
  background:
    radial-gradient(circle at 10% 0%, rgba(var(--primary-rgb), 0.18), transparent 34%),
    radial-gradient(circle at 100% 16%, rgba(66, 230, 164, 0.1), transparent 30%),
    rgba(var(--glass-bg-rgb), 0.34);
  box-shadow: 0 24px 70px rgba(0, 0, 0, 0.2);
  backdrop-filter: blur(18px);
}

.data-hero::before,
.summary-card::before,
.asset-card::before,
.asset-sidebar::before,
.asset-main::before,
.asset-detail::before {
  content: "";
  position: absolute;
  inset: 0;
  border-radius: inherit;
  background: radial-gradient(circle at var(--mx, 18%) var(--my, 0%), rgba(var(--primary-rgb), 0.2), transparent 42%);
  opacity: 0.34;
  pointer-events: none;
}

.data-hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
  min-height: 156px;
  margin-bottom: 12px;
  padding: 20px 22px;
  border-radius: 26px;
}

.hero-copy,
.hero-actions,
.summary-card > *,
.asset-sidebar > *,
.asset-main > *,
.asset-detail > * {
  position: relative;
  z-index: 1;
}

.hero-copy span,
.side-head span,
.cloud-brief span,
.summary-card span,
.detail-title span {
  color: var(--primary-color);
  font-size: 11px;
  font-weight: 950;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.hero-copy h1 {
  margin: 6px 0;
  color: var(--text-primary);
  font-size: clamp(30px, 4vw, 54px);
  font-weight: 950;
  letter-spacing: -0.065em;
}

.hero-copy p {
  max-width: 850px;
  margin: 0;
  color: var(--text-secondary);
  font-size: 14px;
  line-height: 1.8;
}

.hero-actions,
.detail-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.primary-btn,
.ghost-btn,
.selection-bar button,
.asset-card-foot button,
.cloud-brief button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  min-height: 38px;
  padding: 0 15px;
  border: 1px solid rgba(var(--primary-rgb), 0.24);
  border-radius: 999px;
  color: var(--text-primary);
  font-size: 12px;
  font-weight: 950;
  transition: transform 180ms ease, border-color 180ms ease, background 180ms ease;
}

.primary-btn {
  background: linear-gradient(135deg, rgba(var(--primary-rgb), 0.32), rgba(66, 230, 164, 0.18));
  color: var(--primary-color);
}

.ghost-btn,
.selection-bar button,
.asset-card-foot button,
.cloud-brief button {
  background: rgba(var(--glass-bg-rgb), 0.26);
  color: var(--text-secondary);
}

.primary-btn:hover,
.ghost-btn:hover,
.selection-bar button:not(:disabled):hover,
.asset-card-foot button:hover,
.cloud-brief button:hover {
  transform: translateY(-1px);
  border-color: rgba(var(--primary-rgb), 0.45);
  background: rgba(var(--primary-rgb), 0.14);
}

.asset-command-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 12px;
}

.summary-card {
  min-height: 94px;
  padding: 14px 16px;
  border-radius: 22px;
}

.summary-card strong {
  display: block;
  margin-top: 7px;
  color: var(--text-primary);
  font-size: 28px;
  font-weight: 950;
}

.summary-card em {
  color: var(--text-muted);
  font-size: 12px;
  font-style: normal;
}

.asset-workbench {
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr) 320px;
  gap: 14px;
  min-height: 0;
  height: calc(100dvh - var(--header-height, 72px) - 18px - 24px - 156px - 12px - 94px - 12px);
  overflow: hidden;
  overscroll-behavior: contain;
}

.asset-sidebar,
.asset-main,
.asset-detail {
  border-radius: 24px;
}

.asset-sidebar,
.asset-detail {
  max-height: 100%;
  padding: 14px;
  align-self: start;
  overflow-y: auto;
  overscroll-behavior: contain;
}

.side-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 12px;
}

.side-head strong {
  color: var(--text-primary);
  font-size: 18px;
  font-weight: 950;
}

.asset-tab {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  padding: 11px 12px;
  border: 1px solid rgba(148, 163, 184, 0.12);
  border-radius: 15px;
  background: rgba(255, 255, 255, 0.035);
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 950;
  text-align: left;
}

.asset-tab.active {
  border-color: rgba(var(--primary-rgb), 0.42);
  background: rgba(var(--primary-rgb), 0.13);
  color: var(--primary-color);
}

.asset-tab b {
  color: inherit;
}

.cloud-brief {
  margin-top: 16px;
  padding: 14px;
  border: 1px solid rgba(var(--primary-rgb), 0.18);
  border-radius: 18px;
  background: rgba(0, 0, 0, 0.15);
}

.cloud-brief strong {
  display: block;
  margin-top: 7px;
  color: var(--text-primary);
  font-size: 16px;
}

.cloud-brief p {
  margin: 8px 0 12px;
  color: var(--text-muted);
  font-size: 12px;
  line-height: 1.6;
}

.readonly-note {
  margin-top: 12px;
  padding: 12px;
  border: 1px solid rgba(251, 191, 36, 0.2);
  border-radius: 16px;
  background: rgba(251, 191, 36, 0.07);
}

.readonly-note span {
  color: #fbbf24;
  font-size: 10px;
  font-weight: 950;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.readonly-note p {
  margin: 6px 0 0;
  color: var(--text-muted);
  font-size: 11px;
  line-height: 1.6;
}

.asset-main {
  min-width: 0;
  min-height: 0;
  max-height: 100%;
  padding: 14px;
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr);
  overflow: hidden;
}

.asset-toolbar {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) 160px auto;
  gap: 10px;
  margin-bottom: 12px;
}

.search-shell {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 40px;
  padding: 0 13px;
  border: 1px solid rgba(var(--primary-rgb), 0.16);
  border-radius: 999px;
  background: rgba(var(--glass-bg-rgb), 0.28);
  color: var(--text-muted);
}

.search-shell input,
.asset-toolbar select {
  width: 100%;
  height: 100%;
  border: 0;
  outline: none;
  background: transparent;
  color: var(--text-primary);
  font-size: 12px;
}

.asset-toolbar select {
  height: 40px;
  padding: 0 12px;
  border: 1px solid rgba(var(--primary-rgb), 0.16);
  border-radius: 999px;
  background: rgba(var(--glass-bg-rgb), 0.28);
}

.view-switch {
  position: relative;
  display: grid;
  grid-template-columns: 1fr 1fr;
  min-width: 126px;
  padding: 4px;
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.18);
}

.view-switch i {
  position: absolute;
  top: 4px;
  bottom: 4px;
  left: 4px;
  width: calc(50% - 4px);
  border-radius: 999px;
  background: rgba(var(--primary-rgb), 0.18);
  transition: transform 220ms ease;
}

.view-switch.is-table i {
  transform: translateX(100%);
}

.view-switch button {
  position: relative;
  z-index: 1;
  color: var(--text-muted);
  font-size: 11px;
  font-weight: 950;
}

.view-switch button.active {
  color: var(--primary-color);
}

.selection-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  padding: 10px;
  border: 1px solid rgba(148, 163, 184, 0.12);
  border-radius: 16px;
  background: rgba(0, 0, 0, 0.12);
}

.selection-bar span {
  margin-right: auto;
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 900;
}

.selection-bar button:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.asset-cards {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding-right: 4px;
}

.asset-card {
  position: relative;
  min-height: 320px;
  overflow: hidden;
  border: 1px solid rgba(148, 163, 184, 0.14);
  border-radius: 22px;
  background:
    radial-gradient(circle at 20% 0%, rgba(var(--primary-rgb), 0.14), transparent 34%),
    rgba(255, 255, 255, 0.035);
  cursor: pointer;
  animation: assetIn 360ms ease both;
  animation-delay: var(--delay);
  transition: transform 200ms ease, border-color 200ms ease, background 200ms ease;
}

.asset-card:hover,
.asset-card.active {
  transform: translateY(-3px);
  border-color: rgba(var(--primary-rgb), 0.4);
}

.asset-card.selected {
  border-color: rgba(66, 230, 164, 0.8);
  box-shadow: inset 0 0 0 1px rgba(66, 230, 164, 0.45), 0 0 34px rgba(66, 230, 164, 0.08);
}

.asset-card.saved::after {
  content: "Cloud";
  position: absolute;
  top: 14px;
  left: 14px;
  z-index: 2;
  padding: 4px 8px;
  border-radius: 999px;
  background: rgba(var(--primary-rgb), 0.16);
  color: var(--primary-color);
  font-size: 10px;
  font-weight: 950;
}

.asset-card.readonly {
  border-color: rgba(251, 191, 36, 0.28);
}

.readonly-badge {
  position: absolute;
  top: 14px;
  left: 14px;
  z-index: 4;
  padding: 4px 8px;
  border: 1px solid rgba(251, 191, 36, 0.22);
  border-radius: 999px;
  background: rgba(251, 191, 36, 0.14);
  color: #fbbf24;
  font-size: 10px;
  font-weight: 950;
}

.asset-card.saved .readonly-badge {
  left: 66px;
}

.select-dot {
  position: absolute;
  top: 12px;
  right: 12px;
  z-index: 3;
  width: 30px;
  height: 30px;
  display: grid;
  place-items: center;
  border: 1px solid rgba(255, 255, 255, 0.22);
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.22);
  color: #42e6a4;
}

.select-dot:disabled,
.asset-card-foot button:disabled,
.detail-actions button:disabled {
  cursor: not-allowed;
  opacity: 0.45;
  transform: none;
}

.asset-cover {
  position: relative;
  height: 132px;
  display: grid;
  place-items: center;
  align-content: center;
  gap: 6px;
  background:
    radial-gradient(circle at 25% 25%, rgba(var(--primary-rgb), 0.36), transparent 30%),
    radial-gradient(circle at 78% 22%, rgba(66, 230, 164, 0.2), transparent 28%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.08), transparent);
}

.asset-cover span,
.asset-cover em {
  color: rgba(255, 255, 255, 0.7);
  font-size: 10px;
  font-style: normal;
  font-weight: 950;
}

.asset-cover strong {
  color: white;
  font-size: 42px;
  font-weight: 950;
  letter-spacing: -0.08em;
}

.asset-card-copy {
  position: relative;
  z-index: 1;
  padding: 14px 14px 0;
}

.asset-card-copy > div,
.asset-card-foot,
.detail-tags {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.asset-card-copy > div {
  justify-content: space-between;
}

.asset-card-copy span,
.asset-card-foot small {
  color: var(--text-muted);
  font-size: 11px;
  font-weight: 900;
}

.asset-card-copy b,
.detail-tags b {
  padding: 4px 8px;
  border-radius: 999px;
  background: rgba(var(--primary-rgb), 0.1);
  color: var(--primary-color);
  font-size: 10px;
}

.asset-card-copy b.is-pending {
  background: rgba(251, 191, 36, 0.12);
  color: #fbbf24;
}

.asset-card-copy b.is-error {
  background: rgba(251, 113, 133, 0.12);
  color: #fb7185;
}

.asset-card-copy h3 {
  margin: 12px 0 8px;
  color: var(--text-primary);
  font-size: 17px;
  font-weight: 950;
}

.asset-card-copy p {
  display: -webkit-box;
  overflow: hidden;
  min-height: 58px;
  margin: 0;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.6;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
}

.asset-card-foot {
  position: relative;
  z-index: 1;
  justify-content: space-between;
  padding: 14px;
}

.asset-table {
  display: grid;
  gap: 8px;
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding-right: 4px;
}

.asset-row {
  display: grid;
  grid-template-columns: 62px minmax(180px, 1fr) 88px minmax(120px, 0.7fr) 78px 56px;
  align-items: center;
  gap: 10px;
  min-height: 62px;
  padding: 10px;
  border: 1px solid rgba(148, 163, 184, 0.12);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.035);
  color: var(--text-secondary);
  text-align: left;
}

.asset-row:hover,
.asset-row.active {
  border-color: rgba(var(--primary-rgb), 0.36);
  background: rgba(var(--primary-rgb), 0.1);
}

.asset-row.selected {
  border-color: rgba(66, 230, 164, 0.72);
}

.asset-row.readonly {
  border-color: rgba(251, 191, 36, 0.24);
}

.row-type {
  width: 52px;
  height: 38px;
  display: grid;
  place-items: center;
  border-radius: 13px;
  background: rgba(var(--primary-rgb), 0.13);
  color: var(--primary-color);
  font-size: 10px;
  font-weight: 950;
}

.asset-row strong,
.asset-row i {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.asset-row strong {
  color: var(--text-primary);
  font-size: 13px;
}

.asset-row em,
.asset-row i,
.asset-row small,
.asset-row b {
  color: var(--text-muted);
  font-size: 11px;
  font-style: normal;
}

.asset-row b {
  color: var(--primary-color);
}

.asset-row b.disabled {
  color: var(--text-muted);
  cursor: not-allowed;
}

.asset-detail {
  display: grid;
  gap: 14px;
}

.detail-cover {
  height: 160px;
  display: grid;
  place-items: center;
  align-content: center;
  gap: 8px;
  border-radius: 20px;
  background:
    radial-gradient(circle at 20% 20%, rgba(var(--primary-rgb), 0.28), transparent 35%),
    radial-gradient(circle at 78% 28%, rgba(66, 230, 164, 0.14), transparent 32%),
    rgba(0, 0, 0, 0.18);
}

.detail-cover span {
  color: var(--primary-color);
  font-size: 11px;
  font-weight: 950;
}

.detail-cover strong {
  color: var(--text-primary);
  font-size: 54px;
  font-weight: 950;
}

.detail-title h2 {
  margin: 8px 0;
  color: var(--text-primary);
  font-size: 22px;
  font-weight: 950;
  line-height: 1.2;
}

.detail-title p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.7;
}

.metric-list {
  display: grid;
  gap: 8px;
}

.metric-list div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid rgba(148, 163, 184, 0.12);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.035);
}

.metric-list span {
  color: var(--text-muted);
  font-size: 11px;
  font-weight: 900;
}

.metric-list strong {
  color: var(--text-primary);
  font-size: 12px;
  font-weight: 950;
}

.detail-actions {
  display: grid;
}

.detail-empty,
.empty-state {
  min-height: 180px;
  display: grid;
  place-items: center;
  color: var(--text-muted);
  font-size: 13px;
}

.asset-mode-enter-active,
.asset-mode-leave-active {
  transition: opacity 180ms ease, transform 180ms ease;
}

.asset-mode-enter-from,
.asset-mode-leave-to {
  opacity: 0;
  transform: translateY(10px);
}

@keyframes assetIn {
  from {
    opacity: 0;
    transform: translateY(24px);
    filter: blur(8px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
    filter: blur(0);
  }
}

@media (max-width: 1280px) {
  .asset-workbench {
    grid-template-columns: 220px minmax(0, 1fr);
  }

  .asset-detail {
    grid-column: 1 / -1;
    max-height: 320px;
  }

  .asset-cards {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .data-hero,
  .asset-toolbar {
    display: grid;
  }

  .asset-command-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .asset-workbench {
    grid-template-columns: 1fr;
    height: auto;
    overflow: visible;
  }

  .asset-sidebar {
    max-height: none;
  }

  .asset-row {
    grid-template-columns: 52px 1fr;
  }

  .asset-row em,
  .asset-row i,
  .asset-row small,
  .asset-row b {
    grid-column: 2;
  }
}

@media (max-width: 620px) {
  .data-page {
    height: auto;
    overflow: visible;
    padding: 16px;
  }

  .asset-command-grid,
  .asset-cards {
    grid-template-columns: 1fr;
  }
}
</style>
