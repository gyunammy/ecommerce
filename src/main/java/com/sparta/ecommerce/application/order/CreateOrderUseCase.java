package com.sparta.ecommerce.application.order;

import com.sparta.ecommerce.application.cart.CartService;
import com.sparta.ecommerce.application.coupon.UserCouponService;
import com.sparta.ecommerce.application.order.event.OrderCreatedEvent;
import com.sparta.ecommerce.application.product.ProductService;
import com.sparta.ecommerce.application.user.UserService;
import com.sparta.ecommerce.common.transaction.TransactionHandler;
import com.sparta.ecommerce.domain.cart.dto.CartItemResponse;
import com.sparta.ecommerce.domain.cart.exception.CartException;
import com.sparta.ecommerce.domain.order.entity.Order;
import com.sparta.ecommerce.domain.product.entity.Product;
import com.sparta.ecommerce.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import static com.sparta.ecommerce.domain.cart.exception.CartErrorCode.CART_IS_EMPTY;

/**
 * 주문 생성 유스케이스
 *
 * 장바구니 상품을 기반으로 주문을 생성합니다.
 * 비동기 처리 + 최종 일관성(Eventually Consistent) 패턴을 사용합니다.
 *
 * 처리 흐름:
 *   1. 낙관적 검증 (락 없이 재고 확인)
 *   2. 주문을 PENDING 상태로 생성 (트랜잭션)
 *   3. OrderCreatedEvent를 Kafka로 발행
 *   4. [비동기] 재고 차감 리스너가 멀티락 획득 후 재고 차감
 *   5. [비동기] 성공 시 주문 상태를 COMPLETED로 변경
 *   6. [비동기] 실패 시 주문 상태를 FAILED로 변경 및 보상 트랜잭션
 *
 * 트랜잭션 제어:
 *   TransactionHandler를 통해 기술(트랜잭션)과 도메인 로직을 분리합니다.
 *   검증은 트랜잭션 밖에서 수행하여 트랜잭션 시간을 최소화합니다.
 *
 * 이벤트 기반 처리:
 *   주문 생성 완료 후 OrderCreatedEvent를 Kafka로 발행하여 부수 작업을 비동기로 처리합니다.
 *   재고 차감, 포인트 차감, 쿠폰 사용, 랭킹 업데이트 등이 Kafka Consumer에서 비동기로 처리됩니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreateOrderUseCase {

    private final CartService cartService;
    private final UserService userService;
    private final UserCouponService userCouponService;
    private final OrderService orderService;
    private final ProductService productService;
    private final TransactionHandler transactionHandler;
    private final KafkaTemplate<String, OrderCreatedEvent> orderCreatedKafkaTemplate;

    /**
     * 주문을 생성합니다.
     *
     * 낙관적 검증 후 주문을 PENDING 상태로 생성하고,
     * 실제 재고 차감은 비동기 이벤트 리스너에서 처리합니다.
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

        // 낙관적 검증 (락 없이 수행)
        OrderValidation validation = validateOrder(user, userCouponId, findCartItems, productIds);

        // TransactionHandler를 통해 트랜잭션 제어 (주문을 PENDING 상태로 생성)
        transactionHandler.execute(() ->
                executeOrderTransaction(user, userCouponId, findCartItems, validation)
        );
    }

    /**
     * 주문 검증을 수행하고 주문 생성에 필요한 데이터를 준비합니다.
     *
     * Redis 락이 획득된 상태에서 트랜잭션 밖에서 호출됩니다.
     * 재고, 금액, 쿠폰, 포인트 등을 검증하고, 검증 과정에서 계산된 데이터를 반환합니다.
     * 중복 조회/계산을 방지하기 위해 검증 결과와 함께 필요한 데이터를 반환합니다.
     *
     * @param user 사용자
     * @param userCouponId 사용할 쿠폰 ID (null 가능)
     * @param findCartItems 장바구니 상품 목록
     * @param productIds 정렬된 상품 ID 목록
     * @return 검증된 주문 데이터
     */
    private OrderValidation validateOrder(
            User user,
            Long userCouponId,
            List<CartItemResponse> findCartItems,
            List<Long> productIds
    ) {
        // 상품 조회
        Map<Long, Product> productMap = productService.getProductMapByIds(productIds);

        // 검증 - 각 도메인 서비스에 위임
        productService.validateStock(findCartItems, productMap);
        int totalAmount = productService.calculateTotalAmount(findCartItems, productMap);
        UserCouponService.CouponDiscountResult couponResult =
                userCouponService.validateAndCalculateDiscount(userCouponId, user.getUserId(), totalAmount);
        int finalAmount = calculateFinalAmount(totalAmount, couponResult.discountAmount());
        user.validateSufficientPoint(finalAmount);

        // 검증 과정에서 계산된 데이터를 반환 (중복 조회/계산 방지)
        return new OrderValidation(productMap, totalAmount, couponResult.discountAmount(), finalAmount);
    }

    /**
     * 주문 생성 트랜잭션을 수행합니다.
     *
     * Redis 락이 획득된 상태에서 TransactionHandler를 통해 트랜잭션 내에서 호출됩니다.
     * 주문 생성 및 장바구니 삭제만 수행하고, 포인트 차감, 재고 차감, 쿠폰 사용은 이벤트로 처리합니다.
     *
     * @param user 사용자
     * @param userCouponId 사용할 쿠폰 ID (null 가능)
     * @param findCartItems 장바구니 상품 목록
     * @param validation 검증된 주문 데이터
     * @return 생성된 주문
     */
    private Order executeOrderTransaction(
            User user,
            Long userCouponId,
            List<CartItemResponse> findCartItems,
            OrderValidation validation
    ) {
        // 주문 생성
        Order createdOrder = orderService.createOrder(
                user.getUserId(),
                userCouponId,
                validation.totalAmount,
                validation.discountAmount,
                validation.finalAmount,
                findCartItems,
                validation.productMap
        );

        cartService.clearCart(user.getUserId());

        // 주문 생성 완료 이벤트를 Kafka로 발행 (포인트 차감, 재고 차감, 쿠폰 사용, 랭킹 업데이트는 Kafka Consumer에서 처리)
        OrderCreatedEvent event = new OrderCreatedEvent(
                user.getUserId(),
                createdOrder.getOrderId(),
                userCouponId,
                validation.finalAmount,
                findCartItems
        );
        orderCreatedKafkaTemplate.send("order-created-topic", event);
        log.info("주문 생성 이벤트 Kafka 발행 - orderId: {}, userId: {}",
                createdOrder.getOrderId(), user.getUserId());

        return createdOrder;
    }

    /**
     * 최종 결제 금액을 계산합니다 (총액 - 할인액, 최소 0원).
     */
    private int calculateFinalAmount(int totalAmount, int discountAmount) {
        return Math.max(totalAmount - discountAmount, 0);
    }

    /**
     * 주문 검증 결과 및 주문 생성에 필요한 데이터
     *
     * 검증 과정에서 조회/계산된 데이터를 담아 중복 호출을 방지합니다.
     */
    private record OrderValidation(
            Map<Long, Product> productMap,
            int totalAmount,
            int discountAmount,
            int finalAmount
    ) {
    }
}
