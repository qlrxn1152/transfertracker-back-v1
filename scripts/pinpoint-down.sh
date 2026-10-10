#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
PINPOINT_DOCKER_DIR="${PINPOINT_DOCKER_DIR:-/Users/dhoon/Desktop/pinpoint-docker-clean}"
PINPOINT_OVERRIDE_FILE="${PROJECT_ROOT}/infra/pinpoint/docker-compose.transfer-tracker.yml"

if [ ! -f "${PINPOINT_DOCKER_DIR}/docker-compose.yml" ]; then
  echo "Pinpoint docker-compose.yml not found: ${PINPOINT_DOCKER_DIR}/docker-compose.yml" >&2
  exit 1
fi

cd "${PINPOINT_DOCKER_DIR}"

WEB_SERVER_PORT=18080 \
docker compose \
  -f docker-compose.yml \
  -f "${PINPOINT_OVERRIDE_FILE}" \
  down "$@"
