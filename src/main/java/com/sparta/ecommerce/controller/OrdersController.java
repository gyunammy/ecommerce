package com.sparta.ecommerce.controller;

import com.sparta.ecommerce.dto.response.OrderItemResponse;
import com.sparta.ecommerce.dto.response.OrderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@Tag(name = "Orders", description = "주문 관리")
@RestController
@RequestMapping("/orders")
public class OrdersController {

    // 테스트 주문 상품 데이터
    private static final OrderItemResponse TEST_ORDER_ITEM_1 = OrderItemResponse.builder()
            .orderItemId(1L)
            .orderId(1L)
            .productId(1L)
            .productName("무선 블루투스 이어폰")
            .description("고음질 노이즈 캔슬링 기능이 탑재된 프리미엄 무선 이어폰")
            .quantity(2)
            .price(89000)
            .createdAt("2024-01-02T14:30:00Z")
            .build();

    private static final OrderItemResponse TEST_ORDER_ITEM_2 = OrderItemResponse.builder()
            .orderItemId(2L)
            .orderId(1L)
            .productId(3L)
            .productName("노트북 스탠드")
            .description("각도 조절이 가능한 알루미늄 노트북 거치대")
            .quantity(1)
            .price(35000)
            .createdAt("2024-01-02T14:30:00Z")
            .build();

    private static final OrderItemResponse TEST_ORDER_ITEM_3 = OrderItemResponse.builder()
            .orderItemId(3L)
            .orderId(2L)
            .productId(2L)
            .productName("스마트워치")
            .description("건강 관리 기능이 있는 스마트워치")
            .quantity(1)
            .price(250000)
            .createdAt("2024-01-05T10:00:00Z")
            .build();

    // 테스트 주문 데이터
    private static final OrderResponse TEST_ORDER_1 = OrderResponse.builder()
            .orderId(1L)
            .userId(1L)
            .userCouponId(1L)
            .totalAmount(213000)
            .discountAmount(5000)
            .usedPoint(3000)
            .status("COMPLETED")
            .orderItems(Arrays.asList(TEST_ORDER_ITEM_1, TEST_ORDER_ITEM_2))
            .createdAt("2024-01-02T14:30:00Z")
            .build();

    private static final OrderResponse TEST_ORDER_2 = OrderResponse.builder()
            .orderId(2L)
            .userId(1L)
            .userCouponId(null)
            .totalAmount(250000)
            .discountAmount(0)
            .usedPoint(0)
            .status("PENDING")
            .orderItems(Arrays.asList(TEST_ORDER_ITEM_3))
            .createdAt("2024-01-05T10:00:00Z")
            .build();

    private static final List<OrderResponse> TEST_ORDERS = Arrays.asList(
            TEST_ORDER_1,
            TEST_ORDER_2
    );

    @Operation(
            summary = "주문 목록 조회",
            description = "사용자의 주문 목록을 조회합니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                    mediaType = "application/json"
            )
    ))
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getOrders(
            @Parameter(description = "사용자 ID", example = "1")
            @RequestParam(required = false) Long userId) {
        return ResponseEntity.ok(TEST_ORDERS);
    }

    @Operation(summary = "주문 상세 조회", description = "특정 주문의 상세 정보를 조회합니다.")
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(
            @Parameter(description = "주문 ID", required = true, example = "1")
            @PathVariable Long orderId) {
        // 실제로는 orderId로 조회하지만, 테스트를 위해 첫 번째 주문 반환
        OrderResponse order = OrderResponse.builder()
                .orderId(orderId)
                .userId(1L)
                .userCouponId(1L)
                .totalAmount(213000)
                .discountAmount(5000)
                .usedPoint(3000)
                .status("COMPLETED")
                .orderItems(Arrays.asList(TEST_ORDER_ITEM_1, TEST_ORDER_ITEM_2))
                .createdAt("2024-01-02T14:30:00Z")
                .build();
        return ResponseEntity.ok(order);
    }

//    @Operation(summary = "주문하기", description = "새로운 주문을 생성합니다.")
//    @PostMapping
//    public ResponseEntity<OrderResponse> createOrder(
//            @io.swagger.v3.oas.annotations.parameters.RequestBody(
//                    description = "주문 요청 정보",
//                    required = true
//            )
//            @RequestBody OrderRequest orderRequest) {
//        OrderResponse order = OrderResponse.builder()
//                .orderId(3L)
//                .userId(orderRequest.getUserId())
//                .userCouponId(orderRequest.getUserCouponId())
//                .totalAmount(178000)
//                .discountAmount(10000)
//                .usedPoint(orderRequest.getUsedPoint())
//                .status("PENDING")
//                .orderItems(Arrays.asList(TEST_ORDER_ITEM_1))
//                .createdAt("2024-01-29T15:00:00Z")
//                .build();
//        return ResponseEntity.status(HttpStatus.CREATED).body(order);
//    }
}
