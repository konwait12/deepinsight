<template>
  <div ref="containerRef" class="iridescence-field" :class="{ 'is-active': active }" aria-hidden="true">
    <canvas ref="canvasRef"></canvas>
    <div v-if="!webglReady" class="iridescence-fallback"></div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, watch, ref } from 'vue'

type Rgb = [number, number, number]

const props = withDefaults(defineProps<{
  active?: boolean
  color?: Rgb
  speed?: number
  amplitude?: number
  opacity?: number
  themeStrength?: number
  mouseReact?: boolean
}>(), {
  active: true,
  color: () => [1, 1, 1],
  speed: 1,
  amplitude: 0.1,
  opacity: 1,
  themeStrength: 0.58,
  mouseReact: true,
})

const VERT = `#version 300 es
in vec2 position;
out vec2 vUv;

void main() {
  vUv = position * 0.5 + 0.5;
  gl_Position = vec4(position, 0.0, 1.0);
}
`

const FRAG = `#version 300 es
precision highp float;

uniform float uTime;
uniform vec3 uColor;
uniform vec3 uResolution;
uniform vec2 uMouse;
uniform float uAmplitude;
uniform float uSpeed;
uniform float uOpacity;
uniform float uThemeStrength;
uniform vec3 uPrimary;
uniform vec3 uAccent;
uniform vec3 uDanger;

in vec2 vUv;
out vec4 fragColor;

float grain(vec2 p) {
  return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

void main() {
  float mr = min(uResolution.x, uResolution.y);
  vec2 uv = (vUv.xy * 2.0 - 1.0) * uResolution.xy / max(mr, 1.0);
  uv += (uMouse - vec2(0.5)) * uAmplitude;

  float d = -uTime * 0.5 * uSpeed;
  float a = 0.0;
  for (float i = 0.0; i < 8.0; ++i) {
    a += cos(i - d - a * uv.x);
    d += sin(uv.y * i + a);
  }
  d += uTime * 0.5 * uSpeed;

  vec3 col = vec3(cos(uv * vec2(d, a)) * 0.6 + 0.4, cos(a + d) * 0.5 + 0.5);
  col = cos(col * cos(vec3(d, a, 2.5)) * 0.5 + 0.5) * uColor;
  float paper = (grain(gl_FragCoord.xy + floor(uTime * 8.0)) - 0.5) * 0.018;
  float hueFlow = smoothstep(-0.9, 0.95, sin(uv.x * 1.7 + uv.y * 1.1 + d * 0.18));
  float hotLine = smoothstep(0.72, 1.0, cos(uv.x * d - uv.y * a));
  vec3 themeGlow = mix(uPrimary, uAccent, hueFlow);
  themeGlow = mix(themeGlow, uDanger, hotLine * 0.32);
  vec3 themed = col * (0.68 + themeGlow * 0.74) + themeGlow * 0.22;
  vec3 color = clamp(mix(col, themed, clamp(uThemeStrength, 0.0, 1.0)) + paper, 0.0, 1.0);

  fragColor = vec4(color, clamp(uOpacity, 0.0, 1.0));
}
`

const containerRef = ref<HTMLDivElement | null>(null)
const canvasRef = ref<HTMLCanvasElement | null>(null)
const webglReady = ref(true)

let gl: WebGL2RenderingContext | null = null
let program: WebGLProgram | null = null
let buffer: WebGLBuffer | null = null
let rafId = 0
let resizeObserver: ResizeObserver | null = null
let dpr = 1
let width = 1
let height = 1
let mouseX = 0.5
let mouseY = 0.5
let targetMouseX = 0.5
let targetMouseY = 0.5
let paletteColor: Rgb = [1, 1, 1]
let primaryColor: Rgb = [0.26, 0.9, 0.64]
let accentColor: Rgb = [0.49, 0.83, 0.99]
let dangerColor: Rgb = [0.98, 0.44, 0.52]
let mutationObserver: MutationObserver | null = null

const uniforms: Record<string, WebGLUniformLocation | null> = {}

function parseCssColor(value: string, fallback: Rgb): Rgb {
  const color = value.trim()
  if (!color) return fallback
  if (color.startsWith('#')) {
    const clean = color.slice(1)
    const hex = clean.length === 3
      ? clean.split('').map((char) => `${char}${char}`).join('')
      : clean.padEnd(6, '0').slice(0, 6)
    return [
      Number.parseInt(hex.slice(0, 2), 16) / 255,
      Number.parseInt(hex.slice(2, 4), 16) / 255,
      Number.parseInt(hex.slice(4, 6), 16) / 255,
    ]
  }
  const rgbMatch = color.match(/rgba?\(([^)]+)\)/i)
  if (rgbMatch) {
    const parts = rgbMatch[1].split(',').map((part) => Number.parseFloat(part.trim()))
    if (parts.length >= 3 && parts.every((part) => Number.isFinite(part))) {
      return [parts[0] / 255, parts[1] / 255, parts[2] / 255]
    }
  }
  return fallback
}

function readThemeColors() {
  if (typeof window === 'undefined') return
  const style = getComputedStyle(document.documentElement)
  primaryColor = parseCssColor(style.getPropertyValue('--primary-color'), primaryColor)
  accentColor = parseCssColor(style.getPropertyValue('--accent-glow'), accentColor)
  dangerColor = parseCssColor(style.getPropertyValue('--danger-glow'), dangerColor)
  const lift: Rgb = [1, 1, 1]
  paletteColor = [
    Math.min(1, props.color[0] * 0.58 + lift[0] * 0.24 + accentColor[0] * 0.18),
    Math.min(1, props.color[1] * 0.58 + lift[1] * 0.24 + accentColor[1] * 0.18),
    Math.min(1, props.color[2] * 0.58 + lift[2] * 0.24 + accentColor[2] * 0.18),
  ]
}

function createShader(type: number, source: string) {
  if (!gl) return null
  const shader = gl.createShader(type)
  if (!shader) return null
  gl.shaderSource(shader, source)
  gl.compileShader(shader)
  if (!gl.getShaderParameter(shader, gl.COMPILE_STATUS)) {
    console.warn('[IridescenceField] shader compile failed:', gl.getShaderInfoLog(shader))
    gl.deleteShader(shader)
    return null
  }
  return shader
}

function createProgram() {
  if (!gl) return null
  const vertex = createShader(gl.VERTEX_SHADER, VERT)
  const fragment = createShader(gl.FRAGMENT_SHADER, FRAG)
  if (!vertex || !fragment) return null

  const nextProgram = gl.createProgram()
  if (!nextProgram) return null
  gl.attachShader(nextProgram, vertex)
  gl.attachShader(nextProgram, fragment)
  gl.bindAttribLocation(nextProgram, 0, 'position')
  gl.linkProgram(nextProgram)
  gl.deleteShader(vertex)
  gl.deleteShader(fragment)

  if (!gl.getProgramParameter(nextProgram, gl.LINK_STATUS)) {
    console.warn('[IridescenceField] program link failed:', gl.getProgramInfoLog(nextProgram))
    gl.deleteProgram(nextProgram)
    return null
  }

  return nextProgram
}

function resize() {
  const canvas = canvasRef.value
  const container = containerRef.value
  if (!gl || !canvas || !container) return
  const rect = container.getBoundingClientRect()
  width = Math.max(1, Math.floor(rect.width))
  height = Math.max(1, Math.floor(rect.height))
  dpr = Math.min(window.devicePixelRatio || 1, 1.5)
  const nextWidth = Math.floor(width * dpr)
  const nextHeight = Math.floor(height * dpr)
  if (canvas.width !== nextWidth || canvas.height !== nextHeight) {
    canvas.width = nextWidth
    canvas.height = nextHeight
    canvas.style.width = `${width}px`
    canvas.style.height = `${height}px`
  }
  gl.viewport(0, 0, canvas.width, canvas.height)
}

function draw(time = 0) {
  if (!gl || !program || !props.active) {
    rafId = window.requestAnimationFrame(draw)
    return
  }

  mouseX += (targetMouseX - mouseX) * 0.075
  mouseY += (targetMouseY - mouseY) * 0.075

  gl.clear(gl.COLOR_BUFFER_BIT)
  gl.useProgram(program)
  gl.uniform1f(uniforms.uTime, time * 0.001)
  gl.uniform3f(uniforms.uColor, paletteColor[0], paletteColor[1], paletteColor[2])
  gl.uniform3f(uniforms.uResolution, width * dpr, height * dpr, width / Math.max(height, 1))
  gl.uniform2f(uniforms.uMouse, mouseX, mouseY)
  gl.uniform1f(uniforms.uAmplitude, props.mouseReact ? props.amplitude : 0)
  gl.uniform1f(uniforms.uSpeed, props.speed)
  gl.uniform1f(uniforms.uOpacity, props.active ? props.opacity : 0)
  gl.uniform1f(uniforms.uThemeStrength, props.themeStrength)
  gl.uniform3f(uniforms.uPrimary, primaryColor[0], primaryColor[1], primaryColor[2])
  gl.uniform3f(uniforms.uAccent, accentColor[0], accentColor[1], accentColor[2])
  gl.uniform3f(uniforms.uDanger, dangerColor[0], dangerColor[1], dangerColor[2])
  gl.drawArrays(gl.TRIANGLES, 0, 3)

  rafId = window.requestAnimationFrame(draw)
}

function handlePointerMove(event: PointerEvent) {
  if (!props.mouseReact || !containerRef.value) return
  const rect = containerRef.value.getBoundingClientRect()
  targetMouseX = (event.clientX - rect.left) / Math.max(rect.width, 1)
  targetMouseY = 1 - (event.clientY - rect.top) / Math.max(rect.height, 1)
}

function setup() {
  const canvas = canvasRef.value
  const container = containerRef.value
  if (!canvas || !container) return
  gl = canvas.getContext('webgl2', {
    alpha: true,
    antialias: false,
    depth: false,
    stencil: false,
    powerPreference: 'low-power',
  })
  if (!gl) {
    webglReady.value = false
    return
  }

  program = createProgram()
  if (!program) {
    webglReady.value = false
    return
  }

  buffer = gl.createBuffer()
  gl.bindBuffer(gl.ARRAY_BUFFER, buffer)
  gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([-1, -1, 3, -1, -1, 3]), gl.STATIC_DRAW)
  gl.enableVertexAttribArray(0)
  gl.vertexAttribPointer(0, 2, gl.FLOAT, false, 0, 0)

  uniforms.uTime = gl.getUniformLocation(program, 'uTime')
  uniforms.uColor = gl.getUniformLocation(program, 'uColor')
  uniforms.uResolution = gl.getUniformLocation(program, 'uResolution')
  uniforms.uMouse = gl.getUniformLocation(program, 'uMouse')
  uniforms.uAmplitude = gl.getUniformLocation(program, 'uAmplitude')
  uniforms.uSpeed = gl.getUniformLocation(program, 'uSpeed')
  uniforms.uOpacity = gl.getUniformLocation(program, 'uOpacity')
  uniforms.uThemeStrength = gl.getUniformLocation(program, 'uThemeStrength')
  uniforms.uPrimary = gl.getUniformLocation(program, 'uPrimary')
  uniforms.uAccent = gl.getUniformLocation(program, 'uAccent')
  uniforms.uDanger = gl.getUniformLocation(program, 'uDanger')

  gl.disable(gl.DEPTH_TEST)
  gl.disable(gl.CULL_FACE)
  gl.enable(gl.BLEND)
  gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA)
  gl.clearColor(0, 0, 0, 0)

  resizeObserver = new ResizeObserver(resize)
  resizeObserver.observe(container)
  readThemeColors()
  mutationObserver = new MutationObserver(readThemeColors)
  mutationObserver.observe(document.documentElement, {
    attributes: true,
    attributeFilter: ['class', 'style', 'data-palette'],
  })
  resize()
  window.addEventListener('resize', resize)
  window.addEventListener('pointermove', handlePointerMove, { passive: true })
  rafId = window.requestAnimationFrame(draw)
}

function teardown() {
  if (rafId) window.cancelAnimationFrame(rafId)
  rafId = 0
  resizeObserver?.disconnect()
  resizeObserver = null
  mutationObserver?.disconnect()
  mutationObserver = null
  window.removeEventListener('resize', resize)
  window.removeEventListener('pointermove', handlePointerMove)
  if (gl) {
    if (buffer) gl.deleteBuffer(buffer)
    if (program) gl.deleteProgram(program)
    gl.getExtension('WEBGL_lose_context')?.loseContext()
  }
  gl = null
  program = null
  buffer = null
}

watch(() => props.active, () => {
  if (props.active && !rafId && gl && program) rafId = window.requestAnimationFrame(draw)
})

watch(() => props.color, readThemeColors, { deep: true })

onMounted(setup)
onUnmounted(teardown)
</script>

<style scoped>
.iridescence-field {
  position: fixed;
  inset: 0;
  z-index: 1;
  overflow: hidden;
  pointer-events: none;
  opacity: 0;
  mix-blend-mode: normal;
  transition: opacity 520ms var(--ease-smooth);
  contain: paint;
}

.iridescence-field.is-active {
  opacity: 1;
}

.iridescence-field canvas,
.iridescence-fallback {
  width: 100%;
  height: 100%;
  display: block;
}

.iridescence-fallback {
  background:
    linear-gradient(116deg, rgba(255, 255, 255, 0.12), transparent 44%, rgba(96, 165, 250, 0.08)),
    linear-gradient(180deg, rgba(244, 250, 255, 0.7), rgba(232, 247, 242, 0.36));
}

@media (prefers-reduced-motion: reduce) {
  .iridescence-field {
    opacity: 0.34;
  }
}
</style>
