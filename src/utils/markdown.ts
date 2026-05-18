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
  return text
    .replace(/<[^>]+>/g, '')
    .replace(/[^\w\u4e00-\u9fa5]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .toLowerCase()
}

md.renderer.rules.link_open = (tokens, idx, options, _env, self) => {
  tokens[idx].attrSet('target', '_blank')
  tokens[idx].attrSet('rel', 'noopener noreferrer')
  return self.renderToken(tokens, idx, options)
}

md.renderer.rules.heading_open = (tokens, idx, options, _env, self) => {
  const inline = tokens[idx + 1]
  if (inline && inline.type === 'inline') {
    tokens[idx].attrSet('id', slugifyHeading(inline.content || ''))
  }
  return self.renderToken(tokens, idx, options)
}

export function renderMarkdown(raw: string): string {
  if (!raw) return ''
  return md.render(raw)
}

export interface Heading {
  level: number
  text: string
  id: string
}

export function extractHeadings(raw: string): Heading[] {
  const headings: Heading[] = []
  const tokens = md.parse(raw, {})
  tokens.forEach((token, index) => {
    if (token.type !== 'heading_open') return
    const inline = tokens[index + 1]
    if (!inline || inline.type !== 'inline') return
    const text = inline.content || ''
    headings.push({ level: Number(token.tag.slice(1)), text, id: slugifyHeading(text) })
  })
  return headings
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
