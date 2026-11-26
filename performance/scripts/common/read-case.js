import { check } from 'k6';
import { getJson } from './http.js';
import { createSummaryHandler } from './report.js';
import { createReadOptions } from './scenario.js';

export function isObject(value) {
    return value !== null && typeof value === 'object' && !Array.isArray(value);
}

export function hasBoolean(value) {
    return typeof value === 'boolean';
}

export function createReadCase(definition) {
    const fixture = definition.fixture ? definition.fixture() : {};
    const profile = createReadOptions(definition.id);
    const configuration = {
        executor: profile.load.executor,
        rate: profile.load.rate,
        timeUnit: profile.load.timeUnit,
        targetRps: profile.load.rate,
        duration: profile.load.duration,
        preAllocatedVUs: profile.load.preAllocatedVUs,
        maxVUs: profile.load.maxVUs,
        endpoint: definition.path(fixture),
        requiresAuth: Boolean(definition.requiresAuth),
        fixtures: fixture,
    };

    return {
        options: profile.options,
        execute() {
            const result = getJson(definition.path(fixture), { requiresAuth: definition.requiresAuth });
            const checks = {
                [`${definition.id}: status is 200`]: () => result.response.status === 200,
                ...(definition.expectJsonObject === false
                    ? {}
                    : { [`${definition.id}: response is JSON object`]: () => isObject(result.payload) }),
                ...definition.checks(result.payload, fixture),
            };
            check(result.response, checks);
        },
        handleSummary: createSummaryHandler({ scenario: definition.id, configuration }),
    };
}

export function listChecks(payload, key, { nonEmpty = true, pagination = false, each } = {}) {
    const values = payload?.[key];
    const checks = {
        [`response has ${key} array`]: () => Array.isArray(values),
    };
    if (nonEmpty) checks[`response has non-empty ${key}`] = () => Array.isArray(values) && values.length > 0;
    if (pagination) {
        checks['response has pagination booleans'] = () => hasBoolean(payload?.hasNext) && hasBoolean(payload?.hasPrevious);
    }
    if (each) checks[`every ${key} item matches request`] = () => Array.isArray(values) && values.every(each);
    return checks;
}
