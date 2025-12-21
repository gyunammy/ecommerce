package com.sparta.ecommerce.application.user.consumer;

import com.sparta.ecommerce.application.user.UserService;
import com.sparta.ecommerce.application.user.event.PointRestoreEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 포인트 복구 이벤트 Kafka Consumer
 *
 * 주문 실패 시 Kafka에서 포인트 복구 이벤트 메시지를 받아서 포인트를 복구합니다.
 * 보상 트랜잭션(Saga Compensation)의 일부로 동작합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PointRestoreConsumer {

    private final UserService userService;

    /**
     * 포인트 복구 이벤트 메시지를 소비하여 포인트를 복구합니다.
     *
     * @param event 포인트 복구 이벤트 정보
     */
    @KafkaListener(
            topics = "point-restore-topic",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "pointRestoreKafkaListenerContainerFactory"
    )
    public void consumePointRestoreEvent(PointRestoreEvent event) {
        log.info("포인트 복구 이벤트 수신 - UserId: {}, Amount: {}",
                event.userId(), event.amount());

        try {
            userService.restorePoint(event.userId(), event.amount());
            log.info("포인트 복구 이벤트 처리 완료 - UserId: {}, Amount: {}",
                    event.userId(), event.amount());
        } catch (Exception e) {
            log.error("포인트 복구 이벤트 처리 실패 - UserId: {}, Amount: {}, error: {}",
                    event.userId(), event.amount(), e.getMessage(), e);
            // 보상 트랜잭션의 실패는 심각한 문제이므로 모니터링/알람 필요
            // 필요시 DLQ(Dead Letter Queue) 처리 추가 가능
        }
    }
}
