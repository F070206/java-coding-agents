@echo off
if /i not "%~1"=="--window" (
    start "Java Coding Agent" "%ComSpec%" /k call "%~f0" --window
    exit /b
)
setlocal
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0run-offline.ps1"
set "exitCode=%errorlevel%"
echo.
if not "%exitCode%"=="0" (
    echo.
    echo Startup failed with exit code %exitCode%. Review the error above.
) else (
    echo Application has stopped.
)
exit /b %exitCode%
