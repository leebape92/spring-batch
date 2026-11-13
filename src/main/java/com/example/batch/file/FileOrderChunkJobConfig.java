package com.example.batch.file;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * [파일 / Chunk] Job: fileOrderChunkJob
 * orders.csv 를 읽고(Reader) -> 상태코드 변경(Processor) -> 결과 파일 쓰기(Writer)
 */
@Configuration
public class FileOrderChunkJobConfig {

    public static final String OUTPUT_PATH = "build/output/order-chunk-result.csv";

    private static final int CHUNK_SIZE = 2;

    @Bean
    public Job fileOrderChunkJob(JobRepository jobRepository, @Qualifier("fileOrderChunkStep") Step fileOrderChunkStep) {
        Job job = new JobBuilder("fileOrderChunkJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(fileOrderChunkStep) // Step fileOrderChunkStep 메서드 실행 -> FlatFileItemReader<FileOrder> fileOrderReader, FileOrderProcessor fileOrderProcessor, FlatFileItemWriter<FileOrder> fileOrderWriter 준비
                .build();
        return job;
    }

    @Bean
    public Step fileOrderChunkStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, @Qualifier("fileOrderReader") FlatFileItemReader<FileOrder> fileOrderReader,
                                   FileOrderProcessor fileOrderProcessor, @Qualifier("fileOrderWriter") FlatFileItemWriter<FileOrder> fileOrderWriter) {
        Step step = new StepBuilder("fileOrderChunkStep", jobRepository)
                .<FileOrder, FileOrder>chunk(CHUNK_SIZE, transactionManager)
                .reader(fileOrderReader)
                .processor(fileOrderProcessor)
                .writer(fileOrderWriter)
                .build();
        return step;
    }

    /** 읽기: CSV 한 줄 -> FileOrder 1건 */
    @Bean
    public FlatFileItemReader<FileOrder> fileOrderReader() {
        FlatFileItemReader<FileOrder> reader = new FlatFileItemReaderBuilder<FileOrder>()
                .name("fileOrderReader")
                .resource(new ClassPathResource("input/orders.csv"))
                .delimited()
                .delimiter(",")
                .names(new String[] {"orderId", "customerName", "statusCode", "amount"})
                .targetType(FileOrder.class)
                .build();
        return reader;
    }

    /** 쓰기: chunk(2건) 단위로 묶어서 결과 파일에 기록 */
    @Bean
    public FlatFileItemWriter<FileOrder> fileOrderWriter() {
        FlatFileItemWriter<FileOrder> writer = new FlatFileItemWriterBuilder<FileOrder>()
                .name("fileOrderWriter")
                .resource(new FileSystemResource(OUTPUT_PATH))
                .delimited()
                .delimiter(",")
                .names(new String[] {"orderId", "customerName", "statusCode", "amount"})
                .build();
        return writer;
    }
}
