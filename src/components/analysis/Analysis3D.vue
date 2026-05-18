<template>
  <div class="analysis-3d">
    <el-row :gutter="16">
      <el-col :span="12">
        <el-card shadow="never" class="chart-card">
          <template #header><div class="chart-title">3D 模型对比散点</div></template>
          <div class="chart-desc-3d">三维空间中对比所有模型：X=准确率、Y=损失、Z=训练进度。相近颜色=相似模型，散点越靠右上=性能越好。</div>
          <div ref="scatter3dRef" class="chart-box-3d"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never" class="chart-card">
          <template #header><div class="chart-title">3D 训练曲面</div></template>
          <div class="chart-desc-3d">将选中模型的训练曲线映射到 3D 曲面：X=Epoch、Y=指标类型(0=Loss,1=Acc)、Z=值。曲面走势直观展示训练动态。</div>
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

const props = defineProps<{ jobs: any[]; selectedJobId: number | null; curveData: any }>();

const scatter3dRef = ref(); const surfaceRef = ref();

const initCharts = () => {
  // 3D Scatter — real data
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
      tooltip: { formatter: (p: any) => `${p.name}<br/>Acc: ${p.value[0]}%<br/>Loss: ${p.value[1]}<br/>Progress: ${p.value[2]}%` },
      xAxis3D: { type: 'value', name: 'Accuracy %' },
      yAxis3D: { type: 'value', name: 'Loss' },
      zAxis3D: { type: 'value', name: 'Progress %' },
      grid3D: { viewControl: { autoRotate: true, autoRotateSpeed: 3 }, light: { main: { intensity: 1.5 } } },
      series: [{
        type: 'scatter3D', data: scatterData.length > 0 ? scatterData : [{ name: 'No data', value: [50, 0.5, 50] }],
        symbolSize: 12, itemStyle: { color: '#6366f1', borderWidth: 1, borderColor: '#fff' },
        label: { show: true, formatter: (p: any) => p.name, fontSize: 10, distance: 3 },
        emphasis: { itemStyle: { color: '#4dc9f0' }, label: { fontSize: 12 } },
      }],
    });
  }

  // 3D Surface — from training curve
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
    } else {
      // Fallback surface
      for (let i = 0; i <= 40; i++) {
        for (let j = 0; j <= 20; j++) {
          const x = i / 4, y = j / 10;
          surfaceData.push([x, y, 0.5 * x * x + 0.3 * y * y + 0.4 * Math.sin(3 * x) * Math.cos(2 * y)]);
        }
      }
    }
    (c as any).setOption({
      tooltip: {},
      visualMap: cd ? { min: 0, max: 3, inRange: { color: ['#312e81', '#4338ca', '#6366f1', '#818cf8', '#a5b4fc', '#c7d2fe'] } } : { min: 0, max: 2.5, inRange: { color: ['#312e81', '#4338ca', '#6366f1', '#818cf8', '#a5b4fc', '#c7d2fe', '#e0e7ff'] } },
      xAxis3D: { type: 'value', name: cd ? 'Epoch' : 'Weight θ₁' },
      yAxis3D: { type: 'value', name: cd ? 'Metric (0=Loss,1=Acc)' : 'Weight θ₂' },
      zAxis3D: { type: 'value', name: cd ? 'Value' : 'Loss' },
      grid3D: { boxWidth: 100, boxDepth: 100, viewControl: { autoRotate: true, autoRotateSpeed: 1.5, distance: 150 } },
      series: [{ type: 'surface', data: surfaceData, wireframe: { show: false }, shading: 'realistic', realisticMaterial: { roughness: 0.5, metalness: 0 }, itemStyle: { opacity: 0.95 } }],
    });
  }
};

onMounted(() => nextTick(initCharts));
watch(() => [props.jobs, props.selectedJobId, props.curveData], () => nextTick(initCharts), { deep: true });
</script>

<style scoped>
.chart-card { border-radius: 16px; margin-bottom: 16px; }
.chart-title { font-size: 13px; font-weight: 800; color: var(--text-primary); }
.chart-desc-3d { font-size: 11px; color: var(--text-muted); margin-bottom: 4px; line-height: 1.5; padding: 6px 10px; background: var(--bg-input); border-radius: 8px; }
.chart-box-3d { width: 100%; height: 380px; }
</style>
