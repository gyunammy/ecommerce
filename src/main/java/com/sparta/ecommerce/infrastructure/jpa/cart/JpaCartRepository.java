package com.sparta.ecommerce.infrastructure.jpa.cart;

import com.sparta.ecommerce.domain.cart.entity.CartItem;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Primary
@Repository
public interface JpaCartRepository extends JpaRepository<CartItem, Long> {

}
