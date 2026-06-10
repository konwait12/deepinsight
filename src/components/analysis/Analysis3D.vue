<template>
  <div class="analysis-3d">
    <el-row :gutter="16">
      <el-col :span="12">
        <el-card shadow="never" class="chart-card">
          <template #header><div class="chart-title">3D 推荐模型对比散点</div></template>
          <div class="chart-desc-3d">三维空间中对比模型状态：X=推荐质量，Y=误差风险，Z=训练进度。散点来自模型训练字段，用于快速定位异常模型。</div>
          <div ref="scatter3dRef" class="chart-box-3d"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never" class="chart-card">
          <template #header><div class="chart-title">3D 指标曲面</div></template>
          <div class="chart-desc-3d">将选中模型的曲线映射到 3D 曲面：X=轮次，Y=指标类型，Z=指标值。没有曲线时按当前模型指标生成基准曲面。</div>
          <div ref="surfaceRef" class="chart-box-3d"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, watch } from 'vue';
import * as echarts from 'echarts';
import 'echarts-gl';
import { buildChartTheme, chartAlpha, chartTooltip } from '@/utils/chartTheme';

const props = defineProps<{ jobs: any[]; selectedJobId: number | null; curveData: any }>();

const scatter3dRef = ref(); const surfaceRef = ref();

function axis3D(theme: ReturnType<typeof buildChartTheme>) {
  return {
    axisLine: { lineStyle: { color: theme.border } },
    axisLabel: { color: theme.text, fontSize: 11 },
    axisTick: { show: false },
    nameTextStyle: { color: theme.text, fontSize: 12, fontWeight: 600 },
    splitLine: { lineStyle: { color: chartAlpha(theme.border, 0.55) } },
  };
}

const initCharts = () => {
  const theme = buildChartTheme();
  const axis = axis3D(theme);
  const selectedJob = props.jobs?.find((j: any) => j.id === props.selectedJobId) || props.jobs?.[0];

  // 3D Scatter - real model fields
  if (scatter3dRef.value && props.jobs?.length) {
    const c = echarts.init(scatter3dRef.value);
    const scatterData = props.jobs.filter((j: any) => j.accuracy != null).map((j: any) => ({
      name: j.name || '',
      value: [
        Math.round((j.accuracy || 0.5) * 100),
        Math.round((j.loss || 0.5) * 100) / 100,
        Math.round((j.epochs || 0) / Math.max((j.totalEpochs || 100), 1) * 100),
      ],
    }));
    (c as any).setOption({
      backgroundColor: 'transparent',
      tooltip: { ...chartTooltip(theme), formatter: (p: any) => `${p.name}<br/>推荐质量: ${p.value[0]}%<br/>Loss: ${p.value[1]}<br/>训练进度: ${p.value[2]}%` },
      xAxis3D: { type: 'value', name: '推荐质量 %', ...axis },
      yAxis3D: { type: 'value', name: 'Loss', ...axis },
      zAxis3D: { type: 'value', name: '训练进度 %', ...axis },
      grid3D: {
        viewControl: { autoRotate: true, autoRotateSpeed: 2.4, distance: 145 },
        axisPointer: { lineStyle: { color: theme.primary } },
        light: { main: { intensity: 1.35 }, ambient: { intensity: 0.38 } },
      },
      series: [{
        type: 'scatter3D',
        data: scatterData.length > 0 ? scatterData : [{ name: '暂无模型数据', value: [0, 0, 0] }],
        symbolSize: 12,
        itemStyle: { color: theme.primary, borderWidth: 1, borderColor: chartAlpha(theme.title, 0.9) },
        label: { show: true, formatter: (p: any) => p.name, fontSize: 10, color: theme.text, distance: 3 },
        emphasis: { itemStyle: { color: theme.accent }, label: { fontSize: 12, color: theme.title } },
      }],
    });
  }

  // 3D Surface - from training curve or selected model baseline
  if (surfaceRef.value) {
    const c = echarts.init(surfaceRef.value);
    const cd = props.curveData;
    let surfaceData: [number, number, number][] = [];
    if (cd?.loss && cd?.accuracy) {
      const len = Math.min(cd.loss.length, cd.accuracy.length);
      for (let i = 0; i < len; i++) {
        surfaceData.push([i + 1, 0, Math.round(cd.loss[i] * 1000) / 1000]);
        surfaceData.push([i + 1, 1, Math.round(cd.accuracy[i] * 1000) / 1000]);
      }
    } else if (selectedJob) {
      const baseLoss = Number(selectedJob.loss ?? 0.8);
      const baseAccuracy = Number(selectedJob.accuracy ?? 0.55);
      const progress = Math.max(0.15, Math.min(1, Number(selectedJob.epochs || 0) / Math.max(Number(selectedJob.totalEpochs || 100), 1)));
      for (let i = 0; i <= 40; i++) {
        for (let j = 0; j <= 20; j++) {
          const epoch = i + 1;
          const metricBand = j / 20;
          const convergence = Math.exp(-epoch / (18 + progress * 20));
          const value = metricBand < .5
            ? Math.max(0.02, baseLoss * convergence + .025 * Math.sin(epoch * .32))
            : Math.min(1, baseAccuracy * (1 - convergence * .72) + progress * .18);
          surfaceData.push([epoch, metricBand, Math.round(value * 1000) / 1000]);
        }
      }
    }
    (c as any).setOption({
      backgroundColor: 'transparent',
      tooltip: chartTooltip(theme),
      visualMap: {
        min: 0,
        max: cd ? 3 : 1,
        calculable: false,
        textStyle: { color: theme.text },
        inRange: { color: [theme.danger, theme.warning, theme.accent, theme.primary, chartAlpha(theme.primary, .72)] },
      },
      xAxis3D: { type: 'value', name: '轮次', ...axis },
      yAxis3D: { type: 'value', name: '指标层', ...axis },
      zAxis3D: { type: 'value', name: '指标值', ...axis },
      grid3D: {
        boxWidth: 100,
        boxDepth: 100,
        viewControl: { autoRotate: true, autoRotateSpeed: 1.35, distance: 150 },
        light: { main: { intensity: 1.28 }, ambient: { intensity: 0.42 } },
      },
      series: [{ type: 'surface', data: surfaceData, wireframe: { show: false }, shading: 'realistic', realisticMaterial: { roughness: 0.58, metalness: 0 }, itemStyle: { opacity: 0.92 } }],
    });
  }
};

onMounted(() => nextTick(initCharts));
watch(() => [props.jobs, props.selectedJobId, props.curveData], () => nextTick(initCharts), { deep: true });
</script>

<style scoped>
.chart-card {
  margin-bottom:16px;
  background:linear-gradient(145deg, rgba(var(--glass-tint-rgb), .36), rgba(var(--panel-rgb), .12)) !important;
  border:1px solid rgba(var(--primary-rgb), .12) !important;
  border-radius:18px !important;
  box-shadow:var(--shadow-soft);
  backdrop-filter:blur(calc(var(--glass-blur) * .8));
  overflow:hidden;
  transition:
    border-color var(--motion-hover) var(--ease-liquid),
    box-shadow var(--motion-medium) var(--ease-smooth),
    transform var(--motion-hover) var(--ease-liquid);
}
.chart-card :deep(.el-card__header){
  background:rgba(var(--glass-tint-rgb), .2);
  border-bottom:1px solid rgba(var(--primary-rgb), .1);
}
.chart-title { font-size: 13px; font-weight: 800; color: var(--text-primary); }
.chart-desc-3d {
  font-size: 11px;
  color: var(--text-muted);
  margin-bottom: 10px;
  line-height: 1.6;
  padding: 8px 10px;
  background: rgba(var(--glass-tint-rgb), .16);
  border:1px solid rgba(var(--primary-rgb), .08);
  border-radius: 10px;
}
.chart-box-3d { width: 100%; height: 380px; }

@media (hover:hover) and (pointer:fine){
  .chart-card:hover{
    border-color:rgba(var(--primary-rgb), .24) !important;
    box-shadow:var(--shadow-hover);
    transform:translate3d(0,-2px,0);
  }
}

@media (prefers-reduced-motion:reduce){
  .chart-card{transition:none}
}
</style>
