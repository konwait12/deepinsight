const m = require('mysql2/promise');
const fs = require('fs');
const path = require('path');

function loadEnv(file) {
  if (!fs.existsSync(file)) return;
  for (const rawLine of fs.readFileSync(file, 'utf8').split(/\r?\n/)) {
    const line = rawLine.trim();
    if (!line || line.startsWith('#') || !line.includes('=')) continue;
    const index = line.indexOf('=');
    const key = line.slice(0, index).trim();
    const value = line.slice(index + 1).trim().replace(/^['"]|['"]$/g, '');
    if (/^[A-Za-z_][A-Za-z0-9_]*$/.test(key) && process.env[key] === undefined) {
      process.env[key] = value;
    }
  }
}

loadEnv(path.resolve(__dirname, '..', '.env.local'));
loadEnv(path.resolve(__dirname, '.env'));

const config = {
  host: process.env.DB_HOST || 'localhost',
  port: Number(process.env.DB_PORT || 3306),
  user: process.env.DB_USER || 'root',
  password: process.env.DB_PASSWORD || '',
  database: process.env.DB_NAME || 'deepinsight',
};
const includeSecrets = process.env.DB_EXPORT_INCLUDE_SECRETS === 'true';

function redactRow(table, row) {
  if (includeSecrets) return row;
  if (table !== 'ai_configs') return row;
  return { ...row, api_key: null };
}

(async () => {
  const c = await m.createConnection(config);
  const [t] = await c.execute('SHOW TABLES');
  let sql = '-- DeepInsight Database Export\n-- ' + new Date().toISOString() + '\n\n';
  sql += 'CREATE DATABASE IF NOT EXISTS `' + config.database.replace(/`/g, '``') + '` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;\n';
  sql += 'USE `' + config.database.replace(/`/g, '``') + '`;\n\n';

  for (const r of t) {
    const table = Object.values(r)[0];
    const [create] = await c.execute('SHOW CREATE TABLE ' + table);
    sql += create[0]['Create Table'] + ';\n\n';

    const [rows] = await c.execute('SELECT * FROM ' + table);
    if (rows.length > 0) {
      const cols = Object.keys(rows[0]);
      const vals = rows.map(row => {
        return '(' + cols.map(c => {
          const safeRow = redactRow(table, row);
          const v = safeRow[c];
          if (v === null) return 'NULL';
          if (typeof v === 'string') return "'" + v.replace(/\\/g, '\\\\').replace(/'/g, "\\'") + "'";
          if (v instanceof Date) return "'" + v.toISOString().slice(0, 19).replace('T', ' ') + "'";
          if (typeof v === 'boolean') return v ? '1' : '0';
          return v;
        }).join(',') + ')';
      }).join(',\n');
      sql += 'INSERT INTO ' + table + ' (' + cols.join(',') + ') VALUES\n' + vals + ';\n\n';
    }
  }

  fs.mkdirSync(__dirname, { recursive: true });
  fs.writeFileSync(__dirname + '/database.sql', sql);
  console.log('Exported database.sql (' + (sql.length / 1024).toFixed(0) + ' KB)');
  await c.end();
})();
