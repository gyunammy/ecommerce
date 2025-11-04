package com.sparta.ecommerce.presentation.product.controller;

import com.sparta.ecommerce.application.product.GetTopProductsUseCase;
import com.sparta.ecommerce.application.product.ProductService;
import com.sparta.ecommerce.domain.coupon.dto.ProductResponse;
import com.sparta.ecommerce.domain.product.ProductSortType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final GetTopProductsUseCase getTopProductsUseCase;

    /**
     * 상품 목록 조회
     * @return
     */
    @GetMapping
    public ResponseEntity<List<ProductResponse>> fetchProducts() {
        return ResponseEntity.ok(productService.findAll());
    }

    /**
     * 인기 상품 조회
     * @param sortType 정렬 기준 (VIEW_COUNT: 조회수, SOLD_COUNT: 판매량)
     * @return 인기 상품 목록 (상위 10개)
     */
    @GetMapping("/top")
    public ResponseEntity<List<ProductResponse>> fetchTopProducts(
            @RequestParam(name = "sortType") ProductSortType sortType
    ) {
        return ResponseEntity.ok(getTopProductsUseCase.getTopProducts(sortType));
    }

}
