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

export interface ModelOption {
  id?: number
  name: string
  displayNameZh?: string
  paramCountM?: number
  params?: number
  taskType: string
  taskTypeZh?: string
  isOfficial?: boolean
  articleId?: number
  framework?: string
  description?: string
  descriptionZh?: string
  inputSize?: string
  paperUrl?: string
}
