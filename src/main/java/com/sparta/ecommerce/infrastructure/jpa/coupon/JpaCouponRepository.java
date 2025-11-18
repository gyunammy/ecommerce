package com.sparta.ecommerce.infrastructure.jpa.coupon;

import com.sparta.ecommerce.domain.coupon.entity.Coupon;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface JpaCouponRepository extends JpaRepository<Coupon, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Coupon c where c.couponId = :id")
    Optional<Coupon> findByIdWithPessimisticLock(Long id);
}
