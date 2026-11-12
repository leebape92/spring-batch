package com.example.batch.file;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.MultiResourceItemWriter;
import org.springframework.batch.item.file.ResourceSuffixCreator;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.file.builder.MultiResourceItemWriterBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * [파일 / Chunk + 파일 분리] Job: fileOrderSplitJob
 * MultiResourceItemWriter 가 "파일당 N건" 기준으로 order-1.csv, order-2.csv ... 를 만든다.
 *
 * 주의: 파일 분리는 chunk 를 통째로 쓴 "뒤"에 판단한다. (chunk 중간에서 자르지 않음)
 *       그래서 파일당 건수는 CHUNK_SIZE 와 같거나 그 배수로 맞추는 것이 좋다.
 *
 * 10건 / chunk 2건 / 파일당 4건 -> order-1.csv(4건), order-2.csv(4건), order-3.csv(2건)
 */
@Configuration
public class FileOrderSplitJobConfig {

    public static final String BASE_PATH = "build/output/order-split/order";

    private static final int CHUNK_SIZE = 2;
    private static final int ITEMS_PER_FILE = 4;
    
    
    // @Qualifier("fileOrderSplitStep")Step fileOrderSplitStep 스텝 메서드명 지정
    @Bean
    public Job fileOrderSplitJob(JobRepository jobRepository, @Qualifier("fileOrderSplitStep")Step fileOrderSplitStep) {
        Job job = new JobBuilder("fileOrderSplitJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(fileOrderSplitStep)
                .build();
        return job;
    }
    
    // 
    @Bean
    public Step fileOrderSplitStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, @Qualifier("fileOrderSplitReader") FlatFileItemReader<FileOrder> fileOrderSplitReader,
                                   FileOrderProcessor fileOrderProcessor, MultiResourceItemWriter<FileOrder> fileOrderSplitWriter) {
        Step step = new StepBuilder("fileOrderSplitStep", jobRepository)
                .<FileOrder, FileOrder>chunk(CHUNK_SIZE, transactionManager)
                .reader(fileOrderSplitReader)
                .processor(fileOrderProcessor)
                .writer(fileOrderSplitWriter)
                .build();
        return step;
    }

    @Bean
    public FlatFileItemReader<FileOrder> fileOrderSplitReader() {
        FlatFileItemReader<FileOrder> reader = new FlatFileItemReaderBuilder<FileOrder>()
                .name("fileOrderSplitReader")
                .resource(new ClassPathResource("input/orders.csv"))
                .delimited()
                .delimiter(",")
                .names(new String[] {"orderId", "customerName", "statusCode", "amount"})
                .targetType(FileOrder.class)
                .build();
        return reader;
    }

    @Bean
    public MultiResourceItemWriter<FileOrder> fileOrderSplitWriter() {
        // 실제 쓰기를 담당하는 Writer (resource 는 MultiResourceItemWriter 가 파일마다 지정해 준다)
        FlatFileItemWriter<FileOrder> delegate = new FlatFileItemWriterBuilder<FileOrder>()
                .name("fileOrderSplitDelegateWriter")
                .delimited()
                .delimiter(",")
                .names(new String[] {"orderId", "customerName", "statusCode", "amount"})
                .build();

        // 파일명 뒤에 붙는 접미사: order + "-1.csv"
        ResourceSuffixCreator suffixCreator = new ResourceSuffixCreator() {
            @Override
            public String getSuffix(int index) {
                String suffix = "-" + index + ".csv";
                return suffix;
            }
        };

        MultiResourceItemWriter<FileOrder> writer = new MultiResourceItemWriterBuilder<FileOrder>()
                .name("fileOrderSplitWriter")
                .resource(new FileSystemResource(BASE_PATH))
                .itemCountLimitPerResource(ITEMS_PER_FILE)
                .resourceSuffixCreator(suffixCreator)
                .delegate(delegate)
                .build();
        return writer;
    }
}
