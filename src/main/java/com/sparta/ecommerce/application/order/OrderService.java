package com.sparta.ecommerce.application.order;

import com.sparta.ecommerce.domain.cart.dto.CartItemResponse;
import com.sparta.ecommerce.domain.order.OrderRepository;
import com.sparta.ecommerce.domain.order.entity.Order;
import com.sparta.ecommerce.domain.order.entity.OrderItem;
import com.sparta.ecommerce.domain.product.entity.Product;
import com.sparta.ecommerce.infrastructure.jpa.order.JpaOrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final JpaOrderItemRepository orderItemRepository;

    /**
     * 주문 생성
     * @param userId 사용자 ID
     * @param userCouponId 사용자 쿠폰 ID (nullable)
     * @param totalAmount 총 주문 금액
     * @param discountAmount 할인 금액
     * @param usedPoint 사용한 포인트 (최종 결제 금액)
     * @param cartItems 장바구니 아이템들
     * @param productMap 상품 정보 맵
     * @return 생성된 주문
     */
    public Order createOrder(
            Long userId,
            Long userCouponId,
            int totalAmount,
            int discountAmount,
            int usedPoint,
            List<CartItemResponse> cartItems,
            Map<Long, Product> productMap
    ) {
        LocalDateTime now = LocalDateTime.now();

        // 1. Order 엔티티 생성
        Order order = new Order();
        order.setUserId(userId);
        order.setUserCouponId(userCouponId);
        order.setTotalAmount(totalAmount);
        order.setDiscountAmount(discountAmount);
        order.setUsedPoint(usedPoint);  // 실제 사용한 포인트 기록
        order.setStatus("COMPLETED");
        order.setCreatedAt(now);

        // 주문 저장
        Order savedOrder = orderRepository.save(order);

        // 2. OrderItem 엔티티들 생성
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItemResponse cartItem : cartItems) {
            Product product = productMap.get(cartItem.productId());

            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(savedOrder.getOrderId());
            orderItem.setProductId(product.getProductId());
            orderItem.setProductName(product.getProductName());
            orderItem.setDescription(product.getDescription());
            orderItem.setQuantity(cartItem.quantity());
            orderItem.setPrice(product.getPrice());
            orderItem.setCreatedAt(now);

            orderItems.add(orderItem);
        }

        // 주문 아이템들 저장
        orderItemRepository.saveAll(orderItems);

        return savedOrder;
    }
}
