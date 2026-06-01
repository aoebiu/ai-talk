#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

chmod +x mvnw

./mvnw clean

rm -rf ui/node_modules

rm -rf tool/node_modules

echo "Clean complete!"