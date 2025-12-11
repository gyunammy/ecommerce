package com.sparta.ecommerce.application.product.event;

/**
 * 재고 차감 성공 이벤트
 *
 * 주문의 재고 차감이 성공적으로 완료되었을 때 발행되는 이벤트입니다.
 */
public record StockReservedEvent(
        Long orderId,
        Long userId
) {
}
