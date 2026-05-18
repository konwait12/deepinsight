import { ref, onMounted } from 'vue'
import { analysisApi, visualizationApi } from '@/api'
import type { ModelComparison } from '@/types/models'

export interface Job {
  id: number
  name: string
  status: string
  architecture: string
  type: 'training' | 'upload'
  optimizer?: string
  learningRate?: number
  batchSize?: number
  epochs?: number
  currentEpoch?: number
  loss?: number
  accuracy?: number
}

export function useRunSelector() {
  const jobs = ref<Job[]>([])
  const selectedId = ref<number | null>(null)
  const selectedJob = ref<Job | null>(null)
  const tags = ref<string[]>([])
  const loading = ref(false)

  const TAGS = ['train/loss', 'train/accuracy', 'val/loss', 'val/accuracy', 'train/learning_rate']

  const fetchJobs = async () => {
    loading.value = true
    try {
      const res = await visualizationApi.listExperiments()
      if (res.data.code === 200) {
        jobs.value = (res.data.data || []).map((j: any) => ({
          id: j.id,
          name: j.name,
          status: j.status,
          architecture: j.architecture || j.modelArchitecture || '',
          type: j.type === 'upload' ? 'upload' : 'training',
          optimizer: j.optimizer,
          learningRate: j.learningRate,
          batchSize: j.batchSize,
          epochs: j.epochs,
          currentEpoch: j.currentEpoch,
          loss: j.loss ?? j.currentLoss,
          accuracy: j.accuracy ?? j.currentAccuracy,
        }))
        return
      }
      throw new Error(res.data.message || 'Unable to load experiments')
    } catch (e) {
      try {
        const res = await analysisApi.modelComparison()
        if (res.data.code === 200) {
          jobs.value = (res.data.data || []).map((j: ModelComparison) => ({
            id: j.id,
            name: j.name,
            status: j.status,
            architecture: j.architecture || '',
            type: 'training',
            optimizer: j.optimizer,
            learningRate: j.learningRate,
            batchSize: j.batchSize,
            epochs: j.totalEpochs,
            currentEpoch: j.epochs,
            loss: j.loss,
            accuracy: j.accuracy,
          }))
        }
      } catch (fallbackError) {
        console.error(fallbackError)
      }
    } finally {
      loading.value = false
    }
  }

  const selectJob = (id: number | null) => {
    selectedId.value = id
    selectedJob.value = id ? (jobs.value.find(j => j.id === id) || null) : null
    tags.value = id ? TAGS : []
  }

  onMounted(fetchJobs)

  return {
    jobs,
    runs: jobs,
    selectedId,
    selectedRunId: selectedId,
    selectedJob,
    tags,
    loading,
    fetchJobs,
    fetchRuns: fetchJobs,
    selectJob,
    selectRun: selectJob,
  }
}
