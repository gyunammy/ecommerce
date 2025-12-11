package com.sparta.ecommerce.application.coupon.listener;

import com.sparta.ecommerce.application.coupon.UserCouponService;
import com.sparta.ecommerce.application.order.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 쿠폰 사용 이벤트 리스너
 *
 * OrderCreatedEvent를 받아서 병렬로 쿠폰 사용 처리를 위임합니다.
 * 성공/실패 처리 및 이벤트 발행은 UserCouponService에서 처리합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponUsedEventListener {

    private final UserCouponService userCouponService;

    /**
     * 주문 생성 이벤트를 처리하여 쿠폰 사용 처리를 위임합니다.
     *
     * OrderCreatedEvent를 받아서 병렬로 처리됩니다.
     * TransactionPhase.AFTER_COMMIT을 사용하여 주문 생성 트랜잭션 커밋 후 실행됩니다.
     * 실제 처리 로직은 UserCouponService.processCouponUsage()에 위임됩니다.
     *
     * @param event 주문 생성 완료 이벤트
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCouponUsed(OrderCreatedEvent event) {
        userCouponService.processCouponUsage(event);
    }
}
