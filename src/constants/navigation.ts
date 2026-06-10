import type { Component } from 'vue'
import { ROUTES } from '@/constants'
import {
  BarChart3,
  BookOpen,
  Bot,
  BrainCircuit,
  Database,
  Gauge,
  MessageSquare,
} from 'lucide-vue-next'

export type NavItem = {
  label: string
  path: string
  icon: Component
}

export const mainNavItems: NavItem[] = [
  { label: '模型总览', path: ROUTES.TRAINING, icon: BrainCircuit },
  { label: '模型接入测试', path: ROUTES.PREDICTION, icon: Gauge },
  { label: '性能看板', path: ROUTES.VIZ, icon: BarChart3 },
  { label: '数据集实时可视化', path: ROUTES.DATASET_VIZ, icon: Database },
]

export const exploreNavItems: NavItem[] = [
  { label: '知识中心', path: ROUTES.KNOWLEDGE, icon: BookOpen },
  { label: '数据中心', path: ROUTES.DATA, icon: Database },
  { label: '交流论坛', path: ROUTES.FORUM, icon: MessageSquare },
  { label: 'AI', path: ROUTES.AI, icon: Bot },
]

export const navLabelKeys: Record<string, string> = {
  '/dashboard': 'nav.training',
  '/model-overview': 'nav.training',
  '/training': 'nav.training',
  '/model-access-test': 'nav.prediction',
  '/prediction': 'nav.prediction',
  '/performance-dashboard': 'nav.analysis',
  '/viz': 'nav.analysis',
  '/dataset-visualization': 'nav.datasetViz',
  '/dataset-viz': 'nav.datasetViz',
  '/data-center': 'nav.data',
  '/data': 'nav.data',
  '/analysis': 'nav.vizHub',
  '/analysis-workbench': 'nav.vizHub',
  '/knowledge-base': 'nav.knowledge',
  '/knowledge': 'nav.knowledge',
  '/cloud': 'nav.data',
  '/community-forum': 'nav.forum',
  '/forum': 'nav.forum',
  '/ai-studio': 'nav.ai',
  '/ai': 'nav.ai',
  '/profile': 'nav.profile',
}

export const navLabelKey = (path: string) => navLabelKeys[path] || 'nav.training'

export function isMainNavActive(path: string, item: Pick<NavItem, 'path'>) {
  const legacyByNewPath: Record<string, string[]> = {
    [ROUTES.TRAINING]: ['/training', '/dashboard'],
    [ROUTES.PREDICTION]: ['/prediction'],
    [ROUTES.VIZ]: ['/viz'],
    [ROUTES.DATASET_VIZ]: ['/dataset-viz'],
  }
  return path === item.path || (legacyByNewPath[item.path] || []).includes(path)
}

export function isExploreNavActive(path: string, item?: Pick<NavItem, 'path'>) {
  const matches = (target: string) => {
    if (target === ROUTES.KNOWLEDGE) return path === ROUTES.KNOWLEDGE || path.startsWith(`${ROUTES.KNOWLEDGE}/`) || path === '/knowledge' || path.startsWith('/knowledge/')
    if (target === ROUTES.DATA) return path === ROUTES.DATA || path === '/data' || path === '/cloud'
    if (target === ROUTES.FORUM) return path === ROUTES.FORUM || path.startsWith(`${ROUTES.FORUM}/`) || path === '/forum' || path.startsWith('/forum/')
    if (target === ROUTES.AI) return path === ROUTES.AI || path === '/ai'
    return path === target
  }

  if (item) return matches(item.path)
  return exploreNavItems.some((navItem) => matches(navItem.path))
}
