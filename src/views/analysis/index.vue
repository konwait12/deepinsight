<template>
  <div class="analysis-page">
    <div class="page-header entrance-hero"><h2>{{ t('nav.analysis') }}</h2><p>{{ t('analysis.subtitle') }}</p></div>

    <!-- Model Selector + Data Source -->
    <div class="control-bar entrance-up" style="animation-delay: 0.08s">
      <div class="control-left">
        <el-radio-group v-model="dataSource" size="small" @change="onDataSourceChange">
          <el-radio-button value="simulated">模拟训练</el-radio-button>
          <el-radio-button value="uploaded">训练运行</el-radio-button>
        </el-radio-group>
        <span class="control-label ml-3">{{ dataSource === 'simulated' ? t('analysis.selectModel') : '选择运行' }}</span>
        <el-select v-if="dataSource === 'simulated'" v-model="selectedJobId" @change="onModelChange" placeholder="Select model..." class="model-select">
          <el-option v-for="j in jobs" :key="j.id" :label="j.name + ' (' + (j.architecture||'?') + ')'" :value="j.id" />
        </el-select>
        <el-select v-else v-model="selectedRunId" @change="onRunChange" placeholder="选择训练运行..." class="model-select">
          <el-option v-for="r in uploadedRuns" :key="r.id" :label="r.name" :value="r.id" />
        </el-select>
        <el-tag v-if="currentJob" size="small" :type="statusTag(currentJob.status)">{{ currentJob.status }}</el-tag>
      </div>
      <el-button v-if="dataSource === 'simulated'" type="primary" @click="runAiAnalysis" :loading="aiLoading" :style="{backgroundColor:'var(--primary-color)'}">AI 分析</el-button>
      <div v-else class="flex gap-2">
        <el-button size="small" @click="goToViz('scalars')">标量</el-button>
        <el-button size="small" @click="goToViz('images')">样本</el-button>
        <el-button size="small" @click="goToViz('histograms')">直方图</el-button>
        <el-button size="small" @click="goToViz('hparams')">参数</el-button>
      </div>
    </div>

    <!-- Progress Bars -->
    <el-card shadow="never" class="chart-card mb-4">
      <template #header><div class="chart-title">训练进度</div></template>
      <div class="progress-grid entrance-up" style="animation-delay: 0.12s">
        <div v-for="j in jobs" :key="j.id" class="progress-item" :class="{ active: j.id===selectedJobId }" @click="selectedJobId=j.id; onModelChange()">
          <div class="prog-header">
            <span class="prog-name">{{ j.name }}</span>
            <span class="prog-epoch">{{ j.epochs||0 }}/{{ j.totalEpochs||'?' }} epochs</span>
          </div>
          <el-progress :percentage="jobProgress(j)" :status="j.status==='completed'?'success':j.status==='failed'?'exception':undefined" :stroke-width="10"/>
          <div class="prog-meta">
            <span>Loss: {{ j.loss?.toFixed(4) || '--' }}</span>
            <span>Acc: {{ j.accuracy ? (j.accuracy*100).toFixed(1)+'%' : '--' }}</span>
          </div>
        </div>
      </div>
    </el-card>

    <!-- AI Insight -->
    <el-card v-if="aiInsight" shadow="never" class="ai-insight-card mb-4 entrance-up" style="animation-delay:0.18s">
      <template #header><div class="ai-header">AI 分析洞察 — {{ currentJob?.name }}</div></template>
      <div class="ai-insight-body" v-html="aiInsightHtml"></div>
    </el-card>

    <!-- Metric Cards -->
    <el-row :gutter="16" class="mb-4 stagger-fast">
      <el-col :xs="12" :sm="6" v-for="m in metrics" :key="m.label">
        <el-card shadow="hover" class="metric-card"><div class="metric-value" :style="{color:m.color}">{{ m.value }}</div><div class="metric-label">{{ m.label }}</div></el-card>
      </el-col>
    </el-row>

    <!-- Charts Row 1 -->
    <el-row :gutter="16" class="entrance-up" style="animation-delay:0.24s">
      <el-col :span="12">
        <el-card shadow="never" class="chart-card">
          <template #header><div class="chart-title">{{ t('analysis.lossCurve') }} <span class="chart-subtitle">— {{ currentJob?.name || t('analysis.selectModelHint') }}</span></div></template>
          <div class="chart-desc">展示当前推荐模型的训练损失、验证损失和准确率变化。Loss 越低越好，Accuracy 越高越好，用来判断收敛是否稳定。</div>
          <div ref="lossChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never" class="chart-card">
          <template #header><div class="chart-title">{{ t('analysis.radarCompare') }}</div></template>
          <div class="chart-desc">把 Loss、Accuracy、训练进度、学习率、批次规模和综合状态统一成 0-100 分，便于横向比较推荐模型。</div>
          <div ref="radarChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Charts Row 2 -->
    <el-row :gutter="16" class="mt-4 entrance-up" style="animation-delay:0.30s">
      <el-col :span="24">
        <el-card shadow="never" class="chart-card">
          <template #header><div class="chart-title">参数并行坐标</div></template>
          <div class="chart-desc">每个模型是一条横跨多个参数维度的折线，用来观察序列长度、批次大小、学习率和注意力头配置差异。</div>
          <div ref="parallelRef" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>
    <el-row :gutter="16" class="mt-4 entrance-up" style="animation-delay:0.36s">
      <el-col :span="12">
        <el-card shadow="never" class="chart-card">
          <template #header><div class="chart-title">命中 / 排序曲线</div></template>
          <div class="chart-desc">按 Top-K 展示推荐命中趋势，曲线越靠上说明模型在候选列表前排给出有效推荐的能力越强。</div>
          <div ref="prChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never" class="chart-card">
          <template #header><div class="chart-title">训练趋势对比</div></template>
          <div class="chart-desc">所有模型的 Loss 与 Accuracy 双轴对比。左轴看损失，右轴看准确率，帮助快速定位最稳的模型。</div>
          <div ref="barChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 3D -->
    <Analysis3D :jobs="jobs" :selectedJobId="selectedJobId" :curveData="curveData" class="mt-4 entrance-up" style="animation-delay:0.42s" />

    <!-- Comparison Table -->
    <el-card shadow="never" class="chart-card mt-4">
      <template #header><div class="chart-title">{{ t('analysis.modelCompare') }}</div></template>
      <div class="chart-desc">所有训练任务的关键指标一览。点击表头可排序，快速找到最优模型。</div>
      <el-table :data="trainingJobs" stripe empty-text="No data">
        <el-table-column prop="name" :label="t('analysis.expName')" min-width="160"/>
        <el-table-column prop="model" :label="t('analysis.architecture')" width="140"/>
        <el-table-column prop="accuracy" :label="t('analysis.accuracy')" width="100" sortable/>
        <el-table-column prop="loss" :label="t('analysis.loss')" width="100" sortable/>
        <el-table-column prop="status" :label="t('data.status')" width="100"><template #default="{row}"><el-tag :type="row._status==='running'?'primary':row._status==='completed'?'success':'info'" size="small">{{ row._status }}</el-tag></template></el-table-column>
        <el-table-column prop="optimizer" label="Optimizer" width="100"/>
        <el-table-column prop="lr" :label="t('dashboard.learningRate')" width="100"/>
        <el-table-column prop="batch" label="Batch" width="80"/>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import * as echarts from 'echarts';
import { analysisApi, visualizationApi, aiApi } from '@/api';
import type { ChatRequest } from '@/api';
import { renderMarkdown } from '@/utils/markdown';
import { buildChartTheme, chartAlpha, chartAxis, chartGrid, chartLegend, chartTooltip } from '@/utils/chartTheme';
import Analysis3D from '@/components/analysis/Analysis3D.vue';
import type { ModelComparison } from '@/types/models';
import { ROUTES } from '@/constants';

const { t } = useI18n();
const router = useRouter();
const lossChartRef=ref(); const radarChartRef=ref(); const prChartRef=ref(); const barChartRef=ref(); const parallelRef=ref();
const selectedJobId=ref<number|null>(null);
const selectedRunId=ref<number|null>(null);
const dataSource=ref('simulated');
const jobs=ref<ModelComparison[]>([]);
const uploadedRuns=ref<any[]>([]);
const currentJob=ref<ModelComparison | null>(null);
const curveData=ref<{ loss?: number[]; accuracy?: number[]; valLoss?: number[] } | null>(null);
const aiLoading=ref(false); const aiInsight=ref(''); const aiInsightHtml=ref('');
const metrics=ref([{label:'Total',value:'0',color:'var(--primary-color)'},{label:'Running',value:'0',color:'var(--accent-glow)'},{label:'Avg Loss',value:'--',color:'var(--danger-glow)'},{label:'Avg Acc',value:'--',color:'var(--warning-glow)'}]);
const trainingJobs=ref<any[]>([]);

function goToViz(type: string) {
  if (selectedRunId.value) {
    sessionStorage.setItem('vizRunId', String(selectedRunId.value));
    sessionStorage.setItem('vizModuleKey', type);
    router.push(ROUTES.VIZ);
  }
}

const statusTag=(s:string)=>s==='running'?'primary':s==='completed'?'success':s==='failed'?'danger':'info';
const jobProgress=(j:any)=>j.totalEpochs>0?Math.round((j.epochs||0)/j.totalEpochs*100):j.status==='completed'?100:0;
const clampScore=(value:number)=>Math.max(0,Math.min(100,Math.round(value)));
const modelProgressScore=(m:any)=>clampScore((m.epochs||0)/Math.max((m.totalEpochs||100),1)*100);
const lossQualityScore=(m:any)=>clampScore((1-Math.min(Number(m.loss ?? 1),1))*100);
const accuracyScore=(m:any)=>clampScore(Number(m.accuracy||0)*100);
const learningRateScore=(m:any)=>{
  const lr=Number(m.learningRate||0);
  if(!lr)return 0;
  return clampScore(100-Math.abs(Math.log10(lr)-Math.log10(0.001))*28);
};
const batchScore=(m:any)=>clampScore(Math.min(Number(m.batchSize||0),512)/512*100);
const evidenceScore=(m:any)=>clampScore((lossQualityScore(m)+accuracyScore(m)+modelProgressScore(m)+learningRateScore(m)+batchScore(m))/5);

const fetchAll=async()=>{
  try{
    const mc=await analysisApi.modelComparison();
    if(mc.data.code===200){
      jobs.value=(mc.data.data||[]).map((j: ModelComparison) => ({...j, totalEpochs: (j as any).totalEpochs||100, epochs: (j as any).epochs||0}));
      trainingJobs.value=jobs.value.map((j) => ({name:j.name, model:j.architecture||'', accuracy:j.accuracy?Math.round(j.accuracy*10000)/100+'%':'--', loss:j.loss?.toFixed(4)||'--', _status:j.status, status:j.status, optimizer:j.optimizer||'--', lr:j.learningRate||'--', batch:j.batchSize||'--'}));
    }
    const ov=await analysisApi.overview();
    if(ov.data.code===200){const od=ov.data.data;metrics.value=[{label:t('analysis.totalJobs'),value:od.totalJobs,color:'var(--primary-color)'},{label:t('analysis.running'),value:od.runningJobs,color:'var(--accent-glow)'},{label:t('analysis.avgLoss'),value:od.avgLoss,color:'var(--danger-glow)'},{label:t('analysis.avgAccuracy'),value:Math.round(od.avgAccuracy*100)+'%',color:'var(--warning-glow)'}];}
  }catch{/* API unavailable */}
};

const fetchCurve=async()=>{
  if(!selectedJobId.value)return;
  try{
    currentJob.value=jobs.value.find(j => j.id === selectedJobId.value) || null;
    const d=await analysisApi.trainingCurve(selectedJobId.value);
    if(d.data.code===200)curveData.value=d.data.data;
    nextTick(initCharts);
  }catch{/* API unavailable */}
};

const onModelChange=()=>{aiInsight.value='';fetchCurve();};

const fetchUploadedRuns=async()=>{
  try{
    const d=await visualizationApi.listRuns();
    if(d.data.code===200) {
      uploadedRuns.value=d.data.data||[];
      // Bridge: auto-select run from viz page context
      const storedId=sessionStorage.getItem('vizRunId');
      if(storedId && !selectedRunId.value) {
        const id=parseInt(storedId);
        if(uploadedRuns.value.find((r:any)=>r.id===id)) {
          selectedRunId.value=id;
          onRunChange();
        }
      }
    }
  }catch{}
};

const onDataSourceChange=(val:string)=>{
  if(val==='uploaded') {
    fetchUploadedRuns();
    if(!selectedRunId.value && uploadedRuns.value.length>0) {
      selectedRunId.value=uploadedRuns.value[0].id;
      onRunChange();
    }
  }
};

const onRunChange=async()=>{
  if(!selectedRunId.value)return;
  // Bridge: share selected run with viz pages
  sessionStorage.setItem('vizRunId', String(selectedRunId.value));
  try{
    const d=await visualizationApi.getScalars(selectedRunId.value, 'train/loss');
    if(d.data.code===200 && d.data.data?.length) {
      curveData.value={loss:d.data.data.map((s:any)=>s.value),accuracy:[],valLoss:[]};
      nextTick(initCharts);
    }
    const d2=await visualizationApi.getScalars(selectedRunId.value, 'train/accuracy');
    if(d2.data.code===200 && d2.data.data?.length && curveData.value) {
      curveData.value.accuracy=d2.data.data.map((s:any)=>s.value);
      nextTick(initCharts);
    }
  }catch{}
};

const runAiAnalysis=async()=>{
  if(!currentJob.value)return;
  aiLoading.value=true;
  try{
    const j=currentJob.value;
    const prompt=`请分析这个深度学习训练结果：\n模型: ${j.name}\n架构: ${j.architecture}\n状态: ${j.status}\n当前Loss: ${j.loss}\n当前Accuracy: ${j.accuracy}\n训练进度: ${(j as any).epochs||'?'}/${(j as any).totalEpochs||'?'} epochs\n优化器: ${j.optimizer}\n学习率: ${j.learningRate}\n批次大小: ${j.batchSize}\n\n请用中文给出3点分析：1) 是否存在过拟合或欠拟合风险 2) 收敛质量如何 3) 优化建议。每点一句话，总共100字以内。`;
    const d=await aiApi.chat({message:prompt,temperature:0.3});
    if(d.data.code===200&&d.data.data?.reply){aiInsight.value=d.data.data.reply;aiInsightHtml.value=renderMarkdown(d.data.data.reply);}
  }catch{aiInsight.value='AI分析不可用';}
  aiLoading.value=false;
};

let poll:any=null;

const initCharts=async()=>{
  const cd=curveData.value;
  const theme=buildChartTheme();
  const axis=chartAxis(theme);
  const lossColor=theme.danger;
  const valLossColor=theme.warning;
  const accColor=theme.primary;
  const metricPalette=[theme.primary,theme.accent,theme.danger,theme.warning,theme.muted];
  const lossWash=chartAlpha(lossColor,0.15);
  const lossClear=chartAlpha(lossColor,0);
  const accWash=chartAlpha(accColor,0.18);
  const accClear=chartAlpha(accColor,0);
  // Loss/Accuracy
  if(lossChartRef.value){
    const c=echarts.init(lossChartRef.value);
    const x=cd?.loss?Array.from({length:cd.loss.length},(_,i)=>i+1):Array.from({length:50},(_,i)=>i+1);
    c.setOption({backgroundColor:'transparent',tooltip:{...chartTooltip(theme),trigger:'axis'},legend:{data:['Train Loss','Val Loss','Accuracy'],bottom:0,...chartLegend(theme)},grid:chartGrid({left:60,right:60,top:10,bottom:30}),xAxis:{type:'category',data:x,name:'Epoch',...axis},yAxis:[{type:'value',name:'Loss',...axis},{type:'value',name:'Acc %',max:100,...axis}],series:[
      {name:'Train Loss',type:'line',data:cd?.loss||x.map((_:any,i:number)=>+Math.max(0.02,2.5*Math.exp(-0.08*i)+0.025*Math.sin(i*.55)).toFixed(3)),smooth:true,symbol:'none',lineStyle:{color:lossColor},areaStyle:{color:new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color:lossWash},{offset:1,color:lossClear}])}},
      {name:'Val Loss',type:'line',data:cd?.valLoss||x.map((_:any,i:number)=>+Math.max(0.03,2.7*Math.exp(-0.07*i)+0.035*Math.cos(i*.42)).toFixed(3)),smooth:true,symbol:'none',lineStyle:{color:valLossColor}},
      {name:'Accuracy',type:'line',yAxisIndex:1,data:cd?.accuracy?cd.accuracy.map((v:number)=>Math.round(v*10000)/100):x.map((_:any,i:number)=>+(96*(1-Math.exp(-0.06*i))+1.2*Math.sin(i*.28)).toFixed(2)),smooth:true,symbol:'none',lineStyle:{color:accColor},areaStyle:{color:new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color:accWash},{offset:1,color:accClear}])}}
    ]});
  }
  // Radar
  if(radarChartRef.value){
    const c=echarts.init(radarChartRef.value);
    const colors=metricPalette;
    const models=jobs.value.slice(0,4);
    c.setOption({backgroundColor:'transparent',tooltip:chartTooltip(theme),legend:{data:models.map((m:any)=>m.name),bottom:0,...chartLegend(theme)},radar:{center:["50%","45%"],radius:"62%",axisName:{color:theme.text,fontSize:11},axisLine:{lineStyle:{color:theme.border}},splitLine:{lineStyle:{color:theme.border,opacity:.45}},splitArea:{areaStyle:{color:[chartAlpha(theme.primary,.04),chartAlpha(theme.accent,.02)]}},indicator:[{name:'Loss得分',max:100},{name:'命中质量',max:100},{name:'训练进度',max:100},{name:'学习率',max:100},{name:'批次规模',max:100},{name:'综合状态',max:100}]},series:[{type:"radar",data:models.map((m:any,i:number)=>{const color=colors[i%colors.length];return {value:[lossQualityScore(m),accuracyScore(m),modelProgressScore(m),learningRateScore(m),batchScore(m),evidenceScore(m)],name:m.name,areaStyle:{color:chartAlpha(color,.16)},lineStyle:{color,width:2},itemStyle:{color}}})}]});
  }
  // Top-K recommendation curve
  if(prChartRef.value){
    const c=echarts.init(prChartRef.value);
    const topK=[1,3,5,10,20,50];
    const colors=metricPalette;
    const models=jobs.value.slice(0,3);
    c.setOption({backgroundColor:'transparent',tooltip:{...chartTooltip(theme),trigger:"axis"},legend:{data:models.map((m:any)=>m.name),bottom:0,...chartLegend(theme)},grid:chartGrid({left:54,right:24,top:18,bottom:42}),xAxis:{type:"category",name:'Top-K',data:topK.map(k=>`Top-${k}`),...axis},yAxis:{type:"value",name:'命中率',min:0,max:1,...axis},series:models.map((m:any,i:number)=>{const color=colors[i%colors.length];const base=Math.max(0.08,Number(m.accuracy||0.45));return {name:m.name,type:"line",data:topK.map((k:number)=>+Math.min(.99,base*(1-Math.exp(-k/12))+.08).toFixed(3)),smooth:true,symbol:"circle",symbolSize:6,lineStyle:{color,width:2},itemStyle:{color},areaStyle:{color:new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color:chartAlpha(color,.16)},{offset:1,color:chartAlpha(color,0)}])}};})});
  }
  // Bar comparison
  if(barChartRef.value){
    const c=echarts.init(barChartRef.value);
    const names=jobs.value.map((j:any)=>j.name?.slice(0,15)||'');
    const losses=jobs.value.map((j:any)=>j.loss||0);
    const accs=jobs.value.map((j:any)=>(j.accuracy||0)*100);
    c.setOption({backgroundColor:'transparent',tooltip:{...chartTooltip(theme),trigger:'axis'},legend:{data:['Loss','Accuracy %'],bottom:0,...chartLegend(theme)},grid:chartGrid({left:56,right:42,top:18,bottom:56}),xAxis:{type:'category',data:names,axisLabel:{rotate:30,fontSize:9,color:theme.text},...axis},yAxis:[{type:'value',name:'Loss',...axis},{type:'value',name:'Acc %',max:100,...axis}],series:[{name:'Loss',type:'bar',data:losses,itemStyle:{color:lossColor,borderRadius:[6,6,0,0]},barWidth:'36%'},{name:'Accuracy %',type:'bar',yAxisIndex:1,data:accs,itemStyle:{color:accColor,borderRadius:[6,6,0,0]},barWidth:'36%'}]});
  }

  // Parallel Coordinates - async load
  if(parallelRef.value){
    const c=echarts.init(parallelRef.value);
    try{
      const d=await analysisApi.hyperparams();
      if(d.data.code===200&&d.data.data?.data?.length){
        const dims=d.data.data.dimensions||[];
        const rows=d.data.data.data||[];
        c.setOption({
          backgroundColor:'transparent',
          tooltip:chartTooltip(theme),
          parallelAxis: dims.map((dim:string)=>({dim, name:dim, nameTextStyle:{fontSize:10,color:theme.text},axisLine:{lineStyle:{color:theme.border}},axisTick:{show:false},axisLabel:{color:theme.text,fontSize:10},splitLine:{lineStyle:{color:theme.border,opacity:.36}}})),
          parallel: { left:'5%',right:'5%',bottom:'10%',top:'15%',parallelAxisDefault:{nameLocation:'end',nameGap:12}},
          series: [{ type:'parallel', lineStyle:{width:2,opacity:.72,color:theme.primary},
            data: rows.map((r:any)=>dims.map((dim:string)=>r[dim]??0))
          }]
        });
      } else { c.dispose(); }
    } catch {}
  }
};

onMounted(async()=>{
  await fetchAll();
  fetchUploadedRuns();
  if(jobs.value.length>0){selectedJobId.value=jobs.value[0].id;fetchCurve();}
  poll=setInterval(async()=>{await fetchAll();if(currentJob.value){const j=jobs.value.find(x=>x.id===selectedJobId.value);if(j)currentJob.value=j;}nextTick(initCharts);},8000);
});

onUnmounted(()=>{if(poll)clearInterval(poll);});
</script>

<style scoped>
.analysis-page{
  --analysis-glass-bg:
    linear-gradient(135deg, rgba(var(--glass-tint-rgb), .46), rgba(var(--panel-rgb), .18));
  --analysis-card-bg:
    linear-gradient(145deg, rgba(var(--glass-tint-rgb), .38), rgba(var(--panel-rgb), .12));
  padding:24px;
  max-width:1400px;
  margin:0 auto;
}
.page-header h2{font-size:22px;font-weight:800;color:var(--text-primary);margin:0 0 4px}
.page-header p{font-size:13px;color:var(--text-secondary);margin:0 0 20px}
.control-bar{
  display:flex;
  align-items:center;
  justify-content:space-between;
  gap:16px;
  margin-bottom:20px;
  padding:16px 20px;
  background:var(--analysis-glass-bg);
  border:1px solid rgba(var(--primary-rgb), .14);
  border-radius:18px;
  box-shadow:var(--shadow-glass);
  backdrop-filter:blur(var(--glass-blur));
  flex-wrap:wrap;
  transition:
    border-color var(--motion-hover) var(--ease-liquid),
    box-shadow var(--motion-medium) var(--ease-smooth),
    transform var(--motion-hover) var(--ease-liquid);
}
.control-left{display:flex;align-items:center;gap:12px;flex-wrap:wrap}
.control-label{font-size:13px;font-weight:700;color:var(--text-secondary)}
.model-select{width:320px;max-width:100%}
.metric-card,
.chart-card,
.ai-insight-card{
  background:var(--analysis-card-bg) !important;
  border:1px solid rgba(var(--primary-rgb), .12) !important;
  border-radius:18px !important;
  box-shadow:var(--shadow-soft);
  backdrop-filter:blur(calc(var(--glass-blur) * .8));
  overflow:hidden;
  transition:
    border-color var(--motion-hover) var(--ease-liquid),
    box-shadow var(--motion-medium) var(--ease-smooth),
    transform var(--motion-hover) var(--ease-liquid),
    background var(--motion-medium) var(--ease-smooth);
}
.metric-card{text-align:center}
.metric-card :deep(.el-card__body){padding:18px 14px}
.chart-card :deep(.el-card__header),
.ai-insight-card :deep(.el-card__header){
  background:rgba(var(--glass-tint-rgb), .2);
  border-bottom:1px solid rgba(var(--primary-rgb), .1);
}
.chart-card :deep(.el-card__body),
.ai-insight-card :deep(.el-card__body){background:transparent}
.metric-value{font-size:28px;font-weight:900;line-height:1}
.metric-label{font-size:11px;color:var(--text-secondary);margin-top:6px;text-transform:uppercase;letter-spacing:0}
.ai-insight-card{border-color:rgba(var(--primary-rgb), .24) !important}
.ai-header{font-size:13px;font-weight:800;color:var(--primary-color)}
.ai-insight-body{font-size:14px;line-height:1.8;color:var(--text-primary)}
.ai-insight-body :deep(p){margin:0 0 8px}
.ai-insight-body :deep(li){margin-bottom:4px}
.progress-grid{display:flex;flex-direction:column;gap:12px}
.progress-item{
  padding:10px 12px;
  border-radius:14px;
  border:1px solid rgba(var(--primary-rgb), .1);
  background:rgba(var(--glass-tint-rgb), .16);
  cursor:pointer;
  transition:
    border-color var(--motion-hover) var(--ease-liquid),
    background var(--motion-medium) var(--ease-smooth),
    transform var(--motion-hover) var(--ease-liquid);
}
.progress-item.active{
  border-color:rgba(var(--primary-rgb), .38);
  background:rgba(var(--primary-rgb), .09);
}
.prog-header{display:flex;justify-content:space-between;margin-bottom:4px;gap:10px}
.prog-name{font-size:13px;font-weight:700;color:var(--text-primary)}
.prog-epoch{font-size:11px;color:var(--text-secondary);white-space:nowrap}
.prog-meta{display:flex;gap:16px;margin-top:6px;font-size:11px;color:var(--text-secondary);flex-wrap:wrap}
.chart-title{font-size:13px;font-weight:800;color:var(--text-primary)}
.chart-subtitle{font-size:11px;color:var(--text-secondary);font-weight:600}
.chart-desc{
  font-size:11px;
  color:var(--text-muted);
  margin-bottom:10px;
  line-height:1.6;
  padding:8px 10px;
  background:rgba(var(--glass-tint-rgb), .16);
  border:1px solid rgba(var(--primary-rgb), .08);
  border-radius:10px;
}
.chart-box{width:100%;height:320px}
.mt-4{margin-top:16px}
.mb-4{margin-bottom:16px}

@media (hover:hover) and (pointer:fine){
  .control-bar:hover,
  .chart-card:hover,
  .metric-card:hover,
  .ai-insight-card:hover{
    border-color:rgba(var(--primary-rgb), .24) !important;
    box-shadow:var(--shadow-hover);
    transform:translate3d(0,-2px,0);
  }
  .progress-item:hover{
    border-color:rgba(var(--primary-rgb), .32);
    background:rgba(var(--primary-rgb), .08);
    transform:translate3d(0,-1px,0);
  }
}

@media (max-width:768px){
  .analysis-page{padding:16px}
  .control-bar{align-items:flex-start}
  .model-select{width:100%}
}

@media (prefers-reduced-motion:reduce){
  .control-bar,
  .chart-card,
  .metric-card,
  .ai-insight-card,
  .progress-item{transition:none}
}
</style>
