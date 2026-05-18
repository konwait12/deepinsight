<template>
  <div class="detail-page">
    <div class="detail-container">
      <nav class="detail-nav">
        <button class="back-btn" @click="$router.push('/training')">&larr; 返回训练页</button>
        <span class="source-tag">模型介绍</span>
      </nav>

      <div v-if="loading" class="loading-state">加载中...</div>

      <template v-else-if="article">
        <article class="article-main">
          <header class="article-header">
            <h1 class="article-title">{{ article.title }}</h1>
            <div class="article-meta">
              <span>浏览 {{ article.viewCount }}</span>
              <span>&middot;</span>
              <span>{{ formatTime(article.createdAt) }}</span>
              <span v-if="article.paperUrl">&middot;</span>
              <a v-if="article.paperUrl" :href="article.paperUrl" target="_blank" class="paper-link">📄 查看论文</a>
            </div>
          </header>
          <div class="article-body" v-html="renderedMd" />
        </article>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { useRoute } from 'vue-router';
import { renderMarkdown } from '@/utils/markdown';

const route = useRoute();
const article = ref<any>(null);
const loading = ref(true);
const renderedMd = computed(() => article.value?.content ? renderMarkdown(article.value.content) : '');

const formatTime = (t: string) => t ? new Date(t).toLocaleDateString('zh-CN', { year:'numeric', month:'2-digit', day:'2-digit' }) : '';

import { trainingApi } from '@/api';

const fetchArticle = async () => {
  loading.value = true;
  document.querySelector('.app-body')?.scrollTo({ top: 0 });
  try {
    const id = Number(route.params.id);
    const res = await trainingApi.getArticle(id);
    article.value = res.data.data || res.data;
  } catch { /* API unavailable */ }
  loading.value = false;
};

onMounted(fetchArticle);
watch(() => route.params.id, fetchArticle);
</script>

<style scoped>
.detail-page { min-height: 100vh; }
.detail-container { max-width: 800px; margin: 0 auto; padding: 40px 24px 80px; }
.detail-nav { display:flex; align-items:center; gap:10px; margin-bottom:28px }
.back-btn { font-size:13px; font-weight:700; color:var(--text-secondary); border:none; background:var(--bg-input); padding:6px 14px; border-radius:10px; cursor:pointer; transition:all .2s }
.back-btn:hover { color:var(--primary-color); background:rgba(77,201,240,0.1) }
.source-tag { font-size:10px; font-weight:800; text-transform:uppercase; padding:3px 10px; border-radius:8px; background:rgba(77,201,240,0.12); color:#4dc9f0 }

.article-header { margin-bottom:32px }
.article-title { font-size:30px; font-weight:900; color:var(--text-primary); line-height:1.25; margin:0 0 12px }
.article-meta { font-size:13px; color:var(--text-muted); font-weight:600; display:flex; gap:6px; align-items:center }
.paper-link { color:var(--primary-color); text-decoration:none; font-weight:700 }
.paper-link:hover { text-decoration:underline }

.article-body { font-size:15px; line-height:1.9; color:var(--text-primary) }
.article-body :deep(h1) { font-size:24px; font-weight:900; margin:36px 0 14px }
.article-body :deep(h2) { font-size:20px; font-weight:800; margin:30px 0 12px; border-left:4px solid var(--primary-color); padding-left:12px }
.article-body :deep(h3) { font-size:17px; font-weight:700; margin:24px 0 10px }
.article-body :deep(h4) { font-size:15px; font-weight:700; margin:18px 0 8px }
.article-body :deep(p) { margin:0 0 12px }
.article-body :deep(a) { color:var(--primary-color); text-decoration:underline; text-decoration-style:dashed; text-underline-offset:3px }
.article-body :deep(table) { width:100%; border-collapse:collapse; margin:14px 0; font-size:13px }
.article-body :deep(th) { background:var(--bg-input); font-weight:700; text-align:left; padding:8px 12px; border-bottom:2px solid var(--border-color) }
.article-body :deep(td) { padding:6px 12px; border-bottom:1px solid var(--border-color) }
.article-body :deep(code) { font-family:'JetBrains Mono',monospace; font-size:13px; background:var(--bg-input); color:var(--primary-color); padding:2px 6px; border-radius:5px }
.article-body :deep(pre) { background:var(--bg-input); border:1px solid var(--border-color); border-radius:12px; padding:16px; overflow-x:auto; margin:16px 0 }
.article-body :deep(pre code) { background:transparent; padding:0; color:var(--text-primary) }
.article-body :deep(blockquote) { border-left:4px solid var(--primary-color); background:var(--bg-input); border-radius:0 10px 10px 0; padding:10px 16px; margin:14px 0; color:var(--text-secondary); font-style:italic }
.article-body :deep(ul), .article-body :deep(ol) { padding-left:20px; margin:0 0 12px }
.article-body :deep(li::marker) { color:var(--primary-color) }
.article-body :deep(.hljs-keyword) { color:#c792ea }
.article-body :deep(.hljs-string) { color:#c3e88d }
.article-body :deep(.hljs-comment) { color:#546e7a; font-style:italic }
</style>
