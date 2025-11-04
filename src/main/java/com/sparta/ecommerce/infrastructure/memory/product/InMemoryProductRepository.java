package com.sparta.ecommerce.infrastructure.memory.product;

import com.sparta.ecommerce.domain.product.Product;
import com.sparta.ecommerce.domain.product.ProductRepository;
import com.sparta.ecommerce.domain.product.ProductSortType;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Repository
public class InMemoryProductRepository implements ProductRepository {

    private final Map<Long, Product> table = new LinkedHashMap<>();
    private long cursor = 1;

    @PostConstruct
    public void init() {
        LocalDateTime now = LocalDateTime.now();
        table.put(cursor, new Product(cursor++, "노트북", "고성능 노트북", 10, 1500000, 1500, now, now));
        table.put(cursor, new Product(cursor++, "마우스", "무선 마우스",   50, 30000, 3200, now, now));
        table.put(cursor, new Product(cursor++, "키보드", "기계식 키보드", 30, 120000, 2100, now, now));
    }

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

}
