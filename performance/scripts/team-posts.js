import http from 'k6/http';
import { check } from 'k6';
import { simpleReport } from './k6-simple-report.js';

const baseUrl = __ENV.BASE_URL || 'http://localhost:8081';
const authToken = __ENV.AUTH_TOKEN || '';

function readPositiveInteger(name, fallback) {
    const value = __ENV[name];
    if (value === undefined || value === '') {
        return fallback;
    }

    const parsed = Number.parseInt(value, 10);
    if (!Number.isInteger(parsed) || parsed <= 0) {
        throw new Error(`${name} must be a positive integer.`);
    }
    return parsed;
}

function readNonNegativeInteger(name, fallback) {
    const value = __ENV[name];
    if (value === undefined || value === '') {
        return fallback;
    }

    const parsed = Number.parseInt(value, 10);
    if (!Number.isInteger(parsed) || parsed < 0) {
        throw new Error(`${name} must be a non-negative integer.`);
    }
    return parsed;
}

const teamId = readPositiveInteger('TEAM_ID', null);
if (teamId === null) {
    throw new Error('TEAM_ID is required for the team-posts scenario.');
}

const page = readNonNegativeInteger('PAGE', 0);
const rate = readPositiveInteger('RPS', 50);
const duration = __ENV.DURATION || '1m';
const configuredPostCount = __ENV.TEAM_POST_COUNT
    ? readNonNegativeInteger('TEAM_POST_COUNT', 0)
    : null;

const mixedTeamIds = (__ENV.MIXED_TEAM_IDS || '')
    .split(',')
    .map((value) => value.trim())
    .filter((value) => value.length > 0)
    .map((value) => {
        const parsed = Number.parseInt(value, 10);
        if (!Number.isInteger(parsed) || parsed <= 0) {
            throw new Error('MIXED_TEAM_IDS must contain positive integer IDs.');
        }
        return parsed;
    });

const targetTeamIds = mixedTeamIds.length > 0 ? mixedTeamIds : [teamId];

const teamPostsScenario = {
    executor: 'constant-arrival-rate',
    rate,
    timeUnit: '1s',
    duration,
    preAllocatedVUs: 50,
    maxVUs: 200,
};

export const options = {
    scenarios: {
        team_posts: teamPostsScenario,
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
    // __VU is stable for one virtual user, which evenly cycles the available
    // IDs across the pre-allocated VUs in a mixed-team run.
    const targetTeamId = targetTeamIds[(__VU - 1) % targetTeamIds.length];
    const response = http.get(
        `${baseUrl}/api/posts/teams/${targetTeamId}?page=${page}`,
        authToken ? { headers: { Authorization: `Bearer ${authToken}` } } : undefined
    );

    let payload = null;
    try {
        payload = response.json();
    } catch (_) {
        // The check below records malformed/non-JSON responses as a failure.
    }

    check(response, {
        'status is 200': (r) => r.status === 200,
        'response has a non-empty posts list': () =>
            Array.isArray(payload?.posts) && payload.posts.length > 0,
        'response posts belong to requested team': () =>
            Array.isArray(payload?.posts)
            && payload.posts.every((post) => post.teamId === targetTeamId),
    });
}

export function handleSummary(data) {
    const reportDirectory = __ENV.REPORT_DIR;
    const report = simpleReport(data);

    if (!reportDirectory) {
        return {};
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
        scenario: __ENV.PERF_SCENARIO || 'team-posts',
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
            executor: teamPostsScenario.executor,
            rate: teamPostsScenario.rate,
            timeUnit: teamPostsScenario.timeUnit,
            targetRps: teamPostsScenario.rate,
            duration: teamPostsScenario.duration,
            preAllocatedVUs: teamPostsScenario.preAllocatedVUs,
            maxVUs: teamPostsScenario.maxVUs,
            page,
            teamId,
            teamIds: targetTeamIds,
            teamPostCount: configuredPostCount,
            responseValidation: 'status + non-empty posts + matching team ID',
            pinpointAgent: __ENV.PERF_PINPOINT_AGENT || 'unknown',
        },
        result: {
            status: 'pending',
            thresholdPassed,
            exitCode: null,
        },
    };

    return {
        [`${reportDirectory}/report.html`]: report,
        [`${reportDirectory}/summary.json`]: JSON.stringify(data, null, 2),
        [`${reportDirectory}/metadata.json`]: JSON.stringify(metadata, null, 2),
    };
}
