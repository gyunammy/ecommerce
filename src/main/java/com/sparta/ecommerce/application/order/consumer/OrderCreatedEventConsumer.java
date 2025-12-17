package com.sparta.ecommerce.application.order.consumer;

import com.sparta.ecommerce.application.coupon.UserCouponService;
import com.sparta.ecommerce.application.coupon.event.CouponRestoreEvent;
import com.sparta.ecommerce.application.order.OrderService;
import com.sparta.ecommerce.application.order.event.OrderCreatedEvent;
import com.sparta.ecommerce.application.product.ProductService;
import com.sparta.ecommerce.application.product.event.StockRestoreEvent;
import com.sparta.ecommerce.application.user.UserService;
import com.sparta.ecommerce.application.user.event.PointRestoreEvent;
import com.sparta.ecommerce.domain.cart.dto.CartItemResponse;
import com.sparta.ecommerce.domain.product.ProductRankingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 주문 생성 이벤트 Kafka Consumer
 *
 * Kafka에서 주문 생성 이벤트 메시지를 받아서 후속 처리를 수행합니다.
 * - 재고 차감
 * - 포인트 차감
 * - 쿠폰 사용
 * - 상품 랭킹 업데이트
 *
 * 각 단계에서 실패 시 Kafka를 통해 보상 트랜잭션을 발행합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreatedEventConsumer {

    private final ProductService productService;
    private final UserService userService;
    private final UserCouponService userCouponService;
    private final ProductRankingRepository productRankingRepository;
    private final OrderService orderService;
    private final KafkaTemplate<String, StockRestoreEvent> stockRestoreKafkaTemplate;
    private final KafkaTemplate<String, PointRestoreEvent> pointRestoreKafkaTemplate;
    private final KafkaTemplate<String, CouponRestoreEvent> couponRestoreKafkaTemplate;

    /**
     * 주문 생성 이벤트 메시지를 소비합니다.
     *
     * 주문 생성 후 필요한 모든 후속 처리를 순차적으로 수행합니다.
     * 각 단계에서 실패 시 보상 트랜잭션을 Kafka를 통해 발행합니다.
     *
     * @param event 주문 생성 이벤트 정보
     */
    @KafkaListener(
            topics = "order-created-topic",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "orderCreatedKafkaListenerContainerFactory"
    )
    public void consumeOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("주문 생성 이벤트 수신 - orderId: {}, userId: {}",
                event.orderId(), event.userId());

        boolean stockDecreased = false;
        boolean pointDeducted = false;
        boolean couponUsed = false;

        try {
            // 1. 재고 차감 처리 (멀티락 포함)
            log.debug("재고 차감 처리 시작 - orderId: {}", event.orderId());
            productService.decreaseStockWithLock(event.orderId(), event.userId(), event.cartItems());
            stockDecreased = true;
            log.debug("재고 차감 완료 - orderId: {}", event.orderId());

            // 2. 포인트 차감 처리
            log.debug("포인트 차감 처리 시작 - orderId: {}", event.orderId());
            userService.deductPointForOrder(event);
            pointDeducted = true;
            log.debug("포인트 차감 완료 - orderId: {}", event.orderId());

            // 3. 쿠폰 사용 처리
            log.debug("쿠폰 사용 처리 시작 - orderId: {}", event.orderId());
            userCouponService.processCouponUsage(event);
            couponUsed = (event.userCouponId() != null);  // 쿠폰이 있는 경우에만 true
            log.debug("쿠폰 사용 완료 - orderId: {}", event.orderId());

            // 4. 상품 랭킹 업데이트
            log.debug("상품 랭킹 업데이트 시작 - orderId: {}", event.orderId());
            for (CartItemResponse cartItem : event.cartItems()) {
                productRankingRepository.incrementSalesCount(
                        cartItem.productId(),
                        cartItem.quantity()
                );
            }
            log.debug("상품 랭킹 업데이트 완료 - orderId: {}", event.orderId());

            // 5. 주문 상태를 COMPLETED로 변경
            log.debug("주문 완료 처리 시작 - orderId: {}", event.orderId());
            orderService.completeOrder(event.orderId());
            log.info("주문 완료 처리 완료 - orderId: {}, status: COMPLETED", event.orderId());

            log.info("주문 생성 이벤트 처리 완료 - orderId: {}, userId: {}",
                    event.orderId(), event.userId());

        } catch (Exception e) {
            log.error("주문 생성 이벤트 처리 실패 - orderId: {}, userId: {}, error: {}",
                    event.orderId(), event.userId(), e.getMessage(), e);

            // 보상 트랜잭션 처리: 실패 시점에 따라 복구할 항목 결정
            handleCompensation(event, stockDecreased, pointDeducted, couponUsed);
        }
    }

    /**
     * 보상 트랜잭션 처리
     *
     * 실패 시점에 따라 복구가 필요한 항목들을 Kafka로 발행합니다.
     *
     * @param event 주문 생성 이벤트
     * @param stockDecreased 재고 차감 완료 여부
     * @param pointDeducted 포인트 차감 완료 여부
     * @param couponUsed 쿠폰 사용 완료 여부
     */
    private void handleCompensation(OrderCreatedEvent event, boolean stockDecreased, boolean pointDeducted, boolean couponUsed) {
        try {
            log.warn("보상 트랜잭션 시작 - orderId: {}, stockDecreased: {}, pointDeducted: {}, couponUsed: {}",
                    event.orderId(), stockDecreased, pointDeducted, couponUsed);

            // 주문 상태를 FAILED로 변경
            orderService.failOrder(event.orderId());

            // 쿠폰 사용이 완료된 경우 쿠폰 복구 (역순으로 복구)
            if (couponUsed && event.userCouponId() != null) {
                log.info("쿠폰 복구 이벤트 Kafka 발행 - orderId: {}, userCouponId: {}",
                        event.orderId(), event.userCouponId());
                couponRestoreKafkaTemplate.send("coupon-restore-topic",
                        new CouponRestoreEvent(event.userId(), event.userCouponId()));
            }

            // 포인트 차감이 완료된 경우 포인트 복구
            if (pointDeducted) {
                log.info("포인트 복구 이벤트 Kafka 발행 - orderId: {}", event.orderId());
                pointRestoreKafkaTemplate.send("point-restore-topic",
                        new PointRestoreEvent(event.userId(), event.finalAmount()));
            }

            // 재고 차감이 완료된 경우 재고 복구
            if (stockDecreased) {
                log.info("재고 복구 이벤트 Kafka 발행 - orderId: {}", event.orderId());
                stockRestoreKafkaTemplate.send("stock-restore-topic",
                        new StockRestoreEvent(event.cartItems()));
            }

            log.info("보상 트랜잭션 이벤트 발행 완료 - orderId: {}", event.orderId());

        } catch (Exception compensationError) {
            log.error("보상 트랜잭션 처리 중 오류 발생 - orderId: {}, error: {}",
                    event.orderId(), compensationError.getMessage(), compensationError);
            // 보상 트랜잭션 실패는 심각한 문제이므로 모니터링/알람 필요
            // 필요시 DLQ(Dead Letter Queue) 처리 추가
        }
    }
}
