<template>
  <div class="forum-page" :class="{ 'vertical-nav': !themeStore.isHorizontalMenu }">
    <section class="forum-shell">
      <header class="forum-hero">
        <div class="hero-copy">
          <span class="eyebrow">DeepInsight Community</span>
          <h1>社区论坛</h1>
          <p>围绕推荐模型、数据集、评估结果和平台使用展开讨论。这里保留文章与帖子，优先保证阅读、搜索和发布效率。</p>
        </div>

        <div class="forum-actions">
          <button v-if="isLoggedIn" class="primary-action" type="button" @click="showCreate = true">
            发布新帖
          </button>
          <button class="secondary-action" type="button" @click="router.push(ROUTES.KNOWLEDGE)">
            返回知识库
          </button>
        </div>
      </header>

      <section class="forum-toolbar">
        <div class="toolbar-summary">
          <strong>{{ filteredPosts.length }}</strong>
          <span>篇内容</span>
          <i>{{ currentRangeText }}</i>
        </div>

        <el-input
          v-model="searchQuery"
          class="search-input"
          placeholder="搜索标题、正文、模型、数据集或知识库内容"
          clearable
          @keyup.enter="setPage(1)"
        />

        <div v-if="showPager" class="pager-chip">
          <button type="button" :disabled="boundedPage <= 1" @click="setPage(boundedPage - 1)">上一页</button>
          <span>{{ boundedPage }} / {{ totalPages }}</span>
          <button type="button" :disabled="boundedPage >= totalPages" @click="setPage(boundedPage + 1)">下一页</button>
        </div>
      </section>

      <section class="post-board" v-loading="loading">
        <article
          v-for="post in pagedPosts"
          :key="post.id"
          class="post-row"
          :class="{ pinned: post.isPinned, official: post.isOfficial }"
          @click="openPost(post)"
        >
          <div class="post-cover" :class="{ empty: !coverForPost(post) }">
            <img v-if="coverForPost(post)" :src="coverForPost(post)" :alt="post.title" loading="lazy" />
            <span v-else>{{ sourceInitial(post) }}</span>
          </div>

          <div class="post-main">
            <div class="post-meta">
              <span>{{ sourceLabel(post) }}</span>
              <span v-if="post.isOfficial">平台</span>
              <span v-if="post.isPinned">置顶</span>
              <span>浏览 {{ post.viewCount || 0 }}</span>
              <span>{{ formatTime(post.createdAt) }}</span>
            </div>

            <h2>{{ post.title }}</h2>
            <p>{{ excerptForPost(post) }}</p>
          </div>

          <button class="open-action" type="button" @click.stop="openPost(post)">
            打开
          </button>
        </article>

        <div v-if="filteredPosts.length === 0 && !loading" class="empty-state">
          <strong>没有匹配内容</strong>
          <span>换个关键词，或发布一条新的讨论。</span>
        </div>
      </section>
    </section>

    <el-dialog v-model="showCreate" title="发布新帖" width="760px" :close-on-click-modal="false">
      <el-input v-model="newTitle" placeholder="帖子标题" class="!mb-4" />
      <el-input v-model="newCoverUrl" placeholder="封面图片 URL（可选，若正文第一张图相同，详情页会自动去重）" class="!mb-4" />
      <div class="md-editor-wrapper">
        <textarea v-model="newContent" placeholder="支持 Markdown 格式..." class="md-editor" rows="15"></textarea>
        <div class="md-preview" v-html="renderMd(newContent)"></div>
      </div>
      <div class="upload-area">
        <input
          ref="fileInput"
          type="file"
          multiple
          accept=".jpg,.jpeg,.png,.gif,.webp,.svg,.bmp,.md,.txt,.csv,.json,.pdf"
          style="display: none"
          @change="onFilesSelected"
        />
        <el-button size="small" :disabled="uploading" @click="fileInput?.click()">上传文件/图片</el-button>
        <span v-if="uploading" class="upload-status">上传中...</span>
        <div v-if="uploadedFiles.length" class="upload-list">
          <span
            v-for="file in uploadedFiles"
            :key="file.url"
            class="uploaded-item"
            :title="`点击插入：${file.url}`"
            @click="insertFileUrl(file)"
          >
            {{ file.name }} 已上传
          </span>
        </div>
      </div>
      <template #footer>
        <el-button @click="showCreate = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitPost">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { forumApi } from '@/api'
import { useThemeStore } from '@/stores/theme.store'
import { renderMarkdown } from '@/utils/markdown'
import { hasStoredAuthToken } from '@/utils/authState'
import { ROUTES } from '@/constants'

type ForumPost = {
  id: number
  title: string
  content?: string
  coverUrl?: string | null
  isOfficial?: boolean
  isPinned?: boolean
  viewCount?: number
  createdAt?: string
  sourceType?: string | null
}

const router = useRouter()
const themeStore = useThemeStore()
const posts = ref<ForumPost[]>([])
const loading = ref(false)
const showCreate = ref(false)
const submitting = ref(false)
const newTitle = ref('')
const newContent = ref('')
const newCoverUrl = ref('')
const searchQuery = ref('')
const page = ref(1)
const pageSize = 12
const fileInput = ref<HTMLInputElement>()
const uploading = ref(false)
const uploadedFiles = ref<Array<{ name: string; url: string }>>([])
const isLoggedIn = computed(() => hasStoredAuthToken())

const orderedPosts = computed(() =>
  [...posts.value].sort((a, b) => {
    if (a.isPinned !== b.isPinned) return a.isPinned ? -1 : 1
    if (a.isOfficial !== b.isOfficial) return a.isOfficial ? -1 : 1
    return timeValue(b.createdAt) - timeValue(a.createdAt)
  }),
)

const filteredPosts = computed(() => {
  const keyword = searchQuery.value.trim().toLowerCase()
  if (!keyword) return orderedPosts.value
  return orderedPosts.value.filter((post) => {
    const haystack = [
      post.title,
      post.content,
      post.sourceType,
      post.isOfficial ? '平台 official platform' : '',
      post.sourceType === 'MODEL_ARTICLE' ? '模型 model recommender' : '',
      post.sourceType === 'KNOWLEDGE_ARTICLE' ? '知识库 knowledge article' : '',
    ]
      .filter(Boolean)
      .join(' ')
      .toLowerCase()
    return haystack.includes(keyword)
  })
})

const totalPages = computed(() => Math.max(1, Math.ceil(filteredPosts.value.length / pageSize)))
const boundedPage = computed(() => clamp(page.value, 1, totalPages.value))
const showPager = computed(() => filteredPosts.value.length > pageSize)

const pagedPosts = computed(() => {
  const start = (boundedPage.value - 1) * pageSize
  return filteredPosts.value.slice(start, start + pageSize)
})

const currentRangeText = computed(() => {
  if (filteredPosts.value.length === 0) return '0 / 0'
  const start = (boundedPage.value - 1) * pageSize + 1
  const end = Math.min(boundedPage.value * pageSize, filteredPosts.value.length)
  return `${start}-${end} / ${filteredPosts.value.length}`
})

const fetchPosts = async () => {
  loading.value = true
  try {
    const res = await forumApi.listPosts()
    if (res.data.code === 200) posts.value = res.data.data || []
  } catch (error) {
    console.error(error)
    ElMessage.error('论坛内容加载失败')
  } finally {
    loading.value = false
  }
}

const submitPost = async () => {
  if (!newTitle.value.trim() || !newContent.value.trim()) {
    ElMessage.warning('请填写标题和正文')
    return
  }
  submitting.value = true
  try {
    const res = await forumApi.createPost({
      title: newTitle.value.trim(),
      content: newContent.value.trim(),
      coverUrl: newCoverUrl.value.trim() || undefined,
    })
    if (res.data.code === 200) {
      showCreate.value = false
      newTitle.value = ''
      newContent.value = ''
      newCoverUrl.value = ''
      uploadedFiles.value = []
      await fetchPosts()
    } else {
      ElMessage.error(res.data.message || '发布失败')
    }
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '发布失败')
  } finally {
    submitting.value = false
  }
}

const onFilesSelected = async (event: Event) => {
  const input = event.target as HTMLInputElement
  if (!input.files?.length) return
  uploading.value = true
  try {
    const res = await forumApi.uploadFiles(Array.from(input.files))
    if (res.data.code === 200) {
      for (const file of res.data.data) uploadedFiles.value.push(file)
    } else {
      ElMessage.error(res.data.message || '上传失败')
    }
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '上传失败')
  } finally {
    uploading.value = false
    input.value = ''
  }
}

const insertFileUrl = (file: { name: string; url: string }) => {
  const ext = file.name.split('.').pop()?.toLowerCase() || ''
  const isImage = ['jpg', 'jpeg', 'png', 'gif', 'webp', 'svg', 'bmp', 'ico'].includes(ext)
  const markdown = isImage ? `![${file.name}](${file.url})` : `[${file.name}](${file.url})`
  newContent.value += `\n${markdown}\n`
}

const openPost = (post: ForumPost) => {
  router.push(`${ROUTES.FORUM}/${post.id}`)
}

const renderMd = (markdown: string) => renderMarkdown(markdown)

const sourceLabel = (post: ForumPost) => {
  if (post.sourceType === 'MODEL_ARTICLE') return '模型文章'
  if (post.sourceType === 'KNOWLEDGE_ARTICLE') return '知识库文章'
  return post.isOfficial ? '平台发布' : '社区讨论'
}

const sourceInitial = (post: ForumPost) => {
  if (post.sourceType === 'MODEL_ARTICLE') return 'M'
  if (post.sourceType === 'KNOWLEDGE_ARTICLE') return 'K'
  return post.isOfficial ? 'D' : '#'
}

const coverForPost = (post: ForumPost) => {
  const explicitCover = post.coverUrl?.trim()
  if (explicitCover) return explicitCover
  return firstMarkdownImage(post.content || '')
}

const firstMarkdownImage = (markdown: string) => {
  const match = markdown.match(/!\[[^\]]*]\(([^)\s]+)(?:\s+['"][^'"]*['"])?\)/)
  return match?.[1]?.trim() || ''
}

const excerptForPost = (post: ForumPost) => {
  const plain = stripMarkdown(post.content || '')
  return plain.length > 128 ? `${plain.slice(0, 128)}...` : plain || '进入帖子查看完整内容与评论。'
}

const stripMarkdown = (markdown: string) =>
  markdown
    .replace(/!\[[^\]]*]\([^)]+\)/g, '')
    .replace(/\[([^\]]+)]\([^)]+\)/g, '$1')
    .replace(/[`*_#>|-]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()

const formatTime = (value?: string) => {
  if (!value) return '暂无时间'
  return new Date(value).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  })
}

const timeValue = (value?: string) => {
  const time = value ? new Date(value).getTime() : 0
  return Number.isFinite(time) ? time : 0
}

const clamp = (value: number, min: number, max: number) => Math.min(Math.max(value, min), max)
const setPage = (nextPage: number) => {
  page.value = clamp(Math.trunc(nextPage) || 1, 1, totalPages.value)
}

watch(searchQuery, () => {
  page.value = 1
})

watch(totalPages, (value) => {
  setPage(Math.min(page.value, value))
})

onMounted(fetchPosts)
</script>

<style scoped>
.forum-page {
  min-height: 100vh;
  --forum-panel: rgba(var(--glass-bg-rgb), 0.62);
  --forum-panel-strong: rgba(var(--glass-bg-rgb), 0.78);
  --forum-line: color-mix(in srgb, var(--primary-color) 15%, var(--border-color));
  --forum-line-strong: color-mix(in srgb, var(--primary-color) 34%, var(--border-color));
  --forum-muted: var(--text-secondary);
  --forum-soft: color-mix(in srgb, var(--primary-color) 8%, var(--surface-2));
  color: var(--text-primary);
  background:
    radial-gradient(circle at 12% 8%, rgba(var(--primary-rgb), 0.12), transparent 32%),
    radial-gradient(circle at 84% 16%, color-mix(in srgb, var(--accent-glow) 12%, transparent), transparent 34%),
    linear-gradient(180deg, color-mix(in srgb, var(--bg-color) 94%, rgba(var(--primary-rgb), 0.08)), var(--bg-color));
}

.forum-shell {
  width: min(1220px, calc(100vw - 48px));
  margin: 0 auto;
  padding: 42px 0 72px;
}

.vertical-nav .forum-shell {
  width: min(1120px, calc(100vw - 288px));
}

.forum-hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 22px;
}

.hero-copy {
  min-width: 0;
}

.eyebrow {
  display: inline-flex;
  margin-bottom: 12px;
  color: color-mix(in srgb, var(--primary-color) 82%, var(--text-primary));
  font-size: 12px;
  font-weight: var(--font-weight-label);
}

.forum-hero h1 {
  margin: 0;
  font-size: clamp(38px, 5vw, 72px);
  line-height: 0.98;
  font-weight: var(--font-weight-title);
  letter-spacing: 0;
}

.forum-hero p {
  max-width: 680px;
  margin: 16px 0 0;
  color: var(--forum-muted);
  font-size: 15px;
  line-height: 1.75;
}

.forum-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 12px;
}

.primary-action,
.secondary-action,
.open-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--forum-line);
  border-radius: var(--radius-sm);
  cursor: pointer;
  font: inherit;
  font-size: 14px;
  font-weight: var(--font-weight-label);
  transition: transform 180ms var(--ease-smooth), border-color 180ms ease, background 180ms ease, box-shadow 180ms ease;
}

.primary-action,
.secondary-action {
  min-height: 44px;
  padding: 0 18px;
}

.primary-action {
  border-color: color-mix(in srgb, var(--primary-color) 68%, var(--border-color));
  background: var(--primary-color);
  color: var(--text-inverse);
  box-shadow: 0 14px 34px rgba(var(--primary-rgb), 0.24);
}

.secondary-action {
  background: var(--forum-panel);
  color: var(--text-primary);
  backdrop-filter: blur(16px) saturate(140%);
}

.primary-action:hover,
.secondary-action:hover,
.open-action:hover {
  transform: translateY(-1px);
  border-color: var(--forum-line-strong);
  box-shadow: var(--shadow-hover);
}

.forum-toolbar {
  display: grid;
  grid-template-columns: auto minmax(280px, 1fr) auto;
  gap: 14px;
  align-items: center;
  margin-bottom: 14px;
  padding: 14px;
  border: 1px solid var(--forum-line);
  border-radius: var(--radius-lg);
  background: var(--forum-panel);
  backdrop-filter: blur(18px) saturate(145%);
  box-shadow: var(--shadow-soft);
}

.toolbar-summary {
  display: inline-flex;
  align-items: baseline;
  gap: 7px;
  white-space: nowrap;
}

.toolbar-summary strong {
  color: var(--primary-color);
  font-size: 24px;
  line-height: 1;
}

.toolbar-summary span,
.toolbar-summary i {
  color: var(--forum-muted);
  font-size: 12px;
  font-style: normal;
  font-weight: var(--font-weight-body);
}

.search-input :deep(.el-input__wrapper) {
  min-height: 42px;
  border-radius: var(--radius-sm);
  background: color-mix(in srgb, var(--surface-2) 74%, transparent);
  box-shadow: 0 0 0 1px var(--forum-line);
}

.search-input :deep(.el-input__inner) {
  color: var(--text-primary);
}

.pager-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.pager-chip button {
  min-height: 36px;
  border: 1px solid var(--forum-line);
  border-radius: var(--radius-sm);
  background: var(--forum-soft);
  color: var(--text-primary);
  cursor: pointer;
  font-size: 12px;
  font-weight: var(--font-weight-label);
  padding: 0 12px;
}

.pager-chip button:disabled {
  cursor: not-allowed;
  opacity: 0.42;
}

.pager-chip span {
  min-width: 54px;
  color: var(--forum-muted);
  font-size: 12px;
  text-align: center;
}

.post-board {
  position: relative;
  min-height: 420px;
  border: 1px solid var(--forum-line);
  border-radius: var(--radius-lg);
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--surface-1) 84%, transparent), color-mix(in srgb, var(--surface-2) 48%, transparent)),
    var(--forum-panel);
  backdrop-filter: blur(18px) saturate(140%);
  box-shadow: 0 22px 70px rgba(0, 0, 0, 0.16), inset 0 1px 0 rgba(255, 255, 255, 0.06);
  overflow: hidden;
}

.post-row {
  display: grid;
  grid-template-columns: 128px minmax(0, 1fr) auto;
  gap: 18px;
  align-items: center;
  min-height: 136px;
  padding: 20px 22px;
  border-bottom: 1px solid color-mix(in srgb, var(--forum-line) 72%, transparent);
  cursor: pointer;
  transition: background 180ms ease, transform 180ms var(--ease-smooth), border-color 180ms ease;
}

.post-row:last-child {
  border-bottom: none;
}

.post-row:hover {
  background: color-mix(in srgb, var(--primary-color) 7%, transparent);
  transform: translateY(-1px);
}

.post-row.pinned {
  background:
    linear-gradient(90deg, color-mix(in srgb, var(--primary-color) 7%, transparent), transparent 44%);
}

.post-cover {
  display: grid;
  width: 128px;
  height: 86px;
  place-items: center;
  overflow: hidden;
  border: 1px solid var(--forum-line);
  border-radius: var(--radius-sm);
  background:
    linear-gradient(135deg, color-mix(in srgb, var(--primary-color) 18%, transparent), transparent 62%),
    var(--forum-soft);
  color: var(--primary-color);
  font-size: 22px;
  font-weight: var(--font-weight-title);
}

.post-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.post-cover.empty {
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.08);
}

.post-main {
  min-width: 0;
}

.post-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  margin-bottom: 8px;
}

.post-meta span {
  display: inline-flex;
  align-items: center;
  min-height: 22px;
  padding: 0 8px;
  border: 1px solid color-mix(in srgb, var(--primary-color) 12%, transparent);
  border-radius: 999px;
  background: color-mix(in srgb, var(--surface-2) 72%, transparent);
  color: var(--forum-muted);
  font-size: 11px;
  font-weight: var(--font-weight-body);
}

.post-main h2 {
  margin: 0;
  overflow: hidden;
  color: var(--text-primary);
  font-size: clamp(18px, 2vw, 24px);
  font-weight: var(--font-weight-title);
  line-height: 1.26;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.post-main p {
  display: -webkit-box;
  margin: 8px 0 0;
  overflow: hidden;
  color: var(--forum-muted);
  font-size: 14px;
  line-height: 1.72;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.open-action {
  min-height: 38px;
  padding: 0 14px;
  background: color-mix(in srgb, var(--primary-color) 9%, var(--surface-2));
  color: var(--text-primary);
}

.empty-state {
  display: grid;
  min-height: 360px;
  place-items: center;
  align-content: center;
  gap: 8px;
  color: var(--forum-muted);
}

.empty-state strong {
  color: var(--text-primary);
  font-size: 18px;
}

.md-editor-wrapper {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.md-editor {
  width: 100%;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  padding: 12px;
  background: var(--bg-input);
  color: var(--text-primary);
  font-family: "JetBrains Mono", Consolas, monospace;
  font-size: 13px;
  resize: vertical;
}

.md-preview {
  max-height: 400px;
  overflow-y: auto;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  padding: 12px;
  color: var(--text-primary);
  font-size: 13px;
  line-height: 1.7;
}

.upload-area {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
}

.upload-status {
  color: var(--primary-color);
  font-size: 12px;
}

.upload-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.uploaded-item {
  padding: 4px 9px;
  border-radius: var(--radius-sm);
  background: color-mix(in srgb, var(--primary-color) 10%, transparent);
  color: var(--primary-color);
  cursor: pointer;
  font-size: 12px;
}

@media (max-width: 1080px) {
  .forum-shell,
  .vertical-nav .forum-shell {
    width: min(100vw - 24px, 820px);
    padding-top: 28px;
  }

  .forum-hero,
  .forum-toolbar {
    display: grid;
    grid-template-columns: 1fr;
  }

  .forum-actions {
    justify-content: flex-start;
  }

  .forum-toolbar {
    align-items: stretch;
  }
}

@media (max-width: 700px) {
  .post-row {
    grid-template-columns: 92px minmax(0, 1fr);
    gap: 12px;
    padding: 18px;
  }

  .post-cover {
    width: 92px;
    height: 68px;
  }

  .open-action {
    grid-column: 2;
    width: fit-content;
  }

  .post-main h2 {
    white-space: normal;
  }

  .md-editor-wrapper {
    grid-template-columns: 1fr;
  }
}
</style>
