package com.sparta.ecommerce.application.order.listener;

import com.sparta.ecommerce.application.order.OrderCompletionCoordinator;
import com.sparta.ecommerce.application.user.event.PointDeductedSuccessEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 주문 완료를 위한 포인트 차감 성공 이벤트 리스너
 *
 * PointDeductedSuccessEvent를 받아서 OrderCompletionCoordinator에 포인트 차감 완료를 알립니다.
 *
 * NOTE: Kafka로 통일되어 비활성화됨 - OrderCreatedEventConsumer에서 직접 처리
 */
@Slf4j
//@Component
@RequiredArgsConstructor
public class OrderCompletionPointEventListener {

    private final OrderCompletionCoordinator coordinator;

    /**
     * 포인트 차감 성공 이벤트를 처리합니다.
     *
     * @param event 포인트 차감 성공 이벤트
     */
    @EventListener
    public void handle(PointDeductedSuccessEvent event) {
        log.debug("포인트 차감 성공 이벤트 수신 - OrderId: {}", event.orderId());
        coordinator.markPointDeducted(event.orderId());
    }
}
