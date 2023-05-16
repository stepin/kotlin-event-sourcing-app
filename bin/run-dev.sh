#!/usr/bin/env bash
cd "$(dirname "$0")"
set -xeEuo pipefail

cd ..

set -a
[[ -f .env ]] && source .env
set +a

./gradlew bootRun
