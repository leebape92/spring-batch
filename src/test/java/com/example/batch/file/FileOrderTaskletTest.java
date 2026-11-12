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
import org.springframework.batch.repeat.RepeatStatus;

import com.example.batch.OrderTestData;

/** Tasklet 단위 테스트 (스프링 컨텍스트 없이 execute() 를 직접 호출) */
class FileOrderTaskletTest {

    private static final Path OUTPUT = Paths.get(FileOrderTasklet.OUTPUT_PATH);

    private final FileOrderTasklet tasklet = new FileOrderTasklet();

    @BeforeEach
    void setUp() throws Exception {
        Files.deleteIfExists(OUTPUT);
    }

    @Test
    @DisplayName("execute() 는 FINISHED 를 반환한다")
    void returnsFinished() throws Exception {
        RepeatStatus result = tasklet.execute(null, null);

        assertThat(result).isEqualTo(RepeatStatus.FINISHED);
    }

    @Test
    @DisplayName("파일을 읽어 상태코드를 변경한 뒤 결과 파일에 쓴다")
    void readChangeWrite() throws Exception {
        tasklet.execute(null, null);
        
        // 파일 데이터와 일치하는지 검증
        List<String> lines = Files.readAllLines(OUTPUT, StandardCharsets.UTF_8);
        assertThat(lines).containsExactlyElementsOf(OrderTestData.expectedResultLines());
    }
}
