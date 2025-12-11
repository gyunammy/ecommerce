package com.sparta.ecommerce.application.order.listener;

import com.sparta.ecommerce.application.order.OrderCompletionCoordinator;
import com.sparta.ecommerce.application.order.event.OrderCompletionCleanupEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCompletionCleanupListener {

    private final OrderCompletionCoordinator orderCompletionCoordinator;

    @EventListener
    public void handle(OrderCompletionCleanupEvent event) {
        log.info("주문 완료 추적 정리 이벤트 수신 - OrderId: {}", event.orderId());
        try {
            orderCompletionCoordinator.clearTracking(event.orderId());
            log.info("주문 완료 추적 정리 완료 - OrderId: {}", event.orderId());
        } catch (Exception e) {
            log.error("주문 완료 추적 정리 실패 - OrderId: {}, Error: {}", event.orderId(), e.getMessage(), e);
            // 이 작업은 실패해도 다른 보상 트랜잭션에 직접적인 영향을 주지는 않지만,
            // 추적 정보가 남으면 오해의 소지가 있으므로 로깅 및 모니터링이 중요합니다.
        }
    }
}
