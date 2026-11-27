# spring-batch

Spring Batch를 처음 구현해보며 **개념 위주로** 정리한 학습용 프로젝트입니다.
같은 업무(주문 상태코드를 한 단계씩 변경)를 **Tasklet 방식**과 **Chunk 방식**으로 각각 구현하고,
처리 대상을 **DB**와 **파일(CSV)** 두 가지로 나누어 비교할 수 있게 구성했습니다.

## 기술 스택

| 구분 | 내용 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.x, Spring Batch |
| Build | Gradle |
| DB | MySQL (`mysql` 프로파일) |
| Test | JUnit5, AssertJ, spring-batch-test (`@SpringBatchTest`) |

## 업무 규칙 (주문 상태코드 변경)

배치가 실행될 때마다 상태코드를 **한 단계씩** 진행시킵니다. (`OrderStatusRule`)

```
ORDERED(주문접수) → PAID(결제완료) → READY(배송준비) → SHIPPED(배송중) → DONE(배송완료)
                                                                      CANCEL(주문취소)
```

- 변경 대상: `PAID → READY`, `READY → SHIPPED`, `SHIPPED → DONE`
- 변경 대상이 아닌 상태: `ORDERED`, `DONE`, `CANCEL` (그대로 유지)

샘플 데이터와 기대 결과는 [docs/01-business-rule-and-sample.md](docs/01-business-rule-and-sample.md)를 참고하세요.

## Job 목록

| Job 이름 | 방식 | 대상 | 설명 |
|----------|------|------|------|
| `dbOrderTaskletJob` | Tasklet | DB | SQL 2개(이력 INSERT + 주문 UPDATE)로 상태코드를 한 번에 변경 |
| `dbOrderChunkJob` | Chunk | DB | `TB_ORDER`를 읽어 2건씩 묶어 UPDATE + 이력 INSERT |
| `fileOrderTaskletJob` | Tasklet | File | `orders.csv`를 읽고 가공하여 결과 파일 1개 생성 |
| `fileOrderChunkJob` | Chunk | File | `orders.csv`를 읽고 가공하여 결과 파일 1개 생성 |
| `fileOrderChunkSplitJob` | Chunk | File | 결과를 **원하는 건수마다 파일을 나누어** 저장 |

## 패키지 구조

`db` 패키지와 `file` 패키지로 구분하고, 각각 `tasklet` / `chunk` 방식을 나누었습니다.

```
src/main/java/com/example/demo
├── SpringBatchApplication.java
├── common
│   └── OrderStatusRule.java              # 상태코드 변경 규칙 (db/file 공통)
├── db
│   ├── DbOrder.java                      # TB_ORDER 한 행
│   ├── tasklet
│   │   ├── DbOrderTaskletJobConfig.java  # Job / Step 선언
│   │   └── DbOrderTasklet.java           # SQL 2개로 일괄 처리
│   └── chunk
│       ├── DbOrderChunkJobConfig.java    # Job / Step / Reader / Writer 선언
│       └── DbOrderProcessor.java
└── file
    ├── FileOrder.java                    # CSV 한 줄
    ├── tasklet
    │   ├── FileOrderTaskletJobConfig.java
    │   └── FileOrderTasklet.java         # 읽기 → 가공 → 쓰기를 직접 처리
    └── chunk
        ├── FileOrderChunkJobConfig.java       # 결과를 파일 1개로 저장
        ├── FileOrderChunkJobSplitConfig.java  # 결과를 N건씩 여러 파일로 분리 저장
        └── FileOrderProcessor.java

src/main/resources
├── application-mysql.yml     # MySQL 접속 설정 (mysql 프로파일)
├── db/schema.sql             # TB_ORDER, TB_ORDER_STATUS_HIST 생성 스크립트
├── db/sample-data.sql        # 샘플 주문 10건 (실행할 때마다 초기화)
└── input/orders.csv          # 파일 배치 입력 파일 (10건)
```

## 실행 방법

`bootRun`으로 직접 실행하지 않고, **JUnit 통합 테스트로 Job을 실행하며 결과를 검증**합니다.

```sql
CREATE DATABASE batch DEFAULT CHARACTER SET utf8mb4;
```
```bash
export DB_PASSWORD=your_password
./gradlew test
```

사전 준비(환경변수), 테스트 구성, 작성 방식은 [docs/05-testing.md](docs/05-testing.md)를 참고하세요.

## 문서

| 문서 | 내용 |
|------|------|
| [01. 업무 규칙과 샘플 데이터](docs/01-business-rule-and-sample.md) | 상태코드 변경 규칙, 입력 10건과 기대 결과 |
| [02. Tasklet과 Chunk](docs/02-tasklet-and-chunk.md) | 두 방식의 개념, Reader/Processor/Writer, 비교 |
| [03. DB 배치](docs/03-db-batch.md) | DB Tasklet / Chunk 구현, `JdbcCursorItemReader` |
| [04. 파일 배치](docs/04-file-batch.md) | 파일 Tasklet / Chunk, 파일 분리 저장, Writer 구조 |
| [05. 테스트](docs/05-testing.md) | 실행 환경, 테스트 구성, 테스트 작성 방식 |