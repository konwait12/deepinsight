<template>
  <div class="detail-page">
    <div class="detail-container">
      <nav class="detail-nav">
        <button class="back-btn" type="button" @click="router.push(ROUTES.FORUM)">返回社区</button>
      </nav>

      <div v-if="loading" class="loading-state">文章加载中...</div>

      <template v-else-if="post">
        <article class="article-main">
          <header class="article-header">
            <div class="header-badges">
              <span v-if="post.isOfficial" class="badge badge-official">平台文章</span>
              <span v-if="post.isPinned" class="badge badge-pinned">置顶</span>
            </div>
            <h1 class="article-title">{{ post.title }}</h1>
            <div class="article-meta">
              <span>#{{ post.id }}</span>
              <span>浏览 {{ post.viewCount || 0 }}</span>
              <span>{{ formatTime(post.createdAt) }}</span>
            </div>
            <img v-if="displayCoverUrl" :src="displayCoverUrl" :alt="post.title" class="cover-img" />
          </header>

          <ArticleToc v-if="headings.length > 1" :headings="headings" />
          <div class="article-body" v-html="renderedHtml" />

          <section class="comments-section">
            <h4>评论 {{ comments.length }}</h4>
            <div v-if="!comments.length" class="empty-comments">暂无评论</div>
            <div v-for="cm in comments" :key="cm.id" class="comment-item">
              <span class="cm-author">用户 #{{ cm.userId }}</span>
              <span class="cm-time">{{ formatTime(cm.createdAt) }}</span>
              <p class="cm-text">{{ cm.content }}</p>
            </div>
            <div v-if="isLoggedIn" class="add-comment">
              <el-input v-model="newComment" placeholder="写下你的评论..." @keyup.enter="submitComment" />
              <el-button size="small" type="primary" @click="submitComment">发送</el-button>
            </div>
          </section>
        </article>
      </template>

      <div v-else class="loading-state">内容不存在或已被删除</div>
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
import { hasStoredAuthToken } from '@/utils/authState'
import { ROUTES } from '@/constants'

const route = useRoute()
const router = useRouter()
const post = ref<any>(null)
const comments = ref<any[]>([])
const newComment = ref('')
const loading = ref(true)
const isLoggedIn = computed(() => hasStoredAuthToken())

const displayCoverUrl = computed(() => {
  const explicitCover = String(post.value?.coverUrl || '').trim()
  if (explicitCover) return explicitCover
  return firstMarkdownImage(post.value?.content || '')
})

const articleContent = computed(() => {
  const content = removeDisplayedCover(post.value?.content || '', displayCoverUrl.value)
  return enrichArticleContent(content, post.value?.title || '')
})

const headings = computed(() => extractHeadings(articleContent.value).filter((heading) => heading.level <= 3))
const renderedHtml = computed(() => articleContent.value ? renderMarkdown(articleContent.value) : '')

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

const firstMarkdownImage = (markdown: string) => {
  const match = markdown.match(/!\[[^\]]*]\(([^)\s]+)(?:\s+['"][^'"]*['"])?\)/)
  return match?.[1]?.trim() || ''
}

const normalizeUrl = (value: string) => value.trim().replace(/^['"]|['"]$/g, '')

const removeDisplayedCover = (markdown: string, coverUrl: string) => {
  if (!coverUrl) return markdown
  let removed = false
  return markdown.replace(/!\[[^\]]*]\(([^)\s]+)(?:\s+['"][^'"]*['"])?\)\s*/g, (full, url) => {
    if (!removed && normalizeUrl(url) === normalizeUrl(coverUrl)) {
      removed = true
      return ''
    }
    return full
  })
}

const scrollToTop = () => window.scrollTo({ top: 0, behavior: 'auto' })

const fetchPost = async () => {
  loading.value = true
  scrollToTop()
  try {
    const [postRes, commentsRes] = await Promise.all([
      forumApi.getPost(route.params.id as string),
      forumApi.getComments(route.params.id as string),
    ])
    post.value = postRes.data.code === 200 ? postRes.data.data : null
    comments.value = commentsRes.data.code === 200 ? postRes.data.data || [] : []
  } catch (error) {
    console.error(error)
    post.value = null
    comments.value = []
    ElMessage.error('帖子加载失败')
  } finally {
    loading.value = false
  }
}

const submitComment = async () => {
  if (!newComment.value.trim() || !post.value) return
  try {
    const res = await forumApi.addComment(post.value.id, newComment.value.trim())
    if (res.data.code === 200) {
      newComment.value = ''
      await fetchPost()
    } else {
      ElMessage.error(res.data.message || '评论失败')
    }
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '评论失败')
  }
}

onMounted(fetchPost)
watch(() => route.params.id, fetchPost)
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
  max-width: 920px;
  margin: 0 auto;
  padding: 40px 24px 80px;
}

.detail-nav {
  margin-bottom: 22px;
}

.back-btn {
  border: 1px solid color-mix(in srgb, var(--primary-color) 18%, var(--border-color));
  border-radius: var(--radius-sm);
  background: rgba(var(--glass-bg-rgb), 0.62);
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 13px;
  font-weight: var(--font-weight-body);
  padding: 8px 15px;
  transition: color 180ms ease, background 180ms ease, border-color 180ms ease, transform 180ms ease;
}

.back-btn:hover {
  transform: translateY(-1px);
  border-color: color-mix(in srgb, var(--primary-color) 38%, var(--border-color));
  background: rgba(var(--primary-rgb), 0.12);
  color: var(--primary-color);
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

.header-badges {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.badge {
  border: 1px solid transparent;
  border-radius: 999px;
  font-size: 10px;
  font-weight: var(--font-weight-title);
  padding: 4px 9px;
}

.badge-official {
  border-color: rgba(var(--primary-rgb), 0.24);
  background: rgba(var(--primary-rgb), 0.12);
  color: var(--primary-color);
}

.badge-pinned {
  border-color: rgba(245, 158, 11, 0.24);
  background: rgba(245, 158, 11, 0.14);
  color: #f59e0b;
}

.article-title {
  margin: 0 0 14px;
  color: var(--text-primary);
  font-size: clamp(32px, 5vw, 56px);
  font-weight: var(--font-weight-title);
  letter-spacing: 0;
  line-height: 1.06;
}

.article-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 18px;
  color: var(--text-muted);
  font-size: 13px;
  font-weight: var(--font-weight-body);
}

.cover-img {
  width: 100%;
  max-height: 460px;
  object-fit: cover;
  border: 1px solid color-mix(in srgb, var(--primary-color) 14%, var(--border-color));
  border-radius: var(--radius-sm);
  margin-top: 10px;
  box-shadow: 0 22px 64px rgba(0, 0, 0, 0.28);
}

:global(.light) .cover-img {
  box-shadow: 0 18px 50px rgba(30, 41, 59, 0.12);
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

.comments-section {
  margin-top: 40px;
  border-top: 1px solid color-mix(in srgb, var(--primary-color) 16%, var(--border-color));
  padding-top: 24px;
}

.comments-section h4 {
  margin: 0 0 14px;
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 800;
}

.empty-comments {
  border: 1px solid color-mix(in srgb, var(--primary-color) 10%, var(--border-color));
  border-radius: var(--radius-sm);
  background: rgba(var(--glass-bg-rgb), 0.34);
  color: var(--text-muted);
  font-size: 12px;
  padding: 12px 14px;
}

.comment-item {
  border-bottom: 1px solid color-mix(in srgb, var(--primary-color) 10%, var(--border-color));
  padding: 12px 0;
}

.cm-author {
  color: var(--primary-color);
  font-size: 12px;
  font-weight: 700;
}

.cm-time {
  margin-left: 8px;
  color: var(--text-secondary);
  font-size: 10px;
}

.cm-text {
  margin: 4px 0 0;
  color: var(--text-primary);
  font-size: 13px;
}

.add-comment {
  display: flex;
  gap: 10px;
  margin-top: 16px;
}

@media (max-width: 720px) {
  .detail-container {
    padding: 24px 14px 56px;
  }

  .article-main {
    border-radius: 24px;
  }

  .add-comment {
    display: grid;
  }
}
</style>
