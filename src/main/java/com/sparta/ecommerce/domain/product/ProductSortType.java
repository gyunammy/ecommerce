package com.sparta.ecommerce.domain.product;

/**
 * 상품 정렬 기준
 */
public enum ProductSortType {
    VIEW_COUNT("조회수"),
    SOLD_COUNT("판매량");

    private final String description;

    ProductSortType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
