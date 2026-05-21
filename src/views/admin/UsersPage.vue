<template>
  <div class="admin-section">
    <section class="section-hero">
      <div>
        <span>USER ACCESS</span>
        <h2 class="page-title">{{ t('admin.users') }}</h2>
        <p>集中管理用户角色。管理员权限会影响官方资产、AI 配置和后台内容发布，需要谨慎变更。</p>
      </div>
      <el-button type="primary" @click="load">刷新</el-button>
    </section>

    <section class="summary-row">
      <div v-for="item in summary" :key="item.label" class="summary-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </div>
    </section>

    <section class="toolbar-card">
      <el-input v-model="keyword" clearable placeholder="搜索用户名 / 邮箱" />
      <el-select v-model="roleFilter" placeholder="角色" clearable>
        <el-option label="全部角色" value="" />
        <el-option label="管理员" value="ADMIN" />
        <el-option label="研究员" value="RESEARCHER" />
        <el-option label="普通用户" value="USER" />
      </el-select>
    </section>

    <el-table :data="filteredUsers" stripe max-height="620">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="username" :label="t('login.username')" min-width="160" />
      <el-table-column prop="email" :label="t('admin.email')" min-width="220" />
      <el-table-column prop="role" :label="t('admin.role')" width="140">
        <template #default="{ row }">
          <el-tag :type="roleTag(row.role)" size="small">{{ roleLabel(row.role) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" :label="t('admin.createdAt')" min-width="180" />
      <el-table-column :label="t('admin.action')" width="300" fixed="right">
        <template #default="{ row }">
          <el-button size="small" :disabled="row.role === 'USER'" @click="setRole(row, 'USER')">{{ t('admin.setUser') }}</el-button>
          <el-button size="small" type="warning" :disabled="row.role === 'ADMIN'" @click="setRole(row, 'ADMIN')">{{ t('admin.setAdmin') }}</el-button>
          <el-button size="small" type="danger" @click="delUser(row)">{{ t('admin.delete') }}</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api'

const { t } = useI18n()
const users = ref<any[]>([])
const keyword = ref('')
const roleFilter = ref('')

const load = async () => {
  const response = await adminApi.users.list()
  if (response.data.code === 200) users.value = response.data.data || []
}

const filteredUsers = computed(() => {
  const q = keyword.value.trim().toLowerCase()
  return users.value.filter((user) => {
    const matchesKeyword = !q || String(user.username || '').toLowerCase().includes(q) || String(user.email || '').toLowerCase().includes(q)
    const matchesRole = !roleFilter.value || user.role === roleFilter.value
    return matchesKeyword && matchesRole
  })
})

const summary = computed(() => [
  { label: '全部用户', value: users.value.length },
  { label: '管理员', value: users.value.filter((user) => user.role === 'ADMIN').length },
  { label: '研究员', value: users.value.filter((user) => user.role === 'RESEARCHER').length },
  { label: '普通用户', value: users.value.filter((user) => user.role === 'USER').length },
])

const roleTag = (role: string) => role === 'ADMIN' ? 'danger' : role === 'RESEARCHER' ? 'warning' : 'info'
const roleLabel = (role: string) => ({ ADMIN: '管理员', RESEARCHER: '研究员', USER: '普通用户' }[role] || role)

const setRole = async (row: any, role: string) => {
  await ElMessageBox.confirm(`确认将 ${row.username} 的角色改为 ${roleLabel(role)}？`)
  await adminApi.users.setRole(row.id, role)
  ElMessage.success(`${row.username} 已更新为 ${roleLabel(role)}`)
  await load()
}

const delUser = async (row: any) => {
  await ElMessageBox.confirm(`确认删除用户 ${row.username}？该操作不可恢复。`, '危险操作', { type: 'warning' })
  await adminApi.users.delete(row.id)
  ElMessage.success('用户已删除')
  await load()
}

onMounted(load)
</script>

<style scoped>
.admin-section {
  display: grid;
  gap: 16px;
}

.section-hero,
.toolbar-card,
.summary-card {
  border: 1px solid color-mix(in srgb, var(--primary-color) 14%, var(--border-color));
  border-radius: 18px;
  background: rgba(var(--glass-bg-rgb), 0.42);
  box-shadow: var(--shadow-soft);
  backdrop-filter: blur(16px);
}

.section-hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  padding: 22px;
}

.section-hero span,
.summary-card span {
  color: var(--text-muted);
  font-size: 11px;
  font-weight: var(--font-weight-title);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.section-hero p {
  max-width: 760px;
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.7;
}

.summary-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.summary-card {
  display: grid;
  gap: 8px;
  padding: 16px;
}

.summary-card strong {
  color: var(--text-primary);
  font-size: 28px;
  font-weight: var(--font-weight-title);
}

.toolbar-card {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) 180px;
  gap: 12px;
  padding: 14px;
}

@media (max-width: 820px) {
  .section-hero {
    align-items: flex-start;
    flex-direction: column;
  }

  .summary-row,
  .toolbar-card {
    grid-template-columns: 1fr;
  }
}
</style>
