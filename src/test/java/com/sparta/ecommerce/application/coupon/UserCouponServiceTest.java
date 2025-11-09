package com.sparta.ecommerce.application.coupon;

import com.sparta.ecommerce.domain.coupon.CouponRepository;
import com.sparta.ecommerce.domain.coupon.UserCouponRepository;
import com.sparta.ecommerce.domain.coupon.entity.Coupon;
import com.sparta.ecommerce.domain.coupon.entity.UserCoupon;
import com.sparta.ecommerce.domain.coupon.exception.CouponException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.sparta.ecommerce.domain.coupon.exception.CouponErrorCode.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserCouponServiceTest {

    @Mock
    private UserCouponRepository userCouponRepository;

    @Mock
    private CouponRepository couponRepository;

    @InjectMocks
    private UserCouponService userCouponService;

    @Test
    @DisplayName("사용자가 쿠폰을 보유하고 있는지 확인 - 보유")
    void hasCoupon_true() {
        // given
        Long userId = 1L;
        Long couponId = 1L;
        UserCoupon userCoupon = new UserCoupon(1L, userId, couponId, false, LocalDateTime.now(), null);
        given(userCouponRepository.findByUserIdAndCouponId(userId, couponId))
                .willReturn(Optional.of(userCoupon));

        // when
        boolean result = userCouponService.hasCoupon(userId, couponId);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("사용자가 쿠폰을 보유하고 있지 않은 경우")
    void hasCoupon_false() {
        // given
        Long userId = 1L;
        Long couponId = 1L;
        given(userCouponRepository.findByUserIdAndCouponId(userId, couponId))
                .willReturn(Optional.empty());

        // when
        boolean result = userCouponService.hasCoupon(userId, couponId);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("사용자에게 쿠폰 발급")
    void issueCoupon() {
        // given
        Long userId = 1L;
        Long couponId = 1L;
        UserCoupon expectedCoupon = new UserCoupon(1L, userId, couponId, false, LocalDateTime.now(), null);
        given(userCouponRepository.issueUserCoupon(userId, couponId)).willReturn(expectedCoupon);

        // when
        UserCoupon result = userCouponService.issueCoupon(userId, couponId);

        // then
        assertThat(result).isEqualTo(expectedCoupon);
        verify(userCouponRepository).issueUserCoupon(userId, couponId);
    }

    @Test
    @DisplayName("쿠폰 유효성 검증 성공")
    void validateAndGetCoupon_success() {
        // given
        Long userCouponId = 1L;
        Long userId = 1L;
        Long couponId = 1L;

        UserCoupon userCoupon = new UserCoupon(userCouponId, userId, couponId, false, LocalDateTime.now(), null);
        Coupon coupon = new Coupon(couponId, "10% 할인", "RATE", 10, 100, 1, 0, LocalDateTime.now(), LocalDateTime.now().plusDays(30));

        given(userCouponRepository.findById(userCouponId)).willReturn(Optional.of(userCoupon));
        given(couponRepository.findById(couponId)).willReturn(Optional.of(coupon));

        // when
        UserCouponService.ValidatedCoupon result = userCouponService.validateAndGetCoupon(userCouponId, userId);

        // then
        assertThat(result.userCoupon()).isEqualTo(userCoupon);
        assertThat(result.coupon()).isEqualTo(coupon);
    }

    @Test
    @DisplayName("존재하지 않는 사용자 쿠폰 검증 시 예외 발생")
    void validateAndGetCoupon_userCouponNotFound() {
        // given
        Long userCouponId = 999L;
        Long userId = 1L;
        given(userCouponRepository.findById(userCouponId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userCouponService.validateAndGetCoupon(userCouponId, userId))
                .isInstanceOf(CouponException.class)
                .hasMessageContaining(USER_COUPON_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("본인의 쿠폰이 아닐 때 예외 발생")
    void validateAndGetCoupon_notOwned() {
        // given
        Long userCouponId = 1L;
        Long userId = 1L;
        Long differentUserId = 2L;

        UserCoupon userCoupon = new UserCoupon(userCouponId, differentUserId, 1L, false, LocalDateTime.now(), null);
        given(userCouponRepository.findById(userCouponId)).willReturn(Optional.of(userCoupon));

        // when & then
        assertThatThrownBy(() -> userCouponService.validateAndGetCoupon(userCouponId, userId))
                .isInstanceOf(CouponException.class)
                .hasMessageContaining(COUPON_NOT_OWNED.getMessage());
    }

    @Test
    @DisplayName("이미 사용된 쿠폰 검증 시 예외 발생")
    void validateAndGetCoupon_alreadyUsed() {
        // given
        Long userCouponId = 1L;
        Long userId = 1L;

        UserCoupon userCoupon = new UserCoupon(userCouponId, userId, 1L, true, LocalDateTime.now(), LocalDateTime.now());
        given(userCouponRepository.findById(userCouponId)).willReturn(Optional.of(userCoupon));

        // when & then
        assertThatThrownBy(() -> userCouponService.validateAndGetCoupon(userCouponId, userId))
                .isInstanceOf(CouponException.class)
                .hasMessageContaining(COUPON_ALREADY_USED.getMessage());
    }

    @Test
    @DisplayName("쿠폰이 존재하지 않을 때 예외 발생")
    void validateAndGetCoupon_couponNotFound() {
        // given
        Long userCouponId = 1L;
        Long userId = 1L;
        Long couponId = 999L;

        UserCoupon userCoupon = new UserCoupon(userCouponId, userId, couponId, false, LocalDateTime.now(), null);
        given(userCouponRepository.findById(userCouponId)).willReturn(Optional.of(userCoupon));
        given(couponRepository.findById(couponId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userCouponService.validateAndGetCoupon(userCouponId, userId))
                .isInstanceOf(CouponException.class)
                .hasMessageContaining(COUPON_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("쿠폰 사용 처리")
    void markAsUsed() {
        // given
        UserCoupon userCoupon = new UserCoupon(1L, 1L, 1L, false, LocalDateTime.now(), null);

        // when
        userCouponService.markAsUsed(userCoupon);

        // then
        assertThat(userCoupon.isUsed()).isTrue();
        assertThat(userCoupon.getUsedAt()).isNotNull();
        verify(userCouponRepository).update(userCoupon);
    }
}
