package com.sparta.ecommerce.infrastructure.jpa.coupon;

import com.sparta.ecommerce.domain.coupon.UserCouponRepository;
import com.sparta.ecommerce.domain.coupon.entity.UserCoupon;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Primary
@Repository
public interface JpaUserCouponRepository extends JpaRepository<UserCoupon, Long>, UserCouponRepository {
}
