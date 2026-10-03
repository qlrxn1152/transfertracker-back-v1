import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

const RESULT_DIR = __ENV.RESULT_DIR || 'load-test/results/local';

const VU_1 = Number(__ENV.VU_1 || 10);
const VU_2 = Number(__ENV.VU_2 || 25);
const VU_3 = Number(__ENV.VU_3 || 30);

    const APIs = [
         {
             key: 'transfers_list',
             name: 'Transfers List',
             method: 'GET',
             url: `${BASE_URL}/api/transfers`,
         },

         {
             key: 'team_transfers_list',
             name: 'Team Transfers List',
             method: 'GET',
             url: `${BASE_URL}/api/transfers/team/2`,
         },

         {
             key: 'teams_list',
             name: 'Team List',
             method: 'GET',
             url: `${BASE_URL}/api/teams`,
         },

         {
             key: 'team_detail',
             name: 'Team Detail',
             method: 'GET',
             url: `${BASE_URL}/api/team/test/2`,
         },

         // 이번에 개선한 Player 조회
         {
             key: 'players_list',
             name: 'Players List',
             method: 'GET',
             url: `${BASE_URL}/api/players?page=0`,
         },

         {
             key: 'players_league_filter',
             name: 'Players - EPL Filter',
             method: 'GET',
             url: `${BASE_URL}/api/players?page=0&leagueCode=EPL`,
         },

         {
             key: 'players_team_filter',
             name: 'Players - Team Filter',
             method: 'GET',
             url: `${BASE_URL}/api/players?page=0&teamId=2`,
         },

         {
             key: 'players_league_team_filter',
             name: 'Players - EPL + Team Filter',
             method: 'GET',
             url: `${BASE_URL}/api/players?page=0&leagueCode=EPL&teamId=2`,
         },

    ]

export const options = {

    stages: [
        { duration: '10s', target: VU_1 },
        { duration: '20s', target: VU_1 },

        { duration: '10s', target: VU_2 },
        { duration: '30s', target: VU_2 },

        { duration: '10s', target: VU_3 },
        { duration: '30s', target: VU_3 },

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

const metrics = {};

for (const api of APIs) {

    metrics[api.key] = {
        duration: new Trend(`${api.key}_duration`, true),
        fail: new Rate(`${api.key}_fail`),
        requests: new Counter(`${api.key}_requests`),
    };
}




export default function () {

    for (const api of APIs) {

        const response = http.request(
            api.method,
            api.url,
            null,
            {
                tags: {
                    api: api.key,
                },
            }
        );

        metrics[api.key].duration.add(
            response.timings.duration
        );

        metrics[api.key].fail.add(
            response.status !== 200
        );

        metrics[api.key].requests.add(1);

        check(response, {
            [`${api.name} status is 200`]:
                (res) => res.status === 200,
        });
    }

    sleep(1);
}

export function handleSummary(data) {

    const result = {};

    /**
     * JSON 결과 자동 생성
     */
    for (const api of APIs) {

        const key = api.key;

        result[key] = {
            name: api.name,

            avg:
                data.metrics[`${key}_duration`]
                    .values.avg,

            p95:
                data.metrics[`${key}_duration`]
                    .values['p(95)'],

            p99:
                data.metrics[`${key}_duration`]
                    .values['p(99)'],

            rps:
                data.metrics[`${key}_requests`]
                    .values.rate,

            failRate:
                data.metrics[`${key}_fail`]
                    .values.rate,

            requestCount:
                data.metrics[`${key}_requests`]
                    .values.count,
        };
    }


    /**
     * Markdown 표 자동 생성
     */
    const markdownRows = APIs.map(api => {

        const value = result[api.key];

        return (
            `| ${value.name} ` +
            `| ${value.avg.toFixed(2)} ms ` +
            `| ${value.p95.toFixed(2)} ms ` +
            `| ${value.p99.toFixed(2)} ms ` +
            `| ${value.rps.toFixed(2)} ` +
            `| ${(value.failRate * 100).toFixed(2)}% ` +
            `| ${value.requestCount} |`
        );
    });


    const markdown = [
        '# API 성능 테스트 결과',
        '',
        '| API | AVG | P95 ( 95% )  | P99 ( 99% ) | RPS (1초당 완료한 요청 수 )| 실패율 | 요청 수 |',
        '|:---|---:|---:|---:|---:|---:|---:|',
        ...markdownRows,
        '',
    ].join('\n');


    return {
        [`${RESULT_DIR}/all-api-result.json`]:
            JSON.stringify(result, null, 2),

        [`${RESULT_DIR}/all-api-result.md`]:
            markdown,
    };
}
