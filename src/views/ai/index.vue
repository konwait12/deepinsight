<template>
  <div class="ai-workbench" :class="{ entered }">
    <aside class="ai-rail">
      <div class="rail-brand">
        <span class="brand-mark"><Service /></span>
        <div>
          <b>DeepInsight AI</b>
          <em>分析上下文与会话记录</em>
        </div>
      </div>

      <button class="new-chat-btn" type="button" @click="newConversation">
        <el-icon><Plus /></el-icon>
        新对话
      </button>

      <section class="conversation-list">
        <div class="section-head">
          <span>会话</span>
          <b>{{ conversations.length }}</b>
        </div>
        <button
          v-for="conv in conversations"
          :key="conv.id"
          type="button"
          class="conversation-item"
          :class="{ active: activeConvId === conv.id }"
          @click="selectConversation(conv)"
        >
          <span>{{ conv.title }}</span>
          <em>{{ conv.date || '会话记录' }}</em>
          <i @click.stop="deleteConversation(conv.id)"><Close /></i>
        </button>
      </section>
    </aside>

    <main class="chat-stage">
      <header class="stage-header">
        <div>
          <span>AI 分析对话</span>
          <h1>{{ activeConv?.title || '新的分析会话' }}</h1>
        </div>
        <div class="stage-actions">
          <el-tag v-if="conversationId" size="small">Redis #{{ conversationId }}</el-tag>
          <button type="button" @click="exportChat"><Download /></button>
          <button type="button" @click="clearCurrentChat"><Delete /></button>
        </div>
      </header>

      <CloudWorkspacePortal
        class="ai-cloud-panel"
        variant="ai"
        compact
        selectable
        title="云端上下文"
        subtitle="从云端素材库选择模型、数据、矩阵记录、视图或上传文件，直接加入本次 AI 分析。"
        @select="addCloudMaterial"
      />

      <div ref="msgContainer" class="chat-messages">
        <div v-if="messages.length === 0" class="empty-state">
          <div class="empty-orb"><Service /></div>
          <h2>把训练上下文带进对话，继续做可追踪分析</h2>
          <p>你可以导入可视化矩阵里保存的训练分析记录，也可以直接发起模型诊断、代码建议和实验计划。</p>
          <div class="prompt-grid">
            <button v-for="prompt in suggestedPrompts" :key="prompt.label" type="button" @click="askAI(prompt.query)">
              <span>{{ prompt.icon }}</span>
              <strong>{{ prompt.label }}</strong>
              <em>{{ prompt.desc }}</em>
            </button>
          </div>
        </div>

        <article v-for="(msg, index) in messages" :key="index" class="message-row" :class="msg.role">
          <div v-if="msg.role !== 'user'" class="avatar"><Service /></div>
          <div class="message-stack">
            <button
              v-if="msg.thinking"
              type="button"
              class="thinking-block"
              :class="{ open: msg.thinkingExpanded }"
              @click="msg.thinkingExpanded = !msg.thinkingExpanded"
            >
              <span>{{ msg.thinkingExpanded ? '收起推理过程' : '查看推理过程' }}</span>
              <b>{{ msg.reasoningLabel || activeReasoningLabel }}</b>
              <pre v-show="msg.thinkingExpanded">{{ msg.thinking }}</pre>
            </button>
            <div class="message-bubble" v-html="formatContent(msg.content)"></div>
            <div class="msg-actions">
              <time>{{ msg.time }}</time>
              <button v-if="msg.role === 'assistant'" type="button" @click="copyMsg(msg.content)">复制</button>
              <button v-if="msg.role === 'assistant' && index === messages.length - 1 && !loading" type="button" @click="regenerate">重试</button>
            </div>
          </div>
          <div v-if="msg.role === 'user'" class="avatar user"><User /></div>
        </article>

        <article v-if="loading" class="message-row assistant">
          <div class="avatar"><Service /></div>
          <div class="message-stack">
            <div v-if="deepThink" class="thinking-live">
              <span></span>
              {{ activeReasoningLabel }} 推理中
            </div>
            <div class="typing-bubble"><i></i><i></i><i></i></div>
          </div>
        </article>
      </div>

      <form class="composer" @submit.prevent="sendMessage">
        <div class="composer-shell">
          <div v-if="selectedMaterials.length" class="material-strip">
            <div class="material-strip-head">
              <span>已选分析材料</span>
              <b>{{ selectedMaterials.length }}</b>
              <button type="button" @click="clearMaterials">清空</button>
            </div>
            <button
              v-for="item in selectedMaterials"
              :key="materialKey(item)"
              type="button"
              class="material-chip"
              @click="removeMaterial(item)"
            >
              <span>{{ materialTypeLabel(item) }}</span>
              <strong>{{ materialTitle(item) }}</strong>
              <em>移除</em>
            </button>
          </div>
          <textarea
            ref="inputRef"
            v-model="userInput"
            :disabled="loading"
            :placeholder="loading ? 'AI 正在分析...' : '输入问题，Enter 发送，Shift+Enter 换行'"
            rows="1"
            @input="autoResize"
            @keydown.enter.exact.prevent="sendMessage"
            @keydown.enter.shift.exact.prevent="userInput += '\n'"
          />

          <div class="composer-toolbar">
            <div class="material-actions">
              <input ref="fileInputRef" type="file" multiple hidden @change="handleFileInput" />
              <button type="button" class="tool-pill" :disabled="materialBusy" @click="fileInputRef?.click()">
                上传文件/图片
              </button>
              <button type="button" class="tool-pill" :disabled="materialBusy" @click="toggleResourcePanel">
                调用站内资源
              </button>
              <button type="button" class="tool-pill" :disabled="materialBusy || !selectedMaterials.length" @click="saveCurrentMaterials">
                云端保存
              </button>
              <button type="button" class="tool-pill" :disabled="!selectedMaterials.length" @click="localSavePanelOpen = !localSavePanelOpen">
                本地保存
              </button>
            </div>
            <div ref="reasoningMenuRef" class="reasoning-picker" :class="{ open: reasoningMenuOpen }">
              <button
                type="button"
                class="reasoning-trigger"
                :aria-expanded="reasoningMenuOpen"
                @click.stop="toggleReasoningMenu"
              >
                <Service />
                <span>思考：{{ activeReasoningLabel }}</span>
                <em>{{ activeReasoning.hint }}</em>
              </button>
              <div v-show="reasoningMenuOpen" class="reasoning-menu" role="listbox">
                <button
                  v-for="level in reasoningLevels"
                  :key="level.value"
                  type="button"
                  role="option"
                  :aria-selected="reasoningLevel === level.value"
                  :class="{ active: reasoningLevel === level.value }"
                  @click.stop="selectReasoningLevel(level.value)"
                >
                  <strong>{{ level.label }}</strong>
                  <em>{{ level.hint }}</em>
                  <span>{{ level.desc }}</span>
                </button>
              </div>
            </div>

            <label class="temperature-pill">
              <span>温度</span>
              <input v-model.number="temperature" type="range" min="0" max="1" step="0.05" />
              <b>{{ temperature.toFixed(2) }}</b>
            </label>
          </div>

          <div v-if="resourcePanelOpen" class="resource-panel">
            <div class="resource-panel-head">
              <div>
                <strong>选择站内资源作为分析上下文</strong>
                <span>模型、数据集、训练运行、矩阵记录和已保存视图会按当前账号隔离读取。</span>
              </div>
              <div class="resource-panel-actions">
                <button type="button" @click="loadWorkspaceResources">刷新</button>
                <button type="button" @click="resourcePanelOpen = false">关闭</button>
              </div>
            </div>
            <div class="resource-grid">
              <button
                v-for="resource in workspaceResources"
                :key="materialKey(resource)"
                type="button"
                :class="{ active: isMaterialSelected(resource) }"
                @click="toggleMaterial(resource)"
              >
                <span>{{ materialTypeLabel(resource) }}</span>
                <strong>{{ materialTitle(resource) }}</strong>
                <em>{{ resource.summary || resource.meta }}</em>
              </button>
            </div>
          </div>

          <div v-if="localSavePanelOpen" class="local-save-panel">
            <div class="save-option-head">
              <strong>本地保存当前材料</strong>
              <button type="button" @click="localSavePanelOpen = false">关闭</button>
            </div>
            <div class="save-option-grid">
              <label>
                <span>内容</span>
                <select v-model="localSaveKind">
                  <option value="summary">材料摘要</option>
                  <option value="json">完整 JSON</option>
                </select>
              </label>
              <label>
                <span>格式</span>
                <select v-model="localSaveFormat">
                  <option value="md">Markdown</option>
                  <option value="word">Word</option>
                  <option value="json">JSON</option>
                </select>
              </label>
              <button type="button" @click="saveMaterialsToLocal">导出本地文件</button>
            </div>
          </div>
        </div>

        <button v-if="!loading" class="send-button" type="submit" :disabled="!userInput.trim()">
          <Promotion />
        </button>
        <button v-else class="send-button" type="button" @click="stopGeneration">
          <VideoPause />
        </button>
      </form>
    </main>

    <aside class="context-panel">
      <section>
        <span>当前调用</span>
        <h3>{{ activeReasoningLabel }}</h3>
        <p>{{ activeReasoningDesc }}</p>
        <div class="capability-list">
          <b>MySQL 保存分析记录</b>
          <b>Redis 保存对话历史</b>
          <b>用户隔离校验</b>
          <b>可视化结果导入上下文</b>
        </div>
      </section>

      <section>
        <span>上下文记录</span>
        <div class="context-meter">
          <strong>{{ tokenCount }}</strong>
          <em>本页估算 tokens / chars</em>
        </div>
        <p>从矩阵导入的记录会写入新的 AI 会话，后续提问会沿用同一份分析材料。</p>
      </section>
    </aside>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Close, Delete, Download, Plus, Promotion, Service, User, VideoPause } from '@element-plus/icons-vue'
import { aiApi } from '@/api'
import { renderMarkdown } from '@/utils/markdown'
import CloudWorkspacePortal from '@/components/cloud/CloudWorkspacePortal.vue'

type ReasoningLevel = 'off' | 'quick' | 'low' | 'deep' | 'max'
type Message = {
  role: string
  content: string
  time: string
  thinking?: string
  thinkingExpanded?: boolean
  reasoningLabel?: string
}
type Conversation = { id: number; title: string; date: string }
type WorkspaceMaterial = Record<string, any>

const route = useRoute()
const msgContainer = ref<HTMLElement | null>(null)
const inputRef = ref<HTMLTextAreaElement | null>(null)
const fileInputRef = ref<HTMLInputElement | null>(null)
const loading = ref(false)
const thinking = ref(false)
const userInput = ref('')
const conversationId = ref<number | null>(null)
const chatHistory = ref<Array<{ role: string; content: string }>>([])
const messages = ref<Message[]>([])
const conversations = ref<Conversation[]>([])
const activeConvId = ref<number | null>(null)
const entered = ref(false)
const reasoningLevel = ref<ReasoningLevel>('deep')
const temperature = ref(0.7)
const tokenCount = ref(0)
const reasoningMenuOpen = ref(false)
const reasoningMenuRef = ref<HTMLElement | null>(null)
const resourcePanelOpen = ref(false)
const localSavePanelOpen = ref(false)
const materialBusy = ref(false)
const selectedMaterials = ref<WorkspaceMaterial[]>([])
const workspaceResources = ref<WorkspaceMaterial[]>([])
const localSaveKind = ref<'summary' | 'json'>('summary')
const localSaveFormat = ref<'md' | 'word' | 'json'>('md')

const reasoningLevels: Array<{ value: ReasoningLevel; label: string; hint: string; desc: string }> = [
  { value: 'off', label: '关闭', hint: '直接回答', desc: '不请求模型推理能力，适合轻量问答。' },
  { value: 'quick', label: '快速', hint: 'minimal', desc: '低延迟推理，用于小修小问。' },
  { value: 'low', label: '标准', hint: 'low', desc: '平衡速度与分析质量，适合普通诊断。' },
  { value: 'deep', label: '深度', hint: 'high', desc: '调用高强度 reasoning，适合训练异常、矩阵复盘。' },
  { value: 'max', label: '极限', hint: 'xhigh', desc: '最大上下文与推理预算，适合复杂多步分析。' },
]

const suggestedPrompts = [
  { icon: 'M', label: '模型分析', desc: '结构、瓶颈、适用场景', query: '请分析当前模型结构的优缺点，并给出下一步优化建议。' },
  { icon: 'D', label: '训练诊断', desc: '损失、准确率、学习率', query: '训练损失不稳定时，应该如何系统排查？请按优先级输出。' },
  { icon: 'X', label: '矩阵复盘', desc: '多模型 × 多模块', query: '请帮我设计一次多模型多分析模块的复盘流程，输出可执行步骤。' },
  { icon: 'C', label: '代码方案', desc: '实验脚本和伪代码', query: '给我一个 PyTorch 训练诊断脚本模板，包含日志记录和异常检测。' },
]

const activeConv = computed(() => conversations.value.find((item) => item.id === activeConvId.value))
const deepThink = computed(() => reasoningLevel.value !== 'off')
const activeReasoning = computed(() => reasoningLevels.find((item) => item.value === reasoningLevel.value) || reasoningLevels[0])
const activeReasoningLabel = computed(() => activeReasoning.value.label)
const activeReasoningDesc = computed(() => activeReasoning.value.desc)

onMounted(async () => {
  setTimeout(() => { entered.value = true }, 80)
  document.addEventListener('click', closeReasoningMenuOnOutside)
  await loadConversations()
  await loadWorkspaceResources()
  const routeConversationId = Number(route.query.conversationId)
  if (Number.isFinite(routeConversationId) && routeConversationId > 0) {
    const existing = conversations.value.find((conv) => conv.id === routeConversationId)
    if (existing) {
      await selectConversation(existing)
    } else {
      await selectConversation({ id: routeConversationId, title: `分析对话 #${routeConversationId}`, date: '' })
    }
    return
  }
  if (!messages.value.length) {
    messages.value.push({
      role: 'assistant',
      content: '欢迎来到 **DeepInsight AI 分析对话**。这里会保留会话记录；从可视化矩阵导入的分析结果会作为后续讨论材料。',
      time: getTime(),
    })
  }
})

onBeforeUnmount(() => {
  document.removeEventListener('click', closeReasoningMenuOnOutside)
})

function getTime() {
  const d = new Date()
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

async function scrollToBottom() {
  await nextTick()
  if (msgContainer.value) msgContainer.value.scrollTop = msgContainer.value.scrollHeight
}

function autoResize() {
  if (!inputRef.value) return
  inputRef.value.style.height = 'auto'
  inputRef.value.style.height = `${Math.min(inputRef.value.scrollHeight, 140)}px`
}

function formatContent(text: string) {
  return text ? renderMarkdown(text) : ''
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
  const msg = userInput.value.trim()
  userInput.value = ''
  if (inputRef.value) inputRef.value.style.height = 'auto'

  messages.value.push({ role: 'user', content: msg, time: getTime() })
  chatHistory.value.push({ role: 'user', content: msg })
  loading.value = true
  tokenCount.value += msg.length
  await scrollToBottom()

  try {
    thinking.value = true
    const response = await aiApi.chat({
      message: msg,
      history: chatHistory.value.slice(-20),
      temperature: temperature.value,
      deepThink: deepThink.value,
      reasoningLevel: reasoningLevel.value,
      attachments: selectedMaterials.value.filter((item) => ['file', 'image', 'upload'].includes(String(item.itemType || item.sourceType))),
      resources: selectedMaterials.value.filter((item) => !['file', 'image', 'upload'].includes(String(item.itemType || item.sourceType))),
      ...(conversationId.value ? { conversationId: conversationId.value } : {}),
    })
    thinking.value = false
    const data = response.data.data
    if (data.conversationId) {
      conversationId.value = data.conversationId
      if (!activeConvId.value) {
        const newConv = { id: data.conversationId, title: msg.slice(0, 28) + (msg.length > 28 ? '...' : ''), date: new Date().toLocaleDateString('zh-CN') }
        conversations.value.unshift(newConv)
        activeConvId.value = newConv.id
      }
    }
    const reply = data.reply || data.message?.content || 'AI 服务暂未返回内容。'
    const thinkingText = data.reasoning || ''
    messages.value.push({
      role: 'assistant',
      content: reply,
      time: getTime(),
      thinking: thinkingText,
      thinkingExpanded: Boolean(thinkingText),
      reasoningLabel: activeReasoningLabel.value,
    })
    chatHistory.value.push({ role: 'assistant', content: reply })
    tokenCount.value += reply.length
    selectedMaterials.value = []
  } catch (error: any) {
    thinking.value = false
    messages.value.push({ role: 'assistant', content: `AI 服务暂时不可用：${error?.response?.data?.message || error.message || '未知错误'}`, time: getTime() })
  } finally {
    loading.value = false
    await scrollToBottom()
  }
}

function materialKey(item: WorkspaceMaterial) {
  return `${item.type || item.sourceType || item.itemType || 'material'}:${item.id || item.fileUrl || item.title}`
}

function materialTitle(item: WorkspaceMaterial) {
  return String(item.title || item.fileName || item.name || '未命名材料')
}

function materialTypeLabel(item: WorkspaceMaterial) {
  const type = String(item.type || item.itemType || item.sourceType || 'resource')
  const labels: Record<string, string> = {
    training: '训练',
    dataset: '数据',
    model: '模型',
    run: '运行',
    analysis_result: '矩阵',
    saved_view: '视图',
    image: '图片',
    file: '文件',
    upload: '上传',
    text: '文本',
  }
  return labels[type] || '资源'
}

function isMaterialSelected(item: WorkspaceMaterial) {
  const key = materialKey(item)
  return selectedMaterials.value.some((selected) => materialKey(selected) === key)
}

function toggleMaterial(item: WorkspaceMaterial) {
  if (isMaterialSelected(item)) {
    removeMaterial(item)
  } else {
    selectedMaterials.value = [...selectedMaterials.value, item]
  }
}

function addCloudMaterial(item: WorkspaceMaterial) {
  if (!isMaterialSelected(item)) {
    selectedMaterials.value = [...selectedMaterials.value, item]
  }
}

function removeMaterial(item: WorkspaceMaterial) {
  const key = materialKey(item)
  selectedMaterials.value = selectedMaterials.value.filter((selected) => materialKey(selected) !== key)
}

function clearMaterials() {
  selectedMaterials.value = []
  localSavePanelOpen.value = false
}

async function loadWorkspaceResources() {
  try {
    const response = await aiApi.listWorkspaceResources()
    workspaceResources.value = response.data.data || []
  } catch {
    workspaceResources.value = []
  }
}

async function toggleResourcePanel() {
  resourcePanelOpen.value = !resourcePanelOpen.value
  if (resourcePanelOpen.value && !workspaceResources.value.length) await loadWorkspaceResources()
}

async function handleFileInput(event: Event) {
  const input = event.target as HTMLInputElement
  const files = Array.from(input.files || [])
  input.value = ''
  if (!files.length) return
  materialBusy.value = true
  try {
    const response = await aiApi.uploadWorkspaceFiles(files)
    const uploaded = response.data.data || []
    selectedMaterials.value = [...selectedMaterials.value, ...uploaded]
    ElMessage.success(`已上传 ${uploaded.length} 个素材`)
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || '上传失败')
  } finally {
    materialBusy.value = false
  }
}

async function saveCurrentMaterials() {
  if (!selectedMaterials.value.length) return
  materialBusy.value = true
  try {
    await aiApi.saveWorkspaceItem({
      title: `AI 分析素材 ${new Date().toLocaleString('zh-CN')}`,
      itemType: 'context_bundle',
      sourceType: 'ai_chat',
      format: 'json',
      content: buildMaterialsMarkdown(),
      payload: { materials: selectedMaterials.value },
    })
    await loadWorkspaceResources()
    ElMessage.success('已保存到云端素材库')
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || '保存失败')
  } finally {
    materialBusy.value = false
  }
}

function buildMaterialsMarkdown() {
  const lines = [
    '# DeepInsight AI 分析材料',
    '',
    `- 材料数量：${selectedMaterials.value.length}`,
    `- 生成时间：${new Date().toLocaleString('zh-CN')}`,
    '',
    '## 材料清单',
  ]
  selectedMaterials.value.forEach((item, index) => {
    lines.push(
      '',
      `### ${index + 1}. ${materialTitle(item)}`,
      '',
      `- 类型：${materialTypeLabel(item)}`,
      `- 来源：${item.sourceType || item.type || item.itemType || '-'}`,
      `- 格式：${item.format || item.mimeType || '-'}`,
      `- 摘要：${item.summary || item.meta || item.content || '-'}`
    )
    if (item.fileUrl) lines.push(`- 文件地址：${item.fileUrl}`)
  })
  return lines.join('\n')
}

function markdownToWordHtml(markdown: string, title: string) {
  const body = markdown
    .split('\n')
    .map((line) => {
      if (line.startsWith('# ')) return `<h1>${escapeHtml(line.slice(2))}</h1>`
      if (line.startsWith('## ')) return `<h2>${escapeHtml(line.slice(3))}</h2>`
      if (line.startsWith('### ')) return `<h3>${escapeHtml(line.slice(4))}</h3>`
      if (line.startsWith('- ')) return `<p>• ${escapeHtml(line.slice(2))}</p>`
      return line ? `<p>${escapeHtml(line)}</p>` : '<br>'
    })
    .join('')
  return `<!doctype html><html><head><meta charset="utf-8"><title>${escapeHtml(title)}</title></head><body>${body}</body></html>`
}

function escapeHtml(value: string) {
  return value.replace(/[&<>"']/g, (char) => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;',
  }[char] || char))
}

function downloadBlob(content: BlobPart, filename: string, type: string) {
  const blob = new Blob([content], { type })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  URL.revokeObjectURL(url)
}

function saveMaterialsToLocal() {
  if (!selectedMaterials.value.length) return
  const stamp = Date.now()
  const markdown = buildMaterialsMarkdown()
  if (localSaveFormat.value === 'json' || localSaveKind.value === 'json') {
    downloadBlob(JSON.stringify({ materials: selectedMaterials.value, exportedAt: new Date().toISOString() }, null, 2), `deepinsight-ai-materials-${stamp}.json`, 'application/json;charset=utf-8')
  } else if (localSaveFormat.value === 'word') {
    downloadBlob(markdownToWordHtml(markdown, 'DeepInsight AI 分析材料'), `deepinsight-ai-materials-${stamp}.doc`, 'application/msword;charset=utf-8')
  } else {
    downloadBlob(markdown, `deepinsight-ai-materials-${stamp}.md`, 'text/markdown;charset=utf-8')
  }
  localSavePanelOpen.value = false
  ElMessage.success('已导出本地文件')
}

function stopGeneration() {
  loading.value = false
  ElMessage.info('已停止生成')
}

function askAI(query: string) {
  userInput.value = query
  void sendMessage()
}

function copyMsg(text: string) {
  navigator.clipboard.writeText(text)
  ElMessage.success('已复制')
}

function regenerate() {
  const lastUser = [...chatHistory.value].reverse().find((item) => item.role === 'user')
  if (!lastUser) return
  messages.value = messages.value.filter((_, index) => index < messages.value.length - 1)
  chatHistory.value = chatHistory.value.filter((_, index) => index < chatHistory.value.length - 1)
  userInput.value = lastUser.content
  void sendMessage()
}

function newConversation() {
  messages.value = []
  chatHistory.value = []
  conversationId.value = null
  activeConvId.value = null
  userInput.value = ''
  tokenCount.value = 0
  messages.value.push({ role: 'assistant', content: '新对话已开始。你可以描述问题，也可以从可视化矩阵导入分析结果。', time: getTime() })
  nextTick(() => inputRef.value?.focus())
}

async function loadConversations() {
  try {
    const response = await aiApi.listConversations()
    if (response.data.code === 200 && response.data.data) {
      conversations.value = response.data.data.map((item: any) => ({
        id: Number(item.id),
        title: item.title || '对话',
        date: item.createdAt || '',
      }))
    }
  } catch {
    conversations.value = []
  }
}

async function selectConversation(conv: Conversation) {
  activeConvId.value = conv.id
  conversationId.value = conv.id
  try {
    const response = await aiApi.getMessages(conv.id)
    if (response.data.code === 200 && response.data.data) {
      messages.value = response.data.data.map((item: any) => ({
        role: item.role,
        content: item.content,
        time: '',
      }))
      chatHistory.value = response.data.data.map((item: any) => ({ role: item.role, content: item.content }))
      tokenCount.value = chatHistory.value.reduce((sum, item) => sum + item.content.length, 0)
      await scrollToBottom()
    }
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || '无法加载该对话')
  }
}

async function deleteConversation(id: number) {
  await aiApi.deleteConversation(id).catch(() => {})
  conversations.value = conversations.value.filter((item) => item.id !== id)
  if (activeConvId.value === id) newConversation()
}

function clearCurrentChat() {
  messages.value = []
  chatHistory.value = []
  tokenCount.value = 0
  ElMessage.success('当前视图已清空，Redis 历史未删除')
}

function exportChat() {
  if (!messages.value.length) {
    ElMessage.warning('暂无内容')
    return
  }
  const text = messages.value.map((item) => `[${item.time}] ${item.role === 'user' ? '我' : 'AI'}: ${item.content}`).join('\n\n')
  const blob = new Blob([text], { type: 'text/plain' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `deepinsight-chat-${Date.now()}.txt`
  link.click()
  URL.revokeObjectURL(url)
  ElMessage.success('已导出')
}
</script>

<style scoped>
.ai-workbench {
  height: calc(100vh - 72px);
  min-height: 640px;
  display: grid;
  grid-template-columns: 292px minmax(0, 1fr) 288px;
  gap: 16px;
  padding: 18px;
  overflow: hidden;
  opacity: 0;
  transform: translateY(12px);
  transition: opacity 520ms ease, transform 520ms ease;
}

.ai-workbench.entered {
  opacity: 1;
  transform: translateY(0);
}

.ai-rail,
.chat-stage,
.context-panel section {
  border: 1px solid color-mix(in srgb, var(--primary-color) 16%, var(--border-color));
  border-radius: 24px;
  background:
    radial-gradient(circle at 12% 0%, rgba(var(--primary-rgb), 0.12), transparent 34%),
    rgba(var(--glass-bg-rgb), 0.32);
  box-shadow: 0 24px 70px rgba(0, 0, 0, 0.22);
  backdrop-filter: blur(18px);
}

.ai-rail {
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  padding: 14px;
}

.rail-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px;
}

.brand-mark,
.avatar,
.empty-orb {
  display: grid;
  place-items: center;
  border-radius: 16px;
  background: rgba(var(--primary-rgb), 0.14);
  color: var(--primary-color);
}

.brand-mark {
  width: 42px;
  height: 42px;
}

.rail-brand b,
.stage-header h1,
.context-panel h3 {
  display: block;
  color: var(--text-primary);
  font-weight: 950;
}

.rail-brand em,
.card-kicker,
.section-head,
.stage-header span,
.context-panel span {
  color: var(--text-muted);
  font-size: 10px;
  font-style: normal;
  font-weight: 950;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.new-chat-btn,
.conversation-item,
.prompt-grid button,
.stage-actions button,
.msg-actions button,
.composer button {
  border: 0;
  cursor: pointer;
  font: inherit;
}

.new-chat-btn {
  height: 42px;
  margin: 10px 0 12px;
  border-radius: 16px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background: var(--primary-color);
  color: #06100c;
  font-size: 12px;
  font-weight: 950;
  box-shadow: 0 16px 40px rgba(var(--primary-rgb), 0.24);
}

.context-panel p,
.prompt-grid em {
  color: var(--text-secondary);
  font-size: 11px;
  font-weight: 750;
}

.conversation-list {
  min-height: 0;
  flex: 1;
  overflow-y: auto;
  overscroll-behavior: contain;
  margin-top: 12px;
}

.section-head {
  display: flex;
  justify-content: space-between;
  padding: 6px 2px 8px;
}

.conversation-item {
  position: relative;
  width: 100%;
  display: grid;
  gap: 3px;
  margin-bottom: 7px;
  padding: 10px 34px 10px 11px;
  border: 1px solid transparent;
  border-radius: 15px;
  text-align: left;
  background: transparent;
  color: var(--text-secondary);
}

.conversation-item.active {
  border-color: rgba(var(--primary-rgb), 0.38);
  background: rgba(var(--primary-rgb), 0.12);
  color: var(--text-primary);
}

.conversation-item span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
  font-weight: 900;
}

.conversation-item em {
  color: var(--text-muted);
  font-size: 10px;
  font-style: normal;
}

.conversation-item i {
  position: absolute;
  top: 10px;
  right: 10px;
  width: 20px;
  height: 20px;
  display: grid;
  place-items: center;
  border-radius: 8px;
  opacity: 0;
}

.conversation-item:hover i {
  opacity: 0.75;
}

.chat-stage {
  min-width: 0;
  min-height: 0;
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr) auto;
  overflow: hidden;
}

.stage-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 18px 20px;
  border-bottom: 1px solid var(--border-color);
}

.stage-header h1 {
  margin: 3px 0 0;
  font-size: 18px;
}

.stage-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.stage-actions button {
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  border-radius: 12px;
  background: rgba(var(--primary-rgb), 0.1);
  color: var(--primary-color);
}

.ai-cloud-panel {
  margin: 0 18px 10px;
}

.chat-messages {
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding: 22px;
  display: flex;
  flex-direction: column;
  gap: 18px;
  background-image: radial-gradient(rgba(var(--primary-rgb), 0.08) 1px, transparent 1px);
  background-size: 20px 20px;
}

.empty-state {
  min-height: 100%;
  display: grid;
  place-items: center;
  align-content: center;
  text-align: center;
}

.empty-orb {
  width: 72px;
  height: 72px;
  margin-bottom: 18px;
  font-size: 30px;
}

.empty-state h2 {
  margin: 0 0 8px;
  color: var(--text-primary);
  font-size: 22px;
  font-weight: 950;
}

.empty-state p {
  max-width: 620px;
  margin: 0 auto 18px;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.7;
}

.prompt-grid {
  width: min(620px, 100%);
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.prompt-grid button {
  min-height: 104px;
  padding: 14px;
  border: 1px solid rgba(var(--primary-rgb), 0.16);
  border-radius: 18px;
  text-align: left;
  background: rgba(var(--glass-bg-rgb), 0.28);
  color: var(--text-primary);
  transition: transform 180ms ease, border-color 180ms ease;
}

.prompt-grid button:hover {
  transform: translateY(-2px);
  border-color: rgba(var(--primary-rgb), 0.42);
}

.prompt-grid span {
  display: inline-grid;
  width: 26px;
  height: 26px;
  place-items: center;
  margin-bottom: 10px;
  border-radius: 10px;
  background: rgba(var(--primary-rgb), 0.12);
  color: var(--primary-color);
  font-weight: 950;
}

.prompt-grid strong {
  display: block;
  font-size: 13px;
  font-weight: 950;
}

.message-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.message-row.user {
  justify-content: flex-end;
}

.avatar {
  width: 34px;
  height: 34px;
  flex: 0 0 auto;
}

.avatar.user {
  background: rgba(148, 163, 184, 0.14);
  color: var(--text-secondary);
}

.message-stack {
  max-width: min(760px, 78%);
}

.message-bubble {
  padding: 14px 16px;
  border: 1px solid var(--border-color);
  border-radius: 18px;
  background: rgba(var(--glass-bg-rgb), 0.44);
  color: var(--text-primary);
  font-size: 13px;
  line-height: 1.75;
}

.message-row.user .message-bubble {
  border-color: rgba(var(--primary-rgb), 0.46);
  background: var(--primary-color);
  color: #06100c;
}

.msg-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 5px;
  padding: 0 4px;
  color: var(--text-muted);
  font-size: 10px;
  font-weight: 800;
}

.msg-actions button {
  background: transparent;
  color: var(--text-muted);
  font-size: 10px;
  font-weight: 900;
}

.thinking-block {
  width: 100%;
  margin-bottom: 8px;
  padding: 10px 12px;
  border: 1px solid rgba(var(--primary-rgb), 0.18);
  border-radius: 15px;
  text-align: left;
  background: rgba(var(--primary-rgb), 0.08);
  color: var(--text-secondary);
}

.thinking-block span,
.thinking-live {
  color: var(--primary-color);
  font-size: 12px;
  font-weight: 950;
}

.thinking-block b {
  float: right;
  color: var(--text-muted);
  font-size: 10px;
}

.thinking-block pre {
  margin: 10px 0 0;
  white-space: pre-wrap;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.65;
}

.thinking-live {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.thinking-live span {
  width: 12px;
  height: 12px;
  border: 2px solid rgba(var(--primary-rgb), 0.2);
  border-top-color: var(--primary-color);
  border-radius: 999px;
  animation: spin 800ms linear infinite;
}

.typing-bubble {
  width: 74px;
  padding: 14px;
  border-radius: 18px;
  background: rgba(var(--glass-bg-rgb), 0.44);
}

.typing-bubble i {
  display: inline-block;
  width: 7px;
  height: 7px;
  margin-right: 4px;
  border-radius: 999px;
  background: var(--primary-color);
  animation: typing 1.1s infinite ease-in-out;
}

.typing-bubble i:nth-child(2) { animation-delay: 140ms; }
.typing-bubble i:nth-child(3) { animation-delay: 280ms; }

.composer {
  display: grid;
  grid-template-columns: 1fr 48px;
  align-items: end;
  gap: 10px;
  padding: 16px 18px 18px;
  border-top: 1px solid var(--border-color);
  overflow: visible;
}

.composer-shell {
  position: relative;
  display: grid;
  gap: 10px;
  min-width: 0;
  border: 1px solid rgba(var(--primary-rgb), 0.18);
  border-radius: 20px;
  background:
    linear-gradient(135deg, rgba(var(--primary-rgb), 0.1), transparent 42%),
    var(--bg-input);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.06);
  transition: border-color 180ms ease, box-shadow 180ms ease;
}

.composer-shell:focus-within {
  border-color: rgba(var(--primary-rgb), 0.5);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.08),
    0 0 0 3px rgba(var(--primary-rgb), 0.1);
}

.composer textarea {
  min-height: 52px;
  max-height: 140px;
  resize: none;
  border: 0;
  border-radius: 20px 20px 12px 12px;
  padding: 15px 16px 2px;
  outline: none;
  background: transparent;
  color: var(--text-primary);
  font-size: 13px;
  line-height: 1.5;
}

.composer textarea:focus {
  box-shadow: none;
}

.material-strip {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  padding: 10px 10px 0;
}

.material-strip-head {
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 0 4px 0 2px;
  color: var(--text-secondary);
  font-size: 11px;
  font-weight: 950;
}

.material-strip-head b {
  min-width: 20px;
  height: 20px;
  display: grid;
  place-items: center;
  border-radius: 999px;
  background: rgba(var(--primary-rgb), 0.16);
  color: var(--primary-color);
  font-size: 10px;
}

.material-strip-head button {
  height: 24px;
  padding: 0 8px;
  border-radius: 999px;
  background: rgba(248, 113, 113, 0.12);
  color: #fca5a5;
  font-size: 10px;
  font-weight: 950;
}

.material-chip,
.tool-pill {
  border: 1px solid rgba(var(--primary-rgb), 0.2) !important;
  border-radius: 999px;
  background: rgba(var(--glass-bg-rgb), 0.28) !important;
  color: var(--text-secondary) !important;
  box-shadow: none !important;
  font-size: 11px;
  font-weight: 900;
}

.material-chip {
  min-height: 32px;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 0 10px;
}

.material-chip span,
.resource-grid span {
  color: var(--primary-color);
  font-size: 10px;
  font-weight: 950;
}

.material-chip strong {
  max-width: 180px;
  overflow: hidden;
  color: var(--text-primary);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.material-chip em {
  color: var(--text-muted);
  font-size: 10px;
  font-style: normal;
}

.composer-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 0 8px 8px;
}

.material-actions {
  min-width: 0;
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
}

.tool-pill {
  height: 32px;
  padding: 0 11px;
}

.tool-pill:disabled {
  opacity: 0.46;
  cursor: not-allowed;
}

.resource-panel {
  position: absolute;
  left: 10px;
  right: 10px;
  bottom: calc(100% + 12px);
  z-index: 35;
  max-height: min(420px, 54vh);
  overflow: hidden;
  display: grid;
  grid-template-rows: auto 1fr;
  padding: 12px;
  border: 1px solid rgba(var(--primary-rgb), 0.28);
  border-radius: 20px;
  background:
    radial-gradient(circle at 12% 0%, rgba(var(--primary-rgb), 0.18), transparent 34%),
    rgba(12, 18, 26, 0.97);
  box-shadow: 0 24px 70px rgba(0, 0, 0, 0.38);
  backdrop-filter: blur(18px);
}

.local-save-panel {
  position: absolute;
  left: 10px;
  bottom: calc(100% + 12px);
  z-index: 36;
  width: min(520px, calc(100% - 20px));
  display: grid;
  gap: 10px;
  padding: 12px;
  border: 1px solid rgba(var(--primary-rgb), 0.3);
  border-radius: 20px;
  background:
    radial-gradient(circle at 20% 0%, rgba(var(--primary-rgb), 0.18), transparent 38%),
    rgba(12, 18, 26, 0.97);
  box-shadow: 0 24px 70px rgba(0, 0, 0, 0.38);
  backdrop-filter: blur(18px);
}

.save-option-head,
.resource-panel-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.save-option-head strong {
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 950;
}

.save-option-head button,
.resource-panel-actions button {
  height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(var(--primary-rgb), 0.12);
  color: var(--primary-color);
  font-size: 11px;
  font-weight: 950;
}

.save-option-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr)) auto;
  gap: 9px;
}

.save-option-grid label {
  display: grid;
  gap: 6px;
}

.save-option-grid span {
  color: var(--text-muted);
  font-size: 10px;
  font-weight: 900;
}

.save-option-grid select {
  height: 36px;
  border: 1px solid rgba(var(--primary-rgb), 0.2);
  border-radius: 12px;
  background: rgba(var(--glass-bg-rgb), 0.32);
  color: var(--text-primary);
  padding: 0 10px;
  outline: none;
}

.save-option-grid > button {
  align-self: end;
  height: 36px;
  padding: 0 12px;
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(var(--primary-rgb), 0.22), rgba(66, 230, 164, 0.16));
  color: var(--primary-color);
  font-size: 11px;
  font-weight: 950;
}

.resource-panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.resource-panel-head span {
  display: block;
  margin-top: 4px;
  color: var(--text-muted);
  font-size: 11px;
  font-weight: 750;
}

.resource-panel-head strong {
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 950;
}

.resource-grid {
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(190px, 1fr));
  gap: 8px;
}

.resource-grid button {
  min-height: 86px;
  display: grid;
  align-content: start;
  gap: 5px;
  padding: 11px;
  border: 1px solid rgba(148, 163, 184, 0.16);
  border-radius: 15px;
  text-align: left;
  background: rgba(var(--glass-bg-rgb), 0.22);
  color: var(--text-secondary);
}

.resource-grid button.active {
  border-color: rgba(var(--primary-rgb), 0.58);
  background: rgba(var(--primary-rgb), 0.14);
}

.resource-grid strong {
  overflow: hidden;
  color: var(--text-primary);
  font-size: 12px;
  font-weight: 950;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.resource-grid em {
  color: var(--text-muted);
  font-size: 10px;
  font-style: normal;
  line-height: 1.45;
}

.reasoning-picker {
  position: relative;
  min-width: 0;
}

.reasoning-trigger,
.temperature-pill {
  height: 32px;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  border: 1px solid rgba(var(--primary-rgb), 0.18) !important;
  border-radius: 999px;
  background: rgba(var(--glass-bg-rgb), 0.26) !important;
  color: var(--text-secondary) !important;
  box-shadow: none !important;
  font-size: 11px;
  font-weight: 950;
}

.reasoning-trigger {
  max-width: 260px;
  padding: 0 11px;
}

.reasoning-trigger svg {
  width: 14px;
  height: 14px;
  color: var(--primary-color);
}

.reasoning-trigger span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.reasoning-trigger em,
.temperature-pill span {
  color: var(--text-muted);
  font-size: 10px;
  font-style: normal;
}

.reasoning-picker.open .reasoning-trigger {
  border-color: rgba(var(--primary-rgb), 0.5) !important;
  color: var(--text-primary) !important;
}

.reasoning-menu {
  position: absolute;
  left: 0;
  bottom: calc(100% + 10px);
  z-index: 40;
  width: min(330px, calc(100vw - 72px));
  display: grid;
  gap: 7px;
  padding: 10px;
  border: 1px solid rgba(var(--primary-rgb), 0.26);
  border-radius: 18px;
  background:
    radial-gradient(circle at 12% 0%, rgba(var(--primary-rgb), 0.18), transparent 35%),
    rgba(12, 18, 26, 0.96);
  box-shadow: 0 24px 70px rgba(0, 0, 0, 0.36);
  backdrop-filter: blur(18px);
  animation: menuRise 160ms ease both;
}

.reasoning-menu button {
  height: auto;
  display: grid;
  grid-template-columns: 58px 1fr;
  gap: 2px 10px;
  padding: 10px 11px;
  border: 1px solid rgba(148, 163, 184, 0.14);
  border-radius: 14px;
  text-align: left;
  background: rgba(var(--glass-bg-rgb), 0.18);
  color: var(--text-secondary);
  box-shadow: none;
  transition: transform 160ms ease, border-color 160ms ease, background 160ms ease;
}

.reasoning-menu button:hover {
  transform: translateY(-1px);
  border-color: rgba(var(--primary-rgb), 0.38);
}

.reasoning-menu button.active {
  border-color: rgba(var(--primary-rgb), 0.64);
  background: rgba(var(--primary-rgb), 0.18);
  color: var(--text-primary);
  box-shadow: inset 0 0 0 1px rgba(var(--primary-rgb), 0.16);
}

.reasoning-menu strong {
  color: inherit;
  font-size: 12px;
  font-weight: 950;
}

.reasoning-menu em {
  justify-self: start;
  align-self: center;
  padding: 2px 7px;
  border-radius: 999px;
  background: rgba(var(--primary-rgb), 0.1);
  color: var(--primary-color);
  font-size: 10px;
  font-style: normal;
  font-weight: 950;
}

.reasoning-menu span {
  grid-column: 1 / -1;
  color: var(--text-muted);
  font-size: 11px;
  font-weight: 750;
  line-height: 1.45;
}

.temperature-pill {
  flex: 0 0 auto;
  padding: 0 9px 0 10px;
}

.temperature-pill input {
  width: 76px;
  accent-color: var(--primary-color);
}

.temperature-pill b {
  min-width: 28px;
  color: var(--primary-color);
  font-size: 10px;
}

.composer .send-button {
  height: 48px;
  display: grid;
  place-items: center;
  border-radius: 18px;
  background: var(--primary-color);
  color: #06100c;
  box-shadow: 0 16px 34px rgba(var(--primary-rgb), 0.24);
}

.composer .send-button:disabled {
  opacity: 0.42;
  cursor: not-allowed;
}

.context-panel {
  min-height: 0;
  display: grid;
  align-content: start;
  gap: 16px;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding-right: 2px;
}

.context-panel section {
  padding: 16px;
}

.context-panel h3 {
  margin: 6px 0 8px;
  font-size: 18px;
}

.capability-list {
  display: grid;
  gap: 8px;
  margin-top: 14px;
}

.capability-list b {
  padding: 9px 10px;
  border-radius: 13px;
  background: rgba(var(--primary-rgb), 0.1);
  color: var(--primary-color);
  font-size: 11px;
  font-weight: 950;
}

.context-meter {
  margin: 12px 0;
  padding: 14px;
  border-radius: 17px;
  background: rgba(0, 0, 0, 0.16);
}

.context-meter strong {
  display: block;
  color: var(--primary-color);
  font-size: 32px;
  font-weight: 950;
}

.context-meter em {
  color: var(--text-muted);
  font-size: 11px;
  font-style: normal;
}

.message-bubble :deep(pre) {
  overflow-x: auto;
  padding: 12px;
  border-radius: 12px;
  background: rgba(0, 0, 0, 0.26);
}

.message-bubble :deep(code) {
  font-family: "JetBrains Mono", Consolas, monospace;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@keyframes typing {
  0%, 80%, 100% { opacity: 0.25; transform: translateY(0); }
  40% { opacity: 1; transform: translateY(-3px); }
}

@keyframes menuRise {
  from { opacity: 0; transform: translateY(6px) scale(0.98); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

@media (max-width: 1180px) {
  .ai-workbench {
    grid-template-columns: 280px minmax(0, 1fr);
  }

  .context-panel {
    display: none;
  }
}

@media (max-width: 820px) {
  .ai-workbench {
    height: auto;
    min-height: calc(100vh - 72px);
    overflow: visible;
    grid-template-columns: 1fr;
    padding: 12px;
  }

  .ai-rail {
    max-height: 420px;
  }

  .chat-stage {
    min-height: min(720px, calc(100vh - 120px));
  }

  .message-stack {
    max-width: 86%;
  }

  .composer {
    grid-template-columns: 1fr;
  }

  .send-button {
    width: 100%;
  }

  .composer-toolbar {
    flex-wrap: wrap;
  }

  .material-actions {
    width: 100%;
  }

  .resource-panel {
    left: 0;
    right: 0;
  }

  .local-save-panel {
    left: 0;
    right: 0;
    width: auto;
  }

  .save-option-grid {
    grid-template-columns: 1fr;
  }

  .temperature-pill {
    width: 100%;
    justify-content: space-between;
  }

  .temperature-pill input {
    flex: 1;
    width: auto;
  }
}
</style>
