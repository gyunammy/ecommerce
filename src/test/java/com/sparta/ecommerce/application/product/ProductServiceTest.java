package com.sparta.ecommerce.application.product;

import com.sparta.ecommerce.domain.cart.dto.CartItemResponse;
import com.sparta.ecommerce.domain.coupon.dto.ProductResponse;
import com.sparta.ecommerce.domain.product.ProductRepository;
import com.sparta.ecommerce.domain.product.entity.Product;
import com.sparta.ecommerce.domain.product.exception.ProductException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RedissonClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.sparta.ecommerce.domain.product.exception.ProductErrorCode.INSUFFICIENT_STOCK;
import static com.sparta.ecommerce.domain.product.exception.ProductErrorCode.PRODUCT_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private RedissonClient redissonClient;

    @InjectMocks
    private ProductService productService;


    @Test
    @DisplayName("calculateTotalAmount - 장바구니 총액 계산 성공")
    void calculateTotalAmount_success() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.now();
        CartItemResponse cartItem1 = new CartItemResponse(1L, 1L, 10L, 2, now, now);  // 2개 x 10000원 = 20000원
        CartItemResponse cartItem2 = new CartItemResponse(2L, 1L, 20L, 3, now, now);  // 3개 x 5000원 = 15000원
        List<CartItemResponse> cartItems = Arrays.asList(cartItem1, cartItem2);

        Product product1 = new Product(10L, "상품1", "설명1", 100, 10000, 50, now, now);
        Product product2 = new Product(20L, "상품2", "설명2", 200, 5000, 100, now, now);
        Map<Long, Product> productMap = Map.of(10L, product1, 20L, product2);

        // when
        Integer result = (Integer) ReflectionTestUtils.invokeMethod(
                productService,
                "calculateTotalAmount",
                cartItems,
                productMap
        );

        // then
        assertThat(result).isEqualTo(35000);  // 20000 + 15000
    }

    @Test
    @DisplayName("calculateTotalAmount - 단일 상품 총액 계산")
    void calculateTotalAmount_singleProduct() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.now();
        CartItemResponse cartItem = new CartItemResponse(1L, 1L, 100L, 5, now, now);  // 5개 x 20000원 = 100000원
        List<CartItemResponse> cartItems = List.of(cartItem);

        Product product = new Product(100L, "고가상품", "설명", 50, 20000, 30, now, now);
        Map<Long, Product> productMap = Map.of(100L, product);

        // when
        Integer result = (Integer) ReflectionTestUtils.invokeMethod(
                productService,
                "calculateTotalAmount",
                cartItems,
                productMap
        );

        // then
        assertThat(result).isEqualTo(100000);
    }



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
    @DisplayName("상품 ID 목록으로 Product Map 생성 성공")
    void getProductMapByIds_success() {
        // given
        LocalDateTime now = LocalDateTime.now();
        Product product1 = new Product(100L, "상품1", "설명1", 100, 10000, 50, now, now);
        Product product2 = new Product(200L, "상품2", "설명2", 200, 20000, 100, now, now);
        List<Product> products = Arrays.asList(product1, product2);

        given(productRepository.findAllById(Arrays.asList(100L, 200L))).willReturn(products);

        // when
        Map<Long, Product> result = productService.getProductMapByIds(Arrays.asList(100L, 200L));

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(100L)).isEqualTo(product1);
        assertThat(result.get(200L)).isEqualTo(product2);
        verify(productRepository).findAllById(Arrays.asList(100L, 200L));
    }

    @Test
    @DisplayName("존재하지 않는 상품이 포함된 경우 예외 발생")
    void getProductMapByIds_productNotFound() {
        // given
        LocalDateTime now = LocalDateTime.now();
        Product product1 = new Product(100L, "상품1", "설명1", 100, 10000, 50, now, now);
        List<Product> products = Collections.singletonList(product1); // 999L 상품 없음

        given(productRepository.findAllById(Arrays.asList(100L, 999L))).willReturn(products);

        // when & then
        assertThatThrownBy(() -> productService.getProductMapByIds(Arrays.asList(100L, 999L)))
                .isInstanceOf(ProductException.class)
                .hasMessageContaining(PRODUCT_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("빈 상품 ID 목록으로 Product Map 생성 시 빈 Map 반환")
    void getProductMapByIds_empty() {
        // given
        given(productRepository.findAllById(Collections.emptyList())).willReturn(Collections.emptyList());

        // when
        Map<Long, Product> result = productService.getProductMapByIds(Collections.emptyList());

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
        given(productRepository.findTopProductsByViewCount(limit)).willReturn(topProducts);

        // when
        List<ProductResponse> result = productService.findTopProductsByViewCount(limit);

        // then
        assertThat(result).hasSize(3);
        assertThat(result.get(0).viewCount()).isEqualTo(500);
        assertThat(result.get(1).viewCount()).isEqualTo(400);
        assertThat(result.get(2).viewCount()).isEqualTo(300);
        verify(productRepository).findTopProductsByViewCount(limit);
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
    void getProductMapByIds_singleItem() {
        // given
        LocalDateTime now = LocalDateTime.now();
        Product product = new Product(100L, "상품1", "설명1", 100, 10000, 50, now, now);
        List<Product> products = Collections.singletonList(product);

        given(productRepository.findAllById(Collections.singletonList(100L))).willReturn(products);

        // when
        Map<Long, Product> result = productService.getProductMapByIds(Collections.singletonList(100L));

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(100L)).isEqualTo(product);
    }


    @Test
    @DisplayName("validateStock - 재고 검증 성공")
    void validateStock_success() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.now();
        CartItemResponse cartItem1 = new CartItemResponse(1L, 1L, 10L, 5, now, now);
        CartItemResponse cartItem2 = new CartItemResponse(2L, 1L, 20L, 3, now, now);
        List<CartItemResponse> cartItems = Arrays.asList(cartItem1, cartItem2);

        Product product1 = new Product(10L, "상품1", "설명1", 100, 10000, 50, now, now);  // 재고 100개
        Product product2 = new Product(20L, "상품2", "설명2", 200, 5000, 100, now, now);  // 재고 200개
        Map<Long, Product> productMap = Map.of(10L, product1, 20L, product2);

        // when & then - 예외가 발생하지 않아야 함
        ReflectionTestUtils.invokeMethod(
                productService,
                "validateStock",
                cartItems,
                productMap
        );
    }

    @Test
    @DisplayName("validateStock - 재고 부족 시 예외 발생")
    void validateStock_insufficientStock() {
        // given
        LocalDateTime now = LocalDateTime.now();
        CartItemResponse cartItem = new CartItemResponse(1L, 1L, 10L, 50, now, now);  // 50개 주문
        List<CartItemResponse> cartItems = List.of(cartItem);

        Product product = new Product(10L, "상품1", "설명1", 30, 10000, 50, now, now);  // 재고 30개
        Map<Long, Product> productMap = Map.of(10L, product);

        // when & then
        try {
            ReflectionTestUtils.invokeMethod(
                    productService,
                    "validateStock",
                    cartItems,
                    productMap
            );
            // 예외가 발생하지 않으면 테스트 실패
            org.junit.jupiter.api.Assertions.fail("ProductException이 발생해야 합니다");
        } catch (Exception e) {
            // 리플렉션 예외의 원인을 확인
            Throwable rootCause = e;
            while (rootCause.getCause() != null) {
                rootCause = rootCause.getCause();
            }
            assertThat(rootCause).isInstanceOf(ProductException.class);
            assertThat(rootCause.getMessage()).isEqualTo(INSUFFICIENT_STOCK.getMessage());
        }
    }

    @Test
    @DisplayName("decreaseStock - 재고 차감 성공")
    void decreaseStock_success() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.now();
        CartItemResponse cartItem1 = new CartItemResponse(1L, 1L, 10L, 5, now, now);
        CartItemResponse cartItem2 = new CartItemResponse(2L, 1L, 20L, 3, now, now);
        List<CartItemResponse> cartItems = Arrays.asList(cartItem1, cartItem2);

        Product product1 = new Product(10L, "상품1", "설명1", 100, 10000, 50, now, now);
        Product product2 = new Product(20L, "상품2", "설명2", 200, 5000, 100, now, now);
        Map<Long, Product> productMap = Map.of(10L, product1, 20L, product2);

        // when
        ReflectionTestUtils.invokeMethod(
                productService,
                "decreaseStock",
                cartItems,
                productMap
        );

        // then
        assertThat(product1.getQuantity()).isEqualTo(95);  // 100 - 5
        assertThat(product2.getQuantity()).isEqualTo(197);  // 200 - 3
        verify(productRepository, times(2)).save(any(Product.class));
    }

    @Test
    @DisplayName("decreaseStock - 단일 상품 재고 차감")
    void decreaseStock_singleProduct() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.now();
        CartItemResponse cartItem = new CartItemResponse(1L, 1L, 100L, 10, now, now);
        List<CartItemResponse> cartItems = List.of(cartItem);

        Product product = new Product(100L, "상품", "설명", 50, 10000, 30, now, now);
        Map<Long, Product> productMap = Map.of(100L, product);

        // when
        ReflectionTestUtils.invokeMethod(
                productService,
                "decreaseStock",
                cartItems,
                productMap
        );

        // then
        assertThat(product.getQuantity()).isEqualTo(40);  // 50 - 10
        verify(productRepository, times(1)).save(product);
    }
}
