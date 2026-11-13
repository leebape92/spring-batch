package com.example.batch.db.chunck;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import com.example.batch.OrderTestData;
import com.example.batch.db.chunk.DbOrderChunkJobConfig;
import com.example.batch.db.chunk.DbOrderProcessor;

/**
 * [DB / Chunk] Job 통합 테스트 (H2 인메모리 DB 사용).
 * 읽기 10건 -> 변경 대상 7건만 쓰기, 나머지 3건은 Processor 에서 걸러진다(filter).
 */
@SpringBatchTest
@SpringBootTest(
		classes = DbOrderChunkJobConfigTest.TestConfig.class, 
		properties = {
		"spring.batch.job.enabled=false", //테스트가 뜰 때 스프링 배치가 자동으로 Job을 실행해 버리는 것을 막음
        "spring.profiles.active=mysql" // application 설정파일 mysql 읽기
		}
)
class DbOrderChunkJobConfigTest {

    @Configuration
    @EnableAutoConfiguration
    @Import({DbOrderChunkJobConfig.class, DbOrderProcessor.class})
    static class TestConfig {
    }

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;
    
    // 샘플파일의 테이블 생성
    @BeforeEach
    void setUp() {
    	
    	ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
    	        new ClassPathResource("db/schema.sql"),      // 1. 테이블 생성 스크립트 먼저 실행
    	        new ClassPathResource("db/sample-data.sql")     // 2. 그 후 데이터 초기화 스크립트 실행
    	    );
        populator.setSqlScriptEncoding("UTF-8");
        populator.execute(dataSource);
    }

    @Test
    @DisplayName("dbOrderChunkJob: 상태코드가 한 단계씩 변경된다")
    void statusChanged() throws Exception {
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobLauncherTestUtils.getUniqueJobParameters());

        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        List<String> statuses = jdbcTemplate.queryForList("SELECT status_code FROM TB_ORDER ORDER BY order_id", String.class);
        assertThat(statuses).containsExactlyElementsOf(OrderTestData.expectedStatuses());
    }

    @Test
    @DisplayName("Chunk Step: read 10 / filter 3 / write 7 로 집계된다")
    void stepCounts() throws Exception {
        JobExecution jobExecution = jobLauncherTestUtils.launchStep("dbOrderChunkStep");

        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        assertThat(stepExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(stepExecution.getReadCount()).isEqualTo(10);
        assertThat(stepExecution.getFilterCount()).isEqualTo(3);
        assertThat(stepExecution.getWriteCount()).isEqualTo(7);
        assertThat(stepExecution.getSkipCount()).isZero();
    }

    @Test
    @DisplayName("주문 UPDATE 와 이력 INSERT 가 함께 처리된다")
    void historySaved() throws Exception {
        jobLauncherTestUtils.launchJob(jobLauncherTestUtils.getUniqueJobParameters());

        Integer historyCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM TB_ORDER_STATUS_HIST", Integer.class);
        assertThat(historyCount).isEqualTo(7);

        String before = jdbcTemplate.queryForObject("SELECT before_status FROM TB_ORDER_STATUS_HIST WHERE order_id = 7", String.class);
        String after = jdbcTemplate.queryForObject("SELECT after_status FROM TB_ORDER_STATUS_HIST WHERE order_id = 7", String.class);
        assertThat(before).isEqualTo("SHIPPED");
        assertThat(after).isEqualTo("DONE");
    }

    @Test
    @DisplayName("변경 대상이 아닌 주문(ORDERED, DONE, CANCEL)은 이력이 생기지 않는다")
    void notTargetHasNoHistory() throws Exception {
        jobLauncherTestUtils.launchJob(jobLauncherTestUtils.getUniqueJobParameters());

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM TB_ORDER_STATUS_HIST WHERE order_id IN (1, 9, 10)", Integer.class);
        assertThat(count).isZero();
    }
}
