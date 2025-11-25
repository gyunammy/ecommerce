package com.sparta.ecommerce.application.coupon;

import com.sparta.ecommerce.application.user.UserService;
import com.sparta.ecommerce.common.annotation.RedissonLock;
import com.sparta.ecommerce.domain.coupon.CouponRepository;
import com.sparta.ecommerce.domain.coupon.entity.Coupon;
import com.sparta.ecommerce.domain.coupon.exception.CouponErrorCode;
import com.sparta.ecommerce.domain.coupon.exception.CouponException;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IssueCouponUseCase {
    private final UserService userService;
    private final CouponRepository couponRepository;
    private final CouponService couponService;
    private final UserCouponService userCouponService;

    /**
     * 쿠폰 발급 (Redis 분산 락으로 순차 처리)
     *
     * @param userId 사용자 ID
     * @param couponId 쿠폰 ID
     */
    @RedissonLock(value = "coupon:issue", keyParameterIndex = 1)
    @Transactional
    public void issueCoupon(Long userId, Long couponId) {
        try {
            // 1. 사용자 존재 여부 확인
            userService.getUserById(userId);

            // 2. 쿠폰 재조회
            Coupon coupon = couponRepository.findById(couponId)
                    .orElseThrow(() -> new CouponException(CouponErrorCode.COUPON_NOT_FOUND));

            // 3. 쿠폰 발급 가능 여부 검증
            coupon.validateIssuable();

            // 4. 중복 발급 검증
            if (userCouponService.hasCoupon(userId, couponId)) {
                throw new CouponException(CouponErrorCode.COUPON_ALREADY_ISSUED);
            }

            // 5. 사용자에게 쿠폰 발급
            userCouponService.issueCoupon(userId, couponId);

            // 6. 쿠폰 발급 수량 증가
            coupon.increaseIssuedQuantity();
            couponService.saveCoupon(coupon);
        } catch (DataIntegrityViolationException | ConstraintViolationException e) {
            throw new CouponException(CouponErrorCode.COUPON_ALREADY_ISSUED);
        }
    }
}
