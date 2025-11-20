package com.sparta.ecommerce.application.coupon;

import com.sparta.ecommerce.application.user.UserService;
import com.sparta.ecommerce.domain.coupon.entity.Coupon;
import com.sparta.ecommerce.domain.coupon.exception.CouponErrorCode;
import com.sparta.ecommerce.domain.coupon.exception.CouponException;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional
    public void issueCoupon(Long userId, Long couponId) {
        try {
            // 1. 사용자 존재 여부 확인
            userService.getUserById(userId);

            // 2. 쿠폰 조회
            Coupon coupon = couponService.getCouponForUpdate(couponId);

            // 3. 쿠폰 발급 가능 여부 검증 (만료일 + 재고)
            coupon.validateIssuable();

            // 4. 중복 발급 검증
            if (userCouponService.hasCoupon(userId, couponId)) {
                throw new CouponException(CouponErrorCode.COUPON_ALREADY_ISSUED);
            }

            // 5. 사용자에게 쿠폰 발급 (DB Unique 제약 조건으로 동시성 상황에서 중복 방지)
            userCouponService.issueCoupon(userId, couponId);

            // 6. 쿠폰 발급 수량 증가
            coupon.increaseIssuedQuantity();
            couponService.saveCoupon(coupon);
        } catch (DataIntegrityViolationException | ConstraintViolationException e) {
            // DB Unique 제약 조건 위반 (동시성 상황에서 중복 발급 시도)
            throw new CouponException(CouponErrorCode.COUPON_ALREADY_ISSUED);
        } catch (PessimisticLockingFailureException e) {
            // Lock timeout 발생 시
            throw new CouponException(CouponErrorCode.COUPON_LOCK_TIMEOUT);
        }
    }
}
