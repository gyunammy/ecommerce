package com.sparta.ecommerce.application.order.event;

import com.sparta.ecommerce.domain.cart.dto.CartItemResponse;

import java.util.List;

/**
 * 주문 생성 완료 이벤트
 *
 * 주문이 성공적으로 생성되었을 때 발행되는 이벤트입니다.
 * 이벤트 리스너에서 포인트 차감, 재고 차감, 쿠폰 사용 처리, 상품 랭킹 업데이트 등의 부수 작업을 처리합니다.
 */
public record OrderCreatedEvent(
        Long userId,
        Long orderId,
        Long userCouponId,
        int finalAmount,
        List<CartItemResponse> cartItems
) {
}
