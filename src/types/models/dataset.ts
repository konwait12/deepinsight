export interface Dataset {
  id: number
  name: string
  taskType: string
  format: string
  sampleCount: number
  fileSizeMb: number
  splitRatio: string
  status: string
  description?: string
  classCount?: number
  filePath?: string
  createdAt?: string
}

export interface DatasetForm {
  name: string
  taskType: string
  format: string
  description: string
  status: string
  sampleCount: number
  fileSizeMb: number
  splitRatio: string
  classCount?: number
  filePath?: string
}
