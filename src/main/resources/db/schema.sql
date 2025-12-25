-- 사용자 테이블
CREATE TABLE IF NOT EXISTS `user` (
    user_id    BIGINT      PRIMARY KEY AUTO_INCREMENT COMMENT '사용자 ID',
    name       VARCHAR(50) NOT NULL                   COMMENT '사용자명',
    point      INT         DEFAULT 0                  COMMENT '보유 포인트',
    version    BIGINT      DEFAULT 0                  COMMENT '낙관적 락 버전',
    created_at DATETIME    DEFAULT CURRENT_TIMESTAMP  COMMENT '생성일시'
);;

-- 상품 테이블
CREATE TABLE IF NOT EXISTS product (
    product_id   BIGINT       PRIMARY KEY AUTO_INCREMENT                            COMMENT '상품 ID',
    product_name VARCHAR(200) NOT NULL                                              COMMENT '상품명',
    description  VARCHAR(200)                                                       COMMENT '상품 설명',
    quantity     INT          DEFAULT 0                                             COMMENT '재고 수량',
    price        INT          NOT NULL                                              COMMENT '상품 가격',
    view_count   INT          DEFAULT 0                                             COMMENT '조회수',
    created_at   DATETIME     DEFAULT CURRENT_TIMESTAMP                             COMMENT '생성일시',
    update_at    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시'
);;

-- 쿠폰 테이블
CREATE TABLE IF NOT EXISTS coupon (
    coupon_id       BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '쿠폰 ID',
    coupon_name     VARCHAR(200) NOT NULL                   COMMENT '쿠폰명',
    discount_type   VARCHAR(50)  NOT NULL                   COMMENT '할인 타입 (RATE: 비율, AMOUNT: 정액)',
    discount_value  INT          NOT NULL                   COMMENT '할인 값 (비율이면 %, 정액이면 금액)',
    total_quantity  INT          NOT NULL                   COMMENT '총 쿠폰 개수',
    issued_quantity INT          DEFAULT 0                  COMMENT '발급 수량',
    used_quantity   INT          DEFAULT 0                  COMMENT '사용 수량',
    create_at       DATETIME     DEFAULT CURRENT_TIMESTAMP  COMMENT '생성일시',
    expires_at      DATETIME                                COMMENT '만료일시'
);;

-- 사용자별 쿠폰 테이블
CREATE TABLE IF NOT EXISTS user_coupon (
    user_coupon_id BIGINT    PRIMARY KEY AUTO_INCREMENT COMMENT '사용자 쿠폰 ID',
    user_id        BIGINT    NOT NULL                   COMMENT '사용자 ID',
    coupon_id      BIGINT    NOT NULL                   COMMENT '쿠폰 ID',
    used           BOOLEAN   DEFAULT FALSE              COMMENT '사용 여부',
    issued_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP  COMMENT '발급일시',
    used_at        TIMESTAMP NULL                       COMMENT '사용일시',
    UNIQUE(user_id, coupon_id),
    INDEX idx_user_coupon_user_id(user_id),
    INDEX idx_user_coupon_coupon_id(coupon_id)
);;

-- 주문 테이블
CREATE TABLE IF NOT EXISTS `order` (
    order_id        BIGINT      PRIMARY KEY AUTO_INCREMENT COMMENT '주문 ID',
    user_id         BIGINT      NOT NULL                   COMMENT '사용자 ID',
    user_coupon_id  BIGINT      NULL                       COMMENT '사용된 쿠폰 ID',
    total_amount    INT         NOT NULL                   COMMENT '총 주문 금액',
    discount_amount INT         DEFAULT 0                  COMMENT '할인 금액',
    used_point      INT         DEFAULT 0                  COMMENT '사용 포인트',
    status          VARCHAR(50) NOT NULL                   COMMENT '주문 상태 (PENDING, COMPLETED, CANCELLED)',
    created_at      TIMESTAMP   DEFAULT CURRENT_TIMESTAMP  COMMENT '주문일시',
    INDEX idx_order_user_id(user_id)
);;

-- 주문 상품 상세 테이블
CREATE TABLE IF NOT EXISTS order_item (
    order_item_id BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '주문 상품 ID',
    order_id      BIGINT       NOT NULL                   COMMENT '주문 ID',
    product_id    BIGINT       NOT NULL                   COMMENT '상품 ID',
    product_name  VARCHAR(255) NOT NULL                   COMMENT '상품명',
    description   VARCHAR(200)                            COMMENT '상품 설명',
    quantity      INT          NOT NULL                   COMMENT '수량',
    price         INT          NOT NULL                   COMMENT '단가',
    created_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP  COMMENT '생성일시',
    INDEX idx_order_item_order_id(order_id),
    INDEX idx_order_item_product_id(product_id)
);;

-- 포인트 이력 테이블
CREATE TABLE IF NOT EXISTS point_log (
    point_log_id  BIGINT      PRIMARY KEY AUTO_INCREMENT COMMENT '포인트 로그 ID',
    user_id       BIGINT      NOT NULL                   COMMENT '사용자 ID',
    type          VARCHAR(50) NOT NULL                   COMMENT '타입 (EARN: 적립, USE: 사용)',
    point_amount  INT         NOT NULL                   COMMENT '포인트 변동량',
    point_balance INT         NOT NULL                   COMMENT '잔여 포인트',
    created_at    TIMESTAMP   DEFAULT CURRENT_TIMESTAMP  COMMENT '생성일시',
    INDEX idx_point_log_user_id(user_id)
);;

-- 장바구니 테이블
CREATE TABLE IF NOT EXISTS cart_item (
    cart_item_id BIGINT    PRIMARY KEY AUTO_INCREMENT                            COMMENT '장바구니 아이템 ID',
    user_id      BIGINT    NOT NULL                                              COMMENT '사용자 ID',
    product_id   BIGINT    NOT NULL                                              COMMENT '상품 ID',
    quantity     INT       NOT NULL                                              COMMENT '수량',
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP                             COMMENT '추가일시',
    updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    INDEX idx_cart_item_user_id(user_id),
    INDEX idx_cart_item_product_id(product_id)
);;
