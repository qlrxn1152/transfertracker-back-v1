export const baseUrl = (__ENV.BASE_URL || 'http://localhost:8081').replace(/\/$/, '');

export function readPositiveInteger(name, fallback = null) {
    const value = __ENV[name];
    if (value === undefined || value === '') {
        if (fallback !== null) return fallback;
        throw new Error(`${name} is required and must be a positive integer.`);
    }

    const parsed = Number.parseInt(value, 10);
    if (!Number.isInteger(parsed) || parsed <= 0) {
        throw new Error(`${name} must be a positive integer.`);
    }
    return parsed;
}

export function readNonNegativeInteger(name, fallback = 0) {
    const value = __ENV[name];
    if (value === undefined || value === '') return fallback;

    const parsed = Number.parseInt(value, 10);
    if (!Number.isInteger(parsed) || parsed < 0) {
        throw new Error(`${name} must be a non-negative integer.`);
    }
    return parsed;
}

export function readText(name, fallback = '') {
    const value = (__ENV[name] || fallback).trim();
    if (!value) throw new Error(`${name} is required.`);
    return value;
}

export function page() {
    return readNonNegativeInteger('PAGE', 0);
}

export function encodedQuery(values) {
    return Object.entries(values)
        .filter(([, value]) => value !== undefined && value !== null && value !== '')
        .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(String(value))}`)
        .join('&');
}

export function authorizationHeaders(required = false) {
    const token = (__ENV.AUTH_TOKEN || '').trim();
    if (required && !token) {
        throw new Error('AUTH_TOKEN is required for this member scenario. Do not commit tokens; pass it only through the environment.');
    }
    return token ? { Authorization: `Bearer ${token}` } : {};
}
