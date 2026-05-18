import type { ModelOption } from '@/types/models'

type LocaleLike = string | { value: string }

type MetricCopy = {
  label: string
  shortLabel: string
  description: string
}

const taskTypeZh: Record<string, string> = {
  classification: '图像分类',
  detection: '目标检测',
  segmentation: '语义分割',
  recommendation: '推荐系统',
  nlp: '自然语言处理',
  other: '其他任务',
}

const taskTypeEn: Record<string, string> = {
  classification: 'Classification',
  detection: 'Detection',
  segmentation: 'Segmentation',
  recommendation: 'Recommendation',
  nlp: 'NLP',
  other: 'Other',
}

const modelNameZh: Record<string, string> = {
  'ResNet-50': '残差网络 50 层',
  'ResNet-101': '残差网络 101 层',
  'VGG-19': 'VGG 19 层卷积网络',
  'EfficientNet-B4': '高效卷积网络 B4',
  'ViT-B/16': '视觉 Transformer 基础版',
  'Swin-T': 'Swin Transformer 轻量版',
  'ConvNeXt-T': '现代化卷积网络轻量版',
  'MobileNetV3-L': '移动端高效网络大版',
  'DenseNet-201': '密集连接卷积网络 201 层',
  YOLOv8n: 'YOLOv8 Nano 实时检测',
  YOLOv8s: 'YOLOv8 Small 目标检测',
  YOLOv8: 'YOLOv8 目标检测',
  'DeepLabV3-RN50': 'DeepLabV3 语义分割',
  DeepFM: '深度因子分解推荐模型',
  'Wide&Deep': '宽深联合推荐模型',
  NCF: '神经协同过滤模型',
  DIN: '深度兴趣网络',
  'BERT-Base': 'BERT 文本理解基础版',
  'GPT-2': 'GPT-2 文本生成模型',
  'T5-Small': 'T5 文本到文本小型版',
  'LLaMA-7B': 'LLaMA 7B 大语言模型',
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
    zh: { label: '验证损失 Val Loss', shortLabel: '验证损失', description: '验证集预测误差，用来观察泛化能力。' },
    en: { label: 'Validation Loss', shortLabel: 'Val Loss', description: 'Validation-set error used to monitor generalization.' },
  },
  valAccuracy: {
    zh: { label: '验证准确率 Val Accuracy', shortLabel: '验证准确率', description: '验证集预测正确率，用来判断是否过拟合。' },
    en: { label: 'Validation Accuracy', shortLabel: 'Val Accuracy', description: 'Validation-set correctness used to watch overfitting.' },
  },
  learningRate: {
    zh: { label: '学习率 Learning Rate', shortLabel: '学习率', description: '每次参数更新的步长，过大易震荡，过小会变慢。' },
    en: { label: 'Learning Rate', shortLabel: 'LR', description: 'Step size for parameter updates.' },
  },
  prCurve: {
    zh: { label: '精确率/召回率 PR 曲线', shortLabel: 'PR 曲线', description: '不同阈值下精确率与召回率的权衡。' },
    en: { label: 'Precision/Recall Curve', shortLabel: 'PR Curve', description: 'Tradeoff between precision and recall across thresholds.' },
  },
  embedding: {
    zh: { label: '嵌入投影 Embedding', shortLabel: '嵌入投影', description: '把高维特征压到二维/三维，观察类别聚类和分离。' },
    en: { label: 'Embedding Projection', shortLabel: 'Embedding', description: 'Projects high-dimensional features to inspect clusters.' },
  },
  histogram: {
    zh: { label: '权重/梯度直方图 Histogram', shortLabel: '直方图', description: '查看权重或梯度分布，定位饱和、异常值和梯度问题。' },
    en: { label: 'Weight/Gradient Histogram', shortLabel: 'Histogram', description: 'Shows weight or gradient distribution.' },
  },
  profiler: {
    zh: { label: '性能剖析 Profiler', shortLabel: '性能剖析', description: '定位算子耗时、数据拷贝和 GPU 利用率瓶颈。' },
    en: { label: 'Profiler', shortLabel: 'Profiler', description: 'Finds operator, copy, and GPU utilization bottlenecks.' },
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
