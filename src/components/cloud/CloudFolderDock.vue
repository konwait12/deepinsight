<template>
  <button
    class="cloud-folder-dock"
    type="button"
    :aria-label="copy.aria"
    :title="copy.title"
    @click="openCloudCenter"
  >
    <span class="folder-icon" :style="folderVars" aria-hidden="true">
      <i class="paper paper-1"><b>AI</b></i>
      <i class="paper paper-2"><b>VIZ</b></i>
      <i class="paper paper-3"><b>DATA</b></i>
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

const router = useRouter()
const { locale } = useI18n()
const isZh = computed(() => locale.value.startsWith('zh'))
const copy = computed(() => ({
  title: isZh.value ? '云端中心' : 'Cloud Center',
  aria: isZh.value ? '打开云端中心' : 'Open cloud center',
}))
const folderVars = computed(() => ({
  '--folder-color': 'var(--primary-color)',
  '--folder-back-color': 'color-mix(in srgb, var(--primary-color) 72%, #0f172a)',
}))

function openCloudCenter() {
  void router.push('/cloud')
}
</script>

<style scoped>
.cloud-folder-dock {
  position: fixed;
  top: 118px;
  right: 22px;
  z-index: 88;
  display: grid;
  place-items: center;
  width: 70px;
  height: 64px;
  padding: 0;
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 20px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.12), rgba(255, 255, 255, 0.026) 44%, rgba(0, 0, 0, 0.14)),
    rgba(var(--glass-bg-rgb), 0.5);
  color: var(--text-primary);
  cursor: pointer;
  box-shadow:
    0 18px 48px rgba(0, 0, 0, 0.26),
    inset 0 1px 0 rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(22px) saturate(135%);
  -webkit-backdrop-filter: blur(22px) saturate(135%);
  transition:
    transform 220ms var(--ease-smooth),
    border-color 180ms ease,
    background 220ms ease,
    box-shadow 220ms ease;
}

.cloud-folder-dock:hover {
  transform: translateY(-2px) scale(1.015);
  border-color: rgba(255, 255, 255, 0.24);
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

.cloud-folder-dock:hover .folder-icon {
  transform: translateY(-2px);
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

.cloud-folder-dock:hover .paper-1 {
  transform: translate(-112%, -22px) rotate(-14deg);
}

.cloud-folder-dock:hover .paper-2 {
  height: 36px;
  transform: translate(8%, -24px) rotate(14deg);
}

.cloud-folder-dock:hover .paper-3 {
  height: 38px;
  transform: translate(-50%, -35px) rotate(5deg);
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

.cloud-folder-dock:hover .folder-front {
  transform: skew(15deg) scaleY(0.62);
}

.cloud-folder-dock:hover .folder-front.right {
  transform: skew(-15deg) scaleY(0.62);
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

.cloud-folder-dock:hover .folder-shine {
  opacity: 1;
  background-size: 160% 100%;
}

@media (max-width: 760px) {
  .cloud-folder-dock {
    top: auto;
    right: 12px;
    bottom: 88px;
    width: 64px;
    height: 60px;
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
