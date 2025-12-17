package com.sparta.ecommerce.integration;

import com.sparta.ecommerce.application.order.CreateOrderUseCase;
import com.sparta.ecommerce.domain.cart.CartRepository;
import com.sparta.ecommerce.domain.cart.entity.CartItem;
import com.sparta.ecommerce.domain.product.ProductRankingRepository;
import com.sparta.ecommerce.domain.product.entity.Product;
import com.sparta.ecommerce.domain.user.UserRepository;
import com.sparta.ecommerce.domain.user.entity.User;
import com.sparta.ecommerce.infrastructure.jpa.product.JpaProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 상품 판매량 랭킹 통합 테스트
 *
 * Redis를 사용한 판매량 랭킹 집계 및 조회를 검증합니다.
 */
@SpringBootTest(properties = {
    "spring.task.scheduling.enabled=false",  // 테스트 시 스케줄러 비활성화
    "app.async.enabled=false",  // 테스트 시 비동기 작업을 동기로 실행
    "coupon.queue.consumer.enabled=false"  // 쿠폰 발급 Queue Consumer 비활성화
})
@Testcontainers
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ProductRankingIntegrationTest {

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
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);

        // Redis 연결 풀 증가
        registry.add("spring.data.redis.lettuce.pool.max-active", () -> "50");
        registry.add("spring.data.redis.lettuce.pool.max-idle", () -> "50");
        registry.add("spring.data.redis.lettuce.pool.min-idle", () -> "10");
    }

    @Autowired
    private CreateOrderUseCase createOrderUseCase;

    @Autowired
    private ProductRankingRepository productRankingRepository;

    @Autowired
    private JpaProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    private Long product1Id;
    private Long product2Id;
    private Long product3Id;

    @BeforeEach
    void setUp() {
        // 기존 데이터 삭제
        try {
            entityManager.createQuery("DELETE FROM OrderItem").executeUpdate();
            entityManager.createQuery("DELETE FROM Order").executeUpdate();
            entityManager.createQuery("DELETE FROM CartItem").executeUpdate();
            entityManager.createQuery("DELETE FROM Product").executeUpdate();
            entityManager.createQuery("DELETE FROM User").executeUpdate();
        } catch (Exception e) {
            // 첫 실행 시 데이터가 없을 수 있음
        }

        // Redis 데이터 삭제
        productRankingRepository.clearAll();

        LocalDateTime now = LocalDateTime.now();

        // 상품 3개 생성
        Product product1 = productRepository.save(new Product(null, "상품A", "설명A", 100, 10000, 0, now, now));
        Product product2 = productRepository.save(new Product(null, "상품B", "설명B", 100, 20000, 0, now, now));
        Product product3 = productRepository.save(new Product(null, "상품C", "설명C", 100, 30000, 0, now, now));

        product1Id = product1.getProductId();
        product2Id = product2.getProductId();
        product3Id = product3.getProductId();

        // 사용자 3명 생성
        userRepository.save(new User(1L, "user1", 1000000, 0L, now));
        userRepository.save(new User(2L, "user2", 1000000, 0L, now));
        userRepository.save(new User(3L, "user3", 1000000, 0L, now));
    }

    @Test
    @DisplayName("통합 테스트 - 주문 시 판매량 랭킹이 Redis에 정확히 집계된다")
    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void productRanking_shouldBeUpdatedCorrectly() throws InterruptedException {
        // given: 각 사용자가 다른 상품을 다른 수량으로 주문
        LocalDateTime now = LocalDateTime.now();

        // User1 → 상품A 5개 주문
        cartRepository.save(new CartItem(1L, 1L, product1Id, 5, now, now));
        createOrderUseCase.createOrder(1L, null);

        // User2 → 상품B 3개 주문
        cartRepository.save(new CartItem(2L, 2L, product2Id, 3, now, now));
        createOrderUseCase.createOrder(2L, null);

        // User3 → 상품C 10개 주문
        cartRepository.save(new CartItem(3L, 3L, product3Id, 10, now, now));
        createOrderUseCase.createOrder(3L, null);

        // Kafka 이벤트 처리를 위한 대기 - polling으로 랭킹 데이터 확인
        long waitStart = System.currentTimeMillis();
        long maxWaitTime = 15000; // 15초
        boolean rankingUpdated = false;

        while (System.currentTimeMillis() - waitStart < maxWaitTime) {
            Integer count = productRankingRepository.getSalesCount(product3Id);
            if (count != null && count > 0) {
                rankingUpdated = true;
                break;
            }
            Thread.sleep(200);
        }

        // when: 상위 3개 랭킹 조회
        Map<Long, Integer> topRankings = productRankingRepository.getTopRankings(3);

        // then: 판매량 순으로 정렬되어 있어야 함
        assertThat(topRankings).hasSize(3);
        assertThat(topRankings.keySet().stream().toList())
                .containsExactly(product3Id, product1Id, product2Id);  // 10개, 5개, 3개 순

        // 각 상품의 판매량 확인
        assertThat(productRankingRepository.getSalesCount(product1Id)).isEqualTo(5);
        assertThat(productRankingRepository.getSalesCount(product2Id)).isEqualTo(3);
        assertThat(productRankingRepository.getSalesCount(product3Id)).isEqualTo(10);
    }

    @Test
    @DisplayName("통합 테스트 - 여러 번 주문 시 판매량이 누적된다")
    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void productRanking_shouldAccumulateSales() throws InterruptedException {
        // given: 같은 상품을 여러 번 주문
        LocalDateTime now = LocalDateTime.now();

        // User1 → 상품A 2개 주문
        cartRepository.save(new CartItem(1L, 1L, product1Id, 2, now, now));
        createOrderUseCase.createOrder(1L, null);

        // User2 → 상품A 3개 주문
        cartRepository.save(new CartItem(2L, 2L, product1Id, 3, now, now));
        createOrderUseCase.createOrder(2L, null);

        // Kafka 이벤트 처리를 위한 대기 - polling으로 랭킹 데이터 확인
        long waitStart = System.currentTimeMillis();
        long maxWaitTime = 15000; // 15초
        boolean rankingUpdated = false;

        while (System.currentTimeMillis() - waitStart < maxWaitTime) {
            Integer count = productRankingRepository.getSalesCount(product1Id);
            if (count != null && count >= 5) {
                rankingUpdated = true;
                break;
            }
            Thread.sleep(200);
        }

        // when: 판매량 조회
        Integer totalSales = productRankingRepository.getSalesCount(product1Id);

        // then: 총 5개 판매되어야 함
        assertThat(totalSales).isEqualTo(5);
    }

    @Test
    @DisplayName("통합 테스트 - 판매 이력이 없는 상품은 0을 반환한다")
    void productRanking_noSales_shouldReturnZero() {
        // when: 주문하지 않은 상품의 판매량 조회
        Integer salesCount = productRankingRepository.getSalesCount(product1Id);

        // then: 0이어야 함
        assertThat(salesCount).isEqualTo(0);
    }
}
