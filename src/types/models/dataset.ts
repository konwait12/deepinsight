import type { Status } from '../common'

export interface Dataset {
  id: number
  name: string
  taskType: string
  format: string
  sampleCount: number
  fileSizeMb: number
  splitRatio: string
  status: Status
  description?: string
  classCount?: number
  filePath?: string
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
}
