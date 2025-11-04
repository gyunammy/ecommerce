package com.sparta.ecommerce.domain.coupon.exception;

import org.springframework.http.HttpStatus;

public enum CouponErrorCode {
    COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "쿠폰을 찾을 수 없습니다."),
    COUPON_OUT_OF_STOCK(HttpStatus.CONFLICT,  "쿠폰이 모두 소진되었습니다."),
    COUPON_ALREADY_ISSUED(HttpStatus.CONFLICT,"이미 발급받은 쿠폰입니다."),
    USER_COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자 쿠폰을 찾을 수 없습니다."),
    COUPON_ALREADY_USED(HttpStatus.CONFLICT, "이미 사용된 쿠폰입니다."),
    COUPON_EXPIRED(HttpStatus.BAD_REQUEST, "만료된 쿠폰입니다."),
    COUPON_NOT_OWNED(HttpStatus.FORBIDDEN, "본인의 쿠폰이 아닙니다.");

    private final HttpStatus status;
    private final String message;


    CouponErrorCode(HttpStatus status, String message) {

        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() { return status; }

    public String getMessage() {
        return message;
    }
}
