<template>
  <div class="admin-section">
    <section class="section-hero">
      <div>
        <span>MODEL REGISTRY</span>
        <h2 class="page-title">{{ t('admin.models') }}</h2>
        <p>管理员界面是官方模型资产唯一管理入口。普通用户和普通 admin 视图里的官方资产都应只读，避免误删或误改。</p>
      </div>
      <el-button type="primary" @click="load">刷新</el-button>
    </section>

    <section class="policy-card">
      <strong>官方资产保护</strong>
      <span>官方模型用于训练入口、可视化推荐和知识库关联。这里默认隐藏官方模型删除按钮，只允许管理自定义资产。</span>
    </section>

    <section class="summary-row">
      <div class="summary-card"><span>全部模型</span><strong>{{ models.length }}</strong></div>
      <div class="summary-card"><span>官方模型</span><strong>{{ officialCount }}</strong></div>
      <div class="summary-card"><span>自定义模型</span><strong>{{ customCount }}</strong></div>
      <div class="summary-card"><span>框架数</span><strong>{{ frameworkCount }}</strong></div>
    </section>

    <section class="toolbar-card">
      <el-input v-model="keyword" clearable placeholder="搜索模型名 / 任务类型 / 框架" />
      <el-select v-model="typeFilter" clearable placeholder="资产类型">
        <el-option label="全部" value="" />
        <el-option label="官方" value="official" />
        <el-option label="自定义" value="custom" />
      </el-select>
    </section>

    <el-table :data="filteredModels" stripe max-height="640">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column :label="t('admin.name')" min-width="260">
        <template #default="{ row }">
          <span class="model-name">{{ displayModelName(row) }}</span>
          <small v-if="row.displayNameZh" class="raw-name">{{ row.name }}</small>
        </template>
      </el-table-column>
      <el-table-column :label="t('admin.taskType')" width="160">
        <template #default="{ row }">{{ displayTaskType(row) }}</template>
      </el-table-column>
      <el-table-column prop="paramCountM" :label="t('admin.params')" width="120" />
      <el-table-column prop="framework" :label="t('admin.framework')" width="130" />
      <el-table-column :label="t('admin.type')" width="130">
        <template #default="{ row }">
          <el-tag :type="row.isOfficial ? 'primary' : 'success'" size="small">
            {{ row.isOfficial ? t('admin.official') : t('admin.custom') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdBy" :label="t('admin.createdBy')" width="130" />
      <el-table-column :label="t('admin.action')" width="160" fixed="right">
        <template #default="{ row }">
          <el-tooltip v-if="row.isOfficial" content="官方模型受保护，只能在后端种子数据或专用迁移中维护">
            <el-button size="small" disabled>受保护</el-button>
          </el-tooltip>
          <el-button v-else size="small" type="danger" @click="del(row)">{{ t('admin.delete') }}</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api'
import { modelDisplayName, taskTypeLabel } from '@/utils/modelDisplay'

const { t, locale } = useI18n()
const models = ref<any[]>([])
const keyword = ref('')
const typeFilter = ref('')

const load = async () => {
  const response = await adminApi.models.list()
  if (response.data.code === 200) models.value = response.data.data || []
}

const filteredModels = computed(() => {
  const q = keyword.value.trim().toLowerCase()
  return models.value.filter((model) => {
    const matchesText = !q
      || String(model.name || '').toLowerCase().includes(q)
      || String(model.displayNameZh || '').toLowerCase().includes(q)
      || String(model.taskType || '').toLowerCase().includes(q)
      || String(model.framework || '').toLowerCase().includes(q)
    const matchesType = !typeFilter.value || (typeFilter.value === 'official' ? model.isOfficial : !model.isOfficial)
    return matchesText && matchesType
  })
})

const officialCount = computed(() => models.value.filter((item) => item.isOfficial).length)
const customCount = computed(() => models.value.filter((item) => !item.isOfficial).length)
const frameworkCount = computed(() => new Set(models.value.map((item) => item.framework).filter(Boolean)).size)
const displayModelName = (row: any) => modelDisplayName(row, locale)
const displayTaskType = (row: any) => taskTypeLabel(row.taskType, row.taskTypeZh, locale)

const del = async (model: any) => {
  if (model.isOfficial) {
    ElMessage.warning('官方模型受保护，不能在这里删除')
    return
  }
  await ElMessageBox.confirm(`确认删除自定义模型 ${model.name}？`, '危险操作', { type: 'warning' })
  await adminApi.models.delete(model.id)
  ElMessage.success('模型已删除')
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
.toolbar-card,
.summary-card,
.policy-card {
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
.summary-card span {
  color: var(--text-muted);
  font-size: 11px;
  font-weight: var(--font-weight-title);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.section-hero p,
.policy-card span {
  max-width: 820px;
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.7;
}

.policy-card {
  display: grid;
  gap: 6px;
  padding: 16px;
}

.policy-card strong {
  color: var(--text-primary);
  font-weight: var(--font-weight-title);
}

.summary-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.summary-card {
  display: grid;
  gap: 8px;
  padding: 16px;
}

.summary-card strong {
  color: var(--text-primary);
  font-size: 28px;
  font-weight: var(--font-weight-title);
}

.toolbar-card {
  display: grid;
  grid-template-columns: minmax(260px, 1fr) 180px;
  gap: 12px;
  padding: 14px;
}

.model-name {
  display: block;
  color: var(--text-primary);
  font-weight: var(--font-weight-title);
}

.raw-name {
  display: block;
  margin-top: 3px;
  color: var(--text-muted);
  font-size: 11px;
}

@media (max-width: 820px) {
  .section-hero {
    align-items: flex-start;
    flex-direction: column;
  }

  .summary-row,
  .toolbar-card {
    grid-template-columns: 1fr;
  }
}
</style>
