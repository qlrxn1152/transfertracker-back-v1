import csv
import html
import json
import math
import os
from collections import Counter, defaultdict
from pathlib import Path


# ============================================================
# 설정
# ============================================================

RESULT_DIR = Path(
    os.getenv(
        "RESULT_DIR",
        "load-test/results/local"
    )
)

SUMMARY_FILE = (
        RESULT_DIR
        / "all-api-result.json"
)

METRICS_FILE = (
        RESULT_DIR
        / "all-api-metrics.csv"
)

HIKARI_FILE = (
        RESULT_DIR
        / "hikari-metrics.csv"
)

REPORT_FILE = (
        RESULT_DIR
        / "report.html"
)


# P95 Timeline은 표본 수를 확보하기 위해
# 기본 5초 단위로 묶는다.
P95_BUCKET_SECONDS = int(
    os.getenv(
        "P95_BUCKET_SECONDS",
        "5"
    )
)


# 같은 VU가 몇 초 이상 유지되어야
# Hold 구간으로 판단할지 설정한다.
#
# 예:
#
# 30 VU가 20초 유지
# 40 VU가 30초 유지
# 50 VU가 30초 유지
#
# -> 모두 자동으로 Hold로 탐지된다.
MIN_HOLD_SECONDS = int(
    os.getenv(
        "MIN_HOLD_SECONDS",
        "5"
    )
)


# ============================================================
# 공통 함수
# ============================================================

def parse_float(value):

    if (
            value is None
            or
            value == ""
    ):

        return None


    try:

        return float(
            value
        )


    except (
            TypeError,
            ValueError
    ):

        return None


def parse_api_tag(
        extra_tags
):

    if not extra_tags:

        return None


    for tag in (
            extra_tags
                    .split(",")
    ):

        key, _, value = (
            tag.partition("=")
        )


        if key == "api":

            return value


    return None


def percentile(
        values,
        p
):

    if not values:

        return None


    values = sorted(
        values
    )


    index = (
                    len(values)
                    - 1
            ) * p


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


def change_text(
        before,
        after
):

    if before is None:

        return "-"


    if after is None:

        return "-"


    if before == 0:

        return "-"


    rate = (
                   (
                           after
                           - before
                   )
                   / before
           ) * 100


    sign = (
        "+"
        if rate >= 0
        else ""
    )


    return (
        f"{sign}"
        f"{rate:.1f}%"
    )


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


def fmt_int(
        value
):

    if value is None:

        return "-"


    return (
        f"{int(round(value)):,}"
    )


def js(
        value
):

    return json.dumps(
        value,
        ensure_ascii=False,
        separators=(
            ",",
            ":"
        )
    )


def nearest(
        mapping,
        target
):

    if not mapping:

        return None


    candidates = [

        second

        for second in (

            int(
                round(
                    target
                )
            ),

            int(
                math.floor(
                    target
                )
            ),

            int(
                math.ceil(
                    target
                )
            ),

            int(
                round(
                    target
                )
            ) - 1,

            int(
                round(
                    target
                )
            ) + 1,

        )

        if second in mapping

    ]


    if not candidates:

        return None


    second = min(

        candidates,

        key=lambda value:
        abs(
            value
            - target
        )

    )


    return mapping[
        second
    ]


def chart_dataset(
        label,
        data,
        color,
        axis=None
):

    result = {

        "label":
            label,

        "data":
            data,

        "borderColor":
            color,

        "backgroundColor":
            color,

        "pointRadius":
            0,

        "pointHoverRadius":
            4,

        "borderWidth":
            2,

        "tension":
            0.2,

        "spanGaps":
            True,

    }


    if axis:

        result[
            "yAxisID"
        ] = axis


    return result


# ============================================================
# 실제 VU에서 Hold 구간 자동 탐지
# ============================================================

def detect_hold_windows(
        vu_by_second,
        min_hold_seconds
):

    """
    실제 k6 `vus` metric에서
    같은 VU가 연속으로 유지된 구간을 자동 탐지한다.

    따라서:

    10 -> 25 -> 30
    30 -> 40 -> 50
    50 -> 75 -> 100

    어떤 Profile을 실행해도
    VU 값을 코드에 직접 작성할 필요가 없다.
    """


    if not vu_by_second:

        return []


    samples = sorted(
        vu_by_second.items()
    )


    segments = []


    start_second, current_vu = (
        samples[0]
    )


    previous_second = (
        start_second
    )


    for (
            second,
            vu
    ) in samples[1:]:


        consecutive = (
                second
                == previous_second + 1
        )


        same_vu = (
                int(
                    round(
                        vu
                    )
                )
                ==
                int(
                    round(
                        current_vu
                    )
                )
        )


        if (
                consecutive
                and
                same_vu
        ):

            previous_second = (
                second
            )

            continue


        duration = (
                previous_second
                - start_second
                + 1
        )


        if (
                current_vu > 0
                and
                duration
                >= min_hold_seconds
        ):

            segments.append({

                "vu":
                    int(
                        round(
                            current_vu
                        )
                    ),

                "start":
                    start_second,

                "end":
                    previous_second + 1,

                "duration":
                    duration,

            })


        start_second = (
            second
        )

        previous_second = (
            second
        )

        current_vu = (
            vu
        )


    # 마지막 Segment 처리
    duration = (
            previous_second
            - start_second
            + 1
    )


    if (
            current_vu > 0
            and
            duration
            >= min_hold_seconds
    ):

        segments.append({

            "vu":
                int(
                    round(
                        current_vu
                    )
                ),

            "start":
                start_second,

            "end":
                previous_second + 1,

            "duration":
                duration,

        })


    # --------------------------------------------------------
    # 같은 VU가 여러 번 나오는 테스트도 대응
    #
    # 예:
    #
    # 30 VU
    # 50 VU
    # 30 VU
    #
    # -> 30 VU #1
    # -> 50 VU
    # -> 30 VU #2
    # --------------------------------------------------------

    counts = Counter(

        segment[
            "vu"
        ]

        for segment
        in segments

    )


    seen = Counter()


    for segment in segments:

        vu = segment[
            "vu"
        ]


        seen[
            vu
        ] += 1


        if counts[
            vu
        ] == 1:

            segment[
                "name"
            ] = (
                f"{vu} VU"
            )


        else:

            segment[
                "name"
            ] = (

                f"{vu} VU "
                f"#{seen[vu]}"

            )


    return segments


# ============================================================
# 입력 파일 확인
# ============================================================

for required in (

        SUMMARY_FILE,
        METRICS_FILE,
        HIKARI_FILE,

):

    if not required.exists():

        raise FileNotFoundError(

            "필수 파일이 없습니다: "
            f"{required}"

        )


# ============================================================
# Summary JSON
# ============================================================

with open(
        SUMMARY_FILE,
        encoding="utf-8"
) as f:

    summary = json.load(
        f
    )


api_keys = list(
    summary.keys()
)


api_names = {

    api_key:
        summary[
            api_key
        ].get(
            "name",
            api_key
        )

    for api_key
    in api_keys

}


# ============================================================
# k6 CSV 읽기
# ============================================================

http_rows = []

vu_raw = []

vus_max_values = []

http_request_timestamps = []


with open(
        METRICS_FILE,
        encoding="utf-8"
) as f:

    reader = csv.DictReader(
        f
    )


    for row in reader:

        metric = row.get(
            "metric_name"
        )


        timestamp = parse_float(
            row.get(
                "timestamp"
            )
        )


        value = parse_float(
            row.get(
                "metric_value"
            )
        )


        if (
                timestamp is None
                or
                value is None
        ):

            continue


        # ----------------------------------------------------
        # 실제 VU
        # ----------------------------------------------------

        if metric == "vus":

            vu_raw.append(
                (
                    timestamp,
                    value
                )
            )

            continue


        # ----------------------------------------------------
        # k6 설정상 최대 VU
        # ----------------------------------------------------

        if metric == "vus_max":

            vus_max_values.append(
                value
            )

            continue


        # ----------------------------------------------------
        # API Tag
        # ----------------------------------------------------

        api = parse_api_tag(
            row.get(
                "extra_tags"
            )
        )


        if api is None:

            continue


        if api not in summary:

            continue


        # 분석에 필요한 Metric만 저장
        if metric in (

                "http_reqs",
                "http_req_duration",

        ):

            http_rows.append({

                "metric":
                    metric,

                "timestamp":
                    timestamp,

                "value":
                    value,

                "api":
                    api,

            })


            if metric == "http_reqs":

                http_request_timestamps.append(
                    timestamp
                )


if not http_request_timestamps:

    raise RuntimeError(

        "k6 CSV에서 "
        "http_reqs metric을 "
        "찾을 수 없습니다."

    )


# ============================================================
# 테스트 시작 / 종료
# ============================================================

k6_start_time = min(
    http_request_timestamps
)


k6_end_time = max(
    http_request_timestamps
)


test_duration_seconds = (

        int(
            k6_end_time
            - k6_start_time
        )
        + 1

)


# ============================================================
# 실제 VU Timeline
# ============================================================

vu_by_second = {}


for (
        timestamp,
        value
) in vu_raw:

    second = int(
        round(
            timestamp
            - k6_start_time
        )
    )


    if second >= 0:

        vu_by_second[
            second
        ] = value


actual_peak_vu = (

    max(
        vu_by_second.values()
    )

    if vu_by_second

    else None

)


configured_max_vu = (

    max(
        vus_max_values
    )

    if vus_max_values

    else None

)


# ============================================================
# Hold 구간 자동 탐지
# ============================================================

hold_windows = (
    detect_hold_windows(
        vu_by_second,
        MIN_HOLD_SECONDS
    )
)


# ============================================================
# API 초 단위 Metric
# ============================================================

api_duration_by_second = defaultdict(
    lambda:
    defaultdict(
        list
    )
)


api_requests_by_second = defaultdict(
    lambda:
    defaultdict(
        float
    )
)


for row in http_rows:

    second = int(

        row[
            "timestamp"
        ]
        - k6_start_time

    )


    if second < 0:

        continue


    api_key = row[
        "api"
    ]


    # --------------------------------------------------------
    # 응답 시간
    # --------------------------------------------------------

    if (
            row[
                "metric"
            ]
            ==
            "http_req_duration"
    ):

        api_duration_by_second[
            second
        ][
            api_key
        ].append(

            row[
                "value"
            ]

        )


    # --------------------------------------------------------
    # Request 수
    # --------------------------------------------------------

    elif (
            row[
                "metric"
            ]
            ==
            "http_reqs"
    ):

        api_requests_by_second[
            second
        ][
            api_key
        ] += (

            row[
                "value"
            ]

        )


# ============================================================
# Hikari CSV
# ============================================================

hikari_rows = []


with open(
        HIKARI_FILE,
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


        if epoch_ms is None:

            continue


        elapsed = (

                epoch_ms
                / 1000
                - k6_start_time

        )


        # Collector는 k6보다 먼저 실행되므로
        # 테스트 시작 전 Metric은 제외
        if elapsed < 0:

            continue


        values = {

            "elapsed":
                elapsed,

            "timestamp":
                row.get(
                    "timestamp",
                    ""
                ),

            "active":
                parse_float(
                    row.get(
                        "active"
                    )
                ),

            "idle":
                parse_float(
                    row.get(
                        "idle"
                    )
                ),

            "pending":
                parse_float(
                    row.get(
                        "pending"
                    )
                ),

            "max":
                parse_float(
                    row.get(
                        "max"
                    )
                ),

        }


        if all(

                values[
                    key
                ] is None

                for key
                in (
                        "active",
                        "idle",
                        "pending",
                        "max"
                )

        ):

            continue


        hikari_rows.append(
            values
        )


# ============================================================
# 전체 시간축 계산
# ============================================================

max_candidates = [

    test_duration_seconds

]


if vu_by_second:

    max_candidates.append(

        max(
            vu_by_second.keys()
        )
        + 1

    )


if api_duration_by_second:

    max_candidates.append(

        max(
            api_duration_by_second.keys()
        )
        + 1

    )


if hikari_rows:

    max_candidates.append(

        int(
            math.ceil(

                max(

                    row[
                        "elapsed"
                    ]

                    for row
                    in hikari_rows

                )

            )
        )
        + 1

    )


max_second = max(
    max_candidates
)


timeline_seconds = list(

    range(
        0,
        max_second + 1
    )

)


# ============================================================
# 실제 VU Forward Fill
# ============================================================

vu_filled = {}


last_vu = 0.0


for second in timeline_seconds:

    if second in vu_by_second:

        last_vu = (
            vu_by_second[
                second
            ]
        )


    vu_filled[
        second
    ] = last_vu


# ============================================================
# API 1초 AVG / MAX / RPS
# ============================================================

api_avg = defaultdict(
    dict
)


api_max = defaultdict(
    dict
)


api_rps = defaultdict(
    dict
)


total_rps = {}


for second in timeline_seconds:

    total = 0.0


    for api_key in api_keys:

        durations = (

            api_duration_by_second[
                second
            ].get(
                api_key,
                []
            )

        )


        requests = (

            api_requests_by_second[
                second
            ].get(
                api_key,
                0.0
            )

        )


        total += (
            requests
        )


        api_rps[
            second
        ][
            api_key
        ] = requests


        api_avg[
            second
        ][
            api_key
        ] = (

            sum(
                durations
            )
            / len(
                durations
            )

            if durations

            else None

        )


        api_max[
            second
        ][
            api_key
        ] = (

            max(
                durations
            )

            if durations

            else None

        )


    total_rps[
        second
    ] = total


# ============================================================
# API P95 - N초 Bucket
# ============================================================

p95_bucket_starts = list(

    range(

        0,

        max_second
        + P95_BUCKET_SECONDS,

        P95_BUCKET_SECONDS

    )

)


api_p95 = defaultdict(
    dict
)


for bucket_start in (
        p95_bucket_starts
):

    bucket_end = (

            bucket_start
            + P95_BUCKET_SECONDS

    )


    for api_key in api_keys:

        durations = []


        for second in range(
                bucket_start,
                bucket_end
        ):

            durations.extend(

                api_duration_by_second[
                    second
                ].get(
                    api_key,
                    []
                )

            )


        api_p95[
            bucket_start
        ][
            api_key
        ] = (

            percentile(
                durations,
                0.95
            )

            if durations

            else None

        )


# ============================================================
# 자동 탐지 Hold 구간 분석
# ============================================================

hold_results = defaultdict(
    dict
)


for window in hold_windows:

    start = window[
        "start"
    ]


    end = window[
        "end"
    ]


    name = window[
        "name"
    ]


    for api_key in api_keys:

        durations = []

        request_count = 0.0


        for second in range(
                start,
                end
        ):

            durations.extend(

                api_duration_by_second[
                    second
                ].get(
                    api_key,
                    []
                )

            )


            request_count += (

                api_requests_by_second[
                    second
                ].get(
                    api_key,
                    0.0
                )

            )


        if not durations:

            continue


        hold_results[
            api_key
        ][
            name
        ] = {

            "avg":
                sum(
                    durations
                )
                /
                len(
                    durations
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
                request_count
                /
                max(
                    1,
                    end - start
                ),

            "requestCount":
                int(
                    request_count
                ),

        }


# ============================================================
# Hikari Summary
# ============================================================

def values_of(
        key
):

    return [

        row[
            key
        ]

        for row
        in hikari_rows

        if (
                row[
                    key
                ]
                is not None
        )

    ]


active_values = (
    values_of(
        "active"
    )
)


idle_values = (
    values_of(
        "idle"
    )
)


pending_values = (
    values_of(
        "pending"
    )
)


pool_max_values = (
    values_of(
        "max"
    )
)


peak_active = (

    max(
        active_values
    )

    if active_values

    else None

)


minimum_idle = (

    min(
        idle_values
    )

    if idle_values

    else None

)


max_pending = (

    max(
        pending_values
    )

    if pending_values

    else None

)


pool_max = (

    max(
        pool_max_values
    )

    if pool_max_values

    else None

)


# ============================================================
# Pending Event
# ============================================================

pending_events = [

    row

    for row
    in hikari_rows

    if (

            row[
                "pending"
            ]
            is not None

            and

            row[
                "pending"
            ]
            > 0

    )

]


pending_sample_count = (
    len(
        pending_events
    )
)


# ============================================================
# 전체 Summary
# ============================================================

total_requests = sum(

    value.get(
        "requestCount",
        0
    )

    for value
    in summary.values()

)


failed_requests = sum(

    value.get(
        "requestCount",
        0
    )
    *
    value.get(
        "failRate",
        0
    )

    for value
    in summary.values()

)


overall_fail_rate = (

    failed_requests
    / total_requests

    if total_requests

    else 0

)


max_p95_api = max(

    summary.values(),

    key=lambda value:
    value.get(
        "p95",
        0
    )

)


max_p99_api = max(

    summary.values(),

    key=lambda value:
    value.get(
        "p99",
        0
    )

)


# ============================================================
# Chart Data
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


# ============================================================
# VU / Total RPS
# ============================================================

vu_rps_datasets = [

    chart_dataset(

        "Actual VU",

        [

            vu_filled.get(
                second,
                0
            )

            for second
            in timeline_seconds

        ],

        "#2563eb",

        "yVu"

    ),

    chart_dataset(

        "Total RPS",

        [

            total_rps.get(
                second,
                0
            )

            for second
            in timeline_seconds

        ],

        "#dc2626",

        "yRps"

    ),

]


# ============================================================
# Hikari Timeline
# ============================================================

hikari_labels = [

    round(
        row[
            "elapsed"
        ],
        2
    )

    for row
    in hikari_rows

]


hikari_datasets = [

    chart_dataset(

        "Active",

        [

            row[
                "active"
            ]

            for row
            in hikari_rows

        ],

        "#2563eb"

    ),

    chart_dataset(

        "Idle",

        [

            row[
                "idle"
            ]

            for row
            in hikari_rows

        ],

        "#16a34a"

    ),

    chart_dataset(

        "Pending",

        [

            row[
                "pending"
            ]

            for row
            in hikari_rows

        ],

        "#dc2626"

    ),

    chart_dataset(

        "Pool Max",

        [

            row[
                "max"
            ]

            for row
            in hikari_rows

        ],

        "#111827"

    ),

]


# ============================================================
# API Timeline Dataset
# ============================================================

p95_datasets = []

avg_datasets = []

max_datasets = []

rps_datasets = []


for (
        index,
        api_key
) in enumerate(
    api_keys
):

    color = COLORS[
        index
        % len(
            COLORS
        )
        ]


    name = api_names[
        api_key
    ]


    p95_datasets.append(

        chart_dataset(

            name,

            [

                api_p95[
                    bucket
                ].get(
                    api_key
                )

                for bucket
                in p95_bucket_starts

            ],

            color

        )

    )


    avg_datasets.append(

        chart_dataset(

            name,

            [

                api_avg[
                    second
                ].get(
                    api_key
                )

                for second
                in timeline_seconds

            ],

            color

        )

    )


    max_datasets.append(

        chart_dataset(

            name,

            [

                api_max[
                    second
                ].get(
                    api_key
                )

                for second
                in timeline_seconds

            ],

            color

        )

    )


    rps_datasets.append(

        chart_dataset(

            name,

            [

                api_rps[
                    second
                ].get(
                    api_key,
                    0
                )

                for second
                in timeline_seconds

            ],

            color

        )

    )


# ============================================================
# HTML Table - 전체 Summary
# ============================================================

summary_rows = ""


for value in (
        summary.values()
):

    summary_rows += f"""
        <tr>

            <td>
                {
    html.escape(
        value['name']
    )
    }
            </td>

            <td>
                {value['avg']:.2f} ms
            </td>

            <td>
                {value['p95']:.2f} ms
            </td>

            <td>
                {value['p99']:.2f} ms
            </td>

            <td>
                {value['rps']:.2f}
            </td>

            <td>
                {
    value['failRate']
    * 100
    :.2f}%
            </td>

            <td>
                {value['requestCount']:,}
            </td>

        </tr>
    """


# ============================================================
# HTML Table - Hold 구간
# ============================================================

hold_names = [

    window[
        "name"
    ]

    for window
    in hold_windows

]


hold_header = "".join(

    f"""
        <th>
            {
    html.escape(
        window['name']
    )
    }

            <br>

            <small>
                {
    window['start']
    }
                ~
                {
    window['end']
    }s
            </small>
        </th>
    """

    for window
    in hold_windows

)


p95_rows = ""

p99_rows = ""

hold_rps_rows = ""

hold_detail_rows = ""


for api_key in api_keys:

    stages = (
        hold_results.get(
            api_key,
            {}
        )
    )


    p95_values = [

        stages.get(
            name,
            {}
        ).get(
            "p95"
        )

        for name
        in hold_names

    ]


    p99_values = [

        stages.get(
            name,
            {}
        ).get(
            "p99"
        )

        for name
        in hold_names

    ]


    p95_cells = "".join(

        (
            f"<td>"
            f"{value:.2f} ms"
            f"</td>"
        )

        if value is not None

        else "<td>-</td>"

        for value
        in p95_values

    )


    p99_cells = "".join(

        (
            f"<td>"
            f"{value:.2f} ms"
            f"</td>"
        )

        if value is not None

        else "<td>-</td>"

        for value
        in p99_values

    )


    rps_cells = "".join(

        (
            f"<td>"
            f"{stages[name]['rps']:.2f}"
            f"</td>"
        )

        if name in stages

        else "<td>-</td>"

        for name
        in hold_names

    )


    p95_change = (

        change_text(
            p95_values[0],
            p95_values[-1]
        )

        if len(
            p95_values
        ) >= 2

        else "-"

    )


    p99_change = (

        change_text(
            p99_values[0],
            p99_values[-1]
        )

        if len(
            p99_values
        ) >= 2

        else "-"

    )


    p95_rows += f"""
        <tr>

            <td>
                {
    html.escape(
        api_names[
            api_key
        ]
    )
    }
            </td>

            {p95_cells}

            <td>
                {p95_change}
            </td>

        </tr>
    """


    p99_rows += f"""
        <tr>

            <td>
                {
    html.escape(
        api_names[
            api_key
        ]
    )
    }
            </td>

            {p99_cells}

            <td>
                {p99_change}
            </td>

        </tr>
    """


    hold_rps_rows += f"""
        <tr>

            <td>
                {
    html.escape(
        api_names[
            api_key
        ]
    )
    }
            </td>

            {rps_cells}

        </tr>
    """


# ============================================================
# Hold 상세
# ============================================================

for window in hold_windows:

    for api_key in api_keys:

        value = (
            hold_results
            .get(
                api_key,
                {}
            )
            .get(
                window[
                    "name"
                ]
            )
        )


        if not value:

            continue


        hold_detail_rows += f"""
            <tr>

                <td>
                    {
        html.escape(
            window['name']
        )
        }
                </td>

                <td>
                    {
        window['start']
        }
                    ~
                    {
        window['end']
        }s
                </td>

                <td>
                    {
        html.escape(
            api_names[
                api_key
            ]
        )
        }
                </td>

                <td>
                    {value['avg']:.2f} ms
                </td>

                <td>
                    {value['p95']:.2f} ms
                </td>

                <td>
                    {value['p99']:.2f} ms
                </td>

                <td>
                    {value['rps']:.2f}
                </td>

                <td>
                    {value['requestCount']:,}
                </td>

            </tr>
        """


# Hold가 없는 테스트도 깨지지 않게 처리
if not hold_windows:

    hold_header = """
        <th>
            자동 탐지 결과 없음
        </th>
    """


    p95_rows = """
        <tr>
            <td colspan="3">
                안정적으로 유지된 VU 구간을
                찾지 못했습니다.
            </td>
        </tr>
    """


    p99_rows = p95_rows

    hold_rps_rows = p95_rows


    hold_detail_rows = """
        <tr>
            <td colspan="8">
                안정적으로 유지된 VU 구간을
                찾지 못했습니다.
            </td>
        </tr>
    """


# ============================================================
# Pending Event Table
# ============================================================

pending_summary_rows = ""

pending_detail_rows = ""


for event in pending_events:

    elapsed = event[
        "elapsed"
    ]


    second = int(
        round(
            elapsed
        )
    )


    vu = nearest(
        vu_filled,
        elapsed
    )


    rps = nearest(
        total_rps,
        elapsed
    )


    # --------------------------------------------------------
    # 같은 초에서 AVG가 가장 높은 API
    # --------------------------------------------------------

    avg_candidates = [

        (
            value,
            api_key
        )

        for api_key
        in api_keys

        if (
               value :=
               api_avg[
                   second
               ].get(
                   api_key
               )
           ) is not None

    ]


    # --------------------------------------------------------
    # 같은 초에서 MAX가 가장 높은 API
    # --------------------------------------------------------

    max_candidates = [

        (
            value,
            api_key
        )

        for api_key
        in api_keys

        if (
               value :=
               api_max[
                   second
               ].get(
                   api_key
               )
           ) is not None

    ]


    highest_avg = (

        max(
            avg_candidates
        )

        if avg_candidates

        else None

    )


    highest_max = (

        max(
            max_candidates
        )

        if max_candidates

        else None

    )


    highest_avg_text = (

        (
            f"{html.escape(api_names[highest_avg[1]])} "
            f"({highest_avg[0]:.2f} ms)"
        )

        if highest_avg

        else "-"

    )


    highest_max_text = (

        (
            f"{html.escape(api_names[highest_max[1]])} "
            f"({highest_max[0]:.2f} ms)"
        )

        if highest_max

        else "-"

    )


    pending_summary_rows += f"""
        <tr class="pending-row">

            <td>
                {elapsed:.2f}s
            </td>

            <td>
                {fmt_int(vu)}
            </td>

            <td>
                {
    fmt(
        event['active'],
        0
    )
    }
            </td>

            <td>
                {
    fmt(
        event['idle'],
        0
    )
    }
            </td>

            <td>
                <strong>
                    {
    fmt(
        event['pending'],
        0
    )
    }
                </strong>
            </td>

            <td>
                {
    fmt(
        event['max'],
        0
    )
    }
            </td>

            <td>
                {
    fmt(
        rps,
        2
    )
    }
            </td>

            <td>
                {highest_avg_text}
            </td>

            <td>
                {highest_max_text}
            </td>

        </tr>
    """


    # --------------------------------------------------------
    # Pending 발생 초의 API별 상세
    # --------------------------------------------------------

    for api_key in api_keys:

        pending_detail_rows += f"""
            <tr>

                <td>
                    {elapsed:.2f}s
                </td>

                <td>
                    {fmt_int(vu)}
                </td>

                <td>
                    {
        fmt(
            event['pending'],
            0
        )
        }
                </td>

                <td>
                    {
        html.escape(
            api_names[
                api_key
            ]
        )
        }
                </td>

                <td>
                    {
        fmt(
            api_rps[
                second
            ].get(
                api_key,
                0
            ),
            2
        )
        }
                </td>

                <td>
                    {
        fmt(
            api_avg[
                second
            ].get(
                api_key
            ),
            2,
            ' ms'
        )
        }
                </td>

                <td>
                    {
        fmt(
            api_max[
                second
            ].get(
                api_key
            ),
            2,
            ' ms'
        )
        }
                </td>

            </tr>
        """


if not pending_events:

    pending_summary_rows = """
        <tr>

            <td colspan="9">
                Pending &gt; 0 이벤트가 없습니다.
            </td>

        </tr>
    """


    pending_detail_rows = """
        <tr>

            <td colspan="7">
                Pending &gt; 0 이벤트가 없습니다.
            </td>

        </tr>
    """


# ============================================================
# Raw Timeline용 Hikari 초 매핑
# ============================================================

hikari_by_second = {}


for row in hikari_rows:

    second = int(
        round(
            row[
                "elapsed"
            ]
        )
    )


    current = (
        hikari_by_second.get(
            second
        )
    )


    if current is None:

        hikari_by_second[
            second
        ] = row

        continue


    # 같은 초에 여러 Sample이 있으면
    # 정수 초와 가장 가까운 Sample 사용
    if (

            abs(
                row[
                    "elapsed"
                ]
                - second
            )

            <

            abs(
                current[
                    "elapsed"
                ]
                - second
            )

    ):

        hikari_by_second[
            second
        ] = row


# ============================================================
# Raw Timeline Header
# ============================================================

raw_api_header = "".join(

    f"""
        <th>
            {
    html.escape(
        api_names[key]
    )
    }

            <br>

            AVG
        </th>

        <th>
            {
    html.escape(
        api_names[key]
    )
    }

            <br>

            MAX
        </th>
    """

    for key
    in api_keys

)


# ============================================================
# Raw Timeline Rows
# ============================================================

raw_rows = ""


for second in timeline_seconds:

    hikari = (
        hikari_by_second.get(
            second,
            {}
        )
    )


    pending = hikari.get(
        "pending"
    )


    row_class = (

        "pending-row"

        if (
                pending is not None
                and
                pending > 0
        )

        else ""

    )


    api_cells = "".join(

        f"""
            <td>
                {
        fmt(
            api_avg[
                second
            ].get(
                key
            ),
            2
        )
        }
            </td>

            <td>
                {
        fmt(
            api_max[
                second
            ].get(
                key
            ),
            2
        )
        }
            </td>
        """

        for key
        in api_keys

    )


    raw_rows += f"""
        <tr class="{row_class}">

            <td>
                {second}s
            </td>

            <td>
                {
    fmt_int(
        vu_filled.get(
            second
        )
    )
    }
            </td>

            <td>
                {
    fmt(
        hikari.get(
            'active'
        ),
        0
    )
    }
            </td>

            <td>
                {
    fmt(
        hikari.get(
            'idle'
        ),
        0
    )
    }
            </td>

            <td>
                {
    fmt(
        pending,
        0
    )
    }
            </td>

            <td>
                {
    fmt(
        hikari.get(
            'max'
        ),
        0
    )
    }
            </td>

            <td>
                {
    fmt(
        total_rps.get(
            second,
            0
        ),
        2
    )
    }
            </td>

            {api_cells}

        </tr>
    """


# ============================================================
# Hold 탐지 결과 Text
# ============================================================

if hold_windows:

    hold_summary_text = ", ".join(

        (
            f"{window['name']} "
            f"({window['start']}~"
            f"{window['end']}s, "
            f"{window['duration']}초)"
        )

        for window
        in hold_windows

    )


else:

    hold_summary_text = (

        f"{MIN_HOLD_SECONDS}초 이상 "
        "같은 VU가 유지된 구간 없음"

    )


hikari_status = (

    "Connection 대기 발생"

    if (
               max_pending or 0
       ) > 0

    else "Connection 대기 없음"

)


# ============================================================
# HTML
# ============================================================

report = f"""
<!DOCTYPE html>

<html lang="ko">

<head>

<meta charset="UTF-8">

<meta
    name="viewport"
    content="width=device-width, initial-scale=1.0"
>

<title>
    TransferTracker Performance Analysis
</title>


<script
    src="https://cdn.jsdelivr.net/npm/chart.js"
></script>


<style>

body {{

    font-family:
        -apple-system,
        BlinkMacSystemFont,
        "Segoe UI",
        Arial,
        sans-serif;

    max-width:
        1500px;

    margin:
        40px auto;

    padding:
        0 24px 80px;

    color:
        #222;

    background:
        #fff;

    line-height:
        1.5;

}}


h1 {{

    margin-bottom:
        8px;

}}


h2 {{

    margin-top:
        54px;

    margin-bottom:
        12px;

}}


.description {{

    color:
        #666;

    margin-top:
        0;

    margin-bottom:
        20px;

}}


.warning {{

    border:
        1px solid
        #d97706;

    border-radius:
        10px;

    padding:
        14px 16px;

    background:
        #fff7ed;

    margin:
        18px 0 24px;

}}


.cards {{

    display:
        grid;

    grid-template-columns:
        repeat(
            auto-fit,
            minmax(
                190px,
                1fr
            )
        );

    gap:
        14px;

    margin:
        24px 0 36px;

}}


.card {{

    border:
        1px solid
        #ddd;

    border-radius:
        10px;

    padding:
        18px;

}}


.card-title {{

    font-size:
        14px;

    color:
        #666;

    margin-bottom:
        6px;

}}


.card-value {{

    font-size:
        23px;

    font-weight:
        700;

}}


.chart-container {{

    height:
        430px;

    margin:
        18px 0 50px;

    border:
        1px solid
        #ddd;

    border-radius:
        10px;

    padding:
        14px;

}}


.table-wrapper {{

    overflow-x:
        auto;

    margin-bottom:
        42px;

    border:
        1px solid
        #ddd;

    border-radius:
        10px;

}}


table {{

    width:
        100%;

    border-collapse:
        collapse;

    min-width:
        900px;

}}


th,
td {{

    padding:
        10px 12px;

    border-bottom:
        1px solid
        #ddd;

    text-align:
        right;

    white-space:
        nowrap;

}}


th {{

    background:
        #f5f5f5;

    position:
        sticky;

    top:
        0;

    z-index:
        1;

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


.pending-row {{

    background:
        #fff1f2;

}}


.pending-row:hover {{

    background:
        #ffe4e6;

}}


details {{

    margin:
        22px 0 45px;

    border:
        1px solid
        #ddd;

    border-radius:
        10px;

    padding:
        16px;

}}


summary {{

    cursor:
        pointer;

    font-size:
        17px;

    font-weight:
        650;

}}


details .table-wrapper {{

    margin-top:
        18px;

    margin-bottom:
        0;

}}


small,
.small {{

    color:
        #777;

}}

</style>

</head>


<body>


<h1>
    TransferTracker 성능 분석 리포트
</h1>


<p class="description">

    이번 Run의 실제 k6 VU,
    API latency / RPS,
    HikariCP 상태를

    동일한 시간축으로 분석합니다.

</p>


<div class="warning">

<strong>
    Hikari 해석 주의:
</strong>

현재 Collector는

active /
idle /
pending /
max

를 각각 별도 HTTP 요청으로 읽습니다.

따라서 같은 CSV row의 네 값은
완전히 동일한 찰나의
원자적 snapshot이 아닙니다.

Pending 발생 여부와
시간대 추세를 중심으로 보세요.

</div>


<!-- ======================================================= -->
<!-- Summary -->
<!-- ======================================================= -->

<div class="cards">


    <div class="card">

        <div class="card-title">
            실제 Peak VU
        </div>

        <div class="card-value">
            {fmt_int(actual_peak_vu)}
        </div>

    </div>


    <div class="card">

        <div class="card-title">
            k6 Configured Max VU
        </div>

        <div class="card-value">
            {fmt_int(configured_max_vu)}
        </div>

    </div>


    <div class="card">

        <div class="card-title">
            실제 테스트 시간
        </div>

        <div class="card-value">
            {test_duration_seconds}s
        </div>

    </div>


    <div class="card">

        <div class="card-title">
            전체 요청 수
        </div>

        <div class="card-value">
            {total_requests:,}
        </div>

    </div>


    <div class="card">

        <div class="card-title">
            전체 실패율
        </div>

        <div class="card-value">
            {overall_fail_rate * 100:.2f}%
        </div>

    </div>


    <div class="card">

        <div class="card-title">
            최고 전체 P95
        </div>

        <div class="card-value">
            {max_p95_api['p95']:.2f} ms
        </div>

        <div>
            {
html.escape(
    max_p95_api['name']
)
}
        </div>

    </div>


    <div class="card">

        <div class="card-title">
            최고 전체 P99
        </div>

        <div class="card-value">
            {max_p99_api['p99']:.2f} ms
        </div>

        <div>
            {
html.escape(
    max_p99_api['name']
)
}
        </div>

    </div>


    <div class="card">

        <div class="card-title">
            Hikari Pool Max
        </div>

        <div class="card-value">
            {fmt(pool_max, 0)}
        </div>

    </div>


    <div class="card">

        <div class="card-title">
            Peak Active
        </div>

        <div class="card-value">
            {fmt(peak_active, 0)}
        </div>

    </div>


    <div class="card">

        <div class="card-title">
            Minimum Idle
        </div>

        <div class="card-value">
            {fmt(minimum_idle, 0)}
        </div>

    </div>


    <div class="card">

        <div class="card-title">
            Max Pending
        </div>

        <div class="card-value">
            {fmt(max_pending, 0)}
        </div>

    </div>


    <div class="card">

        <div class="card-title">
            Pending Samples
        </div>

        <div class="card-value">
            {pending_sample_count}
        </div>

        <div>
            {hikari_status}
        </div>

    </div>


</div>


<p class="description">

<strong>
    자동 탐지 Hold:
</strong>

{html.escape(hold_summary_text)}

</p>


<!-- ======================================================= -->
<!-- VU + RPS -->
<!-- ======================================================= -->

<h2>
    1. 실제 VU + 전체 RPS Timeline
</h2>


<p class="description">

    Profile 이름이 아니라
    k6가 실제로 기록한 VU와

    초당 전체 요청 수입니다.

</p>


<div class="chart-container">

    <canvas id="vuRpsChart"></canvas>

</div>


<!-- ======================================================= -->
<!-- Hikari -->
<!-- ======================================================= -->

<h2>
    2. HikariCP Timeline
</h2>


<p class="description">

    Pending이 0보다 커지는 시점과

    Active /
    Idle

    흐름을 확인하세요.

</p>


<div class="chart-container">

    <canvas id="hikariChart"></canvas>

</div>


<!-- ======================================================= -->
<!-- Pending -->
<!-- ======================================================= -->

<h2>
    3. Pending 발생 시점
</h2>


<p class="description">

    Pending &gt; 0인 sample만 추출하고,

    가장 가까운 초의
    VU /
    RPS /
    API 응답시간과 연결합니다.

</p>


<div class="table-wrapper">

<table>

<thead>

<tr>

    <th>Time</th>
    <th>VU</th>
    <th>Active</th>
    <th>Idle</th>
    <th>Pending</th>
    <th>Pool Max</th>
    <th>Total RPS</th>
    <th>Highest AVG API</th>
    <th>Highest MAX API</th>

</tr>

</thead>


<tbody>

{pending_summary_rows}

</tbody>

</table>

</div>


<details>

<summary>
    Pending 발생 시점 API별 상세
</summary>


<div class="table-wrapper">

<table>


<thead>

<tr>

    <th>Time</th>
    <th>VU</th>
    <th>Pending</th>
    <th>API</th>
    <th>RPS</th>
    <th>AVG</th>
    <th>MAX</th>

</tr>

</thead>


<tbody>

{pending_detail_rows}

</tbody>

</table>

</div>

</details>


<!-- ======================================================= -->
<!-- P95 -->
<!-- ======================================================= -->

<h2>
    4. API P95 Timeline
    ({P95_BUCKET_SECONDS}초 Bucket)
</h2>


<p class="description">

    P95는 1초보다 표본을 확보하기 위해

    {P95_BUCKET_SECONDS}초 단위로 계산합니다.

</p>


<div class="chart-container">

    <canvas id="p95Chart"></canvas>

</div>


<!-- ======================================================= -->
<!-- AVG -->
<!-- ======================================================= -->

<h2>
    5. API 1초 AVG Timeline
</h2>


<div class="chart-container">

    <canvas id="avgChart"></canvas>

</div>


<!-- ======================================================= -->
<!-- MAX -->
<!-- ======================================================= -->

<h2>
    6. API 1초 MAX Timeline
</h2>


<p class="description">

    순간적인 Spike를 찾는 용도입니다.

    MAX만으로 병목을 확정하지 말고

    P95 /
    Hikari와 같이 보세요.

</p>


<div class="chart-container">

    <canvas id="maxChart"></canvas>

</div>


<!-- ======================================================= -->
<!-- RPS -->
<!-- ======================================================= -->

<h2>
    7. API별 RPS Timeline
</h2>


<div class="chart-container">

    <canvas id="rpsChart"></canvas>

</div>


<!-- ======================================================= -->
<!-- Hold P95 -->
<!-- ======================================================= -->

<h2>
    8. 자동 탐지 Hold 구간 P95
</h2>


<div class="table-wrapper">

<table>


<thead>

<tr>

    <th>API</th>

    {hold_header}

    <th>
        첫 Hold → 마지막 Hold
    </th>

</tr>

</thead>


<tbody>

{p95_rows}

</tbody>


</table>

</div>


<!-- ======================================================= -->
<!-- Hold P99 -->
<!-- ======================================================= -->

<h2>
    9. 자동 탐지 Hold 구간 P99
</h2>


<div class="table-wrapper">

<table>


<thead>

<tr>

    <th>API</th>

    {hold_header}

    <th>
        첫 Hold → 마지막 Hold
    </th>

</tr>

</thead>


<tbody>

{p99_rows}

</tbody>


</table>

</div>


<!-- ======================================================= -->
<!-- Hold RPS -->
<!-- ======================================================= -->

<h2>
    10. 자동 탐지 Hold 구간 RPS
</h2>


<div class="table-wrapper">

<table>


<thead>

<tr>

    <th>API</th>

    {hold_header}

</tr>

</thead>


<tbody>

{hold_rps_rows}

</tbody>


</table>

</div>


<!-- ======================================================= -->
<!-- 전체 결과 -->
<!-- ======================================================= -->

<h2>
    11. 전체 테스트 결과
</h2>


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

{summary_rows}

</tbody>


</table>

</div>


<!-- ======================================================= -->
<!-- Hold Detail -->
<!-- ======================================================= -->

<details>

<summary>
    자동 탐지 Hold 구간 상세 데이터
</summary>


<div class="table-wrapper">

<table>


<thead>

<tr>

    <th>VU</th>
    <th>실제 시간</th>
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


<!-- ======================================================= -->
<!-- Raw Timeline -->
<!-- ======================================================= -->

<details>

<summary>
    초 단위 Raw Timeline 전체 보기
</summary>


<p class="small">

    Pending &gt; 0인 행은
    붉게 강조됩니다.

    API AVG / MAX 단위는 ms입니다.

</p>


<div class="table-wrapper">

<table>


<thead>

<tr>

    <th>Time</th>
    <th>VU</th>
    <th>Active</th>
    <th>Idle</th>
    <th>Pending</th>
    <th>Pool Max</th>
    <th>Total RPS</th>

    {raw_api_header}

</tr>

</thead>


<tbody>

{raw_rows}

</tbody>


</table>

</div>

</details>


<!-- ======================================================= -->
<!-- Chart -->
<!-- ======================================================= -->

<script>


const common = {{

    responsive:
        true,

    maintainAspectRatio:
        false,

    interaction: {{

        mode:
            'index',

        intersect:
            false

    }},

    plugins: {{

        legend: {{

            position:
                'top'

        }}

    }},

    scales: {{

        x: {{

            title: {{

                display:
                    true,

                text:
                    '테스트 시작 후 시간 (초)'

            }}

        }},

        y: {{

            beginAtZero:
                true

        }}

    }}

}};


/*
 * ==========================================================
 * VU + Total RPS
 * ==========================================================
 */

new Chart(

    document.getElementById(
        'vuRpsChart'
    ),

    {{

        type:
            'line',

        data: {{

            labels:
                {js(timeline_seconds)},

            datasets:
                {js(vu_rps_datasets)}

        }},

        options: {{

            responsive:
                true,

            maintainAspectRatio:
                false,

            interaction: {{

                mode:
                    'index',

                intersect:
                    false

            }},

            plugins: {{

                legend: {{

                    position:
                        'top'

                }}

            }},

            scales: {{

                x: {{

                    title: {{

                        display:
                            true,

                        text:
                            '테스트 시작 후 시간 (초)'

                    }}

                }},

                yVu: {{

                    type:
                        'linear',

                    position:
                        'left',

                    beginAtZero:
                        true,

                    title: {{

                        display:
                            true,

                        text:
                            'VU'

                    }}

                }},

                yRps: {{

                    type:
                        'linear',

                    position:
                        'right',

                    beginAtZero:
                        true,

                    grid: {{

                        drawOnChartArea:
                            false

                    }},

                    title: {{

                        display:
                            true,

                        text:
                            'Total RPS'

                    }}

                }}

            }}

        }}

    }}

);


/*
 * ==========================================================
 * HikariCP
 * ==========================================================
 */

new Chart(

    document.getElementById(
        'hikariChart'
    ),

    {{

        type:
            'line',

        data: {{

            labels:
                {js(hikari_labels)},

            datasets:
                {js(hikari_datasets)}

        }},

        options: {{

            ...common,

            scales: {{

                ...common.scales,

                y: {{

                    beginAtZero:
                        true,

                    title: {{

                        display:
                            true,

                        text:
                            'Connections'

                    }}

                }}

            }}

        }}

    }}

);


/*
 * ==========================================================
 * P95
 * ==========================================================
 */

new Chart(

    document.getElementById(
        'p95Chart'
    ),

    {{

        type:
            'line',

        data: {{

            labels:
                {js(p95_bucket_starts)},

            datasets:
                {js(p95_datasets)}

        }},

        options: {{

            ...common,

            scales: {{

                ...common.scales,

                y: {{

                    beginAtZero:
                        true,

                    title: {{

                        display:
                            true,

                        text:
                            'P95 (ms)'

                    }}

                }}

            }}

        }}

    }}

);


/*
 * ==========================================================
 * AVG
 * ==========================================================
 */

new Chart(

    document.getElementById(
        'avgChart'
    ),

    {{

        type:
            'line',

        data: {{

            labels:
                {js(timeline_seconds)},

            datasets:
                {js(avg_datasets)}

        }},

        options: {{

            ...common,

            scales: {{

                ...common.scales,

                y: {{

                    beginAtZero:
                        true,

                    title: {{

                        display:
                            true,

                        text:
                            'AVG (ms)'

                    }}

                }}

            }}

        }}

    }}

);


/*
 * ==========================================================
 * MAX
 * ==========================================================
 */

new Chart(

    document.getElementById(
        'maxChart'
    ),

    {{

        type:
            'line',

        data: {{

            labels:
                {js(timeline_seconds)},

            datasets:
                {js(max_datasets)}

        }},

        options: {{

            ...common,

            scales: {{

                ...common.scales,

                y: {{

                    beginAtZero:
                        true,

                    title: {{

                        display:
                            true,

                        text:
                            'MAX (ms)'

                    }}

                }}

            }}

        }}

    }}

);


/*
 * ==========================================================
 * API RPS
 * ==========================================================
 */

new Chart(

    document.getElementById(
        'rpsChart'
    ),

    {{

        type:
            'line',

        data: {{

            labels:
                {js(timeline_seconds)},

            datasets:
                {js(rps_datasets)}

        }},

        options: {{

            ...common,

            scales: {{

                ...common.scales,

                y: {{

                    beginAtZero:
                        true,

                    title: {{

                        display:
                            true,

                        text:
                            'Requests / Second'

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
    exist_ok=True
)


with open(
        REPORT_FILE,
        "w",
        encoding="utf-8"
) as f:

    f.write(
        report
    )


# ============================================================
# Terminal Summary
# ============================================================

print()

print(
    "Performance analysis report generated:"
)

print(
    REPORT_FILE
)

print()

print(
    "Actual peak VU      : "
    f"{fmt_int(actual_peak_vu)}"
)

print(
    "Configured max VU   : "
    f"{fmt_int(configured_max_vu)}"
)

print(
    "Detected hold count : "
    f"{len(hold_windows)}"
)

print(
    "Max pending         : "
    f"{fmt(max_pending, 0)}"
)

print(
    "Pending samples     : "
    f"{pending_sample_count}"
)

print()