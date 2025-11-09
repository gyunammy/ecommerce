package com.sparta.ecommerce.domain.order;

import com.sparta.ecommerce.domain.order.entity.OrderItem;

import java.util.List;
import java.util.Map;

public interface OrderItemRepository {
    OrderItem save(OrderItem orderItem);
    List<OrderItem> saveAll(List<OrderItem> orderItems);

    /**
     * 상품별 판매량 조회
     * @return 상품 ID를 키로, 판매량을 값으로 하는 Map
     */
    Map<Long, Integer> getSoldCountByProductId();
}
