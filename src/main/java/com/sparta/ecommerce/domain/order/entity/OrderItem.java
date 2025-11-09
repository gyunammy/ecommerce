package com.sparta.ecommerce.domain.order.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {
    private Long          orderItemId; // 주문_상품_ID
    private Long          orderId;     // 주문_ID
    private Long          productId;   // 상품_ID
    private String        productName; // 상품명
    private String        description; // 상품설명
    private Integer       quantity;    // 수량
    private Integer       price;       // 단가
    private LocalDateTime createdAt;   // 등록일시
}
