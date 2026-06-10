<template>
  <main class="dataset-viz-page">
    <DatasetRealtimeVisualization />
  </main>
</template>

<script setup lang="ts">
import DatasetRealtimeVisualization from '@/components/data/DatasetRealtimeVisualization.vue'
</script>

<style scoped>
.dataset-viz-page {
  width: min(1480px, 100%);
  min-height: calc(100dvh - var(--header-height, 72px));
  margin: 0 auto;
  padding: 22px;
  color: var(--text-primary);
}

@media (max-width: 760px) {
  .dataset-viz-page {
    padding: 14px;
  }
}
</style>
