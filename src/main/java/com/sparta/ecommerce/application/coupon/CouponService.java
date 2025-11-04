package com.sparta.ecommerce.application.coupon;

import com.sparta.ecommerce.domain.coupon.CouponRepository;
import com.sparta.ecommerce.domain.coupon.entity.Coupon;
import com.sparta.ecommerce.domain.coupon.exception.CouponErrorCode;
import com.sparta.ecommerce.domain.coupon.exception.CouponException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;

    /**
     * 쿠폰 ID로 쿠폰 조회
     *
     * @param couponId 쿠폰 ID
     * @return 조회된 쿠폰
     * @throws CouponException 쿠폰을 찾을 수 없을 경우
     */
    public Coupon getCoupon(Long couponId) {
        return couponRepository.findById(couponId)
                .orElseThrow(() -> new CouponException(CouponErrorCode.COUPON_NOT_FOUND));
    }

    /**
     * 쿠폰 저장
     *
     * @param coupon 저장할 쿠폰
     */
    public void saveCoupon(Coupon coupon) {
        couponRepository.save(coupon);
    }
}
