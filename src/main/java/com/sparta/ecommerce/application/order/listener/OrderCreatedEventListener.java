package com.sparta.ecommerce.application.order.listener;

import com.sparta.ecommerce.application.order.event.OrderCreatedEvent;
import com.sparta.ecommerce.domain.cart.dto.CartItemResponse;
import com.sparta.ecommerce.domain.product.ProductRankingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 주문 생성 이벤트 리스너
 *
 * 주문 생성 완료 이벤트를 수신하여 상품 판매량 랭킹을 업데이트합니다.
 * 비동기로 실행되어 주문 생성 프로세스와 분리됩니다.
 */
@Component
@RequiredArgsConstructor
public class OrderCreatedEventListener {

    private final ProductRankingRepository productRankingRepository;

    /**
     * 주문 생성 이벤트를 처리하여 상품 판매량 랭킹을 업데이트합니다.
     *
     * @param event 주문 생성 완료 이벤트
     */
    @Async
    @EventListener
    public void handleOrderCreated(OrderCreatedEvent event) {
        for (CartItemResponse cartItem : event.cartItems()) {
            productRankingRepository.incrementSalesCount(
                    cartItem.productId(),
                    cartItem.quantity()
            );
        }
    }
}
