@echo off
chcp 65001 >nul
cd /d "%~dp0"

if not exist "deploy\start-all.cmd" (
  echo [ERROR] deploy\start-all.cmd not found.
  pause
  exit /b 1
)

call "deploy\start-all.cmd"
exit /b %errorlevel%
