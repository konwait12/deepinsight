<template>
  <div class="admin-page">
    <section class="admin-hero">
      <div>
        <span>{{ copy.eyebrow }}</span>
        <h2 class="page-title">{{ copy.title }}</h2>
        <p>{{ copy.subtitle }}</p>
      </div>
      <div class="hero-actions">
        <el-button :loading="configLoading" @click="load">{{ copy.refresh }}</el-button>
        <el-button type="primary" @click="openCreate">{{ copy.addConfig }}</el-button>
      </div>
    </section>

    <section class="admin-metric-grid">
      <article>
        <span>{{ copy.totalConfigs }}</span>
        <strong>{{ configs.length }}</strong>
        <small>{{ copy.totalConfigsHint }}</small>
      </article>
      <article>
        <span>{{ copy.activeConfigs }}</span>
        <strong>{{ activeConfigCount }}</strong>
        <small>{{ copy.activeConfigsHint }}</small>
      </article>
      <article>
        <span>{{ copy.trainedDocs }}</span>
        <strong>{{ trainingStatus?.embeddedDocs ?? '-' }}</strong>
        <small>{{ copy.trainedDocsHint }}</small>
      </article>
      <article>
        <span>{{ copy.vectorDimensions }}</span>
        <strong>{{ trainingStatus?.vectorDimensions ?? '-' }}</strong>
        <small>{{ copy.vectorHint }}</small>
      </article>
    </section>

    <section class="training-panel">
      <div>
        <span>{{ copy.knowledgeTraining }}</span>
        <strong>{{ copy.knowledgeTrainingTitle }}</strong>
        <p>{{ copy.knowledgeTrainingDesc }}</p>
      </div>
      <div class="training-metrics">
        <article>
          <span>{{ copy.modelCount }}</span>
          <strong>{{ trainingStatus?.modelCount ?? '-' }}</strong>
        </article>
        <article>
          <span>{{ copy.trainedDocs }}</span>
          <strong>{{ trainingStatus?.embeddedDocs ?? '-' }}</strong>
        </article>
        <article>
          <span>{{ copy.vectorDimensions }}</span>
          <strong>{{ trainingStatus?.vectorDimensions ?? '-' }}</strong>
        </article>
      </div>
      <el-button type="primary" :loading="trainingLoading" @click="trainKnowledge">
        {{ trainingLoading ? copy.training : copy.trainNow }}
      </el-button>
    </section>

    <section class="admin-toolbar">
      <div class="toolbar-left">
        <el-input v-model="query" clearable :placeholder="copy.search" style="width: 300px" />
        <el-select v-model="typeFilter" style="width: 170px">
          <el-option :label="copy.allTypes" value="all" />
          <el-option label="openai" value="openai" />
          <el-option label="ollama" value="ollama" />
          <el-option label="gemini" value="gemini" />
        </el-select>
      </div>
      <div class="toolbar-right">
        <el-tag>{{ copy.showing }} {{ filteredConfigs.length }}</el-tag>
      </div>
    </section>

    <el-table v-if="filteredConfigs.length" :data="filteredConfigs" stripe max-height="620">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="name" :label="copy.name" min-width="150" />
      <el-table-column :label="copy.type" width="100">
        <template #default="{ row }">
          <el-tag size="small">{{ row.modelType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="modelName" :label="copy.model" min-width="170" />
      <el-table-column prop="apiUrl" :label="copy.apiUrl" min-width="260" show-overflow-tooltip />
      <el-table-column prop="temperature" :label="copy.temperature" width="100" />
      <el-table-column prop="contextWindow" :label="copy.context" width="100" />
      <el-table-column :label="copy.active" width="100">
        <template #default="{ row }">
          <el-tag :type="row.isActive ? 'success' : 'info'" size="small">
            {{ row.isActive ? copy.active : copy.idle }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="copy.action" width="230" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="edit(row)">{{ copy.edit }}</el-button>
          <el-button v-if="!row.isActive" size="small" type="success" @click="activate(row.id)">{{ copy.enable }}</el-button>
          <el-button size="small" type="danger" @click="del(row.id)">{{ copy.delete }}</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div v-else class="empty-hint">{{ copy.empty }}</div>

    <el-dialog v-model="showEdit" :title="editing ? copy.editConfig : copy.addConfig" width="620px">
      <el-form label-position="top" size="small">
        <el-form-item :label="copy.name">
          <el-input v-model="form.name" placeholder="DeepSeek / OpenAI / Qwen" />
        </el-form-item>
        <el-form-item :label="copy.type">
          <el-select v-model="form.modelType" style="width: 100%">
            <el-option :label="copy.openaiCompatible" value="openai" />
            <el-option :label="copy.ollamaLocal" value="ollama" />
            <el-option :label="copy.geminiApi" value="gemini" />
          </el-select>
        </el-form-item>
        <el-form-item :label="copy.model">
          <el-input v-model="form.modelName" placeholder="deepseek-chat / gpt-4o / qwen2.5:7b" />
        </el-form-item>
        <el-form-item :label="copy.apiUrl">
          <el-input v-model="form.apiUrl" placeholder="https://api.deepseek.com/v1" />
        </el-form-item>
        <el-form-item :label="copy.apiKey">
          <el-input
            v-model="form.apiKey"
            type="password"
            show-password
            :placeholder="editing ? copy.keepExistingKey : 'sk-...'"
          />
          <small v-if="editing" class="secret-hint">{{ copy.secretHint }}</small>
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item :label="copy.temperature">
              <el-input v-model.number="form.temperature" type="number" step="0.1" :min="0" :max="2" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="copy.maxTokens">
              <el-input v-model.number="form.maxTokens" type="number" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="copy.context">
              <el-input v-model.number="form.contextWindow" type="number" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item :label="copy.systemPrompt">
          <el-input v-model="form.systemPrompt" type="textarea" :rows="5" :placeholder="copy.systemPromptPlaceholder" />
        </el-form-item>
        <el-form-item :label="copy.active">
          <el-switch v-model="form.isActive" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEdit = false">{{ copy.cancel }}</el-button>
        <el-button type="primary" @click="save">{{ copy.save }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api'

const { locale } = useI18n()
const configs = ref<any[]>([])
const query = ref('')
const typeFilter = ref('all')
const showEdit = ref(false)
const editing = ref<any>(null)
const trainingLoading = ref(false)
const configLoading = ref(false)
const trainingStatus = ref<any>(null)
const form = ref(defaultForm())
const isZh = computed(() => locale.value.startsWith('zh'))

const copy = computed(() => isZh.value ? {
  eyebrow: 'AI 管理',
  title: 'AI 配置与知识训练',
  subtitle: '维护外部 API、模型参数、系统提示词和站内知识训练。用户对话会优先使用启用的配置和训练后的知识索引。',
  refresh: '刷新',
  addConfig: '添加配置',
  editConfig: '编辑配置',
  totalConfigs: '配置',
  totalConfigsHint: '模型供应商记录',
  activeConfigs: '启用',
  activeConfigsHint: '当前可用配置',
  trainedDocs: '已索引文档',
  trainedDocsHint: '写入知识向量',
  vectorDimensions: '向量维度',
  vectorHint: '检索向量维度',
  modelCount: '模型数',
  knowledgeTraining: 'AI 知识训练',
  knowledgeTrainingTitle: '训练站内模型、功能和文章知识',
  knowledgeTrainingDesc: '重建本地知识索引并写入 knowledge_docs.embedding_vector。AI 回答会优先检索这些训练结果。',
  trainNow: '立即训练',
  training: '训练中',
  search: '搜索配置、模型或 API',
  allTypes: '全部类型',
  showing: '当前显示',
  empty: '没有匹配的 AI 配置',
  idle: '未启用',
  name: '名称',
  type: '类型',
  model: '模型',
  apiUrl: 'API 地址',
  temperature: '温度',
  context: '上下文',
  active: '启用',
  action: '操作',
  edit: '编辑',
  enable: '启用',
  delete: '删除',
  save: '保存',
  cancel: '取消',
  apiKey: 'API 密钥',
  keepExistingKey: '留空则保留现有密钥',
  secretHint: '已有密钥会被遮蔽，后端不会回传明文。',
  maxTokens: '最大输出 Token',
  systemPrompt: '系统提示词',
  systemPromptPlaceholder: '定义 AI 助手的行为、边界和回答方式...',
  openaiCompatible: 'OpenAI 兼容接口（DeepSeek 等）',
  ollamaLocal: 'Ollama 本地模型',
  geminiApi: 'Gemini API',
  saved: '配置已保存',
  activated: '配置已启用',
  deleted: '配置已删除',
  trainingDone: 'AI 知识训练完成',
  trainingFailed: 'AI 知识训练失败',
} : {
  eyebrow: 'AI Admin',
  title: 'AI Config and Knowledge Training',
  subtitle: 'Maintain external API providers, model parameters, system prompts, and trained site knowledge.',
  refresh: 'Refresh',
  addConfig: 'Add config',
  editConfig: 'Edit config',
  totalConfigs: 'Configs',
  totalConfigsHint: 'Provider records',
  activeConfigs: 'Active',
  activeConfigsHint: 'Currently usable',
  trainedDocs: 'Indexed docs',
  trainedDocsHint: 'Written vectors',
  vectorDimensions: 'Vector dim',
  vectorHint: 'Retrieval dimension',
  modelCount: 'Models',
  knowledgeTraining: 'AI Knowledge Training',
  knowledgeTrainingTitle: 'Train site model, feature, and article knowledge',
  knowledgeTrainingDesc: 'Rebuilds the local knowledge index and writes knowledge_docs.embedding_vector. AI answers retrieve these trained records first.',
  trainNow: 'Train now',
  training: 'Training',
  search: 'Search config, model, or API',
  allTypes: 'All types',
  showing: 'Showing',
  empty: 'No matching AI configs',
  idle: 'Idle',
  name: 'Name',
  type: 'Type',
  model: 'Model',
  apiUrl: 'API URL',
  temperature: 'Temperature',
  context: 'Context',
  active: 'Active',
  action: 'Actions',
  edit: 'Edit',
  enable: 'Activate',
  delete: 'Delete',
  save: 'Save',
  cancel: 'Cancel',
  apiKey: 'API Key',
  keepExistingKey: 'Leave blank to keep existing key',
  secretHint: 'Existing keys are masked and never returned by the server.',
  maxTokens: 'Max Tokens',
  systemPrompt: 'System Prompt',
  systemPromptPlaceholder: 'Define the assistant behavior, boundaries, and answer style...',
  openaiCompatible: 'OpenAI compatible (DeepSeek, etc.)',
  ollamaLocal: 'Ollama local model',
  geminiApi: 'Gemini API',
  saved: 'Config saved',
  activated: 'Config activated',
  deleted: 'Config deleted',
  trainingDone: 'AI knowledge training completed',
  trainingFailed: 'AI knowledge training failed',
})

const activeConfigCount = computed(() => configs.value.filter((item) => item.isActive).length)
const filteredConfigs = computed(() => {
  const keyword = query.value.trim().toLowerCase()
  return configs.value.filter((item) => {
    const matchType = typeFilter.value === 'all' || item.modelType === typeFilter.value
    const matchKeyword = !keyword || `${item.name || ''} ${item.modelName || ''} ${item.apiUrl || ''}`.toLowerCase().includes(keyword)
    return matchType && matchKeyword
  })
})

function defaultForm() {
  return {
    name: '',
    modelType: 'openai',
    modelName: '',
    apiUrl: '',
    apiKey: '',
    systemPrompt: '',
    temperature: 0.7,
    maxTokens: 4096,
    contextWindow: 20,
    isActive: false,
  }
}

async function load() {
  configLoading.value = true
  try {
    const response = await adminApi.aiConfigs.list()
    if (response.data.code === 200) configs.value = response.data.data || []
  } finally {
    configLoading.value = false
  }
}

async function loadTrainingStatus() {
  try {
    const response = await adminApi.aiKnowledgeTraining.status()
    if (response.data.code === 200) trainingStatus.value = response.data.data
  } catch {
    trainingStatus.value = null
  }
}

async function trainKnowledge() {
  trainingLoading.value = true
  try {
    const response = await adminApi.aiKnowledgeTraining.train()
    if (response.data.code === 200) {
      trainingStatus.value = response.data.data
      ElMessage.success(copy.value.trainingDone)
    }
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || error?.message || copy.value.trainingFailed)
  } finally {
    trainingLoading.value = false
  }
}

function edit(row: any) {
  editing.value = row
  form.value = { ...row, apiKey: '' }
  showEdit.value = true
}

function openCreate() {
  editing.value = null
  form.value = defaultForm()
  showEdit.value = true
}

async function save() {
  const id = editing.value?.id ?? null
  const response = await adminApi.aiConfigs.save(id, form.value)
  if (response.data.code === 200) {
    ElMessage.success(copy.value.saved)
    showEdit.value = false
    load()
  }
}

async function activate(id: number) {
  await adminApi.aiConfigs.activate(id)
  ElMessage.success(copy.value.activated)
  load()
}

async function del(id: number) {
  try {
    await ElMessageBox.confirm(`${copy.value.delete}?`)
    await adminApi.aiConfigs.delete(id)
    ElMessage.success(copy.value.deleted)
    load()
  } catch {
    // cancelled
  }
}

onMounted(() => {
  load()
  loadTrainingStatus()
})
</script>

<style scoped>
.secret-hint {
  display: block;
  margin-top: 6px;
  color: var(--text-secondary);
  font-size: 12px;
}

.training-panel {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(280px, 0.6fr) auto;
  gap: 14px;
  align-items: center;
  padding: 16px;
  border: 1px solid var(--border-color);
  border-radius: 18px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.055), transparent 62%),
    rgba(var(--glass-bg-rgb), 0.52);
  box-shadow: var(--shadow-soft);
}

.training-panel span,
.training-metrics span {
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 700;
}

.training-panel strong {
  display: block;
  margin-top: 4px;
  color: var(--text-primary);
  font-size: 18px;
}

.training-panel p {
  margin: 6px 0 0;
  color: var(--text-secondary);
  line-height: 1.6;
}

.training-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.training-metrics article {
  padding: 10px;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  background: color-mix(in srgb, var(--surface-2) 78%, transparent);
}

.training-metrics strong {
  font-size: 20px;
}

@media (max-width: 980px) {
  .training-panel {
    grid-template-columns: 1fr;
  }
}
</style>
