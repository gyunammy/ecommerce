# 부하 테스트 계획서

## 1. 테스트 대상 선정
- **API**: `GET /products` (상품 목록 조회)
- **선정 이유**: 전자상거래 서비스의 핵심 조회 API로, 많은 사용자가 동시에 접근하는 페이지이며 시스템 전반의 성능을 확인할 수 있는 대표적인 엔드포인트

## 2. 테스트 목적
- 단계적 부하 증가 시 응답 시간 및 처리량 변화 확인
- 최대 200명의 동시 사용자 환경에서 시스템 안정성 검증
- 응답 시간(95% < 1초) 목표 달성 여부 확인
- 스파이크 부하 상황에서 시스템 복원력(resilience) 확인

## 3. 테스트 시나리오

### 시나리오: 상품 목록 조회 단계적 부하 테스트
```
사전 조건:
- Spring Boot 애플리케이션이 정상 실행 중
- 상품 데이터가 DB에 존재 (최소 1개 이상)
- GET /products API가 정상 응답

테스트 흐름:
1단계 (0-20초): 부하 증가
  - VUs: 0 → 100명으로 증가
  - 목적: 시스템이 점진적 부하 증가를 안정적으로 처리하는지 확인
  - 예상 결과: 응답 시간이 완만하게 증가

2단계 (20-40초): 스파이크 부하
  - VUs: 100 → 200명으로 급증
  - 목적: 갑작스런 트래픽 증가 시 시스템 복원력 확인
  - 예상 결과: 응답 시간 증가하지만 에러율 < 1% 유지

3단계 (40-60초): 부하 감소
  - VUs: 200 → 0명으로 감소
  - 목적: 부하 감소 시 시스템 복구 속도 확인
  - 예상 결과: 응답 시간이 정상 수준으로 복귀

총 테스트 시간: 60초 (1분)
각 VU는 1-3초 간격으로 반복 요청 (실제 사용자 행동 시뮬레이션)
```

## 4. 성능 지표 및 목표

| 지표 | 목표 | 설명 |
|------|------|------|
| 응답 시간(p95) | < 1초 | 95%의 요청이 1초(1000ms) 이내 응답 |
| 응답 시간(평균) | < 500ms | 평균 응답 시간 500ms 이내 |
| HTTP 요청 실패율 | < 1% | 전체 요청 중 실패율 1% 미만 |
| 에러율 | < 1% | check 실패 포함 전체 에러 발생률 1% 미만 |
| 처리량 (RPS) | 측정 | 초당 요청 처리 수 (최대 부하 시 측정) |
| 응답 상태 코드 | 200 OK | 모든 정상 요청은 200 응답 |
| Content-Type | application/json | JSON 형식 응답 확인 |

## 5. 테스트 실행 방법

### 사전 준비
```bash
# 1. Spring Boot 애플리케이션 실행
docker-compose up -d

# 또는 IDE에서 직접 실행
# - MySQL, Kafka, Zookeeper가 실행 중이어야 함
# - Application이 http://localhost:8080 에서 실행 중
```

### 테스트 실행

**방법 1: Docker Compose 사용 (권장)**
```bash
# K6 컨테이너로 부하테스트 실행
docker-compose --profile k6 up

# 동작:
# - k6 컨테이너가 자동으로 부하테스트 스크립트 실행
# - 결과를 InfluxDB에 자동 저장
# - Grafana 대시보드에서 실시간 모니터링 가능
# - 완료 후 자동 종료
```

**방법 2: 로컬 K6 사용 (K6를 로컬에 설치한 경우)**
```bash
# K6 실행
k6 run performance/k6-scripts/k6-product-load-test.js

# InfluxDB 출력 포함 (Grafana 연동)
k6 run --out influxdb=http://localhost:8086/k6 performance/k6-scripts/k6-product-load-test.js
```

**방법 3: 커스텀 환경 변수 사용**
```bash
# 다른 서버 테스트
BASE_URL=http://production-server:8080 k6 run performance/k6-scripts/k6-product-load-test.js
```

### 결과 확인
```bash
# 1. K6 콘솔 출력에서 메트릭 확인
#    - http_req_duration(p95): < 1000ms
#    - http_req_duration(avg): < 500ms
#    - http_req_failed: < 1%
#    - http_reqs: 총 요청 수
#    - successful_requests: 성공한 요청 수

# 2. Grafana 대시보드 확인
#    - 브라우저: http://localhost:3000
#    - 실시간 그래프로 성능 추이 확인
#    - VUs, Response Time, Request Rate 등 확인

# 3. 애플리케이션 로그 확인
#    - docker logs sparta-app
#    - 에러 로그 유무 확인
```

## 6. 예상 결과 및 검증 기준

### 성공 기준
- ✅ 응답 시간 p95 < 1초 (1000ms)
- ✅ 응답 시간 평균 < 500ms
- ✅ HTTP 요청 실패율 < 1%
- ✅ 에러율 < 1%
- ✅ 모든 정상 요청이 200 OK 응답
- ✅ Content-Type이 application/json
- ✅ 응답 body가 배열 형태 (상품 목록)
- ✅ 스파이크 부하(200 VUs) 시에도 시스템 안정성 유지

### 실패 시 점검 사항
- 애플리케이션 로그에서 예외/에러 확인
- DB 커넥션 풀 설정 확인 (HikariCP)
- 쿼리 성능 확인 (슬로우 쿼리 로그)
- JVM 메모리 사용량 확인 (힙 메모리, GC)
- 네트워크 지연 확인

## 7. 부하테스트 재실행 방법

상품 목록 조회는 읽기 전용 API이므로 데이터 초기화 없이 반복 실행 가능:

### 방법 1: Docker Compose 사용
```bash
# 부하테스트 재실행
docker-compose --profile k6 up
```

### 방법 2: 로컬 K6 사용
```bash
# 직접 실행
k6 run performance/k6-scripts/k6-product-load-test.js

# Grafana 연동
k6 run --out influxdb=http://localhost:8086/k6 performance/k6-scripts/k6-product-load-test.js
```

## 8. Grafana 대시보드 활용

### 실시간 모니터링
```bash
# 1. Grafana 접속
http://localhost:3000

# 2. k6 Load Testing Results 대시보드 확인
# - VUs (Virtual Users): 동시 접속자 수
# - Request Rate: 초당 요청 수
# - Response Time: p50, p95, p99 응답 시간
# - HTTP Failures: 에러율
```

### 성능 분석 포인트
- **부하 증가 구간 (0-20초)**: 응답 시간 증가 추이 확인
- **스파이크 구간 (20-40초)**: 최대 부하 시 시스템 안정성 확인
- **부하 감소 구간 (40-60초)**: 복구 속도 확인
- **처리량 (RPS)**: 최대 처리 가능한 초당 요청 수 파악

## 9. 개선 방향

테스트 결과 기반으로 다음 항목 검토:
- DB 인덱스 최적화 (상품 조회 쿼리)
- 캐싱 전략 도입 (Redis)
- 커넥션 풀 크기 조정
- 쿼리 최적화 (N+1 문제 등)
- 서버 리소스 증설 검토
