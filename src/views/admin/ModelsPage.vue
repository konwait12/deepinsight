<template>
  <div>
    <h2 class="page-title">{{ t('admin.models') }}</h2>
    <el-table :data="models" stripe max-height="600">
      <el-table-column prop="id" label="ID" width="50" />
      <el-table-column :label="t('admin.name')" min-width="240">
        <template #default="{ row }">
          <span class="model-name">{{ displayModelName(row) }}</span>
          <small v-if="row.displayNameZh" class="raw-name">{{ row.name }}</small>
        </template>
      </el-table-column>
      <el-table-column :label="t('admin.taskType')" width="140">
        <template #default="{ row }">{{ displayTaskType(row) }}</template>
      </el-table-column>
      <el-table-column prop="paramCountM" :label="t('admin.params')" width="110" />
      <el-table-column prop="framework" :label="t('admin.framework')" width="110" />
      <el-table-column :label="t('admin.type')" width="110">
        <template #default="{ row }">
          <el-tag :type="row.isOfficial ? 'primary' : 'success'" size="small">
            {{ row.isOfficial ? t('admin.official') : t('admin.custom') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdBy" :label="t('admin.createdBy')" width="120" />
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
import { modelDisplayName, taskTypeLabel } from '@/utils/modelDisplay'

const { t, locale } = useI18n()
const models = ref<any[]>([])
const load = () => adminApi.models.list().then(r => { if (r.data.code === 200) models.value = r.data.data })
const displayModelName = (row: any) => modelDisplayName(row, locale)
const displayTaskType = (row: any) => taskTypeLabel(row.taskType, row.taskTypeZh, locale)
const del = async (r: any) => {
  try {
    await ElMessageBox.confirm(`${t('admin.delete')} ${r.name}?`)
    await adminApi.models.delete(r.id)
    ElMessage.success(t('admin.delete'))
    load()
  } catch { /* cancelled */ }
}
onMounted(load)
</script>

<style scoped>
.page-title{font-size:20px;font-weight:800;color:var(--text-primary);margin:0 0 16px}
.model-name{display:block;font-weight:800;color:var(--text-primary)}
.raw-name{display:block;margin-top:3px;color:var(--text-muted);font-size:11px}
</style>
