package com.sparta.ecommerce.integration;

import com.sparta.ecommerce.application.cart.CartService;
import com.sparta.ecommerce.application.coupon.CouponService;
import com.sparta.ecommerce.application.coupon.UserCouponService;
import com.sparta.ecommerce.application.order.CreateOrderUseCase;
import com.sparta.ecommerce.application.product.ProductService;
import com.sparta.ecommerce.application.user.UserService;
import com.sparta.ecommerce.domain.cart.CartRepository;
import com.sparta.ecommerce.domain.cart.entity.CartItem;
import com.sparta.ecommerce.domain.cart.exception.CartException;
import com.sparta.ecommerce.domain.coupon.CouponRepository;
import com.sparta.ecommerce.domain.coupon.UserCouponRepository;
import com.sparta.ecommerce.domain.coupon.entity.Coupon;
import com.sparta.ecommerce.domain.coupon.entity.UserCoupon;
import com.sparta.ecommerce.domain.order.OrderRepository;
import com.sparta.ecommerce.domain.order.entity.Order;
import com.sparta.ecommerce.domain.product.entity.Product;
import com.sparta.ecommerce.domain.product.exception.ProductException;
import com.sparta.ecommerce.infrastructure.jpa.product.JpaProductRepository;
import com.sparta.ecommerce.domain.user.UserRepository;
import com.sparta.ecommerce.domain.user.entity.User;
import com.sparta.ecommerce.domain.user.exception.UserException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 주문 생성 통합 테스트
 *
 * Controller → UseCase → Service → Repository 전체 레이어를 통합하여 테스트
 * TestContainers를 사용하여 실제 MySQL 환경에서 동시성 제어 검증
 *
 * 참고: 각 테스트는 독립적으로 실행되며, BeforeEach에서 Repository를 초기화하지 않으므로
 * 테스트 간에 데이터가 공유될 수 있습니다. 이는 실제 운영 환경을 시뮬레이션하기 위함입니다.
 */
@SpringBootTest
@Testcontainers
@org.springframework.transaction.annotation.Transactional
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CreateOrderIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired
    private CreateOrderUseCase createOrderUseCase;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JpaProductRepository productRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CartService cartService;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    private Long productId;
    private static final int PRODUCT_STOCK = 50;
    private static final int PRODUCT_PRICE = 10000;

    @BeforeEach
    void setUp() {
        // 기존 데이터 삭제 (외래키 순서 고려)
        try {
            entityManager.createQuery("DELETE FROM OrderItem").executeUpdate();
            entityManager.createQuery("DELETE FROM Order").executeUpdate();
            entityManager.createQuery("DELETE FROM CartItem").executeUpdate();
            entityManager.createQuery("DELETE FROM UserCoupon").executeUpdate();
            entityManager.createQuery("DELETE FROM Coupon").executeUpdate();
            entityManager.createQuery("DELETE FROM Product").executeUpdate();
            entityManager.createQuery("DELETE FROM User").executeUpdate();
        } catch (Exception e) {
            // 첫 실행 시 데이터가 없을 수 있음
        }

        // 테스트용 상품 생성 (재고 50개)
        LocalDateTime now = LocalDateTime.now();
        Product product = new Product(
                null,
                "테스트 상품",
                "상품 설명",
                PRODUCT_STOCK,  // 재고 50개
                PRODUCT_PRICE,  // 가격 10,000원
                0,
                now,
                now
        );
        Product savedProduct = productRepository.save(product);
        productId = savedProduct.getProductId();

        // 테스트용 사용자 100명 생성 (포인트 충분)
        for (int i = 1; i <= 100; i++) {
            User user = new User((long) i, "user" + i, 1000000, now);
            userRepository.save(user);

            // 각 사용자마다 장바구니에 상품 1개씩 추가
            CartItem cartItem = new CartItem((long) i, (long) i, productId, 1, now, now);
            cartRepository.save(cartItem);
        }
    }

    @Test
    @DisplayName("통합 테스트 - 100명이 동시에 주문하면 재고 50개만 성공")
    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void createOrder_concurrency_integration() throws InterruptedException {
        // given
        int threadCount = 100;
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when: 100명의 사용자가 동시에 주문 시도
        for (int i = 1; i <= threadCount; i++) {
            long userId = i;
            executorService.submit(() -> {
                try {
                    createOrderUseCase.createOrder(userId, null);
                    successCount.incrementAndGet();
                } catch (ProductException e) {
                    // 재고 부족 예외는 예상된 동작
                    if (e.getMessage().contains("재고가 부족합니다")) {
                        failCount.incrementAndGet();
                    } else {
                        throw e;
                    }
                } catch (Exception e) {
                    // 예상치 못한 예외
                    System.err.println("Unexpected error for user " + userId + ": " + e.getMessage());
                    e.printStackTrace();
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        // then: 검증
        // 1. 정확히 50명만 주문 성공
        assertThat(successCount.get()).isEqualTo(PRODUCT_STOCK);
        assertThat(failCount.get()).isEqualTo(threadCount - PRODUCT_STOCK);

        // 2. 상품 재고가 0이 되었는지 확인
        Product product = productRepository.findById(productId).orElseThrow();
        assertThat(product.getQuantity()).isEqualTo(0);

        // 4. 성공한 사용자들의 장바구니가 비워졌는지 확인
        // (장바구니가 비워진 사용자 수 = 성공한 주문 수)
        int emptyCartCount = 0;
        for (int i = 1; i <= threadCount; i++) {
            if (cartRepository.findAllByUserId((long) i).isEmpty()) {
                emptyCartCount++;
            }
        }
        assertThat(emptyCartCount).isEqualTo(PRODUCT_STOCK);
    }

    @Test
    @DisplayName("통합 테스트 - 장바구니가 비어있으면 주문 실패")
    void createOrder_emptyCart_shouldFail() {
        // given: 장바구니를 비운다
        Long userId = 1L;
        cartService.clearCart(userId);

        // when & then: 주문 시도 시 예외 발생
        try {
            createOrderUseCase.createOrder(userId, null);
            org.junit.jupiter.api.Assertions.fail("빈 장바구니로 주문 시 예외가 발생해야 합니다");
        } catch (CartException e) {
            assertThat(e.getMessage()).contains("장바구니가 비어있습니다");
        }
    }

    @Test
    @DisplayName("통합 테스트 - 포인트 부족 시 주문 실패")
    void createOrder_insufficientPoint_shouldFail() {
        // given: 기존 사용자(id=1)의 포인트를 100원으로 변경
        Long userId = 1L;
        User user = userRepository.findById(userId).orElseThrow();
        User poorUser = new User(userId, user.getName(), 100, user.getCreatedAt());  // 100원만 보유
        userRepository.save(poorUser);

        // 영속성 컨텍스트를 DB에 반영
        entityManager.flush();
        entityManager.clear();

        // when & then: 주문 시도 시 예외 발생
        try {
            createOrderUseCase.createOrder(userId, null);
            org.junit.jupiter.api.Assertions.fail("포인트 부족 시 예외가 발생해야 합니다");
        } catch (UserException e) {
            assertThat(e.getMessage()).contains("포인트가 부족합니다");
        }

        // 검증: 상품 재고는 변하지 않아야 함
        Product product = productRepository.findById(productId).orElseThrow();
        assertThat(product.getQuantity()).isEqualTo(PRODUCT_STOCK);
    }

    @Test
    @DisplayName("통합 테스트 - 쿠폰 적용 주문 성공")
    void createOrder_withCoupon_success() {
        // given: 쿠폰 생성
        LocalDateTime now = LocalDateTime.now();
        Coupon coupon = new Coupon(
                null,
                "10% 할인 쿠폰",
                "RATE",
                10,
                100,
                0,
                0,
                now,
                now.plusDays(30)
        );
        Coupon savedCoupon = couponRepository.save(coupon);

        // 사용자에게 쿠폰 발급
        Long userId = 1L;
        UserCoupon newUserCoupon = new UserCoupon();
        newUserCoupon.setUserId(userId);
        newUserCoupon.setCouponId(savedCoupon.getCouponId());
        newUserCoupon.setUsed(false);
        newUserCoupon.setIssuedAt(LocalDateTime.now());
        UserCoupon userCoupon = userCouponRepository.save(newUserCoupon);

        // 현재 주문 개수 기록
        int initialOrderCount = orderRepository.findAll().size();

        // when: 쿠폰을 사용하여 주문
        createOrderUseCase.createOrder(userId, userCoupon.getUserCouponId());

        // then: 검증
        // 1. 주문 생성 확인
        assertThat(orderRepository.findAll().size()).isEqualTo(initialOrderCount + 1);
        Order order = orderRepository.findAll().get(orderRepository.findAll().size() - 1);

        // 2. 할인 금액 확인 (10,000원의 10% = 1,000원 할인)
        assertThat(order.getDiscountAmount()).isEqualTo(1000);
        assertThat(order.getUsedPoint()).isEqualTo(9000);

        // 3. 쿠폰 사용 확인
        UserCoupon usedCoupon = userCouponRepository.findById(userCoupon.getUserCouponId()).orElseThrow();
        assertThat(usedCoupon.isUsed()).isTrue();
        assertThat(usedCoupon.getUsedAt()).isNotNull();

        // 4. 사용자 포인트 차감 확인 (1,000,000 - 9,000 = 991,000)
        User user = userRepository.findById(userId).orElseThrow();
        assertThat(user.getPoint()).isEqualTo(991000);
    }

    @Test
    @DisplayName("통합 테스트 - 전체 주문 플로우 검증")
    void createOrder_fullFlow_verification() {
        // given
        Long userId = 1L;

        // 현재 주문 개수 기록
        int initialOrderCount = orderRepository.findAll().size();

        // when: 주문 생성
        createOrderUseCase.createOrder(userId, null);

        // then: 각 레이어별 상태 검증

        // 1. 주문이 생성되었는지 확인
        assertThat(orderRepository.findAll().size()).isEqualTo(initialOrderCount + 1);
        Order order = orderRepository.findAll().get(orderRepository.findAll().size() - 1);
        assertThat(order.getUserId()).isEqualTo(userId);
        assertThat(order.getTotalAmount()).isEqualTo(PRODUCT_PRICE);
        assertThat(order.getDiscountAmount()).isEqualTo(0);
        assertThat(order.getUsedPoint()).isEqualTo(PRODUCT_PRICE);

        // 2. 상품 재고가 차감되었는지 확인
        Product product = productRepository.findById(productId).orElseThrow();
        assertThat(product.getQuantity()).isEqualTo(PRODUCT_STOCK - 1);

        // 3. 사용자 포인트가 차감되었는지 확인
        User user = userRepository.findById(userId).orElseThrow();
        assertThat(user.getPoint()).isEqualTo(1000000 - PRODUCT_PRICE);

        // 4. 장바구니가 비워졌는지 확인
        assertThat(cartRepository.findAllByUserId(userId)).isEmpty();
    }

    @Test
    @DisplayName("통합 테스트 - 재고 부족 시 주문 실패")
    void createOrder_insufficientStock_shouldFail() {
        // given: 재고를 0으로 만든다 (50명이 주문)
        for (int i = 1; i <= PRODUCT_STOCK; i++) {
            createOrderUseCase.createOrder((long) i, null);
        }

        // 재고 확인
        Product product = productRepository.findById(productId).orElseThrow();
        assertThat(product.getQuantity()).isEqualTo(0);

        // when & then: 51번째 사용자 주문 시도
        try {
            createOrderUseCase.createOrder(51L, null);
            org.junit.jupiter.api.Assertions.fail("재고 부족 시 예외가 발생해야 합니다");
        } catch (ProductException e) {
            assertThat(e.getMessage()).contains("재고가 부족합니다");
        }

        // 검증: 주문 개수는 더 이상 증가하지 않아야 함 (이미 50개 생성됨)
    }

    @Test
    @DisplayName("통합 테스트 - synchronized로 동시성 제어 확인")
    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void createOrder_synchronized_verification() throws InterruptedException {
        // given: 재고 10개, 20명이 주문 시도
        // 별도의 상품 생성 (재고 10개)
        LocalDateTime now = LocalDateTime.now();
        Product limitedProduct = new Product(
                null,
                "한정 상품",
                "재고 10개",
                10,
                5000,
                0,
                now,
                now
        );
        Product savedLimitedProduct = productRepository.save(limitedProduct);
        Long limitedProductId = savedLimitedProduct.getProductId();

        // 20명의 사용자 생성 및 장바구니 추가
        for (int i = 101; i <= 120; i++) {
            User user = new User((long) i, "limitedUser" + i, 100000, now);
            userRepository.save(user);

            CartItem cartItem = new CartItem((long) (i + 1000), (long) i, limitedProductId, 1, now, now);
            cartRepository.save(cartItem);
        }

        int threadCount = 20;
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when: 20명이 동시에 주문 시도
        for (int i = 101; i <= 120; i++) {
            long userId = i;
            executorService.submit(() -> {
                try {
                    createOrderUseCase.createOrder(userId, null);
                    successCount.incrementAndGet();
                } catch (ProductException e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        // then: synchronized로 동기화되어 정확히 10명만 성공
        assertThat(successCount.get()).isEqualTo(10);
        assertThat(failCount.get()).isEqualTo(10);

        // 재고 확인
        Product finalProduct = productRepository.findById(limitedProductId).orElseThrow();
        assertThat(finalProduct.getQuantity()).isEqualTo(0);
    }
}
