-- 샘플 데이터 초기화 + 입력 (여러 번 실행해도 항상 같은 상태로 되돌아간다)
DELETE FROM TB_ORDER_STATUS_HIST;
DELETE FROM TB_ORDER;

INSERT INTO TB_ORDER (order_id, customer_name, status_code, amount) VALUES (1, 'kim', 'ORDERED', 10000);
INSERT INTO TB_ORDER (order_id, customer_name, status_code, amount) VALUES (2, 'lee', 'PAID', 20000);
INSERT INTO TB_ORDER (order_id, customer_name, status_code, amount) VALUES (3, 'park', 'PAID', 15000);
INSERT INTO TB_ORDER (order_id, customer_name, status_code, amount) VALUES (4, 'choi', 'PAID', 30000);
INSERT INTO TB_ORDER (order_id, customer_name, status_code, amount) VALUES (5, 'jung', 'READY', 12000);
INSERT INTO TB_ORDER (order_id, customer_name, status_code, amount) VALUES (6, 'kang', 'READY', 8000);
INSERT INTO TB_ORDER (order_id, customer_name, status_code, amount) VALUES (7, 'yoon', 'SHIPPED', 50000);
INSERT INTO TB_ORDER (order_id, customer_name, status_code, amount) VALUES (8, 'lim', 'SHIPPED', 22000);
INSERT INTO TB_ORDER (order_id, customer_name, status_code, amount) VALUES (9, 'han', 'DONE', 17000);
INSERT INTO TB_ORDER (order_id, customer_name, status_code, amount) VALUES (10, 'seo', 'CANCEL', 9000);
