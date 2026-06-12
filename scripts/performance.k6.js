import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.API_BASE_URL || 'http://localhost:8080/api/v1';
const EMAIL = __ENV.CONSULOS_EMAIL || 'admin@consulos.local';
const PASSWORD = __ENV.CONSULOS_PASSWORD || 'Admin1234!';

export const options = {
  vus: 5,
  duration: '30s',
  thresholds: {
    'http_req_duration{endpoint:usagers}': ['p(95)<200'],
    'http_req_duration{endpoint:dossiers}': ['p(95)<200'],
    'http_req_duration{endpoint:login}': ['p(95)<500'],
    'http_req_duration{endpoint:dashboard}': ['p(95)<1000'],
  },
};

export function setup() {
  const loginRes = http.post(
    `${BASE_URL}/auth/login`,
    JSON.stringify({ email: EMAIL, password: PASSWORD }),
    { headers: { 'Content-Type': 'application/json' }, tags: { endpoint: 'login' } },
  );
  check(loginRes, { 'login ok': (r) => r.status === 200 });
  const body = loginRes.json();
  return { token: body.data.accessToken };
}

export default function (data) {
  const headers = {
    Authorization: `Bearer ${data.token}`,
    'Content-Type': 'application/json',
  };

  http.get(`${BASE_URL}/usagers?size=100`, { headers, tags: { endpoint: 'usagers' } });
  http.get(`${BASE_URL}/dossiers?size=100`, { headers, tags: { endpoint: 'dossiers' } });
  http.get(`${BASE_URL}/dashboard/stats`, { headers, tags: { endpoint: 'dashboard' } });
  sleep(0.5);
}
