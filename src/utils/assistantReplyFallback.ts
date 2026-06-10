type ReplyFallbackContext = {
  userMessage: string
  currentPageTitle?: string
}

const RAW_API_FAILURE_PATTERN = /(AI调用失败|API调用失败:\s*I\/O error on POST request|I\/O error on POST request)/i

export function normalizeAssistantReply(reply: string, context: ReplyFallbackContext): string {
  if (!RAW_API_FAILURE_PATTERN.test(reply)) return reply

  const question = context.userMessage || ''
  const pageTitle = context.currentPageTitle || ''
  const localAnswer = localContextAnswer(question, pageTitle)
  const failure = [
    '外部模型暂时不可用：后端无法连接 DeepSeek（api.deepseek.com）。',
    '可能原因是 DNS 解析失败、网络被代理/防火墙拦截，或 Java 后端没有配置 HTTPS 代理。',
    '这不是当前页面操作错误，也不是前端组件渲染错误。',
  ].join('\n')

  return localAnswer ? `${localAnswer}\n\n${failure}` : failure
}

function localContextAnswer(question: string, currentPageTitle: string) {
  const normalized = `${question} ${currentPageTitle}`.toLowerCase()

  if (normalized.includes('模型总览') || normalized.includes('model overview') || normalized.includes('/training')) {
    return [
      '先基于当前页面上下文回答：',
      '',
      '「模型总览」页用于查看 DeepInsight 已接入的模型清单和模型画像。当前 9 个模型全部是推荐系统模型：BSARec Job、BSARec、BERT4Rec、DuoRec、FEARec、FMLP-Rec、RecBole、SASRec、TiSASRec。',
      '',
      '重点看这些模块：',
      '1. 接入状态：区分服务离线、权重+日志、代码+数据，避免把资产接入误说成在线推理。',
      '2. 核心指标：推荐系统主要看 Top-K、HR@K、NDCG@K、MRR，缺失日志时不要编造指标。',
      '3. 数据集摘要：确认用户数、物品数、交互数、记录数和最大序列长度。',
      '4. 训练配置与参数画像：查看架构、batch size、学习率、序列长度和模型配置。',
      '5. 下一步入口：要试后端代理去「模型接入测试」，要横向比较去「性能看板」，要看数据结构去「数据集实时可视化」。',
    ].join('\n')
  }

  if (normalized.includes('bsarec job')) {
    return [
      '先按站内知识回答：',
      '',
      'BSARec Job 是 DeepInsight 已登记后端推荐代理的岗位序列推荐模型。它有 Job 序列数据、权重、日志和后端代理配置，但 Flask 推荐服务可能没有运行；这时页面会显示“服务离线”。',
      '',
      '下一步可以这样查：',
      '1. 在「模型接入测试」页点击推荐接口，查看后端返回的服务状态。',
      '2. 在「性能看板」查看已有日志指标和接入证据。',
      '3. 在「数据集实时可视化」查看 Job.txt 的用户、物品和交互规模。',
    ].join('\n')
  }

  if (normalized.includes('bsarec') || normalized.includes('bert4rec') || normalized.includes('duorec')
    || normalized.includes('fearec') || normalized.includes('fmlp') || normalized.includes('recbole')
    || normalized.includes('sasrec') || normalized.includes('tisasrec')) {
    return [
      '先按站内知识回答：',
      '',
      '这些条目都属于 DeepInsight 当前的推荐系统模型清单。平台固定展示 9 个模型：BSARec Job、BSARec、BERT4Rec、DuoRec、FEARec、FMLP-Rec、RecBole、SASRec、TiSASRec。',
      '',
      '查看时不要再按旧的分类/检测/分割方式理解，而要看推荐任务的功能状态：数据文件、用户数、物品数、交互数、HR@K、NDCG@K、MRR、权重日志和后端服务状态。缺少日志或服务的模型会显示待补充状态。',
    ].join('\n')
  }

  if (normalized.includes('convnext') || normalized.includes('efficientnet') || normalized.includes('pmt') || normalized.includes('prism')) {
    return [
      '先基于当前页面上下文回答：',
      '',
      '这个名称属于旧任务分类，不在 DeepInsight 模型清单里。当前模型清单固定为 9 个推荐系统模型：BSARec Job、BSARec、BERT4Rec、DuoRec、FEARec、FMLP-Rec、RecBole、SASRec、TiSASRec。',
      '',
      '建议下一步：打开「模型总览」确认模型状态；如果要看可调用入口，打开「模型接入测试」查看 BSARec Job 的服务状态；如果要比较指标，打开「性能看板」查看 BSARec 和 FMLP-Rec 的可用日志指标。',
    ].join('\n')
  }

  return ''
}
