import { ROUTES } from '@/constants';

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
  'open',
  'go',
  'show',
  'view',
  'navigate',
];

export const assistantNavigationTargets: AssistantNavigationTarget[] = [
  {
    key: 'dashboard',
    label: '系统总览',
    path: ROUTES.DASHBOARD,
    description: '查看平台状态、关键指标和入口概览。',
    promptHint: '打开系统总览',
    aliases: ['系统总览', '首页', '仪表盘', 'dashboard', '总览', '概览'],
  },
  {
    key: 'data',
    label: '数据管理',
    path: ROUTES.DATA,
    description: '上传数据集、管理数据资产和训练输入。',
    promptHint: '去数据管理上传数据',
    aliases: ['数据管理', '数据', '数据集', '上传数据', 'dataset', 'data'],
  },
  {
    key: 'training',
    label: '模型总览',
    path: ROUTES.TRAINING,
    description: '查看 BSARec 和对比模型的结构、指标与参数依据。',
    promptHint: '看看模型总览',
    aliases: ['模型总览', '模型', '训练', '模型列表', 'BSARec', 'SASRec', 'BERT4Rec', 'TiSASRec', 'FMLP', 'training', 'model overview'],
  },
  {
    key: 'viz',
    label: '可视化分析',
    path: ROUTES.VIZ,
    description: '查看模型证据板、预测结果回流和多维分析图表。',
    promptHint: '去可视化分析',
    aliases: ['可视化分析', '可视化', '分析页面', '分析', '图表', '模型分析', '预测结果', 'viz', 'visualization', 'analysis'],
  },
  {
    key: 'prediction',
    label: '预测推理',
    path: ROUTES.PREDICTION,
    description: '调用 BSARec 在线推荐并查看推理结果。',
    promptHint: '打开预测推理',
    aliases: ['预测推理', '预测', '推理', '推荐', '岗位推荐', 'recommend', 'prediction', 'inference'],
  },
  {
    key: 'knowledge',
    label: '知识库',
    path: ROUTES.KNOWLEDGE,
    description: '浏览模型知识、文章和学习资料。',
    promptHint: '打开知识库',
    aliases: ['知识库', '知识', '文章', '教程', '资料', 'knowledge'],
  },
  {
    key: 'cloud',
    label: '云空间',
    path: ROUTES.CLOUD,
    description: '管理保存的素材、文件和 AI 上下文资源。',
    promptHint: '进入云空间',
    aliases: ['云空间', '云端', '素材库', '文件库', '素材', 'cloud', 'workspace'],
  },
  {
    key: 'forum',
    label: '交流论坛',
    path: ROUTES.FORUM,
    description: '查看社区帖子、经验分享和问题讨论。',
    promptHint: '打开交流论坛',
    aliases: ['论坛', '交流', '社区', '帖子', 'forum'],
  },
  {
    key: 'ai',
    label: 'AI 工作室',
    path: ROUTES.AI,
    description: '打开完整 AI 对话、素材上下文和后续 DeepSeek 接入工作台。',
    promptHint: '打开 AI 工作室',
    aliases: ['AI 工作室', 'ai工作室', 'AI 对话', '完整对话', '助手页面', 'deepseek', 'deepseek api', 'ai studio'],
  },
];

function normalizeText(text: string) {
  return text.trim().toLowerCase().replace(/\s+/g, '');
}

function includesAny(text: string, words: string[]) {
  return words.some((word) => text.includes(normalizeText(word)));
}

function scoreTarget(text: string, target: AssistantNavigationTarget) {
  const matchedAliases = target.aliases.filter((alias) => text.includes(normalizeText(alias)));
  if (matchedAliases.length === 0) return 0;
  const longestAlias = matchedAliases.reduce((longest, alias) => Math.max(longest, normalizeText(alias).length), 0);
  return matchedAliases.length * 10 + longestAlias;
}

export function resolveAssistantNavigation(rawInput: string): AssistantNavigationIntent | null {
  const text = normalizeText(rawInput);
  if (!text) return null;

  const hasNavigationIntent = includesAny(text, navigationWords);
  if (!hasNavigationIntent) return null;

  const ranked = assistantNavigationTargets
    .map((target) => ({ target, score: scoreTarget(text, target) }))
    .filter((item) => item.score > 0)
    .sort((a, b) => b.score - a.score);

  const best = ranked[0];
  if (!best) return null;

  return {
    ...best.target,
    confidence: Math.min(0.96, 0.58 + best.score / 100),
    reply: `我识别到你想前往「${best.target.label}」。可以直接点击下面的按钮跳转，也可以继续告诉我你想在这个页面完成什么。`,
  };
}
