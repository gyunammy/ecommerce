package com.sparta.ecommerce.domain.coupon;

import com.sparta.ecommerce.domain.coupon.entity.UserCoupon;

import java.util.List;
import java.util.Optional;

public interface UserCouponRepository {

    Optional<UserCoupon> findById(Long userCouponId);

    Optional<UserCoupon> findByUserIdAndCouponId(Long userId, Long couponId);

    UserCoupon save(UserCoupon userCoupon);

    List<UserCoupon> findAll();
}
