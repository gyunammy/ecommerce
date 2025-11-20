package com.sparta.ecommerce.infrastructure.jpa.order.impl;

import com.sparta.ecommerce.domain.coupon.dto.ProductResponse;
import com.sparta.ecommerce.domain.order.OrderItemRepository;
import com.sparta.ecommerce.domain.order.entity.OrderItem;
import com.sparta.ecommerce.infrastructure.jpa.order.JpaOrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class OrderItemRepositoryAdapter implements OrderItemRepository {
    private final JpaOrderItemRepository jpaOrderItemRepository;

    @Override
    public Map<Long, Integer> getSoldCountByProductId() {
        return jpaOrderItemRepository.getSoldCountByProductId();
    }

    @Override
    public List<ProductResponse> findTopProductsBySoldCount(int limit) {
        return jpaOrderItemRepository.findTopProductsBySoldCount(limit);
    }

    @Override
    public void saveAll(List<OrderItem> orderItems) {
        jpaOrderItemRepository.saveAll(orderItems);
    }
}
