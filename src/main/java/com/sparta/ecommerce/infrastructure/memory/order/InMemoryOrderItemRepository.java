package com.sparta.ecommerce.infrastructure.memory.order;

import com.sparta.ecommerce.domain.coupon.dto.ProductResponse;
import com.sparta.ecommerce.domain.order.OrderItemRepository;
import com.sparta.ecommerce.domain.order.entity.OrderItem;
import com.sparta.ecommerce.domain.product.ProductRepository;
import com.sparta.ecommerce.domain.product.entity.Product;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

//@Primary
@Repository
public class InMemoryOrderItemRepository implements OrderItemRepository {

    private final ProductRepository productRepository;
    private final Map<Long, OrderItem> table = new LinkedHashMap<>();
    private long cursor = 1;

    public InMemoryOrderItemRepository(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

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

    @Override
    public List<ProductResponse> findTopProductsBySoldCount(int limit) {
        // 1. 상품별 판매량 집계
        Map<Long, Integer> soldCountMap = getSoldCountByProductId();

        // 2. 판매량이 있는 상품 ID 목록 추출
        List<Long> productIds = new ArrayList<>(soldCountMap.keySet());

        // 3. 해당 상품들 조회
        List<Product> products = productRepository.findAllById(productIds);

        // 4. 판매량 기준 정렬 및 limit 적용
        return products.stream()
                .sorted(Comparator.comparing(
                        (Product p) -> soldCountMap.getOrDefault(p.getProductId(), 0),
                        Comparator.reverseOrder()
                ))
                .limit(limit)
                .map(Product::from)
                .collect(Collectors.toList());
    }
}
