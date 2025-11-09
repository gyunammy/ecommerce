package com.sparta.ecommerce.application.product;

import com.sparta.ecommerce.application.order.OrderItemService;
import com.sparta.ecommerce.domain.coupon.dto.ProductResponse;
import com.sparta.ecommerce.domain.product.ProductSortType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetTopProductsUseCase {

    private final ProductService productService;
    private final OrderItemService orderItemService;

    private static final int TOP_PRODUCTS_LIMIT = 10;

    /**
     * 인기 상품 조회 (상위 10개)
     *
     * 조회수 기준: ProductService에서 조회
     * 판매량 기준: OrderItemService에서 조회 (판매량은 OrderItem 도메인의 책임)
     *
     * @param sortType 정렬 기준 (조회수 또는 판매량)
     * @return 인기 상품 목록 (상위 10개)
     */
    public List<ProductResponse> getTopProducts(ProductSortType sortType) {
        return sortType == ProductSortType.VIEW_COUNT
                ? productService.findTopProductsByViewCount(TOP_PRODUCTS_LIMIT)
                : orderItemService.findTopProductsBySoldCount(TOP_PRODUCTS_LIMIT);
    }
}
