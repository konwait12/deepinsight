<template>
  <div class="training-page">
    <div class="page-header entrance-hero"><h2>{{ $t('nav.training') }}</h2><p>{{ $t('training.subtitle') }}</p></div>

    <el-card v-if="!isLoggedIn" shadow="never" class="login-hint">
      <div class="hint-content">
        <el-icon :size="40"><WarningFilled /></el-icon>
        <h3>{{ pageText.loginRequired }}</h3>
        <p>{{ pageText.loginRequiredDesc }}</p>
        <el-button type="primary" @click="$router.push('/login')" :style="{backgroundColor:'var(--primary-color)'}">{{ pageText.goLogin }}</el-button>
      </div>
    </el-card>

    <template v-else>
      <section class="training-guide entrance-up">
        <article v-for="step in trainingGuide" :key="step.title">
          <span>{{ step.index }}</span>
          <strong>{{ step.title }}</strong>
          <p>{{ step.desc }}</p>
        </article>
      </section>

      <div class="model-step-section entrance-up" style="animation-delay: 0.1s">
        <div class="step-header">
          <div>
            <span class="step-badge">1</span>
            <span>{{ pageText.selectModel }}</span>
            <small>{{ pageText.selectModelHint }}</small>
          </div>
          <span v-if="selectedModel" class="selected-hint">{{ pageText.selected }}: <strong>{{ selectedModelLabel }}</strong></span>
        </div>
        <div class="model-browser">
          <aside class="model-group-menu" :aria-label="pageText.modelScope">
            <button
              v-for="group in modelGroupTabs"
              :key="group.key"
              type="button"
              class="model-group-tab"
              :class="{ active: activeModelGroup === group.key }"
              @click="selectModelGroup(group.key)"
            >
              <span class="group-copy">
                <strong>{{ group.label }}</strong>
                <small>{{ group.desc }}</small>
              </span>
              <em>{{ group.count }}</em>
            </button>
          </aside>

          <div class="model-results-panel">
            <div class="model-filter-bar">
              <div class="task-filter-list" :aria-label="pageText.modelTaskFilter">
                <button
                  v-for="task in taskFilterTabs"
                  :key="task.key"
                  type="button"
                  class="task-filter-btn"
                  :class="{ active: activeTaskType === task.key }"
                  @click="selectTaskFilter(task.key)"
                >
                  <span>{{ task.label }}</span>
                  <em>{{ task.count }}</em>
                </button>
              </div>
              <button type="button" class="model-register-btn" @click="showAddModel = true">
                <span>+</span>
                {{ pageText.registerModel }}
              </button>
            </div>

            <div class="model-result-head">
              <span>{{ activeModelGroupLabel }} / {{ activeTaskFilterLabel }}</span>
              <em>{{ pageText.modelsCount.replace('{count}', String(filteredModelOptions.length)) }}</em>
            </div>

            <div v-if="filteredModelOptions.length" class="model-grid stagger-fast">
              <div v-for="m in filteredModelOptions" :key="m.id||m.name" class="model-chip" :class="{ active: selectedModel===m.name }">
                <span class="chip-body" @click="selectModel(m)">
                  <span class="chip-copy">
                    <span class="chip-name">{{ displayModelName(m) }}</span>
                    <span class="chip-meta">{{ formatParams(m) }} · {{ displayTaskType(m) }}</span>
                    <span v-if="displayModelDesc(m)" class="chip-desc">{{ displayModelDesc(m) }}</span>
                  </span>
                  <span v-if="m.isOfficial" class="chip-flag">{{ pageText.official }}</span>
                  <span v-else class="chip-flag custom-flag">{{ pageText.custom }}</span>
                </span>
                <el-button v-if="m.articleId" link size="small" class="learn-btn" @click.stop="openModelArticle(m.articleId)"><el-icon><Document /></el-icon></el-button>
              </div>
            </div>

            <div v-else class="model-empty-state">
              <strong>{{ pageText.noModelsInFilter }}</strong>
              <span>{{ activeModelGroup === 'custom' ? pageText.customEmptyHint : pageText.switchModelFilter }}</span>
              <button v-if="activeModelGroup === 'custom'" type="button" @click="showAddModel = true">{{ pageText.registerModel }}</button>
            </div>
          </div>
        </div>
      </div>

      <el-row :gutter="20" class="mt-4">
        <el-col :span="8" class="entrance-up" style="animation-delay: 0.15s">
          <el-card shadow="never" class="config-card">
            <template #header>
              <div class="card-header stacked-card-header">
                <span><span class="step-badge">2</span> {{ pageText.config }}</span>
                <small>{{ pageText.configHint }}</small>
              </div>
            </template>
            <div class="selected-model-card" v-if="selectedModelOption">
              <span>{{ pageText.currentModel }}</span>
              <strong>{{ selectedModelLabel }}</strong>
              <em>{{ formatParams(selectedModelOption) }} · {{ displayTaskType(selectedModelOption) }} · {{ selectedModelOption.framework || 'PyTorch' }}</em>
            </div>
            <el-form label-position="top" size="small">
              <el-form-item :label="pageText.jobName"><el-input v-model="jobName"/></el-form-item>
              <el-form-item :label="metricLabel('learningRate')"><el-input v-model.number="lr" type="number" step="0.0001"/></el-form-item>
              <el-form-item :label="pageText.batchSize"><el-input v-model.number="bs" type="number"/></el-form-item>
              <el-form-item :label="pageText.epochs"><el-slider v-model="epochs" :min="1" :max="500" show-input style="width:100%"/></el-form-item>
              <el-form-item :label="pageText.optimizer"><el-select v-model="optimizer" style="width:100%"><el-option label="Adam" value="adam"/><el-option label="SGD" value="sgd"/><el-option label="AdamW" value="adamw"/><el-option label="RMSprop" value="rmsprop"/></el-select></el-form-item>
              <el-form-item :label="pageText.device"><el-radio-group v-model="device"><el-radio label="cpu">CPU</el-radio><el-radio label="cuda:0">GPU:0</el-radio></el-radio-group></el-form-item>
              <el-form-item><el-button type="primary" @click="startTraining" :loading="submitting" :disabled="!selectedModel" style="width:100%" :style="{ backgroundColor:'var(--primary-color)' }"><el-icon><VideoPlay /></el-icon> {{ pageText.start }}</el-button></el-form-item>
            </el-form>
          </el-card>
        </el-col>
        <el-col :span="16" class="entrance-up" style="animation-delay: 0.22s">
          <el-card shadow="never" class="chart-card mb-4">
            <template #header>
              <div class="card-header chart-card-header">
                <span><span class="step-badge">3</span> {{ pageText.liveCurve }}</span>
                <small>{{ pageText.curveHint }}</small>
              </div>
            </template>
            <div ref="trainChartRef" class="chart-box"></div>
            <div class="metric-explainers">
              <span><strong>{{ metricShortLabel('loss') }}</strong>{{ metricDescription('loss') }}</span>
              <span><strong>{{ metricShortLabel('accuracy') }}</strong>{{ metricDescription('accuracy') }}</span>
              <span><strong>{{ metricShortLabel('learningRate') }}</strong>{{ metricDescription('learningRate') }}</span>
            </div>
          </el-card>
          <el-card shadow="never" class="job-card" v-loading="loading">
            <template #header>{{ pageText.jobList }} ({{ jobs.length }})</template>
            <div v-for="j in jobs" :key="j.id" class="job-item">
              <div class="job-info"><span class="job-name">{{ j.name }}</span><span class="job-model">{{ displayArchitecture(j.modelArchitecture) }}</span></div>
              <div class="job-progress"><el-progress :percentage="jobProgress(j)" :status="j.status==='running'?'':j.status==='completed'?'success':'warning'" :stroke-width="8"/></div>
              <div class="job-meta"><el-tag :type="statusTag(j.status)" size="small">{{ statusLabel(j.status) }}</el-tag><span>{{ metricShortLabel('loss') }}: {{ j.currentLoss?.toFixed(4) || '--' }}</span><span>{{ metricShortLabel('accuracy') }}: {{ j.currentAccuracy ? (j.currentAccuracy*100).toFixed(1)+'%' : '--' }}</span></div>
              <div class="job-actions">
                <el-button v-if="j.status==='queued'||j.status==='paused'" size="small" type="primary" @click="startJob(j)"><el-icon><VideoPlay /></el-icon>{{ pageText.resume }}</el-button>
                <el-button v-if="j.status==='running'" size="small" @click="pauseJob(j)"><el-icon><VideoPause /></el-icon>{{ pageText.pause }}</el-button>
                <el-button v-if="j.status==='running'" size="small" type="warning" @click="stopJob(j)"><el-icon><Close /></el-icon>{{ pageText.stop }}</el-button>
                <el-button size="small" type="danger" @click="deleteJob(j)"><el-icon><Delete /></el-icon>{{ pageText.deleteAction }}</el-button>
              </div>
            </div>
            <el-empty v-if="!jobs.length" :description="pageText.noJobs"/>
          </el-card>
        </el-col>
      </el-row>
    </template>

    <el-dialog v-model="showAddModel" :title="pageText.registerCustomModel" width="500px">
      <el-form label-position="top" size="small">
        <el-form-item :label="pageText.modelName"><el-input v-model="newModel.name"/></el-form-item>
        <el-form-item :label="pageText.taskType"><el-select v-model="newModel.taskType" style="width:100%"><el-option v-for="task in taskOptions" :key="task.value" :label="task.label" :value="task.value"/></el-select></el-form-item>
        <el-form-item :label="pageText.params"><el-input v-model.number="newModel.paramCountM" type="number"/></el-form-item>
        <el-form-item :label="pageText.framework"><el-select v-model="newModel.framework" style="width:100%"><el-option label="PyTorch" value="pytorch"/><el-option label="TensorFlow" value="tensorflow"/><el-option label="JAX" value="jax"/></el-select></el-form-item>
        <el-form-item :label="pageText.description"><el-input v-model="newModel.description" type="textarea" :rows="2"/></el-form-item>
      </el-form>
      <template #footer><el-button @click="showAddModel=false">{{ pageText.cancel }}</el-button><el-button type="primary" @click="createModel" :style="{backgroundColor:'var(--primary-color)'}">{{ pageText.register }}</el-button></template>
    </el-dialog>

    <ArticleReaderDrawer
      v-model="articleDrawerOpen"
      :article="activeArticle"
      :loading="articleLoading"
      :source-label="pageText.modelIntro"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, computed, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { VideoPlay, VideoPause, Close, WarningFilled, Document, Delete } from '@element-plus/icons-vue';
import * as echarts from 'echarts';
import { trainingApi, analysisApi } from '@/api';
import type { TrainingJob, ModelOption } from '@/types/models';
import { useI18n } from 'vue-i18n';
import { metricText, modelDescription, modelDisplayName, taskTypeLabel } from '@/utils/modelDisplay';
import ArticleReaderDrawer from '@/components/common/ArticleReaderDrawer.vue';
import { hasStoredAuthToken } from '@/utils/authState';

const { locale } = useI18n();
const isLoggedIn = computed(() => hasStoredAuthToken());
const selectedModel = ref('');
const jobName = ref('training-1');
const lr = ref(0.001); const bs = ref(32); const epochs = ref(100); const optimizer = ref('adam'); const device = ref('cpu');
const trainChartRef = ref<HTMLDivElement>();
const loading = ref(false); const submitting = ref(false);
const articleDrawerOpen = ref(false);
const articleLoading = ref(false);
const activeArticle = ref<any>(null);
let chart: echarts.ECharts | null = null;
let pollTimer: ReturnType<typeof setInterval> | null = null;

const modelOptions = ref<ModelOption[]>([]);
const showAddModel = ref(false);
const newModel = ref({ name: '', taskType: 'classification', description: '', paramCountM: 0, framework: 'pytorch' });
type ModelGroupKey = 'all' | 'vision' | 'language' | 'audio' | 'recommendation' | 'multimodal' | 'custom';
type TaskFilterKey = 'all' | string;
const activeModelGroup = ref<ModelGroupKey>('all');
const activeTaskType = ref<TaskFilterKey>('all');
const featuredOfficialModelNames = new Set([
  'ResNet-50',
  'EfficientNet-B4',
  'ViT-B/16',
  'YOLOv8n',
  'DeepLabV3-RN50',
  'DeepFM',
  'NCF',
  'BERT-Base',
  'T5-Small',
]);

const isZh = computed(() => locale.value.startsWith('zh'));
const pageText = computed(() => isZh.value ? {
  loginRequired: '请先登录',
  loginRequiredDesc: '训练功能需要登录后使用',
  goLogin: '前往登录',
  selectModel: '选择模型',
  selectModelHint: '先选一个官方或自定义模型，下面的训练任务会基于它创建',
  selected: '已选',
  official: '官方',
  custom: '自定义',
  registerModel: '注册新模型',
  modelScope: '模型领域',
  modelTaskFilter: '任务类型筛选',
  modelsCount: '{count} 个模型',
  allTasks: '全部任务',
  noModelsInFilter: '当前分类下暂无模型',
  customEmptyHint: '可以先注册一个自定义模型。',
  switchModelFilter: '切换左侧领域或上方任务类型。',
  config: '训练配置',
  configHint: '只填本次实验最关键的参数，后续曲线和矩阵分析会读取这些真实配置',
  currentModel: '当前模型',
  jobName: '任务名称',
  batchSize: '批次大小',
  epochs: '训练轮次',
  optimizer: '优化器',
  device: '计算设备',
  start: '开始训练',
  liveCurve: '训练曲线',
  curveHint: '真实日志优先；无日志时仅根据当前任务状态预览',
  jobList: '训练任务列表',
  noJobs: '暂无训练任务',
  registerCustomModel: '注册自定义模型',
  modelName: '模型名称',
  taskType: '任务类型',
  params: '参数量(M)',
  framework: '框架',
  description: '描述',
  cancel: '取消',
  register: '注册',
  modelCreated: '模型已创建',
  pickModel: '请先选择一个模型',
  jobCreated: '训练任务已创建',
  createFailed: '创建失败',
  networkFailed: '网络请求失败',
  backendHint: '请确认后端已启动',
  deleteAction: '删除',
  resume: '启动',
  pause: '暂停',
  stop: '停止',
  deleteConfirm: '确认删除',
  deleted: '已删除',
  modelIntro: '模型介绍',
} : {
  loginRequired: 'Login Required',
  loginRequiredDesc: 'Training requires a signed-in account.',
  goLogin: 'Go to Login',
  selectModel: 'Select Model',
  selectModelHint: 'Pick an official or custom model before creating a training job',
  selected: 'Selected',
  official: 'Official',
  custom: 'Custom',
  registerModel: 'Register model',
  modelScope: 'Model scope',
  modelTaskFilter: 'Task filter',
  modelsCount: '{count} models',
  allTasks: 'All tasks',
  noModelsInFilter: 'No models in this filter',
  customEmptyHint: 'Register a custom model first.',
  switchModelFilter: 'Switch the scope or task filter.',
  config: 'Training Config',
  configHint: 'Set the key experiment parameters that will feed charts and matrix analysis',
  currentModel: 'Current model',
  jobName: 'Job Name',
  batchSize: 'Batch Size',
  epochs: 'Epochs',
  optimizer: 'Optimizer',
  device: 'Device',
  start: 'Start Training',
  liveCurve: 'Training Curves',
  curveHint: 'Uses real logs first; falls back to current job status preview when logs are absent',
  jobList: 'Training Jobs',
  noJobs: 'No training jobs',
  registerCustomModel: 'Register Custom Model',
  modelName: 'Model Name',
  taskType: 'Task Type',
  params: 'Params (M)',
  framework: 'Framework',
  description: 'Description',
  cancel: 'Cancel',
  register: 'Register',
  modelCreated: 'Model created',
  pickModel: 'Please select a model first',
  jobCreated: 'Training job created',
  createFailed: 'Create failed',
  networkFailed: 'Network request failed',
  backendHint: 'Please confirm the backend is running',
  deleteAction: 'Delete',
  resume: 'Start',
  pause: 'Pause',
  stop: 'Stop',
  deleteConfirm: 'Confirm Delete',
  deleted: 'Deleted',
  modelIntro: 'Model Guide',
});

const trainingGuide = computed(() => isZh.value ? [
  { index: '01', title: '选模型', desc: '决定这次实验要训练哪一种架构。' },
  { index: '02', title: '配参数', desc: '设置学习率、批次、轮次和设备。' },
  { index: '03', title: '看曲线', desc: '用 Loss / Accuracy 判断训练是否正常。' },
  { index: '04', title: '进分析', desc: '训练记录会进入可视化矩阵和 AI 面板。' },
] : [
  { index: '01', title: 'Pick model', desc: 'Choose the architecture for this experiment.' },
  { index: '02', title: 'Tune config', desc: 'Set learning rate, batch, epochs, and device.' },
  { index: '03', title: 'Watch curves', desc: 'Use Loss / Accuracy to judge training health.' },
  { index: '04', title: 'Analyze', desc: 'Training logs feed the matrix and AI panels.' },
]);

const taskOptions = computed(() => ['classification', 'detection', 'segmentation', 'recommendation', 'nlp', 'audio', 'multimodal', 'other'].map((value) => ({
  value,
  label: taskTypeLabel(value, undefined, locale),
})));

const modelTaskGroups: Record<Exclude<ModelGroupKey, 'all' | 'custom'>, Set<string>> = {
  vision: new Set(['classification', 'detection', 'segmentation']),
  language: new Set(['nlp']),
  audio: new Set(['audio', 'speech']),
  recommendation: new Set(['recommendation']),
  multimodal: new Set(['multimodal']),
};

const normalizeTaskType = (model: ModelOption) => (model.taskType || 'other').toLowerCase();
const modelMatchesGroup = (model: ModelOption, group: ModelGroupKey) => {
  if (group === 'all') return true;
  if (group === 'custom') return !model.isOfficial;
  return modelTaskGroups[group].has(normalizeTaskType(model));
};

const modelGroupBase = computed<Array<{ key: ModelGroupKey; label: string; desc: string }>>(() => isZh.value ? [
  { key: 'all', label: '全部模型', desc: '官方与自定义' },
  { key: 'vision', label: '视觉', desc: '分类 / 检测 / 分割' },
  { key: 'language', label: '语言', desc: '文本理解与生成' },
  { key: 'audio', label: '音频', desc: '语音与频谱分析' },
  { key: 'recommendation', label: '推荐', desc: '排序与兴趣建模' },
  { key: 'multimodal', label: '多模态', desc: '图文联合表示' },
  { key: 'custom', label: '自定义', desc: '自己注册的模型' },
] : [
  { key: 'all', label: 'All', desc: 'Official and custom' },
  { key: 'vision', label: 'Vision', desc: 'Classify / detect / segment' },
  { key: 'language', label: 'Language', desc: 'Understanding and generation' },
  { key: 'audio', label: 'Audio', desc: 'Speech and spectrograms' },
  { key: 'recommendation', label: 'Recsys', desc: 'Ranking and interests' },
  { key: 'multimodal', label: 'Multimodal', desc: 'Image-text representation' },
  { key: 'custom', label: 'Custom', desc: 'User registered models' },
]);

const modelGroupTabs = computed(() => modelGroupBase.value
  .map((group) => ({
    ...group,
    count: modelOptions.value.filter((model) => modelMatchesGroup(model, group.key)).length,
  }))
  .filter((group) => group.key === 'all' || group.key === 'custom' || group.count > 0));

const taskFilterTabs = computed(() => {
  const groupModels = modelOptions.value.filter((model) => modelMatchesGroup(model, activeModelGroup.value));
  const counts = new Map<string, number>();
  groupModels.forEach((model) => {
    const task = normalizeTaskType(model);
    counts.set(task, (counts.get(task) || 0) + 1);
  });
  const order = ['classification', 'detection', 'segmentation', 'nlp', 'audio', 'recommendation', 'multimodal', 'other'];
  const keys = [...counts.keys()].sort((a, b) => {
    const ai = order.indexOf(a);
    const bi = order.indexOf(b);
    if (ai === -1 && bi === -1) return a.localeCompare(b);
    if (ai === -1) return 1;
    if (bi === -1) return -1;
    return ai - bi;
  });
  return [
    { key: 'all', label: pageText.value.allTasks, count: groupModels.length },
    ...keys.map((key) => ({ key, label: taskTypeLabel(key, undefined, locale), count: counts.get(key) || 0 })),
  ];
});

const filteredModelOptions = computed(() => modelOptions.value.filter((model) => {
  const groupMatch = modelMatchesGroup(model, activeModelGroup.value);
  const taskMatch = activeTaskType.value === 'all' || normalizeTaskType(model) === activeTaskType.value;
  return groupMatch && taskMatch;
}));

const activeModelGroupLabel = computed(() => modelGroupTabs.value.find((group) => group.key === activeModelGroup.value)?.label || pageText.value.modelScope);
const activeTaskFilterLabel = computed(() => taskFilterTabs.value.find((task) => task.key === activeTaskType.value)?.label || pageText.value.allTasks);
const selectModelGroup = (key: ModelGroupKey) => {
  activeModelGroup.value = key;
  activeTaskType.value = 'all';
};
const selectTaskFilter = (key: TaskFilterKey) => {
  activeTaskType.value = key;
};

watch(modelOptions, () => {
  if (!modelGroupTabs.value.some((group) => group.key === activeModelGroup.value)) activeModelGroup.value = 'all';
  if (!taskFilterTabs.value.some((task) => task.key === activeTaskType.value)) activeTaskType.value = 'all';
});

const selectedModelOption = computed(() => modelOptions.value.find((model) => model.name === selectedModel.value));
const selectedModelLabel = computed(() => selectedModelOption.value ? modelDisplayName(selectedModelOption.value, locale) : modelDisplayName(selectedModel.value, locale));
const selectModel = (m: ModelOption) => { selectedModel.value = m.name; jobName.value = m.name + '-exp'; };
const displayModelName = (m: ModelOption) => modelDisplayName(m, locale);
const displayArchitecture = (name: string) => {
  const model = modelOptions.value.find((item) => item.name === name);
  return model ? modelDisplayName(model, locale) : modelDisplayName(name, locale);
};
const displayTaskType = (m: ModelOption) => taskTypeLabel(m.taskType, m.taskTypeZh, locale);
const displayModelDesc = (m: ModelOption) => {
  const desc = modelDescription(m, locale);
  return desc.length > 84 ? desc.slice(0, 84) + '...' : desc;
};
const formatParams = (m: ModelOption) => {
  const value = m.paramCountM ?? m.params;
  if (typeof value === 'number') return `${value}M`;
  return value ? String(value) : '--';
};
const metricLabel = (key: Parameters<typeof metricText>[0]) => metricText(key, locale).label;
const metricShortLabel = (key: Parameters<typeof metricText>[0]) => metricText(key, locale).shortLabel;
const metricDescription = (key: Parameters<typeof metricText>[0]) => metricText(key, locale).description;
const statusLabel = (status: string) => {
  if (!isZh.value) return status;
  const labels: Record<string, string> = { queued: '排队中', running: '运行中', completed: '已完成', failed: '失败', paused: '已暂停', stopped: '已停止' };
  return labels[status] || status;
};

const fetchModels = async () => {
  try {
    const res = await trainingApi.listModels();
    if (res.data.code === 200) {
      const official = (res.data.data.official || []).filter((model: ModelOption) => featuredOfficialModelNames.has(model.name));
      const userModels = res.data.data.userModels || [];
      modelOptions.value = [...official, ...userModels];
      if (!modelOptions.value.some((model) => model.name === selectedModel.value)) {
        if (modelOptions.value.length) selectModel(modelOptions.value[0]);
        else selectedModel.value = '';
      }
    }
  } catch { /* API unavailable */ }
};

const openModelArticle = async (articleId: number) => {
  articleDrawerOpen.value = true;
  articleLoading.value = true;
  activeArticle.value = null;
  try {
    const res = await trainingApi.getArticle(articleId);
    activeArticle.value = res.data.data || res.data;
  } catch {
    activeArticle.value = null;
  }
  articleLoading.value = false;
};

const createModel = async () => {
  try {
    const res = await trainingApi.createModel(newModel.value);
    if (res.data.code === 200) { showAddModel.value = false; fetchModels(); ElMessage.success(pageText.value.modelCreated); }
  } catch { /* API unavailable */ }
};

const jobs = ref<TrainingJob[]>([]);
const statusTag = (s: string) => s === 'running' ? 'primary' : s === 'completed' ? 'success' : s === 'failed' ? 'danger' : s === 'paused' ? 'warning' : 'info';
const jobProgress = (j: any) => j.epochs > 0 ? Math.round((j.currentEpoch || 0) / j.epochs * 100) : 0;

const fetchJobs = async () => {
  try { const res = await trainingApi.list(); if (res.data.code === 200) jobs.value = res.data.data || []; } catch { /* API unavailable */ }
};

const startTraining = async () => {
  if (!selectedModel.value) { ElMessage.warning(pageText.value.pickModel); return; }
  submitting.value = true;
  try {
    const res = await trainingApi.create({
      name: jobName.value, modelArchitecture: selectedModel.value,
      learningRate: lr.value, batchSize: bs.value, epochs: epochs.value,
      optimizer: optimizer.value, device: device.value,
    });
    if (res.data.code === 200) { ElMessage.success(pageText.value.jobCreated); await fetchJobs(); }
    else { ElMessage.error((res.data.message || pageText.value.createFailed) + ' [' + res.data.code + ']'); }
  } catch (e: any) { ElMessage.error(pageText.value.networkFailed + ': ' + (e.message || pageText.value.backendHint)); }
  submitting.value = false;
};

const startJob = async (j: TrainingJob) => { await trainingApi.start(j.id); fetchJobs(); };
const pauseJob = async (j: TrainingJob) => { await trainingApi.pause(j.id); fetchJobs(); };
const stopJob = async (j: TrainingJob) => { await trainingApi.stop(j.id); fetchJobs(); };
const deleteJob = async (j: TrainingJob) => {
  try { await ElMessageBox.confirm(pageText.value.deleteAction + ' ' + j.name + '?', pageText.value.deleteConfirm, { type: 'warning' }); await trainingApi.delete(j.id); ElMessage.success(pageText.value.deleted); fetchJobs(); } catch { /* cancelled */ }
};

const curveData = ref<{ loss: number[]; accuracy: number[]; valLoss: number[]; valAccuracy: number[] } | null>(null);

const fetchCurve = async (jobId: number) => {
  try {
    const res = await analysisApi.trainingCurve(jobId);
    if (res.data.code === 200 && res.data.data) {
      const d = res.data.data;
      curveData.value = {
        loss: d.loss || [],
        accuracy: d.accuracy || [],
        valLoss: d.valLoss || [],
        valAccuracy: d.valAccuracy || [],
      };
    }
  } catch { curveData.value = null; }
};

const updateChart = (job?: any) => {
  if (!chart) return;
  const cd = curveData.value;
  const hasRealData = cd && cd.loss.length > 0;
  const epochsArr = hasRealData
    ? Array.from({ length: cd!.loss.length }, (_, i) => i + 1)
    : (job?.currentEpoch ? Array.from({ length: job.currentEpoch }, (_, i) => i + 1) : Array.from({ length: 50 }, (_, i) => i + 1));
  const loss = hasRealData ? cd!.loss : epochsArr.map((_: any, i: number) => {
    const totalEpochs = job?.epochs || 100;
    const finalLoss = job?.currentLoss || 0.1;
    return +(2.5 * Math.exp(-3.5 * i / totalEpochs) + finalLoss + Math.random() * 0.05).toFixed(3);
  });
  const acc = hasRealData ? cd!.accuracy.map((v: number) => +(v * 100).toFixed(1)) : epochsArr.map((_: any, i: number) => {
    const totalEpochs = job?.epochs || 100;
    const finalAcc = job?.currentAccuracy || 0.9;
    return +(finalAcc * 100 * (1 - Math.exp(-4 * i / totalEpochs)) + Math.random() * 2).toFixed(1);
  });
  const valLoss = hasRealData && cd!.valLoss.length > 0 ? cd!.valLoss : [];
  const lossName = metricLabel('loss');
  const accuracyName = metricLabel('accuracy');
  const valLossName = metricLabel('valLoss');
  const series: any[] = [
    { name: lossName, type: 'line', data: loss, smooth: true, symbol: 'none', lineStyle: { color: '#f87171' }, areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(248,113,113,0.2)' }, { offset: 1, color: 'rgba(248,113,113,0)' }]) } },
    { name: accuracyName, type: 'line', yAxisIndex: 1, data: acc, smooth: true, symbol: 'none', lineStyle: { color: '#4ade80' }, areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(74,222,128,0.2)' }, { offset: 1, color: 'rgba(74,222,128,0)' }]) } },
  ];
  if (valLoss.length > 0) {
    series.push({ name: valLossName, type: 'line', data: valLoss, smooth: true, symbol: 'none', lineStyle: { color: '#fbbf24', type: 'dashed' } });
  }
  const legendData = valLoss.length > 0 ? [lossName, accuracyName, valLossName] : [lossName, accuracyName];
  chart.setOption({
    tooltip: { trigger: 'axis' }, legend: { data: legendData, bottom: 0 },
    grid: { left: 60, right: 60, top: 10, bottom: 30 },
    xAxis: { type: 'category', data: epochsArr }, yAxis: [{ type: 'value', name: metricShortLabel('loss'), min: 0 }, { type: 'value', name: metricShortLabel('accuracy') + ' %', max: 100 }],
    series,
  }, true);
};

onMounted(() => {
  if (isLoggedIn.value) { fetchModels(); fetchJobs(); }
  nextTick(() => { if (trainChartRef.value) { chart = echarts.init(trainChartRef.value); updateChart(); } });
  pollTimer = setInterval(async () => {
    if (!isLoggedIn.value) return;
    await fetchJobs();
    const running = jobs.value.find((j: any) => j.status === 'running');
    if (running) {
      await fetchCurve(running.id);
      updateChart(running);
    } else if (jobs.value.length > 0) {
      await fetchCurve(jobs.value[0].id);
      updateChart(jobs.value[0]);
    }
  }, 5000);
});
onUnmounted(() => { if (pollTimer) clearInterval(pollTimer); });
</script>

<style scoped>
.training-page { padding: 24px; max-width: 1400px; margin: 0 auto }
.page-header h2 { font-size: 22px; font-weight: var(--font-weight-body); color: var(--text-primary); margin: 0 0 4px }
.page-header p { font-size: 13px; color: var(--text-secondary); margin: 0 0 20px }
.login-hint { border-radius: 20px; text-align: center; padding: 60px }
.hint-content { display: flex; flex-direction: column; align-items: center; gap: 12px }
.hint-content h3 { font-size: 18px; font-weight: var(--font-weight-body); color: var(--text-primary); margin: 0 }
.hint-content p { font-size: 13px; color: var(--text-secondary); margin: 0 }
.training-guide { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 10px; margin-bottom: 18px }
.training-guide article { position: relative; min-height: 112px; padding: 16px; border: 1px solid rgba(var(--primary-rgb), .14); border-radius: 18px; background: radial-gradient(circle at 12% 0%, rgba(var(--primary-rgb), .16), transparent 46%), color-mix(in srgb, var(--surface-1) 78%, transparent); overflow: hidden }
.training-guide article::after { content: ""; position: absolute; right: -18px; bottom: -28px; width: 92px; height: 92px; border-radius: 50%; border: 1px solid rgba(var(--primary-rgb), .16) }
.training-guide span { display: block; color: var(--primary-color); font-size: 11px; font-weight: var(--font-weight-title); letter-spacing: .12em }
.training-guide strong { display: block; margin-top: 10px; color: var(--text-primary); font-size: 15px; font-weight: var(--font-weight-title) }
.training-guide p { margin: 6px 0 0; color: var(--text-secondary); font-size: 12px; line-height: 1.55 }
.model-step-section { width: 100%; box-sizing: border-box; margin-bottom: 16px; padding: 14px; border: 1px solid var(--border-color); border-radius: var(--radius-lg); background: linear-gradient(180deg, rgba(255, 255, 255, 0.05), rgba(255, 255, 255, 0.01) 40%, rgba(0, 0, 0, 0.03)), rgba(var(--glass-bg-rgb), var(--glass-opacity)); box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.06), inset 0 -1px 0 rgba(0, 0, 0, 0.06), var(--shadow-soft); backdrop-filter: blur(var(--glass-blur)) saturate(var(--glass-saturate)); -webkit-backdrop-filter: blur(var(--glass-blur)) saturate(var(--glass-saturate)) }
.step-header { display: flex; align-items: flex-end; justify-content: space-between; gap: 12px; font-size: 14px; font-weight: var(--font-weight-body); color: var(--text-primary); margin-bottom: 12px }
.step-header > div { display: flex; align-items: center; flex-wrap: wrap; gap: 8px }
.step-header small { width: 100%; margin-left: 30px; color: var(--text-muted); font-size: 11px; font-weight: var(--font-weight-body); line-height: 1.45 }
.step-badge { width: 22px; height: 22px; border-radius: 50%; background: var(--primary-color); color: #fff; display: flex; align-items: center; justify-content: center; font-size: 12px; font-weight: var(--font-weight-title); flex-shrink: 0 }
.selected-hint { font-size: 12px; color: var(--text-secondary); font-weight: 600 }
.selected-hint strong { color: var(--primary-color) }
.selected-model-name { font-size: 11px; color: var(--primary-color); font-weight: var(--font-weight-body) }
.model-browser { display: grid; grid-template-columns: minmax(180px, 220px) minmax(0, 1fr); gap: 14px; align-items: start; width: 100%; min-width: 0; box-sizing: border-box }
.model-group-menu { display: grid; align-content: start; gap: 8px; padding: 10px; border: 1px solid var(--border-color); border-radius: 16px; background: color-mix(in srgb, var(--surface-1) 78%, transparent) }
.model-group-tab { width: 100%; min-height: 58px; padding: 10px 12px; border: 1px solid transparent; border-radius: 12px; background: transparent; color: var(--text-secondary); display: flex; align-items: center; justify-content: space-between; gap: 10px; text-align: left; cursor: pointer; transition: border-color .2s ease, background .2s ease, color .2s ease, transform .2s ease }
.model-group-tab:hover { color: var(--text-primary); background: color-mix(in srgb, var(--surface-2) 76%, transparent); transform: translateX(2px) }
.model-group-tab.active { color: var(--text-primary); border-color: rgba(var(--primary-rgb), .32); background: linear-gradient(135deg, rgba(var(--primary-rgb), .16), rgba(var(--primary-rgb), .04)); box-shadow: inset 0 0 0 1px rgba(255, 255, 255, .08) }
.group-copy { display: grid; gap: 4px; min-width: 0 }
.group-copy strong { font-size: 13px; font-weight: var(--font-weight-title); line-height: 1.2 }
.group-copy small { font-size: 10px; color: var(--text-muted); line-height: 1.35; white-space: normal }
.model-group-tab em { min-width: 28px; height: 22px; padding: 0 8px; border-radius: 999px; background: rgba(var(--primary-rgb), .1); color: var(--primary-color); display: inline-flex; align-items: center; justify-content: center; font-size: 11px; font-style: normal; font-weight: var(--font-weight-title) }
.model-results-panel { min-width: 0; width: 100%; box-sizing: border-box; padding: 12px; border: 1px solid var(--border-color); border-radius: 16px; background: color-mix(in srgb, var(--surface-1) 66%, transparent) }
.model-filter-bar { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 10px }
.task-filter-list { display: flex; align-items: center; gap: 8px; overflow-x: auto; scrollbar-width: thin; padding-bottom: 2px }
.task-filter-btn,
.model-register-btn { min-height: 34px; border: 1px solid var(--border-color); border-radius: 999px; background: color-mix(in srgb, var(--surface-2) 72%, transparent); color: var(--text-secondary); display: inline-flex; align-items: center; gap: 7px; padding: 6px 11px; font-size: 12px; font-weight: var(--font-weight-body); white-space: nowrap; cursor: pointer; transition: border-color .2s ease, background .2s ease, color .2s ease, transform .2s ease }
.task-filter-btn:hover,
.model-register-btn:hover { color: var(--text-primary); border-color: rgba(var(--primary-rgb), .36); transform: translateY(-1px) }
.task-filter-btn.active { color: var(--primary-color); border-color: rgba(var(--primary-rgb), .44); background: rgba(var(--primary-rgb), .12) }
.task-filter-btn em { color: inherit; font-style: normal; opacity: .76 }
.model-register-btn { flex-shrink: 0; border-style: dashed }
.model-register-btn span { width: 17px; height: 17px; border-radius: 50%; background: var(--primary-color); color: white; display: inline-flex; align-items: center; justify-content: center; font-weight: var(--font-weight-title) }
.model-result-head { display: flex; align-items: center; justify-content: space-between; gap: 10px; margin-bottom: 10px; color: var(--text-muted); font-size: 11px }
.model-result-head span { color: var(--text-secondary); font-weight: var(--font-weight-title) }
.model-result-head em { font-style: normal }
.model-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 8px; min-width: 0 }
.model-chip { position: relative; display: flex; align-items: stretch; gap: 6px; min-height: 74px; padding: 10px 12px; border-radius: 12px; border: 1px solid var(--border-color); background: var(--panel-bg); cursor: pointer; overflow: hidden; transition: border-color .2s ease, background .2s ease, box-shadow .2s ease, transform .2s ease }
.model-chip:hover { border-color: rgba(var(--primary-rgb), 0.58); transform: translateY(-1px); box-shadow: 0 10px 24px rgba(var(--primary-rgb), 0.1) }
.model-chip.active {
  border-color: var(--primary-color);
  background:
    radial-gradient(circle at 18% 0%, rgba(var(--primary-rgb), 0.22), transparent 42%),
    rgba(var(--primary-rgb), 0.12);
  box-shadow:
    0 0 0 2px rgba(var(--primary-rgb), 0.34),
    0 14px 34px rgba(var(--primary-rgb), 0.22),
    inset 0 0 0 1px rgba(255, 255, 255, 0.16);
}
.model-chip.active::after {
  content: "";
  position: absolute;
  inset: 4px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 9px;
  pointer-events: none;
}
.model-chip.active::before {
  content: "";
  position: absolute;
  top: 8px;
  right: 9px;
  width: 7px;
  height: 12px;
  border-right: 2px solid var(--primary-color);
  border-bottom: 2px solid var(--primary-color);
  filter: drop-shadow(0 0 6px rgba(var(--primary-rgb), 0.68));
  pointer-events: none;
  transform: rotate(42deg);
}
.chip-body { display: flex; align-items: flex-start; justify-content: space-between; gap: 10px; flex: 1; min-width: 0 }
.chip-copy { display: grid; gap: 4px; min-width: 0; flex: 1 }
.chip-name { font-size: 13px; font-weight: var(--font-weight-body); color: var(--text-primary) }
.chip-meta { font-size: 10px; color: var(--text-secondary) }
.chip-desc { max-width: 420px; color: var(--text-muted); font-size: 10px; line-height: 1.45 }
.chip-flag {
  flex-shrink: 0;
  align-self: flex-start;
  margin-top: 1px;
  padding: 1px 6px;
  border: 1px solid rgba(var(--primary-rgb), 0.22);
  border-radius: 999px;
  background: rgba(var(--primary-rgb), 0.08);
  color: var(--primary-color);
  font-size: 9px;
  font-weight: var(--font-weight-body);
  line-height: 1.35;
}
.custom-flag { color: var(--accent-glow); border-color: color-mix(in srgb, var(--accent-glow) 32%, transparent); background: color-mix(in srgb, var(--accent-glow) 11%, transparent) }
.model-empty-state { min-height: 170px; border: 1px dashed rgba(var(--primary-rgb), .24); border-radius: 14px; background: rgba(var(--primary-rgb), .04); display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8px; text-align: center; color: var(--text-secondary) }
.model-empty-state strong { color: var(--text-primary); font-size: 14px }
.model-empty-state span { font-size: 12px }
.model-empty-state button { min-height: 34px; padding: 6px 13px; border: 1px solid rgba(var(--primary-rgb), .36); border-radius: 999px; background: rgba(var(--primary-rgb), .12); color: var(--primary-color); cursor: pointer }
.card-header { display: flex; align-items: center; gap: 8px; font-weight: var(--font-weight-body); color: var(--text-primary) }
.stacked-card-header { display: grid; align-items: start; gap: 5px }
.stacked-card-header > span { display: flex; align-items: center; gap: 8px }
.stacked-card-header small { color: var(--text-muted); font-size: 11px; font-weight: var(--font-weight-body); line-height: 1.45 }
.selected-model-card { margin-bottom: 14px; padding: 13px 14px; border: 1px solid rgba(var(--primary-rgb), .18); border-radius: 14px; background: linear-gradient(135deg, rgba(var(--primary-rgb), .12), transparent 66%), color-mix(in srgb, var(--surface-2) 70%, transparent) }
.selected-model-card span { display: block; color: var(--text-muted); font-size: 10px; font-weight: var(--font-weight-title); text-transform: uppercase }
.selected-model-card strong { display: block; margin-top: 5px; color: var(--text-primary); font-size: 15px; font-weight: var(--font-weight-title) }
.selected-model-card em { display: block; margin-top: 4px; color: var(--text-secondary); font-size: 11px; font-style: normal; font-weight: var(--font-weight-body) }
.chart-card-header { justify-content: space-between; align-items: flex-start }
.chart-card-header span { display: flex; align-items: center; gap: 8px }
.chart-card-header small { max-width: 320px; color: var(--text-muted); font-size: 11px; font-weight: var(--font-weight-body); line-height: 1.45; text-align: right }
.config-card, .chart-card, .job-card { border-radius: 16px; margin-bottom: 16px }
.chart-box { width: 100%; height: 300px }
.metric-explainers { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 8px; margin-top: 10px }
.metric-explainers span { padding: 9px 10px; border: 1px solid var(--border-color); border-radius: 10px; background: color-mix(in srgb, var(--surface-2) 76%, transparent); color: var(--text-secondary); font-size: 11px; line-height: 1.55 }
.metric-explainers strong { display: block; margin-bottom: 3px; color: var(--text-primary); font-size: 12px }
.job-item { display: flex; align-items: center; gap: 16px; padding: 12px 0; border-bottom: 1px solid var(--border-color) }
.job-info { width: 180px }
.job-name { font-weight: var(--font-weight-body); display: block; color: var(--text-primary) }
.job-model { font-size: 11px; color: var(--text-secondary) }
.job-progress { flex: 1 }
.job-meta { display: flex; gap: 12px; align-items: center; width: 200px; font-size: 12px; color: var(--text-primary) }
.job-actions { display: flex; flex-wrap: wrap; justify-content: flex-end; gap: 8px; min-width: 190px }
.job-actions :deep(.el-button) { min-height: 34px !important; padding: 6px 12px !important }
.mt-4 { margin-top: 16px }
@media (max-width: 980px) {
  .training-guide { grid-template-columns: repeat(2, minmax(0, 1fr)) }
  .model-browser { grid-template-columns: 1fr }
  .model-group-menu { grid-template-columns: repeat(2, minmax(0, 1fr)) }
  .model-group-tab:hover { transform: translateY(-1px) }
  .metric-explainers { grid-template-columns: 1fr }
  .chart-card-header { display: grid; gap: 6px }
  .chart-card-header small { text-align: left }
  .job-item { align-items: flex-start; flex-wrap: wrap }
  .job-info, .job-meta { width: auto; min-width: 180px }
  .job-actions { justify-content: flex-start }
}
@media (max-width: 640px) {
  .training-guide { grid-template-columns: 1fr }
  .step-header { align-items: flex-start; display: grid }
  .model-group-menu { grid-template-columns: 1fr }
  .model-filter-bar { align-items: stretch; flex-direction: column }
  .model-register-btn { justify-content: center }
  .model-grid { grid-template-columns: 1fr }
}
</style>
