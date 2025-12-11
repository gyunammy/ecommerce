package com.sparta.ecommerce.application.order;

import com.sparta.ecommerce.application.coupon.event.CouponUsageFailedEvent;
import com.sparta.ecommerce.application.order.event.OrderCompletionCleanupEvent;
import com.sparta.ecommerce.application.product.event.StockDeductionFailedEvent;
import com.sparta.ecommerce.application.product.event.StockRestoreEvent;
import com.sparta.ecommerce.application.user.event.PointDeductionFailedEvent;
import com.sparta.ecommerce.application.user.event.PointRestoreEvent;
import com.sparta.ecommerce.domain.cart.dto.CartItemResponse;
import com.sparta.ecommerce.domain.order.OrderItemRepository;
import com.sparta.ecommerce.domain.order.OrderRepository;
import com.sparta.ecommerce.domain.order.entity.Order;
import com.sparta.ecommerce.domain.order.entity.OrderItem;
import com.sparta.ecommerce.domain.order.exception.OrderException;
import com.sparta.ecommerce.domain.product.entity.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.sparta.ecommerce.domain.order.exception.OrderErrorCode.ORDER_NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 주문 생성
     *
     * 주문을 PENDING 상태로 생성합니다.
     * 실제 재고 차감, 포인트 차감, 쿠폰 사용은 이벤트 리스너에서 비동기로 처리됩니다.
     *
     * @param userId 사용자 ID
     * @param userCouponId 사용자 쿠폰 ID (nullable)
     * @param totalAmount 총 주문 금액
     * @param discountAmount 할인 금액
     * @param usedPoint 사용한 포인트 (최종 결제 금액)
     * @param cartItems 장바구니 아이템들
     * @param productMap 상품 정보 맵
     * @return 생성된 주문 (PENDING 상태)
     */
    @Transactional
    public Order createOrder(
            Long userId,
            Long userCouponId,
            int totalAmount,
            int discountAmount,
            int usedPoint,
            List<CartItemResponse> cartItems,
            Map<Long, Product> productMap
    ) {
        LocalDateTime now = LocalDateTime.now();

        // 1. Order 엔티티 생성 (PENDING 상태)
        Order order = new Order();
        order.setUserId(userId);
        order.setUserCouponId(userCouponId);
        order.setTotalAmount(totalAmount);
        order.setDiscountAmount(discountAmount);
        order.setUsedPoint(usedPoint);  // 실제 사용한 포인트 기록
        order.setStatus("PENDING");  // 주문 접수 상태
        order.setCreatedAt(now);

        // 주문 저장
        Order savedOrder = orderRepository.save(order);

        // 2. OrderItem 엔티티들 생성
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItemResponse cartItem : cartItems) {
            Product product = productMap.get(cartItem.productId());

            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(savedOrder.getOrderId());
            orderItem.setProductId(product.getProductId());
            orderItem.setProductName(product.getProductName());
            orderItem.setDescription(product.getDescription());
            orderItem.setQuantity(cartItem.quantity());
            orderItem.setPrice(product.getPrice());
            orderItem.setCreatedAt(now);

            orderItems.add(orderItem);
        }

        // 주문 아이템들 저장
        orderItemRepository.saveAll(orderItems);

        return savedOrder;
    }

    /**
     * 주문 조회
     * @param orderId 주문 ID
     * @return 주문 엔티티
     * @throws OrderException 주문을 찾을 수 없는 경우
     */
    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderException(ORDER_NOT_FOUND));
    }

    /**
     * 주문 수정
     * @param order 수정할 주문 엔티티
     * @return 수정된 주문 엔티티
     */
    @Transactional
    public Order updateOrder(Order order) {
        return orderRepository.save(order);
    }

    /**
     * 주문을 실패 상태로 변경합니다.
     * 이 작업은 멱등성을 가집니다. 이미 실패한 주문은 추가 작업을 수행하지 않습니다.
     *
     * @param orderId 실패 처리할 주문 ID
     * @return 상태 변경에 성공했는지 여부 (이미 실패 상태인 경우 false 반환)
     */
    @Transactional
    public boolean failOrder(Long orderId) {
        try {
            Order order = getOrderById(orderId);
            if (order.getStatus().equals("FAILED")) {
                log.info("이미 실패 처리된 주문입니다. OrderId: {}", orderId);
                return false;
            }
            order.setStatus("FAILED");
            updateOrder(order);
            log.warn("주문 상태를 FAILED로 변경했습니다. OrderId: {}", orderId);
            return true;
        } catch (Exception e) {
            log.error("주문 실패 처리 중 오류 발생. OrderId: {}, Error: {}", orderId, e.getMessage(), e);
            // 이 단계에서 예외가 발생하면 심각한 문제이므로, 롤백 후 별도 조치가 필요.
            throw new RuntimeException("주문 실패 상태 변경 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 재고 차감 실패 시 보상 트랜잭션
     *
     * 재고 차감 실패 시 주문을 FAILED 상태로 변경하고 추적 정보를 정리합니다.
     * 재고, 포인트, 쿠폰 모두 차감/사용 전이므로 복구할 것이 없습니다.
     *
     * @param event 재고 차감 실패 이벤트
     */
    @Transactional
    public void handleStockDeductionFailure(StockDeductionFailedEvent event) {
        log.warn("재고 차감 실패 이벤트 수신 - OrderId: {}, Reason: {}",
                event.orderId(), event.errorMessage());
        boolean isFailed = failOrder(event.orderId());

        if (isFailed) {
            log.info("주문 실패 처리 후 보상 이벤트를 발행합니다. OrderId: {}", event.orderId());
            // 재고, 포인트, 쿠폰 모두 차감/사용 전이므로 OrderCompletionCleanupEvent만 발행
            eventPublisher.publishEvent(new OrderCompletionCleanupEvent(event.orderId()));
            log.info("보상 이벤트 발행 완료 (재고 차감 실패) - OrderId: {}", event.orderId());
        }
    }

    /**
     * 포인트 차감 실패 시 보상 트랜잭션
     *
     * 포인트 차감 실패 시 주문을 FAILED 상태로 변경하고 재고를 복구합니다.
     * 재고는 이미 차감되었으므로 복구가 필요합니다.
     *
     * @param event 포인트 차감 실패 이벤트
     */
    @Transactional
    public void handlePointDeductionFailure(PointDeductionFailedEvent event) {
        log.warn("포인트 차감 실패 이벤트 수신 - OrderId: {}, Reason: {}",
                event.orderId(), event.errorMessage());
        boolean isFailed = failOrder(event.orderId());

        if (isFailed) {
            log.info("주문 실패 처리 후 보상 이벤트를 발행합니다. OrderId: {}", event.orderId());
            eventPublisher.publishEvent(new OrderCompletionCleanupEvent(event.orderId()));
            eventPublisher.publishEvent(new StockRestoreEvent(event.cartItems())); // 재고는 복구 필요
            log.info("보상 이벤트 발행 완료 (포인트 차감 실패) - OrderId: {}", event.orderId());
        }
    }

    /**
     * 쿠폰 사용 실패 시 보상 트랜잭션
     *
     * 쿠폰 사용 실패 시 주문을 FAILED 상태로 변경하고 포인트와 재고를 복구합니다.
     * 재고와 포인트는 이미 차감되었으므로 복구가 필요합니다.
     *
     * @param event 쿠폰 사용 실패 이벤트
     */
    @Transactional
    public void handleCouponUsageFailure(CouponUsageFailedEvent event) {
        log.warn("쿠폰 사용 실패 이벤트 수신 - OrderId: {}, Reason: {}",
                event.orderId(), event.errorMessage());
        boolean isFailed = failOrder(event.orderId());

        if (isFailed) {
            log.info("주문 실패 처리 후 보상 이벤트를 발행합니다. OrderId: {}", event.orderId());
            eventPublisher.publishEvent(new OrderCompletionCleanupEvent(event.orderId()));
            eventPublisher.publishEvent(new PointRestoreEvent(event.userId(), event.finalAmount()));
            eventPublisher.publishEvent(new StockRestoreEvent(event.cartItems()));
            log.info("보상 이벤트 발행 완료 (쿠폰 사용 실패) - OrderId: {}", event.orderId());
        }
    }
}
