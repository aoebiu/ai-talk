@echo off
setlocal

rem Dialoger AI Server — shutdown script (Windows)
rem Terminates the server process listening on port 7900.

echo Stopping Dialoger AI Server on port 7900...

set FOUND=0
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":7900 " ^| findstr "LISTENING"') do (
    taskkill /F /PID %%a
    set FOUND=1
)

if "%FOUND%"=="0" (
    echo No Dialoger AI Server process found on port 7900.
) else (
    echo Dialoger AI Server stopped.
)

endlocal
