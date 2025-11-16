package com.sparta.ecommerce.infrastructure.jpa.order.impl;

import com.sparta.ecommerce.domain.order.OrderRepository;
import com.sparta.ecommerce.domain.order.entity.Order;
import com.sparta.ecommerce.infrastructure.jpa.order.JpaOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaOrderRepositoryImpl implements OrderRepository {
    private final JpaOrderRepository jpaOrderRepository;

    @Override
    public Order save(Order order) {
        return jpaOrderRepository.save(order);
    }

    @Override
    public Optional<Order> findById(Long orderId) {
        return jpaOrderRepository.findById(orderId);
    }

    @Override
    public List<Order> findAll() {
        return jpaOrderRepository.findAll();
    }
}
