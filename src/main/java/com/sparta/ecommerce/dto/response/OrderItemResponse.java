package com.sparta.ecommerce.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "주문 상품 응답")
public class OrderItemResponse {
    @Schema(description = "주문 상품 ID", example = "1")
    private Long orderItemId;

    @Schema(description = "주문 ID", example = "1")
    private Long orderId;

    @Schema(description = "상품 ID", example = "1")
    private Long productId;

    @Schema(description = "상품명", example = "무선 블루투스 이어폰")
    private String productName;

    @Schema(description = "상품 설명", example = "고음질 노이즈 캔슬링 기능")
    private String description;

    @Schema(description = "수량", example = "2")
    private Integer quantity;

    @Schema(description = "단가", example = "89000")
    private Integer price;

    @Schema(description = "등록일시", example = "2024-01-02T14:30:00Z")
    private String createdAt;
}
