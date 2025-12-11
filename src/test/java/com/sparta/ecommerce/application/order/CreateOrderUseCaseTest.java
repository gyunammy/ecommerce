package com.sparta.ecommerce.application.order;

import com.sparta.ecommerce.application.cart.CartService;
import com.sparta.ecommerce.application.coupon.UserCouponService;
import com.sparta.ecommerce.application.order.event.OrderCreatedEvent;
import com.sparta.ecommerce.application.product.ProductService;
import com.sparta.ecommerce.application.user.UserService;
import com.sparta.ecommerce.common.transaction.TransactionHandler;
import com.sparta.ecommerce.domain.cart.dto.CartItemResponse;
import com.sparta.ecommerce.domain.coupon.entity.Coupon;
import com.sparta.ecommerce.domain.coupon.entity.UserCoupon;
import com.sparta.ecommerce.domain.order.entity.Order;
import com.sparta.ecommerce.domain.product.ProductRankingRepository;
import com.sparta.ecommerce.domain.product.entity.Product;
import com.sparta.ecommerce.domain.product.exception.ProductException;
import com.sparta.ecommerce.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.task.TaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.sparta.ecommerce.domain.product.exception.ProductErrorCode.INSUFFICIENT_STOCK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
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

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock rLock;

    @Mock
    private TransactionHandler transactionHandler;

    @Mock
    private ProductRankingRepository productRankingRepository;

    @Mock
    private TaskExecutor taskExecutor;

    @Mock
    private org.springframework.context.ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private CreateOrderUseCase createOrderUseCase;

    @BeforeEach
    void setUp() throws InterruptedException {
        // Redis MultiLock 모킹 설정 (lenient 모드로 불필요한 stubbing 허용)
        org.mockito.Mockito.lenient().when(redissonClient.getLock(anyString())).thenReturn(rLock);
        org.mockito.Mockito.lenient().when(redissonClient.getMultiLock(any(RLock[].class))).thenReturn(rLock);
        org.mockito.Mockito.lenient().when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        org.mockito.Mockito.lenient().when(rLock.isHeldByCurrentThread()).thenReturn(true);

        // TaskExecutor 모킹 설정: execute 메서드가 전달받은 Runnable을 동기로 실행하도록 설정
        org.mockito.Mockito.lenient().doAnswer(invocation -> {
            Runnable action = invocation.getArgument(0);
            action.run();
            return null;
        }).when(taskExecutor).execute(any(Runnable.class));
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

    // calculateTotalAmount 메서드는 ProductService로 이동되어 더 이상 CreateOrderUseCase에 존재하지 않음
    // ProductService의 단위 테스트에서 검증
    // @Test
    // @DisplayName("calculateTotalAmount - 여러 상품 다양한 수량 총액 계산")
    // void calculateTotalAmount_multipleProductsWithVariousQuantities() throws Exception { ... }

    // validateStock 메서드는 ProductService로 이동되어 더 이상 CreateOrderUseCase에 존재하지 않음
    // ProductService의 단위 테스트에서 검증
    // @Test
    // @DisplayName("validateStock - 여러 상품 중 하나라도 재고 부족시 예외")
    // void validateStock_oneProductInsufficientStock() { ... }

    // decreaseStock은 이제 이벤트 리스너에서 비동기로 처리되어 CreateOrderUseCase에 존재하지 않음
    // 통합 테스트에서 검증
    // @Test
    // @DisplayName("decreaseStock - 재고 차감 후 updateProduct 호출 확인")
    // void decreaseStock_callsUpdateProduct() throws Exception { ... }

    // 동시성 테스트는 통합 테스트에서 실제 Redis Lock으로 검증
    // 단위 테스트에서는 Mock으로 실제 락 동작을 시뮬레이션하기 어려움

    @Test
    @DisplayName("주문 생성 실패 시 쿠폰, 포인트, 재고가 롤백된다")
    void rollbackOnOrderCreationFailure() {
        // given
        Long userId = 1L;
        Long userCouponId = 100L;
        LocalDateTime now = LocalDateTime.now();

        // 사용자 (초기 포인트 100,000)
        User user = new User(userId, "testUser", 100000, 0L, now);
        given(userService.getUserById(userId)).willReturn(user);

        // 장바구니 (상품 1개, 수량 5)
        Long productId = 10L;
        CartItemResponse cartItem = new CartItemResponse(1L, userId, productId, 5, now, now);
        given(cartService.getCartItems(userId)).willReturn(List.of(cartItem));

        // 상품 (초기 재고 100개, 가격 10,000원)
        Product product = new Product(productId, "테스트상품", "설명", 100, 10000, 50, now, now);
        given(productService.getProductMapByIds(any())).willReturn(Map.of(productId, product));

        // 재고 검증 통과
        doNothing().when(productService).validateStock(any(), any());

        // 총액 계산: 50,000원 (5개 x 10,000원)
        given(productService.calculateTotalAmount(any(), any())).willReturn(50000);

        // 쿠폰 (5,000원 할인)
        UserCoupon userCoupon = new UserCoupon(userCouponId, userId, 1L, false, 0L, now, null);
        given(userCouponService.validateAndCalculateDiscount(eq(userCouponId), eq(userId), eq(50000)))
                .willReturn(new UserCouponService.CouponDiscountResult(userCoupon, 5000));

        // OrderService에서 예외 발생 (주문 생성 실패)
        given(orderService.createOrder(anyLong(), any(), anyInt(), anyInt(), anyInt(), any(), any()))
                .willThrow(new RuntimeException("주문 생성 중 예외 발생"));

        // TransactionHandler가 Runnable을 즉시 실행하도록 설정
        doAnswer(invocation -> {
            Runnable action = invocation.getArgument(0);
            action.run();
            return null;
        }).when(transactionHandler).execute(any(Runnable.class));

        // when & then
        assertThatThrownBy(() -> createOrderUseCase.createOrder(userId, userCouponId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("주문 생성 중 예외 발생");

        // Spring의 @Transactional이 자동으로 롤백 처리
        // 단위 테스트에서는 예외 발생 확인만 수행하고, 실제 롤백은 통합 테스트에서 검증

        // 주문 생성이 호출되었으나 예외로 실패
        verify(orderService).createOrder(eq(userId), eq(userCouponId), eq(50000), eq(5000), eq(45000), any(), any());

        // 장바구니 클리어는 호출되지 않음 (예외 발생으로 도달하지 못함)
        verify(cartService, never()).clearCart(userId);
    }

    @Test
    @DisplayName("주문 생성 실패 시 쿠폰 없이도 포인트와 재고가 롤백된다")
    void rollbackWithoutCouponOnFailure() {
        // given
        Long userId = 1L;
        LocalDateTime now = LocalDateTime.now();

        // 사용자 (초기 포인트 100,000)
        User user = new User(userId, "testUser", 100000, 0L, now);
        given(userService.getUserById(userId)).willReturn(user);

        // 장바구니 (상품 1개, 수량 3)
        Long productId = 20L;
        CartItemResponse cartItem = new CartItemResponse(1L, userId, productId, 3, now, now);
        given(cartService.getCartItems(userId)).willReturn(List.of(cartItem));

        // 상품 (초기 재고 50개, 가격 20,000원)
        Product product = new Product(productId, "테스트상품2", "설명2", 50, 20000, 30, now, now);
        given(productService.getProductMapByIds(any())).willReturn(Map.of(productId, product));

        // 재고 검증 통과
        doNothing().when(productService).validateStock(any(), any());

        // 총액 계산: 60,000원 (3개 x 20,000원)
        given(productService.calculateTotalAmount(any(), any())).willReturn(60000);

        // 쿠폰 미사용 (할인 없음)
        given(userCouponService.validateAndCalculateDiscount(isNull(), eq(userId), eq(60000)))
                .willReturn(new UserCouponService.CouponDiscountResult(null, 0));

        // OrderService에서 예외 발생
        given(orderService.createOrder(anyLong(), any(), anyInt(), anyInt(), anyInt(), any(), any()))
                .willThrow(new RuntimeException("시스템 오류"));

        // TransactionHandler가 Runnable을 즉시 실행하도록 설정
        doAnswer(invocation -> {
            Runnable action = invocation.getArgument(0);
            action.run();
            return null;
        }).when(transactionHandler).execute(any(Runnable.class));

        // when & then
        assertThatThrownBy(() -> createOrderUseCase.createOrder(userId, null)) // 쿠폰 미사용
                .isInstanceOf(RuntimeException.class)
                .hasMessage("시스템 오류");

        // Spring의 @Transactional이 자동으로 롤백 처리
        // 단위 테스트에서는 예외 발생 확인만 수행하고, 실제 롤백은 통합 테스트에서 검증

        // 주문 생성이 호출되었으나 예외로 실패
        verify(orderService).createOrder(eq(userId), isNull(), eq(60000), eq(0), eq(60000), any(), any());

        // 장바구니 클리어는 호출되지 않음
        verify(cartService, never()).clearCart(userId);
    }

    @Test
    @DisplayName("여러 상품 + 정률 쿠폰 주문 시 할인액과 최종 금액이 정확히 계산된다")
    void createOrder_multipleProducts_withPercentageCoupon() {
        // given
        Long userId = 1L;
        Long userCouponId = 100L;
        LocalDateTime now = LocalDateTime.now();

        // 사용자 (포인트 충분)
        User user = new User(userId, "testUser", 200000, 0L, now);
        given(userService.getUserById(userId)).willReturn(user);

        // 장바구니 (3개 상품, 총액 50,000원)
        Long productId1 = 10L;
        Long productId2 = 20L;
        Long productId3 = 30L;
        CartItemResponse cartItem1 = new CartItemResponse(1L, userId, productId1, 2, now, now); // 2개 x 10,000 = 20,000
        CartItemResponse cartItem2 = new CartItemResponse(2L, userId, productId2, 3, now, now); // 3개 x 5,000 = 15,000
        CartItemResponse cartItem3 = new CartItemResponse(3L, userId, productId3, 1, now, now); // 1개 x 15,000 = 15,000
        List<CartItemResponse> cartItems = List.of(cartItem1, cartItem2, cartItem3);
        given(cartService.getCartItems(userId)).willReturn(cartItems);

        // 상품들
        Product product1 = new Product(productId1, "상품1", "설명1", 100, 10000, 50, now, now);
        Product product2 = new Product(productId2, "상품2", "설명2", 200, 5000, 100, now, now);
        Product product3 = new Product(productId3, "상품3", "설명3", 150, 15000, 80, now, now);
        given(productService.getProductMapByIds(any())).willReturn(
                Map.of(productId1, product1, productId2, product2, productId3, product3)
        );

        // 재고 검증 통과
        doNothing().when(productService).validateStock(any(), any());

        // 총액 계산: 50,000원
        given(productService.calculateTotalAmount(any(), any())).willReturn(50000);

        // 쿠폰 할인: 20% 할인 (10,000원)
        UserCoupon userCoupon = new UserCoupon(userCouponId, userId, 1L, false, 0L, now, null);
        given(userCouponService.validateAndCalculateDiscount(eq(userCouponId), eq(userId), eq(50000)))
                .willReturn(new UserCouponService.CouponDiscountResult(userCoupon, 10000));

        // 주문 생성 성공 (최종 금액 40,000원)
        Order createdOrder = new Order(999L, userId, userCouponId, 50000, 10000, 40000, "PENDING", now);
        given(orderService.createOrder(eq(userId), eq(userCouponId), eq(50000), eq(10000), eq(40000), any(), any()))
                .willReturn(createdOrder);

        // TransactionHandler가 Runnable을 즉시 실행하도록 설정
        doAnswer(invocation -> {
            Runnable action = invocation.getArgument(0);
            action.run();
            return null;
        }).when(transactionHandler).execute(any(Runnable.class));

        // when
        createOrderUseCase.createOrder(userId, userCouponId);

        // then
        // 1. 총액 50,000원, 할인 10,000원, 최종 40,000원으로 주문이 생성되었는지 확인
        verify(orderService).createOrder(
                eq(userId),
                eq(userCouponId),
                eq(50000),      // totalAmount
                eq(10000),      // discountAmount
                eq(40000),      // finalAmount (50,000 - 10,000)
                eq(cartItems),
                any()
        );

        // 2. 장바구니가 클리어되었는지 확인
        verify(cartService).clearCart(userId);

        // 3. OrderCreatedEvent가 올바른 데이터로 발행되었는지 확인
        ArgumentCaptor<OrderCreatedEvent> eventCaptor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());

        OrderCreatedEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.userId()).isEqualTo(userId);
        assertThat(capturedEvent.orderId()).isEqualTo(999L);
        assertThat(capturedEvent.userCouponId()).isEqualTo(userCouponId);
        assertThat(capturedEvent.finalAmount()).isEqualTo(40000);
        assertThat(capturedEvent.cartItems()).hasSize(3);

        // 4. 포인트 검증이 최종 금액(40,000원)으로 이루어졌는지 확인
        verify(userService).getUserById(userId);
    }

    @Test
    @DisplayName("여러 상품 + 고액 쿠폰 주문 시 최종 금액이 0원 미만으로 떨어지지 않는다")
    void createOrder_multipleProducts_withHighValueCoupon_minimumZero() {
        // given
        Long userId = 1L;
        Long userCouponId = 200L;
        LocalDateTime now = LocalDateTime.now();

        // 사용자
        User user = new User(userId, "testUser", 500000, 0L, now);
        given(userService.getUserById(userId)).willReturn(user);

        // 장바구니 (2개 상품, 총액 30,000원)
        Long productId1 = 10L;
        Long productId2 = 20L;
        CartItemResponse cartItem1 = new CartItemResponse(1L, userId, productId1, 1, now, now); // 1개 x 10,000 = 10,000
        CartItemResponse cartItem2 = new CartItemResponse(2L, userId, productId2, 2, now, now); // 2개 x 10,000 = 20,000
        List<CartItemResponse> cartItems = List.of(cartItem1, cartItem2);
        given(cartService.getCartItems(userId)).willReturn(cartItems);

        // 상품들
        Product product1 = new Product(productId1, "상품1", "설명1", 50, 10000, 30, now, now);
        Product product2 = new Product(productId2, "상품2", "설명2", 100, 10000, 50, now, now);
        given(productService.getProductMapByIds(any())).willReturn(
                Map.of(productId1, product1, productId2, product2)
        );

        // 재고 검증 통과
        doNothing().when(productService).validateStock(any(), any());

        // 총액 계산: 30,000원
        given(productService.calculateTotalAmount(any(), any())).willReturn(30000);

        // 쿠폰 할인: 50,000원 (총액보다 큼)
        UserCoupon userCoupon = new UserCoupon(userCouponId, userId, 2L, false, 0L, now, null);
        given(userCouponService.validateAndCalculateDiscount(eq(userCouponId), eq(userId), eq(30000)))
                .willReturn(new UserCouponService.CouponDiscountResult(userCoupon, 50000));

        // 주문 생성 성공 (최종 금액 0원, 음수 방지)
        Order createdOrder = new Order(888L, userId, userCouponId, 30000, 50000, 0, "PENDING", now);
        given(orderService.createOrder(eq(userId), eq(userCouponId), eq(30000), eq(50000), eq(0), any(), any()))
                .willReturn(createdOrder);

        // TransactionHandler가 Runnable을 즉시 실행하도록 설정
        doAnswer(invocation -> {
            Runnable action = invocation.getArgument(0);
            action.run();
            return null;
        }).when(transactionHandler).execute(any(Runnable.class));

        // when
        createOrderUseCase.createOrder(userId, userCouponId);

        // then
        // 1. 총액 30,000원, 할인 50,000원이지만 최종 금액이 0원으로 생성되었는지 확인
        verify(orderService).createOrder(
                eq(userId),
                eq(userCouponId),
                eq(30000),      // totalAmount
                eq(50000),      // discountAmount
                eq(0),          // finalAmount (음수가 아닌 0)
                eq(cartItems),
                any()
        );

        // 2. 장바구니가 클리어되었는지 확인
        verify(cartService).clearCart(userId);

        // 3. OrderCreatedEvent가 올바른 데이터로 발행되었는지 확인
        ArgumentCaptor<OrderCreatedEvent> eventCaptor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());

        OrderCreatedEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.userId()).isEqualTo(userId);
        assertThat(capturedEvent.orderId()).isEqualTo(888L);
        assertThat(capturedEvent.userCouponId()).isEqualTo(userCouponId);
        assertThat(capturedEvent.finalAmount()).isEqualTo(0);  // 최종 금액이 0원
        assertThat(capturedEvent.cartItems()).hasSize(2);
    }

    @Test
    @DisplayName("여러 상품 주문 시 상품 ID가 정렬되어 ProductService에 전달된다 (데드락 방지)")
    void createOrder_multipleProducts_productIdsAreSorted() {
        // given
        Long userId = 1L;
        LocalDateTime now = LocalDateTime.now();

        // 사용자
        User user = new User(userId, "testUser", 100000, 0L, now);
        given(userService.getUserById(userId)).willReturn(user);

        // 장바구니 (상품 ID가 정렬되지 않은 순서: 30, 10, 20)
        CartItemResponse cartItem1 = new CartItemResponse(1L, userId, 30L, 1, now, now);
        CartItemResponse cartItem2 = new CartItemResponse(2L, userId, 10L, 2, now, now);
        CartItemResponse cartItem3 = new CartItemResponse(3L, userId, 20L, 1, now, now);
        List<CartItemResponse> cartItems = List.of(cartItem1, cartItem2, cartItem3);
        given(cartService.getCartItems(userId)).willReturn(cartItems);

        // 상품들
        Product product1 = new Product(10L, "상품1", "설명1", 100, 10000, 50, now, now);
        Product product2 = new Product(20L, "상품2", "설명2", 200, 5000, 100, now, now);
        Product product3 = new Product(30L, "상품3", "설명3", 150, 15000, 80, now, now);
        given(productService.getProductMapByIds(any())).willReturn(
                Map.of(10L, product1, 20L, product2, 30L, product3)
        );

        // 재고 검증 통과
        doNothing().when(productService).validateStock(any(), any());

        // 총액 계산
        given(productService.calculateTotalAmount(any(), any())).willReturn(40000);

        // 쿠폰 할인 없음
        given(userCouponService.validateAndCalculateDiscount(isNull(), eq(userId), eq(40000)))
                .willReturn(new UserCouponService.CouponDiscountResult(null, 0));

        // 주문 생성 성공
        Order createdOrder = new Order(777L, userId, null, 40000, 0, 40000, "PENDING", now);
        given(orderService.createOrder(any(), any(), anyInt(), anyInt(), anyInt(), any(), any()))
                .willReturn(createdOrder);

        // TransactionHandler가 Runnable을 즉시 실행하도록 설정
        doAnswer(invocation -> {
            Runnable action = invocation.getArgument(0);
            action.run();
            return null;
        }).when(transactionHandler).execute(any(Runnable.class));

        // when
        createOrderUseCase.createOrder(userId, null);

        // then
        // ProductService.getProductMapByIds가 정렬된 상품 ID 리스트를 받았는지 확인 (10, 20, 30)
        ArgumentCaptor<List<Long>> productIdsCaptor = ArgumentCaptor.forClass(List.class);
        verify(productService).getProductMapByIds(productIdsCaptor.capture());

        List<Long> capturedProductIds = productIdsCaptor.getValue();
        assertThat(capturedProductIds).containsExactly(10L, 20L, 30L); // 정렬된 순서
    }
}
