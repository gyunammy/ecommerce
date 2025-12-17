package com.sparta.ecommerce.application.coupon.event;

/**
 * 쿠폰 복구 이벤트
 *
 * 주문 실패 시 사용된 쿠폰을 복구하기 위해 발행되는 이벤트입니다.
 * Kafka Consumer에서 이 이벤트를 받아 쿠폰 상태를 복구합니다.
 */
public record CouponRestoreEvent(
        Long userId,
        Long userCouponId
) {
}
