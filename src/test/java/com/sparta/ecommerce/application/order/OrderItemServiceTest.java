package com.sparta.ecommerce.application.order;

import com.sparta.ecommerce.domain.coupon.dto.ProductResponse;
import com.sparta.ecommerce.domain.order.entity.OrderItem;
import com.sparta.ecommerce.domain.product.entity.Product;
import com.sparta.ecommerce.infrastructure.jpa.order.JpaOrderItemRepository;
import com.sparta.ecommerce.infrastructure.jpa.product.JpaProductRepository;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OrderItemService 테스트
 * TestContainers를 사용하여 실제 MySQL 환경에서 테스트
 */
@SpringBootTest
@Testcontainers
@org.springframework.transaction.annotation.Transactional
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("OrderItemService 테스트")
class OrderItemServiceTest {

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
    private JpaOrderItemRepository orderItemRepository;

    @Autowired
    private JpaProductRepository productRepository;

    @Autowired
    private OrderItemService orderItemService;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    private Long productId1; // 노트북
    private Long productId2; // 마우스
    private Long productId3; // 키보드

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
            entityManager.flush();
        } catch (Exception e) {
            // 첫 실행 시 데이터가 없을 수 있음
        }

        // Product 데이터 초기화
        LocalDateTime now = LocalDateTime.now();
        Product product1 = new Product(null, "노트북", "고성능 노트북", 10, 1500000, 1500, now, now);
        Product product2 = new Product(null, "마우스", "무선 마우스", 50, 30000, 3200, now, now);
        Product product3 = new Product(null, "키보드", "기계식 키보드", 30, 120000, 2100, now, now);

        Product saved1 = productRepository.save(product1);
        Product saved2 = productRepository.save(product2);
        Product saved3 = productRepository.save(product3);

        productId1 = saved1.getProductId();
        productId2 = saved2.getProductId();
        productId3 = saved3.getProductId();

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("상품별 판매량 조회 - 실제 주문 데이터 기반")
    void getSoldCountByProductId() {
        // given
        LocalDateTime now = LocalDateTime.now();

        // 노트북: 총 15개 판매 (주문 2개)
        OrderItem item1_1 = new OrderItem(null, 1L, productId1, "노트북", "고성능 노트북", 10, 1500000, now);
        OrderItem item1_2 = new OrderItem(null, 2L, productId1, "노트북", "고성능 노트북", 5, 1500000, now);

        // 마우스: 총 30개 판매 (주문 3개)
        OrderItem item2_1 = new OrderItem(null, 1L, productId2, "마우스", "무선 마우스", 10, 30000, now);
        OrderItem item2_2 = new OrderItem(null, 2L, productId2, "마우스", "무선 마우스", 10, 30000, now);
        OrderItem item2_3 = new OrderItem(null, 3L, productId2, "마우스", "무선 마우스", 10, 30000, now);

        // 키보드: 총 8개 판매 (주문 1개)
        OrderItem item3_1 = new OrderItem(null, 3L, productId3, "키보드", "기계식 키보드", 8, 120000, now);

        orderItemRepository.saveAll(Arrays.asList(item1_1, item1_2, item2_1, item2_2, item2_3, item3_1));

        // when
        Map<Long, Integer> result = orderItemService.getSoldCountByProductId();

        // then
        assertThat(result.get(productId1)).isEqualTo(15);  // 노트북: 10 + 5 = 15개
        assertThat(result.get(productId2)).isEqualTo(30);  // 마우스: 10 + 10 + 10 = 30개
        assertThat(result.get(productId3)).isEqualTo(8);   // 키보드: 8개
    }

    @Test
    @DisplayName("판매량 기준 인기 상품 조회 - 판매량 순서대로 정렬")
    void findTopProductsBySoldCount() {
        // given
        LocalDateTime now = LocalDateTime.now();

        // 노트북: 50개 판매
        OrderItem item1 = new OrderItem(null, 1L, productId1, "노트북", "고성능 노트북", 50, 1500000, now);

        // 마우스: 100개 판매 (1위)
        OrderItem item2_1 = new OrderItem(null, 2L, productId2, "마우스", "무선 마우스", 60, 30000, now);
        OrderItem item2_2 = new OrderItem(null, 3L, productId2, "마우스", "무선 마우스", 40, 30000, now);

        // 키보드: 20개 판매
        OrderItem item3 = new OrderItem(null, 4L, productId3, "키보드", "기계식 키보드", 20, 120000, now);

        orderItemRepository.saveAll(Arrays.asList(item1, item2_1, item2_2, item3));

        // when
        List<ProductResponse> result = orderItemService.findTopProductsBySoldCount(3);

        // then
        assertThat(result).hasSize(3);
        // 판매량 순: 마우스(100개) > 노트북(50개) > 키보드(20개)
        assertThat(result.get(0).productId()).isEqualTo(productId2);
        assertThat(result.get(0).productName()).isEqualTo("마우스");

        assertThat(result.get(1).productId()).isEqualTo(productId1);
        assertThat(result.get(1).productName()).isEqualTo("노트북");

        assertThat(result.get(2).productId()).isEqualTo(productId3);
        assertThat(result.get(2).productName()).isEqualTo("키보드");
    }

    @Test
    @DisplayName("판매량 기준 인기 상품 조회 - limit 적용")
    void findTopProductsBySoldCount_withLimit() {
        // given
        LocalDateTime now = LocalDateTime.now();

        OrderItem item1 = new OrderItem(null, 1L, productId1, "노트북", "고성능 노트북", 80, 1500000, now);
        OrderItem item2 = new OrderItem(null, 2L, productId2, "마우스", "무선 마우스", 150, 30000, now);
        OrderItem item3 = new OrderItem(null, 3L, productId3, "키보드", "기계식 키보드", 50, 120000, now);

        orderItemRepository.saveAll(Arrays.asList(item1, item2, item3));

        // when - 상위 2개만 조회
        List<ProductResponse> result = orderItemService.findTopProductsBySoldCount(2);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).productId()).isEqualTo(productId2);  // 마우스 150개
        assertThat(result.get(1).productId()).isEqualTo(productId1);  // 노트북 80개
        // 키보드(50개)는 포함되지 않음
    }

    @Test
    @DisplayName("판매량 기준 인기 상품 조회 - 판매 이력이 있는 상품만 조회")
    void findTopProductsBySoldCount_onlySoldProducts() {
        // given
        LocalDateTime now = LocalDateTime.now();

        // 노트북과 마우스만 판매
        OrderItem item1 = new OrderItem(null, 1L, productId1, "노트북", "고성능 노트북", 100, 1500000, now);
        OrderItem item2 = new OrderItem(null, 2L, productId2, "마우스", "무선 마우스", 50, 30000, now);

        orderItemRepository.saveAll(Arrays.asList(item1, item2));

        // when
        List<ProductResponse> result = orderItemService.findTopProductsBySoldCount(10);

        // then - 판매 이력이 있는 상품만 조회됨 (2개)
        assertThat(result).hasSize(2);
        // 노트북과 마우스만 조회되어야 함
        assertThat(result.stream().anyMatch(p -> p.productId().equals(productId1))).isTrue();
        assertThat(result.stream().anyMatch(p -> p.productId().equals(productId2))).isTrue();
        // 키보드는 판매 이력이 없으므로 조회되지 않음
        assertThat(result.stream().noneMatch(p -> p.productId().equals(productId3))).isTrue();
    }
}
