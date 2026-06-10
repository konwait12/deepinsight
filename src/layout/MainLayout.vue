<template>
  <main class="content-body relative">
    <div class="relative z-10 min-h-full">
      <CloudFolderDock v-if="showCloudPortal" />
      <router-view v-slot="{ Component }">
        <Transition name="workspace-page" mode="out-in" appear>
          <component :is="Component" :key="workspaceViewKey" class="workspace-view-shell" />
        </Transition>
      </router-view>
    </div>
  </main>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useThemeStore } from '@/stores/theme.store'
import CloudFolderDock from '@/components/cloud/CloudFolderDock.vue'

const themeStore = useThemeStore()
const route = useRoute()
const showCloudPortal = computed(() => !['Data', 'Cloud', 'DatasetVisualization'].includes(String(route.name || '')))
const workspaceViewKey = computed(() => route.path)
</script>

<style scoped>
.content-body {
  min-height: calc(100dvh - var(--header-height, 72px));
  min-width: 0;
  overscroll-behavior: contain;
  perspective: 1400px;
}

.workspace-view-shell {
  min-width: 0;
  transform-origin: center 28px;
}

.workspace-page-enter-active,
.workspace-page-leave-active {
  transition:
    opacity 190ms cubic-bezier(0.16, 1, 0.3, 1),
    transform 240ms cubic-bezier(0.16, 1, 0.3, 1),
    filter 190ms ease;
  will-change: opacity, transform, filter;
}

.workspace-page-enter-from {
  opacity: 0;
  filter: blur(4px) saturate(0.98);
  transform: translate3d(0, 7px, 0) scale(0.997);
}

.workspace-page-leave-to {
  opacity: 0;
  filter: blur(3px) saturate(0.98);
  transform: translate3d(0, -3px, 0) scale(0.998);
}

@media (prefers-reduced-motion: reduce) {
  .workspace-page-enter-active,
  .workspace-page-leave-active {
    transition: none;
  }
}
</style>
