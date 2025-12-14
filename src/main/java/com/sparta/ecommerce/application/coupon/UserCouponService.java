package com.sparta.ecommerce.application.coupon;

import com.sparta.ecommerce.application.coupon.event.CouponUsageFailedEvent;
import com.sparta.ecommerce.application.coupon.event.CouponUsedSuccessEvent;
import com.sparta.ecommerce.application.order.OrderService;
import com.sparta.ecommerce.application.order.event.OrderCreatedEvent;
import com.sparta.ecommerce.domain.coupon.CouponRepository;
import com.sparta.ecommerce.domain.coupon.UserCouponRepository;
import com.sparta.ecommerce.domain.coupon.entity.Coupon;
import com.sparta.ecommerce.domain.coupon.entity.UserCoupon;
import com.sparta.ecommerce.domain.coupon.exception.CouponErrorCode;
import com.sparta.ecommerce.domain.coupon.exception.CouponException;
import com.sparta.ecommerce.domain.order.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserCouponService {

    private final UserCouponRepository userCouponRepository;
    private final CouponRepository couponRepository;
    private final OrderService orderService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 사용자가 이미 해당 쿠폰을 보유하고 있는지 확인
     * @param userId 사용자 ID
     * @param couponId 쿠폰 ID
     * @return 이미 보유 중이면 true, 아니면 false
     */
    public boolean hasCoupon(Long userId, Long couponId) {
        return userCouponRepository.findByUserIdAndCouponId(userId, couponId).isPresent();
    }

    /**
     * 사용자에게 쿠폰 발급
     * @param userId 사용자 ID
     * @param couponId 쿠폰 ID
     * @return 발급된 사용자 쿠폰
     */
    public UserCoupon issueCoupon(Long userId, Long couponId) {
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(couponId);
        userCoupon.setUsed(false);
        userCoupon.setIssuedAt(LocalDateTime.now());
        return userCouponRepository.save(userCoupon);
    }

    /**
     * 쿠폰 유효성 검증 및 조회
     * @param userCouponId 사용자 쿠폰 ID
     * @param userId 사용자 ID
     * @return 유효성 검증된 UserCoupon과 Coupon 정보를 담은 DTO
     */
    public ValidatedCoupon validateAndGetCoupon(Long userCouponId, Long userId) {
        // 1. UserCoupon 존재 여부 확인
        UserCoupon userCoupon = userCouponRepository.findById(userCouponId)
                .orElseThrow(() -> new CouponException(CouponErrorCode.USER_COUPON_NOT_FOUND));

        // 2. 본인의 쿠폰인지 확인
        if (!userCoupon.getUserId().equals(userId)) {
            throw new CouponException(CouponErrorCode.COUPON_NOT_OWNED);
        }

        // 3. 사용 여부 확인
        if (userCoupon.isUsed()) {
            throw new CouponException(CouponErrorCode.COUPON_ALREADY_USED);
        }

        // 4. Coupon 정보 조회
        Coupon coupon = couponRepository.findById(userCoupon.getCouponId())
                .orElseThrow(() -> new CouponException(CouponErrorCode.COUPON_NOT_FOUND));

        return new ValidatedCoupon(userCoupon, coupon);
    }

    /**
     * 쿠폰 사용 처리
     * 도메인 엔티티의 상태 변경 로직을 호출하고 저장합니다.
     *
     * @param userCoupon 사용할 쿠폰
     */
    public void markAsUsed(UserCoupon userCoupon) {
        userCoupon.markAsUsed();
        userCouponRepository.save(userCoupon);
    }

    /**
     * 쿠폰 ID로 쿠폰을 사용 처리합니다.
     *
     * @param userCouponId 사용자 쿠폰 ID
     */
    public void markAsUsedById(Long userCouponId) {
        UserCoupon userCoupon = userCouponRepository.findById(userCouponId)
                .orElseThrow(() -> new CouponException(CouponErrorCode.USER_COUPON_NOT_FOUND));
        markAsUsed(userCoupon);
    }

    /**
     * 쿠폰 정보 업데이트
     *
     * @param userCoupon 업데이트할 쿠폰
     */
    public void updateUserCoupon(UserCoupon userCoupon) {
        userCouponRepository.save(userCoupon);
    }

    /**
     * 주문 생성 이벤트를 통한 쿠폰 사용 처리
     *
     * 쿠폰이 없는 경우에도 성공 이벤트를 발행합니다.
     * try-catch로 예외를 처리하며, 성공 시 CouponUsedSuccessEvent, 실패 시 CouponUsageFailedEvent를 발행합니다.
     *
     * @param event 주문 생성 완료 이벤트
     */
    @Transactional
    public void processCouponUsage(OrderCreatedEvent event) {

        // 주문 상태 확인 (FAILED 상태면 이미 다른 작업이 실패함)
        Order order = orderService.getOrderById(event.orderId());
        if ("FAILED".equals(order.getStatus())) {
            log.debug("이미 실패한 주문 - OrderId: {}", event.orderId());
            return;
        }

        // 쿠폰이 없는 주문인 경우 바로 성공 이벤트 발행
        if (event.userCouponId() == null) {
            log.debug("쿠폰 없는 주문 - OrderId: {}", event.orderId());

            // 조율 서비스를 위해 성공 이벤트 발행
            eventPublisher.publishEvent(new CouponUsedSuccessEvent(
                    event.orderId(),
                    event.userId()
            ));
            return;
        }

        try {
            log.debug("쿠폰 사용 처리 시작 - UserCouponId: {}, OrderId: {}",
                    event.userCouponId(), event.orderId());

            markAsUsedById(event.userCouponId());

            log.info("쿠폰 사용 성공 - OrderId: {}", event.orderId());

            // 쿠폰 사용 성공 이벤트 발행 (조율 서비스에서 받음)
            eventPublisher.publishEvent(new CouponUsedSuccessEvent(
                    event.orderId(),
                    event.userId()
            ));

        } catch (Exception e) {
            log.error("쿠폰 사용 처리 실패 - UserCouponId: {}, OrderId: {}, Error: {}",
                    event.userCouponId(), event.orderId(), e.getMessage(), e);

            // 쿠폰 사용 실패 이벤트 발행 (OrderService에서 주문 상태 변경 및 보상 트랜잭션 처리)
            eventPublisher.publishEvent(new CouponUsageFailedEvent(
                    event.orderId(),
                    event.userId(),
                    event.userCouponId(),
                    event.finalAmount(),
                    e.getMessage(),
                    event.cartItems()
            ));
        }
    }

    /**
     * 쿠폰 검증 및 할인 금액을 계산합니다.
     *
     * @param userCouponId 사용자 쿠폰 ID
     * @param userId 사용자 ID
     * @param totalAmount 총 주문 금액
     * @return 쿠폰 할인 계산 결과
     */
    public CouponDiscountResult validateAndCalculateDiscount(Long userCouponId, Long userId, int totalAmount) {
        if (userCouponId == null) {
            return new CouponDiscountResult(null, 0);
        }

        ValidatedCoupon validatedCoupon = validateAndGetCoupon(userCouponId, userId);
        int discountAmount = validatedCoupon.coupon().calculateDiscount(totalAmount);

        return new CouponDiscountResult(validatedCoupon.userCoupon(), discountAmount);
    }

    /**
     * 유효성 검증된 쿠폰 정보를 담는 DTO
     */
    public record ValidatedCoupon(
            UserCoupon userCoupon,
            Coupon coupon
    ) {}

    /**
     * 쿠폰 할인 계산 결과
     */
    public record CouponDiscountResult(
            UserCoupon userCoupon,
            int discountAmount
    ) {}
}
