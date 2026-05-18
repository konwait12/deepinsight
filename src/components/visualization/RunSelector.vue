<template>
  <div class="flex items-center gap-4 flex-wrap">
    <label class="text-sm font-bold text-[var(--text-primary)]">训练任务:</label>
    <el-select v-model="selectedId" placeholder="选择训练任务" @change="(v: number | null) => selectJob(v)" clearable style="width: 300px">
      <el-option v-for="j in jobs" :key="j.id" :label="j.name" :value="j.id" />
    </el-select>
    <el-button size="small" @click="fetchJobs" :loading="loading">刷新</el-button>
  </div>
</template>

<script setup lang="ts">
import { useRunSelector } from '@/composables/useRunSelector'
const { jobs, selectedId, loading, fetchJobs, selectJob } = useRunSelector()
</script>
