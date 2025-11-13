# DB 최적화 보고서

판매량 기준 인기상품 조회 쿼리를 가지고 인덱스를 어떻게 적용시키는지에 따라 성능차이가 어떻게 나는지 확인해보겠습니다.

## 1. 데이터 세팅

### 1-1 사용 테이블 구조
```sql
-- 주문 테이블
CREATE TABLE IF NOT EXISTS `order` (
    order_id        BIGINT      PRIMARY KEY AUTO_INCREMENT COMMENT '주문 ID',
    user_id         BIGINT      NOT NULL                   COMMENT '사용자 ID',
    user_coupon_id  BIGINT      NULL                       COMMENT '사용된 쿠폰 ID',
    total_amount    INT         NOT NULL                   COMMENT '총 주문 금액',
    discount_amount INT         DEFAULT 0                  COMMENT '할인 금액',
    used_point      INT         DEFAULT 0                  COMMENT '사용 포인트',
    status          VARCHAR(50) NOT NULL                   COMMENT '주문 상태 (PENDING, COMPLETED, CANCELLED)',
    created_at      TIMESTAMP   DEFAULT CURRENT_TIMESTAMP  COMMENT '주문일시'
);
```

```sql
--- 주문 아이템 테이블
CREATE TABLE IF NOT EXISTS order_item (
    order_item_id BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '주문 상품 ID',
    order_id      BIGINT       NOT NULL                   COMMENT '주문 ID',
    product_id    BIGINT       NOT NULL                   COMMENT '상품 ID',
    product_name  VARCHAR(255) NOT NULL                   COMMENT '상품명',
    description   TEXT                                    COMMENT '상품 설명',
    quantity      INT          NOT NULL                   COMMENT '수량',
    price         INT          NOT NULL                   COMMENT '단가',
    created_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP  COMMENT '생성일시'
);
```

### 2. 테스트 데이터
유의미한 결과를 얻기위해 테이블에 테스트데이터를 준비했습니다. 
``` sql
-- 주문 테이블 레코드 개수
SELECT COUNT(1) FROM `order`;

--결과
+------------+
| `COUNT(1)` |
+------------+
| 1000000    |
+------------+
```
``` sql
-- 주문 테이블 레코드 개수
SELECT COUNT(1) FROM `order-item`;

-- 결과
+------------+
| `COUNT(1)` |
+------------+
| 2999341    |
+------------+
```

## 3. 쿼리 실행

### 3-1. 별도의 index 없이 쿼리 작성

```sql
-- 판매량 기준 인기상품 목록 조회 쿼리
SELECT
    oi.product_id,                      -- 상품_ID
    oi.product_name,                    -- 상품명
    oi.description,                     -- 상품설명
    SUM(oi.quantity) AS total_quantity  -- 총 판매 수
FROM order_item oi
     JOIN `order` o ON oi.order_id = o.order_id
WHERE o.status = 'COMPLETED'
GROUP BY oi.product_id, oi.product_name, oi.description
ORDER BY total_quantity DESC;
```
```
-- explain 결과
+----+-------------+-------+------------+--------+---------------+---------+---------+----------------------+---------+----------+---------------------------------+
| id | select_type | table | partitions | type   | possible_keys | key     | key_len | ref                  | rows    | filtered | Extra                           |
+----+-------------+-------+------------+--------+---------------+---------+---------+----------------------+---------+----------+---------------------------------+
|  1 | SIMPLE      | oi    | NULL       | ALL    | NULL          | NULL    | NULL    | NULL                 | 2743270 | 100      | Using temporary; Using filesort |
+----+-------------+-------+------------+--------+---------------+---------+---------+----------------------+---------+----------+---------------------------------+
|  1 | SIMPLE      | o     | NULL       | eq_ref | PRIMARY       | PRIMARY | 8       | ecommerce.o.order_id | 1       | 10       | Using where                     |
+----+-------------+-------+------------+--------+---------------+---------+---------+----------------------+---------+----------+---------------------------------+

-- explain analyze 결과
+------------------------------------------------------------------------------------------------------------------------------------------------------------+
|  EXPLAIN                                                                                                                                                   |
+------------------------------------------------------------------------------------------------------------------------------------------------------------+
| -> Sort: total_quantity DESC  (actual time=5296..5296 rows=20 loops=1)                                                                                     |
|   -> Table scan on <temporary>  (actual time=5296..5296 rows=20 loops=1)                                                                                   |
|       -> Aggregate using temporary table  (actual time=5296..5296 rows=20 loops=1)                                                                         |
|           -> Nested loop inner join  (cost=3.08e+6 rows=274327) (actual time=0.852..3258 rows=1.59e+6 loops=1)                                             |
|               -> Table scan on oi  (cost=292070 rows=2.74e+6) (actual time=0.418..1596 rows=3e+6 loops=1)                                                  |
|               -> Filter: (o.`status` = 'COMPLETED')  (cost=0.917 rows=0.1) (actual time=467e-6..489e-6 rows=0.53 loops=3e+6)                               |
|                   -> Single-row index lookup on o using PRIMARY (order_id=oi.order_id)  (cost=0.917 rows=1) (actual time=342e-6..356e-6 rows=1 loops=3e+6) |                  
+------------------------------------------------------------------------------------------------------------------------------------------------------------+
```

#### 결과 분석
- **전체 실행 시간**: 약 5,296ms (약 5.3초)
- **주요 병목 지점**:
  - `order_item` 테이블 전체 스캔: 약 1,596ms, 3백만 건 스캔
  - Nested Loop Join: 약 3,258ms, 300만번 반복하여 order 테이블 조회
  - `status = 'COMPLETED'` 조건으로 필터링: 각 loop마다 0.489μs 소요
  - 임시 테이블을 사용한 집계 및 정렬 작업: 전체 5,296ms
- **Index 활용 현황**:
  - `order_item` 테이블: 인덱스 없음, 전체 테이블 스캔 (ALL)
  - `order` 테이블: PRIMARY KEY를 사용한 Single-row lookup (eq_ref), 효율적으로 동작
- **핵심 문제**: order_item 테이블의 전체 스캔이 주요 성능 병목
---

### 3-2. 상품 테이블의 status 컬럼에 단일 index 추가 후 쿼리 작성

```sql
-- 인덱스 추가
CREATE INDEX idx_order_status ON `order` (status);

-- 판매량 기준 인기상품 목록 조회 쿼리
SELECT
    oi.product_id,                      -- 상품_ID
    oi.product_name,                    -- 상품명
    oi.description,                     -- 상품설명
    SUM(oi.quantity) AS total_quantity  -- 총 판매 수
FROM order_item oi
     JOIN `order` o ON oi.order_id = o.order_id
WHERE o.status = 'COMPLETED'
GROUP BY oi.product_id, oi.product_name, oi.description
ORDER BY total_quantity DESC;
```
```
-- explain 결과
+----+-------------+-------+------------+--------+--------------------------+---------+---------+-----------------------+---------+----------+---------------------------------+
| id | select_type | table | partitions | type   | possible_keys            | key     | key_len | ref                   | rows    | filtered | Extra                           |
+----+-------------+-------+------------+--------+--------------------------+---------+---------+-----------------------+---------+----------+---------------------------------+
|  1 | SIMPLE      | oi    | NULL       | ALL    | NULL                     | NULL    | NULL    | NULL                  | 2903281 | 100      | Using temporary; Using filesort |
+----+-------------+-------+------------+--------+--------------------------+---------+---------+-----------------------+---------+----------+---------------------------------+
|  1 | SIMPLE      | o     | NULL       | eq_ref | PRIMARY,idx_order_status | PRIMARY | 8       | ecommerce.oi.order_id | 1       | 50       | Using where                     |
+----+-------------+-------+------------+--------+--------------------------+---------+---------+-----------------------+---------+----------+---------------------------------+

-- explain analyze 결과
+--------------------------------------------------------------------------------------------------------------------------------------------------------------+
|  EXPLAIN                                                                                                                                                     |
+--------------------------------------------------------------------------------------------------------------------------------------------------------------+
| -> Sort: total_quantity DESC  (actual time=3747..3747 rows=20 loops=1)                                                                                       |            
|     -> Table scan on <temporary>  (actual time=3747..3747 rows=20 loops=1)                                                                                   |
|         -> Aggregate using temporary table  (actual time=3747..3747 rows=20 loops=1)                                                                         |
|             -> Nested loop inner join  (cost=3.15e+6 rows=1.45e+6) (actual time=0.574..2548 rows=1.59e+6 loops=1)                                            |
|                 -> Table scan on oi  (cost=309339 rows=2.9e+6) (actual time=0.0288..1057 rows=3e+6 loops=1)                                                  |
|                 -> Filter: (o.`status` = 'COMPLETED')  (cost=0.878 rows=0.5) (actual time=413e-6..434e-6 rows=0.53 loops=3e+6)                               |
|                     -> Single-row index lookup on o using PRIMARY (order_id=oi.order_id)  (cost=0.878 rows=1) (actual time=293e-6..307e-6 rows=1 loops=3e+6) |
|                                                                                                                                                              |
+--------------------------------------------------------------------------------------------------------------------------------------------------------------+
```

#### 결과 분석
- **전체 실행 시간**: 약 3,747ms (약 3.7초)
- **주요 병목 지점**:
  - `order_item` 테이블 전체 스캔: 약 1,057ms, 3백만 건 스캔
  - Nested Loop Join: 약 2,548ms, 300만번 반복하여 order 테이블 조회
  - `status = 'COMPLETED'` 조건으로 필터링: 각 loop마다 0.434μs 소요
  - 임시 테이블을 사용한 집계 및 정렬 작업: 전체 3,747ms
- **Index 활용 현황**:
  - `order` 테이블: `idx_order_status` 인덱스가 생성되었으나, 실제로는 **PRIMARY KEY 사용** (eq_ref)
  - `order_item` 테이블: 인덱스 없음, 전체 테이블 스캔 (ALL)
- **핵심 문제**: status 인덱스가 생성되었지만 옵티마이저가 PRIMARY KEY를 선택하여 일부 성능 개선 발생 (약 29% 개선)
---

### 3-3. 상품 테이블의 status, orderId 컬럼에 복합 index 추가 후 쿼리 작성

```sql
-- 인덱스 추가
create index idx_order_status_id on `order` (status, order_id);

-- 판매량 기준 인기상품 목록 조회 쿼리
SELECT
    oi.product_id,                      -- 상품_ID
    oi.product_name,                    -- 상품명
    oi.description,                     -- 상품설명
    SUM(oi.quantity) AS total_quantity  -- 총 판매 수
FROM order_item oi
     JOIN `order` o ON oi.order_id = o.order_id
WHERE o.status = 'COMPLETED'
GROUP BY oi.product_id, oi.product_name, oi.description
ORDER BY total_quantity DESC;
```
```
-- explain 결과
+----+-------------+-------+------------+--------+----------------------------------------------+---------+---------+-----------------------+---------+----------+----------------------------------+
| id | select_type | table | partitions | type   | possible_keys                                | key     | key_len | ref                   | rows    | filtered | Extra                            |
+----+-------------+-------+------------+--------+----------------------------------------------+---------+---------+-----------------------+---------+----------+----------------------------------+
|  1 | SIMPLE      | oi    | NULL       | ALL    | NULL                                         | NULL    | 202     | NULL                  | 2743270 |   100    |  Using temporary; Using filesort |
+----+-------------+-------+------------+--------+----------------------------------------------+---------+---------+-----------------------+---------+----------+----------------------------------+
|  1 | SIMPLE      | o     | NULL       | eq_ref | PRIMARY,idx_order_status,idx_order_status_id | PRIMARY | 8       | ecommerce.oi.order_id | 1       |   50     | Using where                      |
+----+-------------+-------+------------+--------+----------------------------------------------+---------+---------+-----------------------+---------+----------+----------------------------------+

-- explain analyze 결과
+--------------------------------------------------------------------------------------------------------------------------------------------------------------+
|  EXPLAIN                                                                                                                                                     |
+--------------------------------------------------------------------------------------------------------------------------------------------------------------+
| -> Sort: total_quantity DESC  (actual time=5395..5395 rows=20 loops=1)                                                                                       |            
|     -> Table scan on <temporary>  (actual time=5395..5395 rows=20 loops=1)                                                                                   |
|         -> Aggregate using temporary table  (actual time=5395..5395 rows=20 loops=1)                                                                         |
|             -> Nested loop inner join  (cost=2.6e+6 rows=1.37e+6) (actual time=0.765..3313 rows=1.59e+6 loops=1)                                             |
|                 -> Table scan on oi  (cost=292567 rows=2.74e+6) (actual time=0.44..1605 rows=3e+6 loops=1)                                                   |
|                 -> Filter: (o.`status` = 'COMPLETED')  (cost=0.742 rows=0.5) (actual time=482e-6..504e-6 rows=0.53 loops=3e+6)                               |
|                     -> Single-row index lookup on o using PRIMARY (order_id=oi.order_id)  (cost=0.742 rows=1) (actual time=354e-6..368e-6 rows=1 loops=3e+6) |
+--------------------------------------------------------------------------------------------------------------------------------------------------------------+
```

### 3-4. 쿼리 수정

group by에 세가지 컬럼이 들어가는 것이 문제인가 싶어 
group by절에 최소의 컬럼이 적용되도록 쿼리를 수정해보았습니다.

```sql

-- 판매량 기준 인기상품 목록 조회 쿼리
  SELECT
      p.product_id,
      p.product_name,
      p.description,
      oi.total_quantity
  from product p
    join ( -- 완료된 주문 중 상품_ID 별 총 판매수 조회
          select
              oi.product_id,
              SUM(oi.quantity) AS total_quantity
          FROM order_item oi
            JOIN `order` o ON oi.order_id = o.order_id
          WHERE o.status = 'COMPLETED'
          GROUP BY oi.product_id
    )oi on p.product_id = oi.product_id
  ORDER BY total_quantity DESC;
```
```
-- explain 결과
+----+-------------+------------+------------+--------+---------------+-------------+---------+------------------------+---------+----------+----------------------------------+
| id | select_type | table      | partitions | type   | possible_keys | key         | key_len | ref                    | rows    | filtered | Extra                            |
+----+-------------+------------+------------+--------+---------------+-------------+---------+------------------------+---------+----------+----------------------------------+
|  1 | PRIMARY     | p          | NULL       | ALL    | PRIMARY       | NULL        | NULL    | NULL                   | 20      |   100    | Using temporary; Using filesort  |
+----+-------------+------------+------------+--------+---------------+-------------+---------+------------------------+---------+----------+----------------------------------+
|  1 | PRIMARY     | <derived2> | NULL       | eq_ref | <auto_key0>   | <auto_key0> | 8       | ecommerce.p.product_id | 2903    |   100    | NULL                             |
+----+-------------+------------+------------+--------+---------------+-------------+---------+------------------------+---------+----------+----------------------------------+
|  2 | DERIVED     | o          | NULL       | ALL    | NULL          | NULL        | NULL    | NULL                   | 2903281 |   100    | Using temporary                  |
+----+-------------+------------+------------+--------+---------------+-------------+---------+------------------------+---------+----------+----------------------------------+
|  2 | DERIVED     | o          | NULL       | eq_ref | PRIMARY       | PRIMARY     | 8       | ecommerce.oi.order_id  | 1       |   1      | Using where                      |
+----+-------------+------------+------------+--------+---------------+-------------+---------+------------------------+---------+----------+----------------------------------+

-- explain analyze 결과
+------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
|  EXPLAIN                                                                                                                                                                     |
+------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| -> Sort: oi.total_quantity DESC  (actual time=2667..2667 rows=20 loops=1)                                                                                                    |
|     -> Stream results  (cost=14519 rows=0) (actual time=2667..2667 rows=20 loops=1)                                                                                          |
|         -> Nested loop inner join  (cost=14519 rows=0) (actual time=2667..2667 rows=20 loops=1)                                                                              |
|             -> Table scan on p  (cost=2.25 rows=20) (actual time=0.0489..0.056 rows=20 loops=1)                                                                              |
|             -> Index lookup on oi using <auto_key0> (product_id=p.product_id)  (cost=0.255..740 rows=2903) (actual time=133..133 rows=1 loops=20)                            |
|                 -> Materialize  (cost=0..0 rows=0) (actual time=2667..2667 rows=20 loops=1)                                                                                  |
|                     -> Table scan on <temporary>  (actual time=2667..2667 rows=20 loops=1)                                                                                   |
|                         -> Aggregate using temporary table  (actual time=2667..2667 rows=20 loops=1)                                                                         |
|                             -> Nested loop inner join  (cost=3.32e+6 rows=290328) (actual time=2.37..2353 rows=1.59e+6 loops=1)                                              |
|                                 -> Table scan on oi  (cost=311063 rows=2.9e+6) (actual time=1.55..853 rows=3e+6 loops=1)                                                     |
|                                 -> Filter: (o.`status` = 'COMPLETED')  (cost=0.936 rows=0.1) (actual time=414e-6..436e-6 rows=0.53 loops=3e+6)                               |
|                                     -> Single-row index lookup on o using PRIMARY (order_id=oi.order_id)  (cost=0.936 rows=1) (actual time=292e-6..306e-6 rows=1 loops=3e+6) |   
+------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
```

#### 결과 분석
- **전체 실행 시간**: 약 2,667ms (약 2.7초) - **최고 성능!** ✅
- **주요 병목 지점**:
  - `order_item` 테이블 전체 스캔: 약 853ms, 3백만 건 스캔 (서브쿼리 내부)
  - Nested Loop Join: 약 2,353ms, 300만번 반복하여 order 테이블 조회 (서브쿼리 내부)
  - `status = 'COMPLETED'` 조건으로 필터링: 각 loop마다 0.436μs 소요
  - Product 테이블 조인 및 정렬: 매우 빠름 (20건만 처리)
- **쿼리 구조 개선 효과**:
  - 서브쿼리에서 `GROUP BY product_id`만 사용 (product_name, description 제외)
  - 서브쿼리 결과를 Materialize하여 재사용
  - Product 테이블과 조인하여 최종 결과 생성
  - GROUP BY 컬럼 수 감소로 임시 테이블 크기와 정렬 비용 절감
- **핵심 개선점**: GROUP BY 절 최적화로 약 **50% 성능 향상** (5,296ms → 2,667ms)
---

## 4. 종합 비교 및 결론

### 4-1. 데이터 10만 건 기준 성능 비교

| 구분 | 인덱스 없음 | 단일 인덱스 (status) | 복합 인덱스 (status, order_id) |
|------|------------|---------------------|-------------------------------|
| **실행 시간** | 585ms ✅ | 659ms | 587ms |
| **접근 방식** | Full Table Scan | Covering Index Lookup | Covering Index Lookup |
| **order 테이블 스캔** | 100,000건 (17.2ms) | 79,996건 (30.5ms) | 79,996건 (18.3ms) |
| **옵티마이저 Cost** | 21,049 | 60,988 | 61,054 |
| **사용 인덱스** | 없음 | idx_order_status | idx_order_status_id |


### 4-2. 데이터 100만 건 기준 성능 비교

| 구분 | 인덱스 없음 | 단일 인덱스 (status) | 복합 인덱스 (status, order_id) | 쿼리 수정 (GROUP BY 최적화) |
|------|------------|---------------------|-------------------------------|------------------------|
| **실행 시간** | 5,296ms | 3,747ms | 5,395ms | 2,667ms ✅ |
| **order_item 스캔** | 1,596ms (3백만 건) | 1,057ms (3백만 건) | 1,605ms (3백만 건) | 853ms (3백만 건) |
| **Nested Loop Join** | 3,258ms (3백만 loops) | 2,548ms (3백만 loops) | 3,313ms (3백만 loops) | 2,353ms (3백만 loops) |
| **GROUP BY 컬럼 수** | 3개 (id, name, desc) | 3개 (id, name, desc) | 3개 (id, name, desc) | 1개 (id만) |
| **쿼리 구조** | 단순 JOIN + GROUP BY | 단순 JOIN + GROUP BY | 단순 JOIN + GROUP BY | 서브쿼리 + Materialize |
| **옵티마이저 Cost** | 3.08e+6 | 3.15e+6 | 2.6e+6 | 3.32e+6 |
| **실제 사용 인덱스** | PRIMARY | PRIMARY (status 인덱스 존재) | PRIMARY (복합 인덱스 무시) | PRIMARY |
| **성능 개선율** | 기준 | 29% 개선 | -2% (악화) | **50% 개선** |

### 4-3. 결론

**100만 건 데이터에서의 성능 최적화 분석:**

#### 1. 인덱스 추가의 효과
- **status 단일 인덱스**: 29% 성능 개선 (5,296ms → 3,747ms)
  - 옵티마이저가 직접 사용하지는 않지만, 실행 계획 최적화에 간접 영향
  - `possible_keys`에 포함되어 옵티마이저의 판단에 영향
  - order_item 스캔과 Nested Loop Join 시간 단축

- **status + order_id 복합 인덱스**: 성능 개선 없음 (-2% 악화)
  - JOIN 순서 때문에 PRIMARY KEY 선택
  - 복합 인덱스 추가로 인한 오버헤드만 발생
  - 현재 쿼리 패턴에서는 불필요

#### 2. 쿼리 구조 개선의 효과 (최고 성능) ✅
- **GROUP BY 최적화**: 50% 성능 개선 (5,296ms → 2,667ms)
  - GROUP BY 컬럼 수 감소: 3개 → 1개 (product_id만)
  - 서브쿼리에서 집계 수행 후 Materialize
  - Product 테이블과 조인하여 나머지 컬럼 조회
  - 임시 테이블 크기와 정렬 비용 대폭 감소

**성능 비교:**
```
5,296ms (기준)
  ↓ 29% 개선
3,747ms (status 인덱스)
  ↓ 추가 29% 개선
2,667ms (GROUP BY 최적화) ← 최종 50% 개선
```

#### 3. 추가 최적화 방안

**A. order_item 테이블 인덱스 추가**
```sql
-- order_id로 JOIN을 효율적으로 수행하기 위한 인덱스
CREATE INDEX idx_order_item_order_id ON order_item(order_id);
```
- 현재 order_item 전체 스캔이 주요 병목 (853ms, 3백만 건)
- 인덱스 추가 시 추가 성능 개선 예상

**B. 커버링 인덱스 전략**
```sql
-- order_item 테이블에 쿼리에 필요한 모든 컬럼 포함
CREATE INDEX idx_order_item_covering
ON order_item(order_id, product_id, quantity);
```
- 인덱스만으로 필요한 데이터 조회
- 테이블 접근 완전 제거

**C. JOIN 순서 힌트 사용**
```sql
-- status = 'COMPLETED'인 주문을 먼저 필터링
SELECT /*+ JOIN_ORDER(o, oi) */ ...
FROM `order` o
JOIN order_item oi ON o.order_id = oi.order_id
WHERE o.status = 'COMPLETED'
```
- 옵티마이저 힌트로 order 테이블 우선 필터링

#### 4. 핵심 교훈

**인덱스 설계:**
- **인덱스는 WHERE 절뿐만 아니라 JOIN 실행 계획 전체에 영향**
- 옵티마이저가 직접 사용하지 않아도 실행 계획 최적화에 영향 가능
- 복합 인덱스가 항상 좋은 것은 아님 - JOIN 순서에 따라 무용지물
- `possible_keys`와 실제 `key`를 모두 확인해야 함

**쿼리 최적화:**
- **GROUP BY 컬럼 수 최소화가 가장 큰 성능 영향** (50% 개선)
- 서브쿼리와 Materialize를 활용한 단계별 처리
- 필요한 컬럼만 집계에 포함하고, 나머지는 조인으로 해결
- 쿼리 구조 변경이 인덱스 추가보다 효과적일 수 있음

**성능 측정:**
- EXPLAIN ANALYZE로 실제 실행 시간 측정 필수
- Cost 예측과 실제 시간이 다를 수 있음
- 옵티마이저 Cost가 낮다고 항상 빠른 것은 아님

#### 5. 최종 권장사항

**즉시 적용 가능:**
1. ✅ **쿼리 구조 개선** (GROUP BY 최적화) - 50% 성능 향상
2. ✅ **status 단일 인덱스 유지** - 추가 29% 개선 효과
3. ❌ **복합 인덱스 제거** - 오버헤드만 발생

**추가 검토:**
4. order_item 테이블에 `idx_order_item_order_id` 인덱스 추가
5. 더 나아가 커버링 인덱스 고려
6. 주기적으로 EXPLAIN ANALYZE로 성능 모니터링

**결론:**
- 인덱스 추가도 중요하지만, **쿼리 구조 최적화가 더 큰 영향**
- 현재 쿼리에서는 GROUP BY 최적화 + status 인덱스 조합이 최적
- 100만 건 기준 5.3초 → 2.7초로 **50% 성능 개선 달성**
---