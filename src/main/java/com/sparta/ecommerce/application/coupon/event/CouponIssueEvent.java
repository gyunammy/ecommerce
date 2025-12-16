package com.sparta.ecommerce.application.coupon.event;

/**
 * Kafka를 통해 전달되는 쿠폰 발급 이벤트 메시지
 */
public record CouponIssueEvent(
        Long userId,
        Long couponId
) {
}
