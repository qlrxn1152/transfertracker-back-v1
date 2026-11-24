import http from 'k6/http';
import { check } from 'k6';
import { simpleReport } from './k6-simple-report.js';

export const options = {
    scenarios: {
        posts_list: {
            executor: 'constant-arrival-rate',
            rate: 50,
            timeUnit: '1s',
            duration: '1m',
            preAllocatedVUs: 50,
            maxVUs: 200,
        },
    },

    thresholds: {
        http_req_duration: ['p(95)<300'],
        http_req_failed: ['rate<0.001'],
        checks: ['rate==1'],
        dropped_iterations: ['count==0'],
    },

    summaryTrendStats: [
        'avg',
        'min',
        'med',
        'max',
        'p(90)',
        'p(95)',
        'p(99)',
    ],
};

export default function () {
    const response = http.get(
        'http://localhost:8081/api/posts?page=0'
    );

    check(response, {
        'status is 200': (r) => r.status === 200,
    });
}

export function handleSummary(data) {
    const report = simpleReport(data);

    console.log('HTML 보고서 생성 완료');

    return {
        'performance/results/posts-afterIndex-report.html': report,
    };
}