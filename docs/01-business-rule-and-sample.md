# 01. 업무 규칙과 샘플 데이터

## 업무 규칙 (주문 상태코드 변경)

배치가 실행될 때마다 상태코드를 **한 단계씩** 진행시킵니다. (`OrderStatusRule`)

```
ORDERED(주문접수) → PAID(결제완료) → READY(배송준비) → SHIPPED(배송중) → DONE(배송완료)
                                                                      CANCEL(주문취소)
```

- 변경 대상: `PAID → READY`, `READY → SHIPPED`, `SHIPPED → DONE`
- 변경 대상이 아닌 상태: `ORDERED`, `DONE`, `CANCEL` (그대로 유지)

이 규칙은 `common` 패키지의 `OrderStatusRule`에 있고, `db`와 `file` 패키지가 **공통으로** 사용합니다.

## 샘플 데이터 / 기대 결과

입력(`orders.csv`, `sample-data.sql`) 10건에 배치를 1회 실행한 결과입니다.

| order_id | 고객 | 변경 전 | 변경 후 |
|----------|------|---------|---------|
| 1 | kim | ORDERED | ORDERED |
| 2 | lee | PAID | READY |
| 3 | park | PAID | READY |
| 4 | choi | PAID | READY |
| 5 | jung | READY | SHIPPED |
| 6 | kang | READY | SHIPPED |
| 7 | yoon | SHIPPED | DONE |
| 8 | lim | SHIPPED | DONE |
| 9 | han | DONE | DONE |
| 10 | seo | CANCEL | CANCEL |

- **파일 배치**: 변경 대상이 아닌 행도 상태코드를 그대로 두고 **10건 전부** 결과 파일에 기록합니다.
- **DB 배치**: 변경 대상 **7건만** 처리하고, 나머지 3건은 건너뜁니다. (Chunk 방식에서는 Processor가 `null`을 반환해 필터링)
- DB 배치는 상태를 바꿀 때마다 `TB_ORDER_STATUS_HIST`에 변경 전/후 이력을 1건씩 저장합니다.
