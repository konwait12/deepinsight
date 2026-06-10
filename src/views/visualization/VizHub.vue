<template>
  <main class="performance-page">
    <div class="performance-entry">
      <span>{{ copy.advanced }}</span>
      <button type="button" @click="router.push(ROUTES.ANALYSIS_WORKBENCH)">{{ copy.open }}</button>
    </div>
    <ModelVisualLab />
  </main>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import ModelVisualLab from '@/components/visualization/ModelVisualLab.vue'
import { ROUTES } from '@/constants'

const router = useRouter()
const { locale } = useI18n()
const copy = computed(() => locale.value.startsWith('en')
  ? {
      advanced: 'Chart Workspace',
      open: 'Open chart workspace',
    }
  : {
      advanced: '图表工作台',
      open: '进入图表工作台',
    })
</script>

<style scoped>
.performance-page {
  width: min(1480px, 100%);
  min-height: calc(100dvh - var(--header-height, 72px));
  margin: 0 auto;
  padding: 22px;
  color: var(--text-primary);
  --viz-hub-glass-bg:
    linear-gradient(180deg, rgba(255, 255, 255, 0.08), rgba(255, 255, 255, 0.018)),
    color-mix(in srgb, rgba(var(--glass-bg-rgb), var(--glass-opacity)) 78%, transparent);
}

.performance-entry {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  margin-bottom: 12px;
}

.performance-entry span {
  min-height: 36px;
  display: inline-flex;
  align-items: center;
  padding: 0 12px;
  border: 1px solid var(--border-color);
  border-radius: 999px;
  background: var(--viz-hub-glass-bg);
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: var(--font-weight-title);
}

.performance-entry button {
  min-height: 38px;
  padding: 0 15px;
  border: 1px solid rgba(var(--primary-rgb), 0.26);
  border-radius: 999px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.08), rgba(255, 255, 255, 0.018)),
    rgba(var(--primary-rgb), 0.1);
  color: var(--primary-color);
  font-size: 13px;
  font-weight: var(--font-weight-title);
  cursor: pointer;
  box-shadow: var(--shadow-soft);
  backdrop-filter: blur(var(--glass-blur)) saturate(var(--glass-saturate));
  -webkit-backdrop-filter: blur(var(--glass-blur)) saturate(var(--glass-saturate));
  transition:
    border-color var(--motion-hover) var(--ease-liquid),
    background var(--motion-medium) var(--ease-smooth),
    box-shadow var(--motion-medium) var(--ease-smooth),
    transform var(--motion-hover) var(--ease-liquid);
}

@media (hover: hover) and (pointer: fine) {
  .performance-entry button:hover {
    border-color: color-mix(in srgb, var(--primary-color) 42%, var(--border-color));
    background: rgba(var(--primary-rgb), 0.16);
    box-shadow: var(--shadow-ring);
    transform: translate3d(0, -1px, 0);
  }
}

@media (prefers-reduced-motion: reduce) {
  .performance-entry button {
    transition: none;
    transform: none !important;
  }
}

@media (max-width: 760px) {
  .performance-page {
    padding: 14px;
  }

  .performance-entry {
    justify-content: stretch;
  }

  .performance-entry span,
  .performance-entry button {
    flex: 1 1 auto;
    justify-content: center;
  }
}
</style>
