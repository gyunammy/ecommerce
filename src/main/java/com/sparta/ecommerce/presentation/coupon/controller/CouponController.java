package com.sparta.ecommerce.presentation.coupon.controller;

import com.sparta.ecommerce.application.coupon.IssueCouponUseCase;
import com.sparta.ecommerce.domain.coupon.dto.UserCouponRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final IssueCouponUseCase issueCouponUseCase;

    /**
     * 쿠폰 발급 신청
     *
     * @return
     */
    @PostMapping("/{couponId}/issue")
    public ResponseEntity<Object> issueCoupon(@PathVariable Long couponId, @RequestBody UserCouponRequest userCouponRequest) {
        issueCouponUseCase.issueCoupon(userCouponRequest.userId(), couponId);
        return ResponseEntity.ok(Map.of("message", "쿠폰이 발급되었습니다."));
    }

}
