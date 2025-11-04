package com.sparta.ecommerce.domain.cart.exception;

import org.springframework.http.HttpStatus;

public enum CartErrorCode {
    CART_IS_EMPTY(HttpStatus.BAD_REQUEST, "장바구니가 비어있습니다.");

    private final HttpStatus status;
    private final String message;

    CartErrorCode(HttpStatus status, String message) {
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
