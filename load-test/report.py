import os
import csv
import json
from collections import defaultdict
from pathlib import Path


# ============================================================
# 기본 설정
# ============================================================

RESULT_DIR = Path(
    os.getenv(
        "RESULT_DIR",
        "load-test/results/local"
    )
)

SUMMARY_FILE = RESULT_DIR / "all-api-result.json"
METRICS_FILE = RESULT_DIR / "all-api-metrics.csv"
REPORT_FILE = RESULT_DIR / "report.html"

BUCKET_SECONDS = 5


# ============================================================
# VU 유지 구간
# ============================================================

VU_1 = int(
    os.getenv(
        "VU_1",
        "10"
    )
)

VU_2 = int(
    os.getenv(
        "VU_2",
        "25"
    )
)

VU_3 = int(
    os.getenv(
        "VU_3",
        "30"
    )
)


HOLD_WINDOWS = [
    {
        "name": f"{VU_1} VU",
        "start": 10,
        "end": 30,
    },
    {
        "name": f"{VU_2} VU",
        "start": 40,
        "end": 70,
    },
    {
        "name": f"{VU_3} VU",
        "start": 80,
        "end": 110,
    },
]


# ============================================================
# 1. Summary JSON 읽기
# ============================================================

with open(SUMMARY_FILE, encoding="utf-8") as f:
    summary = json.load(f)


# ============================================================
# 2. CSV 읽기
# ============================================================

rows = []

with open(METRICS_FILE, encoding="utf-8") as f:
    reader = csv.DictReader(f)

    for row in reader:

        if not row["extra_tags"]:
            continue

        api = None

        for tag in row["extra_tags"].split(","):

            key, _, value = tag.partition("=")

            if key == "api":
                api = value
                break

        if api is None:
            continue

        rows.append({
            "metric": row["metric_name"],
            "timestamp": int(row["timestamp"]),
            "value": float(row["metric_value"]),
            "api": api,
        })


if not rows:
    raise RuntimeError(
        "CSV에서 API metric을 찾을 수 없습니다."
    )


start_time = min(
    row["timestamp"]
    for row in rows
)


# ============================================================
# 3. Percentile 계산 함수
# ============================================================

def percentile(values, percentile_value):

    if not values:
        return None

    values = sorted(values)

    index = (
                    len(values) - 1
            ) * percentile_value

    lower = int(index)

    upper = min(
        lower + 1,
        len(values) - 1
    )

    fraction = index - lower

    return (
            values[lower]
            + (
                    values[upper]
                    - values[lower]
            ) * fraction
    )


# ============================================================
# 4. RPS - 1초 단위
# ============================================================

rps_by_time = defaultdict(
    lambda: defaultdict(float)
)


for row in rows:

    if row["metric"] != "http_reqs":
        continue

    elapsed = (
            row["timestamp"]
            - start_time
    )

    rps_by_time[elapsed][row["api"]] += (
        row["value"]
    )


rps_times = sorted(
    rps_by_time.keys()
)


rps_datasets = []

for api_key, api_summary in summary.items():

    values = [
        rps_by_time[t].get(
            api_key,
            0
        )
        for t in rps_times
    ]

    rps_datasets.append({
        "label": api_summary["name"],
        "data": values,
        "fill": False,
        "tension": 0.2,
    })


# ============================================================
# 5. P95 응답시간 - 5초 단위
# ============================================================

duration_buckets = defaultdict(
    lambda: defaultdict(list)
)


for row in rows:

    if row["metric"] != "http_req_duration":
        continue

    elapsed = (
            row["timestamp"]
            - start_time
    )

    bucket = (
                     elapsed // BUCKET_SECONDS
             ) * BUCKET_SECONDS

    duration_buckets[bucket][
        row["api"]
    ].append(
        row["value"]
    )


duration_times = sorted(
    duration_buckets.keys()
)


p95_datasets = []

for api_key, api_summary in summary.items():

    values = []

    for timestamp in duration_times:

        durations = (
            duration_buckets[
                timestamp
            ].get(
                api_key,
                []
            )
        )

        values.append(
            percentile(
                durations,
                0.95
            )
            if durations
            else None
        )

    p95_datasets.append({
        "label": api_summary["name"],
        "data": values,
        "fill": False,
        "tension": 0.2,
    })


# ============================================================
# 6. VU Hold 구간별 성능 분석
# ============================================================

hold_results = []


for window in HOLD_WINDOWS:

    start = window["start"]
    end = window["end"]

    for api_key, api_summary in summary.items():

        durations = [
            row["value"]
            for row in rows
            if (
                    row["api"] == api_key
                    and
                    row["metric"] == "http_req_duration"
                    and
                    start
                    <= (
                            row["timestamp"]
                            - start_time
                    )
                    < end
            )
        ]


        request_count = sum(
            row["value"]
            for row in rows
            if (
                    row["api"] == api_key
                    and
                    row["metric"] == "http_reqs"
                    and
                    start
                    <= (
                            row["timestamp"]
                            - start_time
                    )
                    < end
            )
        )


        if not durations:
            continue


        duration_seconds = (
                end - start
        )


        hold_results.append({
            "stage":
                window["name"],

            "apiKey":
                api_key,

            "api":
                api_summary["name"],

            "avg":
                sum(durations)
                / len(durations),

            "p95":
                percentile(
                    durations,
                    0.95
                ),

            "p99":
                percentile(
                    durations,
                    0.99
                ),

            "rps":
                request_count
                / duration_seconds,

            "requestCount":
                int(request_count),
        })


# ============================================================
# 7. API → VU 형태로 재구성
# ============================================================

hold_by_api = defaultdict(dict)


for value in hold_results:

    hold_by_api[
        value["apiKey"]
    ][
        value["stage"]
    ] = value


stage_names = [
    window["name"]
    for window in HOLD_WINDOWS
]


first_stage = stage_names[0]
last_stage = stage_names[-1]


# ============================================================
# 8. 변화율 계산
# ============================================================

def change_rate(before, after):

    if before is None:
        return None

    if after is None:
        return None

    if before == 0:
        return None

    return (
            (after - before)
            / before
    ) * 100


def change_text(before, after):

    rate = change_rate(
        before,
        after
    )

    if rate is None:
        return "-"

    sign = "+" if rate >= 0 else ""

    return (
        f"{sign}{rate:.1f}%"
    )


# ============================================================
# 9. 전체 Summary Table
# ============================================================

summary_table_rows = ""


for api_key, value in summary.items():

    summary_table_rows += f"""
        <tr>
            <td>{value['name']}</td>
            <td>{value['avg']:.2f} ms</td>
            <td>{value['p95']:.2f} ms</td>
            <td>{value['p99']:.2f} ms</td>
            <td>{value['rps']:.2f}</td>
            <td>{value['failRate'] * 100:.2f}%</td>
            <td>{value['requestCount']:,}</td>
        </tr>
    """


# ============================================================
# 10. P95 비교 Table
# ============================================================

p95_table_rows = ""


for api_key, api_summary in summary.items():

    values = hold_by_api.get(
        api_key,
        {}
    )

    stage_values = []

    for stage_name in stage_names:

        value = values.get(
            stage_name
        )

        if value is None:
            stage_values.append(None)

        else:
            stage_values.append(
                value["p95"]
            )


    before = stage_values[0]
    after = stage_values[-1]


    cells = ""

    for value in stage_values:

        if value is None:
            cells += "<td>-</td>"
        else:
            cells += (
                f"<td>{value:.2f} ms</td>"
            )


    p95_table_rows += f"""
        <tr>
            <td>{api_summary['name']}</td>
            {cells}
            <td>{change_text(before, after)}</td>
        </tr>
    """


# ============================================================
# 11. P99 비교 Table
# ============================================================

p99_table_rows = ""


for api_key, api_summary in summary.items():

    values = hold_by_api.get(
        api_key,
        {}
    )

    stage_values = []

    for stage_name in stage_names:

        value = values.get(
            stage_name
        )

        if value is None:
            stage_values.append(None)

        else:
            stage_values.append(
                value["p99"]
            )


    before = stage_values[0]
    after = stage_values[-1]


    cells = ""

    for value in stage_values:

        if value is None:
            cells += "<td>-</td>"
        else:
            cells += (
                f"<td>{value:.2f} ms</td>"
            )


    p99_table_rows += f"""
        <tr>
            <td>{api_summary['name']}</td>
            {cells}
            <td>{change_text(before, after)}</td>
        </tr>
    """


# ============================================================
# 12. RPS 비교 Table
# ============================================================

rps_table_rows = ""


for api_key, api_summary in summary.items():

    values = hold_by_api.get(
        api_key,
        {}
    )

    stage_values = []

    for stage_name in stage_names:

        value = values.get(
            stage_name
        )

        if value is None:
            stage_values.append(None)

        else:
            stage_values.append(
                value["rps"]
            )


    cells = ""

    for value in stage_values:

        if value is None:
            cells += "<td>-</td>"
        else:
            cells += (
                f"<td>{value:.2f}</td>"
            )


    rps_table_rows += f"""
        <tr>
            <td>{api_summary['name']}</td>
            {cells}
        </tr>
    """


# ============================================================
# 13. 상세 Hold Table
# ============================================================

hold_detail_rows = ""


for value in hold_results:

    hold_detail_rows += f"""
        <tr>
            <td>{value['stage']}</td>
            <td>{value['api']}</td>
            <td>{value['avg']:.2f} ms</td>
            <td>{value['p95']:.2f} ms</td>
            <td>{value['p99']:.2f} ms</td>
            <td>{value['rps']:.2f}</td>
            <td>{value['requestCount']:,}</td>
        </tr>
    """


# ============================================================
# 14. 전체 테스트 요약 정보
# ============================================================

total_requests = sum(
    value["requestCount"]
    for value in summary.values()
)


estimated_failed_requests = sum(
    value["requestCount"]
    * value["failRate"]
    for value in summary.values()
)


overall_fail_rate = (
    estimated_failed_requests
    / total_requests
    if total_requests > 0
    else 0
)


max_p95_api_key = max(
    summary,
    key=lambda key:
    summary[key]["p95"]
)


max_p95_api = (
    summary[max_p95_api_key]
)


max_p99_api_key = max(
    summary,
    key=lambda key:
    summary[key]["p99"]
)


max_p99_api = (
    summary[max_p99_api_key]
)


# ============================================================
# 15. Stage Header 생성
# ============================================================

stage_header_html = "".join(
    f"<th>{stage}</th>"
    for stage in stage_names
)


# ============================================================
# 16. HTML
# ============================================================

html = f"""
<!DOCTYPE html>

<html lang="ko">

<head>

    <meta charset="UTF-8">

    <title>
        k6 API Performance Report
    </title>

    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>


    <style>

        body {{
            font-family:
                -apple-system,
                BlinkMacSystemFont,
                "Segoe UI",
                Arial,
                sans-serif;

            max-width: 1400px;

            margin: 40px auto;

            padding: 0 24px;

            color: #222;
        }}


        h1 {{
            margin-bottom: 30px;
        }}


        h2 {{
            margin-top: 50px;

            margin-bottom: 18px;
        }}


        .description {{
            color: #666;

            margin-top: -8px;

            margin-bottom: 20px;
        }}


        .summary-cards {{
            display: grid;

            grid-template-columns:
                repeat(
                    auto-fit,
                    minmax(220px, 1fr)
                );

            gap: 16px;

            margin-bottom: 40px;
        }}


        .summary-card {{
            border: 1px solid #ddd;

            border-radius: 10px;

            padding: 20px;

            background: #fff;
        }}


        .summary-card-title {{
            font-size: 14px;

            color: #666;

            margin-bottom: 8px;
        }}


        .summary-card-value {{
            font-size: 24px;

            font-weight: 700;
        }}


        .table-wrapper {{
            overflow-x: auto;

            margin-bottom: 50px;
        }}


        table {{
            width: 100%;

            border-collapse: collapse;

            min-width: 850px;
        }}


        th,
        td {{
            padding: 12px 14px;

            border-bottom:
                1px solid #ddd;

            text-align: right;

            white-space: nowrap;
        }}


        th {{
            background: #f5f5f5;

            font-weight: 600;
        }}


        th:first-child,
        td:first-child {{
            text-align: left;
        }}


        tbody tr:hover {{
            background: #fafafa;
        }}


        .metric-table
        td:first-child {{
            font-weight: 500;
        }}


        .chart-container {{
            height: 500px;

            margin-bottom: 60px;
        }}


        details {{
            margin-top: 30px;

            margin-bottom: 50px;

            border: 1px solid #ddd;

            border-radius: 8px;

            padding: 16px;
        }}


        summary {{
            cursor: pointer;

            font-weight: 600;

            font-size: 18px;
        }}


        details .table-wrapper {{
            margin-top: 20px;

            margin-bottom: 0;
        }}

    </style>

</head>


<body>


<h1>
    API 성능 테스트 결과
</h1>


<!-- ===================================================== -->
<!-- 핵심 요약 -->
<!-- ===================================================== -->

<h2>
    테스트 요약
</h2>


<div class="summary-cards">

    <div class="summary-card">

        <div class="summary-card-title">
            최대 VU
        </div>

        <div class="summary-card-value">
            30 VU
        </div>

    </div>


    <div class="summary-card">

        <div class="summary-card-title">
            전체 요청 수
        </div>

        <div class="summary-card-value">
            {total_requests:,}
        </div>

    </div>


    <div class="summary-card">

        <div class="summary-card-title">
            전체 실패율
        </div>

        <div class="summary-card-value">
            {overall_fail_rate * 100:.2f}%
        </div>

    </div>


    <div class="summary-card">

        <div class="summary-card-title">
            가장 높은 전체 P95
        </div>

        <div class="summary-card-value">
            {max_p95_api['p95']:.2f} ms
        </div>

        <div>
            {max_p95_api['name']}
        </div>

    </div>


    <div class="summary-card">

        <div class="summary-card-title">
            가장 높은 전체 P99
        </div>

        <div class="summary-card-value">
            {max_p99_api['p99']:.2f} ms
        </div>

        <div>
            {max_p99_api['name']}
        </div>

    </div>

</div>


<!-- ===================================================== -->
<!-- P95 -->
<!-- ===================================================== -->

<h2>
    P95 응답시간 비교
</h2>

<p class="description">
    동일 API의 부하 증가에 따른 응답시간 변화를 비교합니다.
    변화율은 {first_stage} 대비 {last_stage} 기준입니다.
</p>


<div class="table-wrapper">

<table class="metric-table">

    <thead>

        <tr>

            <th>API</th>

            {stage_header_html}

            <th>
                {first_stage} → {last_stage}
            </th>

        </tr>

    </thead>


    <tbody>

        {p95_table_rows}

    </tbody>

</table>

</div>


<!-- ===================================================== -->
<!-- P99 -->
<!-- ===================================================== -->

<h2>
    P99 응답시간 비교
</h2>


<div class="table-wrapper">

<table class="metric-table">

    <thead>

        <tr>

            <th>API</th>

            {stage_header_html}

            <th>
                {first_stage} → {last_stage}
            </th>

        </tr>

    </thead>


    <tbody>

        {p99_table_rows}

    </tbody>

</table>

</div>


<!-- ===================================================== -->
<!-- RPS -->
<!-- ===================================================== -->

<h2>
    RPS 비교
</h2>

<p class="description">
    각 VU 유지 구간에서 API별 초당 처리 요청 수입니다.
</p>


<div class="table-wrapper">

<table class="metric-table">

    <thead>

        <tr>

            <th>API</th>

            {stage_header_html}

        </tr>

    </thead>


    <tbody>

        {rps_table_rows}

    </tbody>

</table>

</div>


<!-- ===================================================== -->
<!-- 전체 Summary -->
<!-- ===================================================== -->

<h2>
    전체 테스트 결과
</h2>

<p class="description">
    Ramp-up, Hold, Ramp-down 구간을 모두 포함한 전체 결과입니다.
</p>


<div class="table-wrapper">

<table>

    <thead>

        <tr>

            <th>API</th>

            <th>AVG</th>

            <th>P95</th>

            <th>P99</th>

            <th>RPS</th>

            <th>실패율</th>

            <th>요청 수</th>

        </tr>

    </thead>


    <tbody>

        {summary_table_rows}

    </tbody>

</table>

</div>


<!-- ===================================================== -->
<!-- 상세 Hold Data -->
<!-- ===================================================== -->

<details>

    <summary>
        VU 유지 구간 상세 데이터 보기
    </summary>


    <div class="table-wrapper">

    <table>

        <thead>

            <tr>

                <th>VU</th>

                <th>API</th>

                <th>AVG</th>

                <th>P95</th>

                <th>P99</th>

                <th>RPS</th>

                <th>요청 수</th>

            </tr>

        </thead>


        <tbody>

            {hold_detail_rows}

        </tbody>

    </table>

    </div>

</details>


<!-- ===================================================== -->
<!-- RPS Graph -->
<!-- ===================================================== -->

<h2>
    시간에 따른 API별 RPS
</h2>


<div class="chart-container">

    <canvas id="rpsChart"></canvas>

</div>


<!-- ===================================================== -->
<!-- P95 Graph -->
<!-- ===================================================== -->

<h2>
    시간에 따른 API별 P95 응답시간
</h2>


<div class="chart-container">

    <canvas id="p95Chart"></canvas>

</div>


<script>


/*
 * RPS
 */

new Chart(

    document.getElementById(
        'rpsChart'
    ),

    {{

        type: 'line',

        data: {{

            labels:
                {json.dumps(rps_times)},

            datasets:
                {json.dumps(rps_datasets)}

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

                x: {{

                    title: {{

                        display: true,

                        text:
                            '테스트 시작 후 시간 (초)'

                    }}

                }},

                y: {{

                    beginAtZero: true,

                    title: {{

                        display: true,

                        text:
                            'Requests / Second'

                    }}

                }}

            }}

        }}

    }}

);


/*
 * P95 Duration
 */

new Chart(

    document.getElementById(
        'p95Chart'
    ),

    {{

        type: 'line',

        data: {{

            labels:
                {json.dumps(duration_times)},

            datasets:
                {json.dumps(p95_datasets)}

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

                x: {{

                    title: {{

                        display: true,

                        text:
                            '테스트 시작 후 시간 (초)'

                    }}

                }},

                y: {{

                    beginAtZero: true,

                    title: {{

                        display: true,

                        text:
                            'P95 Response Time (ms)'

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
# 17. HTML 저장
# ============================================================

with open(
        REPORT_FILE,
        "w",
        encoding="utf-8"
) as f:

    f.write(html)


print(
    f"Report generated: {REPORT_FILE}"
)