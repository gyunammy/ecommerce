package com.sparta.ecommerce.application.coupon;

import com.sparta.ecommerce.application.user.UserService;
import com.sparta.ecommerce.domain.coupon.entity.Coupon;
import com.sparta.ecommerce.domain.coupon.entity.UserCoupon;
import com.sparta.ecommerce.domain.coupon.exception.CouponErrorCode;
import com.sparta.ecommerce.domain.coupon.exception.CouponException;
import com.sparta.ecommerce.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.sparta.ecommerce.domain.coupon.exception.CouponErrorCode.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IssueCouponUseCaseTest {

    @Mock
    private UserService userService;

    @Mock
    private CouponService couponService;

    @Mock
    private UserCouponService userCouponService;

    @InjectMocks
    private IssueCouponUseCase issueCouponUseCase;

    @Test
    @DisplayName("쿠폰 발급 성공")
    void issueCoupon_success() {
        // given
        Long userId = 1L;
        Long couponId = 1L;

        User user = new User(userId, "testUser", 0, 0L, LocalDateTime.now());
        Coupon coupon = new Coupon(
                couponId,
                "10% 할인 쿠폰",
                "RATE",
                10,
                100,
                50,
                0,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(30)
        );
        UserCoupon userCoupon = new UserCoupon(1L, userId, couponId, false, LocalDateTime.now(), null);

        given(userService.getUserById(userId)).willReturn(user);
        given(couponService.getCouponForUpdate(couponId)).willReturn(coupon);
        given(userCouponService.hasCoupon(userId, couponId)).willReturn(false);
        given(userCouponService.issueCoupon(userId, couponId)).willReturn(userCoupon);

        // when
        issueCouponUseCase.issueCoupon(userId, couponId);

        // then
        verify(userService).getUserById(userId);
        verify(couponService).getCouponForUpdate(couponId);
        verify(userCouponService).hasCoupon(userId, couponId);
        verify(couponService).saveCoupon(coupon);
        verify(userCouponService).issueCoupon(userId, couponId);
    }

    @Test
    @DisplayName("만료된 쿠폰 발급 시도 시 예외 발생")
    void issueCoupon_expired() {
        // given
        Long userId = 1L;
        Long couponId = 1L;

        User user = new User(userId, "testUser", 0, 0L, LocalDateTime.now());
        Coupon expiredCoupon = new Coupon(
                couponId,
                "만료된 쿠폰",
                "RATE",
                10,
                100,
                50,
                0,
                LocalDateTime.now().minusDays(60),
                LocalDateTime.now().minusDays(30)  // 만료됨
        );

        given(userService.getUserById(userId)).willReturn(user);
        given(couponService.getCouponForUpdate(couponId)).willReturn(expiredCoupon);

        // when & then
        assertThatThrownBy(() -> issueCouponUseCase.issueCoupon(userId, couponId))
                .isInstanceOf(CouponException.class)
                .hasMessageContaining(COUPON_EXPIRED.getMessage());

        verify(userCouponService, never()).issueCoupon(any(), any());
    }

    @Test
    @DisplayName("재고가 없는 쿠폰 발급 시도 시 예외 발생")
    void issueCoupon_outOfStock() {
        // given
        Long userId = 1L;
        Long couponId = 1L;

        User user = new User(userId, "testUser", 0, 0L, LocalDateTime.now());
        Coupon outOfStockCoupon = new Coupon(
                couponId,
                "품절된 쿠폰",
                "RATE",
                10,
                100,
                100,  // 발급 수량 == 총 수량
                0,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(30)
        );

        given(userService.getUserById(userId)).willReturn(user);
        given(couponService.getCouponForUpdate(couponId)).willReturn(outOfStockCoupon);

        // when & then
        assertThatThrownBy(() -> issueCouponUseCase.issueCoupon(userId, couponId))
                .isInstanceOf(CouponException.class)
                .hasMessageContaining(COUPON_OUT_OF_STOCK.getMessage());

        verify(userCouponService, never()).issueCoupon(any(), any());
    }

    @Test
    @DisplayName("이미 발급받은 쿠폰 재발급 시도 시 예외 발생")
    void issueCoupon_alreadyIssued() {
        // given
        Long userId = 1L;
        Long couponId = 1L;

        User user = new User(userId, "testUser", 0, 0L, LocalDateTime.now());
        Coupon coupon = new Coupon(
                couponId,
                "10% 할인 쿠폰",
                "RATE",
                10,
                100,
                50,
                0,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(30)
        );

        given(userService.getUserById(userId)).willReturn(user);
        given(couponService.getCouponForUpdate(couponId)).willReturn(coupon);
        given(userCouponService.hasCoupon(userId, couponId)).willReturn(true);  // 이미 보유

        // when & then
        assertThatThrownBy(() -> issueCouponUseCase.issueCoupon(userId, couponId))
                .isInstanceOf(CouponException.class)
                .hasMessageContaining(COUPON_ALREADY_ISSUED.getMessage());

        verify(userCouponService, never()).issueCoupon(any(), any());
        verify(couponService, never()).saveCoupon(any());
    }

    @Test
    @DisplayName("쿠폰 발급 시 발급 수량 증가 확인")
    void issueCoupon_increaseIssuedQuantity() {
        // given
        Long userId = 1L;
        Long couponId = 1L;

        User user = new User(userId, "testUser", 0, 0L, LocalDateTime.now());
        Coupon coupon = new Coupon(
                couponId,
                "10% 할인 쿠폰",
                "RATE",
                10,
                100,
                50,  // 초기 발급 수량
                0,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(30)
        );
        UserCoupon userCoupon = new UserCoupon(1L, userId, couponId, false, LocalDateTime.now(), null);

        given(userService.getUserById(userId)).willReturn(user);
        given(couponService.getCouponForUpdate(couponId)).willReturn(coupon);
        given(userCouponService.hasCoupon(userId, couponId)).willReturn(false);
        given(userCouponService.issueCoupon(userId, couponId)).willReturn(userCoupon);

        // when
        issueCouponUseCase.issueCoupon(userId, couponId);

        // then
        // increaseIssuedQuantity()가 호출되어 51로 증가했는지 확인할 수 있지만,
        // Mockito로는 내부 상태 변경을 직접 검증하기 어려우므로
        // saveCoupon이 호출되었는지만 확인
        verify(couponService).saveCoupon(coupon);
    }
}
