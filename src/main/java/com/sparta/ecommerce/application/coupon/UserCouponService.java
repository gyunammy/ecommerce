package com.sparta.ecommerce.application.coupon;

import com.sparta.ecommerce.domain.coupon.CouponRepository;
import com.sparta.ecommerce.domain.coupon.entity.Coupon;
import com.sparta.ecommerce.domain.coupon.entity.UserCoupon;
import com.sparta.ecommerce.domain.coupon.UserCouponRepository;
import com.sparta.ecommerce.domain.coupon.exception.CouponErrorCode;
import com.sparta.ecommerce.domain.coupon.exception.CouponException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static com.sparta.ecommerce.domain.coupon.exception.CouponErrorCode.*;

@Service
@RequiredArgsConstructor
public class UserCouponService {

    private final UserCouponRepository userCouponRepository;
    private final CouponRepository couponRepository;

    /**
     * 사용자가 이미 해당 쿠폰을 보유하고 있는지 확인
     * @param userId 사용자 ID
     * @param couponId 쿠폰 ID
     * @return 이미 보유 중이면 true, 아니면 false
     */
    public boolean hasCoupon(Long userId, Long couponId) {
        return userCouponRepository.findByUserIdAndCouponId(userId, couponId).isPresent();
    }

    /**
     * 사용자에게 쿠폰 발급
     * @param userId 사용자 ID
     * @param couponId 쿠폰 ID
     * @return 발급된 사용자 쿠폰
     */
    public UserCoupon issueCoupon(Long userId, Long couponId) {
        return userCouponRepository.issueUserCoupon(userId, couponId);
    }

    /**
     * 쿠폰 유효성 검증 및 조회
     * @param userCouponId 사용자 쿠폰 ID
     * @param userId 사용자 ID
     * @return 유효성 검증된 UserCoupon과 Coupon 정보를 담은 DTO
     */
    public ValidatedCoupon validateAndGetCoupon(Long userCouponId, Long userId) {
        // 1. UserCoupon 존재 여부 확인
        UserCoupon userCoupon = userCouponRepository.findById(userCouponId)
                .orElseThrow(() -> new CouponException(USER_COUPON_NOT_FOUND));

        // 2. 본인의 쿠폰인지 확인
        if (!userCoupon.getUserId().equals(userId)) {
            throw new CouponException(COUPON_NOT_OWNED);
        }

        // 3. 사용 여부 확인
        if (userCoupon.isUsed()) {
            throw new CouponException(COUPON_ALREADY_USED);
        }

        // 4. Coupon 정보 조회
        Coupon coupon = couponRepository.findById(userCoupon.getCouponId())
                .orElseThrow(() -> new CouponException(COUPON_NOT_FOUND));

        return new ValidatedCoupon(userCoupon, coupon);
    }

    /**
     * 쿠폰 사용 처리
     * 도메인 엔티티의 상태 변경 로직을 호출하고 저장합니다.
     *
     * @param userCoupon 사용할 쿠폰
     */
    public void markAsUsed(UserCoupon userCoupon) {
        userCoupon.markAsUsed();
        userCouponRepository.update(userCoupon);
    }

    /**
     * 쿠폰 정보 업데이트
     *
     * @param userCoupon 업데이트할 쿠폰
     */
    public void updateUserCoupon(UserCoupon userCoupon) {
        userCouponRepository.update(userCoupon);
    }

    /**
     * 유효성 검증된 쿠폰 정보를 담는 DTO
     */
    public record ValidatedCoupon(
            UserCoupon userCoupon,
            Coupon coupon
    ) {}
}
