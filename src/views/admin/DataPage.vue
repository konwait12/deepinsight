<template>
  <div class="admin-page">
    <section class="admin-hero">
      <div>
        <span>{{ copy.eyebrow }}</span>
        <h2 class="page-title">{{ copy.title }}</h2>
        <p>{{ copy.subtitle }}</p>
      </div>
      <div class="hero-actions">
        <el-button :loading="loading" @click="load">{{ copy.refresh }}</el-button>
      </div>
    </section>

    <section class="admin-metric-grid">
      <article>
        <span>{{ copy.datasets }}</span>
        <strong>{{ datasets.length }}</strong>
        <small>{{ copy.datasetsHint }}</small>
      </article>
      <article>
        <span>{{ copy.samples }}</span>
        <strong>{{ totalSamples }}</strong>
        <small>{{ copy.samplesHint }}</small>
      </article>
      <article>
        <span>{{ copy.jobs }}</span>
        <strong>{{ jobs.length }}</strong>
        <small>{{ copy.jobsHint }}</small>
      </article>
      <article>
        <span>{{ copy.running }}</span>
        <strong>{{ runningJobs }}</strong>
        <small>{{ copy.runningHint }}</small>
      </article>
    </section>

    <section class="admin-toolbar">
      <div class="toolbar-left">
        <el-input v-model="query" clearable :placeholder="copy.search" style="width: 300px" />
        <el-select v-model="statusFilter" style="width: 170px">
          <el-option :label="copy.allStatus" value="all" />
          <el-option v-for="status in statuses" :key="status" :label="status" :value="status" />
        </el-select>
      </div>
      <div class="toolbar-right">
        <el-tag>{{ copy.datasetShowing }} {{ filteredDatasets.length }}</el-tag>
        <el-tag>{{ copy.jobShowing }} {{ filteredJobs.length }}</el-tag>
      </div>
    </section>

    <section class="admin-grid two">
      <div class="admin-panel">
        <div class="admin-panel-head">
          <div>
            <span>{{ copy.datasets }}</span>
            <strong>{{ copy.datasetTable }}</strong>
          </div>
        </div>
        <el-table v-if="filteredDatasets.length" :data="filteredDatasets" stripe max-height="400">
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="name" :label="copy.name" min-width="220" />
          <el-table-column prop="taskType" :label="copy.type" width="130" />
          <el-table-column prop="sampleCount" :label="copy.samples" width="120" />
          <el-table-column prop="status" :label="copy.status" width="120">
            <template #default="{ row }">
              <el-tag :type="statusTag(row.status)" size="small">{{ row.status || '-' }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
        <div v-else class="empty-hint">{{ copy.emptyDatasets }}</div>
      </div>

      <div class="admin-panel">
        <div class="admin-panel-head">
          <div>
            <span>{{ copy.jobs }}</span>
            <strong>{{ copy.jobTable }}</strong>
          </div>
        </div>
        <el-table v-if="filteredJobs.length" :data="filteredJobs" stripe max-height="400">
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="name" :label="copy.name" min-width="190" />
          <el-table-column prop="modelArchitecture" :label="copy.model" min-width="140" />
          <el-table-column prop="status" :label="copy.status" width="120">
            <template #default="{ row }">
              <el-tag :type="statusTag(row.status)" size="small">{{ row.status || '-' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="currentEpoch" label="Epoch" width="90" />
          <el-table-column prop="currentLoss" label="Loss" width="90" />
          <el-table-column prop="createdBy" :label="copy.user" width="110" />
        </el-table>
        <div v-else class="empty-hint">{{ copy.emptyJobs }}</div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { adminApi } from '@/api'

const { locale } = useI18n()
const datasets = ref<any[]>([])
const jobs = ref<any[]>([])
const loading = ref(false)
const query = ref('')
const statusFilter = ref('all')
const isZh = computed(() => locale.value.startsWith('zh'))
const copy = computed(() => isZh.value ? {
  eyebrow: '数据资产',
  title: '数据集与训练任务',
  subtitle: '查看用户上传数据集、站内数据资产和训练任务记录。这里只展示真实后端记录，不伪造训练结果。',
  refresh: '刷新',
  datasets: '数据集',
  datasetsHint: '当前记录',
  samples: '样本',
  samplesHint: '样本数合计',
  jobs: '训练任务',
  jobsHint: '任务记录',
  running: '运行中',
  runningHint: '当前训练状态',
  search: '搜索数据集或任务',
  allStatus: '全部状态',
  datasetShowing: '数据集',
  jobShowing: '任务',
  datasetTable: '数据集记录',
  jobTable: '训练任务记录',
  name: '名称',
  type: '类型',
  status: '状态',
  model: '模型',
  user: '用户',
  emptyDatasets: '没有匹配的数据集',
  emptyJobs: '没有匹配的训练任务',
  loadFailed: '数据管理记录加载失败',
} : {
  eyebrow: 'Data Assets',
  title: 'Datasets and Training Jobs',
  subtitle: 'Review uploaded datasets, built-in data assets, and training job records. This page only shows real backend records.',
  refresh: 'Refresh',
  datasets: 'Datasets',
  datasetsHint: 'Current records',
  samples: 'Samples',
  samplesHint: 'Total samples',
  jobs: 'Training jobs',
  jobsHint: 'Job records',
  running: 'Running',
  runningHint: 'Active training state',
  search: 'Search dataset or job',
  allStatus: 'All status',
  datasetShowing: 'Datasets',
  jobShowing: 'Jobs',
  datasetTable: 'Dataset records',
  jobTable: 'Training job records',
  name: 'Name',
  type: 'Type',
  status: 'Status',
  model: 'Model',
  user: 'User',
  emptyDatasets: 'No matching datasets',
  emptyJobs: 'No matching training jobs',
  loadFailed: 'Failed to load data admin records',
})

const totalSamples = computed(() => datasets.value.reduce((sum, item) => sum + Number(item.sampleCount || 0), 0))
const runningJobs = computed(() => jobs.value.filter((item) => ['running', 'queued', 'pending'].includes(String(item.status || '').toLowerCase())).length)
const statuses = computed(() => Array.from(new Set([...datasets.value, ...jobs.value].map((item) => item.status).filter(Boolean))))
const filteredDatasets = computed(() => {
  const keyword = query.value.trim().toLowerCase()
  return datasets.value.filter((item) => {
    const matchStatus = statusFilter.value === 'all' || item.status === statusFilter.value
    const matchKeyword = !keyword || `${item.name || ''} ${item.taskType || ''}`.toLowerCase().includes(keyword)
    return matchStatus && matchKeyword
  })
})
const filteredJobs = computed(() => {
  const keyword = query.value.trim().toLowerCase()
  return jobs.value.filter((item) => {
    const matchStatus = statusFilter.value === 'all' || item.status === statusFilter.value
    const matchKeyword = !keyword || `${item.name || ''} ${item.modelArchitecture || ''}`.toLowerCase().includes(keyword)
    return matchStatus && matchKeyword
  })
})

function statusTag(status: string) {
  const normalized = String(status || '').toLowerCase()
  if (['completed', 'ready', 'active', 'success'].includes(normalized)) return 'success'
  if (['running', 'queued', 'pending'].includes(normalized)) return 'primary'
  if (['failed', 'error'].includes(normalized)) return 'danger'
  return 'info'
}

async function load() {
  loading.value = true
  try {
    const [d, tRes] = await Promise.all([adminApi.datasets.list(), adminApi.trainingJobs.list()])
    if (d.data.code === 200) datasets.value = d.data.data || []
    if (tRes.data.code === 200) jobs.value = tRes.data.data || []
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || error?.message || copy.value.loadFailed)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
