# 05. 테스트

이 프로젝트는 `bootRun`으로 직접 실행하지 않고, **JUnit 통합 테스트로 Job을 실행하며 결과를 검증**했습니다.
테스트에서는 `JobLauncherTestUtils`로 Job을 실행하고 결과 파일 / DB 데이터를 확인합니다.

## 1. 사전 준비 (MySQL)

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

## 2. 테스트 실행

IDE(Eclipse / IntelliJ)에서 테스트 클래스를 `JUnit Test`로 실행하거나, 터미널에서 실행합니다.

```bash
./gradlew test                                          # 전체
./gradlew test --tests "*DbOrderChunkJobConfigTest"     # 특정 테스트만
```

## 3. 테스트 구성

| 테스트 | 실행하는 Job | 검증 내용 |
|--------|--------------|-----------|
| `OrderStatusRuleTest` | - | 상태코드 변경 규칙 (단위 테스트) |
| `DbOrderTaskletJobConfigTest` | `dbOrderTaskletJob` | 상태코드 일괄 변경 결과 (`TB_ORDER` 조회) |
| `DbOrderChunkJobConfigTest` | `dbOrderChunkJob` | 상태코드 변경 결과 + Step 집계 (read 10 / filter 3 / write 7) |
| `FileOrderTaskletJobConfigTest` | `fileOrderTaskletJob` | 결과 CSV 내용 (`build/output/order-tasklet-result.csv`) |
| `FileOrderChunkJobConfigTest` | `fileOrderChunkJob` | 결과 CSV 내용 (`build/output/order-chunk-result.csv`) |
| `FileOrderChunkJobSplitConfigTest` | `fileOrderChunkSplitJob` | 파일 3개로 분리 (4건 / 4건 / 2건), 4번째 파일은 생성되지 않음 |

## 4. 테스트 작성 방식

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
