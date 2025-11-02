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
@Schema(description = "장바구니 아이템 응답")
public class CartItemResponse {
    @Schema(description = "장바구니 아이템 ID", example = "1")
    private Long cartItemId;

    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "상품 ID", example = "1")
    private Long productId;

    @Schema(description = "상품명", example = "스마트워치")
    private String productName;

    @Schema(description = "수량", example = "2")
    private Integer quantity;

    @Schema(description = "주문금액", example = "500000")
    private Integer total_amount;

    @Schema(description = "생성일시", example = "2024-01-25T10:30:00Z")
    private String createdAt;

    @Schema(description = "수정일시", example = "2024-01-25T10:30:00Z")
    private String updatedAt;
}
