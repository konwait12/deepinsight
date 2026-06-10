<template>
  <main
    ref="landingRootRef"
    class="di-landing"
    @pointermove="handlePointerMove"
  >
    <section class="hero-section" aria-labelledby="hero-title">
      <IridescenceField
        v-if="!themeStore.isDarkMode"
        class="hero-iridescence"
        :active="true"
        :speed="0.92"
        :amplitude="0.1"
        :opacity="1"
        :theme-strength="0.62"
        :mouse-react="true"
      />
      <ShaderField
        v-if="themeStore.isDarkMode"
        class="hero-shader-field"
        variant="hero"
        :dark="themeStore.isDarkMode"
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
      <div class="hero-day-map" aria-hidden="true">
        <span class="map-line line-1"></span>
        <span class="map-line line-2"></span>
        <span class="map-line line-3"></span>
        <span class="map-line line-4"></span>
        <span class="map-line line-5"></span>
        <span class="map-node node-1"></span>
        <span class="map-node node-2"></span>
        <span class="map-node node-3"></span>
        <span class="map-node node-4"></span>
        <span class="map-node node-5"></span>
        <span class="map-node node-6"></span>
      </div>
      <div class="hero-night-stage" aria-hidden="true">
        <span class="night-orbit orbit-a"></span>
        <span class="night-orbit orbit-b"></span>
        <span class="night-orbit orbit-c"></span>
        <span class="night-horizon"></span>
        <span class="night-spotlight"></span>
      </div>

      <div class="hero-shell entrance-hero">
        <div class="hero-kicker">
          <span class="signal-dot"></span>
          {{ pageCopy.heroKicker }}
        </div>

        <h1 id="hero-title">
          <ShinyText
            text="DeepInsight"
            class-name="hero-shiny-title"
            :color="themeStore.isDarkMode ? 'rgba(248, 253, 255, 0.92)' : 'rgba(18, 24, 31, 0.94)'"
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
          <button class="secondary-action" type="button" @click="scrollTo('capabilities')">
            {{ pageCopy.secondaryAction }}
            <Play :size="16" stroke-width="2.4" />
          </button>
        </div>
      </div>

      <aside class="hero-console entrance-scale" :aria-label="pageCopy.liveTaskAria">
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

    <section class="proof-strip" :aria-label="pageCopy.metricsAria">
      <div v-for="metric in metrics" :key="metric.label" class="metric-cell">
        <strong>{{ metric.value }}</strong>
        <span>{{ metric.label }}</span>
      </div>
    </section>

    <section id="capabilities" class="section-band showcase-band" :aria-label="pageCopy.productPreviewAria">
      <div class="showcase-shell">
        <div class="showcase-heading" data-landing-reveal>
          <span>{{ showcaseCopy.eyebrow }}</span>
          <h2>{{ showcaseCopy.title }}</h2>
          <p>{{ showcaseCopy.desc }}</p>
        </div>

        <div class="showcase-track">
          <article
            v-for="(item, index) in showcaseItems"
            :key="item.title"
            class="showcase-step"
            :class="{ reverse: index % 2 === 1 }"
            :style="{ '--accent': item.accent }"
          >
            <div class="showcase-copy">
              <div class="showcase-index">{{ item.index }}</div>
              <span>{{ item.eyebrow }}</span>
              <h3>{{ item.title }}</h3>
              <p>{{ item.desc }}</p>
              <div class="showcase-tags">
                <i v-for="tag in item.tags" :key="tag">{{ tag }}</i>
              </div>
              <button class="text-action showcase-action" type="button" @click="openShowcaseItem(item)">
                {{ item.action }}
                <ArrowRight :size="15" stroke-width="2.5" />
              </button>
            </div>

            <article
              class="preview-window"
              role="button"
              tabindex="0"
              :aria-label="item.action"
              @click="openShowcaseItem(item)"
              @keydown.enter.prevent="openShowcaseItem(item)"
              @keydown.space.prevent="openShowcaseItem(item)"
            >
              <div class="preview-toolbar">
                <span class="window-dots" aria-hidden="true">
                  <i></i>
                  <i></i>
                  <i></i>
                </span>
                <strong>{{ item.preview.title }}</strong>
                <em>{{ item.preview.mode }}</em>
              </div>

              <div class="preview-stage" :class="`preview-${item.preview.kind}`">
                <template v-if="item.preview.kind === 'catalog'">
                  <div class="mock-hero mock-catalog-hero">
                    <div>
                      <span>{{ previewText.catalogTitle }}</span>
                      <strong>{{ item.preview.title }}</strong>
                      <small>{{ item.desc }}</small>
                    </div>
                    <div class="mock-summary-grid">
                      <article v-for="metric in item.preview.metrics.slice(0, 3)" :key="metric.label">
                        <span>{{ metric.label }}</span>
                        <b>{{ metric.value }}</b>
                      </article>
                    </div>
                  </div>
                  <div class="mock-catalog-shell">
                    <aside class="mock-model-list">
                      <header>
                        <span>{{ previewText.modelStatus }}</span>
                        <b>{{ item.preview.metrics[0]?.value }}</b>
                      </header>
                      <div
                        v-for="(row, rowIndex) in item.preview.rows"
                        :key="row.name"
                        class="mock-model-row"
                        :class="{ active: rowIndex === 0 }"
                      >
                        <i></i>
                        <strong>{{ row.name }}</strong>
                        <span>{{ row.meta }}</span>
                        <em>{{ row.value }}</em>
                      </div>
                    </aside>
                    <main class="mock-model-detail">
                      <div class="mock-detail-head">
                        <div>
                          <span>{{ item.preview.rows[0]?.meta }}</span>
                          <strong>{{ item.preview.rows[0]?.name }}</strong>
                          <small>{{ item.preview.rows[0]?.value }}</small>
                        </div>
                        <b>{{ item.preview.mode }}</b>
                      </div>
                      <div class="mock-kpi-grid">
                        <article v-for="metric in item.preview.metrics" :key="metric.label">
                          <span>{{ metric.label }}</span>
                          <b>{{ metric.value }}</b>
                        </article>
                      </div>
                      <div class="mock-profile-grid">
                        <div>
                          <span>{{ previewText.profile }}</span>
                          <i v-for="bar in item.preview.bars.slice(0, 4)" :key="bar" :style="{ width: `${bar}%` }"></i>
                        </div>
                        <div>
                          <span>{{ previewText.evidence }}</span>
                          <p v-for="note in item.preview.notes" :key="note">{{ note }}</p>
                        </div>
                      </div>
                    </main>
                  </div>
                </template>

                <template v-else-if="item.preview.kind === 'testing'">
                  <div class="mock-hero mock-prediction-hero">
                    <div>
                      <span>{{ previewText.testTitle }}</span>
                      <strong>{{ item.preview.title }}</strong>
                      <small>{{ item.desc }}</small>
                    </div>
                    <div class="mock-mode-card">
                      <span>{{ previewText.currentMode }}</span>
                      <b>{{ item.preview.tabs[0] }}</b>
                      <em>{{ item.preview.mode }}</em>
                    </div>
                  </div>
                  <div class="mock-summary-grid three">
                    <article v-for="metric in item.preview.metrics.slice(0, 3)" :key="metric.label">
                      <span>{{ metric.label }}</span>
                      <b>{{ metric.value }}</b>
                    </article>
                  </div>
                  <div class="mock-tabs">
                    <span v-for="tab in item.preview.tabs" :key="tab" :class="{ active: tab === item.preview.tabs[0] }">{{ tab }}</span>
                  </div>
                  <div class="mock-test-shell">
                    <aside class="mock-control-panel">
                      <strong>{{ previewText.controlPanel }}</strong>
                      <label v-for="metric in item.preview.metrics.slice(0, 3)" :key="metric.label">
                        <span>{{ metric.label }}</span>
                        <i>{{ metric.value }}</i>
                      </label>
                    </aside>
                    <main class="mock-task-panel">
                      <div class="mock-request-card">
                        <span>{{ previewText.requestPanel }}</span>
                        <strong>{{ previewText.compareInput }}</strong>
                        <p>{{ previewText.sequenceInput }}</p>
                      </div>
                      <div class="mock-result-grid">
                        <article v-for="row in item.preview.rows" :key="row.name">
                          <header>
                            <strong>{{ row.name }}</strong>
                            <b>{{ row.value }}</b>
                          </header>
                          <span>{{ row.meta }}</span>
                          <em>{{ item.preview.mode }}</em>
                        </article>
                      </div>
                    </main>
                  </div>
                </template>

                <template v-else-if="item.preview.kind === 'analytics'">
                  <div class="mock-viz-header">
                    <div>
                      <span>{{ previewText.analysisTitle }}</span>
                      <strong>{{ item.preview.title }}</strong>
                      <small>{{ item.desc }}</small>
                    </div>
                    <b>{{ item.preview.metrics.length }}</b>
                  </div>
                  <div class="mock-builder">
                    <main>
                      <span>{{ previewText.analysisTarget }}</span>
                      <strong>{{ previewText.selectModel }}</strong>
                      <div class="mock-select">{{ previewText.modelExamples }}</div>
                    </main>
                    <aside>
                      <span>{{ previewText.analysisModules }}</span>
                      <div>
                        <i v-for="tab in item.preview.tabs" :key="tab">{{ tab }}</i>
                      </div>
                    </aside>
                  </div>
                  <div class="mock-status-band">
                    <article v-for="metric in item.preview.metrics" :key="metric.label">
                      <span>{{ metric.label }}</span>
                      <b>{{ metric.value }}</b>
                    </article>
                  </div>
                  <div class="mock-evidence-board">
                    <aside>
                      <span v-for="note in item.preview.notes" :key="note">{{ note }}</span>
                    </aside>
                    <main>
                      <div class="mock-chart-panel">
                        <i v-for="bar in item.preview.bars" :key="bar" :style="{ height: `${bar}%` }"></i>
                      </div>
                      <div class="mock-evidence-list">
                        <p v-for="row in item.preview.rows" :key="row.name">
                          <span>{{ row.name }}</span>
                          <b>{{ row.value }}</b>
                        </p>
                      </div>
                    </main>
                  </div>
                </template>

                <template v-else-if="item.preview.kind === 'data'">
                  <div class="mock-hero mock-data-hero">
                    <div>
                      <span>{{ previewText.dataTitle }}</span>
                      <strong>{{ item.preview.title }}</strong>
                      <small>{{ item.desc }}</small>
                    </div>
                    <div class="mock-actions">
                      <i>{{ previewText.refreshAssets }}</i>
                      <i>{{ previewText.datasetViz }}</i>
                    </div>
                  </div>
                  <div class="mock-dataset-section">
                    <div class="mock-field-bars">
                      <i v-for="bar in item.preview.bars" :key="bar" :style="{ height: `${bar}%` }"></i>
                    </div>
                    <div class="mock-summary-grid">
                      <article v-for="metric in item.preview.metrics" :key="metric.label">
                        <span>{{ metric.label }}</span>
                        <b>{{ metric.value }}</b>
                      </article>
                    </div>
                  </div>
                  <div class="mock-asset-workbench">
                    <aside>
                      <strong>{{ previewText.assetScope }}</strong>
                      <span v-for="tab in item.preview.tabs" :key="tab">{{ tab }}</span>
                    </aside>
                    <main>
                      <div class="mock-search">{{ previewText.searchPlaceholder }}</div>
                      <div v-for="row in item.preview.rows" :key="row.name" class="mock-asset-row">
                        <b>{{ previewText.dataBadge }}</b>
                        <strong>{{ row.name }}</strong>
                        <em>{{ row.value }}</em>
                      </div>
                    </main>
                    <section>
                      <span>{{ previewText.assetDetail }}</span>
                      <strong>{{ item.preview.rows[0]?.name }}</strong>
                      <p>{{ item.preview.notes.join(' / ') }}</p>
                    </section>
                  </div>
                </template>

                <template v-else>
                  <div class="mock-ai-shell">
                    <aside class="mock-ai-sidebar">
                      <div class="mock-ai-brand">
                        <i></i>
                        <strong>DeepInsight AI</strong>
                        <span>{{ previewText.aiSubtitle }}</span>
                      </div>
                      <b>{{ previewText.newChat }}</b>
                      <div class="mock-conversation-list">
                        <p v-for="row in item.preview.rows.slice(0, 2)" :key="row.name">
                          <strong>{{ row.name }}</strong>
                          <span>{{ row.meta }}</span>
                        </p>
                      </div>
                      <footer>
                        <span>{{ previewText.reasoning }}</span>
                        <strong>{{ item.preview.metrics[0]?.value }}</strong>
                      </footer>
                    </aside>
                    <main class="mock-ai-main">
                      <header>
                        <div>
                          <span>{{ previewText.aiWorkspace }}</span>
                          <strong>{{ item.preview.title }}</strong>
                        </div>
                        <b>{{ previewText.webOff }}</b>
                      </header>
                      <section>
                        <h4>{{ previewText.aiQuestion }}</h4>
                        <div class="mock-prompt-grid">
                          <span v-for="tab in item.preview.tabs" :key="tab">{{ tab }}</span>
                        </div>
                        <article>
                          <p>{{ previewText.aiRecommendation }}</p>
                          <div class="mock-nav-card">{{ item.preview.rows[0]?.value }}</div>
                          <div class="mock-related-card">{{ item.preview.rows[1]?.value }}</div>
                        </article>
                      </section>
                      <footer>{{ previewText.aiComposer }}</footer>
                    </main>
                  </div>
                </template>
              </div>
            </article>
          </article>
        </div>
      </div>
    </section>

    <!--
      已按当前页面节奏移除图2所在的液态玻璃交互预览区。
      保留原代码便于后续恢复：
      <section id="work" class="fluid-stage-band" :aria-label="pageCopy.fluidPreviewAria">
        <FluidGlassStage :dark="themeStore.isDarkMode" />
      </section>
    -->

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
            <span>{{ pageCopy.modelField }}</span>
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
          <button class="text-action" type="button" @click="router.push(ROUTES.TRAINING)">
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
import gsap from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'
import { useAuthStore } from '@/stores/auth.store'
import { useThemeStore } from '@/stores/theme.store'
import { ROUTES } from '@/constants'
// 图2液态玻璃交互预览区已从首页渲染中移除，组件保留在源码中备用。
// import FluidGlassStage from '@/components/effects/FluidGlassStage.vue'
import IridescenceField from '@/components/effects/IridescenceField.vue'
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

type ShowcaseCopy = {
  eyebrow: string
  title: string
  desc: string
}

type ShowcasePreview = {
  title: string
  mode: string
  kind: 'catalog' | 'testing' | 'analytics' | 'data' | 'ai'
  tabs: string[]
  metrics: Array<{ value: string; label: string }>
  bars: number[]
  rows: Array<{ name: string; meta: string; value: string }>
  notes: string[]
}

type ShowcaseItem = {
  index: string
  eyebrow: string
  title: string
  desc: string
  tags: string[]
  path: string
  public?: boolean
  action: string
  accent: string
  preview: ShowcasePreview
}

type LandingCopy = {
  heroKicker: string
  heroCopy: string
  primaryAction: string
  secondaryAction: string
  liveTaskAria: string
  metricsAria: string
  productPreviewAria: string
  fluidPreviewAria: string
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
  modelField: string
  openDashboard: string
  closingTitle: string
  closingAction: string
  cases: Array<Omit<CaseItem, 'accent' | 'bars' | 'icon' | 'lines' | 'nodes'>>
  tasks: Array<{ label: string; state: string }>
  flow: Array<{ index: string; title: string; text: string }>
}

const router = useRouter()
const authStore = useAuthStore()
const themeStore = useThemeStore()
const { locale } = useI18n()
const landingRootRef = ref<HTMLElement | null>(null)
const pointer = { x: 0, y: 0 }
let pointerRaf = 0
let landingMotionContext: ReturnType<typeof gsap.context> | null = null
let pendingPointerCssX = '50%'
let pendingPointerCssY = '50%'
let pendingTiltX = '0deg'
let pendingTiltY = '0deg'

const openWorkspace = () => router.push(authStore.isAuthenticated ? ROUTES.TRAINING : '/login')

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
    heroKicker: '9 个推荐系统模型接入与可视化',
    heroCopy: '把 9 个推荐系统模型、接入测试、性能图表、数据中心和 AI 助手放在同一个网站里。打开页面就能看模型状态、跑推荐测试、预览数据，并让 AI 帮你跳到需要的功能。',
    primaryAction: '进入工作台',
    secondaryAction: '查看能力',
    liveTaskAria: 'DeepInsight 实时任务流',
    metricsAria: 'DeepInsight 关键指标',
    productPreviewAria: 'DeepInsight 产品预览',
    fluidPreviewAria: 'DeepInsight 交互预览',
    consoleRun: '在线接入',
    consoleSections: '04 个主入口',
    metrics: [
      { value: '9', label: '预置模型' },
      { value: '1', label: '推荐系统' },
      { value: '4', label: '分析入口' },
      { value: '1', label: 'AI 导航口' },
    ],
    workEyebrow: '当前流程',
    workTitle: '从模型总览出发，直接进入测试、图表、数据和 AI 助手。',
    disciplineEyebrow: '模型与数据',
    disciplineTitle: '所有模型都以推荐系统功能方式呈现，状态和入口一眼可见。',
    disciplineCopy: '模型总览展示服务状态、参数、数据集、指标和可操作入口。数据中心用于上传、预览、整理数据；接入测试用于验证模型能否返回推荐结果。',
    studioEyebrow: '分析入口',
    studioTitle: '用性能看板、接入测试和数据预览完成日常操作。',
    modelField: '模型场域',
    openDashboard: '进入展示区',
    closingTitle: '从任意入口进入，都能继续查看模型、数据、图表或 AI 助手。',
    closingAction: '开始使用',
    cases: [
      {
        title: '模型总览',
        tag: '模型总览',
        signal: '9 个模型 / 推荐系统',
        desc: '查看 9 个推荐系统模型的接入状态。',
        detail: '每个模型展示参数、数据集、可用指标和服务状态；能测试的直接进入测试页，不能测试的清楚显示当前状态。',
      },
      {
        title: '接入测试',
        tag: '接入测试',
        signal: '接口可用 / 边界清晰',
        desc: '测试已注册的推荐入口。',
        detail: '测试页会区分可调用服务、离线日志和待补充服务，结果区只展示当前能够确认的状态。',
      },
      {
        title: '数据中心',
        tag: '数据中心',
        signal: '数据管理已就绪',
        desc: '上传 CSV 或 ZIP 数据集，保留历史记录和预览结果。',
        detail: '数据中心用于记录、预览和分析准备，不会自动把用户上传数据变成模型训练数据。',
      },
      {
        title: 'AI 导航与问答',
        tag: 'AI',
        signal: '站内上下文就绪',
        desc: '向 AI 说出目标页面，即可获得对应跳转入口。',
        detail: 'AI 可以解释页面功能、模型状态和数据入口，也可以把你带到对应页面继续操作。',
      },
    ],
    tasks: [
      { label: '模型清单读取', state: 'run' },
      { label: '推荐功能统一', state: 'diff' },
      { label: '图表状态检查', state: 'safe' },
      { label: '页面入口匹配', state: 'draft' },
    ],
    flow: [
      { index: '01', title: '选择模型', text: '从 9 个内置推荐系统模型中选择一个模型家族。' },
      { index: '02', title: '运行测试', text: '在接入测试页验证推荐入口和服务状态。' },
      { index: '03', title: '查看图表', text: '用性能看板核对 HR@K、NDCG@K、MRR 和数据规模。' },
      { index: '04', title: '记录材料', text: '把数据集、文章和讨论保留在探索中心。' },
    ],
  },
  en: {
    heroKicker: 'Real recommender-system access for 9 local models',
    heroCopy: 'DeepInsight brings the 9 recommender models, access testing, performance charts, data center, and AI assistant into one website. Open a page to check model status, run recommendation tests, preview data, or ask AI to navigate.',
    primaryAction: 'Enter Workspace',
    secondaryAction: 'View Capabilities',
    liveTaskAria: 'DeepInsight live task stream',
    metricsAria: 'DeepInsight metrics',
    productPreviewAria: 'DeepInsight product preview',
    fluidPreviewAria: 'DeepInsight interactive preview',
    consoleRun: 'RUN MONITOR',
    consoleSections: '04 sections',
    metrics: [
      { value: '9', label: 'model entries' },
      { value: '1', label: 'task family' },
      { value: '4', label: 'analysis views' },
      { value: '1', label: 'AI workspace' },
    ],
    workEyebrow: 'Current workflow',
    workTitle: 'Start from model overview, then move directly into testing, charts, data, and AI help.',
    disciplineEyebrow: 'Models and data',
    disciplineTitle: 'Every model is presented as a usable recommender feature, with status and actions visible at a glance.',
    disciplineCopy: 'The overview shows service status, parameters, datasets, metrics, and available actions. The data center handles upload and preview, while access testing checks whether a model can return recommendations.',
    studioEyebrow: 'Daily workspace',
    studioTitle: 'Use performance charts, access tests, and data previews for everyday work.',
    modelField: 'Model Space',
    openDashboard: 'Open workspace',
    closingTitle: 'Every entry lets you continue with models, data, charts, or the AI assistant.',
    closingAction: 'Start Now',
    cases: [
      {
        title: 'Model Overview',
        tag: 'Models',
        signal: '9 models / recommender only',
        desc: 'Review the real access status of all 9 recommender-system models.',
        detail: 'Each model shows parameters, datasets, available metrics, and service status. Callable models go straight to testing; unavailable ones show their current state clearly.',
      },
      {
        title: 'Access Test',
        tag: 'Access',
        signal: 'endpoint ready / clear boundary',
        desc: 'Only real callable recommendation endpoints are tested.',
        detail: 'BSARec Job is the only model with a backend recommendation proxy. Other models explain missing services or proxies instead of returning fake success.',
      },
      {
        title: 'Data Workflow',
        tag: 'Data',
        signal: 'dataset lineage verified',
        desc: 'Manage uploaded datasets, preview rows, and data-quality hints.',
        detail: 'Uploaded datasets are used for records, preview, and analysis preparation. Model training data is managed separately.',
      },
      {
        title: 'AI Workspace',
        tag: 'AI',
        signal: 'analysis context ready',
        desc: 'Ask for model differences, page entries, related articles, and missing status.',
        detail: 'AI explains page features, model states, and data entries, then takes you to the matching page when you want to continue.',
      },
    ],
    tasks: [
      { label: 'Model catalog loaded', state: 'run' },
      { label: 'Recommendation features aligned', state: 'diff' },
      { label: 'Chart status checked', state: 'safe' },
      { label: 'Page entry matched', state: 'draft' },
    ],
    flow: [
      { index: '01', title: 'Choose Model', text: 'Pick one of the 9 built-in recommender-system models.' },
      { index: '02', title: 'Run Test', text: 'Verify real recommendation endpoints and service status.' },
      { index: '03', title: 'Read Charts', text: 'Compare HR@K, NDCG@K, MRR, and dataset scale.' },
      { index: '04', title: 'Keep Context', text: 'Keep datasets, articles, and discussion records in the platform.' },
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
const showcaseCopy = computed<ShowcaseCopy>(() => localeKey.value === 'en'
  ? {
      eyebrow: 'Product preview',
      title: 'Preview the main DeepInsight features.',
      desc: 'A compact tour of model overview, access testing, performance charts, data center, and AI workspace. Each simplified screen maps to a real page you can open.',
    }
  : {
      eyebrow: '产品预览',
      title: '快速预览 DeepInsight 的主要功能。',
      desc: '用几个核心入口展示模型总览、接入测试、性能图表、数据中心和 AI 工作区。每张简化图都对应一个可进入的页面。',
    })
const previewText = computed(() => localeKey.value === 'en'
  ? {
      catalogTitle: 'Model Overview',
      modelStatus: 'Model status',
      profile: 'Readiness profile',
      evidence: 'Status notes',
      testTitle: 'Access Test Center',
      currentMode: 'Current mode',
      controlPanel: 'Recommendation request controls',
      requestPanel: 'BSARec request panel',
      compareInput: 'Sequence input',
      sequenceInput: 'Historical item IDs and Top-K',
      analysisTitle: 'Visual Analysis',
      analysisTarget: 'Analysis target',
      selectModel: 'Select model or analysis target',
      modelExamples: 'BSARec / FMLP-Rec / SASRec',
      analysisModules: 'Analysis modules',
      dataTitle: 'Explore Center / Data Center',
      dataBadge: 'DATA',
      refreshAssets: 'Refresh assets',
      datasetViz: 'Dataset visualization',
      assetScope: 'Asset scope',
      searchPlaceholder: 'Search name, type, status...',
      assetDetail: 'Asset detail',
      aiSubtitle: 'Model Q&A / Page navigation',
      newChat: 'New chat',
      reasoning: 'Reasoning',
      aiWorkspace: 'AI Workspace',
      webOff: 'Web off',
      aiQuestion: 'Ask about models, data, or page entries. AI returns actionable navigation.',
      aiRecommendation: 'Start from model overview to inspect all 9 local recommender models.',
      aiComposer: 'Type a question, press Enter to send',
    }
  : {
      catalogTitle: '模型总览',
      modelStatus: '模型状态',
      profile: '接入证据',
      evidence: '状态记录',
      testTitle: '模型接入测试中心',
      currentMode: '当前模式',
      controlPanel: '推荐请求控制面板',
      requestPanel: 'BSARec 请求面板',
      compareInput: '序列输入',
      sequenceInput: '历史物品 ID 与返回条数',
      analysisTitle: '可视化分析',
      analysisTarget: '分析目标',
      selectModel: '选择模型或分析对象',
      modelExamples: 'BSARec / FMLP-Rec / SASRec',
      analysisModules: '分析模块',
      dataTitle: '数据中心',
      dataBadge: '数据',
      refreshAssets: '刷新资产',
      datasetViz: '数据集可视化',
      assetScope: '资产范围',
      searchPlaceholder: '搜索名称、类型、状态...',
      assetDetail: '资产详情',
      aiSubtitle: '模型问答 / 页面导航',
      newChat: '新对话',
      reasoning: '推理',
      aiWorkspace: 'AI 工作区',
      webOff: '站内模式',
      aiQuestion: '询问模型、数据或页面位置，可直接打开相关页面。',
      aiRecommendation: '推荐先进入模型总览查看 9 个本地推荐系统模型。',
      aiComposer: '输入问题，Enter 发送',
    })
const showcaseItems = computed<ShowcaseItem[]>(() => localeKey.value === 'en'
  ? [
      {
        index: '01',
        accent: '#48f5a6',
        eyebrow: 'Model catalog',
        title: 'Recommender access overview',
        desc: 'Review the 9 local recommender models by service status, datasets, parameters, metrics, and available actions.',
        tags: ['9 models', 'recommender', 'status'],
        path: ROUTES.TRAINING,
        action: 'Open model overview',
        preview: {
          title: '9 Recommender Models',
          mode: 'mixed status',
          kind: 'catalog',
          tabs: ['Overview', 'BSARec', 'Transformer', 'Status'],
          metrics: [
            { value: '9', label: 'models' },
            { value: '2', label: 'logs' },
            { value: '1', label: 'proxy' },
            { value: '6', label: 'code+data' },
          ],
          bars: [46, 68, 58, 82, 62, 74],
          rows: [
            { name: 'BSARec Job', meta: 'backend proxy registered', value: 'service offline' },
            { name: 'BSARec', meta: 'Beauty / LastFM logs', value: 'artifact+log' },
            { name: 'BERT4Rec', meta: 'Beauty / ML-1M / Steam data', value: 'code+data' },
          ],
          notes: ['dataset scale', 'metric source', 'service boundary'],
        },
      },
      {
        index: '02',
        accent: '#7dd3fc',
        eyebrow: 'Access test',
        title: 'Real recommendation endpoint test',
        desc: 'Test only callable recommendation routes. If a model has code and data but no service proxy, the UI explains the missing layer.',
        tags: ['Top-K', 'service health', 'no fake result'],
        path: ROUTES.PREDICTION,
        action: 'Open test center',
        preview: {
          title: 'Access Test Center',
          mode: 'backend endpoint',
          kind: 'testing',
          tabs: ['BSARec Job', 'Top-K', 'Health'],
          metrics: [
            { value: '10', label: 'Top-K' },
            { value: '5000', label: 'Flask port' },
            { value: '403', label: 'root auth' },
            { value: '0', label: 'fake calls' },
          ],
          bars: [72, 44, 86, 53, 66, 58],
          rows: [
            { name: 'BSARec Job', meta: 'Job sequence request', value: 'service offline' },
            { name: 'BSARec', meta: 'no backend proxy', value: 'not callable' },
            { name: 'FMLP-Rec', meta: 'offline metric logs', value: 'artifact+log' },
          ],
          notes: ['backend request', 'clear failure reason', 'Top-K output only when service responds'],
        },
      },
      {
        index: '03',
        accent: '#facc15',
        eyebrow: 'Visual analysis',
        title: 'Recommendation performance charts',
        desc: 'Compare HR@K, NDCG@K, MRR, dataset scale, and readiness. Models without metrics are marked as pending and do not interrupt existing comparisons.',
        tags: ['HR@K', 'NDCG@K', 'MRR'],
        path: ROUTES.ANALYSIS_WORKBENCH,
        action: 'Open visual analysis',
        preview: {
          title: 'Performance Board',
          mode: 'charts',
          kind: 'analytics',
          tabs: ['Ranking', 'Readiness', 'Data', 'Logs'],
          metrics: [
            { value: 'HR@10', label: 'hit rate' },
            { value: 'NDCG@10', label: 'rank quality' },
            { value: 'MRR', label: 'first relevant' },
            { value: 'Metric', label: 'source' },
          ],
          bars: [38, 77, 54, 92, 64, 70],
          rows: [
            { name: 'BSARec', meta: 'Beauty log', value: 'HR@10 0.1008' },
            { name: 'FMLP-Rec', meta: 'Beauty log', value: 'NDCG@10 0.3317' },
            { name: 'SASRec', meta: 'no local eval log', value: 'metrics missing' },
          ],
          notes: ['available metrics', 'readiness bars', 'pending hints'],
        },
      },
      {
        index: '04',
        accent: '#fb7185',
        eyebrow: 'Data center',
        title: 'Dataset records and preview',
        desc: 'Keep uploaded datasets, preview rows, field quality, and user-isolated records together while model assets stay tied to local files.',
        tags: ['CSV', 'sequence', 'user isolation'],
        path: ROUTES.DATA,
        action: 'Open data center',
        preview: {
          title: 'Data Center',
          mode: 'records',
          kind: 'data',
          tabs: ['Upload', 'Preview', 'Quality', 'Sequences'],
          metrics: [
            { value: 'CSV', label: 'tables' },
            { value: 'SEQ', label: 'sequences' },
            { value: 'UID', label: 'isolation' },
            { value: 'Viz', label: 'preview' },
          ],
          bars: [61, 48, 79, 57, 88, 66],
          rows: [
            { name: 'Job.txt', meta: 'job recommendation sequence', value: '409,466 interactions' },
            { name: 'Beauty.txt', meta: 'shared recommender data', value: '198,502 interactions' },
            { name: 'ml-100k atomic', meta: 'RecBole / DuoRec / FEARec', value: '100,000 interactions' },
          ],
          notes: ['preview record', 'field quality', 'sequence scale'],
        },
      },
      {
        index: '05',
        accent: '#38bdf8',
        eyebrow: 'AI assistant',
        title: 'AI navigation, articles, and web search',
        desc: 'The workspace AI can return page jumps, related articles, Markdown answers, optional web search, and persistent user-scoped conversation records.',
        tags: ['navigation', 'articles', 'history'],
        path: ROUTES.AI,
        action: 'Open AI workspace',
        preview: {
          title: 'AI Workspace',
          mode: 'Markdown',
          kind: 'ai',
          tabs: ['Navigation', 'Articles', 'Search', 'History'],
          metrics: [
            { value: 'MD', label: 'answers' },
            { value: 'Route', label: 'jump' },
            { value: 'Web', label: 'search' },
            { value: 'DB', label: 'history' },
          ],
          bars: [52, 74, 47, 82, 69, 58],
          rows: [
            { name: 'Jump: model overview', meta: 'site navigation', value: ROUTES.TRAINING },
            { name: 'Article: BSARec deep dive', meta: 'knowledge drawer', value: 'related' },
            { name: 'Search: optional web', meta: 'user controlled', value: 'on/off' },
          ],
          notes: ['floating assistant', 'page context', 'user-scoped history'],
        },
      },
    ]
  : [
      {
        index: '01',
        accent: '#48f5a6',
        eyebrow: '模型总览',
        title: '推荐模型接入全览',
        desc: '集中查看 9 个本地推荐系统模型的服务状态、核心指标、参数、数据集和可用操作。',
        tags: ['9 个模型', '推荐系统', '状态总览'],
        path: ROUTES.TRAINING,
        action: '打开模型总览',
        preview: {
          title: '9 个推荐系统模型',
          mode: '状态不一',
          kind: 'catalog',
          tabs: ['总览', 'BSARec', '序列模型', '状态'],
          metrics: [
            { value: '9', label: '模型' },
            { value: '2', label: '日志' },
            { value: '1', label: '代理' },
            { value: '6', label: '代码+数据' },
          ],
          bars: [46, 68, 58, 82, 62, 74],
          rows: [
            { name: 'BSARec Job', meta: '已登记后端代理', value: '服务离线' },
            { name: 'BSARec', meta: 'Beauty / LastFM 日志', value: '权重+日志' },
            { name: 'BERT4Rec', meta: 'Beauty / ML-1M / Steam 数据', value: '代码+数据' },
          ],
          notes: ['数据规模', '指标状态', '服务边界'],
        },
      },
      {
        index: '02',
        accent: '#7dd3fc',
        eyebrow: '接入测试',
        title: '推荐入口测试',
        desc: '测试已注册的推荐链路，清楚区分可调用服务、离线日志和待补充服务。',
        tags: ['Top-K', '服务健康', '状态清晰'],
        path: ROUTES.PREDICTION,
        action: '打开接入测试',
        preview: {
          title: '模型接入测试中心',
          mode: '后端接口',
          kind: 'testing',
          tabs: ['BSARec Job', 'Top-K', '健康检查'],
          metrics: [
            { value: '10', label: 'Top-K' },
            { value: '5000', label: 'Flask 端口' },
            { value: '403', label: '根路径鉴权' },
            { value: '0', label: '占位结果' },
          ],
          bars: [72, 44, 86, 53, 66, 58],
          rows: [
            { name: 'BSARec Job', meta: 'Job 序列请求', value: '服务离线' },
            { name: 'BSARec', meta: '未登记后端代理', value: '不可调用' },
            { name: 'FMLP-Rec', meta: '已有离线指标', value: '权重+日志' },
          ],
          notes: ['后端请求', '错误原因清晰', '服务响应后才显示 Top-K'],
        },
      },
      {
        index: '03',
        accent: '#facc15',
        eyebrow: '可视化分析',
        title: '推荐性能图表工作台',
        desc: '用图表查看 HR@K、NDCG@K、MRR、数据规模和接入证据。没有指标的模型会显示为待补充，不影响已有模型的对比。',
        tags: ['HR@K', 'NDCG@K', 'MRR'],
        path: ROUTES.ANALYSIS_WORKBENCH,
        action: '打开可视化分析',
        preview: {
          title: '性能看板',
          mode: '图表',
          kind: 'analytics',
          tabs: ['排序质量', '接入证据', '数据规模', '日志状态'],
          metrics: [
            { value: 'HR@10', label: '命中率' },
            { value: 'NDCG@10', label: '排序质量' },
            { value: 'MRR', label: '首个相关项' },
            { value: '指标', label: '数据状态' },
          ],
          bars: [38, 77, 54, 92, 64, 70],
          rows: [
            { name: 'BSARec', meta: 'Beauty 日志', value: 'HR@10 0.1008' },
            { name: 'FMLP-Rec', meta: 'Beauty 日志', value: 'NDCG@10 0.3317' },
            { name: 'SASRec', meta: '暂无本地评估日志', value: '指标缺失' },
          ],
          notes: ['可用指标', '扫描证据', '待补充提示'],
        },
      },
      {
        index: '04',
        accent: '#fb7185',
        eyebrow: '数据中心',
        title: '数据集记录与可视化预览',
        desc: '数据管理与云端中心合并为数据中心，记录上传数据、预览行、字段质量、样本分布和用户隔离历史。',
        tags: ['表格数据', '序列数据', '用户隔离'],
        path: ROUTES.DATA,
        action: '打开数据中心',
        preview: {
          title: '数据中心',
          mode: '记录',
          kind: 'data',
          tabs: ['上传', '预览', '质量', '序列'],
          metrics: [
            { value: '表格', label: '表格数据' },
            { value: '序列', label: '序列数据' },
            { value: '隔离', label: '用户隔离' },
            { value: '预览', label: '数据预览' },
          ],
          bars: [61, 48, 79, 57, 88, 66],
          rows: [
            { name: 'Job.txt', meta: '岗位推荐序列', value: '409,466 交互' },
            { name: 'Beauty.txt', meta: '通用推荐数据', value: '198,502 交互' },
            { name: 'ml-100k atomic', meta: 'RecBole / DuoRec / FEARec', value: '100,000 交互' },
          ],
          notes: ['预览记录', '字段质量', '序列规模'],
        },
      },
      {
        index: '05',
        accent: '#38bdf8',
        eyebrow: 'AI 辅助',
        title: '站内导航、文章联动与联网搜索',
        desc: 'AI 工作区和悬浮助手支持页面跳转、相关文章、富文本回答、联网搜索开关和按用户隔离保存的对话记录。',
        tags: ['导航', '文章', '历史'],
        path: ROUTES.AI,
        action: '打开 AI 工作区',
        preview: {
          title: 'AI 工作区',
          mode: '富文本',
          kind: 'ai',
          tabs: ['导航', '文章', '搜索', '历史'],
          metrics: [
            { value: '问答', label: '回答格式' },
            { value: '跳转', label: '页面入口' },
            { value: '联网', label: '联网搜索' },
            { value: '记录', label: '历史记录' },
          ],
          bars: [52, 74, 47, 82, 69, 58],
          rows: [
            { name: '跳转：模型总览', meta: '站内导航', value: ROUTES.TRAINING },
            { name: '文章：BSARec 深度解析', meta: '右侧抽屉', value: '相关' },
            { name: '搜索：联网资料', meta: '用户控制', value: '可开关' },
          ],
          notes: ['悬浮助手', '页面上下文', '用户隔离历史'],
        },
      },
    ])
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

function openShowcaseItem(item: ShowcaseItem) {
  if (item.public || authStore.isAuthenticated) {
    router.push(item.path)
    return
  }
  router.push('/login')
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

function prefersReducedMotion() {
  return typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches
}

function setupLandingMotion() {
  const root = landingRootRef.value
  if (!root || prefersReducedMotion()) return
  gsap.registerPlugin(ScrollTrigger)
  root.classList.add('motion-enhanced')
  landingMotionContext?.revert()
  landingMotionContext = gsap.context(() => {
    gsap.fromTo(
      '.hero-shell',
      { autoAlpha: 0, y: 34, filter: 'blur(14px)' },
      { autoAlpha: 1, y: 0, filter: 'blur(0px)', duration: 0.92, ease: 'power3.out' },
    )
    gsap.fromTo(
      '.hero-console',
      { autoAlpha: 0, y: 46, scale: 0.965, rotateX: 5, filter: 'blur(14px)' },
      { autoAlpha: 1, y: 0, scale: 1, rotateX: 0, filter: 'blur(0px)', duration: 1, delay: 0.12, ease: 'power3.out' },
    )

    const revealTargets = gsap.utils.toArray<HTMLElement>(
      '[data-landing-reveal], .metric-cell, .showcase-step, .preview-window, .flow-step, .studio-feed, .studio-copy, .closing-band h2, .closing-band .primary-action',
    )
    gsap.set(revealTargets, { autoAlpha: 0, y: 34, filter: 'blur(10px)' })
    revealTargets.forEach((target, index) => {
      gsap.to(target, {
        autoAlpha: 1,
        y: 0,
        filter: 'blur(0px)',
        duration: 0.82,
        ease: 'power3.out',
        delay: (index % 4) * 0.035,
        scrollTrigger: {
          trigger: target,
          start: 'top 86%',
          toggleActions: 'play none none reverse',
        },
      })
    })

  }, root)
  ScrollTrigger.refresh()
}

onMounted(() => {
  void nextTick(() => {
    window.scrollTo({ top: 0, left: 0, behavior: 'auto' })
    setupLandingMotion()
  })
  flushPointerVars()
})
onUnmounted(() => {
  if (pointerRaf) window.cancelAnimationFrame(pointerRaf)
  landingMotionContext?.revert()
  landingMotionContext = null
})
</script>

<style scoped>
.di-landing {
  min-height: 100vh;
  overflow-x: hidden;
  background:
    radial-gradient(1120px circle at 12% 6%, rgba(122, 255, 232, 0.13), transparent 48%),
    radial-gradient(980px circle at 94% 18%, rgba(202, 171, 255, 0.11), transparent 52%),
    radial-gradient(820px circle at 68% 88%, rgba(255, 166, 226, 0.075), transparent 54%),
    linear-gradient(145deg, #0a171b 0%, #10161f 42%, #090d14 72%, #05070d 100%);
  color: var(--text-primary);
  --mx: 50%;
  --my: 50%;
  --tilt-x: 0deg;
  --tilt-y: 0deg;
  --flowing-menu-bg: var(--surface-solid);
  --landing-day-wash: color-mix(in srgb, var(--primary-color) 6%, #dbeafe);
  --landing-day-glow: color-mix(in srgb, var(--accent-glow) 10%, transparent);
  --landing-day-ink: rgba(30, 41, 59, 0.055);
  --landing-day-line: rgba(71, 85, 105, 0.12);
}

:global(html.light .di-landing) {
  background:
    linear-gradient(180deg, rgba(250, 253, 255, 0.96), rgba(232, 245, 242, 0.88) 42%, rgba(238, 246, 245, 0.96) 100%),
    linear-gradient(118deg, rgba(66, 230, 164, 0.04), transparent 36%, rgba(96, 165, 250, 0.065)),
    var(--bg-color);
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
.hero-iridescence,
.hero-grid,
.hero-day-map,
.hero-night-stage {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.hero-iridescence {
  z-index: -6 !important;
  position: absolute !important;
  opacity: 1 !important;
  mix-blend-mode: normal;
  mask-image: none;
  -webkit-mask-image: none;
}

.hero-shader-field {
  z-index: -5;
  opacity: 0.78;
  mix-blend-mode: screen;
  filter: saturate(102%) contrast(104%) brightness(0.82);
  mask-image: linear-gradient(180deg, rgba(0,0,0,0.95), rgba(0,0,0,0.74) 76%, rgba(0,0,0,0.22) 96%, transparent 100%);
  -webkit-mask-image: linear-gradient(180deg, rgba(0,0,0,0.95), rgba(0,0,0,0.74) 76%, rgba(0,0,0,0.22) 96%, transparent 100%);
}

:global(html.light .hero-shader-field) {
  display: block !important;
  opacity: 0.34;
  mix-blend-mode: normal;
  filter: saturate(84%) contrast(102%);
  mask-image: linear-gradient(180deg, rgba(0,0,0,0.62), rgba(0,0,0,0.42) 60%, rgba(0,0,0,0.12) 92%, transparent 100%);
  -webkit-mask-image: linear-gradient(180deg, rgba(0,0,0,0.62), rgba(0,0,0,0.42) 60%, rgba(0,0,0,0.12) 92%, transparent 100%);
}

.hero-grid {
  z-index: -4;
  background: none;
  opacity: 1;
}

:global(html.light .hero-grid) {
  display: none;
}

.hero-grid::before {
  content: none;
  position: absolute;
  left: -12%;
  right: -12%;
  bottom: -4%;
  height: 74%;
  background-image:
    linear-gradient(90deg, rgba(177, 249, 232, 0.11) 1px, transparent 1px),
    linear-gradient(0deg, rgba(177, 249, 232, 0.075) 1px, transparent 1px);
  background-size: 58px 58px;
  transform-origin: center bottom;
  transform: perspective(1200px) rotateX(76deg);
  opacity: 0.48;
  mask-image: linear-gradient(180deg, rgba(0, 0, 0, 0.98), transparent 92%);
  animation: perspective-grid-drift 18s linear infinite;
}

:global(html.light .hero-grid::before) {
  content: none;
}

.hero-grid::after {
  content: "";
  position: absolute;
  inset: auto 8% 11% 8%;
  height: 2px;
  background: linear-gradient(90deg, transparent, rgba(177, 249, 232, 0.36), rgba(255, 255, 255, 0.14), rgba(202, 171, 255, 0.28), transparent);
  box-shadow:
    0 0 20px rgba(177, 249, 232, 0.14),
    0 0 70px rgba(202, 171, 255, 0.08);
  opacity: 0.44;
}

:global(html.light .hero-grid::after) {
  content: none;
}

.hero-section::after {
  content: "";
  position: absolute;
  inset: 0;
  z-index: -1;
  pointer-events: none;
  background:
    radial-gradient(760px circle at var(--mx) var(--my), rgba(122, 255, 232, 0.09), transparent 48%),
    radial-gradient(980px circle at 82% 18%, rgba(202, 171, 255, 0.065), transparent 42%),
    linear-gradient(180deg, rgba(7, 13, 18, 0.04), rgba(5, 9, 14, 0.34) 68%, var(--bg-color));
}

:global(html.light .hero-section::after) {
  background:
    radial-gradient(760px circle at var(--mx) var(--my), rgba(255, 255, 255, 0.12), transparent 56%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.05), transparent 58%, rgba(238, 246, 245, 0.24));
}

.hero-day-map {
  z-index: -3;
  display: none;
  overflow: hidden;
  pointer-events: none;
}

.hero-night-stage {
  z-index: -2;
  display: block;
  overflow: hidden;
  pointer-events: none;
  mix-blend-mode: screen;
  opacity: 0.56;
}

:global(html.light .hero-night-stage) {
  display: none;
}

.night-orbit,
.night-horizon,
.night-spotlight {
  position: absolute;
  display: block;
  pointer-events: none;
}

.night-orbit {
  border: 1px solid rgba(177, 249, 232, 0.16);
  border-radius: 999px;
  box-shadow:
    inset 0 0 26px rgba(177, 249, 232, 0.035),
    0 0 36px rgba(124, 233, 255, 0.055);
  transform: rotate(-10deg);
  opacity: 0.34;
}

.orbit-a {
  width: min(52vw, 720px);
  height: min(52vw, 720px);
  right: -8%;
  top: 6%;
  animation: night-orbit-drift 16s ease-in-out infinite alternate;
}

.orbit-b {
  width: min(36vw, 520px);
  height: min(36vw, 520px);
  right: 10%;
  top: 21%;
  border-color: rgba(255, 255, 255, 0.08);
  animation: night-orbit-drift 20s ease-in-out infinite alternate-reverse;
}

.orbit-c {
  width: min(70vw, 940px);
  height: min(28vw, 360px);
  left: 5%;
  bottom: 8%;
  border-color: color-mix(in srgb, var(--accent-glow) 22%, transparent);
  transform: perspective(1000px) rotateX(70deg) rotateZ(-2deg);
  opacity: 0.26;
}

.night-horizon {
  left: 8%;
  right: 8%;
  bottom: 12.5%;
  height: 110px;
  background:
    radial-gradient(closest-side at 50% 100%, rgba(177, 249, 232, 0.24), transparent 48%),
    radial-gradient(closest-side at 50% 100%, rgba(226, 189, 255, 0.16), transparent 62%);
  filter: blur(22px);
  opacity: 0.34;
  transform: scaleX(1.12);
}

.night-spotlight {
  width: 48vw;
  max-width: 680px;
  aspect-ratio: 1;
  right: 8%;
  top: 18%;
  border-radius: 999px;
  background:
    radial-gradient(circle at 50% 50%, rgba(224, 251, 255, 0.08), rgba(99, 255, 209, 0.12) 24%, rgba(226, 189, 255, 0.07) 42%, transparent 66%);
  filter: blur(18px);
  opacity: 0.38;
  animation: night-spotlight-breathe 7.2s ease-in-out infinite;
}

:global(html.light .hero-day-map) {
  display: none;
}

:global(html.light .map-line),
:global(html.light .map-node) {
  display: none;
}

.map-line,
.map-node {
  position: absolute;
  display: block;
}

.map-line {
  height: 1px;
  transform-origin: left center;
  background: linear-gradient(90deg, transparent, rgba(96, 165, 250, 0.22), rgba(var(--primary-rgb), 0.13), transparent);
  box-shadow: 0 0 18px rgba(96, 165, 250, 0.08);
  animation: day-map-scan 8.8s ease-in-out infinite;
}

.line-1 {
  width: 36vw;
  top: 26%;
  left: 46%;
  transform: rotate(-15deg);
}

.line-2 {
  width: 30vw;
  top: 40%;
  left: 55%;
  transform: rotate(13deg);
  animation-delay: 900ms;
}

.line-3 {
  width: 42vw;
  top: 58%;
  left: 34%;
  transform: rotate(-4deg);
  animation-delay: 1600ms;
}

.line-4 {
  width: 28vw;
  top: 70%;
  left: 60%;
  transform: rotate(-22deg);
  animation-delay: 2400ms;
}

.line-5 {
  width: 34vw;
  top: 18%;
  left: 66%;
  transform: rotate(48deg);
  animation-delay: 3200ms;
}

.map-node {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--primary-color) 42%, #60a5fa);
  border: 1px solid rgba(255, 255, 255, 0.86);
  box-shadow:
    0 0 0 8px rgba(96, 165, 250, 0.055),
    0 0 28px rgba(96, 165, 250, 0.16);
  animation: day-node-pulse 3.8s ease-in-out infinite;
}

.node-1 { left: 49%; top: 25%; }
.node-2 { left: 71%; top: 31%; animation-delay: 420ms; }
.node-3 { left: 58%; top: 44%; animation-delay: 760ms; }
.node-4 { left: 79%; top: 57%; animation-delay: 1100ms; }
.node-5 { left: 44%; top: 66%; animation-delay: 1500ms; }
.node-6 { left: 88%; top: 22%; animation-delay: 1900ms; }

.hero-shell {
  position: relative;
  z-index: 2;
  max-width: 920px;
  padding-bottom: 24px;
  min-width: 0;
}

.hero-shell::before {
  content: "";
  position: absolute;
  left: -34px;
  top: -42px;
  width: min(64vw, 760px);
  height: 74%;
  z-index: -1;
  border-radius: 48px;
  background:
    radial-gradient(520px circle at 16% 20%, rgba(var(--primary-rgb), 0.18), transparent 58%),
    linear-gradient(118deg, rgba(255, 255, 255, 0.06), transparent 48%);
  filter: blur(18px);
  opacity: 0.78;
  transform: translate3d(calc((var(--mx) - 50%) * 0.012), calc((var(--my) - 50%) * 0.01), 0);
}

:global(html.light .hero-shell::before) {
  opacity: 0.24;
  filter: blur(28px);
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
  padding: 9px 12px;
  border: 1px solid rgba(var(--primary-rgb), 0.18);
  border-radius: 999px;
  color: color-mix(in srgb, var(--text-primary) 86%, var(--primary-color));
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.075), transparent 48%),
    rgba(var(--glass-bg-rgb), 0.24);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.12),
    0 16px 42px rgba(0, 0, 0, 0.16);
  backdrop-filter: blur(18px) saturate(160%);
  -webkit-backdrop-filter: blur(18px) saturate(160%);
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
  font-size: clamp(76px, 8.3vw, 138px);
  line-height: 0.96;
  font-weight: var(--font-weight-title);
  letter-spacing: 0;
  transform: translateZ(0);
}

.hero-shiny-title {
  padding: 0.1em 0.1em 0.18em 0;
  background-size: 260% auto;
  filter:
    drop-shadow(0 28px 78px rgba(var(--primary-rgb), 0.38))
    drop-shadow(0 0 34px rgba(255, 255, 255, 0.14))
    drop-shadow(0 0 24px rgba(var(--primary-rgb), 0.22));
}

.hero-copy {
  max-width: 700px;
  margin-top: 26px;
  color: color-mix(in srgb, var(--text-primary) 84%, var(--primary-color));
  font-size: clamp(18px, 1.6vw, 23px);
  line-height: 1.68;
  text-shadow:
    0 0 30px rgba(var(--primary-rgb), 0.18),
    0 2px 16px rgba(0, 0, 0, 0.42);
}

:global(html.light .hero-copy) {
  color: color-mix(in srgb, var(--text-secondary) 82%, var(--primary-color));
  text-shadow: 0 12px 32px rgba(var(--primary-rgb), 0.12);
}

.hero-code-ribbon,
.console-code-block {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.hero-code-ribbon {
  margin-top: 26px;
}

.hero-code-ribbon span,
.console-code-block span {
  padding: 5px 8px;
  border: 1px solid rgba(var(--primary-rgb), 0.2);
  border-radius: 999px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.075), transparent 52%),
    rgba(var(--glass-bg-rgb), 0.26);
  color: color-mix(in srgb, var(--text-secondary) 84%, var(--primary-color));
  font-family: "JetBrains Mono", Consolas, monospace;
  font-size: 10px;
  font-weight: var(--font-weight-body);
  white-space: nowrap;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(14px) saturate(150%);
  -webkit-backdrop-filter: blur(14px) saturate(150%);
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
  height: 48px;
  border-radius: 16px;
  padding: 0 20px;
  border: 1px solid var(--border-strong);
  font: inherit;
  font-size: 14px;
  font-weight: var(--font-weight-label);
  color: var(--text-primary);
  background: var(--surface-2);
  cursor: pointer;
  transition: transform 220ms var(--ease-liquid), border-color 220ms ease, background 220ms ease, box-shadow 260ms ease, color 180ms ease;
}

.primary-action {
  border-color: color-mix(in srgb, var(--primary-color) 78%, #ffffff);
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.34), transparent 42%),
    linear-gradient(180deg, color-mix(in srgb, var(--primary-color) 92%, #ffffff), var(--primary-color));
  color: #06100c;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.42),
    0 18px 48px rgba(var(--primary-rgb), 0.38),
    0 0 42px rgba(var(--primary-rgb), 0.16);
}

.secondary-action,
.text-action {
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.08), transparent 48%),
    rgba(var(--glass-bg-rgb), 0.34);
  backdrop-filter: blur(18px) saturate(160%);
  -webkit-backdrop-filter: blur(18px) saturate(160%);
}

.primary-action:hover,
.secondary-action:hover,
.text-action:hover {
  transform: translateY(-2px) scale(1.012);
  border-color: var(--primary-color);
  box-shadow:
    0 20px 58px rgba(var(--primary-rgb), 0.2),
    var(--shadow-hover);
}

.hero-console {
  position: relative;
  z-index: 2;
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 26px;
  padding: 18px;
  overflow: hidden;
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.12), transparent 32%),
    radial-gradient(420px circle at var(--mx) var(--my), rgba(var(--primary-rgb), 0.2), transparent 48%),
    rgba(8, 12, 18, 0.66);
  backdrop-filter: blur(26px) saturate(185%);
  -webkit-backdrop-filter: blur(26px) saturate(185%);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.2),
    inset 0 -1px 0 rgba(255, 255, 255, 0.05),
    0 34px 96px rgba(0, 0, 0, 0.36),
    0 0 70px rgba(var(--primary-rgb), 0.14);
  transform: perspective(900px) rotateX(var(--tilt-x)) rotateY(var(--tilt-y));
  transform-origin: center;
  transition: border-color 220ms ease, box-shadow 260ms ease, transform 220ms var(--ease-liquid);
}

.hero-console::before {
  content: "";
  position: absolute;
  inset: 1px;
  border-radius: 24px;
  pointer-events: none;
  background:
    linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent) top / 100% 1px no-repeat,
    radial-gradient(300px circle at var(--mx) var(--my), rgba(var(--primary-rgb), 0.18), transparent 50%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.08), transparent 28%);
}

.hero-console::after {
  content: "";
  position: absolute;
  inset: auto 16px 16px 16px;
  height: 1px;
  pointer-events: none;
  background: linear-gradient(90deg, transparent, rgba(var(--primary-rgb), 0.52), transparent);
  box-shadow: 0 0 22px rgba(var(--primary-rgb), 0.22);
  opacity: 0.78;
}

.hero-console:hover {
  border-color: rgba(var(--primary-rgb), 0.44);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.24),
    0 38px 104px rgba(0, 0, 0, 0.42),
    0 0 88px rgba(var(--primary-rgb), 0.18);
}

:global(html.light .hero-console) {
  border-color: rgba(64, 96, 112, 0.18);
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.92), rgba(255, 255, 255, 0.52) 42%, rgba(226, 242, 246, 0.48)),
    radial-gradient(520px circle at var(--mx) var(--my), rgba(85, 218, 194, 0.12), transparent 54%),
    rgba(244, 250, 252, 0.72);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.82),
    inset 0 -1px 0 rgba(36, 68, 82, 0.04),
    0 26px 72px rgba(54, 81, 96, 0.16),
    0 0 54px rgba(96, 165, 250, 0.08);
}

:global(html.light .hero-console::before) {
  background:
    linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.82), transparent) top / 100% 1px no-repeat,
    radial-gradient(300px circle at var(--mx) var(--my), rgba(85, 218, 194, 0.12), transparent 52%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.58), transparent 34%);
}

:global(html.light .hero-console::after) {
  background: linear-gradient(90deg, transparent, rgba(85, 218, 194, 0.22), rgba(96, 165, 250, 0.18), transparent);
  box-shadow: 0 0 16px rgba(85, 218, 194, 0.12);
  opacity: 0.72;
}

:global(html.light .hero-console:hover) {
  border-color: rgba(var(--primary-rgb), 0.24);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.86),
    0 30px 78px rgba(54, 81, 96, 0.18),
    0 0 62px rgba(96, 165, 250, 0.1);
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
  border: 1px solid rgba(255, 255, 255, 0.11);
  border-radius: 20px;
  display: grid;
  grid-template-columns: 92px 1fr;
  gap: 16px;
  align-items: center;
  background:
    radial-gradient(260px circle at 16% 20%, rgba(var(--primary-rgb), 0.22), transparent 58%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.075), transparent 54%),
    rgba(var(--glass-bg-rgb), 0.28);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.1),
    0 18px 48px rgba(0, 0, 0, 0.18);
}

:global(html.light .console-stage) {
  border-color: rgba(61, 92, 112, 0.13);
  background:
    radial-gradient(260px circle at 16% 20%, rgba(95, 136, 255, 0.08), transparent 58%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.82), rgba(255, 255, 255, 0.32) 62%),
    rgba(239, 247, 250, 0.66);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.72),
    0 14px 34px rgba(54, 81, 96, 0.09);
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
  border: 1px solid rgba(var(--primary-rgb), 0.52);
  border-radius: 50%;
  background:
    radial-gradient(circle at 50% 50%, rgba(var(--primary-rgb), 0.16), transparent 42%),
    radial-gradient(circle at 50% 50%, rgba(255, 255, 255, 0.08), transparent 58%);
  box-shadow:
    inset 0 0 28px rgba(var(--primary-rgb), 0.12),
    0 0 32px rgba(var(--primary-rgb), 0.16);
}

:global(html.light .stage-radar) {
  border-color: rgba(95, 136, 255, 0.28);
  background:
    radial-gradient(circle at 50% 50%, rgba(95, 136, 255, 0.13), transparent 42%),
    radial-gradient(circle at 50% 50%, rgba(255, 255, 255, 0.82), transparent 58%);
  box-shadow:
    inset 0 0 28px rgba(95, 136, 255, 0.08),
    0 0 28px rgba(95, 136, 255, 0.12);
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
  border: 1px solid rgba(255, 255, 255, 0.09);
  border-radius: 15px;
  color: color-mix(in srgb, var(--text-secondary) 90%, var(--primary-color));
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.055), transparent 52%),
    rgba(var(--glass-bg-rgb), 0.24);
  min-width: 0;
}

:global(html.light .task-row) {
  border-color: rgba(61, 92, 112, 0.11);
  color: rgba(54, 72, 92, 0.72);
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.78), rgba(255, 255, 255, 0.28) 56%),
    rgba(240, 248, 250, 0.58);
}

:global(html.light .hero-code-ribbon span),
:global(html.light .console-code-block span) {
  border-color: rgba(95, 136, 255, 0.16);
  color: rgba(66, 84, 110, 0.62);
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.74), transparent 58%),
    rgba(238, 246, 250, 0.62);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.72);
}

.proof-strip {
  position: relative;
  z-index: 5;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  border-top: 1px solid rgba(var(--primary-rgb), 0.12);
  border-bottom: 1px solid rgba(var(--primary-rgb), 0.12);
  background:
    linear-gradient(90deg, rgba(var(--primary-rgb), 0.1), transparent 22%, transparent 78%, rgba(var(--primary-rgb), 0.07)),
    rgba(5, 7, 10, 0.78);
  color: var(--text-primary);
  backdrop-filter: blur(22px) saturate(150%);
  -webkit-backdrop-filter: blur(22px) saturate(150%);
  box-shadow:
    0 -1px 0 rgba(255, 255, 255, 0.04),
    0 24px 58px rgba(0, 0, 0, 0.2);
}

:global(html.light .proof-strip) {
  background: var(--surface-solid);
  border-top-color: var(--border-color);
  border-bottom-color: var(--border-color);
  box-shadow: 0 -1px 0 var(--border-color), 0 18px 42px rgba(0, 0, 0, 0.04);
  backdrop-filter: none;
  -webkit-backdrop-filter: none;
}

.showcase-band {
  position: relative;
  padding-block: 98px 112px;
  overflow: hidden;
  background:
    radial-gradient(760px circle at 12% 12%, rgba(var(--primary-rgb), 0.1), transparent 56%),
    radial-gradient(720px circle at 88% 36%, color-mix(in srgb, var(--accent-glow) 10%, transparent), transparent 60%),
    linear-gradient(180deg, var(--bg-color), color-mix(in srgb, var(--surface-solid) 88%, var(--bg-color)));
}

:global(html.light .showcase-band) {
  background:
    radial-gradient(760px circle at 12% 12%, rgba(96, 165, 250, 0.08), transparent 58%),
    radial-gradient(720px circle at 88% 36%, color-mix(in srgb, var(--accent-glow) 6%, transparent), transparent 62%),
    linear-gradient(180deg, #f8fafc, var(--bg-color));
}

.showcase-band::before {
  content: "";
  position: absolute;
  inset: 0;
  pointer-events: none;
  background-image:
    linear-gradient(90deg, rgba(var(--primary-rgb), 0.032) 1px, transparent 1px),
    linear-gradient(0deg, rgba(var(--primary-rgb), 0.026) 1px, transparent 1px);
  background-size: 76px 76px;
  mask-image: linear-gradient(180deg, transparent, black 20%, black 82%, transparent);
  opacity: 0.42;
}

.showcase-shell {
  position: relative;
  z-index: 1;
  max-width: 1220px;
  margin: 0 auto;
}

.showcase-heading {
  display: grid;
  grid-template-columns: minmax(0, 0.92fr) minmax(320px, 0.68fr);
  gap: 36px;
  align-items: end;
  margin-bottom: 38px;
}

.showcase-heading span,
.showcase-copy span {
  display: inline-flex;
  color: var(--primary-color);
  font-size: 12px;
  font-weight: var(--font-weight-label);
  text-transform: uppercase;
}

.showcase-heading span {
  margin-bottom: 14px;
}

.showcase-heading h2 {
  max-width: 780px;
  margin: 0;
  font-size: clamp(34px, 4.2vw, 58px);
  line-height: 1.05;
  font-weight: var(--font-weight-title);
  letter-spacing: -0.03em;
}

.showcase-heading p {
  color: var(--text-secondary);
  font-size: 16px;
  line-height: 1.78;
}

.showcase-track {
  display: grid;
  gap: 28px;
}

.showcase-step {
  position: relative;
  display: grid;
  grid-template-columns: minmax(260px, 0.72fr) minmax(0, 1.28fr);
  gap: 26px;
  align-items: center;
  min-height: 390px;
}

.showcase-step.reverse {
  grid-template-columns: minmax(0, 1.28fr) minmax(260px, 0.72fr);
}

.showcase-step.reverse .showcase-copy {
  order: 2;
}

.showcase-step.reverse .preview-window {
  order: 1;
}

.showcase-copy {
  position: relative;
  min-width: 0;
  padding: 22px 4px;
}

.showcase-index {
  margin-bottom: 22px;
  color: color-mix(in srgb, var(--accent) 76%, var(--text-primary));
  font-size: 13px;
  font-weight: var(--font-weight-label);
}

.showcase-copy h3 {
  max-width: 430px;
  margin: 14px 0 14px;
  font-size: clamp(28px, 3vw, 42px);
  line-height: 1.08;
  font-weight: var(--font-weight-title);
  letter-spacing: -0.02em;
}

.showcase-copy p {
  max-width: 520px;
  color: var(--text-secondary);
  font-size: 15px;
  line-height: 1.74;
}

.showcase-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 20px;
}

.showcase-tags i {
  border: 1px solid color-mix(in srgb, var(--accent) 16%, var(--border-color));
  border-radius: 999px;
  color: color-mix(in srgb, var(--accent) 70%, var(--text-secondary));
  background: color-mix(in srgb, var(--accent) 7%, transparent);
  font-size: 11px;
  font-style: normal;
  white-space: nowrap;
}

.showcase-tags i {
  padding: 6px 9px;
}

.showcase-action {
  margin-top: 24px;
}

.preview-window {
  position: relative;
  display: grid;
  gap: 14px;
  min-width: 0;
  min-height: 360px;
  padding: 16px;
  border: 1px solid color-mix(in srgb, var(--accent) 18%, var(--border-color));
  border-radius: var(--radius-sm);
  overflow: hidden;
  color: var(--text-primary);
  text-align: left;
  background:
    radial-gradient(420px circle at 18% 0%, color-mix(in srgb, var(--accent) 12%, transparent), transparent 56%),
    linear-gradient(145deg, rgba(255, 255, 255, 0.055), transparent 36%),
    rgba(var(--glass-bg-rgb), 0.58);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.08),
    var(--shadow-soft);
  cursor: pointer;
  backdrop-filter: blur(18px) saturate(155%);
  -webkit-backdrop-filter: blur(18px) saturate(155%);
  transition: transform 220ms ease, border-color 220ms ease, box-shadow 220ms ease;
}

:global(html.light .preview-window) {
  background:
    radial-gradient(420px circle at 18% 0%, color-mix(in srgb, var(--accent) 9%, transparent), transparent 58%),
    linear-gradient(145deg, rgba(255, 255, 255, 0.9), rgba(255, 255, 255, 0.46) 58%, rgba(255, 255, 255, 0.22)),
    rgba(var(--glass-bg-rgb), 0.72);
}

.preview-window::before {
  content: "";
  position: absolute;
  inset: 42px 0 0;
  pointer-events: none;
  background-image:
    linear-gradient(90deg, color-mix(in srgb, var(--accent) 8%, transparent) 1px, transparent 1px),
    linear-gradient(0deg, rgba(148, 163, 184, 0.06) 1px, transparent 1px);
  background-size: 34px 34px;
  mask-image: radial-gradient(circle at 22% 6%, black, transparent 72%);
  opacity: 0.48;
}

.preview-window:hover {
  transform: translateY(-4px);
  border-color: color-mix(in srgb, var(--accent) 42%, var(--border-color));
  box-shadow:
    0 22px 62px color-mix(in srgb, var(--accent) 12%, transparent),
    var(--shadow-hover);
}

.preview-toolbar,
.preview-stage {
  position: relative;
  z-index: 1;
}

.preview-toolbar {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  min-height: 28px;
}

.window-dots {
  display: inline-flex;
  gap: 6px;
}

.window-dots i {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--accent) 62%, var(--border-strong));
  opacity: 0.72;
}

.preview-toolbar strong {
  min-width: 0;
  overflow: hidden;
  font-size: 13px;
  font-weight: var(--font-weight-label);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-toolbar em {
  color: var(--text-muted);
  font-size: 11px;
  font-style: normal;
  white-space: nowrap;
}

.preview-stage {
  display: grid;
  gap: 8px;
  min-height: 300px;
  color: var(--text-primary);
  font-size: 11px;
}

.preview-stage :where(div, aside, main, section, article, header, footer, p) {
  min-width: 0;
}

.preview-stage :where(strong, b, span, small, em, p, i) {
  overflow: hidden;
  text-overflow: ellipsis;
}

.preview-stage :where(strong, b) {
  color: var(--text-primary);
}

.preview-stage :where(span, small, p, em) {
  color: var(--text-muted);
}

.preview-stage :where(i, em) {
  font-style: normal;
}

.preview-stage :where(strong, b, span, small, em, i) {
  white-space: nowrap;
}

.mock-hero,
.mock-viz-header,
.mock-builder,
.mock-catalog-shell,
.mock-test-shell,
.mock-dataset-section,
.mock-asset-workbench,
.mock-ai-shell {
  border: 1px solid color-mix(in srgb, var(--accent) 14%, var(--border-color));
  border-radius: 16px;
  background:
    linear-gradient(135deg, color-mix(in srgb, var(--accent) 8%, transparent), transparent 48%),
    rgba(var(--glass-bg-rgb), 0.3);
}

.mock-hero,
.mock-viz-header {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(160px, 0.55fr);
  gap: 12px;
  align-items: center;
  padding: 13px;
}

.mock-hero strong,
.mock-viz-header strong,
.mock-ai-main h4 {
  display: block;
  margin-top: 5px;
  font-size: 15px;
  line-height: 1.2;
}

.mock-hero small,
.mock-viz-header small {
  display: block;
  margin-top: 7px;
  white-space: normal;
  line-height: 1.42;
}

.mock-summary-grid,
.mock-kpi-grid,
.mock-status-band,
.mock-result-grid,
.mock-prompt-grid {
  display: grid;
  gap: 7px;
}

.mock-summary-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.mock-summary-grid.three {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.mock-summary-grid article,
.mock-kpi-grid article,
.mock-status-band article {
  min-height: 52px;
  padding: 9px;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  background: rgba(var(--glass-bg-rgb), 0.28);
}

.mock-summary-grid b,
.mock-kpi-grid b,
.mock-status-band b {
  display: block;
  margin-top: 6px;
  color: color-mix(in srgb, var(--accent) 76%, var(--text-primary));
  font-size: 16px;
  line-height: 1;
}

.mock-catalog-shell,
.mock-test-shell {
  grid-template-columns: minmax(150px, 0.42fr) minmax(0, 1fr);
  gap: 9px;
  padding: 9px;
}

.mock-catalog-shell {
  display: grid;
  min-height: 212px;
}

.mock-model-list,
.mock-model-detail,
.mock-control-panel,
.mock-task-panel {
  border: 1px solid var(--border-color);
  border-radius: 14px;
  background: rgba(var(--glass-bg-rgb), 0.24);
}

.mock-model-list {
  display: grid;
  gap: 7px;
  align-content: start;
  padding: 9px;
}

.mock-model-list header,
.mock-detail-head,
.mock-result-grid header,
.mock-ai-main header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.mock-model-list header b,
.mock-detail-head b,
.mock-result-grid header b,
.mock-ai-main header b {
  padding: 4px 7px;
  border-radius: 999px;
  color: #07110d;
  background: color-mix(in srgb, var(--accent) 78%, #ffffff);
  font-size: 10px;
}

.mock-model-row {
  display: grid;
  grid-template-columns: 8px minmax(0, 1fr) auto;
  gap: 7px;
  align-items: center;
  padding: 7px;
  border: 1px solid transparent;
  border-radius: 11px;
  background: rgba(var(--glass-bg-rgb), 0.18);
}

.mock-model-row.active {
  border-color: color-mix(in srgb, var(--accent) 30%, var(--border-color));
  background: color-mix(in srgb, var(--accent) 9%, transparent);
}

.mock-model-row i {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--accent);
  box-shadow: 0 0 14px color-mix(in srgb, var(--accent) 46%, transparent);
}

.mock-model-row span {
  grid-column: 2;
  font-size: 10px;
}

.mock-model-row em {
  grid-column: 3;
  grid-row: 1 / span 2;
  color: color-mix(in srgb, var(--accent) 75%, var(--text-primary));
  font-size: 10px;
}

.mock-model-detail {
  display: grid;
  gap: 9px;
  padding: 10px;
}

.mock-detail-head strong {
  display: block;
  margin-top: 4px;
  font-size: 15px;
}

.mock-detail-head small {
  display: block;
  margin-top: 4px;
}

.mock-kpi-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.mock-profile-grid {
  display: grid;
  grid-template-columns: minmax(0, 0.78fr) minmax(0, 1fr);
  gap: 8px;
}

.mock-profile-grid > div {
  display: grid;
  gap: 6px;
  align-content: start;
  min-height: 74px;
  padding: 9px;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  background: rgba(var(--glass-bg-rgb), 0.22);
}

.mock-profile-grid i {
  display: block;
  height: 5px;
  border-radius: 999px;
  background: linear-gradient(90deg, var(--accent), color-mix(in srgb, var(--accent) 28%, transparent));
}

.mock-profile-grid p {
  white-space: normal;
  line-height: 1.32;
}

.mock-mode-card {
  padding: 12px;
  border: 1px solid color-mix(in srgb, var(--accent) 20%, var(--border-color));
  border-radius: 14px;
  background: rgba(var(--glass-bg-rgb), 0.28);
}

.mock-mode-card b {
  display: block;
  margin-top: 7px;
  font-size: 16px;
}

.mock-mode-card em {
  display: block;
  margin-top: 6px;
}

.mock-tabs {
  display: flex;
  gap: 7px;
}

.mock-tabs span,
.mock-builder i,
.mock-actions i,
.mock-asset-workbench aside span,
.mock-prompt-grid span {
  padding: 6px 9px;
  border: 1px solid color-mix(in srgb, var(--accent) 18%, var(--border-color));
  border-radius: 999px;
  color: color-mix(in srgb, var(--accent) 72%, var(--text-secondary));
  background: color-mix(in srgb, var(--accent) 7%, transparent);
}

.mock-tabs .active {
  color: #07110d;
  background: color-mix(in srgb, var(--accent) 78%, #ffffff);
}

.mock-test-shell {
  display: grid;
  min-height: 194px;
}

.mock-control-panel {
  display: grid;
  gap: 8px;
  align-content: start;
  padding: 10px;
}

.mock-control-panel label {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  padding: 8px;
  border-radius: 10px;
  background: rgba(var(--glass-bg-rgb), 0.18);
}

.mock-control-panel i {
  color: color-mix(in srgb, var(--accent) 80%, var(--text-primary));
}

.mock-task-panel {
  display: grid;
  gap: 8px;
  padding: 9px;
}

.mock-request-card {
  padding: 10px;
  border-radius: 12px;
  background:
    linear-gradient(135deg, color-mix(in srgb, var(--accent) 10%, transparent), transparent 62%),
    rgba(var(--glass-bg-rgb), 0.2);
}

.mock-request-card strong,
.mock-request-card p {
  display: block;
  margin-top: 5px;
}

.mock-result-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.mock-result-grid article {
  min-height: 96px;
  padding: 9px;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  background: rgba(var(--glass-bg-rgb), 0.22);
}

.mock-result-grid span,
.mock-result-grid em {
  display: block;
  margin-top: 10px;
}

.mock-result-grid em {
  color: color-mix(in srgb, var(--accent) 78%, var(--text-primary));
}

.mock-viz-header {
  grid-template-columns: minmax(0, 1fr) auto;
}

.mock-viz-header b {
  display: grid;
  place-items: center;
  width: 52px;
  height: 52px;
  border-radius: 16px;
  color: #07110d;
  background: color-mix(in srgb, var(--accent) 78%, #ffffff);
  font-size: 20px;
}

.mock-builder {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(150px, 0.55fr);
  gap: 9px;
  padding: 10px;
}

.mock-builder main,
.mock-builder aside {
  padding: 10px;
  border: 1px solid var(--border-color);
  border-radius: 13px;
  background: rgba(var(--glass-bg-rgb), 0.22);
}

.mock-builder strong,
.mock-select {
  display: block;
  margin-top: 6px;
}

.mock-select {
  padding: 8px 10px;
  border-radius: 10px;
  color: var(--text-secondary);
  background: rgba(var(--glass-bg-rgb), 0.28);
}

.mock-builder aside div {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.mock-status-band {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.mock-evidence-board {
  display: grid;
  grid-template-columns: minmax(126px, 0.38fr) minmax(0, 1fr);
  gap: 9px;
  min-height: 138px;
  padding: 9px;
  border: 1px solid color-mix(in srgb, var(--accent) 14%, var(--border-color));
  border-radius: 16px;
  background: rgba(var(--glass-bg-rgb), 0.22);
}

.mock-evidence-board aside,
.mock-evidence-board main {
  display: grid;
  gap: 7px;
}

.mock-evidence-board aside span {
  padding: 8px;
  border-radius: 10px;
  background: rgba(var(--glass-bg-rgb), 0.22);
}

.mock-evidence-board main {
  grid-template-columns: minmax(0, 0.72fr) minmax(0, 1fr);
}

.mock-chart-panel {
  display: flex;
  align-items: end;
  justify-content: center;
  gap: 7px;
  padding: 11px;
  border: 1px solid var(--border-color);
  border-radius: 13px;
  background: rgba(var(--glass-bg-rgb), 0.2);
}

.mock-chart-panel i,
.mock-field-bars i {
  width: 12px;
  min-height: 14px;
  border-radius: 7px 7px 3px 3px;
  background: linear-gradient(180deg, color-mix(in srgb, var(--accent) 90%, #fff), color-mix(in srgb, var(--accent) 46%, transparent));
}

.mock-evidence-list {
  display: grid;
  align-content: stretch;
  border: 1px solid var(--border-color);
  border-radius: 13px;
  overflow: hidden;
}

.mock-evidence-list p {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  padding: 9px;
  border-bottom: 1px solid var(--border-color);
}

.mock-evidence-list p:last-child {
  border-bottom: none;
}

.mock-evidence-list b {
  color: color-mix(in srgb, var(--accent) 78%, var(--text-primary));
}

.mock-data-hero {
  grid-template-columns: minmax(0, 1fr) auto;
}

.mock-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: end;
  gap: 7px;
}

.mock-dataset-section {
  display: grid;
  grid-template-columns: minmax(120px, 0.38fr) minmax(0, 1fr);
  gap: 9px;
  padding: 9px;
}

.mock-field-bars {
  display: flex;
  align-items: end;
  justify-content: center;
  gap: 8px;
  min-height: 76px;
  padding: 10px;
  border: 1px solid var(--border-color);
  border-radius: 13px;
  background: rgba(var(--glass-bg-rgb), 0.2);
}

.mock-asset-workbench {
  display: grid;
  grid-template-columns: minmax(112px, 0.28fr) minmax(0, 1fr) minmax(132px, 0.36fr);
  gap: 9px;
  min-height: 154px;
  padding: 9px;
}

.mock-asset-workbench aside,
.mock-asset-workbench main,
.mock-asset-workbench section {
  display: grid;
  gap: 7px;
  align-content: start;
  padding: 9px;
  border: 1px solid var(--border-color);
  border-radius: 13px;
  background: rgba(var(--glass-bg-rgb), 0.2);
}

.mock-search {
  padding: 8px 10px;
  border-radius: 10px;
  color: var(--text-muted);
  background: rgba(var(--glass-bg-rgb), 0.28);
}

.mock-asset-row {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 8px;
  align-items: center;
  padding: 8px;
  border-radius: 10px;
  background: rgba(var(--glass-bg-rgb), 0.18);
}

.mock-asset-row b {
  padding: 4px 6px;
  border-radius: 7px;
  color: #07110d;
  background: color-mix(in srgb, var(--accent) 72%, #ffffff);
  font-size: 9px;
}

.mock-asset-row em {
  color: color-mix(in srgb, var(--accent) 78%, var(--text-primary));
}

.mock-asset-workbench section p {
  margin-top: 6px;
  white-space: normal;
  line-height: 1.42;
}

.mock-ai-shell {
  display: grid;
  grid-template-columns: minmax(150px, 0.36fr) minmax(0, 1fr);
  gap: 0;
  min-height: 300px;
  overflow: hidden;
}

.mock-ai-sidebar,
.mock-ai-main {
  min-width: 0;
}

.mock-ai-sidebar {
  display: grid;
  gap: 10px;
  align-content: start;
  padding: 12px;
  border-right: 1px solid var(--border-color);
  background: rgba(var(--glass-bg-rgb), 0.26);
}

.mock-ai-brand {
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr);
  gap: 8px;
  align-items: center;
}

.mock-ai-brand i {
  grid-row: span 2;
  width: 28px;
  height: 28px;
  border-radius: 10px;
  background:
    radial-gradient(circle at 30% 28%, #fff, transparent 22%),
    var(--accent);
}

.mock-ai-brand span {
  grid-column: 2;
  font-size: 10px;
}

.mock-conversation-list {
  display: grid;
  gap: 7px;
}

.mock-conversation-list p {
  padding: 8px;
  border: 1px solid var(--border-color);
  border-radius: 11px;
  background: rgba(var(--glass-bg-rgb), 0.22);
}

.mock-conversation-list span {
  display: block;
  margin-top: 5px;
}

.mock-ai-sidebar footer {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  padding: 9px;
  border-radius: 11px;
  background: color-mix(in srgb, var(--accent) 8%, transparent);
}

.mock-ai-main {
  display: grid;
  grid-template-rows: auto 1fr auto;
  gap: 10px;
  padding: 12px;
}

.mock-ai-main h4 {
  margin: 0;
  white-space: normal;
}

.mock-ai-main section {
  display: grid;
  gap: 9px;
  align-content: start;
}

.mock-prompt-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.mock-ai-main article {
  display: grid;
  gap: 7px;
  padding: 10px;
  border: 1px solid var(--border-color);
  border-radius: 13px;
  background: rgba(var(--glass-bg-rgb), 0.2);
}

.mock-ai-main article p {
  white-space: normal;
  line-height: 1.42;
}

.mock-nav-card,
.mock-related-card {
  padding: 8px 10px;
  border-radius: 11px;
  color: color-mix(in srgb, var(--accent) 78%, var(--text-primary));
  background: color-mix(in srgb, var(--accent) 8%, transparent);
}

.mock-ai-main footer {
  padding: 9px 11px;
  border: 1px solid var(--border-color);
  border-radius: 999px;
  color: var(--text-muted);
  background: rgba(var(--glass-bg-rgb), 0.24);
}

.motion-enhanced [data-landing-reveal],
.motion-enhanced .metric-cell,
.motion-enhanced .showcase-step,
.motion-enhanced .preview-window,
.motion-enhanced .flow-step,
.motion-enhanced .studio-feed,
.motion-enhanced .studio-copy,
.motion-enhanced .closing-band h2,
.motion-enhanced .closing-band .primary-action {
  will-change: transform, opacity, filter;
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

:global(html.light .fluid-stage-band) {
  background:
    radial-gradient(1100px circle at 24% 38%, rgba(96, 165, 250, 0.13), transparent 58%),
    radial-gradient(900px circle at 80% 22%, color-mix(in srgb, var(--accent-glow) 10%, transparent), transparent 60%),
    linear-gradient(180deg, #f7f9fd 0%, var(--bg-color) 54%, color-mix(in srgb, var(--surface-1) 74%, #eef3fb) 100%);
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

:global(html.light .studio-band) {
  background:
    radial-gradient(560px circle at 84% 16%, rgba(96, 165, 250, 0.08), transparent 46%),
    radial-gradient(680px circle at 16% 82%, color-mix(in srgb, var(--primary-color) 5%, transparent), transparent 58%),
    linear-gradient(180deg, var(--bg-color), #f8fafc);
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

:global(html.light .feed-screen) {
  background:
    radial-gradient(300px circle at var(--mx) var(--my), rgba(96, 165, 250, 0.12), transparent 40%),
    linear-gradient(90deg, rgba(71, 85, 105, 0.06) 1px, transparent 1px),
    linear-gradient(0deg, rgba(71, 85, 105, 0.06) 1px, transparent 1px),
    radial-gradient(circle at 68% 38%, color-mix(in srgb, var(--primary-color) 8%, transparent), transparent 28%),
    var(--surface-3);
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

:global(html.light .closing-band) {
  background:
    radial-gradient(760px circle at 82% 28%, rgba(96, 165, 250, 0.08), transparent 58%),
    linear-gradient(180deg, var(--surface-solid), #f8fafc);
}

.closing-band h2 {
  max-width: 780px;
}

.dark .hero-console,
.dark .studio-feed,
.dark .studio-copy {
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.07), transparent 34%),
    radial-gradient(420px circle at var(--mx) var(--my), rgba(99, 255, 209, 0.12), transparent 52%),
    rgba(7, 13, 19, 0.72);
}

.dark .secondary-action,
.dark .text-action {
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.075), transparent 48%),
    rgba(5, 7, 10, 0.36);
}

@keyframes night-orbit-drift {
  from {
    transform: translate3d(-1.2%, -0.8%, 0) rotate(-10deg) scale(0.985);
  }
  to {
    transform: translate3d(1.4%, 1%, 0) rotate(-6deg) scale(1.025);
  }
}

@keyframes night-spotlight-breathe {
  0%,
  100% {
    opacity: 0.56;
    transform: scale(0.96);
  }
  50% {
    opacity: 0.86;
    transform: scale(1.04);
  }
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

@keyframes day-map-scan {
  0%,
  100% {
    opacity: 0.32;
    filter: blur(0);
  }
  48% {
    opacity: 0.82;
    filter: blur(0.2px);
  }
}

@keyframes day-node-pulse {
  0%,
  100% {
    opacity: 0.56;
    transform: translate3d(0, 0, 0) scale(0.92);
  }
  50% {
    opacity: 1;
    transform: translate3d(0, -2px, 0) scale(1.12);
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

  .night-spotlight {
    right: -4%;
    top: 10%;
    width: 62vw;
  }

  .orbit-a {
    right: -20%;
    top: 2%;
    width: min(72vw, 720px);
    height: min(72vw, 720px);
  }

  .showcase-heading,
  .showcase-step,
  .showcase-step.reverse {
    grid-template-columns: 1fr;
    align-items: start;
  }

  .showcase-step.reverse .showcase-copy,
  .showcase-step.reverse .preview-window {
    order: initial;
  }

}

@media (max-width: 760px) {
  .hero-section {
    min-height: auto;
    padding: 84px 18px 34px;
    gap: 24px;
  }

  .hero-shell::before {
    left: -20px;
    top: -28px;
    width: 100%;
    height: 64%;
    opacity: 0.54;
  }

  .hero-night-stage {
    opacity: 0.72;
  }

  .night-horizon {
    left: -6%;
    right: -6%;
    bottom: 20%;
  }

  .orbit-b,
  .orbit-c {
    display: none;
  }

  :global(html.light .hero-day-map) {
    opacity: 0.52;
    mask-image: linear-gradient(180deg, rgba(0, 0, 0, 0.46), rgba(0, 0, 0, 0.24) 70%, transparent 100%);
    -webkit-mask-image: linear-gradient(180deg, rgba(0, 0, 0, 0.46), rgba(0, 0, 0, 0.24) 70%, transparent 100%);
  }

  .map-line {
    opacity: 0.58;
  }

  .map-node {
    width: 8px;
    height: 8px;
  }

  .hero-shell h1 {
    font-size: clamp(44px, 13vw, 64px);
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

  .proof-strip {
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

  .showcase-band {
    padding-block: 72px;
  }

  .showcase-heading h2 {
    font-size: 32px;
    line-height: 1.12;
  }

  .showcase-track {
    gap: 18px;
  }

  .showcase-step {
    min-height: auto;
    gap: 18px;
  }

  .showcase-copy {
    padding: 8px 0 0;
  }

  .preview-window {
    min-height: 0;
    padding: 12px;
  }

  .preview-stage {
    min-height: auto;
    font-size: 10px;
  }

  .mock-hero,
  .mock-viz-header,
  .mock-catalog-shell,
  .mock-test-shell,
  .mock-builder,
  .mock-evidence-board,
  .mock-dataset-section,
  .mock-asset-workbench,
  .mock-ai-shell,
  .mock-evidence-board main {
    grid-template-columns: 1fr;
  }

  .mock-summary-grid,
  .mock-kpi-grid,
  .mock-status-band,
  .mock-result-grid,
  .mock-prompt-grid,
  .mock-profile-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .mock-hero,
  .mock-viz-header,
  .mock-catalog-shell,
  .mock-test-shell,
  .mock-builder,
  .mock-dataset-section,
  .mock-asset-workbench,
  .mock-ai-main,
  .mock-ai-sidebar {
    padding: 9px;
  }

  .mock-actions {
    justify-content: flex-start;
  }

  .mock-ai-sidebar {
    border-right: none;
    border-bottom: 1px solid var(--border-color);
  }

  .fluid-stage-band :deep(.fluid-glass-stage) {
    min-height: clamp(1880px, 272svh, 2580px);
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

@media (prefers-reduced-motion: reduce) {
  [data-landing-reveal],
  .metric-cell,
  .showcase-step,
  .preview-window,
  .flow-step,
  .studio-feed,
  .studio-copy,
  .closing-band h2,
  .closing-band .primary-action {
    transform: none !important;
    opacity: 1 !important;
    filter: none !important;
  }
}
</style>
