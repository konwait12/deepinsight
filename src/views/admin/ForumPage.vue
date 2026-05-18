<template>
  <div>
    <h2 class="page-title">{{ t('admin.forum') }}</h2>
    <el-table :data="posts" stripe max-height="600">
      <el-table-column prop="id" label="ID" width="50" />
      <el-table-column prop="title" :label="t('admin.titleColumn')" min-width="300" />
      <el-table-column prop="userId" :label="t('admin.user')" width="90" />
      <el-table-column :label="t('admin.official')" width="90">
        <template #default="{ row }">{{ row.official ? 'Y' : '' }}</template>
      </el-table-column>
      <el-table-column prop="viewCount" :label="t('admin.views')" width="90" />
      <el-table-column prop="createdAt" :label="t('admin.createdAt')" width="170" />
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
const posts = ref<any[]>([])
const load = () => adminApi.forumPosts.list().then(r => {
  if (r.data.code === 200) posts.value = (r.data.data || []).map((p: any) => ({ ...p, official: p.is_official || p.isOfficial }))
})
const del = async (r: any) => {
  try {
    await ElMessageBox.confirm(`${t('admin.delete')} ${r.title}?`)
    await adminApi.forumPosts.delete(r.id)
    ElMessage.success(t('admin.delete'))
    load()
  } catch { /* cancelled */ }
}
onMounted(load)
</script>

<style scoped>
.page-title{font-size:20px;font-weight:800;color:var(--text-primary);margin:0 0 16px}
</style>
