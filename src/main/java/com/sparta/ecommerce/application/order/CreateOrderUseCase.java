package com.sparta.ecommerce.application.order;

import com.sparta.ecommerce.application.cart.CartService;
import com.sparta.ecommerce.application.coupon.UserCouponService;
import com.sparta.ecommerce.application.product.ProductService;
import com.sparta.ecommerce.application.user.UserService;
import com.sparta.ecommerce.domain.cart.dto.CartItemResponse;
import com.sparta.ecommerce.domain.cart.exception.CartException;
import com.sparta.ecommerce.domain.coupon.entity.UserCoupon;
import com.sparta.ecommerce.domain.product.entity.Product;
import com.sparta.ecommerce.domain.user.entity.User;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.sparta.ecommerce.domain.cart.exception.CartErrorCode.CART_IS_EMPTY;

/**
 * 주문 생성 유스케이스
 *
 * 장바구니 상품을 기반으로 주문을 생성합니다.
 * Redis MultiLock과 @Transactional을 조합하여 동시성 제어 및 원자성을 보장합니다.
 *
 * 동시성 제어 전략:
 *   1. Redis MultiLock 획득 (트랜잭션 밖) - DB 커넥션 고갈 방지
 *   2. 트랜잭션 시작 (락 안)
 *   3. 상품 조회 + 검증 + 상태 변경 (트랜잭션 안, 락 안) - TOCTOU 방지
 *   4. 트랜잭션 커밋
 *   5. 락 해제
 *
 * Self 주입 패턴:
 *   내부 메서드의 @Transactional이 동작하도록 self를 통해 프록시 호출
 */
@Service
public class CreateOrderUseCase {

    private final CartService cartService;
    private final UserService userService;
    private final UserCouponService userCouponService;
    private final OrderService orderService;
    private final ProductService productService;
    private final RedissonClient redissonClient;

    // Self 주입: @Transactional이 동작하도록 프록시를 통해 호출
    private final CreateOrderUseCase self;

    // 생성자 주입 (Self 주입 시 @Lazy 필수)
    public CreateOrderUseCase(
            CartService cartService,
            UserService userService,
            UserCouponService userCouponService,
            OrderService orderService,
            ProductService productService,
            RedissonClient redissonClient,
            @Lazy CreateOrderUseCase self  // 순환 참조 방지
    ) {
        this.cartService = cartService;
        this.userService = userService;
        this.userCouponService = userCouponService;
        this.orderService = orderService;
        this.productService = productService;
        this.redissonClient = redissonClient;
        this.self = self;
    }

    /**
     * 주문을 생성합니다.
     *
     * @param userId 사용자 ID
     * @param userCouponId 사용할 쿠폰 ID (null 가능)
     * @throws com.sparta.ecommerce.domain.user.exception.UserException 사용자를 찾을 수 없거나 포인트가 부족한 경우
     * @throws com.sparta.ecommerce.domain.product.exception.ProductException 상품을 찾을 수 없거나 재고가 부족한 경우
     * @throws com.sparta.ecommerce.domain.coupon.exception.CouponException 쿠폰이 유효하지 않은 경우
     * @throws com.sparta.ecommerce.domain.cart.exception.CartException 장바구니가 비어있는 경우
     */
    public void createOrder(Long userId, Long userCouponId) {
        // 사용자 및 장바구니 조회
        User user = userService.getUserById(userId);
        List<CartItemResponse> findCartItems = cartService.getCartItems(userId);
        if (findCartItems.isEmpty()) throw new CartException(CART_IS_EMPTY);

        // 상품 ID 추출 및 정렬 (데드락 방지)
        List<Long> productIds = findCartItems.stream()
                .map(CartItemResponse::productId)
                .distinct()
                .sorted()
                .toList();

        // Redis MultiLock 생성
        RLock[] locks = productIds.stream()
                .map(id -> redissonClient.getLock("product:lock:" + id))
                .toArray(RLock[]::new);
        RLock multiLock = redissonClient.getMultiLock(locks);

        try {
            boolean available = multiLock.tryLock(30, 10, TimeUnit.SECONDS);
            if (!available) {
                throw new IllegalStateException("상품 락을 획득할 수 없습니다");
            }

            // Self를 통해 호출하여 프록시를 거쳐 @Transactional 동작
            self.executeOrderTransaction(user, userCouponId, findCartItems, productIds);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("락 획득 중 인터럽트 발생", e);
        } finally {
            if (multiLock.isHeldByCurrentThread()) {
                multiLock.unlock();
            }
        }
    }

    /**
     * 트랜잭션 내에서 주문 생성 로직을 수행합니다.
     *
     * Redis 락이 획득된 상태에서 호출되며, 상품 조회부터 주문 생성까지 원자적으로 처리합니다.
     *
     * 주의: Self 주입 패턴 사용
     * - 반드시 self.executeOrderTransaction()으로 호출해야 @Transactional이 동작합니다.
     * - this.executeOrderTransaction()으로 호출하면 트랜잭션이 적용되지 않습니다.
     *
     * @param user 사용자
     * @param userCouponId 사용할 쿠폰 ID (null 가능)
     * @param findCartItems 장바구니 상품 목록
     * @param productIds 정렬된 상품 ID 목록
     */
    @Transactional
    public void executeOrderTransaction(
            User user,
            Long userCouponId,
            List<CartItemResponse> findCartItems,
            List<Long> productIds
    ) {
        // 상품 조회
        Map<Long, Product> productMap = productService.getProductMapByIds(productIds);

        // 검증
        validateStock(findCartItems, productMap);
        int totalAmount = calculateTotalAmount(findCartItems, productMap);
        CouponDiscountResult couponResult = processCouponDiscount(userCouponId, user.getUserId(), totalAmount);
        int finalAmount = calculateFinalAmount(totalAmount, couponResult.discountAmount());
        user.validateSufficientPoint(finalAmount);

        // 상태 변경
        if (couponResult.userCoupon() != null) {
            userCouponService.markAsUsed(couponResult.userCoupon());
        }
        user.deductPoint(finalAmount);
        userService.updateUser(user);
        decreaseStock(findCartItems, productMap);

        // 주문 생성
        orderService.createOrder(
                user.getUserId(),
                userCouponId,
                totalAmount,
                couponResult.discountAmount(),
                finalAmount,
                findCartItems,
                productMap
        );

        cartService.clearCart(user.getUserId());
    }

    /**
     * 장바구니 상품들의 재고를 검증합니다.
     */
    private void validateStock(List<CartItemResponse> cartItems, Map<Long, Product> productMap) {
        for (CartItemResponse cartItem : cartItems) {
            Product product = productMap.get(cartItem.productId());
            product.validateStock(cartItem.quantity());
        }
    }

    /**
     * 장바구니 상품들의 재고를 차감하고 저장합니다.
     */
    private void decreaseStock(List<CartItemResponse> cartItems, Map<Long, Product> productMap) {
        for (CartItemResponse cartItem : cartItems) {
            Product product = productMap.get(cartItem.productId());
            product.decreaseStock(cartItem.quantity());
            productService.updateProduct(product);
        }
    }

    /**
     * 쿠폰 검증 및 할인 금액을 계산합니다.
     */
    private CouponDiscountResult processCouponDiscount(Long userCouponId, Long userId, int totalAmount) {
        if (userCouponId == null) {
            return new CouponDiscountResult(null, 0);
        }

        UserCouponService.ValidatedCoupon validatedCoupon =
                userCouponService.validateAndGetCoupon(userCouponId, userId);

        int discountAmount = validatedCoupon.coupon().calculateDiscount(totalAmount);

        return new CouponDiscountResult(validatedCoupon.userCoupon(), discountAmount);
    }

    /**
     * 장바구니 총 금액을 계산합니다.
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
     * 최종 결제 금액을 계산합니다 (총액 - 할인액, 최소 0원).
     */
    private int calculateFinalAmount(int totalAmount, int discountAmount) {
        return Math.max(totalAmount - discountAmount, 0);
    }

    /**
     * 쿠폰 할인 처리 결과
     */
    private record CouponDiscountResult(UserCoupon userCoupon, int discountAmount) {
    }
}
