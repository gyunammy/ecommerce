package com.sparta.ecommerce.domain.order;

import com.sparta.ecommerce.domain.order.entity.Order;

public interface OrderRepository {
    Order save(Order order);
}
