package com.sparta.ecommerce.domain.product;

import com.sparta.ecommerce.domain.coupon.dto.ProductResponse;
import com.sparta.ecommerce.domain.product.exception.ProductException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static com.sparta.ecommerce.domain.product.exception.ProductErrorCode.INSUFFICIENT_STOCK;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    private Long          productId;   // 상품_ID
    private String        productName; // 상품_명
    private String        description; // 상품_설명
    private Integer       quantity;    // 재고_수량
    private Integer       price;       // 가격
    private Integer       viewCount;   // 조회수
    private LocalDateTime createdAt;   // 등록일
    private LocalDateTime updateAt;    // 수정일

    /**
     * 재고 가용성 검증
     *
     * @param requestedQuantity 요청한 수량
     * @throws ProductException 재고가 부족한 경우
     */
    public void validateStock(int requestedQuantity) {
        if (this.quantity < requestedQuantity) {
            throw new ProductException(INSUFFICIENT_STOCK);
        }
    }

    /**
     * 재고 차감
     *
     * @param decreaseQuantity 차감할 수량
     * @throws ProductException 재고가 부족한 경우
     */
    public void decreaseStock(int decreaseQuantity) {
        validateStock(decreaseQuantity);
        this.quantity -= decreaseQuantity;
    }

    public ProductResponse from() {
        return new ProductResponse(this.productId, this.productName, this.description, this.quantity, this.price, this.viewCount, this.createdAt, this.updateAt);
    }
}
