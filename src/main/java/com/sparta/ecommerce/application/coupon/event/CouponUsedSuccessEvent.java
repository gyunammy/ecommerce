package com.sparta.ecommerce.application.coupon.event;

/**
 * 쿠폰 사용 성공 이벤트
 *
 * 주문의 쿠폰 사용이 성공적으로 완료되었을 때 발행되는 이벤트입니다.
 */
public record CouponUsedSuccessEvent(
        Long orderId,
        Long userId
) {
}
