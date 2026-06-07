#!/bin/bash
# Dialoger AI Server — shutdown script
# Sends SIGTERM to the server process; force-kills after 30 s if needed.

PID=$(pgrep -f "dialoger-ai-server.jar" 2>/dev/null | head -1)

if [ -z "$PID" ]; then
    echo "Dialoger AI Server is not running."
    exit 0
fi

if ! kill -0 "$PID" 2>/dev/null; then
    echo "Dialoger AI Server is not running."
    exit 0
fi

echo "Stopping Dialoger AI Server..."
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

echo "Dialoger AI Server stopped."
