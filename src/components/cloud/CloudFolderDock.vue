<template>
  <button
    class="cloud-folder-dock"
    type="button"
    :aria-label="copy.aria"
    :title="copy.title"
    @click="openCloudCenter"
  >
    <span class="folder-icon" :style="folderVars" aria-hidden="true">
      <i class="paper paper-1"><b>{{ paperLabels.ai }}</b></i>
      <i class="paper paper-2"><b>{{ paperLabels.viz }}</b></i>
      <i class="paper paper-3"><b>{{ paperLabels.data }}</b></i>
      <span class="folder-back"></span>
      <span class="folder-front"></span>
      <span class="folder-front right"></span>
    </span>

    <span class="folder-shine" aria-hidden="true"></span>
  </button>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ROUTES } from '@/constants'

const router = useRouter()
const { locale } = useI18n()
const isZh = computed(() => locale.value.startsWith('zh'))
const copy = computed(() => ({
  title: isZh.value ? '数据中心' : 'Data Center',
  aria: isZh.value ? '打开数据中心' : 'Open data center',
}))
const paperLabels = computed(() => isZh.value
  ? { ai: '问答', viz: '图表', data: '数据' }
  : { ai: 'AI', viz: 'VIZ', data: 'DATA' })
const folderVars = computed(() => ({
  '--folder-color': 'var(--primary-color)',
  '--folder-back-color': 'color-mix(in srgb, var(--primary-color) 72%, #0f172a)',
}))

function openCloudCenter() {
  void router.push(ROUTES.DATA)
}
</script>

<style scoped>
.cloud-folder-dock {
  position: fixed;
  top: 118px;
  right: 22px;
  z-index: 260;
  display: grid;
  place-items: center;
  width: 70px;
  height: 64px;
  padding: 0;
  border: 1px solid var(--border-color);
  border-radius: 20px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.12), rgba(255, 255, 255, 0.026) 44%, rgba(0, 0, 0, 0.14)),
    rgba(var(--glass-bg-rgb), 0.5);
  color: var(--text-primary);
  cursor: pointer;
  overflow: visible;
  isolation: isolate;
  box-shadow:
    0 18px 48px rgba(0, 0, 0, 0.26),
    inset 0 1px 0 rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(22px) saturate(135%);
  -webkit-backdrop-filter: blur(22px) saturate(135%);
  transition:
    transform var(--motion-medium) var(--ease-liquid),
    border-color var(--motion-quick) ease,
    background var(--motion-medium) var(--ease-smooth),
    box-shadow var(--motion-medium) var(--ease-smooth);
}

.cloud-folder-dock::after {
  content: "";
  position: absolute;
  inset: -12px;
  z-index: 0;
  border-radius: 28px;
  pointer-events: none;
}

.cloud-folder-dock:hover,
.cloud-folder-dock:focus-visible {
  transform: translateY(-1px) scale(1.008);
  border-color: color-mix(in srgb, var(--primary-color) 36%, var(--border-color));
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.15), rgba(255, 255, 255, 0.034) 44%, rgba(0, 0, 0, 0.16)),
    rgba(var(--glass-bg-rgb), 0.6);
  box-shadow:
    0 22px 56px rgba(0, 0, 0, 0.32),
    0 0 0 1px rgba(var(--primary-rgb), 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.16);
}

.cloud-folder-dock:active {
  transform: translateY(0) scale(0.985);
}

.folder-icon {
  --folder-color: var(--primary-color);
  --folder-back-color: color-mix(in srgb, var(--primary-color) 72%, #0f172a);
  position: relative;
  z-index: 1;
  width: 58px;
  height: 46px;
  transition: transform 260ms var(--ease-smooth);
}

.cloud-folder-dock:hover .folder-icon,
.cloud-folder-dock:focus-visible .folder-icon {
  transform: translateY(-1px);
}

.folder-back {
  position: absolute;
  left: 0;
  bottom: 0;
  width: 58px;
  height: 43px;
  border-radius: 0 9px 9px 9px;
  background: var(--folder-back-color);
}

.folder-back::after {
  content: "";
  position: absolute;
  left: 0;
  bottom: 98%;
  width: 20px;
  height: 7px;
  border-radius: 5px 5px 0 0;
  background: var(--folder-back-color);
}

.paper {
  position: absolute;
  z-index: 2;
  bottom: 6px;
  left: 50%;
  width: 40px;
  height: 34px;
  display: grid;
  place-items: start center;
  padding-top: 5px;
  border-radius: 7px;
  background: #f8fafc;
  color: #0f172a;
  font-size: 6px;
  font-style: normal;
  font-weight: var(--font-weight-title);
  transform: translate(-50%, 8px);
  transition: transform 280ms var(--ease-smooth), height 280ms var(--ease-smooth);
}

.paper-2 {
  width: 45px;
  height: 31px;
  background: #eef2ff;
}

.paper-3 {
  width: 50px;
  height: 28px;
  background: #ffffff;
}

.cloud-folder-dock:hover .paper-1,
.cloud-folder-dock:focus-visible .paper-1 {
  transform: translate(-92%, -13px) rotate(-8deg);
}

.cloud-folder-dock:hover .paper-2,
.cloud-folder-dock:focus-visible .paper-2 {
  height: 35px;
  transform: translate(-4%, -15px) rotate(8deg);
}

.cloud-folder-dock:hover .paper-3,
.cloud-folder-dock:focus-visible .paper-3 {
  height: 36px;
  transform: translate(-50%, -22px) rotate(3deg);
}

.folder-front {
  position: absolute;
  z-index: 3;
  inset: 0;
  border-radius: 5px 9px 9px 9px;
  background: var(--folder-color);
  transform-origin: bottom;
  transition: transform 280ms var(--ease-smooth);
}

.folder-front.right {
  clip-path: polygon(50% 0, 100% 0, 100% 100%, 50% 100%);
}

.cloud-folder-dock:hover .folder-front,
.cloud-folder-dock:focus-visible .folder-front {
  transform: skew(10deg) scaleY(0.72);
}

.cloud-folder-dock:hover .folder-front.right,
.cloud-folder-dock:focus-visible .folder-front.right {
  transform: skew(-10deg) scaleY(0.72);
}

.folder-shine {
  position: absolute;
  z-index: 2;
  inset: 8px;
  border-radius: 16px;
  pointer-events: none;
  background:
    linear-gradient(115deg, transparent 10%, rgba(255, 255, 255, 0.24) 42%, transparent 58%) top left / 0% 100% no-repeat;
  opacity: 0;
  transition: opacity 220ms ease, background-size 420ms var(--ease-smooth);
}

.cloud-folder-dock:hover .folder-shine,
.cloud-folder-dock:focus-visible .folder-shine {
  opacity: 1;
  background-size: 160% 100%;
}

@media (prefers-reduced-motion: reduce) {
  .cloud-folder-dock,
  .folder-icon,
  .paper,
  .folder-front,
  .folder-shine {
    transition: none;
    transform: none !important;
  }
}

@media (max-width: 760px) {
  .cloud-folder-dock {
    top: auto;
    right: 12px;
    bottom: 104px;
    width: 64px;
    height: 60px;
    border-radius: 18px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .cloud-folder-dock,
  .folder-icon,
  .paper,
  .folder-front {
    transition-duration: 1ms;
  }
}
</style>
