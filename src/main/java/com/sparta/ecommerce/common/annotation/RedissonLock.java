package com.sparta.ecommerce.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RedissonLock {

    String value(); // Lock의 이름 (고유값)
    int keyParameterIndex() default -1; // Lock 키로 사용할 파라미터 인덱스 (0부터 시작, -1이면 마지막)
    long waitTime() default 30000L; // Lock획득을 시도하는 최대 시간 (ms) - 기본 30초로 증가
    long leaseTime() default 10000L; // 락을 획득한 후, 점유하는 최대 시간 (ms) - 기본 10초로 증가
}
