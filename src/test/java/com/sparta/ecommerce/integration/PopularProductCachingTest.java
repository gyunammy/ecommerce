package com.sparta.ecommerce.integration;

import com.sparta.ecommerce.application.product.GetTopProductsUseCase;
import com.sparta.ecommerce.domain.coupon.dto.ProductResponse;
import com.sparta.ecommerce.domain.product.ProductSortType;
import com.sparta.ecommerce.infrastructure.jpa.product.JpaProductRepository;
import com.sparta.ecommerce.domain.product.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 인기 상품 조회 캐싱 성능 테스트
 *
 * Redis 캐시를 사용한 성능 개선을 검증
 */
@SpringBootTest
@Testcontainers
public class PopularProductCachingTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(
            DockerImageName.parse("redis:7.2-alpine")
    ).withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        // MySQL 설정
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);

        // Redis 설정
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private GetTopProductsUseCase getTopProductsUseCase;

    @Autowired
    private JpaProductRepository productRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setup() {
        // 캐시 비우기
        cacheManager.getCacheNames().forEach(cacheName -> {
            cacheManager.getCache(cacheName).clear();
        });

        // 기존 데이터 삭제
        productRepository.deleteAll();

        // 테스트 데이터 생성 (15개 상품)
        for (int i = 1; i <= 15; i++) {
            LocalDateTime now = LocalDateTime.now();
            Product product = new Product(
                    null,
                    "인기상품 " + i + "위",
                    "상품 설명 " + i,
                    100,
                    10000 * i,
                    1600 - (i * 100), // 조회수: 1500, 1400, 1300...
                    now,
                    now
            );
            productRepository.save(product);
        }
    }

    @Test
    @DisplayName("인기상품 조회 - 캐시 적용 전후 성능 차이 확인")
    void 인기상품조회_캐시적용전_후_성능차이_확인() {
        // --- 1회차: 캐시 미적용 (DB 조회 발생) ---
        long start1 = System.currentTimeMillis();
        List<ProductResponse> firstCall = getTopProductsUseCase.getTopProducts(ProductSortType.VIEW_COUNT);
        long end1 = System.currentTimeMillis();
        long firstCallTime = end1 - start1;

        // --- 2회차: 캐시 적용 (Redis hit) ---
        long start2 = System.currentTimeMillis();
        List<ProductResponse> secondCall = getTopProductsUseCase.getTopProducts(ProductSortType.VIEW_COUNT);
        long end2 = System.currentTimeMillis();
        long secondCallTime = end2 - start2;

        // --- 3회차: 캐시 적용 (Redis hit) ---
        long start3 = System.currentTimeMillis();
        List<ProductResponse> thirdCall = getTopProductsUseCase.getTopProducts(ProductSortType.VIEW_COUNT);
        long end3 = System.currentTimeMillis();
        long thirdCallTime = end3 - start3;

        System.out.println("========== 캐싱 성능 테스트 결과 ==========");
        System.out.println("첫 번째 호출 (DB 조회): " + firstCallTime + "ms");
        System.out.println("두 번째 호출 (캐시 hit): " + secondCallTime + "ms");
        System.out.println("세 번째 호출 (캐시 hit): " + thirdCallTime + "ms");
        System.out.println("성능 개선: " + String.format("%.1f배", (double) firstCallTime / secondCallTime));
        System.out.println("==========================================");

        // 결과 검증
        assertThat(firstCall).hasSize(10);
        assertThat(secondCall).hasSize(10);
        assertThat(thirdCall).hasSize(10);

        // 데이터 일치 확인
        assertThat(firstCall).isEqualTo(secondCall);
        assertThat(secondCall).isEqualTo(thirdCall);

        // 성능 개선 확인 - 캐시 조회가 DB 조회보다 빨라야 함
        // 첫 번째 호출이 두 번째/세 번째보다 느려야 함
        assertThat(firstCallTime).isGreaterThan(secondCallTime);
        assertThat(firstCallTime).isGreaterThan(thirdCallTime);
    }

}
