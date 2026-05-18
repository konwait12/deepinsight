<template>
  <header class="topbar" :class="{ landing: isLanding && !isScrolled }">
    <button class="brand" type="button" @click="goHome">
      <DeepLogo :scale="0.9" />
      <span class="brand-status">{{ t('header.brandStatus') }}</span>
    </button>

    <nav v-if="showTopNav" class="top-nav" aria-label="Primary navigation">
      <div
        v-for="item in navItems"
        :key="item.path"
        class="nav-item-wrap"
      >
        <button
          type="button"
          :class="{ active: isActive(item) }"
          @click="navigateTo(item.path)"
        >
          <component :is="item.icon" :size="15" stroke-width="2.3" />
          <span>{{ navLabel(item.path) }}</span>
        </button>
      </div>

      <div class="nav-item-wrap has-children explore-wrap">
        <button
          type="button"
          :class="{ active: isExploreActive }"
          @click="navigateExplore"
        >
          <Compass :size="15" stroke-width="2.3" />
          <span>{{ t('nav.exploreCenter') }}</span>
          <ChevronDown class="nav-chevron" :size="13" stroke-width="2.5" />
        </button>

        <div class="top-nav-submenu">
          <button
            v-for="item in exploreItems"
            :key="item.path"
            type="button"
            :class="{ active: isExploreItemActive(item) }"
            @click="navigateTo(item.path)"
          >
            <component :is="item.icon" :size="15" stroke-width="2.2" />
            <span>{{ navLabel(item.path) }}</span>
          </button>
        </div>
      </div>
    </nav>

    <div class="top-actions">
      <div class="palette-switcher" aria-label="Palette">
        <button
          v-for="palette in themeStore.palettes"
          :key="palette.id"
          type="button"
          :class="{ active: themeStore.paletteId === palette.id }"
          :title="palette.name"
          :style="{ '--swatch': palette.color }"
          @click="themeStore.setPalette(palette.id)"
        ></button>
      </div>

      <button v-if="route.path !== '/'" class="icon-btn" type="button" aria-label="Toggle navigation" @click="themeStore.toggleMenuMode">
        <PanelLeftClose v-if="themeStore.isHorizontalMenu" :size="17" stroke-width="2.4" />
        <PanelTop v-else :size="17" stroke-width="2.4" />
      </button>

      <button class="lang-btn" type="button" :title="t('header.lang')" @click="handleLangToggle">{{ langLabel }}</button>

      <button class="avatar-btn" type="button" :title="accountLabel" @click="handleAccount">
        <UserRound :size="17" stroke-width="2.4" />
        <span>{{ accountLabel }}</span>
      </button>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useThemeStore } from '@/stores/theme.store'
import { useAuthStore } from '@/stores/auth.store'
import { APP_EVENTS } from '@/constants'
import DeepLogo from '@/components/common/DeepLogo.vue'
import {
  exploreNavItems,
  isExploreNavActive,
  isMainNavActive,
  mainNavItems,
  navLabelKey,
  type NavItem,
} from '@/constants/navigation'
import {
  ChevronDown,
  Compass,
  PanelLeftClose,
  PanelTop,
  UserRound,
} from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()
const themeStore = useThemeStore()
const authStore = useAuthStore()
const { t } = useI18n()
const isScrolled = ref(false)
const isLanding = computed(() => route.name === 'Landing')
const showTopNav = computed(() => isLanding.value || themeStore.isHorizontalMenu)
const navItems = mainNavItems
const exploreItems = exploreNavItems
const langLabel = computed(() => themeStore.lang === 'zh' ? 'EN' : '中')
const accountLabel = computed(() => {
  if (!authStore.isAuthenticated) return t('header.login')
  return authStore.user?.username || t('header.profile')
})

const handleScroll = () => {
  isScrolled.value = window.scrollY > 18
}

const isActive = (item: NavItem) => isMainNavActive(route.path, item)
const isExploreActive = computed(() => isExploreNavActive(route.path))
const isExploreItemActive = (item: NavItem) => isExploreNavActive(route.path, item)

const goHome = () => router.push('/')
const navigateTo = (path: string) => router.push(path)
const navigateExplore = () => router.push(exploreItems[0]?.path || '/knowledge')
const handleAccount = () => router.push(authStore.isAuthenticated ? '/profile' : '/login')
const navLabel = (path: string) => t(navLabelKey(path))
const handleLangToggle = (event: MouseEvent) => window.dispatchEvent(new CustomEvent(APP_EVENTS.TOGGLE_LANG, {
  detail: { x: event.clientX, y: event.clientY },
}))

onMounted(() => window.addEventListener('scroll', handleScroll, { passive: true }))
onUnmounted(() => window.removeEventListener('scroll', handleScroll))
</script>

<style scoped>
.topbar {
  position: fixed;
  top: 14px;
  left: 14px;
  right: 14px;
  z-index: 100;
  height: 58px;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 14px;
  align-items: center;
  padding: 0 10px 0 14px;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  background: var(--nav-bg);
  backdrop-filter: blur(24px) saturate(180%);
  box-shadow: var(--shadow-soft);
  transition: background 240ms ease, border-color 240ms ease, transform 240ms ease;
}

.topbar.landing {
  background: rgba(5, 7, 10, 0.34);
  border-color: rgba(255, 255, 255, 0.12);
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  border: 0;
  padding: 0;
  background: transparent;
  color: inherit;
  cursor: pointer;
}

.brand-status {
  padding-left: 12px;
  border-left: 1px solid var(--border-color);
  color: var(--text-muted);
  font-size: 11px;
  font-weight: 900;
  text-transform: uppercase;
  letter-spacing: 0;
  white-space: nowrap;
}

.brand:hover .brand-status {
  color: var(--primary-color);
}

.top-nav {
  justify-self: center;
  display: flex;
  align-items: center;
  gap: 4px;
  min-width: 0;
  padding: 4px;
  border: 1px solid var(--border-color);
  border-radius: 10px;
  background: var(--surface-3);
}

.nav-item-wrap {
  position: relative;
}

.top-nav button,
.icon-btn,
.lang-btn,
.avatar-btn,
.palette-switcher button {
  border: 0;
  font: inherit;
  cursor: pointer;
}

.top-nav button {
  height: 38px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 0 12px;
  border-radius: 8px;
  background: transparent;
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 850;
  transition: background 180ms ease, color 180ms ease, transform 180ms ease;
}

.nav-chevron {
  opacity: 0.72;
  transition: transform 180ms ease;
}

.has-children:hover .nav-chevron,
.has-children:focus-within .nav-chevron {
  transform: rotate(180deg);
}

.top-nav button:hover,
.top-nav button.active {
  background: rgba(var(--primary-rgb), 0.13);
  color: var(--text-primary);
  transform: translateY(-1px);
}

.top-nav button.active {
  color: var(--primary-color);
}

.top-nav-submenu {
  position: absolute;
  top: calc(100% + 10px);
  left: 50%;
  z-index: 140;
  width: 196px;
  padding: 8px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: rgba(var(--glass-bg-rgb), 0.94);
  backdrop-filter: blur(22px) saturate(170%);
  box-shadow: var(--shadow-soft);
  transform: translate(-50%, 8px);
  opacity: 0;
  pointer-events: none;
  transition: opacity 160ms ease, transform 160ms ease;
}

.top-nav-submenu::before {
  content: "";
  position: absolute;
  left: 0;
  right: 0;
  top: -12px;
  height: 12px;
}

.has-children:hover .top-nav-submenu,
.has-children:focus-within .top-nav-submenu {
  opacity: 1;
  pointer-events: auto;
  transform: translate(-50%, 0);
}

.top-nav .top-nav-submenu button {
  width: 100%;
  height: 36px;
  justify-content: flex-start;
  padding: 0 10px;
}

.top-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 7px;
}

.palette-switcher {
  height: 38px;
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 0 8px;
  border: 1px solid var(--border-color);
  border-radius: 10px;
  background: var(--surface-3);
}

.palette-switcher button {
  width: 20px;
  height: 20px;
  border-radius: 999px;
  background: var(--swatch);
  box-shadow:
    inset 0 0 0 1px rgba(255,255,255,0.5),
    0 0 16px color-mix(in srgb, var(--swatch) 32%, transparent);
  transition: transform 160ms ease, box-shadow 160ms ease, filter 160ms ease;
}

.palette-switcher button.active {
  filter: saturate(1.18) brightness(1.16);
  transform: scale(1.22);
  box-shadow:
    0 0 0 3px rgba(var(--primary-rgb), 0.28),
    0 0 22px rgba(var(--primary-rgb), 0.42),
    inset 0 0 0 1px rgba(255,255,255,0.78);
}

.icon-btn,
.lang-btn,
.avatar-btn {
  height: 38px;
  border-radius: 10px;
  color: var(--text-primary);
  background: var(--surface-3);
  border: 1px solid var(--border-color);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: transform 180ms ease, border-color 180ms ease, background 180ms ease;
}

.icon-btn {
  width: 38px;
}

.lang-btn {
  padding: 0 10px;
  font-size: 12px;
  font-weight: 900;
}

.avatar-btn {
  gap: 8px;
  padding: 0 12px;
  font-size: 12px;
  font-weight: 850;
}

.icon-btn:hover,
.lang-btn:hover,
.avatar-btn:hover {
  transform: translateY(-1px);
  border-color: rgba(var(--primary-rgb), 0.38);
}

@media (max-width: 1180px) {
  .brand-status,
  .top-nav > .nav-item-wrap > button span {
    display: none;
  }
}

@media (max-width: 760px) {
  .topbar {
    top: 8px;
    left: 8px;
    right: 8px;
    grid-template-columns: auto 1fr;
  }

  .top-nav {
    display: none;
  }

  .palette-switcher {
    display: none;
  }
}

@media (max-width: 520px) {
  .avatar-btn span {
    display: none;
  }
}
</style>
