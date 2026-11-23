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

## Job 목록

| Job 이름 | 방식 | 대상 | 설명 |
|----------|------|------|------|
| `dbOrderTaskletJob` | Tasklet | DB | SQL 2개(이력 INSERT + 주문 UPDATE)로 상태코드를 한 번에 변경 |
| `dbOrderChunkJob` | Chunk | DB | `TB_ORDER`를 읽어 2건씩 묶어 UPDATE + 이력 INSERT |
| `fileOrderTaskletJob` | Tasklet | File | `orders.csv`를 읽고 가공하여 결과 파일 1개 생성 |
| `fileOrderChunkJob` | Chunk | File | `orders.csv`를 읽고 가공하여 결과 파일 1개 생성 |
| `fileOrderChunkSplitJob` | Chunk | File | 결과를 **원하는 건수마다 파일을 나누어** 저장 |

---

## 개념 정리

### Tasklet

> 하나의 작업을 `execute()` 안에서 **일괄로** 처리하는 방식

1. `Job`을 선언한다.
2. `Job` 메서드 안에서 실행할 `Step`을 명시한다. (`.start(step)`)
3. `Step` 메서드에서 `Tasklet`이 어떤 작업을 일괄 처리할지 명시한다. (`.tasklet(tasklet, transactionManager)`)

```java
@Bean
public Job dbOrderTaskletJob(JobRepository jobRepository, Step dbOrderTaskletStep) {
    return new JobBuilder("dbOrderTaskletJob", jobRepository)
            .incrementer(new RunIdIncrementer())
            .start(dbOrderTaskletStep)          // 실행할 Step 명시
            .build();
}

@Bean
public Step dbOrderTaskletStep(JobRepository jobRepository,
                               PlatformTransactionManager transactionManager,
                               DbOrderTasklet dbOrderTasklet) {
    return new StepBuilder("dbOrderTaskletStep", jobRepository)
            .tasklet(dbOrderTasklet, transactionManager)  // 일괄 처리할 작업 명시
            .build();
}
```

- DB 버전: `INSERT ... SELECT` / `UPDATE` 두 SQL을 **같은 트랜잭션**에서 실행 (집합 처리)
- 파일 버전: `execute()` 한 번에 파일 읽기 → 상태코드 변경 → 파일 쓰기를 전부 직접 처리

### Chunk

> 데이터를 **읽기(Reader) → 가공(Processor) → 쓰기(Writer)** 로 나누고, 지정한 개수(chunk size)만큼 묶어서 처리하는 방식

1. Tasklet과 마찬가지로 `Job`을 선언하고 `Step`을 선언한다.
2. `Step`에서 어떤 `reader`, `processor`, `writer`를 실행할지 명시한다.

```java
@Bean
public Step dbOrderChunkStep(JobRepository jobRepository,
                             PlatformTransactionManager transactionManager,
                             JdbcCursorItemReader<DbOrder> dbOrderReader,
                             DbOrderProcessor dbOrderProcessor,
                             CompositeItemWriter<DbOrder> dbOrderWriter) {
    return new StepBuilder("dbOrderChunkStep", jobRepository)
            .<DbOrder, DbOrder>chunk(CHUNK_SIZE, transactionManager)
            .reader(dbOrderReader)
            .processor(dbOrderProcessor)
            .writer(dbOrderWriter)
            .build();
}
```

`reader`, `processor`, `writer`에 사용하는 메서드/클래스들은 **Step을 실행하기 위한 준비 단계**라고 이해하면 됩니다.

| 구성요소 | 역할 | 이 프로젝트에서 사용한 구현 |
|----------|------|---------------------------|
| Reader | 데이터를 **1건씩** 읽는다 | `JdbcCursorItemReader`, `FlatFileItemReader` |
| Processor | 1건을 가공한다. `null`을 반환하면 Writer로 넘기지 않는다(필터링) | `DbOrderProcessor`, `FileOrderProcessor` |
| Writer | chunk 단위로 **묶어서** 쓴다 | `CompositeItemWriter`(+`JdbcBatchItemWriter`), `FlatFileItemWriter`, `MultiResourceItemWriter` |

### Tasklet vs Chunk

| | Tasklet | Chunk |
|---|---------|-------|
| 처리 단위 | 작업 전체를 한 번에 | chunk size만큼 묶어서 반복 |
| 구조 | `execute()` 하나 | Reader / Processor / Writer 분리 |
| 트랜잭션 | `execute()` 전체가 하나 | chunk 1개마다 커밋 |
| 잘 맞는 경우 | SQL 한두 개로 끝나는 단순 작업, 일괄 UPDATE | 대용량 데이터, 건별 가공/필터링이 필요한 작업 |

---

## 파일 분리 저장 (`fileOrderChunkSplitJob`)

`file` 패키지는 `db` 패키지와 다르게, **파일의 양이 많은 경우를 대비해**
원하는 개수만큼 저장한 이후에 파일을 나누는 로직을 추가했습니다.

- `MultiResourceItemWriter` : 파일을 나누는 **관리자**
- `FlatFileItemWriter` : 실제로 쓰는 **실무자** (delegate)
- `ResourceSuffixCreator` : 파일명 뒤에 붙는 번호(`-1.csv`, `-2.csv` ...)를 만든다

```java
MultiResourceItemWriter<FileOrder> writer = new MultiResourceItemWriterBuilder<FileOrder>()
        .name("fileOrderSplitWriter")
        .resource(new FileSystemResource(BASE_PATH))
        .itemCountLimitPerResource(ITEMS_PER_FILE)   // 이 개수만큼 쓰면 파일을 닫고 다음 파일로
        .resourceSuffixCreator(suffixCreator)        // order-1.csv, order-2.csv ...
        .delegate(fileOrderSplitDelegateWriter)
        .build();
```

샘플 설정은 `chunk 2건 / 파일당 4건` 이므로 입력 10건은 아래처럼 3개 파일로 나뉩니다.

| 파일 | 건수 |
|------|------|
| `build/output/order-split/order-1.csv` | 4건 |
| `build/output/order-split/order-2.csv` | 4건 |
| `build/output/order-split/order-3.csv` | 2건 |

---

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

### Q2. 파일을 나눠서 저장할 때 Writer가 2개인 이유는? (`MultiResourceItemWriter` / `FlatFileItemWriter`)

역할을 **관리자**와 **실무자**로 나눠서 이해하면 쉽습니다.

| 클래스 | 역할 | 하는 일 |
|--------|------|---------|
| `MultiResourceItemWriter` | **관리자** | 몇 건마다 파일을 나눌지 관리하고, 새 파일이 필요할 때 파일명(resource)을 지정해 준다 |
| `FlatFileItemWriter` | **실무자** | 실제로 파일에 한 줄씩 **쓰는** 작업을 담당한다 (`delegate`) |

그래서 `FlatFileItemWriter`를 만들 때는 `resource`를 지정하지 않습니다.
**파일마다 쓸 resource는 `MultiResourceItemWriter`가 지정해 줍니다.**

```java
// 실제 쓰기를 담당하는 Writer (resource 는 MultiResourceItemWriter 가 파일마다 지정해 준다)
FlatFileItemWriter<FileOrder> fileOrderSplitDelegateWriter = new FlatFileItemWriterBuilder<FileOrder>()
        .name("fileOrderSplitDelegateWriter")
        .delimited().delimiter(",")
        .names(new String[] {"orderId", "customerName", "statusCode", "amount"})
        .build();
```

### Q3. 나뉜 파일의 이름은 어떻게 붙을까? (`ResourceSuffixCreator`)

`ResourceSuffixCreator`는 `MultiResourceItemWriter`가 여러 개의 출력 파일을 분할 생성할 때,
**각 파일 이름 뒤에 붙는 접미사**를 만들어 줍니다. (예: `order` + `-1.csv`)

```java
ResourceSuffixCreator suffixCreator = new ResourceSuffixCreator() {
    @Override
    public String getSuffix(int index) {
        return "-" + index + ".csv";
    }
};
```

동작 순서는 다음과 같습니다.

1. `itemCountLimitPerResource` 에 지정한 개수만큼 파일에 쓰면 현재 파일을 **닫는다**.
2. `MultiResourceItemWriter` 내부의 `resourceIndex` 필드가 `++` 된다.
3. 증가한 `index`로 `ResourceSuffixCreator`가 동작하여 **파일명이 채번**된다. (`order-1.csv` → `order-2.csv` → ...)
4. 새 파일을 열고 이어서 쓴다.

---

## 실행 방법 (JUnit 테스트)

이 프로젝트는 `bootRun`으로 직접 실행하지 않고, **JUnit 통합 테스트로 Job을 실행하며 결과를 검증**했습니다.
테스트에서는 `JobLauncherTestUtils`로 Job을 실행하고 결과 파일 / DB 데이터를 확인합니다.

### 1. 사전 준비 (MySQL)

배치 메타 테이블(`BATCH_JOB_INSTANCE` 등)을 저장할 DB가 필요하므로 로컬 MySQL이 실행 중이어야 합니다.

```sql
CREATE DATABASE batch DEFAULT CHARACTER SET utf8mb4;
```

접속 정보는 `application-mysql.yml`에서 환경변수로 덮어쓸 수 있습니다. (미지정 시 yml의 기본값 사용)

```bash
export DB_URL='jdbc:mysql://localhost:3306/batch?serverTimezone=Asia/Seoul&characterEncoding=UTF-8&useSSL=false&allowPublicKeyRetrieval=true'
export DB_USERNAME=root
export DB_PASSWORD=your_password
```

- 배치 메타 테이블은 최초 실행 시 자동 생성됩니다. (`initialize-schema: always`)
- `TB_ORDER`, `TB_ORDER_STATUS_HIST` 테이블과 샘플 데이터는 **DB 테스트의 `@BeforeEach`에서 자동으로**
  `db/schema.sql` → `db/sample-data.sql` 순서로 실행되므로 따로 만들 필요가 없습니다.

### 2. 테스트 실행

IDE(Eclipse / IntelliJ)에서 테스트 클래스를 `JUnit Test`로 실행하거나, 터미널에서 실행합니다.

```bash
./gradlew test                                          # 전체
./gradlew test --tests "*DbOrderChunkJobConfigTest"     # 특정 테스트만
```

### 3. 테스트 구성

| 테스트 | 실행하는 Job | 검증 내용 |
|--------|--------------|-----------|
| `OrderStatusRuleTest` | - | 상태코드 변경 규칙 (단위 테스트) |
| `DbOrderTaskletJobConfigTest` | `dbOrderTaskletJob` | 상태코드 일괄 변경 결과 (`TB_ORDER` 조회) |
| `DbOrderChunkJobConfigTest` | `dbOrderChunkJob` | 상태코드 변경 결과 + Step 집계 (read 10 / filter 3 / write 7) |
| `FileOrderTaskletJobConfigTest` | `fileOrderTaskletJob` | 결과 CSV 내용 (`build/output/order-tasklet-result.csv`) |
| `FileOrderChunkJobConfigTest` | `fileOrderChunkJob` | 결과 CSV 내용 (`build/output/order-chunk-result.csv`) |
| `FileOrderChunkJobSplitConfigTest` | `fileOrderChunkSplitJob` | 파일 3개로 분리 (4건 / 4건 / 2건), 4번째 파일은 생성되지 않음 |

### 4. 테스트 작성 방식

```java
@SpringBatchTest
@SpringBootTest(
        classes = DbOrderChunkJobConfigTest.TestConfig.class,
        properties = {
                "spring.batch.job.enabled=false",    // 테스트 시작 시 Job이 자동 실행되는 것을 막음
                "spring.profiles.active=mysql"       // application-mysql.yml 사용
        }
)
class DbOrderChunkJobConfigTest {

    @Configuration
    @EnableAutoConfiguration
    @Import({DbOrderChunkJobConfig.class, DbOrderProcessor.class})  // 테스트할 Job 설정만 로딩
    static class TestConfig {}

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Test
    void statusChanged() throws Exception {
        JobExecution jobExecution =
                jobLauncherTestUtils.launchJob(jobLauncherTestUtils.getUniqueJobParameters());
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        // 이후 DB / 결과 파일 내용 검증
    }
}
```

- **Job 설정을 하나만 import** : 프로젝트에 Job이 여러 개라 `JobLauncherTestUtils`가 어떤 Job을 실행할지 고르지 못합니다.
  그래서 테스트마다 해당 Job 설정 클래스만 올려서, 컨테이너에 Job이 1개만 존재하도록 했습니다.
- **`getUniqueJobParameters()`** : 실행할 때마다 다른 파라미터를 만들어 같은 Job을 반복 실행할 수 있게 합니다.
- **`launchStep("stepName")`** : Job 전체가 아니라 Step만 실행하고 `StepExecution`의 `readCount` / `filterCount` / `writeCount`를 확인할 때 사용합니다.
- **DB 테스트** : `@BeforeEach`에서 `ResourceDatabasePopulator`로 테이블 생성 + 샘플 데이터 초기화를 하므로 몇 번을 실행해도 항상 같은 상태에서 시작합니다.
- **파일 테스트** : `@BeforeEach`에서 이전 결과 파일을 삭제한 뒤 실행하고, 결과를 `OrderTestData.expectedResultLines()`와 비교합니다.

---

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
