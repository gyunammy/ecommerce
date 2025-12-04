package com.sparta.ecommerce.scheduler;

import com.sparta.ecommerce.application.coupon.IssueCouponUseCase;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "coupon.queue.consumer.enabled", havingValue = "true", matchIfMissing = true)
public class CouponIssueQueueConsumer {

    private final IssueCouponUseCase issueCouponUseCase;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String QUEUE_KEY = "coupon:issue:queue";

    /**
     * 애플리케이션 시작 시 Consumer 스레드 실행
     * BRPOP으로 큐에 데이터가 있을 때만 처리 (스케줄러 없이)
     */
    @PostConstruct
    public void startConsumer() {
        Thread.startVirtualThread(() -> {
            log.info("쿠폰 발급 Queue Consumer 시작");

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // BRPOP: 데이터가 있을 때까지 블로킹 대기 (5초 타임아웃)
                    String queueData = redisTemplate.opsForList()
                            .rightPop(QUEUE_KEY, 5, TimeUnit.SECONDS);

                    if (queueData != null) {
                        // "userId:couponId" 파싱
                        String[] parts = queueData.split(":");
                        Long userId = Long.parseLong(parts[0]);
                        Long couponId = Long.parseLong(parts[1]);

                        // 쿠폰 발급 처리
                        issueCouponUseCase.executeIssueCoupon(userId, couponId);
                        log.info("쿠폰 발급 완료 - userId: {}, couponId: {}", userId, couponId);
                    }

                } catch (Exception e) {
                    // Redisson shutdown 에러는 로깅하지 않고 조용히 종료
                    if (e.getMessage() != null &&
                        (e.getMessage().contains("Redisson is shutdown") ||
                         e.getMessage().contains("shutdown"))) {
                        log.debug("Redis 연결 종료됨, Consumer 종료");
                        break;
                    }
                    log.error("쿠폰 발급 실패 - error: {}", e.getMessage());
                }
            }
            log.info("쿠폰 발급 Queue Consumer 종료");
        });
    }
}
