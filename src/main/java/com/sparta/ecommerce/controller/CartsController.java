package com.sparta.ecommerce.controller;

import com.sparta.ecommerce.dto.response.CartItemResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@Tag(name = "Carts", description = "장바구니 관리")
@RestController
@RequestMapping("/carts")
public class CartsController {

    // 테스트 장바구니 데이터
    private static final List<CartItemResponse> TEST_CART_ITEMS = Arrays.asList(
            CartItemResponse.builder()
                    .cartItemId(1L)
                    .userId(1L)
                    .productId(1L)
                    .productName("무선 블루투스 이어폰")
                    .quantity(2)
                    .total_amount(178000)  // 89000 * 2
                    .createdAt("2024-01-25T10:30:00Z")
                    .updatedAt("2024-01-25T10:30:00Z")
                    .build(),
            CartItemResponse.builder()
                    .cartItemId(2L)
                    .userId(1L)
                    .productId(2L)
                    .productName("스마트워치")
                    .quantity(1)
                    .total_amount(250000)  // 250000 * 1
                    .createdAt("2024-01-26T11:00:00Z")
                    .updatedAt("2024-01-26T11:00:00Z")
                    .build()
    );

    @Operation(summary = "장바구니 목록 조회", description = "사용자의 장바구니 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<List<CartItemResponse>> getCarts() {
        return ResponseEntity.ok(TEST_CART_ITEMS);
    }

}
