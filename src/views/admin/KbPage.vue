<template>
  <div class="admin-page">
    <section class="admin-hero">
      <div>
        <span>{{ copy.eyebrow }}</span>
        <h2 class="page-title">{{ copy.title }}</h2>
        <p>{{ copy.subtitle }}</p>
      </div>
      <div class="hero-actions">
        <el-button :loading="loading" @click="load">{{ copy.refresh }}</el-button>
      </div>
    </section>

    <section class="admin-metric-grid">
      <article>
        <span>{{ copy.nodes }}</span>
        <strong>{{ nodes.length }}</strong>
        <small>{{ copy.nodesHint }}</small>
      </article>
      <article>
        <span>{{ copy.articles }}</span>
        <strong>{{ articles.length }}</strong>
        <small>{{ copy.articlesHint }}</small>
      </article>
      <article>
        <span>{{ copy.views }}</span>
        <strong>{{ totalViews }}</strong>
        <small>{{ copy.viewsHint }}</small>
      </article>
      <article>
        <span>{{ copy.categories }}</span>
        <strong>{{ categories.length }}</strong>
        <small>{{ copy.categoriesHint }}</small>
      </article>
    </section>

    <section class="admin-toolbar">
      <div class="toolbar-left">
        <el-input v-model="query" clearable :placeholder="copy.search" style="width: 300px" />
        <el-select v-model="nodeFilter" style="width: 190px">
          <el-option :label="copy.allNodes" value="all" />
          <el-option v-for="node in nodes" :key="node.id" :label="node.label" :value="String(node.id)" />
        </el-select>
      </div>
      <div class="toolbar-right">
        <el-tag>{{ copy.showing }} {{ filteredArticles.length }}</el-tag>
      </div>
    </section>

    <section class="admin-grid two">
      <div class="admin-panel">
        <div class="admin-panel-head">
          <div>
            <span>{{ copy.nodes }}</span>
            <strong>{{ copy.nodeTable }}</strong>
          </div>
        </div>
        <el-table v-if="nodes.length" :data="nodes" stripe max-height="360">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="label" :label="copy.name" min-width="160" />
          <el-table-column prop="category" :label="copy.type" width="130" />
          <el-table-column prop="parentId" label="Parent" width="90" />
          <el-table-column prop="color" label="Color" width="120">
            <template #default="{ row }">
              <span class="color-dot" :style="{ background: row.color || 'var(--primary-color)' }"></span>
              {{ row.color || '-' }}
            </template>
          </el-table-column>
        </el-table>
        <div v-else class="empty-hint">{{ copy.emptyNodes }}</div>
      </div>

      <div class="admin-panel">
        <div class="admin-panel-head">
          <div>
            <span>{{ copy.articles }}</span>
            <strong>{{ copy.articleTable }}</strong>
          </div>
        </div>
        <el-table v-if="filteredArticles.length" :data="filteredArticles" stripe max-height="360">
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="title" :label="copy.titleColumn" min-width="260" />
          <el-table-column prop="nodeId" label="Node" width="90" />
          <el-table-column prop="viewCount" :label="copy.views" width="90" />
          <el-table-column :label="copy.action" width="110" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="danger" @click="del(row)">{{ copy.delete }}</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div v-else class="empty-hint">{{ copy.emptyArticles }}</div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api'

const { locale } = useI18n()
const nodes = ref<any[]>([])
const articles = ref<any[]>([])
const loading = ref(false)
const query = ref('')
const nodeFilter = ref('all')
const isZh = computed(() => locale.value.startsWith('zh'))
const copy = computed(() => isZh.value ? {
  eyebrow: '知识库',
  title: '知识与文章管理',
  subtitle: '维护站内知识节点和文章内容。这里的内容会影响知识库展示和 AI 检索质量。',
  refresh: '刷新',
  nodes: '知识节点',
  nodesHint: '图谱与分类',
  articles: '知识文章',
  articlesHint: '可检索内容',
  views: '浏览',
  viewsHint: '文章浏览总量',
  categories: '分类',
  categoriesHint: '节点分类数',
  search: '搜索文章标题',
  allNodes: '全部节点',
  showing: '当前显示',
  nodeTable: '节点清单',
  articleTable: '文章清单',
  name: '名称',
  type: '类型',
  titleColumn: '标题',
  action: '操作',
  delete: '删除',
  emptyNodes: '暂无知识节点',
  emptyArticles: '没有匹配的文章',
  deleted: '文章已删除',
  confirmDelete: '确认删除文章',
  loadFailed: '知识库数据加载失败',
} : {
  eyebrow: 'Knowledge Base',
  title: 'Knowledge and Article Management',
  subtitle: 'Maintain site knowledge nodes and articles. These records affect knowledge pages and AI retrieval quality.',
  refresh: 'Refresh',
  nodes: 'Nodes',
  nodesHint: 'Graph and categories',
  articles: 'Articles',
  articlesHint: 'Retrievable content',
  views: 'Views',
  viewsHint: 'Total article views',
  categories: 'Categories',
  categoriesHint: 'Node categories',
  search: 'Search article title',
  allNodes: 'All nodes',
  showing: 'Showing',
  nodeTable: 'Node list',
  articleTable: 'Article list',
  name: 'Name',
  type: 'Type',
  titleColumn: 'Title',
  action: 'Actions',
  delete: 'Delete',
  emptyNodes: 'No knowledge nodes',
  emptyArticles: 'No matching articles',
  deleted: 'Article deleted',
  confirmDelete: 'Delete article',
  loadFailed: 'Failed to load knowledge data',
})

const totalViews = computed(() => articles.value.reduce((sum, item) => sum + Number(item.viewCount || 0), 0))
const categories = computed(() => Array.from(new Set(nodes.value.map((item) => item.category).filter(Boolean))))
const filteredArticles = computed(() => {
  const keyword = query.value.trim().toLowerCase()
  return articles.value.filter((item) => {
    const matchNode = nodeFilter.value === 'all' || String(item.nodeId) === nodeFilter.value
    const matchKeyword = !keyword || String(item.title || '').toLowerCase().includes(keyword)
    return matchNode && matchKeyword
  })
})

async function load() {
  loading.value = true
  try {
    const [n, a] = await Promise.all([adminApi.knowledgeNodes.list(), adminApi.knowledgeArticles.list()])
    if (n.data.code === 200) nodes.value = n.data.data || []
    if (a.data.code === 200) articles.value = a.data.data || []
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || error?.message || copy.value.loadFailed)
  } finally {
    loading.value = false
  }
}

async function del(row: any) {
  try {
    await ElMessageBox.confirm(`${copy.value.confirmDelete} ${row.title}?`)
    await adminApi.knowledgeArticles.delete(row.id)
    ElMessage.success(copy.value.deleted)
    load()
  } catch {
    // cancelled
  }
}

onMounted(load)
</script>

<style scoped>
.color-dot {
  display: inline-block;
  width: 9px;
  height: 9px;
  margin-right: 7px;
  border-radius: 999px;
}
</style>
