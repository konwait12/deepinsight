<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
      <h2 class="page-title">{{ t('admin.ai') }}</h2>
      <el-button type="primary" @click="showEdit=true;editing=null" :style="{backgroundColor:'var(--primary-color)'}">+ {{ t('admin.addConfig') }}</el-button>
    </div>

    <el-table :data="configs" stripe>
      <el-table-column prop="id" label="ID" width="50"/>
      <el-table-column prop="name" :label="t('admin.name')" width="150"/>
      <el-table-column :label="t('admin.type')" width="90">
        <template #default="{row}"><el-tag size="small">{{ row.modelType }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="modelName" :label="t('admin.model')" width="160"/>
      <el-table-column prop="apiUrl" :label="t('admin.apiUrl')" min-width="260"/>
      <el-table-column prop="temperature" :label="t('admin.temperature')" width="90"/>
      <el-table-column prop="contextWindow" :label="t('admin.context')" width="90"/>
      <el-table-column :label="t('admin.active')" width="90">
        <template #default="{row}"><el-tag :type="row.isActive?'success':'info'" size="small">{{ row.isActive ? '● ' + t('admin.active') : '○ Idle' }}</el-tag></template>
      </el-table-column>
      <el-table-column :label="t('admin.action')" width="210">
        <template #default="{row}">
          <el-button size="small" @click="edit(row)">{{ t('admin.edit') }}</el-button>
          <el-button size="small" type="success" @click="activate(row.id)" v-if="!row.isActive">{{ t('admin.enable') }}</el-button>
          <el-button size="small" type="danger" @click="del(row.id)">{{ t('admin.delete') }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="showEdit" :title="editing ? t('admin.editConfig') : t('admin.addConfig')" width="560px">
      <el-form label-position="top" size="small">
        <el-form-item :label="t('admin.name')"><el-input v-model="form.name" placeholder="DeepSeek-V4"/></el-form-item>
        <el-form-item :label="t('admin.type')">
          <el-select v-model="form.modelType" style="width:100%">
            <el-option label="OpenAI 兼容 (DeepSeek/Gemini等)" value="openai"/>
            <el-option label="Ollama 本地模型" value="ollama"/>
            <el-option label="Gemini API" value="gemini"/>
          </el-select>
        </el-form-item>
        <el-form-item :label="t('admin.model')"><el-input v-model="form.modelName" placeholder="deepseek-chat / gpt-4o / qwen2.5:7b"/></el-form-item>
        <el-form-item :label="t('admin.apiUrl')"><el-input v-model="form.apiUrl" placeholder="https://api.deepseek.com/v1"/></el-form-item>
        <el-form-item label="API Key">
          <el-input
            v-model="form.apiKey"
            type="password"
            show-password
            :placeholder="editing ? 'Leave blank to keep existing key' : 'sk-...'"
          />
          <small v-if="editing" class="secret-hint">Existing keys are masked and never returned by the server.</small>
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="8"><el-form-item :label="t('admin.temperature')"><el-input v-model.number="form.temperature" type="number" step="0.1" :min="0" :max="2"/></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="Max Tokens"><el-input v-model.number="form.maxTokens" type="number"/></el-form-item></el-col>
          <el-col :span="8"><el-form-item :label="t('admin.context')"><el-input v-model.number="form.contextWindow" type="number"/></el-form-item></el-col>
        </el-row>
        <el-form-item label="系统提示词 (System Prompt)">
          <el-input v-model="form.systemPrompt" type="textarea" :rows="4" placeholder="定义AI助手的行为和角色..."/>
        </el-form-item>
        <el-form-item :label="t('admin.active')"><el-switch v-model="form.isActive"/></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEdit=false">{{ t('admin.cancel') }}</el-button>
        <el-button type="primary" @click="save" :style="{backgroundColor:'var(--primary-color)'}">{{ t('admin.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api'

const { t } = useI18n()
const configs = ref<any[]>([])
const showEdit = ref(false)
const editing = ref<any>(null)
const form = ref({ name:'',modelType:'openai',modelName:'',apiUrl:'',apiKey:'',systemPrompt:'',temperature:0.7,maxTokens:4096,contextWindow:20,isActive:false })

const load = async () => {
  const d = await adminApi.aiConfigs.list()
  if (d.data.code===200) configs.value = d.data.data || []
}

const edit = (row:any) => {
  editing.value = row; form.value = {...row, apiKey: ''}; showEdit.value = true
}

const save = async () => {
  const id = editing.value?.id ?? null
  const d = await adminApi.aiConfigs.save(id, form.value)
  if (d.data.code===200) { ElMessage.success(t('admin.save')); showEdit.value = false; load() }
}

const activate = async (id:number) => {
  await adminApi.aiConfigs.activate(id)
  ElMessage.success(t('admin.active')); load()
}

const del = async (id:number) => {
  try { await ElMessageBox.confirm(`${t('admin.delete')}?`); await adminApi.aiConfigs.delete(id); ElMessage.success(t('admin.delete')); load() } catch { /* cancelled */ }
}

onMounted(load)
</script>
<style scoped>.page-title{font-size:20px;font-weight:800;color:var(--text-primary);margin:0}.secret-hint{display:block;margin-top:6px;color:var(--text-secondary);font-size:12px}</style>
