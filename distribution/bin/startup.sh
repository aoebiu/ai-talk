#!/bin/bash
# Dialoger AI Server — startup script
# Starts the server as a background process.

# Resolve the installation root (one level above bin/)
DIALOGER_AI_HOME=$(cd "$(dirname "$0")/.." && pwd)

# ── Prerequisites ─────────────────────────────────────────────────────────────

if [ ! -f "$DIALOGER_AI_HOME/conf/application.yaml" ]; then
    echo "ERROR: Config file not found: $DIALOGER_AI_HOME/conf/application.yaml"
    exit 1
fi

if [ ! -f "$DIALOGER_AI_HOME/lib/dialoger-ai-server.jar" ]; then
    echo "ERROR: Server JAR not found: $DIALOGER_AI_HOME/lib/dialoger-ai-server.jar"
    exit 1
fi

# ── Java executable ───────────────────────────────────────────────────────────

if [ -n "$JAVA_HOME" ]; then
    JAVA="$JAVA_HOME/bin/java"
else
    JAVA=$(command -v java)
fi

if [ -z "$JAVA" ]; then
    echo "ERROR: Java not found. Set JAVA_HOME or add java to PATH."
    exit 1
fi

# ── JVM options ───────────────────────────────────────────────────────────────
# Override heap sizes via environment: JVM_XMS, JVM_XMX, JVM_XMN
# Override DB config via environment:  DB_HOST, DB_PORT, DB_USERNAME, DB_PASSWORD
#   (maps to server placeholders ${datasource.host}, ${datasource.port}, etc.)
# Override ES config via environment:  ES_HOST, ES_PORT, ES_USERNAME, ES_PASSWORD
#   (maps to server placeholders ${es.host}, ${es.port}, etc.)

JAVA_OPT="${JAVA_OPT} -server"
JAVA_OPT="${JAVA_OPT} -Xms${JVM_XMS:-512m} -Xmx${JVM_XMX:-512m} -Xmn${JVM_XMN:-256m}"
JAVA_OPT="${JAVA_OPT} -Dfile.encoding=UTF-8"
JAVA_OPT="${JAVA_OPT} -Duser.timezone=Asia/Shanghai"
JAVA_OPT="${JAVA_OPT} -Dspring.config.additional-location=file:$DIALOGER_AI_HOME/conf/application.yaml"
JAVA_OPT="${JAVA_OPT} -Dspring.output.ansi.enabled=always"
JAVA_OPT="${JAVA_OPT} -Dspring.main.banner-mode=log"
JAVA_OPT="${JAVA_OPT} -Dlogging.file.name=$DIALOGER_AI_HOME/logs/start.out"

[ -n "$DB_HOST" ]     && JAVA_OPT="${JAVA_OPT} -Ddatasource.host=$DB_HOST"
[ -n "$DB_PORT" ]     && JAVA_OPT="${JAVA_OPT} -Ddatasource.port=$DB_PORT"
[ -n "$DB_USERNAME" ] && JAVA_OPT="${JAVA_OPT} -Ddatasource.username=$DB_USERNAME"
[ -n "$DB_PASSWORD" ] && JAVA_OPT="${JAVA_OPT} -Ddatasource.password=$DB_PASSWORD"

[ -n "$ES_HOST" ]     && JAVA_OPT="${JAVA_OPT} -Des.host=$ES_HOST"
[ -n "$ES_PORT" ]     && JAVA_OPT="${JAVA_OPT} -Des.port=$ES_PORT"
[ -n "$ES_USERNAME" ] && JAVA_OPT="${JAVA_OPT} -Des.username=$ES_USERNAME"
[ -n "$ES_PASSWORD" ] && JAVA_OPT="${JAVA_OPT} -Des.password=$ES_PASSWORD"

# ── Launch ────────────────────────────────────────────────────────────────────

LOG_DIR="$DIALOGER_AI_HOME/logs"
START_LOG="$LOG_DIR/start.out"

mkdir -p "$LOG_DIR"

EXISTING_PID=$(pgrep -f "dialoger-ai-server.jar" 2>/dev/null | head -1)
if [ -n "$EXISTING_PID" ] && kill -0 "$EXISTING_PID" 2>/dev/null; then
    echo "Dialoger AI Server is already running."
    exit 0
fi

echo "Starting Dialoger AI Server..."
echo "DIALOGER_AI_HOME: $DIALOGER_AI_HOME"
echo ""

nohup "$JAVA" $JAVA_OPT \
    -jar "$DIALOGER_AI_HOME/lib/dialoger-ai-server.jar" \
    > /dev/null 2>&1 &

echo "Dialoger AI Server is starting."
echo "Log file    : $START_LOG"
