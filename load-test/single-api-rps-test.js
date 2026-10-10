import http from 'k6/http';
import { check } from 'k6';
import {
    Trend,
    Rate,
    Counter,
} from 'k6/metrics';


const BASE_URL =
    __ENV.BASE_URL || 'http://localhost:8080';

const RESULT_DIR =
    __ENV.RESULT_DIR
    || 'load-test/results/single-api-rps';

const API_KEY =
    __ENV.API_KEY || 'team-transfers';

const TARGET_RPS =
    Number(__ENV.TARGET_RPS || 40);

const DURATION =
    __ENV.DURATION || '60s';

const PRE_ALLOCATED_VUS =
    Number(__ENV.PRE_ALLOCATED_VUS || 20);

const MAX_VUS =
    Number(__ENV.MAX_VUS || 100);


// ============================================================
// API
// ============================================================

const API_CONFIGS = {

    'team-transfers': {
        key: 'team_transfers_list',
        name: 'Team Transfers List',
        method: 'GET',
        url: `${BASE_URL}/api/transfers/team/2`,
    },

    'transfers': {
        key: 'transfers_list',
        name: 'Transfers List',
        method: 'GET',
        url: `${BASE_URL}/api/transfers`,
    },
};


const API =
    API_CONFIGS[API_KEY];


if (!API) {

    throw new Error(
        `지원하지 않는 API_KEY 입니다: ${API_KEY}`
    );
}


// ============================================================
// k6 Options
// ============================================================

export const options = {

    scenarios: {

        fixed_rps: {

            executor:
                'constant-arrival-rate',

            rate:
                TARGET_RPS,

            timeUnit:
                '1s',

            duration:
                DURATION,

            preAllocatedVUs:
                PRE_ALLOCATED_VUS,

            maxVUs:
                MAX_VUS,

            gracefulStop:
                '10s',
        },
    },

    summaryTrendStats: [
        'avg',
        'p(95)',
        'p(99)',
        'max',
    ],
};


// ============================================================
// Custom Metric
// ============================================================

const duration =
    new Trend(
        `${API.key}_duration`,
        true
    );


const fail =
    new Rate(
        `${API.key}_fail`
    );


const requests =
    new Counter(
        `${API.key}_requests`
    );


// ============================================================
// Scenario
// ============================================================

export default function () {

    const response =
        http.request(
            API.method,
            API.url,
            null,
            {
                tags: {
                    api: API.key,
                },
            }
        );


    duration.add(
        response.timings.duration
    );


    const failed =
        response.status !== 200;


    fail.add(
        failed
    );


    requests.add(1);


    check(
        response,
        {
            [`${API.name} status is 200`]:
                (res) =>
                    res.status === 200,
        }
    );
}


// ============================================================
// Summary
// ============================================================

export function handleSummary(data) {

    const durationMetric =
        data.metrics[
            `${API.key}_duration`
        ];

    const failMetric =
        data.metrics[
            `${API.key}_fail`
        ];

    const requestMetric =
        data.metrics[
            `${API.key}_requests`
        ];


    const droppedIterations =
        data.metrics
            .dropped_iterations
            ?.values
            ?.count
        ?? 0;


    const result = {

        [API.key]: {

            name:
                API.name,

            targetRps:
                TARGET_RPS,

            duration:
                DURATION,

            actualRps:
                requestMetric
                    .values
                    .rate,

            rps:
                requestMetric
                    .values
                    .rate,

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

            max:
                durationMetric
                    .values.max,

            failRate:
                failMetric
                    .values
                    .rate,

            requestCount:
                requestMetric
                    .values
                    .count,

            droppedIterations:
                droppedIterations,
        },
    };


    const markdown = [

        '# Single API Fixed RPS Test',

        '',

        `API: ${API.name}`,

        `Target RPS: ${TARGET_RPS}`,

        `Actual RPS: ${result[API.key].actualRps.toFixed(2)}`,

        `Duration: ${DURATION}`,

        '',

        '| AVG | P95 | P99 | MAX | Fail Rate | Requests | Dropped |',

        '|---:|---:|---:|---:|---:|---:|---:|',

        (
            `| ${result[API.key].avg.toFixed(2)} ms ` +
            `| ${result[API.key].p95.toFixed(2)} ms ` +
            `| ${result[API.key].p99.toFixed(2)} ms ` +
            `| ${result[API.key].max.toFixed(2)} ms ` +
            `| ${(result[API.key].failRate * 100).toFixed(2)}% ` +
            `| ${result[API.key].requestCount} ` +
            `| ${result[API.key].droppedIterations} |`
        ),

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