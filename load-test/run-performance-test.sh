#!/bin/bash

set -euo pipefail


# ============================================================
# 사용법
#
# ./load-test/run-performance-test.sh prod-10-30
# ./load-test/run-performance-test.sh prod-30-50
# ./load-test/run-performance-test.sh prod-50-100
#
# ./load-test/run-performance-test.sh local-10-30
# ./load-test/run-performance-test.sh local-30-50
# ============================================================

PROFILE="${1:-}"

RUN_COUNT=3
COOLDOWN_SECONDS=30

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
# Hikari Collector 종료 함수
# ============================================================

stop_hikari_collector() {

    if [ -n "${HIKARI_PID:-}" ] && kill -0 "${HIKARI_PID}" 2>/dev/null; then

        echo ""
        echo "Stopping HikariCP metric collector..."
        echo ""

        kill "${HIKARI_PID}" 2>/dev/null || true

        wait "${HIKARI_PID}" 2>/dev/null || true

    fi

    HIKARI_PID=""
}


# ============================================================
# Script 중간 종료 시에도 Collector 정리
# ============================================================

cleanup() {

    stop_hikari_collector

}

trap cleanup EXIT INT TERM


# ============================================================
# Profile
# ============================================================

case "${PROFILE}" in

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


    *)

        echo ""
        echo "사용할 테스트 프로필을 입력해주세요."
        echo ""
        echo "사용 가능한 프로필:"
        echo ""
        echo "  prod-10-30"
        echo "  prod-30-50"
        echo "  prod-50-100"
        echo "  local-10-30"
        echo "  local-30-50"
        echo ""
        echo "예시:"
        echo ""
        echo "  ./load-test/run-performance-test.sh prod-50-100"
        echo ""

        exit 1

        ;;

esac


# ============================================================
# 실행 시각
# ============================================================

RUN_TIMESTAMP=$(date +"%Y%m%d-%H%M%S")


BASE_RESULT_DIR="load-test/results/${ENVIRONMENT}/${SCENARIO}/${RUN_TIMESTAMP}"


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


if [ ! -f "load-test/collect-hikari-metrics.py" ]; then

    echo ""
    echo "load-test/collect-hikari-metrics.py 파일이 없습니다."
    echo ""

    exit 1

fi


if [ ! -f "load-test/all-api-test.js" ]; then

    echo ""
    echo "load-test/all-api-test.js 파일이 없습니다."
    echo ""

    exit 1

fi


if [ ! -f "load-test/report.py" ]; then

    echo ""
    echo "load-test/report.py 파일이 없습니다."
    echo ""

    exit 1

fi


if [ ! -f "load-test/compare-runs.py" ]; then

    echo ""
    echo "load-test/compare-runs.py 파일이 없습니다."
    echo ""

    exit 1

fi


# ============================================================
# ADMIN 인증 정보 확인
#
# collect-hikari-metrics.py가
# /actuator/metrics/** 를 Basic Auth로 호출함
# ============================================================

if [ -z "${ADMIN_USERNAME:-}" ]; then

    echo ""
    echo "ADMIN_USERNAME 환경변수가 설정되어 있지 않습니다."
    echo ""
    echo "예:"
    echo 'export ADMIN_USERNAME="admin"'
    echo ""

    exit 1

fi


if [ -z "${ADMIN_PASSWORD:-}" ]; then

    echo ""
    echo "ADMIN_PASSWORD 환경변수가 설정되어 있지 않습니다."
    echo ""
    echo "예:"
    echo 'export ADMIN_PASSWORD="your-password"'
    echo ""

    exit 1

fi


# ============================================================
# Result Directory
# ============================================================

mkdir -p "${BASE_RESULT_DIR}"


# ============================================================
# 테스트 정보
# ============================================================

echo ""
echo "============================================================"
echo " TransferTracker Performance Test"
echo "============================================================"
echo ""
echo "PROFILE      : ${PROFILE}"
echo "ENVIRONMENT  : ${ENVIRONMENT}"
echo "BASE_URL     : ${BASE_URL}"
echo "SCENARIO     : ${SCENARIO}"
echo ""
echo "VU STAGES    : ${VU_1} -> ${VU_2} -> ${VU_3}"
echo ""
echo "RUN COUNT    : ${RUN_COUNT}"
echo "COOLDOWN     : ${COOLDOWN_SECONDS}s"
echo ""
echo "HIKARI       : enabled"
echo "INTERVAL     : 1 second"
echo ""
echo "RESULT DIR"
echo "${BASE_RESULT_DIR}"
echo ""
echo "============================================================"
echo ""


# ============================================================
# Run 1 ~ 3
# ============================================================

for RUN_NUMBER in $(seq 1 "${RUN_COUNT}")
do

    RUN_DIR="${BASE_RESULT_DIR}/run-${RUN_NUMBER}"

    mkdir -p "${RUN_DIR}"


    echo ""
    echo "============================================================"
    echo " RUN ${RUN_NUMBER} / ${RUN_COUNT}"
    echo "============================================================"
    echo ""
    echo "VU:"
    echo "  ${VU_1} -> ${VU_2} -> ${VU_3}"
    echo ""
    echo "Result:"
    echo "  ${RUN_DIR}"
    echo ""


    # ========================================================
    # HikariCP Metric Collector 시작
    # ========================================================

    HIKARI_RESULT_FILE="${RUN_DIR}/hikari-metrics.csv"
    HIKARI_LOG_FILE="${RUN_DIR}/hikari-collector.log"


    echo ""
    echo "------------------------------------------------------------"
    echo " Starting HikariCP Metric Collector"
    echo "------------------------------------------------------------"
    echo ""
    echo "CSV:"
    echo "  ${HIKARI_RESULT_FILE}"
    echo ""
    echo "LOG:"
    echo "  ${HIKARI_LOG_FILE}"
    echo ""


    BASE_URL="${BASE_URL}" \
    ADMIN_USERNAME="${ADMIN_USERNAME}" \
    ADMIN_PASSWORD="${ADMIN_PASSWORD}" \
    HIKARI_RESULT_FILE="${HIKARI_RESULT_FILE}" \
    METRIC_INTERVAL_SECONDS="1" \
    python3 load-test/collect-hikari-metrics.py \
        > "${HIKARI_LOG_FILE}" 2>&1 &


    HIKARI_PID=$!


    echo "Hikari Collector PID: ${HIKARI_PID}"
    echo ""


    # Collector가 먼저 한 번 정도 Metric을 수집하도록 잠깐 대기
    sleep 1


    # ========================================================
    # k6
    # ========================================================

    echo ""
    echo "------------------------------------------------------------"
    echo " Starting k6"
    echo "------------------------------------------------------------"
    echo ""


    BASE_URL="${BASE_URL}" \
    RESULT_DIR="${RUN_DIR}" \
    VU_1="${VU_1}" \
    VU_2="${VU_2}" \
    VU_3="${VU_3}" \
    k6 run \
        --out "csv=${RUN_DIR}/all-api-metrics.csv" \
        load-test/all-api-test.js


    # ========================================================
    # k6 끝났으므로 Hikari Collector 종료
    # ========================================================

    stop_hikari_collector


    echo ""
    echo "------------------------------------------------------------"
    echo " HikariCP Metric Collector stopped"
    echo "------------------------------------------------------------"
    echo ""


    # ========================================================
    # 개별 Report
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
    python3 load-test/report.py


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
# 3회 결과 통합
# ============================================================

echo ""
echo "============================================================"
echo " Comparing Runs"
echo "============================================================"
echo ""


RESULT_DIR="${BASE_RESULT_DIR}" \
VU_1="${VU_1}" \
VU_2="${VU_2}" \
VU_3="${VU_3}" \
python3 load-test/compare-runs.py


# ============================================================
# 최종 결과
# ============================================================

COMPARISON_REPORT="${BASE_RESULT_DIR}/comparison-report.html"


echo ""
echo "============================================================"
echo " Performance Test Complete"
echo "============================================================"
echo ""
echo "PROFILE:"
echo "  ${PROFILE}"
echo ""
echo "VU:"
echo "  ${VU_1} -> ${VU_2} -> ${VU_3}"
echo ""
echo "RESULT DIRECTORY:"
echo "  ${BASE_RESULT_DIR}"
echo ""
echo "FINAL REPORT:"
echo "  ${COMPARISON_REPORT}"
echo ""
echo "HIKARI METRICS:"
echo "  run-1/hikari-metrics.csv"
echo "  run-2/hikari-metrics.csv"
echo "  run-3/hikari-metrics.csv"
echo ""
echo "============================================================"
echo ""


# ============================================================
# macOS
# ============================================================

if command -v open >/dev/null 2>&1; then

    open "${COMPARISON_REPORT}"

fi