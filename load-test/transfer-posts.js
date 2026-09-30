import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    stages: [
        { duration: '30s', target : 100 },
        { duration: '20s', target : 300 },
        { duration: '1m', target : 300 },
        { duration: '20s', target : 50 },
        { duration: '10s', target : 0 },
    ],

    summaryTrendStats: [
        'avg',
        'p(95)',
        'p(99)',
        'max',
    ],
};

export default function () {

    const response = http.get(
        'http://localhost:8080/api/transfer/posts'
    );

    check(response, {
        'status is 200': (res) => res.status === 200,
    });

    sleep(1);
}

export function handleSummary(data) {

    const result = {
        api: 'transfer-posts',

        avg: data.metrics.http_req_duration.values.avg,
        p95: data.metrics.http_req_duration.values['p(95)'],
        p99: data.metrics.http_req_duration.values['p(99)'],

        rps: data.metrics.http_reqs.values.rate,

        failRate: data.metrics.http_req_failed.values.rate,

        requestCount: data.metrics.http_reqs.values.count,
    };

    return {
        'load-test/results/transfer-posts.json':
            JSON.stringify(result, null, 2),
    };
}