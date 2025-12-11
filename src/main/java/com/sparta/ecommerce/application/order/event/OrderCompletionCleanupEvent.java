package com.sparta.ecommerce.application.order.event;

public record OrderCompletionCleanupEvent(
        Long orderId
) {
}
