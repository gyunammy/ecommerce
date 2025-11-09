package com.sparta.ecommerce.domain.product;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    List<Product> findAll();
    List<Product> findAllById(List<Long> productIds);
    Optional<Product> findById(Long productId);
    void update(Product product);
    List<Product> findTopProducts(ProductSortType sortType, int limit);
    Product save(Product product);
}
