# 本地重新部署流程

以后需要重新部署项目时，直接在项目根目录运行：

```powershell
.\redeploy.bat
```

如果只是重启已经构建好的版本，不想重新打包前后端：

```powershell
.\redeploy.bat -SkipBuild
```

## 脚本会做什么

1. 检查 Java、Node、MySQL、Redis。
2. 默认执行 `npm run lint`、后端 Maven 打包、前端 Vite 构建。
3. 同步构建产物到 `deploy/backend.jar` 和 `deploy/dist`。
4. 停止本机旧服务端口：`8080`、`5173`、`5000`。
5. 启动后端、前端静态服务和 BSARec Flask API。
6. 检查前端、后端、前端代理、BSARec 健康接口和预测接口。

## 常用参数

```powershell
.\redeploy.bat -SkipBuild
```

只重启服务，不重新构建。

```powershell
.\redeploy.bat -SkipLint
```

构建时跳过前端类型检查。

```powershell
.\redeploy.bat -SkipBSARec
```

不启动 BSARec API。

```powershell
.\redeploy.bat -NoStop
```

不主动停止已有端口，适合只想检查启动状态时使用。

## 固定路径和端口

- 前端：`http://localhost:5173`
- 后端：`http://localhost:8080`
- BSARec API：`http://127.0.0.1:5000`
- Java：`D:\software\environment\Java\jdk21\bin\java.exe`
- Python：`C:\TOOLS\python\py311\python.exe`
- BSARec 项目：`C:\Users\Madoka\Desktop\web作业\BSARec-main-api`
- 数据库：`root / 123456`，库名 `deepinsight`

## 日志位置

- 后端标准输出：`deploy/backend.out.log`
- 后端错误日志：`deploy/backend.err.log`
- 前端标准输出：`deploy/frontend.out.log`
- 前端错误日志：`deploy/frontend.err.log`
- BSARec 标准输出：`bsarec-api.out.log`
- BSARec 错误日志：`bsarec-api.err.log`

## 注意

不要再手动跑 `deploy/package.cmd` 做日常部署，它会包含额外的打包和数据库导出流程。日常重新部署统一使用 `redeploy.bat`。
