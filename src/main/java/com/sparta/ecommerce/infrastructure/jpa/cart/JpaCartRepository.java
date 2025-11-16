package com.sparta.ecommerce.infrastructure.jpa.cart;

import com.sparta.ecommerce.domain.cart.entity.CartItem;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Primary
@Repository
public interface JpaCartRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByUserId(Long userId);

    void deleteAllByUserId(Long userId);
}
