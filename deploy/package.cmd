@echo off
chcp 65001 >nul
echo ========================================
echo  DeepInsight 项目打包
echo ========================================
echo.

set "JAVA_HOME_CANDIDATE=%DEEPINSIGHT_JAVA_HOME%"
if "%JAVA_HOME_CANDIDATE%"=="" if exist "D:\jdk\java21\bin\java.exe" set "JAVA_HOME_CANDIDATE=D:\jdk\java21"
if "%JAVA_HOME_CANDIDATE%"=="" (
    echo 未找到 JDK 21。请安装 JDK 21，或设置 DEEPINSIGHT_JAVA_HOME。
    goto :end
)
set "JAVA_HOME=%JAVA_HOME_CANDIDATE%"
set "PATH=%JAVA_HOME_CANDIDATE%\bin;%PATH%"

REM 1. 导出数据库
echo [1/4] 导出 MySQL 数据库...
node export-db.cjs
if %errorlevel% neq 0 (
    echo 数据库导出失败！请确认MySQL正在运行。
    goto :end
)

REM 2. 编译后端
echo.
echo [2/4] 编译后端 JAR...
cd ..\backend
call mvnw.cmd clean package -DskipTests -q
if %errorlevel% neq 0 (
    echo 后端编译失败！
    goto :end
)
copy target\backend-0.0.1-SNAPSHOT.jar ..\deploy\backend.jar >nul
cd ..\deploy

REM 3. 编译前端
echo.
echo [3/4] 编译前端...
cd ..
call npm run build
if %errorlevel% neq 0 (
    echo 前端编译失败！
    goto :end
)
robocopy dist deploy\dist /MIR >nul
if %errorlevel% geq 8 (
    echo 前端静态文件同步失败！
    goto :end
)
cd deploy

REM 4. 收集配置
echo.
echo [4/4] 拷贝配置文件...
copy ..\backend\src\main\resources\application.yml application.yml >nul
copy ..\.env.local .env >nul 2>nul

echo.
echo ========================================
echo  打包完成！
echo ========================================
echo.
echo 输出目录: deploy\
echo   - backend.jar          后端
echo   - dist\                前端静态文件
echo   - database.sql         数据库(默认已脱敏 ai_configs.api_key)
echo   - application.yml      后端配置
echo   - start-all.cmd        一键启动脚本
echo.
echo 部署步骤:
echo   1. 在目标机器上安装 MySQL 和 Redis
echo   2. 配置 deploy\.env 或系统环境变量 DB_PASSWORD / JWT_SECRET
echo   3. 导入数据库: mysql -u %%DB_USER%% -p ^< database.sql
echo   4. 启动后端: java -jar backend.jar
echo   5. 配置 nginx 指向 dist\ 目录，或使用 start-all.cmd 同时启动
echo.

:end
pause
