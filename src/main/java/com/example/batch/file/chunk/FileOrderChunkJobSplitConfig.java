package com.example.batch.file.chunk;

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

import com.example.batch.file.FileOrder;


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
    
    
    // MultiResourceItemWriter 관리자
    // FlatFileItemWriter 실무자
    @Bean
    public MultiResourceItemWriter<FileOrder> fileOrderSplitWriter() {
        // 실제 쓰기를 담당하는 Writer (resource 는 MultiResourceItemWriter 가 파일마다 지정해 준다)
        FlatFileItemWriter<FileOrder> fileOrderSplitDelegateWriter = new FlatFileItemWriterBuilder<FileOrder>()
                .name("fileOrderSplitDelegateWriter")
                .delimited()
                .delimiter(",")
                .names(new String[] {"orderId", "customerName", "statusCode", "amount"})
                .build();

        // ResourceSuffixCreato -> MultiResourceItemWriter 여러 개의 출력 파일을   
        // 분할 생성할 때 각 파일 이름 뒤에 접미사: order + "-1.csv" 붙는 형태
        // MultiResourceItemWriter 내부의 resourceIndex 필드가 ++ 됨
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
                .itemCountLimitPerResource(ITEMS_PER_FILE) // 갯수만큼 파일을 넣으면 파일을 닫음 이후 ResourceSuffixCreator 작동하여 파일명 채번 
                .resourceSuffixCreator(suffixCreator)
                .delegate(fileOrderSplitDelegateWriter)
                .build();
        return writer;
    }
}
