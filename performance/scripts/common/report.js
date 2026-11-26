
import { simpleReport } from '../k6-simple-report.js';
import { baseUrl } from './env.js';

function thresholdPassed(data) {
    return Object.values(data.metrics).every((metric) =>
        Object.values(metric.thresholds || {}).every(
            (threshold) => threshold.ok === true
        )
    );
}

export function createSummaryHandler({ scenario, configuration }) {
    return function handleSummary(data) {
        const reportDirectory = __ENV.REPORT_DIR;

        if (!reportDirectory) return {};

        const endpoint = configuration.endpoint
            ? `GET ${configuration.endpoint}`
            : 'Multiple endpoints';

        const metadata = {
            runId: __ENV.PERF_RUN_ID || null,
            startedAt: __ENV.PERF_STARTED_AT || null,
            finishedAt: null,
            actualDurationSeconds: null,
            environment: __ENV.PERF_ENVIRONMENT || null,
            scenario,
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
            configuration,
            result: {
                status: 'pending',
                thresholdPassed: thresholdPassed(data),
                exitCode: null,
            },
        };

        return {
            [`${reportDirectory}/report.html`]: simpleReport(data, {
                scenario,
                endpoint,
            }),

            [`${reportDirectory}/summary.json`]: JSON.stringify(
                data, null, 2
            ),

            [`${reportDirectory}/metadata.json`]: JSON.stringify(
                metadata, null, 2
            ),
        };
    };
}
