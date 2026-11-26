import { check } from 'k6';
import { getJson } from '../common/http.js';
import { createSummaryHandler } from '../common/report.js';
import { createReadOptions } from '../common/scenario.js';
import { isObject, listChecks } from '../common/read-case.js';
import { page } from '../common/env.js';

const profile = createReadOptions('mixed-read');
const routes = [
    {
        name: 'posts-list',
        path: `/api/posts?page=${page()}`,
        checks: (payload) => listChecks(payload, 'posts', { pagination: true }),
    },
    {
        name: 'players-list',
        path: `/api/players?page=${page()}`,
        checks: (payload) => listChecks(payload, 'players', { pagination: true }),
    },
    {
        name: 'teams-list',
        path: '/api/teams',
        checks: (payload) => listChecks(payload, 'teams'),
    },
    {
        name: 'transfers-list',
        path: `/api/transfers?page=${page()}`,
        checks: (payload) => listChecks(payload, 'transfers', { pagination: true }),
    },
    {
        name: 'post-sources',
        path: '/api/posts/sources',
        checks: (payload) => listChecks(payload, 'sources'),
    },
];

export const options = profile.options;

export default function () {
    const route = routes[__ITER % routes.length];
    const result = getJson(route.path);
    check(result.response, {
        [`mixed-read ${route.name}: status is 200`]: () => result.response.status === 200,
        [`mixed-read ${route.name}: response is JSON object`]: () => isObject(result.payload),
        ...route.checks(result.payload),
    });
}

export function handleSummary(data) {
    return createSummaryHandler({
        scenario: 'mixed-read',
        configuration: {
            executor: profile.load.executor,
            rate: profile.load.rate,
            timeUnit: profile.load.timeUnit,
            targetRps: profile.load.rate,
            duration: profile.load.duration,
            preAllocatedVUs: profile.load.preAllocatedVUs,
            maxVUs: profile.load.maxVUs,
            routes: routes.map(({ name, path }) => ({ name, path })),
            requiresAuth: false,
        },
    })(data);
}
