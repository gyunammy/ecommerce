package com.sparta.ecommerce.domain.order;

import com.sparta.ecommerce.domain.order.entity.Order;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(Long orderId);
    List<Order> findAll();
}
