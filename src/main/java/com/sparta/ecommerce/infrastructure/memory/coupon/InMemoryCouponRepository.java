package com.sparta.ecommerce.infrastructure.memory.coupon;

import com.sparta.ecommerce.domain.coupon.CouponRepository;
import com.sparta.ecommerce.domain.coupon.entity.Coupon;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Repository
public class InMemoryCouponRepository implements CouponRepository {

    private final Map<Long, Coupon> table = new LinkedHashMap<>();
    private long cursor = 1;

    @PostConstruct
    public void init() {
        LocalDateTime now = LocalDateTime.now();
        table.put(cursor, new Coupon(cursor++, "선착순 발급 쿠폰", "RATE", 10, 100, 0, 0, now, now.plusMonths(1)));
    }

    @Override
    public Optional<Coupon> findById(Long couponId) {
        return Optional.ofNullable(table.get(couponId));
    }

    @Override
    public void save(Coupon coupon) {
        table.put(coupon.getCouponId(), coupon);
    }
}
