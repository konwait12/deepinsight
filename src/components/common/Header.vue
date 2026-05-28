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

      <div
        ref="exploreWrapRef"
        class="nav-item-wrap has-children explore-wrap"
        @mouseenter="exploreMenuOpen = true"
        @mouseleave="exploreMenuOpen = false"
        @focusin="exploreMenuOpen = true"
        @focusout="closeExploreOnFocusOut"
      >
        <button
          type="button"
          :class="{ active: isExploreActive }"
          aria-haspopup="menu"
          :aria-expanded="exploreMenuOpen ? 'true' : 'false'"
          @click="navigateExplore"
        >
          <Compass :size="15" stroke-width="2.3" />
          <span>{{ t('nav.exploreCenter') }}</span>
          <ChevronDown class="nav-chevron" :size="13" stroke-width="2.5" />
        </button>

        <div class="top-nav-submenu" role="menu">
          <button
            v-for="item in exploreItems"
            :key="item.path"
            type="button"
            role="menuitem"
            :class="{ active: isExploreItemActive(item) }"
            @click="navigateTo(item.path)"
          >
            <component :is="item.icon" :size="15" stroke-width="2.2" />
            <span>{{ navLabel(item.path) }}</span>
            <ChevronRight class="submenu-arrow" :size="13" stroke-width="2.5" />
          </button>
        </div>
      </div>
    </nav>

    <div class="top-actions">
      <div class="palette-switcher" aria-label="Palette">
        <div class="palette-strip">
          <button
            v-for="palette in featuredPalettes"
            :key="palette.id"
            type="button"
            class="palette-dot"
            :class="{ active: themeStore.paletteId === palette.id }"
            :title="palette.name"
            :aria-label="palette.name"
            :aria-pressed="themeStore.paletteId === palette.id"
            :style="paletteVars(palette)"
            @click="themeStore.setPalette(palette.id)"
          ></button>
          <span class="palette-more" aria-hidden="true"></span>
        </div>

        <div class="palette-panel" :aria-label="paletteCopy.panelAria">
          <div class="palette-panel-head">
            <span>{{ paletteCopy.title }}</span>
            <strong>{{ paletteDisplayName(themeStore.palette) }}</strong>
          </div>
          <div
            class="palette-gradient"
            :style="paletteVars(themeStore.palette)"
          ></div>
          <label class="palette-slider">
            <span>
              <b>{{ paletteCopy.slider }}</b>
              <em>{{ themeStore.customHue }}</em>
            </span>
            <input
              type="range"
              min="0"
              max="360"
              step="1"
              :value="themeStore.customHue"
              :aria-label="paletteCopy.sliderAria"
              @input="handleHueInput"
            >
          </label>
          <div class="palette-grid">
            <button
              v-for="palette in visiblePalettes"
              :key="palette.id"
              type="button"
              :class="{ active: themeStore.paletteId === palette.id }"
              :title="paletteDisplayName(palette)"
              :aria-label="paletteDisplayName(palette)"
              :aria-pressed="themeStore.paletteId === palette.id"
              :style="paletteVars(palette)"
              @click="themeStore.setPalette(palette.id)"
            >
              <span class="palette-swatch"></span>
              <span>{{ paletteDisplayName(palette) }}</span>
            </button>
          </div>
        </div>
      </div>

      <button
        class="icon-btn theme-btn"
        type="button"
        :aria-label="themeStore.isDarkMode ? 'Switch to day theme' : 'Switch to night theme'"
        :title="themeStore.isDarkMode ? 'Day theme' : 'Night theme'"
        @click="themeStore.toggleDarkMode"
      >
        <Sun v-if="themeStore.isDarkMode" :size="17" stroke-width="2.4" />
        <Moon v-else :size="17" stroke-width="2.4" />
      </button>

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
import { useThemeStore, type Palette } from '@/stores/theme.store'
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
  ChevronRight,
  Compass,
  PanelLeftClose,
  PanelTop,
  Moon,
  Sun,
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
const exploreWrapRef = ref<HTMLElement | null>(null)
const exploreMenuOpen = ref(false)
const navItems = mainNavItems
const exploreItems = exploreNavItems
const featuredPalettes = computed(() => themeStore.palettes.slice(0, 4))
const visiblePalettes = computed(() => themeStore.palettes)
const langLabel = computed(() => themeStore.lang === 'zh' ? 'EN' : '中')
const paletteCopy = computed(() => themeStore.lang === 'zh'
  ? {
      title: '主题色',
      slider: '自定义色相',
      sliderAria: '拖动选择自定义主题色',
      panelAria: '主题色预设',
      names: {
        aurora: '极光',
        copper: '暖铜',
        violet: '紫晶',
        graphite: '天青',
        rose: '蔷薇',
        amber: '琥珀',
        indigo: '靛蓝',
        mint: '薄荷',
        custom: '自定义',
      },
    }
  : {
      title: 'Theme Color',
      slider: 'Custom Hue',
      sliderAria: 'Drag to choose a custom theme color',
      panelAria: 'Theme color presets',
      names: {
        aurora: 'Aurora',
        copper: 'Copper',
        violet: 'Violet',
        graphite: 'Sky',
        rose: 'Rose',
        amber: 'Amber',
        indigo: 'Indigo',
        mint: 'Mint',
        custom: 'Custom',
      },
    })
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
const navigateTo = (path: string) => {
  exploreMenuOpen.value = false
  router.push(path)
}
const navigateExplore = () => {
  exploreMenuOpen.value = false
  router.push(exploreItems[0]?.path || '/knowledge')
}
const handleAccount = () => router.push(authStore.isAuthenticated ? '/profile' : '/login')
const navLabel = (path: string) => t(navLabelKey(path))
const paletteVars = (palette: Palette) => ({
  '--swatch': palette.color,
  '--swatch-glow': palette.glow,
})
const paletteDisplayName = (palette: Palette) => paletteCopy.value.names[palette.id] || palette.name
const handleHueInput = (event: Event) => {
  const input = event.target as HTMLInputElement
  themeStore.setCustomHue(input.value)
}
const handleLangToggle = (event: MouseEvent) => window.dispatchEvent(new CustomEvent(APP_EVENTS.TOGGLE_LANG, {
  detail: { x: event.clientX, y: event.clientY },
}))
const closeExploreOnFocusOut = (event: FocusEvent) => {
  const nextTarget = event.relatedTarget as Node | null
  if (!nextTarget || !exploreWrapRef.value?.contains(nextTarget)) {
    exploreMenuOpen.value = false
  }
}

onMounted(() => {
  window.addEventListener('scroll', handleScroll, { passive: true })
})
onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})
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
  border-radius: 14px;
  background: var(--nav-bg);
  backdrop-filter: blur(14px) saturate(115%);
  box-shadow:
    0 1px 4px rgba(0, 0, 0, 0.08),
    0 4px 12px rgba(0, 0, 0, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 0.06),
    inset 0 -1px 0 rgba(0, 0, 0, 0.06);
  transition: background 240ms ease, border-color 240ms ease, transform 240ms ease;
}

.topbar.landing {
  background: rgba(5, 7, 10, 0.34);
  border-color: rgba(255, 255, 255, 0.12);
}

:global(html.light .topbar.landing) {
  background: rgba(255, 255, 255, 0.64);
  border-color: rgba(15, 23, 42, 0.1);
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
  font-weight: var(--font-weight-title);
  text-transform: uppercase;
  letter-spacing: var(--tracking-label);
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
  background: rgba(255, 255, 255, 0.055);
}

:global(html.light .top-nav),
:global(html.light .palette-switcher),
:global(html.light .icon-btn),
:global(html.light .lang-btn),
:global(html.light .avatar-btn) {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.84), rgba(240, 248, 246, 0.58)),
    rgba(var(--glass-bg-rgb), 0.62);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.78),
    0 8px 22px rgba(31, 56, 68, 0.055);
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
  padding: 0 14px;
  border-radius: 8px;
  background: transparent;
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 600;
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
  background: rgba(255, 255, 255, 0.08);
  color: var(--text-primary);
  transform: translateY(-0.5px);
}

:global(html.light .top-nav button:hover),
:global(html.light .top-nav button.active) {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.72), rgba(var(--primary-rgb), 0.11)),
    rgba(var(--primary-rgb), 0.09);
}

.top-nav button.active {
  color: var(--primary-color);
}

.top-nav-submenu {
  position: absolute;
  top: calc(100% + 10px);
  left: 50%;
  z-index: 140;
  width: 224px;
  display: grid;
  gap: 4px;
  padding: 9px;
  border: 1px solid var(--border-color);
  border-radius: 16px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.09), rgba(255, 255, 255, 0.026) 42%, rgba(0, 0, 0, 0.14)),
    var(--workbench-overlay-bg);
  backdrop-filter: blur(18px) saturate(118%);
  -webkit-backdrop-filter: blur(18px) saturate(118%);
  box-shadow:
    0 18px 52px rgba(0, 0, 0, 0.34),
    inset 0 1px 0 rgba(255, 255, 255, 0.1);
  transform: translate(-50%, 8px);
  transform-origin: top center;
  opacity: 0;
  pointer-events: none;
  transition: opacity 160ms ease, transform 160ms ease, border-color 160ms ease;
}

:global(html.light .top-nav-submenu) {
  border-color: rgba(20, 49, 60, 0.12);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.9), rgba(245, 251, 249, 0.66) 46%, rgba(28, 75, 88, 0.035)),
    rgba(var(--glass-bg-rgb), 0.9);
  box-shadow:
    0 18px 46px rgba(15, 23, 42, 0.12),
    inset 0 1px 0 rgba(255, 255, 255, 0.82);
}

.top-nav-submenu::before {
  content: "";
  position: absolute;
  left: 0;
  right: 0;
  top: -12px;
  height: 12px;
}

.top-nav-submenu::after {
  content: "";
  position: absolute;
  top: -5px;
  left: 50%;
  width: 10px;
  height: 10px;
  border-left: 1px solid var(--border-color);
  border-top: 1px solid var(--border-color);
  background: color-mix(in srgb, var(--workbench-overlay-bg) 92%, white 8%);
  transform: translateX(-50%) rotate(45deg);
}

:global(html.light .top-nav-submenu::after) {
  border-color: rgba(20, 49, 60, 0.12);
  background: rgba(249, 253, 251, 0.94);
}

.has-children:hover .top-nav-submenu,
.has-children:focus-within .top-nav-submenu {
  opacity: 1;
  pointer-events: auto;
  transform: translate(-50%, 0);
}

.top-nav .top-nav-submenu button {
  position: relative;
  z-index: 1;
  width: 100%;
  height: 40px;
  justify-content: flex-start;
  padding: 0 10px;
  border: 1px solid transparent;
  border-radius: 11px;
  color: var(--text-secondary);
  font-size: 13px;
  transition: background 160ms ease, border-color 160ms ease, color 160ms ease, transform 160ms ease;
}

.top-nav .top-nav-submenu button:hover,
.top-nav .top-nav-submenu button.active {
  border-color: rgba(var(--primary-rgb), 0.22);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.06), transparent),
    rgba(var(--primary-rgb), 0.1);
  color: var(--primary-color);
  transform: none;
}

.top-nav .top-nav-submenu button span {
  min-width: 0;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.submenu-arrow {
  flex: 0 0 auto;
  opacity: 0;
  transform: translateX(-3px);
  transition: opacity 160ms ease, transform 160ms ease;
}

.top-nav .top-nav-submenu button:hover .submenu-arrow,
.top-nav .top-nav-submenu button.active .submenu-arrow {
  opacity: 0.9;
  transform: translateX(0);
}

.top-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 7px;
}

.palette-switcher {
  position: relative;
  isolation: isolate;
  height: 38px;
  display: flex;
  align-items: center;
  padding: 0 9px;
  border: 1px solid var(--border-color);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.055);
}

.palette-strip {
  display: flex;
  align-items: center;
  gap: 6px;
}

.palette-dot {
  width: 22px;
  height: 22px;
  padding: 0;
  border-radius: 999px;
  background:
    radial-gradient(circle at 32% 24%, rgba(255, 255, 255, 0.82), transparent 0 22%, rgba(255, 255, 255, 0.08) 23% 34%, transparent 35%),
    linear-gradient(145deg, var(--swatch-glow), var(--swatch) 52%, color-mix(in srgb, var(--swatch) 72%, #111827));
  box-shadow:
    0 0 0 1px rgba(255, 255, 255, 0.2),
    0 4px 10px color-mix(in srgb, var(--swatch) 32%, transparent),
    inset 0 0 0 1px rgba(255, 255, 255, 0.5),
    inset 0 -7px 12px rgba(0, 0, 0, 0.18);
  transition: transform 160ms ease, box-shadow 160ms ease, filter 160ms ease;
}

.palette-dot.active,
.palette-grid button.active .palette-swatch {
  filter: saturate(1.08) brightness(1.08);
  transform: scale(1.14);
  box-shadow:
    0 0 0 3px color-mix(in srgb, var(--swatch) 18%, transparent),
    0 0 0 5px rgba(255, 255, 255, 0.1),
    0 7px 16px color-mix(in srgb, var(--swatch) 35%, transparent),
    inset 0 0 0 1px rgba(255, 255, 255, 0.72),
    inset 0 -7px 12px rgba(0, 0, 0, 0.16);
}

.palette-more {
  width: 5px;
  height: 22px;
  border-radius: 999px;
  background:
    linear-gradient(
      180deg,
      var(--primary-color),
      var(--accent-glow) 48%,
      var(--warning-glow)
    );
  opacity: 0.62;
  box-shadow: 0 0 12px rgba(var(--primary-rgb), 0.18);
}

.palette-panel {
  position: absolute;
  top: calc(100% + 10px);
  right: 0;
  z-index: 150;
  width: 286px;
  padding: 14px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.12), rgba(255, 255, 255, 0.035) 44%, rgba(0, 0, 0, 0.18)),
    rgba(var(--glass-bg-rgb), 0.9);
  backdrop-filter: blur(16px) saturate(125%);
  box-shadow:
    0 22px 58px rgba(0, 0, 0, 0.32),
    inset 0 1px 0 rgba(255, 255, 255, 0.12);
  opacity: 0;
  pointer-events: none;
  transform: translateY(8px) scale(0.98);
  transform-origin: top right;
  transition: opacity 160ms ease, transform 160ms ease;
}

.palette-panel::before {
  content: "";
  position: absolute;
  left: 0;
  right: 0;
  top: -12px;
  height: 12px;
}

.palette-switcher:hover .palette-panel,
.palette-switcher:focus-within .palette-panel {
  opacity: 1;
  pointer-events: auto;
  transform: translateY(0) scale(1);
}

:global(html.light .palette-panel) {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.9), rgba(247, 252, 250, 0.66) 46%, rgba(28, 75, 88, 0.035)),
    rgba(var(--glass-bg-rgb), 0.9);
  box-shadow:
    0 18px 46px rgba(15, 23, 42, 0.12),
    inset 0 1px 0 rgba(255, 255, 255, 0.82);
}

.palette-panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.2;
}

.palette-panel-head strong {
  color: var(--text-primary);
  font-size: 16px;
  font-weight: 700;
}

.palette-gradient {
  height: 36px;
  margin-top: 10px;
  border-radius: 999px;
  background:
    radial-gradient(circle at 14% 32%, rgba(255, 255, 255, 0.75), transparent 0 16%, transparent 17%),
    linear-gradient(90deg, color-mix(in srgb, var(--swatch) 12%, white), var(--swatch), var(--swatch-glow));
  box-shadow:
    inset 0 0 0 1px rgba(255, 255, 255, 0.35),
    inset 0 -12px 18px rgba(0, 0, 0, 0.14),
    0 10px 24px color-mix(in srgb, var(--swatch) 20%, transparent);
}

.palette-slider {
  display: grid;
  gap: 9px;
  margin-top: 14px;
  padding: 12px;
  border: 1px solid color-mix(in srgb, var(--swatch) 18%, var(--border-color));
  border-radius: 10px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.09), rgba(255, 255, 255, 0.025)),
    rgba(255, 255, 255, 0.04);
}

:global(html.light .palette-slider) {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.76), rgba(241, 249, 247, 0.52)),
    rgba(var(--glass-bg-rgb), 0.5);
}

.palette-slider span {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.palette-slider b {
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 700;
}

.palette-slider em {
  min-width: 48px;
  padding: 4px 9px;
  border-radius: 8px;
  color: var(--text-primary);
  background: color-mix(in srgb, var(--swatch) 16%, rgba(255, 255, 255, 0.08));
  font-style: normal;
  font-size: 13px;
  font-weight: 800;
  text-align: center;
}

.palette-slider input {
  width: 100%;
  height: 28px;
  margin: 0;
  padding: 0;
  appearance: none;
  background: transparent;
  cursor: pointer;
}

.palette-slider input::-webkit-slider-runnable-track {
  height: 12px;
  border-radius: 999px;
  background:
    linear-gradient(
      90deg,
      #fb7185 0%,
      #f59e5b 16%,
      #facc15 28%,
      #42e6a4 45%,
      #6ee7f9 62%,
      #6366f1 78%,
      #f0abfc 100%
    );
  box-shadow:
    inset 0 0 0 1px rgba(255, 255, 255, 0.42),
    0 8px 20px rgba(0, 0, 0, 0.08);
}

.palette-slider input::-webkit-slider-thumb {
  appearance: none;
  width: 18px;
  height: 24px;
  margin-top: -6px;
  border: 3px solid rgba(255, 255, 255, 0.95);
  border-radius: 999px;
  background: var(--swatch);
  box-shadow:
    0 0 0 2px color-mix(in srgb, var(--swatch) 24%, transparent),
    0 6px 16px color-mix(in srgb, var(--swatch) 38%, transparent);
}

.palette-slider input::-moz-range-track {
  height: 12px;
  border-radius: 999px;
  background:
    linear-gradient(
      90deg,
      #fb7185 0%,
      #f59e5b 16%,
      #facc15 28%,
      #42e6a4 45%,
      #6ee7f9 62%,
      #6366f1 78%,
      #f0abfc 100%
    );
}

.palette-slider input::-moz-range-thumb {
  width: 18px;
  height: 24px;
  border: 3px solid rgba(255, 255, 255, 0.95);
  border-radius: 999px;
  background: var(--swatch);
}

.palette-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin-top: 12px;
}

.palette-grid button {
  height: 38px;
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  padding: 0 10px;
  border-radius: 9px;
  color: var(--text-secondary);
  background: rgba(255, 255, 255, 0.055);
  border: 1px solid color-mix(in srgb, var(--swatch) 18%, var(--border-color));
  font-size: 12px;
  font-weight: 650;
  transition: transform 160ms ease, color 160ms ease, background 160ms ease, border-color 160ms ease, box-shadow 160ms ease;
}

.palette-grid button:hover,
.palette-grid button.active {
  color: var(--text-primary);
  border-color: color-mix(in srgb, var(--swatch) 36%, var(--border-color));
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--swatch) 12%, transparent), rgba(255, 255, 255, 0.045)),
    rgba(255, 255, 255, 0.055);
}

.palette-grid button.active {
  box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--swatch) 22%, transparent);
}

.palette-swatch {
  flex: 0 0 auto;
  width: 18px;
  height: 18px;
  border-radius: 999px;
  background:
    radial-gradient(circle at 32% 24%, rgba(255, 255, 255, 0.84), transparent 0 22%, rgba(255, 255, 255, 0.08) 23% 34%, transparent 35%),
    linear-gradient(145deg, var(--swatch-glow), var(--swatch) 54%, color-mix(in srgb, var(--swatch) 72%, #111827));
  box-shadow:
    inset 0 0 0 1px rgba(255, 255, 255, 0.52),
    inset 0 -6px 10px rgba(0, 0, 0, 0.16);
}

.palette-grid span:last-child {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.icon-btn,
.lang-btn,
.avatar-btn {
  height: 38px;
  border-radius: 10px;
  color: var(--text-primary);
  background: rgba(255, 255, 255, 0.055);
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
  padding: 0 12px;
  font-size: 14px;
  font-weight: 600;
}

.avatar-btn {
  gap: 8px;
  padding: 0 14px;
  font-size: 14px;
  font-weight: 600;
}

.icon-btn:hover,
.lang-btn:hover,
.avatar-btn:hover {
  transform: translateY(-0.5px);
  border-color: rgba(255, 255, 255, 0.22);
}

:global(html.light .icon-btn:hover),
:global(html.light .lang-btn:hover),
:global(html.light .avatar-btn:hover) {
  border-color: rgba(var(--primary-rgb), 0.24);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.72), rgba(var(--primary-rgb), 0.12)),
    rgba(var(--primary-rgb), 0.08);
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
