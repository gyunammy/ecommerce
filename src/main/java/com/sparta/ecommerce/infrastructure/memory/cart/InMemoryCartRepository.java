package com.sparta.ecommerce.infrastructure.memory.cart;

import com.sparta.ecommerce.domain.cart.CartRepository;
import com.sparta.ecommerce.domain.cart.entity.CartItem;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class InMemoryCartRepository implements CartRepository {

    private final Map<Long, CartItem> table = new LinkedHashMap<>();
    private long cursor = 1;

    @PostConstruct
    public void init() {
        LocalDateTime now = LocalDateTime.now();
        table.put(cursor, new CartItem(cursor++, 1L, 1L, 2, now, now));
        table.put(cursor, new CartItem(cursor++, 1L, 2L, 1, now, now));
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
