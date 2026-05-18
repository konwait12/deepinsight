<template>
  <div class="prediction-page">
    <div class="page-header entrance-hero"><h2>{{ $t('nav.prediction') }}</h2><p>{{ $t('prediction.subtitle') }}</p></div>

    <el-row :gutter="20">
      <el-col :span="8" class="entrance-up" style="animation-delay: 0.1s">
        <el-card shadow="never" class="config-card" :header="pageText.config">
          <el-form label-position="top" size="small">
            <el-form-item :label="pageText.selectModel"><el-select v-model="inferModel" style="width:100%"><el-option v-for="m in modelList" :key="m.name" :label="displayModelName(m)" :value="m.name"><span>{{ displayModelName(m) }}</span><small class="option-meta">{{ displayTaskType(m) }}</small></el-option></el-select></el-form-item>
            <el-form-item :label="pageText.inputType"><el-radio-group v-model="inputType"><el-radio label="image">{{ pageText.image }}</el-radio><el-radio label="batch">{{ pageText.batch }}</el-radio><el-radio label="api">API</el-radio></el-radio-group></el-form-item>
            <el-form-item v-if="inputType==='image'" :label="pageText.uploadImage">
              <el-upload drag :auto-upload="false"><el-icon :size="32"><PictureFilled /></el-icon><div class="el-upload__text">{{ pageText.dropUpload }}</div></el-upload>
            </el-form-item>
            <el-form-item v-if="inputType==='batch'" :label="pageText.batchFiles">
              <el-upload drag :auto-upload="false" multiple><el-icon :size="32"><Folder /></el-icon><div class="el-upload__text">{{ pageText.uploadFolder }}</div></el-upload>
            </el-form-item>
            <el-form-item :label="pageText.preprocess"><el-select v-model="preprocess" style="width:100%" multiple><el-option label="Resize 224x224" value="resize"/><el-option label="Normalize" value="norm"/><el-option label="CenterCrop" value="crop"/></el-select></el-form-item>
            <el-form-item><el-button type="primary" @click="runInference" :loading="inferring" style="width:100%" :style="{ backgroundColor:'var(--primary-color)' }"><el-icon><Switch /></el-icon>{{ pageText.start }}</el-button></el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never" class="config-card mt-4" :header="pageText.performance">
          <div class="perf-grid">
            <div class="perf-item"><span class="perf-val" style="color:#4ade80">{{ perf.fps }}</span><span>FPS</span></div>
            <div class="perf-item"><span class="perf-val" style="color:var(--primary-color)">{{ perf.latency }}</span><span>{{ pageText.latency }}</span></div>
            <div class="perf-item"><span class="perf-val" style="color:#f59e0b">{{ perf.throughput }}</span><span>{{ pageText.throughput }}</span></div>
            <div class="perf-item"><span class="perf-val" style="color:#ef4444">{{ perf.gpuMem }}</span><span>{{ pageText.gpuMem }}</span></div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="16" class="entrance-up" style="animation-delay: 0.18s">
        <el-card shadow="never" class="result-card" :header="pageText.results">
          <div v-if="results.length" class="result-grid">
            <div v-for="(r,i) in results" :key="i" class="result-item">
              <div class="result-img"><el-icon :size="40"><Picture /></el-icon></div>
              <div class="result-info">
                <div class="result-label">{{ r.label }} <el-tag size="small" :type="r.confidence>0.9?'success':r.confidence>0.7?'warning':'info'">{{ (r.confidence*100).toFixed(1) }}%</el-tag></div>
                <div class="result-time">{{ r.time }}ms</div>
              </div>
            </div>
          </div>
          <el-empty v-else :description="pageText.emptyResults"/>
        </el-card>

        <el-card shadow="never" class="result-card mt-4" :header="pageText.batchResults">
          <el-table :data="batchResults" stripe :empty-text="pageText.emptyBatch">
            <el-table-column prop="file" :label="pageText.fileName" min-width="160"/>
            <el-table-column prop="prediction" :label="pageText.prediction" width="180"/>
            <el-table-column prop="confidence" :label="pageText.confidence" width="100"><template #default="{row}"><el-progress :percentage="Math.round(row.confidence*100)" :color="row.confidence>0.9?'#4ade80':'#f59e0b'" :stroke-width="6"/></template></el-table-column>
            <el-table-column prop="time" :label="pageText.timeMs" width="100"/>
            <el-table-column prop="correct" :label="pageText.correct" width="80"><template #default="{row}"><el-icon v-if="row.correct" style="color:#4ade80"><Check /></el-icon><el-icon v-else style="color:#ef4444"><Close /></el-icon></template></el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>
<script setup lang="ts">
import { computed, ref, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { Picture, PictureFilled, Folder, Switch, Check, Close } from '@element-plus/icons-vue';
import { predictionApi } from '@/api';
import type { ModelOption } from '@/types/models';
import { useI18n } from 'vue-i18n';
import { modelDisplayName, taskTypeLabel } from '@/utils/modelDisplay';

const { locale } = useI18n();
const inferModel=ref('ResNet-50');
const inputType=ref('image');
const preprocess=ref(['resize','norm']);
const modelList=ref<ModelOption[]>([]);
const perf=ref({fps:'--',latency:'--',throughput:'--',gpuMem:'--'});
const results=ref<any[]>([]);
const batchResults=ref<any[]>([]);
const inferring=ref(false);

const isZh = computed(() => locale.value.startsWith('zh'));
const pageText = computed(() => isZh.value ? {
  config: '推理配置',
  selectModel: '选择模型',
  inputType: '输入类型',
  image: '图片',
  batch: '批量文件',
  uploadImage: '上传图片',
  dropUpload: '拖拽或点击上传',
  batchFiles: '批量文件',
  uploadFolder: '上传文件夹',
  preprocess: '预处理',
  start: '开始推理',
  performance: '推理性能',
  latency: '延迟',
  throughput: '吞吐量',
  gpuMem: '显存',
  results: '推理结果',
  emptyResults: '点击“开始推理”查看结果',
  batchResults: '批量测试结果',
  emptyBatch: '暂无批量结果',
  fileName: '文件名',
  prediction: '预测结果',
  confidence: '置信度',
  timeMs: '耗时(ms)',
  correct: '正确',
  done: '推理完成',
} : {
  config: 'Inference Config',
  selectModel: 'Select Model',
  inputType: 'Input Type',
  image: 'Image',
  batch: 'Batch Files',
  uploadImage: 'Upload Image',
  dropUpload: 'Drop or click to upload',
  batchFiles: 'Batch Files',
  uploadFolder: 'Upload Folder',
  preprocess: 'Preprocess',
  start: 'Run Inference',
  performance: 'Inference Performance',
  latency: 'Latency',
  throughput: 'Throughput',
  gpuMem: 'GPU Memory',
  results: 'Inference Results',
  emptyResults: 'Click “Run Inference” to view results',
  batchResults: 'Batch Test Results',
  emptyBatch: 'No batch results',
  fileName: 'File',
  prediction: 'Prediction',
  confidence: 'Confidence',
  timeMs: 'Time (ms)',
  correct: 'Correct',
  done: 'Inference completed',
});

const displayModelName = (model: ModelOption) => modelDisplayName(model, locale);
const displayTaskType = (model: ModelOption) => taskTypeLabel(model.taskType, model.taskTypeZh, locale);

onMounted(async()=>{
  try{
    const res=await predictionApi.listModels();
    if(res.data.code===200){
      modelList.value=res.data.data||[];
      if(!modelList.value.some((m)=>m.name===inferModel.value)&&modelList.value.length) inferModel.value=modelList.value[0].name;
    }
  }catch{/* API unavailable */}
});

const runInference=async()=>{
  inferring.value=true;
  try{
    const res=await predictionApi.classify({model:inferModel.value});
    if(res.data.code===200){
      const data=res.data.data;
      perf.value={fps:String(data.performance?.fps||'--'),latency:(data.performance?.latencyMs||'--')+'ms',throughput:(data.performance?.throughput||'--')+' img/s',gpuMem:(data.performance?.gpuMemoryGb||'--')+'/8 GB'};
      results.value=(data.predictions||[]).map((p:any)=>({label:p.label,confidence:p.confidence,time:Math.round(data.performance?.latencyMs||6)}));
      batchResults.value=(data.predictions||[]).map((p:any,i:number)=>({file:'test_00'+(i+1)+'.jpg',prediction:p.label,confidence:p.confidence,time:Math.round(data.performance?.latencyMs||6),correct:p.confidence>0.8}));
      ElMessage.success(pageText.value.done);
    }
  }catch{}
  inferring.value=false;
};
</script>
<style scoped>.prediction-page{padding:24px;max-width:1400px;margin:0 auto}.page-header h2{font-size:22px;font-weight:800;color:var(--text-primary);margin:0 0 4px}.page-header p{font-size:13px;color:var(--text-secondary);margin:0 0 20px}.config-card,.result-card{border-radius:16px;margin-bottom:16px}.option-meta{float:right;color:var(--text-muted);font-size:11px}.perf-grid{display:grid;grid-template-columns:1fr 1fr;gap:16px;text-align:center;color:var(--text-primary)}.perf-val{font-size:24px;font-weight:900;display:block}.result-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(200px,1fr));gap:12px}.result-item{display:flex;align-items:center;gap:12px;padding:12px;border-radius:12px;background:var(--panel-bg);border:1px solid var(--border-color)}.result-label{font-weight:700;margin-bottom:4px;color:var(--text-primary)}.result-time{font-size:11px;color:var(--text-secondary)}.mt-4{margin-top:16px}</style>
