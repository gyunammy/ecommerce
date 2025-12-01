package com.sparta.ecommerce.scheduler;

import com.sparta.ecommerce.application.product.GetTopProductsUseCase;
import com.sparta.ecommerce.domain.product.ProductSortType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 인기 상품 캐시 워밍 스케줄러
 *
 * 매일 00시에 인기 상품 목록을 조회하여 Redis 캐시에 미리 적재
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PopularProductCacheScheduler {

    private final GetTopProductsUseCase getTopProductsUseCase;

    /**
     * 매일 00시에 인기 상품 캐시 워밍 실행
     *
     * 조회수 기준과 판매량 기준 모두 캐시에 적재
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void warmUpPopularProductsCache() {
        log.info("========== 인기 상품 캐시 워밍 시작 ==========");

        try {
            // 조회수 기준 인기 상품 캐시 워밍
            long startView = System.currentTimeMillis();
            getTopProductsUseCase.getTopProducts(ProductSortType.VIEW_COUNT);
            long endView = System.currentTimeMillis();
            log.info("조회수 기준 인기 상품 캐시 워밍 완료 - {}ms", (endView - startView));

            // 판매량 기준 인기 상품 캐시 워밍
            long startSold = System.currentTimeMillis();
            getTopProductsUseCase.getTopProducts(ProductSortType.SOLD_COUNT);
            long endSold = System.currentTimeMillis();
            log.info("판매량 기준 인기 상품 캐시 워밍 완료 - {}ms", (endSold - startSold));

            log.info("========== 인기 상품 캐시 워밍 종료 ==========");
        } catch (Exception e) {
            log.error("인기 상품 캐시 워밍 중 오류 발생", e);
        }
    }
}
