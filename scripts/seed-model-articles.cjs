const fs = require('node:fs')
const path = require('node:path')
const { modelArticles, modelNodeIds, buildModelArticle } = require('./recommender-knowledge-data.cjs')

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

const articles = modelArticles.map((model) => ({
  ...model,
  title: `${model.displayName} 深度解析：推荐系统真实接入与数据说明`,
  content: buildModelArticle(model),
}))

for (const article of articles) {
  const compactLength = article.content.replace(/\s/g, '').length
  if (compactLength < 900) {
    throw new Error(`${article.name} article is too short: ${compactLength}`)
  }
}

async function getOfficialUserId(connection) {
  const [users] = await connection.execute(
    `SELECT id
       FROM users
      ORDER BY (username = 'deepinsight-official') DESC, (role = 'ADMIN') DESC, id ASC
      LIMIT 1`
  )
  if (!users.length) {
    throw new Error('No user found. Create an admin user before seeding official model articles.')
  }
  return users[0].id
}

async function clearGeneratedModelContent(connection) {
  const numericIds = articles.map((article) => article.numericId)
  const numericPlaceholders = numericIds.map(() => '?').join(', ')
  const nodePlaceholders = modelNodeIds.map(() => '?').join(', ')

  await connection.execute(
    `DELETE FROM forum_comments
      WHERE post_id IN (
        SELECT id FROM forum_posts WHERE source_type = 'MODEL_ARTICLE'
      )`
  )
  await connection.execute(`DELETE FROM forum_posts WHERE source_type = 'MODEL_ARTICLE'`)
  await connection.execute(`DELETE FROM model_articles WHERE model_id IN (${numericPlaceholders})`, numericIds)
  await connection.execute(
    `DELETE FROM knowledge_articles WHERE node_id IN (${nodePlaceholders})`,
    modelNodeIds
  )
  await connection.execute(
    `DELETE FROM knowledge_docs
      WHERE category = 'model_deep_dive'
         OR tags LIKE '%official-model-deep-dive%'`
  )
}

async function seed() {
  const connection = await mysql.createConnection(dbConfig)
  const now = new Date()
  const userId = await getOfficialUserId(connection)

  await connection.beginTransaction()
  try {
    await clearGeneratedModelContent(connection)

    for (const article of articles) {
      const [modelResult] = await connection.execute(
        `INSERT INTO model_articles
          (model_id, title, content, paper_url, view_count, created_at, updated_at)
         VALUES (?, ?, ?, ?, 0, ?, ?)`,
        [
          article.numericId,
          article.title,
          article.content,
          article.paperUrl || null,
          now,
          now,
        ]
      )
      const modelArticleId = modelResult.insertId

      await connection.execute(
        `INSERT INTO forum_posts
          (title, content, cover_url, source_type, source_id, user_id, is_official, is_pinned, is_locked, view_count, created_at, updated_at)
         VALUES (?, ?, ?, 'MODEL_ARTICLE', ?, ?, 1, 1, 0, 0, ?, ?)`,
        [
          article.title,
          article.content,
          article.cover,
          modelArticleId,
          userId,
          now,
          now,
        ]
      )

      await connection.execute(
        `INSERT INTO knowledge_docs
          (title, content, category, tags, embedding_vector, created_at)
         VALUES (?, ?, 'model_deep_dive', ?, NULL, ?)`,
        [
          article.title,
          article.content,
          `official-model-deep-dive,${article.id},${article.name},${article.displayName},recommendation,${article.architecture}`,
          now,
        ]
      )

      await connection.execute(
        `INSERT INTO knowledge_articles
          (node_id, title, content, author_id, is_pinned, view_count, created_at, updated_at)
         VALUES (?, ?, ?, ?, 0, 0, ?, ?)`,
        [
          article.id,
          article.title,
          article.content,
          userId,
          now,
          now,
        ]
      )
    }

    await connection.commit()

    console.log(`Seeded ${articles.length} recommender model articles.`)
    console.table(
      articles.map((article) => ({
        id: article.id,
        modelId: article.numericId,
        model: article.displayName,
        status: article.status,
        chars: article.content.replace(/\s/g, '').length,
      }))
    )
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
    const [forumRows] = await connection.execute(
      `SELECT id, title, cover_url AS coverUrl, source_type AS sourceType
         FROM forum_posts
        WHERE source_type = 'MODEL_ARTICLE'
        ORDER BY id ASC`
    )
    const [modelRows] = await connection.execute(
      `SELECT id, model_id AS modelId, title
         FROM model_articles
        ORDER BY model_id ASC`
    )
    const [knowledgeRows] = await connection.execute(
      `SELECT id, title, category, tags
         FROM knowledge_docs
        WHERE category = 'model_deep_dive'
        ORDER BY id ASC`
    )
    const nodePlaceholders = modelNodeIds.map(() => '?').join(', ')
    const [visibleKnowledgeRows] = await connection.execute(
      `SELECT id, node_id AS nodeId, title
         FROM knowledge_articles
        WHERE node_id IN (${nodePlaceholders})
        ORDER BY id ASC`,
      modelNodeIds
    )

    console.log(JSON.stringify({
      forumPosts: forumRows.length,
      modelArticles: modelRows.length,
      knowledgeDocs: knowledgeRows.length,
      visibleKnowledgeArticles: visibleKnowledgeRows.length,
      modelTitles: modelRows.map((row) => `${row.modelId}: ${row.title}`),
      visibleKnowledgeTitles: visibleKnowledgeRows.map((row) => `${row.nodeId}: ${row.title}`),
      missingCovers: forumRows.filter((row) => !row.coverUrl).map((row) => row.title),
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
