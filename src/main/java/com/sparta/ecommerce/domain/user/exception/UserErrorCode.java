package com.sparta.ecommerce.domain.user.exception;

import org.springframework.http.HttpStatus;

public enum UserErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    INSUFFICIENT_POINT(HttpStatus.BAD_REQUEST, "포인트가 부족합니다.");

    private final HttpStatus status;
    private final String message;

    UserErrorCode(HttpStatus status, String message) {
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
