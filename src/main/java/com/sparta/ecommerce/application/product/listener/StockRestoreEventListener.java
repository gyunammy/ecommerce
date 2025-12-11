package com.sparta.ecommerce.application.product.listener;

import com.sparta.ecommerce.application.product.ProductService;
import com.sparta.ecommerce.application.product.event.StockRestoreEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 재고 복구 이벤트 리스너
 *
 * 재고 차감 실패 또는 주문 실패 시 재고를 복구합니다.
 * ProductService에서 재고 복구 및 예외 처리를 담당합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockRestoreEventListener {

    private final ProductService productService;

    /**
     * 재고 복구 이벤트를 처리합니다.
     *
     * ProductService에서 재고 복구 및 예외 처리를 담당합니다.
     * 보상 트랜잭션의 일부이므로 실패 시 심각한 문제입니다.
     *
     * @param event 재고 복구 이벤트
     */
    @Transactional
    @EventListener
    public void handle(StockRestoreEvent event) {
        log.debug("재고 복구 이벤트 수신 - Items: {}", event.cartItems());

        // ProductService에서 재고 복구 및 예외 처리
        productService.restoreStock(event.cartItems());
    }
}
