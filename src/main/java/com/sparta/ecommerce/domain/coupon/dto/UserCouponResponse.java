package com.sparta.ecommerce.domain.coupon.dto;

import java.time.LocalDateTime;

public record UserCouponResponse(
        Long          userCouponId, // 사용자_쿠폰_ID
        Long          userId,       // 사용자_ID
        Long          couponId,     // 쿠폰_ID
        Boolean       used,         // 사용_여부
        LocalDateTime issuedAt,     // 발급일
        LocalDateTime usedAt        // 사용일
) {}
