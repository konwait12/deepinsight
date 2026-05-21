<template>
  <main
    ref="landingRootRef"
    class="di-landing"
    @pointermove="handlePointerMove"
  >
    <section class="hero-section" aria-labelledby="hero-title">
      <ShaderField
        class="hero-shader-field"
        variant="hero"
        :dark="true"
        :paused="false"
        :speed="0.88"
        :dot-spacing="14"
        :dot-radius="1.32"
        :cursor-radius="500"
        :bulge-strength="70"
        :glow-radius="230"
        :aurora-intensity="0.4"
        :dot-opacity="0.56"
        :dpr-cap="1.35"
      />
      <div class="hero-grid" aria-hidden="true"></div>

      <div class="hero-shell entrance-hero">
        <div class="hero-kicker">
          <span class="signal-dot"></span>
          {{ pageCopy.heroKicker }}
        </div>

        <h1 id="hero-title">
          <ShinyText
            text="DeepInsight"
            class-name="hero-shiny-title"
            color="rgba(248, 253, 255, 0.92)"
            shine-color="var(--primary-color)"
            :speed="1.35"
            :delay="0.18"
            :spread="102"
          />
        </h1>
        <p class="hero-copy">
          {{ pageCopy.heroCopy }}
        </p>

        <div class="hero-code-ribbon" aria-hidden="true">
          <span v-for="line in activeCase.lines" :key="line">{{ line }}</span>
        </div>

        <div class="hero-actions">
          <button class="primary-action" type="button" @click="openWorkspace">
            {{ pageCopy.primaryAction }}
            <ArrowRight :size="17" stroke-width="2.4" />
          </button>
          <button class="secondary-action" type="button" @click="scrollTo('work')">
            {{ pageCopy.secondaryAction }}
            <Play :size="16" stroke-width="2.4" />
          </button>
        </div>
      </div>

      <aside class="hero-console entrance-scale" aria-label="DeepInsight live task stream">
        <div class="console-topline">
          <span>{{ pageCopy.consoleRun }}</span>
          <span>{{ pageCopy.consoleSections }}</span>
        </div>

        <div class="console-stage">
          <div class="stage-radar" aria-hidden="true">
            <span></span>
            <span></span>
            <span></span>
          </div>
          <div>
            <strong>{{ activeCase.title }}</strong>
            <p>{{ activeCase.signal }}</p>
          </div>
        </div>

        <div class="task-list">
          <div v-for="task in tasks" :key="task.label" class="task-row">
            <component :is="task.icon" :size="16" stroke-width="2.2" />
            <span>{{ task.label }}</span>
          </div>
        </div>

        <div class="console-code-block" aria-hidden="true">
          <span v-for="line in activeCase.lines" :key="line">{{ line }}</span>
        </div>
      </aside>
    </section>

    <section class="proof-strip" aria-label="DeepInsight metrics">
      <div v-for="metric in metrics" :key="metric.label" class="metric-cell">
        <strong>{{ metric.value }}</strong>
        <span>{{ metric.label }}</span>
      </div>
    </section>

    <section id="work" class="fluid-stage-band" aria-label="DeepInsight fluid glass preview">
      <FluidGlassStage :dark="true" />
    </section>

    <section class="section-band narrative-band">
      <div class="narrative-copy">
        <span>{{ pageCopy.disciplineEyebrow }}</span>
        <h2>{{ pageCopy.disciplineTitle }}</h2>
        <p>
          {{ pageCopy.disciplineCopy }}
        </p>
      </div>

      <div class="flow-panel">
        <div v-for="step in flow" :key="step.title" class="flow-step">
          <div class="flow-index">{{ step.index }}</div>
          <div>
            <h3>{{ step.title }}</h3>
            <p>{{ step.text }}</p>
          </div>
        </div>
      </div>
    </section>

    <section class="section-band studio-band">
      <div class="section-heading compact">
        <span>{{ pageCopy.studioEyebrow }}</span>
        <h2>{{ pageCopy.studioTitle }}</h2>
      </div>

      <div class="studio-wall">
        <div class="studio-feed">
          <div class="feed-header">
            <span>MODEL FIELD</span>
            <span>{{ activeCase.tag }}</span>
          </div>
          <div class="feed-screen">
            <div class="signal-map" aria-hidden="true">
              <span v-for="node in signalNodes" :key="node.id" :style="{ left: node.x, top: node.y }"></span>
            </div>
            <div class="pulse-line line-a"></div>
            <div class="pulse-line line-b"></div>
            <div class="pulse-line line-c"></div>
          </div>
        </div>
        <div class="studio-copy">
          <h3>{{ activeCase.title }}</h3>
          <p>{{ activeCase.detail }}</p>
          <button class="text-action" type="button" @click="router.push('/dashboard')">
            {{ pageCopy.openDashboard }}
            <ArrowRight :size="15" stroke-width="2.5" />
          </button>
        </div>
      </div>
    </section>

    <section class="closing-band">
      <h2>{{ pageCopy.closingTitle }}</h2>
      <button class="primary-action" type="button" @click="openWorkspace">
        {{ pageCopy.closingAction }}
        <ArrowRight :size="17" stroke-width="2.4" />
      </button>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth.store'
import FluidGlassStage from '@/components/effects/FluidGlassStage.vue'
import ShaderField from '@/components/effects/ShaderField.vue'
import ShinyText from '@/components/effects/ShinyText.vue'
import {
  Activity,
  ArrowRight,
  Boxes,
  Code2,
  GitPullRequest,
  Play,
  Radar,
  ShieldCheck,
  Sparkles,
  Terminal,
} from 'lucide-vue-next'

type CaseItem = {
  title: string
  tag: string
  signal: string
  desc: string
  detail: string
  accent: string
  bars: number[]
  lines: string[]
  nodes: Array<{ x: string; y: string }>
  icon: typeof Activity
}

type LandingCopy = {
  heroKicker: string
  heroCopy: string
  primaryAction: string
  secondaryAction: string
  consoleRun: string
  consoleSections: string
  metrics: Array<{ value: string; label: string }>
  workEyebrow: string
  workTitle: string
  disciplineEyebrow: string
  disciplineTitle: string
  disciplineCopy: string
  studioEyebrow: string
  studioTitle: string
  openDashboard: string
  closingTitle: string
  closingAction: string
  cases: Array<Omit<CaseItem, 'accent' | 'bars' | 'icon' | 'lines' | 'nodes'>>
  tasks: Array<{ label: string; state: string }>
  flow: Array<{ index: string; title: string; text: string }>
}

const router = useRouter()
const authStore = useAuthStore()
const { locale } = useI18n()
const landingRootRef = ref<HTMLElement | null>(null)
const workCardPointers = ref([
  { x: 42, y: 30 },
  { x: 52, y: 34 },
  { x: 48, y: 36 },
  { x: 56, y: 28 },
])
const pointer = { x: 0, y: 0 }
let pointerRaf = 0
let pendingPointerCssX = '50%'
let pendingPointerCssY = '50%'
let pendingTiltX = '0deg'
let pendingTiltY = '0deg'

const openWorkspace = () => router.push(authStore.isAuthenticated ? '/dashboard' : '/login')

const caseVisuals: Array<Pick<CaseItem, 'accent' | 'bars' | 'icon' | 'lines' | 'nodes'>> = [
  {
    accent: '#48f5a6',
    bars: [34, 58, 78, 52, 88, 66],
    lines: ['epoch.sync()', 'loss.guard()', 'gpu.observe()'],
    nodes: [{ x: '18%', y: '22%' }, { x: '42%', y: '58%' }, { x: '76%', y: '28%' }, { x: '84%', y: '72%' }],
    icon: Activity,
  },
  {
    accent: '#7dd3fc',
    bars: [72, 46, 91, 63, 38, 84],
    lines: ['curve.compare()', 'matrix.trace()', 'signal.focus()'],
    nodes: [{ x: '16%', y: '62%' }, { x: '38%', y: '34%' }, { x: '64%', y: '74%' }, { x: '82%', y: '24%' }],
    icon: Radar,
  },
  {
    accent: '#facc15',
    bars: [42, 76, 55, 92, 64, 48],
    lines: ['dataset.link()', 'version.check()', 'asset.map()'],
    nodes: [{ x: '22%', y: '32%' }, { x: '48%', y: '68%' }, { x: '62%', y: '24%' }, { x: '86%', y: '58%' }],
    icon: Boxes,
  },
  {
    accent: '#fb7185',
    bars: [82, 68, 44, 74, 56, 96],
    lines: ['summary.ready', 'risk.review', 'next.plan'],
    nodes: [{ x: '14%', y: '52%' }, { x: '32%', y: '24%' }, { x: '68%', y: '66%' }, { x: '84%', y: '34%' }],
    icon: Sparkles,
  },
]

const taskIcons = [Terminal, GitPullRequest, ShieldCheck, Code2]

const landingCopy: Record<'zh' | 'en', LandingCopy> = {
  zh: {
    heroKicker: '深度学习可视化分析平台',
    heroCopy: '集中管理训练、分析、预测、知识库和 AI 对话。每一次实验都有数据来源、运行状态、分析结果和后续记录。',
    primaryAction: '进入工作台',
    secondaryAction: '查看能力',
    consoleRun: '运行监控',
    consoleSections: '04 功能区',
    metrics: [
      { value: '4', label: '核心工作流' },
      { value: '12+', label: '可视化视图' },
      { value: '24h', label: '长任务监控' },
      { value: '1', label: '统一实验台' },
    ],
    workEyebrow: '实验流程',
    workTitle: '从实验想法到可验证结论，全链路并行推进。',
    disciplineEyebrow: '数据与记录',
    disciplineTitle: '把实验状态、指标变化和分析结论放在同一条链路里。',
    disciplineCopy: '训练任务、可视化指标、模型分析和知识记录会围绕同一批实验数据组织。你可以看到运行进度、关键曲线、风险提示和保存下来的复盘结果。',
    studioEyebrow: '可视化工作区',
    studioTitle: '用实时图表和分析面板查看训练全过程。',
    openDashboard: '打开仪表盘',
    closingTitle: '从训练数据到分析结论，都保留清晰记录。',
    closingAction: '开始使用',
    cases: [
      {
        title: '训练调度',
        tag: 'Training',
        signal: 'epochs advancing / loss stabilizing',
        desc: '并行观察训练任务、超参变化、指标曲线和资源状态。',
        detail: '把训练过程呈现为可追踪的任务流，实时暴露 loss、accuracy、epoch、设备和配置变化。',
      },
      {
        title: '模型分析',
        tag: 'Analysis',
        signal: 'comparing 12 model snapshots',
        desc: '把曲线、混淆矩阵、可视化结果和指标解释放在同一层。',
        detail: '指标、图形和解释被聚合成可行动结论，帮助研究者更快判断模型状态。',
      },
      {
        title: '数据工作流',
        tag: 'Data',
        signal: 'dataset lineage verified',
        desc: '追踪数据集、上传、版本和训练引用关系。',
        detail: '数据不是孤立表格，而是连接训练、预测、知识库和讨论资产的可追溯资源。',
      },
      {
        title: 'AI 辅助分析',
        tag: 'AI',
        signal: 'analysis context ready',
        desc: '在分析页、知识库和实验页之间延续同一份分析材料。',
        detail: 'AI 会围绕已保存的指标、日志和复盘记录生成状态总结、风险解释和下一步实验建议。',
      },
    ],
    tasks: [
      { label: '训练任务采样', state: 'run' },
      { label: '指标差异归纳', state: 'diff' },
      { label: '异常风险检查', state: 'safe' },
      { label: '报告草稿生成', state: 'draft' },
    ],
    flow: [
      { index: '01', title: '定义目标', text: '上传数据、选择模型、设定训练策略。' },
      { index: '02', title: '并行运行', text: '训练、分析、预测和可视化同时推进。' },
      { index: '03', title: '审查质量', text: '用曲线、日志和评估结果判断风险。' },
      { index: '04', title: '沉淀知识', text: '把结论回写到知识库与讨论资产。' },
    ],
  },
  en: {
    heroKicker: 'Deep Learning Visual Analytics Platform',
    heroCopy: 'Manage training, analysis, inference, knowledge records, and AI conversations in one place. Every experiment keeps its data source, run state, analysis result, and follow-up record.',
    primaryAction: 'Enter Workspace',
    secondaryAction: 'View Capabilities',
    consoleRun: 'RUN MONITOR',
    consoleSections: '04 sections',
    metrics: [
      { value: '4', label: 'Core workflows' },
      { value: '12+', label: 'Visual views' },
      { value: '24h', label: 'Long-run monitoring' },
      { value: '1', label: 'Unified experiment desk' },
    ],
    workEyebrow: 'Experiment workflow',
    workTitle: 'From experiment ideas to verified conclusions, every layer moves in parallel.',
    disciplineEyebrow: 'Data and records',
    disciplineTitle: 'Keep experiment state, metric changes, and analysis conclusions in one traceable flow.',
    disciplineCopy: 'Training jobs, visual metrics, model analysis, and knowledge records are organized around the same experiment data. You can inspect progress, key curves, risk signals, and saved review results.',
    studioEyebrow: 'Visual workspace',
    studioTitle: 'Inspect the full training process with live charts and analysis panels.',
    openDashboard: 'Open Dashboard',
    closingTitle: 'Keep a clear record from training data to analysis conclusions.',
    closingAction: 'Start Now',
    cases: [
      {
        title: 'Training Orchestration',
        tag: 'Training',
        signal: 'epochs advancing / loss stabilizing',
        desc: 'Observe training jobs, hyperparameters, metric curves, and resource state in parallel.',
        detail: 'Training becomes a traceable task stream with live loss, accuracy, epoch, device, and configuration changes.',
      },
      {
        title: 'Model Analysis',
        tag: 'Analysis',
        signal: 'comparing 12 model snapshots',
        desc: 'Put curves, confusion matrices, visual outputs, and metric interpretation on the same layer.',
        detail: 'Metrics, graphics, and explanations are aggregated into actionable conclusions so researchers can judge model state faster.',
      },
      {
        title: 'Data Workflow',
        tag: 'Data',
        signal: 'dataset lineage verified',
        desc: 'Trace datasets, uploads, versions, and the training runs that reference them.',
        detail: 'Data is not an isolated table. It is a traceable resource linked to training, inference, knowledge, and discussion assets.',
      },
      {
        title: 'AI-Assisted Analysis',
        tag: 'AI',
        signal: 'analysis context ready',
        desc: 'Carry the same analysis material across analysis, knowledge, and experiment pages.',
        detail: 'AI uses saved metrics, logs, and review records to produce status summaries, risk explanations, and next experiment suggestions.',
      },
    ],
    tasks: [
      { label: 'Training sample stream', state: 'run' },
      { label: 'Metric delta summary', state: 'diff' },
      { label: 'Anomaly risk check', state: 'safe' },
      { label: 'Report draft generation', state: 'draft' },
    ],
    flow: [
      { index: '01', title: 'Define Goals', text: 'Upload data, choose a model, and set the training strategy.' },
      { index: '02', title: 'Run In Parallel', text: 'Training, analysis, inference, and visualization move together.' },
      { index: '03', title: 'Review Quality', text: 'Use curves, logs, and evaluation results to assess risk.' },
      { index: '04', title: 'Capture Knowledge', text: 'Write conclusions back to the knowledge base and discussion assets.' },
    ],
  },
}

const localeKey = computed<'zh' | 'en'>(() => locale.value.startsWith('en') ? 'en' : 'zh')
const pageCopy = computed(() => landingCopy[localeKey.value])
const cases = computed<CaseItem[]>(() => pageCopy.value.cases.map((item, index) => ({
  ...item,
  ...caseVisuals[index],
})))
const activeCaseIndex = ref(0)
const activeCase = computed(() => cases.value[activeCaseIndex.value] || cases.value[0])
const metrics = computed(() => pageCopy.value.metrics)
const tasks = computed(() => pageCopy.value.tasks.map((task, index) => ({
  ...task,
  icon: taskIcons[index],
})))
const flow = computed(() => pageCopy.value.flow)
const signalNodes = [
  { id: 1, x: '14%', y: '28%' },
  { id: 2, x: '33%', y: '58%' },
  { id: 3, x: '52%', y: '35%' },
  { id: 4, x: '71%', y: '64%' },
  { id: 5, x: '84%', y: '24%' },
]

function scrollTo(id: string) {
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth' })
}

function flushPointerVars() {
  pointerRaf = 0
  const root = landingRootRef.value
  if (!root) return
  root.style.setProperty('--mx', pendingPointerCssX)
  root.style.setProperty('--my', pendingPointerCssY)
  root.style.setProperty('--tilt-x', pendingTiltX)
  root.style.setProperty('--tilt-y', pendingTiltY)
}

function handlePointerMove(event: PointerEvent) {
  pointer.x = event.clientX / window.innerWidth - 0.5
  pointer.y = event.clientY / window.innerHeight - 0.5
  pendingPointerCssX = `${Math.round(event.clientX)}px`
  pendingPointerCssY = `${Math.round(event.clientY)}px`
  pendingTiltX = `${(-pointer.y * 7).toFixed(2)}deg`
  pendingTiltY = `${(pointer.x * 9).toFixed(2)}deg`
  if (!pointerRaf) pointerRaf = window.requestAnimationFrame(flushPointerVars)
}

function handleWorkCardPointer(event: PointerEvent, index: number) {
  const rect = (event.currentTarget as HTMLElement).getBoundingClientRect()
  const x = Math.max(12, Math.min(88, ((event.clientX - rect.left) / rect.width) * 100))
  const y = Math.max(14, Math.min(84, ((event.clientY - rect.top) / rect.height) * 100))
  const current = workCardPointers.value[index]
  if (Math.abs(current.x - x) < 0.4 && Math.abs(current.y - y) < 0.4) return
  workCardPointers.value[index] = { x, y }
}

function resetWorkCardPointer(index: number) {
  const defaults = [
    { x: 42, y: 30 },
    { x: 52, y: 34 },
    { x: 48, y: 36 },
    { x: 56, y: 28 },
  ]
  workCardPointers.value[index] = defaults[index] || { x: 50, y: 32 }
}

onMounted(() => {
  void nextTick(() => {
    window.scrollTo({ top: 0, left: 0, behavior: 'auto' })
  })
  flushPointerVars()
})
onUnmounted(() => {
  if (pointerRaf) window.cancelAnimationFrame(pointerRaf)
})
</script>

<style scoped>
.di-landing {
  min-height: 100vh;
  overflow-x: hidden;
  background: var(--bg-color);
  color: var(--text-primary);
  --mx: 50%;
  --my: 50%;
  --tilt-x: 0deg;
  --tilt-y: 0deg;
  --flowing-menu-bg: var(--surface-solid);
}

.di-landing,
.di-landing * {
  box-sizing: border-box;
}

.hero-section {
  position: relative;
  min-height: calc(100svh - 72px);
  padding: 92px 28px clamp(88px, 11svh, 132px);
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(300px, 430px);
  align-items: end;
  gap: 36px;
  isolation: isolate;
}

.hero-shader-field,
.hero-grid {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.hero-shader-field {
  z-index: -5;
  opacity: 0.98;
  mix-blend-mode: screen;
  mask-image: linear-gradient(180deg, rgba(0,0,0,0.95), rgba(0,0,0,0.74) 76%, rgba(0,0,0,0.22) 96%, transparent 100%);
  -webkit-mask-image: linear-gradient(180deg, rgba(0,0,0,0.95), rgba(0,0,0,0.74) 76%, rgba(0,0,0,0.22) 96%, transparent 100%);
}

:global(.light) .hero-shader-field {
  display: none !important;
  opacity: 0.44;
  mix-blend-mode: normal;
}

.hero-grid {
  z-index: -4;
  background-image:
    linear-gradient(90deg, rgba(var(--primary-rgb), 0.055) 1px, transparent 1px),
    linear-gradient(0deg, rgba(var(--primary-rgb), 0.045) 1px, transparent 1px);
  background-size: 74px 74px;
  mask-image: linear-gradient(180deg, rgba(0, 0, 0, 0.88), rgba(0, 0, 0, 0.76) 78%, rgba(0, 0, 0, 0.18) 96%, transparent 100%);
}

:global(.light) .hero-grid {
  opacity: 0.38;
  background-image:
    linear-gradient(90deg, rgba(var(--primary-rgb), 0.025) 1px, transparent 1px),
    linear-gradient(0deg, rgba(var(--primary-rgb), 0.02) 1px, transparent 1px);
}

.hero-grid::before {
  content: "";
  position: absolute;
  left: -12%;
  right: -12%;
  bottom: -4%;
  height: 74%;
  background-image:
    linear-gradient(90deg, rgba(var(--primary-rgb), 0.1) 1px, transparent 1px),
    linear-gradient(0deg, rgba(var(--primary-rgb), 0.08) 1px, transparent 1px);
  background-size: 58px 58px;
  transform-origin: center bottom;
  transform: perspective(1200px) rotateX(76deg);
  opacity: 0.42;
  mask-image: linear-gradient(180deg, rgba(0, 0, 0, 0.98), transparent 92%);
  animation: perspective-grid-drift 18s linear infinite;
}

:global(.light) .hero-grid::before {
  opacity: 0.18;
  background-image:
    linear-gradient(90deg, rgba(var(--primary-rgb), 0.045) 1px, transparent 1px),
    linear-gradient(0deg, rgba(var(--primary-rgb), 0.032) 1px, transparent 1px);
}

.hero-grid::after {
  content: "";
  position: absolute;
  inset: auto 12% 12% 12%;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(var(--primary-rgb), 0.74), transparent);
  box-shadow: 0 0 18px rgba(var(--primary-rgb), 0.28);
  opacity: 0.72;
}

:global(.light) .hero-grid::after {
  opacity: 0.24;
  box-shadow: 0 0 14px rgba(var(--primary-rgb), 0.12);
}

.hero-section::after {
  content: "";
  position: absolute;
  inset: 0;
  z-index: -1;
  pointer-events: none;
  background:
    radial-gradient(620px circle at var(--mx) var(--my), rgba(var(--primary-rgb), 0.2), transparent 44%),
    linear-gradient(180deg, transparent 58%, var(--bg-color));
}

:global(.light) .hero-section::after {
  background:
    radial-gradient(620px circle at var(--mx) var(--my), rgba(var(--primary-rgb), 0.1), transparent 50%),
    linear-gradient(180deg, transparent 48%, var(--bg-color));
}

.hero-shell {
  max-width: 920px;
  padding-bottom: 24px;
  min-width: 0;
}

.hero-kicker,
.section-heading span,
.narrative-copy span,
.console-topline,
.feed-header {
  font-size: 12px;
  line-height: 1;
  font-weight: var(--font-weight-label);
  letter-spacing: 0;
  text-transform: uppercase;
  color: var(--text-secondary);
}

.hero-kicker {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 28px;
  color: var(--text-primary);
}

.signal-dot {
  width: 9px;
  height: 9px;
  border-radius: 999px;
  background: var(--primary-color);
  box-shadow: 0 0 22px var(--primary-glow);
}

h1,
h2,
h3,
p {
  margin: 0;
}

.hero-shell h1 {
  width: max-content;
  max-width: min(100%, 12ch);
  overflow: visible;
  font-size: 96px;
  line-height: 1.08;
  font-weight: var(--font-weight-title);
  letter-spacing: 0;
}

.hero-shiny-title {
  padding: 0.08em 0.1em 0.16em 0;
  background-size: 260% auto;
  filter:
    drop-shadow(0 18px 52px rgba(var(--primary-rgb), 0.28))
    drop-shadow(0 0 24px rgba(var(--primary-rgb), 0.18));
}

.hero-copy {
  max-width: 700px;
  margin-top: 22px;
  color: color-mix(in srgb, #ffffff 72%, var(--primary-color));
  font-size: 20px;
  line-height: 1.62;
  text-shadow:
    0 0 22px rgba(var(--primary-rgb), 0.14),
    0 2px 12px rgba(0, 0, 0, 0.28);
}

.hero-code-ribbon,
.console-code-block,
.work-card-code {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.hero-code-ribbon {
  margin-top: 26px;
}

.hero-code-ribbon span,
.console-code-block span,
.work-card-code span {
  padding: 5px 8px;
  border: 1px solid rgba(var(--primary-rgb), 0.16);
  border-radius: 9px;
  background: rgba(var(--glass-bg-rgb), 0.24);
  color: var(--text-secondary);
  font-family: "JetBrains Mono", Consolas, monospace;
  font-size: 10px;
  font-weight: var(--font-weight-body);
  white-space: nowrap;
  backdrop-filter: blur(12px);
}

.hero-actions,
.closing-band {
  display: flex;
  align-items: center;
  gap: 12px;
}

.hero-actions {
  flex-wrap: wrap;
  margin-top: 34px;
}

.primary-action,
.secondary-action,
.text-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  min-width: 0;
  height: 46px;
  border-radius: var(--radius-sm);
  padding: 0 18px;
  border: 1px solid var(--border-strong);
  font: inherit;
  font-size: 14px;
  font-weight: var(--font-weight-label);
  color: var(--text-primary);
  background: var(--surface-2);
  cursor: pointer;
  transition: transform 180ms ease, border-color 180ms ease, background 180ms ease, box-shadow 180ms ease;
}

.primary-action {
  border-color: var(--primary-color);
  background: var(--primary-color);
  color: #06100c;
  box-shadow: 0 16px 42px rgba(var(--primary-rgb), 0.28);
}

.primary-action:hover,
.secondary-action:hover,
.text-action:hover {
  transform: translateY(-2px);
  border-color: var(--primary-color);
  box-shadow: var(--shadow-hover);
}

.hero-console {
  position: relative;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  padding: 18px;
  background: rgba(var(--glass-bg-rgb), 0.76);
  backdrop-filter: blur(18px) saturate(170%);
  box-shadow: var(--shadow-soft);
  transform: perspective(900px) rotateX(var(--tilt-x)) rotateY(var(--tilt-y));
  transform-origin: center;
  transition: border-color 180ms ease, box-shadow 180ms ease;
}

.hero-console::before {
  content: "";
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(280px circle at var(--mx) var(--my), rgba(var(--primary-rgb), 0.14), transparent 48%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.06), transparent 28%);
}

.hero-console:hover {
  border-color: rgba(var(--primary-rgb), 0.42);
  box-shadow: var(--shadow-hover);
}

.console-topline,
.feed-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: var(--text-muted);
}

.console-topline span,
.feed-header span,
.task-row span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.console-stage {
  min-height: 156px;
  margin: 18px 0;
  padding: 18px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  display: grid;
  grid-template-columns: 92px 1fr;
  gap: 16px;
  align-items: center;
  background:
    linear-gradient(135deg, rgba(var(--primary-rgb), 0.12), transparent 54%),
    var(--surface-2);
}

.console-stage strong {
  display: block;
  margin-bottom: 8px;
  font-size: 22px;
}

.console-stage p {
  color: var(--text-secondary);
  line-height: 1.45;
}

.stage-radar {
  position: relative;
  width: 82px;
  aspect-ratio: 1;
  border: 1px solid rgba(var(--primary-rgb), 0.42);
  border-radius: 50%;
}

.stage-radar span {
  position: absolute;
  inset: 12px;
  border: 1px solid rgba(var(--primary-rgb), 0.3);
  border-radius: 50%;
  animation: radar-pulse 2.5s ease-in-out infinite;
}

.stage-radar span:nth-child(2) {
  inset: 24px;
  animation-delay: 300ms;
}

.stage-radar span:nth-child(3) {
  inset: 36px;
  background: var(--primary-color);
  box-shadow: 0 0 18px var(--primary-glow);
}

.task-list {
  display: grid;
  gap: 10px;
}

.console-code-block {
  position: relative;
  z-index: 1;
  margin-top: 14px;
}

.task-row {
  height: 44px;
  display: grid;
  grid-template-columns: 22px minmax(0, 1fr);
  align-items: center;
  gap: 10px;
  padding: 0 12px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  background: var(--surface-2);
  min-width: 0;
}

.proof-strip {
  position: relative;
  z-index: 5;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  border-top: 1px solid var(--border-color);
  border-bottom: 1px solid var(--border-color);
  background: var(--surface-solid);
  color: var(--text-primary);
  box-shadow: 0 -1px 0 var(--border-color), 0 18px 42px rgba(0, 0, 0, 0.04);
}

.fluid-stage-band {
  position: relative;
  overflow: hidden;
  border-bottom: 1px solid var(--border-color);
  background:
    radial-gradient(1200px circle at 24% 38%, rgba(var(--primary-rgb), 0.24), transparent 58%),
    radial-gradient(980px circle at 78% 26%, color-mix(in srgb, var(--accent-glow) 24%, transparent), transparent 60%),
    linear-gradient(
      180deg,
      color-mix(in srgb, var(--bg-color) 82%, rgba(var(--primary-rgb), 0.18)) 0%,
      color-mix(in srgb, var(--bg-color) 70%, rgba(var(--primary-rgb), 0.18)) 52%,
      color-mix(in srgb, var(--bg-color) 66%, var(--surface-1) 34%) 100%
    );
}

.fluid-stage-band :deep(.fluid-glass-stage) {
  min-height: clamp(1780px, 258svh, 2520px);
}

.metric-cell {
  position: relative;
  min-height: 132px;
  padding: 24px 28px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 14px;
  border-right: 1px solid var(--border-color);
  overflow: visible;
}

.metric-cell:last-child {
  border-right: none;
}

.metric-cell strong {
  display: block;
  font-size: 40px;
  line-height: 1;
  white-space: nowrap;
}

.metric-cell span {
  display: block;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.35;
  font-weight: var(--font-weight-label);
  overflow-wrap: anywhere;
}

.section-band {
  padding: 104px 28px;
  border-bottom: 1px solid var(--border-color);
}

.section-heading {
  max-width: 860px;
  margin-bottom: 44px;
}

.section-heading h2,
.narrative-copy h2,
.closing-band h2 {
  margin-top: 14px;
  font-size: 46px;
  line-height: 1.08;
  font-weight: var(--font-weight-title);
  letter-spacing: 0;
}

.work-band {
  background:
    radial-gradient(680px circle at var(--mx) var(--my), rgba(var(--primary-rgb), 0.09), transparent 42%),
    var(--bg-color);
}

.work-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.work-card {
  position: relative;
  min-height: 430px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  background: var(--surface-1);
  overflow: hidden;
  outline: none;
  transition: transform 220ms ease, border-color 220ms ease, box-shadow 220ms ease;
}

.work-card::before {
  content: "";
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(220px circle at var(--card-glow-x, 50%) var(--card-glow-y, 32%), color-mix(in srgb, var(--accent) 18%, transparent), transparent 56%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.035), transparent 28%);
  opacity: 0.86;
}

.work-card.active,
.work-card:hover {
  transform: translateY(-6px) scale(1.01);
  border-color: var(--accent);
  box-shadow: 0 24px 70px color-mix(in srgb, var(--accent) 18%, transparent);
  z-index: 2;
}

.work-card-visual {
  position: relative;
  height: 236px;
  padding: 20px 20px 68px;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  color: var(--accent);
  background:
    linear-gradient(135deg, color-mix(in srgb, var(--accent) 18%, transparent), transparent 54%),
    repeating-linear-gradient(90deg, rgba(var(--primary-rgb), 0.08) 0 1px, transparent 1px 28px),
    var(--surface-3);
}

.work-card-grid,
.work-card-spot,
.work-card-orbit,
.work-card-code,
.visual-bars,
.work-card-visual > :deep(svg) {
  position: relative;
  z-index: 1;
}

.work-card-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(90deg, color-mix(in srgb, var(--accent) 12%, transparent) 1px, transparent 1px),
    linear-gradient(0deg, rgba(148, 163, 184, 0.12) 1px, transparent 1px);
  background-size: 24px 24px;
  opacity: 0.54;
  mask-image: radial-gradient(circle at var(--card-glow-x, 50%) var(--card-glow-y, 32%), black, transparent 78%);
}

.work-card-spot {
  position: absolute;
  inset: 0;
  background: radial-gradient(180px circle at var(--card-glow-x, 50%) var(--card-glow-y, 32%), color-mix(in srgb, var(--accent) 20%, transparent), transparent 58%);
}

.work-card-orbit {
  position: absolute;
  inset: 16px 16px 72px;
}

.work-card-orbit span {
  position: absolute;
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: var(--accent);
  box-shadow: 0 0 16px color-mix(in srgb, var(--accent) 46%, transparent);
  transform: translate(-50%, -50%);
  animation: work-node-pulse 3.4s ease-in-out infinite;
}

.work-card-orbit span:nth-child(2) { animation-delay: 380ms; }
.work-card-orbit span:nth-child(3) { animation-delay: 820ms; }
.work-card-orbit span:nth-child(4) { animation-delay: 1160ms; }

.visual-bars {
  height: 132px;
  display: flex;
  align-items: end;
  gap: 8px;
}

.visual-bars span {
  width: 12px;
  min-height: 16px;
  border-radius: 6px 6px 0 0;
  background: var(--accent);
  opacity: 0.84;
}

.work-card-code {
  position: absolute;
  left: 18px;
  right: 18px;
  bottom: 14px;
}

.work-card-code span {
  color: color-mix(in srgb, var(--accent) 72%, var(--text-secondary));
  border-color: color-mix(in srgb, var(--accent) 20%, transparent);
  background: rgba(var(--glass-bg-rgb), 0.18);
}

.work-card-copy {
  padding: 22px;
}

.work-card-copy span {
  display: block;
  margin-bottom: 14px;
  color: var(--text-muted);
  font-size: 12px;
  font-weight: var(--font-weight-title);
}

.work-card-copy h3 {
  margin-bottom: 12px;
  font-size: 24px;
  line-height: 1.12;
}

.work-card-copy p {
  color: var(--text-secondary);
  line-height: 1.62;
  overflow-wrap: anywhere;
}

.narrative-band {
  display: grid;
  grid-template-columns: minmax(0, 0.9fr) minmax(360px, 1fr);
  gap: 52px;
  align-items: start;
  background: var(--surface-solid);
  color: var(--text-primary);
}

.narrative-copy p {
  max-width: 640px;
  margin-top: 24px;
  color: var(--text-secondary);
  font-size: 17px;
  line-height: 1.74;
}

.flow-panel {
  display: grid;
  border-top: 1px solid var(--border-color);
}

.flow-step {
  min-height: 118px;
  display: grid;
  grid-template-columns: 72px 1fr;
  gap: 20px;
  align-items: center;
  border-bottom: 1px solid var(--border-color);
}

.flow-index {
  color: var(--text-muted);
  font-weight: var(--font-weight-title);
}

.flow-step h3 {
  margin-bottom: 8px;
  font-size: 20px;
}

.flow-step p {
  color: var(--text-secondary);
  line-height: 1.5;
}

.compact {
  max-width: 740px;
}

.studio-band {
  background:
    radial-gradient(520px circle at 84% 16%, rgba(var(--primary-rgb), 0.1), transparent 44%),
    var(--bg-color);
}

.studio-wall {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(280px, 0.85fr);
  gap: 18px;
  align-items: stretch;
}

.studio-feed,
.studio-copy {
  min-width: 0;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  background: var(--surface-1);
}

.studio-feed {
  padding: 18px;
}

.feed-screen {
  position: relative;
  height: 430px;
  margin-top: 18px;
  overflow: hidden;
  border-radius: var(--radius-sm);
  background:
    radial-gradient(300px circle at var(--mx) var(--my), rgba(var(--primary-rgb), 0.26), transparent 40%),
    linear-gradient(90deg, rgba(var(--primary-rgb), 0.1) 1px, transparent 1px),
    linear-gradient(0deg, rgba(var(--primary-rgb), 0.1) 1px, transparent 1px),
    radial-gradient(circle at 68% 38%, rgba(var(--primary-rgb), 0.18), transparent 28%),
    var(--surface-3);
  background-size: auto, 42px 42px, 42px 42px, auto, auto;
}

.signal-map span {
  position: absolute;
  width: 12px;
  height: 12px;
  border-radius: 999px;
  background: var(--primary-color);
  box-shadow: 0 0 24px var(--primary-glow);
  transform: translate(-50%, -50%);
  animation: soft-pulse 2.8s ease-in-out infinite;
}

.signal-map span:nth-child(2) { animation-delay: 260ms; background: var(--accent-glow); }
.signal-map span:nth-child(3) { animation-delay: 520ms; background: var(--warning-glow); }
.signal-map span:nth-child(4) { animation-delay: 780ms; background: var(--danger-glow); }
.signal-map span:nth-child(5) { animation-delay: 1040ms; }

.pulse-line {
  position: absolute;
  left: 8%;
  right: 8%;
  height: 2px;
  background: var(--primary-color);
  box-shadow: 0 0 22px var(--primary-glow);
  animation: scan 4s ease-in-out infinite;
}

.line-a { top: 30%; }
.line-b { top: 52%; background: var(--accent-glow); animation-delay: 900ms; }
.line-c { top: 74%; background: var(--danger-glow); animation-delay: 1600ms; }

.studio-copy {
  padding: 32px;
  display: flex;
  flex-direction: column;
  justify-content: end;
}

.studio-copy h3 {
  margin-bottom: 16px;
  font-size: 34px;
}

.studio-copy p {
  margin-bottom: 26px;
  color: var(--text-secondary);
  line-height: 1.72;
}

.text-action {
  width: fit-content;
}

.closing-band {
  min-height: 360px;
  padding: 72px 28px;
  justify-content: space-between;
  background: var(--surface-solid);
}

.closing-band h2 {
  max-width: 780px;
}

.dark .hero-console,
.dark .work-card,
.dark .studio-feed,
.dark .studio-copy {
  background: rgba(9, 12, 16, 0.76);
}

.dark .secondary-action,
.dark .text-action {
  background: rgba(5, 7, 10, 0.3);
}

@keyframes radar-pulse {
  0%,
  100% {
    opacity: 0.45;
    transform: scale(0.96);
  }
  50% {
    opacity: 1;
    transform: scale(1.04);
  }
}

@keyframes scan {
  0%,
  100% {
    opacity: 0.55;
    transform: translateX(-5%);
  }
  50% {
    opacity: 1;
    transform: translateX(5%);
  }
}

@keyframes perspective-grid-drift {
  from {
    background-position: 0 0, 0 0;
  }
  to {
    background-position: 0 58px, 58px 0;
  }
}

@keyframes work-node-pulse {
  0%,
  100% {
    opacity: 0.66;
    transform: translate(-50%, -50%) scale(0.92);
  }
  50% {
    opacity: 1;
    transform: translate(-50%, -50%) scale(1.18);
  }
}

@media (max-width: 1180px) {
  .hero-section,
  .narrative-band,
  .studio-wall {
    grid-template-columns: 1fr;
  }

  .hero-shell h1 {
    font-size: 72px;
  }

  .hero-console {
    width: min(100%, 640px);
    max-width: 640px;
    justify-self: start;
  }

  .work-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .hero-section {
    min-height: auto;
    padding: 84px 18px 34px;
    gap: 24px;
  }

  .hero-shell h1 {
    font-size: 48px;
  }

  .hero-copy {
    margin-top: 22px;
    font-size: 16px;
    line-height: 1.65;
  }

  .section-heading h2,
  .narrative-copy h2,
  .closing-band h2 {
    font-size: 31px;
    line-height: 1.14;
  }

  .hero-actions,
  .closing-band {
    align-items: stretch;
    flex-direction: column;
  }

  .primary-action,
  .secondary-action {
    width: 100%;
  }

  .console-stage {
    grid-template-columns: 1fr;
    min-height: auto;
  }

  .hero-console {
    width: 100%;
    max-width: none;
    transform: none;
  }

  .proof-strip,
  .work-grid {
    grid-template-columns: 1fr;
  }

  .metric-cell {
    min-height: 104px;
    border-right: none;
    border-bottom: 1px solid var(--border-color);
  }

  .section-band {
    padding: 72px 18px;
  }

  .fluid-stage-band :deep(.fluid-glass-stage) {
    min-height: clamp(1880px, 272svh, 2580px);
  }

  .work-card {
    min-height: 360px;
  }

  .feed-screen {
    height: 310px;
  }

  .closing-band {
    min-height: 320px;
  }
}

@media (max-width: 480px) {
  .hero-shell h1 {
    font-size: 40px;
  }

  .console-stage {
    padding: 14px;
  }

  .task-row {
    grid-template-columns: 20px minmax(0, 1fr);
    padding: 0 10px;
  }

  .flow-step {
    grid-template-columns: 1fr;
    gap: 10px;
    align-items: start;
    padding: 18px 0;
  }

  .studio-copy {
    padding: 24px;
  }
}
</style>
