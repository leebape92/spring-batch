# 02. Tasklet과 Chunk

Spring Batch의 Step을 구현하는 두 가지 방식입니다.

## Tasklet

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

## Chunk

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

## Tasklet vs Chunk

| | Tasklet | Chunk |
|---|---------|-------|
| 처리 단위 | 작업 전체를 한 번에 | chunk size만큼 묶어서 반복 |
| 구조 | `execute()` 하나 | Reader / Processor / Writer 분리 |
| 트랜잭션 | `execute()` 전체가 하나 | chunk 1개마다 커밋 |
| 잘 맞는 경우 | SQL 한두 개로 끝나는 단순 작업, 일괄 UPDATE | 대용량 데이터, 건별 가공/필터링이 필요한 작업 |
