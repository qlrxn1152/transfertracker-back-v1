#!/usr/bin/env bash

# Runs independent read-only k6 scenarios sequentially. Child failures are
# recorded and do not stop later baseline measurements.
set -uo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SUITE="${1:-all-read}"
REPORTS_ROOT="$PROJECT_ROOT/performance/reports"

now_kst() {
  TZ=Asia/Seoul date "$1"
}

case "$SUITE" in
  post)
    SCENARIOS=(posts-list team-posts post-journalist post-player post-detail post-sources)
    ;;
  player)
    SCENARIOS=(players-list players-keyword players-league players-team player-detail)
    ;;
  team)
    SCENARIOS=(teams-list teams-league team-detail team-info team-players)
    ;;
  transfer)
    SCENARIOS=(transfers-list transfers-keyword transfers-league transfers-team-filter team-transfers player-transfers transfer-detail)
    ;;
  member)
    SCENARIOS=(member-me member-favorite-teams member-favorite-players member-favorite-player-detail member-followed-journalists member-journalist-posts member-journalists-posts)
    ;;
  all-read)
    # Only public, side-effect-free endpoints. Member scenarios are separate
    # because they require an explicitly supplied local JWT.
    SCENARIOS=(posts-list team-posts post-journalist post-player post-detail post-sources players-list players-keyword players-league players-team player-detail teams-list teams-league team-detail team-info team-players transfers-list transfers-keyword transfers-league transfers-team-filter team-transfers player-transfers transfer-detail)
    ;;
  *)
    echo "지원하지 않는 순차 스위트입니다: $SUITE" >&2
    echo "현재 지원: post, player, team, transfer, member, all-read" >&2
    exit 2
    ;;
esac

RUN_DATE="$(now_kst '+%Y-%m-%d')"
RUN_TIME="$(now_kst '+%H-%M-%S')"
mkdir -p "$REPORTS_ROOT/$RUN_DATE"
BATCH_ROOT="$REPORTS_ROOT/$RUN_DATE/${RUN_TIME}-${SUITE}"
suffix=1
while ! mkdir "$BATCH_ROOT" 2>/dev/null; do
  BATCH_ROOT="$REPORTS_ROOT/$RUN_DATE/${RUN_TIME}-${SUITE}-$(printf '%02d' "$suffix")"
  suffix=$((suffix + 1))
done

MANIFEST="$BATCH_ROOT/manifest.tsv"
printf 'scenario\texitCode\treportDirectory\n' > "$MANIFEST"

printf '\n========================================\n'
printf 'VEYRO Performance Suite: %s\n' "$SUITE"
printf 'Scenarios: %s\n' "${#SCENARIOS[@]}"
printf 'Suite output: %s\n' "${BATCH_ROOT#"$PROJECT_ROOT/"}"
printf '========================================\n\n'

failed=0
for scenario in "${SCENARIOS[@]}"; do
  printf '\n---- [%s] %s ----\n' "$SUITE" "$scenario"
  PERF_SUITE_MANIFEST="$MANIFEST" "$PROJECT_ROOT/performance/run-k6.sh" "$scenario"
  exit_code=$?
  if [[ "$exit_code" -ne 0 ]]; then
    failed=$((failed + 1))
    printf 'Scenario %s finished with exit code %s; continuing suite.\n' "$scenario" "$exit_code" >&2
  fi
done

if command -v node >/dev/null 2>&1; then
  node "$PROJECT_ROOT/performance/scripts/aggregate-reports.mjs" "$MANIFEST" "$BATCH_ROOT" "$SUITE"
  aggregate_exit_code=$?
  if [[ "$aggregate_exit_code" -ne 0 ]]; then
    printf 'Could not generate suite aggregate (exit %s). Individual artifacts were preserved.\n' "$aggregate_exit_code" >&2
    failed=$((failed + 1))
  fi
else
  printf 'Node.js was not found; individual artifacts were preserved but aggregate.json was not generated.\n' >&2
  failed=$((failed + 1))
fi

printf '\n========================================\n'
printf 'VEYRO Performance Suite Result\n'
printf '========================================\n'
printf 'Suite          : %s\n' "$SUITE"
printf 'Scenarios      : %s\n' "${#SCENARIOS[@]}"
printf 'Failed / issue : %s\n' "$failed"
printf 'Suite output   : %s\n' "${BATCH_ROOT#"$PROJECT_ROOT/"}"
printf 'Aggregate JSON : %s/aggregate.json\n' "${BATCH_ROOT#"$PROJECT_ROOT/"}"
printf 'Aggregate MD   : %s/aggregate.md\n' "${BATCH_ROOT#"$PROJECT_ROOT/"}"
printf '========================================\n'

# A suite has finished all children even when this is nonzero. Preserve a
# failure signal for CI or shell callers without masking threshold failures.
if [[ "$failed" -gt 0 ]]; then
  exit 1
fi
