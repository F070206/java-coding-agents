@echo off
if /i not "%~1"=="--window" (
    start "Stop Java Coding Agent" "%ComSpec%" /k call "%~f0" --window
    exit /b
)
setlocal
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0stop.ps1"
if errorlevel 1 (
    echo.
    echo Stop failed. Review the error above.
    exit /b 1
)
