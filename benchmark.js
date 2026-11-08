import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 20 }, // Ramp-up to 20 users
    { duration: '1m', target: 100 }, // Stress at 100 concurrent users
    { duration: '30s', target: 0 }, // Ramp-down
  ],
  thresholds: {
    http_req_duration: ['p(95)<200'], // 95% of requests under 200ms
    http_req_failed: ['rate<0.01'], // Error rate under 1%
  },
};

const token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJGcm9udGVuZCIsInJvbGUiOiJDVVNUT01FUiIsImlhdCI6MTc5NDA3NzUwOSwiZXhwIjoxNzk0MDgxMTA5fQ.ydcenBGgbTjdmcNxm-un1oWyLRC3UWXt62FkqMvzGFE";

export default function () {
  const params = {
    headers: {
      Cookie: 'token=' + token,
    },
  };
  const res = http.get('http://localhost:8080/api/products', params);
  check(res, { 'status is 200': (r) => r.status === 200 });
  sleep(1);
}

