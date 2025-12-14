package com.sparta.ecommerce.application.user.event;

/**
 * 포인트 차감 성공 이벤트
 *
 * 주문의 포인트 차감이 성공적으로 완료되었을 때 발행되는 이벤트입니다.
 */
public record PointDeductedSuccessEvent(
        Long orderId,
        Long userId
) {
}
