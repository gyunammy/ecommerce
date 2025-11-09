package com.sparta.ecommerce.domain.coupon.dto;

import java.time.LocalDateTime;

public record CouponResponse(
        Long          couponId,       // 쿠폰_ID
        String        couponName,     // 쿠폰_명
        String        discountType,   // 할인타입(비율/금액)
        Integer       discountValue,  // 금액이면 금액, 비율이면 %
        Integer       totalQuantity,  // 총_쿠폰_개수
        Integer       issuedQuantity, // 발급_쿠폰_개수
        LocalDateTime createAt,        // 생성일
        LocalDateTime expiresAt        // 만료일
) {}
