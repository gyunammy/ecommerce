package com.sparta.ecommerce.application.coupon;

import com.sparta.ecommerce.domain.coupon.entity.Coupon;
import com.sparta.ecommerce.domain.coupon.exception.CouponException;
import com.sparta.ecommerce.infrastructure.jpa.coupon.JpaCouponRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.sparta.ecommerce.domain.coupon.exception.CouponErrorCode.COUPON_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private JpaCouponRepository couponRepository;

    @InjectMocks
    private CouponService couponService;

    @Test
    @DisplayName("쿠폰 조회 성공")
    void getCoupon_success() {
        // given
        Coupon coupon = new Coupon(
                1L,
                "10% 할인 쿠폰",
                "RATE",
                10,
                100,
                0,
                0,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(30)
        );
        given(couponRepository.findByIdWithPessimisticLock(1L)).willReturn(Optional.of(coupon));

        // when
        Coupon result = couponService.getCouponForUpdate(1L);

        // then
        assertThat(result).isEqualTo(coupon);
        assertThat(result.getCouponName()).isEqualTo("10% 할인 쿠폰");
    }

    @Test
    @DisplayName("존재하지 않는 쿠폰 조회 시 예외 발생")
    void getCoupon_notFound() {
        // when & then
        assertThatThrownBy(() -> couponService.getCouponForUpdate(999L))
                .isInstanceOf(CouponException.class)
                .hasMessageContaining(COUPON_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("쿠폰 저장 성공")
    void saveCoupon() {
        // given
        Coupon coupon = new Coupon(
                1L,
                "5000원 할인 쿠폰",
                "AMOUNT",
                5000,
                50,
                0,
                0,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(15)
        );

        // when
        couponService.saveCoupon(coupon);

        // then
        verify(couponRepository).save(coupon);
    }
}
