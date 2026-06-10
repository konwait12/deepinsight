const modelArticles = [
  {
    numericId: 1,
    id: 'bsarec-job',
    name: 'BSARec Job',
    displayName: 'BSARec 岗位推荐',
    architecture: 'BSARec',
    family: 'BSARec 家族',
    status: '服务离线',
    cover: 'https://images.unsplash.com/photo-1497366754035-f200968a6e72?w=1400&h=800&fit=crop',
    summary: '面向岗位浏览与投递序列的推荐模型，拥有 Job.txt、BSARec_Job.pt 和评估日志，是当前唯一登记了后端推荐代理的模型。',
    data: {
      Dataset: 'Job',
      Users: '15,000',
      Items: '1,152',
      Interactions: '409,466',
      Records: '15,000',
      'Max sequence length': '50',
      Size: '1.7 MB',
    },
    metrics: {
      'HR@5': '0.0150',
      'NDCG@5': '0.0088',
      'HR@10': '0.0287',
      'NDCG@10': '0.0132',
      'HR@20': '0.0564',
      'NDCG@20': '0.0201',
    },
    training: {
      Weight: '存在',
      Log: '存在',
      Entrypoint: 'bsarec_api/app.py',
      Config: 'bsarec_env.yaml',
      Service: '/api/v1/prediction/recommend',
    },
    params: {
      'Embedding Dim': '64',
      'Hidden Dim': '64',
      'Transformer Blocks': '2',
      'Attention Heads': '2',
      Alpha: '0.7',
      c: '5',
    },
    visual: '重点展示岗位序列长度、Top-K 命中率、NDCG 排序质量、服务健康度和推荐耗时。因为权重、数据和日志都存在，但 Flask /health 当前不可达，页面必须把它标为“服务离线”，不能伪造成在线。',
    compare: '适合与 BSARec 序列推荐对比：前者是岗位场景并登记后端代理，后者是多数据集离线资产。两者都属于 BSARec 架构，但接入成熟度不同。',
    aiRule: 'AI 回答时应说明：这是唯一可通过后端代理尝试推荐的模型；如果服务离线，原因是本地 Flask 推荐服务未响应，而不是数据或权重缺失。',
  },
  {
    numericId: 2,
    id: 'bsarec',
    name: 'BSARec',
    displayName: 'BSARec 序列推荐',
    architecture: 'BSARec',
    family: 'BSARec 家族',
    status: '权重+日志',
    cover: 'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?w=1400&h=800&fit=crop',
    summary: 'BSARec 多数据集序列推荐资产，包含 Beauty、LastFM、ML-1M、Sports_and_Outdoors、Toys_and_Games、Yelp 六份真实序列数据，其中 Beauty 和 LastFM 已有权重与评估日志。',
    data: {
      Dataset: 'Beauty, LastFM, ML-1M, Sports_and_Outdoors, Toys_and_Games, Yelp',
      Users: '114,934',
      Items: '69,477',
      Interactions: '2,030,952',
      Records: '114,934',
      'Max sequence length': '2,277',
      Size: '9.7 MB',
    },
    metrics: {
      'HR@5': '0.0736',
      'NDCG@5': '0.0523',
      'HR@10': '0.1008',
      'NDCG@10': '0.0611',
      'HR@20': '0.1373',
      'NDCG@20': '0.0703',
    },
    training: {
      Weights: '2 个',
      Logs: '2 个',
      Entrypoint: 'src/main.py',
      Service: '未注册后端推理服务',
    },
    params: {
      'Embedding Dim': '64',
      'Hidden Dim': '64',
      'Transformer Blocks': '2',
      'Attention Heads': '2',
      'Max sequence length': '50',
    },
    visual: '重点看 HR@10、NDCG@10、HR@20 与跨数据集规模。Beauty/LastFM 有日志，其他数据集主要作为真实数据资产展示，不能把未评估数据集写成已有指标。',
    compare: '适合做推荐基线：和 BERT4Rec、SASRec、TiSASRec 比结构差异，和 FMLP-Rec 比日志完整度，和 BSARec Job 比服务接入成熟度。',
    aiRule: 'AI 应把它解释成离线资产完整的推荐模型，而不是可直接在线推理的服务。',
  },
  {
    numericId: 3,
    id: 'bert4rec',
    name: 'BERT4Rec',
    displayName: 'BERT4Rec 双向序列推荐',
    architecture: 'BERT4Rec',
    family: 'Transformer 序列推荐',
    status: '代码+数据',
    cover: 'https://images.unsplash.com/photo-1551288049-bebda4e38f71?w=1400&h=800&fit=crop',
    summary: '基于双向 Transformer 编码的序列推荐模型，拥有 Beauty、ML-1M、Steam 真实交互文本和 ML-20M 数据归档。',
    data: {
      Dataset: 'beauty, ml-1m, steam, ml-20m',
      Users: '327,694',
      Items: '71,002',
      Interactions: '4,842,458',
      Records: '4,842,458',
      Size: '103.9 MB',
    },
    metrics: {
      Status: '暂无真实评估日志',
      Reason: '当前目录没有可读取的评估日志',
    },
    training: {
      Weight: '缺失',
      Log: '缺失',
      Entrypoint: 'run.py',
      Config: 'bert_config_ml-1m_64.json',
      Service: '未注册后端推理服务',
    },
    params: {
      'Hidden Size': '64',
      'Transformer Blocks': '2',
      'Attention Heads': '2',
      'Max position embeddings': '200',
      Dropout: '0.2',
    },
    visual: '适合展示数据规模、序列 token 化、双向上下文建模和配置参数。没有日志时，可视化只能展示资产就绪度与数据规模，不能生成 HR/NDCG 数值。',
    compare: '可与 SASRec 对比：BERT4Rec 强调双向上下文，SASRec 强调自回归式下一物品推荐。两者都有真实数据，但当前都没有站内评估日志。',
    aiRule: 'AI 需要明确“代码+数据已接入，指标未登记”，并引导用户先补训练或评估日志。',
  },
  {
    numericId: 4,
    id: 'duorec',
    name: 'DuoRec',
    displayName: 'DuoRec 对比序列推荐',
    architecture: 'DuoRec',
    family: '对比学习推荐',
    status: '代码+数据',
    cover: 'https://images.unsplash.com/photo-1552664730-d307ca884978?w=1400&h=800&fit=crop',
    summary: 'DuoRec 以序列增强和对比学习提升用户表示，当前接入真实 ml-100k atomic 数据，便于按模型配置运行。',
    data: {
      Dataset: 'ml-100k',
      Users: '943',
      Items: '1,682',
      Interactions: '100,000',
      Records: '100,000',
      Size: '2.6 MB',
    },
    metrics: {
      Status: '暂无真实评估日志',
      Reason: '当前目录没有可读取的评估日志',
    },
    training: {
      Weight: '缺失',
      Log: '缺失',
      Entrypoint: 'run_seq.py',
      Config: 'seq.yaml',
      Service: '未注册后端推理服务',
    },
    params: {
      Contrast: 'us_x',
      Similarity: 'dot',
      'Batch Size': '256',
      'Learning Rate': '0.001',
      'Max sequence length': '50',
    },
    visual: '重点展示 ml-100k 数据规模、对比学习配置、数据就绪度和缺失日志状态。适合和 FEARec 观察同一数据集下不同增强策略的准备情况。',
    compare: '与 FEARec 同用 ml-100k，可做配置和接入成熟度对比；与 RecBole 比，DuoRec 是具体模型，RecBole 是框架。',
    aiRule: 'AI 不能给 DuoRec 伪造 HR/NDCG，只能解释它的代码、数据和参数配置。',
  },
  {
    numericId: 5,
    id: 'fearec',
    name: 'FEARec',
    displayName: 'FEARec 频域增强序列推荐',
    architecture: 'FEARec',
    family: '频域增强推荐',
    status: '代码+数据',
    cover: 'https://images.unsplash.com/photo-1518186285589-2f7649de83e0?w=1400&h=800&fit=crop',
    summary: 'FEARec 在序列推荐中引入频域增强思想，当前接入真实 ml-100k 数据和运行配置。',
    data: {
      Dataset: 'ml-100k',
      Users: '943',
      Items: '1,682',
      Interactions: '100,000',
      Records: '100,000',
      Size: '2.6 MB',
    },
    metrics: {
      Status: '暂无真实评估日志',
      Reason: '当前目录没有可读取的评估日志',
    },
    training: {
      Weight: '缺失',
      Log: '缺失',
      Entrypoint: 'run_seq.py',
      Config: 'seq.yaml',
      Service: '未注册后端推理服务',
    },
    params: {
      Contrast: 'us_x',
      Similarity: 'dot',
      Epochs: '1000',
      'Batch Size': '256',
      'Learning Rate': '0.001',
      'Max sequence length': '50',
    },
    visual: '重点展示频域增强参数、训练轮数、ml-100k 数据资产和日志缺口。因为 epochs 配置较高，后续补日志时应重点看收敛曲线和过拟合风险。',
    compare: '适合与 DuoRec 同屏比对：同数据、同类对比配置，但增强机制不同。',
    aiRule: 'AI 应把 FEARec 描述为“已具备代码与真实数据，尚未形成站内评估证据”。',
  },
  {
    numericId: 6,
    id: 'fmlprec',
    name: 'FMLP-Rec',
    displayName: 'FMLP-Rec 滤波增强推荐',
    architecture: 'FMLP-Rec',
    family: '滤波增强推荐',
    status: '权重+日志',
    cover: 'https://images.unsplash.com/photo-1558494949-ef010cbdcc31?w=1400&h=800&fit=crop',
    summary: 'FMLP-Rec 用滤波增强思想建模序列偏好，当前 Beauty 数据、权重和测试指标日志都已存在。',
    data: {
      Dataset: 'Beauty',
      Users: '22,363',
      Items: '12,101',
      Interactions: '198,502',
      Records: '22,363',
      'Max sequence length': '204',
      Size: '1.1 MB',
    },
    metrics: {
      'HIT@1': '0.1942',
      'NDCG@1': '0.1942',
      'HIT@5': '0.3963',
      'NDCG@5': '0.3004',
      'HR@10': '0.4935',
      'NDCG@10': '0.3317',
      MRR: '0.2985',
    },
    training: {
      Weight: '1 个',
      Log: '1 个',
      Entrypoint: 'main.py',
      Service: '未注册后端推理服务',
    },
    params: {
      'Hidden Size': '64',
      'Filter-enhanced Blocks': '2',
      'Attention Heads': '2',
      'Batch Size': '256',
      'Learning Rate': '0.001',
      'Max sequence length': '50',
    },
    visual: '重点展示 HIT/NDCG/MRR 的多 K 对比、Beauty 数据规模和权重日志完整度。它适合作为推荐指标教学样例，因为日志指标比多数代码+数据模型更完整。',
    compare: '可与 BSARec Beauty 方向对比：两者都覆盖 Beauty 数据，但指标来源、架构机制和日志文件不同。',
    aiRule: 'AI 可以引用 FMLP-Rec 的真实 HIT/NDCG/MRR 指标，但必须说明未注册后端推理服务。',
  },
  {
    numericId: 7,
    id: 'recbole',
    name: 'RecBole',
    displayName: 'RecBole 推荐框架',
    architecture: 'RecBole',
    family: '推荐框架',
    status: '代码+数据',
    cover: 'https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=1400&h=800&fit=crop',
    summary: 'RecBole 是推荐系统实验框架，当前接入 ml-100k atomic 数据和配置文件，用于统一组织推荐模型实验。',
    data: {
      Dataset: 'ml-100k',
      Users: '943',
      Items: '1,682',
      Interactions: '100,000',
      Records: '100,000',
      Size: '2.6 MB',
    },
    metrics: {
      Status: '暂无真实评估日志',
      Reason: '当前目录没有可读取的评估日志',
    },
    training: {
      Weight: '缺失',
      Log: '缺失',
      Entrypoint: 'run_recbole.py',
      Config: 'ml-100k.yaml',
      Service: '未注册后端推理服务',
    },
    params: {
      Framework: 'RecBole',
      'Dataset format': '.inter/.item/.user',
      Evaluator: '配置驱动',
    },
    visual: '重点展示数据格式、配置驱动能力和框架型接入状态。它不是单一推荐算法，因此图表应强调框架资产和数据准备，而不是给出某个模型的线上推荐结果。',
    compare: '适合与 DuoRec、FEARec 对比：三者都用 ml-100k，但 RecBole 是框架，DuoRec/FEARec 是具体模型。',
    aiRule: 'AI 应把 RecBole 解释为实验框架，不要把它说成已经有权重的单个模型服务。',
  },
  {
    numericId: 8,
    id: 'sasrec',
    name: 'SASRec',
    displayName: 'SASRec 自注意力序列推荐',
    architecture: 'SASRec',
    family: 'Transformer 序列推荐',
    status: '代码+数据',
    cover: 'https://images.unsplash.com/photo-1535223289827-42f1e9919769?w=1400&h=800&fit=crop',
    summary: 'SASRec 使用自注意力建模用户行为序列，当前接入 Beauty、ML-1M、Steam、Video 四份真实交互数据。',
    data: {
      Dataset: 'Beauty, ml-1m, Steam, Video',
      Users: '423,987',
      Items: '97,467',
      Interactions: '5,367,798',
      Records: '5,367,798',
      Size: '61.7 MB',
    },
    metrics: {
      Status: '暂无真实评估日志',
      Reason: '当前目录没有可读取的评估日志',
    },
    training: {
      Weight: '缺失',
      Log: '缺失',
      Entrypoint: 'main.py',
      Service: '未注册后端推理服务',
    },
    params: {
      'Hidden Units': '50',
      'Transformer Blocks': '2',
      'Attention Heads': '1',
      'Batch Size': '128',
      'Learning Rate': '0.001',
      'Max sequence length': '50',
    },
    visual: '重点展示超大交互规模、不同数据集的用户物品分布和自注意力配置。没有日志时只能展示数据资产和接入状态，不能计算真实排序指标。',
    compare: '适合与 BERT4Rec、TiSASRec 比较：SASRec 是经典自注意力序列推荐，BERT4Rec 是双向建模，TiSASRec 额外引入时间间隔。',
    aiRule: 'AI 需要说明 SASRec 当前是代码+数据接入，后续要补训练日志才能进入正式性能排名。',
  },
  {
    numericId: 9,
    id: 'tisasrec',
    name: 'TiSASRec',
    displayName: 'TiSASRec 时间间隔序列推荐',
    architecture: 'TiSASRec',
    family: '时间感知推荐',
    status: '代码+数据',
    cover: 'https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=1400&h=800&fit=crop',
    summary: 'TiSASRec 在序列推荐中显式建模交互时间间隔，当前接入 ML-1M 用户、物品、评分和时间戳数据。',
    data: {
      Dataset: 'ml-1m',
      Users: '6,040',
      Items: '3,706',
      Interactions: '1,000,209',
      Records: '1,000,209',
      Size: '21.5 MB',
    },
    metrics: {
      Status: '暂无真实评估日志',
      Reason: '当前目录没有可读取的评估日志',
    },
    training: {
      Weight: '缺失',
      Log: '缺失',
      Entrypoint: 'main.py',
      Service: '未注册后端推理服务',
    },
    params: {
      'Hidden Units': '50',
      'Transformer Blocks': '2',
      'Attention Heads': '1',
      'Time span': '256',
      'Batch Size': '128',
      'Learning Rate': '0.001',
    },
    visual: '重点展示时间戳字段、时间间隔建模参数、ML-1M 规模和代码+数据接入状态。它适合解释“同样是用户序列，时间间隔为什么会影响推荐”。',
    compare: '适合与 SASRec 对比：两者都使用自注意力，TiSASRec 额外使用时间间隔信息。',
    aiRule: 'AI 应提示用户：TiSASRec 有真实时间戳数据，但没有站内评估日志和后端推理接口。',
  },
]

function table(rows) {
  return Object.entries(rows)
    .map(([key, value]) => `| ${key} | ${value} |`)
    .join('\n')
}

function buildModelArticle(model) {
  return `![${model.displayName} 封面](${model.cover})

# ${model.displayName} 深度解析：推荐系统真实接入与数据说明

## 模型定位

${model.displayName} 属于 DeepInsight 当前固定的 9 个推荐系统模型，架构为 ${model.architecture}，模型家族为 ${model.family}。${model.summary} 这篇文章只描述本项目已经能从本地目录、日志或后端接口核验到的内容，不补写不存在的实验结论。

## 真实数据资产

| 字段 | 当前记录 |
|------|----------|
${table(model.data)}

推荐系统数据的核心不是图片类别或未来曲线，而是用户、物品和交互序列。阅读这类数据时应先确认用户规模、物品规模、交互数、记录数和最大序列长度，再判断模型指标是否可比。跨数据集汇总时，用户 ID 和物品 ID 不做合并去重，所以规模只能代表本地文件资产总量。

## 指标与训练状态

| 指标 | 当前记录 |
|------|----------|
${table(model.metrics)}

| 训练/接入项 | 当前记录 |
|-------------|----------|
${table(model.training)}

| 参数 | 当前记录 |
|------|----------|
${table(model.params)}

当前状态：**${model.status}**。${model.status.includes('日志') ? '这些指标来自本地评估日志，可以进入性能看板做正式展示。' : '当前没有站内评估日志，因此知识库、AI 和可视化页面都只能展示数据、代码、配置和接入成熟度，不能编造 HR、NDCG 或 MRR。'}

## 可视化与比对重点

${model.visual}

${model.compare}

推荐系统可视化应围绕 Top-K 排名、HR@K、NDCG@K、MRR、数据规模、日志可信度、服务就绪度和接入成熟度展开。没有日志的模型仍然可以展示真实数据规模和配置，但不能出现在“性能最好”这类结论里。

## AI 训练口径

${model.aiRule}

AI 被问到 ${model.name} 时，应优先回答：它是推荐系统模型；当前状态是 ${model.status}；数据集是 ${model.data.Dataset}；是否有真实日志要按本文和模型总览为准。遇到缺失指标时必须直接说“当前站内未记录该指标”，不要把同家族或同数据集的指标迁移过来。

## 使用边界

当前平台不提供用户上传模型服务，也不把这些本地模型包装成全部在线推理。模型文章只用于解释真实资产、训练记录、指标口径、可视化方法和接入边界。若后续补齐权重、日志或服务，应该先更新模型总览和本文，再让 AI 使用新的站内知识。`
}

const platformNodes = [
  ['d', null, 'DeepInsight 推荐系统知识图谱', '平台核心', '围绕 9 个推荐系统模型、数据资产、指标、可视化和 AI 知识口径组织内容。', '#42e6a4', 2.5, 0],
  ['platform', 'd', '平台入口', '使用路径', '模型总览、接入测试、性能看板、数据中心、知识中心和 AI 工作区。', '#4dc9f0', 1.45, 1],
  ['model-overview', 'platform', '模型总览', '模型入口', '查看 9 个推荐模型的状态、指标、数据集、训练配置和参数画像。', '#8b5cf6', 1.0, 2],
  ['access-test', 'platform', '接入测试', '模型入口', '围绕推荐系统样例输入、服务健康度和后端代理状态做验证。', '#f59e0b', 1.0, 3],
  ['visualization', 'platform', '性能看板', '分析入口', '按 HR、NDCG、MRR、数据规模和接入成熟度做推荐模型比对。', '#ec4899', 1.0, 4],
  ['dataset-viz', 'platform', '数据集可视化', '数据分析', '查看用户、物品、交互、序列长度和数据质量。', '#60a5fa', 1.0, 5],
  ['data-center', 'platform', '数据中心', '数据资产', '管理上传数据集和素材记录，为推荐数据分析提供入口。', '#38bdf8', 1.0, 6],
  ['ai-workspace', 'platform', 'AI 工作区', '智能入口', '使用站内模型清单、知识文章和导航规则回答推荐系统问题。', '#14b8a6', 1.0, 7],
  ['model-families', 'd', '推荐模型家族', '模型体系', '9 个官方模型全部归入推荐系统，不再按旧任务分级。', '#22c55e', 1.5, 8],
  ['bsarec-family', 'model-families', 'BSARec 家族', '推荐模型', 'BSARec Job 与 BSARec 序列推荐。', '#16a34a', 1.05, 9],
  ['bsarec-job', 'bsarec-family', 'BSARec Job', '推荐模型', '岗位序列推荐，登记后端代理但服务当前离线。', '#86efac', 0.9, 10],
  ['bsarec', 'bsarec-family', 'BSARec', '推荐模型', '多数据集 BSARec，拥有权重和评估日志。', '#86efac', 0.9, 11],
  ['transformer-rec', 'model-families', 'Transformer 序列推荐', '推荐模型', 'BERT4Rec、SASRec、TiSASRec 三种序列建模路线。', '#0ea5e9', 1.05, 12],
  ['bert4rec', 'transformer-rec', 'BERT4Rec', '推荐模型', '双向序列推荐，代码和真实数据已接入。', '#7dd3fc', 0.9, 13],
  ['sasrec', 'transformer-rec', 'SASRec', '推荐模型', '自注意力序列推荐，四份真实交互数据。', '#7dd3fc', 0.9, 14],
  ['tisasrec', 'transformer-rec', 'TiSASRec', '推荐模型', '时间间隔序列推荐，使用 ML-1M 时间戳数据。', '#7dd3fc', 0.9, 15],
  ['enhanced-rec', 'model-families', '增强推荐模型', '推荐模型', 'DuoRec、FEARec、FMLP-Rec 负责对比、频域和滤波增强路线。', '#a78bfa', 1.05, 16],
  ['duorec', 'enhanced-rec', 'DuoRec', '推荐模型', '对比序列推荐，ml-100k 数据已接入。', '#c4b5fd', 0.9, 17],
  ['fearec', 'enhanced-rec', 'FEARec', '推荐模型', '频域增强序列推荐，ml-100k 数据已接入。', '#c4b5fd', 0.9, 18],
  ['fmlprec', 'enhanced-rec', 'FMLP-Rec', '推荐模型', '滤波增强推荐，Beauty 权重和指标日志已存在。', '#c4b5fd', 0.9, 19],
  ['framework-rec', 'model-families', '推荐实验框架', '推荐框架', 'RecBole 负责框架型推荐实验资产。', '#fb7185', 1.0, 20],
  ['recbole', 'framework-rec', 'RecBole', '推荐框架', 'ml-100k atomic 数据和配置驱动推荐框架。', '#fda4af', 0.9, 21],
  ['metrics', 'd', '推荐指标与证据', '评测方法', 'HR@K、NDCG@K、MRR、数据规模、权重日志和服务健康。', '#f97316', 1.35, 22],
  ['hr-k', 'metrics', 'HR@K', '评测指标', '前 K 个推荐是否命中真实目标。', '#fdba74', 0.84, 23],
  ['ndcg-k', 'metrics', 'NDCG@K', '评测指标', '命中位置越靠前得分越高。', '#fdba74', 0.84, 24],
  ['mrr', 'metrics', 'MRR', '评测指标', '首个相关结果排名的倒数均值。', '#fdba74', 0.84, 25],
  ['readiness', 'metrics', '接入成熟度', '运维指标', '数据、代码、权重、日志和服务五个维度。', '#fdba74', 0.84, 26],
  ['rec-data', 'd', '推荐数据资产', '数据体系', 'Job、Beauty、ML-100K、ML-1M、Steam、Video 等真实数据。', '#06b6d4', 1.3, 27],
  ['job-data', 'rec-data', 'Job', '数据集', '岗位推荐序列数据。', '#67e8f9', 0.86, 28],
  ['beauty-data', 'rec-data', 'Beauty', '数据集', '多模型共用的商品序列数据。', '#67e8f9', 0.86, 29],
  ['ml100k-data', 'rec-data', 'ML-100K', '数据集', 'DuoRec、FEARec、RecBole 共用数据。', '#67e8f9', 0.86, 30],
  ['ml1m-data', 'rec-data', 'ML-1M', '数据集', 'BERT4Rec、BSARec、SASRec、TiSASRec 使用的数据。', '#67e8f9', 0.86, 31],
  ['ai-training', 'd', 'AI 知识训练口径', 'AI 知识', '让 AI 只引用站内真实模型、真实指标和真实接入状态。', '#f43f5e', 1.25, 32],
]

const obsoleteNodeIds = [
  'recommendation',
  'prism-baby',
  'prism-cloth',
  'prism-sports',
  ['fore', 'casting'].join(''),
  'pmt-traffic',
  'pmt-weather',
  'pmt-ecl',
  ['class', 'ification'].join(''),
  'convnext-44',
  'convnext-557',
  'efficientnet-b3-44',
  'rec-metrics',
  'forecast-metrics',
  'cls-metrics',
]

const article = (title, nodeId, sections, pinned = false) => ({
  title,
  nodeId,
  pinned,
  content: sections.join('\n\n'),
})

const platformArticles = [
  article('DeepInsight 当前模型口径：9 个推荐系统模型', 'platform', [
    '# DeepInsight 当前模型口径：9 个推荐系统模型',
    '当前平台官方模型固定为 9 个，且全部属于推荐系统：BSARec Job、BSARec、BERT4Rec、DuoRec、FEARec、FMLP-Rec、RecBole、SASRec、TiSASRec。知识库、AI 回答、性能看板和接入测试都必须以这份清单为准。',
    '页面不再按旧任务三分法组织模型。正确的组织方式是推荐模型家族、真实数据资产、评估指标、权重日志、服务健康和接入成熟度。没有日志的模型可以展示代码、配置和数据，不能展示虚构指标；没有后端代理的模型不能写成在线推理服务。',
    '最推荐的使用路径是：先到模型总览确认 9 个模型状态，再到性能看板比较 HR/NDCG/MRR 和接入成熟度，再到数据集可视化检查用户、物品、交互和序列规模，最后在 AI 工作区让助手解释模型差异和下一步。'
  ], true),
  article('模型总览读法：状态比模型名更重要', 'model-overview', [
    '# 模型总览读法：状态比模型名更重要',
    '模型总览的第一层信息是状态。BSARec Job 显示服务离线，含义是权重、Job 数据和后端代理配置都存在，但本地 Flask 服务当前没有响应；BSARec 和 FMLP-Rec 是权重+日志，适合进入性能图表；BERT4Rec、DuoRec、FEARec、RecBole、SASRec、TiSASRec 是代码+数据，主要展示真实资产和配置。',
    '第二层信息是指标来源。BSARec 有 HR@5、NDCG@5、HR@10、NDCG@10、HR@20、NDCG@20；FMLP-Rec 有 HIT、NDCG 和 MRR；没有日志的模型只能显示“当前站内未记录该指标”。',
    '第三层信息是数据规模。推荐系统模型之间的指标不能脱离数据集比较，Job、Beauty、ML-100K、ML-1M、Steam、Video 的用户规模、物品规模和交互密度差别很大。'
  ], true),
  article('接入测试中心：只验证真实可调用的推荐入口', 'access-test', [
    '# 接入测试中心：只验证真实可调用的推荐入口',
    '接入测试中心现在围绕推荐系统输入输出设计。用户给出历史 item 序列和 Top-K 后，页面应显示模型状态、请求结果、推荐列表、耗时和错误原因。当前只有 BSARec Job 登记了后端代理 /api/v1/prediction/recommend。',
    '当 BSARec Job 显示服务离线时，页面和 AI 都要解释为本地推荐服务未响应，而不是模型未接入。其他模型即使有代码和数据，也不能在没有后端代理时强行模拟成在线推荐。',
    '正确的按钮行为是：能调用就调用真实接口；不能调用就说明缺少哪一环，例如权重、日志、服务或代理。任何“假成功”“假推荐结果”都会污染知识库和 AI 训练口径。'
  ]),
  article('性能看板：推荐指标怎么比', 'visualization', [
    '# 性能看板：推荐指标怎么比',
    '推荐模型的核心指标是 HR@K、NDCG@K 和 MRR。HR@K 说明前 K 个推荐里是否命中，NDCG@K 说明命中是否排得靠前，MRR 说明第一个相关结果出现得早不早。它们都服务于排序质量，而不是通用准确率。',
    '看板里应该把“有真实日志”和“没有日志”的模型分开。BSARec 与 FMLP-Rec 可以进入指标比对；BERT4Rec、DuoRec、FEARec、RecBole、SASRec、TiSASRec 先进入数据规模和接入成熟度比对。',
    '如果用户问哪个模型最好，AI 不能直接按一个数下结论。它应先说明数据集、日志来源、Top-K 口径和服务接入状态，再给出可验证的比较。'
  ], true),
  article('数据中心：推荐系统数据先看四件事', 'dataset-viz', [
    '# 数据中心：推荐系统数据先看四件事',
    '推荐数据先看四件事：用户数、物品数、交互数、序列长度。用户数决定覆盖范围，物品数决定候选空间，交互数决定训练信号密度，序列长度决定模型能否学习短期与长期兴趣。',
    'Job 数据用于岗位推荐；Beauty 在 BSARec、BERT4Rec、FMLP-Rec、SASRec 中都出现；ML-100K 用于 DuoRec、FEARec、RecBole；ML-1M 在多个序列推荐模型中出现；Steam、Video 等数据用于观察更大规模交互。',
    '用户上传的数据集当前主要用于记录、预览和分析准备。它不会自动变成官方模型训练数据，也不会自动注册推理服务。'
  ]),
  article('AI 训练口径：助手必须只讲站内真实证据', 'ai-training', [
    '# AI 训练口径：助手必须只讲站内真实证据',
    'DeepInsight 的站内 AI 不是通用聊天说明书，它应该优先读取模型总览、知识文章、平台知识文档、论坛官方文章和导航规则。训练口径只有一条主线：当前 9 个官方模型全部是推荐系统模型。',
    'AI 回答模型问题时必须覆盖五个字段：模型名称、推荐系统定位、真实数据资产、指标/日志状态、接入边界。对于 BSARec Job，要说明它有后端代理但服务当前可能离线；对于 BSARec 和 FMLP-Rec，要引用真实日志指标；对于代码+数据模型，要明确指标未登记。',
    'AI 不应暴露服务器绝对路径、数据库连接、密钥或内部启动细节。它可以给页面路径和操作步骤，例如去模型总览、接入测试、性能看板、数据集可视化或知识中心。遇到缺失指标时要说“当前站内未记录该指标”，不要补写看似合理的数字。'
  ], true),
  article('模型比对路线：从 BSARec 到 TiSASRec', 'model-families', [
    '# 模型比对路线：从 BSARec 到 TiSASRec',
    'BSARec Job 和 BSARec 属于 BSARec 家族，适合讨论岗位推荐、多数据集推荐和服务接入成熟度。BERT4Rec、SASRec、TiSASRec 属于序列 Transformer 路线，适合讨论双向建模、自注意力和时间间隔。DuoRec、FEARec、FMLP-Rec 属于增强推荐路线，适合讨论对比学习、频域增强和滤波增强。RecBole 是推荐实验框架。',
    '比对时先问“比什么”。比线上可用性，只有 BSARec Job 有后端代理；比日志指标，BSARec 和 FMLP-Rec 更完整；比数据规模，SASRec 和 BERT4Rec 覆盖更大交互量；比框架能力，RecBole 是配置驱动的实验框架。',
    '知识文章应该帮助用户建立这种比对路线，而不是把不同状态的模型混在一个排行榜里。'
  ]),
  article('缺失日志怎么处理：代码+数据不是未接入', 'readiness', [
    '# 缺失日志怎么处理：代码+数据不是未接入',
    '代码+数据表示模型目录中能找到运行入口和真实数据，但还没有站内可读取的权重或评估日志。这不是“未接入”，也不是“在线可用”，而是处在资产就绪但证据不足的状态。',
    '正确展示方式是：数据就绪度 100，代码就绪度 100，权重和日志按真实情况显示 0 或缺失。可视化可以展示数据规模、参数配置、文件状态和下一步建议，不能生成指标曲线。',
    '下一步应补训练或评估日志，然后刷新模型总览和知识库。只有当日志存在，AI 才能把对应指标用于正式解释。'
  ]),
  article('推荐指标入门：HR@K、NDCG@K、MRR', 'metrics', [
    '# 推荐指标入门：HR@K、NDCG@K、MRR',
    'HR@K 关注前 K 个推荐里有没有命中目标，适合回答“有没有推荐中”。NDCG@K 关注命中是否排在更靠前的位置，适合回答“排序质量好不好”。MRR 关注第一个相关结果出现的位置，适合回答“用户多快能看到相关结果”。',
    '这些指标必须带 K 值和数据集上下文。BSARec 的 HR@10 与 FMLP-Rec 的 HR@10/HIT@10 不能脱离数据、评估脚本和候选集设置直接下绝对结论。',
    'AI 解释指标时应该同时说明：指标来自哪份日志、是否是同一数据集、模型是否有后端服务、是否存在缺失值。'
  ]),
]

const modelNodeIds = modelArticles.map((model) => model.id)
const managedNodeIds = Array.from(new Set([...platformNodes.map((node) => node[0]), ...modelNodeIds, ...obsoleteNodeIds]))

module.exports = {
  modelArticles,
  modelNodeIds,
  platformNodes,
  platformArticles,
  managedNodeIds,
  obsoleteNodeIds,
  buildModelArticle,
}
