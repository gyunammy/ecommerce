package com.sparta.ecommerce.domain.product.exception;

import org.springframework.http.HttpStatus;

public enum ProductErrorCode {
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."),
    INSUFFICIENT_STOCK(HttpStatus.CONFLICT, "재고가 부족합니다.");

    private final HttpStatus status;
    private final String message;

    ProductErrorCode(HttpStatus status, String message) {
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
