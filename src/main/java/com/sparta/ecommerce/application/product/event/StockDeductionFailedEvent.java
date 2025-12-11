package com.sparta.ecommerce.application.product.event;

/**
 * 재고 차감 실패 이벤트
 *
 * 주문 생성 후 재고 차감 처리가 실패했을 때 발행되는 이벤트입니다.
 * 이벤트 리스너에서 보상 트랜잭션(주문 취소, 포인트/쿠폰 복구)을 처리합니다.
 */
public record StockDeductionFailedEvent(
        Long orderId,
        Long userId,
        String errorMessage
) {
}
