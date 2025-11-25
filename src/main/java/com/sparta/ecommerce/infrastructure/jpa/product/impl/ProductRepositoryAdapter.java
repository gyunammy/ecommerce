package com.sparta.ecommerce.infrastructure.jpa.product.impl;

import com.sparta.ecommerce.domain.product.ProductRepository;
import com.sparta.ecommerce.domain.product.entity.Product;
import com.sparta.ecommerce.infrastructure.jpa.product.JpaProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryAdapter implements ProductRepository {
    private final JpaProductRepository jpaProductRepository;

    @Override
    public Product save(Product product) {
        return jpaProductRepository.save(product);
    }

    @Override
    public List<Product> findAll() {
        return jpaProductRepository.findAll();
    }

    @Override
    public List<Product> findAllByIdWithLock(Iterable<Long> productIds) {
        return jpaProductRepository.findAllByIdWithLock(productIds);
    }

    @Override
    public List<Product> findTopProductsByViewCount(int limit) {
        return jpaProductRepository.findTopProductsByViewCount(limit);
    }
}
