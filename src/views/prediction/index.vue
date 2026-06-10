<template>
  <main class="prediction-page">
    <section class="access-hero">
      <div class="hero-copy">
        <span>{{ copy.heroEyebrow }}</span>
        <h1>{{ copy.title }}</h1>
        <p>{{ copy.subtitle }}</p>
      </div>
      <div class="hero-actions">
        <button type="button" :disabled="catalogLoading" @click="loadModels">
          <RefreshCw :size="16" />
          {{ catalogLoading ? copy.refreshing : copy.refreshCatalog }}
        </button>
        <button type="button" :disabled="healthLoading" @click="refreshRuntimeHealth">
          <Activity :size="16" />
          {{ healthLoading ? copy.checking : copy.checkBsarec }}
        </button>
      </div>
    </section>

    <section class="summary-grid">
      <article v-for="card in summaryCards" :key="card.label">
        <span>{{ card.label }}</span>
        <strong>{{ card.value }}</strong>
        <em>{{ card.hint }}</em>
      </article>
    </section>

    <section class="access-layout">
      <aside class="model-rail">
        <header>
          <div>
            <span>{{ copy.recommenderModels }}</span>
            <strong>{{ modelCatalog.length }} {{ copy.modelsUnit }}</strong>
          </div>
          <b>{{ callableModels }} {{ copy.callableUnit }}</b>
        </header>

        <button
          v-for="model in modelCatalog"
          :key="modelKey(model)"
          type="button"
          class="model-button"
          :class="[statusTone(model), { active: selectedKey === modelKey(model) }]"
          @click="selectModel(model)"
        >
          <i aria-hidden="true"></i>
          <span>
            <strong>{{ modelTitle(model) }}</strong>
            <em>{{ model.architecture || model.framework || copy.recommendationFallback }}</em>
          </span>
          <b>{{ localizedStatusText(model) }}</b>
        </button>
      </aside>

      <section v-if="selectedModel" class="model-console">
        <header class="console-head">
          <div>
            <span>{{ selectedModel.framework || selectedModel.architecture }}</span>
            <h2>{{ modelTitle(selectedModel) }}</h2>
            <p>{{ localizedDescription(selectedModel) }}</p>
          </div>
          <div class="status-panel" :class="statusTone(selectedModel)">
            <strong>{{ localizedStatusText(selectedModel) }}</strong>
            <em>{{ selectedModel.canExecute ? copy.backendRegistered : copy.noBackendRegistered }}</em>
          </div>
        </header>

        <nav class="panel-tabs" :aria-label="copy.modelAccessView">
          <button
            v-for="tab in panelTabs"
            :key="tab.key"
            type="button"
            :class="{ active: activePanel === tab.key }"
            @click="activePanel = tab.key"
          >
            <component :is="tab.icon" :size="15" />
            <span>{{ tab.label }}</span>
          </button>
        </nav>

        <p v-if="notice" class="notice">{{ notice }}</p>

        <section v-if="activePanel === 'request'" class="request-panel">
          <article class="request-form">
            <header>
              <div>
                <span>{{ copy.recommendationRunner }}</span>
                <strong>{{ selectedModel.canExecute ? copy.jobRecommendationFlow : copy.noEndpoint }}</strong>
              </div>
              <b>{{ selectedModel.canExecute ? selectedModel.endpoint : (selectedModel.serviceUrl || copy.noService) }}</b>
            </header>

            <div class="form-grid">
              <label>
                <span>{{ copy.userId }}</span>
                <input v-model="userId" type="text" />
              </label>
              <label>
                <span>Top-K</span>
                <input v-model.number="topK" type="number" min="1" max="20" />
              </label>
              <label>
                <span>{{ copy.scoreThreshold }}</span>
                <input v-model.number="scoreThreshold" type="number" min="0" max="1" step="0.01" />
              </label>
              <label class="check-row">
                <input v-model="includeJobInfo" type="checkbox" />
                <span>{{ copy.includeJobInfo }}</span>
              </label>
              <label class="wide">
                <span>{{ copy.historyItems }}</span>
                <textarea v-model="historyInput" rows="4"></textarea>
                <em>{{ copy.sampleHint }}</em>
              </label>
            </div>

            <div class="sample-strip">
              <button
                v-for="sample in realJobSamples"
                :key="sample.label"
                type="button"
                @click="applySample(sample)"
              >
                <span>{{ sampleLabel(sample) }}</span>
                <em>{{ sample.items.length }} {{ copy.historyItemUnit }}</em>
              </button>
            </div>

            <div class="action-row">
              <button type="button" class="primary-action" :disabled="running" @click="handlePrimaryAction">
                <Play v-if="selectedModel.canExecute" :size="16" />
                <AlertCircle v-else :size="16" />
                {{ primaryActionText }}
              </button>
              <button type="button" @click="activePanel = 'assets'">
                <Database :size="16" />
                {{ copy.viewModelData }}
              </button>
            </div>
          </article>

          <article class="integration-card">
            <header>
              <div>
                <span>{{ copy.accessStatus }}</span>
                <strong>{{ localizedStatusText(selectedModel) }}</strong>
              </div>
              <PlugZap :size="22" />
            </header>
            <p>{{ selectedModel.statusReason || copy.noAccessReason }}</p>
            <dl>
              <div>
                <dt>{{ copy.dataFile }}</dt>
                <dd>{{ selectedModel.datasetExists ? copy.exists : copy.missing }}</dd>
              </div>
              <div>
                <dt>{{ copy.artifactFile }}</dt>
                <dd>{{ selectedModel.artifactExists ? copy.exists : copy.missing }}</dd>
              </div>
              <div>
                <dt>{{ copy.evalLog }}</dt>
                <dd>{{ localizedMetricSourceLabel(selectedModel) }}</dd>
              </div>
              <div>
                <dt>{{ copy.serviceLatency }}</dt>
                <dd>{{ displayValue(selectedModel.latencyMs, copy.noLatency) }} ms</dd>
              </div>
            </dl>
          </article>

          <article class="result-panel">
            <header>
              <div>
                <span>{{ copy.recommendationResult }}</span>
                <strong>{{ lastResult ? resultStatusText(lastResult.status) : copy.notCalled }}</strong>
              </div>
              <b>{{ lastResult ? lastResult.elapsedMs + ' ms' : copy.waitingRequest }}</b>
            </header>

            <div v-if="lastResult" class="result-body">
              <div class="result-chart" ref="recommendationChartRef"></div>
              <div class="ranking-list">
                <div v-if="!lastResult.items.length" class="empty-line">
                  {{ lastResult.message || copy.noItems }}
                </div>
                <div v-for="item in lastResult.items" :key="item.rank + '-' + item.itemId" class="rank-row">
                  <span>#{{ String(item.rank).padStart(2, '0') }}</span>
                  <strong>{{ item.position || ('Item ' + item.itemId) }}</strong>
                  <em>{{ item.company || item.salary || copy.noJobDetail }}</em>
                  <b>{{ formatScore(item.score) }}</b>
                </div>
              </div>
            </div>
            <div v-else class="empty-result">
              {{ copy.emptyRequest }}
            </div>
          </article>
        </section>

        <section v-else-if="activePanel === 'assets'" class="asset-panel">
          <div class="metric-strip">
            <article v-for="metric in selectedMetrics" :key="metric.label">
              <span>{{ metric.label }}</span>
              <strong>{{ metric.value }}</strong>
            </article>
          </div>

          <div class="asset-workbench">
            <article class="asset-table">
              <header>
                <span>{{ copy.dedicatedAssets }}</span>
                <strong>{{ selectedAssetRows.length }} {{ copy.groupsUnit }}</strong>
              </header>
              <div class="table-shell">
                <table>
                  <thead>
                    <tr>
                      <th>{{ copy.assetName }}</th>
                      <th>{{ copy.format }}</th>
                      <th>{{ copy.users }}</th>
                      <th>{{ copy.items }}</th>
                      <th>{{ copy.interactions }}</th>
                      <th>{{ copy.status }}</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="asset in selectedAssetRows" :key="asset.name + asset.path">
                      <td>
                        <strong>{{ asset.name }}</strong>
                        <em>{{ asset.path }}</em>
                      </td>
                      <td>{{ asset.format }}</td>
                      <td>{{ asset.users }}</td>
                      <td>{{ asset.items }}</td>
                      <td>{{ asset.interactions }}</td>
                      <td>{{ asset.status }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </article>

            <aside class="summary-panel">
              <section>
                <span>{{ copy.trainingAccess }}</span>
                <dl>
                  <div v-for="row in trainingRows" :key="row.key">
                    <dt>{{ row.key }}</dt>
                    <dd>{{ row.value }}</dd>
                  </div>
                </dl>
              </section>
              <section>
                <span>{{ copy.parameterSummary }}</span>
                <dl>
                  <div v-for="row in parameterRows" :key="row.key">
                    <dt>{{ row.key }}</dt>
                    <dd>{{ row.value }}</dd>
                  </div>
                </dl>
              </section>
            </aside>
          </div>
        </section>

        <section v-else-if="activePanel === 'compare'" class="compare-panel">
          <article class="chart-card">
            <header>
              <div>
                <span>{{ copy.comparisonTitle }}</span>
                <strong>{{ copy.comparisonDesc }}</strong>
              </div>
              <b>{{ copy.realScan }}</b>
            </header>
            <div ref="comparisonChartRef" class="comparison-chart"></div>
          </article>

          <div class="comparison-list">
            <article
              v-for="row in comparisonRows"
              :key="row.key"
              :class="{ active: row.key === selectedKey }"
              @click="selectByKey(row.key)"
            >
              <header>
                <div>
                  <strong>{{ row.title }}</strong>
                  <span>{{ row.status }} / {{ row.dataset }}</span>
                </div>
                <b>{{ row.summary }}</b>
              </header>
              <div class="bar-list">
                <div v-for="metric in row.metrics" :key="metric.label">
                  <span>{{ metric.label }}</span>
                  <i><b :style="{ width: metric.percent + '%' }"></b></i>
                  <em>{{ metric.display }}</em>
                </div>
              </div>
            </article>
          </div>
        </section>

        <section v-else class="records-panel">
          <header>
            <div>
              <span>{{ copy.pageRecords }}</span>
              <strong>{{ runRecords.length }} {{ copy.timesUnit }}</strong>
            </div>
            <button type="button" :disabled="!runRecords.length" @click="runRecords = []">{{ copy.clear }}</button>
          </header>

          <div class="record-list">
            <article v-for="record in runRecords" :key="record.id">
              <span>{{ record.createdAt }}</span>
              <strong>{{ record.modelName }} / {{ resultStatusText(record.status) }}</strong>
              <p>{{ record.message || copy.recordMessageFallback }}</p>
              <div>
                <b>{{ copy.historyItems }} {{ record.historyLength }}</b>
                <b>Top-K {{ record.requestedTopK }}</b>
                <b>{{ copy.returned }} {{ record.count }}</b>
                <b>{{ record.elapsedMs }} ms</b>
              </div>
            </article>
            <div v-if="!runRecords.length" class="empty-result">
              {{ copy.noRecords }}
            </div>
          </div>
        </section>
      </section>

      <section v-else class="empty-console">
        {{ copy.noCatalog }}
      </section>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import * as echarts from 'echarts'
import {
  Activity,
  AlertCircle,
  BarChart3,
  ClipboardList,
  Database,
  Play,
  PlugZap,
  RefreshCw,
} from 'lucide-vue-next'
import { predictionApi, trainingApi } from '@/api'
import {
  savePredictionAnalysisRecord,
  selectPredictionAnalysisRecord,
} from '@/utils/predictionAnalysis'
import { buildChartTheme, chartAxis, chartGrid, chartLegend, chartTooltip } from '@/utils/chartTheme'
import type { ModelOption } from '@/types/models'
import {
  assetValue as catalogAssetValue,
  dataAssets,
  datasetMetricNumber,
  displayValue,
  metricSourceLabel,
  modelKey,
  modelMetricNumber,
  modelTitle,
  primaryDatasetLabel,
  readinessNumber,
  statusText,
  statusTone,
} from '@/utils/recommenderCatalog'

type PanelKey = 'request' | 'assets' | 'compare' | 'records'

type RealJobSample = {
  label: string
  userId: string
  source: string
  items: number[]
}

type RecommendationItem = {
  rank: number
  itemId: string
  id: string | number
  position?: string
  company?: string
  salary?: string
  score?: number | null
}

type RecommendationResult = {
  id: string
  modelName: string
  status: string
  message: string
  serviceUrl: string
  elapsedMs: number
  requestedTopK: number
  historyLength: number
  count: number
  createdAt: string
  items: RecommendationItem[]
}

const { locale } = useI18n()
const isZh = computed(() => !locale.value.startsWith('en'))

const copy = computed(() => isZh.value
  ? {
      title: '岗位推荐测试',
      heroEyebrow: '推荐模型验证',
      subtitle: '用 BSARec Job 的真实岗位序列生成 Top-K 推荐，并查看数据、权重、日志和服务状态。其他模型展示已接入的数据与缺失的推理服务。',
      recommendationFallback: '推荐系统',
      refreshing: '刷新中',
      refreshCatalog: '刷新模型目录',
      checking: '检查中',
      checkBsarec: '检查 BSARec 服务',
      recommenderModels: '推荐模型',
      modelsUnit: '个',
      callableUnit: '个可运行推荐服务',
      backendRegistered: '后端推荐服务已登记',
      noBackendRegistered: '未登记后端推理接口',
      tabRequest: '推荐测试',
      tabAssets: '数据展示',
      tabCompare: '模型比对',
      tabRecords: '测试记录',
      modelAccessView: '模型接入测试视图',
      realEndpoint: '推荐服务',
      recommendationRunner: '岗位推荐',
      jobRecommendationFlow: '根据历史岗位生成 Top-K 推荐',
      noEndpoint: '未注册接口',
      noService: '无运行服务',
      userId: '用户 ID',
      scoreThreshold: '分数阈值',
      includeJobInfo: '返回岗位信息',
      historyItems: '用户历史 item ID',
      sampleHint: '当前样例来自 `model/BSARec-main-api/src/data/Job.txt` 序列。',
      historyItemUnit: '个历史 item',
      viewModelData: '查看模型数据',
      accessStatus: '接入状态',
      noAccessReason: '后端未返回接入说明。',
      dataFile: '数据文件',
      artifactFile: '权重文件',
      evalLog: '评估日志',
      serviceLatency: '服务延迟',
      exists: '存在',
      missing: '缺失',
      noLatency: '未返回',
      apiResponse: '推荐结果',
      recommendationResult: '推荐结果',
      notCalled: '尚未运行',
      waitingRequest: '等待推荐测试',
      noItems: '没有返回推荐岗位。',
      noJobDetail: '无岗位详情',
      emptyRequest: '选择 BSARec Job 后点击“生成岗位推荐”。如果 Flask 服务没有运行，这里会显示服务离线原因。',
      dedicatedAssets: '专属数据资产',
      groupsUnit: '组',
      assetName: '名称',
      format: '格式',
      users: '用户数',
      items: '物品数',
      interactions: '交互数',
      status: '状态',
      trainingAccess: '训练与接入',
      parameterSummary: '参数摘要',
      comparisonTitle: '横向比对',
      comparisonDesc: '数据、权重、日志、代码、服务的扫描证据',
      realScan: '目录扫描',
      pageRecords: '本页测试记录',
      timesUnit: '次',
      clear: '清空',
      returned: '返回',
      recordMessageFallback: '推荐测试已完成，结果已写入图表工作台记录。',
      noRecords: '暂无测试记录。这里不生成模拟结果，只记录真实后端请求。',
      noCatalog: '后端暂未返回模型目录。请确认后端服务可访问后点击刷新。',
      summaryModel: '模型家族',
      summaryModelHint: '全部来自本地 model 目录',
      summaryData: '数据资产',
      summaryDataHint: '已扫描到数据文件',
      summaryLog: '评估日志',
      summaryLogHint: '可用评估指标',
      summaryEndpoint: '推荐服务',
      summaryEndpointHint: '当前只有 BSARec Job 可运行',
      dataset: '数据集',
      hrMissing: '暂无',
      unnamedDataset: '未命名数据集',
      unmarked: '未标注',
      noPath: '未登记路径',
      trueCalling: '正在生成推荐',
      callReal: '生成岗位推荐',
      viewGap: '查看接入缺口',
      sampleLoadedPrefix: '已载入样例：',
      noInferenceRegistered: '该模型未在后端登记推理接口。',
      catalogLoadedPrefix: '已刷新',
      catalogLoadedSuffix: '个推荐模型。',
      catalogFailed: '模型目录读取失败。',
      healthOnlineReason: 'BSARec Flask /health 已响应，后端代理可以继续调用 /recommend。',
      healthOfflineReason: 'BSARec Flask 服务当前不可达。',
      healthOnline: 'BSARec 服务在线。',
      healthOffline: 'BSARec 服务离线。',
      healthFailed: 'BSARec 健康检查失败。',
      noBackendRecommendation: '该模型未登记后端推荐服务。',
      historyRequired: '请输入至少一个历史 item ID。',
      callingBackend: '正在根据岗位历史生成推荐。',
      successNotice: '已生成 {count}/{topK} 条岗位推荐，并写入图表工作台。',
      nonSuccess: '推荐服务返回非成功状态，详情见推荐结果区域。',
      recommendFailed: '岗位推荐生成失败。',
      noScore: '未返回分数',
      detected: '已发现',
      notDetected: '缺失',
      registered: '已登记',
      notRegistered: '未登记',
      evidenceUnit: '项',
      statusLabels: { success: '成功', offline: '服务离线', error: '错误', unknown: '未知' } as Record<string, string>,
      statusMap: {
        在线: '在线',
        服务离线: '服务离线',
        '权重+日志': '权重+日志',
        仅有权重: '仅有权重',
        '代码+数据': '代码+数据',
        仅有数据: '仅有数据',
        缺少数据: '缺少数据',
        未注册推理: '未注册推理',
      } as Record<string, string>,
      readiness: {
        data: '数据',
        artifacts: '权重',
        metrics: '指标',
        code: '代码',
        service: '服务',
      },
    }
  : {
      title: 'Job Recommendation Test',
      heroEyebrow: 'Recommendation Validation',
      subtitle: 'Generate Top-K job recommendations from real BSARec Job sequences, while keeping data, artifacts, logs, and service status visible. Other models show available assets and missing inference services.',
      recommendationFallback: 'Recommendation',
      refreshing: 'Refreshing',
      refreshCatalog: 'Refresh model catalog',
      checking: 'Checking',
      checkBsarec: 'Check BSARec service',
      recommenderModels: 'Recommender models',
      modelsUnit: 'models',
      callableUnit: 'runnable services',
      backendRegistered: 'Backend recommender service registered',
      noBackendRegistered: 'No backend inference endpoint registered',
      tabRequest: 'Recommendation test',
      tabAssets: 'Data display',
      tabCompare: 'Model comparison',
      tabRecords: 'Test records',
      modelAccessView: 'Model access test view',
      realEndpoint: 'Recommender service',
      recommendationRunner: 'Job recommendation',
      jobRecommendationFlow: 'Generate Top-K jobs from user history',
      noEndpoint: 'No endpoint registered',
      noService: 'No running service',
      userId: 'User ID',
      scoreThreshold: 'Score threshold',
      includeJobInfo: 'Return job info',
      historyItems: 'User history item IDs',
      sampleHint: 'The sample comes from the `model/BSARec-main-api/src/data/Job.txt` sequence.',
      historyItemUnit: 'history items',
      viewModelData: 'View model data',
      accessStatus: 'Access status',
      noAccessReason: 'The backend did not return an access note.',
      dataFile: 'Data file',
      artifactFile: 'Artifact file',
      evalLog: 'Evaluation log',
      serviceLatency: 'Service latency',
      exists: 'Exists',
      missing: 'Missing',
      noLatency: 'No value',
      apiResponse: 'Recommendation result',
      recommendationResult: 'Recommendation result',
      notCalled: 'Not run yet',
      waitingRequest: 'Waiting for test',
      noItems: 'No recommended jobs were returned.',
      noJobDetail: 'No job detail',
      emptyRequest: 'Select BSARec Job and click "Generate job recommendations". If the Flask service is not running, the service-offline reason is shown here.',
      dedicatedAssets: 'Dedicated data assets',
      groupsUnit: 'groups',
      assetName: 'Name',
      format: 'Format',
      users: 'Users',
      items: 'Items',
      interactions: 'Interactions',
      status: 'Status',
      trainingAccess: 'Training and access',
      parameterSummary: 'Parameter summary',
      comparisonTitle: 'Cross-model comparison',
      comparisonDesc: 'Scanned evidence for data, artifacts, logs, code, and service',
      realScan: 'Directory scan',
      pageRecords: 'Page test records',
      timesUnit: 'runs',
      clear: 'Clear',
      returned: 'Returned',
      recordMessageFallback: 'The recommendation test completed and was written to the advanced analysis workspace.',
      noRecords: 'No test records. This page does not generate simulated results; it records real backend requests.',
      noCatalog: 'The backend has not returned a model catalog. Confirm the backend is reachable, then refresh.',
      summaryModel: 'Model families',
      summaryModelHint: 'All from the local model directory',
      summaryData: 'Data assets',
      summaryDataHint: 'Data files scanned',
      summaryLog: 'Evaluation logs',
      summaryLogHint: 'Available evaluation metrics',
      summaryEndpoint: 'Recommendation services',
      summaryEndpointHint: 'Only BSARec Job is runnable currently',
      dataset: 'Dataset',
      hrMissing: 'None',
      unnamedDataset: 'Unnamed dataset',
      unmarked: 'Unmarked',
      noPath: 'No path registered',
      trueCalling: 'Generating recommendations',
      callReal: 'Generate job recommendations',
      viewGap: 'View access gap',
      sampleLoadedPrefix: 'Loaded sample: ',
      noInferenceRegistered: 'This model is not registered for backend inference.',
      catalogLoadedPrefix: 'Refreshed',
      catalogLoadedSuffix: 'recommender models.',
      catalogFailed: 'Failed to read model catalog.',
      healthOnlineReason: 'BSARec Flask /health responded; the backend proxy can continue to call /recommend.',
      healthOfflineReason: 'BSARec Flask service is currently unreachable.',
      healthOnline: 'BSARec service is online.',
      healthOffline: 'BSARec service is offline.',
      healthFailed: 'BSARec health check failed.',
      noBackendRecommendation: 'This model has no registered backend recommender service.',
      historyRequired: 'Enter at least one historical item ID.',
      callingBackend: 'Generating recommendations from job history.',
      successNotice: 'Generated {count}/{topK} job recommendations and wrote them to the advanced analysis workspace.',
      nonSuccess: 'The recommender service returned a non-success status. See the recommendation result panel for details.',
      recommendFailed: 'Job recommendation generation failed.',
      noScore: 'No score returned',
      detected: 'Found',
      notDetected: 'Missing',
      registered: 'Registered',
      notRegistered: 'Not registered',
      evidenceUnit: 'items',
      statusLabels: { success: 'Success', offline: 'Service offline', error: 'Error', unknown: 'Unknown' } as Record<string, string>,
      statusMap: {
        在线: 'Online',
        服务离线: 'Service offline',
        '权重+日志': 'Artifacts + logs',
        仅有权重: 'Artifacts only',
        '代码+数据': 'Code + data',
        仅有数据: 'Data only',
        缺少数据: 'Missing data',
        未注册推理: 'No inference registered',
      } as Record<string, string>,
      readiness: {
        data: 'Data',
        artifacts: 'Artifacts',
        metrics: 'Metrics',
        code: 'Code',
        service: 'Service',
      },
    })

const realJobSamples: RealJobSample[] = [
  {
    label: 'Job 用户 0',
    userId: '0',
    source: 'Job.txt 第 1 行',
    items: [835, 705, 531, 661, 758, 613, 762, 735, 706, 860, 745, 756, 931, 855, 280, 629, 782, 627, 773, 746],
  },
  {
    label: 'Job 用户 2',
    userId: '2',
    source: 'Job.txt 第 3 行',
    items: [33, 115, 234, 17, 1121, 37, 160, 80, 87, 1049, 813, 81, 74, 1071, 261, 240],
  },
  {
    label: 'Job 用户 4',
    userId: '4',
    source: 'Job.txt 第 5 行',
    items: [731, 772, 730, 748, 639, 636, 630, 199, 768, 674, 822, 723, 834, 736, 811, 645],
  },
]

const panelTabs = computed(() => [
  { key: 'request' as const, label: copy.value.tabRequest, icon: PlugZap },
  { key: 'assets' as const, label: copy.value.tabAssets, icon: Database },
  { key: 'compare' as const, label: copy.value.tabCompare, icon: BarChart3 },
  { key: 'records' as const, label: copy.value.tabRecords, icon: ClipboardList },
])

const readinessFields = computed(() => [
  { label: copy.value.readiness.data, keys: ['数据就绪度', 'Dataset readiness'] },
  { label: copy.value.readiness.artifacts, keys: ['权重就绪度', 'Artifact readiness'] },
  { label: copy.value.readiness.metrics, keys: ['指标可信度', 'Metric provenance'] },
  { label: copy.value.readiness.code, keys: ['代码就绪度', 'Code readiness'] },
  { label: copy.value.readiness.service, keys: ['服务就绪度', 'Service readiness'] },
])

const modelCatalog = ref<ModelOption[]>([])
const selectedKey = ref('')
const activePanel = ref<PanelKey>('request')
const catalogLoading = ref(false)
const healthLoading = ref(false)
const running = ref(false)
const notice = ref('')
const userId = ref(realJobSamples[0].userId)
const historyInput = ref(realJobSamples[0].items.join(', '))
const topK = ref(10)
const scoreThreshold = ref(0)
const includeJobInfo = ref(true)
const lastResult = ref<RecommendationResult | null>(null)
const runRecords = ref<RecommendationResult[]>([])
const comparisonChartRef = ref<HTMLElement | null>(null)
const recommendationChartRef = ref<HTMLElement | null>(null)
const charts = new Map<string, echarts.ECharts>()

const selectedModel = computed(() => modelCatalog.value.find((model) => modelKey(model) === selectedKey.value) || modelCatalog.value[0] || null)
const callableModels = computed(() => modelCatalog.value.filter((model) => model.canExecute).length)
const summaryCards = computed(() => [
  { label: copy.value.summaryModel, value: modelCatalog.value.length, hint: copy.value.summaryModelHint },
  { label: copy.value.summaryData, value: modelCatalog.value.filter((model) => model.datasetExists).length, hint: copy.value.summaryDataHint },
  { label: copy.value.summaryLog, value: modelCatalog.value.filter((model) => readinessValue(model, 2) > 0).length, hint: copy.value.summaryLogHint },
  { label: copy.value.summaryEndpoint, value: callableModels.value, hint: copy.value.summaryEndpointHint },
])

const selectedMetrics = computed(() => {
  const model = selectedModel.value
  if (!model) return []
  return [
    { label: copy.value.dataset, value: primaryDatasetLabel(model) },
    { label: copy.value.users, value: displayValue(model.datasetSummary?.['User scale'], copy.value.hrMissing) },
    { label: copy.value.items, value: displayValue(model.datasetSummary?.['Item scale'], copy.value.hrMissing) },
    { label: copy.value.interactions, value: displayValue(model.datasetSummary?.Interactions, copy.value.hrMissing) },
    { label: isZh.value ? '人均交互' : 'Avg/User', value: displayValue(model.datasetSummary?.['Avg interactions/user'], copy.value.hrMissing) },
    { label: isZh.value ? '稠密度' : 'Density', value: displayValue(model.datasetSummary?.Density, copy.value.hrMissing) },
    { label: 'HR@10', value: metricDecimal(modelMetricNumber(model, ['HR@10', 'HIT@10'])) },
    { label: 'NDCG@10', value: metricDecimal(modelMetricNumber(model, 'NDCG@10')) },
  ]
})

const selectedAssetRows = computed(() => {
  const model = selectedModel.value
  if (!model) return []
  const rows = dataAssets(model).map((asset) => ({
    name: catalogAssetValue(asset, ['Name', '名称', 'name'], copy.value.unnamedDataset),
    path: catalogAssetValue(asset, ['Path', '路径', 'path'], ''),
    format: catalogAssetValue(asset, ['Format', '格式', 'format'], copy.value.unmarked),
    users: catalogAssetValue(asset, ['User scale', '用户数', 'users'], '0'),
    items: catalogAssetValue(asset, ['Item scale', '物品数', 'items'], '0'),
    interactions: catalogAssetValue(asset, ['Interactions', '交互数', 'interactions'], '0'),
    status: catalogAssetValue(asset, ['Status', '状态', 'status'], model.datasetExists ? copy.value.exists : copy.value.missing),
  }))
  if (rows.length) return rows
  return [{
    name: primaryDatasetLabel(model),
    path: displayValue(model.dataPath, copy.value.noPath),
    format: displayValue(model.datasetSummary?.['Data format'], copy.value.unmarked),
    users: displayValue(model.datasetSummary?.['User scale'], '0'),
    items: displayValue(model.datasetSummary?.['Item scale'], '0'),
    interactions: displayValue(model.datasetSummary?.Interactions, '0'),
    status: model.datasetExists ? copy.value.exists : copy.value.missing,
  }]
})

const trainingRows = computed(() => objectRows(selectedModel.value?.trainingSummary))
const parameterRows = computed(() => objectRows(selectedModel.value?.parameterSummary))

const comparisonRows = computed(() => modelCatalog.value.map((model) => {
  const metrics = readinessFields.value.map((field, index) => {
    const numeric = readinessValue(model, index)
    const present = numeric > 0
    return {
      label: field.label,
      numeric: present ? 1 : 0,
      display: readinessDisplay(index, present),
      percent: present ? 100 : 0,
    }
  })
  const score = metrics.reduce((sum, item) => sum + item.numeric, 0)
  return {
    key: modelKey(model),
    title: modelTitle(model),
    status: localizedStatusText(model),
    dataset: primaryDatasetLabel(model),
    score,
    summary: `${score}/${metrics.length} ${copy.value.evidenceUnit}`,
    metrics,
    hr10: modelMetricNumber(model, ['HR@10', 'HIT@10']),
    interactions: datasetMetricNumber(model, 'Interactions'),
  }
}).sort((a, b) => b.score - a.score || b.hr10 - a.hr10 || b.interactions - a.interactions))

const primaryActionText = computed(() => {
  if (running.value) return copy.value.trueCalling
  if (selectedModel.value?.canExecute) return copy.value.callReal
  return copy.value.viewGap
})

watch([comparisonRows, selectedKey], renderComparisonChart, { deep: true })
watch(lastResult, renderRecommendationChart, { deep: true })
watch(activePanel, renderVisibleCharts)

function selectModel(model: ModelOption) {
  selectedKey.value = modelKey(model)
  notice.value = ''
}

function selectByKey(key: string) {
  selectedKey.value = key
  activePanel.value = 'assets'
}

function applySample(sample: RealJobSample) {
  userId.value = sample.userId
  historyInput.value = sample.items.join(', ')
  notice.value = `${copy.value.sampleLoadedPrefix}${sample.source}.`
}

function handlePrimaryAction() {
  if (!selectedModel.value?.canExecute) {
    notice.value = selectedModel.value?.statusReason || copy.value.noInferenceRegistered
    activePanel.value = 'assets'
    return
  }
  void runRecommendation()
}

async function loadModels() {
  catalogLoading.value = true
  try {
    const response = await trainingApi.listModels()
    const data = response.data.data
    modelCatalog.value = [...(data.official || []), ...(data.userModels || [])]
    if (!modelCatalog.value.some((model) => modelKey(model) === selectedKey.value)) {
      selectedKey.value = modelCatalog.value[0] ? modelKey(modelCatalog.value[0]) : ''
    }
    notice.value = `${copy.value.catalogLoadedPrefix} ${modelCatalog.value.length} ${copy.value.catalogLoadedSuffix}`
  } catch (error: any) {
    notice.value = error?.response?.data?.message || error?.message || copy.value.catalogFailed
  } finally {
    catalogLoading.value = false
    renderVisibleCharts()
  }
}

async function refreshRuntimeHealth() {
  healthLoading.value = true
  try {
    const response = await predictionApi.health()
    const health = response.data.data || {}
    const online = Boolean(health.online)
    modelCatalog.value = modelCatalog.value.map((model) => {
      if (!isBsaRecJob(model)) return model
      return {
        ...model,
        runtimeHealth: health,
        online,
        integrationStatus: online ? 'online' : 'service-offline',
        statusLabel: online ? '在线' : '服务离线',
        latencyMs: Number(health.elapsedMs || 0),
        serviceUrl: String(health.serviceUrl || model.serviceUrl || ''),
        statusReason: online
          ? copy.value.healthOnlineReason
          : String(health.message || copy.value.healthOfflineReason),
      }
    })
    notice.value = online ? copy.value.healthOnline : String(health.message || copy.value.healthOffline)
  } catch (error: any) {
    notice.value = error?.response?.data?.message || error?.message || copy.value.healthFailed
  } finally {
    healthLoading.value = false
    renderVisibleCharts()
  }
}

async function runRecommendation() {
  const model = selectedModel.value
  const history = parseHistoryInput()
  if (!model?.canExecute) {
    notice.value = model?.statusReason || copy.value.noBackendRecommendation
    return
  }
  if (!history.length) {
    notice.value = copy.value.historyRequired
    return
  }

  const requestedTopK = Math.max(1, Math.min(Number(topK.value) || 10, 20))
  const threshold = Math.max(0, Math.min(Number(scoreThreshold.value) || 0, 1))
  running.value = true
  notice.value = copy.value.callingBackend
  try {
    const startedAt = performance.now()
    const response = await predictionApi.recommend({
      user_history: history,
      user_id: userId.value.trim() || 'deepinsight-user',
      top_k: requestedTopK,
      include_job_info: includeJobInfo.value,
    })
    const payload = response.data.data || {}
    const elapsedMs = Number(payload.elapsedMs ?? Math.round(performance.now() - startedAt))
    const items = normalizeRecommendationItems(payload.recommendations)
      .filter((item) => item.score === null || item.score === undefined || item.score >= threshold)
      .slice(0, requestedTopK)
    const result: RecommendationResult = {
      id: `run-${Date.now()}`,
      modelName: String(payload.model || model.name),
      status: String(payload.status || 'unknown'),
      message: String(payload.message || ''),
      serviceUrl: String(payload.serviceUrl || model.serviceUrl || ''),
      elapsedMs,
      requestedTopK,
      historyLength: history.length,
      count: items.length,
      createdAt: new Date().toLocaleString(isZh.value ? 'zh-CN' : 'en-US'),
      items,
    }
    lastResult.value = result
    runRecords.value = [result, ...runRecords.value].slice(0, 8)
    const record = savePredictionAnalysisRecord({
      modelName: result.modelName,
      request: {
        user_history: history,
        top_k: requestedTopK,
        include_job_info: includeJobInfo.value,
      },
      response: payload as Record<string, unknown>,
      status: result.status,
      message: result.message,
      serviceUrl: result.serviceUrl,
      elapsedMs,
      recommendations: items.map((item) => ({
        rank: item.rank,
        itemId: item.itemId,
        position: item.position,
        company: item.company,
        salary: item.salary,
        score: item.score ?? undefined,
      })),
    })
    selectPredictionAnalysisRecord(record.id)
    notice.value = result.status === 'success'
      ? copy.value.successNotice.replace('{count}', String(items.length)).replace('{topK}', String(requestedTopK))
      : result.message || copy.value.nonSuccess
  } catch (error: any) {
    lastResult.value = null
    notice.value = error?.response?.data?.message || error?.message || copy.value.recommendFailed
  } finally {
    running.value = false
    renderVisibleCharts()
  }
}

function parseHistoryInput() {
  return historyInput.value
    .split(/[,\s]+/)
    .map((item) => Number.parseInt(item.trim(), 10))
    .filter((item) => Number.isFinite(item))
}

function normalizeRecommendationItems(raw: unknown): RecommendationItem[] {
  if (!Array.isArray(raw)) return []
  return raw.map((entry, index) => {
    if (entry && typeof entry === 'object') {
      const item = entry as Record<string, any>
      const itemId = String(item.itemId ?? item.item_id ?? item.id ?? '')
      return {
        rank: Number(item.rank) || index + 1,
        itemId,
        id: item.id ?? itemId,
        position: stringValue(item.position),
        company: stringValue(item.company),
        salary: stringValue(item.salary),
        score: normalizeScore(item.score),
      }
    }
    return {
      rank: index + 1,
      itemId: String(entry),
      id: String(entry),
      score: null,
    }
  })
}

function normalizeScore(value: unknown) {
  if (value === null || value === undefined || value === '') return null
  const number = Number(value)
  return Number.isFinite(number) ? number : null
}

function resultStatusText(status: string) {
  return copy.value.statusLabels[status] || status || copy.value.statusLabels.unknown
}

function formatScore(value: number | null | undefined) {
  return typeof value === 'number' && Number.isFinite(value) ? value.toFixed(4) : copy.value.noScore
}

function readinessValue(model: ModelOption, index: number) {
  const field = readinessFields.value[index]
  if (!field) return 0
  return readinessNumber(model, field.keys)
}

function readinessDisplay(index: number, present: boolean) {
  const serviceIndex = readinessFields.value.length - 1
  if (index === serviceIndex) {
    return present ? copy.value.registered : copy.value.notRegistered
  }
  return present ? copy.value.detected : copy.value.notDetected
}

function objectRows(source?: Record<string, unknown>) {
  return Object.entries(source || {})
    .filter(([, value]) => value !== null && value !== undefined && String(value).trim())
    .map(([key, value]) => ({ key, value: String(value) }))
}

function metricDecimal(value: number, digits = 4) {
  return Number.isFinite(value) && value > 0 ? value.toFixed(digits) : copy.value.hrMissing
}

function localizedStatusText(model?: ModelOption) {
  const raw = statusText(model)
  return copy.value.statusMap[raw] || raw
}

function localizedDescription(model: ModelOption) {
  if (isZh.value) return model.descriptionZh || model.description || model.statusReason || copy.value.noAccessReason
  return model.description || model.descriptionZh || model.statusReason || copy.value.noAccessReason
}

function localizedMetricSourceLabel(model?: ModelOption) {
  const raw = metricSourceLabel(model)
  if (isZh.value) return raw
  return raw === '当前目录没有评估日志' ? 'No evaluation log in the current directory' : raw
}

function sampleLabel(sample: RealJobSample) {
  return isZh.value ? sample.label : `Job user ${sample.userId}`
}

function stringValue(value: unknown) {
  if (value === null || value === undefined) return undefined
  const text = String(value).trim()
  return text || undefined
}

function isBsaRecJob(model: ModelOption) {
  return modelKey(model) === 'bsarec-job' || model.name === 'BSARec Job'
}

function chartPalette() {
  const theme = buildChartTheme()
  return {
    ...theme,
    secondary: theme.text,
    data: theme.primary,
    artifact: theme.accent,
    metric: theme.warning,
    code: theme.success,
    service: theme.danger,
  }
}

function getChart(key: string, el: HTMLElement | null) {
  if (!el) return null
  const existing = charts.get(key)
  if (existing && !existing.isDisposed()) return existing
  const chart = echarts.init(el)
  charts.set(key, chart)
  return chart
}

function renderVisibleCharts() {
  renderComparisonChart()
  renderRecommendationChart()
}

function renderComparisonChart() {
  void nextTick(() => {
    if (activePanel.value !== 'compare') return
    const chart = getChart('comparison', comparisonChartRef.value)
    if (!chart) return
    const colors = chartPalette()
    const axis = chartAxis(colors)
    const rows = comparisonRows.value
    const labels = readinessFields.value.map((field) => field.label)
    chart.setOption({
      backgroundColor: 'transparent',
      tooltip: { ...chartTooltip(colors), trigger: 'axis', axisPointer: { type: 'shadow' } },
      legend: { top: 0, ...chartLegend(colors) },
      color: [colors.data, colors.artifact, colors.metric, colors.code, colors.service],
      grid: chartGrid({ left: 44, right: 18, top: 42, bottom: 72 }),
      xAxis: {
        type: 'category',
        data: rows.map((row) => row.title.replace(' 推荐', '')),
        ...axis,
        axisLabel: { ...axis.axisLabel, rotate: rows.length > 6 ? 22 : 0 },
      },
      yAxis: {
        type: 'value',
        min: 0,
        max: 1,
        interval: 1,
        ...axis,
        axisLabel: { ...axis.axisLabel, formatter: (value: number) => value > 0 ? copy.value.detected : copy.value.notDetected },
      },
      series: labels.map((label) => ({
        name: label,
        type: 'bar',
        barMaxWidth: 18,
        itemStyle: { borderRadius: [6, 6, 0, 0] },
        data: rows.map((row) => row.metrics.find((metric) => metric.label === label)?.numeric || 0),
      })),
    }, true)
  })
}

function renderRecommendationChart() {
  void nextTick(() => {
    if (activePanel.value !== 'request' || !lastResult.value) return
    const chart = getChart('recommendation', recommendationChartRef.value)
    if (!chart) return
    const colors = chartPalette()
    const axis = chartAxis(colors)
    const items = lastResult.value.items
    chart.setOption({
      backgroundColor: 'transparent',
      tooltip: { ...chartTooltip(colors), trigger: 'axis', axisPointer: { type: 'shadow' } },
      grid: chartGrid({ left: 46, right: 18, top: 24, bottom: 54 }),
      xAxis: {
        type: 'category',
        data: items.map((item) => '#' + item.rank),
        ...axis,
      },
      yAxis: {
        type: 'value',
        ...axis,
      },
      series: [{
        name: 'score',
        type: 'bar',
        barMaxWidth: 24,
        itemStyle: { color: colors.primary, borderRadius: [6, 6, 0, 0] },
        data: items.map((item) => item.score ?? 0),
      }],
    }, true)
  })
}

function resizeCharts() {
  charts.forEach((chart) => chart.resize())
}

onMounted(async () => {
  await loadModels()
  window.addEventListener('resize', resizeCharts, { passive: true })
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  charts.forEach((chart) => chart.dispose())
  charts.clear()
})
</script>

<style scoped>
.prediction-page {
  width: min(1480px, 100%);
  min-height: calc(100dvh - var(--header-height, 72px));
  margin: 0 auto;
  padding: 22px;
  color: var(--text-primary);
  --prediction-glass-bg:
    linear-gradient(180deg, rgba(255, 255, 255, 0.08), rgba(255, 255, 255, 0.018)),
    color-mix(in srgb, rgba(var(--glass-bg-rgb), var(--glass-opacity)) 78%, transparent);
  --prediction-card-bg:
    linear-gradient(180deg, rgba(255, 255, 255, 0.055), transparent),
    color-mix(in srgb, var(--panel-bg) 84%, transparent);
}

.access-hero,
.summary-grid article,
.model-rail,
.model-console,
.empty-console,
.request-form,
.integration-card,
.result-panel,
.asset-table,
.summary-panel,
.chart-card,
.comparison-list article,
.records-panel {
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--prediction-glass-bg);
  box-shadow: var(--shadow-soft);
  backdrop-filter: blur(var(--glass-blur)) saturate(var(--glass-saturate));
  -webkit-backdrop-filter: blur(var(--glass-blur)) saturate(var(--glass-saturate));
  transition:
    border-color var(--motion-hover) var(--ease-liquid),
    background var(--motion-medium) var(--ease-smooth),
    box-shadow var(--motion-medium) var(--ease-smooth),
    transform var(--motion-hover) var(--ease-liquid);
}

.access-hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  padding: 24px;
}

.hero-copy span,
.model-rail header span,
.console-head span,
.request-form header span,
.integration-card header span,
.result-panel header span,
.asset-table header span,
.summary-panel span,
.chart-card header span,
.records-panel header span {
  color: var(--primary-color);
  font-size: 12px;
  font-weight: var(--font-weight-title);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.hero-copy h1 {
  margin: 8px 0;
  font-size: clamp(30px, 4vw, 46px);
  letter-spacing: 0;
}

.hero-copy p {
  max-width: 760px;
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.8;
}

.hero-actions,
.action-row,
.panel-tabs,
.sample-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.hero-actions button,
.action-row button,
.panel-tabs button,
.sample-strip button,
.records-panel header button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 40px;
  padding: 0 14px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--surface-color);
  color: var(--text-primary);
  font-weight: var(--font-weight-title);
  cursor: pointer;
  transition:
    border-color var(--motion-hover) var(--ease-liquid),
    background var(--motion-medium) var(--ease-smooth),
    box-shadow var(--motion-medium) var(--ease-smooth),
    transform var(--motion-hover) var(--ease-liquid);
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.58;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin: 14px 0;
}

.summary-grid article {
  padding: 16px;
}

.summary-grid span,
.summary-grid em,
.model-button em,
.model-button b,
.status-panel em,
.request-form header b,
.result-panel header b,
.asset-table td em,
.comparison-list span,
.record-list p {
  color: var(--text-secondary);
}

.summary-grid strong {
  display: block;
  margin: 8px 0 4px;
  font-size: 28px;
}

.summary-grid em {
  font-style: normal;
  font-size: 13px;
}

.access-layout {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 14px;
}

.model-rail {
  align-self: start;
  padding: 14px;
}

.model-rail header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.model-rail header strong {
  display: block;
  margin-top: 4px;
  font-size: 20px;
}

.model-rail header b {
  color: var(--text-secondary);
  font-size: 12px;
}

.model-button {
  display: grid;
  grid-template-columns: 10px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  width: 100%;
  min-height: 62px;
  margin-top: 8px;
  padding: 10px;
  border: 1px solid transparent;
  border-radius: 8px;
  background: color-mix(in srgb, var(--prediction-card-bg) 72%, transparent);
  color: var(--text-primary);
  text-align: left;
  cursor: pointer;
  transition:
    border-color var(--motion-hover) var(--ease-liquid),
    background var(--motion-medium) var(--ease-smooth),
    box-shadow var(--motion-medium) var(--ease-smooth),
    transform var(--motion-hover) var(--ease-liquid);
}

.model-button:hover,
.model-button.active {
  border-color: rgba(var(--primary-rgb), 0.35);
  background: rgba(var(--primary-rgb), 0.08);
}

.model-button i {
  width: 8px;
  height: 34px;
  border-radius: 999px;
  background: var(--border-color);
}

.model-button.online i,
.status-panel.online {
  border-color: color-mix(in srgb, var(--primary-color) 42%, var(--border-color));
}

.model-button.online i {
  background: var(--primary-color);
}

.model-button.service i,
.status-panel.service {
  border-color: color-mix(in srgb, var(--danger-glow) 42%, var(--border-color));
}

.model-button.service i {
  background: var(--danger-glow);
}

.model-button.artifact i,
.status-panel.artifact {
  border-color: color-mix(in srgb, var(--warning-glow) 42%, var(--border-color));
}

.model-button.artifact i {
  background: var(--warning-glow);
}

.model-button.data i,
.status-panel.data {
  border-color: color-mix(in srgb, var(--accent-glow) 42%, var(--border-color));
}

.model-button.data i {
  background: var(--accent-glow);
}

.model-button span {
  min-width: 0;
}

.model-button strong,
.model-button em {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.model-button b {
  font-size: 12px;
  white-space: nowrap;
}

.model-console,
.empty-console {
  padding: 18px;
}

.console-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
}

.console-head h2 {
  margin: 6px 0;
  font-size: 28px;
}

.console-head p {
  max-width: 760px;
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.7;
}

.status-panel {
  flex: 0 0 190px;
  padding: 14px;
}

.status-panel strong,
.status-panel em {
  display: block;
}

.status-panel strong {
  margin-bottom: 4px;
  font-size: 20px;
}

.panel-tabs {
  margin: 18px 0 12px;
}

.panel-tabs button.active,
.hero-actions button:hover,
.action-row button:hover,
.sample-strip button:hover {
  border-color: rgba(var(--primary-rgb), 0.34);
  background: rgba(var(--primary-rgb), 0.1);
  color: var(--primary-color);
}

.notice {
  margin: 0 0 12px;
  padding: 10px 12px;
  border: 1px solid rgba(217, 119, 6, 0.24);
  border-radius: 8px;
  background: rgba(217, 119, 6, 0.08);
  color: var(--text-primary);
  line-height: 1.6;
}

.request-panel {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(280px, 0.65fr);
  gap: 12px;
}

.request-form,
.integration-card,
.result-panel,
.asset-table,
.summary-panel,
.chart-card,
.records-panel {
  padding: 16px;
}

.request-form header,
.integration-card header,
.result-panel header,
.asset-table header,
.chart-card header,
.records-panel header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.request-form header strong,
.integration-card header strong,
.result-panel header strong,
.asset-table header strong,
.chart-card header strong,
.records-panel header strong {
  display: block;
  margin-top: 5px;
  font-size: 18px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.form-grid label {
  display: flex;
  flex-direction: column;
  gap: 6px;
  color: var(--text-secondary);
  font-size: 13px;
}

.form-grid input,
.form-grid textarea {
  width: 100%;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--surface-color);
  color: var(--text-primary);
  font: inherit;
}

.form-grid input {
  height: 40px;
  padding: 0 10px;
}

.form-grid textarea {
  min-height: 104px;
  padding: 10px;
  resize: vertical;
}

.form-grid .wide {
  grid-column: 1 / -1;
}

.form-grid em {
  color: var(--text-tertiary);
  font-style: normal;
}

.check-row {
  justify-content: center;
}

.check-row input {
  width: 16px;
  height: 16px;
}

.sample-strip {
  margin: 12px 0;
}

.sample-strip button {
  align-items: flex-start;
  flex-direction: column;
  min-width: 150px;
}

.sample-strip span,
.sample-strip em {
  display: block;
}

.sample-strip em {
  color: var(--text-secondary);
  font-size: 12px;
  font-style: normal;
}

.primary-action {
  border-color: rgba(var(--primary-rgb), 0.32) !important;
  background: var(--primary-color) !important;
  color: var(--primary-contrast, #fff) !important;
}

.integration-card p {
  margin: 0 0 14px;
  color: var(--text-secondary);
  line-height: 1.7;
}

.integration-card dl,
.summary-panel dl {
  display: grid;
  gap: 10px;
  margin: 0;
}

.integration-card dl div,
.summary-panel dl div {
  display: grid;
  gap: 5px;
  min-width: 0;
}

.integration-card dt,
.summary-panel dt {
  color: var(--text-tertiary);
  font-size: 12px;
}

.integration-card dd,
.summary-panel dd {
  min-width: 0;
  margin: 0;
  overflow-wrap: anywhere;
  color: var(--text-primary);
}

.result-panel {
  grid-column: 1 / -1;
}

.result-body {
  display: grid;
  grid-template-columns: minmax(280px, 0.75fr) minmax(0, 1.25fr);
  gap: 12px;
}

.result-chart,
.comparison-chart {
  width: 100%;
  min-height: 280px;
}

.ranking-list {
  display: grid;
  gap: 8px;
}

.rank-row,
.empty-line,
.empty-result {
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--surface-color);
}

.rank-row {
  display: grid;
  grid-template-columns: 60px minmax(0, 1fr) minmax(120px, 0.7fr) auto;
  align-items: center;
  gap: 10px;
  min-height: 46px;
  padding: 8px 10px;
}

.rank-row span,
.rank-row em {
  color: var(--text-secondary);
  font-style: normal;
}

.rank-row strong,
.rank-row em {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rank-row b {
  color: var(--primary-color);
  white-space: nowrap;
}

.empty-line,
.empty-result {
  padding: 18px;
  color: var(--text-secondary);
  line-height: 1.7;
}

.metric-strip {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 12px;
}

.metric-strip article {
  min-height: 84px;
  padding: 14px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--surface-color);
}

.metric-strip span,
.metric-strip strong {
  display: block;
}

.metric-strip span {
  color: var(--text-secondary);
  font-size: 12px;
}

.metric-strip strong {
  margin-top: 8px;
  overflow-wrap: anywhere;
  font-size: 18px;
}

.asset-workbench {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 330px;
  gap: 12px;
}

.table-shell {
  overflow: auto;
}

table {
  width: 100%;
  min-width: 760px;
  border-collapse: collapse;
}

th,
td {
  padding: 12px 10px;
  border-bottom: 1px solid var(--border-color);
  text-align: left;
  vertical-align: top;
}

th {
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: var(--font-weight-title);
}

td strong,
td em {
  display: block;
}

td em {
  max-width: 360px;
  margin-top: 4px;
  overflow-wrap: anywhere;
  font-size: 12px;
  font-style: normal;
}

.summary-panel {
  display: grid;
  gap: 16px;
}

.summary-panel section + section {
  padding-top: 14px;
  border-top: 1px solid var(--border-color);
}

.compare-panel {
  display: grid;
  gap: 12px;
}

.comparison-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.comparison-list article {
  padding: 14px;
  cursor: pointer;
}

.comparison-list article.active {
  border-color: rgba(var(--primary-rgb), 0.36);
  background: rgba(var(--primary-rgb), 0.08);
}

.comparison-list header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.comparison-list header strong,
.comparison-list header span {
  display: block;
}

.comparison-list header span {
  margin-top: 4px;
  font-size: 12px;
}

.comparison-list header b {
  color: var(--primary-color);
  font-size: 22px;
}

.bar-list {
  display: grid;
  gap: 8px;
}

.bar-list div {
  display: grid;
  grid-template-columns: 40px minmax(0, 1fr) 44px;
  align-items: center;
  gap: 8px;
}

.bar-list i {
  height: 8px;
  overflow: hidden;
  border-radius: 999px;
  background: var(--border-color);
}

.bar-list i b {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--primary-color);
}

.bar-list em {
  color: var(--text-secondary);
  font-style: normal;
  text-align: right;
}

.records-panel header button {
  min-height: 34px;
}

.record-list {
  display: grid;
  gap: 10px;
}

.record-list article {
  padding: 14px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--surface-color);
}

.record-list span {
  color: var(--text-secondary);
  font-size: 12px;
}

.record-list strong {
  display: block;
  margin-top: 5px;
}

.record-list div {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.record-list b {
  padding: 4px 8px;
  border-radius: 999px;
  background: rgba(var(--primary-rgb), 0.1);
  color: var(--primary-color);
  font-size: 12px;
}

.rank-row,
.empty-line,
.empty-result,
.metric-strip article,
.record-list article,
.comparison-list article,
.form-grid input,
.form-grid textarea {
  background: var(--prediction-card-bg);
}

@media (hover: hover) and (pointer: fine) {
  .access-hero:hover,
  .summary-grid article:hover,
  .model-rail:hover,
  .model-console:hover,
  .request-form:hover,
  .integration-card:hover,
  .result-panel:hover,
  .asset-table:hover,
  .summary-panel:hover,
  .chart-card:hover,
  .records-panel:hover {
    border-color: color-mix(in srgb, var(--primary-color) 34%, var(--border-color));
    box-shadow: var(--shadow-hover);
  }

  .hero-actions button:hover,
  .action-row button:hover,
  .panel-tabs button:hover,
  .sample-strip button:hover,
  .records-panel header button:hover,
  .model-button:hover,
  .comparison-list article:hover {
    border-color: color-mix(in srgb, var(--primary-color) 42%, var(--border-color));
    background: rgba(var(--primary-rgb), 0.1);
    box-shadow: var(--shadow-ring);
    transform: translate3d(0, -1px, 0);
  }
}

@media (prefers-reduced-motion: reduce) {
  .access-hero,
  .summary-grid article,
  .model-rail,
  .model-console,
  .empty-console,
  .request-form,
  .integration-card,
  .result-panel,
  .asset-table,
  .summary-panel,
  .chart-card,
  .comparison-list article,
  .records-panel,
  .hero-actions button,
  .action-row button,
  .panel-tabs button,
  .sample-strip button,
  .records-panel header button,
  .model-button {
    transition: none;
    transform: none !important;
  }
}

@media (max-width: 1180px) {
  .access-layout,
  .request-panel,
  .asset-workbench {
    grid-template-columns: 1fr;
  }

  .model-rail {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 8px;
  }

  .model-rail header {
    grid-column: 1 / -1;
  }

  .model-button {
    margin-top: 0;
  }

  .comparison-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 820px) {
  .prediction-page {
    padding: 14px;
  }

  .access-hero,
  .console-head,
  .result-body {
    align-items: stretch;
    grid-template-columns: 1fr;
    flex-direction: column;
  }

  .summary-grid,
  .metric-strip,
  .form-grid,
  .comparison-list,
  .model-rail {
    grid-template-columns: 1fr;
  }

  .status-panel {
    flex: 1 1 auto;
  }

  .rank-row {
    grid-template-columns: 54px minmax(0, 1fr);
  }

  .rank-row em,
  .rank-row b {
    grid-column: 2;
  }
}
</style>
