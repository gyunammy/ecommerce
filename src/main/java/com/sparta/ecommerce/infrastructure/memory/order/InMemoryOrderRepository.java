package com.sparta.ecommerce.infrastructure.memory.order;

import com.sparta.ecommerce.domain.order.OrderRepository;
import com.sparta.ecommerce.domain.order.entity.Order;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.Map;

@Repository
public class InMemoryOrderRepository implements OrderRepository {

    private final Map<Long, Order> table = new LinkedHashMap<>();
    private long cursor = 1;

    @Override
    public Order save(Order order) {
        if (order.getOrderId() == null) {
            order.setOrderId(cursor++);
        }
        table.put(order.getOrderId(), order);
        return order;
    }
}
