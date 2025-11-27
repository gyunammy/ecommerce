package com.sparta.ecommerce.aspect;

import com.sparta.ecommerce.common.annotation.RedissonLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

@Slf4j
@Aspect
@Component
@Order(1)  // Transaction AOP(@Order(Integer.MAX_VALUE))보다 먼저 실행
@RequiredArgsConstructor
public class RedissonLockAspect {

    private final RedissonClient redissonClient;

    @Around("@annotation(com.sparta.ecommerce.common.annotation.RedissonLock)")
    public Object redissonLock(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RedissonLock redissonLock = method.getAnnotation(RedissonLock.class);

        // Lock 키 생성
        String lockKey = redissonLock.value();

        // 메서드 파라미터에서 동적으로 키 생성
        Object[] args = joinPoint.getArgs();
        if (args.length > 0) {
            int paramIndex = redissonLock.keyParameterIndex();

            // -1이면 마지막 파라미터 사용, 아니면 지정된 인덱스 사용
            Object keyParam;
            if (paramIndex == -1) {
                keyParam = args[args.length - 1];
            } else if (paramIndex >= 0 && paramIndex < args.length) {
                keyParam = args[paramIndex];
            } else {
                throw new IllegalArgumentException("Invalid keyParameterIndex: " + paramIndex);
            }

            lockKey = String.format("%s:%s", lockKey, keyParam);
        }

        RLock lock = redissonClient.getLock(lockKey);

        try {
            // Lock 획득 시도
            boolean available = lock.tryLock(
                    redissonLock.waitTime(),
                    redissonLock.leaseTime(),
                    TimeUnit.MILLISECONDS
            );

            if (!available) {
                log.warn("Redisson Lock 획득 실패: {}", lockKey);
                throw new IllegalStateException("락을 획득할 수 없습니다");
            }

            log.info("Redisson Lock 획득 성공: {}", lockKey);

            // 실제 메서드 실행
            return joinPoint.proceed();
        } finally {
            // Lock 해제
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("Redisson Lock 해제: {}", lockKey);
            }
        }
    }
}
