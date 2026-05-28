import type { Component } from 'vue'
import {
  BarChart3,
  BookOpen,
  Bot,
  BrainCircuit,
  Database,
  Gauge,
  LayoutDashboard,
  MessageSquare,
  Cloud,
} from 'lucide-vue-next'

export type NavItem = {
  label: string
  path: string
  icon: Component
}

export const mainNavItems: NavItem[] = [
  { label: 'Dashboard', path: '/dashboard', icon: LayoutDashboard },
  { label: 'Data', path: '/data', icon: Database },
  { label: 'Model Overview', path: '/training', icon: BrainCircuit },
  { label: 'Analysis', path: '/viz', icon: BarChart3 },
  { label: 'Prediction', path: '/prediction', icon: Gauge },
]

export const exploreNavItems: NavItem[] = [
  { label: 'Knowledge', path: '/knowledge', icon: BookOpen },
  { label: 'Cloud', path: '/cloud', icon: Cloud },
  { label: 'Forum', path: '/forum', icon: MessageSquare },
  { label: 'AI', path: '/ai', icon: Bot },
]

export const navLabelKeys: Record<string, string> = {
  '/dashboard': 'nav.dashboard',
  '/data': 'nav.data',
  '/training': 'nav.training',
  '/analysis': 'nav.analysis',
  '/prediction': 'nav.prediction',
  '/viz': 'nav.analysis',
  '/knowledge': 'nav.knowledge',
  '/cloud': 'nav.cloud',
  '/forum': 'nav.forum',
  '/ai': 'nav.ai',
  '/profile': 'nav.profile',
}

export const navLabelKey = (path: string) => navLabelKeys[path] || 'nav.dashboard'

export function isMainNavActive(path: string, item: Pick<NavItem, 'path'>) {
  if (item.path === '/viz') return path === '/analysis' || path.startsWith('/viz')
  return path === item.path
}

export function isExploreNavActive(path: string, item?: Pick<NavItem, 'path'>) {
  const matches = (target: string) => {
    if (target === '/knowledge') return path === '/knowledge' || path.startsWith('/knowledge/')
    if (target === '/cloud') return path === '/cloud'
    if (target === '/forum') return path === '/forum' || path.startsWith('/forum/')
    return path === target
  }

  if (item) return matches(item.path)
  return exploreNavItems.some((navItem) => matches(navItem.path))
}
