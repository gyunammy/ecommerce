package com.sparta.ecommerce.application.user.event;

public record PointRestoreEvent(
        Long userId,
        int amount
) {
}
