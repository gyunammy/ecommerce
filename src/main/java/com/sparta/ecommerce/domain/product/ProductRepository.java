package com.sparta.ecommerce.domain.product;

import com.sparta.ecommerce.domain.product.entity.Product;

import java.util.List;

public interface ProductRepository {
    List<Product> findAll();
    List<Product> findAllById(Iterable<Long> productIds);
    List<Product> findAllByIdWithLock(Iterable<Long> productIds);
    List<Product> findTopProductsByViewCount(int limit);

    Product save(Product product);
}
