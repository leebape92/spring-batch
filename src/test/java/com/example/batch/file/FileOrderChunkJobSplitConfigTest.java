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
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.example.batch.OrderTestData;

/** [파일 / Chunk + 파일 분리] 10건 / chunk 2건 / 파일당 4건 -> 파일 3개 (4건, 4건, 2건) */
@SpringBatchTest
@SpringBootTest(
		classes = FileOrderChunkJobSplitConfigTest.TestConfig.class, 
		properties = "spring.batch.job.enabled=false"
)
class FileOrderChunkJobSplitConfigTest {

    @Configuration
    @EnableAutoConfiguration
    @Import({FileOrderChunkJobSplitConfig.class, FileOrderProcessor.class})
    static class TestConfig {
    }

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @BeforeEach
    void setUp() throws Exception {
        for (int i = 1; i <= 5; i++) {
            Files.deleteIfExists(splitPath(i));
        }
    }

    @Test
    @DisplayName("결과가 3개의 파일로 나뉘어 생성된다")
    void splitFiles() throws Exception {
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobLauncherTestUtils.getUniqueJobParameters());

        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        List<String> expected = OrderTestData.expectedResultLines();
        assertThat(readLines(splitPath(1))).containsExactlyElementsOf(expected.subList(0, 4));
        assertThat(readLines(splitPath(2))).containsExactlyElementsOf(expected.subList(4, 8));
        assertThat(readLines(splitPath(3))).containsExactlyElementsOf(expected.subList(8, 10));
        assertThat(Files.exists(splitPath(4))).isFalse();
    }
    
    private Path splitPath(int index) {
        Path path = Paths.get(FileOrderChunkJobSplitConfig.BASE_PATH + "-" + index + ".csv");
        return path;
    }

    private List<String> readLines(Path path) throws Exception {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        return lines;
    }
}
