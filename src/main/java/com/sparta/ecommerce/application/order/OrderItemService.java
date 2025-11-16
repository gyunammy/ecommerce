package com.sparta.ecommerce.application.order;

import com.sparta.ecommerce.domain.coupon.dto.ProductResponse;
import com.sparta.ecommerce.domain.order.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderItemService {

    private final OrderItemRepository orderItemRepository;

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
     * Repository 계층에서 최적화된 쿼리로 한번에 조회
     *
     * @param limit 조회할 상품 개수
     * @return 판매량 기준 인기 상품 목록
     */
    public List<ProductResponse> findTopProductsBySoldCount(int limit) {
        return orderItemRepository.findTopProductsBySoldCount(limit);
    }
}
