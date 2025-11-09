package com.sparta.ecommerce.infrastructure.memory.order;

import com.sparta.ecommerce.domain.order.OrderRepository;
import com.sparta.ecommerce.domain.order.entity.Order;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryOrderRepository implements OrderRepository {

    private final Map<Long, Order> table = new ConcurrentHashMap<>();
    private final AtomicLong cursor = new AtomicLong(1);

    @Override
    public Order save(Order order) {
        if (order.getOrderId() == null) {
            order.setOrderId(cursor.getAndIncrement());
        }
        table.put(order.getOrderId(), order);
        return order;
    }

    @Override
    public Optional<Order> findById(Long orderId) {
        return Optional.ofNullable(table.get(orderId));
    }

    @Override
    public List<Order> findAll() {
        return new ArrayList<>(table.values());
    }
}
