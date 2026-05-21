<template>
  <div class="forum-page" :class="{ 'vertical-nav': !themeStore.isHorizontalMenu }">
    <section class="forum-shell">
      <header class="forum-hero">
        <div>
          <span class="eyebrow">
            <ShinyText
              text="DeepInsight Community"
              color="rgba(248, 253, 255, 0.86)"
              shine-color="var(--primary-color)"
              :speed="2.4"
              :delay="1.4"
              :spread="118"
            />
          </span>
          <h1>社区论坛</h1>
          <p>官方文章、模型卡片与社区讨论汇聚成一个可拖拽的知识穹顶。点击封面先预览，再点击进入正文。</p>
        </div>
        <div class="forum-actions">
          <el-button class="!rounded-xl !font-bold" type="primary" @click="showCreate = true" v-if="isLoggedIn">
            发布新帖
          </el-button>
          <el-button class="!rounded-xl !font-bold" @click="$router.push('/knowledge')">返回知识图谱</el-button>
        </div>
      </header>

      <div class="gallery-panel" v-loading="loading">
        <div class="gallery-copy">
          <span>{{ filteredPosts.length }} 篇匹配内容 · {{ viewMode === 'gallery' ? galleryRangeText : currentRangeText }}</span>
          <strong>
            <ShinyText
              :text="viewMode === 'gallery' ? '水平拖动旋转，点击放大' : '正常阅读模式'"
              color="rgba(248, 253, 255, 0.92)"
              shine-color="var(--primary-color)"
              :speed="2.2"
              :delay="0.9"
              :spread="126"
              pause-on-hover
            />
          </strong>
          <small>{{ viewMode === 'gallery' ? '画廊会循环填满圆顶，上限 175 张封面' : '正常模式每页最多 12 篇文章' }}</small>
        </div>

        <div class="gallery-tools">
          <el-input
            v-model="searchQuery"
            class="search-input"
            placeholder="搜索标题、概述、官方/模型/知识库..."
            clearable
            @keyup.enter="setPage(1)"
          />
          <div v-if="showPager" class="pager-chip">
            <button type="button" :disabled="boundedPage <= 1" @click="setPage(boundedPage - 1)">上一页</button>
            <span>{{ boundedPage }} / {{ totalPages }}</span>
            <button type="button" :disabled="boundedPage >= totalPages" @click="setPage(boundedPage + 1)">下一页</button>
          </div>
          <div class="mode-switch" :class="`is-${viewMode}`" role="tablist" aria-label="论坛展示模式">
            <span class="mode-thumb" />
            <button type="button" :class="{ active: viewMode === 'gallery' }" @click="viewMode = 'gallery'">画廊</button>
            <button type="button" :class="{ active: viewMode === 'list' }" @click="viewMode = 'list'">正常</button>
          </div>
        </div>

        <transition name="mode-slide" mode="out-in">
          <main
            v-if="viewMode === 'gallery'"
            key="gallery"
            ref="stageRef"
            class="dome-main"
            :style="domeStyle"
            @pointerdown="onPointerDown"
            @click.self="closePreview"
          >
            <div class="dome-stage">
              <div class="dome-sphere" :style="sphereStyle">
                <article
                  v-for="item in galleryItems"
                  :key="item.key"
                  class="dome-item"
                  :class="{ 'is-selected': selectedPost?.id === item.post.id }"
                  :data-post-id="item.post.id"
                  :style="item.style"
                >
                  <button
                    class="dome-tile"
                    type="button"
                    @click.stop="openPreview(item)"
                    @pointermove.stop="onTilePointerMove"
                    @pointerleave="resetTileTilt"
                  >
                    <img :src="item.cover" :alt="item.post.title" draggable="false" />
                    <span class="tile-glint" />
                    <span class="tile-shade" />
                    <span class="tile-label">
                      <em>{{ item.subtitle }}</em>
                      <strong>{{ item.shortTitle }}</strong>
                    </span>
                    <span v-if="item.post.isOfficial" class="tile-badge">官方</span>
                  </button>
                </article>
              </div>
            </div>

            <div class="dome-overlay" />
            <div class="dome-overlay dome-overlay-blur" />
            <div class="edge-fade edge-fade-top" />
            <div class="edge-fade edge-fade-bottom" />

            <transition name="preview">
              <div v-if="selectedItem" class="preview-layer" @click.self="closePreview">
                <button class="preview-card" type="button" @click="enterSelectedPost">
                  <img :src="selectedItem.cover" :alt="selectedItem.post.title" draggable="false" />
                  <span class="preview-gradient" />
                  <span class="preview-content">
                    <span class="preview-kicker">
                      {{ selectedItem.post.isOfficial ? '官方精选' : '社区讨论' }}
                      <i>浏览 {{ selectedItem.post.viewCount || 0 }}</i>
                    </span>
                    <strong>{{ selectedItem.post.title }}</strong>
                    <em>{{ selectedItem.subtitle }}</em>
                    <p>{{ selectedItem.excerpt }}</p>
                    <span class="preview-cta">再次点击进入文章</span>
                  </span>
                </button>
              </div>
            </transition>
          </main>

          <div v-else key="list" class="normal-list">
            <article v-for="item in galleryItems" :key="item.key" class="normal-card" @click="router.push(`/forum/${item.post.id}`)">
              <img :src="item.cover" :alt="item.post.title" />
              <div class="normal-body">
                <div class="normal-meta">
                  <span>{{ item.subtitle }}</span>
                  <span v-if="item.post.isOfficial">官方</span>
                  <span v-if="item.post.isPinned">置顶</span>
                  <span>浏览 {{ item.post.viewCount || 0 }}</span>
                </div>
                <h2>{{ item.post.title }}</h2>
                <p>{{ item.excerpt }}</p>
              </div>
              <span class="normal-arrow">进入</span>
            </article>
          </div>
        </transition>

        <div v-if="filteredPosts.length === 0 && !loading" class="empty">没有匹配的文章，换个关键词试试。</div>
      </div>
    </section>

    <el-dialog v-model="showCreate" title="发布新帖" width="760px" :close-on-click-modal="false">
      <el-input v-model="newTitle" placeholder="帖子标题" class="!mb-4" />
      <el-input v-model="newCoverUrl" placeholder="封面图片 URL（可选）" class="!mb-4" />
      <div class="md-editor-wrapper">
        <textarea v-model="newContent" placeholder="支持 Markdown 格式..." class="md-editor" rows="15"></textarea>
        <div class="md-preview" v-html="renderMd(newContent)"></div>
      </div>
      <div class="upload-area">
        <input
          type="file"
          ref="fileInput"
          multiple
          accept=".jpg,.jpeg,.png,.gif,.webp,.svg,.bmp,.md,.txt,.csv,.json,.pdf"
          @change="onFilesSelected"
          style="display:none"
        />
        <el-button size="small" @click="fileInput?.click()" :disabled="uploading">上传文件/图片</el-button>
        <span v-if="uploading" class="upload-status">上传中...</span>
        <div v-if="uploadedFiles.length" class="upload-list">
          <span
            v-for="f in uploadedFiles"
            :key="f.url"
            class="uploaded-item"
            @click="insertFileUrl(f)"
            :title="'点击插入: ' + f.url"
          >
            {{ f.name }} ✓
          </span>
        </div>
      </div>
      <template #footer>
        <el-button @click="showCreate = false">取消</el-button>
        <el-button type="primary" @click="submitPost" :loading="submitting">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { forumApi } from '@/api'
import { useThemeStore } from '@/stores/theme.store'
import { renderMarkdown } from '@/utils/markdown'
import { hasStoredAuthToken } from '@/utils/authState'
import ShinyText from '@/components/effects/ShinyText.vue'

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

type GalleryItem = {
  key: string
  post: ForumPost
  cover: string
  subtitle: string
  shortTitle: string
  excerpt: string
  style: Record<string, string | number>
}

const router = useRouter()
const themeStore = useThemeStore()
const stageRef = ref<HTMLElement>()
const posts = ref<ForumPost[]>([])
const loading = ref(false)
const showCreate = ref(false)
const submitting = ref(false)
const newTitle = ref('')
const newContent = ref('')
const newCoverUrl = ref('')
const isLoggedIn = computed(() => hasStoredAuthToken())
const fileInput = ref<HTMLInputElement>()
const uploading = ref(false)
const uploadedFiles = ref<Array<{ name: string; url: string }>>([])
const selectedItem = ref<GalleryItem | null>(null)
const selectedPost = computed(() => selectedItem.value?.post || null)
const searchQuery = ref('')
const page = ref(1)
const pageSize = 12
const forumViewModeKey = 'deepinsight-forum-view-mode'
const savedViewMode = sessionStorage.getItem(forumViewModeKey)
const viewMode = ref<'gallery' | 'list'>(savedViewMode === 'list' ? 'list' : 'gallery')
const segments = 35
const galleryRowsPerColumn = 5
const galleryLimit = segments * galleryRowsPerColumn
const rotation = ref({ x: 0, y: 0 })
const dragState = ref({
  active: false,
  moved: false,
  startX: 0,
  startY: 0,
  startRotX: 0,
  startRotY: 0,
  lastX: 0,
  lastY: 0,
  lastT: 0,
  vx: 0,
  vy: 0,
})
let inertiaFrame = 0

const coverPool = [
  'https://images.unsplash.com/photo-1677442136019-21780ecad995?w=900&h=1200&fit=crop',
  'https://images.unsplash.com/photo-1620712943543-bcc4688e7485?w=900&h=1200&fit=crop',
  'https://images.unsplash.com/photo-1555949963-ff9fe0c870eb?w=900&h=1200&fit=crop',
  'https://images.unsplash.com/photo-1518770660439-4636190af475?w=900&h=1200&fit=crop',
  'https://images.unsplash.com/photo-1558494949-ef010cbdcc31?w=900&h=1200&fit=crop',
  'https://images.unsplash.com/photo-1639322537228-f710d846310a?w=900&h=1200&fit=crop',
  'https://images.unsplash.com/photo-1509228627152-72ae9ae6848d?w=900&h=1200&fit=crop',
  'https://images.unsplash.com/photo-1591453089816-0fbb971b454c?w=900&h=1200&fit=crop',
  'https://images.unsplash.com/photo-1555255707-c07966088b7b?w=900&h=1200&fit=crop',
  'https://images.unsplash.com/photo-1485827404703-89b55fcc595e?w=900&h=1200&fit=crop',
  'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?w=900&h=1200&fit=crop',
  'https://images.unsplash.com/photo-1542831371-29b0f74f9713?w=900&h=1200&fit=crop',
]

const filteredPosts = computed(() => {
  const keyword = searchQuery.value.trim().toLowerCase()
  if (!keyword) return posts.value
  return posts.value.filter(post => {
    const haystack = [
      post.title,
      post.content,
      post.sourceType,
      post.isOfficial ? '官方 official' : '',
      post.sourceType === 'MODEL_ARTICLE' ? '模型 model' : '',
      post.sourceType === 'KNOWLEDGE_ARTICLE' ? '知识库 knowledge' : '',
    ]
      .filter(Boolean)
      .join(' ')
      .toLowerCase()
    return haystack.includes(keyword)
  })
})

const totalPages = computed(() => Math.max(1, Math.ceil(filteredPosts.value.length / pageSize)))
const showPager = computed(() => viewMode.value === 'list' && filteredPosts.value.length > pageSize)
const boundedPage = computed(() => clamp(page.value, 1, totalPages.value))

const pagedPosts = computed(() => {
  const start = (boundedPage.value - 1) * pageSize
  return filteredPosts.value.slice(start, start + pageSize)
})

const galleryPosts = computed(() => {
  const source = filteredPosts.value
  if (!source.length) return []
  const used = Array.from({ length: galleryLimit }, (_, index) => source[index % source.length])
  for (let index = 1; index < used.length; index++) {
    if (used[index]?.id !== used[index - 1]?.id) continue
    for (let swapIndex = index + 1; swapIndex < used.length; swapIndex++) {
      if (used[swapIndex]?.id === used[index]?.id) continue
      const temp = used[index]
      used[index] = used[swapIndex]
      used[swapIndex] = temp
      break
    }
  }
  return used
})

const currentRangeText = computed(() => {
  if (filteredPosts.value.length === 0) return '0 篇'
  const start = (boundedPage.value - 1) * pageSize + 1
  const end = Math.min(boundedPage.value * pageSize, filteredPosts.value.length)
  return `${start}-${end} / ${filteredPosts.value.length}`
})

const galleryRangeText = computed(() => {
  if (filteredPosts.value.length === 0) return '0 张'
  return `${galleryPosts.value.length} 张封面槽 / 上限 ${galleryLimit}`
})

const domeStyle = computed(() => ({
  '--segments-x': segments,
  '--segments-y': segments,
  '--radius': themeStore.isHorizontalMenu ? '700px' : '600px',
  '--overlay-blur-color': 'var(--forum-deep)',
  '--tile-radius': '30px',
}))

const sphereStyle = computed(() => ({
  transform: `translateZ(calc(var(--radius) * -1)) rotateY(${rotation.value.y}deg)`,
}))

const galleryItems = computed<GalleryItem[]>(() => {
  const pool = viewMode.value === 'gallery' ? galleryPosts.value : pagedPosts.value
  const xCols = Array.from({ length: segments }, (_, index) => -37 + index * 2)
  const evenYs = [-4, -2, 0, 2, 4]
  const oddYs = [-3, -1, 1, 3, 5]
  const coords = xCols.flatMap((x, column) => {
    const ys = column % 2 === 0 ? evenYs : oddYs
    return ys.map(y => ({ x, y, sizeX: 2, sizeY: 2 }))
  })

  return pool.slice(0, coords.length).map((post, index) => {
    const coord = coords[index]
    return {
      key: `${post.id}-${index}-${viewMode.value}`,
      post,
      cover: coverForPost(post),
      subtitle: subtitleForPost(post),
      shortTitle: compactTitle(post.title),
      excerpt: excerptForPost(post),
      style: {
        '--offset-x': coord.x,
        '--offset-y': coord.y,
        '--item-size-x': coord.sizeX,
        '--item-size-y': coord.sizeY,
        '--accent': accentForPost(post, index),
      },
    }
  })
})

const onFilesSelected = async (event: Event) => {
  const input = event.target as HTMLInputElement
  if (!input.files?.length) return
  uploading.value = true
  try {
    const res = await forumApi.uploadFiles(Array.from(input.files))
    if (res.data.code === 200) {
      for (const file of res.data.data) uploadedFiles.value.push(file)
    }
  } catch (error) {
    console.error(error)
  }
  uploading.value = false
  input.value = ''
}

const insertFileUrl = (file: { name: string; url: string }) => {
  const ext = file.name.split('.').pop()?.toLowerCase() || ''
  const isImg = ['jpg', 'jpeg', 'png', 'gif', 'webp', 'svg', 'bmp', 'ico'].includes(ext)
  const md = isImg ? `![${file.name}](${file.url})` : `[${file.name}](${file.url})`
  newContent.value += `\n${md}\n`
}

const fetchPosts = async () => {
  loading.value = true
  try {
    const res = await forumApi.listPosts()
    if (res.data.code === 200) {
      posts.value = res.data.data || []
    }
  } catch (error) {
    console.error(error)
  }
  loading.value = false
}

const submitPost = async () => {
  if (!newTitle.value.trim() || !newContent.value.trim()) return
  submitting.value = true
  try {
    const res = await forumApi.createPost({
      title: newTitle.value,
      content: newContent.value,
      coverUrl: newCoverUrl.value || undefined,
    })
    if (res.data.code === 200) {
      showCreate.value = false
      newTitle.value = ''
      newContent.value = ''
      newCoverUrl.value = ''
      uploadedFiles.value = []
      fetchPosts()
    } else {
      ElMessage.error(res.data.message || '发布失败')
    }
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '发布失败')
  }
  submitting.value = false
}

const openPreview = (item: GalleryItem) => {
  if (dragState.value.moved) return
  selectedItem.value = item
  document.body.classList.add('dg-scroll-lock')
}

const closePreview = () => {
  selectedItem.value = null
  document.body.classList.remove('dg-scroll-lock')
}

const enterSelectedPost = () => {
  if (!selectedItem.value) return
  router.push(`/forum/${selectedItem.value.post.id}`)
}

const onPointerDown = (event: PointerEvent) => {
  if (selectedItem.value || event.button !== 0) return
  cancelAnimationFrame(inertiaFrame)
  dragState.value = {
    active: true,
    moved: false,
    startX: event.clientX,
    startY: event.clientY,
    startRotX: 0,
    startRotY: rotation.value.y,
    lastX: event.clientX,
    lastY: event.clientY,
    lastT: performance.now(),
    vx: 0,
    vy: 0,
  }
  window.addEventListener('pointermove', onPointerMove)
  window.addEventListener('pointerup', onPointerUp, { once: true })
}

const onPointerMove = (event: PointerEvent) => {
  if (!dragState.value.active) return
  const dx = event.clientX - dragState.value.startX
  const dy = event.clientY - dragState.value.startY
  if (dx * dx + dy * dy > 16) dragState.value.moved = true

  rotation.value = {
    x: 0,
    y: wrapAngle(dragState.value.startRotY + dx / 16),
  }

  const now = performance.now()
  const dt = Math.max(16, now - dragState.value.lastT)
  dragState.value.vx = (event.clientX - dragState.value.lastX) / dt
  dragState.value.vy = (event.clientY - dragState.value.lastY) / dt
  dragState.value.lastX = event.clientX
  dragState.value.lastY = event.clientY
  dragState.value.lastT = now
}

const onPointerUp = () => {
  window.removeEventListener('pointermove', onPointerMove)
  dragState.value.active = false
  if (dragState.value.moved) startInertia(dragState.value.vx)
  window.setTimeout(() => {
    dragState.value.moved = false
  }, 120)
}

const startInertia = (vx: number) => {
  let speedX = clamp(vx * 18, -1.6, 1.6)
  const step = () => {
    speedX *= 0.95
    rotation.value = {
      x: 0,
      y: wrapAngle(rotation.value.y + speedX),
    }
    if (Math.abs(speedX) > 0.02) {
      inertiaFrame = requestAnimationFrame(step)
    }
  }
  inertiaFrame = requestAnimationFrame(step)
}

const onTilePointerMove = (event: PointerEvent) => {
  const tile = event.currentTarget as HTMLElement
  const rect = tile.getBoundingClientRect()
  const px = (event.clientX - rect.left) / rect.width
  const py = (event.clientY - rect.top) / rect.height
  const rotateY = (px - 0.5) * 18
  const rotateX = (0.5 - py) * 18
  tile.style.setProperty('--tilt-x', `${rotateX}deg`)
  tile.style.setProperty('--tilt-y', `${rotateY}deg`)
  tile.style.setProperty('--glint-x', `${px * 100}%`)
  tile.style.setProperty('--glint-y', `${py * 100}%`)
}

const resetTileTilt = (event: PointerEvent) => {
  const tile = event.currentTarget as HTMLElement
  tile.style.setProperty('--tilt-x', '0deg')
  tile.style.setProperty('--tilt-y', '0deg')
  tile.style.setProperty('--glint-x', '50%')
  tile.style.setProperty('--glint-y', '50%')
}

const renderMd = (md: string) => renderMarkdown(md)

const coverForPost = (post: ForumPost) => {
  if (post.coverUrl) return post.coverUrl
  const markdownCover = post.content?.match(/!\[[^\]]*]\(([^)\s]+)(?:\s+"[^"]*")?\)/)?.[1]
  if (markdownCover) return markdownCover
  return coverPool[Math.abs(hashText(`${post.sourceType || 'forum'}-${post.id}-${post.title}`)) % coverPool.length]
}

const subtitleForPost = (post: ForumPost) => {
  if (post.sourceType === 'MODEL_ARTICLE') return '模型文章'
  if (post.sourceType === 'KNOWLEDGE_ARTICLE') return '知识库官方'
  if (post.isPinned) return '置顶讨论'
  return post.isOfficial ? '官方精选' : '社区投稿'
}

const excerptForPost = (post: ForumPost) => {
  const plain = stripMarkdown(post.content || '')
  return plain.length > 86 ? `${plain.slice(0, 86)}...` : plain || '点击进入查看完整内容与讨论。'
}

const compactTitle = (title: string) => (title.length > 18 ? `${title.slice(0, 18)}...` : title)

const stripMarkdown = (md: string) =>
  md
    .replace(/!\[[^\]]*]\([^)]+\)/g, '')
    .replace(/\[[^\]]+]\([^)]+\)/g, match => match.replace(/^\[|\]\([^)]+\)$/g, ''))
    .replace(/[`*_#>|-]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()

const accentForPost = (post: ForumPost, index: number) => {
  if (post.sourceType === 'MODEL_ARTICLE') return 'var(--primary-color)'
  if (post.sourceType === 'KNOWLEDGE_ARTICLE') return 'color-mix(in srgb, var(--primary-color) 72%, #67e8f9)'
  return [
    'var(--primary-color)',
    'color-mix(in srgb, var(--primary-color) 78%, #67e8f9)',
    'color-mix(in srgb, var(--primary-color) 60%, #f6a15d)',
    'color-mix(in srgb, var(--primary-color) 56%, #a7f3d0)',
  ][index % 4]
}

const hashText = (value: string) => {
  let hash = 0
  for (let index = 0; index < value.length; index++) {
    hash = (hash << 5) - hash + value.charCodeAt(index)
    hash |= 0
  }
  return hash
}

const clamp = (value: number, min: number, max: number) => Math.min(Math.max(value, min), max)
const wrapAngle = (value: number) => ((((value + 180) % 360) + 360) % 360) - 180
const setPage = (nextPage: number) => {
  page.value = clamp(Math.trunc(nextPage) || 1, 1, totalPages.value)
}

const onKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Escape') closePreview()
}

watch(searchQuery, () => {
  page.value = 1
  closePreview()
})

watch(totalPages, value => {
  setPage(Math.min(page.value, value))
})

watch(page, () => {
  closePreview()
  cancelAnimationFrame(inertiaFrame)
  rotation.value = { x: 0, y: 0 }
})

watch(viewMode, () => {
  sessionStorage.setItem(forumViewModeKey, viewMode.value)
  closePreview()
  cancelAnimationFrame(inertiaFrame)
  rotation.value = { x: 0, y: 0 }
})

onMounted(() => {
  fetchPosts()
  window.addEventListener('keydown', onKeydown)
})

onUnmounted(() => {
  cancelAnimationFrame(inertiaFrame)
  window.removeEventListener('keydown', onKeydown)
  window.removeEventListener('pointermove', onPointerMove)
  document.body.classList.remove('dg-scroll-lock')
})
</script>

<style scoped>
.forum-page {
  min-height: 100vh;
  --forum-deep: color-mix(in srgb, var(--bg-color) 90%, #02070a);
  --forum-panel: rgba(var(--glass-bg-rgb), 0.76);
  --forum-panel-strong: rgba(var(--glass-bg-rgb), 0.9);
  --forum-line: color-mix(in srgb, var(--primary-color) 18%, var(--border-color));
  --forum-line-strong: color-mix(in srgb, var(--primary-color) 38%, var(--border-color));
  --forum-text: var(--text-primary);
  --forum-muted: var(--text-secondary);
  --forum-accent: var(--primary-color);
  --forum-surface: color-mix(in srgb, var(--surface-1) 78%, transparent);
  background:
    radial-gradient(circle at 16% 12%, rgba(var(--primary-rgb), 0.18), transparent 34%),
    radial-gradient(circle at 82% 18%, rgba(var(--primary-rgb), 0.1), transparent 30%),
    linear-gradient(90deg, rgba(var(--primary-rgb), 0.035) 1px, transparent 1px),
    linear-gradient(180deg, rgba(var(--primary-rgb), 0.032) 1px, transparent 1px),
    linear-gradient(145deg, color-mix(in srgb, var(--bg-color) 94%, rgba(var(--primary-rgb), 0.12)) 0%, var(--bg-color) 52%, var(--forum-deep) 100%);
  background-size: auto, auto, 48px 48px, 48px 48px, auto;
  color: var(--forum-text);
  animation: forum-page-arrive 620ms cubic-bezier(0.16, 1, 0.3, 1) both;
  transform-origin: 50% 0%;
}

.forum-shell {
  width: min(1380px, calc(100vw - 48px));
  margin: 0 auto;
  padding: 46px 0 70px;
  animation: forum-shell-rise 720ms cubic-bezier(0.16, 1, 0.3, 1) 70ms both;
}

.vertical-nav .forum-shell {
  width: min(1180px, calc(100vw - 288px));
  padding: 34px 0 56px;
}

.forum-hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 24px;
  animation: forum-hero-in 680ms cubic-bezier(0.16, 1, 0.3, 1) 120ms both;
}

.eyebrow {
  display: inline-flex;
  margin-bottom: 10px;
  color: var(--forum-accent);
  font-size: 12px;
  font-weight: var(--font-weight-title);
  letter-spacing: 0.24em;
  text-transform: uppercase;
}

.forum-hero h1 {
  margin: 0;
  font-size: clamp(42px, 7vw, 104px);
  line-height: 0.88;
  font-weight: var(--font-weight-title);
  letter-spacing: -0.08em;
}

.forum-hero p {
  max-width: 620px;
  margin: 20px 0 0;
  color: var(--forum-muted);
  font-size: 15px;
  line-height: 1.8;
}

.forum-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 12px;
  animation: forum-tools-in 620ms cubic-bezier(0.16, 1, 0.3, 1) 220ms both;
}

.gallery-panel {
  position: relative;
  overflow: hidden;
  min-height: 820px;
  border: 1px solid var(--forum-line);
  border-radius: 34px;
  background:
    radial-gradient(circle at 8% 0%, rgba(var(--primary-rgb), 0.13), transparent 36%),
    linear-gradient(90deg, rgba(var(--primary-rgb), 0.06) 1px, transparent 1px) 0 0 / 72px 72px,
    linear-gradient(0deg, rgba(var(--primary-rgb), 0.045) 1px, transparent 1px) 0 0 / 72px 72px,
    var(--forum-panel);
  box-shadow: 0 36px 110px rgba(0, 0, 0, 0.36), 0 0 0 1px rgba(var(--primary-rgb), 0.06);
  animation: forum-panel-in 760ms cubic-bezier(0.16, 1, 0.3, 1) 220ms both;
}

.gallery-copy {
  position: absolute;
  z-index: 8;
  top: 28px;
  left: 32px;
  display: grid;
  gap: 6px;
  pointer-events: none;
  animation: forum-tools-in 620ms cubic-bezier(0.16, 1, 0.3, 1) 420ms both;
}

.gallery-copy span,
.gallery-copy small {
  color: var(--forum-muted);
  font-size: 12px;
  font-weight: var(--font-weight-body);
}

.gallery-copy strong {
  font-size: 20px;
  letter-spacing: -0.04em;
}

.gallery-tools {
  position: absolute;
  z-index: 9;
  top: 26px;
  right: 28px;
  display: flex;
  align-items: center;
  gap: 12px;
  justify-content: flex-end;
  animation: forum-tools-in 620ms cubic-bezier(0.16, 1, 0.3, 1) 480ms both;
}

.vertical-nav .gallery-tools {
  top: 24px;
  right: 24px;
  width: min(620px, calc(100% - 360px));
  flex-wrap: nowrap;
  padding-right: 0;
}

.vertical-nav .gallery-tools .pager-chip {
  order: 2;
  margin-left: auto;
}

.vertical-nav .search-input {
  order: 1;
  width: min(320px, 42vw);
  flex: 1 1 260px;
}

.vertical-nav .mode-switch {
  order: 3;
  margin-left: 12px;
  flex: 0 0 auto;
}

.vertical-nav .gallery-tools:not(:has(.pager-chip)) .mode-switch {
  margin-left: auto;
}

.vertical-nav .pager-chip {
  order: 2;
}

.search-input {
  width: min(360px, 32vw);
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 16px;
  background: rgba(var(--glass-bg-rgb), 0.72);
  box-shadow: 0 0 0 1px color-mix(in srgb, var(--primary-color) 18%, transparent);
  backdrop-filter: blur(16px);
}

.search-input :deep(.el-input__inner) {
  color: var(--forum-text);
  font-weight: var(--font-weight-body);
}

.mode-switch {
  position: relative;
  display: inline-flex;
  gap: 4px;
  padding: 5px;
  border: 1px solid var(--forum-line);
  border-radius: 16px;
  background: rgba(var(--glass-bg-rgb), 0.72);
  backdrop-filter: blur(16px);
  flex: 0 0 auto;
}

.mode-thumb {
  position: absolute;
  top: 5px;
  bottom: 5px;
  width: calc(50% - 5px);
  border-radius: 11px;
  background: linear-gradient(135deg, rgba(var(--primary-rgb), 0.32), rgba(var(--primary-rgb), 0.16));
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.12), 0 10px 26px rgba(var(--primary-rgb), 0.18);
  transition: transform 360ms cubic-bezier(0.2, 0.8, 0.2, 1);
}

.mode-switch.is-gallery .mode-thumb {
  transform: translateX(0);
}

.mode-switch.is-list .mode-thumb {
  transform: translateX(calc(100% + 4px));
}

.mode-switch button {
  position: relative;
  z-index: 1;
  min-width: 58px;
  border: none;
  border-radius: 11px;
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 12px;
  font-weight: var(--font-weight-title);
  padding: 8px 12px;
  transition: background 180ms ease, color 180ms ease, transform 180ms ease;
}

.mode-switch button:hover {
  color: var(--text-primary);
  transform: translateY(-1px);
}

.mode-switch button.active {
  color: var(--text-primary);
}

.pager-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px;
  border: 1px solid var(--forum-line);
  border-radius: 16px;
  background: rgba(var(--glass-bg-rgb), 0.7);
  backdrop-filter: blur(16px);
}

.pager-chip button {
  border: none;
  border-radius: 11px;
  background: rgba(var(--primary-rgb), 0.08);
  color: var(--forum-text);
  cursor: pointer;
  font-size: 12px;
  font-weight: var(--font-weight-title);
  padding: 7px 10px;
  transition: background 180ms ease, opacity 180ms ease;
}

.pager-chip button:hover:not(:disabled) {
  background: rgba(var(--primary-rgb), 0.2);
}

.pager-chip button:disabled {
  cursor: not-allowed;
  opacity: 0.38;
}

.pager-chip span {
  min-width: 48px;
  color: var(--forum-muted);
  font-size: 12px;
  font-weight: var(--font-weight-title);
  text-align: center;
}

.dome-main {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  overflow: hidden;
  touch-action: none;
  user-select: none;
  --circ: calc(var(--radius) * 3.14);
  --rot-y: calc((360deg / var(--segments-x)) / 2);
  --rot-x: calc((360deg / var(--segments-y)) / 2);
  --item-width: calc(var(--circ) / var(--segments-x));
  --item-height: calc(var(--circ) / var(--segments-y));
  animation: forum-content-focus 760ms cubic-bezier(0.16, 1, 0.3, 1) 520ms both;
}

.dome-stage {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  perspective: calc(var(--radius) * 2);
  perspective-origin: 50% 50%;
  contain: layout paint size;
  transform: scale(1.08);
  transform-origin: 50% 52%;
}

.vertical-nav .dome-stage {
  transform: scale(1.03);
}

.dome-sphere,
.dome-item,
.dome-tile {
  transform-style: preserve-3d;
}

.dome-sphere {
  will-change: transform;
}

.dome-item {
  width: calc(var(--item-width) * var(--item-size-x));
  height: calc(var(--item-height) * var(--item-size-y));
  position: absolute;
  inset: -999px;
  margin: auto;
  transform-origin: 50% 50%;
  backface-visibility: hidden;
  transition: transform 300ms;
  transform:
    rotateY(calc(var(--rot-y) * (var(--offset-x) + ((var(--item-size-x) - 1) / 2))))
    rotateX(calc(var(--rot-x) * (var(--offset-y) - ((var(--item-size-y) - 1) / 2))))
    translateZ(var(--radius));
}

.dome-tile {
  position: absolute;
  inset: 10px;
  display: block;
  overflow: hidden;
  width: calc(100% - 20px);
  height: calc(100% - 20px);
  padding: 0;
  border: 1px solid color-mix(in srgb, var(--primary-color) 16%, rgba(255, 255, 255, 0.14));
  border-radius: var(--tile-radius);
  background: var(--forum-panel-strong);
  cursor: pointer;
  backface-visibility: hidden;
  --tilt-x: 0deg;
  --tilt-y: 0deg;
  --glint-x: 50%;
  --glint-y: 50%;
  box-shadow: 0 18px 34px rgba(0, 0, 0, 0.28);
  transform: translateZ(0);
  transition: transform 260ms ease, border-color 260ms ease, box-shadow 260ms ease;
  will-change: transform;
}

.dome-tile:hover {
  border-color: var(--accent);
  transform:
    translateZ(34px)
    rotateX(var(--tilt-x))
    rotateY(var(--tilt-y))
    scale(1.1);
  box-shadow: 0 22px 52px rgba(0, 0, 0, 0.42), 0 0 28px color-mix(in srgb, var(--accent), transparent 56%);
}

.dome-tile img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  pointer-events: none;
  filter: saturate(1.08) contrast(1.05);
}

.tile-shade {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(180deg, rgba(0, 0, 0, 0.08), rgba(0, 0, 0, 0.82)),
    radial-gradient(circle at 24% 12%, color-mix(in srgb, var(--accent), transparent 18%), transparent 54%);
}

.tile-glint {
  position: absolute;
  inset: -24%;
  background:
    radial-gradient(circle at var(--glint-x) var(--glint-y), rgba(255, 255, 255, 0.42), transparent 18%),
    linear-gradient(115deg, transparent 20%, rgba(255, 255, 255, 0.2) 48%, transparent 62%);
  mix-blend-mode: screen;
  opacity: 0;
  transform: translateZ(32px);
  transition: opacity 220ms ease;
  pointer-events: none;
}

.dome-tile:hover .tile-glint {
  opacity: 1;
}

.tile-label {
  position: absolute;
  left: 12px;
  right: 12px;
  bottom: 12px;
  display: grid;
  gap: 4px;
  text-align: left;
}

.tile-label em {
  color: color-mix(in srgb, var(--accent), var(--text-primary) 18%);
  font-size: 10px;
  font-style: normal;
  font-weight: var(--font-weight-title);
}

.tile-label strong {
  color: var(--text-primary);
  font-size: 13px;
  line-height: 1.2;
  letter-spacing: -0.03em;
}

.tile-badge {
  position: absolute;
  top: 10px;
  right: 10px;
  padding: 3px 7px;
  border-radius: 999px;
  background: rgba(var(--glass-bg-rgb), 0.72);
  color: var(--text-primary);
  font-size: 9px;
  font-weight: var(--font-weight-title);
  backdrop-filter: blur(10px);
}

.dome-overlay,
.dome-overlay-blur {
  position: absolute;
  inset: 0;
  z-index: 3;
  pointer-events: none;
}

.dome-overlay {
  background-image: radial-gradient(rgba(235, 235, 235, 0) 60%, var(--overlay-blur-color) 100%);
}

.dome-overlay-blur {
  backdrop-filter: blur(2px);
  mask-image: radial-gradient(rgba(235, 235, 235, 0) 66%, #000 92%);
}

.edge-fade {
  position: absolute;
  z-index: 5;
  left: 0;
  right: 0;
  height: 140px;
  pointer-events: none;
  background: linear-gradient(to bottom, transparent, var(--forum-deep));
}

.edge-fade-top {
  top: 0;
  transform: rotate(180deg);
}

.edge-fade-bottom {
  bottom: 0;
}

.preview-layer {
  position: absolute;
  inset: 0;
  z-index: 20;
  display: grid;
  place-items: center;
  background: color-mix(in srgb, var(--bg-color) 54%, transparent);
  backdrop-filter: blur(4px);
}

.preview-card {
  position: relative;
  overflow: hidden;
  width: min(520px, 72vw);
  height: min(630px, 78vh);
  padding: 0;
  border: 1px solid rgba(255, 255, 255, 0.24);
  border-radius: 32px;
  background: var(--forum-deep);
  cursor: pointer;
  box-shadow: 0 34px 100px rgba(0, 0, 0, 0.62);
}

.preview-card img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  filter: saturate(1.12) contrast(1.08);
}

.preview-gradient {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(180deg, rgba(var(--glass-bg-rgb), 0.04) 8%, rgba(var(--glass-bg-rgb), 0.42) 42%, rgba(var(--glass-bg-rgb), 0.96) 100%),
    radial-gradient(circle at 20% 12%, rgba(var(--primary-rgb), 0.32), transparent 38%);
}

.preview-content {
  position: absolute;
  left: 30px;
  right: 30px;
  bottom: 28px;
  display: grid;
  gap: 12px;
  text-align: left;
}

.preview-kicker {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: var(--forum-accent);
  font-size: 12px;
  font-weight: var(--font-weight-title);
  letter-spacing: 0.14em;
}

.preview-kicker i {
  color: rgba(255, 255, 255, 0.64);
  font-style: normal;
  letter-spacing: 0;
}

.preview-content strong {
  color: var(--text-primary);
  font-size: clamp(28px, 4vw, 46px);
  line-height: 1;
  letter-spacing: -0.07em;
}

.preview-content em {
  width: fit-content;
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(var(--primary-rgb), 0.12);
  color: var(--text-primary);
  font-size: 12px;
  font-style: normal;
  font-weight: var(--font-weight-title);
}

.preview-content p {
  margin: 0;
  color: rgba(255, 255, 255, 0.74);
  font-size: 14px;
  line-height: 1.75;
}

.preview-cta {
  width: fit-content;
  margin-top: 4px;
  padding: 9px 14px;
  border-radius: 14px;
  background: var(--forum-accent);
  color: var(--bg-color);
  font-size: 12px;
  font-weight: var(--font-weight-title);
}

.preview-enter-active,
.preview-leave-active {
  transition: opacity 320ms ease;
}

.preview-enter-active .preview-card,
.preview-leave-active .preview-card {
  transition: transform 360ms cubic-bezier(0.2, 0.8, 0.2, 1), opacity 320ms ease;
}

.preview-enter-from,
.preview-leave-to {
  opacity: 0;
}

.preview-enter-from .preview-card,
.preview-leave-to .preview-card {
  opacity: 0;
  transform: scale(0.72) translateY(34px);
}

.normal-list {
  position: absolute;
  inset: 104px 28px 28px;
  z-index: 6;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
  overflow-y: auto;
  padding: 4px 4px 12px;
  animation: forum-content-focus 680ms cubic-bezier(0.16, 1, 0.3, 1) 420ms both;
}

.vertical-nav .normal-list {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  inset: 116px 24px 28px;
}

.mode-slide-enter-active,
.mode-slide-leave-active {
  transition: opacity 360ms ease, transform 420ms cubic-bezier(0.2, 0.8, 0.2, 1), filter 360ms ease;
}

.mode-slide-enter-from {
  opacity: 0;
  transform: translateY(18px) scale(0.985);
  filter: blur(8px);
}

.mode-slide-leave-to {
  opacity: 0;
  transform: translateY(-12px) scale(0.99);
  filter: blur(6px);
}

@keyframes forum-page-arrive {
  from {
    opacity: 0;
    transform: translateY(18px) scale(0.992);
    filter: blur(12px) saturate(0.86);
  }

  to {
    opacity: 1;
    transform: translateY(0) scale(1);
    filter: blur(0) saturate(1);
  }
}

@keyframes forum-shell-rise {
  from {
    opacity: 0;
    transform: translateY(22px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes forum-hero-in {
  from {
    opacity: 0;
    transform: translate3d(-18px, 16px, 0);
    filter: blur(8px);
  }

  to {
    opacity: 1;
    transform: translate3d(0, 0, 0);
    filter: blur(0);
  }
}

@keyframes forum-tools-in {
  from {
    opacity: 0;
    transform: translate3d(18px, -8px, 0) scale(0.98);
    filter: blur(8px);
  }

  to {
    opacity: 1;
    transform: translate3d(0, 0, 0) scale(1);
    filter: blur(0);
  }
}

@keyframes forum-panel-in {
  from {
    opacity: 0;
    transform: translateY(28px) scale(0.985);
    filter: blur(14px);
    box-shadow: 0 12px 46px rgba(0, 0, 0, 0.22);
  }

  to {
    opacity: 1;
    transform: translateY(0) scale(1);
    filter: blur(0);
    box-shadow: 0 36px 110px rgba(0, 0, 0, 0.45);
  }
}

@keyframes forum-content-focus {
  from {
    opacity: 0;
    transform: translateY(16px) scale(0.982);
    filter: blur(12px);
  }

  to {
    opacity: 1;
    transform: translateY(0) scale(1);
    filter: blur(0);
  }
}

.normal-card {
  position: relative;
  overflow: hidden;
  min-height: 220px;
  border: 1px solid var(--forum-line);
  border-radius: 24px;
  background:
    radial-gradient(circle at 18% 0%, rgba(var(--primary-rgb), 0.1), transparent 38%),
    rgba(var(--glass-bg-rgb), 0.64);
  cursor: pointer;
  box-shadow: 0 20px 48px rgba(0, 0, 0, 0.28);
  transition: transform 220ms ease, border-color 220ms ease, box-shadow 220ms ease;
}

.normal-card:hover {
  border-color: var(--forum-line-strong);
  transform: translateY(-5px);
  box-shadow: 0 28px 66px rgba(0, 0, 0, 0.34), 0 0 34px rgba(var(--primary-rgb), 0.16);
}

.normal-card img {
  width: 100%;
  height: 150px;
  object-fit: cover;
  filter: saturate(1.08) contrast(1.04);
}

.normal-body {
  display: grid;
  gap: 9px;
  padding: 16px;
}

.normal-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.normal-meta span {
  padding: 3px 7px;
  border-radius: 999px;
  background: rgba(var(--primary-rgb), 0.08);
  color: var(--forum-muted);
  font-size: 10px;
  font-weight: var(--font-weight-title);
}

.normal-body h2 {
  margin: 0;
  color: var(--text-primary);
  font-size: 18px;
  line-height: 1.25;
  letter-spacing: -0.04em;
}

.normal-body p {
  margin: 0;
  color: var(--forum-muted);
  font-size: 13px;
  line-height: 1.7;
}

.normal-arrow {
  position: absolute;
  right: 14px;
  bottom: 14px;
  padding: 7px 10px;
  border-radius: 12px;
  background: rgba(var(--primary-rgb), 0.12);
  color: var(--text-primary);
  font-size: 11px;
  font-weight: var(--font-weight-title);
  opacity: 0;
  transform: translateY(8px);
  transition: opacity 180ms ease, transform 180ms ease;
}

.normal-card:hover .normal-arrow {
  opacity: 1;
  transform: translateY(0);
}

.empty {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  color: var(--forum-muted);
  font-size: 14px;
  font-weight: var(--font-weight-body);
}

.md-editor-wrapper {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.md-editor {
  width: 100%;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 12px;
  font-family: 'JetBrains Mono', monospace;
  font-size: 13px;
  background: var(--bg-input);
  color: var(--text-primary);
  resize: vertical;
}

.md-preview {
  max-height: 400px;
  overflow-y: auto;
  border: 1px solid var(--border-color);
  border-radius: 12px;
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
  font-size: 11px;
  font-weight: var(--font-weight-body);
}

.upload-list {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.uploaded-item {
  padding: 2px 8px;
  border-radius: 8px;
  background: rgba(var(--primary-rgb), 0.1);
  color: var(--primary-color);
  cursor: pointer;
  font-size: 10px;
  font-weight: var(--font-weight-body);
}

:global(body.dg-scroll-lock) {
  overflow: hidden;
}

@media (max-width: 900px) {
  .forum-shell {
    width: min(100vw - 24px, 760px);
    padding-top: 26px;
  }

  .vertical-nav .forum-shell {
    width: min(100vw - 24px, 760px);
  }

  .forum-hero {
    display: grid;
  }

  .forum-actions {
    justify-content: flex-start;
  }

  .gallery-tools {
    top: auto;
    right: 18px;
    bottom: 18px;
    left: 18px;
    justify-content: space-between;
  }

  .search-input {
    width: min(420px, 56vw);
  }

  .gallery-panel {
    min-height: 720px;
  }

  .normal-list {
    inset: 96px 18px 92px;
    grid-template-columns: 1fr;
  }

  .md-editor-wrapper {
    grid-template-columns: 1fr;
  }
}

@media (min-width: 901px) and (max-width: 1320px) {
  .vertical-nav .forum-hero {
    display: grid;
  }

  .vertical-nav .gallery-panel {
    min-height: 760px;
  }

  .vertical-nav .gallery-copy {
    max-width: 300px;
  }

  .vertical-nav .gallery-tools {
    width: min(520px, calc(100% - 330px));
  }

  .vertical-nav .normal-list {
    grid-template-columns: 1fr;
  }
}
</style>
