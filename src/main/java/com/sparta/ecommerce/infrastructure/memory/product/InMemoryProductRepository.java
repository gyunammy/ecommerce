package com.sparta.ecommerce.infrastructure.memory.product;

import com.sparta.ecommerce.domain.product.Product;
import com.sparta.ecommerce.domain.product.ProductRepository;
import com.sparta.ecommerce.domain.product.ProductSortType;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class InMemoryProductRepository implements ProductRepository {

    private final Map<Long, Product> table = new ConcurrentHashMap<>();
    private final AtomicLong cursor = new AtomicLong(1);

    @Override
    public List<Product> findAll() {
        return new ArrayList<>(table.values());
    }

    @Override
    public List<Product> findAllById(List<Long> productIds) {
        return productIds.stream()
                .map(table::get)
                .filter(product -> product != null)
                .toList();
    }

    @Override
    public java.util.Optional<Product> findById(Long productId) {
        return java.util.Optional.ofNullable(table.get(productId));
    }

    @Override
    public void update(Product product) {
        table.put(product.getProductId(), product);
    }

    @Override
    public List<Product> findTopProducts(ProductSortType sortType, int limit) {
        // ProductRepository는 Product 도메인만 관리
        // 조회수 기준 정렬만 지원 (판매량은 Application 레이어에서 처리)
        return table.values().stream()
                .sorted(Comparator.comparing(Product::getViewCount).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public Product save(Product product) {
        if (product.getProductId() == null) {
            // ID가 없으면 자동 생성
            Long newId = cursor.getAndIncrement();
            Product newProduct = new Product(
                    newId,
                    product.getProductName(),
                    product.getDescription(),
                    product.getQuantity(),
                    product.getPrice(),
                    product.getViewCount(),
                    product.getCreatedAt(),
                    product.getUpdateAt()
            );
            table.put(newId, newProduct);
            return newProduct;
        } else {
            table.put(product.getProductId(), product);
            return product;
        }
    }

}
