package com.sparta.ecommerce.application.order;

import com.sparta.ecommerce.domain.coupon.dto.ProductResponse;
import com.sparta.ecommerce.domain.order.OrderItemRepository;
import com.sparta.ecommerce.domain.product.Product;
import com.sparta.ecommerce.domain.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderItemService {

    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;

    /**
     * 상품별 판매량 조회
     *
     * @return 상품 ID를 키로, 판매량을 값으로 하는 Map
     */
    public Map<Long, Integer> getSoldCountByProductId() {
        return orderItemRepository.getSoldCountByProductId();
    }

    /**
     * 판매량 기준 인기 상품 조회
     *
     * OrderItem 도메인의 핵심 책임인 판매량을 기반으로 상품 정보를 조합
     *
     * @param limit 조회할 상품 개수
     * @return 판매량 기준 인기 상품 목록
     */
    public List<ProductResponse> findTopProductsBySoldCount(int limit) {
        // 1. 주문 아이템에서 상품별 판매량 집계
        Map<Long, Integer> soldCountMap = orderItemRepository.getSoldCountByProductId();

        // 2. 모든 상품 조회 후 판매량 기준 정렬
        return productRepository.findAll().stream()
                .sorted(Comparator.comparing(
                        (Product p) -> soldCountMap.getOrDefault(p.getProductId(), 0),
                        Comparator.reverseOrder()
                ))
                .limit(limit)
                .map(Product::from)
                .collect(Collectors.toList());
    }
}
