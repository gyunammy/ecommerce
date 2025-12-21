package com.sparta.ecommerce.integration;

import com.sparta.ecommerce.application.cart.CartService;
import com.sparta.ecommerce.application.order.CreateOrderUseCase;
import com.sparta.ecommerce.application.order.consumer.OrderCreatedEventConsumer;
import com.sparta.ecommerce.application.order.event.OrderCreatedEvent;
import com.sparta.ecommerce.domain.cart.CartRepository;
import com.sparta.ecommerce.domain.cart.entity.CartItem;
import com.sparta.ecommerce.domain.order.OrderRepository;
import com.sparta.ecommerce.domain.order.entity.Order;
import com.sparta.ecommerce.domain.product.entity.Product;
import com.sparta.ecommerce.domain.user.UserRepository;
import com.sparta.ecommerce.domain.user.entity.User;
import com.sparta.ecommerce.infrastructure.jpa.product.JpaProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

/**
 * Kafka 메시지 발행 및 컨슈머 수신 통합 테스트
 *
 * 주문 생성 시 Kafka로 메시지가 올바르게 발행되고,
 * 컨슈머가 메시지를 수신하여 처리하는지 검증합니다.
 */
@SpringBootTest(properties = {
        "spring.task.scheduling.enabled=false",
        "coupon.queue.consumer.enabled=false",
        "app.async.enabled=false",
        "logging.level.com.sparta.ecommerce.application.order=INFO",
        "logging.level.com.sparta.ecommerce.application.order.consumer=INFO"
})
@Testcontainers
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class KafkaMessageFlowTest {

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
    private OrderRepository orderRepository;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @SpyBean
    private KafkaTemplate<String, OrderCreatedEvent> orderCreatedKafkaTemplate;

    @SpyBean
    private OrderCreatedEventConsumer orderCreatedEventConsumer;

    private Long userId;
    private Long productId;

    @BeforeEach
    @org.springframework.transaction.annotation.Transactional
    void setUp() {
        // 기존 데이터 삭제
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

        LocalDateTime now = LocalDateTime.now();

        // 테스트 상품 생성
        Product product = new Product(
                null,
                "테스트 상품",
                "Kafka 테스트용 상품",
                100,
                10000,
                0,
                now,
                now
        );
        Product savedProduct = productRepository.save(product);
        productId = savedProduct.getProductId();

        // 테스트 사용자 생성
        User user = new User(null, "testUser", 1000000, 0L, now);
        User savedUser = userRepository.save(user);
        userId = savedUser.getUserId();

        // 장바구니에 상품 추가
        CartItem cartItem = new CartItem(null, userId, productId, 1, now, now);
        cartRepository.save(cartItem);
    }

    @Test
    @DisplayName("주문 생성 시 Kafka 메시지가 발행되고 컨슈머가 수신한다")
    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void shouldPublishAndConsumeOrderCreatedEvent() throws InterruptedException {
        // when: 주문 생성
        createOrderUseCase.createOrder(userId, null);

        // then: Kafka 메시지 발행 확인
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<OrderCreatedEvent> eventCaptor = ArgumentCaptor.forClass(OrderCreatedEvent.class);

        // Kafka 메시지가 발행되었는지 확인
        await()
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verify(orderCreatedKafkaTemplate).send(topicCaptor.capture(), eventCaptor.capture());
                });

        // 발행된 메시지 검증
        String capturedTopic = topicCaptor.getValue();
        OrderCreatedEvent capturedEvent = eventCaptor.getValue();

        System.out.println("\n=== Kafka Producer 검증 ===");
        System.out.println("발행된 토픽: " + capturedTopic);
        System.out.println("발행된 이벤트 - userId: " + capturedEvent.userId() + ", orderId: " + capturedEvent.orderId());

        assertThat(capturedTopic).isEqualTo("order-created-topic");
        assertThat(capturedEvent.userId()).isEqualTo(userId);
        assertThat(capturedEvent.finalAmount()).isEqualTo(10000);
        assertThat(capturedEvent.cartItems()).hasSize(1);

        // Consumer가 메시지를 수신했는지 확인
        await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verify(orderCreatedEventConsumer, atLeastOnce()).consumeOrderCreatedEvent(any(OrderCreatedEvent.class));
                });

        System.out.println("\n=== Kafka Consumer 검증 ===");
        System.out.println("Consumer가 메시지를 수신했습니다.");

        // 컨슈머가 받은 이벤트 검증
        ArgumentCaptor<OrderCreatedEvent> consumerEventCaptor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(orderCreatedEventConsumer, atLeastOnce()).consumeOrderCreatedEvent(consumerEventCaptor.capture());

        OrderCreatedEvent consumedEvent = consumerEventCaptor.getValue();
        System.out.println("수신된 이벤트 - userId: " + consumedEvent.userId() + ", orderId: " + consumedEvent.orderId());

        assertThat(consumedEvent.userId()).isEqualTo(userId);
        assertThat(consumedEvent.finalAmount()).isEqualTo(10000);

        // 이벤트 처리 결과 확인 (재고 차감, 포인트 차감 등)
        await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Product product = productRepository.findById(productId).orElseThrow();
                    assertThat(product.getQuantity()).isLessThan(100); // 재고가 차감되었는지 확인
                });

        System.out.println("\n=== 이벤트 처리 결과 검증 ===");
        Product finalProduct = productRepository.findById(productId).orElseThrow();
        System.out.println("재고 차감 확인 - 초기: 100, 최종: " + finalProduct.getQuantity());

        User finalUser = userRepository.findById(userId).orElseThrow();
        System.out.println("포인트 차감 확인 - 초기: 1000000, 최종: " + finalUser.getPoint());

        assertThat(finalProduct.getQuantity()).isEqualTo(99);
        assertThat(finalUser.getPoint()).isLessThan(1000000);

        System.out.println("\n✅ Kafka 메시지 발행 및 컨슈머 수신 검증 완료!");
    }

    @Test
    @DisplayName("여러 주문 생성 시 모든 Kafka 메시지가 순서대로 처리된다")
    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void shouldProcessMultipleOrdersInSequence() throws InterruptedException {
        // given: 추가 사용자와 장바구니 생성
        LocalDateTime now = LocalDateTime.now();
        User user2 = new User(null, "testUser2", 1000000, 0L, now);
        User savedUser2 = userRepository.save(user2);
        Long userId2 = savedUser2.getUserId();

        CartItem cartItem2 = new CartItem(null, userId2, productId, 2, now, now);
        cartRepository.save(cartItem2);

        int initialOrderCount = orderRepository.findAll().size();

        // when: 두 명의 사용자가 주문 생성
        createOrderUseCase.createOrder(userId, null);
        createOrderUseCase.createOrder(userId2, null);

        // then: 모든 메시지가 발행되었는지 확인
        await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verify(orderCreatedKafkaTemplate, org.mockito.Mockito.times(2))
                            .send(eq("order-created-topic"), any(OrderCreatedEvent.class));
                });

        System.out.println("\n=== 다중 주문 Kafka 메시지 발행 검증 ===");
        System.out.println("2개의 주문에 대한 Kafka 메시지가 모두 발행되었습니다.");

        // 모든 메시지가 컨슈머에서 처리되었는지 확인
        await()
                .atMost(15, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verify(orderCreatedEventConsumer, org.mockito.Mockito.times(2))
                            .consumeOrderCreatedEvent(any(OrderCreatedEvent.class));
                });

        System.out.println("2개의 주문에 대한 Kafka 메시지가 모두 Consumer에서 수신되었습니다.");

        // 주문이 모두 생성되었는지 확인
        await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    int finalOrderCount = orderRepository.findAll().size();
                    assertThat(finalOrderCount).isEqualTo(initialOrderCount + 2);
                });

        System.out.println("\n✅ 다중 주문 Kafka 메시지 처리 검증 완료!");
    }
}
