<template>
  <el-config-provider>
    <div
      ref="appRootRef"
      class="app-root"
      :class="['dark', { 'is-public-preview': isPublicPreview, 'is-scroll-isolated': usesIsolatedScroll }]"
      @pointermove="handlePointerMove"
      @pointerleave="handlePointerLeave"
      @pointercancel="handlePointerLeave"
    >
      <SandBackground />
      <ReactBitsCursor
        v-if="!isLandingPage"
        :dark="true"
        :paused="false"
      />
      <div
        v-if="languageTransition.active"
        class="language-transition-layer"
        aria-hidden="true"
      >
        <span
          v-for="glitch in languageTransition.glitches"
          :key="glitch.id"
          class="language-glitch-token"
          :style="{
            left: glitch.left + 'px',
            top: glitch.top + 'px',
            width: glitch.width + 'px',
            height: glitch.height + 'px',
            fontSize: glitch.fontSize + 'px',
            fontWeight: glitch.fontWeight,
            textAlign: glitch.textAlign,
            color: glitch.color,
            letterSpacing: glitch.letterSpacing,
            '--lang-glitch-scale-x': glitch.scaleX.toFixed(3),
            '--lang-glitch-scale-y': glitch.scaleY.toFixed(3),
            '--lang-glitch-delay': glitch.distanceDelay + 'ms',
          }"
        >{{ glitch.displayText }}</span>
      </div>
      <Header />
      <div class="app-main" :class="{ 'has-sidebar': showSidebar && isDesktop }" :style="{ '--sidebar-width': showSidebar && isDesktop ? '240px' : '0px' }">
        <div class="sidebar-slot">
          <Sidebar />
        </div>
        <div class="app-content">
          <router-view v-slot="{ Component }">
            <component :is="Component" />
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
import ReactBitsCursor from '@/components/effects/ReactBitsCursor.vue'
import { onMounted, onUnmounted, computed, ref, nextTick, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useThemeStore } from '@/stores/theme.store'
import { useAuthStore } from '@/stores/auth.store'
import { APP_EVENTS } from '@/constants'

const themeStore = useThemeStore()
const authStore = useAuthStore()
const route = useRoute()
const appRootRef = ref<HTMLElement | null>(null)
const isDesktop = ref(window.innerWidth >= 1024)
const handleResize = () => {
  isDesktop.value = window.innerWidth >= 1024
  clearSpotlightState()
}
const isolatedScrollRoutes = new Set(['Data', 'AI'])
let pointerRaf = 0
let pendingPointerX = window.innerWidth / 2
let pendingPointerY = window.innerHeight / 2
let activeSpotlightElements = new Set<HTMLElement>()
const EDGE_GLOW_REACH = 128
const spotlightTargetSelector = [
  'button',
  '[role="button"]',
  '[data-edge-glow]',
  '.edge-glow-surface',
  '.spotlight-card',
  '.data-hero',
  '.asset-sidebar',
  '.asset-main',
  '.asset-detail',
  '.summary-card',
  '.asset-card',
  '.asset-tab',
  '.cloud-brief',
  '.el-button',
  'button.primary-action',
  'button.secondary-action',
  'button.text-action',
  'button.back-btn',
  'button.ctrl-toggle',
  'button.focus-btn',
  'button.msg-btn',
  'button.send-btn',
  'button.stop-btn',
  'button.model-action',
  'button.rule-action',
  '.mode-switch button',
  '.pager-chip button',
  '.hero-actions button',
  '.panel-expand button',
  '.quick-chips button',
  '.assistant-actions button',
  '.ai-panel-actions button',
  '.result-view-switch button',
  '.result-filter-chips button',
  '.smart-select-action',
  '.clear-select-action',
  '.module-select-toggle',
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
  'button.primary-action',
  'button.secondary-action',
  'button.text-action',
  'button.back-btn',
  'button.ctrl-toggle',
  'button.focus-btn',
  'button.msg-btn',
  'button.send-btn',
  'button.stop-btn',
  'button.model-action',
  'button.rule-action',
  '.mode-switch button',
  '.pager-chip button',
  '.hero-actions button',
  '.panel-expand button',
  '.quick-chips button',
  '.assistant-actions button',
  '.ai-panel-actions button',
  '.result-view-switch button',
  '.result-filter-chips button',
  '.smart-select-action',
  '.clear-select-action',
  '.module-select-toggle',
  '.el-button',
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
type LanguageGlitchToken = {
  id: number
  element: HTMLElement | null
  left: number
  top: number
  width: number
  height: number
  fontSize: number
  fontWeight: string
  textAlign: string
  color: string
  letterSpacing: string
  sourceText: string
  targetText: string
  displayText: string
  binaryLength: number
  scaleX: number
  scaleY: number
  distanceDelay: number
}
const languageTransition = ref<{ active: boolean; glitches: LanguageGlitchToken[] }>({
  active: false,
  glitches: [],
})
let languageTransitionToggleTimer = 0
let languageTransitionCleanupTimer = 0
let languageTransitionGlitchInterval = 0
let languageTransitionTokenId = 0
let languageTransitionStartedAt = 0
let globalStateCleanupTimer = 0
let languageTransitionOrigin = {
  x: window.innerWidth - 72,
  y: 42,
}
const LANGUAGE_GLITCH_STEP_MS = 46
const LANGUAGE_TOGGLE_DELAY_MS = 360
const LANGUAGE_LOCAL_SOURCE_MS = 210
const LANGUAGE_LOCAL_BRIDGE_MS = 180
const LANGUAGE_LOCAL_TARGET_MS = 320
const LANGUAGE_MAX_DISTANCE_DELAY_MS = 180
const LANGUAGE_TRANSITION_TOTAL_MS =
  LANGUAGE_MAX_DISTANCE_DELAY_MS + LANGUAGE_LOCAL_SOURCE_MS + LANGUAGE_LOCAL_BRIDGE_MS + LANGUAGE_LOCAL_TARGET_MS + 180

const languageTransitionSelectors = [
  'button',
  'a',
  'label',
  'p',
  'span',
  'strong',
  'em',
  'small',
  'h1',
  'h2',
  'h3',
  'h4',
  'h5',
  'h6',
  'li',
  'th',
  'td',
  '.el-button',
  '.el-tag',
  '.el-radio__label',
  '.el-checkbox__label',
  '.el-form-item__label',
].join(',')

const binaryChars = ['0', '1']
const randomBinary = (length: number) => Array.from({ length }, () => binaryChars[Math.floor(Math.random() * binaryChars.length)]).join('')
const isSpaceLike = (char: string) => /\s/.test(char)
const charOrder = (seed: number, index: number, count: number) => {
  if (count <= 1) return 0
  return ((index * 17 + seed * 13) % count) / (count - 1)
}
const textLengthForBinary = (text: string) => {
  const compact = text.replace(/\s+/g, '')
  if (!compact) return 0
  return Math.max(4, Math.min(40, compact.length * 2))
}
const reducedMotion = () => typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches
const readTransitionText = (node: HTMLElement | null) => node?.innerText?.trim() || ''
const collectLanguageCandidateNodes = () => {
  const root = appRootRef.value
  if (!root) return [] as Array<{ node: HTMLElement; rect: DOMRect; text: string }>
  return Array.from(root.querySelectorAll<HTMLElement>(languageTransitionSelectors))
    .map((node) => ({ node, rect: node.getBoundingClientRect(), text: readTransitionText(node) }))
    .filter(({ node, rect, text }) => {
      const style = window.getComputedStyle(node)
      if (node.closest('.language-transition-layer')) return false
      if (!text || !/[A-Za-z\u4e00-\u9fff0-9]/.test(text)) return false
      if (rect.width < 24 || rect.height < 10) return false
      if (rect.bottom < 0 || rect.top > window.innerHeight || rect.right < 0 || rect.left > window.innerWidth) return false
      if (style.visibility === 'hidden' || style.display === 'none' || Number(style.opacity) < 0.04) return false
      return true
    })
}
const applyWaveMetadata = (glitches: LanguageGlitchToken[]) => {
  glitches.forEach((glitch) => {
    if (!glitch.element?.isConnected) return
    glitch.element.dataset.langWave = '1'
    glitch.element.style.setProperty('--lang-wave-delay', `${glitch.distanceDelay}ms`)
  })
}
const clearWaveMetadata = (glitches: LanguageGlitchToken[]) => {
  glitches.forEach((glitch) => {
    if (!glitch.element) return
    glitch.element.removeAttribute('data-lang-wave')
    glitch.element.style.removeProperty('--lang-wave-delay')
  })
}

const transitionChar = (value: string | undefined, fallback = '') => value && value.length > 0 ? value : fallback
const sourceToBinaryText = (token: LanguageGlitchToken, progress: number) => {
  const chars = Array.from(token.sourceText)
  const count = Math.max(1, chars.filter((char) => !isSpaceLike(char)).length)
  let seen = 0
  return chars.map((char, index) => {
    if (isSpaceLike(char)) return char
    const order = charOrder(token.id, index, count)
    const shouldReplace = progress >= order
    seen += 1
    return shouldReplace ? randomBinary(1) : char
  }).join('')
}
const bridgeLanguageText = (token: LanguageGlitchToken, progress: number) => {
  const sourceChars = Array.from(token.sourceText)
  const targetChars = Array.from(token.targetText || token.sourceText)
  const count = Math.max(sourceChars.length, targetChars.length, 1)
  return Array.from({ length: count }, (_, index) => {
    const sourceChar = transitionChar(sourceChars[index], '')
    const targetChar = transitionChar(targetChars[index], '')
    const order = charOrder(token.id + 5, index, count)
    if (progress >= order) return targetChar || randomBinary(1)
    if (progress + 0.24 >= order) return randomBinary(1)
    return sourceChar || randomBinary(1)
  }).join('')
}
const binaryToTargetText = (token: LanguageGlitchToken, progress: number) => {
  const chars = Array.from(token.targetText || token.sourceText)
  const count = Math.max(1, chars.filter((char) => !isSpaceLike(char)).length)
  return chars.map((char, index) => {
    if (isSpaceLike(char)) return char
    const order = charOrder(token.id + 11, index, count)
    return progress >= order ? char : randomBinary(1)
  }).join('')
}

const collectLanguageGlitches = () => {
  const root = appRootRef.value
  if (!root) return [] as LanguageGlitchToken[]
  const rootRect = root.getBoundingClientRect()
  const nodes = collectLanguageCandidateNodes().map(({ node, rect, text }) => ({ node, rect, text }))
  const collected: Array<LanguageGlitchToken & { distance: number }> = []

  for (const { node, rect, text } of nodes) {
    const style = window.getComputedStyle(node)
    const centerX = rect.left + rect.width / 2
    const centerY = rect.top + rect.height / 2
    const distance = Math.hypot(centerX - languageTransitionOrigin.x, centerY - languageTransitionOrigin.y)

    collected.push({
      id: ++languageTransitionTokenId,
      element: node,
      left: rect.left - rootRect.left,
      top: rect.top - rootRect.top,
      width: rect.width,
      height: rect.height,
      fontSize: Number.parseFloat(style.fontSize) || 14,
      fontWeight: style.fontWeight || '700',
      textAlign: style.textAlign || 'left',
      color: style.color || 'var(--text-primary)',
      letterSpacing: style.letterSpacing === 'normal' ? '0.04em' : style.letterSpacing || '0.04em',
      sourceText: text,
      targetText: '',
      displayText: text,
      binaryLength: textLengthForBinary(text),
      scaleX: text.length > 8 ? 1.055 : 1.1,
      scaleY: text.length > 8 ? 0.955 : 0.92,
      distanceDelay: 0,
      distance,
    })
  }

  const maxDistance = collected.reduce((max, glitch) => Math.max(max, glitch.distance), 1)
  return collected
    .sort((a, b) => a.distance - b.distance)
    .slice(0, 180)
    .map(({ distance, ...glitch }) => ({
      ...glitch,
      distanceDelay: Math.min(220, (distance / maxDistance) * 220),
    }))
}

const hydrateLanguageTransitionTargets = () => {
  const root = appRootRef.value
  if (!root) return
  const rootRect = root.getBoundingClientRect()
  const candidates = collectLanguageCandidateNodes()
  const used = new Set<HTMLElement>()
  languageTransition.value = {
    active: languageTransition.value.active,
    glitches: languageTransition.value.glitches.map((glitch) => {
      const currentCenterX = rootRect.left + glitch.left + glitch.width / 2
      const currentCenterY = rootRect.top + glitch.top + glitch.height / 2
      const match = candidates
        .filter(({ node }) => !used.has(node))
        .map((candidate) => {
          const candidateCenterX = candidate.rect.left + candidate.rect.width / 2
          const candidateCenterY = candidate.rect.top + candidate.rect.height / 2
          const distance = Math.hypot(candidateCenterX - currentCenterX, candidateCenterY - currentCenterY)
          const sizeDiff = Math.abs(candidate.rect.width - glitch.width) + Math.abs(candidate.rect.height - glitch.height)
          const tagPenalty = candidate.node.tagName === glitch.element?.tagName ? 0 : 28
          return { candidate, score: distance + sizeDiff * 0.35 + tagPenalty }
        })
        .sort((a, b) => a.score - b.score)[0]?.candidate
      if (match) used.add(match.node)
      return {
        ...glitch,
        element: match?.node || glitch.element,
        targetText: match?.text || glitch.targetText || glitch.sourceText,
        width: match ? Math.max(glitch.width, match.rect.width) : glitch.width,
        height: match ? Math.max(glitch.height, match.rect.height) : glitch.height,
        left: match ? match.rect.left - rootRect.left : glitch.left,
        top: match ? match.rect.top - rootRect.top : glitch.top,
      }
    }),
  }
  applyWaveMetadata(languageTransition.value.glitches)
}

const refreshLanguageGlitches = () => {
  const elapsed = performance.now() - languageTransitionStartedAt
  languageTransition.value = {
    active: languageTransition.value.active,
    glitches: languageTransition.value.glitches.map((glitch) => ({
      ...glitch,
      displayText: (() => {
        const localElapsed = elapsed - glitch.distanceDelay
        if (localElapsed <= 0) return glitch.sourceText
        if (localElapsed <= LANGUAGE_LOCAL_SOURCE_MS) {
          return sourceToBinaryText(glitch, localElapsed / LANGUAGE_LOCAL_SOURCE_MS)
        }
        if (localElapsed <= LANGUAGE_LOCAL_SOURCE_MS + LANGUAGE_LOCAL_BRIDGE_MS) {
          return bridgeLanguageText(glitch, (localElapsed - LANGUAGE_LOCAL_SOURCE_MS) / LANGUAGE_LOCAL_BRIDGE_MS)
        }
        const decodeProgress = Math.min(
          1,
          (localElapsed - LANGUAGE_LOCAL_SOURCE_MS - LANGUAGE_LOCAL_BRIDGE_MS) / LANGUAGE_LOCAL_TARGET_MS,
        )
        return binaryToTargetText(glitch, decodeProgress)
      })(),
    })),
  }
}

const clearLanguageTransition = () => {
  const glitches = languageTransition.value.glitches
  if (languageTransitionToggleTimer) {
    window.clearTimeout(languageTransitionToggleTimer)
    languageTransitionToggleTimer = 0
  }
  if (languageTransitionCleanupTimer) {
    window.clearTimeout(languageTransitionCleanupTimer)
    languageTransitionCleanupTimer = 0
  }
  if (languageTransitionGlitchInterval) {
    window.clearInterval(languageTransitionGlitchInterval)
    languageTransitionGlitchInterval = 0
  }
  clearWaveMetadata(glitches)
  languageTransition.value = { active: false, glitches: [] }
  document.documentElement.classList.remove('lang-switching')
  document.documentElement.classList.remove('lang-switch-encoding')
  document.documentElement.classList.remove('lang-switch-decoding')
}

const sanitizeTransientUiState = () => {
  const root = document.documentElement
  const staleLanguageState =
    root.classList.contains('lang-switching') ||
    root.classList.contains('lang-switch-encoding') ||
    root.classList.contains('lang-switch-decoding') ||
    document.querySelector('[data-lang-wave="1"]')

  if (staleLanguageState && !languageTransition.value.active) {
    root.classList.remove('lang-switching', 'lang-switch-encoding', 'lang-switch-decoding')
    document.querySelectorAll<HTMLElement>('[data-lang-wave="1"]').forEach((node) => {
      node.removeAttribute('data-lang-wave')
      node.style.removeProperty('--lang-wave-delay')
    })
  }

  if (root.classList.contains('theme-switching') && !themeStore.themeTransition.active) {
    root.classList.remove('theme-switching')
  }
}

const triggerLanguageTransition = () => {
  if (reducedMotion()) {
    themeStore.toggleLang()
    return
  }

  clearLanguageTransition()
  languageTransition.value = {
    active: true,
    glitches: collectLanguageGlitches(),
  }
  languageTransitionStartedAt = performance.now()
  document.documentElement.classList.add('lang-switching')
  document.documentElement.classList.add('lang-switch-encoding')
  document.documentElement.classList.remove('lang-switch-decoding')
  applyWaveMetadata(languageTransition.value.glitches)
  refreshLanguageGlitches()

  languageTransitionGlitchInterval = window.setInterval(() => {
    refreshLanguageGlitches()
  }, LANGUAGE_GLITCH_STEP_MS)

  languageTransitionToggleTimer = window.setTimeout(() => {
    themeStore.toggleLang()
    nextTick(() => {
      requestAnimationFrame(() => {
        document.documentElement.classList.remove('lang-switch-encoding')
        document.documentElement.classList.add('lang-switch-decoding')
        hydrateLanguageTransitionTargets()
        refreshLanguageGlitches()
      })
    })
  }, LANGUAGE_TOGGLE_DELAY_MS)

  languageTransitionCleanupTimer = window.setTimeout(() => {
    clearLanguageTransition()
  }, LANGUAGE_TRANSITION_TOTAL_MS)
}

const handleLanguageToggleRequest = (event?: Event) => {
  const customEvent = event as CustomEvent<{ x?: number; y?: number }> | undefined
  languageTransitionOrigin = {
    x: customEvent?.detail?.x ?? window.innerWidth - 72,
    y: customEvent?.detail?.y ?? 42,
  }
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
    const radius = Math.max(176, Math.min(320, EDGE_GLOW_REACH + Math.max(rect.width, rect.height) * 0.38))
    const pseudoClass = resolveGlowPseudoClass(element)

    element.classList.add('edge-glow-active')
    if (pseudoClass) element.classList.add(pseudoClass)
    element.style.setProperty('--spotlight-x', `${x}px`)
    element.style.setProperty('--spotlight-y', `${y}px`)
    element.style.setProperty('--mx', `${x}px`)
    element.style.setProperty('--my', `${y}px`)
    element.style.setProperty('--edge-proximity', `${(strength * 100).toFixed(3)}`)
    element.style.setProperty('--edge-glow-strength', strength.toFixed(3))
    element.style.setProperty('--edge-glow-opacity', strength.toFixed(3))
    element.style.setProperty('--edge-glow-radius', `${radius.toFixed(1)}px`)
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
  if (element.closest('.language-transition-layer')) return false
  if (element.closest('.landing-page, .public-preview, .language-transition-layer')) return false
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
  const colors = [style.borderTopColor, style.borderRightColor, style.borderBottomColor, style.borderLeftColor]
  return widths.some((width, index) => {
    const px = Number.parseFloat(width)
    if (!Number.isFinite(px) || px <= 0) return false
    if (styles[index] === 'none' || styles[index] === 'hidden') return false
    return !/rgba?\(\s*0\s*,\s*0\s*,\s*0\s*,\s*0\s*\)|transparent/i.test(colors[index])
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
      const rawStrength = Math.max(0, 1 - distance / EDGE_GLOW_REACH)
      return {
        element,
        rect,
        distance,
        strength: Math.pow(rawStrength, 1.28),
      }
    })
    .filter(({ strength }) => strength > 0.035)
    .sort((a, b) => a.distance - b.distance || a.rect.width * a.rect.height - b.rect.width * b.rect.height)

  const targets: typeof candidates = []
  for (const candidate of candidates) {
    const overlapsSelected = targets.some(({ element }) => element.contains(candidate.element) || candidate.element.contains(element))
    if (overlapsSelected) continue
    targets.push(candidate)
    if (targets.length >= 5) break
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
const showFloatingAssistant = computed(() => !isPublicPreview.value)
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

.language-transition-layer {
  position: absolute;
  inset: 0;
  z-index: 230;
  pointer-events: none;
  overflow: clip;
}

.language-glitch-token {
  position: absolute;
  display: block;
  white-space: nowrap;
  font-family: "JetBrains Mono", "SFMono-Regular", Consolas, monospace;
  line-height: 1.05;
  opacity: 0;
  mix-blend-mode: screen;
  text-shadow:
    0 0 12px rgba(var(--primary-rgb), 0.24),
    0 0 1px currentColor;
  transform-origin: center;
  padding-inline: 0.04em;
  animation: language-glitch-token 560ms cubic-bezier(0.16, 1, 0.3, 1) forwards;
}

:global(html.lang-switching) :where(
  h1, h2, h3, h4, h5, h6,
  p, span, strong, em, small, li, a, button, label,
  th, td, .el-button, .el-tag, .el-radio__label, .el-checkbox__label, .el-form-item__label
) {
  animation: language-text-recode 520ms cubic-bezier(0.16, 1, 0.3, 1);
  transform-origin: center;
}

:global(html.lang-switch-encoding) [data-lang-wave="1"] {
  animation:
    language-wave-squeeze 520ms cubic-bezier(0.16, 1, 0.3, 1) both;
  animation-delay: var(--lang-wave-delay, 0ms);
}

:global(html.lang-switch-decoding) [data-lang-wave="1"] {
  animation:
    language-wave-release 520ms cubic-bezier(0.16, 1, 0.3, 1) both;
  animation-delay: var(--lang-wave-delay, 0ms);
}

:global(html.lang-switching) :where(
  svg, .el-icon, .module-icon, .tut-icon, .app-icon-tile
) {
  animation: language-icon-pulse 520ms cubic-bezier(0.16, 1, 0.3, 1);
  transform-origin: center;
}

:global(html.light.lang-switching) .language-glitch-token {
  mix-blend-mode: multiply;
}

@keyframes language-glitch-token {
  0% {
    opacity: 0;
    transform: scale(0.985);
    filter: blur(6px);
  }
  14% {
    opacity: 0.18;
    transform: scale(1.01, 0.99);
    filter: blur(2px);
  }
  34% {
    opacity: 0.92;
    transform: scale(var(--lang-glitch-scale-x), var(--lang-glitch-scale-y));
    filter: blur(0);
  }
  66% {
    opacity: 0.9;
    transform: scale(1.045, 0.96);
    filter: blur(0.4px);
  }
  100% {
    opacity: 0;
    transform: scale(0.995);
    filter: blur(6px);
  }
}

@keyframes language-text-recode {
  0% {
    opacity: 1;
    transform: scale(1);
    filter: blur(0);
  }
  22% {
    opacity: 0.92;
    transform: scale(1.028, 0.965);
    filter: blur(0.6px);
  }
  44% {
    opacity: 0.78;
    transform: scale(1.055, 0.9);
    filter: blur(0.8px);
  }
  72% {
    opacity: 0.9;
    transform: scale(0.992, 1.024);
    filter: blur(0.35px);
  }
  100% {
    opacity: 1;
    transform: scale(1);
    filter: blur(0);
  }
}

@keyframes language-icon-pulse {
  0% {
    transform: scale(1) rotate(0deg);
    filter: blur(0);
  }
  28% {
    transform: scale(1.06, 0.94) rotate(-2deg);
    filter: blur(0.4px);
  }
  48% {
    transform: scale(1.14, 0.86) rotate(-5deg);
    filter: blur(0.8px);
  }
  78% {
    transform: scale(0.96, 1.08) rotate(3deg);
    filter: blur(0);
  }
  100% {
    transform: scale(1) rotate(0deg);
    filter: blur(0);
  }
}

@keyframes language-wave-squeeze {
  0% {
    transform: scale(1);
    filter: blur(0);
  }
  52% {
    transform: scale(1.012, 0.984);
    filter: blur(0.3px);
  }
  100% {
    transform: scale(0.998, 1.004);
    filter: blur(0.18px);
  }
}

@keyframes language-wave-release {
  0% {
    transform: scale(0.998, 1.004);
    filter: blur(0.22px);
  }
  48% {
    transform: scale(1.006, 0.992);
    filter: blur(0.16px);
  }
  100% {
    transform: scale(1);
    filter: blur(0);
  }
}

@media (prefers-reduced-motion: reduce) {
  .language-transition-layer {
    display: none;
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
