package com.sparta.ecommerce.infrastructure.memory.coupon;

import com.sparta.ecommerce.domain.coupon.CouponRepository;
import com.sparta.ecommerce.domain.coupon.entity.Coupon;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryCouponRepository implements CouponRepository {

    private final Map<Long, Coupon> table = new ConcurrentHashMap<>();
    private final AtomicLong cursor = new AtomicLong(1);

    @Override
    public Optional<Coupon> findById(Long couponId) {
        return Optional.ofNullable(table.get(couponId));
    }

    @Override
    public Coupon save(Coupon coupon) {
        if (coupon.getCouponId() == null) {
            // ID가 없으면 자동 생성하고 새 객체 생성
            Long newId = cursor.getAndIncrement();
            Coupon newCoupon = new Coupon(
                    newId,
                    coupon.getCouponName(),
                    coupon.getDiscountType(),
                    coupon.getDiscountValue(),
                    coupon.getTotalQuantity(),
                    coupon.getIssuedQuantity(),
                    coupon.getUsedQuantity(),
                    coupon.getCreateAt(),
                    coupon.getExpiresAt()
            );
            table.put(newId, newCoupon);
            return newCoupon;
        } else {
            table.put(coupon.getCouponId(), coupon);
            return coupon;
        }
    }
}
