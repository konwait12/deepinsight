# DeepInsight

DeepInsight 是一个深度学习训练与可视化分析平台，覆盖数据管理、模型训练、指标可视化、推理预测、知识记录、社区讨论和 AI 辅助分析。

## 本地运行

前端：

```bash
npm install
npm run dev
```

后端：

```bash
cd backend
.\mvnw.cmd spring-boot:run
```

运行前复制 `.env.example` 为本地 `.env.local`，设置 `DB_PASSWORD`、`JWT_SECRET` 等环境变量。DeepSeek/OpenAI/Gemini 等模型 API Key 只通过后台 AI 配置页写入服务器数据库，不要写入前端环境变量或提交到 Git。
