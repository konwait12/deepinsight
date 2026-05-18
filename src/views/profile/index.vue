<template>
  <div class="profile-page">
    <div class="profile-container entrance-up">
      <div v-if="!user" class="empty">请先登录</div>
      <template v-else>
        <!-- Avatar + Name -->
        <div class="profile-hero">
          <div class="avatar-circle">{{ user.username?.charAt(0).toUpperCase() }}</div>
          <h1>{{ user.username }}</h1>
          <el-tag :type="roleTag(user.role)" size="small">{{ user.role }}</el-tag>
          <p class="email">{{ user.email || '未设置邮箱' }}</p>
        </div>

        <!-- Stats -->
        <el-row :gutter="16" class="stats-row">
          <el-col :span="8">
            <el-card shadow="hover" class="stat-card">
              <div class="stat-val" style="color: var(--primary-color)">{{ stats.totalJobs }}</div>
              <div class="stat-label">训练任务</div>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card shadow="hover" class="stat-card">
              <div class="stat-val" style="color: #4ade80">{{ stats.completedJobs }}</div>
              <div class="stat-label">已完成</div>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card shadow="hover" class="stat-card">
              <div class="stat-val" style="color: #4dc9f0">{{ stats.runningJobs }}</div>
              <div class="stat-label">运行中</div>
            </el-card>
          </el-col>
        </el-row>

        <!-- Training Jobs -->
        <el-card shadow="never" class="section-card">
          <template #header><span class="section-title">我的训练任务</span></template>
          <el-table :data="jobs" stripe empty-text="暂无训练任务">
            <el-table-column prop="name" label="任务名称" min-width="180"/>
            <el-table-column prop="modelArchitecture" label="模型" width="140"/>
            <el-table-column label="状态" width="100">
              <template #default="{row}">
                <el-tag :type="row.status==='running'?'primary':row.status==='completed'?'success':'info'" size="small">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="进度" width="160">
              <template #default="{row}">
                <el-progress :percentage="row.epochs>0?Math.round(row.currentEpoch/row.epochs*100):0" :stroke-width="6"/>
              </template>
            </el-table-column>
            <el-table-column prop="currentLoss" label="Loss" width="80">
              <template #default="{row}">{{ row.currentLoss?.toFixed(4) || '--' }}</template>
            </el-table-column>
            <el-table-column label="Accuracy" width="100">
              <template #default="{row}">{{ row.currentAccuracy ? (row.currentAccuracy*100).toFixed(1)+'%' : '--' }}</template>
            </el-table-column>
            <el-table-column prop="createdAt" label="创建时间" width="160"/>
          </el-table>
        </el-card>

        <!-- Account Info -->
        <el-card shadow="never" class="section-card">
          <template #header><span class="section-title">账户信息</span></template>
          <div class="info-grid">
            <div class="info-item"><span class="info-key">用户ID</span><span class="info-val">{{ user.id ? '#' + user.id : '-' }}</span></div>
            <div class="info-item"><span class="info-key">用户名</span><span class="info-val">{{ user.username }}</span></div>
            <div class="info-item"><span class="info-key">角色</span><span class="info-val">{{ user.role }}</span></div>
            <div class="info-item"><span class="info-key">邮箱</span><span class="info-val">{{ user.email || '-' }}</span></div>
            <div class="info-item"><span class="info-key">注册时间</span><span class="info-val">{{ formatDate(user.createdAt) }}</span></div>
          </div>
          <div class="account-actions">
            <div>
              <strong>{{ t('profile.sessionTitle') }}</strong>
              <span>{{ t('profile.sessionDesc') }}</span>
            </div>
            <el-button type="danger" plain @click="handleLogout">{{ t('profile.logout') }}</el-button>
          </div>
        </el-card>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { ElMessage } from 'element-plus';
import { authApi, trainingApi } from '@/api';
import { useAuthStore } from '@/stores/auth.store';

const authStore = useAuthStore();
const router = useRouter();
const { t } = useI18n();
const user = ref<any>(null);
const jobs = ref<any[]>([]);
const stats = ref({ totalJobs: 0, completedJobs: 0, runningJobs: 0 });

const roleTag = (r: string) => r === 'ADMIN' ? 'danger' : r === 'RESEARCHER' ? 'warning' : 'info';
const formatDate = (t: string) => t ? new Date(t).toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' }) : '-';
const handleLogout = () => {
  authStore.logout();
  ElMessage.success(t('profile.logoutSuccess'));
  router.push('/login');
};

onMounted(async () => {
  // Start with authStore user data as fallback
  if (authStore.user) {
    user.value = { ...authStore.user };
  }
  try {
    const me = await authApi.me();
    if (me.data.code === 200) user.value = me.data.data;
  } catch {
    // /auth/me unavailable — authStore data already set as fallback
  }
  try {
    const t = await trainingApi.list();
    if (t.data.code === 200) {
      jobs.value = t.data.data || [];
      stats.value = {
        totalJobs: jobs.value.length,
        completedJobs: jobs.value.filter((j: any) => j.status === 'completed').length,
        runningJobs: jobs.value.filter((j: any) => j.status === 'running').length,
      };
    }
  } catch {}
});
</script>

<style scoped>
.profile-page { min-height: 100vh; }
.profile-container { max-width: 800px; margin: 0 auto; padding: 48px 24px; }
.empty { text-align: center; padding: 80px; color: var(--text-secondary); font-size: 16px; }

.profile-hero { text-align: center; margin-bottom: 40px; }
.avatar-circle {
  width: 72px; height: 72px; border-radius: 50%; background: var(--primary-color);
  color: #fff; font-size: 32px; font-weight: 900; display: flex;
  align-items: center; justify-content: center; margin: 0 auto 16px;
}
.profile-hero h1 { font-size: 24px; font-weight: 900; color: var(--text-primary); margin: 0 0 8px; }
.profile-hero .email { font-size: 13px; color: var(--text-secondary); margin: 8px 0 0; }

.stats-row { margin-bottom: 24px; }
.stat-card { text-align: center; border-radius: 16px; }
.stat-val { font-size: 28px; font-weight: 900; }
.stat-label { font-size: 11px; color: var(--text-secondary); margin-top: 4px; text-transform: uppercase; }

.section-card { border-radius: 16px; margin-bottom: 24px; }
.section-title { font-size: 14px; font-weight: 800; color: var(--text-primary); }

.info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.info-item { padding: 10px 16px; background: var(--bg-input); border-radius: 10px; display: flex; justify-content: space-between; }
.info-key { font-size: 12px; color: var(--text-secondary); font-weight: 600; }
.info-val { font-size: 13px; color: var(--text-primary); font-weight: 700; }
.account-actions {
  margin-top: 18px;
  padding: 14px 16px;
  border: 1px solid color-mix(in srgb, var(--primary-color) 14%, var(--border-color));
  border-radius: 12px;
  background: color-mix(in srgb, var(--bg-input) 88%, transparent);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}
.account-actions strong {
  display: block;
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 900;
}
.account-actions span {
  display: block;
  margin-top: 4px;
  color: var(--text-secondary);
  font-size: 12px;
}
@media (max-width: 640px) {
  .info-grid { grid-template-columns: 1fr; }
  .account-actions { align-items: stretch; flex-direction: column; }
}
</style>
