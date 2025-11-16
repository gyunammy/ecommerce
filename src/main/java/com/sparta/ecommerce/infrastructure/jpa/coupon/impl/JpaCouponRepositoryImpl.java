package com.sparta.ecommerce.infrastructure.jpa.coupon.impl;

import com.sparta.ecommerce.domain.coupon.CouponRepository;
import com.sparta.ecommerce.domain.coupon.entity.Coupon;
import com.sparta.ecommerce.infrastructure.jpa.coupon.JpaCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaCouponRepositoryImpl implements CouponRepository {
    private final JpaCouponRepository jpaCouponRepository;
    @Override
    public Optional<Coupon> findById(Long couponId) {
        return jpaCouponRepository.findById(couponId);
    }

    @Override
    public Coupon save(Coupon coupon) {
        return jpaCouponRepository.save(coupon);
    }

    @Override
    public Optional<Coupon> findByIdWithPessimisticLock(Long couponId) {
        return jpaCouponRepository.findByIdWithPessimisticLock(couponId);
    }
}
