package com.sparta.ecommerce.application.product.listener;

import com.sparta.ecommerce.application.order.OrderService;
import com.sparta.ecommerce.application.order.event.OrderCreatedEvent;
import com.sparta.ecommerce.application.product.ProductService;
import com.sparta.ecommerce.domain.order.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 재고 차감 이벤트 리스너
 *
 * 주문 생성 이벤트를 수신하여 상품 재고를 차감합니다.
 * AFTER_COMMIT 페이즈를 사용하여 주문 생성 트랜잭션이 커밋된 후 별도 트랜잭션에서 처리됩니다.
 *
 * 동시성 제어:
 * - ProductService에서 Redis MultiLock을 획득하여 동시성 문제 방지
 * - 재고 차감은 별도 트랜잭션에서 실행됨
 *
 * 처리 흐름:
 * 1. 주문 상태 확인
 * 2. ProductService의 재고 차감 메서드 호출 (락 포함)
 * 3. ProductService에서 성공 시: StockReservedEvent 발행
 * 4. ProductService에서 실패 시: StockDeductionFailedEvent 발행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockDecreasedEventListener {

    private final ProductService productService;

    /**
     * 주문 생성 이벤트를 처리하여 상품 재고를 차감합니다.
     *
     * OrderCreatedEvent를 받아서 병렬로 처리됩니다.
     * TransactionPhase.AFTER_COMMIT을 사용하여 주문 생성 트랜잭션 커밋 후 실행됩니다.
     * ProductService에서 Redis MultiLock을 획득하여 동시성 문제를 방지합니다.
     * ProductService에서 재고 차감 성공 시 StockReservedEvent를 발행합니다.
     * ProductService에서 재고 차감 실패 시 StockDeductionFailedEvent를 발행합니다.
     *
     * @param event 주문 생성 완료 이벤트
     */
    @Transactional
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleStockDecreased(OrderCreatedEvent event) {
        log.debug("재고 차감 처리 시작 - OrderId: {}", event.orderId());

        // ProductService에서 재고 차감 및 이벤트 발행 처리
        productService.decreaseStockWithLock(event.orderId(), event.userId(), event.cartItems());
    }
}
