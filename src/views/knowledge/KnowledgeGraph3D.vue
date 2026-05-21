<template>
  <div ref="box" class="kg-root" @mousedown="onDn" @wheel="onWh" @click="onClick">
    <canvas ref="cv"></canvas>

    <!-- 右侧知识文章面板 -->
    <div class="kg-article-panel" :class="{ open: panelOpen }">
      <button class="focus-back" @click.stop="goBack">{{ tt('kg3d.backGlobal') }}</button>
      <div class="panel-header">
        <div>
          <span class="panel-badge">{{ sel?.category || '' }}</span>
          <h3>{{ sel?.label || '' }}</h3>
        </div>
        <button class="panel-close" @click="closePanel">&times;</button>
      </div>
      <p class="panel-desc">{{ sel?.desc || '' }}</p>
      <div v-if="sel?.childCount>0" class="panel-expand">
        <button @click="toggleNode(sel.id)">{{ sel.expanded ? tt('kg3d.collapseChildren') : tt('kg3d.expandChildren', { count: sel.childCount }) }}</button>
      </div>
      <div class="panel-divider"></div>
      <h4 class="panel-section-title">{{ tt('kg3d.relatedArticles') }}</h4>
      <div v-if="articles.length===0&&!articlesLoading" class="panel-empty">{{ tt('kg3d.noArticles') }}</div>
      <div v-if="articlesLoading" class="panel-loading">{{ tt('kg3d.loading') }}</div>
      <div class="panel-articles">
        <div v-for="a in articles" :key="a.id" class="article-item" @click.stop="openArticle(a)">
          <div class="article-title">{{ a.title }}</div>
          <div class="article-meta">{{ tt('kg3d.views') }} {{ a.viewCount || 0 }}</div>
        </div>
      </div>
    </div>

    <!-- 底部控制栏 -->
    <div class="kg-controls">
      <div class="kg-control-row">
        <span class="ctrl-label">{{ tt('kg3d.rotation') }}</span>
        <input type="range" min="0" max="3" step="0.1" :value="rotSpeed" @input="onSpeed" class="ctrl-slider" />
        <span class="ctrl-val">{{ displayRotSpeed.toFixed(1) }}x</span>
      </div>
      <button class="ctrl-toggle" :class="{ active: physicsOn }" @click="physicsOn=!physicsOn">
        {{ physicsOn ? tt('kg3d.physicsOn') : tt('kg3d.physicsOff') }}
      </button>
    </div>

    <div class="kg-hint">{{ tt('kg3d.hint') }}</div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted } from 'vue';
import { useThemeStore } from '@/stores/theme.store';
import { useI18n } from 'vue-i18n';
const theme = useThemeStore();
const { t: tt } = useI18n();
const emit = defineEmits<{ openArticle: [article: any] }>();
const box = ref<HTMLDivElement>(); const cv = ref<HTMLCanvasElement>();
const sel = ref<any>(null); const panelOpen = ref(false);
const articles = ref<any[]>([]); const articlesLoading = ref(false);
let selectedNodeId: string|null = null;
const DEFAULT_ROT_SPEED = 1.0;
const HOVER_ROT_SPEED = 0.5;
const rotSpeed = ref(DEFAULT_ROT_SPEED); const physicsOn = ref(false);
const hoverSpeedActive = ref(false);
const displayRotSpeed = computed(() => hoverSpeedActive.value ? HOVER_ROT_SPEED : rotSpeed.value);

let c: CanvasRenderingContext2D | null = null;
let W = 800, H = 600, rid = 0, t = 0;
let ry = 0, rx = -0.8, zm = 1;
let try_ = 0, trx = -0.8, tzm = 1;
let drag = false, dx = 0, dy = 0, sry = 0, srx = 0;
let dragEndTime = 0, dragEver = false;
let hoveredNodeId: string|null = null;
let focusNodeId: string|null = null;

let focusZoom = 1;
let targetFocusZoom = 1;
let panX = 0, panY = 0, targetPanX = 0, targetPanY = 0;
let mouseX = 0, mouseY = 0;

type ThemeCanvasColors = {
  primary: string;
  primaryRgb: string;
  accent: string;
  bg: string;
  surface: string;
  text: string;
  muted: string;
  grid: string;
};

const alphaColor = (rgb: string, alpha: number) => `rgba(${rgb},${alpha})`;
type Rgb = { r: number; g: number; b: number };

const clamp01 = (value: number) => Math.max(0, Math.min(1, value));
const rgbToCss = ({ r, g, b }: Rgb) => `rgb(${Math.round(r)},${Math.round(g)},${Math.round(b)})`;
const parseCanvasColor = (color: string): Rgb | null => {
  const clean = color.trim();
  if (clean.startsWith('#')) {
    const hex = clean.slice(1);
    const full = hex.length === 3 ? hex.split('').map(ch => ch + ch).join('') : hex;
    const r = parseInt(full.slice(0, 2), 16);
    const g = parseInt(full.slice(2, 4), 16);
    const b = parseInt(full.slice(4, 6), 16);
    return [r, g, b].every(Number.isFinite) ? { r, g, b } : null;
  }
  const rgb = clean.match(/rgba?\(([^)]+)\)/);
  if (!rgb) return null;
  const [r, g, b] = rgb[1].split(',').slice(0, 3).map(part => parseFloat(part.trim()));
  return [r, g, b].every(Number.isFinite) ? { r, g, b } : null;
};

const mixColor = (from: string, to: string, amount: number) => {
  const a = parseCanvasColor(from);
  const b = parseCanvasColor(to);
  if (!a || !b) return from;
  const t = clamp01(amount);
  return rgbToCss({
    r: a.r + (b.r - a.r) * t,
    g: a.g + (b.g - a.g) * t,
    b: a.b + (b.b - a.b) * t,
  });
};

const colorLuminance = (color: string) => {
  const rgb = parseCanvasColor(color);
  if (!rgb) return 0.5;
  return (0.2126 * rgb.r + 0.7152 * rgb.g + 0.0722 * rgb.b) / 255;
};

const hashText = (value: string) => {
  let hash = 2166136261;
  for (const ch of value) {
    hash ^= ch.charCodeAt(0);
    hash = Math.imul(hash, 16777619);
  }
  return Math.abs(hash);
};

const colorWithAlpha = (color: string, alpha: number) => {
  const clean = color.trim();
  if (clean.startsWith('#')) {
    const hex = clean.slice(1);
    const full = hex.length === 3 ? hex.split('').map(ch => ch + ch).join('') : hex;
    const r = parseInt(full.slice(0, 2), 16);
    const g = parseInt(full.slice(2, 4), 16);
    const b = parseInt(full.slice(4, 6), 16);
    if ([r, g, b].every(Number.isFinite)) return `rgba(${r},${g},${b},${alpha})`;
  }
  const rgb = clean.match(/rgba?\(([^)]+)\)/);
  if (rgb) {
    const parts = rgb[1].split(',').slice(0, 3).join(',');
    return `rgba(${parts},${alpha})`;
  }
  return clean;
};
const getCanvasColors = (dark: boolean): ThemeCanvasColors => {
  const root = getComputedStyle(document.documentElement);
  const primary = root.getPropertyValue('--primary-color').trim() || '#42e6a4';
  const primaryRgb = root.getPropertyValue('--primary-rgb').trim() || '66,230,164';
  return {
    primary,
    primaryRgb,
    accent: root.getPropertyValue('--accent-glow').trim() || primary,
    bg: root.getPropertyValue('--bg-color').trim() || (dark ? '#05070a' : '#f3f6f2'),
    surface: root.getPropertyValue('--surface-solid').trim() || (dark ? '#0d1118' : '#fbfcf7'),
    text: root.getPropertyValue('--text-primary').trim() || (dark ? '#f7f3ea' : '#141922'),
    muted: root.getPropertyValue('--text-muted').trim() || (dark ? '#707d8f' : '#88919f'),
    grid: alphaColor(primaryRgb, dark ? 0.08 : 0.07),
  };
};
const nodePalette = [
  '#4dc9f0',
  '#42e6a4',
  '#8b5cf6',
  '#f59e0b',
  '#fb7185',
  '#14b8a6',
  '#60a5fa',
  '#ec4899',
  '#22c55e',
  '#a78bfa',
  '#f97316',
  '#06b6d4',
  '#eab308',
  '#ef4444',
  '#84cc16',
  '#38bdf8',
  '#f472b6',
  '#2dd4bf',
];

const nodeVisualColor = (rn: Pick<RNode, 'id' | 'depth' | 'cat' | 'label' | 'color'>, colors: ThemeCanvasColors) => {
  if (rn.depth === 0) return colors.primary;
  const seed = hashText(`${rn.id}:${rn.cat}:${rn.label}:${rn.depth}`);
  const paletteColor = nodePalette[seed % nodePalette.length];
  const nextColor = nodePalette[(seed + rn.depth * 5 + 3) % nodePalette.length];
  const sourceColor = parseCanvasColor(rn.color) && colorLuminance(rn.color) < 0.82 ? rn.color : paletteColor;
  return mixColor(sourceColor, nextColor, rn.depth === 1 ? 0.28 : 0.42);
};

const nodeVisualTone = (rn: Pick<RNode, 'id' | 'depth' | 'cat' | 'label' | 'color'>, colors: ThemeCanvasColors, dark: boolean) => {
  const base = nodeVisualColor(rn, colors);
  const accent = mixColor(nodePalette[(hashText(`${rn.label}:accent`) + 7) % nodePalette.length], colors.accent, 0.18);
  return {
    core: mixColor(base, colors.primary, rn.depth === 0 ? 0.18 : 0.07),
    rim: mixColor(base, '#ffffff', dark ? 0.24 : 0.18),
    deep: mixColor(base, dark ? '#020617' : '#10261f', dark ? 0.5 : 0.34),
    glow: mixColor(base, accent, 0.28),
    highlight: mixColor('#ffffff', base, dark ? 0.12 : 0.18),
  };
};

const getEffectiveRotSpeed = () => hoveredNodeId ? HOVER_ROT_SPEED : rotSpeed.value;
const setHoveredNode = (id: string | null) => {
  hoveredNodeId = id;
  hoverSpeedActive.value = Boolean(id);
};

const proj = (x: number, y: number, z: number) => {
  const cy = Math.cos(ry), sy = Math.sin(ry), cx = Math.cos(rx), sx = Math.sin(rx);
  let rxx = x * cy - z * sy, rz = x * sy + z * cy;
  let ryy = y * cx - rz * sx, rz2 = y * sx + rz * cx;
  let effZm = zm * focusZoom;
  let d = 6 * effZm, sc = d / (d + rz2);
  return { x: W / 2 + rxx * sc * W * 0.18 + panX, y: H / 2 - ryy * sc * H * 0.18 + panY, s: sc };
};

/* ======== 知识树 (同上) ======== */
interface KNode { id: string; label: string; cat: string; desc: string; color: string; size: number; children?: KNode[]; rel: string[]; }
const TREE: KNode = {
  id: 'd', label: 'DeepInsight', cat: '平台核心', desc: '深度学习全流程可视化分析平台', color: '#4dc9f0', size: 2.5, rel: [],
  children: [
    { id: 'data', label: '数据管理', cat: '功能模块', desc: '数据集上传、预处理、增强与版本管理', color: '#60a5fa', size: 1.5, rel: [], children: [
      { id: 'prep', label: '数据预处理', cat: '数据处理', desc: '标准化、归一化、缺失值填充等', color: '#93c5fd', size: 1.0, rel: [], children: [
        { id: 'norm', label: '标准化', cat: '数据处理', desc: 'Z-Score：(x-μ)/σ', color: '#bae6fd', size: 0.8, rel: [] },
        { id: 'nrm', label: '归一化', cat: '数据处理', desc: 'Min-Max缩放至[0,1]', color: '#bae6fd', size: 0.8, rel: [] },
        { id: 'missing', label: '缺失值处理', cat: '数据处理', desc: '删除/填充/预测填充', color: '#bae6fd', size: 0.75, rel: [] },
        { id: 'onehot', label: 'One-Hot编码', cat: '数据处理', desc: '类别变量转二进制向量', color: '#bae6fd', size: 0.75, rel: [] },
      ]},
      { id: 'aug', label: '数据增强', cat: '数据处理', desc: '旋转/翻转/裁剪扩充数据集', color: '#7dd3fc', size: 1.0, rel: [], children: [
        { id: 'aug-geo', label: '几何变换', cat: '增强', desc: '旋转/翻转/缩放/裁剪', color: '#cffafe', size: 0.7, rel: [] },
        { id: 'aug-color', label: '色彩变换', cat: '增强', desc: '亮度/对比度/饱和度', color: '#cffafe', size: 0.7, rel: [] },
        { id: 'mixup', label: 'MixUp', cat: '增强', desc: '图像混合标签混合', color: '#cffafe', size: 0.7, rel: [] },
        { id: 'cutmix', label: 'CutMix', cat: '增强', desc: '区域剪切粘贴融合', color: '#cffafe', size: 0.7, rel: [] },
      ]},
      { id: 'split', label: '数据集划分', cat: '数据处理', desc: '训练/验证/测试7:2:1', color: '#7dd3fc', size: 0.9, rel: [], children: [
        { id: 'kfold', label: 'K折交叉验证', cat: '验证', desc: '分K份轮换验证', color: '#cffafe', size: 0.7, rel: [] },
      ]},
    ]},
    { id: 'train', label: '模型训练', cat: '功能模块', desc: '超参数配置、架构选择、训练监控', color: '#10b981', size: 1.5, rel: [], children: [
      { id: 'sl', label: '监督学习', cat: '学习范式', desc: '标注数据学习输入→输出映射', color: '#34d399', size: 1.0, rel: [], children: [
        { id: 'cls', label: '分类任务', cat: '任务', desc: '预测离散类别标签', color: '#a7f3d0', size: 0.8, rel: [] },
        { id: 'reg', label: '回归任务', cat: '任务', desc: '预测连续数值', color: '#a7f3d0', size: 0.8, rel: [] },
      ]},
      { id: 'ul', label: '无监督学习', cat: '学习范式', desc: '未标注数据发现隐藏模式', color: '#34d399', size: 1.0, rel: [], children: [
        { id: 'cluster', label: '聚类', cat: '算法', desc: 'K-Means/DBSCAN/层次', color: '#a7f3d0', size: 0.75, rel: [] },
        { id: 'pca', label: 'PCA降维', cat: '算法', desc: '主成分分析线性降维', color: '#a7f3d0', size: 0.75, rel: [] },
      ]},
      { id: 'tl', label: '迁移学习', cat: '学习范式', desc: '预训练模型微调适配新任务', color: '#34d399', size: 1.0, rel: [], children: [
        { id: 'ft', label: 'Fine-tuning', cat: '技术', desc: '冻结部分层训练最后几层', color: '#a7f3d0', size: 0.75, rel: [] },
      ]},
    ]},
    { id: 'arch', label: '模型架构', cat: '功能模块', desc: 'CNN/RNN/Transformer/GAN等', color: '#8b5cf6', size: 1.5, rel: [], children: [
      { id: 'cnn-series', label: 'CNN系列', cat: '架构族', desc: '卷积神经网络, 空间特征', color: '#a78bfa', size: 1.1, rel: [], children: [
        { id: 'cnn', label: 'CNN基础', cat: '架构', desc: '卷积+池化+全连接', color: '#c4b5fd', size: 0.85, rel: [] },
        { id: 'resnet', label: 'ResNet', cat: '架构', desc: '残差网络跳跃连接', color: '#c4b5fd', size: 0.9, rel: [] },
        { id: 'effnet', label: 'EfficientNet', cat: '架构', desc: 'NAS复合缩放', color: '#c4b5fd', size: 0.85, rel: [] },
      ]},
      { id: 'trans-series', label: 'Transformer系列', cat: '架构族', desc: '自注意力, NLP/CV统治', color: '#c084fc', size: 1.2, rel: [], children: [
        { id: 'trans', label: 'Transformer', cat: '架构', desc: 'Self-Attention+位置编码', color: '#d8b4fe', size: 0.9, rel: [] },
        { id: 'vit', label: 'ViT', cat: '架构', desc: 'Vision Transformer图像patch', color: '#d8b4fe', size: 0.85, rel: [] },
        { id: 'gpt', label: 'GPT', cat: '架构', desc: '自回归语言模型', color: '#d8b4fe', size: 0.9, rel: [] },
      ]},
      { id: 'det-seg', label: '检测与分割', cat: '架构族', desc: '目标检测/语义分割', color: '#e879f9', size: 1.0, rel: [], children: [
        { id: 'yolo', label: 'YOLO', cat: '架构', desc: '实时目标检测标杆', color: '#f0abfc', size: 0.85, rel: [] },
        { id: 'unet', label: 'U-Net', cat: '架构', desc: '编码-解码+跳跃连接', color: '#f0abfc', size: 0.8, rel: [] },
      ]},
    ]},
    { id: 'tech', label: '训练技巧', cat: '功能模块', desc: '优化器/学习率/正则化等核心技术', color: '#f59e0b', size: 1.4, rel: [], children: [
      { id: 'opt', label: '优化器', cat: '核心概念', desc: 'SGD/Adam/AdamW参数更新', color: '#fbbf24', size: 1.1, rel: [], children: [
        { id: 'sgd', label: 'SGD', cat: '优化器', desc: '随机梯度下降', color: '#fde68a', size: 0.8, rel: [] },
        { id: 'adam', label: 'Adam', cat: '优化器', desc: '自适应矩估计', color: '#fde68a', size: 0.85, rel: [] },
      ]},
      { id: 'reg', label: '正则化', cat: '核心概念', desc: '防止过拟合的技术集合', color: '#fbbf24', size: 1.0, rel: [], children: [
        { id: 'dropout', label: 'Dropout', cat: '正则化', desc: '随机丢弃神经元', color: '#fde68a', size: 0.85, rel: [] },
        { id: 'bn', label: 'BatchNorm', cat: '正则化', desc: '批归一化加速训练', color: '#fde68a', size: 0.85, rel: [] },
        { id: 'ln', label: 'LayerNorm', cat: '正则化', desc: '层归一化Transformer标配', color: '#fde68a', size: 0.75, rel: [] },
      ]},
      { id: 'loss', label: '损失函数', cat: '核心概念', desc: '衡量预测与真实差距', color: '#f87171', size: 1.1, rel: [], children: [
        { id: 'ce', label: '交叉熵', cat: '损失', desc: '分类任务标准损失', color: '#fca5a5', size: 0.85, rel: [] },
        { id: 'mse', label: 'MSE', cat: '损失', desc: '均方误差', color: '#fca5a5', size: 0.8, rel: [] },
      ]},
    ]},
    { id: 'eval', label: '评估指标', cat: '功能模块', desc: '准确率/召回率/mAP/ROC', color: '#ec4899', size: 1.3, rel: [], children: [
      { id: 'cm', label: '混淆矩阵', cat: '指标', desc: 'TP/FP/FN/TN四格分布', color: '#fbcfe8', size: 0.85, rel: [] },
      { id: 'roc', label: 'ROC曲线', cat: '指标', desc: 'TPR-FPR关系AUC面积', color: '#fbcfe8', size: 0.85, rel: [] },
    ]},
    { id: 'deploy', label: '部署推理', cat: '功能模块', desc: '模型压缩/导出/加速推理', color: '#14b8a6', size: 1.3, rel: [], children: [
      { id: 'quant', label: '量化', cat: '压缩', desc: 'FP32→INT8/FP16减小模型', color: '#ccfbf1', size: 0.8, rel: [] },
      { id: 'prune', label: '剪枝', cat: '压缩', desc: '移除不重要权重', color: '#ccfbf1', size: 0.75, rel: [] },
      { id: 'distill', label: '知识蒸馏', cat: '压缩', desc: '大模型教小模型', color: '#ccfbf1', size: 0.8, rel: [] },
    ]},
  ]
};

/* ======== 运行时节点 ======== */
interface RNode {
  id: string; label: string; cat: string; desc: string; color: string; size: number;
  parentId: string | null; depth: number; expanded: boolean; childIds: string[]; rel: string[];
  orbitR: number; orbitAng: number; orbitH: number; orbitSpd: number;
  x: number; y: number; z: number; sx: number; sy: number; sr: number;
  vx: number; vy: number; vz: number; tx: number; ty: number; tz: number; mode: number;
  visible?: boolean; _wasVisible?: boolean;
}
const MODE = { ORBIT: 0, FALLING: 1, LANDED: 2, RETURNING: 3 } as const;
let rnodes: RNode[] = []; let nodeMap = new Map<string, RNode>();
let conns: [number, number][] = [];
let bg: Array<{x:number;y:number;r:number;a:number;sp:number}> = [];
let trails: Array<Array<{x:number;y:number}>> = [];
let shooters: Array<{x:number;y:number;vx:number;vy:number;life:number;max:number}> = [];

/* ======== 布料物理 ======== */
const CLOTH_W = 36, CLOTH_H = 24;
const PLANE_Y = -3.0;
const CLOTH_X0 = -6.5, CLOTH_Z0 = -6.5, CLOTH_X1 = 6.5, CLOTH_Z1 = 6.5;
let hCurr = new Float32Array(CLOTH_W * CLOTH_H);
let hPrev = new Float32Array(CLOTH_W * CLOTH_H);
const gi = (ix: number, iz: number) => ix + iz * CLOTH_W;
const gx = (ix: number) => CLOTH_X0 + (ix / (CLOTH_W - 1)) * (CLOTH_X1 - CLOTH_X0);
const gz = (iz: number) => CLOTH_Z0 + (iz / (CLOTH_H - 1)) * (CLOTH_Z1 - CLOTH_Z0);

const updateCloth = (dt: number) => {
  if (!physicsOn.value && !dragEver) return; // 物理关闭且无待恢复状态时跳过
  const grav = 0.018, stiff = 0.42, damp = 0.991;
  const acc = stiff * dt * dt;
  // Verlet: 重力 + 张力
  for (let iz = 1; iz < CLOTH_H - 1; iz++) {
    for (let ix = 1; ix < CLOTH_W - 1; ix++) {
      const i = gi(ix, iz);
      const lap = hCurr[i - 1] + hCurr[i + 1] + hCurr[i - CLOTH_W] + hCurr[i + CLOTH_W] - 4 * hCurr[i];
      const diag = hCurr[i - 1 - CLOTH_W] + hCurr[i + 1 - CLOTH_W] + hCurr[i - 1 + CLOTH_W] + hCurr[i + 1 + CLOTH_W] - 4 * hCurr[i];
      const nxt = 2 * hCurr[i] - hPrev[i] + acc * (lap + diag * 0.3) - grav * dt * dt;
      hPrev[i] = hCurr[i];
      hCurr[i] = nxt * damp;
    }
  }
  // 迭代松弛约束 (使布料更硬挺)
  for (let iter = 0; iter < 3; iter++) {
    for (let iz = 1; iz < CLOTH_H - 1; iz++) {
      for (let ix = 1; ix < CLOTH_W - 1; ix++) {
        const i = gi(ix, iz);
        const avg = (hCurr[i - 1] + hCurr[i + 1] + hCurr[i - CLOTH_W] + hCurr[i + CLOTH_W]) * 0.25;
        hCurr[i] += (avg - hCurr[i]) * 0.16;
      }
    }
  }
  // 边缘固定
  for (let iz = 0; iz < CLOTH_H; iz++) {
    hCurr[gi(0, iz)] = hPrev[gi(0, iz)] = 0;
    hCurr[gi(CLOTH_W - 1, iz)] = hPrev[gi(CLOTH_W - 1, iz)] = 0;
  }
  for (let ix = 0; ix < CLOTH_W; ix++) {
    hCurr[gi(ix, 0)] = hPrev[gi(ix, 0)] = 0;
    hCurr[gi(ix, CLOTH_H - 1)] = hPrev[gi(ix, CLOTH_H - 1)] = 0;
  }
};

// 球在布上持续凹陷
const depressCloth = (wx: number, wz: number, strength: number, radius: number) => {
  const fx = (wx - CLOTH_X0) / (CLOTH_X1 - CLOTH_X0) * (CLOTH_W - 1);
  const fz = (wz - CLOTH_Z0) / (CLOTH_Z1 - CLOTH_Z0) * (CLOTH_H - 1);
  const cx = Math.round(fx), cz = Math.round(fz);
  for (let iz = cz - radius; iz <= cz + radius; iz++) {
    for (let ix = cx - radius; ix <= cx + radius; ix++) {
      if (ix < 1 || ix >= CLOTH_W - 1 || iz < 1 || iz >= CLOTH_H - 1) continue;
      const dist = Math.hypot(ix - fx, iz - fz);
      if (dist > radius) continue;
      const falloff = Math.exp(-dist * dist / (radius * radius * 0.5));
      const i = gi(ix, iz);
      const target = -strength * falloff;
      hCurr[i] += (target - hCurr[i]) * 0.24;
      hPrev[i] += (target - hPrev[i]) * 0.16;
    }
  }
};

const impactRipple = (wx: number, wz: number, strength: number) => {
  const fx = (wx - CLOTH_X0) / (CLOTH_X1 - CLOTH_X0) * (CLOTH_W - 1);
  const fz = (wz - CLOTH_Z0) / (CLOTH_Z1 - CLOTH_Z0) * (CLOTH_H - 1);
  const cx = Math.round(fx), cz = Math.round(fz);
  const radius = 8;
  for (let iz = cz - radius; iz <= cz + radius; iz++) {
    for (let ix = cx - radius; ix <= cx + radius; ix++) {
      if (ix < 1 || ix >= CLOTH_W - 1 || iz < 1 || iz >= CLOTH_H - 1) continue;
      const dist = Math.hypot(ix - fx, iz - fz);
      if (dist > radius) continue;
      const falloff = Math.exp(-dist * dist / (radius * radius * 0.34));
      const i = gi(ix, iz);
      hCurr[i] -= strength * falloff * 0.32;
    }
  }
};

// 布料高度 (双线性插值)
const getClothH = (wx: number, wz: number): number => {
  const fx = (wx - CLOTH_X0) / (CLOTH_X1 - CLOTH_X0) * (CLOTH_W - 1);
  const fz = (wz - CLOTH_Z0) / (CLOTH_Z1 - CLOTH_Z0) * (CLOTH_H - 1);
  const ix = Math.max(0, Math.min(CLOTH_W - 2, Math.floor(fx)));
  const iz = Math.max(0, Math.min(CLOTH_H - 2, Math.floor(fz)));
  const tx = fx - ix, tz = fz - iz;
  const h00 = hCurr[gi(ix, iz)], h10 = hCurr[gi(ix + 1, iz)];
  const h01 = hCurr[gi(ix, iz + 1)], h11 = hCurr[gi(ix + 1, iz + 1)];
  return PLANE_Y + (h00 + (h10 - h00) * tx) * (1 - tz) + (h01 + (h11 - h01) * tx) * tz;
};

// 布料梯度 (用于滚动)
const getClothGrad = (wx: number, wz: number): { gx: number; gz: number } => {
  const eps = 0.15;
  return {
    gx: (getClothH(wx + eps, wz) - getClothH(wx - eps, wz)) / (2 * eps),
    gz: (getClothH(wx, wz + eps) - getClothH(wx, wz - eps)) / (2 * eps),
  };
};

/* ======== 节点管理 ======== */
let allNodes: RNode[] = [];
// 一次性创建所有节点(不再每次toggle重建)
const buildAllNodes = (tree: KNode) => {
  allNodes = []; nodeMap = new Map(); expandedSet = new Set(); let idx = 0;
  const flatten = (tn: KNode, parentId: string | null, depth: number): RNode[] => {
    const res: RNode[] = [];
    const ang = depth === 0 ? 0 : (idx * 2.4 + (parentId ? parentId.charCodeAt(0) * 0.3 : 0));
    idx++;
    const rn: RNode = {
    id: tn.id, label: tn.label, cat: tn.cat, desc: tn.desc, color: tn.color,
    size: tn.size, parentId, depth, expanded: depth <= 1, childIds: (tn.children || []).map(c => c.id), rel: tn.rel,
    orbitR: depth === 0 ? 0 : 1.2 + depth * 0.65,
    orbitAng: ang, orbitH: depth === 0 ? 0 : (Math.sin(ang * 2) * 0.4),
    orbitSpd: depth === 0 ? 0 : 0.15 + Math.random() * 0.15,
    x: 0, y: 0, z: 0, sx: 0, sy: 0, sr: 0,
    vx: 0, vy: 0, vz: 0, tx: 0, ty: 0, tz: 0, mode: MODE.ORBIT,
  };
    res.push(rn);
    if (tn.children) for (const c of tn.children) res.push(...flatten(c, tn.id, depth + 1));
    return res;
  };
  allNodes = flatten(tree, null, 0);
  for (const n of allNodes) if (n.depth <= 1) expandedSet.add(n.id);
  nodeMap = new Map(allNodes.map(n => [n.id, n]));
};

const buildTreeFromFlat = (flat: any[]): KNode => {
  const map = new Map<string, KNode>();
  const roots: KNode[] = [];
  for (const n of flat) {
    const kn: KNode = { id: n.id, label: n.label, cat: n.category||'', desc: n.description||'', color: n.color||'#60a5fa', size: n.sizeVal||1, rel: [], children: [] };
    map.set(n.id, kn);
  }
  for (const n of flat) {
    const kn = map.get(n.id)!;
    if (n.parentId && map.has(n.parentId)) {
      map.get(n.parentId)!.children!.push(kn);
    } else if (!n.parentId) {
      roots.push(kn);
    }
  }
  return roots[0] || TREE;
};

let expandedSet = new Set<string>();
// 仅遍历标记可见性, 不创建任何对象 (解决展开卡顿)
const updateVisibility = () => {
  for (const n of allNodes) n.expanded = expandedSet.has(n.id);
  const markVisible = (rn: RNode, parentExpanded: boolean) => {
    rn.visible = parentExpanded;
    if (rn.expanded && parentExpanded) for (const cid of rn.childIds) { const ch = nodeMap.get(cid); if (ch) markVisible(ch, true); }
    else for (const cid of rn.childIds) { const ch = nodeMap.get(cid); if (ch) markVisible(ch, false); }
  };
  // 确保根节点和深度<=1的节点始终可见
  for (const n of allNodes) if (n.depth <= 1 && !expandedSet.has(n.id)) expandedSet.add(n.id);
  for (const n of allNodes) n.expanded = expandedSet.has(n.id);
  const root = allNodes.find(n => n.depth === 0); if (root) markVisible(root, true);
  for (const n of allNodes) {
    if (n.visible && !n._wasVisible && n.parentId) {
      const p = nodeMap.get(n.parentId);
      if (p) { const a = n.orbitAng + (Math.random()-0.5)*0.5; n.x = p.x + n.orbitR*0.25*Math.cos(a); n.y = p.y; n.z = p.z + n.orbitR*0.25*Math.sin(a); n.mode = p.mode; n.vx = n.vy = n.vz = 0; }
    }
    n._wasVisible = n.visible;
  }
  rnodes = allNodes.filter(n => n.visible);
  conns = [];
  for (const rn of rnodes) {
    for (const rid of rn.rel) { const bi = rnodes.findIndex(n => n.id === rid); if (bi !== -1) conns.push([rnodes.indexOf(rn), bi]); }
    if (rn.parentId) { const pi = rnodes.findIndex(n => n.id === rn.parentId); if (pi !== -1) conns.push([rnodes.indexOf(rn), pi]); }
  }
};
const toggleNode = (id: string) => {
  const rn = nodeMap.get(id); if (!rn || rn.childIds.length === 0) return;
  expandedSet.has(id) ? expandedSet.delete(id) : expandedSet.add(id);
  updateVisibility();
  // 保持聚焦模式, 更新信息
  if (focusNodeId === id) { const info = getNodeInfo(id); if (info) sel.value = info; }
  else { const info = getNodeInfo(id); if (info) sel.value = info; }
};
const getNodeInfo = (id: string) => { const rn = nodeMap.get(id); if (!rn) return null; return { id: rn.id, label: rn.label, category: rn.cat, desc: rn.desc, related: rn.rel, expanded: rn.expanded, childCount: rn.childIds.length }; };

/* ======== 物理计算 ======== */
const GRAVITY = 13, SPRING_K = 8, SPRING_DAMP = 4, RETURN_DELAY = 2.0;

const computeOrbitPos = (rn: RNode): { x: number; y: number; z: number } => {
  if (rn.depth === 0) return { x: 0, y: 0, z: 0 };
  const parent = rn.parentId ? nodeMap.get(rn.parentId) : null;
  const px = parent ? parent.x : 0, py = parent ? parent.y : 0, pz = parent ? parent.z : 0;
  const r = rn.orbitR * (parent ? Math.max(0.4, parent.size * 0.55) : 1.5);
  return { x: px + r * Math.cos(rn.orbitAng), y: py + rn.orbitH * 1.6, z: pz + r * Math.sin(rn.orbitAng) };
};

const updatePhysics = (dt: number) => {
  const canGravity = physicsOn.value; // 坠落开关
  // 关闭时所有非轨道节点立即返回轨道
  if (!canGravity && rnodes.some(n => n.mode !== MODE.ORBIT)) {
    for (const rn of rnodes) {
      if (rn.mode !== MODE.ORBIT) {
        rn.mode = MODE.RETURNING;
        const target = computeOrbitPos(rn);
        rn.tx = target.x; rn.ty = target.y; rn.tz = target.z;
      }
    }
    dragEver = false;
  }
  // 开启时所有轨道/返回中节点立即开始坠落
  if (canGravity) {
    for (const rn of rnodes) {
      if (rn.mode === MODE.ORBIT || rn.mode === MODE.RETURNING) {
        rn.mode = MODE.FALLING;
        dragEver = true;
      }
    }
  }
  // 开启时不会自动返回轨道（由开关控制）
  const shouldReturn = false;

  for (const rn of rnodes) {

    if (rn.mode === MODE.FALLING) {
      rn.vy -= GRAVITY * dt;
      rn.vx *= 0.98; rn.vz *= 0.98; rn.vy *= 0.995;
      rn.x += rn.vx * dt; rn.y += rn.vy * dt; rn.z += rn.vz * dt;
      const ch = getClothH(rn.x, rn.z);
      const minY = ch + rn.size * 0.1;
      if (rn.y <= minY) {
        const impactStr = Math.abs(rn.vy);
        rn.y = minY; rn.vy = 0; rn.vx *= 0.4; rn.vz *= 0.4;
        rn.mode = MODE.LANDED;
        impactRipple(rn.x, rn.z, Math.max(1.2, impactStr * 0.55));
      }
    } else if (rn.mode === MODE.LANDED) {
      // 持续压布
      depressCloth(rn.x, rn.z, rn.size * 0.18, 6);
      // 布料梯度 → 滚动
      const grad = getClothGrad(rn.x, rn.z);
      rn.vx += -grad.gx * GRAVITY * 0.6 * dt;
      rn.vz += -grad.gz * GRAVITY * 0.6 * dt;
      rn.vx *= 0.96; rn.vz *= 0.96; rn.vy = 0;
      rn.x += rn.vx * dt; rn.z += rn.vz * dt;
      // 始终贴在布面上
      const ch = getClothH(rn.x, rn.z);
      rn.y = ch + rn.size * 0.1;
      if (shouldReturn) {
        rn.mode = MODE.RETURNING;
        const target = computeOrbitPos(rn);
        rn.tx = target.x; rn.ty = target.y; rn.tz = target.z;
      }
    } else if (rn.mode === MODE.RETURNING) {
      const target = computeOrbitPos(rn);
      rn.tx = target.x; rn.ty = target.y; rn.tz = target.z;
      rn.vx += (rn.tx - rn.x) * SPRING_K * dt;
      rn.vy += (rn.ty - rn.y) * SPRING_K * dt + GRAVITY * 0.5 * dt;
      rn.vz += (rn.tz - rn.z) * SPRING_K * dt;
      rn.vx -= rn.vx * SPRING_DAMP * dt; rn.vy -= rn.vy * SPRING_DAMP * dt; rn.vz -= rn.vz * SPRING_DAMP * dt;
      rn.x += rn.vx * dt; rn.y += rn.vy * dt; rn.z += rn.vz * dt;
      const dist = Math.hypot(rn.x - rn.tx, rn.y - rn.ty, rn.z - rn.tz);
      if (dist < 0.3 && Math.abs(rn.vx) + Math.abs(rn.vy) + Math.abs(rn.vz) < 1.5) {
        rn.x = rn.tx; rn.y = rn.ty; rn.z = rn.tz; rn.vx = rn.vy = rn.vz = 0; rn.mode = MODE.ORBIT;
      }
    } else if (rn.mode === MODE.ORBIT) {
      rn.orbitAng += rn.orbitSpd * dt * getEffectiveRotSpeed();
      const target = computeOrbitPos(rn);
      rn.tx = target.x; rn.ty = target.y; rn.tz = target.z;
      rn.x += (rn.tx - rn.x) * 0.15; rn.y += (rn.ty - rn.y) * 0.15; rn.z += (rn.tz - rn.z) * 0.15;
    }
  }

  // 球间碰撞 (Landed模式)
  const landed = rnodes.filter(n => n.mode === MODE.LANDED);
  for (let i = 0; i < landed.length; i++) {
    for (let j = i + 1; j < landed.length; j++) {
      const a = landed[i], b = landed[j];
      const dx = a.x - b.x, dz = a.z - b.z, dist = Math.hypot(dx, dz);
      const minD = (a.size + b.size) * 0.1;
      if (dist < minD && dist > 0.001) {
        const nx = dx / dist, nz = dz / dist;
        const overlap = minD - dist;
        a.x += nx * overlap * 0.5; a.z += nz * overlap * 0.5;
        b.x -= nx * overlap * 0.5; b.z -= nz * overlap * 0.5;
        // 动量交换
        const relV = (a.vx - b.vx) * nx + (a.vz - b.vz) * nz;
        if (relV > 0) {
          const impulse = relV * 0.8;
          a.vx -= nx * impulse * 0.5; a.vz -= nz * impulse * 0.5;
          b.vx += nx * impulse * 0.5; b.vz += nz * impulse * 0.5;
        }
      }
    }
  }

  if (!drag && dragEver && rnodes.every(n => n.mode === MODE.ORBIT)) dragEver = false;
};

/* ======== 渲染 ======== */
const renderCloth = (dark: boolean) => {
  if (!c) return;
  const colors = getCanvasColors(dark);
  type Pt = { x: number; y: number; h: number };
  const rows: Pt[][] = [];
  for (let iz = 0; iz < CLOTH_H; iz++) {
    const row: Pt[] = [];
    for (let ix = 0; ix < CLOTH_W; ix++) {
      const p = proj(gx(ix), PLANE_Y + hCurr[gi(ix, iz)], gz(iz));
      row.push({ x: p.x, y: p.y, h: hCurr[gi(ix, iz)] });
    }
    rows.push(row);
  }

  // 单层网格线绘制 (布料网)
  for (let iz = 0; iz < CLOTH_H; iz++) {
    c.beginPath(); let started = false;
    for (let ix = 0; ix < CLOTH_W; ix++) {
      const pt = rows[iz][ix];
      if (pt.x > -200 && pt.x < W + 200 && pt.y > -200 && pt.y < H + 200) {
        if (!started) { c.moveTo(pt.x, pt.y); started = true; } else c.lineTo(pt.x, pt.y);
      } else started = false;
    }
    const maxH = Math.max(...rows[iz].map(p => Math.abs(p.h)));
    const isMajor = iz % 4 === 0 || iz === CLOTH_H - 1;
    const alpha = (dark ? (isMajor ? 0.26 : 0.11) : (isMajor ? 0.2 : 0.085)) + Math.min(0.28, maxH * 0.36);
    c.strokeStyle = colorWithAlpha(isMajor ? '#60a5fa' : colors.primary, alpha);
    c.lineWidth = isMajor ? 0.9 : 0.46; c.stroke();
  }
  for (let ix = 0; ix < CLOTH_W; ix++) {
    c.beginPath(); let started = false;
    for (let iz = 0; iz < CLOTH_H; iz++) {
      const pt = rows[iz][ix];
      if (pt.x > -200 && pt.x < W + 200 && pt.y > -200 && pt.y < H + 200) {
        if (!started) { c.moveTo(pt.x, pt.y); started = true; } else c.lineTo(pt.x, pt.y);
      } else started = false;
    }
    const maxH = Math.max(...rows.map(r => Math.abs(r[ix].h)));
    const isMajor = ix % 4 === 0 || ix === CLOTH_W - 1;
    const alpha = (dark ? (isMajor ? 0.26 : 0.11) : (isMajor ? 0.2 : 0.085)) + Math.min(0.28, maxH * 0.36);
    c.strokeStyle = colorWithAlpha(isMajor ? colors.primary : '#60a5fa', alpha);
    c.lineWidth = isMajor ? 0.9 : 0.46; c.stroke();
  }
  for (let iz = 0; iz < CLOTH_H; iz += 4) {
    for (let ix = 0; ix < CLOTH_W; ix += 4) {
      const pt = rows[iz][ix];
      if (pt.x < -80 || pt.x > W + 80 || pt.y < -80 || pt.y > H + 80) continue;
      const lift = Math.min(0.22, Math.abs(pt.h) * 0.28);
      c.beginPath();
      c.arc(pt.x, pt.y, 1.05 + lift * 4, 0, Math.PI * 2);
      c.fillStyle = colorWithAlpha((ix + iz) % 8 === 0 ? '#60a5fa' : colors.primary, (dark ? 0.34 : 0.24) + lift);
      c.fill();
    }
  }
};

const renderAll = () => {
  if (!c) return;
  c.clearRect(0, 0, W, H);
  const dark = theme.isDarkMode, cx = W / 2, cy = H / 2;
  const colors = getCanvasColors(dark);
  const base = c.createLinearGradient(0, 0, W, H);
  if (dark) {
    base.addColorStop(0, colors.bg);
    base.addColorStop(0.5, mixColor(colors.bg, colors.surface, 0.38));
    base.addColorStop(1, '#05070a');
  } else {
    base.addColorStop(0, mixColor(colors.bg, colors.surface, 0.42));
    base.addColorStop(0.58, colors.bg);
    base.addColorStop(1, mixColor(colors.bg, colors.primary, 0.06));
  }
  c.fillStyle = base;
  c.fillRect(0, 0, W, H);

  // 画布背景

  // 背景微粒
  bg.forEach(s => {
    const sx = ((s.x + t * s.sp * 0.01) % 1) * W, sy = ((s.y + Math.sin(t * 0.2 + s.x * 10) * 0.01) % 1) * H;
    c!.beginPath(); c!.arc(sx, sy, s.r, 0, Math.PI * 2);
    c!.fillStyle = alphaColor(colors.primaryRgb, s.a * (dark ? 0.45 : 0.28)); c!.fill();
  });
  const g0 = c.createRadialGradient(cx, cy, 0, cx, cy, W * 0.35);
  g0.addColorStop(0, alphaColor(colors.primaryRgb, dark ? 0.08 : 0.07));
  g0.addColorStop(0.5, alphaColor(colors.primaryRgb, dark ? 0.03 : 0.025));
  g0.addColorStop(1, 'transparent');
  c.fillStyle = g0; c.fillRect(0, 0, W, H);

  // 节点屏幕坐标
  for (const rn of rnodes) {
    const p = proj(rn.x, rn.y, rn.z);
    rn.sx = p.x; rn.sy = p.y; rn.sr = Math.max(2.2, rn.size * (rn.depth === 0 ? 11 : rn.depth === 1 ? 8.5 : 7.2) * p.s);
  }

  // 布料
  renderCloth(dark);

  // 轨道环
  if (!dragEver || drag) {
    [1.5, 2.5, 3.8].forEach(r => {
      const p = proj(r, 0, 0), radius = Math.abs(p.x - cx);
      if (radius < W) {
        c!.beginPath(); c!.arc(cx, cy, radius, 0, Math.PI * 2);
        c!.strokeStyle = alphaColor(colors.primaryRgb, dark ? 0.08 : 0.032);
        c!.lineWidth = 0.5; c!.setLineDash([4, 12]); c!.stroke(); c!.setLineDash([]);
      }
    });
  }

  // 氛围网格线
  const gridSpacing = 80;
  c!.strokeStyle = colors.grid;
  c!.lineWidth = 0.5;
  for (let gx = gridSpacing; gx < W; gx += gridSpacing) { c!.beginPath(); c!.moveTo(gx, 0); c!.lineTo(gx, H); c!.stroke(); }
  for (let gy = gridSpacing; gy < H; gy += gridSpacing) { c!.beginPath(); c!.moveTo(0, gy); c!.lineTo(W, gy); c!.stroke(); }

  const rootNode = rnodes.find(n => n.depth === 0);
  const originX = rootNode?.sx ?? cx;
  const originY = rootNode?.sy ?? cy;
  rnodes
    .filter(rn => rn.depth > 0 && rn.sr >= 2 && rn.sx > -80 && rn.sx < W + 80 && rn.sy > -80 && rn.sy < H + 80)
    .sort((a, b) => a.depth - b.depth)
    .forEach((rn, index) => {
      const tone = nodeVisualTone(rn, colors, dark);
      const cl = tone.core;
      const dx = rn.sx - originX;
      const dy = rn.sy - originY;
      const dist = Math.hypot(dx, dy);
      if (dist < 8) return;
      const alpha = (dark ? 0.34 : 0.25) / Math.max(1, rn.depth * 0.56);
      const grad = c!.createLinearGradient(originX, originY, rn.sx, rn.sy);
      grad.addColorStop(0, alphaColor(colors.primaryRgb, alpha * 1.35));
      grad.addColorStop(0.5, colorWithAlpha(tone.glow, alpha * 1.28));
      grad.addColorStop(1, colorWithAlpha(tone.rim, Math.min(0.82, alpha * 2.3)));
      c!.beginPath();
      c!.moveTo(originX, originY);
      c!.lineTo(rn.sx, rn.sy);
      c!.strokeStyle = grad;
      c!.lineWidth = rn.depth === 1 ? 1.45 : 0.96;
      c!.shadowColor = colorWithAlpha(cl, dark ? 0.28 : 0.18);
      c!.shadowBlur = rn.depth === 1 ? 9 : 5;
      c!.stroke();
      c!.shadowBlur = 0;

      const pulse = 0.5 + 0.5 * Math.sin(t * 2.2 + index * 0.7);
      c!.beginPath();
      c!.arc(rn.sx, rn.sy, Math.max(2.2, rn.sr * 0.42), 0, Math.PI * 2);
      c!.fillStyle = colorWithAlpha(tone.rim, dark ? 0.44 + pulse * 0.16 : 0.36 + pulse * 0.12);
      c!.shadowColor = colorWithAlpha(tone.glow, dark ? 0.52 : 0.36);
      c!.shadowBlur = 12;
      c!.fill();
      c!.shadowBlur = 0;
    });

  // 尾迹
  if (!trails.length) rnodes.forEach(() => trails.push([]));
  rnodes.forEach((rn, i) => {
    if (rn.mode !== MODE.ORBIT) return;
    trails[i].push({ x: rn.sx, y: rn.sy }); if (trails[i].length > 8) trails[i].shift();
    const trailColor = nodeVisualTone(rn, colors, dark).glow;
    trails[i].forEach((tr, j) => {
      const a = j / trails[i].length * 0.045;
      c!.beginPath(); c!.arc(tr.x, tr.y, rn.sr * 0.2, 0, Math.PI * 2);
      c!.fillStyle = colorWithAlpha(trailColor, a); c!.fill();
    });
  });

  // 脉冲
  const pp = (t * 0.6) % 3;
  if (pp < 1.5 && !drag && dark) { c.beginPath(); c.arc(cx, cy, pp * W * 0.3, 0, Math.PI * 2); c.strokeStyle = alphaColor(colors.primaryRgb, (1 - pp / 1.5) * 0.1); c.lineWidth = 2; c.stroke(); }

  // 流星
  if (false && Math.random() < 0.01 && shooters.length < 3) {
    const a = Math.random() * Math.PI * 2, d = W * 0.3 + Math.random() * W * 0.2;
    shooters.push({ x: cx + Math.cos(a) * d, y: cy + Math.sin(a) * d * 0.6, vx: Math.cos(a + 2.5) * 3, vy: Math.sin(a + 2.5) * 3, life: 0, max: 40 + Math.random() * 40 });
  }
  shooters = shooters.filter(ss => { ss.x += ss.vx; ss.y += ss.vy; ss.life++; if (ss.life > ss.max) return false; const a = 0.5 * (1 - ss.life / ss.max); c!.beginPath(); c!.moveTo(ss.x, ss.y); c!.lineTo(ss.x - ss.vx * 8, ss.y - ss.vy * 8); c!.strokeStyle = alphaColor(colors.primaryRgb, a); c!.lineWidth = 1.2; c!.stroke(); return true; });

  // 连线(选中星球外连线变淡)
  conns.forEach(([ai, bi]) => {
    const a = rnodes[ai], b = rnodes[bi]; if (!a || !b) return;
    const baseAlpha = dark ? 0.08 : 0.055;
    const alpha = (a.mode === b.mode ? baseAlpha : baseAlpha * 0.52) + 0.035 * Math.sin(t * 3 + ai + bi);
    const grad = c!.createLinearGradient(a.sx, a.sy, b.sx, b.sy);
    grad.addColorStop(0, colorWithAlpha(nodeVisualTone(a, colors, dark).glow, 0.36)); grad.addColorStop(1, colorWithAlpha(nodeVisualTone(b, colors, dark).glow, 0.32));
    c!.beginPath(); c!.moveTo(a.sx, a.sy); c!.lineTo(b.sx, b.sy);
    c!.strokeStyle = grad; c!.lineWidth = a.parentId === b.id || b.parentId === a.id ? 0.55 : 0.75;
    c!.globalAlpha = alpha; c!.stroke(); c!.globalAlpha = 1;
  });

  // 节点(选中星球高亮, 其他半透明, 子节点保持不透明)
  const selId = selectedNodeId;
  const selectedNode = selId ? nodeMap.get(selId) : null;
  const isChildOfSelected = (rn: RNode) => {
    if (!selectedNode) return false;
    if (rn.id === selId) return false;
    return selectedNode.childIds.includes(rn.id);
  };
  [...rnodes].sort((a, b) => b.sr - a.sr).forEach(rn => {
    const { sx: x, sy: y, sr: r0 } = rn; if (r0 < 2) return;
    const tone = nodeVisualTone(rn, colors, dark);
    const cl = tone.core;
    // Off-screen culling: skip nodes far outside viewport
    if (x < -100 || x > W + 100 || y < -100 || y > H + 100) return;
    const hoverScale = (hoveredNodeId === rn.id && !focusNodeId) ? 1.25 : 1;
    const focusScale = (focusNodeId === rn.id) ? 1.3 : 1;
    const r = r0 * hoverScale * focusScale;
    const auraRadius = r * (hoveredNodeId === rn.id ? 2.75 : 2.28);
    const h = c!.createRadialGradient(x - r * 0.16, y - r * 0.2, r * 0.12, x, y, auraRadius);
    h.addColorStop(0, colorWithAlpha(tone.rim, dark ? 0.42 : 0.34));
    h.addColorStop(0.38, colorWithAlpha(tone.glow, dark ? 0.18 : 0.14));
    h.addColorStop(1, 'transparent');
    c!.beginPath(); c!.arc(x, y, auraRadius, 0, Math.PI * 2); c!.fillStyle = h; c!.fill();
    // 选中态: 其他星球半透明, 子节点保持正常
    let nodeAlpha = 1;
    if (selId && rn.id !== selId && !isChildOfSelected(rn)) nodeAlpha = 0.25;
    c!.globalAlpha = nodeAlpha;
    const g = c!.createRadialGradient(x - r * 0.36, y - r * 0.38, r * 0.03, x + r * 0.22, y + r * 0.25, r * 1.04);
    g.addColorStop(0, dark ? 'rgba(255,255,255,0.98)' : 'rgba(255,255,255,0.96)');
    g.addColorStop(0.11, colorWithAlpha(tone.highlight, dark ? 0.84 : 0.72));
    g.addColorStop(0.32, colorWithAlpha(tone.rim, 0.96));
    g.addColorStop(0.64, colorWithAlpha(tone.core, dark ? 0.94 : 0.88));
    g.addColorStop(1, colorWithAlpha(tone.deep, dark ? 0.96 : 0.84));
    c!.shadowColor = colorWithAlpha(tone.glow, dark ? 0.48 : 0.34);
    c!.shadowBlur = rn.depth === 0 ? 24 : 14;
    c!.beginPath(); c!.arc(x, y, r, 0, Math.PI * 2); c!.fillStyle = g; c!.fill();
    const spec = c!.createRadialGradient(x - r * 0.34, y - r * 0.36, 0, x - r * 0.34, y - r * 0.36, r * 0.44);
    spec.addColorStop(0, dark ? 'rgba(255,255,255,0.72)' : 'rgba(255,255,255,0.62)');
    spec.addColorStop(0.36, colorWithAlpha(tone.highlight, dark ? 0.18 : 0.14));
    spec.addColorStop(1, 'transparent');
    c!.beginPath(); c!.arc(x - r * 0.22, y - r * 0.24, r * 0.52, 0, Math.PI * 2); c!.fillStyle = spec; c!.fill();
    c!.lineWidth = rn.depth === 0 ? 2 : 1.6;
    c!.strokeStyle = colorWithAlpha(tone.rim, dark ? 0.48 : 0.3);
    c!.stroke();
    c!.shadowBlur = 0;
    c!.globalAlpha = 1;
    if (rn.depth === 0) {
      c!.beginPath(); c!.arc(x, y, r * 1.28, 0, Math.PI * 2); c!.strokeStyle = colorWithAlpha(tone.glow, dark ? 0.34 : 0.2); c!.lineWidth = dark ? 1.8 : 1.2; c!.stroke();
    }
    if (rn.expanded && rn.childIds.length > 0) {
      c!.beginPath(); c!.arc(x, y, r * 1.44, 0, Math.PI * 2); c!.strokeStyle = colorWithAlpha(tone.glow, dark ? 0.22 : 0.14); c!.lineWidth = 0.8; c!.stroke();
    }
    if (r > (rn.depth === 0 ? 10 : rn.depth <= 1 ? 8 : 5)) {
      c!.font = `800 ${rn.depth === 0 ? 16 : Math.max(8, r * 0.55)}px "Microsoft YaHei","PingFang SC","Noto Sans SC",Inter,system-ui,sans-serif`;
      c!.fillStyle = colors.text; c!.textAlign = 'center'; c!.textBaseline = 'top';
      c!.fillText(rn.label, x, y + r + (rn.depth === 0 ? 12 : 5));
    }
  });

  // 状态提示
  if (physicsOn.value) {
    c.font = '700 11px "Microsoft YaHei","PingFang SC",Inter,system-ui,sans-serif'; c.fillStyle = colors.muted; c.textAlign = 'center';
    c.fillText(tt('kg3d.gravityMode'), cx, H - 60);
  }
};

/* ======== 事件 ======== */
const onDn = (e: MouseEvent) => { drag = true; dragEver = true; dx = e.clientX; dy = e.clientY; sry = try_; srx = trx; };
const onMv = (e: MouseEvent) => {
  mouseX = e.clientX; mouseY = e.clientY;
  if (!drag) { setHoveredNode(findHovered()); return; }
  try_ = sry + (e.clientX - dx) * 0.005; trx = Math.max(-1.0, Math.min(1.2, srx - (e.clientY - dy) * 0.005));
};
const onUp = () => { if (drag) { drag = false; dragEndTime = t; } };
const onLeave = () => { onUp(); setHoveredNode(null); };
const onWh = (e: WheelEvent) => { e.preventDefault(); tzm = Math.max(0.35, Math.min(3.5, tzm - e.deltaY * 0.0015)); };
const closePanel = () => { panelOpen.value = false; sel.value = null; articles.value = []; selectedNodeId = null; };
const fetchArticles = async (nodeId: string) => {
  articlesLoading.value = true; articles.value = [];
  try {
    const { forumApi } = await import('@/api');
    const res = await forumApi.listKnowledgeArticles();
    if (res.data.code === 200) articles.value = (res.data.data || []).filter((a:any) => a.nodeId === nodeId);
  } catch(e) { console.error(e); }
  articlesLoading.value = false;
};
const openArticle = (article: any) => {
  emit('openArticle', article);
};

const onSpeed = (e: Event) => { rotSpeed.value = parseFloat((e.target as HTMLInputElement).value); };
const findHovered = (): string|null => {
  if (!cv.value) return null;
  const r = cv.value.getBoundingClientRect();
  const mx = mouseX - r.left, my = mouseY - r.top;
  let best: RNode|null = null, bd = Infinity;
  for (const rn of rnodes) { const d = Math.hypot(mx - rn.sx, my - rn.sy); if (d < rn.sr + 18 && d < bd) { bd = d; best = rn; } }
  return best ? best.id : null;
};

const onClick = (e: MouseEvent) => {
  if (!cv.value) return;
  const r = cv.value.getBoundingClientRect(); const mx = e.clientX - r.left, my = e.clientY - r.top;
  let best: RNode | null = null, bd = Infinity;
  for (const rn of rnodes) { const d = Math.hypot(mx - rn.sx, my - rn.sy); if (d < rn.sr + 18 && d < bd) { bd = d; best = rn; } }
  if (best) {
    selectedNodeId = best.id;
    focusNodeId = best.id;
    sel.value = getNodeInfo(best.id);
    panelOpen.value = true;
    fetchArticles(best.id);
  } else {
    closePanel(); selectedNodeId = null; focusNodeId = null;
  }
};

const goBack = () => { focusNodeId = null; targetPanX = 0; targetPanY = 0; targetFocusZoom = 1; closePanel(); };

/* ======== 主循环 ======== */
let lastFrame = 0;
const FRAME_SKIP = 33; // ~30fps
const loop = (ts: number) => {
  // Skip frames when tab is hidden
  if (document.hidden) { rid = requestAnimationFrame(loop); return; }
  // Throttle to ~30fps
  if (ts - lastFrame < FRAME_SKIP) { rid = requestAnimationFrame(loop); return; }
  lastFrame = ts;

  const dt = Math.min(0.05, 0.016); t += dt;
  ry += (try_ - ry) * 0.06; rx += (trx - rx) * 0.06; zm += (tzm - zm) * 0.08;
  if (!drag) try_ += 0.0012 * getEffectiveRotSpeed() * (focusNodeId ? 0.05 : 1);
  if (focusNodeId) {
    targetFocusZoom = 2.2;
    // 平移+放大到星球, 不锁旋转
    const fn = nodeMap.get(focusNodeId);
    if (fn && fn.sr > 0) {
      targetPanX = W/2 - fn.sx;
      targetPanY = H/2 - fn.sy;
    }
  } else { targetFocusZoom = 1; targetPanX = 0; targetPanY = 0; }
  focusZoom += (targetFocusZoom - focusZoom) * 0.06;
  panX += (targetPanX - panX) * 0.08;
  panY += (targetPanY - panY) * 0.08;
  if (!drag && cv.value) setHoveredNode(findHovered());
  updateCloth(dt); updatePhysics(dt); renderAll();
  rid = requestAnimationFrame(loop); // already receives timestamp
};
const fit = () => {
  if (!box.value || !cv.value) return;
  const r = box.value.getBoundingClientRect();
  W = Math.max(1, r.width);
  H = Math.max(1, r.height);
  const ratio = Math.min(window.devicePixelRatio || 1, 2.5);
  cv.value.style.width = `${W}px`;
  cv.value.style.height = `${H}px`;
  cv.value.width = Math.round(W * ratio);
  cv.value.height = Math.round(H * ratio);
  c = cv.value.getContext('2d');
  c?.setTransform(ratio, 0, 0, ratio, 0, 0);
};

onMounted(async () => {
  let tree = TREE;
  try {
    const { forumApi } = await import('@/api');
    const res = await forumApi.listKnowledgeNodes();
    if (res.data.code === 200 && (res.data.data||[]).length > 0) tree = buildTreeFromFlat(res.data.data);
  } catch {}
  buildAllNodes(tree); updateVisibility(); for (let i = 0; i < 100; i++) bg.push({ x: Math.random(), y: Math.random(), r: 0.3 + Math.random() * 1.3, a: 0.1 + Math.random() * 0.5, sp: 0.02 + Math.random() * 0.1 });
  hCurr = new Float32Array(CLOTH_W * CLOTH_H); hPrev = new Float32Array(CLOTH_W * CLOTH_H);
  setTimeout(() => { fit(); loop(0); }, 100);
  window.addEventListener('resize', fit); window.addEventListener('mousemove', onMv); window.addEventListener('mouseup', onUp); window.addEventListener('mouseleave', onLeave);
});
onUnmounted(() => { cancelAnimationFrame(rid); window.removeEventListener('resize', fit); window.removeEventListener('mousemove', onMv); window.removeEventListener('mouseup', onUp); window.removeEventListener('mouseleave', onLeave); });
</script>

<style scoped>
.kg-root { width: 100%; height: 100%; position: relative; cursor: grab; }
.kg-root:active { cursor: grabbing; }
canvas { display: block; width: 100%; height: 100%; }
.kg-root {
  background:
    radial-gradient(circle at 30% 24%, rgba(var(--primary-rgb),0.11), transparent 34%),
    radial-gradient(circle at 72% 62%, color-mix(in srgb, var(--accent-glow) 10%, transparent), transparent 36%),
    linear-gradient(180deg, rgba(255,255,255,0.035), transparent 44%),
    rgba(var(--glass-bg-rgb), 0.12);
}
:global(.light .kg-root) {
  background:
    radial-gradient(circle at 30% 24%, rgba(var(--primary-rgb),0.09), transparent 34%),
    radial-gradient(circle at 72% 62%, color-mix(in srgb, var(--accent-glow) 9%, transparent), transparent 38%),
    linear-gradient(180deg, rgba(var(--primary-rgb),0.045), transparent 48%),
    rgba(var(--glass-bg-rgb), 0.04);
}
.kg-hint { position: absolute; bottom: 20px; left: 24px; font-size: 9px; font-weight: var(--font-weight-body); color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.08em; pointer-events: none; z-index: 20; max-width: 280px; line-height: 1.4; opacity: 0.42; }
:global(.light .kg-hint) { color: var(--text-muted); opacity: 0.52; }

.kg-controls { position: absolute; bottom: 20px; right: 20px; display: flex; flex-direction: column; gap: 5px; z-index: 20; }
.kg-control-row { display: flex; align-items: center; gap: 8px; background: rgba(var(--glass-bg-rgb),0.62); backdrop-filter: blur(12px); border-radius: 20px; padding: 6px 14px; border: 1px solid rgba(var(--primary-rgb),0.12); }
:global(.light .kg-control-row) { background: rgba(var(--glass-bg-rgb),0.58); border-color: rgba(var(--primary-rgb),0.18); box-shadow: 0 12px 34px rgba(29,56,46,0.06); }
.ctrl-label { font-size: 9px; font-weight: var(--font-weight-body); color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.06em; }
:global(.light .ctrl-label) { color: var(--text-muted); }
.ctrl-slider { width: 80px; height: 4px; -webkit-appearance: none; appearance: none; background: var(--surface-3); border-radius: 2px; outline: none; cursor: pointer; }
:global(.light .ctrl-slider) { background: rgba(var(--primary-rgb),0.16); }
.ctrl-slider::-webkit-slider-thumb { -webkit-appearance: none; width: 14px; height: 14px; border-radius: 50%; background: var(--primary-color); cursor: pointer; border: 2px solid var(--surface-solid); }
.ctrl-val { font-size: 10px; font-weight: var(--font-weight-body); color: var(--text-secondary); min-width: 32px; }
:global(.light .ctrl-val) { color: var(--text-secondary); }
.ctrl-toggle { padding: 6px 12px; border-radius: 20px; border: 1px solid rgba(var(--primary-rgb),0.12); background: rgba(var(--glass-bg-rgb),0.62); backdrop-filter: blur(12px); color: var(--text-secondary); font-size: 10px; font-weight: var(--font-weight-body); cursor: pointer; transition: all .2s; }
:global(.light .ctrl-toggle) { background: rgba(var(--glass-bg-rgb),0.58); border-color: rgba(var(--primary-rgb),0.18); color: var(--text-secondary); box-shadow: 0 12px 34px rgba(29,56,46,0.06); }
.ctrl-toggle.active { background: rgba(var(--primary-rgb),0.14); border-color: rgba(var(--primary-rgb),0.36); color: var(--primary-color); }

/* 右侧知识面板 */
.kg-article-panel { position: absolute; top: 14px; right: 14px; bottom: 14px; width: 390px; max-width: 90vw; background: linear-gradient(180deg, rgba(var(--glass-bg-rgb),0.82), rgba(var(--glass-bg-rgb),0.68)); backdrop-filter: blur(24px); border: 1px solid rgba(var(--primary-rgb),0.18); border-radius: 18px; z-index: 50; transform: translateX(calc(100% + 16px)); transition: transform 0.4s cubic-bezier(0.16,1,0.3,1); display: flex; flex-direction: column; overflow-y: auto; padding: 24px; box-shadow: 0 24px 70px rgba(0,0,0,0.22); }
.kg-article-panel.open { transform: translateX(0); }
:global(.light .kg-article-panel) { background: linear-gradient(180deg, rgba(var(--glass-bg-rgb),0.76), rgba(var(--glass-bg-rgb),0.58)); border-color: rgba(var(--primary-rgb),0.18); box-shadow: 0 20px 60px rgba(29,56,46,0.1); }
.panel-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; margin-bottom: 8px; }
.panel-header h3 { font-size: 18px; font-weight: var(--font-weight-label); color: var(--text-primary); margin: 6px 0 0; letter-spacing: 0; }
:global(.light .panel-header h3) { color: var(--text-primary); }
.panel-badge { font-size: 9px; font-weight: var(--font-weight-label); text-transform: uppercase; letter-spacing: .06em; background: rgba(var(--primary-rgb),0.1); color: var(--primary-color); padding: 2px 8px; border-radius: 20px; }
.panel-close { background: none; border: none; font-size: 22px; cursor: pointer; color: #94a3b8; padding: 0; line-height: 1; }
.panel-close:hover { color: #ef4444; }
.panel-desc { font-size: 12px; color: var(--text-secondary); margin: 0 0 12px; line-height: 1.5; }
:global(.light .panel-desc) { color: var(--text-secondary); }
.panel-expand { margin-bottom: 8px; }
.panel-expand button { width: 100%; padding: 9px 12px; border-radius: 10px; border: 1px solid rgba(var(--primary-rgb),0.22); cursor: pointer; font-size: 11px; font-weight: var(--font-weight-label); background: rgba(var(--primary-rgb),0.12); color: var(--primary-color); transition: all .2s; }
.panel-expand button:hover { opacity: 0.9; }
.panel-divider { height: 1px; background: rgba(255,255,255,0.08); margin: 4px 0 16px; }
:global(.light .panel-divider) { background: rgba(var(--primary-rgb),0.14); }
.panel-section-title { font-size: 11px; font-weight: var(--font-weight-body); color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.08em; margin: 0 0 12px; }
:global(.light .panel-section-title) { color: var(--text-muted); }
.panel-empty, .panel-loading { font-size: 12px; color: var(--text-muted); text-align: center; padding: 20px 0; }
:global(.light .panel-empty), :global(.light .panel-loading) { color: var(--text-muted); }
.focus-menu { padding: 0 0 20px; text-align: center; }
.focus-label { font-size: 16px; font-weight: var(--font-weight-title); color: #e8edf5; margin-bottom: 16px; }
:global(.light .focus-label) { color: var(--text-primary); }
.focus-btn { display: block; width: 100%; padding: 12px 16px; margin-bottom: 8px; border-radius: 12px; border: 1px solid rgba(255,255,255,0.1); background: rgba(255,255,255,0.05); color: #cbd5e1; font-size: 13px; font-weight: var(--font-weight-body); cursor: pointer; transition: all .2s; }
.focus-btn:hover { background: rgba(255,255,255,0.1); }
.focus-btn.primary { background: linear-gradient(135deg, #4dc9f0, #6366f1); color: #fff; border: none; }
.focus-btn.primary:hover { opacity: 0.9; }
.focus-back { display: block; width: max-content; padding: 7px 10px; margin: 0 0 14px; border-radius: 999px; border: 1px solid rgba(var(--primary-rgb),0.12); background: rgba(var(--glass-bg-rgb),0.32); color: var(--text-secondary); font-size: 11px; font-weight: var(--font-weight-body); cursor: pointer; transition: all .2s; }
:global(.light .focus-back) { color: var(--text-muted); background: rgba(var(--glass-bg-rgb),0.42); border-color: rgba(var(--primary-rgb),0.16); }
.focus-back:hover { color: rgba(255,255,255,0.6); }
:global(.light .focus-back:hover) { color: var(--text-secondary); background: rgba(var(--primary-rgb),0.08); }
.panel-articles { display: flex; flex-direction: column; gap: 4px; }
.article-item { padding: 13px 14px; border-radius: 12px; cursor: pointer; transition: all .25s; border: 1px solid rgba(var(--primary-rgb),0.08); margin-bottom: 6px; background: color-mix(in srgb, var(--surface-2) 70%, transparent); }
.article-item:hover { background: rgba(var(--primary-rgb),0.08); border-color: rgba(var(--primary-rgb),0.24); transform: translateX(-3px); }
:global(.light .article-item:hover) { background: rgba(var(--primary-rgb),0.08); }
.article-title { font-size: 13px; font-weight: var(--font-weight-body); color: var(--text-primary); margin-bottom: 4px; line-height: 1.4; }
:global(.light .article-title) { color: var(--text-primary); }
.article-meta { font-size: 10px; color: #64748b; font-weight: 600; }

/* 文章详情弹窗 */
.kg-article-detail { position: fixed; inset: 0; z-index: 200; background: rgba(0,0,0,0.7); backdrop-filter: blur(8px); display: flex; align-items: center; justify-content: center; padding: 40px; animation: fadeIn .25s ease; }
@keyframes fadeIn { from{opacity:0} to{opacity:1} }
.detail-card { background: linear-gradient(135deg, #1a1f35, #1e2640); border-radius: 24px; max-width: 820px; width: 100%; max-height: 85vh; overflow-y: auto; padding: 36px 40px; position: relative; border: 1px solid rgba(255,255,255,0.06); box-shadow: 0 30px 80px rgba(0,0,0,0.5); }
:global(.light .detail-card) { background: linear-gradient(135deg, rgba(var(--glass-bg-rgb),0.88), rgba(var(--glass-bg-rgb),0.74)); border-color: rgba(var(--primary-rgb),0.16); box-shadow: 0 30px 80px rgba(29,56,46,0.12); }
.detail-card h2 { font-size: 24px; font-weight: var(--font-weight-title); color: #e8edf5; margin: 0 30px 20px 0; letter-spacing: -0.01em; }
:global(.light .detail-card h2) { color: var(--text-primary); }
.detail-md { font-size: 15px; color: #cbd5e1; line-height: 1.9; }
:global(.light .detail-md) { color: var(--text-secondary); }
.detail-md :deep(h1) { font-size: 22px; font-weight: var(--font-weight-title); color: #e8edf5; margin: 24px 0 12px; }
.detail-md :deep(h2) { font-size: 19px; font-weight: var(--font-weight-body); color: #e8edf5; margin: 22px 0 10px; padding-bottom: 8px; border-bottom: 1px solid rgba(255,255,255,0.06); }
.detail-md :deep(h3) { font-size: 16px; font-weight: var(--font-weight-body); color: #cbd5e1; margin: 18px 0 8px; }
:global(.light .detail-md h1), :global(.light .detail-md h2) { color: var(--text-primary); border-color: rgba(var(--primary-rgb),0.14); }
:global(.light .detail-md h3) { color: var(--text-secondary); }
.detail-md :deep(pre) { background: rgba(0,0,0,0.25); border: 1px solid rgba(255,255,255,0.04); padding: 20px; border-radius: 14px; overflow-x: auto; margin: 12px 0; }
.detail-md :deep(code) { background: rgba(77,201,240,0.08); color: #4dc9f0; padding: 2px 8px; border-radius: 5px; font-size: 13px; font-family: 'JetBrains Mono', monospace; }
.detail-md :deep(pre code) { background: none; color: #cbd5e1; padding: 0; border-radius: 0; }
:global(.light .detail-md pre code) { color: var(--text-secondary); }
.detail-md :deep(strong) { color: #f1f5f9; }
:global(.light .detail-md strong) { color: var(--text-primary); }
.detail-md :deep(li) { margin: 4px 0 4px 16px; }
.detail-md :deep(blockquote) { border-left: 3px solid #4dc9f0; padding: 8px 16px; margin: 12px 0; background: rgba(77,201,240,0.04); border-radius: 0 8px 8px 0; }
.detail-md :deep(a) { color: #4dc9f0; text-decoration: underline; }
.detail-md :deep(table) { width: 100%; border-collapse: collapse; margin: 12px 0; }
.detail-md :deep(th), .detail-md :deep(td) { border: 1px solid rgba(255,255,255,0.06); padding: 10px 14px; text-align: left; font-size: 13px; }
.detail-md :deep(th) { background: rgba(255,255,255,0.03); font-weight: var(--font-weight-body); color: #e8edf5; }
:global(.light .detail-md th) { background: rgba(var(--primary-rgb),0.05); color: var(--text-primary); border-color: rgba(var(--primary-rgb),0.14); }
:global(.light .detail-md td) { border-color: rgba(var(--primary-rgb),0.14); }
.detail-md :deep(hr) { border: none; height: 1px; background: rgba(255,255,255,0.06); margin: 20px 0; }
</style>
