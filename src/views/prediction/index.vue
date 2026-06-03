<template>
  <div class="prediction-page">
    <div class="page-header entrance-hero">
      <div>
        <span class="page-kicker">{{ pageText.kicker }}</span>
        <h2>{{ $t('nav.prediction') }}</h2>
        <p>{{ $t('prediction.subtitle') }}</p>
      </div>
      <div class="service-chip" :class="serviceState">
        <component :is="statusIcon" :size="17" />
        <span>{{ statusLabel }}</span>
      </div>
    </div>

    <section class="inference-layout">
      <aside class="config-panel entrance-up" style="animation-delay: 0.08s">
        <div class="panel-heading">
          <span>{{ pageText.config }}</span>
          <strong>{{ pageText.configTitle }}</strong>
        </div>

        <div class="model-summary">
          <Server :size="22" />
          <div>
            <strong>{{ selectedModel ? displayModelName(selectedModel) : 'BSARec-Job' }}</strong>
            <p>{{ selectedModelDescription }}</p>
          </div>
        </div>

        <el-form label-position="top" size="small" class="inference-form">
          <el-form-item :label="pageText.selectModel">
            <el-select v-model="inferModel" filterable style="width: 100%">
              <el-option
                v-for="model in integratedModels"
                :key="model.name"
                :label="displayModelName(model)"
                :value="model.name"
              >
                <span>{{ displayModelName(model) }}</span>
                <small class="option-meta">{{ displayTaskType(model) }}</small>
              </el-option>
            </el-select>
          </el-form-item>

          <el-form-item :label="pageText.userHistory">
            <el-input
              v-model="historyInput"
              type="textarea"
              :rows="4"
              :placeholder="pageText.historyPlaceholder"
            />
          </el-form-item>

          <div class="sample-row">
            <span>{{ pageText.samples }}</span>
            <button
              v-for="sample in sampleHistories"
              :key="sample.label"
              type="button"
              @click="applySample(sample.value)"
            >
              {{ sample.label }}
            </button>
          </div>

          <el-form-item :label="pageText.topK">
            <el-slider v-model="topK" :min="1" :max="20" :step="1" show-input />
          </el-form-item>

          <el-form-item>
            <el-checkbox v-model="includeJobInfo">{{ pageText.includeJobInfo }}</el-checkbox>
          </el-form-item>

          <el-button
            type="primary"
            class="run-button"
            :loading="inferring"
            :style="{ backgroundColor: 'var(--primary-color)' }"
            @click="runInference"
          >
            <Play :size="16" />
            {{ pageText.start }}
          </el-button>
        </el-form>
      </aside>

      <main class="result-panel entrance-up" style="animation-delay: 0.14s">
        <div class="status-banner" :class="serviceState">
          <component :is="statusIcon" :size="22" />
          <div>
            <strong>{{ statusTitle }}</strong>
            <p>{{ statusDescription }}</p>
          </div>
          <span>{{ responseServiceUrl }}</span>
        </div>

        <div class="summary-grid">
          <article v-for="item in summaryCards" :key="item.label">
            <component :is="item.icon" :size="18" />
            <div>
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </article>
        </div>

        <div v-if="analysisMetricCards.length" class="analysis-metrics-grid">
          <article v-for="item in analysisMetricCards" :key="item.label">
            <component :is="item.icon" :size="18" />
            <div>
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </article>
        </div>

        <div class="analysis-action">
          <div>
            <strong>{{ pageText.analysisTitle }}</strong>
            <p>{{ pageText.analysisDesc }}</p>
          </div>
          <el-button
            type="primary"
            plain
            :disabled="!currentAnalysisRecord"
            @click="openVisualizationAnalysis"
          >
            <Send :size="15" />
            {{ pageText.sendToViz }}
          </el-button>
        </div>

        <div v-if="recommendations.length" class="job-grid">
          <article v-for="item in recommendations" :key="item.itemId + item.rank" class="job-card">
            <div class="job-rank">#{{ item.rank }}</div>
            <div class="job-body">
              <h3>{{ item.position || pageText.unknownJob }}</h3>
              <p>{{ item.company || pageText.unknownCompany }}</p>
              <div class="job-meta">
                <span>ID {{ item.itemId }}</span>
                <span v-if="item.salary">{{ item.salary }}</span>
                <span v-if="item.score !== undefined">{{ pageText.score }} {{ formatScore(item.score) }}</span>
              </div>
            </div>
          </article>
        </div>

        <div v-else class="diagnostic-empty" :class="serviceState">
          <component :is="statusIcon" :size="34" />
          <strong>{{ emptyTitle }}</strong>
          <p>{{ emptyDescription }}</p>
          <small v-if="serviceDetail">{{ serviceDetail }}</small>
        </div>
      </main>
    </section>

    <section class="evidence-grid entrance-up" style="animation-delay: 0.2s">
      <article class="evidence-card">
        <div class="panel-heading compact">
          <span>{{ pageText.requestPreview }}</span>
          <strong>{{ pageText.requestTitle }}</strong>
        </div>
        <div class="payload-summary">
          <div>
            <span>{{ pageText.historyCount }}</span>
            <strong>{{ activeHistoryPreview }}</strong>
          </div>
          <div>
            <span>{{ pageText.topK }}</span>
            <strong>{{ activeTopK }}</strong>
          </div>
          <div>
            <span>{{ pageText.includeJobInfo }}</span>
            <strong>{{ activeIncludeJobInfo }}</strong>
          </div>
        </div>
        <pre>{{ requestJson }}</pre>
      </article>

      <article class="evidence-card">
        <div class="panel-heading compact">
          <span>{{ pageText.integration }}</span>
          <strong>{{ pageText.integrationTitle }}</strong>
        </div>
        <div class="route-list">
          <div v-for="route in integrationRoutes" :key="route.label">
            <span>{{ route.label }}</span>
            <strong>{{ route.value }}</strong>
          </div>
        </div>
      </article>

      <article class="evidence-card">
        <div class="panel-heading compact">
          <span>{{ pageText.lastResponse }}</span>
          <strong>{{ pageText.lastResponseTitle }}</strong>
        </div>
        <div v-if="currentAnalysisRecord" class="response-digest">
          <div>
            <span>{{ pageText.fillRate }}</span>
            <strong>{{ formatPercent(currentAnalysisRecord.metrics.fillRate) }}</strong>
          </div>
          <div>
            <span>{{ pageText.detailCoverage }}</span>
            <strong>{{ formatPercent(currentAnalysisRecord.metrics.detailCoverage) }}</strong>
          </div>
          <div>
            <span>{{ pageText.latency }}</span>
            <strong>{{ currentAnalysisRecord.elapsedMs ?? '--' }}ms</strong>
          </div>
        </div>
        <pre>{{ responseJson }}</pre>
      </article>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import axios from 'axios';
import {
  AlertTriangle,
  BarChart3,
  BriefcaseBusiness,
  Building2,
  CheckCircle2,
  ClipboardList,
  Gauge,
  History,
  Play,
  Server,
  Send,
  WifiOff,
} from 'lucide-vue-next';
import { useI18n } from 'vue-i18n';
import { useRouter } from 'vue-router';
import { predictionApi } from '@/api';
import { ROUTES } from '@/constants';
import type { ModelOption } from '@/types/models';
import { modelDescription, modelDisplayName, taskTypeLabel } from '@/utils/modelDisplay';
import {
  buildPredictionAnalysisRecord,
  savePredictionAnalysisRecord,
  selectPredictionAnalysisRecord,
} from '@/utils/predictionAnalysis';
import type { PredictionAnalysisRecord } from '@/utils/predictionAnalysis';

type RecommendationStatus = 'idle' | 'success' | 'offline' | 'error';

type RecommendationItem = {
  rank: number
  itemId: string
  position?: string
  company?: string
  salary?: string
  score?: number
}

type RecommendRequest = {
  user_history: number[]
  top_k: number
  include_job_info: boolean
}

const { locale } = useI18n();
const router = useRouter();
const inferModel = ref('BSARec-Job');
const modelList = ref<ModelOption[]>([]);
const historyInput = ref('1, 2, 3, 4, 5');
const topK = ref(10);
const includeJobInfo = ref(true);
const inferring = ref(false);
const serviceState = ref<RecommendationStatus>('idle');
const serviceMessage = ref('');
const serviceDetail = ref('');
const responseServiceUrl = ref('http://127.0.0.1:5000/recommend');
const elapsedMs = ref<number | null>(null);
const recommendations = ref<RecommendationItem[]>([]);
const lastRequest = ref<RecommendRequest | null>(null);
const lastResponse = ref<Record<string, unknown> | null>(null);
const savedAnalysisRecordId = ref('');

const isZh = computed(() => locale.value.startsWith('zh'));

const pageText = computed(() => (isZh.value ? {
  kicker: 'BSARec 在线推荐',
  config: '请求配置',
  configTitle: '岗位推荐推理输入',
  selectModel: '已接入模型',
  userHistory: '用户历史岗位 ID',
  historyPlaceholder: '例如：1, 2, 3, 4, 5',
  samples: '样例序列',
  topK: '推荐数量 Top-K',
  includeJobInfo: '返回岗位详情',
  start: '请求 BSARec 推荐',
  statusIdle: '未请求',
  statusReady: '服务已返回',
  statusOffline: 'BSARec 服务离线',
  statusError: '请求异常',
  idleTitle: '等待推荐请求',
  idleDesc: '输入岗位历史 ID 后，页面会请求后端真实推荐接口。',
  successTitle: '推荐结果已返回',
  successDesc: '下方岗位来自 BSARec 推荐接口响应。',
  offlineTitle: '本地 BSARec Flask API 未连通',
  offlineDesc: '后端接口可用，但没有收到本地模型服务响应；页面不会生成模拟推荐。',
  errorTitle: '推荐请求失败',
  errorDesc: '请求没有完成，请检查登录状态、后端服务和 BSARec API。',
  noResultTitle: '暂无岗位推荐',
  noResultDesc: '接口没有返回 recommendations，响应内容会保留在下方用于排查。',
  invalidHistory: '请至少输入一个有效的正整数岗位 ID',
  requestForbidden: '推荐接口被后端权限拦截，请重启 DeepInsight 后端。',
  backendUnavailable: 'DeepInsight 后端未连接，请先启动后端服务。',
  bsarecRequestFailed: 'BSARec 请求失败，请确认后端和本地 Flask API 已启动。',
  unknownJob: '未返回岗位名称',
  unknownCompany: '未返回公司',
  score: '得分',
  recCount: '推荐数',
  latency: '延迟',
  device: '设备',
  historyCount: '历史长度',
  requestPreview: '请求载荷',
  requestTitle: '实际发送给后端的参数',
  integration: '接口链路',
  integrationTitle: '当前页面真实连接的位置',
  lastResponse: '最后响应',
  lastResponseTitle: '后端返回原文',
  noResponse: '尚未请求',
  modelSource: '模型注册',
  backendEndpoint: '平台接口',
  serviceEndpoint: '模型服务',
  inputContract: '输入约束',
  fillRate: 'Top-K 返回率',
  detailCoverage: '详情覆盖率',
  companyCoverage: '公司覆盖',
  averageScore: '平均得分',
  analysisTitle: '推理结果可视化',
  analysisDesc: '保存本次请求、响应、延迟和推荐分布，并在可视化分析页生成模型推理视图。',
  sendToViz: '送往可视化分析',
  sentToViz: '已发送到可视化分析',
  yes: '是',
  no: '否',
} : {
  kicker: 'BSARec Online Recommendation',
  config: 'Request Config',
  configTitle: 'Job recommendation input',
  selectModel: 'Integrated Model',
  userHistory: 'User History Job IDs',
  historyPlaceholder: 'Example: 1, 2, 3, 4, 5',
  samples: 'Samples',
  topK: 'Top K',
  includeJobInfo: 'Return job details',
  start: 'Request BSARec Recommendations',
  statusIdle: 'Not requested',
  statusReady: 'Service returned',
  statusOffline: 'BSARec offline',
  statusError: 'Request error',
  idleTitle: 'Waiting for a recommendation request',
  idleDesc: 'Enter historical job IDs to call the real backend recommendation endpoint.',
  successTitle: 'Recommendations returned',
  successDesc: 'Jobs below come from the BSARec recommendation response.',
  offlineTitle: 'Local BSARec Flask API is unreachable',
  offlineDesc: 'The backend endpoint responded, but the local model service did not. No mock recommendations are generated.',
  errorTitle: 'Recommendation request failed',
  errorDesc: 'Check login state, backend service, and the BSARec API.',
  noResultTitle: 'No recommendations yet',
  noResultDesc: 'The endpoint returned no recommendations. The raw response is kept below for diagnostics.',
  invalidHistory: 'Enter at least one valid positive job ID',
  requestForbidden: 'The recommendation endpoint is blocked by backend security. Restart the DeepInsight backend.',
  backendUnavailable: 'DeepInsight backend is not reachable. Start the backend service first.',
  bsarecRequestFailed: 'BSARec request failed. Check that the backend and local Flask API are running.',
  unknownJob: 'Job name not returned',
  unknownCompany: 'Company not returned',
  score: 'Score',
  recCount: 'Recommendations',
  latency: 'Latency',
  device: 'Device',
  historyCount: 'History length',
  requestPreview: 'Request Payload',
  requestTitle: 'Parameters sent to backend',
  integration: 'Integration',
  integrationTitle: 'Real connection points',
  lastResponse: 'Last Response',
  lastResponseTitle: 'Raw backend payload',
  noResponse: 'Not requested yet',
  modelSource: 'Model registry',
  backendEndpoint: 'Platform endpoint',
  serviceEndpoint: 'Model service',
  inputContract: 'Input contract',
  fillRate: 'Top-K fill rate',
  detailCoverage: 'Detail coverage',
  companyCoverage: 'Company coverage',
  averageScore: 'Average score',
  analysisTitle: 'Visualize this inference',
  analysisDesc: 'Save the request, response, latency, and recommendation distribution for model inference analysis.',
  sendToViz: 'Send to Analysis',
  sentToViz: 'Sent to visual analysis',
  yes: 'Yes',
  no: 'No',
}));

const bsarecModelOption = (): ModelOption => ({
  name: 'BSARec-Job',
  displayNameZh: 'BSARec 岗位推荐模型',
  taskType: 'recommendation',
  taskTypeZh: '推荐系统',
  paramCountM: 0.2,
  inputSize: '50 item ids',
  framework: 'pytorch/cpu',
  description: 'BSARec sequential recommendation model served by the local Flask API.',
  descriptionZh: '通过本地 Flask API 提供 CPU 推理的 BSARec 序列推荐模型。',
  isOfficial: true,
});

const integratedModels = computed<ModelOption[]>(() => {
  const registered = modelList.value.find((model) => model.name === 'BSARec-Job');
  return [registered || bsarecModelOption()];
});

const selectedModel = computed(() => integratedModels.value.find((model) => model.name === inferModel.value) || integratedModels.value[0]);
const selectedModelDescription = computed(() => selectedModel.value ? modelDescription(selectedModel.value, locale) : '');

const parsedHistory = computed(() => parseHistory(historyInput.value));
const requestPayload = computed<RecommendRequest>(() => ({
  user_history: parsedHistory.value,
  top_k: topK.value,
  include_job_info: includeJobInfo.value,
}));

const statusLabel = computed(() => ({
  idle: pageText.value.statusIdle,
  success: pageText.value.statusReady,
  offline: pageText.value.statusOffline,
  error: pageText.value.statusError,
}[serviceState.value]));

const statusTitle = computed(() => ({
  idle: pageText.value.idleTitle,
  success: pageText.value.successTitle,
  offline: pageText.value.offlineTitle,
  error: pageText.value.errorTitle,
}[serviceState.value]));

const statusDescription = computed(() => {
  if (serviceState.value === 'success' && serviceMessage.value) return serviceMessage.value;
  if (serviceState.value === 'error' && serviceMessage.value) return serviceMessage.value;
  return {
    idle: pageText.value.idleDesc,
    success: pageText.value.successDesc,
    offline: pageText.value.offlineDesc,
    error: pageText.value.errorDesc,
  }[serviceState.value];
});

const statusIcon = computed(() => {
  if (serviceState.value === 'success') return CheckCircle2;
  if (serviceState.value === 'offline') return WifiOff;
  if (serviceState.value === 'error') return AlertTriangle;
  return Server;
});

const emptyTitle = computed(() => {
  if (serviceState.value === 'offline') return pageText.value.offlineTitle;
  if (serviceState.value === 'error') return pageText.value.errorTitle;
  if (lastResponse.value) return pageText.value.noResultTitle;
  return pageText.value.idleTitle;
});

const emptyDescription = computed(() => {
  if (serviceState.value === 'offline') return pageText.value.offlineDesc;
  if (serviceState.value === 'error') return pageText.value.errorDesc;
  if (lastResponse.value) return pageText.value.noResultDesc;
  return pageText.value.idleDesc;
});

const summaryCards = computed(() => [
  { label: pageText.value.recCount, value: String(recommendations.value.length), icon: BriefcaseBusiness },
  { label: pageText.value.latency, value: elapsedMs.value === null ? '--' : `${elapsedMs.value}ms`, icon: Gauge },
  { label: pageText.value.historyCount, value: String(parsedHistory.value.length), icon: History },
  { label: pageText.value.device, value: 'CPU / Flask', icon: Server },
]);

const currentAnalysisRecord = computed<PredictionAnalysisRecord | null>(() => {
  if (!lastRequest.value || !lastResponse.value) return null;
  return buildPredictionAnalysisRecord({
    id: savedAnalysisRecordId.value || undefined,
    modelName: 'BSARec-Job',
    request: lastRequest.value,
    response: lastResponse.value,
    status: serviceState.value,
    message: serviceMessage.value || statusDescription.value,
    serviceUrl: responseServiceUrl.value,
    elapsedMs: elapsedMs.value,
    recommendations: recommendations.value,
  });
});

const analysisMetricCards = computed(() => {
  const record = currentAnalysisRecord.value;
  if (!record) return [];
  return [
    { label: pageText.value.fillRate, value: formatPercent(record.metrics.fillRate), icon: ClipboardList },
    { label: pageText.value.detailCoverage, value: formatPercent(record.metrics.detailCoverage), icon: BarChart3 },
    { label: pageText.value.companyCoverage, value: record.metrics.recommendationCount ? `${record.metrics.uniqueCompanyCount}/${record.metrics.recommendationCount}` : '0/0', icon: Building2 },
    { label: pageText.value.averageScore, value: record.metrics.averageScore === null ? '--' : formatScore(record.metrics.averageScore), icon: Gauge },
  ];
});

const integrationRoutes = computed(() => [
  { label: pageText.value.modelSource, value: 'PredictionController.listModels / BSARec-Job' },
  { label: pageText.value.backendEndpoint, value: '/api/v1/prediction/recommend' },
  { label: pageText.value.serviceEndpoint, value: responseServiceUrl.value },
  { label: pageText.value.inputContract, value: 'user_history, top_k, include_job_info' },
]);

const requestJson = computed(() => formatJson(lastRequest.value || requestPayload.value));
const responseJson = computed(() => lastResponse.value ? formatJson(lastResponse.value) : pageText.value.noResponse);
const activeRequest = computed(() => lastRequest.value || requestPayload.value);
const activeHistoryPreview = computed(() => compactHistory(activeRequest.value.user_history));
const activeTopK = computed(() => String(activeRequest.value.top_k));
const activeIncludeJobInfo = computed(() => activeRequest.value.include_job_info ? pageText.value.yes : pageText.value.no);

const sampleHistories = computed(() => [
  { label: isZh.value ? '短序列' : 'Short', value: '1, 2, 3, 4, 5' },
  { label: isZh.value ? '中等序列' : 'Medium', value: '7, 11, 18, 24, 35, 42, 56, 63' },
  { label: isZh.value ? '长序列' : 'Long', value: '3, 9, 12, 19, 27, 31, 44, 58, 71, 86, 94, 108' },
]);

const displayModelName = (model: ModelOption) => modelDisplayName(model, locale);
const displayTaskType = (model: ModelOption) => taskTypeLabel(model.taskType, model.taskTypeZh, locale);

function parseHistory(value: string) {
  return value
    .split(/[\s,，;；]+/)
    .map((item) => Number.parseInt(item.trim(), 10))
    .filter((item) => Number.isInteger(item) && item > 0);
}

function normalizeRecommendation(item: any, index: number): RecommendationItem {
  const jobInfo = item?.job_info || item?.jobInfo || {};
  const itemId = String(item?.item_id ?? item?.itemId ?? item?.id ?? item ?? '');
  const rawScore = item?.score ?? item?.confidence;
  const score = typeof rawScore === 'number' ? rawScore : Number.parseFloat(rawScore);
  return {
    rank: Number(item?.rank || index + 1),
    itemId,
    position: jobInfo.position || jobInfo.title || item?.position,
    company: jobInfo.company || item?.company,
    salary: jobInfo.salary || item?.salary,
    score: Number.isFinite(score) ? score : undefined,
  };
}

function formatJson(value: unknown) {
  return JSON.stringify(value, null, 2);
}

function formatScore(value: number) {
  return Math.abs(value) >= 1 ? value.toFixed(3) : value.toFixed(4);
}

function formatPercent(value: number) {
  return `${Math.round(value * 100)}%`;
}

function compactHistory(ids: number[]) {
  if (!ids.length) return '--';
  const preview = ids.slice(0, 8).join(', ');
  return ids.length > 8 ? `${preview} ... (${ids.length})` : preview;
}

function applySample(value: string) {
  historyInput.value = value;
}

function saveCurrentAnalysisRecord() {
  const record = currentAnalysisRecord.value;
  if (!record) return null;
  const saved = savePredictionAnalysisRecord(record);
  savedAnalysisRecordId.value = saved.id;
  return saved;
}

function openVisualizationAnalysis() {
  const saved = saveCurrentAnalysisRecord();
  if (!saved) {
    ElMessage.warning(pageText.value.noResponse);
    return;
  }
  selectPredictionAnalysisRecord(saved.id);
  ElMessage.success(pageText.value.sentToViz);
  void router.push(ROUTES.VIZ);
}

function localizeBackendMessage(message: string, status?: unknown) {
  const normalized = message.toLowerCase();
  if (status === 'offline' || normalized.includes('not reachable') || normalized.includes('connection refused')) {
    return pageText.value.offlineDesc;
  }
  if (normalized.includes('empty response')) {
    return isZh.value ? 'BSARec 服务返回了空响应，请检查 Flask API 的返回格式。' : 'BSARec returned an empty response. Check the Flask API response format.';
  }
  return message;
}

function localizeBackendDetail(detail: string) {
  const normalized = detail.toLowerCase();
  if (normalized.includes('connection refused')) {
    return isZh.value
      ? '原始错误：连接被拒绝。请确认本地 BSARec Flask API 已在 127.0.0.1:5000 启动，并暴露 /recommend 接口。'
      : 'Raw error: connection refused. Start the local BSARec Flask API at 127.0.0.1:5000 with /recommend enabled.';
  }
  return detail;
}

async function loadModels() {
  try {
    const response = await predictionApi.listModels();
    if (response.data.code === 200) {
      modelList.value = response.data.data || [];
    }
  } catch {
    modelList.value = [bsarecModelOption()];
  }
  inferModel.value = integratedModels.value[0]?.name || 'BSARec-Job';
}

async function runInference() {
  const history = parsedHistory.value;
  if (!history.length) {
    ElMessage.warning(pageText.value.invalidHistory);
    return;
  }

  inferring.value = true;
  serviceMessage.value = '';
  serviceDetail.value = '';
  recommendations.value = [];
  const startedAt = performance.now();
  const payload = { ...requestPayload.value, user_history: history };
  lastRequest.value = payload;
  savedAnalysisRecordId.value = '';

  try {
    const response = await predictionApi.recommend(payload);
    elapsedMs.value = Math.max(1, Math.round(performance.now() - startedAt));
    const data = response.data.data || {};
    lastResponse.value = data;
    responseServiceUrl.value = String(data.serviceUrl || responseServiceUrl.value);
    serviceMessage.value = localizeBackendMessage(String(data.message || ''), data.status);
    serviceDetail.value = localizeBackendDetail(String(data.detail || ''));
    recommendations.value = (Array.isArray(data.recommendations) ? data.recommendations : []).map(normalizeRecommendation);

    if (data.status === 'offline') {
      serviceState.value = 'offline';
      saveCurrentAnalysisRecord();
      ElMessage.warning(pageText.value.offlineTitle);
      return;
    }

    if (data.status === 'error') {
      serviceState.value = 'error';
      saveCurrentAnalysisRecord();
      ElMessage.error(serviceMessage.value || pageText.value.errorTitle);
      return;
    }

    serviceState.value = 'success';
    saveCurrentAnalysisRecord();
    ElMessage.success(pageText.value.successTitle);
  } catch (error) {
    elapsedMs.value = Math.max(1, Math.round(performance.now() - startedAt));
    serviceState.value = 'error';
    lastResponse.value = null;
    if (axios.isAxiosError(error)) {
      if (error.response?.status === 403) {
        serviceMessage.value = pageText.value.requestForbidden;
      } else if (!error.response) {
        serviceMessage.value = pageText.value.backendUnavailable;
      } else {
        serviceMessage.value = pageText.value.bsarecRequestFailed;
      }
    } else {
      serviceMessage.value = pageText.value.bsarecRequestFailed;
    }
    ElMessage.error(serviceMessage.value);
  } finally {
    inferring.value = false;
  }
}

onMounted(loadModels);
</script>

<style scoped>
.prediction-page {
  padding: 24px;
  max-width: 1460px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-start;
  margin-bottom: 20px;
}

.page-kicker {
  display: block;
  color: var(--primary-color);
  font-size: 11px;
  font-weight: var(--font-weight-title);
  letter-spacing: 0.08em;
  text-transform: uppercase;
  margin-bottom: 7px;
}

.page-header h2 {
  font-size: 22px;
  font-weight: var(--font-weight-body);
  color: var(--text-primary);
  margin: 0 0 4px;
}

.page-header p {
  max-width: 720px;
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.7;
  margin: 0;
}

.service-chip {
  min-width: 150px;
  min-height: 42px;
  padding: 9px 12px;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  background: var(--panel-bg);
  color: var(--text-secondary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 13px;
}

.service-chip.success {
  color: #22c55e;
  border-color: rgba(34, 197, 94, 0.3);
  background: rgba(34, 197, 94, 0.08);
}

.service-chip.offline,
.service-chip.error {
  color: #f59e0b;
  border-color: rgba(245, 158, 11, 0.3);
  background: rgba(245, 158, 11, 0.08);
}

.inference-layout {
  display: grid;
  grid-template-columns: minmax(320px, 430px) minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}

.config-panel,
.result-panel,
.evidence-card {
  border: 1px solid var(--border-color);
  border-radius: 14px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.07), rgba(255, 255, 255, 0.015)),
    color-mix(in srgb, var(--surface-1) 82%, transparent);
  box-shadow: var(--shadow-soft);
}

.config-panel,
.result-panel,
.evidence-card {
  padding: 18px;
}

.panel-heading {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 16px;
}

.panel-heading.compact {
  display: grid;
  gap: 4px;
}

.panel-heading span {
  color: var(--text-secondary);
  font-size: 12px;
}

.panel-heading strong {
  color: var(--text-primary);
  font-size: 16px;
  line-height: 1.35;
}

.model-summary {
  margin-bottom: 16px;
  padding: 14px;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  background: var(--panel-bg);
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.model-summary svg {
  color: var(--primary-color);
  flex-shrink: 0;
}

.model-summary strong {
  color: var(--text-primary);
  font-size: 15px;
}

.model-summary p {
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.6;
  margin: 5px 0 0;
}

.option-meta {
  float: right;
  color: var(--text-muted);
  font-size: 11px;
}

.sample-row {
  margin: -4px 0 14px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.sample-row span {
  color: var(--text-secondary);
  font-size: 12px;
}

.sample-row button {
  border: 1px solid var(--border-color);
  border-radius: 999px;
  background: var(--panel-bg);
  color: var(--text-secondary);
  padding: 5px 10px;
  font-size: 12px;
  cursor: pointer;
}

.sample-row button:hover {
  color: var(--primary-color);
  border-color: rgba(var(--primary-rgb), 0.35);
}

.run-button {
  width: 100%;
  min-height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
}

.status-banner {
  min-height: 92px;
  padding: 15px;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  background: var(--panel-bg);
  display: grid;
  grid-template-columns: 32px minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
}

.status-banner svg {
  color: var(--primary-color);
}

.status-banner.success svg {
  color: #22c55e;
}

.status-banner.offline svg,
.status-banner.error svg {
  color: #f59e0b;
}

.status-banner strong {
  color: var(--text-primary);
  font-size: 16px;
}

.status-banner p {
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.6;
  margin: 4px 0 0;
}

.status-banner > span {
  max-width: 260px;
  color: var(--text-muted);
  font-size: 12px;
  word-break: break-all;
  text-align: right;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-top: 14px;
}

.summary-grid article {
  min-height: 88px;
  padding: 14px;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  background: color-mix(in srgb, var(--surface-2) 72%, transparent);
  display: flex;
  align-items: center;
  gap: 10px;
}

.summary-grid svg {
  color: var(--primary-color);
  flex-shrink: 0;
}

.summary-grid span {
  color: var(--text-secondary);
  font-size: 12px;
}

.summary-grid strong {
  display: block;
  color: var(--text-primary);
  font-size: 20px;
  line-height: 1.2;
  margin-top: 5px;
}

.analysis-metrics-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-top: 12px;
}

.analysis-metrics-grid article {
  min-width: 0;
  min-height: 76px;
  padding: 12px;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  background: var(--panel-bg);
  display: flex;
  align-items: center;
  gap: 10px;
}

.analysis-metrics-grid svg {
  color: var(--primary-color);
  flex-shrink: 0;
}

.analysis-metrics-grid span {
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.35;
}

.analysis-metrics-grid strong {
  display: block;
  color: var(--text-primary);
  font-size: 18px;
  line-height: 1.2;
  margin-top: 5px;
  word-break: break-word;
}

.analysis-action {
  margin-top: 12px;
  padding: 13px 14px;
  border: 1px solid rgba(var(--primary-rgb), 0.22);
  border-radius: 12px;
  background: rgba(var(--primary-rgb), 0.08);
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 14px;
  align-items: center;
}

.analysis-action strong {
  color: var(--text-primary);
  font-size: 14px;
}

.analysis-action p {
  margin: 4px 0 0;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.55;
}

.analysis-action :deep(.el-button) {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 34px;
  white-space: nowrap;
}

.job-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 12px;
  margin-top: 14px;
}

.job-card {
  display: flex;
  gap: 12px;
  min-width: 0;
  padding: 14px;
  border-radius: 12px;
  border: 1px solid var(--border-color);
  background: var(--panel-bg);
}

.job-rank {
  flex: 0 0 auto;
  width: 42px;
  height: 42px;
  border-radius: 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: rgba(var(--primary-rgb), 0.12);
  color: var(--primary-color);
  font-weight: 800;
}

.job-body {
  min-width: 0;
}

.job-body h3 {
  margin: 0 0 6px;
  color: var(--text-primary);
  font-size: 15px;
  line-height: 1.35;
  font-weight: 800;
}

.job-body p {
  margin: 0 0 10px;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.4;
}

.job-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.job-meta span {
  padding: 4px 8px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--surface-2) 74%, transparent);
  color: var(--text-muted);
  font-size: 11px;
}

.diagnostic-empty {
  min-height: 250px;
  margin-top: 14px;
  border: 1px dashed var(--border-color);
  border-radius: 12px;
  background: var(--panel-bg);
  color: var(--text-secondary);
  display: grid;
  place-items: center;
  text-align: center;
  padding: 28px;
}

.diagnostic-empty svg {
  color: var(--primary-color);
}

.diagnostic-empty.offline svg,
.diagnostic-empty.error svg {
  color: #f59e0b;
}

.diagnostic-empty strong {
  color: var(--text-primary);
}

.diagnostic-empty p {
  max-width: 520px;
  margin: 0;
  line-height: 1.7;
}

.diagnostic-empty small {
  max-width: 640px;
  color: var(--text-muted);
  word-break: break-word;
}

.evidence-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(260px, 0.8fr) minmax(0, 1fr);
  gap: 14px;
  margin-top: 18px;
}

.evidence-card pre {
  min-height: 188px;
  max-height: 280px;
  margin: 0;
  padding: 14px;
  overflow: auto;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  background: color-mix(in srgb, var(--surface-2) 76%, transparent);
  color: var(--text-primary);
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.payload-summary,
.response-digest {
  display: grid;
  gap: 8px;
  margin-bottom: 10px;
}

.payload-summary {
  grid-template-columns: minmax(0, 1.6fr) minmax(82px, 0.7fr) minmax(92px, 0.8fr);
}

.response-digest {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.payload-summary div,
.response-digest div {
  min-width: 0;
  min-height: 58px;
  padding: 10px 11px;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  background: var(--panel-bg);
}

.payload-summary span,
.response-digest span {
  display: block;
  color: var(--text-secondary);
  font-size: 11px;
  line-height: 1.35;
  margin-bottom: 5px;
}

.payload-summary strong,
.response-digest strong {
  display: block;
  color: var(--text-primary);
  font-size: 13px;
  line-height: 1.4;
  word-break: break-word;
}

.route-list {
  display: grid;
  gap: 10px;
}

.route-list div {
  min-height: 54px;
  padding: 10px 12px;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  background: var(--panel-bg);
}

.route-list span {
  display: block;
  color: var(--text-secondary);
  font-size: 12px;
  margin-bottom: 5px;
}

.route-list strong {
  color: var(--text-primary);
  font-size: 13px;
  line-height: 1.45;
  word-break: break-word;
}

@media (max-width: 1180px) {
  .inference-layout,
  .evidence-grid {
    grid-template-columns: 1fr;
  }

  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .analysis-metrics-grid,
  .payload-summary,
  .response-digest {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .prediction-page {
    padding: 16px;
  }

  .page-header,
  .status-banner {
    grid-template-columns: 1fr;
    display: grid;
    justify-items: start;
  }

  .service-chip {
    justify-content: flex-start;
  }

  .status-banner > span {
    max-width: 100%;
    text-align: left;
  }

  .summary-grid,
  .analysis-metrics-grid,
  .analysis-action,
  .payload-summary,
  .response-digest,
  .job-grid {
    grid-template-columns: 1fr;
  }
}
</style>
