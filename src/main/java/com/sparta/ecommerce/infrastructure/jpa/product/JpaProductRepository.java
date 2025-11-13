package com.sparta.ecommerce.infrastructure.jpa.product;

import com.sparta.ecommerce.domain.product.ProductSortType;
import com.sparta.ecommerce.domain.product.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Primary
@Repository
public interface JpaProductRepository extends JpaRepository<Product, Long> {

    /**
     * JPA 구현체에서만 동시성 제어를 위해 비관적 락 사용
     * 도메인 레이어는 이를 알 필요 없음 (인프라 구현 세부사항)
     */
    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.productId IN :productIds")
    List<Product> findAllById(@Param("productIds") Iterable<Long> productIds);

    /**
     * 인기 상품 조회 (조회수 또는 최신순)
     */
    default List<Product> findTopProducts(ProductSortType sortType, int limit) {
        Sort sort;
        if (sortType == ProductSortType.VIEW_COUNT) {
            sort = Sort.by(Sort.Direction.DESC, "viewCount");
        } else {
            sort = Sort.by(Sort.Direction.DESC, "createdAt");
        }
        return findAll(PageRequest.of(0, limit, sort)).getContent();
    }
}
