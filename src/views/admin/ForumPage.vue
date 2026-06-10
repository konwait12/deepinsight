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
        <span>{{ copy.total }}</span>
        <strong>{{ posts.length }}</strong>
        <small>{{ copy.totalHint }}</small>
      </article>
      <article>
        <span>{{ copy.official }}</span>
        <strong>{{ officialCount }}</strong>
        <small>{{ copy.officialHint }}</small>
      </article>
      <article>
        <span>{{ copy.views }}</span>
        <strong>{{ totalViews }}</strong>
        <small>{{ copy.viewsHint }}</small>
      </article>
      <article>
        <span>{{ copy.users }}</span>
        <strong>{{ authorCount }}</strong>
        <small>{{ copy.usersHint }}</small>
      </article>
    </section>

    <section class="admin-toolbar">
      <div class="toolbar-left">
        <el-input v-model="query" clearable :placeholder="copy.search" style="width: 300px" />
        <el-select v-model="officialFilter" style="width: 170px">
          <el-option :label="copy.allPosts" value="all" />
          <el-option :label="copy.officialOnly" value="official" />
          <el-option :label="copy.communityOnly" value="community" />
        </el-select>
      </div>
      <div class="toolbar-right">
        <el-tag>{{ copy.showing }} {{ filteredPosts.length }}</el-tag>
      </div>
    </section>

    <el-table v-if="filteredPosts.length" :data="filteredPosts" stripe max-height="640">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="title" :label="copy.titleColumn" min-width="320" />
      <el-table-column prop="userId" :label="copy.user" width="100" />
      <el-table-column :label="copy.official" width="110">
        <template #default="{ row }">
          <el-tag :type="row.official ? 'success' : 'info'" size="small">
            {{ row.official ? copy.yes : copy.no }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="viewCount" :label="copy.views" width="100" />
      <el-table-column prop="createdAt" :label="copy.createdAt" min-width="170" />
      <el-table-column :label="copy.action" width="110" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="danger" @click="del(row)">{{ copy.delete }}</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div v-else class="empty-hint">{{ copy.empty }}</div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api'

const { locale } = useI18n()
const posts = ref<any[]>([])
const loading = ref(false)
const query = ref('')
const officialFilter = ref('all')
const isZh = computed(() => locale.value.startsWith('zh'))
const copy = computed(() => isZh.value ? {
  eyebrow: '社区内容',
  title: '论坛与文章管理',
  subtitle: '管理社区帖子和官方内容，删除前请确认不是知识库文章的唯一入口。',
  refresh: '刷新',
  total: '全部内容',
  totalHint: '论坛记录',
  official: '官方',
  officialHint: '平台发布内容',
  views: '浏览',
  viewsHint: '总浏览量',
  users: '作者',
  usersHint: '参与用户数',
  search: '搜索标题',
  allPosts: '全部内容',
  officialOnly: '只看官方',
  communityOnly: '只看社区',
  showing: '当前显示',
  titleColumn: '标题',
  user: '用户',
  createdAt: '创建时间',
  action: '操作',
  delete: '删除',
  yes: '是',
  no: '否',
  empty: '没有匹配的内容',
  deleted: '内容已删除',
  confirmDelete: '确认删除',
  loadFailed: '论坛数据加载失败',
} : {
  eyebrow: 'Community Content',
  title: 'Forum and Article Management',
  subtitle: 'Moderate community posts and official content. Confirm before deleting content that may be linked elsewhere.',
  refresh: 'Refresh',
  total: 'Posts',
  totalHint: 'Forum records',
  official: 'Official',
  officialHint: 'Platform content',
  views: 'Views',
  viewsHint: 'Total views',
  users: 'Authors',
  usersHint: 'Participating users',
  search: 'Search title',
  allPosts: 'All content',
  officialOnly: 'Official only',
  communityOnly: 'Community only',
  showing: 'Showing',
  titleColumn: 'Title',
  user: 'User',
  createdAt: 'Created at',
  action: 'Actions',
  delete: 'Delete',
  yes: 'Yes',
  no: 'No',
  empty: 'No matching content',
  deleted: 'Content deleted',
  confirmDelete: 'Delete',
  loadFailed: 'Failed to load forum data',
})

const officialCount = computed(() => posts.value.filter((item) => item.official).length)
const totalViews = computed(() => posts.value.reduce((sum, item) => sum + Number(item.viewCount || 0), 0))
const authorCount = computed(() => new Set(posts.value.map((item) => item.userId).filter(Boolean)).size)
const filteredPosts = computed(() => {
  const keyword = query.value.trim().toLowerCase()
  return posts.value.filter((item) => {
    const matchOfficial = officialFilter.value === 'all'
      || (officialFilter.value === 'official' && item.official)
      || (officialFilter.value === 'community' && !item.official)
    const matchKeyword = !keyword || String(item.title || '').toLowerCase().includes(keyword)
    return matchOfficial && matchKeyword
  })
})

async function load() {
  loading.value = true
  try {
    const response = await adminApi.forumPosts.list()
    if (response.data.code === 200) {
      posts.value = (response.data.data || []).map((item: any) => ({
        ...item,
        official: item.is_official || item.isOfficial || item.official,
      }))
    }
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || error?.message || copy.value.loadFailed)
  } finally {
    loading.value = false
  }
}

async function del(row: any) {
  try {
    await ElMessageBox.confirm(`${copy.value.confirmDelete} ${row.title}?`)
    await adminApi.forumPosts.delete(row.id)
    ElMessage.success(copy.value.deleted)
    load()
  } catch {
    // cancelled
  }
}

onMounted(load)
</script>
