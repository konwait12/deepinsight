const fs = require('node:fs')
const path = require('node:path')
const mysql = require('../deploy/node_modules/mysql2/promise')
require('dotenv').config({ path: path.resolve(__dirname, '..', '.env.local') })

const sourceDb = process.env.DB_NAME || 'deepinsight'
const targetDb = process.env.DB_TARGET || `${sourceDb}_rebuild_${new Date().toISOString().slice(0, 10).replace(/-/g, '')}`

const config = {
  host: process.env.DB_HOST || 'localhost',
  port: Number(process.env.DB_PORT || 3306),
  user: process.env.DB_USER || 'root',
  password: process.env.DB_PASSWORD || '',
  multipleStatements: true,
}

function quoteId(name) {
  return `\`${String(name).replace(/`/g, '``')}\``
}

async function main() {
  const backupScript = path.resolve(__dirname, 'db-backup.cjs')
  if (!fs.existsSync(backupScript)) {
    throw new Error('Missing db-backup.cjs; refusing to rebuild without backup tooling.')
  }

  const connection = await mysql.createConnection(config)
  const [existing] = await connection.query('SHOW DATABASES LIKE ?', [targetDb])
  if (existing.length > 0) {
    throw new Error(`Target database already exists: ${targetDb}. Set DB_TARGET to a new name.`)
  }

  await connection.query(`CREATE DATABASE ${quoteId(targetDb)} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci`)
  const [tables] = await connection.query(`SHOW FULL TABLES FROM ${quoteId(sourceDb)} WHERE Table_type = 'BASE TABLE'`)
  const tableKey = `Tables_in_${sourceDb}`

  await connection.query('SET FOREIGN_KEY_CHECKS=0')
  for (const row of tables) {
    const table = row[tableKey]
    await connection.query(`CREATE TABLE ${quoteId(targetDb)}.${quoteId(table)} LIKE ${quoteId(sourceDb)}.${quoteId(table)}`)
    await connection.query(`INSERT INTO ${quoteId(targetDb)}.${quoteId(table)} SELECT * FROM ${quoteId(sourceDb)}.${quoteId(table)}`)
  }
  await connection.query('SET FOREIGN_KEY_CHECKS=1')

  const [counts] = await connection.query(
    tables.map((row) => {
      const table = row[tableKey]
      return `SELECT '${table}' AS table_name, (SELECT COUNT(*) FROM ${quoteId(sourceDb)}.${quoteId(table)}) AS source_count, (SELECT COUNT(*) FROM ${quoteId(targetDb)}.${quoteId(table)}) AS target_count`
    }).join(' UNION ALL '),
  )

  await connection.end()

  const mismatch = counts.filter((row) => row.source_count !== row.target_count)
  console.table(counts)
  if (mismatch.length > 0) {
    throw new Error(`Copy completed with row-count mismatches: ${mismatch.map((row) => row.table_name).join(', ')}`)
  }
  console.log(`Rebuild copy created: ${targetDb}`)
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
