package com.sparta.ecommerce.domain.coupon.entity;

import com.sparta.ecommerce.domain.coupon.dto.CouponResponse;
import com.sparta.ecommerce.domain.coupon.exception.CouponException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static com.sparta.ecommerce.domain.coupon.exception.CouponErrorCode.COUPON_EXPIRED;
import static com.sparta.ecommerce.domain.coupon.exception.CouponErrorCode.COUPON_OUT_OF_STOCK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CouponTest {

    @Test
    @DisplayName("쿠폰 발급 가능 - 재고가 남아있음")
    void isIssuable_true() {
        // given
        Coupon coupon = new Coupon(
                1L,
                "10% 할인 쿠폰",
                "RATE",
                10,
                100,
                50,  // 발급 수량 < 총 수량
                0,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(30)
        );

        // when
        boolean result = coupon.isIssuable();

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("쿠폰 발급 불가능 - 재고 없음")
    void isIssuable_false() {
        // given
        Coupon coupon = new Coupon(
                1L,
                "10% 할인 쿠폰",
                "RATE",
                10,
                100,
                100,  // 발급 수량 == 총 수량
                0,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(30)
        );

        // when
        boolean result = coupon.isIssuable();

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("쿠폰 만료 여부 확인 - 만료됨")
    void isExpired_true() {
        // given
        Coupon coupon = new Coupon(
                1L,
                "만료된 쿠폰",
                "RATE",
                10,
                100,
                50,
                0,
                LocalDateTime.now().minusDays(60),
                LocalDateTime.now().minusDays(1)  // 어제 만료
        );

        // when
        boolean result = coupon.isExpired();

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("쿠폰 만료 여부 확인 - 유효함")
    void isExpired_false() {
        // given
        Coupon coupon = new Coupon(
                1L,
                "유효한 쿠폰",
                "RATE",
                10,
                100,
                50,
                0,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(30)
        );

        // when
        boolean result = coupon.isExpired();

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("쿠폰 만료일이 null인 경우 만료되지 않음")
    void isExpired_nullExpiresAt() {
        // given
        Coupon coupon = new Coupon(
                1L,
                "무기한 쿠폰",
                "RATE",
                10,
                100,
                50,
                0,
                LocalDateTime.now(),
                null  // 만료일 없음
        );

        // when
        boolean result = coupon.isExpired();

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("쿠폰 발급 가능 여부 검증 성공")
    void validateIssuable_success() {
        // given
        Coupon coupon = new Coupon(
                1L,
                "유효한 쿠폰",
                "RATE",
                10,
                100,
                50,
                0,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(30)
        );

        // when & then - 예외가 발생하지 않아야 함
        coupon.validateIssuable();
    }

    @Test
    @DisplayName("만료된 쿠폰 검증 시 예외 발생")
    void validateIssuable_expired() {
        // given
        Coupon coupon = new Coupon(
                1L,
                "만료된 쿠폰",
                "RATE",
                10,
                100,
                50,
                0,
                LocalDateTime.now().minusDays(60),
                LocalDateTime.now().minusDays(1)
        );

        // when & then
        assertThatThrownBy(coupon::validateIssuable)
                .isInstanceOf(CouponException.class)
                .hasMessageContaining(COUPON_EXPIRED.getMessage());
    }

    @Test
    @DisplayName("재고 없는 쿠폰 검증 시 예외 발생")
    void validateIssuable_outOfStock() {
        // given
        Coupon coupon = new Coupon(
                1L,
                "품절된 쿠폰",
                "RATE",
                10,
                100,
                100,
                0,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(30)
        );

        // when & then
        assertThatThrownBy(coupon::validateIssuable)
                .isInstanceOf(CouponException.class)
                .hasMessageContaining(COUPON_OUT_OF_STOCK.getMessage());
    }

    @Test
    @DisplayName("쿠폰 발급 수량 증가 성공")
    void increaseIssuedQuantity_success() {
        // given
        Coupon coupon = new Coupon(
                1L,
                "10% 할인 쿠폰",
                "RATE",
                10,
                100,
                50,
                0,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(30)
        );

        // when
        coupon.increaseIssuedQuantity();

        // then
        assertThat(coupon.getIssuedQuantity()).isEqualTo(51);
    }

    @Test
    @DisplayName("재고 없을 때 발급 수량 증가 시도 시 예외 발생")
    void increaseIssuedQuantity_outOfStock() {
        // given
        Coupon coupon = new Coupon(
                1L,
                "품절된 쿠폰",
                "RATE",
                10,
                100,
                100,
                0,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(30)
        );

        // when & then
        assertThatThrownBy(coupon::increaseIssuedQuantity)
                .isInstanceOf(CouponException.class)
                .hasMessageContaining(COUPON_OUT_OF_STOCK.getMessage());
    }

    @Test
    @DisplayName("쿠폰 사용 수량 증가")
    void increaseUsedQuantity() {
        // given
        Coupon coupon = new Coupon(
                1L,
                "10% 할인 쿠폰",
                "RATE",
                10,
                100,
                50,
                10,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(30)
        );

        // when
        coupon.increaseUsedQuantity();

        // then
        assertThat(coupon.getUsedQuantity()).isEqualTo(11);
    }

    @Test
    @DisplayName("비율(RATE) 할인 금액 계산")
    void calculateDiscount_rate() {
        // given
        Coupon coupon = new Coupon(
                1L,
                "10% 할인 쿠폰",
                "RATE",
                10,  // 10%
                100,
                50,
                0,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(30)
        );
        int totalAmount = 10000;

        // when
        int discount = coupon.calculateDiscount(totalAmount);

        // then
        assertThat(discount).isEqualTo(1000);  // 10000 * 10 / 100
    }

    @Test
    @DisplayName("금액(AMOUNT) 할인 금액 계산")
    void calculateDiscount_amount() {
        // given
        Coupon coupon = new Coupon(
                1L,
                "5000원 할인 쿠폰",
                "AMOUNT",
                5000,  // 5000원
                100,
                50,
                0,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(30)
        );
        int totalAmount = 20000;

        // when
        int discount = coupon.calculateDiscount(totalAmount);

        // then
        assertThat(discount).isEqualTo(5000);
    }

    @Test
    @DisplayName("소문자 할인 타입도 정상 처리")
    void calculateDiscount_lowerCaseType() {
        // given
        Coupon coupon = new Coupon(
                1L,
                "20% 할인 쿠폰",
                "rate",  // 소문자
                20,
                100,
                50,
                0,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(30)
        );
        int totalAmount = 10000;

        // when
        int discount = coupon.calculateDiscount(totalAmount);

        // then
        assertThat(discount).isEqualTo(2000);
    }

    @Test
    @DisplayName("알 수 없는 할인 타입일 경우 0 반환")
    void calculateDiscount_unknownType() {
        // given
        Coupon coupon = new Coupon(
                1L,
                "잘못된 쿠폰",
                "UNKNOWN",
                1000,
                100,
                50,
                0,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(30)
        );
        int totalAmount = 10000;

        // when
        int discount = coupon.calculateDiscount(totalAmount);

        // then
        assertThat(discount).isEqualTo(0);
    }

    @Test
    @DisplayName("Coupon을 CouponResponse로 변환")
    void from() {
        // given
        LocalDateTime createAt = LocalDateTime.of(2025, 1, 1, 0, 0);
        LocalDateTime expiresAt = LocalDateTime.of(2025, 12, 31, 23, 59);

        Coupon coupon = new Coupon(
                1L,
                "10% 할인 쿠폰",
                "RATE",
                10,
                100,
                50,
                10,
                createAt,
                expiresAt
        );

        // when
        CouponResponse response = coupon.from();

        // then
        assertThat(response.couponId()).isEqualTo(1L);
        assertThat(response.couponName()).isEqualTo("10% 할인 쿠폰");
        assertThat(response.discountType()).isEqualTo("RATE");
        assertThat(response.discountValue()).isEqualTo(10);
        assertThat(response.totalQuantity()).isEqualTo(100);
        assertThat(response.issuedQuantity()).isEqualTo(50);
        assertThat(response.createAt()).isEqualTo(createAt);
        assertThat(response.expiresAt()).isEqualTo(expiresAt);
    }

    @Test
    @DisplayName("만료일이 null인 Coupon을 CouponResponse로 변환")
    void from_nullExpiresAt() {
        // given
        LocalDateTime createAt = LocalDateTime.of(2025, 1, 1, 0, 0);

        Coupon coupon = new Coupon(
                2L,
                "무기한 쿠폰",
                "AMOUNT",
                5000,
                200,
                100,
                50,
                createAt,
                null  // 만료일 없음
        );

        // when
        CouponResponse response = coupon.from();

        // then
        assertThat(response.couponId()).isEqualTo(2L);
        assertThat(response.couponName()).isEqualTo("무기한 쿠폰");
        assertThat(response.discountType()).isEqualTo("AMOUNT");
        assertThat(response.discountValue()).isEqualTo(5000);
        assertThat(response.totalQuantity()).isEqualTo(200);
        assertThat(response.issuedQuantity()).isEqualTo(100);
        assertThat(response.createAt()).isEqualTo(createAt);
        assertThat(response.expiresAt()).isNull();
    }
}
