import { readPositiveInteger } from './env.js';

export const summaryTrendStats = ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'];

export function readLoadProfile() {
    return {
        executor: 'constant-arrival-rate',
        rate: readPositiveInteger('RPS', 50),
        timeUnit: '1s',
        duration: __ENV.DURATION || '1m',
        preAllocatedVUs: readPositiveInteger('PRE_ALLOCATED_VUS', 50),
        maxVUs: readPositiveInteger('MAX_VUS', 200),
    };
}

export function createReadOptions(scenarioName) {
    const load = readLoadProfile();
    return {
        load,
        options: {
            scenarios: { [scenarioName.replaceAll('-', '_')]: load },
            thresholds: {
                http_req_duration: ['p(95)<300'],
                http_req_failed: ['rate<0.001'],
                checks: ['rate==1'],
                dropped_iterations: ['count==0'],
            },
            summaryTrendStats,
        },
    };
}
