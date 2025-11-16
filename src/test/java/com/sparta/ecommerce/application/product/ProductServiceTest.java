package com.sparta.ecommerce.application.product;

import com.sparta.ecommerce.domain.cart.dto.CartItemResponse;
import com.sparta.ecommerce.domain.coupon.dto.ProductResponse;
import com.sparta.ecommerce.domain.product.ProductRepository;
import com.sparta.ecommerce.domain.product.ProductSortType;
import com.sparta.ecommerce.domain.product.entity.Product;
import com.sparta.ecommerce.domain.product.exception.ProductException;
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
import java.util.Map;

import static com.sparta.ecommerce.domain.product.exception.ProductErrorCode.PRODUCT_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    @DisplayName("모든 상품 조회 성공")
    void findAll_success() {
        // given
        LocalDateTime now = LocalDateTime.now();
        Product product1 = new Product(1L, "상품1", "설명1", 100, 10000, 50, now, now);
        Product product2 = new Product(2L, "상품2", "설명2", 200, 20000, 100, now, now);
        Product product3 = new Product(3L, "상품3", "설명3", 300, 30000, 150, now, now);

        List<Product> products = Arrays.asList(product1, product2, product3);
        given(productRepository.findAll()).willReturn(products);

        // when
        List<ProductResponse> result = productService.findAll();

        // then
        assertThat(result).hasSize(3);
        assertThat(result.get(0).productId()).isEqualTo(1L);
        assertThat(result.get(0).productName()).isEqualTo("상품1");
        assertThat(result.get(1).productId()).isEqualTo(2L);
        assertThat(result.get(2).productId()).isEqualTo(3L);
        verify(productRepository).findAll();
    }

    @Test
    @DisplayName("상품이 없을 때 빈 리스트 반환")
    void findAll_empty() {
        // given
        given(productRepository.findAll()).willReturn(Collections.emptyList());

        // when
        List<ProductResponse> result = productService.findAll();

        // then
        assertThat(result).isEmpty();
        verify(productRepository).findAll();
    }

    @Test
    @DisplayName("장바구니 상품 목록으로 Product Map 생성 성공")
    void getProductMap_success() {
        // given
        LocalDateTime now = LocalDateTime.now();
        CartItemResponse cartItem1 = new CartItemResponse(1L, 1L, 100L, 2, now, now);
        CartItemResponse cartItem2 = new CartItemResponse(2L, 1L, 200L, 3, now, now);
        List<CartItemResponse> cartItems = Arrays.asList(cartItem1, cartItem2);

        Product product1 = new Product(100L, "상품1", "설명1", 100, 10000, 50, now, now);
        Product product2 = new Product(200L, "상품2", "설명2", 200, 20000, 100, now, now);
        List<Product> products = Arrays.asList(product1, product2);

        given(productRepository.findAllById(Arrays.asList(100L, 200L))).willReturn(products);

        // when
        Map<Long, Product> result = productService.getProductMap(cartItems);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(100L)).isEqualTo(product1);
        assertThat(result.get(200L)).isEqualTo(product2);
        verify(productRepository).findAllById(Arrays.asList(100L, 200L));
    }

    @Test
    @DisplayName("장바구니에 존재하지 않는 상품이 포함된 경우 예외 발생")
    void getProductMap_productNotFound() {
        // given
        LocalDateTime now = LocalDateTime.now();
        CartItemResponse cartItem1 = new CartItemResponse(1L, 1L, 100L, 2, now, now);
        CartItemResponse cartItem2 = new CartItemResponse(2L, 1L, 999L, 3, now, now); // 존재하지 않는 상품
        List<CartItemResponse> cartItems = Arrays.asList(cartItem1, cartItem2);

        Product product1 = new Product(100L, "상품1", "설명1", 100, 10000, 50, now, now);
        List<Product> products = Collections.singletonList(product1); // 999L 상품 없음

        given(productRepository.findAllById(Arrays.asList(100L, 999L))).willReturn(products);

        // when & then
        assertThatThrownBy(() -> productService.getProductMap(cartItems))
                .isInstanceOf(ProductException.class)
                .hasMessageContaining(PRODUCT_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("빈 장바구니로 Product Map 생성 시 빈 Map 반환")
    void getProductMap_emptyCart() {
        // given
        List<CartItemResponse> cartItems = Collections.emptyList();
        given(productRepository.findAllById(Collections.emptyList())).willReturn(Collections.emptyList());

        // when
        Map<Long, Product> result = productService.getProductMap(cartItems);

        // then
        assertThat(result).isEmpty();
        verify(productRepository).findAllById(Collections.emptyList());
    }

    @Test
    @DisplayName("상품 정보 업데이트")
    void updateProduct() {
        // given
        LocalDateTime now = LocalDateTime.now();
        Product product = new Product(1L, "상품1", "설명1", 95, 10000, 50, now, now);

        // when
        productService.updateProduct(product);

        // then
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("조회수 기준 인기 상품 조회")
    void findTopProductsByViewCount() {
        // given
        int limit = 3;
        LocalDateTime now = LocalDateTime.now();
        Product product1 = new Product(1L, "인기상품1", "설명1", 100, 10000, 500, now, now);
        Product product2 = new Product(2L, "인기상품2", "설명2", 200, 20000, 400, now, now);
        Product product3 = new Product(3L, "인기상품3", "설명3", 300, 30000, 300, now, now);

        List<Product> topProducts = Arrays.asList(product1, product2, product3);
        given(productRepository.findTopProducts(ProductSortType.VIEW_COUNT, limit)).willReturn(topProducts);

        // when
        List<ProductResponse> result = productService.findTopProductsByViewCount(limit);

        // then
        assertThat(result).hasSize(3);
        assertThat(result.get(0).viewCount()).isEqualTo(500);
        assertThat(result.get(1).viewCount()).isEqualTo(400);
        assertThat(result.get(2).viewCount()).isEqualTo(300);
        verify(productRepository).findTopProducts(ProductSortType.VIEW_COUNT, limit);
    }

    @Test
    @DisplayName("모든 상품 조회 (Product 엔티티 반환)")
    void findAllProducts() {
        // given
        LocalDateTime now = LocalDateTime.now();
        Product product1 = new Product(1L, "상품1", "설명1", 100, 10000, 50, now, now);
        Product product2 = new Product(2L, "상품2", "설명2", 200, 20000, 100, now, now);

        List<Product> products = Arrays.asList(product1, product2);
        given(productRepository.findAll()).willReturn(products);

        // when
        List<Product> result = productService.findAllProducts();

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isEqualTo(product1);
        assertThat(result.get(1)).isEqualTo(product2);
        verify(productRepository).findAll();
    }

    @Test
    @DisplayName("단일 상품으로 Product Map 생성")
    void getProductMap_singleItem() {
        // given
        LocalDateTime now = LocalDateTime.now();
        CartItemResponse cartItem = new CartItemResponse(1L, 1L, 100L, 5, now, now);
        List<CartItemResponse> cartItems = Collections.singletonList(cartItem);

        Product product = new Product(100L, "상품1", "설명1", 100, 10000, 50, now, now);
        List<Product> products = Collections.singletonList(product);

        given(productRepository.findAllById(Collections.singletonList(100L))).willReturn(products);

        // when
        Map<Long, Product> result = productService.getProductMap(cartItems);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(100L)).isEqualTo(product);
    }
}
