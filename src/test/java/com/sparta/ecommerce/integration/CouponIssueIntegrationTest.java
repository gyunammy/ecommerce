package com.sparta.ecommerce.integration;

import com.sparta.ecommerce.application.coupon.IssueCouponUseCase;
import com.sparta.ecommerce.application.coupon.UserCouponService;
import com.sparta.ecommerce.domain.coupon.CouponRepository;
import com.sparta.ecommerce.domain.coupon.UserCouponRepository;
import com.sparta.ecommerce.domain.coupon.entity.Coupon;
import com.sparta.ecommerce.domain.coupon.entity.UserCoupon;
import com.sparta.ecommerce.domain.coupon.exception.CouponException;
import com.sparta.ecommerce.domain.user.UserRepository;
import com.sparta.ecommerce.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 선착순 쿠폰 발급 통합 테스트
 *
 * Controller → UseCase → Service → Repository 전체 레이어를 통합하여 테스트
 * TestContainers를 사용하여 실제 MySQL 환경에서 동시성 제어 검증
 */
@SpringBootTest
@Testcontainers
class CouponIssueIntegrationTest {

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
    private IssueCouponUseCase issueCouponUseCase;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserCouponService userCouponService;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private Long couponId;
    private List<Long> userIds;
    private static final int COUPON_STOCK = 50;
    private static final int THREAD_COUNT = 200;

    @BeforeEach
    void setUp() {
        org.springframework.transaction.support.TransactionTemplate transactionTemplate =
            new org.springframework.transaction.support.TransactionTemplate(transactionManager);

        transactionTemplate.execute(status -> {
            // 기존 데이터 삭제 (외래키 순서 고려)
            entityManager.createQuery("DELETE FROM OrderItem").executeUpdate();
            entityManager.createQuery("DELETE FROM Order").executeUpdate();
            entityManager.createQuery("DELETE FROM CartItem").executeUpdate();
            entityManager.createQuery("DELETE FROM UserCoupon").executeUpdate();
            entityManager.createQuery("DELETE FROM Coupon").executeUpdate();
            entityManager.createQuery("DELETE FROM Product").executeUpdate();
            entityManager.createQuery("DELETE FROM User").executeUpdate();

            // 테스트용 쿠폰 생성 (선착순 50개)
            LocalDateTime now = LocalDateTime.now();
            Coupon coupon = new Coupon(
                    null,
                    "선착순 10% 할인 쿠폰",
                    "RATE",
                    10,
                    COUPON_STOCK,  // 총 수량 50개
                    0,             // 발급 수량 0개
                    0,
                    now,
                    now.plusDays(30)
            );

            Coupon savedCoupon = couponRepository.save(coupon);
            couponId = savedCoupon.getCouponId();

            // 테스트용 사용자 200명 생성
            userIds = new java.util.ArrayList<>();
            for (int i = 1; i <= THREAD_COUNT; i++) {
                User user = new User(null, "user" + i, 1000000, 0L, now);
                User savedUser = userRepository.save(user);
                userIds.add(savedUser.getUserId());
            }

            return null;
        });
    }

    @Test
    @DisplayName("선착순 쿠폰 발급 통합 테스트 - 200명이 동시에 요청하면 50명만 성공")
    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void issueCoupon_concurrency_integration() throws InterruptedException {
        // given
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        AtomicInteger unexpectedErrorCount = new AtomicInteger(0);

        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        // when: 200명의 사용자가 동시에 쿠폰 발급 시도
        for (int i = 0; i < THREAD_COUNT; i++) {
            long userId = userIds.get(i);
            executorService.submit(() -> {
                try {
                    issueCouponUseCase.issueCoupon(userId, couponId);
                    successCount.incrementAndGet();
                } catch (CouponException e) {
                    // 재고 부족 또는 이미 발급받은 경우 예외 발생 (예상된 동작)
                    failCount.incrementAndGet();
                } catch (Exception e) {
                    // 예상치 못한 예외
                    unexpectedErrorCount.incrementAndGet();
                    System.err.println("Unexpected error for userId " + userId + ": " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        // then: 검증
        assertThat(completed).as("모든 스레드가 완료되어야 함").isTrue();
        assertThat(unexpectedErrorCount.get()).as("예상치 못한 에러가 없어야 함").isEqualTo(0);

        // 1. 정확히 50명만 쿠폰 발급 성공
        assertThat(successCount.get()).isEqualTo(COUPON_STOCK);
        assertThat(failCount.get()).isEqualTo(THREAD_COUNT - COUPON_STOCK);

        // 2. 쿠폰의 발급 수량이 50개로 증가했는지 확인
        Coupon issuedCoupon = couponRepository.findById(couponId).orElseThrow();
        assertThat(issuedCoupon.getIssuedQuantity()).isEqualTo(COUPON_STOCK);

        // 3. UserCoupon 테이블에 정확히 50개의 레코드가 생성되었는지 확인
        List<UserCoupon> issuedUserCoupons = userCouponRepository.findAll().stream()
                .filter(uc -> uc.getCouponId().equals(couponId))
                .toList();
        assertThat(issuedUserCoupons).hasSize(COUPON_STOCK);

        // 4. 발급된 쿠폰은 모두 미사용 상태여야 함
        assertThat(issuedUserCoupons).allMatch(uc -> !uc.isUsed());
    }

    @Test
    @DisplayName("통합 테스트 - 동일한 사용자가 같은 쿠폰을 두 번 발급받을 수 없음")
    void issueCoupon_duplicateIssue_shouldFail() {
        // given
        Long userId = userIds.get(0);

        // when: 첫 번째 발급
        issueCouponUseCase.issueCoupon(userId, couponId);

        // then: 두 번째 발급 시도 시 예외 발생
        assertThatThrownBy(() -> issueCouponUseCase.issueCoupon(userId, couponId))
                .isInstanceOf(CouponException.class)
                .hasMessageContaining("이미 발급받은 쿠폰입니다");

        // 검증: 쿠폰은 1개만 발급되어야 함
        Coupon coupon = couponRepository.findById(couponId).orElseThrow();
        assertThat(coupon.getIssuedQuantity()).isEqualTo(1);
    }

    @Test
    @DisplayName("통합 테스트 - 동일한 사용자 ID로 동시 쿠폰 발급 요청 시 Unique 제약 조건으로 한 번만 발급")
    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void issueCoupon_concurrentDuplicateByUserId_shouldFailWithUniqueConstraint() throws InterruptedException {
        // given: 동일한 사용자 ID로 동시에 발급 시도
        Long userId = userIds.get(0);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);

        // when: 동일한 사용자 ID로 2개의 스레드가 동시에 쿠폰 발급 시도
        for (int i = 0; i < 2; i++) {
            executorService.submit(() -> {
                try {
                    issueCouponUseCase.issueCoupon(userId, couponId);
                    successCount.incrementAndGet();
                    System.out.println("성공: userId=" + userId);
                } catch (CouponException e) {
                    // 중복 발급 또는 비즈니스 로직 예외
                    failCount.incrementAndGet();
                    System.out.println("CouponException 발생: " + e.getMessage());
                } catch (Exception e) {
                    // Unique 제약 조건 위반 등 다른 예외도 실패로 카운트
                    failCount.incrementAndGet();
                    System.out.println("예외 발생: " + e.getClass().getName() + " - " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        // then: 검증
        assertThat(completed).as("모든 스레드가 완료되어야 함").isTrue();

        // 1. 정확히 1번만 성공해야 함
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(1);

        // 2. 쿠폰은 1개만 발급되어야 함
        Coupon coupon = couponRepository.findById(couponId).orElseThrow();
        assertThat(coupon.getIssuedQuantity()).isEqualTo(1);

        // 3. UserCoupon 테이블에 해당 사용자의 쿠폰이 1개만 있어야 함
        List<UserCoupon> userCoupons = userCouponRepository.findAll().stream()
                .filter(uc -> uc.getUserId().equals(userId) && uc.getCouponId().equals(couponId))
                .toList();
        assertThat(userCoupons).hasSize(1);
    }

    @Test
    @DisplayName("통합 테스트 - 쿠폰 재고가 모두 소진되면 더 이상 발급 불가")
    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void issueCoupon_stockExhausted_shouldFail() throws InterruptedException {
        // given: 쿠폰 재고를 50개로 설정 (이미 setUp에서 설정됨)

        // when: 50명의 사용자가 쿠폰 발급
        ExecutorService executorService = Executors.newFixedThreadPool(COUPON_STOCK);
        CountDownLatch latch = new CountDownLatch(COUPON_STOCK);

        for (int i = 0; i < COUPON_STOCK; i++) {
            long userId = userIds.get(i);
            executorService.submit(() -> {
                try {
                    issueCouponUseCase.issueCoupon(userId, couponId);
                } catch (Exception e) {
                    // 동시성으로 인한 일부 실패 가능
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(40, TimeUnit.SECONDS);
        executorService.shutdown();
        assertThat(completed).as("모든 스레드가 완료되어야 함").isTrue();

        // then: 51번째 사용자는 쿠폰 발급 실패
        try {
            issueCouponUseCase.issueCoupon(userIds.get(COUPON_STOCK), couponId);
            org.junit.jupiter.api.Assertions.fail("재고 소진 시 예외가 발생해야 합니다");
        } catch (CouponException e) {
            assertThat(e.getMessage()).contains("쿠폰이 모두 소진되었습니다");
        }

        // 검증: 발급 수량이 총 수량과 같음
        Coupon coupon = couponRepository.findById(couponId).orElseThrow();
        assertThat(coupon.getIssuedQuantity()).isEqualTo(COUPON_STOCK);
    }

    @Test
    @DisplayName("통합 테스트 - 만료된 쿠폰은 발급 불가")
    @org.springframework.transaction.annotation.Transactional
    void issueCoupon_expiredCoupon_shouldFail() {
        // given: 만료된 쿠폰 생성
        LocalDateTime now = LocalDateTime.now();
        Coupon expiredCoupon = new Coupon(
                null,
                "만료된 쿠폰",
                "RATE",
                10,
                100,
                0,
                0,
                now.minusDays(60),
                now.minusDays(30)  // 30일 전 만료
        );
        Coupon savedExpiredCoupon = couponRepository.save(expiredCoupon);
        Long expiredCouponId = savedExpiredCoupon.getCouponId();

        // when & then: 발급 시도 시 예외 발생
        try {
            issueCouponUseCase.issueCoupon(userIds.get(0), expiredCouponId);
            org.junit.jupiter.api.Assertions.fail("만료된 쿠폰 발급 시 예외가 발생해야 합니다");
        } catch (CouponException e) {
            assertThat(e.getMessage()).contains("만료된 쿠폰입니다");
        }
    }

    @Test
    @DisplayName("통합 테스트 - 전체 발급 플로우 검증 (사용자 조회 → 쿠폰 검증 → 발급)")
    void issueCoupon_fullFlow_verification() {
        // given
        Long userId = userIds.get(0);

        // when: 쿠폰 발급
        issueCouponUseCase.issueCoupon(userId, couponId);

        // then: 각 레이어별 상태 검증

        // 1. 사용자가 존재하는지 확인
        User user = userRepository.findById(userId).orElseThrow();
        assertThat(user).isNotNull();
        assertThat(user.getName()).isEqualTo("user1");

        // 2. 쿠폰 발급 수량이 증가했는지 확인
        Coupon coupon = couponRepository.findById(couponId).orElseThrow();
        assertThat(coupon.getIssuedQuantity()).isEqualTo(1);

        // 3. UserCoupon이 생성되었는지 확인
        boolean hasCoupon = userCouponService.hasCoupon(userId, couponId);
        assertThat(hasCoupon).isTrue();

        // 4. 생성된 UserCoupon의 상태 확인
        List<UserCoupon> userCoupons = userCouponRepository.findAll();
        UserCoupon userCoupon = userCoupons.stream()
                .filter(uc -> uc.getUserId().equals(userId) && uc.getCouponId().equals(couponId))
                .findFirst()
                .orElseThrow();

        assertThat(userCoupon.isUsed()).isFalse();
        assertThat(userCoupon.getIssuedAt()).isNotNull();
    }
}
