/** localStorage 键名常量 */
export const STORAGE_KEYS = {
  TOKEN: 'token',
  ROLE: 'role',
  USERNAME: 'username',
  LANG: 'lang',
  THEME_PRIMARY_COLOR: 'primary-color',
  THEME_PALETTE: 'theme-palette',
  THEME_CUSTOM_HUE: 'theme-custom-hue',
  THEME_IS_DARK: 'is-dark',
  THEME_IS_HORIZONTAL: 'is-horizontal',
  CURVE_BRIGHTNESS: 'curve-brightness',
  CURVE_CONTRAST: 'curve-contrast',
  CURVE_SATURATION: 'curve-saturation',
  AUTO_SYNC: 'auto-sync',
  ASSISTANT_POS: 'assistant-pos',
  ASSISTANT_PROACTIVE: 'assistant-proactive',
} as const

export const APP_EVENTS = {
  TOGGLE_LANG: 'deepinsight:toggle-lang',
} as const

/** 路由路径常量 */
export const ROUTES = {
  HOME: '/',
  LOGIN: '/login',
  DASHBOARD: '/dashboard',
  DATA: '/data-center',
  TRAINING: '/model-overview',
  ANALYSIS: '/analysis',
  ANALYSIS_WORKBENCH: '/analysis-workbench',
  PREDICTION: '/model-access-test',
  DATASET_VIZ: '/dataset-visualization',
  AI: '/ai-studio',
  CLOUD: '/cloud',
  FORUM: '/community-forum',
  KNOWLEDGE: '/knowledge-base',
  PROFILE: '/profile',
  VIZ: '/performance-dashboard',
  ADMIN: '/admin',
} as const

/** API 端点常量 */
export const API_ENDPOINTS = {
  AUTH: {
    LOGIN: '/auth/login',
    REGISTER: '/auth/register',
    ME: '/auth/me',
  },
  TRAINING: '/training',
  DATASETS: '/datasets',
  MODELS: '/models',
  ANALYSIS: {
    MODEL_COMPARISON: '/analysis/model-comparison',
    OVERVIEW: '/analysis/overview',
    TRAINING_CURVE: '/analysis/training-curve',
    HYPERPARAMS: '/analysis/hyperparams',
  },
  PREDICTION: {
    MODELS: '/prediction/models',
    RECOMMEND: '/prediction/recommend',
    HEALTH: '/prediction/health',
  },
  AI: {
    CHAT: '/ai/chat',
    CHAT_STREAM: '/ai/chat/stream',
  },
  ADMIN: {
    STATUS: '/admin/status',
    USERS: '/admin/users',
    AI_CONFIGS: '/ai/configs',
    KNOWLEDGE_NODES: '/admin/knowledge-nodes',
    KNOWLEDGE_ARTICLES: '/admin/knowledge-articles',
    FORUM_POSTS: '/admin/forum-posts',
    DATASETS: '/admin/datasets',
    TRAINING_JOBS: '/admin/training-jobs',
  },
  FORUM: {
    POSTS: '/forum/posts',
    UPLOAD: '/forum/upload',
    KNOWLEDGE_NODES: '/forum/knowledge/nodes',
    KNOWLEDGE_ARTICLES: '/forum/knowledge/articles',
  },
  LOGS: {
    RUNS: '/logs/runs',
  },
  VISUAL_DATA: '/visual-data',
} as const

/** API 基础路径。开发环境通过 Vite proxy 转发到后端，生产部署可设置 VITE_API_BASE_URL。 */
export const API_BASE_URL = (import.meta as any).env?.VITE_API_BASE_URL || '/api/v1'

/** 分页默认值 */
export const PAGINATION = {
  DEFAULT_PAGE: 1,
  DEFAULT_SIZE: 20,
} as const

/** 轮询间隔 (ms) */
export const POLL_INTERVAL = {
  TRAINING: 5000,
  ANALYSIS: 8000,
} as const

