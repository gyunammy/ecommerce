package com.sparta.ecommerce.controller;

import com.sparta.ecommerce.dto.response.ProductResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@Tag(name = "Products", description = "상품 관리")
@RestController
@RequestMapping("/products")
public class ProductsController {

    // 테스트 상품 데이터
    private static final ProductResponse TEST_PRODUCT_1 = ProductResponse.builder()
            .productId(1L)
            .productName("무선 블루투스 이어폰")
            .description("고음질 노이즈 캔슬링 기능이 탑재된 프리미엄 무선 이어폰")
            .price(89000)
            .quantity(150)
            .createdAt("2024-01-10T10:00:00Z")
            .updatedAt("2024-01-10T10:00:00Z")
            .build();

    private static final ProductResponse TEST_PRODUCT_2 = ProductResponse.builder()
            .productId(2L)
            .productName("스마트워치")
            .description("건강 관리 기능이 있는 스마트워치")
            .price(250000)
            .quantity(80)
            .createdAt("2024-01-15T09:00:00Z")
            .updatedAt("2024-01-15T09:00:00Z")
            .build();

    private static final ProductResponse TEST_PRODUCT_3 = ProductResponse.builder()
            .productId(3L)
            .productName("노트북 스탠드")
            .description("각도 조절이 가능한 알루미늄 노트북 거치대")
            .price(35000)
            .quantity(200)
            .createdAt("2024-01-20T14:00:00Z")
            .updatedAt("2024-01-20T14:00:00Z")
            .build();

    private static final List<ProductResponse> TEST_PRODUCTS = Arrays.asList(
            TEST_PRODUCT_1,
            TEST_PRODUCT_2,
            TEST_PRODUCT_3
    );

    @Operation(summary = "상품 목록 조회", description = "전체 상품 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getProducts() {
        return ResponseEntity.ok(TEST_PRODUCTS);
    }

    @Operation(summary = "상품 상세 조회", description = "특정 상품의 상세 정보를 조회합니다.")
    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> getProduct(
            @Parameter(description = "상품 ID", required = true, example = "1")
            @PathVariable Long productId) {
        // 실제로는 productId로 조회하지만, 테스트를 위해 첫 번째 상품 반환
        ProductResponse product = ProductResponse.builder()
                .productId(productId)
                .productName("무선 블루투스 이어폰")
                .description("고음질 노이즈 캔슬링 기능이 탑재된 프리미엄 무선 이어폰")
                .price(89000)
                .quantity(150)
                .createdAt("2024-01-10T10:00:00Z")
                .updatedAt("2024-01-10T10:00:00Z")
                .build();
        return ResponseEntity.ok(product);
    }
}
