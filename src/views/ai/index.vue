<template>
  <div class="ai-workbench" :class="{ entered }">
    <!-- 左侧边栏：品牌 + 对话列表 -->
    <aside class="ai-rail">
      <div class="rail-brand">
        <span class="brand-mark"><Bot /></span>
        <div>
          <b>DeepInsight AI</b>
          <em>分析上下文与会话记录</em>
        </div>
      </div>

      <button class="new-chat-btn" type="button" @click="newConversation">
        <Plus :size="18" />
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
          <i @click.stop="deleteConversation(conv.id)"><X :size="14" /></i>
        </button>
      </section>

      <!-- 底部上下文概览 -->
      <div class="rail-footer">
        <div class="context-badge">
          <strong>{{ activeReasoningLabel }}</strong>
          <span>{{ tokenCount }} tokens</span>
        </div>
        <button type="button" class="context-toggle" @click="showContextPanel = !showContextPanel">
          <Bot /> 分析能力
        </button>
      </div>
    </aside>

    <!-- 主对话区 -->
    <main class="chat-stage">
      <header class="stage-header">
        <div>
          <span>AI 分析对话</span>
          <h1>{{ activeConv?.title || '新的分析会话' }}</h1>
        </div>
        <div class="stage-actions">
          <button type="button" @click="exportChat" title="导出对话"><Download /></button>
          <button type="button" @click="clearCurrentChat" title="清空视图"><Trash2 /></button>
        </div>
      </header>

      <!-- 云端素材入口（可折叠） -->
      <div class="cloud-toggle-row">
        <button type="button" class="cloud-toggle-btn" :class="{ open: cloudPanelOpen }" @click="cloudPanelOpen = !cloudPanelOpen">
          <FolderOpen :size="14" />
          <span>云端素材</span>
          <em>{{ cloudPanelOpen ? '收起' : '展开' }}</em>
        </button>
        <div v-if="cloudPanelOpen" class="cloud-panel-wrap">
          <CloudWorkspacePortal
            variant="ai"
            compact
            selectable
            title="云端上下文"
            subtitle="从素材库选择模型、数据、矩阵记录加入分析"
            @select="addCloudMaterial"
          />
        </div>
      </div>

      <div ref="msgContainer" class="chat-messages">
        <!-- 空状态 -->
        <div v-if="messages.length === 0" class="empty-state">
          <div class="empty-orb"><Bot /></div>
          <h2>把训练上下文带进对话，做可追踪分析</h2>
          <p>导入可视化矩阵的训练分析记录，或直接发起模型诊断、代码建议和实验计划。</p>
          <div class="prompt-grid">
            <button v-for="prompt in suggestedPrompts" :key="prompt.label" type="button" @click="askAI(prompt.query)">
              <span><component :is="prompt.icon" :size="16" /></span>
              <strong>{{ prompt.label }}</strong>
              <em>{{ prompt.desc }}</em>
            </button>
          </div>
        </div>

        <!-- 消息列表 -->
        <article v-for="(msg, index) in messages" :key="index" class="message-row" :class="msg.role">
          <div v-if="msg.role !== 'user'" class="avatar"><Bot /></div>
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

        <!-- 加载中 -->
        <article v-if="loading" class="message-row assistant">
          <div class="avatar"><Bot /></div>
          <div class="message-stack">
            <div v-if="deepThink" class="thinking-live">
              <span></span>
              {{ activeReasoningLabel }} 推理中
            </div>
            <div class="typing-bubble"><i></i><i></i><i></i></div>
          </div>
        </article>
      </div>

      <!-- 输入区 -->
      <form class="composer" @submit.prevent="sendMessage">
        <!-- 已选材料 -->
        <div v-if="selectedMaterials.length" class="material-strip">
          <span class="material-label">已选 {{ selectedMaterials.length }} 项</span>
          <button
            v-for="item in selectedMaterials"
            :key="materialKey(item)"
            type="button"
            class="material-chip"
            @click="removeMaterial(item)"
          >
            <span>{{ materialTypeLabel(item) }}</span>
            <strong>{{ materialTitle(item) }}</strong>
          </button>
          <button type="button" class="material-clear" @click="clearMaterials">清空</button>
        </div>

        <div class="composer-shell">
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

          <div class="composer-bar">
            <div class="bar-left">
              <input ref="fileInputRef" type="file" multiple hidden @change="handleFileInput" />
              <div ref="actionMenuRef" class="action-trigger-wrap" :class="{ open: actionMenuOpen }">
                <button type="button" class="bar-btn" @click.stop="actionMenuOpen = !actionMenuOpen">
                  <Plus :size="14" /> 添加
                </button>
                <div v-show="actionMenuOpen" class="action-drop">
                  <button type="button" :disabled="materialBusy" @click.stop="fileInputRef?.click(); actionMenuOpen = false">
                    <FileUp :size="14" /> 上传文件
                  </button>
                  <button type="button" :disabled="materialBusy" @click.stop="toggleResourcePanel(); actionMenuOpen = false">
                    <FolderOpen :size="14" /> 站内资源
                  </button>
                  <button type="button" :disabled="materialBusy || !selectedMaterials.length" @click.stop="saveCurrentMaterials(); actionMenuOpen = false">
                    <Save :size="14" /> 云端保存
                  </button>
                  <button type="button" :disabled="!selectedMaterials.length" @click.stop="localSavePanelOpen = !localSavePanelOpen; actionMenuOpen = false">
                    <Download :size="14" /> 本地导出
                  </button>
                </div>
              </div>
              <span v-if="selectedMaterials.length" class="material-count">{{ selectedMaterials.length }} 项素材</span>
            </div>

            <div class="bar-right">
              <div ref="reasoningMenuRef" class="reasoning-trigger-wrap" :class="{ open: reasoningMenuOpen }">
                <button type="button" class="bar-btn" @click.stop="toggleReasoningMenu">
                  <Brain :size="14" /> {{ activeReasoningLabel }}
                </button>
                <div v-show="reasoningMenuOpen" class="reasoning-drop">
                  <button
                    v-for="level in reasoningLevels"
                    :key="level.value"
                    type="button"
                    :class="{ active: reasoningLevel === level.value }"
                    @click.stop="selectReasoningLevel(level.value)"
                  >
                    <strong>{{ level.label }}</strong>
                    <em>{{ level.hint }}</em>
                  </button>
                </div>
              </div>

              <label class="temp-control">
                <span>{{ temperature.toFixed(2) }}</span>
                <input v-model.number="temperature" type="range" min="0" max="1" step="0.05" />
              </label>
            </div>
          </div>

          <!-- 站内资源面板 -->
          <div v-if="resourcePanelOpen" class="resource-panel">
            <div class="rp-head">
              <strong>选择站内资源</strong>
              <div>
                <button type="button" @click="loadWorkspaceResources">刷新</button>
                <button type="button" @click="resourcePanelOpen = false">关闭</button>
              </div>
            </div>
            <div class="rp-grid">
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

          <!-- 本地保存面板 -->
          <div v-if="localSavePanelOpen" class="local-save-panel">
            <div class="rp-head">
              <strong>本地保存</strong>
              <button type="button" @click="localSavePanelOpen = false">关闭</button>
            </div>
            <div class="lp-grid">
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
              <button type="button" @click="saveMaterialsToLocal">导出</button>
            </div>
          </div>
        </div>

        <button v-if="!loading" class="send-button" type="submit" :disabled="!userInput.trim()">
          <Send />
        </button>
        <button v-else class="send-button stopping" type="button" @click="stopGeneration">
          <Square />
        </button>
      </form>
    </main>

    <!-- 分析能力浮窗 -->
    <Transition name="panel-slide">
      <aside v-if="showContextPanel" class="context-overlay">
        <div class="co-head">
          <h3>分析能力</h3>
          <button type="button" @click="showContextPanel = false"><X :size="14" /></button>
        </div>
        <section>
          <span>当前调用</span>
          <h4>{{ activeReasoningLabel }}</h4>
          <p>{{ activeReasoningDesc }}</p>
          <div class="capability-list">
            <b>MySQL 保存分析记录</b>
            <b>Redis 保存对话历史</b>
            <b>用户隔离校验</b>
            <b>可视化结果导入上下文</b>
          </div>
        </section>
        <section>
          <span>上下文</span>
          <div class="context-meter">
            <strong>{{ tokenCount }}</strong>
            <em>估算 tokens / chars</em>
          </div>
        </section>
      </aside>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Bot, Brain, Clipboard, Download, FileUp, FolderOpen, MoreHorizontal, Plus, Save, Send, Square, Trash2, User, X } from 'lucide-vue-next'
import { aiApi } from '@/api'
import { renderMarkdown } from '@/utils/markdown'
import CloudWorkspacePortal from '@/components/cloud/CloudWorkspacePortal.vue'

type ReasoningLevel = 'off' | 'quick' | 'low' | 'deep' | 'max'
type Message = {
  role: string; content: string; time: string
  thinking?: string; thinkingExpanded?: boolean; reasoningLabel?: string
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
const actionMenuOpen = ref(false)
const actionMenuRef = ref<HTMLElement | null>(null)
const resourcePanelOpen = ref(false)
const localSavePanelOpen = ref(false)
const materialBusy = ref(false)
const selectedMaterials = ref<WorkspaceMaterial[]>([])
const workspaceResources = ref<WorkspaceMaterial[]>([])
const localSaveKind = ref<'summary' | 'json'>('summary')
const localSaveFormat = ref<'md' | 'word' | 'json'>('md')
const showContextPanel = ref(false)
const cloudPanelOpen = ref(false)

const reasoningLevels: Array<{ value: ReasoningLevel; label: string; hint: string; desc: string }> = [
  { value: 'off', label: '关闭', hint: '直接回答', desc: '不请求模型推理能力，适合轻量问答。' },
  { value: 'quick', label: '快速', hint: 'minimal', desc: '低延迟推理，用于小修小问。' },
  { value: 'low', label: '标准', hint: 'low', desc: '平衡速度与分析质量，适合普通诊断。' },
  { value: 'deep', label: '深度', hint: 'high', desc: '调用高强度 reasoning，适合训练异常、矩阵复盘。' },
  { value: 'max', label: '极限', hint: 'xhigh', desc: '最大上下文与推理预算，适合复杂多步分析。' },
]

const suggestedPrompts = [
  { icon: Bot, label: '模型分析', desc: '结构、瓶颈、适用场景', query: '请分析当前模型结构的优缺点，并给出下一步优化建议。' },
  { icon: Brain, label: '训练诊断', desc: '损失、准确率、学习率', query: '训练损失不稳定时，应该如何系统排查？请按优先级输出。' },
  { icon: Clipboard, label: '矩阵复盘', desc: '多模型 × 多模块', query: '请帮我设计一次多模型多分析模块的复盘流程，输出可执行步骤。' },
  { icon: FileUp, label: '代码方案', desc: '实验脚本和伪代码', query: '给我一个 PyTorch 训练诊断脚本模板，包含日志记录和异常检测。' },
]

const activeConv = computed(() => conversations.value.find((item) => item.id === activeConvId.value))
const deepThink = computed(() => reasoningLevel.value !== 'off')
const activeReasoning = computed(() => reasoningLevels.find((item) => item.value === reasoningLevel.value) || reasoningLevels[0])
const activeReasoningLabel = computed(() => activeReasoning.value.label)
const activeReasoningDesc = computed(() => activeReasoning.value.desc)

// ====== 以下所有逻辑函数保持不变 ======

onMounted(async () => {
  setTimeout(() => { entered.value = true }, 80)
  document.addEventListener('click', closeMenusOnOutside)
  await loadConversations()
  await loadWorkspaceResources()
  const routeConversationId = Number(route.query.conversationId)
  if (Number.isFinite(routeConversationId) && routeConversationId > 0) {
    const existing = conversations.value.find((conv) => conv.id === routeConversationId)
    if (existing) { await selectConversation(existing) }
    else { await selectConversation({ id: routeConversationId, title: `分析对话 #${routeConversationId}`, date: '' }) }
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

onBeforeUnmount(() => { document.removeEventListener('click', closeMenusOnOutside) })

function getTime() { const d = new Date(); return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}` }
async function scrollToBottom() { await nextTick(); if (msgContainer.value) msgContainer.value.scrollTop = msgContainer.value.scrollHeight }
function autoResize() { if (!inputRef.value) return; inputRef.value.style.height = 'auto'; inputRef.value.style.height = `${Math.min(inputRef.value.scrollHeight, 140)}px` }
function formatContent(text: string) { return text ? renderMarkdown(text) : '' }
function toggleReasoningMenu() { reasoningMenuOpen.value = !reasoningMenuOpen.value }
function selectReasoningLevel(level: ReasoningLevel) { reasoningLevel.value = level; reasoningMenuOpen.value = false }
function closeMenusOnOutside(event: MouseEvent) { const target = event.target as Node | null; if (reasoningMenuOpen.value && target && !reasoningMenuRef.value?.contains(target)) { reasoningMenuOpen.value = false } if (actionMenuOpen.value && target && !actionMenuRef.value?.contains(target)) { actionMenuOpen.value = false } }

async function sendMessage() {
  if (!userInput.value.trim() || loading.value) return
  const msg = userInput.value.trim(); userInput.value = ''
  if (inputRef.value) inputRef.value.style.height = 'auto'
  messages.value.push({ role: 'user', content: msg, time: getTime() }); chatHistory.value.push({ role: 'user', content: msg })
  loading.value = true; tokenCount.value += msg.length; await scrollToBottom()
  try {
    thinking.value = true
    const response = await aiApi.chat({ message: msg, history: chatHistory.value.slice(-20), temperature: temperature.value, deepThink: deepThink.value, reasoningLevel: reasoningLevel.value,
      attachments: selectedMaterials.value.filter((item) => ['file', 'image', 'upload'].includes(String(item.itemType || item.sourceType))),
      resources: selectedMaterials.value.filter((item) => !['file', 'image', 'upload'].includes(String(item.itemType || item.sourceType))),
      ...(conversationId.value ? { conversationId: conversationId.value } : {}),
    })
    thinking.value = false; const data = response.data.data
    if (data.conversationId) { conversationId.value = data.conversationId
      if (!activeConvId.value) { const newConv = { id: data.conversationId, title: msg.slice(0, 28) + (msg.length > 28 ? '...' : ''), date: new Date().toLocaleDateString('zh-CN') }; conversations.value.unshift(newConv); activeConvId.value = newConv.id } }
    const reply = data.reply || data.message?.content || 'AI 服务暂未返回内容。'; const thinkingText = data.reasoning || ''
    messages.value.push({ role: 'assistant', content: reply, time: getTime(), thinking: thinkingText, thinkingExpanded: Boolean(thinkingText), reasoningLabel: activeReasoningLabel.value })
    chatHistory.value.push({ role: 'assistant', content: reply }); tokenCount.value += reply.length; selectedMaterials.value = []
  } catch (error: any) { thinking.value = false; messages.value.push({ role: 'assistant', content: `AI 服务暂时不可用：${error?.response?.data?.message || error.message || '未知错误'}`, time: getTime() }) }
  finally { loading.value = false; await scrollToBottom() }
}

function materialKey(item: WorkspaceMaterial) { return `${item.type || item.sourceType || item.itemType || 'material'}:${item.id || item.fileUrl || item.title}` }
function materialTitle(item: WorkspaceMaterial) { return String(item.title || item.fileName || item.name || '未命名材料') }
function materialTypeLabel(item: WorkspaceMaterial) { const type = String(item.type || item.itemType || item.sourceType || 'resource'); const labels: Record<string, string> = { training: '训练', dataset: '数据', model: '模型', run: '运行', analysis_result: '矩阵', saved_view: '视图', image: '图片', file: '文件', upload: '上传', text: '文本' }; return labels[type] || '资源' }
function isMaterialSelected(item: WorkspaceMaterial) { return selectedMaterials.value.some((selected) => materialKey(selected) === materialKey(item)) }
function toggleMaterial(item: WorkspaceMaterial) { isMaterialSelected(item) ? removeMaterial(item) : selectedMaterials.value = [...selectedMaterials.value, item] }
function addCloudMaterial(item: WorkspaceMaterial) { if (!isMaterialSelected(item)) selectedMaterials.value = [...selectedMaterials.value, item] }
function removeMaterial(item: WorkspaceMaterial) { selectedMaterials.value = selectedMaterials.value.filter((selected) => materialKey(selected) !== materialKey(item)) }
function clearMaterials() { selectedMaterials.value = []; localSavePanelOpen.value = false }
async function loadWorkspaceResources() { try { const response = await aiApi.listWorkspaceResources(); workspaceResources.value = response.data.data || [] } catch { workspaceResources.value = [] } }
async function toggleResourcePanel() { resourcePanelOpen.value = !resourcePanelOpen.value; if (resourcePanelOpen.value && !workspaceResources.value.length) await loadWorkspaceResources() }

async function handleFileInput(event: Event) { const input = event.target as HTMLInputElement; const files = Array.from(input.files || []); input.value = ''; if (!files.length) return; materialBusy.value = true; try { const response = await aiApi.uploadWorkspaceFiles(files); selectedMaterials.value = [...selectedMaterials.value, ...(response.data.data || [])]; ElMessage.success(`已上传 ${(response.data.data || []).length} 个素材`) } catch (error: any) { ElMessage.error(error?.response?.data?.message || '上传失败') } finally { materialBusy.value = false } }

async function saveCurrentMaterials() { if (!selectedMaterials.value.length) return; materialBusy.value = true; try { await aiApi.saveWorkspaceItem({ title: `AI 分析素材 ${new Date().toLocaleString('zh-CN')}`, itemType: 'context_bundle', sourceType: 'ai_chat', format: 'json', content: buildMaterialsMarkdown(), payload: { materials: selectedMaterials.value } }); await loadWorkspaceResources(); ElMessage.success('已保存到云端素材库') } catch (error: any) { ElMessage.error(error?.response?.data?.message || '保存失败') } finally { materialBusy.value = false } }

function buildMaterialsMarkdown() { const lines = ['# DeepInsight AI 分析材料', '', `- 材料数量：${selectedMaterials.value.length}`, `- 生成时间：${new Date().toLocaleString('zh-CN')}`, '', '## 材料清单']; selectedMaterials.value.forEach((item, index) => { lines.push('', `### ${index + 1}. ${materialTitle(item)}`, '', `- 类型：${materialTypeLabel(item)}`, `- 来源：${item.sourceType || item.type || item.itemType || '-'}`, `- 格式：${item.format || item.mimeType || '-'}`, `- 摘要：${item.summary || item.meta || item.content || '-'}`); if (item.fileUrl) lines.push(`- 文件地址：${item.fileUrl}`) }); return lines.join('\n') }
function markdownToWordHtml(markdown: string, title: string) { const body = markdown.split('\n').map((line) => { if (line.startsWith('# ')) return `<h1>${escapeHtml(line.slice(2))}</h1>`; if (line.startsWith('## ')) return `<h2>${escapeHtml(line.slice(3))}</h2>`; if (line.startsWith('### ')) return `<h3>${escapeHtml(line.slice(4))}</h3>`; if (line.startsWith('- ')) return `<p>• ${escapeHtml(line.slice(2))}</p>`; return line ? `<p>${escapeHtml(line)}</p>` : '<br>' }).join(''); return `<!doctype html><html><head><meta charset="utf-8"><title>${escapeHtml(title)}</title></head><body>${body}</body></html>` }
function escapeHtml(value: string) { return value.replace(/[&<>"']/g, (char) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[char] || char)) }
function downloadBlob(content: BlobPart, filename: string, type: string) { const blob = new Blob([content], { type }); const url = URL.createObjectURL(blob); const link = document.createElement('a'); link.href = url; link.download = filename; link.click(); URL.revokeObjectURL(url) }

function saveMaterialsToLocal() { if (!selectedMaterials.value.length) return; const stamp = Date.now(); const markdown = buildMaterialsMarkdown(); if (localSaveFormat.value === 'json' || localSaveKind.value === 'json') { downloadBlob(JSON.stringify({ materials: selectedMaterials.value, exportedAt: new Date().toISOString() }, null, 2), `deepinsight-ai-materials-${stamp}.json`, 'application/json;charset=utf-8') } else if (localSaveFormat.value === 'word') { downloadBlob(markdownToWordHtml(markdown, 'DeepInsight AI 分析材料'), `deepinsight-ai-materials-${stamp}.doc`, 'application/msword;charset=utf-8') } else { downloadBlob(markdown, `deepinsight-ai-materials-${stamp}.md`, 'text/markdown;charset=utf-8') }; localSavePanelOpen.value = false; ElMessage.success('已导出本地文件') }

function stopGeneration() { loading.value = false; ElMessage.info('已停止生成') }
function askAI(query: string) { userInput.value = query; void sendMessage() }
function copyMsg(text: string) { navigator.clipboard.writeText(text); ElMessage.success('已复制') }
function regenerate() { const lastUser = [...chatHistory.value].reverse().find((item) => item.role === 'user'); if (!lastUser) return; messages.value = messages.value.filter((_, index) => index < messages.value.length - 1); chatHistory.value = chatHistory.value.filter((_, index) => index < chatHistory.value.length - 1); userInput.value = lastUser.content; void sendMessage() }
function newConversation() { messages.value = []; chatHistory.value = []; conversationId.value = null; activeConvId.value = null; userInput.value = ''; tokenCount.value = 0; messages.value.push({ role: 'assistant', content: '新对话已开始。你可以描述问题，也可以从可视化矩阵导入分析结果。', time: getTime() }); nextTick(() => inputRef.value?.focus()) }

async function loadConversations() { try { const response = await aiApi.listConversations(); if (response.data.code === 200 && response.data.data) { conversations.value = response.data.data.map((item: any) => ({ id: Number(item.id), title: item.title || '对话', date: item.createdAt || '' })) } } catch { conversations.value = [] } }

async function selectConversation(conv: Conversation) { activeConvId.value = conv.id; conversationId.value = conv.id; try { const response = await aiApi.getMessages(conv.id); if (response.data.code === 200 && response.data.data) { messages.value = response.data.data.map((item: any) => ({ role: item.role, content: item.content, time: '' })); chatHistory.value = response.data.data.map((item: any) => ({ role: item.role, content: item.content })); tokenCount.value = chatHistory.value.reduce((sum: number, item: any) => sum + item.content.length, 0); await scrollToBottom() } } catch (error: any) { ElMessage.error(error?.response?.data?.message || '无法加载该对话') } }

async function deleteConversation(id: number) { await aiApi.deleteConversation(id).catch(() => {}); conversations.value = conversations.value.filter((item) => item.id !== id); if (activeConvId.value === id) newConversation() }
function clearCurrentChat() { messages.value = []; chatHistory.value = []; tokenCount.value = 0; ElMessage.success('当前视图已清空，Redis 历史未删除') }

function exportChat() { if (!messages.value.length) { ElMessage.warning('暂无内容'); return }; const text = messages.value.map((item) => `[${item.time}] ${item.role === 'user' ? '我' : 'AI'}: ${item.content}`).join('\n\n'); downloadBlob(text, `deepinsight-chat-${Date.now()}.txt`, 'text/plain'); ElMessage.success('已导出') }
</script>

<style scoped>
/* ====== 整体布局 ====== */
.ai-workbench {
  height: calc(100vh - 72px);
  min-height: 640px;
  display: grid;
  grid-template-columns: 272px minmax(0, 1fr);
  gap: 16px;
  padding: 18px;
  overflow: hidden;
  opacity: 0;
  transform: translateY(12px);
  transition: opacity 0.45s ease, transform 0.45s ease;
}
.ai-workbench.entered { opacity: 1; transform: translateY(0); }

/* ====== 左侧边栏 ====== */
.ai-rail {
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  padding: 18px;
  border: 1px solid var(--border-color);
  border-radius: 20px;
  background: var(--workbench-shell-bg);
  backdrop-filter: blur(16px);
}

.rail-brand { display: flex; align-items: center; gap: 12px; padding: 0 0 14px; }
.brand-mark { width: 40px; height: 40px; display: grid; place-items: center; border-radius: 14px; background: rgba(var(--primary-rgb), 0.14); color: var(--primary-color); }
.rail-brand b { display: block; color: var(--text-primary); font-size: 15px; font-weight: 600; }
.rail-brand em { color: var(--text-muted); font-size: 11px; font-style: normal; }

.new-chat-btn {
  height: 42px; margin: 4px 0 16px; border-radius: 14px; display: inline-flex; align-items: center; justify-content: center; gap: 8px;
  border: 0; background: var(--primary-color); color: #06100c; font-size: 13px; font-weight: 600;
  cursor: pointer; box-shadow: 0 4px 16px rgba(var(--primary-rgb), 0.2);
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}
.new-chat-btn:hover { transform: translateY(-1px); box-shadow: 0 6px 24px rgba(var(--primary-rgb), 0.28); }

.conversation-list { flex: 1; min-height: 0; overflow-y: auto; }
.section-head { display: flex; justify-content: space-between; padding: 4px 2px 10px; color: var(--text-muted); font-size: 11px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.06em; }

.conversation-item {
  position: relative; width: 100%; display: grid; gap: 3px; margin-bottom: 6px; padding: 10px 36px 10px 12px;
  border: 1px solid transparent; border-radius: 14px; text-align: left; background: transparent; color: var(--text-secondary);
  font: inherit; cursor: pointer; transition: background 0.15s ease, border-color 0.15s ease;
}
.conversation-item:hover { background: rgba(255,255,255,0.04); }
.conversation-item.active { border-color: rgba(255,255,255,0.14); background: rgba(var(--primary-rgb), 0.08); color: var(--text-primary); }
.conversation-item span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 13px; font-weight: 550; }
.conversation-item em { color: var(--text-muted); font-size: 11px; font-style: normal; }
.conversation-item i { position: absolute; top: 8px; right: 8px; width: 22px; height: 22px; display: grid; place-items: center; border-radius: 8px; opacity: 0; transition: opacity 0.15s; }
.conversation-item:hover i { opacity: 0.7; }

/* 侧栏底部 */
.rail-footer { padding-top: 14px; border-top: 1px solid var(--border-color); display: grid; gap: 8px; }
.context-badge { display: flex; justify-content: space-between; align-items: center; }
.context-badge strong { color: var(--primary-color); font-size: 12px; font-weight: 600; }
.context-badge span { color: var(--text-muted); font-size: 11px; }
.context-toggle { height: 34px; border: 1px solid var(--border-color); border-radius: 12px; background: transparent; color: var(--text-secondary); font-size: 12px; cursor: pointer; display: flex; align-items: center; justify-content: center; gap: 6px; transition: background 0.15s; }
.context-toggle:hover { background: rgba(255,255,255,0.04); }

/* ====== 主对话区 ====== */
.chat-stage {
  min-width: 0; min-height: 0; display: grid; grid-template-rows: auto auto minmax(0, 1fr) auto;
  border: 1px solid var(--border-color); border-radius: 20px; background: var(--workbench-shell-bg); backdrop-filter: blur(16px);
  overflow: hidden;
}

.stage-header { display: flex; align-items: center; justify-content: space-between; gap: 14px; padding: 18px 22px; border-bottom: 1px solid var(--border-color); }
.stage-header span { color: var(--text-muted); font-size: 10px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.1em; }
.stage-header h1 { margin: 4px 0 0; color: var(--text-primary); font-size: 20px; font-weight: 600; }
.stage-actions { display: flex; gap: 6px; }
.stage-actions button { width: 34px; height: 34px; display: grid; place-items: center; border: 1px solid var(--border-color); border-radius: 12px; background: transparent; color: var(--text-secondary); cursor: pointer; transition: background 0.15s, color 0.15s; }
.stage-actions button:hover { background: rgba(255,255,255,0.06); color: var(--text-primary); }

/* 云端入口 */
.cloud-toggle-row { margin: 0 20px 8px; }
.cloud-toggle-btn { width: 100%; height: 36px; display: flex; align-items: center; gap: 8px; padding: 0 14px; border: 1px solid var(--border-color); border-radius: 12px; background: transparent; color: var(--text-secondary); font-size: 12px; cursor: pointer; transition: background 0.15s; }
.cloud-toggle-btn:hover { background: rgba(255,255,255,0.03); }
.cloud-toggle-btn span { flex: 1; text-align: left; font-weight: 550; }
.cloud-toggle-btn em { color: var(--text-muted); font-size: 10px; font-style: normal; }
.cloud-panel-wrap { margin-top: 8px; }
.cloud-panel-wrap :deep(.cloud-workspace-portal) { max-height: 320px; overflow-y: auto; }

/* 操作下拉 */
.action-trigger-wrap { position: relative; }
.action-drop { position: absolute; left: 0; bottom: calc(100% + 6px); z-index: 38; min-width: 160px; display: grid; gap: 2px; padding: 6px; border: 1px solid rgba(255,255,255,0.12); border-radius: 14px; background: var(--workbench-overlay-bg); backdrop-filter: blur(16px); box-shadow: 0 12px 40px rgba(0,0,0,0.28); }
.action-drop button { height: 34px; display: flex; align-items: center; gap: 8px; padding: 0 10px; border: 0; border-radius: 9px; background: transparent; color: var(--text-secondary); font-size: 12px; cursor: pointer; white-space: nowrap; }
.action-drop button:hover { background: rgba(255,255,255,0.05); color: var(--text-primary); }
.action-drop button:disabled { opacity: 0.35; cursor: not-allowed; }
.material-count { color: var(--text-muted); font-size: 11px; margin-left: 6px; }

/* ====== 消息区 ====== */
.chat-messages {
  min-height: 0; overflow-y: auto; padding: 24px 22px; display: flex; flex-direction: column; gap: 20px;
  background-image: radial-gradient(rgba(255,255,255,0.03) 1px, transparent 1px); background-size: 22px 22px;
}

.empty-state { min-height: 100%; display: grid; place-items: center; align-content: center; text-align: center; }
.empty-orb { width: 64px; height: 64px; margin: 0 auto 20px; display: grid; place-items: center; border-radius: 18px; background: rgba(var(--primary-rgb), 0.12); color: var(--primary-color); font-size: 28px; }
.empty-state h2 { margin: 0 0 8px; color: var(--text-primary); font-size: 22px; font-weight: 600; }
.empty-state p { max-width: 560px; margin: 0 auto 22px; color: var(--text-secondary); font-size: 13px; line-height: 1.7; }
.prompt-grid { width: min(560px, 100%); display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }
.prompt-grid button { min-height: 96px; padding: 16px; border: 1px solid rgba(255,255,255,0.08); border-radius: 16px; text-align: left; background: var(--workbench-panel-bg); color: var(--text-primary); cursor: pointer; transition: border-color 0.2s, transform 0.2s; }
.prompt-grid button:hover { border-color: rgba(255,255,255,0.18); transform: translateY(-1px); }
.prompt-grid span { display: inline-grid; width: 28px; height: 28px; place-items: center; margin-bottom: 10px; border-radius: 10px; background: rgba(var(--primary-rgb), 0.12); color: var(--primary-color); font-weight: 600; font-size: 13px; }
.prompt-grid strong { display: block; font-size: 14px; font-weight: 600; margin-bottom: 4px; }
.prompt-grid em { color: var(--text-muted); font-size: 11px; font-style: normal; }

/* 消息 */
.message-row { display: flex; align-items: flex-start; gap: 10px; }
.message-row.user { justify-content: flex-end; }
.avatar { width: 34px; height: 34px; flex: 0 0 auto; display: grid; place-items: center; border-radius: 12px; background: rgba(var(--primary-rgb), 0.1); color: var(--primary-color); }
.avatar.user { background: var(--workbench-panel-bg); color: var(--text-secondary); }
.message-stack { max-width: min(720px, 78%); }
.message-bubble { padding: 14px 18px; border: 1px solid var(--border-color); border-radius: 18px; background: var(--workbench-panel-bg); color: var(--text-primary); font-size: 13px; line-height: 1.75; }
.message-row.user .message-bubble { border-color: transparent; background: var(--primary-color); color: #06100c; }
.msg-actions { display: flex; align-items: center; gap: 8px; margin-top: 4px; padding: 0 4px; color: var(--text-muted); font-size: 10px; }
.msg-actions button { background: transparent; border: 0; color: var(--text-muted); font-size: 10px; cursor: pointer; }
.msg-actions button:hover { color: var(--text-primary); }

/* 推理块 */
.thinking-block { width: 100%; margin-bottom: 8px; padding: 10px 14px; border: 1px solid rgba(255,255,255,0.08); border-radius: 14px; text-align: left; background: rgba(var(--primary-rgb), 0.06); color: var(--text-secondary); cursor: pointer; }
.thinking-block span, .thinking-live { color: var(--primary-color); font-size: 12px; font-weight: 600; }
.thinking-block b { float: right; color: var(--text-muted); font-size: 10px; font-weight: 500; }
.thinking-block pre { margin: 10px 0 0; white-space: pre-wrap; color: var(--text-secondary); font-size: 12px; line-height: 1.6; }
.thinking-live { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.thinking-live span { width: 12px; height: 12px; border: 2px solid rgba(var(--primary-rgb), 0.2); border-top-color: var(--primary-color); border-radius: 999px; animation: spin 800ms linear infinite; }
.typing-bubble { width: 68px; padding: 12px 16px; border-radius: 16px; background: var(--workbench-panel-bg); }
.typing-bubble i { display: inline-block; width: 6px; height: 6px; margin-right: 4px; border-radius: 999px; background: var(--primary-color); animation: typing 1.1s infinite ease-in-out; }
.typing-bubble i:nth-child(2) { animation-delay: 140ms; }
.typing-bubble i:nth-child(3) { animation-delay: 280ms; }

/* ====== 输入区 ====== */
.composer { display: grid; grid-template-columns: 1fr 48px; align-items: end; gap: 10px; padding: 14px 18px 18px; border-top: 1px solid var(--border-color); }
.composer-shell { position: relative; display: grid; gap: 6px; min-width: 0; border: 1px solid rgba(255,255,255,0.1); border-radius: 18px; background: var(--bg-input); transition: border-color 0.18s; }
.composer-shell:focus-within { border-color: rgba(255,255,255,0.2); }
.composer textarea { min-height: 48px; max-height: 128px; resize: none; border: 0; border-radius: 18px 18px 8px 8px; padding: 14px 16px 4px; outline: none; background: transparent; color: var(--text-primary); font-size: 13px; line-height: 1.5; }
.composer textarea:focus { box-shadow: none; }

/* 已选材料 */
.material-strip { display: flex; flex-wrap: wrap; align-items: center; gap: 6px; padding: 0 2px; }
.material-label { color: var(--text-muted); font-size: 11px; font-weight: 600; margin-right: 4px; }
.material-chip { height: 28px; display: inline-flex; align-items: center; gap: 5px; padding: 0 10px; border: 1px solid rgba(255,255,255,0.1); border-radius: 999px; background: var(--workbench-panel-bg); color: var(--text-secondary); font-size: 11px; cursor: pointer; }
.material-chip span { color: var(--primary-color); font-size: 10px; font-weight: 600; }
.material-chip strong { max-width: 140px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: var(--text-primary); }
.material-clear { height: 28px; padding: 0 10px; border: 0; border-radius: 999px; background: rgba(248,113,113,0.12); color: #fca5a5; font-size: 10px; cursor: pointer; }

/* 工具栏 */
.composer-bar { display: flex; align-items: center; justify-content: space-between; gap: 8px; padding: 0 8px 8px; }
.bar-left, .bar-right { display: flex; align-items: center; gap: 5px; }
.bar-btn { height: 28px; padding: 0 10px; border: 1px solid rgba(255,255,255,0.08); border-radius: 999px; background: transparent; color: var(--text-secondary); font-size: 11px; cursor: pointer; white-space: nowrap; transition: background 0.15s, border-color 0.15s; }
.bar-btn:hover { background: rgba(255,255,255,0.05); border-color: rgba(255,255,255,0.16); }
.bar-btn:disabled { opacity: 0.4; cursor: not-allowed; }

/* 推理下拉 */
.reasoning-trigger-wrap { position: relative; }
.reasoning-drop { position: absolute; left: 0; bottom: calc(100% + 8px); z-index: 40; width: 200px; display: grid; gap: 4px; padding: 8px; border: 1px solid rgba(255,255,255,0.12); border-radius: 16px; background: var(--workbench-overlay-bg); backdrop-filter: blur(16px); box-shadow: 0 16px 48px rgba(0,0,0,0.32); }
.reasoning-drop button { height: 36px; display: flex; align-items: center; justify-content: space-between; padding: 0 12px; border: 1px solid transparent; border-radius: 10px; background: transparent; color: var(--text-secondary); font-size: 12px; cursor: pointer; }
.reasoning-drop button:hover { background: rgba(255,255,255,0.04); }
.reasoning-drop button.active { border-color: rgba(var(--primary-rgb), 0.2); background: rgba(var(--primary-rgb), 0.08); color: var(--primary-color); }
.reasoning-drop em { color: var(--text-muted); font-size: 10px; font-style: normal; }

/* 温度 */
.temp-control { display: flex; align-items: center; gap: 6px; cursor: pointer; }
.temp-control span { color: var(--primary-color); font-size: 11px; font-weight: 600; min-width: 32px; text-align: right; }
.temp-control input { width: 64px; accent-color: var(--primary-color); }

/* 资源面板 */
.resource-panel { position: absolute; left: 4px; right: 4px; bottom: calc(100% + 8px); z-index: 35; max-height: 340px; overflow: hidden; display: grid; grid-template-rows: auto 1fr; padding: 14px; border: 1px solid rgba(255,255,255,0.12); border-radius: 18px; background: var(--workbench-overlay-bg); backdrop-filter: blur(16px); box-shadow: 0 16px 48px rgba(0,0,0,0.32); }
.rp-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; }
.rp-head strong { color: var(--text-primary); font-size: 13px; font-weight: 600; }
.rp-head button { height: 28px; padding: 0 10px; border: 1px solid rgba(255,255,255,0.1); border-radius: 999px; background: transparent; color: var(--text-secondary); font-size: 11px; cursor: pointer; }
.rp-grid { min-height: 0; overflow-y: auto; display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 8px; }
.rp-grid button { min-height: 80px; display: grid; align-content: start; gap: 4px; padding: 12px; border: 1px solid rgba(255,255,255,0.08); border-radius: 14px; text-align: left; background: var(--workbench-panel-bg); color: var(--text-secondary); cursor: pointer; }
.rp-grid button.active { border-color: rgba(var(--primary-rgb), 0.25); background: rgba(var(--primary-rgb), 0.08); }
.rp-grid span { color: var(--primary-color); font-size: 10px; font-weight: 600; }
.rp-grid strong { overflow: hidden; color: var(--text-primary); font-size: 12px; font-weight: 600; text-overflow: ellipsis; white-space: nowrap; }
.rp-grid em { color: var(--text-muted); font-size: 10px; font-style: normal; line-height: 1.4; }

/* 本地保存面板 */
.local-save-panel { position: absolute; left: 4px; right: 4px; bottom: calc(100% + 8px); z-index: 36; padding: 14px; border: 1px solid rgba(255,255,255,0.12); border-radius: 18px; background: var(--workbench-overlay-bg); backdrop-filter: blur(16px); box-shadow: 0 16px 48px rgba(0,0,0,0.32); }
.lp-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)) auto; gap: 8px; margin-top: 10px; }
.lp-grid label { display: grid; gap: 4px; }
.lp-grid span { color: var(--text-muted); font-size: 10px; font-weight: 600; }
.lp-grid select { height: 36px; border: 1px solid rgba(255,255,255,0.1); border-radius: 10px; background: var(--workbench-control-bg); color: var(--text-primary); padding: 0 10px; outline: none; }
.lp-grid > button { align-self: end; height: 36px; padding: 0 14px; border: 1px solid rgba(var(--primary-rgb), 0.2); border-radius: 10px; background: rgba(var(--primary-rgb), 0.08); color: var(--primary-color); font-size: 12px; cursor: pointer; }

/* 发送按钮 */
.send-button { height: 48px; display: grid; place-items: center; border: 0; border-radius: 16px; background: var(--primary-color); color: #06100c; cursor: pointer; box-shadow: 0 4px 18px rgba(var(--primary-rgb), 0.24); transition: transform 0.15s, box-shadow 0.15s, opacity 0.15s; }
.send-button:hover { transform: scale(1.04); box-shadow: 0 6px 24px rgba(var(--primary-rgb), 0.32); }
.send-button:disabled { opacity: 0.35; cursor: not-allowed; transform: none; }
.send-button.stopping { background: #f43f5e; }

/* ====== 分析能力浮窗 ====== */
.context-overlay {
  position: fixed; right: 20px; top: 92px; bottom: 20px; width: 280px; z-index: 50;
  padding: 20px; border: 1px solid var(--border-color); border-radius: 20px;
  background: var(--workbench-overlay-bg); backdrop-filter: blur(18px);
  box-shadow: 0 16px 64px rgba(0,0,0,0.28);
  overflow-y: auto; display: grid; align-content: start; gap: 18px;
}
.co-head { display: flex; align-items: center; justify-content: space-between; }
.co-head h3 { margin: 0; color: var(--text-primary); font-size: 16px; font-weight: 600; }
.co-head button { width: 28px; height: 28px; display: grid; place-items: center; border: 0; border-radius: 8px; background: transparent; color: var(--text-muted); cursor: pointer; }

.context-overlay section span { color: var(--text-muted); font-size: 10px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.08em; }
.context-overlay section h4 { margin: 6px 0 4px; color: var(--primary-color); font-size: 18px; font-weight: 600; }
.context-overlay section p { color: var(--text-secondary); font-size: 12px; line-height: 1.6; margin: 0; }
.capability-list { display: grid; gap: 6px; margin-top: 12px; }
.capability-list b { padding: 8px 12px; border-radius: 10px; background: rgba(var(--primary-rgb), 0.08); color: var(--primary-color); font-size: 11px; font-weight: 500; }
.context-meter { margin-top: 8px; padding: 16px; border-radius: 14px; background: var(--workbench-soft-bg); }
.context-meter strong { display: block; color: var(--primary-color); font-size: 28px; font-weight: 600; }
.context-meter em { color: var(--text-muted); font-size: 11px; font-style: normal; }

/* 浮窗动画 */
.panel-slide-enter-active { transition: all 0.25s ease; }
.panel-slide-leave-active { transition: all 0.2s ease; }
.panel-slide-enter-from { opacity: 0; transform: translateX(20px); }
.panel-slide-leave-to { opacity: 0; transform: translateX(20px); }

/* ====== 动画 ====== */
@keyframes spin { to { transform: rotate(360deg); } }
@keyframes typing { 0%, 80%, 100% { opacity: 0.25; transform: translateY(0); } 40% { opacity: 1; transform: translateY(-3px); } }

/* Markdown 代码块 */
.message-bubble :deep(pre) { overflow-x: auto; padding: 12px; border-radius: 12px; background: rgba(0,0,0,0.3); }
.message-bubble :deep(code) { font-family: "JetBrains Mono", Consolas, monospace; }

/* ====== 响应式 ====== */
@media (max-width: 900px) {
  .ai-workbench { height: auto; min-height: calc(100vh - 72px); grid-template-columns: 1fr; padding: 10px; }
  .ai-rail { max-height: 360px; }
  .chat-stage { min-height: min(640px, calc(100vh - 140px)); }
  .message-stack { max-width: 88%; }
  .composer { grid-template-columns: 1fr; }
  .send-button { width: 100%; }
  .composer-bar { flex-wrap: wrap; }
  .bar-left { width: 100%; }
  .prompt-grid { grid-template-columns: 1fr; }
}
</style>
