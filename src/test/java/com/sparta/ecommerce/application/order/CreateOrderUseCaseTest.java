package com.sparta.ecommerce.application.order;

import com.sparta.ecommerce.application.cart.CartService;
import com.sparta.ecommerce.application.coupon.UserCouponService;
import com.sparta.ecommerce.application.product.ProductService;
import com.sparta.ecommerce.application.user.UserService;
import com.sparta.ecommerce.domain.cart.dto.CartItemResponse;
import com.sparta.ecommerce.domain.coupon.entity.Coupon;
import com.sparta.ecommerce.domain.coupon.entity.UserCoupon;
import com.sparta.ecommerce.domain.product.Product;
import com.sparta.ecommerce.domain.product.exception.ProductException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.sparta.ecommerce.domain.product.exception.ProductErrorCode.INSUFFICIENT_STOCK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateOrderUseCaseTest {

    @Mock
    private CartService cartService;

    @Mock
    private UserService userService;

    @Mock
    private UserCouponService userCouponService;

    @Mock
    private OrderService orderService;

    @Mock
    private ProductService productService;

    @InjectMocks
    private CreateOrderUseCase createOrderUseCase;

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
                createOrderUseCase,
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
                createOrderUseCase,
                "calculateTotalAmount",
                cartItems,
                productMap
        );

        // then
        assertThat(result).isEqualTo(100000);
    }

    @Test
    @DisplayName("calculateFinalAmount - 최종 결제 금액 계산 (할인 적용)")
    void calculateFinalAmount_withDiscount() throws Exception {
        // given
        int totalAmount = 50000;
        int discountAmount = 10000;

        // when
        Integer result = (Integer) ReflectionTestUtils.invokeMethod(
                createOrderUseCase,
                "calculateFinalAmount",
                totalAmount,
                discountAmount
        );

        // then
        assertThat(result).isEqualTo(40000);  // 50000 - 10000
    }

    @Test
    @DisplayName("calculateFinalAmount - 할인이 없는 경우")
    void calculateFinalAmount_noDiscount() throws Exception {
        // given
        int totalAmount = 30000;
        int discountAmount = 0;

        // when
        Integer result = (Integer) ReflectionTestUtils.invokeMethod(
                createOrderUseCase,
                "calculateFinalAmount",
                totalAmount,
                discountAmount
        );

        // then
        assertThat(result).isEqualTo(30000);
    }

    @Test
    @DisplayName("calculateFinalAmount - 할인이 총액보다 큰 경우 최소 0원 보장")
    void calculateFinalAmount_discountExceedsTotal() throws Exception {
        // given
        int totalAmount = 10000;
        int discountAmount = 15000;

        // when
        Integer result = (Integer) ReflectionTestUtils.invokeMethod(
                createOrderUseCase,
                "calculateFinalAmount",
                totalAmount,
                discountAmount
        );

        // then
        assertThat(result).isEqualTo(0);  // 음수가 아닌 0
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
                createOrderUseCase,
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
                    createOrderUseCase,
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
                createOrderUseCase,
                "decreaseStock",
                cartItems,
                productMap
        );

        // then
        assertThat(product1.getQuantity()).isEqualTo(95);  // 100 - 5
        assertThat(product2.getQuantity()).isEqualTo(197);  // 200 - 3
        verify(productService, times(2)).updateProduct(any(Product.class));
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
                createOrderUseCase,
                "decreaseStock",
                cartItems,
                productMap
        );

        // then
        assertThat(product.getQuantity()).isEqualTo(40);  // 50 - 10
        verify(productService, times(1)).updateProduct(product);
    }

    @Test
    @DisplayName("processCouponDiscount - 쿠폰 없는 경우 할인 0원")
    void processCouponDiscount_noCoupon() throws Exception {
        // given
        Long userCouponId = null;
        Long userId = 1L;
        int totalAmount = 50000;

        // when
        Object result = ReflectionTestUtils.invokeMethod(
                createOrderUseCase,
                "processCouponDiscount",
                userCouponId,
                userId,
                totalAmount
        );

        // then
        assertThat(result).isNotNull();
        // CouponDiscountResult의 필드 확인
        Object userCoupon = ReflectionTestUtils.getField(result, "userCoupon");
        Object discountAmount = ReflectionTestUtils.getField(result, "discountAmount");

        assertThat(userCoupon).isNull();
        assertThat(discountAmount).isEqualTo(0);
        verifyNoInteractions(userCouponService);
    }

    @Test
    @DisplayName("processCouponDiscount - 정액 할인 쿠폰 적용")
    void processCouponDiscount_fixedAmountCoupon() throws Exception {
        // given
        Long userCouponId = 100L;
        Long userId = 1L;
        int totalAmount = 50000;

        LocalDateTime now = LocalDateTime.now();
        UserCoupon userCoupon = new UserCoupon(userCouponId, userId, 1L, false, now, now);
        Coupon coupon = new Coupon(1L, "5000원 할인", "AMOUNT", 5000, 100, 10, 5, now, now);

        UserCouponService.ValidatedCoupon validatedCoupon = new UserCouponService.ValidatedCoupon(userCoupon, coupon);
        given(userCouponService.validateAndGetCoupon(userCouponId, userId)).willReturn(validatedCoupon);

        // when
        Object result = ReflectionTestUtils.invokeMethod(
                createOrderUseCase,
                "processCouponDiscount",
                userCouponId,
                userId,
                totalAmount
        );

        // then
        assertThat(result).isNotNull();
        Object returnedUserCoupon = ReflectionTestUtils.getField(result, "userCoupon");
        Object discountAmount = ReflectionTestUtils.getField(result, "discountAmount");

        assertThat(returnedUserCoupon).isEqualTo(userCoupon);
        assertThat(discountAmount).isEqualTo(5000);
        verify(userCouponService).validateAndGetCoupon(userCouponId, userId);
    }

    @Test
    @DisplayName("processCouponDiscount - 정률 할인 쿠폰 적용")
    void processCouponDiscount_percentageCoupon() throws Exception {
        // given
        Long userCouponId = 200L;
        Long userId = 2L;
        int totalAmount = 100000;

        LocalDateTime now = LocalDateTime.now();
        UserCoupon userCoupon = new UserCoupon(userCouponId, userId, 2L, false, now, now);
        Coupon coupon = new Coupon(2L, "10% 할인", "RATE", 10, 100, 20, 8, now, now);

        UserCouponService.ValidatedCoupon validatedCoupon = new UserCouponService.ValidatedCoupon(userCoupon, coupon);
        given(userCouponService.validateAndGetCoupon(userCouponId, userId)).willReturn(validatedCoupon);

        // when
        Object result = ReflectionTestUtils.invokeMethod(
                createOrderUseCase,
                "processCouponDiscount",
                userCouponId,
                userId,
                totalAmount
        );

        // then
        assertThat(result).isNotNull();
        Object returnedUserCoupon = ReflectionTestUtils.getField(result, "userCoupon");
        Object discountAmount = ReflectionTestUtils.getField(result, "discountAmount");

        assertThat(returnedUserCoupon).isEqualTo(userCoupon);
        assertThat(discountAmount).isEqualTo(10000);  // 100000 * 10% = 10000
        verify(userCouponService).validateAndGetCoupon(userCouponId, userId);
    }

    @Test
    @DisplayName("calculateTotalAmount - 여러 상품 다양한 수량 총액 계산")
    void calculateTotalAmount_multipleProductsWithVariousQuantities() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.now();
        CartItemResponse cartItem1 = new CartItemResponse(1L, 1L, 10L, 1, now, now);   // 1개 x 50000원 = 50000원
        CartItemResponse cartItem2 = new CartItemResponse(2L, 1L, 20L, 10, now, now);  // 10개 x 1000원 = 10000원
        CartItemResponse cartItem3 = new CartItemResponse(3L, 1L, 30L, 2, now, now);   // 2개 x 25000원 = 50000원
        List<CartItemResponse> cartItems = Arrays.asList(cartItem1, cartItem2, cartItem3);

        Product product1 = new Product(10L, "고가상품", "설명1", 10, 50000, 20, now, now);
        Product product2 = new Product(20L, "저가상품", "설명2", 100, 1000, 200, now, now);
        Product product3 = new Product(30L, "중가상품", "설명3", 50, 25000, 100, now, now);
        Map<Long, Product> productMap = Map.of(10L, product1, 20L, product2, 30L, product3);

        // when
        Integer result = (Integer) ReflectionTestUtils.invokeMethod(
                createOrderUseCase,
                "calculateTotalAmount",
                cartItems,
                productMap
        );

        // then
        assertThat(result).isEqualTo(110000);  // 50000 + 10000 + 50000
    }

    @Test
    @DisplayName("validateStock - 여러 상품 중 하나라도 재고 부족시 예외")
    void validateStock_oneProductInsufficientStock() {
        // given
        LocalDateTime now = LocalDateTime.now();
        CartItemResponse cartItem1 = new CartItemResponse(1L, 1L, 10L, 5, now, now);   // 재고 충분
        CartItemResponse cartItem2 = new CartItemResponse(2L, 1L, 20L, 100, now, now); // 재고 부족!
        List<CartItemResponse> cartItems = Arrays.asList(cartItem1, cartItem2);

        Product product1 = new Product(10L, "상품1", "설명1", 100, 10000, 50, now, now);  // 재고 100개
        Product product2 = new Product(20L, "상품2", "설명2", 50, 5000, 100, now, now);   // 재고 50개 (100개 요청)
        Map<Long, Product> productMap = Map.of(10L, product1, 20L, product2);

        // when & then
        try {
            ReflectionTestUtils.invokeMethod(
                    createOrderUseCase,
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
        }
    }

    @Test
    @DisplayName("decreaseStock - 재고 차감 후 updateProduct 호출 확인")
    void decreaseStock_callsUpdateProduct() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.now();
        CartItemResponse cartItem1 = new CartItemResponse(1L, 1L, 10L, 2, now, now);
        CartItemResponse cartItem2 = new CartItemResponse(2L, 1L, 20L, 3, now, now);
        CartItemResponse cartItem3 = new CartItemResponse(3L, 1L, 30L, 1, now, now);
        List<CartItemResponse> cartItems = Arrays.asList(cartItem1, cartItem2, cartItem3);

        Product product1 = new Product(10L, "상품1", "설명1", 100, 10000, 50, now, now);
        Product product2 = new Product(20L, "상품2", "설명2", 200, 5000, 100, now, now);
        Product product3 = new Product(30L, "상품3", "설명3", 150, 15000, 80, now, now);
        Map<Long, Product> productMap = Map.of(10L, product1, 20L, product2, 30L, product3);

        // when
        ReflectionTestUtils.invokeMethod(
                createOrderUseCase,
                "decreaseStock",
                cartItems,
                productMap
        );

        // then
        verify(productService, times(3)).updateProduct(any(Product.class));
        verify(productService).updateProduct(product1);
        verify(productService).updateProduct(product2);
        verify(productService).updateProduct(product3);
    }
}
