@echo off
chcp 65001 >nul

echo DeepInsight — 停止服务
echo.

:: 根据窗口标题精确停止进程
echo 停止后端...
taskkill /FI "WINDOWTITLE eq DeepInsight-Backend*" /F 2>nul
:: 也尝试通过端口释放
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080" ^| findstr "LISTENING" 2^>nul') do (
    taskkill /PID %%a /F 2>nul
)
echo   后端已停止

echo 停止前端...
taskkill /FI "WINDOWTITLE eq DeepInsight-Frontend*" /F 2>nul
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":3000" ^| findstr "LISTENING" 2^>nul') do (
    taskkill /PID %%a /F 2>nul
)
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5173" ^| findstr "LISTENING" 2^>nul') do (
    taskkill /PID %%a /F 2>nul
)
echo   前端已停止

echo.
echo ✅ 完成
timeout /t 2 >nul
