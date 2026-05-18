const fs = require('node:fs')
const http = require('node:http')
const path = require('node:path')

const root = path.resolve(process.argv[2] || path.join(__dirname, 'dist'))
const port = Number(process.argv[3] || process.env.PORT || 3000)

const mimeTypes = {
  '.css': 'text/css; charset=utf-8',
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.map': 'application/json; charset=utf-8',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.svg': 'image/svg+xml',
  '.webp': 'image/webp',
  '.woff': 'font/woff',
  '.woff2': 'font/woff2',
}

function resolveRequest(urlPath) {
  const decodedPath = decodeURIComponent(urlPath.split('?')[0])
  const requested = path.resolve(root, `.${decodedPath}`)
  if (!requested.startsWith(root)) return null
  if (fs.existsSync(requested) && fs.statSync(requested).isFile()) return requested
  return path.join(root, 'index.html')
}

const server = http.createServer((request, response) => {
  const filePath = resolveRequest(request.url || '/')
  if (!filePath || !fs.existsSync(filePath)) {
    response.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' })
    response.end('Not found')
    return
  }

  const ext = path.extname(filePath).toLowerCase()
  response.writeHead(200, { 'Content-Type': mimeTypes[ext] || 'application/octet-stream' })
  fs.createReadStream(filePath).pipe(response)
})

server.listen(port, () => {
  console.log(`Serving ${root} at http://localhost:${port}`)
})
