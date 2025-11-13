package com.sparta.ecommerce.domain.cart;

import com.sparta.ecommerce.domain.cart.entity.CartItem;

import java.util.List;

public interface CartRepository {
    CartItem save(CartItem cartItem);

    List<CartItem> findAllByUserId(Long userId);

    void deleteAllByUserId(Long userId);
}
