import csv
import html
import json
import os
import statistics
from pathlib import Path


# ============================================================
# Config
# ============================================================

BASE_DIR = Path(
    os.environ["RESULT_DIR"]
)

RUN_COUNT = int(
    os.getenv(
        "RUN_COUNT",
        "5"
    )
)

TARGET_RPS = float(
    os.getenv(
        "TARGET_RPS",
        "40"
    )
)

REPORT_FILE = (
        BASE_DIR
        / "comparison-report.html"
)


# ============================================================
# Utility
# ============================================================

def parse_float(value):

    if (
            value is None
            or value == ""
    ):
        return None


    try:
        return float(value)

    except (
            TypeError,
            ValueError
    ):
        return None


def fmt(
        value,
        digits=2,
        suffix=""
):

    if value is None:
        return "-"


    return (
        f"{value:.{digits}f}"
        f"{suffix}"
    )


# ============================================================
# Run 분석
# ============================================================

def analyze_run(
        run_number
):

    run_name = (
        f"run-{run_number}"
    )

    run_dir = (
            BASE_DIR
            / run_name
    )


    summary_file = (
            run_dir
            / "all-api-result.json"
    )


    hikari_file = (
            run_dir
            / "hikari-metrics.csv"
    )


    if not summary_file.exists():

        raise FileNotFoundError(
            summary_file
        )


    if not hikari_file.exists():

        raise FileNotFoundError(
            hikari_file
        )


    # ========================================================
    # k6 Summary
    # ========================================================

    with open(
            summary_file,
            encoding="utf-8"
    ) as file:

        summary = json.load(
            file
        )


    if len(summary) != 1:

        raise RuntimeError(
            f"{run_name}: "
            "Single API 테스트인데 "
            "API가 1개가 아닙니다."
        )


    api_key = next(
        iter(summary)
    )


    api = summary[
        api_key
    ]


    # ========================================================
    # Hikari
    # ========================================================

    active_values = []
    idle_values = []
    pending_values = []
    max_values = []


    with open(
            hikari_file,
            encoding="utf-8"
    ) as file:

        reader = csv.DictReader(
            file
        )


        for row in reader:

            active = parse_float(
                row.get("active")
            )

            idle = parse_float(
                row.get("idle")
            )

            pending = parse_float(
                row.get("pending")
            )

            pool_max = parse_float(
                row.get("max")
            )


            if active is not None:
                active_values.append(
                    active
                )


            if idle is not None:
                idle_values.append(
                    idle
                )


            if pending is not None:
                pending_values.append(
                    pending
                )


            if pool_max is not None:
                max_values.append(
                    pool_max
                )


    peak_active = (
        max(active_values)
        if active_values
        else None
    )


    min_idle = (
        min(idle_values)
        if idle_values
        else None
    )


    max_pending = (
        max(pending_values)
        if pending_values
        else None
    )


    pending_samples = sum(

        1

        for value in pending_values

        if value > 0
    )


    pool_max = (
        max(max_values)
        if max_values
        else None
    )


    return {

        "run":
            run_name,

        "apiKey":
            api_key,

        "apiName":
            api.get(
                "name",
                api_key
            ),

        "targetRps":
            api.get(
                "targetRps",
                TARGET_RPS
            ),

        "actualRps":
            api.get(
                "actualRps",
                api.get(
                    "rps"
                )
            ),

        "avg":
            api.get(
                "avg"
            ),

        "p95":
            api.get(
                "p95"
            ),

        "p99":
            api.get(
                "p99"
            ),

        "max":
            api.get(
                "max"
            ),

        "failRate":
            api.get(
                "failRate",
                0
            ),

        "requestCount":
            api.get(
                "requestCount",
                0
            ),

        "droppedIterations":
            api.get(
                "droppedIterations",
                0
            ),

        "peakActive":
            peak_active,

        "minIdle":
            min_idle,

        "maxPending":
            max_pending,

        "pendingSamples":
            pending_samples,

        "poolMax":
            pool_max,

        "detailReport":
            f"{run_name}/report.html",
    }


# ============================================================
# Analyze
# ============================================================

runs = [

    analyze_run(
        run_number
    )

    for run_number
    in range(
        1,
        RUN_COUNT + 1
    )

]


api_name = (
    runs[0]["apiName"]
)


# ============================================================
# Aggregate
# ============================================================

def median_of(
        key
):

    values = [

        run[key]

        for run in runs

        if run[key] is not None
    ]


    if not values:
        return None


    return statistics.median(
        values
    )


actual_rps_median = (
    median_of(
        "actualRps"
    )
)

p95_median = (
    median_of(
        "p95"
    )
)

p99_median = (
    median_of(
        "p99"
    )
)

peak_active_max = max(

    (
        run["peakActive"] or 0
        for run in runs
    ),

    default=0
)

max_pending = max(

    (
        run["maxPending"] or 0
        for run in runs
    ),

    default=0
)


pending_runs = sum(

    1

    for run in runs

    if (
            run["maxPending"] is not None
            and
            run["maxPending"] > 0
    )
)


total_dropped = sum(

    run["droppedIterations"]

    for run in runs
)


total_failures = sum(

    1

    for run in runs

    if run["failRate"] > 0
)


# ============================================================
# HTML Rows
# ============================================================

rows = []


for run in runs:

    pending = (
            run["maxPending"]
            or 0
    )


    status = (
        "CHECK"
        if (
                pending > 0
                or run["droppedIterations"] > 0
                or run["failRate"] > 0
        )
        else
        "OK"
    )


    rows.append(
        f"""
        <tr>
            <td>{html.escape(run["run"])}</td>

            <td>
                {fmt(run["targetRps"])}
            </td>

            <td>
                {fmt(run["actualRps"])}
            </td>

            <td>
                {fmt(run["avg"], suffix=" ms")}
            </td>

            <td>
                {fmt(run["p95"], suffix=" ms")}
            </td>

            <td>
                {fmt(run["p99"], suffix=" ms")}
            </td>

            <td>
                {fmt(run["max"], suffix=" ms")}
            </td>

            <td>
                {run["requestCount"]:,}
            </td>

            <td>
                {run["droppedIterations"]:,}
            </td>

            <td>
                {run["peakActive"]}
            </td>

            <td>
                {run["minIdle"]}
            </td>

            <td>
                {run["maxPending"]}
            </td>

            <td>
                {run["pendingSamples"]}
            </td>

            <td>
                {run["poolMax"]}
            </td>

            <td>
                {run["failRate"] * 100:.2f}%
            </td>

            <td>
                <strong>{status}</strong>
            </td>

            <td>
                <a href="{run["detailReport"]}">
                    상세
                </a>
            </td>
        </tr>
        """
    )


# ============================================================
# HTML
# ============================================================

document = f"""
<!DOCTYPE html>

<html lang="ko">

<head>

<meta charset="UTF-8">

<meta
    name="viewport"
    content="width=device-width, initial-scale=1.0"
>

<title>
    Single API Fixed RPS Comparison
</title>

<style>

body {{
    font-family:
        -apple-system,
        BlinkMacSystemFont,
        "Segoe UI",
        Arial,
        sans-serif;

    max-width: 1500px;

    margin: 40px auto;

    padding:
        0
        24px
        80px;

    color: #222;
}}

.cards {{
    display: grid;

    grid-template-columns:
        repeat(
            auto-fit,
            minmax(180px, 1fr)
        );

    gap: 14px;

    margin:
        24px
        0
        36px;
}}

.card {{
    border:
        1px
        solid
        #ddd;

    border-radius: 10px;

    padding: 18px;
}}

.card-title {{
    color: #666;

    font-size: 14px;

    margin-bottom: 6px;
}}

.card-value {{
    font-size: 22px;

    font-weight: 700;
}}

.table-wrapper {{
    overflow-x: auto;

    border:
        1px
        solid
        #ddd;

    border-radius: 10px;
}}

table {{
    width: 100%;

    border-collapse:
        collapse;
}}

th,
td {{
    padding:
        10px
        12px;

    border-bottom:
        1px
        solid
        #ddd;

    text-align: right;

    white-space: nowrap;
}}

th {{
    background: #f5f5f5;
}}

th:first-child,
td:first-child {{
    text-align: left;
}}

a {{
    color: #2563eb;

    text-decoration: none;
}}

.description {{
    color: #666;
}}

</style>

</head>


<body>


<h1>
    Single API Fixed RPS Comparison
</h1>


<p class="description">

    API:
    <strong>
        {html.escape(api_name)}
    </strong>

</p>


<div class="cards">

    <div class="card">

        <div class="card-title">
            Target RPS
        </div>

        <div class="card-value">
            {TARGET_RPS:.0f}
        </div>

    </div>


    <div class="card">

        <div class="card-title">
            Run 수
        </div>

        <div class="card-value">
            {RUN_COUNT}
        </div>

    </div>


    <div class="card">

        <div class="card-title">
            Actual RPS Median
        </div>

        <div class="card-value">
            {fmt(actual_rps_median)}
        </div>

    </div>


    <div class="card">

        <div class="card-title">
            P95 Median
        </div>

        <div class="card-value">
            {fmt(
    p95_median,
    suffix=" ms"
)}
        </div>

    </div>


    <div class="card">

        <div class="card-title">
            P99 Median
        </div>

        <div class="card-value">
            {fmt(
    p99_median,
    suffix=" ms"
)}
        </div>

    </div>


    <div class="card">

        <div class="card-title">
            Peak Active
        </div>

        <div class="card-value">
            {peak_active_max}
        </div>

    </div>


    <div class="card">

        <div class="card-title">
            Worst Pending
        </div>

        <div class="card-value">
            {max_pending}
        </div>

    </div>


    <div class="card">

        <div class="card-title">
            Pending Run
        </div>

        <div class="card-value">
            {pending_runs} / {RUN_COUNT}
        </div>

    </div>


    <div class="card">

        <div class="card-title">
            Dropped Iterations
        </div>

        <div class="card-value">
            {total_dropped}
        </div>

    </div>

</div>


<h2>
    Run별 결과
</h2>


<div class="table-wrapper">

<table>

<thead>

<tr>

    <th>Run</th>

    <th>Target RPS</th>

    <th>Actual RPS</th>

    <th>AVG</th>

    <th>P95</th>

    <th>P99</th>

    <th>MAX</th>

    <th>Requests</th>

    <th>Dropped</th>

    <th>Peak Active</th>

    <th>Min Idle</th>

    <th>Max Pending</th>

    <th>Pending Samples</th>

    <th>Pool Max</th>

    <th>Fail Rate</th>

    <th>Status</th>

    <th>Detail</th>

</tr>

</thead>


<tbody>

{''.join(rows)}

</tbody>

</table>

</div>


</body>

</html>
"""


REPORT_FILE.write_text(
    document,
    encoding="utf-8"
)


print()
print(
    "Fixed RPS comparison report generated:"
)
print(
    REPORT_FILE
)
print()