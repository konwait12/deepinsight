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
  opacity: 0.075;
  filter: blur(128px);
}

.light .ambient-gradient {
  opacity: 1;
  filter: blur(4px) saturate(112%);
  background:
    radial-gradient(920px circle at 13% 13%, rgba(var(--primary-rgb), 0.16), transparent 46%),
    radial-gradient(840px circle at 88% 17%, color-mix(in srgb, var(--accent-glow) 18%, transparent), transparent 48%),
    radial-gradient(760px circle at 50% 108%, rgba(125, 211, 252, 0.13), transparent 52%),
    linear-gradient(118deg, transparent 0%, rgba(255, 255, 255, 0.36) 44%, transparent 64%);
}

.light .ambient-grid {
  opacity: 0.48;
  background-image:
    linear-gradient(rgba(30, 80, 92, 0.045) 1px, transparent 1px),
    linear-gradient(90deg, rgba(30, 80, 92, 0.04) 1px, transparent 1px),
    linear-gradient(rgba(var(--primary-rgb), 0.035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(var(--primary-rgb), 0.03) 1px, transparent 1px);
  background-position: 0 0, 0 0, 18px 18px, 18px 18px;
  background-size: 72px 72px, 72px 72px, 18px 18px, 18px 18px;
  mask-image: linear-gradient(180deg, rgba(0,0,0,0.72), rgba(0,0,0,0.46) 58%, transparent 94%);
}

.light .ambient-grain {
  opacity: 0.18;
  background-image:
    radial-gradient(circle at 20% 20%, rgba(36, 71, 82, 0.2) 0 0.7px, transparent 1px),
    radial-gradient(circle at 80% 50%, color-mix(in srgb, var(--accent-glow) 18%, transparent) 0 0.8px, transparent 1px),
    linear-gradient(135deg, transparent, rgba(255, 255, 255, 0.24), transparent);
  background-size: 18px 18px, 27px 27px, 100% 100%;
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
