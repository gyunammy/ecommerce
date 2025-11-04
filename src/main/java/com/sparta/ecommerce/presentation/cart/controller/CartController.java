package com.sparta.ecommerce.presentation.cart.controller;

import com.sparta.ecommerce.application.cart.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users/{userId}/carts")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    /**
     * 장바구니 목록 조회
     *
     * @return
     */
    @GetMapping
    public ResponseEntity<Object> getCartItems(@PathVariable Long userId) {
        return ResponseEntity.ok().body(cartService.getCartItems(userId));
    }
}
