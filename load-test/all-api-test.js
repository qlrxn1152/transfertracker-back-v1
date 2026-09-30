import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';

    const APIs = [
        {
            key: 'team_detail',
            name: 'Team Detail',
            method: 'GET',
            url: 'http://localhost:8080/api/team/test/2'
        },

        {
            key: 'transfer_posts',
            name: 'Transfer Posts',
            method: 'GET',
            url: 'http://localhost:8080/api/transfer/posts'
        },

        {
            key: 'players_list',
            name: 'Player List',
            method: 'GET',
            url: 'http://localhost:8080/api/players'
        },

        {
            key: 'teams_list',
            name: 'Team List',
            method: 'GET',
            url: 'http://localhost:8080/api/teams'
        },

        {
            key: 'transfers_list',
            name: 'Transfers List',
            method: 'GET',
            url: 'http://localhost:8080/api/transfers'
        },
    ]

export const options = {

    stages: [
        { duration: '20s', target: 20 },
        { duration: '30s', target: 40 },
        { duration: '1m', target: 45 },
        { duration: '30s', target: 30 },
        { duration: '10s', target: 20 },
        { duration: '20s', target: 0 },
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
        'load-test/results/all-api-result.json':
            JSON.stringify(result, null, 2),

        'load-test/results/all-api-result.md':
            markdown,
    };
}
