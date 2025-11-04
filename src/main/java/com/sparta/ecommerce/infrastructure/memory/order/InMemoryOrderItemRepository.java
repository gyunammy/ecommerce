package com.sparta.ecommerce.infrastructure.memory.order;

import com.sparta.ecommerce.domain.order.OrderItemRepository;
import com.sparta.ecommerce.domain.order.entity.OrderItem;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class InMemoryOrderItemRepository implements OrderItemRepository {

    private final Map<Long, OrderItem> table = new LinkedHashMap<>();
    private long cursor = 1;

    @Override
    public OrderItem save(OrderItem orderItem) {
        if (orderItem.getOrderItemId() == null) {
            orderItem.setOrderItemId(cursor++);
        }
        table.put(orderItem.getOrderItemId(), orderItem);
        return orderItem;
    }

    @Override
    public List<OrderItem> saveAll(List<OrderItem> orderItems) {
        List<OrderItem> savedItems = new ArrayList<>();
        for (OrderItem orderItem : orderItems) {
            savedItems.add(save(orderItem));
        }
        return savedItems;
    }

    @Override
    public Map<Long, Integer> getSoldCountByProductId() {
        return table.values().stream()
                .collect(Collectors.groupingBy(
                        OrderItem::getProductId,
                        Collectors.summingInt(OrderItem::getQuantity)
                ));
    }
}
