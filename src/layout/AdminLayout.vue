<template>
  <div class="admin-root" :class="{ dark: isDark }" :style="{ backgroundColor: 'var(--bg-color)' }">
    <aside class="admin-sidebar" :class="{ dark: isDark }">
      <div class="sidebar-head">
        <div class="admin-badge">{{ t('admin.badge') }}</div>
        <h3>{{ t('admin.title') }}</h3>
        <p>{{ t('admin.subtitle') }}</p>
      </div>

      <nav class="sidebar-nav">
        <button v-for="item in navItems" :key="item.key"
          type="button"
          class="nav-item" :class="{ active: route.path.startsWith(item.path) }"
          @click="router.push(item.path)">
          <el-icon :size="16"><component :is="item.icon" /></el-icon>
          <span>{{ navLabel(item) }}</span>
        </button>
      </nav>

      <div class="sidebar-foot">
        <button type="button" class="nav-item exit" @click="router.push(ROUTES.TRAINING)">
          <el-icon :size="16"><Back /></el-icon>
          <span>{{ t('admin.back') }}</span>
        </button>
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
        <router-view v-slot="{ Component }">
          <Transition name="admin-page" mode="out-in" appear>
            <component :is="Component" :key="route.fullPath" class="admin-view-shell" />
          </Transition>
        </router-view>
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
import { User, Collection, ChatLineSquare, ChatDotRound, DataAnalysis, Back, Monitor } from '@element-plus/icons-vue';

const route = useRoute(); const router = useRouter();
const { t } = useI18n();
const theme = useThemeStore();
const isDark = computed(() => theme.isDarkMode);
const stats = ref<Record<string,number>>({});
const username = ref('');

const navItems = [
  { key:'overview', label:'overview', path:'/admin/overview', icon:'Monitor' },
  { key:'users',  label:'admin.users', path:'/admin/users',  icon:'User' },
  { key:'ai',     label:'admin.ai',  path:'/admin/ai',    icon:'ChatDotRound' },
  { key:'kb',     label:'admin.knowledge',   path:'/admin/kb',     icon:'Collection' },
  { key:'forum',  label:'admin.forum', path:'/admin/forum',  icon:'ChatLineSquare' },
  { key:'data',   label:'admin.dataTraining', path:'/admin/data',  icon:'DataAnalysis' },
];

const navLabel = (item: { key: string; label: string }) => {
  if (item.key === 'overview') return theme.lang === 'zh' ? '总览' : 'Overview'
  return t(item.label)
};

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
  font-weight: var(--font-weight-title);
}

.sidebar-head h3 {
  margin: 0;
  color: var(--text-primary);
  font-size: 18px;
  font-weight: var(--font-weight-title);
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
  width: 100%;
  min-width: 0;
  min-height: 42px;
  padding: 10px 12px;
  border: 1px solid transparent;
  border-radius: var(--radius-lg);
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 13px;
  font-weight: var(--font-weight-label);
  font-family: inherit;
  text-align: left;
  transition: background-color 180ms ease, border-color 180ms ease, color 180ms ease, transform 180ms ease;
}

.nav-item span {
  min-width: 0;
  line-height: 1.35;
  overflow-wrap: anywhere;
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
  min-width: 0;
  max-width: 100%;
  min-height: 28px;
  padding: 0 10px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  background: var(--surface-2);
  color: var(--text-secondary);
  font-weight: var(--font-weight-body);
  line-height: 1.35;
  overflow-wrap: anywhere;
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
  padding: clamp(22px, 3vw, 34px);
  perspective: 1400px;
}

.admin-view-shell {
  min-width: 0;
  transform-origin: center 24px;
}

.admin-page-enter-active,
.admin-page-leave-active {
  transition:
    opacity 300ms cubic-bezier(0.16, 1, 0.3, 1),
    transform 360ms cubic-bezier(0.16, 1, 0.3, 1),
    filter 300ms ease;
  will-change: opacity, transform, filter;
}

.admin-page-enter-from {
  opacity: 0;
  filter: blur(9px) saturate(0.94);
  transform: translate3d(0, 14px, 0) scale(0.99);
}

.admin-page-leave-to {
  opacity: 0;
  filter: blur(6px) saturate(0.96);
  transform: translate3d(0, -7px, 0) scale(0.995);
}

@media (prefers-reduced-motion: reduce) {
  .admin-page-enter-active,
  .admin-page-leave-active {
    transition: none;
  }
}

:deep(.page-title) {
  position: relative;
  margin: 0 !important;
  padding-bottom: 0;
  border-bottom: 0;
  color: var(--text-primary) !important;
  font-size: clamp(24px, 2.7vw, 34px) !important;
  font-weight: var(--font-weight-title) !important;
}

:deep(.page-title::after) {
  display: none;
}

:deep(.sub) {
  display: inline-flex;
  align-items: center;
  margin: 18px 0 10px !important;
  color: var(--text-primary) !important;
  font-size: 13px !important;
  font-weight: var(--font-weight-title) !important;
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
  overflow: auto;
}

:deep(.admin-page) {
  display: grid;
  gap: 18px;
}

:deep(.admin-hero),
:deep(.admin-panel) {
  border: 1px solid var(--border-color);
  border-radius: 18px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.055), transparent 62%),
    rgba(var(--glass-bg-rgb), 0.58);
  box-shadow: var(--shadow-soft);
  backdrop-filter: blur(18px) saturate(130%);
  -webkit-backdrop-filter: blur(18px) saturate(130%);
}

:deep(.admin-hero) {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
  padding: 22px;
}

:deep(.admin-hero span),
:deep(.admin-panel-head span),
:deep(.admin-metric-grid article span) {
  display: block;
  color: var(--text-muted);
  font-size: 11px;
  font-weight: var(--font-weight-title);
}

:deep(.admin-hero p) {
  max-width: 68ch;
  margin: 10px 0 0;
  color: var(--text-secondary);
  line-height: 1.7;
}

:deep(.hero-actions),
:deep(.admin-toolbar) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 10px;
}

:deep(.admin-toolbar) {
  padding: 12px;
  border: 1px solid var(--border-color);
  border-radius: 16px;
  background: rgba(var(--glass-bg-rgb), 0.42);
}

:deep(.toolbar-left),
:deep(.toolbar-right) {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

:deep(.admin-metric-grid) {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

:deep(.admin-metric-grid article) {
  min-width: 0;
  padding: 16px;
  border: 1px solid var(--border-color);
  border-radius: 16px;
  background: rgba(var(--glass-bg-rgb), 0.5);
  box-shadow: var(--shadow-soft);
}

:deep(.admin-metric-grid article strong) {
  display: block;
  margin-top: 8px;
  color: var(--text-primary);
  font-size: 26px;
}

:deep(.admin-metric-grid article small) {
  display: block;
  margin-top: 6px;
  color: var(--text-secondary);
}

:deep(.admin-grid) {
  display: grid;
  gap: 16px;
}

:deep(.admin-grid.two) {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

:deep(.admin-panel) {
  min-width: 0;
  padding: 16px;
}

:deep(.admin-panel-head) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

:deep(.admin-panel-head strong) {
  display: block;
  margin-top: 4px;
  color: var(--text-primary);
  font-size: 16px;
}

:deep(.status-list),
:deep(.shortcut-list) {
  display: grid;
  gap: 8px;
}

:deep(.status-list div),
:deep(.shortcut-list button) {
  width: 100%;
  min-width: 0;
  padding: 12px;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  background: var(--surface-2);
}

:deep(.status-list div) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

:deep(.status-list span),
:deep(.shortcut-list small) {
  color: var(--text-secondary);
}

:deep(.shortcut-list button) {
  border: 1px solid var(--border-color);
  color: var(--text-primary);
  font: inherit;
  text-align: left;
  cursor: pointer;
  transition: background 180ms ease, border-color 180ms ease, transform 180ms ease;
}

:deep(.shortcut-list button:hover) {
  transform: translateY(-1px);
  border-color: rgba(var(--primary-rgb), 0.26);
  background: rgba(var(--primary-rgb), 0.08);
}

:deep(.shortcut-list span) {
  display: block;
  font-weight: var(--font-weight-title);
}

:deep(.empty-hint) {
  padding: 28px;
  border: 1px dashed var(--border-color);
  border-radius: 16px;
  color: var(--text-secondary);
  text-align: center;
  background: rgba(var(--glass-bg-rgb), 0.34);
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

  .nav-item {
    border-radius: 14px;
  }

  .admin-topbar {
    position: relative;
    align-items: flex-start;
    flex-direction: column;
  }

  :deep(.page-title) {
    font-size: 24px !important;
  }

  :deep(.admin-hero) {
    align-items: stretch;
    flex-direction: column;
  }

  :deep(.admin-metric-grid),
  :deep(.admin-grid.two) {
    grid-template-columns: 1fr;
  }
}
</style>

