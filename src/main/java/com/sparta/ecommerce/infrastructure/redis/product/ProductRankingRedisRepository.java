package com.sparta.ecommerce.infrastructure.redis.product;

import com.sparta.ecommerce.domain.product.ProductRankingRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis 기반 상품 판매량 랭킹 저장소 구현체
 *
 * Redis Sorted Set을 사용하여 상품별 판매량을 실시간으로 집계하고 랭킹을 관리합니다.
 * 랭킹은 날짜별로 분리되며(오늘 날짜 기준), 1일 후 자동으로 만료됩니다.
 */
@Repository
public class ProductRankingRedisRepository implements ProductRankingRepository {

    private static final String RANKING_KEY_PREFIX = "product:sales:ranking";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final long TTL_DAYS = 1;

    private final RedisTemplate<String, String> redisTemplate;
    private final ZSetOperations<String, String> zSetOps;

    public ProductRankingRedisRepository(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.zSetOps = redisTemplate.opsForZSet();
    }

    /**
     * 오늘 날짜 기준 랭킹 키를 생성합니다.
     * 형식: product:sales:ranking:2025-12-03
     */
    private String getTodayRankingKey() {
        String today = LocalDate.now().format(DATE_FORMATTER);
        return RANKING_KEY_PREFIX + ":" + today;
    }

    /**
     * 상품의 판매량을 증가시킵니다.
     *
     * Redis Sorted Set의 incrementScore를 사용하여 원자적으로 증가시킵니다.
     * 오늘 날짜 기준 랭킹 키에 저장하며, 키가 처음 생성될 때만 1일 TTL을 설정합니다.
     */
    @Override
    public void incrementSalesCount(Long productId, int quantity) {
        String key = getTodayRankingKey();

        // 키 존재 여부 확인
        Boolean keyExists = redisTemplate.hasKey(key);

        zSetOps.incrementScore(key, productId.toString(), quantity);

        // 키가 처음 생성될 때만 TTL 설정 (중복 갱신 방지)
        if (Boolean.FALSE.equals(keyExists)) {
            redisTemplate.expire(key, TTL_DAYS, TimeUnit.DAYS);
        }
    }

    /**
     * 오늘 날짜 기준 상위 N개 상품의 판매량 랭킹을 조회합니다.
     *
     * Redis Sorted Set의 reverseRangeWithScores를 사용하여
     * 점수(판매량) 높은 순으로 조회합니다.
     */
    @Override
    public Map<Long, Integer> getTopRankings(int limit) {
        String key = getTodayRankingKey();
        Set<ZSetOperations.TypedTuple<String>> topProducts =
            zSetOps.reverseRangeWithScores(key, 0, limit - 1);

        Map<Long, Integer> rankings = new LinkedHashMap<>();
        if (topProducts != null) {
            for (ZSetOperations.TypedTuple<String> tuple : topProducts) {
                String productId = tuple.getValue();
                Double score = tuple.getScore();
                if (productId != null && score != null) {
                    rankings.put(Long.parseLong(productId), score.intValue());
                }
            }
        }

        return rankings;
    }

    /**
     * 오늘 날짜 기준 특정 상품의 판매량을 조회합니다.
     */
    @Override
    public Integer getSalesCount(Long productId) {
        String key = getTodayRankingKey();
        Double score = zSetOps.score(key, productId.toString());
        return score != null ? score.intValue() : 0;
    }

    /**
     * 오늘 날짜 기준 판매량 랭킹 데이터를 삭제합니다.
     * (주로 테스트 용도)
     */
    @Override
    public void clearAll() {
        String key = getTodayRankingKey();
        redisTemplate.delete(key);
    }
}
