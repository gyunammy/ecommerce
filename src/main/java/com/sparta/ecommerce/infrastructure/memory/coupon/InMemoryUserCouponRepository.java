package com.sparta.ecommerce.infrastructure.memory.coupon;

import com.sparta.ecommerce.domain.coupon.entity.UserCoupon;
import com.sparta.ecommerce.domain.coupon.UserCouponRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

//@Primary
@Repository
public class InMemoryUserCouponRepository implements UserCouponRepository {

    private final Map<Long, UserCoupon> table = new ConcurrentHashMap<>();
    private final AtomicLong cursor = new AtomicLong(1);

    @Override
    public Optional<UserCoupon> findById(Long userCouponId) {
        return Optional.ofNullable(table.get(userCouponId));
    }

    /**
     * 사용자에게 특정 쿠폰이 이미 발급되었는지 확인합니다.
     *
     * @param userId   조회할 사용자 ID
     * @param couponId 조회할 쿠폰 ID
     * @return Optional<UserCoupon> - 해당 사용자가 이미 해당 쿠폰을 발급받은 경우 UserCoupon 반환, 없으면 empty
     */
    @Override
    public Optional<UserCoupon> findByUserIdAndCouponId(Long userId, Long couponId) {
        for (UserCoupon userCoupon : table.values()) {
            if (userCoupon.getCouponId().equals(couponId) && userCoupon.getUserId().equals(userId)) {
                return Optional.of(userCoupon);
            }
        }

        return Optional.empty();
    }

    @Override
    public UserCoupon save(UserCoupon userCoupon) {
        // ID가 없으면 새로 생성 (insert)
        if (userCoupon.getUserCouponId() == null) {
            Long id = cursor.getAndIncrement();
            UserCoupon newUserCoupon = new UserCoupon(
                    id,
                    userCoupon.getUserId(),
                    userCoupon.getCouponId(),
                    userCoupon.isUsed(),
                    userCoupon.getIssuedAt(),
                    userCoupon.getUsedAt()
            );
            table.put(id, newUserCoupon);
            return newUserCoupon;
        }

        // ID가 있으면 업데이트 (update)
        table.put(userCoupon.getUserCouponId(), userCoupon);
        return userCoupon;
    }

    @Override
    public List<UserCoupon> findAll() {
        return new ArrayList<>(table.values());
    }
}
