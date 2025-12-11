package com.sparta.ecommerce.application.coupon.event;

import com.sparta.ecommerce.domain.cart.dto.CartItemResponse;

import java.util.List;

/**
 * 쿠폰 사용 실패 이벤트
 *
 * 주문 생성 후 쿠폰 사용 처리가 실패했을 때 발행되는 이벤트입니다.
 * 이벤트 리스너에서 보상 트랜잭션(주문 취소, 포인트/재고 복구)을 처리합니다.
 */
public record CouponUsageFailedEvent(
        Long orderId,
        Long userId,
        Long userCouponId,
        int finalAmount,
        String errorMessage,
        List<CartItemResponse> cartItems
) {
}
