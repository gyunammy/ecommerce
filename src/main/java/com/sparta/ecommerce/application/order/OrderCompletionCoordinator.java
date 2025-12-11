package com.sparta.ecommerce.application.order;

import com.sparta.ecommerce.common.transaction.TransactionHandler;
import com.sparta.ecommerce.domain.order.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 주문 완료 조율 서비스
 *
 * 재고 차감, 포인트 차감, 쿠폰 사용이 모두 성공했을 때 주문을 COMPLETED 상태로 변경합니다.
 * 병렬 처리를 지원하며, 3가지 작업이 모두 완료되어야 주문이 완료됩니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCompletionCoordinator {

    private final OrderService orderService;

    private final TransactionHandler transactionHandler;

    // 주문별 완료된 작업 추적 (orderId -> Set of completed tasks)
    private final ConcurrentHashMap<Long, Set<String>> completedTasks = new ConcurrentHashMap<>();

    private static final String STOCK_RESERVED = "STOCK_RESERVED";
    private static final String POINT_DEDUCTED = "POINT_DEDUCTED";
    private static final String COUPON_USED = "COUPON_USED";

    /**
     * 재고 차감 완료 표시
     */
    public void markStockReserved(Long orderId) {
        log.debug("재고 차감 완료 - OrderId: {}", orderId);
        markTaskCompleted(orderId, STOCK_RESERVED);
    }

    /**
     * 포인트 차감 완료 표시
     */
    public void markPointDeducted(Long orderId) {
        log.debug("포인트 차감 완료 - OrderId: {}", orderId);
        markTaskCompleted(orderId, POINT_DEDUCTED);
    }

    /**
     * 쿠폰 사용 완료 표시
     */
    public void markCouponUsed(Long orderId) {
        log.debug("쿠폰 사용 완료 - OrderId: {}", orderId);
        markTaskCompleted(orderId, COUPON_USED);
    }

    /**
     * 작업 완료를 기록하고, 모든 작업이 완료되었는지 확인합니다.
     */
    private void markTaskCompleted(Long orderId, String taskName) {
        Set<String> tasks = completedTasks.computeIfAbsent(orderId,
            k -> ConcurrentHashMap.newKeySet());

        tasks.add(taskName);

        log.debug("작업 완료 기록 - OrderId: {}, Task: {}, Completed: {}/3",
            orderId, taskName, tasks.size());

        // 모든 작업이 완료되었는지 확인
        if (tasks.size() == 3) {
            transactionHandler.execute(() -> completeOrder(orderId));
            // 완료된 주문의 추적 정보 삭제
            completedTasks.remove(orderId);
        }
    }

    /**
     * 주문을 COMPLETED 상태로 변경합니다.
     */
    private void completeOrder(Long orderId) {
        try {
            log.info("모든 작업 완료 - 주문 완료 처리 시작 - OrderId: {}", orderId);

            Order order = orderService.getOrderById(orderId);

            // 이미 완료된 주문이면 스킵
            if ("COMPLETED".equals(order.getStatus())) {
                log.debug("이미 완료된 주문 - OrderId: {}", orderId);
                return;
            }

            order.setStatus("COMPLETED");
            orderService.updateOrder(order);

            log.info("주문 완료 - OrderId: {}, Status: COMPLETED", orderId);

        } catch (Exception e) {
            log.error("주문 완료 처리 실패 - OrderId: {}, Error: {}",
                orderId, e.getMessage(), e);
        }
    }

    /**
     * 특정 주문의 추적 정보를 제거합니다. (테스트 또는 실패 처리 시 사용)
     */
    public void clearTracking(Long orderId) {
        completedTasks.remove(orderId);
        log.debug("주문 추적 정보 제거 - OrderId: {}", orderId);
    }
}
