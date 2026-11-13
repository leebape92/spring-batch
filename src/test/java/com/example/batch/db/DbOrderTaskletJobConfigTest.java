package com.example.batch.db;

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

/**
 * [DB / Tasklet] Job 통합 테스트.
 */
@SpringBatchTest
@SpringBootTest(
		classes = DbOrderTaskletJobConfigTest.TestConfig.class, 
		properties = {
	        "spring.batch.job.enabled=false", //테스트가 뜰 때 스프링 배치가 자동으로 Job을 실행해 버리는 것을 막음
	        "spring.profiles.active=mysql"
		}
)
class DbOrderTaskletJobConfigTest {

    @Configuration
    @EnableAutoConfiguration
    @Import({DbOrderTaskletJobConfig.class, DbOrderTasklet.class})
    static class TestConfig {
    }

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;
    
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
    @DisplayName("dbOrderTaskletJob: SQL 한 번으로 7건의 상태코드가 변경된다")
    void statusChanged() throws Exception {
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobLauncherTestUtils.getUniqueJobParameters());

        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        
        
        // jdbcTemplate.query(sql, rowMapper) -> DTO로 받아옴
        List<String> statuses = jdbcTemplate.queryForList("SELECT status_code FROM TB_ORDER ORDER BY order_id", String.class);
        System.out.println("statuses:::" + statuses);
        assertThat(statuses).containsExactlyElementsOf(OrderTestData.expectedStatuses());
    }
//
//    @Test
//    @DisplayName("변경된 주문만 updated_at 이 기록되고, 이력이 7건 저장된다")
//    void historySaved() throws Exception {
//        jobLauncherTestUtils.launchJob(jobLauncherTestUtils.getUniqueJobParameters());
//
//        Integer updatedCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM TB_ORDER WHERE updated_at IS NOT NULL", Integer.class);
//        Integer historyCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM TB_ORDER_STATUS_HIST", Integer.class);
//        assertThat(updatedCount).isEqualTo(7);
//        assertThat(historyCount).isEqualTo(7);
//
//        String before = jdbcTemplate.queryForObject("SELECT before_status FROM TB_ORDER_STATUS_HIST WHERE order_id = 2", String.class);
//        String after = jdbcTemplate.queryForObject("SELECT after_status FROM TB_ORDER_STATUS_HIST WHERE order_id = 2", String.class);
//        assertThat(before).isEqualTo("PAID");
//        assertThat(after).isEqualTo("READY");
//    }
//
//    @Test
//    @DisplayName("Tasklet Step 은 read/write 건수를 자동 집계하지 않는다(0)")
//    void stepCountsAreZero() throws Exception {
//        JobExecution jobExecution = jobLauncherTestUtils.launchStep("dbOrderTaskletStep");
//
//        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
//        assertThat(stepExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
//        assertThat(stepExecution.getReadCount()).isZero();
//        assertThat(stepExecution.getWriteCount()).isZero();
//    }
}
