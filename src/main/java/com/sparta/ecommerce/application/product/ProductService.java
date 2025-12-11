package com.sparta.ecommerce.application.product;

import com.sparta.ecommerce.application.order.OrderService;
import com.sparta.ecommerce.application.product.event.StockDeductionFailedEvent;
import com.sparta.ecommerce.application.product.event.StockReservedEvent;
import com.sparta.ecommerce.domain.cart.dto.CartItemResponse;
import com.sparta.ecommerce.domain.coupon.dto.ProductResponse;
import com.sparta.ecommerce.domain.order.entity.Order;
import com.sparta.ecommerce.domain.product.ProductRepository;
import com.sparta.ecommerce.domain.product.entity.Product;
import com.sparta.ecommerce.domain.product.exception.ProductException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.sparta.ecommerce.domain.product.exception.ProductErrorCode.PRODUCT_NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final RedissonClient redissonClient;
    private final OrderService orderService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 모든 상품 조회
     *
     * @return 상품 목록
     */
    public List<ProductResponse> findAll() {
        return productRepository.findAll()
                .stream()
                .map(Product::from)
                .collect(Collectors.toList());
    }

    /**
     * 장바구니 상품 목록으로부터 상품 정보를 조회하여 Map으로 변환합니다.
     *
     * N+1 문제를 방지하기 위해 배치 조회를 사용하며,
     * 빠른 상품 조회를 위해 상품 ID를 키로 하는 Map을 생성합니다.
     *
     * 주의: 이 메서드는 락을 획득하지 않습니다.
     * 동시성 제어가 필요한 경우 호출자가 락을 관리해야 합니다.
     *
     * @param productIds 상품 ID 목록
     * @return 상품 ID를 키로, Product 객체를 값으로 하는 Map
     * @throws ProductException 상품을 찾을 수 없는 경우
     */
    public Map<Long, Product> getProductMapByIds(List<Long> productIds) {
        // 상품 조회
        List<Product> products = productRepository.findAllById(productIds);

        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getProductId, product -> product));

        // 상품 존재 여부 검증
        for (Long productId : productIds) {
            if (!productMap.containsKey(productId)) {
                throw new ProductException(PRODUCT_NOT_FOUND);
            }
        }

        return productMap;
    }

    /**
     * 상품 정보 업데이트 (재고 변경 등)
     *
     * @param product 업데이트할 상품
     */
    public void updateProduct(Product product) {
        productRepository.save(product);
    }

    /**
     * 조회수 기준 인기 상품 조회
     *
     * @param limit 조회할 상품 개수
     * @return 조회수 기준 인기 상품 목록
     */
    public List<ProductResponse> findTopProductsByViewCount(int limit) {
        return productRepository.findTopProductsByViewCount(limit)
                .stream()
                .map(Product::from)
                .collect(Collectors.toList());
    }

    /**
     * 모든 상품 조회 (Product 엔티티 반환)
     *
     * @return 상품 엔티티 목록
     */
    public List<Product> findAllProducts() {
        return productRepository.findAll();
    }

    /**
     * 장바구니 상품들의 재고를 검증합니다.
     *
     * @param cartItems 장바구니 상품 목록
     * @param productMap 상품 정보 맵
     * @throws com.sparta.ecommerce.domain.product.exception.ProductException 재고가 부족한 경우
     */
    public void validateStock(List<CartItemResponse> cartItems, Map<Long, Product> productMap) {
        for (CartItemResponse cartItem : cartItems) {
            Product product = productMap.get(cartItem.productId());
            product.validateStock(cartItem.quantity());
        }
    }

    /**
     * 장바구니 총 금액을 계산합니다.
     *
     * @param cartItems 장바구니 상품 목록
     * @param productMap 상품 정보 맵
     * @return 총 금액
     */
    public int calculateTotalAmount(List<CartItemResponse> cartItems, Map<Long, Product> productMap) {
        return cartItems.stream()
                .mapToInt(cartItem -> {
                    Product product = productMap.get(cartItem.productId());
                    return product.getPrice() * cartItem.quantity();
                })
                .sum();
    }

    /**
     * 재고 차감 (락 없음)
     *
     * 호출자의 트랜잭션에 참여합니다.
     * Redis MultiLock이 획득된 상태에서 호출되어야 합니다.
     * 재고 차감 실패 시 예외를 던져 트랜잭션 롤백을 트리거합니다.
     *
     * @param cartItems 차감할 장바구니 상품 목록
     * @param productMap 상품 정보 맵
     * @throws com.sparta.ecommerce.domain.product.exception.ProductException 재고 차감 실패 시
     */
    public void decreaseStock(List<CartItemResponse> cartItems, Map<Long, Product> productMap) {
        log.debug("재고 차감 처리 시작");

        for (CartItemResponse cartItem : cartItems) {
            Product product = productMap.get(cartItem.productId());
            product.decreaseStock(cartItem.quantity());
            updateProduct(product);

            log.debug("재고 차감 - ProductId: {}, Quantity: {}",
                    cartItem.productId(), cartItem.quantity());
        }

        log.debug("재고 차감 처리 완료");
    }

    /**
     * Redis MultiLock을 획득하고 재고를 차감합니다.
     *
     * 동시성 제어를 위해 Redis MultiLock을 사용하며,
     * 데드락 방지를 위해 상품 ID를 정렬하여 락을 획득합니다.
     * 트랜잭션 내에서 실행되며, 재고 차감 실패 시 롤백됩니다.
     * 재고 차감 성공 시 StockReservedEvent를 발행합니다.
     * 재고 차감 실패 시 StockDeductionFailedEvent를 발행합니다.
     *
     * @param orderId 주문 ID
     * @param userId 사용자 ID
     * @param cartItems 차감할 장바구니 상품 목록
     */
    @Transactional
    public void decreaseStockWithLock(Long orderId, Long userId, List<CartItemResponse> cartItems) {
        log.debug("락 획득 및 재고 차감 시작 - OrderId: {}", orderId);

        // 주문 상태 확인 (FAILED 상태면 이미 다른 작업이 실패함)
        Order order = orderService.getOrderById(orderId);
        if ("FAILED".equals(order.getStatus())) {
            log.debug("이미 실패한 주문 - OrderId: {}", orderId);
            return;
        }

        try {
            // 상품 ID 추출 및 정렬 (데드락 방지)
            List<Long> productIds = cartItems.stream()
                    .map(CartItemResponse::productId)
                    .distinct()
                    .sorted()
                    .toList();

            // Redis MultiLock 생성
            RLock[] locks = productIds.stream()
                    .map(id -> redissonClient.getLock("product:lock:" + id))
                    .toArray(RLock[]::new);
            RLock multiLock = redissonClient.getMultiLock(locks);

            try {
                // 락 획득 (대기 30초, 점유 10초)
                boolean available = multiLock.tryLock(30, 10, TimeUnit.SECONDS);
                if (!available) {
                    throw new IllegalStateException("상품 락을 획득할 수 없습니다");
                }

                try {
                    // 상품 조회
                    Map<Long, Product> productMap = getProductMapByIds(productIds);

                    // 재고 차감
                    decreaseStock(cartItems, productMap);

                    log.info("락 획득 및 재고 차감 완료 - OrderId: {}", orderId);

                    // 재고 차감 성공 이벤트 발행
                    eventPublisher.publishEvent(new StockReservedEvent(orderId, userId));
                    log.debug("재고 차감 성공 이벤트 발행 - OrderId: {}", orderId);

                } finally {
                    // 락 해제
                    if (multiLock.isHeldByCurrentThread()) {
                        multiLock.unlock();
                        log.debug("락 해제 완료 - OrderId: {}", orderId);
                    }
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("락 획득 중 인터럽트 발생 - OrderId: {}", orderId, e);
                throw new IllegalStateException("Lock acquisition interrupted", e);
            }

        } catch (Exception e) {
            log.error("재고 차감 실패 - OrderId: {}, Error: {}", orderId, e.getMessage(), e);

            // 재고 차감 실패 이벤트 발행
            eventPublisher.publishEvent(new StockDeductionFailedEvent(
                    orderId,
                    userId,
                    e.getMessage()
            ));
        }
    }

    /**
     * 재고 복구 (보상 트랜잭션)
     *
     * ProductService에서 재고 복구 및 예외 처리를 담당합니다.
     * 실패 시 로그를 남기고 예외를 발행합니다.
     *
     * @param cartItems 복구할 장바구니 상품 목록
     */
    public void restoreStock(List<CartItemResponse> cartItems) {
        log.debug("재고 복구 시작 - Items: {}", cartItems);

        try {
            // 상품 ID 추출
            List<Long> productIds = cartItems.stream()
                    .map(CartItemResponse::productId)
                    .distinct()
                    .toList();

            // 상품 조회
            Map<Long, Product> productMap = getProductMapByIds(productIds);

            // 재고 복구
            for (CartItemResponse cartItem : cartItems) {
                Product product = productMap.get(cartItem.productId());
                product.restoreStock(cartItem.quantity());
                updateProduct(product);
                log.debug("재고 복구 완료 - ProductId: {}, Quantity: {}",
                        cartItem.productId(), cartItem.quantity());
            }

            log.info("재고 복구 처리 완료");

        } catch (Exception e) {
            log.error("재고 복구 실패 - Error: {}", e.getMessage(), e);
            // 보상 트랜잭션의 일부이므로, 여기서 실패 시 심각한 문제. 모니터링/알람 필요.
            throw new RuntimeException("재고 복구 실패: " + e.getMessage(), e);
        }
    }
}
