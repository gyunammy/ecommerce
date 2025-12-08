package com.sparta.ecommerce.common.transaction;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 트랜잭션 제어를 담당하는 컴포넌트
 *
 * 비즈니스 로직과 트랜잭션 기술을 분리하여 코드의 응집도를 높입니다.
 */
@Component
public class TransactionHandler {

    /**
     * 주어진 작업을 트랜잭션 내에서 실행합니다.
     *
     * @param action 트랜잭션 내에서 실행할 작업
     */
    @Transactional
    public void execute(Runnable action) {
        action.run();
    }
}
