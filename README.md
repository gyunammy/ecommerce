# 동시성 문제 해결방안 보고서

---

## 1. 문제 상황

### 1-1. 선착순 쿠폰 발급
- 동시에 여러 사용자가 쿠폰 발급 요청 시 정확히 재고만큼만 발급
- 동일 사용자가 동일 쿠폰을 중복으로 발급받을 수 없음
- 쿠폰 재고가 초과 발급되거나 누락되는 일이 없어야 함

### 1-2. 주문 생성
- 동시에 여러 사용자가 같은 상품을 주문할 때 재고가 초과 판매되지 않아야 함
- 상품 재고 차감 시 Lost Update가 발생하지 않아야 함
- 주문 과정에서 쿠폰 사용, 포인트 차감이 원자적으로 처리되어야 함
- 주문 실패 시 모든 변경사항이 롤백되어야 함

---

## 2. 선착순 쿠폰 발급 동시성 제어

### 2-1. 적용된 해결 방안

**1. Pessimistic Lock (비관적 락)**
- 쿠폰 조회 시 `SELECT ... FOR UPDATE` 사용
- 쿠폰 재고 확인 및 변경 시 동시 접근 차단

**2. DB Unique 제약 조건**
- `UserCoupon` 테이블에 `(userId, couponId)` 복합 Unique 인덱스
- 동일 사용자의 중복 발급을 데이터베이스 레벨에서 차단

**3. 트랜잭션 관리**
- `@Transactional`로 원자성 보장
- 예외 발생 시 자동 롤백

**4. 예외 처리**
- `DataIntegrityViolationException`: Unique 제약 조건 위반 시

### 2-2. 발급 흐름

```
1. 사용자 존재 확인
2. 쿠폰 조회 (Pessimistic Lock 획득) ← 동시성 제어 1
3. 쿠폰 재고 및 만료일 검증
4. 중복 발급 검증 (애플리케이션 레벨)
5. UserCoupon 생성 (Unique 제약 조건) ← 동시성 제어 2
6. 쿠폰 발급 수량 증가
7. 트랜잭션 커밋 (Lock 해제)
```

### 2-3. 핵심 코드

**Pessimistic Lock 적용**
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select c from Coupon c where c.couponId = :id")
Optional<Coupon> findByIdWithPessimisticLock(Long id);
```

**Unique 제약 조건**
```java
@Table(uniqueConstraints = {
    @UniqueConstraint(columnNames = {"userId", "couponId"})
})
public class UserCoupon { ... }
```
---

## 3. 주문 생성 동시성 제어

### 3-1. 적용된 해결 방안

**1. Pessimistic Lock (비관적 락) - 상품 재고**
- 상품 조회 시 `SELECT ... FOR UPDATE` 사용
- **ORDER BY로 락 획득 순서 보장하여 데드락 방지**
- 재고 차감 시 동시 접근 차단

**2. Optimistic Lock (낙관적 락) - 사용자 포인트 & 쿠폰**
- `User` 엔티티에 `@Version` 필드 추가
- `UserCoupon` 엔티티에 `@Version` 필드 추가
- 포인트 차감 및 쿠폰 사용 시 충돌 감지

**3. 트랜잭션 관리**
- `@Transactional`로 원자성 보장
- 주문 실패 시 재고, 포인트, 쿠폰 자동 롤백

**4. 간접적 동시성 보호**
- 상품 락이 트랜잭션 전체를 순차 처리하게 만듦
- 같은 상품 포함 시 User/UserCoupon 충돌 방지

### 3-2. 주문 생성 흐름

```
1. 사용자 조회
2. 장바구니 조회
3. 상품 조회 (Pessimistic Lock + ORDER BY) ← 동시성 제어 1
4. 재고 검증
5. 주문 금액 계산
6. 쿠폰 검증 및 할인 계산
7. 포인트 검증
8. 쿠폰 사용 처리 (Optimistic Lock) ← 동시성 제어 2
9. 포인트 차감 (Optimistic Lock) ← 동시성 제어 3
10. 재고 차감
11. 주문 생성
12. 장바구니 비우기
13. 트랜잭션 커밋 (Lock 해제)
```

### 3-3. 핵심 코드

**상품 Pessimistic Lock + ORDER BY (데드락 방지)**
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT p FROM Product p WHERE p.productId IN :productIds ORDER BY p.productId ASC")
List<Product> findAllByIdWithLock(@Param("productIds") Iterable<Long> productIds);
```

**사용자 Optimistic Lock**
```java
@Entity
public class User {
    @Version
    private Long version = 0L;

    public void deductPoint(int amount) {
        validateSufficientPoint(amount);
        this.point -= amount;
        // version이 자동으로 증가하며 충돌 감지
    }
}
```

**쿠폰 Optimistic Lock**
```java
@Entity
public class UserCoupon {
    @Version
    private Long version = 0L;

    public void markAsUsed() {
        this.used = true;
        this.usedAt = LocalDateTime.now();
        // version이 자동으로 증가하며 충돌 감지
    }
}
```

### 3-4. 데드락 방지 전략

**문제 상황**
```
주문A: 상품 [1, 2, 3]
주문B: 상품 [3, 5, 6]

만약 락 획득 순서가 보장되지 않으면:
- 주문A: 1 → 2 → [3 대기]
- 주문B: 3 → 5 → [1 대기]
→ 데드락 발생!
```

**해결 방법**
```sql
-- ORDER BY p.productId ASC 추가
SELECT * FROM product WHERE product_id IN (1,2,3) ORDER BY product_id ASC FOR UPDATE;
SELECT * FROM product WHERE product_id IN (3,5,6) ORDER BY product_id ASC FOR UPDATE;

항상 오름차순으로 락 획득:
- 주문A: 1 → 2 → 3
- 주문B: [3 대기] → 5 → 6
→ 순차 처리, 데드락 없음!
```

### 3-5. 동시성 시나리오별 분석

| 시나리오 | 동시성 제어 | 결과 |
|---------|------------|------|
| 같은 상품 포함된 주문들 | 상품 락으로 순차 처리 | ✅ 안전 |
| 완전히 다른 상품 주문 (같은 사용자) | 포인트/쿠폰 낙관적 락 | ✅ 안전 (드문 케이스) |
| 다른 사용자 다른 상품 | 독립적 처리 | ✅ 안전 |
| 다른 사용자 같은 상품 | 상품 락으로 순차 처리 | ✅ 안전 |

