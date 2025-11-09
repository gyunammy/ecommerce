package com.sparta.ecommerce.application.cart;

import com.sparta.ecommerce.domain.cart.CartRepository;
import com.sparta.ecommerce.domain.cart.dto.CartItemResponse;
import com.sparta.ecommerce.domain.cart.entity.CartItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @InjectMocks
    private CartService cartService;

    @Test
    @DisplayName("사용자의 장바구니 아이템 조회 성공")
    void getCartItems_success() {
        // given
        Long userId = 1L;
        LocalDateTime now = LocalDateTime.now();

        CartItem item1 = new CartItem(1L, userId, 100L, 2, now, now);
        CartItem item2 = new CartItem(2L, userId, 200L, 3, now, now);
        CartItem item3 = new CartItem(3L, userId, 300L, 1, now, now);

        List<CartItem> cartItems = Arrays.asList(item1, item2, item3);
        given(cartRepository.findAllByUserId(userId)).willReturn(cartItems);

        // when
        List<CartItemResponse> result = cartService.getCartItems(userId);

        // then
        assertThat(result).hasSize(3);

        for (int i = 0; i < result.size(); i++) {
            CartItemResponse cartItemResponse = result.get(i);

            assertThat(cartItemResponse.cartItemId()).isEqualTo(cartItems.get(i).getCartItemId());
            assertThat(cartItemResponse.productId()).isEqualTo(cartItems.get(i).getProductId());
            assertThat(cartItemResponse.quantity()).isEqualTo(cartItems.get(i).getQuantity());
        }

        verify(cartRepository).findAllByUserId(userId);
    }

    @Test
    @DisplayName("장바구니가 비어있는 경우 빈 리스트 반환")
    void getCartItems_emptyCart() {
        // given
        Long userId = 1L;
        given(cartRepository.findAllByUserId(userId)).willReturn(Collections.emptyList());

        // when
        List<CartItemResponse> result = cartService.getCartItems(userId);

        // then
        assertThat(result).isEmpty();
        verify(cartRepository).findAllByUserId(userId);
    }

    @Test
    @DisplayName("장바구니에 단일 아이템만 있는 경우")
    void getCartItems_singleItem() {
        // given
        Long userId = 1L;
        LocalDateTime now = LocalDateTime.now();

        CartItem item = new CartItem(1L, userId, 100L, 5, now, now);
        List<CartItem> cartItems = Collections.singletonList(item);
        given(cartRepository.findAllByUserId(userId)).willReturn(cartItems);

        // when
        List<CartItemResponse> result = cartService.getCartItems(userId);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).cartItemId()).isEqualTo(1L);
        assertThat(result.get(0).userId()).isEqualTo(userId);
        assertThat(result.get(0).productId()).isEqualTo(100L);
        assertThat(result.get(0).quantity()).isEqualTo(5);
        verify(cartRepository).findAllByUserId(userId);
    }

    @Test
    @DisplayName("장바구니 비우기 성공")
    void clearCart_success() {
        // given
        Long userId = 1L;
        LocalDateTime now = LocalDateTime.now();
        CartItem item = new CartItem(1L, userId, 100L, 5, now, now);
        cartRepository.save(item);

        // when
        cartService.clearCart(userId);

        // then
        verify(cartRepository).deleteAllByUserId(userId);
        List<CartItemResponse> result = cartService.getCartItems(userId);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("비어있는 장바구니 비우기 호출")
    void clearCart_emptyCart() {
        // given
        Long userId = 1L;

        // when
        cartService.clearCart(userId);

        // then
        // 빈 장바구니도 deleteAll이 호출되어야 함
        verify(cartRepository).deleteAllByUserId(userId);
        List<CartItemResponse> result = cartService.getCartItems(userId);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("다른 사용자의 장바구니는 조회되지 않음")
    void getCartItems_differentUser() {
        // given
        Long userId1 = 1L;
        Long userId2 = 2L;
        LocalDateTime now = LocalDateTime.now();

        CartItem item1 = new CartItem(1L, userId1, 100L, 2, now, now);
        given(cartRepository.findAllByUserId(userId1)).willReturn(Collections.singletonList(item1));
        given(cartRepository.findAllByUserId(userId2)).willReturn(Collections.emptyList());

        // when
        List<CartItemResponse> result1 = cartService.getCartItems(userId1);
        List<CartItemResponse> result2 = cartService.getCartItems(userId2);

        // then
        assertThat(result1).hasSize(1);
        assertThat(result1.get(0).userId()).isEqualTo(userId1);
        assertThat(result2).isEmpty();
        verify(cartRepository).findAllByUserId(userId1);
        verify(cartRepository).findAllByUserId(userId2);
    }
}
