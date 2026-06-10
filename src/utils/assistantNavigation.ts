import { ROUTES } from '@/constants'

export type AssistantNavigationTarget = {
  key: string
  label: string
  path: string
  description: string
  promptHint: string
  aliases: string[]
}

export type AssistantNavigationIntent = AssistantNavigationTarget & {
  confidence: number
  reply: string
  mode?: 'direct' | 'suggested'
  reason?: string
}

const navigationWords = [
  '打开',
  '跳转',
  '跳到',
  '前往',
  '进入',
  '去',
  '看看',
  '查看',
  '带我',
  '切到',
  '导航',
  '入口',
  '链接',
  '在哪',
  '在哪里',
  '页面',
  'open',
  'go',
  'show',
  'view',
  'navigate',
  'link',
]

export const assistantNavigationTargets: AssistantNavigationTarget[] = [
  {
    key: 'training',
    label: '模型总览',
    path: ROUTES.TRAINING,
    description: '查看 9 个推荐系统模型的状态、指标、数据集、训练配置与参数画像。',
    promptHint: '看看模型总览',
    aliases: ['模型总览', '模型', '训练', '模型列表', '已接入模型', 'model overview', 'training'],
  },
  {
    key: 'prediction',
    label: '模型接入测试',
    path: ROUTES.PREDICTION,
    description: '验证推荐系统样例输入、Top-K 输出、后端代理和服务健康状态。',
    promptHint: '打开模型接入测试',
    aliases: ['模型接入测试', '接入测试', '预测', '推理', '推荐测试', '模型测试', 'prediction', 'inference'],
  },
  {
    key: 'performance',
    label: '性能看板',
    path: ROUTES.VIZ,
    description: '按 HR、NDCG、MRR、数据规模和接入证据查看推荐模型对比。',
    promptHint: '打开性能看板',
    aliases: ['性能看板', '性能', '指标看板', '模型可视化', '模型分析', '对比图表', 'viz', 'performance'],
  },
  {
    key: 'datasetViz',
    label: '数据集实时可视化',
    path: ROUTES.DATASET_VIZ,
    description: '上传或选择数据集，查看字段、分布、缺失、图表和样例数据。',
    promptHint: '打开数据集实时可视化',
    aliases: ['数据集实时可视化', '数据集可视化', '数据可视化', '上传数据集', 'csv可视化', 'dataset', 'dataset visualization'],
  },
  {
    key: 'data',
    label: '数据中心',
    path: ROUTES.DATA,
    description: '在探索中心统一管理数据集、数据资产、云端素材和 AI 上下文资源。',
    promptHint: '进入数据中心',
    aliases: ['数据中心', '数据管理', '数据', '云端中心', '云空间', '云端', '素材库', '文件库', 'cloud', 'workspace', 'data'],
  },
  {
    key: 'analysisWorkbench',
    label: '图表工作台',
    path: ROUTES.ANALYSIS_WORKBENCH,
    description: '打开保留的训练运行、预测记录和分析批次工作台。',
    promptHint: '打开图表工作台',
    aliases: ['图表工作台', '可视化分析', '分析工作台', '分析页面', 'analysis workbench'],
  },
  {
    key: 'ai',
    label: 'AI 工作区',
    path: ROUTES.AI,
    description: '打开完整 AI 对话、素材上下文、深度思考和站内知识问答界面。',
    promptHint: '打开 AI 工作区',
    aliases: ['AI 工作区', 'ai工作区', 'AI 对话', '完整对话', '助手页面', 'deepseek', 'deepseek api', 'ai studio'],
  },
  {
    key: 'knowledge',
    label: '知识中心',
    path: ROUTES.KNOWLEDGE,
    description: '浏览模型知识、知识图谱、文章详情和学习资料。',
    promptHint: '打开知识中心',
    aliases: ['知识库', '知识中心', '知识', '文章', '教程', '资料', 'knowledge'],
  },
  {
    key: 'forum',
    label: '交流论坛',
    path: ROUTES.FORUM,
    description: '查看社区帖子、平台文章、经验分享和问题讨论。',
    promptHint: '打开交流论坛',
    aliases: ['论坛', '交流', '社区', '帖子', '平台文章', 'forum'],
  },
  {
    key: 'profile',
    label: '个人中心',
    path: ROUTES.PROFILE,
    description: '查看个人资料、账号信息和用户设置。',
    promptHint: '打开个人中心',
    aliases: ['个人中心', '个人资料', '账号', '用户信息', 'profile'],
  },
  {
    key: 'adminAi',
    label: '管理员-AI 配置',
    path: '/admin/ai',
    description: '配置 AI 服务商、模型、系统提示词、温度和上下文窗口。',
    promptHint: '打开管理员 AI 配置',
    aliases: ['AI配置', 'ai配置', '管理员AI', '模型配置', '系统提示词', 'admin ai'],
  },
  {
    key: 'adminKb',
    label: '管理员-知识库',
    path: '/admin/kb',
    description: '维护知识库文档、知识节点、分类、标签和检索内容。',
    promptHint: '打开管理员知识库',
    aliases: ['知识库管理', '管理员知识库', '管理知识', 'admin kb'],
  },
  {
    key: 'adminForum',
    label: '管理员-社区',
    path: '/admin/forum',
    description: '维护论坛帖子、平台文章和社区内容。',
    promptHint: '打开管理员社区',
    aliases: ['论坛管理', '社区管理', '文章管理', '管理员论坛', 'admin forum'],
  },
  {
    key: 'adminData',
    label: '管理员-数据',
    path: '/admin/data',
    description: '管理用户上传数据集、训练任务和数据资产记录。',
    promptHint: '打开管理员数据',
    aliases: ['数据管理后台', '管理员数据', '数据后台', 'admin data'],
  },
  {
    key: 'adminUsers',
    label: '管理员-用户',
    path: '/admin/users',
    description: '管理用户账号、角色、权限和状态。',
    promptHint: '打开管理员用户',
    aliases: ['用户管理', '管理员用户', '账号管理', '权限管理', 'admin users'],
  },
]

function normalizeText(text: string) {
  return text.trim().toLowerCase().replace(/\s+/g, '')
}

function includesAny(text: string, words: string[]) {
  return words.some((word) => text.includes(normalizeText(word)))
}

function scoreTarget(text: string, target: AssistantNavigationTarget) {
  const pathScore = text.includes(normalizeText(target.path)) ? 100 : 0
  const labelScore = text.includes(normalizeText(target.label)) ? 40 + normalizeText(target.label).length : 0
  const matchedAliases = target.aliases.filter((alias) => text.includes(normalizeText(alias)))
  if (pathScore + labelScore === 0 && matchedAliases.length === 0) return 0
  const longestAlias = matchedAliases.reduce((longest, alias) => Math.max(longest, normalizeText(alias).length), 0)
  return pathScore + labelScore + matchedAliases.length * 12 + longestAlias
}

export function resolveAssistantNavigation(
  rawInput: string,
  options: { allowImplicitTarget?: boolean } = {},
): AssistantNavigationIntent | null {
  const text = normalizeText(rawInput)
  if (!text) return null

  const hasNavigationIntent = options.allowImplicitTarget || includesAny(text, navigationWords)
  if (!hasNavigationIntent) return null

  const ranked = assistantNavigationTargets
    .map((target) => ({ target, score: scoreTarget(text, target) }))
    .filter((item) => item.score > 0)
    .sort((a, b) => b.score - a.score)

  const best = ranked[0]
  if (!best) return null

  return {
    ...best.target,
    confidence: Math.min(0.97, 0.58 + best.score / 120),
    reply: `我识别到你想前往「${best.target.label}」。可以直接点击下面的入口跳转，也可以继续告诉我你想在这个页面完成什么。`,
  }
}

export function normalizeAssistantNavigation(value: any): AssistantNavigationIntent | null {
  if (!value || typeof value !== 'object') return null
  const path = typeof value.path === 'string' ? value.path : ''
  const key = typeof value.key === 'string' ? value.key : ''
  const target = assistantNavigationTargets.find((item) => item.path === path || item.key === key)
  if (!target && !path) return null
  return {
    ...(target || {
      key: key || path,
      label: String(value.label || '页面入口'),
      path,
      description: String(value.description || '站内页面跳转入口'),
      promptHint: String(value.promptHint || value.label || '打开页面'),
      aliases: [],
    }),
    label: String(value.label || target?.label || '页面入口'),
    path: String(value.path || target?.path || path),
    description: String(value.description || target?.description || '站内页面跳转入口'),
    promptHint: String(value.promptHint || target?.promptHint || value.label || '打开页面'),
    confidence: Number(value.confidence || 0.8),
    mode: value.mode === 'direct' || value.mode === 'suggested' ? value.mode : undefined,
    reason: typeof value.reason === 'string' ? value.reason : undefined,
    reply: String(value.reply || `已为你准备好「${value.label || target?.label || '页面'}」的跳转入口。`),
  }
}
