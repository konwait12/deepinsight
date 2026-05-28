<template>
  <div
    ref="assistantRef"
    class="assistant-container"
    :class="{ expanded: isExpanded, dragging: isDragging }"
    :style="{ left: x + 'px', top: y + 'px' }"
  >
    <button
      v-if="!isExpanded"
      type="button"
      class="assistant-orb glass-panel"
      aria-label="打开 AI 助手"
      title="打开 AI 助手"
      @mousedown="startDragCollapsed"
    >
      <BotMessageSquare :size="27" stroke-width="2.3" />
      <span>AI</span>
    </button>

    <section v-else class="assistant-panel glass-panel-heavy" aria-label="DeepInsight AI assistant">
      <header class="assistant-header" @mousedown="startDragExpanded">
        <div class="assistant-title">
          <span><Bot :size="17" /></span>
          <div>
            <h3>DeepInsight AI</h3>
            <p>{{ loading ? `${activeReasoningLabel} 推理中` : '就绪，支持上下文对话' }}</p>
          </div>
        </div>
        <div class="assistant-actions">
          <button type="button" title="清空" @click.stop="clearChat"><Trash2 :size="15" /></button>
          <button type="button" title="打开完整分析对话" @click.stop="openFullChat"><Maximize2 :size="15" /></button>
          <button type="button" title="收起" @click.stop="toggleExpand"><Minus :size="16" /></button>
        </div>
      </header>

      <div ref="msgContainer" class="assistant-messages">
        <article v-for="(msg, index) in messages" :key="index" class="message-row" :class="msg.role">
          <span v-if="msg.role !== 'user'" class="message-avatar"><Bot :size="14" /></span>
          <div class="message-stack">
            <button
              v-if="msg.thinking"
              type="button"
              class="assistant-thinking"
              @click="msg.thinkingExpanded = !msg.thinkingExpanded"
            >
              {{ msg.thinkingExpanded ? '收起推理' : '查看推理' }}
              <pre v-show="msg.thinkingExpanded">{{ msg.thinking }}</pre>
            </button>
            <div class="message-bubble">{{ msg.content }}</div>
            <time>{{ msg.time }}</time>
          </div>
          <span v-if="msg.role === 'user'" class="message-avatar user-avatar"><UserRound :size="14" /></span>
        </article>

        <article v-if="loading" class="message-row assistant">
          <span class="message-avatar"><Bot :size="14" /></span>
          <div class="typing-bubble"><i></i><i></i><i></i></div>
        </article>
      </div>

      <div class="quick-chips">
        <button v-for="chip in quickChips" :key="chip.label" type="button" @click="askAI(chip.query)">
          {{ chip.label }}
        </button>
      </div>

      <form class="assistant-input" @submit.prevent="sendMessage">
        <div class="assistant-input-shell">
          <textarea
            ref="inputRef"
            v-model="userInput"
            :placeholder="loading ? 'AI 正在分析...' : '输入问题，Enter 发送'"
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

            <label class="assistant-temperature">
              <span>温度</span>
              <input v-model.number="temperature" type="range" min="0" max="1" step="0.05" />
            </label>
          </div>
        </div>
        <button v-if="!loading" class="assistant-send" type="submit" :disabled="!userInput.trim()" title="发送">
          <SendHorizontal :size="17" />
        </button>
        <button v-else class="assistant-send" type="button" title="停止" @click="stopGeneration">
          <Square :size="15" />
        </button>
      </form>
      <p class="assistant-note">对话存入 Redis；深度思考会真实传给模型 API。</p>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { aiApi } from '@/api'
import { STORAGE_KEYS } from '@/constants'
import { ElMessage } from 'element-plus'
import {
  Bot,
  BotMessageSquare,
  Brain,
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
}

const router = useRouter()
const assistantRef = ref<HTMLElement | null>(null)
const msgContainer = ref<HTMLElement | null>(null)
const inputRef = ref<HTMLTextAreaElement | null>(null)
const x = ref(window.innerWidth - 84)
const y = ref(window.innerHeight - 92)
const isExpanded = ref(false)
const isDragging = ref(false)
const loading = ref(false)
const userInput = ref('')
const messages = ref<AssistantMessage[]>([])
const chatHistory = ref<Array<{ role: string; content: string }>>([])
const conversationId = ref<number | null>(null)
const reasoningLevel = ref<ReasoningLevel>('low')
const temperature = ref(0.7)
const reasoningMenuOpen = ref(false)
const reasoningMenuRef = ref<HTMLElement | null>(null)

const reasoningLevels: Array<{ value: ReasoningLevel; label: string; hint: string; desc: string }> = [
  { value: 'off', label: '直答', hint: 'off', desc: '不启用模型推理，适合轻量问答。' },
  { value: 'quick', label: '快速', hint: 'minimal', desc: '低延迟思考，适合简单修改。' },
  { value: 'low', label: '标准', hint: 'low', desc: '平衡速度与分析质量。' },
  { value: 'deep', label: '深度', hint: 'high', desc: '适合训练诊断和复杂复盘。' },
  { value: 'max', label: '极限', hint: 'xhigh', desc: '更高推理预算，适合多步分析。' },
]

const quickChips = [
  { label: '训练诊断', query: '请帮我诊断训练损失不稳定的常见原因，按优先级输出。' },
  { label: '模型建议', query: '请根据当前任务给出模型选择和调参建议。' },
  { label: '实验计划', query: '请帮我拆一个下一轮实验计划。' },
]

const deepThink = computed(() => reasoningLevel.value !== 'off')
const activeReasoningLabel = computed(() => reasoningLevels.find((level) => level.value === reasoningLevel.value)?.label || '标准')

let dragStartX = 0
let dragStartY = 0
let startLeft = 0
let startTop = 0
let hasMoved = false

function getTime() {
  const d = new Date()
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

function clampPosition() {
  const width = isExpanded.value ? Math.min(420, window.innerWidth - 20) : 56
  const height = isExpanded.value ? Math.min(580, window.innerHeight - 20) : 56
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
  if (!hasMoved) toggleExpand()
}

function stopDragExpanded() {
  window.removeEventListener('mousemove', onDrag)
  window.removeEventListener('mouseup', stopDragExpanded)
  isDragging.value = false
}

function toggleExpand() {
  isExpanded.value = !isExpanded.value
  clampPosition()
  if (isExpanded.value && messages.value.length === 0) {
    messages.value.push({
      role: 'assistant',
      content: '你好，我是 DeepInsight AI。可以帮你诊断训练状态、整理实验计划，也能打开完整分析对话继续复盘。',
      time: getTime(),
    })
  }
  nextTick(() => {
    inputRef.value?.focus()
    scrollToBottom()
  })
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
  loading.value = true
  await scrollToBottom()

  try {
    const response = await aiApi.chat({
      message,
      history: chatHistory.value.slice(-20),
      temperature: temperature.value,
      deepThink: deepThink.value,
      reasoningLevel: reasoningLevel.value,
      ...(conversationId.value ? { conversationId: conversationId.value } : {}),
    })
    const data = response.data.data
    if (data?.conversationId) conversationId.value = data.conversationId
    const reply = data?.reply || 'AI 服务暂未返回内容。'
    messages.value.push({
      role: 'assistant',
      content: reply,
      time: getTime(),
      thinking: data?.reasoning || '',
      thinkingExpanded: Boolean(data?.reasoning),
    })
    chatHistory.value.push({ role: 'assistant', content: reply })
  } catch (error: any) {
    messages.value.push({
      role: 'assistant',
      content: `AI 服务暂时不可用：${error?.response?.data?.message || error?.message || '未知错误'}`,
      time: getTime(),
    })
  } finally {
    loading.value = false
    await scrollToBottom()
  }
}

function stopGeneration() {
  loading.value = false
  ElMessage.info('已停止生成')
}

function askAI(query: string) {
  userInput.value = query
  void sendMessage()
}

function clearChat() {
  messages.value = []
  chatHistory.value = []
  conversationId.value = null
  ElMessage.success('对话已清空')
}

function openFullChat() {
  isExpanded.value = false
  router.push(conversationId.value ? `/ai?conversationId=${conversationId.value}` : '/ai')
}

function handleResize() {
  clampPosition()
}

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
  document.addEventListener('click', closeReasoningMenuOnOutside)
})

onUnmounted(() => {
  localStorage.setItem(STORAGE_KEYS.ASSISTANT_POS, JSON.stringify({ x: x.value, y: y.value }))
  window.removeEventListener('resize', handleResize)
  document.removeEventListener('click', closeReasoningMenuOnOutside)
  window.removeEventListener('mousemove', onDrag)
  window.removeEventListener('mouseup', stopDragCollapsed)
  window.removeEventListener('mouseup', stopDragExpanded)
})
</script>

<style scoped>
.assistant-container {
  position: fixed;
  z-index: 999;
  width: 56px;
  height: 56px;
  user-select: none;
  transition:
    width 260ms cubic-bezier(0.22, 1, 0.36, 1),
    height 260ms cubic-bezier(0.22, 1, 0.36, 1),
    filter 220ms ease;
}

.assistant-container.expanded {
  width: min(420px, calc(100vw - 20px));
  height: min(580px, calc(100vh - 20px));
}

.assistant-orb {
  position: relative;
  width: 56px;
  height: 56px;
  display: grid;
  place-items: center;
  border-radius: 18px !important;
  color: var(--primary-color);
  cursor: grab;
  overflow: hidden;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.14), rgba(255, 255, 255, 0.035) 42%, rgba(0, 0, 0, 0.18)),
    var(--workbench-shell-bg);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.18),
    0 16px 42px rgba(0, 0, 0, 0.24),
    var(--workbench-shadow);
  animation: assistantOrbBreath 4.6s ease-in-out infinite;
  transition:
    transform 260ms var(--ease-smooth),
    border-color 220ms ease,
    box-shadow 260ms var(--ease-smooth),
    background 260ms ease;
}

.assistant-orb span {
  position: absolute;
  left: 7px;
  bottom: 7px;
  padding: 1px 5px;
  border-radius: 6px;
  background: var(--workbench-panel-bg-strong);
  color: var(--primary-color);
  font-size: 9px;
  font-weight: var(--font-weight-title);
  letter-spacing: -0.02em;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.12);
}

.assistant-orb svg,
.assistant-actions button svg,
.assistant-send svg,
.assistant-reasoning-trigger svg {
  transform-origin: center;
  transition: transform 260ms var(--ease-smooth), filter 260ms ease, color 180ms ease;
}

.assistant-orb:hover {
  transform: translateY(-2px) scale(1.024);
  border-color: rgba(255, 255, 255, 0.24);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.22),
    0 18px 48px rgba(0, 0, 0, 0.3),
    0 0 0 1px rgba(var(--primary-rgb), 0.1),
    var(--workbench-shadow);
}

.assistant-orb:hover svg,
.assistant-actions button:hover svg,
.assistant-send:hover svg,
.assistant-reasoning-trigger:hover svg {
  animation: assistantIconKinetic 920ms var(--ease-smooth);
  filter: drop-shadow(0 0 7px rgba(255, 255, 255, 0.14));
}

.assistant-orb:hover :deep(svg[fill="none"] :where(path, line, polyline, polygon, circle, rect)),
.assistant-actions button:hover :deep(svg[fill="none"] :where(path, line, polyline, polygon, circle, rect)),
.assistant-send:hover :deep(svg[fill="none"] :where(path, line, polyline, polygon, circle, rect)),
.assistant-reasoning-trigger:hover :deep(svg[fill="none"] :where(path, line, polyline, polygon, circle, rect)) {
  animation: assistantIconLineFlow 1180ms var(--ease-smooth);
  filter: drop-shadow(0 0 5px rgba(255, 255, 255, 0.14));
}

.assistant-panel {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border-radius: var(--radius-sm) !important;
}

.assistant-header {
  height: 62px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 0 14px 0 16px;
  border-bottom: 1px solid var(--border-color);
  cursor: grab;
}

.assistant-title {
  display: flex;
  align-items: center;
  gap: 11px;
}

.assistant-title > span,
.message-avatar {
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  border-radius: 11px;
  background: rgba(var(--primary-rgb), 0.13);
  color: var(--primary-color);
}

.assistant-title h3 {
  margin: 0;
  color: var(--text-primary);
  font-size: 13px;
  font-weight: var(--font-weight-title);
}

.assistant-title p {
  margin: 2px 0 0;
  color: var(--text-muted);
  font-size: 11px;
  font-weight: var(--font-weight-body);
}

.assistant-actions,
.quick-chips {
  display: flex;
  gap: 6px;
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
  height: 30px;
  display: grid;
  place-items: center;
  border-radius: 11px;
  background: transparent;
  color: var(--text-secondary);
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

.quick-chips button {
  padding: 6px 9px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.06);
  color: var(--text-secondary);
  font-size: 10px;
  font-weight: var(--font-weight-title);
}

.assistant-messages {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 15px;
  display: flex;
  flex-direction: column;
  gap: 13px;
  background-image: radial-gradient(rgba(255, 255, 255, 0.045) 1px, transparent 1px);
  background-size: 18px 18px;
}

.message-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.message-row.user {
  justify-content: flex-end;
}

.user-avatar {
  background: var(--workbench-panel-bg);
  color: var(--text-secondary);
}

.message-stack {
  max-width: 78%;
}

.message-bubble,
.typing-bubble {
  padding: 10px 13px;
  border: 1px solid var(--border-color);
  border-radius: 15px;
  background: var(--workbench-panel-bg-strong);
  color: var(--text-primary);
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
}

.message-row.user .message-bubble {
  border-color: rgba(255, 255, 255, 0.28);
  background: var(--primary-color);
  color: #06100c;
}

.message-stack time {
  display: block;
  margin-top: 4px;
  color: var(--text-muted);
  font-size: 10px;
}

.assistant-thinking {
  width: 100%;
  margin-bottom: 6px;
  padding: 8px 10px;
  border-radius: 12px;
  text-align: left;
  background: rgba(var(--primary-rgb), 0.1);
  color: var(--primary-color);
  font-size: 11px;
  font-weight: var(--font-weight-title);
}

.assistant-thinking pre {
  margin: 8px 0 0;
  white-space: pre-wrap;
  color: var(--text-secondary);
  font-size: 11px;
  line-height: 1.55;
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
  flex-wrap: wrap;
  padding: 10px 14px;
  border-top: 1px solid var(--border-color);
}

.quick-chips button {
  color: var(--primary-color);
}

.assistant-input {
  display: grid;
  grid-template-columns: 1fr 40px;
  align-items: end;
  gap: 8px;
  padding: 12px 14px 8px;
  border-top: 1px solid var(--border-color);
  overflow: visible;
}

.assistant-input-shell {
  position: relative;
  display: grid;
  min-width: 0;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 17px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.06), transparent 42%),
    var(--bg-input);
  transition: border-color 160ms ease, box-shadow 160ms ease;
}

.assistant-input-shell:focus-within {
  border-color: rgba(255, 255, 255, 0.24);
  box-shadow: 0 0 0 3px rgba(var(--primary-rgb), 0.075);
}

.assistant-input textarea {
  width: 100%;
  max-height: 96px;
  resize: none;
  border: 0;
  border-radius: 17px 17px 10px 10px;
  padding: 10px 12px 0;
  outline: none;
  background: transparent;
  color: var(--text-primary);
  font-size: 13px;
}

.assistant-input textarea:focus {
  box-shadow: none;
}

.assistant-input-tools {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  padding: 0 7px 7px;
}

.assistant-reasoning {
  position: relative;
}

.assistant-reasoning-trigger,
.assistant-temperature {
  height: 27px;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  border: 1px solid rgba(var(--primary-rgb), 0.18) !important;
  border-radius: 999px !important;
  background: var(--workbench-control-bg) !important;
  color: var(--text-secondary) !important;
  box-shadow: none !important;
  font-size: 10px;
  font-weight: var(--font-weight-title);
}

.assistant-reasoning-trigger {
  padding: 0 9px;
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
  border-color: rgba(255, 255, 255, 0.24) !important;
  color: var(--text-primary) !important;
}

.assistant-reasoning-trigger:hover {
  transform: translateY(-0.5px);
  border-color: rgba(255, 255, 255, 0.24) !important;
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
  border: 1px solid var(--border-color);
  border-radius: 16px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.08), rgba(255, 255, 255, 0.022) 42%, rgba(0, 0, 0, 0.12)),
    var(--workbench-overlay-bg);
  box-shadow: 0 18px 52px rgba(0, 0, 0, 0.34), inset 0 1px 0 rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(18px) saturate(118%);
  -webkit-backdrop-filter: blur(18px) saturate(118%);
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
  background: var(--workbench-panel-bg);
  color: var(--text-secondary);
  box-shadow: none;
  transition: background 160ms ease, border-color 160ms ease, color 160ms ease;
}

.assistant-reasoning-menu button:hover {
  border-color: rgba(var(--primary-rgb), 0.2);
  background: rgba(var(--primary-rgb), 0.1);
  color: var(--text-primary);
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
  color: var(--text-muted);
  font-size: 10px;
  font-weight: var(--font-weight-body);
  line-height: 1.45;
}

.assistant-temperature {
  flex: 1;
  min-width: 0;
  justify-content: flex-end;
  padding: 0 7px;
}

.assistant-temperature span {
  color: var(--text-muted);
}

.assistant-temperature input {
  width: 68px;
  accent-color: var(--primary-color);
}

.assistant-input .assistant-send {
  height: 40px;
  display: grid;
  place-items: center;
  border-radius: 16px;
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

.assistant-note {
  margin: 0;
  padding: 0 14px 12px;
  color: var(--text-muted);
  text-align: center;
  font-size: 10px;
  font-weight: var(--font-weight-body);
}

@keyframes typing {
  0%, 80%, 100% { opacity: 0.25; transform: translateY(0); }
  40% { opacity: 1; transform: translateY(-3px); }
}

@keyframes assistantOrbBreath {
  0%, 100% {
    box-shadow:
      inset 0 1px 0 rgba(255, 255, 255, 0.18),
      0 16px 42px rgba(0, 0, 0, 0.24),
      var(--workbench-shadow);
  }

  50% {
    box-shadow:
      inset 0 1px 0 rgba(255, 255, 255, 0.22),
      0 20px 52px rgba(0, 0, 0, 0.3),
      0 0 0 1px rgba(var(--primary-rgb), 0.08),
      var(--workbench-shadow);
  }
}

@keyframes assistantIconKinetic {
  0% { transform: translate3d(0, 0, 0) scale(1); }
  42% { transform: translate3d(0, -1px, 0) scale(1.05); }
  74% { transform: translate3d(0, 0, 0) scale(1.016); }
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
}

@media (max-width: 560px) {
  .assistant-container.expanded {
    left: 10px !important;
    width: calc(100vw - 20px);
    height: calc(100vh - 20px);
  }
}
</style>
