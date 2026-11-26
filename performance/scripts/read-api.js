import { getReadCase, supportedReadCaseIds } from './common/read-cases.js';

const caseId = __ENV.API_CASE || 'posts-list';
const definition = getReadCase(caseId);
if (!definition) {
    throw new Error(`Unsupported API_CASE: ${caseId}. Supported: ${supportedReadCaseIds().join(', ')}`);
}

export const options = definition.options;
export default function () {
    definition.execute();
}
export function handleSummary(data) {
    return definition.handleSummary(data);
}
