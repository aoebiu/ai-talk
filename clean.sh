#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

chmod +x mvnw

./mvnw clean

rm -rf ai-talk-ui/node_modules

rm -rf ai-talk-tool/node_modules

echo "Clean complete!"