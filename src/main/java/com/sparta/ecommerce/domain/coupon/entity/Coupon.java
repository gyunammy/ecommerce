package com.sparta.ecommerce.domain.coupon.entity;

import com.sparta.ecommerce.domain.coupon.dto.CouponResponse;
import com.sparta.ecommerce.domain.coupon.exception.CouponErrorCode;
import com.sparta.ecommerce.domain.coupon.exception.CouponException;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Coupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long          couponId;       // 쿠폰_ID
    private String        couponName;     // 쿠폰_명
    private String        discountType;   // 할인타입(비율/금액)
    private Integer       discountValue;  // 금액이면 금액, 비율이면 %
    private Integer       totalQuantity;  // 총_쿠폰_개수
    private Integer       issuedQuantity; // 발급_쿠폰_개수
    private Integer       usedQuantity;   // 사용_쿠폰_개수
    private LocalDateTime createAt;       // 생성일
    private LocalDateTime expiresAt;      // 만료일

    /**
     * 쿠폰 발급 가능 여부 확인 (재고 기준)
     *
     * @return 발급 가능하면 true, 불가능하면 false
     */
    public boolean isIssuable() {
        return this.totalQuantity > this.issuedQuantity;
    }

    /**
     * 쿠폰 만료 여부 확인
     *
     * @return 만료되었으면 true, 아니면 false
     */
    public boolean isExpired() {
        return this.expiresAt != null && this.expiresAt.isBefore(LocalDateTime.now());
    }

    /**
     * 쿠폰 발급 가능 여부 검증 (만료일 + 재고)
     *
     * @throws CouponException 만료되었거나 재고가 없을 경우
     */
    public void validateIssuable() {
        if (isExpired()) {
            throw new CouponException(CouponErrorCode.COUPON_EXPIRED);
        }
        if (!isIssuable()) {
            throw new CouponException(CouponErrorCode.COUPON_OUT_OF_STOCK);
        }
    }

    /**
     * 쿠폰 발급 수량 증가
     * 발급 가능 여부를 확인하고 발급 수량을 1 증가시킴
     *
     * @throws CouponException 발급 가능한 쿠폰이 없을 경우
     */
    public void increaseIssuedQuantity() {
        if (!isIssuable()) {
            throw new CouponException(CouponErrorCode.COUPON_OUT_OF_STOCK);
        }
        this.issuedQuantity++;
    }

    /**
     * 쿠폰 사용 수량 증가
     */
    public void increaseUsedQuantity() {
        this.usedQuantity++;
    }

    /**
     * 쿠폰 할인액 계산
     *
     * @param totalAmount 총 금액
     * @return 할인 금액
     */
    public int calculateDiscount(int totalAmount) {
        return switch (this.discountType.toUpperCase()) {
            case "RATE" -> totalAmount * this.discountValue / 100;  // 비율 할인
            case "AMOUNT" -> this.discountValue;  // 금액 할인
            default -> 0;
        };
    }

    public CouponResponse from(){
        return new CouponResponse(
                this.couponId,
                this.couponName,
                this.discountType,
                this.discountValue,
                this.totalQuantity,
                this.issuedQuantity,
                this.createAt,
                this.expiresAt);
    }
}
