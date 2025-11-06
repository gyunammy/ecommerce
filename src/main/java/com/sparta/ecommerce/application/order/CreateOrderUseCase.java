package com.sparta.ecommerce.application.order;

import com.sparta.ecommerce.application.cart.CartService;
import com.sparta.ecommerce.application.coupon.UserCouponService;
import com.sparta.ecommerce.application.product.ProductService;
import com.sparta.ecommerce.application.user.UserService;
import com.sparta.ecommerce.domain.cart.dto.CartItemResponse;
import com.sparta.ecommerce.domain.cart.exception.CartException;
import com.sparta.ecommerce.domain.coupon.entity.Coupon;
import com.sparta.ecommerce.domain.coupon.entity.UserCoupon;
import com.sparta.ecommerce.domain.order.dto.OrderDto;
import com.sparta.ecommerce.domain.order.entity.Order;
import com.sparta.ecommerce.domain.product.Product;
import com.sparta.ecommerce.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import static com.sparta.ecommerce.domain.cart.exception.CartErrorCode.CART_IS_EMPTY;

/**
 * 주문 생성 유스케이스
 *
 * 사용자의 장바구니 상품을 기반으로 주문을 생성하는 비즈니스 로직을 처리합니다.
 * 주문 생성 과정에서 재고 검증, 쿠폰 적용, 포인트 차감 등을 수행합니다.
 *
 * 주문 생성 흐름:
 *   - 사용자 및 장바구니 조회
 *   - 상품 재고 검증
 *   - 주문 금액 계산
 *   - 쿠폰 할인 적용
 *   - 사용자 포인트 검증 및 차감
 *   - 주문 생성 및 재고 차감
 */
@Service
@RequiredArgsConstructor
public class CreateOrderUseCase {

    private final CartService cartService;
    private final UserService userService;
    private final UserCouponService userCouponService;
    private final OrderService orderService;
    private final ProductService productService;

    /**
     * 주문을 생성합니다.
     *
     * 사용자의 장바구니 상품을 기반으로 주문을 생성하고,
     * 재고, 포인트, 쿠폰 유효성을 검증한 후 주문을 확정합니다.
     *
     * @param orderDto 주문 생성에 필요한 정보 (사용자 ID, 쿠폰 ID 등)
     * @throws com.sparta.ecommerce.domain.user.exception.UserException 사용자를 찾을 수 없거나 포인트가 부족한 경우
     * @throws com.sparta.ecommerce.domain.product.exception.ProductException 상품을 찾을 수 없거나 재고가 부족한 경우
     * @throws com.sparta.ecommerce.domain.coupon.exception.CouponException 쿠폰이 유효하지 않은 경우
     */

    public synchronized void createOrder(Long userId, Long userCouponId) {
        // === 1단계: 모든 검증 ===

        // 1-1. 사용자 유효성 검증
        User user = userService.getUserById(userId);

        // 1-2. 장바구니 조회
        List<CartItemResponse> findCartItems = cartService.getCartItems(userId);

        // 1-3. 빈 장바구니 체크
        if (findCartItems.isEmpty()) throw new CartException(CART_IS_EMPTY);

        // 1-4. 상품 정보 조회 및 Map 변환 (상품 존재 여부 검증 포함)
        Map<Long, Product> productMap = productService.getProductMap(findCartItems);

        // 1-5. 재고 검증 (도메인 모델 사용)
        validateStock(findCartItems, productMap);

        // 1-6. 장바구니 총액 계산
        int totalAmount = calculateTotalAmount(findCartItems, productMap);

        // 1-7. 쿠폰 처리 및 할인 계산
        CouponDiscountResult couponResult = processCouponDiscount(userCouponId, userId, totalAmount);

        // 1-8. 최종 결제 금액 계산
        int finalAmount = calculateFinalAmount(totalAmount, couponResult.discountAmount());

        // 1-9. 포인트 검증 (도메인 모델 사용)
        user.validateSufficientPoint(finalAmount);

        // === 2단계: 상태 변경 (주문 생성 전에) ===

        // 2-1. 쿠폰 사용 처리
        if (couponResult.userCoupon() != null) {
            userCouponService.markAsUsed(couponResult.userCoupon());
        }

        // 2-2. 포인트 차감
        user.deductPoint(finalAmount);
        userService.updateUser(user);

        // 2-3. 재고 차감
        decreaseStock(findCartItems, productMap);

        // === 3단계: 주문 생성 (모든 상태 변경 후 마지막에) ===
        Order order = orderService.createOrder(
                userId,
                userCouponId,
                totalAmount,
                couponResult.discountAmount(),
                finalAmount,  // 실제 사용한 포인트
                findCartItems,
                productMap
        );

        // === 4단계: 장바구니 비우기 ===
        cartService.clearCart(userId);

        // TODO: 외부로 주문완료 데이터 전송
    }

    /**
     * 재고 검증
     * 도메인 모델을 사용하여 각 상품의 재고를 검증합니다.
     *
     * @param cartItems 장바구니 상품 목록
     * @param productMap 상품 ID를 키로 하는 상품 정보 Map
     * @throws com.sparta.ecommerce.domain.product.exception.ProductException 재고가 부족한 경우
     */
    private void validateStock(List<CartItemResponse> cartItems, Map<Long, Product> productMap) {
        for (CartItemResponse cartItem : cartItems) {
            Product product = productMap.get(cartItem.productId());
            product.validateStock(cartItem.quantity());
        }
    }

    /**
     * 재고 차감
     * 도메인 모델을 사용하여 각 상품의 재고를 차감하고 저장합니다.
     *
     * @param cartItems 장바구니 상품 목록
     * @param productMap 상품 ID를 키로 하는 상품 정보 Map
     */
    private void decreaseStock(List<CartItemResponse> cartItems, Map<Long, Product> productMap) {
        for (CartItemResponse cartItem : cartItems) {
            Product product = productMap.get(cartItem.productId());
            product.decreaseStock(cartItem.quantity());
            productService.updateProduct(product);
        }
    }

    /**
     * 쿠폰을 검증하고 할인 금액을 계산합니다.
     *
     * 쿠폰 ID가 제공된 경우, 쿠폰의 유효성을 검증하고 할인 금액을 계산합니다.
     * 쿠폰이 없는 경우 할인 금액 0으로 반환합니다.
     *
     * @param userCouponId 사용자 쿠폰 ID (null 가능)
     * @param userId 사용자 ID
     * @param totalAmount 총 주문 금액
     * @return 쿠폰 정보와 할인 금액을 포함한 결과 객체
     * @throws com.sparta.ecommerce.domain.coupon.exception.CouponException 쿠폰이 유효하지 않은 경우
     */
    private CouponDiscountResult processCouponDiscount(Long userCouponId, Long userId, int totalAmount) {
        if (userCouponId == null) {
            return new CouponDiscountResult(null, 0);
        }

        UserCouponService.ValidatedCoupon validatedCoupon =
                userCouponService.validateAndGetCoupon(userCouponId, userId);

        UserCoupon userCoupon = validatedCoupon.userCoupon();
        Coupon coupon = validatedCoupon.coupon();

        int discountAmount = coupon.calculateDiscount(totalAmount);

        return new CouponDiscountResult(userCoupon, discountAmount);
    }

    /**
     * 장바구니 상품들의 총액 계산
     *
     * @param cartItems 장바구니 아이템들
     * @param productMap 상품 정보 맵
     * @return 총액
     */
    private int calculateTotalAmount(List<CartItemResponse> cartItems, Map<Long, Product> productMap) {
        return cartItems.stream()
                .mapToInt(cartItem -> {
                    Product product = productMap.get(cartItem.productId());
                    return product.getPrice() * cartItem.quantity();
                })
                .sum();
    }

    /**
     * 최종 결제 금액 계산 (총액 - 할인액)
     *
     * @param totalAmount 총액
     * @param discountAmount 할인액
     * @return 최종 결제 금액
     */
    private int calculateFinalAmount(int totalAmount, int discountAmount) {
        int finalAmount = totalAmount - discountAmount;
        return Math.max(finalAmount, 0);  // 최소 0원
    }

    /**
     * 쿠폰 할인 처리 결과를 담는 불변 객체
     *
     * @param userCoupon 사용된 사용자 쿠폰 (null 가능)
     * @param discountAmount 할인 금액
     */
    private record CouponDiscountResult(UserCoupon userCoupon, int discountAmount) {
    }
}
