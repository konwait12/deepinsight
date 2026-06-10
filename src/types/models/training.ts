import type { Status } from '../common'

export interface TrainingJob {
  id: number
  name: string
  modelArchitecture: string
  status: Status
  learningRate: number
  batchSize: number
  epochs: number
  currentEpoch: number
  optimizer: string
  device: string
  currentLoss: number | null
  currentAccuracy: number | null
  totalTimeSec: number | null
  updatedAt?: string
  createdAt?: string
}

export interface TrainingConfig {
  name: string
  modelArchitecture: string
  learningRate: number
  batchSize: number
  epochs: number
  optimizer: string
  device: string
}

export type ModelMetricValue = string | number | null | undefined
export type ModelMetricMap = Record<string, ModelMetricValue>

export interface ModelOption {
  id?: number | string
  name: string
  displayNameZh?: string
  paramCountM?: number
  params?: number | string
  taskType: string
  taskTypeZh?: string
  isOfficial?: boolean
  official?: boolean
  readOnly?: boolean
  canManage?: boolean
  canSync?: boolean
  articleid?: number | string
  framework?: string
  architecture?: string
  modelGroup?: string
  statusLabel?: string
  description?: string
  descriptionZh?: string
  inputSize?: string
  paperUrl?: string
  catalogSource?: string
  integrationStatus?: string
  online?: boolean
  canExecute?: boolean
  latencyMs?: number
  endpoint?: string
  serviceUrl?: string
  metricSource?: string
  statusReason?: string
  artifactExists?: boolean
  datasetExists?: boolean
  jobInfoExists?: boolean
  artifactPath?: string
  dataPath?: string
  runtimeHealth?: Record<string, unknown>
  topK?: number
  metrics?: ModelMetricMap
  datasetSummary?: ModelMetricMap
  trainingSummary?: ModelMetricMap
  parameterSummary?: ModelMetricMap
  visualProfile?: ModelMetricMap
  dataAssets?: Array<Record<string, string | number | boolean | null | undefined>>
  databaseBacked?: boolean
}
