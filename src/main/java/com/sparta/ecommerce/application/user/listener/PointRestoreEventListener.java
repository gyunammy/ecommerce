package com.sparta.ecommerce.application.user.listener;

import com.sparta.ecommerce.application.user.UserService;
import com.sparta.ecommerce.application.user.event.PointRestoreEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PointRestoreEventListener {

    private final UserService userService;

    @EventListener
    public void handle(PointRestoreEvent event) {
        log.info("포인트 복구 이벤트 수신 - UserId: {}, Amount: {}", event.userId(), event.amount());
        try {
            userService.restorePoint(event.userId(), event.amount());
            log.info("포인트 복구 완료 - UserId: {}", event.userId());
        } catch (Exception e) {
            log.error("포인트 복구 실패 - UserId: {}, Error: {}", event.userId(), e.getMessage(), e);
            // 보상 트랜잭션의 일부이므로, 여기서 실패 시 심각한 문제. 모니터링/알람 필요.
            throw new RuntimeException("포인트 복구에 실패했습니다.", e);
        }
    }
}
