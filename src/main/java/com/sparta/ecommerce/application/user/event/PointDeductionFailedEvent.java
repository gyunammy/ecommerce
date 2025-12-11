package com.sparta.ecommerce.application.user.event;

import com.sparta.ecommerce.domain.cart.dto.CartItemResponse;

import java.util.List;

/**
 * 포인트 차감 실패 이벤트
 *
 * 주문 생성 후 포인트 차감 처리가 실패했을 때 발행되는 이벤트입니다.
 * 이벤트 리스너에서 보상 트랜잭션(주문 취소, 재고 복구)을 처리합니다.
 */
public record PointDeductionFailedEvent(
        Long orderId,
        Long userId,
        int finalAmount,
        String errorMessage,
        List<CartItemResponse> cartItems
) {
}
