<template>
  <div>
    <h2 class="page-title">{{ t('admin.knowledge') }}</h2>
    <h4 class="sub">{{ t('admin.nodes') }} ({{ nodes.length }})</h4>
    <el-table :data="nodes" stripe max-height="300">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="label" :label="t('admin.name')" width="160" />
      <el-table-column prop="category" :label="t('admin.type')" width="120" />
      <el-table-column prop="parentId" label="Parent" width="90" />
      <el-table-column prop="color" label="Color" width="100">
        <template #default="{ row }">
          <span :style="{ color: row.color }">●</span> {{ row.color }}
        </template>
      </el-table-column>
    </el-table>

    <h4 class="sub mt">{{ t('admin.articles') }} ({{ articles.length }})</h4>
    <el-table :data="articles" stripe max-height="300">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="title" :label="t('admin.titleColumn')" min-width="300" />
      <el-table-column prop="nodeId" label="Node" width="90" />
      <el-table-column prop="viewCount" :label="t('admin.views')" width="90" />
      <el-table-column :label="t('admin.action')" width="100">
        <template #default="{ row }">
          <el-button size="small" type="danger" @click="del(row)">{{ t('admin.delete') }}</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api'

const { t } = useI18n()
const nodes = ref([])
const articles = ref([])
const load = async () => {
  const [n, a] = await Promise.all([adminApi.knowledgeNodes.list(), adminApi.knowledgeArticles.list()])
  if (n.data.code === 200) nodes.value = n.data.data
  if (a.data.code === 200) articles.value = a.data.data
}
const del = async (r: any) => {
  try {
    await ElMessageBox.confirm(`${t('admin.delete')} ${r.title}?`)
    await adminApi.knowledgeArticles.delete(r.id)
    ElMessage.success(t('admin.delete'))
    load()
  } catch { /* cancelled */ }
}
onMounted(load)
</script>

<style scoped>
.page-title{font-size:20px;font-weight:800;color:var(--text-primary);margin:0 0 16px}
.sub{font-size:13px;font-weight:800;color:var(--text-primary);margin:8px 0}
.mt{margin-top:16px}
</style>
