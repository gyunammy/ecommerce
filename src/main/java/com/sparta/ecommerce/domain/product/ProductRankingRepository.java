package com.sparta.ecommerce.domain.product;

import java.util.Map;

/**
 * 상품 판매량 랭킹 저장소
 *
 * Redis Sorted Set을 사용하여 상품 판매량 기준 랭킹을 관리합니다.
 */
public interface ProductRankingRepository {

    /**
     * 상품의 판매량을 증가시킵니다.
     *
     * @param productId 상품 ID
     * @param quantity 판매 수량
     */
    void incrementSalesCount(Long productId, int quantity);

    /**
     * 상위 N개 상품의 판매량 랭킹을 조회합니다.
     *
     * @param limit 조회할 상품 개수
     * @return 상품 ID와 판매량 맵 (판매량 내림차순)
     */
    Map<Long, Integer> getTopRankings(int limit);

    /**
     * 특정 상품의 판매량을 조회합니다.
     *
     * @param productId 상품 ID
     * @return 판매량 (없으면 0)
     */
    Integer getSalesCount(Long productId);

    /**
     * 모든 판매량 랭킹 데이터를 삭제합니다.
     * (주로 테스트 용도)
     */
    void clearAll();
}
