const fs = require('node:fs')
const path = require('node:path')
const {
  platformNodes,
  platformArticles,
  modelNodeIds,
  managedNodeIds,
} = require('./recommender-knowledge-data.cjs')

function loadEnv(filePath) {
  if (!fs.existsSync(filePath)) return
  for (const rawLine of fs.readFileSync(filePath, 'utf8').split(/\r?\n/)) {
    const line = rawLine.trim()
    if (!line || line.startsWith('#') || !line.includes('=')) continue
    const index = line.indexOf('=')
    const key = line.slice(0, index).trim()
    const value = line.slice(index + 1).trim().replace(/^['"]|['"]$/g, '')
    if (/^[A-Za-z_][A-Za-z0-9_]*$/.test(key) && process.env[key] === undefined) {
      process.env[key] = value
    }
  }
}

function loadMysql() {
  try {
    return require('mysql2/promise')
  } catch {
    return require('../deploy/node_modules/mysql2/promise')
  }
}

loadEnv(path.resolve(__dirname, '..', '.env.local'))

const mysql = loadMysql()
const dbConfig = {
  host: process.env.DB_HOST || 'localhost',
  port: Number(process.env.DB_PORT || 3306),
  user: process.env.DB_USER || 'root',
  password: process.env.DB_PASSWORD || '',
  database: process.env.DB_NAME || 'deepinsight',
  charset: 'utf8mb4',
  multipleStatements: false,
}

function getModelNodeIdFromTags(tags = '') {
  const tagSet = tags.split(',').map((tag) => tag.trim())
  return modelNodeIds.find((nodeId) => tagSet.includes(nodeId))
}

async function getAuthorId(connection) {
  const [users] = await connection.execute(
    `SELECT id
       FROM users
      ORDER BY (username = 'deepinsight-official') DESC, (role = 'ADMIN') DESC, id ASC
      LIMIT 1`
  )
  if (!users.length) {
    throw new Error('No user found. Create an admin user before seeding knowledge articles.')
  }
  return users[0].id
}

async function clearGeneratedPlatformContent(connection) {
  const placeholders = managedNodeIds.map(() => '?').join(', ')
  await connection.execute(
    `DELETE FROM knowledge_articles
      WHERE node_id IN (${placeholders})
         OR title IN (${platformArticles.map(() => '?').join(', ')})`,
    [...managedNodeIds, ...platformArticles.map((item) => item.title)]
  )
  await connection.execute(
    `DELETE FROM knowledge_nodes
      WHERE id IN (${placeholders})
         OR parent_id IN (${placeholders})`,
    [...managedNodeIds, ...managedNodeIds]
  )
  await connection.execute(
    `DELETE FROM knowledge_docs
      WHERE category = 'platform_knowledge'
         OR tags LIKE '%platform-knowledge%'`
  )
}

async function seed() {
  const connection = await mysql.createConnection(dbConfig)
  const now = new Date()
  const authorId = await getAuthorId(connection)

  await connection.beginTransaction()
  try {
    await clearGeneratedPlatformContent(connection)

    for (const node of platformNodes) {
      await connection.execute(
        `INSERT INTO knowledge_nodes
          (id, parent_id, label, category, description, color, size_val, sort_order, created_at, updated_at)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
        [...node, now, now]
      )
    }

    for (const item of platformArticles) {
      const [result] = await connection.execute(
        `INSERT INTO knowledge_articles
          (node_id, title, content, author_id, is_pinned, view_count, created_at, updated_at)
         VALUES (?, ?, ?, ?, ?, 0, ?, ?)`,
        [item.nodeId, item.title, item.content, authorId, item.pinned ? 1 : 0, now, now]
      )
      await connection.execute(
        `INSERT INTO knowledge_docs
          (title, content, category, tags, embedding_vector, created_at)
         VALUES (?, ?, 'platform_knowledge', ?, NULL, ?)`,
        [
          item.title,
          item.content,
          `platform-knowledge,recommender-only,${item.nodeId},article-id-${result.insertId}`,
          now,
        ]
      )
    }

    const [modelDocs] = await connection.execute(
      `SELECT title, content, tags
         FROM knowledge_docs
        WHERE category = 'model_deep_dive'
        ORDER BY id ASC`
    )

    for (const doc of modelDocs) {
      const nodeId = getModelNodeIdFromTags(doc.tags || '')
      if (!nodeId) continue
      await connection.execute(
        `INSERT INTO knowledge_articles
          (node_id, title, content, author_id, is_pinned, view_count, created_at, updated_at)
         VALUES (?, ?, ?, ?, 0, 0, ?, ?)`,
        [
          nodeId,
          doc.title,
          doc.content,
          authorId,
          now,
          now,
        ]
      )
    }

    await connection.commit()
    console.log(`Seeded ${platformNodes.length} recommender knowledge nodes, ${platformArticles.length} platform articles, and ${modelDocs.length} model articles.`)
  } catch (error) {
    await connection.rollback()
    throw error
  } finally {
    await connection.end()
  }
}

async function verify() {
  const connection = await mysql.createConnection(dbConfig)
  try {
    const [nodeRows] = await connection.execute('SELECT id, parent_id AS parentId, label FROM knowledge_nodes ORDER BY sort_order ASC')
    const [articleRows] = await connection.execute('SELECT id, node_id AS nodeId, title FROM knowledge_articles ORDER BY is_pinned DESC, id ASC')
    const [docRows] = await connection.execute('SELECT category, COUNT(*) AS count FROM knowledge_docs GROUP BY category ORDER BY category ASC')
    console.log(JSON.stringify({
      knowledgeNodes: nodeRows.length,
      knowledgeArticles: articleRows.length,
      knowledgeDocsByCategory: docRows,
      articleTitles: articleRows.map((row) => row.title),
    }, null, 2))
  } finally {
    await connection.end()
  }
}

const runner = process.argv.includes('--verify') ? verify : seed

runner().catch((error) => {
  console.error(error)
  process.exit(1)
})
