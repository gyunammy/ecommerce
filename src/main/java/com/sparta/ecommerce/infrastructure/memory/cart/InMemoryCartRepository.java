package com.sparta.ecommerce.infrastructure.memory.cart;

import com.sparta.ecommerce.domain.cart.CartRepository;
import com.sparta.ecommerce.domain.cart.entity.CartItem;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class InMemoryCartRepository implements CartRepository {

    private final Map<Long, CartItem> table = new ConcurrentHashMap<>();
    private final AtomicLong cursor = new AtomicLong(1);

    @Override
    public CartItem save(CartItem cartItem) {
        Long newId = cursor.getAndIncrement();
        CartItem saveCartItem = new CartItem(newId, cartItem.getUserId(), cartItem.getProductId(), cartItem.getQuantity(), cartItem.getCreateAt(), cartItem.getCreateAt());
        table.put(newId, saveCartItem);
        return saveCartItem;
    }

    @Override
    public List<CartItem> findAllByUserId(Long userId) {
        return table.values()
                .stream()
                .filter(item -> item.getUserId().equals(userId))
                .collect(Collectors.toList());
    }

    @Override
    public void deleteAllByUserId(Long userId) {
        table.entrySet().removeIf(entry -> entry.getValue().getUserId().equals(userId));
    }
}
