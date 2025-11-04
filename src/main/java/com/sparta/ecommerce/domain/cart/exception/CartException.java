package com.sparta.ecommerce.domain.cart.exception;

import lombok.Getter;

@Getter
public class CartException extends RuntimeException {
    private final CartErrorCode errorCode;

    public CartException(CartErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
