package com.sparta.ecommerce.application.order;

import com.sparta.ecommerce.domain.coupon.dto.ProductResponse;
import com.sparta.ecommerce.domain.order.OrderItemRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * OrderItemService 단위 테스트
 * Mockito를 사용하여 의존성을 모킹
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderItemService 단위 테스트")
class OrderItemServiceTest {

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private OrderItemService orderItemService;

    @Test
    @DisplayName("상품별 판매량 조회 - Repository 메서드 호출 검증")
    void getSoldCountByProductId() {
        // given
        Map<Long, Integer> mockSoldCount = Map.of(
                1L, 15,  // 노트북: 15개
                2L, 30,  // 마우스: 30개
                3L, 8    // 키보드: 8개
        );

        when(orderItemRepository.getSoldCountByProductId()).thenReturn(mockSoldCount);

        // when
        Map<Long, Integer> result = orderItemService.getSoldCountByProductId();

        // then
        assertThat(result).isNotNull();
        assertThat(result.get(1L)).isEqualTo(15);
        assertThat(result.get(2L)).isEqualTo(30);
        assertThat(result.get(3L)).isEqualTo(8);
    }

    @Test
    @DisplayName("판매량 기준 인기 상품 조회 - 판매량 순서대로 정렬")
    void findTopProductsBySoldCount() {
        // given
        LocalDateTime now = LocalDateTime.now();
        List<ProductResponse> mockProducts = List.of(
                new ProductResponse(2L, "마우스", "무선 마우스", 50, 30000, 3200, now, now),      // 1위
                new ProductResponse(1L, "노트북", "고성능 노트북", 10, 1500000, 1500, now, now),   // 2위
                new ProductResponse(3L, "키보드", "기계식 키보드", 30, 120000, 2100, now, now)     // 3위
        );

        when(orderItemRepository.findTopProductsBySoldCount(3)).thenReturn(mockProducts);

        // when
        List<ProductResponse> result = orderItemService.findTopProductsBySoldCount(3);

        // then
        assertThat(result).hasSize(3);
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
        List<ProductResponse> mockProducts = List.of(
                new ProductResponse(2L, "마우스", "무선 마우스", 50, 30000, 3200, now, now),
                new ProductResponse(1L, "노트북", "고성능 노트북", 10, 1500000, 1500, now, now)
                // 키보드는 limit=2이므로 포함되지 않음
        );

        when(orderItemRepository.findTopProductsBySoldCount(2)).thenReturn(mockProducts);

        // when - 상위 2개만 조회
        List<ProductResponse> result = orderItemService.findTopProductsBySoldCount(2);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).productId()).isEqualTo(2L);
        assertThat(result.get(1).productId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("판매량 기준 인기 상품 조회 - 판매 이력이 있는 상품만 조회")
    void findTopProductsBySoldCount_onlySoldProducts() {
        // given - 노트북과 마우스만 판매됨 (키보드는 판매 이력 없음)
        LocalDateTime now = LocalDateTime.now();
        List<ProductResponse> mockProducts = List.of(
                new ProductResponse(1L, "노트북", "고성능 노트북", 10, 1500000, 1500, now, now),
                new ProductResponse(2L, "마우스", "무선 마우스", 50, 30000, 3200, now, now)
                // 키보드(productId=3)는 판매 이력이 없으므로 포함되지 않음
        );

        when(orderItemRepository.findTopProductsBySoldCount(10)).thenReturn(mockProducts);

        // when
        List<ProductResponse> result = orderItemService.findTopProductsBySoldCount(10);

        // then - 판매 이력이 있는 상품만 조회됨 (2개)
        assertThat(result).hasSize(2);
        assertThat(result.stream().anyMatch(p -> p.productId().equals(1L))).isTrue();
        assertThat(result.stream().anyMatch(p -> p.productId().equals(2L))).isTrue();
        // 키보드는 판매 이력이 없으므로 조회되지 않음
        assertThat(result.stream().noneMatch(p -> p.productId().equals(3L))).isTrue();
    }
}
