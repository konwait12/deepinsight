<template>
  <div class="admin-section">
    <section class="section-hero">
      <div>
        <span>AI PROVIDERS</span>
        <h2 class="page-title">{{ t('admin.ai') }}</h2>
        <p>统一管理 DeepSeek、OpenAI 兼容接口、Gemini 和 Ollama。密钥只保存在后端，前端只显示脱敏状态。</p>
      </div>
      <el-button type="primary" @click="openCreate">+ {{ t('admin.addConfig') }}</el-button>
    </section>

    <section class="status-grid">
      <div class="status-card">
        <span>激活配置</span>
        <strong>{{ activeConfig?.name || '未启用' }}</strong>
        <em>{{ activeConfig?.modelName || 'AI 对话与自动分析会降级' }}</em>
      </div>
      <div class="status-card">
        <span>配置数量</span>
        <strong>{{ configs.length }}</strong>
        <em>{{ providerSummary }}</em>
      </div>
      <div class="status-card">
        <span>密钥策略</span>
        <strong>后端保存</strong>
        <em>列表仅返回脱敏密钥，编辑留空表示保留原密钥</em>
      </div>
    </section>

    <section v-if="!activeConfig" class="warning-card">
      <strong>当前没有启用 AI 配置</strong>
      <span>可视化分析导入对话、AI 面板刷新、悬浮助手都会受影响。请新增配置并启用。</span>
    </section>

    <el-table :data="configs" stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="name" :label="t('admin.name')" min-width="170" />
      <el-table-column :label="t('admin.type')" width="120">
        <template #default="{ row }"><el-tag size="small">{{ providerLabel(row.modelType) }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="modelName" :label="t('admin.model')" min-width="180" />
      <el-table-column prop="apiUrl" :label="t('admin.apiUrl')" min-width="260" />
      <el-table-column prop="temperature" :label="t('admin.temperature')" width="110" />
      <el-table-column prop="contextWindow" :label="t('admin.context')" width="110" />
      <el-table-column label="Key" width="130">
        <template #default="{ row }">
          <el-tag :type="row.apiKey ? 'success' : 'warning'" size="small">{{ row.apiKey ? '已配置' : '缺失' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('admin.active')" width="120">
        <template #default="{ row }">
          <el-tag :type="row.isActive ? 'success' : 'info'" size="small">{{ row.isActive ? '启用中' : '待机' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('admin.action')" width="260" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="edit(row)">{{ t('admin.edit') }}</el-button>
          <el-button size="small" type="success" :disabled="row.isActive" @click="activate(row.id)">{{ t('admin.enable') }}</el-button>
          <el-button size="small" type="danger" @click="del(row.id)">{{ t('admin.delete') }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="showEdit" :title="editing ? t('admin.editConfig') : t('admin.addConfig')" width="620px">
      <el-form label-position="top" size="small">
        <el-form-item :label="t('admin.name')"><el-input v-model="form.name" placeholder="DeepSeek 主配置" /></el-form-item>
        <el-form-item :label="t('admin.type')">
          <el-select v-model="form.modelType" style="width:100%">
            <el-option label="OpenAI 兼容接口（DeepSeek / GPT / Qwen 等）" value="openai" />
            <el-option label="Ollama 本地模型" value="ollama" />
            <el-option label="Gemini API" value="gemini" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('admin.model')"><el-input v-model="form.modelName" placeholder="deepseek-chat / gpt-4o / qwen2.5:7b" /></el-form-item>
        <el-form-item :label="t('admin.apiUrl')"><el-input v-model="form.apiUrl" placeholder="https://api.deepseek.com/v1" /></el-form-item>
        <el-form-item label="API Key">
          <el-input v-model="form.apiKey" type="password" show-password :placeholder="editing ? '留空则保留后端现有密钥' : 'sk-...'" />
          <small class="secret-hint">保存后后端会脱敏返回。不要把真实 Key 写进项目文件或截图。</small>
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="8"><el-form-item :label="t('admin.temperature')"><el-input v-model.number="form.temperature" type="number" step="0.1" :min="0" :max="2" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="Max Tokens"><el-input v-model.number="form.maxTokens" type="number" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item :label="t('admin.context')"><el-input v-model.number="form.contextWindow" type="number" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="系统提示词">
          <el-input v-model="form.systemPrompt" type="textarea" :rows="4" placeholder="定义 AI 助手的行为边界和项目角色..." />
        </el-form-item>
        <el-form-item :label="t('admin.active')"><el-switch v-model="form.isActive" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEdit=false">{{ t('admin.cancel') }}</el-button>
        <el-button type="primary" @click="save">{{ t('admin.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api'

const { t } = useI18n()
const configs = ref<any[]>([])
const showEdit = ref(false)
const editing = ref<any>(null)
const emptyForm = () => ({ name: '', modelType: 'openai', modelName: '', apiUrl: '', apiKey: '', systemPrompt: '', temperature: 0.7, maxTokens: 4096, contextWindow: 20, isActive: false })
const form = ref(emptyForm())

const activeConfig = computed(() => configs.value.find((item) => item.isActive))
const providerSummary = computed(() => {
  const counts = configs.value.reduce((acc: Record<string, number>, item) => {
    acc[item.modelType || 'unknown'] = (acc[item.modelType || 'unknown'] || 0) + 1
    return acc
  }, {})
  return Object.entries(counts).map(([key, value]) => `${providerLabel(key)} ${value}`).join(' / ') || '暂无配置'
})

const providerLabel = (type: string) => {
  if (type === 'ollama') return 'Ollama'
  if (type === 'gemini') return 'Gemini'
  return 'OpenAI 兼容'
}

const load = async () => {
  const response = await adminApi.aiConfigs.list()
  if (response.data.code === 200) configs.value = response.data.data || []
}

const openCreate = () => {
  editing.value = null
  form.value = emptyForm()
  showEdit.value = true
}

const edit = (row: any) => {
  editing.value = row
  form.value = { ...emptyForm(), ...row, apiKey: '' }
  showEdit.value = true
}

const save = async () => {
  if (!form.value.name || !form.value.modelName) {
    ElMessage.warning('请填写配置名称和模型名称')
    return
  }
  const id = editing.value?.id ?? null
  const response = await adminApi.aiConfigs.save(id, form.value)
  if (response.data.code === 200) {
    ElMessage.success('AI 配置已保存')
    showEdit.value = false
    await load()
  }
}

const activate = async (id: number) => {
  await adminApi.aiConfigs.activate(id)
  ElMessage.success('AI 配置已启用')
  await load()
}

const del = async (id: number) => {
  await ElMessageBox.confirm('确认删除该 AI 配置？密钥也会从后端配置中移除。', '危险操作', { type: 'warning' })
  await adminApi.aiConfigs.delete(id)
  ElMessage.success('AI 配置已删除')
  await load()
}

onMounted(load)
</script>

<style scoped>
.admin-section {
  display: grid;
  gap: 16px;
}

.section-hero,
.status-card,
.warning-card {
  border: 1px solid color-mix(in srgb, var(--primary-color) 14%, var(--border-color));
  border-radius: 18px;
  background: rgba(var(--glass-bg-rgb), 0.42);
  box-shadow: var(--shadow-soft);
  backdrop-filter: blur(16px);
}

.section-hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  padding: 22px;
}

.section-hero span,
.status-card span {
  color: var(--text-muted);
  font-size: 11px;
  font-weight: var(--font-weight-title);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.section-hero p {
  max-width: 820px;
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.7;
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.status-card {
  display: grid;
  gap: 8px;
  padding: 16px;
}

.status-card strong {
  color: var(--text-primary);
  font-size: 22px;
  font-weight: var(--font-weight-title);
}

.status-card em,
.secret-hint,
.warning-card span {
  color: var(--text-secondary);
  font-size: 12px;
  font-style: normal;
  line-height: 1.55;
}

.warning-card {
  display: grid;
  gap: 6px;
  padding: 15px 16px;
  border-color: color-mix(in srgb, var(--tone-amber) 36%, var(--border-color));
}

.warning-card strong {
  color: var(--text-primary);
}

.secret-hint {
  display: block;
  margin-top: 6px;
}

@media (max-width: 820px) {
  .section-hero {
    align-items: flex-start;
    flex-direction: column;
  }

  .status-grid {
    grid-template-columns: 1fr;
  }
}
</style>
