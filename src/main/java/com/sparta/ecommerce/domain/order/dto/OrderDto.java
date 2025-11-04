package com.sparta.ecommerce.domain.order.dto;

public record OrderDto(
        Long userId,
        Long userCouponId
) {}
