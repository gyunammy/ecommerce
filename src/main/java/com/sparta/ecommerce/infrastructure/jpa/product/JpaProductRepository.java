package com.sparta.ecommerce.infrastructure.jpa.product;

import com.sparta.ecommerce.domain.product.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JpaProductRepository extends JpaRepository<Product, Long> {

    /**
     * JPA 구현체에서만 동시성 제어를 위해 비관적 락 사용
     * 도메인 레이어는 이를 알 필요 없음 (인프라 구현 세부사항)
     *
     * ORDER BY를 통해 항상 동일한 순서로 락을 획득하여 데드락 방지
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.productId IN :productIds ORDER BY p.productId ASC")
    List<Product> findAllByIdWithLock(@Param("productIds") Iterable<Long> productIds);

    /**
     * 조회수 기준 인기 상품 조회 (상위 N개)
     *
     * @param limit 조회할 상품 개수
     * @return 조회수 내림차순 상위 N개 상품
     */
    @Query("SELECT p FROM Product p ORDER BY p.viewCount DESC LIMIT :limit")
    List<Product> findTopProductsByViewCount(@Param("limit") int limit);
}
