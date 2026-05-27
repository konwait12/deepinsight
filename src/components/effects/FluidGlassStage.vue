<template>
  <section
    ref="rootRef"
    class="fluid-glass-stage"
    @pointermove="handlePointerMove"
    @pointerleave="handlePointerLeave"
  >
    <div
      ref="stickyRef"
      class="fluid-sticky"
      :style="stageVars"
    >
      <canvas ref="canvasRef" class="fluid-canvas" aria-hidden="true"></canvas>

      <div class="fluid-signal-field" aria-hidden="true">
        <section
          v-for="curve in signalCurves"
          :key="curve.label"
          class="signal-curve"
          :class="curve.className"
        >
          <div class="signal-curve-label">
            <span>{{ curve.label }}</span>
            <strong>{{ curve.value }}</strong>
          </div>
          <svg viewBox="0 0 360 180" preserveAspectRatio="none">
            <path class="signal-grid-line" d="M0 42 H360 M0 92 H360 M0 142 H360" />
            <path class="signal-grid-line vertical" d="M48 0 V180 M132 0 V180 M216 0 V180 M300 0 V180" />
            <path
              v-if="curve.area"
              class="signal-area"
              :class="curve.tone"
              :d="curve.area"
            />
            <path class="signal-path" :class="curve.tone" :d="curve.path" />
            <path
              v-if="curve.ghost"
              class="signal-path ghost"
              :d="curve.ghost"
            />
            <circle
              v-for="(dot, dotIndex) in curve.dots"
              :key="`${curve.label}-${dotIndex}`"
              class="signal-dot"
              :class="dotIndex === 0 ? 'p1' : 'p2'"
              :cx="dot.x"
              :cy="dot.y"
              :r="dotIndex === 0 ? 5 : 4"
            />
          </svg>
        </section>
      </div>

      <div class="fluid-title fluid-title-front" aria-hidden="true">
        <strong>{{ sceneCopy.title }}</strong>
        <span>{{ sceneCopy.flow }}</span>
      </div>

      <div class="fluid-content fluid-content-base" aria-hidden="true">
        <div class="fluid-title">
          <strong>{{ sceneCopy.title }}</strong>
          <span>{{ sceneCopy.flow }}</span>
        </div>

        <article
          v-for="(item, index) in sceneCopy.modules"
          :key="item.title"
          class="fluid-card"
          :class="`card-${index + 1}`"
          :style="{ '--accent': item.accent, ...cardMotionVars(index) }"
          @pointerenter="handleCardPointerMove($event, index)"
          @pointermove="handleCardPointerMove($event, index)"
          @pointerdown="handleCardPointerDown($event, index)"
          @pointerup="handleCardPointerUp($event)"
          @pointercancel="handleCardPointerUp($event)"
          @pointerleave="handleCardPointerLeave"
        >
          <span>{{ item.tag }}</span>
          <h3>{{ item.title }}</h3>
          <div class="fluid-lines">
            <em v-for="line in item.lines" :key="line">{{ line }}</em>
          </div>
          <div class="fluid-chart">
            <i
              v-for="(bar, barIndex) in item.bars"
              :key="`${item.title}-${barIndex}`"
              :style="{ height: `${bar}%` }"
            ></i>
          </div>
          <div class="fluid-card-caption">{{ item.title }}</div>
        </article>
      </div>

      <div
        class="fluid-glass"
        :class="`glass-${mode}`"
        aria-hidden="true"
      >
        <div class="fluid-lens-scale">
          <div class="fluid-lens-field"></div>
          <div class="fluid-signal-field fluid-signal-field-lens">
            <section
              v-for="curve in signalCurves"
              :key="`${curve.label}-lens`"
              class="signal-curve"
              :class="curve.className"
            >
              <div class="signal-curve-label">
                <span>{{ curve.label }}</span>
                <strong>{{ curve.value }}</strong>
              </div>
              <svg viewBox="0 0 360 180" preserveAspectRatio="none">
                <path class="signal-grid-line" d="M0 42 H360 M0 92 H360 M0 142 H360" />
                <path class="signal-grid-line vertical" d="M48 0 V180 M132 0 V180 M216 0 V180 M300 0 V180" />
                <path
                  v-if="curve.area"
                  class="signal-area"
                  :class="curve.tone"
                  :d="curve.area"
                />
                <path class="signal-path" :class="curve.tone" :d="curve.path" />
                <path
                  v-if="curve.ghost"
                  class="signal-path ghost"
                  :d="curve.ghost"
                />
                <circle
                  v-for="(dot, dotIndex) in curve.dots"
                  :key="`${curve.label}-lens-${dotIndex}`"
                  class="signal-dot"
                  :class="dotIndex === 0 ? 'p1' : 'p2'"
                  :cx="dot.x"
                  :cy="dot.y"
                  :r="dotIndex === 0 ? 5 : 4"
                />
              </svg>
            </section>
          </div>
          <div class="fluid-content fluid-content-lens">
            <div class="fluid-title">
              <strong>{{ sceneCopy.title }}</strong>
              <span>{{ sceneCopy.flow }}</span>
            </div>

            <article
              v-for="(item, index) in sceneCopy.modules"
              :key="`${item.title}-lens`"
              class="fluid-card"
              :class="`card-${index + 1}`"
              :style="{ '--accent': item.accent, ...cardMotionVars(index) }"
            >
              <span>{{ item.tag }}</span>
              <h3>{{ item.title }}</h3>
              <div class="fluid-lines">
                <em v-for="line in item.lines" :key="line">{{ line }}</em>
              </div>
              <div class="fluid-chart">
                <i
                  v-for="(bar, barIndex) in item.bars"
                  :key="`${item.title}-lens-${barIndex}`"
                  :style="{ height: `${bar}%` }"
                ></i>
              </div>
              <div class="fluid-card-caption">{{ item.title }}</div>
            </article>
          </div>
        </div>
        <span class="fluid-lens-sheen"></span>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import * as THREE from 'three'
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'

type GlassMode = 'lens' | 'bar' | 'cube'

type GlassModeProps = {
  scale?: number
  ior?: number
  thickness?: number
  chromaticAberration?: number
  anisotropy?: number
  transmission?: number
  roughness?: number
}

type ModuleCard = {
  accent: string
  tag: string
  title: string
  lines: string[]
  bars: number[]
}

type SignalCurve = {
  className: string
  label: string
  value: string
  tone: string
  path: string
  area?: string
  ghost?: string
  dots: Array<{ x: number; y: number }>
}

const props = withDefaults(defineProps<{
  dark?: boolean
  mode?: GlassMode
  lensProps?: GlassModeProps
  barProps?: GlassModeProps & { navItems?: Array<{ label: string; link: string }> }
  cubeProps?: GlassModeProps
}>(), {
  dark: true,
  mode: 'lens',
})

const { locale } = useI18n()
const rootRef = ref<HTMLElement | null>(null)
const stickyRef = ref<HTMLElement | null>(null)
const canvasRef = ref<HTMLCanvasElement | null>(null)

const localeKey = computed(() => (locale.value.startsWith('en') ? 'en' : 'zh'))
const mode = computed(() => props.mode)
const scrollProgress = ref(0)
const contentProgress = ref(0)
const glassX = ref(0)
const glassY = ref(0)
const glassSize = ref(420)
const stageWidth = ref(1)
const stageHeight = ref(1)
const titleOpacity = ref(1)
const activeCardIndex = ref(-1)
const cardTiltX = ref(0)
const cardTiltY = ref(0)
const cardCaptionX = ref(0)
const cardCaptionY = ref(0)
const cardCaptionRotate = ref(0)
const cardDragOffsets = ref([
  { x: 0, y: 0 },
  { x: 0, y: 0 },
  { x: 0, y: 0 },
  { x: 0, y: 0 },
  { x: 0, y: 0 },
])
const draggingCardIndex = ref(-1)
const dragStart = { pointerX: 0, pointerY: 0, cardX: 0, cardY: 0 }
let lastCardOffsetY = 0

const sceneCopy = computed(() => {
  if (localeKey.value === 'en') {
    return {
      title: 'DeepInsight',
      flow: 'Training / Analysis / Knowledge / AI Review / Visualization',
      modules: [
        { accent: 'var(--primary-color)', tag: 'Capability 01', title: 'Training Control', lines: ['Task Queue', 'GPU Monitor', 'Metric Tracking'], bars: [36, 58, 82, 54, 88, 70, 46, 76] },
        { accent: 'var(--accent-glow)', tag: 'Capability 02', title: 'Model Analysis', lines: ['Curve Compare', 'Risk Review', 'Report Draft'], bars: [78, 48, 92, 66, 42, 84, 60, 72] },
        { accent: 'var(--warning-glow)', tag: 'Capability 03', title: 'Knowledge Base', lines: ['Dataset Link', 'Version Trace', 'Asset Index'], bars: [44, 76, 56, 90, 64, 52, 84, 62] },
        { accent: 'var(--danger-glow)', tag: 'Capability 04', title: 'AI Analysis', lines: ['State Summary', 'Risk Review', 'Experiment Plan'], bars: [86, 70, 48, 76, 58, 94, 66, 74] },
        { accent: 'var(--primary-color)', tag: 'Capability 05', title: 'Visual Analytics', lines: ['Run View', 'Signal Map', 'Quality Gate'], bars: [52, 88, 68, 44, 82, 60, 96, 72] },
      ] satisfies ModuleCard[],
    }
  }

  return {
    title: 'DeepInsight',
    flow: '训练 / 分析 / 知识 / AI 复盘 / 可视化',
    modules: [
      { accent: 'var(--primary-color)', tag: '功能 01', title: '训练调度', lines: ['任务队列', '资源监控', '指标追踪'], bars: [36, 58, 82, 54, 88, 70, 46, 76] },
      { accent: 'var(--accent-glow)', tag: '功能 02', title: '模型分析', lines: ['曲线对比', '风险复盘', '报告草稿'], bars: [78, 48, 92, 66, 42, 84, 60, 72] },
      { accent: 'var(--warning-glow)', tag: '功能 03', title: '知识库', lines: ['数据关联', '版本追踪', '资产索引'], bars: [44, 76, 56, 90, 64, 52, 84, 62] },
      { accent: 'var(--danger-glow)', tag: '功能 04', title: 'AI 分析', lines: ['状态总结', '风险复盘', '实验建议'], bars: [86, 70, 48, 76, 58, 94, 66, 74] },
      { accent: 'var(--primary-color)', tag: '功能 05', title: '可视化分析', lines: ['运行视图', '信号地图', '质量门禁'], bars: [52, 88, 68, 44, 82, 60, 96, 72] },
    ] satisfies ModuleCard[],
  }
})

const signalCurves: SignalCurve[] = [
  {
    className: 'signal-loss',
    label: 'loss(t)',
    value: '0.184',
    tone: 'primary',
    area: 'M0 52 C38 68 62 40 96 58 C128 75 146 116 184 104 C220 92 235 52 272 64 C304 74 326 42 360 36 V180 H0 Z',
    path: 'M0 52 C38 68 62 40 96 58 C128 75 146 116 184 104 C220 92 235 52 272 64 C304 74 326 42 360 36',
    dots: [{ x: 184, y: 104 }, { x: 272, y: 64 }],
  },
  {
    className: 'signal-accuracy',
    label: 'accuracy',
    value: '94.7%',
    tone: 'accent',
    area: 'M0 138 C34 128 58 132 92 116 C130 98 144 80 178 82 C220 84 242 54 282 46 C314 40 336 30 360 26 V180 H0 Z',
    path: 'M0 138 C34 128 58 132 92 116 C130 98 144 80 178 82 C220 84 242 54 282 46 C314 40 336 30 360 26',
    dots: [{ x: 178, y: 82 }, { x: 282, y: 46 }],
  },
  {
    className: 'signal-scheduler',
    label: 'lr schedule',
    value: '3e-4',
    tone: 'warning',
    path: 'M0 126 L56 126 L58 58 L118 58 L120 88 L178 88 L180 48 L238 48 L240 112 L300 112 L302 70 L360 70',
    ghost: 'M0 146 C48 142 92 138 132 126 C178 112 204 96 246 100 C292 104 320 88 360 84',
    dots: [{ x: 180, y: 48 }, { x: 302, y: 70 }],
  },
  {
    className: 'signal-gpu',
    label: 'gpu load',
    value: '87%',
    tone: 'danger',
    area: 'M0 116 C28 66 54 82 84 74 C124 64 140 128 176 104 C214 78 232 90 260 68 C302 34 330 58 360 46 V180 H0 Z',
    path: 'M0 116 C28 66 54 82 84 74 C124 64 140 128 176 104 C214 78 232 90 260 68 C302 34 330 58 360 46',
    dots: [{ x: 176, y: 104 }, { x: 260, y: 68 }],
  },
  {
    className: 'signal-grad',
    label: 'grad norm',
    value: '1.32',
    tone: 'primary',
    path: 'M0 96 C32 102 54 42 82 70 C110 98 134 96 160 72 C192 42 204 150 238 118 C272 86 288 92 318 68 C338 52 348 58 360 50',
    ghost: 'M0 124 C46 120 72 108 112 112 C168 118 194 132 238 118 C292 102 314 86 360 92',
    dots: [{ x: 204, y: 150 }, { x: 318, y: 68 }],
  },
  {
    className: 'signal-throughput',
    label: 'throughput',
    value: '412/s',
    tone: 'accent',
    area: 'M0 142 C36 134 58 126 92 124 C132 122 152 92 188 96 C228 100 242 62 284 64 C322 66 338 46 360 42 V180 H0 Z',
    path: 'M0 142 C36 134 58 126 92 124 C132 122 152 92 188 96 C228 100 242 62 284 64 C322 66 338 46 360 42',
    dots: [{ x: 188, y: 96 }, { x: 284, y: 64 }],
  },
  {
    className: 'signal-f1',
    label: 'val f1',
    value: '0.913',
    tone: 'warning',
    area: 'M0 128 C30 118 56 124 84 100 C118 72 144 76 176 86 C214 98 238 70 270 52 C310 30 334 38 360 28 V180 H0 Z',
    path: 'M0 128 C30 118 56 124 84 100 C118 72 144 76 176 86 C214 98 238 70 270 52 C310 30 334 38 360 28',
    dots: [{ x: 176, y: 86 }, { x: 270, y: 52 }],
  },
]

const stageVars = computed<Record<string, string>>(() => ({
  '--progress': scrollProgress.value.toFixed(4),
  '--content-progress': contentProgress.value.toFixed(4),
  '--glass-x': `${glassX.value.toFixed(1)}px`,
  '--glass-y': `${glassY.value.toFixed(1)}px`,
  '--glass-size': `${glassSize.value.toFixed(1)}px`,
  '--stage-width': `${stageWidth.value.toFixed(1)}px`,
  '--stage-height': `${stageHeight.value.toFixed(1)}px`,
  '--lens-zoom': props.mode === 'bar' ? '1.02' : props.mode === 'cube' ? '1.04' : '1.07',
  '--title-opacity': titleOpacity.value.toFixed(3),
  '--lens-active': activeCardIndex.value >= 0 ? '1' : '0',
  '--lens-hover-zoom': activeCardIndex.value >= 0 ? '0.68' : '0.9',
  '--lens-total-zoom': activeCardIndex.value >= 0
    ? (props.mode === 'bar' ? '0.68' : props.mode === 'cube' ? '0.69' : '0.71')
    : (props.mode === 'bar' ? '0.92' : props.mode === 'cube' ? '0.94' : '0.96'),
}))

const cardMotions = [
  { x: -210, y: 150, rotate: -7 },
  { x: 190, y: 230, rotate: 5 },
  { x: 150, y: -150, rotate: 5 },
  { x: -160, y: -240, rotate: -5 },
  { x: 230, y: -320, rotate: 7 },
]

function cardMotionVars(index: number): Record<string, string> {
  const motion = cardMotions[index] ?? cardMotions[0]
  const drag = cardDragOffsets.value[index] ?? { x: 0, y: 0 }
  const progress = scrollProgress.value - 0.5
  const isActive = activeCardIndex.value === index
  const isDragging = draggingCardIndex.value === index
  return {
    '--motion-x': `${(progress * motion.x).toFixed(1)}px`,
    '--motion-y': `${(progress * motion.y).toFixed(1)}px`,
    '--drag-x': `${drag.x.toFixed(1)}px`,
    '--drag-y': `${drag.y.toFixed(1)}px`,
    '--motion-rotate': `${(progress * motion.rotate).toFixed(2)}deg`,
    '--tilt-x': `${isActive ? cardTiltX.value.toFixed(2) : '0'}deg`,
    '--tilt-y': `${isActive ? cardTiltY.value.toFixed(2) : '0'}deg`,
    '--hover-scale': isActive ? '1.06' : '1',
    '--hover-lift': isActive ? '-12px' : '0px',
    '--hover-glow': isActive ? '1' : '0',
    '--dragging': isDragging ? '1' : '0',
    '--caption-x': `${isActive ? cardCaptionX.value.toFixed(1) : '0'}px`,
    '--caption-y': `${isActive ? cardCaptionY.value.toFixed(1) : '0'}px`,
    '--caption-rotate': `${isActive ? cardCaptionRotate.value.toFixed(2) : '0'}deg`,
    '--caption-opacity': isActive ? '1' : '0',
  }
}

let renderer: THREE.WebGLRenderer | null = null
let scene: THREE.Scene | null = null
let camera: THREE.OrthographicCamera | null = null
let backgroundMaterial: THREE.ShaderMaterial | null = null
let resizeObserver: ResizeObserver | null = null
let rafId = 0
let width = 1
let height = 1
let targetPointerX = 0.5
let targetPointerY = 0.46
let pointerX = 0.5
let pointerY = 0.46
let reducedMotion = false
const clock = new THREE.Clock()

function clamp(value: number, min = 0, max = 1) {
  return Math.max(min, Math.min(max, value))
}

function getModeProps() {
  if (props.mode === 'bar') return props.barProps ?? {}
  if (props.mode === 'cube') return props.cubeProps ?? {}
  return props.lensProps ?? {}
}

function readCssColor(name: string, fallback: string) {
  const value = getComputedStyle(document.documentElement).getPropertyValue(name).trim()
  return value || fallback
}

function syncThemeColors() {
  if (!backgroundMaterial) return
  backgroundMaterial.uniforms.uPrimary.value.set(readCssColor('--primary-color', '#42e6a4'))
  backgroundMaterial.uniforms.uAccent.value.set(readCssColor('--accent-glow', '#7dd3fc'))
}

function setupBackground() {
  const canvas = canvasRef.value
  if (!canvas) return false

  renderer = new THREE.WebGLRenderer({
    canvas,
    alpha: true,
    antialias: false,
    powerPreference: 'high-performance',
  })
  renderer.outputColorSpace = THREE.SRGBColorSpace
  renderer.setClearColor(0x05070a, 0)

  scene = new THREE.Scene()
  camera = new THREE.OrthographicCamera(-1, 1, 1, -1, 0, 1)
  backgroundMaterial = new THREE.ShaderMaterial({
    uniforms: {
      uTime: { value: 0 },
      uProgress: { value: 0 },
      uPointer: { value: new THREE.Vector2(0.5, 0.46) },
      uPrimary: { value: new THREE.Color('#42e6a4') },
      uAccent: { value: new THREE.Color('#7dd3fc') },
      uDark: { value: props.dark ? 1 : 0 },
    },
    vertexShader: `
      varying vec2 vUv;
      void main() {
        vUv = uv;
        gl_Position = vec4(position.xy, 0.0, 1.0);
      }
    `,
    fragmentShader: `
      precision highp float;
      varying vec2 vUv;
      uniform float uTime;
      uniform float uProgress;
      uniform float uDark;
      uniform vec2 uPointer;
      uniform vec3 uPrimary;
      uniform vec3 uAccent;

      float grid(vec2 uv, float scale) {
        vec2 g = abs(fract(uv * scale - 0.5) - 0.5) / fwidth(uv * scale);
        return 1.0 - min(min(g.x, g.y), 1.0);
      }

      void main() {
        vec2 uv = vUv;
        vec3 top = mix(vec3(0.030, 0.145, 0.108), vec3(0.78, 0.92, 0.84), 1.0 - uDark);
        vec3 bottom = mix(vec3(0.020, 0.096, 0.118), vec3(0.56, 0.72, 0.64), 1.0 - uDark);
        vec3 color = mix(bottom, top, uv.y);

        float g = grid(uv + vec2(uProgress * 0.06, -uTime * 0.01), 22.0);
        float g2 = grid(uv + vec2(-uProgress * 0.03, uTime * 0.006), 7.0);
        float pointerGlow = smoothstep(0.62, 0.0, distance(uv, uPointer));
        float leftGlow = smoothstep(0.76, 0.0, distance(uv, vec2(0.18, 0.24 + uProgress * 0.22)));
        float rightGlow = smoothstep(0.84, 0.0, distance(uv, vec2(0.82, 0.32)));

        color += uPrimary * leftGlow * 0.34;
        color += uAccent * rightGlow * 0.32;
        color += mix(uPrimary, vec3(1.0), 0.22) * pointerGlow * 0.16;
        color += mix(uPrimary, uAccent, 0.22) * g * 0.055;
        color += uAccent * g2 * 0.028;
        color *= 1.0 - smoothstep(0.54, 1.02, distance(uv, vec2(0.5))) * 0.26;

        gl_FragColor = vec4(color, 1.0);
      }
    `,
    depthWrite: false,
    depthTest: false,
  })

  scene.add(new THREE.Mesh(new THREE.PlaneGeometry(2, 2), backgroundMaterial))
  return true
}

function resize() {
  const sticky = stickyRef.value
  if (!sticky || !renderer) return

  const rect = sticky.getBoundingClientRect()
  width = Math.max(1, Math.round(rect.width))
  height = Math.max(1, Math.round(rect.height))
  stageWidth.value = width
  stageHeight.value = height
  const dpr = Math.min(window.devicePixelRatio || 1, 1.6)
  renderer.setPixelRatio(dpr)
  renderer.setSize(width, height, false)
  updateGlassSize()
  syncThemeColors()
}

function updateGlassSize() {
  const scale = getModeProps().scale ?? 0.25
  if (props.mode === 'bar') {
    glassSize.value = Math.min(width * 0.74, 980) * (scale / 0.25)
    return
  }
  if (props.mode === 'cube') {
    glassSize.value = Math.max(170, Math.min(width * 0.18, 280)) * (scale / 0.25)
    return
  }
  glassSize.value = Math.max(190, Math.min(width * 0.18, 320)) * (scale / 0.25)
}

function updateScrollProgress() {
  const root = rootRef.value
  if (!root) return

  const rootRect = root.getBoundingClientRect()
  const travel = rootRect.height + window.innerHeight
  scrollProgress.value = travel > 2 ? clamp((window.innerHeight - rootRect.top) / travel) : 0
  contentProgress.value = clamp(scrollProgress.value)
  titleOpacity.value = 0.72
}

function handlePointerMove(event: PointerEvent) {
  const sticky = stickyRef.value
  if (!sticky) return
  const rect = sticky.getBoundingClientRect()
  targetPointerX = clamp((event.clientX - rect.left) / Math.max(rect.width, 1))
  targetPointerY = clamp((event.clientY - rect.top) / Math.max(rect.height, 1))
}

function handlePointerLeave() {
  targetPointerX = 0.5
  targetPointerY = props.mode === 'bar' ? 0.88 : 0.46
  handleCardPointerLeave()
}

function handleCardPointerMove(event: PointerEvent, index: number) {
  const target = event.currentTarget as HTMLElement | null
  if (!target) return

  if (draggingCardIndex.value === index) {
    updateCardDrag(event, target, index)
  }

  const rect = target.getBoundingClientRect()
  const offsetX = event.clientX - rect.left - rect.width / 2
  const offsetY = event.clientY - rect.top - rect.height / 2
  const rotateAmplitude = 12
  const nextTiltX = (offsetY / Math.max(rect.height / 2, 1)) * -rotateAmplitude
  const nextTiltY = (offsetX / Math.max(rect.width / 2, 1)) * rotateAmplitude

  activeCardIndex.value = index
  cardTiltX.value += (nextTiltX - cardTiltX.value) * 0.36
  cardTiltY.value += (nextTiltY - cardTiltY.value) * 0.36
  cardCaptionX.value = event.clientX - rect.left + 14
  cardCaptionY.value = event.clientY - rect.top + 12
  cardCaptionRotate.value = clamp((offsetY - lastCardOffsetY) * -0.45, -16, 16)
  lastCardOffsetY = offsetY

  const sticky = stickyRef.value
  if (sticky) {
    const stickyRect = sticky.getBoundingClientRect()
    targetPointerX = clamp((event.clientX - stickyRect.left) / Math.max(stickyRect.width, 1))
    targetPointerY = clamp((event.clientY - stickyRect.top) / Math.max(stickyRect.height, 1))
  }
}

function updateCardDrag(event: PointerEvent, target: HTMLElement, index: number) {
  const root = rootRef.value
  if (!root) return

  const rootRect = root.getBoundingClientRect()
  const cardRect = target.getBoundingClientRect()
  const rawX = dragStart.cardX + event.clientX - dragStart.pointerX
  const rawY = dragStart.cardY + event.clientY - dragStart.pointerY
  const margin = 18
  const minX = rootRect.left + margin - (cardRect.left - dragStart.cardX)
  const maxX = rootRect.right - margin - cardRect.width - (cardRect.left - dragStart.cardX)
  const minY = rootRect.top + margin - (cardRect.top - dragStart.cardY)
  const maxY = rootRect.bottom - margin - cardRect.height - (cardRect.top - dragStart.cardY)

  const next = [...cardDragOffsets.value]
  next[index] = {
    x: clamp(rawX, minX, maxX),
    y: clamp(rawY, minY, maxY),
  }
  cardDragOffsets.value = next
}

function handleCardPointerDown(event: PointerEvent, index: number) {
  const target = event.currentTarget as HTMLElement | null
  if (!target) return

  activeCardIndex.value = index
  draggingCardIndex.value = index
  dragStart.pointerX = event.clientX
  dragStart.pointerY = event.clientY
  dragStart.cardX = cardDragOffsets.value[index]?.x ?? 0
  dragStart.cardY = cardDragOffsets.value[index]?.y ?? 0
  target.setPointerCapture?.(event.pointerId)
}

function handleCardPointerUp(event: PointerEvent) {
  const target = event.currentTarget as HTMLElement | null
  target?.releasePointerCapture?.(event.pointerId)
  draggingCardIndex.value = -1
}

function handleCardPointerLeave() {
  if (draggingCardIndex.value >= 0) return
  activeCardIndex.value = -1
  cardTiltX.value = 0
  cardTiltY.value = 0
  cardCaptionRotate.value = 0
  lastCardOffsetY = 0
}

function tick() {
  rafId = window.requestAnimationFrame(tick)
  if (!renderer || !scene || !camera || !backgroundMaterial || document.hidden) return

  updateScrollProgress()
  pointerX += (targetPointerX - pointerX) * 0.12
  pointerY += (targetPointerY - pointerY) * 0.12

  if (props.mode === 'bar') {
    glassX.value = width / 2
    glassY.value = height - 72
  } else {
    glassX.value = pointerX * width
    glassY.value = pointerY * height
  }

  const time = reducedMotion ? 0 : clock.getElapsedTime()
  backgroundMaterial.uniforms.uTime.value = time
  backgroundMaterial.uniforms.uProgress.value = scrollProgress.value
  backgroundMaterial.uniforms.uPointer.value.set(pointerX, 1 - pointerY)
  backgroundMaterial.uniforms.uDark.value = props.dark ? 1 : 0
  syncThemeColors()
  renderer.render(scene, camera)
}

watch(() => [props.mode, props.lensProps, props.barProps, props.cubeProps], () => {
  updateGlassSize()
  handlePointerLeave()
}, { deep: true })

onMounted(() => {
  reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  if (!setupBackground()) return

  resize()
  handlePointerLeave()
  resizeObserver = new ResizeObserver(resize)
  if (stickyRef.value) resizeObserver.observe(stickyRef.value)
  window.addEventListener('scroll', updateScrollProgress, { passive: true })
  window.addEventListener('resize', resize, { passive: true })
  rafId = window.requestAnimationFrame(tick)
})

onUnmounted(() => {
  window.cancelAnimationFrame(rafId)
  window.removeEventListener('scroll', updateScrollProgress)
  window.removeEventListener('resize', resize)
  resizeObserver?.disconnect()
  backgroundMaterial?.dispose()
  scene?.traverse((object) => {
    if (object instanceof THREE.Mesh) {
      object.geometry.dispose()
    }
  })
  renderer?.dispose()
  renderer = null
  scene = null
  camera = null
  backgroundMaterial = null
})
</script>

<style scoped>
.fluid-glass-stage {
  position: relative;
  min-height: clamp(1780px, 258svh, 2520px);
  background:
    radial-gradient(1280px circle at 18% 24%, rgba(var(--primary-rgb), 0.28), transparent 58%),
    radial-gradient(1080px circle at 82% 30%, color-mix(in srgb, var(--accent-glow) 28%, transparent), transparent 62%),
    linear-gradient(
      180deg,
      color-mix(in srgb, var(--primary-color) 18%, var(--bg-color)) 0%,
      color-mix(in srgb, var(--bg-color) 74%, rgba(var(--primary-rgb), 0.2)) 48%,
      color-mix(in srgb, var(--bg-color) 68%, var(--surface-1) 32%) 100%
    );
  isolation: isolate;
}

.fluid-sticky {
  position: relative;
  top: 0;
  min-height: inherit;
  height: 100%;
  overflow: hidden;
  background:
    radial-gradient(1280px circle at 18% 24%, rgba(var(--primary-rgb), 0.28), transparent 58%),
    radial-gradient(1080px circle at 82% 30%, color-mix(in srgb, var(--accent-glow) 28%, transparent), transparent 62%),
    linear-gradient(
      180deg,
      color-mix(in srgb, var(--primary-color) 18%, var(--bg-color)) 0%,
      color-mix(in srgb, var(--bg-color) 74%, rgba(var(--primary-rgb), 0.2)) 48%,
      color-mix(in srgb, var(--bg-color) 68%, var(--surface-1) 32%) 100%
    );
}

.fluid-canvas,
.fluid-content,
.fluid-glass {
  position: absolute;
}

.fluid-canvas {
  inset: 0;
  z-index: 0;
  width: 100%;
  height: 100%;
  display: block;
}

.fluid-signal-field {
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;
  opacity: 0.7;
}

.fluid-signal-field-lens {
  left: calc(var(--glass-size) / 2 - var(--glass-x));
  top: calc(var(--glass-size) / 2 - var(--glass-y));
  right: auto;
  bottom: auto;
  width: var(--stage-width);
  height: var(--stage-height);
  z-index: 1;
  opacity: 0.98;
}

.signal-curve {
  position: absolute;
  width: min(29vw, 410px);
  height: 204px;
  padding: 16px;
  border: 1px solid color-mix(in srgb, var(--primary-color) 18%, transparent);
  border-radius: 24px;
  background:
    radial-gradient(280px circle at 28% 20%, rgba(var(--primary-rgb), 0.18), transparent 60%),
    rgba(var(--glass-bg-rgb), 0.22);
  box-shadow:
    0 28px 80px rgba(0, 0, 0, 0.18),
    inset 0 1px 0 rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(10px) saturate(130%);
  overflow: hidden;
  animation: signal-float 8s ease-in-out infinite;
}

.signal-loss {
  left: 43vw;
  top: 26vh;
}

.signal-accuracy {
  right: 5vw;
  top: 94vh;
  animation-delay: -2.4s;
}

.signal-scheduler {
  left: 24vw;
  top: 178vh;
  animation-delay: -4.8s;
}

.signal-gpu {
  left: 6vw;
  top: 72vh;
  animation-delay: -1.1s;
}

.signal-grad {
  right: 9vw;
  top: 144vh;
  animation-delay: -3.7s;
}

.signal-throughput {
  left: 48vw;
  top: 206vh;
  animation-delay: -5.8s;
}

.signal-f1 {
  left: 7vw;
  top: 218vh;
  animation-delay: -6.9s;
}

.signal-curve-label {
  position: relative;
  z-index: 2;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  color: color-mix(in srgb, var(--text-secondary) 84%, var(--primary-color));
  font-size: 11px;
  line-height: 1;
  font-weight: var(--font-weight-title);
  text-transform: uppercase;
}

.signal-curve-label strong {
  color: var(--text-primary);
  font-family: "JetBrains Mono", Consolas, monospace;
  font-size: 13px;
}

.signal-curve svg {
  position: absolute;
  inset: 40px 14px 12px;
  width: calc(100% - 28px);
  height: calc(100% - 52px);
  overflow: visible;
}

.signal-grid-line {
  fill: none;
  stroke: color-mix(in srgb, var(--border-color) 54%, transparent);
  stroke-width: 1;
  vector-effect: non-scaling-stroke;
}

.signal-grid-line.vertical {
  opacity: 0.58;
}

.signal-area {
  fill: color-mix(in srgb, var(--primary-color) 16%, transparent);
  opacity: 0.74;
  animation: signal-area-breathe 5.4s ease-in-out infinite;
}

.signal-area.accent {
  fill: color-mix(in srgb, var(--accent-glow) 18%, transparent);
}

.signal-area.warning {
  fill: color-mix(in srgb, var(--warning-glow) 18%, transparent);
}

.signal-area.danger {
  fill: color-mix(in srgb, var(--danger-glow) 17%, transparent);
}

.signal-path {
  fill: none;
  stroke: var(--primary-color);
  stroke-width: 3;
  stroke-linecap: round;
  stroke-linejoin: round;
  vector-effect: non-scaling-stroke;
  stroke-dasharray: 520;
  stroke-dashoffset: 520;
  filter: drop-shadow(0 0 14px rgba(var(--primary-rgb), 0.42));
  animation: signal-draw 4.8s ease-in-out infinite;
}

.signal-path.accent {
  stroke: var(--accent-glow);
  filter: drop-shadow(0 0 14px color-mix(in srgb, var(--accent-glow) 48%, transparent));
  animation-delay: -1.2s;
}

.signal-path.warning {
  stroke: var(--warning-glow);
  filter: drop-shadow(0 0 14px color-mix(in srgb, var(--warning-glow) 44%, transparent));
  animation-duration: 5.6s;
}

.signal-path.danger {
  stroke: var(--danger-glow);
  filter: drop-shadow(0 0 14px color-mix(in srgb, var(--danger-glow) 42%, transparent));
  animation-delay: -0.8s;
  animation-duration: 5.2s;
}

.signal-path.ghost {
  stroke: color-mix(in srgb, var(--danger-glow) 68%, var(--text-secondary));
  stroke-width: 2;
  opacity: 0.58;
  animation-delay: -2.1s;
}

.signal-dot {
  fill: #ffffff;
  stroke: var(--primary-color);
  stroke-width: 2;
  filter: drop-shadow(0 0 12px rgba(var(--primary-rgb), 0.7));
  animation: signal-dot-pulse 2.4s ease-in-out infinite;
}

.signal-dot.p2 {
  animation-delay: 680ms;
}

.fluid-content {
  inset: 0;
  z-index: 1;
  height: 100%;
  transform: translate3d(0, calc(var(--content-progress) * -10vh), 0);
  will-change: transform;
  perspective: 1100px;
}

.fluid-content-base {
  pointer-events: auto;
}

.fluid-content-base > .fluid-title,
.fluid-content-lens .fluid-title {
  visibility: hidden;
}

.fluid-title {
  position: absolute;
  left: 50%;
  top: 8vh;
  width: min(86vw, 1280px);
  transform: translateX(-50%) scale(calc(0.9 + var(--progress) * 0.04));
  transform-origin: center top;
  text-align: center;
  opacity: var(--title-opacity);
}

.fluid-title-front {
  z-index: 4;
  pointer-events: none;
}

.fluid-title strong {
  display: block;
  color: var(--text-primary);
  font-size: clamp(42px, 6.4vw, 108px);
  line-height: 0.88;
  font-weight: var(--font-weight-title);
  letter-spacing: 0;
  text-shadow: 0 0 30px rgba(var(--primary-rgb), 0.24);
}

.fluid-title span {
  display: block;
  margin-top: 16px;
  color: color-mix(in srgb, var(--text-secondary) 78%, var(--primary-color));
  font-size: clamp(12px, 1.1vw, 18px);
  font-weight: var(--font-weight-label);
}

.fluid-card {
  --accent: var(--primary-color);
  --motion-x: 0px;
  --motion-y: 0px;
  --drag-x: 0px;
  --drag-y: 0px;
  --motion-rotate: 0deg;
  --tilt-x: 0deg;
  --tilt-y: 0deg;
  --hover-scale: 1;
  --hover-lift: 0px;
  --hover-glow: 0;
  --caption-x: 0px;
  --caption-y: 0px;
  --caption-rotate: 0deg;
  --caption-opacity: 0;
  --dragging: 0;
  --depth: 0px;
  position: absolute;
  overflow: hidden;
  border: 1px solid color-mix(in srgb, var(--accent) 42%, var(--border-color));
  border-radius: 28px;
  padding: clamp(18px, 2vw, 28px);
  color: var(--text-primary);
  background:
    radial-gradient(360px circle at 22% 16%, color-mix(in srgb, var(--accent) 34%, transparent), transparent 58%),
    linear-gradient(145deg, rgba(255, 255, 255, 0.13), transparent 42%, rgba(255, 255, 255, 0.08)),
    rgba(var(--glass-bg-rgb), 0.68);
  box-shadow:
    0 30px 90px rgba(0, 0, 0, var(--glass-shadow-opacity)),
    inset 0 1px 0 rgba(255, 255, 255, 0.18);
  backdrop-filter: blur(18px) saturate(150%);
  cursor: pointer;
  user-select: none;
  touch-action: none;
  transform:
    translate3d(
      calc(var(--motion-x) + var(--drag-x)),
      calc(var(--motion-y) + var(--drag-y) + var(--hover-lift)),
      var(--depth)
    )
    rotate(var(--motion-rotate))
    rotateX(var(--tilt-x))
    rotateY(var(--tilt-y))
    scale(var(--hover-scale));
  transform-origin: center;
  transform-style: preserve-3d;
  transition:
    border-color 240ms ease,
    box-shadow 240ms ease,
    background 240ms ease,
    transform calc(140ms - var(--dragging) * 140ms) ease-out;
  will-change: transform, box-shadow;
}

.fluid-card::before {
  content: "";
  position: absolute;
  inset: 0;
  pointer-events: none;
  background-image:
    linear-gradient(90deg, color-mix(in srgb, var(--accent) 18%, transparent) 1px, transparent 1px),
    linear-gradient(0deg, color-mix(in srgb, var(--border-color) 62%, transparent) 1px, transparent 1px);
  background-size: 34px 34px;
  mask-image: radial-gradient(circle at calc(var(--caption-x) + 20px) calc(var(--caption-y) + 20px), black, transparent 78%);
  opacity: calc(0.7 + var(--hover-glow) * 0.3);
  transform: translateZ(22px);
}

.fluid-card::after {
  content: "";
  position: absolute;
  inset: -1px;
  pointer-events: none;
  border-radius: inherit;
  background:
    radial-gradient(300px circle at var(--caption-x) var(--caption-y), color-mix(in srgb, var(--accent) 36%, transparent), transparent 58%),
    linear-gradient(115deg, transparent 18%, rgba(255, 255, 255, calc(0.08 + var(--hover-glow) * 0.24)) 38%, transparent 58%);
  opacity: var(--hover-glow);
  transform: translateZ(42px);
  transition: opacity 220ms ease;
}

.fluid-card > * {
  position: relative;
  z-index: 1;
  transform: translateZ(28px);
}

.fluid-card:hover {
  z-index: 5;
  border-color: color-mix(in srgb, var(--accent) 78%, rgba(255, 255, 255, 0.38));
  box-shadow:
    0 38px 110px rgba(0, 0, 0, calc(var(--glass-shadow-opacity) + 0.12)),
    0 0 58px color-mix(in srgb, var(--accent) 28%, transparent),
    inset 0 1px 0 rgba(255, 255, 255, 0.28);
}

.fluid-card:active {
  cursor: grabbing;
}

.fluid-card span {
  display: block;
  color: color-mix(in srgb, var(--accent) 78%, var(--text-primary));
  font-size: 12px;
  line-height: 1;
  font-weight: var(--font-weight-title);
}

.fluid-card h3 {
  margin: 16px 0 0;
  color: var(--text-primary);
  font-size: clamp(28px, 3vw, 48px);
  line-height: 1.02;
  font-weight: var(--font-weight-title);
}

.fluid-lines {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 20px;
}

.fluid-lines em {
  border: 1px solid color-mix(in srgb, var(--accent) 26%, transparent);
  border-radius: 999px;
  padding: 7px 10px;
  color: color-mix(in srgb, var(--text-secondary) 82%, var(--accent));
  background: rgba(var(--glass-bg-rgb), 0.2);
  font-size: 12px;
  font-style: normal;
  font-weight: var(--font-weight-label);
}

.fluid-chart {
  position: absolute;
  left: clamp(18px, 2vw, 28px);
  right: clamp(18px, 2vw, 28px);
  bottom: clamp(18px, 2vw, 28px);
  height: 28%;
  display: flex;
  align-items: end;
  gap: 10px;
}

.fluid-chart i {
  flex: 1;
  min-width: 6px;
  border-radius: 999px 999px 4px 4px;
  background: linear-gradient(180deg, #ffffff, var(--accent));
  box-shadow: 0 0 22px color-mix(in srgb, var(--accent) 54%, transparent);
  opacity: 0.84;
}

.fluid-card-caption {
  position: absolute;
  left: 0;
  top: 0;
  z-index: 4;
  max-width: 220px;
  padding: 7px 12px;
  border: 1px solid color-mix(in srgb, var(--accent) 34%, rgba(255, 255, 255, 0.24));
  border-radius: 999px;
  color: #06100c;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.94), color-mix(in srgb, var(--accent) 42%, #ffffff));
  box-shadow:
    0 14px 34px rgba(0, 0, 0, 0.22),
    0 0 28px color-mix(in srgb, var(--accent) 32%, transparent);
  font-size: 11px;
  line-height: 1;
  font-weight: var(--font-weight-title);
  white-space: nowrap;
  opacity: var(--caption-opacity);
  pointer-events: none;
  transform:
    translate3d(var(--caption-x), var(--caption-y), 70px)
    rotate(var(--caption-rotate));
  transition: opacity 160ms ease;
  will-change: transform, opacity;
}

.card-1 {
  left: 7vw;
  top: 24vh;
  width: min(34vw, 560px);
  height: min(58vh, 560px);
}

.card-2 {
  right: 7vw;
  top: 58vh;
  width: min(28vw, 460px);
  height: min(42vh, 420px);
}

.card-3 {
  left: 10vw;
  top: 110vh;
  width: min(24vw, 380px);
  height: 42vh;
}

.card-4 {
  left: 39vw;
  top: 150vh;
  width: min(24vw, 360px);
  height: 40vh;
}

.card-5 {
  right: 9vw;
  top: 190vh;
  width: min(27vw, 440px);
  height: 44vh;
}

.fluid-glass {
  left: var(--glass-x);
  top: var(--glass-y);
  z-index: 3;
  width: var(--glass-size);
  height: var(--glass-size);
  border: 1px solid rgba(255, 255, 255, 0.46);
  border-radius: 999px;
  overflow: hidden;
  pointer-events: none;
  background: rgba(255, 255, 255, 0.03);
  box-shadow:
    0 28px 92px rgba(0, 0, 0, 0.36),
    0 0 calc(44px + var(--lens-active) * 34px) rgba(var(--primary-rgb), calc(0.18 + var(--lens-active) * 0.18)),
    inset 0 1px 3px rgba(255, 255, 255, 0.68),
    inset 0 -18px 40px rgba(var(--primary-rgb), 0.12);
  backdrop-filter: blur(calc(7px + var(--lens-active) * 3px)) saturate(calc(150% + var(--lens-active) * 32%)) contrast(calc(110% + var(--lens-active) * 8%));
  transform:
    translate(-50%, -50%)
    scale(calc(1 - var(--lens-active) * 0.18))
    rotateX(calc((0.5 - var(--progress)) * 8deg))
    rotateY(calc((var(--progress) - 0.5) * 10deg));
  transition:
    width 220ms ease,
    height 220ms ease,
    box-shadow 220ms ease,
    backdrop-filter 220ms ease;
  will-change: left, top, transform;
}

.fluid-glass::before,
.fluid-glass::after,
.fluid-lens-sheen {
  content: "";
  position: absolute;
  inset: 0;
  border-radius: inherit;
  pointer-events: none;
}

.fluid-glass::before {
  border: 1px solid rgba(255, 255, 255, 0.26);
  box-shadow:
    inset 0 0 0 2px rgba(255, 255, 255, 0.08),
    inset 18px 18px 30px rgba(255, 255, 255, 0.14),
    inset -24px -28px 42px rgba(0, 0, 0, 0.44);
  transform: scale(0.96);
  z-index: 5;
}

.fluid-glass::after {
  background:
    radial-gradient(circle at 30% 19%, rgba(255, 255, 255, 0.95), transparent 8%),
    radial-gradient(circle at 43% 26%, color-mix(in srgb, var(--accent-glow) 56%, transparent), transparent 16%),
    linear-gradient(90deg, color-mix(in srgb, var(--danger-glow) 52%, transparent), transparent 8%, transparent 88%, color-mix(in srgb, var(--accent-glow) 58%, transparent)),
    radial-gradient(circle at 50% 50%, transparent 54%, rgba(255, 255, 255, 0.24) 72%, transparent 74%);
  mix-blend-mode: screen;
  z-index: 6;
}

.fluid-lens-sheen {
  inset: 7%;
  z-index: 7;
  border: 1px solid rgba(255, 255, 255, 0.18);
  background:
    radial-gradient(ellipse at 35% 16%, rgba(255, 255, 255, 0.58), transparent 22%),
    linear-gradient(165deg, rgba(255, 255, 255, 0.18), transparent 36%, rgba(255, 255, 255, 0.08) 62%, transparent);
  filter: blur(0.1px);
}

.fluid-lens-scale {
  position: absolute;
  inset: 0;
  z-index: 1;
  border-radius: inherit;
  overflow: hidden;
  transform: scale(var(--lens-total-zoom));
  transform-origin: center;
  filter: saturate(calc(1.35 - var(--lens-active) * 0.16)) contrast(calc(1.12 - var(--lens-active) * 0.08)) brightness(calc(1 - var(--lens-active) * 0.06));
  transition: filter 220ms ease, transform 220ms ease;
}

.fluid-lens-field,
.fluid-content-lens {
  position: absolute;
  left: calc(var(--glass-size) / 2 - var(--glass-x));
  top: calc(var(--glass-size) / 2 - var(--glass-y));
  right: auto;
  bottom: auto;
  width: var(--stage-width);
  height: var(--stage-height);
}

.fluid-lens-field {
  z-index: 0;
  background:
    radial-gradient(880px circle at calc(var(--glass-x)) calc(var(--glass-y)), rgba(var(--primary-rgb), 0.26), transparent 42%),
    radial-gradient(760px circle at 84% 28%, color-mix(in srgb, var(--accent-glow) 30%, transparent), transparent 58%),
    linear-gradient(
      180deg,
      color-mix(in srgb, var(--primary-color) 20%, var(--bg-color)) 0%,
      color-mix(in srgb, var(--bg-color) 86%, rgba(var(--primary-rgb), 0.12)) 38%,
      color-mix(in srgb, var(--bg-color) 72%, var(--surface-1) 28%) 70%,
      var(--surface-1) 100%
    );
}

.fluid-lens-field::after {
  content: "";
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(90deg, rgba(var(--primary-rgb), 0.12) 1px, transparent 1px),
    linear-gradient(0deg, color-mix(in srgb, var(--accent-glow) 10%, transparent) 1px, transparent 1px);
  background-size: 34px 34px;
  transform: translate3d(calc(var(--progress) * 26px), calc(var(--progress) * -18px), 0);
}

.fluid-content-lens {
  z-index: 2;
  height: 100%;
  transform: translate3d(0, calc(var(--content-progress) * -10vh), 0);
  opacity: 0.98;
  pointer-events: none;
}

.fluid-content-lens .fluid-card {
  cursor: default;
}

.fluid-content-lens .fluid-card:hover {
  z-index: auto;
}

.glass-bar {
  aspect-ratio: auto;
  height: 82px;
  border-radius: 32px;
  transform: translate(-50%, -50%);
}

.glass-cube {
  aspect-ratio: 1;
  height: var(--glass-size);
  border-radius: 34px;
  transform: translate(-50%, -50%) rotateX(18deg) rotateY(-24deg) rotateZ(8deg);
}

@keyframes signal-float {
  0%,
  100% {
    transform: translate3d(0, 0, 0) rotate(-0.6deg);
  }
  50% {
    transform: translate3d(0, -18px, 0) rotate(0.8deg);
  }
}

@keyframes signal-draw {
  0% {
    stroke-dashoffset: 520;
    opacity: 0.45;
  }
  42%,
  72% {
    stroke-dashoffset: 0;
    opacity: 1;
  }
  100% {
    stroke-dashoffset: -520;
    opacity: 0.42;
  }
}

@keyframes signal-area-breathe {
  0%,
  100% {
    opacity: 0.38;
  }
  50% {
    opacity: 0.76;
  }
}

@keyframes signal-dot-pulse {
  0%,
  100% {
    opacity: 0.56;
    transform: scale(0.9);
  }
  50% {
    opacity: 1;
    transform: scale(1.18);
  }
}

@media (max-width: 900px) {
  .fluid-glass-stage {
    min-height: clamp(1880px, 272svh, 2580px);
  }

  .fluid-sticky {
    min-height: inherit;
  }

  .fluid-content {
    height: 100%;
    transform: translate3d(0, calc(var(--content-progress) * -28vh), 0);
  }

  .fluid-title {
    top: 8vh;
  }

  .fluid-title strong {
    font-size: clamp(34px, 12vw, 64px);
  }

  .fluid-title span {
    margin-top: 12px;
    font-size: 13px;
  }

  .fluid-card {
    width: auto;
    min-height: 220px;
    border-radius: 22px;
  }

  .card-1 {
    left: 18px;
    right: 18px;
    top: 30vh;
    height: 36vh;
  }

  .card-2 {
    left: 18px;
    right: 18px;
    top: 62vh;
    height: 34vh;
  }

  .card-3 {
    left: 18px;
    right: 18px;
    top: 114vh;
    height: 38vh;
  }

  .card-4 {
    left: 18px;
    right: 18px;
    top: 152vh;
    height: 38vh;
  }

  .card-5 {
    left: 18px;
    right: 18px;
    top: 190vh;
    height: 40vh;
  }

  .fluid-glass {
    width: min(var(--glass-size), 78vw);
    backdrop-filter: blur(12px) saturate(170%) contrast(112%);
  }
}

@media (prefers-reduced-motion: reduce) {
  .fluid-card {
    transform: none;
  }
}
</style>
