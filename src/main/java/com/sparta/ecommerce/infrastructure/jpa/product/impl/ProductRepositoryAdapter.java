package com.sparta.ecommerce.infrastructure.jpa.product.impl;

import com.sparta.ecommerce.domain.product.ProductRepository;
import com.sparta.ecommerce.domain.product.ProductSortType;
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
    public List<Product> findAllById(Iterable<Long> productIds) {
        return jpaProductRepository.findAllById(productIds);
    }

    @Override
    public List<Product> findTopProducts(ProductSortType sortType, int limit) {
        return jpaProductRepository.findTopProducts(sortType, limit);
    }
}
