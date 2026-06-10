<template>
  <div class="deep-logo" :style="{ '--logo-scale': scale || 1 }">
    <span class="logo-mark">
      <span></span>
    </span>
    <span class="logo-word">
      <strong>Deep</strong>
      <em>Insight</em>
    </span>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  scale?: number
}>()
</script>

<style scoped>
.deep-logo {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 3px 0 3px 2px;
  transform-origin: left center;
  transform: scale(var(--logo-scale, 1));
  color: var(--text-primary);
  transition: transform 260ms cubic-bezier(0.2, 0.9, 0.2, 1);
  will-change: transform;
}

.deep-logo:hover {
  transform: translateY(-2px) scale(calc((var(--logo-scale, 1)) * 1.02));
}

.logo-mark {
  position: relative;
  flex: 0 0 auto;
  width: 38px;
  height: 38px;
  border-radius: 12px;
  border: 1px solid rgba(var(--primary-rgb), 0.42);
  background:
    radial-gradient(circle at 24% 18%, rgba(255,255,255,0.78), transparent 18%),
    linear-gradient(135deg, rgba(var(--primary-rgb), 0.24), transparent 52%),
    var(--surface-2);
  box-shadow:
    0 10px 30px rgba(var(--primary-rgb), 0.16),
    inset 0 1px 0 rgba(255, 255, 255, 0.2);
  overflow: hidden;
  transform-origin: center;
  animation: logo-breathe 4.6s ease-in-out infinite;
  transition:
    border-radius 320ms cubic-bezier(0.2, 0.9, 0.2, 1),
    box-shadow 320ms ease,
    background-position 320ms ease;
}

.logo-mark::selection,
.logo-word::selection {
  background: transparent;
}

.logo-mark::before,
.logo-mark::after,
.logo-mark span {
  content: "";
  position: absolute;
  background: var(--primary-color);
  border-radius: 99px;
  box-shadow: 0 0 16px var(--primary-glow);
}

.logo-mark::before {
  width: 17px;
  height: 2px;
  left: 9px;
  top: 13px;
}

.logo-mark::after {
  width: 2px;
  height: 17px;
  right: 11px;
  bottom: 8px;
}

.logo-mark span {
  width: 7px;
  height: 7px;
  left: 10px;
  bottom: 10px;
  animation: logo-node-pulse 2.4s ease-in-out infinite;
}

.logo-mark span::before {
  content: "";
  position: absolute;
  inset: -7px;
  border-radius: inherit;
  border: 1px solid rgba(255,255,255,0.78);
  opacity: 0;
  transform: scale(0.62);
}

.logo-mark::before {
  animation: logo-scan-x 3.2s ease-in-out infinite;
}

.logo-mark::after {
  animation: logo-scan-y 3.2s ease-in-out infinite;
}

.logo-word {
  display: inline-flex;
  align-items: baseline;
  gap: 2px;
  font-weight: var(--font-weight-title);
  letter-spacing: 0;
  line-height: 1;
}

.logo-word strong,
.logo-word em {
  font-style: normal;
  font-size: 22px;
}

.logo-word em {
  color: var(--primary-color);
}

.deep-logo:hover .logo-mark {
  animation-duration: 1.4s;
  border-radius: 13px;
  box-shadow:
    0 18px 42px rgba(var(--primary-rgb), 0.34),
    0 0 0 1px rgba(var(--primary-rgb), 0.28),
    inset 0 1px 0 rgba(255,255,255,0.42);
}

.deep-logo:hover .logo-mark::before {
  animation: logo-hover-x 760ms cubic-bezier(0.2, 0.9, 0.2, 1) both;
}

.deep-logo:hover .logo-mark::after {
  animation: logo-hover-y 760ms cubic-bezier(0.2, 0.9, 0.2, 1) both;
}

.deep-logo:hover .logo-mark span {
  animation: logo-hover-node 760ms cubic-bezier(0.2, 0.9, 0.2, 1) both;
}

.deep-logo:hover .logo-mark span::before {
  animation: logo-node-ring 760ms ease both;
}

@keyframes logo-breathe {
  0%, 100% { transform: translateY(0) scale(1); }
  50% { transform: translateY(-1px) scale(1.035); }
}

@keyframes logo-node-pulse {
  0%, 100% { opacity: 0.78; transform: scale(0.9); }
  50% { opacity: 1; transform: scale(1.18); }
}

@keyframes logo-scan-x {
  0%, 100% { transform: translateX(0); opacity: 0.78; }
  50% { transform: translateX(3px); opacity: 1; }
}

@keyframes logo-scan-y {
  0%, 100% { transform: translateY(0); opacity: 0.78; }
  50% { transform: translateY(-3px); opacity: 1; }
}

@keyframes logo-hover-x {
  0% { transform: translateX(0) scaleX(1); }
  38% { transform: translateX(5px) scaleX(1.24); }
  68% { transform: translateX(2px) scaleX(0.92); }
  100% { transform: translateX(3px) scaleX(1.08); }
}

@keyframes logo-hover-y {
  0% { transform: translateY(0) scaleY(1); }
  38% { transform: translateY(-5px) scaleY(1.2); }
  68% { transform: translateY(-2px) scaleY(0.94); }
  100% { transform: translateY(-3px) scaleY(1.08); }
}

@keyframes logo-hover-node {
  0% { transform: translate(0, 0) scale(1); }
  36% { transform: translate(4px, -4px) scale(1.34); }
  66% { transform: translate(1px, -2px) scale(0.96); }
  100% { transform: translate(2px, -2px) scale(1.16); }
}

@keyframes logo-node-ring {
  0% { opacity: 0; transform: scale(0.62); }
  42% { opacity: 0.78; transform: scale(1.2); }
  100% { opacity: 0; transform: scale(1.9); }
}

@media (prefers-reduced-motion: reduce) {
  .logo-mark,
  .logo-mark::before,
  .logo-mark::after,
  .logo-mark span {
    animation: none;
  }
}
</style>
