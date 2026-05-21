@echo off
chcp 65001 >nul

echo DeepInsight - Stop Services
echo.

echo Stopping backend...
taskkill /FI "WINDOWTITLE eq DeepInsight-Backend*" /F 2>nul
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080" ^| findstr "LISTENING" 2^>nul') do (
    taskkill /PID %%a /F 2>nul
)
echo   Backend stopped.

echo Stopping frontend...
taskkill /FI "WINDOWTITLE eq DeepInsight-Frontend*" /F 2>nul
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":3000" ^| findstr "LISTENING" 2^>nul') do (
    taskkill /PID %%a /F 2>nul
)
echo   Frontend stopped.

echo.
echo Done.
timeout /t 2 >nul
