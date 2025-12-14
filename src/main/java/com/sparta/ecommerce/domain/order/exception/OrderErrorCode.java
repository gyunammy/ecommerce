package com.sparta.ecommerce.domain.order.exception;

import org.springframework.http.HttpStatus;

public enum OrderErrorCode {
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String message;

    OrderErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
