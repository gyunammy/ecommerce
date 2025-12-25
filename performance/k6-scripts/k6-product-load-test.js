import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Counter, Trend } from 'k6/metrics';

// 사용자 정의 메트릭
const errorRate = new Rate('errors');
const successCount = new Counter('successful_requests');
const productResponseTime = new Trend('product_response_time');

// 부하 테스트 옵션 설정 - 단계적 부하 증가
export const options = {
  scenarios: {
    product_load_test: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '20s', target: 100 },  // 20초 동안 100명까지 증가
        { duration: '20s', target: 200 },  // 20초 동안 200명까지 스파이크
        { duration: '20s', target: 0 },    // 20초 동안 0명으로 감소
      ],
    },
  },
  thresholds: {
    'http_req_duration': ['p(95)<1000'],  // 95%의 요청이 1초 이내
    'http_req_failed': ['rate<0.01'],     // 실패율 1% 미만
    'errors': ['rate<0.01'],              // 에러율 1% 미만
  },
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
};

// 환경 설정
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
  // VU 정보 활용
  const vuId = __VU;  // Virtual User ID
  const iteration = __ITER;  // Iteration 번호

  // 상품 목록 조회 (VU 데이터 전달)
  fetchProducts(vuId, iteration);

  // 1-3초 랜덤 대기 (실제 사용자 행동 시뮬레이션)
  sleep(1 + Math.random() * 2);
}

// 상품 목록 조회 함수
function fetchProducts(vuId, iteration) {
  const response = http.get(`${BASE_URL}/products`, {
    headers: {
      'X-VU-ID': `${vuId}`,  // VU 번호를 헤더로 전달
      'X-Iteration': `${iteration}`,  // Iteration 번호를 헤더로 전달
      'X-User-ID': `user-${vuId}`,  // 가상 사용자 ID
    },
    tags: {
      name: 'fetch_products',
      vu_id: `${vuId}`,  // 메트릭에 VU ID 태그 추가 (문자열로 변환)
      iteration: `${iteration}`,  // Iteration도 태그로 추가
    },
  });

  // 응답 시간 기록
  productResponseTime.add(response.timings.duration);

  // 요청 결과 확인
  const success = check(response, {
    '상태 코드 200': (r) => r.status === 200,
    '응답 시간 < 1000ms': (r) => r.timings.duration < 1000,
    'Content-Type이 JSON': (r) => r.headers['Content-Type']?.includes('application/json'),
    '상품 목록이 배열': (r) => {
      try {
        const body = JSON.parse(r.body);
        return Array.isArray(body);
      } catch (e) {
        return false;
      }
    },
  });

  if (success) {
    successCount.add(1);
  } else {
    errorRate.add(1);
    console.log(`요청 실패: ${response.status} - ${response.body?.substring(0, 100)}`);
  }
}

// 테스트 시작 전 실행
export function setup() {
  console.log('========================================');
  console.log('=== 상품 목록 조회 성능 부하 테스트 ===');
  console.log(`테스트 대상: ${BASE_URL}/products`);
  console.log('');
  console.log('테스트 시나리오:');
  console.log('  - 단계 1: 20초 동안 0→100명 증가');
  console.log('  - 단계 2: 20초 동안 100→200명 스파이크');
  console.log('  - 단계 3: 20초 동안 200→0명 감소');
  console.log('');
  console.log('총 테스트 시간: 1분');
  console.log('');
  console.log('측정 지표:');
  console.log('  - http_req_duration (응답 시간)');
  console.log('  - http_req_failed (실패율)');
  console.log('  - http_reqs (초당 요청 수)');
  console.log('  - successful_requests (성공한 요청 수)');
  console.log('========================================');
}

// 테스트 종료 후 실행
export function teardown(data) {
  console.log('========================================');
  console.log('=== 테스트 완료 ===');
  console.log('');
  console.log('결과 확인:');
  console.log('  1. Grafana 대시보드: http://localhost:3000');
  console.log('  2. k6 Summary에서 성능 지표 확인');
  console.log('');
  console.log('분석 포인트:');
  console.log('  - 부하 증가 시 응답 시간 변화');
  console.log('  - 200명 스파이크 시 시스템 안정성');
  console.log('  - p95, p99 응답 시간 확인');
  console.log('  - 초당 처리량 (RPS) 확인');
  console.log('========================================');
}
