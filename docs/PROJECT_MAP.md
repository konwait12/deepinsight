# DeepInsight 项目文件地图

这份文件用于快速分辨项目根目录里的东西。日常开发优先看 `src/`、`backend/`、`sdk/`、`docs/`，其他多数是构建、依赖、部署或临时产物。

## 根目录

- `src/`：前端源码，Vue 页面、组件、路由、状态、API 调用都在这里。
- `backend/`：后端源码，Spring Boot 接口、服务、实体、数据库仓库和配置都在这里。
- `sdk/`：Python SDK，用于在训练脚本里写入 DeepInsight 可解析的训练日志。
- `docs/`：项目说明、架构说明、文件地图等文档。
- `scripts/`：本地开发启动脚本，例如启动前端、后端、打包后端。
- `deploy/`：部署包相关脚本和配置。
- `artifacts/`：截图、浏览器测试输出、清理归档等临时产物，不是源码。
- `backups/`：数据库或项目备份，不参与日常代码开发。
- `dist/`：前端构建产物，可重新生成。
- `node_modules/`：前端依赖，可通过 `npm install` 重新生成。
- `.git/`：Git 仓库数据，不要手动修改。
- `.claude/`：本地 AI/工具配置痕迹，不属于业务代码。

## 根目录文件

- `package.json`：前端依赖和常用 npm 命令。
- `package-lock.json`：依赖锁定文件。
- `vite.config.ts`：Vite 前端构建和开发服务器配置。
- `tsconfig.json`：TypeScript 配置。
- `tailwind.config.ts`：Tailwind 配置。
- `index.html`：Vite 前端 HTML 入口。
- `README.md`：项目简短说明。
- `项目开发文档.md`：中文交接/开发记录。
- `.env.example`：环境变量示例，可以复制成 `.env.local`。
- `.env.local`：本地环境变量，包含私密配置，不要提交。
- `start.bat` / `stop.bat`：Windows 快捷启动/停止脚本。

## 前端 `src/`

- `src/main.ts`：前端应用入口。
- `src/App.vue`：全局外壳，负责背景、导航、悬浮助手、全局交互效果。
- `src/router/`：路由表和登录拦截。
- `src/api/`：前端 HTTP 请求封装。
- `src/stores/`：Pinia 全局状态，例如登录和主题。
- `src/views/`：页面级组件，对应路由页面。
- `src/components/`：复用组件。
- `src/layout/`：主布局和后台布局。
- `src/styles/`、`src/index.css`：全局样式和设计变量。
- `src/types/`：TypeScript 类型。
- `src/utils/`：纯工具函数。
- `src/constants/`：常量、导航配置、接口路径。

## 后端 `backend/`

- `backend/pom.xml`：Maven 依赖和 Java 版本配置。
- `backend/src/main/java/com/deepinsight/backend/controller/`：REST API 入口。
- `backend/src/main/java/com/deepinsight/backend/service/`：业务逻辑。
- `backend/src/main/java/com/deepinsight/backend/repository/`：数据库访问层。
- `backend/src/main/java/com/deepinsight/backend/entity/`：数据库实体。
- `backend/src/main/java/com/deepinsight/backend/security/`：JWT、登录鉴权、安全过滤器。
- `backend/src/main/java/com/deepinsight/backend/config/`：Spring 配置。
- `backend/src/main/java/com/deepinsight/backend/common/`：统一返回、异常处理、常量。
- `backend/src/main/resources/application.yml`：后端运行配置。
- `backend/src/main/resources/db/`：数据库建表和迁移 SQL。

## SDK `sdk/`

- `sdk/deepinsight_sdk/log_writer.py`：训练日志写入主接口。
- `sdk/deepinsight_sdk/records.py`：日志记录格式定义。
- `sdk/deepinsight_sdk/log_reader.py`：读取日志记录。
- `sdk/deepinsight_sdk/cli/`：命令行入口。
- `sdk/tests/`：SDK 测试。

## 已收纳的临时文件

根目录之前堆积的浏览器临时目录、运行日志、截图和探测文件已移动到：

`artifacts/root-cleanup-2026-05-27/`

里面按用途分为：

- `browser-temp/`：Playwright/Chrome/CDP 临时目录。
- `logs/`：前后端启动、探测、构建日志。
- `screenshots/`：历史截图和视觉对比图。
- `probes-notes/`：探测 marker、旧代码快照和临时文本。
- `runtime/`：本地运行产生的数据文件。

这些文件没有删除，只是归档。确认不再需要后，可以整体删除 `artifacts/root-cleanup-2026-05-27/`。
