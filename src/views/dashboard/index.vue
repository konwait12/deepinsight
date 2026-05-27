<template>
  <div class="dashboard-container">
    <section class="overview-hero entrance-hero">
      <div class="hero-copy">
        <span>{{ copy.status }}</span>
        <h1>{{ copy.title }}</h1>
        <p>{{ copy.subtitle }}</p>
      </div>
      <div class="hero-actions">
        <button type="button" class="ghost-btn" :disabled="loading" @click="loadDashboard">
          <el-icon><Refresh /></el-icon>
          {{ loading ? copy.refreshing : copy.refresh }}
        </button>
        <button type="button" class="primary-btn" @click="router.push('/training')">
          <el-icon><Plus /></el-icon>
          {{ copy.newTraining }}
        </button>
      </div>
    </section>

    <section class="overview-metrics entrance-up">
      <article v-for="(metric, index) in overviewCards" :key="metric.label" class="metric-card" :style="{ '--delay': `${index * 45}ms` }">
        <span>{{ metric.label }}</span>
        <strong>{{ metric.value }}</strong>
        <em>{{ metric.hint }}</em>
      </article>
    </section>

    <section class="dashboard-grid">
      <main class="primary-column entrance-up">
        <div class="panel-shell command-panel">
          <div class="panel-head">
            <div>
              <span>{{ copy.workspace }}</span>
              <h2>{{ copy.workspaceTitle }}</h2>
            </div>
            <button type="button" @click="router.push('/cloud')">{{ copy.cloudEntry }}</button>
          </div>
          <MagicBento
            class="workspace-bento"
            :items="quickLinks"
            :particle-count="10"
            :spotlight-radius="280"
            :enable-stars="false"
            :enable-tilt="false"
            :enable-magnetism="false"
            :click-effect="false"
            glow-color="66, 230, 164"
            @select="openQuickLink"
          />
        </div>

        <div class="panel-shell training-panel">
          <div class="panel-head">
            <div>
              <span>{{ copy.trainingStatus }}</span>
              <h2>{{ copy.activeTraining }}</h2>
            </div>
            <b>{{ runningJobs.length }}</b>
          </div>

          <div v-if="runningJobs.length" class="running-grid">
            <article v-for="job in runningJobs" :key="job.id" class="running-card">
              <div>
                <span>#{{ job.id }}</span>
                <strong>{{ job.name }}</strong>
                <em>{{ job.modelArchitecture || copy.unknownModel }}</em>
              </div>
              <b>{{ progressOf(job) }}%</b>
              <div class="progress-track">
                <i :style="{ width: `${progressOf(job)}%` }"></i>
              </div>
              <p>{{ copy.loss }} {{ formatNumber(job.currentLoss) }} · {{ copy.accuracy }} {{ formatPercent(job.currentAccuracy) }}</p>
            </article>
          </div>
          <div v-else class="empty-panel">{{ copy.noRunning }}</div>
        </div>

        <div class="panel-shell cloud-preview-panel">
          <CloudWorkspacePortal
            compact
            title="云端素材"
            subtitle="快速查看、上传和归档当前账号的云端资料。完整管理可进入云端中心。"
          />
        </div>
      </main>

      <aside class="side-column entrance-up">
        <div class="panel-shell asset-panel">
          <div class="panel-head">
            <div>
              <span>{{ copy.dataAssets }}</span>
              <h2>{{ copy.assetHealth }}</h2>
            </div>
            <button type="button" @click="router.push('/data')">{{ copy.manage }}</button>
          </div>
          <div class="asset-rings">
            <div v-for="ring in assetRings" :key="ring.label" class="ring-row">
              <span>{{ ring.label }}</span>
              <strong>{{ ring.value }}</strong>
              <div><i :style="{ width: `${ring.percent}%` }"></i></div>
            </div>
          </div>
        </div>

        <div class="panel-shell recent-panel">
          <div class="panel-head">
            <div>
              <span>{{ copy.recent }}</span>
              <h2>{{ copy.recentResults }}</h2>
            </div>
            <el-icon><Clock /></el-icon>
          </div>

          <div class="recent-list">
            <article v-for="job in completedJobs" :key="job.id" @click="router.push('/training')">
              <span>{{ job.updatedAt ? new Date(job.updatedAt).toISOString().slice(0, 10) : '--' }}</span>
              <strong>{{ job.name }}</strong>
              <em>{{ formatPercent(job.currentAccuracy) }} · {{ job.modelArchitecture || copy.unknownModel }}</em>
            </article>
            <div v-if="!completedJobs.length" class="empty-panel small">{{ copy.noResults }}</div>
          </div>
        </div>

        <div class="panel-shell security-panel">
          <span>{{ copy.security }}</span>
          <h2>{{ copy.securityTitle }}</h2>
          <p>{{ copy.securityDesc }}</p>
          <div>
            <b>JWT</b>
            <b>BCrypt</b>
            <b>CORS</b>
            <b>Private Cloud</b>
          </div>
        </div>
      </aside>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Clock, Cpu, DataAnalysis, PieChart, Plus, Refresh, Odometer, Cloudy } from '@element-plus/icons-vue'
import { analysisApi, datasetApi, trainingApi } from '@/api'
import CloudWorkspacePortal from '@/components/cloud/CloudWorkspacePortal.vue'
import MagicBento from '@/components/effects/MagicBento.vue'
import { hasStoredAuthToken } from '@/utils/authState'
import type { DataAssetOverview } from '@/api/modules/data.api'
import type { TrainingJob } from '@/types/models'

const router = useRouter()
const { locale } = useI18n()

const loading = ref(false)
const allJobs = ref<TrainingJob[]>([])
const overview = ref({ totalJobs: 0, runningJobs: 0, completedJobs: 0, failedJobs: 0, avgLoss: 0, avgAccuracy: 0 })
const assetsOverview = ref<DataAssetOverview | null>(null)

const copy = computed(() => {
  if (locale.value.startsWith('en')) {
    return {
      status: 'Platform status',
      title: 'System Overview',
      subtitle: 'A single operating view for training jobs, data assets, visualization outputs, cloud materials, and AI-ready context.',
      refresh: 'Refresh',
      refreshing: 'Refreshing',
      newTraining: 'New training',
      workspace: 'Workspace',
      workspaceTitle: 'Core entries',
      cloudEntry: 'Cloud center',
      trainingStatus: 'Training status',
      activeTraining: 'Active training jobs',
      unknownModel: 'Unknown model',
      loss: 'Loss',
      accuracy: 'Accuracy',
      noRunning: 'No active training jobs right now',
      dataAssets: 'Data assets',
      assetHealth: 'Asset readiness',
      manage: 'Manage',
      recent: 'Recent',
      recentResults: 'Completed training',
      noResults: 'No completed runs yet',
      security: 'Security',
      securityTitle: 'Account-isolated workspace',
      securityDesc: 'Private cloud files are accessed through authenticated APIs, with scoped CORS and password hashing in place.',
      totalAssets: 'Total assets',
      cloudAssets: 'Cloud items',
      readyAssets: 'Reusable',
      activeJobs: 'Active jobs',
    }
  }
  return {
    status: '平台状态',
    title: '系统总览',
    subtitle: '统一查看训练任务、数据资产、可视化结果、云端素材和可导入 AI 的上下文状态。',
    refresh: '刷新',
    refreshing: '刷新中',
    newTraining: '新建训练',
    workspace: '工作区',
    workspaceTitle: '核心入口',
    cloudEntry: '云端中心',
    trainingStatus: '训练状态',
    activeTraining: '运行中的训练任务',
    unknownModel: '未知模型',
    loss: '损失',
    accuracy: '准确率',
    noRunning: '当前没有运行中的训练任务',
    dataAssets: '数据资产',
    assetHealth: '资产可用性',
    manage: '管理',
    recent: '最近',
    recentResults: '完成的训练',
    noResults: '暂无完成记录',
    security: '安全状态',
    securityTitle: '账号隔离工作区',
    securityDesc: '私有云端文件通过认证接口读取，配合受限跨域、密码哈希和用户级数据查询。',
    totalAssets: '资产总数',
    cloudAssets: '云端素材',
    readyAssets: '可复用',
    activeJobs: '运行任务',
  }
})

const runningJobs = computed(() => allJobs.value.filter((job) => job.status === 'running').slice(0, 4))
const completedJobs = computed(() => allJobs.value.filter((job) => job.status === 'completed').slice(0, 5))
const assetSummary = computed(() => assetsOverview.value?.summary || { total: 0, datasets: 0, training: 0, runs: 0, analysis: 0, cloud: 0, ready: 0 })

const overviewCards = computed(() => [
  { label: copy.value.totalAssets, value: assetSummary.value.total, hint: `${assetSummary.value.datasets} datasets / ${assetSummary.value.analysis} analysis` },
  { label: copy.value.cloudAssets, value: assetSummary.value.cloud, hint: locale.value.startsWith('en') ? 'Stored in server cloud' : '已进入服务器云端' },
  { label: copy.value.readyAssets, value: assetSummary.value.ready, hint: locale.value.startsWith('en') ? 'Available for reuse' : '可训练、可分析或可读取' },
  { label: copy.value.activeJobs, value: overview.value.runningJobs || runningJobs.value.length, hint: `${overview.value.completedJobs || completedJobs.value.length} ${copy.value.recentResults}` },
])

const quickLinks = computed(() => [
  {
    title: locale.value.startsWith('en') ? 'Training' : '模型训练',
    description: locale.value.startsWith('en') ? 'Configure and monitor model runs.' : '配置训练任务并追踪实时指标。',
    label: 'TRAIN',
    meta: `${overview.value.runningJobs || runningJobs.value.length} ${copy.value.activeJobs}`,
    path: '/training',
    icon: Cpu,
    accent: 'var(--primary-color)',
    layout: 'feature' as const,
  },
  {
    title: locale.value.startsWith('en') ? 'Data' : '数据管理',
    description: locale.value.startsWith('en') ? 'Manage datasets, logs, results, and cloud assets.' : '管理数据集、日志、结果和云端素材。',
    label: 'DATA',
    meta: `${assetSummary.value.total} ${copy.value.totalAssets}`,
    path: '/data',
    icon: DataAnalysis,
    accent: 'var(--tone-blue)',
    layout: 'wide' as const,
  },
  {
    title: locale.value.startsWith('en') ? 'Visualization' : '可视化分析',
    description: locale.value.startsWith('en') ? 'Run matrix analysis and save reusable views.' : '执行矩阵分析并保存复用视图。',
    label: 'VIZ',
    meta: `${assetSummary.value.analysis} ${locale.value.startsWith('en') ? 'saved' : '已保存'}`,
    path: '/viz',
    icon: PieChart,
    accent: 'var(--tone-violet)',
  },
  {
    title: locale.value.startsWith('en') ? 'Cloud' : '云端中心',
    description: locale.value.startsWith('en') ? 'Upload, organize, and reuse materials.' : '上传、分组和复用研究素材。',
    label: 'CLOUD',
    meta: `${assetSummary.value.cloud} ${copy.value.cloudAssets}`,
    path: '/cloud',
    icon: Cloudy,
    accent: 'var(--tone-green)',
  },
  {
    title: locale.value.startsWith('en') ? 'Prediction' : '预测推理',
    description: locale.value.startsWith('en') ? 'Test models against batch inputs.' : '用批量输入测试模型表现。',
    label: 'PREDICT',
    meta: locale.value.startsWith('en') ? 'batch input' : '批量输入',
    path: '/prediction',
    icon: Odometer,
    accent: 'var(--tone-amber)',
  },
])

const assetRings = computed(() => {
  const total = Math.max(assetSummary.value.total, 1)
  return [
    { label: locale.value.startsWith('en') ? 'Datasets' : '数据集', value: assetSummary.value.datasets, percent: Math.round((assetSummary.value.datasets / total) * 100) },
    { label: locale.value.startsWith('en') ? 'Runs / logs' : '运行 / 日志', value: assetSummary.value.runs, percent: Math.round((assetSummary.value.runs / total) * 100) },
    { label: locale.value.startsWith('en') ? 'Analysis' : '分析结果', value: assetSummary.value.analysis, percent: Math.round((assetSummary.value.analysis / total) * 100) },
    { label: locale.value.startsWith('en') ? 'Cloud' : '云端素材', value: assetSummary.value.cloud, percent: Math.round((assetSummary.value.cloud / total) * 100) },
  ]
})

onMounted(loadDashboard)

async function loadDashboard() {
  if (!hasStoredAuthToken()) return
  loading.value = true
  try {
    const [jobsRes, overviewRes, assetsRes] = await Promise.all([
      trainingApi.list(),
      analysisApi.overview(),
      datasetApi.assets(),
    ])
    if (jobsRes.data.code === 200) allJobs.value = jobsRes.data.data || []
    if (overviewRes.data.code === 200) overview.value = overviewRes.data.data as any
    if (assetsRes.data.code === 200) assetsOverview.value = assetsRes.data.data
  } catch {
    // Keep the dashboard usable even if one service is restarting.
  } finally {
    loading.value = false
  }
}

function progressOf(job: TrainingJob) {
  if (!job.epochs) return 0
  return Math.min(100, Math.round(((job.currentEpoch || 0) / job.epochs) * 100))
}

function formatNumber(value?: number | null) {
  return value == null ? '--' : value.toFixed(4)
}

function formatPercent(value?: number | null) {
  return value == null ? '--' : `${(value * 100).toFixed(1)}%`
}

function openQuickLink(item: { path?: string }) {
  if (item.path) router.push(item.path)
}
</script>

<style scoped>
.dashboard-container {
  max-width: 1500px;
  margin: 0 auto;
  padding: clamp(24px, 2.3vw, 36px);
  --dashboard-section-gap: clamp(22px, 2.1vw, 34px);
  --dashboard-panel-gap: clamp(18px, 1.5vw, 24px);
  --overview-outer-bg:
    linear-gradient(135deg, rgba(7, 14, 20, 0.6), rgba(6, 12, 18, 0.42)),
    linear-gradient(115deg, rgba(var(--primary-rgb), 0.12), transparent 36%),
    radial-gradient(circle at 82% 18%, rgba(77, 201, 240, 0.16), transparent 30%),
    rgba(8, 12, 18, 0.34);
  --overview-inner-bg:
    linear-gradient(145deg, rgba(255, 255, 255, 0.07), rgba(255, 255, 255, 0.018) 46%, rgba(0, 0, 0, 0.04)),
    linear-gradient(120deg, rgba(var(--primary-rgb), 0.08), transparent 44%),
    rgba(255, 255, 255, 0.055);
  --overview-border: color-mix(in srgb, rgba(var(--primary-rgb), 0.22) 55%, rgba(255, 255, 255, 0.1));
  --overview-border-strong: rgba(var(--primary-rgb), 0.4);
  --overview-radius-outer: 16px;
  --overview-radius-inner: 12px;
}

:global(html.light .dashboard-container) {
  --overview-outer-bg:
    linear-gradient(180deg, rgba(255, 255, 255, 0.82), rgba(242, 249, 247, 0.54) 46%, rgba(28, 75, 88, 0.035)),
    linear-gradient(115deg, rgba(var(--primary-rgb), 0.12), transparent 40%),
    radial-gradient(circle at 82% 18%, rgba(77, 201, 240, 0.12), transparent 34%),
    rgba(247, 252, 250, 0.74);
  --overview-inner-bg:
    linear-gradient(180deg, rgba(255, 255, 255, 0.82), rgba(243, 250, 248, 0.52) 46%, rgba(28, 75, 88, 0.02)),
    linear-gradient(120deg, rgba(var(--primary-rgb), 0.075), transparent 46%),
    rgba(250, 253, 252, 0.72);
  --overview-border: rgba(20, 49, 60, 0.13);
  --overview-border-strong: rgba(var(--primary-rgb), 0.36);
}

.overview-hero,
.overview-metrics,
.panel-shell {
  position: relative;
  isolation: isolate;
  overflow: hidden;
  border: 1px solid var(--overview-border);
  border-radius: var(--overview-radius-outer);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.085), rgba(255, 255, 255, 0.016) 46%, rgba(0, 0, 0, 0.06)),
    radial-gradient(circle at 10% 0%, rgba(var(--primary-rgb), 0.12), transparent 28%),
    radial-gradient(circle at 100% 16%, rgba(77, 201, 240, 0.14), transparent 24%),
    var(--overview-outer-bg);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.1),
    inset 0 -1px 0 rgba(0, 0, 0, 0.08),
    0 24px 64px rgba(0, 0, 0, 0.18);
  backdrop-filter: blur(28px) saturate(188%);
  -webkit-backdrop-filter: blur(28px) saturate(188%);
}

.overview-hero::before,
.overview-metrics::before,
.panel-shell::before {
  content: "";
  position: absolute;
  inset: 0;
  z-index: 0;
  border-radius: inherit;
  background:
    linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.18), transparent) top / 100% 1px no-repeat,
    radial-gradient(circle at 8% 0%, rgba(var(--primary-rgb), 0.12), transparent 26%),
    radial-gradient(circle at 100% 0%, rgba(77, 201, 240, 0.12), transparent 24%);
  opacity: 0.82;
  pointer-events: none;
}

:global(html.light .overview-hero),
:global(html.light .overview-metrics),
:global(html.light .panel-shell) {
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.82),
    inset 0 -1px 0 rgba(28, 75, 88, 0.045),
    0 18px 46px rgba(31, 56, 68, 0.1);
  backdrop-filter: blur(20px) saturate(134%);
  -webkit-backdrop-filter: blur(20px) saturate(134%);
}

:global(html.light .overview-hero::before),
:global(html.light .overview-metrics::before),
:global(html.light .panel-shell::before) {
  background:
    linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.84), transparent) top / 100% 1px no-repeat,
    radial-gradient(circle at 8% 0%, rgba(var(--primary-rgb), 0.12), transparent 30%),
    radial-gradient(circle at 100% 0%, rgba(77, 201, 240, 0.11), transparent 28%);
  opacity: 0.94;
}

.overview-hero {
  min-height: clamp(220px, 23vw, 270px);
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: clamp(22px, 3vw, 48px);
  margin-bottom: var(--dashboard-section-gap);
  padding: clamp(28px, 3vw, 42px);
}

.hero-copy,
.hero-actions,
.overview-metrics > *,
.metric-card > *,
.panel-shell > * {
  position: relative;
  z-index: 1;
}

.hero-copy span,
.panel-head span,
.metric-card span,
.quick-card span,
.security-panel > span {
  color: var(--primary-color);
  font-size: 11px;
  font-weight: var(--font-weight-title);
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.hero-copy h1 {
  margin: 8px 0;
  color: var(--text-primary);
  font-size: clamp(34px, 5vw, 58px);
  font-weight: var(--font-weight-title);
  letter-spacing: -0.065em;
}

.hero-copy p {
  max-width: 820px;
  margin: 0;
  color: var(--text-secondary);
  font-size: 15px;
  line-height: 1.8;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  justify-content: flex-end;
  align-items: center;
  min-width: min(340px, 100%);
}

.primary-btn,
.ghost-btn,
.panel-head button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 9px;
  min-height: 42px;
  min-width: 104px;
  padding: 0 18px;
  border: 1px solid var(--overview-border);
  border-radius: 10px;
  background: var(--overview-inner-bg);
  color: var(--text-primary);
  font-size: 12px;
  font-weight: var(--font-weight-title);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.08),
    0 10px 24px rgba(0, 0, 0, 0.08);
  transition: transform 180ms ease, border-color 180ms ease, background 180ms ease, color 180ms ease;
}

.primary-btn {
  border-color: rgba(var(--primary-rgb), 0.34);
  background:
    linear-gradient(135deg, rgba(var(--primary-rgb), 0.22), rgba(77, 201, 240, 0.1)),
    rgba(var(--primary-rgb), 0.12);
  color: var(--primary-color);
}

.primary-btn:hover,
.ghost-btn:hover,
.panel-head button:hover {
  transform: translateY(-1px);
  border-color: rgba(var(--primary-rgb), 0.38);
  background: rgba(var(--primary-rgb), 0.14);
}

.overview-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: var(--dashboard-section-gap);
  padding: 10px;
}

.metric-card {
  position: relative;
  overflow: hidden;
  min-height: 138px;
  padding: 20px;
  border: 1px solid var(--overview-border);
  border-radius: var(--overview-radius-inner);
  background: var(--overview-inner-bg);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.1),
    0 14px 30px rgba(0, 0, 0, 0.09);
  animation: metricIn 360ms ease both;
  animation-delay: var(--delay);
  transition: border-color 180ms ease, background 180ms ease, transform 180ms ease;
}

:global(html.light .metric-card),
:global(html.light .running-card),
:global(html.light .ring-row),
:global(html.light .recent-list article),
:global(html.light .empty-panel) {
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.78),
    inset 0 -1px 0 rgba(28, 75, 88, 0.035),
    0 12px 28px rgba(31, 56, 68, 0.065);
}

.metric-card::before {
  content: "";
  position: absolute;
  inset: 0;
  border-radius: inherit;
  background:
    linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.15), transparent) top / 100% 1px no-repeat,
    radial-gradient(circle at 0% 0%, rgba(var(--primary-rgb), 0.14), transparent 30%);
  opacity: 0.72;
  pointer-events: none;
}

.metric-card:hover {
  transform: translateY(-1px);
  border-color: var(--overview-border-strong);
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.08), rgba(255, 255, 255, 0.016) 48%, rgba(0, 0, 0, 0.03)),
    linear-gradient(120deg, rgba(var(--primary-rgb), 0.11), transparent 42%),
    rgba(255, 255, 255, 0.06);
}

:global(html.light .metric-card:hover),
:global(html.light .recent-list article:hover) {
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.9), rgba(242, 250, 248, 0.52) 48%, rgba(28, 75, 88, 0.03)),
    linear-gradient(120deg, rgba(var(--primary-rgb), 0.12), transparent 44%),
    rgba(250, 253, 252, 0.76);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.84),
    0 16px 34px rgba(31, 56, 68, 0.095);
}

.metric-card strong {
  display: block;
  margin-top: 14px;
  color: var(--text-primary);
  font-size: 34px;
  font-weight: var(--font-weight-title);
}

.metric-card em {
  display: block;
  margin-top: 8px;
  color: var(--text-muted);
  font-size: 12px;
  font-style: normal;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(340px, 380px);
  gap: var(--dashboard-panel-gap);
}

.primary-column,
.side-column {
  display: grid;
  align-content: start;
  gap: var(--dashboard-panel-gap);
}

.panel-shell {
  padding: clamp(18px, 1.6vw, 24px);
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px 22px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--overview-border);
}

.panel-head > div {
  min-width: 0;
}

.panel-head button {
  flex: 0 0 auto;
}

.panel-head h2,
.security-panel h2 {
  margin: 5px 0 0;
  color: var(--text-primary);
  font-size: 20px;
  font-weight: var(--font-weight-title);
}

.panel-head b {
  color: var(--primary-color);
  font-size: 28px;
  font-weight: var(--font-weight-title);
}

.workspace-bento {
  margin-top: 0;
}

.workspace-bento :deep(.magic-bento) {
  grid-auto-rows: minmax(170px, auto);
  gap: 14px;
  overflow: visible;
}

.workspace-bento :deep(.magic-bento-card) {
  min-height: 170px;
  padding: 22px;
  border-color: var(--overview-border) !important;
  border-radius: var(--overview-radius-inner) !important;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.06), rgba(255, 255, 255, 0.016)),
    linear-gradient(130deg, color-mix(in srgb, var(--card-accent) 12%, transparent), transparent 44%),
    var(--overview-inner-bg) !important;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.1),
    0 14px 30px rgba(0, 0, 0, 0.08) !important;
}

.workspace-bento :deep(.magic-bento-card--wide) {
  grid-column: span 4;
}

.workspace-bento :deep(.magic-bento-card::before) {
  background:
    linear-gradient(90deg, transparent, color-mix(in srgb, var(--card-accent) 18%, transparent), transparent) top / 100% 1px no-repeat,
    linear-gradient(120deg, rgba(255, 255, 255, 0.05), transparent 34%);
  opacity: 0.58;
}

.workspace-bento :deep(.magic-bento-card:hover) {
  border-color: color-mix(in srgb, var(--card-accent) 34%, var(--overview-border)) !important;
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--card-accent) 12%, transparent), rgba(255, 255, 255, 0.014)),
    linear-gradient(130deg, color-mix(in srgb, var(--card-accent) 14%, transparent), transparent 46%),
    var(--overview-inner-bg) !important;
}

.workspace-bento :deep(.magic-bento-card__icon) {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  border-color: color-mix(in srgb, var(--card-accent) 24%, var(--overview-border));
  background: color-mix(in srgb, var(--card-accent) 10%, transparent);
}

.workspace-bento :deep(.magic-bento-card__title) {
  font-size: clamp(16px, 1.25vw, 20px);
}

.workspace-bento :deep(.magic-bento-card__description) {
  max-width: 38ch;
  line-height: 1.65;
}

.running-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.running-card {
  display: grid;
  gap: 12px;
  padding: 16px;
  border: 1px solid var(--overview-border);
  border-radius: var(--overview-radius-inner);
  background: var(--overview-inner-bg);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.08),
    0 10px 24px rgba(0, 0, 0, 0.06);
}

.running-card > div:first-child {
  display: grid;
  gap: 4px;
}

.running-card span,
.running-card em,
.running-card p {
  color: var(--text-muted);
  font-size: 11px;
  font-style: normal;
}

.running-card strong {
  color: var(--text-primary);
  font-size: 15px;
  font-weight: var(--font-weight-title);
}

.running-card > b {
  color: var(--primary-color);
  font-size: 24px;
  font-weight: var(--font-weight-title);
}

.progress-track,
.ring-row div {
  height: 7px;
  overflow: hidden;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.14);
}

.progress-track i,
.ring-row i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, var(--primary-color), #42e6a4);
  box-shadow: 0 0 18px rgba(var(--primary-rgb), 0.35);
}

.cloud-preview-panel {
  padding: 0;
  background: transparent;
  box-shadow: none;
  border: 0;
  backdrop-filter: none;
  -webkit-backdrop-filter: none;
}

:global(html.light .cloud-preview-panel) {
  box-shadow: none;
}

.cloud-preview-panel :deep(.cloud-portal) {
  margin: 0;
  border-color: var(--overview-border) !important;
  border-radius: var(--overview-radius-outer) !important;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.08), rgba(255, 255, 255, 0.016) 46%, rgba(0, 0, 0, 0.05)),
    radial-gradient(circle at 12% 0%, rgba(var(--primary-rgb), 0.12), transparent 30%),
    radial-gradient(circle at 100% 18%, rgba(77, 201, 240, 0.12), transparent 26%),
    var(--overview-outer-bg) !important;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.1),
    0 24px 64px rgba(0, 0, 0, 0.18) !important;
  backdrop-filter: blur(28px) saturate(188%);
  -webkit-backdrop-filter: blur(28px) saturate(188%);
}

:global(html.light .cloud-preview-panel) :deep(.cloud-portal) {
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.72),
    0 16px 44px rgba(15, 23, 42, 0.08) !important;
  backdrop-filter: blur(16px) saturate(120%);
  -webkit-backdrop-filter: blur(16px) saturate(120%);
}

.cloud-preview-panel :deep(.cloud-portal-top) {
  padding: 18px;
}

.cloud-preview-panel :deep(.cloud-workspace) {
  padding: 0 18px 18px;
}

.cloud-preview-panel :deep(.cloud-nav),
.cloud-preview-panel :deep(.cloud-toolbar),
.cloud-preview-panel :deep(.cloud-body),
.cloud-preview-panel :deep(.cloud-folder-pane),
.cloud-preview-panel :deep(.cloud-showcase),
.cloud-preview-panel :deep(.cloud-detail),
.cloud-preview-panel :deep(.folder-row),
.cloud-preview-panel :deep(.table-row),
.cloud-preview-panel :deep(.detail-card),
.cloud-preview-panel :deep(.upload-zone) {
  border-color: var(--overview-border) !important;
  background: var(--overview-inner-bg) !important;
}

.cloud-preview-panel :deep(.cloud-nav),
.cloud-preview-panel :deep(.cloud-toolbar),
.cloud-preview-panel :deep(.cloud-folder-pane),
.cloud-preview-panel :deep(.cloud-showcase),
.cloud-preview-panel :deep(.cloud-detail),
.cloud-preview-panel :deep(.folder-row),
.cloud-preview-panel :deep(.table-row),
.cloud-preview-panel :deep(.detail-card),
.cloud-preview-panel :deep(.upload-zone) {
  border-radius: var(--overview-radius-inner) !important;
}

.cloud-preview-panel :deep(.cloud-body) {
  border-radius: var(--overview-radius-outer) !important;
}

.cloud-preview-panel :deep(.cloud-actions button),
.cloud-preview-panel :deep(.display-switch button),
.cloud-preview-panel :deep(.folder-tools button),
.cloud-preview-panel :deep(.detail-actions button),
.cloud-preview-panel :deep(.upload-zone button) {
  border-radius: 10px !important;
  border-color: var(--overview-border) !important;
  background: var(--overview-inner-bg) !important;
}

.asset-rings {
  display: grid;
  gap: 14px;
}

.ring-row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 8px 10px;
  align-items: center;
  padding: 14px;
  border: 1px solid var(--overview-border);
  border-radius: var(--overview-radius-inner);
  background: var(--overview-inner-bg);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.06);
}

.ring-row span {
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: var(--font-weight-title);
}

.ring-row strong {
  color: var(--text-primary);
  font-size: 13px;
  font-weight: var(--font-weight-title);
}

.ring-row div {
  grid-column: 1 / -1;
}

.recent-list {
  display: grid;
  gap: 12px;
}

.recent-list article {
  display: grid;
  gap: 6px;
  padding: 14px;
  border: 1px solid var(--overview-border);
  border-radius: var(--overview-radius-inner);
  background: var(--overview-inner-bg);
  cursor: pointer;
  transition: border-color 180ms ease, background 180ms ease, transform 180ms ease;
}

.recent-list article:hover {
  transform: translateY(-1px);
  border-color: var(--overview-border-strong);
  background:
    linear-gradient(140deg, rgba(var(--primary-rgb), 0.1), transparent 48%),
    rgba(255, 255, 255, 0.06);
}

.recent-list span,
.recent-list em {
  color: var(--text-muted);
  font-size: 11px;
  font-style: normal;
}

.recent-list strong {
  color: var(--text-primary);
  font-size: 13px;
  font-weight: var(--font-weight-title);
}

.security-panel p {
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.75;
}

.security-panel div {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.security-panel b {
  padding: 6px 10px;
  border: 1px solid var(--overview-border);
  border-radius: 10px;
  background: var(--overview-inner-bg);
  color: var(--primary-color);
  font-size: 10px;
}

.empty-panel {
  min-height: 136px;
  display: grid;
  place-items: center;
  border: 1px dashed var(--overview-border);
  border-radius: var(--overview-radius-inner);
  background: var(--overview-inner-bg);
  color: var(--text-muted);
  font-size: 13px;
}

.empty-panel.small {
  min-height: 80px;
}

@keyframes metricIn {
  from {
    opacity: 0;
    transform: translateY(18px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 1280px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }

  .side-column {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    align-items: start;
  }

  .security-panel {
    grid-column: 1 / -1;
  }
}

@media (max-width: 920px) {
  .dashboard-container {
    padding: 16px;
  }

  .overview-hero {
    display: grid;
    min-height: 0;
  }

  .hero-actions {
    justify-content: flex-start;
  }

  .overview-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .side-column {
    grid-template-columns: 1fr;
  }

  .running-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 560px) {
  .overview-metrics {
    grid-template-columns: 1fr;
  }
}
</style>
