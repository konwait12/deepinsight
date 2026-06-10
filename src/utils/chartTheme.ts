export type DeepChartTheme = {
  primary: string
  accent: string
  warning: string
  danger: string
  success: string
  muted: string
  text: string
  title: string
  border: string
  tooltipBg: string
  tooltipBorder: string
  palette: string[]
}

function cssVar(name: string, fallback: string) {
  if (typeof window === 'undefined') return fallback
  const value = getComputedStyle(document.documentElement).getPropertyValue(name).trim()
  return value || fallback
}

export function buildChartTheme(): DeepChartTheme {
  const primary = cssVar('--primary-color', '#42e6a4')
  const accent = cssVar('--accent-glow', '#60a5fa')
  const warning = cssVar('--warning-glow', '#f59e0b')
  const danger = cssVar('--danger-glow', '#fb7185')
  const text = cssVar('--text-secondary', '#94a3b8')
  const title = cssVar('--text-primary', '#f8fafc')
  const border = cssVar('--border-color', 'rgba(148, 163, 184, 0.22)')

  return {
    primary,
    accent,
    warning,
    danger,
    success: primary,
    muted: '#94a3b8',
    text,
    title,
    border,
    tooltipBg: cssVar('--workbench-overlay-bg', 'rgba(15, 23, 42, 0.94)'),
    tooltipBorder: border,
    palette: [primary, accent, warning, danger, '#14b8a6', '#64748b'],
  }
}

export function chartTooltip(theme = buildChartTheme()) {
  return {
    backgroundColor: theme.tooltipBg,
    borderColor: theme.tooltipBorder,
    borderWidth: 1,
    padding: [10, 12],
    textStyle: {
      color: theme.title,
      fontSize: 12,
      lineHeight: 18,
    },
    extraCssText: 'backdrop-filter: blur(14px); border-radius: 10px; box-shadow: 0 16px 42px rgba(0,0,0,.18);',
  }
}

export function chartAxis(theme = buildChartTheme()) {
  return {
    axisLine: { lineStyle: { color: theme.border } },
    axisTick: { show: false, lineStyle: { color: theme.border } },
    axisLabel: {
      color: theme.text,
      fontSize: 11,
      hideOverlap: true,
    },
    splitLine: {
      lineStyle: {
        color: theme.border,
        opacity: 0.42,
        type: 'dashed',
      },
    },
  }
}

export function chartLegend(theme = buildChartTheme()) {
  return {
    textStyle: { color: theme.text, fontSize: 11 },
    itemWidth: 10,
    itemHeight: 8,
    icon: 'roundRect',
  }
}

export function chartGrid(overrides: Record<string, unknown> = {}) {
  return {
    left: 52,
    right: 24,
    top: 42,
    bottom: 58,
    containLabel: true,
    ...overrides,
  }
}

export function chartAlpha(color: string, alpha: number) {
  const value = color.trim()
  const normalizedAlpha = Math.max(0, Math.min(1, alpha))
  const hex = value.replace('#', '')

  if (/^[0-9a-f]{3}$/i.test(hex)) {
    const [r, g, b] = hex.split('').map((part) => Number.parseInt(part + part, 16))
    return `rgba(${r}, ${g}, ${b}, ${normalizedAlpha})`
  }

  if (/^[0-9a-f]{6}$/i.test(hex)) {
    const r = Number.parseInt(hex.slice(0, 2), 16)
    const g = Number.parseInt(hex.slice(2, 4), 16)
    const b = Number.parseInt(hex.slice(4, 6), 16)
    return `rgba(${r}, ${g}, ${b}, ${normalizedAlpha})`
  }

  const rgb = value.match(/^rgba?\(([^)]+)\)$/i)
  if (rgb) {
    const [r, g, b] = rgb[1].split(',').slice(0, 3).map((part) => part.trim())
    return `rgba(${r}, ${g}, ${b}, ${normalizedAlpha})`
  }

  return value
}
