<template>
  <div class="detail-page">
    <div class="detail-container">
      <nav class="detail-nav">
        <button class="back-btn" type="button" @click="returnToKnowledgeIndex">返回平台文章</button>
        <span v-if="article?.nodeLabel" class="source-tag">{{ article.nodeLabel }}</span>
      </nav>

      <div v-if="loading" class="loading-state">文章加载中...</div>

      <template v-else-if="article">
        <article class="article-main">
          <header class="article-header">
            <span class="article-kicker">知识文章</span>
            <h1 class="article-title">{{ article.title }}</h1>
            <div class="article-meta">
              <span>浏览 {{ article.viewCount || 0 }}</span>
              <span>{{ formatTime(article.createdAt) }}</span>
            </div>
          </header>

          <ArticleToc v-if="headings.length > 1" :headings="headings" />
          <div class="article-body" v-html="renderedHtml" />
        </article>
      </template>

      <div v-else class="loading-state">未找到这篇文章</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { forumApi } from '@/api'
import ArticleToc from '@/components/common/ArticleToc.vue'
import { enrichArticleContent, extractHeadings, renderMarkdown } from '@/utils/markdown'
import { ROUTES } from '@/constants'

const route = useRoute()
const router = useRouter()
const article = ref<any>(null)
const loading = ref(true)
const articleIndexRestoreKey = 'deepinsight:knowledge:articleIndex'

const displayContent = computed(() => enrichArticleContent(article.value?.content || '', article.value?.title || ''))
const headings = computed(() => extractHeadings(displayContent.value).filter((heading) => heading.level <= 3))
const renderedHtml = computed(() => displayContent.value ? renderMarkdown(displayContent.value) : '')

const returnToKnowledgeIndex = () => {
  window.sessionStorage.setItem(articleIndexRestoreKey, 'open')
  router.push({
    path: ROUTES.KNOWLEDGE,
    query: { articleIndex: 'open' },
  })
}

const formatTime = (value?: string) => {
  if (!value) return ''
  return new Date(value).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

const fetchArticle = async () => {
  loading.value = true
  window.scrollTo({ top: 0, behavior: 'auto' })
  try {
    const res = await forumApi.getKnowledgeArticle(route.params.id as string)
    article.value = res.data.code === 200 ? res.data.data : res.data
  } catch (error) {
    console.error(error)
    article.value = null
    ElMessage.error('文章加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(fetchArticle)
watch(() => route.params.id, fetchArticle)
</script>

<style scoped>
.detail-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at 14% 8%, rgba(var(--primary-rgb), 0.16), transparent 34%),
    radial-gradient(circle at 86% 16%, rgba(var(--primary-rgb), 0.08), transparent 30%),
    linear-gradient(90deg, rgba(var(--primary-rgb), 0.035) 1px, transparent 1px),
    linear-gradient(180deg, rgba(var(--primary-rgb), 0.03) 1px, transparent 1px),
    linear-gradient(145deg, color-mix(in srgb, var(--bg-color) 94%, rgba(var(--primary-rgb), 0.1)) 0%, var(--bg-color) 52%, color-mix(in srgb, var(--bg-color) 88%, #02070a) 100%);
  background-size: auto, auto, 48px 48px, 48px 48px, auto;
}

:global(.light) .detail-page {
  background:
    radial-gradient(circle at 12% 8%, rgba(var(--primary-rgb), 0.11), transparent 34%),
    radial-gradient(circle at 84% 12%, rgba(96, 165, 250, 0.1), transparent 32%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.42), transparent 42%),
    var(--bg-color);
}

.detail-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 40px 24px 80px;
}

.detail-nav {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 22px;
}

.back-btn,
.source-tag {
  border: 1px solid color-mix(in srgb, var(--primary-color) 18%, var(--border-color));
  border-radius: var(--radius-sm);
  background: rgba(var(--glass-bg-rgb), 0.62);
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: var(--font-weight-body);
  padding: 8px 15px;
}

.back-btn {
  cursor: pointer;
  transition: color 180ms ease, background 180ms ease, border-color 180ms ease, transform 180ms ease;
}

.back-btn:hover {
  transform: translateY(-1px);
  border-color: color-mix(in srgb, var(--primary-color) 38%, var(--border-color));
  background: rgba(var(--primary-rgb), 0.12);
  color: var(--primary-color);
}

.source-tag {
  color: var(--primary-color);
  font-size: 11px;
}

.loading-state,
.article-main {
  border: 1px solid color-mix(in srgb, var(--primary-color) 16%, var(--border-color));
  border-radius: var(--radius-lg);
  background:
    radial-gradient(circle at 16% 0%, rgba(var(--primary-rgb), 0.1), transparent 36%),
    rgba(var(--glass-bg-rgb), 0.72);
  box-shadow: 0 34px 100px rgba(0, 0, 0, 0.34), 0 0 0 1px rgba(var(--primary-rgb), 0.05);
  backdrop-filter: blur(18px) saturate(140%);
  -webkit-backdrop-filter: blur(18px) saturate(140%);
}

:global(.light) .loading-state,
:global(.light) .article-main {
  box-shadow: 0 24px 70px rgba(30, 41, 59, 0.1);
}

.loading-state {
  display: grid;
  min-height: 360px;
  place-items: center;
  color: var(--text-secondary);
  font-weight: var(--font-weight-title);
}

.article-main {
  overflow: hidden;
  padding: clamp(22px, 4vw, 44px);
}

.article-header {
  margin-bottom: 32px;
}

.article-kicker {
  display: inline-flex;
  margin-bottom: 12px;
  padding: 5px 9px;
  border: 1px solid rgba(var(--primary-rgb), 0.22);
  border-radius: 999px;
  background: rgba(var(--primary-rgb), 0.08);
  color: var(--primary-color);
  font-size: 10px;
  font-weight: var(--font-weight-title);
}

.article-title {
  margin: 0 0 14px;
  color: var(--text-primary);
  font-size: clamp(32px, 5vw, 54px);
  font-weight: var(--font-weight-title);
  line-height: 1.08;
}

.article-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  color: var(--text-muted);
  font-size: 13px;
  font-weight: var(--font-weight-body);
}

.article-body {
  color: var(--text-primary);
  font-size: 15px;
  line-height: 1.9;
}

.article-body :deep(h1),
.article-body :deep(h2),
.article-body :deep(h3),
.article-body :deep(h4) {
  scroll-margin-top: 96px;
}

.article-body :deep(h1) {
  margin: 36px 0 14px;
  font-size: 24px;
  font-weight: 900;
}

.article-body :deep(h2) {
  margin: 30px 0 12px;
  border-left: 4px solid var(--primary-color);
  padding-left: 12px;
  font-size: 20px;
  font-weight: 800;
}

.article-body :deep(h3) {
  margin: 24px 0 10px;
  font-size: 17px;
  font-weight: 700;
}

.article-body :deep(h4) {
  margin: 18px 0 8px;
  font-size: 15px;
  font-weight: 700;
}

.article-body :deep(p) {
  margin: 0 0 12px;
  color: var(--text-secondary);
}

.article-body :deep(strong) {
  color: var(--text-primary);
}

.article-body :deep(a) {
  color: var(--primary-color);
  text-decoration: underline;
  text-decoration-style: dashed;
  text-underline-offset: 3px;
}

.article-body :deep(blockquote) {
  margin: 14px 0;
  border-left: 4px solid var(--primary-color);
  border-radius: 0 10px 10px 0;
  background: rgba(var(--primary-rgb), 0.08);
  padding: 10px 16px;
  color: var(--text-secondary);
}

.article-body :deep(hr) {
  border: none;
  border-top: 1px solid var(--border-color);
  margin: 24px 0;
}

.article-body :deep(ul),
.article-body :deep(ol) {
  margin: 0 0 12px;
  padding-left: 20px;
  color: var(--text-secondary);
}

.article-body :deep(li) {
  margin-bottom: 3px;
}

.article-body :deep(li::marker) {
  color: var(--primary-color);
}

.article-body :deep(img) {
  max-width: 100%;
  border-radius: var(--radius-sm);
  margin: 12px 0;
}

.article-body :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 14px 0;
  font-size: 13px;
}

.article-body :deep(th) {
  border-bottom: 2px solid var(--border-color);
  background: rgba(var(--primary-rgb), 0.08);
  padding: 8px 12px;
  text-align: left;
  font-weight: 700;
}

.article-body :deep(td) {
  border-bottom: 1px solid var(--border-color);
  padding: 6px 12px;
}

.article-body :deep(tr:hover td) {
  background: rgba(var(--primary-rgb), 0.05);
}

.article-body :deep(code) {
  border-radius: 5px;
  background: rgba(var(--primary-rgb), 0.08);
  color: var(--primary-color);
  font-family: "JetBrains Mono", monospace;
  font-size: 13px;
  padding: 2px 6px;
}

.article-body :deep(pre) {
  overflow-x: auto;
  margin: 16px 0;
  border: 1px solid color-mix(in srgb, var(--primary-color) 14%, var(--border-color));
  border-radius: var(--radius-sm);
  background: rgba(var(--glass-bg-rgb), 0.72);
  padding: 16px;
}

.article-body :deep(pre code) {
  background: transparent;
  color: var(--text-primary);
  padding: 0;
}

@media (max-width: 720px) {
  .detail-container {
    padding: 24px 14px 56px;
  }

  .article-main {
    border-radius: 24px;
  }
}
</style>
