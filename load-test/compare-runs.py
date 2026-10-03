import os
import csv
import json
import statistics
from pathlib import Path


# ============================================================
# 기본 설정
# ============================================================

BASE_DIR = Path(
    os.getenv(
        "RESULT_DIR",
        "load-test/results/prod/10-30-vu"
    )
)


RUN_COUNT = int(
    os.getenv(
        "RUN_COUNT",
        "3"
    )
)


RUN_NAMES = [
    f"run-{number}"
    for number in range(
        1,
        RUN_COUNT + 1
    )
]


REPORT_FILE = (
        BASE_DIR
        / "comparison-report.html"
)


# ============================================================
# VU
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


# ============================================================
# VU 유지 구간
#
# 0   ~ 10  : Ramp
# 10  ~ 30  : VU_1 Hold
#
# 30  ~ 40  : Ramp
# 40  ~ 70  : VU_2 Hold
#
# 70  ~ 80  : Ramp
# 80  ~ 110 : VU_3 Hold
# ============================================================

HOLD_WINDOWS = [

    {
        "name":
            f"{VU_1} VU",

        "start":
            10,

        "end":
            30,
    },

    {
        "name":
            f"{VU_2} VU",

        "start":
            40,

        "end":
            70,
    },

    {
        "name":
            f"{VU_3} VU",

        "start":
            80,

        "end":
            110,
    },

]


# ============================================================
# Utility
# ============================================================

def percentile(
        values,
        percentile_value
):

    if not values:
        return None


    values = sorted(
        values
    )


    index = (
                    len(values) - 1
            ) * percentile_value


    lower = int(
        index
    )


    upper = min(
        lower + 1,
        len(values) - 1
    )


    fraction = (
            index
            - lower
    )


    return (
            values[lower]
            +
            (
                    values[upper]
                    - values[lower]
            )
            * fraction
    )


def change_rate(
        before,
        after
):

    if before is None:
        return None

    if after is None:
        return None

    if before == 0:
        return None


    return (
            (
                    after
                    - before
            )
            / before
    ) * 100


def change_text(
        before,
        after
):

    rate = change_rate(
        before,
        after
    )


    if rate is None:
        return "-"


    sign = (
        "+"
        if rate >= 0
        else ""
    )


    return (
        f"{sign}"
        f"{rate:.1f}%"
    )


def parse_float(
        value
):

    if value is None:
        return None

    if value == "":
        return None


    try:

        return float(
            value
        )

    except ValueError:

        return None


def format_number(
        value
):

    if value is None:
        return "-"

    return f"{value:.0f}"


# ============================================================
# Hikari 상태 판단
# ============================================================

def hikari_status(
        value
):

    if value is None:
        return "수집 데이터 없음"


    if value.get(
            "saturationDetected",
            False
    ):

        return (
            "Pool 포화 흔적 있음"
        )


    max_pending = value.get(
        "maxPending"
    )


    if (
            max_pending is not None
            and
            max_pending > 0
    ):

        return (
            "Connection 대기 발생"
        )


    return (
        "Connection 대기 없음"
    )


# ============================================================
# k6 CSV 읽기
# ============================================================

def read_k6_metrics(
        metrics_file
):

    rows = []


    with open(
            metrics_file,
            encoding="utf-8"
    ) as f:

        reader = csv.DictReader(
            f
        )


        for row in reader:

            if not row[
                "extra_tags"
            ]:

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
                    row[
                        "metric_name"
                    ],

                "timestamp":
                    int(
                        row[
                            "timestamp"
                        ]
                    ),

                "value":
                    float(
                        row[
                            "metric_value"
                        ]
                    ),

                "api":
                    api,
            })


    return rows


# ============================================================
# Hikari CSV 분석
# ============================================================

def analyze_hikari(
        hikari_file,
        k6_start_time
):

    if not hikari_file.exists():
        return {}


    hikari_rows = []


    with open(
            hikari_file,
            encoding="utf-8"
    ) as f:

        reader = csv.DictReader(
            f
        )


        for row in reader:

            epoch_ms = parse_float(
                row.get(
                    "timestamp_epoch_ms"
                )
            )

            active = parse_float(
                row.get(
                    "active"
                )
            )

            idle = parse_float(
                row.get(
                    "idle"
                )
            )

            pending = parse_float(
                row.get(
                    "pending"
                )
            )

            pool_max = parse_float(
                row.get(
                    "max"
                )
            )


            if epoch_ms is None:
                continue


            # 모든 metric이 비어있다면
            # 정상 수집 데이터가 아니다.
            if (
                    active is None
                    and
                    idle is None
                    and
                    pending is None
                    and
                    pool_max is None
            ):

                continue


            elapsed = (
                    epoch_ms / 1000
                    - k6_start_time
            )


            hikari_rows.append({

                "elapsed":
                    elapsed,

                "active":
                    active,

                "idle":
                    idle,

                "pending":
                    pending,

                "max":
                    pool_max,
            })


    results = {}


    for window in HOLD_WINDOWS:

        stage_name = (
            window["name"]
        )

        start = (
            window["start"]
        )

        end = (
            window["end"]
        )


        samples = [

            row

            for row in hikari_rows

            if (
                    start
                    <= row["elapsed"]
                    < end
            )

        ]


        if not samples:
            continue


        active_values = [

            row["active"]

            for row in samples

            if row["active"]
               is not None

        ]


        idle_values = [

            row["idle"]

            for row in samples

            if row["idle"]
               is not None

        ]


        pending_values = [

            row["pending"]

            for row in samples

            if row["pending"]
               is not None

        ]


        max_values = [

            row["max"]

            for row in samples

            if row["max"]
               is not None

        ]


        # 중요한 부분:
        #
        # Active Max / Idle Min / Pending Max가
        # 서로 다른 순간에 찍힌 값일 수 있으므로
        # 단순히 세 극단값만 조합해서
        # 포화라고 판정하지 않는다.
        #
        # 동일한 샘플 시점에
        #
        # active >= max
        # idle <= 0
        # pending > 0
        #
        # 이 모두 발생했는지 확인한다.

        saturation_detected = any(

            row["active"]
            is not None

            and

            row["idle"]
            is not None

            and

            row["pending"]
            is not None

            and

            row["max"]
            is not None

            and

            row["active"]
            >= row["max"]

            and

            row["idle"]
            <= 0

            and

            row["pending"]
            > 0

            for row in samples

        )


        results[
            stage_name
        ] = {

            "maxActive":
                (
                    max(
                        active_values
                    )
                    if active_values
                    else None
                ),

            "minIdle":
                (
                    min(
                        idle_values
                    )
                    if idle_values
                    else None
                ),

            "maxPending":
                (
                    max(
                        pending_values
                    )
                    if pending_values
                    else None
                ),

            "poolMax":
                (
                    max(
                        max_values
                    )
                    if max_values
                    else None
                ),

            "saturationDetected":
                saturation_detected,
        }


    return results


# ============================================================
# 한 Run 분석
# ============================================================

def analyze_run(
        run_name
):

    run_dir = (
            BASE_DIR
            / run_name
    )


    summary_file = (
            run_dir
            / "all-api-result.json"
    )


    metrics_file = (
            run_dir
            / "all-api-metrics.csv"
    )


    hikari_file = (
            run_dir
            / "hikari-metrics.csv"
    )


    if not summary_file.exists():

        raise FileNotFoundError(
            f"{summary_file} "
            f"파일이 없습니다."
        )


    if not metrics_file.exists():

        raise FileNotFoundError(
            f"{metrics_file} "
            f"파일이 없습니다."
        )


    # --------------------------------------------------------
    # Summary
    # --------------------------------------------------------

    with open(
            summary_file,
            encoding="utf-8"
    ) as f:

        summary = json.load(
            f
        )


    # --------------------------------------------------------
    # k6 CSV
    # --------------------------------------------------------

    rows = read_k6_metrics(
        metrics_file
    )


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
    # API Hold 분석
    # --------------------------------------------------------

    results = {}


    for api_key, api_summary in (
            summary.items()
    ):

        results[
            api_key
        ] = {

            "name":
                api_summary[
                    "name"
                ],

            "stages":
                {},
        }


        for window in HOLD_WINDOWS:

            stage_name = (
                window[
                    "name"
                ]
            )

            start = (
                window[
                    "start"
                ]
            )

            end = (
                window[
                    "end"
                ]
            )


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
                    end
                    - start
            )


            results[
                api_key
            ][
                "stages"
            ][
                stage_name
            ] = {

                "avg":
                    (
                            sum(
                                durations
                            )
                            / len(
                        durations
                    )
                    ),

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
                    (
                            request_count
                            / duration_seconds
                    ),

                "requestCount":
                    int(
                        request_count
                    ),
            }


    # --------------------------------------------------------
    # Hikari
    # --------------------------------------------------------

    hikari_results = analyze_hikari(
        hikari_file,
        start_time
    )


    return {

        "name":
            run_name,

        "summary":
            summary,

        "results":
            results,

        "hikari":
            hikari_results,
    }


# ============================================================
# Run 분석
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
# API 일관성
# ============================================================

first_api_keys = set(
    runs[0][
        "results"
    ].keys()
)


for run in runs[1:]:

    current_api_keys = set(
        run[
            "results"
        ].keys()
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
# API 통합 결과
# ============================================================

aggregated = {}


for api_key in first_api_keys:

    api_name = (
        runs[0]
        ["results"]
        [api_key]
        ["name"]
    )


    aggregated[
        api_key
    ] = {

        "name":
            api_name,

        "stages":
            {},
    }


    for window in HOLD_WINDOWS:

        stage_name = (
            window[
                "name"
            ]
        )


        avg_values = []
        p95_values = []
        p99_values = []
        rps_values = []
        request_count_values = []


        for run in runs:

            stage = (
                run[
                    "results"
                ]
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
                stage[
                    "avg"
                ]
            )

            p95_values.append(
                stage[
                    "p95"
                ]
            )

            p99_values.append(
                stage[
                    "p99"
                ]
            )

            rps_values.append(
                stage[
                    "rps"
                ]
            )

            request_count_values.append(
                stage[
                    "requestCount"
                ]
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

            "avgMean":
                statistics.mean(
                    avg_values
                ),

            "avgMedian":
                statistics.median(
                    avg_values
                ),

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

            "rpsMean":
                statistics.mean(
                    rps_values
                ),

            "rpsMedian":
                statistics.median(
                    rps_values
                ),

            "requestCountMean":
                statistics.mean(
                    request_count_values
                ),
        }


# ============================================================
# Stage
# ============================================================

STAGE_NAMES = [

    window[
        "name"
    ]

    for window in HOLD_WINDOWS

]


FIRST_STAGE = (
    STAGE_NAMES[0]
)

LAST_STAGE = (
    STAGE_NAMES[-1]
)


stage_headers = "".join(

    f"<th>{stage}</th>"

    for stage in STAGE_NAMES

)


# ============================================================
# P95 Table
# ============================================================

p95_rows = ""


for api_key, api in (
        aggregated.items()
):

    stages = (
        api[
            "stages"
        ]
    )


    values = []


    for stage_name in STAGE_NAMES:

        stage = stages.get(
            stage_name
        )


        values.append(

            stage[
                "p95Median"
            ]

            if stage

            else None

        )


    cells = ""


    for value in values:

        if value is None:

            cells += (
                "<td>-</td>"
            )

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

            <td>
                {api['name']}
            </td>

            {cells}

            <td>
                {change}
            </td>

        </tr>
    """


# ============================================================
# P99 Table
# ============================================================

p99_rows = ""


for api_key, api in (
        aggregated.items()
):

    stages = (
        api[
            "stages"
        ]
    )


    values = []


    for stage_name in STAGE_NAMES:

        stage = stages.get(
            stage_name
        )


        values.append(

            stage[
                "p99Median"
            ]

            if stage

            else None

        )


    cells = ""


    for value in values:

        if value is None:

            cells += (
                "<td>-</td>"
            )

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

            <td>
                {api['name']}
            </td>

            {cells}

            <td>
                {change}
            </td>

        </tr>
    """


# ============================================================
# RPS Table
# ============================================================

rps_rows = ""


for api_key, api in (
        aggregated.items()
):

    stages = (
        api[
            "stages"
        ]
    )


    cells = ""


    for stage_name in STAGE_NAMES:

        stage = stages.get(
            stage_name
        )


        if stage is None:

            cells += (
                "<td>-</td>"
            )

        else:

            cells += (
                f"<td>"
                f"{stage['rpsMean']:.2f}"
                f"</td>"
            )


    rps_rows += f"""
        <tr>

            <td>
                {api['name']}
            </td>

            {cells}

        </tr>
    """


# ============================================================
# P99 Variation
# ============================================================

variation_rows = ""


for api_key, api in (
        aggregated.items()
):

    stages = (
        api[
            "stages"
        ]
    )


    cells = ""


    for stage_name in STAGE_NAMES:

        stage = stages.get(
            stage_name
        )


        if stage is None:

            cells += (
                "<td>-</td>"
            )

            continue


        min_value = (
            stage[
                "p99Min"
            ]
        )

        max_value = (
            stage[
                "p99Max"
            ]
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

            <td>
                {api['name']}
            </td>

            {cells}

        </tr>
    """


# ============================================================
# Run별 API 상세
# ============================================================

detail_rows = ""


for run in runs:

    run_name = (
        run[
            "name"
        ]
    )


    for api_key, api in (
            run[
                "results"
            ].items()
    ):

        for stage_name in STAGE_NAMES:

            stage = (
                api[
                    "stages"
                ]
                .get(
                    stage_name
                )
            )


            if stage is None:
                continue


            detail_rows += f"""
                <tr>

                    <td>
                        {run_name}
                    </td>

                    <td>
                        {stage_name}
                    </td>

                    <td>
                        {api['name']}
                    </td>

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
# P95 / P99 최대 증가율
# ============================================================

p95_changes = []
p99_changes = []


for api_key, api in (
        aggregated.items()
):

    stages = (
        api[
            "stages"
        ]
    )


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
        first[
            "p95Median"
        ],
        last[
            "p95Median"
        ]
    )


    p99_change = change_rate(
        first[
            "p99Median"
        ],
        last[
            "p99Median"
        ]
    )


    if p95_change is not None:

        p95_changes.append(
            (
                p95_change,
                api[
                    "name"
                ]
            )
        )


    if p99_change is not None:

        p99_changes.append(
            (
                p99_change,
                api[
                    "name"
                ]
            )
        )


max_p95_change = (

    max(
        p95_changes,
        key=lambda x:
        x[0]
    )

    if p95_changes

    else (
        0,
        "-"
    )

)


max_p99_change = (

    max(
        p99_changes,
        key=lambda x:
        x[0]
    )

    if p99_changes

    else (
        0,
        "-"
    )

)


# ============================================================
# Hikari 통합
# ============================================================

hikari_detail_rows = ""


all_active = []
all_idle = []
all_pending = []
all_pool_max = []

global_saturation = False


# ------------------------------------------------------------
# Run별 상세
# ------------------------------------------------------------

for run in runs:

    run_name = (
        run[
            "name"
        ]
    )

    hikari = run.get(
        "hikari",
        {}
    )


    for stage_name in STAGE_NAMES:

        value = hikari.get(
            stage_name
        )


        if value is None:
            continue


        max_active = value.get(
            "maxActive"
        )

        min_idle = value.get(
            "minIdle"
        )

        max_pending = value.get(
            "maxPending"
        )

        pool_max = value.get(
            "poolMax"
        )


        if max_active is not None:

            all_active.append(
                max_active
            )


        if min_idle is not None:

            all_idle.append(
                min_idle
            )


        if max_pending is not None:

            all_pending.append(
                max_pending
            )


        if pool_max is not None:

            all_pool_max.append(
                pool_max
            )


        if value.get(
                "saturationDetected",
                False
        ):

            global_saturation = True


        status = hikari_status(
            value
        )


        hikari_detail_rows += f"""
            <tr>

                <td>
                    {run_name}
                </td>

                <td>
                    {stage_name}
                </td>

                <td>
                    {format_number(max_active)}
                </td>

                <td>
                    {format_number(min_idle)}
                </td>

                <td>
                    {format_number(max_pending)}
                </td>

                <td>
                    {format_number(pool_max)}
                </td>

                <td>
                    {status}
                </td>

            </tr>
        """


# ------------------------------------------------------------
# 전체 Worst Case
# ------------------------------------------------------------

hikari_peak_active = (

    max(
        all_active
    )

    if all_active

    else None

)


hikari_min_idle = (

    min(
        all_idle
    )

    if all_idle

    else None

)


hikari_peak_pending = (

    max(
        all_pending
    )

    if all_pending

    else None

)


hikari_pool_max = (

    max(
        all_pool_max
    )

    if all_pool_max

    else None

)


if not all_active:

    hikari_overall_status = (
        "수집 데이터 없음"
    )

elif global_saturation:

    hikari_overall_status = (
        "Pool 포화 흔적 있음"
    )

elif (
        hikari_peak_pending
        is not None

        and

        hikari_peak_pending
        > 0
):

    hikari_overall_status = (
        "Connection 대기 발생"
    )

else:

    hikari_overall_status = (
        "Connection 대기 없음"
    )


# ============================================================
# VU별 Hikari Worst Case
# ============================================================

hikari_stage_rows = ""


for stage_name in STAGE_NAMES:

    values = []


    for run in runs:

        value = (
            run
            .get(
                "hikari",
                {}
            )
            .get(
                stage_name
            )
        )


        if value is not None:

            values.append(
                value
            )


    if not values:

        hikari_stage_rows += f"""
            <tr>

                <td>
                    {stage_name}
                </td>

                <td>-</td>
                <td>-</td>
                <td>-</td>
                <td>-</td>

                <td>
                    수집 데이터 없음
                </td>

            </tr>
        """

        continue


    active_values = [

        value[
            "maxActive"
        ]

        for value in values

        if value.get(
            "maxActive"
        )
           is not None

    ]


    idle_values = [

        value[
            "minIdle"
        ]

        for value in values

        if value.get(
            "minIdle"
        )
           is not None

    ]


    pending_values = [

        value[
            "maxPending"
        ]

        for value in values

        if value.get(
            "maxPending"
        )
           is not None

    ]


    max_values = [

        value[
            "poolMax"
        ]

        for value in values

        if value.get(
            "poolMax"
        )
           is not None

    ]


    stage_value = {

        "maxActive":
            (
                max(
                    active_values
                )
                if active_values
                else None
            ),

        "minIdle":
            (
                min(
                    idle_values
                )
                if idle_values
                else None
            ),

        "maxPending":
            (
                max(
                    pending_values
                )
                if pending_values
                else None
            ),

        "poolMax":
            (
                max(
                    max_values
                )
                if max_values
                else None
            ),

        "saturationDetected":
            any(
                value.get(
                    "saturationDetected",
                    False
                )
                for value in values
            ),
    }


    hikari_stage_rows += f"""
        <tr>

            <td>
                {stage_name}
            </td>

            <td>
                {
    format_number(
        stage_value[
            'maxActive'
        ]
    )
    }
            </td>

            <td>
                {
    format_number(
        stage_value[
            'minIdle'
        ]
    )
    }
            </td>

            <td>
                {
    format_number(
        stage_value[
            'maxPending'
        ]
    )
    }
            </td>

            <td>
                {
    format_number(
        stage_value[
            'poolMax'
        ]
    )
    }
            </td>

            <td>
                {
    hikari_status(
        stage_value
    )
    }
            </td>

        </tr>
    """


# ============================================================
# HTML용 값
# ============================================================

p95_change_text = (
    f"{max_p95_change[0]:+.1f}%"
)

p99_change_text = (
    f"{max_p99_change[0]:+.1f}%"
)


hikari_pool_max_text = (
    format_number(
        hikari_pool_max
    )
)

hikari_peak_active_text = (
    format_number(
        hikari_peak_active
    )
)

hikari_min_idle_text = (
    format_number(
        hikari_min_idle
    )
)

hikari_peak_pending_text = (
    format_number(
        hikari_peak_pending
    )
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

            max-width:
                1400px;

            margin:
                40px auto;

            padding:
                0 24px;

            color:
                #222;
        }}


        h1 {{
            margin-bottom:
                10px;
        }}


        h2 {{
            margin-top:
                55px;

            margin-bottom:
                16px;
        }}


        .description {{
            color:
                #666;

            margin-bottom:
                20px;

            line-height:
                1.6;
        }}


        .cards {{
            display:
                grid;

            grid-template-columns:
                repeat(
                    auto-fit,
                    minmax(
                        210px,
                        1fr
                    )
                );

            gap:
                16px;

            margin-top:
                30px;

            margin-bottom:
                40px;
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
    k6 {len(runs)}회 성능 테스트 비교
</h1>


<p class="description">

    동일한 부하 테스트를
    {len(runs)}회 실행한 결과입니다.

    P95 / P99는
    실행 결과의 중앙값을
    대표값으로 사용합니다.

</p>


<!-- ===================================================== -->
<!-- Summary -->
<!-- ===================================================== -->

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
            {p95_change_text}
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
            {p99_change_text}
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

    각 VU 유지 구간에서
    {len(runs)}회 테스트의
    P95 중앙값입니다.

    마지막 열은
    {FIRST_STAGE}
    대비
    {LAST_STAGE}
    변화율입니다.

</p>


<div class="table-wrapper">

<table>

    <thead>

        <tr>

            <th>
                API
            </th>

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

    Tail Latency가
    부하 증가에 따라
    반복적으로 증가하는지 확인합니다.

</p>


<div class="table-wrapper">

<table>

    <thead>

        <tr>

            <th>
                API
            </th>

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

    동일 VU 구간에서
    실행별 API RPS 평균입니다.

</p>


<div class="table-wrapper">

<table>

    <thead>

        <tr>

            <th>
                API
            </th>

            {stage_headers}

        </tr>

    </thead>


    <tbody>

        {rps_rows}

    </tbody>

</table>

</div>


<!-- ===================================================== -->
<!-- Hikari -->
<!-- ===================================================== -->

<h2>
    HikariCP Connection Pool
</h2>


<p class="description">

    k6 부하 테스트 중
    Spring Boot의 DB Connection Pool 상태입니다.

    Pending 값이 0보다 크다면
    DB Connection을 얻지 못하고
    기다린 요청이 있었다는 의미입니다.

</p>


<div class="cards">


    <div class="card">

        <div class="card-title">
            Pool Max
        </div>

        <div class="card-value">
            {hikari_pool_max_text}
        </div>

    </div>


    <div class="card">

        <div class="card-title">
            Peak Active
        </div>

        <div class="card-value">
            {hikari_peak_active_text}
        </div>

    </div>


    <div class="card">

        <div class="card-title">
            Minimum Idle
        </div>

        <div class="card-value">
            {hikari_min_idle_text}
        </div>

    </div>


    <div class="card">

        <div class="card-title">
            Peak Pending
        </div>

        <div class="card-value">
            {hikari_peak_pending_text}
        </div>

    </div>


    <div class="card">

        <div class="card-title">
            Connection Pool 상태
        </div>

        <div class="card-value">
            {hikari_overall_status}
        </div>

    </div>


</div>


<h2>
    VU별 HikariCP 상태
</h2>


<p class="description">

    각 VU 유지 구간에서
    모든 Run 중 가장 불리했던
    Connection Pool 상태입니다.

</p>


<div class="table-wrapper">

<table>

    <thead>

        <tr>

            <th>
                VU
            </th>

            <th>
                Max Active
            </th>

            <th>
                Min Idle
            </th>

            <th>
                Max Pending
            </th>

            <th>
                Pool Max
            </th>

            <th>
                판단
            </th>

        </tr>

    </thead>


    <tbody>

        {hikari_stage_rows}

    </tbody>

</table>

</div>


<!-- ===================================================== -->
<!-- P99 Variation -->
<!-- ===================================================== -->

<h2>
    P99 실행별 변동폭
</h2>


<p class="description">

    반복 테스트 중
    최소값과 최대값입니다.

    범위가 크다면
    일시적인 네트워크 지연,
    JVM,
    서버 또는 DB 환경 변동 등을
    추가로 확인할 필요가 있습니다.

</p>


<div class="table-wrapper">

<table>

    <thead>

        <tr>

            <th>
                API
            </th>

            {stage_headers}

        </tr>

    </thead>


    <tbody>

        {variation_rows}

    </tbody>

</table>

</div>


<!-- ===================================================== -->
<!-- Hikari Detail -->
<!-- ===================================================== -->

<details>

    <summary>
        Run별 HikariCP 상세 결과 보기
    </summary>


    <div class="table-wrapper">

    <table>

        <thead>

            <tr>

                <th>
                    Run
                </th>

                <th>
                    VU
                </th>

                <th>
                    Max Active
                </th>

                <th>
                    Min Idle
                </th>

                <th>
                    Max Pending
                </th>

                <th>
                    Pool Max
                </th>

                <th>
                    판단
                </th>

            </tr>

        </thead>


        <tbody>

            {hikari_detail_rows}

        </tbody>

    </table>

    </div>

</details>


<!-- ===================================================== -->
<!-- API Detail -->
<!-- ===================================================== -->

<details>

    <summary>
        Run별 API 상세 결과 보기
    </summary>


    <div class="table-wrapper">

    <table>

        <thead>

            <tr>

                <th>
                    Run
                </th>

                <th>
                    VU
                </th>

                <th>
                    API
                </th>

                <th>
                    AVG
                </th>

                <th>
                    P95
                </th>

                <th>
                    P99
                </th>

                <th>
                    RPS
                </th>

                <th>
                    요청 수
                </th>

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

    f.write(
        html
    )


print()
print(
    "Comparison report generated:"
)
print(
    REPORT_FILE
)
print()