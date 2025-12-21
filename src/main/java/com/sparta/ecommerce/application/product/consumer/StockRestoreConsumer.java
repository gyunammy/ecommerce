package com.sparta.ecommerce.application.product.consumer;

import com.sparta.ecommerce.application.product.ProductService;
import com.sparta.ecommerce.application.product.event.StockRestoreEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 재고 복구 이벤트 Kafka Consumer
 *
 * 주문 실패 시 Kafka에서 재고 복구 이벤트 메시지를 받아서 재고를 복구합니다.
 * 보상 트랜잭션(Saga Compensation)의 일부로 동작합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockRestoreConsumer {

    private final ProductService productService;

    /**
     * 재고 복구 이벤트 메시지를 소비하여 재고를 복구합니다.
     *
     * @param event 재고 복구 이벤트 정보
     */
    @KafkaListener(
            topics = "stock-restore-topic",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "stockRestoreKafkaListenerContainerFactory"
    )
    public void consumeStockRestoreEvent(StockRestoreEvent event) {
        log.info("재고 복구 이벤트 수신 - Items: {}", event.cartItems());

        try {
            productService.restoreStock(event.cartItems());
            log.info("재고 복구 이벤트 처리 완료");
        } catch (Exception e) {
            log.error("재고 복구 이벤트 처리 실패 - error: {}", e.getMessage(), e);
            // 보상 트랜잭션의 실패는 심각한 문제이므로 모니터링/알람 필요
            // 필요시 DLQ(Dead Letter Queue) 처리 추가 가능
        }
    }
}
