package com.sparta.ecommerce.domain.product;

import com.sparta.ecommerce.domain.product.entity.Product;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    List<Product> findAll();
    List<Product> findAllById(Iterable<Long> productIds);
    Optional<Product> findById(Long productId);
    List<Product> findTopProducts(ProductSortType sortType, int limit);
    Product save(Product product);
}
