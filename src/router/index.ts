import { createRouter, createWebHistory } from 'vue-router'
import { STORAGE_KEYS, ROUTES } from '@/constants'
import { clearAuthStorage, readAuthToken } from '@/utils/authState'

const mainLayout = () => import('@/layout/MainLayout.vue')

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior(to, _from, savedPosition) {
    if (savedPosition) return savedPosition
    if (to.hash) return { el: to.hash, behavior: 'smooth' }
    return { top: 0, left: 0 }
  },
  routes: [
    {
      path: ROUTES.HOME,
      name: 'Landing',
      component: () => import('@/views/Landing.vue'),
    },
    {
      path: ROUTES.LOGIN,
      name: 'Login',
      component: () => import('@/views/Login.vue'),
    },
    {
      path: ROUTES.DASHBOARD,
      component: mainLayout,
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          name: 'Dashboard',
          component: () => import('@/views/dashboard/index.vue'),
        },
      ],
    },
    {
      path: ROUTES.DATA,
      component: mainLayout,
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          name: 'Data',
          component: () => import('@/views/data/index.vue'),
        },
      ],
    },
    {
      path: ROUTES.TRAINING,
      component: mainLayout,
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          name: 'Training',
          component: () => import('@/views/training/index.vue'),
        },
      ],
    },
    {
      path: ROUTES.ANALYSIS,
      redirect: ROUTES.VIZ,
    },
    {
      path: ROUTES.PREDICTION,
      component: mainLayout,
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          name: 'Prediction',
          component: () => import('@/views/prediction/index.vue'),
        },
      ],
    },
    {
      path: ROUTES.VIZ,
      component: mainLayout,
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          name: 'VizHub',
          component: () => import('@/views/visualization/VizHub.vue'),
        },
      ],
    },
    { path: '/viz/:module(.*)*', redirect: ROUTES.VIZ },
    {
      path: '/ai',
      name: 'AI',
      component: () => import('@/views/ai/index.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/forum',
      name: 'Forum',
      component: () => import('@/views/forum/index.vue'),
      meta: { requiresAuth: false },
    },
    {
      path: '/forum/:id(\\d+)',
      name: 'ForumDetail',
      component: () => import('@/views/forum/detail.vue'),
      meta: { requiresAuth: false },
    },
    {
      path: '/knowledge/article/:id(\\d+)',
      name: 'KnowledgeArticle',
      component: () => import('@/views/knowledge/article.vue'),
      meta: { requiresAuth: false },
    },
    {
      path: '/knowledge',
      name: 'Knowledge',
      component: () => import('@/views/knowledge/index.vue'),
      meta: { requiresAuth: false },
    },
    {
      path: ROUTES.CLOUD,
      name: 'Cloud',
      component: () => import('@/views/cloud/index.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/profile',
      name: 'Profile',
      component: () => import('@/views/profile/index.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/model-article/:id(\\d+)',
      name: 'ModelArticle',
      component: () => import('@/views/models/article.vue'),
      meta: { requiresAuth: false },
    },
    {
      path: '/admin',
      component: () => import('@/layout/AdminLayout.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
      redirect: '/admin/overview',
      children: [
        { path: 'overview', name: 'AdminOverview', component: () => import('@/views/admin/OverviewPage.vue') },
        { path: 'users', name: 'AdminUsers', component: () => import('@/views/admin/UsersPage.vue') },
        { path: 'models', name: 'AdminModels', component: () => import('@/views/admin/ModelsPage.vue') },
        { path: 'ai', name: 'AdminAi', component: () => import('@/views/admin/AiPage.vue') },
        { path: 'kb', name: 'AdminKb', component: () => import('@/views/admin/KbPage.vue') },
        { path: 'forum', name: 'AdminForum', component: () => import('@/views/admin/ForumPage.vue') },
        { path: 'data', name: 'AdminData', component: () => import('@/views/admin/DataPage.vue') },
      ],
    },
  ],
})

router.beforeEach((to, _from, next) => {
  const token = readAuthToken()
  const role = localStorage.getItem(STORAGE_KEYS.ROLE) || ''
  if (!token) clearAuthStorage()

  if (to.meta.requiresAuth && !token) {
    next(ROUTES.LOGIN)
  } else if (to.meta.requiresAdmin && role !== 'ADMIN') {
    next(ROUTES.DASHBOARD)
  } else {
    next()
  }
})

export default router
