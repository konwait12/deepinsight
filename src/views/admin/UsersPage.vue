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
        <span>{{ copy.total }}</span>
        <strong>{{ users.length }}</strong>
        <small>{{ copy.totalHint }}</small>
      </article>
      <article>
        <span>ADMIN</span>
        <strong>{{ roleCount.ADMIN }}</strong>
        <small>{{ copy.adminHint }}</small>
      </article>
      <article>
        <span>RESEARCHER</span>
        <strong>{{ roleCount.RESEARCHER }}</strong>
        <small>{{ copy.researcherHint }}</small>
      </article>
      <article>
        <span>USER</span>
        <strong>{{ roleCount.USER }}</strong>
        <small>{{ copy.userHint }}</small>
      </article>
    </section>

    <section class="admin-toolbar">
      <div class="toolbar-left">
        <el-input v-model="query" clearable :placeholder="copy.search" style="width: 280px" />
        <el-select v-model="roleFilter" style="width: 160px">
          <el-option :label="copy.allRoles" value="all" />
          <el-option label="ADMIN" value="ADMIN" />
          <el-option label="RESEARCHER" value="RESEARCHER" />
          <el-option label="USER" value="USER" />
        </el-select>
      </div>
      <div class="toolbar-right">
        <el-tag>{{ copy.showing }} {{ filteredUsers.length }}</el-tag>
      </div>
    </section>

    <el-table v-if="filteredUsers.length" :data="filteredUsers" stripe max-height="620">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="username" :label="copy.username" min-width="150" />
      <el-table-column prop="email" :label="copy.email" min-width="220" />
      <el-table-column prop="role" :label="copy.role" width="130">
        <template #default="{ row }">
          <el-tag :type="row.role === 'ADMIN' ? 'danger' : row.role === 'RESEARCHER' ? 'warning' : 'info'" size="small">
            {{ row.role }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" :label="copy.createdAt" min-width="170" />
      <el-table-column :label="copy.action" width="270" fixed="right">
        <template #default="{ row }">
          <el-button size="small" :disabled="row.role === 'USER'" @click="setRole(row, 'USER')">{{ copy.setUser }}</el-button>
          <el-button size="small" type="warning" :disabled="row.role === 'ADMIN'" @click="setRole(row, 'ADMIN')">{{ copy.setAdmin }}</el-button>
          <el-button size="small" type="danger" @click="delUser(row)">{{ copy.delete }}</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div v-else class="empty-hint">{{ copy.empty }}</div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api'

const { locale } = useI18n()
const users = ref<any[]>([])
const loading = ref(false)
const query = ref('')
const roleFilter = ref('all')
const isZh = computed(() => locale.value.startsWith('zh'))
const copy = computed(() => isZh.value ? {
  eyebrow: '账号与权限',
  title: '用户管理',
  subtitle: '管理用户角色、管理员权限和异常账号。删除操作会影响对应账号登录，请谨慎处理。',
  refresh: '刷新',
  total: '全部账号',
  totalHint: '当前数据库记录',
  adminHint: '拥有后台权限',
  researcherHint: '研究者角色',
  userHint: '普通用户',
  search: '搜索用户名或邮箱',
  allRoles: '全部角色',
  showing: '当前显示',
  username: '用户名',
  email: '邮箱',
  role: '角色',
  createdAt: '创建时间',
  action: '操作',
  setUser: '设为用户',
  setAdmin: '设为管理员',
  delete: '删除',
  empty: '没有匹配的用户',
  roleChanged: '角色已更新',
  deleted: '用户已删除',
  confirmDelete: '确认删除用户',
  loadFailed: '用户列表加载失败',
} : {
  eyebrow: 'Accounts and Roles',
  title: 'User Management',
  subtitle: 'Manage user roles, administrator permissions, and abnormal accounts. Deletion affects account access.',
  refresh: 'Refresh',
  total: 'Accounts',
  totalHint: 'Database records',
  adminHint: 'Admin access',
  researcherHint: 'Research role',
  userHint: 'Standard users',
  search: 'Search username or email',
  allRoles: 'All roles',
  showing: 'Showing',
  username: 'Username',
  email: 'Email',
  role: 'Role',
  createdAt: 'Created at',
  action: 'Actions',
  setUser: 'Set User',
  setAdmin: 'Set Admin',
  delete: 'Delete',
  empty: 'No matching users',
  roleChanged: 'Role updated',
  deleted: 'User deleted',
  confirmDelete: 'Delete user',
  loadFailed: 'Failed to load users',
})

const roleCount = computed(() => ({
  ADMIN: users.value.filter((item) => item.role === 'ADMIN').length,
  RESEARCHER: users.value.filter((item) => item.role === 'RESEARCHER').length,
  USER: users.value.filter((item) => item.role === 'USER').length,
}))
const filteredUsers = computed(() => {
  const keyword = query.value.trim().toLowerCase()
  return users.value.filter((item) => {
    const matchRole = roleFilter.value === 'all' || item.role === roleFilter.value
    const matchKeyword = !keyword || `${item.username || ''} ${item.email || ''}`.toLowerCase().includes(keyword)
    return matchRole && matchKeyword
  })
})

async function load() {
  loading.value = true
  try {
    const response = await adminApi.users.list()
    if (response.data.code === 200) users.value = response.data.data || []
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || error?.message || copy.value.loadFailed)
  } finally {
    loading.value = false
  }
}

async function setRole(row: any, role: string) {
  await adminApi.users.setRole(row.id, role)
  ElMessage.success(`${copy.value.roleChanged}: ${row.username} -> ${role}`)
  load()
}

async function delUser(row: any) {
  try {
    await ElMessageBox.confirm(`${copy.value.confirmDelete} ${row.username}?`)
    await adminApi.users.delete(row.id)
    ElMessage.success(copy.value.deleted)
    load()
  } catch {
    // cancelled
  }
}

onMounted(load)
</script>
