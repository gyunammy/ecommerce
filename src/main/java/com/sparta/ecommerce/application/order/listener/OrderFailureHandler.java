package com.sparta.ecommerce.application.order.listener;

import com.sparta.ecommerce.application.coupon.event.CouponUsageFailedEvent;
import com.sparta.ecommerce.application.order.OrderService;
import com.sparta.ecommerce.application.product.event.StockDeductionFailedEvent;
import com.sparta.ecommerce.application.user.event.PointDeductionFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 주문 실패 처리 핸들러 (보상 트랜잭션)
 *
 * 주문 생성 후 부수 작업 실패 이벤트를 수신하여 보상 트랜잭션을 실행합니다.
 * Saga 패턴의 보상 트랜잭션으로, 이미 처리된 작업들을 롤백합니다.
 * 실제 처리 로직은 OrderService에 위임됩니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderFailureHandler {

    private final OrderService orderService;

    /**
     * 재고 차감 실패 이벤트를 처리하여 주문 실패 처리를 실행합니다.
     *
     * @param event 재고 차감 실패 이벤트
     */
    @EventListener
    public void handleStockDeductionFailure(StockDeductionFailedEvent event) {
        orderService.handleStockDeductionFailure(event);
    }

    /**
     * 포인트 차감 실패 이벤트를 처리하여 보상 트랜잭션을 실행합니다.
     *
     * @param event 포인트 차감 실패 이벤트
     */
    @EventListener
    public void handlePointDeductionFailure(PointDeductionFailedEvent event) {
        orderService.handlePointDeductionFailure(event);
    }

    /**
     * 쿠폰 사용 실패 이벤트를 처리하여 보상 트랜잭션을 실행합니다.
     *
     * @param event 쿠폰 사용 실패 이벤트
     */
    @EventListener
    public void handleCouponUsageFailure(CouponUsageFailedEvent event) {
        orderService.handleCouponUsageFailure(event);
    }
}
