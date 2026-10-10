#!/bin/bash

set -euo pipefail


# ============================================================
# 사용법
#
# Team Transfers / Local / 40 RPS
#
# ./load-test/run-single-api-rps-test.sh \
#   local \
#   team-transfers \
#   40
#
#
# Transfers List
#
# ./load-test/run-single-api-rps-test.sh \
#   local \
#   transfers \
#   40
#
#
# 기본:
#
# RUN_COUNT=5
# DURATION=60s
# COOLDOWN_SECONDS=30
# ============================================================


ENVIRONMENT="${1:-}"
API_KEY="${2:-}"
TARGET_RPS="${3:-40}"


RUN_COUNT="${RUN_COUNT:-5}"

DURATION="${DURATION:-60s}"

COOLDOWN_SECONDS="${COOLDOWN_SECONDS:-30}"

METRIC_INTERVAL_SECONDS="${METRIC_INTERVAL_SECONDS:-1}"

PRE_ALLOCATED_VUS="${PRE_ALLOCATED_VUS:-20}"

MAX_VUS="${MAX_VUS:-100}"


HIKARI_PID=""


# ============================================================
# Arguments
# ============================================================

if [ -z "${ENVIRONMENT}" ]; then

    echo ""
    echo "환경을 입력해주세요."
    echo ""
    echo "local 또는 prod"
    echo ""

    exit 1

fi


if [ -z "${API_KEY}" ]; then

    echo ""
    echo "API_KEY를 입력해주세요."
    echo ""
    echo "지원 API:"
    echo ""
    echo "  team-transfers"
    echo "  transfers"
    echo ""

    exit 1

fi


case "${API_KEY}" in

    team-transfers|transfers)
        ;;

    *)

        echo ""
        echo "지원하지 않는 API입니다."
        echo ""
        echo "  team-transfers"
        echo "  transfers"
        echo ""

        exit 1

        ;;

esac


# ============================================================
# .env
# ============================================================

ENV_FILE=".env"


if [ ! -f "${ENV_FILE}" ]; then

    echo ""
    echo ".env 파일을 찾을 수 없습니다."
    echo ""

    exit 1

fi


echo ""
echo ".env 파일을 로드합니다."
echo ""


set -a

source "${ENV_FILE}"

set +a


# ============================================================
# Environment
# ============================================================

case "${ENVIRONMENT}" in

    local)

        BASE_URL="http://localhost:8080"

        ;;


    prod)

        BASE_URL="https://transfertracker-back-v1-production.up.railway.app"

        ;;


    *)

        echo ""
        echo "지원하지 않는 환경입니다."
        echo ""
        echo "local 또는 prod"
        echo ""

        exit 1

        ;;

esac


# ============================================================
# Dependency
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


if ! python3 -c \
    "import certifi" \
    >/dev/null 2>&1; then

    echo ""
    echo "certifi가 설치되어 있지 않습니다."
    echo ""
    echo "python3 -m pip install certifi"
    echo ""

    exit 1

fi


for FILE in \
    "load-test/single-api-rps-test.js" \
    "load-test/collect-hikari-metrics.py" \
    "load-test/report.py" \
    "load-test/compare-single-rps-runs.py"
do

    if [ ! -f "${FILE}" ]; then

        echo ""
        echo "${FILE} 파일이 없습니다."
        echo ""

        exit 1

    fi

done


# ============================================================
# ADMIN
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
# Hikari Stop
# ============================================================

stop_hikari_collector() {

    if [ -n "${HIKARI_PID:-}" ] \
        && kill -0 "${HIKARI_PID}" 2>/dev/null; then

        echo ""
        echo "Stopping HikariCP collector..."
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


cleanup() {

    stop_hikari_collector

}


trap cleanup EXIT


# ============================================================
# Result Directory
# ============================================================

RUN_TIMESTAMP=$(
    date +"%Y%m%d-%H%M%S"
)


BASE_RESULT_DIR="load-test/results/single-api-rps/${ENVIRONMENT}/${API_KEY}/${TARGET_RPS}-rps/${RUN_TIMESTAMP}"


mkdir -p \
    "${BASE_RESULT_DIR}"


# ============================================================
# Test Info
# ============================================================

echo ""
echo "============================================================"
echo " TransferTracker Single API Fixed RPS Test"
echo "============================================================"
echo ""
echo "ENVIRONMENT"
echo "  ${ENVIRONMENT}"
echo ""
echo "API"
echo "  ${API_KEY}"
echo ""
echo "TARGET RPS"
echo "  ${TARGET_RPS}"
echo ""
echo "DURATION"
echo "  ${DURATION}"
echo ""
echo "RUN COUNT"
echo "  ${RUN_COUNT}"
echo ""
echo "COOLDOWN"
echo "  ${COOLDOWN_SECONDS}s"
echo ""
echo "PRE ALLOCATED VUS"
echo "  ${PRE_ALLOCATED_VUS}"
echo ""
echo "MAX VUS"
echo "  ${MAX_VUS}"
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

    RUN_DIR="${BASE_RESULT_DIR}/run-${RUN_NUMBER}"


    mkdir -p \
        "${RUN_DIR}"


    echo ""
    echo "============================================================"
    echo " RUN ${RUN_NUMBER} / ${RUN_COUNT}"
    echo "============================================================"
    echo ""
    echo "API:"
    echo "  ${API_KEY}"
    echo ""
    echo "Target RPS:"
    echo "  ${TARGET_RPS}"
    echo ""
    echo "Result:"
    echo "  ${RUN_DIR}"
    echo ""


    # ========================================================
    # Hikari
    # ========================================================

    HIKARI_RESULT_FILE="${RUN_DIR}/hikari-metrics.csv"

    HIKARI_LOG_FILE="${RUN_DIR}/hikari-collector.log"


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


    # 초기 Metric 수집
    sleep 3


    # ========================================================
    # Collector Check
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
    ) as file:

        rows = list(
            csv.DictReader(file)
        )


    valid = any(

        row.get("active", "") != ""
        and row.get("idle", "") != ""
        and row.get("pending", "") != ""
        and row.get("max", "") != ""

        for row in rows
    )


    if not valid:
        sys.exit(1)


except Exception:
    sys.exit(1)

PY

    then

        echo ""
        echo "HikariCP Metric 수집 실패"
        echo ""
        echo "k6 테스트를 시작하지 않습니다."
        echo ""
        echo "${HIKARI_LOG_FILE}"
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

    BASE_URL="${BASE_URL}" \
    RESULT_DIR="${RUN_DIR}" \
    API_KEY="${API_KEY}" \
    TARGET_RPS="${TARGET_RPS}" \
    DURATION="${DURATION}" \
    PRE_ALLOCATED_VUS="${PRE_ALLOCATED_VUS}" \
    MAX_VUS="${MAX_VUS}" \
    k6 run \
        --out \
        "csv=${RUN_DIR}/all-api-metrics.csv" \
        load-test/single-api-rps-test.js


    # ========================================================
    # Hikari Stop
    # ========================================================

    stop_hikari_collector


    # ========================================================
    # Existing Detail Report
    # ========================================================

    echo ""
    echo "Generating detail report..."
    echo ""


    RESULT_DIR="${RUN_DIR}" \
    python3 \
        load-test/report.py


    echo ""
    echo "RUN ${RUN_NUMBER} 완료"
    echo ""


    # ========================================================
    # Cooldown
    # ========================================================

    if [ "${RUN_NUMBER}" -lt "${RUN_COUNT}" ]; then

        echo ""
        echo "${COOLDOWN_SECONDS}초 Cool Down"
        echo ""

        sleep "${COOLDOWN_SECONDS}"

    fi

done


# ============================================================
# Fixed RPS Comparison
# ============================================================

echo ""
echo "============================================================"
echo " Comparing Fixed RPS Runs"
echo "============================================================"
echo ""


RESULT_DIR="${BASE_RESULT_DIR}" \
RUN_COUNT="${RUN_COUNT}" \
TARGET_RPS="${TARGET_RPS}" \
python3 \
    load-test/compare-single-rps-runs.py


# ============================================================
# Final
# ============================================================

COMPARISON_REPORT="${BASE_RESULT_DIR}/comparison-report.html"


echo ""
echo "============================================================"
echo " Fixed RPS Test Complete"
echo "============================================================"
echo ""
echo "API"
echo "  ${API_KEY}"
echo ""
echo "TARGET RPS"
echo "  ${TARGET_RPS}"
echo ""
echo "RUN COUNT"
echo "  ${RUN_COUNT}"
echo ""
echo "RESULT"
echo "  ${BASE_RESULT_DIR}"
echo ""
echo "REPORT"
echo "  ${COMPARISON_REPORT}"
echo ""
echo "============================================================"
echo ""


if command -v open >/dev/null 2>&1; then

    open \
        "${COMPARISON_REPORT}"

fi