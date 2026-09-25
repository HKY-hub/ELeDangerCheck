@echo off
chcp 65001 >nul
REM ========================================================
REM 电力风险智能交底系统 - 用 VS Code 打开本项目
REM 用法：双击本文件即可
REM ========================================================
setlocal

set "PROJECT_DIR=%~dp0"
if "%PROJECT_DIR:~-1%"=="\" set "PROJECT_DIR=%PROJECT_DIR:~0,-1%"

set "VSCODE_CLI="
for /f "delims=" %%i in ('where code.cmd 2^>nul') do if not defined VSCODE_CLI set "VSCODE_CLI=%%i"
if not defined VSCODE_CLI if exist "%LOCALAPPDATA%\Programs\Microsoft VS Code\bin\code.cmd" set "VSCODE_CLI=%LOCALAPPDATA%\Programs\Microsoft VS Code\bin\code.cmd"
if not defined VSCODE_CLI for /f "delims=" %%i in ('where code 2^>nul') do if not defined VSCODE_CLI set "VSCODE_CLI=%%i"

if not defined VSCODE_CLI (
    echo [错误] 未检测到 VS Code 的 code 命令。
    echo        请先安装 VS Code，或把其 bin 目录加入 PATH。
    pause
    exit /b 1
)

call "%VSCODE_CLI%" "%PROJECT_DIR%"
if errorlevel 1 (
    echo [错误] 启动 VS Code 失败。
    pause
    exit /b 1
)
exit /b 0