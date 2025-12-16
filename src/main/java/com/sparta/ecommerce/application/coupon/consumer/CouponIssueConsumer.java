package com.sparta.ecommerce.application.coupon.consumer;

import com.sparta.ecommerce.application.coupon.IssueCouponUseCase;
import com.sparta.ecommerce.application.coupon.event.CouponIssueEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 쿠폰 발급 이벤트 Kafka Consumer
 *
 * Kafka에서 쿠폰 발급 이벤트 메시지를 받아서 실제 발급 처리를 수행합니다.
 * 순차적으로 메시지를 처리하여 선착순 쿠폰 발급을 보장합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponIssueConsumer {

    private final IssueCouponUseCase issueCouponUseCase;

    /**
     * 쿠폰 발급 이벤트 메시지를 소비합니다.
     *
     * @param event 쿠폰 발급 이벤트 정보 (userId, couponId)
     */
    @KafkaListener(
            topics = "coupon-issue-topic",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeCouponIssueEvent(CouponIssueEvent event) {
        log.info("쿠폰 발급 이벤트 수신 - userId: {}, couponId: {}",
                event.userId(), event.couponId());

        try {
            // 실제 쿠폰 발급 처리
            issueCouponUseCase.executeIssueCoupon(event.userId(), event.couponId());
            log.info("쿠폰 발급 처리 완료 - userId: {}, couponId: {}",
                    event.userId(), event.couponId());
        } catch (Exception e) {
            log.error("쿠폰 발급 처리 실패 - userId: {}, couponId: {}, error: {}",
                    event.userId(), event.couponId(), e.getMessage(), e);
            // TODO: 실패 처리 로직 (재시도, DLQ 등)
        }
    }
}
