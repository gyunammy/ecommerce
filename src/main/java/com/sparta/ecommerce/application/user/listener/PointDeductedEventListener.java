package com.sparta.ecommerce.application.user.listener;

import com.sparta.ecommerce.application.order.event.OrderCreatedEvent;
import com.sparta.ecommerce.application.user.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 포인트 차감 이벤트 리스너
 *
 * OrderCreatedEvent를 받아서 병렬로 사용자 포인트를 차감합니다.
 * 실제 처리 로직은 UserService.deductPointForOrder()에 위임됩니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PointDeductedEventListener {

    private final UserService userService;

    /**
     * 주문 생성 이벤트를 처리하여 사용자 포인트를 차감합니다.
     *
     * OrderCreatedEvent를 받아서 병렬로 처리됩니다.
     * TransactionPhase.AFTER_COMMIT을 사용하여 주문 생성 트랜잭션 커밋 후 실행됩니다.
     * 실제 처리 로직은 UserService.deductPointForOrder()에 위임됩니다.
     *
     * @param event 주문 생성 완료 이벤트
     */
    @Transactional
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePointDeducted(OrderCreatedEvent event) {
        userService.deductPointForOrder(event);
    }
}
