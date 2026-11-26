#!/usr/bin/env bash

# One-command helper for the local performance environment. It starts the
# isolated MySQL and, only when needed, a local Spring Boot performance process.

set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SCENARIO="${1:-posts-list}"
BASE_URL="${BASE_URL:-http://localhost:8081}"
COMPOSE=(docker compose -f "$PROJECT_ROOT/performance/docker-compose.yml")
MYSQL=("${COMPOSE[@]}" exec -T mysql-performance mysql -h 127.0.0.1 -uperformance -pperformance_password -D veyro_performance)
RUNTIME_DIR="$PROJECT_ROOT/performance/.runtime"
APP_LOG="$RUNTIME_DIR/application.log"
APP_PID_FILE="$RUNTIME_DIR/application.pid"

fail() {
  printf 'Performance baseline setup failed: %s\n' "$1" >&2
  exit 1
}

application_healthy() {
  curl -fsS --max-time 3 "$BASE_URL/actuator/health" >/dev/null
}

start_application_if_needed() {
  if application_healthy; then
    printf 'Performance application is already healthy at %s.\n' "$BASE_URL"
    return
  fi

  [[ "$BASE_URL" == "http://localhost:8081" ]] \
    || fail "Application is not healthy at $BASE_URL. Automatic startup is available only for the local default URL."

  mkdir -p "$RUNTIME_DIR"
  if [[ -f "$APP_PID_FILE" ]] && kill -0 "$(cat "$APP_PID_FILE")" 2>/dev/null; then
    fail "A previous performance application process is still starting (PID $(cat "$APP_PID_FILE")). Check $APP_LOG."
  fi

  printf 'Starting local Spring Boot performance application...\n'
  (
    cd "$PROJECT_ROOT"
    API_FOOTBALL_KEY=performance-disabled \
    X_API_BEARER_TOKEN=performance-disabled \
    OPENAI_API_KEY=performance-disabled \
    JWT_SECRET=bG9jYWwtdHJhbnNmZXItdHJhY2tlci1qd3Qtc2VjcmV0LWtleS0xMjM0NTY3ODkw \
      ./gradlew bootRun --args='--spring.profiles.active=performance'
  ) >"$APP_LOG" 2>&1 &
  app_pid=$!
  printf '%s\n' "$app_pid" > "$APP_PID_FILE"

  for _ in $(seq 1 90); do
    if application_healthy; then
      printf 'Performance application is ready (PID %s).\n' "$app_pid"
      return
    fi
    if ! kill -0 "$app_pid" 2>/dev/null; then
      tail -n 30 "$APP_LOG" >&2 || true
      fail 'The performance application stopped during startup.'
    fi
    sleep 1
  done

  tail -n 30 "$APP_LOG" >&2 || true
  fail "The performance application was not healthy within 90 seconds. Check $APP_LOG."
}

printf 'Preparing VEYRO local performance environment...\n'
"${COMPOSE[@]}" up -d >/dev/null

for _ in $(seq 1 60); do
  if "${COMPOSE[@]}" exec -T mysql-performance mysqladmin ping -h 127.0.0.1 -uperformance -pperformance_password --silent >/dev/null 2>&1; then
    break
  fi
  sleep 1
done

"${COMPOSE[@]}" exec -T mysql-performance mysqladmin ping -h 127.0.0.1 -uperformance -pperformance_password --silent >/dev/null 2>&1 \
  || fail 'MySQL did not become ready within 60 seconds.'

start_application_if_needed

post_count="$("${MYSQL[@]}" -N -e 'SELECT COUNT(*) FROM post;' 2>/dev/null)" \
  || fail 'The performance schema does not exist yet. Start the application with the performance profile once so Flyway can create it.'

if [[ "$post_count" == "0" ]]; then
  printf 'Seeding 1,000,000 posts for the first time...\n'
  "${MYSQL[@]}" < "$PROJECT_ROOT/performance/seed/seed-posts.sql"
  "${MYSQL[@]}" < "$PROJECT_ROOT/performance/seed/seed-posts-extend.sql"
elif [[ "$post_count" != "1000000" ]]; then
  fail "Expected either 0 or 1000000 posts, found $post_count. Refusing to modify a partially seeded database."
fi

printf 'Ensuring team, player, relation, and transfer fixtures...\n'
"${MYSQL[@]}" < "$PROJECT_ROOT/performance/seed/seed-team-posts.sql"
"${MYSQL[@]}" < "$PROJECT_ROOT/performance/seed/seed-api-read-fixtures.sql" >/dev/null

TEAM_ID="$("${MYSQL[@]}" -N -e 'SELECT team_id FROM team WHERE api_football_id = 990000001;')"
PLAYER_ID="$("${MYSQL[@]}" -N -e 'SELECT player_id FROM player WHERE api_football_id = 991000001;')"
POST_ID="$("${MYSQL[@]}" -N -e "SELECT post_id FROM post WHERE external_post_id = 'PERF_POST_1';")"
TRANSFER_ID="$("${MYSQL[@]}" -N -e "SELECT transfer_id FROM transfer WHERE player_id = $PLAYER_ID AND transfer_date = '2026-07-01';")"

[[ -n "$TEAM_ID" && -n "$PLAYER_ID" && -n "$POST_ID" && -n "$TRANSFER_ID" ]] \
  || fail 'Could not resolve one or more performance fixture IDs.'

printf 'Fixtures ready: team=%s, player=%s, post=%s, transfer=%s\n' "$TEAM_ID" "$PLAYER_ID" "$POST_ID" "$TRANSFER_ID"
printf 'Running scenario: %s\n\n' "$SCENARIO"

exec env \
  BASE_URL="$BASE_URL" \
  TEAM_ID="$TEAM_ID" \
  PLAYER_ID="$PLAYER_ID" \
  POST_ID="$POST_ID" \
  TRANSFER_ID="$TRANSFER_ID" \
  TEAM_POST_COUNT=700000 \
  "$PROJECT_ROOT/performance/run-k6.sh" "$SCENARIO"
