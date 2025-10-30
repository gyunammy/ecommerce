package com.sparta.ecommerce.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "주문 응답")
public class OrderResponse {
    @Schema(description = "주문 ID", example = "1")
    private Long orderId;

    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "사용자 쿠폰 ID", example = "1", nullable = true)
    private Long userCouponId;

    @Schema(description = "총 주문 금액", example = "129000")
    private Integer totalAmount;

    @Schema(description = "할인 금액", example = "5000")
    private Integer discountAmount;

    @Schema(description = "사용 포인트", example = "1000")
    private Integer usedPoint;

    @Schema(description = "주문 상태", example = "COMPLETED")
    private String status;

    @Schema(description = "주문 상품 목록")
    private List<OrderItemResponse> orderItems;

    @Schema(description = "주문일시", example = "2024-01-02T14:30:00Z")
    private String createdAt;
}
