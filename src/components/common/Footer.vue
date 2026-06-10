<template>
  <footer class="app-footer">
    <div class="footer-main">
      <div class="footer-brand">
        <DeepLogo :scale="0.72" />
        <p>{{ copy.desc }}</p>
      </div>

      <nav class="footer-links" :aria-label="copy.resourcesAria">
        <button v-for="item in copy.resources" :key="item" type="button">{{ item }}</button>
      </nav>

      <div class="footer-status">
        <span>{{ copy.runtime }}</span>
        <strong>{{ copy.online }}</strong>
        <div class="status-bar"><i></i></div>
      </div>
    </div>

    <div class="footer-bottom">
      <span>2026 DeepInsight</span>
      <span>ICP 20260429</span>
    </div>
  </footer>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import DeepLogo from '@/components/common/DeepLogo.vue'

const { locale } = useI18n()
const isZh = computed(() => !locale.value.startsWith('en'))
const copy = computed(() => isZh.value
  ? {
      desc: '面向推荐系统模型接入、数据预览、性能可视化和知识记录的 DeepInsight 工作台。',
      resourcesAria: '页脚资源',
      resources: ['功能指南', '接口状态', '使用支持'],
      runtime: '运行状态',
      online: '在线',
    }
  : {
      desc: 'DeepInsight workspace for recommender model access, data preview, visual analysis, and knowledge records.',
      resourcesAria: 'Footer resources',
      resources: ['Feature Guide', 'API Status', 'Support'],
      runtime: 'Runtime',
      online: 'Online',
    })
</script>

<style scoped>
.app-footer {
  position: relative;
  z-index: 2;
  margin: 56px 24px 24px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  background: var(--surface-1);
  box-shadow: var(--shadow-soft);
  overflow: hidden;
}

.footer-main {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(180px, 0.8fr) minmax(180px, 0.55fr);
  gap: 28px;
  padding: 28px;
}

.footer-brand p {
  max-width: 520px;
  margin: 18px 0 0;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.7;
  font-weight: var(--font-weight-body);
}

.footer-links {
  display: grid;
  align-content: start;
  gap: 10px;
}

.footer-links button {
  width: max-content;
  max-width: 100%;
  overflow: hidden;
  border: 0;
  padding: 0;
  background: transparent;
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: var(--font-weight-label);
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
  transition: color 160ms ease, transform 160ms ease;
}

.footer-links button:hover {
  color: var(--primary-color);
  transform: translateX(3px);
}

.footer-status {
  padding: 14px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  background: var(--surface-2);
}

.footer-status span,
.footer-bottom {
  color: var(--text-muted);
  font-size: 11px;
  font-weight: var(--font-weight-label);
  text-transform: uppercase;
  letter-spacing: 0;
}

.footer-status strong {
  display: block;
  margin-top: 8px;
  color: var(--primary-color);
  font-size: 18px;
}

.status-bar {
  height: 6px;
  margin-top: 16px;
  overflow: hidden;
  border-radius: 999px;
  background: var(--surface-3);
}

.status-bar i {
  display: block;
  width: 72%;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, var(--primary-color), var(--accent-glow));
}

.footer-bottom {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 28px;
  border-top: 1px solid var(--border-color);
}

@media (max-width: 760px) {
  .app-footer {
    margin: 36px 14px 14px;
  }

  .footer-main {
    grid-template-columns: 1fr;
    padding: 22px;
  }

  .footer-bottom {
    flex-direction: column;
    padding: 14px 22px;
  }
}
</style>
