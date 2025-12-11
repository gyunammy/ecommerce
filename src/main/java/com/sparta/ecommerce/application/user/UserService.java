package com.sparta.ecommerce.application.user;

import com.sparta.ecommerce.application.order.OrderService;
import com.sparta.ecommerce.application.order.event.OrderCreatedEvent;
import com.sparta.ecommerce.application.user.event.PointDeductedSuccessEvent;
import com.sparta.ecommerce.application.user.event.PointDeductionFailedEvent;
import com.sparta.ecommerce.domain.order.entity.Order;
import com.sparta.ecommerce.domain.user.UserRepository;
import com.sparta.ecommerce.domain.user.entity.User;
import com.sparta.ecommerce.domain.user.exception.UserException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.sparta.ecommerce.domain.user.exception.UserErrorCode.USER_NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final OrderService orderService;
    private final ApplicationEventPublisher eventPublisher;

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserException(USER_NOT_FOUND));
    }

    public void updateUser(User user) {
        userRepository.save(user);
    }

    /**
     * 주문 생성 이벤트를 통한 포인트 차감 처리
     *
     * 포인트 차감을 수행하고 성공/실패 이벤트를 발행합니다.
     * try-catch로 예외를 처리하며, 성공 시 PointDeductedSuccessEvent, 실패 시 PointDeductionFailedEvent를 발행합니다.
     *
     * @param event 주문 생성 완료 이벤트
     */
    @Transactional
    public void deductPointForOrder(OrderCreatedEvent event) {
        try {
            log.debug("포인트 차감 처리 시작 - UserId: {}, Amount: {}",
                    event.userId(), event.finalAmount());

            // 주문 상태 확인 (FAILED 상태면 이미 다른 작업이 실패함)
            Order order = orderService.getOrderById(event.orderId());
            if ("FAILED".equals(order.getStatus())) {
                log.debug("이미 실패한 주문 - OrderId: {}", event.orderId());
                return;
            }

            User user = getUserById(event.userId());
            user.deductPoint(event.finalAmount());
            updateUser(user);

            log.info("포인트 차감 처리 완료 - UserId: {}, Amount: {}",
                    event.userId(), event.finalAmount());

            // 포인트 차감 성공 이벤트 발행
            eventPublisher.publishEvent(new PointDeductedSuccessEvent(
                    event.orderId(),
                    event.userId()
            ));

        } catch (Exception e) {
            log.error("포인트 차감 처리 실패 - UserId: {}, OrderId: {}, Amount: {}, Error: {}",
                    event.userId(), event.orderId(), event.finalAmount(), e.getMessage(), e);

            // 포인트 차감 실패 이벤트 발행
            eventPublisher.publishEvent(new PointDeductionFailedEvent(
                    event.orderId(),
                    event.userId(),
                    event.finalAmount(),
                    e.getMessage(),
                    event.cartItems()
            ));
        }
    }

    /**
     * 포인트 복구 (보상 트랜잭션)
     *
     * @param userId 사용자 ID
     * @param amount 복구할 포인트 금액
     */
    public void restorePoint(Long userId, int amount) {
        try {
            log.debug("포인트 복구 시작 - UserId: {}, Amount: {}", userId, amount);

            User user = getUserById(userId);
            user.restorePoint(amount);
            updateUser(user);

            log.debug("포인트 복구 완료 - UserId: {}, Amount: {}", userId, amount);
        } catch (Exception e) {
            log.error("포인트 복구 실패 - UserId: {}, Amount: {}, Error: {}",
                    userId, amount, e.getMessage(), e);
            throw new RuntimeException("포인트 복구 실패: " + e.getMessage(), e);
        }
    }
}
