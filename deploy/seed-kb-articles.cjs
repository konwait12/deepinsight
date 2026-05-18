const m = require('mysql2/promise');
const fs = require('fs');
const path = require('path');

function loadEnv(file) {
  if (!fs.existsSync(file)) return;
  for (const rawLine of fs.readFileSync(file, 'utf8').split(/\r?\n/)) {
    const line = rawLine.trim();
    if (!line || line.startsWith('#') || !line.includes('=')) continue;
    const index = line.indexOf('=');
    const key = line.slice(0, index).trim();
    const value = line.slice(index + 1).trim().replace(/^['"]|['"]$/g, '');
    if (/^[A-Za-z_][A-Za-z0-9_]*$/.test(key) && process.env[key] === undefined) {
      process.env[key] = value;
    }
  }
}

loadEnv(path.resolve(__dirname, '..', '.env.local'));
loadEnv(path.resolve(__dirname, '.env'));

const dbConfig = {
  host: process.env.DB_HOST || 'localhost',
  port: Number(process.env.DB_PORT || 3306),
  user: process.env.DB_USER || 'root',
  password: process.env.DB_PASSWORD || '',
  database: process.env.DB_NAME || 'deepinsight',
  charset: 'utf8mb4',
};
const articles = {
d:'## DeepInsight 平台核心\n\nDeepInsight 是下一代深度学习全流程可视化分析平台。整合数据管理、模型训练、可视化分析和预测推理四大核心能力。\n\n### 核心特性\n\n- **3D知识图谱**: 交互式星系图,拖拽探索AI知识体系\n- **实时训练监控**: Loss/Accuracy曲线实时更新\n- **多维度分析**: 混淆矩阵、雷达图、PR曲线、ROC曲线\n- **AI助手**: 内置大语言模型对话\n- **模型管理**: 20+官方预置模型,支持自定义',
data:'## 数据管理\n\n数据管理模块提供数据集的全生命周期管理,从上传到预处理再到版本控制。\n\n### 数据预处理\n\n- **标准化**: Z-Score (x-μ)/σ\n- **归一化**: Min-Max缩放至[0,1]\n- **数据增强**: 翻转、旋转、裁剪、色彩抖动\n- **缺失值处理**: 删除、均值填充、KNN填充\n\n### 最佳实践\n\n训练/验证/测试集比例建议7:2:1或8:1:1',
train:'## 模型训练\n\n模型训练是深度学习的核心环节。\n\n### 训练流程\n\n1. 数据加载 → 2. 前向传播 → 3. 损失计算 → 4. 反向传播 → 5. 参数更新\n\n### 关键超参数\n\n- **学习率**: 控制更新步长,推荐0.001(Adam)\n- **批次大小**: 常用32/64/128,受显存限制\n- **训练轮次**: 配合早停策略,一般50-200\n- **优化器**: Adam通用,AdamW更好,SGD部分任务更优',
arch:'## 模型架构\n\n深度学习模型架构定义了网络层的组织结构。\n\n### 主要分类\n\n| 架构 | 代表模型 | 适用任务 |\n|------|---------|----------|\n| CNN | ResNet, VGG, EfficientNet | 图像 |\n| Transformer | ViT, BERT, GPT | 视觉/NLP |\n| GAN | StyleGAN, CycleGAN | 图像生成 |\n\n### 选择指南\n\n图像分类→ResNet-50, 移动端→EfficientNet, 检测→YOLOv8, NLP→BERT',
tech:'## 训练技巧\n\n### 优化器选择\n\n- **Adam**: 最通用,自适应学习率\n- **AdamW**: 解耦权重衰减,泛化更好\n- **SGD+Momentum**: 经典方案,部分任务优于Adam\n\n### 学习率调度\n\n- Cosine Annealing: SOTA常用\n- Warmup: Transformer必备\n- StepLR: 简单有效\n\n### 正则化\n\n- Dropout: 随机丢弃神经元\n- BatchNorm/LayerNorm: 归一化层\n- Weight Decay: L2正则化',
eval:'## 评估指标\n\n### 分类指标\n\n- **Accuracy**: 预测正确比例\n- **Precision**: 关注误报\n- **Recall**: 关注漏报\n- **F1**: P和R的调和平均\n- **AUC-ROC**: >0.8算好模型\n\n### 回归指标\n\n- **MSE**: 对大误差惩罚重\n- **MAE**: 对异常值鲁棒\n- **R²**: 越接近1越好',
deploy:'## 部署推理\n\n### 模型压缩\n\n- **量化**: FP32→INT8,模型减小4倍\n- **剪枝**: 移除不重要权重\n- **蒸馏**: 大模型教小模型\n\n### 推理引擎\n\n| 引擎 | 特点 |\n|------|------|\n| TensorRT | NVIDIA GPU优化 |\n| ONNX Runtime | 跨平台通用 |\n| OpenVINO | Intel CPU优化 |\n| TFLite | 移动端首选 |',
prep:'## 数据预处理\n\n### 标准化\n\nZ-Score: z = (x - μ) / σ, 均值为0标准差为1\n\n### 归一化\n\nMin-Max: x_norm = (x - x_min) / (x_max - x_min), 映射到[0,1]\n\n### 缺失值处理\n\n- 删除法: 简单但损失信息\n- 均值/中位数填充: 常用\n- KNN填充: 利用相似样本\n- MICE: 多重插补,适合复杂模式',
aug:'## 数据增强\n\n防止过拟合的第一道防线。\n\n### 图像增强\n\n- 随机翻转(水平/垂直)\n- 随机旋转(±15°)\n- 随机裁剪\n- 色彩抖动(亮度/对比度/饱和度)\n- 高斯噪声\n\n### 高级增强\n\n- MixUp: 两张图线性混合\n- CutMix: 区域替换\n- RandomErasing: 随机遮挡',
cnn:'## CNN 卷积神经网络\n\n### 核心思想\n\n利用局部相关性和平移不变性,大幅减少参数。\n\n### 三大组件\n\n1. 卷积层: 提取特征\n2. 池化层: 降采样\n3. 全连接层: 分类决策\n\n### 发展历程\n\nLeNet-5(1998)→AlexNet(2012)→VGG(2014)→ResNet(2015)→DenseNet(2017)',
rnn:'## RNN 循环神经网络\n\n专为序列数据设计。\n\n### LSTM 长短期记忆\n\n解决梯度消失: 遗忘门、输入门、输出门\n\n### GRU 门控循环单元\n\nLSTM简化版,参数更少\n\n### 应用\n\n文本分类、机器翻译、语音识别、时间序列预测',
transformer:'## Transformer\n\n2017年Google提出,取代RNN成为NLP主流。\n\n### 自注意力\n\nAttention(Q,K,V) = softmax(QK^T/√d_k)·V\n\n### 多头注意力\n\n多组QKV并行,关注不同方面\n\n### 位置编码\n\n正弦/余弦编码,告诉模型token位置',
gan:'## GAN 生成对抗网络\n\n### 核心思想\n\n生成器G生成假数据,判别器D判断真假,两者博弈。\n\n### 经典变体\n\n- DCGAN: CNN+GAN\n- StyleGAN: 控制生成风格\n- CycleGAN: 无配对风格迁移\n- WGAN: 更稳定的训练',
optim:'## 优化器\n\n### Adam\n\nm_t = β₁·m_{t-1} + (1-β₁)·g_t\nv_t = β₂·v_{t-1} + (1-β₂)·g_t²\n\n结合Momentum和RMSprop,自适应学习率。\n\n### SGD+Momentum\n\nv_t = β·v_{t-1} + (1-β)·g_t\n\n经典方案,泛化性能有时更好。\n\n### AdamW\n\n解耦权重衰减,泛化优于Adam。',
regular:'## 正则化\n\n### Dropout\n\n训练时随机丢弃神经元(p=0.2~0.5),防止共适应。\n\n### BatchNorm\n\n批归一化,加速收敛,轻微正则化。\n\n### LayerNorm\n\n不依赖batch size,Transformer标配。\n\n### Weight Decay\n\nL2正则化,惩罚大权重。',
lr:'## 学习率调度\n\n### 阶梯衰减(StepLR)\n\n每隔N epoch将lr乘以γ\n\n### 余弦退火\n\nlr沿余弦曲线衰减,平滑且SOTA常用。\n\n### Warmup\n\n前N epoch线性增长,Transformer必备。\n\n### ReduceLROnPlateau\n\n监控验证指标自适应降低。',
cls_metrics:'## 分类指标\n\n### 混淆矩阵\n\n| | 预测正 | 预测负 |\n|---|--------|--------|\n| 实际正 | TP | FN |\n| 实际负 | FP | TN |\n\n### 核心公式\n\n- Accuracy = (TP+TN)/(Total)\n- Precision = TP/(TP+FP)\n- Recall = TP/(TP+FN)\n- F1 = 2×P×R/(P+R)',
reg_metrics:'## 回归指标\n\n### MSE 均方误差\n\nMSE = (1/n)Σ(y_i-ŷ_i)²,对大误差惩罚重\n\n### MAE 平均绝对误差\n\nMAE = (1/n)Σ|y_i-ŷ_i|,对异常值鲁棒\n\n### R² 决定系数\n\nR² = 1 - SS_res/SS_tot,越接近1越好\n\n### 选择\n\n异常值多→MAE, 严惩大误差→MSE, 综合评价→R²',
matrix:'## 混淆矩阵\n\nN×N矩阵,N为类别数。行=实际,列=预测。\n\n### 分析\n\n- 对角线: 正确分类\n- 非对角线: 错误分类\n- 查找易混淆的类别对\n\n### 可视化\n\n热力图、3D柱状图、归一化显示',
quant:'## 模型量化\n\nFP32→INT8,模型减小4倍。\n\n### PTQ vs QAT\n\n- PTQ: 训练后量化,快速\n- QAT: 量化感知训练,精度更高\n\n### 硬件支持\n\nTensorRT(NVIDIA), OpenVINO(Intel), TFLite(移动端)',
prune:'## 模型剪枝\n\n### 非结构化剪枝\n\n移除单个权重,压缩率高但需稀疏硬件\n\n### 结构化剪枝\n\n移除通道/神经元,可直接加速\n\n### 知识蒸馏\n\n大模型(Teacher)教小模型(Student)',
edge:'## 边缘部署\n\n### 推理引擎\n\n| 引擎 | 平台 |\n|------|------|\n| TFLite | 移动端 |\n| ONNX Runtime | 跨平台 |\n| OpenVINO | Intel |\n| CoreML | Apple |\n\n### 优化\n\n量化、算子融合、内存复用、多线程',
resnet:'## ResNet 残差网络\n\n### 核心创新: 跳跃连接\n\ny = F(x) + x\n\n梯度可无损流过深层网络,解决退化问题。\n\n### Bottleneck\n\n1×1→3×3→1×1,减少计算量。\n\n### 变体\n\nResNet-50/101/152, ResNeXt, Wide ResNet',
vgg:'## VGG 网络\n\n### 设计哲学\n\n全部使用3×3小卷积核堆叠,简洁规整。\n\n### VGG-16/19\n\n- VGG-16: 138M参数\n- VGG-19: 143M参数\n\n### 特点\n\n结构简洁,特征迁移极好,但参数大推理慢',
efficient:'## EfficientNet\n\n### 复合缩放\n\n同时缩放深度、宽度和分辨率,找到最优平衡。\n\n### B0-B7系列\n\nB0(5.3M)→B4(19.3M)→B7(66M),效率极高。\n\n### 核心组件 MBConv\n\n深度可分离卷积 + SE注意力 + 跳跃连接',
vit:'## Vision Transformer\n\n### 图像即序列\n\n切分图像为Patch→线性嵌入→位置编码→Transformer\n\n### vs CNN\n\n全局建模强,但需要更多数据预训练。\n\n### 变体\n\nDeiT, Swin Transformer, MAE',
bert:'## BERT\n\nGoogle 2018, NLP里程碑。\n\n### MLM 掩码语言模型\n\n随机mask 15% token,让模型预测。\n\n### NSP 下一句预测\n\n判断两句是否连续。\n\n### Bert-Base\n\n12层,768维,12头,110M参数',
gpt:'## GPT 系列\n\n### GPT-2 (2019)\n\n15亿参数,自回归语言模型。\n\n### In-Context Learning\n\n无需微调,通过提示完成Zero/Few-Shot任务。\n\n### Scaling Law\n\n性能随规模幂律增长。\n\n### GPT演进\n\nGPT-1(117M)→GPT-2(1.5B)→GPT-3(175B)→GPT-4',
norm_methods:'## 标准化方法\n\n### Z-Score\n\nz = (x-μ)/σ, 零中心单位方差\n\n### Min-Max\n\nx_norm = (x-x_min)/(x_max-x_min), 映射[0,1]\n\n### Robust Scaler\n\n使用中位数和IQR,抗异常值\n\n### 选择\n\n高斯分布→Z-Score, 需特定范围→Min-Max, 异常值多→Robust',
missing:'## 缺失值处理\n\n### 删除法\n\n简单但损失信息\n\n### 填充法\n\n均值/中位数/众数/前向填充\n\n### 高级方法\n\nKNN填充, MICE多重插补, 模型预测\n\n### 建议\n\n缺失率>50%考虑删除, 时间序列优先前向填充',
adam:'## Adam 优化器\n\n### 核心公式\n\nm_t = β₁·m_{t-1} + (1-β₁)·g_t\nv_t = β₂·v_{t-1} + (1-β₂)·g_t²\nθ_t = θ_{t-1} - α·m̂_t/(√v̂_t+ε)\n\n### 推荐参数\n\nα=0.001, β₁=0.9, β₂=0.999, ε=1e-8\n\n### 特点\n\n自适应学习率,收敛快,最通用优化器',
sgd:'## SGD + Momentum\n\n### 经典SGD\n\nθ = θ - α·∇L(θ)\n\n### Momentum\n\nv_t = β·v_{t-1} + (1-β)·g_t\nθ = θ - α·v_t\n\n累积历史梯度加速,阻尼震荡。\n\n### Nesterov\n\n\"向前看\"的动量,先走一步再算梯度。\n\n### 适用场景\n\nCV任务,需要更好泛化,配合学习率调度',
dropout:'## Dropout\n\n### 原理\n\n训练时以概率p随机丢弃神经元,相当于集成多个子网络。\n\n### 推荐值\n\n输入层p=0.2, 隐藏层p=0.5, 输出层不用\n\n### 注意\n\n训练开测试关, CNN中用于FC层, 现代网络使用率在降低',
batchnorm:'## Batch Normalization\n\n### 原理\n\nμ_B, σ²_B → normalize → γ·x̂+β\n\n### 好处\n\n加速收敛,缓解梯度消失,轻微正则化\n\n### 局限\n\n依赖batch size, 训练推理行为不同, Transformer中已被LayerNorm替代',
};

(async () => {
  const c = await m.createConnection(dbConfig);
  const [nodes] = await c.execute('SELECT id,label FROM knowledge_nodes ORDER BY sort_order');
  let count = 0;
  for (const n of nodes) {
    const content = articles[n.id];
    if (!content) continue;
    const [existing] = await c.execute('SELECT id FROM knowledge_articles WHERE node_id=?', [n.id]);
    if (existing.length > 0) continue;
    await c.execute('INSERT INTO knowledge_articles (node_id,title,content,is_pinned) VALUES (?,?,?,?)',
      [n.id, n.label, content, false]);
    count++;
  }
  console.log('Created ' + count + ' knowledge articles');
  const [total] = await c.execute('SELECT COUNT(*) c FROM knowledge_articles');
  console.log('Total knowledge articles: ' + total[0].c);
  await c.end();
})();
