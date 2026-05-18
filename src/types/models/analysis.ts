export interface ModelComparison {
  id: number
  name: string
  architecture: string
  status: string
  loss: number
  accuracy: number
  epochs: number
  totalEpochs: number
  optimizer: string
  learningRate: number
  batchSize: number
}

export interface AnalysisOverview {
  totalJobs: number
  runningJobs: number
  avgLoss: number
  avgAccuracy: number
}

export interface TrainingCurve {
  loss: number[]
  accuracy: number[]
  valLoss: number[]
}

export interface HyperparamData {
  dimensions: string[]
  data: Record<string, number>[]
}
