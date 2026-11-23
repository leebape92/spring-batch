-- =====================================================================
-- MySQL 테이블 생성 스크립트 (스프링 배치 테스트용)
-- 사전 준비: CREATE DATABASE batchdb DEFAULT CHARACTER SET utf8mb4;
--            USE batchdb;
-- 배치 메타 테이블(BATCH_*)은 앱 실행 시 자동 생성되므로 여기에는 없다.
-- =====================================================================

-- 주문
-- status_code : ORDERED(주문접수) -> PAID(결제완료) -> READY(배송준비)
--               -> SHIPPED(배송중) -> DONE(배송완료) / CANCEL(주문취소)
CREATE TABLE IF NOT EXISTS TB_ORDER (
    order_id      BIGINT       NOT NULL                          COMMENT '주문번호',
    customer_name VARCHAR(100) NOT NULL                          COMMENT '고객명',
    status_code   VARCHAR(20)  NOT NULL                          COMMENT '주문상태코드',
    amount        BIGINT       NOT NULL DEFAULT 0                COMMENT '주문금액',
    ordered_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '주문일시',
    updated_at    DATETIME     NULL                              COMMENT '상태변경일시',
    PRIMARY KEY (order_id),
    KEY IX_TB_ORDER_STATUS (status_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '주문';

-- 주문 상태 변경 이력 (배치가 상태를 바꿀 때마다 1건씩 INSERT)
CREATE TABLE IF NOT EXISTS TB_ORDER_STATUS_HIST (
    hist_id       BIGINT       NOT NULL AUTO_INCREMENT           COMMENT '이력번호',
    order_id      BIGINT       NOT NULL                          COMMENT '주문번호',
    before_status VARCHAR(20)  NOT NULL                          COMMENT '변경 전 상태코드',
    after_status  VARCHAR(20)  NOT NULL                          COMMENT '변경 후 상태코드',
    changed_by    VARCHAR(50)  NOT NULL DEFAULT 'BATCH'          COMMENT '변경 주체',
    changed_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '변경일시',
    PRIMARY KEY (hist_id),
    KEY IX_TB_ORDER_STATUS_HIST_ORDER (order_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '주문 상태 변경 이력';
