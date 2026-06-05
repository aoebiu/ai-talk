#!/bin/bash
# Dialoger AI Server — shutdown script
# Sends SIGTERM to the server process; force-kills after 30 s if needed.

DIALOGER_AI_HOME=$(cd "$(dirname "$0")/.." && pwd)
PID_FILE="$DIALOGER_AI_HOME/dialoger-ai.pid"

if [ ! -f "$PID_FILE" ]; then
    echo "PID file not found ($PID_FILE). Is Dialoger AI Server running?"
    exit 1
fi

PID=$(cat "$PID_FILE")

if ! kill -0 "$PID" 2>/dev/null; then
    echo "Process $PID is not running. Cleaning up PID file."
    rm -f "$PID_FILE"
    exit 0
fi

echo "Stopping Dialoger AI Server (PID: $PID)..."
kill "$PID"

# Wait up to 30 seconds for a clean exit
for i in $(seq 1 30); do
    if ! kill -0 "$PID" 2>/dev/null; then
        break
    fi
    echo "Waiting for shutdown... ($i/30)"
    sleep 1
done

if kill -0 "$PID" 2>/dev/null; then
    echo "Process did not exit cleanly after 30 s — force killing."
    kill -9 "$PID"
fi

rm -f "$PID_FILE"
echo "Dialoger AI Server stopped."
