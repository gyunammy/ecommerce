package com.sparta.ecommerce.infrastructure.jpa.cart.impl;

import com.sparta.ecommerce.domain.cart.CartRepository;
import com.sparta.ecommerce.domain.cart.entity.CartItem;
import com.sparta.ecommerce.infrastructure.jpa.cart.JpaCartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class JpaCartRepositoryImpl implements CartRepository {
    private final JpaCartRepository jpaCartRepository;

    @Override
    public CartItem save(CartItem cartItem) {
        return jpaCartRepository.save(cartItem);
    }

    @Override
    public List<CartItem> findAllByUserId(Long userId) {
        return jpaCartRepository.findByUserId(userId);
    }

    @Override
    public void deleteAllByUserId(Long userId) {
        jpaCartRepository.deleteAllByUserId(userId);
    }
}
