import http from 'k6/http';
import { check } from 'k6';
import { Trend } from 'k6/metrics';

const railwayDuration = new Trend('railway_duration');
const vercelDuration = new Trend('vercel_duration');

const RAILWAY_URL =
  'https://transfertracker-back-v1-production.up.railway.app';

const VERCEL_URL = __ENV.VERCEL_URL;

export const options = {
  vus: 1,
  iterations: 10,
};

export default function () {
  // 1. Railway 직접 호출
  const railwayRes = http.get(
    `${RAILWAY_URL}/api/transfers?page=0`
  );

  railwayDuration.add(railwayRes.timings.duration);

  check(railwayRes, {
    'Railway status is 200': (r) => r.status === 200,
  });

  // 2. Vercel 경유 호출
  const vercelRes = http.get(
    `${VERCEL_URL}/api/transfers?page=0`
  );

  vercelDuration.add(vercelRes.timings.duration);

  check(vercelRes, {
    'Vercel status is 200': (r) => r.status === 200,
  });

  console.log(`Railway status: ${railwayRes.status}`);
  console.log(`Vercel status: ${vercelRes.status}`);
}