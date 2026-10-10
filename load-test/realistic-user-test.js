import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';


const BASE_URL =
    __ENV.BASE_URL || 'http://localhost:8080';

const RESULT_DIR =
    __ENV.RESULT_DIR || 'load-test/results/realistic-user';

const VU_1 =
    Number(__ENV.VU_1 || 10);

const VU_2 =
    Number(__ENV.VU_2 || 25);

const VU_3 =
    Number(__ENV.VU_3 || 30);

const THINK_TIME_SECONDS =
    Number(__ENV.THINK_TIME_SECONDS || 1);


/**
 * 실제 사용자 요청 비율
 *
 * Transfer = 80%
 * Player   = 15%
 * Team     = 5%
 */
const APIs = [

    // ========================================================
    // Transfer - 80%
    // ========================================================

    {
        key: 'transfers_list',
        name: 'Transfers List',
        method: 'GET',
        url: `${BASE_URL}/api/transfers`,
        weight: 40,
    },

    {
        key: 'team_transfers_list',
        name: 'Team Transfers List',
        method: 'GET',
        url: `${BASE_URL}/api/transfers/team/2`,
        weight: 40,
    },


    // ========================================================
    // Player - 15%
    // ========================================================

    {
        key: 'players_list',
        name: 'Players List',
        method: 'GET',
        url: `${BASE_URL}/api/players?page=0`,
        weight: 8,
    },

    {
        key: 'players_league_filter',
        name: 'Players - EPL Filter',
        method: 'GET',
        url: `${BASE_URL}/api/players?page=0&leagueCode=EPL`,
        weight: 3,
    },

    {
        key: 'players_team_filter',
        name: 'Players - Team Filter',
        method: 'GET',
        url: `${BASE_URL}/api/players?page=0&teamId=2`,
        weight: 2,
    },

    {
        key: 'players_league_team_filter',
        name: 'Players - EPL + Team Filter',
        method: 'GET',
        url:
            `${BASE_URL}` +
            `/api/players?page=0&leagueCode=EPL&teamId=2`,
        weight: 2,
    },


    // ========================================================
    // Team - 5%
    // ========================================================

    {
        key: 'teams_list',
        name: 'Team List',
        method: 'GET',
        url: `${BASE_URL}/api/teams`,
        weight: 3,
    },

    {
        key: 'team_detail',
        name: 'Team Detail',
        method: 'GET',
        url: `${BASE_URL}/api/team/test/2`,
        weight: 2,
    },
];


// ============================================================
// Weight Validation
// ============================================================

const TOTAL_WEIGHT =
    APIs.reduce(
        (sum, api) => sum + api.weight,
        0
    );

if (TOTAL_WEIGHT !== 100) {

    throw new Error(
        `API weight total must be 100. ` +
        `Current: ${TOTAL_WEIGHT}`
    );
}


// ============================================================
// k6 Options
// ============================================================

export const options = {

    stages: [
        { duration: '10s', target: VU_1 },
        { duration: '60s', target: VU_1 },

        { duration: '10s', target: VU_2 },
        { duration: '60s', target: VU_2 },

        { duration: '10s', target: VU_3 },
        { duration: '60s', target: VU_3 },

        { duration: '20s', target: VU_1 },
        { duration: '10s', target: 0 },
    ],

    summaryTrendStats: [
        'avg',
        'p(95)',
        'p(99)',
        'max',
    ],
};

// ============================================================
// Custom Metrics
// ============================================================

const metrics = {};


for (const api of APIs) {

    metrics[api.key] = {

        duration:
            new Trend(
                `${api.key}_duration`,
                true
            ),

        fail:
            new Rate(
                `${api.key}_fail`
            ),

        requests:
            new Counter(
                `${api.key}_requests`
            ),
    };
}


// ============================================================
// API 선택
//
// Math.random()
// 0.0 <= random < 1.0
//
// 100을 곱해서
// 0 <= random < 100
//
// cumulativeWeight를 이용해
// weight에 맞는 API 하나를 선택한다.
// ============================================================

function selectApi() {

    const random =
        Math.random() * TOTAL_WEIGHT;

    let cumulativeWeight = 0;


    for (const api of APIs) {

        cumulativeWeight += api.weight;


        if (random < cumulativeWeight) {

            return api;
        }
    }


    // 부동소수점 오차 방어
    return APIs[APIs.length - 1];
}


// ============================================================
// User Scenario
// ============================================================

export default function () {

    /**
     * 한 명의 사용자는
     * 한 iteration에서 API 하나만 호출한다.
     */
    const api =
        selectApi();


    const response =
        http.request(
            api.method,
            api.url,
            null,
            {
                tags: {
                    api: api.key,
                },
            }
        );


    // ========================================================
    // Metric
    // ========================================================

    metrics[api.key]
        .duration
        .add(
            response.timings.duration
        );


    const failed =
        response.status !== 200;


    metrics[api.key]
        .fail
        .add(
            failed
        );


    metrics[api.key]
        .requests
        .add(1);


    check(
        response,
        {
            [`${api.name} status is 200`]:
                (res) =>
                    res.status === 200,
        }
    );


    // ========================================================
    // Think Time
    //
    // 사용자가 응답을 확인하고
    // 다음 행동을 하기까지 1초
    // ========================================================

    sleep(
        THINK_TIME_SECONDS
    );
}


// ============================================================
// Summary
// ============================================================

export function handleSummary(data) {

    const result = {};


    // ========================================================
    // API 결과
    // ========================================================

    for (const api of APIs) {

        const key =
            api.key;

        const durationMetric =
            data.metrics[
                `${key}_duration`
            ];

        const requestMetric =
            data.metrics[
                `${key}_requests`
            ];

        const failMetric =
            data.metrics[
                `${key}_fail`
            ];


        /**
         * 낮은 비율 API가 우연히 한 번도 호출되지 않은
         * 극단적인 경우까지 방어한다.
         */
        if (
            !durationMetric
            || !requestMetric
            || !failMetric
        ) {

            result[key] = {

                name:
                    api.name,

                weight:
                    api.weight,

                avg:
                    null,

                p95:
                    null,

                p99:
                    null,

                rps:
                    0,

                failRate:
                    0,

                requestCount:
                    0,
            };

            continue;
        }


        result[key] = {

            name:
                api.name,

            weight:
                api.weight,

            avg:
                durationMetric
                    .values
                    .avg,

            p95:
                durationMetric
                    .values['p(95)'],

            p99:
                durationMetric
                    .values['p(99)'],

            rps:
                requestMetric
                    .values
                    .rate,

            failRate:
                failMetric
                    .values
                    .rate,

            requestCount:
                requestMetric
                    .values
                    .count,
        };
    }


    // ========================================================
    // 전체 요청 수 / 전체 RPS
    // ========================================================

    let totalRequests = 0;
    let totalRps = 0;


    for (const api of APIs) {

        const apiResult =
            result[api.key];


        totalRequests +=
            apiResult.requestCount;


        totalRps +=
            apiResult.rps;
    }


    // ========================================================
    // Markdown
    // ========================================================

    const markdownRows =
        APIs.map(
            (api) => {

                const value =
                    result[api.key];


                const avg =
                    value.avg === null
                        ? '-'
                        : `${value.avg.toFixed(2)} ms`;

                const p95 =
                    value.p95 === null
                        ? '-'
                        : `${value.p95.toFixed(2)} ms`;

                const p99 =
                    value.p99 === null
                        ? '-'
                        : `${value.p99.toFixed(2)} ms`;


                return (
                    `| ${value.name} ` +
                    `| ${value.weight}% ` +
                    `| ${avg} ` +
                    `| ${p95} ` +
                    `| ${p99} ` +
                    `| ${value.rps.toFixed(2)} ` +
                    `| ${(value.failRate * 100).toFixed(2)}% ` +
                    `| ${value.requestCount} |`
                );
            }
        );


    const markdown = [

        '# Realistic User 성능 테스트 결과',

        '',

        `- Think Time: ${THINK_TIME_SECONDS}s`,
        `- Total Requests: ${totalRequests}`,
        `- Total RPS: ${totalRps.toFixed(2)}`,

        '',

        '| API | Weight | AVG | P95 | P99 | RPS | 실패율 | 요청 수 |',

        '|:---|---:|---:|---:|---:|---:|---:|---:|',

        ...markdownRows,

        '',

    ].join('\n');


    return {

        [`${RESULT_DIR}/all-api-result.json`]:
            JSON.stringify(
                result,
                null,
                2
            ),

        [`${RESULT_DIR}/all-api-result.md`]:
            markdown,
    };
}