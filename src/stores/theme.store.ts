import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import i18n from '@/i18n'
import { STORAGE_KEYS } from '@/constants'
import type { Lang } from '@/types/common'

export type PaletteId = 'aurora' | 'copper' | 'violet' | 'graphite' | 'rose' | 'amber' | 'indigo' | 'mint' | 'custom'

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
  { id: 'graphite', name: 'Sky', color: '#6ee7f9', rgb: [110, 231, 249], glow: '#cbd5e1', warning: '#a3e635', danger: '#fda4af' },
  { id: 'rose', name: 'Rose', color: '#fb7185', rgb: [251, 113, 133], glow: '#f0abfc', warning: '#fbbf24', danger: '#ef4444' },
  { id: 'amber', name: 'Amber', color: '#fbbf24', rgb: [251, 191, 36], glow: '#fb7185', warning: '#34d399', danger: '#f97316' },
  { id: 'indigo', name: 'Indigo', color: '#6366f1', rgb: [99, 102, 241], glow: '#38bdf8', warning: '#f59e0b', danger: '#fb7185' },
  { id: 'mint', name: 'Mint', color: '#2dd4bf', rgb: [45, 212, 191], glow: '#a7f3d0', warning: '#fde047', danger: '#f472b6' },
]

const GLASS_OPACITY = 0.68
const GLASS_OPACITY_LIGHT = 0.72
const STALE_SKIN_KEYS = ['theme-hue', 'theme-saturation', 'theme-lightness', 'theme-glass-opacity']
const DEFAULT_CUSTOM_HUE = 186

function clamp(value: number, min: number, max: number) {
  return Math.min(max, Math.max(min, value))
}

function normalizeHue(value: number | string | null) {
  const parsed = Number(value)
  if (!Number.isFinite(parsed)) return DEFAULT_CUSTOM_HUE
  return Math.round(clamp(parsed, 0, 360))
}

function hslToRgb(hue: number, saturation: number, lightness: number): [number, number, number] {
  const normalizedHue = (((hue % 360) + 360) % 360) / 360
  const q = lightness < 0.5 ? lightness * (1 + saturation) : lightness + saturation - lightness * saturation
  const p = 2 * lightness - q
  const convert = (offset: number) => {
    let t = normalizedHue + offset
    if (t < 0) t += 1
    if (t > 1) t -= 1
    if (t < 1 / 6) return p + (q - p) * 6 * t
    if (t < 1 / 2) return q
    if (t < 2 / 3) return p + (q - p) * (2 / 3 - t) * 6
    return p
  }
  return [
    Math.round(convert(1 / 3) * 255),
    Math.round(convert(0) * 255),
    Math.round(convert(-1 / 3) * 255),
  ]
}

function rgbToHex([r, g, b]: [number, number, number]) {
  return `#${[r, g, b].map((value) => value.toString(16).padStart(2, '0')).join('')}`
}

function buildCustomPalette(hue: number): Palette {
  const normalizedHue = Math.round(clamp(hue, 0, 360))
  const rgb = hslToRgb(normalizedHue, 0.72, 0.62)
  const glow = rgbToHex(hslToRgb((normalizedHue + 42) % 360, 0.78, 0.72))
  const warning = rgbToHex(hslToRgb((normalizedHue + 148) % 360, 0.78, 0.58))
  const danger = rgbToHex(hslToRgb((normalizedHue + 214) % 360, 0.78, 0.64))
  return {
    id: 'custom',
    name: 'Custom',
    color: rgbToHex(rgb),
    rgb,
    glow,
    warning,
    danger,
  }
}

export const useThemeStore = defineStore('theme', () => {
  const storedPalette = localStorage.getItem(STORAGE_KEYS.THEME_PALETTE) as PaletteId | null
  const customHue = ref(normalizeHue(localStorage.getItem(STORAGE_KEYS.THEME_CUSTOM_HUE)))
  const customPalette = computed(() => buildCustomPalette(customHue.value))
  const allPalettes = computed<Palette[]>(() => [...palettes, customPalette.value])
  const paletteId = ref<PaletteId>(allPalettes.value.some((p) => p.id === storedPalette) ? storedPalette! : 'aurora')
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

  const palette = computed(() => allPalettes.value.find((p) => p.id === paletteId.value) || palettes[0])
  const primaryColor = computed(() => palette.value.color)
  let themeTransitionRaf: number | null = null
  let themeTransitionEndTimer: number | null = null

  const persistPalette = () => {
    localStorage.setItem(STORAGE_KEYS.THEME_PRIMARY_COLOR, primaryColor.value)
    localStorage.setItem(STORAGE_KEYS.THEME_PALETTE, paletteId.value)
    localStorage.setItem(STORAGE_KEYS.THEME_CUSTOM_HUE, String(customHue.value))
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
      root.style.setProperty('--glass-bg-rgb', '250, 252, 255')
      root.style.setProperty('--glass-opacity', `${glassOpacity}`)
      root.style.setProperty('--glass-border-opacity', '0.12')
      root.style.setProperty('--glass-shadow-opacity', '0.07')
      root.style.setProperty('--glass-highlight-opacity', '0.2')
      root.style.setProperty('--primary-glow', `rgba(${r}, ${g}, ${b}, 0.1)`)
      root.style.setProperty('--primary-subtle', `rgba(${r}, ${g}, ${b}, 0.035)`)
      root.style.setProperty('--bg-tint', `rgba(${r}, ${g}, ${b}, 0.028)`)
      root.style.setProperty('--spotlight-color', `rgba(${r}, ${g}, ${b}, 0.055)`)
      root.style.setProperty('--spotlight-soft-color', `rgba(${r}, ${g}, ${b}, 0.038)`)
      root.style.setProperty('--spotlight-button-color', `rgba(15, 23, 42, 0.048)`)
      root.style.setProperty('--surface-2', `rgba(232, 238, 247, 0.74)`)
      root.style.setProperty('--surface-3', `rgba(15, 23, 42, 0.045)`)
      root.style.setProperty('--shadow-hover', `0 10px 30px rgba(30, 41, 59, 0.11), 0 0 0 1px rgba(${r}, ${g}, ${b}, 0.09)`)
      root.style.setProperty('--shadow-ring', `0 0 0 1px rgba(${r}, ${g}, ${b}, 0.12), 0 18px 44px rgba(30, 41, 59, 0.09)`)
      root.style.setProperty('--bg-input', `rgba(15, 23, 42, 0.045)`)
      root.style.setProperty('--border-color', `rgba(42, 55, 75, 0.12)`)
      root.style.setProperty('--border-strong', `rgba(42, 55, 75, 0.2)`)
      root.style.setProperty('--nav-bg', `linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(247, 249, 253, 0.78) 58%, rgba(232, 238, 247, 0.58)), linear-gradient(90deg, rgba(${r}, ${g}, ${b}, 0.035), transparent 34%, rgba(96, 165, 250, 0.07)), rgba(250, 252, 255, 0.8)`)
      root.style.setProperty('--bg-card', `rgba(250, 252, 255, 0.78)`)
      root.style.setProperty('--bg-card-hover', `rgba(255, 255, 255, 0.9)`)
      root.style.setProperty('--panel-bg', `rgba(250, 252, 255, 0.78)`)
      root.style.setProperty('--surface-1', `rgba(250, 252, 255, 0.78)`)
      root.style.setProperty('--workbench-control-bg', `rgba(248, 251, 255, 0.66)`)
      root.style.setProperty('--workbench-soft-bg', `rgba(232, 238, 247, 0.58)`)
      root.style.setProperty('--workbench-overlay-bg', `rgba(244, 247, 251, 0.94)`)
      root.style.setProperty('--theme-nav-alpha', '0.78')
      root.style.setProperty('--theme-surface-alpha', '0.74')
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
    const next = allPalettes.value.find((item) => item.color.toLowerCase() === color.toLowerCase())
    setPalette(next?.id || 'aurora')
  }

  const setPalette = (id: PaletteId) => {
    const next = allPalettes.value.find((p) => p.id === id)
    if (!next) return
    paletteId.value = next.id
    updateThemeClasses()
    applyThemeVars()
    persistPalette()
  }

  const setCustomHue = (value: number | string) => {
    customHue.value = normalizeHue(value)
    paletteId.value = 'custom'
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
    palettes: allPalettes,
    presetPalettes: palettes,
    customHue,
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
    setCustomHue,
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
