import csv
import html
import json
import math
import os
import statistics
from collections import Counter, defaultdict
from pathlib import Path


# ============================================================
# 기본 설정
# ============================================================

BASE_DIR = Path(
    os.getenv(
        "RESULT_DIR",
        "load-test/results/local",
    )
)

RUN_COUNT = int(
    os.getenv(
        "RUN_COUNT",
        "3",
    )
)

RUN_NAMES = [
    f"run-{number}"
    for number in range(1, RUN_COUNT + 1)
]

REPORT_FILE = BASE_DIR / "comparison-report.html"

# 실제 VU가 이 시간 이상 동일하게 유지되면 Hold로 판단한다.
MIN_HOLD_SECONDS = int(
    os.getenv(
        "MIN_HOLD_SECONDS",
        "5",
    )
)


# ============================================================
# 공통 Utility
# ============================================================

def parse_float(value):
    if value is None or value == "":
        return None

    try:
        return float(value)
    except (TypeError, ValueError):
        return None


def parse_api_tag(extra_tags):
    if not extra_tags:
        return None

    for tag in extra_tags.split(","):
        key, _, value = tag.partition("=")
        if key == "api":
            return value

    return None


def percentile(values, p):
    if not values:
        return None

    values = sorted(values)
    index = (len(values) - 1) * p
    lower = int(index)
    upper = min(lower + 1, len(values) - 1)
    fraction = index - lower

    return (
        values[lower]
        + (values[upper] - values[lower]) * fraction
    )


def change_rate(before, after):
    if before is None or after is None or before == 0:
        return None

    return ((after - before) / before) * 100


def change_text(before, after):
    rate = change_rate(before, after)

    if rate is None:
        return "-"

    sign = "+" if rate >= 0 else ""
    return f"{sign}{rate:.1f}%"


def fmt(value, digits=2, suffix=""):
    if value is None:
        return "-"

    return f"{value:.{digits}f}{suffix}"


def fmt_int(value):
    if value is None:
        return "-"

    return f"{int(round(value)):,}"


def js(value):
    return json.dumps(
        value,
        ensure_ascii=False,
        separators=(",", ":"),
    )


def nearest(mapping, target):
    if not mapping:
        return None

    candidates = [
        second
        for second in (
            int(round(target)),
            int(math.floor(target)),
            int(math.ceil(target)),
            int(round(target)) - 1,
            int(round(target)) + 1,
        )
        if second in mapping
    ]

    if not candidates:
        return None

    selected = min(
        candidates,
        key=lambda second: abs(second - target),
    )

    return mapping[selected]


def chart_dataset(label, data, color):
    return {
        "label": label,
        "data": data,
        "borderColor": color,
        "backgroundColor": color,
        "pointRadius": 2,
        "pointHoverRadius": 5,
        "borderWidth": 2,
        "tension": 0.2,
        "spanGaps": True,
    }


# ============================================================
# 실제 VU 기반 Hold 자동 탐지
# ============================================================

def detect_hold_windows(vu_by_second, min_hold_seconds):
    if not vu_by_second:
        return []

    samples = sorted(vu_by_second.items())

    segments = []
    start_second, current_vu = samples[0]
    previous_second = start_second

    for second, vu in samples[1:]:
        same_vu = (
            int(round(vu))
            == int(round(current_vu))
        )

        consecutive = (
            second == previous_second + 1
        )

        if same_vu and consecutive:
            previous_second = second
            continue

        duration = previous_second - start_second + 1

        if current_vu > 0 and duration >= min_hold_seconds:
            segments.append(
                {
                    "vu": int(round(current_vu)),
                    "start": start_second,
                    "end": previous_second + 1,
                    "duration": duration,
                }
            )

        start_second = second
        previous_second = second
        current_vu = vu

    duration = previous_second - start_second + 1

    if current_vu > 0 and duration >= min_hold_seconds:
        segments.append(
            {
                "vu": int(round(current_vu)),
                "start": start_second,
                "end": previous_second + 1,
                "duration": duration,
            }
        )

    # 같은 VU Hold가 여러 번 등장하는 테스트도 구분한다.
    counts = Counter(segment["vu"] for segment in segments)
    seen = Counter()

    for segment in segments:
        vu = segment["vu"]
        seen[vu] += 1

        if counts[vu] == 1:
            segment["name"] = f"{vu} VU"
        else:
            segment["name"] = f"{vu} VU #{seen[vu]}"

    return segments


# ============================================================
# 한 Run의 k6 CSV 읽기
# ============================================================

def read_k6_metrics(metrics_file):
    http_rows = []
    vu_raw = []
    vus_max_values = []
    http_request_timestamps = []

    with open(metrics_file, encoding="utf-8") as file:
        reader = csv.DictReader(file)

        for row in reader:
            metric = row.get("metric_name")
            timestamp = parse_float(row.get("timestamp"))
            value = parse_float(row.get("metric_value"))

            if timestamp is None or value is None:
                continue

            if metric == "vus":
                vu_raw.append((timestamp, value))
                continue

            if metric == "vus_max":
                vus_max_values.append(value)
                continue

            api = parse_api_tag(row.get("extra_tags"))

            if api is None:
                continue

            if metric not in ("http_reqs", "http_req_duration"):
                continue

            http_rows.append(
                {
                    "metric": metric,
                    "timestamp": timestamp,
                    "value": value,
                    "api": api,
                }
            )

            if metric == "http_reqs":
                http_request_timestamps.append(timestamp)

    if not http_request_timestamps:
        raise RuntimeError(
            f"{metrics_file}: http_reqs metric을 찾을 수 없습니다."
        )

    start_time = min(http_request_timestamps)
    end_time = max(http_request_timestamps)

    return {
        "httpRows": http_rows,
        "vuRaw": vu_raw,
        "vusMaxValues": vus_max_values,
        "startTime": start_time,
        "endTime": end_time,
    }


# ============================================================
# Hikari CSV 읽기
# ============================================================

def read_hikari_metrics(hikari_file, k6_start_time):
    if not hikari_file.exists():
        return []

    rows = []

    with open(hikari_file, encoding="utf-8") as file:
        reader = csv.DictReader(file)

        for row in reader:
            epoch_ms = parse_float(row.get("timestamp_epoch_ms"))

            if epoch_ms is None:
                continue

            elapsed = epoch_ms / 1000 - k6_start_time

            # Collector는 k6보다 먼저 실행된다.
            if elapsed < 0:
                continue

            active = parse_float(row.get("active"))
            idle = parse_float(row.get("idle"))
            pending = parse_float(row.get("pending"))
            pool_max = parse_float(row.get("max"))

            if all(
                value is None
                for value in (
                    active,
                    idle,
                    pending,
                    pool_max,
                )
            ):
                continue

            rows.append(
                {
                    "elapsed": elapsed,
                    "timestamp": row.get("timestamp", ""),
                    "active": active,
                    "idle": idle,
                    "pending": pending,
                    "max": pool_max,
                }
            )

    return rows


# ============================================================
# 한 Run 분석
# ============================================================

def analyze_run(run_name):
    run_dir = BASE_DIR / run_name
    summary_file = run_dir / "all-api-result.json"
    metrics_file = run_dir / "all-api-metrics.csv"
    hikari_file = run_dir / "hikari-metrics.csv"
    detail_report = run_dir / "report.html"

    for required in (summary_file, metrics_file):
        if not required.exists():
            raise FileNotFoundError(
                f"{required} 파일이 없습니다."
            )

    with open(summary_file, encoding="utf-8") as file:
        summary = json.load(file)

    api_keys = list(summary.keys())
    api_names = {
        api_key: summary[api_key].get("name", api_key)
        for api_key in api_keys
    }

    k6 = read_k6_metrics(metrics_file)
    start_time = k6["startTime"]
    end_time = k6["endTime"]

    # --------------------------------------------------------
    # 실제 VU Timeline
    # --------------------------------------------------------
    vu_by_second = {}

    for timestamp, value in k6["vuRaw"]:
        second = int(round(timestamp - start_time))
        if second >= 0:
            vu_by_second[second] = value

    actual_peak_vu = (
        max(vu_by_second.values())
        if vu_by_second
        else None
    )

    configured_max_vu = (
        max(k6["vusMaxValues"])
        if k6["vusMaxValues"]
        else None
    )

    hold_windows = detect_hold_windows(
        vu_by_second,
        MIN_HOLD_SECONDS,
    )

    # Forward Fill된 VU
    max_vu_second = max(vu_by_second.keys()) if vu_by_second else 0
    vu_filled = {}
    last_vu = 0.0

    for second in range(0, max_vu_second + 1):
        if second in vu_by_second:
            last_vu = vu_by_second[second]
        vu_filled[second] = last_vu

    # --------------------------------------------------------
    # API 초 단위 Metric
    # --------------------------------------------------------
    duration_by_second = defaultdict(lambda: defaultdict(list))
    requests_by_second = defaultdict(lambda: defaultdict(float))

    for row in k6["httpRows"]:
        second = int(row["timestamp"] - start_time)

        if second < 0:
            continue

        api_key = row["api"]

        if api_key not in summary:
            continue

        if row["metric"] == "http_req_duration":
            duration_by_second[second][api_key].append(row["value"])

        elif row["metric"] == "http_reqs":
            requests_by_second[second][api_key] += row["value"]

    max_api_second = 0

    if duration_by_second:
        max_api_second = max(max_api_second, max(duration_by_second.keys()))

    if requests_by_second:
        max_api_second = max(max_api_second, max(requests_by_second.keys()))

    max_second = max(max_vu_second, max_api_second)

    # VU Forward Fill을 API 종료 시점까지 확장한다.
    if max_second > max_vu_second:
        for second in range(max_vu_second + 1, max_second + 1):
            vu_filled[second] = last_vu

    api_avg = defaultdict(dict)
    api_max = defaultdict(dict)
    api_rps = defaultdict(dict)
    total_rps = {}

    for second in range(0, max_second + 1):
        total = 0.0

        for api_key in api_keys:
            durations = duration_by_second[second].get(api_key, [])
            requests = requests_by_second[second].get(api_key, 0.0)

            total += requests
            api_rps[second][api_key] = requests

            api_avg[second][api_key] = (
                sum(durations) / len(durations)
                if durations
                else None
            )

            api_max[second][api_key] = (
                max(durations)
                if durations
                else None
            )

        total_rps[second] = total

    # --------------------------------------------------------
    # 실제 Hold별 API 분석
    # --------------------------------------------------------
    stages = {}

    for api_key in api_keys:
        stages[api_key] = {
            "name": api_names[api_key],
            "stages": {},
        }

        for window in hold_windows:
            durations = []
            request_count = 0.0

            for second in range(window["start"], window["end"]):
                durations.extend(
                    duration_by_second[second].get(api_key, [])
                )

                request_count += (
                    requests_by_second[second].get(api_key, 0.0)
                )

            if not durations:
                continue

            duration_seconds = max(1, window["end"] - window["start"])

            stages[api_key]["stages"][window["name"]] = {
                "avg": sum(durations) / len(durations),
                "p95": percentile(durations, 0.95),
                "p99": percentile(durations, 0.99),
                "rps": request_count / duration_seconds,
                "requestCount": int(request_count),
            }

    # --------------------------------------------------------
    # Hikari
    # --------------------------------------------------------
    hikari_rows = read_hikari_metrics(
        hikari_file,
        start_time,
    )

    def values_of(key):
        return [
            row[key]
            for row in hikari_rows
            if row[key] is not None
        ]

    active_values = values_of("active")
    idle_values = values_of("idle")
    pending_values = values_of("pending")
    pool_max_values = values_of("max")

    peak_active = max(active_values) if active_values else None
    minimum_idle = min(idle_values) if idle_values else None
    max_pending = max(pending_values) if pending_values else None
    pool_max = max(pool_max_values) if pool_max_values else None

    pending_events = [
        row
        for row in hikari_rows
        if (
            row["pending"] is not None
            and row["pending"] > 0
        )
    ]

    # Hold별 Hikari Summary
    hikari_stages = {}

    for window in hold_windows:
        samples = [
            row
            for row in hikari_rows
            if window["start"] <= row["elapsed"] < window["end"]
        ]

        if not samples:
            continue

        stage_active = [
            row["active"]
            for row in samples
            if row["active"] is not None
        ]

        stage_idle = [
            row["idle"]
            for row in samples
            if row["idle"] is not None
        ]

        stage_pending = [
            row["pending"]
            for row in samples
            if row["pending"] is not None
        ]

        stage_max = [
            row["max"]
            for row in samples
            if row["max"] is not None
        ]

        hikari_stages[window["name"]] = {
            "maxActive": max(stage_active) if stage_active else None,
            "minIdle": min(stage_idle) if stage_idle else None,
            "maxPending": max(stage_pending) if stage_pending else None,
            "poolMax": max(stage_max) if stage_max else None,
            "pendingSamples": sum(
                1
                for value in stage_pending
                if value > 0
            ),
        }

    # --------------------------------------------------------
    # Pending 발생 시점에 같은 초의 API 상태 연결
    # --------------------------------------------------------
    pending_event_details = []

    for event in pending_events:
        elapsed = event["elapsed"]
        second = int(round(elapsed))

        vu = nearest(vu_filled, elapsed)
        rps = nearest(total_rps, elapsed)

        avg_candidates = [
            (value, api_key)
            for api_key in api_keys
            if (
                value := api_avg[second].get(api_key)
            ) is not None
        ]

        max_candidates = [
            (value, api_key)
            for api_key in api_keys
            if (
                value := api_max[second].get(api_key)
            ) is not None
        ]

        highest_avg = max(avg_candidates) if avg_candidates else None
        highest_max = max(max_candidates) if max_candidates else None

        pending_event_details.append(
            {
                "elapsed": elapsed,
                "vu": vu,
                "active": event["active"],
                "idle": event["idle"],
                "pending": event["pending"],
                "poolMax": event["max"],
                "totalRps": rps,
                "highestAvgApi": (
                    api_names[highest_avg[1]]
                    if highest_avg
                    else None
                ),
                "highestAvgMs": (
                    highest_avg[0]
                    if highest_avg
                    else None
                ),
                "highestMaxApi": (
                    api_names[highest_max[1]]
                    if highest_max
                    else None
                ),
                "highestMaxMs": (
                    highest_max[0]
                    if highest_max
                    else None
                ),
            }
        )

    # --------------------------------------------------------
    # Pending 여부 + 동일 VU 기준 API Latency 원본 수집
    #
    # 단순히 Pending=0 전체 구간과 Pending>0 구간을 비교하면
    # 낮은 VU 구간이 Normal 쪽에 많이 섞여 왜곡될 수 있다.
    # 따라서 VU별로 원본 request duration을 분리해 둔다.
    # 최종 comparison 단계에서 Pending이 관측된 VU와
    # 동일한 VU의 Normal 구간만 매칭해서 비교한다.
    # --------------------------------------------------------
    hikari_state_by_second = {}

    for row in hikari_rows:
        pending = row.get("pending")

        if pending is None:
            continue

        second = int(round(row["elapsed"]))
        previous = hikari_state_by_second.get(second)

        # 같은 정수 초에 여러 Hikari sample이 매핑될 경우
        # 하나라도 Pending > 0이면 Pending 구간으로 우선 분류한다.
        if pending > 0:
            hikari_state_by_second[second] = "pending"
        elif previous is None:
            hikari_state_by_second[second] = "normal"

    latency_by_state_vu = {
        api_key: {
            "normal": defaultdict(list),
            "pending": defaultdict(list),
        }
        for api_key in api_keys
    }

    for second, state in hikari_state_by_second.items():
        vu = nearest(vu_filled, second)

        if vu is None:
            continue

        vu = int(round(vu))

        for api_key in api_keys:
            durations = duration_by_second[second].get(api_key, [])

            if not durations:
                continue

            latency_by_state_vu[api_key][state][vu].extend(durations)

    total_requests = sum(
        value.get("requestCount", 0)
        for value in summary.values()
    )

    failed_requests = sum(
        value.get("requestCount", 0)
        * value.get("failRate", 0)
        for value in summary.values()
    )

    fail_rate = (
        failed_requests / total_requests
        if total_requests
        else 0
    )

    return {
        "name": run_name,
        "runDir": run_dir,
        "detailReportExists": detail_report.exists(),
        "summary": summary,
        "apiKeys": api_keys,
        "apiNames": api_names,
        "stages": stages,
        "holdWindows": hold_windows,
        "actualPeakVu": actual_peak_vu,
        "configuredMaxVu": configured_max_vu,
        "testDurationSeconds": int(end_time - start_time) + 1,
        "totalRequests": total_requests,
        "failRate": fail_rate,
        "hikariRows": hikari_rows,
        "hikariStages": hikari_stages,
        "peakActive": peak_active,
        "minimumIdle": minimum_idle,
        "maxPending": max_pending,
        "poolMax": pool_max,
        "pendingSamples": len(pending_events),
        "pendingEvents": pending_event_details,
        "latencyByPendingStateVu": latency_by_state_vu,
    }


# ============================================================
# 모든 Run 분석
# ============================================================

runs = []

for run_name in RUN_NAMES:
    print(f"Analyzing {run_name}...")
    runs.append(analyze_run(run_name))

if not runs:
    raise RuntimeError("분석할 Run이 없습니다.")


# ============================================================
# Run 간 API 목록 일관성 확인
# ============================================================

first_api_keys = runs[0]["apiKeys"]
first_api_key_set = set(first_api_keys)

for run in runs[1:]:
    if set(run["apiKeys"]) != first_api_key_set:
        raise RuntimeError(
            "Run마다 API 목록이 다릅니다. "
            "동일한 all-api-test.js로 실행했는지 확인하세요."
        )

api_keys = first_api_keys
api_names = runs[0]["apiNames"]


# ============================================================
# Run 간 Hold 목록 구성
#
# 첫 Run의 순서를 우선하고,
# 다른 Run에만 있는 Hold가 있다면 뒤에 붙인다.
# ============================================================

hold_names = []
hold_meta = {}

for run in runs:
    for window in run["holdWindows"]:
        name = window["name"]

        if name not in hold_meta:
            hold_names.append(name)
            hold_meta[name] = {
                "vu": window["vu"],
                "durations": [],
                "starts": [],
                "ends": [],
            }

        hold_meta[name]["durations"].append(window["duration"])
        hold_meta[name]["starts"].append(window["start"])
        hold_meta[name]["ends"].append(window["end"])


# ============================================================
# API 통합 결과
# ============================================================

aggregated = {}

for api_key in api_keys:
    aggregated[api_key] = {
        "name": api_names[api_key],
        "stages": {},
    }

    for hold_name in hold_names:
        avg_values = []
        p95_values = []
        p99_values = []
        rps_values = []
        request_count_values = []

        for run in runs:
            stage = (
                run["stages"]
                .get(api_key, {})
                .get("stages", {})
                .get(hold_name)
            )

            if stage is None:
                continue

            avg_values.append(stage["avg"])
            p95_values.append(stage["p95"])
            p99_values.append(stage["p99"])
            rps_values.append(stage["rps"])
            request_count_values.append(stage["requestCount"])

        if not p95_values:
            continue

        aggregated[api_key]["stages"][hold_name] = {
            "sampleRuns": len(p95_values),
            "avgMedian": statistics.median(avg_values),
            "p95Median": statistics.median(p95_values),
            "p95Min": min(p95_values),
            "p95Max": max(p95_values),
            "p99Median": statistics.median(p99_values),
            "p99Min": min(p99_values),
            "p99Max": max(p99_values),
            "rpsMedian": statistics.median(rps_values),
            "rpsMin": min(rps_values),
            "rpsMax": max(rps_values),
            "requestCountMedian": statistics.median(request_count_values),
        }


# ============================================================
# 전체 통합 Summary
# ============================================================

total_requests_all_runs = sum(run["totalRequests"] for run in runs)

weighted_failed_requests = sum(
    run["totalRequests"] * run["failRate"]
    for run in runs
)

overall_fail_rate = (
    weighted_failed_requests / total_requests_all_runs
    if total_requests_all_runs
    else 0
)

actual_peak_vu_all = max(
    (
        run["actualPeakVu"]
        for run in runs
        if run["actualPeakVu"] is not None
    ),
    default=None,
)

configured_max_vu_all = max(
    (
        run["configuredMaxVu"]
        for run in runs
        if run["configuredMaxVu"] is not None
    ),
    default=None,
)

pool_max_all = max(
    (
        run["poolMax"]
        for run in runs
        if run["poolMax"] is not None
    ),
    default=None,
)

peak_active_all = max(
    (
        run["peakActive"]
        for run in runs
        if run["peakActive"] is not None
    ),
    default=None,
)

minimum_idle_all = min(
    (
        run["minimumIdle"]
        for run in runs
        if run["minimumIdle"] is not None
    ),
    default=None,
)

max_pending_all = max(
    (
        run["maxPending"]
        for run in runs
        if run["maxPending"] is not None
    ),
    default=None,
)

pending_runs = sum(
    1
    for run in runs
    if (run["maxPending"] or 0) > 0
)

pending_samples_all = sum(
    run["pendingSamples"]
    for run in runs
)


# ============================================================
# Pending 여부에 따른 API Latency 비교
#
# 핵심:
# - Pending 구간의 API 응답시간과 Normal 구간을 비교한다.
# - 부하량 차이로 인한 왜곡을 줄이기 위해
#   "동일한 실제 VU"가 양쪽 모두 존재하는 VU만 비교한다.
# - 초별 평균을 다시 평균내지 않고 실제 request duration 원본을 합친다.
# ============================================================

pending_latency_raw = {
    api_key: {
        "normal": defaultdict(list),
        "pending": defaultdict(list),
    }
    for api_key in api_keys
}

for run in runs:
    source = run.get("latencyByPendingStateVu", {})

    for api_key in api_keys:
        api_source = source.get(api_key, {})

        for state in ("normal", "pending"):
            by_vu = api_source.get(state, {})

            for vu, durations in by_vu.items():
                pending_latency_raw[api_key][state][int(vu)].extend(durations)


pending_latency_comparison = {}

for api_key in api_keys:
    normal_by_vu = pending_latency_raw[api_key]["normal"]
    pending_by_vu = pending_latency_raw[api_key]["pending"]

    matched_vus = sorted(
        vu
        for vu in pending_by_vu.keys()
        if pending_by_vu.get(vu) and normal_by_vu.get(vu)
    )

    normal_durations = []
    pending_durations = []

    for vu in matched_vus:
        normal_durations.extend(normal_by_vu[vu])
        pending_durations.extend(pending_by_vu[vu])

    normal_avg = (
        statistics.mean(normal_durations)
        if normal_durations
        else None
    )

    pending_avg = (
        statistics.mean(pending_durations)
        if pending_durations
        else None
    )

    normal_p95 = percentile(normal_durations, 0.95)
    pending_p95 = percentile(pending_durations, 0.95)
    normal_p99 = percentile(normal_durations, 0.99)
    pending_p99 = percentile(pending_durations, 0.99)

    pending_latency_comparison[api_key] = {
        "name": api_names[api_key],
        "matchedVus": matched_vus,
        "normalAvg": normal_avg,
        "pendingAvg": pending_avg,
        "avgChange": change_rate(normal_avg, pending_avg),
        "normalP95": normal_p95,
        "pendingP95": pending_p95,
        "p95Change": change_rate(normal_p95, pending_p95),
        "normalP99": normal_p99,
        "pendingP99": pending_p99,
        "p99Change": change_rate(normal_p99, pending_p99),
        "normalCount": len(normal_durations),
        "pendingCount": len(pending_durations),
    }

# P95 변화율이 큰 API부터 보여준다.
# 비교 가능한 데이터가 없는 API는 마지막으로 보낸다.
pending_latency_order = sorted(
    api_keys,
    key=lambda api_key: (
        pending_latency_comparison[api_key]["p95Change"] is None,
        -(pending_latency_comparison[api_key]["p95Change"] or -10**9),
    ),
)


# ============================================================
# 전체 API Summary의 Run 중앙값
# ============================================================

overall_api_aggregated = {}

for api_key in api_keys:
    avg_values = []
    p95_values = []
    p99_values = []
    rps_values = []
    fail_rates = []
    request_counts = []

    for run in runs:
        value = run["summary"].get(api_key)

        if value is None:
            continue

        avg_values.append(value.get("avg", 0))
        p95_values.append(value.get("p95", 0))
        p99_values.append(value.get("p99", 0))
        rps_values.append(value.get("rps", 0))
        fail_rates.append(value.get("failRate", 0))
        request_counts.append(value.get("requestCount", 0))

    overall_api_aggregated[api_key] = {
        "name": api_names[api_key],
        "avgMedian": statistics.median(avg_values) if avg_values else None,
        "p95Median": statistics.median(p95_values) if p95_values else None,
        "p99Median": statistics.median(p99_values) if p99_values else None,
        "rpsMedian": statistics.median(rps_values) if rps_values else None,
        "failRateMedian": statistics.median(fail_rates) if fail_rates else None,
        "requestCountMedian": statistics.median(request_counts) if request_counts else None,
    }


# ============================================================
# 최대 P95 / P99 증가 API
# ============================================================

p95_changes = []
p99_changes = []

if len(hold_names) >= 2:
    first_hold = hold_names[0]
    last_hold = hold_names[-1]

    for api_key in api_keys:
        stages = aggregated[api_key]["stages"]
        first = stages.get(first_hold)
        last = stages.get(last_hold)

        if first is None or last is None:
            continue

        p95_change = change_rate(
            first["p95Median"],
            last["p95Median"],
        )

        p99_change = change_rate(
            first["p99Median"],
            last["p99Median"],
        )

        if p95_change is not None:
            p95_changes.append((p95_change, api_names[api_key]))

        if p99_change is not None:
            p99_changes.append((p99_change, api_names[api_key]))

max_p95_change = (
    max(p95_changes, key=lambda item: item[0])
    if p95_changes
    else (0, "-")
)

max_p99_change = (
    max(p99_changes, key=lambda item: item[0])
    if p99_changes
    else (0, "-")
)


# ============================================================
# HTML 조각: Hold Header
# ============================================================

hold_headers = "".join(
    f"<th>{html.escape(name)}</th>"
    for name in hold_names
)

first_hold_name = hold_names[0] if hold_names else "-"
last_hold_name = hold_names[-1] if hold_names else "-"


# ============================================================
# HTML 조각: Run Summary
# ============================================================

run_summary_rows = ""

for run in runs:
    status = (
        "Connection 대기 발생"
        if (run["maxPending"] or 0) > 0
        else "Connection 대기 없음"
    )

    report_link = (
        f'<a href="{html.escape(run["name"])}/report.html">상세 분석</a>'
        if run["detailReportExists"]
        else "-"
    )

    run_summary_rows += f"""
        <tr class="{'pending-row' if (run['maxPending'] or 0) > 0 else ''}">
            <td>{html.escape(run['name'])}</td>
            <td>{fmt_int(run['actualPeakVu'])}</td>
            <td>{fmt_int(run['configuredMaxVu'])}</td>
            <td>{run['testDurationSeconds']}s</td>
            <td>{run['totalRequests']:,}</td>
            <td>{run['failRate'] * 100:.2f}%</td>
            <td>{fmt(run['peakActive'], 0)}</td>
            <td>{fmt(run['minimumIdle'], 0)}</td>
            <td><strong>{fmt(run['maxPending'], 0)}</strong></td>
            <td>{run['pendingSamples']}</td>
            <td>{fmt(run['poolMax'], 0)}</td>
            <td>{status}</td>
            <td>{report_link}</td>
        </tr>
    """


# ============================================================
# HTML 조각: Pending Event
# ============================================================

pending_event_rows = ""

for run in runs:
    for event in run["pendingEvents"]:
        avg_text = "-"
        max_text = "-"

        if event["highestAvgApi"] is not None:
            avg_text = (
                f"{html.escape(event['highestAvgApi'])} "
                f"({event['highestAvgMs']:.2f} ms)"
            )

        if event["highestMaxApi"] is not None:
            max_text = (
                f"{html.escape(event['highestMaxApi'])} "
                f"({event['highestMaxMs']:.2f} ms)"
            )

        pending_event_rows += f"""
            <tr class="pending-row">
                <td>{html.escape(run['name'])}</td>
                <td>{event['elapsed']:.2f}s</td>
                <td>{fmt_int(event['vu'])}</td>
                <td>{fmt(event['active'], 0)}</td>
                <td>{fmt(event['idle'], 0)}</td>
                <td><strong>{fmt(event['pending'], 0)}</strong></td>
                <td>{fmt(event['poolMax'], 0)}</td>
                <td>{fmt(event['totalRps'], 2)}</td>
                <td>{avg_text}</td>
                <td>{max_text}</td>
                <td><a href="{html.escape(run['name'])}/report.html">Run 상세</a></td>
            </tr>
        """

if not pending_event_rows:
    pending_event_rows = """
        <tr>
            <td colspan="11">
                전체 Run에서 Pending &gt; 0 이벤트가 없습니다.
            </td>
        </tr>
    """


# ============================================================
# HTML 조각: Pending 유무에 따른 API Latency 비교
# ============================================================

pending_latency_rows = ""

for api_key in pending_latency_order:
    value = pending_latency_comparison[api_key]

    matched_vus_text = (
        ", ".join(str(vu) for vu in value["matchedVus"])
        if value["matchedVus"]
        else "-"
    )

    row_class = (
        "suspect-row"
        if (value["p95Change"] or 0) >= 20
        else ""
    )

    pending_latency_rows += f"""
        <tr class="{row_class}">
            <td>{html.escape(value['name'])}</td>
            <td>{html.escape(matched_vus_text)}</td>
            <td>{fmt(value['normalAvg'], 2, ' ms')}</td>
            <td>{fmt(value['pendingAvg'], 2, ' ms')}</td>
            <td>{change_text(value['normalAvg'], value['pendingAvg'])}</td>
            <td>{fmt(value['normalP95'], 2, ' ms')}</td>
            <td>{fmt(value['pendingP95'], 2, ' ms')}</td>
            <td><strong>{change_text(value['normalP95'], value['pendingP95'])}</strong></td>
            <td>{fmt(value['normalP99'], 2, ' ms')}</td>
            <td>{fmt(value['pendingP99'], 2, ' ms')}</td>
            <td>{change_text(value['normalP99'], value['pendingP99'])}</td>
            <td>{value['normalCount']:,}</td>
            <td>{value['pendingCount']:,}</td>
        </tr>
    """

if not any(
    pending_latency_comparison[api_key]["pendingCount"] > 0
    and pending_latency_comparison[api_key]["normalCount"] > 0
    for api_key in api_keys
):
    pending_latency_rows = """
        <tr>
            <td colspan="13">
                동일 VU에서 Normal / Pending 양쪽을 비교할 수 있는 데이터가 없습니다.
            </td>
        </tr>
    """


# ============================================================
# HTML 조각: Hold 탐지 결과
# ============================================================

hold_detect_rows = ""

for run in runs:
    if not run["holdWindows"]:
        hold_detect_rows += f"""
            <tr>
                <td>{html.escape(run['name'])}</td>
                <td colspan="5">자동 탐지 Hold 없음</td>
            </tr>
        """
        continue

    for window in run["holdWindows"]:
        hold_detect_rows += f"""
            <tr>
                <td>{html.escape(run['name'])}</td>
                <td>{html.escape(window['name'])}</td>
                <td>{window['vu']}</td>
                <td>{window['start']}s</td>
                <td>{window['end']}s</td>
                <td>{window['duration']}s</td>
            </tr>
        """


# ============================================================
# HTML 조각: P95 / P99 / RPS
# ============================================================

p95_rows = ""
p99_rows = ""
rps_rows = ""
variation_rows = ""

for api_key in api_keys:
    api = aggregated[api_key]
    stages = api["stages"]

    p95_values = []
    p99_values = []
    p95_cells = ""
    p99_cells = ""
    rps_cells = ""
    variation_cells = ""

    for hold_name in hold_names:
        stage = stages.get(hold_name)

        if stage is None:
            p95_values.append(None)
            p99_values.append(None)
            p95_cells += "<td>-</td>"
            p99_cells += "<td>-</td>"
            rps_cells += "<td>-</td>"
            variation_cells += "<td>-</td>"
            continue

        p95_values.append(stage["p95Median"])
        p99_values.append(stage["p99Median"])

        p95_cells += f"<td>{stage['p95Median']:.2f} ms</td>"
        p99_cells += f"<td>{stage['p99Median']:.2f} ms</td>"
        rps_cells += f"<td>{stage['rpsMedian']:.2f}</td>"

        diff = stage["p99Max"] - stage["p99Min"]

        variation_cells += f"""
            <td>
                {stage['p99Min']:.2f} ~ {stage['p99Max']:.2f} ms
                <br>
                <small>폭 {diff:.2f} ms</small>
            </td>
        """

    p95_change = (
        change_text(p95_values[0], p95_values[-1])
        if len(p95_values) >= 2
        else "-"
    )

    p99_change = (
        change_text(p99_values[0], p99_values[-1])
        if len(p99_values) >= 2
        else "-"
    )

    p95_rows += f"""
        <tr>
            <td>{html.escape(api['name'])}</td>
            {p95_cells}
            <td>{p95_change}</td>
        </tr>
    """

    p99_rows += f"""
        <tr>
            <td>{html.escape(api['name'])}</td>
            {p99_cells}
            <td>{p99_change}</td>
        </tr>
    """

    rps_rows += f"""
        <tr>
            <td>{html.escape(api['name'])}</td>
            {rps_cells}
        </tr>
    """

    variation_rows += f"""
        <tr>
            <td>{html.escape(api['name'])}</td>
            {variation_cells}
        </tr>
    """

if not hold_names:
    hold_headers = "<th>Hold 없음</th>"
    p95_rows = p99_rows = rps_rows = variation_rows = """
        <tr>
            <td colspan="4">
                실제 VU에서 안정적으로 유지된 Hold 구간을 찾지 못했습니다.
            </td>
        </tr>
    """


# ============================================================
# HTML 조각: 전체 API Summary 중앙값
# ============================================================

overall_api_rows = ""

for api_key in api_keys:
    value = overall_api_aggregated[api_key]

    overall_api_rows += f"""
        <tr>
            <td>{html.escape(value['name'])}</td>
            <td>{fmt(value['avgMedian'], 2, ' ms')}</td>
            <td>{fmt(value['p95Median'], 2, ' ms')}</td>
            <td>{fmt(value['p99Median'], 2, ' ms')}</td>
            <td>{fmt(value['rpsMedian'], 2)}</td>
            <td>{fmt((value['failRateMedian'] or 0) * 100, 2, '%')}</td>
            <td>{fmt_int(value['requestCountMedian'])}</td>
        </tr>
    """


# ============================================================
# HTML 조각: Run별 API Hold 상세
# ============================================================

api_detail_rows = ""

for run in runs:
    for api_key in api_keys:
        for hold_name in hold_names:
            stage = (
                run["stages"]
                .get(api_key, {})
                .get("stages", {})
                .get(hold_name)
            )

            if stage is None:
                continue

            api_detail_rows += f"""
                <tr>
                    <td>{html.escape(run['name'])}</td>
                    <td>{html.escape(hold_name)}</td>
                    <td>{html.escape(api_names[api_key])}</td>
                    <td>{stage['avg']:.2f} ms</td>
                    <td>{stage['p95']:.2f} ms</td>
                    <td>{stage['p99']:.2f} ms</td>
                    <td>{stage['rps']:.2f}</td>
                    <td>{stage['requestCount']:,}</td>
                </tr>
            """


# ============================================================
# HTML 조각: Run별 Hikari Hold 상세
# ============================================================

hikari_detail_rows = ""

for run in runs:
    for hold_name in hold_names:
        value = run["hikariStages"].get(hold_name)

        if value is None:
            continue

        status = (
            "Connection 대기 발생"
            if (value["maxPending"] or 0) > 0
            else "Connection 대기 없음"
        )

        hikari_detail_rows += f"""
            <tr class="{'pending-row' if (value['maxPending'] or 0) > 0 else ''}">
                <td>{html.escape(run['name'])}</td>
                <td>{html.escape(hold_name)}</td>
                <td>{fmt(value['maxActive'], 0)}</td>
                <td>{fmt(value['minIdle'], 0)}</td>
                <td><strong>{fmt(value['maxPending'], 0)}</strong></td>
                <td>{value['pendingSamples']}</td>
                <td>{fmt(value['poolMax'], 0)}</td>
                <td>{status}</td>
            </tr>
        """


# ============================================================
# Chart Dataset
# ============================================================

COLORS = [
    "#2563eb",
    "#dc2626",
    "#16a34a",
    "#9333ea",
    "#ea580c",
    "#0891b2",
    "#4f46e5",
    "#be123c",
    "#65a30d",
    "#7c3aed",
    "#0f766e",
    "#b45309",
]

run_labels = [run["name"] for run in runs]

hikari_chart_datasets = [
    chart_dataset(
        "Peak Active",
        [run["peakActive"] for run in runs],
        "#2563eb",
    ),
    chart_dataset(
        "Max Pending",
        [run["maxPending"] for run in runs],
        "#dc2626",
    ),
    chart_dataset(
        "Pool Max",
        [run["poolMax"] for run in runs],
        "#111827",
    ),
]

p95_chart_datasets = []
p99_chart_datasets = []
rps_chart_datasets = []

pending_latency_labels = [
    pending_latency_comparison[api_key]["name"]
    for api_key in pending_latency_order
]

pending_latency_chart_datasets = [
    {
        "label": "Normal P95",
        "data": [
            pending_latency_comparison[api_key]["normalP95"]
            for api_key in pending_latency_order
        ],
        "backgroundColor": "#2563eb",
        "borderColor": "#2563eb",
        "borderWidth": 1,
    },
    {
        "label": "Pending P95",
        "data": [
            pending_latency_comparison[api_key]["pendingP95"]
            for api_key in pending_latency_order
        ],
        "backgroundColor": "#dc2626",
        "borderColor": "#dc2626",
        "borderWidth": 1,
    },
]

for index, api_key in enumerate(api_keys):
    color = COLORS[index % len(COLORS)]
    stages = aggregated[api_key]["stages"]

    p95_chart_datasets.append(
        chart_dataset(
            api_names[api_key],
            [
                stages.get(name, {}).get("p95Median")
                for name in hold_names
            ],
            color,
        )
    )

    p99_chart_datasets.append(
        chart_dataset(
            api_names[api_key],
            [
                stages.get(name, {}).get("p99Median")
                for name in hold_names
            ],
            color,
        )
    )

    rps_chart_datasets.append(
        chart_dataset(
            api_names[api_key],
            [
                stages.get(name, {}).get("rpsMedian")
                for name in hold_names
            ],
            color,
        )
    )


# ============================================================
# HTML
# ============================================================

report = f"""
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>TransferTracker Performance Comparison</title>
<script src="https://cdn.jsdelivr.net/npm/chart.js"></script>

<style>
body {{
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Arial, sans-serif;
    max-width: 1500px;
    margin: 40px auto;
    padding: 0 24px 80px;
    color: #222;
    background: #fff;
    line-height: 1.5;
}}

h1 {{ margin-bottom: 8px; }}
h2 {{ margin-top: 54px; margin-bottom: 12px; }}
.description {{ color: #666; margin-top: 0; margin-bottom: 20px; }}

.warning {{
    border: 1px solid #d97706;
    border-radius: 10px;
    padding: 14px 16px;
    background: #fff7ed;
    margin: 18px 0 24px;
}}

.cards {{
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
    gap: 14px;
    margin: 24px 0 36px;
}}

.card {{
    border: 1px solid #ddd;
    border-radius: 10px;
    padding: 18px;
}}

.card-title {{ font-size: 14px; color: #666; margin-bottom: 6px; }}
.card-value {{ font-size: 23px; font-weight: 700; }}

.chart-container {{
    height: 430px;
    margin: 18px 0 50px;
    border: 1px solid #ddd;
    border-radius: 10px;
    padding: 14px;
}}

.table-wrapper {{
    overflow-x: auto;
    margin-bottom: 42px;
    border: 1px solid #ddd;
    border-radius: 10px;
}}

table {{ width: 100%; border-collapse: collapse; min-width: 900px; }}

th, td {{
    padding: 10px 12px;
    border-bottom: 1px solid #ddd;
    text-align: right;
    white-space: nowrap;
}}

th {{
    background: #f5f5f5;
    position: sticky;
    top: 0;
    z-index: 1;
}}

th:first-child, td:first-child {{ text-align: left; }}
tbody tr:hover {{ background: #fafafa; }}
.pending-row {{ background: #fff1f2; }}
.pending-row:hover {{ background: #ffe4e6; }}
.suspect-row {{ background: #fff7ed; }}
.suspect-row:hover {{ background: #ffedd5; }}

a {{ color: #2563eb; text-decoration: none; font-weight: 600; }}
a:hover {{ text-decoration: underline; }}

details {{
    margin: 22px 0 45px;
    border: 1px solid #ddd;
    border-radius: 10px;
    padding: 16px;
}}

summary {{ cursor: pointer; font-size: 17px; font-weight: 650; }}
details .table-wrapper {{ margin-top: 18px; margin-bottom: 0; }}
small, .small {{ color: #777; }}
</style>
</head>

<body>

<h1>TransferTracker 성능 비교 리포트</h1>

<p class="description">
    {RUN_COUNT}개의 Run을 비교합니다.
    각 Run의 실제 VU, API 성능, HikariCP 상태를 CSV에서 동적으로 계산합니다.
</p>

<div class="warning">
    <strong>Hikari 해석 주의:</strong>
    현재 Collector는 active / idle / pending / max를 각각 별도 HTTP 요청으로 수집합니다.
    따라서 하나의 CSV row에 있는 값들이 완전히 동일한 찰나의 원자적 snapshot이라고 볼 수 없습니다.
    Pending 발생 여부와 반복되는 시간대 추세를 중심으로 해석하세요.
</div>

<div class="cards">
    <div class="card">
        <div class="card-title">Run 수</div>
        <div class="card-value">{len(runs)}</div>
    </div>

    <div class="card">
        <div class="card-title">실제 Peak VU</div>
        <div class="card-value">{fmt_int(actual_peak_vu_all)}</div>
    </div>

    <div class="card">
        <div class="card-title">k6 Configured Max VU</div>
        <div class="card-value">{fmt_int(configured_max_vu_all)}</div>
    </div>

    <div class="card">
        <div class="card-title">전체 Run 요청 수</div>
        <div class="card-value">{total_requests_all_runs:,}</div>
    </div>

    <div class="card">
        <div class="card-title">전체 실패율</div>
        <div class="card-value">{overall_fail_rate * 100:.2f}%</div>
    </div>

    <div class="card">
        <div class="card-title">Hikari Pool Max</div>
        <div class="card-value">{fmt(pool_max_all, 0)}</div>
    </div>

    <div class="card">
        <div class="card-title">Peak Active</div>
        <div class="card-value">{fmt(peak_active_all, 0)}</div>
    </div>

    <div class="card">
        <div class="card-title">Minimum Idle</div>
        <div class="card-value">{fmt(minimum_idle_all, 0)}</div>
    </div>

    <div class="card">
        <div class="card-title">Worst Max Pending</div>
        <div class="card-value">{fmt(max_pending_all, 0)}</div>
    </div>

    <div class="card">
        <div class="card-title">Pending 발생 Run</div>
        <div class="card-value">{pending_runs} / {len(runs)}</div>
    </div>

    <div class="card">
        <div class="card-title">Pending Samples</div>
        <div class="card-value">{pending_samples_all}</div>
    </div>

    <div class="card">
        <div class="card-title">최대 P95 증가</div>
        <div class="card-value">{max_p95_change[0]:+.1f}%</div>
        <div>{html.escape(max_p95_change[1])}</div>
    </div>

    <div class="card">
        <div class="card-title">최대 P99 증가</div>
        <div class="card-value">{max_p99_change[0]:+.1f}%</div>
        <div>{html.escape(max_p99_change[1])}</div>
    </div>
</div>

<h2>1. Run별 핵심 상태</h2>
<p class="description">
    먼저 어느 Run에서 Connection 대기가 발생했는지 확인하세요.
    "상세 분석"을 누르면 해당 Run의 초 단위 report.html로 이동합니다.
</p>

<div class="table-wrapper">
<table>
<thead>
<tr>
    <th>Run</th>
    <th>Peak VU</th>
    <th>Configured Max</th>
    <th>테스트 시간</th>
    <th>요청 수</th>
    <th>실패율</th>
    <th>Peak Active</th>
    <th>Min Idle</th>
    <th>Max Pending</th>
    <th>Pending Samples</th>
    <th>Pool Max</th>
    <th>상태</th>
    <th>상세</th>
</tr>
</thead>
<tbody>
{run_summary_rows}
</tbody>
</table>
</div>

<h2>2. Run별 HikariCP 비교</h2>
<p class="description">
    Peak Active / Max Pending / Pool Max를 Run별로 비교합니다.
</p>

<div class="chart-container">
    <canvas id="hikariRunChart"></canvas>
</div>

<h2>3. 전체 Pending 발생 시점</h2>
<p class="description">
    모든 Run의 Pending &gt; 0 sample을 한 곳에 모았습니다.
    같은 초의 실제 VU, Total RPS, 가장 높은 AVG/MAX API도 함께 보여줍니다.
</p>

<div class="table-wrapper">
<table>
<thead>
<tr>
    <th>Run</th>
    <th>Time</th>
    <th>VU</th>
    <th>Active</th>
    <th>Idle</th>
    <th>Pending</th>
    <th>Pool Max</th>
    <th>Total RPS</th>
    <th>Highest AVG API</th>
    <th>Highest MAX API</th>
    <th>상세</th>
</tr>
</thead>
<tbody>
{pending_event_rows}
</tbody>
</table>
</div>

<h2>4. Pending 유무에 따른 API 응답시간 비교</h2>
<p class="description">
    Pending이 관측된 순간의 API latency가 평상시보다 실제로 악화되는지 비교합니다.
    부하량 차이로 인한 왜곡을 줄이기 위해 <strong>Pending이 발생한 실제 VU와 동일한 VU의 Normal 구간만</strong> 비교합니다.
    초별 AVG를 평균내지 않고 실제 request duration 원본을 합쳐 AVG / P95 / P99를 계산합니다.
</p>

<div class="warning">
    <strong>해석 주의:</strong> 이 표는 Pending과 latency 상승의 <strong>상관관계</strong>를 보여줍니다.
    어떤 API가 Connection을 오래 점유해서 Pending을 만들었다는 인과관계를 직접 증명하지는 않습니다.
    또한 Hikari는 약 1초 간격의 sampling이므로 "Normal"은 해당 sample에서 Pending이 관측되지 않았다는 의미입니다.
</div>

<div class="chart-container">
    <canvas id="pendingLatencyChart"></canvas>
</div>

<div class="table-wrapper">
<table>
<thead>
<tr>
    <th>API</th>
    <th>비교 VU</th>
    <th>Normal AVG</th>
    <th>Pending AVG</th>
    <th>AVG 변화</th>
    <th>Normal P95</th>
    <th>Pending P95</th>
    <th>P95 변화</th>
    <th>Normal P99</th>
    <th>Pending P99</th>
    <th>P99 변화</th>
    <th>Normal 요청</th>
    <th>Pending 요청</th>
</tr>
</thead>
<tbody>
{pending_latency_rows}
</tbody>
</table>
</div>

<h2>5. 실제 VU에서 자동 탐지한 Hold</h2>
<p class="description">
    VU_1 / VU_2 / VU_3 환경변수나 고정 시간표를 사용하지 않습니다.
    각 Run의 실제 k6 vus metric에서 {MIN_HOLD_SECONDS}초 이상 동일 VU가 유지된 구간을 찾습니다.
</p>

<div class="table-wrapper">
<table>
<thead>
<tr>
    <th>Run</th>
    <th>Hold</th>
    <th>VU</th>
    <th>Start</th>
    <th>End</th>
    <th>Duration</th>
</tr>
</thead>
<tbody>
{hold_detect_rows}
</tbody>
</table>
</div>

<h2>6. Hold별 P95 중앙값</h2>
<p class="description">
    동일 Hold에서 각 Run의 P95를 구한 뒤 중앙값을 비교합니다.
</p>

<div class="chart-container">
    <canvas id="p95Chart"></canvas>
</div>

<div class="table-wrapper">
<table>
<thead>
<tr>
    <th>API</th>
    {hold_headers}
    <th>{html.escape(first_hold_name)} → {html.escape(last_hold_name)}</th>
</tr>
</thead>
<tbody>
{p95_rows}
</tbody>
</table>
</div>

<h2>7. Hold별 P99 중앙값</h2>
<p class="description">
    Tail latency가 부하 증가에 따라 어떻게 움직이는지 확인합니다.
</p>

<div class="chart-container">
    <canvas id="p99Chart"></canvas>
</div>

<div class="table-wrapper">
<table>
<thead>
<tr>
    <th>API</th>
    {hold_headers}
    <th>{html.escape(first_hold_name)} → {html.escape(last_hold_name)}</th>
</tr>
</thead>
<tbody>
{p99_rows}
</tbody>
</table>
</div>

<h2>8. Hold별 RPS 중앙값</h2>
<p class="description">
    VU가 올라갈 때 처리량이 같이 증가하는지 확인합니다.
</p>

<div class="chart-container">
    <canvas id="rpsChart"></canvas>
</div>

<div class="table-wrapper">
<table>
<thead>
<tr>
    <th>API</th>
    {hold_headers}
</tr>
</thead>
<tbody>
{rps_rows}
</tbody>
</table>
</div>

<h2>9. 전체 테스트 API 중앙값</h2>
<p class="description">
    Ramp / Hold / Ramp-down 전체를 포함한 각 Run의 all-api-result.json 값을 Run 간 중앙값으로 집계합니다.
</p>

<div class="table-wrapper">
<table>
<thead>
<tr>
    <th>API</th>
    <th>AVG Median</th>
    <th>P95 Median</th>
    <th>P99 Median</th>
    <th>RPS Median</th>
    <th>Fail Rate Median</th>
    <th>Request Count Median</th>
</tr>
</thead>
<tbody>
{overall_api_rows}
</tbody>
</table>
</div>

<h2>10. P99 Run별 변동폭</h2>
<p class="description">
    동일 VU Hold에서 Run 간 P99 최소~최대 범위를 보여줍니다.
    변동폭이 크다면 JVM / DB / 네트워크 / 로컬 환경 변동을 추가로 확인할 수 있습니다.
</p>

<div class="table-wrapper">
<table>
<thead>
<tr>
    <th>API</th>
    {hold_headers}
</tr>
</thead>
<tbody>
{variation_rows}
</tbody>
</table>
</div>

<details>
<summary>Run별 HikariCP Hold 상세</summary>
<div class="table-wrapper">
<table>
<thead>
<tr>
    <th>Run</th>
    <th>Hold</th>
    <th>Max Active</th>
    <th>Min Idle</th>
    <th>Max Pending</th>
    <th>Pending Samples</th>
    <th>Pool Max</th>
    <th>상태</th>
</tr>
</thead>
<tbody>
{hikari_detail_rows}
</tbody>
</table>
</div>
</details>

<details>
<summary>Run별 API Hold 상세</summary>
<div class="table-wrapper">
<table>
<thead>
<tr>
    <th>Run</th>
    <th>Hold</th>
    <th>API</th>
    <th>AVG</th>
    <th>P95</th>
    <th>P99</th>
    <th>RPS</th>
    <th>요청 수</th>
</tr>
</thead>
<tbody>
{api_detail_rows}
</tbody>
</table>
</div>
</details>

<script>
const common = {{
    responsive: true,
    maintainAspectRatio: false,
    interaction: {{
        mode: 'index',
        intersect: false
    }},
    plugins: {{
        legend: {{
            position: 'top'
        }}
    }},
    scales: {{
        y: {{
            beginAtZero: true
        }}
    }}
}};

new Chart(
    document.getElementById('pendingLatencyChart'),
    {{
        type: 'bar',
        data: {{
            labels: {js(pending_latency_labels)},
            datasets: {js(pending_latency_chart_datasets)}
        }},
        options: {{
            responsive: true,
            maintainAspectRatio: false,
            interaction: {{
                mode: 'index',
                intersect: false
            }},
            plugins: {{
                legend: {{
                    position: 'top'
                }}
            }},
            scales: {{
                y: {{
                    beginAtZero: true,
                    title: {{
                        display: true,
                        text: 'P95 Response Time (ms)'
                    }}
                }}
            }}
        }}
    }}
);

new Chart(
    document.getElementById('hikariRunChart'),
    {{
        type: 'bar',
        data: {{
            labels: {js(run_labels)},
            datasets: {js(hikari_chart_datasets)}
        }},
        options: common
    }}
);

new Chart(
    document.getElementById('p95Chart'),
    {{
        type: 'line',
        data: {{
            labels: {js(hold_names)},
            datasets: {js(p95_chart_datasets)}
        }},
        options: {{
            ...common,
            scales: {{
                y: {{
                    beginAtZero: true,
                    title: {{
                        display: true,
                        text: 'P95 (ms)'
                    }}
                }}
            }}
        }}
    }}
);

new Chart(
    document.getElementById('p99Chart'),
    {{
        type: 'line',
        data: {{
            labels: {js(hold_names)},
            datasets: {js(p99_chart_datasets)}
        }},
        options: {{
            ...common,
            scales: {{
                y: {{
                    beginAtZero: true,
                    title: {{
                        display: true,
                        text: 'P99 (ms)'
                    }}
                }}
            }}
        }}
    }}
);

new Chart(
    document.getElementById('rpsChart'),
    {{
        type: 'line',
        data: {{
            labels: {js(hold_names)},
            datasets: {js(rps_chart_datasets)}
        }},
        options: {{
            ...common,
            scales: {{
                y: {{
                    beginAtZero: true,
                    title: {{
                        display: true,
                        text: 'Requests / Second'
                    }}
                }}
            }}
        }}
    }}
);
</script>

</body>
</html>
"""


# ============================================================
# HTML 저장
# ============================================================

REPORT_FILE.parent.mkdir(
    parents=True,
    exist_ok=True,
)

with open(
    REPORT_FILE,
    "w",
    encoding="utf-8",
) as file:
    file.write(report)


# ============================================================
# Terminal Summary
# ============================================================

print()
print("Comparison report generated:")
print(REPORT_FILE)
print()
print(f"Runs                : {len(runs)}")
print(f"Actual peak VU      : {fmt_int(actual_peak_vu_all)}")
print(f"Configured max VU   : {fmt_int(configured_max_vu_all)}")
print(f"Worst max pending   : {fmt(max_pending_all, 0)}")
print(f"Pending runs        : {pending_runs} / {len(runs)}")
print(f"Pending samples     : {pending_samples_all}")
print(f"Detected holds      : {', '.join(hold_names) if hold_names else '-'}")
print()
