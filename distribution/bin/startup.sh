#!/bin/bash
# Dialoger AI Server — startup script
# Starts the server as a background process and writes the PID to dialoger-ai.pid

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

JAVA_OPT="${JAVA_OPT} -server"
JAVA_OPT="${JAVA_OPT} -Xms${JVM_XMS:-512m} -Xmx${JVM_XMX:-512m} -Xmn${JVM_XMN:-256m}"
JAVA_OPT="${JAVA_OPT} -Dfile.encoding=UTF-8"
JAVA_OPT="${JAVA_OPT} -Duser.timezone=Asia/Shanghai"
JAVA_OPT="${JAVA_OPT} -Dspring.config.additional-location=file:$DIALOGER_AI_HOME/conf/application.yaml"
JAVA_OPT="${JAVA_OPT} -Dspring.output.ansi.enabled=always"
JAVA_OPT="${JAVA_OPT} -Dspring.main.banner-mode=log"
JAVA_OPT="${JAVA_OPT} -Dlogging.file.name=$DIALOGER_AI_HOME/logs/start.out"

# ── Launch ────────────────────────────────────────────────────────────────────

LOG_DIR="$DIALOGER_AI_HOME/logs"
PID_FILE="$DIALOGER_AI_HOME/dialoger-ai.pid"
START_LOG="$LOG_DIR/start.out"

mkdir -p "$LOG_DIR"

if [ -f "$PID_FILE" ]; then
    EXISTING_PID=$(cat "$PID_FILE")
    if kill -0 "$EXISTING_PID" 2>/dev/null; then
        echo "Dialoger AI Server is already running (PID: $EXISTING_PID)."
        exit 0
    fi
fi

echo "Starting Dialoger AI Server..."
echo "DIALOGER_AI_HOME: $DIALOGER_AI_HOME"
echo ""

nohup "$JAVA" $JAVA_OPT \
    -jar "$DIALOGER_AI_HOME/lib/dialoger-ai-server.jar" \
    > /dev/null 2>&1 &

echo $! > "$PID_FILE"

echo "Dialoger AI Server is starting."
echo "Log file    : $START_LOG"
