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
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

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
 * 선착순 쿠폰 발급 통합 테스트 (Kafka 기반)
 *
 * UseCase → Kafka Producer → Kafka Consumer → Service → Repository 전체 플로우를 통합하여 테스트
 * TestContainers를 사용하여 실제 MySQL, Redis, Kafka 환경에서 동시성 제어 검증
 */
@SpringBootTest(properties = {
    "spring.task.scheduling.enabled=false"  // 테스트 시 스케줄러 비활성화
})
@Testcontainers
class CouponIssueIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        // MySQL 설정
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);

        // Redis 설정
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);

        // Redis 연결 풀 증가 (Lettuce 기본값: 8 → 50으로 증가)
        registry.add("spring.data.redis.lettuce.pool.max-active", () -> "50");
        registry.add("spring.data.redis.lettuce.pool.max-idle", () -> "50");
        registry.add("spring.data.redis.lettuce.pool.min-idle", () -> "10");

        // Kafka 설정
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.consumer.group-id", () -> "test-group");
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");

        // Kafka Consumer Concurrency 설정 (병렬 처리)
        registry.add("spring.kafka.listener.concurrency", () -> "3");
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
    private static final int THREAD_COUNT = 100;  // 200명 → 100명으로 감소

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

            // 테스트용 사용자 생성 (THREAD_COUNT + 추가 여유분)
            userIds = new java.util.ArrayList<>();
            for (int i = 1; i <= THREAD_COUNT + 50; i++) {  // 여유분 추가
                User user = new User(null, "user" + i, 1000000, 0L, now);
                User savedUser = userRepository.save(user);
                userIds.add(savedUser.getUserId());
            }

            return null;
        });
    }

    @Test
    @DisplayName("선착순 쿠폰 발급 통합 테스트 - 100명이 동시에 요청하면 50명만 성공")
    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void issueCoupon_concurrency_integration() throws InterruptedException {
        // given
        AtomicInteger kafkaPublishCount = new AtomicInteger(0);
        AtomicInteger publishFailCount = new AtomicInteger(0);

        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        // when: 100명의 사용자가 동시에 쿠폰 발급 요청 (Kafka로 메시지 발행)
        for (int i = 0; i < THREAD_COUNT; i++) {
            long userId = userIds.get(i);
            executorService.submit(() -> {
                try {
                    issueCouponUseCase.issueCoupon(userId, couponId);
                    kafkaPublishCount.incrementAndGet();
                } catch (CouponException e) {
                    // 중복 발급 체크에서 실패
                    publishFailCount.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("Unexpected error for userId " + userId + ": " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        // Kafka 발행 완료 대기
        boolean completed = latch.await(60, TimeUnit.SECONDS);
        executorService.shutdown();
        assertThat(completed).as("모든 스레드가 완료되어야 함").isTrue();

        System.out.println("Kafka 발행 성공: " + kafkaPublishCount.get() + ", 실패: " + publishFailCount.get());

        // Kafka Consumer가 메시지를 모두 처리할 때까지 대기 (최대 120초)
        // 실제 발급된 UserCoupon 수를 확인하여 더 정확하게 대기
        int maxWaitSeconds = 120;
        int actualIssuedCount = 0;
        for (int i = 0; i < maxWaitSeconds; i++) {
            Thread.sleep(1000);

            // 실제 발급된 UserCoupon 수 확인
            List<UserCoupon> currentUserCoupons = userCouponRepository.findAll().stream()
                    .filter(uc -> uc.getCouponId().equals(couponId))
                    .toList();
            actualIssuedCount = currentUserCoupons.size();

            Coupon coupon = couponRepository.findById(couponId).orElseThrow();

            // 재고가 소진되었거나, 실제 발급 수가 재고에 도달한 경우 대기 종료
            if (actualIssuedCount >= COUPON_STOCK || coupon.getIssuedQuantity() >= COUPON_STOCK) {
                System.out.println("Kafka Consumer 처리 완료 - " +
                        "실제 발급: " + actualIssuedCount + "개, " +
                        "issuedQuantity: " + coupon.getIssuedQuantity() + "개");
                break;
            }

            // 5초마다 진행 상황 출력
            if (i % 5 == 0 && i > 0) {
                System.out.println("대기 중... (" + i + "초 경과) - " +
                        "실제 발급: " + actualIssuedCount + "개, " +
                        "issuedQuantity: " + coupon.getIssuedQuantity() + "개");
            }

            if (i == maxWaitSeconds - 1) {
                System.err.println("Kafka Consumer 처리 시간 초과 - " +
                        "실제 발급: " + actualIssuedCount + "개, " +
                        "issuedQuantity: " + coupon.getIssuedQuantity() + "개");
                org.junit.jupiter.api.Assertions.fail("Kafka Consumer 처리 시간 초과");
            }
        }

        // 최종 검증 전 추가 대기 (진행 중인 트랜잭션 완료 대기)
        Thread.sleep(2000);

        // then: 검증
        // 1. UserCoupon 테이블에 정확히 50개의 레코드 생성
        List<UserCoupon> issuedUserCoupons = userCouponRepository.findAll().stream()
                .filter(uc -> uc.getCouponId().equals(couponId))
                .toList();
        assertThat(issuedUserCoupons)
                .as("UserCoupon 테이블에 정확히 " + COUPON_STOCK + "개의 레코드가 생성되어야 함")
                .hasSize(COUPON_STOCK);

        // 2. 쿠폰의 발급 수량이 정확히 50개
        Coupon issuedCoupon = couponRepository.findById(couponId).orElseThrow();
        assertThat(issuedCoupon.getIssuedQuantity())
                .as("쿠폰의 issuedQuantity가 정확히 " + COUPON_STOCK + "개여야 함")
                .isEqualTo(COUPON_STOCK);

        // 3. 발급된 쿠폰은 모두 미사용 상태여야 함
        assertThat(issuedUserCoupons).allMatch(uc -> !uc.isUsed());

        System.out.println("테스트 완료 - 총 발급: " + issuedUserCoupons.size() + "개");
    }

    @Test
    @DisplayName("통합 테스트 - 동일한 사용자가 같은 쿠폰을 두 번 발급받을 수 없음")
    void issueCoupon_duplicateIssue_shouldFail() throws InterruptedException {
        // given
        Long userId = userIds.get(0);

        // when: 첫 번째 발급 요청
        issueCouponUseCase.issueCoupon(userId, couponId);

        // Kafka Consumer 처리 대기
        Thread.sleep(2000);

        // then: 두 번째 발급 시도 시 예외 발생 (중복 체크)
        assertThatThrownBy(() -> issueCouponUseCase.issueCoupon(userId, couponId))
                .isInstanceOf(CouponException.class)
                .hasMessageContaining("이미 발급받은 쿠폰입니다");

        // 검증: 쿠폰은 1개만 발급되어야 함
        Coupon coupon = couponRepository.findById(couponId).orElseThrow();
        assertThat(coupon.getIssuedQuantity()).isEqualTo(1);
    }

    @Test
    @DisplayName("통합 테스트 - 동일한 사용자 ID로 동시 쿠폰 발급 요청 시 DB Unique 제약 조건으로 한 번만 발급")
    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void issueCoupon_concurrentDuplicateByUserId_shouldFailWithUniqueConstraint() throws InterruptedException {
        // given: 동일한 사용자 ID로 동시에 발급 시도
        Long userId = userIds.get(0);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);

        // when: 동일한 사용자 ID로 2개의 스레드가 동시에 쿠폰 발급 요청
        for (int i = 0; i < 2; i++) {
            executorService.submit(() -> {
                try {
                    issueCouponUseCase.issueCoupon(userId, couponId);
                    successCount.incrementAndGet();
                    System.out.println("Kafka 발행 성공: userId=" + userId);
                } catch (CouponException e) {
                    // 중복 발급 체크에서 실패
                    failCount.incrementAndGet();
                    System.out.println("CouponException 발생: " + e.getMessage());
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    System.out.println("예외 발생: " + e.getClass().getName() + " - " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        // then: 검증
        assertThat(completed).as("모든 스레드가 완료되어야 함").isTrue();

        // Kafka 발행은 Race Condition으로 1번 또는 2번 성공 가능
        System.out.println("Kafka 발행 성공: " + successCount.get() + ", 실패: " + failCount.get());

        // Kafka Consumer 처리 대기
        Thread.sleep(3000);

        // 중요: 쿠폰은 DB Unique 제약 조건으로 1개만 발급되어야 함
        Coupon coupon = couponRepository.findById(couponId).orElseThrow();
        assertThat(coupon.getIssuedQuantity()).isEqualTo(1);

        // UserCoupon 테이블에 해당 사용자의 쿠폰이 1개만 있어야 함
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

        // when: 50명의 사용자가 쿠폰 발급 요청
        ExecutorService executorService = Executors.newFixedThreadPool(COUPON_STOCK);
        CountDownLatch latch = new CountDownLatch(COUPON_STOCK);

        for (int i = 0; i < COUPON_STOCK; i++) {
            long userId = userIds.get(i);
            executorService.submit(() -> {
                try {
                    issueCouponUseCase.issueCoupon(userId, couponId);
                } catch (Exception e) {
                    // 중복 체크 실패 가능
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(120, TimeUnit.SECONDS);
        executorService.shutdown();
        assertThat(completed).as("모든 스레드가 완료되어야 함").isTrue();

        // Kafka Consumer 처리 대기 (재고 소진까지)
        int maxWaitSeconds = 60;
        for (int i = 0; i < maxWaitSeconds; i++) {
            Thread.sleep(1000);
            Coupon coupon = couponRepository.findById(couponId).orElseThrow();
            if (coupon.getIssuedQuantity() >= COUPON_STOCK) {
                break;
            }
        }

        // then: 51번째 사용자는 Kafka 발행은 되지만 실제 발급은 실패
        issueCouponUseCase.issueCoupon(userIds.get(COUPON_STOCK), couponId);

        // Kafka Consumer가 처리하면서 재고 부족으로 실패 (발급 수량은 50 유지)
        Thread.sleep(3000);

        // 검증: 발급 수량이 총 수량과 같음 (50개)
        Coupon coupon = couponRepository.findById(couponId).orElseThrow();
        assertThat(coupon.getIssuedQuantity()).isEqualTo(COUPON_STOCK);
    }

    @Test
    @DisplayName("통합 테스트 - 만료된 쿠폰은 발급 불가")
    @org.springframework.transaction.annotation.Transactional
    void issueCoupon_expiredCoupon_shouldFail() throws InterruptedException {
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

        // when & then: Kafka 발행 전 만료 체크로 예외 발생
        assertThatThrownBy(() -> issueCouponUseCase.issueCoupon(userIds.get(0), expiredCouponId))
                .isInstanceOf(CouponException.class)
                .hasMessageContaining("만료된 쿠폰입니다");
    }

    @Test
    @DisplayName("통합 테스트 - 전체 발급 플로우 검증 (사용자 조회 → 쿠폰 검증 → Kafka 발행 → Consumer 처리 → 발급)")
    void issueCoupon_fullFlow_verification() throws InterruptedException {
        // given
        Long userId = userIds.get(0);

        // when: 쿠폰 발급 요청 (Kafka로 메시지 발행)
        issueCouponUseCase.issueCoupon(userId, couponId);

        // Kafka Consumer 처리 대기
        Thread.sleep(2000);

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
