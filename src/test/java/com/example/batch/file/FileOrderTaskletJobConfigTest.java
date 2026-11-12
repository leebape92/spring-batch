package com.example.batch.file;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.example.batch.OrderTestData;

/**
 * [파일 / Tasklet] Job 통합 테스트.
 * Job 이 여러 개라 JobLauncherTestUtils 가 Job 을 고르지 못하므로, 이 테스트에서는 해당 Job 설정만 올린다.
 */
@SpringBatchTest
@SpringBootTest(classes = FileOrderTaskletJobConfigTest.TestConfig.class,
        properties = "spring.batch.job.enabled=false")
class FileOrderTaskletJobConfigTest {

    @Configuration
    @EnableAutoConfiguration
    @Import({FileOrderTaskletJobConfig.class, FileOrderTasklet.class})
    static class TestConfig {
    }

    private static final Path OUTPUT = Paths.get(FileOrderTasklet.OUTPUT_PATH);

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @BeforeEach
    void setUp() throws Exception {
        Files.deleteIfExists(OUTPUT);
    }

    @Test
    @DisplayName("fileOrderTaskletJob 이 정상 완료되고 상태코드가 변경된 파일이 생성된다")
    void jobCompleted() throws Exception {
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(
                jobLauncherTestUtils.getUniqueJobParameters());

        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

        List<String> lines = Files.readAllLines(OUTPUT, StandardCharsets.UTF_8);
        assertThat(lines).containsExactlyElementsOf(OrderTestData.expectedResultLines());
    }

    @Test
    @DisplayName("Tasklet Step 은 read/write 건수를 자동 집계하지 않는다(0)")
    void stepCountsAreZero() throws Exception {
        JobExecution jobExecution = jobLauncherTestUtils.launchStep("fileOrderTaskletStep");

        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        assertThat(stepExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(stepExecution.getReadCount()).isZero();
        assertThat(stepExecution.getWriteCount()).isZero();
    }
}
