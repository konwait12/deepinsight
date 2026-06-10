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
    radial-gradient(circle at 12% 8%, color-mix(in srgb, var(--bg-tint) 62%, transparent), transparent 36%),
    radial-gradient(circle at 88% 18%, color-mix(in srgb, var(--accent-glow) 8%, transparent), transparent 34%),
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
    linear-gradient(115deg, transparent 0%, rgba(255, 255, 255, 0.025) 44%, transparent 64%),
    radial-gradient(circle at 50% 100%, rgba(var(--primary-rgb), 0.055), transparent 44%);
  filter: blur(6px);
  animation: slow-drift 18s ease-in-out infinite alternate;
}

.ambient-grid {
  background-image:
    linear-gradient(rgba(255, 255, 255, calc(var(--sand-dot-opacity) * 0.58)) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, calc(var(--sand-dot-opacity) * 0.58)) 1px, transparent 1px);
  background-size: 56px 56px;
  mask-image: linear-gradient(180deg, rgba(0,0,0,0.62), transparent 82%);
}

.ambient-grain {
  opacity: var(--sand-noise-opacity);
  background-image:
    radial-gradient(circle at 20% 20%, rgba(255, 255, 255, 0.42) 0 1px, transparent 1px),
    radial-gradient(circle at 80% 50%, color-mix(in srgb, var(--accent-glow) 28%, transparent) 0 1px, transparent 1px);
  background-size: 17px 17px, 23px 23px;
}

.ambient-ribbon {
  width: 58vw;
  height: 58vw;
  border-radius: 999px;
  filter: blur(120px);
  opacity: 0.095;
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
  opacity: 0.045;
  filter: blur(142px);
}

.light .ambient-bg {
  background:
    linear-gradient(180deg, rgba(250, 253, 255, 0.96), rgba(232, 245, 242, 0.88) 52%, rgba(224, 239, 247, 0.82)),
    linear-gradient(118deg, rgba(66, 230, 164, 0.045), transparent 36%, rgba(96, 165, 250, 0.065)),
    var(--bg-color);
}

.light .ambient-gradient {
  opacity: 1;
  filter: blur(12px) saturate(112%);
  background:
    linear-gradient(105deg, transparent 0%, rgba(255, 255, 255, 0.5) 42%, transparent 62%),
    linear-gradient(158deg, rgba(var(--primary-rgb), 0.08), transparent 28%, rgba(96, 165, 250, 0.07) 78%, transparent),
    linear-gradient(26deg, transparent, rgba(255, 255, 255, 0.38), transparent 70%);
}

.light .ambient-grid {
  opacity: 0.72;
  background-image:
    linear-gradient(118deg, transparent 0 30%, rgba(255, 255, 255, 0.42) 44%, transparent 58%),
    linear-gradient(172deg, rgba(32, 82, 98, 0.035), transparent 28%, rgba(255, 255, 255, 0.26) 54%, transparent 78%),
    linear-gradient(88deg, transparent, rgba(66, 230, 164, 0.035), transparent 52%);
  background-position: 0 0, 0 0, 0 0;
  background-size: 100% 100%;
  mask-image: linear-gradient(180deg, rgba(0,0,0,0.78), rgba(0,0,0,0.5) 62%, transparent 96%);
}

.light .ambient-grain {
  opacity: 0.16;
  background-image:
    linear-gradient(90deg, rgba(255, 255, 255, 0.18), transparent 18%, rgba(34, 93, 108, 0.028) 48%, transparent 78%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.16), transparent 36%, rgba(66, 230, 164, 0.024));
  background-size: 100% 100%;
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
