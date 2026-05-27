@echo off
chcp 65001 >nul

set "REPO_ROOT=%~dp0.."
set "DEPLOY_DIR=%REPO_ROOT%\deploy"
call :load_env "%REPO_ROOT%\.env.local"
call :load_env "%DEPLOY_DIR%\.env"
set "JAVA_CMD="
if defined DEEPINSIGHT_JAVA_HOME if exist "%DEEPINSIGHT_JAVA_HOME%\bin\java.exe" set "JAVA_CMD=%DEEPINSIGHT_JAVA_HOME%\bin\java.exe"
if not defined JAVA_CMD if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
if not defined JAVA_CMD for /f "delims=" %%I in ('where java 2^>nul') do if not defined JAVA_CMD set "JAVA_CMD=%%I"

if not defined JAVA_CMD (
  echo No usable Java runtime found. Install Java 21, add java.exe to PATH, or set DEEPINSIGHT_JAVA_HOME.
  pause
  exit /b 1
)

cd /d "%DEPLOY_DIR%"

"%JAVA_CMD%" --version
"%JAVA_CMD%" -jar backend.jar
pause
exit /b %errorlevel%

:load_env
if not exist "%~1" exit /b 0
for /f "usebackq eol=# tokens=1,* delims==" %%A in ("%~1") do (
  if not "%%A"=="" set "%%A=%%~B"
)
exit /b 0
