package com.sparta.ecommerce.application.order;

import com.sparta.ecommerce.domain.cart.dto.CartItemResponse;
import com.sparta.ecommerce.domain.order.OrderItemRepository;
import com.sparta.ecommerce.domain.order.OrderRepository;
import com.sparta.ecommerce.domain.order.entity.Order;
import com.sparta.ecommerce.domain.order.entity.OrderItem;
import com.sparta.ecommerce.domain.product.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private OrderService orderService;

    @Captor
    private ArgumentCaptor<Order> orderCaptor;

    @Captor
    private ArgumentCaptor<List<OrderItem>> orderItemsCaptor;

    @Test
    @DisplayName("주문 생성 - 정상적인 주문 생성")
    void createOrder_success() {
        // given
        Long userId = 1L;
        Long userCouponId = 100L;
        int totalAmount = 50000;
        int discountAmount = 5000;
        int usedPoint = 45000;

        LocalDateTime now = LocalDateTime.now();
        CartItemResponse cartItem1 = new CartItemResponse(1L, userId, 10L, 2, now, now);
        CartItemResponse cartItem2 = new CartItemResponse(2L, userId, 20L, 1, now, now);
        List<CartItemResponse> cartItems = Arrays.asList(cartItem1, cartItem2);

        Product product1 = new Product(10L, "노트북", "고성능 노트북", 100, 20000, 50, now, now);
        Product product2 = new Product(20L, "마우스", "무선 마우스", 200, 10000, 100, now, now);
        Map<Long, Product> productMap = Map.of(10L, product1, 20L, product2);

        Order savedOrder = new Order(1L, userId, userCouponId, totalAmount, discountAmount, usedPoint, "COMPLETED", now);
        given(orderRepository.save(any(Order.class))).willReturn(savedOrder);

        // when
        Order result = orderService.createOrder(userId, userCouponId, totalAmount, discountAmount, usedPoint, cartItems, productMap);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(1L);
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getUserCouponId()).isEqualTo(userCouponId);
        assertThat(result.getTotalAmount()).isEqualTo(totalAmount);
        assertThat(result.getDiscountAmount()).isEqualTo(discountAmount);
        assertThat(result.getUsedPoint()).isEqualTo(usedPoint);
        assertThat(result.getStatus()).isEqualTo("COMPLETED");

        verify(orderRepository).save(orderCaptor.capture());
        Order capturedOrder = orderCaptor.getValue();
        assertThat(capturedOrder.getUserId()).isEqualTo(userId);
        assertThat(capturedOrder.getUserCouponId()).isEqualTo(userCouponId);
        assertThat(capturedOrder.getStatus()).isEqualTo("COMPLETED");

        verify(orderItemRepository).saveAll(orderItemsCaptor.capture());
        List<OrderItem> capturedOrderItems = orderItemsCaptor.getValue();
        assertThat(capturedOrderItems).hasSize(2);

        // 첫 번째 주문 아이템 검증
        OrderItem orderItem1 = capturedOrderItems.get(0);
        assertThat(orderItem1.getOrderId()).isEqualTo(savedOrder.getOrderId());
        assertThat(orderItem1.getProductId()).isEqualTo(10L);
        assertThat(orderItem1.getProductName()).isEqualTo("노트북");
        assertThat(orderItem1.getQuantity()).isEqualTo(2);
        assertThat(orderItem1.getPrice()).isEqualTo(20000);

        // 두 번째 주문 아이템 검증
        OrderItem orderItem2 = capturedOrderItems.get(1);
        assertThat(orderItem2.getOrderId()).isEqualTo(savedOrder.getOrderId());
        assertThat(orderItem2.getProductId()).isEqualTo(20L);
        assertThat(orderItem2.getProductName()).isEqualTo("마우스");
        assertThat(orderItem2.getQuantity()).isEqualTo(1);
        assertThat(orderItem2.getPrice()).isEqualTo(10000);
    }

    @Test
    @DisplayName("주문 생성 - 쿠폰 없이 주문")
    void createOrder_withoutCoupon() {
        // given
        Long userId = 1L;
        Long userCouponId = null; // 쿠폰 없음
        int totalAmount = 30000;
        int discountAmount = 0;
        int usedPoint = 30000;

        LocalDateTime now = LocalDateTime.now();
        CartItemResponse cartItem = new CartItemResponse(1L, userId, 10L, 3, now, now);
        List<CartItemResponse> cartItems = List.of(cartItem);

        Product product = new Product(10L, "키보드", "기계식 키보드", 50, 10000, 30, now, now);
        Map<Long, Product> productMap = Map.of(10L, product);

        Order savedOrder = new Order(1L, userId, null, totalAmount, discountAmount, usedPoint, "COMPLETED", now);
        given(orderRepository.save(any(Order.class))).willReturn(savedOrder);

        // when
        Order result = orderService.createOrder(userId, userCouponId, totalAmount, discountAmount, usedPoint, cartItems, productMap);

        // then
        assertThat(result.getUserCouponId()).isNull();
        assertThat(result.getDiscountAmount()).isEqualTo(0);

        verify(orderRepository).save(orderCaptor.capture());
        Order capturedOrder = orderCaptor.getValue();
        assertThat(capturedOrder.getUserCouponId()).isNull();

        verify(orderItemRepository).saveAll(orderItemsCaptor.capture());
        List<OrderItem> capturedOrderItems = orderItemsCaptor.getValue();
        assertThat(capturedOrderItems).hasSize(1);
        assertThat(capturedOrderItems.get(0).getProductId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("주문 생성 - 단일 상품 주문")
    void createOrder_singleProduct() {
        // given
        Long userId = 2L;
        Long userCouponId = 200L;
        int totalAmount = 100000;
        int discountAmount = 10000;
        int usedPoint = 90000;

        LocalDateTime now = LocalDateTime.now();
        CartItemResponse cartItem = new CartItemResponse(1L, userId, 100L, 1, now, now);
        List<CartItemResponse> cartItems = List.of(cartItem);

        Product product = new Product(100L, "모니터", "27인치 모니터", 20, 100000, 80, now, now);
        Map<Long, Product> productMap = Map.of(100L, product);

        Order savedOrder = new Order(10L, userId, userCouponId, totalAmount, discountAmount, usedPoint, "COMPLETED", now);
        given(orderRepository.save(any(Order.class))).willReturn(savedOrder);

        // when
        Order result = orderService.createOrder(userId, userCouponId, totalAmount, discountAmount, usedPoint, cartItems, productMap);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(10L);

        verify(orderItemRepository).saveAll(orderItemsCaptor.capture());
        List<OrderItem> capturedOrderItems = orderItemsCaptor.getValue();
        assertThat(capturedOrderItems).hasSize(1);

        OrderItem orderItem = capturedOrderItems.get(0);
        assertThat(orderItem.getProductId()).isEqualTo(100L);
        assertThat(orderItem.getProductName()).isEqualTo("모니터");
        assertThat(orderItem.getQuantity()).isEqualTo(1);
        assertThat(orderItem.getPrice()).isEqualTo(100000);
    }

    @Test
    @DisplayName("주문 생성 - 여러 상품을 다양한 수량으로 주문")
    void createOrder_multipleProductsWithVariousQuantities() {
        // given
        Long userId = 3L;
        Long userCouponId = 300L;
        int totalAmount = 200000;
        int discountAmount = 20000;
        int usedPoint = 180000;

        LocalDateTime now = LocalDateTime.now();
        CartItemResponse cartItem1 = new CartItemResponse(1L, userId, 10L, 5, now, now);
        CartItemResponse cartItem2 = new CartItemResponse(2L, userId, 20L, 3, now, now);
        CartItemResponse cartItem3 = new CartItemResponse(3L, userId, 30L, 1, now, now);
        List<CartItemResponse> cartItems = Arrays.asList(cartItem1, cartItem2, cartItem3);

        Product product1 = new Product(10L, "USB", "USB 3.0", 100, 10000, 200, now, now);
        Product product2 = new Product(20L, "HDMI 케이블", "2m HDMI 케이블", 50, 15000, 150, now, now);
        Product product3 = new Product(30L, "웹캠", "FHD 웹캠", 30, 50000, 80, now, now);
        Map<Long, Product> productMap = Map.of(10L, product1, 20L, product2, 30L, product3);

        Order savedOrder = new Order(5L, userId, userCouponId, totalAmount, discountAmount, usedPoint, "COMPLETED", now);
        given(orderRepository.save(any(Order.class))).willReturn(savedOrder);

        // when
        Order result = orderService.createOrder(userId, userCouponId, totalAmount, discountAmount, usedPoint, cartItems, productMap);

        // then
        assertThat(result.getOrderId()).isEqualTo(5L);
        assertThat(result.getTotalAmount()).isEqualTo(200000);

        verify(orderItemRepository).saveAll(orderItemsCaptor.capture());
        List<OrderItem> capturedOrderItems = orderItemsCaptor.getValue();
        assertThat(capturedOrderItems).hasSize(3);

        // 각 주문 아이템의 수량 검증
        assertThat(capturedOrderItems.get(0).getQuantity()).isEqualTo(5);
        assertThat(capturedOrderItems.get(1).getQuantity()).isEqualTo(3);
        assertThat(capturedOrderItems.get(2).getQuantity()).isEqualTo(1);

        // 각 주문 아이템의 상품 정보 검증
        assertThat(capturedOrderItems.get(0).getProductName()).isEqualTo("USB");
        assertThat(capturedOrderItems.get(1).getProductName()).isEqualTo("HDMI 케이블");
        assertThat(capturedOrderItems.get(2).getProductName()).isEqualTo("웹캠");
    }

    @Test
    @DisplayName("주문 생성 - Order와 OrderItem에 동일한 orderId가 설정됨")
    void createOrder_orderIdConsistency() {
        // given
        Long userId = 4L;
        Long userCouponId = 400L;
        int totalAmount = 50000;
        int discountAmount = 5000;
        int usedPoint = 45000;

        LocalDateTime now = LocalDateTime.now();
        CartItemResponse cartItem = new CartItemResponse(1L, userId, 10L, 1, now, now);
        List<CartItemResponse> cartItems = List.of(cartItem);

        Product product = new Product(10L, "헤드셋", "게이밍 헤드셋", 30, 50000, 100, now, now);
        Map<Long, Product> productMap = Map.of(10L, product);

        Long expectedOrderId = 999L;
        Order savedOrder = new Order(expectedOrderId, userId, userCouponId, totalAmount, discountAmount, usedPoint, "COMPLETED", now);
        given(orderRepository.save(any(Order.class))).willReturn(savedOrder);

        // when
        Order result = orderService.createOrder(userId, userCouponId, totalAmount, discountAmount, usedPoint, cartItems, productMap);

        // then
        assertThat(result.getOrderId()).isEqualTo(expectedOrderId);

        verify(orderItemRepository).saveAll(orderItemsCaptor.capture());
        List<OrderItem> capturedOrderItems = orderItemsCaptor.getValue();
        assertThat(capturedOrderItems).hasSize(1);

        // OrderItem의 orderId가 Order의 orderId와 일치하는지 검증
        OrderItem orderItem = capturedOrderItems.get(0);
        assertThat(orderItem.getOrderId()).isEqualTo(expectedOrderId);
    }

    @Test
    @DisplayName("주문 생성 - 포인트만 사용한 주문 (할인 없음)")
    void createOrder_pointOnlyNoDiscount() {
        // given
        Long userId = 5L;
        Long userCouponId = null;
        int totalAmount = 25000;
        int discountAmount = 0;
        int usedPoint = 25000;

        LocalDateTime now = LocalDateTime.now();
        CartItemResponse cartItem = new CartItemResponse(1L, userId, 50L, 1, now, now);
        List<CartItemResponse> cartItems = List.of(cartItem);

        Product product = new Product(50L, "마우스패드", "대형 마우스패드", 100, 25000, 50, now, now);
        Map<Long, Product> productMap = Map.of(50L, product);

        Order savedOrder = new Order(100L, userId, null, totalAmount, discountAmount, usedPoint, "COMPLETED", now);
        given(orderRepository.save(any(Order.class))).willReturn(savedOrder);

        // when
        Order result = orderService.createOrder(userId, userCouponId, totalAmount, discountAmount, usedPoint, cartItems, productMap);

        // then
        assertThat(result.getUserCouponId()).isNull();
        assertThat(result.getDiscountAmount()).isEqualTo(0);
        assertThat(result.getUsedPoint()).isEqualTo(25000);

        verify(orderRepository).save(orderCaptor.capture());
        Order capturedOrder = orderCaptor.getValue();
        assertThat(capturedOrder.getStatus()).isEqualTo("COMPLETED");
    }
}
