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
  border: 1px solid rgba(var(--primary-rgb), 0.1);
  overflow: hidden;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

/* ========== 磨砂透明度档位 ========== */
.glass-card--light {
  background: var(--surface-2);
  backdrop-filter: blur(12px) saturate(150%);
}
.glass-card--medium {
  background: rgba(var(--glass-bg-rgb), 0.72);
  backdrop-filter: blur(20px) saturate(180%);
}
.glass-card--heavy {
  background: rgba(var(--glass-bg-rgb), 0.86);
  backdrop-filter: blur(28px) saturate(200%);
}

/* ========== 边框光晕 ========== */
.glass-card--glow {
  box-shadow:
    0 8px 32px rgba(var(--primary-rgb), 0.08),
    inset 0 1px 0 rgba(var(--primary-rgb), 0.06);
}
.glass-card--glow:hover {
  box-shadow:
    0 12px 40px rgba(var(--primary-rgb), 0.14),
    inset 0 1px 0 rgba(var(--primary-rgb), 0.1);
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
    rgba(var(--primary-rgb), 0.3) 20%,
    rgba(var(--primary-rgb), 0.5) 50%,
    rgba(var(--primary-rgb), 0.3) 80%,
    transparent 100%
  );
  opacity: 0.6;
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
  background: rgba(var(--glass-bg-rgb), 0.74);
}
.light .glass-card--heavy {
  background: rgba(var(--glass-bg-rgb), 0.88);
}
</style>
