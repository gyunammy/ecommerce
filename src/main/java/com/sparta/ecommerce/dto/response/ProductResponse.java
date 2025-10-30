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
@Schema(description = "상품 응답")
public class ProductResponse {
    @Schema(description = "상품 ID", example = "1")
    private Long productId;

    @Schema(description = "상품명", example = "무선 블루투스 이어폰")
    private String productName;

    @Schema(description = "상품 설명", example = "고음질 노이즈 캔슬링 기능이 탑재된 프리미엄 무선 이어폰")
    private String description;

    @Schema(description = "가격", example = "89000")
    private Integer price;

    @Schema(description = "재고 수량", example = "150")
    private Integer quantity;

    @Schema(description = "생성일시", example = "2024-01-10T10:00:00Z")
    private String createdAt;

    @Schema(description = "수정일시", example = "2024-01-10T10:00:00Z")
    private String updatedAt;
}
