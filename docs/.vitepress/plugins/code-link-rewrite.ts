/**
 * VitePress 代码链接环境动态切换插件
 *
 * 功能：
 * - pnpm build（线上）：保持 GitHub URL 原样输出
 * - pnpm dev（本地）：markdown-it 插件将 GitHub URL 重写为本地路径 + Vite 中间件提供文件服务
 *
 * 配置常量（迁移到其他项目时修改这里）：
 */

import type MarkdownIt from 'markdown-it'
import { readdirSync, readFileSync, statSync, existsSync } from 'node:fs'
import { resolve, join, relative } from 'node:path'
import type { Plugin } from 'vite'

// ========== 配置常量 ==========
// GitHub 仓库中代码目录的 URL 前缀
export const GITHUB_CODE_URL = 'https://github.com/skyhe58/guide-java/tree/main/code-examples/'
// 本地文件服务的 URL 前缀
export const LOCAL_CODE_PREFIX = '/code-examples/'
// ==============================

/**
 * markdown-it 插件：dev 模式下将 GitHub code-examples URL 重写为本地路径
 * 并为链接添加 target="_blank"
 */
export function codeLinksPlugin(md: MarkdownIt): void {
  const defaultRender = md.renderer.rules.link_open ||
    function (tokens, idx, options, _env, self) {
      return self.renderToken(tokens, idx, options)
    }

  md.renderer.rules.link_open = function (tokens, idx, options, env, self) {
    const hrefIndex = tokens[idx].attrIndex('href')
    if (hrefIndex >= 0) {
      const href = tokens[idx].attrs![hrefIndex][1]
      // 匹配 GitHub code-examples URL
      if (href.startsWith(GITHUB_CODE_URL)) {
        const localPath = LOCAL_CODE_PREFIX + href.slice(GITHUB_CODE_URL.length)
        tokens[idx].attrs![hrefIndex][1] = localPath
        // 新标签页打开，方便文档和代码对照
        tokens[idx].attrPush(['target', '_blank'])
      }
    }
    return defaultRender(tokens, idx, options, env, self)
  }
}

/**
 * Vite 插件：dev 模式下提供 code-examples 目录的文件服务
 * - 文件请求：返回文件内容
 * - 目录请求：返回目录文件列表 HTML
 */
export function serveCodeExamples(projectRoot: string): Plugin {
  const codeRoot = resolve(projectRoot, 'code-examples')

  return {
    name: 'serve-code-examples',
    configureServer(server) {
      server.middlewares.use((req, res, next) => {
        if (!req.url?.startsWith(LOCAL_CODE_PREFIX)) {
          return next()
        }

        // 解码 URL 并防止目录遍历攻击
        const urlPath = decodeURIComponent(req.url.slice(LOCAL_CODE_PREFIX.length))
        if (urlPath.includes('..')) {
          res.statusCode = 403
          res.end('Forbidden')
          return
        }

        const fullPath = resolve(codeRoot, urlPath)

        // 确保路径在 code-examples 目录内
        if (!fullPath.startsWith(codeRoot)) {
          res.statusCode = 403
          res.end('Forbidden')
          return
        }

        if (!existsSync(fullPath)) {
          res.statusCode = 404
          res.end('Not Found')
          return
        }

        const stat = statSync(fullPath)

        if (stat.isDirectory()) {
          // 目录：返回文件列表 HTML
          const files = readdirSync(fullPath)
          const relDir = relative(codeRoot, fullPath) || '.'
          const html = generateDirectoryListing(relDir, files, fullPath)
          res.setHeader('Content-Type', 'text/html; charset=utf-8')
          res.end(html)
        } else {
          // 文件：返回纯文本内容（方便查看源码）
          const content = readFileSync(fullPath, 'utf-8')
          res.setHeader('Content-Type', 'text/plain; charset=utf-8')
          res.end(content)
        }
      })
    },
  }
}

/**
 * 生成目录列表 HTML
 */
function generateDirectoryListing(relDir: string, files: string[], dirPath: string): string {
  const items = files
    .filter(f => !f.startsWith('.'))
    .map(f => {
      const isDir = statSync(join(dirPath, f)).isDirectory()
      const display = isDir ? `${f}/` : f
      const href = `${LOCAL_CODE_PREFIX}${relDir === '.' ? '' : relDir + '/'}${f}${isDir ? '/' : ''}`
      return `<li><a href="${href}">${display}</a></li>`
    })
    .join('\n      ')

  return `<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <title>code-examples/${relDir}</title>
  <style>
    body { font-family: monospace; padding: 2rem; background: #1a1a2e; color: #e0e0e0; }
    h1 { color: #64b5f6; font-size: 1.2rem; }
    ul { list-style: none; padding: 0; }
    li { padding: 0.3rem 0; }
    a { color: #81c784; text-decoration: none; }
    a:hover { text-decoration: underline; }
  </style>
</head>
<body>
  <h1>📂 code-examples/${relDir}</h1>
  <ul>
      ${items}
  </ul>
</body>
</html>`
}
