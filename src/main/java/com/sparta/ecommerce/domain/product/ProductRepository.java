package com.sparta.ecommerce.domain.product;

import com.sparta.ecommerce.domain.product.entity.Product;

import java.util.List;

public interface ProductRepository {
    List<Product> findAll();
    List<Product> findAllByIdWithLock(Iterable<Long> productIds);
    List<Product> findTopProducts(ProductSortType sortType, int limit);

    Product save(Product product);
}
