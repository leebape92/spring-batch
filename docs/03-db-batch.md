# 03. DB 배치

`db` 패키지는 `TB_ORDER` 테이블의 주문 상태코드를 변경하고, 변경 이력을 `TB_ORDER_STATUS_HIST`에 저장합니다.

| Job | 방식 | 처리 |
|-----|------|------|
| `dbOrderTaskletJob` | Tasklet | SQL 2개(이력 INSERT + 주문 UPDATE)로 상태코드를 한 번에 변경 |
| `dbOrderChunkJob` | Chunk | `TB_ORDER`를 읽어 2건씩 묶어 UPDATE + 이력 INSERT |

두 방식의 개념 차이는 [02. Tasklet과 Chunk](02-tasklet-and-chunk.md)를 참고하세요.

## Tasklet 방식 (`dbOrderTaskletJob`)

`DbOrderTasklet.execute()` 안에서 SQL 2개를 **같은 트랜잭션**으로 실행합니다. 행을 하나씩 다루지 않고 SQL 한 번에 대상 전체를 처리하는 **집합 처리**입니다.

1. `INSERT ... SELECT` : 변경 대상의 변경 전/후 상태를 `TB_ORDER_STATUS_HIST`에 저장
2. `UPDATE` : `TB_ORDER`의 상태코드 변경

## Chunk 방식 (`dbOrderChunkJob`)

| 구성요소 | 구현 |
|----------|------|
| Reader | `JdbcCursorItemReader` (`TB_ORDER`를 1건씩 읽음) |
| Processor | `DbOrderProcessor` (상태 변경, 변경 대상이 아니면 `null` 반환하여 필터링) |
| Writer | `CompositeItemWriter` (`JdbcBatchItemWriter`로 UPDATE + 이력 INSERT) |
| Chunk size | 2건 |

샘플 10건 기준 Step 집계는 `read 10 / filter 3 / write 7` 입니다. 변경 대상이 아닌 3건(`ORDERED`, `DONE`, `CANCEL`)은 Processor에서 걸러집니다.

## 구현하면서 궁금했던 점

### Q1. DB에는 행이 여러 개인데, Reader는 어떻게 1건씩 읽어올까? (`JdbcCursorItemReader`)

테이블을 조회하면 여러 개의 행이 존재하지만, Reader는 데이터를 **1개씩** 읽어옵니다.
`JdbcCursorItemReader`가 이 방식을 담당합니다.

```java
@Bean
public JdbcCursorItemReader<DbOrder> dbOrderReader(DataSource dataSource) {
    return new JdbcCursorItemReaderBuilder<DbOrder>()
            .name("dbOrderReader")
            .dataSource(dataSource)
            .sql("SELECT order_id, customer_name, status_code, amount FROM TB_ORDER ORDER BY order_id")
            .rowMapper(new BeanPropertyRowMapper<DbOrder>(DbOrder.class))  // 읽어온 행을 DTO에 매핑
            .build();
}
```

- **커서 기반 조회** : JDBC의 `ResultSet`을 이용해 쿼리를 **한 번만** 실행하고,
  스트리밍처럼 커서를 한 칸씩(Row 단위로) 이동시키며 데이터를 읽습니다.
- **별도의 Connection 유지** : 배치 처리가 시작되어 끝날 때까지(Read가 완료될 때까지)
  데이터베이스 커넥션이 계속 연결된 상태를 유지합니다.
- **자동 매핑** : `BeanPropertyRowMapper`가 컬럼명을 DTO 필드명으로 변환해 줍니다.
  (`order_id` → `orderId`, `status_code` → `statusCode`)
