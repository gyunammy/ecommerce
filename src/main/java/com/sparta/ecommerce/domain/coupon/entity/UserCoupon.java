package com.sparta.ecommerce.domain.coupon.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(uniqueConstraints = {
    @UniqueConstraint(columnNames = {"userId", "couponId"})
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class UserCoupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long          userCouponId; // 사용자_쿠폰_ID
    private Long          userId;       // 사용자_ID
    private Long          couponId;     // 쿠폰_ID
    private boolean       used;         // 사용_여부
    private LocalDateTime issuedAt;     // 발급일
    private LocalDateTime usedAt;       // 사용일

    /**
     * 쿠폰 사용 처리
     * 쿠폰을 사용 상태로 변경하고 사용 시간을 기록합니다.
     */
    public void markAsUsed() {
        this.used = true;
        this.usedAt = LocalDateTime.now();
    }

    /**
     * 쿠폰 사용 취소 (주문 실패 시 롤백용)
     * 쿠폰을 미사용 상태로 되돌리고 사용 시간을 제거합니다.
     */
    public void cancelUsage() {
        this.used = false;
        this.usedAt = null;
    }
}
