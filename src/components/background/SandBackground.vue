<template>
  <div v-if="route.name !== 'Landing'" class="ambient-bg" aria-hidden="true">
    <div class="ambient-gradient"></div>
    <div class="ambient-grid"></div>
    <div class="ambient-grain"></div>
    <div class="ambient-ribbon ribbon-a"></div>
    <div class="ambient-ribbon ribbon-b"></div>
  </div>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'

const route = useRoute()
</script>

<style scoped>
.ambient-bg {
  position: fixed;
  inset: 0;
  z-index: 0;
  overflow: hidden;
  pointer-events: none;
  background:
    radial-gradient(circle at 12% 8%, var(--bg-tint), transparent 34%),
    radial-gradient(circle at 88% 18%, color-mix(in srgb, var(--accent-glow) 18%, transparent), transparent 32%),
    var(--bg-color);
  transition: background 320ms var(--ease-smooth), opacity 260ms ease;
}

.ambient-gradient,
.ambient-grid,
.ambient-grain,
.ambient-ribbon {
  position: absolute;
  inset: 0;
}

.ambient-gradient {
  background:
    linear-gradient(115deg, transparent 0%, rgba(var(--primary-rgb), 0.10) 42%, transparent 62%),
    radial-gradient(circle at 50% 100%, rgba(var(--primary-rgb), 0.12), transparent 42%);
  filter: blur(4px);
  animation: slow-drift 18s ease-in-out infinite alternate;
}

.ambient-grid {
  background-image:
    linear-gradient(rgba(var(--primary-rgb), var(--sand-dot-opacity)) 1px, transparent 1px),
    linear-gradient(90deg, rgba(var(--primary-rgb), var(--sand-dot-opacity)) 1px, transparent 1px);
  background-size: 56px 56px;
  mask-image: linear-gradient(180deg, rgba(0,0,0,0.62), transparent 82%);
}

.ambient-grain {
  opacity: var(--sand-noise-opacity);
  background-image:
    radial-gradient(circle at 20% 20%, rgba(var(--primary-rgb), 0.72) 0 1px, transparent 1px),
    radial-gradient(circle at 80% 50%, color-mix(in srgb, var(--accent-glow) 62%, transparent) 0 1px, transparent 1px);
  background-size: 17px 17px, 23px 23px;
}

.ambient-ribbon {
  width: 58vw;
  height: 58vw;
  border-radius: 999px;
  filter: blur(120px);
  opacity: 0.18;
}

.ribbon-a {
  top: -22vw;
  left: -14vw;
  background: var(--primary-color);
  animation: ribbon-float 22s ease-in-out infinite;
}

.ribbon-b {
  right: -18vw;
  bottom: -24vw;
  background: var(--accent-glow);
  animation: ribbon-float 27s ease-in-out infinite reverse;
}

.light .ambient-ribbon {
  opacity: 0.038;
  filter: blur(150px);
}

.light .ambient-gradient {
  opacity: 0.22;
  filter: blur(5px) saturate(108%);
}

.light .ambient-grid {
  opacity: 0.26;
  background-size: 62px 62px;
}

.light .ambient-grain {
  opacity: calc(var(--sand-noise-opacity) * 0.36);
  background-size: 20px 20px, 28px 28px;
}

@keyframes slow-drift {
  from { transform: translate3d(-1.5%, -1%, 0) scale(1); }
  to { transform: translate3d(1.5%, 1%, 0) scale(1.04); }
}

@keyframes ribbon-float {
  0%, 100% { transform: translate3d(0, 0, 0) scale(1); }
  50% { transform: translate3d(5%, 4%, 0) scale(1.12); }
}
</style>
