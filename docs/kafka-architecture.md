# Kafka 기반 이벤트 처리 아키텍처 설계 문서

## 개요

이 문서는 sparta-e-commerce 프로젝트에서 Kafka를 활용하여 쿠폰 발급과 주문 생성 로직을 구현한 설계를 설명합니다.

## 목차

1. [쿠폰 발급 시스템](#1-쿠폰-발급-시스템)
2. [주문 생성 시스템](#2-주문-생성-시스템)
3. [Kafka 설정](#3-kafka-설정)

---

## 1. 쿠폰 발급 시스템

### 1.1 설계 목적

선착순 쿠폰 발급 시 동시성 문제를 해결하기 위해 Kafka의 순차 처리 특성을 활용합니다.

### 1.2 아키텍처 플로우

```
[Client Request]
       ↓
[IssueCouponUseCase.issueCoupon()]
    - 사용자 검증
    - 쿠폰 존재 확인
    - 중복 발급 검증
       ↓
[Kafka Producer]
    - Topic: coupon-issue-topic
    - Event: CouponIssueEvent(userId, couponId)
       ↓
[Kafka Broker]
    - 메시지 큐잉
    - 순차 처리 보장
       ↓
[CouponIssueConsumer.consumeCouponIssueEvent()]
    - @KafkaListener로 이벤트 수신
       ↓
[IssueCouponUseCase.executeIssueCoupon()]
    - 쿠폰 재조회
    - 발급 가능 여부 검증
    - 중복 발급 검증
    - 쿠폰 발급 (UserCoupon 생성)
    - 발급 수량 증가 (낙관적 락 적용)
```

### 1.3 핵심 컴포넌트

#### Producer (IssueCouponUseCase)

```java
// src/main/java/com/sparta/ecommerce/application/coupon/IssueCouponUseCase.java:32
public void issueCoupon(Long userId, Long couponId) {
    // 1. 사전 검증
    userService.getUserById(userId);
    Coupon coupon = couponService.getCouponById(couponId);

    // 2. Kafka로 이벤트 발행
    CouponIssueEvent event = new CouponIssueEvent(userId, couponId);
    kafkaTemplate.send("coupon-issue-topic", event);
}
```

#### Consumer (CouponIssueConsumer)

```java
// src/main/java/com/sparta/ecommerce/application/coupon/consumer/CouponIssueConsumer.java:28
@KafkaListener(
    topics = "coupon-issue-topic",
    groupId = "${spring.kafka.consumer.group-id}",
    containerFactory = "kafkaListenerContainerFactory"
)
public void consumeCouponIssueEvent(CouponIssueEvent event) {
    issueCouponUseCase.executeIssueCoupon(event.userId(), event.couponId());
}
```

### 1.4 동시성 제어 전략

1. **Kafka의 순차 처리**: Consumer가 메시지를 순차적으로 처리하여 선착순 보장
2. **낙관적 락**: Coupon 엔티티의 version 필드로 동시 수정 방지
3. **DB Unique Constraint**: user_id + coupon_id 복합 유니크 제약으로 중복 발급 방지

---

## 2. 주문 생성 시스템

### 2.1 설계 목적

주문 생성 후 재고 차감, 포인트 차감, 쿠폰 사용 등의 부수 작업을 비동기로 처리하여 응답 속도를 개선하고, Saga 패턴으로 분산 트랜잭션을 관리합니다.

### 2.2 아키텍처 플로우

```
[Client Request]
       ↓
[CreateOrderUseCase.createOrder()]
    - 낙관적 검증 (재고, 금액, 쿠폰, 포인트)
    - 주문 PENDING 상태로 생성
    - 장바구니 삭제
       ↓
[Kafka Producer]
    - Topic: order-created-topic
    - Event: OrderCreatedEvent
       ↓
[OrderCreatedEventConsumer.consumeOrderCreatedEvent()]
       ↓
    [1] 재고 차감 (멀티락 사용)
       ↓
    [2] 포인트 차감
       ↓
    [3] 쿠폰 사용
       ↓
    [4] 랭킹 업데이트
       ↓
    [5] 주문 상태 → COMPLETED

    [실패 시 보상 트랜잭션]
       ↓
    - 주문 상태 → FAILED
    - 쿠폰 복구 (Kafka: coupon-restore-topic)
    - 포인트 복구 (Kafka: point-restore-topic)
    - 재고 복구 (Kafka: stock-restore-topic)
```

### 2.3 핵심 컴포넌트

#### Producer (CreateOrderUseCase)

```java
// src/main/java/com/sparta/ecommerce/application/order/CreateOrderUseCase.java:141
private Order executeOrderTransaction(
    User user,
    Long userCouponId,
    List<CartItemResponse> findCartItems,
    OrderValidation validation
) {
    // 주문 PENDING 상태로 생성
    Order createdOrder = orderService.createOrder(...);
    cartService.clearCart(user.getUserId());

    // Kafka로 이벤트 발행
    OrderCreatedEvent event = new OrderCreatedEvent(
        user.getUserId(),
        createdOrder.getOrderId(),
        userCouponId,
        validation.finalAmount,
        findCartItems
    );
    orderCreatedKafkaTemplate.send("order-created-topic", event);

    return createdOrder;
}
```

#### Consumer (OrderCreatedEventConsumer)

```java
// src/main/java/com/sparta/ecommerce/application/order/consumer/OrderCreatedEventConsumer.java:52
@KafkaListener(
    topics = "order-created-topic",
    groupId = "${spring.kafka.consumer.group-id}",
    containerFactory = "orderCreatedKafkaListenerContainerFactory"
)
public void consumeOrderCreatedEvent(OrderCreatedEvent event) {
    try {
        // 1. 재고 차감 (멀티락 포함)
        productService.decreaseStockWithLock(event.orderId(), event.userId(), event.cartItems());
        stockDecreased = true;

        // 2. 포인트 차감
        userService.deductPointForOrder(event);
        pointDeducted = true;

        // 3. 쿠폰 사용
        userCouponService.processCouponUsage(event);
        couponUsed = (event.userCouponId() != null);

        // 4. 랭킹 업데이트
        for (CartItemResponse cartItem : event.cartItems()) {
            productRankingRepository.incrementSalesCount(
                cartItem.productId(),
                cartItem.quantity()
            );
        }

        // 5. 주문 완료 처리
        orderService.completeOrder(event.orderId());

    } catch (Exception e) {
        // 보상 트랜잭션 처리
        handleCompensation(event, stockDecreased, pointDeducted, couponUsed);
    }
}
```

### 2.4 보상 트랜잭션 (Saga Pattern)

주문 처리 중 실패 시 Saga 패턴으로 롤백을 수행합니다.

```java
// src/main/java/com/sparta/ecommerce/application/order/consumer/OrderCreatedEventConsumer.java:121
private void handleCompensation(
    OrderCreatedEvent event,
    boolean stockDecreased,
    boolean pointDeducted,
    boolean couponUsed
) {
    // 주문 상태를 FAILED로 변경
    orderService.failOrder(event.orderId());

    // 역순으로 복구
    if (couponUsed && event.userCouponId() != null) {
        couponRestoreKafkaTemplate.send("coupon-restore-topic",
            new CouponRestoreEvent(event.userId(), event.userCouponId()));
    }

    if (pointDeducted) {
        pointRestoreKafkaTemplate.send("point-restore-topic",
            new PointRestoreEvent(event.userId(), event.finalAmount()));
    }

    if (stockDecreased) {
        stockRestoreKafkaTemplate.send("stock-restore-topic",
            new StockRestoreEvent(event.cartItems()));
    }
}
```

---

## 3. Kafka 설정

### 3.1 토픽 구성

| 토픽명 | 파티션 수 | 복제본 수 | 용도 |
|--------|-----------|-----------|------|
| coupon-issue-topic | 3 | 1 | 쿠폰 발급 이벤트 |
| order-created-topic | 3 | 1 | 주문 생성 이벤트 |
| stock-restore-topic | 3 | 1 | 재고 복구 이벤트 |
| point-restore-topic | 3 | 1 | 포인트 복구 이벤트 |
| coupon-restore-topic | 3 | 1 | 쿠폰 복구 이벤트 |

### 3.2 Consumer 설정

```java
// src/main/java/com/sparta/ecommerce/config/KafkaConfig.java:46
@Bean
public NewTopic couponIssueTopic() {
    return TopicBuilder.name("coupon-issue-topic")
            .partitions(3)  // Consumer concurrency와 매칭
            .replicas(1)    // 테스트 환경용
            .build();
}
```

### 3.3 주요 설정 값

- **bootstrap-servers**: Kafka 브로커 주소
- **group-id**: Consumer 그룹 ID
- **auto-offset-reset**: earliest (처음부터 소비)
- **serializer/deserializer**: JSON 직렬화/역직렬화

---

## 4. 설계의 장점

### 4.1 쿠폰 발급

- **선착순 보장**: Kafka의 순차 처리로 자연스럽게 선착순 구현
- **부하 분산**: 대량 요청이 들어와도 Kafka가 큐잉하여 안정적 처리
- **확장성**: 파티션 수를 늘려 처리량 증대 가능

### 4.2 주문 생성

- **응답 속도 개선**: 주문 생성 후 즉시 응답, 나머지는 비동기 처리
- **트랜잭션 분리**: 각 단계별 트랜잭션 분리로 락 시간 최소화
- **장애 격리**: 한 부분의 실패가 전체 시스템에 영향을 주지 않음
- **보상 트랜잭션**: 실패 시 자동 롤백으로 데이터 일관성 유지

### 4.3 공통

- **느슨한 결합**: Producer와 Consumer가 독립적으로 동작
- **메시지 영속성**: Kafka가 메시지를 디스크에 저장하여 데이터 손실 방지
- **재처리 가능**: 실패 시 재시도 또는 DLQ(Dead Letter Queue) 처리 가능
