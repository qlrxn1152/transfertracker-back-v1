#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
PINPOINT_DOCKER_DIR="${PINPOINT_DOCKER_DIR:-/Users/dhoon/Desktop/pinpoint-docker-clean}"
PINPOINT_OVERRIDE_FILE="${PROJECT_ROOT}/infra/pinpoint/docker-compose.transfer-tracker.yml"

if [ ! -f "${PINPOINT_DOCKER_DIR}/docker-compose.yml" ]; then
  echo "Pinpoint docker-compose.yml not found: ${PINPOINT_DOCKER_DIR}/docker-compose.yml" >&2
  echo "Set PINPOINT_DOCKER_DIR to your pinpoint-docker checkout." >&2
  exit 1
fi

cd "${PINPOINT_DOCKER_DIR}"

compose() {
  WEB_SERVER_PORT=18080 docker compose \
    -f docker-compose.yml \
    -f "${PINPOINT_OVERRIDE_FILE}" \
    "$@"
}

wait_for_zookeeper() {
  local service="$1"

  for _ in {1..30}; do
    if compose exec -T "${service}" zkServer.sh status >/dev/null 2>&1; then
      return 0
    fi

    sleep 2
  done

  echo "ZooKeeper did not become ready: ${service}" >&2
  return 1
}

wait_for_hbase_schema() {
  local service="pinpoint-hbase"
  local table="AgentId"
  local ready_pattern="Created table ${table}|Tables already exist"

  for _ in {1..40}; do
    if compose logs --no-log-prefix --tail=260 "${service}" 2>/dev/null | grep -Eq "${ready_pattern}"; then
      return 0
    fi

    sleep 3
  done

  echo "HBase schema did not become ready. Check the pinpoint-hbase logs." >&2
  compose logs --tail=160 "${service}" >&2 || true
  return 1
}

compose up -d zoo1 zoo2 zoo3
wait_for_zookeeper zoo1
wait_for_zookeeper zoo2
wait_for_zookeeper zoo3

compose up -d pinpoint-hbase
compose up -d pinpoint-mysql redis

echo "Waiting for HBase bootstrap. First startup can take several minutes."
wait_for_hbase_schema

compose up -d pinpoint-collector pinpoint-web

echo ""
echo "Pinpoint is starting."
echo "Web UI: http://localhost:18080"
echo "Collector gRPC: localhost:9991, localhost:9992, localhost:9993"
