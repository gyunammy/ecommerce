package com.sparta.ecommerce.application.cart;

import com.sparta.ecommerce.domain.cart.CartRepository;
import com.sparta.ecommerce.domain.cart.dto.CartItemResponse;
import com.sparta.ecommerce.domain.cart.entity.CartItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {
    private final CartRepository cartRepository;

    public List<CartItemResponse> getCartItems(Long userId) {
        return cartRepository.findAllByUserId(userId).stream()
                .map(CartItem::from)
                .toList();
    }

    public void clearCart(Long userId) {
        cartRepository.deleteAllByUserId(userId);
    }
}
