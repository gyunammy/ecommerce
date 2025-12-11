package com.sparta.ecommerce.application.order.event;

/**
 * 재고 차감 완료 이벤트
 *
 * 주문의 재고 차감이 성공적으로 완료되었을 때 발행되는 이벤트입니다.
 * 이벤트 리스너에서 포인트 차감, 쿠폰 사용 등의 후속 작업을 처리합니다.
 */
public record OrderStockReservedEvent(
        Long userId,
        Long orderId,
        Long userCouponId,
        int finalAmount
) {
}
