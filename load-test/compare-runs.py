import os
import csv
import json
import statistics
from pathlib import Path
from collections import defaultdict


# ============================================================
# 기본 설정
# ============================================================

BASE_DIR = Path(
    os.getenv(
        "RESULT_DIR",
        "load-test/results/prod/10-30-vu"
    )
)

RUN_NAMES = [
    "run-1",
    "run-2",
    "run-3",
]

REPORT_FILE = BASE_DIR / "comparison-report.html"


# ============================================================
# VU 유지 구간
#
# all-api-test.js
#
# 0  ~ 10 : 0 -> 10
# 10 ~ 30 : 10 VU 유지
#
# 30 ~ 40 : 10 -> 25
# 40 ~ 70 : 25 VU 유지
#
# 70 ~ 80 : 25 -> 30
# 80 ~110 : 30 VU 유지
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
# Percentile
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
# 변화율
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

    return f"{sign}{rate:.1f}%"


# ============================================================
# 한 번의 Run 분석
# ============================================================

def analyze_run(run_name):

    run_dir = BASE_DIR / run_name

    summary_file = (
            run_dir
            / "all-api-result.json"
    )

    metrics_file = (
            run_dir
            / "all-api-metrics.csv"
    )


    if not summary_file.exists():
        raise FileNotFoundError(
            f"{summary_file} 파일이 없습니다."
        )

    if not metrics_file.exists():
        raise FileNotFoundError(
            f"{metrics_file} 파일이 없습니다."
        )


    # --------------------------------------------------------
    # Summary
    # --------------------------------------------------------

    with open(
            summary_file,
            encoding="utf-8"
    ) as f:

        summary = json.load(f)


    # --------------------------------------------------------
    # CSV
    # --------------------------------------------------------

    rows = []

    with open(
            metrics_file,
            encoding="utf-8"
    ) as f:

        reader = csv.DictReader(f)

        for row in reader:

            if not row["extra_tags"]:
                continue

            api = None

            for tag in row[
                "extra_tags"
            ].split(","):

                key, _, value = (
                    tag.partition("=")
                )

                if key == "api":
                    api = value
                    break


            if api is None:
                continue


            rows.append({
                "metric":
                    row["metric_name"],

                "timestamp":
                    int(row["timestamp"]),

                "value":
                    float(
                        row["metric_value"]
                    ),

                "api":
                    api,
            })


    if not rows:
        raise RuntimeError(
            f"{run_name}: "
            f"CSV에서 API metric을 "
            f"찾을 수 없습니다."
        )


    start_time = min(
        row["timestamp"]
        for row in rows
    )


    # --------------------------------------------------------
    # Hold 구간 분석
    # --------------------------------------------------------

    results = {}


    for api_key, api_summary in (
            summary.items()
    ):

        results[api_key] = {
            "name":
                api_summary["name"],

            "stages": {},
        }


        for window in HOLD_WINDOWS:

            start = window["start"]
            end = window["end"]


            durations = [
                row["value"]
                for row in rows
                if (
                        row["api"]
                        == api_key

                        and

                        row["metric"]
                        == "http_req_duration"

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
                        row["api"]
                        == api_key

                        and

                        row["metric"]
                        == "http_reqs"

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


            results[
                api_key
            ][
                "stages"
            ][
                window["name"]
            ] = {

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
            }


    return {
        "name":
            run_name,

        "summary":
            summary,

        "results":
            results,
    }


# ============================================================
# 3개 Run 분석
# ============================================================

runs = []

for run_name in RUN_NAMES:

    print(
        f"Analyzing {run_name}..."
    )

    runs.append(
        analyze_run(
            run_name
        )
    )


# ============================================================
# API 일관성 확인
# ============================================================

first_api_keys = set(
    runs[0]["results"].keys()
)


for run in runs[1:]:

    current_api_keys = set(
        run["results"].keys()
    )

    if (
            current_api_keys
            != first_api_keys
    ):
        raise RuntimeError(
            "Run마다 API 목록이 다릅니다. "
            "동일한 all-api-test.js로 "
            "실행했는지 확인하세요."
        )


# ============================================================
# 통합 결과
# ============================================================

aggregated = {}


for api_key in first_api_keys:

    api_name = (
        runs[0]["results"]
        [api_key]["name"]
    )


    aggregated[api_key] = {
        "name":
            api_name,

        "stages": {},
    }


    for window in HOLD_WINDOWS:

        stage_name = (
            window["name"]
        )


        p95_values = []

        p99_values = []

        avg_values = []

        rps_values = []

        request_count_values = []


        for run in runs:

            stage = (
                run["results"]
                .get(
                    api_key,
                    {}
                )
                .get(
                    "stages",
                    {}
                )
                .get(
                    stage_name
                )
            )


            if stage is None:
                continue


            avg_values.append(
                stage["avg"]
            )

            p95_values.append(
                stage["p95"]
            )

            p99_values.append(
                stage["p99"]
            )

            rps_values.append(
                stage["rps"]
            )

            request_count_values.append(
                stage["requestCount"]
            )


        if not p95_values:
            continue


        aggregated[
            api_key
        ][
            "stages"
        ][
            stage_name
        ] = {

            # AVG
            "avgMean":
                statistics.mean(
                    avg_values
                ),

            "avgMedian":
                statistics.median(
                    avg_values
                ),

            # P95
            "p95Mean":
                statistics.mean(
                    p95_values
                ),

            "p95Median":
                statistics.median(
                    p95_values
                ),

            "p95Min":
                min(
                    p95_values
                ),

            "p95Max":
                max(
                    p95_values
                ),

            # P99
            "p99Mean":
                statistics.mean(
                    p99_values
                ),

            "p99Median":
                statistics.median(
                    p99_values
                ),

            "p99Min":
                min(
                    p99_values
                ),

            "p99Max":
                max(
                    p99_values
                ),

            # RPS
            "rpsMean":
                statistics.mean(
                    rps_values
                ),

            "rpsMedian":
                statistics.median(
                    rps_values
                ),

            # 요청 수
            "requestCountMean":
                statistics.mean(
                    request_count_values
                ),
        }


# ============================================================
# Stage 정보
# ============================================================

STAGE_NAMES = [
    window["name"]
    for window in HOLD_WINDOWS
]


FIRST_STAGE = STAGE_NAMES[0]
LAST_STAGE = STAGE_NAMES[-1]


stage_headers = "".join(
    f"<th>{stage}</th>"
    for stage in STAGE_NAMES
)


# ============================================================
# P95 중앙값 비교표
# ============================================================

p95_rows = ""


for api_key, api in aggregated.items():

    stages = api["stages"]

    values = []


    for stage_name in STAGE_NAMES:

        stage = stages.get(
            stage_name
        )

        values.append(
            stage["p95Median"]
            if stage
            else None
        )


    cells = ""

    for value in values:

        if value is None:
            cells += "<td>-</td>"

        else:
            cells += (
                f"<td>"
                f"{value:.2f} ms"
                f"</td>"
            )


    change = change_text(
        values[0],
        values[-1]
    )


    p95_rows += f"""
        <tr>
            <td>{api['name']}</td>
            {cells}
            <td>{change}</td>
        </tr>
    """


# ============================================================
# P99 중앙값 비교표
# ============================================================

p99_rows = ""


for api_key, api in aggregated.items():

    stages = api["stages"]

    values = []


    for stage_name in STAGE_NAMES:

        stage = stages.get(
            stage_name
        )

        values.append(
            stage["p99Median"]
            if stage
            else None
        )


    cells = ""

    for value in values:

        if value is None:
            cells += "<td>-</td>"

        else:
            cells += (
                f"<td>"
                f"{value:.2f} ms"
                f"</td>"
            )


    change = change_text(
        values[0],
        values[-1]
    )


    p99_rows += f"""
        <tr>
            <td>{api['name']}</td>
            {cells}
            <td>{change}</td>
        </tr>
    """


# ============================================================
# RPS 평균 비교표
# ============================================================

rps_rows = ""


for api_key, api in aggregated.items():

    stages = api["stages"]


    cells = ""


    for stage_name in STAGE_NAMES:

        stage = stages.get(
            stage_name
        )


        if stage is None:
            cells += "<td>-</td>"

        else:
            cells += (
                f"<td>"
                f"{stage['rpsMean']:.2f}"
                f"</td>"
            )


    rps_rows += f"""
        <tr>
            <td>{api['name']}</td>
            {cells}
        </tr>
    """


# ============================================================
# P99 변동폭
#
# 3회 결과 중 최소 ~ 최대
# ============================================================

variation_rows = ""


for api_key, api in aggregated.items():

    stages = api["stages"]


    cells = ""


    for stage_name in STAGE_NAMES:

        stage = stages.get(
            stage_name
        )


        if stage is None:
            cells += "<td>-</td>"

        else:

            min_value = (
                stage["p99Min"]
            )

            max_value = (
                stage["p99Max"]
            )

            diff = (
                    max_value
                    - min_value
            )


            cells += f"""
                <td>
                    {min_value:.2f}
                    ~
                    {max_value:.2f}
                    ms

                    <br>

                    <small>
                        폭 {diff:.2f} ms
                    </small>
                </td>
            """


    variation_rows += f"""
        <tr>
            <td>{api['name']}</td>
            {cells}
        </tr>
    """


# ============================================================
# Run별 상세 데이터
# ============================================================

detail_rows = ""


for run in runs:

    run_name = run["name"]


    for api_key, api in (
            run["results"].items()
    ):

        for stage_name in STAGE_NAMES:

            stage = (
                api["stages"]
                .get(
                    stage_name
                )
            )


            if stage is None:
                continue


            detail_rows += f"""
                <tr>
                    <td>{run_name}</td>
                    <td>{stage_name}</td>
                    <td>{api['name']}</td>

                    <td>
                        {stage['avg']:.2f} ms
                    </td>

                    <td>
                        {stage['p95']:.2f} ms
                    </td>

                    <td>
                        {stage['p99']:.2f} ms
                    </td>

                    <td>
                        {stage['rps']:.2f}
                    </td>

                    <td>
                        {stage['requestCount']:,}
                    </td>
                </tr>
            """


# ============================================================
# 가장 큰 P95 / P99 증가율
# ============================================================

p95_changes = []

p99_changes = []


for api_key, api in aggregated.items():

    stages = api["stages"]


    first = stages.get(
        FIRST_STAGE
    )

    last = stages.get(
        LAST_STAGE
    )


    if (
            first is None
            or
            last is None
    ):
        continue


    p95_change = change_rate(
        first["p95Median"],
        last["p95Median"]
    )


    p99_change = change_rate(
        first["p99Median"],
        last["p99Median"]
    )


    if p95_change is not None:

        p95_changes.append(
            (
                p95_change,
                api["name"]
            )
        )


    if p99_change is not None:

        p99_changes.append(
            (
                p99_change,
                api["name"]
            )
        )


max_p95_change = max(
    p95_changes,
    key=lambda x: x[0]
)


max_p99_change = max(
    p99_changes,
    key=lambda x: x[0]
)


# ============================================================
# HTML
# ============================================================

html = f"""
<!DOCTYPE html>

<html lang="ko">

<head>

    <meta charset="UTF-8">

    <title>
        k6 Multi Run Comparison Report
    </title>


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
            margin-bottom: 10px;
        }}


        h2 {{
            margin-top: 55px;

            margin-bottom: 16px;
        }}


        .description {{
            color: #666;

            margin-bottom: 20px;
        }}


        .cards {{
            display: grid;

            grid-template-columns:
                repeat(
                    auto-fit,
                    minmax(
                        230px,
                        1fr
                    )
                );

            gap: 16px;

            margin-top: 30px;

            margin-bottom: 40px;
        }}


        .card {{
            border:
                1px solid #ddd;

            border-radius:
                10px;

            padding:
                20px;
        }}


        .card-title {{
            font-size:
                14px;

            color:
                #666;

            margin-bottom:
                8px;
        }}


        .card-value {{
            font-size:
                24px;

            font-weight:
                700;
        }}


        .table-wrapper {{
            overflow-x:
                auto;

            margin-bottom:
                50px;
        }}


        table {{
            width:
                100%;

            min-width:
                850px;

            border-collapse:
                collapse;
        }}


        th,
        td {{
            padding:
                12px 14px;

            border-bottom:
                1px solid #ddd;

            text-align:
                right;

            white-space:
                nowrap;
        }}


        th {{
            background:
                #f5f5f5;
        }}


        th:first-child,
        td:first-child {{
            text-align:
                left;
        }}


        tbody tr:hover {{
            background:
                #fafafa;
        }}


        td:first-child {{
            font-weight:
                500;
        }}


        small {{
            color:
                #777;
        }}


        details {{
            margin-top:
                30px;

            margin-bottom:
                50px;

            border:
                1px solid #ddd;

            border-radius:
                8px;

            padding:
                16px;
        }}


        summary {{
            cursor:
                pointer;

            font-size:
                18px;

            font-weight:
                600;
        }}


        details .table-wrapper {{
            margin-top:
                20px;

            margin-bottom:
                0;
        }}

    </style>

</head>


<body>


<h1>
    k6 3회 성능 테스트 비교
</h1>

<p class="description">
    동일한 부하 테스트를 3회 실행한 결과를 비교합니다.
    P95 / P99는 중앙값을 대표값으로 사용합니다.
</p>


<div class="cards">

    <div class="card">

        <div class="card-title">
            테스트 반복 횟수
        </div>

        <div class="card-value">
            {len(runs)}회
        </div>

    </div>


    <div class="card">

        <div class="card-title">
            최대 VU
        </div>

        <div class="card-value">
            {VU_3} VU
        </div>

    </div>


    <div class="card">

        <div class="card-title">
            P95 증가폭이 가장 큰 API
        </div>

        <div class="card-value">
            +{max_p95_change[0]:.1f}%
        </div>

        <div>
            {max_p95_change[1]}
        </div>

    </div>


    <div class="card">

        <div class="card-title">
            P99 증가폭이 가장 큰 API
        </div>

        <div class="card-value">
            +{max_p99_change[0]:.1f}%
        </div>

        <div>
            {max_p99_change[1]}
        </div>

    </div>

</div>


<!-- ===================================================== -->
<!-- P95 -->
<!-- ===================================================== -->

<h2>
    P95 중앙값 비교
</h2>

<p class="description">
    각 VU에서 3회 테스트의 P95 중앙값입니다.
    마지막 열은 {FIRST_STAGE} 대비 {LAST_STAGE} 변화율입니다.
</p>


<div class="table-wrapper">

<table>

    <thead>

        <tr>

            <th>API</th>

            {stage_headers}

            <th>
                {FIRST_STAGE}
                →
                {LAST_STAGE}
            </th>

        </tr>

    </thead>


    <tbody>

        {p95_rows}

    </tbody>

</table>

</div>


<!-- ===================================================== -->
<!-- P99 -->
<!-- ===================================================== -->

<h2>
    P99 중앙값 비교
</h2>

<p class="description">
    Tail Latency가 부하 증가에 따라 반복적으로 증가하는지 확인합니다.
</p>


<div class="table-wrapper">

<table>

    <thead>

        <tr>

            <th>API</th>

            {stage_headers}

            <th>
                {FIRST_STAGE}
                →
                {LAST_STAGE}
            </th>

        </tr>

    </thead>


    <tbody>

        {p99_rows}

    </tbody>

</table>

</div>


<!-- ===================================================== -->
<!-- RPS -->
<!-- ===================================================== -->

<h2>
    평균 RPS 비교
</h2>

<p class="description">
    동일 VU 구간에서 3회 측정한 API별 RPS 평균입니다.
</p>


<div class="table-wrapper">

<table>

    <thead>

        <tr>

            <th>API</th>

            {stage_headers}

        </tr>

    </thead>


    <tbody>

        {rps_rows}

    </tbody>

</table>

</div>


<!-- ===================================================== -->
<!-- Variation -->
<!-- ===================================================== -->

<h2>
    P99 실행별 변동폭
</h2>

<p class="description">
    3회 테스트 중 최소값과 최대값입니다.
    범위가 크다면 일시적인 네트워크 지연이나
    서버/DB 환경 변동의 영향을 의심할 수 있습니다.
</p>


<div class="table-wrapper">

<table>

    <thead>

        <tr>

            <th>API</th>

            {stage_headers}

        </tr>

    </thead>


    <tbody>

        {variation_rows}

    </tbody>

</table>

</div>


<!-- ===================================================== -->
<!-- Detail -->
<!-- ===================================================== -->

<details>

    <summary>
        Run별 상세 결과 보기
    </summary>


    <div class="table-wrapper">

    <table>

        <thead>

            <tr>

                <th>Run</th>

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

            {detail_rows}

        </tbody>

    </table>

    </div>

</details>


</body>

</html>
"""


# ============================================================
# HTML 저장
# ============================================================

with open(
        REPORT_FILE,
        "w",
        encoding="utf-8"
) as f:

    f.write(html)


print()
print("Comparison report generated:")
print(REPORT_FILE)
print()