package com.sparta.ecommerce.domain.product;

import com.sparta.ecommerce.domain.coupon.dto.ProductResponse;
import com.sparta.ecommerce.domain.product.exception.ProductException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static com.sparta.ecommerce.domain.product.exception.ProductErrorCode.INSUFFICIENT_STOCK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

    @Test
    @DisplayName("재고 검증 성공 - 요청 수량이 재고보다 적음")
    void validateStock_success() {
        // given
        Product product = new Product(
                1L,
                "테스트 상품",
                "설명",
                100,  // 재고 100개
                10000,
                50,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        // when & then - 예외가 발생하지 않아야 함
        product.validateStock(50);
        product.validateStock(100);
    }

    @Test
    @DisplayName("재고 검증 실패 - 요청 수량이 재고보다 많음")
    void validateStock_insufficientStock() {
        // given
        Product product = new Product(
                1L,
                "테스트 상품",
                "설명",
                100,  // 재고 100개
                10000,
                50,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        // when & then
        assertThatThrownBy(() -> product.validateStock(101))
                .isInstanceOf(ProductException.class)
                .hasMessageContaining(INSUFFICIENT_STOCK.getMessage());
    }

    @Test
    @DisplayName("재고 검증 실패 - 재고가 0인 경우")
    void validateStock_zeroStock() {
        // given
        Product product = new Product(
                1L,
                "품절 상품",
                "설명",
                0,  // 재고 0개
                10000,
                50,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        // when & then
        assertThatThrownBy(() -> product.validateStock(1))
                .isInstanceOf(ProductException.class)
                .hasMessageContaining(INSUFFICIENT_STOCK.getMessage());
    }

    @Test
    @DisplayName("재고 차감 성공")
    void decreaseStock_success() {
        // given
        Product product = new Product(
                1L,
                "테스트 상품",
                "설명",
                100,
                10000,
                50,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        // when
        product.decreaseStock(30);

        // then
        assertThat(product.getQuantity()).isEqualTo(70);
    }

    @Test
    @DisplayName("재고 차감 실패 - 재고 부족")
    void decreaseStock_insufficientStock() {
        // given
        Product product = new Product(
                1L,
                "테스트 상품",
                "설명",
                50,
                10000,
                50,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        // when & then
        assertThatThrownBy(() -> product.decreaseStock(100))
                .isInstanceOf(ProductException.class)
                .hasMessageContaining(INSUFFICIENT_STOCK.getMessage());

        // 재고가 변경되지 않았는지 확인
        assertThat(product.getQuantity()).isEqualTo(50);
    }

    @Test
    @DisplayName("재고를 정확히 0으로 만드는 차감")
    void decreaseStock_toZero() {
        // given
        Product product = new Product(
                1L,
                "테스트 상품",
                "설명",
                50,
                10000,
                50,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        // when
        product.decreaseStock(50);

        // then
        assertThat(product.getQuantity()).isEqualTo(0);
    }

    @Test
    @DisplayName("재고 차감 여러 번 수행")
    void decreaseStock_multiple() {
        // given
        Product product = new Product(
                1L,
                "테스트 상품",
                "설명",
                100,
                10000,
                50,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        // when
        product.decreaseStock(10);
        product.decreaseStock(20);
        product.decreaseStock(30);

        // then
        assertThat(product.getQuantity()).isEqualTo(40);
    }

    @Test
    @DisplayName("Product를 ProductResponse로 변환")
    void from() {
        // given
        LocalDateTime createdAt = LocalDateTime.of(2025, 1, 1, 10, 0);
        LocalDateTime updateAt = LocalDateTime.of(2025, 1, 5, 15, 30);

        Product product = new Product(
                1L,
                "테스트 상품",
                "상품 설명입니다",
                100,
                25000,
                150,
                createdAt,
                updateAt
        );

        // when
        ProductResponse response = product.from();

        // then
        assertThat(response.productId()).isEqualTo(1L);
        assertThat(response.productName()).isEqualTo("테스트 상품");
        assertThat(response.description()).isEqualTo("상품 설명입니다");
        assertThat(response.quantity()).isEqualTo(100);
        assertThat(response.price()).isEqualTo(25000);
        assertThat(response.viewCount()).isEqualTo(150);
        assertThat(response.createdAt()).isEqualTo(createdAt);
        assertThat(response.updatedAt()).isEqualTo(updateAt);
    }

    @Test
    @DisplayName("재고가 0인 Product를 ProductResponse로 변환")
    void from_zeroStock() {
        // given
        LocalDateTime now = LocalDateTime.now();

        Product product = new Product(
                2L,
                "품절 상품",
                "품절되었습니다",
                0,
                50000,
                1000,
                now,
                now
        );

        // when
        ProductResponse response = product.from();

        // then
        assertThat(response.productId()).isEqualTo(2L);
        assertThat(response.productName()).isEqualTo("품절 상품");
        assertThat(response.quantity()).isEqualTo(0);
        assertThat(response.price()).isEqualTo(50000);
        assertThat(response.viewCount()).isEqualTo(1000);
    }

    @Test
    @DisplayName("Product 생성 및 필드 검증")
    void create_product() {
        // given
        Long productId = 1L;
        String productName = "테스트 상품";
        String description = "상품 설명";
        Integer quantity = 100;
        Integer price = 10000;
        Integer viewCount = 50;
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime updateAt = LocalDateTime.now();

        // when
        Product product = new Product(
                productId,
                productName,
                description,
                quantity,
                price,
                viewCount,
                createdAt,
                updateAt
        );

        // then
        assertThat(product.getProductId()).isEqualTo(productId);
        assertThat(product.getProductName()).isEqualTo(productName);
        assertThat(product.getDescription()).isEqualTo(description);
        assertThat(product.getQuantity()).isEqualTo(quantity);
        assertThat(product.getPrice()).isEqualTo(price);
        assertThat(product.getViewCount()).isEqualTo(viewCount);
        assertThat(product.getCreatedAt()).isEqualTo(createdAt);
        assertThat(product.getUpdateAt()).isEqualTo(updateAt);
    }
}
