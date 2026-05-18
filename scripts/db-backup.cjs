const fs = require('node:fs')
const path = require('node:path')
const mysql = require('../deploy/node_modules/mysql2/promise')
require('dotenv').config({ path: path.resolve(__dirname, '..', '.env.local') })

const config = {
  host: process.env.DB_HOST || 'localhost',
  port: Number(process.env.DB_PORT || 3306),
  user: process.env.DB_USER || 'root',
  password: process.env.DB_PASSWORD || '',
  database: process.env.DB_NAME || 'deepinsight',
}
const includeSecrets = process.env.DB_BACKUP_INCLUDE_SECRETS === 'true'

function quoteId(name) {
  return `\`${String(name).replace(/`/g, '``')}\``
}

function sqlValue(value) {
  if (value === null || value === undefined) return 'NULL'
  if (value instanceof Date) return `'${value.toISOString().slice(0, 19).replace('T', ' ')}'`
  if (Buffer.isBuffer(value)) return `X'${value.toString('hex')}'`
  if (typeof value === 'boolean') return value ? '1' : '0'
  if (typeof value === 'number') return Number.isFinite(value) ? String(value) : 'NULL'
  return `'${String(value).replace(/\\/g, '\\\\').replace(/'/g, "\\'")}'`
}

function redactRow(table, row) {
  if (includeSecrets) return row
  if (table !== 'ai_configs') return row
  return { ...row, api_key: null }
}

async function main() {
  const stamp = new Date().toISOString().replace(/[:.]/g, '-')
  const backupDir = path.resolve(__dirname, '..', 'backups')
  const backupFile = path.join(backupDir, `deepinsight-${stamp}.sql`)
  fs.mkdirSync(backupDir, { recursive: true })

  const connection = await mysql.createConnection(config)
  const [tables] = await connection.query('SHOW FULL TABLES WHERE Table_type = "BASE TABLE"')
  const tableKey = `Tables_in_${config.database}`

  let sql = ''
  sql += `-- DeepInsight database backup\n`
  sql += `-- Created: ${new Date().toISOString()}\n`
  sql += `-- Database: ${config.database}\n\n`
  sql += `CREATE DATABASE IF NOT EXISTS ${quoteId(config.database)} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;\n`
  sql += `USE ${quoteId(config.database)};\n\n`
  sql += `SET FOREIGN_KEY_CHECKS=0;\n\n`

  for (const row of tables) {
    const table = row[tableKey]
    const [createRows] = await connection.query(`SHOW CREATE TABLE ${quoteId(table)}`)
    const createSql = createRows[0]['Create Table']

    sql += `DROP TABLE IF EXISTS ${quoteId(table)};\n`
    sql += `${createSql};\n\n`

    const [rows] = await connection.query(`SELECT * FROM ${quoteId(table)}`)
    if (rows.length > 0) {
      const columns = Object.keys(rows[0])
      const columnSql = columns.map(quoteId).join(', ')
      const values = rows.map((dataRow) => {
        const safeRow = redactRow(table, dataRow)
        return `(${columns.map((column) => sqlValue(safeRow[column])).join(', ')})`
      })
      sql += `INSERT INTO ${quoteId(table)} (${columnSql}) VALUES\n${values.join(',\n')};\n\n`
    }
  }

  sql += `SET FOREIGN_KEY_CHECKS=1;\n`
  fs.writeFileSync(backupFile, sql, 'utf8')
  await connection.end()

  console.log(`Backup written: ${backupFile}`)
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
