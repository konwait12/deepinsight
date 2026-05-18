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

function parseColor(color: string): [number, number, number] {
  if (color.startsWith('#')) {
    const hex = color.replace('#', '')
    return [
      parseInt(hex.substring(0, 2), 16),
      parseInt(hex.substring(2, 4), 16),
      parseInt(hex.substring(4, 6), 16),
    ]
  }

  const match = color.match(/hsl\((\d+),\s*(\d+)%,\s*(\d+)%\)/)
  if (!match) return [66, 230, 164]

  const hue = parseInt(match[1])
  const sat = parseInt(match[2]) / 100
  const light = parseInt(match[3]) / 100
  const a = sat * Math.min(light, 1 - light)
  const f = (n: number) => {
    const k = (n + hue / 30) % 12
    return light - a * Math.max(-1, Math.min(k - 3, 9 - k, 1))
  }
  return [Math.round(f(0) * 255), Math.round(f(8) * 255), Math.round(f(4) * 255)]
}

export const useThemeStore = defineStore('theme', () => {
  const storedPalette = localStorage.getItem(STORAGE_KEYS.THEME_PALETTE) as PaletteId | null
  const paletteId = ref<PaletteId>(palettes.some((p) => p.id === storedPalette) ? storedPalette! : 'aurora')
  const primaryColor = ref(palettes.find((p) => p.id === paletteId.value)!.color)
  const isDarkMode = ref(true)
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
  let themeTransitionRaf: number | null = null
  let themeTransitionEndTimer: number | null = null

  const applyThemeVars = () => {
    const [r, g, b] = palette.value.color === primaryColor.value ? palette.value.rgb : parseColor(primaryColor.value)
    const root = document.documentElement
    root.style.setProperty('--primary-color', primaryColor.value)
    root.style.setProperty('--primary-rgb', `${r}, ${g}, ${b}`)
    root.style.setProperty('--primary-color-rgb', `${r} ${g} ${b}`)
    root.style.setProperty('--primary-glow', `rgba(${r}, ${g}, ${b}, 0.34)`)
    root.style.setProperty('--primary-subtle', `rgba(${r}, ${g}, ${b}, 0.09)`)
    root.style.setProperty('--accent-glow', palette.value.glow)
    root.style.setProperty('--warning-glow', palette.value.warning)
    root.style.setProperty('--danger-glow', palette.value.danger)
    root.style.setProperty('--bg-tint', `rgba(${r}, ${g}, ${b}, 0.11)`)
  }

  const updateThemeClasses = () => {
    document.documentElement.classList.add('dark')
    document.documentElement.classList.remove('light')
    document.documentElement.dataset.palette = paletteId.value
  }

  const commitDarkMode = () => {
    isDarkMode.value = true
    updateThemeClasses()
    applyThemeVars()
    localStorage.setItem(STORAGE_KEYS.THEME_IS_DARK, 'true')
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

  const animateDarkMode = (_value: boolean, _origin: ThemeTransitionOrigin) => {
    clearThemeTransition()
    commitDarkMode()
  }

  const setPrimaryColor = (color: string) => {
    primaryColor.value = color
    applyThemeVars()
    localStorage.setItem(STORAGE_KEYS.THEME_PRIMARY_COLOR, color)
  }

  const setPalette = (id: PaletteId) => {
    const next = palettes.find((p) => p.id === id)
    if (!next) return
    paletteId.value = next.id
    primaryColor.value = next.color
    updateThemeClasses()
    applyThemeVars()
    localStorage.setItem(STORAGE_KEYS.THEME_PALETTE, next.id)
    localStorage.setItem(STORAGE_KEYS.THEME_PRIMARY_COLOR, next.color)
  }

  const setDarkMode = (_value: boolean, origin?: ThemeTransitionOrigin) => {
    if (origin) {
      animateDarkMode(true, origin)
      return
    }
    clearThemeTransition()
    commitDarkMode()
  }

  const toggleDarkMode = (origin?: ThemeTransitionOrigin) => {
    if (origin) {
      animateDarkMode(true, origin)
      return
    }
    clearThemeTransition()
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
    primaryColor.value = palette.value.color
    localStorage.setItem(STORAGE_KEYS.THEME_IS_DARK, 'true')
    localStorage.setItem(STORAGE_KEYS.THEME_PRIMARY_COLOR, palette.value.color)
    updateThemeClasses()
    applyThemeVars()
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
