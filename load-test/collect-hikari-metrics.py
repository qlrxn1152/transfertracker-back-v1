import os
import csv
import json
import time
import base64
import ssl
import certifi
import urllib.request
import urllib.error
from datetime import datetime
from pathlib import Path


# ============================================================
# 환경 변수
# ============================================================

BASE_URL = os.environ["BASE_URL"].rstrip("/")

ADMIN_USERNAME = os.environ["ADMIN_USERNAME"]
ADMIN_PASSWORD = os.environ["ADMIN_PASSWORD"]

RESULT_FILE = Path(
    os.getenv(
        "HIKARI_RESULT_FILE",
        "load-test/results/hikari-metrics.csv"
    )
)

INTERVAL_SECONDS = float(
    os.getenv(
        "METRIC_INTERVAL_SECONDS",
        "1"
    )
)


# ============================================================
# Metric
# ============================================================

METRICS = {
    "active":
        "hikaricp.connections.active",

    "idle":
        "hikaricp.connections.idle",

    "pending":
        "hikaricp.connections.pending",

    "max":
        "hikaricp.connections.max",
}


# ============================================================
# Basic Auth
# ============================================================

credentials = (
    f"{ADMIN_USERNAME}:{ADMIN_PASSWORD}"
)

encoded_credentials = (
    base64.b64encode(
        credentials.encode("utf-8")
    )
    .decode("utf-8")
)


# ============================================================
# SSL
#
# macOS Python에서 Railway HTTPS 인증서 검증 실패 방지를 위해
# certifi CA Bundle 사용
# ============================================================

SSL_CONTEXT = ssl.create_default_context(
    cafile=certifi.where()
)


# ============================================================
# Metric 조회
# ============================================================

def get_metric(metric_name):

    url = (
        f"{BASE_URL}"
        f"/actuator/metrics/"
        f"{metric_name}"
    )

    request = urllib.request.Request(
        url
    )

    request.add_header(
        "Authorization",
        f"Basic {encoded_credentials}"
    )

    try:

        with urllib.request.urlopen(
                request,
                timeout=5,
                context=SSL_CONTEXT
        ) as response:

            data = json.loads(
                response.read()
                .decode("utf-8")
            )

            measurements = data.get(
                "measurements",
                []
            )

            if not measurements:
                return None

            return measurements[0].get(
                "value"
            )

    except Exception as e:

        print(
            f"[HIKARI] "
            f"{metric_name} 조회 실패: {e}",
            flush=True
        )

        return None


# ============================================================
# CSV 준비
# ============================================================

RESULT_FILE.parent.mkdir(
    parents=True,
    exist_ok=True
)


fieldnames = [
    "timestamp",
    "timestamp_epoch_ms",
    "elapsed_seconds",
    "active",
    "idle",
    "pending",
    "max",
]


start_time = time.time()


print()
print(
    "HikariCP metric collector started",
    flush=True
)
print(
    f"BASE_URL : {BASE_URL}",
    flush=True
)
print(
    f"OUTPUT   : {RESULT_FILE}",
    flush=True
)
print()


# ============================================================
# Metric Polling
# ============================================================

with open(
        RESULT_FILE,
        "w",
        newline="",
        encoding="utf-8"
) as f:

    writer = csv.DictWriter(
        f,
        fieldnames=fieldnames
    )

    writer.writeheader()

    f.flush()


    while True:

        cycle_started_at = time.time()


        values = {}

        for key, metric_name in (
                METRICS.items()
        ):

            values[key] = get_metric(
                metric_name
            )


        now = time.time()

        elapsed = (
                now - start_time
        )


        row = {

            "timestamp":
                datetime.now()
                .astimezone()
                .isoformat(),

            "timestamp_epoch_ms":
                int(now * 1000),

            "elapsed_seconds":
                round(
                    elapsed,
                    3
                ),

            "active":
                values["active"],

            "idle":
                values["idle"],

            "pending":
                values["pending"],

            "max":
                values["max"],
        }


        writer.writerow(
            row
        )

        f.flush()


        print(
            f"[{elapsed:7.2f}s] "
            f"active={values['active']} "
            f"idle={values['idle']} "
            f"pending={values['pending']} "
            f"max={values['max']}",
            flush=True
        )


        # API 호출 시간까지 포함해서
        # 가능한 한 설정한 polling interval에 맞춘다.
        cycle_duration = (
                time.time()
                - cycle_started_at
        )

        sleep_seconds = max(
            0,
            INTERVAL_SECONDS
            - cycle_duration
        )


        if sleep_seconds > 0:

            time.sleep(
                sleep_seconds
            )