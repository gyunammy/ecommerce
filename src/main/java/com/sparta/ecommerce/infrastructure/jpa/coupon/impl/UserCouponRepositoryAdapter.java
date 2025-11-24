package com.sparta.ecommerce.infrastructure.jpa.coupon.impl;

import com.sparta.ecommerce.domain.coupon.UserCouponRepository;
import com.sparta.ecommerce.domain.coupon.entity.UserCoupon;
import com.sparta.ecommerce.infrastructure.jpa.coupon.JpaUserCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserCouponRepositoryAdapter implements UserCouponRepository {
    private final JpaUserCouponRepository jpaUserCouponRepository;

    @Override
    public Optional<UserCoupon> findById(Long userCouponId) {
        return jpaUserCouponRepository.findById(userCouponId);
    }

    @Override
    public Optional<UserCoupon> findByUserIdAndCouponId(Long userId, Long couponId) {
        return jpaUserCouponRepository.findByUserIdAndCouponId(userId, couponId);
    }

    @Override
    public UserCoupon save(UserCoupon userCoupon) {
        return jpaUserCouponRepository.save(userCoupon);
    }

    @Override
    public List<UserCoupon> findAll() {
        return jpaUserCouponRepository.findAll();
    }
}
