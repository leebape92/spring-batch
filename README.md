# spring-batch (Spring Boot 3.2 / Java 17 / Gradle)

주문 상태코드를 한 단계씩 진행시키는 배치로 **Tasklet** 과 **Chunk** 방식을 비교합니다.

```
PAID(결제완료) → READY(배송준비) → SHIPPED(배송중) → DONE(배송완료)
ORDERED / DONE / CANCEL 은 변경 대상 아님
```

## 패키지 구조

| 패키지 | 내용 |
|---|---|
| `common` | `OrderStatusRule` : 상태코드 변경 규칙 (파일/DB 공통) |
| `file` | 파일(CSV) 읽기/쓰기 배치 |
| `db` | MySQL 테이블 읽기/쓰기 배치 |

## Job 목록

| Job 이름 | 방식 | 설명 |
|---|---|---|
| `fileOrderTaskletJob` | 파일 / Tasklet | `input/orders.csv` 읽기 → 상태 변경 → `build/output/order-tasklet-result.csv` |
| `fileOrderChunkJob` | 파일 / Chunk | 위와 같은 일을 Reader → Processor → Writer 로 처리 |
| `fileOrderSplitJob` | 파일 / Chunk | 결과를 파일당 4건씩 `order-1.csv`, `order-2.csv` … 로 분리 |
| `dbOrderTaskletJob` | DB / Tasklet | SQL 2개로 상태 변경 + 이력 저장을 한 번에 처리 |
| `dbOrderChunkJob` | DB / Chunk | 1건씩 읽어 변경 대상만 UPDATE + 이력 INSERT (대상 아니면 filter) |

## 테이블 (MySQL)

- `TB_ORDER` : 주문 (order_id PK, customer_name, status_code, amount, ordered_at, updated_at)
- `TB_ORDER_STATUS_HIST` : 상태 변경 이력 (hist_id PK AUTO_INCREMENT, order_id, before_status, after_status, changed_by, changed_at)
- `BATCH_*` : 스프링 배치 메타 테이블 → 앱 실행 시 자동 생성 (`initialize-schema: always`)

## MySQL 준비

```sql
CREATE DATABASE batchdb DEFAULT CHARACTER SET utf8mb4;
USE batchdb;
-- 1) src/main/resources/db/mysql/schema.sql   실행 (테이블 생성)
-- 2) src/main/resources/db/sample-data.sql    실행 (샘플 10건 입력, 다시 실행하면 초기 상태로 복구)
```

접속 정보는 `application-mysql.yml` 을 수정하거나 환경변수(`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`)로 지정합니다.

## 실행

```bash
# 파일 배치 (H2 사용, DB 설치 불필요)
./gradlew bootRun --args='--job.name=fileOrderChunkJob'

# DB 배치 (내 MySQL)
./gradlew bootRun --args='--spring.profiles.active=mysql --job.name=dbOrderChunkJob'
```

## 테스트

```bash
./gradlew test
```

- 파일/DB 통합 테스트는 모두 H2 인메모리 DB를 사용하므로 내 MySQL 에는 영향이 없습니다.
- 샘플 데이터를 다시 돌려 보려면 MySQL 에서 `sample-data.sql` 을 다시 실행하세요.
