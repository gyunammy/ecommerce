package com.sparta.ecommerce.presentation.order.controller;

import com.sparta.ecommerce.application.order.CreateOrderUseCase;
import com.sparta.ecommerce.domain.order.dto.OrderRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final CreateOrderUseCase CreateOrderUseCase;

    /**
     * 주문 요청
     * @return
     */
    @PostMapping
    public ResponseEntity<Object> createOrder(@RequestBody OrderRequest orderRequest) {
        CreateOrderUseCase.createOrder(orderRequest.userId(), orderRequest.userCouponId());
        return ResponseEntity.ok(Map.of("message", "주문이 완료되었습니다."));
    }

}
