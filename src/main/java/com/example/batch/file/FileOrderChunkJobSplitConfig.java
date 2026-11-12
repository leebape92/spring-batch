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


@Configuration
public class FileOrderChunkJobSplitConfig {

    public static final String BASE_PATH = "build/output/order-split/order";

    private static final int CHUNK_SIZE = 2;
    private static final int ITEMS_PER_FILE = 4;
    
    @Bean
    public Job fileOrderChunkSplitJob(JobRepository jobRepository, @Qualifier("fileOrderChunkSplitStep")Step fileOrderChunkSplitStep) {
        Job job = new JobBuilder("fileOrderChunkSplitJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(fileOrderChunkSplitStep)
                .build();
        return job;
    }
    
    // 
    @Bean
    public Step fileOrderChunkSplitStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, @Qualifier("fileOrderReader") FlatFileItemReader<FileOrder> fileOrderReader,
                                   FileOrderProcessor fileOrderProcessor, MultiResourceItemWriter<FileOrder> fileOrderSplitWriter) {
        Step step = new StepBuilder("fileOrderChunkSplitStep", jobRepository)
                .<FileOrder, FileOrder>chunk(CHUNK_SIZE, transactionManager)
                .reader(fileOrderReader)
                .processor(fileOrderProcessor)
                .writer(fileOrderSplitWriter)
                .build();
        return step;
    }

    @Bean
    public FlatFileItemReader<FileOrder> fileOrderReader() {
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
