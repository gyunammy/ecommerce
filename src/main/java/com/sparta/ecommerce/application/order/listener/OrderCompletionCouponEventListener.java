package com.sparta.ecommerce.application.order.listener;

import com.sparta.ecommerce.application.coupon.event.CouponUsedSuccessEvent;
import com.sparta.ecommerce.application.order.OrderCompletionCoordinator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 주문 완료를 위한 쿠폰 사용 성공 이벤트 리스너
 *
 * CouponUsedSuccessEvent를 받아서 OrderCompletionCoordinator에 쿠폰 사용 완료를 알립니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCompletionCouponEventListener {

    private final OrderCompletionCoordinator coordinator;

    /**
     * 쿠폰 사용 성공 이벤트를 처리합니다.
     *
     * @param event 쿠폰 사용 성공 이벤트
     */
    @EventListener
    public void handle(CouponUsedSuccessEvent event) {
        log.debug("쿠폰 사용 성공 이벤트 수신 - OrderId: {}", event.orderId());
        coordinator.markCouponUsed(event.orderId());
    }
}
