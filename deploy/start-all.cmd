@echo off
chcp 65001 >nul
title DeepInsight Server

set "DEPLOY_DIR=%~dp0"
call :load_env "%DEPLOY_DIR%\.env"
set "JAVA_HOME_CANDIDATE=%DEEPINSIGHT_JAVA_HOME%"
if "%JAVA_HOME_CANDIDATE%"=="" if exist "D:\jdk\java21\bin\java.exe" set "JAVA_HOME_CANDIDATE=D:\jdk\java21"
if not "%JAVA_HOME_CANDIDATE%"=="" (
  set "JAVA_HOME=%JAVA_HOME_CANDIDATE%"
  set "PATH=%JAVA_HOME_CANDIDATE%\bin;%PATH%"
)
cd /d "%DEPLOY_DIR%"

echo ========================================
echo  DeepInsight - starting services
echo ========================================
echo.

REM Ensure Redis is running
echo [1/3] Starting Redis...
if exist "D:\redis\redis-server.exe" (
  start "DeepInsight-Redis" "D:\redis\redis-server.exe"
  timeout /t 2 >nul
) else (
  echo Redis executable not found at D:\redis\redis-server.exe
)

REM Start Backend
echo [2/3] Starting backend with Java...
java -version
start "DeepInsight-Backend" java -jar backend.jar
timeout /t 8 >nul

REM Start Frontend (simple HTTP server for dist/)
echo [3/3] Starting frontend...
start "DeepInsight-Frontend" node serve-dist.cjs dist 3000

echo.
echo ========================================
echo  Backend:  http://localhost:8080
echo  Frontend: http://localhost:3000
echo ========================================
echo.
pause
exit /b %errorlevel%

:load_env
if not exist "%~1" exit /b 0
for /f "usebackq eol=# tokens=1,* delims==" %%A in ("%~1") do (
  if not "%%A"=="" set "%%A=%%~B"
)
exit /b 0
