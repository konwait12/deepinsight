<template>
  <div class="analysis-3d">
    <section class="analysis-3d-card">
      <header class="analysis-3d-head">
        <div>
          <span class="eyebrow">{{ isZh ? '模型状态空间' : 'Model state space' }}</span>
          <h3>{{ isZh ? '3D 模型对比散点' : '3D model scatter' }}</h3>
        </div>
        <p>{{ isZh ? '用准确率、损失与训练进度构成三维坐标，快速比较不同模型状态。' : 'Compare runs by accuracy, loss, and training progress in a 3D space.' }}</p>
      </header>
      <div ref="scatter3dRef" class="chart-box-3d"></div>
    </section>

    <section class="analysis-3d-card">
      <header class="analysis-3d-head">
        <div>
          <span class="eyebrow">{{ isZh ? '训练曲面' : 'Training surface' }}</span>
          <h3>{{ isZh ? '3D 损失与准确率曲面' : '3D loss and accuracy surface' }}</h3>
        </div>
        <p>{{ isZh ? '基于当前训练标量生成曲面，不再把没有上传的日志模块误判为无数据。' : 'Builds a surface from current scalar metrics instead of showing empty modules.' }}</p>
      </header>
      <div ref="surfaceRef" class="chart-box-3d"></div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import * as echarts from 'echarts'
import 'echarts-gl'

type TrainingJobLike = {
  id?: number | string
  name?: string
  accuracy?: number
  loss?: number
  epochs?: number
  totalEpochs?: number
  progress?: number
}

type CurveData = {
  steps?: number[]
  loss?: number[]
  accuracy?: number[]
}

const props = defineProps<{
  jobs: TrainingJobLike[]
  selectedJobId: number | string | null
  curveData: CurveData
}>()

const { locale } = useI18n()
const isZh = computed(() => locale.value.startsWith('zh'))

const scatter3dRef = ref<HTMLElement | null>(null)
const surfaceRef = ref<HTMLElement | null>(null)
let scatterChart: echarts.ECharts | null = null
let surfaceChart: echarts.ECharts | null = null
let resizeObserver: ResizeObserver | null = null

function toPercent(value: unknown) {
  const numberValue = Number(value)
  if (!Number.isFinite(numberValue)) return 0
  return numberValue <= 1.5 ? numberValue * 100 : numberValue
}

function selectedJobFallback() {
  return props.jobs.find((job) => String(job.id) === String(props.selectedJobId)) || props.jobs[0]
}

function buildScatterData() {
  const jobs = props.jobs.length ? props.jobs : selectedJobFallback() ? [selectedJobFallback()] : []
  return jobs
    .filter(Boolean)
    .map((job, index) => {
      const accuracy = toPercent(job.accuracy ?? props.curveData.accuracy?.at(-1) ?? 0)
      const loss = Number(job.loss ?? props.curveData.loss?.at(-1) ?? 0)
      const epoch = Number(job.epochs ?? props.curveData.steps?.at(-1) ?? index + 1)
      const totalEpochs = Math.max(Number(job.totalEpochs ?? props.curveData.steps?.length ?? epoch ?? 1), 1)
      const progress = Number.isFinite(Number(job.progress)) ? Number(job.progress) : Math.min(100, (epoch / totalEpochs) * 100)
      return {
        name: job.name || `${isZh.value ? '模型' : 'Run'} ${index + 1}`,
        value: [Number(accuracy.toFixed(2)), Number(loss.toFixed(4)), Number(progress.toFixed(2))],
      }
    })
    .filter((point) => point.value.some((value) => value > 0))
}

function buildSurfaceData() {
  const loss = props.curveData.loss || []
  const accuracy = props.curveData.accuracy || []
  const steps = props.curveData.steps || []
  const length = Math.max(loss.length, accuracy.length)
  const data: [number, number, number][] = []

  for (let index = 0; index < length; index += 1) {
    const step = Number(steps[index] ?? index + 1)
    if (Number.isFinite(loss[index])) data.push([step, 0, Number(loss[index].toFixed(5))])
    if (Number.isFinite(accuracy[index])) data.push([step, 1, Number((accuracy[index] / 100).toFixed(5))])
  }

  return data
}

function renderCharts() {
  if (!scatter3dRef.value || !surfaceRef.value) return

  scatterChart ||= echarts.init(scatter3dRef.value, undefined, { renderer: 'canvas' })
  surfaceChart ||= echarts.init(surfaceRef.value, undefined, { renderer: 'canvas' })

  const scatterData = buildScatterData()
  const surfaceData = buildSurfaceData()
  const commonText = {
    color: 'rgba(234, 240, 255, 0.78)',
    fontFamily: 'inherit',
  }

  scatterChart.setOption({
    backgroundColor: 'transparent',
    tooltip: {
      borderWidth: 0,
      backgroundColor: 'rgba(8, 14, 32, 0.86)',
      textStyle: { color: '#f8fbff' },
      formatter: (p: any) => `${p.name}<br/>Acc: ${p.value[0]}%<br/>Loss: ${p.value[1]}<br/>Progress: ${p.value[2]}%`,
    },
    xAxis3D: { type: 'value', name: 'Acc %', nameTextStyle: commonText, axisLabel: commonText },
    yAxis3D: { type: 'value', name: 'Loss', nameTextStyle: commonText, axisLabel: commonText },
    zAxis3D: { type: 'value', name: 'Progress %', nameTextStyle: commonText, axisLabel: commonText },
    grid3D: {
      boxWidth: 112,
      boxDepth: 82,
      viewControl: { autoRotate: true, autoRotateSpeed: 1.2, distance: 154 },
      axisLine: { lineStyle: { color: 'rgba(185, 204, 255, 0.28)' } },
      axisPointer: { lineStyle: { color: 'rgba(117, 197, 255, 0.62)' } },
      splitLine: { lineStyle: { color: 'rgba(185, 204, 255, 0.12)' } },
      light: { main: { intensity: 1.15 }, ambient: { intensity: 0.52 } },
    },
    series: [{
      type: 'scatter3D',
      data: scatterData,
      symbolSize: 12,
      itemStyle: { color: '#7dd3fc', opacity: 0.94, borderWidth: 1, borderColor: 'rgba(255,255,255,0.68)' },
      label: { show: scatterData.length <= 8, formatter: (p: any) => p.name, color: '#eef6ff', fontSize: 10, distance: 4 },
      emphasis: { itemStyle: { color: '#a7f3d0' } },
    }],
  }, true)

  surfaceChart.setOption({
    backgroundColor: 'transparent',
    tooltip: {
      borderWidth: 0,
      backgroundColor: 'rgba(8, 14, 32, 0.86)',
      textStyle: { color: '#f8fbff' },
      formatter: (p: any) => `${isZh.value ? '步数' : 'Step'}: ${p.value[0]}<br/>${p.value[1] === 0 ? 'Loss' : 'Acc'}: ${p.value[2]}`,
    },
    visualMap: {
      show: false,
      min: 0,
      max: Math.max(...surfaceData.map((item) => item[2]), 1),
      inRange: { color: ['#10233f', '#155e75', '#38bdf8', '#bae6fd'] },
    },
    xAxis3D: { type: 'value', name: isZh.value ? '步数' : 'Step', nameTextStyle: commonText, axisLabel: commonText },
    yAxis3D: {
      type: 'value',
      name: isZh.value ? '指标' : 'Metric',
      min: 0,
      max: 1,
      interval: 1,
      nameTextStyle: commonText,
      axisLabel: { ...commonText, formatter: (value: number) => value === 0 ? 'Loss' : 'Acc' },
    },
    zAxis3D: { type: 'value', name: isZh.value ? '数值' : 'Value', nameTextStyle: commonText, axisLabel: commonText },
    grid3D: {
      boxWidth: 112,
      boxDepth: 82,
      viewControl: { autoRotate: true, autoRotateSpeed: 0.8, distance: 154 },
      axisLine: { lineStyle: { color: 'rgba(185, 204, 255, 0.28)' } },
      splitLine: { lineStyle: { color: 'rgba(185, 204, 255, 0.12)' } },
      light: { main: { intensity: 1.05 }, ambient: { intensity: 0.58 } },
    },
    series: [{
      type: 'surface',
      data: surfaceData,
      wireframe: { show: true, lineStyle: { color: 'rgba(226, 245, 255, 0.16)', width: 1 } },
      shading: 'lambert',
      itemStyle: { opacity: 0.88 },
    }],
  }, true)
}

watch(
  () => [props.jobs, props.selectedJobId, props.curveData, locale.value],
  () => nextTick(renderCharts),
  { deep: true, immediate: true },
)

watch([scatter3dRef, surfaceRef], () => {
  nextTick(() => {
    renderCharts()
    resizeObserver?.disconnect()
    resizeObserver = new ResizeObserver(() => {
      scatterChart?.resize()
      surfaceChart?.resize()
    })
    if (scatter3dRef.value) resizeObserver.observe(scatter3dRef.value)
    if (surfaceRef.value) resizeObserver.observe(surfaceRef.value)
  })
})

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  scatterChart?.dispose()
  surfaceChart?.dispose()
  scatterChart = null
  surfaceChart = null
})
</script>

<style scoped>
.analysis-3d {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.analysis-3d-card {
  min-width: 0;
  overflow: hidden;
  border: 1px solid rgba(185, 204, 255, 0.16);
  border-radius: 18px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.07), rgba(255, 255, 255, 0.025)),
    rgba(11, 18, 36, 0.54);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.11), 0 18px 44px rgba(4, 10, 24, 0.22);
}

.analysis-3d-head {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  padding: 16px 18px 0;
}

.analysis-3d-head h3 {
  margin: 4px 0 0;
  color: var(--text-primary);
  font-size: 16px;
  font-weight: var(--font-weight-heading);
}

.analysis-3d-head p {
  max-width: 280px;
  margin: 0;
  color: var(--text-muted);
  font-size: 12px;
  line-height: 1.65;
  text-align: right;
}

.eyebrow {
  color: var(--text-muted);
  font-size: 11px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.chart-box-3d {
  width: 100%;
  height: 360px;
}

@media (max-width: 1100px) {
  .analysis-3d {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .analysis-3d-head {
    display: block;
  }

  .analysis-3d-head p {
    max-width: none;
    margin-top: 8px;
    text-align: left;
  }

  .chart-box-3d {
    height: 300px;
  }
}
</style>
