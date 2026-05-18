<template>
  <aside class="sidebar-shell">
    <div class="sidebar-head">
      <span>{{ t('sidebar.workspace') }}</span>
      <strong>{{ currentSection }}</strong>
    </div>

    <nav class="sidebar-nav" aria-label="Workspace navigation">
      <template v-for="item in navItems" :key="item.path">
        <button
          type="button"
          :class="{ active: isNavActive(item) }"
          @click="handleNavClick(item)"
        >
          <component :is="item.icon" :size="17" stroke-width="2.25" />
          <span>{{ navLabel(item.path) }}</span>
        </button>
      </template>
    </nav>

    <div class="sidebar-section">
      <span class="section-label">{{ t('sidebar.exploreCenter') }}</span>
      <button
        v-for="item in exploreItems"
        :key="item.path"
        type="button"
        class="mini-row"
        :class="{ active: isExploreNavActive(route.path, item) }"
        @click="router.push(item.path)"
      >
        <component :is="item.icon" :size="16" stroke-width="2.2" />
        <span>{{ navLabel(item.path) }}</span>
      </button>
    </div>

    <div class="status-panel">
      <div class="status-top">
        <span>{{ t('sidebar.environment') }}</span>
        <em>{{ t('sidebar.active') }}</em>
      </div>
      <div class="meter">
        <div style="width: 72%"></div>
      </div>
      <p>{{ t('dashboard.gpuVram') }} 11.4 / 16 GB</p>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  exploreNavItems,
  isExploreNavActive,
  isMainNavActive,
  mainNavItems,
  navLabelKey,
  type NavItem,
} from '@/constants/navigation'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const navItems = mainNavItems
const exploreItems = exploreNavItems

const currentSection = computed(() => {
  const direct = [...navItems, ...exploreItems].find((item) => item.path === route.path)
  if (route.path.startsWith('/viz')) return t('nav.analysis')
  return direct ? navLabel(direct.path) : 'DeepInsight'
})

const navLabel = (path: string) => t(navLabelKey(path))

function isNavActive(item: NavItem) {
  return isMainNavActive(route.path, item)
}

function handleNavClick(item: NavItem) {
  router.push(item.path)
}
</script>

<style scoped>
.sidebar-shell {
  width: 240px;
  position: fixed;
  top: 86px;
  bottom: 14px;
  left: 14px;
  z-index: 90;
  padding: 18px 12px;
  overflow-y: auto;
  overscroll-behavior: contain;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  background: var(--nav-bg);
  backdrop-filter: blur(24px) saturate(180%);
  -webkit-backdrop-filter: blur(24px) saturate(180%);
  box-shadow: var(--shadow-soft);
  scrollbar-width: thin;
}

.sidebar-shell::before {
  content: "";
  position: absolute;
  inset: 0;
  pointer-events: none;
  border-radius: inherit;
  background:
    linear-gradient(180deg, rgba(255,255,255,0.16), transparent 34%),
    radial-gradient(circle at 18% 0%, rgba(var(--primary-rgb),0.12), transparent 30%);
  opacity: 0.72;
}

.sidebar-shell > * {
  position: relative;
  z-index: 1;
}

.sidebar-head {
  padding: 12px 12px 16px;
  border-bottom: 1px solid var(--border-color);
  margin-bottom: 14px;
}

.sidebar-head span,
.section-label,
.status-top span {
  display: block;
  font-size: 10px;
  font-weight: 900;
  text-transform: uppercase;
  letter-spacing: 0;
  color: var(--text-muted);
}

.sidebar-head strong {
  display: block;
  margin-top: 6px;
  font-size: 20px;
  color: var(--text-primary);
}

.sidebar-nav,
.sidebar-section {
  display: grid;
  gap: 6px;
}

.sidebar-nav button,
.mini-row {
  width: 100%;
  height: 42px;
  border: 1px solid transparent;
  border-radius: 9px;
  display: grid;
  grid-template-columns: 22px 1fr auto;
  align-items: center;
  gap: 9px;
  padding: 0 10px;
  background: transparent;
  color: var(--text-secondary);
  font: inherit;
  font-size: 13px;
  font-weight: 800;
  text-align: left;
  cursor: pointer;
  transition: background 180ms ease, color 180ms ease, border-color 180ms ease, transform 180ms ease;
}

.sidebar-nav button:hover,
.mini-row:hover,
.sidebar-nav button.active,
.mini-row.active {
  color: var(--text-primary);
  border-color: rgba(var(--primary-rgb), 0.2);
  background: rgba(var(--primary-rgb), 0.09);
}

.sidebar-nav button.active,
.mini-row.active {
  color: var(--primary-color);
}

.chevron {
  transition: transform 180ms ease;
}

.chevron.open {
  transform: rotate(90deg);
}

.sidebar-section {
  margin-top: 22px;
}

.section-label {
  padding: 0 12px 6px;
}

.status-panel {
  margin-top: 28px;
  padding: 14px;
  border: 1px solid var(--border-color);
  border-radius: 10px;
  background: var(--surface-2);
}

.status-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.status-top em {
  font-style: normal;
  font-size: 10px;
  font-weight: 900;
  color: var(--primary-color);
}

.meter {
  height: 6px;
  border-radius: 999px;
  background: var(--surface-3);
  overflow: hidden;
}

.meter div {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, var(--primary-color), var(--accent-glow));
}

.status-panel p {
  margin: 10px 0 0;
  color: var(--text-muted);
  font-size: 11px;
  font-weight: 800;
}
</style>
