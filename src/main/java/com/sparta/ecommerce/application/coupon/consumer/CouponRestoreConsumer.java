package com.sparta.ecommerce.application.coupon.consumer;

import com.sparta.ecommerce.application.coupon.UserCouponService;
import com.sparta.ecommerce.application.coupon.event.CouponRestoreEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 쿠폰 복구 이벤트 Kafka Consumer
 *
 * 주문 실패 시 Kafka에서 쿠폰 복구 이벤트 메시지를 받아서 쿠폰을 복구합니다.
 * 보상 트랜잭션(Saga Compensation)의 일부로 동작합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponRestoreConsumer {

    private final UserCouponService userCouponService;

    /**
     * 쿠폰 복구 이벤트 메시지를 소비하여 쿠폰을 복구합니다.
     *
     * @param event 쿠폰 복구 이벤트 정보
     */
    @KafkaListener(
            topics = "coupon-restore-topic",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "couponRestoreKafkaListenerContainerFactory"
    )
    public void consumeCouponRestoreEvent(CouponRestoreEvent event) {
        log.info("쿠폰 복구 이벤트 수신 - UserId: {}, UserCouponId: {}",
                event.userId(), event.userCouponId());

        try {
            userCouponService.restoreCoupon(event.userId(), event.userCouponId());
            log.info("쿠폰 복구 이벤트 처리 완료 - UserId: {}, UserCouponId: {}",
                    event.userId(), event.userCouponId());
        } catch (Exception e) {
            log.error("쿠폰 복구 이벤트 처리 실패 - UserId: {}, UserCouponId: {}, error: {}",
                    event.userId(), event.userCouponId(), e.getMessage(), e);
            // 보상 트랜잭션의 실패는 심각한 문제이므로 모니터링/알람 필요
            // 필요시 DLQ(Dead Letter Queue) 처리 추가 가능
        }
    }
}
