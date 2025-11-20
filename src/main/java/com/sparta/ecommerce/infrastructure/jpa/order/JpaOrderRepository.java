package com.sparta.ecommerce.infrastructure.jpa.order;

import com.sparta.ecommerce.domain.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaOrderRepository extends JpaRepository<Order, Long>{

}