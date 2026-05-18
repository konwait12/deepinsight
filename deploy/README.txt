DeepInsight deployment package
================================

Requirements:
  - Java 21
  - MySQL 8.0
  - Redis
  - Node.js only when serving the bundled frontend with serve-dist.cjs

Security setup:
  1. Copy .env.example to deploy\.env or create environment variables.
  2. Set DB_PASSWORD and a strong JWT_SECRET before starting the backend.
  3. Store model provider keys, such as DeepSeek/OpenAI/Gemini keys, through the admin AI config page.
  4. Do not commit deploy\.env, database.sql, backend.jar, dist\, or any local database dump.

Database import:
  mysql -u <DB_USER> -p < database.sql

Start:
  start-all.cmd

URLs:
  Backend:  http://localhost:8080
  Frontend: http://localhost:3000
