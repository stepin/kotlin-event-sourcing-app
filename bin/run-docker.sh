#!/usr/bin/env bash
cd "$(dirname "$0")"
set -xeEuo pipefail

docker run --rm -i \
  -p 8080:8080 \
  -e SPRING_R2DBC_URL=r2dbc:postgresql://172.17.0.1:55000/kotlin-bootstrap-app_dev \
  stepin/kotlin-event-sourcing-app
