package com.sparta.ecommerce.domain.cart.entity;

import com.sparta.ecommerce.domain.cart.dto.CartItemResponse;
import com.sparta.ecommerce.domain.coupon.dto.UserCouponResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CartItem {
    private Long          cartItemId; // 장바구니_아이템_ID
    private Long          userId;     // 사용자_ID
    private Long          productId;  // 상품_ID
    private Integer       quantity;   // 개수
    private LocalDateTime createAt;   // 생성일
    private LocalDateTime updateAt;   // 수정일

    public CartItemResponse from(){
        return new CartItemResponse(
                this.cartItemId,
                this.userId,
                this.productId,
                this.quantity,
                this.createAt,
                this.updateAt);
    }
}
