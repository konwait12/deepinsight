export const PREDICTION_ANALYSIS_STORAGE_KEY = 'deepinsight:prediction-analysis-records';
export const SELECTED_PREDICTION_ANALYSIS_RECORD_KEY = 'deepinsight:selected-prediction-analysis-record';

export type PredictionAnalysisRecommendation = {
  rank: number
  itemId: string
  position?: string
  company?: string
  salary?: string
  score?: number
}

export type PredictionAnalysisRequest = {
  user_history: number[]
  top_k: number
  include_job_info: boolean
}

export type PredictionAnalysisMetrics = {
  historyLength: number
  requestedTopK: number
  recommendationCount: number
  fillRate: number
  detailCoverage: number
  scoreCoverage: number
  uniqueCompanyCount: number
  averageScore: number | null
  latencyBucket: 'excellent' | 'good' | 'slow' | 'unknown'
}

export type PredictionAnalysisRecord = {
  id: string
  createdAt: string
  modelName: string
  request: PredictionAnalysisRequest
  response: Record<string, unknown> | null
  status: string
  message: string
  serviceUrl: string
  elapsedMs: number | null
  recommendations: PredictionAnalysisRecommendation[]
  metrics: PredictionAnalysisMetrics
}

export type PredictionAnalysisRecordInput = {
  id?: string
  createdAt?: string
  modelName: string
  request: PredictionAnalysisRequest
  response?: Record<string, unknown> | null
  status?: string
  message?: string
  serviceUrl?: string
  elapsedMs?: number | null
  recommendations: PredictionAnalysisRecommendation[]
}

const MAX_RECORDS = 8;

function roundRatio(value: number) {
  if (!Number.isFinite(value)) return 0;
  return Math.round(value * 1000) / 1000;
}

function storageArea(type: 'local' | 'session') {
  if (typeof window !== 'undefined') {
    return type === 'local' ? window.localStorage : window.sessionStorage;
  }
  if (type === 'local' && typeof globalThis.localStorage !== 'undefined') return globalThis.localStorage;
  if (type === 'session' && typeof globalThis.sessionStorage !== 'undefined') return globalThis.sessionStorage;
  return null;
}

function latencyBucket(elapsedMs?: number | null): PredictionAnalysisMetrics['latencyBucket'] {
  if (elapsedMs === null || elapsedMs === undefined) return 'unknown';
  if (elapsedMs <= 500) return 'excellent';
  if (elapsedMs <= 1500) return 'good';
  return 'slow';
}

function buildMetrics(input: PredictionAnalysisRecordInput): PredictionAnalysisMetrics {
  const recommendationCount = input.recommendations.length;
  const requestedTopK = Math.max(1, Number(input.request.top_k || recommendationCount || 1));
  const detailCount = input.recommendations.filter((item) => Boolean(item.position || item.company || item.salary)).length;
  const scored = input.recommendations
    .map((item) => item.score)
    .filter((score): score is number => typeof score === 'number' && Number.isFinite(score));
  const companies = new Set(input.recommendations.map((item) => item.company?.trim()).filter(Boolean));

  return {
    historyLength: input.request.user_history.length,
    requestedTopK,
    recommendationCount,
    fillRate: roundRatio(recommendationCount / requestedTopK),
    detailCoverage: recommendationCount ? roundRatio(detailCount / recommendationCount) : 0,
    scoreCoverage: recommendationCount ? roundRatio(scored.length / recommendationCount) : 0,
    uniqueCompanyCount: companies.size,
    averageScore: scored.length ? roundRatio(scored.reduce((sum, score) => sum + score, 0) / scored.length) : null,
    latencyBucket: latencyBucket(input.elapsedMs),
  };
}

function makeRecordId() {
  return `prediction-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
}

function responseValue(response: Record<string, unknown> | null | undefined, key: string) {
  const value = response?.[key];
  return value === undefined || value === null ? '' : String(value);
}

export function buildPredictionAnalysisRecord(input: PredictionAnalysisRecordInput): PredictionAnalysisRecord {
  const response = input.response || null;
  return {
    id: input.id || makeRecordId(),
    createdAt: input.createdAt || new Date().toISOString(),
    modelName: input.modelName,
    request: input.request,
    response,
    status: input.status || responseValue(response, 'status') || 'success',
    message: input.message || responseValue(response, 'message'),
    serviceUrl: input.serviceUrl || responseValue(response, 'serviceUrl'),
    elapsedMs: input.elapsedMs ?? null,
    recommendations: input.recommendations,
    metrics: buildMetrics(input),
  };
}

export function listPredictionAnalysisRecords(): PredictionAnalysisRecord[] {
  const storage = storageArea('local');
  if (!storage) return [];

  try {
    const raw = storage.getItem(PREDICTION_ANALYSIS_STORAGE_KEY);
    const parsed = raw ? JSON.parse(raw) : [];
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

export function savePredictionAnalysisRecord(
  input: PredictionAnalysisRecord | PredictionAnalysisRecordInput,
  maxRecords = MAX_RECORDS,
): PredictionAnalysisRecord {
  const record = 'metrics' in input ? input : buildPredictionAnalysisRecord(input);
  const storage = storageArea('local');
  if (!storage) return record;

  const previous = listPredictionAnalysisRecords().filter((item) => item.id !== record.id);
  storage.setItem(PREDICTION_ANALYSIS_STORAGE_KEY, JSON.stringify([record, ...previous].slice(0, maxRecords)));
  return record;
}

export function selectPredictionAnalysisRecord(id: string) {
  storageArea('session')?.setItem(SELECTED_PREDICTION_ANALYSIS_RECORD_KEY, id);
}

export function getSelectedPredictionAnalysisRecordId() {
  return storageArea('session')?.getItem(SELECTED_PREDICTION_ANALYSIS_RECORD_KEY) || '';
}
