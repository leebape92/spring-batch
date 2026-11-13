package com.example.batch.file.tasklet;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.example.batch.common.OrderStatusRule;

/**
 * Tasklet 방식: execute() 한 번에 [파일 읽기 -> 상태코드 변경 -> 파일 쓰기]를 전부 직접 처리한다.
 */
@Component
public class FileOrderTasklet implements Tasklet {

    public static final String INPUT_PATH = "input/orders.csv";
    public static final String OUTPUT_PATH = "build/output/order-tasklet-result.csv";

    private static final Logger log = LoggerFactory.getLogger(FileOrderTasklet.class);

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        // 1. 읽기 + 가공
        List<String> resultLines = new ArrayList<String>();
        ClassPathResource resource = new ClassPathResource(INPUT_PATH);
        BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));
        try {
            String line = reader.readLine();
            while (line != null) {
                if (!line.trim().isEmpty()) {
                    String[] columns = line.split(",");
                    String statusCode = columns[2].trim();
                    
                    // 주문상태값 변경
                    String nextStatus = OrderStatusRule.next(statusCode);
                    if (nextStatus == null) {
                        nextStatus = statusCode;
                    }

                    String resultLine = columns[0].trim() + "," + columns[1].trim() + ","+ nextStatus + "," + columns[3].trim();
                    log.info("읽기 + 가공 [File Tasklet] {} -> {}", line, resultLine);
                    resultLines.add(resultLine);
                }
                line = reader.readLine();
            }
        } finally {
            reader.close();
        }

        // 2. 쓰기
        Path outputPath = Paths.get(OUTPUT_PATH);
        Files.createDirectories(outputPath.getParent());
        Files.write(outputPath, resultLines, StandardCharsets.UTF_8);
        log.info("쓰기 [File Tasklet] write : {} ({} lines)", outputPath.toAbsolutePath(), resultLines.size());

        RepeatStatus status = RepeatStatus.FINISHED;
        return status;
    }
}
