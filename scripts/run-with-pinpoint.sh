#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

PINPOINT_AGENT_DIR="${PINPOINT_AGENT_DIR:-/Users/dhoon/Desktop/pinpoint-agent-3.1.1}"
PINPOINT_AGENT_ID="${PINPOINT_AGENT_ID:-transfer-tracker-local}"
PINPOINT_APPLICATION_NAME="${PINPOINT_APPLICATION_NAME:-transfer-tracker}"
PINPOINT_PROFILE="${PINPOINT_PROFILE:-local}"
PINPOINT_COLLECTOR_IP="${PINPOINT_COLLECTOR_IP:-127.0.0.1}"
PINPOINT_LOG_DIR="${PINPOINT_LOG_DIR:-${PROJECT_ROOT}/infra/pinpoint/logs}"
GRADLE_USER_HOME="${GRADLE_USER_HOME:-${PROJECT_ROOT}/.gradle-home}"

if [ ! -f "${PINPOINT_AGENT_DIR}/pinpoint-bootstrap.jar" ]; then
  echo "Pinpoint agent not found: ${PINPOINT_AGENT_DIR}/pinpoint-bootstrap.jar" >&2
  echo "Set PINPOINT_AGENT_DIR to your extracted pinpoint-agent directory." >&2
  exit 1
fi

cd "${PROJECT_ROOT}"

if [ -f ".env" ]; then
  set -a
  # shellcheck disable=SC1091
  source ".env"
  set +a
fi

mkdir -p "${PINPOINT_LOG_DIR}"

./gradlew bootJar -x test --gradle-user-home "${GRADLE_USER_HOME}"

APP_JAR="$(find "${PROJECT_ROOT}/build/libs" -maxdepth 1 -type f -name "*.jar" ! -name "*plain.jar" | head -n 1)"

if [ -z "${APP_JAR}" ]; then
  echo "Spring Boot jar not found under ${PROJECT_ROOT}/build/libs" >&2
  exit 1
fi

exec java \
  -javaagent:"${PINPOINT_AGENT_DIR}/pinpoint-bootstrap.jar" \
  -Dpinpoint.agentId="${PINPOINT_AGENT_ID}" \
  -Dpinpoint.applicationName="${PINPOINT_APPLICATION_NAME}" \
  -Dpinpoint.profiler.profiles.active="${PINPOINT_PROFILE}" \
  -Dprofiler.transport.grpc.collector.ip="${PINPOINT_COLLECTOR_IP}" \
  -Dpinpoint.log="${PINPOINT_LOG_DIR}" \
  -jar "${APP_JAR}" \
  "$@"
