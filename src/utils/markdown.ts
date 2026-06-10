import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'

const md = new MarkdownIt({
  html: false,
  linkify: true,
  typographer: true,
  highlight(code: string, lang: string): string {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return '<pre class="hljs"><code>' +
          hljs.highlight(code, { language: lang, ignoreIllegals: true }).value +
          '</code></pre>'
      } catch {
        // Fall through to escaped plain code.
      }
    }
    return '<pre class="hljs"><code>' + md.utils.escapeHtml(code) + '</code></pre>'
  },
})

function slugifyHeading(text: string): string {
  const slug = text
    .replace(/<[^>]+>/g, '')
    .replace(/[^\w\u4e00-\u9fa5]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .toLowerCase()
  return slug || 'section'
}

function uniqueHeadingId(text: string, counts: Map<string, number>): string {
  const base = slugifyHeading(text)
  const next = (counts.get(base) || 0) + 1
  counts.set(base, next)
  return next === 1 ? base : `${base}-${next}`
}

md.renderer.rules.link_open = (tokens, idx, options, _env, self) => {
  tokens[idx].attrSet('target', '_blank')
  tokens[idx].attrSet('rel', 'noopener noreferrer')
  return self.renderToken(tokens, idx, options)
}

md.renderer.rules.heading_open = (tokens, idx, options, _env, self) => {
  const inline = tokens[idx + 1]
  if (inline && inline.type === 'inline') {
    const env = _env as Record<string, any>
    const counts = env.__headingSlugCounts || (env.__headingSlugCounts = new Map<string, number>())
    tokens[idx].attrSet('id', uniqueHeadingId(inline.content || '', counts))
  }
  return self.renderToken(tokens, idx, options)
}

export function renderMarkdown(raw: string): string {
  if (!raw) return ''
  return md.render(raw, { __headingSlugCounts: new Map<string, number>() })
}

export interface Heading {
  level: number
  text: string
  id: string
}

export function extractHeadings(raw: string): Heading[] {
  const headings: Heading[] = []
  const counts = new Map<string, number>()
  const tokens = md.parse(raw, {})
  tokens.forEach((token, index) => {
    if (token.type !== 'heading_open') return
    const inline = tokens[index + 1]
    if (!inline || inline.type !== 'inline') return
    const text = inline.content || ''
    headings.push({ level: Number(token.tag.slice(1)), text, id: uniqueHeadingId(text, counts) })
  })
  return headings
}

export function enrichArticleContent(raw: string, title = ''): string {
  const content = (raw || '').trim()
  if (!content) return ''
  const existingHeadings = extractHeadings(content).filter((heading) => heading.level >= 2 && heading.level <= 4)
  if (existingHeadings.length >= 3) return content

  const paragraphs = content
    .replace(/!\[[^\]]*]\([^)]+\)\s*/g, '')
    .split(/\n{2,}/)
    .map((part) => part.trim())
    .filter((part) => part && !/^#{1,6}\s+/.test(part))
    .slice(0, 4)

  const normalizedTitle = (title || extractHeadings(content)[0]?.text || '这篇文章').trim()
  const summary = paragraphs[0] || '这篇文章用于补充 DeepInsight 站内知识，帮助用户理解推荐系统模型、数据资源、评测口径和页面使用方式。'
  const isModelArticle = /BSARec|BERT4Rec|DuoRec|FEARec|FMLP|RecBole|SASRec|TiSASRec|模型|推荐/.test(`${normalizedTitle}\n${content}`)

  const additions = [
    '## 核心结论',
    summary,
    '',
    '## 阅读重点',
    isModelArticle
      ? '- 先确认文章讨论的是推荐系统模型、数据集、训练日志还是接入状态。\n- 指标解释以站内已有评估日志和页面展示为准，没有日志时不编造分数。\n- 如果文章涉及模型对比，应同时关注数据集、序列长度、训练配置和评价指标。'
      : '- 先看文章对应的知识节点，再结合页面入口理解它在系统中的作用。\n- 站内文章可以被 AI 检索引用，但用户私有资料仍按账号隔离。\n- 涉及外部资料时，应区分站内事实、外部参考和 AI 推断。',
    '',
    '## 在 DeepInsight 中怎么用',
    isModelArticle
      ? '- 到模型总览查看模型是否已登记、是否有权重、日志和数据集。\n- 到接入测试验证后端推荐接口是否可调用。\n- 到性能看板或数据集可视化查看真实数据和评测结果。'
      : '- 到知识中心查看关联节点和文章。\n- 到 AI 工作室提问时，可以让 AI 结合站内知识文章、论坛文章和用户已授权资料回答。\n- 到素材库搜索站内资源时，官方资料只读，用户上传资料写入对应账号数据库。',
  ]

  return `${content}\n\n${additions.join('\n')}`
}

export function estimateReadingTime(raw: string): { words: number; minutes: number } {
  if (!raw) return { words: 0, minutes: 0 }
  const cleaned = raw.replace(/```[\s\S]*?```/g, '').replace(/[#*`~\[\]()>|!\-_]/g, '')
  const chineseChars = (cleaned.match(/[\u4e00-\u9fa5]/g) || []).length
  const englishWords = cleaned.replace(/[\u4e00-\u9fa5]/g, '').split(/\s+/).filter(Boolean).length
  const totalWords = chineseChars + englishWords
  return { words: totalWords, minutes: Math.max(1, Math.ceil(totalWords / 400)) }
}

export function stripMarkdown(raw: string, maxLen = 200): string {
  if (!raw) return ''
  const cleaned = raw
    .replace(/```[\s\S]*?```/g, ' ')
    .replace(/[#*`~\[\]()>|!\-_]/g, '')
    .replace(/\s+/g, ' ')
    .trim()
  return cleaned.length > maxLen ? cleaned.slice(0, maxLen) + '...' : cleaned
}
