package com.sparta.ecommerce.application.coupon;

import com.sparta.ecommerce.application.user.UserService;
import com.sparta.ecommerce.domain.coupon.CouponRepository;
import com.sparta.ecommerce.domain.coupon.entity.Coupon;
import com.sparta.ecommerce.domain.coupon.exception.CouponErrorCode;
import com.sparta.ecommerce.domain.coupon.exception.CouponException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IssueCouponUseCase {
    private final UserService userService;
    private final CouponRepository couponRepository;
    private final CouponService couponService;
    private final UserCouponService userCouponService;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String QUEUE_KEY = "coupon:issue:queue";

    /**
     * 쿠폰 발급 요청 (Queue에 추가)
     */
    public void issueCoupon(Long userId, Long couponId) {
        // 1. 사용자 존재 여부 확인
        userService.getUserById(userId);

        // 2. 쿠폰 존재 여부 및 만료 확인
        Coupon coupon = couponService.getCouponById(couponId);
        if (coupon.isExpired()) {
            throw new CouponException(CouponErrorCode.COUPON_EXPIRED);
        }

        // 3. 중복 발급 검증
        if (userCouponService.hasCoupon(userId, couponId)) {
            throw new CouponException(CouponErrorCode.COUPON_ALREADY_ISSUED);
        }

        // 4. Queue에 추가 (userId:couponId 형태)
        String queueData = userId + ":" + couponId;
        redisTemplate.opsForList().leftPush(QUEUE_KEY, queueData);
        log.info("쿠폰 발급 요청 Queue 추가 - {}", queueData);
    }

    /**
     * 쿠폰 실제 발급 처리 (Queue Consumer에서 호출)
     * Redis Queue의 순차 처리 + DB unique constraint + 낙관적 락으로 동시성 제어
     */
    @Transactional
    public void executeIssueCoupon(Long userId, Long couponId) {
        try {
            // 1. 쿠폰 재조회
            Coupon coupon = couponRepository.findById(couponId)
                    .orElseThrow(() -> new CouponException(CouponErrorCode.COUPON_NOT_FOUND));

            // 2. 쿠폰 발급 가능 여부 검증
            coupon.validateIssuable();

            // 3. 중복 발급 검증
            if (userCouponService.hasCoupon(userId, couponId)) {
                throw new CouponException(CouponErrorCode.COUPON_ALREADY_ISSUED);
            }

            // 4. 사용자에게 쿠폰 발급
            userCouponService.issueCoupon(userId, couponId);

            // 5. 쿠폰 발급 수량 증가
            coupon.increaseIssuedQuantity();
            couponService.saveCoupon(coupon);

            log.info("쿠폰 발급 완료 - userId: {}, couponId: {}", userId, couponId);
        } catch (DataIntegrityViolationException | ConstraintViolationException e) {
            throw new CouponException(CouponErrorCode.COUPON_ALREADY_ISSUED);
        }
    }
}
