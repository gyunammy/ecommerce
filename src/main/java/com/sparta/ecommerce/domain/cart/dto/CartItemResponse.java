package com.sparta.ecommerce.domain.cart.dto;

import java.time.LocalDateTime;

public record CartItemResponse(
        Long          cartItemId, // 장바구니_상품_ID
        Long          userId,     // 사용자_ID
        Long          productId,  // 상품_ID
        Integer       quantity,   // 수량
        LocalDateTime createAt,   // 등록일
        LocalDateTime updatedAt   // 수정일
) {}
