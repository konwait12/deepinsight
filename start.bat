@echo off
chcp 65001 >nul
title DeepInsight Launcher
cd /d "%~dp0"

echo ========================================
echo   DeepInsight - Start All Services
echo ========================================
echo.

set "JAVA_HOME=%JAVA_HOME%"
if "%JAVA_HOME%"=="" set "JAVA_HOME=D:\jdk\java21"
set "PATH=%JAVA_HOME%\bin;%PATH%"

if "%DB_HOST%"=="" set "DB_HOST=localhost"
if "%DB_PORT%"=="" set "DB_PORT=3306"
if "%DB_NAME%"=="" set "DB_NAME=deepinsight"
if "%DB_USER%"=="" set "DB_USER=root"
if "%DB_PASSWORD%"=="" set "DB_PASSWORD=change-me-local"
if "%REDIS_HOST%"=="" set "REDIS_HOST=localhost"
if "%REDIS_PORT%"=="" set "REDIS_PORT=6379"
if "%SERVER_PORT%"=="" set "SERVER_PORT=8080"
if "%JWT_SECRET%"=="" set "JWT_SECRET=change-me-in-local-env-at-least-32-bytes"

echo [1/3] Checking environment...
if not exist "%JAVA_HOME%\bin\java.exe" (
    echo [ERROR] Java not found at %JAVA_HOME%
    pause
    exit /b 1
)
echo        Java OK

if not exist "node_modules\vite\bin\vite.js" (
    echo [ERROR] node_modules not found. Run npm install first.
    pause
    exit /b 1
)
echo        Node OK

echo.
echo [2/3] Starting backend on port %SERVER_PORT%...
start "DeepInsight-Backend" /min cmd /c "cd /d "%~dp0backend" && call mvnw.cmd spring-boot:run 2>"%~dp0backend.log""

echo        Waiting for backend (up to 60s)...
for /L %%i in (1,1,30) do (
    timeout /t 2 /nobreak >nul
    curl -s -o NUL http://localhost:%SERVER_PORT%/api/v1/auth/test 2>nul
    if not errorlevel 1 goto backend_ok
    echo        ... %%i/30
)

:backend_ok
echo        Backend ready.

echo.
echo [3/3] Starting frontend on port 3000...
start "DeepInsight-Frontend" /min cmd /c "cd /d "%~dp0" && node node_modules\vite\bin\vite.js --port=3000 --host=0.0.0.0 2>"%~dp0vite.log""

timeout /t 3 /nobreak >nul

echo.
echo ========================================
echo   All services started!
echo   Frontend: http://localhost:3000
echo   Backend:  http://localhost:%SERVER_PORT%
echo ========================================
echo.

start http://localhost:3000

echo Done.
pause >nul
