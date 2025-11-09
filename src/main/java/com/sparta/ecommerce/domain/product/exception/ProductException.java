package com.sparta.ecommerce.domain.product.exception;

public class ProductException extends RuntimeException {
    private final ProductErrorCode errorCode;

    public ProductException(ProductErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ProductErrorCode getErrorCode() {
        return errorCode;
    }
}
