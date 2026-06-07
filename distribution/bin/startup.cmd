@echo off
setlocal

rem Dialoger AI Server — startup script (Windows)
rem Launches the server in a new console window.

set "DIALOGER_AI_HOME=%~dp0.."

rem ── Java executable ──────────────────────────────────────────────────────────
if "%JAVA_HOME%" == "" (
    set "JAVA=java"
) else (
    set "JAVA=%JAVA_HOME%\bin\java.exe"
)

rem ── Validate paths ───────────────────────────────────────────────────────────
if not exist "%DIALOGER_AI_HOME%\lib\dialoger-ai-server.jar" (
    echo ERROR: Server JAR not found: %DIALOGER_AI_HOME%\lib\dialoger-ai-server.jar
    exit /b 1
)

if not exist "%DIALOGER_AI_HOME%\conf\application.yaml" (
    echo ERROR: Config file not found: %DIALOGER_AI_HOME%\conf\application.yaml
    exit /b 1
)

rem ── Directories ──────────────────────────────────────────────────────────────
set "LOG_DIR=%DIALOGER_AI_HOME%\logs"
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

rem ── JVM options ──────────────────────────────────────────────────────────────
rem Override DB config via environment:  DB_HOST, DB_PORT, DB_USERNAME, DB_PASSWORD
rem Override ES config via environment:  ES_HOST, ES_PORT, ES_USERNAME, ES_PASSWORD
set JAVA_OPT=-server -Xms512m -Xmx512m -Xmn256m
set JAVA_OPT=%JAVA_OPT% -Dfile.encoding=UTF-8
set JAVA_OPT=%JAVA_OPT% -Dspring.config.additional-location=file:%DIALOGER_AI_HOME%/conf/application.yaml
set JAVA_OPT=%JAVA_OPT% -Dspring.output.ansi.enabled=always
set JAVA_OPT=%JAVA_OPT% -Dspring.main.banner-mode=log
set JAVA_OPT=%JAVA_OPT% -Dlogging.file.name=%LOG_DIR%\start.out

if defined DB_HOST     set "JAVA_OPT=%JAVA_OPT% -Ddatasource.host=%DB_HOST%"
if defined DB_PORT     set "JAVA_OPT=%JAVA_OPT% -Ddatasource.port=%DB_PORT%"
if defined DB_USERNAME set "JAVA_OPT=%JAVA_OPT% -Ddatasource.username=%DB_USERNAME%"
if defined DB_PASSWORD set "JAVA_OPT=%JAVA_OPT% -Ddatasource.password=%DB_PASSWORD%"

if defined ES_HOST     set "JAVA_OPT=%JAVA_OPT% -Des.host=%ES_HOST%"
if defined ES_PORT     set "JAVA_OPT=%JAVA_OPT% -Des.port=%ES_PORT%"
if defined ES_USERNAME set "JAVA_OPT=%JAVA_OPT% -Des.username=%ES_USERNAME%"
if defined ES_PASSWORD set "JAVA_OPT=%JAVA_OPT% -Des.password=%ES_PASSWORD%"

rem ── Launch ────────────────────────────────────────────────────────────────────
echo Starting Dialoger AI Server...
echo DIALOGER_AI_HOME: %DIALOGER_AI_HOME%
echo.

start "Dialoger AI Server" "%JAVA%" %JAVA_OPT% -jar "%DIALOGER_AI_HOME%\lib\dialoger-ai-server.jar"

echo Dialoger AI Server launched. Check %LOG_DIR% for output.
endlocal
