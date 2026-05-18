<template>
  <div>
    <h2 class="page-title">{{ t('admin.dataTraining') }}</h2>
    <h4 class="sub">{{ t('admin.dataset') }} ({{ datasets.length }})</h4>
    <el-table :data="datasets" stripe max-height="300">
      <el-table-column prop="id" label="ID" width="50" />
      <el-table-column prop="name" :label="t('admin.name')" width="220" />
      <el-table-column prop="taskType" :label="t('admin.type')" width="120" />
      <el-table-column prop="sampleCount" :label="t('admin.samples')" width="110" />
      <el-table-column prop="status" :label="t('admin.status')" width="100" />
    </el-table>

    <h4 class="sub mt">{{ t('admin.trainingJobs') }} ({{ jobs.length }})</h4>
    <el-table :data="jobs" stripe max-height="300">
      <el-table-column prop="id" label="ID" width="50" />
      <el-table-column prop="name" :label="t('admin.name')" width="220" />
      <el-table-column prop="modelArchitecture" :label="t('admin.model')" width="140" />
      <el-table-column prop="status" :label="t('admin.status')" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'running' ? 'primary' : row.status === 'completed' ? 'success' : 'info'" size="small">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="currentEpoch" label="Epoch" width="100" />
      <el-table-column prop="currentLoss" label="Loss" width="90" />
      <el-table-column prop="createdBy" :label="t('admin.user')" width="120" />
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { adminApi } from '@/api'

const { t } = useI18n()
const datasets = ref([])
const jobs = ref([])
onMounted(async () => {
  const [d, tRes] = await Promise.all([adminApi.datasets.list(), adminApi.trainingJobs.list()])
  if (d.data.code === 200) datasets.value = d.data.data
  if (tRes.data.code === 200) jobs.value = tRes.data.data
})
</script>

<style scoped>
.page-title{font-size:20px;font-weight:800;color:var(--text-primary);margin:0 0 16px}
.sub{font-size:13px;font-weight:800;color:var(--text-primary);margin:8px 0}
.mt{margin-top:16px}
</style>
