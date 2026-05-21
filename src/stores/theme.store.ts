import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import i18n from '@/i18n'
import { STORAGE_KEYS } from '@/constants'
import type { Lang } from '@/types/common'

export type PaletteId = 'aurora' | 'copper' | 'violet' | 'graphite'

export type Palette = {
  id: PaletteId
  name: string
  color: string
  rgb: [number, number, number]
  glow: string
  warning: string
  danger: string
}

type ThemeTransitionOrigin = {
  x: number
  y: number
}

type ThemeTransitionState = {
  active: boolean
  x: number
  y: number
  radius: number
  targetDark: boolean
  ringStrong: string
  ringSoft: string
  halo: string
}

export const palettes: Palette[] = [
  { id: 'aurora', name: 'Aurora', color: '#42e6a4', rgb: [66, 230, 164], glow: '#7dd3fc', warning: '#facc15', danger: '#fb7185' },
  { id: 'copper', name: 'Copper', color: '#f59e5b', rgb: [245, 158, 91], glow: '#ffd166', warning: '#34d399', danger: '#f97316' },
  { id: 'violet', name: 'Violet', color: '#9b8cff', rgb: [155, 140, 255], glow: '#f0abfc', warning: '#67e8f9', danger: '#fb7185' },
  { id: 'graphite', name: 'Graphite', color: '#6ee7f9', rgb: [110, 231, 249], glow: '#cbd5e1', warning: '#a3e635', danger: '#fda4af' },
]

const GLASS_OPACITY = 0.68
const GLASS_OPACITY_LIGHT = 0.6
const STALE_SKIN_KEYS = ['theme-hue', 'theme-saturation', 'theme-lightness', 'theme-glass-opacity']

function clamp(value: number, min: number, max: number) {
  return Math.min(max, Math.max(min, value))
}

export const useThemeStore = defineStore('theme', () => {
  const storedPalette = localStorage.getItem(STORAGE_KEYS.THEME_PALETTE) as PaletteId | null
  const paletteId = ref<PaletteId>(palettes.some((p) => p.id === storedPalette) ? storedPalette! : 'aurora')
  const isDarkMode = ref(localStorage.getItem(STORAGE_KEYS.THEME_IS_DARK) !== 'false')
  const isHorizontalMenu = ref(localStorage.getItem(STORAGE_KEYS.THEME_IS_HORIZONTAL) !== null ? localStorage.getItem(STORAGE_KEYS.THEME_IS_HORIZONTAL) === 'true' : true)
  const lang = ref<Lang>((localStorage.getItem(STORAGE_KEYS.LANG) as Lang) || 'zh')
  const themeTransition = ref<ThemeTransitionState>({
    active: false,
    x: 0,
    y: 0,
    radius: 0,
    targetDark: false,
    ringStrong: 'rgba(255,255,255,0.35)',
    ringSoft: 'rgba(255,255,255,0.16)',
    halo: 'rgba(66, 230, 164, 0.18)',
  })

  const palette = computed(() => palettes.find((p) => p.id === paletteId.value) || palettes[0])
  const primaryColor = computed(() => palette.value.color)
  let themeTransitionRaf: number | null = null
  let themeTransitionEndTimer: number | null = null

  const persistPalette = () => {
    localStorage.setItem(STORAGE_KEYS.THEME_PRIMARY_COLOR, primaryColor.value)
    localStorage.setItem(STORAGE_KEYS.THEME_PALETTE, paletteId.value)
  }

  const applyThemeVars = () => {
    const selectedPalette = palette.value
    const primary = selectedPalette.color
    const [r, g, b] = selectedPalette.rgb
    const dark = isDarkMode.value
    const root = document.documentElement

    root.style.setProperty('--primary-color', primary)
    root.style.setProperty('--primary-rgb', `${r}, ${g}, ${b}`)
    root.style.setProperty('--primary-color-rgb', `${r} ${g} ${b}`)
    root.style.setProperty('--accent-glow', selectedPalette.glow)
    root.style.setProperty('--warning-glow', selectedPalette.warning)
    root.style.setProperty('--danger-glow', selectedPalette.danger)

    if (dark) {
      const glassOpacity = GLASS_OPACITY
      const glassMix = 0.035
      const glassBase = [28, 31, 38]
      const glassR = Math.round(glassBase[0] * (1 - glassMix) + r * glassMix)
      const glassG = Math.round(glassBase[1] * (1 - glassMix) + g * glassMix)
      const glassB = Math.round(glassBase[2] * (1 - glassMix) + b * glassMix)
      root.style.setProperty('--glass-bg-rgb', `${glassR}, ${glassG}, ${glassB}`)
      root.style.setProperty('--glass-opacity', `${glassOpacity}`)
      root.style.setProperty('--glass-border-opacity', '0.14')
      root.style.setProperty('--glass-shadow-opacity', '0.1')
      root.style.setProperty('--glass-highlight-opacity', '0.18')
      root.style.setProperty('--primary-glow', `rgba(${r}, ${g}, ${b}, 0.1)`)
      root.style.setProperty('--primary-subtle', `rgba(${r}, ${g}, ${b}, 0.04)`)
      root.style.setProperty('--bg-tint', `rgba(${r}, ${g}, ${b}, 0.04)`)
      root.style.setProperty('--spotlight-color', `rgba(${r}, ${g}, ${b}, 0.1)`)
      root.style.setProperty('--spotlight-soft-color', `rgba(255, 255, 255, 0.07)`)
      root.style.setProperty('--spotlight-button-color', `rgba(255, 255, 255, 0.12)`)
      root.style.setProperty('--surface-2', `rgba(255, 255, 255, 0.07)`)
      root.style.setProperty('--surface-3', `rgba(255, 255, 255, 0.09)`)
      root.style.setProperty('--shadow-hover', `0 2px 12px rgba(0, 0, 0, 0.14), 0 0 0 1px rgba(${r}, ${g}, ${b}, 0.1)`)
      root.style.setProperty('--shadow-ring', `0 0 0 1px rgba(${r}, ${g}, ${b}, 0.12), 0 4px 16px rgba(0, 0, 0, 0.1)`)
      root.style.setProperty('--bg-input', `rgba(255, 255, 255, 0.075)`)
      root.style.setProperty('--border-color', `rgba(255, 255, 255, 0.12)`)
      root.style.setProperty('--border-strong', `rgba(255, 255, 255, 0.18)`)
      root.style.setProperty('--nav-bg', `linear-gradient(180deg, rgba(255, 255, 255, 0.11), rgba(255, 255, 255, 0.04) 34%, rgba(0, 0, 0, 0.16)), rgba(22, 24, 29, 0.62)`)
      root.style.setProperty('--bg-card', `rgba(28, 31, 38, 0.68)`)
      root.style.setProperty('--bg-card-hover', `rgba(33, 36, 43, 0.76)`)
      root.style.setProperty('--panel-bg', `rgba(28, 31, 38, 0.68)`)
      root.style.setProperty('--surface-1', `rgba(30, 33, 40, 0.68)`)
      root.style.setProperty('--workbench-control-bg', `rgba(28, 31, 38, 0.22)`)
      root.style.setProperty('--workbench-soft-bg', `rgba(22, 24, 29, 0.24)`)
      root.style.setProperty('--workbench-overlay-bg', `rgba(22, 24, 29, 0.92)`)
      root.style.setProperty('--theme-nav-alpha', '0.62')
      root.style.setProperty('--theme-surface-alpha', '0.068')
    } else {
      const glassOpacity = GLASS_OPACITY_LIGHT
      root.style.setProperty('--glass-bg-rgb', '255, 255, 255')
      root.style.setProperty('--glass-opacity', `${glassOpacity}`)
      root.style.setProperty('--glass-border-opacity', '0.08')
      root.style.setProperty('--glass-shadow-opacity', '0.04')
      root.style.setProperty('--glass-highlight-opacity', '0.14')
      root.style.setProperty('--primary-glow', `rgba(${r}, ${g}, ${b}, 0.14)`)
      root.style.setProperty('--primary-subtle', `rgba(${r}, ${g}, ${b}, 0.06)`)
      root.style.setProperty('--bg-tint', `rgba(${r}, ${g}, ${b}, 0.06)`)
      root.style.setProperty('--spotlight-color', `rgba(${r}, ${g}, ${b}, 0.1)`)
      root.style.setProperty('--spotlight-soft-color', `rgba(${r}, ${g}, ${b}, 0.06)`)
      root.style.setProperty('--spotlight-button-color', `rgba(0, 0, 0, 0.04)`)
      root.style.setProperty('--surface-2', `rgba(242, 242, 247, 0.8)`)
      root.style.setProperty('--surface-3', `rgba(0, 0, 0, 0.04)`)
      root.style.setProperty('--shadow-hover', `0 2px 8px rgba(0, 0, 0, 0.08), 0 0 0 1px rgba(${r}, ${g}, ${b}, 0.1)`)
      root.style.setProperty('--shadow-ring', `0 0 0 1px rgba(${r}, ${g}, ${b}, 0.14), 0 4px 16px rgba(0, 0, 0, 0.06)`)
      root.style.setProperty('--bg-input', `rgba(0, 0, 0, 0.04)`)
      root.style.setProperty('--border-color', `rgba(0, 0, 0, 0.08)`)
      root.style.setProperty('--border-strong', `rgba(0, 0, 0, 0.16)`)
      root.style.setProperty('--nav-bg', `rgba(255, 255, 255, 0.72)`)
      root.style.setProperty('--bg-card', `rgba(255, 255, 255, 0.72)`)
      root.style.setProperty('--bg-card-hover', `rgba(255, 255, 255, 0.86)`)
      root.style.setProperty('--panel-bg', `rgba(255, 255, 255, 0.72)`)
      root.style.setProperty('--surface-1', `rgba(255, 255, 255, 0.72)`)
      root.style.setProperty('--workbench-control-bg', `rgba(255, 255, 255, 0.5)`)
      root.style.setProperty('--workbench-soft-bg', `rgba(245, 245, 247, 0.5)`)
      root.style.setProperty('--workbench-overlay-bg', `rgba(245, 245, 247, 0.92)`)
      root.style.setProperty('--theme-nav-alpha', '0.72')
      root.style.setProperty('--theme-surface-alpha', '0.72')
    }

    const dynamicVars = [
      '--primary-color', '--primary-rgb', '--primary-color-rgb',
      '--glass-bg-rgb', '--primary-glow', '--primary-subtle',
      '--accent-glow', '--warning-glow', '--danger-glow', '--bg-tint',
      '--nav-bg', '--bg-card', '--bg-card-hover', '--panel-bg',
      '--surface-1', '--surface-2', '--surface-3', '--bg-input',
      '--border-color', '--border-strong', '--glass-opacity',
      '--glass-border-opacity', '--glass-shadow-opacity',
      '--spotlight-color', '--spotlight-soft-color', '--spotlight-button-color',
      '--shadow-hover', '--shadow-ring',
      '--workbench-control-bg', '--workbench-soft-bg', '--workbench-overlay-bg',
      '--glass-highlight-opacity',
      '--theme-nav-alpha', '--theme-surface-alpha',
    ]
    const mirrorTargets = [document.body, document.getElementById('app')].filter((target): target is HTMLElement => Boolean(target))
    mirrorTargets.forEach((target) => {
      dynamicVars.forEach((name) => {
        const value = root.style.getPropertyValue(name)
        if (value) target.style.setProperty(name, value)
      })
    })
  }

  const updateThemeClasses = () => {
    const root = document.documentElement
    if (isDarkMode.value) {
      root.classList.add('dark')
      root.classList.remove('light')
    } else {
      root.classList.add('light')
      root.classList.remove('dark')
    }
    root.dataset.palette = paletteId.value
  }

  const commitDarkMode = () => {
    updateThemeClasses()
    applyThemeVars()
    localStorage.setItem(STORAGE_KEYS.THEME_IS_DARK, String(isDarkMode.value))
  }

  const clearThemeTransition = () => {
    if (themeTransitionRaf !== null) {
      window.cancelAnimationFrame(themeTransitionRaf)
      themeTransitionRaf = null
    }
    if (themeTransitionEndTimer) {
      clearTimeout(themeTransitionEndTimer)
      themeTransitionEndTimer = null
    }
    if (themeTransition.value.active) {
      themeTransition.value = { ...themeTransition.value, active: false }
    }
    document.documentElement.classList.remove('theme-switching')
  }

  const animateDarkMode = (value: boolean, _origin: ThemeTransitionOrigin) => {
    clearThemeTransition()
    isDarkMode.value = value
    commitDarkMode()
  }

  const setPrimaryColor = (color: string) => {
    const next = palettes.find((item) => item.color.toLowerCase() === color.toLowerCase())
    setPalette(next?.id || 'aurora')
  }

  const setPalette = (id: PaletteId) => {
    const next = palettes.find((p) => p.id === id)
    if (!next) return
    paletteId.value = next.id
    updateThemeClasses()
    applyThemeVars()
    persistPalette()
  }

  const setDarkMode = (value: boolean, origin?: ThemeTransitionOrigin) => {
    if (origin) {
      animateDarkMode(value, origin)
      return
    }
    clearThemeTransition()
    isDarkMode.value = value
    commitDarkMode()
  }

  const toggleDarkMode = (origin?: ThemeTransitionOrigin) => {
    const next = !isDarkMode.value
    if (origin) {
      animateDarkMode(next, origin)
      return
    }
    clearThemeTransition()
    isDarkMode.value = next
    commitDarkMode()
  }

  const toggleDarkModeFromEvent = (_event: MouseEvent) => toggleDarkMode()

  const toggleLang = () => {
    lang.value = lang.value === 'zh' ? 'en' : 'zh'
    i18n.global.locale.value = lang.value
    localStorage.setItem(STORAGE_KEYS.LANG, lang.value)
  }

  const toggleMenuMode = () => {
    isHorizontalMenu.value = !isHorizontalMenu.value
    localStorage.setItem(STORAGE_KEYS.THEME_IS_HORIZONTAL, isHorizontalMenu.value.toString())
  }

  const curveBrightness = ref(parseFloat(localStorage.getItem(STORAGE_KEYS.CURVE_BRIGHTNESS) || '1.0'))
  const curveContrast = ref(parseFloat(localStorage.getItem(STORAGE_KEYS.CURVE_CONTRAST) || '1.0'))
  const curveSaturation = ref(parseFloat(localStorage.getItem(STORAGE_KEYS.CURVE_SATURATION) || '1.0'))

  const updateCurve = () => {
    localStorage.setItem(STORAGE_KEYS.CURVE_BRIGHTNESS, String(curveBrightness.value))
    localStorage.setItem(STORAGE_KEYS.CURVE_CONTRAST, String(curveContrast.value))
    localStorage.setItem(STORAGE_KEYS.CURVE_SATURATION, String(curveSaturation.value))
  }

  const resetCurve = () => {
    curveBrightness.value = 1.0
    curveContrast.value = 1.0
    curveSaturation.value = 1.0
    updateCurve()
  }

  const autoSyncEnabled = ref(localStorage.getItem(STORAGE_KEYS.AUTO_SYNC) !== 'false')
  let autoSyncTimer: ReturnType<typeof setInterval> | null = null

  const getBeijingHour = (): number => {
    const now = new Date()
    const beijingTime = new Date(now.getTime() + (8 * 60 + now.getTimezoneOffset()) * 60000)
    return beijingTime.getHours() + beijingTime.getMinutes() / 60
  }

  const applyAutoSync = () => {
    const hour = getBeijingHour()
    const dayPhase = Math.sin(((hour - 6) / 24) * Math.PI * 2)
    curveBrightness.value = Math.round((0.9 + (dayPhase + 1) * 0.08) * 100) / 100
    curveContrast.value = Math.round((0.92 + (dayPhase + 1) * 0.06) * 100) / 100
    curveSaturation.value = Math.round((0.86 + (dayPhase + 1) * 0.08) * 100) / 100
    updateCurve()
  }

  const toggleAutoSync = () => {
    autoSyncEnabled.value = !autoSyncEnabled.value
    localStorage.setItem(STORAGE_KEYS.AUTO_SYNC, String(autoSyncEnabled.value))
    if (autoSyncEnabled.value) {
      applyAutoSync()
      startAutoSync()
    } else {
      stopAutoSync()
    }
  }

  const startAutoSync = () => {
    if (autoSyncTimer) clearInterval(autoSyncTimer)
    if (!autoSyncEnabled.value) return
    applyAutoSync()
    autoSyncTimer = setInterval(applyAutoSync, 120000)
  }

  const stopAutoSync = () => {
    if (autoSyncTimer) {
      clearInterval(autoSyncTimer)
      autoSyncTimer = null
    }
  }

  const initTheme = () => {
    STALE_SKIN_KEYS.forEach((key) => localStorage.removeItem(key))
    updateThemeClasses()
    applyThemeVars()
    persistPalette()
    updateCurve()
    if (autoSyncEnabled.value) startAutoSync()
    i18n.global.locale.value = lang.value
  }

  return {
    paletteId,
    palette,
    palettes,
    primaryColor,
    isDarkMode,
    isHorizontalMenu,
    lang,
    themeTransition,
    curveBrightness,
    curveContrast,
    curveSaturation,
    autoSyncEnabled,
    setPalette,
    setPrimaryColor,
    setDarkMode,
    toggleDarkMode,
    toggleDarkModeFromEvent,
    toggleLang,
    toggleMenuMode,
    toggleAutoSync,
    updateCurve,
    resetCurve,
    clearThemeTransition,
    startAutoSync,
    stopAutoSync,
    initTheme,
  }
})
