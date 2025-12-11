package com.sparta.ecommerce.application.product.event;

import com.sparta.ecommerce.domain.cart.dto.CartItemResponse;

import java.util.List;

public record StockRestoreEvent(
        List<CartItemResponse> cartItems
) {
}
