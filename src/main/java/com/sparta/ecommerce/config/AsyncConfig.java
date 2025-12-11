package com.sparta.ecommerce.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 비동기 작업 실행을 위한 설정
 *
 * 운영 환경: ThreadPoolTaskExecutor로 비동기 실행
 * 테스트 환경: SyncTaskExecutor로 동기 실행 (테스트 안정성 확보)
 */
@EnableAsync
@Configuration
public class AsyncConfig {

    /**
     * TaskExecutor 빈 등록
     * - 운영 환경: ThreadPoolTaskExecutor로 비동기 실행
     * - 테스트 환경 (app.async.enabled=false): SyncTaskExecutor로 동기 실행
     */
    @Bean("taskExecutor")
    public TaskExecutor taskExecutor(
            @org.springframework.beans.factory.annotation.Value("${app.async.enabled:true}") boolean asyncEnabled
    ) {
        if (asyncEnabled) {
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setCorePoolSize(5);
            executor.setMaxPoolSize(10);
            executor.setQueueCapacity(100);
            executor.setThreadNamePrefix("async-task-");
            executor.initialize();
            return executor;
        } else {
            return new SyncTaskExecutor();
        }
    }
}
