package com.sparta.ecommerce.application.order.listener;

import com.sparta.ecommerce.application.order.OrderCompletionCoordinator;
import com.sparta.ecommerce.application.product.event.StockReservedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 주문 완료를 위한 재고 차감 성공 이벤트 리스너
 *
 * StockReservedEvent를 받아서 OrderCompletionCoordinator에 재고 차감 완료를 알립니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCompletionStockEventListener {

    private final OrderCompletionCoordinator coordinator;

    /**
     * 재고 차감 성공 이벤트를 처리합니다.
     *
     * @param event 재고 차감 성공 이벤트
     */
    @EventListener
    public void handle(StockReservedEvent event) {
        log.debug("재고 차감 성공 이벤트 수신 - OrderId: {}", event.orderId());
        coordinator.markStockReserved(event.orderId());
    }
}
