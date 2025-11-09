package com.sparta.ecommerce.domain.product;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductSortTypeTest {

    @Test
    @DisplayName("VIEW_COUNT의 설명이 올바른지 확인")
    void viewCount_description() {
        // when
        String description = ProductSortType.VIEW_COUNT.getDescription();

        // then
        assertThat(description).isEqualTo("조회수");
    }

    @Test
    @DisplayName("SOLD_COUNT의 설명이 올바른지 확인")
    void soldCount_description() {
        // when
        String description = ProductSortType.SOLD_COUNT.getDescription();

        // then
        assertThat(description).isEqualTo("판매량");
    }

    @Test
    @DisplayName("모든 ProductSortType 값 확인")
    void allValues() {
        // when
        ProductSortType[] values = ProductSortType.values();

        // then
        assertThat(values).hasSize(2);
        assertThat(values).containsExactly(ProductSortType.VIEW_COUNT, ProductSortType.SOLD_COUNT);
    }

    @Test
    @DisplayName("valueOf로 enum 값 가져오기")
    void valueOf_test() {
        // when
        ProductSortType viewCount = ProductSortType.valueOf("VIEW_COUNT");
        ProductSortType soldCount = ProductSortType.valueOf("SOLD_COUNT");

        // then
        assertThat(viewCount).isEqualTo(ProductSortType.VIEW_COUNT);
        assertThat(soldCount).isEqualTo(ProductSortType.SOLD_COUNT);
        assertThat(viewCount.getDescription()).isEqualTo("조회수");
        assertThat(soldCount.getDescription()).isEqualTo("판매량");
    }
}
