package com.sparta.ecommerce.application.product;

import com.sparta.ecommerce.domain.cart.dto.CartItemResponse;
import com.sparta.ecommerce.domain.coupon.dto.ProductResponse;
import com.sparta.ecommerce.domain.product.ProductRepository;
import com.sparta.ecommerce.domain.product.ProductSortType;
import com.sparta.ecommerce.domain.product.entity.Product;
import com.sparta.ecommerce.domain.product.exception.ProductException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.sparta.ecommerce.domain.product.exception.ProductErrorCode.PRODUCT_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    /**
     * 모든 상품 조회
     *
     * @return 상품 목록
     */
    public List<ProductResponse> findAll() {
        return productRepository.findAll()
                .stream()
                .map(Product::from)
                .collect(Collectors.toList());
    }

    /**
     * 장바구니 상품 목록으로부터 상품 정보를 조회하여 Map으로 변환합니다.
     *
     * N+1 문제를 방지하기 위해 배치 조회를 사용하며,
     * 빠른 상품 조회를 위해 상품 ID를 키로 하는 Map을 생성합니다.
     *
     * @param cartItems 장바구니 상품 목록
     * @return 상품 ID를 키로, Product 객체를 값으로 하는 Map
     * @throws ProductException 상품을 찾을 수 없는 경우
     */
    public Map<Long, Product> getProductMap(List<CartItemResponse> cartItems) {
        List<Long> productIds = cartItems.stream()
                .map(CartItemResponse::productId)
                .toList();

        List<Product> products = productRepository.findAllByIdWithLock(productIds);

        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getProductId, product -> product));

        // 상품 존재 여부 검증
        for (CartItemResponse cartItem : cartItems) {
            if (!productMap.containsKey(cartItem.productId())) {
                throw new ProductException(PRODUCT_NOT_FOUND);
            }
        }

        return productMap;
    }

    /**
     * 상품 정보 업데이트 (재고 변경 등)
     *
     * @param product 업데이트할 상품
     */
    public void updateProduct(Product product) {
        productRepository.save(product);
    }

    /**
     * 조회수 기준 인기 상품 조회
     *
     * @param limit 조회할 상품 개수
     * @return 조회수 기준 인기 상품 목록
     */
    public List<ProductResponse> findTopProductsByViewCount(int limit) {
        return productRepository.findTopProducts(ProductSortType.VIEW_COUNT, limit)
                .stream()
                .map(Product::from)
                .collect(Collectors.toList());
    }

    /**
     * 모든 상품 조회 (Product 엔티티 반환)
     *
     * @return 상품 엔티티 목록
     */
    public List<Product> findAllProducts() {
        return productRepository.findAll();
    }
}
