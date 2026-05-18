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
.detail-page { min-height: 100vh; }
.detail-container { max-width: 800px; margin: 0 auto; padding: 40px 24px 80px; }

.detail-nav { margin-bottom: 28px; }
.back-btn { font-size:13px; font-weight:700; color:var(--text-secondary); border:none; background:var(--bg-input); padding:6px 14px; border-radius:10px; cursor:pointer; transition:all .2s; }
.back-btn:hover { color:var(--primary-color); background:rgba(77,201,240,0.1); }

.article-header { margin-bottom: 32px; }
.header-badges { display:flex; gap:6px; margin-bottom:10px; }
.badge { font-size:9px; font-weight:800; padding:2px 8px; border-radius:6px; }
.badge-official { background:rgba(77,201,240,0.12); color:#4dc9f0; }
.badge-pinned { background:rgba(245,158,11,0.15); color:#f59e0b; }

.article-title { font-size:30px; font-weight:900; color:var(--text-primary); line-height:1.25; margin:0 0 12px; }
.article-meta { font-size:13px; color:var(--text-muted); font-weight:600; display:flex; gap:6px; align-items:center; margin-bottom:16px; }
.cover-img { width:100%; border-radius:14px; margin-top:8px; }

/* —— MD 排版 ——— */
.article-body { font-size:15px; line-height:1.9; color:var(--text-primary); }
.article-body :deep(h1) { font-size:24px; font-weight:900; margin:36px 0 14px; }
.article-body :deep(h2) { font-size:20px; font-weight:800; margin:30px 0 12px; border-left:4px solid var(--primary-color); padding-left:12px; }
.article-body :deep(h3) { font-size:17px; font-weight:700; margin:24px 0 10px; }
.article-body :deep(h4) { font-size:15px; font-weight:700; margin:18px 0 8px; }
.article-body :deep(p) { margin:0 0 12px; }
.article-body :deep(a) { color:var(--primary-color); text-decoration:underline; text-decoration-style:dashed; text-underline-offset:3px; }
.article-body :deep(blockquote) { border-left:4px solid var(--primary-color); background:var(--bg-input); border-radius:0 10px 10px 0; padding:10px 16px; margin:14px 0; color:var(--text-secondary); font-style:italic; }
.article-body :deep(hr) { border:none; border-top:1px solid var(--border-color); margin:24px 0; }
.article-body :deep(ul), .article-body :deep(ol) { padding-left:20px; margin:0 0 12px; }
.article-body :deep(li) { margin-bottom:3px; }
.article-body :deep(li::marker) { color:var(--primary-color); }
.article-body :deep(img) { max-width:100%; border-radius:12px; margin:12px 0; }
.article-body :deep(table) { width:100%; border-collapse:collapse; margin:14px 0; font-size:13px; }
.article-body :deep(th) { background:var(--bg-input); font-weight:700; text-align:left; padding:8px 12px; border-bottom:2px solid var(--border-color); }
.article-body :deep(td) { padding:6px 12px; border-bottom:1px solid var(--border-color); }
.article-body :deep(tr:hover td) { background:var(--bg-input); }
.article-body :deep(code) { font-family:'JetBrains Mono',monospace; font-size:13px; background:var(--bg-input); color:var(--primary-color); padding:2px 6px; border-radius:5px; }
.article-body :deep(pre) { background:var(--bg-input); border:1px solid var(--border-color); border-radius:12px; padding:16px; overflow-x:auto; margin:16px 0; }
.article-body :deep(pre code) { background:transparent; padding:0; color:var(--text-primary); }
.article-body :deep(.hljs-keyword) { color:#c792ea; }
.article-body :deep(.hljs-string) { color:#c3e88d; }
.article-body :deep(.hljs-comment) { color:#546e7a; font-style:italic; }
.article-body :deep(.hljs-function) { color:#82aaff; }
.article-body :deep(.hljs-number) { color:#f78c6c; }
.article-body :deep(.hljs-title) { color:#82aaff; }
.article-body :deep(.hljs-built_in) { color:#ffcb6b; }

.comments-section { margin-top:40px; padding-top:24px; border-top:1px solid var(--border-color); }
.comments-section h4 { font-size:14px; font-weight:800; color:var(--text-primary); margin:0 0 14px; }
.comment-item { padding:10px 0; border-bottom:1px solid var(--border-color); }
.cm-author { font-size:12px; font-weight:700; color:var(--primary-color); }
.cm-time { font-size:10px; color:var(--text-secondary); margin-left:8px; }
.cm-text { margin:4px 0 0; font-size:13px; color:var(--text-primary); }
.add-comment { display:flex; margin-top:16px; }
</style>
