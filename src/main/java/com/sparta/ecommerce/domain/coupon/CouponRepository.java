package com.sparta.ecommerce.domain.coupon;

import com.sparta.ecommerce.domain.coupon.entity.Coupon;

import java.util.Optional;

public interface CouponRepository {
    Optional<Coupon> findById(Long couponId);

    Coupon save(Coupon coupon);
}
