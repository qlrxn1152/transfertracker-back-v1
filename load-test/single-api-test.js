import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';


// ============================================================
// Target API
// ============================================================

const BASE_URL =
    __ENV.BASE_URL
    || 'http://localhost:8080';


const TARGET_PATH =
    __ENV.TARGET_PATH
    || '/api/players?page=0';


const API_KEY =
    (__ENV.API_KEY || 'single_api')
        .replace(
            /[^a-zA-Z0-9_]/g,
            '_'
        );


const API_NAME =
    __ENV.API_NAME
    || 'Single API';


const METHOD =
    (__ENV.METHOD || 'GET')
        .toUpperCase();


const RESULT_DIR =
    __ENV.RESULT_DIR
    || 'load-test/results/single-api';


// ============================================================
// Load
//
// 기본:
// 30 VU → 40 VU → 50 VU
//
// 환경변수로 변경 가능
// ============================================================

const VU_1 =
    Number(
        __ENV.VU_1
        || 30
    );


const VU_2 =
    Number(
        __ENV.VU_2
        || 40
    );


const VU_3 =
    Number(
        __ENV.VU_3
        || 50
    );


const SLEEP_SECONDS =
    Number(
        __ENV.SLEEP_SECONDS
        || 1
    );


// ============================================================
// Optional Request Body
//
// GET:
//
// 별도 설정 필요 없음.
//
// POST:
//
// METHOD=POST
// REQUEST_BODY='{"name":"test"}'
// ============================================================

const REQUEST_BODY =
    __ENV.REQUEST_BODY
    || null;


const CONTENT_TYPE =
    __ENV.CONTENT_TYPE
    || 'application/json';


// ============================================================
// Target
// ============================================================

const TARGET_API = {

    key:
        API_KEY,

    name:
        API_NAME,

    method:
        METHOD,

    url:
        `${BASE_URL}${TARGET_PATH}`,
};


// ============================================================
// Custom Metrics
// ============================================================

const duration =
    new Trend(
        `${API_KEY}_duration`,
        true
    );


const fail =
    new Rate(
        `${API_KEY}_fail`
    );


const requests =
    new Counter(
        `${API_KEY}_requests`
    );


// ============================================================
// Scenario
//
// 기존 all-api-test와 동일한 형태로 유지.
//
// Ramp
// ↓
// Hold
// ↓
// Ramp
// ↓
// Hold
//
// report.py가 실제 vus Metric을 보고
// Hold 구간을 자동으로 탐지한다.
// ============================================================

export const options = {

    stages: [

        // ----------------------------------------------------
        // VU 1
        // ----------------------------------------------------

        {
            duration: '10s',
            target: VU_1,
        },

        {
            duration: '20s',
            target: VU_1,
        },


        // ----------------------------------------------------
        // VU 2
        // ----------------------------------------------------

        {
            duration: '10s',
            target: VU_2,
        },

        {
            duration: '30s',
            target: VU_2,
        },


        // ----------------------------------------------------
        // VU 3
        // ----------------------------------------------------

        {
            duration: '10s',
            target: VU_3,
        },

        {
            duration: '30s',
            target: VU_3,
        },


        // ----------------------------------------------------
        // Ramp Down
        // ----------------------------------------------------

        {
            duration: '20s',
            target: VU_1,
        },

        {
            duration: '10s',
            target: 0,
        },
    ],


    summaryTrendStats: [
        'avg',
        'p(95)',
        'p(99)',
        'max',
    ],
};


// ============================================================
// Test
// ============================================================

export default function () {

    const params = {

        tags: {

            /*
             * 매우 중요.
             *
             * 기존 report.py가
             * extra_tags의 api 값을 읽어
             * API별 Metric을 구분한다.
             */
            api:
                TARGET_API.key,
        },

        headers: {

            'Content-Type':
                CONTENT_TYPE,
        },
    };


    const response =
        http.request(

            TARGET_API.method,

            TARGET_API.url,

            REQUEST_BODY,

            params
        );


    // --------------------------------------------------------
    // Custom Duration
    // --------------------------------------------------------

    duration.add(
        response.timings.duration
    );


    // --------------------------------------------------------
    // Failure
    // --------------------------------------------------------

    const isFailed =

        response.status < 200

        ||

        response.status >= 300;


    fail.add(
        isFailed
    );


    // --------------------------------------------------------
    // Request Count
    // --------------------------------------------------------

    requests.add(
        1
    );


    // --------------------------------------------------------
    // Check
    // --------------------------------------------------------

    check(
        response,
        {

            [`${TARGET_API.name} status is 2xx`]:

                (res) =>

                    res.status >= 200

                    &&

                    res.status < 300,
        }
    );


    // --------------------------------------------------------
    // 기존 all-api-test와 동일하게
    // VU당 1초 간격 유지
    // --------------------------------------------------------

    sleep(
        SLEEP_SECONDS
    );
}


// ============================================================
// Summary
//
// 중요:
// 기존 report.py가 읽을 수 있도록
//
// {
//   "api_key": {
//       ...
//   }
// }
//
// 형태를 유지한다.
// ============================================================

export function handleSummary(data) {

    const durationMetric =

        data.metrics[
            `${API_KEY}_duration`
        ];


    const failMetric =

        data.metrics[
            `${API_KEY}_fail`
        ];


    const requestMetric =

        data.metrics[
            `${API_KEY}_requests`
        ];


    const apiResult = {

        name:
            TARGET_API.name,

        method:
            TARGET_API.method,

        url:
            TARGET_API.url,


        avg:
            durationMetric.values.avg,


        p95:
            durationMetric.values['p(95)'],


        p99:
            durationMetric.values['p(99)'],


        max:
            durationMetric.values.max,


        rps:
            requestMetric.values.rate,


        failRate:
            failMetric.values.rate,


        requestCount:
            requestMetric.values.count,
    };


    /*
     * 기존 report.py와 호환되는 구조.
     */
    const result = {

        [TARGET_API.key]:
            apiResult,
    };


    const markdown = [

        '# Single API 성능 테스트 결과',

        '',

        `- API: ${TARGET_API.name}`,

        `- Method: ${TARGET_API.method}`,

        `- URL: ${TARGET_API.url}`,

        `- VU: ${VU_1} → ${VU_2} → ${VU_3}`,

        '',

        '| AVG | P95 | P99 | MAX | RPS | 실패율 | 요청 수 |',

        '|---:|---:|---:|---:|---:|---:|---:|',

        (
            `| ${apiResult.avg.toFixed(2)} ms ` +

            `| ${apiResult.p95.toFixed(2)} ms ` +

            `| ${apiResult.p99.toFixed(2)} ms ` +

            `| ${apiResult.max.toFixed(2)} ms ` +

            `| ${apiResult.rps.toFixed(2)} ` +

            `| ${(apiResult.failRate * 100).toFixed(2)}% ` +

            `| ${apiResult.requestCount} |`
        ),

        '',
    ].join('\n');


    return {

        /*
         * 기존 report.py에서 읽는 이름과 동일하게 맞춘다.
         */
        [`${RESULT_DIR}/all-api-result.json`]:

            JSON.stringify(
                result,
                null,
                2
            ),


        [`${RESULT_DIR}/single-api-result.md`]:

            markdown,
    };
}