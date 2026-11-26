import http from 'k6/http';
import { authorizationHeaders, baseUrl } from './env.js';

export function getJson(path, { requiresAuth = false } = {}) {
    const response = http.get(`${baseUrl}${path}`, {
        headers: authorizationHeaders(requiresAuth),
        tags: { name: path.split('?')[0] },
    });

    let payload = null;
    try {
        payload = response.json();
    } catch (_) {
        // Validation records malformed responses as check failures.
    }

    return { response, payload };
}
