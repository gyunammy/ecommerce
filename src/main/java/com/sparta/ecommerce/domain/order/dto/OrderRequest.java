package com.sparta.ecommerce.domain.order.dto;

public record OrderRequest(
        Long userId,        // 사용자_ID
        Long userCouponId   // 사용자_쿠폰_ID (nullable)
) {}
