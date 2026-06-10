<template>
  <div class="qa-page" :class="{ entered }">
    <aside class="qa-sidebar">
      <div class="brand-block">
        <span><Bot :size="22" /></span>
        <div>
          <strong>DeepInsight AI</strong>
          <em>{{ aiText.brandSubtitle }}</em>
        </div>
      </div>

      <button class="new-chat" type="button" @click="newConversation">
        <Plus :size="17" />
        {{ aiText.newChat }}
      </button>

      <section class="conversation-panel">
        <div class="panel-title">
          <span>{{ aiText.conversations }}</span>
          <b>{{ conversations.length }}</b>
        </div>
        <button
          v-for="conv in conversations"
          :key="conv.id"
          type="button"
          class="conversation-item"
          :class="{ active: activeConvId === conv.id, loading: conversationLoading && activeConvId === conv.id }"
          :disabled="conversationLoading && activeConvId === conv.id"
          @click="selectConversation(conv)"
        >
          <strong>{{ conv.title }}</strong>
          <em>{{ conversationLoading && activeConvId === conv.id ? aiText.loading : formatConversationDate(conv.date) }}</em>
          <i @click.stop="deleteConversation(conv.id)"><X :size="13" /></i>
        </button>
        <div v-if="!conversations.length" class="conversation-empty">{{ aiText.noConversations }}</div>
      </section>

      <div class="sidebar-foot">
        <div>
          <span>{{ aiText.reasoningLabel }}</span>
          <strong>{{ activeReasoning.label }}</strong>
        </div>
        <div>
          <span>{{ aiText.contextLabel }}</span>
          <strong>{{ tokenCount }} {{ aiText.chars }}</strong>
        </div>
      </div>
    </aside>

    <main class="qa-main">
      <header class="qa-topbar">
        <div>
          <span class="eyebrow">{{ aiText.workspace }}</span>
          <h1>{{ activeConv?.title || aiText.newQuestion }}</h1>
        </div>
        <div class="top-actions">
          <button
            type="button"
            class="top-icon-action"
            :class="{ active: selectedMaterials.length > 0 }"
            :title="materialText.open"
            @click="toggleMaterialPanel"
          >
            <Paperclip :size="16" />
            <b v-if="selectedMaterials.length">{{ selectedMaterials.length }}</b>
          </button>
          <button
            type="button"
            class="top-icon-action"
            :class="{ active: webSearchEnabled }"
            :title="webSearchEnabled ? aiText.webOnTitle : aiText.webOffTitle"
            @click="webSearchEnabled = !webSearchEnabled"
          >
            <Globe2 :size="15" />
          </button>
          <button type="button" class="top-icon-action" :title="aiText.exportChat" @click="exportChat"><Download :size="16" /></button>
          <button type="button" class="top-icon-action" :title="aiText.clearView" @click="clearCurrentChat"><Trash2 :size="16" /></button>
        </div>
      </header>

      <section ref="msgContainer" class="qa-scroll" @scroll="handleMessageScroll">
        <div v-if="messages.length <= 1" class="qa-hero">
          <span class="hero-mark"><Sparkles :size="28" /></span>
          <h2>{{ aiText.heroTitle }}</h2>
          <p>{{ aiText.heroDesc }}</p>
          <div class="prompt-grid">
            <button v-for="prompt in suggestedPrompts" :key="prompt.label" type="button" @click="askAI(prompt.query)">
              <span><component :is="prompt.icon" :size="17" /></span>
              <strong>{{ prompt.label }}</strong>
              <em>{{ prompt.desc }}</em>
            </button>
          </div>
        </div>

        <article v-for="(msg, index) in messages" :key="index" class="message-row" :class="msg.role">
          <div class="avatar" aria-hidden="true">
            <component :is="msg.role === 'user' ? User : Bot" :size="17" />
          </div>
          <div class="message-body">
            <template v-if="msg.role === 'user'">
              <div class="bubble" v-html="formatContent(msg.content)"></div>
              <div v-if="msg.controlSnapshot" class="message-control-strip user">
                <span>{{ msg.controlSnapshot.title }}</span>
                <b v-for="chip in controlSnapshotChips(msg.controlSnapshot)" :key="chip">{{ chip }}</b>
              </div>
              <div class="user-message-tools">
                <button type="button" :title="aiText.copy" @click="copyMsg(msg.content)">
                  <Copy :size="20" />
                </button>
                <button type="button" :title="aiText.editMessage" @click="editMessage(msg.content)">
                  <PencilLine :size="20" />
                </button>
              </div>
            </template>

            <template v-else>
            <button
              v-if="shouldShowThinking(msg, index)"
              type="button"
              class="thinking-card"
              :class="{ open: msg.thinkingExpanded }"
              :aria-expanded="msg.thinkingExpanded ? 'true' : 'false'"
              @click="msg.thinkingExpanded = !msg.thinkingExpanded"
            >
              <span class="thinking-head">
                <Atom :size="24" />
                <strong>{{ thinkingLabel(msg, index) }}</strong>
                <ChevronDown :size="18" />
              </span>
            </button>
            <div
              v-if="hasThinkingText(msg)"
              v-show="msg.thinkingExpanded"
              class="thinking-stream"
              :class="{ live: loading && index === messages.length - 1 }"
            >
              <pre>{{ msg.thinking }}</pre>
            </div>
            <div v-if="msg.streamStatus && isLiveAssistantMessage(msg, index)" class="assistant-run-status">
              <Atom :size="14" />
              <span>{{ msg.streamStatus }}</span>
            </div>
            <div v-if="msg.controlSnapshot" class="message-control-strip assistant">
              <span>{{ msg.controlSnapshot.title }}</span>
              <b v-for="chip in controlSnapshotChips(msg.controlSnapshot)" :key="chip">{{ chip }}</b>
            </div>
            <div v-if="msg.content.trim()" class="bubble" v-html="formatContent(msg.content)"></div>
            <div
              v-if="msg.reasoningDiagnostics"
              class="diagnostics-card"
              :class="{ open: msg.diagnosticsExpanded }"
            >
              <button
                type="button"
                class="diagnostics-head"
                @click="msg.diagnosticsExpanded = !msg.diagnosticsExpanded"
              >
                <span>{{ aiText.diagnosticsTitle }}</span>
                <b>{{ msg.diagnosticsExpanded ? aiText.collapse : diagnosticsBrief(msg.reasoningDiagnostics) }}</b>
              </button>
              <div v-show="msg.diagnosticsExpanded" class="diagnostics-body">
                <div v-for="item in diagnosticsItems(msg.reasoningDiagnostics)" :key="item.label">
                  <span>{{ item.label }}</span>
                  <strong>{{ item.value }}</strong>
                </div>
                <p v-if="msg.reasoningDiagnostics.strategy">{{ msg.reasoningDiagnostics.strategy }}</p>
              </div>
            </div>
            <div
              v-if="msg.navigation"
              class="navigation-card"
              :class="{ collapsed: msg.navigationExpanded === false }"
            >
              <button
                type="button"
                class="navigation-toggle"
                @click="msg.navigationExpanded = msg.navigationExpanded === false"
              >
                <div>
                  <span>{{ msg.navigation.mode === 'suggested' ? aiText.suggestedEntry : aiText.pageNavigation }}</span>
                  <strong>{{ msg.navigation.label }}</strong>
                </div>
                <b>{{ msg.navigationExpanded === false ? aiText.expand : aiText.collapse }}</b>
              </button>
              <div v-show="msg.navigationExpanded !== false" class="navigation-body">
                <div>
                  <em>{{ msg.navigation.description }}</em>
                  <small v-if="msg.navigation.reason">{{ msg.navigation.reason }}</small>
                </div>
                <button type="button" class="navigation-goto" @click="goToNavigation(msg.navigation)">{{ aiText.go }}</button>
              </div>
            </div>
            <div v-if="msg.relatedArticles?.length" class="related-card">
              <button
                type="button"
                class="related-head"
                @click="msg.relatedExpanded = !msg.relatedExpanded"
              >
                <span>
                  <BookOpen :size="15" />
                  {{ aiText.relatedArticles }}
                </span>
                <b>{{ msg.relatedExpanded ? aiText.collapse : `${aiText.expand} ${msg.relatedArticles.length}` }}</b>
              </button>
              <div v-show="msg.relatedExpanded" class="related-list">
                <button
                  v-for="article in msg.relatedArticles.slice(0, 4)"
                  :key="`${article.source}-${article.id}`"
                  type="button"
                  class="related-item"
                  @click="openRelatedArticle(article)"
                >
                  <strong>{{ article.title }}</strong>
                  <em>{{ article.sourceLabel }} · {{ article.snippet }}</em>
                </button>
              </div>
            </div>
            <div v-if="msg.webResults?.length" class="related-card web">
              <button
                type="button"
                class="related-head"
                @click="msg.webExpanded = !msg.webExpanded"
              >
                <span>
                  <Globe2 :size="15" />
                  {{ aiText.webReferences }}
                </span>
                <b>{{ msg.webExpanded ? aiText.collapse : `${aiText.expand} ${msg.webResults.length}` }}</b>
              </button>
              <small v-if="msg.webSearchQuery" v-show="msg.webExpanded" class="web-query-line">
                {{ aiText.searchQuery }}：{{ msg.webSearchQuery }}
              </small>
              <div v-show="msg.webExpanded" class="related-list">
                <button
                  v-for="(result, resultIndex) in msg.webResults.slice(0, MAX_WEB_RESULTS)"
                  :key="result.url"
                  type="button"
                  class="related-item"
                  @click="openWebResult(result.url)"
                >
                  <span class="web-result-title">
                    <b>{{ result.refId || `W${resultIndex + 1}` }}</b>
                    <strong>{{ result.title }}</strong>
                  </span>
                  <em>{{ result.source }} · {{ result.snippet }}</em>
                </button>
              </div>
            </div>
            <div v-else-if="msg.webSearchAttempted" class="related-card web">
              <button
                type="button"
                class="related-head"
                @click="msg.webExpanded = !msg.webExpanded"
              >
                <span>
                  <Globe2 :size="15" />
                  {{ aiText.webReferences }}
                </span>
                <b>{{ msg.webExpanded ? aiText.collapse : aiText.expand }}</b>
              </button>
              <small v-if="msg.webSearchQuery" v-show="msg.webExpanded" class="web-query-line">
                {{ aiText.searchQuery }}：{{ msg.webSearchQuery }}
              </small>
              <p v-show="msg.webExpanded" class="related-empty">{{ aiText.noWebResults }}</p>
            </div>
            <div v-if="msg.content.trim() || !isLiveAssistantMessage(msg, index)" class="message-meta">
              <time>{{ msg.time }}</time>
              <button type="button" :title="aiText.copy" @click="copyMsg(msg.content)">
                <Copy :size="21" />
              </button>
              <button
                v-if="index === messages.length - 1 && !loading"
                type="button"
                :title="aiText.retry"
                @click="regenerate"
              >
                <RotateCcw :size="21" />
              </button>
              <button
                type="button"
                :class="{ active: msg.feedback === 'up' }"
                :title="aiText.goodAnswer"
                @click="rateMessage(msg, 'up')"
              >
                <ThumbsUp :size="21" />
              </button>
              <button
                type="button"
                :class="{ active: msg.feedback === 'down' }"
                :title="aiText.badAnswer"
                @click="rateMessage(msg, 'down')"
              >
                <ThumbsDown :size="21" />
              </button>
              <button type="button" :title="aiText.share" @click="shareMessage(msg.content)">
                <Share2 :size="21" />
              </button>
            </div>
            </template>
          </div>
        </article>

        <article v-if="showTypingIndicator" class="message-row assistant">
          <div class="avatar"><Bot :size="17" /></div>
          <div class="message-body">
            <div class="thinking-card thinking-placeholder">
              <span class="thinking-head">
                <Atom :size="24" />
                <strong>{{ aiText.thinkingRunning }}</strong>
                <ChevronDown :size="18" />
              </span>
            </div>
          </div>
        </article>
      </section>

      <form class="composer" @submit.prevent="sendMessage">
        <div class="composer-shell" :class="{ expanded: materialPanelOpen }">
          <div v-show="materialPanelOpen" class="material-panel">
            <div class="material-panel-head">
              <div>
                <strong>{{ materialText.title }}</strong>
                <span>{{ materialText.hint }}</span>
              </div>
              <button type="button" :disabled="materialLoading" :title="materialText.refresh" @click="loadWorkspaceMaterials">
                <RefreshCw :size="15" />
              </button>
            </div>
            <label class="material-search">
              <Search :size="16" />
              <input v-model.trim="materialSearchQuery" type="search" :placeholder="materialText.searchPlaceholder" />
            </label>
            <div v-if="materialLoading" class="material-empty">{{ materialText.loading }}</div>
            <div v-else-if="!workspaceMaterials.length" class="material-empty">{{ materialText.empty }}</div>
            <div v-else class="material-picker">
              <div class="material-library-summary">
                <span>{{ materialText.scopeSummary }}</span>
                <b>{{ visibleMaterials.length }} / {{ workspaceMaterials.length }}</b>
              </div>
              <div class="material-tabs" :aria-label="materialText.title">
                <button
                  v-for="tab in materialTabs"
                  :key="tab.key"
                  type="button"
                  :class="{ active: activeMaterialScope === tab.key }"
                  @click="activeMaterialScope = tab.key"
                >
                  <span>{{ tab.label }}</span>
                  <b>{{ tab.count }}</b>
                </button>
              </div>
              <div v-if="!visibleMaterials.length" class="material-empty material-empty-inline">
                {{ materialSearchQuery ? materialText.noSearchResults : materialText.noScopeResults }}
              </div>
              <div v-else class="material-list">
                <button
                  v-for="item in visibleMaterials"
                  :key="item.key"
                  type="button"
                  class="material-row"
                  :class="[
                    item.sourceScope,
                    item.storageMode,
                    { active: selectedMaterialKeys.includes(item.key), readonly: item.readOnly },
                  ]"
                  @click="toggleMaterial(item)"
                >
                  <span class="material-check" aria-hidden="true">
                    <Check v-if="selectedMaterialKeys.includes(item.key)" :size="14" />
                  </span>
                  <span class="material-row-copy">
                    <strong>{{ item.title }}</strong>
                    <em>{{ materialDescriptor(item) }}</em>
                  </span>
                  <b class="material-badge">{{ materialScopeLabel(item) }}</b>
                </button>
              </div>
            </div>
          </div>

          <div v-if="selectedMaterials.length" class="selected-materials">
            <div class="selected-summary">
              <strong>{{ materialText.selectedPrefix }} {{ selectedMaterials.length }}</strong>
              <span>{{ selectedMaterialSummary }}</span>
            </div>
            <div class="selected-actions">
              <button type="button" class="material-analysis-action" :disabled="loading" @click="analyzeSelectedMaterials">
                <Sparkles :size="14" />
                {{ materialText.analyzeSelected }}
              </button>
              <button type="button" class="clear-materials" @click="selectedMaterialKeys = []">{{ materialText.clear }}</button>
            </div>
            <div class="selected-chips">
              <button
                v-for="item in selectedMaterials.slice(0, 4)"
                :key="item.key"
                type="button"
                :class="item.sourceScope"
                @click="toggleMaterial(item)"
              >
                <span>{{ item.title }}</span>
                <X :size="13" />
              </button>
              <span v-if="selectedMaterials.length > 4" class="selected-more">+{{ selectedMaterials.length - 4 }}</span>
            </div>
          </div>

          <textarea
            ref="inputRef"
            v-model="userInput"
            :disabled="loading"
            :placeholder="loading ? aiText.aiAnalyzing : aiText.inputPlaceholder"
            rows="1"
            @input="autoResize"
            @keydown.enter.exact.prevent="sendMessage"
            @keydown.enter.shift.exact.prevent="userInput += '\n'"
          ></textarea>

          <div class="composer-toolbar composer-dock">
            <div class="composer-left-tools">
              <button
                type="button"
                class="composer-icon-action"
                :class="{ active: materialPanelOpen || selectedMaterials.length }"
                :title="selectedMaterials.length ? `${selectedMaterials.length} ${materialText.selected}` : materialText.open"
                @click="toggleMaterialPanel"
              >
                <Plus :size="18" />
                <b v-if="selectedMaterials.length">{{ selectedMaterials.length }}</b>
              </button>
              <button
                type="button"
                class="composer-quick-action"
                :class="{ ready: canSendMessage }"
                :disabled="loading"
                @click="startConversationFromComposer"
              >
                <span>{{ aiText.startChat }}</span>
              </button>
              <button type="button" class="composer-tool composer-web-toggle" :class="{ active: webSearchEnabled }" @click="webSearchEnabled = !webSearchEnabled">
                <Globe2 :size="15" />
                <span>{{ webSearchEnabled ? aiText.webOnShort : aiText.webOffShort }}</span>
              </button>
            </div>

            <div class="composer-right-tools">
              <div class="reasoning-menu" :class="{ open: reasoningMenuOpen }">
                <button
                  type="button"
                  class="reasoning-pill"
                  :aria-expanded="reasoningMenuOpen ? 'true' : 'false'"
                  :aria-label="aiText.reasoningStrength"
                  @click="toggleReasoningMenu"
                >
                  <span>{{ aiText.reasoningLabel }}</span>
                  <strong>{{ activeReasoning.label }}</strong>
                  <ChevronDown :size="15" />
                </button>
                <div v-if="reasoningMenuOpen" class="reasoning-popover">
                  <button
                    v-for="level in reasoningLevels"
                    :key="level.value"
                    type="button"
                    :class="{ active: reasoningLevel === level.value }"
                    @click="setReasoningLevel(level.value)"
                  >
                    <span>{{ level.label }}</span>
                    <em>{{ level.desc }}</em>
                  </button>
                  <label class="reasoning-temperature">
                    <span>{{ aiText.temperatureShort }} {{ temperature.toFixed(2) }}</span>
                    <input v-model.number="temperature" type="range" min="0" max="1" step="0.05" />
                  </label>
                </div>
              </div>

              <button v-if="!loading" class="composer-submit composer-submit-round" type="submit" :title="aiText.startChat" :disabled="!canSendMessage">
                <ArrowUp :size="19" />
              </button>
              <button v-else type="button" class="composer-submit composer-submit-round stop" :title="aiText.stopped" @click="stopGeneration">
                <Square :size="18" />
              </button>
            </div>
          </div>
          <div class="composer-effect-line">
            <span>{{ activeReasoning.desc }}</span>
            <b>{{ controlEffectText }}</b>
          </div>
        </div>
      </form>
    </main>

    <ArticleReaderDrawer
      v-model="readerOpen"
      :article="readerArticle"
      :loading="readerLoading"
      :source-label="readerSourceLabel"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { ArrowUp, Atom, BookOpen, Bot, BrainCircuit, Check, ChevronDown, ClipboardList, Copy, Download, Globe2, History, Paperclip, PencilLine, Plus, RefreshCw, RotateCcw, Search, Share2, Sparkles, Square, ThumbsDown, ThumbsUp, Trash2, User, X } from 'lucide-vue-next'
import { aiApi, forumApi } from '@/api'
import type { ChatApiStatus, ChatReasoningDiagnostics, ChatRelatedArticle, ChatWebResult } from '@/api/modules/ai.api'
import ArticleReaderDrawer from '@/components/common/ArticleReaderDrawer.vue'
import { renderMarkdown } from '@/utils/markdown'
import { normalizeAssistantNavigation, type AssistantNavigationIntent } from '@/utils/assistantNavigation'
import { normalizeAssistantReply } from '@/utils/assistantReplyFallback'
import { clearAiAuthIfNeeded, formatAiErrorMessage, isAiAuthError } from '@/utils/aiError'
import { ROUTES } from '@/constants'

type ReasoningLevel = 'off' | 'quick' | 'low' | 'deep' | 'max'
type ControlSnapshot = {
  title: string
  level: ReasoningLevel
  levelLabel: string
  levelDesc: string
  temperature: number
  temperatureLabel: string
  webSearch: boolean
  materialCount: number
  relatedMatched?: number
  relatedLimit?: number
  webReturned?: number
  webLimit?: number
  modelLabel?: string
  nativeReasoning?: string
  maxTokens?: number
}
type Message = {
  role: 'user' | 'assistant'
  content: string
  time: string
  thinking?: string
  thinkingExpanded?: boolean
  thinkingDuration?: number
  feedback?: 'up' | 'down'
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
  controlSnapshot?: ControlSnapshot
}
type Conversation = { id: number; title: string; date: string }
type MaterialScope = 'stored' | 'owned' | 'official'
type MaterialScopeFilter = 'all' | MaterialScope
type MaterialStorageMode = 'persisted' | 'database' | 'virtual'
type WorkspaceMaterial = {
  key: string
  id: number
  title: string
  itemType?: string
  sourceType?: string
  format?: string
  summary?: string
  meta?: string
  fileName?: string
  updatedAt?: string
  sourceScope: MaterialScope
  storageMode: MaterialStorageMode
  origin: 'item' | 'resource'
  resourceType?: string
  official?: boolean
  readOnly?: boolean
  canManage?: boolean
  canSync?: boolean
  [key: string]: any
}
const MAX_WEB_RESULTS = 15
const CONTENT_TYPE_INTERVAL = 12
const REASONING_TYPE_INTERVAL = 10
const SCROLL_FOLLOW_THRESHOLD = 96

const route = useRoute()
const router = useRouter()
const { locale } = useI18n()
const msgContainer = ref<HTMLElement | null>(null)
const inputRef = ref<HTMLTextAreaElement | null>(null)
const loading = ref(false)
const conversationLoading = ref(false)
const entered = ref(false)
const autoFollowMessages = ref(true)
const userInput = ref('')
const conversationId = ref<number | null>(null)
const activeConvId = ref<number | null>(null)
const chatHistory = ref<Array<{ role: string; content: string }>>([])
const messages = ref<Message[]>([])
const conversations = ref<Conversation[]>([])
const workspaceMaterials = ref<WorkspaceMaterial[]>([])
const selectedMaterialKeys = ref<string[]>([])
const activeMaterialScope = ref<MaterialScopeFilter>('all')
const materialSearchQuery = ref('')
const materialPanelOpen = ref(false)
const materialLoading = ref(false)
const readerOpen = ref(false)
const readerArticle = ref<any>(null)
const readerLoading = ref(false)
const readerSourceLabel = ref('')
const reasoningLevel = ref<ReasoningLevel>('deep')
const reasoningMenuOpen = ref(false)
const temperature = ref(0.7)
const webSearchEnabled = ref(false)
const tokenCount = ref(0)
const streamController = ref<AbortController | null>(null)
let contentTypingTarget: Message | null = null
let reasoningTypingTarget: Message | null = null
let contentTypingTimer: ReturnType<typeof window.setTimeout> | null = null
let reasoningTypingTimer: ReturnType<typeof window.setTimeout> | null = null
let contentTypingQueue: string[] = []
let reasoningTypingQueue: string[] = []
const isZh = computed(() => !locale.value.startsWith('en'))

const aiText = computed(() => isZh.value
  ? {
      brandSubtitle: '模型问答 / 页面导航',
      reasoningLabel: '推理强度',
      contextLabel: '上下文',
      newChat: '新对话',
      startChat: '发起对话',
      conversations: '会话记录',
      loading: '加载中...',
      noConversations: '暂无历史会话',
      chars: '字符',
      workspace: 'AI 工作区',
      newQuestion: '新的站内问答',
      reasoningStrength: '推理强度',
      webOnTitle: '联网搜索已开启',
      webOffTitle: '联网搜索已关闭，仅使用站内知识',
      webOnShort: '联网开',
      webOffShort: '站内',
      exportChat: '导出对话',
      clearView: '清空当前视图',
      heroTitle: '询问模型、数据或页面位置，AI 返回可执行入口。',
      heroDesc: '你可以直接问模型、数据中心、接入测试、性能图表或页面入口。AI 会给出可继续操作的回答，并在需要时带你跳转到对应页面。',
      collapseReasoning: '收起推理过程',
      viewReasoning: '查看推理过程',
      diagnosticsTitle: '思考深度实际生效',
      collapse: '收起',
      expand: '展开',
      suggestedEntry: '建议入口',
      pageNavigation: '页面导航',
      go: '前往',
      relatedArticles: '相关文章',
      webReferences: '联网参考',
      searchQuery: '检索词',
      noWebResults: '已尝试联网搜索，当前没有返回可展示结果。',
      copy: '复制',
      retry: '重试',
      analyzing: '分析中',
      aiAnalyzing: 'AI 正在分析...',
      inputPlaceholder: '输入问题，Enter 发送，Shift+Enter 换行',
      welcome: '欢迎来到 **DeepInsight AI 工作区**。你可以询问模型总览、接入测试、性能看板、数据中心或页面跳转入口。',
      newChatStarted: '新的站内问答已开始。告诉我你想查看的模型、数据、页面或功能入口。',
      noConversationMessages: '该会话暂时没有历史消息。你可以继续在下方输入问题。',
      loadConversationFailed: '无法加载该对话',
      tryLater: '请稍后重试',
      stopped: '已停止当前等待',
      clearSuccess: '当前视图已清空',
      copied: '已复制',
      noExport: '暂无内容',
      unsafeLink: '外部链接协议不安全，已拦截',
      invalidLink: '外部链接格式无效',
      userLabel: '我',
      noAiContent: 'AI 服务暂未返回内容。',
      opened: '已前往',
      noArticleSummary: '暂无文章摘要',
      editMessage: '编辑消息',
      goodAnswer: '有帮助',
      badAnswer: '没帮助',
      share: '分享',
      feedbackSaved: '反馈已记录',
      shareCopied: '内容已复制，可直接分享',
      thinkingDone: '已思考',
      thinkingRunning: '正在思考',
      thinkingSeconds: '用时 {seconds} 秒',
      runSettings: '本轮设置',
      effectiveRun: '实际执行',
      temperatureShort: '温度',
      temperatureStable: '稳定',
      temperatureBalanced: '平衡',
      temperatureCreative: '发散',
      materialShort: '素材',
      webShort: '联网',
      webEnabled: '开',
      webDisabled: '关',
      articlesShort: '文章',
      sitesShort: '网站',
      outputShort: '输出',
      modelShort: '模型',
      directEffect: '少上下文，短回答',
      quickEffect: '低检索，快速定位',
      standardEffect: '常规站内知识',
      deepEffect: '更多文章与长输出',
      maxEffect: '最多 15 个联网结果',
    }
  : {
      brandSubtitle: 'Model Q&A / Page navigation',
      reasoningLabel: 'Reasoning',
      contextLabel: 'Context',
      newChat: 'New chat',
      startChat: 'Start chat',
      conversations: 'Conversations',
      loading: 'Loading...',
      noConversations: 'No conversations yet',
      chars: 'chars',
      workspace: 'AI Workspace',
      newQuestion: 'New site Q&A',
      reasoningStrength: 'Reasoning level',
      webOnTitle: 'Web search enabled',
      webOffTitle: 'Web search disabled; site knowledge only',
      webOnShort: 'Web on',
      webOffShort: 'Web off',
      exportChat: 'Export chat',
      clearView: 'Clear current view',
      heroTitle: 'Ask about models, data, or page entries. AI returns actionable navigation.',
      heroDesc: 'The assistant answers around 9 recommender-system models, data center, access tests, and performance dashboards. Missing logs or evaluation records are clearly marked.',
      collapseReasoning: 'Hide reasoning',
      viewReasoning: 'View reasoning',
      diagnosticsTitle: 'Effective reasoning diagnostics',
      collapse: 'Collapse',
      expand: 'Expand',
      suggestedEntry: 'Suggested entry',
      pageNavigation: 'Page navigation',
      go: 'Go',
      relatedArticles: 'Related articles',
      webReferences: 'Web references',
      searchQuery: 'Query',
      noWebResults: 'Web search was attempted, but no displayable result was returned.',
      copy: 'Copy',
      retry: 'Retry',
      analyzing: 'analyzing',
      aiAnalyzing: 'AI is analyzing...',
      inputPlaceholder: 'Type a question. Enter to send, Shift+Enter for a new line',
      welcome: 'Welcome to **DeepInsight AI Workspace**. Ask about model overview, access tests, performance dashboards, data center, or page navigation.',
      newChatStarted: 'A new site Q&A has started. Tell me which model, data, evaluation page, or management entry you want.',
      noConversationMessages: 'This conversation has no messages yet. You can continue below.',
      loadConversationFailed: 'Unable to load this conversation',
      tryLater: 'Please try again later',
      stopped: 'Stopped waiting for the current response',
      clearSuccess: 'Current view cleared',
      copied: 'Copied',
      noExport: 'Nothing to export',
      unsafeLink: 'Unsafe external link protocol blocked',
      invalidLink: 'Invalid external link',
      userLabel: 'Me',
      noAiContent: 'AI did not return content.',
      opened: 'Opened',
      noArticleSummary: 'No article summary available',
      editMessage: 'Edit message',
      goodAnswer: 'Helpful',
      badAnswer: 'Not helpful',
      share: 'Share',
      feedbackSaved: 'Feedback saved',
      shareCopied: 'Content copied and ready to share',
      thinkingDone: 'Thought',
      thinkingRunning: 'Thinking',
      thinkingSeconds: '{seconds}s',
      runSettings: 'This turn',
      effectiveRun: 'Applied',
      temperatureShort: 'Temp',
      temperatureStable: 'stable',
      temperatureBalanced: 'balanced',
      temperatureCreative: 'creative',
      materialShort: 'Materials',
      webShort: 'Web',
      webEnabled: 'on',
      webDisabled: 'off',
      articlesShort: 'Articles',
      sitesShort: 'Sites',
      outputShort: 'Output',
      modelShort: 'Model',
      directEffect: 'minimal context, short answer',
      quickEffect: 'low retrieval, fast routing',
      standardEffect: 'standard site knowledge',
      deepEffect: 'more articles and longer output',
      maxEffect: 'up to 15 web results',
    })

const materialText = computed(() => isZh.value
  ? {
      title: '资源库',
      hint: '搜索站内文章、模型、数据和个人素材，选择后作为 AI 上下文。',
      searchPlaceholder: '搜索文章、模型、数据集、训练记录...',
      scopeSummary: '当前结果',
      selected: '已选',
      empty: '还没有可用资源。可先上传个人文件、保存分析结果，或刷新站内资源。',
      loading: '读取资源中',
      refresh: '刷新资源',
      open: '打开资源库',
      close: '收起资源库',
      clear: '清空',
      attached: '已作为 AI 上下文',
      failed: '资源库读取失败',
      partialFailed: '部分资源来源读取失败，已显示可用内容',
      all: '全部',
      noScopeResults: '这个分类下暂无内容。',
      noSearchResults: '没有匹配的站内资源或个人素材。',
      selectedPrefix: '已选',
      analyzeSelected: '分析所选',
      analyzePromptPrefix: '分析我选择的资源',
      myStorage: '我的素材',
      myStorageHint: '上传文件和手动保存内容，按当前账号隔离。',
      myStorageEmpty: '还没有上传或保存个人素材。',
      myData: '我的数据',
      myDataHint: '数据集、训练任务、运行记录和保存分析，来自你的业务数据。',
      myDataEmpty: '还没有可引用的数据集或训练记录。',
      official: '官方资源',
      officialHint: '站内固定只读资源，不写入个人素材库。',
      officialEmpty: '暂无官方只读资源。',
      storedBadge: '真实存储',
      ownedBadge: '用户数据',
      officialBadge: '站内只读',
      file: '文件',
      image: '图片',
      text: '文本',
      analysis: '分析',
      data: '数据',
      model: '模型',
      training: '训练',
      run: '运行',
      article: '文章',
      knowledge: '知识',
      material: '素材',
    }
  : {
      title: 'Resource library',
      hint: 'Search site articles, models, data, and personal materials as AI context.',
      searchPlaceholder: 'Search articles, models, datasets, training records...',
      scopeSummary: 'Current results',
      selected: 'selected',
      empty: 'No usable resources yet. Upload personal files, save analysis results, or refresh site resources.',
      loading: 'Loading resources',
      refresh: 'Refresh resources',
      open: 'Open resource library',
      close: 'Collapse resource library',
      clear: 'Clear',
      attached: 'Attached as AI context',
      failed: 'Failed to load resources',
      partialFailed: 'Some resource sources failed; available content is shown',
      all: 'All',
      noScopeResults: 'No content in this category.',
      noSearchResults: 'No matching site resource or personal material.',
      selectedPrefix: 'Selected',
      analyzeSelected: 'Analyze selected',
      analyzePromptPrefix: 'Analyze my selected resources',
      myStorage: 'My materials',
      myStorageHint: 'Uploaded files and saved notes, isolated by current account.',
      myStorageEmpty: 'No uploaded or saved personal materials yet.',
      myData: 'My data',
      myDataHint: 'Datasets, training jobs, runs, and saved analysis from your records.',
      myDataEmpty: 'No datasets or training records available yet.',
      official: 'Official resources',
      officialHint: 'Built-in read-only resources, not written into your material library.',
      officialEmpty: 'No official resources available.',
      storedBadge: 'Stored',
      ownedBadge: 'User data',
      officialBadge: 'Read-only',
      file: 'File',
      image: 'Image',
      text: 'Text',
      analysis: 'Analysis',
      data: 'Data',
      model: 'Model',
      training: 'Training',
      run: 'Run',
      article: 'Article',
      knowledge: 'Knowledge',
      material: 'Material',
    })

const reasoningLevels = computed<Array<{ value: ReasoningLevel; label: string; desc: string }>>(() => isZh.value
  ? [
      { value: 'off', label: '直接', desc: '用于页面入口、按钮含义或字段解释。' },
      { value: 'quick', label: '快速', desc: '用于快速定位模型、数据或图表页面。' },
      { value: 'low', label: '标准', desc: '用于常规模型状态、页面入口和数据概览。' },
      { value: 'deep', label: '深度', desc: '用于模型接入差异、图表结果和数据质量分析。' },
      { value: 'max', label: '极限', desc: '用于跨页面、多模型、多数据源的完整复核。' },
    ]
  : [
      { value: 'off', label: 'Direct', desc: 'For page entries, buttons, and field explanations.' },
      { value: 'quick', label: 'Quick', desc: 'For fast model, data, or chart navigation.' },
      { value: 'low', label: 'Standard', desc: 'For model status, metric definitions, and data notes.' },
      { value: 'deep', label: 'Deep', desc: 'For access differences, evaluation results, and data quality.' },
      { value: 'max', label: 'Max', desc: 'For cross-page, multi-model, multi-source review.' },
    ])

const suggestedPrompts = computed(() => isZh.value
  ? [
      { icon: BrainCircuit, label: '模型总览', desc: '查看 9 个推荐系统模型和状态', query: '打开模型总览页面' },
      { icon: History, label: '性能看板', desc: '对比 HR、NDCG、MRR 和数据规模', query: '打开性能看板' },
      { icon: ClipboardList, label: '接入测试', desc: '验证推荐输入、Top-K 输出和服务状态', query: '打开模型接入测试页面' },
      { icon: Sparkles, label: '数据中心', desc: '管理上传数据集和素材记录', query: '打开数据中心' },
    ]
  : [
      { icon: BrainCircuit, label: 'Model overview', desc: 'Review 9 recommender models and metric sources', query: 'Open model overview' },
      { icon: History, label: 'Performance board', desc: 'Compare HR, NDCG, MRR, and dataset scale', query: 'Open performance dashboard' },
      { icon: ClipboardList, label: 'Access test', desc: 'Verify recommendation input, Top-K output, and service status', query: 'Open model access test page' },
      { icon: Sparkles, label: 'Data center', desc: 'Manage uploaded datasets and material records', query: 'Open data center' },
    ])

const activeConv = computed(() => conversations.value.find((item) => item.id === activeConvId.value))
const activeReasoning = computed(() => reasoningLevels.value.find((item) => item.value === reasoningLevel.value) || reasoningLevels.value[0]!)
const deepThink = computed(() => reasoningLevel.value !== 'off')
const controlEffectText = computed(() => `${reasoningEffectLabel(reasoningLevel.value)} · ${aiText.value.temperatureShort} ${temperature.value.toFixed(2)} ${temperatureEffectLabel(temperature.value)}`)
const selectedMaterials = computed(() => {
  const selected = new Set(selectedMaterialKeys.value)
  return workspaceMaterials.value.filter((item) => selected.has(item.key))
})
const materialTabs = computed(() => {
  const count = (scope: MaterialScopeFilter) => scope === 'all'
    ? searchedMaterials.value.length
    : searchedMaterials.value.filter((item) => item.sourceScope === scope).length
  return [
    { key: 'all' as const, label: materialText.value.all, count: count('all') },
    { key: 'stored' as const, label: materialText.value.myStorage, count: count('stored') },
    { key: 'owned' as const, label: materialText.value.myData, count: count('owned') },
    { key: 'official' as const, label: materialText.value.official, count: count('official') },
  ]
})
const searchedMaterials = computed(() => {
  const query = materialSearchQuery.value.trim().toLowerCase()
  if (!query) return workspaceMaterials.value
  return workspaceMaterials.value.filter((item) => materialMatchesSearch(item, query))
})
const visibleMaterials = computed(() => activeMaterialScope.value === 'all'
  ? searchedMaterials.value
  : searchedMaterials.value.filter((item) => item.sourceScope === activeMaterialScope.value))
const selectedMaterialSummary = computed(() => selectedMaterials.value.slice(0, 3).map((item) => item.title).join(' / '))
const canSendMessage = computed(() => Boolean(userInput.value.trim() || selectedMaterials.value.length))
const showTypingIndicator = computed(() => {
  if (!loading.value) return false
  const last = messages.value[messages.value.length - 1]
  return !last || last.role !== 'assistant'
})

function redirectToLoginAfterAiAuthError(error: any) {
  if (!clearAiAuthIfNeeded(error)) return false
  const message = formatAiErrorMessage(error, isZh.value)
  ElMessage.warning(message)
  void router.replace({ path: '/login', query: { redirect: route.fullPath } })
  return true
}

function toBoolean(value: any) {
  return value === true || value === 'true' || value === 1 || value === '1'
}

function normalizeWorkspaceMaterial(raw: any, origin: 'item' | 'resource'): WorkspaceMaterial | null {
  if (!raw || typeof raw !== 'object') return null
  const id = Number(raw.id)
  if (!Number.isFinite(id) || id <= 0) return null
  const rawType = String(raw.resourceType || raw.resource_type || raw.type || raw.itemType || raw.item_type || raw.sourceType || raw.source_type || 'file')
  const official = toBoolean(raw.official) || (origin === 'resource' && toBoolean(raw.readOnly) && String(raw.status || '').toLowerCase() === 'official')
  const readOnly = official || toBoolean(raw.readOnly)
  const sourceScope: MaterialScope = official ? 'official' : origin === 'item' ? 'stored' : 'owned'
  const storageMode: MaterialStorageMode = official ? 'virtual' : origin === 'item' ? 'persisted' : 'database'
  return {
    ...raw,
    key: origin === 'item' ? `item:${id}` : `resource:${rawType}:${id}`,
    id,
    title: String(raw.title || raw.fileName || raw.name || `${materialText.value.material} #${id}`),
    itemType: rawType,
    sourceType: String(raw.sourceType || raw.source_type || raw.itemType || raw.item_type || rawType),
    format: raw.format ? String(raw.format) : '',
    summary: raw.summary ? String(raw.summary) : '',
    meta: raw.meta ? String(raw.meta) : '',
    fileName: raw.fileName || raw.file_name,
    updatedAt: raw.updatedAt || raw.updated_at,
    sourceScope,
    storageMode,
    origin,
    resourceType: rawType,
    official,
    readOnly,
    canManage: raw.canManage ?? raw.can_manage ?? !readOnly,
    canSync: raw.canSync ?? raw.can_sync ?? false,
  }
}

async function loadWorkspaceMaterials() {
  materialLoading.value = true
  try {
    const [itemsResponse, resourcesResponse] = await Promise.allSettled([
      aiApi.listWorkspaceItems(80),
      aiApi.listWorkspaceResources(),
    ])
    if (itemsResponse.status === 'rejected' && resourcesResponse.status === 'rejected') {
      throw itemsResponse.reason || resourcesResponse.reason
    }
    const storedItems = itemsResponse.status === 'fulfilled' ? (itemsResponse.value.data.data || []) : []
    const resourceItems = resourcesResponse.status === 'fulfilled' ? (resourcesResponse.value.data.data || []) : []
    workspaceMaterials.value = [
      ...storedItems.map((item: any) => normalizeWorkspaceMaterial(item, 'item')),
      ...resourceItems.map((item: any) => normalizeWorkspaceMaterial(item, 'resource')),
    ]
      .filter((item): item is WorkspaceMaterial => Boolean(item))
    const availableKeys = new Set(workspaceMaterials.value.map((item) => item.key))
    selectedMaterialKeys.value = selectedMaterialKeys.value.filter((key) => availableKeys.has(key))
    if (itemsResponse.status === 'rejected' || resourcesResponse.status === 'rejected') {
      ElMessage.warning(materialText.value.partialFailed)
    }
  } catch (error: any) {
    workspaceMaterials.value = []
    selectedMaterialKeys.value = []
    if (!redirectToLoginAfterAiAuthError(error)) {
      ElMessage.error(error?.response?.data?.message || materialText.value.failed)
    }
  } finally {
    materialLoading.value = false
  }
}

async function toggleMaterialPanel() {
  materialPanelOpen.value = !materialPanelOpen.value
  if (materialPanelOpen.value) reasoningMenuOpen.value = false
  if (materialPanelOpen.value && !workspaceMaterials.value.length && !materialLoading.value) {
    await loadWorkspaceMaterials()
  }
}

function toggleReasoningMenu() {
  reasoningMenuOpen.value = !reasoningMenuOpen.value
  if (reasoningMenuOpen.value) materialPanelOpen.value = false
}

function setReasoningLevel(level: ReasoningLevel) {
  reasoningLevel.value = level
  reasoningMenuOpen.value = false
}

async function startConversationFromComposer() {
  if (loading.value) return
  if (canSendMessage.value) {
    await sendMessage()
    return
  }
  await nextTick()
  inputRef.value?.focus()
}

function toggleMaterial(item: WorkspaceMaterial) {
  selectedMaterialKeys.value = selectedMaterialKeys.value.includes(item.key)
    ? selectedMaterialKeys.value.filter((key) => key !== item.key)
    : [...selectedMaterialKeys.value, item.key].slice(-12)
}

function materialMatchesSearch(item: WorkspaceMaterial, query: string) {
  const haystack = [
    item.title,
    item.summary,
    item.meta,
    item.fileName,
    item.format,
    item.resourceType,
    item.itemType,
    item.sourceType,
    item.category,
    item.tags,
    item.nodeId,
    materialTypeLabel(item),
    materialScopeLabel(item),
  ]
    .map((value) => String(value || '').toLowerCase())
    .join(' ')
  return haystack.includes(query)
}

function materialTypeLabel(item: WorkspaceMaterial) {
  const type = String(item.itemType || item.sourceType || '').toLowerCase()
  const resourceType = String(item.resourceType || '').toLowerCase()
  if (type.includes('article') || resourceType.includes('article')) return materialText.value.article
  if (type.includes('knowledge') || resourceType.includes('knowledge')) return materialText.value.knowledge
  if (type.includes('image')) return materialText.value.image
  if (type.includes('analysis') || type.includes('view')) return materialText.value.analysis
  if (type.includes('dataset') || type.includes('data')) return materialText.value.data
  if (type.includes('training')) return materialText.value.training
  if (type === 'run' || type.includes('experiment')) return materialText.value.run
  if (type.includes('model')) return materialText.value.model
  if (type.includes('text') || ['md', 'txt', 'json', 'csv'].includes(String(item.format || '').toLowerCase())) return materialText.value.text
  if (type.includes('file') || type.includes('upload')) return materialText.value.file
  return materialText.value.material
}

function materialDescriptor(item: WorkspaceMaterial) {
  const type = materialTypeLabel(item)
  const meta = item.summary || item.meta || item.fileName || item.format || ''
  return meta ? `${type} · ${meta}` : type
}

function materialScopeLabel(item: WorkspaceMaterial) {
  if (item.sourceScope === 'official') return materialText.value.officialBadge
  if (item.sourceScope === 'owned') return materialText.value.ownedBadge
  return materialText.value.storedBadge
}

function isStoredMaterial(item: WorkspaceMaterial) {
  return item.origin === 'item' && item.sourceScope === 'stored' && item.storageMode === 'persisted'
}

function materialAttachment(item: WorkspaceMaterial) {
  return {
    id: item.id,
    type: item.itemType || item.sourceType || 'file',
    itemType: item.itemType || item.sourceType || 'file',
    title: item.title,
    fileName: item.fileName,
    format: item.format,
    summary: item.summary || item.meta,
  }
}

function materialResource(item: WorkspaceMaterial) {
  return {
    id: item.id,
    type: item.resourceType || item.itemType || item.sourceType || 'resource',
    title: item.title,
    summary: item.summary || item.meta,
    official: item.official,
    readOnly: item.readOnly,
    sourceScope: item.sourceScope,
    storageMode: item.storageMode,
  }
}

function selectedMaterialPrompt() {
  const names = selectedMaterials.value
    .slice(0, 8)
    .map((item) => `${item.title}（${materialScopeLabel(item)} / ${materialTypeLabel(item)}）`)
    .join('、')
  return isZh.value
    ? `${materialText.value.analyzePromptPrefix}：${names}。请结合这些资源说明关键内容、异常点、可以采取的下一步，并明确哪些结论来自用户数据库数据，哪些只是站内只读资源。`
    : `${materialText.value.analyzePromptPrefix}: ${names}. Summarize key points, anomalies, next actions, and clearly separate user database data from read-only site resources.`
}

async function analyzeSelectedMaterials() {
  if (loading.value || !selectedMaterials.value.length) return
  userInput.value = selectedMaterialPrompt()
  materialPanelOpen.value = false
  reasoningMenuOpen.value = false
  await nextTick()
  await sendMessage()
}

onMounted(async () => {
  setTimeout(() => { entered.value = true }, 80)
  await loadConversations()
  const routeConversationId = Number(route.query.conversationId)
  if (Number.isFinite(routeConversationId) && routeConversationId > 0) {
    const existing = conversations.value.find((conv) => conv.id === routeConversationId)
    await selectConversation(existing || { id: routeConversationId, title: `Chat #${routeConversationId}`, date: '' })
    return
  }
  const routePrompt = typeof route.query.prompt === 'string' ? route.query.prompt : ''
  if (routePrompt) {
    userInput.value = routePrompt
    nextTick(() => inputRef.value?.focus())
  }
  if (!messages.value.length) {
    messages.value.push({
      role: 'assistant',
      content: aiText.value.welcome,
      time: getTime(),
    })
  }
})

onUnmounted(() => {
  discardTypedTextQueues()
  streamController.value?.abort()
})

function getTime() {
  const d = new Date()
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

function formatConversationDate(value: string) {
  if (!value) return aiText.value.conversations
  const raw = String(value).trim()
  const numeric = Number(raw)
  const date = /^\d+$/.test(raw) && Number.isFinite(numeric) ? new Date(numeric) : new Date(raw)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleDateString(isZh.value ? 'zh-CN' : 'en-US')
}

function isNearMessageBottom(element = msgContainer.value) {
  if (!element) return true
  return element.scrollHeight - element.scrollTop - element.clientHeight <= SCROLL_FOLLOW_THRESHOLD
}

function handleMessageScroll() {
  autoFollowMessages.value = isNearMessageBottom()
}

async function scrollToBottom(force = false) {
  await nextTick()
  if (!msgContainer.value) return
  if (!force && !autoFollowMessages.value) return
  msgContainer.value.scrollTop = msgContainer.value.scrollHeight
  autoFollowMessages.value = true
}

function autoResize() {
  if (!inputRef.value) return
  inputRef.value.style.height = 'auto'
  inputRef.value.style.height = `${Math.min(inputRef.value.scrollHeight, 150)}px`
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
  const native = item.model?.nativeReasoning
    ? (isZh.value ? '模型推理已启用' : 'model reasoning enabled')
    : (isZh.value ? '上下文增强' : 'context enhanced')
  const related = item.related ? `${isZh.value ? '文章' : 'articles'} ${item.related.matched || 0}/${item.related.limit || 0}` : ''
  const web = item.web?.attempted
    ? `${isZh.value ? '联网' : 'web'} ${item.web.returned || 0}/${item.web.limit || 0}`
    : (isZh.value ? '未联网' : 'web off')
  return [item.label || item.level || activeReasoning.value.label, native, related, web].filter(Boolean).join(' · ')
}

function diagnosticsItems(item: ChatReasoningDiagnostics) {
  const model = item.model || {}
  const related = item.related || {}
  const web = item.web || {}
  const searchQuery = typeof web.query === 'string' && web.query.trim() ? web.query.trim() : '-'
  const controls = Array.isArray(model.appliedControls) ? model.appliedControls.join('；') : ''
  return [
    { label: isZh.value ? '实际检索词' : 'Query', value: searchQuery },
    { label: isZh.value ? '深度档位' : 'Reasoning', value: `${item.label || item.level || activeReasoning.value.label}${item.enabled ? (isZh.value ? '（已启用）' : ' (enabled)') : (isZh.value ? '（直答）' : ' (direct)')}` },
    { label: isZh.value ? '模型侧' : 'Model side', value: `${model.provider || '-'} / ${model.effectiveModel || model.model || '-'}` },
    { label: isZh.value ? '原生推理' : 'Native reasoning', value: model.nativeReasoning ? (model.nativeReasoningLabel || (isZh.value ? '已启用' : 'enabled')) : (model.nativeReasoningLabel || (isZh.value ? '未启用' : 'disabled')) },
    { label: isZh.value ? '站内文章' : 'Site articles', value: `${related.matched || 0} / ${related.limit || 0}` },
    { label: isZh.value ? '联网搜索' : 'Web search', value: web.attempted ? `${web.returned || 0} / ${web.limit || 0}` : (web.requested ? (isZh.value ? '已请求但未触发' : 'requested but not triggered') : (isZh.value ? '未请求' : 'not requested')) },
    { label: isZh.value ? '输出上限' : 'Max output', value: model.maxTokens ? `${model.maxTokens} tokens` : '-' },
    { label: isZh.value ? '实际参数' : 'Applied controls', value: controls || (isZh.value ? '无额外模型参数' : 'no extra model controls') },
  ]
}

function temperatureEffectLabel(value: number) {
  if (value <= 0.3) return aiText.value.temperatureStable
  if (value >= 0.75) return aiText.value.temperatureCreative
  return aiText.value.temperatureBalanced
}

function reasoningEffectLabel(level: ReasoningLevel) {
  return ({
    off: aiText.value.directEffect,
    quick: aiText.value.quickEffect,
    low: aiText.value.standardEffect,
    deep: aiText.value.deepEffect,
    max: aiText.value.maxEffect,
  })[level]
}

function makeControlSnapshot(title: string, materialCount: number): ControlSnapshot {
  const active = reasoningLevels.value.find((item) => item.value === reasoningLevel.value) || activeReasoning.value
  return {
    title,
    level: reasoningLevel.value,
    levelLabel: active.label,
    levelDesc: active.desc,
    temperature: Number(temperature.value.toFixed(2)),
    temperatureLabel: temperatureEffectLabel(temperature.value),
    webSearch: webSearchEnabled.value,
    materialCount,
  }
}

function controlSnapshotChips(snapshot: ControlSnapshot) {
  const chips = [snapshot.levelLabel]
  if (snapshot.webSearch) chips.push(aiText.value.webOnShort)
  if (snapshot.materialCount > 0) chips.push(`${aiText.value.materialShort} ${snapshot.materialCount}`)
  if (typeof snapshot.relatedLimit === 'number') {
    chips.push(`${aiText.value.articlesShort} ${snapshot.relatedMatched || 0}/${snapshot.relatedLimit}`)
  }
  if (typeof snapshot.webLimit === 'number') {
    chips.push(`${aiText.value.sitesShort} ${snapshot.webReturned || 0}/${snapshot.webLimit}`)
  }
  if (snapshot.maxTokens) chips.push(`${aiText.value.outputShort} ${snapshot.maxTokens}`)
  if (snapshot.nativeReasoning) chips.push(snapshot.nativeReasoning)
  if (snapshot.modelLabel) chips.push(`${aiText.value.modelShort} ${snapshot.modelLabel}`)
  return chips
}

function applyDiagnosticsToControlSnapshot(target: Message) {
  const diagnostics = target.reasoningDiagnostics
  if (!diagnostics) return
  const base = target.controlSnapshot || makeControlSnapshot(aiText.value.effectiveRun, 0)
  target.controlSnapshot = {
    ...base,
    title: aiText.value.effectiveRun,
    levelLabel: diagnostics.label || base.levelLabel,
    levelDesc: diagnostics.strategy || base.levelDesc,
    temperature: typeof diagnostics.temperature === 'number' ? diagnostics.temperature : base.temperature,
    temperatureLabel: temperatureEffectLabel(typeof diagnostics.temperature === 'number' ? diagnostics.temperature : base.temperature),
    relatedMatched: diagnostics.related?.matched || 0,
    relatedLimit: diagnostics.related?.limit || 0,
    webSearch: Boolean(diagnostics.web?.requested || diagnostics.web?.attempted),
    webReturned: diagnostics.web?.returned || 0,
    webLimit: diagnostics.web?.limit || 0,
    materialCount: diagnostics.attachments?.count ?? base.materialCount,
    modelLabel: diagnostics.model?.effectiveModel || diagnostics.model?.model,
    nativeReasoning: diagnostics.model?.nativeReasoningLabel,
    maxTokens: diagnostics.model?.maxTokens,
  }
}

function hasThinkingText(msg: Message) {
  return Boolean(msg.thinking?.trim())
}

function isLiveAssistantMessage(msg: Message, index: number) {
  return loading.value && index === messages.value.length - 1 && msg.role === 'assistant'
}

function shouldShowThinking(msg: Message, index: number) {
  return hasThinkingText(msg) || isLiveAssistantMessage(msg, index)
}

function thinkingLabel(msg: Message, index: number) {
  if (loading.value && index === messages.value.length - 1 && msg.role === 'assistant') {
    return aiText.value.thinkingRunning
  }
  if (!msg.thinkingDuration) return aiText.value.thinkingDone
  return `${aiText.value.thinkingDone}（${aiText.value.thinkingSeconds.replace('{seconds}', String(msg.thinkingDuration))}）`
}

function normalizeWebResults(value: unknown): ChatWebResult[] {
  return Array.isArray(value) ? value.slice(0, MAX_WEB_RESULTS) : []
}

function applyStreamStatus(target: Message, data: any) {
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

function enqueueTypedText(target: Message, field: 'content' | 'thinking', delta: string) {
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
      tokenCount.value += char.length
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

function flushTypedText(field: 'content' | 'thinking', target?: Message | null) {
  if (field === 'content') {
    if (contentTypingTimer) window.clearTimeout(contentTypingTimer)
    contentTypingTimer = null
    const activeTarget = target || contentTypingTarget
    const rest = contentTypingQueue.join('')
    contentTypingQueue = []
    if (activeTarget && rest) {
      activeTarget.content += rest
      tokenCount.value += rest.length
    }
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

function clearTypedTextQueues() {
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

async function sendMessage() {
  const typedMessage = userInput.value.trim()
  if (loading.value || (!typedMessage && !selectedMaterials.value.length)) return
  const msg = typedMessage || selectedMaterialPrompt()
  userInput.value = ''
  if (inputRef.value) inputRef.value.style.height = 'auto'
  materialPanelOpen.value = false
  reasoningMenuOpen.value = false

  const activeAttachments = selectedMaterials.value.filter(isStoredMaterial).map(materialAttachment)
  const activeResources = selectedMaterials.value.filter((item) => !isStoredMaterial(item)).map(materialResource)
  const requestedSnapshot = makeControlSnapshot(aiText.value.runSettings, activeAttachments.length + activeResources.length)
  messages.value.push({ role: 'user', content: msg, time: getTime(), controlSnapshot: requestedSnapshot })
  chatHistory.value.push({ role: 'user', content: msg })
  tokenCount.value += msg.length

  const startedAt = Date.now()
  const assistantMessage: Message = {
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
    controlSnapshot: { ...requestedSnapshot, title: aiText.value.effectiveRun },
  }
  messages.value.push(assistantMessage)

  loading.value = true
  const controller = new AbortController()
  streamController.value = controller
  await scrollToBottom(true)
  let streamFailed = false
  try {
    await aiApi.chatStream({
      message: msg,
      history: chatHistory.value.slice(-20),
      temperature: temperature.value,
      deepThink: deepThink.value,
      reasoningLevel: reasoningLevel.value,
      webSearch: webSearchEnabled.value,
      ...(activeAttachments.length ? { attachments: activeAttachments } : {}),
      ...(activeResources.length ? { resources: activeResources } : {}),
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
        applyStreamMetadata(assistantMessage, data, msg)
      },
      onError(data) {
        streamFailed = true
        discardTypedTextQueues()
        assistantMessage.content = data.message || aiText.value.tryLater
      },
    }, controller.signal)

    clearTypedTextQueues()
    if (!assistantMessage.content.trim()) assistantMessage.content = aiText.value.noAiContent
    assistantMessage.content = normalizeAssistantReply(assistantMessage.content, {
      userMessage: msg,
      currentPageTitle: aiText.value.workspace,
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

function applyStreamMetadata(target: Message, data: any, userMessage: string) {
  data = data && typeof data === 'object' ? data : {}
  if (data.conversationId) {
    conversationId.value = data.conversationId
    if (!activeConvId.value) {
      const newConv = {
        id: data.conversationId,
        title: userMessage.slice(0, 28) + (userMessage.length > 28 ? '...' : ''),
        date: new Date().toISOString(),
      }
      conversations.value.unshift(newConv)
      activeConvId.value = newConv.id
    }
  }
  if (typeof data.reply === 'string' && data.reply && !target.content.trim()) {
    target.content = data.reply
  }
  const responseNavigation = normalizeAssistantNavigation(data.navigation)
  if ('navigation' in data) {
    target.navigation = responseNavigation || undefined
    target.navigationExpanded = true
  }
  if ('relatedArticles' in data) {
    target.relatedArticles = Array.isArray(data.relatedArticles) ? data.relatedArticles : []
    target.relatedExpanded = false
  }
  if ('webResults' in data) {
    target.webResults = normalizeWebResults(data.webResults)
    target.webExpanded = target.webResults.length > 0 || target.webExpanded
  }
  if ('webSearchAttempted' in data) target.webSearchAttempted = Boolean(data.webSearchAttempted)
  if ('webSearchUsed' in data) target.webSearchUsed = Boolean(data.webSearchUsed)
  if (typeof data.webSearchQuery === 'string' && data.webSearchQuery.trim()) {
    target.webSearchQuery = data.webSearchQuery.trim()
  }
  if ('reasoningDiagnostics' in data) {
    target.reasoningDiagnostics = normalizeReasoningDiagnostics(data.reasoningDiagnostics)
    target.diagnosticsExpanded = false
    applyDiagnosticsToControlSnapshot(target)
  }
  if ('apiStatus' in data) target.apiStatus = data.apiStatus
  if (data.reasoning && !target.thinking?.includes(String(data.reasoning))) {
    target.thinking = `${target.thinking || ''}${target.thinking ? '\n' : ''}${data.reasoning}`
  }
}

function askAI(query: string) {
  userInput.value = query
  void sendMessage()
}

function stopGeneration() {
  streamController.value?.abort()
  streamController.value = null
  discardTypedTextQueues()
  loading.value = false
  ElMessage.info(aiText.value.stopped)
}

function newConversation() {
  discardTypedTextQueues()
  reasoningMenuOpen.value = false
  messages.value = [{
    role: 'assistant',
    content: aiText.value.newChatStarted,
    time: getTime(),
  }]
  chatHistory.value = []
  conversationId.value = null
  activeConvId.value = null
  tokenCount.value = 0
  userInput.value = ''
  nextTick(() => inputRef.value?.focus())
}

async function loadConversations() {
  try {
    const response = await aiApi.listConversations()
    if (response.data.code === 200 && response.data.data) {
      conversations.value = response.data.data.map((item: any) => ({
        id: Number(item.id),
        title: item.title || aiText.value.conversations,
        date: item.createdAt || '',
      }))
    }
  } catch (error: any) {
    conversations.value = []
    if (isAiAuthError(error)) {
      const errorMessage = formatAiErrorMessage(error, isZh.value)
      messages.value = [{
        role: 'assistant',
        content: errorMessage,
        time: getTime(),
      }]
      redirectToLoginAfterAiAuthError(error)
    }
  }
}

async function selectConversation(conv: Conversation) {
  activeConvId.value = conv.id
  conversationId.value = conv.id
  conversationLoading.value = true
  try {
    const response = await aiApi.getMessages(conv.id)
    const history = Array.isArray(response.data?.data) ? response.data.data : []
    if (response.data.code === 200) {
      messages.value = history.length ? history.map((item: any) => {
        const role = item.role === 'user' ? 'user' : 'assistant'
        const navigation = normalizeAssistantNavigation(item.navigation)
        const relatedArticles = Array.isArray(item.relatedArticles) ? item.relatedArticles : []
        const webResults = normalizeWebResults(item.webResults)
        const reasoningDiagnostics = normalizeReasoningDiagnostics(item.reasoningDiagnostics)
        return {
          role,
          content: String(item.content || ''),
          time: '',
          thinking: role === 'assistant' ? String(item.reasoning || '') : '',
          thinkingExpanded: false,
          navigation: navigation || undefined,
          navigationExpanded: true,
          relatedArticles,
          relatedExpanded: false,
          webResults,
          webExpanded: false,
          webSearchAttempted: Boolean(item.webSearchAttempted),
          webSearchUsed: Boolean(item.webSearchUsed),
          webSearchQuery: typeof item.webSearchQuery === 'string' ? item.webSearchQuery : undefined,
          reasoningDiagnostics,
          diagnosticsExpanded: false,
        }
      }) : [{
        role: 'assistant',
        content: aiText.value.noConversationMessages,
        time: getTime(),
      }]
      chatHistory.value = history.map((item: any) => ({
        role: item.role === 'user' ? 'user' : 'assistant',
        content: String(item.content || ''),
      }))
      tokenCount.value = chatHistory.value.reduce((sum: number, item: any) => sum + String(item.content || '').length, 0)
      await router.replace({ path: ROUTES.AI, query: { ...route.query, conversationId: String(conv.id) } })
      await scrollToBottom(true)
    }
  } catch (error: any) {
    const errorMessage = formatAiErrorMessage(error, isZh.value)
    messages.value = [{
      role: 'assistant',
      content: isAiAuthError(error)
        ? errorMessage
        : `${aiText.value.loadConversationFailed}：${error?.response?.data?.message || error.message || aiText.value.tryLater}`,
      time: getTime(),
    }]
    chatHistory.value = []
    tokenCount.value = 0
    if (!redirectToLoginAfterAiAuthError(error)) {
      ElMessage.error(error?.response?.data?.message || aiText.value.loadConversationFailed)
    }
  } finally {
    conversationLoading.value = false
  }
}

async function deleteConversation(id: number) {
  await aiApi.deleteConversation(id).catch(() => {})
  conversations.value = conversations.value.filter((item) => item.id !== id)
  if (activeConvId.value === id) newConversation()
}

function clearCurrentChat() {
  discardTypedTextQueues()
  messages.value = []
  chatHistory.value = []
  tokenCount.value = 0
  ElMessage.success(aiText.value.clearSuccess)
}

function copyMsg(text: string) {
  void navigator.clipboard.writeText(text)
  ElMessage.success(aiText.value.copied)
}

async function editMessage(text: string) {
  userInput.value = text
  await nextTick()
  autoResize()
  inputRef.value?.focus()
}

function rateMessage(msg: Message, feedback: 'up' | 'down') {
  msg.feedback = msg.feedback === feedback ? undefined : feedback
  ElMessage.success(aiText.value.feedbackSaved)
}

async function shareMessage(text: string) {
  const shareApi = navigator as Navigator & { share?: (data: ShareData) => Promise<void> }
  if (shareApi.share) {
    await shareApi.share({ text }).catch(() => undefined)
    return
  }
  await navigator.clipboard.writeText(text)
  ElMessage.success(aiText.value.shareCopied)
}

function regenerate() {
  const lastUser = [...chatHistory.value].reverse().find((item) => item.role === 'user')
  if (!lastUser) return
  messages.value = messages.value.slice(0, -1)
  chatHistory.value = chatHistory.value.slice(0, -1)
  userInput.value = lastUser.content
  void sendMessage()
}

function goToNavigation(navigation: AssistantNavigationIntent) {
  void router.push(navigation.path)
  ElMessage.success(`${aiText.value.opened}${isZh.value ? '' : ': '} ${navigation.label}`)
}

async function openRelatedArticle(article: ChatRelatedArticle) {
  readerOpen.value = true
  readerLoading.value = true
  readerSourceLabel.value = article.sourceLabel || aiText.value.relatedArticles
  readerArticle.value = {
    title: article.title,
    content: article.snippet || aiText.value.noArticleSummary,
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
      ElMessage.warning(aiText.value.unsafeLink)
      return
    }
    window.open(parsed.href, '_blank', 'noopener,noreferrer')
  } catch {
    ElMessage.warning(aiText.value.invalidLink)
  }
}

function exportChat() {
  if (!messages.value.length) {
    ElMessage.warning(aiText.value.noExport)
    return
  }
  const text = messages.value.map((item) => `[${item.time}] ${item.role === 'user' ? aiText.value.userLabel : 'AI'}: ${item.content}`).join('\n\n')
  const blob = new Blob([text], { type: 'text/plain;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `deepinsight-chat-${Date.now()}.txt`
  link.click()
  URL.revokeObjectURL(url)
}
</script>

<style scoped>
.qa-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 292px minmax(0, 1fr);
  color: #171d19;
  background:
    radial-gradient(circle at 18% 12%, rgba(20, 184, 166, 0.14), transparent 30%),
    radial-gradient(circle at 88% 4%, rgba(245, 158, 11, 0.14), transparent 28%),
    linear-gradient(135deg, #f8f4ea, #f3f7ef 52%, #eef3ff);
  opacity: 0;
  transform: translateY(10px);
  transition: opacity 220ms var(--ease-smooth), transform 240ms var(--ease-smooth);
}

.qa-page.entered {
  opacity: 1;
  transform: translateY(0);
}

.qa-sidebar {
  position: sticky;
  top: 0;
  height: 100vh;
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr) auto;
  gap: 14px;
  padding: 18px;
  border-right: 1px solid rgba(23, 29, 25, 0.1);
  background: rgba(19, 32, 24, 0.94);
  color: #fbf5e7;
}

.brand-block {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 10px 8px 18px;
}

.brand-block span,
.hero-mark,
.avatar {
  display: grid;
  place-items: center;
}

.brand-block span {
  width: 42px;
  height: 42px;
  border-radius: 16px;
  background: rgba(113, 255, 179, 0.13);
  color: #71ffb3;
}

.brand-block strong,
.brand-block em {
  display: block;
}

.brand-block strong {
  font-size: 16px;
}

.brand-block em {
  margin-top: 3px;
  color: rgba(251, 245, 231, 0.58);
  font-size: 12px;
  font-style: normal;
}

.new-chat {
  min-height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: 0;
  border-radius: 16px;
  background: #71ffb3;
  color: #102015;
  font-weight: 900;
  cursor: pointer;
}

.conversation-panel {
  min-height: 0;
  overflow-y: auto;
}

.panel-title {
  display: flex;
  justify-content: space-between;
  padding: 6px 4px 10px;
  color: rgba(251, 245, 231, 0.54);
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0;
}

.conversation-item {
  position: relative;
  width: 100%;
  display: grid;
  gap: 4px;
  margin-bottom: 7px;
  padding: 12px 36px 12px 12px;
  border: 1px solid rgba(251, 245, 231, 0.08);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.035);
  color: inherit;
  text-align: left;
  cursor: pointer;
}

.conversation-item:hover,
.conversation-item.active {
  border-color: rgba(113, 255, 179, 0.35);
  background: rgba(113, 255, 179, 0.1);
}

.conversation-item.loading {
  cursor: wait;
  opacity: 0.82;
}

.conversation-item strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}

.conversation-item em,
.conversation-empty {
  color: rgba(251, 245, 231, 0.52);
  font-size: 11px;
  font-style: normal;
}

.conversation-item i {
  position: absolute;
  top: 10px;
  right: 10px;
  opacity: 0.6;
}

.conversation-empty {
  padding: 18px 8px;
}

.sidebar-foot {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.sidebar-foot div {
  padding: 12px;
  border: 1px solid rgba(251, 245, 231, 0.08);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.035);
}

.sidebar-foot span,
.sidebar-foot strong {
  display: block;
}

.sidebar-foot span {
  color: rgba(251, 245, 231, 0.48);
  font-size: 10px;
}

.sidebar-foot strong {
  margin-top: 5px;
  font-size: 12px;
}

.qa-main {
  min-width: 0;
  height: 100vh;
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr) auto;
}

.qa-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 28px 12px;
}

.qa-topbar > div:first-child {
  min-width: 0;
}

.eyebrow {
  color: #0f766e;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0;
}

.qa-topbar h1 {
  margin: 5px 0 0;
  display: -webkit-box;
  overflow: hidden;
  font-size: clamp(22px, 2.2vw, 32px);
  line-height: 1.12;
  letter-spacing: 0;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.top-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.top-actions select,
.top-actions button,
.top-actions label {
  min-height: 38px;
  border: 1px solid rgba(23, 29, 25, 0.12);
  border-radius: 999px;
  background: rgba(255, 252, 244, 0.72);
  color: #171d19;
}

.top-actions select {
  padding: 0 12px;
  color-scheme: light dark;
}

.top-actions select option {
  background: #fffaf0;
  color: #171d19;
}

.top-actions select option:checked {
  background: rgba(15, 118, 110, 0.16);
  color: #0f1f18;
}

.top-actions button {
  width: 38px;
  display: grid;
  place-items: center;
  cursor: pointer;
}

.top-actions .web-search-toggle {
  width: auto;
  display: inline-flex;
  gap: 6px;
  padding: 0 12px;
  font-size: 12px;
  font-weight: 900;
  color: #0f766e;
}

.top-actions .web-search-toggle.active {
  border-color: rgba(15, 118, 110, 0.34);
  background: rgba(15, 118, 110, 0.13);
}

.top-actions label {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 12px;
  color: #0f766e;
  font-size: 12px;
  font-weight: 900;
}

.top-actions input {
  width: 72px;
  accent-color: #0f766e;
}

.qa-scroll {
  min-height: 0;
  overflow-y: auto;
  width: 100%;
  justify-self: stretch;
  box-sizing: border-box;
  padding: 14px clamp(28px, 3vw, 56px) 120px;
}

.qa-hero {
  display: grid;
  justify-items: center;
  text-align: center;
  padding: 58px 0 36px;
}

.hero-mark {
  width: 72px;
  height: 72px;
  border-radius: 24px;
  background: #132018;
  color: #71ffb3;
  box-shadow: 0 20px 60px rgba(19, 32, 24, 0.24);
}

.qa-hero h2 {
  max-width: 720px;
  margin: 22px 0 10px;
  font-size: clamp(32px, 5vw, 58px);
  line-height: 0.98;
  letter-spacing: 0;
}

.qa-hero p {
  max-width: 720px;
  margin: 0;
  color: rgba(23, 29, 25, 0.62);
  line-height: 1.8;
}

.prompt-grid {
  width: min(100%, 760px);
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 24px;
}

.prompt-grid button {
  min-height: 112px;
  padding: 16px;
  border: 1px solid rgba(23, 29, 25, 0.11);
  border-radius: 22px;
  background: rgba(255, 252, 244, 0.74);
  color: inherit;
  text-align: left;
  cursor: pointer;
}

.prompt-grid button:hover {
  border-color: rgba(15, 118, 110, 0.32);
  transform: translateY(-1px);
}

.prompt-grid span {
  display: inline-grid;
  width: 34px;
  height: 34px;
  place-items: center;
  margin-bottom: 12px;
  border-radius: 13px;
  background: rgba(15, 118, 110, 0.11);
  color: #0f766e;
}

.prompt-grid strong,
.prompt-grid em {
  display: block;
}

.prompt-grid strong {
  font-size: 15px;
}

.prompt-grid em {
  margin-top: 5px;
  color: rgba(23, 29, 25, 0.56);
  font-size: 12px;
  font-style: normal;
}

.message-row {
  display: grid;
  grid-template-columns: 38px minmax(0, 1fr);
  gap: 12px;
  margin: 18px 0;
}

.message-row.user {
  grid-template-columns: minmax(0, 1fr) 38px;
}

.message-row.user .avatar {
  grid-column: 2;
  grid-row: 1;
}

.message-row.user .message-body {
  grid-column: 1;
  justify-self: end;
}

.avatar {
  width: 38px;
  height: 38px;
  border-radius: 15px;
  background: #132018;
  color: #71ffb3;
}

.message-row.user .avatar {
  background: rgba(15, 118, 110, 0.12);
  color: #0f766e;
}

.message-body {
  max-width: min(760px, 100%);
}

.bubble {
  padding: 16px 18px;
  border: 1px solid rgba(23, 29, 25, 0.1);
  border-radius: 22px;
  background: rgba(255, 252, 244, 0.78);
  box-shadow: 0 16px 48px rgba(23, 29, 25, 0.08);
  line-height: 1.78;
}

.bubble :deep(:first-child) {
  margin-top: 0;
}

.bubble :deep(:last-child) {
  margin-bottom: 0;
}

.bubble :deep(p) {
  margin: 0 0 10px;
}

.bubble :deep(ol),
.bubble :deep(ul) {
  margin: 10px 0;
  padding-left: 22px;
}

.bubble :deep(li) {
  margin: 5px 0;
  padding-left: 2px;
}

.bubble :deep(strong) {
  color: inherit;
  font-weight: 900;
}

.bubble :deep(code) {
  padding: 2px 6px;
  border-radius: 7px;
  background: rgba(15, 118, 110, 0.1);
  color: #0f766e;
  font-family: "JetBrains Mono", Consolas, monospace;
  font-size: 0.92em;
}

.message-row.user .bubble {
  background: #132018;
  color: #fbf5e7;
}

.bubble :deep(pre) {
  overflow-x: auto;
  padding: 14px;
  border-radius: 14px;
  background: #101612;
  color: #d7ffe8;
}

.bubble :deep(pre code) {
  padding: 0;
  background: transparent;
  color: inherit;
}

.thinking-card,
.navigation-card,
.diagnostics-card {
  margin-top: 10px;
  border: 1px solid rgba(15, 118, 110, 0.18);
  border-radius: 18px;
  background: rgba(15, 118, 110, 0.08);
}

.thinking-card {
  width: 100%;
  padding: 12px 14px;
  color: #0f766e;
  text-align: left;
  cursor: pointer;
}

.thinking-card pre {
  margin: 10px 0 0;
  white-space: pre-wrap;
  color: rgba(23, 29, 25, 0.72);
}

.diagnostics-card {
  overflow: hidden;
  background:
    linear-gradient(135deg, rgba(15, 118, 110, 0.09), rgba(99, 102, 241, 0.08));
}

.diagnostics-head {
  width: 100%;
  min-height: 42px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 11px 14px;
  border: 0;
  background: transparent;
  color: inherit;
  cursor: pointer;
  font: inherit;
  text-align: left;
}

.diagnostics-head span {
  color: #0f766e;
  font-size: 12px;
  font-weight: 900;
}

.diagnostics-head b {
  overflow: hidden;
  color: rgba(23, 29, 25, 0.62);
  font-size: 11px;
  font-weight: 800;
  text-align: right;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.diagnostics-body {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  padding: 0 14px 14px;
}

.diagnostics-body div {
  min-width: 0;
  padding: 9px 10px;
  border: 1px solid rgba(15, 118, 110, 0.12);
  border-radius: 13px;
  background: rgba(255, 252, 244, 0.46);
}

.diagnostics-body span,
.diagnostics-body strong {
  display: block;
}

.diagnostics-body span {
  color: rgba(23, 29, 25, 0.5);
  font-size: 10px;
  font-weight: 900;
}

.diagnostics-body strong {
  overflow-wrap: anywhere;
  margin-top: 4px;
  color: #171d19;
  font-size: 12px;
  line-height: 1.45;
}

.diagnostics-body p {
  grid-column: 1 / -1;
  margin: 2px 0 0;
  color: rgba(23, 29, 25, 0.58);
  font-size: 12px;
  line-height: 1.6;
}

.navigation-card {
  padding: 14px;
}

.navigation-toggle,
.related-head {
  width: 100%;
  border: 0;
  background: transparent;
  color: inherit;
  cursor: pointer;
  font: inherit;
  text-align: left;
}

.navigation-toggle {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 0;
}

.navigation-toggle b,
.related-head b {
  flex: 0 0 auto;
  color: rgba(15, 118, 110, 0.68);
  font-size: 11px;
  font-weight: 900;
}

.navigation-card span,
.related-head span {
  color: #0f766e;
  font-size: 11px;
  font-weight: 900;
}

.navigation-card strong,
.navigation-card em {
  display: block;
}

.navigation-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid rgba(15, 118, 110, 0.12);
}

.navigation-card em {
  color: rgba(23, 29, 25, 0.58);
  font-style: normal;
}

.navigation-card small {
  display: block;
  margin-top: 4px;
  color: rgba(23, 29, 25, 0.5);
  font-size: 11px;
}

.navigation-goto {
  border: 0;
  border-radius: 999px;
  background: #132018;
  color: #fbf5e7;
  padding: 10px 16px;
  cursor: pointer;
}

.related-card {
  display: grid;
  gap: 10px;
  margin-top: 10px;
  padding: 14px;
  border: 1px solid rgba(15, 118, 110, 0.16);
  border-radius: 18px;
  background: rgba(15, 118, 110, 0.07);
}

.related-card.web {
  border-color: rgba(59, 130, 246, 0.16);
  background: rgba(59, 130, 246, 0.06);
}

.related-card.web .related-list {
  max-height: 420px;
  overflow-y: auto;
  padding-right: 2px;
}

.related-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 7px;
  padding: 0;
}

.related-head span {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-size: 12px;
}

.related-list {
  display: grid;
  gap: 10px;
}

.related-item {
  display: grid;
  gap: 5px;
  padding: 12px;
  border: 1px solid rgba(23, 29, 25, 0.08);
  border-radius: 14px;
  background: rgba(255, 252, 244, 0.64);
  color: inherit;
  text-align: left;
  cursor: pointer;
}

.related-item:hover {
  border-color: rgba(15, 118, 110, 0.28);
  transform: translateY(-1px);
}

.related-item strong {
  color: #171d19;
  font-size: 13px;
}

.web-query-line {
  color: var(--qa-chat-muted, rgba(23, 29, 25, 0.52));
  font-size: 12px;
  line-height: 1.5;
}

.web-result-title {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.web-result-title b {
  min-width: 34px;
  padding: 3px 7px;
  border-radius: 999px;
  background: rgba(var(--primary-rgb), 0.12);
  color: var(--qa-chat-accent, #1d4ed8);
  font-size: 11px;
  font-weight: 800;
  text-align: center;
}

.web-result-title strong {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.related-item em {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  color: rgba(23, 29, 25, 0.58);
  font-size: 12px;
  font-style: normal;
  line-height: 1.55;
}

.related-empty {
  margin: 0;
  color: rgba(23, 29, 25, 0.58);
  font-size: 12px;
  line-height: 1.55;
}

.message-meta {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-top: 6px;
  color: rgba(23, 29, 25, 0.46);
  font-size: 11px;
}

.message-meta button {
  border: 0;
  background: transparent;
  color: #0f766e;
  cursor: pointer;
}

.typing-card {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 13px 16px;
  border-radius: 999px;
  background: rgba(255, 252, 244, 0.78);
  color: rgba(23, 29, 25, 0.58);
}

.typing-card span {
  width: 7px;
  height: 7px;
  border-radius: 999px;
  background: #0f766e;
  animation: typing 1s infinite ease-in-out;
}

.typing-card span:nth-child(2) { animation-delay: 140ms; }
.typing-card span:nth-child(3) { animation-delay: 280ms; }
.typing-card em { margin-left: 4px; font-style: normal; font-size: 12px; }

.composer {
  position: sticky;
  bottom: 0;
  width: 100%;
  justify-self: stretch;
  box-sizing: border-box;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 52px;
  gap: 10px;
  padding: 14px clamp(28px, 3vw, 56px) 24px;
  background: linear-gradient(180deg, rgba(248, 244, 234, 0), rgba(248, 244, 234, 0.94) 34%, rgba(248, 244, 234, 0.98));
}

.composer textarea {
  min-height: 54px;
  max-height: 150px;
  resize: none;
  border: 1px solid rgba(23, 29, 25, 0.13);
  border-radius: 24px;
  outline: none;
  padding: 17px 18px;
  background: rgba(255, 252, 244, 0.9);
  color: #171d19;
  box-shadow: 0 18px 60px rgba(23, 29, 25, 0.12);
}

.composer textarea:focus {
  border-color: rgba(15, 118, 110, 0.35);
}

.composer button {
  width: 52px;
  height: 52px;
  border: 0;
  border-radius: 20px;
  display: grid;
  place-items: center;
  background: #132018;
  color: #71ffb3;
  cursor: pointer;
  box-shadow: 0 18px 48px rgba(19, 32, 24, 0.2);
}

.composer button:disabled {
  opacity: 0.42;
  cursor: not-allowed;
}

.composer button.stop {
  background: var(--qa-danger-bg);
  color: var(--qa-danger-text);
}

@keyframes typing {
  0%, 80%, 100% { opacity: 0.3; transform: translateY(0); }
  40% { opacity: 1; transform: translateY(-3px); }
}

@media (max-width: 960px) {
  .qa-page {
    display: block;
  }

  .qa-sidebar {
    position: relative;
    height: auto;
    grid-template-rows: auto auto auto auto;
  }

  .conversation-panel {
    max-height: 260px;
  }

  .qa-main {
    height: auto;
    min-height: 100vh;
  }

  .qa-topbar {
    align-items: flex-start;
    flex-direction: column;
    padding: 20px;
  }

  .top-actions {
    width: 100%;
    flex-wrap: wrap;
  }

  .prompt-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 620px) {
  .qa-scroll {
    padding-inline: 14px;
  }

  .message-row,
  .message-row.user {
    grid-template-columns: 1fr;
  }

  .message-row .avatar {
    display: none;
  }

  .message-row.user .message-body {
    grid-column: auto;
  }

  .composer {
    width: 100%;
    padding-inline: 14px;
  }
}

/* theme-token-alignment:start */
.qa-page {
  --qa-danger-bg: color-mix(in srgb, var(--danger-glow) 84%, var(--bg-color));
  --qa-danger-text: var(--text-inverse);
  --qa-code-bg: color-mix(in srgb, var(--surface-solid) 92%, var(--bg-color));
  color: var(--text-primary);
  background: transparent;
}

.qa-sidebar {
  border-color: var(--border-color);
  background: rgba(var(--glass-bg-rgb), var(--glass-opacity));
  box-shadow: var(--shadow-soft);
  backdrop-filter: blur(var(--glass-blur));
  color: var(--text-primary);
}

.brand-block span,
.hero-mark,
.avatar,
.prompt-grid span {
  background: rgba(var(--primary-rgb), 0.12);
  color: var(--primary-color);
}

.brand-block strong,
.conversation-item strong,
.sidebar-foot strong,
.qa-topbar h1,
.qa-hero h2,
.prompt-grid strong,
.bubble,
.navigation-card strong,
.related-item strong {
  color: var(--text-primary);
}

.brand-block em,
.panel-title,
.conversation-item em,
.conversation-empty,
.sidebar-foot span,
.qa-hero p,
.prompt-grid em,
.navigation-card em,
.navigation-card small,
.related-item em,
.related-empty,
.message-meta,
.typing-card,
.thinking-card pre,
.diagnostics-head b,
.diagnostics-body span,
.diagnostics-body p {
  color: var(--text-secondary);
}

.new-chat,
.composer button,
.navigation-goto,
.message-row.user .bubble {
  background: var(--primary-color);
  color: var(--text-inverse);
}

.conversation-panel,
.conversation-item,
.sidebar-foot div,
.qa-hero,
.prompt-grid button,
.bubble,
.thinking-card,
.navigation-card,
.diagnostics-card,
.related-card,
.related-item,
.typing-card,
.top-actions select,
.top-actions button,
.top-actions label,
.composer textarea {
  border-color: var(--border-color);
  background: rgba(var(--glass-bg-rgb), var(--glass-opacity));
  backdrop-filter: blur(var(--glass-blur));
  color: var(--text-primary);
}

.conversation-item:hover,
.conversation-item.active,
.prompt-grid button:hover,
.navigation-card,
.related-card,
.thinking-card,
.diagnostics-card {
  border-color: rgba(var(--primary-rgb), 0.28);
  background: rgba(var(--primary-rgb), 0.09);
}

.qa-topbar {
  border-color: var(--border-color);
  background: rgba(var(--glass-bg-rgb), var(--glass-opacity));
  box-shadow: var(--shadow-soft);
  backdrop-filter: blur(var(--glass-blur));
}

.eyebrow,
.top-actions label span,
.top-actions .web-search-toggle,
.related-head,
.diagnostics-head span,
.navigation-card span,
.message-meta button,
.thinking-card,
.top-actions input {
  color: var(--primary-color);
  accent-color: var(--primary-color);
}

.diagnostics-body div {
  border-color: var(--border-color);
  background: color-mix(in srgb, var(--surface-2) 68%, transparent);
}

.diagnostics-body strong {
  color: var(--text-primary);
}

.bubble :deep(pre),
.debug-grid pre {
  background: var(--qa-code-bg);
  color: var(--text-primary);
}

.bubble :deep(code) {
  background: rgba(var(--primary-rgb), 0.1);
  color: var(--primary-color);
}

.message-row.user .avatar {
  background: rgba(var(--primary-rgb), 0.12);
  color: var(--primary-color);
}

.typing-card span {
  background: var(--primary-color);
}

.related-card.web {
  border-color: color-mix(in srgb, var(--accent-glow) 22%, var(--border-color));
  background: color-mix(in srgb, var(--accent-glow) 8%, var(--surface-2));
}

.composer {
  background: linear-gradient(180deg, color-mix(in srgb, var(--bg-color) 0%, transparent), var(--bg-color) 42%, var(--bg-color));
}

.composer textarea:focus {
  border-color: rgba(var(--primary-rgb), 0.36);
}

.composer button {
  box-shadow: 0 18px 48px rgba(var(--primary-rgb), 0.16);
}

.composer button.stop {
  background: var(--qa-danger-bg);
  color: var(--qa-danger-text);
}

.top-actions select option {
  background: var(--surface-solid);
  color: var(--text-primary);
}

.top-actions select option:checked {
  background: color-mix(in srgb, var(--primary-color) 22%, var(--surface-solid));
  color: var(--text-primary);
}
/* theme-token-alignment:end */
/* ai-density-adjustment:start */
.qa-page {
  min-height: calc(100dvh - var(--header-height, 72px));
  grid-template-columns: 280px minmax(0, 1fr);
}

.qa-sidebar,
.qa-main {
  height: calc(100dvh - var(--header-height, 72px));
}

.qa-sidebar {
  gap: 12px;
  padding: 16px;
}

.brand-block {
  padding-bottom: 14px;
}

.brand-block span {
  width: 38px;
  height: 38px;
  border-radius: 14px;
}

.new-chat {
  min-height: 40px;
  border-radius: 14px;
}

.conversation-item {
  padding: 10px 34px 10px 11px;
  border-radius: 14px;
}

.qa-topbar {
  min-height: 88px;
  align-items: center;
  padding: 12px clamp(20px, 2vw, 32px) 10px;
}

.qa-topbar > div:first-child {
  max-width: min(980px, calc(100% - 390px));
}

.eyebrow {
  font-size: 11px;
  letter-spacing: 0;
}

.qa-topbar h1 {
  margin-top: 3px;
  font-size: clamp(20px, 1.55vw, 28px);
  line-height: 1.14;
  letter-spacing: 0;
}

.top-actions {
  flex: 0 0 auto;
  align-self: center;
}

.top-actions select,
.top-actions button,
.top-actions label {
  min-height: 34px;
}

.top-actions button {
  width: 34px;
}

.qa-scroll {
  width: 100%;
  justify-self: stretch;
  padding: 4px clamp(24px, 3vw, 52px) 94px;
}

.qa-hero {
  padding: 30px 0 22px;
  border-color: transparent;
  background: transparent;
  box-shadow: none;
}

.hero-mark {
  width: 54px;
  height: 54px;
  border-radius: 18px;
  box-shadow: 0 16px 42px rgba(var(--primary-rgb), 0.14);
}

.qa-hero h2 {
  max-width: 620px;
  margin: 16px 0 8px;
  font-size: clamp(28px, 3.2vw, 42px);
  line-height: 1.04;
  letter-spacing: 0;
}

.qa-hero p {
  max-width: 600px;
  font-size: 14px;
  line-height: 1.65;
}

.prompt-grid {
  width: min(100%, 660px);
  gap: 10px;
  margin-top: 18px;
}

.prompt-grid button {
  min-height: 86px;
  padding: 13px 14px;
  border-radius: 18px;
}

.prompt-grid span {
  width: 28px;
  height: 28px;
  margin-bottom: 8px;
  border-radius: 11px;
}

.prompt-grid strong {
  font-size: 14px;
}

.prompt-grid em {
  margin-top: 3px;
  font-size: 11px;
}

.message-row {
  margin: 14px 0;
}

.avatar {
  width: 34px;
  height: 34px;
  border-radius: 13px;
}

.message-body {
  max-width: min(1240px, calc(100% - 54px));
}

.bubble {
  padding: 13px 15px;
  border-radius: 18px;
  line-height: 1.68;
}

.composer {
  width: 100%;
  justify-self: stretch;
  grid-template-columns: minmax(0, 1fr) 48px;
  padding: 10px clamp(24px, 3vw, 52px) 18px;
}

.composer textarea {
  min-height: 48px;
  max-height: 126px;
  padding: 14px 16px;
  border-radius: 20px;
  box-shadow: var(--shadow-soft);
}

.composer button {
  width: 48px;
  height: 48px;
  border-radius: 18px;
}

@media (max-width: 960px) {
  .qa-page {
    min-height: calc(100dvh - var(--header-height, 72px));
    display: grid;
    grid-template-columns: 1fr;
  }

  .qa-sidebar,
  .qa-main {
    height: auto;
  }

  .qa-topbar > div:first-child {
    max-width: none;
  }

  .qa-sidebar {
    position: relative;
    border-right: 0;
    border-bottom: 1px solid var(--border-color);
  }

  .qa-main {
    min-height: calc(100dvh - var(--header-height, 72px));
  }

  .qa-hero h2 {
    font-size: clamp(26px, 8vw, 38px);
  }
}

@media (max-width: 720px) {
  .qa-topbar {
    padding: 16px;
  }

  .top-actions {
    align-items: stretch;
  }

  .top-actions select,
  .top-actions label,
  .top-actions .web-search-toggle {
    flex: 1 1 150px;
    justify-content: center;
  }

  .qa-scroll {
    padding: 8px 14px 90px;
  }

  .message-row,
  .message-row.user {
    grid-template-columns: 1fr;
  }

  .message-row .avatar,
  .message-row.user .avatar {
    display: none;
  }

  .message-body,
  .message-row.user .message-body {
    width: 100%;
    max-width: 100%;
    grid-column: auto;
    justify-self: stretch;
  }

  .navigation-body {
    grid-template-columns: 1fr;
  }

  .diagnostics-body {
    grid-template-columns: 1fr;
  }

  .navigation-goto {
    justify-self: start;
  }

  .composer {
    grid-template-columns: minmax(0, 1fr) 46px;
    padding: 10px 14px 14px;
  }

  .composer button {
    width: 46px;
    height: 46px;
    border-radius: 17px;
  }
}
/* ai-density-adjustment:end */
/* ai-chat-reference-layout:start */
.qa-main {
  --qa-chat-bg: transparent;
  --qa-chat-surface: color-mix(in srgb, var(--surface-2) 86%, transparent);
  --qa-chat-surface-strong: rgba(var(--glass-bg-rgb), var(--glass-opacity));
  --qa-chat-soft: color-mix(in srgb, var(--primary-color) 10%, var(--surface-2));
  --qa-chat-soft-hover: color-mix(in srgb, var(--primary-color) 14%, var(--surface-2));
  --qa-chat-text: var(--text-primary);
  --qa-chat-muted: var(--text-secondary);
  --qa-chat-border: var(--border-color);
  --qa-chat-accent: var(--primary-color);
  background: transparent;
}

.qa-scroll {
  padding: 24px clamp(28px, 5.2vw, 108px) 124px;
}

.message-row,
.message-row.user {
  display: flex;
  margin: 28px 0 42px;
}

.message-row.assistant {
  justify-content: flex-start;
}

.message-row.user {
  justify-content: flex-end;
}

.message-row .avatar,
.message-row.user .avatar {
  display: none;
}

.message-body,
.message-row.user .message-body {
  grid-column: auto;
}

.message-row.assistant .message-body {
  width: min(100%, 1180px);
  max-width: min(100%, 1180px);
}

.message-row.user .message-body {
  width: auto;
  max-width: min(680px, 62vw);
  justify-self: end;
}

.message-row.assistant .bubble {
  padding: 0;
  border: 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
  color: var(--qa-chat-text);
  font-size: clamp(16px, 1.08vw, 20px);
  line-height: 1.82;
}

.message-row.user .bubble {
  display: inline-block;
  padding: 18px 28px;
  border: 0;
  border-radius: 30px;
  background: var(--qa-chat-soft);
  color: var(--qa-chat-text);
  box-shadow: none;
  font-size: clamp(17px, 1.2vw, 24px);
  line-height: 1.45;
}

.message-row.user .bubble :deep(p) {
  margin: 0;
}

.message-row.assistant .bubble :deep(p) {
  margin: 0 0 18px;
}

.message-row.assistant .bubble :deep(ul),
.message-row.assistant .bubble :deep(ol) {
  margin: 14px 0 20px;
}

.message-row.assistant .bubble :deep(li) {
  margin: 7px 0;
}

.thinking-card {
  display: inline-flex;
  width: auto;
  max-width: 100%;
  margin: 0 0 18px;
  padding: 0;
  border: 0;
  border-radius: 0;
  background: transparent;
  color: var(--qa-chat-muted);
  box-shadow: none;
}

.thinking-card.thinking-placeholder {
  margin-bottom: 8px;
  cursor: default;
}

.thinking-head {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: var(--qa-chat-muted);
  font-size: clamp(18px, 1.25vw, 24px);
  line-height: 1.4;
}

.thinking-head svg:first-child {
  color: var(--qa-chat-accent);
}

.thinking-head strong {
  font-weight: 500;
}

.thinking-head svg:last-child {
  color: var(--qa-chat-muted);
  transition: transform 160ms ease;
}

.thinking-card.open .thinking-head svg:last-child {
  transform: rotate(180deg);
}

.thinking-stream {
  display: grid;
  grid-template-columns: 16px minmax(0, 1fr);
  gap: 12px;
  max-width: min(100%, 1060px);
  margin: 0 0 22px 3px;
  color: var(--qa-chat-muted);
}

.thinking-stream::before {
  content: '';
  width: 10px;
  height: 10px;
  margin-top: 0.82em;
  border-radius: 999px;
  background: var(--qa-chat-accent);
  box-shadow: 0 0 0 4px rgba(var(--primary-rgb), 0.1);
}

.thinking-stream.live::before {
  animation: thinkingPulse 1.15s ease-in-out infinite;
}

.thinking-stream pre {
  margin: 0;
  padding: 0;
  color: var(--qa-chat-muted);
  font-family: inherit;
  font-size: clamp(16px, 1.08vw, 20px);
  line-height: 1.88;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

@keyframes thinkingPulse {
  0%, 100% {
    opacity: 0.66;
    transform: scale(0.92);
  }
  50% {
    opacity: 1;
    transform: scale(1.12);
  }
}

.message-meta,
.user-message-tools {
  display: flex;
  align-items: center;
  gap: 16px;
  color: var(--qa-chat-muted);
}

.message-meta {
  margin-top: 20px;
}

.user-message-tools {
  justify-content: flex-end;
  margin-top: 12px;
  padding-right: 8px;
}

.message-meta time {
  display: none;
}

.message-meta button,
.user-message-tools button {
  width: 28px;
  height: 28px;
  display: grid;
  place-items: center;
  border: 0;
  border-radius: 10px;
  background: transparent;
  color: var(--qa-chat-muted);
  cursor: pointer;
  transition: background-color 140ms ease, color 140ms ease, transform 140ms ease;
}

.message-meta button:hover,
.user-message-tools button:hover,
.message-meta button.active {
  background: var(--qa-chat-soft-hover);
  color: var(--qa-chat-accent);
  transform: translateY(-1px);
}

.diagnostics-card,
.navigation-card,
.related-card {
  max-width: 900px;
  margin-top: 18px;
  border-color: var(--qa-chat-border);
  border-radius: 16px;
  background: var(--qa-chat-surface);
  box-shadow: none;
}

.typing-card {
  padding: 14px 20px;
  background: var(--qa-chat-surface);
  color: var(--qa-chat-muted);
  box-shadow: none;
}

.typing-card span {
  background: var(--qa-chat-accent);
}

.qa-topbar {
  background: rgba(var(--glass-bg-rgb), var(--glass-opacity));
  backdrop-filter: blur(var(--glass-blur));
}

.composer {
  background: linear-gradient(180deg, transparent, rgba(var(--glass-bg-rgb), 0.36) 48%, rgba(var(--glass-bg-rgb), 0.5));
  backdrop-filter: blur(var(--glass-blur));
}

.composer textarea {
  background: var(--qa-chat-surface-strong);
  color: var(--qa-chat-text);
  box-shadow: 0 14px 48px rgba(var(--primary-rgb), 0.12);
  backdrop-filter: blur(var(--glass-blur));
}

@media (max-width: 720px) {
  .qa-scroll {
    padding: 16px 16px 104px;
  }

  .message-row,
  .message-row.user {
    margin: 22px 0 34px;
  }

  .message-row.user .message-body,
  .message-row.assistant .message-body {
    width: 100%;
    max-width: 100%;
  }

  .message-row.user .bubble {
    max-width: 88%;
    padding: 14px 20px;
    border-radius: 24px;
  }

  .message-meta {
    gap: 12px;
  }
}
/* ai-chat-reference-layout:end */

/* ai-liquid-system-pass:start */
.qa-page {
  color: var(--text-primary);
  background: transparent;
}

.qa-sidebar {
  border-right: 1px solid var(--border-color);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.075), rgba(255, 255, 255, 0.018)),
    color-mix(in srgb, rgba(var(--glass-bg-rgb), var(--glass-opacity)) 88%, transparent);
  color: var(--text-primary);
  box-shadow: 16px 0 42px rgba(0, 0, 0, 0.08);
  backdrop-filter: blur(var(--glass-blur)) saturate(var(--glass-saturate));
  -webkit-backdrop-filter: blur(var(--glass-blur)) saturate(var(--glass-saturate));
}

.brand-block span {
  background: rgba(var(--primary-rgb), 0.12);
  color: var(--primary-color);
}

.brand-block em,
.panel-title,
.conversation-item em {
  color: var(--text-secondary);
}

.new-chat {
  background: var(--primary-color);
  color: var(--primary-contrast, #fff);
  box-shadow: var(--shadow-ring);
}

.conversation-item {
  border-color: var(--border-color);
  background: color-mix(in srgb, var(--panel-bg) 72%, transparent);
  transition:
    border-color var(--motion-hover) var(--ease-liquid),
    background var(--motion-medium) var(--ease-smooth),
    box-shadow var(--motion-medium) var(--ease-smooth),
    transform var(--motion-hover) var(--ease-liquid);
}

.conversation-item:hover,
.conversation-item.active {
  border-color: color-mix(in srgb, var(--primary-color) 44%, var(--border-color));
  background: rgba(var(--primary-rgb), 0.1);
}

.qa-topbar {
  border-bottom: 1px solid var(--border-color);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.065), rgba(255, 255, 255, 0.012)),
    color-mix(in srgb, rgba(var(--glass-bg-rgb), var(--glass-opacity)) 82%, transparent);
  -webkit-backdrop-filter: blur(var(--glass-blur)) saturate(var(--glass-saturate));
  backdrop-filter: blur(var(--glass-blur)) saturate(var(--glass-saturate));
}

.top-actions select,
.top-actions button,
.top-actions label,
.composer textarea,
.related-item,
.diagnostics-body div {
  border-color: var(--border-color);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.055), transparent),
    color-mix(in srgb, var(--panel-bg) 82%, transparent);
  color: var(--text-primary);
}

.top-actions .web-search-toggle,
.top-actions label {
  color: var(--primary-color);
}

.top-actions .web-search-toggle.active,
.assistant-web-toggle.active {
  border-color: color-mix(in srgb, var(--primary-color) 48%, var(--border-color));
  background: rgba(var(--primary-rgb), 0.12);
  box-shadow: var(--shadow-ring);
}

.diagnostics-card,
.navigation-card,
.related-card,
.typing-card {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.06), transparent),
    color-mix(in srgb, var(--panel-bg) 78%, transparent);
  box-shadow: var(--shadow-soft);
  backdrop-filter: blur(var(--glass-blur));
  -webkit-backdrop-filter: blur(var(--glass-blur));
}

.related-card.web {
  border-color: color-mix(in srgb, var(--accent-glow) 36%, var(--border-color));
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--accent-glow) 11%, transparent), transparent),
    color-mix(in srgb, var(--panel-bg) 80%, transparent);
}

.web-query-line,
.diagnostics-head b,
.diagnostics-body p,
.navigation-card em,
.navigation-card small {
  color: var(--text-secondary);
}

.related-item {
  transition:
    border-color var(--motion-hover) var(--ease-liquid),
    background var(--motion-medium) var(--ease-smooth),
    transform var(--motion-hover) var(--ease-liquid);
}

.related-item strong,
.diagnostics-body strong {
  color: var(--text-primary);
}

.composer {
  background:
    linear-gradient(180deg, transparent, color-mix(in srgb, rgba(var(--glass-bg-rgb), var(--glass-opacity)) 62%, transparent) 58%, rgba(var(--glass-bg-rgb), var(--glass-opacity)));
  -webkit-backdrop-filter: blur(var(--glass-blur)) saturate(var(--glass-saturate));
  backdrop-filter: blur(var(--glass-blur)) saturate(var(--glass-saturate));
}

.composer textarea {
  box-shadow: var(--shadow-ring);
  -webkit-backdrop-filter: blur(var(--glass-blur)) saturate(var(--glass-saturate));
  backdrop-filter: blur(var(--glass-blur)) saturate(var(--glass-saturate));
}

@media (hover: hover) and (pointer: fine) {
  .conversation-item:hover,
  .top-actions button:hover,
  .top-actions label:hover,
  .related-item:hover,
  .message-meta button:hover,
  .user-message-tools button:hover {
    transform: translate3d(0, -1px, 0);
  }

  .diagnostics-card:hover,
  .navigation-card:hover,
  .related-card:hover {
    border-color: color-mix(in srgb, var(--primary-color) 30%, var(--border-color));
  }
}

@media (prefers-reduced-motion: reduce) {
  .qa-page,
  .conversation-item,
  .top-actions button,
  .top-actions label,
  .related-item,
  .message-meta button,
  .user-message-tools button,
  .diagnostics-card,
  .navigation-card,
  .related-card {
    transition: none;
    transform: none !important;
  }
}
/* ai-liquid-system-pass:end */

/* ai-button-polish:start */
.new-chat,
.top-actions select,
.top-actions button,
.top-actions label,
.prompt-grid button,
.conversation-item,
.navigation-goto,
.composer button {
  min-width: 0;
  user-select: none;
}

.new-chat {
  position: relative;
  min-height: 42px;
  justify-content: flex-start;
  padding: 0 13px;
  border: 1px solid color-mix(in srgb, var(--primary-color) 34%, var(--border-color));
  border-radius: 13px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.09), rgba(255, 255, 255, 0.018)),
    rgba(var(--primary-rgb), 0.1);
  color: var(--text-primary);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.12),
    0 12px 28px rgba(var(--primary-rgb), 0.08);
  overflow: hidden;
  transition:
    transform var(--motion-hover) var(--ease-liquid),
    border-color var(--motion-hover) var(--ease-liquid),
    background var(--motion-medium) var(--ease-smooth),
    box-shadow var(--motion-medium) var(--ease-smooth);
}

.new-chat svg {
  flex: 0 0 auto;
  width: 24px;
  height: 24px;
  padding: 5px;
  border-radius: 9px;
  background: rgba(var(--primary-rgb), 0.16);
  color: var(--primary-color);
}

.new-chat:hover {
  transform: translate3d(0, -1px, 0);
  border-color: color-mix(in srgb, var(--primary-color) 52%, var(--border-color));
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.12), rgba(255, 255, 255, 0.026)),
    rgba(var(--primary-rgb), 0.14);
  box-shadow: var(--shadow-ring);
}

.new-chat:active,
.top-actions button:active,
.prompt-grid button:active,
.conversation-item:active,
.composer button:active {
  transform: translate3d(0, 0, 0) scale(0.985);
}

.conversation-item {
  border-radius: 12px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.055), rgba(255, 255, 255, 0.012)),
    color-mix(in srgb, var(--panel-bg) 70%, transparent);
}

.conversation-item i {
  display: grid;
  place-items: center;
  width: 24px;
  height: 24px;
  border-radius: 8px;
  color: var(--text-muted);
  transition: background 160ms ease, color 160ms ease, opacity 160ms ease;
}

.conversation-item i:hover {
  background: color-mix(in srgb, var(--danger-glow) 12%, transparent);
  color: var(--danger-glow);
  opacity: 1;
}

.top-actions {
  gap: 7px;
}

.top-actions select,
.top-actions button,
.top-actions label {
  border-radius: 11px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.075), rgba(255, 255, 255, 0.018)),
    color-mix(in srgb, var(--panel-bg) 78%, transparent);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.08);
  transition:
    transform var(--motion-hover) var(--ease-liquid),
    border-color var(--motion-hover) var(--ease-liquid),
    background var(--motion-medium) var(--ease-smooth),
    box-shadow var(--motion-medium) var(--ease-smooth);
}

.top-actions select {
  max-width: 132px;
}

.top-actions button {
  border-radius: 11px;
}

.top-actions .web-search-toggle {
  max-width: 112px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.top-actions button:hover,
.top-actions label:hover,
.top-actions select:hover {
  border-color: color-mix(in srgb, var(--primary-color) 38%, var(--border-color));
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.09), rgba(255, 255, 255, 0.02)),
    rgba(var(--primary-rgb), 0.08);
  box-shadow: var(--shadow-ring);
}

.prompt-grid button {
  border-radius: 16px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.075), rgba(255, 255, 255, 0.018)),
    color-mix(in srgb, var(--panel-bg) 80%, transparent);
}

.composer button {
  border: 1px solid color-mix(in srgb, var(--primary-color) 34%, var(--border-color));
  background:
    radial-gradient(circle at 35% 20%, rgba(255, 255, 255, 0.2), transparent 34%),
    rgba(var(--primary-rgb), 0.16);
  color: var(--primary-color);
  box-shadow: var(--shadow-ring);
}

.composer button:not(:disabled):hover {
  transform: translate3d(0, -1px, 0);
  border-color: color-mix(in srgb, var(--primary-color) 52%, var(--border-color));
  background:
    radial-gradient(circle at 35% 20%, rgba(255, 255, 255, 0.25), transparent 34%),
    rgba(var(--primary-rgb), 0.2);
}

@media (max-width: 720px) {
  .top-actions .web-search-toggle {
    max-width: none;
  }
}
/* ai-button-polish:end */

/* ai-context-materials-pass:start */
.qa-main {
  grid-template-rows: auto auto minmax(0, 1fr) auto;
}

.material-strip {
  display: grid;
  gap: 8px;
  padding: 8px clamp(20px, 2vw, 32px) 10px;
  border-bottom: 1px solid color-mix(in srgb, var(--primary-color) 10%, var(--border-color));
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.045), rgba(255, 255, 255, 0.01)),
    color-mix(in srgb, rgba(var(--glass-bg-rgb), var(--glass-opacity)) 54%, transparent);
  -webkit-backdrop-filter: blur(var(--glass-blur)) saturate(var(--glass-saturate));
  backdrop-filter: blur(var(--glass-blur)) saturate(var(--glass-saturate));
}

.material-strip-head {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.material-toggle,
.material-refresh,
.selected-materials button,
.material-item {
  border: 1px solid var(--border-color);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.065), transparent),
    color-mix(in srgb, var(--panel-bg) 76%, transparent);
  color: var(--text-primary);
  -webkit-backdrop-filter: blur(var(--glass-blur)) saturate(var(--glass-saturate));
  backdrop-filter: blur(var(--glass-blur)) saturate(var(--glass-saturate));
  transition:
    transform var(--motion-hover) var(--ease-liquid),
    border-color var(--motion-hover) var(--ease-liquid),
    background var(--motion-medium) var(--ease-smooth),
    box-shadow var(--motion-medium) var(--ease-smooth);
}

.material-toggle {
  flex: 1 1 auto;
  min-width: 0;
  min-height: 38px;
  display: grid;
  grid-template-columns: auto auto minmax(0, 1fr);
  align-items: center;
  gap: 9px;
  padding: 0 12px;
  border-radius: 12px;
  text-align: left;
  cursor: pointer;
}

.material-toggle svg,
.material-refresh svg {
  color: var(--primary-color);
}

.material-toggle span {
  color: var(--text-primary);
  font-size: 12px;
  font-weight: var(--font-weight-title);
  white-space: nowrap;
}

.material-toggle b {
  min-width: 0;
  overflow: hidden;
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.material-refresh {
  flex: 0 0 auto;
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  border-radius: 12px;
  cursor: pointer;
}

.selected-materials {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
}

.selected-materials button {
  max-width: min(320px, 100%);
  min-height: 30px;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 0 9px;
  border-radius: 999px;
  color: var(--primary-color);
  cursor: pointer;
}

.selected-materials button span {
  min-width: 0;
  overflow: hidden;
  color: var(--text-primary);
  font-size: 11px;
  font-weight: var(--font-weight-title);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.selected-materials .clear-materials {
  color: var(--text-secondary);
}

.material-panel {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
  max-height: 210px;
  overflow-y: auto;
  padding: 2px 2px 4px;
}

.material-item {
  position: relative;
  min-height: 72px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 4px 8px;
  align-content: start;
  padding: 10px;
  border-radius: 12px;
  text-align: left;
  cursor: pointer;
}

.material-item span {
  grid-column: 1 / -1;
  color: var(--primary-color);
  font-size: 10px;
  font-weight: var(--font-weight-title);
}

.material-item strong,
.material-item em {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.material-item strong {
  color: var(--text-primary);
  font-size: 12px;
}

.material-item em {
  grid-column: 1 / -1;
  color: var(--text-secondary);
  font-size: 11px;
  font-style: normal;
}

.material-item.active {
  border-color: color-mix(in srgb, var(--primary-color) 46%, var(--border-color));
  background: rgba(var(--primary-rgb), 0.12);
  box-shadow: var(--shadow-ring);
}

.material-item > svg {
  position: absolute;
  top: 10px;
  right: 10px;
  color: var(--primary-color);
}

.material-empty {
  grid-column: 1 / -1;
  padding: 12px;
  border: 1px dashed var(--border-color);
  border-radius: 12px;
  color: var(--text-secondary);
  font-size: 12px;
  text-align: center;
}

.conversation-panel {
  padding-right: 2px;
  background: transparent !important;
}

.conversation-item {
  border-color: color-mix(in srgb, var(--primary-color) 14%, var(--border-color));
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.048), rgba(255, 255, 255, 0.01)),
    color-mix(in srgb, rgba(var(--glass-bg-rgb), var(--glass-opacity)) 46%, transparent) !important;
  box-shadow: none;
}

.conversation-item:hover,
.conversation-item.active {
  border-color: color-mix(in srgb, var(--primary-color) 46%, var(--border-color));
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.065), rgba(255, 255, 255, 0.015)),
    rgba(var(--primary-rgb), 0.1) !important;
  box-shadow: var(--shadow-ring);
}

.thinking-card {
  min-height: 34px;
}

.thinking-stream pre {
  color: var(--text-secondary);
}

.material-toggle:hover,
.material-refresh:hover,
.selected-materials button:hover,
.material-item:hover {
  transform: translate3d(0, -1px, 0);
  border-color: color-mix(in srgb, var(--primary-color) 40%, var(--border-color));
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.075), rgba(255, 255, 255, 0.018)),
    rgba(var(--primary-rgb), 0.08);
}

@media (max-width: 1280px) {
  .material-panel {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .material-strip {
    padding-inline: 14px;
  }

  .material-panel {
    grid-template-columns: 1fr;
    max-height: 280px;
  }
}
/* ai-context-materials-pass:end */

/* ai-composer-control-redesign:start */
.qa-main {
  grid-template-rows: auto minmax(0, 1fr) auto !important;
}

.qa-topbar {
  min-height: 68px;
  padding: 10px clamp(20px, 2vw, 32px);
}

.qa-topbar h1 {
  font-size: clamp(18px, 1.35vw, 24px);
}

.top-actions {
  gap: 6px;
}

.top-actions .top-icon-action {
  position: relative;
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  border: 1px solid color-mix(in srgb, var(--primary-color) 14%, var(--border-color));
  border-radius: 12px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.06), rgba(255, 255, 255, 0.012)),
    color-mix(in srgb, var(--panel-bg) 58%, transparent);
  color: var(--text-secondary);
  box-shadow: none;
}

.top-actions .top-icon-action.active,
.top-actions .top-icon-action:hover {
  border-color: color-mix(in srgb, var(--primary-color) 42%, var(--border-color));
  background: rgba(var(--primary-rgb), 0.1);
  color: var(--primary-color);
  box-shadow: var(--shadow-ring);
}

.top-actions .top-icon-action b {
  position: absolute;
  top: -5px;
  right: -5px;
  min-width: 17px;
  height: 17px;
  display: grid;
  place-items: center;
  border-radius: 999px;
  background: var(--primary-color);
  color: var(--primary-contrast, var(--text-inverse));
  font-size: 10px;
  font-weight: var(--font-weight-title);
}

.composer {
  display: block !important;
  grid-template-columns: none !important;
  padding: 12px clamp(24px, 3vw, 52px) 18px;
  background:
    linear-gradient(180deg, transparent, color-mix(in srgb, rgba(var(--glass-bg-rgb), var(--glass-opacity)) 54%, transparent) 38%, rgba(var(--glass-bg-rgb), var(--glass-opacity)));
}

.composer-shell {
  width: min(1120px, 100%);
  margin: 0 auto;
  display: grid;
  gap: 10px;
  padding: 12px;
  border: 1px solid color-mix(in srgb, var(--primary-color) 16%, var(--border-color));
  border-radius: 22px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.075), rgba(255, 255, 255, 0.018)),
    color-mix(in srgb, var(--panel-bg) 76%, transparent);
  box-shadow:
    0 22px 60px rgba(0, 0, 0, 0.12),
    inset 0 1px 0 rgba(255, 255, 255, 0.1);
  -webkit-backdrop-filter: blur(var(--glass-blur)) saturate(var(--glass-saturate));
  backdrop-filter: blur(var(--glass-blur)) saturate(var(--glass-saturate));
}

.composer-shell.expanded {
  border-color: color-mix(in srgb, var(--primary-color) 34%, var(--border-color));
}

.composer textarea {
  width: 100%;
  min-height: 72px;
  max-height: 160px;
  padding: 10px 8px 4px;
  border: 0 !important;
  border-radius: 14px;
  background: transparent !important;
  box-shadow: none !important;
  color: var(--text-primary);
  font-size: 15px;
  line-height: 1.65;
}

.composer textarea:focus {
  border: 0 !important;
  box-shadow: none !important;
}

.composer-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  min-width: 0;
}

.composer-left-tools {
  min-width: 0;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.composer-tool,
.reasoning-segment,
.temperature-control,
.composer-submit,
.material-panel-head button {
  border: 1px solid var(--border-color);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.06), rgba(255, 255, 255, 0.014)),
    color-mix(in srgb, var(--panel-bg) 78%, transparent);
  color: var(--text-primary);
  -webkit-backdrop-filter: blur(var(--glass-blur)) saturate(var(--glass-saturate));
  backdrop-filter: blur(var(--glass-blur)) saturate(var(--glass-saturate));
}

.composer-tool {
  width: auto !important;
  height: auto !important;
  min-height: 34px;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 0 11px;
  border-radius: 12px;
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: var(--font-weight-title);
  cursor: pointer;
}

.composer-tool svg {
  color: var(--primary-color);
}

.composer-tool.active,
.composer-tool:hover {
  border-color: color-mix(in srgb, var(--primary-color) 42%, var(--border-color));
  background: rgba(var(--primary-rgb), 0.11);
  color: var(--primary-color);
  box-shadow: var(--shadow-ring);
}

.reasoning-segment {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  min-height: 34px;
  padding: 3px;
  border-radius: 13px;
}

.reasoning-segment button {
  width: auto !important;
  height: auto !important;
  min-height: 28px;
  min-width: 52px;
  padding: 0 11px;
  border: 0;
  border-radius: 10px;
  background: transparent;
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: var(--font-weight-title);
  cursor: pointer;
  transition:
    color var(--motion-hover) var(--ease-liquid),
    background var(--motion-medium) var(--ease-smooth),
    transform var(--motion-hover) var(--ease-liquid);
}

.reasoning-segment button.active {
  background: rgba(var(--primary-rgb), 0.16);
  color: var(--primary-color);
  box-shadow: inset 0 0 0 1px rgba(var(--primary-rgb), 0.18);
}

.reasoning-segment button:hover {
  color: var(--primary-color);
  transform: translateY(-1px);
}

.temperature-control {
  flex: 0 0 auto;
  min-height: 34px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 0 11px;
  border-radius: 12px;
  color: var(--primary-color);
  font-size: 12px;
  font-weight: var(--font-weight-title);
}

.selected-materials button {
  width: auto !important;
  height: auto !important;
}

.temperature-control input {
  width: 76px;
  accent-color: var(--primary-color);
}

.composer-submit {
  flex: 0 0 auto;
  width: 38px !important;
  height: 38px !important;
  border-radius: 14px !important;
  display: grid;
  place-items: center;
  background:
    radial-gradient(circle at 35% 20%, rgba(255, 255, 255, 0.22), transparent 34%),
    var(--primary-color) !important;
  color: var(--primary-contrast, var(--text-inverse)) !important;
  box-shadow: var(--shadow-ring);
}

.composer-submit.stop {
  background: var(--qa-danger-bg) !important;
}

.material-panel {
  display: grid;
  gap: 10px;
  max-height: min(310px, 40dvh);
  overflow: hidden auto;
  padding: 0;
}

.material-panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-width: 0;
  padding: 2px 2px 0;
}

.material-panel-head strong,
.material-panel-head span {
  display: block;
}

.material-panel-head strong {
  color: var(--text-primary);
  font-size: 13px;
}

.material-panel-head span {
  margin-top: 2px;
  color: var(--text-secondary);
  font-size: 12px;
}

.material-panel-head button {
  flex: 0 0 auto;
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  border-radius: 12px;
  color: var(--primary-color);
  cursor: pointer;
}

.material-sections {
  display: grid;
  gap: 12px;
}

.material-section {
  display: grid;
  gap: 8px;
}

.material-section-title {
  display: grid;
  grid-template-columns: auto auto minmax(0, 1fr);
  align-items: center;
  gap: 8px;
  min-width: 0;
  padding: 0 2px;
}

.material-section-title span {
  color: var(--text-primary);
  font-size: 12px;
  font-weight: var(--font-weight-title);
}

.material-section-title b {
  min-width: 20px;
  height: 20px;
  display: grid;
  place-items: center;
  border-radius: 999px;
  background: rgba(var(--primary-rgb), 0.12);
  color: var(--primary-color);
  font-size: 10px;
  font-weight: var(--font-weight-title);
}

.material-section-title em {
  min-width: 0;
  overflow: hidden;
  color: var(--text-muted);
  font-size: 11px;
  font-style: normal;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.material-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.material-grid .material-item {
  width: 100% !important;
  height: auto !important;
  min-height: 82px;
}

.material-empty {
  margin: 0;
}

.material-empty-inline {
  padding: 10px;
  text-align: left;
}

.material-item .material-type-label {
  padding-right: 76px;
}

.material-item .material-badge {
  position: absolute;
  top: 8px;
  right: 8px;
  min-height: 22px;
  display: inline-flex;
  align-items: center;
  padding: 0 7px;
  border: 1px solid color-mix(in srgb, var(--primary-color) 18%, var(--border-color));
  border-radius: 999px;
  background: color-mix(in srgb, var(--panel-bg) 74%, transparent);
  color: var(--text-secondary);
  font-size: 10px;
  font-weight: var(--font-weight-title);
}

.material-grid .material-item > svg {
  top: auto;
  right: 10px;
  bottom: 10px;
}

.material-item.official .material-badge,
.selected-materials button.official small {
  border-color: color-mix(in srgb, var(--warning-color, #f59e0b) 34%, var(--border-color));
  background: color-mix(in srgb, var(--warning-color, #f59e0b) 12%, transparent);
  color: color-mix(in srgb, var(--warning-color, #f59e0b) 84%, var(--text-primary));
}

.material-item.owned .material-badge,
.selected-materials button.owned small {
  border-color: color-mix(in srgb, var(--primary-color) 34%, var(--border-color));
  background: rgba(var(--primary-rgb), 0.11);
  color: var(--primary-color);
}

.material-item.stored .material-badge,
.selected-materials button.stored small {
  border-color: color-mix(in srgb, var(--success-color, #22c55e) 32%, var(--border-color));
  background: color-mix(in srgb, var(--success-color, #22c55e) 11%, transparent);
  color: color-mix(in srgb, var(--success-color, #22c55e) 84%, var(--text-primary));
}

.material-item.readonly {
  border-style: dashed;
}

.selected-materials {
  padding: 0 2px;
}

.selected-materials button small {
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  min-height: 20px;
  padding: 0 6px;
  border: 1px solid var(--border-color);
  border-radius: 999px;
  font-size: 10px;
  font-weight: var(--font-weight-title);
}

.material-picker {
  display: grid;
  gap: 10px;
}

.material-tabs {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 6px;
  padding: 3px;
  border: 1px solid color-mix(in srgb, var(--primary-color) 12%, var(--border-color));
  border-radius: 14px;
  background: color-mix(in srgb, var(--panel-bg) 58%, transparent);
}

.material-tabs button {
  min-width: 0;
  min-height: 32px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 0 8px;
  border: 0;
  border-radius: 11px;
  background: transparent;
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: var(--font-weight-title);
  cursor: pointer;
  transition:
    background var(--motion-hover) var(--ease-liquid),
    color var(--motion-hover) var(--ease-liquid),
    transform var(--motion-hover) var(--ease-liquid);
}

.material-tabs button:hover {
  color: var(--primary-color);
}

.material-tabs button.active {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.08), transparent),
    rgba(var(--primary-rgb), 0.13);
  color: var(--primary-color);
  box-shadow: inset 0 0 0 1px rgba(var(--primary-rgb), 0.14);
}

.material-tabs button b {
  min-width: 18px;
  height: 18px;
  display: grid;
  place-items: center;
  border-radius: 999px;
  background: color-mix(in srgb, var(--panel-bg) 74%, transparent);
  color: inherit;
  font-size: 10px;
}

.material-list {
  display: grid;
  gap: 6px;
  max-height: min(260px, 34dvh);
  overflow: hidden auto;
  padding-right: 2px;
}

.material-row {
  position: relative;
  width: 100% !important;
  min-height: 56px;
  display: grid;
  grid-template-columns: 24px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border: 1px solid color-mix(in srgb, var(--primary-color) 12%, var(--border-color));
  border-radius: 13px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.045), transparent),
    color-mix(in srgb, var(--panel-bg) 66%, transparent);
  color: var(--text-primary);
  text-align: left;
  cursor: pointer;
  box-shadow: none;
  transition:
    transform var(--motion-hover) var(--ease-liquid),
    border-color var(--motion-hover) var(--ease-liquid),
    background var(--motion-hover) var(--ease-liquid);
}

.material-row:hover {
  transform: translate3d(0, -1px, 0);
  border-color: color-mix(in srgb, var(--primary-color) 34%, var(--border-color));
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.065), transparent),
    rgba(var(--primary-rgb), 0.07);
}

.material-row.active {
  border-color: color-mix(in srgb, var(--primary-color) 48%, var(--border-color));
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.075), transparent),
    rgba(var(--primary-rgb), 0.12);
  box-shadow: var(--shadow-ring);
}

.material-row.readonly {
  border-style: dashed;
}

.material-check {
  width: 22px;
  height: 22px;
  display: grid;
  place-items: center;
  border: 1px solid color-mix(in srgb, var(--primary-color) 18%, var(--border-color));
  border-radius: 999px;
  color: var(--primary-color);
  background: color-mix(in srgb, var(--panel-bg) 66%, transparent);
}

.material-row.active .material-check {
  border-color: var(--primary-color);
  background: rgba(var(--primary-rgb), 0.16);
}

.material-row-copy {
  min-width: 0;
  display: grid;
  gap: 3px;
}

.material-row-copy strong,
.material-row-copy em {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.material-row-copy strong {
  color: var(--text-primary);
  font-size: 12px;
  font-weight: var(--font-weight-title);
}

.material-row-copy em {
  color: var(--text-secondary);
  font-size: 11px;
  font-style: normal;
}

.material-row .material-badge {
  min-height: 22px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 8px;
  border: 1px solid color-mix(in srgb, var(--primary-color) 18%, var(--border-color));
  border-radius: 999px;
  background: color-mix(in srgb, var(--panel-bg) 72%, transparent);
  color: var(--text-secondary);
  font-size: 10px;
  font-weight: var(--font-weight-title);
  white-space: nowrap;
}

.material-row.official .material-badge,
.selected-chips button.official {
  border-color: color-mix(in srgb, var(--warning-color, #f59e0b) 30%, var(--border-color));
  background: color-mix(in srgb, var(--warning-color, #f59e0b) 10%, transparent);
  color: color-mix(in srgb, var(--warning-color, #f59e0b) 84%, var(--text-primary));
}

.material-row.owned .material-badge,
.selected-chips button.owned {
  border-color: color-mix(in srgb, var(--primary-color) 32%, var(--border-color));
  background: rgba(var(--primary-rgb), 0.1);
  color: var(--primary-color);
}

.material-row.stored .material-badge,
.selected-chips button.stored {
  border-color: color-mix(in srgb, var(--success-color, #22c55e) 30%, var(--border-color));
  background: color-mix(in srgb, var(--success-color, #22c55e) 10%, transparent);
  color: color-mix(in srgb, var(--success-color, #22c55e) 84%, var(--text-primary));
}

.selected-materials {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
  padding: 8px;
  border: 1px solid color-mix(in srgb, var(--primary-color) 14%, var(--border-color));
  border-radius: 16px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.055), transparent),
    color-mix(in srgb, var(--panel-bg) 62%, transparent);
}

.selected-summary {
  min-width: 0;
  display: grid;
  gap: 2px;
}

.selected-summary strong {
  color: var(--text-primary);
  font-size: 12px;
  font-weight: var(--font-weight-title);
}

.selected-summary span {
  min-width: 0;
  overflow: hidden;
  color: var(--text-secondary);
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.selected-actions {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.selected-actions button,
.selected-chips button {
  width: auto !important;
  height: auto !important;
}

.selected-actions .material-analysis-action {
  min-height: 32px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 0 12px;
  border: 1px solid color-mix(in srgb, var(--primary-color) 42%, var(--border-color));
  border-radius: 12px;
  background:
    radial-gradient(circle at 30% 20%, rgba(255, 255, 255, 0.22), transparent 38%),
    var(--primary-color);
  color: var(--primary-contrast, var(--text-inverse));
  font-size: 12px;
  font-weight: var(--font-weight-title);
  box-shadow: var(--shadow-ring);
}

.selected-actions .clear-materials {
  min-height: 32px;
  padding: 0 10px;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  background: transparent;
  color: var(--text-secondary);
  font-size: 12px;
}

.selected-chips {
  grid-column: 1 / -1;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  min-width: 0;
}

.selected-chips button,
.selected-more {
  max-width: min(240px, 100%);
  min-height: 26px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 0 8px;
  border: 1px solid var(--border-color);
  border-radius: 999px;
  color: var(--text-secondary);
  font-size: 11px;
  cursor: pointer;
}

.selected-chips button span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.selected-more {
  cursor: default;
}

.message-control-strip {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  max-width: min(100%, 900px);
  margin: 10px 0 0;
  color: var(--text-secondary);
  font-size: 11px;
  line-height: 1.35;
}

.message-control-strip.user {
  justify-content: flex-end;
}

.message-control-strip span {
  color: var(--text-muted);
  font-weight: var(--font-weight-title);
}

.message-control-strip b {
  min-height: 24px;
  display: inline-flex;
  align-items: center;
  padding: 0 8px;
  border: 1px solid color-mix(in srgb, var(--primary-color) 18%, var(--border-color));
  border-radius: 999px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.065), transparent),
    color-mix(in srgb, var(--panel-bg) 72%, transparent);
  color: var(--text-secondary);
  font-weight: var(--font-weight-title);
  -webkit-backdrop-filter: blur(var(--glass-blur)) saturate(var(--glass-saturate));
  backdrop-filter: blur(var(--glass-blur)) saturate(var(--glass-saturate));
}

.message-control-strip.assistant b:first-of-type,
.message-control-strip.user b:first-of-type {
  border-color: color-mix(in srgb, var(--primary-color) 42%, var(--border-color));
  background: rgba(var(--primary-rgb), 0.11);
  color: var(--primary-color);
}

.assistant-run-status {
  width: fit-content;
  max-width: min(100%, 820px);
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 12px;
  padding: 7px 11px;
  border: 1px solid color-mix(in srgb, var(--primary-color) 18%, var(--border-color));
  border-radius: 999px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.07), transparent),
    color-mix(in srgb, var(--panel-bg) 78%, transparent);
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.35;
  -webkit-backdrop-filter: blur(var(--glass-blur)) saturate(var(--glass-saturate));
  backdrop-filter: blur(var(--glass-blur)) saturate(var(--glass-saturate));
}

.assistant-run-status svg {
  flex: 0 0 auto;
  color: var(--primary-color);
  animation: thinkingPulse 1.15s ease-in-out infinite;
}

.composer-effect-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  min-width: 0;
  padding: 0 3px 1px;
  color: var(--text-muted);
  font-size: 11px;
  line-height: 1.35;
}

.composer-effect-line span,
.composer-effect-line b {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.composer-effect-line b {
  flex: 0 0 auto;
  color: var(--primary-color);
  font-weight: var(--font-weight-title);
}

@media (max-width: 1180px) {
  .composer-toolbar {
    align-items: stretch;
    flex-wrap: wrap;
  }

  .temperature-control {
    flex: 1 1 160px;
    justify-content: center;
  }

  .composer-submit {
    margin-left: auto;
  }
}

@media (max-width: 760px) {
  .qa-topbar {
    min-height: auto;
  }

  .qa-topbar > div:first-child {
    max-width: calc(100% - 176px);
  }

  .composer {
    padding-inline: 12px;
  }

  .composer-shell {
    border-radius: 18px;
  }

  .composer-left-tools,
  .reasoning-segment {
    width: 100%;
  }

  .reasoning-segment {
    overflow-x: auto;
  }

  .reasoning-segment button {
    flex: 1 0 auto;
    min-width: 58px;
  }

  .material-grid {
    grid-template-columns: 1fr;
  }

  .composer-effect-line {
    align-items: flex-start;
    flex-direction: column;
    gap: 3px;
  }

  .composer-effect-line span,
  .composer-effect-line b {
    white-space: normal;
  }

  .message-control-strip.user {
    justify-content: flex-start;
  }
}
/* ai-composer-control-redesign:end */

/* ai-cherry-studio-alignment:start */
.qa-page {
  --qa-shell-width: min(980px, calc(100vw - 336px));
  --qa-soft-panel:
    linear-gradient(180deg, rgba(255, 255, 255, 0.07), rgba(255, 255, 255, 0.018)),
    color-mix(in srgb, var(--panel-bg) 74%, transparent);
  --qa-quiet-panel:
    linear-gradient(180deg, rgba(255, 255, 255, 0.045), rgba(255, 255, 255, 0.01)),
    color-mix(in srgb, var(--panel-bg) 54%, transparent);
  --qa-border: color-mix(in srgb, var(--primary-color) 12%, var(--border-color));
  min-height: 100dvh;
  grid-template-columns: 268px minmax(0, 1fr) !important;
  background:
    radial-gradient(circle at 10% 10%, rgba(var(--primary-rgb), 0.08), transparent 28%),
    linear-gradient(135deg, color-mix(in srgb, var(--bg-color) 96%, var(--primary-color)), var(--bg-color));
  color: var(--text-primary);
}

.qa-sidebar {
  width: auto !important;
  height: 100dvh !important;
  padding: 14px !important;
  border-right: 1px solid var(--qa-border);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.065), rgba(255, 255, 255, 0.015)),
    color-mix(in srgb, var(--bg-color) 78%, transparent) !important;
  -webkit-backdrop-filter: blur(var(--glass-blur)) saturate(var(--glass-saturate));
  backdrop-filter: blur(var(--glass-blur)) saturate(var(--glass-saturate));
  box-shadow: inset -1px 0 0 rgba(255, 255, 255, 0.035);
}

.brand-block {
  min-height: 44px;
  padding: 6px 4px 10px;
  border-bottom: 1px solid var(--qa-border);
}

.brand-block > span,
.avatar,
.hero-mark {
  display: grid;
  place-items: center;
}

.brand-block > span {
  width: 34px;
  height: 34px;
  border-radius: 12px;
  background: rgba(var(--primary-rgb), 0.13);
  color: var(--primary-color);
  box-shadow: inset 0 0 0 1px rgba(var(--primary-rgb), 0.12);
}

.brand-block strong {
  font-size: 14px;
  letter-spacing: 0;
}

.brand-block em {
  margin-top: 2px;
  font-size: 11px;
}

.new-chat {
  min-height: 38px !important;
  margin-top: 8px;
  border-radius: 12px !important;
  justify-content: center !important;
  background: rgba(var(--primary-rgb), 0.12) !important;
  color: var(--text-primary) !important;
  box-shadow: none !important;
}

.new-chat svg {
  width: 18px !important;
  height: 18px !important;
  padding: 0 !important;
  background: transparent !important;
}

.conversation-panel {
  gap: 6px;
  min-height: 0;
  padding: 2px !important;
  overflow-y: auto;
}

.panel-title {
  padding: 8px 4px 6px;
  color: var(--text-muted);
  font-size: 11px;
}

.conversation-item {
  min-height: 54px;
  padding: 10px 34px 10px 11px !important;
  border-radius: 12px !important;
  background: transparent !important;
  box-shadow: none !important;
}

.conversation-item:hover,
.conversation-item.active {
  background: rgba(var(--primary-rgb), 0.09) !important;
  border-color: color-mix(in srgb, var(--primary-color) 28%, var(--border-color)) !important;
}

.conversation-item strong {
  font-size: 12px;
  line-height: 1.35;
}

.conversation-item em {
  margin-top: 4px;
  font-size: 10px;
}

.sidebar-foot {
  grid-template-columns: 1fr;
  gap: 6px;
}

.sidebar-foot div {
  padding: 9px 10px !important;
  border-radius: 12px !important;
  background: var(--qa-quiet-panel) !important;
}

.qa-main {
  height: 100dvh !important;
  grid-template-rows: 58px minmax(0, 1fr) auto !important;
  overflow: hidden;
}

.qa-topbar {
  min-height: 58px !important;
  padding: 8px clamp(18px, 2vw, 32px) !important;
  border-bottom: 1px solid var(--qa-border) !important;
  background: color-mix(in srgb, rgba(var(--glass-bg-rgb), var(--glass-opacity)) 42%, transparent) !important;
}

.qa-topbar > div:first-child {
  width: min(620px, 100%);
}

.eyebrow {
  color: var(--text-muted) !important;
  font-size: 10px !important;
  font-weight: var(--font-weight-label) !important;
}

.qa-topbar h1 {
  margin-top: 2px !important;
  color: var(--text-primary);
  font-size: clamp(15px, 1.05vw, 19px) !important;
  font-weight: var(--font-weight-title);
  line-height: 1.25;
}

.top-actions {
  gap: 5px !important;
}

.top-actions .top-icon-action {
  width: 34px !important;
  height: 34px !important;
  border-radius: 11px !important;
  background: transparent !important;
  color: var(--text-secondary) !important;
  box-shadow: none !important;
}

.top-actions .top-icon-action:hover,
.top-actions .top-icon-action.active {
  background: rgba(var(--primary-rgb), 0.1) !important;
  color: var(--primary-color) !important;
}

.qa-scroll {
  padding: 18px clamp(18px, 3vw, 56px) 116px !important;
  scroll-behavior: smooth;
}

.qa-scroll > .qa-hero,
.qa-scroll > .message-row {
  width: var(--qa-shell-width);
  max-width: 100%;
  margin-inline: auto;
}

.qa-hero {
  min-height: min(460px, 56dvh);
  align-content: center;
  padding: 26px 0 20px !important;
}

.hero-mark {
  width: 54px !important;
  height: 54px !important;
  border-radius: 18px !important;
  background: rgba(var(--primary-rgb), 0.13) !important;
  color: var(--primary-color) !important;
  box-shadow: none !important;
}

.qa-hero h2 {
  max-width: 640px !important;
  margin-top: 16px !important;
  font-size: clamp(25px, 3.2vw, 42px) !important;
  line-height: 1.08 !important;
}

.qa-hero p {
  max-width: 620px !important;
  color: var(--text-secondary) !important;
  line-height: 1.7 !important;
}

.prompt-grid {
  width: min(100%, 720px) !important;
  gap: 8px !important;
  margin-top: 20px !important;
}

.prompt-grid button {
  min-height: 86px !important;
  padding: 12px !important;
  border-radius: 14px !important;
  background: var(--qa-quiet-panel) !important;
  box-shadow: none !important;
}

.prompt-grid span {
  width: 28px !important;
  height: 28px !important;
  margin-bottom: 8px !important;
  border-radius: 10px !important;
}

.message-row {
  grid-template-columns: 30px minmax(0, 1fr) !important;
  gap: 10px !important;
  margin: 16px auto !important;
}

.message-row.user {
  grid-template-columns: minmax(0, 1fr) 30px !important;
}

.avatar {
  width: 30px !important;
  height: 30px !important;
  border-radius: 10px !important;
  background: rgba(var(--primary-rgb), 0.12) !important;
  color: var(--primary-color) !important;
  box-shadow: none !important;
}

.message-body {
  max-width: min(760px, 100%) !important;
}

.message-row.assistant .bubble {
  padding: 2px 0 0 !important;
  border: 0 !important;
  border-radius: 0 !important;
  background: transparent !important;
  box-shadow: none !important;
}

.message-row.user .bubble {
  max-width: min(680px, 100%);
  padding: 12px 14px !important;
  border-radius: 18px 18px 6px 18px !important;
  border: 1px solid color-mix(in srgb, var(--primary-color) 26%, var(--border-color)) !important;
  background: rgba(var(--primary-rgb), 0.13) !important;
  color: var(--text-primary) !important;
  box-shadow: none !important;
}

.bubble {
  color: var(--text-primary);
  font-size: 14px;
  line-height: 1.78 !important;
}

.thinking-card,
.assistant-run-status,
.diagnostics-card,
.navigation-card,
.related-card {
  width: fit-content;
  max-width: min(100%, 760px);
  border-radius: 13px !important;
  border-color: var(--qa-border) !important;
  background: var(--qa-quiet-panel) !important;
  box-shadow: none !important;
}

.thinking-card {
  min-height: 30px !important;
  margin: 0 0 8px !important;
  padding: 7px 10px !important;
}

.thinking-head {
  gap: 7px;
}

.thinking-head svg:first-child {
  width: 16px;
  height: 16px;
}

.thinking-head strong {
  font-size: 12px;
}

.thinking-stream {
  max-width: min(100%, 760px);
  margin: 0 0 10px;
}

.message-control-strip {
  margin-top: 8px !important;
  gap: 5px !important;
}

.message-control-strip b {
  min-height: 21px !important;
  padding: 0 7px !important;
  font-size: 10px !important;
  background: var(--qa-quiet-panel) !important;
}

.message-meta,
.user-message-tools {
  opacity: 0;
  transition: opacity var(--motion-hover) var(--ease-liquid);
}

.message-row:hover .message-meta,
.message-row:hover .user-message-tools {
  opacity: 1;
}

.message-meta button,
.user-message-tools button {
  color: var(--text-muted) !important;
}

.composer {
  position: sticky !important;
  bottom: 0;
  z-index: 20;
  padding: 10px clamp(18px, 3vw, 56px) 16px !important;
  background:
    linear-gradient(180deg, transparent, color-mix(in srgb, var(--bg-color) 78%, transparent) 22%, var(--bg-color) 76%) !important;
}

.composer-shell {
  position: relative;
  width: var(--qa-shell-width) !important;
  max-width: 100%;
  gap: 8px !important;
  padding: 10px !important;
  border-radius: 20px !important;
  background: var(--qa-soft-panel) !important;
  box-shadow:
    0 18px 46px rgba(0, 0, 0, 0.14),
    inset 0 1px 0 rgba(255, 255, 255, 0.08) !important;
}

.material-panel {
  position: absolute;
  left: 0;
  right: 0;
  bottom: calc(100% + 10px);
  max-height: min(420px, 54dvh) !important;
  padding: 12px !important;
  border: 1px solid var(--qa-border);
  border-radius: 18px;
  background: var(--qa-soft-panel) !important;
  box-shadow: var(--shadow-ring);
  -webkit-backdrop-filter: blur(var(--glass-blur)) saturate(var(--glass-saturate));
  backdrop-filter: blur(var(--glass-blur)) saturate(var(--glass-saturate));
}

.material-panel-head {
  padding: 0 !important;
}

.material-panel-head span {
  max-width: 56ch;
}

.material-tabs {
  grid-template-columns: repeat(4, minmax(0, 1fr)) !important;
  border-radius: 12px !important;
  background: color-mix(in srgb, var(--surface-2) 58%, transparent) !important;
}

.material-list {
  max-height: min(290px, 36dvh) !important;
}

.material-row {
  min-height: 50px !important;
  border-radius: 12px !important;
  background: transparent !important;
}

.material-row:hover,
.material-row.active {
  background: rgba(var(--primary-rgb), 0.09) !important;
  box-shadow: none !important;
}

.selected-materials {
  padding: 7px 8px !important;
  border-radius: 14px !important;
  background: rgba(var(--primary-rgb), 0.07) !important;
  box-shadow: none !important;
}

.selected-summary span {
  max-width: min(520px, 100%);
}

.selected-chips {
  display: none !important;
}

.selected-actions .material-analysis-action {
  min-height: 30px !important;
  border-radius: 10px !important;
  box-shadow: none !important;
}

.selected-actions .clear-materials {
  min-height: 30px !important;
}

.composer textarea {
  min-height: 62px !important;
  padding: 8px 6px 2px !important;
  border: 0 !important;
  background: transparent !important;
  box-shadow: none !important;
  line-height: 1.55 !important;
}

.composer-toolbar {
  gap: 8px !important;
}

.composer-left-tools {
  gap: 6px !important;
}

.composer-tool,
.reasoning-segment,
.temperature-control {
  min-height: 31px !important;
  border-radius: 10px !important;
  background: transparent !important;
  box-shadow: none !important;
}

.composer-tool {
  padding: 0 9px !important;
}

.temperature-control {
  color: var(--text-muted) !important;
}

.reasoning-segment {
  padding: 2px !important;
  background: color-mix(in srgb, var(--surface-2) 54%, transparent) !important;
}

.reasoning-segment button {
  min-width: 44px !important;
  min-height: 26px !important;
  padding: 0 8px !important;
}

.composer-effect-line {
  display: none !important;
}

.composer-submit {
  width: 36px !important;
  height: 36px !important;
  border-radius: 12px !important;
  box-shadow: none !important;
}

@media (max-width: 980px) {
  .qa-page {
    --qa-shell-width: min(100%, calc(100vw - 28px));
    display: block !important;
  }

  .qa-sidebar {
    height: auto !important;
    max-height: 280px;
    border-right: 0;
    border-bottom: 1px solid var(--qa-border);
  }

  .sidebar-foot {
    display: none !important;
  }

  .qa-main {
    height: calc(100dvh - 280px) !important;
    min-height: 620px;
  }
}

@media (max-width: 720px) {
  .qa-main {
    height: auto !important;
    min-height: 100dvh;
  }

  .qa-topbar {
    align-items: flex-start;
  }

  .top-actions {
    flex-wrap: wrap;
    justify-content: flex-end;
  }

  .prompt-grid,
  .material-tabs {
    grid-template-columns: 1fr 1fr !important;
  }

  .selected-materials,
  .composer-toolbar {
    grid-template-columns: 1fr !important;
    align-items: stretch !important;
  }

  .selected-actions {
    justify-content: flex-start;
  }

  .temperature-control {
    display: none !important;
  }
}

@media (prefers-reduced-motion: reduce) {
  .qa-scroll {
    scroll-behavior: auto;
  }
}
/* ai-cherry-studio-alignment:end */

/* ai-composer-reference-pass:start */
.qa-page {
  --qa-shell-width: min(1020px, 100%);
}

.qa-scroll,
.composer {
  padding-inline: clamp(22px, 3.8vw, 64px) !important;
}

.qa-scroll {
  scrollbar-gutter: stable;
}

.qa-scroll > .qa-hero,
.qa-scroll > .message-row,
.composer-shell {
  width: min(1020px, 100%) !important;
  margin-inline: auto !important;
}

.composer-shell {
  gap: 9px !important;
  padding: 12px !important;
  border-radius: 24px !important;
  border-color: color-mix(in srgb, var(--primary-color) 18%, var(--border-color)) !important;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.09), rgba(255, 255, 255, 0.025)),
    color-mix(in srgb, var(--panel-bg) 82%, transparent) !important;
}

.composer textarea {
  min-height: 56px !important;
  padding: 7px 8px 0 !important;
  font-size: 15px !important;
}

.composer-dock {
  min-height: 42px;
  display: flex !important;
  align-items: center !important;
  justify-content: space-between !important;
  gap: 10px !important;
}

.composer-left-tools,
.composer-right-tools {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.composer-right-tools {
  flex: 0 0 auto;
}

.composer-icon-action,
.composer-quick-action,
.reasoning-pill,
.composer-submit-round {
  transition:
    transform var(--motion-hover) var(--ease-liquid),
    background var(--motion-medium) var(--ease-smooth),
    border-color var(--motion-medium) var(--ease-smooth),
    box-shadow var(--motion-medium) var(--ease-smooth);
}

.composer-icon-action {
  position: relative;
  flex: 0 0 auto;
  width: 38px !important;
  height: 38px !important;
  display: grid;
  place-items: center;
  border: 1px solid color-mix(in srgb, var(--primary-color) 16%, var(--border-color));
  border-radius: 999px !important;
  background: color-mix(in srgb, var(--surface-2) 62%, transparent);
  color: var(--text-primary);
  cursor: pointer;
}

.composer-icon-action b {
  position: absolute;
  top: -5px;
  right: -4px;
  min-width: 17px;
  height: 17px;
  display: grid;
  place-items: center;
  border-radius: 999px;
  background: var(--primary-color);
  color: var(--primary-contrast, var(--text-inverse));
  font-size: 10px;
  font-weight: var(--font-weight-title);
}

.composer-icon-action:hover,
.composer-icon-action.active {
  border-color: color-mix(in srgb, var(--primary-color) 44%, var(--border-color));
  background: rgba(var(--primary-rgb), 0.12);
  color: var(--primary-color);
  transform: translateY(-1px);
}

.composer-quick-action {
  flex: 0 0 auto;
  min-height: 38px !important;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 0 15px !important;
  border: 1px solid color-mix(in srgb, var(--primary-color) 22%, var(--border-color));
  border-radius: 999px !important;
  background: rgba(var(--primary-rgb), 0.1);
  color: var(--primary-color);
  font-size: 13px;
  font-weight: var(--font-weight-title);
  white-space: nowrap;
  cursor: pointer;
}

.composer-quick-action.ready {
  background: rgba(var(--primary-rgb), 0.16);
  box-shadow: inset 0 0 0 1px rgba(var(--primary-rgb), 0.08);
}

.composer-quick-action:not(:disabled):hover {
  transform: translateY(-1px);
  box-shadow: var(--shadow-ring);
}

.composer-web-toggle {
  min-height: 38px !important;
  padding: 0 13px !important;
  border-radius: 999px !important;
  font-size: 13px !important;
}

.reasoning-menu {
  position: relative;
  flex: 0 0 auto;
}

.reasoning-pill {
  min-height: 38px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 0 11px 0 13px;
  border: 1px solid color-mix(in srgb, var(--primary-color) 16%, var(--border-color));
  border-radius: 999px;
  background: color-mix(in srgb, var(--surface-2) 64%, transparent);
  color: var(--text-primary);
  cursor: pointer;
}

.reasoning-pill span {
  color: var(--text-muted);
  font-size: 11px;
  font-weight: var(--font-weight-label);
}

.reasoning-pill strong {
  font-size: 13px;
  font-weight: var(--font-weight-title);
}

.reasoning-menu.open .reasoning-pill,
.reasoning-pill:hover {
  border-color: color-mix(in srgb, var(--primary-color) 44%, var(--border-color));
  background: rgba(var(--primary-rgb), 0.1);
  color: var(--primary-color);
  transform: translateY(-1px);
}

.reasoning-menu.open .reasoning-pill svg {
  transform: rotate(180deg);
}

.reasoning-popover {
  position: absolute;
  right: 0;
  bottom: calc(100% + 10px);
  z-index: 40;
  width: min(330px, calc(100vw - 40px));
  display: grid;
  gap: 5px;
  padding: 8px;
  border: 1px solid color-mix(in srgb, var(--primary-color) 18%, var(--border-color));
  border-radius: 18px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.09), rgba(255, 255, 255, 0.025)),
    color-mix(in srgb, var(--panel-bg) 92%, transparent);
  box-shadow:
    0 24px 70px rgba(0, 0, 0, 0.18),
    inset 0 1px 0 rgba(255, 255, 255, 0.09);
  -webkit-backdrop-filter: blur(var(--glass-blur)) saturate(var(--glass-saturate));
  backdrop-filter: blur(var(--glass-blur)) saturate(var(--glass-saturate));
}

.reasoning-popover button {
  width: 100% !important;
  min-height: 48px !important;
  display: grid;
  gap: 2px;
  padding: 8px 10px !important;
  border: 0;
  border-radius: 13px !important;
  background: transparent;
  color: var(--text-primary);
  text-align: left;
  cursor: pointer;
}

.reasoning-popover button span,
.reasoning-popover button em {
  display: block;
}

.reasoning-popover button span {
  font-size: 13px;
  font-weight: var(--font-weight-title);
}

.reasoning-popover button em {
  overflow: hidden;
  color: var(--text-muted);
  font-size: 11px;
  font-style: normal;
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.reasoning-popover button:hover,
.reasoning-popover button.active {
  background: rgba(var(--primary-rgb), 0.11);
  color: var(--primary-color);
}

.reasoning-temperature {
  min-height: 40px;
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 3px;
  padding: 6px 10px;
  border-top: 1px solid var(--qa-border);
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: var(--font-weight-title);
}

.reasoning-temperature input {
  flex: 1;
  min-width: 120px;
  accent-color: var(--primary-color);
}

.composer-submit-round {
  width: 40px !important;
  height: 40px !important;
  border-radius: 999px !important;
  border-color: transparent !important;
  background:
    radial-gradient(circle at 34% 24%, rgba(255, 255, 255, 0.28), transparent 32%),
    color-mix(in srgb, var(--text-primary) 92%, var(--primary-color)) !important;
  color: var(--bg-color) !important;
  box-shadow: 0 12px 26px rgba(0, 0, 0, 0.2) !important;
}

.composer-submit-round:not(:disabled):hover {
  transform: translateY(-1px) scale(1.02);
}

.composer-submit-round:disabled,
.composer-quick-action:disabled {
  opacity: 0.46 !important;
  cursor: not-allowed !important;
  transform: none !important;
  box-shadow: none !important;
}

.composer-submit-round.stop {
  background: var(--qa-danger-bg) !important;
  color: var(--text-primary) !important;
}

@media (max-width: 980px) {
  .qa-page {
    --qa-shell-width: min(100%, calc(100vw - 28px));
  }

  .qa-scroll,
  .composer {
    padding-inline: 14px !important;
  }
}

@media (max-width: 720px) {
  .composer-shell {
    border-radius: 20px !important;
  }

  .composer-dock {
    align-items: center !important;
    gap: 8px !important;
  }

  .composer-left-tools {
    flex: 1 1 auto;
    gap: 6px !important;
  }

  .composer-right-tools {
    gap: 6px;
  }

  .composer-web-toggle span,
  .reasoning-pill span {
    display: none;
  }

  .composer-quick-action {
    min-width: 0;
    padding-inline: 12px !important;
  }

  .composer-quick-action span {
    max-width: 5.5em;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .reasoning-popover {
    right: -48px;
    width: min(330px, calc(100vw - 24px));
  }
}
/* ai-composer-reference-pass:end */

/* ai-visible-composer-fix:start */
.qa-page {
  height: calc(100dvh - var(--header-height, 72px)) !important;
  min-height: 0 !important;
  max-height: calc(100dvh - var(--header-height, 72px)) !important;
  overflow: hidden !important;
}

.qa-sidebar {
  position: relative !important;
  top: auto !important;
  height: 100% !important;
  min-height: 0 !important;
  max-height: 100% !important;
  overflow: hidden !important;
}

.qa-main {
  height: 100% !important;
  min-height: 0 !important;
  max-height: 100% !important;
  grid-template-rows: 58px minmax(0, 1fr) auto !important;
}

.qa-scroll {
  min-height: 0 !important;
  overflow-y: auto !important;
  padding-bottom: 22px !important;
}

.composer {
  position: relative !important;
  bottom: auto !important;
  flex: 0 0 auto;
  padding-top: 8px !important;
  padding-bottom: max(12px, env(safe-area-inset-bottom)) !important;
}

.composer-shell {
  max-height: min(212px, 32dvh);
  overflow: visible;
}

.composer textarea {
  max-height: 94px !important;
}

@media (max-height: 760px) {
  .qa-topbar {
    min-height: 50px !important;
    padding-block: 6px !important;
  }

  .qa-main {
    grid-template-rows: 50px minmax(0, 1fr) auto !important;
  }

  .composer-shell {
    padding: 10px !important;
  }

  .composer textarea {
    min-height: 44px !important;
    max-height: 76px !important;
  }

  .composer-dock {
    min-height: 38px;
  }
}

@media (max-width: 980px) {
  .qa-page {
    height: auto !important;
    min-height: calc(100dvh - var(--header-height, 72px)) !important;
    max-height: none !important;
    overflow: visible !important;
  }

  .qa-sidebar {
    height: auto !important;
    max-height: none !important;
    overflow: visible !important;
  }
}
/* ai-visible-composer-fix:end */

/* ai-composer-codex-minimal:start */
.composer-shell {
  gap: 8px !important;
  padding: 10px !important;
  border-radius: 18px !important;
  border-color: color-mix(in srgb, var(--text-primary) 13%, transparent) !important;
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--panel-bg) 90%, transparent), color-mix(in srgb, var(--panel-bg) 78%, transparent)) !important;
  box-shadow:
    0 14px 44px rgba(0, 0, 0, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.06) !important;
}

.composer textarea {
  min-height: 52px !important;
  padding: 8px 10px 0 !important;
  border-radius: 12px !important;
  background: color-mix(in srgb, var(--surface-2) 42%, transparent) !important;
}

.composer-dock {
  min-height: 34px !important;
  gap: 8px !important;
}

.composer-left-tools,
.composer-right-tools {
  gap: 4px !important;
}

.composer-icon-action,
.composer-quick-action,
.composer-web-toggle,
.reasoning-pill,
.composer-submit-round {
  height: 34px !important;
  min-height: 34px !important;
  border-radius: 9px !important;
  border-color: transparent !important;
  background: transparent !important;
  color: var(--text-secondary) !important;
  box-shadow: none !important;
  transform: none !important;
}

.composer-icon-action {
  width: 34px !important;
}

.composer-icon-action b {
  top: 2px;
  right: 2px;
  min-width: 14px;
  height: 14px;
  border-radius: 5px;
  background: color-mix(in srgb, var(--primary-color) 20%, var(--surface-2));
  color: var(--primary-color);
  font-size: 9px;
}

.composer-quick-action,
.composer-web-toggle,
.reasoning-pill {
  padding-inline: 9px !important;
  font-size: 13px !important;
  font-weight: var(--font-weight-title);
}

.composer-quick-action {
  min-width: 86px;
  justify-content: center;
}

.composer-web-toggle {
  gap: 6px !important;
}

.reasoning-pill {
  gap: 5px;
}

.reasoning-pill span {
  display: none;
}

.reasoning-pill strong {
  color: inherit;
  font-size: 13px;
}

.composer-icon-action:hover,
.composer-icon-action.active,
.composer-quick-action:not(:disabled):hover,
.composer-quick-action.ready,
.composer-web-toggle:hover,
.composer-web-toggle.active,
.reasoning-menu.open .reasoning-pill,
.reasoning-pill:hover {
  border-color: color-mix(in srgb, var(--text-primary) 10%, transparent) !important;
  background: color-mix(in srgb, var(--surface-2) 64%, transparent) !important;
  color: var(--text-primary) !important;
  box-shadow: none !important;
  transform: none !important;
}

.composer-submit-round {
  width: 36px !important;
  height: 36px !important;
  border-radius: 10px !important;
  background: color-mix(in srgb, var(--text-primary) 82%, transparent) !important;
  color: var(--bg-color) !important;
}

.composer-submit-round:not(:disabled):hover {
  background: var(--text-primary) !important;
  transform: none !important;
}

.composer-submit-round:disabled {
  background: color-mix(in srgb, var(--text-muted) 42%, transparent) !important;
  color: color-mix(in srgb, var(--bg-color) 78%, transparent) !important;
}

.composer-submit-round.stop {
  background: color-mix(in srgb, var(--qa-danger-bg) 86%, var(--surface-2)) !important;
  color: var(--text-primary) !important;
}

.reasoning-popover {
  border-radius: 14px;
  box-shadow:
    0 18px 54px rgba(0, 0, 0, 0.18),
    inset 0 1px 0 rgba(255, 255, 255, 0.06);
}

.reasoning-popover button {
  min-height: 42px !important;
  border-radius: 9px !important;
}

@media (max-width: 720px) {
  .composer-quick-action {
    min-width: auto;
  }

  .composer-quick-action span {
    max-width: none;
  }
}
/* ai-composer-codex-minimal:end */

/* ai-composer-no-overlap:start */
.composer-dock {
  width: 100%;
  min-width: 0;
}

.composer-left-tools {
  flex: 1 1 auto;
  min-width: 0;
  overflow: hidden;
}

.composer-right-tools {
  flex: 0 0 auto;
  min-width: max-content;
}

.composer-icon-action,
.composer-quick-action,
.composer-web-toggle,
.reasoning-pill,
.composer-submit-round {
  flex: 0 0 auto;
  overflow: hidden;
  white-space: nowrap;
}

.composer-quick-action {
  max-width: 132px;
}

.composer-web-toggle {
  max-width: 104px;
}

.reasoning-pill {
  max-width: 92px;
}

.composer-quick-action span,
.composer-web-toggle span,
.reasoning-pill strong {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.composer-web-toggle svg,
.reasoning-pill svg,
.composer-submit-round svg {
  flex: 0 0 auto;
}

@media (max-width: 1180px) {
  .composer-web-toggle span {
    display: none;
  }

  .composer-web-toggle {
    width: 34px !important;
    padding-inline: 0 !important;
    justify-content: center;
  }
}

@media (max-width: 860px) {
  .composer-quick-action {
    width: 34px !important;
    min-width: 34px !important;
    padding-inline: 0 !important;
  }

  .composer-quick-action span {
    display: none;
  }

  .reasoning-pill {
    max-width: 76px;
    padding-inline: 8px !important;
  }
}

@media (max-width: 640px) {
  .reasoning-pill strong {
    display: none;
  }

  .reasoning-pill {
    width: 34px !important;
    padding-inline: 0 !important;
    justify-content: center;
  }
}
/* ai-composer-no-overlap:end */

/* ai-composer-horizontal-tools:start */
.composer-dock {
  flex-wrap: nowrap !important;
  align-items: center !important;
  gap: 18px !important;
}

.composer-left-tools,
.composer-right-tools {
  width: auto !important;
  flex-direction: row !important;
  flex-wrap: nowrap !important;
  align-items: center !important;
  gap: 14px !important;
  overflow: visible !important;
}

.composer-left-tools {
  flex: 1 1 auto;
}

.composer-right-tools {
  flex: 0 0 auto;
}

.composer-icon-action,
.composer-quick-action,
.composer-web-toggle,
.reasoning-pill {
  width: auto !important;
  max-width: none !important;
  min-width: 0 !important;
  display: inline-flex !important;
  flex-direction: row !important;
  align-items: center !important;
  justify-content: center !important;
  gap: 8px !important;
  padding: 0 !important;
  border: 0 !important;
  background: transparent !important;
  color: var(--text-secondary) !important;
  line-height: 1 !important;
}

.composer-icon-action {
  width: 28px !important;
  min-width: 28px !important;
}

.composer-quick-action,
.composer-web-toggle,
.reasoning-pill {
  min-height: 32px !important;
  height: 32px !important;
}

.composer-quick-action span,
.composer-web-toggle span,
.reasoning-pill strong {
  display: inline !important;
  max-width: none !important;
  overflow: visible !important;
  text-overflow: clip !important;
  white-space: nowrap !important;
  line-height: 1 !important;
}

.composer-web-toggle svg,
.reasoning-pill svg {
  width: 16px !important;
  height: 16px !important;
  flex: 0 0 16px !important;
}

.composer-icon-action:hover,
.composer-icon-action.active,
.composer-quick-action:not(:disabled):hover,
.composer-quick-action.ready,
.composer-web-toggle:hover,
.composer-web-toggle.active,
.reasoning-menu.open .reasoning-pill,
.reasoning-pill:hover {
  background: transparent !important;
  color: var(--text-primary) !important;
}

.composer-submit-round {
  flex: 0 0 38px !important;
  width: 38px !important;
  min-width: 38px !important;
  height: 38px !important;
  display: inline-flex !important;
  align-items: center !important;
  justify-content: center !important;
}

@media (max-width: 760px) {
  .composer-left-tools,
  .composer-right-tools {
    gap: 10px !important;
  }

  .composer-web-toggle span,
  .composer-quick-action span {
    display: none !important;
  }
}

@media (max-width: 560px) {
  .reasoning-pill strong {
    display: none !important;
  }
}
/* ai-composer-horizontal-tools:end */

/* ai-resource-library-panel:start */
.material-panel {
  right: auto !important;
  width: min(860px, 100%) !important;
  max-height: min(560px, 62dvh) !important;
  padding: 16px !important;
  border: 1px solid color-mix(in srgb, var(--primary-color) 18%, var(--border-color)) !important;
  border-radius: 18px !important;
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--surface-2) 16%, var(--bg-color)), var(--bg-color)) !important;
  box-shadow:
    0 26px 84px rgba(0, 0, 0, 0.32),
    inset 0 1px 0 color-mix(in srgb, #fff 8%, transparent) !important;
  -webkit-backdrop-filter: none !important;
  backdrop-filter: none !important;
}

.material-panel-head {
  align-items: flex-start !important;
  padding: 0 !important;
}

.material-panel-head strong {
  font-size: 15px !important;
}

.material-panel-head span {
  max-width: 62ch !important;
  color: var(--text-secondary) !important;
}

.material-search {
  min-height: 42px;
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 0 12px;
  border: 1px solid color-mix(in srgb, var(--text-primary) 10%, transparent);
  border-radius: 12px;
  background: color-mix(in srgb, var(--surface-2) 72%, var(--bg-color));
  color: var(--text-muted);
}

.material-search input {
  width: 100%;
  min-width: 0;
  border: 0;
  outline: 0;
  background: transparent;
  color: var(--text-primary);
  font: inherit;
  font-size: 13px;
}

.material-search input::placeholder {
  color: var(--text-muted);
}

.material-picker {
  display: grid !important;
  grid-template-columns: 178px minmax(0, 1fr);
  grid-template-areas:
    "summary summary"
    "tabs list";
  gap: 12px;
  min-height: 0;
}

.material-library-summary {
  grid-area: summary;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: var(--text-muted);
  font-size: 12px;
}

.material-library-summary b {
  color: var(--text-primary);
  font-weight: var(--font-weight-title);
}

.material-tabs {
  grid-area: tabs;
  grid-template-columns: 1fr !important;
  align-content: start;
  gap: 4px !important;
  padding: 4px !important;
  border-radius: 12px !important;
  background: color-mix(in srgb, var(--surface-2) 62%, var(--bg-color)) !important;
}

.material-tabs button {
  min-height: 36px !important;
  justify-content: space-between !important;
  padding: 0 10px !important;
  border-radius: 9px !important;
  text-align: left;
}

.material-list,
.material-empty-inline {
  grid-area: list;
}

.material-list {
  max-height: min(372px, 42dvh) !important;
  display: grid;
  gap: 8px;
  padding-right: 4px;
}

.material-row {
  min-height: 62px !important;
  grid-template-columns: 24px minmax(0, 1fr) auto !important;
  gap: 12px !important;
  padding: 10px 12px !important;
  border: 1px solid color-mix(in srgb, var(--text-primary) 9%, transparent) !important;
  border-radius: 12px !important;
  background: color-mix(in srgb, var(--surface-2) 48%, var(--bg-color)) !important;
}

.material-row:hover,
.material-row.active {
  border-color: color-mix(in srgb, var(--primary-color) 42%, var(--border-color)) !important;
  background: color-mix(in srgb, var(--primary-color) 9%, var(--surface-2)) !important;
}

.material-row-copy strong {
  max-width: 100%;
  color: var(--text-primary) !important;
}

.material-row-copy em {
  max-width: 100%;
  color: var(--text-muted) !important;
}

.material-row .material-badge {
  white-space: nowrap;
}

@media (max-width: 860px) {
  .material-panel {
    width: 100% !important;
  }

  .material-picker {
    grid-template-columns: 1fr;
    grid-template-areas:
      "summary"
      "tabs"
      "list";
  }

  .material-tabs {
    grid-template-columns: repeat(2, minmax(0, 1fr)) !important;
  }
}
/* ai-resource-library-panel:end */

/* ai-chat-polish-restore:start */
.qa-page {
  --ai-chat-shell-border: color-mix(in srgb, var(--primary-color) 16%, var(--border-color));
  --ai-chat-glass-panel:
    linear-gradient(180deg, color-mix(in srgb, #fff 7%, transparent), color-mix(in srgb, #fff 2%, transparent)),
    color-mix(in srgb, var(--panel-bg) 82%, transparent);
  grid-template-columns: 282px minmax(0, 1fr) !important;
  gap: 14px !important;
  padding: 14px 16px 16px !important;
  background: transparent !important;
  overflow: hidden !important;
}

.qa-sidebar {
  height: auto !important;
  min-height: 0 !important;
  margin: 0 !important;
  overflow: hidden !important;
  border: 1px solid var(--ai-chat-shell-border) !important;
  border-radius: 24px !important;
  background: var(--ai-chat-glass-panel) !important;
  box-shadow:
    0 22px 68px color-mix(in srgb, #000 18%, transparent),
    inset 0 1px 0 color-mix(in srgb, #fff 10%, transparent) !important;
  -webkit-backdrop-filter: blur(var(--glass-blur)) saturate(var(--glass-saturate));
  backdrop-filter: blur(var(--glass-blur)) saturate(var(--glass-saturate));
}

.qa-main {
  min-width: 0 !important;
  margin: 0 !important;
  overflow: hidden !important;
  border: 1px solid var(--ai-chat-shell-border) !important;
  border-radius: 24px !important;
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--surface-2) 18%, transparent), transparent 34%),
    color-mix(in srgb, var(--panel-bg) 54%, transparent) !important;
  box-shadow:
    0 26px 80px color-mix(in srgb, #000 22%, transparent),
    inset 0 1px 0 color-mix(in srgb, #fff 10%, transparent) !important;
  -webkit-backdrop-filter: blur(var(--glass-blur)) saturate(var(--glass-saturate));
  backdrop-filter: blur(var(--glass-blur)) saturate(var(--glass-saturate));
}

.qa-topbar {
  margin: 0 !important;
  padding: 24px clamp(24px, 3vw, 42px) 16px !important;
  border-bottom: 1px solid color-mix(in srgb, var(--text-primary) 8%, transparent) !important;
  background: transparent !important;
}

.qa-scroll {
  padding: 26px clamp(24px, 4vw, 72px) 132px !important;
}

.qa-scroll > .qa-hero,
.qa-scroll > .message-row,
.composer-shell {
  width: min(1120px, 100%) !important;
}

.message-row {
  grid-template-columns: 34px minmax(0, 1fr) !important;
  gap: 12px !important;
  align-items: start !important;
}

.message-row.user {
  grid-template-columns: minmax(0, 1fr) !important;
  justify-items: end !important;
}

.message-row.user .avatar {
  display: none !important;
}

.message-row.user .message-body {
  grid-column: 1 !important;
  width: fit-content !important;
  max-width: min(560px, 72%) !important;
  justify-self: end !important;
  display: grid !important;
  justify-items: end !important;
}

.message-row.assistant .message-body {
  max-width: min(780px, 86%) !important;
}

.message-row.assistant .bubble {
  width: 100% !important;
  padding: 18px 20px !important;
  overflow: auto !important;
  border: 1px solid color-mix(in srgb, var(--text-primary) 9%, transparent) !important;
  border-radius: 22px 22px 22px 8px !important;
  background:
    linear-gradient(180deg, color-mix(in srgb, #fff 5%, transparent), transparent 42%),
    color-mix(in srgb, var(--panel-bg) 44%, transparent) !important;
  color: var(--text-primary) !important;
  box-shadow:
    0 14px 46px color-mix(in srgb, #000 14%, transparent),
    inset 0 1px 0 color-mix(in srgb, #fff 8%, transparent) !important;
  font-size: 15px !important;
  font-weight: 480 !important;
  line-height: 1.74 !important;
}

.message-row.assistant .bubble :deep(h1),
.message-row.assistant .bubble :deep(h2),
.message-row.assistant .bubble :deep(h3),
.message-row.assistant .bubble :deep(h4) {
  margin: 18px 0 8px !important;
  color: var(--text-primary) !important;
  font-weight: 760 !important;
  line-height: 1.28 !important;
  letter-spacing: 0 !important;
}

.message-row.assistant .bubble :deep(h1) {
  font-size: 20px !important;
}

.message-row.assistant .bubble :deep(h2) {
  font-size: 18px !important;
}

.message-row.assistant .bubble :deep(h3) {
  font-size: 16px !important;
}

.message-row.assistant .bubble :deep(p) {
  margin: 0 0 11px !important;
  color: color-mix(in srgb, var(--text-primary) 88%, transparent) !important;
}

.message-row.assistant .bubble :deep(hr) {
  height: 1px !important;
  margin: 14px 0 !important;
  border: 0 !important;
  background: color-mix(in srgb, var(--text-primary) 11%, transparent) !important;
}

.message-row.assistant .bubble :deep(table) {
  width: 100% !important;
  min-width: 560px !important;
  margin: 12px 0 14px !important;
  border-collapse: separate !important;
  border-spacing: 0 !important;
  overflow: hidden !important;
  border: 1px solid color-mix(in srgb, var(--text-primary) 10%, transparent) !important;
  border-radius: 14px !important;
  background: color-mix(in srgb, var(--surface-2) 30%, transparent) !important;
}

.message-row.assistant .bubble :deep(th),
.message-row.assistant .bubble :deep(td) {
  padding: 10px 12px !important;
  border-bottom: 1px solid color-mix(in srgb, var(--text-primary) 8%, transparent) !important;
  color: color-mix(in srgb, var(--text-primary) 88%, transparent) !important;
  font-size: 13px !important;
  line-height: 1.45 !important;
  text-align: left !important;
  vertical-align: top !important;
}

.message-row.assistant .bubble :deep(th) {
  color: var(--text-primary) !important;
  font-weight: 760 !important;
  background: color-mix(in srgb, var(--primary-color) 8%, transparent) !important;
}

.message-row.assistant .bubble :deep(tr:last-child td) {
  border-bottom: 0 !important;
}

.message-row.assistant .bubble :deep(ul),
.message-row.assistant .bubble :deep(ol) {
  margin: 9px 0 12px !important;
  padding-left: 20px !important;
}

.message-row.assistant .bubble :deep(li) {
  margin: 5px 0 !important;
  padding-left: 2px !important;
  color: color-mix(in srgb, var(--text-primary) 88%, transparent) !important;
}

.message-row.assistant .bubble :deep(strong) {
  color: var(--text-primary) !important;
  font-weight: 760 !important;
}

.message-row.user .bubble {
  width: fit-content !important;
  max-width: 100% !important;
  padding: 15px 18px !important;
  border: 1px solid color-mix(in srgb, var(--primary-color) 34%, var(--border-color)) !important;
  border-radius: 20px 20px 7px 20px !important;
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--primary-color) 18%, transparent), color-mix(in srgb, var(--surface-2) 58%, transparent)),
    color-mix(in srgb, var(--panel-bg) 78%, transparent) !important;
  color: var(--text-primary) !important;
  box-shadow:
    0 14px 38px color-mix(in srgb, #000 15%, transparent),
    inset 0 1px 0 color-mix(in srgb, #fff 12%, transparent) !important;
  text-align: left !important;
}

.message-row.user .bubble :deep(p) {
  margin: 0 !important;
}

.message-control-strip.user {
  display: none !important;
}

.message-control-strip.assistant {
  width: fit-content !important;
  max-width: min(100%, 760px) !important;
  margin: 8px 0 10px !important;
}

.message-control-strip.assistant span {
  display: none !important;
}

.message-control-strip.assistant b {
  min-height: 24px !important;
  padding: 0 9px !important;
  border-radius: 999px !important;
  font-size: 11px !important;
}

.user-message-tools {
  width: auto !important;
  margin-top: 10px !important;
  justify-content: flex-end !important;
}

.user-message-tools button,
.message-meta button {
  width: 30px !important;
  height: 30px !important;
  border-radius: 10px !important;
}

.thinking-card,
.assistant-run-status,
.diagnostics-card,
.navigation-card,
.related-card {
  border: 1px solid color-mix(in srgb, var(--text-primary) 10%, transparent) !important;
  background: var(--ai-chat-glass-panel) !important;
  box-shadow:
    0 16px 52px color-mix(in srgb, #000 16%, transparent),
    inset 0 1px 0 color-mix(in srgb, #fff 8%, transparent) !important;
}

.navigation-card {
  padding: 18px 20px !important;
  border-radius: 22px !important;
}

.navigation-toggle {
  align-items: flex-start !important;
}

.navigation-toggle b,
.related-head b {
  color: color-mix(in srgb, var(--text-secondary) 76%, transparent) !important;
  font-size: 12px !important;
  font-weight: 680 !important;
}

.navigation-card span,
.related-head span,
.message-diagnostics-toggle span,
.resource-card-title,
.resource-card-title b,
.message-card-toggle b,
.message-diagnostics-toggle b {
  color: color-mix(in srgb, var(--primary-color) 74%, var(--text-primary)) !important;
}

.navigation-card strong {
  margin-top: 4px !important;
  color: var(--text-primary) !important;
  font-size: 18px !important;
  line-height: 1.35 !important;
}

.navigation-body {
  grid-template-columns: minmax(0, 1fr) auto !important;
  gap: 18px !important;
  margin-top: 16px !important;
  padding-top: 16px !important;
  border-top: 1px solid color-mix(in srgb, var(--text-primary) 9%, transparent) !important;
}

.navigation-card em {
  color: color-mix(in srgb, var(--text-primary) 72%, transparent) !important;
  font-size: 14px !important;
  line-height: 1.65 !important;
}

.navigation-card small {
  color: color-mix(in srgb, var(--text-primary) 52%, transparent) !important;
  font-size: 12px !important;
}

.navigation-goto {
  min-width: 82px !important;
  height: 40px !important;
  display: inline-flex !important;
  align-items: center !important;
  justify-content: center !important;
  padding: 0 16px !important;
  border: 1px solid color-mix(in srgb, var(--primary-color) 30%, var(--border-color)) !important;
  border-radius: 14px !important;
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--primary-color) 18%, transparent), color-mix(in srgb, var(--surface-2) 48%, transparent)),
    color-mix(in srgb, var(--panel-bg) 72%, transparent) !important;
  color: var(--text-primary) !important;
  box-shadow:
    0 12px 28px color-mix(in srgb, #000 14%, transparent),
    inset 0 1px 0 color-mix(in srgb, #fff 12%, transparent) !important;
  font-size: 14px !important;
  font-weight: 720 !important;
  transition:
    transform 180ms var(--ease-smooth),
    border-color 180ms ease,
    background 180ms ease,
    box-shadow 180ms ease !important;
}

.navigation-goto:hover {
  transform: translateY(-1px) !important;
  border-color: color-mix(in srgb, var(--primary-color) 46%, var(--border-color)) !important;
  background: color-mix(in srgb, var(--primary-color) 13%, var(--surface-2)) !important;
}

.navigation-goto:active {
  transform: translateY(0) scale(0.985) !important;
}

.composer {
  padding: 12px clamp(24px, 4vw, 72px) 20px !important;
  background:
    linear-gradient(180deg, transparent, color-mix(in srgb, var(--bg-color) 52%, transparent) 22%, color-mix(in srgb, var(--bg-color) 88%, transparent) 78%) !important;
}

.composer-shell {
  border: 1px solid var(--ai-chat-shell-border) !important;
  border-radius: 24px !important;
  background: var(--ai-chat-glass-panel) !important;
  box-shadow:
    0 22px 68px color-mix(in srgb, #000 24%, transparent),
    inset 0 1px 0 color-mix(in srgb, #fff 10%, transparent) !important;
}

.composer textarea {
  min-height: 68px !important;
  padding: 10px 12px 2px !important;
  background: transparent !important;
}

.composer-dock {
  gap: 12px !important;
}

.composer-left-tools,
.composer-right-tools {
  gap: 10px !important;
}

.composer-quick-action {
  display: none !important;
}

.composer-icon-action,
.composer-web-toggle,
.reasoning-pill {
  min-height: 34px !important;
  height: 34px !important;
  padding: 0 9px !important;
  border: 1px solid color-mix(in srgb, var(--text-primary) 9%, transparent) !important;
  border-radius: 12px !important;
  background: color-mix(in srgb, var(--surface-2) 40%, transparent) !important;
  color: var(--text-secondary) !important;
}

.composer-icon-action {
  width: 34px !important;
  min-width: 34px !important;
  padding: 0 !important;
}

.composer-web-toggle span {
  max-width: 4em !important;
  overflow: hidden !important;
  text-overflow: ellipsis !important;
}

.reasoning-pill {
  max-width: 82px !important;
  gap: 5px !important;
}

.reasoning-pill span {
  display: none !important;
}

.reasoning-pill strong {
  max-width: 3.2em !important;
  overflow: hidden !important;
  text-overflow: ellipsis !important;
  white-space: nowrap !important;
}

.composer-icon-action:hover,
.composer-icon-action.active,
.composer-web-toggle:hover,
.composer-web-toggle.active,
.reasoning-menu.open .reasoning-pill,
.reasoning-pill:hover {
  border-color: color-mix(in srgb, var(--primary-color) 38%, var(--border-color)) !important;
  background: color-mix(in srgb, var(--primary-color) 11%, var(--surface-2)) !important;
  color: var(--text-primary) !important;
}

.reasoning-popover {
  width: min(218px, calc(100vw - 36px)) !important;
  gap: 3px !important;
  padding: 7px !important;
  border-radius: 16px !important;
}

.reasoning-popover button {
  min-height: 34px !important;
  grid-template-columns: 1fr auto;
  align-items: center;
  padding: 0 10px !important;
  border-radius: 10px !important;
}

.reasoning-popover button em {
  display: none !important;
}

.reasoning-temperature {
  min-height: 34px !important;
  margin-top: 4px !important;
  padding: 7px 9px 4px !important;
  gap: 8px !important;
  font-size: 11px !important;
}

.reasoning-temperature input {
  min-width: 86px !important;
}

.composer-effect-line {
  display: none !important;
}

.composer-submit-round {
  flex: 0 0 38px !important;
  width: 38px !important;
  min-width: 38px !important;
  height: 38px !important;
  border-radius: 13px !important;
}

@media (max-width: 980px) {
  .qa-page {
    padding: 10px !important;
    gap: 10px !important;
  }

  .qa-sidebar {
    border-radius: 20px !important;
  }

  .qa-main {
    margin: 0 !important;
    border-radius: 20px !important;
  }

  .message-row.user .message-body {
    max-width: min(620px, 86%) !important;
  }
}

@media (max-width: 640px) {
  .qa-scroll {
    padding-inline: 14px !important;
  }

  .message-row.user .message-body,
  .message-row.assistant .message-body {
    max-width: 100% !important;
  }

  .composer-web-toggle span {
    display: none !important;
  }
}
/* ai-chat-polish-restore:end */
</style>
