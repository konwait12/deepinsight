<template>
  <div class="admin-page">
    <section class="admin-hero">
      <div>
        <span>{{ copy.eyebrow }}</span>
        <h2 class="page-title">{{ copy.title }}</h2>
        <p>{{ copy.subtitle }}</p>
      </div>
      <div class="hero-actions">
        <el-button :loading="loading" @click="loadAll">{{ copy.refresh }}</el-button>
        <el-button type="primary" @click="router.push('/admin/ai')">{{ copy.aiConfig }}</el-button>
      </div>
    </section>

    <section class="admin-metric-grid">
      <article v-for="item in metrics" :key="item.key">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <small>{{ item.hint }}</small>
      </article>
    </section>

    <section class="admin-grid two">
      <div class="admin-panel">
        <div class="admin-panel-head">
          <div>
            <span>{{ copy.service }}</span>
            <strong>{{ copy.serviceTitle }}</strong>
          </div>
          <el-tag :type="statusOk ? 'success' : 'warning'">{{ statusOk ? copy.online : copy.partial }}</el-tag>
        </div>
        <div class="status-list">
          <div v-for="item in statusRows" :key="item.key">
            <span>{{ item.key }}</span>
            <strong>{{ item.value }}</strong>
          </div>
        </div>
      </div>

      <div class="admin-panel">
        <div class="admin-panel-head">
          <div>
            <span>{{ copy.shortcuts }}</span>
            <strong>{{ copy.shortcutsTitle }}</strong>
          </div>
        </div>
        <div class="shortcut-list">
          <button v-for="item in shortcuts" :key="item.path" type="button" @click="router.push(item.path)">
            <span>{{ item.title }}</span>
            <small>{{ item.desc }}</small>
          </button>
        </div>
      </div>
    </section>

    <section class="admin-grid two">
      <div class="admin-panel">
        <div class="admin-panel-head">
          <div>
            <span>{{ copy.content }}</span>
            <strong>{{ copy.contentTitle }}</strong>
          </div>
        </div>
        <el-table :data="contentRows" stripe>
          <el-table-column prop="name" :label="copy.name" min-width="150" />
          <el-table-column prop="count" :label="copy.count" width="100" />
          <el-table-column prop="note" :label="copy.note" min-width="180" />
        </el-table>
      </div>

      <div class="admin-panel">
        <div class="admin-panel-head">
          <div>
            <span>{{ copy.ai }}</span>
            <strong>{{ copy.aiTitle }}</strong>
          </div>
        </div>
        <el-table :data="aiRows" stripe>
          <el-table-column prop="name" :label="copy.name" min-width="140" />
          <el-table-column prop="model" :label="copy.model" min-width="150" />
          <el-table-column prop="active" :label="copy.state" width="100">
            <template #default="{ row }">
              <el-tag :type="row.active ? 'success' : 'info'" size="small">
                {{ row.active ? copy.enabled : copy.disabled }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { adminApi } from '@/api'

const router = useRouter()
const { locale } = useI18n()
const loading = ref(false)
const status = ref<Record<string, number>>({})
const users = ref<any[]>([])
const configs = ref<any[]>([])
const nodes = ref<any[]>([])
const articles = ref<any[]>([])
const posts = ref<any[]>([])
const datasets = ref<any[]>([])
const jobs = ref<any[]>([])

const isZh = computed(() => locale.value.startsWith('zh'))
const copy = computed(() => isZh.value ? {
  eyebrow: '管理中心',
  title: '站点管理总览',
  subtitle: '集中查看账号、AI、知识库、社区内容、数据集和训练任务状态。',
  refresh: '刷新',
  aiConfig: '配置 AI',
  service: '服务',
  serviceTitle: '后端与数据状态',
  online: '正常',
  partial: '待检查',
  shortcuts: '快捷操作',
  shortcutsTitle: '常用管理入口',
  content: '内容',
  contentTitle: '站内内容资产',
  ai: 'AI',
  aiTitle: '模型配置状态',
  name: '名称',
  count: '数量',
  note: '说明',
  model: '模型',
  state: '状态',
  enabled: '启用',
  disabled: '停用',
  loadFailed: '管理数据加载失败',
  users: '用户',
  configs: 'AI 配置',
  knowledge: '知识节点',
  articles: '知识文章',
  forum: '社区内容',
  datasets: '数据集',
  jobs: '训练任务',
} : {
  eyebrow: 'Admin Center',
  title: 'Site Administration',
  subtitle: 'Review accounts, AI, knowledge content, forum posts, datasets, and training jobs in one place.',
  refresh: 'Refresh',
  aiConfig: 'Configure AI',
  service: 'Service',
  serviceTitle: 'Backend and data status',
  online: 'Healthy',
  partial: 'Check',
  shortcuts: 'Shortcuts',
  shortcutsTitle: 'Common admin entries',
  content: 'Content',
  contentTitle: 'Site content assets',
  ai: 'AI',
  aiTitle: 'Model configuration state',
  name: 'Name',
  count: 'Count',
  note: 'Note',
  model: 'Model',
  state: 'State',
  enabled: 'Enabled',
  disabled: 'Disabled',
  loadFailed: 'Failed to load admin data',
  users: 'Users',
  configs: 'AI configs',
  knowledge: 'Knowledge nodes',
  articles: 'Articles',
  forum: 'Forum posts',
  datasets: 'Datasets',
  jobs: 'Training jobs',
})

const metrics = computed(() => [
  { key: 'users', label: copy.value.users, value: users.value.length, hint: isZh.value ? '账号与权限' : 'Accounts and roles' },
  { key: 'ai', label: copy.value.configs, value: configs.value.length, hint: isZh.value ? `${configs.value.filter((item) => item.isActive).length} 个启用` : `${configs.value.filter((item) => item.isActive).length} active` },
  { key: 'articles', label: copy.value.articles, value: articles.value.length, hint: isZh.value ? '知识库内容' : 'Knowledge content' },
  { key: 'datasets', label: copy.value.datasets, value: datasets.value.length, hint: isZh.value ? `${jobs.value.length} 个训练任务` : `${jobs.value.length} training jobs` },
])

const statusRows = computed(() => Object.entries(status.value).map(([key, value]) => ({ key, value })))
const statusOk = computed(() => statusRows.value.length > 0)
const contentRows = computed(() => [
  { name: copy.value.knowledge, count: nodes.value.length, note: isZh.value ? '知识图谱节点' : 'Knowledge graph nodes' },
  { name: copy.value.articles, count: articles.value.length, note: isZh.value ? '可被 AI 检索的文章' : 'AI-retrievable articles' },
  { name: copy.value.forum, count: posts.value.length, note: isZh.value ? '社区与官方内容' : 'Community and official posts' },
  { name: copy.value.datasets, count: datasets.value.length, note: isZh.value ? '用户上传和站内数据' : 'User and built-in datasets' },
])
const aiRows = computed(() => configs.value.slice(0, 8).map((item) => ({
  name: item.name || '-',
  model: item.modelName || '-',
  active: Boolean(item.isActive),
})))
const shortcuts = computed(() => [
  { path: '/admin/users', title: copy.value.users, desc: isZh.value ? '角色、账号删除' : 'Roles and account removal' },
  { path: '/admin/ai', title: copy.value.configs, desc: isZh.value ? '供应商、密钥、知识训练' : 'Provider, key, knowledge training' },
  { path: '/admin/kb', title: copy.value.articles, desc: isZh.value ? '知识节点与文章' : 'Knowledge nodes and articles' },
  { path: '/admin/forum', title: copy.value.forum, desc: isZh.value ? '社区内容维护' : 'Community moderation' },
  { path: '/admin/data', title: copy.value.datasets, desc: isZh.value ? '数据集与训练任务' : 'Datasets and training jobs' },
])

async function loadAll() {
  loading.value = true
  try {
    const [s, u, c, n, a, p, d, j] = await Promise.all([
      adminApi.status(),
      adminApi.users.list(),
      adminApi.aiConfigs.list(),
      adminApi.knowledgeNodes.list(),
      adminApi.knowledgeArticles.list(),
      adminApi.forumPosts.list(),
      adminApi.datasets.list(),
      adminApi.trainingJobs.list(),
    ])
    if (s.data.code === 200) status.value = s.data.data || {}
    if (u.data.code === 200) users.value = u.data.data || []
    if (c.data.code === 200) configs.value = c.data.data || []
    if (n.data.code === 200) nodes.value = n.data.data || []
    if (a.data.code === 200) articles.value = a.data.data || []
    if (p.data.code === 200) posts.value = p.data.data || []
    if (d.data.code === 200) datasets.value = d.data.data || []
    if (j.data.code === 200) jobs.value = j.data.data || []
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || error?.message || copy.value.loadFailed)
  } finally {
    loading.value = false
  }
}

onMounted(loadAll)
</script>
