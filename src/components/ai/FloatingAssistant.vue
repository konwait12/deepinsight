<template>
  <div
    ref="assistantRef"
    class="assistant-container"
    :class="{ expanded: isExpanded, collapsing: isCollapsing, dragging: isDragging, 'bubble-right': proactiveBubbleRight, 'theme-dark': themeStore.isDarkMode, 'theme-light': !themeStore.isDarkMode }"
    :style="{ left: x + 'px', top: y + 'px' }"
  >
    <button
      v-if="!isExpanded && !isCollapsing"
      type="button"
      class="assistant-orb"
      :aria-label="assistantText.openAssistant"
      :title="assistantText.openAssistant"
      @mousedown="startDragCollapsed"
    >
      <span class="assistant-orb-halo" aria-hidden="true"></span>
      <span class="assistant-orb-mark" aria-hidden="true">
        <Bot :size="25" stroke-width="2.25" />
      </span>
      <span class="assistant-orb-ring" aria-hidden="true"></span>
    </button>

    <div v-if="!isExpanded && !isCollapsing && proactiveBubbleVisible" class="assistant-proactive-bubble">
      <span>{{ assistantText.proactivePrefix }}「{{ currentPageTitle }}」{{ assistantText.proactiveSuffix }}</span>
      <div>
        <button type="button" @mousedown.stop @click.stop="askCurrentPageFromBubble">{{ assistantText.askPage }}</button>
        <button type="button" class="ghost" @mousedown.stop @click.stop="dismissProactiveBubble">{{ assistantText.close }}</button>
      </div>
    </div>

    <section
      v-if="isExpanded || isCollapsing"
      class="assistant-panel assistant-opaque-surface"
      :class="{ closing: isCollapsing }"
      aria-label="DeepInsight AI assistant"
    >
      <header class="assistant-header" @mousedown="startDragExpanded">
        <div class="assistant-title">
          <span><Bot :size="17" /></span>
          <div>
            <h3>DeepInsight AI</h3>
            <p>{{ loading ? assistantText.thinkingRunning : assistantSubtitle }}</p>
          </div>
        </div>
        <div class="assistant-actions">
          <button
            type="button"
            :class="{ active: proactiveEnabled }"
            :title="proactiveEnabled ? assistantText.disableProactive : assistantText.enableProactive"
            @click.stop="toggleProactive"
          >
            <component :is="proactiveEnabled ? BellRing : BellOff" :size="15" />
          </button>
          <button type="button" :title="assistantText.clear" @click.stop="clearChat"><Trash2 :size="15" /></button>
          <button type="button" :title="assistantText.openFullChat" @click.stop="openFullChat"><Maximize2 :size="15" /></button>
          <button type="button" :title="assistantText.collapse" @click.stop="toggleExpand"><Minus :size="16" /></button>
        </div>
      </header>

      <div ref="msgContainer" class="assistant-messages">
        <article v-for="(msg, index) in messages" :key="index" class="message-row" :class="msg.role">
          <span v-if="msg.role !== 'user'" class="message-avatar"><Bot :size="14" /></span>
          <div class="message-stack">
            <button
              v-if="shouldShowThinking(msg, index)"
              type="button"
              class="assistant-thinking"
              :class="{ open: msg.thinkingExpanded, pending: !msg.thinking && loading && index === messages.length - 1 }"
              :aria-expanded="msg.thinkingExpanded ? 'true' : 'false'"
              @click="msg.thinkingExpanded = !msg.thinkingExpanded"
            >
              <span class="assistant-thinking-head">
                <Atom :size="18" />
                <strong>{{ thinkingLabel(msg, index) }}</strong>
                <ChevronDown :size="14" />
              </span>
            </button>
            <div
              v-if="msg.thinking"
              v-show="msg.thinkingExpanded"
              class="assistant-thinking-stream"
              :class="{ live: loading && index === messages.length - 1 }"
            >
              <pre v-show="msg.thinkingExpanded">{{ msg.thinking }}</pre>
            </div>
            <div v-if="msg.streamStatus && loading && index === messages.length - 1" class="assistant-run-status">
              <Atom :size="12" />
              <span>{{ msg.streamStatus }}</span>
            </div>
            <div v-if="msg.content.trim()" class="message-bubble" v-html="formatContent(msg.content)"></div>
            <div
              v-if="msg.reasoningDiagnostics"
              class="message-diagnostics-card"
              :class="{ open: msg.diagnosticsExpanded }"
            >
              <button
                type="button"
                class="message-diagnostics-toggle"
                @click="msg.diagnosticsExpanded = !msg.diagnosticsExpanded"
              >
                <span>{{ assistantText.diagnostics }}</span>
                <b>{{ msg.diagnosticsExpanded ? assistantText.collapse : diagnosticsBrief(msg.reasoningDiagnostics) }}</b>
              </button>
              <div v-show="msg.diagnosticsExpanded" class="message-diagnostics-body">
                <div v-for="item in diagnosticsItems(msg.reasoningDiagnostics)" :key="item.label">
                  <span>{{ item.label }}</span>
                  <strong>{{ item.value }}</strong>
                </div>
              </div>
            </div>
            <div
              v-if="msg.navigation"
              class="message-action-card"
              :class="{ collapsed: msg.navigationExpanded === false }"
            >
              <button
                type="button"
                class="message-card-toggle"
                @click="msg.navigationExpanded = msg.navigationExpanded === false"
              >
                <span>
                  <strong>{{ msg.navigation.label }}</strong>
                  <small>{{ msg.navigation.mode === 'suggested' ? assistantText.suggestedEntry : assistantText.pageNavigation }}</small>
                </span>
                <b>{{ msg.navigationExpanded === false ? assistantText.expand : assistantText.collapse }}</b>
              </button>
              <div v-show="msg.navigationExpanded !== false" class="message-action-body">
                <span>{{ msg.navigation.description }}</span>
                <small v-if="msg.navigation.mode === 'suggested'">{{ assistantText.suggestedEntry }} · {{ msg.navigation.reason }}</small>
                <div class="message-action-buttons">
                  <button type="button" @click="goToNavigation(msg.navigation)">{{ assistantText.goPage }}</button>
                  <button type="button" class="ghost" @click="openFullChatWithPrompt(msg.navigation)">{{ assistantText.continueAnalysis }}</button>
                </div>
              </div>
            </div>
            <div v-if="msg.relatedArticles?.length" class="message-resource-card">
              <button
                type="button"
                class="resource-card-title"
                @click="msg.relatedExpanded = !msg.relatedExpanded"
              >
                <span><BookOpen :size="13" /> {{ assistantText.relatedArticles }}</span>
                <b>{{ msg.relatedExpanded ? assistantText.collapse : `${assistantText.expand} ${msg.relatedArticles.length}` }}</b>
              </button>
              <div v-show="msg.relatedExpanded" class="resource-list">
                <button
                  v-for="article in msg.relatedArticles.slice(0, 3)"
                  :key="`${article.source}-${article.id}`"
                  type="button"
                  class="resource-item"
                  @click="openRelatedArticle(article)"
                >
                  <strong>{{ article.title }}</strong>
                  <span>{{ article.sourceLabel }} · {{ article.snippet }}</span>
                </button>
              </div>
            </div>
            <div v-if="msg.webResults?.length" class="message-resource-card web">
              <button
                type="button"
                class="resource-card-title"
                @click="msg.webExpanded = !msg.webExpanded"
              >
                <span><Globe2 :size="13" /> {{ assistantText.webReferences }}</span>
                <b>{{ msg.webExpanded ? assistantText.collapse : `${assistantText.expand} ${msg.webResults.length}` }}</b>
              </button>
              <small v-if="msg.webSearchQuery" v-show="msg.webExpanded" class="resource-query">
                {{ assistantText.searchQuery }}：{{ msg.webSearchQuery }}
              </small>
              <div v-show="msg.webExpanded" class="resource-list">
                <button
                  v-for="(result, resultIndex) in msg.webResults.slice(0, MAX_WEB_RESULTS)"
                  :key="result.url"
                  type="button"
                  class="resource-item"
                  @click="openWebResult(result.url)"
                >
                  <span class="resource-title-line">
                    <b>{{ result.refId || `W${resultIndex + 1}` }}</b>
                    <strong>{{ result.title }}</strong>
                  </span>
                  <span>{{ result.source }} · {{ result.snippet }}</span>
                </button>
              </div>
            </div>
            <div v-else-if="msg.webSearchAttempted" class="message-resource-card web">
              <button
                type="button"
                class="resource-card-title"
                @click="msg.webExpanded = !msg.webExpanded"
              >
                <span><Globe2 :size="13" /> {{ assistantText.webReferences }}</span>
                <b>{{ msg.webExpanded ? assistantText.collapse : assistantText.expand }}</b>
              </button>
              <small v-if="msg.webSearchQuery" v-show="msg.webExpanded" class="resource-query">
                {{ assistantText.searchQuery }}：{{ msg.webSearchQuery }}
              </small>
              <p v-show="msg.webExpanded" class="resource-empty">{{ assistantText.noWebResults }}</p>
            </div>
            <time>{{ msg.time }}</time>
          </div>
          <span v-if="msg.role === 'user'" class="message-avatar user-avatar"><UserRound :size="14" /></span>
        </article>

        <article v-if="showTypingIndicator" class="message-row assistant">
          <span class="message-avatar"><Bot :size="14" /></span>
          <div class="assistant-thinking pending">
            <span class="assistant-thinking-head">
              <Atom :size="18" />
              <strong>{{ assistantText.thinkingRunning }}</strong>
              <ChevronDown :size="14" />
            </span>
          </div>
        </article>
      </div>

      <div class="assistant-context-strip">
        <span>{{ assistantText.currentPage }}：{{ currentPageTitle }}</span>
        <strong v-if="lastInteraction">{{ assistantText.lastSelection }}：{{ lastInteraction.label }}</strong>
      </div>

      <div class="quick-chips">
        <button v-for="chip in quickChips" :key="chip.label" type="button" @click="askAI(chip.query)">
          <span>{{ chip.label }}</span>
        </button>
      </div>

      <form class="assistant-input" @submit.prevent="sendMessage">
        <div class="assistant-input-shell">
          <textarea
            ref="inputRef"
            v-model="userInput"
            :placeholder="loading ? assistantText.aiAnalyzing : assistantText.inputPlaceholder"
            :disabled="loading"
            rows="1"
            @input="autoResize"
            @keydown.enter.exact.prevent="sendMessage"
            @keydown.enter.shift.exact.prevent="userInput += '\n'"
          />
          <div class="assistant-input-tools">
            <div ref="reasoningMenuRef" class="assistant-reasoning" :class="{ open: reasoningMenuOpen }">
              <button type="button" class="assistant-reasoning-trigger" @click.stop="toggleReasoningMenu">
                <Brain :size="13" />
                <span>{{ activeReasoningLabel }}</span>
              </button>
              <div v-show="reasoningMenuOpen" class="assistant-reasoning-menu">
                <button
                  v-for="level in reasoningLevels"
                  :key="level.value"
                  type="button"
                  :class="{ active: reasoningLevel === level.value }"
                  @click.stop="selectReasoningLevel(level.value)"
                >
                  <strong>{{ level.label }}</strong>
                  <em>{{ level.hint }}</em>
                  <span>{{ level.desc }}</span>
                </button>
              </div>
            </div>

            <button
              type="button"
              class="assistant-web-toggle"
              :class="{ active: webSearchEnabled }"
              :title="webSearchEnabled ? assistantText.webOnTitle : assistantText.webOffTitle"
              @click="webSearchEnabled = !webSearchEnabled"
            >
              <Globe2 :size="12" />
              <span>{{ webSearchEnabled ? assistantText.webOnShort : assistantText.webOffShort }}</span>
            </button>

            <label class="assistant-temperature">
              <span>{{ assistantText.temperature }}</span>
              <input v-model.number="temperature" type="range" min="0" max="1" step="0.05" />
            </label>
          </div>
        </div>
        <button v-if="!loading" class="assistant-send" type="submit" :disabled="!userInput.trim()" :title="assistantText.send">
          <SendHorizontal :size="17" />
        </button>
        <button v-else class="assistant-send" type="button" :title="assistantText.stop" @click="stopGeneration">
          <Square :size="15" />
        </button>
      </form>
    </section>

    <ArticleReaderDrawer
      v-model="readerOpen"
      :article="readerArticle"
      :loading="readerLoading"
      :source-label="readerSourceLabel"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { aiApi, forumApi } from '@/api'
import type { ChatApiStatus, ChatReasoningDiagnostics, ChatRelatedArticle, ChatWebResult } from '@/api/modules/ai.api'
import { ROUTES, STORAGE_KEYS } from '@/constants'
import { exploreNavItems, mainNavItems, navLabelKey } from '@/constants/navigation'
import ArticleReaderDrawer from '@/components/common/ArticleReaderDrawer.vue'
import { normalizeAssistantNavigation, type AssistantNavigationIntent } from '@/utils/assistantNavigation'
import { normalizeAssistantReply } from '@/utils/assistantReplyFallback'
import { clearAiAuthIfNeeded, formatAiErrorMessage } from '@/utils/aiError'
import { renderMarkdown } from '@/utils/markdown'
import { useThemeStore } from '@/stores/theme.store'
import { ElMessage } from 'element-plus'
import {
  Atom,
  BellOff,
  BellRing,
  BookOpen,
  Bot,
  Brain,
  ChevronDown,
  Globe2,
  Maximize2,
  Minus,
  SendHorizontal,
  Square,
  Trash2,
  UserRound,
} from 'lucide-vue-next'

type ReasoningLevel = 'off' | 'quick' | 'low' | 'deep' | 'max'
type AssistantMessage = {
  role: 'user' | 'assistant'
  content: string
  time: string
  thinking?: string
  thinkingExpanded?: boolean
  thinkingDuration?: number
  navigation?: AssistantNavigationIntent
  navigationExpanded?: boolean
  relatedArticles?: ChatRelatedArticle[]
  relatedExpanded?: boolean
  webResults?: ChatWebResult[]
  webExpanded?: boolean
  webSearchAttempted?: boolean
  webSearchUsed?: boolean
  webSearchQuery?: string
  reasoningDiagnostics?: ChatReasoningDiagnostics
  apiStatus?: ChatApiStatus
  diagnosticsExpanded?: boolean
  streamStatus?: string
}

type PageInteraction = {
  label: string
  tag: string
  route: string
  at: number
}

const PROACTIVE_FIRST_IDLE_MS = 10_000
const PROACTIVE_REPEAT_IDLE_MS = 60_000
const COLLAPSE_ANIMATION_MS = 280
const PAGE_CONTEXT_TEXT_LIMIT = 1_200
const MAX_WEB_RESULTS = 15
const CONTENT_TYPE_INTERVAL = 12
const REASONING_TYPE_INTERVAL = 10
const INTERACTIVE_SELECTOR = [
  'button',
  'a',
  '[role="button"]',
  '[data-ai-context]',
  '.nav-item',
  '.conversation-item',
  '.model-card',
  '.metric-card',
  '.dataset-card',
  '.article-card',
  '.resource-item',
  '.tab-button',
  '.el-tabs__item',
  '.el-button',
].join(',')

const route = useRoute()
const router = useRouter()
const { locale, t } = useI18n()
const themeStore = useThemeStore()
const assistantRef = ref<HTMLElement | null>(null)
const msgContainer = ref<HTMLElement | null>(null)
const inputRef = ref<HTMLTextAreaElement | null>(null)
const x = ref(window.innerWidth - 84)
const y = ref(window.innerHeight - 92)
const isExpanded = ref(false)
const isCollapsing = ref(false)
const isDragging = ref(false)
const loading = ref(false)
const userInput = ref('')
const messages = ref<AssistantMessage[]>([])
const chatHistory = ref<Array<{ role: string; content: string }>>([])
const conversationId = ref<number | null>(null)
const readerOpen = ref(false)
const readerArticle = ref<any>(null)
const readerLoading = ref(false)
const readerSourceLabel = ref('')
const reasoningLevel = ref<ReasoningLevel>('low')
const temperature = ref(0.7)
const webSearchEnabled = ref(false)
const reasoningMenuOpen = ref(false)
const reasoningMenuRef = ref<HTMLElement | null>(null)
const streamController = ref<AbortController | null>(null)
let contentTypingTarget: AssistantMessage | null = null
let reasoningTypingTarget: AssistantMessage | null = null
let contentTypingTimer: ReturnType<typeof window.setTimeout> | null = null
let reasoningTypingTimer: ReturnType<typeof window.setTimeout> | null = null
let contentTypingQueue: string[] = []
let reasoningTypingQueue: string[] = []
const proactiveEnabled = ref(localStorage.getItem(STORAGE_KEYS.ASSISTANT_PROACTIVE) !== 'false')
const proactiveBubbleVisible = ref(false)
const lastInteraction = ref<PageInteraction | null>(null)
const isZh = computed(() => !locale.value.startsWith('en'))

const assistantText = computed(() => isZh.value
  ? {
      openAssistant: '打开 DeepInsight AI 助手',
      orbBrand: 'AI',
      proactivePrefix: '我可以解读当前',
      proactiveSuffix: '页面',
      askPage: '解读',
      close: '关闭',
      reasoning: '推理中',
      disableProactive: '关闭页面停留提醒',
      enableProactive: '开启页面停留提醒',
      clear: '清空对话',
      openFullChat: '打开完整 AI 工作区',
      collapse: '收起',
      expand: '展开',
      hideReasoning: '收起推理过程',
      viewReasoning: '查看推理过程',
      thinkingRunning: '正在思考',
      thinkingDone: '已思考',
      thinkingSeconds: '用时 {seconds} 秒',
      diagnostics: '思考深度实际生效',
      suggestedEntry: '建议入口',
      pageNavigation: '页面导航',
      goPage: '前往页面',
      continueAnalysis: '继续分析',
      relatedArticles: '相关文章',
      webReferences: '联网参考',
      searchQuery: '检索词',
      noWebResults: '已尝试联网搜索，当前没有返回可展示结果。',
      currentPage: '当前页面',
      lastSelection: '最近选择',
      aiAnalyzing: 'AI 正在分析...',
      inputPlaceholder: '询问当前页面、模型、数据或功能入口',
      webOnTitle: '联网搜索已开启',
      webOffTitle: '联网搜索已关闭，仅使用站内知识',
      webOnShort: '联网',
      webOffShort: '站内',
      temperature: '温度',
      send: '发送',
      stop: '停止',
      reasoningStandard: '标准',
      viewing: '正在查看',
      adminPage: '管理后台',
      profilePage: '个人中心',
      currentPageFallback: '当前页面',
      quickCurrent: '解读当前页',
      quickSelection: '解释所选项',
      quickModel: '模型总览',
      quickAccess: '接入测试',
      quickData: '数据中心',
      queryModelOverview: '打开模型总览页面',
      queryAccessTest: '打开模型接入测试页面',
      queryDataCenter: '打开数据中心',
      currentPageQuestion: '请结合我当前所在的「{page}」页面，整理这里能做什么、重点看哪些模块。',
      interactionQuestion: '请结合当前页面解释我刚才点击的「{label}」是什么，下一步应该怎么用。',
      proactiveOn: '已开启页面停留提醒',
      proactiveOff: '已关闭页面停留提醒',
      welcome: '你好，我是 DeepInsight AI。可以整理模型状态、数据中心、接入测试、图表页面和跳转入口；需要外部资料时可以打开联网搜索。',
      noAiContent: 'AI 服务暂未返回内容。',
      stopped: '已停止生成',
      chatCleared: '对话已清空',
      openedPrefix: '已前往',
      noArticleSummary: '暂无文章摘要',
      unsafeLink: '外部链接协议不安全，已拦截',
      invalidLink: '外部链接格式无效',
      modelReasoning: '模型推理',
      contextEnhanced: '上下文增强',
      articles: '文章',
      web: '联网',
      noWeb: '未联网',
      effectiveQuery: '实际检索词',
      reasoningLevelLabel: '档位',
      modelLabel: '模型',
      nativeReasoning: '原生推理',
      siteArticles: '站内文章',
      webSearch: '联网搜索',
      enabled: '已启用',
      directAnswer: '直答',
      nativeEnabled: '已启用',
      nativeDisabled: '未启用',
      requestedNoTrigger: '已请求未触发',
      notRequested: '未请求',
    }
  : {
      openAssistant: 'Open DeepInsight AI assistant',
      orbBrand: 'AI',
      proactivePrefix: 'I can explain the',
      proactiveSuffix: 'page',
      askPage: 'Explain',
      close: 'Close',
      reasoning: 'reasoning',
      disableProactive: 'Disable page idle tips',
      enableProactive: 'Enable page idle tips',
      clear: 'Clear chat',
      openFullChat: 'Open full AI workspace',
      collapse: 'Collapse',
      expand: 'Expand',
      hideReasoning: 'Hide reasoning',
      viewReasoning: 'View reasoning',
      thinkingRunning: 'Thinking',
      thinkingDone: 'Thought',
      thinkingSeconds: '{seconds}s',
      diagnostics: 'Effective reasoning diagnostics',
      suggestedEntry: 'Suggested entry',
      pageNavigation: 'Page navigation',
      goPage: 'Go to page',
      continueAnalysis: 'Continue analysis',
      relatedArticles: 'Related articles',
      webReferences: 'Web references',
      searchQuery: 'Query',
      noWebResults: 'Web search was attempted, but no displayable result was returned.',
      currentPage: 'Current page',
      lastSelection: 'Last selection',
      aiAnalyzing: 'AI is analyzing...',
      inputPlaceholder: 'Ask about this page, a model, data, or evaluation results',
      webOnTitle: 'Web search enabled',
      webOffTitle: 'Web search disabled; site knowledge only',
      webOnShort: 'Web',
      webOffShort: 'Site',
      temperature: 'Temp',
      send: 'Send',
      stop: 'Stop',
      reasoningStandard: 'Standard',
      viewing: 'Viewing',
      adminPage: 'Admin',
      profilePage: 'Profile',
      currentPageFallback: 'Current page',
      quickCurrent: 'Explain page',
      quickSelection: 'Explain selection',
      quickModel: 'Model overview',
      quickAccess: 'Access test',
      quickData: 'Data center',
      queryModelOverview: 'Open the model overview page',
      queryAccessTest: 'Open the model access test page',
      queryDataCenter: 'Open the data center',
      currentPageQuestion: 'Based on my current "{page}" page, explain what it can do and which modules matter most.',
      interactionQuestion: 'Based on this page, explain what I just clicked: "{label}", and what I should do next.',
      proactiveOn: 'Page idle tips enabled',
      proactiveOff: 'Page idle tips disabled',
      welcome: 'Hi, I am DeepInsight AI. I can explain recommender-system models, datasets, evaluation entries, and page navigation; turn on web search when external references are needed.',
      noAiContent: 'AI did not return content.',
      stopped: 'Stopped generation',
      chatCleared: 'Chat cleared',
      openedPrefix: 'Opened',
      noArticleSummary: 'No article summary available',
      unsafeLink: 'Unsafe external link protocol blocked',
      invalidLink: 'Invalid external link',
      modelReasoning: 'Model reasoning',
      contextEnhanced: 'Context enhanced',
      articles: 'articles',
      web: 'web',
      noWeb: 'no web',
      effectiveQuery: 'Effective query',
      reasoningLevelLabel: 'Level',
      modelLabel: 'Model',
      nativeReasoning: 'Native reasoning',
      siteArticles: 'Site articles',
      webSearch: 'Web search',
      enabled: 'enabled',
      directAnswer: 'direct',
      nativeEnabled: 'enabled',
      nativeDisabled: 'disabled',
      requestedNoTrigger: 'requested but not triggered',
      notRequested: 'not requested',
    })

const reasoningLevels = computed<Array<{ value: ReasoningLevel; label: string; hint: string; desc: string }>>(() => isZh.value
  ? [
      { value: 'off', label: '直答', hint: 'off', desc: '用于页面入口、字段含义等简短问题。' },
      { value: 'quick', label: '快速', hint: 'minimal', desc: '用于快速定位模型、数据或图表入口。' },
      { value: 'low', label: '标准', hint: 'low', desc: '用于常规模型状态、页面入口和数据概览。' },
      { value: 'deep', label: '深度', hint: 'high', desc: '用于模型接入、图表结果和数据问题分析。' },
      { value: 'max', label: '极限', hint: 'xhigh', desc: '用于跨页面、多模型、多数据源的复核。' },
    ]
  : [
      { value: 'off', label: 'Direct', hint: 'off', desc: 'For page entries, field notes, and short answers.' },
      { value: 'quick', label: 'Quick', hint: 'minimal', desc: 'For quick model, data, or chart navigation.' },
      { value: 'low', label: 'Standard', hint: 'low', desc: 'For model status and metric definitions.' },
      { value: 'deep', label: 'Deep', hint: 'high', desc: 'For access gaps, evaluation differences, and data issues.' },
      { value: 'max', label: 'Max', hint: 'xhigh', desc: 'For cross-page, multi-model, multi-source review.' },
    ])

const deepThink = computed(() => reasoningLevel.value !== 'off')
const activeReasoningLabel = computed(() => reasoningLevels.value.find((level) => level.value === reasoningLevel.value)?.label || assistantText.value.reasoningStandard)
const showTypingIndicator = computed(() => {
  if (!loading.value) return false
  const last = messages.value[messages.value.length - 1]
  return !last || last.role !== 'assistant'
})
const currentPageTitle = computed(() => resolveCurrentPageTitle())
const proactiveBubbleRight = computed(() => x.value < 300)
const assistantSubtitle = computed(() => {
  const page = currentPageTitle.value
  if (lastInteraction.value && lastInteraction.value.route === route.fullPath) {
    return `${assistantText.value.viewing}: ${page} · ${lastInteraction.value.label}`
  }
  return `${assistantText.value.viewing}: ${page}`
})
const quickChips = computed(() => {
  const chips = [
    { label: assistantText.value.quickCurrent, query: buildCurrentPageQuestion() },
    { label: assistantText.value.quickModel, query: assistantText.value.queryModelOverview },
    { label: assistantText.value.quickAccess, query: assistantText.value.queryAccessTest },
    { label: assistantText.value.quickData, query: assistantText.value.queryDataCenter },
  ]
  if (lastInteraction.value?.route === route.fullPath) {
    chips.splice(1, 0, { label: assistantText.value.quickSelection, query: buildInteractionQuestion() })
  }
  return chips
})

let dragStartX = 0
let dragStartY = 0
let startLeft = 0
let startTop = 0
let hasMoved = false
let proactiveTimer: ReturnType<typeof window.setTimeout> | null = null
let collapseTimer: ReturnType<typeof window.setTimeout> | null = null
let proactivePromptedOnRoute = false

function getTime() {
  const d = new Date()
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

function resolveCurrentPageTitle() {
  locale.value
  const path = route.path
  const navItem = [...mainNavItems, ...exploreNavItems].find((item) => {
    if (item.path === path) return true
    return item.path !== '/' && path.startsWith(`${item.path}/`)
  })
  if (navItem) return t(navLabelKey(navItem.path))
  if (path.startsWith('/admin')) return assistantText.value.adminPage
  if (path.startsWith('/profile')) return assistantText.value.profilePage
  return route.name ? String(route.name) : assistantText.value.currentPageFallback
}

function compactText(value: string, max = 120) {
  const text = value.replace(/\s+/g, ' ').trim()
  if (!text) return ''
  return text.length > max ? `${text.slice(0, max - 1)}…` : text
}

function describeElement(element: Element) {
  const explicit = element.getAttribute('data-ai-context')
    || element.getAttribute('aria-label')
    || element.getAttribute('title')
  return compactText(explicit || element.textContent || '', 88)
}

function elementSignature(element: Element) {
  const tag = element.tagName.toLowerCase()
  const classes = Array.from(element.classList)
    .filter((name) => /^[a-z0-9_-]+$/i.test(name))
    .slice(0, 2)
    .join('.')
  return classes ? `${tag}.${classes}` : tag
}

function capturePageInteraction(event: MouseEvent) {
  const target = event.target
  if (!(target instanceof Element)) return
  if (assistantRef.value?.contains(target)) return
  const element = target.closest(INTERACTIVE_SELECTOR)
  if (!element) return
  const label = describeElement(element)
  if (!label) return
  lastInteraction.value = {
    label,
    tag: elementSignature(element),
    route: route.fullPath,
    at: Date.now(),
  }
  scheduleProactiveBubble()
}

function collectVisiblePageText() {
  const root = document.querySelector('main') || document.body
  const nodes = Array.from(root.querySelectorAll<HTMLElement>('h1,h2,h3,h4,p,button,a,label,summary,th,td,.card-title,.section-title,.panel-title,.metric-label'))
  const chunks: string[] = []
  const seen = new Set<string>()

  for (const node of nodes) {
    if (assistantRef.value?.contains(node)) continue
    const rect = node.getBoundingClientRect()
    const visible = rect.width > 0 && rect.height > 0 && rect.bottom >= 0 && rect.top <= window.innerHeight
    if (!visible) continue
    const text = compactText(node.getAttribute('aria-label') || node.textContent || '', 120)
    if (!text || seen.has(text)) continue
    seen.add(text)
    chunks.push(text)
    if (chunks.join(' / ').length >= PAGE_CONTEXT_TEXT_LIMIT) break
  }

  return chunks.join(' / ').slice(0, PAGE_CONTEXT_TEXT_LIMIT)
}

function buildPageContextResources() {
  const interaction = lastInteraction.value?.route === route.fullPath ? lastInteraction.value : null
  return [{
    type: 'page_context',
    title: currentPageTitle.value,
    path: route.fullPath,
    lastInteraction: interaction ? `${interaction.label} (${interaction.tag})` : '',
    visibleText: collectVisiblePageText(),
    capturedAt: new Date().toISOString(),
  }]
}

function buildCurrentPageQuestion() {
  return assistantText.value.currentPageQuestion.replace('{page}', currentPageTitle.value)
}

function buildInteractionQuestion() {
  const label = lastInteraction.value?.route === route.fullPath ? lastInteraction.value.label : ''
  return label
    ? assistantText.value.interactionQuestion.replace('{label}', label)
    : buildCurrentPageQuestion()
}

function clearProactiveTimer() {
  if (!proactiveTimer) return
  window.clearTimeout(proactiveTimer)
  proactiveTimer = null
}

function scheduleProactiveBubble(delayMs?: number) {
  clearProactiveTimer()
  proactiveBubbleVisible.value = false
  if (!proactiveEnabled.value || isExpanded.value) return
  const routeKey = route.fullPath
  const delay = delayMs ?? (proactivePromptedOnRoute ? PROACTIVE_REPEAT_IDLE_MS : PROACTIVE_FIRST_IDLE_MS)
  proactiveTimer = window.setTimeout(() => {
    if (!proactiveEnabled.value || isExpanded.value || loading.value || route.fullPath !== routeKey) return
    proactivePromptedOnRoute = true
    proactiveBubbleVisible.value = true
  }, delay)
}

function toggleProactive() {
  proactiveEnabled.value = !proactiveEnabled.value
  localStorage.setItem(STORAGE_KEYS.ASSISTANT_PROACTIVE, proactiveEnabled.value ? 'true' : 'false')
  if (proactiveEnabled.value) {
    ElMessage.success(assistantText.value.proactiveOn)
    scheduleProactiveBubble()
  } else {
    ElMessage.info(assistantText.value.proactiveOff)
    proactiveBubbleVisible.value = false
    clearProactiveTimer()
  }
}

function redirectToLoginAfterAiAuthError(error: any) {
  if (!clearAiAuthIfNeeded(error)) return false
  const message = formatAiErrorMessage(error, isZh.value)
  ElMessage.warning(message)
  collapseAssistant()
  void router.replace({ path: '/login', query: { redirect: route.fullPath } })
  return true
}

function dismissProactiveBubble() {
  proactiveBubbleVisible.value = false
  scheduleProactiveBubble(PROACTIVE_REPEAT_IDLE_MS)
}

function askCurrentPageFromBubble() {
  proactiveBubbleVisible.value = false
  if (!isExpanded.value) toggleExpand()
  nextTick(() => askAI(buildCurrentPageQuestion()))
}

function clampPosition() {
  const width = isExpanded.value ? Math.min(440, window.innerWidth - 20) : 72
  const height = isExpanded.value ? Math.min(640, window.innerHeight - 20) : 64
  x.value = Math.max(10, Math.min(window.innerWidth - width - 10, x.value))
  y.value = Math.max(10, Math.min(window.innerHeight - height - 10, y.value))
}

function startDragCollapsed(event: MouseEvent) {
  beginDrag(event, stopDragCollapsed)
}

function startDragExpanded(event: MouseEvent) {
  beginDrag(event, stopDragExpanded)
}

function beginDrag(event: MouseEvent, stopHandler: () => void) {
  isDragging.value = true
  hasMoved = false
  proactiveBubbleVisible.value = false
  clearProactiveTimer()
  dragStartX = event.clientX
  dragStartY = event.clientY
  startLeft = x.value
  startTop = y.value
  window.addEventListener('mousemove', onDrag)
  window.addEventListener('mouseup', stopHandler)
}

function onDrag(event: MouseEvent) {
  if (!isDragging.value) return
  const dx = event.clientX - dragStartX
  const dy = event.clientY - dragStartY
  if (Math.abs(dx) > 3 || Math.abs(dy) > 3) hasMoved = true
  x.value = startLeft + dx
  y.value = startTop + dy
  clampPosition()
}

function stopDragCollapsed() {
  window.removeEventListener('mousemove', onDrag)
  window.removeEventListener('mouseup', stopDragCollapsed)
  isDragging.value = false
  if (!hasMoved) {
    toggleExpand()
  } else {
    scheduleProactiveBubble()
  }
}

function stopDragExpanded() {
  window.removeEventListener('mousemove', onDrag)
  window.removeEventListener('mouseup', stopDragExpanded)
  isDragging.value = false
}

function toggleExpand() {
  if (isExpanded.value) {
    collapseAssistant()
    return
  }
  if (collapseTimer) {
    window.clearTimeout(collapseTimer)
    collapseTimer = null
  }
  isCollapsing.value = false
  isExpanded.value = true
  clampPosition()
  if (isExpanded.value && messages.value.length === 0) {
    messages.value.push({
      role: 'assistant',
      content: assistantText.value.welcome,
      time: getTime(),
    })
  }
  nextTick(() => {
    inputRef.value?.focus()
    scrollToBottom()
  })
}

function collapseAssistant() {
  if (isCollapsing.value) return
  proactiveBubbleVisible.value = false
  clearProactiveTimer()
  isCollapsing.value = true
  isExpanded.value = false
  clampPosition()
  if (collapseTimer) window.clearTimeout(collapseTimer)
  collapseTimer = window.setTimeout(() => {
    isCollapsing.value = false
    collapseTimer = null
    clampPosition()
    scheduleProactiveBubble()
  }, COLLAPSE_ANIMATION_MS)
}

async function scrollToBottom() {
  await nextTick()
  if (msgContainer.value) msgContainer.value.scrollTop = msgContainer.value.scrollHeight
}

function autoResize() {
  if (!inputRef.value) return
  inputRef.value.style.height = 'auto'
  inputRef.value.style.height = `${Math.min(inputRef.value.scrollHeight, 96)}px`
}

function normalizeChatMarkdown(text: string) {
  return text
    .replace(/(^|\n)(#{1,6})([^\s#\n])/g, '$1$2 $3')
    .replace(/([。！？；:：）\)\]】\u4e00-\u9fa5A-Za-z])(\d+\.\s+)/g, '$1\n\n$2')
    .replace(/([^\n])(-\s+\*\*)/g, '$1\n\n$2')
}

function formatContent(text: string) {
  return text ? renderMarkdown(normalizeChatMarkdown(text)) : ''
}

function normalizeReasoningDiagnostics(value: unknown): ChatReasoningDiagnostics | undefined {
  return value && typeof value === 'object' ? value as ChatReasoningDiagnostics : undefined
}

function diagnosticsBrief(item: ChatReasoningDiagnostics) {
  const native = item.model?.nativeReasoning ? assistantText.value.modelReasoning : assistantText.value.contextEnhanced
  const related = item.related ? `${assistantText.value.articles} ${item.related.matched || 0}/${item.related.limit || 0}` : ''
  const web = item.web?.attempted
    ? `${assistantText.value.web} ${item.web.returned || 0}/${item.web.limit || 0}`
    : assistantText.value.noWeb
  return [item.label || item.level || assistantText.value.reasoningStandard, native, related, web].filter(Boolean).join(' · ')
}

function diagnosticsItems(item: ChatReasoningDiagnostics) {
  const model = item.model || {}
  const related = item.related || {}
  const web = item.web || {}
  const searchQuery = typeof web.query === 'string' && web.query.trim() ? web.query.trim() : '-'
  return [
    { label: assistantText.value.effectiveQuery, value: searchQuery },
    {
      label: assistantText.value.reasoningLevelLabel,
      value: `${item.label || item.level || assistantText.value.reasoningStandard} / ${item.enabled ? assistantText.value.enabled : assistantText.value.directAnswer}`,
    },
    { label: assistantText.value.modelLabel, value: `${model.provider || '-'} / ${model.effectiveModel || model.model || '-'}` },
    {
      label: assistantText.value.nativeReasoning,
      value: model.nativeReasoning
        ? (model.nativeReasoningLabel || assistantText.value.nativeEnabled)
        : (model.nativeReasoningLabel || assistantText.value.nativeDisabled),
    },
    { label: assistantText.value.siteArticles, value: `${related.matched || 0} / ${related.limit || 0}` },
    {
      label: assistantText.value.webSearch,
      value: web.attempted
        ? `${web.returned || 0} / ${web.limit || 0}`
        : (web.requested ? assistantText.value.requestedNoTrigger : assistantText.value.notRequested),
    },
  ]
}

function shouldShowThinking(msg: AssistantMessage, index: number) {
  return Boolean(msg.thinking) || (loading.value && index === messages.value.length - 1 && msg.role === 'assistant')
}

function thinkingLabel(msg: AssistantMessage, index: number) {
  if (loading.value && index === messages.value.length - 1 && msg.role === 'assistant') {
    return assistantText.value.thinkingRunning
  }
  if (!msg.thinkingDuration) return assistantText.value.thinkingDone
  const seconds = assistantText.value.thinkingSeconds.replace('{seconds}', String(msg.thinkingDuration))
  return isZh.value ? `${assistantText.value.thinkingDone}（${seconds}）` : `${assistantText.value.thinkingDone} (${seconds})`
}

function normalizeWebResults(value: unknown): ChatWebResult[] {
  return Array.isArray(value) ? value.slice(0, MAX_WEB_RESULTS) : []
}

function applyStreamStatus(target: AssistantMessage, data: any) {
  const message = String(data?.message || data?.delta || data?.status || '').trim()
  if (message) target.streamStatus = message
  if (typeof data?.webSearchQuery === 'string' && data.webSearchQuery.trim()) {
    target.webSearchQuery = data.webSearchQuery.trim()
  }
  if (typeof data?.webSearchAttempted === 'boolean') {
    target.webSearchAttempted = data.webSearchAttempted
  }
  if (typeof data?.webResultCount === 'number' && data.webResultCount > 0) {
    target.webExpanded = true
  }
  void scrollToBottom()
}

function enqueueTypedText(target: AssistantMessage, field: 'content' | 'thinking', delta: string) {
  if (!delta) return
  const queue = field === 'content' ? contentTypingQueue : reasoningTypingQueue
  const currentTarget = field === 'content' ? contentTypingTarget : reasoningTypingTarget
  if (currentTarget && currentTarget !== target) flushTypedText(field, currentTarget)
  if (field === 'content') contentTypingTarget = target
  else reasoningTypingTarget = target
  queue.push(...Array.from(delta))
  startTypedText(field)
}

function startTypedText(field: 'content' | 'thinking') {
  const hasTimer = field === 'content' ? contentTypingTimer : reasoningTypingTimer
  if (hasTimer) return

  const tick = () => {
    const queue = field === 'content' ? contentTypingQueue : reasoningTypingQueue
    const target = field === 'content' ? contentTypingTarget : reasoningTypingTarget
    if (!target || !queue.length) {
      if (field === 'content') contentTypingTimer = null
      else reasoningTypingTimer = null
      return
    }

    const char = queue.shift() || ''
    if (field === 'content') {
      target.content += char
    } else {
      target.thinking = `${target.thinking || ''}${char}`
      target.thinkingExpanded = true
    }
    void scrollToBottom()

    const interval = field === 'content' ? CONTENT_TYPE_INTERVAL : REASONING_TYPE_INTERVAL
    if (field === 'content') contentTypingTimer = window.setTimeout(tick, interval)
    else reasoningTypingTimer = window.setTimeout(tick, interval)
  }

  tick()
}

function flushTypedText(field: 'content' | 'thinking', target?: AssistantMessage | null) {
  if (field === 'content') {
    if (contentTypingTimer) window.clearTimeout(contentTypingTimer)
    contentTypingTimer = null
    const activeTarget = target || contentTypingTarget
    const rest = contentTypingQueue.join('')
    contentTypingQueue = []
    if (activeTarget && rest) activeTarget.content += rest
    contentTypingTarget = null
    return
  }

  if (reasoningTypingTimer) window.clearTimeout(reasoningTypingTimer)
  reasoningTypingTimer = null
  const activeTarget = target || reasoningTypingTarget
  const rest = reasoningTypingQueue.join('')
  reasoningTypingQueue = []
  if (activeTarget && rest) {
    activeTarget.thinking = `${activeTarget.thinking || ''}${rest}`
    activeTarget.thinkingExpanded = true
  }
  reasoningTypingTarget = null
}

function flushTypedTextQueues() {
  flushTypedText('content')
  flushTypedText('thinking')
}

function discardTypedTextQueues() {
  if (contentTypingTimer) window.clearTimeout(contentTypingTimer)
  if (reasoningTypingTimer) window.clearTimeout(reasoningTypingTimer)
  contentTypingTimer = null
  reasoningTypingTimer = null
  contentTypingTarget = null
  reasoningTypingTarget = null
  contentTypingQueue = []
  reasoningTypingQueue = []
}

function toggleReasoningMenu() {
  reasoningMenuOpen.value = !reasoningMenuOpen.value
}

function selectReasoningLevel(level: ReasoningLevel) {
  reasoningLevel.value = level
  reasoningMenuOpen.value = false
}

function closeReasoningMenuOnOutside(event: MouseEvent) {
  if (!reasoningMenuOpen.value) return
  const target = event.target as Node | null
  if (target && reasoningMenuRef.value?.contains(target)) return
  reasoningMenuOpen.value = false
}

async function sendMessage() {
  if (!userInput.value.trim() || loading.value) return
  const message = userInput.value.trim()
  userInput.value = ''
  if (inputRef.value) inputRef.value.style.height = 'auto'
  messages.value.push({ role: 'user', content: message, time: getTime() })
  chatHistory.value.push({ role: 'user', content: message })

  const startedAt = Date.now()
  const assistantMessage: AssistantMessage = {
    role: 'assistant',
    content: '',
    time: getTime(),
    thinking: '',
    thinkingExpanded: true,
    navigationExpanded: true,
    relatedArticles: [],
    relatedExpanded: false,
    webResults: [],
    webExpanded: false,
    webSearchAttempted: false,
    webSearchUsed: false,
    diagnosticsExpanded: false,
  }
  messages.value.push(assistantMessage)

  loading.value = true
  const controller = new AbortController()
  streamController.value = controller
  await scrollToBottom()
  let streamFailed = false

  try {
    await aiApi.chatStream({
      message,
      history: chatHistory.value.slice(-20),
      temperature: temperature.value,
      deepThink: deepThink.value,
      reasoningLevel: reasoningLevel.value,
      webSearch: webSearchEnabled.value,
      resources: buildPageContextResources(),
      ...(conversationId.value ? { conversationId: conversationId.value } : {}),
    }, {
      onStatus(data) {
        applyStreamStatus(assistantMessage, data)
      },
      onReasoning(data) {
        if (data.delta) {
          enqueueTypedText(assistantMessage, 'thinking', data.delta)
          assistantMessage.thinkingExpanded = true
        }
      },
      onContent(data) {
        if (data.delta) {
          enqueueTypedText(assistantMessage, 'content', data.delta)
        }
      },
      onMetadata(data) {
        applyStreamMetadata(assistantMessage, data)
      },
      onError(data) {
        streamFailed = true
        discardTypedTextQueues()
        assistantMessage.content = data.message || assistantText.value.noAiContent
      },
    }, controller.signal)

    flushTypedTextQueues()
    if (!assistantMessage.content.trim()) assistantMessage.content = assistantText.value.noAiContent
    assistantMessage.content = normalizeAssistantReply(assistantMessage.content, {
      userMessage: message,
      currentPageTitle: currentPageTitle.value,
    })
  } catch (error: any) {
    if (error?.name === 'AbortError') return
    discardTypedTextQueues()
    const errorMessage = formatAiErrorMessage(error, isZh.value)
    assistantMessage.content = errorMessage
    streamFailed = true
    redirectToLoginAfterAiAuthError(error)
  } finally {
    if (streamController.value === controller) streamController.value = null
    assistantMessage.thinkingDuration = Math.max(1, Math.round((Date.now() - startedAt) / 1000))
    if (!streamFailed && assistantMessage.content.trim()) {
      chatHistory.value.push({ role: 'assistant', content: assistantMessage.content })
    }
    loading.value = false
    await scrollToBottom()
  }
}

function stopGeneration() {
  streamController.value?.abort()
  streamController.value = null
  discardTypedTextQueues()
  loading.value = false
  ElMessage.info(assistantText.value.stopped)
}

function applyStreamMetadata(target: AssistantMessage, data: any) {
  data = data && typeof data === 'object' ? data : {}
  if (data?.conversationId) conversationId.value = data.conversationId
  if (typeof data?.reply === 'string' && data.reply && !target.content.trim()) {
    target.content = data.reply
  }
  const responseNavigation = normalizeAssistantNavigation(data?.navigation)
  if ('navigation' in data) {
    target.navigation = responseNavigation || undefined
    target.navigationExpanded = true
  }
  if ('relatedArticles' in data) {
    target.relatedArticles = Array.isArray(data?.relatedArticles) ? data.relatedArticles : []
    target.relatedExpanded = false
  }
  if ('webResults' in data) {
    target.webResults = normalizeWebResults(data?.webResults)
    target.webExpanded = target.webResults.length > 0 || target.webExpanded
  }
  if ('webSearchAttempted' in data) target.webSearchAttempted = Boolean(data?.webSearchAttempted)
  if ('webSearchUsed' in data) target.webSearchUsed = Boolean(data?.webSearchUsed)
  if (typeof data?.webSearchQuery === 'string' && data.webSearchQuery.trim()) {
    target.webSearchQuery = data.webSearchQuery.trim()
  }
  if ('reasoningDiagnostics' in data) {
    target.reasoningDiagnostics = normalizeReasoningDiagnostics(data?.reasoningDiagnostics)
    target.diagnosticsExpanded = false
  }
  if ('apiStatus' in data) target.apiStatus = data?.apiStatus
  if (data?.reasoning && !target.thinking?.includes(String(data.reasoning))) {
    target.thinking = `${target.thinking || ''}${target.thinking ? '\n' : ''}${data.reasoning}`
  }
}

function askAI(query: string) {
  userInput.value = query
  void sendMessage()
}

function clearChat() {
  discardTypedTextQueues()
  messages.value = []
  chatHistory.value = []
  conversationId.value = null
  ElMessage.success(assistantText.value.chatCleared)
}

function openFullChat() {
  isExpanded.value = false
  router.push(conversationId.value ? `${ROUTES.AI}?conversationId=${conversationId.value}` : ROUTES.AI)
}

function goToNavigation(navigation: AssistantNavigationIntent) {
  isExpanded.value = false
  router.push(navigation.path)
  ElMessage.success(`${assistantText.value.openedPrefix}${isZh.value ? '' : ': '} ${navigation.label}`)
}

async function openRelatedArticle(article: ChatRelatedArticle) {
  readerOpen.value = true
  readerLoading.value = true
  readerSourceLabel.value = article.sourceLabel || assistantText.value.relatedArticles
  readerArticle.value = {
    title: article.title,
    content: article.snippet || assistantText.value.noArticleSummary,
    nodeLabel: article.sourceLabel,
  }
  try {
    if (article.source === 'knowledge_article') {
      const res = await forumApi.getKnowledgeArticle(article.id)
      readerArticle.value = res.data.data || res.data || readerArticle.value
    } else if (article.source === 'forum_post') {
      const res = await forumApi.getPost(article.id)
      readerArticle.value = res.data.data || res.data || readerArticle.value
    }
  } catch {
    // 保留摘要兜底，避免弹窗空白。
  } finally {
    readerLoading.value = false
  }
}

function openWebResult(url: string) {
  if (!url) return
  try {
    const parsed = new URL(url, window.location.origin)
    if (!['http:', 'https:'].includes(parsed.protocol)) {
      ElMessage.warning(assistantText.value.unsafeLink)
      return
    }
    window.open(parsed.href, '_blank', 'noopener,noreferrer')
  } catch {
    ElMessage.warning(assistantText.value.invalidLink)
  }
}

function openFullChatWithPrompt(navigation: AssistantNavigationIntent) {
  isExpanded.value = false
  router.push({
    path: ROUTES.AI,
    query: {
      intent: navigation.key,
      prompt: navigation.promptHint,
    },
  })
}

function handleResize() {
  clampPosition()
}

watch(() => route.fullPath, () => {
  lastInteraction.value = null
  proactivePromptedOnRoute = false
  scheduleProactiveBubble()
})

watch(isExpanded, (expanded) => {
  if (expanded) {
    proactiveBubbleVisible.value = false
    clearProactiveTimer()
    return
  }
  if (isCollapsing.value) return
  scheduleProactiveBubble()
})

onMounted(() => {
  const saved = localStorage.getItem(STORAGE_KEYS.ASSISTANT_POS)
  if (saved) {
    try {
      const position = JSON.parse(saved)
      x.value = Number(position.x) || x.value
      y.value = Number(position.y) || y.value
    } catch {
      localStorage.removeItem(STORAGE_KEYS.ASSISTANT_POS)
    }
  }
  clampPosition()
  window.addEventListener('resize', handleResize)
  document.addEventListener('click', capturePageInteraction, true)
  document.addEventListener('click', closeReasoningMenuOnOutside)
  scheduleProactiveBubble()
})

onUnmounted(() => {
  localStorage.setItem(STORAGE_KEYS.ASSISTANT_POS, JSON.stringify({ x: x.value, y: y.value }))
  clearProactiveTimer()
  if (collapseTimer) window.clearTimeout(collapseTimer)
  window.removeEventListener('resize', handleResize)
  document.removeEventListener('click', capturePageInteraction, true)
  document.removeEventListener('click', closeReasoningMenuOnOutside)
  window.removeEventListener('mousemove', onDrag)
  window.removeEventListener('mouseup', stopDragCollapsed)
  window.removeEventListener('mouseup', stopDragExpanded)
  discardTypedTextQueues()
  streamController.value?.abort()
})
</script>

<style scoped>
.assistant-container {
  --assistant-surface: #fbfdff;
  --assistant-surface-top: #ffffff;
  --assistant-bar-bg: #ffffff;
  --assistant-messages-bg: #f7faff;
  --assistant-bubble-bg: #ffffff;
  --assistant-input-bg: #ffffff;
  --assistant-menu-bg: #ffffff;
  --assistant-control-bg: rgba(var(--primary-rgb), 0.055);
  --assistant-control-hover-bg: rgba(var(--primary-rgb), 0.08);
  --assistant-border: rgba(15, 23, 42, 0.16);
  --assistant-border-strong: rgba(15, 23, 42, 0.28);
  --assistant-panel-outline: rgba(255, 255, 255, 0.92);
  --assistant-text-primary: #1d1d1f;
  --assistant-text-secondary: #475569;
  --assistant-text-muted: #64748b;
  --assistant-user-text: #06100c;
  --assistant-dot-color: rgba(15, 23, 42, 0.038);
  --assistant-panel-shadow:
    0 34px 88px rgba(15, 23, 42, 0.28),
    0 12px 26px rgba(15, 23, 42, 0.12),
    inset 0 1px 0 rgba(255, 255, 255, 0.98);

  position: fixed;
  z-index: 999;
  width: 72px;
  height: 64px;
  user-select: none;
  transition:
    width 280ms cubic-bezier(0.22, 1, 0.36, 1),
    height 280ms cubic-bezier(0.22, 1, 0.36, 1),
    filter 220ms ease;
  will-change: width, height;
}

.assistant-container.theme-dark {
  --assistant-surface: #101827;
  --assistant-surface-top: #192236;
  --assistant-bar-bg: #182235;
  --assistant-messages-bg: #111a2b;
  --assistant-bubble-bg: #1b2638;
  --assistant-input-bg: #101827;
  --assistant-menu-bg: #121b2d;
  --assistant-control-bg: rgba(var(--primary-rgb), 0.08);
  --assistant-control-hover-bg: rgba(var(--primary-rgb), 0.12);
  --assistant-border: rgba(255, 255, 255, 0.18);
  --assistant-border-strong: rgba(255, 255, 255, 0.24);
  --assistant-panel-outline: rgba(255, 255, 255, 0.08);
  --assistant-text-primary: #f8fafc;
  --assistant-text-secondary: #cbd5e1;
  --assistant-text-muted: #94a3b8;
  --assistant-user-text: #06100c;
  --assistant-dot-color: rgba(255, 255, 255, 0.035);
  --assistant-panel-shadow: 0 28px 70px rgba(0, 0, 0, 0.36), inset 0 1px 0 rgba(255, 255, 255, 0.1);
}

.assistant-container.expanded {
  width: min(440px, calc(100vw - 20px));
  height: min(640px, calc(100vh - 20px));
}

.assistant-container.collapsing {
  overflow: hidden;
  filter: saturate(1.03);
}

.assistant-orb {
  position: relative;
  width: 66px;
  height: 66px;
  display: grid;
  place-items: center;
  padding: 0;
  border: 1px solid color-mix(in srgb, var(--primary-color) 44%, rgba(255, 255, 255, 0.42));
  border-radius: 24px !important;
  color: #fff;
  cursor: grab;
  overflow: hidden;
  background:
    radial-gradient(circle at 34% 22%, rgba(255, 255, 255, 0.74), transparent 0 10%, transparent 11%),
    radial-gradient(circle at 64% 72%, color-mix(in srgb, var(--accent-glow) 58%, transparent), transparent 34%),
    linear-gradient(145deg, color-mix(in srgb, var(--primary-color) 82%, #ffffff) 0%, var(--primary-color) 46%, color-mix(in srgb, var(--accent-glow) 64%, #1d2433) 100%);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.54),
    inset 0 -16px 28px rgba(5, 15, 23, 0.2),
    0 18px 42px rgba(var(--primary-rgb), 0.3),
    0 12px 34px rgba(15, 23, 42, 0.24);
  backdrop-filter: blur(14px) saturate(156%);
  -webkit-backdrop-filter: blur(14px) saturate(156%);
  will-change: transform, box-shadow;
  animation: assistantOrbBreath 7.2s var(--ease-smooth) infinite;
  transition:
    transform var(--motion-medium) var(--ease-liquid),
    border-color var(--motion-quick) ease,
    box-shadow var(--motion-medium) var(--ease-smooth),
    background var(--motion-medium) var(--ease-smooth);
}

.assistant-orb::before {
  content: "";
  position: absolute;
  inset: 8px;
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.22);
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.18), transparent 42%),
    radial-gradient(circle at 50% 50%, rgba(255, 255, 255, 0.09), transparent 54%);
  pointer-events: none;
}

.assistant-orb-halo,
.assistant-orb-ring {
  position: absolute;
  pointer-events: none;
}

.assistant-orb-halo {
  inset: 6px;
  border-radius: 22px;
  background:
    conic-gradient(from 210deg, transparent 0 20%, rgba(255, 255, 255, 0.52), transparent 45% 72%, color-mix(in srgb, var(--accent-glow) 48%, transparent), transparent 88% 100%);
  opacity: 0.58;
  mask-image: radial-gradient(circle, transparent 51%, black 54%, black 61%, transparent 64%);
  animation: assistantOrbOrbit 9.6s linear infinite;
}

.assistant-orb-ring {
  width: 34px;
  height: 34px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow:
    0 0 0 7px rgba(255, 255, 255, 0.035),
    0 0 26px rgba(var(--primary-rgb), 0.34);
}

.assistant-orb-mark {
  position: relative;
  z-index: 1;
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  border-radius: 16px;
  background:
    radial-gradient(circle at 34% 24%, rgba(255, 255, 255, 0.82), transparent 34%),
    rgba(255, 255, 255, 0.18);
  color: #ffffff;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.46),
    inset 0 -12px 18px rgba(0, 0, 0, 0.08),
    0 10px 22px rgba(15, 23, 42, 0.2);
}

.assistant-orb svg,
.assistant-actions button svg,
.assistant-send svg,
.assistant-reasoning-trigger svg {
  transform-origin: center;
  transition: transform 420ms var(--ease-liquid), filter 360ms ease, color 220ms ease;
}

.assistant-orb:hover {
  transform: translate3d(0, -2px, 0) scale(1.014);
  border-color: color-mix(in srgb, var(--primary-color) 34%, rgba(255, 255, 255, 0.7));
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.62),
    inset 0 -16px 28px rgba(5, 15, 23, 0.18),
    0 24px 54px rgba(var(--primary-rgb), 0.34),
    0 18px 42px rgba(15, 23, 42, 0.24);
}

.assistant-orb:active {
  transform: translate3d(0, 0.5px, 0) scale(0.988);
  transition-duration: 150ms;
}

.assistant-orb:hover .assistant-orb-halo {
  opacity: 0.72;
  animation-duration: 7.2s;
}

.assistant-orb:hover svg,
.assistant-actions button:hover svg,
.assistant-send:hover svg,
.assistant-reasoning-trigger:hover svg {
  animation: assistantIconKinetic 520ms var(--ease-liquid);
  filter: drop-shadow(0 0 6px rgba(255, 255, 255, 0.13));
}

.assistant-orb:hover :deep(svg[fill="none"] :where(path, line, polyline, polygon, circle, rect)),
.assistant-actions button:hover :deep(svg[fill="none"] :where(path, line, polyline, polygon, circle, rect)),
.assistant-send:hover :deep(svg[fill="none"] :where(path, line, polyline, polygon, circle, rect)),
.assistant-reasoning-trigger:hover :deep(svg[fill="none"] :where(path, line, polyline, polygon, circle, rect)) {
  animation: assistantIconLineFlow 680ms var(--ease-liquid);
  filter: drop-shadow(0 0 5px rgba(255, 255, 255, 0.12));
}

.assistant-proactive-bubble {
  position: absolute;
  right: calc(100% + 12px);
  bottom: 0;
  width: min(278px, calc(100vw - 110px));
  display: grid;
  gap: 10px;
  padding: 12px;
  border: 1px solid var(--assistant-border);
  border-radius: 18px;
  background:
    linear-gradient(135deg, rgba(var(--primary-rgb), 0.12), transparent 58%),
    var(--assistant-surface);
  color: var(--assistant-text-primary);
  box-shadow: 0 18px 48px rgba(15, 23, 42, 0.22);
  animation: assistantBubbleIn 220ms var(--ease-smooth) both;
}

.assistant-container.bubble-right .assistant-proactive-bubble {
  right: auto;
  left: calc(100% + 12px);
}

.assistant-proactive-bubble::after {
  content: '';
  position: absolute;
  right: -7px;
  bottom: 22px;
  width: 12px;
  height: 12px;
  transform: rotate(45deg);
  border-top: 1px solid var(--assistant-border);
  border-right: 1px solid var(--assistant-border);
  background: var(--assistant-surface);
}

.assistant-container.bubble-right .assistant-proactive-bubble::after {
  right: auto;
  left: -7px;
  border: 0;
  border-left: 1px solid var(--assistant-border);
  border-bottom: 1px solid var(--assistant-border);
}

.assistant-proactive-bubble span {
  color: var(--assistant-text-primary);
  font-size: 12px;
  line-height: 1.55;
}

.assistant-proactive-bubble div {
  display: flex;
  gap: 7px;
}

.assistant-proactive-bubble button {
  min-height: 28px;
  padding: 0 10px;
  border: 0;
  border-radius: 999px;
  background: var(--primary-color);
  color: var(--assistant-user-text);
  cursor: pointer;
  font-size: 11px;
  font-weight: var(--font-weight-title);
}

.assistant-proactive-bubble button.ghost {
  border: 1px solid rgba(var(--primary-rgb), 0.16);
  background: rgba(var(--primary-rgb), 0.08);
  color: var(--primary-color);
}

.assistant-panel {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border-radius: var(--radius-sm) !important;
  transform-origin: top left;
  transition:
    opacity 220ms ease,
    transform 280ms cubic-bezier(0.22, 1, 0.36, 1),
    filter 220ms ease;
  will-change: opacity, transform;
}

.assistant-panel.closing {
  pointer-events: none;
  opacity: 0;
  transform: scale(0.74);
  filter: blur(1px);
}

.assistant-opaque-surface {
  border: 1px solid var(--assistant-border);
  outline: 1px solid var(--assistant-panel-outline);
  background-color: var(--assistant-surface) !important;
  background-image: linear-gradient(180deg, #ffffff 0%, var(--assistant-surface) 100%) !important;
  color: var(--assistant-text-primary);
  box-shadow: var(--assistant-panel-shadow);
  backdrop-filter: none;
  -webkit-backdrop-filter: none;
}

.assistant-container.theme-dark .assistant-opaque-surface {
  background-image: linear-gradient(180deg, var(--assistant-surface-top) 0%, var(--assistant-surface) 100%) !important;
}

.assistant-header {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 0 12px;
  border-bottom: 1px solid var(--assistant-border);
  background: var(--assistant-bar-bg);
  cursor: grab;
}

.assistant-title {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.assistant-title > span,
.message-avatar {
  width: 32px;
  height: 32px;
  flex: 0 0 32px;
  display: grid;
  place-items: center;
  border-radius: 10px;
  background: rgba(var(--primary-rgb), 0.13);
  color: var(--primary-color);
}

.assistant-title h3 {
  margin: 0;
  color: var(--assistant-text-primary);
  font-size: 13px;
  font-weight: var(--font-weight-title);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.assistant-title p {
  margin: 2px 0 0;
  color: var(--assistant-text-muted);
  font-size: 11px;
  font-weight: var(--font-weight-body);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.assistant-actions,
.quick-chips {
  display: flex;
  gap: 7px;
}

.assistant-actions button,
.quick-chips button,
.assistant-input button,
.assistant-thinking {
  border: 0;
  cursor: pointer;
  font: inherit;
}

.assistant-actions button {
  width: 30px;
  min-width: 0;
  height: 30px;
  min-height: 0;
  max-height: 30px;
  flex: 0 0 30px;
  padding: 0;
  display: grid;
  place-items: center;
  border-radius: 10px;
  background: transparent;
  color: var(--assistant-text-secondary);
  transition:
    transform 220ms var(--ease-smooth),
    background 180ms ease,
    color 180ms ease,
    box-shadow 220ms ease;
}

.assistant-actions button:hover {
  background: rgba(var(--primary-rgb), 0.1);
  color: var(--primary-color);
  transform: translateY(-1px);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.1);
}

.assistant-actions button.active {
  background: rgba(var(--primary-rgb), 0.13);
  color: var(--primary-color);
}

.quick-chips button {
  min-width: 0;
  max-width: 100%;
  height: 30px;
  min-height: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 6px 10px;
  border-radius: 999px;
  border: 1px solid rgba(var(--primary-rgb), 0.12);
  background: rgba(var(--primary-rgb), 0.06);
  color: var(--assistant-text-secondary);
  font-size: 10px;
  font-weight: var(--font-weight-title);
}

.quick-chips button span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.assistant-messages {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 14px 14px 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  background-color: var(--assistant-messages-bg);
  background-image: radial-gradient(var(--assistant-dot-color) 1px, transparent 1px);
  background-size: 18px 18px;
}

.message-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  min-width: 0;
}

.message-row.user {
  justify-content: flex-end;
}

.user-avatar {
  background: var(--assistant-bubble-bg);
  color: var(--assistant-text-secondary);
}

.message-stack {
  max-width: min(86%, 332px);
  min-width: 0;
}

.message-bubble,
.typing-bubble {
  padding: 9px 12px;
  border: 1px solid var(--assistant-border);
  border-radius: 14px;
  background: var(--assistant-bubble-bg);
  color: var(--assistant-text-primary);
  font-size: 12px;
  line-height: 1.58;
  white-space: normal;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.message-bubble :deep(:first-child) {
  margin-top: 0;
}

.message-bubble :deep(:last-child) {
  margin-bottom: 0;
}

.message-bubble :deep(p) {
  margin: 0 0 8px;
}

.message-bubble :deep(ol),
.message-bubble :deep(ul) {
  margin: 8px 0;
  padding-left: 18px;
}

.message-bubble :deep(li) {
  margin: 4px 0;
  padding-left: 2px;
}

.message-bubble :deep(strong) {
  color: var(--assistant-text-primary);
  font-weight: var(--font-weight-title);
}

.message-bubble :deep(code) {
  padding: 1px 5px;
  border-radius: 6px;
  background: rgba(var(--primary-rgb), 0.1);
  color: var(--primary-color);
  font-family: "JetBrains Mono", Consolas, monospace;
  font-size: 0.92em;
}

.message-bubble :deep(pre) {
  overflow-x: auto;
  margin: 8px 0;
  padding: 10px;
  border-radius: 10px;
  background: color-mix(in srgb, var(--assistant-surface) 80%, black);
}

.message-bubble :deep(pre code) {
  padding: 0;
  background: transparent;
  color: inherit;
}

.message-action-card,
.message-diagnostics-card {
  margin-top: 8px;
  padding: 10px;
  border: 1px solid rgba(var(--primary-rgb), 0.2);
  border-radius: 14px;
  background-color: var(--assistant-bubble-bg);
  background-image: linear-gradient(135deg, rgba(var(--primary-rgb), 0.15), transparent 64%);
  display: grid;
  gap: 9px;
}

.message-diagnostics-card {
  padding: 9px;
  border-color: rgba(var(--primary-rgb), 0.16);
  background-image: linear-gradient(135deg, rgba(var(--primary-rgb), 0.11), rgba(99, 102, 241, 0.08));
}

.message-action-body {
  min-width: 0;
  display: grid;
  gap: 7px;
  padding-top: 8px;
  border-top: 1px solid rgba(var(--primary-rgb), 0.12);
}

.message-card-toggle,
.message-diagnostics-toggle,
.resource-card-title {
  width: 100%;
  min-height: 0;
  padding: 0;
  border: 0;
  background: transparent;
  color: inherit;
  cursor: pointer;
  font: inherit;
  text-align: left;
}

.message-card-toggle,
.message-diagnostics-toggle {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.message-card-toggle > span {
  min-width: 0;
  display: grid;
  gap: 2px;
}

.message-card-toggle b,
.message-diagnostics-toggle b,
.resource-card-title b {
  flex: 0 0 auto;
  color: var(--primary-color);
  font-size: 10px;
  font-weight: var(--font-weight-title);
  opacity: 0.78;
}

.message-diagnostics-toggle span {
  color: var(--primary-color);
  font-size: 10px;
  font-weight: var(--font-weight-title);
}

.message-diagnostics-toggle b {
  min-width: 0;
  overflow: hidden;
  color: var(--assistant-text-muted);
  text-align: right;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.message-diagnostics-body {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 6px;
  padding-top: 8px;
  border-top: 1px solid rgba(var(--primary-rgb), 0.1);
}

.message-diagnostics-body div {
  min-width: 0;
  padding: 7px;
  border: 1px solid rgba(var(--primary-rgb), 0.1);
  border-radius: 10px;
  background: rgba(var(--primary-rgb), 0.045);
}

.message-diagnostics-body span,
.message-diagnostics-body strong {
  display: block;
}

.message-diagnostics-body span {
  color: var(--assistant-text-muted);
  font-size: 9px;
  font-weight: var(--font-weight-title);
}

.message-diagnostics-body strong {
  overflow-wrap: anywhere;
  margin-top: 3px;
  color: var(--assistant-text-primary);
  font-size: 10px;
  line-height: 1.35;
}

.message-action-card strong {
  color: var(--assistant-text-primary);
  font-size: 12px;
  font-weight: var(--font-weight-title);
}

.message-action-card span {
  color: var(--assistant-text-muted);
  font-size: 11px;
  line-height: 1.45;
  overflow-wrap: anywhere;
}

.message-action-card small {
  color: var(--assistant-text-muted);
  font-size: 10px;
  line-height: 1.35;
}

.message-action-buttons button {
  min-height: 30px;
  min-width: 0;
  padding: 0 10px;
  border: 0;
  border-radius: 11px;
  background: var(--primary-color);
  color: var(--assistant-user-text);
  cursor: pointer;
  font: inherit;
  font-size: 11px;
  font-weight: var(--font-weight-title);
}

.message-action-buttons {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 7px;
}

.message-action-buttons button.ghost {
  border: 1px solid rgba(var(--primary-rgb), 0.18);
  background: rgba(var(--primary-rgb), 0.08);
  color: var(--primary-color);
}

.message-resource-card {
  margin-top: 8px;
  padding: 9px;
  border: 1px solid rgba(var(--primary-rgb), 0.14);
  border-radius: 14px;
  background: color-mix(in srgb, var(--assistant-bubble-bg) 88%, rgba(var(--primary-rgb), 0.12));
  display: grid;
  gap: 7px;
}

.message-resource-card.web {
  border-color: rgba(59, 130, 246, 0.18);
}

.message-resource-card.web .resource-list {
  max-height: 260px;
  overflow-y: auto;
  padding-right: 2px;
}

.resource-card-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 5px;
  color: var(--primary-color);
  font-size: 10px;
  font-weight: var(--font-weight-title);
}

.resource-card-title span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.resource-list {
  display: grid;
  gap: 7px;
}

.resource-query {
  color: var(--assistant-text-muted);
  font-size: 10px;
  line-height: 1.45;
}

.resource-item {
  width: 100%;
  display: grid;
  gap: 3px;
  padding: 7px 8px;
  border: 1px solid rgba(var(--primary-rgb), 0.12) !important;
  border-radius: 11px;
  background: rgba(var(--primary-rgb), 0.045);
  color: var(--assistant-text-primary);
  text-align: left;
  cursor: pointer;
}

.resource-item:hover {
  border-color: rgba(var(--primary-rgb), 0.22) !important;
  background: rgba(var(--primary-rgb), 0.075);
}

.resource-item strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 11px;
  font-weight: var(--font-weight-title);
}

.resource-item span {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  color: var(--assistant-text-muted);
  font-size: 10px;
  line-height: 1.4;
}

.resource-item .resource-title-line {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  gap: 6px;
  min-width: 0;
  overflow: visible;
  -webkit-line-clamp: unset;
  -webkit-box-orient: initial;
  color: var(--assistant-text-primary);
}

.resource-title-line b {
  min-width: 30px;
  padding: 2px 6px;
  border-radius: 999px;
  background: rgba(var(--primary-rgb), 0.12);
  color: var(--primary-color);
  font-size: 9px;
  font-weight: var(--font-weight-title);
  line-height: 1.35;
  text-align: center;
}

.resource-title-line strong {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.resource-empty {
  margin: 0;
  color: var(--assistant-text-muted);
  font-size: 10px;
  line-height: 1.45;
}

.message-row.user .message-bubble {
  border-color: rgba(255, 255, 255, 0.28);
  background: var(--primary-color);
  color: var(--assistant-user-text);
}

.message-stack time {
  display: block;
  margin-top: 4px;
  color: var(--assistant-text-muted);
  font-size: 10px;
}

.assistant-thinking {
  width: auto;
  max-width: 100%;
  display: inline-flex;
  margin: 0 0 7px;
  padding: 0;
  border: 0 !important;
  border-radius: 0;
  text-align: left;
  background: transparent;
  color: var(--assistant-text-muted);
  box-shadow: none;
  font-size: 12px;
  font-weight: var(--font-weight-title);
}

.assistant-thinking.pending {
  cursor: default;
}

.assistant-thinking-head {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  min-width: 0;
  color: var(--assistant-text-muted);
  line-height: 1.35;
}

.assistant-thinking-head svg:first-child {
  color: var(--primary-color);
}

.assistant-thinking-head svg:last-child {
  color: var(--assistant-text-muted);
  transition: transform 160ms ease;
}

.assistant-thinking.open .assistant-thinking-head svg:last-child {
  transform: rotate(180deg);
}

.assistant-thinking-head strong {
  overflow: hidden;
  font-weight: var(--font-weight-body);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.assistant-thinking-stream {
  display: grid;
  grid-template-columns: 10px minmax(0, 1fr);
  gap: 8px;
  margin: 0 0 8px 2px;
  color: var(--assistant-text-secondary);
}

.assistant-thinking-stream::before {
  content: '';
  width: 7px;
  height: 7px;
  margin-top: 0.74em;
  border-radius: 999px;
  background: var(--primary-color);
  box-shadow: 0 0 0 3px rgba(var(--primary-rgb), 0.1);
}

.assistant-thinking-stream.live::before {
  animation: assistantThinkingPulse 1.15s ease-in-out infinite;
}

.assistant-thinking-stream pre {
  margin: 0;
  padding: 0;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  color: var(--assistant-text-secondary);
  font-size: 11px;
  line-height: 1.65;
  font-family: inherit;
}

.assistant-run-status {
  width: fit-content;
  max-width: 100%;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin: 0 0 8px;
  padding: 5px 8px;
  border: 1px solid rgba(var(--primary-rgb), 0.16);
  border-radius: 999px;
  background: color-mix(in srgb, var(--assistant-bubble-bg) 84%, rgba(var(--primary-rgb), 0.12));
  color: var(--assistant-text-muted);
  font-size: 10px;
  line-height: 1.35;
}

.assistant-run-status svg {
  flex: 0 0 auto;
  color: var(--primary-color);
  animation: assistantThinkingPulse 1.15s ease-in-out infinite;
}

.typing-bubble i {
  display: inline-block;
  width: 6px;
  height: 6px;
  margin-right: 4px;
  border-radius: 999px;
  background: var(--primary-color);
  animation: typing 1.1s infinite ease-in-out;
}

.typing-bubble i:nth-child(2) { animation-delay: 140ms; }
.typing-bubble i:nth-child(3) { animation-delay: 280ms; }

.quick-chips {
  flex-wrap: nowrap;
  overflow-x: auto;
  padding: 8px 12px;
  border-top: 1px solid var(--assistant-border);
  background: var(--assistant-bar-bg);
  scrollbar-width: none;
}

.quick-chips::-webkit-scrollbar {
  display: none;
}

.quick-chips button {
  color: var(--primary-color);
}

.assistant-context-strip {
  display: grid;
  gap: 3px;
  padding: 8px 12px;
  border-top: 1px solid var(--assistant-border);
  background: color-mix(in srgb, var(--assistant-bar-bg) 86%, rgba(var(--primary-rgb), 0.12));
}

.assistant-context-strip span,
.assistant-context-strip strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 10px;
}

.assistant-context-strip span {
  color: var(--assistant-text-muted);
}

.assistant-context-strip strong {
  color: var(--primary-color);
  font-weight: var(--font-weight-title);
}

.assistant-input {
  display: grid;
  grid-template-columns: 1fr 38px;
  align-items: end;
  gap: 8px;
  padding: 10px 12px 12px;
  border-top: 1px solid var(--assistant-border);
  background: var(--assistant-bar-bg);
  overflow: visible;
}

.assistant-input-shell {
  position: relative;
  display: grid;
  min-width: 0;
  border: 1px solid var(--assistant-border);
  border-radius: 16px;
  background-color: var(--assistant-input-bg);
  background-image: linear-gradient(180deg, rgba(255, 255, 255, 0.04), transparent 42%);
  transition: border-color 160ms ease, box-shadow 160ms ease;
}

.assistant-input-shell:focus-within {
  border-color: var(--assistant-border-strong);
  box-shadow: 0 0 0 3px rgba(var(--primary-rgb), 0.075);
}

.assistant-input textarea {
  width: 100%;
  max-height: 96px;
  min-height: 36px;
  resize: none;
  border: 0;
  border-radius: 16px 16px 10px 10px;
  padding: 9px 11px 2px;
  outline: none;
  background: transparent;
  color: var(--assistant-text-primary);
  font-size: 13px;
  line-height: 1.45;
}

.assistant-input textarea:focus {
  box-shadow: none;
}

.assistant-input-tools {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  padding: 2px 6px 6px;
}

.assistant-reasoning {
  position: relative;
}

.assistant-reasoning-trigger,
.assistant-temperature,
.assistant-web-toggle {
  height: 26px;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  border: 1px solid rgba(var(--primary-rgb), 0.18) !important;
  border-radius: 999px !important;
  background: var(--assistant-control-bg) !important;
  color: var(--assistant-text-secondary) !important;
  box-shadow: none !important;
  font-size: 10px;
  font-weight: var(--font-weight-title);
}

.assistant-web-toggle {
  min-width: 0;
  padding: 0 8px;
  transition:
    transform 180ms var(--ease-smooth),
    border-color 180ms ease,
    background 180ms ease,
    color 180ms ease;
}

.assistant-web-toggle svg {
  color: var(--primary-color);
}

.assistant-web-toggle.active {
  border-color: rgba(var(--primary-rgb), 0.34) !important;
  background: rgba(var(--primary-rgb), 0.14) !important;
  color: var(--primary-color) !important;
}

.assistant-web-toggle:hover {
  transform: translateY(-0.5px);
  border-color: var(--assistant-border-strong) !important;
}

.assistant-reasoning-trigger {
  min-width: 0;
  padding: 0 8px;
  transition:
    transform 180ms var(--ease-smooth),
    border-color 180ms ease,
    background 180ms ease,
    color 180ms ease,
    box-shadow 200ms ease;
}

.assistant-reasoning-trigger svg {
  color: var(--primary-color);
}

.assistant-reasoning.open .assistant-reasoning-trigger {
  border-color: var(--assistant-border-strong) !important;
  color: var(--assistant-text-primary) !important;
}

.assistant-reasoning-trigger:hover {
  transform: translateY(-0.5px);
  border-color: var(--assistant-border-strong) !important;
  box-shadow: 0 12px 26px rgba(0, 0, 0, 0.16) !important;
}

.assistant-reasoning-menu {
  position: absolute;
  left: 0;
  bottom: calc(100% + 9px);
  z-index: 5;
  width: min(288px, calc(100vw - 64px));
  display: grid;
  gap: 6px;
  padding: 9px;
  border: 1px solid var(--assistant-border);
  border-radius: 16px;
  background-color: var(--assistant-menu-bg);
  background-image: linear-gradient(180deg, rgba(255, 255, 255, 0.08), rgba(255, 255, 255, 0.022) 42%, rgba(0, 0, 0, 0.12));
  box-shadow: 0 18px 52px rgba(0, 0, 0, 0.34), inset 0 1px 0 rgba(255, 255, 255, 0.08);
  backdrop-filter: none;
  -webkit-backdrop-filter: none;
  animation: assistantMenuRise 160ms ease both;
}

.assistant-reasoning-menu button {
  height: auto;
  display: grid;
  grid-template-columns: 48px 1fr;
  gap: 2px 8px;
  padding: 8px 9px;
  border: 1px solid rgba(148, 163, 184, 0.14);
  border-radius: 12px;
  text-align: left;
  background: var(--assistant-bubble-bg);
  color: var(--assistant-text-secondary);
  box-shadow: none;
  transition: background 160ms ease, border-color 160ms ease, color 160ms ease;
}

.assistant-reasoning-menu button:hover {
  border-color: rgba(var(--primary-rgb), 0.2);
  background: var(--assistant-control-hover-bg);
  color: var(--assistant-text-primary);
}

.assistant-reasoning-menu button.active {
  border-color: rgba(var(--primary-rgb), 0.24);
  background: rgba(var(--primary-rgb), 0.12);
  color: var(--primary-color);
}

.assistant-reasoning-menu strong {
  font-size: 11px;
  font-weight: var(--font-weight-title);
}

.assistant-reasoning-menu em {
  justify-self: start;
  padding: 1px 6px;
  border-radius: 999px;
  background: rgba(var(--primary-rgb), 0.1);
  color: var(--primary-color);
  font-size: 9px;
  font-style: normal;
  font-weight: var(--font-weight-title);
}

.assistant-reasoning-menu span {
  grid-column: 1 / -1;
  color: var(--assistant-text-muted);
  font-size: 10px;
  font-weight: var(--font-weight-body);
  line-height: 1.45;
}

.assistant-temperature {
  flex: 1;
  min-width: 0;
  max-width: 136px;
  justify-content: flex-end;
  padding: 0 6px;
}

.assistant-temperature span {
  color: var(--assistant-text-muted);
}

.assistant-temperature input {
  width: min(78px, 22vw);
  accent-color: var(--primary-color);
}

.assistant-input .assistant-send {
  width: 38px;
  min-width: 0;
  height: 38px;
  padding: 0;
  display: grid;
  place-items: center;
  border-radius: 14px;
  background: var(--primary-color);
  color: #06100c;
  transition:
    transform 220ms var(--ease-smooth),
    box-shadow 220ms ease,
    filter 180ms ease;
}

.assistant-input .assistant-send:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow:
    0 14px 30px rgba(0, 0, 0, 0.18),
    0 0 0 1px rgba(255, 255, 255, 0.28) inset;
  filter: saturate(1.08) brightness(1.04);
}

.assistant-input .assistant-send:disabled {
  opacity: 0.42;
  cursor: not-allowed;
}

@keyframes typing {
  0%, 80%, 100% { opacity: 0.25; transform: translateY(0); }
  40% { opacity: 1; transform: translateY(-3px); }
}

@keyframes assistantThinkingPulse {
  0%, 100% { opacity: 0.62; transform: scale(0.92); }
  50% { opacity: 1; transform: scale(1.12); }
}

@keyframes assistantOrbBreath {
  0%, 100% {
    box-shadow:
      inset 0 1px 0 rgba(255, 255, 255, 0.54),
      inset 0 -16px 28px rgba(5, 15, 23, 0.2),
      0 18px 42px rgba(var(--primary-rgb), 0.3),
      0 12px 34px rgba(15, 23, 42, 0.24);
  }

  50% {
    box-shadow:
      inset 0 1px 0 rgba(255, 255, 255, 0.62),
      inset 0 -16px 28px rgba(5, 15, 23, 0.18),
      0 20px 50px rgba(var(--primary-rgb), 0.4),
      0 14px 38px rgba(15, 23, 42, 0.26);
  }
}

@keyframes assistantOrbOrbit {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@keyframes assistantIconKinetic {
  0% { transform: translate3d(0, 0, 0) scale(1); }
  46% { transform: translate3d(0, -0.4px, 0) scale(1.018); }
  76% { transform: translate3d(0, 0, 0) scale(1.006); }
  100% { transform: translate3d(0, 0, 0) scale(1); }
}

@keyframes assistantIconLineFlow {
  0% {
    stroke-dasharray: 1 0;
    stroke-dashoffset: 0;
    opacity: 0.82;
  }

  38% {
    stroke-dasharray: 10 18;
    stroke-dashoffset: 14;
    opacity: 0.92;
  }

  72% {
    stroke-dasharray: 10 18;
    stroke-dashoffset: 0;
    opacity: 1;
  }

  100% {
    stroke-dasharray: 1 0;
    stroke-dashoffset: 0;
    opacity: 1;
  }
}

@keyframes assistantMenuRise {
  from { opacity: 0; transform: translateY(6px) scale(0.98); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

@keyframes assistantBubbleIn {
  from { opacity: 0; transform: translateY(8px) scale(0.96); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

@media (prefers-reduced-motion: reduce) {
  .assistant-orb {
    animation: none;
  }

  .assistant-orb:hover svg,
  .assistant-actions button:hover svg,
  .assistant-send:hover svg,
  .assistant-reasoning-trigger:hover svg {
    animation: none;
  }

  .assistant-orb:hover :deep(svg[fill="none"] :where(path, line, polyline, polygon, circle, rect)),
  .assistant-actions button:hover :deep(svg[fill="none"] :where(path, line, polyline, polygon, circle, rect)),
  .assistant-send:hover :deep(svg[fill="none"] :where(path, line, polyline, polygon, circle, rect)),
  .assistant-reasoning-trigger:hover :deep(svg[fill="none"] :where(path, line, polyline, polygon, circle, rect)) {
    animation: none;
  }

  .assistant-proactive-bubble {
    animation: none;
  }
}

@media (max-width: 560px) {
  .assistant-container.expanded {
    left: 10px !important;
    width: calc(100vw - 20px);
    height: calc(100vh - 20px);
  }

  .assistant-proactive-bubble,
  .assistant-container.bubble-right .assistant-proactive-bubble {
    right: auto;
    left: 0;
    bottom: calc(100% + 10px);
  }

  .assistant-proactive-bubble::after,
  .assistant-container.bubble-right .assistant-proactive-bubble::after {
    display: none;
  }
}

/* floating-ai-polish:start */
.assistant-container {
  --floating-shell-border: color-mix(in srgb, var(--primary-color) 16%, var(--border-color));
  --floating-glass-panel:
    linear-gradient(180deg, color-mix(in srgb, #fff 7%, transparent), color-mix(in srgb, #fff 2%, transparent)),
    color-mix(in srgb, var(--panel-bg) 84%, transparent);
  --assistant-surface: color-mix(in srgb, var(--panel-bg) 86%, transparent);
  --assistant-surface-top: color-mix(in srgb, var(--surface-2) 72%, transparent);
  --assistant-bar-bg: transparent;
  --assistant-messages-bg: transparent;
  --assistant-bubble-bg: color-mix(in srgb, var(--panel-bg) 60%, transparent);
  --assistant-input-bg: color-mix(in srgb, var(--panel-bg) 58%, transparent);
  --assistant-menu-bg: color-mix(in srgb, var(--panel-bg) 92%, transparent);
  --assistant-border: color-mix(in srgb, var(--text-primary) 10%, transparent);
  --assistant-border-strong: color-mix(in srgb, var(--text-primary) 18%, transparent);
  --assistant-panel-outline: color-mix(in srgb, #fff 8%, transparent);
  --assistant-text-primary: var(--text-primary);
  --assistant-text-secondary: var(--text-secondary);
  --assistant-text-muted: var(--text-muted);
  --assistant-user-text: var(--text-primary);
  --assistant-dot-color: color-mix(in srgb, var(--text-primary) 5%, transparent);
  --assistant-panel-shadow:
    0 26px 80px color-mix(in srgb, #000 24%, transparent),
    inset 0 1px 0 color-mix(in srgb, #fff 10%, transparent);
}

.assistant-container.expanded {
  width: min(460px, calc(100vw - 20px));
  height: min(660px, calc(100vh - 20px));
}

.assistant-orb {
  width: 62px !important;
  height: 62px !important;
  border-radius: 22px !important;
  border-color: var(--floating-shell-border) !important;
  background:
    linear-gradient(180deg, color-mix(in srgb, #fff 14%, transparent), color-mix(in srgb, var(--surface-2) 22%, transparent)),
    color-mix(in srgb, var(--panel-bg) 72%, transparent) !important;
  color: color-mix(in srgb, var(--primary-color) 78%, var(--text-primary)) !important;
  box-shadow:
    0 18px 52px color-mix(in srgb, #000 22%, transparent),
    inset 0 1px 0 color-mix(in srgb, #fff 16%, transparent) !important;
}

.assistant-orb::before {
  inset: 7px !important;
  border-radius: 18px !important;
  border-color: color-mix(in srgb, #fff 14%, transparent) !important;
  background:
    radial-gradient(circle at 34% 24%, color-mix(in srgb, var(--primary-color) 16%, transparent), transparent 38%),
    linear-gradient(135deg, color-mix(in srgb, #fff 10%, transparent), transparent 58%) !important;
}

.assistant-orb-halo {
  opacity: 0.38 !important;
}

.assistant-orb-ring {
  width: 34px !important;
  height: 34px !important;
  border-color: color-mix(in srgb, var(--primary-color) 28%, transparent) !important;
  box-shadow: 0 0 0 7px color-mix(in srgb, var(--primary-color) 7%, transparent) !important;
}

.assistant-orb-mark {
  width: 36px !important;
  height: 36px !important;
  border-radius: 14px !important;
  background:
    linear-gradient(180deg, color-mix(in srgb, #fff 13%, transparent), color-mix(in srgb, var(--surface-2) 18%, transparent)),
    color-mix(in srgb, var(--primary-color) 10%, transparent) !important;
  color: color-mix(in srgb, var(--primary-color) 82%, var(--text-primary)) !important;
}

.assistant-orb:hover {
  transform: translate3d(0, -2px, 0) scale(1.018) !important;
  box-shadow:
    0 24px 64px color-mix(in srgb, #000 26%, transparent),
    inset 0 1px 0 color-mix(in srgb, #fff 18%, transparent) !important;
}

.assistant-panel {
  border-radius: 24px !important;
}

.assistant-opaque-surface {
  border: 1px solid var(--floating-shell-border) !important;
  outline: 0 !important;
  background: var(--floating-glass-panel) !important;
  -webkit-backdrop-filter: blur(var(--glass-blur)) saturate(var(--glass-saturate)) !important;
  backdrop-filter: blur(var(--glass-blur)) saturate(var(--glass-saturate)) !important;
}

.assistant-header {
  height: 58px !important;
  padding: 0 14px !important;
  border-bottom-color: color-mix(in srgb, var(--text-primary) 8%, transparent) !important;
}

.assistant-title > span,
.message-avatar {
  border: 1px solid color-mix(in srgb, var(--primary-color) 18%, transparent) !important;
  background: color-mix(in srgb, var(--primary-color) 9%, transparent) !important;
  color: color-mix(in srgb, var(--primary-color) 78%, var(--text-primary)) !important;
}

.assistant-actions button,
.quick-chips button,
.assistant-reasoning-trigger,
.assistant-web-toggle {
  border: 1px solid color-mix(in srgb, var(--text-primary) 9%, transparent) !important;
  background: color-mix(in srgb, var(--surface-2) 36%, transparent) !important;
  color: var(--assistant-text-secondary) !important;
}

.assistant-actions button:hover,
.quick-chips button:hover,
.assistant-reasoning-trigger:hover,
.assistant-web-toggle:hover,
.assistant-web-toggle.active {
  border-color: color-mix(in srgb, var(--primary-color) 36%, var(--border-color)) !important;
  background: color-mix(in srgb, var(--primary-color) 10%, var(--surface-2)) !important;
  color: var(--assistant-text-primary) !important;
}

.assistant-messages {
  padding: 14px 14px 12px !important;
  background-color: transparent !important;
  background-image: radial-gradient(var(--assistant-dot-color) 1px, transparent 1px) !important;
  background-size: 18px 18px !important;
}

.message-stack {
  max-width: min(88%, 344px) !important;
}

.message-bubble,
.typing-bubble {
  padding: 10px 12px !important;
  border: 1px solid color-mix(in srgb, var(--text-primary) 9%, transparent) !important;
  border-radius: 16px 16px 16px 7px !important;
  background:
    linear-gradient(180deg, color-mix(in srgb, #fff 5%, transparent), transparent 42%),
    color-mix(in srgb, var(--panel-bg) 54%, transparent) !important;
  box-shadow:
    0 12px 34px color-mix(in srgb, #000 12%, transparent),
    inset 0 1px 0 color-mix(in srgb, #fff 7%, transparent) !important;
  font-size: 12px !important;
  line-height: 1.62 !important;
}

.message-row.user .message-bubble {
  border-radius: 16px 16px 7px 16px !important;
  border-color: color-mix(in srgb, var(--primary-color) 30%, var(--border-color)) !important;
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--primary-color) 15%, transparent), color-mix(in srgb, var(--surface-2) 50%, transparent)),
    color-mix(in srgb, var(--panel-bg) 76%, transparent) !important;
  color: var(--text-primary) !important;
}

.message-bubble :deep(h1),
.message-bubble :deep(h2),
.message-bubble :deep(h3) {
  margin: 10px 0 6px !important;
  color: var(--text-primary) !important;
  font-size: 13px !important;
  line-height: 1.35 !important;
}

.message-bubble :deep(table) {
  width: 100% !important;
  min-width: 320px !important;
  margin: 8px 0 !important;
  border-collapse: separate !important;
  border-spacing: 0 !important;
  border: 1px solid color-mix(in srgb, var(--text-primary) 10%, transparent) !important;
  border-radius: 12px !important;
  overflow: hidden !important;
}

.message-bubble :deep(th),
.message-bubble :deep(td) {
  padding: 7px 8px !important;
  border-bottom: 1px solid color-mix(in srgb, var(--text-primary) 8%, transparent) !important;
  font-size: 11px !important;
  line-height: 1.4 !important;
  text-align: left !important;
}

.message-action-card,
.message-diagnostics-card,
.message-resource-card {
  border: 1px solid color-mix(in srgb, var(--text-primary) 9%, transparent) !important;
  border-radius: 16px !important;
  background: color-mix(in srgb, var(--panel-bg) 58%, transparent) !important;
  box-shadow:
    0 12px 34px color-mix(in srgb, #000 12%, transparent),
    inset 0 1px 0 color-mix(in srgb, #fff 7%, transparent) !important;
}

.message-card-toggle b,
.message-diagnostics-toggle b,
.resource-card-title b,
.message-diagnostics-toggle span,
.resource-card-title,
.resource-card-title span {
  color: color-mix(in srgb, var(--primary-color) 72%, var(--text-primary)) !important;
}

.message-action-buttons button {
  min-height: 32px !important;
  border: 1px solid color-mix(in srgb, var(--primary-color) 30%, var(--border-color)) !important;
  border-radius: 12px !important;
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--primary-color) 16%, transparent), color-mix(in srgb, var(--surface-2) 44%, transparent)),
    color-mix(in srgb, var(--panel-bg) 74%, transparent) !important;
  color: var(--text-primary) !important;
}

.message-action-buttons button.ghost {
  border-color: color-mix(in srgb, var(--text-primary) 10%, transparent) !important;
  background: color-mix(in srgb, var(--surface-2) 34%, transparent) !important;
  color: var(--assistant-text-secondary) !important;
}

.assistant-context-strip {
  border-top-color: color-mix(in srgb, var(--text-primary) 8%, transparent) !important;
  background: color-mix(in srgb, var(--panel-bg) 34%, transparent) !important;
}

.assistant-input {
  border-top-color: color-mix(in srgb, var(--text-primary) 8%, transparent) !important;
  background: transparent !important;
}

.assistant-input-shell {
  border: 1px solid color-mix(in srgb, var(--text-primary) 10%, transparent) !important;
  border-radius: 18px !important;
  background: color-mix(in srgb, var(--panel-bg) 56%, transparent) !important;
  box-shadow: inset 0 1px 0 color-mix(in srgb, #fff 8%, transparent) !important;
}

.assistant-input .assistant-send {
  border-radius: 13px !important;
  background: color-mix(in srgb, var(--primary-color) 18%, var(--surface-2)) !important;
  color: var(--text-primary) !important;
  box-shadow:
    0 12px 26px color-mix(in srgb, #000 15%, transparent),
    inset 0 1px 0 color-mix(in srgb, #fff 12%, transparent) !important;
}
/* floating-ai-polish:end */
</style>
