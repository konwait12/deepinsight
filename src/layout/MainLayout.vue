<template>
  <main class="content-body relative">
    <div class="relative z-10 min-h-full">
      <CloudFolderDock v-if="showCloudPortal" />
      <router-view />
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
const showCloudPortal = computed(() => !['Dashboard'].includes(String(route.name || '')))
</script>

<style scoped>
.content-body {
  min-height: calc(100vh - 56px);
}
</style>
