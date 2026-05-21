<template>
  <div class="admin-overview">
    <section class="hero-panel">
      <div>
        <span class="eyebrow">ADMIN COMMAND</span>
        <h1>平台控制总览</h1>
        <p>集中查看用户、模型、AI 配置、数据与训练状态。这里优先暴露需要管理员处理的风险，而不是只堆表格。</p>
      </div>
      <div class="hero-actions">
        <el-button type="primary" @click="refreshAll">刷新状态</el-button>
        <el-button @click="router.push('/admin/ai')">配置 AI</el-button>
      </div>
    </section>

    <section class="metric-grid">
      <article v-for="card in metricCards" :key="card.key" class="metric-card" @click="router.push(card.path)">
        <span>{{ card.label }}</span>
        <strong>{{ card.value }}</strong>
        <em>{{ card.hint }}</em>
      </article>
    </section>

    <section class="ops-grid">
      <article class="ops-panel">
        <div class="panel-head">
          <span>风险队列</span>
          <strong>{{ risks.length || '无阻断项' }}</strong>
        </div>
        <div class="risk-list">
          <div v-for="risk in risks" :key="risk.title" class="risk-item" :class="risk.level">
            <b>{{ risk.title }}</b>
            <span>{{ risk.desc }}</span>
            <el-button size="small" text @click="router.push(risk.path)">处理</el-button>
          </div>
          <div v-if="!risks.length" class="empty-state">系统核心配置齐全，暂无需要立即处理的管理风险。</div>
        </div>
      </article>

      <article class="ops-panel">
        <div class="panel-head">
          <span>AI 服务</span>
          <strong>{{ activeConfig?.name || '未启用' }}</strong>
        </div>
        <div class="ai-status">
          <div>
            <span>模型</span>
            <b>{{ activeConfig?.modelName || '-' }}</b>
          </div>
          <div>
            <span>类型</span>
            <b>{{ activeConfig?.modelType || '-' }}</b>
          </div>
          <div>
            <span>密钥状态</span>
            <b>{{ activeConfig?.apiKey ? '已安全保存' : '未配置' }}</b>
          </div>
        </div>
        <p class="panel-note">API Key 只在后端保存与调用，管理员列表中只展示脱敏值；普通前端页面不会接触密钥。</p>
      </article>

      <article class="ops-panel wide">
        <div class="panel-head">
          <span>最近训练任务</span>
          <strong>{{ jobs.length }}</strong>
        </div>
        <el-table :data="recentJobs" height="260">
          <el-table-column prop="name" label="任务" min-width="180" />
          <el-table-column prop="modelArchitecture" label="模型" width="150" />
          <el-table-column prop="status" label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="进度" width="160">
            <template #default="{ row }">
              <el-progress :percentage="jobProgress(row)" :stroke-width="8" />
            </template>
          </el-table-column>
        </el-table>
      </article>

      <article class="ops-panel">
        <div class="panel-head">
          <span>快捷管理</span>
          <strong>Actions</strong>
        </div>
        <div class="quick-actions">
          <button v-for="action in quickActions" :key="action.path" type="button" @click="router.push(action.path)">
            <b>{{ action.title }}</b>
            <span>{{ action.desc }}</span>
          </button>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { adminApi } from '@/api'

const router = useRouter()
const stats = ref<Record<string, number>>({})
const users = ref<any[]>([])
const models = ref<any[]>([])
const configs = ref<any[]>([])
const datasets = ref<any[]>([])
const jobs = ref<any[]>([])
const posts = ref<any[]>([])
const articles = ref<any[]>([])
const loading = ref(false)

const safeList = async (fn: () => Promise<any>) => {
  try {
    const response = await fn()
    return response.data.code === 200 ? response.data.data || [] : []
  } catch {
    return []
  }
}

const refreshAll = async () => {
  loading.value = true
  try {
    const [statusRes, userList, modelList, configList, datasetList, jobList, postList, articleList] = await Promise.all([
      adminApi.status().then((r) => r.data.data || {}).catch(() => ({})),
      safeList(() => adminApi.users.list()),
      safeList(() => adminApi.models.list()),
      safeList(() => adminApi.aiConfigs.list()),
      safeList(() => adminApi.datasets.list()),
      safeList(() => adminApi.trainingJobs.list()),
      safeList(() => adminApi.forumPosts.list()),
      safeList(() => adminApi.knowledgeArticles.list()),
    ])
    stats.value = statusRes as Record<string, number>
    users.value = userList
    models.value = modelList
    configs.value = configList
    datasets.value = datasetList
    jobs.value = jobList
    posts.value = postList
    articles.value = articleList
  } finally {
    loading.value = false
  }
}

const activeConfig = computed(() => configs.value.find((item) => item.isActive))
const officialModels = computed(() => models.value.filter((item) => item.isOfficial))
const customModels = computed(() => models.value.filter((item) => !item.isOfficial))
const runningJobs = computed(() => jobs.value.filter((item) => item.status === 'running'))
const failedJobs = computed(() => jobs.value.filter((item) => item.status === 'failed'))
const readyDatasets = computed(() => datasets.value.filter((item) => item.status === 'ready'))
const recentJobs = computed(() => [...jobs.value].sort((a, b) => Number(b.id || 0) - Number(a.id || 0)).slice(0, 8))

const metricCards = computed(() => [
  { key: 'users', label: '用户', value: stats.value.users ?? users.value.length, hint: `${users.value.filter((u) => u.role === 'ADMIN').length} 名管理员`, path: '/admin/users' },
  { key: 'models', label: '模型资产', value: stats.value.models ?? models.value.length, hint: `${officialModels.value.length} 官方 / ${customModels.value.length} 自定义`, path: '/admin/models' },
  { key: 'ai', label: 'AI 配置', value: configs.value.length, hint: activeConfig.value ? `启用 ${activeConfig.value.modelName}` : '未启用模型', path: '/admin/ai' },
  { key: 'data', label: '数据集', value: stats.value.datasets ?? datasets.value.length, hint: `${readyDatasets.value.length} 个可训练`, path: '/admin/data' },
  { key: 'jobs', label: '训练任务', value: stats.value.trainingJobs ?? jobs.value.length, hint: `${runningJobs.value.length} 运行中 / ${failedJobs.value.length} 失败`, path: '/admin/data' },
  { key: 'content', label: '内容', value: (stats.value.forumPosts ?? posts.value.length) + (stats.value.knowledgeArticles ?? articles.value.length), hint: '论坛 + 知识库', path: '/admin/forum' },
])

const risks = computed(() => {
  const list: Array<{ title: string; desc: string; level: string; path: string }> = []
  if (!activeConfig.value) list.push({ title: 'AI 模型未启用', desc: '导入分析结果、AI 面板刷新和对话能力会降级。', level: 'danger', path: '/admin/ai' })
  if (!officialModels.value.length) list.push({ title: '缺少官方模型资产', desc: '训练入口和官方资产展示会变弱，需要检查模型注册表。', level: 'warning', path: '/admin/models' })
  if (failedJobs.value.length) list.push({ title: '存在失败训练任务', desc: `${failedJobs.value.length} 个训练任务失败，建议检查数据集、参数或运行日志。`, level: 'warning', path: '/admin/data' })
  if (!readyDatasets.value.length) list.push({ title: '没有可训练数据集', desc: '数据管理页缺少 ready 状态数据集，训练流程会受阻。', level: 'warning', path: '/admin/data' })
  if (!users.value.some((item) => item.role === 'ADMIN')) list.push({ title: '管理员账户异常', desc: '当前用户列表中没有管理员角色，请立即修复权限。', level: 'danger', path: '/admin/users' })
  return list
})

const quickActions = [
  { title: '用户权限', desc: '审查角色、删除异常账户', path: '/admin/users' },
  { title: '模型资产', desc: '检查官方/自定义模型边界', path: '/admin/models' },
  { title: 'AI 配置', desc: '启用 DeepSeek/OpenAI/Ollama', path: '/admin/ai' },
  { title: '数据训练', desc: '查看数据集与训练运行状态', path: '/admin/data' },
]

const statusType = (status: string) => {
  if (status === 'running') return 'primary'
  if (status === 'completed') return 'success'
  if (status === 'failed') return 'danger'
  if (status === 'paused') return 'warning'
  return 'info'
}

const statusText = (status: string) => {
  const map: Record<string, string> = { queued: '排队', running: '运行中', completed: '完成', failed: '失败', paused: '暂停', stopped: '停止' }
  return map[status] || status || '-'
}

const jobProgress = (job: any) => {
  const current = Number(job.currentEpoch ?? job.epochs ?? 0)
  const total = Number(job.epochs ?? job.totalEpochs ?? 0)
  if (total > 0) return Math.min(100, Math.round((current / total) * 100))
  return job.status === 'completed' ? 100 : job.status === 'running' ? 50 : 0
}

onMounted(() => {
  void refreshAll().catch(() => ElMessage.error('管理员总览加载失败'))
})
</script>

<style scoped>
.admin-overview {
  display: grid;
  gap: 18px;
}

.hero-panel,
.metric-card,
.ops-panel {
  border: 1px solid color-mix(in srgb, var(--primary-color) 14%, var(--border-color));
  border-radius: 18px;
  background:
    linear-gradient(135deg, rgba(var(--primary-rgb), 0.12), transparent 42%),
    rgba(var(--glass-bg-rgb), 0.48);
  box-shadow: var(--shadow-soft);
  backdrop-filter: blur(18px) saturate(150%);
}

.hero-panel {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
  padding: 26px;
}

.eyebrow,
.panel-head span,
.metric-card span {
  color: var(--text-muted);
  font-size: 11px;
  font-weight: var(--font-weight-title);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.hero-panel h1 {
  margin: 8px 0;
  color: var(--text-primary);
  font-size: clamp(28px, 4vw, 46px);
  font-weight: var(--font-weight-title);
  line-height: 1;
}

.hero-panel p {
  max-width: 760px;
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.75;
}

.hero-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 12px;
}

.metric-card {
  display: grid;
  gap: 8px;
  min-height: 132px;
  padding: 16px;
  cursor: pointer;
  transition: transform 180ms ease, border-color 180ms ease, background 180ms ease;
}

.metric-card:hover {
  transform: translateY(-2px);
  border-color: color-mix(in srgb, var(--primary-color) 36%, var(--border-color));
}

.metric-card strong {
  color: var(--text-primary);
  font-size: 34px;
  font-weight: var(--font-weight-title);
}

.metric-card em,
.panel-note {
  color: var(--text-secondary);
  font-size: 12px;
  font-style: normal;
  line-height: 1.55;
}

.ops-grid {
  display: grid;
  grid-template-columns: 1.25fr 0.9fr;
  gap: 14px;
}

.ops-panel {
  min-width: 0;
  padding: 18px;
}

.ops-panel.wide {
  grid-column: span 1;
}

.panel-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.panel-head strong {
  color: var(--text-primary);
  font-size: 18px;
  font-weight: var(--font-weight-title);
}

.risk-list,
.quick-actions,
.ai-status {
  display: grid;
  gap: 10px;
}

.risk-item,
.ai-status div,
.quick-actions button,
.empty-state {
  border: 1px solid rgba(148, 163, 184, 0.16);
  border-radius: 14px;
  background: rgba(var(--glass-bg-rgb), 0.32);
  padding: 12px;
}

.risk-item {
  display: grid;
  grid-template-columns: minmax(0, 0.8fr) minmax(0, 1.4fr) auto;
  align-items: center;
  gap: 12px;
}

.risk-item b,
.quick-actions b,
.ai-status b {
  color: var(--text-primary);
  font-weight: var(--font-weight-title);
}

.risk-item span,
.quick-actions span,
.ai-status span {
  color: var(--text-secondary);
  font-size: 12px;
}

.risk-item.danger {
  border-color: color-mix(in srgb, var(--tone-rose) 34%, var(--border-color));
}

.risk-item.warning {
  border-color: color-mix(in srgb, var(--tone-amber) 34%, var(--border-color));
}

.quick-actions {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.quick-actions button {
  display: grid;
  gap: 6px;
  text-align: left;
  cursor: pointer;
  transition: transform 160ms ease, border-color 160ms ease;
}

.quick-actions button:hover {
  transform: translateY(-1px);
  border-color: color-mix(in srgb, var(--primary-color) 32%, var(--border-color));
}

.empty-state {
  color: var(--text-secondary);
  font-size: 13px;
}

@media (max-width: 1180px) {
  .metric-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .ops-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .hero-panel {
    align-items: flex-start;
    flex-direction: column;
  }

  .metric-grid,
  .quick-actions {
    grid-template-columns: 1fr;
  }

  .risk-item {
    grid-template-columns: 1fr;
  }
}
</style>
