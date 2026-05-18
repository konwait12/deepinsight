<template>
  <div ref="containerRef" class="shader-field" :class="`shader-field--${variant}`" aria-hidden="true">
    <canvas ref="canvasRef"></canvas>
    <div v-if="!webglReady" class="shader-field__fallback"></div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'

type FieldVariant = 'global' | 'hero'
type Rgb = [number, number, number]

const props = withDefaults(defineProps<{
  variant?: FieldVariant
  dark?: boolean
  paused?: boolean
  dotSpacing?: number
  dotRadius?: number
  bulgeStrength?: number
  cursorRadius?: number
  glowRadius?: number
  auroraIntensity?: number
  dotOpacity?: number
  speed?: number
  dprCap?: number
}>(), {
  variant: 'global',
  dark: false,
  paused: false,
  dotSpacing: undefined,
  dotRadius: undefined,
  bulgeStrength: undefined,
  cursorRadius: undefined,
  glowRadius: undefined,
  auroraIntensity: undefined,
  dotOpacity: undefined,
  speed: 1,
  dprCap: undefined,
})

const VERT = `#version 300 es
in vec2 position;

void main() {
  gl_Position = vec4(position, 0.0, 1.0);
}
`

const FRAG = `#version 300 es
precision highp float;

uniform vec2 uResolution;
uniform vec2 uMouse;
uniform float uTime;
uniform float uMouseSpeed;
uniform float uDark;
uniform float uVariant;
uniform float uDpr;
uniform float uDotSpacing;
uniform float uDotRadius;
uniform float uBulgeStrength;
uniform float uCursorRadius;
uniform float uGlowRadius;
uniform float uAuroraIntensity;
uniform float uDotOpacity;
uniform vec3 uPrimary;
uniform vec3 uAccent;
uniform vec3 uViolet;
uniform vec3 uRose;

out vec4 fragColor;

float hash21(vec2 p) {
  p = fract(p * vec2(123.34, 456.21));
  p += dot(p, p + 45.32);
  return fract(p.x * p.y);
}

float noise(vec2 p) {
  vec2 i = floor(p);
  vec2 f = fract(p);
  vec2 u = f * f * (3.0 - 2.0 * f);
  float a = hash21(i);
  float b = hash21(i + vec2(1.0, 0.0));
  float c = hash21(i + vec2(0.0, 1.0));
  float d = hash21(i + vec2(1.0, 1.0));
  return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float fbm(vec2 p) {
  float v = 0.0;
  float a = 0.5;
  mat2 rot = mat2(0.80, -0.60, 0.60, 0.80);
  for (int i = 0; i < 4; i++) {
    v += a * noise(p);
    p = rot * p * 2.03 + 12.7;
    a *= 0.52;
  }
  return v;
}

float dotMask(vec2 p, float spacing, float radius) {
  vec2 grid = p / spacing;
  vec2 id = floor(grid);
  vec2 cell = fract(grid) - 0.5;
  float jitter = (hash21(id) - 0.5) * 0.22;
  float d = length(cell * spacing);
  float r = radius * (0.86 + jitter);
  return 1.0 - smoothstep(r, r + 1.18 * uDpr, d);
}

void main() {
  vec2 frag = gl_FragCoord.xy;
  vec2 uv = frag / uResolution;
  vec2 centered = (frag - uResolution * 0.5) / max(uResolution.x, uResolution.y);
  float aspect = uResolution.x / max(uResolution.y, 1.0);
  float t = uTime;
  float lightMode = 1.0 - uDark;
  vec3 lightPaper = vec3(0.12, 0.16, 0.14);

  float mouseDistance = length(frag - uMouse);
  float cursor = 1.0 - smoothstep(0.0, uCursorRadius, mouseDistance);
  float cursorSoft = cursor * cursor * (0.35 + uMouseSpeed * 0.65);
  vec2 cursorDir = normalize(frag - uMouse + vec2(0.001, -0.001));
  vec2 warpedFrag = frag - cursorDir * cursorSoft * uBulgeStrength;

  vec2 flowUv = vec2(uv.x * aspect, uv.y);
  float slowNoise = fbm(flowUv * vec2(1.55, 2.25) + vec2(t * 0.045, -t * 0.025));
  float weaveNoise = fbm(flowUv * vec2(5.8, 2.1) + vec2(-t * 0.075, t * 0.035));
  float upperPath = 0.68 + sin(flowUv.x * 2.4 + slowNoise * 2.6 + t * 0.11) * 0.11;
  float lowerPath = 0.26 + cos(flowUv.x * 2.1 - weaveNoise * 2.2 - t * 0.09) * 0.10;
  float upperBand = exp(-pow((uv.y - upperPath) * (3.3 + uVariant * 0.7), 2.0));
  float lowerBand = exp(-pow((uv.y - lowerPath) * (4.1 + uVariant * 0.8), 2.0));
  float diagonal = sin((flowUv.x * 1.4 + flowUv.y * 1.1 + slowNoise * 0.8 + t * 0.045) * 6.28318) * 0.5 + 0.5;
  float fabric = smoothstep(0.28, 1.0, upperBand * 0.62 + lowerBand * 0.46 + diagonal * 0.22 + weaveNoise * 0.3);

  vec3 colorA = mix(uPrimary, uAccent, smoothstep(0.0, 1.0, uv.x + slowNoise * 0.18));
  vec3 colorB = mix(uViolet, uRose, smoothstep(0.1, 0.95, diagonal + weaveNoise * 0.25));
  vec3 auroraColor = mix(colorA, colorB, 0.28 + weaveNoise * 0.36);
  float vignette = 1.0 - smoothstep(0.12, 0.74, length(centered));
  float auroraAlpha = fabric * vignette * uAuroraIntensity;

  float dot = dotMask(warpedFrag, uDotSpacing, uDotRadius);
  float dotNoise = fbm((warpedFrag / uDotSpacing) * 0.42 + t * 0.018);
  float dotFade = mix(0.58, 1.18, dotNoise) * (0.74 + cursor * 0.52);
  vec3 dotColor = mix(uPrimary, uAccent, smoothstep(0.0, 1.0, uv.x + uv.y * 0.25));
  dotColor = mix(dotColor, uViolet, 0.16 + weaveNoise * 0.18);
  float dotAlpha = dot * uDotOpacity * dotFade;

  float glow = exp(-pow(mouseDistance / max(uGlowRadius, 1.0), 2.0));
  vec3 glowColor = mix(uPrimary, uAccent, 0.34 + slowNoise * 0.24);
  float glowAlpha = glow * (0.10 + 0.20 * uMouseSpeed) * (uVariant > 0.5 ? 1.18 : 0.72);

  float lineMask = smoothstep(0.98, 1.0, sin((uv.x * 36.0 + uv.y * 18.0 + slowNoise * 3.0 + t * 0.12) * 6.28318));
  float filament = lineMask * fabric * 0.018 * (0.35 + uVariant * 0.65);

  auroraColor = mix(auroraColor, lightPaper, lightMode * 0.03);
  dotColor = mix(dotColor, lightPaper, lightMode * 0.04);
  glowColor = mix(glowColor, lightPaper, lightMode * 0.03);

  vec3 color = auroraColor * auroraAlpha;
  color += dotColor * dotAlpha;
  color += glowColor * glowAlpha;
  color += colorB * filament;

  float alpha = auroraAlpha * (uDark > 0.5 ? 0.66 : 0.34)
    + dotAlpha * (uDark > 0.5 ? 0.92 : 0.58)
    + glowAlpha
    + filament;

  alpha *= mix(1.0, 0.44, lightMode);
  fragColor = vec4(color, clamp(alpha, 0.0, 0.86));
}
`

const containerRef = ref<HTMLDivElement | null>(null)
const canvasRef = ref<HTMLCanvasElement | null>(null)
const webglReady = ref(true)
const paletteTick = ref(0)
const variant = computed(() => props.variant)

let gl: WebGL2RenderingContext | null = null
let program: WebGLProgram | null = null
let buffer: WebGLBuffer | null = null
let rafId = 0
let resizeObserver: ResizeObserver | null = null
let mutationObserver: MutationObserver | null = null
let dpr = 1
let width = 1
let height = 1
let pointerX = 0.5
let pointerY = 0.42
let targetPointerX = 0.5
let targetPointerY = 0.42
let previousPointerX = 0.5
let previousPointerY = 0.42
let smoothedSpeed = 0
let lastRenderTime = 0
let reducedMotion = false

const uniforms: Record<string, WebGLUniformLocation | null> = {}

function readCssVar(name: string, fallback: string) {
  paletteTick.value
  if (typeof window === 'undefined') return fallback
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim() || fallback
}

function parseColor(color: string, fallback: Rgb): Rgb {
  const value = color.trim()
  if (!value) return fallback
  if (value.startsWith('#')) {
    const clean = value.slice(1)
    const hex = clean.length === 3
      ? clean.split('').map((char) => `${char}${char}`).join('')
      : clean.padEnd(6, '0').slice(0, 6)
    return [
      parseInt(hex.slice(0, 2), 16) / 255,
      parseInt(hex.slice(2, 4), 16) / 255,
      parseInt(hex.slice(4, 6), 16) / 255,
    ]
  }
  if (value.startsWith('rgb')) {
    const nums = value.match(/[\d.]+/g)?.slice(0, 3).map(Number)
    if (nums && nums.length >= 3) return [nums[0] / 255, nums[1] / 255, nums[2] / 255]
  }
  return fallback
}

function cssColor(name: string, fallback: string, rgbFallback: Rgb): Rgb {
  return parseColor(readCssVar(name, fallback), rgbFallback)
}

function createShader(context: WebGL2RenderingContext, type: number, source: string) {
  const shader = context.createShader(type)
  if (!shader) return null
  context.shaderSource(shader, source)
  context.compileShader(shader)
  if (!context.getShaderParameter(shader, context.COMPILE_STATUS)) {
    console.warn('[ShaderField] shader compile failed:', context.getShaderInfoLog(shader))
    context.deleteShader(shader)
    return null
  }
  return shader
}

function cacheUniforms() {
  if (!gl || !program) return
  [
    'uResolution',
    'uMouse',
    'uTime',
    'uMouseSpeed',
    'uDark',
    'uVariant',
    'uDpr',
    'uDotSpacing',
    'uDotRadius',
    'uBulgeStrength',
    'uCursorRadius',
    'uGlowRadius',
    'uAuroraIntensity',
    'uDotOpacity',
    'uPrimary',
    'uAccent',
    'uViolet',
    'uRose',
  ].forEach((name) => {
    uniforms[name] = gl!.getUniformLocation(program!, name)
  })
}

function resize() {
  const canvas = canvasRef.value
  const container = containerRef.value
  if (!canvas || !container || !gl) return
  const rect = container.getBoundingClientRect()
  const cap = props.dprCap ?? (props.variant === 'hero' ? 1.45 : 1.2)
  dpr = Math.min(window.devicePixelRatio || 1, cap)
  width = Math.max(1, Math.round(rect.width * dpr))
  height = Math.max(1, Math.round(rect.height * dpr))
  if (canvas.width !== width || canvas.height !== height) {
    canvas.width = width
    canvas.height = height
    canvas.style.width = `${rect.width}px`
    canvas.style.height = `${rect.height}px`
  }
  gl.viewport(0, 0, width, height)
}

function handlePointerMove(event: PointerEvent) {
  const container = containerRef.value
  if (!container) return
  const rect = container.getBoundingClientRect()
  targetPointerX = (event.clientX - rect.left) / Math.max(rect.width, 1)
  targetPointerY = (event.clientY - rect.top) / Math.max(rect.height, 1)
}

function applyUniforms(time: number) {
  if (!gl || !program) return
  paletteTick.value
  const isDark = props.dark
  const isHero = props.variant === 'hero'
  const primary = cssColor('--primary-color', '#42e6a4', [0.26, 0.9, 0.64])
  const accent = cssColor('--accent-glow', '#7dd3fc', [0.49, 0.83, 0.99])
  const violet = cssColor('--tone-violet', '#8b5cf6', [0.55, 0.36, 0.96])
  const rose = cssColor('--tone-rose', '#fb7185', [0.98, 0.44, 0.52])
  const spacing = (props.dotSpacing ?? (isHero ? (isDark ? 14 : 16) : (isDark ? 18 : 22))) * dpr
  const radius = (props.dotRadius ?? (isHero ? (isDark ? 1.32 : 1.04) : (isDark ? 0.92 : 0.72))) * dpr
  const bulge = (props.bulgeStrength ?? (isHero ? (isDark ? 70 : 48) : (isDark ? 42 : 30))) * dpr
  const cursorRadius = (props.cursorRadius ?? (isHero ? 500 : 420)) * dpr
  const glowRadius = (props.glowRadius ?? (isHero ? 230 : 190)) * dpr
  const auroraIntensity = props.auroraIntensity ?? (isHero ? (isDark ? 0.4 : 0.26) : (isDark ? 0.17 : 0.08))
  const dotOpacity = props.dotOpacity ?? (isHero ? (isDark ? 0.56 : 0.38) : (isDark ? 0.28 : 0.13))

  gl.uniform2f(uniforms.uResolution, width, height)
  gl.uniform2f(uniforms.uMouse, pointerX * width, (1 - pointerY) * height)
  gl.uniform1f(uniforms.uTime, time * 0.001 * props.speed)
  gl.uniform1f(uniforms.uMouseSpeed, smoothedSpeed)
  gl.uniform1f(uniforms.uDark, isDark ? 1 : 0)
  gl.uniform1f(uniforms.uVariant, isHero ? 1 : 0)
  gl.uniform1f(uniforms.uDpr, dpr)
  gl.uniform1f(uniforms.uDotSpacing, spacing)
  gl.uniform1f(uniforms.uDotRadius, radius)
  gl.uniform1f(uniforms.uBulgeStrength, bulge)
  gl.uniform1f(uniforms.uCursorRadius, cursorRadius)
  gl.uniform1f(uniforms.uGlowRadius, glowRadius)
  gl.uniform1f(uniforms.uAuroraIntensity, auroraIntensity)
  gl.uniform1f(uniforms.uDotOpacity, dotOpacity)
  gl.uniform3fv(uniforms.uPrimary, primary)
  gl.uniform3fv(uniforms.uAccent, accent)
  gl.uniform3fv(uniforms.uViolet, violet)
  gl.uniform3fv(uniforms.uRose, rose)
}

function render(time = 0) {
  if (!gl || !program) return
  rafId = window.requestAnimationFrame(render)
  if (document.hidden) return
  if (props.paused) return

  const frameGap = time - lastRenderTime
  const minFrameGap = props.variant === 'hero' ? 15 : 22
  if (!reducedMotion && frameGap < minFrameGap) return
  lastRenderTime = time

  pointerX += (targetPointerX - pointerX) * 0.12
  pointerY += (targetPointerY - pointerY) * 0.12
  const dx = pointerX - previousPointerX
  const dy = pointerY - previousPointerY
  const instantSpeed = Math.min(1, Math.sqrt(dx * dx + dy * dy) * 85)
  smoothedSpeed += (instantSpeed - smoothedSpeed) * 0.18
  smoothedSpeed *= 0.965
  previousPointerX = pointerX
  previousPointerY = pointerY

  gl.clear(gl.COLOR_BUFFER_BIT)
  applyUniforms(reducedMotion ? 0 : time)
  gl.drawArrays(gl.TRIANGLES, 0, 3)
}

function refreshPalette() {
  paletteTick.value += 1
}

function initWebgl() {
  const canvas = canvasRef.value
  if (!canvas) return false
  gl = canvas.getContext('webgl2', {
    alpha: true,
    antialias: false,
    depth: false,
    stencil: false,
    premultipliedAlpha: true,
    powerPreference: 'high-performance',
  })
  if (!gl) return false

  const vert = createShader(gl, gl.VERTEX_SHADER, VERT)
  const frag = createShader(gl, gl.FRAGMENT_SHADER, FRAG)
  if (!vert || !frag) return false

  program = gl.createProgram()
  if (!program) return false
  gl.attachShader(program, vert)
  gl.attachShader(program, frag)
  gl.linkProgram(program)
  gl.deleteShader(vert)
  gl.deleteShader(frag)
  if (!gl.getProgramParameter(program, gl.LINK_STATUS)) {
    console.warn('[ShaderField] program link failed:', gl.getProgramInfoLog(program))
    return false
  }

  gl.useProgram(program)
  buffer = gl.createBuffer()
  gl.bindBuffer(gl.ARRAY_BUFFER, buffer)
  gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([-1, -1, 3, -1, -1, 3]), gl.STATIC_DRAW)
  const position = gl.getAttribLocation(program, 'position')
  gl.enableVertexAttribArray(position)
  gl.vertexAttribPointer(position, 2, gl.FLOAT, false, 0, 0)
  gl.clearColor(0, 0, 0, 0)
  gl.enable(gl.BLEND)
  gl.blendFunc(gl.ONE, gl.ONE_MINUS_SRC_ALPHA)
  cacheUniforms()
  return true
}

watch(
  () => [props.dark, props.variant, props.dotSpacing, props.dotRadius, props.bulgeStrength, props.speed],
  refreshPalette,
)

onMounted(() => {
  reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  webglReady.value = initWebgl()
  if (!webglReady.value) return

  resize()
  applyUniforms(0)
  gl?.drawArrays(gl.TRIANGLES, 0, 3)
  resizeObserver = new ResizeObserver(resize)
  if (containerRef.value) resizeObserver.observe(containerRef.value)
  mutationObserver = new MutationObserver(refreshPalette)
  mutationObserver.observe(document.documentElement, {
    attributes: true,
    attributeFilter: ['class', 'style', 'data-palette'],
  })
  window.addEventListener('pointermove', handlePointerMove, { passive: true })
  window.addEventListener('resize', resize, { passive: true })
  rafId = window.requestAnimationFrame(render)
})

onUnmounted(() => {
  window.cancelAnimationFrame(rafId)
  window.removeEventListener('pointermove', handlePointerMove)
  window.removeEventListener('resize', resize)
  resizeObserver?.disconnect()
  mutationObserver?.disconnect()
  if (gl && program) gl.deleteProgram(program)
  if (gl && buffer) gl.deleteBuffer(buffer)
  gl?.getExtension('WEBGL_lose_context')?.loseContext()
  gl = null
  program = null
  buffer = null
})
</script>

<style scoped>
.shader-field {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  overflow: hidden;
  pointer-events: none;
  contain: strict;
}

.shader-field canvas,
.shader-field__fallback {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  display: block;
}

.shader-field__fallback {
  background:
    radial-gradient(circle at 24% 18%, rgba(var(--primary-rgb), 0.14), transparent 34%),
    radial-gradient(circle at 78% 24%, color-mix(in srgb, var(--accent-glow) 16%, transparent), transparent 34%);
}
</style>
