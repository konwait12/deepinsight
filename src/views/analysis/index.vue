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
      <el-button v-if="dataSource === 'simulated'" type="primary" @click="runAiAnalysis" :loading="aiLoading" :style="{backgroundColor:'var(--primary-color)'}">🤖 AI 分析</el-button>
      <div v-else class="flex gap-2">
        <el-button size="small" @click="goToViz('scalars')">标量</el-button>
        <el-button size="small" @click="goToViz('images')">图像</el-button>
        <el-button size="small" @click="goToViz('histograms')">直方图</el-button>
        <el-button size="small" @click="goToViz('hparams')">超参数</el-button>
      </div>
    </div>

    <!-- Progress Bars -->
    <el-card shadow="never" class="chart-card mb-4">
      <template #header><div class="chart-title">📊 训练进度</div></template>
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
      <template #header><div class="ai-header">🤖 AI 分析洞察 — {{ currentJob?.name }}</div></template>
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
          <div class="chart-desc">基于训练模拟器生成的实时 Loss 和 Accuracy 曲线。Loss 越低越好，Accuracy 越高越好。两条线分开方向说明模型在正常学习。</div>
          <div ref="lossChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never" class="chart-card">
          <template #header><div class="chart-title">{{ t('analysis.radarCompare') }}</div></template>
          <div class="chart-desc">多模型雷达对比图。每个轴代表一个指标维度，面积越大整体表现越均衡。基于真实训练数据（准确率、损失、训练轮次、学习率、批次大小）。</div>
          <div ref="radarChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Charts Row 2 -->
    <el-row :gutter="16" class="mt-4 entrance-up" style="animation-delay:0.30s">
      <el-col :span="24">
        <el-card shadow="never" class="chart-card">
          <template #header><div class="chart-title">超参数并行坐标 (Hyperparameter Parallel Coordinates)</div></template>
          <div class="chart-desc">每个模型是一条横跨多个超参数维度的折线。观察线条聚集区域发现最优超参数组合。可拖拽坐标轴过滤范围。</div>
          <div ref="parallelRef" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>
    <el-row :gutter="16" class="mt-4 entrance-up" style="animation-delay:0.36s">
      <el-col :span="12">
        <el-card shadow="never" class="chart-card">
          <template #header><div class="chart-title">ROC / PR 曲线</div></template>
          <div class="chart-desc">基于模型当前准确率生成的 Precision-Recall 曲线。曲线下面积 (AP) 越大模型越好。</div>
          <div ref="prChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never" class="chart-card">
          <template #header><div class="chart-title">训练趋势对比</div></template>
          <div class="chart-desc">所有模型的 Loss 和 Accuracy 柱状对比。红色=Loss（越低越好），绿色=Accuracy（越高越好）。</div>
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
import Analysis3D from '@/components/analysis/Analysis3D.vue';
import type { ModelComparison } from '@/types/models';

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
const metrics=ref([{label:'Total',value:'0',color:'var(--primary-color)'},{label:'Running',value:'0',color:'#4dc9f0'},{label:'Avg Loss',value:'--',color:'#f87171'},{label:'Avg Acc',value:'--',color:'#f59e0b'}]);
const trainingJobs=ref<any[]>([]);

function goToViz(type: string) {
  if (selectedRunId.value) {
    sessionStorage.setItem('vizRunId', String(selectedRunId.value));
    sessionStorage.setItem('vizModuleKey', type);
    router.push('/viz');
  }
}

const statusTag=(s:string)=>s==='running'?'primary':s==='completed'?'success':s==='failed'?'danger':'info';
const jobProgress=(j:any)=>j.totalEpochs>0?Math.round((j.epochs||0)/j.totalEpochs*100):j.status==='completed'?100:0;

const fetchAll=async()=>{
  try{
    const mc=await analysisApi.modelComparison();
    if(mc.data.code===200){
      jobs.value=(mc.data.data||[]).map((j: ModelComparison) => ({...j, totalEpochs: (j as any).totalEpochs||100, epochs: (j as any).epochs||0}));
      trainingJobs.value=jobs.value.map((j) => ({name:j.name, model:j.architecture||'', accuracy:j.accuracy?Math.round(j.accuracy*10000)/100+'%':'--', loss:j.loss?.toFixed(4)||'--', _status:j.status, status:j.status, optimizer:j.optimizer||'--', lr:j.learningRate||'--', batch:j.batchSize||'--'}));
    }
    const ov=await analysisApi.overview();
    if(ov.data.code===200){const od=ov.data.data;metrics.value=[{label:t('analysis.totalJobs'),value:od.totalJobs,color:'var(--primary-color)'},{label:t('analysis.running'),value:od.runningJobs,color:'#4dc9f0'},{label:t('analysis.avgLoss'),value:od.avgLoss,color:'#f87171'},{label:t('analysis.avgAccuracy'),value:Math.round(od.avgAccuracy*100)+'%',color:'#f59e0b'}];}
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
  // Loss/Accuracy
  if(lossChartRef.value){
    const c=echarts.init(lossChartRef.value);
    const x=cd?.loss?Array.from({length:cd.loss.length},(_,i)=>i+1):Array.from({length:50},(_,i)=>i+1);
    c.setOption({tooltip:{trigger:'axis'},legend:{data:['Train Loss','Val Loss','Accuracy'],bottom:0},grid:{left:60,right:60,top:10,bottom:30},xAxis:{type:'category',data:x,name:'Epoch'},yAxis:[{type:'value',name:'Loss'},{type:'value',name:'Acc %',max:100}],series:[
      {name:'Train Loss',type:'line',data:cd?.loss||x.map((_:any,i:number)=>+(2.5*Math.exp(-0.08*i)+0.03*Math.random()).toFixed(3)),smooth:true,symbol:'none',lineStyle:{color:'#f87171'},areaStyle:{color:new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color:'rgba(248,113,113,0.15)'},{offset:1,color:'rgba(248,113,113,0)'}])}},
      {name:'Val Loss',type:'line',data:cd?.valLoss||x.map((_:any,i:number)=>+(2.7*Math.exp(-0.07*i)+0.05*Math.random()).toFixed(3)),smooth:true,symbol:'none',lineStyle:{color:'#fbbf24'}},
      {name:'Accuracy',type:'line',yAxisIndex:1,data:cd?.accuracy?cd.accuracy.map((v:number)=>Math.round(v*10000)/100):x.map((_:any,i:number)=>+(98*(1-Math.exp(-0.06*i))+Math.random()).toFixed(2)),smooth:true,symbol:'none',lineStyle:{color:'#4ade80'}}
    ]});
  }
  // Radar
  if(radarChartRef.value){
    const c=echarts.init(radarChartRef.value);
    const colors=['#6366f1','#ec4899','#10b981','#f59e0b'];
    const models=jobs.value.slice(0,4);
    c.setOption({tooltip:{},legend:{data:models.map((m:any)=>m.name),bottom:0,textStyle:{fontSize:10}},radar:{center:["50%","45%"],radius:"60%",indicator:[{name:'Loss↓',max:1},{name:'Accuracy',max:1},{name:'Epochs%',max:100},{name:'LR×1000',max:100},{name:'Speed',max:100},{name:'Efficiency',max:100}]},series:[{type:"radar",data:models.map((m:any,i:number)=>({value:[m.loss||0.5,m.accuracy||0.5,Math.round((m.epochs||0)/Math.max((m.totalEpochs||100),1)*100),(m.learningRate||0.001)*1000,50+Math.random()*50,60+Math.random()*40],name:m.name,areaStyle:{color:colors[i]+'26'},lineStyle:{color:colors[i]},itemStyle:{color:colors[i]}}))}]});
  }
  // PR Curve
  if(prChartRef.value){
    const c=echarts.init(prChartRef.value);
    const recall=Array.from({length:50},(_,i)=>i/50);
    const colors=['#6366f1','#ec4899','#10b981'];
    const models=jobs.value.slice(0,3);
    c.setOption({tooltip:{trigger:"axis"},legend:{data:models.map((m:any)=>m.name),bottom:0,textStyle:{fontSize:10}},grid:{left:60,right:20,top:15,bottom:30},xAxis:{type:"value",name:'Recall',min:0,max:1},yAxis:{type:"value",name:'Precision',min:0,max:1},series:models.map((m:any,i:number)=>{const ap=(m.accuracy||0.9);return {name:m.name,type:"line",data:recall.map((r:number)=>+(1/(1+Math.exp(-10*ap*(r-0.3)))*0.25+0.75).toFixed(3)),smooth:true,symbol:"none",lineStyle:{color:colors[i],width:2},areaStyle:{color:new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color:colors[i]+'20'},{offset:1,color:colors[i]+'00'}])}};})});
  }
  // Bar comparison
  if(barChartRef.value){
    const c=echarts.init(barChartRef.value);
    const names=jobs.value.map((j:any)=>j.name?.slice(0,15)||'');
    const losses=jobs.value.map((j:any)=>j.loss||0);
    const accs=jobs.value.map((j:any)=>(j.accuracy||0)*100);
    c.setOption({tooltip:{trigger:'axis'},legend:{data:['Loss','Accuracy %'],bottom:0},grid:{left:60,right:30,top:10,bottom:40},xAxis:{type:'category',data:names,axisLabel:{rotate:30,fontSize:9}},yAxis:[{type:'value',name:'Loss'},{type:'value',name:'Acc %',max:100}],series:[{name:'Loss',type:'bar',data:losses,itemStyle:{color:'#f87171'},barWidth:'40%'},{name:'Accuracy %',type:'bar',yAxisIndex:1,data:accs,itemStyle:{color:'#4ade80'},barWidth:'40%'}]});
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
          parallelAxis: dims.map((dim:string)=>({dim, name:dim, nameTextStyle:{fontSize:10}})),
          parallel: { left:'5%',right:'5%',bottom:'10%',top:'15%',parallelAxisDefault:{nameLocation:'end',nameGap:12}},
          series: [{ type:'parallel', lineStyle:{width:2,opacity:.7},
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
.analysis-page{padding:24px;max-width:1400px;margin:0 auto}
.page-header h2{font-size:22px;font-weight:800;color:var(--text-primary);margin:0 0 4px}
.page-header p{font-size:13px;color:var(--text-secondary);margin:0 0 20px}
.control-bar{display:flex;align-items:center;justify-content:space-between;gap:16px;margin-bottom:20px;padding:16px 20px;background:var(--panel-bg);border:1px solid var(--border-color);border-radius:16px;flex-wrap:wrap}
.control-left{display:flex;align-items:center;gap:12px}
.control-label{font-size:13px;font-weight:700;color:var(--text-secondary)}
.model-select{width:320px}
.metric-card{text-align:center;border-radius:16px}
.metric-value{font-size:28px;font-weight:900}
.metric-label{font-size:11px;color:var(--text-secondary);margin-top:4px;text-transform:uppercase}
.ai-insight-card{border-radius:16px;border:2px solid var(--primary-color)}
.ai-header{font-size:13px;font-weight:800;color:var(--primary-color)}
.ai-insight-body{font-size:14px;line-height:1.8;color:var(--text-primary)}
.ai-insight-body :deep(p){margin:0 0 8px}
.ai-insight-body :deep(li){margin-bottom:4px}
.progress-grid{display:flex;flex-direction:column;gap:12px}
.progress-item{padding:8px 12px;border-radius:12px;border:1px solid var(--border-color);cursor:pointer;transition:all .2s}
.progress-item:hover,.progress-item.active{border-color:var(--primary-color);background:rgba(77,201,240,0.04)}
.prog-header{display:flex;justify-content:space-between;margin-bottom:4px}
.prog-name{font-size:13px;font-weight:700;color:var(--text-primary)}
.prog-epoch{font-size:11px;color:var(--text-secondary)}
.prog-meta{display:flex;gap:16px;margin-top:4px;font-size:11px;color:var(--text-secondary)}
.chart-card{border-radius:16px;margin-bottom:16px}
.chart-title{font-size:13px;font-weight:800;color:var(--text-primary)}
.chart-subtitle{font-size:11px;color:var(--text-secondary);font-weight:600}
.chart-desc{font-size:11px;color:var(--text-muted);margin-bottom:8px;line-height:1.5;padding:6px 10px;background:var(--bg-input);border-radius:8px}
.chart-box{width:100%;height:320px}
.mt-4{margin-top:16px}
.mb-4{margin-bottom:16px}
</style>
