package com.sparta.ecommerce.application.order;

import com.sparta.ecommerce.domain.coupon.dto.ProductResponse;
import com.sparta.ecommerce.domain.order.OrderItemRepository;
import com.sparta.ecommerce.domain.order.entity.OrderItem;
import com.sparta.ecommerce.domain.product.Product;
import com.sparta.ecommerce.domain.product.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class OrderItemServiceTest {

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderItemService orderItemService;

    // InMemoryProductRepository에 초기화되어 있는 상품들:
    // 1L: 노트북
    // 2L: 마우스
    // 3L: 키보드

    @Test
    @DisplayName("상품별 판매량 조회 - 실제 주문 데이터 기반")
    void getSoldCountByProductId() {
        // given
        LocalDateTime now = LocalDateTime.now();

        // 노트북(1L): 총 15개 판매 (주문 2개)
        OrderItem item1_1 = new OrderItem(1L, 1L, 1L, "노트북", "고성능 노트북", 10, 1500000, now);
        OrderItem item1_2 = new OrderItem(2L, 2L, 1L, "노트북", "고성능 노트북", 5, 1500000, now);

        // 마우스(2L): 총 30개 판매 (주문 3개)
        OrderItem item2_1 = new OrderItem(3L, 1L, 2L, "마우스", "무선 마우스", 10, 30000, now);
        OrderItem item2_2 = new OrderItem(4L, 2L, 2L, "마우스", "무선 마우스", 10, 30000, now);
        OrderItem item2_3 = new OrderItem(5L, 3L, 2L, "마우스", "무선 마우스", 10, 30000, now);

        // 키보드(3L): 총 8개 판매 (주문 1개)
        OrderItem item3_1 = new OrderItem(6L, 3L, 3L, "키보드", "기계식 키보드", 8, 120000, now);

        orderItemRepository.saveAll(Arrays.asList(item1_1, item1_2, item2_1, item2_2, item2_3, item3_1));

        // when
        Map<Long, Integer> result = orderItemService.getSoldCountByProductId();

        // then
        assertThat(result.get(1L)).isEqualTo(15);  // 노트북: 10 + 5 = 15개
        assertThat(result.get(2L)).isEqualTo(30);  // 마우스: 10 + 10 + 10 = 30개
        assertThat(result.get(3L)).isEqualTo(8);   // 키보드: 8개
    }

    @Test
    @DisplayName("판매량 기준 인기 상품 조회 - 판매량 순서대로 정렬")
    void findTopProductsBySoldCount() {
        // given
        LocalDateTime now = LocalDateTime.now();

        // 노트북(1L): 50개 판매
        OrderItem item1 = new OrderItem(1L, 1L, 1L, "노트북", "고성능 노트북", 50, 1500000, now);

        // 마우스(2L): 100개 판매 (1위)
        OrderItem item2_1 = new OrderItem(2L, 2L, 2L, "마우스", "무선 마우스", 60, 30000, now);
        OrderItem item2_2 = new OrderItem(3L, 3L, 2L, "마우스", "무선 마우스", 40, 30000, now);

        // 키보드(3L): 20개 판매
        OrderItem item3 = new OrderItem(4L, 4L, 3L, "키보드", "기계식 키보드", 20, 120000, now);

        orderItemRepository.saveAll(Arrays.asList(item1, item2_1, item2_2, item3));

        // when
        List<ProductResponse> result = orderItemService.findTopProductsBySoldCount(3);

        // then
        assertThat(result).hasSize(3);
        // 판매량 순: 마우스(100개) > 노트북(50개) > 키보드(20개)
        assertThat(result.get(0).productId()).isEqualTo(2L);
        assertThat(result.get(0).productName()).isEqualTo("마우스");

        assertThat(result.get(1).productId()).isEqualTo(1L);
        assertThat(result.get(1).productName()).isEqualTo("노트북");

        assertThat(result.get(2).productId()).isEqualTo(3L);
        assertThat(result.get(2).productName()).isEqualTo("키보드");
    }

    @Test
    @DisplayName("판매량 기준 인기 상품 조회 - limit 적용")
    void findTopProductsBySoldCount_withLimit() {
        // given
        LocalDateTime now = LocalDateTime.now();

        OrderItem item1 = new OrderItem(1L, 1L, 1L, "노트북", "고성능 노트북", 80, 1500000, now);
        OrderItem item2 = new OrderItem(2L, 2L, 2L, "마우스", "무선 마우스", 150, 30000, now);
        OrderItem item3 = new OrderItem(3L, 3L, 3L, "키보드", "기계식 키보드", 50, 120000, now);

        orderItemRepository.saveAll(Arrays.asList(item1, item2, item3));

        // when - 상위 2개만 조회
        List<ProductResponse> result = orderItemService.findTopProductsBySoldCount(2);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).productId()).isEqualTo(2L);  // 마우스 150개
        assertThat(result.get(1).productId()).isEqualTo(1L);  // 노트북 80개
        // 키보드(50개)는 포함되지 않음
    }

    @Test
    @DisplayName("판매량 기준 인기 상품 조회 - 판매 이력이 없는 상품 포함")
    void findTopProductsBySoldCount_includeNoSalesProduct() {
        // given
        LocalDateTime now = LocalDateTime.now();

        // 노트북, 마우스만 판매됨, 키보드는 판매 이력 없음
        OrderItem item1 = new OrderItem(1L, 1L, 1L, "노트북", "고성능 노트북", 30, 1500000, now);
        OrderItem item2 = new OrderItem(2L, 2L, 2L, "마우스", "무선 마우스", 20, 30000, now);

        orderItemRepository.saveAll(Arrays.asList(item1, item2));

        // when
        List<ProductResponse> result = orderItemService.findTopProductsBySoldCount(10);

        // then
        assertThat(result).hasSize(3);  // 전체 상품 3개
        // 판매량 순: 노트북(30개) > 마우스(20개) > 키보드(0개)
        assertThat(result.get(0).productId()).isEqualTo(1L);
        assertThat(result.get(1).productId()).isEqualTo(2L);
        assertThat(result.get(2).productId()).isEqualTo(3L);
    }
}
