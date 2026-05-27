<template>
  <div class="cloud-center-page">
    <section class="cloud-center-hero">
      <div>
        <span>{{ copy.eyebrow }}</span>
        <h1>{{ copy.title }}</h1>
        <p>{{ copy.desc }}</p>
        <div class="hero-cloud-actions">
          <button type="button" @click="router.push('/data')">{{ copy.dataEntry }}</button>
          <button type="button" @click="router.push('/ai')">{{ copy.aiEntry }}</button>
          <button type="button" @click="router.push('/viz')">{{ copy.vizEntry }}</button>
        </div>
      </div>
      <div class="cloud-hero-orbit">
        <i></i>
        <i></i>
        <i></i>
        <strong>Cloud</strong>
      </div>
    </section>

    <CloudWorkspacePortal
      variant="page"
      default-open
      :title="copy.portalTitle"
      :subtitle="copy.portalSubtitle"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import CloudWorkspacePortal from '@/components/cloud/CloudWorkspacePortal.vue'

const router = useRouter()
const { locale } = useI18n()
const copy = computed(() => {
  if (locale.value.startsWith('en')) {
    return {
      eyebrow: 'Cloud Center',
      title: 'Cloud Center',
      desc: 'Manage uploaded files, images, saved views, matrix records, model references, and data assets in one account-isolated server workspace.',
      dataEntry: 'Data assets',
      aiEntry: 'AI workspace',
      vizEntry: 'Visualization',
      portalTitle: 'Research cloud library',
      portalSubtitle: 'Images use visual covers; documents, models, matrix records, and views receive readable text covers. Use folders, drag-and-drop, upload, download, and AI selection from the same workspace.',
    }
  }
  return {
    eyebrow: 'Cloud Center',
    title: '云端中心',
    desc: '集中管理上传文件、图片、保存视图、矩阵记录、模型引用和数据资产。所有内容写入服务器数据库，并按账号隔离。',
    dataEntry: '数据资产',
    aiEntry: 'AI 工作室',
    vizEntry: '可视化分析',
    portalTitle: '研究素材云端库',
    portalSubtitle: '图片以封面呈现；文档、模型、矩阵记录和视图生成文字封面。这里支持文件夹、拖动归档、上传下载，以及导入 AI 分析。',
  }
})
</script>

<style scoped>
.cloud-center-page {
  position: relative;
  min-height: calc(100dvh - var(--header-height, 72px));
  padding: 18px 24px 24px;
  overflow-x: clip;
  overflow-y: visible;
  overscroll-behavior-x: contain;
  touch-action: pan-y;
}

.cloud-center-page::before {
  content: "";
  position: fixed;
  inset: 72px 0 0;
  z-index: -1;
  pointer-events: none;
  background:
    radial-gradient(circle at 72% 14%, rgba(var(--primary-rgb), 0.11), transparent 34%),
    radial-gradient(circle at 18% 82%, rgba(66, 230, 164, 0.075), transparent 30%);
  transform: translateZ(0);
}

.cloud-center-hero {
  position: relative;
  width: min(1240px, 100%);
  display: grid;
  grid-template-columns: minmax(0, 1fr) 260px;
  gap: 18px;
  align-items: center;
  margin: 0 auto 14px;
  padding: 20px 24px;
  overflow: hidden;
  border: 1px solid color-mix(in srgb, var(--primary-color) 18%, var(--border-color));
  border-radius: 34px;
  background: var(--workbench-shell-bg-strong);
  box-shadow: var(--workbench-shadow);
  backdrop-filter: blur(18px);
  animation: cloudSceneRise 720ms cubic-bezier(0.16, 1, 0.3, 1) both;
}

.cloud-center-hero span {
  color: var(--primary-color);
  font-size: 12px;
  font-weight: var(--font-weight-title);
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.cloud-center-hero h1 {
  margin: 6px 0 8px;
  color: var(--text-primary);
  font-size: clamp(32px, 4.6vw, 60px);
  font-weight: var(--font-weight-title);
  letter-spacing: -0.07em;
}

.cloud-center-hero p {
  max-width: 820px;
  margin: 0;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.65;
}

.hero-cloud-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 16px;
}

.hero-cloud-actions button {
  min-height: 40px;
  min-width: 108px;
  padding: 0 18px;
  border: 1px solid rgba(var(--primary-rgb), 0.24);
  border-radius: 999px;
  background: var(--workbench-control-bg);
  color: var(--text-primary);
  font-size: 12px;
  font-weight: var(--font-weight-title);
  transition: transform 180ms ease, border-color 180ms ease, background 180ms ease;
}

.hero-cloud-actions button:hover {
  transform: translateY(-1px);
  border-color: rgba(var(--primary-rgb), 0.48);
  background: rgba(var(--primary-rgb), 0.14);
}

.cloud-hero-orbit {
  position: relative;
  width: 156px;
  height: 156px;
  display: grid;
  place-items: center;
  justify-self: end;
  border-radius: 50%;
  background:
    radial-gradient(circle, rgba(var(--primary-rgb), 0.18), transparent 60%),
    var(--workbench-panel-bg);
}

.cloud-hero-orbit i {
  position: absolute;
  inset: 12px;
  border: 1px solid rgba(var(--primary-rgb), 0.24);
  border-radius: 50%;
  animation: orbitSpin 12s linear infinite;
}

.cloud-hero-orbit i:nth-child(2) {
  inset: 30px;
  border-color: rgba(66, 230, 164, 0.22);
  animation-duration: 9s;
  animation-direction: reverse;
}

.cloud-hero-orbit i:nth-child(3) {
  inset: 52px;
  border-color: rgba(103, 232, 249, 0.2);
  animation-duration: 6s;
}

.cloud-hero-orbit i::after {
  content: "";
  position: absolute;
  top: -4px;
  left: 50%;
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: var(--primary-color);
  box-shadow: 0 0 18px rgba(var(--primary-rgb), 0.7);
}

.cloud-hero-orbit strong {
  color: var(--text-primary);
  font-size: 22px;
  font-weight: var(--font-weight-title);
}

.cloud-center-page :deep(.cloud-portal) {
  width: min(1240px, 100%);
  margin-left: auto;
  margin-right: auto;
  animation: cloudSceneRise 820ms 70ms cubic-bezier(0.16, 1, 0.3, 1) both;
}

.cloud-center-page :deep(.cloud-workspace) {
  height: clamp(720px, calc(100dvh - var(--header-height, 72px) - 120px), 960px);
  min-height: 720px;
}

@keyframes orbitSpin {
  to { transform: rotate(360deg); }
}

@keyframes cloudSceneRise {
  from {
    opacity: 0;
    transform: translateY(32px);
    filter: blur(10px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
    filter: blur(0);
  }
}

@media (prefers-reduced-motion: reduce) {
  .cloud-center-hero,
  .cloud-center-page :deep(.cloud-portal) {
    animation: none;
  }
}

@media (max-width: 840px) {
  .cloud-center-page {
    height: auto;
    overflow: visible;
    padding: 84px 14px 28px;
  }

  .cloud-center-page :deep(.cloud-workspace) {
    height: auto;
  }

  .cloud-center-hero {
    grid-template-columns: 1fr;
  }

  .cloud-hero-orbit {
    justify-self: center;
  }
}
</style>
