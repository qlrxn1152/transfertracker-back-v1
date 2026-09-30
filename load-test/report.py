import csv
import json
from collections import defaultdict
from pathlib import Path

RESULT_DIR = Path("load-test/results")

SUMMARY_FILE = RESULT_DIR / "all-api-result.json"
METRICS_FILE = RESULT_DIR / "all-api-metrics.csv"
REPORT_FILE = RESULT_DIR / "report.html"

BUCKET_SECONDS = 5


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
    raise RuntimeError("CSV에서 API metric을 찾을 수 없습니다.")


start_time = min(row["timestamp"] for row in rows)


# ============================================================
# 3. RPS - 1초 단위
# ============================================================

rps_by_time = defaultdict(lambda: defaultdict(float))

for row in rows:

    if row["metric"] != "http_reqs":
        continue

    elapsed = row["timestamp"] - start_time

    rps_by_time[elapsed][row["api"]] += row["value"]


rps_times = sorted(rps_by_time.keys())


rps_datasets = []

for api_key, api_summary in summary.items():

    values = [
        rps_by_time[t].get(api_key, 0)
        for t in rps_times
    ]

    rps_datasets.append({
        "label": api_summary["name"],
        "data": values,
        "fill": False,
        "tension": 0.2,
    })


# ============================================================
# 4. P95 응답시간 - 5초 단위
# ============================================================

duration_buckets = defaultdict(
    lambda: defaultdict(list)
)


for row in rows:

    if row["metric"] != "http_req_duration":
        continue

    elapsed = row["timestamp"] - start_time

    bucket = (
                     elapsed // BUCKET_SECONDS
             ) * BUCKET_SECONDS

    duration_buckets[bucket][row["api"]].append(
        row["value"]
    )


def percentile(values, percentile_value):

    if not values:
        return None

    values = sorted(values)

    index = (len(values) - 1) * percentile_value

    lower = int(index)
    upper = min(lower + 1, len(values) - 1)

    fraction = index - lower

    return (
            values[lower]
            + (values[upper] - values[lower]) * fraction
    )


duration_times = sorted(duration_buckets.keys())


p95_datasets = []

for api_key, api_summary in summary.items():

    values = []

    for timestamp in duration_times:

        durations = duration_buckets[timestamp].get(
            api_key,
            []
        )

        values.append(
            percentile(durations, 0.95)
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
# 5. Summary Table
# ============================================================

table_rows = ""

for api, value in summary.items():

    table_rows += f"""
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
# 6. HTML
# ============================================================

html = f"""
<!DOCTYPE html>
<html lang="ko">

<head>

    <meta charset="UTF-8">

    <title>k6 API Performance Report</title>

    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>

    <style>

        body {{
            font-family: Arial, sans-serif;

            max-width: 1200px;

            margin: 40px auto;

            padding: 0 20px;
        }}

        table {{
            width: 100%;

            border-collapse: collapse;

            margin-bottom: 50px;
        }}

        th, td {{
            padding: 12px;

            border-bottom: 1px solid #ddd;

            text-align: right;
        }}

        th:first-child,
        td:first-child {{
            text-align: left;
        }}

        th {{
            background: #f5f5f5;
        }}

        .chart-container {{
            height: 500px;

            margin-bottom: 60px;
        }}

    </style>

</head>


<body>


<h1>API 성능 테스트 결과</h1>


<h2>요약</h2>

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
        {table_rows}
    </tbody>

</table>



<h2>시간에 따른 API별 RPS</h2>

<div class="chart-container">
    <canvas id="rpsChart"></canvas>
</div>



<h2>시간에 따른 API별 P95 응답시간</h2>

<div class="chart-container">
    <canvas id="p95Chart"></canvas>
</div>



<script>

/*
 * RPS
 */

new Chart(
    document.getElementById('rpsChart'),
    {{
        type: 'line',

        data: {{
            labels: {json.dumps(rps_times)},
            datasets: {json.dumps(rps_datasets)}
        }},

        options: {{

            responsive: true,

            maintainAspectRatio: false,

            interaction: {{
                mode: 'index',
                intersect: false
            }},

            scales: {{

                x: {{
                    title: {{
                        display: true,
                        text: '테스트 시작 후 시간 (초)'
                    }}
                }},

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


/*
 * P95 Duration
 */

new Chart(
    document.getElementById('p95Chart'),
    {{
        type: 'line',

        data: {{
            labels: {json.dumps(duration_times)},
            datasets: {json.dumps(p95_datasets)}
        }},

        options: {{

            responsive: true,

            maintainAspectRatio: false,

            interaction: {{
                mode: 'index',
                intersect: false
            }},

            scales: {{

                x: {{
                    title: {{
                        display: true,
                        text: '테스트 시작 후 시간 (초)'
                    }}
                }},

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

</script>


</body>

</html>
"""


with open(REPORT_FILE, "w", encoding="utf-8") as f:
    f.write(html)


print(f"Report generated: {REPORT_FILE}")