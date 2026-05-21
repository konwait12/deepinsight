<template>
  <div
    class="glass-card"
    :class="[
      `glass-card--${intensity}`,
      { 'glass-card--glow': borderGlow }
    ]"
    :style="glassStyle"
  >
    <!-- 顶部光条（模拟玻璃折射） -->
    <div v-if="intensity !== 'light'" class="glass-card__highlight" />

    <div class="glass-card__content">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';

const props = withDefaults(defineProps<{
  intensity?: 'light' | 'medium' | 'heavy';
  borderGlow?: boolean;
}>(), {
  intensity: 'medium',
  borderGlow: false,
});

const glassStyle = computed(() => ({
  '--gc-primary': 'var(--primary-color)',
  '--gc-primary-rgb': 'var(--primary-rgb)',
}));
</script>

<style scoped>
.glass-card {
  position: relative;
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-color);
  overflow: hidden;
  transition: background 0.28s var(--ease-spring), box-shadow 0.28s var(--ease-spring), border-color 0.28s var(--ease-spring), transform 0.28s var(--ease-spring);
}

/* ========== 磨砂透明度档位 ========== */
.glass-card--light {
  background: var(--surface-2);
  backdrop-filter: blur(10px) saturate(120%);
}
.glass-card--medium {
  background: rgba(var(--glass-bg-rgb), var(--glass-opacity));
  backdrop-filter: blur(14px) saturate(120%);
}
.glass-card--heavy {
  background: rgba(var(--glass-bg-rgb), 0.7);
  backdrop-filter: blur(18px) saturate(120%);
}

/* ========== 边框光晕 ========== */
.glass-card--glow {
  box-shadow:
    0 2px 8px rgba(var(--primary-rgb), 0.06),
    inset 0 1px 0 rgba(var(--primary-rgb), 0.04);
}
.glass-card--glow:hover {
  box-shadow:
    0 4px 16px rgba(var(--primary-rgb), 0.1),
    inset 0 1px 0 rgba(var(--primary-rgb), 0.06);
}

/* ========== 顶部折射高光条 ========== */
.glass-card__highlight {
  position: absolute;
  top: 0;
  left: 5%;
  right: 5%;
  height: 1px;
  background: linear-gradient(90deg,
    transparent 0%,
    rgba(var(--primary-rgb), 0.12) 20%,
    rgba(var(--primary-rgb), 0.2) 50%,
    rgba(var(--primary-rgb), 0.12) 80%,
    transparent 100%
  );
  opacity: 0.3;
  z-index: 2;
  pointer-events: none;
}

.glass-card__content {
  position: relative;
  z-index: 1;
}

/* ========== 明亮模式适配 ========== */
.light .glass-card--light {
  background: var(--surface-2);
}
.light .glass-card--medium {
  background: rgba(var(--glass-bg-rgb), var(--glass-opacity));
}
.light .glass-card--heavy {
  background: rgba(var(--glass-bg-rgb), 0.78);
}
</style>
