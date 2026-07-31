import http from 'k6/http';
import { check, sleep } from 'k6';

// TICKET-ADV158 — k6 load test: 200 concurrent users creating trades for 60s
export const options = {
  stages: [
    { duration: '10s', target: 200 }, // ramp up to 200 users
    { duration: '40s', target: 200 }, // stay at 200 users
    { duration: '10s', target: 0 },   // ramp down
  ],
  thresholds: {
    http_req_failed: ['rate<0.01'], // < 1% errors
    http_req_duration: ['p(95)<500'], // 95% of requests must complete below 500ms
  },
};

const BASE_URL = 'http://localhost:8080/api';
let token = null;

export function setup() {
  // Login to get a JWT token for the test
  const loginRes = http.post(`${BASE_URL}/auth/login`, JSON.stringify({
    email: 'trader1@reconx.com',
    password: 'password'
  }), {
    headers: { 'Content-Type': 'application/json' }
  });
  
  if (loginRes.status === 200) {
    return loginRes.json('token');
  }
  return null;
}

export default function (authToken) {
  if (!authToken) {
    console.error('No auth token available, skipping request');
    return;
  }

  const tradePayload = JSON.stringify({
    tradeRef: `TRD-LOAD-${__VU}-${__ITER}-${Date.now()}`,
    counterpartyId: 'CP-1001',
    instrumentId: 'INST-2001',
    type: 'EQUITY',
    side: 'BUY',
    quantity: 100,
    price: 150.25,
    currency: 'USD',
    tradeDate: new Date().toISOString().split('T')[0],
    settlementDate: new Date(Date.now() + 2 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
    status: 'PENDING'
  });

  const res = http.post(`${BASE_URL}/v1/trades`, tradePayload, {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${authToken}`
    }
  });

  check(res, {
    'is status 201': (r) => r.status === 201,
  });

  sleep(1);
}
