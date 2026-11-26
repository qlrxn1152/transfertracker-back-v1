#!/usr/bin/env bash

# Creates immutable k6 report artifacts without changing the test scenario itself.
# Usage: ./performance/run-k6.sh [scenario]

set -uo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SCENARIO="${1:-posts-list}"
K6_BIN="${K6_BIN:-k6}"
BASE_URL="${BASE_URL:-http://localhost:8081}"
PERF_ENVIRONMENT="${PERF_ENVIRONMENT:-performance}"
REPORT_SCENARIO="${PERF_SCENARIO:-$SCENARIO}"
TEAM_ID="${TEAM_ID:-}"
TEAM_POST_COUNT="${TEAM_POST_COUNT:-}"
MIXED_TEAM_IDS="${MIXED_TEAM_IDS:-}"
PAGE="${PAGE:-0}"
RPS="${RPS:-50}"
DURATION="${DURATION:-1m}"
PINPOINT_AGENT="${PERF_PINPOINT_AGENT:-unknown}"
AUTH_TOKEN="${AUTH_TOKEN:-}"
PLAYER_ID="${PLAYER_ID:-}"
POST_ID="${POST_ID:-}"
TRANSFER_ID="${TRANSFER_ID:-}"
JOURNALIST="${JOURNALIST:-}"
KEYWORD="${KEYWORD:-}"
LEAGUE_CODE="${LEAGUE_CODE:-}"
PRE_ALLOCATED_VUS="${PRE_ALLOCATED_VUS:-50}"
MAX_VUS="${MAX_VUS:-200}"
PERF_SUITE_MANIFEST="${PERF_SUITE_MANIFEST:-}"
API_CASE=""

case "$SCENARIO" in
  posts-list)
    # Keep the legacy script available for direct k6 invocation, but use the
    # common implementation here so wrapper executions accept the standard
    # load environment variables and validate the paged response shape.
    TEST_SCRIPT_RELATIVE="performance/scripts/read-api.js"
    API_CASE="posts-list"
    ;;
  team-posts)
    TEST_SCRIPT_RELATIVE="performance/scripts/team-posts.js"
    ;;
  post-journalist|post-player|post-detail|post-sources|players-list|players-keyword|players-league|players-team|player-detail|teams-list|teams-league|team-detail|team-info|team-players|transfers-list|transfers-keyword|transfers-league|transfers-team-filter|team-transfers|player-transfers|transfer-detail|member-me|member-favorite-teams|member-favorite-players|member-favorite-player-detail|member-followed-journalists|member-journalist-posts|member-journalists-posts)
    TEST_SCRIPT_RELATIVE="performance/scripts/read-api.js"
    API_CASE="$SCENARIO"
    ;;
  mixed-read)
    TEST_SCRIPT_RELATIVE="performance/scripts/mixed/read-traffic.js"
    ;;
  post|player|team|transfer|member|all-read)
    exec "$PROJECT_ROOT/performance/run-suite.sh" "$SCENARIO"
    ;;
  *)
    echo "지원하지 않는 성능 테스트 시나리오입니다: $SCENARIO" >&2
    echo "현재 지원: posts-list, team-posts, post-*, players-*, teams-*, transfers-*, member-*, mixed-read" >&2
    echo "순차 스위트: post, player, team, transfer, member, all-read" >&2
    exit 2
    ;;
esac

json_escape() {
  local value="$1"
  value=${value//\\/\\\\}
  value=${value//\"/\\\"}
  value=${value//$'\n'/\\n}
  value=${value//$'\r'/\\r}
  value=${value//$'\t'/\\t}
  printf '%s' "$value"
}

now_kst() {
  TZ=Asia/Seoul date "$1"
}

STARTED_AT="$(now_kst '+%Y-%m-%dT%H:%M:%S%z')"
STARTED_EPOCH="$(date +%s)"
RUN_DATE="$(now_kst '+%Y-%m-%d')"
RUN_TIME="$(now_kst '+%H-%M-%S')"

REPORTS_ROOT="$PROJECT_ROOT/performance/reports"
DATE_DIRECTORY="$REPORTS_ROOT/$RUN_DATE"
mkdir -p "$DATE_DIRECTORY"

RUN_DIRECTORY="$DATE_DIRECTORY/${RUN_TIME}-${SCENARIO}"
suffix=1

while ! mkdir "$RUN_DIRECTORY" 2>/dev/null; do
  RUN_DIRECTORY="$DATE_DIRECTORY/${RUN_TIME}-${SCENARIO}-$(printf '%02d' "$suffix")"
  suffix=$((suffix + 1))
done

RUN_DIRECTORY_RELATIVE="performance/reports/${RUN_DIRECTORY#"$REPORTS_ROOT/"}"
RUN_ID="${RUN_DATE}_${RUN_DIRECTORY##*/}"

GIT_COMMIT=""
GIT_BRANCH=""
GIT_DIRTY="unknown"
if git -C "$PROJECT_ROOT" rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  GIT_COMMIT="$(git -C "$PROJECT_ROOT" rev-parse HEAD 2>/dev/null || true)"
  GIT_BRANCH="$(git -C "$PROJECT_ROOT" symbolic-ref --short -q HEAD 2>/dev/null || true)"
  if [[ -z "$GIT_BRANCH" && -n "$GIT_COMMIT" ]]; then
    GIT_BRANCH="HEAD (detached)"
  fi

  if [[ -n "$(git -C "$PROJECT_ROOT" status --porcelain 2>/dev/null)" ]]; then
    GIT_DIRTY="true"
  else
    GIT_DIRTY="false"
  fi
fi

K6_VERSION="$($K6_BIN version 2>/dev/null || true)"

write_fallback_metadata() {
  local finished_at="$1"
  local duration_seconds="$2"
  local exit_code="$3"
  local status="$4"

  cat > "$RUN_DIRECTORY/metadata.json" <<EOF
{
  "runId": "$(json_escape "$RUN_ID")",
  "startedAt": "$(json_escape "$STARTED_AT")",
  "finishedAt": "$(json_escape "$finished_at")",
  "actualDurationSeconds": $duration_seconds,
  "environment": "$(json_escape "$PERF_ENVIRONMENT")",
  "scenario": "$(json_escape "$REPORT_SCENARIO")",
  "testScript": "$(json_escape "$TEST_SCRIPT_RELATIVE")",
  "baseUrl": "$(json_escape "$BASE_URL")",
  "git": {
    "commitSha": $(if [[ -n "$GIT_COMMIT" ]]; then printf '"%s"' "$(json_escape "$GIT_COMMIT")"; else printf 'null'; fi),
    "branch": $(if [[ -n "$GIT_BRANCH" ]]; then printf '"%s"' "$(json_escape "$GIT_BRANCH")"; else printf 'null'; fi),
    "dirty": $(if [[ "$GIT_DIRTY" == "true" || "$GIT_DIRTY" == "false" ]]; then printf '%s' "$GIT_DIRTY"; else printf 'null'; fi)
  },
  "k6Version": $(if [[ -n "$K6_VERSION" ]]; then printf '"%s"' "$(json_escape "$K6_VERSION")"; else printf 'null'; fi),
  "configuration": null,
  "result": {
    "status": "$status",
    "thresholdPassed": null,
    "exitCode": $exit_code
  },
  "note": "k6 summary was not generated; see terminal output for the execution error."
}
EOF
}

"$K6_BIN" run \
  -e "REPORT_DIR=$RUN_DIRECTORY" \
  -e "BASE_URL=$BASE_URL" \
  -e "PERF_RUN_ID=$RUN_ID" \
  -e "PERF_STARTED_AT=$STARTED_AT" \
  -e "PERF_ENVIRONMENT=$PERF_ENVIRONMENT" \
  -e "PERF_SCENARIO=$REPORT_SCENARIO" \
  -e "PERF_TEST_SCRIPT=$TEST_SCRIPT_RELATIVE" \
  -e "PERF_GIT_COMMIT=$GIT_COMMIT" \
  -e "PERF_GIT_BRANCH=$GIT_BRANCH" \
  -e "PERF_GIT_DIRTY=$GIT_DIRTY" \
  -e "PERF_K6_VERSION=$K6_VERSION" \
  -e "TEAM_ID=$TEAM_ID" \
  -e "TEAM_POST_COUNT=$TEAM_POST_COUNT" \
  -e "MIXED_TEAM_IDS=$MIXED_TEAM_IDS" \
  -e "PAGE=$PAGE" \
  -e "RPS=$RPS" \
  -e "DURATION=$DURATION" \
  -e "PERF_PINPOINT_AGENT=$PINPOINT_AGENT" \
  -e "AUTH_TOKEN=$AUTH_TOKEN" \
  -e "API_CASE=$API_CASE" \
  -e "PLAYER_ID=$PLAYER_ID" \
  -e "POST_ID=$POST_ID" \
  -e "TRANSFER_ID=$TRANSFER_ID" \
  -e "JOURNALIST=$JOURNALIST" \
  -e "KEYWORD=$KEYWORD" \
  -e "LEAGUE_CODE=$LEAGUE_CODE" \
  -e "PRE_ALLOCATED_VUS=$PRE_ALLOCATED_VUS" \
  -e "MAX_VUS=$MAX_VUS" \
  "$PROJECT_ROOT/$TEST_SCRIPT_RELATIVE"
K6_EXIT_CODE=$?

FINISHED_AT="$(now_kst '+%Y-%m-%dT%H:%M:%S%z')"
FINISHED_EPOCH="$(date +%s)"
ACTUAL_DURATION_SECONDS=$((FINISHED_EPOCH - STARTED_EPOCH))
METADATA_FILE="$RUN_DIRECTORY/metadata.json"

if [[ "$K6_EXIT_CODE" -eq 0 ]]; then
  RESULT_STATUS="completed"
elif [[ "$K6_EXIT_CODE" -eq 99 ]]; then
  RESULT_STATUS="threshold_failed"
else
  RESULT_STATUS="execution_failed"
fi

if [[ -f "$METADATA_FILE" ]]; then
  FINISHED_AT="$FINISHED_AT" \
  ACTUAL_DURATION_SECONDS="$ACTUAL_DURATION_SECONDS" \
  K6_EXIT_CODE="$K6_EXIT_CODE" \
  RESULT_STATUS="$RESULT_STATUS" \
    perl -0pi -e '
      s/"finishedAt": null/"finishedAt": "$ENV{FINISHED_AT}"/;
      s/"actualDurationSeconds": null/"actualDurationSeconds": $ENV{ACTUAL_DURATION_SECONDS}/;
      s/"status": "pending"/"status": "$ENV{RESULT_STATUS}"/;
      s/"exitCode": null/"exitCode": $ENV{K6_EXIT_CODE}/;
    ' "$METADATA_FILE"
else
  write_fallback_metadata "$FINISHED_AT" "$ACTUAL_DURATION_SECONDS" "$K6_EXIT_CODE" "$RESULT_STATUS"
fi

if grep -q '"thresholdPassed": true' "$METADATA_FILE"; then
  THRESHOLD_RESULT="PASSED"
elif grep -q '"thresholdPassed": false' "$METADATA_FILE"; then
  THRESHOLD_RESULT="FAILED"
else
  THRESHOLD_RESULT="NOT AVAILABLE"
fi

GIT_COMMIT_SHORT="${GIT_COMMIT:0:7}"
if [[ -z "$GIT_COMMIT_SHORT" ]]; then
  GIT_COMMIT_SHORT="unknown"
fi

printf '\n========================================\n'
printf 'VEYRO Performance Test Result\n'
printf '========================================\n\n'
printf 'Scenario      : %s\n' "$REPORT_SCENARIO"
printf 'Environment   : %s\n' "$PERF_ENVIRONMENT"
printf 'Started At    : %s KST\n' "$STARTED_AT"
printf 'Finished At   : %s KST\n' "$FINISHED_AT"
printf 'Git Commit    : %s\n' "$GIT_COMMIT_SHORT"
printf 'Git Branch    : %s\n' "${GIT_BRANCH:-unknown}"
printf 'Git Dirty     : %s\n' "$GIT_DIRTY"
printf 'Threshold     : %s\n' "$THRESHOLD_RESULT"
printf 'Exit Code     : %s\n\n' "$K6_EXIT_CODE"
if [[ "$SCENARIO" == "team-posts" ]]; then
  printf 'Team ID       : %s\n' "${TEAM_ID:-unknown}"
  printf 'Team Posts    : %s\n' "${TEAM_POST_COUNT:-unknown}"
  printf 'Page          : %s\n' "$PAGE"
  printf 'Target RPS    : %s\n' "$RPS"
  printf 'Duration      : %s\n\n' "$DURATION"
fi
printf 'Report Directory:\n%s/\n\n' "$RUN_DIRECTORY_RELATIVE"
printf 'HTML Report:   %s/report.html\n' "$RUN_DIRECTORY_RELATIVE"
printf 'JSON Summary:  %s/summary.json\n' "$RUN_DIRECTORY_RELATIVE"
printf 'Metadata:      %s/metadata.json\n' "$RUN_DIRECTORY_RELATIVE"
printf '\n========================================\n'

if [[ -n "$PERF_SUITE_MANIFEST" ]]; then
  printf '%s\t%s\t%s\n' "$SCENARIO" "$K6_EXIT_CODE" "$RUN_DIRECTORY" >> "$PERF_SUITE_MANIFEST"
fi

exit "$K6_EXIT_CODE"
