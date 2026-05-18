<template>
  <div class="admin-root" :class="{ dark: isDark }" :style="{ backgroundColor: 'var(--bg-color)' }">
    <aside class="admin-sidebar" :class="{ dark: isDark }">
      <div class="sidebar-head">
        <div class="admin-badge">{{ t('admin.badge') }}</div>
        <h3>{{ t('admin.title') }}</h3>
        <p>{{ t('admin.subtitle') }}</p>
      </div>

      <nav class="sidebar-nav">
        <div v-for="item in navItems" :key="item.key"
          class="nav-item" :class="{ active: route.path.startsWith(item.path) }"
          @click="router.push(item.path)">
          <el-icon :size="16"><component :is="item.icon" /></el-icon>
          <span>{{ t(item.label) }}</span>
        </div>
      </nav>

      <div class="sidebar-foot">
        <div class="nav-item exit" @click="router.push('/dashboard')">
          <el-icon :size="16"><Back /></el-icon>
          <span>{{ t('admin.back') }}</span>
        </div>
      </div>
    </aside>

    <main class="admin-main">
      <div class="admin-topbar">
        <div class="topbar-info">
          <span class="service-chip"><span class="dot online"></span> MySQL</span>
          <span class="service-chip"><span class="dot online"></span> Redis</span>
          <span v-for="(v,k) in stats" :key="k" class="stat-chip">{{ k }}:{{ v }}</span>
        </div>
        <div class="topbar-right">
          <span class="user-tag">{{ username }} · ADMIN</span>
          <el-button size="small" type="danger" text @click="logout">{{ t('admin.logout') }}</el-button>
        </div>
      </div>
      <div class="admin-content">
        <router-view />
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { useThemeStore } from '@/stores/theme.store';
import { authApi, adminApi } from '@/api';
import { ROUTES } from '@/constants';
import { clearAuthStorage } from '@/utils/authState';
import { User, Cpu, Collection, ChatLineSquare, ChatDotRound, DataAnalysis, Back } from '@element-plus/icons-vue';

const route = useRoute(); const router = useRouter();
const { t } = useI18n();
const theme = useThemeStore();
const isDark = computed(() => theme.isDarkMode);
const stats = ref<Record<string,number>>({});
const username = ref('');

const navItems = [
  { key:'users',  label:'admin.users', path:'/admin/users',  icon:'User' },
  { key:'models', label:'admin.models', path:'/admin/models', icon:'Cpu' },
  { key:'ai',     label:'admin.ai',  path:'/admin/ai',    icon:'ChatDotRound' },
  { key:'kb',     label:'admin.knowledge',   path:'/admin/kb',     icon:'Collection' },
  { key:'forum',  label:'admin.forum', path:'/admin/forum',  icon:'ChatLineSquare' },
  { key:'data',   label:'admin.dataTraining', path:'/admin/data',  icon:'DataAnalysis' },
];

const logout = () => {
  clearAuthStorage()
  router.push(ROUTES.LOGIN)
};

onMounted(async () => {
  try {
    const me = await authApi.me();
    if (me.data.code === 200) username.value = me.data.data.username;
    const s = await adminApi.status();
    if (s.data.code === 200) stats.value = s.data.data;
  } catch { /* API unavailable */ }
});
</script>

<style scoped>
.admin-root {
  --admin-table-bg: color-mix(in srgb, var(--surface-1) 86%, transparent);
  --admin-table-head-bg: color-mix(in srgb, var(--surface-2) 72%, var(--bg-color));
  --admin-table-row-bg: color-mix(in srgb, var(--surface-1) 78%, transparent);
  --admin-table-row-alt-bg: color-mix(in srgb, var(--primary-color) 4%, var(--surface-1));
  --admin-table-row-hover-bg: color-mix(in srgb, var(--primary-color) 9%, var(--surface-1));
  display: flex;
  min-height: 100vh;
  color: var(--text-primary);
  background:
    linear-gradient(180deg, rgba(var(--primary-rgb), 0.055), transparent 260px),
    linear-gradient(90deg, rgba(var(--primary-rgb), 0.03) 1px, transparent 1px),
    linear-gradient(180deg, rgba(var(--primary-rgb), 0.03) 1px, transparent 1px),
    var(--bg-color);
  background-size: auto, 42px 42px, 42px 42px, auto;
}

.admin-sidebar {
  position: sticky;
  top: 0;
  width: 248px;
  height: 100vh;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  padding: 14px;
  z-index: 100;
  border-right: 1px solid var(--border-color);
  background: rgba(var(--glass-bg-rgb), 0.78);
  color: var(--text-primary);
  backdrop-filter: blur(24px) saturate(180%);
  -webkit-backdrop-filter: blur(24px) saturate(180%);
}

.sidebar-head {
  padding: 16px 12px 18px;
  border-bottom: 1px solid var(--border-color);
}

.admin-badge {
  display: inline-flex;
  align-items: center;
  height: 22px;
  margin-bottom: 10px;
  padding: 0 9px;
  border: 1px solid rgba(var(--primary-rgb), 0.28);
  border-radius: var(--radius-sm);
  background: rgba(var(--primary-rgb), 0.1);
  color: var(--primary-color);
  font-size: 9px;
  font-weight: 900;
}

.sidebar-head h3 {
  margin: 0;
  color: var(--text-primary);
  font-size: 18px;
  font-weight: 900;
}

.sidebar-head p {
  margin: 6px 0 0;
  color: var(--text-secondary);
  font-size: 11px;
  line-height: 1.6;
}

.sidebar-nav {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px 0;
}

.nav-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 42px;
  padding: 10px 12px;
  border: 1px solid transparent;
  border-radius: var(--radius-lg);
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 13px;
  font-weight: 850;
  transition: background-color 180ms ease, border-color 180ms ease, color 180ms ease, transform 180ms ease;
}

.nav-item:hover {
  transform: translateX(2px);
  border-color: var(--border-color);
  background: var(--surface-2);
  color: var(--text-primary);
}

.nav-item.active {
  border-color: rgba(var(--primary-rgb), 0.35);
  background: rgba(var(--primary-rgb), 0.11);
  color: var(--primary-color);
  box-shadow: inset 3px 0 0 var(--primary-color);
}

.sidebar-foot {
  padding-top: 12px;
  border-top: 1px solid var(--border-color);
}

.nav-item.exit:hover {
  color: var(--tone-rose);
}

.admin-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.admin-topbar {
  position: sticky;
  top: 0;
  z-index: 50;
  min-height: 58px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 10px 24px;
  border-bottom: 1px solid var(--border-color);
  background: rgba(var(--glass-bg-rgb), 0.76);
  color: var(--text-secondary);
  backdrop-filter: blur(22px) saturate(170%);
  -webkit-backdrop-filter: blur(22px) saturate(170%);
  font-size: 12px;
}

.topbar-info,
.topbar-right {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.service-chip,
.stat-chip,
.user-tag {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  min-height: 28px;
  padding: 0 10px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  background: var(--surface-2);
  color: var(--text-secondary);
  font-weight: 800;
}

.user-tag {
  color: var(--text-primary);
}

.dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
}

.dot.online {
  background: var(--tone-green);
  box-shadow: 0 0 0 4px rgba(34, 197, 94, 0.12);
}

.admin-content {
  flex: 1;
  width: min(100%, 1440px);
  margin: 0 auto;
  padding: var(--page-padding);
}

:deep(.page-title) {
  position: relative;
  margin: 0 0 18px !important;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--border-color);
  color: var(--text-primary) !important;
  font-size: 28px !important;
  font-weight: 900 !important;
}

:deep(.page-title::after) {
  content: "";
  position: absolute;
  left: 0;
  bottom: -1px;
  width: 180px;
  height: 1px;
  background: linear-gradient(90deg, var(--primary-color), transparent);
}

:deep(.sub) {
  display: inline-flex;
  align-items: center;
  margin: 18px 0 10px !important;
  color: var(--text-primary) !important;
  font-size: 13px !important;
  font-weight: 900 !important;
}

:deep(.el-table) {
  --el-table-bg-color: var(--admin-table-bg);
  --el-table-tr-bg-color: var(--admin-table-row-bg);
  --el-table-header-bg-color: var(--admin-table-head-bg);
  --el-table-row-hover-bg-color: var(--admin-table-row-hover-bg);
  --el-table-border-color: var(--border-color);
  --el-table-text-color: var(--text-secondary);
  --el-table-header-text-color: var(--text-primary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  background: var(--admin-table-bg);
  box-shadow: var(--shadow-soft);
  overflow: hidden;
}

:deep(.el-table__inner-wrapper::before),
:deep(.el-table__border-left-patch) {
  background-color: var(--border-color);
}

:deep(.el-table th.el-table__cell) {
  background: var(--admin-table-head-bg) !important;
  border-bottom-color: var(--border-color);
}

:deep(.el-table tr),
:deep(.el-table td.el-table__cell) {
  background: var(--admin-table-row-bg) !important;
  border-bottom-color: color-mix(in srgb, var(--border-color) 70%, transparent);
}

:deep(.el-table--striped .el-table__body tr.el-table__row--striped td.el-table__cell) {
  background: var(--admin-table-row-alt-bg) !important;
}

:deep(.el-table__body tr:hover > td.el-table__cell),
:deep(.el-table--enable-row-hover .el-table__body tr:hover > td.el-table__cell) {
  background: var(--admin-table-row-hover-bg) !important;
}

:deep(.el-table__fixed),
:deep(.el-table__fixed-right),
:deep(.el-table__fixed-header-wrapper),
:deep(.el-table__fixed-body-wrapper) {
  background: var(--admin-table-bg);
}

@media (max-width: 900px) {
  .admin-root {
    display: block;
  }

  .admin-sidebar {
    position: relative;
    width: 100%;
    height: auto;
  }

  .sidebar-nav {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .admin-topbar {
    position: relative;
    align-items: flex-start;
    flex-direction: column;
  }

  :deep(.page-title) {
    font-size: 24px !important;
  }
}
</style>
