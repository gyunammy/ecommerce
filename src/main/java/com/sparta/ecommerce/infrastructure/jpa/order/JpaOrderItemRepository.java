package com.sparta.ecommerce.infrastructure.jpa.order;

import com.sparta.ecommerce.domain.coupon.dto.ProductResponse;
import com.sparta.ecommerce.domain.order.entity.OrderItem;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public interface JpaOrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("SELECT oi.productId as productId, SUM(oi.quantity) as soldCount FROM OrderItem oi GROUP BY oi.productId")
    List<ProductSoldCount> findSoldCountByProductId();

    default Map<Long, Integer> getSoldCountByProductId() {
        return findSoldCountByProductId().stream()
                .collect(Collectors.toMap(
                        ProductSoldCount::getProductId,
                        psc -> psc.getSoldCount().intValue()
                ));
    }

    @Query("""
                SELECT
                    p.productId,
                    p.productName,
                    p.description,
                    p.quantity,
                    p.price,
                    p.viewCount,
                    p.createdAt,
                    p.updateAt,
                    SUM(oi.quantity) AS soldCount
                FROM OrderItem oi
                JOIN `Order` o ON oi.orderId = o.orderId
                JOIN Product p ON oi.productId = p.productId
                WHERE o.status = 'COMPLETED'
                GROUP BY p.productId
                ORDER BY soldCount DESC
                LIMIT :limit
            """)
    List<ProductWithSoldCount> findTopProductsWithSoldCount(@Param("limit") int limit);

    default List<ProductResponse> findTopProductsBySoldCount(int limit) {
        return findTopProductsWithSoldCount(limit).stream()
                .map(p -> new ProductResponse(
                        p.getProductId(),
                        p.getProductName(),
                        p.getDescription(),
                        p.getQuantity(),
                        p.getPrice(),
                        p.getViewCount(),
                        p.getCreatedAt(),
                        p.getUpdateAt()
                ))
                .collect(Collectors.toList());
    }

    interface ProductSoldCount {
        Long getProductId();
        Long getSoldCount();
    }

    interface ProductWithSoldCount {
        Long getProductId();
        String getProductName();
        String getDescription();
        Integer getQuantity();
        Integer getPrice();
        Integer getViewCount();
        LocalDateTime getCreatedAt();
        LocalDateTime getUpdateAt();
        Long getSoldCount();
    }
}
