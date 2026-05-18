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
  min-height: calc(100vh - 72px);
  height: calc(100vh - 72px);
  padding: 96px 24px 42px;
  overflow: hidden;
}

.cloud-center-hero {
  position: relative;
  width: min(1240px, 100%);
  display: grid;
  grid-template-columns: minmax(0, 1fr) 260px;
  gap: 24px;
  align-items: center;
  margin: 0 auto 24px;
  padding: 30px;
  overflow: hidden;
  border: 1px solid color-mix(in srgb, var(--primary-color) 18%, var(--border-color));
  border-radius: 34px;
  background:
    radial-gradient(circle at 8% 0%, rgba(var(--primary-rgb), 0.2), transparent 34%),
    linear-gradient(135deg, rgba(var(--glass-bg-rgb), 0.42), rgba(5, 10, 15, 0.26));
  box-shadow: 0 28px 90px rgba(0, 0, 0, 0.24);
  backdrop-filter: blur(18px);
}

.cloud-center-hero span {
  color: var(--primary-color);
  font-size: 12px;
  font-weight: 950;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.cloud-center-hero h1 {
  margin: 8px 0 12px;
  color: var(--text-primary);
  font-size: clamp(40px, 7vw, 86px);
  font-weight: 950;
  letter-spacing: -0.07em;
}

.cloud-center-hero p {
  max-width: 820px;
  margin: 0;
  color: var(--text-secondary);
  font-size: 15px;
  line-height: 1.8;
}

.hero-cloud-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 18px;
}

.hero-cloud-actions button {
  height: 38px;
  padding: 0 15px;
  border: 1px solid rgba(var(--primary-rgb), 0.24);
  border-radius: 999px;
  background: rgba(var(--glass-bg-rgb), 0.32);
  color: var(--text-primary);
  font-size: 12px;
  font-weight: 950;
  transition: transform 180ms ease, border-color 180ms ease, background 180ms ease;
}

.hero-cloud-actions button:hover {
  transform: translateY(-1px);
  border-color: rgba(var(--primary-rgb), 0.48);
  background: rgba(var(--primary-rgb), 0.14);
}

.cloud-hero-orbit {
  position: relative;
  width: 220px;
  height: 220px;
  display: grid;
  place-items: center;
  justify-self: end;
  border-radius: 50%;
  background:
    radial-gradient(circle, rgba(var(--primary-rgb), 0.18), transparent 60%),
    rgba(255, 255, 255, 0.025);
}

.cloud-hero-orbit i {
  position: absolute;
  inset: 18px;
  border: 1px solid rgba(var(--primary-rgb), 0.24);
  border-radius: 50%;
  animation: orbitSpin 12s linear infinite;
}

.cloud-hero-orbit i:nth-child(2) {
  inset: 40px;
  border-color: rgba(66, 230, 164, 0.22);
  animation-duration: 9s;
  animation-direction: reverse;
}

.cloud-hero-orbit i:nth-child(3) {
  inset: 70px;
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
  font-size: 28px;
  font-weight: 950;
}

.cloud-center-page :deep(.cloud-portal) {
  width: min(1240px, 100%);
  margin-left: auto;
  margin-right: auto;
}

.cloud-center-page :deep(.cloud-workspace) {
  height: calc(100vh - 72px - 96px - 42px - 268px);
}

@keyframes orbitSpin {
  to { transform: rotate(360deg); }
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
