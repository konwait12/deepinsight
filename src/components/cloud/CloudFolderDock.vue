<template>
  <div class="cloud-folder-dock" :class="{ open: panelOpen }">
    <button class="folder-trigger" type="button" aria-label="打开云端素材" @click="togglePanel">
      <span class="folder-icon" :style="folderVars">
        <i class="paper paper-1"><b>AI</b></i>
        <i class="paper paper-2"><b>VIZ</b></i>
        <i class="paper paper-3"><b>DATA</b></i>
        <span class="folder-back"></span>
        <span class="folder-front"></span>
        <span class="folder-front right"></span>
      </span>
      <span class="folder-copy">
        <strong>云端</strong>
        <em>素材入口</em>
      </span>
    </button>

    <Transition name="folder-panel">
      <div v-if="panelOpen" class="folder-panel">
        <CloudWorkspacePortal
          compact
          default-open
          title="云端素材"
          subtitle="快速查看当前账号保存的分析记录、视图、图片和文件。完整管理请进入云端中心。"
        />
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import CloudWorkspacePortal from '@/components/cloud/CloudWorkspacePortal.vue'

const panelOpen = ref(false)
const folderVars = computed(() => ({
  '--folder-color': 'var(--primary-color)',
  '--folder-back-color': 'color-mix(in srgb, var(--primary-color) 72%, #0f172a)',
}))

function togglePanel() {
  panelOpen.value = !panelOpen.value
}
</script>

<style scoped>
.cloud-folder-dock {
  position: fixed;
  top: 92px;
  right: 22px;
  z-index: 88;
}

.folder-trigger {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 132px;
  height: 74px;
  padding: 8px 12px 8px 10px;
  border: 1px solid rgba(var(--primary-rgb), 0.22);
  border-radius: 22px;
  background:
    radial-gradient(circle at 16% 0%, rgba(var(--primary-rgb), 0.18), transparent 42%),
    rgba(var(--glass-bg-rgb), 0.54);
  box-shadow: 0 18px 48px rgba(0, 0, 0, 0.24);
  backdrop-filter: blur(18px);
}

.folder-icon {
  --folder-color: var(--primary-color);
  --folder-back-color: color-mix(in srgb, var(--primary-color) 72%, #0f172a);
  position: relative;
  width: 64px;
  height: 50px;
  flex: 0 0 auto;
  transition: transform 220ms ease;
}

.folder-trigger:hover .folder-icon,
.cloud-folder-dock.open .folder-icon {
  transform: translateY(-5px);
}

.folder-back {
  position: absolute;
  left: 0;
  bottom: 0;
  width: 64px;
  height: 48px;
  border-radius: 0 9px 9px 9px;
  background: var(--folder-back-color);
}

.folder-back::after {
  content: "";
  position: absolute;
  left: 0;
  bottom: 98%;
  width: 22px;
  height: 8px;
  border-radius: 5px 5px 0 0;
  background: var(--folder-back-color);
}

.paper {
  position: absolute;
  z-index: 2;
  bottom: 6px;
  left: 50%;
  width: 45px;
  height: 38px;
  display: grid;
  place-items: start center;
  padding-top: 5px;
  border-radius: 7px;
  background: #f8fafc;
  color: #0f172a;
  font-size: 7px;
  font-style: normal;
  font-weight: 950;
  transform: translate(-50%, 8px);
  transition: transform 280ms ease, height 280ms ease;
}

.paper-2 {
  width: 50px;
  height: 34px;
  background: #eef2ff;
}

.paper-3 {
  width: 55px;
  height: 30px;
  background: #ffffff;
}

.folder-trigger:hover .paper-1,
.cloud-folder-dock.open .paper-1 {
  transform: translate(-110%, -28px) rotate(-14deg);
}

.folder-trigger:hover .paper-2,
.cloud-folder-dock.open .paper-2 {
  height: 40px;
  transform: translate(6%, -30px) rotate(14deg);
}

.folder-trigger:hover .paper-3,
.cloud-folder-dock.open .paper-3 {
  height: 42px;
  transform: translate(-50%, -42px) rotate(5deg);
}

.folder-front {
  position: absolute;
  z-index: 3;
  inset: 0;
  border-radius: 5px 9px 9px 9px;
  background: var(--folder-color);
  transform-origin: bottom;
  transition: transform 280ms ease;
}

.folder-front.right {
  clip-path: polygon(50% 0, 100% 0, 100% 100%, 50% 100%);
}

.folder-trigger:hover .folder-front,
.cloud-folder-dock.open .folder-front {
  transform: skew(15deg) scaleY(0.62);
}

.folder-trigger:hover .folder-front.right,
.cloud-folder-dock.open .folder-front.right {
  transform: skew(-15deg) scaleY(0.62);
}

.folder-copy {
  display: grid;
  gap: 2px;
  text-align: left;
}

.folder-copy strong {
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 950;
}

.folder-copy em {
  color: var(--text-muted);
  font-size: 10px;
  font-style: normal;
  font-weight: 900;
}

.folder-panel {
  position: absolute;
  top: calc(100% + 14px);
  right: 0;
  width: min(980px, calc(100vw - 44px));
  max-height: calc(100vh - 132px);
  overflow: auto;
  border-radius: 28px;
  box-shadow: 0 28px 90px rgba(0, 0, 0, 0.34);
}

.folder-panel :deep(.cloud-portal) {
  margin: 0;
}

.folder-panel :deep(.cloud-body) {
  min-height: 440px;
  grid-template-columns: 230px minmax(0, 1fr);
}

.folder-panel :deep(.cloud-detail) {
  grid-column: 1 / -1;
}

.folder-panel-enter-active,
.folder-panel-leave-active {
  transition: opacity 180ms ease, transform 180ms ease;
}

.folder-panel-enter-from,
.folder-panel-leave-to {
  opacity: 0;
  transform: translateY(-8px) scale(0.98);
}

@media (max-width: 760px) {
  .cloud-folder-dock {
    top: auto;
    right: 12px;
    bottom: 88px;
  }

  .folder-copy {
    display: none;
  }

  .folder-trigger {
    min-width: 82px;
    width: 82px;
  }
}
</style>
