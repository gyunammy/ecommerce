package com.sparta.ecommerce.domain.coupon.dto;

import java.time.LocalDateTime;

public record ProductResponse (
    Long productId,

    String productName,

    String description,

    Integer quantity,

    Integer price,

    Integer viewCount,

    LocalDateTime createdAt,

    LocalDateTime updatedAt
){}
