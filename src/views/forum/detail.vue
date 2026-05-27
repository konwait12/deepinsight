<template>
  <div class="detail-page">
    <div class="detail-container">
      <nav class="detail-nav">
        <button class="back-btn" @click="$router.push('/forum')">&larr; 返回论坛</button>
      </nav>

      <div v-if="loading" class="loading-state">加载中...</div>

      <template v-else-if="post">
        <article class="article-main">
          <header class="article-header">
            <div class="header-badges">
              <span v-if="post.isOfficial" class="badge badge-official">官方</span>
              <span v-if="post.isPinned" class="badge badge-pinned">置顶</span>
            </div>
            <h1 class="article-title">{{ post.title }}</h1>
            <div class="article-meta">
              <span>#{{ post.id }}</span>
              <span>&middot;</span>
              <span>浏览 {{ post.viewCount }}</span>
              <span>&middot;</span>
              <span>{{ formatTime(post.createdAt) }}</span>
            </div>
            <img v-if="post.coverUrl" :src="post.coverUrl" class="cover-img" />
          </header>

          <div class="article-body" v-html="renderedHtml" />

          <div class="comments-section">
            <h4>评论 ({{ comments.length }})</h4>
            <div v-for="cm in comments" :key="cm.id" class="comment-item">
              <span class="cm-author">用户#{{ cm.userId }}</span>
              <span class="cm-time">{{ formatTime(cm.createdAt) }}</span>
              <p class="cm-text">{{ cm.content }}</p>
            </div>
            <div class="add-comment" v-if="isLoggedIn">
              <el-input v-model="newComment" placeholder="写下你的评论..." @keyup.enter="submitComment" />
              <el-button size="small" type="primary" @click="submitComment" class="!ml-2 !rounded-lg">评论</el-button>
            </div>
          </div>
        </article>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { forumApi } from '@/api';
import { renderMarkdown } from '@/utils/markdown';
import { hasStoredAuthToken } from '@/utils/authState';

const route = useRoute();
const post = ref<any>(null);
const comments = ref<any[]>([]);
const newComment = ref('');
const loading = ref(true);
const isLoggedIn = computed(() => hasStoredAuthToken());

const renderedHtml = computed(() => post.value?.content ? renderMarkdown(post.value.content) : '');

const formatTime = (t: string) => {
  if (!t) return '';
  return new Date(t).toLocaleDateString('zh-CN', { year:'numeric', month:'2-digit', day:'2-digit', hour:'2-digit', minute:'2-digit' });
};

const fetchPost = async () => {
  loading.value = true;
  document.querySelector('.app-body')?.scrollTo({ top: 0 });
  try {
    const [postRes, commentsRes] = await Promise.all([
      forumApi.getPost(route.params.id as string),
      forumApi.getComments(route.params.id as string),
    ]);
    if (postRes.data.code === 200) post.value = postRes.data.data;
    if (commentsRes.data.code === 200) comments.value = commentsRes.data.data || [];
  } catch {}
  loading.value = false;
};

const submitComment = async () => {
  if (!newComment.value.trim() || !post.value) return;
  try {
    const res = await forumApi.addComment(post.value.id, newComment.value);
    if (res.data.code === 200) {
      newComment.value = '';
      fetchPost();
    } else {
      ElMessage.error(res.data.message || '评论失败');
    }
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '评论失败');
  }
};

onMounted(fetchPost);
watch(() => route.params.id, fetchPost);
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
  border-radius: 999px;
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
  border-radius: 32px;
  background:
    radial-gradient(circle at 16% 0%, rgba(var(--primary-rgb), 0.1), transparent 36%),
    rgba(var(--glass-bg-rgb), 0.72);
  box-shadow: 0 34px 100px rgba(0, 0, 0, 0.34), 0 0 0 1px rgba(var(--primary-rgb), 0.05);
  backdrop-filter: blur(18px) saturate(140%);
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
  letter-spacing: -0.06em;
  line-height: 1.04;
}

.article-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 7px;
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
  border-radius: 22px;
  margin-top: 10px;
  box-shadow: 0 22px 64px rgba(0, 0, 0, 0.28);
}

/* —— MD 排版 ——— */
.article-body { font-size:15px; line-height:1.9; color:var(--text-primary); }
.article-body :deep(h1) { font-size:24px; font-weight:900; margin:36px 0 14px; }
.article-body :deep(h2) { font-size:20px; font-weight:800; margin:30px 0 12px; border-left:4px solid var(--primary-color); padding-left:12px; }
.article-body :deep(h3) { font-size:17px; font-weight:700; margin:24px 0 10px; }
.article-body :deep(h4) { font-size:15px; font-weight:700; margin:18px 0 8px; }
.article-body :deep(p) { margin:0 0 12px; }
.article-body :deep(a) { color:var(--primary-color); text-decoration:underline; text-decoration-style:dashed; text-underline-offset:3px; }
.article-body :deep(blockquote) { border-left:4px solid var(--primary-color); background:rgba(var(--primary-rgb), 0.08); border-radius:0 10px 10px 0; padding:10px 16px; margin:14px 0; color:var(--text-secondary); font-style:italic; }
.article-body :deep(hr) { border:none; border-top:1px solid var(--border-color); margin:24px 0; }
.article-body :deep(ul), .article-body :deep(ol) { padding-left:20px; margin:0 0 12px; }
.article-body :deep(li) { margin-bottom:3px; }
.article-body :deep(li::marker) { color:var(--primary-color); }
.article-body :deep(img) { max-width:100%; border-radius:12px; margin:12px 0; }
.article-body :deep(table) { width:100%; border-collapse:collapse; margin:14px 0; font-size:13px; }
.article-body :deep(th) { background:rgba(var(--primary-rgb), 0.08); font-weight:700; text-align:left; padding:8px 12px; border-bottom:2px solid var(--border-color); }
.article-body :deep(td) { padding:6px 12px; border-bottom:1px solid var(--border-color); }
.article-body :deep(tr:hover td) { background:rgba(var(--primary-rgb), 0.06); }
.article-body :deep(code) { font-family:'JetBrains Mono',monospace; font-size:13px; background:rgba(var(--primary-rgb), 0.08); color:var(--primary-color); padding:2px 6px; border-radius:5px; }
.article-body :deep(pre) { background:rgba(var(--glass-bg-rgb), 0.72); border:1px solid color-mix(in srgb, var(--primary-color) 14%, var(--border-color)); border-radius:12px; padding:16px; overflow-x:auto; margin:16px 0; }
.article-body :deep(pre code) { background:transparent; padding:0; color:var(--text-primary); }
.article-body :deep(.hljs-keyword) { color:#c792ea; }
.article-body :deep(.hljs-string) { color:#c3e88d; }
.article-body :deep(.hljs-comment) { color:#546e7a; font-style:italic; }
.article-body :deep(.hljs-function) { color:#82aaff; }
.article-body :deep(.hljs-number) { color:#f78c6c; }
.article-body :deep(.hljs-title) { color:#82aaff; }
.article-body :deep(.hljs-built_in) { color:#ffcb6b; }

.comments-section { margin-top:40px; padding-top:24px; border-top:1px solid color-mix(in srgb, var(--primary-color) 16%, var(--border-color)); }
.comments-section h4 { font-size:14px; font-weight:800; color:var(--text-primary); margin:0 0 14px; }
.comment-item { padding:12px 0; border-bottom:1px solid color-mix(in srgb, var(--primary-color) 10%, var(--border-color)); }
.cm-author { font-size:12px; font-weight:700; color:var(--primary-color); }
.cm-time { font-size:10px; color:var(--text-secondary); margin-left:8px; }
.cm-text { margin:4px 0 0; font-size:13px; color:var(--text-primary); }
.add-comment { display:flex; margin-top:16px; }

@media (max-width: 720px) {
  .detail-container {
    padding: 24px 14px 56px;
  }

  .article-main {
    border-radius: 24px;
  }

  .add-comment {
    display: grid;
    gap: 10px;
  }
}
</style>
