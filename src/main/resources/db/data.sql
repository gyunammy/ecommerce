-- ==========================================
-- 부하 테스트용 초기 데이터
-- ==========================================
-- 상품 목록 조회 성능 테스트를 위한 데이터

-- ==========================================
-- 0. 테이블 생성 (존재하지 않는 경우)
-- ==========================================
CREATE TABLE IF NOT EXISTS product (
    product_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_name VARCHAR(255),
    description VARCHAR(1000),
    quantity INT,
    price INT,
    view_count INT,
    created_at DATETIME,
    update_at DATETIME
);

-- ==========================================
-- 1. 기존 테스트 데이터 삭제
-- ==========================================
DELETE FROM product WHERE product_id BETWEEN 1 AND 100;

-- ==========================================
-- 2. 테스트용 상품 생성 (100개)
-- ==========================================
-- AUTO_INCREMENT 리셋하여 ID=1부터 시작
ALTER TABLE product AUTO_INCREMENT = 1;

-- 프로시저를 사용하여 100개 상품 생성
DELIMITER $$

DROP PROCEDURE IF EXISTS create_test_products$$

CREATE PROCEDURE create_test_products()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE category VARCHAR(50);
    DECLARE base_price INT;

    WHILE i <= 100 DO
        -- 카테고리별 가격대 설정
        CASE
            WHEN i <= 20 THEN
                SET category = '전자제품';
                SET base_price = 500000;
            WHEN i <= 40 THEN
                SET category = '의류';
                SET base_price = 50000;
            WHEN i <= 60 THEN
                SET category = '도서';
                SET base_price = 15000;
            WHEN i <= 80 THEN
                SET category = '식품';
                SET base_price = 10000;
            ELSE
                SET category = '생활용품';
                SET base_price = 20000;
        END CASE;

        INSERT INTO product (product_name, description, quantity, price, view_count, created_at)
        VALUES (
            CONCAT(category, ' 상품 ', i),
            CONCAT('테스트용 ', category, ' 상품입니다. 품질이 우수합니다.'),
            FLOOR(100 + RAND() * 900),  -- 재고: 100~999개
            base_price + FLOOR(RAND() * base_price * 0.5),  -- 가격: 기본가 ± 50%
            FLOOR(RAND() * 10000),  -- 조회수: 0~9999
            NOW()
        );

        SET i = i + 1;
    END WHILE;
END$$

DELIMITER ;

CALL create_test_products();
DROP PROCEDURE IF EXISTS create_test_products;
