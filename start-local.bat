@echo off
chcp 65001 >nul
title Chunkoid NG 本地开发服务

echo ======================================================
echo           Chunkoid NG 本地服务启动器
echo ======================================================
echo.
echo 正在启动全栈服务（后端 API + 前台网页托管）...
echo 服务访问地址: http://localhost:3000
echo.

cd /d "%~dp0"
node server/index.js

pause
