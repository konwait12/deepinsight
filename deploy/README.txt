DeepInsight deployment package
================================

Requirements:
  - Java 21
  - MySQL 8.0
  - Redis
  - Node.js only when serving the bundled frontend with serve-dist.cjs

Security setup:
  1. Copy deploy\.env.example to deploy\.env, or create matching environment variables.
  2. Set DB_PASSWORD and a strong JWT_SECRET before starting the backend.
  3. Store model provider keys, such as DeepSeek/OpenAI/Gemini keys, through the admin AI config page.
  4. Do not commit deploy\.env, database.sql, backend.jar, dist\, or any local database dump.

Machine-specific paths:
  - start-all.cmd first reads the project root .env.local, then deploy\.env.
  - If java.exe is on PATH, no Java path is needed.
  - Otherwise set DEEPINSIGHT_JAVA_HOME in deploy\.env, for example:
      DEEPINSIGHT_JAVA_HOME=C:\Program Files\Java\jdk-21
  - If Redis is already running, set REDIS_SKIP_START=1.
  - If Redis is not on PATH, set REDIS_EXE to the local redis-server.exe path.

Database import:
  mysql -u <DB_USER> -p < database.sql

Start:
  start-all.cmd

URLs:
  Backend:  http://localhost:8080
  Frontend: http://localhost:5173
