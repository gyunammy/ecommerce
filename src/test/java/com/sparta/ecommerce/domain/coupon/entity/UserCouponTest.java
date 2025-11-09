package com.sparta.ecommerce.domain.coupon.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class UserCouponTest {

    @Test
    @DisplayName("쿠폰 사용 처리 - 사용 여부와 사용 시간이 설정됨")
    void markAsUsed() {
        // given
        UserCoupon userCoupon = new UserCoupon(
                1L,
                1L,
                1L,
                false,
                LocalDateTime.now(),
                null
        );

        // when
        LocalDateTime beforeMark = LocalDateTime.now();
        userCoupon.markAsUsed();
        LocalDateTime afterMark = LocalDateTime.now();

        // then
        assertThat(userCoupon.isUsed()).isTrue();
        assertThat(userCoupon.getUsedAt()).isNotNull();
        assertThat(userCoupon.getUsedAt()).isBetween(beforeMark, afterMark);
    }

    @Test
    @DisplayName("이미 사용된 쿠폰 재사용 처리")
    void markAsUsed_alreadyUsed() {
        // given
        LocalDateTime firstUsedAt = LocalDateTime.now().minusHours(1);
        UserCoupon userCoupon = new UserCoupon(
                1L,
                1L,
                1L,
                true,
                LocalDateTime.now().minusDays(1),
                firstUsedAt
        );

        // when
        userCoupon.markAsUsed();

        // then
        assertThat(userCoupon.isUsed()).isTrue();
        assertThat(userCoupon.getUsedAt()).isNotEqualTo(firstUsedAt);
        assertThat(userCoupon.getUsedAt()).isAfter(firstUsedAt);
    }

    @Test
    @DisplayName("미사용 쿠폰 생성")
    void create_unusedCoupon() {
        // given & when
        LocalDateTime issuedAt = LocalDateTime.now();
        UserCoupon userCoupon = new UserCoupon(
                1L,
                1L,
                1L,
                false,
                issuedAt,
                null
        );

        // then
        assertThat(userCoupon.getUserCouponId()).isEqualTo(1L);
        assertThat(userCoupon.getUserId()).isEqualTo(1L);
        assertThat(userCoupon.getCouponId()).isEqualTo(1L);
        assertThat(userCoupon.isUsed()).isFalse();
        assertThat(userCoupon.getIssuedAt()).isEqualTo(issuedAt);
        assertThat(userCoupon.getUsedAt()).isNull();
    }
}
