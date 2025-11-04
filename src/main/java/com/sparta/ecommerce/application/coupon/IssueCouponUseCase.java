package com.sparta.ecommerce.application.coupon;

import com.sparta.ecommerce.application.user.UserService;
import com.sparta.ecommerce.domain.coupon.entity.Coupon;
import com.sparta.ecommerce.domain.coupon.exception.CouponErrorCode;
import com.sparta.ecommerce.domain.coupon.exception.CouponException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IssueCouponUseCase {
    private final UserService userService;
    private final CouponService couponService;
    private final UserCouponService userCouponService;

    /**
     * 쿠폰 발급
     *
     * 1. 사용자 존재 여부 확인
     * 2. 쿠폰 조회 및 존재 여부 확인
     * 3. 쿠폰 발급 가능 여부 검증 (만료일 + 재고)
     * 4. 중복 발급 검증
     * 5. 쿠폰 발급 수량 증가
     * 6. 사용자에게 쿠폰 발급
     *
     * @param userId 사용자 ID
     * @param couponId 쿠폰 ID
     */
    public void issueCoupon(Long userId, Long couponId) {
        // 1. 사용자 존재 여부 확인
        userService.getUserById(userId);

        // 2. 쿠폰 조회
        Coupon coupon = couponService.getCoupon(couponId);

        // 3. 쿠폰 발급 가능 여부 검증 (만료일 + 재고)
        coupon.validateIssuable();

        // 4. 중복 발급 검증
        if (userCouponService.hasCoupon(userId, couponId)) {
            throw new CouponException(CouponErrorCode.COUPON_ALREADY_ISSUED);
        }

        // 5. 쿠폰 발급 수량 증가
        coupon.increaseIssuedQuantity();
        couponService.saveCoupon(coupon);

        // 6. 사용자에게 쿠폰 발급
        userCouponService.issueCoupon(userId, couponId);
    }
}
