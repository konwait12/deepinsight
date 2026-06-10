<template>
  <div class="profile-page">
    <div class="profile-container entrance-up">
      <div v-if="!user" class="empty">
        <strong>{{ copy.loginRequired }}</strong>
        <span>{{ copy.loginRequiredDesc }}</span>
      </div>

      <template v-else>
        <section class="profile-hero">
          <div class="avatar-circle">{{ avatarLetter }}</div>
          <div>
            <span class="eyebrow">{{ copy.accountCenter }}</span>
            <h1>{{ user.username }}</h1>
            <p class="email">{{ user.email || copy.emailUnset }}</p>
          </div>
          <el-tag :type="roleTag(user.role)" size="small">{{ roleLabel(user.role) }}</el-tag>
        </section>

        <el-row :gutter="16" class="stats-row">
          <el-col :xs="24" :sm="8">
            <el-card shadow="never" class="stat-card">
              <div class="stat-val primary">{{ stats.totalJobs }}</div>
              <div class="stat-label">{{ copy.trainingJobs }}</div>
            </el-card>
          </el-col>
          <el-col :xs="24" :sm="8">
            <el-card shadow="never" class="stat-card">
              <div class="stat-val success">{{ stats.completedJobs }}</div>
              <div class="stat-label">{{ copy.completedJobs }}</div>
            </el-card>
          </el-col>
          <el-col :xs="24" :sm="8">
            <el-card shadow="never" class="stat-card">
              <div class="stat-val accent">{{ stats.runningJobs }}</div>
              <div class="stat-label">{{ copy.runningJobs }}</div>
            </el-card>
          </el-col>
        </el-row>

        <el-card shadow="never" class="section-card">
          <template #header><span class="section-title">{{ copy.myTrainingJobs }}</span></template>
          <el-table :data="jobs" stripe :empty-text="copy.noTrainingJobs">
            <el-table-column prop="name" :label="copy.taskName" min-width="180" />
            <el-table-column :label="copy.model" width="150">
              <template #default="{ row }">{{ row.modelArchitecture || row.model || row.architecture || '--' }}</template>
            </el-table-column>
            <el-table-column :label="copy.status" width="110">
              <template #default="{ row }">
                <el-tag :type="statusTag(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column :label="copy.progress" width="170">
              <template #default="{ row }">
                <el-progress :percentage="jobProgress(row)" :stroke-width="6" />
              </template>
            </el-table-column>
            <el-table-column prop="currentLoss" label="Loss" width="90">
              <template #default="{ row }">{{ row.currentLoss?.toFixed(4) || row.loss?.toFixed?.(4) || '--' }}</template>
            </el-table-column>
            <el-table-column label="Accuracy" width="110">
              <template #default="{ row }">{{ formatAccuracy(row.currentAccuracy ?? row.accuracy) }}</template>
            </el-table-column>
            <el-table-column prop="createdAt" :label="copy.createdAt" width="160">
              <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
            </el-table-column>
          </el-table>
        </el-card>

        <el-card shadow="never" class="section-card">
          <template #header><span class="section-title">{{ copy.accountInfo }}</span></template>
          <div class="info-grid">
            <div class="info-item"><span class="info-key">{{ copy.userId }}</span><span class="info-val">{{ user.id ? '#' + user.id : '-' }}</span></div>
            <div class="info-item"><span class="info-key">{{ copy.username }}</span><span class="info-val">{{ user.username }}</span></div>
            <div class="info-item"><span class="info-key">{{ copy.role }}</span><span class="info-val">{{ roleLabel(user.role) }}</span></div>
            <div class="info-item"><span class="info-key">{{ copy.email }}</span><span class="info-val">{{ user.email || '-' }}</span></div>
            <div class="info-item"><span class="info-key">{{ copy.registeredAt }}</span><span class="info-val">{{ formatDate(user.createdAt) }}</span></div>
          </div>

          <div class="account-actions">
            <div>
              <strong>{{ copy.sessionTitle }}</strong>
              <span>{{ copy.sessionDesc }}</span>
            </div>
            <el-button type="danger" plain @click="handleLogout">{{ copy.logout }}</el-button>
          </div>
        </el-card>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { ElMessage } from 'element-plus';
import { authApi, trainingApi } from '@/api';
import { useAuthStore } from '@/stores/auth.store';

const authStore = useAuthStore();
const router = useRouter();
const { locale } = useI18n();
const user = ref<any>(null);
const jobs = ref<any[]>([]);
const stats = ref({ totalJobs: 0, completedJobs: 0, runningJobs: 0 });

const isZh = computed(() => locale.value.startsWith('zh'));
const copy = computed(() => (isZh.value
  ? {
      loginRequired: '请先登录',
      loginRequiredDesc: '登录后可以查看账号、训练任务和当前会话状态。',
      accountCenter: '个人工作台',
      emailUnset: '未设置邮箱',
      trainingJobs: '训练任务',
      completedJobs: '已完成',
      runningJobs: '运行中',
      myTrainingJobs: '我的训练任务',
      noTrainingJobs: '暂无训练任务',
      taskName: '任务名称',
      model: '模型',
      status: '状态',
      progress: '进度',
      createdAt: '创建时间',
      accountInfo: '账号信息',
      userId: '用户 ID',
      username: '用户名',
      role: '角色',
      email: '邮箱',
      registeredAt: '注册时间',
      sessionTitle: '登录状态',
      sessionDesc: '退出后会清除当前账号凭证，并返回登录页。',
      logout: '退出登录',
      logoutSuccess: '已退出登录',
      roles: { ADMIN: '管理员', RESEARCHER: '研究员', USER: '普通用户' },
      statusMap: { running: '运行中', completed: '已完成', failed: '失败', pending: '等待中' },
    }
  : {
      loginRequired: 'Please sign in',
      loginRequiredDesc: 'Sign in to view account, training jobs, and session state.',
      accountCenter: 'Profile Workspace',
      emailUnset: 'Email not set',
      trainingJobs: 'Training Jobs',
      completedJobs: 'Completed',
      runningJobs: 'Running',
      myTrainingJobs: 'My Training Jobs',
      noTrainingJobs: 'No training jobs',
      taskName: 'Task Name',
      model: 'Model',
      status: 'Status',
      progress: 'Progress',
      createdAt: 'Created At',
      accountInfo: 'Account Info',
      userId: 'User ID',
      username: 'Username',
      role: 'Role',
      email: 'Email',
      registeredAt: 'Registered At',
      sessionTitle: 'Session',
      sessionDesc: 'Logging out clears the current account session and returns to the login page.',
      logout: 'Log Out',
      logoutSuccess: 'Logged out',
      roles: { ADMIN: 'Admin', RESEARCHER: 'Researcher', USER: 'User' },
      statusMap: { running: 'Running', completed: 'Completed', failed: 'Failed', pending: 'Pending' },
    }));

const avatarLetter = computed(() => user.value?.username?.charAt(0)?.toUpperCase() || 'D');
const roleTag = (role: string) => role === 'ADMIN' ? 'danger' : role === 'RESEARCHER' ? 'warning' : 'info';
const statusTag = (status: string) => status === 'running' ? 'primary' : status === 'completed' ? 'success' : status === 'failed' ? 'danger' : 'info';
const roleLabel = (role: string) => copy.value.roles[role as keyof typeof copy.value.roles] || role || '-';
const statusLabel = (status: string) => copy.value.statusMap[status as keyof typeof copy.value.statusMap] || status || '-';
const jobProgress = (job: any) => {
  const total = Number(job.epochs || job.totalEpochs || 0);
  const current = Number(job.currentEpoch || job.epoch || 0);
  if (total > 0) return Math.max(0, Math.min(100, Math.round(current / total * 100)));
  return job.status === 'completed' ? 100 : 0;
};
const formatAccuracy = (value: number | null | undefined) => {
  if (value === null || value === undefined) return '--';
  return `${(Number(value) * 100).toFixed(1)}%`;
};
const formatDate = (value: string) => {
  if (!value) return '-';
  return new Date(value).toLocaleDateString(isZh.value ? 'zh-CN' : 'en-US', { year: 'numeric', month: '2-digit', day: '2-digit' });
};
const handleLogout = () => {
  authStore.logout();
  ElMessage.success(copy.value.logoutSuccess);
  router.push('/login');
};

onMounted(async () => {
  if (authStore.user) {
    user.value = { ...authStore.user };
  }
  try {
    const me = await authApi.me();
    if (me.data.code === 200) user.value = me.data.data;
  } catch {
    // authStore user is already used as the local fallback.
  }
  try {
    const response = await trainingApi.list();
    if (response.data.code === 200) {
      jobs.value = response.data.data || [];
      stats.value = {
        totalJobs: jobs.value.length,
        completedJobs: jobs.value.filter((job: any) => job.status === 'completed').length,
        runningJobs: jobs.value.filter((job: any) => job.status === 'running').length,
      };
    }
  } catch {
    // Keep the profile usable even if training records are unavailable.
  }
});
</script>

<style scoped>
.profile-page {
  min-height: calc(100dvh - var(--header-height, 72px));
  color: var(--text-primary);
}

.profile-container {
  width: min(980px, 100%);
  margin: 0 auto;
  padding: 32px 24px 48px;
  --profile-glass-bg:
    linear-gradient(135deg, rgba(255, 255, 255, 0.08), rgba(255, 255, 255, 0.018)),
    color-mix(in srgb, rgba(var(--glass-bg-rgb), var(--glass-opacity)) 70%, transparent);
  --profile-card-bg:
    linear-gradient(145deg, rgba(255, 255, 255, 0.052), rgba(255, 255, 255, 0.012)),
    color-mix(in srgb, rgba(var(--glass-bg-rgb), var(--glass-opacity)) 44%, transparent);
  --profile-border: color-mix(in srgb, var(--primary-color) 16%, var(--border-color));
}

.empty,
.profile-hero,
.section-card,
.stat-card {
  border: 1px solid var(--profile-border) !important;
  border-radius: 12px !important;
  background: var(--profile-glass-bg) !important;
  box-shadow: var(--shadow-soft);
  backdrop-filter: blur(calc(var(--glass-blur) * 0.78)) saturate(150%);
  -webkit-backdrop-filter: blur(calc(var(--glass-blur) * 0.78)) saturate(150%);
}

.empty {
  min-height: 220px;
  display: grid;
  place-items: center;
  gap: 8px;
  padding: 40px;
  text-align: center;
  color: var(--text-secondary);
}

.empty strong {
  color: var(--text-primary);
  font-size: 18px;
}

.profile-hero {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 18px;
  align-items: center;
  margin-bottom: 18px;
  padding: 22px;
}

.avatar-circle {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background:
    radial-gradient(circle at 32% 24%, rgba(255, 255, 255, 0.42), transparent 30%),
    linear-gradient(145deg, var(--primary-color), var(--accent-glow));
  color: var(--text-inverse);
  font-size: 32px;
  font-weight: var(--font-weight-title);
  display: grid;
  place-items: center;
  box-shadow: var(--shadow-ring);
}

.eyebrow {
  display: block;
  margin-bottom: 6px;
  color: var(--primary-color);
  font-size: 11px;
  font-weight: var(--font-weight-title);
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.profile-hero h1 {
  margin: 0;
  color: var(--text-primary);
  font-size: 26px;
  font-weight: var(--font-weight-title);
  line-height: 1.2;
}

.profile-hero .email {
  margin: 7px 0 0;
  color: var(--text-secondary);
  font-size: 13px;
}

.stats-row {
  margin-bottom: 18px;
}

.stat-card {
  text-align: center;
}

.stat-card :deep(.el-card__body) {
  padding: 18px 14px;
}

.stat-val {
  font-size: 30px;
  line-height: 1;
  font-weight: var(--font-weight-title);
}

.stat-val.primary { color: var(--primary-color); }
.stat-val.success { color: var(--warning-glow); }
.stat-val.accent { color: var(--accent-glow); }

.stat-label {
  margin-top: 7px;
  color: var(--text-secondary);
  font-size: 11px;
  text-transform: uppercase;
}

.section-card {
  margin-bottom: 18px;
  background: var(--profile-glass-bg) !important;
}

.section-card :deep(.el-card__header) {
  background: rgba(var(--glass-bg-rgb), 0.18);
  border-bottom: 1px solid var(--profile-border);
}

.section-card :deep(.el-card__body) {
  background: transparent;
}

.section-title {
  color: var(--text-primary);
  font-size: 14px;
  font-weight: var(--font-weight-title);
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.info-item {
  min-width: 0;
  display: flex;
  justify-content: space-between;
  gap: 14px;
  padding: 12px 14px;
  border: 1px solid var(--profile-border);
  border-radius: 10px;
  background: var(--profile-card-bg);
}

.info-key {
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 600;
}

.info-val {
  color: var(--text-primary);
  font-size: 13px;
  font-weight: var(--font-weight-body);
  text-align: right;
  word-break: break-word;
}

.account-actions {
  margin-top: 18px;
  padding: 14px 16px;
  border: 1px solid color-mix(in srgb, var(--danger-glow) 22%, var(--border-color));
  border-radius: 12px;
  background: var(--profile-card-bg);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.account-actions strong {
  display: block;
  color: var(--text-primary);
  font-size: 13px;
  font-weight: var(--font-weight-title);
}

.account-actions span {
  display: block;
  margin-top: 4px;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.55;
}

@media (hover: hover) and (pointer: fine) {
  .profile-hero,
  .section-card,
  .stat-card,
  .info-item {
    transition:
      border-color var(--motion-hover) var(--ease-liquid),
      box-shadow var(--motion-medium) var(--ease-smooth),
      transform var(--motion-hover) var(--ease-liquid);
  }

  .profile-hero:hover,
  .section-card:hover,
  .stat-card:hover {
    border-color: color-mix(in srgb, var(--primary-color) 30%, var(--border-color)) !important;
    box-shadow: var(--shadow-hover);
    transform: translate3d(0, -1px, 0);
  }

  .info-item:hover {
    border-color: color-mix(in srgb, var(--primary-color) 26%, var(--border-color));
  }
}

@media (max-width: 640px) {
  .profile-container {
    padding: 18px 16px 36px;
  }

  .profile-hero {
    grid-template-columns: 1fr;
    justify-items: start;
  }

  .info-grid {
    grid-template-columns: 1fr;
  }

  .account-actions {
    align-items: stretch;
    flex-direction: column;
  }
}

@media (prefers-reduced-motion: reduce) {
  .profile-hero,
  .section-card,
  .stat-card,
  .info-item {
    transition: none;
    transform: none !important;
  }
}
</style>
