@echo off
chcp 65001 >nul

set "REPO_ROOT=%~dp0.."
set "DEPLOY_DIR=%REPO_ROOT%\deploy"
call :load_env "%REPO_ROOT%\.env.local"
call :load_env "%DEPLOY_DIR%\.env"
set "JAVA_HOME_CANDIDATE=%DEEPINSIGHT_JAVA_HOME%"
if "%JAVA_HOME_CANDIDATE%"=="" if exist "D:\jdk\java21\bin\java.exe" set "JAVA_HOME_CANDIDATE=D:\jdk\java21"
if "%JAVA_HOME_CANDIDATE%"=="" set "JAVA_HOME_CANDIDATE=%JAVA_HOME%"

if not exist "%JAVA_HOME_CANDIDATE%\bin\java.exe" (
  echo No usable Java home found. Install JDK 21 or set DEEPINSIGHT_JAVA_HOME.
  pause
  exit /b 1
)

set "JAVA_HOME=%JAVA_HOME_CANDIDATE%"
set "PATH=%JAVA_HOME%\bin;%PATH%"
cd /d "%DEPLOY_DIR%"

java -version
java -jar backend.jar
pause
exit /b %errorlevel%

:load_env
if not exist "%~1" exit /b 0
for /f "usebackq eol=# tokens=1,* delims==" %%A in ("%~1") do (
  if not "%%A"=="" set "%%A=%%~B"
)
exit /b 0
