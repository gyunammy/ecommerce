package com.sparta.ecommerce.domain.cart.entity;

import com.sparta.ecommerce.domain.cart.dto.CartItemResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CartItemTest {

    @Test
    @DisplayName("CartItem을 CartItemResponse로 변환")
    void from() {
        // given
        LocalDateTime createAt = LocalDateTime.of(2025, 1, 1, 10, 0);
        LocalDateTime updateAt = LocalDateTime.of(2025, 1, 2, 15, 30);

        CartItem cartItem = new CartItem(
                1L,
                100L,
                200L,
                5,
                createAt,
                updateAt
        );

        // when
        CartItemResponse response = cartItem.from();

        // then
        assertThat(response.cartItemId()).isEqualTo(1L);
        assertThat(response.userId()).isEqualTo(100L);
        assertThat(response.productId()).isEqualTo(200L);
        assertThat(response.quantity()).isEqualTo(5);
        assertThat(response.createAt()).isEqualTo(createAt);
        assertThat(response.updatedAt()).isEqualTo(updateAt);
    }

    @Test
    @DisplayName("수량이 1인 CartItem 변환")
    void from_singleQuantity() {
        // given
        LocalDateTime now = LocalDateTime.now();

        CartItem cartItem = new CartItem(
                10L,
                1L,
                999L,
                1,
                now,
                now
        );

        // when
        CartItemResponse response = cartItem.from();

        // then
        assertThat(response.cartItemId()).isEqualTo(10L);
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.productId()).isEqualTo(999L);
        assertThat(response.quantity()).isEqualTo(1);
        assertThat(response.createAt()).isEqualTo(now);
        assertThat(response.updatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("수량이 많은 CartItem 변환")
    void from_largeQuantity() {
        // given
        LocalDateTime createAt = LocalDateTime.of(2025, 1, 1, 0, 0);
        LocalDateTime updateAt = LocalDateTime.of(2025, 1, 10, 12, 0);

        CartItem cartItem = new CartItem(
                5L,
                2L,
                300L,
                100,
                createAt,
                updateAt
        );

        // when
        CartItemResponse response = cartItem.from();

        // then
        assertThat(response.cartItemId()).isEqualTo(5L);
        assertThat(response.userId()).isEqualTo(2L);
        assertThat(response.productId()).isEqualTo(300L);
        assertThat(response.quantity()).isEqualTo(100);
        assertThat(response.createAt()).isEqualTo(createAt);
        assertThat(response.updatedAt()).isEqualTo(updateAt);
    }

    @Test
    @DisplayName("생성 시간과 수정 시간이 다른 CartItem 변환")
    void from_differentTimestamps() {
        // given
        LocalDateTime createAt = LocalDateTime.of(2025, 1, 1, 10, 0);
        LocalDateTime updateAt = LocalDateTime.of(2025, 1, 5, 14, 30);

        CartItem cartItem = new CartItem(
                3L,
                5L,
                150L,
                3,
                createAt,
                updateAt
        );

        // when
        CartItemResponse response = cartItem.from();

        // then
        assertThat(response.createAt()).isEqualTo(createAt);
        assertThat(response.updatedAt()).isEqualTo(updateAt);
        assertThat(response.updatedAt()).isAfter(response.createAt());
    }

    @Test
    @DisplayName("CartItem 생성 및 필드 검증")
    void create_cartItem() {
        // given
        Long cartItemId = 1L;
        Long userId = 100L;
        Long productId = 200L;
        Integer quantity = 3;
        LocalDateTime createAt = LocalDateTime.now();
        LocalDateTime updateAt = LocalDateTime.now();

        // when
        CartItem cartItem = new CartItem(
                cartItemId,
                userId,
                productId,
                quantity,
                createAt,
                updateAt
        );

        // then
        assertThat(cartItem.getCartItemId()).isEqualTo(cartItemId);
        assertThat(cartItem.getUserId()).isEqualTo(userId);
        assertThat(cartItem.getProductId()).isEqualTo(productId);
        assertThat(cartItem.getQuantity()).isEqualTo(quantity);
        assertThat(cartItem.getCreateAt()).isEqualTo(createAt);
        assertThat(cartItem.getUpdateAt()).isEqualTo(updateAt);
    }

    @Test
    @DisplayName("CartItem 필드 수정 가능 확인")
    void update_cartItem() {
        // given
        CartItem cartItem = new CartItem(
                1L,
                100L,
                200L,
                2,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        // when
        cartItem.setQuantity(5);
        LocalDateTime newUpdateAt = LocalDateTime.now();
        cartItem.setUpdateAt(newUpdateAt);

        // then
        assertThat(cartItem.getQuantity()).isEqualTo(5);
        assertThat(cartItem.getUpdateAt()).isEqualTo(newUpdateAt);
    }
}
