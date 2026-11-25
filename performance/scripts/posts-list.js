import http from 'k6/http';
import { check } from 'k6';
import { simpleReport } from './k6-simple-report.js';

const baseUrl = __ENV.BASE_URL || 'http://localhost:8081';
const legacyHtmlReportPath = 'performance/results/posts-afterIndex-report.html';

// Keep the scenario definition in a stable init-context value. k6 normalizes
// the exported options before handleSummary(), so reading options.scenarios
// there is not reliable.
const postsListScenario = {
    executor: 'constant-arrival-rate',
    rate: 50,
    timeUnit: '1s',
    duration: '1m',
    preAllocatedVUs: 50,
    maxVUs: 200,
};

export const options = {
    scenarios: {
        posts_list: postsListScenario,
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
        `${baseUrl}/api/posts?page=0`
    );

    check(response, {
        'status is 200': (r) => r.status === 200,
    });
}

export function handleSummary(data) {
    const report = simpleReport(data);
    const reportDirectory = __ENV.REPORT_DIR;

    if (!reportDirectory) {
        console.log(`HTML 보고서 생성 완료: ${legacyHtmlReportPath}`);

        return {
            [legacyHtmlReportPath]: report,
        };
    }

    const thresholdPassed = Object.values(data.metrics).every((metric) =>
        Object.values(metric.thresholds || {}).every((threshold) => threshold.ok === true)
    );

    const metadata = {
        runId: __ENV.PERF_RUN_ID || null,
        startedAt: __ENV.PERF_STARTED_AT || null,
        finishedAt: null,
        actualDurationSeconds: null,
        environment: __ENV.PERF_ENVIRONMENT || null,
        scenario: __ENV.PERF_SCENARIO || 'posts-list',
        testScript: __ENV.PERF_TEST_SCRIPT || null,
        baseUrl,
        git: {
            commitSha: __ENV.PERF_GIT_COMMIT || null,
            branch: __ENV.PERF_GIT_BRANCH || null,
            dirty: __ENV.PERF_GIT_DIRTY === 'true'
                ? true
                : __ENV.PERF_GIT_DIRTY === 'false'
                    ? false
                    : null,
        },
        k6Version: __ENV.PERF_K6_VERSION || null,
        configuration: {
            executor: postsListScenario.executor || null,
            rate: postsListScenario.rate ?? null,
            timeUnit: postsListScenario.timeUnit || null,
            targetRps: postsListScenario.timeUnit === '1s'
                ? postsListScenario.rate
                : null,
            duration: postsListScenario.duration || null,
            preAllocatedVUs: postsListScenario.preAllocatedVUs ?? null,
            maxVUs: postsListScenario.maxVUs ?? null,
        },
        result: {
            status: 'pending',
            thresholdPassed,
            exitCode: null,
        },
    };

    console.log(`성능 테스트 결과 생성 완료: ${reportDirectory}`);

    return {
        [`${reportDirectory}/report.html`]: report,
        [`${reportDirectory}/summary.json`]: JSON.stringify(data, null, 2),
        [`${reportDirectory}/metadata.json`]: JSON.stringify(metadata, null, 2),
    };
}
