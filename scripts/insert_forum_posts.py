import os
import mysql.connector
conn = mysql.connector.connect(
    host=os.getenv('DB_HOST', 'localhost'),
    port=int(os.getenv('DB_PORT', '3306')),
    user=os.getenv('DB_USER', 'root'),
    password=os.getenv('DB_PASSWORD', ''),
    database=os.getenv('DB_NAME', 'deepinsight'),
)
cur = conn.cursor()

posts = [
  {
    'title': '深度学习入门：从感知机到GPT的进化之路',
    'cover': 'https://images.unsplash.com/photo-1677442136019-21780ecad995?w=800&h=400&fit=crop',
    'content': '''![cover](https://images.unsplash.com/photo-1677442136019-21780ecad995?w=800&h=400&fit=crop)

## 第一章：一切的起点——感知机

1958年，Frank Rosenblatt 发明了感知机（Perceptron），这是现代深度学习的始祖。感知机本质上是一个线性分类器：**y = sign(w*x + b)**

它只能解决线性可分问题。1969年，Minsky 和 Papert 证明了单层感知机甚至无法解决 XOR 问题，直接导致了第一次 AI 寒冬。

## 第二章：多层网络与反向传播

1986年，Rumelhart、Hinton 和 Williams 发表了反向传播算法，让训练多层网络成为可能。核心思想是用链式法则从输出层向输入层逐层计算梯度。

多层感知机 + 反向传播 = 可以拟合任意函数的万能近似器。

## 第三章：深度学习的崛起

2012年是分水岭。AlexNet 在 ImageNet 上将错误率从 26.2% 降到 15.4%，碾压所有传统方法。三大要素聚齐：

1. **大数据** —— ImageNet 百万级标注图片
2. **大算力** —— GPU 并行计算
3. **好算法** —— ReLU、Dropout、数据增强

## 第四章：CNN 时代（2012-2017）

| 年份 | 模型 | 核心创新 | Top-5 错误率 |
|------|------|---------|-------------|
| 2012 | AlexNet | ReLU + Dropout + GPU | 15.4% |
| 2014 | VGG-19 | 全3x3小卷积核 | 7.3% |
| 2014 | GoogLeNet | Inception多分支 | 6.7% |
| 2015 | ResNet-152 | 跳跃连接 | 3.6% |
| 2017 | SENet | 通道注意力 | 2.3% |

ResNet 的跳跃连接是最优雅的设计：**y = F(x) + x**。梯度可以直接流过恒等映射，152层网络的训练成为可能。

## 第五章：Transformer 革命（2017-至今）

2017年，Google 发表了《Attention Is All You Need》。自注意力机制让每个词直接看到所有其他词，完全并行。

- **BERT**（2018）：双向上下文理解，11项NLP任务SOTA
- **GPT-3**（2020）：1750亿参数，零样本学习
- **ViT**（2020）：Transformer 进入 CV 领域
- **GPT-4**（2023）：多模态理解与生成

## 第六章：你该从哪里开始？

1. 理解**线性回归**和**逻辑回归**（1周）
2. 手写**三层全连接网络**（用NumPy，不用框架）（2周）
3. 用 PyTorch 跑通 **MNIST**（1周）
4. 尝试 **CIFAR-10**，从 LeNet 到 ResNet（2周）
5. 加入 **Kaggle 竞赛**实战（持续学习）

> 深度学习的精髓不在于背诵公式，而在于动手实验。每当你困惑时，跑一个实验比看十篇论文更有用。'''
  },
  {
    'title': '为什么Transformer取代了RNN？序列建模的范式革命',
    'cover': 'https://images.unsplash.com/photo-1620712943543-bcc4688e7485?w=800&h=400&fit=crop',
    'content': '''![cover](https://images.unsplash.com/photo-1620712943543-bcc4688e7485?w=800&h=400&fit=crop)

## RNN 的黄金时代

循环神经网络曾是序列建模的王者。LSTM 用三个门控机制解决梯度消失：遗忘门决定丢弃多少旧记忆，输入门决定写入多少新信息，输出门决定输出多少当前状态。

但 RNN 有一个致命缺陷：**必须串行计算**。第 t 步依赖第 t-1 步的隐藏状态，无法并行。当序列长度超过 100 时，即使 LSTM 也难以捕捉长程依赖。

## Transformer 的降维打击

Transformer 的核心思想如此简单却强大：**让每个词直接看到所有其他词**。自注意力机制的并行计算，让训练时间从数周缩短到数天。

## 关键技术对比

| 特性 | RNN/LSTM | Transformer |
|------|---------|------------|
| 并行计算 | 否，必须串行 | 是，完全并行 |
| 长程依赖 | LSTM缓解但不完美 | 直接关注任意位置 |
| 训练速度 | 慢 | 快（GPU友好） |
| 可解释性 | 中等 | 高（注意力权重可视化） |
| 位置信息 | 天然内置 | 需要位置编码 |

## 自注意力的代价

自注意力的计算复杂度是 **O(n^2)**，n 是序列长度。短序列（n<512）Transformer 完胜，长序列（n>2048）需要 Longformer、BigBird 等稀疏注意力变体。

## CV 领域的革命

2020年 ViT 证明：把图像切成 16x16 的 patch 当 token 输入 Transformer，效果就能超越 CNN。Swin Transformer 用窗口注意力进一步降低计算量。

## 未来趋势

- 状态空间模型（Mamba 等）试图实现 O(n) 推理
- 混合架构：CNN 的局部性 + Transformer 的全局性
- FlashAttention 已在硬件层面将速度提升 2-4 倍

> Transformer 不是终点，但它是过去十年深度学习最重要的架构创新。理解自注意力，你就能理解现代 AI 的核心。'''
  },
  {
    'title': '训练深度学习模型的10个实战技巧',
    'cover': 'https://images.unsplash.com/photo-1555949963-ff9fe0c870eb?w=800&h=400&fit=crop',
    'content': '''![cover](https://images.unsplash.com/photo-1555949963-ff9fe0c870eb?w=800&h=400&fit=crop)

## 前言

经过无数次的 debug 和熬夜，我总结了 10 个最实用的技巧。这些不是在论文里能读到的——它们是踩坑踩出来的。

## 1. 先从过拟合一个小数据集开始

取 100-500 个样本，关闭所有正则化，训练到完美过拟合。如果训练损失降不到接近零，说明模型容量不够或学习率有问题。如果轻松过拟合，模型架构OK，可以加正则化上全量数据。

## 2. 学习率是最重要的超参数

学习率的影响力远超其他所有超参数之和。用 LR Finder 从很小的 lr 开始，每个 batch 增大，观察损失曲线。选择损失下降最快区间的 lr，实际训练时用这个 lr 的 1/10。

## 3. 梯度裁剪是必备的

```python
torch.nn.utils.clip_grad_norm_(model.parameters(), max_norm=1.0)
```

尤其在训练 RNN 和 Transformer 时，梯度裁剪能防止一次坏 batch 毁掉整个训练。

## 4. 监控梯度范数

持续增大→梯度爆炸需要裁剪；持续减小到零→梯度消失检查激活函数；稳定在合理范围→健康。

## 5. EMA 指数移动平均

保存模型参数的指数移动平均用于推理，几乎零成本提升模型表现。ema_weight = 0.999 * ema_weight + 0.001 * current_weight

## 6. 混合精度训练

用 FP16 训练，速度翻倍，显存减半。PyTorch 中只需 GradScaler + autocast 几行代码。

## 7. 数据增强是免费的午餐

RandAugment 和 TrivialAugment 是近年 SOTA 策略——只需指定增强强度，算法自动选择增强组合。

## 8. 保存所有实验记录

用 wandb 或 tensorboard 记录每一个实验。至少记录超参数配置、损失曲线、最终指标和简短备注。

## 9. 验证集的正确用法

验证集不是用来看的——是用来做决策的。每当你根据验证集调整策略，验证集就已经泄漏了。真正的最终评估用完全未触碰过的测试集。

## 10. 状态checkpoint要存完整

保存 optimizer state、scheduler state、random state——这样你能从精确的中断点恢复训练，而不仅仅是加载模型权重。

> 最好的技巧是动手去做。读完这篇文章，打开 Colab，跑一个模型。明天的你会感谢今天开始动手的你。'''
  },
  {
    'title': 'ImageNet十年：图像分类的进化与启示',
    'cover': 'https://images.unsplash.com/photo-1561736778-92e52a7769ef?w=800&h=400&fit=crop',
    'content': '''![cover](https://images.unsplash.com/photo-1561736778-92e52a7769ef?w=800&h=400&fit=crop)

## 2012：AlexNet——一切的开端

AlexNet 将 ImageNet top-5 错误率从 26.2% 降到 15.4%，降幅超过 10 个百分点。核心贡献：ReLU 激活函数（比 tanh 快 6 倍）、Dropout 正则化、双 GPU 训练。

## 2014：更深 vs 更宽

- **VGG**：更深的网络（19层），全部用 3x3 小卷积核。144M 参数，证明了深度本身就是有效的正则化
- **GoogLeNet**：更宽的网络，Inception 模块并行多个卷积分支。仅 5M 参数

## 2015：ResNet——转折点

何恺明团队解决了困扰学界多年的问题：为什么更深的网络反而效果更差？答案是退化问题而非梯度消失。ResNet 的跳跃连接 y = F(x) + x 让 152 层网络稳定训练，ImageNet top-5 错误率降到 3.6%，**首次超越人类水平**。

## 2016-2017：注意力的萌芽

SENet 引入通道注意力——让网络学会关注重要的特征通道。重要的哲学转变：网络不应该对所有特征一视同仁。这个思想后来在 Transformer 中得到了最充分的发挥。

## 2019：EfficientNet——设计科学化

用 NAS 系统性地探索深度、宽度、分辨率的最优关系。在固定计算预算下，三个维度按比例同时缩放。EfficientNet-B7 以 84.4% top-1 准确率刷新记录。

## 2020-至今：Transformer 全面入侵

ViT 证明纯 Transformer 在图像分类上可超越 CNN。真正的胜利在于：同一架构统一处理图像、文本、语音、视频——这是 CNN 做不到的。

## 十年启示

1. **简单的想法最持久**——跳跃连接、自注意力都是简单的数学操作
2. **计算力是创新的催化剂**——没有 GPU 就没有深度学习
3. **开源和竞赛推动进步**——ImageNet、COCO 是集体智慧的汇聚点

> 下一个十年会怎样？也许我们回看 Transformer 就像现在回看 AlexNet 一样——它开启了一个时代，但远不是终点。'''
  },
  {
    'title': '深度学习硬件选择：GPU、TPU与本地训练方案',
    'cover': 'https://images.unsplash.com/photo-1558494949-ef010cbdcc31?w=800&h=400&fit=crop',
    'content': '''![cover](https://images.unsplash.com/photo-1558494949-ef010cbdcc31?w=800&h=400&fit=crop)

## 为什么 GPU 是深度学习的标配？

CPU 是跑车（单核快但核数少），GPU 是卡车（单个核慢但几千个核）。矩阵乘法——深度学习 90% 的计算——恰好可以完美拆分到几千个核上并行。

## GPU 关键参数

- **CUDA 核心数**：越多越好（RTX 4090 有 16384 个）
- **显存 VRAM**：决定能训练的模型大小
- **Tensor Core**：专门加速矩阵运算的硬件单元
- **显存带宽**：数据从显存到计算核心的速度

## 不同场景 GPU 推荐

| 场景 | 推荐 GPU | 显存 | 适合 |
|------|---------|------|------|
| 入门学习 | RTX 4060 Ti 16GB | 16GB | 小型模型、教程 |
| 个人主力 | RTX 4090 24GB | 24GB | 7B模型微调 |
| 专业工作站 | RTX 6000 Ada | 48GB | 大模型训练 |
| 云上训练 | A100 80GB | 80GB | 按需使用 |
| 苹果生态 | M2 Ultra 192GB | 192GB统一 | 大模型推理 |

## 云端 vs 本地

**云端优势**：零维护成本，按需付费，可随时升级。Colab 免费版就能用 T4（16GB）。

**本地优势**：一次性投入，可 24/7 跑实验，数据隐私。

**建议策略**：入门用 Colab 免费版；确定方向后如果每天训练超2小时，买 RTX 4090 比租云 GPU 半年更划算。

## 显存估算公式

显存 (GB) = 参数量 x 4 bytes x 4 (优化器状态) x 2 (激活值) / 1024^3

例如 7B 参数的模型约需 208 GB。实际用 LoRA/QLoRA 等微调技术，7B 模型在 24GB 显存上就可以微调。

## 其他硬件

- **Apple Silicon**：统一内存可跑大模型推理，训练不如 NVIDIA
- **TPU**：Google 自研，Colab 免费提供
- **NPU**：手机上的 AI 推理芯片，只推理不训练

> 硬件是工具，不是目的。一块 RTX 4060 加扎实的基础知识，胜过 A100 加空泛的调参。'''
  },
]

for p in posts:
    cur.execute(
        'INSERT INTO forum_posts (title, content, cover_url, user_id, is_pinned, is_official) VALUES (%s,%s,%s,%s,%s,%s)',
        (p['title'], p['content'], p['cover'], 1, 1, 1)
    )
conn.commit()
cur.execute('SELECT COUNT(*) FROM forum_posts')
print(f'Total forum posts: {cur.fetchone()[0]}')
conn.close()
