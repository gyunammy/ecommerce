package com.sparta.ecommerce.application.product;

import com.sparta.ecommerce.application.order.OrderItemService;
import com.sparta.ecommerce.domain.coupon.dto.ProductResponse;
import com.sparta.ecommerce.domain.product.ProductSortType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class GetTopProductsUseCaseTest {

    @Mock
    private ProductService productService;

    @Mock
    private OrderItemService orderItemService;

    @InjectMocks
    private GetTopProductsUseCase getTopProductsUseCase;

    @Test
    @DisplayName("조회수 기준 인기 상품 조회")
    void getTopProducts_byViewCount() {
        // given
        LocalDateTime now = LocalDateTime.now();
        ProductResponse product1 = new ProductResponse(1L, "인기상품1", "설명1", 100, 10000, 500, now, now);
        ProductResponse product2 = new ProductResponse(2L, "인기상품2", "설명2", 200, 20000, 400, now, now);
        ProductResponse product3 = new ProductResponse(3L, "인기상품3", "설명3", 300, 30000, 300, now, now);

        List<ProductResponse> topProducts = Arrays.asList(product1, product2, product3);
        given(productService.findTopProductsByViewCount(10)).willReturn(topProducts);

        // when
        List<ProductResponse> result = getTopProductsUseCase.getTopProducts(ProductSortType.VIEW_COUNT);

        // then
        assertThat(result).hasSize(3);
        assertThat(result.get(0).viewCount()).isEqualTo(500);
        assertThat(result.get(1).viewCount()).isEqualTo(400);
        assertThat(result.get(2).viewCount()).isEqualTo(300);
        verify(productService).findTopProductsByViewCount(10);
        verifyNoInteractions(orderItemService);
    }

    @Test
    @DisplayName("판매량 기준 인기 상품 조회")
    void getTopProducts_bySoldCount() {
        // given
        LocalDateTime now = LocalDateTime.now();
        ProductResponse product1 = new ProductResponse(1L, "베스트상품1", "설명1", 100, 10000, 100, now, now);
        ProductResponse product2 = new ProductResponse(2L, "베스트상품2", "설명2", 200, 20000, 200, now, now);

        List<ProductResponse> topProducts = Arrays.asList(product1, product2);
        given(orderItemService.findTopProductsBySoldCount(10)).willReturn(topProducts);

        // when
        List<ProductResponse> result = getTopProductsUseCase.getTopProducts(ProductSortType.SOLD_COUNT);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).productName()).isEqualTo("베스트상품1");
        assertThat(result.get(1).productName()).isEqualTo("베스트상품2");
        verify(orderItemService).findTopProductsBySoldCount(10);
        verifyNoInteractions(productService);
    }
}
