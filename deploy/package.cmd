@echo off
chcp 65001 >nul
setlocal EnableExtensions

set "DEPLOY_DIR=%~dp0"
for %%I in ("%DEPLOY_DIR%..") do set "REPO_ROOT=%%~fI"

call :load_env "%REPO_ROOT%\.env.local"
call :load_env "%DEPLOY_DIR%.env"

echo ========================================
echo  DeepInsight package
echo ========================================
echo.

call :find_java
if errorlevel 1 goto :end

where node >nul 2>nul
if errorlevel 1 (
  echo [ERROR] Node.js was not found. Install Node.js or add node.exe to PATH.
  goto :end
)

cd /d "%DEPLOY_DIR%"

echo [1/4] Export MySQL database...
node export-db.cjs
if errorlevel 1 (
  echo [ERROR] Database export failed. Check MySQL and deploy\.env settings.
  goto :end
)

echo.
echo [2/4] Build backend jar...
cd /d "%REPO_ROOT%\backend"
call mvnw.cmd clean package -DskipTests -q
if errorlevel 1 (
  echo [ERROR] Backend build failed.
  goto :end
)
copy /Y "target\backend-0.0.1-SNAPSHOT.jar" "%DEPLOY_DIR%backend.jar" >nul

echo.
echo [3/4] Build frontend...
cd /d "%REPO_ROOT%"
call npm run build
if errorlevel 1 (
  echo [ERROR] Frontend build failed.
  goto :end
)
robocopy "dist" "%DEPLOY_DIR%dist" /MIR >nul
if errorlevel 8 (
  echo [ERROR] Frontend files sync failed.
  goto :end
)

echo.
echo [4/4] Copy deployment config...
copy /Y "%REPO_ROOT%\backend\src\main\resources\application.yml" "%DEPLOY_DIR%application.yml" >nul
if not exist "%DEPLOY_DIR%.env" if exist "%REPO_ROOT%\.env.local" (
  copy /Y "%REPO_ROOT%\.env.local" "%DEPLOY_DIR%.env" >nul
)

echo.
echo ========================================
echo  Package complete
echo ========================================
echo  Output dir : %DEPLOY_DIR%
echo  Backend    : backend.jar
echo  Frontend   : dist\
echo  Database   : database.sql
echo  Start      : start-all.cmd
echo.
echo  Before moving to another machine, edit deploy\.env for that machine.
echo ========================================
echo.
pause
exit /b 0

:load_env
if not exist "%~1" exit /b 0
for /f "usebackq eol=# tokens=1,* delims==" %%A in ("%~1") do (
  if not "%%A"=="" set "%%A=%%~B"
)
exit /b 0

:find_java
set "JAVA_CMD="
if defined DEEPINSIGHT_JAVA_HOME if exist "%DEEPINSIGHT_JAVA_HOME%\bin\java.exe" set "JAVA_CMD=%DEEPINSIGHT_JAVA_HOME%\bin\java.exe"
if not defined JAVA_CMD if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
if not defined JAVA_CMD for /f "delims=" %%I in ('where java 2^>nul') do if not defined JAVA_CMD set "JAVA_CMD=%%I"
if not defined JAVA_CMD (
  echo [ERROR] Java was not found.
  echo         Install Java 21, add java.exe to PATH, or set DEEPINSIGHT_JAVA_HOME in deploy\.env.
  exit /b 1
)
"%JAVA_CMD%" --version
exit /b 0

:end
echo.
echo Package did not complete.
pause
exit /b 1
