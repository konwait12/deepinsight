<template>
  <Teleport to="body">
    <Transition name="reader-drawer">
      <div v-if="modelValue" class="reader-backdrop" @click.self="close">
        <aside class="reader-drawer" role="dialog" aria-modal="true">
          <button class="reader-close" type="button" @click="close">×</button>
          <div v-if="loading" class="reader-state">{{ loadingText }}</div>
          <template v-else-if="article">
            <header class="reader-header">
              <span class="reader-kicker">{{ sourceLabel || article.nodeLabel || tagFallback }}</span>
              <h2>{{ article.title }}</h2>
              <div class="reader-meta">
                <span v-if="article.viewCount !== undefined">{{ viewsText }} {{ article.viewCount }}</span>
                <span v-if="article.createdAt">{{ formatTime(article.createdAt) }}</span>
                <a v-if="article.paperUrl" :href="article.paperUrl" target="_blank" rel="noreferrer">{{ paperText }}</a>
              </div>
            </header>
            <div class="reader-body" v-html="renderedHtml"></div>
          </template>
          <div v-else class="reader-state">{{ emptyText }}</div>
        </aside>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { renderMarkdown } from '@/utils/markdown'

const props = defineProps<{
  modelValue: boolean
  article?: any
  loading?: boolean
  sourceLabel?: string
}>()

const emit = defineEmits<{ 'update:modelValue': [value: boolean] }>()
const { locale } = useI18n()
const isZh = computed(() => locale.value.startsWith('zh'))

const renderedHtml = computed(() => props.article?.content ? renderMarkdown(props.article.content) : '')
const loadingText = computed(() => isZh.value ? '文章加载中...' : 'Loading article...')
const emptyText = computed(() => isZh.value ? '暂无文章内容' : 'No article content')
const viewsText = computed(() => isZh.value ? '浏览' : 'Views')
const paperText = computed(() => isZh.value ? '查看论文' : 'Paper')
const tagFallback = computed(() => isZh.value ? '知识文章' : 'Article')

const close = () => emit('update:modelValue', false)
const formatTime = (value: string) => {
  if (!value) return ''
  return new Date(value).toLocaleDateString(isZh.value ? 'zh-CN' : 'en-US', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  })
}
</script>

<style scoped>
.reader-backdrop {
  position: fixed;
  inset: 0;
  z-index: 3200;
  display: flex;
  justify-content: flex-end;
  background: linear-gradient(90deg, transparent 0%, rgba(var(--primary-rgb), 0.028) 100%);
  backdrop-filter: none;
}

:global(.light .reader-backdrop) {
  background: transparent;
}

.reader-drawer {
  position: relative;
  width: min(620px, 92vw);
  height: 100%;
  padding: 34px 38px 54px;
  border-left: 1px solid rgba(var(--primary-rgb), 0.22);
  background:
    radial-gradient(circle at 18% 12%, rgba(var(--primary-rgb), 0.14), transparent 32%),
    linear-gradient(180deg, rgba(var(--glass-bg-rgb), 0.92), rgba(var(--glass-bg-rgb), 0.82)),
    var(--surface-1);
  box-shadow: -30px 0 90px rgba(2, 6, 23, 0.28);
  overflow-y: auto;
}

.reader-close {
  position: sticky;
  top: 0;
  float: right;
  width: 34px;
  height: 34px;
  border: 1px solid var(--border-color);
  border-radius: 999px;
  background: rgba(var(--glass-bg-rgb), 0.7);
  color: var(--text-secondary);
  font-size: 22px;
  line-height: 1;
  cursor: pointer;
  backdrop-filter: blur(14px);
}

.reader-close:hover {
  color: var(--primary-color);
  border-color: rgba(var(--primary-rgb), 0.34);
}

.reader-header {
  margin: 8px 42px 26px 0;
}

.reader-kicker {
  display: inline-flex;
  margin-bottom: 12px;
  padding: 5px 9px;
  border: 1px solid rgba(var(--primary-rgb), 0.22);
  border-radius: 999px;
  background: rgba(var(--primary-rgb), 0.08);
  color: var(--primary-color);
  font-size: 10px;
  font-weight: 900;
}

.reader-header h2 {
  margin: 0;
  color: var(--text-primary);
  font-size: 28px;
  font-weight: 950;
  line-height: 1.18;
}

.reader-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
  margin-top: 12px;
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 750;
}

.reader-meta a {
  color: var(--primary-color);
  text-decoration: none;
}

.reader-body {
  color: var(--text-primary);
  font-size: 15px;
  line-height: 1.9;
}

.reader-body :deep(h1),
.reader-body :deep(h2),
.reader-body :deep(h3) {
  color: var(--text-primary);
  line-height: 1.25;
}

.reader-body :deep(h1) { font-size: 24px; margin: 28px 0 12px; }
.reader-body :deep(h2) { font-size: 20px; margin: 26px 0 10px; }
.reader-body :deep(h3) { font-size: 17px; margin: 22px 0 8px; }
.reader-body :deep(p) { margin: 0 0 12px; color: var(--text-secondary); }
.reader-body :deep(strong) { color: var(--text-primary); }
.reader-body :deep(ul),
.reader-body :deep(ol) { padding-left: 20px; margin: 0 0 14px; color: var(--text-secondary); }
.reader-body :deep(li) { margin-bottom: 4px; }
.reader-body :deep(li::marker) { color: var(--primary-color); }
.reader-body :deep(blockquote) {
  margin: 14px 0;
  padding: 10px 14px;
  border-left: 3px solid var(--primary-color);
  border-radius: 0 10px 10px 0;
  background: rgba(var(--primary-rgb), 0.06);
  color: var(--text-secondary);
}
.reader-body :deep(code) {
  padding: 2px 6px;
  border-radius: 6px;
  background: var(--bg-input);
  color: var(--primary-color);
  font-family: "JetBrains Mono", Consolas, monospace;
  font-size: 13px;
}
.reader-body :deep(pre) {
  margin: 16px 0;
  padding: 16px;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  background: var(--bg-input);
  overflow-x: auto;
}
.reader-body :deep(pre code) {
  padding: 0;
  background: transparent;
  color: var(--text-primary);
}
.reader-body :deep(table) {
  width: 100%;
  margin: 16px 0;
  border-collapse: collapse;
  font-size: 13px;
}
.reader-body :deep(th),
.reader-body :deep(td) {
  padding: 9px 11px;
  border-bottom: 1px solid var(--border-color);
  text-align: left;
}
.reader-body :deep(th) {
  background: var(--bg-input);
  color: var(--text-primary);
  font-weight: 850;
}

.reader-state {
  display: grid;
  min-height: 50vh;
  place-items: center;
  color: var(--text-muted);
  font-size: 13px;
  font-weight: 800;
}

.reader-drawer-enter-active,
.reader-drawer-leave-active {
  transition: opacity 220ms ease;
}
.reader-drawer-enter-active .reader-drawer,
.reader-drawer-leave-active .reader-drawer {
  transition: transform 260ms cubic-bezier(0.16, 1, 0.3, 1), filter 260ms ease;
}
.reader-drawer-enter-from,
.reader-drawer-leave-to {
  opacity: 0;
}
.reader-drawer-enter-from .reader-drawer,
.reader-drawer-leave-to .reader-drawer {
  transform: translateX(100%);
  filter: blur(6px);
}
</style>
