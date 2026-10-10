#!/bin/bash

set -euo pipefail


# ============================================================
# 사용법
#
# ./load-test/run-single-api-test.sh local-10-30 players
# ./load-test/run-single-api-test.sh local-30-50 players-team
# ./load-test/run-single-api-test.sh local-30-50 players-league-team
# ./load-test/run-single-api-test.sh local-50-100 transfers
#
# 운영:
#
# ./load-test/run-single-api-test.sh prod-30-50 players-league-team
#
#
# 기본 3회 실행
#
# 1회만:
#
# RUN_COUNT=1 \
# ./load-test/run-single-api-test.sh local-30-50 players-league-team
#
#
# 5회:
#
# RUN_COUNT=5 \
# ./load-test/run-single-api-test.sh local-30-50 players-league-team
#
# ============================================================


LOAD_PROFILE="${1:-}"
API_PROFILE="${2:-}"


RUN_COUNT="${RUN_COUNT:-3}"

COOLDOWN_SECONDS="${COOLDOWN_SECONDS:-30}"

METRIC_INTERVAL_SECONDS="${METRIC_INTERVAL_SECONDS:-1}"

SLEEP_SECONDS="${SLEEP_SECONDS:-1}"


HIKARI_PID=""


# ============================================================
# .env 로드
# ============================================================

ENV_FILE=".env"


if [ -f "${ENV_FILE}" ]; then

    echo ""
    echo ".env 파일을 로드합니다."
    echo ""

    set -a

    source "${ENV_FILE}"

    set +a

else

    echo ""
    echo ".env 파일을 찾을 수 없습니다."
    echo ""

    exit 1

fi


# ============================================================
# Load Profile
# ============================================================

case "${LOAD_PROFILE}" in


    # ========================================================
    # PROD
    # ========================================================

    prod-10-30)

        ENVIRONMENT="prod"

        BASE_URL="https://transfertracker-back-v1-production.up.railway.app"

        SCENARIO="10-30-vu"

        VU_1=10
        VU_2=25
        VU_3=30

        ;;


    prod-30-50)

        ENVIRONMENT="prod"

        BASE_URL="https://transfertracker-back-v1-production.up.railway.app"

        SCENARIO="30-50-vu"

        VU_1=30
        VU_2=40
        VU_3=50

        ;;


    prod-50-100)

        ENVIRONMENT="prod"

        BASE_URL="https://transfertracker-back-v1-production.up.railway.app"

        SCENARIO="50-100-vu"

        VU_1=50
        VU_2=75
        VU_3=100

        ;;


    # ========================================================
    # LOCAL
    # ========================================================

    local-10-30)

        ENVIRONMENT="local"

        BASE_URL="http://localhost:8080"

        SCENARIO="10-30-vu"

        VU_1=10
        VU_2=25
        VU_3=30

        ;;


    local-30-50)

        ENVIRONMENT="local"

        BASE_URL="http://localhost:8080"

        SCENARIO="30-50-vu"

        VU_1=30
        VU_2=40
        VU_3=50

        ;;


    local-50-100)

        ENVIRONMENT="local"

        BASE_URL="http://localhost:8080"

        SCENARIO="50-100-vu"

        VU_1=50
        VU_2=75
        VU_3=100

        ;;


    *)

        echo ""
        echo "사용할 Load Profile을 입력해주세요."
        echo ""
        echo "사용 가능한 Profile:"
        echo ""
        echo "  local-10-30"
        echo "  local-30-50"
        echo "  local-50-100"
        echo ""
        echo "  prod-10-30"
        echo "  prod-30-50"
        echo "  prod-50-100"
        echo ""
        echo "예:"
        echo ""
        echo "  ./load-test/run-single-api-test.sh local-30-50 players-league-team"
        echo ""

        exit 1

        ;;

esac


# ============================================================
# API Profile
# ============================================================

case "${API_PROFILE}" in


    # ========================================================
    # Player
    # ========================================================

    players)

        TARGET_PATH="/api/players?page=0"

        API_KEY="players_list"

        API_NAME="Players List"

        ;;


    players-epl)

        TARGET_PATH="/api/players?page=0&leagueCode=EPL"

        API_KEY="players_league_filter"

        API_NAME="Players - EPL Filter"

        ;;


    players-team)

        TARGET_PATH="/api/players?page=0&teamId=2"

        API_KEY="players_team_filter"

        API_NAME="Players - Team Filter"

        ;;


    players-league-team)

        TARGET_PATH="/api/players?page=0&leagueCode=EPL&teamId=2"

        API_KEY="players_league_team_filter"

        API_NAME="Players - EPL + Team Filter"

        ;;


    # ========================================================
    # Transfer
    # ========================================================

    transfers)

        TARGET_PATH="/api/transfers"

        API_KEY="transfers_list"

        API_NAME="Transfers List"

        ;;


    team-transfers)

        TARGET_PATH="/api/transfers/team/2"

        API_KEY="team_transfers_list"

        API_NAME="Team Transfers List"

        ;;


    # ========================================================
    # Team
    # ========================================================

    teams)

        TARGET_PATH="/api/teams"

        API_KEY="teams_list"

        API_NAME="Team List"

        ;;


    team-detail)

        TARGET_PATH="/api/team/test/2"

        API_KEY="team_detail"

        API_NAME="Team Detail"

        ;;


    *)

        echo ""
        echo "테스트할 API Profile을 입력해주세요."
        echo ""
        echo "사용 가능한 API:"
        echo ""
        echo "  players"
        echo "  players-epl"
        echo "  players-team"
        echo "  players-league-team"
        echo ""
        echo "  transfers"
        echo "  team-transfers"
        echo ""
        echo "  teams"
        echo "  team-detail"
        echo ""
        echo "예:"
        echo ""
        echo "  ./load-test/run-single-api-test.sh local-30-50 players-league-team"
        echo ""

        exit 1

        ;;

esac


# ============================================================
# Dependency Check
# ============================================================

if ! command -v k6 >/dev/null 2>&1; then

    echo ""
    echo "k6가 설치되어 있지 않습니다."
    echo ""

    exit 1

fi


if ! command -v python3 >/dev/null 2>&1; then

    echo ""
    echo "python3가 설치되어 있지 않습니다."
    echo ""

    exit 1

fi


if ! python3 -c "import certifi" >/dev/null 2>&1; then

    echo ""
    echo "Python certifi 패키지가 없습니다."
    echo ""
    echo "다음 명령어로 설치해주세요:"
    echo ""
    echo "python3 -m pip install certifi"
    echo ""

    exit 1

fi


if [ ! -f "load-test/single-api-test.js" ]; then

    echo ""
    echo "single-api-test.js 파일이 없습니다."
    echo ""

    exit 1

fi


if [ ! -f "load-test/collect-hikari-metrics.py" ]; then

    echo ""
    echo "collect-hikari-metrics.py 파일이 없습니다."
    echo ""

    exit 1

fi


if [ ! -f "load-test/report.py" ]; then

    echo ""
    echo "report.py 파일이 없습니다."
    echo ""

    exit 1

fi


if [ ! -f "load-test/compare-runs.py" ]; then

    echo ""
    echo "compare-runs.py 파일이 없습니다."
    echo ""

    exit 1

fi


# ============================================================
# ADMIN 인증 정보
# ============================================================

if [ -z "${ADMIN_USERNAME:-}" ]; then

    echo ""
    echo "ADMIN_USERNAME 환경변수가 없습니다."
    echo ""

    exit 1

fi


if [ -z "${ADMIN_PASSWORD:-}" ]; then

    echo ""
    echo "ADMIN_PASSWORD 환경변수가 없습니다."
    echo ""

    exit 1

fi


# ============================================================
# 실행 시각
# ============================================================

RUN_TIMESTAMP=$(
    date +"%Y%m%d-%H%M%S"
)


# ============================================================
# Base Result Directory
#
# 예:
#
# load-test/results/
#   single-api/
#     local/
#       players_league_team_filter/
#         30-50-vu/
#           20261007-220000/
#             run-1/
#             run-2/
#             run-3/
#             comparison-report.html
# ============================================================

BASE_RESULT_DIR="load-test/results/single-api/${ENVIRONMENT}/${API_KEY}/${SCENARIO}/${RUN_TIMESTAMP}"


mkdir -p \
    "${BASE_RESULT_DIR}"


# ============================================================
# Hikari Collector 종료
# ============================================================

stop_hikari_collector() {

    if [ -n "${HIKARI_PID:-}" ] \
        && kill -0 "${HIKARI_PID}" 2>/dev/null; then

        echo ""
        echo "Stopping HikariCP metric collector..."
        echo ""

        kill "${HIKARI_PID}" \
            2>/dev/null \
            || true


        wait "${HIKARI_PID}" \
            2>/dev/null \
            || true

    fi


    HIKARI_PID=""

}


# ============================================================
# Script 종료 시 Collector 정리
# ============================================================

cleanup() {

    stop_hikari_collector

}


trap cleanup EXIT


# ============================================================
# Test Info
# ============================================================

echo ""
echo "============================================================"
echo " TransferTracker Single API Performance Test"
echo "============================================================"
echo ""
echo "LOAD PROFILE"
echo "  ${LOAD_PROFILE}"
echo ""
echo "ENVIRONMENT"
echo "  ${ENVIRONMENT}"
echo ""
echo "SCENARIO"
echo "  ${SCENARIO}"
echo ""
echo "API PROFILE"
echo "  ${API_PROFILE}"
echo ""
echo "API"
echo "  ${API_NAME}"
echo ""
echo "TARGET"
echo "  ${TARGET_PATH}"
echo ""
echo "VU"
echo "  ${VU_1} -> ${VU_2} -> ${VU_3}"
echo ""
echo "RUN COUNT"
echo "  ${RUN_COUNT}"
echo ""
echo "COOLDOWN"
echo "  ${COOLDOWN_SECONDS}s"
echo ""
echo "RESULT"
echo "  ${BASE_RESULT_DIR}"
echo ""
echo "============================================================"
echo ""


# ============================================================
# Run
# ============================================================

for RUN_NUMBER in $(
    seq 1 "${RUN_COUNT}"
)
do


    # ========================================================
    # Run Directory
    # ========================================================

    RUN_DIR="${BASE_RESULT_DIR}/run-${RUN_NUMBER}"


    mkdir -p \
        "${RUN_DIR}"


    echo ""
    echo "============================================================"
    echo " RUN ${RUN_NUMBER} / ${RUN_COUNT}"
    echo "============================================================"
    echo ""
    echo "API:"
    echo "  ${API_NAME}"
    echo ""
    echo "VU:"
    echo "  ${VU_1} -> ${VU_2} -> ${VU_3}"
    echo ""
    echo "RESULT:"
    echo "  ${RUN_DIR}"
    echo ""


    # ========================================================
    # HikariCP Metric Collector
    # ========================================================

    HIKARI_RESULT_FILE="${RUN_DIR}/hikari-metrics.csv"

    HIKARI_LOG_FILE="${RUN_DIR}/hikari-collector.log"


    echo ""
    echo "------------------------------------------------------------"
    echo " Starting HikariCP Metric Collector"
    echo "------------------------------------------------------------"
    echo ""


    BASE_URL="${BASE_URL}" \
    ADMIN_USERNAME="${ADMIN_USERNAME}" \
    ADMIN_PASSWORD="${ADMIN_PASSWORD}" \
    HIKARI_RESULT_FILE="${HIKARI_RESULT_FILE}" \
    METRIC_INTERVAL_SECONDS="${METRIC_INTERVAL_SECONDS}" \
    python3 \
        load-test/collect-hikari-metrics.py \
        > "${HIKARI_LOG_FILE}" \
        2>&1 &


    HIKARI_PID=$!


    echo ""
    echo "Hikari Collector PID:"
    echo "  ${HIKARI_PID}"
    echo ""


    # ========================================================
    # Collector 초기 Metric 수집 대기
    # ========================================================

    sleep 3


    # ========================================================
    # Collector 정상 여부 확인
    #
    # Hikari Metric 수집이 실패했다면
    # 성능 테스트 자체를 시작하지 않는다.
    # ========================================================

    if ! python3 \
        - \
        "${HIKARI_RESULT_FILE}" <<'PY'

import csv
import sys


file_path = sys.argv[1]


try:

    with open(
        file_path,
        encoding="utf-8"
    ) as f:

        rows = list(
            csv.DictReader(f)
        )


    valid = any(

        row.get(
            "active",
            ""
        ) != ""

        and

        row.get(
            "idle",
            ""
        ) != ""

        and

        row.get(
            "pending",
            ""
        ) != ""

        and

        row.get(
            "max",
            ""
        ) != ""

        for row in rows
    )


    if not valid:

        sys.exit(1)


except Exception:

    sys.exit(1)

PY

    then

        echo ""
        echo "============================================================"
        echo " HikariCP Metric 수집 실패"
        echo "============================================================"
        echo ""
        echo "k6 테스트를 시작하지 않습니다."
        echo ""
        echo "로그:"
        echo "  ${HIKARI_LOG_FILE}"
        echo ""

        stop_hikari_collector

        exit 1

    fi


    echo ""
    echo "HikariCP Metric 수집 정상"
    echo ""


    # ========================================================
    # k6
    # ========================================================

    echo ""
    echo "------------------------------------------------------------"
    echo " Starting k6"
    echo "------------------------------------------------------------"
    echo ""


    BASE_URL="${BASE_URL}" \
    TARGET_PATH="${TARGET_PATH}" \
    API_KEY="${API_KEY}" \
    API_NAME="${API_NAME}" \
    METHOD="GET" \
    RESULT_DIR="${RUN_DIR}" \
    VU_1="${VU_1}" \
    VU_2="${VU_2}" \
    VU_3="${VU_3}" \
    SLEEP_SECONDS="${SLEEP_SECONDS}" \
    k6 run \
        --out \
        "csv=${RUN_DIR}/all-api-metrics.csv" \
        load-test/single-api-test.js


    # ========================================================
    # k6 종료 → Hikari Collector 종료
    # ========================================================

    stop_hikari_collector


    echo ""
    echo "------------------------------------------------------------"
    echo " HikariCP Metric Collector stopped"
    echo "------------------------------------------------------------"
    echo ""


    # ========================================================
    # 개별 Run Report
    #
    # report.py가 아래 파일을 읽는다.
    #
    # all-api-result.json
    # all-api-metrics.csv
    # hikari-metrics.csv
    # ========================================================

    echo ""
    echo "------------------------------------------------------------"
    echo " Generating run-${RUN_NUMBER} report"
    echo "------------------------------------------------------------"
    echo ""


    RESULT_DIR="${RUN_DIR}" \
    VU_1="${VU_1}" \
    VU_2="${VU_2}" \
    VU_3="${VU_3}" \
    python3 \
        load-test/report.py


    echo ""
    echo "RUN ${RUN_NUMBER} 완료"
    echo ""


    # ========================================================
    # Cool Down
    # ========================================================

    if [ "${RUN_NUMBER}" -lt "${RUN_COUNT}" ]; then

        echo ""
        echo "------------------------------------------------------------"
        echo " ${COOLDOWN_SECONDS}초 Cool Down"
        echo "------------------------------------------------------------"
        echo ""

        sleep "${COOLDOWN_SECONDS}"

    fi


done


# ============================================================
# Run 결과 통합
#
# compare-runs.py가
#
# run-1/
# run-2/
# run-3/
#
# 아래 결과를 자동으로 읽는다.
# ============================================================

echo ""
echo "============================================================"
echo " Comparing Runs"
echo "============================================================"
echo ""


RESULT_DIR="${BASE_RESULT_DIR}" \
RUN_COUNT="${RUN_COUNT}" \
VU_1="${VU_1}" \
VU_2="${VU_2}" \
VU_3="${VU_3}" \
python3 \
    load-test/compare-runs.py


# ============================================================
# 최종 결과
# ============================================================

COMPARISON_REPORT="${BASE_RESULT_DIR}/comparison-report.html"


echo ""
echo "============================================================"
echo " Single API Performance Test Complete"
echo "============================================================"
echo ""
echo "PROFILE"
echo "  ${LOAD_PROFILE}"
echo ""
echo "API"
echo "  ${API_NAME}"
echo ""
echo "TARGET"
echo "  ${TARGET_PATH}"
echo ""
echo "VU"
echo "  ${VU_1} -> ${VU_2} -> ${VU_3}"
echo ""
echo "RUN COUNT"
echo "  ${RUN_COUNT}"
echo ""
echo "RESULT DIRECTORY"
echo "  ${BASE_RESULT_DIR}"
echo ""
echo "FINAL REPORT"
echo "  ${COMPARISON_REPORT}"
echo ""
echo "============================================================"
echo ""


# ============================================================
# macOS
# ============================================================

if command -v open >/dev/null 2>&1; then

    open \
        "${COMPARISON_REPORT}"

fi