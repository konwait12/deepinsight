import type { ModelOption } from '@/types/models'

type LocaleLike = string | { value: string }

type MetricCopy = {
  label: string
  shortLabel: string
  description: string
}

const taskTypeZh: Record<string, string> = {
  recommendation: '推荐系统',
  other: '其他任务',
}

const taskTypeEn: Record<string, string> = {
  recommendation: 'Recommendation',
  other: 'Other',
}

const modelNameZh: Record<string, string> = {
  'BSARec-Job': 'BSARec 岗位推荐模型',
  BSARec: 'BSARec 序列推荐模型',
  BERT4Rec: 'BERT4Rec 双向序列推荐模型',
  DuoRec: 'DuoRec 对比学习序列推荐模型',
  FEARec: 'FEARec 频域增强序列推荐模型',
  'FMLP-Rec': 'FMLP-Rec 滤波增强 MLP 推荐模型',
  RecBole: 'RecBole 推荐算法框架',
  SASRec: 'SASRec 自注意力序列推荐模型',
  TiSASRec: 'TiSASRec 时间间隔感知推荐模型',
}

export const metricCopy = {
  loss: {
    zh: { label: '训练损失 Loss', shortLabel: '损失', description: '模型预测误差，越低通常表示训练拟合越好。' },
    en: { label: 'Training Loss', shortLabel: 'Loss', description: 'Prediction error. Lower usually means better fit.' },
  },
  accuracy: {
    zh: { label: '训练准确率 Accuracy', shortLabel: '准确率', description: '预测正确的样本比例，越高越好。' },
    en: { label: 'Training Accuracy', shortLabel: 'Accuracy', description: 'Share of correct predictions. Higher is better.' },
  },
  valLoss: {
    zh: { label: '验证损失 Val Loss', shortLabel: '验证损失', description: '验证集误差，用来观察泛化能力。' },
    en: { label: 'Validation Loss', shortLabel: 'Val Loss', description: 'Validation-set error used to monitor generalization.' },
  },
  valAccuracy: {
    zh: { label: '验证准确率 Val Accuracy', shortLabel: '验证准确率', description: '验证集预测正确率，用来判断是否过拟合。' },
    en: { label: 'Validation Accuracy', shortLabel: 'Val Accuracy', description: 'Validation-set correctness used to watch overfitting.' },
  },
  learningRate: {
    zh: { label: '学习率 Learning Rate', shortLabel: '学习率', description: '每次参数更新的步长。' },
    en: { label: 'Learning Rate', shortLabel: 'LR', description: 'Step size for parameter updates.' },
  },
  prCurve: {
    zh: { label: '精确率/召回率 PR 曲线', shortLabel: 'PR 曲线', description: '不同阈值下精确率与召回率的权衡。' },
    en: { label: 'Precision/Recall Curve', shortLabel: 'PR Curve', description: 'Tradeoff between precision and recall across thresholds.' },
  },
  embedding: {
    zh: { label: '嵌入投影 Embedding', shortLabel: '嵌入投影', description: '把高维特征压到二维或三维，观察聚类结构。' },
    en: { label: 'Embedding Projection', shortLabel: 'Embedding', description: 'Projects high-dimensional features to inspect clusters.' },
  },
  histogram: {
    zh: { label: '权重/梯度直方图 Histogram', shortLabel: '直方图', description: '查看权重或梯度分布，定位异常值和梯度问题。' },
    en: { label: 'Weight/Gradient Histogram', shortLabel: 'Histogram', description: 'Shows weight or gradient distribution.' },
  },
  profiler: {
    zh: { label: '性能剖析 Profiler', shortLabel: '性能剖析', description: '定位算子耗时、数据拷贝和吞吐瓶颈。' },
    en: { label: 'Profiler', shortLabel: 'Profiler', description: 'Finds operator, copy, and utilization bottlenecks.' },
  },
} as const

export function isZhLocale(locale: LocaleLike) {
  const value = typeof locale === 'string' ? locale : locale.value
  return value.startsWith('zh')
}

export function taskTypeLabel(taskType?: string, taskTypeZhValue?: string, locale: LocaleLike = 'zh') {
  if (!taskType) return isZhLocale(locale) ? '未知任务' : 'Unknown'
  if (isZhLocale(locale)) return taskTypeZhValue || taskTypeZh[taskType] || taskType
  return taskTypeEn[taskType] || taskType
}

export function modelDisplayName(model: Pick<ModelOption, 'name' | 'displayNameZh'> | string, locale: LocaleLike = 'zh') {
  const name = typeof model === 'string' ? model : model.name
  const zhName = typeof model === 'string' ? modelNameZh[name] : model.displayNameZh || modelNameZh[name]
  if (!isZhLocale(locale) || !zhName || zhName === name) return name
  return `${zhName} (${name})`
}

export function compactModelName(model: Pick<ModelOption, 'name' | 'displayNameZh'> | string, locale: LocaleLike = 'zh') {
  const name = typeof model === 'string' ? model : model.name
  const zhName = typeof model === 'string' ? modelNameZh[name] : model.displayNameZh || modelNameZh[name]
  return isZhLocale(locale) && zhName ? zhName : name
}

export function modelDescription(model: ModelOption, locale: LocaleLike = 'zh') {
  return isZhLocale(locale) ? model.descriptionZh || model.description || '' : model.description || model.descriptionZh || ''
}

export function metricText(key: keyof typeof metricCopy, locale: LocaleLike = 'zh'): MetricCopy {
  return metricCopy[key][isZhLocale(locale) ? 'zh' : 'en']
}
