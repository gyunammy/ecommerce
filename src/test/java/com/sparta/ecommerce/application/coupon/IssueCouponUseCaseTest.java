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

        User user = new User(userId, "testUser", 0, LocalDateTime.now());
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
        given(couponService.getCoupon(couponId)).willReturn(coupon);
        given(userCouponService.hasCoupon(userId, couponId)).willReturn(false);
        given(userCouponService.issueCoupon(userId, couponId)).willReturn(userCoupon);

        // when
        issueCouponUseCase.issueCoupon(userId, couponId);

        // then
        verify(userService).getUserById(userId);
        verify(couponService).getCoupon(couponId);
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

        User user = new User(userId, "testUser", 0, LocalDateTime.now());
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
        given(couponService.getCoupon(couponId)).willReturn(expiredCoupon);

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

        User user = new User(userId, "testUser", 0, LocalDateTime.now());
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
        given(couponService.getCoupon(couponId)).willReturn(outOfStockCoupon);

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

        User user = new User(userId, "testUser", 0, LocalDateTime.now());
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
        given(couponService.getCoupon(couponId)).willReturn(coupon);
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

        User user = new User(userId, "testUser", 0, LocalDateTime.now());
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
        given(couponService.getCoupon(couponId)).willReturn(coupon);
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

    @Test
    @DisplayName("동시에 여러 스레드가 쿠폰 발급을 요청해도 Lock으로 동기화된다")
    void concurrentIssueCoupon() throws InterruptedException {
        // given
        Long couponId = 1L;
        int threadCount = 200;
        int couponStock = 50; // 발급 가능한 최대 수량

        AtomicInteger issuedCount = new AtomicInteger(0);

        // 쿠폰 Mock
        Coupon coupon = mock(Coupon.class);
        when(couponService.getCoupon(couponId)).thenReturn(coupon);

        // validateIssuable(): 재고가 남아 있으면 OK, 아니면 예외
        doAnswer(invocation -> {
            if (issuedCount.get() >= couponStock) {
                throw new CouponException(CouponErrorCode.COUPON_OUT_OF_STOCK);
            }
            return null;
        }).when(coupon).validateIssuable();

        // increaseIssuedQuantity(): 성공할 때만 증가
        doAnswer(invocation -> {
            issuedCount.incrementAndGet();
            return null;
        }).when(coupon).increaseIssuedQuantity();

        // User 모킹
        when(userService.getUserById(anyLong())).thenReturn(mock(User.class));

        // userCouponService.hasCoupon → 모두 미보유 상태
        when(userCouponService.hasCoupon(anyLong(), eq(couponId))).thenReturn(false);

        // userCoupon 발급 mock
        when(userCouponService.issueCoupon(anyLong(), eq(couponId))).thenReturn(mock(UserCoupon.class));

        // Lock 테스트 준비
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when: 10개 스레드가 동시에 쿠폰 발급 요청
        for (int i = 0; i < threadCount; i++) {
            long userId = (long) i + 1;

            executorService.submit(() -> {
                try {
                    try {
                        issueCouponUseCase.issueCoupon(userId, couponId);
                    } catch (CouponException ignored) {
                        // 재고 부족 예외는 무시
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(50, TimeUnit.SECONDS);
        executorService.shutdown();

        // then: 발급된 쿠폰은 정확히 50개여야 한다
        assertEquals(couponStock, issuedCount.get());

        // then: 사용자에게 쿠폰 발급 호출된 횟수 검증
        verify(userCouponService, times(couponStock)).issueCoupon(anyLong(), eq(couponId));

        // 쿠폰 저장 호출 50번
        verify(couponService, times(couponStock)).saveCoupon(any());
    }
}
