<template>
  <div class="admin-page">
    <div v-if="!authorized" class="denied">
      <el-icon :size="48"><WarningFilled /></el-icon>
      <h2>需要管理员权限</h2>
      <p>当前账号没有管理员权限，请使用管理员账号登录。</p>
      <el-button type="primary" @click="$router.push('/login')">切换账号</el-button>
    </div>

    <template v-else>
      <div class="admin-header">
        <h1>DeepInsight 管理控制台</h1>
        <div class="header-info">
          <span class="dot online"></span> MySQL: deepinsight@localhost
          <span class="dot online"></span> Redis: localhost:6379
          <span v-for="(v,k) in stats" :key="k" class="stat-chip">{{ k }}: {{ v }}</span>
        </div>
      </div>

      <el-tabs v-model="tab" type="border-card" class="admin-tabs">
        <!-- Users -->
        <el-tab-pane label="用户管理" name="users">
          <el-table :data="users" stripe max-height="500">
            <el-table-column prop="id" label="ID" width="60"/>
            <el-table-column prop="username" label="用户名" width="150"/>
            <el-table-column prop="email" label="邮箱" width="200"/>
            <el-table-column prop="role" label="角色" width="120">
              <template #default="{row}">
                <el-tag :type="row.role==='ADMIN'?'danger':row.role==='RESEARCHER'?'warning':'info'" size="small">{{ row.role }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="注册时间" min-width="160"/>
            <el-table-column label="操作" width="200">
              <template #default="{row}">
                <el-button size="small" @click="setRole(row,'USER')">设为用户</el-button>
                <el-button size="small" @click="setRole(row,'ADMIN')" type="warning">设为管理</el-button>
                <el-button size="small" type="danger" @click="delUser(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- Models -->
        <el-tab-pane label="模型管理" name="models">
          <el-table :data="adminModels" stripe max-height="500">
            <el-table-column prop="id" label="ID" width="50"/>
            <el-table-column prop="name" label="名称" width="180"/>
            <el-table-column prop="taskType" label="类型" width="100"/>
            <el-table-column prop="paramCountM" label="参数量(M)" width="100"/>
            <el-table-column prop="framework" label="框架" width="90"/>
            <el-table-column label="官方/自定义" width="100">
              <template #default="{row}"><el-tag :type="row.isOfficial?'primary':'success'" size="small">{{ row.isOfficial?'官方':'自定义' }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="createdBy" label="创建者" width="100"/>
            <el-table-column label="操作" width="100">
              <template #default="{row}"><el-button size="small" type="danger" @click="delModel(row)">删除</el-button></template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- Knowledge -->
        <el-tab-pane label="知识库" name="kb">
          <h4 class="section-title">知识节点 ({{kbNodes.length}})</h4>
          <el-table :data="kbNodes" stripe max-height="250">
            <el-table-column prop="id" label="ID" width="80"/>
            <el-table-column prop="label" label="标签" width="150"/>
            <el-table-column prop="category" label="分类" width="100"/>
            <el-table-column prop="parentId" label="父节点" width="80"/>
            <el-table-column prop="color" label="颜色" width="80"><template #default="{row}"><span :style="{color:row.color}">●</span> {{ row.color }}</template></el-table-column>
          </el-table>
          <h4 class="section-title mt-4">知识文章 ({{kbArticles.length}})</h4>
          <el-table :data="kbArticles" stripe max-height="250">
            <el-table-column prop="id" label="ID" width="50"/>
            <el-table-column prop="title" label="标题" min-width="300"/>
            <el-table-column prop="nodeId" label="节点" width="80"/>
            <el-table-column prop="viewCount" label="浏览" width="70"/>
            <el-table-column label="操作" width="100">
              <template #default="{row}"><el-button size="small" type="danger" @click="delArticle(row)">删除</el-button></template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- Forum -->
        <el-tab-pane label="论坛管理" name="forum">
          <el-table :data="forumPosts" stripe max-height="500">
            <el-table-column prop="id" label="ID" width="50"/>
            <el-table-column prop="title" label="标题" min-width="300"/>
            <el-table-column prop="userId" label="用户ID" width="80"/>
            <el-table-column label="官方" width="60"><template #default="{row}">{{ row.isOfficial?'Y':'' }}</template></el-table-column>
            <el-table-column prop="viewCount" label="浏览" width="70"/>
            <el-table-column prop="createdAt" label="时间" width="160"/>
            <el-table-column label="操作" width="100">
              <template #default="{row}"><el-button size="small" type="danger" @click="delPost(row)">删除</el-button></template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- Training & Datasets -->
        <el-tab-pane label="数据/训练" name="data">
          <h4 class="section-title">数据集 ({{datasets.length}})</h4>
          <el-table :data="datasets" stripe max-height="250">
            <el-table-column prop="id" label="ID" width="50"/>
            <el-table-column prop="name" label="名称" width="200"/>
            <el-table-column prop="taskType" label="类型" width="100"/>
            <el-table-column prop="sampleCount" label="样本数" width="100"/>
            <el-table-column prop="status" label="状态" width="80"/>
          </el-table>
          <h4 class="section-title mt-4">训练任务 ({{trainingJobs.length}})</h4>
          <el-table :data="trainingJobs" stripe max-height="250">
            <el-table-column prop="id" label="ID" width="50"/>
            <el-table-column prop="name" label="名称" width="200"/>
            <el-table-column prop="modelArchitecture" label="模型" width="120"/>
            <el-table-column prop="status" label="状态" width="80">
              <template #default="{row}"><el-tag :type="row.status==='running'?'primary':row.status==='completed'?'success':'info'" size="small">{{ row.status }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="currentEpoch" label="Epoch" width="100"/>
            <el-table-column prop="currentLoss" label="Loss" width="80"/>
            <el-table-column prop="createdBy" label="用户" width="100"/>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { WarningFilled } from '@element-plus/icons-vue'
import { authApi, adminApi } from '@/api'

const tab = ref('users')
const authorized = ref(false)
const stats = ref<Record<string,number>>({})

const users = ref<any[]>([])
const adminModels = ref<any[]>([])
const kbNodes = ref<any[]>([])
const kbArticles = ref<any[]>([])
const forumPosts = ref<any[]>([])
const datasets = ref<any[]>([])
const trainingJobs = ref<any[]>([])

const safeList = (fn: () => Promise<any>) => fn().then(r => (r.data.code === 200 ? r.data.data : [])).catch(() => [])

const refreshAll = async () => {
  const [s, u, m, kn, ka, fp, ds, tj] = await Promise.all([
    adminApi.status().then(r => r.data.data).catch(() => ({})),
    safeList(() => adminApi.users.list()),
    safeList(() => adminApi.models.list()),
    safeList(() => adminApi.knowledgeNodes.list()),
    safeList(() => adminApi.knowledgeArticles.list()),
    safeList(() => adminApi.forumPosts.list()),
    safeList(() => adminApi.datasets.list()),
    safeList(() => adminApi.trainingJobs.list()),
  ])
  stats.value = s
  users.value = u; adminModels.value = m; kbNodes.value = kn; kbArticles.value = ka
  forumPosts.value = fp; datasets.value = ds; trainingJobs.value = tj
}

const setRole = async (row: any, role: string) => {
  await adminApi.users.setRole(row.id, role)
  ElMessage.success(`${row.username} → ${role}`); refreshAll()
}
const delUser = async (row: any) => {
  try { await ElMessageBox.confirm(`删除用户 ${row.username}?`); await adminApi.users.delete(row.id); ElMessage.success('已删除'); refreshAll() } catch { /* cancelled */ }
}
const delModel = async (row: any) => {
  try { await ElMessageBox.confirm(`删除模型 ${row.name}?`); await adminApi.models.delete(row.id); ElMessage.success('已删除'); refreshAll() } catch { /* cancelled */ }
}
const delArticle = async (row: any) => {
  try { await ElMessageBox.confirm(`删除文章 ${row.title}?`); await adminApi.knowledgeArticles.delete(row.id); ElMessage.success('已删除'); refreshAll() } catch { /* cancelled */ }
}
const delPost = async (row: any) => {
  try { await ElMessageBox.confirm(`删除帖子 ${row.title}?`); await adminApi.forumPosts.delete(row.id); ElMessage.success('已删除'); refreshAll() } catch { /* cancelled */ }
}

onMounted(async () => {
  try {
    const me = await authApi.me()
    if (me.data.code === 200 && me.data.data.role === 'ADMIN') { authorized.value = true; refreshAll() }
  } catch { /* auth check failed */ }
})
</script>

<style scoped>
.admin-page { padding: 24px; max-width: 1500px; margin: 0 auto; min-height: 100vh; background: var(--bg-color); }
.denied { text-align: center; padding: 120px; color: var(--text-secondary); }
.denied h2 { color: var(--text-primary); margin: 16px 0 8px; }
.denied p { margin: 0 0 20px; }
.admin-header { margin-bottom: 20px; }
.admin-header h1 { font-size: 24px; font-weight: var(--font-weight-title); color: var(--text-primary); margin: 0 0 8px; }
.header-info { display: flex; flex-wrap: wrap; gap: 12px; font-size: 12px; color: var(--text-secondary); }
.dot { width: 8px; height: 8px; border-radius: 50%; display: inline-block; }
.dot.online { background: #4ade80; }
.stat-chip { background: var(--bg-input); padding: 2px 8px; border-radius: 6px; font-size: 11px; }
.admin-tabs { border-radius: 16px; overflow: hidden; }
.section-title { font-size: 13px; font-weight: var(--font-weight-body); color: var(--text-primary); margin: 8px 0 8px; }
.mt-4 { margin-top: 16px }
</style>
