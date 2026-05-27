@echo off
chcp 65001 >nul
setlocal EnableExtensions
title DeepInsight Startup
goto :main

:load_env
if not exist "%~1" exit /b 0
for /f "usebackq eol=# tokens=1,* delims==" %%A in ("%~1") do (
  if not "%%A"=="" set "%%A=%%~B"
)
exit /b 0

:sleep
set "SLEEP_SECONDS=%~1"
if not defined SLEEP_SECONDS set "SLEEP_SECONDS=1"
set /a "PING_COUNT=SLEEP_SECONDS+1" >nul
ping -n %PING_COUNT% 127.0.0.1 >nul
exit /b 0

:is_port_open
set "CHECK_HOST=%~1"
set "CHECK_PORT=%~2"
powershell -NoProfile -ExecutionPolicy Bypass -Command "$client = New-Object Net.Sockets.TcpClient; try { $iar = $client.BeginConnect('%CHECK_HOST%', %CHECK_PORT%, $null, $null); if (-not $iar.AsyncWaitHandle.WaitOne(1000, $false)) { exit 1 }; $client.EndConnect($iar); exit 0 } catch { exit 1 } finally { $client.Close() }" >nul 2>nul
exit /b %errorlevel%

:wait_for_backend
echo Waiting for backend on http://localhost:%SERVER_PORT% ...
for /L %%i in (1,1,45) do (
  curl -fsS "http://localhost:%SERVER_PORT%/api/v1/auth/test" >nul 2>nul
  if not errorlevel 1 exit /b 0
  call :sleep 2
)
exit /b 1

:main
set "DEPLOY_DIR=%~dp0"
for %%I in ("%DEPLOY_DIR%..") do set "REPO_ROOT=%%~fI\"

call :load_env "%REPO_ROOT%.env.local"
call :load_env "%DEPLOY_DIR%.env"

set "JAVA_HOME=D:\software\environment\Java\jdk21"
set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
set "PATH=%JAVA_HOME%\bin;%PATH%"

if not defined DB_HOST set "DB_HOST=localhost"
if not defined DB_PORT set "DB_PORT=3306"
if not defined DB_NAME set "DB_NAME=deepinsight"
if not defined DB_USER set "DB_USER=root"
if not defined DB_PASSWORD set "DB_PASSWORD=123456"
if not defined REDIS_HOST set "REDIS_HOST=localhost"
if not defined REDIS_PORT set "REDIS_PORT=6379"
if not defined SERVER_PORT set "SERVER_PORT=8080"
if not defined BACKEND_PORT set "BACKEND_PORT=%SERVER_PORT%"
if not defined FRONTEND_PORT set "FRONTEND_PORT=5173"
if not defined BACKEND_URL set "BACKEND_URL=http://localhost:%SERVER_PORT%"
if not defined APP_CORS_ALLOWED_ORIGINS set "APP_CORS_ALLOWED_ORIGINS=http://localhost:%FRONTEND_PORT%,http://127.0.0.1:%FRONTEND_PORT%,http://localhost:3000,http://127.0.0.1:3000"
if not defined JWT_SECRET set "JWT_SECRET=RGVlcEluc2lnaHQtSldULVNlY3JldC1LZXktMjAyNS1TZWN1cmUtQXQtTGVhc3QtMzItQnl0ZXMh"
if not defined JWT_EXPIRATION set "JWT_EXPIRATION=86400000"

cd /d "%DEPLOY_DIR%"

echo ========================================
echo  DeepInsight - local deployment
echo ========================================
echo  Java       : %JAVA_CMD%
echo  Backend    : http://localhost:%SERVER_PORT%
echo  Frontend   : http://localhost:%FRONTEND_PORT%
echo  Database   : %DB_USER%@%DB_HOST%:%DB_PORT%/%DB_NAME%
echo  Redis      : %REDIS_HOST%:%REDIS_PORT%
echo ========================================
echo.

if not exist "%JAVA_CMD%" (
  echo [ERROR] Java was not found at:
  echo         %JAVA_CMD%
  echo         Check the JDK path in deploy\start-all.cmd.
  goto :fail
)

where node >nul 2>nul
if errorlevel 1 (
  echo [ERROR] Node.js was not found on PATH.
  echo         Install Node.js or add node.exe to PATH before starting the frontend.
  goto :fail
)

echo [1/5] Runtime check
"%JAVA_CMD%" --version
node --version
echo.

echo [2/5] Dependency ports
call :is_port_open "%DB_HOST%" "%DB_PORT%"
if errorlevel 1 (
  echo [WARN] MySQL is not reachable at %DB_HOST%:%DB_PORT%.
) else (
  echo MySQL is reachable at %DB_HOST%:%DB_PORT%.
)

call :is_port_open "%REDIS_HOST%" "%REDIS_PORT%"
if errorlevel 1 (
  echo [WARN] Redis is not reachable at %REDIS_HOST%:%REDIS_PORT%.
  echo        Start Redis first if login or verification code APIs fail.
) else (
  echo Redis is reachable at %REDIS_HOST%:%REDIS_PORT%.
)
echo.

echo [3/5] Backend
if not exist "%DEPLOY_DIR%backend.jar" (
  echo [ERROR] backend.jar not found in %DEPLOY_DIR%
  goto :fail
)
call :is_port_open "localhost" "%SERVER_PORT%"
if errorlevel 1 (
  if exist "%DEPLOY_DIR%backend.out.log" del "%DEPLOY_DIR%backend.out.log" >nul 2>nul
  if exist "%DEPLOY_DIR%backend.err.log" del "%DEPLOY_DIR%backend.err.log" >nul 2>nul
  start "DeepInsight-Backend" /min cmd /c ""%JAVA_CMD%" -jar "%DEPLOY_DIR%backend.jar" 1^> "%DEPLOY_DIR%backend.out.log" 2^> "%DEPLOY_DIR%backend.err.log""
) else (
  echo Port %SERVER_PORT% is already in use; trying to use the running backend.
)

call :wait_for_backend
if errorlevel 1 (
  echo [ERROR] Backend did not answer within 90 seconds.
  echo         See deploy\backend.out.log and deploy\backend.err.log.
  goto :fail
)
echo Backend is ready.
echo.

echo [4/5] Frontend
if not exist "%DEPLOY_DIR%dist\index.html" (
  echo [ERROR] dist\index.html not found in %DEPLOY_DIR%
  goto :fail
)
call :is_port_open "localhost" "%FRONTEND_PORT%"
if errorlevel 1 (
  if exist "%DEPLOY_DIR%frontend.out.log" del "%DEPLOY_DIR%frontend.out.log" >nul 2>nul
  if exist "%DEPLOY_DIR%frontend.err.log" del "%DEPLOY_DIR%frontend.err.log" >nul 2>nul
  start "DeepInsight-Frontend" /min cmd /c ""node" "%DEPLOY_DIR%serve-dist.cjs" "%DEPLOY_DIR%dist" %FRONTEND_PORT% 1^> "%DEPLOY_DIR%frontend.out.log" 2^> "%DEPLOY_DIR%frontend.err.log""
) else (
  echo Port %FRONTEND_PORT% is already in use; trying to use the running frontend.
)
call :sleep 2
echo.

echo [5/5] Quick checks
curl -fsS "http://localhost:%SERVER_PORT%/api/v1/auth/test" >nul 2>nul
if errorlevel 1 (
  echo [WARN] Backend health check failed.
) else (
  echo Backend health check passed.
)

curl -fsS "http://localhost:%FRONTEND_PORT%/" >nul 2>nul
if errorlevel 1 (
  echo [WARN] Frontend page check failed.
) else (
  echo Frontend page check passed.
)

curl -fsS "http://localhost:%FRONTEND_PORT%/api/v1/auth/test" >nul 2>nul
if errorlevel 1 (
  echo [WARN] Frontend API proxy check failed.
) else (
  echo Frontend API proxy check passed.
)

echo.
echo ========================================
echo  Started.
echo  Frontend: http://localhost:%FRONTEND_PORT%
echo  Backend : http://localhost:%SERVER_PORT%
echo  Admin   : admin / ChangeMe-Admin-2026!
echo ========================================
echo.
pause
exit /b 0

:fail
echo.
echo Startup did not complete.
pause
exit /b 1
