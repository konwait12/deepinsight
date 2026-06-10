<template>
  <el-config-provider>
    <div
      ref="appRootRef"
      class="app-root"
      :class="{ 'is-public-preview': isPublicPreview, 'is-scroll-isolated': usesIsolatedScroll }"
      @pointermove="handlePointerMove"
      @pointerleave="handlePointerLeave"
      @pointercancel="handlePointerLeave"
    >
      <SandBackground />
      <IridescenceField
        v-if="!isLandingPage"
        class="app-iridescence-field"
        :active="!themeStore.isDarkMode"
        :speed="0.46"
        :amplitude="0.045"
        :opacity="0.3"
        :theme-strength="0.72"
        :mouse-react="true"
      />
      <ReactBitsCursor
        v-if="!isLandingPage && themeStore.isDarkMode"
        :dark="themeStore.isDarkMode"
        :paused="false"
      />
      <Header />
      <div class="app-main" :class="{ 'has-sidebar': showSidebar && isDesktop }" :style="{ '--sidebar-width': showSidebar && isDesktop ? '240px' : '0px' }">
        <div class="sidebar-slot">
          <Sidebar />
        </div>
        <div class="app-content">
          <router-view v-slot="{ Component }">
            <Transition name="route-cinema" mode="out-in" appear>
              <component :is="Component" :key="rootViewKey" class="route-view-shell" />
            </Transition>
          </router-view>
          <Footer v-if="!isPublicPreview" />
        </div>
      </div>
      <FloatingAssistant v-if="showFloatingAssistant" />
    </div>
  </el-config-provider>
</template>

<script setup lang="ts">
import FloatingAssistant from '@/components/ai/FloatingAssistant.vue'
import Header from '@/components/common/Header.vue'
import Sidebar from '@/components/common/Sidebar.vue'
import Footer from '@/components/common/Footer.vue'
import SandBackground from '@/components/background/SandBackground.vue'
import IridescenceField from '@/components/effects/IridescenceField.vue'
import ReactBitsCursor from '@/components/effects/ReactBitsCursor.vue'
import { onMounted, onUnmounted, computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useThemeStore } from '@/stores/theme.store'
import { useAuthStore } from '@/stores/auth.store'
import { APP_EVENTS, ROUTES } from '@/constants'

const themeStore = useThemeStore()
const authStore = useAuthStore()
const route = useRoute()
const appRootRef = ref<HTMLElement | null>(null)
const isDesktop = ref(window.innerWidth >= 1024)
const handleResize = () => {
  isDesktop.value = window.innerWidth >= 1024
  clearSpotlightState()
}
const isolatedScrollRoutes = new Set(['AI'])
let pointerRaf = 0
let pendingPointerX = window.innerWidth / 2
let pendingPointerY = window.innerHeight / 2
let activeSpotlightElements = new Set<HTMLElement>()
const EDGE_GLOW_REACH = 104
const TILT_MAX_ROTATION = 1.18
const TILT_COMPACT_ROTATION = 0.34
const spotlightTargetSelector = [
  'button',
  '[role="button"]',
  '[data-edge-glow]',
  '.edge-glow-surface',
  '.el-card',
  '.glass-panel',
  '.glass-panel-light',
  '.glass-panel-heavy',
  '.surface-hover',
  '.toolbar',
  '.control-bar',
  '.control-card',
  '.table-card',
  '.chart-card',
  '.config-card',
  '.result-card',
  '.job-card',
  '.section-card',
  '.ai-insight-card',
  '.model-chip',
  '.result-item',
  '.progress-item',
  '.perf-item',
  '.module-card',
  '.data-hero',
  '.summary-card',
  '.asset-card',
  '.cloud-brief',
  '.step-section',
  '.el-button',
  '.el-input__wrapper',
  '.el-select__wrapper',
  '.icon-btn',
  '.lang-btn',
  '.avatar-btn',
  '.top-nav button',
  '.palette-switcher button',
  '.module-icon',
  '.tut-icon',
  '.app-icon-tile',
].join(',')
const nativeGlowSurfaceSelector = [
  'button',
  '.el-card',
  '.glass-panel',
  '.glass-panel-light',
  '.glass-panel-heavy',
  '.surface-hover',
  '.toolbar',
  '.control-bar',
  '.control-card',
  '.table-card',
  '.chart-card',
  '.config-card',
  '.result-card',
  '.job-card',
  '.section-card',
  '.ai-insight-card',
  '.model-chip',
  '.result-item',
  '.progress-item',
  '.perf-item',
  '.module-card',
  '.step-section',
  '.el-button',
  '.el-input__wrapper',
  '.icon-btn',
  '.lang-btn',
  '.avatar-btn',
  '.palette-switcher button',
  '.module-icon',
  '.tut-icon',
  '.app-icon-tile',
].join(',')
const kineticTiltSelector = [
  'button:not(:disabled)',
  'a[href]',
  '[role="button"]',
  '.el-button:not(.is-disabled)',
  '.model-row',
  '.asset-row',
  '.dataset-card-button',
  '.asset-tab',
  '.module-chip',
  '.mode-tabs button',
  '.task-tabs button',
  '.top-nav button',
  '.mini-row',
  '.cloud-folder-dock',
].join(',')
const compactKineticSelector = [
  'button',
  'a[href]',
  '[role="button"]',
  '.el-button',
  '.icon-btn',
  '.lang-btn',
  '.avatar-btn',
  '.top-nav button',
  '.palette-switcher button',
  '.result-view-switch button',
  '.result-filter-chips button',
  '.analysis-mode-switch button',
].join(',')
const textEntrySurfaceSelector = [
  'input',
  'textarea',
  'select',
  '[contenteditable="true"]',
  '[role="textbox"]',
  '.el-input',
  '.el-input__wrapper',
  '.el-input__inner',
  '.el-textarea',
  '.el-textarea__inner',
  '.el-select',
  '.el-select__wrapper',
  '.composer',
  '.assistant-input',
  '.assistant-input-shell',
  '.cloud-search',
  '.folder-tools',
  '.search-shell',
  '.search-input',
].join(',')
let languageTransitionCleanupTimer = 0
let globalStateCleanupTimer = 0
const reducedMotion = () => typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches
const coarsePointer = () => typeof window !== 'undefined' && window.matchMedia('(hover: none), (pointer: coarse)').matches
const clampNumber = (value: number, min: number, max: number) => Math.max(min, Math.min(max, value))
const isPointerInsideRect = (rect: DOMRect, x: number, y: number) => x >= rect.left && x <= rect.right && y >= rect.top && y <= rect.bottom

const resetKineticTiltElement = (element: HTMLElement | null) => {
  if (!element?.isConnected) return
  element.classList.remove('kinetic-tilt-active', 'kinetic-tilt-compact')
  element.style.removeProperty('--tilt-rx')
  element.style.removeProperty('--tilt-ry')
  element.style.removeProperty('--tilt-lift')
  element.style.removeProperty('--tilt-scale')
  element.style.removeProperty('--tilt-glint')
}

const isKineticTiltCandidate = (element: HTMLElement) => {
  if (element.closest('[data-tilt="off"], .tilt-disabled')) return false
  if (element.matches(textEntrySurfaceSelector) || element.closest(textEntrySurfaceSelector)) return false
  if (element instanceof HTMLInputElement || element instanceof HTMLTextAreaElement || element instanceof HTMLSelectElement) return false
  return element.matches(kineticTiltSelector)
}

const isCompactKineticSurface = (element: HTMLElement) =>
  element.matches(compactKineticSelector) ||
  element.closest('.top-nav, .palette-switcher, .result-view-switch, .result-filter-chips, .analysis-mode-switch, .ai-panel-actions') !== null

const applyKineticTiltState = (element: HTMLElement, rect: DOMRect, localX: number, localY: number, strength: number) => {
  if (reducedMotion() || !isKineticTiltCandidate(element)) {
    resetKineticTiltElement(element)
    return
  }
  if (localX < 0 || localX > rect.width || localY < 0 || localY > rect.height) {
    resetKineticTiltElement(element)
    return
  }

  const compact = isCompactKineticSurface(element)
  const maxRotation = compact ? TILT_COMPACT_ROTATION : TILT_MAX_ROTATION
  const strength01 = Math.min(strength, 1)
  const dx = localX - rect.width / 2
  const dy = localY - rect.height / 2
  const rotateX = clampNumber((-dy / Math.max(rect.height / 2, 1)) * maxRotation, -maxRotation, maxRotation)
  const rotateY = clampNumber((dx / Math.max(rect.width / 2, 1)) * maxRotation, -maxRotation, maxRotation)
  const lift = compact ? -0.1 - strength01 * 0.24 : -0.28 - strength01 * 0.64
  const scale = 1 + strength01 * (compact ? 0.0016 : 0.0038)
  const glint = compact ? Math.min(0.12, 0.03 + strength * 0.08) : Math.min(0.18, 0.04 + strength * 0.12)

  element.classList.add('kinetic-tilt-active')
  element.classList.toggle('kinetic-tilt-compact', compact)
  element.style.setProperty('--tilt-rx', `${rotateX.toFixed(3)}deg`)
  element.style.setProperty('--tilt-ry', `${rotateY.toFixed(3)}deg`)
  element.style.setProperty('--tilt-lift', `${lift.toFixed(3)}px`)
  element.style.setProperty('--tilt-scale', scale.toFixed(4))
  element.style.setProperty('--tilt-glint', glint.toFixed(3))
}

const clearLanguageTransition = () => {
  if (languageTransitionCleanupTimer) {
    window.clearTimeout(languageTransitionCleanupTimer)
    languageTransitionCleanupTimer = 0
  }
  document.documentElement.classList.remove('lang-soft-switching')
}

const sanitizeTransientUiState = () => {
  const root = document.documentElement
  root.classList.remove('lang-switching', 'lang-switch-encoding', 'lang-switch-decoding')
  document.querySelectorAll<HTMLElement>('[data-lang-wave="1"]').forEach((node) => {
    node.removeAttribute('data-lang-wave')
    node.style.removeProperty('--lang-wave-delay')
  })

  if (root.classList.contains('theme-switching') && !themeStore.themeTransition.active) {
    root.classList.remove('theme-switching')
  }
}

const triggerLanguageTransition = () => {
  clearLanguageTransition()
  if (!reducedMotion()) document.documentElement.classList.add('lang-soft-switching')
  themeStore.toggleLang()
  languageTransitionCleanupTimer = window.setTimeout(() => {
    document.documentElement.classList.remove('lang-soft-switching')
    languageTransitionCleanupTimer = 0
  }, 240)
}

const handleLanguageToggleRequest = () => {
  triggerLanguageTransition()
}

function flushPointerVars() {
  pointerRaf = 0
  const root = appRootRef.value
  if (!root) return
  root.style.setProperty('--pointer-x', `${pendingPointerX}px`)
  root.style.setProperty('--pointer-y', `${pendingPointerY}px`)

  if (isLandingPage.value) {
    clearSpotlightState()
    return
  }

  const nextSpotlightElements = collectGlowTargets(root)
  applyGlowTargetState(nextSpotlightElements)
}

const applyGlowTargetState = (nextSpotlightElements: Map<HTMLElement, { rect: DOMRect; strength: number }>) => {
  activeSpotlightElements.forEach((element) => {
    if (!nextSpotlightElements.has(element)) resetSpotlightElement(element)
  })

  nextSpotlightElements.forEach((edgeState, element) => {
    const { rect, strength } = edgeState
    const x = pendingPointerX - rect.left
    const y = pendingPointerY - rect.top
    const cx = rect.width / 2
    const cy = rect.height / 2
    const dx = x - cx
    const dy = y - cy
    const radians = Math.atan2(dy, dx)
    let angleDeg = radians * (180 / Math.PI) + 90
    if (angleDeg < 0) angleDeg += 360

    const radius = Math.max(116, Math.min(210, EDGE_GLOW_REACH + Math.max(rect.width, rect.height) * 0.18))
    const pseudoClass = resolveGlowPseudoClass(element)
    const glowOpacity = Math.min(0.62, strength * 0.62)

    element.classList.add('edge-glow-active')
    if (pseudoClass) element.classList.add(pseudoClass)
    element.style.setProperty('--spotlight-x', `${x}px`)
    element.style.setProperty('--spotlight-y', `${y}px`)
    element.style.setProperty('--mx', `${x}px`)
    element.style.setProperty('--my', `${y}px`)
    element.style.setProperty('--edge-proximity', `${(strength * 100).toFixed(3)}`)
    element.style.setProperty('--cursor-angle', `${angleDeg.toFixed(3)}deg`)
    element.style.setProperty('--edge-glow-strength', strength.toFixed(3))
    element.style.setProperty('--edge-glow-opacity', glowOpacity.toFixed(3))
    element.style.setProperty('--edge-glow-radius', `${radius.toFixed(1)}px`)
    applyKineticTiltState(element, rect, x, y, strength)
  })

  activeSpotlightElements = new Set(nextSpotlightElements.keys())
}

const resetSpotlightElement = (element: HTMLElement | null) => {
  if (!element?.isConnected) return
  element.style.setProperty('--spotlight-x', '50%')
  element.style.setProperty('--spotlight-y', '50%')
  element.style.removeProperty('--mx')
  element.style.removeProperty('--my')
  element.style.setProperty('--edge-proximity', '0')
  element.style.setProperty('--cursor-angle', '45deg')
  element.style.removeProperty('--edge-glow-strength')
  element.style.removeProperty('--edge-glow-opacity')
  element.style.removeProperty('--edge-glow-radius')
  element.style.removeProperty('--spotlight-color')
  resetKineticTiltElement(element)
  element.classList.remove('edge-glow-active')
  element.classList.remove('edge-glow-pseudo-before')
  element.classList.remove('edge-glow-pseudo-after')
}

const clearSpotlightState = () => {
  activeSpotlightElements.forEach(resetSpotlightElement)
  activeSpotlightElements = new Set()
}

const isGlowCandidate = (element: HTMLElement) => {
  const rect = element.getBoundingClientRect()
  if (rect.width < 36 || rect.height < 24) return false
  if (rect.bottom < 0 || rect.top > window.innerHeight || rect.right < 0 || rect.left > window.innerWidth) return false
  if (element.closest('.landing-page, .public-preview')) return false
  if (element.closest('[data-edge-glow="off"], .edge-glow-disabled')) return false
  if (element.matches(textEntrySurfaceSelector) || element.closest(textEntrySurfaceSelector)) return false
  if (element.classList.contains('magic-bento-card')) return false
  if (element.classList.contains('app-root') || element.classList.contains('app-main') || element.classList.contains('app-content')) return false
  const style = window.getComputedStyle(element)
  if (style.visibility === 'hidden' || ['none', 'contents'].includes(style.display) || Number(style.opacity) < 0.05) return false
  if (element instanceof HTMLInputElement || element instanceof HTMLTextAreaElement || element instanceof HTMLSelectElement) return false
  if (!hasVisibleBorder(style)) return false
  return true
}

const hasVisibleBorder = (style: CSSStyleDeclaration) => {
  const widths = [style.borderTopWidth, style.borderRightWidth, style.borderBottomWidth, style.borderLeftWidth]
  const styles = [style.borderTopStyle, style.borderRightStyle, style.borderBottomStyle, style.borderLeftStyle]
  return widths.some((width, index) => {
    const px = Number.parseFloat(width)
    if (!Number.isFinite(px) || px <= 0) return false
    return styles[index] !== 'none' && styles[index] !== 'hidden'
  })
}

const distanceToRectEdge = (rect: DOMRect, x: number, y: number) => {
  const insideX = x >= rect.left && x <= rect.right
  const insideY = y >= rect.top && y <= rect.bottom
  if (insideX && insideY) {
    return Math.min(x - rect.left, rect.right - x, y - rect.top, rect.bottom - y)
  }

  const dx = x < rect.left ? rect.left - x : x > rect.right ? x - rect.right : 0
  const dy = y < rect.top ? rect.top - y : y > rect.bottom ? y - rect.bottom : 0
  return Math.hypot(dx, dy)
}

const collectGlowTargets = (root: HTMLElement) => {
  const candidates = Array.from(root.querySelectorAll<HTMLElement>(spotlightTargetSelector))
    .filter(isGlowCandidate)
    .map((element) => {
      const rect = element.getBoundingClientRect()
      const distance = distanceToRectEdge(rect, pendingPointerX, pendingPointerY)
      const pointerInside = isPointerInsideRect(rect, pendingPointerX, pendingPointerY)
      const rawStrength = pointerInside ? Math.max(0.18, 1 - distance / EDGE_GLOW_REACH) : Math.max(0, 1 - distance / EDGE_GLOW_REACH)
      return {
        element,
        rect,
        distance,
        strength: Math.pow(rawStrength, 1.35),
      }
    })
    .filter(({ strength }) => strength > 0.06)
    .sort((a, b) => a.distance - b.distance || a.rect.width * a.rect.height - b.rect.width * b.rect.height)

  const targets: typeof candidates = []
  for (const candidate of candidates) {
    const overlapsSelected = targets.some(({ element }) => element.contains(candidate.element) || candidate.element.contains(element))
    if (overlapsSelected) continue
    targets.push(candidate)
    if (targets.length >= 3) break
  }

  return new Map(targets.map(({ element, rect, strength }) => [element, { rect, strength }]))
}

const syncNearbyEdgeGlow = () => {
  const root = appRootRef.value
  if (!root || isLandingPage.value) return
  const nextSpotlightElements = collectGlowTargets(root)
  applyGlowTargetState(nextSpotlightElements)
}

const isPseudoSlotAvailable = (content: string) => ['none', 'normal'].includes(content.trim().toLowerCase())

const resolveGlowPseudoClass = (element: HTMLElement) => {
  if (element.matches(nativeGlowSurfaceSelector)) return ''
  if (element.classList.contains('edge-glow-pseudo-after')) return 'edge-glow-pseudo-after'
  if (element.classList.contains('edge-glow-pseudo-before')) return 'edge-glow-pseudo-before'
  const afterContent = window.getComputedStyle(element, '::after').content
  if (isPseudoSlotAvailable(afterContent)) return 'edge-glow-pseudo-after'
  const beforeContent = window.getComputedStyle(element, '::before').content
  if (isPseudoSlotAvailable(beforeContent)) return 'edge-glow-pseudo-before'
  return ''
}

const handlePointerMove = (event: PointerEvent) => {
  if (reducedMotion() || coarsePointer()) return
  pendingPointerX = event.clientX
  pendingPointerY = event.clientY
  if (!pointerRaf) pointerRaf = window.requestAnimationFrame(flushPointerVars)
}

const handlePointerLeave = () => {
  clearSpotlightState()
}

const routeName = computed(() => String(route.name || ''))
const isLandingPage = computed(() => routeName.value === 'Landing')
const isPublicPreview = computed(() => ['Landing', 'Login'].includes(routeName.value))
const usesIsolatedScroll = computed(() => isolatedScrollRoutes.has(routeName.value))
const isAiWorkspaceRoute = computed(() => route.path === ROUTES.AI || route.path === '/ai')
const workspaceRouteAliases = new Set([
  ROUTES.TRAINING,
  '/training',
  ROUTES.DATA,
  '/data',
  ROUTES.PREDICTION,
  '/prediction',
  ROUTES.DATASET_VIZ,
  '/dataset-viz',
  ROUTES.VIZ,
  '/viz',
])
const rootViewKey = computed(() => {
  const path = route.path
  if (workspaceRouteAliases.has(path) || path.startsWith('/analysis')) return 'workspace-layout'
  if (path === ROUTES.AI || path === '/ai') return 'ai-workspace'
  if (path.startsWith('/admin')) return 'admin-layout'
  return route.fullPath
})
const showFloatingAssistant = computed(() => !isPublicPreview.value && !isAiWorkspaceRoute.value)
const showSidebar = computed(() => {
  if (route.path === '/login') return false
  if (route.path === '/') return false
  return !themeStore.isHorizontalMenu
})

watch(
  () => route.fullPath,
  () => {
    clearSpotlightState()
    clearLanguageTransition()
    themeStore.clearThemeTransition()
    sanitizeTransientUiState()
  },
)

onMounted(() => {
  themeStore.initTheme()
  authStore.bootstrap()
  flushPointerVars()
  sanitizeTransientUiState()
  globalStateCleanupTimer = window.setInterval(sanitizeTransientUiState, 1200)
  window.addEventListener('resize', handleResize)
  window.addEventListener('scroll', syncNearbyEdgeGlow, true)
  window.addEventListener('blur', sanitizeTransientUiState)
  document.addEventListener('visibilitychange', sanitizeTransientUiState)
  window.addEventListener(APP_EVENTS.TOGGLE_LANG, handleLanguageToggleRequest)
})

onUnmounted(() => {
  if (pointerRaf) window.cancelAnimationFrame(pointerRaf)
  if (globalStateCleanupTimer) window.clearInterval(globalStateCleanupTimer)
  clearSpotlightState()
  clearLanguageTransition()
  themeStore.clearThemeTransition()
  window.removeEventListener('resize', handleResize)
  window.removeEventListener('scroll', syncNearbyEdgeGlow, true)
  window.removeEventListener('blur', sanitizeTransientUiState)
  document.removeEventListener('visibilitychange', sanitizeTransientUiState)
  window.removeEventListener(APP_EVENTS.TOGGLE_LANG, handleLanguageToggleRequest)
})
</script>

<style>
.app-root {
  width: 100%;
  min-width: 0;
  min-height: 100dvh;
  background: var(--bg-color);
  color: var(--text-primary);
  overflow-x: hidden;
  overscroll-behavior-x: none;
  transition: background 280ms ease, color 280ms ease;
}

.app-root.dark {
  background: #05070a !important;
  color: #f7f3ea !important;
  --text-primary: #f7f3ea !important;
  --text-secondary: #aab4c2 !important;
  --text-muted: #707d8f !important;
}

.app-main {
  width: 100%;
  min-width: 0;
  min-height: 100dvh;
  padding-top: var(--header-height, 72px);
  display: grid;
  grid-template-columns: var(--sidebar-width, 0px) minmax(0, 1fr);
  overscroll-behavior: contain;
  transition: grid-template-columns 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}
.app-main.has-sidebar { grid-template-columns: 240px minmax(0, 1fr); }
.app-content {
  width: 100%;
  min-width: 0;
  min-height: calc(100dvh - var(--header-height, 72px));
  position: relative;
  z-index: 2;
  overscroll-behavior: contain;
  perspective: 1600px;
}

.app-iridescence-field {
  z-index: 1;
  opacity: 0.95;
  filter: saturate(1.08) contrast(1.02);
}

html.light .app-root {
  background:
    linear-gradient(180deg, rgba(250, 253, 255, 0.34), rgba(238, 246, 245, 0.2)),
    var(--bg-color);
}

.route-view-shell {
  min-width: 0;
  transform-origin: center 24px;
  backface-visibility: hidden;
}

.route-cinema-enter-active,
.route-cinema-leave-active {
  transition:
    opacity 220ms cubic-bezier(0.16, 1, 0.3, 1),
    transform 260ms cubic-bezier(0.16, 1, 0.3, 1),
    filter 220ms ease;
  will-change: opacity, transform, filter;
}

.route-cinema-enter-from {
  opacity: 0;
  filter: blur(5px) saturate(0.97);
  transform: translate3d(0, 8px, 0) scale(0.996);
}

.route-cinema-leave-to {
  opacity: 0;
  filter: blur(4px) saturate(0.98);
  transform: translate3d(0, -4px, 0) scale(0.998);
}

.route-cinema-enter-to,
.route-cinema-leave-from {
  opacity: 1;
  filter: blur(0) saturate(1);
  transform: translate3d(0, 0, 0) scale(1) rotateX(0deg);
}

:global(html.lang-soft-switching) .app-content,
:global(html.lang-soft-switching) .topbar,
:global(html.lang-soft-switching) .sidebar-slot {
  animation: language-soft-refresh 220ms cubic-bezier(0.16, 1, 0.3, 1) both;
}

@keyframes language-soft-refresh {
  0% {
    opacity: 0.84;
    filter: blur(2px) saturate(0.97);
    transform: translateY(1px);
  }
  42% {
    opacity: 0.96;
    filter: blur(0.5px) saturate(1.02);
  }
  100% {
    opacity: 1;
    filter: blur(0) saturate(1);
    transform: translateY(0);
  }
}

.app-root.is-scroll-isolated {
  height: 100dvh;
  overflow: hidden;
}

.app-root.is-scroll-isolated .app-main {
  height: 100dvh;
  overflow: hidden;
}

.app-root.is-scroll-isolated .app-content {
  height: calc(100dvh - var(--header-height, 72px));
  overflow: hidden;
}

@media (prefers-reduced-motion: reduce) {
  :global(html.lang-soft-switching) .app-content,
  :global(html.lang-soft-switching) .topbar,
  :global(html.lang-soft-switching) .sidebar-slot {
    animation: none;
  }

  .route-cinema-enter-active,
  .route-cinema-leave-active {
    transition: none;
  }
}

/* Keep a real layout gutter while the visual sidebar is fixed. */
.sidebar-slot {
  width: var(--sidebar-width, 0px);
  min-width: 0;
  position: relative;
  z-index: 3;
  overflow: visible;
  pointer-events: none;
  transition: width 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}
.sidebar-slot > * {
  width: 240px;
  transition: transform 0.35s cubic-bezier(0.4, 0, 0.2, 1), opacity 0.35s;
}
.has-sidebar .sidebar-slot {
  pointer-events: auto;
}
.has-sidebar .sidebar-slot > * { transform: translateX(0); opacity: 1; }
.app-main:not(.has-sidebar) .sidebar-slot > * {
  transform: translateX(-30px);
  opacity: 0;
  pointer-events: none;
}
</style>
