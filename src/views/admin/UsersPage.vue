<template>
  <div>
    <h2 class="page-title">{{ t('admin.users') }}</h2>
    <el-table :data="users" stripe max-height="600">
      <el-table-column prop="id" label="ID" width="60"/>
      <el-table-column prop="username" :label="t('login.username')" width="150"/>
      <el-table-column prop="email" :label="t('admin.email')" width="200"/>
      <el-table-column prop="role" :label="t('admin.role')" width="120">
        <template #default="{row}">
          <el-tag :type="row.role==='ADMIN'?'danger':row.role==='RESEARCHER'?'warning':'info'" size="small">{{ row.role }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" :label="t('admin.createdAt')" min-width="160"/>
      <el-table-column :label="t('admin.action')" width="220">
        <template #default="{row}">
          <el-button size="small" @click="setRole(row,'USER')">{{ t('admin.setUser') }}</el-button>
          <el-button size="small" @click="setRole(row,'ADMIN')" type="warning">{{ t('admin.setAdmin') }}</el-button>
          <el-button size="small" type="danger" @click="delUser(row)">{{ t('admin.delete') }}</el-button>
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
const users = ref<any[]>([])
const load = () => adminApi.users.list().then(r => { if(r.data.code===200) users.value = r.data.data })
const setRole = async (row:any, role:string) => {
  await adminApi.users.setRole(row.id, role)
  ElMessage.success(`${row.username} → ${role}`); load()
}
const delUser = async (row:any) => {
  try { await ElMessageBox.confirm(`删除 ${row.username}?`); await adminApi.users.delete(row.id); ElMessage.success('已删除'); load() } catch { /* cancelled */ }
}
onMounted(load)
</script>
<style scoped>.page-title{font-size:20px;font-weight:800;color:var(--text-primary);margin:0 0 16px}</style>
