package com.sparta.ecommerce.domain.order.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long          orderId;        // 주문_ID
    private Long          userId;         // 사용자_ID
    private Long          userCouponId;   // 사용자_쿠폰_ID
    private Integer       totalAmount;    // 총_주문_금액
    private Integer       discountAmount; // 할인_금액
    private Integer       usedPoint;      // 사용_포인트
    private String        status;         // 주문_상태 (PENDING, COMPLETED, CANCELLED)
    private LocalDateTime createdAt;      // 주문일시
}
